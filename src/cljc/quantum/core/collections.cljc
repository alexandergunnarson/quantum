(ns
  ^{:doc
      "Various collections functions.

       Includes better versions of the following than clojure.core:

       for, doseq, repeat, repeatedly, range, merge,
       count, vec, reduce, into, first, second, rest,
       last, butlast, get, pop, peek ...

       and more.

       Many of them are aliased from other namespaces like
       quantum.core.collections.core, or quantum.core.reducers."
    :attribution "Alex Gunnarson"
    :cljs-self-referencing? true}
  quantum.core.collections
           (:refer-clojure :exclude
             [for doseq reduce
              contains?
              repeat repeatedly
              interpose
              range
              take take-while
              drop  drop-while
              subseq
              key val
              merge sorted-map sorted-map-by
              into
              count
              vec empty empty?
              split-at
              first second rest last butlast get pop peek
              select-keys
              zipmap
              reverse
              conj
              conj! assoc! dissoc! disj!
              #?(:cljs boolean?)])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )                  :as core   ]
                     [quantum.core.data.map                   :as map    ]
                     [quantum.core.data.set                   :as set    ]
                     [quantum.core.data.vector                :as vec  
                       :refer [catvec subvec+]                           ]
                     [quantum.core.collections.base           :as base   ]
                     [quantum.core.collections.core           :as coll   ]
                     [quantum.core.collections.sociative      :as soc    ]
                     [quantum.core.collections.differential   :as diff   ]
                     [quantum.core.collections.generative     :as gen    ]
                     [quantum.core.collections.map-filter     :as mf     ]
                     [quantum.core.collections.selective      :as sel    ]
                     [quantum.core.collections.tree           :as tree   ]
                     [quantum.core.error                      :as err  
                       :refer [->ex]                                     ]
                     [quantum.core.fn                         :as fn  
                       :refer [#?@(:clj [compr <- fn-> fn->>  
                                         f*n]) fn-nil juxt-kv withf->>]  ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic
                       :refer [#?@(:clj [fn-not fn-or fn-and whenf whenf*n
                                         ifn if*n condf condf*n]) nnil? any?]]
                     [quantum.core.macros                     :as macros 
                       :refer [#?@(:clj [defnt])]                        ]
                     [quantum.core.reducers                   :as red    ]
                     [quantum.core.string                     :as str    ]
                     [quantum.core.string.format              :as sform  ]
                     [quantum.core.type                       :as type  
                       :refer [#?@(:clj [lseq? transient? editable? 
                                         boolean? should-transientize?])]]
                     [quantum.core.analyze.clojure.predicates :as anap   ]
                     [quantum.core.type.predicates            :as tpred  ]
                     [clojure.walk                            :as walk   ]
                     [quantum.core.loops                      :as loops  ]
                     [quantum.core.vars                       :as var  
                       :refer [#?@(:clj [defalias])]                     ])
  #?(:cljs (:require-macros  
                     [quantum.core.collections.core           :as coll   ]
                     [quantum.core.collections.differential   :as diff   ]
                     [quantum.core.collections     
                       :refer [for lfor doseq doseqi reduce reducei
                               seq-loop
                               count lasti
                               subseq
                               contains? containsk? containsv?
                               index-of last-index-of
                               first second rest last butlast get pop peek
                               conjl conj! assoc! dissoc! disj!
                               map-entry]]
                     [quantum.core.fn                         :as fn
                       :refer [compr <- fn-> fn->> f*n]                  ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic 
                       :refer [fn-not fn-or fn-and whenf whenf*n 
                               ifn if*n condf condf*n]                   ]
                     [quantum.core.loops                      :as loops  ]
                     [quantum.core.macros                     :as macros 
                       :refer [defnt]                                    ]
                     [quantum.core.reducers                   :as red    ]
                     [quantum.core.type                       :as type 
                       :refer [lseq? transient? editable? boolean? 
                               should-transientize?]                     ]
                     [quantum.core.vars                       :as var 
                       :refer [defalias]                                 ])))

(defalias key     coll/key    )
(defalias val     coll/val    )
(defalias reverse coll/reverse)

#?(:clj (defmacro map-entry [a b] `[~a ~b]))

(defn genkeyword
  ([]    (keyword (gensym)))
  ([arg] (keyword (gensym arg))))

(defn wrap-delay [f]
  (if (delay? f) f (delay ((or f fn-nil)))))

; ; ====== COLLECTIONS ======

#?(:clj (defalias index-of      coll/index-of     ))
#?(:clj (defalias last-index-of coll/last-index-of))
#?(:clj (defalias count         coll/count        ))
#?(:clj (defalias lasti         coll/lasti        ))
#?(:clj (defalias getr          coll/getr         ))
#?(:clj (defalias subseq        getr              ))
        (defalias lsubseq       core/subseq       )
#?(:clj (defalias get           coll/get          ))
        (defalias gets          coll/gets         )
        (defalias getf          coll/getf         )

; ; If not |defalias|ed, "ArityException Wrong number of args (2) passed to: core/eval36441/fn--36457/G--36432--36466"
#?(:clj (defalias conjl         coll/conjl        ))
#?(:clj (defalias conjr         coll/conjr        ))
#?(:clj (defalias pop           coll/pop          ))
#?(:clj (defalias popl          coll/popl         ))
#?(:clj (defalias popr          coll/popr         ))
#?(:clj (defalias peek          coll/peek         ))
#?(:clj (defalias first         coll/first        ))
#?(:clj (defalias second        coll/second       ))
        (defalias third         coll/third        )
#?(:clj (defalias rest          coll/rest         ))
        (defalias lrest         core/rest         )
#?(:clj (defalias butlast       coll/butlast      ))
#?(:clj (defalias last          coll/last         ))
#?(:clj (defalias assoc!        coll/assoc!       ))
#?(:clj (defalias dissoc!       coll/dissoc!      ))
        (defalias conj          coll/conj         )
