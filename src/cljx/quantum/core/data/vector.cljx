(ns
  ^{:doc "Vector operations. Includes relaxed radix-balanced vectors (RRB vectors)
          my Michal Marczyk. Also includes |conjl| (for now)."
    :attribution "Alex Gunnarson"}
  quantum.core.data.vector
  (:require
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    [quantum.core.type       :as type :refer [instance+?]]
    [clojure.core.rrb-vector :as vec+]
    #+cljs [clojure.core.rrb-vector.rrbt])
  #+clj
  (:import
    clojure.core.Vec
    (clojure.core.rrb_vector.rrbt Vector)
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

#+clj (ns/require-all *ns* :clj)

; slice
(def catvec  vec+/catvec)
(def vec+    vec+/vec)
(def vector+ vec+/vector)

(defn subvec+
  "Produces a new vector containing the appropriate subrange of the input vector in logarithmic time
   (in contrast to clojure.core/subvec, which returns a reference to the input vector)
   clojure.core/subvec is a constant-time operation that prevents the underlying vector
   from becoming eligible for garbage collection"
  {:attribution "Alex Gunnarson"}
  [coll a b]
  (try (vec+/subvec coll a b)
    (catch
      #+clj  IllegalArgumentException
      #+cljs js/Error
      _
      (subvec coll a b))))

(defn vector+? [obj]
  (instance+?
    #+clj  clojure.core.rrb_vector.rrbt.Vector
    #+cljs clojure.core.rrb-vector.rrbt.Vector obj))

(defn conjl
  {:attribution "Alex Gunnarson"
   :todo ["Add support for conjl with other data structures."
          "This shouldn't go in this namespace."]}
  ([vec-0 elem]
    (-> elem vector+ (catvec vec-0)))
  ([vec-0 elem & elems]
    (reduce conjl elem elems)))
