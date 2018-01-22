(ns
  ^{:doc "UI style functions"}
  quantum.ui.style.core
  (:require
    [quantum.core.log :as log
      :include-macros true]
    [quantum.core.system :as sys]))

(log/this-ns)

; Flex helpers
(def layout-x         :row)
(def layout-y         :column)
(def layout           :justify-content)
(def layout-perp      :align-items)
(def layout-wrap      :flex-wrap)
(def layout-direction :flex-direction)
(def layout-fit       :flex)
(def autofit          1)

(def scaling-factors
  {:iphone-6 0.58})

(def scaling-factor
  (case sys/os
    "ios" (:iphone-6 scaling-factors)
    1))

(defn scaled [x] (* x scaling-factor))
