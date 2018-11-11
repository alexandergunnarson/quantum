(ns quantum.test.core.logic
  (:require
    [quantum.untyped.core.numeric.combinatorics :as combo]
    [quantum.core.logic :as ns]
    [quantum.core.test  :as test
      :refer [deftest is]]))

#?(:clj
(deftest test|some-but-not-more-than-n
  (doseq [n [1]] ; TODO test more
    (doseq [args-n (range 5)]
      (doseq [args (combo/selections #{true false} args-n)]
        (is (= (boolean (eval `(ns/some-but-not-more-than-n ~n ~@args)))
               (boolean (eval `(and (or ~@args) (not (and ~@args))))))))))))

(deftest test|default
  (let [a (atom 0)]
    (ns/default nil   (reset! a 1))
    (is (= @a 1))
    (ns/default true  (reset! a 2))
    (is (= @a 1))
    (ns/default false (reset! a 3))
    (is (= @a 1))
    (ns/default 1     (reset! a 4))
    (is (= @a 1))
    (ns/default nil   (reset! a 5))
    (is (= @a 5))))