#?(:clj (defalias conj!         coll/conj!        ))
#?(:clj (defalias disj!         coll/disj!        ))
#?(:clj (defalias update!       coll/update!      ))
#?(:clj (defalias contains?     coll/contains?    ))
#?(:clj (defalias containsk?    coll/containsk?   ))
#?(:clj (defalias containsv?    coll/containsv?   ))
#?(:clj (defalias empty?        coll/empty?       ))
#?(:clj (defalias empty         coll/empty        ))
#?(:clj (defalias array         coll/array        ))
        (defalias join          red/join          )
        (defalias joinl         red/join          )
        (defalias pjoin         red/pjoin         )
        (defalias pjoinl        red/pjoin         )

        (defalias fold          red/fold          )
        (defalias cat+          red/cat+          )
        (defalias foldcat+      red/foldcat+      )
        (defalias indexed+      red/indexed+      )
        (defalias reductions+   red/reductions+   )
        (defalias ltake         diff/ltake        )
        (defalias take+         diff/take+        )
#?(:clj (defalias taker+        diff/taker+       ))
        (defalias take-while+   diff/take-while+  )
        (defalias take-after    diff/take-after   )
        (defalias taker-after   diff/taker-after  )
        (defalias take-until    diff/take-until   )
#?(:clj (defalias taker-until   diff/taker-until  ))
        (defalias drop          diff/drop         )
        (defalias drop+         diff/drop+        )
        (defalias dropl         diff/dropl        )
        (defalias ldropl        diff/ldropl       )
        (defalias drop-while+   red/drop-while+   )
        (defalias dropr         diff/dropr        )
#?(:clj (defalias dropr+        diff/dropr+       ))
        (defalias dropr-until   diff/dropr-until  )
        (defalias group-by+     red/group-by+     )
        (defalias flatten+      red/flatten+      )
        (defalias flatten-1+    red/flatten-1+    )
        (defalias iterate+      red/iterate+      )
        (defalias reduce-by+    red/reduce-by+    )
        (defalias distinct-by+  red/distinct-by+  )
        (defalias distinct+     red/distinct+     )
        (defalias zipvec+       red/zipvec+       )
        ; for+
        ; doseq+
        
        (def flatten-1 (partial apply concat)) ; TODO more efficient

; _______________________________________________________________
; ============================ LOOPS ============================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
#?(:clj (defalias reduce   loops/reduce  ))
#?(:clj (defalias reduce-  loops/reduce- ))
#?(:clj (defalias reducei  loops/reducei ))
#?(:clj (defalias reducei- loops/reducei-))
        (defalias reduce-2 loops/reduce-2)
#?(:clj (defalias seq-loop loops/seq-loop))
#?(:clj (defalias loopr    loops/seq-loop))
;#?(:clj(defalias for+     red/for+      )) ; TODO have this
#?(:clj (defalias for      loops/for     )) #?(:clj (alter-meta! (var for) assoc :macro true))
#?(:clj (defalias fori     loops/fori    ))
#?(:clj (defalias for-m    loops/for-m   ))
#?(:clj (defalias until    loops/until   ))
#?(:clj (defmacro lfor   [& args] `(loops/lfor   ~@args)))
#?(:clj (defmacro doseq  [& args] `(loops/doseq  ~@args)))
#?(:clj (defmacro doseqi [& args] `(loops/doseqi ~@args)))

        (defalias break reduced)
; _______________________________________________________________
; ========================= GENERATIVE ==========================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
        (defalias repeat        gen/repeat        )
        (defalias lrepeat       gen/lrepeat       )
#?(:clj (defalias repeatedly    gen/repeatedly    ))
        (defalias lrepeatedly   gen/repeatedly    )
        (defalias range         gen/range         )
        (defalias rrange        gen/rrange        )
        (defalias range+        gen/range+        )
        (defalias lrange        gen/lrange        )
        (defalias lrrange       gen/lrrange       )
; _______________________________________________________________
; ================== FULL-SEQUENCE TRANSFORMS ===================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
        (defalias map+            mf/map+            )
        (defalias lmap            mf/lmap            )
        (defalias map-indexed+    mf/map-indexed+    )
        (defalias map-keys+       mf/map-keys+       )
        (defalias map-vals+       mf/map-vals+       )
        (defalias filter+         mf/filter+         )
        (defalias filter-keys+    mf/filter-keys+    )
        (defalias filter-vals+    mf/filter-vals+    )
        (defalias lfilter         mf/lfilter         )
        (defalias ffilter         mf/ffilter         )
        (defalias remove+         mf/remove+         )
        (defalias remove-keys+    mf/remove-keys+    )
        (defalias remove-vals+    mf/remove-vals+    )
        (defalias lremove         mf/lremove         )
        (defalias keep+           red/keep+          )
        (defalias mapcat+         red/mapcat+        )
; _______________________________________________________________
; ============================ TREE =============================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
#?(:clj (defalias postwalk        tree/postwalk      ))
#?(:clj (defalias prewalk         tree/prewalk       ))
; _______________________________________________________________
; ======================== COMBINATIVE ==========================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
        (defalias zipmap          core/zipmap        )
        (defalias merge           map/merge          )
        (defalias sorted-map      map/sorted-map     )
        (defalias sorted-map-by   map/sorted-map-by  )
; _______________________________________________________________
; ========================== SOCIATIVE ==========================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••
        (defalias assoc+          soc/assoc+         )
        (defalias assocs-in+      soc/assocs-in+     )
        (defalias dissoc-in+      soc/dissoc-in+     )
        (defalias update-val+     soc/update-val+    )
        (defalias assoc-when-none soc/assoc-when-none)
        (defalias assoc-with      soc/assoc-with     )



(defn deep-merge
  "Like merge, but merges maps recursively."
  {:from "r0man/noencore"}
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn deep-merge-with
  "Like merge-with, but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level."
  {:from "r0man/noencore"}
  [f & maps]
  (apply
   (fn m [& maps]
     (if (every? map? maps)
       (apply merge-with m maps)
       (apply f maps)))
   maps))

