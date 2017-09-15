(ns quantum.core.untyped.collections
  (:refer-clojure :exclude [assoc-in])
  (:require [clojure.core :as core]))

(defn assoc-in
  "Like `assoc-in`, but allows multiple k-v pair arguments like `assoc`."
  ([x ks v] (core/assoc-in x ks v))
  ([x ks v & ks-vs]
    (reduce (fn [x' [ks' v']] (assoc-in x' ks' v'))
            (assoc-in x ks v)
            (partition-all 2 ks-vs))))
