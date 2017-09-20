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
  (:refer-clojure :exclude
    [reduce Range ->Range empty? deref reset!
     contains?, assoc!, conj! get, transduce])
  (:require
    [clojure.core                  :as core]
    [clojure.core.reducers         :as r]
    [quantum.core.collections.base :as cbase]
    [quantum.core.collections.core :as ccoll
      :refer [empty? contains?, assoc!, conj!, get, ->objects]]
    [quantum.core.core
      :refer [->sentinel]]
    [quantum.core.data.map         :as map]
    [quantum.core.data.set         :as set]
    [quantum.core.data.vector      :as vec
      :refer [catvec subsvec !vector]]
    [quantum.core.error            :as err
      :refer [->ex TODO]]
    [quantum.core.fn               :as fn
      :refer [call firsta aritoid
              fn1 fn-> fn->> fn' fn&2 rcomp defcurried
              with-do]]
    [quantum.core.log
      :refer [prl!]]
    [quantum.core.logic            :as logic
      :refer [fn-not fn-or fn-and fn-nil fn-true whenf whenf1 ifn condf condf1]]
    [quantum.core.macros           :as macros
      :refer [defnt case-env assert-args env-lang]]
    [quantum.core.macros.core
      :refer [gen-args arity-builder max-positional-arity
              unify-gensyms]]
    [quantum.core.numeric          :as num]
    [quantum.core.refs             :as refs
      :refer [! deref reset! volatile atom*]]
    [quantum.core.type             :as type
      :refer [instance+? lseq?]]
    [quantum.core.reducers.reduce  :as red
      :refer [transformer]]
    [quantum.core.reducers.fold    :as fold]
    [quantum.core.untyped.qualify  :as qual]
    [quantum.core.vars             :as var
      :refer [defalias def-]])
#?(:cljs
  (:require-macros
    [quantum.core.reducers
      :refer [reduce join]]))
#?(:clj
  (:import [java.util.concurrent.atomic AtomicLong]
           [clojure.lang Volatile]
           [quantum.core.refs IMutableLong])))

; TODO investigate https://github.com/aphyr/tesser

; TODO move and `fnt`
(defn inc!          [^IMutableLong *i] (reset! *i (inc (deref *i))))
(defn inc!:volatile [^Volatile     *i] (reset! *i (inc (deref *i))))
(defn inc!:atom*    [^AtomicLong   *i] (.incrementAndGet *i))

(def sentinel (->sentinel))

(defalias transducer->transformer red/transducer->transformer)

(defn preserving-reduced [rf]
  #(let [ret (rf %1 %2)]
     (if (reduced? ret)
         (reduced ret)
         ret)))

