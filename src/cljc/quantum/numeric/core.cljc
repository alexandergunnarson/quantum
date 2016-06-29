(ns ^{:doc "Higher-order numeric operations such as sigma, sum, etc."}
  quantum.numeric.core
           (:refer-clojure :exclude [reduce])
           (:require
             [quantum.core.numeric     :as num
               :refer [#?@(:clj [sqrt])]
               #?@(:cljs [:refer-macros [sqrt]])]
             [quantum.core.error       :as err
               :refer [->ex]]
             [quantum.core.collections :as coll
               :refer [map+ range+ filter+ mapcat+ #?@(:clj [reduce join])]]
             [quantum.core.vars
               :refer [#?(:clj defalias)]])
  #?(:cljs (:require-macros
             [quantum.core.collections
               :refer [reduce join]]
             [quantum.core.vars
               :refer [defalias]])))

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

(defalias ∑ sigma)

(defn pi* [set- step-fn]
  (->> set- (map+ #(step-fn %)) product))

(defalias ∏ pi*)

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
  [n] (err/todo))

; TODO MERGE
;#?(:cljs
;(defn gcd [x y]
;  (if (.isZero y)
;      x
;      (recur y (.modulo x y)))))

(defn gcd
  "(gcd a b) computes the greatest common divisor of a and b."
  ([a b]
  (if (zero? b)
      a
      (recur b (num/mod a b))))
  ([a b & args]
    (reduce gcd (gcd a b) args)))

(defn sq [x] (* x x)) ; TODO cube