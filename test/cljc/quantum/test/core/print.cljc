(ns quantum.test.core.print
  (:require [quantum.core.print :as ns]))

(defn test:!
  ([obj])
  ([obj & objs]))

(defn test:representative-coll
  [source-0])

(defn test:!* [obj] (-> obj representative-coll !))

(defn test:pr-attrs
  [obj])