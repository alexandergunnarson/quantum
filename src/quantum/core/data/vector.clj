(ns quantum.core.data.vector (:gen-class))

(require
  '[quantum.core.ns       :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[clojure.core.rrb-vector :as vec+])

; RRB Vectors: logarithmic time concatenation and slicing ; O(logN)

; conjl
; slice
(defalias subvec+
  ; produces a new vector containing the appropriate subrange of the input vector in logarithmic time
  ; (in contrast to clojure.core/subvec, which returns a reference to the input vector)
  ; clojure.core/subvec is a constant-time operation that prevents the underlying vector
  ; from becoming eligible for garbage collection
  vec+/subvec)
(defalias catvec  vec+/catvec)
(defalias vec+    vec+/vec)
(defalias vector+ vec+/vector)
(defn vector+? [obj] (instance? Vector obj))
(defn conjl
  ([vec-0 elem]
    (-> elem vector+ (catvec vec-0)))
  ([vec-0 elem & elems]
    (reduce conjl elem elems)))