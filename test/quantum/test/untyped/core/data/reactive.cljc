(ns quantum.test.untyped.core.data.reactive
  (:require
    [quantum.untyped.core.test
      :refer [deftest is testing]]
    [quantum.untyped.core.data.reactive :as self
      :refer [ratom rx]]))

(defn test-perf []
  ;; (set! debug? false) ; yes but we need to think about CLJ
  (dotimes [_ 10]
    (let [a   (self/ratom 0)
          f   (fn [] (quot (long @a) 10))
          q   (self/alist)
          mid (self/>rx f {:queue q})
          res (self/>track! (fn [] (inc (long @mid))) [] {:queue q})]
      @res
      (time (dotimes [_ 100000] ; ~70ms per 100K
              (swap! a inc)
              (@#'self/flush! q)))
      (self/dispose res))))

(deftest basic-ratom
  (binding [self/*enqueue!* @#'self/alist-conj!]
    (let [runs  @@#'self/*running
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
      (self/flush! self/global-queue)
      (is (= @out 3)) ; not correct; showing 2
      (is (<= 2 @ct 3))
      (self/dispose const)
      (is (= @@#'self/*running runs)))))

(deftest double-dependency
  (let [runs @@#'self/*running
        start (ratom 0)
        c3-count (ratom 0)
        c1 (rx @start 1)
        c2 (rx @start)
        c3 (self/>rx
            (fn []
              (swap! c3-count inc)
              (+ @c1 @c2))
            {:auto-run true :queue self/global-queue})]
    (self/flush! self/global-queue)
    (is (= @c3-count 0))
    (is (= @c3 1))
    (is (= @c3-count 1) "t1")
    (swap! start inc)
    (self/flush! self/global-queue)
    (is (= @c3-count 2) "t2")
    (is (= @c3 2))
    (is (= @c3-count 2) "t3")
    (self/dispose c3)
    (is (= @@#'self/*running runs))))
