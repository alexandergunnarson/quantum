(ns quantum.test.core.core
  (:require [quantum.core.core :as ns]))

; ===== TYPE PREDICATES =====

(defn test:atom? [x])

(defn test:boolean? [x])

(defn test:seqable? [x])

(defn test:editable? [coll])

; ===== REFS AND ATOMS =====

(defn test:deref* [a])

(defn test:lens [x getter])

(defn test:cursor
  [x getter & [setter]])

(defn test:seq-equals [a b])

; ===== TYPE =====

(defn test:unchecked-inc-long [x])

(defn test:with
  [expr & body])

(defn test:name+ [x])

(defn test:ensure-println [& args])

(defn test:js-println [& args])