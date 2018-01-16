(ns quantum.core.core
  (:refer-clojure :exclude [seqable? boolean? get set])
  (:require [clojure.core              :as core
             #?@(:cljs [:refer [IDeref IAtom]])]
            [clojure.spec.alpha        :as s]
    #?(:clj [clojure.core.specs.alpha  :as ss])
            [cuerdas.core              :as str+]
   #?(:clj  [environ.core              :as env])
            [quantum.core.untyped.core :as qcore]))

#?(:clj
(defn pid []
  (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
       (.getName))))

#?(:clj
(binding [*out* *err*]
  (when (:print-pid?          env/env) (println "PID:" (pid)))
  (when (:print-java-version? env/env) (println "Java version:" (System/getProperty "java.version")))
  (flush)))

(def lang #?(:clj :clj :cljs :cljs))

(defonce debug?   (atom false))
(defonce externs? (atom true ))

(defonce registered-components (atom {}))

(defn ->sentinel [] #?(:clj (Object.) :cljs #js {}))
(defn ->object   [] #?(:clj (Object.) :cljs #js {}))

(def seq= qcore/seq=)

(def has? (comp not empty?)) ; TODO fix this performance-wise

; ===== PREDICATES ===== ;

(def val?     qcore/val?)
(def boolean? qcore/boolean?)
(def seqable? qcore/seqable?)
(def regex?   qcore/regex?)

; ===== TYPE =====

(def unchecked-inc-long
  #?(:clj  (fn [^long x] (unchecked-inc x))
     :cljs inc))

(defprotocol IValue
  (get [this])
  (set [this newv]))

#?(:clj
(defmacro with
  "Evaluates @expr, then @body, then returns @expr.
   For side effects."
  [expr & body]
  `(let [expr# ~expr]
    ~@body
    expr#)))

(defn str->integer [s]
  (assert (string? s) {:s s})
  #?(:clj  (Long/parseLong ^String s)
     :cljs (js/parseInt            s)))

; Nested |let-mutable| :
    ; ClassCastException java.lang.Long cannot be cast to proteus.Containers$L

#?(:cljs
(defn ensure-println [& args]
  (enable-console-print!)
  (apply println args)))

(defn js-println [& args]
  (print "\n/* " )
  (apply println args)
  (println "*/"))

(defn quote-map-base [kw-modifier ks & [no-quote?]]
  (->> ks
       (map #(vector (cond->> (kw-modifier %) (not no-quote?) (list 'quote)) %))
       (apply concat)))

(defn >keyword [x]
  (cond (keyword? x) x
        (symbol?  x) (keyword (namespace x) (name x))
        :else        (-> x str keyword)))

#?(:clj (defmacro kw-map    [& ks] (list* `hash-map (quote-map-base >keyword ks))))
#?(:clj (defmacro quote-map [& ks] (list* `hash-map (quote-map-base identity  ks))))

#?(:clj (defmacro istr [& args] `(qcore/istr ~@args)))
