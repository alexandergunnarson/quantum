(ns quantum.untyped.core.data.set
  (:refer-clojure :exclude [- +, not < <= >= >])
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

;; ===== Comparison ===== ;;

(defn compare [s0 #_set?, s1 #_set?]
  (if (empty? s0)
      (if (empty? s1) ucomp/=ident ucomp/<ident)
      (if (empty? s1)
          ucomp/>ident
          ;; TODO do fewer comparisons here
          (let [diff0 (- s0 s1), diff1 (- s1 s0)]
            (if (empty? diff0)
                (if (empty? diff1)
                    ucomp/=ident
                    ucomp/<ident)
                (if (empty? diff1)
                    ucomp/>ident
                    (if (some #(contains? s1 %) s0)
                        ucomp/><ident
                        ucomp/<>ident)))))))

(defn <     [x0 x1] (ucomp/compf<  compare x0 x1))
(defalias proper-subset? <)
(defn <=    [x0 x1] (ucomp/compf<= compare x0 x1))
(defalias subset? <=)
(defn >=    [x0 x1] (ucomp/compf>= compare x0 x1))
(defalias superset? >=)
(defn >     [x0 x1] (ucomp/compf>  compare x0 x1))
(defalias proper-superset? >)
(defn ><    [x0 x1] (ucomp/compf>< compare x0 x1))
(defn <>    [x0 x1] (ucomp/compf<> compare x0 x1))
(defalias disjoint? <>)
