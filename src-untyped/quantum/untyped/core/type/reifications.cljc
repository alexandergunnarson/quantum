(ns quantum.untyped.core.type.reifications
  "Some notes on these types:
  - Here `c/=` tests for structural equivalence
  - If a type is named, its name must be a qualified symbol and is assumed to be globally
    resolvable.
  - If you define a named type, `>form` it, and redefine the type, the value will change when you
    `eval` the form"
          (:refer-clojure :exclude
            [==])
          (:require
            [clojure.core                               :as core]
            [clojure.set                                :as set]
            [fipp.ednize                                :as fedn]
            [quantum.untyped.core.analyze.expr
#?@(:cljs    [:refer [Expression]])]
            [quantum.untyped.core.collections
              :refer [>vec]]
            [quantum.untyped.core.collections.logic
              :refer [seq-and-2]]
            [quantum.untyped.core.compare
              :refer [== not==]]
            [quantum.untyped.core.core                  :as ucore]
            [quantum.untyped.core.data.hash             :as uhash]
            [quantum.untyped.core.data.reactive         :as urx]
            [quantum.untyped.core.defnt
              :refer [defns]]
            [quantum.untyped.core.error
              :refer [err! TODO]]
            [quantum.untyped.core.form                  :as uform
              :refer [>form]]
            [quantum.untyped.core.form.generate.deftype :as udt]
            [quantum.untyped.core.identifiers
              :refer [>symbol]]
            [quantum.untyped.core.loops
              :refer [reduce-2]]
            [quantum.untyped.core.numeric               :as unum]
            [quantum.untyped.core.print] ; for fipp.edn extensions
            [quantum.untyped.core.refs                  :as uref
              :refer [!]]
            [quantum.untyped.core.spec                  :as us])
 #?(:clj  (:import
            [quantum.untyped.core.analyze.expr Expression])))

(ucore/log-this-ns)

