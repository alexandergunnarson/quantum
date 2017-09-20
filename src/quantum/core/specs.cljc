(ns quantum.core.specs
  "Adapted from clojure.core.specs version 1.9.0-alpha19
   and enabled in CLJS. Other specs added too.
   See https://github.com/clojure/core.specs.alpha/blob/5d85f93ab78386855374256934475af0afe7f380/src/main/clojure/clojure/core/specs/alpha.clj"
  (:require
    [clojure.core      :as core]
    [clojure.set       :as set]
    [quantum.core.collections.base
      :refer [dissoc-if]]
    [quantum.core.core
      :refer [val?]]
    [quantum.core.fn   :as fn
      :refer [fn1 fnl]]
    [quantum.core.spec :as s]))

;;;; GENERAL

(s/def ::meta map?)

;;;; destructure

(s/def ::local-name (s/and simple-symbol? #(not= '& %)))

(s/def ::binding-form
  (s/or :sym ::local-name
        :seq ::seq-binding-form
        :map ::map-binding-form))

;; sequential destructuring

(s/def ::seq-binding-form
  (s/and vector?
         (s/cat :elems (s/* ::binding-form)
                :rest  (s/? (s/cat :amp #{'&} :form ::binding-form))
                :as    (s/? (s/cat :as #{:as} :sym ::local-name)))))

;; map destructuring

(s/def ::keys (s/coll-of ident?         :kind vector?))
(s/def ::syms (s/coll-of symbol?        :kind vector?))
(s/def ::strs (s/coll-of simple-symbol? :kind vector?))
(s/def ::or   (s/map-of simple-symbol? any?))
(s/def ::as ::local-name)

(s/def ::map-special-binding
  (s/keys :opt-un [::as ::or ::keys ::syms ::strs]))

(s/def ::map-binding (s/tuple ::binding-form any?))

(s/def ::ns-keys
  (s/tuple
    (s/and qualified-keyword? #(-> % name #{"keys" "syms"}))
    (s/coll-of simple-symbol? :kind vector?)))

(s/def ::map-bindings
  (s/every (s/or :mb ::map-binding
                 :nsk ::ns-keys
                 :msb (s/tuple #{:as :or :keys :syms :strs} any?)) :into {}))

(s/def ::map-binding-form (s/merge ::map-bindings ::map-special-binding))

;; bindings

(s/def ::binding (s/cat :binding ::binding-form :init-expr any?))
(s/def ::bindings (s/and vector? (s/* ::binding)))

;; let, if-let, when-let

(s/fdef core/let
  :args (s/cat :bindings ::bindings
               :body (s/* any?)))

(s/fdef core/if-let
  :args (s/cat :bindings (s/and vector? ::binding)
               :then any?
               :else (s/? any?)))

(s/fdef core/when-let
  :args (s/cat :bindings (s/and vector? ::binding)
               :body (s/* any?)))

;; defn, defn-, fn

(s/def ::fn:arglist
  (s/and
    vector?
    (s/cat :args    (s/* ::binding-form)
           :varargs (s/? (s/cat :amp #{'&} :form ::binding-form)))))

(s/def ::fn:prepost
  (s/and (s/keys :req-un [(or ::core/pre ::core/post)]) ; TODO we actually really only want to accept un-namespaced keys...
         (s/conformer #(set/rename-keys % {::core/pre :pre ::core/post :post}))))

(s/def ::fn:body
  (s/alt :prepost+body (s/cat :prepost ::fn:prepost
                              :body    (s/+ any?))
         :body         (s/* any?)))

(s/def ::fn:arglist+body
  (s/cat ::fn:arglist ::fn:arglist :body ::fn:body))

(s/def ::fn:name simple-symbol?)

(s/def ::docstring string?)

(s/def ::fn:unique-doc
  #(->> [(::docstring %)
         (-> % ::fn:name meta :doc)
         (-> % :pre-meta      :doc)
         (-> % :post-meta     :doc)]
        (filter val?)
        count
        ((fn [x] (<= x 1)))))

(s/def ::fn:unique-meta
  #(empty? (set/intersection
             (-> % ::fn:name meta keys set)
             (-> % :pre-meta  keys set)
             (-> % :post-meta keys set))))

(s/def ::fn:aggregate-meta
  (s/conformer
    (fn [{:keys [::fn:name ::docstring pre-meta post-meta] :as m}]
      (-> m
          (dissoc ::docstring :pre-meta :post-meta)
          (cond-> fn:name
            (update ::fn:name with-meta
              (-> (merge (meta fn:name) pre-meta post-meta) ; TODO use `merge-unique` instead of `::defn:unique-meta`
                  (cond-> docstring (assoc :doc docstring)))))))))

(s/def ::fn:postchecks
  (s/and (s/conformer
           (fn [v]
             (let [[arities-k arities-v] (get v :arities)
                   arities
                    (-> (case arities-k
                          :arity-1 {:arities [arities-v]}
                          :arity-n arities-v)
                        (update :arities
                          (fnl mapv
                            (fn1 update :body
                              (fn [[k v]]
                                (case k
                                  :body         {:body v}
                                  :prepost+body v))))))]
               (assoc v :post-meta (:post-meta arities)
                        :arities   (:arities   arities)))))
         ::fn:unique-doc
         ::fn:unique-meta
         ;; TODO validate metadata like return value etc.
         ::fn:aggregate-meta))

(s/def ::fn
  (s/and (s/spec
           (s/cat ::fn:name (s/? ::fn:name)
                  :arities  (s/alt :arity-1 ::fn:arglist+body
                                   :arity-n (s/cat :arities (s/+ (s/spec ::fn:arglist+body))))))
         ::fn:postchecks))

(s/def ::defn
  (s/and
    (s/spec
      (s/cat ::fn:name   ::fn:name
             ::docstring (s/? ::docstring)
             :pre-meta   (s/? ::meta)
             :arities    (s/alt :arity-1 ::fn:arglist+body
                                :arity-n (s/cat :arities   (s/+ (s/spec ::fn:arglist+body))
                                                :post-meta (s/? ::meta)))))
    ::fn:postchecks))

(s/fdef core/defn  :args ::defn :ret any?)
(s/fdef core/defn- :args ::defn :ret any?)
(s/fdef core/fn    :args ::fn   :ret any?)

;;;; ns

(s/def ::exclude (s/coll-of simple-symbol?))
(s/def ::only    (s/coll-of simple-symbol?))
(s/def ::rename  (s/map-of simple-symbol? simple-symbol?))
(s/def ::filters (s/keys* :opt-un [::exclude ::only ::rename]))

(s/def ::ns-refer-clojure
  (s/spec (s/cat :clause #{:refer-clojure}
                 :filters ::filters)))

(s/def ::refer (s/or :all #{:all}
                     :syms (s/coll-of simple-symbol?)))

(s/def ::prefix-list
  (s/spec
    (s/cat :prefix simple-symbol?
           :libspecs (s/+ ::libspec))))

(s/def ::libspec
  (s/alt :lib      simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer])))))

(s/def ::ns-require
  (s/spec (s/cat :clause #{:require}
                 :body (s/+ (s/alt :libspec ::libspec
                                   :prefix-list ::prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(s/def ::package-list
  (s/spec
    (s/cat :package simple-symbol?
           :classes (s/* simple-symbol?))))

(s/def ::import-list
  (s/* (s/alt :class simple-symbol?
              :package-list ::package-list)))

(s/def ::ns-import
  (s/spec
    (s/cat :clause #{:import}
           :classes ::import-list)))

(s/def ::ns-refer
  (s/spec (s/cat :clause #{:refer}
                 :lib simple-symbol?
                 :filters ::filters)))

;; same as ::prefix-list, but with ::use-libspec instead
(s/def ::use-prefix-list
  (s/spec
    (s/cat :prefix simple-symbol?
           :libspecs (s/+ ::use-libspec))))

;; same as ::libspec, but also supports the ::filters options in the libspec
(s/def ::use-libspec
  (s/alt :lib simple-symbol?
         :lib+opts (s/spec (s/cat :lib simple-symbol?
                                  :options (s/keys* :opt-un [::as ::refer ::exclude ::only ::rename])))))

(s/def ::ns-use
  (s/spec (s/cat :clause #{:use}
                 :libs (s/+ (s/alt :libspec ::use-libspec
                                   :prefix-list ::use-prefix-list
                                   :flag #{:reload :reload-all :verbose})))))

(s/def ::ns-load
  (s/spec (s/cat :clause #{:load}
                 :libs (s/* string?))))

(s/def ::name simple-symbol?)
(s/def ::extends simple-symbol?)
(s/def ::implements (s/coll-of simple-symbol? :kind vector?))
(s/def ::init symbol?)
(s/def ::class-ident (s/or :class simple-symbol? :class-name string?))
(s/def ::signature (s/coll-of ::class-ident :kind vector?))
(s/def ::constructors (s/map-of ::signature ::signature))
(s/def ::post-init symbol?)
(s/def ::method (s/and vector?
                  (s/cat :name simple-symbol?
                         :param-types ::signature
                         :return-type simple-symbol?)))
(s/def ::methods (s/coll-of ::method :kind vector?))
(s/def ::main boolean?)
(s/def ::factory simple-symbol?)
(s/def ::state simple-symbol?)
(s/def ::get simple-symbol?)
(s/def ::set simple-symbol?)
(s/def ::expose (s/keys :opt-un [::get ::set]))
(s/def ::exposes (s/map-of simple-symbol? ::expose))
(s/def ::prefix string?)
(s/def ::impl-ns simple-symbol?)
(s/def ::load-impl-ns boolean?)

(s/def ::ns-gen-class
  (s/spec (s/cat :clause #{:gen-class}
                 :options (s/keys* :opt-un [::name ::extends ::implements
                                            ::init ::constructors ::post-init
                                            ::methods ::main ::factory ::state
                                            ::exposes ::prefix ::impl-ns ::load-impl-ns]))))

(s/def ::ns-clauses
  (s/* (s/alt :refer-clojure ::ns-refer-clojure
              :require       ::ns-require
              :import        ::ns-import
              :use           ::ns-use
              :refer         ::ns-refer
              :load          ::ns-load
              :gen-class     ::ns-gen-class)))

(s/def ::ns-form
  (s/cat :name      simple-symbol?
         :docstring (s/? ::docstring)
         :attr-map  (s/? ::meta)
         :clauses   ::ns-clauses))

(s/fdef core/ns
  :args ::ns-form)

#?(:clj
(defmacro ^:private quotable
  "Returns a spec that accepts both the spec and a (quote ...) form of the spec"
  [spec]
  `(s/or :spec ~spec :quoted-spec (s/cat :quote #{'quote} :spec ~spec))))

(s/def ::quotable-import-list
  (s/* (s/alt :class (quotable simple-symbol?)
              :package-list (quotable ::package-list))))

(s/fdef core/import
  :args ::quotable-import-list)

(s/fdef core/refer-clojure
  :args (s/* (s/alt
               :exclude (s/cat :op (quotable #{:exclude}) :arg (quotable ::exclude))
               :only    (s/cat :op (quotable #{:only})    :arg (quotable ::only))
               :rename  (s/cat :op (quotable #{:rename})  :arg (quotable ::rename)))))

#_(do

;;; =====>>> PRIMITIVES <<<===== ;;;

#?(:clj  (def unboxed-boolean?   boolean))
#?(:clj  (def boxed-boolean?     Boolean))
         (def boolean?           (s/or '{:clj  (s/or boolean boxed-boolean?)
                                         :cljs js/Boolean}))

#?(:clj  (def unboxed-byte?      byte))
#?(:clj  (def boxed-byte?        Byte))
#?(:clj  (def byte?              (s/or byte boxed-byte?)))

#?(:clj  (def unboxed-char?      char))
#?(:clj  (def boxed-char?        Character))
#?(:clj  (def char?              (s/or char boxed-char?)))

#?(:clj  (def unboxed-short?     short))
#?(:clj  (def boxed-short?       Short))
#?(:clj  (def short?             (s/or short boxed-short?)))

#?(:clj  (def unboxed-int?       int))
#?(:clj  (def boxed-int?         Integer))
#?(:clj  (def int?               (s/or int boxed-int?)))

#?(:clj  (def unboxed-long?      long))
#?(:clj  (def boxed-long?        Long)) ; TODO CLJS may have this with goog.Long ?
#?(:clj  (def long?              (s/or long boxed-long?)))

#?(:clj  (def unboxed-float?     float))
#?(:clj  (def boxed-float?       Float))
#?(:clj  (def float?             (s/or float boxed-float?)))

#?(:clj  (def unboxed-double?    double))
#?(:clj  (def boxed-double?      (s/or Double)))
         (def double?            (s/or '{:clj  (s/or double boxed-double?)
                                         :cljs js/Number}))

#?(:clj  (def unboxed-primitive? (s/or boolean byte char short int long float double)))

#?(:clj  (def prim?              unboxed-primitive?))

; Possibly can't check for boxedness in Java because it does auto-(un)boxing, but it's nice to have
#?(:clj  (def boxed-primitive?   (s/or boxed-boolean? boxed-byte? boxed-char?
                                       boxed-short? boxed-int? boxed-long?
                                       boxed-float? boxed-double?)))

#?(:clj  (def prim-comparable?   (s/and prim? (s/not boolean))))

;;; =====>>> GENERAL <<<===== ;;;

         (def nil?               (s/== nil))

         (def object?            (s/or '{:clj Object :cljs js/Object}))

         (def any?               (s/or nil? primitive? object?
                                       '{:cljs (s/or js/Boolean js/Number js/String js/Symbol)}))

;;; =====>>> NUMBERS <<<===== ;;;

         (def bigint?            (s/or '{:clj  (s/or clojure.lang.BigInt java.math.BigInteger)
                                         :cljs com.gfredericks.goog.math.Integer}))

         (def integer?           (s/or byte? short? int? long? bigint?))

#?(:clj  (def bigdec?            (s/or java.math.BigDecimal))) ; TODO CLJS may have this

         (def decimal?           (s/or float? double? bigdec?))

         (def ratio?             (s/or '{:clj  clojure.lang.Ratio
                                         :cljs quantum.core.numeric.types.Ratio})) ; TODO add this CLJS entry to the predicate after the fact

#?(:clj  (def primitive-number?  (s/or short int long float double)))

         (def number?            (s/or '{:clj  (s/or primitive-number? Number)
                                         :cljs (s/or integer? decimal? ratio?)}))

; ----- NUMBER LIKENESSES ----- ;

         (def integer-value?     (s/or integer? (s/and decimal? decimal-is-integer-value?)))

         (def numeric-primitive?  (t/- primitive? boolean?))

         (def numerically-byte?   (t/and integer? #(<= -128                 % 127)))
         (def numerically-short?  (t/and integer? #(<= -32768               % 32767)))
         (def numerically-char?   (t/and integer? #(<=  0                   % 65535)))
         (def numerically-unsigned-short? numerically-char?)
         (def numerically-int?    (t/and integer? #(<= -2147483648          % 2147483647)))
         (def numerically-long?   (t/and integer? #(<= -9223372036854775808 % 9223372036854775807)))
         (def numerically-float?  (t/and decimal? representable-by-float?))  ; because there are 'holes'
         (def numerically-double? (t/and decimal? representable-by-double?)) ; because there are 'holes'

         (def int-like?           (s/and integer-value? numerically-int?))

;;; =====>>> COLLECTIONS <<<===== ;;;

; ----- TUPLES ----- ;

         (def tuple?             (s/or Tuple)) ; clojure.lang.Tuple was discontinued; we won't support it for now
#?(:clj  (def map-entry?         (s/or java.util.Map$Entry)))

;; ===== SEQUENCES ===== ;; Sequential (generally not efficient Lookup / RandomAccess)

         (def cons?              (s/or '{:clj  clojure.lang.Cons
                                         :cljs cljs.core/Cons}))
         (def lseq?              (s/or '{:clj  clojure.lang.LazySeq
                                         :cljs cljs.core/LazySeq}))
         (def misc-seq?          (s/or '{:clj  (s/or clojure.lang.APersistentMap$ValSeq
                                                     clojure.lang.APersistentMap$KeySeq
                                                     clojure.lang.PersistentVector$ChunkedSeq
                                                     clojure.lang.IndexedSeq)
                                         :cljs (s/or cljs.core/ValSeq
                                                     cljs.core/KeySeq
                                                     cljs.core/IndexedSeq
                                                     cljs.core/ChunkedSeq)}))

         (def non-list-seq?      (s/or cons? lseq? misc-seq?))

; ----- LISTS ----- ; Not extremely different from Sequences ; TODO clean this up

         (def cdlist?            nil
                                 #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                             quantum.core.data.finger_tree.CountedDoubleList}
                                     :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
         (def dlist?             nil
                                 #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                             quantum.core.data.finger_tree.CountedDoubleList}
                                    :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
         (def +list?             (s/or '{:clj  clojure.lang.IPersistentList
                                         :cljs (s/or cljs.core/List cljs.core/EmptyList)}))

#?(:clj  (def !list?             (s/or java.util.LinkedList)))
         (def list?              (s/or '{:clj java.util.List :cljs +list?}))

; ----- GENERIC ----- ;

         (def seq?               (s/or '{:clj  clojure.lang.ISeq
                                         :cljs (s/or non-list-seq? list?)}))

;; ===== MAPS ===== ;; Associative

; ----- Generators ----- ;

(defn <fastutil-package [x]
  (if (= x 'ref) "objects" (str (name x) "s")))

(defn <fastutil-long-form [x]
  (if (= x 'ref) "Reference" (-> x name str/capitalize)))

(defn !map-type:gen [from to suffixes]
  (let [fastutil-class-name-base
          (str "it.unimi.dsi.fastutil." (<fastutil-package from)
               "." (<fastutil-long-form from) "2" (<fastutil-long-form to))]
   `{:clj '~(->> suffixes
                 (map #(symbol (str fastutil-class-name-base %)))
                 set)}))

(defn !map-types*:gen [prefix genf ref->ref]
  (let [?prefix      (when prefix (str prefix "-"))
        base-types   (conj (keys primitive-type-meta) 'ref)
        type-combos  (->> base-types
                          (<- combo/selections 2)
                          (remove (fn-> first (= 'boolean))))
        gen-same-sym (fn [t]       (symbol (str "!" ?prefix "map:" t "-types")))
        gen-map-sym  (fn [from to] (symbol (str "!" ?prefix "map:" from "->" to "-types")))
        any-*-defs
          (->> base-types
               (map (fn [t]
                      (let [any-key-sym (symbol (str "!" ?prefix "map:" "any" "->" t     "-types"))
                            any-val-sym (symbol (str "!" ?prefix "map:" t     "->" "any" "-types"))
                            cond-union:any
                             (fn [pred] (list* 'cond-union
                                          (->> type-combos (filter (fn-> pred (= t))) (map (partial apply gen-map-sym)))))]
                        `(do (def ~any-key-sym ~(cond-union:any second))
                             (def ~any-val-sym ~(cond-union:any first )))))))
        sym=>code
         (->> type-combos
              (map (fn [[from to]]
                     (let [body (genf from to)
                           sym  (gen-map-sym from to)]
                       [sym `(do (def ~sym ~body)
                                 ~(when (= from to)
                                    `(def ~(gen-same-sym from) ~sym)))])))
              (into (map/om)))]
    (concat (vals sym=>code)
            [(let [ref->ref-sym (gen-map-sym 'ref 'ref)]
               `(do (def ~ref->ref-sym ~ref->ref)
                    (def ~(gen-same-sym 'ref) ~ref->ref-sym)))]
            any-*-defs
            [`(def ~(symbol (str "!" ?prefix "map-types")) (cond-union ~@(keys sym=>code)))])))

#?(:clj
(defmacro !hash-map-types:gen [ref->ref]
  `(do ~@(!map-types*:gen "hash"
           (fn [from to] (!map-type:gen from to #{"OpenHashMap" "OpenCustomHashMap"}))
           ref->ref))))

#?(:clj
(defmacro !unsorted-map-types:gen []
  `(do ~@(!map-types*:gen "unsorted"
           (fn [from to] (symbol (str "!hash-map:" from "->" to "-types")))
           '!hash-map:ref-types))))

#?(:clj
(defmacro !sorted-map-types:gen [ref->ref]
  `(do ~@(!map-types*:gen "sorted" (fn [from to] {}) ref->ref))))

#?(:clj
(defmacro !map-types:gen []
  ; technically also `object` for CLJS
  `(do ~@(!map-types*:gen nil (fn [from to] (!map-type:gen from to #{"Map"}))
           '(cond-union !unsorted-map:ref-types !sorted-map:ref-types)))))

(defn !set-type:gen [t suffixes]
  (let [fastutil-class-name-base
          (str "it.unimi.dsi.fastutil." (<fastutil-package t) "." (<fastutil-long-form t))]
   `{:clj '~(->> suffixes
                 (map #(symbol (str fastutil-class-name-base %)))
                 set)}))

(defn !set-types*:gen [prefix genf ref-val]
  (let [?prefix (when prefix (str prefix "-"))
        sym=>code
         (->> (conj (keys primitive-type-meta) 'ref)
              (remove (fn= 'boolean))
              (map (fn [t]
                     (let [body (genf t)
                           sym  (symbol (str "!" ?prefix "set:" t "-types"))]
                       [sym `(def ~sym ~body)])))
              (into (map/om)))]
    (concat (vals sym=>code)
            [`(def ~(symbol (str "!" ?prefix "set:ref-types")) ~ref-val)
             `(def ~(symbol (str "!" ?prefix "set-types")) (cond-union ~@(keys sym=>code)))])))

#?(:clj
(defmacro !hash-set-types:gen [ref-val]
  `(do ~@(!set-types*:gen "hash"
           (fn [t] (!set-type:gen t #{"OpenHashSet" "OpenCustomHashSet"}))
           ref-val))))

#?(:clj
(defmacro !unsorted-set-types:gen []
  `(do ~@(!set-types*:gen "unsorted"
           (fn [t] (symbol (str "!hash-set:" t "-types")))
           '!hash-set:ref-types))))

#?(:clj
(defmacro !sorted-set-types:gen [ref-val]
  `(do ~@(!set-types*:gen "sorted" (fn [t] {}) ref-val))))

#?(:clj
(defmacro !set-types:gen []
  `(do ~@(!set-types*:gen nil (fn [t] (!set-type:gen t #{"Set"}))
           '(cond-union !unsorted-set:ref-types !sorted-set:ref-types)))))

; ----- ;

         (def +array-map?        (s/or '{:clj  clojure.lang.PersistentArrayMap
                                         :cljs cljs.core/PersistentArrayMap}))
         (def !+array-map?       (s/or '{:clj  clojure.lang.PersistentArrayMap$TransientArrayMap
                                         :cljs cljs.core/TransientArrayMap}))
         (def ?!+array-map?      (s/or +array-map? !+array-map?))
         (def !array-map?        nil)
#?(:clj  (def !!array-map?       nil))
         (def array-map?         (s/or ?!+array-map?
                                       !array-map?
                                       #?(:clj !!array-map?)))

         (def +hash-map?         (s/or '{:clj  clojure.lang.PersistentHashMap
                                         :cljs cljs.core/PersistentHashMap}))
         (def !+hash-map?        (s/or '{:clj  clojure.lang.PersistentHashMap$TransientHashMap
                                         :cljs cljs.core/TransientHashMap}))
         (def ?!+hash-map?       (s/or +hash-map? !+hash-map?))

(!hash-map-types:gen
  '{:clj  #{java.util.HashMap
            java.util.IdentityHashMap
            it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
            it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenCustomHashMap}
    :cljs #{goog.structs.Map js/Map}})

#?(:clj  (def !!hash-map?        (s/or java.util.concurrent.ConcurrentHashMap)))
         (def hash-map?          (s/or ?!+hash-map? !hash-map? #?(:clj !!hash-map?)))

         (def +unsorted-map?     (s/or   +hash-map?   +array-map?))
         (def !+unsorted-map?    (s/or  !+hash-map?  !+array-map?))
         (def ?!+unsorted-map?   (s/or ?!+hash-map? ?!+array-map?))

(!unsorted-map-types:gen)

#?(:clj  (def !!unsorted-map?    !!hash-map-types))
         (def unsorted-map?      (s/or ?!+unsorted-map? !unsorted-map? #?(:clj !!unsorted-map?)))

         (def +sorted-map?       (s/or '{:clj  clojure.lang.PersistentTreeMap
                                         :cljs cljs.core/PersistentTreeMap}))
         (def !+sorted-map?      nil)
         (def ?!+sorted-map?     (s/or +sorted-map? !+sorted-map?))

(!sorted-map-types:gen
 '{:clj  #{java.util.TreeMap}
   :cljs #{goog.structs.AvlTree}})

#?(:clj  (def !!sorted-map?      nil))
         (def sorted-map-types   (s/or '{:clj  (s/or ?!+sorted-map? java.util.SortedMap)
                                         :cljs (s/or +sorted-map? !sorted-map?)}))

#?(:clj  (def !insertion-map?    (s/or java.util.LinkedHashMap)))
#?(:clj  (def +insertion-map?    (s/or flatland.ordered.map.OrderedMap)))
#?(:clj  (def insertion-map?     (s/or !insertion-map? +insertion-map?)))

         (def !+map?             (s/or '{:clj  clojure.lang.ITransientMap
                                         :cljs !+unsorted-map?}))
         (def +map?              (s/or '{:clj  clojure.lang.IPersistentMap
                                         :cljs (s/or +unsorted-map? +sorted-map?)}))
         (def ?!+map?            (s/or !+map? +map?))

(!map-types:gen)

#?(:clj  (def !!map?             (s/or !!unsorted-map? !!sorted-map?)))
         (def map?               (s/or {:clj  (s/or !+map? java.util.Map) ; TODO IPersistentMap as well, yes, but all persistent Clojure maps implement java.util.Map
                                                                          ; TODO add typed maps into this definition once lazy compilation is in place
                                        :cljs (s/or ?!+map? !map?)}))

; ===== SETS ===== ; Associative; A special type of Map whose keys and vals are identical

         (def +hash-set?         (s/or '{:clj  clojure.lang.PersistentHashSet
                                         :cljs cljs.core/PersistentHashSet}))
         (def !+hash-set?        (s/or '{:clj  clojure.lang.PersistentHashSet$TransientHashSet
                                         :cljs cljs.core/TransientHashSet}))
         (def ?!+hash-set?       (s/or !+hash-set? +hash-set?))

(!hash-set-types:gen
  '{:clj  #{java.util.HashSet
            #_java.util.IdentityHashSet}
    :cljs #{goog.structs.Set}})

#?(:clj  (def !!hash-set?        nil)) ; technically you can make something from ConcurrentHashMap but...
         (def hash-set?          (s/or ?!+hash-set? !hash-set? #?(:clj !!hash-set?)))

         (def +unsorted-set?     +hash-set?)
         (def !+unsorted-set?   !+hash-set?)
         (def ?!+unsorted-set? ?!+hash-set?)

(!unsorted-set-types:gen)

#?(:clj  (def !!unsorted-set?    !!hash-set?))
         (def unsorted-set?      hash-set?)

         ; TODO clojure.lang.Sorted + clojure.lang.IPersistentSet
         (def +sorted-set?       (s/or '{:clj  clojure.lang.PersistentTreeSet
                                         :cljs cljs.core/PersistentTreeSet}))
         (def !+sorted-set?      nil)
         (def ?!+sorted-set?     (s/or +sorted-set? !+sorted-set?))

(!sorted-set-types:gen
  '{:clj #{java.util.TreeSet}}) ; TODO CLJS can have via AVLTree with same KVs

#?(:clj  (def !!sorted-set?      nil))
         (def sorted-set?        (s/or '{:clj  (s/or +sorted-set? java.util.SortedSet)
                                         :cljs (s/or +sorted-set? !sorted-set?)}))

         (def !+set?             (s/or '{:clj  clojure.lang.ITransientSet
                                         :cljs !+unsorted-set?}))
         (def +set?              (s/or '{:clj  clojure.lang.IPersistentSet
                                         :cljs (s/or +unsorted-set? +sorted-set?)}))
         (def ?!+set?            (s/or !+set? +set?))

(!set-types:gen)

#?(:clj  (def !set:int?          it.unimi.dsi.fastutil.ints.IntSet))
#?(:clj  (def !set:long?         it.unimi.dsi.fastutil.longs.LongSet))
#?(:clj  (def !set:double?       it.unimi.dsi.fastutil.doubles.DoubleSet))
         (def !set:ref?          (s/or !unsorted-set:ref? !sorted-set:ref?))
         (def !set?              (s/or !unsorted-set? !sorted-set?))
#?(:clj  (def !!set?             (s/or !!unsorted-set? !!sorted-set?)))
         (def set?               (s/or '{:clj  (s/or !+set? java.util.Set) ; TODO IPersistentSet as well, yes, but all persistent Clojure sets implement java.util.Set
                                         :cljs (s/or ?!+set? !set?)}))

; ===== ARRAYS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                     ; dense integer values), not extensible
; TODO do e.g. {:clj {0 {:byte ...}}}
(def array-1d-types*      `{:clj  {:byte          (type (byte-array    0)      )
                                   :char          (type (char-array    "")     )
                                   :short         (type (short-array   0)      )
                                   :long          (type (long-array    0)      )
                                   :float         (type (float-array   0)      )
                                   :int           (type (int-array     0)      )
                                   :double        (type (double-array  0.0)    )
                                   :boolean       (type (boolean-array [false]))
                                   :object        (type (object-array  [])     )}
                            :cljs {:byte          js/Int8Array
                                   :ubyte         js/Uint8Array
                                   :ubyte-clamped js/Uint8ClampedArray
                                   :char          js/Uint16Array ; kind of
                                   :ushort        js/Uint16Array
                                   :short         js/Int16Array
                                   :int           js/Int32Array
                                   :uint          js/Uint32Array
                                   :float         js/Float32Array
                                   :double        js/Float64Array
                                   :object        (type (cljs.core/array))}})
(def undistinguished-array-1d-types (->> array-1d-types* (map (fn [[k v]] [k (-> v vals set)])) (into {})))
(def array-2d-types*       {:clj (->array-nd-types* 2 )})
(def array-3d-types*       {:clj (->array-nd-types* 3 )})
(def array-4d-types*       {:clj (->array-nd-types* 4 )})
(def array-5d-types*       {:clj (->array-nd-types* 5 )})
(def array-6d-types*       {:clj (->array-nd-types* 6 )})
(def array-7d-types*       {:clj (->array-nd-types* 7 )})
(def array-8d-types*       {:clj (->array-nd-types* 8 )})
(def array-9d-types*       {:clj (->array-nd-types* 9 )})
(def array-10d-types*      {:clj (->array-nd-types* 10)})
(def array-types           (cond-union (->> array-1d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-2d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-3d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-4d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-5d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-6d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-7d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-8d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-9d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                       (->> array-10d-types* (map (fn [[k v]] [k (-> v vals set)])) (into {}))))

; ----- STRING ----- ; A special wrapper for char array where different encodings, etc. are possible

         (def !string?           (s/or '{:clj StringBuilder :cljs goog.string.StringBuffer}))

         (def string?            (s/or '{:clj String :cljs js/String}))

#?(:clj  (def char-seq?          (s/or CharSequence)))

; ===== VECTORS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                      ; dense integer values), extensible

         (def !array-list?       (s/or '{:clj  (s/or java.util.ArrayList
                                                     java.util.Arrays$ArrayList) ; indexed and associative, but not extensible
                                         :cljs #_cljs.core.ArrayList ; not used
                                               js/Array})) ; because supports .push etc.
; svec = "spliceable vector"
         (def svector?           (s/or clojure.core.rrb_vector.rrbt.Vector))
         (def +vector?           (s/or '{:clj  clojure.lang.IPersistentVector
                                         :cljs (s/or svector? cljs.core/PersistentVector)}))
         (def !+vector?          (s/or '{:clj  clojure.lang.ITransientVector
                                         :cljs cljs.core/TransientVector}))
         (def ?!+vector?         (s/or +vector? !+vector?))
#?(:clj  (def !vector:long?      (s/or it.unimi.dsi.fastutil.longs.LongArrayList)))
         (def !vector:ref?       (s/or '{:clj  java.util.ArrayList
                                         :cljs js/Array})) ; because supports .push etc.
         (def !vector?           (s/or #?(:clj !vector:long?) !vector:ref?))
                                 ; java.util.Vector is deprecated, because you can
                                 ; just create a synchronized wrapper over an ArrayList
                                 ; via java.util.Collections
#?(:clj  (def !!vector?          nil))
         (def vector?            (s/or ?!+vector? !vector? #?(:clj !!vector?)))

; ===== QUEUES ===== ; Particularly FIFO queues, as LIFO = stack = any vector

         (def +queue?            (s/or '{:clj  clojure.lang.PersistentQueue
                                         :cljs cljs.core/PersistentQueue}))
         (def !+queue?           nil)
         (def ?!+queue?          (s/or +queue? !+queue?))
         (def !queue?            (s/or '{:clj  java.util.ArrayDeque ; TODO *MANY* more here
                                              :cljs goog.structs.Queue}))
#?(:clj  (def !!queue?           nil)) ; TODO *MANY* more here
         (def queue?             (s/or '{:clj  (s/or ?!+queue? java.util.Queue)
                                         :cljs (s/or ?!+queue? !queue?)}))

; ===== GENERIC ===== ;

; ----- COLLECTIONS ----- ;

                                 ; TODO this might be ambiguous
                                 ; TODO clojure.lang.Indexed / cljs.core/IIndexed?
         (def indexed?           (s/or array? string? vector?
                                   #?(:clj clojure.lang.APersistentVector$RSeq)))
                                 ; TODO this might be ambiguous
                                 ; TODO clojure.lang.Associative / cljs.core/IAssociative?
         (def associative?       (s/or map? set? indexed?))
                                 ; TODO this might be ambiguous
                                 ; TODO clojure.lang.Sequential / cljs.core/ISequential?
         (def sequential?        (s/or seq? list? indexed?))
                                 ; TODO this might be ambiguous
                                 ; TODO clojure.lang.ICollection / cljs.core/ICollection?
         (def counted?           (s/or array? string?
                                   '{:clj  (s/or !vector? !!vector?
                                                 !map?    !!map?
                                                 !set     !!set?
                                                 clojure.lang.Counted)
                                     :cljs (s/or vector? map? set?)}))

         (def coll?              (s/or sequential? associative?))

         (def sorted?            (s/or '{:clj  (s/or clojure.lang.Sorted java.util.SortedMap java.util.SortedSet)
                                         :cljs (s/or sorted-set? sorted-map?)})) ; TODO add in `cljs.core/ISorted

         (def transient?         (s/or '{:clj  clojure.lang.ITransientCollection
                                         :cljs cljs.core/ITransientCollection}))

         ; Collections that have Transient counterparts
         (def editable?          (s/or '{:clj  clojure.lang.IEditableCollection
                                         :cljs cljs.core/IEditableCollection}))

; ===== FUNCTIONS ===== ;

         (def fn?                (s/or '{:clj  clojure.lang.Fn
                                         :cljs (s/or js/Function cljs.core/Fn)}))
         (def ifn?               (s/or '{:clj  clojure.lang.IFn
                                         :cljs (s/or fn? cljs.core/IFn)})
         (def multimethod?       (s/or '{:clj  clojure.lang.MultiFn
                                         :Cljs cljs.core/MultiFn}))

; ===== MISCELLANEOUS ===== ;

         (def regex?          '{:clj  #{java.util.regex.Pattern}
                                     :cljs #{js/RegExp              }})

         (def atom?           '{:clj  #{clojure.lang.IAtom}
                                     :cljs #{cljs.core/Atom}})
         (def volatile?       '{:clj  #{clojure.lang.Volatile}
                                     :cljs #{cljs.core/Volatile}})
         (def atomic?          {:clj  (set/union (:clj atom-types) (:clj volatile-types)
                                             '#{java.util.concurrent.atomic.AtomicReference
                                                ; From the java.util.concurrent package:
                                                ; "Additionally, classes are provided only for those
                                                ;  types that are commonly useful in intended applications.
                                                ;  For example, there is no atomic class for representing
                                                ;  byte. In those infrequent cases where you would like
                                                ;  to do so, you can use an AtomicInteger to hold byte
                                                ;  values, and cast appropriately. You can also hold floats
                                                ;  using Float.floatToIntBits and Float.intBitstoFloat
                                                ;  conversions, and doubles using Double.doubleToLongBits
                                                ;  and Double.longBitsToDouble conversions.
                                                java.util.concurrent.atomic.AtomicBoolean
                                              #_java.util.concurrent.atomic.AtomicByte
                                              #_java.util.concurrent.atomic.AtomicShort
                                                java.util.concurrent.atomic.AtomicInteger
                                                java.util.concurrent.atomic.AtomicLong
                                              #_java.util.concurrent.atomic.AtomicFloat
                                              #_java.util.concurrent.atomic.AtomicDouble
                                                com.google.common.util.concurrent.AtomicDouble
                                              })})

         (def m2m-chan?       '{:clj  #{clojure.core.async.impl.channels.ManyToManyChannel}
                                     :cljs #{cljs.core.async.impl.channels/ManyToManyChannel}})

         (def chan?           '{:clj  #{clojure.core.async.impl.protocols.Channel}
                                     :cljs #{cljs.core.async.impl.channels/ManyToManyChannel
                                             #_"TODO more?"}})

         (def keyword?           (s/or '{:clj  clojure.lang.Keyword
                                         :cljs cljs.core/Keyword}))

         (def symbol?            (s/or '{:clj  clojure.lang.Symbol
                                         :cljs cljs.core/Symbol}))

         (def file?          '{:clj  #{java.io.File}
                                    :cljs #{#_js/File}}) ; isn't always available! Use an abstraction

         (def comparable?      {:clj  (set/union '#{byte char short int long float double} '#{Comparable})
                                     :cljs (:cljs number-types)})

         (def record?          (s/or '{:clj  clojure.lang.IRecord
                                       :cljs cljs.core/IRecord}))

         (def transformer?     '{:clj #{#_clojure.core.protocols.CollReduce ; no, in order to find most specific type
                                             quantum.core.type.defs.Transformer}
                                      :cljs #{#_cljs.core/IReduce ; CLJS problems with dispatching on protocol
                                              quantum.core.type.defs.Transformer}})

         (def booleans-types       {:clj #{(-> array-1d-types*  :clj  :boolean)}})
         (def bytes-types          {:clj #{(-> array-1d-types*  :clj  :byte   )} :cljs #{(-> array-1d-types* :cljs :byte   )}})
         (def ubytes-types         {                                             :cljs #{(-> array-1d-types* :cljs :ubyte  )}})
         (def ubytes-clamped-types {                                             :cljs #{(-> array-1d-types* :cljs :ubyte-clamped)}})
         (def chars-types          {:clj #{(-> array-1d-types*  :clj  :char   )} :cljs #{(-> array-1d-types* :cljs :char   )}})
         (def shorts-types         {:clj #{(-> array-1d-types*  :clj  :short  )} :cljs #{(-> array-1d-types* :cljs :short  )}})
         (def ushorts-types        {                                             :cljs #{(-> array-1d-types* :cljs :ushort )}})
         (def ints-types           {:clj #{(-> array-1d-types*  :clj  :int    )} :cljs #{(-> array-1d-types* :cljs :int    )}})
         (def uints-types          {                                             :cljs #{(-> array-1d-types* :cljs :uint  )}})
         (def longs-types          {:clj #{(-> array-1d-types*  :clj  :long   )} :cljs #{(-> array-1d-types* :cljs :long   )}})
         (def floats-types         {:clj #{(-> array-1d-types*  :clj  :float  )} :cljs #{(-> array-1d-types* :cljs :float  )}})
         (def doubles-types        {:clj #{(-> array-1d-types*  :clj  :double )} :cljs #{(-> array-1d-types* :cljs :double )}})
         (def objects-types        {:clj #{(-> array-1d-types*  :clj  :object )} :cljs #{(-> array-1d-types* :cljs :object )}})

         (def numeric-1d-types  (cond-union bytes-types
                                            ubytes-types
                                            ubytes-clamped-types
                                            chars-types
                                            shorts-types
                                            ints-types
                                            uints-types
                                            longs-types
                                            floats-types
                                            doubles-types))

         (def booleans-2d-types {:clj #{(-> array-2d-types* :clj :boolean)} :cljs #{(-> array-2d-types* :cljs :boolean)}})
         (def bytes-2d-types    {:clj #{(-> array-2d-types* :clj :byte   )} :cljs #{(-> array-2d-types* :cljs :byte   )}})
         (def chars-2d-types    {:clj #{(-> array-2d-types* :clj :char   )} :cljs #{(-> array-2d-types* :cljs :char   )}})
         (def shorts-2d-types   {:clj #{(-> array-2d-types* :clj :short  )} :cljs #{(-> array-2d-types* :cljs :short  )}})
         (def ints-2d-types     {:clj #{(-> array-2d-types* :clj :int    )} :cljs #{(-> array-2d-types* :cljs :int    )}})
         (def longs-2d-types    {:clj #{(-> array-2d-types* :clj :long   )} :cljs #{(-> array-2d-types* :cljs :long   )}})
         (def floats-2d-types   {:clj #{(-> array-2d-types* :clj :float  )} :cljs #{(-> array-2d-types* :cljs :float  )}})
         (def doubles-2d-types  {:clj #{(-> array-2d-types* :clj :double )} :cljs #{(-> array-2d-types* :cljs :double )}})
         (def objects-2d-types  {:clj #{(-> array-2d-types* :clj :object )} :cljs #{(-> array-2d-types* :cljs :object )}})
         (def numeric-2d-types  (cond-union bytes-2d-types
                                            chars-2d-types
                                            shorts-2d-types
                                            ints-2d-types
                                            longs-2d-types
                                            floats-2d-types
                                            doubles-2d-types))

; ===== PREDICATES ===== ;

#?(:clj
(defmacro gen-<type-pred=>type> []
  (->> (ns-interns *ns*)
       keys
       (filter (fn-> name (str/ends-with? "-types")))
       (map    (fn [t] [(list 'quote (symbol (str/replace (name t) #"-types$" "?")))
                        t]))
       (into   {}))))

(def type-pred=>type
  (merge (gen-<type-pred=>type>)
    {'default              {:clj  '#{Object}
                            :cljs '#{(quote default)}}
     'boolean-array?       {:clj #{(-> array-1d-types*  :clj  :boolean)}}
     'byte-array?          {:clj #{(-> array-1d-types*  :clj  :byte   )} :cljs #{(-> array-1d-types* :cljs :byte   )}}
     'ubyte-array?         {                                             :cljs #{(-> array-1d-types* :cljs :ubyte  )}}
     'ubyte-array-clamped? {                                             :cljs #{(-> array-1d-types* :cljs :ubyte-clamped)}}
     'char-array?          {:clj #{(-> array-1d-types*  :clj  :char   )} :cljs #{(-> array-1d-types* :cljs :char   )}}
     'short-array?         {:clj #{(-> array-1d-types*  :clj  :short  )} :cljs #{(-> array-1d-types* :cljs :short  )}}
     'ushort-array?        {                                             :cljs #{(-> array-1d-types* :cljs :ushort )}}
     'int-array?           {:clj #{(-> array-1d-types*  :clj  :int    )} :cljs #{(-> array-1d-types* :cljs :int    )}}
     'uint-array?          {                                             :cljs #{(-> array-1d-types* :cljs :uint  )}}
     'long-array?          {:clj #{(-> array-1d-types*  :clj  :long   )} :cljs #{(-> array-1d-types* :cljs :long   )}}
     'float-array?         {:clj #{(-> array-1d-types*  :clj  :float  )} :cljs #{(-> array-1d-types* :cljs :float  )}}
     'double-array?        {:clj #{(-> array-1d-types*  :clj  :double )} :cljs #{(-> array-1d-types* :cljs :double )}}
     'object-array?        {:clj #{(-> array-1d-types*  :clj  :object )} :cljs #{(-> array-1d-types* :cljs :object )}}

     'array-1d?            {:clj  (->> array-1d-types*  :clj  vals set)
                            :cljs (->> array-1d-types*  :cljs vals set)}

     'array-2d?            {:clj  (->> array-2d-types*  :clj  vals set)
                            :cljs (->> array-2d-types*  :cljs vals set)}
     'array-3d?            {:clj  (->> array-3d-types*  :clj  vals set)
                            :cljs (->> array-3d-types*  :cljs vals set)}
     'array-4d?            {:clj  (->> array-4d-types*  :clj  vals set)
                            :cljs (->> array-4d-types*  :cljs vals set)}
     'array-5d?            {:clj  (->> array-5d-types*  :clj  vals set)
                            :cljs (->> array-5d-types*  :cljs vals set)}
     'array-6d?            {:clj  (->> array-6d-types*  :clj  vals set)
                            :cljs (->> array-6d-types*  :cljs vals set)}
     'array-7d?            {:clj  (->> array-7d-types*  :clj  vals set)
                            :cljs (->> array-7d-types*  :cljs vals set)}
     'array-8d?            {:clj  (->> array-8d-types*  :clj  vals set)
                            :cljs (->> array-8d-types*  :cljs vals set)}
     'array-9d?            {:clj  (->> array-9d-types*  :clj  vals set)
                            :cljs (->> array-9d-types*  :cljs vals set)}
     'array-10d?           {:clj  (->> array-10d-types* :clj  vals set)
                            :cljs (->> array-10d-types* :cljs vals set)}}))
)
