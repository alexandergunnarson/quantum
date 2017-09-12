(ns quantum.core.untyped.reducers
  (:refer-clojure :exclude [apply every? vec])
  (:require
    [clojure.core          :as core]
    [clojure.core.reducers :as r]
    [quantum.core.core
      :refer [->sentinel]]
    [quantum.core.fn
      :refer [fn->> rcomp]]
    [quantum.core.untyped.reducers.rfns :as rf]))

(defonce sentinel (->sentinel))

(def ^{:doc "A marriage of `transduce` and `reduce`.
             Like `reduce`, does not have a notion of a transforming function
             (unlike `transduce`). Like `transduce`, uses the seed (0-arity) and
             completing (1-arity) arities of the reducing function when performing
             a reduction (unlike `reduce`)."}
  educe (partial transduce identity))

(defn transducer->reducer
  "Converts a transducer into a reducer."
  {:todo #{"More arity"}}
  ([^long n xf]
    (case n
          0 (fn ([]            (xf))
                ([xs]          (r/reducer xs (xf))))
          1 (fn ([a0]          (xf a0))
                ([a0 xs]       (r/reducer xs (xf a0))))
          2 (fn ([a0 a1]       (xf a0 a1))
                ([a0 a1 xs]    (r/reducer xs (xf a0 a1))))
          3 (fn ([a0 a1 a2]    (xf a0 a1 a2))
                ([a0 a1 a2 xs] (r/reducer xs (xf a0 a1 a2))))
          (throw (ex-info "Unhandled arity for transducer" nil)))))

(def  map+    (transducer->reducer 1 core/map))
(def  map-indexed+    (transducer->reducer 1 core/map-indexed))
(defn map-keys* [f-xs] (fn [f xs] (->> xs (f-xs (juxt (rcomp key f) val)))))
(def  map-keys+ (map-keys* map+))
(defn map-vals* [f-xs] (fn [f xs] (->> xs (f-xs (juxt key (rcomp val f))))))
(def  map-vals+ (map-vals* map+))

(def filter+ (transducer->reducer 1 core/filter))
(defn filter-keys* [f-xs] (fn [pred xs] (->> xs (f-xs (rcomp key pred)))))
(def  filter-keys+ (filter-keys* filter+))

(def remove+ (transducer->reducer 1 core/remove))

(def indexed+ (fn->> (map-indexed+ vector)))

(def partition-all+ (transducer->reducer 1 core/partition-all))

(def lasti (rcomp count dec))

(defn join
  ([from] (if (vector? from) from (join [] from)))
  ([to from] (core/into to from)))

(defn into! [xs0 xs1] (reduce (fn [xs0' x] (conj! xs0' x)) xs0 xs1))

(defn vec [xs] (join xs))

(defn reducei
  "`reduce`, indexed."
  [f init xs]
  (let [f' (let [*i (volatile! -1)]
              (fn ([ret x]
                    (f ret x (vreset! *i (unchecked-inc (long @*i)))))))]
    (reduce f' init xs)))

(defn every-val
  "Yields what every value in `xs` is equivalent to (via `=`), or the provided
   `not-equivalent` value if they are not all equivalent."
  [not-equivalent xs]
  (reduce (fn [ret x]
            (cond (identical? ret sentinel) x
                  (not= x ret)              (reduced not-equivalent)
                  :else                     ret))
          sentinel
          xs))

(defn multiplex
  ([completef rf0]
    (fn ([]      (rf0))
        ([x0]    (completef (rf0 x0)))
        ([x0 x'] (rf0 x0 x'))))
  ([completef rf0 rf1]
    (fn ([]           [(rf0) (rf1)])
        ([[x0 x1]]    (completef (rf0 x0) (rf1 x1)))
        ([[x0 x1] x'] [(rf0 x0 x') (rf1 x1 x')]))))

(defn every?
  "A faster version of `every?` using `reduce` instead of `seq`."
  ([pred] #(every? pred %))
  ([pred xs] (educe (rf/every? pred) xs)))

(defn apply
  "Applies ->`f` to ->`xs`, pairwise, using `reduce`."
  [f xs]
  (let [ret (reduce
              (fn
                ([ret] ret)
                ([ret x]
                  (if (identical? ret sentinel) (f x) (f ret x))))
              sentinel
              xs)]
    (if (identical? ret sentinel) (f) ret)))

