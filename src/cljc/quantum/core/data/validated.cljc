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
      :refer [fn-> fn1]]
    [quantum.core.logic
      :refer [eq? fn-and whenf1 whenp nnil?]]
    [quantum.core.log
      :refer [prl]]
    [quantum.core.macros.defrecord
      :refer [defrecord+]]
    [quantum.core.validate  :as v
      :refer [validate defspec]]
    [quantum.core.vars      :as var
      :refer [update-meta]]))

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

(defn replace-value-types
  "In spec, replaces all keywords of :db/<x> with :quantum.db.datomic.core/-<x>"
  [spec]
  (postwalk
    (whenf1 (fn-and keyword? (fn-> namespace (= "db")))
      #(keyword "quantum.db.datomic.core" (str "-" (name %))))
    spec))

#?(:clj
(defmacro def-validated
  "Defines a validated value."
  [sym spec]
  (TODO "need to handle :^db?: schema creations")
  (v/validate sym symbol?)
  (let [other     (gensym "other")
        spec-name (keyword (str (ns-name *ns*)) (name sym))
        type-hash (hash-classname sym)
        db-mode?  (-> sym meta :db?)
        spec (if db-mode? (replace-value-types spec) spec)
        _ (prl :user db-mode? spec)]
    `(do (defspec ~spec-name ~spec)
         ;(swap! quantum.db.datomic.entities/schemas    assoc ~attr-k ~schema-f)
         (deftype-compatible ~sym ~'[v]
           {~'?Object
             {~'hash     ([_#] (.hashCode ~'v))
              ~'equals   ~(std-equals sym other '=)}
            ~'?HashEq
              {~'hash-eq ([_#] (int (bit-xor ~type-hash (~(if-cljs &env '.-hash '.hashEq) ~'v))))}
            ~'?Deref
              {~'deref   ([_#] ~'v)}
            quantum.core.core/IValue
              {get       ([_#] ~'v)
               set       ([_# v#] (new ~sym (v/validate v# ~spec-name)))}})
         (defn ~(symbol (str "->" sym)) [v#]
           (new ~sym (v/validate v# ~spec-name))))))) ; TODO conformer?

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
  {:args-doc '{db-mode?   "Whether in db mode or not"
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
                      (let [[_ inner-name inner-spec] x
                            _ (validate inner-name keyword?
                                        inner-spec nnil?)
                            inner-name (if (= (namespace inner-name) "this")
                                           (keyword (str (name kw-context) ":" (name inner-name)))
                                           inner-name)
                            _ (validate (namespace inner-name) nil?)]
                        (swap! to-prepend conj
                          `(def-validated ~(with-meta (symbol (name inner-name)) ; inherits :db? from parents
                                             (assoc (meta x) :db? db-mode?))
                                          ~inner-spec))
                        inner-name) ; only left with processed keyword name of the inner def
                      ; if not inner def, assume keyword later
                      x))
        spec-f (->> spec
                    (map (fn [[k v]] [k (->> v (mapv extract-inner-def))]))
                    (into {}))]
    {:to-prepend @to-prepend
     :spec       spec-f}))

