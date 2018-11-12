(ns quantum.core.data.numeric
  "Better `bigint?` for CLJ:
   - There are almost certainly faster ones but whether they will be as correct is unknown

   Better `bigdec?` for CLJ:
   - There are almost certainly faster ones

   `bigint?` for CLJS:
   - `BigInt` is a built-in JS object in Chrome as of 5/2018. It's faster than bn.js. https://developers.google.com/web/updates/2018/05/bigint
   - As of 5/2018, the best current substitute for `BigInt` is bn.js, but there apparently is a
     polyfill available for BigInt so maybe that's better.

   `bigdec?` for CLJS:
   - decimal.js is the best contender as of 9/27/2018. https://github.com/MikeMcl/decimal.js

   `ratio?` for CLJS:
   - Fraction.js is the best contender as of 9/27/2018. https://github.com/infusion/Fraction.js"
         (:refer-clojure :exclude ; otherwise `Unable to resolve symbol: eval`
           [decimal? denominator integer? number? numerator ratio? zero?])
         (:require
           [clojure.core                      :as core]
           [clojure.string                    :as str]
  #?(:cljs goog.math.Integer)
  #?(:cljs goog.math.Long)
           [quantum.core.compare.core         :as c?]
           [quantum.core.data.primitive       :as p]
         #_[quantum.core.logic
             :refer [whenf fn-not fn=]]
           [quantum.core.type                 :as t]
           ;; TODO TYPED excise reference
           [quantum.untyped.core.vars         :as var
             :refer [defalias]])
#?(:clj  (:import
           [clojure.lang BigInt Numbers Ratio]
           [java.math    BigInteger]
           [quantum.core Numeric Primitive])))

;; ===== Types ===== ;;

;; ----- Integers ----- ;;

;; Incorporated `clojure.core/int?`
;; Incorporated `cljs.core/int?`
(var/def fixint? "The set of all fixed-precision (though not necessarily primitive) integers."
  (t/or #?@(:clj [p/byte? p/short?]) p/int? p/long?))

#?(:clj (def java-bigint? (t/isa? java.math.BigInteger)))
#?(:clj (def clj-bigint?  (t/isa? clojure.lang.BigInt)))

(var/def bigint? "The set of all 'big' (arbitrary-precision) integers."
  #?(:clj  ;; TODO bring in a better implementation per the ns docstring?
           (t/or clj-bigint? java-bigint?)
           ;; TODO bring in implementation per the ns docstring
     :cljs t/none?))

;; Incorporated `clojure.lang.Util/isInteger`
;; Incorporated `clojure.core/integer?`
;; Incorporated `cljs.core/integer?`
(def integer? (t/or fixint? bigint?))

;; ----- Decimals ----- ;;

(t/defn ^:inline nan?
  "`Float/NaN`, `Double/NaN`, and `js/NaN` are all non-self-identical. `js/NaN` is not a
   `js/Number` (sensibly so), but `Float/NaN` is a `Float` and `Double/NaN` is a `Double`."
  > p/boolean?
  #?(:cljs ([x t/any? > (t/assume p/boolean?)]        (js/isNaN     x)))
  #?(:clj  ([x p/float?]                              (Float/isNaN  x)))
  #?(:clj  ([x p/double?]                             (Double/isNaN x)))
           ;; TODO TYPED `(t/- t/any? p/float? p/double?)`
  #?(:clj  ([x (t/- p/primitive? p/float? p/double?)] false))
  #?(:clj  ([x t/ref?]                                false)))

;; Incorporated `clojure.core/float?`
;; Incorporated `cljs.core/float?`
(var/def fixdec? "The set of all fixed-precision decimals."
  (t/or #?(:clj p/float?) p/double?))

;; Incorporated `clojure.core/decimal?`
(var/def bigdec? "The set of all 'big' (arbitrary-precision) decimals."
  #?(:clj  ;; TODO bring in a better implementation per the ns docstring?
           (t/isa? BigDecimal)
           ;; TODO bring in implementation per the ns docstring
     :cljs t/none?))

(def decimal? (t/or fixdec? bigdec?))

;; ----- Precision ----- ;;

(var/def fixnum? "The set of all fixed-precision numbers."
  (t/or fixint? fixdec?))

(var/def fixed-numeric?
  "The set of all fixed-precision numeric things. Something 'numeric' is something that may be
   treated as a number but may not actually *be* one."
  (t/or fixnum? p/char?))

(var/def bignum? "The set of all 'big' (arbitrary-precision) numbers."
  (t/or fixint? fixdec?))

;; ----- Ratios ----- ;;

(def ratio? #?(:clj  (t/isa? clojure.lang.Ratio)
                     ;; TODO bring in implementation per the ns docstring
               :cljs t/none?))

;; ----- General ----- ;;

(def exact? (t/or integer? ratio?))

(def number? (t/or #?@(:clj  [(t/isa? java.lang.Number)]
                       :cljs [integer? decimal? ratio?])))

(var/def numeric?
  "Something 'numeric' is something that may be treated as a number but may not actually *be* one."
  (t/or number? #?(:clj p/char?)))

;; ===== Arbitrary-precision conversions (needed for comparisons) ===== ;;

;; Forward declaration so bigint conversions can use it
;; TODO figure out how to use with CLJS, including goog.math.Integer/Long
#?(:clj
(t/defn ^:inline >long*
  "May involve non-out-of-range truncation."
  > p/long?
  (     [x p/long?] x)
  (     [x p/char?] (Primitive/uncheckedLongCast x))
  (^:in [x (t/- p/primitive? p/long? p/boolean? p/char?)] (clojure.lang.RT/uncheckedLongCast x))
  (     [x (t/ref number?)] (.longValue x))))

;; TODO figure out how to use with goog.math.Integer/Long
(t/defn ^:inline >double*
  "May involve non-out-of-range truncation."
  > p/double?
        (     [x p/double?] x)
        (     [x p/char?] (Primitive/uncheckedDoubleCast x))
#?(:clj (^:in [x (t/- p/primitive? p/boolean? p/char? p/double?)]
          (clojure.lang.RT/uncheckedDoubleCast x))))

