(ns quantum.test.numeric.statistics.core
  (:require
    [quantum.core.test
      :refer [deftest is testing]]
    [quantum.numeric.statistics.core :as ns]))

(deftest test:mode
  (is (= 1 (ns/mode [0 :a "a" 1 :b "b" :c 3 2 1 :a 1]))))
