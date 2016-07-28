(ns quantum.test.core.data.set
  (:require [quantum.core.data.set :refer :all]))

#?(:clj (defn test:hash-set? [x]))

; ============ PREDICATES ============

(defn test:xset?
  [fn-key set1 set2])

(defn test:subset?          [a b])
(defn test:superset?        [a b])
(defn test:proper-subset?   [a b])
(defn test:proper-superset? [a b])

; ============ OPERATIONS ============

(defn test:union
  ([])
  ([s0])
  ([s0 s1])
  ([s0 s1 & ss]))

(defn test:punion
  ([])
  ([s0])
  ([s0 s1])
  ([s0 s1 & ss]))