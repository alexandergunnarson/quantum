(ns quantum.core.data.validated
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   ) :as core]
    [quantum.core.error :as err
      :refer [->ex TODO]]
    [quantum.core.macros.core
      :refer [#?@(:clj [if-cljs])]]
    [quantum.core.macros.deftype
      :refer        [#?@(:clj [deftype-compatible])]
      :refer-macros [          deftype-compatible]]
    [quantum.core.logic
      :refer        [#?@(:clj [eq?])]
      :refer-macros [          eq?]]
    [quantum.core.macros.defrecord
      :refer        [#?@(:clj [defrecord+])]
      :refer-macros [          defrecord+]]
    [quantum.core.validate         :as v
      :refer        [#?@(:clj [validate defspec])]
      :refer-macros [          validate defspec]]))


#_(defmacro defvalidated []
  ())

#_(defnt whatever-function
    [^MyTypeOfValidatedMap v] ; so the schema is enforced much more cheaply
    )

(defn enforce-get [base-record c ks k]
  (when-not (#?@(:clj  [.containsKey ^java.util.Map base-record]
                 :cljs [contains? base-record]) k)
    (throw (->ex nil "Key is not present in ValidatedMap's spec" {:class c :k k :keyspec ks}))))


(defprotocol IValidatedKV
  (get- [this])
  (set- [this newv]))

; (defmacro defvalidated-kv
;   [name- spec]
;   `(deftype-compatible ~name- ~'[v]
;      {IValidatedKV
;        {~'get ([_#] ~'v)
;         ~'set ([_# newv#]
;               (let [conformed# (s/conform spec)]
;                 (new ~name- conformed)))}}))

; (defmacro defvalidated
;   [name- spec]
;   (cond (keyword? name-)
;         (do (s/def name- ~spec)
;             (defvalidated-kv name- spec)))

;   )

#?(:clj
(defmacro def-validated-map
  "Same semantics of `clojure.spec/keys`"
  {:usage `(def-validated-map MyTypeOfValidatedMap :req [::a ::b ::c ::d] :opt [::e])}
  [sym req req-ks & [opt opt-ks]]
  (v/validate-all
    sym    symbol?
    req    (v/or* nil? (eq? :req))
    req-ks (v/coll-of keyword?)
    opt    (v/or* nil? (eq? :opt))
    opt-ks (v/coll-of keyword?))
  (let [record-sym (symbol (str (name sym) "__"))
        other   (gensym "other")
        required-keys-record (with-meta (gensym "required-keys-record")
                                        {:tag record-sym})
        all-keys-record      (gensym "all-keys-record")
        ;all-ks      (into req-ks opt-ks)
        req-ks-syms (mapv #(symbol (namespace %) (name %)) req-ks)
        keyspec     (vec (remove nil? [req req-ks opt opt-ks]))
        keyspec-name (keyword (str (ns-name *ns*)) (name sym))
        keyspec-sym  (gensym "keyspec")]
   `(do (defspec  ~keyspec-name (v/keys ~@keyspec))
        (defrecord+ ~record-sym ~req-ks-syms)
        (def ~required-keys-record (~(symbol (str "map->" record-sym)) nil))
        (def ~all-keys-record      (merge (~(symbol (str "map->" record-sym)) nil)
                                     (zipmap ~opt-ks (repeat nil))))
        (def ~keyspec-sym ~keyspec)
        (deftype-compatible ~sym
          [~(with-meta 'v {:tag record-sym})]
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
              ~'equals      ([_# other#] (~(if-cljs &env '.-equiv '.equiv) ~'v other#))
              ~'conj        ([_# [k0# v0#]]
                              (let [k# k0#
                                    v# (validate k# v0#)]
                                (new ~sym
                                  (~(if-cljs &env '.-assoc '.assoc) ~'v k# v#))))}
           ~'?Associative
             {~'assoc       ([_# k0# v0#]
                              (let [k# k0#
                                    v# (validate k# v0#)]
                                (new ~sym
                                  (~(if-cljs &env '.-assoc '.assoc) ~'v k# v#))))
              ~'assoc!      ([_# _# _#] (throw (UnsupportedOperationException.)))
              ~'merge!      ([_# _#] (throw (UnsupportedOperationException.)))
              ~'dissoc      ([_# k0#]
                              (let [k# k0#]
                                (when (#?@(:clj  [.containsKey ~required-keys-record]
                                           :cljs [contains? ~required-keys-record]) k#)
                                  (throw (->ex nil "Key is in ValidatedMap's required keys and cannot be dissoced"
                                                   {:class ~sym :k k# :keyspec ~keyspec-sym})))
                                (new ~sym
                                  (~(if-cljs &env '.-dissoc '.without) ~'v k#))))
              ; `dissoc` is currently not possible, just as adding extra keys isn't
              ~'dissoc!     ([_# _#] (throw (UnsupportedOperationException.)))
              ~'keys        ([_#] (.keySet   ~'v))
              ~'vals        ([_#] (.values   ~'v))
              ~'entries     ([_#] (.entrySet ~'v))}
           ~'?Lookup
             {~'contains?   ([_# k#]   (~(if-cljs &env nil '.containsKey) ~'v k#))
              ~'containsv?  ([_# v#]   (~(if-cljs &env nil '.containsValue) ~'v v#))
              ; Currently fully unrestricted `get`s: all "fields"/key-value pairs are public.
              ~'get        [([_# k#]
                              #_(enforce-get ~empty-record ~sym ~keyspec-sym k#)
                              (~(if-cljs &env '.-lookup '.valAt) ~'v k#))
                            #_([_# k# else#] (~(if-cljs &env '.-lookup '.valAt) ~'v k# else#))]
              ~'kw-get      ([_# k#]
                              #_(enforce-get ~empty-record ~sym ~keyspec-sym k#)
                              (.getLookupThunk ~'v k#))
              ~'get-entry   ([_# k#]
                              #_(enforce-get ~empty-record ~sym ~keyspec-sym k#)
                              (~(if-cljs &env nil '.entryAt) ~'v k#))}
           ~'?Object
             {~'hash        ([_#] (.hashCode ~'v))
              ~'equals      ([this# ~other]
                              (and (not (nil? ~other))
                                   (or (identical? this# ~other)
                                       (and (instance? ~sym ~other)
                                            (~(if-cljs &env '.equiv '.equals)
                                             ~'v (.-m ~(with-meta other {:tag sym})))))))}
           ~'?Iterable
             {~'iterator    ([_#] (~(if-cljs &env '.-iterator '.iterator) ~'v))}
           ~'?Meta
             {~'meta        ([_#] (meta ~'v))
              ~'with-meta   ([_# new-meta#] (new ~sym (with-meta ~'v new-meta#)))}
           ~'?Print
             {~'pr          ([_# w# opts#] (.-pr-writer ~'v w# opts#))}
           ~'?HashEq
             {~'hash-eq     ([_#] (~(if-cljs &env '.-hash '.hashEq) ~'v))}})
        (defn ~(symbol (str "->" sym)) [m#]
          (let [m-f# (if (instance? ~record-sym m#)
                         (v/validate ~keyspec-name m#) ; TODO conformer?
                         (~(symbol (str "map->" record-sym))
                          (v/validate ~keyspec-name m#)))] ; TODO conformer?
            (new ~sym m-f#)))))))
