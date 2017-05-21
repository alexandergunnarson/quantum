(ns quantum.test.numeric.statistics.core
  (:require
    [quantum.core.test
      :refer [deftest is testing]]
    [quantum.numeric.statistics.core :as ns]))

(deftest test:mode
  (is (= 1 (ns/mode [0 :a "a" 1 :b "b" :c 3 2 1 :a 1]))))

(deftest test:modes
  (is (= #{1}    (ns/modes [0 :a "a" 1 :b "b" :c 3 2 1 :a 1])))
  (is (= #{1 :a} (ns/modes [0 :a "a" 1 :b "b" :c 3 2 1 :a 1 :a])))
  (is (= #{0 :a} (ns/modes [0 :a])))
  (is (= #{0}    (ns/modes [0])))
  (is (= #{}     (ns/modes []))))
