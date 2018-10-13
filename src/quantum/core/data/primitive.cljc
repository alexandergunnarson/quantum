(ns quantum.core.data.primitive
        (:refer-clojure :exclude
          [boolean? char? comparable? decimal? double? false? float? int? integer? true?])
        (:require
 #?(:cljs [com.gfredericks.goog.math.Integer :as int])
 #?(:cljs goog.math.Integer)
 #?(:cljs goog.math.Long)
          [quantum.core.compare.core         :as ccomp]
          [quantum.core.type                 :as t]
          [quantum.untyped.core.type         :as ut]
          ;; TODO TYPED excise reference
          [quantum.untyped.core.vars         :as var
            :refer [defaliases]])
#?(:clj (:import
          [clojure.lang Numbers Util]
          [java.nio     ByteBuffer]
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
  (^:intrinsic [x boolean? > (t/assume (t/ref (t/type x)))] (Boolean/valueOf   x))
  (^:intrinsic [x byte?    > (t/assume (t/ref (t/type x)))] (Byte/valueOf      x))
  (^:intrinsic [x char?    > (t/assume (t/ref (t/type x)))] (Character/valueOf x))
  (^:intrinsic [x short?   > (t/assume (t/ref (t/type x)))] (Short/valueOf     x))
  (^:intrinsic [x int?     > (t/assume (t/ref (t/type x)))] (Integer/valueOf   x))
  (^:intrinsic [x long?    > (t/assume (t/ref (t/type x)))] (Long/valueOf      x))
  (^:intrinsic [x float?   > (t/assume (t/ref (t/type x)))] (Float/valueOf     x))
  (^:intrinsic [x double?  > (t/assume (t/ref (t/type x)))] (Double/valueOf    x))
  (            [x t/ref?] x)))

#?(:clj
(t/defn ^:inline unbox
  (^:intrinsic [x (t/ref boolean?) > (t/unref (t/type x))] (.booleanValue x))
  (^:intrinsic [x (t/ref byte?)    > (t/unref (t/type x))] (.byteValue    x))
  (^:intrinsic [x (t/ref char?)    > (t/unref (t/type x))] (.charValue    x))
  (^:intrinsic [x (t/ref short?)   > (t/unref (t/type x))] (.shortValue   x))
  (^:intrinsic [x (t/ref int?)     > (t/unref (t/type x))] (.intValue     x))
  (^:intrinsic [x (t/ref long?)    > (t/unref (t/type x))] (.longValue    x))
  (^:intrinsic [x (t/ref float?)   > (t/unref (t/type x))] (.floatValue   x))
  (^:intrinsic [x (t/ref double?)  > (t/unref (t/type x))] (.doubleValue  x))))

;; ===== Bit lengths ===== ;;

(var/def boolean-bits "Implementationally might not be bit-manipulable but logically 1 bit" 1)
(def byte-bits   8)
(def short-bits  16)
(def char-bits   16)
(def int-bits    32)
(def long-bits   64)
(def float-bits  32)
(def double-bits 64)

;; ===== Extreme magnitudes and values ===== ;;

(t/defn ^:inline >min-magnitude
  #?(:clj ([x byte?   > (type x)]          (byte  0)))
  #?(:clj ([x short?  > (type x)]          (short 0)))
  #?(:clj ([x char?   > (type x)]          (char  0)))
  #?(:clj ([x int?    > (type x)]          (int   0)))
  #?(:clj ([x long?   > (type x)]          (long  0)))
  #?(:clj ([x float?  > (type x)]          Float/MIN_VALUE))
          ([x double? > (type x)] #?(:clj  Double/MIN_VALUE
                                     :cljs js/Number.MIN_VALUE)))

