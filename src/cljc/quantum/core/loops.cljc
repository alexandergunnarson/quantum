(ns
  ^{:doc "Useful looping constructs. Most of these, like |doseq| and |for|,
          are faster than their lazy clojure.core counterparts."
    :attribution "Alex Gunnarson"
    :cljs-self-referring? true}
  quantum.core.loops
  (:refer-clojure :exclude [doseq for reduce dotimes])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )           :as core   ]
             #?(:clj [proteus
                       :refer [let-mutable]                       ])
                     [quantum.core.core                :as qcore  ]
                     [quantum.core.error               :as err
                       :refer [->ex]                              ]
                     [quantum.core.log                 :as log
                       :include-macros true]
                     [quantum.core.macros.core         :as cmacros
                       :refer [#?@(:clj [if-cljs])]               ]
                     [quantum.core.macros              :as macros
                       :refer [#?@(:clj [assert-args])]           ]
                     [quantum.core.reducers.reduce     :as red    ]
                     [quantum.core.fn
                       :refer [#?@(:clj [rfn])]]
                     [quantum.core.macros.optimization :as opt    ]
                     [quantum.core.type                :as type   ])
  #?(:cljs (:require-macros
                     [quantum.core.loops
                       :refer [doseq reducei]                     ])))

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

#?(:clj
(defmacro reduce-
  ([lang f coll]
   `(reduce- ~lang ~f (~f) ~coll))
  ([lang f ret coll]
   (let [externed
          (condp = lang
            :clj  (if @qcore/externs?
                      (try (opt/extern- f)
                        (catch Throwable _
                          (log/pr ::macro-expand "COULD NOT EXTERN" f)
                          f))
                      f)
            :cljs f)
         code `(red/reduce ~externed ~ret ~coll)]
     code))))

#?(:clj
(defmacro reduce [& args]
  `(reduce- ~(if-cljs &env :cljs :clj) ~@args)))

; TODO THIS IS THE ORIGINAL AND BETTER
#?(:clj
(defmacro reducei-
  [should-extern? f ret-i coll & args]
  (let [f-final
         `(~(if (and should-extern? @qcore/externs?)
                `quantum.core.macros/extern+
                `quantum.core.macros.optimization/identity*)
           (let [i# (volatile! (long -1))]
             (fn ([ret# elem#]
                   (vswap! i# qcore/unchecked-inc-long)
                   (opt/inline-replace (~f ret# elem# @i#)))
                 ([ret# k# v#]
                   (vswap! i# qcore/unchecked-inc-long)
                   (opt/inline-replace (~f ret# k# v# @i#))))))
        _ (log/ppr ::macro-expand "F FINAL EXTERNED" f-final)
        code `(red/reduce ~f-final ~ret-i ~coll)
        _ (log/ppr ::macro-expand "REDUCEI CODE" code)]
 code)))

#?(:clj
(defmacro reducei
  "|reduce|, indexed.

   This is a macro to eliminate the wrapper function call.
   Originally used a mutable counter on the inside just for fun...
   but the counter might be propagated via @f, so it's best to use
   an atom instead."
  {:attribution "Alex Gunnarson"
   :todo ["Make this an inline function, not a macro."]}
  [f ret coll]
  `(reducei- false ~f ~ret ~coll)))

(defn reduce-2
  "Like |reduce|, but reduces over two items in a collection at a time.

   Its function @func must take three arguments:
   1) The accumulated return value of the reduction function
   2) The                next item in the collection being reduced over
   3) The item after the next item in the collection being reduced over

   Doesn't use CollReduce... so not as fast as |reduce|."
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
  {:equivalent {`(red-for [ret {}
                           m   [{1 2} {3 4}]]
                   (merge-with + ret m))
                `(reduce
                   (rfn [ret m] (merge-with + ret m))
                   {}
                   [{1 2} {3 4}])}}
  [[ret-sym init x-sym coll] & body]
  `(reduce
     (rfn ~[ret-sym x-sym] ~@body)
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
  {:attribution "Alex Gunnarson"}
  [should-extern? bindings & body]
  (assert-args
    (vector? bindings)       "a vector for its binding")
  (condp = (count bindings)
    3
      (let [[k v coll] bindings]
        `(reduce
           (fn [_# ~k ~v]
                 ~@body
                 nil)
           nil
           ~coll))
    2
      (let [[elem coll] bindings]
        `(reduce
           (fn [_# ~elem]
                 ~@body
                 nil)
           nil
           ~coll))
    (throw (->ex nil (str "|doseq| takes either 2 or 3 args in bindings. Received " (count bindings)))))))

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



#?(:clj
(defmacro dotimes
  "Hopefully an improvement on |dotimes|, via a few optimizations
   like |(get _ 0)| instead of |first| and |let-mutable|."
  {:performance
    "For 1000000 loops: (dotimes [n 1000000] nil)

     |clojure.core/dotimes| : 352 µs  — 374 µs  — 405 µs

     Using |while| as loop  : 4.2 ms  — 6.5 ms  — 11.8 ms
     - Macroexpansion for |while| loop was likely non-trivial?

     This version           : 4.269954 ms — 4.601226 ms — 5.965863 ms ... strange...
    "
   :attribution "Alex Gunnarson"}
  [bindings & body]
  ; (assert-args
  ;   (vector? bindings)       "a vector for its binding"
  ;   (even? (count bindings)) "an even number of forms in binding vector")
  (let [i (-> bindings (get 0))
        n (-> bindings (get 1))]
    `(let-mutable [n# (clojure.lang.RT/longCast ~n)
                   ~i (clojure.lang.RT/longCast 0)]
       (loop []
         (when (< ~i n#)
           ~@body
           (set! ~i (unchecked-inc ~i))
           (recur)))))))



; (defmacro doseq+
;   "Repeatedly executes body (presumably for side-effects) with
;   bindings and filtering as provided by \"for\".  Does not retain
;   the head of the sequence. Returns nil.

;   Timothy Baldridge: 'The net effect is about a 3x performance boost for vectors, with no impact on code for seqs.'"
;   {:attribution "Timothy Baldridge, http://dev.clojure.org/jira/browse/CLJ-1658"}
;   [seq-exprs & body]
;   (assert-args
;     (vector? seq-exprs)       "a vector for its binding"
;     (even? (count seq-exprs)) "an even number of forms in binding vector")
;   (let [step (fn step [[k v & seqs] body]
;                (let [body (if seqs
;                               (step seqs body)
;                               body)]
;                  (if k
;                      (if (keyword? k)
;                          (cond
;                            (= k :let)   `(let ~v ~body)
;                            (= k :when)  `(when ~v ~body)
;                            (= k :while) `(if ~v ~body (reduced nil)))
;                          `(reduce
;                             (fn [_# ~k]
;                               ~body)
;                             nil
;                             ~v))
;                      body)))]
;     `(do ~(step seq-exprs `(do ~@body nil)) nil)))

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
  [f coll]
  (doseq [x coll] (f x))
  coll)

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
