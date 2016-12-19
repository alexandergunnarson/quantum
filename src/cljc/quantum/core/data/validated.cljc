(ns quantum.core.data.validated
  (:require
    [clojure.core           :as core]
    [clojure.walk
      :refer [postwalk]]
    [quantum.core.core
      :refer [ns-keyword?]]
    [quantum.core.data.set  :as set]
    [quantum.core.error     :as err
      :refer [->ex TODO]]
    [quantum.core.macros.core
      :refer [if-cljs]]
    [quantum.core.macros.deftype
      :refer [deftype-compatible]]
    [quantum.core.fn
      :refer [fn-> fn->> fn1 fn$ <-]]
    [quantum.core.logic
      :refer [eq? fn-and fn-or whenf1 whenf whenp nnil? nempty?]]
    [quantum.core.log
      :refer [prl]]
    [quantum.core.macros.defrecord
      :refer [defrecord+]]
    [quantum.core.macros.optimization
      :refer [identity*]]
    [quantum.core.validate  :as v
      :refer [validate defspec]]
    [quantum.core.vars      :as var
      :refer [update-meta]])
  #?(:cljs
  (:require-macros
    [quantum.core.data.validated :as self
      :refer [def-validated-map def-validated]])))

(defn enforce-get [base-record c ks k]
  (when-not (#?@(:clj  [.containsKey ^java.util.Map base-record]
                 :cljs [contains? base-record]) k)
    (throw (->ex nil "Key is not present in ValidatedMap's spec" {:class c :k k :keyspec ks}))))

#?(:clj
(defn hash-classname [cname]
  (hash (with-meta (symbol (str (namespace-munge *ns*) "." cname)) (meta cname)))))

