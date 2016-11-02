#?(:clj (do (require '[clojure.tools.namespace.repl :refer [refresh]])
            (refresh)))

(ns quantum.dev
  (:refer-clojure :exclude [reduce for])
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   )  :as core]
    [clojure.tools.namespace.repl
      :refer [refresh]]
    [quantum.core.collections :as coll]
    [quantum.core.numeric     :as num ]
    [quantum.core.fn          :as fn  ]
    [quantum.core.error
      :refer [->ex]]
    [quantum.core.logic]
    [quantum.system :as gsys]
    [quantum.core.time.core :as time]
    [quantum.core.print :as pr
      :refer        [#?(:clj !)]
      :refer-macros [!]]))

#?(:cljs (enable-console-print!))
(println "Hey console!")

#?(:clj (clojure.spec/check-asserts true))
