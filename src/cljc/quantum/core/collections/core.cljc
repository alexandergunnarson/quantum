 (ns
  ^{:doc "Retakes on core collections functions like first, rest,
          get, nth, last, index-of, etc.

          Also includes innovative functions like getr, etc."}
  quantum.core.collections.core
  (:refer-clojure :exclude
    [vector hash-map rest count first second butlast last aget get nth pop peek
     conj! conj assoc! dissoc! dissoc disj! contains? key val reverse
     empty? empty class reduce
     #?@(:cljs [array])])
  (:require [#?(:clj  clojure.core
                :cljs cljs.core   )         :as core    ]
    #?(:clj [seqspert.vector                            ])
    #?(:clj [clojure.core.async             :as casync  ])
            [quantum.core.log               :as log     ]
            [quantum.core.collections.base
              :refer        [#?(:clj kmap)]
              :refer-macros [kmap]]
            [quantum.core.convert.primitive :as pconvert
              :refer [->boolean
                      ->byte
              #?(:clj ->char)
                      ->short
                      ->int
                      ->long
              #?(:clj ->float)
                      ->double
            #?@(:clj [->byte*
                      ->char*
                      ->short*
                      ->int*
                      ->long*
                      ->float*
                      ->double*])]]
            [quantum.core.data.vector       :as vec
              :refer [catvec subvec+ vector+]]
            [quantum.core.error             :as err
              :refer [->ex TODO]]
            [quantum.core.fn                :as fn
              :refer        [#?@(:clj [fn1 rfn])]
              :refer-macros [          fn1 rfn]]
            [quantum.core.logic             :as logic
              :refer        [nnil? nempty?
                             #?@(:clj [eq? fn-eq? whenc whenf ifn1])]
              :refer-macros [          eq? fn-eq? whenc whenf ifn1]]
            [quantum.core.collections.logic
              :refer        [seq-or]]
            [quantum.core.macros            :as macros
              :refer        [#?@(:clj [defnt])]
              :refer-macros [          defnt]]
            [quantum.core.reducers          :as red
              :refer        [drop+ take+
                             #?@(:clj [dropr+ taker+ reduce])]
              :refer-macros [reduce]]
            [quantum.core.type              :as type
              :refer        [class
                             #?(:clj pattern?)]
              :refer-macros [        pattern?]]
            [quantum.core.vars              :as var
              :refer        [#?(:clj defalias)]
              :refer-macros [        defalias]])
 #?(:clj (:import quantum.core.data.Array
                  (java.util List)
                  clojure.core.async.impl.channels.ManyToManyChannel)))

; FastUtil is the best
; http://java-performance.info/hashmap-overview-jdk-fastutil-goldman-sachs-hppc-koloboke-trove-january-2015/

; TODO notify of changes to:
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Util.java
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Numbers.java
; TODO Queues need support

; TODO implement all these using wagjo/data-cljs
; slice [o start end] - like clojure.core/subvec
; slice-from [o start] - like slice, but until the end of o
; slice-to [o end] - like slice, but from the beginning of o
; split-at [o index] - clojure.core/split-at
; cat [o o2] - eager variant of clojure.core/concat
; splice [o index n val] - fast remove and insert in one go
; splice-arr [o index n val-arr] - fast remove and insert in one go
; insert-before [o index val] - insert one item inside coll
; insert-before-arr [o index val] - insert array of items inside coll
; remove-at [o index] - remove one itfem from index pos
; remove-n [o index n] - remove n items starting at index pos
; rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
; sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr

; mape-indexed [f o] - eager version of clojure.core/map-indexed
; reduce-reverse [f init o] - like reduce but in reverse order
; reduce2-reverse [f o] - like reduce but in reverse order
; reduce-kv-reverse [f init o] - like reduce-kv but in reverse order
; reduce2-kv-reverse [f o] - like reduce-kv but in reverse order

; TODO use set/map-invert instead of |reverse-keys|

; Arbitrary.
; TODO test this on every permutation for inflection point.
(def ^:const parallelism-threshold 10000)

; https://github.com/JulesGosnell/seqspert
; Very useful sequence and data structure info.

;___________________________________________________________________________________________________________________________________
;=================================================={        EQUIVALENCE       }=====================================================
;=================================================={       =, identical?      }=====================================================
; (defnt ^boolean identical?
;   [^Object k1, ^Object k2]
;   (clojure.lang.RT/identical k1 k2))

; static public boolean pcequiv(Object k1, Object k2){
;   if(k1 instanceof IPersistentCollection)
;     return ((IPersistentCollection)k1).equiv(k2);
;   return ((IPersistentCollection)k2).equiv(k1);
; }

; static public boolean equals(Object k1, Object k2){
;   if(k1 == k2)
;     return true;
;   return k1 != null && k1.equals(k2);
; }

; static public boolean equiv(Object k1, Object k2){
;   if(k1 == k2)
;     return true;
;   if(k1 != null)
;     {
;     if(k1 instanceof Number && k2 instanceof Number)
;       return Numbers.equal((Number)k1, (Number)k2);
;     else if(k1 instanceof IPersistentCollection || k2 instanceof IPersistentCollection)
;       return pcequiv(k1,k2);
;     return k1.equals(k2);
;     }
;   return false;
; }

; equivNull   : boolean equiv(Object k1, Object k2) return k2 == null
; equivEquals : boolean equiv(Object k1, Object k2) return k1.equals(k2)
; equivNumber : boolean equiv(Object k1, Object k2)
;             if(k2 instanceof Number)
;                 return Numbers.equal((Number) k1, (Number) k2);
;             return false

; equivColl : boolean equiv(Object k1, Object k2)
;             if(k1 instanceof IPersistentCollection || k2 instanceof IPersistentCollection)
;                 return pcequiv(k1, k2);
;             return k1.equals(k2);

; ; equivPred:
; ;     nil             : equivNull
; ;     Number          : equivNumber
; ;     String, Symbol  : equivEquals
; ;     Collection, Map : equivColl
; ;     :else           : equivEquals

; (defnt equiv ^boolean
;   ([^Object                a #{long double boolean} b] (clojure.lang.RT/equiv a b))
;   ([#{long double boolean} a ^Object                b] (clojure.lang.RT/equiv a b))
;   ([#{long double boolean} a #{long double boolean} b] (clojure.lang.RT/equiv a b))
;   ([^char                  a ^char                  b] (clojure.lang.RT/equiv a b))

;   )

;___________________________________________________________________________________________________________________________________
;=================================================={         RETRIEVAL        }=====================================================
;=================================================={     get, first, rest     }=====================================================
#_(defnt ^"Object[]" ->array
  {:source "clojure.lang.RT.toArray"}
  ([^"Object[]" x] x)
  ([^Collection x] (.toArray x))
  ([^Iterable   x]
    (let [ret (ArrayList. x)]
      (doseq [elem x]
        (.add ret elem))
      (.toArray ret)))
  ([^Map    x] (-> x (.entrySet) (.toArray)))
  ([^String x]

    (let [chars (-> x (.toCharArray))
          ret   (object-array (count chars))]

    ;  for(int i = 0 i < chars.length i++)
    ;   ret[i] = chars[i]
    ; return ret;
     ))
  ([^array? x]
    (let [s   (seq x)
          ret (object-array (count s))]
    ;   for(int i = 0; i < ret.length; i++, s = s.next())
    ;   ret[i] = s.first();
    ; return ret
    ))
  ([^Object x]
    (if (nil? x)
        clojure.lang.RT/EMPTY_ARRAY
        (throw (Util/runtimeException (str "Unable to convert: " (.getClass x) " to Object[]"))))))

(declare array)

(defnt count ; TODO incorporate clojure.lang.RT/count
  #?(:cljs (^long [^array?            x] (.-length x)))
  #?(:clj  (^long [^any-array?        x] (Array/count x)))
           (^long [^string?           x] (#?(:clj .length :cljs .-length) x))
           (^long [^keyword?          x] (count ^String (name x)))
         #?(:clj
           (^long [^ManyToManyChannel x] (count (.buf x))))
           (^long [^vector?           x] (#?(:clj .count :cljs core/count) x))
           (^long [                   x] (core/count x))
           (^long [^reducer?          x] (red/reduce-count x))
           ; Debatable whether this should be allowed
           (^long [:else              x] 0))

(defnt empty?
  ([#{array? string? keyword? #?(:clj ManyToManyChannel)} x] (zero? (count x)))
  ([        x] (core/empty? x)  ))

(defnt empty
  {:todo ["Most of this should be in some static map somewhere"]}
           ([^boolean?  x] false         )
  #?(:clj  ([^char?     x] (char   0)    ))
  #?(:clj  ([^byte?     x] (byte   0)    ))
  #?(:clj  ([^short?    x] (short  0)    ))
  #?(:clj  ([^int?      x] (int    0)    ))
  #?(:clj  ([^long?     x] (long   0)    ))
  #?(:clj  ([^float?    x] (short  0)    ))
  #?(:clj  ([^double?   x] (double 0)    ))
  #?(:cljs ([^num?      x] 0             ))
           ([^string?   x] ""            )
           ([           x] (core/empty x)))

(defnt #?(:clj  ^long lasti
          :cljs       lasti)
  "Last index of a coll."
  [coll] (unchecked-dec (count coll)))

#?(:clj
(defnt array-of-type
  (^first [^short-array?   obj ^pinteger? n] (short-array   n))
  (^first [^long-array?    obj ^pinteger? n] (long-array    n))
  (^first [^float-array?   obj ^pinteger? n] (float-array   n))
  (^first [^int-array?     obj ^pinteger? n] (int-array     n))
  (^first [^double-array?  obj ^pinteger? n] (double-array  n))
  (^first [^boolean-array? obj ^pinteger? n] (boolean-array n))
  (^first [^byte-array?    obj ^pinteger? n] (byte-array    n))
  (^first [^char-array?    obj ^pinteger? n] (char-array    n))
  (^first [^object-array?  obj ^pinteger? n] (object-array  n))))

(defnt ->array
  #?(:cljs ([x ct] (TODO)))
  #?(:clj (^boolean-array? [^boolean?        t ^pinteger? ct] (boolean-array ct)))
  #?(:clj (^byte-array?    [^byte?           t ^pinteger? ct] (byte-array    ct)))
  #?(:clj (^char-array?    [^char?           t ^pinteger? ct] (char-array    ct)))
  #?(:clj (^short-array?   [^short?          t ^pinteger? ct] (short-array   ct)))
  #?(:clj (^int-array?     [^int?            t ^pinteger? ct] (int-array     ct)))
  #?(:clj (^long-array?    [^long?           t ^pinteger? ct] (long-array    ct)))
  #?(:clj (^float-array?   [^float?          t ^pinteger? ct] (float-array   ct)))
  #?(:clj (^double-array?  [^double?         t ^pinteger? ct] (double-array  ct)))
  #?(:clj (                [^java.lang.Class c ^pinteger? ct] (make-array c  ct)))) ; object-array is subsumed into this

(defnt getr
  {:todo "Differentiate between |subseq| and |slice|"}
  ; inclusive range
          ([^string?     coll ^pinteger? a ^pinteger? b] (.substring coll a (inc b)))
          ([^reducer?    coll ^pinteger? a ^pinteger? b] (->> coll (take+ b) (drop+ a)))
  #?(:clj ([^array-list? coll ^pinteger? a ^pinteger? b] (.subList coll a b)))
          ([^vec?        coll ^pinteger? a ^pinteger? b] (subvec+ coll a (inc b)))
          ([^vec?        coll ^pinteger? a             ] (subvec+ coll a (-> coll count)))
  ; TODO slice for CLJS arrays
  #?(:clj (^first [^array?      coll ^pinteger? a ^pinteger? b]
            (let [arr-f (array-of-type coll (core/long (inc (- b a))))] ; TODO make long cast unnecessary
              (System/arraycopy coll a arr-f 0
                (inc (- b a)))
              arr-f)))
          ([^:obj        coll ^pinteger? a             ] (->> coll (drop a)))
          ([^:obj        coll ^pinteger? a ^pinteger? b] (->> coll (take b) (drop a))))

(defnt rest
  "Eager rest."
  ([^keyword? k]    (-> k name core/rest))
  ([^symbol?  s]    (-> s name core/rest))
  ([^reducer? coll] (drop+ 1 coll))
  ([^string?  coll] (getr coll 1 (lasti coll)))
  ([^vec?     coll] (getr coll 1 (lasti coll)))
  ([^array?   coll] (getr coll 1 (core/long (lasti coll)))) ; TODO use macro |long|
  ([          coll] (core/rest coll)))

#?(:clj (defalias popl rest))

(def neg-1? (eq? -1))

(defnt index-of
  {:todo ["Reflection on short, bigint"
          "Add 3-arity for |index-of-from|"]}
  ([^vec?    coll elem] (whenc (.indexOf coll elem) neg-1? nil))
  ([^string? coll elem]
    (cond (string? elem)
          (whenc (.indexOf coll (str elem)) neg-1? nil)
          (pattern? elem)
          #?(:clj  (let [^java.util.regex.Matcher matcher
                          (re-matcher (re-pattern elem) coll)]
                     (when (.find matcher)
                       (.start matcher)))
             :cljs (throw (->ex :unimplemented
                                (str "|index-of| not implemented for " (class coll) " on " (class elem))
                                (kmap coll elem))))))
  #_([coll elem] (throw (->ex :unimplemented
                            (str "|index-of| not implemented for " (class coll) " on " (class elem))
                            (kmap coll elem)))))

; Spent too much time on this...
; (defn nth-index-of [super sub n]
;   (reducei
;     (fn [[sub-matched i-found indices-found :as state] elem i]
;       (let-alias [sub-match?      (= elem (get super i))
;                   match-complete? (= (inc sub-matched) (count sub))
;                   nth-index?      (= (inc indices-found) n)]
;         (if sub-match?
;             (let [[sub-matched-n+1 i-found-n+1 indices-found-n+1 :as state]
;                    []])
;             (if match-complete?
;                   (if nth-index?
;                       i-found))
;             (if (= n (lasti super))
;                 nil
;                 state))))
;     [0 0 nil]
;     super))

(defnt last-index-of
  {:todo ["Reflection on short, bigint"]}
  ([^vec?    coll elem] (whenc (.lastIndexOf coll elem      ) neg-1? nil))
  ([^string? coll elem] (whenc (.lastIndexOf coll (str elem)) neg-1? nil))
  #_([coll elem] (throw (->ex :unimplemented
                            (str "|last-index-of| not implemented for " (class coll) " on " (class elem))
                            (kmap coll elem)))))

(defnt containsk?
  {:imported "clojure.lang.RT.contains"}
           ([#{string? array?}                            coll ^pinteger? n] (and (>= n 0) (<  (count coll))))
  #?(:clj  ([#{clojure.lang.Associative    java.util.Map} coll            k] (.containsKey   coll k)))
  #?(:clj  ([#{clojure.lang.IPersistentSet java.util.Set} coll            k] (.contains      coll k)))
  #?(:cljs ([#{set? map?}                                 coll            k] (core/contains? coll k))) ; TODO find out how to make faster
           ([^:obj                                        coll            k]
             (if (nil? coll)
                 false
                 (throw (->ex :not-supported
                          (str "contains? not supported on type: " (-> coll class)))))))

#?(:clj (defalias contains? containsk?))

(defnt containsv?
  ([^string?  coll elem]
    (and (nnil? elem) (index-of coll elem)))
  ([^pattern? coll elem]
    (nnil? (re-find elem coll)))
  ([          coll elem]
    (seq-or (fn-eq? elem) coll)))

; static Object getFrom(Object coll, Object key, Object notFound){
;   else if(coll instanceof Map) {
;     Map m = (Map) coll;
;     if(m.containsKey(key))
;       return m.get(key);
;     return notFound;
;   }
;   else if(coll instanceof IPersistentSet) {
;     IPersistentSet set = (IPersistentSet) coll;
;     if(set.contains(key))
;       return set.get(key);
;     return notFound;
;   }
;   else if(key instanceof Number && (coll instanceof String || coll.getClass().isArray())) {
;     int n = ((Number) key).intValue();
;     return n >= 0 && n < count(coll) ? nth(coll, n) : notFound;
;   }
;   return notFound;

; }

(defnt aget  ;  (java.lang.reflect.Array/get coll n) is about 4 times faster than core/get
  "Basically this is the whole quantum/java Array file.
   Takes only 1-2 seconds to generate and compile this."
          ([^array? x #?(:clj #{pinteger?}) i1]
            (#?(:clj  Array/get
                :cljs core/aget) x i1))
  #?(:clj ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
            ^int i1]
            (Array/get x i1))))

#?(:clj ; TODO macro to de-repetitivize
(defnt aget-in*
  ([#{array? array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1]
    (Array/get x i1))
  ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2]
    (Array/get x i1 i2))
  ([#{array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3]
    (Array/get x i1 i2 i3))
  ([#{array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4]
    (Array/get x i1 i2 i3 i4))
  ([#{array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5]
    (Array/get x i1 i2 i3 i4 i5))
  ([#{array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6]
    (Array/get x i1 i2 i3 i4 i5 i6))
  ([#{array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7]
    (Array/get x i1 i2 i3 i4 i5 i6 i7))
  ([#{array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8]
    (Array/get x i1 i2 i3 i4 i5 i6 i7 i8))
  ([#{array-9d? array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8 ^int i9]
    (Array/get x i1 i2 i3 i4 i5 i6 i7 i8 i9))
  ([#{array-10d?} x
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8 ^int i9 ^int i10]
    (Array/get x i1 i2 i3 i4 i5 i6 i7 i8 i9 i10))))

#?(:cljs (defn aget-in*-protocol [arr & ks] (TODO)))

(defnt get
  {:imported "clojure.lang.RT/get"}
  #?(:clj  ([^clojure.lang.ILookup coll            k             ] (.valAt coll k)))
  #?(:clj  ([^clojure.lang.ILookup coll            k if-not-found] (.valAt coll k if-not-found)))
  #?(:clj  ([#{java.util.Map clojure.lang.IPersistentSet}
                                   coll            k             ] (.get coll k)))
           ([^string?              coll ^pinteger? n             ] (.charAt  coll n             ))
  #?(:clj  ([^array-list?          coll ^pinteger? n             ] (get      coll n nil         )))
  #?(:clj  ([^array-list?          coll ^pinteger? n if-not-found]
             (try (.get coll n)
               (catch ArrayIndexOutOfBoundsException e# if-not-found))))
           ([^array?               coll ^pinteger? n             ] (aget     coll n             ))
           ([^listy?               coll            n             ] (core/nth coll n nil         ))
           ([^listy?               coll            n if-not-found] (core/nth coll n if-not-found))
           ; TODO look at clojure.lang.RT/get for how to handle these edge cases efficiently
  #?(:cljs ([^nil?                 coll            n             ] (core/get coll n nil         )))
  #?(:cljs ([^nil?                 coll            n if-not-found] (core/get coll n if-not-found)))
           ([                      coll            n             ] (core/get coll n nil         ))
           ([                      coll            n if-not-found] (core/get coll n if-not-found)))

(defnt nth
  ; TODO import clojure.lang.RT/nth
  ([#{vector? string? array-list? #_array? ; for now, because java.lang.VerifyError: reify method: Nth signature: ([Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;) Incompatible object argument for function call
      listy?} coll i] (get coll i))
  ([^reducer? coll i]
    (let [i' (volatile! 0)]
      (reduce (rfn [ret x] (if (= @i' i)
                                (reduced x)
                                (do (vswap! i' inc)
                                    ret)))
        nil coll)))
  ([coll i] (core/nth coll i))
  #_([#{clojure.data.avl.AVLSet
      clojure.data.avl.AVLMap
      java.util.Map
      clojure.lang.IPersistentSet} coll i] (core/nth coll i)))

#_(defnt aget-in ; TODO construct using a macro
  "Haven't fixed reflection issues for unused code paths. Also not performant."
  ([#{array? array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    indices]
   (condp = (count indices)
     1  (aget-in* x (int (get indices 0))                )
     2  (aget-in* x (int (get indices 0)) (int (get indices 1)))
     3  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)))
     4  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)))
     5  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)))
     6  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)))
     7  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)))
     8  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)) (int (get indices 7)))
     9  (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)) (int (get indices 7)) (int (get indices 8)))
     10 (aget-in* x (int (get indices 0)) (int (get indices 1)) (int (get indices 2)) (int (get indices 3)) (int (get indices 4)) (int (get indices 5)) (int (get indices 6)) (int (get indices 7)) (int (get indices 8)) (int (get indices 9)))
     0 (throw (->ex "Indices can't be empty"))
     :else (throw (->ex "Indices count can't be >10")))))

