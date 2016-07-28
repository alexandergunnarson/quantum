; Or, "nested"
(ns quantum.core.collections.inner
  (:require [quantum.core.data.map :as map]
            [quantum.core.data.set :as set]))

(defn test:nest-keys
  ([m nskv])
  ([m nskv ex]))

(defn test:unnest-keys
  ([m nskv])
  ([m nskv ex]))

(defn test:key-paths
  ([m])
  ([m max])
  ([m max arr]))

(defn test:keys-nested
  ([m]))

(defn test:merge-nested
  ([])
  ([m])
  ([m1 m2])
  ([m1 m2 & ms]))
