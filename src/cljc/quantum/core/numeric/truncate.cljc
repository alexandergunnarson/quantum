(ns quantum.core.numeric.truncate
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   ) :as core]
    [quantum.core.error     :as err
      :refer        [TODO]          ]
    [quantum.core.macros
      :refer        [#?@(:clj [defnt defnt'])]
      :refer-macros [defnt]]
    [quantum.core.numeric.convert
      :refer [#?@(:clj [->bigdec])]]))

#?(:clj
(defnt' rint "The double value that is closest in value to @x and is equal to a mathematical integer."
  (^double [^double x] (Math/rint x))))

#?(:clj  (defnt' round' "Rounds up in cases of ambiguity."
           (^long                 [^double               x] (Math/round x))
           (^long                 [^float                x] (Math/round x))
           (^java.math.BigDecimal [^java.math.BigDecimal x math-context]
             (.round x math-context))
           (^java.math.BigDecimal [^clojure.lang.Ratio x]
             (round' (core/double x)))
           (^java.math.BigDecimal [^clojure.lang.Ratio x math-context]
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
   :clj (defalias round round')) ; TODO fix

#?(:clj  (defnt ceil
           (^double [^double x] (Math/ceil x))
           (^double [        x] (TODO "fix") (ceil (core/double x))))  
   :cljs (defn ceil [x] (js/Math.ceil x)))

#?(:clj  (defnt floor
           (^double [^double x] (Math/floor x))
           (^double [        x] (TODO "fix") (floor (core/double x)))) 
   :cljs (defn floor [x] (js/Math.floor x)))

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
