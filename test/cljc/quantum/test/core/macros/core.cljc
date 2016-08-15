(ns quantum.test.core.macros.core
  (:require [quantum.core.macros.core :as ns]))

; ===== ENVIRONMENT =====

(defn test:cljs-env?
  [env])

(defn test:if-cljs
  ([env then else]))

(defn test:when-cljs
  ([env then])))

(defn test:context []))

; ===== LOCAL EVAL & RESOLVE =====

(defn test:eval-local
  ([context expr]))
 
(defn test:let-eval [expr])

(defn test:tag
  [obj tag-])

(defn test:resolve-local
  [sym])

(defn test:compile-if
  [exp then else])

; ===== SYMBOLS =====

(defn test:hint-meta [sym hint])

; ===== MACROEXPANSION ====

(defn test:macroexpand-1 [form & [impl & args]])

(defn test:macroexpand-all
  [x & [impl]])

; ===== MACRO CREATION HELPERS =====

(defn test:name-with-attrs
  [name macro-args])

; ===== USEFUL =====

(defn test:defmalias
  ([name orig-sym])
  ([name clj-sym cljs-sym]))

; ------------- SYNTAX QUOTE; QUOTE+ -------------

(defn test:unquote-replacement
  [sym-map quoted-form])

(defn test:quote+
  [form])