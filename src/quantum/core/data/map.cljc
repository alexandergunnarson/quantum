(ns
  ^{:doc "Useful map functions. |map-entry|, a better merge, sorted-maps, etc."
    :attribution "alexandergunnarson"}
  quantum.core.data.map
  (:refer-clojure :exclude
    [split-at, merge, sorted-map sorted-map-by, array-map, hash-map])
  (:require
    [quantum.untyped.core.data.map :as u]
    [quantum.untyped.core.vars
      :refer [defaliases]]))

(defaliases u
  #?@(:clj [int-map hash-map:long->ref])
  array-map hash-map ordered-map om #?(:clj !ordered-map) #?(:clj kw-omap)
  sorted-map      sorted-map-by sorted-map-by-val
  sorted-rank-map sorted-rank-map-by
  nearest rank-of subrange split-key split-at
  map-entry map-entry-seq
  #?(:clj hash-map?)
  merge #?(:clj pmerge)
  !hash-map
  #?@(:clj [!hash-map:int->ref    !hash-map:int->object
            !hash-map:long->long  !hash-map:long
            !hash-map:long->ref   !hash-map:long->object
            !hash-map:double->ref !hash-map:double->object
            !hash-map:ref->long   !hash-map:object->long])
  bubble-max-key difference-by-key union-by-key intersection-by-key)