(defn gen-multiplex* [lang]
  `(~'defn ~'multiplex
     "Multiplexes multiple reducing functions with the provided finishing fn `f`
      which must accept as many arguments as reducing functions passed.
      Must call the initializer (0-arity) in order to function properly, e.g. via
      `transduce`.

      Note that the example is just to capture the concept and does not accurately
      reflect the source code.

      This function is not thread-safe."
     {:attribution "alexandergunnarson"
      :equivalent '~'{(!multiplex /
                        (aritoid + identity +                )   ; sum:rf
                        (aritoid + identity (rcomp firsta inc))) ; count:rf
                      (fn ([] [(f0) (f1)])
                          ([[x0 x1]] (/ (f0 x0) (f1 x1))) ; to get the mean at the end
                          ([[x0 x1] x'] [(f0 x0 x') (f1 x1 x')]))}}
    ~@(let [!xs (with-meta (gensym "!xs") {:tag (case lang :clj "[Ljava.lang.Object;" :cljs nil)})]
        (arity-builder (fn [args] (if (= 2 (count args))
                                      (let [[f rf0] args] `(aritoid ~rf0 ~f ~rf0))
                                      (let [[f & rfs] args rfs-i (map-indexed vector rfs)]
                                        (unify-gensyms
                                         `(fn ([] (let [~!xs (->objects ~(count rfs-i))]
                                                   ~@(for [[i rf] rfs-i]
                                                      `(assoc! ~!xs ~i (~rf)))))
                                              ([~!xs] (~f ~@(for [[i rf] rfs-i] `(~rf (get ~!xs ~i)))))
                                              ([~!xs x'##]
                                                ~@(for [[i rf] rfs-i] `(assoc! ~!xs ~i (~rf (get ~!xs ~i) x'##)))))))))
                       (fn [[f & rfs] rest-rfs]
                         (unify-gensyms
                          `(let [rfs-ct# (+ ~(count rfs) (count ~rest-rfs))
                                 rfs##   (->objects rfs-ct#)
                                 _#      (do ~@(for [[i rf] (map-indexed vector rfs)]
                                                 `(assoc! rfs## ~i ~rf)))
                                 _#      (dotimes [i# rfs-ct#] (assoc! rfs## i# (get ~rest-rfs i#)))]
                             (fn ([        ] (let [!xs# (->objects rfs-ct#)]
                                               (dotimes [i# rfs-ct#] (assoc! !xs# i# (get rfs## i#)))
                                               !xs#))
                                 ([~!xs    ] (dotimes [i# rfs-ct#]
                                               (assoc! ~!xs i# ((get rfs## i#) (get ~!xs i#))))
                                             (apply ~f ~!xs))
                                 ([~!xs x'#] (dotimes [i# rfs-ct#]
                                               (assoc! ~!xs i# ((get rfs## i#) (get ~!xs i#) x'#))))))))
                       2 3
                       (fn [i] (if (= i 0) "f" (str "rf" (dec i))))))))

#?(:clj (defmacro gen-multiplex [] (gen-multiplex* (env-lang))))

(gen-multiplex)

#?(:clj (defalias join      ccoll/join   ))
#?(:clj (defalias joinl'    ccoll/joinl' ))
#?(:clj (defalias join'     ccoll/join'  ))
        (defalias pjoin     fold/pjoin   )
        (defalias pjoin'    fold/pjoin'  )
#?(:clj (defalias reduce    red/reduce   ))
#?(:clj (defalias transduce red/transduce))
        (defalias fold*     fold/fold    ) ; "fold*" to avoid clash of namespace quantum.core.reducers.fold with var quantum.core.reducers/fold

(defn red-apply
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

(defn reduce-sentinel
  "Calls `reduce` with a sentinel.
   Useful for e.g. `max` and `min`."
  {:attribution "alexandergunnarson"}
  [rf xs]
  (red-apply (aritoid (fn' nil) identity rf) xs))

(defn first-non-nil-reducer
  "A reducing function that simply returns the first non-nil element in the
  collection."
  {:source "tesser.utils"}
  [_ x] (when-not (nil? x) (reduced x)))

;___________________________________________________________________________________________________________________________________
;=================================================={    transduce.reducers    }=====================================================
;=================================================={                          }=====================================================
(defcurried map-state
  "Like map, but threads a state through the sequence of transformations.
  For each x in coll, f is applied to [state x] and should return [state' x'].
  The first invocation of f uses init as the state."
  {:attribution 'transduce.reducers
   :todo        #{"Make a single-threaded version with mutable fields"}}
  [f init xs]
  (transformer xs
    (fn [f1]
      (let [state (atom init)]
        (fn [acc x]
          (let [[state' x'] (f @state x)]
            (reset! state state')
            (f1 acc x')))))))

(defalias reduce-count ccoll/reduce-count)

(defn fold-count
  {:attribution "parkour.reducers"}
  [xs] (fold* (aritoid + identity +) ccoll/count:rf xs))
;___________________________________________________________________________________________________________________________________
;=================================================={           CAT            }=====================================================
;=================================================={                          }=====================================================
(defn cat:transducer [rf]
  (let [rrf (preserving-reduced rf)]
    (fn
      ([] (rf))
      ([ret] (rf ret))
      ([ret x] (reduce rrf ret x))
      ([ret k v] (reduce rrf ret [k v]))))) ; TODO is this arity right?

(defn cat+ [xs] (transformer xs cat:transducer))

(defn foldcat+
  "Equivalent to `(fold cat+ conj! xs)`"
  {:adapted-from "clojure.core.reducers"}
  [xs] (fold* cat+ (fn&2 conj!) xs))
;___________________________________________________________________________________________________________________________________
;=================================================={           MAP            }=====================================================
;=================================================={                          }=====================================================
(defn map:transducer [f]
  (fn [rf]
    (fn ; TODO auto-generate?
      ([]                  (rf))
      ([ret]               (rf ret))
      ([ret x0]            (rf ret       (f x0)))
      ([ret x0 x1]         (rf ret       (f x0 x1)))
      ([ret x0 x1 x2]      (rf ret       (f x0 x1 x2)))
      ([ret x0 x1 x2 & xs] (rf ret (apply f x0 x1 x2 xs))))))

(def map+ (transducer->transformer 1 map:transducer))

; ----- MAP-INDEXED ----- ;

(defn map-indexed:transducer-base
  [f !box inc!f]
  (fn [rf]
    (let [*i (!box -1)]
      (aritoid rf rf (fn [ret x] (rf ret (f (inc!f *i) x)))))))

(defn !map-indexed:transducer
  "Like the transducer of `core/map-indexed`, but uses a mutable variable internally
   instead of a `volatile`.
   As the name suggests, this transducer is not thread-safe."
  [f] (map-indexed:transducer-base f (fn [^long i] (! i)) inc!))

(def !map-indexed+ (transducer->transformer 1 !map-indexed:transducer))

(defn v!map-indexed:transducer
  "Same as the transducer of `core/map-indexed`, but uses a typed volatile to avoid autoboxing."
  [f] (map-indexed:transducer-base f (fn [^long i] (volatile i)) inc!:volatile)) ; TODO use typed volatile

(def v!map-indexed+ (transducer->transformer 1 v!map-indexed:transducer))

(defn map-indexed:transducer
  "Like the transducer of `core/map-indexed`, but uses an `AtomicLong` internally
   instead of a `volatile`."
  [f] (map-indexed:transducer-base f (fn [^long i] (atom* i)) inc!:atom*))

(def map-indexed+ (transducer->transformer 1 map-indexed:transducer))

; TODO pmap-indexed+

; ----- INDEXED ----- ;

; TODO e.g. !indexed:objects+
(defn !indexed+  [xs] (!map-indexed+  vector xs))
(defn v!indexed+ [xs] (v!map-indexed+ vector xs))
(defn indexed+   [xs] (map-indexed+   vector xs))

; TODO pindexed+
;___________________________________________________________________________________________________________________________________
;=================================================={          MAPCAT          }=====================================================
;=================================================={                          }=====================================================
(def mapcat+ (transducer->transformer 1 core/mapcat))

(defn concat+ [& args] (mapcat+ identity args))
;___________________________________________________________________________________________________________________________________
;=================================================={        REDUCTIONS        }=====================================================
;=================================================={                          }=====================================================
(defn map-accum:transducer
  {:attribution "alexandergunnarson"}
  [f] (fn [rf] (aritoid rf rf (fn [ret x] (rf ret (f ret x))))))

(def ^{:doc "Like `map+`, but the accumulated reduction gets passed through as the
            first argument to `f`, and the current element as the second argument."}
  map-accum+ (transducer->transformer 1 map-accum:transducer))

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
(defn filter:transducer [pred]
  (fn [rf]
    (aritoid rf rf
      (fn [ret x]   (if (pred x)   (rf ret x)   ret))
      (fn [ret k v] (if (pred k v) (rf ret k v) ret)))))

(def ^{:doc "Returns a version of the folder which only passes on inputs to subsequent
             transforms when `(pred <input>)` is truthy."}
  filter+ (transducer->transformer 1 filter:transducer))

; ----- FILTER-INDEXED ----- ;

(defn filter-indexed:transducer-base
  [pred !box inc!f]
  (fn [rf]
    (let [*i (!box -1)]
      (aritoid rf rf (fn [ret x] (if (pred (inc!f *i) x) (rf ret x) ret))))))

(defn !filter-indexed:transducer
  [f] (filter-indexed:transducer-base f (fn [^long i] (! i)) inc!))

(def ^{:doc "map+ : filter+ :: !map-indexed+ : !filter-indexed+"}
  !filter-indexed+ (transducer->transformer 1 !filter-indexed:transducer))

(defn v!filter-indexed:transducer
  [f] (filter-indexed:transducer-base f (fn [^long i] (volatile i)) inc!:volatile)) ; TODO use typed volatile

(def ^{:doc "map+ : filter+ :: v!map-indexed+ : v!filter-indexed+"}
  v!filter-indexed+ (transducer->transformer 1 v!filter-indexed:transducer))

(defn filter-indexed:transducer
  [f] (filter-indexed:transducer-base f (fn [^long i] (atom* i)) inc!:atom*))

(def filter-indexed+ (transducer->transformer 1 filter-indexed:transducer))

; TODO pfilter-indexed+

; ----- REMOVE ----- ;

(defn remove+
  "Returns a version of the folder which only passes on inputs to subsequent
   transforms when `(pred <input>)` is falsey."
  ([pred]    (filter+ (complement pred))) ; TODO `fn-not`
  ([pred xs] (filter+ (complement pred) xs))) ; TODO `fn-not`

; ----- REMOVE-INDEXED ----- ;

(defn !remove-indexed+
  "map+ : remove+ :: !map-indexed+ : !remove-indexed+"
  ([pred]    (!filter-indexed+ (comp not pred))) ; TODO use `fn-not` here
  ([pred xs] (!filter-indexed+ (comp not pred) xs))) ; TODO use `fn-not` here

(defn v!remove-indexed+
  "map+ : remove+ :: v!map-indexed+ : v!remove-indexed+"
  ([pred]    (v!filter-indexed+ (comp not pred))) ; TODO use `fn-not` here
  ([pred xs] (v!filter-indexed+ (comp not pred) xs))) ; TODO use `fn-not` here

(defn remove-indexed+
  "map+ : remove+ :: map-indexed+ : remove-indexed+"
  ([pred]    (filter-indexed+ (comp not pred))) ; TODO use `fn-not` here
  ([pred xs] (filter-indexed+ (comp not pred) xs))) ; TODO use `fn-not` here

(def keep+         (transducer->transformer 1 core/keep))
(def v!keep-indexed+ (transducer->transformer 1 core/keep-indexed)) ; TODO add non-volatile forms
(defn keep-indexed+ [& args] (TODO))

;___________________________________________________________________________________________________________________________________
;=================================================={         FLATTEN          }=====================================================
;=================================================={                          }=====================================================
(declare flatten+)

(defn flatten:transducer []
  (fn [rf]
    (fn ([] (rf))
        ([ret v]
           (if (sequential? v) ; TODO use `t/sequential?` ?
               (reduce rf ret (flatten+ v))
               (rf ret v))))))

(def flatten+ (transducer->transformer 0 flatten:transducer))
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
    (transformer
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
    (transformer
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
  (transformer
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
  ([^default end                       ] (transformer (Range.     0     end 1   ) identity-rf))
  ([^long    end                       ] (transformer (LongRange. 0     end 1   ) identity-rf))
  ; TODO fix types here
  ([^default start       end           ] (transformer (Range.     start end 1   ) identity-rf))
  ([^long    start ^long end           ] (transformer (LongRange. start end 1   ) identity-rf))
  ; TODO fix types here
  ([^default start       end       step] (transformer (Range.     start end step) identity-rf))
  ([^long    start ^long end ^long step] (transformer (LongRange. start end step) identity-rf)))

#?(:clj
(defmacro range+
  ([] `(iterate+ ~(case-env :cljs `inc `inc') 0))
  ([& args] `(range+* ~@args))))
;___________________________________________________________________________________________________________________________________
;=================================================={     TAKE, TAKE-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defalias take+ ccoll/take+) ; TODO mark this as mutable

(def take-while+ (transducer->transformer 1 core/take-while))

(def v!take-nth+   (transducer->transformer 1 core/take-nth)) ; TODO non-volatile forms
(defn take-nth+ [& args] (TODO))

#?(:clj (defalias taker+ ccoll/taker+))
;___________________________________________________________________________________________________________________________________
;=================================================={     DROP, DROP-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defalias drop+ ccoll/drop+)

(def drop-while+ (transducer->transformer 1 core/drop-while))

#?(:clj (defalias dropr+ ccoll/dropr+))
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
(defn reduce-by+
  "Partition `coll` with `kf` as per `partition-by`, then reduce
  each partition with `f` and optional initial value `init` as per
  `reduce`."
  {:attribution "parkour.reducers"}
  ([kf f xs]
    (->> (mapcat+ identity [xs [sentinel]])
         (map-state
           (fn [[k acc] x]
             (if (identical? sentinel x)
               [nil acc]
               (let [k' (kf x)]
                 (if (or (= k k') (identical? sentinel k))
                   [[k' (f acc x)] sentinel]
                   [[k' (f (f) x)] acc]))))
           [sentinel (f)])
         (remove+ (partial identical? sentinel))))
  ([kf f init xs]
     (let [f (fn ([] init) ([acc x] (f acc x)))]
       (reduce-by+ kf f xs))))

(def partition-by+ (transducer->transformer 1 core/partition-by))

; TODO conform to `group-by-into`
; (group-by-into init kf (aritoid vector nil conj) xs)

(defn !partition-into:transducer-base [all? ^long n combinef]
  (fn [rf]
    (let [!chunk-temp (combinef)] ; this could in theory be an atomic, in which case `empty?` and `count` would need to be adjusted
      (fn
        ([] (rf))
        ([ret]
          (rf (if (or (not all?) (empty? !chunk-temp))
                  ret
                  (let [chunk (combinef !chunk-temp)]
                    (unreduced (rf ret chunk))))))
        ([ret x]
          (combinef !chunk-temp x)
          (if (= n (count !chunk-temp))
              (let [chunk (combinef !chunk-temp)]
                (rf ret chunk))
              ret))))))

(defn !partition-?all:transducer [all? ^long n]
  (!partition-into:transducer-base all? n
    (fn ([] (java.util.ArrayList. n))
        ([^java.util.ArrayList xs] (with-do (vec (.toArray xs)) (.clear xs)))
        ([^java.util.ArrayList xs x] (conj! xs x)))))

; ----- PARTITION(-INTO)? ----- ;

(defn !partition-into:transducer [n genf combinef]
  (!partition-into:transducer-base false n
    (aritoid (fn [] (genf n)) combinef combinef)))

(defn !partition:transducer [n] (!partition-?all:transducer false n))

(def !partition+ (transducer->transformer 1 !partition:transducer))

(defn partition+ [& args] (TODO))

; ----- PARTITION-ALL(-INTO)? ----- ;

(defn !partition-all-into:transducer [n genf combinef]
  (!partition-into:transducer-base true n
    (aritoid (fn [] (genf n)) combinef combinef)))

(defn !partition-all:transducer [n] (!partition-?all:transducer true n))

(def !partition-all-into+ (transducer->transformer 3 !partition-all-into:transducer))

(defn partition-all-into+ [& args] (TODO))

(def !partition-all+ (transducer->transformer 1 !partition-all:transducer))

(defn partition-all+ [& args] (TODO))

; TODO partition-all-into-timeout
#?(:clj
(defn !partition-all-timeout:transducer [^long n ^long timeout-ms]
  (fn [rf]
    (let [a                (java.util.ArrayList. n)
          *last-aggregated (! Long/MAX_VALUE)] ; to ensure that aggregation doesn't happen immediately
      (fn
        ([] (rf))
        ([ret]
          (rf (if (empty? a)
                  ret
                  (let [v (vec (.toArray a))]
                    ;; clear first!
                    (.clear a)
                    (unreduced (rf ret v))))))
        ([ret x]
          (conj! a x)
          (let [now    (System/currentTimeMillis)
                before (deref *last-aggregated)]
            (if (or (= n (.size a))
                    (>= (- now before) timeout-ms))
              (let [_ (reset! *last-aggregated now)
                    v (vec (.toArray a))]
                (.clear a)
                (rf ret v))
              ret))))))))

; TODO do non-single-threaded version
#?(:clj
(def
  ^{:doc "Partitions `xs` into chunks of `n`, unless the timer specified by `timeout-ms`
          expires before there are at least `n` elements available, in which case a
          partition smaller than `n` will happen and the timer will be reset, and so on.

          Useful for `chan` when one might want to aggregate results into chunks of no
          more than `n`, at least every `timeout-ms`.

          This transformer is not thread-safe."}
  !partition-all-timeout+
  (transducer->transformer 2 !partition-all-timeout:transducer)))

(defn partition-all-timeout+ [& args] (TODO))

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
(defn v!dedupe-by:transducer [kf]
  (fn abcde [rf]
    (let [*prior (volatile! ::none)]
      (aritoid rf rf
        (fn defgh [result input]
          (let [prior  @*prior
                input' (kf input)]
            (vreset! *prior input')
            (if (= prior input')
                result
                (rf result input))))))))

(def v!dedupe-by+ (transducer->transformer 1 v!dedupe-by:transducer))

(defn v!dedupe:transducer
  [] (v!dedupe-by:transducer identity))

(def v!dedupe+ (transducer->transformer 0 v!dedupe:transducer))

; TODO compare this to clojure/core `dedupe`, and an impl of it using atoms
(defn dedupe+
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

; TODO default to using a `HashSet` internally ? Other options?
; TODO do volatile and unsync-mutable versions
(defn distinct-by-storing:transducer
  "Like `core/distinct`, but you can choose what collection to store the distinct items in."
  ([kf] (distinct-by-storing:transducer kf
          (fn [] (atom #{}))))
  ([kf genf]
    (distinct-by-storing:transducer kf genf
      (fn [seen x] (contains? @seen x))))
  ([kf genf contains?f]
    (distinct-by-storing:transducer kf genf contains?f
      (fn [seen x] (swap! seen conj x))))
  ([kf genf contains?f conj!f]
    (fn [rf]
      (let [seen (genf)]
        (fn ([] (rf))
            ([ret] (rf ret))
            ([ret x]
             (let [x' (kf x)]
               (if (contains?f seen x')
                   ret
                   (do (conj!f seen x')
                       (rf ret x))))))))))

(defn distinct-by-storing+
  ([kf genf                  ] (distinct-by-storing:transducer kf genf))
  ([kf genf contains?f       ] (distinct-by-storing:transducer kf genf contains?f))
  ([kf genf contains?f conj!f] (distinct-by-storing:transducer kf genf contains?f conj!f))
  ([kf genf contains?f conj!f xs] (transformer xs (distinct-by-storing+ kf genf contains?f conj!f))))

(defn distinct-by:transducer [kf] (distinct-by-storing:transducer kf))

(def distinct-by+ (transducer->transformer 1 distinct-by:transducer))

(defn distinct-storing+
  ([genf                  ] (distinct-by-storing+ identity genf))
  ([genf contains?f       ] (distinct-by-storing+ identity genf contains?f))
  ([genf contains?f conj!f] (distinct-by-storing+ identity genf contains?f conj!f))
  ([genf contains?f conj!f xs] (transformer xs (distinct-storing+ genf contains?f conj!f))))

(defn distinct:transducer [] (distinct-storing:transducer identity))

(def distinct+ (transducer->transformer 0 distinct:transducer))

(def replace+ (transducer->transformer 1 core/replace))

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

(def interpose+ (transducer->transformer 1 core/interpose))

(defalias zipvec+ interpose+)
;___________________________________________________________________________________________________________________________________
;=================================================={ LOOPS / LIST COMPREHENS. }=====================================================
;=================================================={        for, doseq        }=====================================================
; TODO all of `for+` can be reduced to `map+`
; TODO all of `fori+` can be reduced to `map-indexed+`

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
   `(transformer ~(last bindings) ~(for+:gen-f f f-inner arities-2-3)))))

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
   `(transformer ~(-> bindings butlast last)
      (let [~i* (~atomic-container -1)] ~(for+:gen-f f f-inner arities-2-3))))))

#?(:clj (defmacro !fori+ "Like `for+`, but indexed."               [& args] `(fori+* volatile! vswap! ~@args))) ; TODO use unsynchronized mutable counter
#?(:clj (defmacro fori+  "Like `for+`, but thread-safely indexed." [& args] `(fori+* atom       swap! ~@args)))
; TODO `pfor+`

#?(:clj
(defmacro doseq+
  "|doseq| but based on reducers."
  {:attribution "Christophe Grand, https://gist.github.com/cgrand/5643767"}
  [bindings & body]
 `(red/reduce fn-nil (for+ ~bindings (do ~@body)))))
; TODO `doseqi+`
; TODO `pdoseqi+` etc

(def sample+ (transducer->transformer 1 core/random-sample))

(defalias random-sample+ sample+) ; TODO allow to use a different source of randomness

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