#?(:clj
(t/defn ^:inline >java-bigint > java-bigint?
  ([x fixed-numeric? > (t/assume java-bigint?)] (-> x >long* BigInteger/valueOf))
  ([x java-bigint?] x)
  ([x clj-bigint? > (t/assume java-bigint?)] (.toBigInteger x))
  ;; Truncates the decimal portion
  ;; TODO should this overload be part of `>java-bigint*`?
  ([x bigdec? > (t/assume java-bigint?)] (.toBigInteger x))
  ;; Truncates the decimal portion
  ;; TODO should this overload be part of `>java-bigint*`?
  ([x ratio? > (t/assume java-bigint?)] (.bigIntegerValue x)))

#?(:clj
(t/defn ^:inline >clj-bigint > clj-bigint?
  ([x fixed-numeric? > (t/assume clj-bigint?)] (-> x >long* BigInt/fromLong))
  ([x java-bigint?   > (t/assume clj-bigint?)] (BigInt/fromBigInteger x))
  ([x clj-bigint?] x)
  ;; Truncates the decimal portion
  ;; TODO should this overload be part of `>clj-bigint*`?
  ([x bigdec?] (-> x >java-bigint >clj-bigint))
  ;; Truncates the decimal portion
  ;; TODO should this overload be part of `>clj-bigint*`?
  ([x ratio?] (-> x >java-bigint >clj-bigint))))

#?(:clj
(t/defn ^:inline >bigdec > bigdec?
  ([x (t/or fixint? p/char?) > (t/assume bigdec?)] (-> x >long*   BigDecimal/valueOf))
  ([x fixdec?                > (t/assume bigdec?)] (-> x >double* BigDecimal/valueOf))
  ([x java-bigint?] (BigDecimal. x))
  ([x clj-bigint? > (t/assume bigdec?)] (.toBigDecimal x))
  ([x bigdec?] x)
  ([x ratio? > (t/assume bigdec?)] (.divide (-> x ^:val (.numerator)   >bigdec)
                                            (-> x ^:val (.denominator) >bigdec)))))

#?(:clj
(t/defn ^:inline >ratio > ratio?
  ([x (t/or fixed-numeric? bigint?)] (Ratio. (>java-bigint x) BigInteger/ONE))
  ([x bigdec?] (let [v ^:val (.unscaledValue x)
                     scale (.scale x)]
                 (if (c?/< scale 0)
                     (Ratio. (.multiply v (.pow ^:val (. BigInteger TEN)
                                                (Numbers/unchecked_int_negate scale)))
                             ^:val (. BigInteger ONE))
                     (Ratio. v (.pow ^:val (. BigInteger TEN) scale)))))
  ([x ratio?] x)))

