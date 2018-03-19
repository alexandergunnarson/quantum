(ns
  ^{:doc "Definitions for types."
    :attribution "alexandergunnarson"}
  quantum.untyped.core.type.defs
  (:refer-clojure :exclude
    [boolean byte char short int long float double])
  (:require
    [clojure.core.async.impl.channels]
#?@(:clj
   [[clojure.core.async.impl.protocols]]
    :cljs
   [[com.gfredericks.goog.math.Integer]])
    [clojure.core                               :as core]
    [clojure.core.rrb-vector.rrbt]
    [clojure.string                             :as str]
    [quantum.untyped.core.data.map              :as umap
      :refer [om]]
    [quantum.untyped.core.data.set              :as uset]
    [quantum.untyped.core.data.tuple
      #?@(:cljs [:refer [Tuple]])]
    [quantum.untyped.core.fn
      :refer [<- fn-> fnl rcomp]]
    [quantum.untyped.core.form.evaluate
      :refer [env-lang]]
    [quantum.untyped.core.logic
      :refer [fn-and fn= condf1]]
    [quantum.untyped.core.numeric.combinatorics :as combo])
  (:import
    #?@(:clj  [#_clojure.core.async.impl.channels.ManyToManyChannel
               com.google.common.util.concurrent.AtomicDouble
               quantum.untyped.core.data.tuple.Tuple]
        :cljs [goog.string.StringBuffer
               goog.structs.Map
               goog.structs.Set
               goog.structs.AvlTree
               goog.structs.Queue])))

;; TODO transition all predicates in file to `t/<pred>`s once the old `defnt` is done away with

#?(:clj (def boolean Boolean/TYPE))
#?(:clj (def byte    Byte/TYPE))
#?(:clj (def char    Character/TYPE))
#?(:clj (def short   Short/TYPE))
#?(:clj (def int     Integer/TYPE))
#?(:clj (def long    Long/TYPE))
#?(:clj (def float   Float/TYPE))
#?(:clj (def double  Double/TYPE))

(def primitive-type-meta quantum.untyped.core.type/unboxed-symbol->type-meta)

(def array-ident->primitive-sym
  (->> unboxed-symbol->type-meta (map (juxt (rcomp val :array-ident) key)) (into {})))

(def elem-types-clj
  (->> unboxed-symbol->type-meta
       (map (fn [[k v]] [(:outer-type v) k]))
       (reduce
         (fn [m [k v]]
           (assoc m k v (symbol k) v))
         {})))

#?(:clj
(def boxed-types
  (->> unboxed-symbol->type-meta
       (map (fn [[k v]] [k (:boxed v)]))
       (into {}))))

#?(:clj
(def unboxed-types
  (zipmap (vals boxed-types) (keys boxed-types))))

#?(:clj
(def boxed->unboxed-types-evaled (->> unboxed-symbol->type-meta vals (map (juxt :boxed :unboxed)) (into {}) eval)))

(def max-values
  (->> unboxed-symbol->type-meta
       (map (fn [[k v]] [k (:max v)]))
       (into {})))

#?(:clj
(def promoted-types
  {'short  'int
   'byte   'short ; Because char is unsigned
   'char   'int
   'int    'long
   'float  'double}))

;; TODO move this
(defn max-type [types]
  (->> types
       (map (fn [type] [(get max-values type) type]))
       (remove (fn-> first nil?))
       (into (core/sorted-map-by >))
       first val))

#?(:clj (def class->str (fn-> str (.substring 6))))

