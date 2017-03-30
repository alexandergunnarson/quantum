(ns quantum.core.core
  (:refer-clojure :exclude [seqable? boolean? get set])
  (:require [clojure.core :as core
             #?@(:cljs [:refer [IDeref IAtom]])]
            [cuerdas.core :as str+]
   #?(:clj  [environ.core :as env]))
  #?(:clj (:import [clojure.lang IDeref IAtom])))

#?(:clj (do (in-ns 'clojure.core) (defn require-macros [& args]) (in-ns 'quantum.core.core)))

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

; ===== TYPE PREDICATES =====

(defn atom?    [x] (#?(:clj  instance?
                       :cljs satisfies?)
                    IAtom x))

(defn boolean? [x] #?(:clj  (instance? Boolean x)
                      :cljs (or (true? x) (false? x))))

(defn regex? [x] (instance? #?(:clj java.util.regex.Pattern :cljs js/RegExp) x))

#?(:clj  (defn seqable?
           "Returns true if (seq x) will succeed, false otherwise."
           {:from "clojure.contrib.core"}
           [x]
           (or (seq? x)
               (instance? clojure.lang.Seqable x)
               (nil? x)
               (instance? Iterable x)
               (-> x class .isArray)
               (string? x)
               (instance? java.util.Map x)))
   :cljs (def seqable? core/seqable?))

(defn editable? [coll]
  #?(:clj  (instance? clojure.lang.IEditableCollection coll)
     :cljs (satisfies? cljs.core.IEditableCollection coll)))

; ===== REFS AND ATOMS =====

(defn seq-equals [a b]
  (boolean
    (when (or (sequential? b) #?(:clj  (instance? java.util.List b)
                                 :cljs (list? b)))
      (loop [a (seq a) b (seq b)]
        (when (= (nil? a) (nil? b))
          (or
            (nil? a)
            (when (= (first a) (first b))
              (recur (next a) (next b)))))))))

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

(defn name+ [x]
  (cond   (nil? x)
          x
#?@(:clj [(class? x)
          (.getName ^Class x)])
          :else (name x)))

(defn simple-keyword?    [x] (and (symbol?  x) (nil? (namespace x))))
(defn qualified-keyword? [x] (and (keyword? x)       (namespace x)))
(defn simple-symbol?     [x] (and (symbol?  x) (nil? (namespace x))))
(defn qualified-symbol?  [x] (and (keyword? x)       (namespace x)))

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

(defn quote-map-base [make-map kw-modifier ks]
  `(~make-map
     ~@(->> ks
            (map #(vector (list 'quote (kw-modifier %)) %))
            (apply concat))))

#?(:clj
(defmacro istr
  "'Interpolated string.' Accepts one or more strings; emits a `str` invocation that
  concatenates the string data and evaluated expressions contained
  within that argument.  Evaluation is controlled using ~{} and ~()
  forms. The former is used for simple value replacement using
  clojure.core/str; the latter can be used to embed the results of
  arbitrary function invocation into the produced string.
  Examples:
      user=> (def v 30.5)
      #'user/v
      user=> (istr \"This trial required ~{v}ml of solution.\")
      \"This trial required 30.5ml of solution.\"
      user=> (istr \"There are ~(int v) days in November.\")
      \"There are 30 days in November.\"
      user=> (def m {:a [1 2 3]})
      #'user/m
      user=> (istr \"The total for your order is $~(->> m :a (apply +)).\")
      \"The total for your order is $6.\"
      user=> (istr \"Just split a long interpolated string up into ~(-> m :a (get 0)), \"
               \"~(-> m :a (get 1)), or even ~(-> m :a (get 2)) separate strings \"
               \"if you don't want a << expression to end up being e.g. ~(* 4 (int v)) \"
               \"columns wide.\")
      \"Just split a long interpolated string up into 1, 2, or even 3 separate strings if you don't want a << expression to end up being e.g. 120 columns wide.\"
  Note that quotes surrounding string literals within ~() forms must be
  escaped."
  [& args] `(str+/istr ~@args)))
