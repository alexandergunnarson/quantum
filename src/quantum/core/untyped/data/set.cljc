(ns quantum.core.untyped.data.set
  (:refer-clojure :exclude [not])
  (:require
    [flatland.ordered.set :as oset]))

#?(:clj (def ordered-set oset/ordered-set)) ; insertion-ordered set
#?(:clj (def oset        ordered-set))

(def not complement)