(defprotocol PType (with-name [this name']))

(defn type? [x #_> #_boolean?] (satisfies? PType x))

(defn- ?with-name [form ?name] (if ?name (list 't/named ?name form) form))

;; ----- MetaType ----- ;;

(udt/deftype MetaType
  [         meta #_(t/? ::meta)
            name #_qualified-symbol?
            t    #_t/type?
   ^boolean assume?
   ^boolean ref?
   ^boolean runtime?]
  {PType          {with-name ([this name'] (MetaType. meta name' t assume? ref? runtime?))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (MetaType. meta' name t assume? ref? runtime?))}
   ?Equals        {=         ([this that] (or (== this that)
                                              (if (instance? MetaType that)
                                                  (= t (.-t ^MetaType that))
                                                  (= that t))))}
   uform/PGenForm {>form     ([this] (or name
                                         (list 'new 'quantum.untyped.core.type.reifications.MetaType
                                           (>form meta) name (>form t) assume? ref? runtime?)))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (cond->> (fedn/-edn t)
                                           assume?  (list 't/assume)
                                           ref?     (list 't/ref)
                                           runtime? (list 't/*))
                                         (?with-name name)))}})

(defns meta-type? [x _ > boolean?] (instance? MetaType x))

(defns meta-type>inner-type [t meta-type?] (.-t ^MetaType t))

;; ----- UniversalSetType (`t/U`) ----- ;;

(udt/deftype
  ^{:doc "Represents the set of all sets that do not include themselves (including the empty set).
          Equivalent to `(constantly true)`."}
  UniversalSetType [meta #_(t/? ::meta)]
  {PType          {with-name ([this _] this)}
   ?Fn            {invoke    ([_ x] true)}
   ?Hash          {hash      ([this] (hash       UniversalSetType))
                   hash-code ([this] (uhash/code UniversalSetType))}
   ?Equals        {=         ([this that] (or (== this that) (instance? UniversalSetType that)))}
   uform/PGenForm {>form     ([this] 'quantum.untyped.core.type/any?)}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] 't/any?)}})

(def universal-set (UniversalSetType. nil))

;; ----- EmptySetType (`t/∅`) ----- ;;

(udt/deftype
  ^{:doc "Represents the empty set.
          Equivalent to `(constantly false)`."}
  EmptySetType [meta #_(t/? ::meta)]
  {PType          {with-name ([this _] this)}
   ?Fn            {invoke    ([_ x] false)}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (EmptySetType. meta'))}
   ?Hash          {hash      ([this] (hash       EmptySetType))
                   hash-code ([this] (uhash/code EmptySetType))}
   ?Equals        {=         ([this that] (or (== this that) (instance? EmptySetType that)))}
   uform/PGenForm {>form     ([this] 'quantum.untyped.core.type/none?)}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] 't/none?)}})

(def empty-set (EmptySetType. nil))

;; ----- NotType (`t/not` / `t/!`) ----- ;;

(udt/deftype NotType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta #_(t/? ::meta)
   name #_(t/? qualified-symbol?)
   t    #_t/type?]
  {PType          {with-name ([this name'] (NotType. hash hash-code meta name' t))}
   ?Fn            {invoke    ([_ x] (not (t x)))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (NotType. hash hash-code meta' name t))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      NotType t))
                   hash-code ([this] (uhash/caching-set-code!    hash-code NotType t))}
   ?Equals        {=         ([this that]
                               (or (== this that)
                                   (and (instance? NotType that)
                                        (= t (.-t ^NotType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name (list 'new 'quantum.untyped.core.type.reifications.NotType
                                          hash hash-code (>form meta) name (>form t))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/not (fedn/-edn t)) (?with-name name)))}})

(defns not-type? [x _ > boolean?] (instance? NotType x))

(defns not-type>inner-type [t not-type?] (.-t ^NotType t))

;; ----- OrType (`t/or` / `t/|`) ----- ;;

(udt/deftype OrType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta #_(t/? ::meta)
   name #_(t/? qualified-symbol?)
   args #_(t/and t/indexed? (t/seq t/type?))
   *logical-complement]
  {PType          {with-name ([this name'] (OrType. hash hash-code meta name' args
                                                    *logical-complement))}
   ?Fn            {invoke    ([_ x] (reduce
                                      (fn [_ t]
                                        (let [satisfies-type? (t x)]
                                          (and satisfies-type? (reduced satisfies-type?))))
                                      true ; vacuously
                                      args))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (OrType. hash hash-code meta' name args
                                                    *logical-complement))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      OrType args))
                   hash-code ([this] (uhash/caching-set-code!    hash-code OrType args))}
   ?Equals        {=         ([this that]
                               (or (== this that)
                                   (and (instance? OrType that)
                                        (= args (.-args ^OrType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name (list 'new 'quantum.untyped.core.type.reifications.OrType
                                          hash hash-code (>form meta) name (-> args >vec >form)
                                          `(atom nil))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list* 't/or (map fedn/-edn args)) (?with-name name)))}})

(defns or-type? [x _ > boolean?] (instance? OrType x))

(defns or-type>args [x or-type?] (.-args ^OrType x))

;; ----- AndType (`t/and` | `t/&`) ----- ;;

(udt/deftype AndType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta #_(t/? ::meta)
   name #_(t/? qualified-symbol?)
   args #_(t/and t/indexed? (t/seq t/type?))
   *logical-complement]
  {PType          {with-name ([this name'] (AndType. hash hash-code meta name' args
                                                     *logical-complement))}
   ?Fn            {invoke    ([_ x] (reduce (fn [_ t] (or (t x) (reduced false)))
                                      true ; vacuously
                                      args))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (AndType. hash hash-code meta' name args
                                                     *logical-complement))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      AndType args))
                   hash-code ([this] (uhash/caching-set-code!    hash-code AndType args))}
   ?Equals        {=         ([this that]
                               (or (== this that)
                                   (and (instance? AndType that)
                                        (= args (.-args ^AndType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name (list 'new 'quantum.untyped.core.type.reifications.AndType
                                          hash hash-code (>form meta) name (-> args >vec >form)
                                          `(atom nil))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list* 't/and (map fedn/-edn args)) (?with-name name)))}})

(defns and-type? [x _ > boolean?] (instance? AndType x))

(defns and-type>args [x and-type?] (.-args ^AndType x))

;; ----- Expression ----- ;;

#?(:clj (extend-protocol PType Expression))

;; ----- ProtocolType ----- ;;

(udt/deftype ProtocolType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta #_(t/? ::meta)
   name #_(t/? qualified-symbol?)
   p    #_t/protocol?]
  {PType          {with-name ([this name'] (ProtocolType. hash hash-code meta name' p))}
   ?Fn            {invoke    ([_ x] (satisfies? p x))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (ProtocolType. hash hash-code meta' name p))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      ProtocolType p))
                   hash-code ([this] (uhash/caching-set-code!    hash-code ProtocolType p))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ProtocolType that)
                                        (= p (.-p ^ProtocolType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name (list 'new
                                          'quantum.untyped.core.type.reifications.ProtocolType
                                          hash hash-code (>form meta) name (-> p :var >symbol))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/isa?|protocol (-> p :var >symbol))
                                         (?with-name name)))}})

(defns protocol-type? [x _] (instance? ProtocolType x))

(defns protocol-type>protocol [t protocol-type?] (.-p ^ProtocolType t))

;; ----- DirectProtocolType ----- ;;

#?(:cljs
(udt/deftype
  ^{:doc "Differs from `ProtocolType` in that an `implements?` check is performed instead of a
          `satisfies?` check, i.e. native-type protocol dispatch is ignored."}
  DirectProtocolType
  [^number ^:! hash
   ^number ^:! hash-code
   meta #_(t/? ::meta)
   name #_(t/? qualified-symbol?)
   p    #_t/protocol?]
  {PType          {with-name ([this name'] (DirectProtocolType. hash hash-code meta name' p))}
   ?Fn            {invoke    ([_ x] (implements? p x))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (DirectProtocolType. hash hash-code meta' name p))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      DirectProtocolType p))
                   hash-code ([this] (uhash/caching-set-code!    hash-code DirectProtocolType p))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? DirectProtocolType that)
                                        (= p (.-p ^DirectProtocolType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name (list 'new
                                          'quantum.untyped.core.type.reifications.DirectProtocolType
                                          hash hash-code (>form meta) name (-> p :var >symbol))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/isa?|protocol|direct (-> p :var >symbol))
                                         (?with-name name)))}}))

#?(:cljs (defns direct-protocol-type? [x _] (instance? DirectProtocolType x)))

#?(:cljs (defns direct-protocol-type>protocol [t direct-protocol-type?] (.-p t)))

;; ----- ClassType ----- ;;

(udt/deftype ClassType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta     #_meta/meta?
   name     #_(t/? qualified-symbol?)
   ^Class c #_t/class?]
  {PType          {with-name ([this name'] (ClassType. hash hash-code meta name' c))}
   ?Fn            {invoke    ([_ x] (instance? c x))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (ClassType. hash hash-code meta' name c))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      ClassType c))
                   hash-code ([this] (uhash/caching-set-code!    hash-code ClassType c))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ClassType that)
                                        (= c (.-c ^ClassType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name (list 'new 'quantum.untyped.core.type.reifications.ClassType
                                          hash hash-code (>form meta) name (>form c))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/isa? (fedn/-edn c)) (?with-name name)))}})

(defns class-type? [x _] (instance? ClassType x))

(defns class-type>class [t class-type?] (.-c ^ClassType t))

;; ----- UnorderedType ----- ;;

(defn- satisfies-unordered-type? [xs data]
  (and (seqable? xs) ; TODO `dc/reducible?`
       (let [!frequencies (! {})
             each-input-matches-one-type-not-exceeding-frequency?
               (->> xs
                    (reduce
                      (fn [each-input-matches-one-type-not-exceeding-frequency? x]
                        (->> data
                             (reduce-kv
                               (fn [input-matches-one-type? t freq]
                                 (if (t x)
                                     (do (uref/update! !frequencies #(update % t unum/inc-default))
                                         (if (> (get @!frequencies t) (get data t))
                                             (reduced (reduced false))
                                             true))
                                     input-matches-one-type?))
                               false)))
                      true))]
         (and each-input-matches-one-type-not-exceeding-frequency?
              (= @!frequencies data)))))

(udt/deftype UnorderedType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta #_meta/meta?
   name #_(t/? qualified-symbol?)
   data #_(t/type (dc/map-of t/type? (t/and integer? (> 1))) "Val is frequency of type")]
  {PType          {with-name ([this name'] (UnorderedType. hash hash-code meta name' data))}
   ?Fn            {invoke    ([_ xs] (satisfies-unordered-type? xs data))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (UnorderedType. hash hash-code meta' name data))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      UnorderedType data))
                   hash-code ([this] (uhash/caching-set-code!    hash-code UnorderedType data))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? UnorderedType that)
                                        (= data (.-data ^UnorderedType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name
                                   (list 'new 'quantum.untyped.core.type.reifications.UnorderedType
                                     hash hash-code (>form meta) name (>form data))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/unordered (fedn/-edn data)) (?with-name name)))}})

(defn unordered-type? [x] (instance? UnorderedType x))

(defns unordered-type>data [t unordered-type?] (.-data ^UnorderedType t))

;; ----- OrderedType ----- ;;

(udt/deftype OrderedType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta #_meta/meta?
   name #_(t/? qualified-symbol?)
   data #_dc/sequential?]
  {PType          {with-name ([this name'] (OrderedType. hash hash-code meta name' data))}
   ?Fn            {invoke    ([_ xs] (and (seqable? xs) ; TODO `dc/reducible?`
                                          (seq-and-2 (fn [t x] (t x))
                                            (sequence data) (sequence xs))))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (OrderedType. hash hash-code meta' name data))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      OrderedType data))
                   hash-code ([this] (uhash/caching-set-code!    hash-code OrderedType data))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? OrderedType that)
                                        (= data (.-data ^OrderedType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name
                                   (list 'new 'quantum.untyped.core.type.reifications.OrderedType
                                     hash hash-code (>form meta) name (-> data >vec >form))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/ordered (fedn/-edn data)) (?with-name name)))}})

(defn ordered-type? [x] (instance? OrderedType x))

(defns ordered-type>data [t ordered-type?] (.-data ^OrderedType t))

;; ----- ValueType ----- ;;

(udt/deftype ValueType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta #_(t/? ::meta)
   name #_(t/? qualified-symbol?)
   v    #_any?]
  {PType          {with-name ([this name'] (ValueType. hash hash-code meta name' v))}
   ?Fn            {invoke    ([_ x] (= x v))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (ValueType. hash hash-code meta' name v))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      ValueType v))
                   hash-code ([this] (uhash/caching-set-code!    hash-code ValueType v))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ValueType that)
                                        (= v (.-v ^ValueType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name (list 'new 'quantum.untyped.core.type.reifications.ValueType
                                          hash hash-code (>form meta) name (>form v))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/value (fedn/-edn v)) (?with-name name)))}})

(defns value-type? [x _] (instance? ValueType x))

(defns value-type>value [v value-type?] (.-v ^ValueType v))

;; ----- FnType ----- ;;

;; TODO add `hash` and `hash-code`
(udt/deftype FnType
  [meta         #_(t/? ::meta)
   name         #_(t/? qualified-symbol?)
   output-type  #_t/type?
   arities-form
   arities      #_(s/map-of nneg-int? (s/seq-of (s/kv {:input-types (s/vec-of type?)
                                                       :output-type type?})))]
  {PType          {with-name ([this name'] (FnType. meta name' output-type arities-form arities))}
   ;; Outputs whether the args match any input spec
   ?Fn            {invoke    ([this args] (TODO))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (FnType. meta' name output-type arities-form arities))}
   uform/PGenForm {>form     ([this] (or name
                                         (list 'new 'quantum.untyped.core.type.reifications.FnType
                                           (>form meta) name (>form output-type)
                                           (>form arities-form) (>form arities))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn ([this] (-> (list* 't/ftype (fedn/-edn output-type)
                                                    (fedn/-edn arities-form))
                                    (?with-name name)))}})

(defns fn-type? [x _ > boolean?] (instance? FnType x))

(defns fn-type>name [^FnType x fn-type?] (.-name x))

(defns fn-type>arities [^FnType x fn-type?] (.-arities x))

(defns fn-type>output-type [^FnType x fn-type?] (.-output-type x))

(us/def :quantum.untyped.core.type/fn-type|arity
  (us/and
    (us/cat
      :input-types      (us/* type?)
      :output-type-pair (us/? (us/cat :ident #{:>} :type type?)))
    (us/conformer
      (fn [x] (-> x (update :output-type-pair :type)
                    (update :input-types >vec)
                    (set/rename-keys {:output-type-pair :output-type}))))))

;; ----- MetaOrType ----- ;;

(udt/deftype MetaOrType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
   meta  #_(t/? ::meta)
   name  #_(t/? qualified-symbol?)
   types #_(t/seq-of form?)]
  {PType          {with-name ([this name'] (MetaOrType. hash hash-code meta name' types))}
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta'] (MetaOrType. hash hash-code meta' name types))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      MetaOrType types))
                   hash-code ([this] (uhash/caching-set-code!    hash-code MetaOrType types))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? MetaOrType that)
                                        (= types (.-types ^MetaOrType that)))))}
   uform/PGenForm {>form     ([this]
                               (or name
                                   (list 'new 'quantum.untyped.core.type.reifications.MetaOrType
                                     hash hash-code (>form meta) name (>form types))))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/meta-or types) (?with-name name)))}})

(defn meta-or-type? [x] (instance? MetaOrType x))

(defns meta-or-type>types [^MetaOrType t meta-or-type?] (.-types t))

;; ----- ReactiveType ----- ;;

(declare rx-type?)

(defn- validate-type [x]
  (or (and (type? x) (not (rx-type? x)))
      (err! "Found invalid value when derefing `ReactiveType`"
            {:kind (core/type x)})))

(udt/deftype ReactiveType
  [#?(:clj ^int ^:! hash      :cljs ^number ^:! hash)
   #?(:clj ^int ^:! hash-code :cljs ^number ^:! hash-code)
       meta          #_(t/? ::meta)
       name          #_(t/? qualified-symbol?)
       body-codelist #_(t/seq-of form?)
   ^:! v             #_(t/? type?)
       rx            #_(t/isa? urx/PReactive)]
  {PType          {with-name ([this name']
                               (ReactiveType. hash hash-code meta name' body-codelist v rx))}
   urx/PReactive  nil
   ?Meta          {meta      ([this] meta)
                   with-meta ([this meta']
                               (ReactiveType. hash hash-code meta' name body-codelist v rx))}
   ?Hash          {hash      ([this] (uhash/caching-set-ordered! hash      ReactiveType rx))
                   hash-code ([this] (uhash/caching-set-code!    hash-code ReactiveType rx))}
   ?Equals        {=         ([this that #_any?]
                               (or (== this that)
                                   (and (instance? ReactiveType that)
                                        (= rx (.-rx ^ReactiveType that)))))}
   ?Deref         {deref     ([this] (doto @rx validate-type))}
   uform/PGenForm {>form     ([this]
                               (or name (err! "Can't call `>form` on anonymous reactive type"
                                              {:t this})))}
   fedn/IOverride nil
   fedn/IEdn      {-edn      ([this] (-> (list 't/reactive-type {:value (urx/norx-deref this)})
                                         (?with-name name)))}})

(defn rx-type? [x] (instance? ReactiveType x))

(defn deref-when-reactive [x] (if (rx-type? x) @x x))
