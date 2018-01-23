(ns quantum.reactive.db
  #?(:cljs
  (:require
    [re-posh.core :as re-rx])))

#?(:cljs (def reg-query!     re-rx/reg-query-sub))
#?(:cljs (def reg-pull!      re-rx/reg-pull-sub))
#?(:cljs (def reg-transform! re-rx/reg-event-ds))
