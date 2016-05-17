(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support.

      Adds some interesting reducers and folders from different sources
      gleaned from the far reaches of the internet. Some of them have
      unexpectedly great performance."
      :author       "Rich Hickey"
      :contributors #{"Alan Malloy" "Alex Gunnarson" "Christophe Grand"}}
  quantum.core.reducers.fold
           (:refer-clojure :exclude [reduce into])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )        :as core  ]
                     [quantum.core.collections.base :as cbase ]
                     [quantum.core.data.map         :as map   ]
                     [quantum.core.data.set         :as set   ]
                     [quantum.core.data.vector      :as vec   
                       :refer [subvec+]                       ]
           #?@(:clj [[seqspert.hash-set                       ]
                     [seqspert.hash-map                       ]])
                     [quantum.core.fn               :as fn
                       :refer [#?@(:clj [f*n fn-> compr])
                               aritoid]                       ]
                     [quantum.core.logic            :as logic
                       :refer [#?@(:clj [fn-or fn-and whenf
                                         condf*n])
                               nnil?]                         ]
                     [quantum.core.macros           :as macros
                       :refer [#?@(:clj [defnt])]             ]
                     [quantum.core.reducers.reduce  :as red
                       :refer [#?@(:clj [reduce joinl])]      ]
                     [quantum.core.type             :as type
                       :refer [#?@(:clj [lseq? editable?
                                         hash-set? hash-map?
                                         ->joinable])
                               transient!* persistent!*]      ])
  #?(:cljs (:require-macros
                     [quantum.core.reducers.reduce  :as red
                       :refer [reduce joinl]                  ]
                     [quantum.core.fn               :as fn
                       :refer [f*n fn-> compr]                ]
                     [quantum.core.logic            :as logic
                       :refer [fn-or fn-and whenf condf*n]    ]
                     [quantum.core.macros           :as macros
                       :refer [defnt]                         ]
                     [quantum.core.type             :as type
                       :refer [lseq? editable? hash-set?
                               hash-map? ->joinable]          ])))

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
     (defn fjinvoke [f]
       (if (java.util.concurrent.ForkJoinTask/inForkJoinPool)
           (f)
           (.invoke ^java.util.concurrent.ForkJoinPool @pool ^java.util.concurrent.ForkJoinTask (fjtask f))))
     (defn fjfork [task] (.fork ^java.util.concurrent.ForkJoinTask task))
     (defn fjjoin [task] (.join ^java.util.concurrent.ForkJoinTask task)))
   ; Running a JDK < 7
   (do
     (def pool (delay (jsr166y.ForkJoinPool.)))
     (defn fjtask [^Callable f]
       (jsr166y.ForkJoinTask/adapt f))
     (defn fjinvoke [f]
       (if (jsr166y.ForkJoinTask/inForkJoinPool)
           (f)
           (.invoke ^jsr166y.ForkJoinPool @pool ^jsr166y.ForkJoinTask (fjtask f))))
     (defn fjfork [task] (.fork ^jsr166y.ForkJoinTask task))
     (defn fjjoin [task] (.join ^jsr166y.ForkJoinTask task)))))

#?(:cljs (defn fjtask   [f]     f    ))
#?(:cljs (defn fjinvoke [f]    (f)   ))
#?(:cljs (defn fjfork   [task] task  ))
#?(:cljs (defn fjjoin   [task] (task)))

;___________________________________________________________________________________________________________________________________
;=================================================={     FOLDING FUNCTIONS    }=====================================================
;=================================================={       (Generalized)      }=====================================================
(defprotocol CollFold
  (coll-fold [coll n combinef reducef]))

(defn fold-by-halves
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

#?(:clj
  (extend-type quantum.core.reducers.reduce.Folder
    CollFold
      (coll-fold [fldr n combine-fn reduce-fn]
        (coll-fold (:coll fldr) n combine-fn ((:transform fldr) reduce-fn)))))

