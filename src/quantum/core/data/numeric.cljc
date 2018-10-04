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
         (:refer-clojure :exclude
           [decimal? denominator integer? number? numerator ratio?])
         (:require
           [clojure.core                      :as core]
           [clojure.string                    :as str]
  #?(:cljs goog.math.Integer)
  #?(:cljs goog.math.Long)
           [quantum.core.data.primitive       :as p]
         #_[quantum.core.logic
             :refer [whenf fn-not fn=]]
           [quantum.core.type                 :as t]
           ;; TODO TYPED excise reference
           [quantum.untyped.core.vars         :as var
             :refer [defalias]]))

;; ===== Integers ===== ;;

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

;; TODO TYPED `>long`
#_(:clj
(t/defn >java-bigint > java-bigint?
  ([x java-bigint?] x)
  ([x clj-bigint? > (t/assume java-bigint?)] (.toBigInteger x))
  ([;; TODO TYPED `(- number? BigInteger BigInt)`
    x (t/or p/short? p/int? p/long?) > (t/assume java-bigint?)] ; TODO BigDecimal
    (-> x p/>long BigInteger/valueOf))))

#?(:cljs
(t/defn >bigint > bigint?
  ([x bigint?] x)
  ([x p/double?] (-> x (.toString) >bigint))))

;; ===== Decimals ===== ;;

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

;; ===== Precision ===== ;;

(var/def fixnum? "The set of all fixed-precision numbers."
  (t/or fixint? fixdec?))

(var/def bignum? "The set of all 'big' (arbitrary-precision) numbers."
  (t/or fixint? fixdec?))

;; ===== Ratios ===== ;;

(def ratio? #?(:clj  (t/isa? clojure.lang.Ratio)
                     ;; TODO bring in implementation per the ns docstring
               :cljs t/none?))

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

(t/defn ^:inline >zero-of-type #_> #_zero?
        ([x p/byte?        > (t/type x)] Numeric/byte0)
        ([x p/short?       > (t/type x)] Numeric/short0)
        ([x p/char?        > (t/type x)] Numeric/char0)
        ([x p/int?         > #?(:clj (type x) :cljs (t/assume (t/type x)))]
           #?(:clj Numeric/int0 :cljs goog.math.Integer/ZERO))
        ([x p/long?        > #?(:clj (type x) :cljs (t/assume (t/type x)))]
           #?(:clj 0 :cljs goog.math.Long/ZERO))
        ([x p/float?       > (t/type x)] Numeric/float0)
        ([x p/double?      > (t/type x)] 0.0)
#?(:clj ([x p/java-bigint? > (t/type x)] java.math.BigInteger/ZERO))
#?(:clj ([x p/clj-bigint?  > (t/type x)] clojure.lang.BigInt/ZERO)))

(t/defn ^:inline >one-of-type #_> #_one?
        ([x p/byte?        > (t/type x)] Numeric/byte1)
        ([x p/short?       > (t/type x)] Numeric/short1)
        ([x p/char?        > (t/type x)] Numeric/char1)
        ([x p/int?         > #?(:clj (type x) :cljs (t/assume (t/type x)))]
           #?(:clj Numeric/int1 :cljs goog.math.Integer/ONE))
        ([x p/long?        > #?(:clj (type x) :cljs (t/assume (t/type x)))]
           #?(:clj 1 :cljs goog.math.Long/ONE))
        ([x p/float?       > (t/type x)] Numeric/float1)
        ([x p/double?      > (t/type x)] 1.0)
#?(:clj ([x p/java-bigint? > (t/type x)] java.math.BigInteger/ONE))
#?(:clj ([x p/clj-bigint?  > (t/type x)] clojure.lang.BigInt/ONE)))

(t/defn >one-of-type)

(t/defn ^:inline numerator > numerically-integer?
        ([x numerically-integer? > (t/type x)] x)
#?(:clj ([x ratio?               > (t/assume java-bigint?)] (.numerator x))))

(t/defn ^:inline denominator > numerically-integer?
        ([x numerically-integer? > (t/type x)] (>one-of-type x))
#?(:clj ([x ratio?               > (t/assume java-bigint?)] (.denominator x))))

;; ===== Likenesses ===== ;;


;; TODO incorporate
(defn ^boolean numerically-integer?
  "Returns true if n is a JavaScript number with no decimal part."
  [n]
  (and (number? n)
       (not ^boolean (js/isNaN n))
       (not (identical? n js/Infinity))
       (== (js/parseFloat n) (js/parseInt n 10))))

#_(def numerically-integer?        (or integer? (and decimal? (>expr unum/integer-value?))))

#_(def numeric-primitive?          (and primitive? (not boolean?)))

#_(def numerically-byte?
    (and numerically-integer? (>expr (c/fn [x] (c/<= -128                 x 127)))))

#_(def numerically-short?
    (and numerically-integer? (>expr (c/fn [x] (c/<= -32768               x 32767)))))

#_(def numerically-char?
    (and numerically-integer?  (>expr (c/fn [x] (c/<=  0                   x 65535)))))

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

(def number? (t/or #?@(:clj  [(t/isa? java.lang.Number)]
                       :cljs [integer? decimal? ratio?])))

(def primitive-number? (t/or #?@(:clj [p/short? t/int? t/long? t/float?]) t/double?))

(var/def numeric?
  "Something 'numeric' is something that may be treated as a number but may not actually *be* one."
  (t/or number? #?(:clj p/char?)))

(def numeric-primitive? p/numeric?)

(def numerically-integer-double? (t/and p/double? numerically-integer?))

(def numerically-integer-primitive? (t/and p/primitive? numerically-integer?))

(def numerically-int-double? (t/and p/double? numerically-int?))
(def ni-double? numerically-int-double?)

;; TODO excise?
(def std-integer? (t/or integer? #?(:cljs numerically-integer-double?)))

(def std-fixint? #?(:clj long? :cljs numerically-integer-double?))

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
        integral (read-string integral-str)
        decimal  (read-string decimal-str)
        scale    (if decimal
                     (#?(:clj Math/pow :cljs js/Math.pow) 10 (count decimal-str))
                     1)]
    (* (if (= minus-ct 1) -1 1)
       (->ratio (+ (* scale integral) (or decimal 0))
                scale))))
