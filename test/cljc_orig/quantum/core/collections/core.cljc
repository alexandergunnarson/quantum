 (ns
  ^{:doc "Retakes on core collections functions like first, rest,
          get, nth, last, index-of, etc.

          Also includes innovative functions like getr, etc."}
  quantum.core.collections.core
  (:refer-clojure :exclude
    [vector hash-map rest count first second butlast last get pop peek
     conj! conj assoc! dissoc! dissoc disj! contains?
     dec dec' inc inc'])
  #?(:clj (:require seqspert.vector))
  (:require-quantum
    [ns err fn log logic red str map set macros type vec arr pconvert num])
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

; mape [f o] - eager version of clojure.core/map
; mape-indexed [f o] - eager version of clojure.core/map-indexed
; reduce [f init o] - faster version of clojure.core/reduce
; reduce2 [f o] - faster version of clojure.core/reduce
; reduce-reverse [f init o] - like reduce but in reverse order
; reduce2-reverse [f o] - like reduce but in reverse order
; reduce-kv [f init o] - faster version of clojure.core/reduce-kv
; reduce2-kv [f o] - faster version of clojure.core/reduce-kv
; reduce-kv-reverse [f init o] - like reduce-kv but in reverse order
; reduce2-kv-reverse [f o] - like reduce-kv but in reverse order

; TODO use set/map-invert instead of |reverse-keys|

; Arbitrary.
; TODO test this on every permutation for inflection point.
(def ^:const parallelism-threshold 10000)

; https://github.com/JulesGosnell/seqspert
; Very useful sequence and data structure info.


(definterface IMutable
  (get [])
  (set [x]))

(deftype MutableContainer [^:unsynchronized-mutable val]
  IMutable
  (get [this] val)
  (set [this x]
    (set! val x)))

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

(defnt ^long count
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

(macros/defnt prim-long? ; defnt' isn't ready yet
  ([^long n] true)
  ([:else n] false))

(defnt ^long lasti
  "Last index of a coll."
  [coll] (dec (count coll) ))

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
  ([^array-list? coll ^pinteger? a ^pinteger? b] (.subList coll a b))
  ([^vec?        coll ^pinteger? a ^pinteger? b] (subvec+ coll a (inc b)))
  ([^vec?        coll ^pinteger? a             ] (subvec+ coll a (-> coll count)))
#?(:clj
  (^first [^array?      coll ^pinteger? a ^pinteger? b]
    (let [arr-f (array-of-type coll (core/long (inc (- b a))))] ; TODO make long cast unnecessary
      (System/arraycopy coll a arr-f 0
        (inc (- b a)))
      arr-f)))
  ([^Object      coll ^pinteger? a             ] (->> coll (drop a)))
  ([^Object      coll ^pinteger? a ^pinteger? b] (->> coll (take b) (drop a)))) 

(defnt rest
  "Eager rest."
  ([^keyword?  k]    (-> k name core/rest))
  ([^symbol?   s]    (-> s name core/rest))
  ([^qreducer? coll] (drop+ 1 coll))
  ([^string?   coll] (getr coll 1 (lasti coll)))
  ([^vec?      coll] (getr coll 1 (lasti coll)))
  ([^array?    coll] (getr coll 1 (core/long (lasti coll))))
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
  ([coll elem] (throw+ (Err. :unimplemented "Index-of not implemented for" (class coll)))))

(defnt containsk?
  {:imported "clojure.lang.RT.contains"}
  ([#{String array?}                             coll ^pinteger? n] (and (>= n 0) (<  (count coll))))
  ([#{clojure.lang.Associative    java.util.Map} coll            k] (.containsKey coll k))
  ([#{clojure.lang.IPersistentSet java.util.Set} coll            k] (.contains    coll k))
  ([^Object                                      coll            k]
    (if (nil? coll)
        false
        (throw (IllegalArgumentException.
                 (str "contains? not supported on type: " (-> coll class (.getName))))))))

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
  ([^clojure.lang.ILookup coll            k             ] (.valAt coll k))
  ([^clojure.lang.ILookup coll            k if-not-found] (.valAt coll k if-not-found))
  ([#{java.util.Map
      clojure.lang.IPersistentSet} coll k] (.get coll k))
  ([^string?              coll ^pinteger? n             ] (.charAt  coll n             ))
  ([^array-list?          coll ^pinteger? n             ] (get      coll n nil         ))
  ([^array-list?          coll ^pinteger? n if-not-found]
    (try (.get coll n)
      (catch ArrayIndexOutOfBoundsException e# if-not-found)))
  ([^array?               coll ^pinteger? n             ] (aget     coll n             ))
  ([^listy?               coll            n             ] (nth      coll n nil         ))
  ([^listy?               coll            n if-not-found] (nth      coll n if-not-found))
  ; TODO look at clojure.lang.RT/get for how to handle these edge cases efficiently
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
  ([^clojure.lang.IPersistentMap coll k] (.without coll k))
  ([coll k]
    (if (nil? coll)
        nil
        (throw (Exception. (str "|dissoc| not supported on" (class coll)))))))

(defnt dissoc!
  ([^transient? coll k  ] (core/dissoc! coll k))
  ([^atom?      coll k  ] (swap! coll (extern (mfn 2 dissoc)) k)))

(defnt conj!
  ([^transient? coll obj] (core/conj! coll obj))
  ([^atom?      coll obj] (swap! coll core/conj obj)))

(defnt disj!
  ([^transient? coll obj] (core/disj! coll obj))
  ([^atom?      coll obj] (swap! coll disj obj)))

(defmacro update! [coll i f]
  `(assoc! ~coll ~i (~f (get ~coll ~i))))

(defnt first
  ([#{string? array-list? array?} coll] (get coll 0))
  ([^vec?                         coll] (get coll (Long. 0))) ; to cast it...
  ; TODO is this wise?
  ([^integral?                    coll] coll)
  ([:else                         coll] (core/first coll)))

(defalias firstl first)

(defnt second
  ([#{string? array-list?} coll] (get coll 1))
  ; 2.8  nanos to (.cast Long _)
  ; 1.26 nanos to (Long. _)
  ([^vec?                  coll] (get coll (Long. 1))) ; to cast it...
  ([^qreducer?             coll] (take+ 1 coll))
  ([:else                  coll] (core/second coll)))

(defnt butlast
  {:todo ["Add support for arrays"]}
  ([^string?                       coll] (getr coll 0 (-> coll lasti dec)))
#?(:clj
  ([^qreducer?                     coll] (dropr+ 1 coll)))
  ([^clojure.lang.IPersistentStack coll] (.pop coll))
  ([^vec?                          coll] (whenf coll nempty? core/pop))
  ([^clojure.lang.IPersistentList  coll] (core/butlast coll))
  ([:else                          coll] (core/butlast coll)))

(defalias pop  butlast)
(defalias popr butlast)

(defnt last
  ([^string?          coll] (get coll (lasti coll)))
#?(:clj
  ([^qreducer?        coll] (taker+ 1 coll)))
  ([^vec?             coll] (.peek coll)) ; because |peek| works on lists too
  ([#{array-list? clojure.lang.PersistentVector$TransientVector} coll]
    (get coll (lasti coll)))
  ([:else             coll] (core/last coll)))
    
(defalias peek last)
(defalias firstr last)

(defn array
  {:todo ["Consider efficiency here"]}
  [& args] (into-array (-> args first class) args))


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

(defnt ^clojure.lang.PersistentVector ->vec
  "513.214568 msecs (vec a1) 
   182.745605 msecs (seqspert.vector/array-to-vector a1)"
  ([^array? x] (if (> (count x) parallelism-threshold)
                   (seqspert.vector/array-to-vector x)
                   (vec x))))

(defnt ^"[Ljava.lang.Object;" ->arr
  "513.214568 msecs (vec a1) 
   182.745605 msecs (seqspert.vector/array-to-vector a1)"
  ([^vector? x] (if (> (count x) parallelism-threshold)
                    (seqspert.vector/vector-to-array x)
                    (into-array Object x))))

; VECTORS
; 166.884981 msecs (mapv identity v1)
; 106.545886 msecs (seqspert.vector/vmap   identity v1)))
; 22.778568  msecs (seqspert.vector/fjvmap identity v1)


; If the array is not sorted:
; java.util.Arrays.asList(theArray).indexOf(o)
; If the array is sorted, you can make use of a binary search for performance:
; java.util.Arrays.binarySearch(theArray, o)




