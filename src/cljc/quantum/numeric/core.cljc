(ns ^{:doc "Higher-order numeric operations such as sigma, sum, etc."}
  quantum.numeric.core
  (:refer-clojure :exclude
    [reduce mod first, count, *' +', map, dotimes])
  (:require
    [quantum.core.numeric      :as num
      :refer [abs mod sqrt pow #?(:clj *') +' exactly]
      #?@(:cljs [:refer-macros [*']])]
    [quantum.core.data.binary  :as bin
      :refer [>>]]
    [quantum.core.collections  :as coll
      :refer [map map+, range+, filter+, mapcat+
              reduce reduce-multi, join join'
              for', dotimes, first, get-in* assoc-in!*
              count kw-map, blank]]
    [quantum.core.error        :as err
      :refer [->ex TODO]]
    [quantum.core.fn
      :refer [fn-> <- fn1 fnl fn& fn&2]]
    [quantum.core.log          :as log]
    [quantum.core.type         :as t]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.macros
      :refer [defnt #?(:clj defnt')]]
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

(defn sum+count     [xs]
  (reduce-multi [+ coll/count:rfn] xs))

(defn product+count [xs]
  (reduce-multi [* coll/count:rfn] xs))

(def sum     (fnl reduce +))
(def product (fnl reduce *))

(defn sigma [xs step-fn] (->> xs (map+ step-fn) sum))

#?(:clj (defalias ∑ sigma))

(defn pi* [xs step-fn] (->> xs (map+ step-fn) product))

#?(:clj (defalias ∏ pi*))

(defn normalize-sum-to
  "Ensures that the sum of `xs` sums to `target-sum`, by
   normalizing the values of `xs`."
  [xs target-sum]
  (let [[sum ct] (sum+count xs)
        xs'      (->> xs (map #(- (* % (/ sum)) (/ (- 1 target-sum) ct))))
        sum'     (reduce + xs')
        xs''     (update xs' 0 #(+ % (- target-sum sum')))]
    xs''))

(defn factors
  "All factors of @n."
  [n]
  (->> (range+ 1 (inc (sqrt n)))
       (filter+ #(zero? (rem n %)))
       (mapcat+ (fn [x] [x (num/div* n x)])) ; TODO have a choice of using unsafe div
       (join    #{})))

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

(defnt sq [^number? x] (* x x))

(defnt cube [^number? x] (* x x x))

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

(defn normalize
  "Given `x•`, a 1D tensor of real values, computes a version of `x•
   whose values are normalized between `a` and `b`.
   `a` defaults to 0 and `b` defaults to 1."
  ([x•] (normalize x• 0 1))
  ([x• a b]
    (let [min- (coll/reduce-min x•)
          max- (coll/reduce-max x•)
          rng  (abs (- min- max-))
          rng' (abs (- a b))
          min' (min a b)]
      (->> x• (map+ (fn-> (- min-) (/ rng) (* rng') (+ min'))) join))))

(defnt normalize-2d:column
  "Given `x••`, a 2D tensor of real values, computes a version of it
   whose values are normalized by column between `a` and `b`.
   That is, the min and max are calculated not by row, but by column."
  {:todo       #{"`skip-cols` must be something for which `get` can return truthy or falsey"}
   :params-doc '{skip-cols "Indices of columns for which to skip normalization"}}
  ([#{numeric-2d? objects-2d?} x••] (normalize-2d:column x•• nil 0 1))
  ([#{numeric-2d? objects-2d?} x•• skip-cols] (normalize-2d:column x•• skip-cols 0 1))
  ([#{numeric-2d? objects-2d?} x•• skip-cols #_double a #_double b] ; TODO fix this because we're getting primitive type hint complaints!!
    (let [x••'    (blank x••)
          ct:rows (count x••')
          ct:cols (-> x••' first count)
          rng'    (abs (- a b))
          min'    (min a b)]
      (dotimes [i:col ct:cols]
        (if (get skip-cols i:col)
            (dotimes [i:row ct:rows]
              (coll/assoc-in!*& x••' (coll/get-in*& x•• i:row i:col) i:row i:col))
            (let [min:col (->> x•• (map+ (fn1 get i:col)) coll/reduce-min)
                  max:col (->> x•• (map+ (fn1 get i:col)) coll/reduce-max)
                  rng:col (abs (- min:col max:col))]
              (dotimes [i:row ct:rows]
                (coll/assoc-in!*& x••'
                  (t/static-cast-depth x•• 2
                    (-> (coll/get-in*& x•• i:row i:col)
                        (- min:col) (/ rng:col) (* rng') (+ min')))
                  i:row i:col)))))
      x••')))

; TODO:
#_"Dividing by the range allows
outliers (extreme values) to have a profound effect on the contribution of an attribute.
In order to avoid outliers (be robust in their presence), it is common to divide by the
standard deviation instead of range, or to “trim” the range by removing the highest and lowest
few percent (e.g., 5%) of the data from consideration in defining the range. It is also possible to
map any value outside this range to the minimum or maximum value to avoid normalized
values outside the range 0..1. Domain knowledge can often be used to decide which method is
most appropriate."
