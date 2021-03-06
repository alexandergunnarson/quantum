(ns quantum.test.untyped.core.collections
  (:require
    [quantum.core.test
      :refer [deftest is is= testing]]
    [quantum.untyped.core.collections :as this]))

(deftest test:flatten
  (is= (this/flatten [[0 1] [2 3 4]] 0)
       [[0 1] [2 3 4]])

  (is= (this/flatten [[0 1] [2 3 4]] 1)
       [0 1 2 3 4])

  (is= (this/flatten [[[0 1]] [[2 3 4]]] 2)
       [0 1 2 3 4]))
