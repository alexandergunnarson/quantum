(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support.

      Adds some interesting reducers and folders from different sources
      gleaned from the far reaches of the internet. Some of them have
      unexpectedly great performance."
      :author       "Rich Hickey"
      :contributors #{"Alan Malloy" "Alex Gunnarson" "Christophe Grand"}}
  quantum.core.reducers
  (:refer-clojure :exclude [reduce Range ->Range])
  (:require
    [clojure.core                  :as core]
    [clojure.core.reducers         :as r]
    [quantum.core.collections.base :as cbase
      :refer [nnil?]]
    [quantum.core.collections.core :as ccoll]
    [quantum.core.core
      :refer [->sentinel]]
    [quantum.core.data.map         :as map]
    [quantum.core.data.set         :as set]
    [quantum.core.data.vector      :as vec
      :refer [catvec subsvec]]
    [quantum.core.error            :as err
      :refer [->ex]]
    [quantum.core.fn               :as fn
      :refer [call firsta aritoid
              fn1 fn-> fn->> fn' fn&2 rcomp defcurried]]
    [quantum.core.logic            :as logic
      :refer [fn-not fn-or fn-and fn-nil fn-true whenf whenf1 ifn condf condf1]]
    [quantum.core.macros           :as macros
      :refer [defnt case-env assert-args]]
    [quantum.core.numeric          :as num]
    [quantum.core.type             :as type
      :refer [instance+? lseq?]]
    [quantum.core.reducers.reduce  :as red
      :refer [reducer first-non-nil-reducer]]
    [quantum.core.reducers.fold    :as fold
      :refer [folder]]
    [quantum.core.vars             :as var
      :refer [defalias def-]])
  (:require-macros
    [quantum.core.reducers
      :refer [reduce join]]))

(def sentinel (->sentinel))

(defn transducer->reducer
  "Converts a transducer into a reducer."
  {:todo #{"More arity"}}
  ([transducer xs a0        ] (reducer xs (      transducer a0)))
  ([transducer xs a0 a1     ] (reducer xs (      transducer a0 a1)))
  ([transducer xs a0 a1 & as] (reducer xs (apply transducer a0 a1 as))))

#?(:clj (defalias join      ccoll/join     ))
#?(:clj (defalias joinl'    ccoll/joinl'   ))
#?(:clj (defalias join'     ccoll/join'    ))
        (defalias pjoin     fold/pjoin   )
        (defalias pjoin'    fold/pjoin'  )
#?(:clj (defalias reduce    red/reduce   ))
        (defalias fold*     fold/fold    ) ; "fold*" to avoid clash of namespace quantum.core.reducers.fold with var quantum.core.reducers/fold
        (defalias red-apply red/red-apply)
;___________________________________________________________________________________________________________________________________
;=================================================={    transduce.reducers    }=====================================================
;=================================================={                          }=====================================================
(defcurried map-state
  "Like map, but threads a state through the sequence of transformations.
  For each x in coll, f is applied to [state x] and should return [state' x'].
  The first invocation of f uses init as the state."
  {:attribution "transduce.reducers"
   :todo ["Test if volatiles will work."]}
  [f init coll]
  (reducer coll
    (fn [f1]
      (let [state (atom init)]
        (fn [acc x]
          (let [[state' x'] (f @state x)] ; How about using volatiles here?
            (reset! state state')
            (f1 acc x')))))))

(defalias reduce-count ccoll/reduce-count)

(defn fold-count
  {:attribution "parkour.reducers"}
  [coll]
  (fold*
    (aritoid (fn' 0) identity +)
    (aritoid (fn' 0) identity (rcomp firsta inc))
    coll))
;___________________________________________________________________________________________________________________________________
;=================================================={           CAT            }=====================================================
;=================================================={                          }=====================================================
(defn cat+
  ([] (throw (->ex :not-supported))) ; TODO fix this arity that CLJS is complaining about
  ([f] (core/cat f))
  ([f coll] (folder coll (core/cat f))))

(defn append!
  ".adds x to acc and returns acc"
  {:adapted-from "clojure.core.reducers"}
  [acc x]
  #?(:clj  (doto ^java.util.Collection acc (.add  x))
     :cljs (doto acc (.push x))))

(defn foldcat+
  "Equivalent to `(fold cat+ append! xs)`"
  {:adapted-from "clojure.core.reducers"
   :performance "`foldcat+` is faster than `into` a PersistentVector because it outputs ArrayLists"}
  [xs]
  (fold* cat+ append! xs))
;___________________________________________________________________________________________________________________________________
;=================================================={           MAP            }=====================================================
;=================================================={                          }=====================================================
(defn map:transducer [f]
  (fn [rf]
    (fn ; TODO auto-generate?
      ([            ] (rf))
      ([ret         ] (rf ret))
      ([ret x0      ] (rf ret (f x0)))
      ([ret x0 x1   ] (rf ret (f x0 x1)))
      ([ret x0 x1 x2] (rf ret (f x0 x1 x2)))
      ([ret x0 x1 x2 & xs]
         (rf ret (apply f x0 x1 x2 xs))))))

(defn map+
  ([f] (map:transducer f))
  ([f coll] (folder coll (map:transducer f))))

(defn map-indexed:transducer [f] (core/map-indexed f))

(defn map-indexed+
  ([f] (map-indexed:transducer f))
  ([f coll] (folder coll (map-indexed:transducer f))))

(defn pmap-indexed+
  "Thread-safe `map-indexed+`.
   Just `map-indexed+` with an `atom` instead of a `volatile`."
  [f coll]
  (folder coll
    (fn [rf]
      (let [i (atom -1)]
        (fn
          ([] (rf))
          ([result] (rf result))
          ([result input]
           (rf result (f (swap! i inc) input))))))))

(defn indexed+
  "Returns an ordered sequence of vectors `[index item]`, where item is a
  value in coll, and index its position starting from zero."
  {:attribution "weavejester.medley"}
  [coll]
  (map-indexed+ vector coll))

(defn pindexed+
  "`map-indexed+` : `indexed+` :: `pmap-indexed+` : `pindexed+`"
  [coll] (pmap-indexed+ vector coll))
;___________________________________________________________________________________________________________________________________
;=================================================={          MAPCAT          }=====================================================
;=================================================={                          }=====================================================
; mapcat: ([:a 1] [:b 2] [:c 3]) versus mapcat+: (:a 1 :b 2 :c 3) ; hmm...

(defn #_defcurried mapcat+
  "Applies f to every value in the reduction of coll, concatenating the result
  colls of (f val). Foldable."
  [f coll]
  (folder coll (core/mapcat f)))

(defn concat+ [& args] (mapcat+ identity args))
;___________________________________________________________________________________________________________________________________
;=================================================={        REDUCTIONS        }=====================================================
;=================================================={                          }=====================================================
(defn map-accum-transducer
  {:attribution "alexandergunnarson"}
  [f]
  (fn [rf]
    (fn
      ([] (rf))
      ([xs'] (rf xs'))
      ([xs' x] (rf xs' (f xs' x))))))

(defn map-accum+
  "Like `map+`, but the accumulated reduction gets passed through as the
   first argument to `f`, and the current element as the second argument."
  [f xs] (folder xs (map-accum-transducer f)))

#_(defn reductions-transducer ; TODO finish
  {:attribution "alexandergunnarson"}
  [f]
  (fn [rf]
    (fn
      ([] (rf))
      ([xs'] (rf xs'))
      ([xs' x] (rf xs' (f xs' x))))))

#_(defn reductions+ ; TODO finish
  {:attribution "alexandergunnarson"}
  [f xs] (folder xs (reductions-transducer f)))
;___________________________________________________________________________________________________________________________________
;=================================================={      FILTER, REMOVE      }=====================================================
;=================================================={                          }=====================================================
(defn filter-transducer [pred]
  (fn [rf]
    (fn
      ([] (rf))
      ([ret] (rf ret))
      ([ret x]
         (if (pred x)
             (rf ret x)
             ret))
      ([ret k v]
         (if (pred k v)
             (rf ret k v)
             ret)))))

(defn filter+
  "Returns a version of the folder which only passes on inputs to subsequent
   transforms when (@pred <input>) is truthy."
  [pred coll] (folder coll (filter-transducer pred)))

(defn remove+
  "Returns a version of the folder which only passes on inputs to subsequent
   transforms when (@pred <input>) is falsey."
  [pred coll] (filter+ (complement pred) coll))

(defn keep+         [f coll] (folder coll (core/keep         f)))
(defn keep-indexed+ [f coll] (folder coll (core/keep-indexed f)))
;___________________________________________________________________________________________________________________________________
;=================================================={         FLATTEN          }=====================================================
;=================================================={                          }=====================================================
(defcurried flatten+
  "Takes any nested combination of sequential things (lists, vectors,
  etc.) and returns their contents as a single, flat foldable
  collection."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [coll]
  (folder coll
   (fn [reducer-]
     (fn ([] (reducer-))
         ([ret v]
            (if (sequential? v)
                (red/reduce reducer- ret (flatten+ v))
                (reducer- ret v)))))))

(def flatten-1+ (fn->> (mapcat+ identity)))
;___________________________________________________________________________________________________________________________________
;=================================================={          SOURCES         }=====================================================
;=================================================={                          }=====================================================
(def identity-rf (fn [rf] (aritoid rf rf rf)))
;___________________________________________________________________________________________________________________________________
;=================================================={          REPEAT          }=====================================================
;=================================================={                          }=====================================================
(declare repeatedly+)

#?(:clj
(defn fold-repeatedly
  {:adapted-from "CLJ-994 (Jason Jackson)"}
  [n f group-size combinef reducef]
  (if (<= n group-size)
      (reduce reducef (combinef) (repeatedly+ n f))
      (let [left  (quot n 2)
            right (- n left)
            fc (fn [n] #(fold-repeatedly n f group-size combinef reducef))]
        (apply combinef (fold/fj-invoke-2-fns (fc left) (fc right)))))))

(defn repeatedly+
  "Yields a reducible collection of infinite (or length n if supplied)
   sequence of xs. If n is specified, then it is foldable."
  {:adapted-from "CLJ-994 (Jason Jackson)"}
  ([f]
    (reducer
      (reify
        #?(:clj  clojure.core.protocols/CollReduce
           :cljs cljs.core/IReduce)
        (#?(:clj coll-reduce :cljs -reduce) [this f1]
          (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce) this f1 (f1)))
        (#?(:clj coll-reduce :cljs -reduce) [this f1 init]
          (loop [ret init]
            (if (reduced? ret)
              @ret
              (recur (f1 ret (f)))))))
      identity-rf))
  ([n f]
    (folder
      (reify
        #?(:clj  clojure.core.protocols/CollReduce
           :cljs cljs.core/IReduce)
        (#?(:clj coll-reduce :cljs -reduce) [this f1]
          (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce) this f1 (f1)))
        (#?(:clj coll-reduce :cljs -reduce) [this f1 init]
          (loop [ret init n n]
            (cond (reduced? ret) @ret
                  (<= n 0) ret
                  :else (recur (f1 ret (f)) (dec n)))))
        r/CollFold
        (coll-fold [this group-size combinef reducef]
          #?(:clj  (fold-repeatedly n f group-size combinef reducef)
             :cljs (red/reduce this reducef (reducef)))))
      identity-rf)))

(defn repeat+
  ([  x] (repeatedly+   (fn' x)))
  ([n x] (repeatedly+ n (fn' x))))
;___________________________________________________________________________________________________________________________________
;=================================================={         ITERATE          }=====================================================
;=================================================={                          }=====================================================
(defn iterate+
  "A reducible collection of [seed, (f seed), (f (f seed)), ...]"
  {:adapted-from "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"
   :performance "Untested"}
  [f seed]
  (reducer
    (reify
     #?(:clj  clojure.core.protocols/CollReduce
        :cljs cljs.core/IReduce)
       (#?(:clj coll-reduce :cljs -reduce) [this f1]
         (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce) this f1 (f1)))
       (#?(:clj coll-reduce :cljs -reduce) [this f1 init]
         (loop [ret (f1 init seed) seed seed]
           (if (reduced? ret)
             @ret
             (let [next-n (f seed)]
               (recur (f1 ret next-n) next-n))))))
    identity-rf))
;___________________________________________________________________________________________________________________________________
;=================================================={          RANGE           }=====================================================
;=================================================={                          }=====================================================
; https://gist.github.com/amalloy/1586b2460329dde1c374 - Creating new Reducer sources
(deftype Range [start end step]
  #?(:clj  clojure.lang.Counted
     :cljs cljs.core/ICounted)
    (#?(:clj count :cljs -count) [this]
      (int (num/ceil (/ (- end start) step))))
  #?(:clj  clojure.lang.Seqable
     :cljs cljs.core/ISeqable)
    (#?(:clj seq :cljs -seq) [this]
      (seq (range start end step)))
  #?(:clj  clojure.core.protocols/CollReduce
     :cljs cljs.core/IReduce)
    (#?(:clj coll-reduce :cljs -reduce) [this f1]
      (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce) this f1 (f1)))
    (#?(:clj coll-reduce :cljs -reduce) [this f1 init]
      (let [cmp (if (pos? step) < >)]
        (loop [ret init i start]
          (if (reduced? ret)
            @ret
            (if (cmp i end)
              (recur (f1 ret i) (+ i step))
              ret)))))
  r/CollFold
    (coll-fold [this n combinef reducef]
      (fold/fold-by-halves
        (fn [_ size] ;; the range passed is always just this Range
            (let [split (-> (quot size 2)
                            (* step)
                            (+ start))]
              [(quantum.core.reducers/Range. start split step)
               (quantum.core.reducers/Range. split end step)]))
          this n combinef reducef)))

(deftype LongRange [^long start ^long end ^long step]
  #?(:clj  clojure.lang.Counted
     :cljs cljs.core/ICounted)
    (#?(:clj count :cljs -count) [this]
      (int (num/ceil (/ (- end start) step))))
  #?(:clj  clojure.lang.Seqable
     :cljs cljs.core/ISeqable)
    (#?(:clj seq :cljs -seq) [this]
      (seq (range start end step)))
  #?(:clj  clojure.core.protocols/CollReduce
     :cljs cljs.core/IReduce)
    (#?(:clj coll-reduce :cljs -reduce) [this f1]
      (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce) this f1 (f1)))
    (#?(:clj coll-reduce :cljs -reduce) [this f1 init]
      (let [cmp (if (pos? step) < >)]
        (loop [ret init i start]
          (if (reduced? ret)
            @ret
            (if (cmp i end)
              (recur (f1 ret i) (unchecked-add i step))
              ret)))))
  r/CollFold
    (coll-fold [this n combinef reducef]
      (fold/fold-by-halves
        (fn [_ size] ;; the range passed is always just this Range
            (let [split (-> (quot size 2)
                            (* step)
                            (+ start))]
              [(quantum.core.reducers/LongRange. start split step)
               (quantum.core.reducers/LongRange. split end step)]))
          this n combinef reducef)))

; "Range and iterate shouldn't be novel in reducers, but just enhanced return values of core fns.
; Range and iterate are sources, not transformers, and only transformers
; (which must be different from their seq-based counterparts) must reside in reducers." - Rich Hickey

(defnt range+*
  "Returns a reducible collection of nums from start (inclusive) to end
  (exclusive), by step, where start defaults to 0, step to 1, and end
  to infinity."
  {:adapted-from "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  ([^default end                       ] (folder (Range.     0     end 1   ) identity-rf))
  ([^long    end                       ] (folder (LongRange. 0     end 1   ) identity-rf))
  ; TODO fix types here
  ([^default start       end           ] (folder (Range.     start end 1   ) identity-rf))
  ([^long    start ^long end           ] (folder (LongRange. start end 1   ) identity-rf))
  ; TODO fix types here
  ([^default start       end       step] (folder (Range.     start end step) identity-rf))
  ([^long    start ^long end ^long step] (folder (LongRange. start end step) identity-rf)))

#?(:clj
(defmacro range+
  ([] `(iterate+ ~(case-env :cljs `inc `inc') 0))
  ([& args] `(range+* ~@args))))
;___________________________________________________________________________________________________________________________________
;=================================================={     TAKE, TAKE-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defalias take+ ccoll/take+)

(defn take-while+ [pred coll] (reducer coll (core/take-while pred)))
(defn take-nth+   [n    coll] (reducer coll (core/take-nth   n   )))

#?(:clj (defalias taker+ ccoll/taker+))
;___________________________________________________________________________________________________________________________________
;=================================================={     DROP, DROP-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defalias drop+ ccoll/drop+)

(defn drop-while+ [pred coll] (reducer coll (core/drop-while pred)))

#?(:clj (defalias dropr+ ccoll/dropr+))
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
(defn reduce-by+
  "Partition `coll` with `keyfn` as per /partition-by/, then reduce
  each partition with `f` and optional initial value `init` as per
  /reduce/."
  {:attribution "parkour.reducers"}
  ([keyfn f coll]
    (->> (mapcat+ identity [coll [sentinel]])
         (map-state
           (fn [[k acc] x]
             (if (identical? sentinel x)
               [nil acc]
               (let [k' (keyfn x)]
                 (if (or (= k k') (identical? sentinel k))
                   [[k' (f acc x)] sentinel]
                   [[k' (f (f) x)] acc]))))
           [sentinel (f)])
         (remove+ (partial identical? sentinel))))
  ([keyfn f init coll]
     (let [f (fn ([] init) ([acc x] (f acc x)))]
       (reduce-by+ keyfn f coll))))

(defn partition-by+  [f coll] (folder coll (core/partition-by  f)))
(defn partition-all+ [n coll] (folder coll (core/partition-all n)))

(defn group-by+ ; Yes, but folds a lazy sequence... hmm...
  "Reducers version. Possibly slower than |core/group-by|.
   A terminal transform."
  {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/12c3k7ztbz/group-by-vs-reducers"
   :usage '(group-by+ odd? (range 10))
   :out   '{false [0 2 4 6 8], true [1 3 5 7 9]}}
  [f coll]
  (fold*
    (partial merge-with ; merge-with is why...
      (fn [v1 v2] (->> (concat+ v1 v2) (join []))))
    (fn ([ret] ret)
        ([groups a]
          (let [k (f a)]
            (assoc groups k (conj (get groups k []) a)))))
    coll))
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================
(defn dedupe+:sorted ; 228.936664 ms (pretty much attains java speeds!!!)
  "Remove adjacent duplicate values of (@f x) for each x in @coll.
   CAVEAT: Requires @coll to be sorted to work correctly."
  {:attribution "parkour.reducers"}
  [f comp-fn xs]
  (->> xs
       (map-state
         (fn [x x']
           (let [xf  (whenf x  (fn-not (fn1 identical? sentinel)) f)
                 xf' (whenf x' (fn-not (fn1 identical? sentinel)) f)]
             [x' (if (comp-fn xf xf') sentinel x')]))
         sentinel)
       (remove+ (partial identical? sentinel))))

(defn dedupe+ [xs] (folder xs (core/dedupe)))

(defn pdedupe+
  "Thread-safe `dedupe+`.
   Just `dedupe+` with an `atom` instead of a `volatile`."
  [f xs]
  (folder xs
    (fn [rf]
      (let [pv (atom ::none)]
        (fn
          ([] (rf))
          ([result] (rf result))
          ([result input]
            (let [prior @pv]
              (reset! pv input)
              (if (= prior input)
                result
                (rf result input)))))))))

(defn distinct+ [xs] (folder xs (core/distinct)))

(defn pdistinct+
  "Thread-safe `distinct+`.
   Just `distinct+` with an `atom` instead of a `volatile`."
  {:todo #{"Test with `!!hash-set`"}}
  [xs]
  (fn [rf]
    (let [seen (atom #{})]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if (contains? @seen input)
           result
           (do (swap! seen conj input)
               (rf result input))))))))

(defn distinct-by+  [f xs] (->> xs (map+ f)  distinct+))
(defn pdistinct-by+ [f xs] (->> xs (map+ f) pdistinct+))

(defn replace+ [smap xs] (folder xs (core/replace smap)))

(defn reduce-sentinel
  "Calls `reduce` with a sentinel.
   Useful for e.g. `max` and `min`."
  {:attribution "alexandergunnarson"}
  [rf xs]
  (let [ret (reduce
              (fn [ret x] (if (identical? ret sentinel) x (rf ret x)))
              sentinel
              xs)]
    (if (identical? ret sentinel) nil ret)))

(defn reduce-max-key [kfn xs] (reduce-sentinel (fn&2 max-key kfn) xs))
(defn reduce-min-key [kfn xs] (reduce-sentinel (fn&2 min-key kfn) xs))
(defn reduce-min     [    xs] (reduce-sentinel       min          xs))
(defn reduce-max     [    xs] (reduce-sentinel       max          xs))

(defn fold-frequencies
  "Like clojure.core/frequencies, returns a map of inputs to the number of
   times those inputs appeared in the collection.
   A terminal transform."
  {:todo ["Can probably make this use transients in the reducing fn."]}
  [coll]
  (fold*
    (aritoid hash-map identity (partial merge-with +))
    (aritoid hash-map identity (fn add [freqs x]
                                 (assoc freqs x (inc (get freqs x 0)))))
    coll))

(defn fold-some
 "Returns the first logical true value of (pred input). If no such satisfying
  input exists, returns nil.
  This is potentially *less* efficient than clojure.core/some because each
  reducer has to find a matching element independently, and they have no way to
  communicate when one has found an element. In the worst-case scenario,
  requires N calls to `pred`. However, unlike clojure.core/some, this version
  is parallelizable--which can make it more efficient when the element is rare."
  {:source "tesser.core"
   :adapted-by "alexandergunnarson"
   :todo ["Have folder-threads communicate when found an element"]
   :tests '{(->> [1 2 3 4 5 6] (fold-some #{1 2}))
            2}}
  [pred coll]
  (fold*
    (aritoid fn-nil identity first-non-nil-reducer)
    (aritoid fn-nil
             identity
             (fn [_ x] (when-let [v (pred x)] (reduced v))))
    coll))

(defn fold-any
  "Returns any single input from the collection. O(chunks).
   Terminal transform."
  {:source "tesser.core"
   :adapted-by "alexandergunnarson"
   :tests '{(->> [1 2 3 4 5 6] (fold-any))
            4}}
  [coll]
  (let [combiner+reducer
         (aritoid fn-nil identity first-non-nil-reducer)]
    (fold* combiner+reducer combiner+reducer coll)))

(defn fold-extremum
  "Finds the largest element using a comparison function, e.g. `compare`.
   Terminal transform."
  {:source "tesser.core"
   :adapted-by "alexandergunnarson"
   :tests '{(->> [5 4 3 2 1 0] (fold-extremum compare))
            5}}
  [compare-fn coll]
  (let [extremum-reducer
         (fn [m x]
           (cond (nil? m)                x
                 (nil? x)                m
                 (<= 0 (compare-fn x m)) x
                 true                    m))
        combiner+reducer
          (aritoid fn-nil identity extremum-reducer)]
    (fold* combiner+reducer combiner+reducer coll)))

(defn fold-min
  "Finds the smallest value using `compare`."
  {:tests '{(->> [:a :b :c :d] (fold-min))
            :a}}
  [coll]
  (->> coll (fold-extremum (rcomp compare -))))

(defn fold-max
  "Finds the largest value using `compare`."
  {:tests '{(->> [:a :b :c :d] (fold-max))
            :d}}
  [coll]
  (->> coll (fold-extremum compare)))

(defn fold-empty?
  "Returns true iff no inputs arrive; false otherwise."
  {:source "tesser.core"
   :adapted-by "alexandergunnarson"
   :tests '{(->> [] fold-empty?)
            true}}
  [coll]
  (->> coll
       (map+ fn-true)
       (fold-some true?)
       boolean not))

(defn fold-every?
  "True iff every input satisfies the given predicate, false otherwise."
  {:source "tesser.core"
   :adapted-by "alexandergunnarson"
   :tests '{(->> [1 3 5] (fold-every? odd?))
            true}}
  [pred coll]
  (->> coll
       (remove+ pred)
       fold-empty?))

(defn interpose+ [sep coll] (folder coll (core/interpose sep)))

(defalias zipvec+ interpose+)
;___________________________________________________________________________________________________________________________________
;=================================================={ LOOPS / LIST COMPREHENS. }=====================================================
;=================================================={        for, doseq        }=====================================================
(defn for+:gen-arities-2-3 [bindings kv-ct f-inner main-arity]
  (cond
     (= (count bindings) kv-ct)
     `(~main-arity
       ([ret# k# v#] (~f-inner ret# [k# v#])))
     (= (count bindings) (inc kv-ct))
     `(([ret# kv#] (~f-inner ret# (first kv#) (second kv#)))
       ~main-arity)
     :else (throw (->ex "Too many binding values for `for+`:" {:bindings bindings}))))

(defn for+:gen-f [f f-inner arities-2-3]
  `(fn [~f]
     (fn ~f-inner
       ([] (~f))
       ~@arities-2-3)))

#?(:clj
(defmacro for+
  "Reducer comprehension. Like `for` but yields a reducible/foldable collection.
   Essentially `map+` but written differently."
  {:origin      "Christophe Grand, https://gist.github.com/cgrand/5643767"
   :adapted-by  "alexandergunnarson"
   :performance "51.454164 ms vs. 72.568330 ms for |doall| with normal (lazy) |for|"}
  [bindings & body]
  (assert-args (vector? bindings) "a vector for its bindings")
  (let [f       (gensym "f")
        f-inner (gensym "f-inner")
        main-arity  `([ret# ~@(butlast bindings)] (~f ret# (do ~@body)))
        arities-2-3 (for+:gen-arities-2-3 bindings 2 f-inner main-arity)]
   `(folder ~(last bindings) ~(for+:gen-f f f-inner arities-2-3)))))

#?(:clj
(defmacro fori+*
  [atomic-container atomic-mutator bindings & body]
  (assert-args (vector? bindings) "a vector for its bindings")
  (let [i*      (gensym "i*")
        i       (last bindings)
        f       (gensym "f")
        f-inner (gensym "f-inner")
        main-arity `([ret# ~@(-> bindings butlast butlast)]
                      (let [~i (~atomic-mutator ~i* inc)]
                        (~f ret# (do ~@body))))
        arities-2-3 (for+:gen-arities-2-3 bindings 3 f-inner main-arity)]
   `(folder ~(-> bindings butlast last)
      (let [~i* (~atomic-container -1)] ~(for+:gen-f f f-inner arities-2-3))))))

#?(:clj (defmacro fori+  "Like `for+`, but indexed."               [& args] `(fori+* volatile! vswap! ~@args)))
#?(:clj (defmacro pfori+ "Like `for+`, but thread-safely indexed." [& args] `(fori+* atom       swap! ~@args)))

; <<<<------ ALREADY REDUCED ------>>>>
#?(:clj
(defmacro doseq+
  "|doseq| but based on reducers."
  {:attribution "Christophe Grand, https://gist.github.com/cgrand/5643767"}
  [bindings & body]
 `(red/reduce fn-nil (for+ ~bindings (do ~@body)))))

(defcurried each ; like doseq
  "Applies f to each item in coll, returns nil"
  {:attribution "transduce.reducers"}
  [f coll]
  (red/reduce (fn [_ x] (f x) nil) nil coll))

(defn sample+ [prob coll] (folder coll (core/random-sample prob)))

(defalias random-sample+ sample+)

;___________________________________________________________________________________________________________________________________
;=================================================={      TO INCORPORATE      }=====================================================
;=================================================={                          }=====================================================

(comment
(defn- chunk-reducer
  "Given a compiled fold, constructs a function which takes a chunk and returns
  its post-reduced value."
  [{:keys [reducer reducer-identity post-reducer]}]
  (fn r [chunk]
    (->> chunk
         (reduce reducer (reducer-identity))
         post-reducer)))

(defn- chunk-combiner
  "Given a compiled fold, returns a reducing function that merges new
  post-reduced values into an accumulator `combined`, iff the current
  accumulator is not already reduced."
  [{:keys [combiner]}]
  (fn c [combined post-reduced]
    (if (reduced? combined)
        combined
        (combiner combined post-reduced))))

; Unlike reducers, we don't use the Java forkjoin pool, just plain old threads;
; it avoids contention issues and improves performance on most JDKs.

(defn pcollapse*
  "Compiles a fold (set of transforms) and applies it to a sequence of sequences
  of inputs. Runs num-procs threads for the parallel (reducer) portion of the fold
  Reducers take turns combining their results, which prevents unbounded memory
  consumption by the reduce phase.
      (->> [[\"hi\"] [\"there\"]]
           (t/fold str)
           (t/collapse {:chunked? true}))
      ; => \"therehi\""
  [folder & [{:keys [threads chunked?] :as opts}]]
  (let [^Iterable chunks coll
        t0         (System/nanoTime)
        threads    (or threads (.. Runtime getRuntime availableProcessors))
        iter       (.iterator chunks)
        reducer    (chunk-reducer  folder)
        combiner   (chunk-combiner folder)
        combined   (atom (combiner))]
    (let [workers
          (->> threads
               core/range
               (mapv
                 (fn spawn [i]
                   (future-call
                     (fn worker []
                       (while
                         (let [chunk (locking iter
                                       (if (.hasNext iter)
                                         (.next iter)
                                         ::finished))]
                           (when (not= ::finished chunk)
                             (let [; Concurrent reduction
                                   result (reducer chunk)

                                   ; Sequential combine phase
                                   combined' (locking combined
                                               (swap! combined
                                                      combiner result))]
                               ; Abort early if reduced.
                               (not (reduced? combined')))))))))))]
      (try
        ; Wait for workers
        (mapv deref workers)
        (let [combined @combined
              ; Unwrap reduced
              combined (if (reduced? combined) @combined combined)
              ; Postcombine
              result ((:post-combiner folder) combined)
              t1    (System/nanoTime)]
          result)
        (finally
          ; Ensure workers are dead
          (mapv future-cancel workers))))))

(deftransform take+
  "Like clojure.core/take, limits the number of inputs passed to the downstream
  transformer to exactly n, or if fewer than n inputs exist in total, all
  inputs.
      (->> (t/map inc)
           (t/take 5)
           (t/into [])
           (t/tesser [[1 2 3] [4 5 6] [7 8 9]]))
      ; => [6 7 5 3 4]
  Space complexity note: take's reducers produce log2(chunk-size) reduced
  values per chunk, ranging from 1 to chunk-size/2 inputs, rather than a single
  reduced value for each chunk. See the source for *why* this is the case."
  [n]
  (assert (not (neg? n)))
  (->Folder
    {:reducer-identity  (fn reducer-identity [] (list 0 (reducer-identity-)))
     :reducer           (fn reducer [[c acc & finished :as reductions] input]
                          ; TODO: limit to n
                          (if (zero? c)
                            ; Overwrite the 0 pair
                            (scred reducer-
                                   (list 1 (reducer- acc input)))
                            (let [limit (Math/pow 2 (dec (/ (count reductions) 2)))]
                              (if (<= limit c)
                                ; We've filled this chunk; proceed to the next.
                                (scred reducer-
                                       (cons 1 (cons (reducer- (reducer-identity-) input)
                                                     reductions)))
                                ; Chunk isn't full yet; keep going.
                                (scred reducer-
                                       (cons (inc c) (cons (reducer- acc input) finished)))))))
     :post-reducer      (fn post-reducer [reductions]
                          (->> reductions
                               (partition 2)
                               (mapcat (fn [[n acc]] (list n (post-reducer- acc))))))
     :combiner-identity (fn combiner-identity [] (list 0 (combiner-identity-)))
     :combiner          (fn combiner [outer-acc reductions]
                          (let [acc' (reduce (fn merger [acc [c2 x2]]
                                               (let [[c1 x1] acc]
                                                 (if (= n c1)
                                                   ; Done
                                                   (reduced acc)
                                                   ; Okay, how big would we get if we
                                                   ; merged x2?
                                                   (let [c' (+ c1 c2)]
                                                     (if (< n c')
                                                       ; Too big; pass
                                                       acc
                                                       ; Within bounds; take it!
                                                       (scred combiner-
                                                              (list c' (combiner-
                                                                         x1 x2))))))))
                                             outer-acc
                                             (partition 2 reductions))]

                            ; Break off as soon as we have n elements
                            (if (= n (first acc'))
                              (reduced acc')
                              acc')))
     :post-combiner     (comp post-combiner- second)}))

(defwraptransform post-combine
  "Transforms the output of a fold by applying a function to it.
  For instance, to find the square root of the mean of a sequence of numbers,
  try
      (->> (t/mean) (t/post-combine sqrt) (t/tesser nums))
  For clarity in ->> composition, post-combine composes in the opposite
  direction from map, filter, etc. It *prepends* a transform to the given fold
  instead of *appending* one. This means post-combines take effect in the same
  order you'd expect from ->> with normal function calls:
      (->> (t/mean)                 (->> (mean nums)
           (t/post-combine sqrt)         (sqrt)
           (t/post-combine inc))         (inc))"
  [f folder]
  (assoc downstream :post-combiner
         (fn post-combiner [x]
           (f (post-combiner- x)))))

(deftransform group-by+
  "Every input belongs to exactly one category, and you'd like to apply a fold
  to each category separately.
  Group-by takes a function that returns a category for every element, and
  returns a map of those categories to the results of the downstream fold
  applied to the inputs in that category.
  For instance, say we have a collection of particles of various types, and
  want to find the highest mass of each particle type:
      (->> (t/group-by :type)
           (t/map :mass)
           (t/max)
           (t/tesser [[{:name :electron, :type :lepton, :mass 0.51}
                       {:name :muon,     :type :lepton, :mass 105.65}
                       {:name :up,       :type :quark,  :mass 1.5}
                       {:name :down,     :type :quark,  :mass 3.5}]]))
      ; => {:lepton 105.65, :quark 3.5}"
  [category-fn folder]
  (->Folder
    {:reducer-identity  #(transient {})
     :reducer           (fn reducer [acc input]
                          (let [category (category-fn input)]
                            (assoc! acc category
                              (reducer-
                                ; TODO: invoke downstream identity only when
                                ; necessary.
                                (get acc category (reducer-identity-))
                                input))))
     :post-reducer      persistent!
     :combiner-identity hash-map
     :combiner        (fn combiner [m1 m2]
                        (merge-with combiner- m1 m2))
     :post-combiner   (fn post-combiner [m]
                        (map-vals post-combiner- m))}))

(deftransform facet
  "Your inputs are maps, and you want to apply a fold to each value
  independently. Facet generalizes a fold over a single value to operate on
  maps of keys to those values, returning a map of keys to the results of the
  fold over all values for that key. Each key gets an independent instance of
  the fold.
  For instance, say you have inputs like
      {:x 1, :y 2}
      {}
      {:y 3, :z 4}
  Then the fold
      (->> (facet)
           (mean))
  returns a map for each key's mean value:
      {:x 1, :y 2, :z 4}"
  [folder]
  (->Folder
    {:reducer-identity  #(transient {})
     :reducer           (fn reducer [acc m]
                          ; Fold value m into accumulator map
                          (core/reduce
                            (fn [acc [k v]]
                              ; Fold in each kv pair in m
                              (assoc! acc k
                                (reducer-
                                  ; TODO: only invoke downstream identity
                                  ; when necessary
                                  (get acc k (reducer-identity-)) v)))
                            acc
                            m))
     :post-reducer      persistent!
     :combiner-identity hash-map
     :combiner          (fn combiner [m1 m2]
                          (merge-with combiner- m1 m2))
     :post-combiner     (fn post-combiner [m]
                          (map-vals post-combiner- m))}))

; Fold combinators like facet and fuse allow multiple reductions to be done in a single pass,
; possibly sharing expensive operations like deserialization. This is a particularly
; effective way of working with a set of data files on disk or in Hadoop.

(deftransform fuse
  "You've got several folders, and want to execute them in one pass. Fuse is the
  function for you! It takes a map from keys to folds, like
      (->> people
           (map+ parse-person)
           (fuse {:age-range    (->> (map :age) (range))
                  :colors-prefs (->> (map :favorite-color) (frequencies))})
           (pcollapse))
  And returns a map from those same keys to the results of the corresponding
  folders:
      {:age-range   [0 74],
       :color-prefs {:red        120
                     :blue       312
                     :watermelon 1953
                     :imhotep    1}}
  Note that this fold only invokes `parse-person` once for each record, and
  completes in a single pass. If we ran the age and color folders independently,
  it'd take two passes over the dataset--and require parsing every person
  *twice*.
  Fuse and facet both return maps, but generalize over different axes. Fuse
  applies a fixed set of *independent* folders over the *same* inputs, where
  facet applies the *same* fold to a dynamic set of keys taken from the
  inputs.
  Note that fuse compiles the folders you pass to it, so you need to build them
  completely *before* fusing. The fold `fuse` returns can happily be combined
  with other transformations at its level, but its internal folders are sealed
  and opaque."
  [fold-map folder]
  (assert (not (transformer? downstream)) "|fuse| is a terminal transform")
  (let [ks             (vec (keys fold-map))
        n              (count ks)
        folders        (mapv (comp compile-fold (partial get fold-map)) ks)
        reducers       (mapv :reducer  folders)
        combiners      (mapv :combiner folders)]
    ; We're gonna project into a particular key basis vector for the
    ; reduce/combine steps
    (->Folder
      {:reducer-identity    (fn identity []
                              (let [a (object-array n)]
                                (dotimes [i n]
                                  (aset a i ((-> folders
                                                 (nth i)
                                                 :reducer-identity))))
                                a))
       :reducer             (fn reducer [^objects accs x]
                              (dotimes [i n]
                                (aset accs i ((nth reducers i) (aget accs i) x)))
                              accs)
       :post-reducer        identity
       :combiner-identity   (if (empty? fold-map)
                              vector
                              (apply juxt (map :combiner-identity folders)))
       :combiner            (fn combiner [accs1 accs2]
                              (mapv (fn [f acc1 acc2] (f acc1 acc2))
                                    combiners accs1 accs2))
       ; Then inflate the vector back into a map
       :post-combiner (comp (partial zipmap ks)
                            ; After having applied the post-combiners
                            (fn post-com [xs] (mapv (fn [f x] (f x))
                                                    (map :post-combiner folders)
                                                    xs)))
       :coll          downstream})))

(defn fold-extrema
  "Returns a pair of `[smallest largest]` inputs, using `compare`.
  For example:
      (t/tesser [[4 5 6] [1 2 3]] (t/range))
      ; => [1 6]"
  [& [f]]
  (->> f
       (fuse {:min (fold-min)
              :max (fold-max)})
       (post-combine (juxt :min :max))))
)

; TODO completing, transduce, eduction, sequence

#?(:clj
(defmacro defeager [sym plus-sym]
  (let [lazy-sym            (symbol (str "l" sym))
        quoted-sym          (symbol (str sym "'"))
        parallel-sym        (symbol (str "p" sym))
        parallel-quoted-sym (symbol (str "p" sym "'"))]
    `(do (defalias ~lazy-sym ~(symbol (case-env :cljs "cljs.core" "clojure.core") (name sym)))
         (defalias ~(var/unqualify plus-sym) ~plus-sym)
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