(defn merge-with-set [m1 m2]
  (merge-with (fn [v1 v2] (if (set? v1)
                              (if (set? v2)
                                  (set/union v1 v2)
                                  (conj v1 v2))
                              (if (set? v2)
                                  (conj v2 v1)
                                  #{v1 v2}))) m1 m2))

(defn compact-map
  "Removes all map entries where the value of the entry is empty."
  {:from "r0man/noencore"}
  [m]
  (reduce
   (fn [m k]
     (let [v (get m k)]
       (if (or (nil? v)
               (and (or (map? v)
                        (sequential? v))
                    (empty? v)))
         (dissoc m k) m)))
   m (keys m)))



(defn containsv? [super sub]
  (if (string? super)
      (not= -1 (.indexOf ^String super sub)) ; because .contains is not supported in JS
      (throw (->ex :not-implemented))))

; TODO extract code pattern for lazy transformation
(defn lflatten
  "Like #(apply concat %), but fully lazy: it evaluates each sublist
   only when it is needed."
  {:from "clojure.algo.monads"}
  [ss]
  (lazy-seq
   (when-let [s (seq ss)]
     (concat (first s) (lflatten (rest s))))))

(defalias concatv catvec)
; TODO generalize concat
(defalias lconcat core/concat)

(defalias safe-mapcat anap/safe-mapcat)

(defn dezip
  "The inverse of zip. — Unravels a seq of m n-tuples into a
  n-tuple of seqs of length m.
  Example:
    (dezip '([11 12] [21 22] [31 32] [41 42]))
      ;=> [(11 21 31 41) (12 22 32 42)]
  Umm, actually there is no zip in Clojure. Instead, you'd use this:
    (apply map vector ['(11 21 31 41) '(12 22 32 42)])
      ;=> ([11 12] [21 22] [31 32] [41 42]))
  Note that I'm using lists here only for demonstrating that we're not limited
  to vectors."
  {:from "theatralia.database.txd-gen"}
  [s]
  (let [tuple-size (count (first s))
        s-seq (seq s)]
    (mapv (fn [n]
            (map #(nth % n) s-seq))
          (range tuple-size))))

(defn- flatten-map
  "Flatten a map into a seq of alternate keys and values"
  {:from "clojure.tools/reader"}
  [form]
  (loop [s (seq form) key-vals (transient [])]
    (if s
      (let [e (first s)]
        (recur (next s) (-> key-vals
                          (conj! (key e))
                          (conj! (val e)))))
      (seq (persistent! key-vals)))))

; ----- META ----- ;

(defn merge-meta
  "Returns an object of the same type and value as `obj`, with its
   metadata merged over `m`."
  {:from "cljs.tools/reader"}
  [obj m]
  (let [orig-meta (meta obj)]
    (with-meta obj (merge m (dissoc orig-meta :source)))))

(defnt into!
  "Like into, but for transients"
  [^transient? x coll]
  (doseq [elem coll] (conj! x elem)) x)

(defn butlast+last
  "Returns same value as (juxt butlast last), but slightly more
   efficient since it only traverses the input sequence s once, not
   twice."
  {:from "clojure/tools.analyzer/utils"}
  [s]
  (loop [butlast (transient [])
         s s]
    (if-let [xs (next s)]
      (recur (conj! butlast (first s)) xs)
      [(seq (persistent! butlast)) (first s)])))

(defn update-vals
  "Applies f to all the vals in the map"
  [m f]
  (reduce-kv (fn [m k v] (assoc m k (f v))) {} (or m {})))

(defn update-keys
  "Applies f to all the keys in the map"
  [m f]
  (reduce-kv (fn [m k v] (assoc m (f k) v)) {} (or m {})))

(defn update-kv
  "Applies f to all the keys and vals in the map"
  [m f]
  (reduce-kv (fn [m k v] (assoc m (f k) (f v))) {} (or m {})))

(def mmerge
  "Same as (fn [m1 m2] (merge-with merge m2 m1))"
  #(merge-with merge %2 %1))

(defn mapv'
  "Like mapv, but short-circuits on reduced"
  {:from "clojure.tools.analyzer.utils"}
  [f v]
  (let [c (count v)]
    (loop [ret (transient []) i 0]
      (if (> c i)
        (let [val (f (nth v i))]
          (if (reduced? val)
            (reduced (persistent! (reduce #(conj! %1 %2) (conj! ret @val) (subvec v (inc i)))))
            (recur (conj! ret val) (inc i))))
        (persistent! ret)))))

(def frest (fn-> rest first))

; ----- SPLIT ----- ;

(def ^{:doc "split the given collection at the given index; similar to
             clojure.core/split-at, but operates on and returns data.avl
             collections"}
  split-at clojure.data.avl/split-at)


; ----- MISCELLANEOUS ----- ;

(defn abs-difference 
  "Returns the absolute difference between a and b.
   That is, (a diff b) union (b diff a)."
  {:out 'Set
   :todo ["Probably a better name for this."]}
  [a b]
  (set/union
    (set/difference a b)
    (set/difference b a)))

; ================================================ INDEX-OF ================================================
(defn seq-contains?
  "Like |contains?|, but tests if a collection @super contains
   all the constituent elements of @sub, in the order in which they
   appear in @sub.

   A prime example would be substrings within strings."
  [super sub]
  (nnil? (index-of super sub)))

(defn indices-of-elem
  {:todo ["Make parallizeable"]}
  [coll elem-0]
  (if (should-transientize? coll)
      (persistent!
        (loops/reducei
          (fn [ret elem-n n]
            (if (= elem-0 elem-n)
                (conj! ret n)
                ret))
          (transient [])
          coll))
      (loops/reducei
        (fn [ret elem-n n]
          (if (= elem-0 elem-n)
              (conj ret n)
              ret))
        []
        coll)))

(defn indices-of
  {:todo ["Make parallizeable"
          "|drop| is a performance killer here"]}
  [coll elem-0]
  (loop [coll-n coll indices []]
    (let [i (index-of coll-n elem-0)]
      (if-not i
        indices
        (recur
          (drop (+ i (count elem-0)) coll-n)
          (conj indices
            (+ i (if-let [li (last indices)]
                   (+ li (count elem-0))
                   0))))))))

(defn lindices-of
  "Lazy |indices-of|."
  {:source "zcaudate/hara.data.seq"}
  [pred coll]
  (keep-indexed
    (fn [idx x]
      (when (pred x)
        idx))
    coll))





; ================================================ MERGE ================================================

(defn index-with [coll f]
  (->> coll
       (map+ #(map-entry (f %) %))
       (join {})))

(defn mergel  [a b] (merge b a))
(defalias merge-keep-left mergel)
(defn merger [a b] (merge a b))
(defalias merge-keep-right merger)
            
(defn split-remove
  {:todo ["Slightly inefficient — two /index-of/ implicit."]}
  [split-at-obj coll]
  [(take-until split-at-obj coll)
   (take-after split-at-obj coll)])

#_(defn zipmap
  ([ks vs] (zipmap hash-map ks vs))
  ([map-gen-fn ks-0 vs-0]
    (loop [map (map-gen-fn)
           ks (seq ks-0)
           vs (seq vs-0)]
      (if (and ks vs)
        (recur (assoc map (first ks) (first vs))
               (next ks)
               (next vs))
        map))))

#?(:clj (defalias kmap base/kmap))
#?(:clj (defalias eval-map base/eval-map))

(defn select
  "Applies a list of functions, @fns, separately to an object, @coll.
   A good use case is returning values from an associative structure with keys as @fns.
   Returns a vector of the results."
  ^{:attribution "Alex Gunnarson"
    :usage "(select {:a 1 :b [3]} :a (compr :b 0)) => [1 3]"}
  [coll & fns]
  ((apply juxt fns) coll))

(defn comparator-extreme-of
  "For compare-fns that don't have enough arity to do, say,
   |(apply time/latest [date1 date2 date3])|.

   Gets the most \"extreme\" element in collection @coll,
   \"extreme\" being defined on the @compare-fn.

   In the case of |time/latest|, it would return the latest
   DateTime in a collection.

   In the case of |>| (greater than), it would return the
   greatest element in the collection:

   (comparator-extreme-of [1 2 3] (fn [a b] (if (> a b) a b)) )
   :: 3

   |(fn [a b] (if (> a b) a b))| is the same thing as
   |(choice-comparator >)|."
  {:todo ["Rename this function."
          "Possibly belongs in a different namespace"]}
  [coll compare-fn]
  (loops/reducei
    (fn [ret elem n]
      (if (= n 0)
          elem
          (compare-fn ret elem)))
    nil
    coll))

;___________________________________________________________________________________________________________________________________
;=================================================={         LAZY SEQS        }=====================================================
;=================================================={                          }=====================================================
#?(:clj (defalias lseq lazy-seq))

#?(:clj
  (def lseq+
    (condf*n
      (fn-or seq? nil? coll?) #(lseq %) ; not |partial|, because can't take value of a macro
      :else (fn-> list lseq first))))

(defn unchunk
  "Takes a seqable and returns a lazy sequence that
   is maximally lazy and doesn't realize elements due to either
   chunking or apply.

   Useful when you don't want chunking, for instance,
   (first awesome-website? (map slurp <a-bunch-of-urls>))
   may slurp up to 31 unneed webpages, whereas
   (first awesome-website? (map slurp (unchunk <a-bunch-of-urls>)))
   is guaranteed to stop slurping after the first awesome website.

  Taken from http://stackoverflow.com/questions/3407876/how-do-i-avoid-clojures-chunking-behavior-for-lazy-seqs-that-i-want-to-short-ci"
  {:attribution "prismatic.plumbing"}
  [s]
  (when (seq s)
    (cons (first s)
          (lazy-seq (s lrest unchunk)))))
;___________________________________________________________________________________________________________________________________
;=================================================={  POSITION IN COLLECTION  }=====================================================
;=================================================={ first, rest, nth, get ...}=====================================================
; (defn- nth-red
;   "|nth| implemented in terms of |reduce|."
;   {:deprecated  true
;    :attribution "Alex Gunnarson"
;    :performance "Twice as slow as |nth|"}
;   [coll n]
;   (let [nn (volatile! 0)]
;     (->> coll
;          (reduce
;            (fn
;              ([ret elem]
;               (if (= n @nn)
;                   (reduced elem)
;                   (do (vswap! nn inc) ret)))
;              ([ret k v]
;                (if (= n @nn)
;                    (reduced [k v])
;                    (do (vswap! nn inc) ret))))
;            []))))


(def fkey (fn-> first key))
(def fval (fn-> first val))

(defn up-val
  {:in '[{:a "ABC" :b 123} :a]
   :out '{"ABC" {:b 123}}
   :todo ["hash-map creation inefficient ATM"]}
  [m k]
  (hash-map
    (get m k)
    (-> m (dissoc k))))

(defn rename-keys [m-0 rename-m]
  (loops/reduce
    (fn [ret k-0 k-f]
      (-> ret
          (assoc  k-f (get ret k-0))
          (dissoc k-0)))
    m-0
    rename-m))

; ; for /subseq/, the coll must be a sorted collection (e.g., not a [], but rather a sorted-map or sorted-set)
; ; test(s) one of <, <=, > or >=

; ; /nthrest/
; ; (nthrest (range 10) 4) => (4 5 6 7 8 9)

; ; TODO: get-in from clojure, make it better
(defn get-in+ [coll [iden :as keys-0]] ; implement recursively
  (if (= iden identity)
      coll
      (get-in coll keys-0)))

(def single?
  "Does coll have only one element?"
  (fn-and seq (fn-not next)))
;___________________________________________________________________________________________________________________________________
;=================================================={   ADDITIVE OPERATIONS    }=====================================================
;=================================================={    conj, cons, assoc     }=====================================================

;___________________________________________________________________________________________________________________________________
;=================================================={           MERGE          }=====================================================
;=================================================={      zipmap, zipvec      }=====================================================
; A better zipvec...
;(defn zipvec+ [& colls-0] ; (map vector [] [] [] []) ; 1.487238 ms for zipvec+ vs. 1.628670 ms for doall + map-vector.
;   (let [colls (->> colls-0 (map+ fold+) fold+)]
;     (for+ [n (range 0 (count (get colls 0)))] ; should be easy, because count will be O(1) with folded colls
;       (->> colls
;            (map (f*n get+ n)))))) ; get+ doesn't take long at all; also, apparently can't use map+ within for+...
;                                   ; 234.462665 ms if you realize them
; (defn zipfor- [& colls-0] ;  [[1 2 3] [4 5 6] [7 8 9]]
;   (let [colls (->> colls-0 (map+ fold+) fold+) ; nested /for/s, no
;         rng   (range 0 (-> colls count dec))]
;     (for   [n  rng] ; [[1 2 3] [4 5 6] [7 8 9]]
;       (for [cn rng] ; ((1 4 7) (4 5 6) ...)
;         (-> colls (get cn) (get n))))))
;; (zipvec-- [[1 2 3] [4 5 6] [7 8 9]])
;(defn zipvec-- [& colls-0] ; nested /map/s, no
;  (let [colls (vec+ colls-0)]
;    (map+
;      (fn [n]
;        (map+ (getf+ n) colls))
;      (range 0 (inc 2)))))

; ; a better merge-with?

(defn merge-with+
  "Like merge-with, but the merging function takes the key being merged
   as the first argument"
   {:attribution  "prismatic.plumbing"
    :todo ["Make it not output HashMaps but preserve records"]
    :contributors ["Alex Gunnarson"]}
  [f & maps]
  (when (any? identity maps)
    (let [merge-entry
           (fn [m e]
             (let [k (key e) v (val e)]
               (if (containsk? m k)
                 (assoc m k (f k (get m k) v))
                 (assoc m k v))))
          merge2
            (fn ([] {})
                ([m1 m2]
                 (loops/reduce merge-entry (or m1 {}) (seq m2))))]
      (loops/reduce merge2 maps))))

(defn merge-vals-left
  "Merges into the left map all elements of the right map whose
   keys are found in the left map.

   Combines using @f, a |merge-with| function."
  {:todo "Make a reducer, not just implement using |reduce| function."
   :in ['{:a {:aa 1}
          :b {:aa 3}}
         {:a {:aa 5}
          :c {:bb 4}}
         (fn [k v1 v2] (+ v1 v2))]
   :out '{:a {:aa 6}
          :b {:aa 3}}}
  [left right f]
  (persistent!
    (loops/reduce
      (fn [left-f k-right v-right]
       ;(if ((fn-not contains?) left-f k-right) ; can't call |contains?| on a transient, apparently...
       ;    left-f)
       (let [v-left (core/get left k-right)]
         (if (nil? v-left)
             left-f
             (let [merged-vs
                   (merge-with+ f v-left v-right)]
               (assoc! left-f k-right merged-vs)))))
      (transient left)
      right)))
;___________________________________________________________________________________________________________________________________
;=================================================={      CONCATENATION       }=====================================================
;=================================================={ cat, fold, (map|con)cat  }=====================================================
; (defn- concat++
;   {:todo ["Needs optimization"]}
;   ([coll]
;     (try (loops/reduce catvec coll)
;       (catch Exception e (loops/reduce (zeroid into []) coll))))
;   ([coll & colls]
;     (try (apply catvec coll colls)
;       (catch Exception e (into [] coll colls)))))
;  Use original vectors until they are split. Subvec-orig below a certain range? Before the inflection point of log-n
;___________________________________________________________________________________________________________________________________
;=================================================={  FINDING IN COLLECTION   }=====================================================
;=================================================={  in?, index-of, find ... }=====================================================
(defalias in?          sel/in?         )
(defalias in-k?        sel/in-k?       )
(defalias in-v?        sel/in-v?       )
(defalias select-keys  sel/select-keys )
(defalias select-keys+ sel/select-keys+)
(defalias get-keys     sel/get-keys    )
(defalias get-key      sel/get-key     )
(defalias keys+        sel/keys+       )
(defalias vals+        sel/vals+       )
;___________________________________________________________________________________________________________________________________
;=================================================={     PARTITION, GROUP     }=====================================================
;=================================================={       incl. slice        }=====================================================
; slice-from [o start] - like slice, but until the end of o
; slice-to [o end] - like slice, but from the beginning of o
#_(defn slice ; TODO commented only for now
  "Divide coll into n approximately equal slices.
   Like partition."
  {:attribution "flatland.useful.seq"
   :todo ["Optimize" "Use transients"]}
  [n-0 coll]
  (loop [n-n n-0 slices [] items (core/vec coll)]
    (if (empty? items)
      slices
      (let [size (num/ceil (/ (count items) n-n))]
        (recur (dec n-n)
               (conj slices (subvec+ items 0 size))
               (subvec+ items size (lasti items)))))))


(defn select-as+
  {:todo ["Name this function more appropriately"]
   :attribution "Alex Gunnarson"
   :out 'Map}
  ([coll kfs]
    (->> (loops/reduce
           (fn [ret k f]
             (assoc+ ret k (f coll)))
           {}
           kfs)))
  ([coll k1 f1 & {:as kfs}]
    (select-as+ coll (assoc+ kfs k1 f1))))
;___________________________________________________________________________________________________________________________________
;=================================================={   DISTINCT, INTERLEAVE   }=====================================================
;=================================================={  interpose, frequencies  }=====================================================


; (defn plicates
;   {:attribution "Alex Gunnarson"}
;   [oper n]
;   (fn [coll]
;      (-> (fn [elem]
;            (-> (filter+ (fn-eq? elem) coll)
;                count
;                (oper n))) ; duplicates? keep them
;          (filter+ coll)
;          distinct+
;          (join [])))))

; TODO: make a reducers version of coll/elem
(defnt interpose*
  ([^string? coll elem]
    (str/join elem coll))
  ([coll elem]
    (core/interpose elem coll)))

(defn interpose
  {:todo ["|definline| this"]}
  [elem coll] (interpose* coll elem))

(defn linterleave-all
  "Analogy: partition:partition-all :: interleave:interleave-all"
  {:attribution "prismatic/plumbing"}
  [& colls]
  (lazy-seq
   ((fn helper [seqs]
      (when (seq seqs)
        (concat (map #(first %1) seqs)
                (lazy-seq (helper (keep next seqs))))))
    (keep seq colls))))

; (defn interleave+ [& args] ; 4.307220 ms vs. 1.424329 ms normal interleave :/ because of zipvec...
;   (reduce
;     (fn ([]      [])
;         ([a]     (conj [] a))
;         ([a b]   (conj    a b)) 
;         ([a b c] (conj    a b c)))
;     (apply zipvec+ args)))

; ; /partition-by/
; ; splits the coll each time f returns a new value
; ; (partition-by odd? [1 1 1 2 2 3 3])
; ; => ((1 1 1) (2 2) (3 3)) /lseq/


#?(:clj
  (defn frequencies+
    "Like clojure.core/frequencies, but faster.
     Uses Java's equal/hash, so may produce incorrect results if
     given values that are = but not .equal"
    {:attribution "prismatic.plumbing"
     :performance "4.048617 ms vs. |frequencies| 6.341091 ms"}
    [xs]
    (let [res (java.util.HashMap.)]
      (doseq [x xs]
        (->> (.get res x)
             (or 0)
             int
             unchecked-inc)
             (.put res x))
      (join {} res))))
;___________________________________________________________________________________________________________________________________
;=================================================={         GROUPING         }=====================================================
;=================================================={     group, aggregate     }=====================================================
(defn group-merge-with+
  {:attribution "Alex Gunnarson"
   :todo ["Can probably make the |merge| process parallel."]
   :in [":a"
        "(fn [k v1 v2] v1)"
        "[{:a 1 :b 2} {:a 1 :b 5} {:a 5 :b 65}]"]
   :out "[{:b 65, :a 5} {:a 1, :b 2}]"
   :out-type 'Reducer}
  [group-by-f merge-with-f coll]
  (let [merge-like-elems 
         (fn [grouped-elems]
           (if (single? grouped-elems)
               grouped-elems
               (loops/reduce
                 (fn [ret elem]
                   (merge-with+ merge-with-f ret elem))
                 (first grouped-elems)
                 (rest  grouped-elems))))]
    (->> coll
         (group-by+ group-by-f)
         (map+ val) ; [[{}] [{}{}{}]]
         (map+ merge-like-elems)
         flatten+)))

(defn merge-left 
  ([alert-level] ; Keyword
    (fn [k v1 v2]
      (when (not= v1 v2)
        (log/pr alert-level
          "Values do not match for merge key"
          (str (str/squote k) ":")
          (str/squote v1) "|" (str/squote v2)))
      v1))
  ([k v1 v2] v1))

(defn merge-right
  ([alert-level] ; Keyword
    (fn [k v1 v2]
      (when (not= v1 v2)
        (log/pr alert-level
          "Values do not match for merge key"
          (str (str/squote k) ":")
          (str/squote v1) "|" (str/squote v2)))
      v1))
  ([k v1 v2] v2))

(defn first-uniques-by+ [k coll]
  (->> coll
       (group-by+ k)
       (map+ (update-val+ #(first %1)))))



; ===== SORTING ===== ;

(defn- sort-parts
  "Lazy, tail-recursive, incremental quicksort. Works against
   and creates partitions based on the pivot, defined as 'work'."
  {:attribution "The Joy of Clojure, 2nd ed."}
  [work]
  (lazy-seq
    (loop [[part & parts] work]
      (if-let [[pivot & xs] (seq part)]
        (let [smaller? #(< % pivot)]
          (recur (list*
                  (filter smaller? xs)
                  pivot
                  (remove smaller? xs)
                  parts)))
        (when-let [[x & parts] parts]
          (cons x (sort-parts parts)))))))

(defn lsort
  "Lazy 'quick'-sorting"
  {:attribution "The Joy of Clojure, 2nd ed."}
  [elems]
  (sort-parts (list elems))) 
;___________________________________________________________________________________________________________________________________
;=================================================={   COLLECTIONS CREATION   }=====================================================
;=================================================={                          }=====================================================

; TODO fix
(def map->record hash-map)

(defn reverse-kvs [m]
  (zipmap (vals m) (keys m)))

(defn- update-nth-list*
  [x n f]
  (if (= n 0)
      (conjl (rest x) (f (first x)))
      (concat (ltake n x) (list (f (get x n))) (nthnext x (inc n)))))

(defnt update-nth
  {:todo ["Fix class overlap"]}
          ([^vector? x n f] (update x n f))
          #_([^cdlist? x n f] (if (= n (lasti x)) ; TODO ENABLE THIS
                                (conj (.pop x) (f (last x)))
                                (update-nth-list* x n f)))
  #?(:clj ([^clojure.lang.Seqable #_listy?  x n f] (update-nth-list* x n f))))

(defn update-first [x f] (update-nth x 0         f))
(defn update-last  [x f] (update-nth x (lasti x) f))



(defn index-with-ids
  "Adds unique ids to each entry."
  [vec-0]
  (let [ids (->> vec-0
                 (map+ :id)
                 (remove+ nil?)
                 (join (sorted-set-by (fn [a b] (> a b))))
                 atom)]
    (reducei
      (fn [vec-n entry n]
        (if (or (contains? entry :id)
                (empty?    entry))
            vec-n
            (let [id (-> ids deref first (ifn nil? (constantly 1) inc))]
              (conj! ids id)
              (assoc vec-n n (assoc entry :id id)))))
      vec-0
      vec-0)))



; REQUIRES hara.string.PATH/JOIN
#_(defn flatten-keys
  "takes map `m` and flattens the first nested layer onto the root layer.
  (flatten-keys {:a {:b 2 :c 3} :e 4})
  => {:a/b 2 :a/c 3 :e 4}
  (flatten-keys {:a {:b {:c 3 :d 4}
                     :e {:f 5 :g 6}}
                 :h {:i 7}
                 :j 8})
  => {:a/b {:c 3 :d 4} :a/e {:f 5 :g 6} :h/i 7 :j 8}"
  {:source "zcaudate/hara.data.path"}
  ([m]
   (reduce-kv (fn [m k v]
                (if (map/hash-map? v)
                    (reduce-kv (fn [m sk sv]
                                 (assoc m (path/join [k sk]) sv))
                               m
                               v)
                    (assoc m k v)))
              {}
              m)))

(defn- pathify-keys-nested
  {:source "zcaudate/hara.data.path"}
  ([m] (pathify-keys-nested m -1 false []))
  ([m max] (pathify-keys-nested m max false []))
  ([m max keep-empty] (pathify-keys-nested m max keep-empty []))
  ([m max keep-empty arr]
   (reduce-kv (fn [m k v]
                (if (or (and (not (> 0 max))
                             (<= max 1))
                        (not (#?(:clj  map/hash-map?
                                 :cljs map?) v))
                        (and keep-empty
                             (empty? v)))
                  (assoc m (conj arr k) v)
                  (merge m (pathify-keys-nested v (dec max) keep-empty (conj arr k)))))
              {}
              m)))

; REQUIRES hara.string.PATH/JOIN
#_(defn flatten-keys-nested
  "Returns a single associative map with all of the nested
   keys of `m` flattened. If `keep` is added, it preserves all the
   empty sets
  (flatten-keys-nested {\"a\" {\"b\" {\"c\" 3 \"d\" 4}
                               \"e\" {\"f\" 5 \"g\" 6}}
                          \"h\" {\"i\" {}}})
  => {\"a/b/c\" 3 \"a/b/d\" 4 \"a/e/f\" 5 \"a/e/g\" 6}
  (flatten-keys-nested {\"a\" {\"b\" {\"c\" 3 \"d\" 4}
                               \"e\" {\"f\" 5 \"g\" 6}}
                          \"h\" {\"i\" {}}}
                       -1 true)
  => {\"a/b/c\" 3 \"a/b/d\" 4 \"a/e/f\" 5 \"a/e/g\" 6 \"h/i\" {}}"
  {:source "zcaudate/hara.data.path"}
  ([m] (flatten-keys-nested m -1 false))
  ([m max keep-empty]
   (-> (pathify-keys-nested m max keep-empty)
       (nested/update-keys-in [] path/join))))

; REQUIRES hara.string.PATH/SPLIT
#_(defn treeify-keys
  "Returns a nested map, expanding out the first
   level of keys into additional hash-maps.
  (treeify-keys {:a/b 2 :a/c 3})
  => {:a {:b 2 :c 3}}
  (treeify-keys {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e/f 1}
          :c {:g/h 1}}}"
  {:source "zcaudate/hara.data.path"}
  [m]
  (reduce-kv (fn [m k v]
               (assoc-in m (path/split k) v))
             {}
             m))

; REQUIRES hara.string.PATH/SPLIT
#_(defn treeify-keys-nested
  "Returns a nested map, expanding out all
 levels of keys into additional hash-maps.
  (treeify-keys-nested {:a/b 2 :a/c 3})
  => {:a {:b 2 :c 3}}
  (treeify-keys-nested {:a/b {:e/f 1} :a/c {:g/h 1}})
  => {:a {:b {:e {:f 1}}
          :c {:g {:h 1}}}}"
  {:source "zcaudate/hara.data.path"}
  [m]
  (reduce-kv (fn [m k v]
               (if (and (map/hash-map? v) (not (empty? v)))
                 (update-in m (path/split k) nested/merge-nested (treeify-keys-nested v))
                 (assoc-in m (path/split k) v)))
             {}
             m))

(defn remove-repeats
  "Returns a vector of the items in `coll` for which `(f item)` is unique
   for sequential `item`'s in `coll`.
    (remove-repeats [1 1 2 2 3 3 4 5 6])
    ;=> [1 2 3 4 5 6]
    (remove-repeats even? [2 4 6 1 3 5])
    ;=> [2 1]

    h/remove-repeats [1 1 2 2 3 3 4 5 6])
    => [1 2 3 4 5 6]
    (h/remove-repeats :n [{:n 1} {:n 1} {:n 1} {:n 2} {:n 2}])
    => [{:n 1} {:n 2}]
    (h/remove-repeats even? [2 4 6 1 3 5])
    => [2 1])"
  {:source "zcaudate/hara"
   :todo "merge with something else"}
  ([coll] (remove-repeats identity coll))
  ([f coll] (remove-repeats f coll [] nil))
  ([f coll output last]
     (if-let [v (first coll)]
       (cond (and last (= (f last) (f v)))
             (recur f (next coll) output last)
             :else (recur f (next coll) (conj output v) v))
       output)))


(defn transient-copy
  {:attribution ["Alex Gunnarson"]}
  [t]
  (let [copy (transient [])]
    (dotimes [n (count t)]
      (conj! copy (get t n)))
    (persistent! copy)))

(defnt ensurec*
  ([#{vector? set? map?} ensurer ensured]
    (cond ((type/->pred ensurer) ensured)
          ensured
          (nil? ensured)
          ensurer
          :else (conj (type/->base ensurer) ensured))))

(defn ensurec
  "ensure-collection.
   Ensures that @ensured is the same class as @ensurer.
   This might be used in cases where one would like to ensure that
   |conj|ing onto a value in a map is valid."
  {:author "Alex Gunnarson"
   :tests '{(ensurec nil [:a])
              [:a]
            (ensurec :a  [:b])
              [:a]
            (ensurec []  [:b])
              []
            (update {:a 1} :a (fn-> (ensurec []) (conj 3)))
              {:a [1 3]}}}
  [ensured ensurer]
  (ensurec* ensurer ensured))

(defn index-by-vals
  {:tests '{(index-by-vals
              {:a #{1 2 3}
               :b #{2 4 5}
               :c #{1 3 4}})
            {1 #{:a :c}
             2 #{:a :b}
             3 #{:a :c}
             4 #{:b :c}
             5 #{:b}}}}
  [coll & [{:keys [into-coll get-key get-val]
            :or {into-coll #{}
                 get-key fn/seconda
                 get-val fn/firsta}
            :as opts}]]
  (let [update-f (fn [index-f k v]
                   (update! index-f (get-key k v)
                     (fn-> (ensurec into-coll) (conj (get-val k v)))))]
    (persistent!
      (reduce
        (fn [index k vs]
          (reduce (fn ([index-f v      ] (update-f index-f k v                  ))
                      ([index-f v-k v-v] (update-f index-f k (map-entry v-k v-v))))
            index
            vs))
        (transient {})
        coll))))

(defn merge-deep-with
  "Like `merge-with` but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.

  (merge-deep-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                    {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  => {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  {:attribution "clojure.contrib.map-utils via taoensso.encore"
   :todo ["Replace |merge-with| with a more performant version which uses |map/merge|."]}
  [f & maps]
  (apply
    (fn m [& maps]
      (if (every? map? maps)
          (apply merge-with m maps)
          (apply f maps)))
    maps))

(def merge-deep
  (partial merge-deep-with
    (fn ([x]   (second x))
        ([x y] y))))
; TODO: incorporate |split-at| into the quantum.core.collections/split-at protocol


(defn seq-ldifference
  "Like |set/difference| but for seqs.
   Returns what is in @l but not in @r
   based on the results the application of @selectors returns."
  {:in [[{:n 1 :a 3}
         {:n 2 :a 3}]
        [{:n 4 :b 5}
         {:n 2 :a 3 :b 10}]
        #{:n :a}]
   :out [[{:n 2 :a 3}]]}
  ([l r]
    (set/difference
      (->> l (join #{}))
      (->> r (join #{}))))
  ([l r selectors]
    (let [l-grouped (->> l (group-by (apply juxt selectors)))
          r-grouped (->> r (group-by (apply juxt selectors)))]
      (->> (set/difference
             (->> l-grouped keys (join #{}))
             (->> r-grouped keys (join #{})))
           (map+ (fn->> (get l-grouped)))
           (reduce #(join %1 %2) #{})))))



;; find rank of element as primitive long, -1 if not found
; (doc avl/rank-of)
; ;; find element closest to the given key and </<=/>=/> according
; ;; to coll's comparator
; (doc avl/nearest)
; ;; split the given collection at the given key returning
; ;; [left entry? right]
; (doc avl/split-key)
;; return subset/submap of the given collection; accepts arguments
;; reminiscent of clojure.core/{subseq,rsubseq}
; (doc avl/subrange)

#?(:clj
(defn get-map-constructor
  "Gets a record's map-constructor function via its class name."
  [rec]
  (let [^String class-name-0
          (if (class? rec)
              (-> rec str)
              (-> rec class str))
        ^String class-name
          (subseq class-name-0
            (-> class-name-0 (last-index-of ".") inc)
            (-> class-name-0 count))
        map-constructor-fn
          (->> class-name (str "map->") symbol eval)]
    map-constructor-fn)))


#?(:clj
(defmacro deficlass
  "Define an immutable class.
   Based on the limitations of |defrecord|, multi-arity
   functions are declared separately."
  [name- fields constructor & fns]
  (let [constructor-sym (->> name- name sform/un-camelcase (str "->") symbol)
        protocol-sym    (-> name- name (str "Functions") symbol)
        fns-signatures  (->> fns
                             (map (compr (juxt #(first %1)
                                               #(second %1))))
                             (group-by #(first %1))
                             (map-vals+ (fn->> (map (fn-> rest)) flatten-1))
                             (map+ (fn [x] (cons (first x) (second x))))
                             (join [])) ]
    (log/pr :macro-expand "FNS-SIGNATURES" fns-signatures)
   `(do (defprotocol ~protocol-sym ~@fns-signatures)
        (declare ~constructor-sym)
        (defrecord ~name- ~fields ~protocol-sym
          ~@fns)
        ~(concat (list 'defn constructor-sym)
                 constructor)))))

(defn into-map-by [m k ms]
  (reduce (fn [ret elem] (assoc ret (k elem) elem)) m ms))

(defn pivot
  "Pivot a table à la Excel.
   Defaults to right pivot."
  {:in '[[1 4 7 a]
         [2 5 8 b]
         [3 6 9 c]]
   :out '[[1 2 3]
          [4 5 6]
          [7 8 9]
          [a b c]]
   :todo ["Make cleaner/ more parallelizable."]}
  [table-0]
  (let [height-f (count (first table-0))
        width-f  (count table-0)
        table-f  (seq-loop [row-i   (range height-f)
                            table-n (transient [])]
                   (let [row-f (seq-loop [col-i (range width-f)
                                          row   (transient [])]
                                 (conj! row (-> table-0 (get col-i) (get row-i))))]
                     (conj! table-n (persistent! row-f))))]
    (persistent! table-f)))

(defn merge-keys-with 
  {:tests '{(merge-keys-with {:a {:b 1 :c 2}
                              :b {:c 3 :a 4}}
              [:a :b]
              (fn [a b] a))
            {:a {:b 1 :c 2 :a 4}}}}
  [m [k-0 & ks] f]
  (reduce
    (fn [ret k]
      (-> m
          (update k-0 #(merge-with f % (get m k)))
          (dissoc k)))
    m
    ks))

; =========== NECESSITIES FOR DATASCRIPT AND POSH ============== ;

(defn trim-head
  {:from "tonsky/datascript-todo.util"}
  [xs n]
  (->> xs
       (ldropl (- (count xs) n))
       core/vec))

(defn take-until
  {:from "mpdairy/posh.q-pattern-gen"}
  [stop-at? ls]
  (if (or
       (empty? ls)
       (stop-at? (first ls)))
    []
    (cons (first ls) (take-until stop-at? (rest ls)))))

(defn rest-at
  {:from "mpdairy/posh.q-pattern-gen"}
  [rest-at? ls]
  (if (or (empty? ls) (rest-at? (first ls)))
    ls
    (recur rest-at? (rest ls))))

(defn split-list-at
  {:from "mpdairy/posh.q-pattern-gen"}
  [split-at? ls]
  (if (empty? ls)
    {}
    (merge {(first ls) (take-until split-at? (take-until split-at? (rest ls)))}
           (split-list-at split-at? (rest-at split-at? (rest ls))))))

(defn deep-list?
  {:from "mpdairy/posh.core"}
  [x]
  (cond (list? x) true
        (coll? x) (if (empty? x) false
                      (or (deep-list? (first x))
                          (deep-list? (core/vec (rest x)))))))

(defn deep-find
  {:from "mpdairy/posh.core"}
  [f x]
  (if (coll? x)
      (if (empty? x)
        false
        (or (deep-find f (first x))
             (deep-find f (rest x))))
      (f x)))

(defn deep-map [f x]
  {:from "mpdairy/posh.core"}
  (cond
   (map? x) (let [r (map (partial deep-map f) x)]
              (zipmap (map #(first %1) r) (map #(second %1) r)))
   (coll? x) (core/vec (map (partial deep-map f) x))
   :else (f x)))

(defn drop-tail
  {:from "tonsky/datascript-todo.util"
   :todo ["It doesn't seem like this actually does anything"]}
  [xs pred]
  (loop [acc []
         xs  xs]
    (let [x (first xs)]
      (cond
        (nil? x) acc
        (pred x) (conj acc x)
        :else  (recur (conj acc x) (next xs))))))
