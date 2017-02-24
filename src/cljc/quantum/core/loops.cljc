(ns
  ^{:doc "Useful looping constructs. Most of these, like |doseq| and |for|,
          are faster than their lazy clojure.core counterparts."
    :attribution "Alex Gunnarson"}
  quantum.core.loops
  (:refer-clojure :exclude [doseq for reduce dotimes])
  (:require
    [clojure.core                     :as core]
#?@(:clj
   [[proteus
      :refer [let-mutable]]])
    [quantum.core.core                :as qcore]
    [quantum.core.error               :as err
      :refer [->ex]]
    [quantum.core.log                 :as log]
    [quantum.core.macros              :as macros
      :refer [assert-args]]
    [quantum.core.reducers.reduce     :as red]
    [quantum.core.fn
      :refer [rfn]]
    [quantum.core.macros.optimization :as opt]
    [quantum.core.type                :as type]
    [quantum.core.vars                :as var
      :refer [defalias]])
  (:require-macros
    [quantum.core.loops               :as self
      :refer [reduce reducei doseq]]))

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

#?(:clj (defalias reduce red/reduce))

#?(:clj
(defmacro reducei
   "`reduce`, indexed.

   This is a macro to eliminate the wrapper function call.
   Originally used a mutable counter on the inside just for fun...
   but the counter might be propagated via @f, so it's best to use
   an atomic value instead."
  {:attribution "Alex Gunnarson"
   :todo ["Make this an inline function, not a macro."]}
  [f ret-i coll & args]
  (let [f-final
         `(let [i# (volatile! (long -1))]
            (fn ([ret# elem#]
                  (vswap! i# quantum.core.core/unchecked-inc-long)
                  (~f ret# elem# @i#))
                ([ret# k# v#]
                  (vswap! i# quantum.core.core/unchecked-inc-long)
                  (~f ret# k# v# @i#))))
        code `(reduce ~f-final ~ret-i ~coll)]
    code)))

(defn reduce-2
  "Like |reduce|, but reduces over two items in a collection at a time.

   Its function @func must take three arguments:
   1) The accumulated return value of the reduction function
   2) The                next item in the collection being reduced over
   3) The item after the next item in the collection being reduced over

   Doesn't use `reduce`... so not as fast."
  {:todo        ["Possibly find a better way to do it?"]
   :attribution "Alex Gunnarson"}
  [func init coll]
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))

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
  {:attribution "Alex Gunnarson"}
  [obj-0 pred func]
  (loop [obj obj-0]
      (if (not (pred obj))
          obj
          (recur (func obj)))))

#?(:clj
(defmacro dos
  "Same as |(apply (memfn do) <args>)|."
  {:attribution "Alex Gunnarson"}
  [args]
  `(do ~@args)))

#?(:clj
(defmacro doseq*
  "A lighter version of |doseq| based on |reduce|.
   Optimized for one destructured coll."
  {:attribution "Alex Gunnarson"
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
    (throw (->ex (str "|doseq| takes either 2 or 3 args in bindings. Received " (count bindings)))))))

#?(:clj
(defmacro doseq-
  [bindings & body]
  `(doseq* false ~bindings ~@body)))

