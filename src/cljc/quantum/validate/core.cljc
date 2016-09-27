(ns quantum.validate.core
  (:require
    [quantum.core.validate :as v
      :refer [spec]]
    [quantum.core.collections :as coll
      :refer [containsv?]]))

(def no-blanks? (spec (fn no-blanks? [x] (not (containsv? x " ")))))
