(ns quantum.core.data.time
  (:require
    [quantum.core.data.numeric :as dnum]
    [quantum.core.type         :as t]))

;; TODO is this the right place to put this?
#?(:cljs (t/defn date>millis [x js/Date > (t/assume dnum/ni-double?)] (.valueOf x)))
