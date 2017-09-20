(ns quantum.core.untyped.compare
  (:refer-clojure :exclude [==])
  (:require
    [quantum.core.fn   :as fn
      :refer [rcomp]]
    [quantum.core.vars :as var
      :refer [defalias]]))

(defalias == identical?)
(def not== (comp not identical?))

(def class->comparator
  {#?@(:clj
        [Class (fn [^Class a ^Class b]
                 (.compareTo (.getName a) (.getName b)))])})
