(ns quantum.test.core.collections.generative
  (:require [quantum.core.collections.generative :refer :all]))

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