(ns quantum.untyped.core.type.predicates
  "For type predicates that are not yet turned into specs.
   TODO excise and place in `quantum.untyped.core.type`."
  (:refer-clojure :exclude
    [any? array? boolean? double? ident? pos-int? qualified-keyword? seqable? simple-symbol?])
  (:require
    [clojure.core   :as core]
#?(:clj
    [clojure.future :as fcore])
    [quantum.untyped.core.core :as ucore]
    [quantum.untyped.core.vars
      :refer [defalias defaliases]]))

(ucore/log-this-ns)

;; The reason we use `resolve` and `eval` here is that currently we need to prefer built-in impls
;; where possible in order to leverage their generators

#?(:clj  (eval `(defalias ~(if (resolve `fcore/any?)
                               `fcore/any?
                               `core/any?)))
   :cljs (defalias core/any?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/boolean?)
                               `fcore/boolean?
                               `core/boolean?)))
   :cljs (defalias core/boolean?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/double?)
                               `fcore/double?
                               `core/double?)))
   :cljs (defalias core/double?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/ident?)
                               `fcore/ident?
                               `core/ident?)))
   :cljs (defalias core/ident?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/pos-int?)
                               `fcore/pos-int?
                               `core/pos-int?)))
   :cljs (defalias core/pos-int?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/qualified-keyword?)
                               `fcore/qualified-keyword?
                               `core/qualified-keyword?)))
   :cljs (defalias core/qualified-keyword?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/simple-symbol?)
                               `fcore/simple-symbol?
                               `core/simple-symbol?)))
   :cljs (defalias core/simple-symbol?))

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

(def val? some?)

(defn lookup? [x]
  #?(:clj  (instance? clojure.lang.ILookup x)
     :cljs (satisfies? ILookup x)))

(defn protocol? [x]
  #?(:clj  (and (lookup? x) (-> x (get :on-interface) class?))
           ;; Unfortunately there's no better check in CLJS, at least as of 03/18/2018
     :cljs (and (fn? x) (= (str x) "function (){}"))))

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
  #?(:clj  (instance?  clojure.lang.IEditableCollection coll)
     :cljs (satisfies? cljs.core.IEditableCollection    coll)))

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

(defaliases ucore metable? with-metable?)

(defn derefable? [x]
  #?(:clj  (instance?  clojure.lang.IDeref x)
     :cljs (satisfies? cljs.core/IDeref    x)))

#?(:cljs (defn defined? [x] (not (undefined? x))))

;; TODO move to type predicates
(defn array? [x]
  #?(:clj  (-> x class .isArray) ; must be reflective
     :cljs (core/array? x)))

(defn transient? [x]
  #?(:clj  (instance?  clojure.lang.ITransientCollection x)
     :cljs (satisfies? cljs.core/ITransientCollection    x)))

#?(:clj (defn unbound? [x] (instance? clojure.lang.Var$Unbound x)))
