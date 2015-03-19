(ns
  ^{:doc "Useful looping constructs. Most of these, like |doseq| and |for|,
          are faster than their lazy clojure.core counterparts."
    :attribution "Alex Gunnarson"}
  quantum.core.loops
  (:require
    [quantum.core.ns :as ns :refer
      #+clj [alias-ns defalias]
      #+cljs [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    #+clj [clojure.pprint  :as pprint :refer [pprint]]
    [quantum.core.data.map :as map    :refer [sorted-map+]]
    [quantum.core.function :as fn     :refer [fn-not]]
    [quantum.core.macros #+clj :refer #+clj [assert-args]]
    #+clj [proteus :refer [let-mutable]])
  #+cljs
  (:require-macros
    [quantum.core.macros :refer [assert-args]])
  #+clj
  (:import
    clojure.core.Vec
    (quantum.core.ns
      Nil Bool Num ExactNum Int Decimal Key Set
             ArrList TreeMap LSeq Regex Editable Transient Queue Map))
  #+clj (:gen-class))

#+clj (ns/require-all *ns* :clj)

(defmacro until [pred & body]
  `(while (not ~pred) ~@body))

(set! *warn-on-reflection* true)
(set! *unchecked-math*     true)

(defmacro reducei
  "|reduce|, indexed.

   This is a macro to eliminate the wrapper function call.
   Originally used a mutable counter on the inside just for fun...
   but the counter might be propagated, so it's best to use volatiles instead."
  {:attribution "Alex Gunnarson"}
  [f ret coll]
  `(let [n# (volatile! (long -1))] ; works because it's single-threaded
     (reduce
        (fn [ret-n# elem#]
          (vswap! n# unchecked-inc)
          (~f ret-n# elem# @n#))
        ~ret ~coll)))
 
(defn reduce-2
  "|reduce|s over 2 values in a collection with each pass.

   Doesn't use CollReduce... so not as fast as |reduce|."
  {:todo        ["Possibly find a better way to do it?"]
   :attribution "Alex Gunnarson"}
  [func init coll]
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))

(defn while-recur
  {:attribution "Alex Gunnarson"}
  [obj-0 pred func]
  (loop [obj obj-0]
      (if ((fn-not pred) obj)
          obj
          (recur (func obj)))))

(defmacro dos
  "Same as |(apply (memfn do) <args>)|."
  {:attribution "Alex Gunnarson"}
  [args]
  `(do ~@args))

(def ^:macro lfor #+clj #'clojure.core/for #+cljs #'cljs.core/for) ; "lazy-for"

(defmacro doseq
  "A lighter version of |doseq| based on |reduce|.
   Optimized for one destructured coll."
  {:attribution "Alex Gunnarson"}
  [[elem coll :as bindings] & body]
  (assert-args
    (vector? bindings)       "a vector for its binding"
    (even? (count bindings)) "an even number of forms in binding vector")
  `(reduce
     (fn [_# ~elem]
       ~@body
       nil)
     nil
     ~coll))

(defmacro doseqi
  "|doseq|, indexed. Starts index at 0."
  {:attribution "Alex Gunnarson"}
  ([[elem coll index-sym :as bindings] & body]
  (assert-args
    (vector? bindings)     "a vector for its binding"
    (= 3 (count bindings)) "three forms in binding vector")
  `(reducei
     (fn [_# ~elem ^long ~index-sym] ; this metadata is lost in macroexpansion, I think
       ~@body
       nil)
     nil ~coll)))

(defmacro for
  "A lighter, eager version of |for| based on |reduce|.
   Optimized for one destructured coll.
   Recognizes persistent vs. transient tradeoff."
  {:attribution "Alex Gunnarson"
   :performance "    2.043435 ms (for+ [elem v] nil))
                 vs. 2.508727 ms (doall (for [elem v] nil))

                 , where 'v' is a vectorized (range 1000000).

                 22.5% faster!"}
  [[elem coll :as bindings] & body]
  (assert-args
    (vector? bindings)       "a vector for its binding"
    (even? (count bindings)) "an even number of forms in binding vector")
  (if (should-transientize? coll)
      `(persistent!
         (reduce
           (fn [ret# ~elem]
             (conj! ret# (do ~@body)))
           (transient [])
           ~coll))
      `(reduce
         (fn [ret# ~elem]
           (conj ret# (do ~@body)))
         []
         ~coll)))

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



;(defmacro dotimes
;  "Hopefully an improvement on |dotimes|, via a few optimizations
;   like |(get _ 0)| instead of |first| and |let-mutable|."
;  {:performance
;    "For 1000000 loops: (dotimes [n 1000000] nil)
;
;     |clojure.core/dotimes| : 352 µs  — 374 µs  — 405 µs
;        
;     Using |while| as loop  : 4.2 ms  — 6.5 ms  — 11.8 ms
;     - Macroexpansion for |while| loop was likely non-trivial?
;
;     This version           : 4.269954 ms — 4.601226 ms — 5.965863 ms ... strange... 
;    "
;   :attribution "Alex Gunnarson"}
;  [bindings & body]
;  ; (assert-args
;  ;   (vector? bindings)       "a vector for its binding"
;  ;   (even? (count bindings)) "an even number of forms in binding vector")
;  (let [i (-> bindings (get 0))
;        n (-> bindings (get 1))]
;    `(let-mutable [n# (clojure.lang.RT/longCast ~n)
;                   ~i (clojure.lang.RT/longCast 0)]
;       (loop []      
;         (when (< ~i n#)
;           ~@body
;           (set! ~i (unchecked-inc ~i))
;           (recur))))))
;