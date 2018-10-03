(ns quantum.untyped.core.analyze.ast
  "Facilities for creating AST nodes (for now, just for Clojure).
   No actual analysis is done here."
  (:refer-clojure :exclude
    [symbol Symbol #?(:cljs ->Symbol) symbol?
     ==
     unbound?])
  (:require
    [quantum.untyped.core.analyze.expr      :as uxp]
    [quantum.untyped.core.compare           :as comp
      :refer [==]]
    [quantum.untyped.core.core              :as ucore]
    [quantum.untyped.core.form.type-hint    :as ufth]
    [quantum.untyped.core.type              :as t]
    [quantum.untyped.core.type.reifications :as utr]))

(ucore/log-this-ns)

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

(defrecord Unbound [env #_::env, form #_symbol?, minimum-type #_t/type?, type #_t/type?] ;; TODO `type` should be `t/deducible-type?`
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `unbound form {:minimum minimum-type :deduced type})))

(defn unbound
  ([form t] (unbound nil form t))
                ;; TODO should wrap second `t` in `t/deducible`
  ([env form t] (Unbound. env (ufth/with-type-hint form (>type-hint form t)) t t)))

(defn unbound? [x] (instance? Unbound x))

(defrecord Literal [env #_::env, form #_::t/literal, type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `literal form type)))

(defn literal
  ([form t] (literal nil form t))
  ([env form t] (Literal. env (ufth/with-type-hint form (>type-hint form t)) t)))

(defn literal? [x] (instance? Literal x))

(defrecord Symbol
  [env   #_::env
   form  #_symbol?
   value #_t/any?
   type  #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `symbol (into (array-map) this))))

(defn symbol
  ([form value t] (symbol nil form value t))
  ([env form value t] (Symbol. env (ufth/with-type-hint form (>type-hint form t)) value t)))

(defn symbol? [x] (instance? Symbol x))

;; ===== Special calls ===== ;;

(defrecord Quoted
  [env #_::env, form #_::t/form, type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `quoted form type)))

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
    (-edn [this] (list `let* (into (array-map) this))))

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
    (-edn [this] (list `do (into (array-map) this))))

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
    (-edn [this] (list `macro-call (into (array-map) this))))

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
    (-edn [this] (list `if-node (into (array-map) this))))

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
    (-edn [this] (list `field-access (into (array-map) this))))

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
    (-edn [this] (list `method-call (into (array-map) this))))

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
    (-edn [this] (list `call-node (into (array-map) this))))

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
    (-edn [this] (list `new-node (into (array-map) this))))

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
    (-edn [this] (list `throw-node (into (array-map) this))))

;; Not type hinted because there's no point
(defn throw-node [m] (map->ThrowNode m))

(defn throw-node? [x] (instance? ThrowNode x))
