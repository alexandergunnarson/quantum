(ns ^{:doc "Higher-order numeric operations such as sigma, sum, etc."}
  quantum.numeric.core
  (:refer-clojure :exclude [reduce mod])
  (:require
    [quantum.core.numeric     :as num
      :refer        [#?@(:clj [sqrt mod])]
      :refer-macros [sqrt mod]]
    [quantum.core.data.binary :as bin
      :refer        [>>]]
    [quantum.core.error       :as err
      :refer        [->ex TODO]]
    [quantum.core.fn
      :refer        [#?@(:clj [fn-> <-])]
      :refer-macros [fn-> <-]]
    [quantum.core.collections :as coll
      :refer        [map+ range+ filter+ mapcat+ #?@(:clj [reduce join])]
      :refer-macros [reduce join]]
    [quantum.core.vars
      :refer        [#?(:clj defalias)]
      :refer-macros [defalias]]
    [quantum.core.numeric.misc :as misc]))

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

(def sum     #(reduce + %)) ; TODO use +* and +', differentiating sum* and sum'
(def product #(reduce * %)) ; TODO use ** and *', differentiating product* and product'

(defn sigma [set- step-fn]
  (->> set- (map+ #(step-fn %)) sum))

#?(:clj (defalias ∑ sigma))

(defn pi* [set- step-fn]
  (->> set- (map+ #(step-fn %)) product))

#?(:clj (defalias ∏ pi*))

(defn find-max-by ; |max-by| would be the first of it
  ([pred x] x)
  ([pred a b] (if (> (pred a) (pred b))
                  a
                  b)))

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

(defn sq [x] (* x x))

(defn cube [x] (* x x x))

(defn mod-pow
  "Computes the modular power: (a^b) mod n"
  {:adapted-from "Applied Cryptography by Bruce Schneier, via Wikipedia"
   :O "O(log(@e))"
   :tests `{(mod-pow 105 235 391)
            41}}
  [a b n]
  (loop [r 1 a' a b' b]
    (let [r' (if (= 1 (mod b' 2))
                 (mod (* r a') n)
                 r)
          b'' (bit-shift-right b' 1)]
      (if (zero? b'')
          r'
          (recur r' (mod (* a' a') n) b'')))))
  #_(if (= m 1)
      0
      ; Elided assertion of pseudocode due to Clojure's numeric auto-promotion
      (loop [ret 1
             b'  (mod b m)
             e'  e]
        (if ())
        #_(if (> e' 0)
            (recur (if (= 1 (mod e' 2))
                       (mod (* ret b') m)
                       ret)
                   (>> e' 1)
                   (mod (sq b') m))
            ret)))

(defn prime?
  "Checks whether @x is prime."
  [x]
  (TODO))

; ===== STATISTICAL CALCULATIONS ===== ;

(defn mean [v]
  (/ (sum v) (count v)))

(defn std-dev [v]
  (let [mean' (mean v)]
    (->> v
         (map+ (fn-> (- mean') sq))
         sum
         (<- (/ (count v)))
         num/sqrt)))
