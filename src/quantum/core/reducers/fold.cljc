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
  (:require
    [clojure.core                  :as core]
    [clojure.core.reducers         :as r]
    [quantum.core.collections.core
      :refer [joinl ?transient! ?persistent!]]
    [quantum.core.data.map         :as map]
    [quantum.core.data.set         :as set]
    [quantum.core.data.vector      :as vec
      :refer [subsvec]]
#?@(:clj
   [[seqspert.hash-set]
    [seqspert.hash-map]])
    [quantum.core.error
      :refer [TODO]]
    [quantum.core.fn               :as fn
      :refer [aritoid fn1 fn-> rcomp fn' fnl]]
    [quantum.core.logic            :as logic
      :refer [fn-or fn-and whenf condf1]]
    [quantum.core.macros           :as macros
      :refer [defnt]]
    [quantum.core.reducers.reduce  :as red
      :refer [reduce]]
    [quantum.core.type             :as t
      :refer [lseq? editable? ->joinable]]
    [quantum.untyped.core.reducers
      #?@(:cljs [:refer [Transformer]])])
  #?(:clj (:import [quantum.untyped.core.reducers Transformer]
                   [clojure.core.reducers CollFold])))

;___________________________________________________________________________________________________________________________________
;=================================================={        FORK/JOIN         }=====================================================
;=================================================={                          }=====================================================
#?(:clj
(doseq [v [#'r/fjfork #'r/fjinvoke #'r/fjjoin #'r/fjtask]]
  (alter-meta! v assoc :private false)))

#?(:clj
(defn fj-invoke-2-fns [f1 f2]
  (r/fjinvoke
   #(let [t2 (r/fjtask f2)]
      (r/fjfork t2)
      [(f1) (r/fjjoin t2)]))))

;___________________________________________________________________________________________________________________________________
;=================================================={     FOLDING FUNCTIONS    }=====================================================
;=================================================={       (Generalized)      }=====================================================
(declare fold-by-halves)

(defnt fold*
  {:adapted-from 'clojure.core.reducers}
  #?(:clj ([^+hash-map?              xs n combinef reducef]
            (.fold xs n combinef reducef r/fjinvoke r/fjtask r/fjfork r/fjjoin)))
          ([^clojure.data.avl.AVLMap xs n combinef reducef]
            (fold-by-halves
              (fn [xs' ^long ct]
                (let [split-ind (quot ct 2)]
                  (map/split-at split-ind xs')))
              xs n combinef reducef))
          ([^+vector?                   xs n combinef reducef]
            (fold-by-halves
              (fn [xs' ^long ct]
                (let [split-ind (quot ct 2)]
                  [(subvec xs' 0 split-ind) ; TODO test subvec against subsvec
                   (subvec xs'   split-ind ct)]))
              xs n combinef reducef))
          ([^transformer?            xs n combinef reducef]
            (fold* (.-prev xs) n combinef ((.-xf xs) reducef)))
  #?(:clj ([^CollFold                xs n combinef reducef]
            (r/coll-fold xs n combinef reducef)))
          ([^default                 xs n combinef reducef]
            (cond (nil? xs)
                    (combinef)
       #?@(:cljs [(satisfies? r/CollFold xs)
                    (r/coll-fold xs n combinef reducef)])
                  true
                    (reduce reducef (combinef) xs)))) ; TODO CollFold needs to be looked at here

(defn fold-by-halves
  "Folds the provided collection by halving it until it is smaller than the
  requested size, and folding each subsection. halving-fn will be passed as
  input a collection and its size (so you need not recompute the size); it
  should return the left and right halves of the collection as a pair. Those
  halves will normally be of the same type as the parent collection, but
  anything foldable is sufficient.

  Generalized from `foldvec` to work for anything you can split in half."
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
                  child-fn (fn [child] #(fold* child n combinef reducef))]
              (r/fjinvoke
               #(let [f1 (child-fn left)
                      t2 (r/fjtask (child-fn right))]
                  (r/fjfork t2)
                  (combinef (f1) (r/fjjoin t2)))))
           :cljs
             (reduce reducef (combinef) coll)))))

;___________________________________________________________________________________________________________________________________
;=================================================={           FOLD           }=====================================================
;=================================================={           into           }=====================================================
(defnt transformer->coll
  "Gets the original collection a transformer was to reduce/fold over."
  ([^Transformer x] (.-xs x)))

(def ^{:doc "Given a collection, determines its appropriate chunk size."}
  ->chunk-size
  #?(:clj  (let [from-proc (fn-> count dec
                                 (quot (.. Runtime getRuntime availableProcessors))
                                 inc)]
             (condf1
               (fn-and t/transformer? (fn-> transformer->coll counted?))
                 (fn-> transformer->coll from-proc)
               counted?
                 from-proc
               (fn' 512)))
     :cljs count)) ; Because it's only single-threaded anyway... ; TODO but if we want to use webworkers this is not right
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
  ->`combinef` must be associative. When called with no
  arguments, (combinef) must produce its identity element.
  These operations may be performed in parallel, but the results will preserve order."
  {:added "1.5"
   :adapted-from "clojure.core.reducers"
   :contributors ["Alex Gunnarson"]
   :todo         #{"add support for customization of parallelism"}}
  ([     rf xs] (fold rf rf xs))
  ([  cf rf xs] (fold (->chunk-size xs) cf rf xs))
  ([n cf rf xs]
    (cf (fold* xs n
          (aritoid rf nil
            (fn [a b] (cf (rf a) (rf b))))
          rf))))

(def preduce fold)

(defn pjoinl-fold
  "`pjoinl` using `fold`."
  [to from]
  (let [red-fn (if (editable? to) red/conj!-red red/conj-red)]
    (fold (aritoid nil                             (fn1 ?persistent!) #(joinl %1 %2))
          (aritoid #(?transient! (t/->base to)) (fn1 ?persistent!) red-fn red-fn)
          from)))

(defnt pjoinl*
  {:attribution "alexandergunnarson"
   :todo ["Shorten this code using type differences and type unions with |editable?|"
          "Handle arrays"]}
  ([^default        to  ] to)
  ([^transformer?   from] (pjoinl-fold [] from))
  ([^+unsorted-set? to from] #?(:clj  (if (t/+unsorted-set? from)
                                          (seqspert.hash-set/parallel-splice-hash-sets to from)
                                          (pjoinl-fold to from))
                                :cljs (pjoinl-fold to from)))
  ([^+hash-map?     to from] #?(:clj  (if (t/+hash-map? from)
                                          (seqspert.hash-map/parallel-splice-hash-maps to from)
                                          (pjoinl-fold to from))
                                :cljs (pjoinl-fold to from)))
  ([^default        to from] (if (nil? to) from (pjoinl-fold to from))))

(defn pjoinl
  "Parallel join, left.
   Like `joinl`, but is parallel."
  {:attribution "alexandergunnarson"}
  ([]         nil)
  ([to]      (pjoinl* to))
  ([to from] (pjoinl* to from))
  ([to from & froms] (reduce #(pjoinl* %1 %2) (pjoinl* to from) froms)))

; TODO make pjoin actually parallel in CLJS via WebWorkers

(def pjoin pjoinl)

(defn pjoin' [& args] (TODO))

; TODO use uncomplicate/clojurecl for lower-level parallel operations
