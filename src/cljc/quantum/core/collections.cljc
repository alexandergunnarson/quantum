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
    :attribution "Alex Gunnarson"}
  quantum.core.collections
  (:refer-clojure :exclude
    [for doseq reduce
     contains?
     repeat repeatedly
     range
     take take-while
     drop  drop-while
     subseq
     key val
     merge sorted-map sorted-map-by
     into
     count
     vec empty
     split-at
     first second rest last butlast get pop peek
     zipmap
     conj! assoc! dissoc! disj!
     partition-all])
  (:require-quantum [:core logic #_type macros #_num vec set
                     log err fn str])
  (:require
            [quantum.core.data.map         :as map  ]
            [quantum.core.collections.core :as coll ]
            [quantum.core.collections.base :as base ]
            [quantum.core.reducers         :as red  ]
            [quantum.core.string.format    :as sform]
            [quantum.core.analyze.clojure.predicates :as anap]
            [quantum.core.type.predicates  :as tpred]
            [clojure.walk                  :as walk ]
    #?(:clj [quantum.core.loops            :as loops])
    #?(:clj [clojure.pprint :refer [pprint]]))
  #?(:cljs
    (:require-macros
      [quantum.core.reducers   :as red  ]
      [quantum.core.loops      :as loops])))

(defn key
  ([kv] (if (nil? kv) nil (core/key kv)))
  ([k v] k))

(defn val
  ([kv] (if (nil? kv) nil (core/val kv)))
  ([k v] v))

#?(:clj (defmacro map-entry [a b] `[~a ~b]))

(defn genkeyword
  ([]    (keyword (gensym)))
  ([arg] (keyword (gensym arg))))

(defnt empty
  ([^string? obj] "")
  ([         obj] (core/empty obj)))

(defn wrap-delay [f]
  (if (delay? f) f (delay ((or f fn-nil)))))

#?(:clj (defalias reduce   loops/reduce  ))
#?(:clj (defalias reduce-  loops/reduce- ))
#?(:clj (defalias reducei  loops/reducei ))
#?(:clj (defalias reducei- loops/reducei-))
#?(:clj (defalias seq-loop loops/seq-loop))
; Loop via |reduce|  
#?(:clj (defalias loopr    loops/seq-loop))

(defalias break reduced)

(defalias vec         red/vec+       )    
#_(defalias array       coll/array     ) ; TODO for now
(defalias into        red/into+      ) 

(defalias redv        red/fold+      )
(defalias redm        red/reducem+   )
(defalias fold        red/fold+      ) ; only certain arities
(defalias foldv       red/foldp+     ) ; only certain arities
(defalias foldm       red/foldm+     ) 
(defalias map+        red/map+       )
(defalias filter+     red/filter+    )
(defalias lfilter     filter         )
(defalias remove+     red/remove+    )
(defalias lremove     remove         )
(defalias take+       red/take+      )
(defalias take-while+ red/take-while+)
(defalias drop+       red/drop+      )
(defalias group-by+   red/group-by+  )
(defalias flatten+    red/flatten+   )
(def flatten-1 (partial apply concat)) ; TODO more efficient

; ; ====== LOOPS ======
;(def cljs-for+ (var red/for+))
; (defalias for+   #?(:clj red/for+         :cljs red/for+))
; (alter-meta! (var for+) assoc :macro true)

; (def cljs-for (var loops/for)) ; doesn't work because not a var
; (def cljs-for (mfn loops/for)) ; doesn't work because no |eval|
#?(:clj (defalias for   loops/for  ))
#?(:clj (defalias fori  loops/fori ))
#?(:clj (defalias for-m loops/for-m))
#?(:clj (defalias until loops/until))
#?(:clj (alter-meta! (var for) assoc :macro true))

;(def cljs-lfor (var clojure.core/for))
;(defalias lfor   #?(:clj clojure.core/for :cljs cljs-lfor))
;#?(:clj (defalias lfor clojure.core/for))
#?(:clj (defmacro lfor   [& args] `(clojure.core/for ~@args)))

;(def cljs-doseq (var loops/doseq))
;(defalias doseq  #?(:clj loops/doseq      :cljs cljs-doseq))
#?(:clj (defmacro doseq  [& args] `(loops/doseq      ~@args)))

;(def cljs-doseqi (var loops/doseqi))
;(defalias doseqi #?(:clj loops/doseqi     :cljs cljs-doseqi))
#?(:clj (defmacro doseqi [& args] `(loops/doseqi     ~@args)))

#?(:cljs
  (defn kv+
    "For some reason ClojureScript reducers have an issue and it's terrible... so use it like so:
     (map+ (compr kv+ <myfunc>) _)
     |reduce| doesn't have this problem."
    {:todo ["Eliminate the need for this."]}
    ([obj] obj)
    ([k v] k)))

; ; ====== COLLECTIONS ======

; ; TODO Don't redefine these vars
; #?(:cljs (defn map+    [f coll] (red/map+    (compr kv+ f) coll)))
; #?(:cljs (defn filter+ [f coll] (red/filter+ (compr kv+ f) coll)))
; #?(:cljs (defn remove+ [f coll] (red/remove+ (compr kv+ f) coll)))

; TODO change these back
; (defalias lasti         coll/lasti        )
; (defalias index-of      coll/index-of     )
; (defalias last-index-of coll/last-index-of)
(defalias count         core/count        )
; (defalias getr          coll/getr         )
; (defalias subseq        getr              )
; (defalias lsubseq       core/subseq       )
(defalias get           core/get          )
; (defalias gets          coll/gets         )
; (defalias getf          coll/getf         )

; ; If not |defalias|ed, "ArityException Wrong number of args (2) passed to: core/eval36441/fn--36457/G--36432--36466"
; (defalias conjl         coll/conjl        )
; (defalias conjr         coll/conjr        )
(defalias pop           core/pop          )
; (defalias popr          coll/popr         )
; (defalias popl          coll/popl         )
(defalias peek          core/peek         )
(defalias first         core/first        )
(defalias second        core/second       )
; (defalias third         coll/third        )
(defalias rest          core/rest         )
; (defalias lrest         core/rest         )
(defalias butlast       core/butlast      )
(defalias last          core/last         )
(defalias assoc!        core/assoc!       )
; (defalias dissoc!       coll/dissoc!      )
(defalias conj!         core/conj!        )
; (defalias disj!         coll/disj!        )
; (defalias update!       coll/update!      )
(defalias contains?     core/contains?    )
; (defalias containsk?    coll/containsk?   )
; (defalias containsv?    coll/containsv?   )


; TODO this doesn't belong here
(defalias drop core/drop)
(defalias range core/range)
(defalias zipmap core/zipmap)
(defalias postwalk clojure.walk/postwalk)




(defn reduce-map
  {:from "r0man/noencore"}
  [f coll]
  (if (tpred/editable? coll)
      (persistent! (reduce-kv (f assoc!) (transient (empty coll)) coll))
      (reduce-kv (f assoc) (empty coll) coll)))

(defn map-keys
  "Maps a function over the keys of an associative collection."
  {:from "r0man/noencore"}
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m (f k) v))) coll))

