(ns quantum.core.data.set
  "A set may be thought of as a special type of Map whose keys and vals are identical."
        (:refer-clojure :exclude
          [+ -, and or not, compare, split-at hash-set])
        (:require
          ;; TODO TYPED excise
          [clojure.core             :as core]
  #?(:clj [clojure.data.finger-tree :as ftree])
  #?(:clj [clojure.data.int-map     :as imap])
          ;; TODO TYPED excise
          [clojure.set              :as set]
          ;; TODO TYPED excise
          [clojure.data.avl         :as avl]
          [linked.core              :as linked]
          [quantum.core.vars        :as var
            :refer [defalias defaliases]]
          ;; TODO TYPED excise
          [quantum.untyped.core.data.set :as uset]
          ;; TODO TYPED excise
          [quantum.untyped.core.error :as uerr]
  #?(:clj [seqspert.hash-set]))
#?(:clj (:import
          [it.unimi.dsi.fastutil.doubles DoubleOpenHashSet]
          [it.unimi.dsi.fastutil.ints    IntOpenHashSet]
          [it.unimi.dsi.fastutil.longs   LongOpenHashSet]
          [java.util                     HashSet])))

;; ===== Sets ===== ;;

#?(:clj  (def java-set?              (t/isa? java.util.Set)))

;; ----- Identity Sets (identity-based equality) ----- ;;

        (def   +identity-set? t/none?)
        (def  !+identity-set? t/none?)
        (def ?!+identity-set? (t/or +identity-set? !+identity-set?))

        (var/def !identity-set?
          "`java.util.IdentityHashSet` doesn't exist."
          #?(:clj t/none? :cljs (t/isa? js/Set)))

#?(:clj (def  !!identity-set? t/none?))

        (def    identity-set? (t/or ?!+identity-set? !identity-set? #?(:cljs !!identity-set?)))

;; ----- Hash Sets (value-based equality) ----- ;;

(def   +hash-set?        (t/isa? #?(:clj  clojure.lang.PersistentHashSet
                                    :cljs cljs.core/PersistentHashSet)))
(def  !+hash-set?        (t/isa? #?(:clj  clojure.lang.PersistentHashSet$TransientHashSet
                                    :cljs cljs.core/TransientHashSet)))
(def ?!+hash-set?        (t/or +hash-set? !+hash-set?))

(def   !hash-set|byte?   #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.ByteOpenHashSet)
                            :cljs t/none?))
(def   !hash-set|char?   #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.CharOpenHashSet)
                            :cljs t/none?))
(def   !hash-set|short?  #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.ShortOpenHashSet)
                            :cljs t/none?))
(def   !hash-set|int?    #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.IntOpenHashSet)
                            :cljs t/none?))
(def   !hash-set|long?   #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.LongOpenHashSet)
                            :cljs t/none?))
(def   !hash-set|float?  #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.FloatOpenHashSet)
                            :cljs t/none?))
(def   !hash-set|double? #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet)
                            :cljs t/none?))

(def   !hash-set|ref?    #?(:clj  (t/or (t/isa? java.util.HashSet)
                                        (t/isa? it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet))
                          :cljs t/none?))

(def   !hash-set?
  (t/or !hash-set|ref?
        !hash-set|byte? !hash-set|short? !hash-set|char? !hash-set|int? !hash-set|long?
        !hash-set|float? !hash-set|double?))

#?(:clj
(var/def !!hash-set?
  "CLJ technically can have a `!!hash-set?` via `java.util.concurrent.ConcurrentHashMap` with same
   KVs but this hasn't been implemented yet."
  t/none?))

