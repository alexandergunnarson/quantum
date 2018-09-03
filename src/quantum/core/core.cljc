(ns quantum.core.core
  (:refer-clojure :exclude
    [get set])
  (:require [clojure.core              :as core]
            [clojure.spec.alpha        :as s]
    #?(:clj [clojure.core.specs.alpha  :as ss])
            [cuerdas.core              :as str+]
    #?(:clj [environ.core              :as env])
            ;; TODO TYPED move to quantum.core.type
          #_[quantum.core.type         :as t
              :refer [declare-fnt defnt defmacrot deft]]
            [quantum.untyped.core.core :as u]
            [quantum.untyped.core.defnt
              :refer [defnt]]
            ;; TODO TYPED move to quantum.core.type
            [quantum.untyped.core.type :as t
              :refer [?]]
            [quantum.untyped.core.vars
              :refer [defalias defaliases]]))

;; ===== Environment ===== ;;

(deft lang t/keyword? "The language this code is compiled under" u/lang)

#?(:clj
(defnt pid [> (? t/string?)]
  (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
       (.getName))))

;; ===== Compilation ===== ;;

;; TODO TYPED
(defalias u/externs?)

;; ===== quantum.core.system ===== ;;

;; TODO TYPED
;; TODO move
(defalias u/*registered-components)

;; ===== Miscellaneous ===== ;;

;; TODO move
(defnt >sentinel [> t/object?] #?(:clj (Object.) :cljs #js {}))
(defalias >object >sentinel)

;; ===== Mutability/Effects ===== ;;

;; TODO TYPED
;; TODO move?
(defprotocol IValue
  (get [this])
  (set [this newv]))

#_(do (declare-fnt get [this _])
      (declare-fnt set [this _, newv _]))

;; TODO TYPED
;; TODO move?
#?(:clj
(defmacro with
  "Evaluates @expr, then @body, then returns @expr.
   For (side) effects."
  [expr & body]
  `(let [expr# ~expr] ~@body expr#)))

#_(:clj
(defmacrot with
  "Evaluates @expr, then @body, then returns @expr.
   For (side) effects."
  [expr t/form? & body (? (t/seq-of t/form?))]
  `(let [expr# ~expr] ~@body expr#)))
