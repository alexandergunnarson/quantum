(ns quantum.ai.ml.core
  (:require
    [quantum.core.data.validated :as dv]
    [quantum.core.fn
      :refer [fn1]]
    [quantum.core.type           :as t]))

(dv/def instances (fn1 t/sequential?)) ; TODO better validation
(dv/def targets   (fn1 t/sequential?)) ; TODO better validation