(defalias doto! swap!)

(defnt aset!
  "Yay, |aset| no longer causes reflection or needs type hints!"
  {:performance"|java.lang.reflect.Array/set| is 26 times faster
                 than 'normal' reflection"}
  #?(:cljs (^first [^array?         coll            i          v] (aset coll i v             ) coll))
  #?(:clj  (^first [^boolean-array? coll ^pinteger? i ^boolean v] (aset coll i v             ) coll))
  #?(:clj  (^first [^byte-array?    coll ^pinteger? i ^byte    v] (aset coll i (core/byte  v)) coll)) ; TODO make this not required
  #?(:clj  (^first [^char-array?    coll ^pinteger? i ^char    v] (aset coll i v)              coll))
  #?(:clj  (^first [^short-array?   coll ^pinteger? i ^short   v] (aset coll i (core/short v)) coll)) ; TODO make this not required
  #?(:clj  (^first [^int-array?     coll ^pinteger? i ^int     v] (aset coll i v             ) coll))
  #?(:clj  (^first [^long-array?    coll ^pinteger? i ^long    v] (aset coll i v             ) coll))
  #?(:clj  (^first [^float-array?   coll ^pinteger? i ^float   v] (aset coll i v             ) coll))
  #?(:clj  (^first [^double-array?  coll ^pinteger? i ^double  v] (aset coll i v             ) coll))
  #?(:clj  (^first [^object-array?  coll ^pinteger? i          v] (aset coll i v             ) coll))
  #?(:clj  (^first [                coll ^pinteger? i          v] (java.lang.reflect.Array/set coll i v) coll)))

; TODO
; (defnt aset-in!)

; TODO assoc-in and assoc-in! for files
(defnt assoc!
  {:todo ["Remove reflection for |aset!|."]}
  #?(:cljs (^first [^array?         coll            k          v] (aset coll k v             )))
  #?(:clj  (^first [^boolean-array? coll ^pinteger? k ^boolean v] (aset coll k v             )))
  #?(:clj  (^first [^byte-array?    coll ^pinteger? k ^byte    v] (aset coll k (core/byte  v)))) ; TODO make this not required
  #?(:clj  (^first [^char-array?    coll ^pinteger? k ^char    v] (aset coll k v             )))
  #?(:clj  (^first [^short-array?   coll ^pinteger? k ^short   v] (aset coll k (core/short v)))) ; TODO make this not required
  #?(:clj  (^first [^int-array?     coll ^pinteger? k ^int     v] (aset coll k v             )))
  #?(:clj  (^first [^long-array?    coll ^pinteger? k ^long    v] (aset coll k v             )))
  #?(:clj  (^first [^float-array?   coll ^pinteger? k ^float   v] (aset coll k v             )))
  #?(:clj  (^first [^double-array?  coll ^pinteger? k ^double  v] (aset coll k v             )))
  #?(:clj  (^first [^object-array?  coll ^pinteger? k          v] (aset coll k v             )))
           (^first [^transient?     coll            k          v] (core/assoc! coll k v))
           (       [^atom?          coll            k          v] (swap! coll assoc k v)))

(defnt assoc!*
  #?(:cljs (^first [^array?         coll            k          v] (assoc! coll k v)))
  #?(:clj  (^first [^boolean-array? coll ^pinteger? k ^boolean v] (assoc! coll k v)))
  #?(:clj  (^first [^byte-array?    coll ^pinteger? k ^byte    v] (assoc! coll k v)))
  #?(:clj  (^first [^char-array?    coll ^pinteger? k ^char    v] (assoc! coll k v)))
  #?(:clj  (^first [^short-array?   coll ^pinteger? k ^short   v] (assoc! coll k v)))
  #?(:clj  (^first [^int-array?     coll ^pinteger? k ^int     v] (assoc! coll k v)))
  #?(:clj  (^first [^long-array?    coll ^pinteger? k ^long    v] (assoc! coll k v)))
  #?(:clj  (^first [^float-array?   coll ^pinteger? k ^float   v] (assoc! coll k v)))
  #?(:clj  (^first [^double-array?  coll ^pinteger? k ^double  v] (assoc! coll k v)))
  #?(:clj  (^first [^object-array?  coll ^pinteger? k          v] (assoc! coll k v)))
           (^first [^transient?     coll            k          v] (assoc! coll k v))
           (       [^atom?          coll            k          v] (assoc! coll k v))
           (       [                coll            k          v] (core/assoc coll k v)))

(defnt dissoc
  {:imported "clojure.lang.RT/dissoc"}
  #?(:clj  ([^clojure.lang.IPersistentMap coll k] (.without coll k)))
  #?(:cljs ([^map?                        coll k] (core/dissoc coll k)))
           ([coll k]
             (if (nil? coll)
                 nil
                 (throw (->ex :not-supported (str "|dissoc| not supported on" (class coll)))))))

(defnt dissoc!
  ([^transient? coll k  ] (core/dissoc! coll k))
  ([^atom?      coll k  ] (swap! coll (fn [m k-n] (dissoc m k-n)) k)))

(defnt conj!
  ([^transient? coll obj] (core/conj! coll obj))
  ([^atom?      coll obj] (swap! coll core/conj obj)))

(defnt disj!
  ([^transient? coll obj] (core/disj! coll obj))
  ([^atom?      coll obj] (swap! coll disj obj)))

#?(:clj
(defmacro update! [coll i f]
  `(assoc! ~coll ~i (~f (get ~coll ~i)))))

(defnt first
  ([#{string? #?(:clj array-list?) array?} coll] (get coll 0))
  ([^vec?                                  coll] (get coll #?(:clj (Long. 0) :cljs 0))) ; to cast it...
  ; TODO is this wise?
  ([^integral?                             coll] coll)
  ([^reducer?                              coll] (reduce (rfn [_ x] (reduced x)) nil coll))
  ([:else                                  coll] (core/first coll)))

(defalias firstl first) ; TODO not always true

(defnt second
  ([#{string? #?(:clj array-list?)} coll] (get coll 1))
  ; 2.8  nanos to (.cast Long _)
  ; 1.26 nanos to (Long. _)
  ([^vec?                           coll] (get coll #?(:clj (Long. 1) :cljs 1))) ; to cast it...
  ([^reducer?                       coll] (nth coll 1))
  ([:else                           coll] (core/second coll)))

(defnt butlast
  {:todo ["Add support for arrays"
          "Add support for CLJS IPersistentStack"]}
          ([^string?                       coll] (getr coll 0 (-> coll lasti dec)))
  #?(:clj ([^reducer?                      coll] (dropr+ 1 coll)))
  #?(:clj ([^clojure.lang.IPersistentStack coll] (.pop coll)))
          ([^vec?                          coll] (whenf coll nempty? core/pop))
  #?(:clj ([^clojure.lang.IPersistentList  coll] (core/butlast coll)))
          ([:else                          coll] (core/butlast coll)))

(defalias pop  butlast)
(defalias popr butlast)

(defnt last
          ([^string?          coll] (get coll (lasti coll)))
  #?(:clj ([^reducer?         coll] (taker+ 1 coll)))
          ; TODO reference to field peek on clojure.lang.APersistentVector$RSeq can't be resolved.
          ([^vec?             coll] (#?(:clj .peek :cljs .-peek) coll)) ; because |peek| works on lists too
  #?(:clj ([#{#?@(:clj  [array-list? clojure.lang.PersistentVector$TransientVector]
                  :cljs [cljs.core/TransientVector])} coll]
            (get coll (lasti coll))))
          ([:else             coll] (core/last coll)))

(defalias peek last)
(defalias firstr last)

#?(:clj  (defn array
           {:todo ["Consider efficiency here"]}
           [& args] (into-array (-> args first class) args))
   :cljs (defalias array core/array))


(defn gets [coll indices]
  (->> indices (red/map+ #(get coll %)) (red/join [])))

(def third (fn1 get 2))

(defn getf [n] (fn1 get n))

;--------------------------------------------------{           CONJL          }-----------------------------------------------------
; This will take AGES to compile if you try to allow primitives
(defnt conjl
  ([^listy? coll a          ] (->> coll (cons a)                                             ))
  ([^listy? coll a b        ] (->> coll (cons b) (cons a)                                    ))
  ([^listy? coll a b c      ] (->> coll (cons c) (cons b) (cons a)                           ))
  ([^listy? coll a b c d    ] (->> coll (cons d) (cons c) (cons b) (cons a)                  ))
  ([^listy? coll a b c d e  ] (->> coll (cons e) (cons d) (cons c) (cons b) (cons a)         ))
  ([^listy? coll a b c d e f] (->> coll (cons f) (cons e) (cons d) (cons c) (cons b) (cons a)))
  ([^vec?   coll a          ] (catvec (vector+ a          ) coll))
  ([^vec?   coll a b        ] (catvec (vector+ a b        ) coll))
  ([^vec?   coll a b c      ] (catvec (vector+ a b c      ) coll))
  ([^vec?   coll a b c d    ] (catvec (vector+ a b c d    ) coll))
  ([^vec?   coll a b c d e  ] (catvec (vector+ a b c d e  ) coll))
  ([^vec?   coll a b c d e f] (catvec (vector+ a b c d e f) coll))
  ([^vec?   coll a b c d e f & more]
    (reduce (fn [ret elem] (conjl ret elem)) ; should just be |conjl|
      (vector+ a b c d e f) more)))

; TODO to finish from RT
; (defnt conj
;   ([^IPersistentCollection coll ^Object x]
;     (if (nil? coll)
;         (PersistentList. x)
;         (.cons coll x))))

(defalias conj core/conj)

(defnt conjr
  ([^vec?   coll a    ] (core/conj a    ))
  ([^vec?   coll a b  ] (core/conj a b  ))
  ([^vec?   coll a b c] (core/conj a b c))
  ;([coll a & args] (apply conj a args))
  ([^listy? coll a    ] (concat coll (list a    )))
  ([^listy? coll a b  ] (concat coll (list a b  )))
  ([^listy? coll a b c] (concat coll (list a b c)))
  ;([coll a & args] (concat coll (cons arg args)))
  )

#?(:clj (defnt ^clojure.lang.PersistentVector ->vec
          "513.214568 msecs (vec a1)
           182.745605 msecs (seqspert.vector/array-to-vector a1)"
          ([^array? x] (if (> (count x) parallelism-threshold)
                           (seqspert.vector/array-to-vector x)
                           (vec x))))
   :cljs (defalias ->vec core/vec))

#?(:clj
(defnt ^"[Ljava.lang.Object;" ->arr
  "513.214568 msecs (vec a1)
   182.745605 msecs (seqspert.vector/array-to-vector a1)"
  ([^vector? x] (if (> (count x) parallelism-threshold)
                    (seqspert.vector/vector-to-array x)
                    (into-array Object x)))))

; VECTORS
; 166.884981 msecs (mapv identity v1)
; 106.545886 msecs (seqspert.vector/vmap   identity v1)))
; 22.778568  msecs (seqspert.vector/fjvmap identity v1)

(defn- handle-kv
  [kv f]
  (if (-> kv count (= 2))
      (f)
      (throw (->ex nil "`key/val` not supported on collections of count != 2"
                   {:coll kv :ct (count kv)}))))

(defnt key*
  #?@(:clj  [([^map-entry? kv] (core/key kv))
             ([^List       kv] (handle-kv kv #(first kv)))]
      :cljs [([#{vec? array?} kv] (handle-kv kv #(first kv)))]))

(defnt val*
  #?@(:clj  [([^map-entry? kv] (core/val kv))
             ([^List       kv] (handle-kv kv #(second kv)))]
      :cljs [([#{vec? array?} kv] (handle-kv kv #(second kv)))]))

(defn key ([kv] (when kv (key* kv))) ([k v] k))
(defn val ([kv] (when kv (val* kv))) ([k v] v))

; what about arrays? some transient loop or something
(def reverse (ifn1 reversible? rseq core/reverse))
