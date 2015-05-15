#?(:clj 
(ns quantum.core.loops
  (:refer-clojure :exclude [doseq for])))

(ns
  ^{:doc "Useful looping constructs. Most of these, like |doseq| and |for|,
          are faster than their lazy clojure.core counterparts."
    :attribution "Alex Gunnarson"}
  quantum.core.loops
  (:refer-clojure :exclude [doseq for])
  (:require
    [quantum.core.ns :as ns :refer
      #?(:clj  [alias-ns defalias]
         :cljs [Exception IllegalArgumentException
                Nil Bool Num ExactNum Int Decimal Key Vec Set
                ArrList TreeMap LSeq Regex Editable Transient Queue Map])
      #?@(:cljs [:refer-macros [defalias]])]
    #?(:clj [clojure.pprint  :as pprint :refer [pprint]])
    [quantum.core.data.map :as map    :refer [sorted-map+]]
    [quantum.core.macros #?@(:clj [:refer [assert-args]])]
    #?(:clj [proteus :refer [let-mutable]])
    [quantum.core.type     :as type :refer
      [#?(:clj bigint?) #?(:cljs class) instance+? array-list? boolean? double? map-entry?
       sorted-map? queue? lseq? coll+? pattern? regex?
       editable? transient? #?(:clj should-transientize?)]])
  #?(:cljs
    (:require-macros
      [quantum.core.macros :refer [assert-args]]
      [quantum.core.type   :refer [should-transientize?]]))
  #?@(:clj
      [(:import
        clojure.core.Vec
        (quantum.core.ns
          Nil Bool Num ExactNum Int Decimal Key Set
                 ArrList TreeMap LSeq Regex Editable Transient Queue Map))
       (:gen-class)]))

#?(:clj (ns/require-all *ns* :clj))
#?(:clj (set! *unchecked-math* true))

#?(:clj
(defmacro until [pred & body]
  `(while (not ~pred) ~@body)))

#?(:clj
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
        ~ret ~coll))))
 
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
(defmacro lfor [& args]
  `(#?(:clj clojure.core/for :cljs cljs.core/for) ~@args)) ); "lazy-for"

#?(:clj
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
     ~coll)))

#?(:clj
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
     nil ~coll))))

#?(:clj
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
         ~coll))))

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