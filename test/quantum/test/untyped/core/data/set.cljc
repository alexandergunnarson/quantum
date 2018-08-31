(ns quantum.untyped.test.core.data.set
  (:require
    [quantum.untyped.core.compare  :as ucomp]
    [quantum.untyped.core.data.set :as uset]
    [quantum.untyped.core.test :as test
      :refer [deftest is is= testing]]))

#?(:clj
(defmacro test-comparison|set
  "Performs a `t/compare` on `a` and `b`, ensuring that their relationship is symmetric, and that
   the inputs are internally commutative if applicable (e.g. if `a` is an `AndType`, ensures that
   it is commutative).
   The basis comparison is the first input."
  [c #_ucomp/comparisons a #_set? b #_set?]
  `(let [c# ~c, a# ~a, b# ~b]
     ;; Symmetry
     (is= c#                (uset/compare a# b#))
     (is= (ucomp/invert c#) (uset/compare b# a#)))))

(deftest test|set
  (testing "< , >"
    (test-comparison|set -1 #{1}     #{1 2})
    (test-comparison|set -1 #{1 2}   #{1 2 3}))
  (testing "="
    (test-comparison|set  0 #{}      #{})
    (test-comparison|set  0 #{1}     #{1})
    (test-comparison|set  0 #{1 2}   #{1 2}))
  (testing "><"
    (test-comparison|set  2 #{1 2}   #{1 3})
    (test-comparison|set  2 #{1 2}   #{1 3})
    (test-comparison|set  2 #{1 2 3} #{1 4}))
  (testing "<>"
    (test-comparison|set  3 #{}      #{1})
    (test-comparison|set  3 #{}      #{1 2})
    (test-comparison|set  3 #{1}     #{2})
    (test-comparison|set  3 #{3}     #{1 2})))
