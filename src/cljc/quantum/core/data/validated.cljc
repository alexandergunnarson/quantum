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
    [quantum.core.log       :as log
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

; TODO un-namespaced (req-un) should accept namespaced as well
; TODO Every entity can have an :db/ident or :db/conforms-to
; That way if we want to look for all providers we can just do a lookup on :conforms-to

; For database-based validated objects, here is the responsibility of each:
; Peer/client:
; - All single/per-attribute validation (unless explicitly assigned to transactor as well)
; - :conformer (unless explicitly assigned to transactor as well)
; Transactor
; - Cross-attribute validation (as defined by :invariant)
; - Cross-entity validation (as defined by :db-invariant)

; TODO get to this when possible

; ; FOR ENTITIES
;   (if (or (instance? ~class-name ~args-sym)
;                     (identifier? ~args-sym)
;                     (lookup?     ~args-sym)
;                     (dbfn-call?  ~args-sym))
;                 ~args-sym
;                 )
; ; FOR ATTRIBUTES
;                 (or (instance? ~class-name ~v-0)
;                     (and ~(= type :ref) (identifier? ~v-0))
;                     (lookup?    ~v-0)
;                     (dbfn-call? ~v-0)
;                     (nil?       ~v-0))
;                 ~v-0

;                 (def identifier? (fn-or keyword? integer?
;                         #?(:clj #(instance? datomic.db.DbId %))))

; (def lookup? (fn-and vector? (fn->  first keyword?)))

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
                  :doc         ~(-> sym meta :doc        )
                  :component?  ~(-> sym meta :component? )
                  :index?      ~(-> sym meta :index?     )
                  :full-text?  ~(-> sym meta :full-text? )
                  :unique      ~(-> sym meta :unique     )
                  :no-history? ~(-> sym meta :no-history?)}
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
  {:todo #{"handle DB schema caching, schema metadata, etc."
           "more clearly handle namespaced keywords"
           "warn when non-inner-defs (normal spec-keys) retain history while
            the parent sym doesn't"}
   :args-doc '{db-mode?   "Whether in db mode or not"
               kw-context {:doc     "Used for expanding :this/<x> keywords"
                           :example :whatever}
               spec       {:example '{:req [(def :this/numerator   ::a)
                                            (def :this/denominator ::a)]}}}}
  [spec parent-sym db-mode? kw-context]
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
                                 (assoc (meta x)
                                   :db?         db-mode?
                                   :no-history? (-> parent-sym meta :no-history?)))
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
  {:usage `(do (def-validated-map  ^:db? MyTypeOfValidatedMap
                 :invariant    #(= 5 (+ (::a %) (::b %)))
                 :db-invariant ([db m] m)
                 :conformer    (fn [m] (assoc m ::a 6))
                 :req [::a ::b ::c ::d] :opt [::e])
               (defspec ::a number?) (defspec ::b number?) (defspec ::c number?) (defspec ::d number?)
               (assoc (->MyTypeOfValidatedMap {::a 1 ::b 2 ::c 3 ::d 4}) ::a 3))
   :todo {1 "Break this macro up"
          2 ".assoc may call .conj, in which case it's inefficient with double validation"
          3 "allow transactional manipulation, in which multiple values can be updated at once"
          4 "incorporate invariant (and conformer?) into schema"
          5 "add db-invariant into schema and used with `defn!`"}}
  [sym-0 & {:keys [req opt req-un opt-un invariant db-invariant conformer] :as spec-0}]
  (validate (-> spec-0 keys set) (fn1 set/subset? #{:req :opt :req-un :opt-un :invariant :db-invariant :conformer}))
  (validate
    sym-0 symbol?
    req (v/or* nil? vector?) req-un (v/or* nil? vector?)
    opt (v/or* nil? vector?) opt-un (v/or* nil? vector?))
  (let [db-mode?   (-> sym-0 meta :db?)
        _          (when db-invariant (assert db-mode?))
        kw-context (-> sym-0 name keyword)
        {{:keys [req opt req-un opt-un] :as spec} :spec to-prepend :to-prepend}
          (-> spec-0
              (dissoc :invariant :db-invariant :conformer)
              (whenp db-mode? replace-value-types)
              (extract-inner-defs sym-0 db-mode? kw-context))
        invariant (contextualize-keywords kw-context invariant)
        conformer (contextualize-keywords kw-context conformer)]
    (validate
      req                            (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      opt                            (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      req-un                         (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      opt-un                         (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      (concat req opt req-un opt-un) (v/coll-of ns-keyword? :distinct true))
    (let [{:keys [spec-name sym]} (sym->spec-name+sym sym-0 *ns*)
          schema               (when db-mode? (spec->schema sym-0 nil)) ; TODO #4, #5
          qualified-sym        (var/qualify-class sym)
          req-record-sym       (symbol (str (name sym) "__"))
          qualified-record-sym (var/qualify-class req-record-sym)
          un-record-sym        (gensym)
          all-mod-record-sym   (gensym)
          all-record-sym       (gensym)
          un-ks-to-ks          (gensym)
          other                (gensym "other")
          ns-str               (str (ns-name *ns*))
          required-keys-record (with-meta (gensym "required-keys-record")
                                          {:tag qualified-record-sym})
          un-keys-record       (gensym "un-keys-record"     )
          all-mod-keys-record  (gensym "all-mod-keys-record")
          all-keys-record      (gensym "all-keys-record"    )
          req-un'              (mapv #(keyword (name %)) req-un)
          opt-un'              (mapv #(keyword (name %)) opt-un)
          special-ks           (when db-mode? #{:schema/type :db/id :db/ident})
          req-ks               (concat req     req-un )
          opt-ks               (concat opt     opt-un )
          un-ks                (concat req-un' opt-un')
          un-ks-qualified      (concat req-un  opt-un )
          all-mod-ks           (set (concat req opt req-un opt-un))
          special-ks-syms      (mapv #(symbol (namespace %) (name %)) special-ks)
          req-ks-syms          (mapv #(symbol (namespace %) (name %)) req-ks    )
          un-ks-syms           (mapv #(symbol               (name %)) un-ks     )
          all-mod-ks-syms      (mapv #(symbol (namespace %) (name %)) all-mod-ks)
          concatv              #(when-let [v (concat %1 %2)] (vec v))
          keyspec              (vec (remove nil? [(when req    :req   ) req
                                                  (when opt    :opt   ) opt
                                                  (when req-un :req-un) req-un
                                                  (when opt-un :opt-un) opt-un]))
          conformer-sym        (gensym "conformer")
          invariant-spec-name  (keyword ns-str (name (gensym "invariant")))
          spec-sym             (gensym "keyspec")
          type-hash            (hash-classname sym)
          k-gen                (gensym "k")
          v-gen                (gensym "v")
          create               (gensym "create")]
     (prl ::debug invariant conformer req-ks un-ks all-mod-ks to-prepend)
     (let [code `(do (declare-spec ~sym-0)
          ~@to-prepend
          (defrecord+ ~req-record-sym     ~(into req-ks-syms     special-ks-syms))
          (defrecord+ ~un-record-sym      ~un-ks-syms )
          (defrecord+ ~all-mod-record-sym ~all-mod-ks-syms)
          (defrecord+ ~all-record-sym     ~(into all-mod-ks-syms special-ks-syms))
          (defspec ~spec-name ~(if invariant
                                   `(v/and (v/keys ~@keyspec) ~invariant)
                                   `(v/keys ~@keyspec)))
          ~(when db-mode? `(swap! db-schemas assoc ~spec-name ~schema))
          ~(when invariant `(defspec ~invariant-spec-name ~invariant))
          ~(when conformer `(def ~conformer-sym ~conformer))
          (def ~required-keys-record (~(symbol (str "map->" req-record-sym    )) ~(merge (zipmap req-ks     req-ks    ) (zipmap special-ks special-ks))))
          (def ~un-keys-record       (~(symbol (str "map->" un-record-sym     )) ~(zipmap un-ks      un-ks)))
          (def ~all-mod-keys-record  (~(symbol (str "map->" all-mod-record-sym)) ~(zipmap all-mod-ks all-mod-ks)))
          (def ~all-keys-record      (~(symbol (str "map->" all-record-sym    )) ~(merge (zipmap all-mod-ks all-mod-ks) (zipmap special-ks special-ks))))
          (def ~un-ks-to-ks          ~(zipmap un-ks un-ks-qualified))
          (def ~spec-sym             ~keyspec)
          (defn ~create [m#]
            (if (instance? ~qualified-record-sym m#)
                m#
                (~(symbol (str "map->" req-record-sym))
                  (let [m# (-> m#
                               ~(if-not db-mode?  `identity* `(assoc :schema/type ~spec-name))
                               ~(if-not conformer `identity* conformer-sym)
                               (v/validate ~spec-name)
                               (set/rename-keys ~un-ks-to-ks))
                        _# (v/validate (:db/id    m#) (v/or* nil? :db/id   ))
                        _# (v/validate (:db/ident m#) (v/or* nil? :db/ident))]
                    (v/validate (keys m#) (fn1 set/subset? ~all-keys-record))
                    m#))))
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
                                (let [~k-gen (or (get ~all-mod-keys-record k0#)
                                                 (get ~un-ks-to-ks         k0#)
                                                 (throw (->ex nil "Key not in validated map spec" {:k k0# :class '~qualified-record-sym})))
                                      ~v-gen (validate v0# ~k-gen)]
                                  (-> (new ~sym (~(if-cljs &env '-assoc '.assoc) ~'v ~k-gen ~v-gen))
                                      ~(if-not conformer `identity* conformer-sym)
                                      ~(if-not invariant `identity* `(validate ~invariant-spec-name)))))}
             ~'?Associative
               {~'assoc       ([_# k0# v0#]
                                (let [~k-gen (or (get ~all-mod-keys-record k0#)
                                                 (get ~un-ks-to-ks         k0#)
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
               {~'pr          ([_# w# opts#] (~'-pr-writer ~'v w# opts#))}
             ~'?HashEq
               {~'hash-eq     ([_#] (int (bit-xor ~type-hash (~(if-cljs &env '-hash '.hashEq) ~'v))))}
             quantum.core.core/IValue
               {~'get         ([_#] ~'v)
                ~'set         ([_# v#] (new ~sym (~create v#)))}})
          (defn ~(symbol (str "->" sym)) [m#] (new ~qualified-sym (~create m#)))
          ~(if-cljs &env qualified-sym `(import (quote ~qualified-sym))))]
     (prl ::debug code)
     code)))))

; TODO validated vector, set, and (maybe) list
; Not sure what else might be useful to create a validated wrapper for... I mean, queues I guess