;; ===== Comparison extensions ===== ;;

;; TODO primitive with non-primitive
;; TODO this errors out in the middle of the effects queue
((t/extend-defn! c?/=
        ;; `.equals` takes into account precision even if they're numerically equivalent
        ;; `core/=` uses `.equals` for `BigDecimal`s
#?(:clj ([a bigdec?     , b bigdec?]      (c?/comp= a b)))
#?(:clj ([a bigdec?     , b numeric?]     (c?/= a (>bigdec b))))
#?(:clj ([a numeric?    , b bigdec?]      (c?/= (>bigdec a) b)))
#?(:clj ([a java-bigint?, b java-bigint?] (.equals a b)))
#?(:clj ([a java-bigint?, b numeric?]     (c?/= a (>java-bigint b))))
#?(:clj ([a numeric?    , b java-bigint?] (c?/= (>java-bigint a) b)))
#?(:clj ([a clj-bigint? , b clj-bigint?]  (.equals a b)))
#?(:clj ([a clj-bigint? , b numeric?]     (c?/= a (>clj-bigint b))))
#?(:clj ([a numeric?    , b clj-bigint?]  (c?/= (>clj-bigint a) b)))
#?(:clj ([a ratio?      , b ratio?]
          (and (c?/= ^:val (.numerator   a) ^:val (.numerator   b))
               (c?/= ^:val (.denominator a) ^:val (.denominator b)))))
#?(:clj ([a ratio?      , b numeric?]     (c?/= a (>ratio b))))
#?(:clj ([a numeric?    , b ratio?]       (c?/= (>ratio a) b)))))

;; TODO primitive with non-primitive
(t/extend-defn! c?/<
        ([x numeric?] true)
#?(:clj ([a bigdec?     , b bigdec?]      (c?/comp< a b)))
#?(:clj ([a bigdec?     , b numeric?]     (c?/< a (>bigdec b))))
#?(:clj ([a numeric?    , b bigdec?]      (c?/< (>bigdec a) b)))
#?(:clj ([a java-bigint?, b java-bigint?] (c?/comp< a b)))
#?(:clj ([a java-bigint?, b numeric?]     (c?/< a (>java-bigint b))))
#?(:clj ([a numeric?    , b java-bigint?] (c?/< (>java-bigint a) b)))
#?(:clj ([a clj-bigint?, b clj-bigint?]
          (if (and (p/nil? (.bipart a)) (p/nil? (.bipart b)))
              (c?/<     (.lpart      a) (.lpart      b))
              (c?/comp< (>java-bigint a) (>java-bigint b)))))
#?(:clj ([a clj-bigint? , b numeric?]     (c?/< a (>clj-bigint b))))
#?(:clj ([a numeric?    , b clj-bigint?]  (c?/< (>clj-bigint a) b)))
#?(:clj ([a ratio?      , b ratio?]
          (c?/< (.multiply (.numerator   a) (.numerator   b))
                (.multiply (.denominator a) (.denominator b)))))
#?(:clj ([a ratio?      , b numeric?]     (c?/< a (>ratio b))))
#?(:clj ([a numeric?    , b ratio?]       (c?/< (>ratio a) b))))

;; TODO primitive with non-primitive
(t/extend-defn! c?/<=
        ([x numeric?] true)
#?(:clj ([a bigdec?    , b bigdec?]     (c?/comp<= a b)))
#?(:clj ([a bigdec?    , b numeric?]    (c?/<= a (>bigdec b))))
#?(:clj ([a numeric?   , b bigdec?]     (c?/<= (>bigdec a) b)))
#?(:clj ([a clj-bigint?, b clj-bigint?]
          (if (and (p/nil? (.bipart a)) (p/nil? (.bipart b)))
              (c?/<=     (.lpart      a) (.lpart      b))
              (c?/comp<= (>java-bigint a) (>java-bigint b)))))
#?(:clj ([a ratio?     , b ratio?]
          (c?/<= (.multiply (.numerator   a) (.numerator   b))
                 (.multiply (.denominator a) (.denominator b))))))

