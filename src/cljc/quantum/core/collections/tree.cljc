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
  quantum.core.collections.tree
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
     empty empty?
     split-at
     first second rest last butlast get pop peek
     select-keys
     zipmap
     reverse
     conj
     conj! assoc! dissoc! disj!
     boolean?])
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   )             :as core]
    [quantum.core.collections.core :as coll
      :refer        [#?@(:clj [first conj!])]
      :refer-macros [          first conj!]]
    [quantum.core.collections.map-filter     :as mf
      :refer        [map-keys+]]
    [quantum.core.collections.selective :as sel
      :refer        [in-k?]]
    [quantum.core.collections.zip       :as zip
      :refer        [#?@(:clj [walking])]
      :refer-macros [walking]]
    [quantum.core.fn                    :as fn
      :refer        [withf->>
                     #?@(:clj [fn1 fn->>])]
      :refer-macros [          fn1 fn->>]]
    [quantum.core.logic
      :refer        [#?@(:clj [whenf1])]
      :refer-macros [          whenf1]]
    [quantum.core.reducers              :as red
     :refer        [#?@(:clj [join])]
     :refer-macros [          join]]
    [quantum.core.string                :as str]))
;___________________________________________________________________________________________________________________________________
;=================================================={     TREE STRUCTURES      }=====================================================
;=================================================={                          }=====================================================
(defn walk
  "Traverses form, an arbitrary data structure.  inner and outer are
  functions.  Applies inner to each element of form, building up a
  data structure of the same type, then applies outer to the result.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [inner outer form]
  (outer (walking form inner)))

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

(defn prewalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values. Like clojure/replace but works on any data structure. Does
  replacement at the root of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (prewalk (whenf1 (fn1 in-k? smap) smap) form))

(defn postwalk-replace
  "Recursively transforms form by replacing keys in smap with their
  values. Like clojure/replace but works on any data structure. Does
  replacement at the leaves of the tree first."
  {:attribution "Stuart Sierra, stuartsierra/clojure.walk2"}
  [smap form]
  (postwalk (whenf1 (fn1 in-k? smap) smap) form))

(defn tree-filter
  "Like |filter|, but performs a |postwalk| on a treelike structure @tree, putting in a new vector
   only the elements for which @pred is true."
  {:attribution "Alex Gunnarson"}
  [pred tree]
  (let [results (transient [])]
    (postwalk
      (whenf1 pred
        (fn->> (withf->> #(conj! results %)))) ; keep it the same
      tree)
    (persistent! results)))

#?(:clj (defn prewalk-find [pred x] ; can't find nil but oh well ; TODO clean
  (cond (try (pred x)
          (catch Throwable _ false))
        [true x]
        (instance? clojure.lang.Seqable x) ;
        (let [x' (first (filter #(first (prewalk-find pred %)) x))]
          (if (nil? x') [false x'] [true x']))
        :else [false nil])))

; ===== Transform nested maps =====

(defn apply-to-keys
  {:attribution "Alex Gunnarson"}
  ([m] (apply-to-keys m identity))
  ([m f]
    (postwalk
      (whenf1 map? (fn->> (map-keys+ f) (join {})))
      m)))

(defn keywordize-keys
  "Recursively transforms all map keys from strings to proper keywords."
  {:attribution "Alex Gunnarson"}
  [x]
  (apply-to-keys x str/keywordize))

(defn keywordify-keys
  "Recursively transforms all map keys from strings to keywords."
  {:attribution "Alex Gunnarson"}
  [x]
  (apply-to-keys x (whenf1 string? keyword)))

(defn stringify-keys
  "Recursively transforms all map keys from keywords to strings."
  {:attribution "Alex Gunnarson"}
  [x]
  (apply-to-keys x (whenf1 keyword? name)))

; ZIPPER TREES

(defn zip-walk
  "|walk| for zippers.
   @inner and @outer must both return a non-zipper."
  ([inner outer form] (zip/node (zip-walk inner outer nil (zip/zipper form))))
  ([inner outer _ loc-0]
    (let [[i loc] (loop [i    0
                         loc  loc-0]
                    (if-let [loc' (if (zero? i)
                                      (zip/down  loc)
                                      (zip/right loc))]
                      (let [innered (inner loc')
                            _ (assert (not (instance? fast_zip.core.ZipperLocation innered))
                                      {:innered innered
                                       :derefed (zip/node innered)
                                       :arg     (zip/node loc')})
                            replaced (zip/replace loc' innered)]
                        (recur (inc i)
                               replaced))
                      [i loc]))
          loc' (if (> i 0) (zip/up loc) loc)
          outered (outer loc')
          _ (assert (not (instance? fast_zip.core.ZipperLocation outered)) {:outered outered})
          ret (zip/replace loc' outered)
          _ (assert (instance? fast_zip.core.ZipperLocation ret) {:ret ret})]
      ret)))

(defn zip-postwalk
  "|postwalk| with zippers.
   @f must return a non-zipper."
  ([f form    ] (zip/node* (zip-postwalk f nil (zip/zipper form))))
  ([f _    loc] (zip-walk (comp zip/node #(zip-postwalk f nil %)) f nil loc)))

(defn zip-prewalk
  "|prewalk| with zippers.
   @f must return a non-zipper."
  ([f form    ] (zip/node* (zip-prewalk f nil (zip/zipper form))))
  ([f _    loc] (zip-walk (comp zip/node #(zip-prewalk f nil %)) zip/node nil
                  #_(zip/update f loc)
                  (zip/replace loc (f loc)))))
