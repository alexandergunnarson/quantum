(ns quantum.untyped.core.type.predicates
  "For type predicates that are not yet turned into specs.
   TODO excise and place in `quantum.untyped.core.type`."
  (:refer-clojure :exclude
    [any? pos-int? seqable?])
  (:require
        [clojure.core              :as core]
#?(:clj [clojure.future            :as fcore])
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

#?(:clj  (eval `(defalias ~(if (resolve `fcore/pos-int?)
                               `fcore/pos-int?
                               `core/pos-int?)))
   :cljs (defalias core/pos-int?))

(defn lookup? [x]
  #?(:clj  (instance?  clojure.lang.ILookup x)
     :cljs (satisfies? cljs.core/ILookup    x)))

(defn protocol? [x]
  #?(:clj  (and (lookup? x) (-> x (get :on-interface) class?))
           ;; Unfortunately there's no better check in CLJS, at least as of 03/18/2018
     :cljs (and (fn? x) (= (str x) "function (){}"))))

;; TODO this references data.array
#?(:clj  (defn seqable?
           "Returns true if (seq x) will succeed, false otherwise."
           {:from "clojure.contrib.core"}
           [x]
           (or (seq? x)
               (instance? clojure.lang.Seqable x)
               (nil? x)
               (instance? Iterable x)
               (array? x)
               (string? x)
               (instance? java.util.Map x)))
   :cljs (def seqable? core/seqable?))

(defn editable? [x]
  #?(:clj  (instance?  clojure.lang.IEditableCollection x)
     :cljs (satisfies? cljs.core/IEditableCollection    x)))

(defn derefable? [x]
  #?(:clj  (instance?  clojure.lang.IDeref x)
     :cljs (satisfies? cljs.core/IDeref    x)))

(defn transient? [x]
  #?(:clj  (instance?  clojure.lang.ITransientCollection x)
     :cljs (satisfies? cljs.core/ITransientCollection    x)))
