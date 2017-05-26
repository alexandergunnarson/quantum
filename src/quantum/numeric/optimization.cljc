(ns quantum.numeric.optimization
  "Minimization and maximization"
  (:refer-clojure :exclude [for reduce repeat max-key])
  (:require
    [clojure.core :as core]
    [quantum.core.compare
      :refer [max-key]]
    [quantum.core.fn
      :refer [fn&]]
    [quantum.core.collections
      :refer [repeat take+ filter+ for+ red-apply
              for reduce red-for join]]
    [quantum.core.log :as log])
#?(:cljs
  (:require-macros
    [quantum.numeric.optimization :as self])))

; TO EXPLORE
; - Mathematica
;   - Constrained and unconstrained local and global optimization
; - <org.apache.commons.math3.optim.*>
; =============================

; ===== COMBINATORIAL ===== ;

(defn knapsack
  "A solution to the knapsack problem. Repetitions allowed.
   Given a set of items, each with a weight and a value,
   determine the number of each item to include in a collection
   so that the total weight is less than or equal to a given limit
   and the total value is as large as possible."
  {:time-complexity '(* (count table) W)}
  [W table]
  (red-for [w (range 1 (inc W))
            K [[[] 0]]]
    (conj K (->> (for+ [{ii :item wi :weight vi :value}
                         (filter+ #(<= (:weight %) w) table)]
                   (let [[items value] (get K (- w wi))]
                     [(conj items ii) (+ value vi)]))
                 (red-apply (fn& max-key second))))))

(defn knapsack-no-repeat
  "A solution to the knapsack problem where repetitions are not allowed."
  {:time-complexity '(* (count table) W)}
  [W table]
  (red-for [j (range 1 (inc (count table)))
            K (for [w (range 0 (inc W))]
                (for [j (range 0 (inc (count table)))] [0 []]))]
    (red-for [w (range 1 (inc W)) K K]
      (let [{ij :item wj :weight vj :value} (get table (dec j))]
        (assoc-in K [w j]
          (max-key first
            (get-in K [w (dec j)])
            (if (> wj w)
                [0 nil] ; won't be the max
                (-> (get-in K [(- w wj) (dec j)])
                    (update 0 +    vj)
                    (update 1 conj ij)))))))))