#?(:clj
(defmacro doseq
  [bindings & body]
  `(doseq* false ~bindings ~@body)))

#?(:clj
(defmacro doseqi*
  "|doseq|, indexed. Starts index at 0."
  {:attribution "Alex Gunnarson"}
  [should-extern? [elem coll index-sym :as bindings] & body]
  (assert-args
    (vector? bindings)     "a vector for its binding"
    (= 3 (count bindings)) "three forms in binding vector")
  (let [code `(let [i# (volatile! 0)]
                (reduce
                  (fn [_# ~elem]
                    (let [~index-sym @i#] ~@body)
                    (vswap! i# qcore/unchecked-inc-long)
                    nil)
                  nil ~coll))
        _ (log/ppr ::macro-expand "DOSEQI CODE IS" code)]
    code)))

#?(:clj
(defmacro doseqi-
  [bindings & body]
  `(doseqi* false ~bindings ~@body)))

#?(:clj
(defmacro doseqi
  [bindings & body]
  `(doseqi* true ~bindings ~@body)))

#?(:clj
(defmacro for*
  "A lighter, eager version of |for| based on |reduce|.
   Also accepts a collection into which to aggregate the results
   (i.e. doesn't default to a lazy seq or a vector, etc.)"
  {:attribution "Alex Gunnarson"
   :performance "    2.043435 ms (for+ [elem v] nil))
                 vs. 2.508727 ms (doall (for [elem v] nil))

                 , where 'v' is a vectorized (range 1000000).

                 22.5% faster!"
   :todo ["Could be as simple as (->> coll (map+ f) (into ret))"]}
  [ret bindings & body]
  (assert-args
    (vector? bindings) "a vector for its binding")
  `(let [coll# ~(last bindings)
         [pre# conj# post#] (type/recommended-transient-fns coll#)]
     (post#
       (reduce
         (fn [ret# ~@(butlast bindings)]
           (conj# ret# (do ~@body)))
         (pre# ~ret)
         coll#)))))

#?(:clj
(defmacro for
  "A lighter, eager version of |for| based on |reduce|.
   Optimized for one coll."
  {:attribution "Alex Gunnarson"
   :performance "    2.043435 ms (for+ [elem v] nil))
                 vs. 2.508727 ms (doall (for [elem v] nil))

                 , where 'v' is a vectorized (range 1000000).

                 22.5% faster!"}
  [bindings & body]
  `(for* [] ~bindings ~@body)))


#?(:clj
(defmacro fori*
  {:attribution "Alex Gunnarson"}
  [ret bindings & body]
  (let [n-sym (last bindings)]
  `(let [n# (volatile! 0)]
     (for* ~ret ~(vec (butlast bindings))
       (let [~n-sym (deref n#)
             res# (do ~@body)]
         (vswap! n# qcore/unchecked-inc-long)
         res#))))))

#?(:clj
(defmacro fori
  "fori:for::reducei:reduce"
  {:attribution "Alex Gunnarson"}
  [bindings & body]
  `(fori* [] ~bindings ~@body)))

#?(:clj
(defmacro seq-loop
  [bindings & exprs]
  (condp = (count bindings)
    4 (let [[elem-sym coll
             ret-sym init] bindings]
       `(reduce
          (fn [~ret-sym ~elem-sym]
            ~@exprs)
          ~init
          ~coll))
    5 (let [[k-sym v-sym coll
             ret-sym init] bindings]
       `(reduce
          (fn [~ret-sym ~k-sym ~v-sym]
            ~@exprs)
          ~init
          ~coll)))))

#?(:clj
(defmacro ifor
  "Imperative |for| loop."
  {:usage '(ifor [n 0 (< n 100) (inc n)] (println n))}
  [[sym val-0 pred val-n+1] & body]
  `(loop [~sym ~val-0]
    (when ~pred ~@body (recur ~val-n+1)))))

#?(:clj (defmacro lfor [& args] `(core/for ~@args)))

; 2.284878 ms... strangely not faster than transient |for|
; (defmacro for-internally-mutable
;   "A lighter, eager version of |for| based on |reduce|.
;    Optimized for one destructured coll.
;    Recognizes persistent vs. transient tradeoff."
;   {:attribution "Alex Gunnarson"}
;   [[elem coll] & body]
;   `(let-mutable [ret# (transient [])]
;      (reduce ; would normally replace this with this namespace's |doseq|, but needed the |^:local| hint to be present
;        ^:local
;        (fn [_# ~elem]
;          (set! ret# (conj! ret# (do ~@body)))
;          nil)
;        nil ~coll)
;      (persistent! ret#)))

#?(:clj (defalias dotimes core/dotimes))

#?(:clj
(defmacro fortimes
  "`dotimes` meets `for`"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [v# (transient [])]
     (dotimes [~i ~n] (conj! v# (do ~@body)))
     (persistent! v#))))

#?(:clj
(defmacro fortimes:objects
  "`dotimes` meets `for`, efficiently wrapped into an object array"
  [[i n & xs'] & body]
  (assert (empty? xs'))
  `(let [n# ~n v# (object-array ~n)]
     (dotimes [~i n#] (aset v# ~i (do ~@body)))
     v#)))

#?(:clj
(defmacro while-let
  "Repeatedly executes body while test expression is true, evaluating the body
   with binding-form bound to the value of test."
  {:source "markmandel/while-let"}
  [[form test] & body]
  `(loop [~form ~test]
     (when ~form
         ~@body
         (recur ~test)))))

(defn doeach
  "Like |run!|, but returns @coll.
   Like an in-place |doseq|."
  [f coll] (doseq [x coll] (f x)) coll)

(defn each
  "Same as |core/run!| but uses reducers' reduce"
  [f coll]
  (red/reduce #(f %2) nil coll)
  nil)

(defn eachi
  "eachi : each :: fori : for"
  [f coll]
  (reducei (fn [_ x i] (f x i)) nil coll)
  nil)

#?(:clj (set! *unchecked-math* false))
