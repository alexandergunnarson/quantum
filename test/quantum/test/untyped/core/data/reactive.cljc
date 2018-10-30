(ns quantum.test.untyped.core.data.reactive
  "Tests adapted from `reagenttest.testratom`."
  {:todo #{"Adapt tests from https://github.com/reagent-project/reagent/blob/master/test/reagenttest/testratomasync.cljs"
           "Adapt tests from https://github.com/reagent-project/reagent/blob/master/test/reagenttest/testtrack.cljs"}}
  (:require
    [quantum.untyped.core.test          :as utest
      :refer [deftest is is= testing]]
    [quantum.untyped.core.data.vector   :as uvec]
    [quantum.untyped.core.data.reactive :as urx
      :refer [! !eager-rx !run-rx !rx >!rx dispose! flush!]]
    [quantum.untyped.core.refs          :as uref]))

(defn with-debug [f]
  (flush! urx/global-queue)
  (binding [urx/*debug?* true] (f)))

(utest/use-fixtures :once with-debug)

(defn- running [] @@#'urx/*running)

(defn test-perf []
  ;; (set! debug? false) ; yes but we need to think about CLJ
  (dotimes [_ 10]
    (let [a   (! 0)
          f   (fn [] (quot (long @a) 10))
          q   (uvec/alist)
          mid (>!rx f {:queue q})
          res (urx/>track! (fn [] (inc (long @mid))) [] {:queue q})]
      @res
      (time (dotimes [_ 100000] ; ~70ms per 100K in CLJ so 0.0007ms for one (0.7 µs or 700 ns)
              (uref/update! a inc)
              (@#'urx/flush! q)))
      (dispose! res))))

(deftest basic-atom
  (binding [urx/*enqueue!* uvec/alist-conj!]
    (let [runs  (running)
          start (! 0)
          sv    (!eager-rx @start)
          comp  (!eager-rx @sv (+ 2 @sv))
          c2    (!eager-rx (inc @comp))
          ct    (! 0)
          out   (! 0)
          res   (!eager-rx (uref/update! ct inc) @sv @c2 @comp)
          const (!run-rx (uref/set! out @res))]
      (is (= @ct 1) "constrain ran")
      (is (= @out 2))
      (uref/set! start 1)
      (flush! urx/global-queue)
      (is (= @out 3)) ; not correct; showing 2
      (is (<= 2 @ct 3))
      (dispose! const)
      (is (= (running) runs)))))

(deftest double-dependency
  (let [runs     (running)
        start    (! 0)
        c3-count (! 0)
        c1       (!eager-rx @start 1)
        c2       (!eager-rx @start)
        c3       (!rx (uref/update! c3-count inc)
                     (+ @c1 @c2))]
    (flush! urx/global-queue)
    (is (= @c3-count 0))
    (is (= @c3 1))
    (is (= @c3-count 1) "t1")
    (uref/update! start inc)
    (flush! urx/global-queue)
    (is (= @c3-count 2) "t2")
    (is (= @c3 2))
    (is (= @c3-count 2) "t3")
    (dispose! c3)
    (is (= (running) runs))))

(deftest test-from-reflex ; https://github.com/lynaghk/reflex
  (let [runs (running)]
    (let [*counter (! 0)
          *signal  (! "All I do is change")
          co (!run-rx @*signal (uref/update! *counter inc))]
      (is (= 1 @*counter) "Constraint run on init")
      (uref/set! *signal "foo")
      (flush! urx/global-queue)
      (is (= 2 @*counter)
          "Counter auto updated")
      (dispose! co))
    (let [*x  (! 0)
          *co (!rx (inc @*x))]
      (is (= 1 @*co) "CO has correct value on first deref")
      (uref/update! *x inc)
      (is (= 2 @*co) "CO auto-updates")
      (dispose! *co))
    (is (= (running) runs))))

(deftest test-unsubscribe
  (dotimes [x 10]
    (let [runs      (running)
          a         (! 0)
          a1        (!eager-rx (inc @a))
          a2        (!eager-rx @a)
          b-changed (! 0)
          c-changed (! 0)
          b         (!eager-rx
                      (uref/update! b-changed inc)
                      (inc @a1))
          c         (!eager-rx
                      (uref/update! c-changed inc)
                      (+ 10 @a2))
          res       (!run-rx (if (< @a2 1) @b @c))]
      (is (= @res (+ 2 @a)))
      (is (= @b-changed 1))
      (is (= @c-changed 0))

      (uref/set! a -1)
      (is (= @res (+ 2 @a)))
      (is (= @b-changed 2))
      (is (= @c-changed 0))

      (uref/set! a 2)
      (is (= @res (+ 10 @a)))
      (is (<= 2 @b-changed 3))
      (is (= @c-changed 1))

      (uref/set! a 3)
      (is (= @res (+ 10 @a)))
      (is (<= 2 @b-changed 3))
      (is (= @c-changed 2))

      (uref/set! a 3)
      (is (= @res (+ 10 @a)))
      (is (<= 2 @b-changed 3))
      (is (= @c-changed 2))

      (uref/set! a -1)
      (is (= @res (+ 2 @a)))
      (dispose! res)
      (is (= (running) runs)))))

(deftest maybe-broken
  (let [runs (running)]
    (let [runs (running)
          a    (! 0)
          b    (!eager-rx (inc @a))
          c    (!eager-rx (dec @a))
          d    (!eager-rx (str @b))
          res  (! 0)
          cs   (!run-rx (uref/set! res @d))]
      (is (= @res "1"))
      (dispose! cs))
    ;; should be broken according to https://github.com/lynaghk/reflex/issues/1
    ;; but isnt
    (let [a (! 0)
          b (!eager-rx (inc @a))
          c (!eager-rx (dec @a))
          d (!run-rx [@b @c])]
      (is (= @d [1 -1]))
      (dispose! d))
    (let [a (! 0)
          b (!eager-rx (inc @a))
          c (!eager-rx (dec @a))
          d (!run-rx [@b @c])
          res (! 0)]
      (is (= @d [1 -1]))
      (let [e (!run-rx (uref/set! res @d))]
        (is (= @res [1 -1]))
        (dispose! e))
      (dispose! d))
    (is (= (running) runs))))

(deftest test-dispose
  (binding [urx/*enqueue!* uvec/alist-conj!]
    (dotimes [x 10]
      (let [runs         (running)
            a            (! 0)
            disposed     (! nil)
            disposed-c   (! nil)
            disposed-cns (! nil)
            count-b      (! 0)
            b   (>!rx (fn [] (uref/update! count-b inc) (inc @a))
                        {:always-recompute? true
                         :on-dispose        (fn [r] (uref/set! disposed true))
                         :queue             urx/global-queue})
            c   (>!rx #(if (< @a 1) (inc @b) (dec @a))
                        {:always-recompute? true
                         :on-dispose        (fn [r] (uref/set! disposed-c true))
                         :queue             urx/global-queue})
            res (! nil)
            cns (>!rx #(uref/set! res @c)
                        {:on-dispose (fn [r] (uref/set! disposed-cns true))
                         :queue      urx/global-queue})]
        @cns
        (is (= @res 2))
        (is (= (+ 4 runs) (running)))
        (is (= @count-b 1))
        (uref/set! a -1)
        (flush! urx/global-queue)
        (is (= @res 1))
        (is (= @disposed nil))
        (is (= @count-b 2))
        (is (= (+ 4 runs) (running)) "still running")
        (uref/set! a 2)
        (flush! urx/global-queue)
        (is (= @res 1))
        (is (= @disposed true))
        (is (= (+ 2 runs) (running)) "less running count")

        (uref/set! disposed nil)
        (uref/set! a -1)
        (flush! urx/global-queue)
        ;; This fails sometimes on node. I have no idea why.
        (is (= 1 @res) "should be one again")
        (is (= @disposed nil))
        (uref/set! a 2)
        (flush! urx/global-queue)
        (is (= @res 1))
        (is (= @disposed true))
        (dispose! cns)
        (is (= @disposed-c true))
        (is (= @disposed-cns true))
        (is (= runs (running)))))))

(deftest test-add-dispose
  (dotimes [x 10]
    (let [runs         (running)
          a            (! 0)
          disposed     (! nil)
          disposed-c   (! nil)
          disposed-cns (! nil)
          count-b      (! 0)
          b            (!eager-rx (uref/update! count-b inc) (inc @a))
          c            (!eager-rx (if (< @a 1) (inc @b) (dec @a)))
          res          (! nil)
          cns          (!rx (uref/set! res @c))]
      (urx/add-on-dispose! b (fn [r]
                               (is (= r b))
                               (uref/set! disposed true)))
      (urx/add-on-dispose! c   (fn [r] (uref/set! disposed-c true)))
      (urx/add-on-dispose! cns (fn [r] (uref/set! disposed-cns true)))
      @cns
      (is (= @res 2))
      (is (= (+ 4 runs) (running)))
      (is (= @count-b 1))
      (uref/set! a -1)
      (flush! urx/global-queue)
      (is (= @res 1))
      (is (= @disposed nil))
      (is (= @count-b 2))
      (is (= (+ 4 runs) (running)) "still running")
      (uref/set! a 2)
      (flush! urx/global-queue)
      (is (= @res 1))
      (is (= @disposed true))
      (is (= (+ 2 runs) (running)) "less running count")

      (uref/set! disposed nil)
      (uref/set! a -1)
      (flush! urx/global-queue)
      (is (= 1 @res) "should be one again")
      (is (= @disposed nil))
      (uref/set! a 2)
      (flush! urx/global-queue)
      (is (= @res 1))
      (is (= @disposed true))
      (dispose! cns)
      (is (= @disposed-c true))
      (is (= @disposed-cns true))
      (is (= runs (running))))))

(deftest non-reactive-deref
  (let [runs (running)
        a    (! 0)
        b    (!eager-rx (+ 5 @a))]
    (is (= @b 5))
    (is (= runs (running)))

    (uref/set! a 1)
    (is (= @b 6))
    (is (= runs (running)))))

(deftest reset-in-reaction
  (let [runs  (running)
        state (! {})
        c1    (!eager-rx (get-in @state [:data :a]))
        c2    (!eager-rx (get-in @state [:data :b]))
        rxn   (!rx (let [cc1 @c1, cc2 @c2]
                    (uref/update! state assoc :derived (+ (or cc1 0) (or cc2 0)))
                    nil))]
    @rxn
    (is (= (:derived @state) 0))
    (uref/update! state assoc :data {:a 1 :b 2})
    (flush! urx/global-queue)
    (is (= (:derived @state) 3))
    (uref/update! state assoc :data {:a 11 :b 22})
    (flush! urx/global-queue)
    (is (= (:derived @state) 33))
    (dispose! rxn)
    (is (= runs (running)))))

(deftest exception-recover
  (let [runs  (running)
        state (! 1)
        count (! 0)
        r     (!run-rx
                (uref/update! count inc)
                (when (> @state 1) (throw (ex-info "oops" {}))))]
    (is (= @count 1))
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                 (do (uref/update! state inc)
                     (flush! urx/global-queue))))
    (is (= @count 2))
    (uref/update! state dec)
    (flush! urx/global-queue)
    (is (= @count 3))
    (dispose! r)
    (is (= runs (running)))))

(deftest exception-recover-indirect
  (let [runs  (running)
        state (! 1)
        count (! 0)
        ref   (!eager-rx (when (= @state 2)
                          (throw (ex-info "err" {}))))
        r (!run-rx
            (uref/update! count inc)
            @ref)]
    (is (= @count 1))
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                 (do (uref/update! state inc)
                     (flush! urx/global-queue))))
    (is (= @count 2))
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo) @ref))
    (uref/update! state inc)
    (flush! urx/global-queue)
    (is (= @count 3))
    (dispose! r)
    (is (= runs (running)))))

(deftest exception-side-effect
  (binding [urx/*enqueue!* uvec/alist-conj!]
    (let [runs   (running)
          state  (! {:val 1})
          rstate (!eager-rx @state)
          spy    (atom nil)
          r1     (!run-rx @rstate)
          r2     (let [val (!eager-rx (:val @rstate))]
                   (!run-rx
                     (reset! spy @val)
                     (is (some? @val))))
          r3     (!run-rx
                   (when (:error? @rstate)
                     (throw (ex-info "Error detected!" {}))))]
      (uref/update! state assoc :val 2)
      (flush! urx/global-queue)
      (uref/update! state assoc :error? true)
      (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                   (flush! urx/global-queue)))
      (flush! urx/global-queue)
      (flush! urx/global-queue)
      (dispose! r1)
      (dispose! r2)
      (dispose! r3)
      (is (= runs (running))))))

(deftest exception-reporting
  (binding [urx/*enqueue!* uvec/alist-conj!]
    (let [runs   (running)
          state  (! {:val 1})
          rstate (!eager-rx (:val @state))
          r1     (!run-rx
                   (when (= @rstate 13)
                     (throw (ex-info "fail" {}))))]
      (uref/update! state assoc :val 13)
      (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core/ExceptionInfo)
                   (flush! urx/global-queue)))
      (uref/update! state assoc :val 2)
      (flush! urx/global-queue)
      (dispose! r1)
      (is (= runs (running))))))

(deftest atom-with-meta
  (let [value      {:val 1}
        meta-value {:meta-val 1}
        state      (with-meta (! value) meta-value)]
    (is (= (meta state) meta-value))
    (is (= @state value))))

(deftest test-eager-vs-lazy-reaction
  (let [a         (! 123)
        b-ct      (atom 0)
        b         (!eager-rx (swap! b-ct inc) (+ @a  2))
        c-ct      (atom 0)
        c         (!eager-rx (swap! c-ct inc) (* @b -1))
        b-lazy-ct (atom 0)
        b-lazy    (!rx (swap! b-lazy-ct inc) (+ @a       2))
        c-lazy-ct (atom 0)
        c-lazy    (!rx (swap! c-lazy-ct inc) (* @b-lazy -1))]
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

      (uref/set! a 234)

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

      (uref/set! a 234) ; resetting to the same state

      @c-lazy
      (is= @b-lazy-ct 2)
      (is= @c-lazy-ct 1)

      (uref/set! a 123)

      @c-lazy
      (is= @b-lazy-ct 3)
      (is= @c-lazy-ct 2)
      @c-lazy
      (is= @b-lazy-ct 3)
      (is= @c-lazy-ct 2))))
