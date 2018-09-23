(ns quantum.core.numeric.predicates
        (:refer-clojure :exclude
          [neg? pos? pos-int? zero?])
        (:require
 #?(:cljs [com.gfredericks.goog.math.Integer :as int])
          [quantum.core.compare.core         :as comp]
          [quantum.core.data.numeric         :as dnum
            :refer [big-integer? bigdec? bigint? clj-bigint? numeric-primitive?]]
          [quantum.core.data.primitive       :as p]
          [quantum.core.logic                :as l]
          [quantum.core.type                 :as t]
          ;; TODO TYPED excise reference
          [quantum.core.untyped.error
            :refer [TODO]])
#?(:clj (:import
          [quantum.core Numeric])))

       ;; TODO TYPED add CLJS ratio impl
       (t/defn ^:inline neg? > p/boolean?
         ([x numeric-primitive?] #?(:clj (Numeric/isNeg x) :cljs (comp/< x 0)))
#?(:clj  ([x (t/or big-integer? bigdec?)] (-> x .signum neg?)))
#?(:clj  ([x clj-bigint?] (if (-> x .bipart         p/nil?)
                              (-> x .lpart          neg?)
                              (-> x .bipart .signum neg?))))
#?(:cljs ([x bigint?] (.isNegative x)))
#?(:clj  ([x dnum/ratio?] (-> x .numerator .signum neg?))))

       ;; TODO TYPED add CLJS ratio impl
       (t/defn ^:inline pos? > p/boolean?
         ([x numeric-primitive?] #?(:clj (Numeric/isPos x) :cljs (comp/> x 0)))
#?(:clj  ([x (t/or big-integer? bigdec?)] (-> x .signum pos?)))
#?(:clj  ([x clj-bigint?] (if (-> x .bipart         p/nil?)
                              (-> x .lpart          pos?)
                              (-> x .bipart .signum pos?))))
#?(:cljs ([x bigint?] (l/not (.isNegative x))))
#?(:clj  ([x dnum/ratio?] (-> x .numerator .signum pos?))))

       ;; TODO TYPED add CLJS ratio impl
       (t/defn ^:inline zero? > p/boolean?
         ([x numeric-primitive?] #?(:clj (Numeric/isZero x) :cljs (comp/== x 0)))
#?(:clj  ([x (t/or big-integer? bigdec?)] (-> x .signum zero?)))
#?(:clj  ([x clj-bigint?] (if (-> x .bipart         p/nil?)
                              (-> x .lpart          zero?)
                              (-> x .bipart .signum zero?))))
#?(:cljs ([x bigint?] (.isZero x)))
#?(:clj  ([x dnum/ratio?] (-> x .numerator .signum zero?))))

       (t/defnt ^:inline nan? > p/boolean?
#?(:clj  ([x p/float?]  (Float/isNaN x)))
         ([x p/double?] (#?(:clj Double/isNaN :cljs js/Number.isNaN) x))
         ([x t/any?]    false))

(def npos?     (l/fn-not pos?))
(def nneg?     (l/fn-not neg?))
(def pos-int?  (l/fn-and dnum/integer? pos?))
(def neg-int?  (l/fn-and dnum/integer? neg?))
(def npos-int? (l/fn-and dnum/integer? npos?))
(def nneg-int? (l/fn-and dnum/integer? nneg?))

(t/defn exact? > p/boolean? [x p/numeric?] (TODO))