(defn map-vals
  "Maps a function over the values of an associative collection."
  {:from "r0man/noencore"}
  [f coll]
  (reduce-map (fn [xf] (fn [m k v] (xf m k (f v)))) coll))

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

(defn trim-head
  {:from "tonsky/datascript-todo.util"}
  [xs n]
  (->> xs
       (drop (- (count xs) n))
       vec))

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
    (map/merge {(first ls) (take-until split-at? (take-until split-at? (rest ls)))}
                (split-list-at split-at? (rest-at split-at? (rest ls))))))

(defn deep-list?
  {:from "mpdairy/posh.core"}
  [x]
  (cond (list? x) true
        (coll? x) (if (empty? x) false
                      (or (deep-list? (first x))
                          (deep-list? (vec (rest x)))))))

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
              (zipmap (map first r) (map second r)))
   (coll? x) (vec (map (partial deep-map f) x))
   :else (f x)))

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

(defn wrap-delay [f]
  (if (delay? f) f (delay ((or f fn-nil)))))

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

(defn keywordize
  "Transforms string @x to a dash-case keyword."
  [x]
  (if (string? x)
      (keyword (#?(:clj  .replaceAll
                   :cljs .replace) x "_" "-"))
      x))

(defn apply-to-keys
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"
   :contributors #{"Alex Gunnarson"}}
  ([m] (apply-to-keys m identity))
  ([m f]
    (let [apply-fn
           (fn [[k v]] (map-entry (f k) v))]
      ; only apply to maps
      (postwalk
        (whenf*n map? (fn->> (map apply-fn) (into {})))
        m))))

(defn keywordize-keys
  "Recursively transforms all map keys from keywords to strings."
  [x]
  (apply-to-keys x keywordize))

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

(defalias safe-mapcat anap/safe-mapcat)

(defn dezip
  "The inverse of zip. – Unravels a seq of m n-tuples into a
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

(defn assoc-if
  "Works like assoc, but only associates if condition is true."
  {:from "macourtney/clojure-tools"}
  ([condition map key val] 
    (if condition
      (assoc map key val)
      map))
  ([condition map key val & kvs]
    (reduce 
      (fn [output key-pair] 
        (assoc-if condition output (first key-pair) (second key-pair)))
      (assoc-if condition map key val)
      (partition 2 kvs))))

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

(defn merge-meta
  "Returns an object of the same type and value as `obj`, with its
   metadata merged over `m`."
  {:from "cljs.tools/reader"}
  [obj m]
  (let [orig-meta (meta obj)]
    (with-meta obj (map/merge m (dissoc orig-meta :source)))))

(defn into!
  "Like into, but for transients"
  [to from]
  (reduce conj! to from))

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

(defn select-keys'
  "Like clojure.core/select-keys, but uses transients and doesn't preserve meta"
  {:from "clojure.tools.analyzer.utils"}
  [map keyseq]
  (loop [ret (transient {}) keys (seq keyseq)]
    (if keys
      (let [entry (find map (first keys))]
        (recur (if entry
                 (conj! ret entry)
                 ret)
               (next keys)))
      (persistent! ret))))

(def mmerge
  "Same as (fn [m1 m2] (merge-with merge m2 m1))"
  #(merge-with map/merge %2 %1))

(defn mapv'
  "Like mapv, but short-circuits on reduced"
  {:from "clojure.tools.analyzer.utils"}
  [f v]
  (let [c (count v)]
    (loop [ret (transient []) i 0]
      (if (> c i)
        (let [val (f (nth v i))]
          (if (reduced? val)
            (reduced (persistent! (reduce conj! (conj! ret @val) (subvec v (inc i)))))
            (recur (conj! ret val) (inc i))))
        (persistent! ret)))))