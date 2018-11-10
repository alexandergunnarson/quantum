(ns quantum.core.data.primitive
        (:refer-clojure :exclude
          [boolean? char? comparable? decimal? double? false? float? int? integer? true?])
        (:require
 #?(:cljs [com.gfredericks.goog.math.Integer :as int])
 #?(:cljs goog.math.Integer)
 #?(:cljs goog.math.Long)
          [quantum.core.compare.core         :as c?]
          [quantum.core.type                 :as t]
          [quantum.untyped.core.logic
            :refer [ifs]]
          [quantum.untyped.core.type         :as ut]
          ;; TODO TYPED excise reference
          [quantum.untyped.core.vars         :as var
            :refer [defaliases]])
#?(:clj (:import
          [clojure.lang Numbers Util]
          [java.nio     ByteBuffer]
          [quantum.core Numeric Primitive])))

;; TODO for CLJS nil/val, we need to check via `js/==` not `js/===`
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

        (def primitive-type?
          (t/or (t/value boolean?)
      #?@(:clj [(t/value byte?) (t/value short?) (t/value char?) (t/value int?) (t/value long?)
                (t/value float?)]) (t/value double?)))

        (var/def integer? "Specifically primitive integers."
          (t/or #?@(:clj [byte? short? int? long?])))

        (var/def decimal? "Specifically primitive decimals."
          (t/or #?(:clj float?) double?))

        (var/def numeric?
          "Specifically primitive numeric things.
           Something 'numeric' is something that may be treated as a number but may not actually *be* one."
          (t/- primitive? boolean?))

        (def numeric-type? (t/- primitive-type? (t/value boolean?)))

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

(t/defn ^:inline >type > t/type?
  ([x (t/or boolean? (t/value boolean?))] boolean?)
  ([x (t/or byte?    (t/value byte?))]    byte?)
  ([x (t/or char?    (t/value char?))]    char?)
  ([x (t/or short?   (t/value short?))]   short?)
  ([x (t/or int?     (t/value int?))]     int?)
  ([x (t/or long?    (t/value long?))]    long?)
  ([x (t/or float?   (t/value float?))]   float?)
  ([x (t/or double?  (t/value double?))]  double?))

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
#?(:clj ([x (t/or byte?   (t/value byte?))   > byte?]  (byte  0)))
#?(:clj ([x (t/or short?  (t/value short?))  > short?] (short 0)))
#?(:clj ([x (t/or char?   (t/value char?))   > char?]  (char  0)))
#?(:clj ([x (t/or int?    (t/value int?))    > int?]   (int   0)))
#?(:clj ([x (t/or long?   (t/value long?))   > long?]  (long  0)))
#?(:clj ([x (t/or float?  (t/value float?))  > float?] Float/MIN_VALUE))
        ([x (t/or double? (t/value double?)) > double?]
          #?(:clj Double/MIN_VALUE :cljs js/Number.MIN_VALUE)))

;; TODO TYPED these are probably getting boxed
#?(:clj (var/def- min-float  (Numeric/negate Float/MAX_VALUE)))
        (var/def- min-double (- #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

(t/defn ^:inline >min-value
#?(:clj ([x (t/or byte?   (t/value byte?))   > byte?]   Byte/MIN_VALUE))
#?(:clj ([x (t/or short?  (t/value short?))  > short?]  Short/MIN_VALUE))
#?(:clj ([x (t/or char?   (t/value char?))   > char?]   Character/MIN_VALUE))
#?(:clj ([x (t/or int?    (t/value int?))    > int?]    Integer/MIN_VALUE))
#?(:clj ([x (t/or long?   (t/value long?))   > long?]   Long/MIN_VALUE))
#?(:clj ([x (t/or float?  (t/value float?))  > float?]  min-float))
        ([x (t/or double? (t/value double?)) > double?] min-double))

(t/defn ^:inline >max-value
#?@(:clj [([x (t/or byte?   (t/value byte?))   > byte?]   Byte/MAX_VALUE)
          ([x (t/or short?  (t/value short?))  > short?]  Short/MAX_VALUE)
          ([x (t/or char?   (t/value char?))   > char?]   Character/MAX_VALUE)
          ([x (t/or int?    (t/value int?))    > int?]    Integer/MAX_VALUE)
          ([x (t/or long?   (t/value long?))   > long?]   Long/MAX_VALUE)
          ([x (t/or float?  (t/value float?))  > float?]  Float/MAX_VALUE)])
          ([x (t/or double? (t/value double?)) > double?]
            #?(:clj Double/MAX_VALUE :cljs js/Number.MAX_VALUE)))

(t/defn ^:inline >min-safe-integer-value
#?@(:clj [([x (t/or byte?   (t/value byte?))   > byte?]   (>min-value x))
          ([x (t/or short?  (t/value short?))  > short?]  (>min-value x))
          ([x (t/or char?   (t/value char?))   > char?]   (>min-value x))
          ([x (t/or int?    (t/value int?))    > int?]    (>min-value x))
          ([x (t/or long?   (t/value long?))   > long?]   (>min-value x))
          ;; [2 ^ (<mantissa bits> + 1)] - 1
          ([x (t/or float?  (t/value float?))  > float?]  (float -16777216.0))])
          ([x (t/or double? (t/value double?)) > double?] -9007199254740991.0))

(t/defn ^:inline >max-safe-integer-value
#?@(:clj [([x (t/or byte?   (t/value byte?))   > byte?]   (>max-value x))
          ([x (t/or short?  (t/value short?))  > short?]  (>max-value x))
          ([x (t/or char?   (t/value char?))   > char?]   (>max-value x))
          ([x (t/or int?    (t/value int?))    > int?]    (>max-value x))
          ([x (t/or long?   (t/value long?))   > long?]   (>max-value x))
          ;; [2 ^ (<mantissa bits> + 1)] - 1
          ([x (t/or float?  (t/value float?))  > float?]  (float 16777216.0))])
          ([x (t/or double? (t/value double?)) > double?] 9007199254740991.0))

;; ===== Primitive type properties ===== ;;

(t/defn ^:inline signed?
          ([x (t/or char?    (t/value Character)                       (t/value char?))]   false)
#?@(:clj [([x (t/or byte?    (t/value Byte)                            (t/value byte?)
                    short?   (t/value Short)                           (t/value short?)
                    int?     (t/value Integer)                         (t/value int?)
                    long?    (t/value Long)                            (t/value long?)
                    float?   (t/value Float)                           (t/value float?)
                    double?  (t/value #?(:clj Double :cljs js/Number)) (t/value double?))] true)]))

;; TODO TYPED `t/numerically-integer?`
(t/defn ^:inline >bit-size ; > t/numerically-integer?
          ([x (t/or boolean? (t/value #?(:clj Boolean :cljs js/Boolean)) (t/value boolean?))]
            boolean-bits)
#?@(:clj [([x (t/or byte?    (t/value Byte)      (t/value byte?))]         byte-bits)
          ([x (t/or short?   (t/value Short)     (t/value short?))]        short-bits)
          ([x (t/or char?    (t/value Character) (t/value char?))]         char-bits)
          ([x (t/or int?     (t/value Integer)   (t/value int?))]          int-bits)
          ([x (t/or long?    (t/value Long)      (t/value long?))]         long-bits)
          ([x (t/or float?   (t/value Float)     (t/value float?))]        float-bits)])
          ([x (t/or double?  (t/value #?(:clj Double :cljs js/Number)) (t/value double?))]
            double-bits))

;; ===== Conversion ===== ;;
;; Note that numeric-primitive conversions do not go here (but may be found in
;; `quantum.core.data.numeric`) because they take as inputs and produce outputs things that are
;; within a numeric range.

;; ----- Boolean ----- ;;

;; TODO CLJS
;; TODO rethink — is everything that's a 0 false and everything that's a 1 a true? Or is it just
;; 0's that are false? Etc.
(t/defn ^:inline >boolean
  "Converts input to a boolean.
   Differs from asking whether something is truthy/falsey."
  > boolean?
         ([x boolean?] x)          ;; For purposes of Clojure intrinsics
#?(:clj  ([x (t/or long? double?)] (-> x clojure.lang.Numbers/isZero Numeric/not)))
#?(:clj  ([x (t/- primitive? boolean? long? double?)] (-> x Numeric/isZero Numeric/not))))

;; ===== Extensions ===== ;;

#?(:clj
(t/extend-defn! c?/==
  (^:in [a boolean?                    , b boolean?]                     (Util/equiv    a b))
  (     [a boolean?                    , b (t/- primitive? boolean?)]    false)
  (     [a (t/- primitive? boolean?)   , b boolean?]                     false)
  (^:in [a long?                       , b long?]                        (Numbers/equiv a b))
  (     [a long?                       , b (t/- numeric? double? long?)] (Numeric/eq    a b))
  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/eq    a b))
  (^:in [a double?                     , b double?]                      (Numbers/equiv a b))
  (     [a double?                     , b (t/- numeric? double? long?)] (Numeric/eq    a b))
  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/eq    a b))
  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/eq    a b))))

#?(:clj
(t/extend-defn! c?/not==
  ([a boolean?                 , b boolean?]                  (Numeric/neq a b))
  ([a boolean?                 , b (t/- primitive? boolean?)] false)
  ([a (t/- primitive? boolean?), b boolean?]                  false)
  ([a numeric?                 , b numeric?]                  (Numeric/neq a b))))

(t/extend-defn! c?/=
         ([a primitive?, b primitive?] (c?/== a b))
#?(:cljs ([a primitive?, b t/any?]     false)))

(t/extend-defn! c?/not=
  ([a primitive?, b primitive?] (c?/not== a b)))

(t/extend-defn! c?/<
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/lt a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? double? long?)] (Numeric/lt a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/lt a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/lt a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double? long?)] (Numeric/lt a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/lt a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/lt a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/< a b)))
)

