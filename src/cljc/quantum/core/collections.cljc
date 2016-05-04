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
  (:require-quantum [:core logic type macros #_num vec set
                     log err fn str list])
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
      [quantum.core.reducers         :as red  ]
      [quantum.core.loops            :as loops]
      [quantum.core.collections.core :as coll ]
      [quantum.core.collections     
        :refer [for lfor doseq doseqi reduce reducei
                seq-loop
                count lasti
                subseq
                contains? containsk? containsv?
                index-of last-index-of
                first second rest last butlast get pop peek
                conjl
                conj! assoc! dissoc! disj!
                map-entry]])))

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

; TODO put in collections.core
(defnt empty
  ([^string? obj] "")
  ([         obj] (core/empty obj)))

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
#?(:clj (defalias conj!         coll/conj!        ))
#?(:clj (defalias disj!         coll/disj!        ))
#?(:clj (defalias update!       coll/update!      ))
#?(:clj (defalias contains?     coll/contains?    ))
#?(:clj (defalias containsk?    coll/containsk?   ))
#?(:clj (defalias containsv?    coll/containsv?   ))



(defalias vec         red/vec+       )    
#?(:clj (defalias array       coll/array     ))
(defalias into        red/into+      ) 

(defalias redv        red/fold+      )
(defalias redm        red/reducem+   )
(defalias fold        red/fold+      ) ; only certain arities
(defalias foldv       red/foldp+     ) ; only certain arities
(defalias foldm       red/foldm+     ) 
(defalias map+        red/map+       )
(defalias remove+     red/remove+    )
(defalias lremove     remove         )
(defalias take+       red/take+      )
(defalias take-while+ red/take-while+)
(defalias drop+       red/drop+      )
(defalias group-by+   red/group-by+  )
(defalias flatten+    red/flatten+   )
(def flatten-1 (partial apply concat)) ; TODO more efficient


; _______________________________________________________________
; ============================ LOOPS ============================
; •••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••••

; ============================ REDUCE ============================

#?(:clj (defalias reduce   loops/reduce  ))
#?(:clj (defalias reduce-  loops/reduce- ))
#?(:clj (defalias reducei  loops/reducei ))
#?(:clj (defalias reducei- loops/reducei-))

(defn reduce-2
  "Like |reduce|, but reduces over two items in a collection at a time.

   Its function @func must take three arguments:
   1) The accumulated return value of the reduction function
   2) The                next item in the collection being reduced over
   3) The item after the next item in the collection being reduced over"
  {:attribution "Alex Gunnarson"}
  [func init coll] ; not actually implementing of CollReduce... so not as fast...
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))

