 (ns
  ^{:doc "Retakes on core collections functions like `first`, `rest`,
          `get`, `nth`, `last`, `index-of`, `slice` etc."}
  quantum.core.collections.core
  (:refer-clojure :exclude
    [vector hash-map rest count first second butlast last aget get nth pop peek
     conj! conj assoc assoc! dissoc dissoc! disj! contains? key val reverse subseq
     empty? empty class reduce swap! reset!
     #?@(:cljs [array])])
  (:require [clojure.core                   :as core
             #?@(:cljs [:refer IEmptyableCollection])]
    #?(:clj [seqspert.vector                            ])
    #?(:clj [clojure.core.async             :as casync])
            [quantum.core.log               :as log]
            [quantum.core.collections.base
              :refer [kmap nempty? nnil?]]
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
              :refer [catvec svector subsvec]]
            [quantum.core.error             :as err
              :refer [->ex TODO]]
            [quantum.core.fn                :as fn
              :refer [fn1 fn&2 rfn rcomp firsta]]
            [quantum.core.logic             :as logic
              :refer [fn= whenc whenf ifn1]]
            [quantum.core.collections.logic
              :refer [seq-or]]
            [quantum.core.macros            :as macros
              :refer [defnt #?(:clj defnt') case-env]]
            [quantum.core.macros.optimization
              :refer [identity*]]
            [quantum.core.loops
              :refer [reducei]]
            [quantum.core.reducers.reduce   :as red
              :refer [reduce reducer]]
            [quantum.core.type              :as t
              :refer [class pattern?]]
            [quantum.core.type.defs         :as tdef]
            [quantum.core.type.core         :as tcore]
            [quantum.core.vars              :as var
              :refer [defalias #?(:clj defmalias) def-]])
  (:require-macros
    [quantum.core.collections.core
      :refer [assoc gen-typed-array-defnts
              ->boolean-array
              #_->boolean-array-cljs
              ->byte-array
              #_->byte-array-cljs
              ->ubyte-array
              #_->ubyte-array-cljs
              ->char-array
              #_->char-array-cljs
              ->short-array
              #_->short-array-cljs
              ->ushort-array
              #_->ushort-array-cljs
              ->int-array
              #_->int-array-cljs
              ->uint-array
              #_->uint-array-cljs
              ->float-array
              #_->float-array-cljs
              ->double-array
              #_->double-array-cljs
              ->object-array]])
 #?(:clj  (:import
            quantum.core.data.Array
            [clojure.lang IAtom Counted IPersistentCollection]
            [java.util List Collection Map Map$Entry]
            [java.util.concurrent.atomic AtomicReference AtomicBoolean AtomicInteger AtomicLong])
    :cljs (:import
            goog.string.StringBuffer)))

 (log/this-ns)

; FastUtil is the best
; http://java-performance.info/hashmap-overview-jdk-fastutil-goldman-sachs-hppc-koloboke-trove-january-2015/

; TODO notify of changes to:
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/RT.java
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Util.java
; https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/Numbers.java
; TODO Queues need support

; TODO implement all these using wagjo/data-cljs
; split-at [o index] - clojure.core/split-at
; splice [o index n val] - fast remove and insert in one go
; splice-arr [o index n val-arr] - fast remove and insert in one go
; insert-before [o index val] - insert one item inside coll
; insert-before-arr [o index val] - insert array of items inside coll
; remove-at [o index] - remove one itfem from index pos
; remove-n [o index n] - remove n items starting at index pos
; rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
; sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr

; Arbitrary.
; TODO test this on every permutation for inflection point.
(def- parallelism-threshold 10000)

; https://github.com/JulesGosnell/seqspert
; Very useful sequence and data structure info.

(defn #_defcurried drop+
  [n coll] (reducer coll (core/drop n)))

#?(:clj
(defn dropr+ ; This is extremely slow by comparison. About twice as slow
  {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
  [n coll]
   (reducer coll
     (fn [f1]
       (let [buffer (java.util.ArrayDeque. (int n))]
         (fn self
           ([] (f1))
           ([ret x]
             (let [ret (if (= (.size buffer) n) ; because Java object
                         (f1 ret (.pop buffer))
                         ret)]
               (.add buffer x)
               ret))))))))

(defn #_defcurried take+
  [n coll] (reducer coll (core/take n)))


#?(:clj
(defn taker+
  {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
  [n coll]
   (reify
     clojure.core.protocols.CollReduce
     ;#+cljs cljs.core/IReduce
     (coll-reduce [this f1]
       (clojure.core.protocols/coll-reduce this f1 (f1)))
     (coll-reduce [_ f1 init]
       (clojure.core.protocols/coll-reduce
         (clojure.core.protocols/coll-reduce
           coll
           (fn [^java.util.Deque q x]
             (when (= (.size q) n)
               (.pop q))
             (.add q x)
             q)
           (java.util.ArrayDeque. (int n)))
         f1 init)))))
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
  ([^array-1d? x]
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

(defn reduce-count
  {:attribution "parkour.reducers"
   :performance "On non-counted collections, `count` is 71.542581 ms, whereas
                 `reduce-count` is 36.824665 ms - twice as fast"}
  [coll]
  (reduce (rcomp firsta inc) 0 coll))

(defnt ^long count
  "Incorporated `clojure.lang.RT/count` and `clojure.lang.RT/countFrom`"
  {:todo #{"handle persistent maps"}}
           ([^array?     x] (#?(:clj Array/count :cljs .-length) x))
           ([^tuple?     x] (count (.-vs x)))
  #?(:cljs ([^string?    x] (.-length   x)))
  #?(:cljs ([^!string?   x] (.getLength x)))
  #?(:clj  ([^char-seq?  x] (.length x)))
           ([^keyword?   x] (count ^String (name x)))
           ([^m2m-chan?  x] (count (#?(:clj .buf :cljs .-buf) x)))
           ([^+vec?      x] (#?(:clj .count :cljs core/count) x))
  #?(:clj  ([#{Collection Map} x] (.size x)))
  #?(:clj  ([^Counted    x] (.count x)))
  #?(:clj  ([^Map$Entry  x] (if (nil? x) 0 2))) ; TODO fix this potential null issue
           ([            x] (core/count x))
           ([^reducer?   x] (reduce-count x)))

(defnt empty?
          ([#{array? ; TODO anything that `count` accepts
              string? !string? keyword? m2m-chan?
              +vec? tuple?} x] (zero? (count x)))
  #?(:clj ([#{Collection Map} x] (.isEmpty x)))
          ([            x] (core/empty? x)  ))

; ===== ARRAYS ===== ;

#?(:clj
(defmacro gen-typed-array-defnts []
  (case-env
    :clj  `(do ~@(for [k (-> tdef/array-1d-types :clj (core/dissoc :object) keys)]
              (let [fn-sym (symbol (str "->" (name k) "-array-clj"))]
                `(defmacro ~fn-sym [& args#] (with-meta `(~~(symbol "core" (str (name k) "-array")) ~@args#) {:tag ~(str (name k) "s")})))))
    :cljs `(do ~@(for [[k type-sym] (-> tdef/array-1d-types :cljs (core/dissoc :object))]
             (let [fn-sym (symbol (str "->" (name k) "-array-cljs"))]
               `(defnt ~fn-sym
                  ([#{~type-sym} x#] x#)
                  ([~(into (core/get tcore/cljs-typed-array-convertible-classes type-sym)
                           '#{objects? number?}) x#] (new ~type-sym x#))
                  ([x#] (assert (coll? x#)) ; TODO maybe other acceptable datatypes? Reducibles?
                        ; TODO compare `reducei` to `doseqi`
                        (reducei (fn [buf# elem# i#] (core/aset buf# i# elem#) buf#)
                                 (~fn-sym (count x#))
                                 x#)))))))))

(gen-typed-array-defnts)

#?(:clj (defmalias ->boolean-array     quantum.core.collections.core/->boolean-array-clj quantum.core.collections.core/->boolean-array-cljs))
#?(:clj (defalias  ->booleans ->boolean-array))
#?(:clj (defmalias ->byte-array        quantum.core.collections.core/->byte-array-clj    quantum.core.collections.core/->byte-array-cljs   ))
#?(:clj (defalias  ->bytes    ->byte-array))
#?(:clj (defmalias ->ubyte-array       nil                                               quantum.core.collections.core/->ubyte-array-cljs  ))
#?(:clj (defalias  ->ubytes   ->ubyte-array))
#?(:clj (defmalias ->char-array        quantum.core.collections.core/->char-array-clj    nil))
#?(:clj (defalias  ->chars    ->char-array))
#?(:clj (defmalias ->ushort-array      nil                                               quantum.core.collections.core/->ushort-array-cljs ))
#?(:clj (defalias  ->ushorts  ->ushort-array))
#?(:clj (defmalias ->short-array       quantum.core.collections.core/->short-array-clj   quantum.core.collections.core/->short-array-cljs  ))
#?(:clj (defalias  ->shorts   ->short-array))
#?(:clj (defmalias ->int-array         quantum.core.collections.core/->int-array-clj     quantum.core.collections.core/->int-array-cljs    ))
#?(:clj (defalias  ->ints     ->int-array))
#?(:clj (defmalias ->uint-array        nil                                               quantum.core.collections.core/->uint-array-cljs   ))
#?(:clj (defalias  ->uints    ->uint-array))
#?(:clj (defmalias ->long-array        quantum.core.collections.core/->long-array-clj    nil))
#?(:clj (defalias  ->longs    ->long-array))
#?(:clj (defmalias ->float-array       quantum.core.collections.core/->float-array-clj   quantum.core.collections.core/->float-array-cljs  ))
#?(:clj (defalias  ->floats   ->float-array))
#?(:clj (defmalias ->double-array      quantum.core.collections.core/->double-array-clj  quantum.core.collections.core/->double-array-cljs ))
#?(:clj (defalias  ->doubles  ->double-array))
        (defalias  ->object-array   object-array)
#?(:clj (alter-meta! #'->object-array core/assoc :tag "[Ljava.lang.Object;"))
        (defalias  ->objects      ->object-array)

(defnt array-of-type ; TODO get this from `Arrays`
  #?@(:clj  [(^<0> [^array?    x ^int n] (Array/arrayOfType x n))]
      :cljs [(^<0> [^bytes?    x ^int n] (->byte-array   n))
             (^<0> [^ubytes?   x ^int n] (->ubyte-array  n))
             (^<0> [^shorts?   x ^int n] (->short-array  n))
             (^<0> [^ushorts?  x ^int n] (->ushort-array n))
             (^<0> [^ints?     x ^int n] (->int-array    n))
             (^<0> [^uints?    x ^int n] (->uint-array   n))
             (^<0> [^floats?   x ^int n] (->float-array  n))
             (^<0> [^doubles?  x ^int n] (->double-array n))
             (^<0> [^objects?  x ^int n] (->object-array n))]))

(defnt ->array
  #?(:clj  (^boolean-array? [^boolean? t ^nat-long? ct] (->boolean-array ct)))
  #?(:clj  (^byte-array?    [^byte?    t ^nat-long? ct] (->byte-array    ct)))
  #?(:clj  (^char-array?    [^char?    t ^nat-long? ct] (->char-array    ct)))
  #?(:clj  (^short-array?   [^short?   t ^nat-long? ct] (->short-array   ct)))
  #?(:clj  (^int-array?     [^int?     t ^nat-long? ct] (->int-array     ct)))
  #?(:clj  (^long-array?    [^long?    t ^nat-long? ct] (->long-array    ct)))
  #?(:clj  (^float-array?   [^float?   t ^nat-long? ct] (->float-array   ct)))
           (^double-array?  [^double?  t ^nat-long? ct] (->double-array  ct))
  #?(:cljs (                [          x ^nat-long? ct] (->object-array  ct)))
  #?(:clj  (                [^Class    c ^nat-long? ct] (make-array c    ct)))) ; object-array is subsumed into this

(defnt empty
  {:todo #{"Most of this should be in some static map somewhere for efficiency"
           "implement core/empty"}}
           (     [^boolean?  x] false         )
  #?(:clj  (     [^char?     x] (->char   0)  ))
  #?(:clj  (     [^byte?     x] (->byte   0)  ))
  #?(:clj  (     [^short?    x] (->short  0)  ))
  #?(:clj  (     [^int?      x] (->int    0)  ))
  #?(:clj  (     [^long?     x] (->long   0)  ))
  #?(:clj  (     [^float?    x] (->float  0)  ))
  #?(:clj  (     [^double?   x] (->double 0)  ))
  #?(:cljs (^<0> [^pnum?     x] 0             ))
           (^<0> [^string?   x] ""            )
           ; TODO ^array?
           (^<0> [^array-1d? x] (array-of-type x (count x))) ; TODO should it be `Array/cloneSizes`?
           (^<0> [#{#?(:clj  IPersistentCollection
                       :cljs IEmptyableCollection)} x] (#?(:clj .empty :cljs -empty) x)))

(defnt lasti
  "Last index of a coll."
  (^int  [#{string? array?} x] (int (unchecked-dec (count x))))
  (^long [                  x] (unchecked-dec (count-protocol x))))

; ===== COPY ===== ;

(#?(:clj defnt' :cljs defnt) copy! ; shallow copy
  (^<0> [^array? in ^int? in-pos :<0> out ^int? out-pos ^int? length]
    #?(:clj  (System/arraycopy in in-pos out out-pos length)
       :cljs (dotimes [i (- (.-length in) in-pos)]
               (core/aset out (+ i out-pos) (core/aget in i))))
    out)
  (^<0> [^array? in :<0> out ^nat-int? length]
    (copy! in 0 out 0 length)))

#?(:clj (defalias shallow-copy! copy!))

(defn deep-copy! [in out length] (TODO))

; TODO `array?`
(defnt copy ([^array-1d? in] #?(:clj (copy! in (empty in) (count in)) :cljs (.slice in))))

; ===== SLICE ===== ;

(defnt subseq
  "Returns a view of ->`x`, [->`a` to ->`b`), in O(1) time."
          ([^+vec?       x ^nat-long? a             ] (subvec       x a  ))
          ([^+vec?       x ^nat-long? a ^nat-long? b] (subvec       x a b))
  #?(:clj ([^array-list? x ^nat-long? a             ] (.subList     x a (count x))))
  #?(:clj ([^array-list? x ^nat-long? a ^nat-long? b] (.subList     x a b)))
  #?(:clj ([^string?     x ^nat-long? a             ] (.subSequence x a (count x))))
  #?(:clj ([^string?     x ^nat-long? a ^nat-long? b] (.subSequence x a b)))
          ([^reducer?    x ^nat-long? a             ] (->> x (drop+ a)))
          ([^reducer?    x ^nat-long? a ^nat-long? b] (->> x (drop+ a) (take+ b))))

(defnt slice
  "Makes a subcopy of ->`x`, [->`a`, ->`b`), in the most efficient way possible.
   Differs from `subseq` in that it does not simply return a view in O(1) time."
  (     [^string?     x ^nat-long? a             ] (.substring x a (count x)))
  (     [^string?     x ^nat-long? a ^nat-long? b] (.substring x a b))
  (     [^reducer?    x ^nat-long? a ^nat-long? b] (->> x (drop+ a) (take+ b)))
  (     [^+vec?       x ^nat-long? a             ] (subsvec x a (count x)))
  (     [^+vec?       x ^nat-long? a ^nat-long? b] (subsvec x a b))
  (^<0> [^array-1d?   x ^nat-long? a             ]
    (slice x a (- (count x) a)))
  (^<0> [^array-1d?   x ^nat-long? a ^nat-long? b]
    #?(:clj  (let [n   (- b a)
                   ret (array-of-type x (int n))] ; TODO make int cast unnecessary
               (copy! x a ret 0 n))
       :cljs (.slice x a b)))
  (     [^:obj        x ^nat-long? a             ] (->> x (drop a)))
  (     [^:obj        x ^nat-long? a ^nat-long? b] (->> x (take b) (drop a)))
  (     [^array-1d?   x] (copy x)))

(defnt rest
  "Eager rest."
  ([^keyword?  k   ] (-> k name core/rest))
  ([^symbol?   s   ] (-> s name core/rest))
  ([^reducer?  coll] (drop+ 1 coll))
  ([^string?   coll] (slice coll 1 (count coll)))
  ([^+vec?     coll] (slice coll 1 (count coll)))
  ([^array-1d? coll] (slice coll 1 (count coll)))
  ([           coll] (core/rest coll)))

#?(:clj (defalias popl rest))

(defnt index-of
  {:todo ["Add 3-arity for |index-of-from|"]}
  ; Reflection warning - call to method indexOf on clojure.lang.IPersistentVector can't be resolved (no such method).
  ;([^+vec?   coll elem] (let [i (.indexOf coll elem)] (if (= i -1) nil i)))
  ([^string? coll elem]
    (cond (string? elem)
          (let [i (.indexOf coll ^String elem)] (if (= i -1) nil i))
          (pattern? elem)
          #?(:clj  (let [^java.util.regex.Matcher matcher
                          (re-matcher elem coll)]
                     (when (.find matcher)
                       (.start matcher)))
             :cljs (throw (->ex :unimplemented
                                (str "|index-of| not implemented for " (class coll) " on " (class elem))
                                (kmap coll elem))))
          :else (throw (->ex :unimplemented
                             (str "|last-index-of| not implemented for String on" (class elem))
                             (kmap coll elem))))))

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
   ; Reflection warning - call to method lastIndexOf on clojure.lang.IPersistentVector can't be resolved (no such method).
  ;([^+vec?   coll elem] (let [i (.lastIndexOf coll elem)] (if (= i -1) nil i)))
  ([^string? coll elem]
    (cond (string? elem)
          (let [i (.lastIndexOf coll ^String elem)] (if (= i -1) nil i))
          :else (throw (->ex :unimplemented
                             (str "|last-index-of| not implemented for String on" (class elem))
                             (kmap coll elem))))))

(defnt containsk?
  {:imported "clojure.lang.RT.contains"}
           ([#{string? array?}                            coll ^nat-long? n] (and (>= n 0) (<  (count coll))))
  #?(:clj  ([#{clojure.lang.Associative    java.util.Map} coll           k] (.containsKey   coll k)))
  #?(:clj  ([#{clojure.lang.IPersistentSet java.util.Set} coll           k] (.contains      coll k)))
  #?(:cljs ([#{+set? +map?}                               coll           k] (core/contains? coll k))) ; TODO find out how to make faster
           ([^:obj                                        coll           k]
             (if (nil? coll)
                 false
                 (throw (->ex :not-supported
                          (str "contains? not supported on type: " (-> coll class)))))))

#?(:clj (defalias contains? containsk?))

(defnt containsv?
  ([^string?  x elem]
    (and (nnil? elem) (index-of x elem)))
  ([#{keyword? symbol?} x elem]
    (or (some-> x name      (containsv? elem))
        (some-> x namespace (containsv? elem))))
  ([^pattern? x elem]
    (nnil? (re-find elem x)))
  ([          x elem]
    (seq-or (fn= elem) x)))

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

(defnt get
  {:imported    "clojure.lang.RT/get"
   :todo        {0 "Need to excise non-O(1) `nth`"}
   :performance "(java.lang.reflect.Array/get coll n) is about 4 times faster than core/get"}
  #?(:clj  ([^clojure.lang.ILookup           x            k             ] (.valAt x k)))
  #?(:clj  ([^clojure.lang.ILookup           x            k if-not-found] (.valAt x k if-not-found)))
  #?(:clj  ([#{java.util.Map clojure.lang.IPersistentSet}
                                             x            k             ] (.get x k)))
           ([^string?                        x ^nat-long? i if-not-found] (if (>= i (count x)) if-not-found (.charAt x i)))
  #?(:clj  ([^array-list?                    x ^nat-long? i if-not-found] (if (>= i (count x)) if-not-found (.get    x i))))
           ([#{string? #?(:clj array-list?)} x ^nat-long? i             ] (get      x i nil))

           ([^array-1d? x #?(:clj #{int}) i1]
            (#?(:clj  Array/get
                :cljs core/aget) x i1))
           #?(:clj ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
            ^int i1]
            (Array/get x i1)))
           ([^tuple?                         x ^nat-long? i             ] (get (.-vs x) i))
           ([^seq?                           x            i             ] (core/nth x i nil         ))
           ([^seq?                           x            i if-not-found] (core/nth x i if-not-found))
           ; TODO look at clojure.lang.RT/get for how to handle these edge cases efficiently
  #?(:cljs ([^nil?                           x            i             ] (core/get x i nil         )))
  #?(:cljs ([^nil?                           x            i             ] (core/get x i nil         )))
  #?(:cljs ([^nil?                           x            i if-not-found] (core/get x i if-not-found)))
         #_([                                x            i             ] (core/get x i nil         ))
         #_([                                x            i if-not-found] (core/get x i if-not-found)))

#?(:clj ; TODO macro to de-repetitivize
(defnt get-in*
  ([#{array-1d? array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1]
    (Array/get x i1))
  ([^tuple?                 x ^nat-long? i] (get x i))
  ([#{clojure.lang.ILookup
      clojure.lang.APersistentVector$RSeq} x k1] (get x k1))
  ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x
    ^int i1 ^int i2]
    (Array/get x i1 i2))
  ([#{clojure.lang.ILookup
      clojure.lang.APersistentVector$RSeq} x k1 k2] (-> x (get k1) (get k2)))
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

#?(:cljs (defn get-in*-protocol [arr & ks] (TODO)))

(defnt nth
  ; TODO import clojure.lang.RT/nth
  ([#{+vec? seq?}    coll            i] (get coll i))
  ([#{string? array-list?
      array? tuple?} coll ^nat-long? i] (get coll i))
  ([^reducer?        coll ^nat-long? i]
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

#?(:clj  (defnt swap!
           ([^IAtom x f      ] (.swap x f      ))
           ([^IAtom x f a0   ] (.swap x f a0   ))
           ([^IAtom x f a0 a1] (.swap x f a0 a1)))
   :cljs (defalias swap! core/swap!))

(defalias doto! swap!)

#?(:clj  (defnt reset!
           ([#{IAtom clojure.lang.Volatile} x          v] (.reset x v) v)
           ([^AtomicReference               x          v] (.set   x v) v)
           ([^AtomicBoolean                 x ^boolean v] (.set   x v) v)
           ([^AtomicInteger                 x ^int     v] (.set   x v) v)
           ([^AtomicLong                    x ^long    v] (.set   x v) v))
   :cljs (defalias reset! core/reset!))

(declare assoc-protocol)

; TODO assoc!, assoc-in! for files
(defnt assoc!
  {:performance "|java.lang.reflect.Array/set| is 26 times faster
                  than 'normal' reflection"}
  #?(:cljs (^<0> [^array?         x ^int i :<0>:1 v] (aset      x i v) x))
  #?(:clj  (^<0> [^array?         x ^int i :<0>:1 v] (Array/set x v i)))
  #?(:clj  (^<0> [^list?          x ^int i       v] (.set x i v) x)) ; it may fail sometimes
           (^<0> [^transient?     coll      k       v] (core/assoc! coll k v))
           (     [^atom?          coll      k       v] (swap! coll assoc-protocol k v))
  #?(:clj  (^<0> [                x ^int i       v]
             (if (t/array? x)
                 (java.lang.reflect.Array/set x i v)
                 (throw (->ex :not-supported "`assoc!` not supported on this object" {:type (type x)}))))))

(defnt assoc
  {:imported "clojure.lang.RT/assoc"}
  #?(:clj  ([^clojure.lang.Associative x k v] (.assoc x k v)))
  #?(:cljs ([#{+vec? +map?}            x k v] (cljs.core/-assoc x k v)))
  #?(:cljs ([^nil?                     x k v] {k v}))
  #?(:clj  ([                          x k v]
             (if (nil? x)
                 {k v}
                 (throw (->ex :not-supported "`assoc` not supported on this object" {:type (type x)}))))))

(defnt assoc?!
  "`assoc`, maybe mutable. General `assoc(!)`.
   If the value is mutable  , it will mutably   `assoc!`.
   If the value is immutable, it will immutably `assoc`."
  (^<0> [^array?     x ^int k :<0>:1 v] (assoc! x k v))
  (^<0> [^transient? x      k        v] (assoc! x k v))
  (     [^atom?      x      k        v] (assoc! x k v))
  (     [            x      k        v] (assoc  x k v)))

#?(:clj ; TODO macro to de-repetitivize
(defnt' assoc-in!*
  "get-in : get-in* :: assoc-in! : assoc-in!*"
  ([#{array-1d? array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x :<0>:1 v
    ^int i1]
    (Array/set x v i1))
  ([#{array-2d? array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x :<0>:2 v
    ^int i1 ^int i2]
    (Array/set x v i1 i2))
  ([#{array-3d? array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x :<0>:3 v
    ^int i1 ^int i2 ^int i3]
    (Array/set x v i1 i2 i3))
  ([#{array-4d? array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x :<0>:4 v
    ^int i1 ^int i2 ^int i3 ^int i4]
    (Array/set x v i1 i2 i3 i4))
  ([#{array-5d? array-6d? array-7d? array-8d? array-9d? array-10d?} x :<0>:5 v
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5]
    (Array/set x v i1 i2 i3 i4 i5))
  ([#{array-6d? array-7d? array-8d? array-9d? array-10d?} x :<0>:6 v
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6]
    (Array/set x v i1 i2 i3 i4 i5 i6))
  ([#{array-7d? array-8d? array-9d? array-10d?} x :<0>:7 v
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7]
    (Array/set x v i1 i2 i3 i4 i5 i6 i7))
  ([#{array-8d? array-9d? array-10d?} x :<0>:8 v
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8]
    (Array/set x v i1 i2 i3 i4 i5 i6 i7 i8))
  ([#{array-9d? array-10d?} x :<0>:9 v
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8 ^int i9]
    (Array/set x v i1 i2 i3 i4 i5 i6 i7 i8 i9))
  ([#{array-10d?} x :<0>:10 v
    ^int i1 ^int i2 ^int i3 ^int i4 ^int i5
    ^int i6 ^int i7 ^int i8 ^int i9 ^int i10]
    (Array/set x v i1 i2 i3 i4 i5 i6 i7 i8 i9 i10))))

#?(:cljs (defn assoc-in!*-protocol [arr & ks] (TODO)))

(defnt dissoc
  {:imported "clojure.lang.RT/dissoc"}
           ([^+map?                       coll k] (#?(:clj .without :cljs -dissoc ) coll k))
           ([^+set?                       coll x] (#?(:clj .disjoin :cljs -disjoin) coll x))
           ([^+vec?                       coll i]
             (catvec (subvec coll 0 i) (subvec coll (inc (#?(:clj identity* :cljs long) i)) (count coll))))
  #?(:cljs ([^nil?                        coll x] nil))
  #?(:clj  ([                             coll x]
             (if (nil? coll)
                 nil
                 (throw (->ex :not-supported "`dissoc` not supported on this object" {:type (type coll)}))))))

(defnt dissoc!
  ([^transient? coll k  ] (core/dissoc! coll k))
  ([^atom?      coll k  ] (swap! coll (fn&2 dissoc) k)))

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
  ([^array?                         x] (nth x 0))
  ([#{string? #?(:clj array-list?)} x] (get x 0 nil))
  ([#{symbol? keyword?}             x] (if (namespace x) (-> x namespace first) (-> x name first)))
  ([^+vec?                          x] (nth x 0))
  ([^reducer?                       x] (reduce (rfn [_ x'] (reduced x')) nil x))
  #_([:else                           x] (core/first x)))

(defalias firstl first) ; TODO not always true

(defnt second
  ([#{array? +vec? reducer?}        x] (nth x 1))
  ([#{string? #?(:clj array-list?)} x] (#?(:clj get& :cljs get) x 1 nil))
  ([#{symbol? keyword?}             x] (if (namespace x) (-> x namespace second) (-> x name second)))
  #_([:else                           x] (core/second x)))

(defnt butlast
  {:todo ["Add support for CLJS IPersistentStack"]}
          ([#{string? array-1d?}           x] (#?(:clj slice& :cljs slice) x 0 (#?(:clj lasti& :cljs lasti) x)))
  #?(:clj ([^reducer?                      x] (dropr+ 1 x)))
          ; TODO reference to field pop on clojure.lang.APersistentVector$RSeq can't be resolved.
          ([^+vec?                         x] (if (empty? x) (#?(:clj .pop :cljs -pop) x) x))
          ([^default                       x] (core/butlast x)))

(defalias pop  butlast) ; TODO not always correct
(defalias popr butlast)

(defnt last
          ([#{string? array?}   x] (#?(:clj get& :cljs get) x (#?(:clj lasti& :cljs lasti) x)))
          ([#{symbol? keyword?} x] (-> x name last))
  #?(:clj ([^reducer?           x] (taker+ 1 x)))
          ; TODO reference to field peek on clojure.lang.APersistentVector$RSeq can't be resolved.
          ([^+vec?              x] (#?(:clj .peek :cljs .-peek) x))
  #?(:clj ([#{#?@(:clj  [array-list? clojure.lang.PersistentVector$TransientVector]
                  :cljs [cljs.core/TransientVector])} x]
            (get x (lasti x))))
          ([^default            x] (core/last x)))

(defalias peek   last) ; TODO not always correct
(defalias firstr last)

#?(:clj  (defn array
           {:todo ["Consider efficiency here"]}
           [& args]
           (let [c (-> args first class)]
             (into-array (get tdef/boxed->unboxed-types-evaled c c) args)))
   :cljs (defalias array core/array))

(def third   (fn1 get 2))
(def fourth  (fn1 get 3))
(def fifth   (fn1 get 4))
(def sixth   (fn1 get 5))
(def seventh (fn1 get 6))
(def eighth  (fn1 get 7))
(def ninth   (fn1 get 8))
(def tenth   (fn1 get 9))

;--------------------------------------------------{           CONJL          }-----------------------------------------------------
; This will take AGES to compile if you try to allow primitives
(defnt conjl
  ([^seq?  coll a          ] (->> coll (cons a)                                             ))
  ([^seq?  coll a b        ] (->> coll (cons b) (cons a)                                    ))
  ([^seq?  coll a b c      ] (->> coll (cons c) (cons b) (cons a)                           ))
  ([^seq?  coll a b c d    ] (->> coll (cons d) (cons c) (cons b) (cons a)                  ))
  ([^seq?  coll a b c d e  ] (->> coll (cons e) (cons d) (cons c) (cons b) (cons a)         ))
  ([^seq?  coll a b c d e f] (->> coll (cons f) (cons e) (cons d) (cons c) (cons b) (cons a)))
  ([^+vec? coll a          ] (catvec (svector a          ) coll))
  ([^+vec? coll a b        ] (catvec (svector a b        ) coll))
  ([^+vec? coll a b c      ] (catvec (svector a b c      ) coll))
  ([^+vec? coll a b c d    ] (catvec (svector a b c d    ) coll))
  ([^+vec? coll a b c d e  ] (catvec (svector a b c d e  ) coll))
  ([^+vec? coll a b c d e f] (catvec (svector a b c d e f) coll))
  ([^+vec? coll a b c d e f & more]
    (reduce (fn [ret elem] (conjl ret elem)) ; should just be |conjl|
      (svector a b c d e f) more)))

; TODO to finish from RT
; (defnt conj
;   ([^IPersistentCollection coll ^Object x]
;     (if (nil? coll)
;         (PersistentList. x)
;         (.cons coll x))))

(defalias conj core/conj)

(defnt conj?!
  "`conj`, maybe mutable. General `conj(!)`.
   If the value is mutable  , it will mutably   `conj!`.
   If the value is immutable, it will immutably `conj`."
  ([^transient? x v] (conj! x v))
  ([^atom?      x v] (conj! x v))
  ([            x v] (conj  x v)))

(defnt conjr
  ([^+vec? coll a    ] (core/conj a    ))
  ([^+vec? coll a b  ] (core/conj a b  ))
  ([^+vec? coll a b c] (core/conj a b c))
  ;([coll a & args] (apply conj a args))
  ([^seq?  coll a    ] (concat coll (list a    )))
  ([^seq?  coll a b  ] (concat coll (list a b  )))
  ([^seq?  coll a b c] (concat coll (list a b c)))
  ;([coll a & args] (concat coll (cons arg args)))
  )

#?(:clj (defnt ^clojure.lang.PersistentVector ->vec
          "513.214568 msecs (vec a1)
           182.745605 msecs (seqspert.vector/array-to-vector a1)"
          ([^array-1d? x] (if (> (count x) parallelism-threshold)
                              (seqspert.vector/array-to-vector x)
                              (vec x))))
   :cljs (defalias ->vec core/vec))

#?(:clj
(defnt ^"[Ljava.lang.Object;" ->arr
  ([^+vec? x] (if (> (count x) parallelism-threshold)
                  (seqspert.vector/vector-to-array x)
                  (into-array Object x)))))

; TODO VECTOR OPS
; 166.884981 msecs (mapv identity v1)
; 106.545886 msecs (seqspert.vector/vmap   identity v1)))
; 22.778568  msecs (seqspert.vector/fjvmap identity v1)

(defn- handle-kv
  [kv f]
  (if (-> kv count (= 2))
      (f)
      (throw (->ex :not-supported "`key/val` not supported on collections of count != 2"
                   {:coll kv :ct (count kv)}))))

(defnt ^:private key*
  {:todo #{"Implement core/key"}}
  #?@(:clj  [([^map-entry?     kv] (core/key kv))
             ([^list?          kv] (handle-kv kv #(first-protocol kv)))] ; TODO make not protocol
      :cljs [([#{+vec? array?} kv] (handle-kv kv #(first-protocol kv)))])) ; TODO make not protocol

(defnt ^:private val*
  {:todo #{"Implement core/val"}}
  #?@(:clj  [([^map-entry?     kv] (core/val kv))
             ([^list?          kv] (handle-kv kv #(second-protocol kv)))] ; TODO make not protocol
      :cljs [([#{+vec? array?} kv] (handle-kv kv #(second-protocol kv)))])) ; TODO make not protocol

(defn key ([kv] (when kv (key* kv))) ([k v] k))
(defn val ([kv] (when kv (val* kv))) ([k v] v))

(defnt reverse
  #_(^<0> [^array? x] ; TODO the code is good but it has a VerifyError
    (let [n   (count x)
          ret (empty x)]
      (dotimes [i n] (assoc! ret i (get x (- n (inc i)))))
      ret))
  (^<0> [x] (if (reversible? x) (rseq x) (core/reverse x)))) ; TODO

#?(:cljs (defnt reverse! (^<0> [^array? x] (.reverse x))))

; ===== JOIN ===== ;

(defnt joinl
  "Join, left.
   Like |into|, but handles kv sources,
   and chooses the most efficient joining/combining operation possible
   based on the input types."
  {:attribution "Alex Gunnarson"
   :todo ["Shorten this code using type differences and type unions with |editable?|"
          "Handle arrays"
          "Handle one-arg"
          "Handle mutable collections"]}
  ([to] to)
  ([^reducer?       from] (joinl [] from))
  ([^+vec?          to from] (if (vector? from)
                                 (catvec              to from)
                                 (red/transient-into to from)))
  ([^+unsorted-set? to from] #?(:clj  (if (t/+unsorted-set?-protocol from)
                                          (seqspert.hash-set/sequential-splice-hash-sets to from)
                                          (red/transient-into to from))
                             :cljs (red/transient-into to from)))
  ([^+sorted-set?   to from] (if (t/+set?-protocol from)
                                 (clojure.set/union    to from)
                                 (red/persistent-into to from)))
  ([^+hash-map?     to from] #?(:clj  (if (t/+hash-map?-protocol from)
                                          (seqspert.hash-map/sequential-splice-hash-maps to from)
                                          (red/transient-into to from))
                                :cljs (red/transient-into to from)))
  ([^+sorted-map?   to from] (red/persistent-into to from))
  ([^string?        to from] (str #?(:clj  (red/reduce*-protocol from #(.append ^StringBuilder %1 %2) (StringBuilder. to))
                                     :cljs (red/reduce*-protocol from #(.append ^StringBuffer  %1 %2) (StringBuffer.  to)))))
  (^<0> [^array-1d? a :<0> b] ; returns a new array
    (if (identical? (class a) (class b))
        #?(:clj  (let [al  (count a)
                       bl  (count b)
                       n   (+ al bl)
                       ret (array-of-type a (int n))]
                   (copy! a 0 ret 0  al)
                   (copy! b 0 ret al bl)
                   ret)
           :cljs (.concat a b))
        (TODO)))
  ([                to from] (if (nil? to) from (red/persistent-into to from))))

#_(defn joinl
  ([] nil)
  ([to] to)
  ([to from] (joinl* to from))
  ([to from & froms]
    (reduce joinl (joinl to from) froms)))

(defnt joinl'
  "Like `joinl`, but reduces into the empty version of the
   collection passed."
  ([^reducer?     from] (joinl' (empty          (:coll from)) from))
  ([              from] (joinl' (empty-protocol        from ) from))
  ([^+list?    to from] (list* (concat to from))) ; To preserve order ; TODO test whether (reverse (join to from)) is faster
  ([           to from] (joinl-protocol to from)))

#?(:clj (defalias join  joinl ))
#?(:clj (defalias join' joinl'))
