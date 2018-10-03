(ns quantum.untyped.core.analyze.expr
  "An expression is an object whose form is retained and editable to form new objects."
  (:refer-clojure :exclude
    [flatten ==])
  (:require
    [clojure.core                               :as core]
    [quantum.untyped.core.form.generate.deftype :as udt #?@(:cljs [:include-macros true])] ; should be obvious but oh well
    [quantum.untyped.core.collections
      :refer [flatten partition-all+]]
    [quantum.untyped.core.collections.logic
      :refer [seq-or]]
    [quantum.untyped.core.compare
      :refer [== not==]]
    [quantum.untyped.core.core                  :as ucore]
    [quantum.untyped.core.error                 :as uerr
      :refer [err! TODO]]
    [quantum.untyped.core.form                  :as uform
      :refer [>form]]
    [quantum.untyped.core.identifiers           :as uident
      :refer [>symbol]]
    [quantum.untyped.core.print                 :as upr]
    [quantum.untyped.core.reducers              :as ur
      :refer [join]]
    [quantum.untyped.core.vars
      :refer [defalias]]))

(ucore/log-this-ns)

(defn expr>form [x] (cond-> x (fn? x) >symbol))

(#?(:clj definterface :cljs defprotocol) IExpr)

(defn iexpr? [x] (#?(:clj instance? :cljs satisfies?) IExpr x))

(defprotocol PExpr
  (with-form   [this form'])
  (update-form [this f])
  (>evaled     [this]))

(#?(:clj definterface :cljs defprotocol) ICall)

(defn call? [x] (#?(:clj instance? :cljs satisfies?) ICall x))

#?(:clj
(defmacro def [sym x]
  `(def ~sym (NamedExpr. '~(uident/qualify sym) ~x))))

#?(:clj (defalias -def def))

(defrecord NamedExpr [sym #_symbol? x #__]
  IExpr
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] sym))

(udt/deftype
  ^{:doc "All possible behaviors of `form` (e.g. `get`/`update`/`conj`) are inherited except
          function-callability, which is used for calling the evaled form itself.

          Modification of a tagged literal is only supported to the extent the quoted form
          of the literal may be modified."}
  Expression [form evaled]
  {;; expression-like
   IExpr          nil
   uform/PGenForm {>form       ([this]         form)}
   PExpr          {with-form   ([this form']
                                 (Expression. form'
                                   (#?(:clj eval :cljs (TODO "eval not supported")) form')))
                   update-form ([this f]       (with-form this (f form)))
                   >evaled     ([this]         evaled)}
   ;; `form`-like
   ?Associative   {assoc       ([this k v]     (with-form this (assoc  form k v)))
                   dissoc      ([this k]       (with-form this (dissoc form k)))
                   keys        ([this]         (with-form this (keys   form)))
                   vals        ([this]         (with-form this (vals   form)))
                   contains?   ([this]         (contains? form))
                   find        (([this k]      (with-form this (find   form)))
                                ([this k else] (with-form this (find   form else))))}
   ?Collection    {empty       ([this]         (with-form this (empty  form)))
                   conj        ([this x]       (with-form this (conj   form x)))
                   empty?      ([this]         (empty? form))
                   equals      ([this that]    (or (== this that)
                                                   (and (instance? Expression that)
                                                        (let [^Expression that that]
                                                          (= evaled (.-evaled that))
                                                          (= form   (.-form   that))))))}
   ?Counted       {count       ([this]         (count form))}
   ?Indexed       {nth         ([this i]       (with-form this (nth form i)))}
   ?Lookup        {get         (([this k]      (with-form this (get form k)))
                              #_([this k else] (with-form this (get form k else))))} ; TODO   make it work
   ?Meta          {meta        ([this]         (meta form))
                   with-meta   ([this meta']   (Expression. (with-meta form meta') evaled))}
   ?Reversible    {rseq        ([this]         (with-form this (rseq  form)))}
   ?Seq           {first       ([this]         (with-form this (first form)))
                   rest        ([this]         (with-form this (rest  form)))
                   next        ([this]         (with-form this (next  form)))}
   ?Seqable       {seq         ([this]         (with-form this (seq   form)))}
   ?Stack         {peek        ([this]         (with-form this (peek  form)))
                   pop         ([this]         (with-form this (pop   form)))}
   ;; `evaled`-like
   ?Fn            {invoke      (([this]        (evaled))
                                ([this a0]     (evaled a0))
                                ([this a0 a1]  (evaled a0 a1)))}
   ;; printing
   fipp.ednize/IOverride nil
   fipp.ednize/IEdn      {-edn ([this] (tagged-literal 'expr form))}})

#?(:clj
(defmacro >expr [expr-] `(quantum.untyped.core.analyze.expr.Expression. '~expr- ~expr-)))

#?(:clj (defn expr? [x] (instance? Expression x)))
