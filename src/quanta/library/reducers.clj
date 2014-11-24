(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support. See Clojure's pom.xml for the
      dependency info."
      :author       "Rich Hickey"
      :contributors "Alan Malloy, Alex Gunnarson"}
  quanta.library.reducers
  (:gen-class))
(set! *warn-on-reflection* true)
(require
  '[quanta.library.ns               :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[clojure.walk :as walk]
  '[quanta.library.function         :as fn    :refer :all]
  '[quanta.library.logic            :as log   :refer :all]
  '[quanta.library.data.vector      :as vec   :refer [subvec+ catvec]]
  '[quanta.library.type             :as type  :refer :all]
  '[quanta.library.data.map         :as map   :refer [map-entry sorted-map+ merge+]]
  '[quanta.library.data.set         :as set])
(alias 'core 'clojure.core)


; preduce+
;___________________________________________________________________________________________________________________________________
;=================================================={          TODO            }=====================================================
;=================================================={                          }=====================================================
; /repeat/              http://dev.clojure.org/jira/browse/CLJ-994
; dependent on: /range/ http://dev.clojure.org/jira/browse/CLJ-993
; /keep-indexed/
;___________________________________________________________________________________________________________________________________
;=================================================={      MULTIREDUCIBLES     }=====================================================
;=================================================={  with support for /fold/ }=====================================================
; From wagjo, https://gist.github.com/wagjo/10017343 - Apr 7, 2014
(comment
  (def s1 "Hello World")
  (def s2 "lllllllllll")
  ;; create multireducible with zip
  (seq (zip s1 s2))
  ;; => ((\H \l) (\e \l) (\l \l) (\l \l) (\o \l) (\space \l) (\W \l) (\o \l) (\r \l) (\l \l) (\d \l))
   ;; /indexed/ is an alias for (zip (range) coll)
  (def indexed (partial zip (range))) ; my own code
  (seq (indexed s1))
  ;; => ((0 \H) (1 \e) (2 \l) (3 \l) (4 \o) (5 \space) (6 \W) (7 \o) (8 \r) (9 \l) (10 \d))
   
  ;; you can have more than 2 collections in the multireducible
  (reduce conj [] (indexed s1 s2))
  ;; => [(0 \H \l) (1 \e \l) (2 \l \l) (3 \l \l) (4 \o \l) (5 \space \l) (6 \W \l) (7 \o \l) (8 \r \l) (9 \l \l) (10 \d \l)]
   
  ;; reduce can work on tuples (as seen above), or can work on individual elements
  (reduce conj [] (unpacked (indexed s1 s2)))
  ;; => [0 \H \l 1 \e \l 2 \l \l 3 \l \l 4 \o \l 5 \space \l 6 \W \l 7 \o \l 8 \r \l 9 \l \l 10 \d \l]
   
  ;; lets find index where both strings have same character
  (let [s (unpacked (indexed s1 s2))
        f (fn [index ch1 ch2] (when (= ch1 ch2) index))]
    (into [] (keep f s)))
  ;; => [2 3 9]
   
  ;; some multireducibles also support folding, both with tuples and individual elements
  (let [s (unpacked (indexed s1 s2))
        combinef (fn ([] []) ([a b] [a b]))
        reducef (fn [val index ch1 ch2] (if (= ch1 ch2) (conj val index) val))]
    (fold+ 5 combinef reducef s))
  ;; => [[2 3] [[] [9]]]
   
  ;; maps can behave like multireducibles too
  (reduce conj [] (unpacked {:foo 1 :bar 2 :baz 3}))
  ;; => [:baz 3 :bar 2 :foo 1]
   
  ;; and did I mention multireducibles are very fast?
  (time (let [mr (indexed (range -1000000 1000000)
                          (range 1000000 -1000000 -1))
              f (fn [index v1 v2] (when (== v1 v2) index))]
          (into [] (keep f (unpacked mr))))) ;; => [1000000] "Elapsed time: 356.012467 msecs" ; USE KEEP+
   ;;* patch to clojure is needed to support this feature
  ; from https://gist.github.com/wagjo/b687158ab0ff6aa8cb33 -  May 2, 2014
  (defn fold-sectionable
    "Perform fold on sectionable collection."
    ([coll pool n combinef reducef reduce-mode]
       (fold-sectionable coll pool n combinef reducef
                         reduce-mode section)) ; SECTION???
    ([coll pool n combinef reducef reduce-mode section]
       (fold-sectionable coll pool n combinef reducef
                         reduce-mode section count))
    ([coll pool n combinef reducef reduce-mode section count]
       (let [cnt    (count coll)
             reduce (reduce-from-mode reduce-mode)
             n (max 1 n)]
         (cond
          (empty? coll) (combinef)
          (<= cnt n) (reduce reducef (combinef) coll)
          :else
          (let [split (quot cnt 2)
                v1 (section coll 0 split)
                v2 (section coll split cnt)
                fc (fn [child]
                     #(-fold child pool n combinef reducef
                             reduce-mode))]
            (invoke pool
                    #(let [f1 (fc v1)
                           t2 (fork (fc v2))]
                       (combinef (f1) (join t2)))))))))
   
  (deftype ZipIterator [^:unsynchronized-mutable iters]
    IOpenAware
    (-open? [this] (every? open? iters)) ; open?
    IReference
    (-deref [this] (apply tuple (map deref iters))) ; tuple
    IIterator
    (-next! [this] (set! iters (seq (map next! iters))) this)) ; next!
  
  (deftype FoldableZip [colls]
    IRed
    (-reduce [this f init]
      (loop [iters (seq (map iterator colls))
             val init]
        (if (every? open? iters)
          (reduced-let [ret (f val (apply tuple (map deref iters)))]
            (recur (seq (map next! iters)) ret))
          val)))
    IIterable
    (-iterator [this]
      (->ZipIterator (seq (map iterator colls))))
    IUnpackedRed
    (-reduce-unpacked [this f init]
      (loop [iters (seq (map iterator colls))
             val init]
        (if (every? open? iters)
          (reduced-let [ret (apply f val (map deref iters))]
            (recur (seq (map next! iters)) ret))
          val)))
    IFoldable
    (-fold [coll pool n combinef reducef reduce-mode]
      (fold-sectionable coll pool n combinef reducef reduce-mode))
    ISectionable
    (-section [this new-begin new-end]
      (let [l (count this)
            new-end (prepare-ordered-section new-begin new-end l)]
        (if (and (zero? new-begin) (== new-end l))
          this
          (->FoldableZip (map #(section % new-begin new-end) colls)))))
    ICounted
    (-count [this] (apply min (map count colls)))))
;___________________________________________________________________________________________________________________________________
;=================================================={         REDUCERS         }=====================================================
;=================================================={     reducer, folder      }=====================================================
; No reducer delays anything
; In-place optimizers instead of sequential transformations? ; Hello, reducers! :)

; By directly combining functions via function transformations,
; reducers avoid unnecessary allocations for intermediate operations, and can improve JVM JIT performance.
; Creating a reducer holds the head of the collection in a closure.
; Thus, a huge lazy-seq can be tied up memory as it becomes realized.

; So you can’t mix and match transformations on sequences and on reducers when you care for laziness. Or, wait! - maybe you can!
; Reducers are just a clever way to compose big functions while giving the user the illusion of manipulating collections. 

; WHAT IT IS
; A parallel execution framework for extremely efficient parallel processing. It exploits the internal tree-like data structures
; of the data being reduced.
; Regular sequences are inherently sequential. Their performant operation is to pull items from the beginning one at a time, so
; it’s difficult to efficiently distribute work across their members. However, Reducers is aware of the internal structure of
; Clojure’s persistent data structures and can leverage that to efficiently distribute worker processes across the data.
; ; Rich Hickey says: Lseqs vs. reducers: a complementary set of fundamental operations that tradeoff laziness for parallelism.

; WITH ARRAYS
; If you are dealing with numerical arrays then you will almost certainly be better off using something like core.matrix instead.
; Or, hip-hip (array!)
;___________________________________________________________________________________________________________________________________
;=================================================={      LAZY REDUCERS       }=====================================================
;=================================================={                          }=====================================================
; Sometimes in a seq pipeline, you know that some intermediate results are, well,
; intermediate and as such don’t need to be persistent but, on the whole, you still need the laziness.
(defn- reverse-conses 
  ^{:attribution "Christophe Grand, http://clj-me.cgrand.net/2013/02/11/from-lazy-seqs-to-reducers-and-back/"}
  ([s tail] 
    (if (identical? (rest s) tail)
      s
      (reverse-conses s tail tail)))
  ([s from-tail to-tail]
    (loop [f s b to-tail]
      (if (identical? f from-tail)
        b
        (recur (rest f) (cons (first f) b))))))
(defn seq-seq
  "USAGE:
    (seq->> (range) (map+ str) (take+ 25) (drop+ 5))
  (\"5\" \"6\" \"7\" \"8\" \"9\" \"10\" \"11\" \"12\" \"13\" \"14\" \"15\" \"16\" 
   \"17\" \"18\" \"19\" \"20\" \"21\" \"22\" \"23\" \"24\")"
  ^{:attribution "Christophe Grand, http://clj-me.cgrand.net/2013/02/11/from-lazy-seqs-to-reducers-and-back/"}
  [f s] 
  (let [f1 (reduce #(cons %2 %1) nil ; Note that the captured function (f1) may be impure, so don’t share it!
              (f (reify clojure.core.protocols.CollReduce
                   (coll-reduce [this f1 init]
                     f1))))]
    ((fn this [s]
       (lazy-seq 
         (when-let [s (seq s)]
           (let [more (this (rest s)) 
                 x (f1 more (first s))]
             (if (reduced? x)
               (reverse-conses @x more nil)
               (reverse-conses x more)))))) s)))
(defmacro lseq->>
  ^{:attribution "Christophe Grand, http://clj-me.cgrand.net/2013/02/11/from-lazy-seqs-to-reducers-and-back/"}
  [s & forms] 
  `(seq-seq (fn [n#] (->> n# ~@forms)) ~s))
(defn seq-once
  "Returns a sequence on which seq can be called only once.
   Use when you don't want to keep the head of a lazy-seq while using reducers.
   Shouldn't be necessary with the fix starting around line 60."
  ^{:attribution "Christophe Grand, http://stackoverflow.com/questions/22031829/tuning-clojure-reducers-library-performace"}
  [coll]
  (let [a (atom coll)]
    (reify clojure.lang.Seqable
      (seq [_]
        (let [coll @a]
          (reset! a nil)
          (seq coll))))))
;___________________________________________________________________________________________________________________________________
;=================================================={        FORK/JOIN         }=====================================================
;=================================================={                          }=====================================================
(defmacro ^:private compile-if
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to
  `then`.  Else expand to `else`.

  (compile-if (Class/forName \"java.util.concurrent.ForkJoinTask\")
    (do-cool-stuff-with-fork-join)
    (fall-back-to-executor-services))"
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))
(compile-if
 (Class/forName "java.util.concurrent.ForkJoinTask")
 ; Running a JDK >= 7
 (do
   (def pool (delay (java.util.concurrent.ForkJoinPool.)))
   (defn fjtask [^Callable f]
     (java.util.concurrent.ForkJoinTask/adapt f))
   (defn- fjinvoke [f]
     (if (java.util.concurrent.ForkJoinTask/inForkJoinPool)
       (f)
       (.invoke ^java.util.concurrent.ForkJoinPool @pool ^java.util.concurrent.ForkJoinTask (fjtask f))))
   (defn- fjfork [task] (.fork ^java.util.concurrent.ForkJoinTask task))
   (defn- fjjoin [task] (.join ^java.util.concurrent.ForkJoinTask task)))
 ; Running a JDK < 7
 (do
   (def pool (delay (jsr166y.ForkJoinPool.)))
   (defn fjtask [^Callable f]
     (jsr166y.ForkJoinTask/adapt f))
   (defn- fjinvoke [f]
     (if (jsr166y.ForkJoinTask/inForkJoinPool)
       (f)
       (.invoke ^jsr166y.ForkJoinPool @pool ^jsr166y.ForkJoinTask (fjtask f))))
   (defn- fjfork [task] (.fork ^jsr166y.ForkJoinTask task))
   (defn- fjjoin [task] (.join ^jsr166y.ForkJoinTask task))))
;___________________________________________________________________________________________________________________________________
;=================================================={      FUNCTIONEERING      }=====================================================
;=================================================={      incl. currying      }=====================================================
(defn- do-rfn [f1 k fkv]
  `(fn
     ([] (~f1))
     ~(clojure.walk/postwalk
       #(if (sequential? %)
          ((if (vector? %) vec identity)
           (remove #{k} %))
          %)
       fkv)
     ~fkv))
(defmacro ^:private rfn
  "Builds 3-arity reducing fn given names of wrapped fn and key, and k/v impl."
  ^{:attribution "clojure.core.reducers"}
  [[f1 k] fkv]
  (do-rfn f1 k fkv))
(defn- reduce-impl
  "Creates an implementation of CollReduce using the given reducer.
  The two-argument implementation of reduce will call f1 with no args
  to get an init value, and then forward on to your three-argument version."
  ^{:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  [reducer-n]
  {:coll-reduce
    (fn ([coll f1     ] (reducer-n coll f1 (f1)))
        ([coll f1 init] (reducer-n coll f1 init)))})
;___________________________________________________________________________________________________________________________________
;=================================================={          REDUCE          }=====================================================
;=================================================={                          }=====================================================
; reduce-reverse [f init o] - like reduce but in reverse order

; /reduce/ vs. /apply/?
; (response by Michal Marczyk)
; /reduce/ might shave off a fraction of a blink of an eye in a lot of the common cases. 
; On the other hand, a complex function might take advantage of some optimization opportunities which aren't general
; enough to be built into reduce; then apply would let you take advantage of those while reduce might actually slow you down.
; A good example is provided by /str/: it uses a StringBuilder internally and will benefit significantly from the use of
; apply rather than reduce.
; Use /apply/ when in doubt.
; Processes each element in a sequence and builds a result.
; Applies a function to each value in the sequence and the running result to accumulate a final value.
; (reduce +    [1 2 3 4]) ; 10 ; (+ (+ (+ 1 2) 3) 4)
; (reduce + 15 [1 2 3 4]) ; 25 ; Reduce also takes an optional initial value

(defn reduce+
  "Like core/reduce except:
     When init is not provided, (f) is used.
     Maps are reduced with reduce-kv."
  ^{:attribution "clojure.core.reducers"}
  ([f coll] (reduce+ f (f) coll))
  ([f init coll]
    (if (instance? java.util.Map coll)
        (do ; (println "java.util.Map kv-reduce! class:" (class coll))
            (try (clojure.core.protocols/kv-reduce   coll f init)
              (catch Exception e
                (do (println "There was an exception in kv-reduce!")
                    (clojure.stacktrace/print-stack-trace e)
                    (throw (Exception. "Broke out of kv-reduce"))))))
        (clojure.core.protocols/coll-reduce coll f init))))
(defn reducei+
  "|reduce|, indexed"
  [^AFunction f ret coll]
  (let [n (volatile! -1)]
    (reduce+
      (fn [ret-n elem]
        (vswap! n inc)
        (f ret-n elem @n))
      ret coll)))
(defn reducem+
  "Requires only one argument for preceding functions in its call chain."
  {:attribution "Alex Gunnarson"
   :performance "9.94 ms vs. 17.02 ms for 10000 calls to (into+ {}) for small collections ;
           This is because the |transient| function deals a performance hit."}
  [coll]
  (->> coll force
       (reduce+
         (fn [ret k v]
           (assoc ret k v))
         {}))) 

(defn count* ; count implemented in terms of reduce
  ; /count/ is 71.542581 ms, whereas
  ; /count*/ is 36.824665 ms - twice as fast!! :D wow! that's amazing!
  ^{:attribution "parkour.reducers"}
  [coll]
  (reduce+ (comp inc firsta) 0 coll))
; (in-ns 'clj-qb.req-gen)
(defn reducer+
  "Given a reducible collection, and a transformation function xf,
  returns a reducible collection, where any supplied reducing
  fn will be transformed by xf. xf is a function of reducing fn to
  reducing fn."
  {:added "1.5"}
  ([coll xf]
     (reify
      clojure.core.protocols/CollReduce
      (coll-reduce [this f1]
                   (clojure.core.protocols/coll-reduce this f1 (f1)))
      (coll-reduce [_ f1 init]
                   (clojure.core.protocols/coll-reduce coll (xf f1) init)))))

; Fixing it so the seqs are headless.
; Christophe Grand - https://groups.google.com/forum/#!searchin/clojure-dev/reducer/clojure-dev/t6NhGnYNH1A/2lXghJS5HywJ
; (defrecord Reducer [coll xf])
; (extend-protocol clojure.core.protocols/CollReduce
;   Reducer
;     (coll-reduce [r f1]
;       (clojure.core.protocols/coll-reduce r f1 (f1)))
;     (coll-reduce [r f1 init]
;       (clojure.core.protocols/coll-reduce (:coll r) ((:xf r) f1) init)))
; (def rreducer ->Reducer) ; I think ->___ means "new record"
; (defn rmap [f coll]
;   (rreducer coll
;     (fn [g] 
;       (fn [acc x]
;         (g acc (f x))))))
;___________________________________________________________________________________________________________________________________
;=================================================={     FOLDING FUNCTIONS    }=====================================================
;=================================================={       (Generalized)      }=====================================================
(defprotocol CollFold
  (coll-fold [coll n combinef reducef]))
; Alan Malloy generalized foldvec to work for anything you can split in half, and used that implementation for Range.
; There's now a generic fold-by-halves that can be used by anything that
; can be split in half.
; (in-ns 'quanta.library.reducers)
(defn- fold-by-halves
  "Folds the provided collection by halving it until it is smaller than the
  requested size, and folding each subsection. halving-fn will be passed as
  input a collection and its size (so you need not recompute the size); it
  should return the left and right halves of the collection as a pair. Those
  halves will normally be of the same type as the parent collection, but
  anything foldable is sufficient."
  ^{:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  [halving-fn coll n combinef reducef]
  (let [size (count coll)]
    (cond
      (= 0 size)
      (combinef)
      (<= size n)
      (reduce+ reducef (combinef) coll)    
      :else
      (let [[left right] (halving-fn coll size)
            child-fn (fn [child] #(coll-fold child n combinef reducef))]
        (fjinvoke
         #(let [f1 (child-fn left)
                t2 (fjtask (child-fn right))]
            (fjfork t2)
            (combinef (f1) (fjjoin t2))))))))
; (in-ns 'clj-qb.req-gen)
;___________________________________________________________________________________________________________________________________
;=================================================={           FOLD           }=====================================================
;=================================================={           into           }=====================================================
(extend-protocol CollFold ; clojure.core.reducers
  nil
    (coll-fold [coll n combinef reducef]
      (combinef))
  Object
    (coll-fold [coll n combinef reducef]
      ; (println "Object Fold!" "Class:" (class coll))
      ; Can't fold - single reduce
      (reduce+ reducef (combinef) coll))
  clojure.lang.IPersistentVector
    (coll-fold [coll n combinef reducef]
      ; (println "Vector Fold!" "Class:" (class coll))
      (fold-by-halves
        (fn [coll-0 ct]
          (let [split-ind (quot ct 2)]
            [(subvec+ coll-0 0 split-ind) ; test subvec against subvec+
             (subvec+ coll-0   split-ind ct)]))
          coll n combinef reducef)) 
  clojure.lang.PersistentHashMap
    (coll-fold [coll n combinef reducef]
      ; (println "HashMap Fold!" "Class:" (class coll))
      (.fold coll n combinef reducef fjinvoke fjtask fjfork fjjoin))
  clojure.data.avl.AVLMap
    (coll-fold [coll n combinef reducef]
      ; (println "AVLMap Fold!" "Class:" (class coll))
      (fold-by-halves
        (fn [coll-0 ct]
          (let [split-ind (quot ct 2)]
            (map/split-at split-ind coll-0)))
          coll n combinef reducef)))
; (extend clojure.lang.IPersistentVector ; 10 MAY 2012
;   CollFold
;     (fold-by-halves
;       (fn [v size]
;         (let [split (quot size 2)]
;           [(subvec v 0 split)
;            (subvec v split size)]))))
; In general most users will not call r/reduce directly and instead should prefer r/fold, which
; implements parallel reduce and combine.
; If a collection does not support folding, it will fall back to non-parallel reduce instead

; It partitions collections into blocks, which are then reduced, with the outputs of 
; this stage later combined by the combining function. Order is preserved. - Michal Marczyk
; /foldcat+/ is about 5% faster than /vec+/, but we'll sacrifice it; faster than doall, as well (because of .append with ArrayList)
  ; doall:    70.257915 ms
  ; vec+:     54.162415 ms (into)
  ; foldcat+: 49.738498 ms

; (defn folder+
;   "Given a foldable collection, and a transformation function xf,
;   returns a foldable collection, where any supplied reducing
;   fn will be transformed by xf. xf is a function of reducing fn to
;   reducing fn."
;   {:added "1.5"
;    :attribution "clojure.core.reducers"}
;   ([coll xf]
;      (reify
;         clojure.core.protocols/CollReduce
;           (coll-reduce [_ f1]
;             (clojure.core.protocols/coll-reduce coll (xf f1) (f1)))
;           (coll-reduce [_ f1 init]
;             (clojure.core.protocols/coll-reduce coll (xf f1) init))
;         CollFold
;           (coll-fold [_ n combinef reducef]
;             (coll-fold coll n combinef (xf reducef))))))

; HEADLESS FIX
; {:attribution "Christophe Grand - http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/"}
(defrecord Folder [coll xf])
(extend-type Folder
  clojure.core.protocols/CollReduce
    (coll-reduce [fldr f1]
      (clojure.core.protocols/coll-reduce (:coll fldr) ((:xf fldr) f1) (f1)))
    (coll-reduce [fldr f1 init]
      (clojure.core.protocols/coll-reduce (:coll fldr) ((:xf fldr) f1) init))
  CollFold
    (coll-fold [fldr n combinef reducef]
      (coll-fold (:coll fldr) n combinef ((:xf fldr) reducef))))
(defn folder+
  "Given a foldable collection, and a transformation function xf,
  returns a foldable collection, where any supplied reducing
  fn will be transformed by xf. xf is a function of reducing fn to
  reducing fn.

  Modifies reducers to not use Java methods but external extensions.
  This is because the protocol methods is not a Java method of the reducer
  object anymore and thus it can be reclaimed while the protocol method
  is executing."
  {:attribution "Christophe Grand - http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/"}
  ([coll xf]
     (Folder. coll xf)))

; (doall    (concat            (range  10000) (range  10000 20000)))  ; 817.381379 µs
; (into []  (mapcat+ identity [(range+ 10000) (range+ 10000 20000)])) ; 562.562681 µs
; (into []  (reduce+ cat+     [(range+ 10000) (range+ 10000 20000)])) ; 469.923140 µs
; (foldcat+ (reduce+ cat+     [(range+ 10000) (range+ 10000 20000)])) ; 323.518074 µs

; fold can do more - run in parallel. It does this when the collection is amenable to parallel subdivision.
; Ideal candidates are data structures built from trees.
; reduce catvec is the same as mapcat for all-vectors
; (On the iMac)
; (persistent! (reduce  conj!   (transient       [])                        (range 0 1000000))) ; 76 ms L 63 ms U 133 ms
; (persistent! (reduce+ conj!   (transient       [])                        (range 0 1000000))) ; 64 ms L 63 ms U 65  ms
; (persistent! (fold+   (fn ([] (transient       []))  ([a b] (conj! a b))) (range 0 1000000))) ; 60 ms L 59 ms U 62  ms
; The following falls back to normal /reduce/ because it was operating on a sequential collection... that's why
; (persistent! (fold+   (fn ([] (transient (vec+ []))) ([a b] (conj! a b))) (range 0 1000000))) ; 64 ms L 63 ms U 67  ms
(def reducer? 
  (compr class
    (fn-or (f*n isa? clojure.core.protocols.CollReduce)
           (f*n isa? quanta.library.reducers.Folder))))
(def fold-pre
  (condf*n
    fn?    (compr call  (if*n delay? force identity))
    delay? (compr force (if*n fn?    call  identity))
    :else identity))
(def fold-size
  (fn-> count dec  
        (quot (.. Runtime getRuntime availableProcessors))
        inc))
(defn orig-folder-coll [coll-0]
  (def coll-n (atom (:coll coll-0)))
  (while (reducer? @coll-n)
    (swap! coll-n :coll))
  @coll-n)
; Even a fold version on 2 cores will probably be still be much slower than the clojure.core/frequencies
; version which uses transients.
; (fold+ + + (range+ 1000000))
; 512     - 17.025276 ms
; 1024    - 14.462560 ms
; 2056    - 12.935123 ms
; 250000  - 11.736387 ms (optimal, because evenly chunked)
; reduce+ - 22.148465 ms
; apply   - 36.894498 ms
(defn into+
  ([to from]
    (if (every? vector? [to from])
        (catvec to from)
        (into to (fold-pre from))))
  ([to from & froms]
    (reduce+ into+ (into+ to (fold-pre from)) froms)))
(def vec+
  (condf*n
    vector? identity
    (fn-or coll? reducer? array-list?) (partial into+ [])
    nil?    (constantly [])
    :else   vector)) ; faster than vec, I think ; even faster than type-hinted arrays or cons cells... wow!
(defn fold+
  "Reduces a collection using a (potentially parallel) reduce-combine
  strategy. The collection is partitioned into groups of approximately
  n (default 512), each of which is reduced with reducef (with a seed
  value obtained by calling (combinef) with no arguments). The results
  of these reductions are then reduced with combinef (default
  reducef).
  /combinef/ must be associative. When called with no
  arguments, (combinef) must produce its identity element.
  These operations may be performed in parallel, but the results will preserve order."
  {:added "1.5" :attribution "clojure.core.reducers" :contributors "Alex Gunnarson"}
  ([obj]
    (ifn (fold-pre obj)
      (fn-or reducer? lseq?) vec+ identity))
  ([           reducef coll] (fold+     reducef  reducef coll))
  ([  combinef reducef coll]
    (fold+
      (condf (-> coll fold-pre)
        (fn-and (fn-not reducer?) counted?)
          fold-size
        (compr orig-folder-coll counted?)
          (compr orig-folder-coll fold-size)
        :else (constantly 512)) ; Why 512, particularly? 
      combinef reducef coll))
  ([n combinef reducef coll] (coll-fold (fold-pre coll) n combinef reducef)))
(defn foldp-max+ [obj]
  (fold+ 1 (monoid into+ vector) conj obj))
(defn foldp+
  {:todo ["Detect whether there can be a speed improvement achieved or not"]}
  [obj]
  (fold+ (monoid into+ vector) conj obj))
(defn foldm* [map-fn obj]
  (fold+
    (fn ([]        (map-fn))
        ([ret m]   (merge+ ret m))
        ([ret k v] (assoc ret k v))) ; assoc+, but it's foldp...
    obj))
(defn foldm+   [obj] (foldm* hash-map  obj))
(defn fold-am+ [obj] (foldm* array-map obj))
(defn fold-sm+ [obj] (foldm* sorted-map+ obj))
(defn foldm-s+
  "Single-threaded to get around a weird 'ClassCastException' which
   occurs presumably because of thread overload."
  [obj]
  (->> obj fold+ (into+ {})))
(defn fold-s+
  "Fold into hash-set."
  {:todo ["Speed this up!!"]}
  [coll]
  (fold+
    (fn ([]         (hash-set))
        ([ret elem]
          (set/union ret
            (if (not (set? elem))
                (hash-set elem)
                elem))))
    coll))
(defn reduce-s+
  "Reduce into hash-set."
  {:todo ["Speed this up!!"]}
  [coll]
  (->> coll force
       (reduce+
         (fn
           ([ret elem]
             (conj ret elem))
           ([ret k v]
             (conj ret (map-entry k v))))
         #{})))
;___________________________________________________________________________________________________________________________________
;=================================================={    transduce.reducers    }=====================================================
;=================================================={                          }=====================================================
(defcurried map-state ; yields a reducer
  "Like map, but threads a state through the sequence of transformations.
  For each x in coll, f is applied to [state x] and should return [state' x'].
  The first invocation of f uses init as the state."
  {:attribution "transduce.reducers"}
  [f init coll]
  (reducer+ (fold-pre coll)
    (fn [f1]
      (let [state (atom init)]
        (fn [acc x]
          (let [[state* x*] (f @state x)]
            (reset! state state*)
            (f1 acc x*)))))))
(defcurried mapcat-state
  "Like mapcat, but threads a state through the sequence of transformations. ; so basically like /reductions/?
  For each x in coll, f is applied to [state x] and should return [state' xs].
  The result is the concatenation of each returned xs."
  {:attribution "transduce.reducers"}
  [f init coll]
  (reducer+ coll
    (fn [f1]
      (let [state (atom init)]
        (fn [acc x]
          (let [[state* xs] (f @state x)]
            (reset! state state*)
            (if (seq xs)
                (reduce f1 acc xs)
                acc)))))))
;___________________________________________________________________________________________________________________________________
;=================================================={           CAT            }=====================================================
;=================================================={                          }=====================================================
; Do not construct this directly; use cat.
(deftype Cat [cnt left right]
  clojure.lang.Counted
    (count [_] cnt)
  clojure.lang.Seqable
    (seq [_] (concat (seq left) (seq right)))
  clojure.core.protocols/CollReduce
    (coll-reduce [this f1] (clojure.core.protocols/coll-reduce this f1 (f1)))
    (coll-reduce
      [_  f1 init]
      (clojure.core.protocols/coll-reduce
       right f1
       (clojure.core.protocols/coll-reduce left f1 init)))
  CollFold
    (coll-fold
     [_ n combinef reducef]
     (fjinvoke
       (fn []
         (let [rt (fjfork (fjtask #(coll-fold right n combinef reducef)))]
           (combinef
            (coll-fold left n combinef reducef)
            (fjjoin rt)))))))
(defn cat+
  "A high-performance combining fn that yields the catenation of the
  reduced values. The result is reducible, foldable, seqable and
  counted, providing the identity collections are reducible, seqable
  and counted. The single argument version will build a combining fn
  with the supplied identity constructor. Tests for identity
  with (zero? (count x)). See also foldcat."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  ([] (java.util.ArrayList.))
  ([ctor]
    (fn
      ([] (ctor))
      ([left right] (cat+ left right))))
  ([left-0 right-0]
    (let [left  (fold-pre left-0)
          right (fold-pre right-0)]
      (cond
        (zero? (count left )) right ; count* takes longer, because /count/ for ArrayLists is O(1)
        (zero? (count right)) left
        :else
        (Cat. (+ (count left) (count right)) left right)))))
(defn append!
  ".adds x to acc and returns acc"
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [^java.util.Collection acc x]
  (doto acc (.add x)))
; foldcat+ is probably the best choice! better, even, than vec+! but it outputs ArrayLists - that's why...
(defn foldcat+
  "Equivalent to (fold cat append! coll)"
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [coll]
  (fold+ cat+ append! coll))
;___________________________________________________________________________________________________________________________________
;=================================================={           MAP            }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private map*
  "Applies f to every value in the reduction of coll. Foldable."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [f coll]
  (folder+ coll
    (fn [f1]
      (rfn [f1 k]
           ([ret k v]
              (f1 ret (f k v)))))))
(defn map+ [func coll] (->> coll fold-pre (map* func) delay))

; (defcurried ^:private map*
;   "Like (map f coll-0 coll-1)."
;   {:attribution "Alex Gunnarson"}
;   [f coll]
;   (folder+ coll
;     (fn [f1]
;       (rfn [f1 k] ; "Builds 3-arity reducing fn given names of wrapped fn and key, and k/v impl."
;            ([ret k v]
;               (f1 ret (f k v)))))))
; (defn map+ [func coll] (->> coll fold-pre (map* func) delay))
(defn map-indexed+ ; does this need a delay?
  "Reducers version of /map-indexed/."
  ^{:attribution "parkour.reducers"}
  [f coll]
  ; USAGE:
  ; (map-indexed vector "foobar")
  ; ([0 \f] [1 \o] [2 \o] [3 \b] [4 \a] [5 \r])  
  (map-state
    (fn [n x] [(inc n) (f n x)]) ; juxt?
    0 coll))
(defn indexed+
  "Returns an ordered sequence of vectors `[index item]`, where item is a
  value in coll, and index its position starting from zero."
  ^{:attribution "weavejester.medley"}
  [coll]
  (map-indexed+ vector coll))
; / keep-indexed/
; (keep-indexed
;   (fn [idx v]
;     (if (pos? v) idx))
;   [-9 0 29 -7 45 3 -8])
; => (2 4 5)
;___________________________________________________________________________________________________________________________________
;=================================================={          MAPCAT          }=====================================================
;=================================================={                          }=====================================================
; mapcat+ is fixed in Clojure 1.6, so don't worry!
(defcurried ^:private mapcat*
  "Applies f to every value in the reduction of coll, concatenating the result
  colls of (f val). Foldable."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [f coll]
  (folder+ coll
    (fn [f1]
      (let [f1 (fn
                 ([ret v]
                   (let [x (f1 ret   v)] (if (reduced? x) (reduced x) x)))
                 ([ret k v]
                   (let [x (f1 ret k v)] (if (reduced? x) (reduced x) x))))]
        (rfn [f1 k]
             ([ret k v]
                (reduce+ f1 ret (f k v))))))))
(defn mapcat+ [func coll] (->> coll fold-pre (mapcat* func) delay)) ; mapcat: ([:a 1] [:b 2] [:c 3]) versus mapcat+: (:a 1 :b 2 :c 3) ; hmm...
; (defn mapcat++ ^{:attribution "rxacevedo - https://gist.github.com/rxacevedo/9e3713f7a274126612c9"}
;   [f coll]
;   (defn mapcat* [f]
;     (fn [f1]
;       (fn [l r] (reduce f1 l (f r)))))
;   (reduce ((mapcat* f) conj) [] coll))
(defn concat+ [& args]
  ; (mapcat+ identity args) ; one way
  (reduce+ cat+ args))
;___________________________________________________________________________________________________________________________________
;=================================================={        REDUCTIONS        }=====================================================
;=================================================={                          }=====================================================
(defn reductions+
  "Reducers version of /reductions/.
   Returns a reducer of the intermediate values of the reduction (as per reduce) of coll by f.
   USAGE:
   (into+ [] (reductions+ + [1 2 3]))
   => [1 3 6]"
  ^{:attribution "parkour.reducers"}
  ([f coll] (reductions+ f (f) coll))
  ([f init coll]
     (let [sentinel (Object.)]
       (->> (mapcat+ identity [coll [sentinel]])
            (map-state
              (fn [acc x]
                (if (identical? sentinel x)
                  [nil acc]
                  (let [acc' (f acc x)]
                    [acc' acc])))
              init)
            delay))))
;___________________________________________________________________________________________________________________________________
;=================================================={      FILTER, REMOVE      }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private filter*
  "Retains values in the reduction of coll for which (pred val)
  returns logical true. Foldable."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [pred coll]
  (folder+ coll
    (fn [f1]
      (rfn [f1 k]
           ([ret k v]
              (if (pred k v)
                  (f1 ret k v)
                  ret))))))
(defn filter+ [func coll] (->> coll fold-pre (filter* func) delay))
(defcurried ^:private remove*
  "Removes values in the reduction of coll for which (pred val)
  returns logical true. Foldable."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [pred coll]
  (filter+ (complement pred) coll))
(defn remove+ [func coll] (->> coll fold-pre (remove* func)))
(def keep+ ; does this need a delay?
  "Like /keep/, but implemented in terms of /reduce/.
  USAGE
  (vec+ (keep+ even? (range 1 10)))
  => [false true false true false true false true false]"
  ^{:attribution "parkour.reducers"}
  (compr map+ (partial remove+ nil?))) 
;___________________________________________________________________________________________________________________________________
;=================================================={         FLATTEN          }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private flatten*
  "Takes any nested combination of sequential things (lists, vectors,
  etc.) and returns their contents as a single, flat foldable
  collection."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [coll]
  (folder+ coll
   (fn [f1]
     (fn
       ([] (f1))
       ([ret v]
          (if (sequential? v)
              (clojure.core.protocols/coll-reduce (flatten* v) f1 ret) ; does this not cause issues though?
              (f1 ret v)))))))
(defn flatten+ [coll] (->> coll fold-pre flatten* delay))
(def  flatten-1+ (partial mapcat+ identity))
;___________________________________________________________________________________________________________________________________
;=================================================={          REPEAT          }=====================================================
;=================================================={                          }=====================================================

;___________________________________________________________________________________________________________________________________
;=================================================={         ITERATE          }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private iterate*
  "A reducible collection of [seed, (f seed), (f (f seed)), ...]"
  {:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  [f seed]
  (reify
    clojure.core.protocols/CollReduce
      (coll-reduce [this f1] (clojure.core.protocols/coll-reduce this f1 (f1)))
      (coll-reduce [this f1 init]
        (loop [ret (f1 init seed) seed seed]
          (if (reduced? ret)
            @ret
            (let [next-n (f seed)]
              (recur (f1 ret next-n) next-n)))))
    clojure.lang.Seqable
      (seq [this]
        (seq (iterate f seed)))))
(defn iterate+ [func seed] (->> seed fold-pre (iterate* func) delay))
;___________________________________________________________________________________________________________________________________
;=================================================={          RANGE           }=====================================================
;=================================================={                          }=====================================================
; https://gist.github.com/amalloy/1586b2460329dde1c374 - Creating new Reducer sources
; Implement range as a reducer (also foldable).
; range and iterate shouldn't be novel in reducers, but just enhanced return values of core fns. - Rich Hickey
; range and iterate are sources, not transformers, and only transformers
; (which must be different from their seq-based counterparts) must reside in reducers.
(deftype Range [start end step]
  clojure.lang.Counted
  (count [this]
    (int (Math/ceil (/ (- end start) step))))
  clojure.lang.Seqable
  (seq [this]
    (seq (range start end step)))
  clojure.core.protocols/CollReduce
  (coll-reduce [this f1] (clojure.core.protocols/coll-reduce this f1 (f1)))
  (coll-reduce [this f1 init]
    (let [cmp (if (pos? step) < >)]
      (loop [ret init i start]
        (if (reduced? ret)
          @ret
          (if (cmp i end)
            (recur (f1 ret i) (+ i step))
            ret)))))
  CollFold
  (coll-fold [this n combinef reducef]
    (fold-by-halves
      (fn [_ size] ;; the range passed is always just this Range
          (let [split (-> (quot size 2)
                          (* step)
                          (+ start))]
            [(Range. start split step)
             (Range. split end step)]))
        this n combinef reducef)))
; (reduce+ + (range  1000000)) ; 43.516887 ms
; (fold+   + (range  1000000)) ; 43.179942 ms
; (reduce+ + (range+ 1000000)) ; 23.914831 ms ; because of splitting the vector in two
; (fold+   + (range+ 1000000)) ; 11.635794 ms ; because of 4 cores :D
(defn range+
  "Returns a reducible collection of nums from start (inclusive) to end
  (exclusive), by step, where start defaults to 0, step to 1, and end
  to infinity."
  ^{:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  ([              ] (iterate+ inc 0))
  ([      end     ] (Range. 0     end 1))
  ([start end     ] (Range. start end 1))
  ([start end step] (Range. start end step)))
;___________________________________________________________________________________________________________________________________
;=================================================={     TAKE, TAKE-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private take*
  "Ends the reduction of coll after consuming n values."
  {:added "1.5"
   :attribution  "clojure.core.reducers"
   :contributors "Alex Gunnarson"}
  [n coll]
  (reducer+ coll
    (fn [f1]
      (let [cnt (atom n)]
        (rfn [f1 k]
          ([ret k v]
             (swap! cnt dec)
             (if (neg? @cnt)
               (reduced ret)
               (f1 ret k v))))))))
(defn  take+       [n    coll] (->> coll fold-pre (take*       n)    delay))
(defcurried ^:private take-while*
  "Ends the reduction of coll when (pred val) returns logical false."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [pred coll]
  (reducer+ coll
    (fn [f1]
      (rfn [f1 k]
           ([ret k v]
              (if (pred k v)
                  (f1 ret k v)
                  (reduced ret)))))))
(defn  take-while+ [pred coll] (->> coll fold-pre (take-while* pred) delay))
(defn- take-last*
  ^{:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
  [n coll]
   (reify clojure.core.protocols.CollReduce
     (coll-reduce [this f1]
       (clojure.core.protocols/coll-reduce this f1 (f1)))
     (coll-reduce [_ f1 init]
       (clojure.core.protocols/coll-reduce
         (clojure.core.protocols/coll-reduce
           coll
           (fn [^java.util.Deque q x]
             (when (= (count q) n)
               (.pop q))
             (.add q x)
             q) (java.util.ArrayDeque. (int n)))
         f1 init))))
(defn  take-last+  [n    coll] (->> coll fold-pre (take-last*  n)    delay))
;___________________________________________________________________________________________________________________________________
;=================================================={     DROP, DROP-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private drop*
  "Elides the first n values from the reduction of coll."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [n coll]
  (reducer+ coll
    (fn [f1]
      (let [cnt (atom n)]
        (rfn [f1 k]
          ([ret k v]
             (swap! cnt dec)
             (if (neg? @cnt)
               (f1 ret k v)
               ret)))))))
(defn  drop+       [n    coll] (->> coll fold-pre (drop*       n)    delay))
(defcurried ^:private drop-while*
  "Skips values from the reduction of coll while (pred val) returns logical true."
  {:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  [pred coll]
  (reducer+ coll
    (fn [f1]
      (let [keeping? (atom false)]
        (rfn [f1 k]
          ([ret k v]
             (if (or @keeping?
                     (reset! keeping? (not (pred k v))))
               (f1 ret k v)
               ret)))))))
(defn  drop-while+ [pred coll] (->> coll fold-pre (drop-while* pred) delay))
(defn- drop-last* ; This is extremely slow by comparison. About twice as slow
  ^{:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
  [n coll]
   (reducer+ coll
     (fn [f1]
       (let [buffer (java.util.ArrayDeque. (int n))]
         (fn self
           ([] (f1))
           ([ret x]
             (let [ret (if (= (count buffer) n) ; because Java object
                         (f1 ret (.pop buffer))
                         ret)]
               (.add buffer x)
               ret)))))))
(defn  drop-last+  [n    coll] (->> coll fold-pre (drop-last*  n)    delay))
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
(defn reduce-by+
  "Partition `coll` with `keyfn` as per /partition-by/, then reduce
  each partition with `f` and optional initial value `init` as per
  /reduce+/."
  ^{:attribution "parkour.reducers"}
  ([keyfn f coll]
     (let [sentinel (Object.)]
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
            (remove+ (partial identical? sentinel)))))
  ([keyfn f init coll]
     (let [f (fn ([] init) ([acc x] (f acc x)))]
       (reduce-by+ keyfn f coll))))
; /group-by/
; Returns a map of the elements of coll keyed by the result of
; f on each element. The value at each key will be a vector of the
; corresponding elements, in the order they appeared in coll.
; (group-by odd? (range 10))
; => {false [0 2 4 6 8], true [1 3 5 7 9]}
; (group-by :user-id
;   [{:user-id 1 :uri "/"}
;    {:user-id 2 :uri "/foo"}
;    {:user-id 1 :uri "/account"}])
; => {1 [{:user-id 1, :uri "/"} {:user-id 1, :uri "/account"}],
;     2 [{:user-id 2, :uri "/foo"}]}
(defn group-by+ ; Yes, but folds a lazy sequence... hmm...
  "Reducers version."
  ^{:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/12c3k7ztbz/group-by-vs-reducers"}
  [f coll]
  (fold+
    (partial merge-with
      (fn [v1 v2] (-> (concat+ v1 v2) fold+)))
    (fn [groups a]
      (let [k (f a)]
        (assoc groups k (conj (get groups k []) a))))
    coll))
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================
(defn distinct-by+ ; 228.936664 ms (pretty much attains java speeds!!!)
  "Remove adjacent duplicate values of (@f x) for each x in @coll.
   CAVEAT: Requires @coll to be sorted to work correctly."
  ^{:attribution "parkour.reducers"}
  [f coll]
  (let [sentinel (Object.)] ; instead of nil, because it's unique
    (->> (apply concat+ [(fold-pre coll) [sentinel]]) ; /fold/ed coll, otherwise it doesn't know how to /mapcat/ it
         (map-state
           (fn [x x']
             (let [xf  (ifn x  (partial identical? sentinel) identity f)
                   xf' (ifn x' (partial identical? sentinel) identity f)]
               [x' (if (= xf xf') sentinel x')]))
           sentinel)
         (remove+ (partial identical? sentinel)))))
; <<<<------ REDUCER ------>>>> ; UNFORTUNATELY REQUIRES FOLDING BEFOREHAND... ; DOES IT?
(defn distinct+
  "Remove adjacent duplicate values from @coll.
   CAVEAT: Requires @coll to be sorted to work correctly."
  ^{:attribution "parkour.reducers"}
  [coll] (->> coll fold-pre (distinct-by+ identity)))
;___________________________________________________________________________________________________________________________________
;=================================================={          ZIPVEC          }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private zipvec*
  "Zipvec. Needs a better implementation.
   Must start out with pre-catvec'd colls."
  {:attribution "Alex Gunnarson"}
  [coll]
  (let [ind-n   (atom 0) ; This probably makes it single-threaded only :/
        coll-ct (-> coll count (/ 2) long)]
    (folder+ coll
      (fn [f1]
        (rfn [f1 k]
          ([ret k v]
            (if (< (count ret) coll-ct)
                (do (f1 ret k [v nil]))
                (do  ; this part is problematic
                    (swap! ind-n inc)
                    (f1 (assoc! ret (dec @ind-n) [(-> ret (get (dec @ind-n)) (get 0)) v]) k nil)))))))))
(defn zipvec+
  ([vec-0]
    (->> vec-0 fold-pre zipvec* (take+ (/ (count vec-0) 2))))
  ([vec-0 & vecs]
    (->> vecs (apply map vector vec-0) fold+)))
;___________________________________________________________________________________________________________________________________
;=================================================={    MISC. BOILERPLATE?    }=====================================================
;=================================================={                          }=====================================================
(defmacro ^{:private true :attribution "clojure.core, via Christophe Grand - https://gist.github.com/cgrand/5643767"}
  assert-args [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                  (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args more)))))
(defn emit-comprehension
  ^{:attribution "clojure.core, via Christophe Grand - https://gist.github.com/cgrand/5643767"}
  [&form {:keys [emit-other emit-inner]} seq-exprs body-expr]
  (assert-args
     (vector? seq-exprs) "a vector for its binding"
     (even? (count seq-exprs)) "an even number of forms in binding vector")
  (let [groups (reduce (fn [groups [k v]]
                         (if (keyword? k)
                           (conj (pop groups) (conj (peek groups) [k v]))
                           (conj groups [k v])))
                 [] (partition 2 seq-exprs)) ; /partition/... hmm...
        inner-group (peek groups)
        other-groups (pop groups)]
    (reduce emit-other (emit-inner body-expr inner-group) other-groups)))
(defn- do-mod [mod-pairs cont & {:keys [skip stop]}]
  (let [err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))]
    (reduce 
      (fn [cont [k v]]
        (cond 
          (= k :let) `(let ~v ~cont)
          (= k :while) `(if ~v ~cont ~stop)
          (= k :when) `(if ~v ~cont ~skip)
          :else (err "Invalid 'for' keyword " k)))
      cont (reverse mod-pairs)))) ; this is terrible
;___________________________________________________________________________________________________________________________________
;=================================================={ LOOPS / LIST COMPREHENS. }=====================================================
;=================================================={        for, doseq        }=====================================================
; <<<<---------- REDUCER ---------->>>>
(defmacro for+ ; 51.454164 ms vs. 72.568330 ms for doall with normal for (!)
  "Reducer comprehension, behaves like \"for\" but yields a reducible/foldable collection.
   Leverages kv-reduce when destructuring and iterating over a map."
  ^{:attribution "Christophe Grand, https://gist.github.com/cgrand/5643767"}
  [seq-exprs body-expr]
  (letfn [(emit-fn [form] 
            (fn [sub-expr [bind expr & mod-pairs]]
              (let [foldable (not-any? (comp #{:while} first) mod-pairs)
                    kv-able (and (vector? bind) (not-any? #{:as} bind)
                              (every? #(and (symbol? %) (not= % '&)) (take 2 bind)))
                    [kv-args kv-bind] 
                    (if kv-able
                      [(take 2 (concat bind (repeat `_#)))
                       (if (< 2 (count bind)) 
                         [(subvec bind 2) nil]
                         [])]
                      `[[k# v#] [~bind (map-entry k# v#)]])
                    combiner (if kv-able
                               (if foldable `folder+ `reducer+)
                               (if foldable `folder+ `reducer+)) ; (if foldable `r/folder `r/reducer)
                    f (gensym "f__")
                    ret (gensym "ret__")
                    body (do-mod mod-pairs (form f ret sub-expr)
                           :skip ret
                           :stop `(reduced ~ret))]
                `(~combiner ~expr
                   (fn [~f]
                     (fn
                       ([] (~f))
                       ([~ret ~bind] ~body)
                       ([~ret ~@kv-args] (let ~kv-bind ~body))))))))]
    (emit-comprehension &form
      {:emit-other (emit-fn (partial list `reduce+)) :emit-inner (emit-fn list)}
      seq-exprs body-expr)))
; <<<<------ ALREADY REDUCED ------>>>>
(defmacro doseq+
  "doseq but based on reducers, leverages kv-reduce when iterating on maps."
  ^{:attribution "Christophe Grand, https://gist.github.com/cgrand/5643767"}
  [bindings & body]
 `(reduce+ (constantly nil) (for+ ~bindings (do ~@body))))
(defcurried each ; like doseq
  "Applies f to each item in coll, returns nil"
  {:attribution "transduce.reducers"}
  [f coll]
  (reduce+ (fn [_ x] (f x) nil) nil coll))