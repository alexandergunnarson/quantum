(ns quantum.core.data.primitive
        (:refer-clojure :exclude
          [boolean? char? comparable? decimal? double? float? int? integer?])
        (:require
 #?(:cljs [com.gfredericks.goog.math.Integer :as int])
          [quantum.core.type                 :as t
            :refer [defnt]]
          [quantum.core.vars
            :refer [def-]])
#?(:clj (:import
          [java.nio ByteBuffer]
          [quantum.core Numeric Primitive])))

;; ===== Predicates ===== ;;

#?(:clj (def boolean? (t/isa? #?(:clj Boolean :cljs js/Boolean))))
#?(:clj (def byte?    (t/isa? Byte)))
#?(:clj (def short?   (t/isa? Short)))
#?(:clj (def char?    (t/isa? Character)))
#?(:clj (def int?     (t/isa? Integer)))
#?(:clj (def long?    (t/isa? Long)))
#?(:clj (def float?   (t/isa? Float)))
        (def double?  (t/isa? #?(:clj Double :cljs js/Number)))

        (def primitive? (t/or boolean? #?@(:clj [byte? short? char? int? long? float?]) double?))

        ;; Specifically primitive integers
        (def integer? (t/or #?@(:clj [byte? short? int? long?])))

        ;; Specifically primitive decimals
        (def decimal? (t/or #?(:clj float?) double?))

        ;; Specifically primitive integrals
        (def integral? (t/or integer? char?))

        ;; Specifically comparable primitives
        (def comparable? (t/- primitive? boolean?))

;; ===== Boxing/unboxing ===== ;;

#?(:clj
(def unboxed-class->boxed-class
  {Boolean/TYPE   Boolean
   Byte/TYPE      Byte
   Character/TYPE Character
   Long/TYPE      Long
   Double/TYPE    Double
   Short/TYPE     Short
   Integer/TYPE   Integer
   Float/TYPE     Float}))

#?(:clj
(def boxed-class->unboxed-class
  {Integer   Integer/TYPE
   Long      Long/TYPE
   Float     Float/TYPE
   Short     Short/TYPE
   Boolean   Boolean/TYPE
   Byte      Byte/TYPE
   Character Character/TYPE
   Double    Double/TYPE
   Void      Void/TYPE}))

#?(:clj
(defnt box
  ([x boolean? > (t/ref boolean?)] (Boolean/valueOf   x))
  ([x byte?    > (t/ref byte?)]    (Byte/valueOf      x))
  ([x char?    > (t/ref char?)]    (Character/valueOf x))
  ([x short?   > (t/ref short?)]   (Short/valueOf     x))
  ([x int?     > (t/ref int?)]     (Integer/valueOf   x))
  ([x long?    > (t/ref long?)]    (Long/valueOf      x))
  ([x float?   > (t/ref float?)]   (Float/valueOf     x))
  ([x double?  > (t/ref double?)]  (Double/valueOf    x))))

#?(:clj
(defnt unbox
  ([x (t/ref boolean?) > boolean?] (.booleanValue x))
  ([x (t/ref byte?)    > byte?]    (.byteValue    x))
  ([x (t/ref char?)    > char?]    (.charValue    x))
  ([x (t/ref short?)   > short?]   (.shortValue   x))
  ([x (t/ref int?)     > int?]     (.intValue     x))
  ([x (t/ref long?)    > long?]    (.longValue    x))
  ([x (t/ref float?)   > float?]   (.floatValue   x))
  ([x (t/ref double?)  > double?]  (.doubleValue  x))))

;; ===== Extreme magnitudes and values ===== ;;

(defnt ^:inline >min-magnitude
  #?(:clj ([x byte?   > byte?]            (byte  0)))
  #?(:clj ([x short?  > short?]           (short 0)))
  #?(:clj ([x char?   > char?]            (char  0)))
  #?(:clj ([x int?    > int?]             (int   0)))
  #?(:clj ([x long?   > long?]            (long  0)))
  #?(:clj ([x float?  > float?]           Float/MIN_VALUE))
          ([x double? > double?] #?(:clj  Double/MIN_VALUE
                                    :cljs js/Number.MIN_VALUE)))

#?(:clj (def- min-float  (- Float/MAX_VALUE)))
        (def- min-double (- #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

;; TODO TYPED for some reason it's not figuring out the type of `min-float` and `min-double`
#_(defnt ^:inline >min-value
  #?(:clj ([x byte?   > byte?]   Byte/MIN_VALUE))
  #?(:clj ([x short?  > short?]  Short/MIN_VALUE))
  #?(:clj ([x char?   > char?]   Character/MIN_VALUE))
  #?(:clj ([x int?    > int?]    Integer/MIN_VALUE))
  #?(:clj ([x long?   > long?]   Long/MIN_VALUE))
  #?(:clj ([x float?  > float?]  min-float))
          ([x double? > double?] min-double))

(defnt ^:inline >max-value
  #?@(:clj [([x byte?   > byte?]           Byte/MAX_VALUE)
            ([x short?  > short?]          Short/MAX_VALUE)
            ([x char?   > char?]           Character/MAX_VALUE)
            ([x int?    > int?]            Integer/MAX_VALUE)
            ([x long?   > long?]           Long/MAX_VALUE)
            ([x float?  > float?]          Float/MAX_VALUE)])
            ([x double? > double?] #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

;; ===== Primitive type properties ===== ;;

(defnt ^:inline signed?
          ([x (t/or char?    (t/value Character))]             false)
#?@(:clj [([x (t/or byte?    (t/value Byte)
                    short?   (t/value Short)
                    int?     (t/value Integer)
                    long?    (t/value Long)
                    float?   (t/value Float)
                    double?  #?(:clj Double :cljs js/Number))] true)))

;; TODO TYPED `t/numerically-integer?`
(defnt ^:inline >bit-size ; > t/numerically-integer?
          ([x (t/or boolean? (t/value #?(:clj Boolean :cljs js/Boolean)))] 1) ; kind of
#?@(:clj [([x (t/or byte?    (t/value Byte))]                              8)
          ([x (t/or short?   (t/value Short))]                             16)
          ([x (t/or char?    (t/value Character))]                         16)
          ([x (t/or int?     (t/value Integer))]                           32)
          ([x (t/or long?    (t/value Long))]                              64)
          ([x (t/or float?   (t/value Float))]                             32)])
          ([x (t/or double?  #?(:clj Double :cljs js/Number))]             64))

;; ===== Conversion ===== ;;

(def radix? (fnt [x integer?]
              (<= #?(:clj Character/MIN_RADIX :cljs 2) x #?(:clj Character/MAX_RADIX :cljs 36)))

;; ----- Boolean ----- ;;

(defnt ^:inline >boolean > boolean?
  ([x boolean?] x)
  ([x (t/value "true")] true)
  ([x (t/value "false")] false)   ;; For purposes of intrinsics
  ([x (t/or long? double?)] (-> x clojure.lang.Numbers/isZero Numeric/not))
  ([x (t/- primitive? boolean? long? double?)] (-> x Numeric/isZero Numeric/not)))

;; ----- Int ----- ;;
;; Forward-declared so `radix?` coercion to `int` works

#?(:clj
(defnt ^:inline >int* > int?
  "May involve non-out-of-range truncation"
  ([x int?] x)                        ;; For purposes of intrinsics
  ([x (t/- primitive? int? boolean?)] (clojure.lang.RT/uncheckedIntCast x))))

(defnt ^:inline >int > #?(:clj int? :cljs numerically-int?)
  "May involve non-out-of-range truncation"
       ([x #?(:clj int? :cljs numerically-int?)] x)
#?(:clj  ([x (t/and (t/- primitive? int? boolean?) (range-of int?))] (>int* x))
   :cljs ([x (t/and double? (range-of int?))] (js/Math.round x)))
         ([x boolean?] (if x #?(:clj (int 1) :cljs 1) #?(:clj (int 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) (range-of int?))] (>int* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) (range-of int?))] (.intValue x)))
#?(:clj  ([x (t/and dnum/ratio? (range-of int?))] (-> x .bigIntegerValue .intValue)))
         ([x string?]
           #?(:clj  (Integer/parseInteger x)
                     ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x}))))
         ([x string?, radix radix?]
           #?(:clj  (Integer/parseInteger x (>int radix))
                    ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x})))))

; js/Math.trunc for CLJS

;; ----- Byte ----- ;;

#?(:clj
(defnt ^:inline >byte* > byte?
  "May involve non-out-of-range truncation"
  ([x byte?] x)
  ([x (t/- primitive? byte? boolean?)] (Primitive/uncheckedByteCast x))))

(defnt ^:inline >byte > #?(:clj byte? :cljs numerically-byte?)
  "May involve non-out-of-range truncation"
         ([x #?(:clj byte? :cljs numerically-byte?)] x)
#?(:clj  ([x (t/and (t/- primitive? byte? boolean?) (range-of byte?))] (>byte* x))
   :cljs ([x (t/and double? (range-of byte?))] (js/Math.round x)))
         ([x boolean?] (if x #?(:clj (byte 1) :cljs 1) #?(:clj (byte 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) (range-of byte?))] (>byte* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) (range-of byte?))] (.byteValue x)))
#?(:clj  ([x (t/and dnum/ratio? (range-of byte?))] (-> x .bigIntegerValue .byteValue)))
         ([x string?]
           #?(:clj  (Byte/parseByte x)
                     ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x}))))
         ([x string?, radix radix?]
           #?(:clj  (Byte/parseByte x (>int radix))
                    ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x})))))

;; ----- Short ----- ;;

#?(:clj
(defnt ^:inline >short* > short?
  "May involve non-out-of-range truncation"
  ([x short?] x)
  ([x (t/- primitive? short? boolean?)] (Primitive/uncheckedShortCast x))))

#?(:clj
(defnt ^:inline >short > #?(:clj short? :cljs numerically-short?)
  "May involve non-out-of-range truncation"
         ([x #?(:clj short? :cljs numerically-short?)] x)
#?(:clj  ([x (t/and (t/- primitive? short? boolean?) (range-of short?))] (>short* x))
   :cljs ([x (t/and double? (range-of short?))] (js/Math.round x)))
         ([x boolean?] (if x #?(:clj (short 1) :cljs 1) #?(:clj (short 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) (range-of short?))] (>short* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) (range-of short?))] (.shortValue x)))
#?(:clj  ([x (t/and dnum/ratio? (range-of short?))] (-> x .bigIntegerValue .shortValue)))
         ([x string?]
           #?(:clj  (Short/parseShort x)
                     ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x}))))
         ([x string?, radix radix?]
           #?(:clj  (Short/parseShort x (>int radix))
                    ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x}))))))