;; TODO primitive with non-primitive
(t/extend-defn! c?/>
        ([x numeric?] true)
#?(:clj ([a bigdec?    , b bigdec?]  (c?/comp> a b)))
#?(:clj ([a bigdec?    , b numeric?] (c?/> a (>bigdec b))))
#?(:clj ([a numeric?   , b bigdec?]  (c?/> (>bigdec a) b)))
#?(:clj ([a clj-bigint?, b clj-bigint?]
          (if (and (p/nil? (.bipart a)) (p/nil? (.bipart b)))
              (c?/>     (.lpart       a) (.lpart       b))
              (c?/comp> (>java-bigint a) (>java-bigint b)))))
#?(:clj ([a ratio?     , b ratio?]
          (c?/> (.multiply (.numerator   a) (.numerator   b))
                (.multiply (.denominator a) (.denominator b))))))

;; TODO primitive with non-primitive
(t/extend-defn! c?/>=
        ([x numeric?] true)
#?(:clj ([a bigdec?    , b bigdec?]  (c?/comp>= a b)))
#?(:clj ([a bigdec?    , b numeric?] (c?/>= a (>bigdec b))))
#?(:clj ([a numeric?   , b bigdec?]  (c?/>= (>bigdec a) b)))
#?(:clj ([a clj-bigint?, b clj-bigint?]
          (if (and (p/nil? (.bipart a)) (p/nil? (.bipart b)))
              (c?/>=     (.lpart       a) (.lpart       b))
              (c?/comp>= (>java-bigint a) (>java-bigint b)))))
#?(:clj ([a ratio?     , b ratio?]
          (c?/>= (.multiply (.numerator   a) (.numerator   b))
                 (.multiply (.denominator a) (.denominator b))))))

;; TODO `c?/compare`

;; ===== Comparisons to constants (e.g. to 0 or 1) ===== ;;