(defn std-equals [this-class other equals-fn]
  `([this# ~other] ; TODO currently equals requires they be an instance of this class
     (and (not (nil? ~other))
          (or (identical? this# ~other)
              (and (instance? ~this-class ~other)
                   (~equals-fn ~'v (.-v ~(with-meta other {:tag this-class}))))))))

; ===== TRANSFORMATIONS ===== ;

(defn replace-value-types
  "In spec, replaces all keywords of :db/<x> with :quantum.db.datomic.core/-<x>"
  [spec]
  (postwalk
    (whenf1 (fn-and keyword? (fn-> namespace (= "db")))
      #(keyword "quantum.db.datomic.core" (str "-" (name %))))
    spec))

(defn dbify-keyword [k]
  (validate k             keyword?
            (namespace k) nil?)
  (keyword "schema" (name k)))

(defn contextualize-keyword [k kw-context]
  (validate k keyword?)
  (cond (= (namespace k) "this")
        (keyword (str (name kw-context) ":" (name k)))
        (and (-> k namespace nil?)
             (= (name k) "this"))
        kw-context
        :else k))

(defn contextualize-keywords [spec-name spec]
  (postwalk (whenf1 keyword? #(contextualize-keyword % spec-name)) spec))

(def ^{:doc "See also |quantum.db.datomic.core/allowed-types|."}
  db-value-types
 #{:keyword
   :string
   :boolean
   :long
   :bigint
   :float
   :double
   :bigdec
   :instant
   :uuid
   :uri
   :bytes})

(def db-type
  (fn-or (fn-and keyword?
           (fn-or (fn-and (fn->> namespace (= "db"))
                          (fn->> name keyword db-value-types))
                  (fn-and (fn->> namespace (= "quantum.db.datomic.core"))
                          (fn->> name rest (apply str) keyword db-value-types))))
         (fn-and seq? (fn-> first symbol?) (fn-> first name (= "and")) ; TODO really this is core.validate/and
           (fn->> rest (filter db-type) first db-type))
         (constantly :ref)))

(defn spec->schema
  {:todo #{"Enforce validators/specs using database transaction functions"}}
  [sym spec]
  `(quantum.db.datomic.core/->schema
     (->> ~(->> `{:ident       ~(-> sym name keyword)
                  :type        ~(db-type spec)
                  :cardinality ~(if ((fn-and seq? (fn-> first symbol?) (fn-> first name (= "set-of")))
                                     spec)
                                    :many
                                    :one)
                  :doc         ~(-> sym meta :doc       )
                  :component?  ~(-> sym meta :component?)
                  :index?      ~(-> sym meta :index?    )
                  :full-text?  ~(-> sym meta :full-text?)
                  :unique      ~(-> sym meta :unique    )}
                 (remove (fn [[k v]] (nil? v)))
                 (into {}))
          (remove (fn [[k# v#]] (nil? v#)))
          (into {}))))

(defn extract-inner-defs
  "Extracts inner `def`s from a `def-validated-map` like:
   ```
   (def-validated-map ^:db? whatever
     :req [(def :this/numerator   ::a)
           (def :this/denominator ::a)])
   ```
   and places them in code outside of the block like so:
   ```
   (def-validated ^:db? whatever:numerator   ::a)
   (def-validated ^:db? whatever:denominator ::a)
   (def-validated-map ^:db? whatever
     :req [:whatever:denominator :whatever:numerator])
   ```

   Out: {:pre-code <Code to be prepended>
         :spec     <Final spec with inner defs replaced with keywords>}"
  {:todo #{"handle DB schema caching, metadata, etc."
           "more clearly handle namespaced keywords"}
   :args-doc '{db-mode?   "Whether in db mode or not"
               kw-context {:doc     "Used for expanding :this/<x> keywords"
                           :example :whatever}
               spec       {:example '{:req [(def :this/numerator   ::a)
                                            (def :this/denominator ::a)]}}}}
  [spec db-mode? kw-context]
  (validate (-> spec keys set) (fn1 set/subset? #{:req :opt :req-un :opt-un}))
  (let [to-prepend (atom [])
        inner-def? (fn-and seq? (fn-> first (= 'def)))
        ; TODO do inner validated maps too, which can have infinitely nested ones
        extract-inner-def
          (fn [x] (if (inner-def? x)
                      (let [[_ inner-name & inner-spec-args] x
                            _ (validate inner-name keyword?
                                        inner-spec-args nempty?)
                            inner-name (contextualize-keyword inner-name kw-context)
                            inner-name-sym
                              (with-meta (symbol (namespace inner-name) (name inner-name)) ; inherits :db? from parents
                                 (assoc (meta x) :db? db-mode?))
                            inner-spec (if (-> inner-spec-args count (= 1))
                                           `(def-validated ~inner-name-sym
                                              ~@(contextualize-keywords kw-context inner-spec-args))
                                           `(def-validated-map ~inner-name-sym
                                              ~@inner-spec-args))]
                        (swap! to-prepend conj inner-spec)
                        ; only left with processed keyword name of the inner def
                        (whenp inner-name db-mode? dbify-keyword))
                      ; if not inner def, assume keyword (and validate later)
                      (whenp x db-mode? dbify-keyword)))
        spec-f (->> spec
                    (map (fn [[k v]] [k (->> v (mapv extract-inner-def))]))
                    (into {}))]
    {:to-prepend @to-prepend
     :spec       spec-f}))

(defn sym->spec-name+sym [sym ns-]
  (let [db-mode?  (-> sym meta :db?)
        spec-name (keyword (or (namespace sym)
                               (if db-mode? "schema" (str (ns-name ns-))))
                           (name sym))]
    {:spec-name spec-name
     :sym       (-> (symbol (if (or (#{(str (ns-name ns-)) "schema"} (namespace spec-name)))
                                (name spec-name)
                                (str (namespace spec-name) ":" (name spec-name))))
                    (with-meta (meta sym)))}))

; ===== TOP-LEVEL MACROS ===== ;

(defonce db-schemas (atom {}))

#?(:clj
(defmacro declare-spec [sym]
  (let [{:keys [spec-name]} (sym->spec-name+sym sym *ns*)]
    `(defspec ~spec-name (fn [x#] (TODO))))))

#?(:clj
(defmacro def-validated
  "Defines a validated value."
  ([sym-0 spec-0] `(def-validated ~sym-0 ~spec-0 nil))
  ([sym-0 spec-0 conformer]
    (v/validate sym-0 symbol?)
    (let [other         (gensym "other")
          {:keys [spec-name sym]} (sym->spec-name+sym sym-0 *ns*)
          type-hash     (hash-classname sym-0)
          db-mode?      (-> sym-0 meta :db?)
          kw-context    (-> sym-0 name keyword)
          spec          (->> spec-0
                             (<- whenp db-mode? replace-value-types)
                             (<- whenf (fn-and (constantly db-mode?)
                                               keyword? (fn-> namespace nil?))
                                       dbify-keyword))
          conformer-sym (gensym "conformer")
          schema        (when db-mode? (spec->schema sym-0 spec))
          code `(do (defspec ~spec-name ~spec)
                    ~(when db-mode?  `(swap! db-schemas assoc ~spec-name ~schema))
                    ~(when conformer `(def ~conformer-sym ~conformer))
                    (deftype-compatible ~sym ~'[v]
                      {~'?Object
                        {~'hash     ([_#] (.hashCode ~'v))
                         ~'equals   ~(std-equals sym other '=)}
                       ~'?HashEq
                         {~'hash-eq ([_#] (int (bit-xor ~type-hash (~(if-cljs &env '-hash '.hashEq) ~'v))))}
                       quantum.core.core/IValue
                         {~'get     ([_#] ~'v)
                          ~'set     ([_# v#] (new ~sym (-> v# ~(if-not conformer `identity* conformer-sym)
                                                            (v/validate ~spec-name))))}})
                    (defn ~(symbol (str "->" sym)) [v#]
                      (new ~sym (v/validate v# ~spec-name))))]
      (prl ::debug db-mode? sym-0 sym spec-name spec schema code)
      code))))

