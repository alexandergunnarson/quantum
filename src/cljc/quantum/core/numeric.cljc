(ns
  ^{:doc "Useful numeric functions. Floor, ceil, round, sin, abs, neg, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.numeric
  (:refer-clojure :exclude
    [* + - / < > <= >= == rem inc dec zero? min max format])
  (:require-quantum [ns logic type fn macros err log pconvert])
  #?(:clj (:import [java.nio ByteBuffer]
                   [quantum.core Numeric]))) ; loops?

(def overridden-fns
  '#{+ - * /
     dec inc
     > < <= >=
     zero?
     rem min max})

(def override-fns? (atom false))
;_____________________________________________________________________
;==================={        OPERATORS         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(macros/variadic-proxy +                quantum.core.Numeric/add)

#_(defmacro subtract
  [x])

#_(defnt -
  ([^primitive? x] (quantum.core.Numeric/negate x))
  ([x] ))

(macros/variadic-proxy -                quantum.core.Numeric/subtract
  "A primitive macro version of |-|" (fn [x] (quote+ (quantum.core.Numeric/negate ~x))))

(macros/variadic-proxy *                quantum.core.Numeric/multiply)
(macros/variadic-proxy /                quantum.core.Numeric/divide  )

(defalias +* unchecked-add     )
(defalias -* unchecked-subtract)
(defalias ** unchecked-multiply)

#_(defnt ^BigInteger ->bigint
  ([^BigInteger x] x)
  ([^BigInt     x] (.toBigInteger x))
  ([#{(- number? BigInteger BigInt)} x]
    (-> x ->long (BigInteger/valueOf))))

#_(defalias bigint ->bigint)

