(ns quantum.core.cache
  (:refer-clojure :exclude [memoize])
  (:require
    [quantum.untyped.core.cache :as u]
    [quantum.untyped.core.vars  :as uvar
      :refer [defaliases]]))

(defaliases u
  memoize* memoize #?(:clj defmemoized)
  callable-times
  init! clear!)

