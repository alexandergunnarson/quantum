(ns quantum.core.data.tuple
  (:require
    [quantum.untyped.core.data.tuple :as u]
    [quantum.untyped.core.vars
      :refer [defalias]]))

#?(:clj (defalias u/tuple))