#_(defnt ^BigDecimal ->bigdec
  ([^BigDecimal x] x)
  ([^BigInt x]
      (if (-> x (.bipart) nil?              )
          (-> x (.lpart ) BigDecimal/valueOf)
          (-> x (.bipart) (BigDecimal.)     )))
  ([^BigInteger x] (BigDecimal. x))
  ([#{(- decimal? :curr)} x] (BigDecimal. x))
  ([^Ratio x] (/ (BigDecimal. (.numerator x)) (.denominator x)))
  ([#{(- number? :curr)} x] (BigDecimal/valueOf x)))

#_(defalias bigdec ->bigdec)

#_(defnt ^Number rationalize
  ([#{float? double?} x]
    (-> x ->bigdec rationalize))
  ([^BigDecimal x]
    (let [^BigInteger bv (.unscaledValue x)
          ^long scale (.scale x)] ; technically int
      (if (< scale 0)
          (BigInt/fromBigInteger bv.multiply(BigInteger.TEN.pow(-scale)));
          (/ bv (. BigInteger/TEN pow scale)))))
  ([#{(- number? :curr)} x] x))

; (/ x 4) = (& x 3)

; What about the other unchecked?
; unchecked-byte
; unchecked-char
; unchecked-double
; unchecked-float
; unchecked-int
; unchecked-long
; unchecked-short
; unchecked-negate
; unchecked-divide-int
; unchecked-add-int
; unchecked-dec-int
; unchecked-divide-int
; unchecked-inc-int
; unchecked-multiply-int
; unchecked-negate-int
; unchecked-remainder-int
; unchecked-subtract-int

;_____________________________________________________________________
;==================={   UNARY MATH OPERATORS   }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(defmacro dec [x] `(Numeric/dec ~x))

#_(defnt ^long dec
  {:warn-on-boxed false
   :todo ["Take out auto quote generator. |dec'| is a different operation"]}
  ([^pinteger? n] (clojure.lang.Numbers/minus n 1)))

(defnt ^long dec*
  {:warn-on-boxed false}
  ([^pinteger? n] (clojure.lang.Numbers/unchecked_minus n 1)))

(defmacro inc [x] `(Numeric/inc ~x))

#_(defnt ^long inc
  {:warn-on-boxed false
   :todo ["Take out auto quote generator. |inc'| is a different operation"]}
  ([^pinteger? n] (clojure.lang.Numbers/add n 1)))

(defnt ^long inc*
  {:warn-on-boxed false}
  ([^pinteger? n] (clojure.lang.Numbers/unchecked_add n 1)))
;_____________________________________________________________________
;==================={        PREDICATES        }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(macros/variadic-predicate-proxy >      quantum.core.Numeric/gt )
(macros/variadic-predicate-proxy <      quantum.core.Numeric/lt )
(macros/variadic-predicate-proxy <=     quantum.core.Numeric/lte)
(macros/variadic-predicate-proxy >=     quantum.core.Numeric/gte)
(macros/variadic-predicate-proxy ==     quantum.core.Numeric/eq )
(macros/variadic-predicate-proxy not==  quantum.core.Numeric/neq
  "A primitive macro complement of |==|")

(defmacro zero?    [x]     `(Numeric/isZero  ~x))

(defmacro rem      [n div] `(Numeric/rem     ~n ~div))
(macros/variadic-proxy min              quantum.core.Numeric/min)
(macros/variadic-proxy max              quantum.core.Numeric/max)

#?(:clj
(when-not @override-fns?
  (doseq [sym overridden-fns]
    (let [qualified-sym (symbol "core" (name sym))]
      (eval (quote+ (defalias ~sym ~qualified-sym)))))))


(defn within-tolerance? [n total tolerance]
  (and (>= n (- total tolerance))
       (<= n (+ total tolerance))))

; https://github.com/clojure/math.numeric-tower/
(defn sign [n]  (if (neg? n) -1 1))
(def  nneg?     (fn-not neg?))
(def  pos-int?  (fn-and integer? pos?))
(def  nneg-int? (fn-and integer? nneg?))
(def  neg       (f*n -))
(def  abs       (whenf*n neg? neg))
(def  int-nil   (whenf*n nil? (constantly 0)))

; For units
(def ^:const ten              (long   10    ))
(def ^:const hundred          (long   100   ))
(def ^:const thousand         (long   1000  ))
(def ^:const ten-thousand     (long   10000 ))
(def ^:const hundred-thousand (long   100000))
(def ^:const million          (long   1E6   ))
(def ^:const billion          (long   1E9   ))
(def ^:const trillion         (long   1E12  ))
(def ^:const quadrillion      (long   1E15  ))
(def ^:const quintillion      (long   1E18  )) ; + exa | - atto
(def ^:const sextillion       (bigint 1E21  ))
(def ^:const septillion       (bigint 1E24  ))
(def ^:const octillion        (bigint 1E27  ))
(def ^:const nonillion        (bigint 1E30  ))
(def ^:const decillion        (bigint 1E33  ))

(defn evenly-divisible-by? [a b] (= 0 (rem a b)))

(defn exp
  {:todo "Performance"}
  [x n]
#?(:clj  (java.lang.Math/pow x n)
   :cljs (.pow js/Math       x n)))

#?(:clj
(defn exp'
  {:todo "Performance"}
  [x n]
  (loop [acc 1 n n]
    (if (zero? n) acc
        (recur (*' x acc) (dec* n))))))

#?(:clj
  (defn rationalize+ [n]
    (-> n rationalize
        (whenf bigint? long))))

(defn floor [x]
  #?(:clj  (java.lang.Math/floor x)
     :cljs (.floor js/Math       x)))

(defn ceil [x]
  #?(:clj  (java.lang.Math/ceil x)
     :cljs (.ceil js/Math       x)))

; TODO macro to reduce repetitiveness here
(defn safe+
  ([a    ] (int-nil a))
  ([a b  ] (+ (int-nil a) (int-nil b)))
  ([a b c] (+ (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce (MWA +)))))

(defn safe*
  ([a    ] (int-nil a))
  ([a b  ] (* (int-nil a) (int-nil b)))
  ([a b c] (* (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce (MWA *)))))

(defn safe-
  ([a] (neg (int-nil a)))
  ([a b] (- (int-nil a) (int-nil b)))
  ([a b c] (- (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce (MWA -)))))

(defn safediv
  ([a b  ] (/ (int-nil a) (int-nil b)))
  ([a b c] (/ (int-nil a) (int-nil b) (int-nil c)))
  ([a b c & args] (->> (conj args c b a) (map int-nil) (reduce (MWA /)))))

#?(:clj
  (defn round
    "Probably deprecated; use:
     |(with-precision <decimal-places> (bigdec <number>))|"
    {:todo ["Port to cljs"]}
    [num-0 & {:keys [type to] :or {to 0}}]
    (let [round-type
            (if (nil? type)
                (. BigDecimal ROUND_HALF_UP)
                (case type
                  :unnecessary BigDecimal/ROUND_UNNECESSARY
                  :ceiling     BigDecimal/ROUND_CEILING
                  :up          BigDecimal/ROUND_UP
                  :half-up     BigDecimal/ROUND_HALF_UP
                  :half-even   BigDecimal/ROUND_HALF_DOWN
                  :half-down   BigDecimal/ROUND_HALF_DOWN
                  :down        BigDecimal/ROUND_DOWN
                  :floor       BigDecimal/ROUND_FLOOR))]
      (.setScale (bigdec num-0) ^Integer to round-type))))

(defn rcompare
  "Reverse comparator."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [x y] (compare y x))

(defn greatest
  "Returns the 'greatest' element in coll in O(n) time."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (pos? (comparator a b)) b a)))
      coll))) ; almost certainly can implement this with /fold+/

(defn least
  "Returns the 'least' element in coll in O(n) time."
  ^{:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (neg? (comparator a b)) b a)))
      coll)))

(defn greatest-or [a b else]
  (cond (> a b) a
        (> b a) b
        :else else))

(defn least-or [a b else]
  (cond (< a b) a
        (< b a) b
        :else else))

(defn approx? [tolerance a b]
  (-> (- (int-nil a) (int-nil b)) abs (< tolerance)))

(defn sin [n]
  #?(:clj  (java.lang.Math/sin n)
     :cljs (.sin js/Math       n)))

(defnt exactly
  #?(:clj
 ([^integer?           n] (bigint      n)))
 ([^decimal?           n] (rationalize n))
 ([^clojure.lang.Ratio n] n))

; (defn accounting-display)
;___________________________________________________________________________________________________________________________________
;=================================================={       TYPE-CASTING       }=====================================================
;=================================================={                          }=====================================================


;___________________________________________________________________________________________________________________________________
;=================================================={         DISPLAY          }=====================================================
;=================================================={                          }=====================================================
(def display-num (fn-> double (round :to 2)))
(defn format [n type]
  (condp = type
    :dollar
      (->> n display-num (str "$"))
    ;:accounting
    (throw+ (Err. nil "Unrecognized format" type))))