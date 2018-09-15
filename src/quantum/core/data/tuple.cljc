(ns quantum.core.data.tuple
  (:require
    [quantum.core.type               :as t]
    [quantum.core.vars
      :refer [defalias]]
    ;; TODO TYPED excise
    [quantum.untyped.core.data.tuple :as u]))

        ;; clojure.lang.Tuple was discontinued; we won't support it for now
        (def tuple? (t/isa? quantum.untyped.core.data.tuple.Tuple))

#?(:clj (def map-entry? (t/isa? java.util.Map$Entry)))

#?(:clj (defalias u/tuple))
