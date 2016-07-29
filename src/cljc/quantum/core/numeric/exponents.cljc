(ns quantum.core.numeric.exponents
          (:refer-clojure :exclude [+ *])
          (:require
            [#?(:clj  clojure.core
                :cljs cljs.core   )     :as core  ]
            [quantum.core.error :as err
              :refer [TODO]            ]
            [quantum.core.macros
              :refer        [#?@(:clj [defnt defnt'])]
              :refer-macros [defnt]]
            [quantum.core.vars
              :refer        [#?@(:clj [defalias defmalias])]
              :refer-macros [defalias]]
            [quantum.core.numeric.operators
              :refer        [#?@(:clj [+ * dec* div*])]
              :refer-macros [+ * dec* div*]])
  #?(:clj (:import [net.jafama FastMath])))

; ===== EXPONENTS ===== ;

#?(:clj (defnt' pow-
          {:todo ["Performance" "Rename"]}
          [#{byte #_char short int float double} x #{long? double?} n]
          (loop [acc (Long. 1) nn n]
            (if (<= (double nn) 0) acc
                (recur (* x acc) (dec* nn)))))
   :cljs (defn pow- [x n] (TODO)))

(defn pow'
  "Strict |pow|"
  [x y]
  (TODO))

#?(:clj  (defnt pow "x ^ y"
           (^double ^:intrinsic [^double   x ^double y] (Math/pow x y))
           (^double             [#{byte short int long float} x y]
                                  (pow (core/double x) (core/double y))))
   :cljs (defn pow "x ^ y" [x] (js/Math.pow x)))

#?(:clj
(defnt' pow*
  "x ^ y"
  {:performance ["2.7 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x ^double y] (FastMath/pow x y))))

#?(:clj
(defnt' expm1*
  "Much more accurate than exp(value)-1 for arguments (and results) close to zero."
  {:performance ["6.6 times faster than java.lang.Math"
                 "Worst case 5E-14 difference"]}
  (^double [^double x] (FastMath/expm1 x))))

#?(:clj
(defnt' get-exp "Unbiased exponent used in @x"
  (^int [^double x] (Math/getExponent x))
  (^int [^float  x] (Math/getExponent x))))

; ===== INVERSE EXPONENTS (ROOTS) ===== ;

#?(:clj  (defnt' sqrt
           (^double ^:intrinsic [^double x] (Math/sqrt x)))
   :cljs (defn sqrt [x] (js/Math.sqrt  x)))

#?(:clj  (defnt' cbrt "cube root"
           (^double [^double x] (Math/cbrt x)))
   :cljs (defn cbrt "cube root" [x] (js/Math.cbrt x)))

#?(:clj
(defnt cbrt*
  "returns angle theta"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/cbrt x))))

; ===== CONVERSE EXPONENTS (LOGARITHMS) ===== ;

#?(:clj  (defnt' e-exp "Euler's number (e) raised to the power of @x"
           (^double ^:intrinsic [^double x] (Math/exp x)))
   :cljs (defn e-exp
           "Euler's number (e) raised to the power of @x"
           [x] (js/Math.exp x)))

#?(:clj
(defnt' e-exp*
  "Euler's number (e) raised to the power of @x"
  {:performance ["4.6 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/exp x))))

#?(:clj  (defnt' log-e "Natural logarithm"
           (^double ^:intrinsic [^double x] (Math/log x)))
   :cljs (defn log-e "Natural logarithm"
           [x] (js/Math.log x)))

(defalias ln log-e)

#?(:clj  (defn log-2 [x] (TODO))
   :cljs (defn log-2 [x] (js/Math.log2 x)))

#?(:clj
(defnt' log-e*
  "Natural logarithm"
  {:performance ["1.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x] (FastMath/log x))))

#?(:clj  (defnt' log-10 "Logarithm, base 10"
           (^double ^:intrinsic [^double x] (Math/log10 x)))
   :cljs (defn log-10 "Logarithm, base 10"
           [x] (js/Math.log10 x)))

#?(:clj
(defnt' log-10*
  "Logarithm, base 10"
  {:performance ["2.1 times faster than java.lang.Math"
                 "Worst case 6E-14 difference"]}
  (^double [^double x] (FastMath/log10 x))))

#?(:clj
(defnt' log1p*
  "Much more accurate than log(1+value) for arguments (and results) close to zero."
  {:performance ["6.5 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/log1p x))))

(#?(:clj defnt' :cljs defn) log-
  {:todo ["Need to intelligently determine, at compile time if possible, whether
           @x is e, 2, or 10 and choose the appropriate fn."]}
  ([#?(:clj #{double}) x #?(:clj #{double}) base] ; arbitrary to choose ln vs. log-10
    (div* (ln x) (ln base))))

#?(:clj
(defmacro log [base x] ; TODO do ln'
  `(log- (double ~x) (double ~base))))
;pow'
; Only works with integers larger than zero.
; private double pow'(double d, int exp) {
;     double r = d;
;     for(int i = 1; i<exp; i++) {
;         r *= d;
;     }
;     return r;
; }