(ns quantum.test.core.macros.optimization
  (:require [quantum.core.macros.optimization :as ns]))

; ===== EXTERN =====

(defn extern? [x])

(defn test:extern* [ns- [spec-sym quoted-obj & extra-args]])

(defn test:extern- [obj])

; ===== MISCELLANEOUS =====

(defn test:identity* [obj])

(defn test:inline-replace [obj])