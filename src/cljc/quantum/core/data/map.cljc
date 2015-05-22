#?(:clj
(do
  (set! *warn-on-reflection* false)
  (set! *unchecked-math*     false)))

(ns
  ^{:doc "Useful map functions. |map-entry|, a better merge (|merge+|), sorted-maps, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.data.map
  (:refer-clojure :exclude [split-at])
  (:require-quantum [ns])
  (:require
    [clojure.data.avl     :as avl]
    #?(:clj [flatland.ordered.map :as omap])))

(defn map-entry
  "A performant replacement for creating 2-tuples (vectors), e.g., as return values
   in a |kv-reduce| function."
  {:attribution "Alex Gunnarson"}
  [k v]
  #?(:clj  (clojure.lang.MapEntry. k v)
     :cljs [k v]))

#?(:clj
  [(defalias ordered-map omap/ordered-map)
   (defalias om omap/ordered-map)])

(defn merge+
  "A performant drop-in replacemen for |clojure.core/merge|."
  {:attribution "Alex Gunnarson"
   :performance "782.922731 ms |merge+| vs. 1.133217 sec normal |merge| ; 1.5 times faster!"}
  [map-0 & maps]
  (if #?(:clj  (instance?  Editable map-0)
         :cljs (satisfies? Editable map-0))
      (->> maps
           (reduce conj! (transient map-0))
           persistent!)
      (apply merge map-0 maps)))

(defn merge-deep-with
  "Like `merge-with` but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.

  (merge-deep-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                    {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  => {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  {:attribution "clojure.contrib.map-utils via taoensso.encore"
   :todo ["Replace |merge-with| with a more performant version which uses |merge+|."]}
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
          (apply merge-with m maps)
          (apply f maps)))
    maps))

(def merge-deep (partial merge-deep-with second))
(comment (merge-deep {:a {:b {:c {:d :D :e :E}}}}
                     {:a {:b {:g :G :c {:c {:f :F}}}}}))

(def sorted-map+    avl/sorted-map)
(def sorted-map-by+ avl/sorted-map-by)
; TODO: incorporate |split-at| into the quantum.core.collections/split-at protocol
(def split-at       avl/split-at)

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