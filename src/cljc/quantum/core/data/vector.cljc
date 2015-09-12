(ns
  ^{:doc "Vector operations. Includes relaxed radix-balanced vectors (RRB vectors)
          my Michal Marczyk. Also includes |conjl| (for now)."
    :attribution "Alex Gunnarson"}
  quantum.core.data.vector
  (:require-quantum [ns])
  (:require
    [clojure.core.rrb-vector :as vec+]
    [clojure.core.rrb-vector.protocols :refer
      [PSliceableVector slicev
      PSpliceableVector splicev]]
    [clojure.core.rrb-vector.rrbt :refer [AsRRBT as-rrbt]]))

; To fix "No implementation of method: :as-rrbt of protocol ___"
; TODO inefficient


(extend-protocol AsRRBT
  clojure.lang.Tuple$T0 (as-rrbt [v] (as-rrbt       (vector)   ))
  clojure.lang.Tuple$T1 (as-rrbt [v] (as-rrbt (into (vector) v)))
  clojure.lang.Tuple$T2 (as-rrbt [v] (as-rrbt (into (vector) v)))
  clojure.lang.Tuple$T3 (as-rrbt [v] (as-rrbt (into (vector) v)))
  clojure.lang.Tuple$T4 (as-rrbt [v] (as-rrbt (into (vector) v)))
  clojure.lang.Tuple$T5 (as-rrbt [v] (as-rrbt (into (vector) v)))
  clojure.lang.Tuple$T6 (as-rrbt [v] (as-rrbt (into (vector) v))))

; TODO inefficient
(extend-protocol PSliceableVector
  clojure.lang.Tuple$T0 (slicev  [v a b] (slicev        (vector)    a b))
  clojure.lang.Tuple$T1 (slicev  [v a b] (slicev  (into (vector) v) a b))
  clojure.lang.Tuple$T2 (slicev  [v a b] (slicev  (into (vector) v) a b))
  clojure.lang.Tuple$T3 (slicev  [v a b] (slicev  (into (vector) v) a b))
  clojure.lang.Tuple$T4 (slicev  [v a b] (slicev  (into (vector) v) a b))
  clojure.lang.Tuple$T5 (slicev  [v a b] (slicev  (into (vector) v) a b))
  clojure.lang.Tuple$T6 (slicev  [v a b] (slicev  (into (vector) v) a b)))

; TODO inefficient
(extend-protocol PSpliceableVector
  clojure.lang.Tuple$T0 (splicev [v1 v2] (splicev       (vector)    v2))
  clojure.lang.Tuple$T1 (splicev [v1 v2] (splicev (into (vector) v1) v2))
  clojure.lang.Tuple$T2 (splicev [v1 v2] (splicev (into (vector) v1) v2))
  clojure.lang.Tuple$T3 (splicev [v1 v2] (splicev (into (vector) v1) v2))
  clojure.lang.Tuple$T4 (splicev [v1 v2] (splicev (into (vector) v1) v2))
  clojure.lang.Tuple$T5 (splicev [v1 v2] (splicev (into (vector) v1) v2))
  clojure.lang.Tuple$T6 (splicev [v1 v2] (splicev (into (vector) v1) v2)))


(def vec+    vec+/vec)
(def vector+ vec+/vector)

; slice
(defn catvec
  "|empty| checks to get around StackOverflowErrors inherent in |catvec|
   (At least in Clojure version)"
  {:attribution "Alex Gunnarson"}
  ([a] a)
  ([a b]
    (if (empty? a)
        (if (empty? b)
            (vector+)
            (vec+ b))
        (if (empty? b)
            (vec+ a)
            (vec+/catvec a b))))
  ([a b c]
    (catvec (catvec a b) c))
  ([a b c d]
    (catvec (catvec a b c) d))
  ([a b c d e]
    (catvec (catvec a b c d) e))
  ([a b c d e f]
    (catvec (catvec a b c d e) f))
  ([a b c d e f & more]
    (reduce catvec (catvec a b c d e f) more)))

(defn subvec+
  "Produces a new vector containing the appropriate subrange of the input vector in logarithmic time
   (in contrast to clojure.core/subvec, which returns a reference to the input vector)
   clojure.core/subvec is a constant-time operation that prevents the underlying vector
   from becoming eligible for garbage collection"
  {:attribution "Alex Gunnarson"}
  [coll a b]
  (try (vec+/subvec coll a b)
    (catch
      #?(:clj  IllegalArgumentException
         :cljs js/Error)
      _
      (subvec coll a b))))

(defn vector+? [obj]
  (instance?
    #?(:clj  clojure.core.rrb_vector.rrbt.Vector
      :cljs clojure.core.rrb-vector.rrbt.Vector) obj))

