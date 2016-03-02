(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support.

      Adds some interesting reducers and folders from different sources
      gleaned from the far reaches of the internet. Some of them have
      unexpectedly great performance.

      Also adds a delay at every stage of reducer/folder-transformation.
      That seemed important at one point, but maybe it isn't actually."
      :author       "Rich Hickey"
      :contributors #{"Alan Malloy" "Alex Gunnarson" "Christophe Grand"}}
  quantum.core.reducers
  #?(:clj  (:refer-clojure :exclude [reduce])
     :cljs (:refer-clojure :exclude [Range ->Range reduce]))
  (:require-quantum [:core fn logic macros #_num type map set vec log cbase err])
  (:require         [clojure.walk :as walk]
                    [quantum.core.numeric :as num])
  #?(:cljs
  (:require-macros  [quantum.core.numeric :as num])))

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
;=================================================={      LAZY REDUCERS       }=====================================================
;=================================================={                          }=====================================================
; Sometimes in a seq pipeline, you know that some intermediate results are, well,
; intermediate and as such don’t need to be persistent but, on the whole, you still need the laziness.
(defn- reverse-conses 
  {:attribution "Christophe Grand, http://clj-me.cgrand.net/2013/02/11/from-lazy-seqs-to-reducers-and-back/"}
  ([s tail] 
    (if (identical? (rest s) tail)
      s
      (reverse-conses s tail tail)))
  ([s from-tail to-tail]
    (loop [f s b to-tail]
      (if (identical? f from-tail)
        b
        (recur (rest f) (cons (first f) b))))))

#?(:clj
  (defn seq-seq
    {:attribution "Christophe Grand, http://clj-me.cgrand.net/2013/02/11/from-lazy-seqs-to-reducers-and-back/"
     :usage "(seq->> (range) (map+ str) (take+ 25) (drop+ 5))"
     :out "(\"5\" \"6\" \"7\" \"8\" \"9\" \"10\" \"11\" \"12\" \"13\" \"14\" \"15\" \"16\" 
            \"17\" \"18\" \"19\" \"20\" \"21\" \"22\" \"23\" \"24\")"}
    [f s] 
    (let [f1 (clojure.core/reduce #(cons %2 %1) nil ; Note that the captured function (f1) may be impure, so don’t share it!
                (f (reify clojure.core.protocols.CollReduce
                     (#?(:clj coll-reduce :cljs -reduce) [this f1 init]
                       f1))))]
      ((fn this [s]
         (lazy-seq 
           (when-let [s (seq s)]
             (let [more (this (rest s)) 
                   x (f1 more (first s))]
               (if (reduced? x)
                 (reverse-conses @x more nil)
                 (reverse-conses x more)))))) s))))

