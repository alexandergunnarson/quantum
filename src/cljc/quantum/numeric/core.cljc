(ns ^{:doc "Higher-order numeric operations such as sigma, sum, etc."}
  quantum.numeric.core
  (:refer-clojure :exclude
    [reduce mod count *' +'])
  (:require
    [quantum.core.numeric      :as num
      :refer [*+* *-* *** *div* mod
              sqrt pow *' +' exactly]
      #?@(:cljs [:refer-macros [*']])]
    [quantum.core.data.binary  :as bin
      :refer [>>]]
    [quantum.core.error        :as err
      :refer [->ex TODO]]
    [quantum.core.fn
      :refer [fn-> <- fn& fn&2]]
    [quantum.core.log          :as log]
    [quantum.core.collections  :as coll
      :refer [map+ range+ filter+ mapcat+
              reduce join count kmap]]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.numeric.misc :as misc]))

(log/this-ns)

; TO EXPLORE
; - GNU Multiple Precision Arithmetic Library
;   - GMP aims to be faster than any other bignum library for all operand sizes.
; - Mathematica
;   - Number theory function library
;   - Elementary and Special mathematical function libraries
;   - Support for complex numbers, arbitrary precision, interval arithmetic
;   - Solvers for systems of equations, diophantine equations, ODEs, PDEs, DAEs, DDEs, SDEs and recurrence relations
;   - Finite element analysis including 2D and 3D adaptive mesh generation
;   - Numeric and symbolic tools for discrete and continuous calculus including continuous and discrete integral transforms
;   - Computational geometry in 2D, 3D and higher dimensions
;   - Libraries for signal processing including wavelet analysis on sounds, images and data
;   - Linear and non-linear Control systems libraries
;   - Tools for 2D and 3D image processing[11] and morphological image processing including image recognition
;   - Group theory and symbolic tensor functions
;   - Import and export filters for data, images, video, sound, CAD, GIS,[12] document and biomedical formats
;   - Database collection for mathematical, scientific, and socio-economic information and access to WolframAlpha data and computations
;   - Technical word processing including formula editing and automated report generating
; ================================

#_(defalias $ exp)

#_(defn quartic-root [a b c d]
  (let [A (+ (* 2  ($ b 3))
             (* -9 a b c)
             (* 27 ($ c 2))
             (* 27 ($ a 2) d)
             (* -72 b d))]
    (exp (/ (+ A
               (sqrt
                 (+ (* -4 ($ (+ ($ b 2)
                                (* -3 a c)
                                (* 12 d))
                             3))
                    ($ A 2))))
            54)
         (/ 1 3))))

; slash, ratios
(def scales
  {:minor-second   (/ 16 15)
   :major-second   (/ 9 8)
   :minor-third    (/ 6 5)
   :major-third    (/ 5 4)
   :perfect-fourth (/ 4 3)
   :aug-fourth     (/ 1.411 1) ; TODO more exact
   :perfect-fifth  (/ 3 2)
   :minor-sixth    (/ 8 5)
   :golden         (/ 1.61803 1) ; TODO more exact
   :major-sixth    (/ 5 3)
   :minor-seventh  (/ 16 9)
   :major-seventh  (/ 15 8)
   :octave         (/ 2 1)
   :major-tenth    (/ 5 2)
   :major-eleventh (/ 8 3)
   :major-twelfth  (/ 3 1)
   :double-octave  (/ 4 1)})

(defn sum+count     [xs] ; TODO is this necessary?
  (reduce (fn [[sum ct] x] [(*+* sum x) (inc ct)]) [0 0] xs))

(defn product+count [xs] ; TODO is this necessary?
  (reduce (fn [[sum ct] x] [(*** sum x) (inc ct)]) [1 0] xs))

