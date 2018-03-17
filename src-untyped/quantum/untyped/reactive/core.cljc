(ns quantum.untyped.reactive.core
  (:refer-clojure :exclude [atom])
#?(:cljs
  (:require
    [re-frame.core :as re]
    [reagent.core  :as reagent])))

#?(:cljs (def reg-sub re/reg-sub))

#?(:cljs
(def std-interceptors
  [(when js/goog.DEBUG re/debug)]))

#?(:cljs
(defn- reg-event*
  ([reg-event-fn k f] (reg-event* reg-event-fn k std-interceptors f))
  ([reg-event-fn k interceptors f]
    (reg-event-fn k
      (if (false? interceptors)
          nil
          (conj std-interceptors interceptors)) f))))

#?(:cljs (def reg-event-db (partial reg-event* re/reg-event-db)))
#?(:cljs (def reg-event-fx (partial reg-event* re/reg-event-fx)))

#?(:cljs (def reg-fx      re/reg-fx))

#?(:cljs (def sub         re/subscribe))
#?(:cljs (def event!      re/dispatch))
#?(:cljs (def event-sync! re/dispatch-sync))

#?(:cljs (def atom        reagent/atom))