#?(:clj
(defmacro def-validated-map
  "Defines a validated associative structure.
   Same semantics of `clojure.spec/keys`.
   Basically a validator on a record."
  {:usage `(do (def-validated-map MyTypeOfValidatedMap
                 :invariant #(= 5 (+ (::a %) (::b %)))
                 :req [::a ::b ::c ::d] :opt [::e])
               (defspec ::a number?) (defspec ::b number?) (defspec ::c number?) (defspec ::d number?)
               (assoc (->MyTypeOfValidatedMap {::a 1 ::b 2 ::c 3 ::d 4}) ::a 3))
   :todo  ["Break this macro up"
           ".assoc may call .conj, in which case it's inefficient with double validation"
           "allow transactional manipulation, in which multiple values can be updated at once"
           "DB mode allows for non-namespaced spec keywords"]}
  [sym & {:keys [req opt req-un opt-un invariant] :as spec-0}]
  (validate (-> spec-0 keys set) (fn1 set/subset? #{:req :opt :req-un :opt-un :invariant}))
  (validate
    sym    symbol?
    req    (v/or* nil? vector?) req-un (v/or* nil? vector?)
    opt    (v/or* nil? vector?) opt-un (v/or* nil? vector?))
  (let [db-mode? (-> sym meta :db?)
        {{:keys [req opt req-un opt-un] :as spec} :spec to-prepend :to-prepend}
          (-> spec-0
              (dissoc :invariant)
              (whenp db-mode? replace-value-types)
              (extract-inner-defs db-mode? (-> sym name keyword)))]
          (prl :user spec to-prepend)
    (validate
      req                            (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      opt                            (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      req-un                         (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      opt-un                         (v/or* nil? (v/coll-of ns-keyword? :distinct true))
      (concat req opt req-un opt-un) (v/coll-of ns-keyword? :distinct true))
    (let [qualified-sym        (var/qualify-class sym)
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
          req-ks               (concat req     req-un')
          opt-ks               (concat opt     opt-un')
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
          spec-name            (keyword ns-str (name sym))
          invariant-spec-name  (keyword ns-str (name (gensym)))
          spec-sym             (gensym "keyspec")
          type-hash            (hash-classname sym)
          k-gen                (gensym "k")
          v-gen                (gensym "v")]
     `(do (defrecord+ ~req-record-sym ~req-ks-syms)
          (defrecord+ ~un-record-sym  ~un-ks-syms )
          (defrecord+ ~all-record-sym ~all-ks-syms)
          (defspec ~spec-name ~(if invariant
                                   `(v/and (v/keys ~@keyspec) ~invariant)
                                   `(v/keys ~@keyspec)))
          ~(when invariant `(defspec ~invariant-spec-name ~invariant))
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
             ; ?Cloneable     ([_] (#?(:clj .clone :cljs .-clone) m))
             ~'?Counted
               {~'count       ([_#] (~(if-cljs &env '.-count '.count) ~'v))}
             ~'?Collection
               {~'empty       ([_#] (~(if-cljs &env '.-empty '.empty) ~'v))
                ~'empty!      ([_#] (throw (UnsupportedOperationException.)))
                ~'empty?      ([_#] (~(if-cljs &env nil '.isEmpty) ~'v))
                ~'equals      ~(std-equals sym other (if-cljs &env '.-equiv '.equiv))
                ~'conj        ([_# [k0# v0#]]
                                (let [k# (or (get ~all-keys-record k0#)
                                             (get ~un-keys-record  k0#)
                                             (get ~un-ks-to-ks     k0#)
                                             (throw (->ex nil "Key not in validated map spec" {:k k0# :class '~qualified-record-sym})))
                                      v# (validate v0# k#)]
                                  (new ~sym
                                    (~(if-cljs &env '.-assoc '.assoc) ~'v k# v#))))}
             ~'?Associative
               {~'assoc       ([_# k0# v0#]
                                (let [~k-gen (or (get ~all-keys-record k0#)
                                                 (get ~un-keys-record  k0#)
                                                 (get ~un-ks-to-ks     k0#)
                                                 (throw (->ex nil "Key not in validated map spec" {:k k0# :class '~qualified-record-sym})))
                                      ~v-gen (validate v0# ~k-gen)]
                                  ~(if invariant
                                       `(validate (new ~sym (~(if-cljs &env '.-assoc '.assoc) ~'v ~k-gen ~v-gen)) ~invariant-spec-name)
                                       `(new ~sym (~(if-cljs &env '.-assoc '.assoc) ~'v ~k-gen ~v-gen)))))
                ~'assoc!      ([_# _# _#] (throw (UnsupportedOperationException.)))
                ~'merge!      ([_# _#   ] (throw (UnsupportedOperationException.)))
                ~'dissoc      ([_# k0#]
                                (let [~k-gen k0#]
                                  (when (#?@(:clj  [.containsKey ~required-keys-record]
                                             :cljs [contains? ~required-keys-record]) ~k-gen)
                                    (throw (->ex nil "Key is in ValidatedMap's required keys and cannot be dissoced"
                                                     {:class ~sym :k ~k-gen :keyspec ~spec-sym})))
                                  ~(if invariant
                                       `(validate (new ~sym (~(if-cljs &env '.-dissoc '.without) ~'v ~k-gen)) ~invariant-spec-name)
                                       `(new ~sym (~(if-cljs &env '.-dissoc '.without) ~'v ~k-gen)))))
                ~'dissoc!     ([_# _#] (throw (UnsupportedOperationException.)))
                ~'keys        ([_#] (.keySet   ~'v))
                ~'vals        ([_#] (.values   ~'v))
                ~'entries     ([_#] (.entrySet ~'v))}
             ~'?Lookup
               {~'contains?   ([_# k#] (~(if-cljs &env nil '.containsKey  ) ~'v k#))
                ~'containsv?  ([_# v#] (~(if-cljs &env nil '.containsValue) ~'v v#))
                ; Currently fully unrestricted `get`s: all "fields"/key-value pairs are public.
                ~'get        [([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (~(if-cljs &env '.-lookup '.valAt) ~'v k#))
                              #_([_# k# else#] (~(if-cljs &env '.-lookup '.valAt) ~'v k# else#))]
                ~'kw-get      ([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (reify clojure.lang.ILookupThunk
                                  (get [this# target#]
                                    (if (identical? (class target#) ~sym) (.valAt ~'v k#) this#))))
                ~'get-entry   ([_# k#]
                                #_(enforce-get ~empty-record ~sym ~spec-sym k#)
                                (~(if-cljs &env nil '.entryAt) ~'v k#))}
             ~'?Object
               {~'hash        ([_#] (.hashCode ~'v))
                ~'equals      ~(std-equals sym other (if-cljs &env '.equiv '.equiv))}
             ~'?Iterable
               {~'iterator    ([_#] (~(if-cljs &env '.-iterator '.iterator) ~'v))}
             ~'?Meta
               {~'meta        ([_#] (meta ~'v))
                ~'with-meta   ([_# new-meta#] (new ~sym (with-meta ~'v new-meta#)))}
             ~'?Print
               {~'pr          ([_# w# opts#] (.-pr-writer ~'v w# opts#))}
             ~'?HashEq
               {~'hash-eq     ([_#] (int (bit-xor ~type-hash (~(if-cljs &env '.-hash '.hashEq) ~'v))))}
               ~'?Deref
               {~'deref       ([_#] ~'v)}
               quantum.core.core/IValue
               {get           ([_#] ~'v)
                set           ([_# v#] (new ~sym (v/validate v# ~spec-name)))}})
          (defn ~(symbol (str "->" sym)) [m#]
            (let [m-f# (if (instance? ~qualified-record-sym m#)
                           m# ; no coercion needed
                           (~(symbol (str "map->" req-record-sym))
                            (-> (v/validate m# ~spec-name)
                                (set/rename-keys ~un-ks-to-ks))))] ; TODO conformer?
              (new ~qualified-sym m-f#)))
          ~(if-cljs &env ~qualified-sym `(import (quote ~qualified-sym))))))))

; TODO validated vector, set, and (maybe) list
; Not sure what else might be useful to create a validated wrapper for... I mean, queues I guess

(def-validated-map ^:db?
  ^{:doc "A ratio specifically using longs instead of bigints."}
  ratio:long
  :req [(def :this/numerator   :db/long)
        (def :this/denominator :db/long)])

; TO

; (def-validated ^:db? ratio:long:numerator   :db/long)

; (def-validated ^:db? ratio:long:denominator :db/long)

; (def-validated-map ^:db?
;   ^{:doc "A ratio specifically using longs instead of bigints."}
;   ratio:long
;   :req [:ratio:long:numerator :ratio:long:denominator])
