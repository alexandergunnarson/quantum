(ns quantum.test.core.loops
  (:require [quantum.core.loops :as ns]))
  
(defn test:until [pred-expr & body])

(defn test:reduce-
  ([f coll])
  ([f ret coll]))

(defn test:reduce*
  ([lang f coll])
  ([lang f ret coll]))

(defn test:reduce [& args])

(defn test:reducei*
  [should-extern? f ret-i coll & args])

(defn test:reducei-
  [f ret coll])

(defn test:reducei
  [f ret coll])
 
(defn test:reduce-2
  [func init coll])

(defn test:while-recur
  [obj-0 pred func])

(defn test:dos
  [args])

(defn test:lfor [& args])

(defn test:doseq
  [bindings & body])

(defn test:doseqi
  [bindings & body])

(defn test:for
  [bindings & body])
 
(defn test:fori
  [bindings & body])

(defn test:seq-loop
  [bindings & exprs])

(defn test:ifor
  [[sym val-0 pred val-n+1] & body])

(defn test:lfor [& args])

(defn test:dotimes
  [bindings & body])

(defn test:while-let
  [[form test] & body])

(defn test:each
  [f coll])