(ns quantum.test.db.datomic.entities
  (:require [quantum.db.datomic.entities :refer :all]))

(defn test:attribute? [x])

(defn test:identifier? [x])
(defn test:lookup? [x])

(defn test:keyword->class-name [k])

(defn test:attr->constructor-sym [k])

(defn test:->ref-tos-constructors
  [opts])

#?(:clj
(defmacro test:defattribute
  [attr-k schema]))

#?(:clj
(defmacro test:defentity
  [attr-k & args]))

#?(:clj
(defmacro test:declare-entity
  [entity-k]))

(defn test:transact-schemas! [])