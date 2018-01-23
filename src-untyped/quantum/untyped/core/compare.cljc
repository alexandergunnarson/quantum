(ns quantum.untyped.core.compare
  (:refer-clojure :exclude [==]))

(def == identical?)
(def not== (comp not identical?))

(def class->comparator
  {#?@(:clj
        [Class (fn [^Class a ^Class b]
                 (.compareTo (.getName a) (.getName b)))])})

(defn invert [c]
  (case c
     nil  c
     0    c
    -1    1
     1   -1))
