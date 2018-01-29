(ns quantum.untyped.core.data
  (:require
    [quantum.untyped.core.convert :as uconv
      :refer [>keyword]]
    [quantum.untyped.core.core    :as ucore]))

(ucore/log-this-ns)

(defn quote-map-base [kw-modifier ks & [no-quote?]]
  (->> ks
       (map #(vector (cond->> (kw-modifier %) (not no-quote?) (list 'quote)) %))
       (apply concat)))

#?(:clj (defmacro kw-map    [& ks] (list* `hash-map (quote-map-base >keyword ks))))
#?(:clj (defmacro quote-map [& ks] (list* `hash-map (quote-map-base identity ks))))
