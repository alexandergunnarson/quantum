(ns
  ^{:doc "Useful array functions. Array creation, joining, reversal, etc."
    :attribution "alexandergunnarson"
    :todo ["Incorporate amap, areduce, etc."]}
  quantum.core.data.array
  (:refer-clojure :exclude
    [== reverse boolean-array byte-array char-array short-array
     int-array long-array float-array double-array
     empty count get doseq assoc!])
  (:require
    [clojure.core                  :as core]
#?(:clj
    [loom.alg-generic              :as alg]) ; temporarily
    [quantum.core.collections.base :as cbase
      :refer [reducei]]
    [quantum.core.collections.core :as ccoll
      :refer [empty count get assoc!]]
    [quantum.core.type.core        :as tcore]
    [quantum.core.core             :as qcore
      :refer [name+]]
    [quantum.core.fn               :as fn
      :refer [<- fn->]]
    [quantum.core.log              :as log]
    [quantum.core.logic            :as logic
      :refer [whenc whenc->]]
    [quantum.core.error
      :refer [TODO]]
    [quantum.core.loops            :as loops
      :refer [doseqi doseq]]
    [quantum.core.macros           :as macros
      :refer [defnt defnt']]
    [quantum.core.macros.type-hint :as th]
    [quantum.core.compare          :as comp]
    [quantum.core.numeric          :as num]
    [quantum.core.type
      :refer [static-cast]]
    [quantum.core.vars             :as var
      :refer [defalias]])
#?(:cljs
  (:require-macros
    [quantum.core.data.array       :as self]))
#?(:clj
  (:import
    [java.io File FileInputStream BufferedInputStream InputStream ByteArrayOutputStream]
    [java.nio ByteBuffer]
    java.util.ArrayList)))

(log/this-ns)

; TODO look at http://fastutil.di.unimi.it to complete this namespace
; TODO `fill!` <~> `Arrays/fill`, `lodash/fill`
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
          fn-sym (-> arr-sym (th/with-type-hint (get tcore/type-casts-map arr-sym)))
          core-sym (symbol "clojure.core" (name fn-sym))
          n-sym (-> 'n gensym (th/with-type-hint 'nat-int?))]
      `(defnt ~fn-sym ([~n-sym] (core-sym ~n-sym)))))))

; ----- BOOLEAN ARRAY ----- ;

#?(:clj
(defnt boolean-array [^int n]
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
    {:attribution "alexandergunnarson"
     :todo ["Make less repetitive via macro"]}
    ([size]
      (byte-array (long size)))
    ([size & args]
      (let [^"[B" arr (byte-array (long size))]
        (doseqi [arg args n]
          (assoc! arr n (-> arg first byte)))
        arr))))

; ----- INT ARRAY ----- ;

#?(:clj
  (defn ^ints int-array+
    "Like /int-array/ but allows for array initializers a la Java:
     int[] arr = int[]{12, 8, 10}"
    {:attribution "alexandergunnarson"
     :todo ["Make less repetitive via macro"]}
    ([size]
      (core/int-array (long size)))
    ([size & args]
      (let [^ints arr (core/int-array (long size))]
        (doseqi [arg args n]
          (assoc! arr (long n) (-> arg first int)))
        arr))))

; TODO: Use a macro for this
#?(:clj
  (defn long-array-of
    "Creates a long array with the specified values."
    {:attribution "mikera.cljutils.arrays"}
    (^longs [] (core/long-array 0))
    (^longs [a]
      (let [arr (core/long-array 1)]
        (assoc! arr 0 (long a))
        arr))
    ([a b]
      (let [arr (core/long-array 2)]
        (assoc! arr 0 (long a))
        (assoc! arr 1 (long b))
        arr))
    ; ([a b & more]
    ;   (let [arr (long-array (+ 2 (count more)))]
    ;     (assoc! arr 0 (long a))
    ;     (assoc! arr 1 (long b))
    ;     (doseqi [x more i] (assoc! arr (+ 2 i) (long x)))
    ;     arr))
    ))

; ----- OBJECT ARRAY ----- ;

; TODO: Use a macro for this
#_(:clj
  (defn object-array-of
    "Creates an object array with the specified values."
    {:attribution "mikera.cljutils.arrays"}
    ([] (ccoll/->object-array 0))
    ([a]
      (let [arr (ccoll/->object-array 1)]
        (assoc! arr 0 a)
        arr))
    ([a b]
      (let [arr (ccoll/->object-array 2)]
        (assoc! arr 0 a)
        (assoc! arr 1 b)
        arr))
    ([a b c]
      (let [arr (ccoll/->object-array 3)]
        (assoc! arr 0 a)
        (assoc! arr 1 b)
        (assoc! arr 2 c)
        arr))
    ([a b c d]
      (let [arr (ccoll/->object-array 4)]
        (assoc! arr 0 a)
        (assoc! arr 1 b)
        (assoc! arr 2 c)
        (assoc! arr 3 d)
        arr))
    ; ([a b & more]
    ;   (let [arr (object-array (+ 2 (count more)))]
    ;     (assoc! arr 0 a)
    ;     (assoc! arr 1 b)
    ;     (doseqi [x more i] (assoc! arr (+ 2 i) x))
    ;     arr))
    ))

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
    #?(:clj (let [^String encoding-f (or (some-> encoding name) "UTF-8")] ; TODO 0
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
          longs-f  (ccoll/->longs longs-ct)
          ; Empty bytes are put at end
          buffer   (doto (ByteBuffer/allocate (* 8 longs-ct))
                         (.put b))]
      (doseq [i (range (count longs-f))]
        (assoc! longs-f i (.getLong buffer (* i 8))))
      longs-f))))

#?(:clj
(defnt' ^boolean ==
  "Compares two arrays for equality."
  {:adapted-from "mikera.cljutils.bytes"}
  ([^array? a :<0> b]
    (java.util.Arrays/equals a b))))

(defnt swap-at! [#{array? !array-list?} x ^int i ^int j]
  (let [tmp (get x i)]
    (assoc! x i (get x j))
    (assoc! x j tmp)))


; TODO Compress
; Compresses a PersistentVector into a typed array to fit as closely together as possible

; Check if a sorted array contains the specified value
; boolean arrayContains(String[] sortedArray, ^String val) {
;     return Arrays.binarySearch(sortedArray, key) >= 0;
; }
