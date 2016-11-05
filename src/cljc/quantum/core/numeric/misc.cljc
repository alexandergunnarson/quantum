(ns quantum.core.numeric.misc
          (:refer-clojure :exclude [quot rem mod neg? zero? <= -'])
          (:require
            [#?(:clj  clojure.core
                :cljs cljs.core   )     :as core  ]
            [quantum.core.error         :as err
              :refer [TODO]]
            [quantum.core.macros
              :refer        [#?@(:clj [defnt defnt'])]
              :refer-macros [defnt]]
            [quantum.core.vars
              :refer        [#?@(:clj [defalias def-])]
              :refer-macros [defalias def-]]
            [quantum.core.compare
              :refer [#?@(:clj [<=])]
              :refer-macros [<=]]
            [quantum.core.numeric.operators
              :refer        [#?@(:clj [-' abs'])]
              :refer-macros [-' abs']]
            [quantum.core.numeric.predicates
              :refer        [#?@(:clj [neg? zero?])]
              :refer-macros [neg? zero?]])
  #?(:clj (:import [net.jafama FastMath])))

#?(:clj  (defmacro rem [n div] `(Numeric/rem ~n ~div))
   :cljs (defnt rem
           ([^number?                           x n] (core/rem x n))
           ([^com.gfredericks.goog.math.Integer x n] (.modulo  x n))))

#?(:clj  (defalias mod core/mod) ; TODO fix
   :cljs (defnt mod
           ([^number? x n] (core/mod x n))
           ([^com.gfredericks.goog.math.Integer x n]
             (let [y (rem x n)]
               (cond-> y (.isNegative y) (.add n))))))

#?(:clj
(defnt' ieee-rem "The remainder operation on two arguments as prescribed by the IEEE 754 standard"
  (^double [^double x ^double y] (Math/IEEEremainder x y))))

#?(:clj  (defalias quot core/quot) ; TODO fix
   :cljs (defn quot
           [x n]
           (TODO "fix")
           {:pre [(integer? x) (integer? n)]}
           (.divide x n)))


#?(:clj  (defnt' hypot "(sqrt (^ x 2) (^ y 2)) without intermediate over/underflow"
           (^double [^double x ^double y] (Math/hypot x y)))
   :cljs (defn hypot [x] (js/Math.hypot x)))

#?(:clj
(defnt' hypot*
  "(sqrt (^ x 2) (^ y 2)) without intermediate over/underflow"
  {:performance ["18.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x ^double y] (FastMath/hypot x y))))

(defn sign [n] (if (neg? n) -1 1))

#?(:clj (defnt' sign'
          "Zero if the argument is zero,
           1.0 if the argument is greater than zero,
           -1.0 if the argument is less than zero."
          (^double [^double x] (Math/signum x))
          (^float  [^float  x] (Math/signum x)))
   :cljs (defnt sign' [^number? x] (js/Math.sign x)))

#?(:clj
(defnt' with-sign "Returns @x with the sign of @y."
  (^double [^double x ^double y] (Math/copySign x y))
  (^float  [^float  x ^float  y] (Math/copySign x y))))

#?(:clj
(defnt' scalb
  "Returns (x * 2) ^ y, rounded as if performed by a single correctly rounded
   floating-point multiply to a member of the double value set."
  (^double [^double x ^int y] (Math/scalb x y))
  (^float  [^float  x ^int y] (Math/scalb x y))))

#?(:clj
(defnt' ulp "Size of an ulp (?) of @x"
  (^double [^double x] (Math/ulp x))
  (^float  [^float  x] (Math/ulp x))))

; (defn whole? [n]
;   (assert (instance? Double n))
;   (= (mod n 1) 0.0))

#?(:clj  (defn leading-zeros [x] (TODO))
   :cljs (defn leading-zeros [x] (js/Math.clz32 x)))

(def- two-to-fifty-three
  (apply core/* (repeat 53 2)))

(def- minus-two-to-fifty-three
  (-' two-to-fifty-three))

(defn native-integer?
  "TODO what does this even mean?"
  [n]
  (and (number? n)
       (<= minus-two-to-fifty-three
           n
           two-to-fifty-three)
       (core/integer? n)))

(defn power-of-two?
  {:implemented-by '#{org.apache.commons.math3.util.ArithmeticUtils/isPowerOfTwo}}
  [n] (TODO))

; TODO look at org.apache.commons.math3.util.ArithmeticUtils/gcd
; Gets the greatest common divisor of the absolute value of two numbers, using the "binary gcd" method which avoids division and modulo operations.
(defn gcd
  "(gcd a b) computes the greatest common divisor of a and b
   (using Euclid's algorithm)"
  {:O "O(n^3)"}
  ([a b]
  (if (zero? b)
      a
      (recur b (mod a b))))
  ([a b & args]
    (reduce gcd (gcd a b) args)))

(defalias gcf gcd)

(defn lcm
  "Returns the least common multiple of the absolute value of
   `a` and `b`."
  [a b]
  (let [a' (abs' a) b' (abs' b)] ; TODO use `abs`
    (* b' (/ a' (gcd a' b')))))