(defn reduce-map
  {:from "r0man/noencore"}
  [f coll]
  (if (tpred/editable? coll)
      (persistent! (reduce-kv (f #(assoc! %1 %2 %3)) (transient (empty coll)) coll))
      (reduce-kv (f assoc) (empty coll) coll)))






#?(:clj (defalias seq-loop loops/seq-loop))
; Loop via |reduce|  
#?(:clj (defalias loopr    loops/seq-loop))

(defalias break reduced)

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


(defalias zipmap core/zipmap)
(defalias merge         map/merge        )
(defalias sorted-map    map/sorted-map   )
(defalias sorted-map-by map/sorted-map-by)

; ===== REPEAT =====

(declare range)

(defn repeat
  ([obj]   (core/repeat obj)) ; can't do eager infinite repeat
  ([n obj] (for [i (range n)] obj)))

(defalias lrepeat core/repeat)

; ===== REPEATEDLY ===== ;

#?(:clj
(defmacro repeatedly-into
  {:todo ["Best to just inline this. Makes no sense to have a macro."]}
  [coll n & body]
  `(let [coll# ~coll
         n#    ~n]
     (if (should-transientize? coll#)
         (loop [v# (transient coll#) idx# 0]
           (if (>= idx# n#)
               (persistent! v#)
               (recur (conj! v# ~@body)
                      (inc idx#))))
         (loop [v#   coll# idx# 0]
           (if (>= idx# n#)
               v#
               (recur (conj v# ~@body)
                      (inc idx#))))))))

(def lrepeatedly clojure.core/repeatedly)

#?(:clj
(defmacro repeatedly
  "Like |clojure.core/.repeatedly| but (significantly) faster and returns a vector."
  ; ([n f]
  ;   `(repeatedly-into* [] ~n ~arg1 ~@body))
  {:todo ["Makes no sense to have a macro. Just inline"]}
  ([n arg1 & body]
    `(repeatedly-into* [] ~n ~arg1 ~@body))))

; ===== RANGE ===== ;

(#?(:clj defalias :cljs def) range+ red/range+)

(defn lrrange
  "Lazy reverse range."
  {:usage '(lrrange 0 5)
   :out   '(5 4 3 2 1)}
  ([]    (iterate core/dec 0))
  ([a]   (iterate core/dec a))
  ([a b]
    (->> (iterate core/dec b) (core/take (- b (core/dec a))))))

(defn lrange
  ([]  (core/range))
  ([a] (core/range a))
  ([a b]
    (if (neg? (- a b))
        (lrrange a b)
        (core/range a b))))

(defn ^Vec rrange
  "Reverse range"
  {:todo ["Performance with |range+| on [a b] arity, and rseq"]}
  ([]    (lrrange))
  ([a]   (lrrange a))
  ([a b] (-> (range+ a b) redv reverse)))

(defn ^Vec range
  {:todo ["Performance with |range+| on [a b] arity"]}
  ([]    (lrange))
  ([a]   (lrange a))
  ([a b]
    (if (neg? (- a b))
        (rrange a b))
        (-> (range+ a b) redv)))

; ============================ FILTER ============================

(defalias filter+     red/filter+    )
(defalias lfilter     filter         )

(defn ffilter
  "Returns the first result of a |filter| operation.
   Uses lazy |filter| so as to do it in the fastest possible way."
   [filter-fn coll]
   (->> coll (filter filter-fn) first))

(defn ffilter+
  {:todo ["Use a delayed reduction as the base!"]}
  [pred coll]
  (reduce
    (fn [ret elem-n]
      (when (pred elem-n)
        (reduced elem-n)))
    nil
    coll))

(defn ffilteri
  {:todo ["Use a delayed reduction as the base!" "Allow parallelization"]
   :in   '[(fn-eq? "4") ["a" "d" "t" "4" "10"]]
   :out  [3 "4"]
   :out-type 'MapEntry}
  [pred coll]
  (reducei
    (fn [ret elem-n index-n]
      (if (pred elem-n)
          (reduced (map-entry index-n elem-n))
          ret))
    (map-entry nil nil)
    coll))

(defn filteri
  {:todo ["Use reducers"]}
  [pred coll]
  (if (type/should-transientize? coll)
      (persistent!
        (loops/reducei
          (fn [ret elem-n n]
            (if (pred elem-n)
                (conj! ret (map-entry n elem-n))
                ret))
          (transient [])
          coll))
      (loops/reducei
        (fn [ret elem-n n]
          (if (pred elem-n)
              (conj ret (map-entry n elem-n))
              ret))
        []
        coll)))

(defnt ^clojure.lang.MapEntry last-filteri*
  {:todo ["Use a delayed reduction as the base!" "Allow parallelization"]
   :in   '[["a" "d" "t" "4" "4" "10"] (fn-eq? "4")]
   :out  [4 "4"]}
  ([^indexed? coll pred]
    (->> coll rseq (ffilteri pred)
         (<- update 0 (partial - (lasti coll)))))
  ([coll pred]
    (loops/reducei
      (fn [ret elem-n index-n]
        (if (pred elem-n)
            (map-entry index-n elem-n)
            ret))
      (map-entry nil nil)
      coll)))

#?(:clj  (definline last-filteri [pred coll] `(last-filteri* ~coll ~pred))
   :cljs (defn      last-filteri [pred coll]  (last-filteri*  coll  pred)))







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

(declare drop ldropl)

(defn trim-head
  {:from "tonsky/datascript-todo.util"}
  [xs n]
  (->> xs
       (ldropl (- (count xs) n))
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
              (zipmap (map (MWA first) r) (map (MWA second) r)))
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

(declare postwalk)

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

; ----- META ----- ;

(defn merge-meta
  "Returns an object of the same type and value as `obj`, with its
   metadata merged over `m`."
  {:from "cljs.tools/reader"}
  [obj m]
  (let [orig-meta (meta obj)]
    (with-meta obj (map/merge m (dissoc orig-meta :source)))))

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

; ; a better merge?
; ; a better merge-with?
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







; TODO UNCOMMENT ALL THIS BECAUSE THIS IS GOOD STUFF
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
  {:todo ["Make parallizeable"]}
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
  "Lazy |indices-of|.

   Originally |positions|."
  {:source "zcaudate/hara.data.seq"}
  [pred coll]
  (keep-indexed
    (fn [idx x]
      (when (pred x)
        idx))
    coll))

(defn index-of-pred      [coll pred]
  (->> coll (ffilteri pred) key))

(defn last-index-of-pred [coll pred]
  (->> coll (last-filteri pred) key))
; ================================================ TAKE ================================================
; ============ TAKE-LEFT ============
(defn takel
  {:in  [2 "abcdefg"]
   :out "ab"}
  [i super]
  (subseq super 0 (-> i long)))

(def take takel)
(def ltake core/take)

(defn takel-fromi
  "Take starting at and including index n."
  {:in  [2 "abcdefg"]
   :out "cdefg"}
  [i super]
  (subseq super i (lasti super)))

(def take-fromi takel-fromi)

(defn takel-from
  {:in  ["cd" "abcdefg"]
   :out "cdefg"}
  [sub super]
  (let [i (or (index-of super sub)
              (throw+ (Err. :out-of-bounds nil [super sub])))]
    (takel-fromi i super)))

(def take-from takel-from)

(defn takel-afteri
  {:in  [2 "abcdefg"]
   :out "defg"}
  [i super]
  (subseq super (-> i long inc) (lasti super)))

(def take-afteri takel-afteri)

(defn takel-after
  {:in  ["cd" "abcdefg"]
   :out "efg"}
  [sub super]
  (let [i (or (index-of super sub)
              (throw+ (Err. :out-of-bounds nil [super sub])))
        i-f (+ i (lasti sub))]
    (takel-afteri i-f super)))

(def take-after takel-after)

(defn takel-while
  {:in '[(f*n in-v? str/alphanum-chars) "abcdef123&^$sd"]
   :out "abcdef123"}
  [pred super]
  (subseq super 0 (index-of-pred super pred)))

(def take-while   takel-while    )
(def ltakel-while core/take-while)
(def ltake-while  ltakel-while   )

(def takel-untili takel)
(def take-untili  take-untili)

(defnt takel-until
  ([^fn? pred super] (takel-while super (fn-not pred)))
  ([     sub  super]
    (if-let [i (index-of super sub)]
      (takel-untili i super)
      super)))

(defalias take-until takel-until)

(defnt takel-until-inc
  ([sub super]
    (if-let [i (index-of super sub)]
      (takel-untili (+ i (count sub)) super)
      super)))

(defalias take-until-inc takel-until-inc)

; ============ TAKE-RIGHT ============
(defn taker 
  [i super]
  (subseq super (- (count super) i) (lasti super)))

(defn takeri
  "Take up to and including index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "cdefg"}
  [i super]
  (subseq super i (lasti super)))

(defn taker-untili
  "Take until index, starting at the right."
  {:in  [2 "abcdefg"]
   :out "defg"}
  [i super]
  (subseq super (-> i long inc) (lasti super)))

(defn taker-while
  {:todo ["Use rreduce (reversed reduce) instead of reverse. Possibly reversed-last-index-of"]}
  [pred super]
  (let [rindex
          (reduce
            (fn [i elem]
              (if (pred elem) (dec i) (reduced i)))
            (count super)
            (reverse super))]
    (subseq super rindex (lasti super))))

(defn taker-until-workaround
  ([sub super]
      (taker-until-workaround sub super super))
    ([sub alt super]
      (let [i (last-index-of super sub)]
        (if i
            (taker-untili i super)
            alt))))

(defnt taker-until*
  "Take until index of, starting at the right."
  {:in  ["c" "abcdefg"]
   :out "defg"}
  ([^fn? pred super] (taker-while (fn-not pred) super)))

; This is how to handle variadic protocols 
#?(:clj
(defmacro taker-until
  [& args]
  (if (-> args count (= 2))
      `(taker-until* ~@args)
      `(taker-until-workaround ~@args))))

(defn taker-after
  {:in ["." "abcdefg.asd"]
   :out "abcdefg"}
  [sub super]
  (if-let [i (last-index-of super sub)]
    (subseq super 0 (-> i long dec))
    nil))

; ================================================ DROP ================================================

(defn dropl [n coll] (subseq coll n (lasti coll)))

(def drop dropl)

(def ldropl core/drop)

(defn dropr
  {:in  [3 "abcdefg"]
   :out "abcd"}
  [n coll]
  (subseq coll 0 (-> coll lasti long (- (long n)))))

(defn ldropr
  {:attribution "Alex Gunnarson"}
  [n coll]
  (lsubseq coll 0 (-> coll lasti long (- (long n)))))

(defn dropr-while [pred super]
  (subseq super 0
    (dec (or (last-index-of-pred super pred)
             (count super)))))

(defnt dropr-until
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "abcd"}
  ([^fn? pred super] (dropr-while super (fn-not pred)))
  ([sub super]
    (if-let [i (last-index-of super sub)]
      (subseq super 0 (+ i (lasti sub)))
      super)))

