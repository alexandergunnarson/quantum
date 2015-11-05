(ns quantum.measure.core
  (:require-quantum [ns fn logic num set err macros pr log str])
  (:require
    [quantum.measure.reg]
    [quantum.core.graph :as g
      :refer [graph digraph weighted-digraph]]
    [loom.alg         :as g.alg]
    [loom.graph       :as g.graph]
    [loom.alg-generic :as g.gen-alg]))

(defn ->str
  ([k joiner]
    (->> [(namespace k) (name k)]
         (core/remove empty?)
         (str/join joiner))))

; TODO Include frinj stuff
; https://github.com/martintrojer/frinj/blob/master/src/frinj/feeds.clj
; {:metric-cables #{:length} ...}
(defn defunits-of* [unit-type std-unit unit-pairs emit-type]
  (throw-unless (contains? #{:map :code} emit-type) (Err. nil "Emit type not recognized" emit-type))
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
           nodes (into #{} (g/nodes units-graph))
           conversion-map-sym 'conversion-map
           conversion-map
             (when (= emit-type :map)
               (reduce (fn [m [[from to] multiplier]]
                         (let [multiplier-f (whenc (/ 1 multiplier) (eq? 1) 1)]
                           (assoc-in m [from to] multiplier-f)))
                 {}
                 fn-chart))]
       (concat 
         ; Incorporate the new units in
         (list 'do)
         `[(swap! quantum.measure.reg/reg-units
             (fn [u#]
               (core/reduce
                 (fn [ret# [k# f#]] (update ret# k# f#))
                 u#
                 (zipmap ~nodes
                         (repeat (fn [node#] (ifn (core/get quantum.measure.reg/reg-units node#) identity
                                                (f*n core/conj ~unit-type-k)
                                                (constantly #{~unit-type-k}))))))))]
         (condp = emit-type
           :map
             (quote+ ((def ~conversion-map-sym ~conversion-map)))
           :code
             (core/for [[[from to] multiplier] fn-chart]
               (let [multiplier (/ 1 multiplier)
                     fn-name (symbol (str (->str from "-") "->" (->str to "-")))]
                 (if (= multiplier 1) ; Identical
                     (quote+ (defn ~fn-name [n] n))
                     (quote+ (defn ~fn-name [n] (* n ~multiplier))))))))))

(defmacro defunits-of [unit-type std-unit & {:as unit-pairs}]
  (defunits-of* unit-type std-unit unit-pairs :map))
