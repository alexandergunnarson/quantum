(ns quantum.core.data.vector
  "A vector is Sequential, Associative (specifically, whose keys are sequential, dense
   integer values), and extensible."
  (:refer-clojure :exclude
    [vector vector?])
  (:require
    ;; TODO TYPED excise
    [clojure.core              :as core]
    [clojure.core.rrb-vector   :as svec]
#?@(:clj
   [[clojure.core.rrb-vector.protocols
      :refer [PSliceableVector  slicev
              PSpliceableVector splicev]]
    [clojure.core.rrb-vector.rrbt
      :refer [AsRRBT as-rrbt]]])
    [quantum.core.type         :as t]
    [quantum.core.vars         :as var
      :refer [defalias]]
    ;; TODO TYPED excise
    [quantum.core.untyped.fn
      :refer [rcomp]]
    [quantum.core.untyped.type :as ut])
#?(:clj
  (:import
    java.util.ArrayList
    [it.unimi.dsi.fastutil.booleans BooleanArrayList]
    [it.unimi.dsi.fastutil.bytes    ByteArrayList]
    [it.unimi.dsi.fastutil.chars    CharArrayList]
    [it.unimi.dsi.fastutil.shorts   ShortArrayList]
    [it.unimi.dsi.fastutil.ints     IntArrayList]
    [it.unimi.dsi.fastutil.longs    LongArrayList]
    [it.unimi.dsi.fastutil.floats   FloatArrayList]
    [it.unimi.dsi.fastutil.doubles  DoubleArrayList]
    [it.unimi.dsi.fastutil.objects  ObjectArrayList])))

; TO EXPLORE
; - michalmarczyk/devec: double-ended vector
; =======================================

(def !array-list?
  #?(:clj  (t/or (t/isa? java.util.ArrayList)
                 ;; indexed and associative, but not extensible
                 (t/isa? java.util.Arrays$ArrayList))
     :cljs (t/or ;; not used
                 #_(t/isa? cljs.core/ArrayList)
                 ;; because supports .push etc.
                 (t/isa? js/Array))))

(def svector?
  "The set of spliceable vectors."
  (t/isa? #?(:clj  clojure.core.rrb_vector.rrbt.Vector
             :cljs clojure.core.rrb-vector.rrbt.Vector)))

(def   +vector?          (t/isa?|direct #?(:clj  clojure.lang.IPersistentVector
                                           :cljs cljs.core/IVector)))

(defalias ut/+vector|built-in)

(def  !+vector?          (t/isa?|direct #?(:clj  clojure.lang.ITransientVector
                                           :cljs cljs.core/ITransientVector)))

(def ?!+vector?          (t/or +vector? !+vector?))

(def !vector|byte?   #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.ByteArrayList)     :cljs t/none?))
(def !vector|short?  #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.ShortArrayList)   :cljs t/none?))
(def !vector|char?   #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.CharArrayList)     :cljs t/none?))
(def !vector|int?    #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.IntArrayList)       :cljs t/none?))
(def !vector|long?   #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.LongArrayList)     :cljs t/none?))
(def !vector|float?  #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.FloatArrayList)   :cljs t/none?))
(def !vector|double? #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.DoubleArrayList) :cljs t/none?))

(def !vector|ref?
  #?(:clj  (t/or (t/isa? java.util.ArrayList)
                 (t/isa? it.unimi.dsi.fastutil.objects.ReferenceArrayList))
     ;; because supports .push etc.
     :cljs (t/isa? js/Array)))

(def !vector?
  (t/or !vector|ref?
        !vector|byte? !vector|short? !vector|char? !vector|int? !vector|long?
        !vector|float? !vector|double?))

         ;; java.util.Vector is deprecated, because you can
         ;; just create a synchronized wrapper over an ArrayList
         ;; via java.util.Collections
#?(:clj  (def !!vector? t/none?))

;; We could maybe duck-type as `(t/and (isa? java.util.RandomAccess) (isa? java.util.List))` but
;; it's not really sufficient as that doesn't really capture all the properties we want out of a
;; vector
(def vector? (t/or ?!+vector? !vector? #?(:clj !!vector?)))


;; TODO TYPED below


(defalias  vector core/vector)
(defalias +vector vector)
(def !+vector (rcomp vector transient))

(defn !+vector|sized [n]
  (let [xs (!+vector)]
    (dotimes [i n] (conj! xs nil))
    xs))

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

#?(:clj
(defalias
  ^{:doc "Creates a new vector capable of storing homogenous items of type t,
  which should be one of :object, :int, :long, :float, :double, :byte,
  :short, :char, :boolean. Primitives are stored unboxed."}
  svector-of svec/vector-of))

; TODO use |vec+/vec| to convert a vector to an RRBT vector. Benchmark this

; TODO macro-generate this
(defn ^ArrayList !vector
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

#_BooleanArrayList
#_ByteArrayList
#_CharArrayList
#_ShortArrayList
#_IntArrayList
#_LongArrayList
#_FloatArrayList
#_DoubleArrayList
#_ObjectArrayList

#?(:clj (defn ^LongArrayList !vector|long [] (LongArrayList.)))
