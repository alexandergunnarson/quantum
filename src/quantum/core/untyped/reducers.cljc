(ns quantum.core.untyped.reducers
  (:refer-clojure :exclude [apply every? vec == for])
  (:require
    [clojure.core                 :as core]
    [clojure.core.reducers        :as r]
    [fast-zip.core                :as zip]
    [quantum.core.core
      :refer [->sentinel]]
    [quantum.core.fn
      :refer [fn->> rcomp]]
    [quantum.core.untyped.compare :as comp
      :refer [== not==]]))

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

(def  mapcat+ (transducer->reducer 1 core/mapcat))
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

(defn zip-reduce* [f init z]
  (loop [xs (zip/down z) v init]
    (if (nil? xs)
        v
        (let [ret (f v xs)]
          (if (reduced? ret)
              @ret
              (recur (zip/right xs) ret))))))

(defn reducei
  "`reduce`, indexed."
  [f init xs]
  (let [f' (let [*i (volatile! -1)]
              (fn ([ret x]
                    (f ret x (vreset! *i (unchecked-inc (long @*i)))))))]
    (reduce f' init xs)))

(defn reduce-pair
  "Like |reduce|, but reduces over two items in a collection at a time.

   Its function @func must take three arguments:
   1) The accumulated return value of the reduction function
   2) The                next item in the collection being reduced over
   3) The item after the next item in the collection being reduced over

   Doesn't use `reduce`... so not as fast."
  {:todo        ["Possibly find a better way to do it?"]
   :attribution 'alexandergunnarson}
  [func init coll]
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))

(defn multiplex
  ([completef rf0]
    (fn ([]      (rf0))
        ([x0]    (completef (rf0 x0)))
        ([x0 x'] (rf0 x0 x'))))
  ([completef rf0 rf1]
    (fn ([]           [(rf0) (rf1)])
        ([[x0 x1]]    (completef (rf0 x0) (rf1 x1)))
        ([[x0 x1] x'] [(rf0 x0 x') (rf1 x1 x')]))))

(defn incremental-apply
  "Applies ->`f` to reducible ->`xs`, incrementally, using `reduce`.

   Note that this is not the same as `apply`.
   To behave like `apply`, one would have to keep track of all
   values that come through the reduction function, which is equivalent
   to `(->> xs join! core/apply)`.
   It is also not the same as `reduce`, as `reduce` (without init) does
   not consider the first element of a reducible separately.

   `(apply             - [1])`     -> -1
   `(reduce            - [1])`     ->  1 ; ignores
   `(incremental-apply - [1])`     -> -1
   `(apply             - [1 2 3])` -> -4
   `(reduce            - [1 2 3])` -> -4 ; pairwise reduction
   `(incremental-apply - [1 2 3])` -> -6 ; considers first element separately

   And given `(defn counta [& args] (count args))`:

   `(apply             counta [1 2 3 4 5 6])` ->  6
   `(reduce            counta [1 2 3 4 5 6])` ->  2
   `(incremental-apply counta [1 2 3 4 5 6])` ->  2"
  [f xs]
  (let [ret (reduce
              (fn [ret x] (if (== ret sentinel) (f x) (f ret x)))
              sentinel
              xs)]
    (if (== ret sentinel) (f) ret)))

;; ===== LOOPS ===== ;;

#?(:clj
(defmacro for
  "See typed docs."
  [[x-sym coll ret-sym init] & body]
  `(reduce
     (fn ~[ret-sym x-sym] ~@body)
     ~init
     ~coll)))

#?(:clj
(defmacro fori
  "See typed docs."
  [[x-sym coll ret-sym init i-sym] & body]
  `(reducei
     (fn f# (~[ret-sym x-sym i-sym] ~@body))
     ~init
     ~coll)))