;; TODO TYPED these are probably getting boxed
#?(:clj (var/def- min-float  (Numeric/negate Float/MAX_VALUE)))
        (var/def- min-double (- #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

;; TODO TYPED for some reason it's not figuring out the type of `min-float` and `min-double`
(t/defn ^:inline >min-value
  #?(:clj ([x byte?   > (type x)] Byte/MIN_VALUE))
  #?(:clj ([x short?  > (type x)] Short/MIN_VALUE))
  #?(:clj ([x char?   > (type x)] Character/MIN_VALUE))
  #?(:clj ([x int?    > (type x)] Integer/MIN_VALUE))
  #?(:clj ([x long?   > (type x)] Long/MIN_VALUE))
  #?(:clj ([x float?  > (type x)] min-float))
          ([x double? > (type x)] min-double))

(t/defn ^:inline >max-value
  #?@(:clj [([x byte?   > (type x)]         Byte/MAX_VALUE)
            ([x short?  > (type x)]         Short/MAX_VALUE)
            ([x char?   > (type x)]         Character/MAX_VALUE)
            ([x int?    > (type x)]         Integer/MAX_VALUE)
            ([x long?   > (type x)]         Long/MAX_VALUE)
            ([x float?  > (type x)]         Float/MAX_VALUE)])
            ([x double? > (type x)] #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

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
          ([x (t/or boolean? (t/value #?(:clj Boolean :cljs js/Boolean)))] boolean-bits)
#?@(:clj [([x (t/or byte?    (t/value Byte))]                              byte-bits)
          ([x (t/or short?   (t/value Short))]                             short-bits)
          ([x (t/or char?    (t/value Character))]                         char-bits)
          ([x (t/or int?     (t/value Integer))]                           int-bits)
          ([x (t/or long?    (t/value Long))]                              long-bits)
          ([x (t/or float?   (t/value Float))]                             float-bits)])
          ([x (t/or double?  #?(:clj Double :cljs js/Number))]             double-bits))

;; ===== Extensions ===== ;;

#?(:clj
(macroexpand '(t/extend-defn! ccomp/==
  #_(^:in [a boolean?                    , b boolean?]                     (Util/equiv    a b))
  (     [a boolean?                    , b (t/- primitive? boolean?)]    false)
  #_(     [a (t/- primitive? boolean?)   , b boolean?]                     false)
  #_(^:in [a long?                       , b long?]                        (Numbers/equiv a b))
  #_(     [a long?                       , b (t/- numeric? long?)]         (Numeric/eq    a b))
  #_(     [a (t/- numeric? long?)        , b long?]                        (Numeric/eq    a b))
  #_(^:in [a double?                     , b double?]                      (Numbers/equiv a b))
  #_(     [a double?                     , b (t/- numeric? double?)]       (Numeric/eq    a b))
  #_(     [a (t/- numeric? double?)      , b double?]                      (Numeric/eq    a b))
  #_(     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/eq    a b)))))

#?(:clj
(t/extend-defn! ccomp/not==
  ([a boolean?                 , b boolean?]                  (Numbers/neq a b))
  ([a boolean?                 , b (t/- primitive? boolean?)] false)
  ([a (t/- primitive? boolean?), b boolean?]                  false)
  ([a numeric?                 , b numeric?]                  (Numeric/neq a b))))

(t/extend-defn! ccomp/=
  ([a primitive?, b primitive?] (ccomp/== a b)))

(t/extend-defn! ccomp/not=
  ([a primitive?, b primitive?] (ccomp/not== a b)))

(t/extend-defn! ccomp/<
         (     [x numeric?] true)
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/lt a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? long?)]         (Numeric/lt a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/lt a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/lt a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double?)]       (Numeric/lt a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/lt a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/lt a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/< a b)))
  ;; TODO rest of numbers, but not nil
  ;; CLJ just does `>long` for both args and performs comparison that way (which is kind of unsafe)
  )

(t/extend-defn! ccomp/<=
         (     [x numeric?] true)
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/lte  a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? long?)]         (Numeric/lte  a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/lte  a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/lte  a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double?)]       (Numeric/lte  a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/lte  a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/lte  a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/<= a b)))
  ;; TODO rest of numbers, but not nil
  ;; CLJ just does `>long` for both args and performs comparison that way (which is kind of unsafe)
  )

(t/extend-defn! ccomp/>
         (     [x numeric?] true)
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/gt  a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? long?)]         (Numeric/gt  a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/gt  a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/gt  a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double?)]       (Numeric/gt  a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/gt  a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/gt  a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/> a b)))
  ;; TODO rest of numbers, but not nil
  ;; CLJ just does `>long` for both args and performs comparison that way (which is kind of unsafe)
  )

(t/extend-defn! ccomp/>=
         (     [x numeric?] true)
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/gte  a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? long?)]         (Numeric/gte  a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/gte  a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/gte  a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double?)]       (Numeric/gte  a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/gte  a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/gte  a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/>= a b)))
  ;; TODO rest of numbers, but not nil
  ;; CLJ just does `>long` for both args and performs comparison that way (which is kind of unsafe)
  )
