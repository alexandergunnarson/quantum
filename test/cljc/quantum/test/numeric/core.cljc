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

(deftest normalize-sum-to
  (dotimes [i 100] ; doubles might be off by a little bit — TODO have tests for doubles
    (let [target-sum (rationalize (rand))]
      (is (= (->> (repeatedly (rand/rand-int-between 5 20) #(rationalize (rand)))
                  (<- ns/normalize-sum-to target-sum)
                  (apply +))
             target-sum)))))
