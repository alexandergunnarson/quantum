(ns quantum.numeric.geometry
  (:require
    [quantum.core.error :as err
      :refer [TODO]]))

(defn smallest-enclosing-ball
  "Uses the Emo Welzl algorithm to find the smallest
   enclosing ball in linear time."
  {:implemented-by '#{org.apache.commons.math3.geometry.enclosing.*}}
  [pts] (TODO))

(defn convex-hull
  "Solution to the convex hull problem."
  {:implemented-by '#{org.apache.commons.math3.geometry.hull.*}}
  [pts] (TODO))
