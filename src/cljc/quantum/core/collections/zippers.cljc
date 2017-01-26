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
  quantum.core.collections.zippers
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
     conj! assoc! dissoc! dissoc disj!
     boolean?
     replace remove update])
  (:require
    [clojure.core                  :as core]
    [fast-zip.core                 :as zip]
    [quantum.core.data.map         :as map
      :refer [map-entry]]
    [quantum.core.data.vector      :as vec
      :refer [vec+]]
    [quantum.core.collections.base :as base]
    [quantum.core.collections.core :as coll
      :refer [key val first second conj conj! empty]]
    [quantum.core.fn               :as fn
      :refer [with-do]]
    [quantum.core.macros           :as macros
      :refer [defnt]]
    [quantum.core.reducers         :as red
      :refer [join]]
    [quantum.core.vars             :as var
      :refer [replace-meta-from defalias] ]))

; Stuart Sierra: "In my tests, clojure.walk2 is about 2 times faster than clojure.walk."
(defnt walking
  "If @coll is a collection, applies @f to each element of the collection
   and returns a collection of the results, of the same type and order
   as coll. If coll is not a collection, returns it unchanged. \"Same
   type\" means a type with the same behavior. For example, a hash-map
   may be returned as an array-map, but a a sorted-map will be returned
   as a sorted-map with the same comparator."
  {:todo ["Fix class overlap" "fix clojure.lang.PersistentList$EmptyList"]}
  ; Special case to preserve type
  ([^list? coll f        ] (replace-meta-from (apply list (map f coll)) coll))
  ([^list? coll _ to-join] (replace-meta-from (apply list to-join     ) coll))
  ([^transientizable? coll f]
     (replace-meta-from
       (persistent!
         (core/reduce
           (fn [r x] (core/conj! r (f x)))
           (transient (empty coll)) coll))
       coll))
  ([^transientizable? coll _ to-join]
     (replace-meta-from (join (empty coll) to-join) coll))
  ; generic sequence fallback
  ; TODO add any seq in general
  ; TODO fix queue?
  ([#{cons? lseq? misc-seq? queue?} coll f        ]
    (replace-meta-from (map f coll) coll))
  ([#{cons? lseq? misc-seq? queue?} coll _ to-join]
    (replace-meta-from (seq to-join) coll))
  ([^vec+? coll f        ]
    (replace-meta-from (vec+ (mapv f coll)) coll))
  ([^vec+? coll _ to-join]
    (replace-meta-from (vec+ to-join) coll))
  ; Persistent collections that don't support transients
  #?(:clj  ([#{clojure.lang.PersistentStructMap
               clojure.lang.PersistentTreeMap
               clojure.lang.PersistentTreeSet} coll f]
             (replace-meta-from
               (core/reduce (fn [r x] (conj r (f x))) (empty coll) coll)
               coll)))
  #?(:clj  ([#{clojure.lang.PersistentStructMap
               clojure.lang.PersistentTreeMap
               clojure.lang.PersistentTreeSet} coll _ to-join]
             (replace-meta-from (core/reduce conj (empty coll) to-join) coll)))
  #?(:clj  ([^map-entry? coll f        ]
             (map-entry (f (key coll)) (f (val coll)))))
  #?(:clj  ([^map-entry? coll _ to-join]
             (map-entry (first to-join) (second to-join))))
  #?(:clj  ([^record?    coll f]
             (replace-meta-from (core/reduce (fn [r x] (conj r (f x))) coll coll) coll)))
  #?(:clj  ([^record?    coll _ to-join]
             (replace-meta-from (core/reduce conj coll to-join) coll)))
  #?(:clj  ([:else       x    f] x))
  #?(:cljs ([:else       x    f]
             (if (coll? x) (replace-meta-from (join (empty x) (map f x)) x) x)))
  #?(:cljs ([:else       x    _ to-join]
             (if (coll? x) (replace-meta-from (join (empty x) to-join) x)))))
;___________________________________________________________________________________________________________________________________
;=================================================={     ZIPPERS     }=====================================================
;=================================================={                 }=====================================================

(defalias node    zip/node   )

(defn ?node [x] (if (instance? fast_zip.core.ZipperLocation x) (node x) x))

(defalias up      zip/up     )
(defalias down    zip/down   )
(defalias left    zip/left   )
(defalias right   zip/right  )
(defalias replace zip/replace)
(defalias remove  zip/remove )
(defalias dissoc remove)

(defalias edit zip/edit)

(defn update
  "A slightly more performant (and threading-macro-last) alternative to zip/edit"
  [f ^fast_zip.core.ZipperLocation loc]
  (replace loc (f (.-node loc))))

(defn insert-left  [item loc] (zip/insert-left  loc item))
(defn insert-right [item loc] (zip/insert-right loc item))

(defalias zip-reduce* base/zip-reduce*)

(defn zipper
  "General-purpose zipper."
  {:attribution "Alex Gunnarson"}
  [coll]
  (zip/zipper coll? seq #(walking %1 nil %2) coll))

(defn zip-reduce [f init coll] (zip-reduce* f init (zipper coll)))

(defn zip-reduce-with
  [accumulator f post init coll loc]
  (let [loc* (volatile! (down loc))]
    (post
      (core/reduce
        (fn [r x] (with-do (accumulator r (f x @loc*))
                    (vswap! loc* right)))
        init coll))))

(defn zip-map-with [coll f loc]
  (let [loc* (volatile! (zip/down loc))]
    (map (fn [x] (with-do (f x @loc*)
                   (vswap! loc* right))) coll)))

(defn zip-mapv
  "Like `mapv` but allows zip functions to be applied to each elem."
  [f coll]
  (loop [ret  (transient [])
         elem (-> coll zipper down)]
    (if (nil? elem)
        (persistent! ret)
        (recur (conj! ret (f elem)) (right elem)))))

(defn right-until [pred z]
  (loop [z' z]
    (when z'
      (if (pred (node z'))
          z'
          (recur (right z'))))))

(defn left-until [pred z]
  (loop [z' z]
    (when z'
      (if (pred (node z'))
          z'
          (recur (left z'))))))
