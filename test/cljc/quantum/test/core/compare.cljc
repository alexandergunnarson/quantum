(ns quantum.test.core.compare
  (:require [quantum.core.compare :as ns]))

(defn test:<
  ([x])
  ([x y])
  ([x y & more]))

(defn test:>
  ([x])
  ([x y])
  ([x y & more]))

(defn test:<=
  ([x] )
  ([x y])
  ([x y & more]))

(defn test:>=
  ([x])
  ([x y])
  ([x y & more]))

(defn test:max
  ([x])
  ([x y])
  ([x y & more]))

(defn test:min
  ([x])
  ([x y])
  ([x y & more]))

(defn test:min-key
  ([k x])
  ([k x y])
  ([k x y & more]))

(defn test:max-key
  ([k x])
  ([k x y])
  ([k x y & more]))

(defn test:compare-bytes-lexicographically
  [a b]))

(defn test:extreme-comparator [comparator-n])