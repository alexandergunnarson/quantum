(ns quantum.test.core.java
  (:require [quantum.core.java :as ns]))

(defn test:get-by-key [object info-type & args])
  
(defn test:methods-names [object])
(defn test:load-deps [deps])

(defn test:invoke
  [instance method & params])

(defn test:field [instance field])

(defn test:invoke*
  [method instance & params])