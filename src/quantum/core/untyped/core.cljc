(ns quantum.core.untyped.core
  (:refer-clojure :exclude [seqable? boolean?])
  (:require [cuerdas.core :as str+]))

(defn ->sentinel [] #?(:clj (Object.) :cljs #js {}))

(defn quote-map-base [kw-modifier ks & [no-quote?]]
  (->> ks
       (map #(vector (cond->> (kw-modifier %) (not no-quote?) (list 'quote)) %))
       (apply concat)))

(defn >keyword [x]
  (cond (keyword? x) x
        (symbol?  x) (keyword (namespace x) (name x))
        :else        (-> x str keyword)))

#?(:clj (defmacro kw-map [& ks] (list* `hash-map (quote-map-base >keyword ks))))

; ===== TYPE PREDICATES =====

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

(def val? some?)

(defn boolean? [x] #?(:clj  (instance? Boolean x)
                      :cljs (or (true? x) (false? x))))

(defn lookup? [x]
  #?(:clj  (instance? clojure.lang.ILookup x)
     :cljs (satisfies? ILookup x)))

#?(:clj
(defn protocol? [x]
  (and (lookup? x) (-> x (get :on-interface) class?))))

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

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

#?(:clj (defn metable? [x] (instance? clojure.lang.IMeta x)))

; ===== COLLECTIONS =====

(defn seq=
  ([a b] (seq= a b =))
  ([a b eq-f]
  (boolean
    (when (or (sequential? b) #?(:clj  (instance? java.util.List b)
                                 :cljs (list? b)))
      (loop [a (seq a) b (seq b)]
        (when (= (nil? a) (nil? b))
          (or (nil? a)
              (when (eq-f (first a) (first b))
                (recur (next a) (next b))))))))))

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

(defn code=
  "Ensures that two pieces of code are equivalent.
   This means ensuring that seqs, vectors, and maps are only allowed to be compared with
   each other, and that metadata is equivalent."
  [code0 code1]
  (if (metable? code0)
      (and (metable? code1)
           (= (meta code0) (meta code1))
           (cond (seq?    code0) (and (seq?    code1) (seq=      code0       code1  code=))
                 (vector? code0) (and (vector? code1) (seq= (seq code0) (seq code1) code=))
                 (map?    code0) (and (map?    code1) (seq= (seq code0) (seq code1) code=))
                 :else           (= code0 code1)))
      (and (not (metable? code1))
           (= code0 code1))))
