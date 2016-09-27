(ns quantum.test.core.collections.base
  (:require [quantum.core.collections.base :as ns]))

(defn test:name [x])

(defn test:default-zipper [coll])

(defn test:ensure-set [x])

(defn test:zip-reduce* [f init z])

(defn test:reducei [f init coll])

(defn test:merge-call [m f])

(defn test:camelcase
  [str-0 & [method?]])

(defn test:ns-qualify [sym ns-])

(defn test:frequencies-by
  [f coll])

(defn test:update-first [x f])

(defn test:update-val [[k v] f])

#?(:clj
(defmacro test:kmap [& ks]))

#?(:clj
(defmacro test:eval-map [& ks]))


(defn test:appears-within?
  [x coll])

(defn test:dissoc-in
  [m ks])