#?(:clj
(defmacro def-validated-map
  "Defines a validated associative structure.
   Same semantics of `clojure.spec/keys`.
   Basically a validator on a record."
  {:usage `(do (def-validated-map MyTypeOfValidatedMap
                 :invariant #(= 5 (+ (::a %) (::b %)))
                 :conformer (fn [m] m)
                 :req [::a ::b ::c ::d] :opt [::e])
               (defspec ::a number?) (defspec ::b number?) (defspec ::c number?) (defspec ::d number?)
               (assoc (->MyTypeOfValidatedMap {::a 1 ::b 2 ::c 3 ::d 4}) ::a 3))
   :todo {1 "Break this macro up"
          2 ".assoc may call .conj, in which case it's inefficient with double validation"
          3 "allow transactional manipulation, in which multiple values can be updated at once"
          4 "incorporate invariant and conformer into schema"}}
  [sym-0 & {:keys [req opt req-un opt-un invariant conformer] :as spec-0}]
  (validate (-> spec-0 keys set) (fn1 set/subset? #{:req :opt :req-un :opt-un :invariant :conformer}))
  (validate
    sym-0 symbol?
    req (v/or* nil? vector?) req-un (v/or* nil? vector?)
    opt (v/or* nil? vector?) opt-un (v/or* nil? vector?))
  (let [db-mode?   (-> sym-0 meta :db?)
        kw-context (-> sym-0 name keyword)
        {{:keys [req opt req-un opt-un] :as spec} :spec to-prepend :to-prepend}
          (-> spec-0
              (dissoc :invariant :conformer)
              (whenp db-mode? replace-value-types)
              (extract-inner-defs db-mode? kw-context))
        invariant (contextualize-keywords kw-context invariant)
        conformer (contextualize-keywords kw-context conformer)]
    (validate
      req                            (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      opt                            (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      req-un                         (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      opt-un                         (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      (concat req opt req-un opt-un) (v/coll-of ns-keyword? :distinct true))
    (let [{:keys [spec-name sym]} (sym->spec-name+sym sym-0 *ns*)
          schema               (when db-mode? (spec->schema sym-0 nil)) ; TODO #4
          qualified-sym        (var/qualify-class sym)
          req-record-sym       (symbol (str (name sym) "__"))
          qualified-record-sym (var/qualify-class req-record-sym)
          un-record-sym        (gensym)
          all-record-sym       (gensym)
          un-ks-to-ks          (gensym)
          other                (gensym "other")
          ns-str               (str (ns-name *ns*))
          required-keys-record (with-meta (gensym "required-keys-record")
                                          {:tag qualified-record-sym})
          un-keys-record       (gensym "un-keys-record" )
          all-keys-record      (gensym "all-keys-record")
          req-un'              (mapv #(keyword (name %)) req-un)
          opt-un'              (mapv #(keyword (name %)) opt-un)
          req-ks               (concat req     req-un )
          opt-ks               (concat opt     opt-un )
          un-ks                (concat req-un' opt-un')
          un-ks-qualified      (concat req-un  opt-un )
          all-ks               (set (concat req opt req-un opt-un))
          req-ks-syms          (mapv #(symbol (namespace %) (name %)) req-ks)
          un-ks-syms           (mapv #(symbol               (name %)) un-ks )
          all-ks-syms          (mapv #(symbol (namespace %) (name %)) all-ks)
          keyspec              (vec (remove nil? [(when req    :req   ) req
                                                  (when opt    :opt   ) opt
                                                  (when req-un :req-un) req-un
                                                  (when opt-un :opt-un) opt-un]))
          conformer-sym        (gensym "conformer")
          invariant-spec-name  (keyword ns-str (name (gensym "invariant")))
          spec-sym             (gensym "keyspec")
          type-hash            (hash-classname sym)
          k-gen                (gensym "k")
          v-gen                (gensym "v")]
     (prl ::debug invariant conformer req-ks un-ks all-ks to-prepend)
     (let [code `(do (declare-spec ~sym-0)
          ~@to-prepend
          (defrecord+ ~req-record-sym ~req-ks-syms)
          (defrecord+ ~un-record-sym  ~un-ks-syms )
          (defrecord+ ~all-record-sym ~all-ks-syms)
          (defspec ~spec-name ~(if invariant
                                   `(v/and (v/keys ~@keyspec) ~invariant)
                                   `(v/keys ~@keyspec)))
          ~(when db-mode? `(swap! db-schemas assoc ~spec-name ~schema))
          ~(when invariant `(defspec ~invariant-spec-name ~invariant))
          ~(when conformer `(def ~conformer-sym ~conformer))
          (def ~required-keys-record (~(symbol (str "map->" req-record-sym)) ~(zipmap req-ks req-ks)))
          (def ~un-keys-record       (~(symbol (str "map->" un-record-sym )) ~(zipmap un-ks  un-ks )))
          (def ~all-keys-record      (~(symbol (str "map->" all-record-sym)) ~(zipmap all-ks all-ks)))
          (def ~un-ks-to-ks          ~(zipmap un-ks un-ks-qualified))
          (def ~spec-sym             ~keyspec)
          (deftype-compatible ~sym
            [~(with-meta 'v {:tag req-record-sym})]
            {~'?Seqable
              {~'seq          ([_#] (seq ~'v))}
             ~'?Record        true
             ~'?Sequential    true
             ; ?Cloneable     ([_] (#?(:clj .clone :cljs -clone) m))
             ~'?Counted
               {~'count       ([_#] (~(if-cljs &env '-count '.count) ~'v))}
             ~'?Collection
               {~'empty       ([_#] (~(if-cljs &env '-empty '.empty) ~'v))
                ~'empty!      ([_#] (throw (UnsupportedOperationException.)))
                ~'empty?      ([_#] (~(if-cljs &env nil '.isEmpty) ~'v))
                ~'equals      ~(std-equals sym other (if-cljs &env '-equiv '.equiv))
                ~'conj        ([_# [k0# v0#]]
                                (let [~k-gen (or (get ~all-keys-record k0#)
                                                 (get ~un-ks-to-ks     k0#)
                                                 (throw (->ex nil "Key not in validated map spec" {:k k0# :class '~qualified-record-sym})))
                                      ~v-gen (validate v0# ~k-gen)]
                                  (-> (new ~sym (~(if-cljs &env '-assoc '.assoc) ~'v ~k-gen ~v-gen))
                                      ~(if-not conformer `identity* conformer-sym)
                                      ~(if-not invariant `identity* `(validate ~invariant-spec-name)))))}
             ~'?Associative
               {~'assoc       ([_# k0# v0#]
                                (let [~k-gen (or (get ~all-keys-record k0#)
                                                 (get ~un-ks-to-ks     k0#)
                                                 (throw (->ex nil "Key not in validated map spec" {:k k0# :class '~qualified-record-sym})))
                                      ~v-gen (validate v0# ~k-gen)]
                                  (-> (new ~sym (~(if-cljs &env '-assoc '.assoc) ~'v ~k-gen ~v-gen))
                                      ~(if-not conformer `identity* conformer-sym)
                                      ~(if-not invariant `identity* `(validate ~invariant-spec-name)))))
                ~'assoc!      ([_# _# _#] (throw (UnsupportedOperationException.)))
                ~'merge!      ([_# _#   ] (throw (UnsupportedOperationException.)))
                ~'dissoc      ([_# k0#]
                                (let [~k-gen k0#]
                                  (when (#?@(:clj  [.containsKey ~required-keys-record]
                                             :cljs [contains? ~required-keys-record]) ~k-gen)
                                    (throw (->ex nil "Key is in ValidatedMap's required keys and cannot be dissoced"
                                                     {:class ~sym :k ~k-gen :keyspec ~spec-sym})))
                                   (-> (new ~sym (~(if-cljs &env '-dissoc '.without) ~'v ~k-gen))
                                       ~(if-not conformer `identity* conformer-sym)
                                       ~(if-not invariant `identity* `(validate ~invariant-spec-name)))))
                ~'dissoc!     ([_# _#] (throw (UnsupportedOperationException.)))
                ~'keys        ([_#] (.keySet   ~'v))
                ~'vals        ([_#] (.values   ~'v))
                ~'entries     ([_#] (.entrySet ~'v))}
             ~'?Lookup
               {~'contains?   ([_# k#] (or (~(if-cljs &env nil '.containsKey) ~'v k#)
                                           (~(if-cljs &env nil '.containsKey) ~'v (get ~un-ks-to-ks k#))))
                ~'containsv?  ([_# v#] (~(if-cljs &env nil '.containsValue) ~'v v#))
                ; Currently fully unrestricted `get`s: all "fields"/key-value pairs are public.
                ~'get        [([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (or (~(if-cljs &env '-lookup '.valAt) ~'v k#)
                                    (~(if-cljs &env '-lookup '.valAt) ~'v (get ~un-ks-to-ks k#))))
                              #_([_# k# else#] (~(if-cljs &env '-lookup '.valAt) ~'v k# else#))]
                ~'kw-get      ([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (reify clojure.lang.ILookupThunk
                                  (get [this# target#]
                                    (if (identical? (class target#) ~sym)
                                        (or (.valAt ~'v k#)
                                            (.valAt ~'v (get ~un-ks-to-ks k#)))
                                        this#))))
                ~'get-entry   ([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (~(if-cljs &env nil '.entryAt) ~'v k#))}
             ~'?Object
               {~'hash        ([_#] (.hashCode ~'v))
                ~'equals      ~(std-equals sym other (if-cljs &env '.equiv '.equiv))}
             ~'?Iterable
               {~'iterator    ([_#] (~(if-cljs &env '-iterator '.iterator) ~'v))}
             ~'?Meta
               {~'meta        ([_#] (meta ~'v))
                ~'with-meta   ([_# new-meta#] (new ~sym (with-meta ~'v new-meta#)))}
             ~'?Print
               {~'pr          ([_# w# opts#] (-pr-writer ~'v w# opts#))}
             ~'?HashEq
               {~'hash-eq     ([_#] (int (bit-xor ~type-hash (~(if-cljs &env '-hash '.hashEq) ~'v))))}
             quantum.core.core/IValue
               {~'get         ([_#] ~'v)
                ~'set         ([_# v#] (new ~sym (v/validate v# ~spec-name)))}})
          (defn ~(symbol (str "->" sym)) [m#]
            (let [m-f# (if (instance? ~qualified-record-sym m#)
                           m# ; no coercion needed
                           (~(symbol (str "map->" req-record-sym))
                            (-> m#
                                ~(if-not conformer `identity* conformer-sym)
                                (v/validate ~spec-name)
                                (set/rename-keys ~un-ks-to-ks))))]
              (new ~qualified-sym m-f#)))
          ~(if-cljs &env qualified-sym `(import (quote ~qualified-sym))))]
     (prl ::debug code)
     code)))))

; TODO validated vector, set, and (maybe) list
; Not sure what else might be useful to create a validated wrapper for... I mean, queues I guess

(def ^{:doc "See also Datomic's documentation."}
  allowed-types
 #{:keyword
   :string
   :boolean
   :long
   :bigint
   :float
   :double
   :bigdec
   :ref
   :instant
   :uuid
   :uri
   :bytes})

(quantum.core.log/enable! ::debug)
(quantum.core.log/enable! :quantum.core.macros.defrecord/debug)

(quantum.core.macros.defrecord/defrecord+
        intermediate-schema1111__
        [datomic:schema1111/ident
         datomic:schema1111/type
         datomic:schema1111/cardinality])

#_(do
       (quantum.core.data.validated/declare-spec
        intermediate-schema1111)
       #_(quantum.core.data.validated/def-validated
        datomic:schema1111/ident
        keyword?)
       #_(quantum.core.data.validated/def-validated
        datomic:schema1111/type
        allowed-types)
       #_(quantum.core.data.validated/def-validated
        datomic:schema1111/cardinality
        #{:one :many})

       (quantum.core.macros.defrecord/defrecord+
        G__130859
        [ident type cardinality])
       (quantum.core.macros.defrecord/defrecord+
        G__130860
        [datomic:schema1111/cardinality
         datomic:schema1111/type
         datomic:schema1111/ident])
       (quantum.core.validate/defspec
        :quantum.core.data.validated/intermediate-schema1111
        (quantum.core.validate/keys
         :req-un
         [:datomic:schema1111/ident
          :datomic:schema1111/type
          :datomic:schema1111/cardinality]))
       nil
       nil
       nil
       (def
        required-keys-record130863
        (map->intermediate-schema1111__
         {:datomic:schema1111/ident :datomic:schema1111/ident,
          :datomic:schema1111/type :datomic:schema1111/type,
          :datomic:schema1111/cardinality :datomic:schema1111/cardinality}))
       (def
        un-keys-record130864
        (map->G__130859
         {:ident :ident, :type :type, :cardinality :cardinality}))
       (def
        all-keys-record130865
        (map->G__130860
         {:datomic:schema1111/cardinality :datomic:schema1111/cardinality,
          :datomic:schema1111/type :datomic:schema1111/type,
          :datomic:schema1111/ident :datomic:schema1111/ident}))
       (def
        G__130861
        {:ident :datomic:schema1111/ident,
         :type :datomic:schema1111/type,
         :cardinality :datomic:schema1111/cardinality})
       (def
        keyspec130868
        [:req-un
         [:datomic:schema1111/ident
          :datomic:schema1111/type
          :datomic:schema1111/cardinality]])
       (quantum.core.macros.deftype/deftype-compatible
        intermediate-schema1111
        [v]
        {?Associative {dissoc ([___130431__auto__ k0__130436__auto__]
                               (clojure.core/let
                                [k130869 k0__130436__auto__]
                                (clojure.core/when
                                 (.containsKey
                                  required-keys-record130863
                                  k130869)
                                 (throw
                                  (quantum.core.error/->ex
                                   nil
                                   "Key is in ValidatedMap's required keys and cannot be dissoced"
                                   {:keyspec keyspec130868,
                                    :k k130869,
                                    :class intermediate-schema1111})))
                                (clojure.core/->
                                 (new
                                  intermediate-schema1111
                                  (.-dissoc v k130869))
                                 quantum.core.macros.optimization/identity*
                                 quantum.core.macros.optimization/identity*))),
                       merge! ([___130431__auto__ ___130431__auto__]
                               (throw
                                (java.lang.UnsupportedOperationException.))),
                       entries ([___130431__auto__] (.entrySet v)),
                       dissoc! ([___130431__auto__ ___130431__auto__]
                                (throw
                                 (java.lang.UnsupportedOperationException.))),
                       assoc! ([___130431__auto__
                                ___130431__auto__
                                ___130431__auto__]
                               (throw
                                (java.lang.UnsupportedOperationException.))),
                       vals ([___130431__auto__] (.values v)),
                       keys ([___130431__auto__] (.keySet v)),
                       assoc ([___130431__auto__
                               k0__130436__auto__
                               v0__130437__auto__]
                              (clojure.core/let
                               [k130869
                                (clojure.core/or
                                 (clojure.core/get
                                  all-keys-record130865
                                  k0__130436__auto__)
                                 (clojure.core/get
                                  G__130861
                                  k0__130436__auto__)
                                 (throw
                                  (quantum.core.error/->ex
                                   nil
                                   "Key not in validated map spec"
                                   {:k k0__130436__auto__,
                                    :class (quote
                                            quantum.core.data.validated.intermediate-schema1111__)})))
                                v130870
                                (quantum.core.validate/validate
                                 v0__130437__auto__
                                 k130869)]
                               (clojure.core/->
                                (new
                                 intermediate-schema1111
                                 (.-assoc v k130869 v130870))
                                quantum.core.macros.optimization/identity*
                                quantum.core.macros.optimization/identity*)))},
         ?Counted {count ([___130431__auto__] (.-count v))},
         ?Collection {conj ([___130431__auto__
                             [k0__130436__auto__ v0__130437__auto__]]
                            (clojure.core/let
                             [k130869
                              (clojure.core/or
                               (clojure.core/get
                                all-keys-record130865
                                k0__130436__auto__)
                               (clojure.core/get
                                G__130861
                                k0__130436__auto__)
                               (throw
                                (quantum.core.error/->ex
                                 nil
                                 "Key not in validated map spec"
                                 {:k k0__130436__auto__,
                                  :class (quote
                                          quantum.core.data.validated.intermediate-schema1111__)})))
                              v130870
                              (quantum.core.validate/validate
                               v0__130437__auto__
                               k130869)]
                             (clojure.core/->
                              (new
                               intermediate-schema1111
                               (.-assoc v k130869 v130870))
                              quantum.core.macros.optimization/identity*
                              quantum.core.macros.optimization/identity*))),
                      #_empty? #_([___130431__auto__] (nil v)),
                      empty ([___130431__auto__] (.-empty v)),
                      equals ([this__130279__auto__ other130862]
                              (clojure.core/and
                               (clojure.core/not
                                (clojure.core/nil? other130862))
                               (clojure.core/or
                                (clojure.core/identical?
                                 this__130279__auto__
                                 other130862)
                                (clojure.core/and
                                 (clojure.core/instance?
                                  intermediate-schema1111
                                  other130862)
                                 (.-equiv v (.-v other130862)))))),
                      empty! ([___130431__auto__]
                              (throw
                               (java.lang.UnsupportedOperationException.)))},
         ?Lookup {#_contains? #_([___130431__auto__ k__130432__auto__]
                             (clojure.core/or
                              (nil v k__130432__auto__)
                              (nil
                               v
                               (clojure.core/get
                                G__130861
                                k__130432__auto__)))),
                  get [([___130431__auto__ k__130432__auto__]
                        (clojure.core/or
                         (.-lookup v k__130432__auto__)
                         (.-lookup
                          v
                          (clojure.core/get
                           G__130861
                           k__130432__auto__))))],
                  #_containsv? #_([___130431__auto__ v__130433__auto__]
                              (nil v v__130433__auto__)),
                  kw-get ([___130431__auto__ k__130432__auto__]
                          (clojure.core/reify
                           clojure.lang.ILookupThunk
                           (clojure.core/get
                            [this__130434__auto__
                             target__130435__auto__]
                            (if
                             (clojure.core/identical?
                              (clojure.core/class
                               target__130435__auto__)
                              intermediate-schema1111)
                             (clojure.core/or
                              (.valAt v k__130432__auto__)
                              (.valAt
                               v
                               (clojure.core/get
                                G__130861
                                k__130432__auto__)))
                             this__130434__auto__)))),
                  #_get-entry #_([___130431__auto__ k__130432__auto__]
                             (nil v k__130432__auto__))},
         ?Print {pr ([___130431__auto__
                      w__130439__auto__
                      opts__130440__auto__]
                     (.-pr-writer
                      v
                      w__130439__auto__
                      opts__130440__auto__))},
         ?Object {hash ([___130431__auto__] (.hashCode v)),
                  equals ([this__130279__auto__ other130862]
                          (clojure.core/and
                           (clojure.core/not
                            (clojure.core/nil? other130862))
                           (clojure.core/or
                            (clojure.core/identical?
                             this__130279__auto__
                             other130862)
                            (clojure.core/and
                             (clojure.core/instance?
                              intermediate-schema1111
                              other130862)
                             (.equiv v (.-v other130862))))))},
         ?Meta {meta ([___130431__auto__] (clojure.core/meta v)),
                with-meta ([___130431__auto__
                            new-meta__130438__auto__]
                           (new
                            intermediate-schema1111
                            (clojure.core/with-meta
                             v
                             new-meta__130438__auto__)))},
         ?Sequential true,
         quantum.core.core/IValue {set ([___130431__auto__
                                         v__130433__auto__]
                                        (new
                                         intermediate-schema1111
                                         (quantum.core.validate/validate
                                          v__130433__auto__
                                          :quantum.core.data.validated/intermediate-schema1111))),
                                   get ([___130431__auto__] v)},
         ?Record true,
         ?Iterable {iterator ([___130431__auto__] (.-iterator v))},
         ?HashEq {hash-eq ([___130431__auto__]
                           (clojure.core/int
                            (clojure.core/bit-xor
                             -805031456
                             (.-hash v))))},
         ?Seqable {seq ([___130431__auto__] (clojure.core/seq v))}})
       (clojure.core/defn
        ->intermediate-schema1111
        [m__130441__auto__]
        (clojure.core/let
         [m-f__130442__auto__
          (if
           (clojure.core/instance?
            quantum.core.data.validated.intermediate-schema1111__
            m__130441__auto__)
           m__130441__auto__
           (map->intermediate-schema1111__
            (clojure.core/->
             m__130441__auto__
             quantum.core.macros.optimization/identity*
             (quantum.core.validate/validate
              :quantum.core.data.validated/intermediate-schema1111)
             (quantum.core.data.set/rename-keys G__130861))))]
         (new
          quantum.core.data.validated.intermediate-schema1111
          m-f__130442__auto__)))
       quantum.core.data.validated.intermediate-schema1111)

(def-validated-map intermediate-schema1111
  #_:invariant
  #_#(if (:datomic:schema1111/component? %)
                  (-> % :datomic:schema1111/type (= :ref))
                  true)

  :req-un [(def :datomic:schema1111/ident       keyword?)
           (def :datomic:schema1111/type        allowed-types)
           (def :datomic:schema1111/cardinality #{:one :many})])

#?(:cljs (throw (->ex nil "Done" nil)))
