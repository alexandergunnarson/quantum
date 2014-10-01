(ns quanta.library.data.map
  (:refer-clojure :exclude [split-at])
  (:require
    [quanta.library.ns    :as ns :refer [defalias]]
    [clojure.data.avl     :as avl]
    [flatland.ordered.map :as map])
  (:gen-class))

(set! *warn-on-reflection* true)
; (:refer-clojure :exclude [sorted-map sorted-map-by])

(defn map-entry [key-0 val-0] (clojure.lang.MapEntry. key-0 val-0))
(defalias ordered-map map/ordered-map)
; a better merge?
; a better merge-with?
(defn merge+ [map-0 & maps] ; 782.922731 ms /merge+/ vs. 1.133217 sec normal /merge/ ; 1.5 times faster! 
  (persistent! (reduce conj! (transient map-0) maps)))
(defn merge-deep-with
  "Like `merge-with` but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.

  (merge-deep-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                    {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  => {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  ^{:attribution "clojure.contrib.map-utils via taoensso.encore"}
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))
; (def merge-deep (partial merge-deep-with (fn [x y] y)))
(def merge-deep (partial merge-deep-with second))
(comment (merge-deep {:a {:b {:c {:d :D :e :E}}}}
                     {:a {:b {:g :G :c {:c {:f :F}}}}}))

(defalias sorted-map+    avl/sorted-map)
(defalias sorted-map-by+ avl/sorted-map-by)
(defalias split-at       avl/split-at)
;; find rank of element as primitive long, -1 if not found
; (doc avl/rank-of)
; ;; find element closest to the given key and </<=/>=/> according
; ;; to coll's comparator
; (doc avl/nearest)
; ;; split the given collection at the given key returning
; ;; [left entry? right]
; (doc avl/split-key)
; ;; split the given collection at the given index; similar to
; ;; clojure.core/split-at, but operates on and returns data.avl
; ;; collections
; (doc avl/split-at)
;; return subset/submap of the given collection; accepts arguments
;; reminiscent of clojure.core/{subseq,rsubseq}
; (doc avl/subrange)

; SORTED MAPS AND SETS
; Persistent sorted maps and sets with support for transients and additional O(logN) operations:
; rank queries, "nearest key" lookups, splits by index or key and subsets/submaps.
; data.avl maps and sets behave like the core Clojure variants, with the following differences:
; 1) They have transient counterparts and use transients during construction
; 2) They are typically noticeably faster during lookups and somewhat slower during non-transient "updates" (assoc, dissoc)
; 3) They add some memory overhead - a reference and two ints per key (for implementation reasons).