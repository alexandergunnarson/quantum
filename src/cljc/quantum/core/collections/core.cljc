 (ns
  ^{:doc "Retakes on core collections functions like first, rest,
          get, nth, last, index-of, etc.

          Also includes innovative functions like getr, etc."}
  quantum.core.collections.core
  (:refer-clojure :exclude
    [vector hash-map rest count first second butlast last get pop peek
     conj! conj assoc! dissoc! dissoc disj! contains?
     #?@(:cljs [empty? array])])
  (:require-quantum
    [:core err fn log logic red #_str map set macros type vec arr pconvert #_num])
  #?(:clj (:require [seqspert.vector]))
)

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


#?(:clj
(definterface IMutable
  (get [])
  (set [x])))

#?(:clj
(deftype MutableContainer [^:unsynchronized-mutable val]
  IMutable
  (get [this] val)
  (set [this x]
    (set! val x))))

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

(defnt #?(:clj ^long count
          :cljs      count)
  ([^array?                                             x] (alength x))
#?(:clj 
  ([^clojure.core.async.impl.channels.ManyToManyChannel x] (count (.buf x))))
  ([                                                    x] (core/count x))
  ; Debatable whether this should be allowed
  ([:else                                               x] 0))

#?(:cljs
(defnt empty?
  ([^array? x] (zero? (count x)))))

(comment
  #?(:clj
(defnt empty
  {:todo ["This should be in some static map somewhere"]}
  ([^boolean x] false)
  ([^char    x] (char   0))
  ([^byte    x] (byte   0))
  ([^short   x] (short  0))
  ([^int     x] (int    0))
  ([^long    x] (long   0))
  ([^float   x] (short  0))
  ([^double  x] (double 0)))))

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

(defnt getr
  {:todo "Differentiate between |subseq| and |slice|"}
  ; inclusive range
          ([^string?     coll ^pinteger? a ^pinteger? b] (.substring coll a (inc b)))
          ([^qreducer?   coll ^pinteger? a ^pinteger? b] (->> coll (take+ b) (drop+ a)))
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
  ([^keyword?  k]    (-> k name core/rest))
  ([^symbol?   s]    (-> s name core/rest))
  ([^qreducer? coll] (drop+ 1 coll))
  ([^string?   coll] (getr coll 1 (lasti coll)))
  ([^vec?      coll] (getr coll 1 (lasti coll)))
  ([^array?    coll] (getr coll 1 (core/long (lasti coll)))) ; TODO use macro |long|
  ([           coll] (core/rest coll))) 

(defalias popl rest)

(def neg-1? (eq? -1))

(defnt index-of
  {:todo ["Reflection on short, bigint"
          "Add 3-arity for |index-of-from|"]}
  ([^vec?    coll elem] (whenc (.indexOf coll elem) neg-1? nil))
  ([^string? coll elem] (whenc (.indexOf coll (str elem)) neg-1? nil))
  ([coll elem] (throw+ (Err. :unimplemented "Index-of not implemented for" (class coll)))))

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
  ([^vec?    coll elem] (whenc (.lastIndexOf coll elem) neg-1? nil))
  ([^string? coll elem] (whenc (.lastIndexOf coll (str elem)) neg-1? nil))
  ([coll elem] (throw (->ex :unimplemented "Index-of not implemented for" (class coll)))))