(defn >array-nd-types [n]
  {:boolean (symbol (str (apply str (repeat n \[)) "Z"))
   :byte    (symbol (str (apply str (repeat n \[)) "B"))
   :char    (symbol (str (apply str (repeat n \[)) "C"))
   :short   (symbol (str (apply str (repeat n \[)) "S"))
   :int     (symbol (str (apply str (repeat n \[)) "I"))
   :long    (symbol (str (apply str (repeat n \[)) "J"))
   :float   (symbol (str (apply str (repeat n \[)) "F"))
   :double  (symbol (str (apply str (repeat n \[)) "D"))
   :object  (symbol (str (apply str (repeat n \[)) "Ljava.lang.Object;"))})

#_(t/def ::lang->type (t/map-of t/keyword? (t/set-of symbol?)))

;; Mainly for CLJ use within macros for doing type-related things with CLJS
(defonce *types|unevaled (atom {}))

;; Empty in CLJS, but may be used later so not excising
(defonce *types          (atom {}))

(defn reg-pred! [pred-sym #_t/symbol? data|unevaled #_::lang->type]
   (swap! *types|unevaled
     (fn [types|unevaled]
       (reduce (fn [ret [lang types-for-lang]] (cond-> ret (seq types-for-lang) (assoc-in [lang pred-sym] types-for-lang)))
               types|unevaled
               data|unevaled)))
#?(:clj (swap! *types assoc-in [:clj pred-sym] (-> data|unevaled :clj eval)))
   true)

(defn reg-preds! [pred->lang->type]
  (doseq [[pred lang->type] pred->lang->type]
    (reg-pred! pred lang->type)))

(defn- retrieve [lang #_t/keyword? preds]
  (->> preds (map #(get-in @*types|unevaled [lang %])) (remove empty?) (apply uset/union)))

(defn- preds>types [& preds]
  {:clj  (retrieve :clj  preds)
   :cljs (retrieve :cljs preds)})

(defn- types-union [& lang->types]
  {:clj  (->> lang->types (map :clj)  (apply uset/union))
   :cljs (->> lang->types (map :cljs) (apply uset/union))})

; ===== MAPS ===== ; Associative

; ----- Generators ----- ;

(defn- >fastutil-package [x]
  (if (= x 'ref) "objects" (str (name x) "s")))

(defn- >fastutil-long-form [x]
  (if (= x 'ref) "Reference" (-> x name str/capitalize)))

(defn- >lang->type|!map [k-type #_t/symbol? v-type #_t/symbol? suffixes #_(t/seqable-of t/string?)] #_> #_::lang->type
  (let [fastutil-class-name-base
          (str "it.unimi.dsi.fastutil." (>fastutil-package k-type)
               "." (>fastutil-long-form k-type) "2" (>fastutil-long-form v-type))]
    {:clj (->> suffixes
               (map #(symbol (str fastutil-class-name-base %)))
               set)}))

(defn- >pred->lang->type|!map|base
  [prefix      #_(? t/string?)
   >lang->type #_(t/spec t/fn? "Generates the `lang->type` corresponding to key and value map types")
   ref->ref    #_::lang->type]
  (let [?prefix     (when prefix (str prefix "-"))
        base-types  (conj (keys unboxed-symbol->type-meta) 'ref)
        type-combos (->> base-types
                         (<- (combo/selections 2))
                         ;; No `boolean->*` maps exist in fastutil, for obvious reasons
                         (remove (fn-> first (= 'boolean))))
        ;; To generate a map predicate symbol when `k-type` and `v-type` are the same
        >same-pred-sym (fn [t]               (symbol (str "!" ?prefix "map|" t "?")))
        >map-pred-sym  (fn [[k-type v-type]] (symbol (str "!" ?prefix "map|" k-type "->" v-type "?")))
        pred->lang->type|combos
          (->> type-combos
               (map (fn [[k-type v-type]]
                      (let [pred-sym   (>map-pred-sym [k-type v-type])
                            lang->type (>lang->type k-type v-type)]
                        (cond-> (om pred-sym lang->type)
                                (= k-type v-type) (assoc (>same-pred-sym k-type) lang->type)))))
               (reduce into (om)))
        pred->lang->type|any
          (->> base-types
               (map (fn [t]
                      (let [any-key-sym (symbol (str "!" ?prefix "map|" "any" "->" t     "?"))
                            any-val-sym (symbol (str "!" ?prefix "map|" t     "->" "any" "?"))
                            preds>types|any
                              (fn [getf] (->> type-combos
                                              (filter (fn-> getf (= t)))
                                              (map >map-pred-sym)
                                              (map (fn [pred-sym] (get pred->lang->type|combos pred-sym)))
                                              (apply types-union)))]
                        (om any-key-sym (preds>types|any second)
                            any-val-sym (preds>types|any first)))))
               (reduce into (om)))
        pred->lang->type|ref
          (om (>map-pred-sym ['ref 'ref]) ref->ref
              (>same-pred-sym 'ref)     ref->ref)
        pred->lang->type|non-general
          (reduce into (om) [pred->lang->type|combos pred->lang->type|any pred->lang->type|ref])
        pred->lang->type|general
          (om (symbol (str "!" ?prefix "map?")) (apply types-union (vals pred->lang->type|non-general)))]
    (reduce into (om) [pred->lang->type|non-general pred->lang->type|general])))

(defn- >pred->lang->type|!hash-map [lang->type|ref->ref]
  (>pred->lang->type|!map|base "hash"
    (fn [k-type #_t/symbol? v-type #_t/symbol?] (>lang->type|!map k-type v-type #{"OpenHashMap" "OpenCustomHashMap"}))
    lang->type|ref->ref))

;; TODO this is dependent on state of `*types|unevaled`
(defn- >pred->lang->type|!unsorted-map []
  (>pred->lang->type|!map|base "unsorted"
    (fn [k-type #_t/symbol? v-type #_t/symbol?] (preds>types (symbol (str "!hash-map|" k-type "->" v-type "?"))))
    (preds>types '!hash-map|ref?)))

(defn- >pred->lang->type|!sorted-map [lang->type|ref->ref]
  (>pred->lang->type|!map|base "sorted" (fn [k-type v-type] {}) lang->type|ref->ref))

;; TODO this is dependent on state of `*types|unevaled`
(defn- >pred->lang->type|!map []
  ;; technically also `object` for CLJS
  (>pred->lang->type|!map|base nil (fn [k-type #_t/symbol? v-type #_t/symbol?] (>lang->type|!map k-type v-type #{"Map"}))
    (preds>types '!unsorted-map|ref? '!sorted-map|ref?)))

(defn- >lang->type|!set [t suffixes] #_> #_::lang->type
  (let [fastutil-class-name-base
          (str "it.unimi.dsi.fastutil." (>fastutil-package t) "." (>fastutil-long-form t))]
    {:clj (->> suffixes
               (map #(symbol (str fastutil-class-name-base %)))
               set)}))

(defn- >pred->lang->type|!set|base
  [prefix         #_(? t/string?)
   >lang->type    #_(t/spec t/fn? "Generates the `lang->type` corresponding to Set type")
   lang->type|ref #_::lang->type]
  (let [?prefix (when prefix (str prefix "-"))
        pred->lang->type|base
          (->> (conj (keys unboxed-symbol->type-meta) 'ref)
               ;; No `boolean` sets exist in fastutil, for obvious reasons
               (remove (fn= 'boolean))
               (map (fn [t]
                      (let [pred-sym   (symbol (str "!" ?prefix "set|" t "?"))
                            lang->type (>lang->type t)]
                        [pred-sym lang->type])))
               (into (om)))]
    (assoc pred->lang->type|base
           (symbol (str "!" ?prefix "set|ref?")) lang->type|ref
           (symbol (str "!" ?prefix "set?"))     (apply types-union (vals pred->lang->type|base)))))

(defn- >pred->lang->type|!hash-set [lang->type|ref]
  (>pred->lang->type|!set|base "hash"
    (fn [t] (>lang->type|!set t #{"OpenHashSet" #_"OpenCustomHashSet"}))
    lang->type|ref))

;; TODO this is dependent on state of `*types|unevaled`
(defn- >pred->lang->type|!unsorted-set []
  (>pred->lang->type|!set|base "unsorted"
    (fn [t] (preds>types (symbol (str "!hash-set|" t "?"))))
    (preds>types '!hash-set|ref?)))

(defn- >pred->lang->type|!sorted-set [lang->type|ref]
  (>pred->lang->type|!set|base "sorted" (fn [t] {}) lang->type|ref))

;; TODO this is dependent on state of `*types|unevaled`
(defn- >pred->lang->type|!set []
  (>pred->lang->type|!set|base nil (fn [t] (>lang->type|!set t #{"Set"}))
    (preds>types '!unsorted-set|ref? '!sorted-set|ref?)))

; ----- ;

(reg-pred!   '+array-map?         '{:clj  #{clojure.lang.PersistentArrayMap}
                                    :cljs #{cljs.core/PersistentArrayMap}})
(reg-pred!  '!+array-map?         '{:clj  #{clojure.lang.PersistentArrayMap$TransientArrayMap}
                                    :cljs #{cljs.core/TransientArrayMap}})
(reg-pred! '?!+array-map?          (preds>types '!+array-map? '+array-map?))
(reg-pred!   '!array-map?          {})
(reg-pred!  '!!array-map?          {})
(reg-pred!    'array-map?          (preds>types '?!+array-map? '!array-map? '!!array-map?))

(reg-pred!   '+hash-map?          '{:clj  #{clojure.lang.PersistentHashMap}
                                    :cljs #{cljs.core/PersistentHashMap}})
(reg-pred!  '!+hash-map?          '{:clj  #{clojure.lang.PersistentHashMap$TransientHashMap}
                                    :cljs #{cljs.core/TransientHashMap}})
(reg-pred! '?!+hash-map?           (preds>types '!+hash-map? '+hash-map?))

(reg-preds!
  (>pred->lang->type|!hash-map
    '{:clj  #{java.util.HashMap
              java.util.IdentityHashMap
              it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
              it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenCustomHashMap}
      :cljs #{goog.structs.Map}}))

(reg-pred!  '!!hash-map?           '{:clj  #{java.util.concurrent.ConcurrentHashMap}})
(reg-pred!    'hash-map?            (preds>types '?!+hash-map? '!hash-map? '!!hash-map?))

(reg-pred!   '+unsorted-map?        (preds>types   '+hash-map?   '+array-map?))
(reg-pred!  '!+unsorted-map?        (preds>types  '!+hash-map?  '!+array-map?))
(reg-pred! '?!+unsorted-map?        (preds>types '?!+hash-map? '?!+array-map?))

(reg-preds! (>pred->lang->type|!unsorted-map))

(reg-pred! '!!unsorted-map?         (preds>types '!!hash-map?))
(reg-pred!   'unsorted-map?         (preds>types '?!+unsorted-map? '!unsorted-map? '!!unsorted-map?))

(reg-pred!   '+sorted-map?         '{:clj  #{clojure.lang.PersistentTreeMap}
                                     :cljs #{cljs.core/PersistentTreeMap}})
(reg-pred!  '!+sorted-map?          {})
(reg-pred! '?!+sorted-map?          (preds>types '+sorted-map? '!+sorted-map?))

(reg-preds!
  (>pred->lang->type|!sorted-map
   '{:clj  #{java.util.TreeMap}
     :cljs #{goog.structs.AvlTree}}))

(reg-pred! '!!sorted-map?           {})
(reg-pred!   'sorted-map?           {:clj  (uset/union (:clj (preds>types '?!+sorted-map?))
                                                       '#{java.util.SortedMap})
                                     :cljs (:cljs (preds>types '+sorted-map? '!sorted-map?))})

(reg-pred! '!insertion-ordered-map? {:clj '#{java.util.LinkedHashMap}})
(reg-pred! '+insertion-ordered-map? {:clj '#{flatland.ordered.map.OrderedMap}})
(reg-pred!  'insertion-ordered-map? (preds>types '!insertion-ordered-map?
                                                 '+insertion-ordered-map?))

(reg-pred!  '!+map?                 {:clj  '#{clojure.lang.ITransientMap}
                                     :cljs (:cljs (preds>types '!+unsorted-map?))})
(reg-pred!   '+map?                 {:clj  '#{clojure.lang.IPersistentMap}
                                     :cljs (:cljs (preds>types '+unsorted-map? '+sorted-map?))})
(reg-pred! '?!+map?                 (preds>types '!+map? '+map?))

(reg-preds! (>pred->lang->type|!map))

(reg-pred! '!!map?                  (preds>types '!!unsorted-map? '!!sorted-map?))
(reg-pred!   'map?                  {:clj  (uset/union (:clj (preds>types '!+map?))
                                             '#{;;' TODO IPersistentMap as well, yes, but all persistent Clojure maps implement java.util.Map
                                                ;; TODO add typed maps into this definition once lazy compilation is in place
                                                java.util.Map})
                                     :cljs (:cljs (preds>types '?!+map? '!map? '!!map?))})

; ===== SETS ===== ; Associative; A special type of Map whose keys and vals are identical

(reg-pred!   '+hash-set?           '{:clj  #{clojure.lang.PersistentHashSet}
                                     :cljs #{cljs.core/PersistentHashSet}})
(reg-pred!  '!+hash-set?           '{:clj  #{clojure.lang.PersistentHashSet$TransientHashSet}
                                     :cljs #{cljs.core/TransientHashSet}})
(reg-pred! '?!+hash-set?            (preds>types '!+hash-set? '+hash-set?))

(reg-preds!
  (>pred->lang->type|!hash-set
    '{:clj  #{java.util.HashSet
              #_java.util.IdentityHashSet}
      :cljs #{goog.structs.Set}}))

(reg-pred! '!!hash-set?             {}) ; technically you can make something from ConcurrentHashMap but...
(reg-pred!   'hash-set?             (preds>types '?!+hash-set? '!hash-set? '!!hash-set?))

(reg-pred!   '+unsorted-set?        (preds>types   '+hash-set?))
(reg-pred!  '!+unsorted-set?        (preds>types  '!+hash-set?))
(reg-pred! '?!+unsorted-set?        (preds>types '?!+hash-set?))

(reg-preds! (>pred->lang->type|!unsorted-set))

(reg-pred! '!!unsorted-set?         (preds>types '!!hash-set?))
(reg-pred!   'unsorted-set?         (preds>types   'hash-set?))

(reg-pred!   '+sorted-set?         '{:clj  #{clojure.lang.PersistentTreeSet}
                                     :cljs #{cljs.core/PersistentTreeSet}})
(reg-pred!  '!+sorted-set?          {})
(reg-pred! '?!+sorted-set?          (preds>types '+sorted-set? '!+sorted-set?))

(reg-preds!
  (>pred->lang->type|!sorted-set
    '{:clj #{java.util.TreeSet}})) ; CLJS can have via AVLTree with same KVs

(reg-pred! '!!sorted-set?           {})
(reg-pred!   'sorted-set?           {:clj  (uset/union (:clj (preds>types '+sorted-set?))
                                                       '#{java.util.SortedSet})
                                     :cljs (:cljs (preds>types '+sorted-set? '!sorted-set? '!!sorted-set?))})

(reg-pred!  '!+set?                 {:clj  '#{clojure.lang.ITransientSet}
                                     :cljs (:cljs (preds>types '!+unsorted-set?))})
(reg-pred!   '+set?                 {:clj '#{clojure.lang.IPersistentSet}
                                     :cljs (:cljs (preds>types '+unsorted-set? '+sorted-set?))})
(reg-pred! '?!+set?                 (preds>types '!+set? '+set?))

(reg-preds! (>pred->lang->type|!set))

(reg-pred!  '!set|int?              {:clj '#{it.unimi.dsi.fastutil.ints.IntSet}})
(reg-pred!  '!set|long?             {:clj '#{it.unimi.dsi.fastutil.longs.LongSet}})
(reg-pred!  '!set|double?           {:clj '#{it.unimi.dsi.fastutil.doubles.DoubleSet}})
(reg-pred!  '!set|ref?              (preds>types '!unsorted-set|ref? '!sorted-set|ref?))
(reg-pred!  '!set?                  (preds>types '!unsorted-set? '!sorted-set?))
(reg-pred! '!!set?                  (preds>types '!!unsorted-set? '!!sorted-set?))
(reg-pred!   'set?                  {:clj  (uset/union (:clj (preds>types '!+set?))
                                             '#{;; TODO IPersistentSet as well, yes, but all persistent Clojure sets implement java.util.Set
                                                java.util.Set})
                                     :cljs (:clj (preds>types '?!+set? '!set? '!!set?))})

; ===== ARRAYS ===== ;
; TODO do e.g. {:clj {0 {:byte ...}}}
(def array-1d-types        {:clj   {:boolean       (symbol "[Z")
                                    :byte          (symbol "[B")
                                    :char          (symbol "[C")
                                    :short         (symbol "[S")
                                    :long          (symbol "[J")
                                    :float         (symbol "[F")
                                    :int           (symbol "[I")
                                    :double        (symbol "[D")
                                    :object        (symbol "[Ljava.lang.Object;")}
                            :cljs '{:byte          js/Int8Array
                                    :ubyte         js/Uint8Array
                                    :ubyte-clamped js/Uint8ClampedArray
                                    :char          js/Uint16Array ; kind of
                                    :ushort        js/Uint16Array
                                    :short         js/Int16Array
                                    :int           js/Int32Array
                                    :uint          js/Uint32Array
                                    :float         js/Float32Array
                                    :double        js/Float64Array
                                    :object        js/Array}})

(reg-pred! 'undistinguished-array-1d? (->> array-1d-types (map (fn [[k v]] [k (-> v vals set)])) (into {})))

(def array-2d-types        {:clj (>array-nd-types 2 )})
(def array-3d-types        {:clj (>array-nd-types 3 )})
(def array-4d-types        {:clj (>array-nd-types 4 )})
(def array-5d-types        {:clj (>array-nd-types 5 )})
(def array-6d-types        {:clj (>array-nd-types 6 )})
(def array-7d-types        {:clj (>array-nd-types 7 )})
(def array-8d-types        {:clj (>array-nd-types 8 )})
(def array-9d-types        {:clj (>array-nd-types 9 )})
(def array-10d-types       {:clj (>array-nd-types 10)})
(reg-pred! 'array?         (types-union (->> array-1d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-2d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-3d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-4d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-5d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-6d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-7d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-8d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-9d-types  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-10d-types (map (fn [[k v]] [k (-> v vals set)])) (into {}))))

(reg-pred! 'booleans?       {:clj #{(-> array-1d-types  :clj  :boolean)}})
(reg-pred! 'boolean-array?  (preds>types 'booleans?))
(reg-pred! 'bytes?          {:clj #{(-> array-1d-types  :clj  :byte   )} :cljs #{(-> array-1d-types :cljs :byte   )}})
(reg-pred! 'byte-array?     (preds>types 'bytes?))
(reg-pred! 'ubytes?         {                                            :cljs #{(-> array-1d-types :cljs :ubyte  )}})
(reg-pred! 'ubyte-array?    (preds>types 'ubytes?))
(reg-pred! 'ubytes-clamped? {                                            :cljs #{(-> array-1d-types :cljs :ubyte-clamped)}})
(reg-pred! 'ubyte-array-clamped? (preds>types 'ubytes-clamped?))
(reg-pred! 'chars?          {:clj #{(-> array-1d-types  :clj  :char   )} :cljs #{(-> array-1d-types :cljs :char   )}})
(reg-pred! 'char-array?     (preds>types 'chars?))
(reg-pred! 'shorts?         {:clj #{(-> array-1d-types  :clj  :short  )} :cljs #{(-> array-1d-types :cljs :short  )}})
(reg-pred! 'short-array?    (preds>types 'shorts?))
(reg-pred! 'ushorts?        {                                            :cljs #{(-> array-1d-types :cljs :ushort )}})
(reg-pred! 'ushort-array?   (preds>types 'ushorts?))
(reg-pred! 'ints?           {:clj #{(-> array-1d-types  :clj  :int    )} :cljs #{(-> array-1d-types :cljs :int    )}})
(reg-pred! 'int-array?      (preds>types 'ints?))
(reg-pred! 'uints?          {                                            :cljs #{(-> array-1d-types :cljs :uint  )}})
(reg-pred! 'uint-array?     (preds>types 'uints?))
(reg-pred! 'longs?          {:clj #{(-> array-1d-types  :clj  :long   )} :cljs #{(-> array-1d-types :cljs :long   )}})
(reg-pred! 'long-array?     (preds>types 'longs?))
(reg-pred! 'floats?         {:clj #{(-> array-1d-types  :clj  :float  )} :cljs #{(-> array-1d-types :cljs :float  )}})
(reg-pred! 'float-array?    (preds>types 'floats?))
(reg-pred! 'doubles?        {:clj #{(-> array-1d-types  :clj  :double )} :cljs #{(-> array-1d-types :cljs :double )}})
(reg-pred! 'double-array?   (preds>types 'doubles?))
(reg-pred! 'objects?        {:clj #{(-> array-1d-types  :clj  :object )} :cljs #{(-> array-1d-types :cljs :object )}})
(reg-pred! 'object-array?   (preds>types 'objects?))

(reg-pred! 'array-1d?       {:clj  (->> array-1d-types  :clj  vals set)
                             :cljs (->> array-1d-types  :cljs vals set)})


(reg-pred! 'numeric-1d?     (preds>types 'bytes? 'ubytes? 'ubytes-clamped?
                                         'chars?
                                         'shorts? 'ints? 'uints? 'longs?
                                         'floats? 'doubles?))

(reg-pred! 'booleans-2d?    {:clj #{(-> array-2d-types  :clj :boolean)} :cljs #{(-> array-2d-types :cljs :boolean)}})
(reg-pred! 'bytes-2d?       {:clj #{(-> array-2d-types  :clj :byte   )} :cljs #{(-> array-2d-types :cljs :byte   )}})
(reg-pred! 'chars-2d?       {:clj #{(-> array-2d-types  :clj :char   )} :cljs #{(-> array-2d-types :cljs :char   )}})
(reg-pred! 'shorts-2d?      {:clj #{(-> array-2d-types  :clj :short  )} :cljs #{(-> array-2d-types :cljs :short  )}})
(reg-pred! 'ints-2d?        {:clj #{(-> array-2d-types  :clj :int    )} :cljs #{(-> array-2d-types :cljs :int    )}})
(reg-pred! 'longs-2d?       {:clj #{(-> array-2d-types  :clj :long   )} :cljs #{(-> array-2d-types :cljs :long   )}})
(reg-pred! 'floats-2d?      {:clj #{(-> array-2d-types  :clj :float  )} :cljs #{(-> array-2d-types :cljs :float  )}})
(reg-pred! 'doubles-2d?     {:clj #{(-> array-2d-types  :clj :double )} :cljs #{(-> array-2d-types :cljs :double )}})
(reg-pred! 'objects-2d?     {:clj #{(-> array-2d-types  :clj :object )} :cljs #{(-> array-2d-types :cljs :object )}})

(reg-pred! 'array-2d?       {:clj  (->> array-2d-types  :clj  vals set)
                             :cljs (->> array-2d-types  :cljs vals set)})

(reg-pred! 'numeric-2d?     (preds>types 'bytes-2d?
                                         'chars-2d?
                                         'shorts-2d?
                                         'ints-2d?
                                         'longs-2d?
                                         'floats-2d?
                                         'doubles-2d?))

(reg-pred! 'array-3d?       {:clj  (->> array-3d-types   :clj  vals set)
                             :cljs (->> array-3d-types   :cljs vals set)})
(reg-pred! 'array-4d?       {:clj  (->> array-4d-types   :clj  vals set)
                             :cljs (->> array-4d-types   :cljs vals set)})
(reg-pred! 'array-5d?       {:clj  (->> array-5d-types   :clj  vals set)
                             :cljs (->> array-5d-types   :cljs vals set)})
(reg-pred! 'array-6d?       {:clj  (->> array-6d-types   :clj  vals set)
                             :cljs (->> array-6d-types   :cljs vals set)})
(reg-pred! 'array-7d?       {:clj  (->> array-7d-types   :clj  vals set)
                             :cljs (->> array-7d-types   :cljs vals set)})
(reg-pred! 'array-8d?       {:clj  (->> array-8d-types   :clj  vals set)
                             :cljs (->> array-8d-types   :cljs vals set)})
(reg-pred! 'array-9d?       {:clj  (->> array-9d-types   :clj  vals set)
                             :cljs (->> array-9d-types   :cljs vals set)})
(reg-pred! 'array-10d?      {:clj  (->> array-10d-types  :clj  vals set)
                             :cljs (->> array-10d-types  :cljs vals set)})

(reg-pred! 'objects-nd?     {:clj  #{(-> array-1d-types  :clj  :object )
                                     (-> array-2d-types  :clj  :object )
                                     (-> array-3d-types  :clj  :object )
                                     (-> array-4d-types  :clj  :object )
                                     (-> array-5d-types  :clj  :object )
                                     (-> array-6d-types  :clj  :object )
                                     (-> array-7d-types  :clj  :object )
                                     (-> array-8d-types  :clj  :object )
                                     (-> array-9d-types  :clj  :object )
                                     (-> array-10d-types :clj  :object ) }
                             :cljs (:cljs (preds>types 'objects?))})

;; ===== Predicates ===== ;;

;; TODO this is just a temporary thing and breaks extensibility
(def types          @*types)
;; TODO this is just a temporary thing and breaks extensibility
(def types|unevaled @*types|unevaled)
