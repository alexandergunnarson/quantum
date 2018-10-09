(ns quantum.test.untyped.core.collections
  (:require
    [quantum.untyped.core.collections :as self]
    [quantum.untyped.core.test
      :refer [deftest is is= testing]]))

(deftest test|flatten
  (is= (self/flatten [[0 1] [2 3 4]] 0)
       [[0 1] [2 3 4]])

  (is= (self/flatten [[0 1] [2 3 4]] 1)
       [0 1 2 3 4])

  (is= (self/flatten [[[0 1]] [[2 3 4]]] 2)
       [0 1 2 3 4]))

(def conj|map (fn ([] {}) ([x] x) ([ret x] (conj ret x))))

(deftest test|>combinatoric-tree
  (let [in '[[0 [a b a]]
             [1 [a b c]]
             [2 [a c d]]
             [3 [c b a]]
             [4 [c c a]]
             [5 [d a a]]]]
    (is= (self/>combinatoric-tree 3 in)
         '[[a [[b [[a 0]
                   [c 1]]]
               [c [[d 2]]]]]
           [c [[b [[a 3]]]
               [c [[a 4]]]]]
           [d [[a [[a 5]]]]]])
    (is= (self/>combinatoric-tree 3 = conj|map conj|map in)
         '{a {c {d 2}
           b {a 0, c 1}}
           c {c {a 4}
              b {a 3}}
           d {a {a 5}}})))
