(ns quantum.test.core.type.predicates
  (:require [quantum.core.type.predicates :as ns]))

(defn test:regex? [obj])
(defn test:derefable? [obj])

(defn test:map-entry? [x])

(defn test:listy? [obj] (seq? obj))