(defnt containsk?
  {:imported "clojure.lang.RT.contains"}
           ([#{string? array?}                            coll ^pinteger? n] (and (>= n 0) (<  (count coll))))
  #?(:clj  ([#{clojure.lang.Associative    java.util.Map} coll            k] (.containsKey coll k)))
  #?(:clj  ([#{clojure.lang.IPersistentSet java.util.Set} coll            k] (.contains    coll k)))
  #?(:cljs ([#{set? map?}                                 coll            k] (core/contains? coll k))) ; TODO find out how to make faster   
           ([^:obj                                        coll            k]
             (if (nil? coll)
                 false
                 (throw (->ex :not-supported
                          (str "contains? not supported on type: " (-> coll type)))))))

(defalias contains? containsk?)

(defnt containsv?
  ([^string?  coll elem]
    (and (nnil? subs) (index-of coll elem)))
  ([^pattern? coll elem]
    (nnil? (re-find elem coll)))
  ([          coll elem]
    (any? (fn-eq? elem) coll)))

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
           ([^listy?               coll            n             ] (nth      coll n nil         ))
           ([^listy?               coll            n if-not-found] (nth      coll n if-not-found))
           ; TODO look at clojure.lang.RT/get for how to handle these edge cases efficiently
  #?(:cljs ([^nil?                 coll            n             ] (core/get coll n nil         )))
  #?(:cljs ([^nil?                 coll            n if-not-found] (core/get coll n if-not-found)))
           ([                      coll            n             ] (core/get coll n nil         ))
           ([                      coll            n if-not-found] (core/get coll n if-not-found)))

(defalias doto! swap!)

; TODO assoc-in and assoc-in! for files
(defnt assoc!
  {:todo ["Remove reflection for |aset|."]}
  ([^array?     coll ^pinteger? i :elem v] (aset        coll i v) coll)
  ([^transient? coll            k       v] (core/assoc! coll k v))
  ([^atom?      coll            k       v] (swap! coll assoc k v)))

(defnt dissoc
  {:imported "clojure.lang.RT/dissoc"}
  #?(:clj  ([^clojure.lang.IPersistentMap coll k] (.without coll k)))
  #?(:cljs ([^map?                        coll k] (core/dissoc coll k)))
           ([coll k]
             (if (nil? coll)
                 nil
                 (throw (->ex :not-supported (str "|dissoc| not supported on" (type coll)))))))

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
  ([:else                                  coll] (core/first coll)))

(defalias firstl first)

(defnt second
  ([#{string? #?(:clj array-list?)} coll] (get coll 1))
  ; 2.8  nanos to (.cast Long _)
  ; 1.26 nanos to (Long. _)
  ([^vec?                           coll] (get coll #?(:clj (Long. 1) :cljs 1))) ; to cast it...
  ([^qreducer?                      coll] (take+ 1 coll))
  ([:else                           coll] (core/second coll)))

(defnt butlast
  {:todo ["Add support for arrays"
          "Add support for CLJS IPersistentStack"]}
          ([^string?                       coll] (getr coll 0 (-> coll lasti dec)))
  #?(:clj ([^qreducer?                     coll] (dropr+ 1 coll)))
  #?(:clj ([^clojure.lang.IPersistentStack coll] (.pop coll)))
          ([^vec?                          coll] (whenf coll nempty? core/pop))
  #?(:clj ([^clojure.lang.IPersistentList  coll] (core/butlast coll)))
          ([:else                          coll] (core/butlast coll)))

(defalias pop  butlast)
(defalias popr butlast)

(defnt last
          ([^string?          coll] (get coll (lasti coll)))
  #?(:clj ([^qreducer?        coll] (taker+ 1 coll)))
          ([^vec?             coll] (#?(:clj .peek :cljs .-peek) coll)) ; because |peek| works on lists too
  #?(:clj ([#{#?@(:clj  [array-list? clojure.lang.PersistentVector$TransientVector]
                  :cljs [cljs.core/TransientVector])} coll]
            (get coll (lasti coll))))
          ([:else             coll] (core/last coll)))
    
(defalias peek last)
(defalias firstr last)

#?(:clj  (defn array
           {:todo ["Consider efficiency here"]}
           [& args] (into-array (-> args first type) args))
   :cljs (defalias array core/array))


(defn gets [coll indices]
  (->> indices (red/map+ #(get coll %)) red/fold+))

(def third (f*n get 2))

(defn getf [n] (f*n get n))

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


; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)


