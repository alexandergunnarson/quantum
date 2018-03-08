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
#?(:cljs
  (:require-macros
    [quantum.untyped.core.type.defs :as self
      :refer [->array-nd-types*]]))
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

(def ^{:doc "Could do <Class>/MAX_VALUE for the maxes in Java but JS doesn't like it of course
             In JavaScript, all numbers are 64-bit floating point numbers.
             This means you can't represent in JavaScript all the Java longs
             Max 'safe' int: (dec (Math/pow 2 53))"}
  primitive-type-meta
  {'boolean {:bits 1
             :min  0
             :max  1
   #?@(:clj [:array-ident "Z"
             :outer-type "[Z"
             :boxed      'java.lang.Boolean
             :unboxed    'Boolean/TYPE])}
   'byte    {:bits 8
             :min -128
             :max  127
   #?@(:clj [:array-ident "B"
             :outer-type  "[B"
             :boxed       'java.lang.Byte
             :unboxed     'Byte/TYPE])}
   'short   {:bits 16
             :min -32768
             :max  32767
   #?@(:clj [:array-ident "S"
             :outer-type  "[S"
             :boxed       'java.lang.Short
             :unboxed     'Short/TYPE])}
   'char    {:bits 16
             :min  0
             :max  65535
   #?@(:clj [:array-ident "C"
             :outer-type  "[C"
             :boxed       'java.lang.Character
             :unboxed     'Character/TYPE])}
   'int     {:bits 32
             :min -2147483648
             :max  2147483647
   #?@(:clj [:array-ident "I"
             :outer-type  "[I"
             :boxed       'java.lang.Integer
             :unboxed     'Integer/TYPE])}
   'long    {:bits 64
             :min -9223372036854775808
             :max  9223372036854775807
   #?@(:clj [:array-ident "J"
             :outer-type  "[J"
             :boxed       'java.lang.Long
             :unboxed     'Long/TYPE])}
   ; Technically with floating-point nums, "min" isn't the most negative;
   ; it's the smallest absolute
   'float   {:bits 32
             :min  1.4E-45
             :max  3.4028235E38
   #?@(:clj [:array-ident "F"
             :outer-type  "[F"
             :boxed       'java.lang.Float
             :unboxed     'Float/TYPE])}
   'double  {:bits 64
             ; Because:
             ; Double/MIN_VALUE        = 4.9E-324
             ; (.-MIN_VALUE js/Number) = 5e-324
             :min  #?(:clj  Double/MIN_VALUE
                      :cljs (.-MIN_VALUE js/Number))
             :max  1.7976931348623157E308 ; Max number in JS
   #?@(:clj [:array-ident "D"
             :outer-type  "[D"
             :boxed       'java.lang.Double
             :unboxed     'Double/TYPE])}})

(def array-ident->primitive-sym
  (->> primitive-type-meta (map (juxt (rcomp val :array-ident) key)) (into {})))

(def elem-types-clj
  (->> primitive-type-meta
       (map (fn [[k v]] [(:outer-type v) k]))
       (reduce
         (fn [m [k v]]
           (assoc m k v (symbol k) v))
         {})))

#?(:clj
(def boxed-types*
  (->> primitive-type-meta
       (map (fn [[k v]] [k (:boxed v)]))
       (into {}))))

#?(:clj
(def unboxed-types*
  (zipmap (vals boxed-types*) (keys boxed-types*))))

#?(:clj
(def boxed->unboxed-types-evaled (->> primitive-type-meta vals (map (juxt :boxed :unboxed)) (into {}) eval)))

(def max-values
  (->> primitive-type-meta
       (map (fn [[k v]] [k (:max v)]))
       (into {})))

