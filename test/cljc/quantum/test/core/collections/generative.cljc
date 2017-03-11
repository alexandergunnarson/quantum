(ns quantum.test.core.collections.generative
  (:require
    [quantum.core.collections.generative :as ns]
    [quantum.core.test
      :refer [deftest is testing]]))

; ===== REPEAT =====

(defn test:repeat
  ([obj])
  ([n obj]))

; ===== REPEATEDLY ===== ;

#?(:clj
(defmacro test:repeatedly-into
  [coll n & body]))

#?(:clj
(defmacro test:repeatedly
  ([n arg1 & body])))

; ===== RANGE ===== ;

(defn test:lrrange
  ([] )
  ([a])
  ([a b]))

(defn test:lrange
  ([])
  ([a])
  ([a b]))

(defn test:rrange
  ([] )
  ([a] )
  ([a b]))

(defn test:range
  ([])
  ([a])
  ([a b]))

#?(:clj
(deftest test:!range:longs
  (is (= (vec (ns/!range:longs  0   )) (range  0   )))
  (is (= (vec (ns/!range:longs  1   )) (range  1   )))
  (is (= (vec (ns/!range:longs  4   )) (range  4   )))
  (is (= (vec (ns/!range:longs  0 5 )) (range  0 5 )))
  (is (= (vec (ns/!range:longs -1 5 )) (range -1 5 )))
  (is (= (vec (ns/!range:longs -12 5)) (range -12 5)))
  (is (= (vec (ns/!range:longs -12 5)) (range -12 5)))
  (is (= (vec (ns/!range:longs 5 -12)) (range 5 -12)))))
