(ns quantum.core.data.tuple
  (:refer-clojure :exclude
    [map-entry?])
  (:require
    [quantum.core.type               :as t]
    [quantum.core.vars
      :refer [defalias]]
    ;; TODO TYPED excise
    [quantum.untyped.core.data.tuple :as u]))

;; clojure.lang.Tuple was discontinued; we won't support it for now
(def tuple? (t/isa? quantum.untyped.core.data.tuple.Tuple))

(def map-entry? (t/isa|direct? #?(:clj java.util.Map$Entry :cljs cljs.core/IMapEntry)))

#?(:clj (defalias u/tuple))