(t/defn ^:inline >zero-of-type #_> #_zero?
#?(:clj ([x p/byte?      > (t/type x)] Numeric/byte0))
#?(:clj ([x p/short?     > (t/type x)] Numeric/short0))
#?(:clj ([x p/char?      > (t/type x)] Numeric/char0))
        ([x p/int?       > #?(:clj (t/type x) :cljs (t/assume (t/type x)))]
           #?(:clj Numeric/int0 :cljs goog.math.Integer/ZERO))
        ([x p/long?      > #?(:clj (t/type x) :cljs (t/assume (t/type x)))]
           #?(:clj 0 :cljs goog.math.Long/ZERO))
#?(:clj ([x p/float?     > (t/type x)] Numeric/float0))
        ([x p/double?    > (t/type x)] 0.0)
#?(:clj ([x java-bigint? > (t/assume (t/type x))] java.math.BigInteger/ZERO))
#?(:clj ([x clj-bigint?  > (t/assume (t/type x))] clojure.lang.BigInt/ZERO))
#?(:clj ([x bigdec?      > (t/assume (t/type x))] java.math.BigDecimal/ZERO)))

(t/defn ^:inline zero? > p/boolean?
#?(:clj  (^:in [x (t/or p/long? p/double?)] (Numbers/isZero x)))
#?(:clj  (     [x (t/- p/numeric? p/long? p/double?)] (Numeric/isZero x)))
#?(:clj  (     [x clj-bigint?] (if (p/nil? (.bipart x))
                                   (-> x .lpart  zero?)
                                   (-> x .bipart zero?))))
#?(:clj  (     [x (t/or java-bigint? bigdec?)] (-> x .signum zero?)))
#?(:clj  (     [x ratio?]  (-> x .numerator zero?)))
         (     [x #?(:clj (t/ref number?) :cljs numeric?)] (?/= x 0)))

(t/defn ^:inline >one-of-type #_> #_one?
#?(:clj ([x p/byte?      > (t/type x)] Numeric/byte1))
#?(:clj ([x p/short?     > (t/type x)] Numeric/short1))
#?(:clj ([x p/char?      > (t/type x)] Numeric/char1))
        ([x p/int?       > #?(:clj (type x) :cljs (t/assume (t/type x)))]
           #?(:clj Numeric/int1 :cljs goog.math.Integer/ONE))
        ([x p/long?      > #?(:clj (type x) :cljs (t/assume (t/type x)))]
           #?(:clj 1 :cljs goog.math.Long/ONE))
#?(:clj ([x p/float?     > (t/type x)] Numeric/float1))
        ([x p/double?    > (t/type x)] 1.0)
#?(:clj ([x java-bigint? > (t/assume (t/type x))] java.math.BigInteger/ONE))
#?(:clj ([x clj-bigint?  > (t/assume (t/type x))] clojure.lang.BigInt/ONE))
#?(:clj ([x bigdec?      > (t/assume (t/type x))] java.math.BigDecimal/ONE)))

(t/defn ^:inline one? > p/boolean?
#?(:clj ([x p/numeric?] (c?/= x (>one-of-type x))))
        ([x #?(:clj (t/ref number?) :cljs numeric?)] (c?/= x 1)))

(t/defn ^:inline neg? > p/boolean?
#?(:clj  (^:in [x (t/or p/long? p/double?)] (Numbers/isNeg x)))
#?(:clj  (     [x (t/- p/numeric? p/long? p/double?)] (Numeric/isNeg x)))
#?(:clj  (     [x clj-bigint?] (if (?/nil? (.bipart x))
                                   (-> x .lpart  neg?)
                                   (-> x .bipart neg?))))
#?(:clj  (     [x (t/or java-bigint? bigdec?)] (-> x .signum neg?)))
#?(:clj  (     [x ratio?] (-> x .numerator neg?)))
         (     [x #?(:clj (t/ref number?) :clj numeric?)] (?/< x 0)))

;; TODO TYPED this should realize that we're negating a `<` and change the operator to `<=`
(t/def nneg? (fn/comp ?/not neg?))

(t/defn ^:inline pos? > p/boolean?
#?(:clj  (^:in [x (t/or p/long? p/double?)] (Numbers/isPos x)))
#?(:clj  (     [x (t/- p/numeric? p/long? p/double?)] (Numeric/isPos x)))
#?(:clj  (     [x clj-bigint?] (if (?/nil? (.bipart x))
                                   (-> x .lpart  pos?)
                                   (-> x .bipart pos?))))
#?(:clj  (     [x (t/or java-bigint? bigdec?)] (-> x .signum pos?)))
#?(:clj  (     [x ratio?] (-> x .numerator pos?)))
         (     [x #?(:clj (t/ref number?) :clj numeric?)] (c?/> x 0)))

;; TODO TYPED this should realize that we're negating a `<` and change the operator to `<=`
(t/def npos? (fn/comp ?/not pos?))

(t/def pos-int?  (fn/and integer? pos?))
(t/def neg-int?  (fn/and integer? neg?))
(t/def npos-int? (fn/and integer? npos?))
(t/def nneg-int? (fn/and integer? nneg?))

(t/defn ^:inline infinite?
  "`Float/NEGATIVE_INFINITY`, `Float/POSITIVE_INFINITY`, `Double/NEGATIVE_INFINITY`, and
   `Double/POSITIVE_INFINITY` are all non-self-identical. `js/Number.NEGATIVE_INFINITY` and
   `js/Number.POSITIVE_INFINITY` are self-identical, but neither of them are `js/Number`s. By
   contrast, the `Float` and `Double` infinities are instances of `Float` and `Double`,
   respectively."
#?(:cljs ([x t/any? > (t/assume p/boolean?)]      (??/not (js/isFinite x))))
#?(:clj  ([x p/float?]                            (Float/isInfinite  x)))
#?(:clj  ([x p/double?]                           (Double/isInfinite x)))
#?(:clj  ([x (t/- p/numeric? p/float? p/double?)] false))
         ;; This leaves room for other numbers to be infinite
#?(:clj  ([x (t/ref number?)]                     false)))

;; ===== Likenesses ===== ;;

(t/defn integer-value? ; TODO this is the same as `numerically-integer?` but we need to turn it into a predicate
  {:adapted-from
    '#{com.google.common.math.DoubleMath/isMathematicalInteger
       "https://stackoverflow.com/questions/1078953/check-if-bigdecimal-is-integer-value"}}
  > p/boolean?
         (     [x integer?] true)
#?(:cljs (^:in [x p/double? > (t/assume p/boolean?)] (js/Number.isInteger x)))
#?(:clj  (     [x (t/or p/float? p/double?)] (c?/= x (>long* x))))
#?(:clj  (     [x bigdec?] (or (zero? (.signum x))
                               (-> x .scale npos?)
                               (-> x .stripTrailingZeros .scale npos?))))
#?(:clj  (     [x (t/ref number?)] x)))

(def numerically-integer? (t/or integer? (t/and decimal? (>expr unum/integer-value?))))

#_(def numerically-byte?
    (and numerically-integer? (>expr (c/fn [x] (c/<= -128                 x 127)))))

#_(def numerically-short?
    (and numerically-integer? (>expr (c/fn [x] (c/<= -32768               x 32767)))))

#_(def numerically-char?
    (and numerically-integer? (>expr (c/fn [x] (c/<=  0                   x 65535)))))

#_(def numerically-unsigned-short? numerically-char?)

#_(def numerically-int?
    (and numerically-integer? (>expr (c/fn [x] (c/<= -2147483648          x 2147483647)))))

#_(def numerically-long?
    (and numerically-integer? (>expr (c/fn [x] (c/<= -9223372036854775808 x 9223372036854775807)))))

#_(def numerically-float?
    (and number?
         (>expr (c/fn [x] (c/<= -3.4028235E38 x 3.4028235E38)))
         (>expr (c/fn [x] (-> x #?(:clj clojure.lang.RT/floatCast :cljs c/float) (c/== x))))))

#_(def numerically-double?
    (and number?
         (>expr (c/fn [x] (c/<= -1.7976931348623157E308 x 1.7976931348623157E308)))
         (>expr (c/fn [x] (-> x clojure.lang.RT/doubleCast (c/== x))))))

#_(-def int-like? (and numerically-integer? numerically-int?))

#_(defn numerically
  [t]
  (assert (utr/class-type? t))
  (let [c (.-c ^ClassType t)]
    (case (.getName ^Class c)
      "java.lang.Byte"      numerically-byte?
      "java.lang.Short"     numerically-short?
      "java.lang.Character" numerically-char?
      "java.lang.Integer"   numerically-int?
      "java.lang.Long"      numerically-long?
      "java.lang.Float"     numerically-float?
      ;; TODO fix
      ;;"java.lang.Double"    numerically-double?
      (err! "Could not find numerical range type for class" {:c c}))))


(def primitive-number? (t/or #?@(:clj [p/short? p/int? p/long? p/float?]) p/double?))

(def numeric-primitive? p/numeric?)

(def numerically-integer-double? (t/and p/double? numerically-integer?))

(def numerically-integer-primitive? (t/and p/primitive? numerically-integer?))

(def numerically-int-double? (t/and p/double? numerically-int?))
(def ni-double? numerically-int-double?)

;; TODO excise?
(def std-integer? (t/or integer? #?(:cljs numerically-integer-double?)))

(def std-fixint? #?(:clj p/long? :cljs numerically-integer-double?))

;; ===== Conversion ===== ;;
;; Note that numeric-primitive conversions go here because they take as inputs and produce outputs
;; things that are within a numeric range.

;; TODO evaluate
(defnt ->num
  (^long [^boolean x] (if x 1 0))
  ([#{number? byte char} x] x))

;; TODO evaluate
(defn ->boolean-num [x] (if x 1 0))

;; TODO evaluate
#?(:clj
(defnt exactly
  ([#{decimal?} x]
    (-> x rationalize exactly))
  ([#{int? long?} x] (->bigint x))
  ([#{bigint? ratio?} x] x)))



;; ----- Byte ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >byte*
  "May involve non-out-of-range truncation."
  > p/byte?
  ([x p/byte?] x)
  ([x (t/- p/primitive? p/byte? p/boolean?)] (Primitive/uncheckedByteCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
(t/defn ^:inline >byte > #?(:clj p/byte? :cljs numerically-byte?)
  "Does not involve truncation or rounding."
         ([x #?(:clj p/byte? :cljs numerically-byte?)] x)
#?(:clj  ([x (t/and (t/- p/primitive? p/byte? p/boolean?) numerically-byte?)] (>byte* x))
   :cljs ([x (t/and p/double? numerically-byte?)] x))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-byte?)] (>byte* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-byte?)] (.byteValue x)))
#?(:clj  ([x (t/and ratio? numerically-byte?)] (-> x .bigIntegerValue .byteValue))))

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
(t/defn ^:inline >char > #?(:clj char? :cljs numerically-char?)
  "Does not involve truncation or rounding.
   For CLJS, returns not a String of length 1 but a numerically-char Number."
         ([x #?(:clj char? :cljs numerically-char?)] x)
#?(:clj  ([x (t/and (t/- primitive? char? boolean?) numerically-char?)] (>char* x))
   :cljs ([x (t/and double? numerically-char?)] x))
         ([x boolean?] (if x #?(:clj (char 1) :cljs 1) #?(:clj (char 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-char?)] (>char* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-char?)] (.charValue x)))
#?(:clj  ([x (t/and ratio? numerically-char?)] (-> x .bigIntegerValue .charValue))))

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
(t/defn ^:inline >short > #?(:clj short? :cljs numerically-short?)
  "Does not involve truncation or rounding."
         ([x #?(:clj short? :cljs numerically-short?)] x)
#?(:clj  ([x (t/and (t/- primitive? short? boolean?) numerically-short?)] (>short* x))
   :cljs ([x (t/and double? numerically-short?)] x))
         ([x boolean?] (if x #?(:clj (short 1) :cljs 1) #?(:clj (short 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-short?)] (>short* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-short?)] (.shortValue x)))
#?(:clj  ([x (t/and ratio? numerically-short?)] (-> x .bigIntegerValue .shortValue))))

;; ----- Int ----- ;;

;; TODO figure out how to use with goog.math.Integer/Long
#?(:clj
(t/defn ^:inline >int*
  "May involve non-out-of-range truncation."
  > int?
  (     [x int?] x)
  (^:in [x (t/- primitive? int? boolean?)] (clojure.lang.RT/uncheckedIntCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
(t/defn ^:inline >int
  "Does not involve truncation or rounding."
  > int?
         ([x int?] x)
#?(:clj  ([x (t/and (t/- primitive? int? boolean?) numerically-int?)] (>int* x))
   :cljs ([x (t/and double? numerically-int?)] x))
         ([x boolean?] (if x #?(:clj (int 1) :cljs 1) #?(:clj (int 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-int?)] (>int* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-int?)] (.intValue x)))
#?(:clj  ([x (t/and ratio? numerically-int?)] (-> x .bigIntegerValue .intValue))))

;; ----- Long ----- ;;

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
(t/defn ^:inline >long
  "Does not involve truncation or rounding."
  > #?(:clj long? :cljs numerically-long?)
         ([x #?(:clj long? :cljs numerically-long?)] x)
#?(:clj  ([x (t/and (t/- primitive? long? boolean?) numerically-long?)] (>long* x))
   :cljs ([x (t/and double? numerically-long?)] x))
         ([x boolean?] (if x 1 0))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt)
                    numerically-long?
                    ;; TODO This might be faster than `numerically-long?`
                  #_(t/fn [x ?] (nil? (.bipart x))))] (.lpart x)))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger)
                    numerically-long?
                    ;; TODO This might be faster than `numerically-long?`
                  #_(t/fn [x ?] (< (.bitLength x) 64)))] (.longValue x)))
#?(:clj  ([x (t/and ratio? numerically-long?)] (-> x .bigIntegerValue .longValue))))

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
(t/defn ^:inline >float > #?(:clj float? :cljs numerically-float?)
  "Does not involve truncation or rounding."
         ([x #?(:clj float? :cljs numerically-float?)] x)
#?(:clj  ([x (t/and (t/- primitive? float? boolean?) numerically-float?)] (>float* x))
   :cljs ([x (t/and double? numerically-float?)] x))
         ([x boolean?] (if x #?(:clj (float 1) :cljs 1) #?(:clj (float 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-float?)] (>float* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-float?)] (.floatValue x)))
#?(:clj  ([x (t/and ratio? numerically-float?)] (-> x .bigIntegerValue .floatValue))))

;; ----- Double ----- ;;

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
(t/defn ^:inline >double > double?
  "Does not involve truncation or rounding."
        ([x double?] x)
#?(:clj ([x (t/and (t/- primitive? double? boolean?) numerically-double?)] (>double* x)))
        ([x boolean?] (if x #?(:clj 1.0 :cljs 1) #?(:clj 1.0 :cljs 0)))
#?(:clj ([x (t/and (t/isa? clojure.lang.BigInt) numerically-double?)] (>double* (.lpart x))))
#?(:clj ([x (t/and (t/isa? java.math.BigInteger) numerically-double?)] (.doubleValue x)))
#?(:clj ([x (t/and ratio? numerically-double?)] (-> x .bigIntegerValue .doubleValue))))

;; ----- Unsigned ----- ;;

#?(:clj
(t/defn >unsigned
  {:adapted-from #{'ztellman/primitive-math 'gloss.data.primitives}}
  ([x p/byte?]  (Numeric/bitAnd (short 0xFF)       x))
  ([x p/short?] (Numeric/bitAnd (int   0xFFFF)     x))
  ([x p/int?]   (Numeric/bitAnd (long  0xFFFFFFFF) x))
  ([x p/long?]  (java.math.BigInteger. (int 1)
                (-> ^:val (ByteBuffer/allocate (int 8))
                    ^:val (.putLong x)
                    .array)))))

;; TODO TYPED awaiting `>long`
#_(:clj (t/defn ubyte>byte   [x long?   > long?] (-> x >byte   >long)))
#_(:clj (t/defn ushort>short [x long?   > long?] (-> x >short  >long)))
#_(:clj (t/defn uint>int     [x long?   > long?] (-> x >int    >long)))
#_(:clj (t/defn ulong>long   [x bigint? > long?] (-> x >bigint >long)))


;; ----- Ratios ----- ;;

;; TODO TYPED >double
#_(:clj
(t/defn rationalize
  "Outputs the rational value of `n`."
  {:incorporated {'clojure.lang.Numbers/rationalize "9/2018"}}
  > (t/isa? java.lang.Number)
  ([x (t/or p/float? p/double?)]
    (rationalize (BigDecimal/valueOf (p/>double x))))
  ([x (t/isa? BigDecimal)]
    (let [bv (.unscaledValue x)
          scale (.scale x)]
  		(if (< scale 0)
  			  (BigInt/fromBigInteger (.multiply bv (.pow BigInteger.TEN (- scale))))
  			  (Numbers/divide bv (.pow BigInteger.TEN scale)))))
  ([x (t/isa? java.lang.Number)] x)))

;; TODO TYPED finish
#_(t/defn >ratio > ratio?
  #?(:clj ([x ??] (>ratio x 1)))
  #?(:clj ([x ??, y ??]
            (whenf (rationalize (/ x y))
              (fn-not core/ratio?)
              #(clojure.lang.Ratio. (->big-integer %) java.math.BigInteger/ONE)))))


;; ===== General ===== ;;

(t/defn ^:inline numerator > numerically-integer?
        ([x numerically-integer? > (t/type x)] x)
#?(:clj ([x ratio?               > (t/assume java-bigint?)] (.numerator x))))

(t/defn ^:inline denominator > numerically-integer?
        ([x numerically-integer? > (t/type x)] (>one-of-type x))
#?(:clj ([x ratio?               > (t/assume java-bigint?)] (.denominator x))))




(t/defn >std-fixint
  "Converts input to a `std-fixint?` in a way that may involve truncation or rounding."
  > std-fixint?
#?(:cljs ([x double? > (t/assume std-fixint?)] (js/Math.round x))))

;; TODO TYPED
(t/defn read-rational
  "Create cross-platform literal rational numbers from decimal, without intermediate inexact
   (e.g. float/double) representation.

   Example:
   #r 2.712 -> (rationalize 2.712M)"
  {:todo #{"Support exponent notation e.g. 2.313E7 | 2.313e7"}}
  [r string?]
  (let [r-str (cond (string? r)
                    r
                    (symbol? r)
                    (do (assert (-> r namespace nil?))
                        (assert (-> r name first (= \r)))
                        (->> r name rest (apply str))))
        minus-ct (->> r-str (filter #(= % \-)) count)
        _        (assert (#{0 1} minus-ct))
        r-str    (case minus-ct
                   0 r-str
                   1 (do (assert (-> r-str first (= \-)))
                         (->> r-str rest (apply str))))
        [integral-str decimal-str :as split] (str/split r-str #"\.")
        _ (when (-> split count (> 2))
            (throw (ex-info "Number cannot have more than one decimal point" {:num r-str})))
        _ (doseq [s split]
            (when-not (every? #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9} s)
              (throw (ex-info "Number must have only numeric characters" {:num s}))))
        integral (read-string integral-str) ; TODO we should just pass the raw string to the ratio
        decimal  (read-string decimal-str) ; TODO we should just pass the raw string to the ratio
        scale    (if decimal
                     (#?(:clj Math/pow :cljs js/Math.pow) 10 (count decimal-str))
                     1)]
    (* (if (= minus-ct 1) -1 1)
       (>ratio (+ (* scale integral) (or decimal 0))
               scale))))