#?(:clj
(def promoted-types*
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

#?(:clj
(defmacro ->array-nd-types* [n]
  `{:boolean '~(symbol (str (apply str (repeat n \[)) "Z"))
    :byte    '~(symbol (str (apply str (repeat n \[)) "B"))
    :char    '~(symbol (str (apply str (repeat n \[)) "C"))
    :short   '~(symbol (str (apply str (repeat n \[)) "S"))
    :int     '~(symbol (str (apply str (repeat n \[)) "I"))
    :long    '~(symbol (str (apply str (repeat n \[)) "J"))
    :float   '~(symbol (str (apply str (repeat n \[)) "F"))
    :double  '~(symbol (str (apply str (repeat n \[)) "D"))
    :object  '~(symbol (str (apply str (repeat n \[)) "Ljava.lang.Object;"))}))

#_(t/def ::lang->type (t/map-of t/keyword? (t/set-of symbol?)))

;; Mainly for CLJ use within macros for doing type-related things with CLJS
(def *types|unevaled (atom {}))

;; Empty in CLJS, but may be used later so not excising
(def *types          (atom {}))

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

(reg-pred! 'default         '{:clj  #{Object}
                              :cljs #{(quote default)}})

; ______________________ ;
; ===== PRIMITIVES ===== ;
; •••••••••••••••••••••• ;

(reg-pred! 'nil?            '{:clj #{nil} :cljs #{nil}})

; ===== NON-NUMERIC PRIMITIVES ===== ; ; TODO CLJS

(reg-pred! 'unboxed-bool?   '{:clj  #{boolean}
                              :cljs #{js/Boolean}})
(reg-pred! 'unboxed-boolean? (preds>types 'unboxed-bool?))
(reg-pred! 'boxed-bool?     '{:clj  #{java.lang.Boolean}
                              :cljs #{js/Boolean}})
(reg-pred! 'boxed-boolean?   (preds>types 'boxed-bool?))
(reg-pred! '?bool?           (preds>types 'boxed-bool?))
(reg-pred! '?boolean?        (preds>types '?bool?))
(reg-pred! 'bool?            (preds>types 'unboxed-bool? 'boxed-bool?))
(reg-pred! 'boolean?         (preds>types 'bool?))

(reg-pred! 'unboxed-byte?   '{:clj  #{byte}})
(reg-pred! 'boxed-byte?     '{:clj  #{java.lang.Byte}})
(reg-pred! '?byte?           (preds>types 'boxed-byte?))
(reg-pred! 'byte?            (preds>types 'unboxed-byte? 'boxed-byte?))

(reg-pred! 'unboxed-char?   '{:clj  #{char}})
(reg-pred! 'boxed-char?     '{:clj  #{java.lang.Character}})
(reg-pred! '?char?           (preds>types 'boxed-char?))
(reg-pred! 'char?            (preds>types 'unboxed-char? 'boxed-char?))

; ===== NUMBERS ===== ; ; TODO CLJS

; ----- INTEGERS ----- ;

(reg-pred! 'unboxed-short?  '{:clj  #{short}})
(reg-pred! 'boxed-short?    '{:clj  #{java.lang.Short}})
(reg-pred! '?short?          (preds>types 'boxed-short?))
(reg-pred! 'short?           (preds>types 'unboxed-short? 'boxed-short?))

(reg-pred! 'unboxed-int?    '{:clj  #{int}
                              ;; because the integral values representable by JS numbers are in the
                              ;; range of Java ints, though technically one needs to ensure that
                              ;; there is only an integral value, no decimal value
                              :cljs #{js/Number}})
(reg-pred! 'boxed-int?      '{:clj  #{java.lang.Integer}
                              :cljs #{js/Number}})
(reg-pred! '?int?            (preds>types 'boxed-int?))
(reg-pred! 'int?             (preds>types 'unboxed-int? 'boxed-int?))

(reg-pred! 'unboxed-long?   '{:clj  #{long}})
(reg-pred! 'boxed-long?     '{:clj  #{java.lang.Long}})
(reg-pred! '?long?           (preds>types 'boxed-long?))
(reg-pred! 'long?            (preds>types 'unboxed-long? 'boxed-long?))

(reg-pred! 'bigint?         '{:clj  #{clojure.lang.BigInt java.math.BigInteger}
                              :cljs #{com.gfredericks.goog.math.Integer}})

(reg-pred! 'integer?         (preds>types 'unboxed-short? 'unboxed-int? 'unboxed-long? 'bigint?))

; ----- DECIMALS ----- ;

(reg-pred! 'unboxed-float?  '{:clj  #{float}})
(reg-pred! 'boxed-float?    '{:clj  #{java.lang.Float}})
(reg-pred! '?float?          (preds>types 'boxed-float?))
(reg-pred! 'float?           (preds>types 'unboxed-float? 'boxed-float?))

(reg-pred! 'unboxed-double? '{:clj  #{double}
                              :cljs #{js/Number}})
(reg-pred! 'boxed-double?   '{:clj  #{java.lang.Double}
                              :cljs #{js/Number}})
(reg-pred! '?double?         (preds>types 'boxed-double?))
(reg-pred! 'double?          (preds>types 'unboxed-double? 'boxed-double?))

(reg-pred! 'bigdec?         '{:clj #{java.math.BigDecimal}})

(reg-pred! 'decimal?         (preds>types 'unboxed-float? 'unboxed-double? 'bigdec?))

; ----- GENERAL ----- ;

(reg-pred! 'ratio?          '{:clj  #{clojure.lang.Ratio}
                              :cljs #{quantum.core.numeric.types.Ratio}})

(reg-pred! 'number?          {:clj  (uset/union
                                      (:clj (preds>types 'unboxed-short? 'unboxed-int? 'unboxed-long?
                                                         'unboxed-float? 'unboxed-double?))
                                      '#{java.lang.Number})
                              :cljs (:cljs (preds>types 'integer? 'decimal? 'ratio?))})

;; 'Platform number'
(reg-pred! 'pnumber?        '{:cljs #{js/Number}})

;; The closest thing to a native int the platform has
(reg-pred! 'nat-int?        '{:clj  #{int}
                              :cljs #{js/Number}})

;; The closest thing to a native long the platform has
(reg-pred! 'nat-long?       '{:clj  #{long}
                              :cljs #{js/Number}})

; _______________________ ;
; ===== COLLECTIONS ===== ;
; ••••••••••••••••••••••• ;

; ===== TUPLES ===== ;

(reg-pred! 'tuple?          '{:clj  #{Tuple} ; clojure.lang.Tuple was discontinued; we won't support it for now
                              :cljs #{Tuple}})
(reg-pred! 'map-entry?      '{:clj #{java.util.Map$Entry}})

; ===== SEQUENCES ===== ; Sequential (generally not efficient Lookup / RandomAccess)

(reg-pred! 'cons?           '{:clj  #{clojure.lang.Cons}
                              :cljs #{cljs.core/Cons}})
(reg-pred! 'lseq?           '{:clj  #{clojure.lang.LazySeq}
                              :cljs #{cljs.core/LazySeq}})
(reg-pred! 'misc-seq?       '{:clj  #{clojure.lang.APersistentMap$ValSeq
                                      clojure.lang.APersistentMap$KeySeq
                                      clojure.lang.PersistentVector$ChunkedSeq
                                      clojure.lang.IndexedSeq}
                              :cljs #{cljs.core/ValSeq
                                      cljs.core/KeySeq
                                      cljs.core/IndexedSeq
                                      cljs.core/ChunkedSeq}})

(reg-pred! 'non-list-seq?    (preds>types 'cons? 'lseq? 'misc-seq?))

; ----- LISTS ----- ; Not extremely different from Sequences ; TODO clean this up

(reg-pred! 'cdlist?          {}
                          #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                      quantum.core.data.finger_tree.CountedDoubleList}
                              :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
(reg-pred! 'dlist?           {}
                          #_'{:clj  #{clojure.data.finger_tree.CountedDoubleList
                                      quantum.core.data.finger_tree.CountedDoubleList}
                              :cljs #{quantum.core.data.finger-tree/CountedDoubleList}})
(reg-pred! '+list?           {:clj  '#{clojure.lang.IPersistentList}
                              :cljs (uset/union (:cljs (preds>types 'dlist? 'cdlist?))
                                                '#{cljs.core/List cljs.core/EmptyList})})
(reg-pred! '!list?           '{:clj  #{java.util.LinkedList}})
(reg-pred!  'list?           {:clj  '#{java.util.List}
                              :cljs (:cljs (preds>types '+list?))})

; ----- GENERIC ----- ;

(reg-pred! 'seq?             {:clj  '#{clojure.lang.ISeq}
                              :cljs (:cljs (preds>types 'non-list-seq? 'list?))})

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
        base-types  (conj (keys primitive-type-meta) 'ref)
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
          (->> (conj (keys primitive-type-meta) 'ref)
               ;; No `boolean` sets exist in fastutil, for obvious reasons
               (remove (fn= 'boolean))
               (map (fn [t]
                      (let [pred-sym   (symbol (str "!" ?prefix "set|" t "-types"))
                            lang->type (>lang->type t)]
                        [pred-sym lang->type])))
               (into (om)))]
    (assoc pred->lang->type|base
           (symbol (str "!" ?prefix "set|ref-types")) lang->type|ref
           (symbol (str "!" ?prefix "set-types"))     (apply types-union (vals pred->lang->type|base)))))

(defn- >pred->lang->type|!hash-set [lang->type|ref]
  (>pred->lang->type|!set|base "hash"
    (fn [t] (>lang->type|!set t #{"OpenHashSet" #_"OpenCustomHashSet"}))
    lang->type|ref))

;; TODO this is dependent on state of `*types|unevaled`
(defn- >pred->lang->type|!unsorted-set []
  (>pred->lang->type|!set|base "unsorted"
    (fn [t] (preds>types (symbol (str "!hash-set|" t "-types"))))
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

; ===== ARRAYS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                     ; dense integer values), not extensible
; TODO do e.g. {:clj {0 {:byte ...}}}
(def array-1d-types*      '{:clj  {:boolean       (symbol "[Z")
                                   :byte          (symbol "[B")
                                   :char          (symbol "[C")
                                   :short         (symbol "[S")
                                   :long          (symbol "[J")
                                   :float         (symbol "[F")
                                   :int           (symbol "[I")
                                   :double        (symbol "[D")
                                   :object        (symbol "[Ljava.lang.Object;")}
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
                                   :object        js/Array}})
(reg-pred! 'undistinguished-array-1d? (->> array-1d-types* (map (fn [[k v]] [k (-> v vals set)])) (into {})))
(def array-2d-types*       {:clj (->array-nd-types* 2 )})
(def array-3d-types*       {:clj (->array-nd-types* 3 )})
(def array-4d-types*       {:clj (->array-nd-types* 4 )})
(def array-5d-types*       {:clj (->array-nd-types* 5 )})
(def array-6d-types*       {:clj (->array-nd-types* 6 )})
(def array-7d-types*       {:clj (->array-nd-types* 7 )})
(def array-8d-types*       {:clj (->array-nd-types* 8 )})
(def array-9d-types*       {:clj (->array-nd-types* 9 )})
(def array-10d-types*      {:clj (->array-nd-types* 10)})
(reg-pred! 'array?         (preds>types (->> array-1d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-2d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-3d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-4d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-5d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-6d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-7d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-8d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-9d-types*  (map (fn [[k v]] [k (-> v vals set)])) (into {}))
                                        (->> array-10d-types* (map (fn [[k v]] [k (-> v vals set)])) (into {}))))

; String: A special wrapper for char array where different encodings, etc. are possible

;; Mutable String
(reg-pred! '!string?        '{:clj  #{StringBuilder}
                              :cljs #{goog.string.StringBuffer}})
;; Immutable String
(reg-pred! 'string?         '{:clj  #{String}
                              :cljs #{js/String}})

(reg-pred! 'char-seq?       '{:clj #{CharSequence}})

; ===== VECTORS ===== ; Sequential, Associative (specifically, whose keys are sequential,
                      ; dense integer values), extensible

(reg-pred! '!array-list?    '{:clj  #{java.util.ArrayList
                                      java.util.Arrays$ArrayList} ; indexed and associative, but not extensible
                              :cljs #_cljs.core.ArrayList ; not used
                                    #{js/Array}}) ; because supports .push etc.
;; svec = "spliceable vector"
(reg-pred! 'svector?        '{:clj  #{clojure.core.rrb_vector.rrbt.Vector}
                              :cljs #{clojure.core.rrb_vector.rrbt.Vector}})
(reg-pred! '+vector?         {:clj  '#{clojure.lang.IPersistentVector}
                              :cljs (uset/union (:cljs (preds>types 'svector?))
                                                '#{cljs.core/PersistentVector})})
(reg-pred! '!+vector?       '{:clj  #{clojure.lang.ITransientVector}
                              :cljs #{cljs.core/TransientVector}})
(reg-pred! '?!+vector?       (preds>types '+vector? '!+vector?))
(reg-pred! '!vector|long?   '{:clj  #{it.unimi.dsi.fastutil.longs.LongArrayList}})
(reg-pred! '!vector|ref?    '{:clj  #{java.util.ArrayList}
                              :cljs #{js/Array}})  ; because supports .push etc.
(reg-pred! '!vector?         (preds>types '!vector|long? '!vector|ref?))
                             ;; java.util.Vector is deprecated, because you can
                             ;; just create a synchronized wrapper over an ArrayList
                             ;; via java.util.Collections
(reg-pred! '!!vector?        {})
(reg-pred! 'vector?          (preds>types '?!+vector? '!vector? '!!vector?))

; ===== QUEUES ===== ; Particularly FIFO queues, as LIFO = stack = any vector

(reg-pred!   '+queue?       '{:clj  #{clojure.lang.PersistentQueue}
                              :cljs #{cljs.core/PersistentQueue}})
(reg-pred!  '!+queue?        {})
(reg-pred! '?!+queue?        (preds>types '+queue? '!+queue?))
(reg-pred!   '!queue?       '{:clj  #{java.util.ArrayDeque} ; TODO *MANY* more here
                              :cljs #{goog.structs.Queue}})
(reg-pred!  '!!queue?        {}) ; TODO *MANY* more here
(reg-pred!    'queue?        {:clj  (uset/union (:clj (preds>types '?!+queue?))
                                                '#{java.util.Queue})
                              :cljs (:cljs (preds>types '?!+queue? '!queue? '!!queue?))})

; ===== GENERIC ===== ;

; ----- PRIMITIVES ----- ;

(reg-pred! 'primitive-unboxed? (preds>types 'unboxed-bool? 'unboxed-byte? 'unboxed-char?
                                 'unboxed-short? 'unboxed-int? 'unboxed-long?
                                 'unboxed-float? 'unboxed-double?))

(reg-pred! 'prim?              (preds>types 'primitive-unboxed?))

(reg-pred! 'prim-comparable?   (preds>types 'unboxed-byte? 'unboxed-char?
                                 'unboxed-short? 'unboxed-int? 'unboxed-long?
                                 'unboxed-float? 'unboxed-double?))

;; Possibly can't check for boxedness in Java because it does auto-(un)boxing, but it's nice to have
(reg-pred! 'primitive-boxed?   (preds>types 'boxed-bool? 'boxed-byte? 'boxed-char?
                                 'boxed-short? 'boxed-int? 'boxed-long?
                                 'boxed-float? 'boxed-double?))

(reg-pred! 'primitive?         (preds>types 'bool? 'byte? 'char?
                                 'short? 'int? 'long?
                                 'float? 'double?
                               #_{:cljs #{js/String}}))

;; Standard "uncuttable" types
(reg-pred! 'integral?          (preds>types 'bool? 'byte? 'char? 'number?))

; ----- COLLECTIONS ----- ;

                               ;; TODO this might be ambiguous
                               ;; TODO clojure.lang.Indexed / cljs.core/IIndexed?
(reg-pred! 'indexed?           (preds>types 'array? 'string? 'vector?
                                '{:clj #{clojure.lang.APersistentVector$RSeq}}))
                               ;; TODO this might be ambiguous
                               ;; TODO clojure.lang.Associative / cljs.core/IAssociative?
(reg-pred! 'associative?       (preds>types 'map? 'set? 'indexed?))
                               ;; TODO this might be ambiguous
                               ;; TODO clojure.lang.Sequential / cljs.core/ISequential?
(reg-pred! 'sequential?        (preds>types 'seq? 'list? 'indexed?))
                               ;; TODO this might be ambiguous
                               ;; TODO clojure.lang.ICollection / cljs.core/ICollection?
(reg-pred! 'counted?           (preds>types 'array? 'string?
                                 {:clj  (uset/union (:clj (preds>types '!vector? '!!vector?
                                                                       '!map?    '!!map?
                                                                       '!set?    '!!set?))
                                                    '#{clojure.lang.Counted})
                                  :cljs (:clj (preds>types 'vector? 'map? 'set?))}))

(reg-pred! 'coll?              (preds>types 'sequential? 'associative?))

(reg-pred! 'sorted?            {:clj  '#{clojure.lang.Sorted java.util.SortedMap java.util.SortedSet}
                                :cljs (:cljs (preds>types 'sorted-set? 'sorted-map?))}) ; TODO add in `cljs.core/ISorted

(reg-pred! 'transient?        '{:clj  #{clojure.lang.ITransientCollection}
                                :cljs #{cljs.core/TransientVector
                                        cljs.core/TransientHashSet
                                        cljs.core/TransientArrayMap
                                        cljs.core/TransientHashMap}})

;; Collections that have Transient counterparts
(reg-pred! 'transientizable?   (preds>types #_core-tuple?
                                '{:clj  #{clojure.lang.PersistentArrayMap
                                          clojure.lang.PersistentHashMap
                                          clojure.lang.PersistentHashSet
                                          clojure.lang.PersistentVector}
                                  :cljs #{cljs.core/PersistentArrayMap
                                          cljs.core/PersistentHashMap
                                          cljs.core/PersistentHashSet
                                          cljs.core/PersistentVector}}))