(defn dropr-after
  "Until right index of."
  {:todo "Combine code with /takeri/"
   :in  ["cd" "abcdefg"]
   :out "ab"}
  ([sub super]
    (if (index-of super sub)
        (subseq super 0 (index-of super sub))
        super)))

; (let [index-r
;           (whenc (last-index-of super sub) (fn= -1)
;             (throw (str "Index of" (str/squote sub) "not found.")))])
;   (subseq super
;     (inc (last-index-of super sub))
;     (-> super lasti))

; ================================================ MERGE ================================================

(defn index-with [coll f]
  (->> coll
       (map+ #(map-entry (f %) %))
       redm))

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

(defn coll-if [obj]
  (whenf obj (fn-not coll?) vector))

(defn seq-if [obj]
  (condf obj
    (fn-or seq? nil?) identity
    coll?             seq
    :else             list))
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


(defn fkey+ [m]
  (-> m first key))
(defn fval+ [m]
  (-> m first val))

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
(defn reverse+ [coll] ; what about arrays? some transient loop or something
  (ifn coll reversible? rseq reverse))
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
(defn in?
  "The inverse of |contains?|"
  {:todo ["|definline| this?"]}
  [elem coll] (contains? coll elem))

(defn in-k?
  {:todo ["|definline| this?"]}
  [elem coll] (containsk? coll elem))

(defn in-v?
  {:todo ["|definline| this?"]}
  [elem coll] (containsv? coll elem))

; ;-----------------------{       SELECT-KEYS       }-----------------------
(defn- select-keys-large
  "A transient and reducing version of clojure.core's |select-keys|."
  {:performance
    "45.3 ms vs. core's 60.29 ms on:
     (dotimes [_ 100000]
       (select-keys
         {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7}
         [:b :c :e]))).
     Performs much better on large set of keys."} 
  [keyseq m]
    (-> (transient {})
        (loops/reduce
          (fn [ret k]
            (let [entry (find m k)]
              (if entry
                  (conj! ret entry)
                  ret)))
          (seq keyseq))
        persistent!
        (with-meta (meta m))))

(defn- select-keys-small
  "A transient version of clojure.core's |select-keys|.

   Note: using a reducer here incurs the overhead of creating a
   function on the fly (can't extern it because of a closure).
   This is better for small set of keys."
  {:performance
    "39.09 ms vs. core's 60.29 ms on:
     (dotimes [_ 100000]
       (select-keys
         {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7}
         [:b :c :e])))"} 
  [keyseq m]
    (loop [ret (transient {}) keys (seq keyseq)]
      (if keys
        (let [entry (find m (first keys))]
          (recur
           (if entry
             (conj! ret entry)
             ret)
           (next keys)))
        (with-meta (persistent! ret) (meta m)))))

(defn- select-keys-delay
  "Not as fast as select-keys with transients."
  {:todo ["FIX THIS"]}
  [ks m]
  (let [ks-set (into #{} ks)]
    (->> m
         (filter+
           (compr key (f*n in-k? ks-set))))))

(defn select-keys+
  {:todo
    ["Determine actual inflection point at which select-keys-large
      should be used over select-keys-small."]}
  [m ks]
  (if (-> ks count (> 10))
      (select-keys-small m ks)
      (select-keys-large m ks)))

; ;-----------------------{       CONTAINMENT       }-----------------------

; ; index-of-from [o val index-from] - index-of, starting at index-from
; (defn contains-or? [coll elems]
;   (apply-or (map (partial contains? coll) elems)))
(defn get-keys
  {:attribution "Alex Gunnarson"}
  [m obj]
  (persistent!
    (loops/reduce
      (fn [ret k v]
        (if (identical? obj v)
            (conj! ret k)
            ret))
      (transient [])
      m)))
(defn get-key [m obj] (-> m (get-keys obj) first))
; ; /find/ Returns the map entry for key, or nil if key not present
; ; (find {:b 2 :a 1 :c 3} :a) => [:a 1]
; ; (select-keys {:a 1 :b 2} [:a :c]) =>  {:a 1}
; ; /frequencies/
; ; (frequencies ['a 'b 'a 'a])
; ; {a 3, b 1}
; ; /partition-by/
; ; splits the coll each time f returns a new value
; ; (partition-by odd? [1 1 1 2 2 3 3])
; ; => ((1 1 1) (2 2) (3 3)) /lseq/
;___________________________________________________________________________________________________________________________________
;=================================================={  FILTER + REMOVE + KEEP  }=====================================================
;=================================================={                          }=====================================================
(defn map-keys+ [f coll] (->> coll (map+ (juxt-kv f identity))))
(defn map-vals+ [f coll] (->> coll (map+ (juxt-kv identity f))))


(defn filter-keys+ [pred coll] (->> coll (filter+ (compr key pred))))
(defn remove-keys+ [pred coll] (->> coll (remove+ (compr key pred))))
(defn filter-vals+ [pred coll] (->> coll (filter+ (compr val pred))))
(defn remove-vals+ [pred coll] (->> coll (remove+ (compr val pred))))

(defn vals+
  {:attribution "Alex Gunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ val) redv))
(defn keys+
  {:attribution "Alex Gunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ key) redv))
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
  (loop [n-n n-0 slices [] items (vec coll)]
    (if (empty? items)
      slices
      (let [size (num/ceil (/ (count items) n-n))]
        (recur (dec n-n)
               (conj slices (subvec+ items 0 size))
               (subvec+ items size (lasti items)))))))
; /partition/
; (partition 4 (range 20))
; => ((0 1 2 3) (4 5 6 7) (8 9 10 11) (12 13 14 15) (16 17 18 19))
; (partition 4 6 ["a" "b" "c" "d"] (range 20))
; => ((0 1 2 3) (6 7 8 9) (12 13 14 15) (18 19 "a" "b"))
; /partition-all/
; Returns a lazy sequence of lists like partition, but may include
; partitions with fewer than n items at the end.
; (partition-all 4 [0 1 2 3 4 5 6 7 8 9])
; => ((0 1 2 3) (4 5 6 7) (8 9))

(defn partition-all
  ([n] (core/partition-all n))
  ([n coll]
    (if (= n 0) (list) (core/partition-all n coll)))
  ([n step coll]
    (core/partition-all n step coll)))
;___________________________________________________________________________________________________________________________________
;=================================================={  DIFFERENTIAL OPERATIONS }=====================================================
;=================================================={     take, drop, split    }=====================================================
; /take-nth/
; (take-nth 2 (range 10))
; => (0 2 4 6 8)
; /cycle/
; (take 5 (cycle ["a" "b"]))
; => ("a" "b" "a" "b" "a")
; /take-last/ ; turn this into a subvec
; (take-last 2 [1 2 3 4]) => (3 4)
; /last/    is a limiting case (1) of take-last
; /drop-last/ ; (drop-last 2 [1 2 3 4]) => (1 2)
; /butlast/ is a limiting case (1) of drop-last

; splice [o index n val] - fast remove and insert in one go
; splice-arr [o index n val-arr] - fast remove and insert in one go
; insert-before [o index val] - insert one item inside coll
; insert-before-arr [o index val] - insert array of items inside coll
; remove-at [o index] - remove one item from index pos
; remove-n [o index n] - remove n items starting at index pos
; triml [o n] - trims n items from left
; trimr [o n] - trims n items from right
; trim [o nl nr] - trims nl items from left and nr items from right
; rip [o index] - rips coll and returns [pre-coll item-at suf-coll]
; sew [pre-coll item-arr suf-coll] - opposite of rip, but with arr
(defn split [ind coll-0]
  (if (vector? coll-0)
      [(subvec+ coll-0 0   ind)
       (subvec+ coll-0 ind (count coll-0))]
      (split-at coll-0 ind)))
(defn split-with-v+ [pred coll-0] ; IMPROVE
  (->> coll-0
       (split-with pred)
       (map+ vec)))
;_._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._._
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{        ASSOCIATIVE       }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*{                          }=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=*=
;___________________________________________________________________________________________________________________________________
;=================================================={          ASSOC           }=====================================================
;=================================================={ update(-in), assoc(-in)  }=====================================================
(defn- extend-coll-to
  "Extends an associative structure (for now, only vector) to a given index."
  {:attribution "Alex Gunnarson"
   :usage "USAGE: (extend-coll-to [1 2 3] 5) => [1 2 3 nil nil]"}
  [coll-0 k]
  (if (and (vector? coll-0)
           (number? k)
           (-> coll-0 count dec (< k)))
      (let [trans?   (transient? coll-0)
            trans-fn (if trans? identity transient)
            pers-fn  (if trans? identity persistent!)]
        (pers-fn
          (loops/reduce
            (fn [coll-n _] (conj! coll-n nil)) ; extend-vec part
            (trans-fn coll-0)
            (range (count coll-0) (inc k)))))
      coll-0))

(defn assoc+
  {:todo ["Protocolize on IEditableCollection"
          "Probably has performance issues"
          "Stop the assoc things"]}
  ([coll-0 k v]
    (assoc (extend-coll-to coll-0 k) k v))
    ; once probably gives no performance benefit from transience
  ([coll-0 k v & kvs-0]
    (let [edit?    (editable? coll-0)
          trans-fn (if edit? transient   identity)
          pers-fn  (if edit? persistent! identity)
          assoc-fn (if edit? #(assoc! %1 %2 %3) #(assoc %1 %2 %3))]
      (loop [kvs-n  kvs-0
             coll-f (-> coll-0 trans-fn
                        (extend-coll-to k)
                        (assoc-fn k v))]
        (if (empty? kvs-n)
            (pers-fn coll-f)
            (recur (-> kvs-n rest rest)
                   (let [k-n (first kvs-n)]
                     (-> coll-f (extend-coll-to k-n)
                         (assoc-fn k-n (second kvs-n))))))))))

(defn assoc-when-none 
  "assoc's @args to @m only when the respective keys are not present in @m."
  [m & args]
  (reduce
    (fn [m-f k v]
      (if (contains? m-f k)
          m-f
          (assoc m-f k v)))
    m
    (partition-all 2 args)))

(defn update+
  "Updates the value in an associative data structure @coll associated with key @k
   by applying the function @f to the existing value."
  ^{:attribution "weavejester.medley"
    :contributors ["Alex Gunnarson"]}
  ([coll k f]      (assoc+ coll k       (f (get coll k))))
  ([coll k f args] (assoc+ coll k (apply f (get coll k) args))))

(defn update-when
  "Updates only if @pred is true for @k in @m."
  [m k pred f]
  (if (-> m k pred)
      (update m k f)
      m))

(defn updates+
  "For each key-function pair in @kfs,
   updates value in an associative data structure @coll associated with key
   by applying the function @f to the existing value."
  ^{:attribution "Alex Gunnarson"
    :todo ["Probably updates and update are redundant"]}
  ([coll & kfs]
    (reduce-2 ; This is inefficient
      (fn [ret k f] (update+ ret k f))
      coll
      kfs)))

(defn update-key+
  {:attribution "Alex Gunnarson"
   :usage '(->> {:a 4 :b 12}
                (map+ (update-key+ str)))}
  ([f]
    (fn
      ([kv]
        (assoc+ kv 0 (f (get kv 0))))
      ([k v]
        (map-entry (f k) v)))))

(defn update-val+
  {:attribution "Alex Gunnarson"
   :usage '(->> {:a 4 :b 12}
                (map+ (update-val+ (f*n / 2))))}
  ([f]
    (fn
      ([kv]
        (assoc+ kv 1 (f (get kv 1))))
      ([k v]
        (map-entry k (f v))))))

(defn mapmux
  ([kv]  kv)
  ([k v] (map-entry k v)))
(defn record->map [rec]
  (into {} rec))

;--------------------------------------------------{        UPDATE-IN         }-----------------------------------------------------
(defn update-in!
  "'Updates' a value in a nested associative structure, where ks is a sequence of keys and
  f is a function that will take the old value and any supplied args and return the new
  value, and returns a new nested structure. The associative structure can have transients
  in it, but if any levels do not exist, non-transient hash-maps will be created."
  {:attribution "flatland.useful"}
  [m [k & ks] f & args]
  (let [assoc-fn (if ((fn-or transient?) m)
                     #(assoc! %1 %2 %3)
                     #(assoc  %1 %2 %3))
        val (get m k)]
    (assoc-fn m k
      (if ks
          (apply update-in! val ks f args)
          (apply f val args)))))
; perhaps make a version of update-in : update :: assoc-in : assoc ?

(defn update-in+
  "Created so vectors would also automatically be grown like maps,
   given indices not present in the vector."
  {:attribution "Alex Gunnarson"
   :todo ["optimize via transients"
          "allow to use :last on vectors"
          "allow |identity| function for unity's sake"]}
  [coll-0 [k0 & keys-0] v0]
  (let [value (core/get coll-0 k0 (when (-> keys-0 first number?) []))
        coll-f (extend-coll-to coll-0 k0)
        val-f (if keys-0
                  (update-in+ value keys-0 v0) ; make a non-stack-consuming version, possibly via trampoline? 
                  v0)
        final  (assoc coll-f k0 (whenf val-f fn? #(% (get coll-f k0))))]
   final))
;--------------------------------------------------{         ASSOC-IN         }-----------------------------------------------------
(defn assoc-in+
  [coll ks v]
  (update-in+ coll ks (constantly v)))

(defnt assoc-in!
  "Associates a value in a nested associative structure, where ks is a sequence of keys
  and v is the new value and returns a new nested structure. The associative structure
  can have transients in it, but if any levels do not exist, non-transient hash-maps will
  be created."
  {:attribution "flatland.useful"}
  ([^atom? m ks obj] (swap! m assoc-in ks obj))
  ([m ks v]
    (update-in! m ks (constantly v))))

(defn assocs-in+
  {:usage "(assocs-in ['file0' 'file1' 'file2']
             [0] 'file10'
             [1] 'file11'
             [2] 'file12')"}
  [coll & kvs]
  (reduce-2 ; this is inefficient
    (fn [ret k v] (assoc-in+ ret k v))
    coll
    kvs))
;___________________________________________________________________________________________________________________________________
;=================================================={          DISSOC          }=====================================================
;=================================================={                          }=====================================================
(defn dissoc+
  {:todo ["Protocolize"]}
  ([coll key-0]
    (try
      (cond ; probably use tricks to see which subvec is longer to into is less consumptive
        (vector? coll)
          (catvec (subvec+ coll 0 key-0)
                  (subvec+ coll (inc key-0) (count coll)))
        (editable? coll)
          (-> coll transient (core/dissoc! coll key-0) persistent!)
        :else
          (dissoc coll key-0))))
  ([coll key-0 & keys-0]
    (loops/reduce dissoc+ coll (cons key-0 keys-0))))

(defn dissocs+ [coll & ks]
  (loops/reduce
    (fn [ret k]
      (dissoc+ ret k))
    coll
    ks))

(defn dissoc-if+ [coll pred k] ; make dissoc-ifs+
  (whenf coll (fn-> (get k) pred)
    (f*n dissoc+ k)))

(defnt dissoc++
  {:todo ["Move to collections.core"
          "Implement for vector"]}
  ([#{map?} coll obj] (dissoc coll obj))
  ([^:obj   coll obj] (dissoc coll obj))
  ([^set?   coll obj] (disj   coll obj)))

(defn dissoc-in+
  "Dissociate a value in a nested assocative structure, identified by a sequence
  of keys. Any collections left empty by the operation will be dissociated from
  their containing structures.
  This implementation was adapted from clojure.core.contrib"
  {:attribution "weavejester.medley"
   :todo ["Transientize"]}
  [m ks]
  (if-let [[k & ks] (seq ks)]
    (if (empty? ks)
        (dissoc++ m k)
        (let [new-n (dissoc-in+ (get m k) ks)] ; this is terrible
          (if (empty? new-n) ; dissoc's empty ones
              (dissoc++ m k)
              (assoc m k new-n))))
    m))

(defn updates-in+
  [coll & kfs]
  (reduce-2 ; Inefficient
    (fn [ret k-n f-n] (update-in+ ret k-n f-n))
    coll
    kfs))

(defn re-assoc+ [coll k-0 k-f]
  (if (containsk? coll k-0)
      (-> coll
         (assoc+  k-f (get coll k-0))
         (dissoc+ k-0))
      coll))

(defn re-assocs+ [coll & kfs]
  (reduce-2 ; Inefficient
    (fn [ret k-n f-n] (re-assoc+ ret k-n f-n))
    coll
    kfs))

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
(defn distinct-by
  "Returns a lazy sequence of the elements of coll, removing any elements that
  return duplicate values when passed to a function f."
  {:attribution "medley.core"}
  [f coll]
  (let [step (fn step [xs seen]
               (lazy-seq
                ((fn [[x :as xs] seen]
                   (when-let [s (seq xs)]
                     (let [fx (f x)]
                       (if (contains? seen fx) 
                         (recur (rest s) seen)
                         (cons x (step (rest s) (conj seen fx)))))))
                 xs seen)))]
    (step coll #{})))

#?(:clj
(defn distinct-by-java
  "Returns elements of coll which return unique
   values according to f. If multiple elements of coll return the same
   value under f, the first is returned"
  {:attribution "prismatic.plumbing"
   :performance "Faster than |distinct-by|"}
  [f coll]
  (let [s (java.util.HashSet.)] ; instead of #{}
    (lfor [x coll
           :let [id (f x)]
           :when (not (.contains s id))]
     (do (.add s id)
         x)))))

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
;          redv)))

; TODO: make a reducers version of coll/elem
(defnt interpose+-
  ([^string? coll elem]
    (str/join elem coll))
  ([coll elem]
    (interpose elem coll)))

(defn interpose+
  {:todo ["|definline| this"]}
  [elem coll] (interpose+- coll elem))

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

; (defn partition-all*
;   "Groups the elements of @coll into groups of @n."
;   {:attribution "Alex Gunnarson"
;    :todo "Compare performance to |partition-all|"}
;   [coll n]
;   (loop [coll-n coll]
;     (if (-> coll-n count (< n))
;         coll-n
;         (recur (ltake n coll)))))

; (defn interleave+ [& args] ; 4.307220 ms vs. 1.424329 ms normal interleave :/ because of zipvec...
;   (reduce
;     (fn ([]      [])
;         ([a]     (conj [] a))
;         ([a b]   (conj    a b)) 
;         ([a b c] (conj    a b c)))
;     (apply zipvec+ args)))

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
      (into {} res))))
;___________________________________________________________________________________________________________________________________
;=================================================={         GROUPING         }=====================================================
;=================================================={     group, aggregate     }=====================================================
(defn ^Delay group-merge-with+
  {:attribution "Alex Gunnarson"
   :todo ["Can probably make the |merge| process parallel."]
   :in [":a"
        "(fn [k v1 v2] v1)"
        "[{:a 1 :b 2} {:a 1 :b 5} {:a 5 :b 65}]"]
   :out "[{:b 65, :a 5} {:a 1, :b 2}]"}
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
;___________________________________________________________________________________________________________________________________
;=================================================={     TREE STRUCTURES      }=====================================================
;=================================================={                          }=====================================================
; Stuart Sierra: "In my tests, clojure.walk2 is about 2 times faster than clojure.walk."
(defnt walk2
  "If coll is a collection, applies f to each element of the collection
   and returns a collection of the results, of the same type and order
   as coll. If coll is not a collection, returns it unchanged. \"Same
   type\" means a type with the same behavior. For example, a hash-map
   may be returned as an array-map, but a a sorted-map will be returned
   as a sorted-map with the same comparator."
  {:todo ["Fix class overlap" "Preserve metadata"]}
  ; clojure.lang.PersistentList$EmptyList : '()
  ; special case to preserve type
  ([^list?  coll f] (apply list  (map f coll)))
  #_([^dlist? coll f] (apply dlist (map f coll)))  ; TODO ENABLE THIS UPON RESTART
  ([^transientizable? coll f]
     (persistent!
       (core/reduce
         (fn [r x] (core/conj! r (f x)))
         (transient (core/empty coll)) coll)))
  ; generic sequence fallback
  ; TODO add any seq in general
  ([#{lseq? #_(- seq? list?)} coll f] (map f coll))
  ; |transient| discards metadata as of Clojure 1.6.0

  ; Persistent collections that don't support transients
  #?(:clj  ([#{clojure.lang.PersistentQueue
               clojure.lang.PersistentStructMap
               clojure.lang.PersistentTreeMap
               clojure.lang.PersistentTreeSet} coll f]
             (core/reduce
               (fn [r x] (conj r (f x)))
               (empty coll) coll)))
  #?(:clj  ([^map-entry? coll f] (map-entry (f (key coll)) (f (val coll)))))
  #?(:clj  ([^record?    coll f] (core/reduce (fn [r x] (conj r (f x))) coll coll)))
  #?(:clj  ([:else       x    f] x))
  #?(:cljs ([:else obj  f]
             (if (coll? obj)
                 (into (empty obj) (map f obj))
                 obj))))

(defn walk
  "Traverses form, an arbitrary data structure.  inner and outer are
  functions.  Applies inner to each element of form, building up a
  data structure of the same type, then applies outer to the result.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [inner outer form]
  (outer (walk2 form inner)))

(defn postwalk
  "Performs a depth-first, post-order traversal of form.  Calls f on
  each sub-form, uses f's return value in place of the original.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [f form]
  (walk (partial postwalk f) f form))

(defn prewalk
  "Like postwalk, but does pre-order traversal."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [f form]
  (walk (partial prewalk f) identity (f form)))

; COMBINE THESE TWO
(defn keywordify-keys
  "Recursively transforms all map keys from keywords to strings."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"
   :contributors #{"Alex Gunnarson"}}
  [m]
  (let [keywordify-key
         (fn [k v]
           (if (string? k)
               (map-entry (keyword k) v)
               (map-entry k v)))]
    ; only apply to maps
    (postwalk
      (whenf*n map? (fn->> (map+ keywordify-key) redm))
      m)))

; COMBINE THESE TWO
(defn stringify-keys
  "Recursively transforms all map keys from keywords to strings."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"
   :contributors #{"Alex Gunnarson"}}
  [m]
  (let [stringify-key
         (fn [[k v]]
           (if (keyword? k)
               (map-entry (name k) v)
               (map-entry k v)))]
    ; only apply to maps
    (postwalk
      (whenf*n map? (fn->> (map+ stringify-key) redm))
      m)))

(defn properize-key [k v]
  (let [k-0 (str/keywordize k)
        k-f (if (boolean? v)
                (str/keyword+ k-0 "?")
                k-0)]
    k-f))

(defn properize-keys-1
  "Properizes keys for @m.
   Only one level deep."
  [m]
  (->> m
       (map+ (fn [k v] (map-entry (properize-key k v) v)))
       redm))

(defn prewalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values.  Like clojure/replace but works on any data structure.  Does
  replacement at the root of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (prewalk (whenf*n (f*n in-k? smap) smap) form))

(defn postwalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values.  Like clojure/replace but works on any data structure.  Does
  replacement at the leaves of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (postwalk (whenf*n (f*n in-k? smap) smap) form))

(defn tree-filter
  "Like |filter|, but performs a |postwalk| on a treelike structure @tree, putting in a new vector
   only the elements for which @pred is true."
  {:attribution "Alex Gunnarson"}
  [pred tree]
  (let [results (transient [])]
    (postwalk
      (whenf*n pred
        (fn->> (withf->> #(conj! results %)))) ; keep it the same
      tree)
    (persistent! results)))

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
                 (into (sorted-set-by (fn [a b] (> a b))))
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

#?(:clj
(defn ^java.util.function.Predicate ->predicate [f]
  (reify java.util.function.Predicate
    (^boolean test [this ^Object elem]
      (f elem)))))

#?(:clj
(defnt filter!
  ([^javafx.collections.ObservableList coll pred]
    (doseqi [elem coll n]
      (when-not (pred elem)
        (.remove coll (int n) (int n)))))
  ([^javafx.collections.transformation.FilteredList coll pred]
    (.setPredicate coll (->predicate pred)))))

#?(:clj
(defnt remove!
  ([^javafx.collections.ObservableList coll pred]
    (doseqi [elem coll n]
      (when (pred elem)
        (.remove coll (int n) (int n)))))
  ([^javafx.collections.transformation.FilteredList coll pred]
    (.setPredicate coll (->predicate (fn-not pred))))))

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
  ([^vector? ensurer ensured]
    (cond
      (vector? ensured)
        ensured
      (nil? ensured)
        ensurer
      :else (vector ensured))))

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

(defn assoc-with
  "Like |merge-with| but for |assoc|."
  {:author "Alex Gunnarson"
   :tests '{(assoc-with {:a 1} (fn [a b] (-> a (ensurec []) (conj b))) :a 3)
            {:a [1 3]}}}
  [m f k v]
  (if-let [v-0 (get m k)]
    (assoc m k (f v-0 v))
    (assoc m k v)))

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
      (->> l (into #{}))
      (->> r (into #{}))))
  ([l r selectors]
    (let [l-grouped (->> l (group-by (apply juxt selectors)))
          r-grouped (->> r (group-by (apply juxt selectors)))]
      (->> (set/difference
             (->> l-grouped keys (into #{}))
             (->> r-grouped keys (into #{})))
           (map+ (fn->> (get l-grouped))) force
           (reduce into #{})))))

; TODO extern would be better...
#_(defn+ extreme-comparator [comparator-n]
  (get (extern {> num/greatest
                < num/least   })
    comparator-n))

#_(defn extreme-comparator [comparator-n]
  (get {> num/greatest
        < num/least   }
    comparator-n))

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
(defmacro deficlass
  "Define an immutable class.
   Based on the limitations of |defrecord|, multi-arity
   functions are declared separately."
  [name- fields constructor & fns]
  (let [constructor-sym (->> name- name sform/un-camelcase (str "->") symbol)
        protocol-sym    (-> name- name (str "Functions") symbol)
        fns-signatures  (->> fns
                             (map (compr (juxt (MWA first)
                                               (MWA second))))
                             (group-by (MWA first))
                             (map-vals+ (fn->> (map (fn-> rest)) flatten-1))
                             (map+ (fn [x] (cons (first x) (second x))))
                             redv) ]
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