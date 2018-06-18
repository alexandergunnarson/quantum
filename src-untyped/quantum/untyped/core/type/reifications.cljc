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
            [quantum.untyped.core.data.hash             :as uhash]
            [quantum.untyped.core.defnt
              :refer [defns]]
            [quantum.untyped.core.form.generate.deftype :as udt])
 #?(:clj  (:import
            [quantum.untyped.core.analyze.expr Expression])))

(ucore/log-this-ns)

(defprotocol PType)

(defns type? [x _ > boolean?] (satisfies? PType x))

;; Here `c/=` tests for structural equivalence

;; ----- UniversalSetType (`t/U`) ----- ;;

(udt/deftype
  ^{:doc "Represents the set of all sets that do not include themselves (including the empty set).
          Equivalent to `(constantly true)`."}
  UniversalSetType []
  {PType          nil
   ?Fn            {invoke    ([_ x] true)}
   ?Hash          {hash      ([this] (hash       UniversalSetType))}
   ?Object        {hash-code ([this] (uhash/code UniversalSetType))
                   equals    ([this that] (or (== this that) (instance? UniversalSetType that)))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] 'quantum.untyped.core.type/U)}})

(def universal-set (UniversalSetType.))

;; ----- EmptySetType (`t/∅`) ----- ;;

(udt/deftype
  ^{:doc "Represents the empty set.
          Equivalent to `(constantly false)`."}
  EmptySetType []
  {PType          nil
   ?Fn            {invoke    ([_ x] false)}
   ?Hash          {hash      ([this] (hash       EmptySetType))}
   ?Object        {hash-code ([this] (uhash/code EmptySetType))
                   equals    ([this that] (or (== this that) (instance? EmptySetType that)))}
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] 'quantum.untyped.core.type/∅)}})

(def empty-set (EmptySetType.))

;; ----- NotType (`t/not` / `t/!`) ----- ;;

(udt/deftype NotType
  [^int ^:unsynchronized-mutable hash
   ^int ^:unsynchronized-mutable hash-code
   t #_t/type?]
  {PType          nil
   ?Fn            {invoke    ([_ x] (not (t x)))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      NotType t))}
   ?Object        {hash-code ([this] (uhash/caching-set-code!    hash-code NotType t))
                   equals    ([this that]
                               (or (== this that)
                                   (and (instance? NotType that)
                                        (= t (.-t ^NotType that)))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (list 'quantum.untyped.core.type/not t))}})

(defns not-type? [x _ > boolean?] (instance? NotType x))

(defns not-type>inner-type [t not-type?] (.-t ^NotType t))

;; ----- OrType (`t/or` / `t/|`) ----- ;;

(udt/deftype OrType
  [^int ^:unsynchronized-mutable hash
   ^int ^:unsynchronized-mutable hash-code
   args #_(t/and t/indexed? (t/seq t/type?))
   *logical-complement]
  {PType          nil
   ?Fn            {invoke    ([_ x] (reduce
                                      (fn [_ t]
                                        (let [satisfies-type? (t x)]
                                          (and satisfies-type? (reduced satisfies-type?))))
                                      true ; vacuously
                                      args))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      OrType args))}
   ?Object        {hash-code ([this] (uhash/caching-set-code!    hash-code OrType args))
                   equals    ([this that]
                               (or (== this that)
                                   (and (instance? OrType that)
                                        (= args (.-args ^OrType that)))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (list* 'quantum.untyped.core.type/or args))}})

(defns or-type? [x _ > boolean?] (instance? OrType x))

(defns or-type>args [x or-type?] (.-args ^OrType x))

;; ----- AndType (`t/and` | `t/&`) ----- ;;

(udt/deftype AndType
  [^int ^:unsynchronized-mutable hash
   ^int ^:unsynchronized-mutable hash-code
   args #_(t/and t/indexed? (t/seq t/type?))
   *logical-complement]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (list* 'quantum.untyped.core.type/and args))}
   ?Fn            {invoke    ([_ x] (reduce (fn [_ t] (or (t x) (reduced false)))
                                      true ; vacuously
                                      args))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      AndType args))}
   ?Object        {hash-code ([this] (uhash/caching-set-code!    hash-code AndType args))
                   equals    ([this that]
                               (or (== this that)
                                   (and (instance? AndType that)
                                        (= args (.-args ^AndType that)))))}})

(defns and-type? [x _ > boolean?] (instance? AndType x))

(defns and-type>args [x and-type?] (.-args ^AndType x))

;; ----- Expression ----- ;;

#?(:clj (extend-protocol PType Expression))

;; ----- ProtocolType ----- ;;

(udt/deftype ProtocolType
  [^int ^:unsynchronized-mutable hash
   ^int ^:unsynchronized-mutable hash-code
   meta #_(t/? ::meta)
   p    #_t/protocol?
   name #_(t/? t/symbol?)]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (or name (list 'quantum.untyped.core.type/isa?|protocol (:on p))))}
   ?Fn            {invoke    ([_ x] (satisfies? p x))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (ProtocolType. hash hash-code meta' p name))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      ProtocolType p))}
   ?Object        {hash-code ([this] (uhash/caching-set-code!    hash-code ProtocolType p))
                   equals    ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ProtocolType that)
                                        (= p (.-p ^ProtocolType that)))))}})

(defns protocol-type? [x _] (instance? ProtocolType x))

(defns protocol-type>protocol [t protocol-type?] (.-p ^ProtocolType t))

;; ----- ClassType ----- ;;

(udt/deftype ClassType
  [^int ^:unsynchronized-mutable hash
   ^int ^:unsynchronized-mutable hash-code
          meta #_(t/? ::meta)
   ^Class c    #_t/class?
          name #_(t/? t/symbol?)]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (or name (list 'quantum.untyped.core.type/isa? c)))}
   ?Fn            {invoke    ([_ x] (instance? c x))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (ClassType. hash hash-code meta' c name))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      ClassType c))}
   ?Object        {hash-code ([this] (uhash/caching-set-code!    hash-code ClassType c))
                   equals    ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ClassType that)
                                        (= c (.-c ^ClassType that)))))}})

(defns class-type? [x _] (instance? ClassType x))

(defns class-type>class [t class-type?] (.-c ^ClassType t))

;; ----- ValueType ----- ;;

(udt/deftype ValueType
  [^int ^:unsynchronized-mutable hash
   ^int ^:unsynchronized-mutable hash-code
   v #_any?]
  {PType          nil
   fedn/IOverride nil
   fedn/IEdn      {-edn   ([this] (list 'quantum.untyped.core.type/value v))}
   ?Fn            {invoke ([_ x] (= x v))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      ValueType v))}
   ?Object        {hash-code ([this] (uhash/caching-set-code!    hash-code ValueType v))
                   equals ([this that #_any?]
                            (or (== this that)
                                (and (instance? ValueType that)
                                     (= v (.-v ^ValueType that)))))}})

(defns value-type? [x _] (instance? ValueType x))

(defns value-type>value [v value-type?] (.-v ^ValueType v))
