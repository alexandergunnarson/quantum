(ns
  ^{:doc
      "Various collections functions.

       Includes better versions of the following than clojure.core:

       for, doseq, repeat, repeatedly, range, merge,
       count, vec, reduce, into, first, second, rest,
       last, butlast, get, pop, peek ...

       and more.

       Many of them are aliased from other namespaces like
       quantum.core.collections.core, or quantum.core.reducers."
    :attribution "alexandergunnarson"}
  quantum.core.collections.generative
  (:refer-clojure :exclude
    [boolean?
     for doseq reduce
     contains?
     repeat repeatedly
     interpose
     range
     take take-while
     drop  drop-while
     subseq
     key val
     merge sorted-map sorted-map-by
     into
     count
     empty empty?
     split-at
     first second rest last butlast get pop peek
     select-keys
     zipmap
     reverse
     conj
     conj! assoc! dissoc! disj!])
  (:require
    [clojure.core                  :as core]
    [quantum.core.collections.core :as coll
      :refer [key val reverse
              count first rest slice last-index-of index-of lasti]]
    [quantum.core.reducers         :as red
      :refer [join]]
    [quantum.core.logic
      :refer [whenc->]]
    [quantum.core.type-old         :as type
      :refer [should-transientize?]]
    [quantum.core.loops            :as loops
      :refer [for fortimes]]
    [quantum.core.macros
      :refer [defnt]]
    [quantum.core.vars             :as var
      :refer [defalias]]))

; TODO technically you can define `repeat` in terms of `repeatedly`; how to optimize though?

; ===== REPEAT ===== ;

(declare range)

(defalias lrepeat core/repeat)

(defn repeat
  ([x] (lrepeat x))
  ([n x] (fortimes [i n] x)))

(defalias repeat+ red/repeat+)

; ===== REPEATEDLY ===== ;

(def lrepeatedly core/repeatedly)

(defn repeatedly
  ([f] (lrepeatedly f))
  ([n f] (fortimes [i n] (f))))

(defalias repeatedly+ red/repeatedly+)

; ===== RANGE ===== ;

#?(:clj (defalias range+ red/range+))

(defn lrrange
  "Lazy reverse range."
  {:usage '(lrrange 0 5)
   :out   '(4 3 2 1 0)}
  ([]    (iterate core/dec 0))
  ([a]   (iterate core/dec a))
  ([a b]
    (->> (iterate core/dec (core/dec b)) (core/take (- b a)))))

(defn lrange
  ([]  (core/range))
  ([a] (core/range a))
  ([a b]
    (if (neg? (- b a))
        (lrrange a b)
        (core/range a b))))

(defn rrange
  "Reverse range"
  {:ret-type 'Vector
   :todo ["Performance with |range+| on [a b] arity, and rseq"]}
  ([]    (lrrange))
  ([a]   (lrrange a))
  ([a b] (->> (range+ a b) (join []) reverse)))

(defn range
  {:ret-type 'Vector
   :todo ["Performance with |range+| on [a b] arity"]}
  ([]    (lrange))
  ([a]   (lrange a))
  ([a b]
    (if (neg? (- a b))
        (rrange a b))
        (->> (range+ a b) (join []))))

#?(:clj
(defnt !range:longs
  {:todo #{"CLJS"}}
  (^"[J" [^long b] (!range:longs 0 b)) ; TODO make hint unnecessary via recursive analysis
  ([^long a ^long b]
    (let [ct  (whenc-> (- b a) neg? 0)
          ret (coll/->longs-nd ct)]
      (dotimes [i ct] (coll/assoc! ret i (+ i a)))
      ret))))
