(ns quantum.core.numeric.truncate
  (:refer-clojure :exclude [<= >= pos?])
  (:require
    [clojure.core           :as core]
    [quantum.core.error     :as err
      :refer [TODO ->ex]]
    [quantum.core.macros
      :refer [defnt #?@(:clj [defnt'])]]
    [quantum.core.vars      :as var
      :refer [defalias defaliases]]
    [quantum.core.convert.primitive
      :refer [#?@(:clj [->int ->double])]]
    [quantum.core.numeric.convert
      :refer [->bigdec]]
    [quantum.core.compare   :as comp
      :refer [<= >=]]
    [quantum.core.numeric.predicates
      :refer [pos?]])
  #?(:clj (:import java.math.BigDecimal clojure.lang.Ratio)))

#?(:clj
(defnt' rint "The double value that is closest in value to @x and is equal to a mathematical integer."
  (^double [^double x] (Math/rint x))))

#?(:clj (defalias round-double rint))

#?(:clj  (defnt round-int "Rounds up in cases of ambiguity."
           {:todo {0 "longs and ratios should round conditionally, taking care of overflow"}}
           (^long       [^double               x] (Math/round x))
           (^long       [^float                x] (Math/round x))
           (^BigDecimal [^BigDecimal x math-context]
             (.round x math-context))
           (^long       [#{long ratio?} x]
             (round-int (->double x))) ; TODO 0
           (^BigDecimal [#{long ratio?} x math-context]
             (round-int (->bigdec x) math-context)))
   :cljs (defn round-int [x] (js/Math.round x)))

#?(:clj
(defn ^Long/TYPE round-type->bigdec-round-type [round-type]
  (case round-type
    nil          BigDecimal/ROUND_HALF_UP
    :unnecessary BigDecimal/ROUND_UNNECESSARY
    :ceiling     BigDecimal/ROUND_CEILING
    :up          BigDecimal/ROUND_UP
    :half-up     BigDecimal/ROUND_HALF_UP
    :half-even   BigDecimal/ROUND_HALF_DOWN
    :half-down   BigDecimal/ROUND_HALF_DOWN
    :down        BigDecimal/ROUND_DOWN
    :floor       BigDecimal/ROUND_FLOOR
    (throw (->ex "Invalid round type" {:round-type round-type})))))

#?(:clj  (defnt' round
           "Rounds `n` to `places`, the specified number of decimal places,
            according to `round-type`."
           {:todo #{"Port to CLJS"}}
           ([^integer? n ^int places] n)
           ([^integer? n ^int places round-type] n)
           ([#{double ratio? bigdec?} n ^int places] (round n places nil))
           ([#{double ratio? bigdec?} n ^int places round-type]
             (.setScale (->bigdec n) places (round-type->bigdec-round-type round-type))))
   :cljs (defalias round round-int)) ; TODO fix

; http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
#_(:clj
(defn ^double round [^double value ^long places]
  (when (neg? places) (throw (->ex "|places| must be positive" places)))

  (-> value
      (java.math.BigDecimal.)
      (.setScale places java.math.RoundingMode/HALF_UP)
      (.doubleValue))))

#_(:cljs (defn round [value places] value))

(defn nearest
  "Round x to the nearest n."
  {:tests `{(nearest 23 15)
            30
            (nearest 22 15)
            15}}
  [x n]
  (-> x (/ n) round-int (* n)))

#?(:clj  (defnt ceil
           (^double [^double x] (Math/ceil x))
           (^double [        x] (TODO "fix") (ceil (core/double x))))
   :cljs (defnt ceil [^number? x] (js/Math.ceil x)))

#?(:clj  (defnt floor
           (^double [^double x] (Math/floor x))
           (^double [        x] (TODO "fix") (floor (core/double x))))
   :cljs (defnt floor [^number? x] (js/Math.floor x)))

#?(:clj
(defnt' floor-div
  (^int  [^int  x ^int  y] (Math/floorDiv x y))
  (^long [^long x ^long y] (Math/floorDiv x y))))

#?(:clj
(defnt' floor-mod
  (^int  [^int  x ^int  y] (Math/floorMod x y))
  (^long [^long x ^long y] (Math/floorMod x y))))


#?(:clj
(defnt' next-after
  (^double [^double start ^double direction] (Math/nextAfter start direction))
  (^float  [^float  start ^double direction] (Math/nextAfter start direction))))

#?(:clj
(defnt' next-down
  (^double [^double x] (Math/nextDown x))
  (^float  [^float  x] (Math/nextDown x))))

#?(:clj
(defnt' next-up
  (^double [^double x] (Math/nextUp x))
  (^float  [^float  x] (Math/nextUp x))))

(defn trunc
  "Round towards zero to an integral value."
  [x] (if (pos? x) (floor x) (ceil x)))

#?(:clj  (defnt' clamp
           "Clamp v between [a, b]."
           {:todo #{"`defnt'` -> `defnt`"}}
           [^number? a ^number? b ^number? v]
           (cond
             (<= v a) a
             (>= v b) b
             :else v))
   :cljs (defn clamp
           "Clamp v between [a, b]."
           [a b v]
           (cond
             (<= v a) a
             (>= v b) b
             :else v)))