(t/extend-defn! c?/<=
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/lte  a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? double? long?)] (Numeric/lte  a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/lte  a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/lte  a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double? long?)] (Numeric/lte  a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/lte  a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/lte  a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/<= a b)))
)

(t/extend-defn! c?/>
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/gt  a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? double? long?)] (Numeric/gt  a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/gt  a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/gt  a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double? long?)] (Numeric/gt  a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/gt  a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/gt  a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/> a b)))
)

(t/extend-defn! c?/>=
#?(:clj  (^:in [a long?                       , b long?]                        (Numbers/gte  a b)))
#?(:clj  (     [a long?                       , b (t/- numeric? double? long?)] (Numeric/gte  a b)))
#?(:clj  (     [a (t/- numeric? long?)        , b long?]                        (Numeric/gte  a b)))
#?(:clj  (^:in [a double?                     , b double?]                      (Numbers/gte  a b)))
#?(:clj  (     [a double?                     , b (t/- numeric? double? long?)] (Numeric/gte  a b)))
#?(:clj  (     [a (t/- numeric? double?)      , b double?]                      (Numeric/gte  a b)))
#?(:clj  (     [a (t/- numeric? double? long?), b (t/- numeric? double? long?)] (Numeric/gte  a b)))
#?(:cljs (     [a numeric?                    , b numeric?]                     (cljs.core/>= a b)))
)

