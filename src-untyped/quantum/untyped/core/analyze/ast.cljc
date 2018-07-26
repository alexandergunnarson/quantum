(ns quantum.untyped.core.analyze.ast
  "Facilities for creating AST nodes (for now, just for Clojure).
   No actual analysis is done here."
  (:refer-clojure :exclude
    [symbol Symbol #?(:cljs ->Symbol) symbol?
     ==
     unbound?])
  (:require
    [quantum.untyped.core.compare :as comp
      :refer [==]]
    [quantum.untyped.core.core    :as ucore]
    [quantum.untyped.core.type    :as t]))

(ucore/log-this-ns)

(do

;; ===== Constituent types ===== ;;

(#?(:clj definterface :cljs defprotocol) INode
  (getForm [#?(:cljs this)])
  (getType [#?(:cljs this)]))

(defn node? [x] (instance? INode x))

#_(t/def ::node (t/isa? INode))
#_(t/def ::env  (t/map-of t/symbol? ::node))

;; ===== Nodes ===== ;;

(defrecord Unbound [env #_::env, form #_t/symbol?, minimum-type #_t/type?, type #_t/type?] ;; TODO `type` should be `t/deducible-type?`
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `unbound form {:minimum minimum-type :deduced type})))

(defn unbound
  ([form t] (unbound nil form t))
  ([env form t] (Unbound. env form t t))) ; TODO should wrap second `t` in `t/deducible`

(defn unbound? [x] (instance? Unbound x))

(defrecord Literal [env #_::env, form #_::t/literal, type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `literal form type)))

(defn literal
  ([form t] (literal nil form t))
  ([env form t] (Literal. env form t)))

(defrecord Symbol
  [env   #_::env
   form  #_t/symbol?
   value #_t/any?
   type  #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `symbol (into (array-map) this))))

(defn symbol
  ([form value t] (symbol nil form value t))
  ([env form value t] (Symbol. env form value t)))

(defn symbol? [x] (instance? Symbol x))

;; ===== Special calls ===== ;;

(defrecord Quoted
  [env #_::env, form #_::t/form, type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `quoted form type)))

(defn quoted [form t] (Quoted. nil form t))

(defrecord Let*
  [env      #_::env
   form     #_::t/body
   bindings #_::env
   body     #_(t/and t/sequential? t/indexed? (t/every? ::node))
   type     #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `let* (into (array-map) this))))

(defn let* [m] (map->Let* m))

(defrecord Do
  [env  #_::env
   form #_::t/form
   expanded-form #_::t/form
   body #_(t/and t/sequential? t/indexed? (t/every? ::node))
   type #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `do (into (array-map) this))))

(defn do [m] (map->Do m))

(defrecord MacroCall
  [env           #_::env
   form          #_::t/form
   expanded-form #_::t/form ; the *fully* expanded form
   expanded      #_::node
   type          #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `macro-call (into (array-map) this))))

(defn macro-call [m] (-> m map->MacroCall (assoc :type (-> m :expanded :type))))

(defrecord IfNode
  [env        #_::env
   form       #_::t/form
   pred-node  #_::node
   true-node  #_::node
   false-node #_::node
   type       #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `if-node (into (array-map) this))))

(defn if-node [m] (map->IfNode m))

;; ===== RUNTIME CALLS ===== ;;

(defrecord FieldAccess
  [env    #_::env
   form   #_::t/form
   target #_::node
   field  #_t/unqualified-symbol?
   type   #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `field-access (into (array-map) this))))

(defn field-access [m] (map->FieldAccess m))

(defrecord MethodCall
  [env    #_::env
   form   #_::t/form
   target #_::node
   method #_::t/unqualified-symbol?
   args   #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   type   #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `method-call (into (array-map) this))))

(defn method-call [m] (map->MethodCall m))

(defrecord CallNode ; by a `t/callable?`
  [env    #_::env
   form   #_::t/form
   caller #_::node
   args   #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   type   #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `call-node (into (array-map) this))))

(defn call-node [m] (map->CallNode m))

(defrecord NewNode
  [env   #_::env
   form  #_::t/form
   class #_t/class?
   args  #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   type  #_t/type?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `new-node (into (array-map) this))))

(defn new-node [m] (map->NewNode m))

(defrecord ThrowNode
  [env  #_::env
   form #_::t/form
   arg  #_::node
   type #_t/nil?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `throw-node (into (array-map) this))))

(defn throw-node [m] (map->ThrowNode m))

)
