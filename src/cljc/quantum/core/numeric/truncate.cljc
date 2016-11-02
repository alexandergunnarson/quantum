(ns quantum.core.numeric.truncate
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   ) :as core]
    [quantum.core.error     :as err
      :refer        [TODO]          ]
    [quantum.core.macros
      :refer        [#?@(:clj [defnt defnt'])]
      :refer-macros [defnt]]
    [quantum.core.vars      :as var
      :refer        [#?@(:clj [defalias defaliases])]
      :refer-macros [defalias defaliases]          ]
    [quantum.core.convert.primitive
      :refer [#?@(:clj [->double])]]
    [quantum.core.numeric.convert
      :refer [#?@(:clj [->bigdec])]]))

#?(:clj
(defnt' rint "The double value that is closest in value to @x and is equal to a mathematical integer."
  (^double [^double x] (Math/rint x))))

#?(:clj  (defnt round' "Rounds up in cases of ambiguity."
           (^long                 [^double               x] (Math/round x))
           (^long                 [^float                x] (Math/round x))
           (^java.math.BigDecimal [^java.math.BigDecimal x math-context]
             (.round x math-context))
           (^long                 [#{long clojure.lang.Ratio} x]
             (round' (->double x))) ; TODO use ->double
           (^java.math.BigDecimal [#{long clojure.lang.Ratio} x math-context]
             (round' (->bigdec x) math-context)))
   :cljs (defn round' [x] (js/Math.round x)))

#?(:clj (defn round
          "Probably deprecated; use:
           |(with-precision <decimal-places> (bigdec <number>))|"
          {:todo ["Port to cljs"]}
          [num-0 & {:keys [type to] :or {to 0}}]
          (let [round-type
                  (if (nil? type)
                      (. BigDecimal ROUND_HALF_UP)
                      (case type
                        :unnecessary BigDecimal/ROUND_UNNECESSARY
                        :ceiling     BigDecimal/ROUND_CEILING
                        :up          BigDecimal/ROUND_UP
                        :half-up     BigDecimal/ROUND_HALF_UP
                        :half-even   BigDecimal/ROUND_HALF_DOWN
                        :half-down   BigDecimal/ROUND_HALF_DOWN
                        :down        BigDecimal/ROUND_DOWN
                        :floor       BigDecimal/ROUND_FLOOR))]
            (.setScale ^BigDecimal (bigdec num-0) ^Integer to round-type)))
   :cljs (defalias round round')) ; TODO fix

; http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
#_(:clj
(defn ^double round [^double value ^long places]
  (when (neg? places) (throw (->ex nil "|places| must be positive" places)))

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
  (-> x (/ n) round' (* n)))

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

(defn clamp
  "Clamp v between [a, b]."
  [a b v]
  (cond
    (<= v a) a
    (>= v b) b
    :else v))
