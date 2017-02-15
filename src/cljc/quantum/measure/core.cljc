(ns quantum.measure.core
  (:require
    [clojure.core             :as core    ]
    [quantum.core.error       :as err
      :refer [->ex throw-unless]]
    [quantum.core.numeric     :as num     ]
    [quantum.core.string      :as str     ]
    [quantum.core.graph       :as g
      :refer [#?@(:clj [->graph ->digraph ->weighted-digraph])]]
    [quantum.core.fn          :as fn
      :refer [fn1]]
    [quantum.core.logic       :as logic
      :refer [fn= whenc ifn]]
    [quantum.measure.reg                 ]
    [quantum.core.macros.core :as cmacros
      :refer [case-env*]]))

(defn ->str
  {:todo ["MOVE TO CONVERT"]}
  ([k joiner]
    (->> [(namespace k) (name k)]
         (core/remove empty?)
         (str/join joiner))))

; TODO Include frinj stuff
; https://github.com/martintrojer/frinj/blob/master/src/frinj/feeds.clj
; {:metric-cables #{:length} ...}
#?(:clj
(defn defunits-of* [env unit-type std-unit unit-pairs emit-type]
  (case-env* env
    :cljs (println "/* CLJS quantum.measure.* graph algorithm is broken — causes infinite loop. */")
    (do (println "/* CLJ quantum.measure.* graph algorithm */")
        (throw-unless (contains? #{:map :code} emit-type) (->ex "Emit type not recognized" emit-type))
        (let [num-cast (case-env* env :clj identity :cljs double)
              units-graph
                (->> (core/for [[unit [[rate conv-unit] & [aliases]]] unit-pairs]
                       (->> (core/for [alias aliases]
                              [[alias conv-unit (num-cast (/ 1 (num/exactly rate)))]
                               [conv-unit alias (num-cast (num/exactly rate))]])
                            (apply concat)
                            (concat [[unit conv-unit (num-cast (/ 1 (num/exactly rate)))]
                                     [conv-unit unit (num-cast (num/exactly rate))]])))
                     (apply concat)
                     (apply ->weighted-digraph))
              fn-chart (->> (g/all-pairs-shortest-paths units-graph *)
                            g/root-node-paths)
              unit-type-k        (-> unit-type name keyword)
              nodes              (into #{} (g/nodes units-graph))
              conversion-map-sym 'conversion-map
              conversion-map
                (when (= emit-type :map)
                  (reduce (fn [m [[from to] multiplier]]
                            (let [multiplier-f (whenc (num-cast (/ 1 multiplier)) (fn= 1) 1)]
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
                                                      (fn1 core/conj ~unit-type-k)
                                                      (constantly #{~unit-type-k}))))))))]
               (condp = emit-type
                 :map
                   (list (list 'def 'conversion-map conversion-map))
                 :code
                   (core/for [[[from to] multiplier] fn-chart]
                     (let [multiplier (num-cast (/ 1 multiplier))
                           fn-name (symbol (str (->str from "-") "->" (->str to "-")))]
                       (if (= multiplier 1) ; Identical
                           `(defn ~'fn-name [n] n)
                           `(defn ~'fn-name [n] (* n ~multiplier))))))))))))

#?(:clj
(defmacro defunits-of [unit-type std-unit & {:as unit-pairs}]
  (defunits-of* &env unit-type std-unit unit-pairs :map)))
