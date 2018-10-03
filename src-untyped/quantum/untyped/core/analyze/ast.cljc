(ns quantum.untyped.core.analyze.ast
  "Facilities for creating AST nodes (for now, just for Clojure).
   No actual analysis is done here."
  (:refer-clojure :exclude
    [== symbol Symbol #?(:cljs ->Symbol) symbol? unbound? var var?])
  (:require
    [quantum.untyped.core.analyze.expr      :as uxp]
    [quantum.untyped.core.compare           :as comp
      :refer [==]]
    [quantum.untyped.core.core              :as ucore]
    [quantum.untyped.core.form.type-hint    :as ufth]
    [quantum.untyped.core.type              :as t]
    [quantum.untyped.core.type.reifications :as utr]))

(ucore/log-this-ns)

(def ^:dynamic ^{:doc "Controls whether `:env` is printed on AST nodes."} *print-env?* true)

(defn std-print-structure [record]
  (cond-> (into (array-map) record) (not *print-env?*) (dissoc :env)))

(defn >type-hint
  "Applied on every `form` of every AST node created in order to avoid reflection wherever
   possible."
  [form t]
  (if (or (not (t/with-metable? form))
          (utr/fn-type? t)
          ;; TODO for now
          (uxp/iexpr? t))
      nil
      (let [cs (t/type>classes t)]
        (case (count cs)
          1 (let [c (first cs)]
              (when-let [not-primitive? (not (contains? t/boxed-class->unboxed-symbol c))]
                (ufth/>body-embeddable-tag c)))
          2 (when (contains? cs nil)
              (-> cs (disj nil) first ufth/>body-embeddable-tag))
          nil))))

(defn with-type-hint [node]
  (if-let [type-hint (>type-hint (:form node) (:type node))]
    (-> node
        (update :form ufth/with-type-hint type-hint)
        (cond-> (contains? node :unexpanded-form)
          (update :unexpanded-form ufth/with-type-hint type-hint)))
    node))

;; ===== Constituent types ===== ;;

(#?(:clj definterface :cljs defprotocol) INode
  (getForm [#?(:cljs this)])
  (getType [#?(:cljs this)]))

(defn node? [x] (instance? INode x))

#_(t/def ::node (t/isa? INode))
#_(t/def ::env  (t/map-of symbol? ::node))

;; ===== Nodes ===== ;;

;; Does not include unbound vars; this is specifically for arguments
(defrecord Unbound
  [env          #_::env
   form         #_symbol?
   minimum-type #_t/type?
   type         #_t/type?] ;; TODO should be `t/deducible-type?`
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `unbound (std-print-structure this))))

(defn unbound
  ([form t] (unbound nil form t))
                ;; TODO should wrap second `t` in `t/deducible`
  ([env form t] (Unbound. env (ufth/with-type-hint form (>type-hint form t)) t t)))

(defn unbound? [x] (instance? Unbound x))

(defrecord
  ^{:doc "AST node whose `type` is `(t/value form)`."}
  Literal [env #_::env, form #_t/literal?, type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `literal (std-print-structure this))))

(defn literal
  ([form t] (literal nil form t))
  ([env form t] (Literal. env (ufth/with-type-hint form (>type-hint form t)) t)))

(defn literal? [x] (instance? Literal x))

(defrecord ClassValue
  [env   #_::env
   form  #_simple-symbol?
   value #_t/class?
   type  #_(t/value value)]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `class-value (std-print-structure this))))

(defn class-value
  ([form v] (class-value nil form v))
  ([env form v] (ClassValue. env form v (t/value v))))

(defn class-value? [x] (instance? ClassValue x))

(defrecord
  ^{:doc "AST node generated from the value of a non-dynamic var.
          The `type` may not always be `(t/value value)` because in the case of e.g. `t/defn`s,
          their corresponding var value may just be a `core/defn`, but the type of the `t/defn` is
          annotated in the var's metadata."}
  VarValue
  [env   #_::env
   form  #_qualified-symbol?
   value #_t/any?
   type  #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `var-value (std-print-structure this))))

(defn var-value
  ([form v t] (var-value nil form v t))
  ([env form v t] (VarValue. env (ufth/with-type-hint form (>type-hint form t)) v t)))

(defn var-value? [x] (instance? VarValue x))

(defrecord
  ^{:doc "AST node reserved only for dynamic vars."}
  Var
  [env   #_::env
   form  ; (list 'var <qualified-symbol?>)
   value #_core/var?
   type  #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `var* (std-print-structure this))))

(defn var*
  ([form value t] (var* nil form value t))
  ([env form value t] (Var. env form value t)))

(defn var? [x] (instance? Var x))

(defrecord Symbol
  [env  #_::env
   form #_id/symbol?
   node #_t/any?
   type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `symbol (std-print-structure this))))

(defn symbol
  ([form node t] (symbol nil form node t))
  ([env form node t] (Symbol. env (ufth/with-type-hint form (>type-hint form t)) node t)))

(defn symbol? [x] (instance? Symbol x))

;; ===== Special calls ===== ;;

(defrecord Quoted
  [env #_::env, form #_::t/form, type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `quoted (std-print-structure this))))

(defn quoted
  ([form t] (quoted nil form t))
  ([env form t] (Quoted. nil (ufth/with-type-hint form (>type-hint form t)) t)))

(defn quoted? [x] (instance? Quoted x))

(defrecord Let*
  [env             #_::env
   unanalyzed-form #_::t/form
   form            #_::t/body
   bindings        #_::env
   body            #_(t/and t/sequential? t/indexed? (t/every? ::node))
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `let* (std-print-structure this))))

(defn let* [m] (-> m map->Let* with-type-hint))

(defn let*? [x] (instance? Let* x))

(defrecord Do
  [env             #_::env
   unanalyzed-form #_::t/form
   form            #_::t/form
   body            #_(t/and t/sequential? t/indexed? (t/every? ::node))
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `do (std-print-structure this))))

(defn do [m] (-> m map->Do with-type-hint))

(defn do? [x] (instance? Do x))

(defrecord MacroCall
  [env             #_::env
   unexpanded-form #_::t/form ; the original form
   unanalyzed-form #_::t/form ; the expanded-once form, pre-analysis
   form            #_::t/form ; the *fully* expanded form, post-analysis
   expanded        #_::node
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `macro-call (std-print-structure this))))

(defn macro-call [m] (-> m map->MacroCall with-type-hint))

(defn macro-call? [x] (instance? MacroCall x))

(defrecord IfNode
  [env             #_::env
   unanalyzed-form #_::t/form
   form            #_::t/form
   pred-node       #_::node
   true-node       #_::node
   false-node      #_::node
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `if-node (std-print-structure this))))

(defn if-node [m] (-> m map->IfNode with-type-hint))

(defn if-node? [x] (instance? IfNode x))

;; ===== RUNTIME CALLS ===== ;;

(defrecord FieldAccess
  [env             #_::env
   form            #_::t/form
   target          #_::node
   field           #_unqualified-symbol?
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `field-access (std-print-structure this))))

;; Not type hinted because it's inferred
(defn field-access [m] (map->FieldAccess m))

(defn field-access? [x] (instance? FieldAccess x))

(defrecord MethodCall
  [env             #_::env
   unanalyzed-form #_::t/form
   form            #_::t/form
   target          #_::node
   method          #_::unqualified-symbol?
   args            #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `method-call (std-print-structure this))))

;; Not type hinted because it's inferred
(defn method-call [m] (map->MethodCall m))

(defn method-call? [x] (instance? MethodCall x))

(defrecord CallNode ; by a `t/callable?`
  [env             #_::env
   unanalyzed-form #_::t/form
   form            #_::t/form
   caller          #_::node
   args            #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `call-node (std-print-structure this))))

(defn call-node [m] (-> m map->CallNode with-type-hint))

(defn call-node? [x] (instance? CallNode x))

(defrecord NewNode
  [env             #_::env
   unanalyzed-form #_::t/form
   form            #_::t/form
   class           #_t/class?
   args            #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   type            #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `new-node (std-print-structure this))))

;; Not type hinted because it's inferred
(defn new-node [m] (map->NewNode m))

(defn new-node? [x] (instance? NewNode x))

(defrecord ThrowNode
  [env             #_::env
   unanalyzed-form #_::t/form
   form            #_::t/form
   arg             #_::node
   type            #_t/nil?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `throw-node (std-print-structure this))))

;; Not type hinted because there's no point
(defn throw-node [m] (map->ThrowNode m))

(defn throw-node? [x] (instance? ThrowNode x))