(reg-pred! 'editable?          {:clj  '#{clojure.lang.IEditableCollection}
                                :cljs #_#{cljs.core/IEditableCollection} ; can't dispatch on a protocol
                                      (:cljs (preds>types 'transientizable?))})

; ===== FUNCTIONS ===== ;

(reg-pred! 'fn?               '{:clj #{clojure.lang.Fn}  :cljs #{js/Function}})
(reg-pred! 'ifn?              '{:clj #{clojure.lang.IFn} :cljs #{js/Function}}) ; TODO keyword types?
(reg-pred! 'multimethod?      '{:clj #{clojure.lang.MultiFn}})

; ===== MISCELLANEOUS ===== ;

(reg-pred! 'regex?            '{:clj  #{java.util.regex.Pattern}
                                :cljs #{js/RegExp}})

(reg-pred! 'atom?             '{:clj  #{clojure.lang.IAtom}
                                :cljs #{cljs.core/Atom}})
(reg-pred! 'volatile?         '{:clj  #{clojure.lang.Volatile}
                                :cljs #{cljs.core/Volatile}})
(reg-pred! 'atomic?            {:clj  (uset/union (:clj (preds>types 'atom? 'volatile?))
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
                                        com.google.common.util.concurrent.AtomicDouble})})

(reg-pred! 'm2m-chan?         '{:clj  #{clojure.core.async.impl.channels.ManyToManyChannel}
                                :cljs #{cljs.core.async.impl.channels/ManyToManyChannel}})

(reg-pred! 'chan?             '{:clj  #{clojure.core.async.impl.protocols.Channel}
                                :cljs #{cljs.core.async.impl.channels/ManyToManyChannel
                                        #_"TODO more?"}})

(reg-pred! 'keyword?          '{:clj  #{clojure.lang.Keyword}
                                :cljs #{cljs.core/Keyword}})

(reg-pred! 'symbol?           '{:clj  #{clojure.lang.Symbol}
                                :cljs #{cljs.core/Symbol}})

(reg-pred! 'file?             '{:clj  #{java.io.File}
                                :cljs #{#_js/File}}) ; isn't always available! Use an abstraction

(reg-pred! 'any?               {:clj  (uset/union (:clj (preds>types 'prim?)) #{'java.lang.Object})
                                :cljs '#{(quote default)}})

(reg-pred! 'comparable?        {:clj  (uset/union '#{byte char short int long float double} '#{Comparable})
                                :cljs (:cljs (preds>types 'number?))})

(reg-pred! 'record?           '{:clj  #{clojure.lang.IRecord}
                              #_:cljs #_#{cljs.core/IRecord}}) ; because can't protocol-dispatch on protocols in CLJS

(reg-pred! 'transformer?      '{:clj #{#_clojure.core.protocols.CollReduce ; no, in order to find most specific type
                                       quantum.untyped.core.reducers.Transformer}
                                :cljs #{#_cljs.core/IReduce ; CLJS problems with dispatching on protocol
                                        quantum.untyped.core.reducers.Transformer}})

#_(reg-pred! 'reducible?       (preds>types
                                 'array?
                                 'string?
                                 'record?
                                 'reducer?
                                 'chan?
                                 {:cljs (:cljs (preds>types '+map?))}
                                 {:cljs (:cljs (preds>types '+set?))}
                                 'integer?
                                 {:clj  '#{clojure.lang.IReduce
                                           clojure.lang.IReduceInit
                                           clojure.lang.IKVReduce
                                           #_clojure.core.protocols.CollReduce} ; no, in order to find most specific type
                                  #_:cljs #_'#{cljs.core/IReduce}}  ; because can't protocol-dispatch on protocols in CLJS
                                  {:clj  '#{fast_zip.core.ZipperLocation}
                                  :cljs '#{fast-zip.core/ZipperLocation}}))

(reg-pred! 'booleans?       {:clj #{(-> array-1d-types*  :clj  :boolean)}})
(reg-pred! 'boolean-array?  (preds>types 'booleans?))
(reg-pred! 'bytes?          {:clj #{(-> array-1d-types*  :clj  :byte   )} :cljs #{(-> array-1d-types* :cljs :byte   )}})
(reg-pred! 'byte-array?     (preds>types 'bytes?))
(reg-pred! 'ubytes?         {                                             :cljs #{(-> array-1d-types* :cljs :ubyte  )}})
(reg-pred! 'ubyte-array?    (preds>types 'ubytes?))
(reg-pred! 'ubytes-clamped? {                                             :cljs #{(-> array-1d-types* :cljs :ubyte-clamped)}})
(reg-pred! 'ubyte-array-clamped? (preds>types 'ubytes-clamped?))
(reg-pred! 'chars?          {:clj #{(-> array-1d-types*  :clj  :char   )} :cljs #{(-> array-1d-types* :cljs :char   )}})
(reg-pred! 'char-array?     (preds>types 'chars?))
(reg-pred! 'shorts?         {:clj #{(-> array-1d-types*  :clj  :short  )} :cljs #{(-> array-1d-types* :cljs :short  )}})
(reg-pred! 'short-array?    (preds>types 'shorts?))
(reg-pred! 'ushorts?        {                                             :cljs #{(-> array-1d-types* :cljs :ushort )}})
(reg-pred! 'ushort-array?   (preds>types 'ushorts?))
(reg-pred! 'ints?           {:clj #{(-> array-1d-types*  :clj  :int    )} :cljs #{(-> array-1d-types* :cljs :int    )}})
(reg-pred! 'int-array?      (preds>types 'ints?))
(reg-pred! 'uints?          {                                             :cljs #{(-> array-1d-types* :cljs :uint  )}})
(reg-pred! 'uint-array?     (preds>types 'uints?))
(reg-pred! 'longs?          {:clj #{(-> array-1d-types*  :clj  :long   )} :cljs #{(-> array-1d-types* :cljs :long   )}})
(reg-pred! 'long-array?     (preds>types 'longs?))
(reg-pred! 'floats?         {:clj #{(-> array-1d-types*  :clj  :float  )} :cljs #{(-> array-1d-types* :cljs :float  )}})
(reg-pred! 'float-array?    (preds>types 'floats?))
(reg-pred! 'doubles?        {:clj #{(-> array-1d-types*  :clj  :double )} :cljs #{(-> array-1d-types* :cljs :double )}})
(reg-pred! 'double-array?   (preds>types 'doubles?))
(reg-pred! 'objects?        {:clj #{(-> array-1d-types*  :clj  :object )} :cljs #{(-> array-1d-types* :cljs :object )}})
(reg-pred! 'object-array?   (preds>types 'objects?))

(reg-pred! 'array-1d?       {:clj  (->> array-1d-types*  :clj  vals set)
                             :cljs (->> array-1d-types*  :cljs vals set)})


(reg-pred! 'numeric-1d?     (preds>types 'bytes? 'ubytes? 'ubytes-clamped?
                                         'chars?
                                         'shorts? 'ints? 'uints? 'longs?
                                         'floats? 'doubles?))

(reg-pred! 'booleans-2d?    {:clj #{(-> array-2d-types* :clj :boolean)} :cljs #{(-> array-2d-types* :cljs :boolean)}})
(reg-pred! 'bytes-2d?       {:clj #{(-> array-2d-types* :clj :byte   )} :cljs #{(-> array-2d-types* :cljs :byte   )}})
(reg-pred! 'chars-2d?       {:clj #{(-> array-2d-types* :clj :char   )} :cljs #{(-> array-2d-types* :cljs :char   )}})
(reg-pred! 'shorts-2d?      {:clj #{(-> array-2d-types* :clj :short  )} :cljs #{(-> array-2d-types* :cljs :short  )}})
(reg-pred! 'ints-2d?        {:clj #{(-> array-2d-types* :clj :int    )} :cljs #{(-> array-2d-types* :cljs :int    )}})
(reg-pred! 'longs-2d?       {:clj #{(-> array-2d-types* :clj :long   )} :cljs #{(-> array-2d-types* :cljs :long   )}})
(reg-pred! 'floats-2d?      {:clj #{(-> array-2d-types* :clj :float  )} :cljs #{(-> array-2d-types* :cljs :float  )}})
(reg-pred! 'doubles-2d?     {:clj #{(-> array-2d-types* :clj :double )} :cljs #{(-> array-2d-types* :cljs :double )}})
(reg-pred! 'objects-2d?     {:clj #{(-> array-2d-types* :clj :object )} :cljs #{(-> array-2d-types* :cljs :object )}})

(reg-pred! 'array-2d?       {:clj  (->> array-2d-types*  :clj  vals set)
                             :cljs (->> array-2d-types*  :cljs vals set)})

(reg-pred! 'numeric-2d?     (preds>types 'bytes-2d?
                                         'chars-2d?
                                         'shorts-2d?
                                         'ints-2d?
                                         'longs-2d?
                                         'floats-2d?
                                         'doubles-2d?))

(reg-pred! 'array-3d?       {:clj  (->> array-3d-types*  :clj  vals set)
                             :cljs (->> array-3d-types*  :cljs vals set)})
(reg-pred! 'array-4d?       {:clj  (->> array-4d-types*  :clj  vals set)
                             :cljs (->> array-4d-types*  :cljs vals set)})
(reg-pred! 'array-5d?       {:clj  (->> array-5d-types*  :clj  vals set)
                             :cljs (->> array-5d-types*  :cljs vals set)})
(reg-pred! 'array-6d?       {:clj  (->> array-6d-types*  :clj  vals set)
                             :cljs (->> array-6d-types*  :cljs vals set)})
(reg-pred! 'array-7d?       {:clj  (->> array-7d-types*  :clj  vals set)
                             :cljs (->> array-7d-types*  :cljs vals set)})
(reg-pred! 'array-8d?       {:clj  (->> array-8d-types*  :clj  vals set)
                             :cljs (->> array-8d-types*  :cljs vals set)})
(reg-pred! 'array-9d?       {:clj  (->> array-9d-types*  :clj  vals set)
                             :cljs (->> array-9d-types*  :cljs vals set)})
(reg-pred! 'array-10d?      {:clj  (->> array-10d-types* :clj  vals set)
                             :cljs (->> array-10d-types* :cljs vals set)})

(reg-pred! 'objects-nd?     {:clj  #{(-> array-1d-types*  :clj  :object )
                                     (-> array-2d-types*  :clj  :object )
                                     (-> array-3d-types*  :clj  :object )
                                     (-> array-4d-types*  :clj  :object )
                                     (-> array-5d-types*  :clj  :object )
                                     (-> array-6d-types*  :clj  :object )
                                     (-> array-7d-types*  :clj  :object )
                                     (-> array-8d-types*  :clj  :object )
                                     (-> array-9d-types*  :clj  :object )
                                     (-> array-10d-types* :clj  :object ) }
                             :cljs (:cljs (preds>types 'objects?))})

;; ===== Predicates ===== ;;

;; TODO this is just a temporary thing and breaks extensibility
(def types          @*types)
;; TODO this is just a temporary thing and breaks extensibility
(def types|unevaled @*types|unevaled)
