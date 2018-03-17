(ns quantum.core.core
  (:refer-clojure :exclude [get set])
  (:require [clojure.core              :as core]
            [clojure.spec.alpha        :as s]
    #?(:clj [clojure.core.specs.alpha  :as ss])
            [cuerdas.core              :as str+]
   #?(:clj  [environ.core              :as env])
            [quantum.untyped.core.core :as u]
            [quantum.untyped.core.vars
              :refer [defalias defaliases]]))

;; ===== Environment ===== ;;

(defaliases u lang #?(:clj pid))

;; ===== Compilation ===== ;;

(defalias u/externs?)

;; ===== quantum.core.system ===== ;;

(defalias u/*registered-components)

;; ===== Miscellaneous ===== ;;

(defaliases u >sentinel >object)

(def unchecked-inc-long
  #?(:clj  (fn [^long x] (unchecked-inc x))
     :cljs inc))

;; ===== Mutability/Effects ===== ;;

(defprotocol IValue
  (get [this])
  (set [this newv]))

#?(:clj
(defmacro with
  "Evaluates @expr, then @body, then returns @expr.
   For (side) effects."
  [expr & body]
  `(let [expr# ~expr]
    ~@body
    expr#)))
