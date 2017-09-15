(ns quantum.core.untyped.analyze.ast
  "Facilities for creating AST nodes (for now, just for Clojure).
   No actual analysis is done here."
  (:refer-clojure :exclude
    [symbol Symbol
     ==])
  (:require
    [quantum.core.untyped.compare :as comp
      :refer [==]]
    [quantum.core.untyped.type    :as t]))

(do

;; ===== CONSTITUENT SPECS ===== ;;

(definterface INode)

#_(t/def ::node (t/isa? INode))
#_(t/def ::env  (t/map-of t/symbol? ::node))

;; ===== NODES ===== ;;

(deftype Unbound [sym #_t/symbol?]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `unbound sym))
  Object
    (equals [this that]
      (or (== this that)
          (and (instance? Unbound that)
               (= sym (.-sym ^Unbound that))))))

(defn unbound [sym] (Unbound. sym))

(deftype Literal [form #_::t/literal, spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `literal form spec))
  Object
    (equals [this that]
      (or (== this that)
          (and (instance? Literal that)
               (= form (.-form ^Literal that))
               (= spec (.-spec ^Literal that))))))

(defn literal
  ([form spec] (Literal. form spec)))

(defrecord Symbol
  [env  #_::env
   form #_::t/form
   spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `symbol (into (array-map) this))))

(defn symbol [m] (map->Symbol m))

;; ===== SPECIAL CALLS ===== ;;

(deftype Quoted
  [form #_::t/form, spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `quoted form spec))
  Object
    (equals [this that]
      (or (== this that)
          (and (instance? Quoted that)
               (= form (.-form ^Quoted that))
               (= spec (.-spec ^Quoted that))))))

(defn quoted [form spec] (Quoted. form spec))

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
  [form     #_::t/form
   expanded #_::node]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `macro-call (into (array-map) this))))

(defn macro-call [m] (map->MacroCall m))

;; ===== RUNTIME CALLS ===== ;;

(defrecord StaticCall
  [env  #_::env
   form #_::t/form
   f    #_t/qualified-symbol?
   args #_(t/and t/sequential? t/indexed? (t/every? ::node))
   spec #_::t/spec]
  INode
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (list `static-call (into (array-map) this))))

(defn static-call [m] (map->StaticCall m))

)
