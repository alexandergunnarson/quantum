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

;; ===== CONSTITUENT SPECS ===== ;;

(#?(:clj definterface :cljs defprotocol) INode
  (getForm [#?(:cljs this)])
  (getSpec [#?(:cljs this)]))

(defn node? [x] (instance? INode x))

#_(t/def ::node (t/isa? INode))
#_(t/def ::env  (t/map-of t/symbol? ::node))

;; ===== NODES ===== ;;

(defrecord Unbound [env #_::env, form #_t/symbol?, minimum-spec #_t/spec?, spec #_t/spec?] ;; TODO `spec` should be `t/deducible-spec?`
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `unbound form {:minimum minimum-spec :deduced spec})))

(defn unbound
  ([form spec] (unbound nil form spec))
  ([env form spec] (Unbound. env form spec spec))) ; TODO should wrap second `spec` in `t/deducible`

(defn unbound? [x] (instance? Unbound x))

(defrecord Literal [env #_::env, form #_::t/literal, spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `literal form spec)))

(defn literal
  ([form spec] (literal nil form spec))
  ([env form spec] (Literal. env form spec)))

(defrecord Symbol
  [env  #_::env
   form #_t/symbol?
   spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `symbol (into (array-map) this))))

(defn symbol
  ([form spec] (symbol nil form spec))
  ([env form spec] (Symbol. env form spec)))

(defn symbol? [x] (instance? Symbol x))

;; ===== SPECIAL CALLS ===== ;;

(defrecord Quoted
  [env #_::env, form #_::t/form, spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `quoted form spec)))

(defn quoted [form spec] (Quoted. nil form spec))

(defrecord Let*
  [env      #_::env
   form     #_::t/body
   bindings #_::env
   body     #_(t/and t/sequential? t/indexed? (t/every? ::node))
   spec     #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `let* (into (array-map) this))))

(defn let* [m] (map->Let* m))

(defrecord Do
  [env  #_::env
   form #_::t/form
   body #_(t/and t/sequential? t/indexed? (t/every? ::node))
   spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `do (into (array-map) this))))

(defn do [m] (map->Let* m))

(defrecord MacroCall
  [env      #_::env
   form     #_::t/form
   expanded #_::node
   spec     #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `macro-call (into (array-map) this))))

(defn macro-call [m] (-> m map->MacroCall (assoc :spec (-> m :expanded :spec))))

(defrecord IfExpr
  [env        #_::env
   form       #_::t/form
   pred-expr  #_::node
   true-expr  #_::node
   false-expr #_::node
   spec       #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `if-expr (into (array-map) this))))

(defn if-expr [m] (map->IfExpr m))

;; ===== RUNTIME CALLS ===== ;;

(defrecord FieldAccess
  [env    #_::env
   form   #_::t/form
   target #_::node
   field  #_t/unqualified-symbol?
   spec   #_::t/spec]
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
   spec   #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `method-call (into (array-map) this))))

(defn method-call [m] (map->MethodCall m))

(defrecord CallExpr ; by a `t/callable?`
  [env    #_::env
   form   #_::t/form
   caller #_::node
   args   #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   spec   #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `call-expr (into (array-map) this))))

(defn call-expr [m] (map->CallExpr m))

(defrecord NewExpr
  [env   #_::env
   form  #_::t/form
   class #_t/class?
   args  #_(t/and t/sequential? t/indexed? (t/seq-and ::node))
   spec  #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `new-expr (into (array-map) this))))

(defn new-expr [m] (map->NewExpr m))

(defrecord ThrowExpr
  [env  #_::env
   form #_::t/form
   arg  #_::node
   spec #_t/nil?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `throw-expr (into (array-map) this))))

(defn throw-expr [m] (map->ThrowExpr m))

)