;; ----- Char ----- ;;

#?(:clj
(defnt ^:inline >char* > char?
  "May involve non-out-of-range truncation"
  ([x char?] x)
  ([x (t/- primitive? char? boolean?)] (Primitive/uncheckedCharCast x))))

(defnt ^:inline >char > #?(:clj char? :cljs numerically-char?)
  "May involve non-out-of-range truncation.
   For CLJS, returns not a String of length 1 but a numerically-char Number."
         ([x #?(:clj char? :cljs numerically-char?)] x)
#?(:clj  ([x (t/and (t/- primitive? char? boolean?) (range-of char?))] (>char* x))
   :cljs ([x (t/and double? (range-of char?))] (js/Math.round x)))
         ([x boolean?] (if x #?(:clj (char 1) :cljs 1) #?(:clj (char 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) (range-of char?))] (>char* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) (range-of char?))] (.charValue x)))
#?(:clj  ([x (t/and dnum/ratio? (range-of char?))] (-> x .bigIntegerValue .charValue))))

;; ----- Long ----- ;;

#?(:clj
(defnt ^:inline >long* > long?
  "May involve non-out-of-range truncation"
  ([x long?] x)                        ;; For purposes of intrinsics
  ([x (t/- primitive? long? boolean?)] (clojure.lang.RT/uncheckedLongCast x))))

(defnt ^:inline >long > #?(:clj long? :cljs numerically-long?)
  "May involve non-out-of-range truncation"
         ([x #?(:clj long? :cljs numerically-long?)] x)
#?(:clj  ([x (t/and (t/- primitive? long? boolean?) (range-of long?))] (>long* x))
   :cljs ([x double?] (js/Math.round x)))
         ([x boolean?] (if x 1 0))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt)
                    (range-of long?)
                    ;; This might be faster than `(range-of long?)`
                  #_(fnt [x ?] (nil? (.bipart x))))] (.lpart x)))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger)
                    (range-of long?)
                    ;; This might be faster than `(range-of long?)`
                  #_(fnt [x ?] (< (.bitLength x) 64)))] (.longValue x)))
#?(:clj  ([x (t/and dnum/ratio? (range-of long?))] (-> x .bigIntegerValue .longValue)))
         ([x string?]
           #?(:clj  (Long/parseLong x)
                     ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x}))))
         ([x string?, radix radix?]
           #?(:clj  (Long/parseLong x (>int radix))
                    ;; NOTE could use `js/parseInt` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x})))))

