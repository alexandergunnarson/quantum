(ns
  ^{:doc "UI style functions"}
  quantum.untyped.ui.style.core
  (:require
    [clojure.string                   :as str]
    [quantum.untyped.core.collections :as uc]
    [quantum.untyped.core.core        :as ucore]
    [quantum.untyped.core.identifiers
      :refer [>?name]]
    [quantum.untyped.core.system      :as usys]))

(ucore/log-this-ns)

; Flex helpers
(def layout-x         :row)
(def layout-y         :column)
(def layout           :justify-content)
(def layout-perp      :align-items)
(def layout-start     :flex-start)
(def layout-end       :flex-end)
(def layout-wrap      :flex-wrap)
(def layout-direction :flex-direction)
(def layout-fit       :flex)
(def autofit          1)

(def scaling-factors
  {:iphone-6 0.58})

(def scaling-factor
  (case usys/os
    "ios" (:iphone-6 scaling-factors)
    1))

(defn scaled [x] (* x scaling-factor))

(defn >class [& classes]
  (->> classes (uc/lmap >?name) (str/join " ")))
