(ns quantum.core.numeric.matrix
           (:require [#?(:clj  clojure.pprint
                         :cljs cljs.pprint) :as pprint]
                     [quantum.core.error    :as err
                       :refer [->ex]                  ]
                     [quantum.core.loops    :as loops
                       :refer [#?(:clj reducei)]      ])
  #?(:cljs (:require-macros
                     [quantum.core.loops    :as loops
                       :refer [reducei]               ])))

; TODO clean this namespace up

(def ->vec vec)

(defn prm [m]
  (pprint/print-table
    (range (-> m first count))
    m))

(def ^:dynamic *round* false)
(def ^:dynamic *print* false)

; http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
#?(:clj 
(defn ^double round [^double value ^long places]
  (when (neg? places) (throw (->ex nil "|places| must be positive" places)))

  (-> value
      (java.math.BigDecimal.)
      (.setScale places java.math.RoundingMode/HALF_UP)
      (.doubleValue))))

#?(:cljs (defn round [value places] value))

; For matrices
; (defn row-op [op r1 r2]
;   (let [i (volatile! -1)]
;     (reduce
;       (fn [ret x] (vswap! i inc)
;         (update ret @i op (get r2 @i)))
;       r1
;       r1)))

(defn row-op [r1 op r2 & [round-to]]
  (reducei
    (fn [ret x i]
      (update ret i
        (fn [n]
          (let [ret (-> n (op (get r2 i)))]
            (if *round*
                (round ret *round*)
                ret)))))
    r1
    r1))

(defn rows-op
  {:usage
    '(binding [*round* 3
               *print* true]
       (-> [[51  70 260 400]
            [5.4 15 9   30 ]
            [5.2 0  5   10 ]]
           (rows-op 2 * (/ 5.4 5.2))
           (rows-op 2 - [1])))}
  [m i op arg]
  (let [res (update m i
      (fn [r]
        (cond
          (vector? arg)
          (reduce
            (fn [ret r-i]
              (row-op ret op (get m r-i)))
            r
            arg)
    
          (number? arg)
          (row-op r op
            (->vec (repeat (count r) arg))))))]
    (when *print* (prm res))
    res))