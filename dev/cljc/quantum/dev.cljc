(ns quantum.dev
  (:refer-clojure :exclude [reduce for])
  (:require [clojure.core :as core]
            [quantum.core.collections :as coll
              :refer [for lfor reduce join map+ vals+ filter+ flatten-1+ range+ kmap ffilter]]
            [quantum.core.numeric :as num]
            [quantum.core.fn :as fn
              :refer [<- fn-> fn->>]]
            [quantum.core.error
              :refer [->ex]]
            [quantum.core.logic
              :refer [coll-or condpc]]
            [quantum.core.reducers :as red
              :refer [reduce-count]]
            [quantum.numeric.core
              :refer [∏ ∑ sum]]))

#?(:cljs (enable-console-print!))
(println "Hey console!")
