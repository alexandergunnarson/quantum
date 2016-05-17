(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support.

      Adds some interesting reducers and folders from different sources
      gleaned from the far reaches of the internet. Some of them have
      unexpectedly great performance."
      :author       "Rich Hickey"
      :contributors #{"Alan Malloy" "Alex Gunnarson" "Christophe Grand"}
      :cljs-self-referring? true}
  quantum.core.reducers
           (:refer-clojure :exclude [reduce #?@(:clj [Range ->Range])])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )        :as core                ]
                     [quantum.core.collections.base :as cbase               ]
                     [quantum.core.data.map         :as map                 ]
                     [quantum.core.data.set         :as set                 ]
                     [quantum.core.data.vector      :as vec                 
                       :refer [catvec subvec+]                              ]
                     [quantum.core.error            :as err               
                       :refer [->ex]                                        ]
                     [quantum.core.fn               :as fn
                       :refer [#?@(:clj [f*n fn-> fn->> compr defcurried rfn])
                               call firsta monoid aritoid]                   ]
                     [quantum.core.logic            :as logic
                       :refer [#?@(:clj [fn-not fn-or fn-and
                                         whenf whenf*n ifn condf condf*n])
                               nnil?]                                       ]
                     [quantum.core.macros           :as macros
                       :refer [#?@(:clj [defnt])]                           ]
                     [quantum.core.numeric          :as num                 ]
                     [quantum.core.type             :as type
                       :refer [#?@(:clj [array-list? lseq?]) instance+?]    ]
                     [quantum.core.reducers.reduce  :as red
                       :refer [reducer first-non-nil-reducer]               ]
                     [quantum.core.reducers.fold    :as fold
                       :refer [folder coll-fold CollFold 
                               fjinvoke fjtask fjfork fjjoin]               ]
                     [quantum.core.vars             :as var
                       :refer [#?(:clj defalias)]                           ])
  #?(:cljs (:require-macros
                     [quantum.core.fn               :as fn
                       :refer [f*n fn-> fn->> compr defcurried rfn]         ]
                     [quantum.core.logic            :as logic
                       :refer [fn-not fn-or fn-and whenf whenf*n ifn condf
                               condf*n]                                     ]
                     [quantum.core.macros           :as macros
                       :refer [defnt]                                       ]
                     [quantum.core.numeric          :as num                 ]
                     [quantum.core.type             :as type
                       :refer [array-list? lseq?]                           ]
                     [quantum.core.reducers         :as red
                       :refer [reduce join]                                 ]
                     [quantum.core.vars             :as var
                       :refer [defalias]                                    ])))

(comment
  "How a reducer works:"
  (->> coll (map+ inc) (map+ triple) (join []))
  "Results in this reducer:"
  (fn reducer [acc x]
    (let [reducer1 (fn reducer [acc x]
                     (conj acc (triple x)))]
      (reducer1 acc (inc x))))
  "Which is used as the reducing function to reduce into a vector.")

#?(:clj (defalias join   red/join  ))
#?(:clj (defalias pjoin  fold/pjoin))
#?(:clj (defalias reduce red/reduce))
        (defalias fold   fold/fold )
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
; intermediate and as such don't need to be persistent but, on the whole, you still need the laziness.
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
    (let [f1 (clojure.core/reduce #(cons %2 %1) nil ; Note that the captured function (f1) may be impure, so don't share it!
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

(defn reduce-count
  {:attribution "parkour.reducers"
   :performance "On non-counted collections, |count| is 71.542581 ms, whereas
                 |count*| is 36.824665 ms - twice as fast!!"}
  [coll]
  (reduce (compr firsta inc) 0 coll))

(defn fold-count
  {:attribution "parkour.reducers"
   :performance "On non-counted collections, |count| is 71.542581 ms, whereas
                 |count*| is 36.824665 ms - twice as fast!!"}
  [coll]
  (fold
    (aritoid (constantly 0) identity +)
    (aritoid (constantly 0) identity (compr firsta inc))
    coll))
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
          (let [[state* x*] (f @state x)] ; How about using volatiles here?
            (reset! state state*)
            (f1 acc x*)))))))

(defcurried mapcat-state
  "Like mapcat, but threads a state through the sequence of transformations. ; so basically like /reductions/?
  For each x in coll, f is applied to [state x] and should return [state' xs].
  The result is the concatenation of each returned xs."
  {:attribution "transduce.reducers"}
  [f init coll]
  (reducer coll
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
      (#?(:clj clojure.core.protocols/coll-reduce :cljs reduce) this f1 (f1)))
    (#?(:clj coll-reduce :cljs -reduce) [_  f1 init]
      (#?(:clj clojure.core.protocols/coll-reduce :cljs reduce)
       right f1
       (#?(:clj clojure.core.protocols/coll-reduce :cljs reduce) left f1 init)))

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
  ([left right]
    (cond
      (zero? (count left )) right ; count* takes longer, because /count/ for ArrayLists is O(1)
      (zero? (count right)) left
      :else
      (Cat. (+ (count left) (count right)) left right))))

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
  (fold cat+ append! coll))
;___________________________________________________________________________________________________________________________________
;=================================================={           MAP            }=====================================================
;=================================================={                          }=====================================================
(defcurried map+
  "Applies f to every value in the reduction of coll. Foldable."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [f coll]
  (folder coll
    (fn [reducer-]
      (rfn [reducer- k]
           ([ret k v]
              (reducer- ret (f k v)))))))

(defn map-indexed+
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
; mapcat: ([:a 1] [:b 2] [:c 3]) versus mapcat+: (:a 1 :b 2 :c 3) ; hmm...

(defcurried mapcat+
  "Applies f to every value in the reduction of coll, concatenating the result
  colls of (f val). Foldable."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [f coll]
  (folder coll
    (fn [f1-0]
      (let [reducer- (fn
                 ([]
                   (let [x (f1-0        )] (if (reduced? x) (reduced x) x)))
                 ([ret]
                   (let [x (f1-0 ret    )] (if (reduced? x) (reduced x) x)))
                 ([ret v]
                   (let [x (f1-0 ret   v)] (if (reduced? x) (reduced x) x)))
                 ([ret k v]
                   (let [x (f1-0 ret k v)] (if (reduced? x) (reduced x) x))))]
        (rfn [reducer- k]
             ([ret k v]
                (reduce reducer- ret (f k v))))))))

(defn concat+ [& args] (reduce cat+ args))
;___________________________________________________________________________________________________________________________________
;=================================================={        REDUCTIONS        }=====================================================
;=================================================={                          }=====================================================
(defn reductions+
  "Reducers version of /reductions/.
   Returns a reducer of the intermediate values of the reduction (as per reduce) of coll by f.
   "
  {:attribution "parkour.reducers"
   :usage '(join [] (reductions+ + [1 2 3]))
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
              init)))))
;___________________________________________________________________________________________________________________________________
;=================================================={      FILTER, REMOVE      }=====================================================
;=================================================={                          }=====================================================
(defcurried filter+
  "Returns a version of the folder which only passes on inputs to subsequent
   transforms when (@pred <input>) is truthy."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [pred coll]
  (folder coll
    (fn [reducer-]
      (rfn [reducer- k]
           ([acc k v]
              (if (pred k v)
                  (reducer- acc k v)
                  acc))))))

(defcurried remove+
  "Returns a version of the folder which only passes on inputs to subsequent
   transforms when (@pred <input>) is falsey."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [pred coll]
  (filter+ (complement pred) coll))

(def keep+ (compr map+ (partial remove+ nil?))) 
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
                (#?(:clj clojure.core.protocols/coll-reduce :cljs -reduce)
                   (flatten+ v) reducer- ret)
                (reducer- ret v)))))))

