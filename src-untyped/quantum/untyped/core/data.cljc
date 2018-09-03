(ns quantum.untyped.core.data
  (:refer-clojure :exclude
    [seqable?])
  (:require
    [quantum.untyped.core.convert    :as uconv
      :refer [>keyword]]
    [quantum.untyped.core.core       :as ucore]
    [quantum.untyped.core.data.array :as uarr]
    [quantum.untyped.core.vars
      :refer [defalias]]))

(ucore/log-this-ns)

(def val? some?)

(defn quote-map-base [kw-modifier ks & [no-quote?]]
  (->> ks
       (map #(vector (cond->> (kw-modifier %) (not no-quote?) (list 'quote)) %))
       (apply concat)))

#?(:clj (defmacro kw-map    [& ks] (list* `hash-map (quote-map-base >keyword ks))))
#?(:clj (defmacro quote-map [& ks] (list* `hash-map (quote-map-base identity ks))))

#?(:clj  (defn seqable?
           "Returns true if (seq x) will succeed, false otherwise."
           {:from "clojure.contrib.core"}
           [x]
           (or (seq? x)
               (instance? clojure.lang.Seqable x)
               (nil? x)
               (instance? Iterable x)
               (uarr/array? x)
               (string? x)
               (instance? java.util.Map x)))
   :cljs (def seqable? core/seqable?))

(defalias ucore/lookup?)

(defn editable? [x]
  #?(:clj  (instance?  clojure.lang.IEditableCollection x)
     :cljs (satisfies? cljs.core/IEditableCollection    x)))

(defn transient? [x]
  #?(:clj  (instance?  clojure.lang.ITransientCollection x)
     :cljs (satisfies? cljs.core/ITransientCollection    x)))