(def sum     #(reduce *+* %)) ; TODO use +* and +', differentiating sum* and sum'
(def product #(reduce *** %)) ; TODO use ** and *', differentiating product* and product'

(defn sigma [set- step-fn]
  (->> set- (map+ #(step-fn %)) sum))

#?(:clj (defalias ∑ sigma))

(defn pi* [set- step-fn]
  (->> set- (map+ #(step-fn %)) product))

#?(:clj (defalias ∏ pi*))

(defn factors
  "All factors of @n."
  [n]
  (->> (range+ 1 (inc (sqrt n)))
       (filter+ #(zero? (rem n %)))
       (mapcat+ (fn [x] [x (num/div* n x)])) ; TODO have a choice of using unsafe div
       (join #{})))

(defn lfactors
  "All factors of @n, lazily computed."
  [n] (TODO))

; TODO MERGE
;#?(:cljs
;(defn gcd [x y]
;  (if (.isZero y)
;      x
;      (recur y (.modulo x y)))))

(defn call-max [f a b]
  (if (> a b)
      (f a b)
      (f b a)))

(defalias gcd misc/gcd)
(defalias gcf misc/gcf)

(defn extended-euclid*
  {:adapted-from "http://anh.cs.luc.edu/331/notes/xgcd.pdf"}
  [a b]
  (loop [prev-x 1 x 0
         prev-y 0 y 1
         a'     a
         b'     b]
    (if (zero? b')
        [a' prev-x prev-y]
        (let [q (long (num/floor (/ a' b')))]
          (recur x
                 (- prev-x (* q x))
                 y
                 (- prev-y (* q y))
                 b'
                 (mod a' b'))))))

(defn gcd-via-extended-euclid [a b]
  (first (extended-euclid* a b)))

(defn modular-multiplicative-inverse
  {:adapted-from "http://www.geeksforgeeks.org/multiplicative-inverse-under-modulo-m/"
   :tests `{(modular-multiplicative-inverse 60 13)
            5
            (modular-multiplicative-inverse 60 12)
            nil}}
  [a b]
  (let [[g x y] (extended-euclid* a b)]
    (when (= g 1)
      (-> x (mod b) (+ b) (mod b)))))

(defn sq [x] (*** x x))

(defn cube [x] (*** x x x))

(defn mod-pow
  "Computes the modular power: (a^b) mod n"
  {:adapted-from "Applied Cryptography by Bruce Schneier, via Wikipedia"
   :time-complexity '(log n)
   :tests `{(mod-pow 105 235 391)
            41}}
  [a b n]
  (loop [r 1 a' a b' b]
    (let [r'  (if (= 1 (mod b' 2))
                  (mod (* r a') n)
                  r)
          b'' (>> b' 1)]
      (if (zero? b'')
          r'
          (recur r' (mod (* a' a') n) b'')))))

; ===== PRIMES ===== ;

(defn prime?*
  "Checks whether @x is a (provable) prime or not.
   Uses the Miller-Rabin probabilistic test in such
   a way that a result is guaranteed: it uses the
   firsts prime numbers as successive base (see
   Handbook of applied cryptography by Menezes, table 4.1)."
  {:implemented-by '#{org.apache.commons.math3.primes.Primes}}
  [x] (TODO))

(defn prime?
  "Checks whether @x is a prime or not. Less efficient
   than `prime?*` but exhaustive."
  [x] (TODO))

(defn next-prime
  "Evaluates to the smallest prime p >= x."
  {:implemented-by '#{org.apache.commons.math3.primes.Primes}}
  [x] (TODO))

(defn prime-factors
  "Prime factors decomposition"
  {:implemented-by '#{org.apache.commons.math3.primes.Primes}}
  [x] (TODO))

(defn !
  "Computes the factorial of `n`."
  {:implemented-by '#{org.apache.commons.math3.util.CombinatoricsUtils}
   :todo ["Optionally use memoization to make this more efficient"]}
  [n] (reduce (fn&2 *') (range 1 (inc n))))

(defn e
  "Computes the constant `e` to the `k`-th series term."
  {:author 'alexandergunnarson
   :todo ["Optionally use memoization to make this more efficient"]}
  [k] (sigma (range 0 k) #(/ 1 (! %))))

(defn pi
  "Computes the constant `π` to the `k`-th series term
   using the Newton / Euler Convergence Transformation."
  {:author 'alexandergunnarson
   :example `(with-precision 10000 (bigdec (pi 20)))
   :todo ["Optionally use memoization to make this more efficient"]}
  [k] (*' 2 (sigma (range 0 k)
                   #(/ (*' (exactly (pow 2     %)) ; TODO use pow'
                           (exactly (pow (! %) 2)))
                       (! (+' 1 (*' 2 %)))))))