(def hash-set? (t/or ?!+hash-set? !hash-set? #?(:clj !!hash-set?)))

;; ----- Unsorted Sets ----- ;;

(def   +unsorted-set?        +hash-set?)
(def  !+unsorted-set?       !+hash-set?)
(def ?!+unsorted-set?      ?!+hash-set?)

(def   !unsorted-set|byte?   !hash-set|byte?)
(def   !unsorted-set|short?  !hash-set|char?)
(def   !unsorted-set|char?   !hash-set|short?)
(def   !unsorted-set|int?    !hash-set|int?)
(def   !unsorted-set|long?   !hash-set|long?)
(def   !unsorted-set|float?  !hash-set|float?)
(def   !unsorted-set|double? !hash-set|double?)
(def   !unsorted-set|ref?    !hash-set|ref?)

(def   !unsorted-set?
  (t/or !unsorted-set|ref?
        !unsorted-set|byte? !unsorted-set|short? !unsorted-set|char?
        !unsorted-set|int? !unsorted-set|long?
        !unsorted-set|float? !unsorted-set|double?))

#?(:clj (def !!unsorted-set? !!hash-set?))
        (def   unsorted-set?   hash-set?)

;; ----- Sorted Sets ----- ;;

(def   +sorted-set?         (t/isa? #?(:clj  clojure.lang.PersistentTreeSet
                                       :cljs cljs.core/PersistentTreeSet)))
(def  !+sorted-set?         t/none?)
(def ?!+sorted-set?         (t/or +sorted-set? !+sorted-set?))

(def   !sorted-set|byte?    #?(:clj  (t/isa? it.unimi.dsi.fastutil.bytes.ByteSortedSet)
                               :cljs t/none?))
(def   !sorted-set|short?   #?(:clj  (t/isa? it.unimi.dsi.fastutil.shorts.ShortSortedSet)
                               :cljs t/none?))
(def   !sorted-set|char?    #?(:clj  (t/isa? it.unimi.dsi.fastutil.chars.CharSortedSet)
                               :cljs t/none?))
(def   !sorted-set|int?     #?(:clj  (t/isa? it.unimi.dsi.fastutil.ints.IntSortedSet)
                               :cljs t/none?))
(def   !sorted-set|long?    #?(:clj  (t/isa? it.unimi.dsi.fastutil.longs.LongSortedSet)
                               :cljs t/none?))
(def   !sorted-set|float?   #?(:clj  (t/isa? it.unimi.dsi.fastutil.floats.FloatSortedSet)
                               :cljs t/none?))
(def   !sorted-set|double?  #?(:clj  (t/isa? it.unimi.dsi.fastutil.doubles.DoubleSortedSet)
                               :cljs t/none?))

(var/def !sorted-set|ref?
  "CLJS technically can have a `!sorted-set|ref?` via `goog.structs.AVLTree` with same KVs but this
   hasn't been implemented yet."
  #?(:clj (t/isa? java.util.TreeSet) :cljs t/none?))

(def !sorted-set?
  (t/or !sorted-set|ref?
        !sorted-set|byte? !sorted-set|short? !sorted-set|char? !sorted-set|int? !sorted-set|long?
        !sorted-set|float? !sorted-set|double?))

#?(:clj
(var/def !!sorted-set?
  "CLJ technically can have a `!!sorted-set?` via a `java.util.concurrent.ConcurrentSkipListMap`
   with same KVs but this hasn't been implemented yet."
  t/none?))

