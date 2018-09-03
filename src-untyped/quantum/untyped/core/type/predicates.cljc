(ns quantum.untyped.core.type.predicates
  "For type predicates that are not yet turned into specs.
   TODO excise and place in `quantum.untyped.core.type`."
  (:refer-clojure :exclude
    [any? seqable?])
  (:require
        [clojure.core              :as core]
#?(:clj [clojure.future            :as fcore])
        [quantum.untyped.core.core :as ucore]
        [quantum.untyped.core.vars
          :refer [defalias defaliases]]))

(ucore/log-this-ns)

#?(:clj  (eval `(defalias ~(if (resolve `fcore/any?)
                               `fcore/any?
                               `core/any?)))
   :cljs (defalias core/any?))

(defn protocol? [x]
  #?(:clj  (and (lookup? x) (-> x (get :on-interface) class?))
           ;; Unfortunately there's no better check in CLJS, at least as of 03/18/2018
     :cljs (and (fn? x) (= (str x) "function (){}"))))

(defn derefable? [x]
  #?(:clj  (instance?  clojure.lang.IDeref x)
     :cljs (satisfies? cljs.core/IDeref    x)))


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

(defn lookup? [x]
  #?(:clj  (instance?  clojure.lang.ILookup x)
     :cljs (satisfies? cljs.core/ILookup    x)))

(defn editable? [x]
  #?(:clj  (instance?  clojure.lang.IEditableCollection x)
     :cljs (satisfies? cljs.core/IEditableCollection    x)))

(defn transient? [x]
  #?(:clj  (instance?  clojure.lang.ITransientCollection x)
     :cljs (satisfies? cljs.core/ITransientCollection    x)))
