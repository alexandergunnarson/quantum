(ns quantum.numeric.geometry
  {:todo #{"Incorporate https://github.com/thi-ng/geom/blob/master/geom-core/src/intersect.org"
           "Incorporate https://github.com/thi-ng/geom/blob/master/geom-core/src/utils.org"
           "Incorporate https://github.com/thi-ng/geom/blob/master/geom-core/src/core.org"
           "Incorporate https://github.com/thi-ng/ndarray/blob/master/src/contours.org"
           "Incorporate https://github.com/thi-ng/geom/tree/master/geom-types/src"
           "Incorporate https://github.com/thi-ng/geom/tree/master/geom-meshops/src"}}
  (:require
    [quantum.core.error :as err
      :refer [TODO]]))

(defn smallest-enclosing-ball
  "Uses the Emo Welzl algorithm to find the smallest enclosing ball in linear time."
  {:implemented-by '#{org.apache.commons.math3.geometry.enclosing.*}}
  [pts] (TODO))

(defn convex-hull
  "Solution to the convex hull problem."
  {:implemented-by '#{org.apache.commons.math3.geometry.hull.*}}
  [pts] (TODO))
