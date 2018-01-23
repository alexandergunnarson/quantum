(ns quantum.untyped.core.reducers
  (:refer-clojure :exclude [apply every? vec == for seqable?])
  (:require
    [clojure.core                 :as core]
    [clojure.core.reducers        :as r]
    [fast-zip.core                :as zip]
    [quantum.untyped.core.compare :as comp
      :refer [== not==]]
    [quantum.untyped.core.core
      :refer [->sentinel seqable?]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env]]
    [quantum.untyped.core.qualify :as qual]
    [quantum.untyped.core.vars    :as uvar
      :refer [defalias]]))

(defonce sentinel (->sentinel))

;; ===== Transformer and transducer conversion ===== ;;

;; TODO `xs` will hold on to heads of seqs while stepping through; see also http://dev.clojure.org/jira/browse/CLJ-1793
;; A cross between a `reducer` and a `folder`
(deftype Transformer [xs prev xf]
  #?(:clj clojure.lang.IReduce :cljs cljs.core/IReduce)
    (#?(:clj reduce :cljs -reduce) [this f]
      (let [rf (xf f)]
        (rf (core/reduce rf (rf) prev))))
    (#?(:clj reduce :cljs -reduce) [this f init]
      (let [rf (xf f)]
        (rf (core/reduce rf init prev)))))

(defn transformer
  "Given a reducible collection, and a transformation function transform,
  returns a reducible collection, where any supplied reducing
  fn will be transformed by transform. transform is a function of reducing fn to
  reducing fn."
  ([xs xf]
    (if (instance? Transformer xs)
        (Transformer. (.-xs ^Transformer xs) xs xf)
        (Transformer. xs                     xs xf))))

(defn transformer? [x] (instance? Transformer x))

(defn transducer>
  {:todo #{"More arity"}}
  ([^long n xf tf]
    (case n
          0 (fn ([]            (xf))
                ([xs]          (tf xs (xf))))
          1 (fn ([a0]          (xf a0))
                ([a0 xs]       (tf xs (xf a0))))
          2 (fn ([a0 a1]       (xf a0 a1))
                ([a0 a1 xs]    (tf xs (xf a0 a1))))
          3 (fn ([a0 a1 a2]    (xf a0 a1 a2))
                ([a0 a1 a2 xs] (tf xs (xf a0 a1 a2))))
          (throw (ex-info "Unhandled arity for transducer" nil)))))

(defn transducer->transformer
  "Converts a transducer into a transformer."
  [^long n xf] (transducer> n xf transformer))

(defn transducer->reducer
  "Converts a transducer into a reducer."
  [^long n xf] (transducer> n xf r/reducer))

;; ===== Reduction functions ===== ;;

(def ^{:doc "A marriage of `transduce` and `reduce`.
             Like `reduce`, does not have a notion of a transforming function
             (unlike `transduce`). Like `transduce`, uses the seed (0-arity) and
             completing (1-arity) arities of the reducing function when performing
             a reduction (unlike `reduce`)."}
  educe (partial transduce identity))

(defn join
  ([from] (if (vector? from) from (join [] from)))
  ([to from] (core/into to from)))

(defn join'
  "Like `joinl`, but reduces into an empty version of the collection passed."
  [xs]
  (cond
    (transformer? xs)
      (join (empty (.-xs ^Transformer xs)) xs)
    (seq? (empty xs)) ; `conj`es on left, not right
      (core/into (empty xs) (reverse xs))
    :else
      (join (empty xs) xs)))

;; for purposes of `defeager`
(declare pjoin pjoin')

#?(:clj
(defmacro defeager [sym plus-sym]
  (let [lazy-sym            (when (resolve (symbol "clojure.core" (name sym)))
                              (symbol (str "l" sym)))
        quoted-sym          (symbol (str sym "'"))
        parallel-sym        (symbol (str "p" sym))
        parallel-quoted-sym (symbol (str "p" sym "'"))]
    `(do ~(when lazy-sym
           `(defalias ~lazy-sym ~(symbol (case-env :cljs "cljs.core" "clojure.core") (name sym))))
         (defalias ~(qual/unqualify plus-sym) ~plus-sym)
         (defn ~sym
           ~(str "Like `core/" sym "`, but eager. Reduces into vector.")
           ([f#] (fn [coll#] (~sym f# coll#)))
           ([f# coll#] (->> coll# (~plus-sym f#) join)))
         (defn ~quoted-sym
           ~(str "Like `" sym "`, but reduces into the empty version of the collection which was passed to it.")
           ([f#] (fn [coll#] (~quoted-sym f# coll#)))
           ([f# coll#] (->> coll# (~plus-sym f#) join')))
         (defn ~parallel-sym
           ~(str "Like `core/" sym "`, but eager and parallelized. Folds into vector.")
           ([f#] (fn [coll#] (~parallel-sym f# coll#)))
           ([f# coll#] (->> coll# (~plus-sym f#) pjoin)))
         (defn ~parallel-quoted-sym
           ~(str "Like `" sym "`, but parallel-folds into the empty version of the collection which was passed to it.")
           ([f#] (fn [coll#] (~parallel-quoted-sym f# coll#)))
           ([f# coll#] (->> coll# (~plus-sym f#) pjoin')))))))


(defn into! [xs0 xs1] (reduce (fn [xs0' x] (conj! xs0' x)) xs0 xs1))

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

;; ===== Loops ===== ;;

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
