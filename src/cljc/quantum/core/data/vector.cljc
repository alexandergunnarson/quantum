(ns
  ^{:doc "Vector operations. Includes relaxed radix-balanced vectors (RRB vectors)
          my Michal Marczyk. Also includes |conjl| (for now)."
    :attribution "alexandergunnarson"}
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
      :refer [defalias]])
  #?(:clj (:import java.util.ArrayList)))

; TO EXPLORE
; - michalmarczyk/devec: double-ended vector
; =======================================

; svec = "spliceable vector"
(defalias svec    svec/vec)
(defalias svector svec/vector)

; slice
(defn catvec
  "|empty| checks to get around StackOverflowErrors inherent in |catvec|
   (At least in Clojure version)
   Assumes inputs are vectors."
  {:attribution "alexandergunnarson"}
  ([] (svector))
  ([a] a)
  ([a b]
    (if (empty? a)
        (if (empty? b)
            (svector)
            b)
        (if (empty? b)
            a
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
  {:attribution "alexandergunnarson"}
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

; TODO macro-generate this
(defn !vector
  "Creates a single-threaded, mutable vector.
   On the JVM, this is a java.util.ArrayList.

   On JS, this is a native array (which is really an array list under the hood)."
  ([] #?(:clj (ArrayList.) :cljs #js []))
  ([x0]
    (doto #?(:clj (ArrayList.) :cljs #js [])
          (#?(:clj .add :cljs .push) x0)))
  ([x0 x1]
    (doto #?(:clj (ArrayList.) :cljs #js [])
          (#?(:clj .add :cljs .push) x0)
          (#?(:clj .add :cljs .push) x1)))
  ([x0 x1 x2]
    (doto #?(:clj (ArrayList.) :cljs #js [])
          (#?(:clj .add :cljs .push) x0)
          (#?(:clj .add :cljs .push) x1)
          (#?(:clj .add :cljs .push) x2)))
  ([x0 x1 x2 x3]
    (doto #?(:clj (ArrayList.) :cljs #js [])
          (#?(:clj .add :cljs .push) x0)
          (#?(:clj .add :cljs .push) x1)
          (#?(:clj .add :cljs .push) x2)
          (#?(:clj .add :cljs .push) x3)))
  ([x0 x1 x2 x3 x4]
    (doto #?(:clj (ArrayList.) :cljs #js [])
          (#?(:clj .add :cljs .push) x0)
          (#?(:clj .add :cljs .push) x1)
          (#?(:clj .add :cljs .push) x2)
          (#?(:clj .add :cljs .push) x3)
          (#?(:clj .add :cljs .push) x4)))
  ([x0 x1 x2 x3 x4 x5]
    (doto #?(:clj (ArrayList.) :cljs #js [])
          (#?(:clj .add :cljs .push) x0)
          (#?(:clj .add :cljs .push) x1)
          (#?(:clj .add :cljs .push) x2)
          (#?(:clj .add :cljs .push) x3)
          (#?(:clj .add :cljs .push) x4)
          (#?(:clj .add :cljs .push) x5)))
  ([x0 x1 x2 x3 x4 x5 x6 & xs]
    (reduce
      (fn [#?(:clj ^ArrayList xs :cljs xs) x] (doto xs (#?(:clj .add :cljs .push) x)))
      (doto #?(:clj (ArrayList.) :cljs #js [])
            (#?(:clj .add :cljs .push) x0)
            (#?(:clj .add :cljs .push) x1)
            (#?(:clj .add :cljs .push) x2)
            (#?(:clj .add :cljs .push) x3)
            (#?(:clj .add :cljs .push) x4)
            (#?(:clj .add :cljs .push) x5)
            (#?(:clj .add :cljs .push) x6))
      xs)))

(defalias !array-list !vector)
