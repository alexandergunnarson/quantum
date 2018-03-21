(ns
  ^{:doc "UI style functions"}
  quantum.ui.style.core
  (:require
    [quantum.untyped.core.log :as log]
    [quantum.untyped.core.vars
      :refer [defaliases]]
    [quantum.untyped.ui.style.core :as u]))

(log/this-ns)

(defaliases u
  layout-x layout-y layout
  layout-perp layout-wrap layout-direction
  layout-fit autofit
  scaling-factors scaling-factor scaled)
