(ns quantum.dev
  (:refer-clojure :exclude [reduce for])
  (:require  [#?(:clj  clojure.core
                 :cljs cljs.core   )  :as core]
            [quantum.core.collections :as coll]
            [quantum.core.numeric :as num]
            [quantum.core.fn :as fn]
            [quantum.core.error
              :refer [->ex]]
            [quantum.core.logic]
            [quantum.system :as gsys]))

#?(:cljs (enable-console-print!))
(println "Hey console!")
