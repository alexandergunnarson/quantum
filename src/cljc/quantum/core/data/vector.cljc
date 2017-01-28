(ns
  ^{:doc "Vector operations. Includes relaxed radix-balanced vectors (RRB vectors)
          my Michal Marczyk. Also includes |conjl| (for now)."
    :attribution "Alex Gunnarson"}
  quantum.core.data.vector
  (:require
    [clojure.core.rrb-vector  :as svec]
#?@(:clj
   [[clojure.core.rrb-vector.protocols
      :refer [PSliceableVector  slicev
              PSpliceableVector splicev]]
    [clojure.core.rrb-vector.rrbt
      :refer [AsRRBT as-rrbt]]])
    [quantum.core.vars        :as var
      :refer [defalias]]))

; TO EXPLORE
; - michalmarczyk/devec: double-ended vector
; =======================================

; svec = "spliceable vector"
(defalias svec    svec/vec)
(defalias svector svec/vector)

; slice
(defn catvec
  "|empty| checks to get around StackOverflowErrors inherent in |catvec|
   (At least in Clojure version)"
  {:attribution "Alex Gunnarson"}
  ([] (svector))
  ([a] a)
  ([a b]
    (if (empty? a)
        (if (empty? b)
            (svector)
            (svec b))
        (if (empty? b)
            (svec a)
            (svec/catvec a b))))
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

(defn subsvec
  "Produces a new vector containing the appropriate subrange of the input vector in logarithmic time
   (in contrast to clojure.core/subvec, which returns a reference to the input vector)
   clojure.core/subvec is a constant-time operation that prevents the underlying vector
   from becoming eligible for garbage collection"
  {:attribution "Alex Gunnarson"}
  [coll a b]
  (try (svec/subvec coll a b)
    (catch
      #?(:clj  IllegalArgumentException
         :cljs js/Error)
      _
      (subsvec coll a b))))

(def svec?
  (partial instance?
    #?(:clj  clojure.core.rrb_vector.rrbt.Vector
       :cljs clojure.core.rrb-vector.rrbt.Vector)))

#?(:clj
(defalias
  ^{:doc "Creates a new vector capable of storing homogenous items of type t,
  which should be one of :object, :int, :long, :float, :double, :byte,
  :short, :char, :boolean. Primitives are stored unboxed."}
  svector-of svec/vector-of))

; TODO use |vec+/vec| to convert a vector to an RRBT vector. Benchmark this
