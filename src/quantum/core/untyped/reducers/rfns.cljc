(ns quantum.core.untyped.reducers.rfns
  (:refer-clojure :exclude [every? < <= = > >=])
  (:require
    [clojure.core :as core]))

(defn every? [pred]
  (fn ([] true) ; vacuously
      ([ret] ret)
      ([_ x] (or (pred x) (reduced false)))))

(defn geometric-every?
  "Meant to be used as the `f` of `r/apply`"
  [pred]
  (fn ([] true) ; vacuously
      ([x] (pred x))
      ([prev x] (if (pred prev x) prev) (or  (reduced false)))))

(def <  (geometric-every? core/< ))
(def <= (geometric-every? core/<=))
(def =  (geometric-every? core/= ))
(def >  (geometric-every? core/> ))
(def >= (geometric-every? core/>=))
