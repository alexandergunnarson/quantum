(ns
  ^{:doc "Useful array functions. Array creation, joining, reversal, etc."
    :attribution "Alex Gunnarson"
    :todo ["Incorporate amap, areduce, aset-char, aset-boolean, etc."]}
  quantum.core.data.array
  (:refer-clojure :exclude
    [== reverse boolean-array byte-array char-array short-array
     int-array long-array float-array double-array
     empty count get doseq])
           (:require [clojure.core                  :as core]
             #?(:clj [loom.alg-generic              :as alg]) ; temporarily
                     [quantum.core.collections.base :as cbase
                       :refer [reducei]]
                     [quantum.core.collections.core :as ccoll
                       :refer [empty count get aset!]]
                     [quantum.core.type.core        :as tcore]
                     [quantum.core.core             :as qcore
                       :refer [name+]]
                     [quantum.core.fn               :as fn
                       :refer [<- fn->]]
                     [quantum.core.log              :as log]
                     [quantum.core.logic            :as logic
                       :refer [whenc]]
                     [quantum.core.error
                       :refer [TODO]]
                     [quantum.core.loops            :as loops
                       :refer [doseqi doseq]]
                     [quantum.core.macros           :as macros
                       :refer [defnt defnt']]
                     [quantum.core.compare :as comp]
                     [quantum.core.numeric :as num]
                     [quantum.core.type
                       :refer [static-cast]]
                     [quantum.core.vars             :as var
                       :refer [defalias]])
  #?(:cljs (:require-macros
                     [quantum.core.data.array       :as self]))
  #?(:clj  (:import  [java.io File FileInputStream BufferedInputStream InputStream ByteArrayOutputStream]
                     [java.nio ByteBuffer]
                     java.util.ArrayList)))

(log/this-ns)

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
          n-sym (-> 'n gensym (macros/hint-meta 'nat-int?))]
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
    ([] (ccoll/->object-array 0))
    ([a]
      (let [arr (ccoll/->object-array 1)]
        (aset! arr 0 a)
        arr))
    ([a b]
      (let [arr (ccoll/->object-array 2)]
        (aset! arr 0 a)
        (aset! arr 1 b)
        arr))
    ([a b c]
      (let [arr (ccoll/->object-array 3)]
        (aset! arr 0 a)
        (aset! arr 1 b)
        (aset! arr 2 c)
        arr))
    ([a b c d]
      (let [arr (ccoll/->object-array 4)]
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
(defmacro array [type n] ; TODO move
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
  {:attribution  ["ztellman/byte-streams" "funcool/octet" "gloss.data.primitives"]
   :contributors {"Alex Gunnarson" "defnt-ed"}
   :todo         {0 "make encoding more rigorous via (Charset/defaultCharset)"}}
  (^{:cost 0} [^bytes? x] x)
  ; "gloss.data.primitives"
  (           [^long?  x] (-> (ByteBuffer/allocate 8) (.putLong x) .array))
  (^{:cost 2} [^String s         ] (->bytes s nil))
  (^{:cost 2} [^String s encoding]
    #?(:clj (let [^String encoding-f (whenc encoding (fn-> name+ nil?) "UTF-8")] ; TODO 0
              (.getBytes s encoding-f))
       ; funcool/octet.spec.string
       :cljs (let [buff (js/ArrayBuffer. (count s))
                   view (js/Uint8Array. buff)]
               (dotimes [i (count s)]
                 (aset view i (.charCodeAt value i)))
               (js/Int8Array. buff))))
  (^{:cost 1} [^java.nio.ByteBuffer buf]
    (if (.hasArray buf)
        (if (comp/= (int (count (.array buf))) (.remaining buf))
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
  (^{:cost 1.5} [^InputStream         in] (-> in (BufferedInputStream.) ->bytes))
  (^{:cost 1.5} [^BufferedInputStream in]
    (let [out (ByteArrayOutputStream. (comp/max 64 (.available in)))
          buf (byte-array 16384)]
      (loop []
        (let [len (.read in buf 0 16384)]
          (when-not (neg? len)
            (.write out buf 0 len)
            (recur))))
      (.toByteArray out)))
  ([^File x]
    (let [in (FileInputStream. x)]
      (->bytes (BufferedInputStream. in))))
  #_(^{:cost 2} [#'proto/ByteSource src options]
    (let [os (ByteArrayOutputStream.)]
      (transfer src os)
      (.toByteArray os)))))

#?(:clj
(defnt ^"[J" bytes->longs
  ([^bytes? b]
    (let [longs-ct (-> b count (/ 8) num/ceil int)
          longs-f  (ccoll/->long-array longs-ct)
          ; Empty bytes are put at end
          buffer   (doto (ByteBuffer/allocate (* 8 longs-ct))
                         (.put b))]
      (doseq [i (range (count longs-f))]
        (aset! longs-f i (.getLong buffer (* i 8))))
      longs-f))))

; ===== COPY ===== ;

(#?(:clj defnt' :cljs defnt) copy! ; shallow copy
  (^first [^array? in ^int? in-pos :first out ^int? out-pos ^int? length]
    #?(:clj  (System/arraycopy in in-pos out out-pos length)
       :cljs (dotimes [i (- (.-length in) in-pos)]
               (aset out (+ i out-pos) (aget in i))))
    out)
  (^first [^array? in :first out ^nat-int? length]
    (copy! in 0 out 0 length)))

#?(:clj (defalias shallow-copy! copy!))

(defn deep-copy! [in out length] (TODO))

(defnt copy (^first [^array? in] #?(:clj (copy! in (empty in) (count in)) :cljs (.slice in))))

; ===== EDIT ===== ;

(defnt reverse
  {:adapted-from "mikera.cljutils.bytes"}
  (^first [^array? x]
    (let [n   (count x)
          ret (empty x)]
      (dotimes [i n] (aset! ret i (get x (- n (inc i)))))
      ret)))

#?(:cljs (defnt reverse! (^first [^array? x] (.reverse x))))

(defnt aconcat  ; TODO join
  {:adapted-from "mikera.cljutils.bytes"
   :todo #{"cljs, probably use .concat"}}
  (^first [^array? a :first b]
    (let [al  (count a)
          bl  (count b)
          n   (+ al bl)
          ret (ccoll/array-of-type a (int n))]
      (copy! a 0 ret 0  al)
      (copy! b 0 ret al bl)
      ret)))

(defnt slice
  "Slices an array with a given start and length"
  {:adapted-from "mikera.cljutils.bytes"
   :todo #{"cljs, probably use .slice"}}
  (^first [^array? a ^int? start]
    (slice a start (- (count a) (int start))))
  (^first [^array? a ^int? start ^int? n]
    (let [al  (count a)
          ret (ccoll/array-of-type a (int n))]
      (copy! a start ret 0 n)
      ret)))

#?(:clj
(defnt' ^boolean ==
  "Compares two arrays for equality."
  {:adapted-from "mikera.cljutils.bytes"}
  ([^array? a :first b]
    (java.util.Arrays/equals a b))))

; ===== GENERATION ===== ;

#?(:clj ; TODO move ; TODO CLJS
(defn array-list [& args]
  (reduce (fn [ret elem] (.add ^ArrayList ret elem) ret)
          (ArrayList.) args)))


; TODO Compress
; Compresses a PersistentVector into a typed array to fit as closely together as possible

; Check if a sorted array contains the specified value
; boolean arrayContains(String[] sortedArray, ^String val) {
;     return Arrays.binarySearch(sortedArray, key) >= 0;
; }
