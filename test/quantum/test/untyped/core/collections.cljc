(ns quantum.test.untyped.core.collections
  (:require
    [quantum.untyped.core.collections :as self]
    [quantum.untyped.core.fn
      :refer [aritoid fn']]
    [quantum.untyped.core.test
      :refer [deftest is is= testing]]))

(deftest test|flatten
  (is= (self/flatten [[0 1] [2 3 4]] 0)
       [[0 1] [2 3 4]])

  (is= (self/flatten [[0 1] [2 3 4]] 1)
       [0 1 2 3 4])

  (is= (self/flatten [[[0 1]] [[2 3 4]]] 2)
       [0 1 2 3 4]))

(def conj|map (aritoid (fn' {}) identity conj))

(def conj|!vec (aritoid (fn [] (transient [])) persistent! conj!))

(deftest test|>combinatoric-tree
  (let [in '[[0 [a b a]]
             [1 [a b c]]
             [2 [a c d]]
             [3 [c b a]]
             [4 [c c a]]
             [5 [d a a]]]
        vec-result '[[a [[b [[a 0]
                             [c 1]]]
                         [c [[d 2]]]]]
                     [c [[b [[a 3]]]
                         [c [[a 4]]]]]
                     [d [[a [[a 5]]]]]]]
    (is= (self/>combinatoric-tree 3 in) vec-result)
    (testing "All arities of the combinatory fns are exercised"
      (is= (self/>combinatoric-tree 3 = conj|!vec conj|!vec
             (fn ([] (transient []))
                 ([ret] (persistent! ret))
                 ([ret [k [x*]]] (conj! ret [x* k])))
             in)))
    (is= (self/>combinatoric-tree 3 = conj|map conj|map
           (fn ([] {}) ([ret] ret) ([ret [k [x*]]] (assoc ret x* k))) in)
         '{a {b {a 0
                 c 1}
              c {d 2}}
           c {b {a 3}
              c {a 4}}
           d {a {a 5}}})
    (is= (self/>combinatoric-tree 3 = conj|map conj
           (fn ([] []) ([ret] ret) ([ret [k [x*]]] (conj ret [x* k]))) in)
         '{a {b [[a 0]
                 [c 1]]
              c [[d 2]]}
           c {b [[a 3]]
              c [[a 4]]}
           d {a [[a 5]]}})))