(t/extend-defn! c?/compare
        ([a false?                 , b false?]   (int  0))
        ([a false?                 , b true?]    (int -1))
        ([a true?                  , b false?]   (int  1))
        ([a true?                  , b true?]    (int  0))
        ([a boolean?               , b boolean?]
          (if a (if b (int 0) (int 1)) (if b (int -1) (int 0))))
        ([a numeric?               , b numeric?]
          (ifs (c?/< a b) (int -1) (c?/> a b) (int 1) (int 0)))
#?(:clj ([a (t/ref c?/icomparable?), b primitive?]              (.compareTo a       b)))
#?(:clj ([a primitive?             , b (t/ref c?/icomparable?)] (.compareTo (box a) b))))

(t/extend-defn! c?/comp<
  ([a (t/input-type c?/compare :? :_), b (t/input-type c?/compare [= (t/type a)] :?)]
    (c?/<  (c?/compare a b) 0)))

(t/extend-defn! c?/comp<=
  ([a (t/input-type c?/compare :? :_), b (t/input-type c?/compare [= (t/type a)] :?)]
    (c?/<= (c?/compare a b) 0)))

(t/extend-defn! c?/comp=
  ([a (t/input-type c?/compare :? :_), b (t/input-type c?/compare [= (t/type a)] :?)]
    (c?/=  (c?/compare a b) 0)))

(t/extend-defn! c?/comp>=
  ([a (t/input-type c?/compare :? :_), b (t/input-type c?/compare [= (t/type a)] :?)]
    (c?/>= (c?/compare a b) 0)))

(t/extend-defn! c?/comp>
  ([a (t/input-type c?/compare :? :_), b (t/input-type c?/compare [= (t/type a)] :?)]
    (c?/>  (c?/compare a b) 0)))

;; TODO come back to this
;; Use interval tree?
#_(t/defn promote-type
  "Based on max/min safe integer value."
  ;; TODO Write it all out and compress later
  ([t|<min (t/value byte?), t|>max (t/value byte?)]   t|<min)
  ([t|<min (t/value byte?), t|>max (t/value short?)]  t|>max)
  ([t|<min (t/value byte?), t|>max (t/value char?)]   int?)
  ([t|<min (t/value byte?), t|>max (t/value int?)]    t|>max)
  ([t|<min (t/value byte?), t|>max (t/value long?)]   t|>max)
  ([t|<min (t/value byte?), t|>max (t/value float?)]  t|>max)
  ([t|<min (t/value byte?), t|>max (t/value double?)] t|>max))

