(ns quantum.untyped.core.analyze.expr
  (:refer-clojure :exclude
    [flatten get ==])
  (:require
    [clojure.core                     :as core]
    [quantum.core.error               :as err
      :refer [err!]]
    [quantum.core.macros.deftype      :as dt]
    [quantum.core.print               :as pr]
    [quantum.core.spec                :as s]
    [quantum.core.specs               :as ss]
    [quantum.core.vars                :as var
      :refer [defalias]]
    [quantum.untyped.core.collections :as coll
      :refer [flatten]]
    [quantum.untyped.core.collections.logic
      :refer [seq-or]]
    [quantum.untyped.core.compare     :as comp
      :refer [== not==]]
    [quantum.untyped.core.convert     :as uconv
      :refer [>symbol]]
    [quantum.untyped.core.qualify     :as qual]
    [quantum.untyped.core.reducers    :as r
      :refer [partition-all+ join]]))

(do

(defn expr>code [x] (cond-> x (fn? x) >symbol))

(definterface IExpr)

(defprotocol PExpr
  (>code       [this])
  (with-code   [this code'])
  (update-code [this f])
  (>evaled     [this]))

(definterface ICall)

(defn icall? [x] (instance? ICall x))

#?(:clj
(defmacro def [sym x]
  `(def ~sym (NamedExpr. '~(qual/qualify sym) ~x))))

#?(:clj (defalias -def def))

(defrecord NamedExpr
  [sym #_symbol? x #__]
  IExpr
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] sym))

;; ===== LOGIC ===== ;;

(defrecord Expr|casef
  [f #_t/fn?, cases #_t/+map?]
  IExpr ICall
  clojure.lang.IFn
    (invoke [_ x]
      (let [dispatch (f x)]
        (if-let [[_ then] (find cases dispatch)]
          (if (icall? then) (then x) then)
          (err! "No matching clause found" {:dispatch dispatch}))))
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this]
      (if pr/*print-as-code?*
          (list* `casef (expr>code f) (map pr/>group cases))
          (list* `casef f cases))))

(defn casef [f & cases]
  (new Expr|casef f (->> cases (partition-all+ 2) (join {}))))

(defrecord Expr|condpf->
  [pred #_t/fn?, f #_t/fn?, clauses #_(t/and* t/sequential? t/indexed?)]
  IExpr ICall
  clojure.lang.IFn
    (invoke [_ x]
      (let [v (f x)]
        (if-let [[_ then :as matching-clause]
                   (seq-or (fn [clause]
                      (if (-> clause count (= 1))
                          clause
                          (let [[condition then] clause]
                            (when (pred v condition)
                              clause)))) clauses)]
          (if (icall? then) (then x) then)
          (err! "No matching clause found" {:v v}))))
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this]
      (if pr/*print-as-code?*
          (list* `condpf->
            (expr>code pred)
            (expr>code f)
            (map pr/>group clauses))
          (list* `condpf-> pred f clauses))))

(defn condpf-> [pred f & clauses]
  (new Expr|condpf-> pred f (->> clauses (partition-all+ 2) join)))

(defrecord Expr|get [k]
  IExpr
  clojure.lang.IFn
    (invoke [this m] (core/get m k))
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `get k)))

(defn get [k] (new Expr|get k))

(defrecord Expr|fn [name arities]
  IExpr
  clojure.lang.IFn
    (invoke [this]       ((get arities 0)))
    (invoke [this a0]    ((get arities 1) a0))
    (invoke [this a0 a1] ((get arities 2) a0 a1))
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (concat [`fn] (when name [name]) arities)))

(dt/deftype
  ^{:doc "All possible behaviors of `code` are inherited except function-callability, which
          is used for calling the evaled code itself.
          A code form may consist of any of the following, recursively:
          - nil
          - number
            - double
            - long
            - bigdec (`M`)
            - bigint (`N`)
          - string
          - symbol
          - keyword
          - seq
          - vector
          - map

          Modification of a tagged literal is only supported to the extent the quoted form
          of the literal may be modified."}
  Expression [code evaled]
  {;; expression-like
   IExpr        nil
   PExpr        {>code       ([this]         code)
                 with-code   ([this code']   (Expression. code' (eval code')))
                 update-code ([this f]       (with-code this (f code)))
                 >evaled     ([this]         evaled)}
   ;; `code`-like
   ?Associative {assoc       ([this k v]     (with-code this (assoc  code k v)))
                 dissoc      ([this k]       (with-code this (dissoc code k)))
                 keys        ([this]         (with-code this (keys      code)))
                 vals        ([this]         (with-code this (vals      code)))
                 contains?   ([this]         (with-code this (contains? code)))
                 find        (([this k]      (with-code this (find      code)))
                              ([this k else] (with-code this (find      code else))))}
   ?Collection  {empty       ([this]         (with-code this (empty     code)))
                 conj        ([this x]       (with-code this (conj      code x)))
                 empty?      ([this]         (with-code this (empty?    code)))
                 equals      ([this that]    (or (== this that)
                                                 (let [^Expression that that]
                                                   (= evaled (.-evaled that))
                                                   (= code   (.-code   that)))))}
   ?Counted     {count       ([this]         (with-code this (count     code)))}
   ?Indexed     {nth         ([this i]       (with-code this (nth       code i)))}
   ?Lookup      {get         (([this k]      (with-code this (core/get  code k)))
                            #_([this k else] (with-code this (core/get  code k else))))} ; TODO make it work
   ?Meta        {meta        ([this]         (with-code this (meta  code)))
                 with-meta   ([this meta']   (Expression. (with-meta code meta') evaled))}
   ?Reversible  {rseq        ([this]         (with-code this (rseq  code)))}
   ?Seq         {first       ([this]         (with-code this (first code)))
                 rest        ([this]         (with-code this (rest  code)))
                 next        ([this]         (with-code this (next  code)))}
   ?Seqable     {seq         ([this]         (with-code this (seq   code)))}
   ?Stack       {peek        ([this]         (with-code this (peek  code)))
                 pop         ([this]         (with-code this (pop   code)))}
   ;; `evaled`-like
   ?Fn          {invoke      (([this]        (evaled))
                              ([this a0]     (evaled a0))
                              ([this a0 a1]  (evaled a0 a1)))}
   ;; printing
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn
     {-edn ([this] (tagged-literal 'expr code))}})

#?(:clj
(defmacro >expr [expr-] `(quantum.untyped.core.analyze.expr.Expression. '~expr- ~expr-)))

(defn expr? [x] (instance? Expression x))

)
