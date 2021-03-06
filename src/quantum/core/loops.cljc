(ns
  ^{:doc "Useful looping constructs. Most of these, like |doseq| and |for|,
          are faster than their lazy clojure.core counterparts."
    :attribution "alexandergunnarson"}
  quantum.core.loops
  (:refer-clojure :exclude
    [doseq for reduce dotimes, deref reset!])
  (:require
    [clojure.core                     :as core]
#?@(:clj
   [[proteus
      :refer [let-mutable]]])
    [quantum.core.core                :as qcore]
    [quantum.core.collections.core    :as c]
    [quantum.core.error               :as err
      :refer [>ex-info]]
    [quantum.core.fn                  :as fn
      :refer [rfn <- call fn' rcomp firsta seconda]]
    [quantum.core.log                 :as log]
    [quantum.core.macros              :as macros
      :refer [assert-args defnt #?(:clj defnt')]]
    [quantum.core.macros.optimization :as opt]
    [quantum.core.reducers.reduce     :as red]
    [quantum.core.reducers
      :refer [map+]]
    [quantum.core.refs                :as refs
      :refer [deref reset! !long]]
    [quantum.core.type                :as type]
    [quantum.untyped.core.reducers    :as ured]
    [quantum.core.vars                :as var
      :refer [defalias]])
#?(:cljs
  (:require-macros
    [quantum.core.loops               :as self
      :refer [reduce reducei doseq]]))
  #?(:clj (:import [quantum.core.data Array])))

(log/this-ns)

#?(:clj (set! *unchecked-math* true))

#?(:clj
(defmacro until [pred-expr & body]
 `(while (not ~pred-expr) ~@body)))

#_(:clj
(defmacro reduce-
  ([f coll]
   `(red/reduce ~f ~coll))
  ([f ret coll]
   `(red/reduce ~f ~ret ~coll))))

#?(:clj (defalias reduce  red/reduce ))
#?(:clj (defalias reducei red/reducei))

(defalias reduce-pair ured/reduce-pair)

#?(:clj
(defmacro reduce*
  "Like `reduce`, but with the syntax of `for*`."
  {:equivalent {`(->> coll
                      (reduce* {} [ret m]
                        (merge-with + ret m)))
                `(reduce
                   (rfn [ret m] (merge-with + ret m))
                   {}
                   coll)}}
  [init & body]
  `(reduce
     (rfn ~@(butlast body))
     ~init
     ~(last body))))

#?(:clj
(defmacro red-for
  "Like `reduce`, but with a similar syntax to `for`."
  {:equivalent {`(red-for [m   [{1 2} {3 4}]
                           ret {}]
                   (merge-with + ret m))
                `(reduce
                   (rfn [ret m] (merge-with + ret m))
                   {}
                   [{1 2} {3 4}])}}
  [[x-sym coll ret-sym init] & body]
  `(reduce
     (rfn ~[ret-sym x-sym] ~@body)
     ~init
     ~coll)))

#?(:clj
(defmacro red-fori
  "Like `reducei`, but with a similar syntax to `for`."
  {:equivalent {`(red-fori [m   [{1 2} {3 4}]
                            ret {} i]
                   (merge-with + ret m))
                `(reducei
                   (rfn [ret m i] (merge-with + ret m))
                   {}
                   [{1 2} {3 4}])}}
  [[x-sym coll ret-sym init i-sym] & body]
  `(reducei
     (fn f# ( [ret# k# v# i#] (f# ret# [k# v#] i#))
            (~[ret-sym x-sym i-sym] ~@body))
     ~init
     ~coll)))

(defn while-recur
  {:attribution "alexandergunnarson"}
  [obj-0 pred func]
  (loop [obj obj-0]
      (if (not (pred obj))
          obj
          (recur (func obj)))))

#?(:clj
(defmacro dos
  "Same as |(apply (memfn do) <args>)|."
  [args] `(do ~@args)))

#?(:clj
(defmacro doseq*
  "A lighter version of |doseq| based on |reduce|.
   Optimized for one destructured coll."
  {:attribution "alexandergunnarson"
   :see-also "Timothy Baldridge, http://dev.clojure.org/jira/browse/CLJ-1658"}
  [should-extern? bindings & body]
  (assert (vector? bindings) "`doseq` takes a vector for its bindings")
  (condp = (count bindings)
    3
      (let [[k v coll] bindings]
        `(reduce
           (rfn [_# ~k ~v]
                 ~@body
                 nil)
           nil
           ~coll))
    2
      (let [[elem coll] bindings]
        `(reduce
           (rfn [_# ~elem]
                 ~@body
                 nil)
           nil
           ~coll))
    (throw (>ex-info (str "|doseq| takes either 2 or 3 args in bindings. Received " (count bindings)))))))

#?(:clj
(defmacro doseq-
  [bindings & body]
  `(doseq* false ~bindings ~@body)))

#?(:clj
(defmacro doseq
  [bindings & body]
  `(doseq* false ~bindings ~@body)))

#?(:clj
(defmacro doseqi
  "`doseq`, indexed thread-unsafely. Starts index at 0."
  {:attribution "alexandergunnarson"}
  [[elem xs index-sym :as bindings] & body]
  `(let [*i# (!long -1) xs# ~xs]
     (reduce
       (fn [_# ~elem]
         (let [~index-sym (reset! *i# (inc (deref *i#)))] ~@body))
       nil
       xs#)
     xs#)))

(defn for-join*
  [joinf bindings & body]
  (assert-args
    (vector? bindings) "a vector for its bindings")
  `(->> ~(last bindings)
        (<- (red/transformer (core/map (rfn [~@(butlast bindings)] ~@body)))) ; bootstrapping `map+`
        ~joinf))

#?(:clj
(defmacro for-join
  "A lighter, eager version of `core/for` based on `reduce`.
   Also accepts a collection into which to aggregate the results
   (i.e. doesn't default to a lazy seq or a vector, etc.)"
  {:attribution "alexandergunnarson"}
  [ret bindings & body]
  (apply for-join* (list 'quantum.core.collections.core/join ret) bindings body)))

#?(:clj
(defmacro for-join!
  {:attribution "alexandergunnarson"}
  [ret bindings & body]
  (apply for-join* (list 'quantum.core.collections.core/join! ret) bindings body)))

#?(:clj
(defmacro for
  "A lighter, eager version of |for| based on |reduce|.
   Optimized for one coll."
  {:attribution "alexandergunnarson"
   :performance "    2.043435 ms (for+ [elem v] nil))
                 vs. 2.508727 ms (doall (for [elem v] nil))

                 , where 'v' is a vectorized (range 1000000).

                 22.5% faster!"}
  [bindings & body]
  `(for-join [] ~bindings ~@body)))

#?(:clj
(defmacro for'
  "`for` : `for'` :: `join` : `join'`."
  [bindings & body]
  (apply for-join* (list 'quantum.core.collections.core/join') bindings body)))

(defn fori-join*
  {:attribution "alexandergunnarson"}
  [joinf bindings & body]
  (let [n-sym  (last bindings)
        *n-sym (gensym "mutable-n")]
   `(let [~*n-sym (!long -1)]
     ~(for-join* joinf (vec (butlast bindings))
       `(let [~n-sym (reset! ~*n-sym (inc (deref ~*n-sym)))
              res#   (do ~@body)]
          res#)))))

#?(:clj
(defmacro fori-join [ret bindings & body]
  (apply fori-join* (list 'quantum.core.collections.core/join ret) bindings body)))

#?(:clj
(defmacro fori-join! [ret bindings & body]
  (apply fori-join* (list 'quantum.core.collections.core/join! ret) bindings body)))

#?(:clj
(defmacro fori
  "fori : for :: reducei : reduce"
  {:attribution "alexandergunnarson"}
  [bindings & body]
  `(fori-join [] ~bindings ~@body)))

#?(:clj
(defmacro fori'
  "`fori'` : `for'` :: `fori` : `for`."
  [bindings & body]
  (apply fori-join* (list 'quantum.core.collections.core/join') bindings body)))

#?(:clj
(defmacro ifor
  "Imperative `for` loop."
  {:usage '(ifor [n 0 (< n 100) (inc n)] (println n))}
  [[sym val-0 pred val-n+1] & body]
  `(loop [~sym ~val-0 ret# nil]
    (if ~pred
        (recur ~val-n+1 (do ~@body))
        ret#))))

#?(:clj
(defmacro ifori
  "Imperative `for` loop, indexed."
  {:usage '(ifor [i n 0 (< n 100) (inc n)] (println i n))}
  [[i-sym sym val-0 pred val-n+1] & body]
  `(loop [~i-sym 0 ~sym ~val-0 ret# nil]
    (if ~pred
        (recur (unchecked-inc ~i-sym) ~val-n+1 (do ~@body))
        ret#))))

#?(:clj (defmacro lfor [& args] `(core/for ~@args)))

; 2.284878 ms... strangely not faster than transient |for|
; (defmacro for-internally-mutable
;   "A lighter, eager version of |for| based on |reduce|.
;    Optimized for one destructured coll.
;    Recognizes persistent vs. transient tradeoff."
;   {:attribution "alexandergunnarson"}
;   [[elem coll] & body]
;   `(let-mutable [ret# (transient [])]
;      (reduce ; would normally replace this with this namespace's |doseq|, but needed the |^:local| hint to be present
;        ^:local
;        (fn [_# ~elem]
;          (set! ret# (conj! ret# (do ~@body)))
;          nil)
;        nil ~coll)
;      (persistent! ret#)))

#?(:clj
(defmacro dotimes-1
  "Like `dotimes`, but returns the last thing done in the loop instead of always nil."
  [bindings & body]
  (let [i (first bindings)
        n (second bindings)]
    `(let [n# (long ~n)]
       (when (> n# 0)
         (loop [~i 0]
           (let [ret# (do ~@body)]
             (if (= ~i (unchecked-dec n#))
                 ret#
                 (recur (unchecked-inc ~i))))))))))

#?(:clj (defmacro dotimes
  "Like `dotimes`, but enables multiple bindings like `for` for
   Cartesian-product effect."
  [bindings & body]
  (assert (vector? bindings))
  (assert (-> bindings count even?))
  (let [bindings' (->> bindings (partition-all 2) reverse)]
    (reduce
      (fn [code binding]
        `(dotimes-1 ~(vec binding) ~code))
      `(dotimes-1 ~(vec (first bindings')) ~@body)
      (rest bindings')))))

#?(:clj
(defmacro fortimes ; TODO replace the body of this with `reduce` once it becomes as efficient as `dotimes`
  "`dotimes` meets `for`"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [v# (transient [])]
     (persistent! (dotimes [~i ~n] (conj! v# (do ~@body)))))))

; TODO deduplicate all the `fortimes` array code
#?(:clj
(defmacro fortimes:objects
  "`dotimes` meets `for`, efficiently wrapped into an object array"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [n# ~n v# (Array/newUninitialized1dObjectArray n#)]
     (dotimes [~i n#] (c/assoc-in!*& v# (do ~@body) ~i)))))

#?(:clj
(defmacro fortimes:objects2
  "`dotimes` meets `for`, efficiently wrapped into an object[][] array"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [n# ~n v# (Array/newUninitialized2dObjectArray n#)]
     (dotimes [~i n#] (c/assoc-in!*& v# (do ~@body) ~i)))))

#?(:clj
(defmacro fortimes:doubles
  "`dotimes` meets `for`, efficiently wrapped into an double array"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [n# ~n v# (Array/newUninitialized1dDoubleArray n#)]
     (dotimes [~i n#] (c/assoc-in!*& v# (do ~@body) ~i)))))

#?(:clj
(defmacro fortimes:doubles2
  "`dotimes` meets `for`, efficiently wrapped into an double[][] array"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [n# ~n v# (Array/newUninitialized2dDoubleArray n#)]
     (dotimes [~i n#] (c/assoc-in!*& v# (do ~@body) ~i)))))

#?(:clj
(defmacro fortimes:doubles3
  "`dotimes` meets `for`, efficiently wrapped into an double[][][] array"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [n# ~n v# (Array/newUninitialized2dDoubleArray n#)]
     (dotimes [~i n#] (c/assoc-in!*& v# (do ~@body) ~i)))))

#?(:clj
(defmacro while-let
  "Like `while`, but binds the test value as by `let`."
  [[bind testv] & body]
  `(loop [sym# ~testv]
     (let [~bind sym#]
       (when sym# ~@body (recur ~testv))))))

#?(:clj
(defmacro while-let-pred
  "Like `while`, but binds the test value as by `let`.
   Only executes if `pred` is true on the test value."
  [[bind testv pred] & body]
  `(loop [sym# ~testv]
     (let [~bind sym#]
       (when (~pred sym#) ~@body (recur ~testv))))))

#?(:clj
(defmacro doreduce ; TODO demacro when type inference is done
  "Performs a reduction for purposes of side effects.
   Returns the last value passed into the reducing function."
  [xs] `(reduce (fn/rf-fix seconda) nil ~xs)))

#?(:clj
(defmacro doeach ; TODO demacro when type inference is done
  "Like `run!`, but returns ->`xs`.
   Like an in-place `doseq`."
  [f xs] `(let [f# ~f xs# ~xs] (doseq [x# xs#] (f# x#)) xs#)))

#?(:clj
(defmacro each ; TODO demacro when type inference is done
  "Same as |core/run!| but uses reducers' reduce"
  [f xs] `(do (reduce (fn/rf-fix (rcomp seconda ~f)) nil ~xs) nil)))

#?(:clj
(defmacro eachi  ; TODO demacro when type inference is done
  "eachi : each :: fori : for"
  [f xs] `(let [f# ~f] (reducei (fn [_# x# i#] (f# x# i#)) nil ~xs) nil)))

#?(:clj (set! *unchecked-math* false))

(defn reduce-2
  "Reduces over two seqables at a time."
  {:todo #{"`defnt` this and have it dispatch to e.g. reduce-2:indexed"}}
  ([f xs0 xs1] (reduce-2 f nil xs0 xs1))
  ([f init xs0 xs1] (reduce-2 f init xs0 xs1 false))
  ([f init xs0 xs1 assert-same-count?]
    (loop [ret init xs0' xs0 xs1' xs1]
      (cond (reduced? ret)
            @ret
            (or (empty? xs0') (empty? xs1'))
            (do (when (and assert-same-count?
                           (or (and (empty? xs0') (seq    xs1'))
                               (and (seq    xs0') (empty? xs1'))))
                  (throw (>ex-info "Seqables are not the same count")))
                ret)
            :else (recur (f ret (first xs0') (first xs1'))
                         (next xs0')
                         (next xs1'))))))

(defn reducei-2
  "`reduce-2` + `reducei`"
  {:todo #{"`defnt` this and have it dispatch to e.g. reducei-2:indexed"}}
  ([f xs0 xs1] (reduce-2 f nil xs0 xs1))
  ([f init xs0 xs1] (reduce-2 f init xs0 xs1 false))
  ([f init xs0 xs1 assert-same-count?]
    (loop [ret init xs0' xs0 xs1' xs1 i 0]
      (cond (reduced? ret)
            @ret
            (or (empty? xs0') (empty? xs1'))
            (do (when (and assert-same-count?
                           (or (and (empty? xs0') (seq    xs1'))
                               (and (seq    xs0') (empty? xs1'))))
                  (throw (>ex-info "Seqables are not the same count")))
                ret)
            :else (recur (f ret (first xs0') (first xs1') i)
                         (next xs0')
                         (next xs1')
                         (unchecked-inc i))))))

#?(:clj
(defnt' reduce-2:indexed
  "Reduces over two indexed collections at a time."
  {:todo #{"Merge with `reduce-2`; collections"
           "Lazily compile this:
            8 primitive types + 1 Object type = 9
            9 * 10 array depths = 90
            90 + ~8 other indexed types = 98
            98*98 = 9,604 possibilities
            We don't want to compute this (the Cartesian product of all indexed types) up front!"}}
  [^fn? f init #_indexed? #{array-1d? +vector?} xs0 #_indexed? #{array-1d? +vector?} xs1]
  (let [ct0 (c/count xs0) ct1 (c/count xs1)]
    (loop [ret init i 0]
      (cond (reduced? ret)
            @ret
            (or (>= i ct0) (>= i ct1))
            ret
            :else (recur (f ret (c/get xs0 i) (c/get xs1 i))
                         (unchecked-inc i)))))))

(defnt reduce-multi:objects
  "Reduces, mapping multiple reducing functions to multiple return values
   of `reduce`.
   Returns the aggregation of values as an object array."
  [#{+vector? objects?} fs xs] ; TODO fs can be any 1D-indexed, and xs must be reducible ; TODO infer this
  (reduce
    (fn [^objects rets x]
      (doseqi [f fs i] (c/assoc! rets i (f (c/get rets i) x)))
      rets)
    (->> fs (map+ call) (c/join! (c/->objects-nd (c/count fs)))) ; TODO inefficient if `(count fs)` isn't known
    xs))

(defnt reduce-multi
  "Reduces, mapping multiple reducing functions to multiple return values
   of `reduce`."
  {:usage `{(reduce-multi [+ *] (range 1 5))
            [[10 24]]}}
  [#{+vector? objects?} fs xs] ; TODO fs can be any 1D-indexed, and xs must be reducible ; TODO infer this
  (vec (reduce-multi:objects fs xs))) ; TODO is this faster or is transient `assoc!` better?

(defn mapfn
  ([fs] (fn [xs] (mapfn xs fs)))
  ([xs fs]
    (fori [x xs i]
      ((or (get fs i) identity) x))))
