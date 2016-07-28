(ns quantum.test.core.data.map
  (:require [quantum.core.data.map :refer :all]))

(defn test:map-entry [k v])

(defn test:map-entry-seq [args])

#?(:clj (defn test:hash-map? [x]))

(defn test:merge
  ([])
  ([m0])
  ([m0 m1])
  ([m0 m1 & ms]))

(defn test:pmerge
  ([])
  ([m0])
  ([m0 m1])
  ([m0 m1 & ms]))