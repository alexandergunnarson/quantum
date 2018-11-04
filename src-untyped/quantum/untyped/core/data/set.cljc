(ns quantum.untyped.core.data.set
  (:refer-clojure :exclude [- +, not, compare < <= >= >])
  (:require
#?@(:clj
   [[seqspert.hash-set]])
    [clojure.core                 :as core]
    [clojure.set                  :as set]
    [linked.core                  :as linked]
    [quantum.untyped.core.compare :as ucomp]
    [quantum.untyped.core.core    :as ucore]
    [quantum.untyped.core.vars
      :refer [defalias]]))

(ucore/log-this-ns)

#?(:clj (def hash-set? (partial instance? clojure.lang.PersistentHashSet)))

(def ordered-set linked/set) ; insertion-ordered set
(def oset        ordered-set)

(def not complement)

; (and a (not b))
(defalias differencel         set/difference)
(defalias -                   differencel)
(defalias relative-complement differencel)

#?(:clj
    (defn union
      "337.050528 msecs (core/union s1 s2)
       158.255666 msecs (seqspert.hash-set/sequential-splice-hash-sets s1 s2)))
       This is superseded by quantum.core.reducers.reduce/join."
      ([] (hash-set))
      ([s0] s0)
      ([s0 s1]
        ; To avoid NullPointerException
        (cond (nil? s0) s1
              (nil? s1) s0
              (core/and (hash-set? s0) (hash-set? s1))
              (seqspert.hash-set/sequential-splice-hash-sets s0 s1)
              :else (set/union s0 s1)))
      ([s0 s1 & ss]
        (reduce union (union s0 s1) ss)))
   :cljs (def union set/union))

(defalias + union)

;; ===== Set-specific comparison ===== ;;

(def ^:const <ident -1) ; subset
(def ^:const =ident  0) ; value-identical
(def ^:const >ident  1) ; superset
(def ^:const ><ident 2) ; intersect
(def ^:const <>ident 3) ; disjoint

(def comparisons #{<ident =ident >ident ><ident <>ident})
(def comparison? comparisons)

(defn invert-comparison [^long c #_comparison? #_> #_comparison?]
  (case c
    -1      >ident
     1      <ident
    (0 2 3) c))

(defn normalize-comparison [^long c #_comparison?]
  (case c (2 3) 0 c))

(defn compare [s0 #_set?, s1 #_set?]
  (if (empty? s0)
      (if (empty? s1) =ident <ident)
      (if (empty? s1)
          >ident
          ;; TODO do fewer comparisons here
          (let [diff0 (- s0 s1), diff1 (- s1 s0)]
            (if (empty? diff0)
                (if (empty? diff1)
                    =ident
                    <ident)
                (if (empty? diff1)
                    >ident
                    (if (some #(contains? s1 %) s0)
                        ><ident
                        <>ident)))))))

(defn comparison<     [c] (identical? c <ident))
(defn comparison<=    [c] (or (identical? c <ident) (identical? c =ident)))
(defn comparison=     [c] (identical? c =ident))
(defn comparison-not= [c] (core/not (identical? c =ident)))
(defn comparison>=    [c] (or (identical? c >ident) (identical? c =ident)))
(defn comparison>     [c] (identical? c >ident))
(defn comparison><    [c] (identical? c ><ident))
(defn comparison<>    [c] (identical? c <>ident))

(defn comp<     ([      x0 x1] (comp<            compare x0 x1))
                ([compf x0 x1] (comparison<     (compf   x0 x1))))
(defn comp<=    ([      x0 x1] (comp<=           compare x0 x1))
                ([compf x0 x1] (comparison<=    (compf   x0 x1))))
(defn comp=     ([      x0 x1] (comp=            compare x0 x1))
                ([compf x0 x1] (comparison=     (compf   x0 x1))))
(defn comp-not= ([      x0 x1] (comp-not=        compare x0 x1))
                ([compf x0 x1] (comparison-not= (compf   x0 x1))))
(defn comp>=    ([      x0 x1] (comp>=           compare x0 x1))
                ([compf x0 x1] (comparison>=    (compf   x0 x1))))
(defn comp>     ([      x0 x1] (comp>            compare x0 x1))
                ([compf x0 x1] (comparison>     (compf   x0 x1))))
(defn comp><    ([      x0 x1] (comp><           compare x0 x1))
                ([compf x0 x1] (comparison><    (compf   x0 x1))))
(defn comp<>    ([      x0 x1] (comp<>           compare x0 x1))
                ([compf x0 x1] (comparison<>    (compf   x0 x1))))

(defn <     [x0 x1] (comp<  x0 x1))
(defalias proper-subset? <)
(defn <=    [x0 x1] (comp<= x0 x1))
(defalias subset? <=)
(defn >=    [x0 x1] (comp>= x0 x1))
(defalias superset? >=)
(defn >     [x0 x1] (comp>  x0 x1))
(defalias proper-superset? >)
(defn ><    [x0 x1] (comp>< x0 x1))
(defn <>    [x0 x1] (comp<> x0 x1))
(defalias disjoint? <>)
