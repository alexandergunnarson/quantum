(ns quantum.test.core.vars
  (:require [quantum.core.vars :as ns]))

; ============ DECLARATION ============

(defn test:defalias
  ([name orig])
  ([name orig doc]))

(defn test:defaliases
  [ns- & names])

(defn test:var-name
  [v])

#?(:clj
(defn test:alias-var
  [sym var-0]))

#?(:clj
(defmacro test:defonce
  [name & sigs]))

#?(:clj
(defmacro test:def-
  [sym v]))

#?(:clj
(defmacro test:defmacro-
  [name & decls]))

; ============ MANIPULATION + OTHER ============

(defn test:reset-var!
  [var-0 val-f])

(defn test:swap-var!
  ([var-0 f])
  ([var-0 f & args]))

#?(:clj
(defn test:clear-vars!
  [& vars]))

#?(:clj
(defn test:var-name [v]))

#?(:clj
(defn test:alias-var
  [sym var-0]))

#?(:clj
(defn test:alias-ns
  [ns-name-]))

#?(:clj
(defn test:defs
  [& {:as vars}]))

#?(:clj
(defn test:defs-
  [& {:as vars}]))

#?(:clj
(defn test:namespace-exists?
  [ns-sym]))

(defn test:unqualify [sym])