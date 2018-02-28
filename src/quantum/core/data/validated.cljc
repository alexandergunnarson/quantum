(ns quantum.core.data.validated
  (:refer-clojure :exclude [contains?])
  (:require
    [clojure.core           :as core]
    [quantum.core.data.set  :as set]
    [quantum.core.error     :as err
      :refer [>ex-info TODO catch-all]]
    [quantum.core.macros.deftype :as deftype]
    [quantum.core.fn
      :refer [fn-> fn->> fn1 fnl <- fn']]
    [quantum.core.logic
      :refer [fn= fn-and fn-or whenf1 whenf whenp default]]
    [quantum.core.log       :as log
      :refer [prl]]
    [quantum.core.macros.defrecord
      :refer [defrecord+]]
    [quantum.core.macros.optimization
      :refer [identity*]]
    [quantum.core.spec      :as s
      :refer [validate]]
    [quantum.untyped.core.collections :as ucoll
      :refer [contains?]]
    [quantum.untyped.core.collections.tree :as utree
      :refer [postwalk]]
    [quantum.untyped.core.qualify :as uqual]
    [quantum.untyped.core.form.evaluate
      :refer [case-env]])
#?(:cljs
  (:require-macros
    [quantum.core.data.validated :as self
      :refer [def-map]])))

;; IMPORTANT TODO
;; Validated structures could turn off validation temporarily
;; kind of like transients turn off persistence temporarily.
;; Just as transients error when being modified in different
;; threads, validated structures could change class when
;; validation is turned off, and thus error when a spec is
;; trying to check its structure. But it would still be very
;; much open for modification, just faster.

(s/def :type/any (fn' true))

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

(defn enforce-get [base-record c ks k]
  (when-not (#?@(:clj  [.containsKey ^java.util.Map base-record]
                 :cljs [contains? base-record]) k)
    (throw (>ex-info "Key is not present in ValidatedMap's spec" {:class c :k k :keyspec ks}))))

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

(defn dbify-keyword [k ns-name-str]
  (validate k keyword?)
  (if (namespace k) k (keyword ns-name-str (name k))))

(defn contextualize-keyword [k kw-context]
  (validate k keyword?)
  (cond (= (namespace k) "this")
        (if (namespace kw-context)
            (keyword (str (namespace kw-context) ":" (name kw-context)) ; would be a "." but no, to avoid classname clashes
                     (name k))
            (keyword (name kw-context) (name k)))
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

(def dbfn-call? (fn-and seq?
                        (fn-> first keyword?)
                        (fn-> first namespace (= "fn"))))

(def db-type
  (fn-or (fn-and keyword?
           (fn-or (fn-and (fn->> namespace (= "db"))
                          (fn->> name keyword db-value-types))
                  (fn-and (fn->> namespace (= "quantum.db.datomic.core"))
                          (fn->> name rest (apply str) keyword db-value-types))))
         (fn-and seq? (fn-> first symbol?) (fn-> first name (= "and")) ; TODO really this should check for core.validate/and
           (fn->> rest (filter db-type) first db-type))
         (fn' :ref)))

(defn spec->schema
  {:todo #{"Enforce validators/specs using database transaction functions"}}
  [sym spec]
  `(quantum.db.datomic.core/->schema
     (->> ~(let [type (db-type spec)]
             (->> {:ident       (keyword (namespace sym) (name sym))
                   :type        type
                   :cardinality (if ((fn-and seq? (fn-> first symbol?) (fn-> first name (= "set-of")))
                                     spec)
                                    :many
                                    :one)
                   :doc         (-> sym meta :doc        )
                   :component?  (boolean (and (-> sym meta :component?)
                                              (= type :ref)))
                   :index?      (-> sym meta :index?     )
                   :full-text?  (-> sym meta :full-text? )
                   :unique      (-> sym meta :unique     )
                   :no-history? (-> sym meta :no-history?)}
                  (remove (fn [[k v]] (nil? v)))
                  (into {})))
          (remove (fn [[k# v#]] (nil? v#)))
          (into {}))))

(def allowed-keys #{:req :opt :req-un :opt-un :conformer :invariant})

(defn extract-inner-defs
  "Extracts inner `def`s from a `def-map` like:
   ```
   (dv/def-map ^:db? whatever
     :req [(def :this/numerator   ::a)
           (def :this/denominator ::a)])
   ```
   and places them in code outside of the block like so:
   ```
   (dv/def ^:db? whatever:numerator   ::a)
   (dv/def ^:db? whatever:denominator ::a)
   (dv/def-map ^:db? whatever
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
  [spec parent-sym db-mode? kw-context ns-name-str]
  (validate (-> spec keys set) (fn1 set/subset? allowed-keys))
  (let [to-prepend (atom [])
        inner-def? (fn-and seq? (fn-> first symbol?) (fn-> first name (= "def")))
        ; TODO do inner validated maps too, which can have infinitely nested ones
        extract-inner-def
          (fn [x]
            (if (inner-def? x)
                (let [[_ inner-name & inner-spec-args] x
                      _ (validate inner-name      keyword?
                                  inner-spec-args contains?)
                      inner-name (contextualize-keyword inner-name kw-context)
                      inner-name-sym
                        (with-meta (symbol (namespace inner-name) (name inner-name)) ; inherits :db? from parents
                           (assoc (meta x)
                             :db?         db-mode?
                             :no-history? (default (-> x meta :no-history?)
                                                   (-> parent-sym meta :no-history?)) ; Datomic requires this
                             :component?  (default (-> x meta :component?) db-mode?))) ; all inner defs which are of type :ref, which are not marked `:component? false`, are components
                      inner-spec (if (-> inner-spec-args first allowed-keys)
                                     `(def-map ~inner-name-sym
                                        ~@inner-spec-args)
                                     `(quantum.core.data.validated/def ~inner-name-sym
                                        ~@(contextualize-keywords kw-context inner-spec-args)))]
                  (swap! to-prepend conj inner-spec)
                  ; only left with processed keyword name of the inner def
                  (whenp inner-name db-mode? (fn1 dbify-keyword ns-name-str)))
                ; if not inner def, assume keyword (and validate later)
                (whenp x db-mode? (fn1 dbify-keyword ns-name-str))))
        spec-f (->> spec
                    (map (fn [[k v]] [k (->> v (mapv extract-inner-def))]))
                    (into {}))]
    {:to-prepend @to-prepend
     :spec       spec-f}))

(defn sym->spec-name+sym [sym ns-name-str]
  (let [db-mode?  (-> sym meta :db?)
        spec-name (keyword (or (namespace sym) ns-name-str)
                           (name sym))]
    {:spec-name spec-name
     :sym       (-> (symbol (if (= ns-name-str (namespace spec-name))
                                (name spec-name)
                                (str (namespace spec-name) ":" (name spec-name))))
                    (with-meta (meta sym)))}))

; ===== TOP-LEVEL MACROS ===== ;

(defonce spec-infos (atom {}))

(defrecord SpecInfo [constructor conformer invariant req-all un-all un-> schema])

#?(:clj
(defmacro declare-spec [sym]
  (let [{:keys [spec-name]} (sym->spec-name+sym sym (str (ns-name *ns*)))]
    `(s/def ~spec-name (fn [x#] (TODO))))))

#?(:clj
(defmacro def
  "Defines a validated value."
  ([sym-0 spec-0] `(quantum.core.data.validated/def ~sym-0 ~spec-0 nil))
  ([sym-0 spec-0 conformer]
    (s/validate sym-0 symbol?)
    (let [other         (gensym "other")
          ns-name-str   (str (ns-name *ns*))
          {:keys [spec-name sym]} (sym->spec-name+sym sym-0 ns-name-str)
          type-hash     (hash-classname sym-0)
          db-mode?      (-> sym-0 meta :db?)
          kw-context    (keyword (namespace sym-0) (name sym-0))
          spec          (->> spec-0
                             (<- (whenp db-mode? replace-value-types))
                             (<- (whenf (fn-and (fn' db-mode?)
                                                keyword? (fn-> namespace nil?))
                                        (fn1 dbify-keyword ns-name-str))))
          spec-base     (gensym "spec-base")
          conformer-sym (gensym "conformer")
          constructor-sym (symbol (str "->" sym))
          schema        (when db-mode? (spec->schema sym-0 spec))
          code `(do (def ~conformer-sym ~conformer)
                    (deftype/deftype ~(with-meta sym {:no-factory? true}) ~'[v]
                      {~'?Object
                        {~'hash     ([_#] (.hashCode ~'v))
                         ~'equals   ~(std-equals sym other '=)}
                       ~'?HashEq
                         {~'hash-eq ([_#] (int (bit-xor ~type-hash (~(case-env :clj '.hashEq :cljs '-hash) ~'v))))}
                       ~'?Deref
                         {~'deref   ([_#] ~'v)}
                       quantum.core.core/IValue
                         {~'get     ([_#] ~'v)
                          ~'set     ([_# v#] (new ~sym (-> v# ~(if-not conformer `identity* conformer-sym)
                                                           (s/validate ~spec-name))))}})
                    (def ~spec-base ~spec)
                    (s/def ~spec-name
                      (s/conformer (fn [x#] (if (instance? ~sym x#)
                                                x#
                                                (-> x# ~(if-not conformer `identity* conformer-sym)
                                                    (validate ~spec-base))))))
                    (defn ~constructor-sym [v#]
                      (new ~sym (-> v# ~(if-not conformer `identity* conformer-sym)
                                    (s/validate ~spec-name))))
                    (swap! spec-infos assoc ~spec-name
                      (map->SpecInfo {:conformer ~conformer-sym :schema ~schema :constructor ~constructor-sym}))
                    ~sym)]
      (prl ::debug db-mode? sym-0 sym spec-name spec schema code)
      code))))

#?(:clj
(defmacro def-map
  "Defines a validated associative structure.
   Same semantics of `clojure.spec/keys`.
   Basically a validator on a record."
  {:usage `(do (def-map  ^:db? my-type-of-validated-map
                 :invariant    #(= 7 (+ (::a %) (::b %)))
                 :db-invariant ([db m] m)
                 :conformer    (fn [m] (assoc m ::a 6))
                 :req [::a ::b ::c ::d] :opt [::e])
               (s/def ::a number?) (s/def ::b number?) (s/def ::c number?) (s/def ::d number?)
               (assoc (->my-type-of-validated-map {::a 2 ::b 1 ::c 3 ::d 4}) ::a 3))
   :todo {1 "Break this macro up"
          2 ".assoc may call .conj, in which case it's inefficient with double validation"
          3 "allow transactional manipulation, in which multiple values can be updated at once"
          4 "incorporate invariant (and conformer?) into schema"
          5 "add db-invariant into schema and used with `defn!`"}}
  [sym-0 & {:keys [req opt req-un opt-un invariant db-invariant conformer] :as spec-0}]
  (validate (-> spec-0 keys set) (fn1 set/subset? #{:req :opt :req-un :opt-un :invariant :db-invariant :conformer}))
  (validate
    sym-0 symbol?
    req (s/or* nil? vector?) req-un (s/or* nil? vector?)
    opt (s/or* nil? vector?) opt-un (s/or* nil? vector?))
  (let [db-mode?    (-> sym-0 meta :db?)
        _           (when db-invariant (assert db-mode?))
        kw-context  (keyword (namespace sym-0) (name sym-0))
        ns-name-str (str (ns-name *ns*))
        {{:keys [req opt req-un opt-un] :as spec} :spec to-prepend :to-prepend}
          (-> spec-0
              (dissoc :invariant :db-invariant :conformer)
              (whenp db-mode? replace-value-types)
              (extract-inner-defs sym-0 db-mode? kw-context ns-name-str))
        invariant (contextualize-keywords kw-context invariant)
        conformer (contextualize-keywords kw-context conformer)]
    (validate
      req                            (s/or* nil? (s/coll-of qualified-keyword? :distinct true))
      opt                            (s/or* nil? (s/coll-of qualified-keyword? :distinct true))
      req-un                         (s/or* nil? (s/coll-of qualified-keyword? :distinct true))
      opt-un                         (s/or* nil? (s/coll-of qualified-keyword? :distinct true))
      (concat req opt req-un opt-un) (s/coll-of qualified-keyword? :distinct true))
    (let [{:keys [spec-name sym]} (sym->spec-name+sym sym-0 ns-name-str)
          schema               (when db-mode? (spec->schema sym-0 nil)) ; TODO #4, #5
          qualified-sym        (uqual/qualify|class sym)
          req-record-sym       (symbol (str (name sym) ":__required"))
          qualified-record-sym (uqual/qualify|class req-record-sym)
          un-record-sym        (symbol (str (name sym) ":__un"))
          all-mod-record-sym   (symbol (str (name sym) ":__all-mod"))
          all-record-sym       (symbol (str (name sym) ":__all"))
          un-ks-to-ks          (symbol (str (name sym) ":__un->ks"))
          other                (gensym "other")
          required-keys-record (with-meta (symbol (str (name sym) ":__required-keys-record"))
                                          {:tag qualified-record-sym})
          un-keys-record       (symbol (str (name sym) ":__un-keys-record"     ))
          all-mod-keys-record  (symbol (str (name sym) ":__all-mod-keys-record"))
          all-keys-record      (symbol (str (name sym) ":__all-keys-record"    ))
          req-un'              (mapv #(keyword (name %)) req-un)
          opt-un'              (mapv #(keyword (name %)) opt-un)
          special-ks           (when db-mode? #{:schema/type :db/id :db/ident :db/txInstant})
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
          keyspec              (vec (apply concat
                                      [(when (or req req-un) [:req (vec (concat req req-un))])
                                       (when (or opt opt-un) [:opt (vec (concat opt opt-un))])]))
          conformer-sym        (gensym "conformer")
          invariant-spec-name  (keyword ns-name-str (name (gensym "invariant")))
          spec-sym             (gensym "keyspec")
          spec-base            (gensym "spec-base")
          constructor-sym      (symbol (str "->" sym))
          type-hash            (hash-classname sym)
          k-gen                (gensym "k")
          v-gen                (gensym "v")
          m-gen                (gensym "m")
          create               (symbol (str "create-" sym))
          invalid              (case-env :cljs :cljs.spec.alpha/invalid :clojure.spec.alpha/invalid)
          stored-record-sym    (if (-> sym-0 meta :no-hash-map?)
                                   all-record-sym
                                   req-record-sym)]
     (prl ::debug invariant conformer req-ks un-ks all-mod-ks to-prepend)
     (let [code `(do (declare-spec ~sym-0)
          ~@to-prepend
          (defrecord+ ~req-record-sym     ~(into req-ks-syms     special-ks-syms))
          (defrecord+ ~un-record-sym      ~un-ks-syms )
          (defrecord+ ~all-mod-record-sym ~all-mod-ks-syms)
          (defrecord+ ~all-record-sym     ~(into all-mod-ks-syms special-ks-syms))
          ~(when invariant `(s/def ~invariant-spec-name ~invariant))
          (def ~conformer-sym ~conformer)
          (def ~required-keys-record (~(symbol (str "map->" req-record-sym    )) ~(merge (zipmap req-ks     req-ks    ) (zipmap special-ks special-ks))))
          (def ~un-keys-record       (~(symbol (str "map->" un-record-sym     )) ~(zipmap un-ks      un-ks)))
          (def ~all-mod-keys-record  (~(symbol (str "map->" all-mod-record-sym)) ~(zipmap all-mod-ks all-mod-ks)))
          (def ~all-keys-record      (~(symbol (str "map->" all-record-sym    )) ~(merge (zipmap all-mod-ks all-mod-ks) (zipmap special-ks special-ks))))
          (def ~un-ks-to-ks          ~(zipmap un-ks un-ks-qualified))
          (def ~spec-sym             ~keyspec)
          (def ~spec-base            (s/and (s/keys ~@keyspec) ~@(when invariant [invariant])))
          (defn ~create [~m-gen]
            (if (or (instance? ~qualified-record-sym ~m-gen)
                    ~@(when db-mode?
                        [`(s/valid? :quantum.db.datomic.core/-ref ~m-gen)]))
                ~m-gen
                (let [_# (s/validate ~m-gen map?)
                      m# (-> ~m-gen
                             ~(if-not db-mode?  `identity* `(assoc :schema/type ~spec-name))
                             ~(if-not conformer `identity* conformer-sym) ; TODO conform only *after* validation does not pass?
                             (set/rename-keys ~un-ks-to-ks) ; All :*-un keys -> namespaced
                             (s/validate ~spec-base))
                      _# (s/validate (:db/id    m#) (s/or* nil? :db/id   ))
                      _# (s/validate (:db/ident m#) (s/or* nil? :db/ident))]
                  (s/validate (keys m#) (fn1 set/subset? ~all-keys-record))
                  (~(symbol (str "map->" stored-record-sym)) m#))))
          (deftype/deftype ~(with-meta sym {:no-factory? true})
            [~(with-meta 'v {:tag stored-record-sym})]
            {~'?Seqable
              {~'seq          ([_#] (seq ~'v))}
             ~'?Record        true
             ~'?Sequential    true
             ; ?Cloneable     ([_] (#?(:clj .clone :cljs -clone) m))
             ~'?Counted
               {~'count       ([_#] (~(case-env :clj '.count   :cljs '-count) ~'v))}
             ~'?Collection
               {~'empty       ([_#] (~(case-env :clj '.empty   :cljs '-empty) ~'v))
                ~'empty!      ([_#] (throw (UnsupportedOperationException.)))
                ~'empty?      ([_#] (~(case-env :clj '.isEmpty :cljs nil    ) ~'v))
                ~'equals      ~(std-equals sym other (case-env :clj '.equiv :cljs '-equiv))
                ~'conj        ([_# [k0# v0#]]
                                (let [~k-gen (or (get ~all-mod-keys-record k0#)
                                                 (get ~un-ks-to-ks         k0#)
                                                 (throw (>ex-info "Key not in validated map spec" {:k k0# :class '~qualified-record-sym})))
                                      ~v-gen (validate v0# ~k-gen)]
                                  (-> (new ~sym (~(case-env :clj '.assoc :cljs '-assoc) ~'v ~k-gen ~v-gen))
                                      ~(if-not conformer `identity* conformer-sym)
                                      ~(if-not invariant `identity* `(validate ~invariant-spec-name)))))}
             ~'?Associative
               {~'assoc       ([_# k0# v0#]
                                (let [~k-gen (or (get ~all-mod-keys-record k0#)
                                                 (get ~un-ks-to-ks         k0#)
                                                 (throw (>ex-info "Key not in validated map spec" {:k k0# :class '~qualified-record-sym})))
                                      ~v-gen (validate v0# ~k-gen)]
                                  (-> (new ~sym (~(case-env :clj '.assoc :cljs '-assoc) ~'v ~k-gen ~v-gen))
                                      ~(if-not conformer `identity* conformer-sym)
                                      ~(if-not invariant `identity* `(validate ~invariant-spec-name)))))
                ~'assoc!      ([_# _# _#] (throw (UnsupportedOperationException.)))
                ~'merge!      ([_# _#   ] (throw (UnsupportedOperationException.)))
                ~'dissoc      ([_# k0#]
                                (let [~k-gen k0#]
                                  (when (#?@(:clj  [.containsKey ~required-keys-record]
                                             :cljs [contains? ~required-keys-record]) ~k-gen)
                                    (throw (>ex-info "Key is in ValidatedMap's required keys and cannot be dissoced"
                                                 {:class ~sym :k ~k-gen :keyspec ~spec-sym})))
                                   (-> (new ~sym (~(case-env :clj '.without :cljs '-dissoc) ~'v ~k-gen))
                                       ~(if-not conformer `identity* conformer-sym)
                                       ~(if-not invariant `identity* `(validate ~invariant-spec-name)))))
                ~'dissoc!     ([_# _#] (throw (UnsupportedOperationException.)))
                ~'keys        ([_#] (.keySet   ~'v))
                ~'vals        ([_#] (.values   ~'v))
                ~'entries     ([_#] (.entrySet ~'v))}
             ~'?Lookup
               {~'contains?   ([_# k#] (or (~(case-env :clj '.containsKey :cljs nil) ~'v k#)
                                           (~(case-env :clj '.containsKey :cljs nil) ~'v (get ~un-ks-to-ks k#))))
                ~'containsv?  ([_# v#] (~(case-env :clj '.containsValue :cljs nil) ~'v v#))
                ; Currently fully unrestricted `get`s: all "fields"/key-value pairs are public.
                ~'get        [([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (or (~(case-env :clj '.valAt :cljs '-lookup) ~'v k#)
                                    (~(case-env :clj '.valAt :cljs '-lookup) ~'v (get ~un-ks-to-ks k#))))
                              #_([_# k# else#] (~(case-env :clj '.valAt :cljs '-lookup) ~'v k# else#))]
                ~'kw-get      ([this# k#]
                                (reify clojure.lang.ILookupThunk
                                  (get [this# ~v-gen]
                                    (if (identical? (class ~v-gen) ~sym)
                                        (let [v0# (.valAt ~(with-meta v-gen {:tag sym}) k#)]
                                          (if (nil? v0#) ; really, it's if it's not found...
                                              (.valAt ~(with-meta v-gen {:tag sym}) (get ~un-ks-to-ks k#))
                                              v0#))
                                        this#))))
                ~'find        ([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (~(case-env :clj '.entryAt :cljs nil) ~'v k#))}
             ~'?Object
               {~'hash        ([_#] (.hashCode ~'v))
                ~'equals      ~(std-equals sym other (case-env :clj '.equiv :cljs '.equiv))}
             ~'?Iterable
               {~'iterator    ([_#] (~(case-env :clj '.iterator :cljs '-iterator) ~'v))}
             ~'?Meta
               {~'meta        ([_#] (meta ~'v))
                ~'with-meta   ([_# new-meta#] (new ~sym (with-meta ~'v new-meta#)))}
             ~'?Print
               {~'pr          ([_# w# opts#] (~'-pr-writer ~'v w# opts#))}
             ~'?HashEq
               {~'hash-eq     ([_#] (int (bit-xor ~type-hash (~(case-env :clj '.hashEq :cljs '-hash) ~'v))))}
             quantum.core.core/IValue
               {~'get         ([_#] ~'v)
                ~'set         ([_# v#] (if (instance? ~sym v#) v# (new ~sym (~create v#))))}})
          (defn ~constructor-sym [m#] (new ~qualified-sym (~create m#)))
          (s/def ~spec-name (s/conformer
                              (fn [x#] (cond (instance? ~qualified-sym x#)
                                             x#
                                             (dbfn-call? x#) ; TODO only if DB mode
                                             x#
                                             :else (new ~qualified-sym (~create x#))
                                           #_(catch-all (new ~qualified-sym (~create x#))
                                             e# ~invalid))))) ; TODO avoid semi-expensive try-catch here by using conformers all the way down the line
          (swap! spec-infos assoc ~spec-name
            (map->SpecInfo {:conformer ~conformer-sym :invariant ~invariant
                            :schema    ~schema :constructor ~constructor-sym
                            :req-all   ~required-keys-record
                            :un-all    ~un-keys-record
                            :un->      ~un-ks-to-ks}))
          ~(case-env :clj `(import (quote ~qualified-sym)) :cljs qualified-sym))]
     (prl ::debug code)
     code)))))

; TODO validated vector, set, and (maybe) list
; Not sure what else might be useful to create a validated wrapper for... I mean, queues I guess