(def sorted-set?
  (t/or ?!+sorted-set? !sorted-set? #?@(:clj [!!sorted-set? (t/isa? java.util.SortedSet)])))

;; ----- Other Sets ----- ;;

(def   +insertion-ordered-set? (or (isa? linked.set.LinkedSet)
                                   ;; This is true, but we have replaced OrderedSet with LinkedSet
                                 #_(:clj (isa? flatland.ordered.set.OrderedSet))))
(def  !+insertion-ordered-set? t/none?
                               ;; This is true, but we have replaced OrderedSet with LinkedSet
                             #_(t/isa? flatland.ordered.set.TransientOrderedSet))
(def ?!+insertion-ordered-set? (t/or +insertion-ordered-set? !+insertion-ordered-set?))

(def   !insertion-ordered-set? #?(:clj (t/isa? java.util.LinkedHashSet) :cljs t/none?))

#?(:clj
(var/def !!insertion-ordered-set?
  "CLJ technically can have this via a `java.util.concurrent.ConcurrentLinkedHashMap with same KVs
   but this hasn't been implemented yet."
  t/none?))

(def insertion-ordered-set?
  (t/or ?!+insertion-ordered-set? !insertion-ordered-set? #?(:clj !!insertion-ordered-set?)))

;; ----- General Sets ----- ;;

(def  !+set?        (t/isa? #?(:clj  clojure.lang.ITransientSet
                               :cljs cljs.core/ITransientSet)))

(var/defalias ut/+set|built-in?)

(def   +set?        (t/isa? #?(:clj  clojure.lang.IPersistentSet
                               :cljs cljs.core/ISet)))

(def ?!+set?        (t/or !+set? +set?))

(def   !set|byte?   #?(:clj (t/isa? it.unimi.dsi.fastutil.bytes.ByteSet)     :cljs t/none?))
(def   !set|short?  #?(:clj (t/isa? it.unimi.dsi.fastutil.shorts.ShortSet)   :cljs t/none?))
(def   !set|char?   #?(:clj (t/isa? it.unimi.dsi.fastutil.chars.CharSet)     :cljs t/none?))
(def   !set|int?    #?(:clj (t/isa? it.unimi.dsi.fastutil.ints.IntSet)       :cljs t/none?))
(def   !set|long?   #?(:clj (t/isa? it.unimi.dsi.fastutil.longs.LongSet)     :cljs t/none?))
(def   !set|float?  #?(:clj (t/isa? it.unimi.dsi.fastutil.floats.FloatSet)   :cljs t/none?))
(def   !set|double? #?(:clj (t/isa? it.unimi.dsi.fastutil.doubles.DoubleSet) :cljs t/none?))
(def   !set|ref?    (t/or !unsorted-set|ref? !sorted-set|ref?))

(def   !set?        (t/or !set|ref?
                          !set|byte? !set|short? !set|char? !set|int? !set|long?
                          !set|float? !set|double?))

#?(:clj (def !!set? (t/or !!unsorted-set? !!sorted-set?)))
        (def   set? (t/or ?!+set? !set? #?@(:clj [!!set? (t/isa? java.util.Set)])))

; ============ STRUCTURES ============

        (defalias ordered-set         linked/set) ; insertion-ordered set
        (defalias oset                ordered-set)
#?(:clj (defalias c-sorted-set        ftree/counted-sorted-set)) ; sorted set that provides log-n nth
        (defalias sorted-rank-set     avl/sorted-set   )
        (defalias sorted-rank-set-by  avl/sorted-set-by)
        (defalias nearest             avl/nearest      )
        (defalias rank-of             avl/rank-of      )
        (defalias subrange            avl/subrange     )
        (defalias split-key           avl/split-key    )
        (defalias split-at            avl/split-at     )

#?(:clj (defalias long-set            imap/int-set))
#?(:clj (defalias set|long            long-set))
        (defalias hash-set            core/hash-set)
#?(:clj (defalias hash-set|long       set|long))
#?(:clj (defalias dense-long-set      imap/dense-int-set))
#?(:clj (defalias set|long|dense      dense-long-set))
#?(:clj (defalias hash-set|long|dense set|long|dense))

#?(:clj (defalias hash-set? uset/hash-set?))

(defn ->set
  "Like `clojure.core/set`"
  [xs] (uerr/TODO))

#?(:clj
(defn !bit-set
  "There is the java.util.BitSet implementation.
   There is also net/openhft/chronicle/algo/bitset/ReusableBitSet:
     - has a rigid `logicalSize`|capacity
     - attempts to `get`, `set` or `clear` bits at indices
       exceeding the size cause an `IndexOutOfBoundsException`"
  [& args] (uerr/TODO)))

#?(:clj
(defn !bit-set-frame
  {:see-also "net/openhft/chronicle/algo/bitset/SingleThreadedFlatBitSetFrame"}
  [& args] (uerr/TODO)))

#?(:clj
(defn !!bit-set-frame
  {:see-also "net/openhft/chronicle/algo/bitset/ConcurrentFlatBitSetFrame"}
  [& args] (uerr/TODO)))

;; ===== Comparison ===== ;;

(defaliases u compare < proper-subset? <= subset? >= superset? > proper-superset?)

; ============ OPERATIONS ============

(defalias not complement)

; `union` <~> `lodash/union`
; TODO `union-by` <~> `lodash/unionBy`
; TODO `union-with` <~> `lodash/unionWith`
; TODO use |clojure.data.int-map/union, intersection, difference| for int sets and dense int sets
; Benchmark these
(defalias union uset/union)
(defalias or union)
(defalias +  union)

#?(:clj
(defn punion
  "337.050528 msecs (core/union s1 s2)
   28.837984  msecs (seqspert.hash-set/parallel-splice-hash-sets s1 s2)))
   This is superseded by quantum.core.reducers.fold/pjoin."
  ([] (hash-set))
  ([s0] s0)
  ([s0 s1]
    (cond (nil? s0) s1
          (nil? s1) s0
          (core/and (hash-set? s0) (hash-set? s1))
            (#?(:clj  seqspert.hash-set/parallel-splice-hash-sets
                :cljs seqspert.hash-set/sequential-splice-hash-sets) s0 s1)
          :else (throw (ex-info "Could not perform parallel union; can try sequential." {}))))
  ([s0 s1 & ss]
    (reduce punion (punion s0 s1) ss))))

; `intersection` <~> `lodash/intersection`
; TODO `intersection-by` <~> `lodash/intersectionBy`
; TODO `intersection-with` <~> `lodash/intersectionWith`
(defalias intersection        set/intersection)
(defalias and                 intersection    )

; (and a (not b))
(defaliases u - relative-complement differencel)

(def differencer (fn/reversea differencel))

; `symmetric-difference` <~> `lodash/xor`
; TODO `symmetric-difference-by` <~> `lodash/xorBy`
(defn symmetric-difference
  "Analogous to logical `xor`.
   Returns the symmetric difference between a and b.
   That is, (a - b) ∪ (b - a), or (a ∪ b) - (a ∩ b)
   AKA disjunctive union."
  [a b] (difference (union a b) (intersection a b))) ; perhaps the code is quicker to do in another way?

(defalias xor symmetric-difference)

(defalias rename-keys  set/rename-keys)

; TODO generate these functions via macros
(defn #?(:clj ^HashSet !hash-set :cljs !hash-set)
  "Creates a single-threaded, mutable hash set.
   On the JVM, this is a java.util.HashSet.

   On JS, this is a ECMAScript 6 Set (`js/Set`)."
  ([] #?(:clj (HashSet.) :cljs (js/Set.)))
  ([v0]
    (doto #?(:clj (HashSet.) :cljs (js/Set.))
          (.add v0)))
  ([v0 v1]
    (doto #?(:clj (HashSet.) :cljs (js/Set.))
          (.add v0)
          (.add v1)))
  ([v0 v1 v2]
    (doto #?(:clj (HashSet.) :cljs (js/Set.))
          (.add v0)
          (.add v1)
          (.add v2)))
  ([v0 v1 v2 v3]
    (doto #?(:clj (HashSet.) :cljs (js/Set.))
          (.add v0)
          (.add v1)
          (.add v2)
          (.add v3)))
  ([v0 v1 v2 v3 v4]
    (doto #?(:clj (HashSet.) :cljs (js/Set.))
          (.add v0)
          (.add v1)
          (.add v2)
          (.add v3)
          (.add v4)))
  ([v0 v1 v2 v3 v4 v5]
    (doto #?(:clj (HashSet.) :cljs (js/Set.))
          (.add v0)
          (.add v1)
          (.add v2)
          (.add v3)
          (.add v4)
          (.add v5)))
  ([v0 v1 v2 v3 v4 v5 v6 & vs]
    (reduce
      (fn [#?(:clj ^HashSet xs :cljs xs) v] (doto xs (.add v)))
      (doto #?(:clj (HashSet.) :cljs (js/Set.))
            (.add v0)
            (.add v1)
            (.add v2)
            (.add v3)
            (.add v4)
            (.add v5)
            (.add v6))
      vs)))

; TODO generate these functions via macros
#?(:clj (defn ^IntOpenHashSet    !hash-set|int    [] (IntOpenHashSet.   )))
#?(:clj (defn ^LongOpenHashSet   !hash-set|long   [] (LongOpenHashSet.  )))
#?(:clj (defn ^DoubleOpenHashSet !hash-set|double [] (DoubleOpenHashSet.)))
