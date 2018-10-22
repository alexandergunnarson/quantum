(ns quantum.test.untyped.core.data.reactive
  "Tests adapted from `reagenttest.testratom`."
  (:require
    [quantum.untyped.core.test          :as utest
      :refer [deftest is testing]]
    [quantum.untyped.core.data.reactive :as self
      :refer [dispose! flush! ratom rx]]))

(defn with-debug [f]
  (flush! self/global-queue)
  (binding [self/*debug?* true] (f)))

(utest/use-fixtures :once with-debug)

(defn- running [] @@#'self/*running)

(defn test-perf []
  ;; (set! debug? false) ; yes but we need to think about CLJ
  (dotimes [_ 10]
    (let [a   (ratom 0)
          f   (fn [] (quot (long @a) 10))
          q   (@#'self/alist)
          mid (self/>rx f {:queue q})
          res (self/>track! (fn [] (inc (long @mid))) [] {:queue q})]
      @res
      (time (dotimes [_ 100000] ; ~70ms per 100K
              (swap! a inc)
              (@#'self/flush! q)))
      (dispose! res))))

(deftest basic-ratom
  (binding [self/*enqueue!* @#'self/alist-conj!]
    (let [runs  (running)
          start (ratom 0)
          sv    (rx @start)
          comp  (rx @sv (+ 2 @sv))
          c2    (rx (inc @comp))
          ct    (ratom 0)
          out   (ratom 0)
          res   (rx (swap! ct inc)
                    @sv @c2 @comp)
          const (self/run! (reset! out @res))]
      (is (= @ct 1) "constrain ran")
      (is (= @out 2))
      (reset! start 1)
      (flush! self/global-queue)
      (is (= @out 3)) ; not correct; showing 2
      (is (<= 2 @ct 3))
      (dispose! const)
      (is (= (running) runs)))))

(deftest double-dependency
  (let [runs (running)
        start (ratom 0)
        c3-count (ratom 0)
        c1 (rx @start 1)
        c2 (rx @start)
        c3 (self/>rx
            (fn []
              (swap! c3-count inc)
              (+ @c1 @c2))
            {:auto-run true :queue self/global-queue})]
    (flush! self/global-queue)
    (is (= @c3-count 0))
    (is (= @c3 1))
    (is (= @c3-count 1) "t1")
    (swap! start inc)
    (flush! self/global-queue)
    (is (= @c3-count 2) "t2")
    (is (= @c3 2))
    (is (= @c3-count 2) "t3")
    (dispose! c3)
    (is (= (running) runs))))

(deftest test-from-reflex ; https://github.com/lynaghk/reflex
  (let [runs (running)]
    (let [*counter (ratom 0)
          *signal  (ratom "All I do is change")
          co (self/run!
              ;; when I change...
              @*signal
              ;; update the counter
              (swap! *counter inc))]
      (is (= 1 @*counter) "Constraint run on init")
      (reset! *signal "foo")
      (flush! self/global-queue)
      (is (= 2 @*counter)
          "Counter auto updated")
      (dispose! co))
    (let [*x  (ratom 0)
          *co (self/>rx #(inc @*x) {:auto-run true :queue self/global-queue})]
      (is (= 1 @*co) "CO has correct value on first deref")
      (swap! *x inc)
      (is (= 2 @*co) "CO auto-updates")
      (dispose! *co))
    (is (= (running) runs))))

(deftest test-unsubscribe
  (dotimes [x 10]
    (let [runs (running)
          a  (ratom 0)
          a1 (rx (inc @a))
          a2 (rx @a)
          b-changed (ratom 0)
          c-changed (ratom 0)
          b (rx (swap! b-changed inc)
                (inc @a1))
          c (rx (swap! c-changed inc)
                (+ 10 @a2))
          res (self/run! (if (< @a2 1) @b @c))]
      (is (= @res (+ 2 @a)))
      (is (= @b-changed 1))
      (is (= @c-changed 0))

      (reset! a -1)
      (is (= @res (+ 2 @a)))
      (is (= @b-changed 2))
      (is (= @c-changed 0))

      (reset! a 2)
      (is (= @res (+ 10 @a)))
      (is (<= 2 @b-changed 3))
      (is (= @c-changed 1))

      (reset! a 3)
      (is (= @res (+ 10 @a)))
      (is (<= 2 @b-changed 3))
      (is (= @c-changed 2))

      (reset! a 3)
      (is (= @res (+ 10 @a)))
      (is (<= 2 @b-changed 3))
      (is (= @c-changed 2))

      (reset! a -1)
      (is (= @res (+ 2 @a)))
      (dispose! res)
      (is (= (running) runs)))))

(deftest maybe-broken
  (let [runs (running)]
    (let [runs (running)
          a    (ratom 0)
          b    (rx (inc @a))
          c    (rx (dec @a))
          d    (rx (str @b))
          res  (ratom 0)
          cs   (self/run! (reset! res @d))]
      (is (= @res "1"))
      (dispose! cs))
    ;; should be broken according to https://github.com/lynaghk/reflex/issues/1
    ;; but isnt
    (let [a (ratom 0)
          b (rx (inc @a))
          c (rx (dec @a))
          d (self/run! [@b @c])]
      (is (= @d [1 -1]))
      (dispose! d))
    (let [a (ratom 0)
          b (rx (inc @a))
          c (rx (dec @a))
          d (self/run! [@b @c])
          res (ratom 0)]
      (is (= @d [1 -1]))
      (let [e (self/run! (reset! res @d))]
        (is (= @res [1 -1]))
        (dispose! e))
      (dispose! d))
    (is (= (running) runs))))

(deftest test-dispose
  (dotimes [x 10]
    (let [runs         (running)
          a            (ratom 0)
          disposed     (ratom nil)
          disposed-c   (ratom nil)
          disposed-cns (ratom nil)
          count-b      (ratom 0)
          b   (self/>rx (fn []
                          (swap! count-b inc)
                          (inc @a))
                        {:on-dispose (fn [r] (reset! disposed true))
                         :queue      self/global-queue})
          c   (self/>rx #(if (< @a 1) (inc @b) (dec @a))
                        {:on-dispose (fn [r] (reset! disposed-c true))
                         :queue      self/global-queue})
          res (ratom nil)
          cns (self/>rx #(reset! res @c)
                        {:auto-run   true
                         :on-dispose (fn [r] (reset! disposed-cns true))
                         :queue      self/global-queue})]
      @cns
      (is (= @res 2))
      (is (= (+ 4 runs) (running)))
      (is (= @count-b 1))
      (reset! a -1)
      (flush! self/global-queue)
      (is (= @res 1))
      (is (= @disposed nil))
      (is (= @count-b 2))
      (is (= (+ 4 runs) (running)) "still running")
      (reset! a 2)
      (flush! self/global-queue)
      (is (= @res 1))
      (is (= @disposed true))
      (is (= (+ 2 runs) (running)) "less running count")

      (reset! disposed nil)
      (reset! a -1)
      (flush! self/global-queue)
      ;; This fails sometimes on node. I have no idea why.
      (is (= 1 @res) "should be one again")
      (is (= @disposed nil))
      (reset! a 2)
      (flush! self/global-queue)
      (is (= @res 1))
      (is (= @disposed true))
      (dispose! cns)
      (is (= @disposed-c true))
      (is (= @disposed-cns true))
      (is (= runs (running))))))
