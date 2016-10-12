(ns quantum.validate.core
  (:require
    [quantum.core.validate :as v
      :refer        [#?@(:clj [spec])]
      :refer-macros [          spec]]
    [quantum.core.collections :as coll
      :refer        [#?@(:clj [containsv?])]
      :refer-macros [          containsv?]]))

(def no-blanks? (spec (fn no-blanks? [x] (not (containsv? x " ")))))
