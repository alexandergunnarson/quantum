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
           [#?@(:cljs [-compare]) decimal? denominator integer? number? numerator ratio?])
         (:require
           [clojure.core                      :as core]
           [clojure.string                    :as str]
           [quantum.core.data.primitive       :as p]
           [quantum.core.data.string          :as dstr]
           [quantum.core.logic
             :refer [whenf fn-not fn=]]
           [quantum.core.type                 :as t
             :refer [defnt]]
           [quantum.core.vars                 :as var
             :refer [defalias]])
         (:import
           [clojure.lang BigInt Numbers]
           [java.math BigDecimal BigInteger])
#?(:cljs (:require-macros
           [quantum.core.data.numeric :as self])))


#?(:clj  (defalias numerator core/numerator)
   :cljs (t/defn numerator))

#?(:clj  (defalias denominator core/denominator)
   :cljs (t/defn denominator))

#?(:clj  (defalias ratio? core/ratio?)
   :cljs (defn ratio? [x] (instance? Ratio x)))




;; ===== Integers ===== ;;

#?(:clj (def big-integer? (t/isa? BigInteger)))

#?(:clj (def clj-bigint? (t/isa? clojure.lang.BigInt)))

(def bigint? #?(:clj  (t/or clj-bigint? big-integer?)
                      ;; TODO bring in implementation per the ns docstring
                :cljs t/none?))

;; Incorporated `clojure.lang.Util/isInteger`
;; Incorporated `clojure.core/integer?`
;; Incorporated `cljs.core/integer?`
(def integer? (t/or #?@(:clj [p/byte? p/short?]) p/int? p/long? bigint?))

;; Incorporated `clojure.core/int?`
;; Incorporated `cljs.core/int?`
(var/def fixed-integer? "The set of all fixed-precision integers."
  (t/or ?@(:clj [p/byte? p/short?]) p/int? p/long?))

#?(:clj
(t/defn >big-integer > big-integer?
  ([x big-integer?] x)
  ([x clj-bigint? > (t/* big-integer?)] (.toBigInteger x))
  ([;; TODO TYPED `(- number? BigInteger BigInt)`
    x (t/or p/short? p/int? p/long?) > (t/* big-integer?)] ; TODO BigDecimal
    (-> x p/>long (BigInteger/valueOf)))))

#?(:cljs
(t/defn >bigint > bigint?
  ([x bigint?] x)
  ([x p/double?] (-> x (.toString) >bigint))))

;; ===== Decimals ===== ;;

(def bigdec? #?(:clj  ;; TODO bring in a better implementation per the ns docstring?
                      (t/isa? BigDecimal)
                      ;; TODO bring in implementation per the ns docstring
                :cljs t/none?))

;; ===== Ratios ===== ;;

(def ratio? (t/isa? #?(:clj clojure.lang.Ratio :cljs quantum.core.data.numeric.Ratio)))

#?(:clj
(defnt rationalize
  "Outputs the rational value of `n`."
  {:adapted-from 'clojure.lang.Numbers/rationalize}
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

(defnt >ratio > ratio?
  ([x ??] (>ratio x #?(:clj 1 :cljs int/ONE)))
  ([x ??, y ??]
    #?(:clj  (whenf (rationalize (/ x y))
                    (fn-not core/ratio?)
                    #(clojure.lang.Ratio. (->big-integer %) java.math.BigInteger/ONE))
       :cljs (let [x  (>bigint x)
                   y  (>bigint y)
                   d  (gcd x y)
                   x' (.divide x d)
                   y' (.divide y d)]
               (if (.isNegative y')
                   (Ratio. (.negate x') (.negate y'))
                   (Ratio. x' y'))))))

;; ===== General ===== ;;

(def decimal? (or #?(:clj p/float?) p/double? bigdec?))

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
(def ni-double? numerically-integer-double?)

(def numerically-integer-primitive? (t/and p/primitive? numerically-integer?))

(def std-integer? (t/or integer? #?(:cljs numerically-integer-double?)))

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
