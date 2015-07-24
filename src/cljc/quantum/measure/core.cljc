(ns quantum.measure.core
  (:require-quantum [:lib])
  (:require
    [quantum.measure.reg]
    [quantum.core.graph :as g
      :refer [graph digraph weighted-digraph]]
      [loom.alg         :as g.alg]
      [loom.graph       :as g.graph]
      [loom.alg-generic :as g.gen-alg]))

; TODO Include frinj stuff
; https://github.com/martintrojer/frinj/blob/master/src/frinj/feeds.clj

(defn defunits-of* [unit-type std-unit unit-pairs]
  (let [units-graph
             (->> (core/for [[unit [[rate conv-unit] & [aliases]]] unit-pairs]
                    (->> (core/for [alias aliases]
                           [[alias conv-unit (/ 1 (num/exactly rate))]
                            [conv-unit alias (num/exactly rate)]])
                         (apply concat)
                         (concat [[unit conv-unit (/ 1 (num/exactly rate))]
                                  [conv-unit unit (num/exactly rate)]])))
                  (apply concat)
                  (apply weighted-digraph))
           fn-chart
             (->> (g/all-pairs-shortest-paths units-graph *)
                  g/root-node-paths)
           unit-type-k (-> unit-type name keyword)
           nodes (into (sorted-set+) (g/nodes units-graph))]
       (concat 
         ; Incorporate the new units in
         (quote+
           [(swap! quantum.measure.reg/reg-units
              (fn [u]
                (reduce
          (fn [ret k f] (update ret k f))
          u
          (zipmap ~nodes
                    (repeat (fn [node] (map-entry node
                                       (if (get quantum.measure.reg/reg-units node)
                                           (f*n conj ~unit-type-k)
                                           (constantly #{~unit-type-k})))))))))])
         (core/for [[[from to] multiplier] fn-chart]
           (let [multiplier (/ 1 multiplier)
                 fn-name (symbol (str (str/str+ from "-") "->" (str/str+ to "-")))]
             (if (= multiplier 1) ; Identical
                 (quote+ (defn ~fn-name [n] n))
                 (quote+ (defn ~fn-name [n] (* n ~multiplier)))))))))

