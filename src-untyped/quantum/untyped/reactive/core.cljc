(ns quantum.untyped.reactive.core
  (:refer-clojure :exclude [atom])
#?(:cljs
  (:require
    [re-frame.core :as re]
    [reagent.core  :as reagent])))

#?(:cljs (def sub             re/subscribe))
#?(:cljs (def transform!      re/dispatch))
#?(:cljs (def transform-sync! re/dispatch-sync))

#?(:cljs (def atom            reagent/atom))
