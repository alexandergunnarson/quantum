(ns quantum.core.data.time
  (:require
    [quantum.core.data.numeric :as dn]
    [quantum.core.type         :as t]))

;; TODO is this the right place to put this?
#?(:cljs (t/defn date>millis [x js/Date > (t/assume dn/std-fixint?)] (.valueOf x)))