(defn folder
  "Given a foldable collection, and a transformation function transform,
  returns a foldable collection, where any supplied reducing
  fn will be transformed by transform. transform is a function of
  reducing fn to reducing fn.

  Modifies reducers to not use Java methods but external extensions.
  This is because the protocol methods is not a Java method of the reducer
  object anymore and thus it can be reclaimed while the protocol method
  is executing."
  {:attribution "Christophe Grand - http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/"
   :todo ["Possibly fix the CLJS version?"]}
  ([coll transform]
    #?(:clj  (quantum.core.reducers.reduce.Folder. coll transform)
       :cljs (reify
               cljs.core/IReduce
               (-reduce [_ f1]
                 (reduce coll (transform f1) (f1)))
               (-reduce [_ f1 init]
                 (reduce coll (transform f1) init))
       
               CollFold
               (coll-fold [_ n combinef reducef]
                 (coll-fold coll n combinef (transform reducef)))))))

(def folder? #(instance? quantum.core.reducers.reduce.Folder %))

(def transformer? (fn-or red/reducer? folder?))

(defn folder->coll
  "Gets the original collection a folder was to reduce/fold over."
  [coll-0]
  (let [coll-n (volatile! (:coll coll-0))]
    (while (red/reducer? @coll-n)
      (vswap! coll-n :coll))
    @coll-n))

(def ^{:doc "Given a collection, determines its appropriate chunk size."}
  ->chunk-size
  #?(:clj  (let [from-proc (fn-> count dec  
                                 (quot (.. Runtime getRuntime availableProcessors))
                                 inc)]
             (condf*n
               (fn-and transformer? (fn-> folder->coll counted?))
                 (fn-> folder->coll from-proc)
               counted?
                 from-proc
               :else (constantly 512)))
     :cljs count)) ; Because it's only single-threaded anyway...

;___________________________________________________________________________________________________________________________________
;=================================================={           FOLD           }=====================================================
;=================================================={         (PREDUCE)        }=====================================================
(defn fold
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
   :contributors ["Alex Gunnarson"]}
  ([             reduce-fn coll] (fold reduce-fn reduce-fn coll))
  ([  combine-fn reduce-fn coll] (fold (->chunk-size coll) combine-fn reduce-fn coll))
  ([n combine-fn reduce-fn coll]
    (combine-fn ; single-arity combine-fn is the post-combine
      (coll-fold coll n
        (aritoid combine-fn nil
          (compr combine-fn reduce-fn)) ; single-arity reduce-fn is the post-reduce
        reduce-fn))))

(def preduce fold)

(defn pjoinl-fold
  "|pjoinl| using |fold|."
  [to from]
  (let [red-fn (if (editable? to) red/conj!-red red/conj-red)]
    (fold (aritoid #(->joinable to)                identity     #(joinl %1 %2))
          (aritoid #(transient!* (type/->base to)) persistent!* red-fn red-fn)
          from)))

(defnt pjoinl*
  {:attribution "Alex Gunnarson"
   :todo ["Shorten this code using type differences and type unions with |editable?|"
          "Handle arrays"]}
  ([to] to)
  ([^hash-set?   to from] (if (hash-set? from)  
                              (seqspert.hash-set/parallel-splice-hash-sets to from)
                              (pjoinl-fold to from)))
  ([^hash-map?   to from] (if (hash-map? from)
                              (seqspert.hash-map/parallel-splice-hash-maps to from)
                              (pjoinl-fold to from)))
  ([             to from] (if (nil? to) from (pjoinl-fold to from))))

(defn pjoinl
  "Parallel join, left.
   Like |joinl|, but is parallel."
  {:attribution "Alex Gunnarson"}
  ([] nil)
  ([to] to)
  ([to from] (pjoinl* to from))
  ([to from & froms]
    (reduce #(pjoinl* %1 %2) (pjoinl* to from) froms)))

(def pjoin pjoinl)

; TODO move
(defnt ->vec
  ([^vector? x] x)
  ([#{map? list? set? array-list? quantum.core.reducers.reduce.Folder
      #?(:clj  clojure.core.protocols.CollReduce
         :cljs cljs.core/IReduce)} x] (joinl [] x))
  ([x] (if (nil? x) [] [x])))
