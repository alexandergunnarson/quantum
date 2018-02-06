(ns quantum.ui.style.fonts
  (:require
    [quantum.core.log               :as log
      :include-macros true]
    [quantum.core.vars
      :refer [defaliases]]
    [quantum.untyped.ui.style.fonts :as u]))

(log/this-ns)

(defaliases u ios-default-fonts families family link font)
