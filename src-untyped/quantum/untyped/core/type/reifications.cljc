(ns quantum.untyped.core.type.reifications
          (:refer-clojure :exclude
            [==])
          (:require
            [fipp.ednize                                :as fedn]
            [quantum.untyped.core.analyze.expr
#?@(:cljs    [:refer [Expression]])]
            [quantum.untyped.core.compare
              :refer [== not==]]
            [quantum.untyped.core.core                  :as ucore]
            [quantum.untyped.core.defnt
              :refer [defns]]
            [quantum.untyped.core.form.generate.deftype :as udt])
 #?(:clj  (:import
            [quantum.untyped.core.analyze.expr Expression])))

(ucore/log-this-ns)

(defprotocol PType)

(defns type? [x _ > boolean?] (satisfies? PType x))

;; ----- UniversalSetType (`t/U`) ----- ;;

(udt/deftype
  ^{:doc "Represents the set of all sets that do not include themselves (including the empty set).
          Equivalent to `(constantly true)`."}
  UniversalSetType []
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] 'quantum.untyped.core.type/U)}})

(def universal-set (UniversalSetType.))

;; ----- EmptySetType (`t/∅`) ----- ;;

(udt/deftype
  ^{:doc "Represents the empty set.
          Equivalent to `(constantly false)`."}
  EmptySetType []
  {PType         nil
   fednIOverride nil
   fednIEdn      {-edn ([this] 'quantum.untyped.core.type/∅)}})

(def empty-set (EmptySetType.))

;; ----- NotType (`t/not` / `t/!`) ----- ;;

(udt/deftype NotType [t #_t/type?]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn   ([this] (list 'quantum.untyped.core.type/not t))}
   ?Fn            {invoke ([_ x] (t x))}
   ?Object        ;; Tests for structural equivalence
                  {equals ([this that]
                            (or (== this that)
                                (and (instance? NotType that)
                                     (= t (.-t ^NotType that)))))}})

(defns not-type? [x _ > boolean?] (instance? NotType x))

(defns not-type>inner-type [t not-type?] (.-t ^NotType t))

;; ----- OrType (`t/or` / `t/|`) ----- ;;

(udt/deftype OrType [args #_(t/and t/indexed? (t/seq t/type?)) *logical-complement]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (list* 'quantum.untyped.core.type/or args))}
   ?Fn            {invoke ([_ x] (reduce
                                   (fn [_ t]
                                     (let [satisfies-type? (t x)]
                                       (and satisfies-type? (reduced satisfies-type?))))
                                   true ; vacuously
                                   args))}
   ?Object        ;; Tests for structural equivalence
                  {equals ([this that]
                            (or (== this that)
                                (and (instance? OrType that)
                                     (= args (.-args ^OrType that)))))}})

(defns or-type? [x _ > boolean?] (instance? OrType x))

(defns or-type>args [x or-type?] (.-args ^OrType x))

;; ----- AndType (`t/and` | `t/&`) ----- ;;

(udt/deftype AndType [args #_(t/and t/indexed? (t/seq t/type?)) *logical-complement]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (list* 'quantum.untyped.core.type/and args))}
   ?Fn            {invoke ([_ x] (reduce (fn [_ t] (or (t x) (reduced false)))
                                   true ; vacuously
                                   args))}
   ?Object        ;; Tests for structural equivalence
                  {equals ([this that]
                            (or (== this that)
                                (and (instance? AndType that)
                                     (= args (.-args ^AndType that)))))}})

(defns and-type? [x _ > boolean?] (instance? AndType x))

(defns and-type>args [x and-type?] (.-args ^AndType x))

;; ----- Expression ----- ;;

#?(:clj (extend-protocol PType Expression))

;; ----- ProtocolType ----- ;;

(udt/deftype ProtocolType
  [meta #_(t/? ::meta)
   p    #_t/protocol?
   name #_(t/? t/symbol?)]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (or name (list 'quantum.untyped.core.type/isa?|protocol (:on p))))}
   ?Fn            {invoke    ([_ x] (satisfies? p x))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (ProtocolType. meta' p name))}
   ?Object        {equals    ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ProtocolType that)
                                        (= p (.-p ^ProtocolType that)))))}})

(defns protocol-type? [x _] (instance? ProtocolType x))

(defns protocol-type>protocol [t protocol-type?] (.-p ^ProtocolType t))

;; ----- ClassType ----- ;;

(udt/deftype ClassType
  [       meta #_(t/? ::meta)
   ^Class c    #_t/class?
          name #_(t/? t/symbol?)]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (or name (list 'quantum.untyped.core.type/isa? c)))}
   ?Fn            {invoke    ([_ x] (instance? c x))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (ClassType. meta' c name))}
   ?Object        {equals    ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ClassType that)
                                        (= c (.-c ^ClassType that)))))}})

(defns class-type? [x _] (instance? ClassType x))

(defns class-type>class [t class-type?] (.-c ^ClassType t))

;; ----- ValueType ----- ;;

(udt/deftype ValueType [v #_any?]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn   ([this] (list 'quantum.untyped.core.type/value v))}
   ?Fn            {invoke ([_ x] (= x v))}
   ?Object        {equals ([this that #_any?]
                            (or (== this that)
                                (and (instance? ValueType that)
                                     (= v (.-v ^ValueType that)))))}})

(defns value-type? [x _] (instance? ValueType x))

(defns value-type>value [v value-type?] (.-v ^ValueType v))
