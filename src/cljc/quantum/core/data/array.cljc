(ns
  ^{:doc "Useful array functions. Array creation, joining, reversal, etc."
    :attribution "Alex Gunnarson"
    :todo ["Incorporate amap, areduce, aset-char, aset-boolean, etc."]}
  quantum.core.data.array
  (:refer-clojure :exclude
    [== reverse boolean-array byte-array char-array short-array
     int-array long-array float-array double-array])
  (:require-quantum [:core err log logic fn #_num loops macros])
  (:require [quantum.core.type.core :as tcore]
            [quantum.core.core :refer [name+]]
            [quantum.core.numeric :as num]
            #?(:clj [loom.alg-generic       :as alg  ])) ; temporarily
  #?(:clj (:import [java.io ByteArrayOutputStream]
                   [java.nio ByteBuffer]
                   java.util.ArrayList)))

(defalias aset! aset)

; TODO look at http://fastutil.di.unimi.it to complete this namespace
; TODO move this to type
(def package-class-map
  '{boolean Boolean
    byte    Byte
    char    Char
    short   Short
    int     Int
    long    Long
    float   Float
    double  Double})

#?(:clj
(defmacro create-fns
  "TODO ADD DOCUMENTATION"
  []
  (for [[package class] package-class-map]
    (let [arr-sym (symbol (str (name package) "-array"))
          fn-sym (-> arr-sym (macros/hint-meta (get tcore/type-casts-map arr-sym)))
          core-sym (symbol "clojure.core" (name fn-sym))
          n-sym (-> 'n gensym (macros/hint-meta 'pinteger?))]
      `(defnt ~fn-sym ([~n-sym] (core-sym ~n-sym)))))))

; ----- BOOLEAN ARRAY ----- ;

#?(:clj
(defnt boolean-array [^integer? n]
  (if (> n Integer/MAX_VALUE)
      (it.unimi.dsi.fastutil.booleans.BooleanBigArrays/newBigArray 1)
      (core/boolean-array n))))

; ----- BYTE ARRAY ----- ;

#?(:clj  (defalias byte-array core/byte-array)
   :cljs (defn byte-array [length] (js/Int8Array. length)))

#?(:clj
  (defn ^"[B"
    byte-array+
    "Like /byte-array/ but allows for array initializers a la Java:
     byte[] arr = byte[]{12, 8, 10}"
    {:attribution "Alex Gunnarson"
     :todo ["Make less repetitive via macro"]}
    ([size]
      (byte-array (long size)))
    ([size & args]
      (let [^"[B" arr (byte-array (long size))]
        (doseqi [arg args n]
          (aset! arr n (-> arg first byte)))
        arr))))

; ----- INT ARRAY ----- ;

#?(:clj
  (defn ^ints int-array+
    "Like /int-array/ but allows for array initializers a la Java:
     int[] arr = int[]{12, 8, 10}"
    {:attribution "Alex Gunnarson"
     :todo ["Make less repetitive via macro"]}
    ([size]
      (core/int-array (long size)))
    ([size & args]
      (let [^ints arr (core/int-array (long size))]
        (doseqi [arg args n]
          (aset! arr (long n) (-> arg first int)))
        arr))))

; TODO: Use a macro for this
#?(:clj
  (defn long-array-of
    "Creates a long array with the specified values."
    {:attribution "mikera.cljutils.arrays"}
    (^longs [] (core/long-array 0))
    (^longs [a] 
      (let [arr (core/long-array 1)]
        (aset! arr 0 (long a))
        arr))
    ([a b] 
      (let [arr (core/long-array 2)]
        (aset! arr 0 (long a))
        (aset! arr 1 (long b))
        arr))
    ; ([a b & more] 
    ;   (let [arr (long-array (+ 2 (count more)))]
    ;     (aset! arr 0 (long a))
    ;     (aset! arr 1 (long b))
    ;     (doseqi [x more i] (aset! arr (+ 2 i) (long x))) 
    ;     arr))
    ))

; ----- OBJECT ARRAY ----- ;

; TODO: Use a macro for this
#?(:clj
  (defn object-array-of 
    "Creates an object array with the specified values."
    {:attribution "mikera.cljutils.arrays"}
    ([] (core/object-array 0))
    ([a] 
      (let [arr (core/object-array 1)]
        (aset! arr 0 a)
        arr))
    ([a b] 
      (let [arr (core/object-array 2)]
        (aset! arr 0 a)
        (aset! arr 1 b)
        arr))
    ([a b c] 
      (let [arr (core/object-array 3)]
        (aset! arr 0 a)
        (aset! arr 1 b)
        (aset! arr 2 c)
        arr))
    ([a b c d] 
      (let [arr (core/object-array 4)]
        (aset! arr 0 a)
        (aset! arr 1 b)
        (aset! arr 2 c)
        (aset! arr 3 d)
        arr))
    ; ([a b & more] 
    ;   (let [arr (object-array (+ 2 (count more)))]
    ;     (aset! arr 0 a)
    ;     (aset! arr 1 b)
    ;     (doseqi [x more i] (aset! arr (+ 2 i) x)) 
    ;     arr))
    ))

#?(:clj
(defmacro array [type n]
  (condp = type
    'boolean `(boolean-array ~n)
    'byte    `(byte-array    ~n)
    'char    `(char-array    ~n)
    'short   `(short-array   ~n)
    'int     `(int-array     ~n)
    'long    `(long-array    ~n)
    'float   `(float-array   ~n)
    'double  `(double-array  ~n)
    'object  `(object-array  ~n)
    'Object  `(object-array  ~n)
    `(make-array ~type ~n))))

; ===== BITMAPS ===== ;

; TODO these should be both clj and cljs
#?(:clj (defalias ->bitmap       alg/bm-new         ))
#?(:clj (defalias bm-set!        alg/bm-set         ))
#?(:clj (defalias bm-get         alg/bm-get         )) ; TODO incorporate into |get|
#?(:clj (defalias bm-or          alg/bm-or          ))
#?(:clj (defalias bm-get-indices alg/bm-get-indicies))

; ===== CONVERSION ===== ;

#?(:clj
(defnt ^"[B" ->bytes
  {:attribution ["ztellman/byte-streams" "funcool/octet" "gloss.data.primitives"]
   :contributors {"Alex Gunnarson" "defnt-ed"}}
  (^{:cost 0} [^bytes? x] x)
  ; "gloss.data.primitives"
  (           [^long?  x] (-> (ByteBuffer/allocate 8) (.putLong x) .array))
  (^{:cost 2} [^String s         ] (->bytes s nil))
  (^{:cost 2} [^String s encoding]
    #?(:clj (let [^String encoding-f (whenc encoding (fn-> name+ nil?) "UTF-8")]
              (.getBytes s encoding-f))
       ; funcool/octet.spec.string
       :cljs (let [buff (js/ArrayBuffer. (count s))
                   view (js/Uint8Array. buff)]
               (dotimes [i (count s)]
                 (aset view i (.charCodeAt value i)))
               (js/Int8Array. buff))))
  (^{:cost 1} [^java.nio.ByteBuffer buf]
    (if (.hasArray buf)
        (if (num/== (alength (.array buf)) (.remaining buf))
            (.array buf)
            (let [arr (byte-array (.remaining buf))]
              (doto buf
                .mark
                (.get arr 0 (.remaining buf))
                .reset)
              arr))
        (let [^bytes arr (byte-array (.remaining buf))]
          (doto buf .mark (.get arr) .reset)
          arr)))
  (^{:cost 1.5} [^java.io.InputStream in options]
    (let [out (ByteArrayOutputStream. (num/max 64 (.available in)))
          buf (byte-array 16384)]
      (loop []
        (let [len (.read in buf 0 16384)]
          (when-not (neg? len)
            (.write out buf 0 len)
            (recur))))
      (.toByteArray out))) 
  #_(^{:cost 2} [#'proto/ByteSource src options]
    (let [os (ByteArrayOutputStream.)]
      (transfer src os)
      (.toByteArray os)))))

; TODO DEPS
#_#?(:clj
(defnt ^longs bytes->longs
  ([^bytes? b]
    (let [longs-ct (-> b count (/ 8) num/ceil int)
          longs-f  (core/long-array longs-ct)
          ; Empty bytes are put at end
          buffer   (doto (ByteBuffer/allocate (* 8 longs-ct))
                         (.put b))]
      (doseq [i (range (count longs-f))] 
        (aset longs-f i (.getLong buffer (* i 8))))
      longs-f))))

#?(:clj
(defnt' copy
  ([^bytes? input ^bytes? output ^pinteger? length]
    (System/arraycopy input 0 output 0 length)
    output)))

; TODO fix type-hint-predicates of CLJS version
(def copy identity)

; #?(:cljs
; (defnt copy
;   ([^bytes? input ^bytes? output ^pinteger? length]
;     (reduce ; TODO implement with |dotimes|
;       (fn [_ i]
;         (aset output i (aget input i)))
;       nil
;       (range (.-length input))))))

#?(:clj
(defn array-list [& args]
  (reduce (fn [ret elem]
          (.add ^ArrayList ret elem)
          ret)
          (ArrayList.) args)))

#?(:clj
(defn reverse 
  {:attribution "mikera.cljutils.bytes"}
  (^"[B" [^"[B" bs]
    (let [n (alength bs)
          res (core/byte-array n)]
      (dotimes [i n]
        (aset! res i (aget bs (- n (inc i)))))
      res))))

; CANDIDATE 0
#?(:clj
(defn ^"[B" aconcat  ; join
  {:attribution "mikera.cljutils.bytes"}
  ([^"[B" a ^"[B" b]
    (let [al (int (alength a))
          bl (int (alength b))
          n  (int (+ al bl))
          ^"[B" res (core/byte-array n)]
      (System/arraycopy a (int 0) res (int 0) al)
      (System/arraycopy b (int 0) res (int al) bl)
      res))))

; CANDIDATE 1
#_(:clj
(defn- aconcat
  "Concatenates arrays of given type."
  [type & xs]
  (let [target (make-array type (apply + (map count xs)))]
    (loop [i 0 idx 0]
      (when-let [a (nth xs i nil)]
        (System/arraycopy a 0 target idx (count a))
        (recur (inc i) (+ idx (count a)))))
    target)))

#?(:clj
(defn slice
  "Slices a byte array with a given start and length"
  {:attribution "mikera.cljutils.bytes"}
  (^"[B" [a start]
    (slice a start (- (alength ^"[B" a) start)))
  (^"[B" [a start length]
    (let [al (int (alength ^"[B" a))
          ^"[B" res (core/byte-array length)]
      (System/arraycopy a (int start) res (int 0) length)
      res))))

#?(:clj
(defnt' ^boolean ==
  "Compares two byte arrays for equality."
  {:attribution "mikera.cljutils.bytes"}
  ([^array? a :first b]
    (java.util.Arrays/equals a b))))

#_(defn aprint [arr]
  (core/doseq [elem arr]
    (pr elem) (.append *out* \space))
  (.append *out* \newline)
  nil)

#?(:cljs
(defnt ->uint8-array
  ([^js/Uint8Array x] x)
  ([#{js/Int8Array array?} x] (js/Uint8Array. x))))

#?(:cljs
(defnt ->int8-array
  ([^js/Int8Array x] x)
  ([#{js/Uint8Array array?} x] (js/Int8Array. x))))


; TODO Compress
; Compresses a PersistentVector into a typed array to fit as closely together as possible

; Check if a sorted array contains the specified value
; boolean arrayContains(String[] sortedArray, ^String val) {
;     return Arrays.binarySearch(sortedArray, key) >= 0;
; }