;; ----- Float ----- ;;

#?(:clj
(defnt ^:inline >float* > float?
  "May involve non-out-of-range truncation"
  ([x float?] x)
  ([x (t/- primitive? float? boolean?)] (Primitive/uncheckedFloatCast x))))

(defnt ^:inline >float > #?(:clj float? :cljs numerically-float?)
  "May involve non-out-of-range truncation"
         ([x #?(:clj float? :cljs numerically-float?)] x)
#?(:clj  ([x (t/and (t/- primitive? float? boolean?) (range-of float?))] (>float* x))
   :cljs ([x (t/and double? (range-of float?)) > (t/assume numerically-float?)] (js.Math/fround x)))
         ([x boolean?] (if x #?(:clj (float 1) :cljs 1) #?(:clj (float 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) (range-of float?))] (>float* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) (range-of float?))] (.floatValue x)))
#?(:clj  ([x (t/and dnum/ratio? (range-of float?))] (-> x .bigIntegerValue .floatValue)))
         ([x string?]
           #?(:clj  (Float/parseFloat x)
                     ;; NOTE could use `js/parseFloat` but it's very 'unsafe'
              :cljs (throw (ex-info "Parsing not implemented" {:string x})))))

;; ----- Double ----- ;;

#?(:clj
(defnt ^:inline >double* > double?
  "May involve non-out-of-range truncation"
  ([x double?] x)                        ;; For purposes of intrinsics
  ([x (t/- primitive? double? boolean?)] (clojure.lang.RT/uncheckedDoubleCast x))))


(defnt ^:inline >double > double?
  "May involve non-out-of-range truncation"
        ([x double?] x)
#?(:clj ([x (t/and (t/- primitive? double? boolean?) (range-of double?))] (>double* x)))
        ([x boolean?] (if x #?(:clj (double 1) :cljs 1) #?(:clj (double 0) :cljs 0)))
#?(:clj ([x (t/and (t/isa? clojure.lang.BigInt) (range-of double?))] (>double* (.lpart x))))
#?(:clj ([x (t/and (t/isa? java.math.BigInteger) (range-of double?))] (.doubleValue x)))
#?(:clj ([x (t/and dnum/ratio? (range-of double?))] (-> x .bigIntegerValue .doubleValue)))
        ([x string?]
          #?(:clj  (Double/parseDouble x)
                   ;; NOTE could use `js/parseFloat` but it's very 'unsafe'
             :cljs (throw (ex-info "Parsing not implemented" {:string x}))))))

;; ===== Unsigned ===== ;;

#?(:clj
(defnt >unsigned
  {:adapted-from #{'ztellman/primitive-math 'gloss.data.primitives}}
  ([x byte?]  (Numeric/bitAnd (short 0xFF)       x))
  ([x short?] (Numeric/bitAnd (int   0xFFFF)     x))
  ([x int?]   (Numeric/bitAnd (long  0xFFFFFFFF) x))
  ([x long?]  (BigInteger. 1 (-> (ByteBuffer/allocate 8) (.putLong x) .array))))) ; TODO reflection

#?(:clj (defnt ubyte>byte   [x long?   > long?] (>long (>byte   x))))
#?(:clj (defnt ushort>short [x long?   > long?] (>long (>short  x))))
#?(:clj (defnt uint>int     [x long?   > long?] (>long (>int    x))))
#?(:clj (defnt ulong>long   [x bigint? > long?] (>long (>bigint x))))
