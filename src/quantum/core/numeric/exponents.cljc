(ns quantum.core.numeric.exponents
  (:require
    [clojure.core                   :as core]
    [quantum.core.numeric.operators :as no]
    [quantum.core.type              :as t]
    [quantum.core.vars
      :refer [defalias]]
    ;; TODO TYPED excise this reference
    [quantum.untyped.core.error
      :refer [TODO]])
#?(:clj
  (:import
    [quantum.core Numeric]
    [net.jafama FastMath])))

; ===== EXPONENTS ===== ;

#?(:clj (defnt' pow-
          {:todo ["Performance" "Rename"]}
          [#{byte #_char short int float double} x #{long? double?} n]
          (loop [acc (Long. 1) nn n]
            (if (<= (double nn) 0) acc
                (recur (no/* x acc) (no/dec* nn)))))
   :cljs (defn pow- [x n] (TODO)))

(defn pow'
  "Strict |pow|"
  [x y]
  (TODO))

(defnt pow "x ^ y"
  #?(:clj  (^double ^:intrinsic [^double   x ^double y] (Math/pow x y))
     :cljs ([^number? x #_number? y] (js/Math.pow x y)))
  #?(:clj (^double             [#{byte short int long float} x y]
                                 (pow (core/double x) (core/double y)))))

#?(:clj (defalias expt pow))

#?(:clj
(defnt' pow*
  "x ^ y"
  {:performance ["2.7 times faster than java.lang.Math"
                 "Worst case 1E-11 difference"]}
  (^double [^double x ^double y] (FastMath/pow x y))))

#?(:clj (defalias expt* pow*))

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
   :cljs (defnt sqrt
           ([^number? x] (js/Math.sqrt  x))))

#?(:clj (defalias √2 sqrt))

#?(:clj  (defnt' cbrt "cube root"
           (^double [^double x] (Math/cbrt x)))
   :cljs (defnt cbrt "cube root"
           ([^number? x] (js/Math.cbrt x))))

#?(:clj (defalias √3 cbrt))

#?(:clj
(defnt cbrt*
  "cube root"
  {:performance ["6.2 times faster than java.lang.Math"
                 "Worst case 2E-14 difference"]}
  (^double [^double x] (FastMath/cbrt x))))

#?(:clj (defalias √3* cbrt*))

; ===== CONVERSE EXPONENTS (LOGARITHMS) ===== ;

#?(:clj  (defnt' e-exp "Euler's number (e) raised to the power of @x"
           (^double ^:intrinsic [^double x] (Math/exp x)))
   :cljs (defnt e-exp
           "Euler's number (e) raised to the power of @x"
           ([^number? x] (js/Math.exp x))))

#?(:clj
(defnt' e-exp*
  "Euler's number (e) raised to the power of @x"
  {:performance ["4.6 times faster than java.lang.Math"
                 "Worst case 4E-14 difference"]}
  (^double [^double x] (FastMath/exp x))))

#?(:clj  (defnt' log-e "Natural logarithm"
           (^double ^:intrinsic [^double x] (Math/log x)))
   :cljs (defnt log-e "Natural logarithm"
           [^number? x] (js/Math.log x)))

(defalias ln log-e)

#?(:clj (def ^:const ^double ln-2 (ln 2)))

#?(:clj  (defnt log-2 [^double x] (/ (ln x) ln-2))
   :cljs (defnt log-2 [^number? x] (js/Math.log2 x)))

#?(:clj
(defnt' log-e*
  "Natural logarithm"
  {:performance ["1.9 times faster than java.lang.Math"
                 "Worst case 3E-14 difference"]}
  (^double [^double x] (FastMath/log x))))

#?(:clj  (defnt' log-10 "Logarithm, base 10"
           (^double ^:intrinsic [^double x] (Math/log10 x)))
   :cljs (defnt log-10 "Logarithm, base 10"
           [^number? x] (js/Math.log10 x)))

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

(#?(:clj defnt' :cljs defnt) log-
  {:todo ["Need to intelligently determine, at compile time if possible, whether
           @x is e, 2, or 10 and choose the appropriate fn."]}
  ([#?(:clj #{double}) x #?(:clj #{double}) base] ; arbitrary to choose ln vs. log-10
    (no/div* (ln x) (ln base))))

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


(defnt ^long integer-log
  {:adapted-from 'apache.lucene.util.MathUtil}
  [^long x ^long base]
  (assert (> base 1))
  (let [ret 0]
    (loop [x x ret ret]
      (if (>= x base)
          (recur (#?(:clj unchecked-divide-int :cljs cljs.core//) x base) ; TODO need to handle `long` truncation via Numeric/divideTruncate ; truncation is acceptable
                 (inc ret))
          ret))))
