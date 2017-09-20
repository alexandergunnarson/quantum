(ns quantum.core.untyped.analyze.expr
  (:refer-clojure :exclude
    [count flatten get ==])
  (:require
    [quantum.core.cache               :as cache
      :refer [defmemoized]]
    [quantum.core.vars                :as var
      :refer [defalias]]
    [quantum.core.untyped.collections :as coll
      :refer [flatten]]
    [quantum.core.untyped.compare     :as comp
      :refer [== not==]]
    [quantum.core.untyped.reducers    :as r
      :refer [partition-all+ join]]
    [quantum.core.untyped.type        :as t]))

(do

(definterface IExpr)

#?(:clj (defalias -def def))

(defrecord NamedExpr
  [sym #_symbol? x #__]
  IExpr
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] sym))

#?(:clj
(defmacro def [sym x]
  `(def ~sym (NamedExpr. '~(var/qualify sym) ~x))))


;; ===== LOGIC ===== ;;

(defrecord Expr:casef
  [f #_t/fn?, cases #_t/+map?]
  IExpr
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list* `casef f cases)))

(defn casef [f & cases]
  (new Expr:casef f (->> cases (partition-all+ 2) (join {}))))

(defrecord Expr:condpf->
  [pred #_t/fn?, f #_t/fn?, clauses #_(t/and* t/sequential? t/indexed?)]
  IExpr
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list* `condpf-> pred f clauses)))

(defn condpf-> [pred f & clauses]
  (new Expr:condpf-> pred f (->> clauses (partition-all+ 2) join)))

;; TODO this is just temporary until we transition `Expr:get` to a `defrecord` which overloads on equality
(defmemoized gen-get-fn
  {:memoize-only-first? true
   :assoc-fn (fn [*cache k *v] (when (t/literal? k) (swap! *cache assoc k @*v)))}
  [k] (coll/get k))

(defrecord Expr:get [f #_t/fn? k]
  IExpr
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `get k)))

(defn get [k] (new Expr:get (gen-get-fn k) k))

)