;; TODO come back to this
#_(t/defn narrowest
  "Based on max/min safe integer value."
  > t/type?
  ([t0 (t/and (t/input-type >min-safe-integer-value [:? t/>= t/type?])
              (t/input-type >max-safe-integer-value [:? t/>= t/type?]))
    t1 (t/and (t/input-type >min-safe-integer-value [:? t/>= t/type?])
              (t/input-type >max-safe-integer-value [:? t/>= t/type?]))]
    (let [t0-min (>min-safe-integer-value t0)
          t1-min (>min-safe-integer-value t1)
          t0-max (>max-safe-integer-value t0)
          t1-max (>max-safe-integer-value t1)]
      ;; TODO this provides great room for auto-optimization
      (ifs (c?/= t0-min t1-min)
             (ifs (c?/= t0-max t1-max) t0
                  (c?/< t0-max t1-max) t1
                  t0)
           (c?/< t0-min t1-min)
             (ifs (c?/< t0-max t1-max) (promote-type t0 t1)
                  (c?/= t0-max t1-max) t0
                  t0)
           (ifs (c?/> t0-max t1-max) (promote-type t1 t0)
                (c?/= t0-max t1-max) t1
                t1)))))

;; TODO maybe use `> (narrowest (t/type a) (t/type b))` for `min` and `max`
(t/extend-defn! c?/min
#?(:clj  (     [a (t/- numeric? int? float? double?)
                b (t/- numeric? int? float? double?)]                         (if (c?/< a b) a b)))
#?(:clj  (     [a int?                      , b (t/- numeric? int?)]          (if (c?/< a b) a b)))
#?(:clj  (     [a (t/- numeric? int?)       , b int?]                         (if (c?/< a b) a b)))
#?(:clj  (^:in [a int?                      , b int?]                         (Math/min      a b)))
#?(:clj  (     [a float?                    , b (t/- numeric? int? float?)]   (if (c?/< a b) a b)))
#?(:clj  (     [a (t/- numeric? int? float?), b float?]                       (if (c?/< a b) a b)))
#?(:clj  (     [a float?                    , b float?]                       (Math/min      a b)))
#?(:clj  (     [a double?
                b (t/- numeric? int? float? double?)]                         (if (c?/< a b) a b)))
#?(:clj  (     [a (t/- numeric? int? float? double?)
                b double?]                                                    (if (c?/< a b) a b)))
#?(:clj  (     [a double?                   , b double?]                      (Math/min      a b)))
#?(:cljs (     [a double?                   , b double? > (t/assume double?)] (js/Math.min   a b))))

(t/extend-defn! c?/max
#?(:clj  (     [a (t/- numeric? int? float? double?)
                b (t/- numeric? int? float? double?)]                         (if (c?/> a b) a b)))
#?(:clj  (     [a int?                      , b (t/- numeric? int?)]          (if (c?/> a b) a b)))
#?(:clj  (     [a (t/- numeric? int?)       , b int?]                         (if (c?/> a b) a b)))
#?(:clj  (^:in [a int?                      , b int?]                         (Math/max      a b)))
#?(:clj  (     [a float?                    , b (t/- numeric? int? float?)]   (if (c?/> a b) a b)))
#?(:clj  (     [a (t/- numeric? int? float?), b float?]                       (if (c?/> a b) a b)))
#?(:clj  (     [a float?                    , b float?]                       (Math/max      a b)))
#?(:clj  (     [a double?
                b (t/- numeric? int? float? double?)]                         (if (c?/> a b) a b)))
#?(:clj  (     [a (t/- numeric? int? float? double?)
                b double?]                                                    (if (c?/> a b) a b)))
#?(:clj  (     [a double?                   , b double?]                      (Math/max      a b)))
#?(:cljs (     [a double?                   , b double? > (t/assume double?)] (js/Math.max   a b))))
