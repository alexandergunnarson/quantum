(ns quantum.core.compare
  (:refer-clojure :exclude
    [= not= < > <= >= max min max-key min-key neg? pos? zero? - -' + inc compare reduce])
  (:require
    [clojure.core       :as core]
    [quantum.core.compare.core :as ccomp]
    [quantum.core.convert.primitive  :as pconv
      :refer [->boxed ->boolean ->long]]
    [quantum.core.error :as err
      :refer [TODO]]
    [quantum.core.fn
      :refer [fn&2]]
    [quantum.core.macros
      :refer [defnt #?@(:clj [defnt' variadic-proxy variadic-predicate-proxy])]]
    [quantum.core.numeric.convert
      :refer [->num ->num&]]
    [quantum.core.numeric.operators  :as op
      :refer [- -' + abs inc div:natural]]
    [quantum.core.numeric.predicates :as pred
      :refer [neg? pos? zero?]]
    [quantum.core.numeric.types      :as ntypes]
    [quantum.core.reducers           :as red
      :refer [reduce]]
    [quantum.core.vars
      :refer [defalias defaliases]])
  (:require-macros
    [quantum.core.compare
      :refer [< > <= >=]])
  #?(:clj (:import clojure.lang.BigInt quantum.core.Numeric)))

(defaliases ccomp
  compare
  min-key first-min-key second-min-key
  max-key first-max-key second-max-key
  #?@(:clj [compare-1d-arrays-lexicographically
            =   =&   not=     not=&
            <   <&   comp<    comp<&
            <=  <=&  comp<=   comp<=&
            >   >&   comp>    comp>&
            >=  >=&  comp>=   comp>=&
            min min& comp-min comp-min&
            max max& comp-max comp-max&
            min-byte   max-byte
            min-char   max-char
            min-short  max-short
            min-int    max-int
            min-long   max-long
            min-float  max-float
            min-double max-double
            #_first-min #_second-min
            #_first-max #_second-max]))

(defn reduce-first-max-key  [kf xs] (red/reduce-sentinel (fn&2 first-max-key  kf) xs))
(defn reduce-second-max-key [kf xs] (red/reduce-sentinel (fn&2 second-max-key kf) xs))
(defn reduce-max-key        [kf xs] (red/reduce-sentinel (fn&2 max-key        kf) xs))

(defn reduce-first-min-key  [kf xs] (red/reduce-sentinel (fn&2 first-min-key  kf) xs))
(defn reduce-second-min-key [kf xs] (red/reduce-sentinel (fn&2 second-min-key kf) xs))
(defn reduce-min-key        [kf xs] (red/reduce-sentinel (fn&2 min-key        kf) xs))

(defn reduce-first-min      [   xs] (red/reduce-sentinel ccomp/first-min-temp           xs))
(defn reduce-second-min     [   xs] (red/reduce-sentinel ccomp/second-min-temp          xs))
(defn reduce-min            [   xs] (red/reduce-sentinel ccomp/min-temp                 xs))

(defn reduce-first-max      [   xs] (red/reduce-sentinel ccomp/first-max-temp           xs))
(defn reduce-second-max     [   xs] (red/reduce-sentinel ccomp/second-max-temp          xs))
(defn reduce-max            [   xs] (red/reduce-sentinel ccomp/max-temp                 xs))


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
      coll)))

(defn least
  "Returns the 'least' element in coll in O(n) time."
  {:attribution "taoensso.encore, possibly via weavejester.medley"}
  [coll & [?comparator]]
  (let [comparator (or ?comparator rcompare)]
    (reduce
      (fn ([] nil) ([a b] (if (neg? (comparator a b)) b a)))
      coll)))

#?(:clj
(defmacro min-or [a b else]
 `(let [a# ~a b# ~b]
    (cond (< a# b#) a#
          (< b# a#) b#
          :else ~else))))

#?(:clj
(defmacro max-or [a b else]
 `(let [a# ~a b# ~b]
    (cond (> a# b#) a#
          (> b# a#) b#
          :else ~else))))

#_(defn extreme-comparator [comparator-n]
  (get {> num/greatest
        < num/least   }
    comparator-n))

; ===== APPROXIMATION ===== ;

(defn approx=
  "Return true if the absolute value of the difference between x and y
   is less than eps."
  {:todo #{"`core/<` -> `<` "}}
  [x y eps]
  (core/< (abs (- x y)) eps))

(defn within-tolerance?
  {:todo #{"`core/<` -> `<` "}}
  [n total tolerance]
  (and (core/>= n (- total tolerance))
       (core/<= n (+ total tolerance))))

(defnt' within-percent-tolerance? [^number? actual ^number? expected percent-tolerance]
  (core/< (div:natural (double (abs (core/- expected actual))) expected) ; TODO remove `double` cast!!!
          percent-tolerance))
