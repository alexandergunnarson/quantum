(ns quantum.core.data.numeric
         (:refer-clojure :exclude
           [#?@(:cljs [-compare]) decimal? denominator integer? number? numerator ratio?
            read-string])
         (:require
           [clojure.core                      :as core]
           [clojure.string                    :as str]
           [clojure.tools.reader
             :refer [read-string]]
  #?(:cljs [com.gfredericks.goog.math.Integer :as int])
           [quantum.core.data.primitive       :as p]
           [quantum.core.data.string          :as dstr]
           [quantum.core.logic
             :refer [whenf fn-not fn=]]
           [quantum.core.type                 :as t
             :refer [defnt]]
           [quantum.core.vars
             :refer [defalias]])
         (:import
           [clojure.lang BigInt Numbers]
           [java.math BigDecimal BigInteger])
#?(:cljs (:require-macros
           [quantum.core.data.numeric :as self])))

;; ===== Integers ===== ;;

#?(:clj (def big-integer? (t/isa? BigInteger)))

#?(:clj (def clj-bigint? (t/isa? clojure.lang.BigInt)))

(def bigint? #?(:clj  (t/or clj-bigint? big-integer?)
                :cljs (t/isa? com.gfredericks.goog.math.Integer)))

(def integer? (t/or #?@(:clj [p/byte? p/short? p/int? p/long?]) bigint?))

#?(:clj
(defnt >big-integer > big-integer?
  ([x big-integer?] x)
  ([x clj-bigint? > (t/* big-integer?)] (.toBigInteger x))
  ([; TODO TYPED `(- number? BigInteger BigInt)`
    x (t/or p/short? p/int? p/long?) > (t/* big-integer?)] ; TODO BigDecimal
    (-> x p/>long (BigInteger/valueOf)))))

#?(:cljs
(defnt >bigint > bigint?
  ([x bigint?] x)
  ([x dstr/string?] (int/fromString x))
  ([x p/double?] (-> x (.toString) >bigint))))

;; ===== Decimals ===== ;;

(def bigdec? #?(:clj (t/isa? BigDecimal) :cljs t/none?))

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

#_(-def integer-value?              (or integer? (and decimal? (>expr unum/integer-value?))))

#_(-def numeric-primitive?          (and primitive? (not boolean?)))

#_(-def numerically-byte?           (and integer-value? (>expr (c/fn [x] (c/<= -128                 x 127)))))
#_(-def numerically-short?          (and integer-value? (>expr (c/fn [x] (c/<= -32768               x 32767)))))
#_(-def numerically-char?           (and integer-value? (>expr (c/fn [x] (c/<=  0                   x 65535)))))
#_(-def numerically-unsigned-short? numerically-char?)
#_(-def numerically-int?            (and integer-value? (>expr (c/fn [x] (c/<= -2147483648          x 2147483647)))))
#_(-def numerically-long?           (and integer-value? (>expr (c/fn [x] (c/<= -9223372036854775808 x 9223372036854775807)))))
#_(-def numerically-float?          (and number?
                                          (>expr (c/fn [x] (c/<= -3.4028235E38 x 3.4028235E38)))
                                          (>expr (c/fn [x] (-> x #?(:clj clojure.lang.RT/floatCast :cljs c/float) (c/== x))))))
#_(-def numerically-double?         (and number?
                                          (>expr (c/fn [x] (c/<= -1.7976931348623157E308 x 1.7976931348623157E308)))
                                          (>expr (c/fn [x] (-> x clojure.lang.RT/doubleCast (c/== x))))))

#_(-def int-like?                   (and integer-value? numerically-int?))

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
