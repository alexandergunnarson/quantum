(ns quantum.core.data.primitive
        (:refer-clojure :exclude
          [boolean? char? comparable? decimal? double? false? float? int? integer? true?])
        (:require
 #?(:cljs [com.gfredericks.goog.math.Integer :as int])
 #?(:cljs goog.math.Integer)
 #?(:cljs goog.math.Long)
          [quantum.core.type                 :as t]
          [quantum.untyped.core.type         :as ut]
          ;; TODO TYPED excise reference
          [quantum.untyped.core.vars         :as var
            :refer [defaliases]])
#?(:clj (:import
          [java.nio ByteBuffer]
          [quantum.core Numeric Primitive])))

(def nil? ut/nil?)
(def val? ut/val?)

;; ===== Predicates ===== ;;

#?(:clj (def boolean? (t/isa? #?(:clj Boolean :cljs js/Boolean))))

#?(:clj (def byte?    (t/isa? Byte)))

#?(:clj (def short?   (t/isa? Short)))

#?(:clj (def char?    (t/isa? Character)))

        (var/def int?
          "For CLJS, `int?` is not primitive even though it mimics the boxed version of the Java
           `int` primitive. It is included in this namespace merely for cohesion."
          (t/isa? #?(:clj Integer :cljs goog.math.Integer)))

        (var/def long?
          "For CLJS, `long?` is not primitive even though it mimics the boxed version of the Java
           `long` primitive. It is included in this namespace merely for cohesion."
          (t/isa? #?(:clj Long :cljs goog.math.Long)))

#?(:clj (def float?   (t/isa? Float)))

        (def double?  (t/isa? #?(:clj Double :cljs js/Number)))

        (var/def primitive?
          "For CLJS, `int?` and `long?` are not primitive even though they mimic Java primitives.
           For CLJS, does not include built-in platform types like js/String that are considered
           'primitive' in some contexts."
          (t/or boolean? #?@(:clj [byte? short? char? int? long? float?]) double?))

        (var/def integer? "Specifically primitive integers."
          (t/or #?@(:clj [byte? short? int? long?])))

        (var/def decimal? "Specifically primitive decimals."
          (t/or #?(:clj float?) double?))

        (var/def numeric?
          "Specifically primitive numeric things.
           Something 'numeric' is something that may be treated as a number but may not actually *be* one."
          (t/- primitive? boolean?))

        (defaliases ut true? false?)

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
(t/defn ^:inline box
  ([x boolean? > (t/assume (t/ref boolean?))] (Boolean/valueOf   x))
  ([x byte?    > (t/assume (t/ref byte?))]    (Byte/valueOf      x))
  ([x char?    > (t/assume (t/ref char?))]    (Character/valueOf x))
  ([x short?   > (t/assume (t/ref short?))]   (Short/valueOf     x))
  ([x int?     > (t/assume (t/ref int?))]     (Integer/valueOf   x))
  ([x long?    > (t/assume (t/ref long?))]    (Long/valueOf      x))
  ([x float?   > (t/assume (t/ref float?))]   (Float/valueOf     x))
  ([x double?  > (t/assume (t/ref double?))]  (Double/valueOf    x))
  ([x t/ref?] x)))

#?(:clj
(t/defn ^:inline unbox
  ([x (t/ref boolean?) > boolean?] (.booleanValue x))
  ([x (t/ref byte?)    > byte?]    (.byteValue    x))
  ([x (t/ref char?)    > char?]    (.charValue    x))
  ([x (t/ref short?)   > short?]   (.shortValue   x))
  ([x (t/ref int?)     > int?]     (.intValue     x))
  ([x (t/ref long?)    > long?]    (.longValue    x))
  ([x (t/ref float?)   > float?]   (.floatValue   x))
  ([x (t/ref double?)  > double?]  (.doubleValue  x))))

;; ===== Extreme magnitudes and values ===== ;;

(t/defn ^:inline >min-magnitude
  #?(:clj ([x byte?   > byte?]            (byte  0)))
  #?(:clj ([x short?  > short?]           (short 0)))
  #?(:clj ([x char?   > char?]            (char  0)))
  #?(:clj ([x int?    > int?]             (int   0)))
  #?(:clj ([x long?   > long?]            (long  0)))
  #?(:clj ([x float?  > float?]           Float/MIN_VALUE))
          ([x double? > double?] #?(:clj  Double/MIN_VALUE
                                    :cljs js/Number.MIN_VALUE)))

#?(:clj (def ^:private min-float  (- Float/MAX_VALUE)))
        (def ^:private min-double (- #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

;; TODO TYPED for some reason it's not figuring out the type of `min-float` and `min-double`
#_(t/defn ^:inline >min-value
  #?(:clj ([x byte?   > byte?]   Byte/MIN_VALUE))
  #?(:clj ([x short?  > short?]  Short/MIN_VALUE))
  #?(:clj ([x char?   > char?]   Character/MIN_VALUE))
  #?(:clj ([x int?    > int?]    Integer/MIN_VALUE))
  #?(:clj ([x long?   > long?]   Long/MIN_VALUE))
  #?(:clj ([x float?  > float?]  min-float))
          ([x double? > double?] min-double))

(t/defn ^:inline >max-value
  #?@(:clj [([x byte?   > byte?]           Byte/MAX_VALUE)
            ([x short?  > short?]          Short/MAX_VALUE)
            ([x char?   > char?]           Character/MAX_VALUE)
            ([x int?    > int?]            Integer/MAX_VALUE)
            ([x long?   > long?]           Long/MAX_VALUE)
            ([x float?  > float?]          Float/MAX_VALUE)])
            ([x double? > double?] #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

;; ===== Primitive type properties ===== ;;

(t/defn ^:inline signed?
          ([x (t/or char?    (t/value Character))]             false)
#?@(:clj [([x (t/or byte?    (t/value Byte)
                    short?   (t/value Short)
                    int?     (t/value Integer)
                    long?    (t/value Long)
                    float?   (t/value Float)
                    double?  #?(:clj Double :cljs js/Number))] true)]))

;; TODO TYPED `t/numerically-integer?`
(t/defn ^:inline >bit-size ; > t/numerically-integer?
          ([x (t/or boolean? (t/value #?(:clj Boolean :cljs js/Boolean)))] 1) ; kind of
#?@(:clj [([x (t/or byte?    (t/value Byte))]                              8)
          ([x (t/or short?   (t/value Short))]                             16)
          ([x (t/or char?    (t/value Character))]                         16)
          ([x (t/or int?     (t/value Integer))]                           32)
          ([x (t/or long?    (t/value Long))]                              64)
          ([x (t/or float?   (t/value Float))]                             32)])
          ([x (t/or double?  #?(:clj Double :cljs js/Number))]             64))

;; ===== Conversion ===== ;;

;; ----- Boolean ----- ;;

(t/defn ^:inline >boolean
  "Converts input to a boolean.
   Differs from asking whether something is truthy/falsey."
  > boolean?
  ([x boolean?] x)          ;; For purposes of Clojure intrinsics
  ([x (t/or long? double?)] (-> x clojure.lang.Numbers/isZero Numeric/not))
  ([x (t/- primitive? boolean? long? double?)] (-> x Numeric/isZero Numeric/not)))

;; ----- Byte ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >byte*
  "May involve non-out-of-range truncation."
  > byte?
  ([x byte?] x)
  ([x (t/- primitive? byte? boolean?)] (Primitive/uncheckedByteCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(defnt ^:inline >byte > #?(:clj byte? :cljs numerically-byte?)
  "Does not involve truncation or rounding."
         ([x #?(:clj byte? :cljs numerically-byte?)] x)
#?(:clj  ([x (t/and (t/- primitive? byte? boolean?) numerically-byte?)] (>byte* x))
   :cljs ([x (t/and double? numerically-byte?)] x))
         ([x boolean?] (if x #?(:clj (byte 1) :cljs 1) #?(:clj (byte 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-byte?)] (>byte* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-byte?)] (.byteValue x)))
#?(:clj  ([x (t/and dnum/ratio? numerically-byte?)] (-> x .bigIntegerValue .byteValue))))

;; ----- Short ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >short*
  "May involve non-out-of-range truncation."
  > short?
  ([x short?] x)
  ([x (t/- primitive? short? boolean?)] (Primitive/uncheckedShortCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >short > #?(:clj short? :cljs numerically-short?)
  "Does not involve truncation or rounding."
         ([x #?(:clj short? :cljs numerically-short?)] x)
#?(:clj  ([x (t/and (t/- primitive? short? boolean?) numerically-short?)] (>short* x))
   :cljs ([x (t/and double? numerically-short?)] x))
         ([x boolean?] (if x #?(:clj (short 1) :cljs 1) #?(:clj (short 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-short?)] (>short* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-short?)] (.shortValue x)))
#?(:clj  ([x (t/and dnum/ratio? numerically-short?)] (-> x .bigIntegerValue .shortValue))))

;; ----- Char ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >char*
  "May involve non-out-of-range truncation."
  > char?
  ([x char?] x)
  ([x (t/- primitive? char? boolean?)] (Primitive/uncheckedCharCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >char > #?(:clj char? :cljs numerically-char?)
  "Does not involve truncation or rounding.
   For CLJS, returns not a String of length 1 but a numerically-char Number."
         ([x #?(:clj char? :cljs numerically-char?)] x)
#?(:clj  ([x (t/and (t/- primitive? char? boolean?) numerically-char?)] (>char* x))
   :cljs ([x (t/and double? numerically-char?)] x))
         ([x boolean?] (if x #?(:clj (char 1) :cljs 1) #?(:clj (char 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-char?)] (>char* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-char?)] (.charValue x)))
#?(:clj  ([x (t/and dnum/ratio? numerically-char?)] (-> x .bigIntegerValue .charValue))))

;; ----- Int ----- ;;

;; TODO figure out how to use with goog.math.Integer/Long
#?(:clj
(t/defn ^:inline >int*
  "May involve non-out-of-range truncation."
  > int?
  ([x int?] x)                        ;; For purposes of Clojure intrinsics
  ([x (t/- primitive? int? boolean?)] (clojure.lang.RT/uncheckedIntCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >int
  "Does not involve truncation or rounding."
  > int?
         ([x int?] x)
#?(:clj  ([x (t/and (t/- primitive? int? boolean?) numerically-int?)] (>int* x))
   :cljs ([x (t/and double? numerically-int?)] x))
         ([x boolean?] (if x #?(:clj (int 1) :cljs 1) #?(:clj (int 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-int?)] (>int* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-int?)] (.intValue x)))
#?(:clj  ([x (t/and dnum/ratio? numerically-int?)] (-> x .bigIntegerValue .intValue))))

;; ----- Long ----- ;;

;; TODO figure out how to use with CLJS, including goog.math.Integer/Long
#?(:clj
(t/defn ^:inline >long*
  "May involve non-out-of-range truncation."
  > long?
  ([x long?] x)
  ([x char?] (Primitive/uncheckedLongCast x))  ;; For purposes of Clojure intrinsics
  ([x (t/- primitive? long? boolean? char?)] (clojure.lang.RT/uncheckedLongCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >long
  "Does not involve truncation or rounding."
  > #?(:clj long? :cljs numerically-long?)
         ([x #?(:clj long? :cljs numerically-long?)] x)
#?(:clj  ([x (t/and (t/- primitive? long? boolean?) numerically-long?)] (>long* x))
   :cljs ([x (t/and double? numerically-long?)] x))
         ([x boolean?] (if x 1 0))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt)
                    numerically-long?
                    ;; TODO This might be faster than `numerically-long?`
                  #_(fnt [x ?] (nil? (.bipart x))))] (.lpart x)))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger)
                    numerically-long?
                    ;; TODO This might be faster than `numerically-long?`
                  #_(fnt [x ?] (< (.bitLength x) 64)))] (.longValue x)))
#?(:clj  ([x (t/and dnum/ratio? numerically-long?)] (-> x .bigIntegerValue .longValue))))

;; ----- Float ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >float*
  "May involve non-out-of-range truncation."
  > float?
  ([x float?] x)
  ([x (t/- primitive? float? boolean?)] (Primitive/uncheckedFloatCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >float > #?(:clj float? :cljs numerically-float?)
  "Does not involve truncation or rounding."
         ([x #?(:clj float? :cljs numerically-float?)] x)
#?(:clj  ([x (t/and (t/- primitive? float? boolean?) numerically-float?)] (>float* x))
   :cljs ([x (t/and double? numerically-float?)] x))
         ([x boolean?] (if x #?(:clj (float 1) :cljs 1) #?(:clj (float 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-float?)] (>float* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-float?)] (.floatValue x)))
#?(:clj  ([x (t/and dnum/ratio? numerically-float?)] (-> x .bigIntegerValue .floatValue))))

;; ----- Double ----- ;;

;; TODO figure out how to use with goog.math.Integer/Long
(t/defn ^:inline >double*
  "May involve non-out-of-range truncation."
  > double?
        ([x double?] x)
        ([x char?] (Primitive/uncheckedDoubleCast x))  ;; For purposes of Clojure intrinsics
#?(:clj ([x (t/- primitive? double? boolean? char?)] (clojure.lang.RT/uncheckedDoubleCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >double > double?
  "Does not involve truncation or rounding."
        ([x double?] x)
#?(:clj ([x (t/and (t/- primitive? double? boolean?) numerically-double?)] (>double* x)))
        ([x boolean?] (if x #?(:clj 1.0 :cljs 1) #?(:clj 1.0 :cljs 0)))
#?(:clj ([x (t/and (t/isa? clojure.lang.BigInt) numerically-double?)] (>double* (.lpart x))))
#?(:clj ([x (t/and (t/isa? java.math.BigInteger) numerically-double?)] (.doubleValue x)))
#?(:clj ([x (t/and dnum/ratio? numerically-double?)] (-> x .bigIntegerValue .doubleValue))))

;; ===== Unsigned ===== ;;

#?(:clj
(t/defn >unsigned
  {:adapted-from #{'ztellman/primitive-math 'gloss.data.primitives}}
  ([x byte?]  (Numeric/bitAnd (short 0xFF)       x))
  ([x short?] (Numeric/bitAnd (int   0xFFFF)     x))
  ([x int?]   (Numeric/bitAnd (long  0xFFFFFFFF) x))
  ([x long?]  (java.math.BigInteger. (int 1)
                (-> ^:val (ByteBuffer/allocate (int 8))
                    ^:val (.putLong x)
                    .array)))))

;; TODO TYPED awaiting `>long`
#_(:clj (t/defn ubyte>byte   [x long?   > long?] (-> x >byte   >long)))
#_(:clj (t/defn ushort>short [x long?   > long?] (-> x >short  >long)))
#_(:clj (t/defn uint>int     [x long?   > long?] (-> x >int    >long)))
#_(:clj (t/defn ulong>long   [x bigint? > long?] (-> x >bigint >long)))
