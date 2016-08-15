(ns quantum.test.core.string.format
  (:require [quantum.core.string.format :as ns]))

(defn test:replace [s pre post])

(defn test:->regexp
  ([s])
  ([s flags]))

; ====== CASES ======

(defn test:->lower-case [x])

(defn test:->upper-case [x])

(defn test:capitalize [s])

(defn test:capitalize-each-word [string])

(defn test:un-camelcase
  [sym])

(defn test:separate-camel-humps
  [value])

(defn test:->title-case
  [value])

(defn test:->camel-case [value])

(defn test:->capital-camel-case
  [value])

(defn test:->snake-case
  [value])

(defn test:->spear-case
  [value])

(defn test:->human-case
  [s])