(def flatten-1+ (fn->> (mapcat+ identity)))
;___________________________________________________________________________________________________________________________________
;=================================================={          REPEAT          }=====================================================
;=================================================={                          }=====================================================

;___________________________________________________________________________________________________________________________________
;=================================================={         ITERATE          }=====================================================
;=================================================={                          }=====================================================
(defcurried iterate+
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
      (fold/fold-by-halves
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
  ([              ] (iterate+ inc 0))
  ([      end     ] (quantum.core.reducers/Range. 0     end 1))
  ([start end     ] (quantum.core.reducers/Range. start end 1))
  ([start end step] (quantum.core.reducers/Range. start end step)))
;___________________________________________________________________________________________________________________________________
;=================================================={     TAKE, TAKE-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defcurried take+
  "Ends the reduction of coll after consuming n values."
  {:added "1.5"
   :attribution  "clojure.core.reducers"
   :contributors "Alex Gunnarson"}
  [n coll]
  (reducer coll
    (fn [reducer-]
      (let [cnt (atom n)]
        (rfn [reducer- k]
          ([ret k v]
             (swap! cnt dec)
             (if (neg? @cnt)
               (reduced ret)
               (reducer- ret k v))))))))

(defcurried take-while+
  "Ends the reduction of coll when (pred val) returns logical false."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [pred coll]
  (reducer coll
    (fn [f1]
      (rfn [f1 k]
           ([ret k v]
              (if (pred k v)
                  (f1 ret k v)
                  (reduced ret)))))))

#?(:clj
(defn taker+
  {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
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

;___________________________________________________________________________________________________________________________________
;=================================================={     DROP, DROP-WHILE     }=====================================================
;=================================================={                          }=====================================================
(defcurried drop+
  "Elides the first n values from the reduction of coll."
  {:added "1.5"
   :attribution "clojure.core.reducers"}
  [n coll]
  (reducer coll
    (fn [f1]
      (let [cnt (atom n)]
        (rfn [f1 k]
          ([ret k v]
             (swap! cnt dec)
             (if (neg? @cnt)
               (f1 ret k v)
               ret)))))))

(defcurried drop-while+
  "Skips values from the reduction of coll while (pred val) returns logical true."
  {:attribution "Alan Malloy - http://dev.clojure.org/jira/browse/CLJ-993"}
  [pred coll]
  (reducer coll
    (fn [f1]
      (let [keeping? (atom false)]
        (rfn [f1 k]
          ([ret k v]
             (if (or @keeping?
                     (reset! keeping? (not (pred k v))))
               (f1 ret k v)
               ret)))))))

#?(:clj
  (defn dropr+ ; This is extremely slow by comparison. About twice as slow
    {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/1388ev2krx/butlast-with-reducers"}
    [n coll]
     (reducer coll
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

(defn group-by+ ; Yes, but folds a lazy sequence... hmm...
  "Reducers version. Possibly slower than |core/group-by|.
   A terminal transform."
  {:attribution "Christophe Grand - http://grokbase.com/t/gg/clojure/12c3k7ztbz/group-by-vs-reducers"
   :usage '(group-by+ odd? (range 10))
   :out   '{false [0 2 4 6 8], true [1 3 5 7 9]}}
  [f coll]
  (fold
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
(defn distinct-by+ ; 228.936664 ms (pretty much attains java speeds!!!)
  "Remove adjacent duplicate values of (@f x) for each x in @coll.
   CAVEAT: Requires @coll to be sorted to work correctly."
  {:attribution "parkour.reducers"}
  [f coll]
  (let  #?(:clj  [sentinel (Object.)] ; instead of nil, because it's unique
           :cljs [sentinel (array  )])
    (->> (apply concat+ [coll [sentinel]])
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
  [coll] (->> coll (distinct-by+ identity)))

 
(defn fold-frequencies
  "Like clojure.core/frequencies, returns a map of inputs to the number of
   times those inputs appeared in the collection.
   A terminal transform."
  {:todo ["Can probably make this use transients in the reducing fn."]}
  [coll]
  (fold
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
  (fold
    (aritoid (constantly nil) identity first-non-nil-reducer)
    (aritoid (constantly nil)
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
         (aritoid (constantly nil) identity first-non-nil-reducer)]
    (fold combiner+reducer combiner+reducer coll)))

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
          (aritoid (constantly nil) identity extremum-reducer)]
    (fold combiner+reducer combiner+reducer coll)))

(defn fold-min
  "Finds the smallest value using `compare`."
  {:tests '{(->> [:a :b :c :d] (fold-min))
            :a}}
  [coll]
  (->> coll (fold-extremum (compr compare -))))

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
       (map+ (constantly true))
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
    (folder coll
      (fn [reducer-]
        (rfn [reducer- k]
          ([ret k v]
            (if (< (count ret) coll-ct)
                (do (reducer- ret k [v nil]))
                (do  ; This part is problematic
                    (swap! ind-n inc)
                    (reducer- (assoc! ret (dec @ind-n) [(-> ret (get (dec @ind-n)) (get 0)) v]) k nil)))))))))
(defn zipvec+
  ([vec-0]
    (->> vec-0 zipvec* (take+ (/ (count vec-0) 2))))
  ([vec-0 & vecs]
    (->> vecs (apply map vector vec-0) fold)))
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
                             (if foldable `folder `reducer)
                             (if foldable `folder `reducer)) ; (if foldable `r/folder `r/reducer)
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
    seq-exprs body-expr))))

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
              result ((:post-combiner fold) combined)
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
  (assert (not (folder? downstream)) "|fuse| is a terminal transform")
  (let [ks             (vec (keys fold-map))
        n              (count ks)
        folders        (mapv (comp compile-fold (partial get fold-map)) ks)
        reducers       (mapv :reducer folders)
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