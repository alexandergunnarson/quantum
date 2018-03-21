(ns quantum.test.numeric.core
  (:require
    [quantum.numeric.core          :as ns]
    [quantum.core.nondeterministic :as rand]
    [quantum.core.fn
      :refer [<-]]
    [quantum.core.test
      :refer [deftest is testing]]))

#_(defn test:quartic-root [a b c d])

(defn test:sigma [set- step-fn])

(defn test:pi* [set- step-fn])

(defn test:factors [n])

(defn test:lfactors [n])

(defn test:gcd
  ([a b])
  ([a b & args]))

(deftest test:normalize-sum-to
  (dotimes [i 100] ; doubles might be off by a little bit — TODO have tests for doubles
    (let [target-sum (rationalize (rand))]
      (is (= (->> (repeatedly (rand/int-between 5 20) #(rationalize (rand)))
                  (<- (ns/normalize-sum-to target-sum))
                  (apply +))
             target-sum)))))

(deftest test:normalize
  (is (= [-8/9 -1 -13/18 -17/18 -7/9 -5/6 13/18 -5/6 -7/9 1 -5/6]
         (ns/normalize [0 -2 3 -1 2 1 29 1 2 34 1] -1 1)))
  (is (= [1/18 0 5/36 1/36 1/9 1/12 31/36 1/12 1/9 1 1/12]
         (ns/normalize [0 -2 3 -1 2 1 29 1 2 34 1]))))