#?(:clj
  (defmacro lseq->>
    ^{:attribution "Christophe Grand, http://clj-me.cgrand.net/2013/02/11/from-lazy-seqs-to-reducers-and-back/"}
    [s & forms] 
    `(seq-seq (fn [n#] (->> n# ~@forms)) ~s)))

#?(:clj
  (defn seq-once
    "Returns a sequence on which seq can be called only once.
     Use when you don't want to keep the head of a lazy-seq while using reducers.
     Shouldn't be necessary with the fix starting around line 60."
    ^{:attribution "Christophe Grand, http://stackoverflow.com/questions/22031829/tuning-clojure-reducers-library-performace"}
    [coll]
    (let [a (atom coll)]
      (reify clojure.lang.Seqable ; ClojureScript has cljs.core.ISeqable but its method |seq| is private 
        (seq [_]
          (let [coll @a]
            (reset! a nil)
            (seq coll)))))))
;___________________________________________________________________________________________________________________________________
;=================================================={        FORK/JOIN         }=====================================================
;=================================================={                          }=====================================================
#?(:clj
  (macros/compile-if
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
     (defn- fjjoin [task] (.join ^jsr166y.ForkJoinTask task)))))
; #?@ with defn doesn't work
#?(:cljs (defn- fjtask   [f]     f    ))
#?(:cljs (defn- fjinvoke [f]    (f)   ))
#?(:cljs (defn- fjfork   [task] task  ))
#?(:cljs (defn- fjjoin   [task] (task)))
;___________________________________________________________________________________________________________________________________
;=================================================={      FUNCTIONEERING      }=====================================================
;=================================================={      incl. currying      }=====================================================
(defn- reduce-impl
  "Creates an implementation of CollReduce using the given reducer.
  The two-argument implementation of reduce will call f1 with no args
  to get an init value, and then forward on to your three-argument version."
  {:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  [reducer-n]
  {#?(:clj  :coll-reduce
      :cljs :-reduce)
    (fn ([coll f1     ] (reducer-n coll f1 (f1)))
        ([coll f1 init] (reducer-n coll f1 init)))})
;___________________________________________________________________________________________________________________________________
;=================================================={          REDUCE          }=====================================================
;=================================================={                          }=====================================================
; reduce-reverse [f init o] - like reduce but in reverse order
#?(:cljs
  (defn- -reduce-seq
    "For some reason |reduce| is not implemented in ClojureScript for certain types.
     This is a |loop|-|recur| replacement for it."
    {:attribution "Alex Gunnarson"
     :todo ["Check if this is really the case..." "Improve performance with chunking, etc."]}
    [coll f init]
    (loop [coll-n coll
           ret    init]
      (if (empty? coll-n)
          ret
          (recur (rest coll-n)
                 (f ret (first coll-n)))))))

(defnt reduce+
  "Like |core/reduce| except:
      When init is not provided, (f) is used.
      Maps are reduced with |reduce-kv|."
  {:attribution "Alex Gunnarson"}
  ([^fast_zip.core.ZipperLocation z f init]
    (cbase/zip-reduce f init z))
  ([^array? arr f init]
    #?(:clj  (loop [i (long 0) ret init]
               (if (< i (-> arr count long))
                   (recur (unchecked-inc i) (f ret (get arr i)))
                   ret))
       :cljs (array-reduce arr f init)))
  ([^string? s f init]
    #?(:clj (clojure.core.protocols/coll-reduce s f init)
       :cljs
      (let [last-index (-> s count unchecked-dec long)]
        (cond
          (> last-index #?(:clj Long/MAX_VALUE :cljs js/Number.-MAX_VALUE))
            (throw (->ex nil "String too large to reduce over (at least, efficiently)."))
          (= last-index -1)
            (f init nil)
          :else
            (loop [n   (long 0)
                   ret init]
              (if (> n last-index)
                  ret
                  (recur (unchecked-inc n)
                         (f ret (.charAt s n)))))))))
#?(:clj
  ([^record? coll f init] (clojure.core.protocols/coll-reduce coll f init)))
  ([^map? coll f init]
    (#?(:clj  clojure.core.protocols/kv-reduce
        :cljs -kv-reduce)
     coll f init))
  ([^set? coll f init]
    (#?(:clj  clojure.core.protocols/coll-reduce
        :cljs -reduce-seq)
     coll f init))
  ([:else coll f init]
    (when (nnil? coll)
      #?(:clj  (clojure.core.protocols/coll-reduce coll f init)
         :cljs (-reduce coll f init)))))

; (defn- -reduce-kv
;   [coll f init]
;     (try (#?(:clj  clojure.core.protocols/kv-reduce
;              :cljs -kv-reduce)
;             coll f init)
;       (catch #?(:clj Throwable :cljs js/Object) e
;         (do (println "There was an exception in kv-reduce!") ; TODO log this
;             #?(:clj (clojure.stacktrace/print-stack-trace e))
;             (throw (Exception. "Broke out of kv-reduce"))))))
      
#?(:clj
(defmacro reduce
  "Like |core/reduce| except:
   When init is not provided, (f) is used.
   Maps are reduced with reduce-kv.
   
   Entry point for internal reduce (in order to switch the args
   around to dispatch on type)."
  {:attribution "Alex Gunnarson"
   :todo ["definline"]}
  ([f coll]      `(reduce ~f (~f) ~coll))
  ([f init coll] `(quantum.core.reducers/reduce+ ~coll ~f ~init))))

#?(:cljs
(defn reduce
  "Like |core/reduce| except:
   When init is not provided, (f) is used.
   Maps are reduced with reduce-kv.

   Entry point for internal reduce (in order to switch the args
   around to dispatch on type)."
  {:attribution "Alex Gunnarson"}
  ([f coll]      (reduce f (f) coll))
  ([f init coll] (quantum.core.reducers/reduce+ coll f init))))

; ======= END REDUCE ========

(def fold-pre
  (condf*n
    fn?    (fn-> call  (whenf delay? force))
    delay? (fn-> force (whenf fn?    call ))
    :else identity))

(defn into+
  {:todo ["Add extra arities"]}
  ([to from]
    (if (and (vector? to) (vector? from))
        (catvec to from)
        (into to (fold-pre from))))
  ([to from & froms]
    (reduce into+ (into+ to (fold-pre from)) froms)))

(defn reducem+ ; was defn+
  "Requires only one argument for preceding functions in its call chain."
  {:attribution "Alex Gunnarson"
   :performance "9.94 ms vs. 17.02 ms for 10000 calls to (into+ {}) for small collections ;
           This is because the |transient| function deals a performance hit."}
  [coll]
  #?(:clj
    (->> coll force
         (reduce
           (identity #_extern
             (fn ([ret [k v]] (assoc! ret k v))
                 ([ret  k v]  (assoc! ret k v))))
           (transient {}))
         persistent!)
    :cljs
    (into+ {} coll))) 

(defn count*
  {:attribution "parkour.reducers"
   :performance "On non-counted collections, |count| is 71.542581 ms, whereas
                 |count*| is 36.824665 ms - twice as fast!!"}
  [coll]
  (reduce (compr firsta (MWA inc)) 0 coll))

(defn reducer+
  "Given a reducible collection, and a transformation function xf,
  returns a reducible collection, where any supplied reducing
  fn will be transformed by xf. xf is a function of reducing fn to
  reducing fn."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  ([coll xf]
    (reify
      #?(:clj  clojure.core.protocols/CollReduce
         :cljs cljs.core/IReduce)
      (#?(:clj coll-reduce :cljs -reduce) [this f1]
        (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce) this f1 (f1)))
      (#?(:clj coll-reduce :cljs -reduce) [_ f1 init]
        (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce) coll (xf f1) init)))))

; Creating a reducer holds the head of the collection in a closure.
; Thus, a huge lazy-seq can be tied up memory as it becomes realized.

; Fixing it so the seqs are headless.
; Christophe Grand - https://groups.google.com/forum/#!searchin/clojure-dev/reducer/clojure-dev/t6NhGnYNH1A/2lXghJS5HywJ
; (defrecord Reducer [coll xf])
; (extend-protocol clojure.core.protocols/CollReduce
;   Reducer
;     (#+clj coll-reduce #+cljs -reduce [r f1]
;       (#+clj clojure.core.protocols/coll-reduce #+cljs -reduce r f1 (f1)))
;     (#+clj coll-reduce #+cljs -reduce [r f1 init]
;       (#+clj clojure.core.protocols/coll-reduce #+cljs -reduce (:coll r) ((:xf r) f1) init)))
; (def rreducer ->Reducer)
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

(defn- fold-by-halves
  "Folds the provided collection by halving it until it is smaller than the
  requested size, and folding each subsection. halving-fn will be passed as
  input a collection and its size (so you need not recompute the size); it
  should return the left and right halves of the collection as a pair. Those
  halves will normally be of the same type as the parent collection, but
  anything foldable is sufficient.

  Generalized from |foldvec| to work for anything you can split in half."
  {:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  [halving-fn coll n combinef reducef]
  (let [size (count coll)]
    (cond
      (zero? size)
        (combinef)
      (<= size n)
        (reduce reducef (combinef) coll)    
      :else
        #?(:clj
            (let [[left right] (halving-fn coll size)
                  child-fn (fn [child] #(coll-fold child n combinef reducef))]
              (fjinvoke
               #(let [f1 (child-fn left)
                      t2 (fjtask (child-fn right))]
                  (fjfork t2)
                  (combinef (f1) (fjjoin t2)))))
           :cljs
             (reduce reducef (combinef) coll)))))
;___________________________________________________________________________________________________________________________________
;=================================================={           FOLD           }=====================================================
;=================================================={           into           }=====================================================
(extend-protocol CollFold ; clojure.core.reducers
  #?@(:clj
  [nil
    (coll-fold [coll n combinef reducef]
      (combinef))])
  #?(:clj  Object
     :cljs object)
    (coll-fold [coll n combinef reducef]
      (reduce reducef (combinef) coll))
  #?(:clj  clojure.lang.IPersistentVector
     :cljs cljs.core/PersistentVector)
    (coll-fold [coll n combinef reducef]
      (fold-by-halves
        (fn [coll-0 ct]
          (let [split-ind (quot ct 2)]
            [(subvec+ coll-0 0 split-ind) ; test subvec against subvec+
             (subvec+ coll-0   split-ind ct)]))
          coll n combinef reducef)) 
  #?@(:clj
    [clojure.lang.PersistentHashMap
      (coll-fold [coll n combinef reducef]
        (.fold coll n combinef reducef fjinvoke fjtask fjfork fjjoin))])
  clojure.data.avl.AVLMap
    (coll-fold [coll n combinef reducef]
      (fold-by-halves
        (fn [coll-0 ct]
          (let [split-ind (quot ct 2)]
            (map/split-at split-ind coll-0)))
          coll n combinef reducef)))

; HEADLESS FIX
; {:attribution "Christophe Grand - http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/"}
#?(:clj (defrecord Folder [coll xf]))

#?(:clj
  (extend-type Folder
    clojure.core.protocols/CollReduce
      (coll-reduce [fldr f1]
        (reduce+ (:coll fldr) ((:xf fldr) f1) (f1)))
      (coll-reduce [fldr f1 init]
        (reduce+ (:coll fldr) ((:xf fldr) f1) init))
    CollFold
      (coll-fold [fldr n combinef reducef]
        (coll-fold (:coll fldr) n combinef ((:xf fldr) reducef)))))

(defn folder+
  "Given a foldable collection, and a transformation function xf,
  returns a foldable collection, where any supplied reducing
  fn will be transformed by xf. xf is a function of reducing fn to
  reducing fn.

  Modifies reducers to not use Java methods but external extensions.
  This is because the protocol methods is not a Java method of the reducer
  object anymore and thus it can be reclaimed while the protocol method
  is executing."
  {:attribution "Christophe Grand - http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/"
   :todo ["Possibly fix the CLJS version?"]}
  ([coll xf]
    #?(:clj (Folder. coll xf)
       :cljs
       (reify
         cljs.core/IReduce
         (-reduce [_ f1]
           (reduce+ coll (xf f1) (f1)))
         (-reduce [_ f1 init]
           (reduce+ coll (xf f1) init))
 
         CollFold
         (coll-fold [_ n combinef reducef]
           (coll-fold coll n combinef (xf reducef)))))))

; (doall    (concat            (range  10000) (range  10000 20000)))  ; 817.381379 µs
; (into []  (mapcat+ identity [(range+ 10000) (range+ 10000 20000)])) ; 562.562681 µs
; (into []  (reduce cat+     [(range+ 10000) (range+ 10000 20000)])) ; 469.923140 µs
; (foldcat+ (reduce cat+     [(range+ 10000) (range+ 10000 20000)])) ; 323.518074 µs

; fold can do more - run in parallel. It does this when the collection is amenable to parallel subdivision.
; Ideal candidates are data structures built from trees.
; reduce catvec is the same as mapcat for all-vectors
; (On the iMac)
; (persistent! (reduce  conj!   (transient       [])                        (range 0 1000000))) ; 76 ms L 63 ms U 133 ms
; (persistent! (reduce conj!   (transient       [])                        (range 0 1000000))) ; 64 ms L 63 ms U 65  ms
; (persistent! (fold+   (fn ([] (transient       []))  ([a b] (conj! a b))) (range 0 1000000))) ; 60 ms L 59 ms U 62  ms
; The following falls back to normal /reduce/ because it was operating on a sequential collection... that's why
; (persistent! (fold+   (fn ([] (transient (vec+ []))) ([a b] (conj! a b))) (range 0 1000000))) ; 64 ms L 63 ms U 67  ms
(def reducer? 
  (compr class
    (fn-or #?(:clj  (f*n isa?       clojure.core.protocols.CollReduce)
              :cljs (f*n instance+? cljs.core/IReduce))
           (f*n isa? quantum.core.reducers.Folder))))

(def fold-size
  #?(:clj
    (fn-> count dec  
          (quot (.. Runtime getRuntime availableProcessors))
          inc)
    :cljs
    count)) ; Because it's only single-threaded anyway...
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
; reduce - 22.148465 ms
; apply   - 36.894498 ms

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
  @combinef must be associative. When called with no
  arguments, (combinef) must produce its identity element.
  These operations may be performed in parallel, but the results will preserve order."
  {:added "1.5"
   :attribution "clojure.core.reducers"
   :contributors ["Alex Gunnarson"]
   :todo ["Make more efficient." "So many fns created in this lead to inefficiency."]}
  ([obj]
    #?(:clj (whenf (fold-pre obj) (fn-or reducer? lseq?) vec+)
       :cljs (->> obj fold-pre (into+ []))))
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
  ([n combinef reducef coll]
    (coll-fold (fold-pre coll) n combinef reducef)))

(defn foldp-max+ [obj]
  (fold+ 1 (monoid into+ vector) conj obj))
(defn foldp+
  {:todo ["Detect whether there can be a speed improvement achieved or not"]}
  [obj]
  (fold+ (monoid into+ vector) conj obj))
(defn foldm* [map-fn obj]
  (fold+
    (fn ([]        (map-fn))
        ([ret m]   (map/merge ret m))
        ([ret k v] (assoc ret k v))) ; assoc+, but it's foldp...
    obj))
(defn foldm+   [obj] (foldm* hash-map  obj))
(defn fold-am+ [obj] (foldm* array-map obj))
(defn fold-sm+ [obj] (foldm* map/sorted-map obj))
(defn foldm-s+
  "Single-threaded to get around a weird 'ClassCastException' which
   occurs presumably because of thread overload."
  [obj]
  (->> obj fold+ (into+ {})))

(defn fold-s+
  "Fold into hash-set."
  {:todo ["Speed this up!"]}
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
  {:todo ["Speed this up!"]}
  [coll]
  (->> coll force
       (reduce
         (fn
           ([ret elem]
             (conj ret elem))
           ([ret k v]
             (conj ret [k v])))
         #{})))
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
  (reducer+ (fold-pre coll)
    (fn [f1]
      (let [state (atom init)]
        (fn [acc x]
          (let [[state* x*] (f @state x)] ; How about using volatiles here?
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
(deftype Cat [cnt left right]
  #?@(:clj  [clojure.lang.Counted (count  [_] cnt)]
      :cljs [cljs.core/ICounted   (-count [_] cnt)])

  #?@(:clj  [clojure.lang.Seqable (seq  [_] (concat (seq left) (seq right)))]
      :cljs [cljs.core/ISeqable   (-seq [_] (concat (seq left) (seq right)))])

  #?(:clj  clojure.core.protocols/CollReduce
     :cljs cljs.core/IReduce)
    (#?(:clj coll-reduce :cljs -reduce) [this f1]
      (#?(:clj clojure.core.protocols/coll-reduce :cljs reduce+) this f1 (f1)))
    (#?(:clj coll-reduce :cljs -reduce) [_  f1 init]
      (#?(:clj clojure.core.protocols/coll-reduce :cljs reduce+)
       right f1
       (#?(:clj clojure.core.protocols/coll-reduce :cljs reduce+) left f1 init)))

  CollFold
    (coll-fold
     [#?(:clj _ :cljs this) n combinef reducef]
      #?(:cljs (reduce+ this reducef (reducef)) ; For ClojureScript, |fold| just falls back on reduce. No crazy async things.
         :clj
         (fjinvoke
           (fn []
             (let [rt (fjfork (fjtask #(coll-fold right n combinef reducef)))]
               (combinef
                (coll-fold left n combinef reducef)
                (fjjoin rt))))))))

(defn cat+
  "A high-performance combining fn that yields the catenation of the
  reduced values. The result is reducible, foldable, seqable and
  counted, providing the identity collections are reducible, seqable
  and counted. The single argument version will build a combining fn
  with the supplied identity constructor. Tests for identity
  with (zero? (count x)). See also foldcat."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  ([] #?(:clj  (java.util.ArrayList.)
         :cljs (array)))
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
  [acc x]
  #?(:clj  (doto ^java.util.Collection acc (.add  x))
     :cljs (doto acc (.push x))))

(defn foldcat+
  "Equivalent to (fold cat append! coll)"
  {:added "1.5"
   :attribution "clojure.core.reducers"
   :performance "foldcat+ is faster than |into| a PersistentVector because it outputs ArrayLists"}
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

(defn map-indexed+ ; does this need a delay?
  "Reducers version of /map-indexed/."
  {:attribution "parkour.reducers"
   :usage '(map-indexed vector "foobar")
   :out   '([0 \f] [1 \o] [2 \o] [3 \b] [4 \a] [5 \r])}
  [f coll]
  (map-state
    (fn [n x] [(inc n) (f n x)]) ; juxt?
    0 coll))

(defn indexed+
  "Returns an ordered sequence of vectors `[index item]`, where item is a
  value in coll, and index its position starting from zero."
  {:attribution "weavejester.medley"}
  [coll]
  (map-indexed+ vector coll))
;___________________________________________________________________________________________________________________________________
;=================================================={          MAPCAT          }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private mapcat*
  "Applies f to every value in the reduction of coll, concatenating the result
  colls of (f val). Foldable."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [f coll]
  (folder+ coll
    (fn [f1-0]
      (let [f1 (fn
                 ([]
                   (let [x (f1-0        )] (if (reduced? x) (reduced x) x)))
                 ([ret]
                   (let [x (f1-0 ret    )] (if (reduced? x) (reduced x) x)))
                 ([ret v]
                   (let [x (f1-0 ret   v)] (if (reduced? x) (reduced x) x)))
                 ([ret k v]
                   (let [x (f1-0 ret k v)] (if (reduced? x) (reduced x) x))))]
        (rfn [f1 k]
             ([ret k v]
                (reduce f1 ret (f k v))))))))

(defn mapcat+ [func coll] (->> coll fold-pre (mapcat* func) delay)) ; mapcat: ([:a 1] [:b 2] [:c 3]) versus mapcat+: (:a 1 :b 2 :c 3) ; hmm...

(defn concat+ [& args]
  ; (mapcat+ identity args) ; one way
  (reduce cat+ args))
;___________________________________________________________________________________________________________________________________
;=================================================={        REDUCTIONS        }=====================================================
;=================================================={                          }=====================================================
(defn reductions+
  "Reducers version of /reductions/.
   Returns a reducer of the intermediate values of the reduction (as per reduce) of coll by f.
   "
  {:attribution "parkour.reducers"
   :usage '(into+ [] (reductions+ + [1 2 3]))
   :out   '[1 3 6]}
  ([f coll] (reductions+ f (f) coll))
  ([f init coll]
     (let #?(:clj  [sentinel (Object.)]) ; instead of nil, because it's unique
          #?(:cljs [sentinel (array  )])
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

(def keep+
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
              (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce)
                 (flatten* v) f1 ret) ; does this not cause issues though?
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
  {:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"
   :performance "Untested"}
  [f seed]
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
              (recur (f1 ret next-n) next-n)))))
    #?(:clj  clojure.lang.Seqable
       :cljs cljs.core/ISeqable)
      (#?(:clj seq :cljs -seq) [this]
        (seq (iterate f seed)))))

(defn iterate+ [func seed] (->> seed fold-pre (iterate* func) delay))
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
  CollFold
    (coll-fold [this n combinef reducef]
      (fold-by-halves
        (fn [_ size] ;; the range passed is always just this Range
            (let [split (-> (quot size 2)
                            (* step)
                            (+ start))]
              [(quantum.core.reducers/Range. start split step)
               (quantum.core.reducers/Range. split end step)]))
          this n combinef reducef)))

; "Range and iterate shouldn't be novel in reducers, but just enhanced return values of core fns.
; Range and iterate are sources, not transformers, and only transformers
; (which must be different from their seq-based counterparts) must reside in reducers." - Rich Hickey

(defn range+
  "Returns a reducible collection of nums from start (inclusive) to end
  (exclusive), by step, where start defaults to 0, step to 1, and end
  to infinity."
  {:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  ([              ] (iterate+ (MWA inc) 0))
  ([      end     ] (quantum.core.reducers/Range. 0     end 1))
  ([start end     ] (quantum.core.reducers/Range. start end 1))
  ([start end step] (quantum.core.reducers/Range. start end step)))
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
             (swap! cnt (MWA dec))
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

#?(:clj
  (defn- taker*
    ^{:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
    [n coll]
     (reify
       clojure.core.protocols.CollReduce
       ;#+cljs cljs.core/IReduce
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
               q)
             (java.util.ArrayDeque. (int n)))
           f1 init)))))

#?(:clj
  (defn taker+ [n coll]
    (->> coll fold-pre (taker* n) delay)))
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
             (swap! cnt (MWA dec))
             (if (neg? @cnt)
               (f1 ret k v)
               ret)))))))

(defn drop+ [n coll]
  (->> coll fold-pre (drop* n) delay))

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

(defn drop-while+ [pred coll]
  (->> coll fold-pre (drop-while* pred) delay))

#?(:clj
  (defn- dropr* ; This is extremely slow by comparison. About twice as slow
    {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
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
                 ret))))))))

#?(:clj
  (defn dropr+ [n coll]
    (->> coll fold-pre (dropr* n) delay)))
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
(defn reduce-by+
  "Partition `coll` with `keyfn` as per /partition-by/, then reduce
  each partition with `f` and optional initial value `init` as per
  /reduce/."
  ^{:attribution "parkour.reducers"}
  ([keyfn f coll]
     (let #?(:clj  [sentinel (Object.)] ; instead of nil, because it's unique
             :cljs [sentinel (array  )])
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

; (group-by :user-id
;   [{:user-id 1 :uri "/"}
;    {:user-id 2 :uri "/foo"}
;    {:user-id 1 :uri "/account"}])
; => {1 [{:user-id 1, :uri "/"} {:user-id 1, :uri "/account"}],
;     2 [{:user-id 2, :uri "/foo"}]}
(defn group-by+ ; Yes, but folds a lazy sequence... hmm...
  "Reducers version. Possibly slower than |core/group-by|"
  {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/12c3k7ztbz/group-by-vs-reducers"
   :usage '(group-by odd? (range 10))
   :out   '{false [0 2 4 6 8], true [1 3 5 7 9]}}
  [f coll]
  (fold+
    (partial merge-with ; merge-with is why...
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
  {:attribution "parkour.reducers"}
  [f coll]
  (let  #?(:clj  [sentinel (Object.)] ; instead of nil, because it's unique
           :cljs [sentinel (array  )])
    (->> (apply concat+ [(fold-pre coll) [sentinel]]) ; /fold/ed coll, otherwise it doesn't know how to /mapcat/ it
         (map-state
           (fn [x x']
             (let [xf  (ifn x  (partial identical? sentinel) identity f)
                   xf' (ifn x' (partial identical? sentinel) identity f)]
               [x' (if (= xf xf') sentinel x')]))
           sentinel)
         (remove+ (partial identical? sentinel)))))

(defn distinct+
  "Remove adjacent duplicate values from @coll.
   CAVEAT: Requires @coll to be sorted to work correctly."
  {:attribution "parkour.reducers"}
  [coll] (->> coll fold-pre (distinct-by+ identity)))
;___________________________________________________________________________________________________________________________________
;=================================================={          ZIPVEC          }=====================================================
;=================================================={                          }=====================================================
(defcurried ^:private zipvec*
  "Zipvec. Needs a better implementation.
   Must start out with pre-catvec'd colls."
  {:attribution "Alex Gunnarson"}
  [coll]
  (let [ind-n   (atom 0) ; This probably makes it single-threaded only...
        coll-ct (-> coll count (/ 2) long)]
    (folder+ coll
      (fn [f1]
        (rfn [f1 k]
          ([ret k v]
            (if (< (count ret) coll-ct)
                (do (f1 ret k [v nil]))
                (do  ; This part is problematic
                    (swap! ind-n (MWA inc))
                    (f1 (assoc! ret (dec @ind-n) [(-> ret (get (dec @ind-n)) (get 0)) v]) k nil)))))))))
(defn zipvec+
  ([vec-0]
    (->> vec-0 fold-pre zipvec* (take+ (/ (count vec-0) 2))))
  ([vec-0 & vecs]
    (->> vecs (apply map vector vec-0) fold+)))
;___________________________________________________________________________________________________________________________________
;=================================================={ LOOPS / LIST COMPREHENS. }=====================================================
;=================================================={        for, doseq        }=====================================================
#?(:clj
(defmacro for+
  "Reducer comprehension, behaves like \"for\" but yields a reducible/foldable collection.
   Leverages kv-reduce when destructuring and iterating over a map."
  {:attribution "Christophe Grand, https://gist.github.com/cgrand/5643767"
   :performance "51.454164 ms vs. 72.568330 ms for |doall| with normal |for|"}
  [seq-exprs body-expr]
  (identity ; was...delay but defmacro says print-dup not defined
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
                      `[[k# v#] [~bind [k# v#]]])
                    combiner (if kv-able
                               (if foldable `folder+ `reducer+)
                               (if foldable `folder+ `reducer+)) ; (if foldable `r/folder `r/reducer)
                    f   (gensym "f__")
                    ret (gensym "ret__")
                    body (quantum.core.macros/do-mod mod-pairs
                           (form f ret sub-expr)
                           :skip ret
                           :stop `(reduced ~ret))]
                `(~combiner ~expr
                   (fn [~f]
                     (fn
                       ([] (~f))
                       ([~ret ~bind] ~body)
                       ([~ret ~@kv-args] (let ~kv-bind ~body))))))))]
    (quantum.core.macros/emit-comprehension &form
      {:emit-other (emit-fn (partial list `reduce)) :emit-inner (emit-fn list)}
      seq-exprs body-expr)))))

; <<<<------ ALREADY REDUCED ------>>>>
#?(:clj
(defmacro doseq+
  "|doseq| but based on reducers."
  {:attribution "Christophe Grand, https://gist.github.com/cgrand/5643767"}
  [bindings & body]
 `(reduce (constantly nil) (for+ ~bindings (do ~@body)))))

(defcurried each ; like doseq
  "Applies f to each item in coll, returns nil"
  {:attribution "transduce.reducers"}
  [f coll]
  (reduce (fn [_ x] (f x) nil) nil coll))

