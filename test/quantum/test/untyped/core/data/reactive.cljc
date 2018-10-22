(ns quantum.test.untyped.core.data.reactive
  "Tests adapted from `reagenttest.testratom`."
  {:todo #{"Adapt tests from https://github.com/reagent-project/reagent/blob/master/test/reagenttest/testratomasync.cljs"
           "Adapt tests from https://github.com/reagent-project/reagent/blob/master/test/reagenttest/testtrack.cljs"}}
  (:require
    [quantum.untyped.core.test          :as utest
      :refer [deftest is is= testing]]
    [quantum.untyped.core.data.reactive :as rx
      :refer [dispose! eager-rx flush! rx]]))

(defn with-debug [f]
  (flush! rx/global-queue)
  (binding [rx/*debug?* true] (f)))

(utest/use-fixtures :once with-debug)

(defn- running [] @@#'rx/*running)

(defn test-perf []
  ;; (set! debug? false) ; yes but we need to think about CLJ
  (dotimes [_ 10]
    (let [a   (rx/atom 0)
          f   (fn [] (quot (long @a) 10))
          q   (@#'rx/alist)
          mid (rx/>rx f {:queue q})
          res (rx/>track! (fn [] (inc (long @mid))) [] {:queue q})]
      @res
      (time (dotimes [_ 100000] ; ~70ms per 100K in CLJ so 0.0007ms for one (0.7 µs or 700 ns)
              (swap! a inc)
              (@#'rx/flush! q)))
      (dispose! res))))

(deftest basic-atom
  (binding [rx/*enqueue!* @#'rx/alist-conj!]
    (let [runs  (running)
          start (rx/atom 0)
          sv    (eager-rx @start)
          comp  (eager-rx @sv (+ 2 @sv))
          c2    (eager-rx (inc @comp))
          ct    (rx/atom 0)
          out   (rx/atom 0)
          res   (eager-rx (swap! ct inc) @sv @c2 @comp)
          const (rx/run! (reset! out @res))]
      (is (= @ct 1) "constrain ran")
      (is (= @out 2))
      (reset! start 1)
      (flush! rx/global-queue)
      (is (= @out 3)) ; not correct; showing 2
      (is (<= 2 @ct 3))
      (dispose! const)
      (is (= (running) runs)))))

(deftest double-dependency
  (let [runs     (running)
        start    (rx/atom 0)
        c3-count (rx/atom 0)
        c1       (eager-rx @start 1)
        c2       (eager-rx @start)
        c3       (rx (swap! c3-count inc)
                     (+ @c1 @c2))]
    (flush! rx/global-queue)
    (is (= @c3-count 0))
    (is (= @c3 1))
    (is (= @c3-count 1) "t1")
    (swap! start inc)
    (flush! rx/global-queue)
    (is (= @c3-count 2) "t2")
    (is (= @c3 2))
    (is (= @c3-count 2) "t3")
    (dispose! c3)
    (is (= (running) runs))))

(deftest test-from-reflex ; https://github.com/lynaghk/reflex
  (let [runs (running)]
    (let [*counter (rx/atom 0)
          *signal  (rx/atom "All I do is change")
          co (rx/run! @*signal (swap! *counter inc))]
      (is (= 1 @*counter) "Constraint run on init")
      (reset! *signal "foo")
      (flush! rx/global-queue)
      (is (= 2 @*counter)
          "Counter auto updated")
      (dispose! co))
    (let [*x  (rx/atom 0)
          *co (rx (inc @*x))]
      (is (= 1 @*co) "CO has correct value on first deref")
      (swap! *x inc)
      (is (= 2 @*co) "CO auto-updates")
      (dispose! *co))
    (is (= (running) runs))))

(deftest test-unsubscribe
  (dotimes [x 10]
    (let [runs      (running)
          a         (rx/atom 0)
          a1        (eager-rx (inc @a))
          a2        (eager-rx @a)
          b-changed (rx/atom 0)
          c-changed (rx/atom 0)
          b         (eager-rx
                      (swap! b-changed inc)
                      (inc @a1))
          c         (eager-rx
                      (swap! c-changed inc)
                      (+ 10 @a2))
          res       (rx/run! (if (< @a2 1) @b @c))]
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
          a    (rx/atom 0)
          b    (eager-rx (inc @a))
          c    (eager-rx (dec @a))
          d    (eager-rx (str @b))
          res  (rx/atom 0)
          cs   (rx/run! (reset! res @d))]
      (is (= @res "1"))
      (dispose! cs))
    ;; should be broken according to https://github.com/lynaghk/reflex/issues/1
    ;; but isnt
    (let [a (rx/atom 0)
          b (eager-rx (inc @a))
          c (eager-rx (dec @a))
          d (rx/run! [@b @c])]
      (is (= @d [1 -1]))
      (dispose! d))
    (let [a (rx/atom 0)
          b (eager-rx (inc @a))
          c (eager-rx (dec @a))
          d (rx/run! [@b @c])
          res (rx/atom 0)]
      (is (= @d [1 -1]))
      (let [e (rx/run! (reset! res @d))]
        (is (= @res [1 -1]))
        (dispose! e))
      (dispose! d))
    (is (= (running) runs))))

(deftest test-dispose
  (binding [rx/*enqueue!* @#'rx/alist-conj!]
    (dotimes [x 10]
      (let [runs         (running)
            a            (rx/atom 0)
            disposed     (rx/atom nil)
            disposed-c   (rx/atom nil)
            disposed-cns (rx/atom nil)
            count-b      (rx/atom 0)
            b   (rx/>rx (fn [] (swap! count-b inc) (inc @a))
                        {:always-recompute? true
                         :on-dispose        (fn [r] (reset! disposed true))
                         :queue             rx/global-queue})
            c   (rx/>rx #(if (< @a 1) (inc @b) (dec @a))
                        {:always-recompute? true
                         :on-dispose        (fn [r] (reset! disposed-c true))
                         :queue             rx/global-queue})
            res (rx/atom nil)
            cns (rx/>rx #(reset! res @c)
                        {:on-dispose (fn [r] (reset! disposed-cns true))
                         :queue      rx/global-queue})]
        @cns
        (is (= @res 2))
        (is (= (+ 4 runs) (running)))
        (is (= @count-b 1))
        (reset! a -1)
        (flush! rx/global-queue)
        (is (= @res 1))
        (is (= @disposed nil))
        (is (= @count-b 2))
        (is (= (+ 4 runs) (running)) "still running")
        (reset! a 2)
        (flush! rx/global-queue)
        (is (= @res 1))
        (is (= @disposed true))
        (is (= (+ 2 runs) (running)) "less running count")

        (reset! disposed nil)
        (reset! a -1)
        (flush! rx/global-queue)
        ;; This fails sometimes on node. I have no idea why.
        (is (= 1 @res) "should be one again")
        (is (= @disposed nil))
        (reset! a 2)
        (flush! rx/global-queue)
        (is (= @res 1))
        (is (= @disposed true))
        (dispose! cns)
        (is (= @disposed-c true))
        (is (= @disposed-cns true))
        (is (= runs (running)))))))

(deftest test-add-dispose
  (dotimes [x 10]
    (let [runs         (running)
          a            (rx/atom 0)
          disposed     (rx/atom nil)
          disposed-c   (rx/atom nil)
          disposed-cns (rx/atom nil)
          count-b      (rx/atom 0)
          b            (eager-rx (swap! count-b inc) (inc @a))
          c            (eager-rx (if (< @a 1) (inc @b) (dec @a)))
          res          (rx/atom nil)
          cns          (rx (reset! res @c))]
      (rx/add-on-dispose! b (fn [r]
                                (is (= r b))
                                (reset! disposed true)))
      (rx/add-on-dispose! c   (fn [r] (reset! disposed-c true)))
      (rx/add-on-dispose! cns (fn [r] (reset! disposed-cns true)))
      @cns
      (is (= @res 2))
      (is (= (+ 4 runs) (running)))
      (is (= @count-b 1))
      (reset! a -1)
      (flush! rx/global-queue)
      (is (= @res 1))
      (is (= @disposed nil))
      (is (= @count-b 2))
      (is (= (+ 4 runs) (running)) "still running")
      (reset! a 2)
      (flush! rx/global-queue)
      (is (= @res 1))
      (is (= @disposed true))
      (is (= (+ 2 runs) (running)) "less running count")

      (reset! disposed nil)
      (reset! a -1)
      (flush! rx/global-queue)
      (is (= 1 @res) "should be one again")
      (is (= @disposed nil))
      (reset! a 2)
      (flush! rx/global-queue)
      (is (= @res 1))
      (is (= @disposed true))
      (dispose! cns)
      (is (= @disposed-c true))
      (is (= @disposed-cns true))
      (is (= runs (running))))))

(deftest test-on-set
  (let [runs (running)
        a (rx/atom 0)
        b (rx/>rx #(+ 5 @a)
                  {:on-set (fn [oldv newv] (reset! a (+ 10 newv)))
                   :queue  rx/global-queue})]
    @b
    (is (= 5 @b))
    (reset! a 1)
    (is (= 6 @b))
    (reset! b 1)
    (is (= 11 @a))
    (is (= 16 @b))
    (dispose! b)
    (is (= runs (running)))))

(deftest non-reactive-deref
  (let [runs (running)
        a    (rx/atom 0)
        b    (eager-rx (+ 5 @a))]
    (is (= @b 5))
    (is (= runs (running)))

    (reset! a 1)
    (is (= @b 6))
    (is (= runs (running)))))

(deftest reset-in-reaction
  (let [runs  (running)
        state (rx/atom {})
        c1    (eager-rx (get-in @state [:data :a]))
        c2    (eager-rx (get-in @state [:data :b]))
        rxn   (rx (let [cc1 @c1, cc2 @c2]
                    (swap! state assoc :derived (+ (or cc1 0) (or cc2 0)))
                    nil))]
    @rxn
    (is (= (:derived @state) 0))
    (swap! state assoc :data {:a 1 :b 2})
    (flush! rx/global-queue)
    (is (= (:derived @state) 3))
    (swap! state assoc :data {:a 11 :b 22})
    (flush! rx/global-queue)
    (is (= (:derived @state) 33))
    (dispose! rxn)
    (is (= runs (running)))))

(deftest exception-recover
  (let [runs  (running)
        state (rx/atom 1)
        count (rx/atom 0)
        r     (rx/run!
                (swap! count inc)
                (when (> @state 1) (throw (ex-info "oops" {}))))]
    (is (= @count 1))
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                 (do (swap! state inc)
                     (flush! rx/global-queue))))
    (is (= @count 2))
    (swap! state dec)
    (flush! rx/global-queue)
    (is (= @count 3))
    (dispose! r)
    (is (= runs (running)))))

(deftest exception-recover-indirect
  (let [runs  (running)
        state (rx/atom 1)
        count (rx/atom 0)
        ref   (eager-rx (when (= @state 2)
                          (throw (ex-info "err" {}))))
        r (rx/run!
            (swap! count inc)
            @ref)]
    (is (= @count 1))
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                 (do (swap! state inc)
                     (flush! rx/global-queue))))
    (is (= @count 2))
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo) @ref))
    (swap! state inc)
    (flush! rx/global-queue)
    (is (= @count 3))
    (dispose! r)
    (is (= runs (running)))))

(deftest exception-side-effect
  (binding [rx/*enqueue!* @#'rx/alist-conj!]
    (let [runs   (running)
          state  (rx/atom {:val 1})
          rstate (eager-rx @state)
          spy    (atom nil)
          r1     (rx/run! @rstate)
          r2     (let [val (eager-rx (:val @rstate))]
                   (rx/run!
                     (reset! spy @val)
                     (is (some? @val))))
          r3     (rx/run!
                   (when (:error? @rstate)
                     (throw (ex-info "Error detected!" {}))))]
      (swap! state assoc :val 2)
      (flush! rx/global-queue)
      (swap! state assoc :error? true)
      (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                   (flush! rx/global-queue)))
      (flush! rx/global-queue)
      (flush! rx/global-queue)
      (dispose! r1)
      (dispose! r2)
      (dispose! r3)
      (is (= runs (running))))))

(deftest exception-reporting
  (binding [rx/*enqueue!* @#'rx/alist-conj!]
    (let [runs   (running)
          state  (rx/atom {:val 1})
          rstate (eager-rx (:val @state))
          r1     (rx/run!
                   (when (= @rstate 13)
                     (throw (ex-info "fail" {}))))]
      (swap! state assoc :val 13)
      (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                   (flush! rx/global-queue)))
      (swap! state assoc :val 2)
      (flush! rx/global-queue)
      (dispose! r1)
      (is (= runs (running))))))

(deftest atom-with-meta
  (let [value      {:val 1}
        meta-value {:meta-val 1}
        state      (with-meta (rx/atom value) meta-value)]
    (is (= (meta state) meta-value))
    (is (= @state value))))

(deftest test-eager-vs-lazy-reaction
  (let [a         (rx/atom 123)
        b-ct      (atom 0)
        b         (eager-rx (swap! b-ct inc) (+ @a  2))
        c-ct      (atom 0)
        c         (eager-rx (swap! c-ct inc) (* @b -1))
        b-lazy-ct (atom 0)
        b-lazy    (rx (swap! b-lazy-ct inc) (+ @a       2))
        c-lazy-ct (atom 0)
        c-lazy    (rx (swap! c-lazy-ct inc) (* @b-lazy -1))]
    (testing "eager"
      @c
      (is= @b-ct 1)
      (is= @c-ct 1)
      @c
      (is= @b-ct 2)
      (is= @c-ct 2)
      @c
      (is= @b-ct 3)
      (is= @c-ct 3)

      (reset! a 234)

      @c
      (is= @b-ct 4)
      (is= @c-ct 4))

    (testing "lazy"
      @c-lazy
      (is= @b-lazy-ct 1)
      (is= @c-lazy-ct 1)
      @c-lazy
      (is= @b-lazy-ct 1)
      (is= @c-lazy-ct 1)
      @c-lazy
      (is= @b-lazy-ct 1)
      (is= @c-lazy-ct 1)

      (reset! a 234) ; resetting to the same state

      @c-lazy
      (is= @b-lazy-ct 2)
      (is= @c-lazy-ct 1)

      (reset! a 123)

      @c-lazy
      (is= @b-lazy-ct 3)
      (is= @c-lazy-ct 2)
      @c-lazy
      (is= @b-lazy-ct 3)
      (is= @c-lazy-ct 2))))
