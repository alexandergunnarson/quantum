(ns quantum.test.core.data.vector
  (:require [quantum.core.data.vector :as ns]))

(defn test:catvec
  ([])
  ([a])
  ([a b])
  ([a b c])
  ([a b c d])
  ([a b c d e])
  ([a b c d e f])
  ([a b c d e f & more]))

(defn test:subvec+
  [coll a b])

(defn test:vector+? [x])