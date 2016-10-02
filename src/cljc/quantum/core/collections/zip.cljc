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
  quantum.core.collections.zip
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
    [#?(:clj  clojure.core
        :cljs cljs.core   )        :as core]
    [fast-zip.core                 :as zip]
    [quantum.core.data.map         :as map
      :refer        [map-entry]]
    [quantum.core.data.vector      :as vec
      :refer        [vec+]]
    [quantum.core.collections.base :as base]
    [quantum.core.collections.core :as coll
      :refer        [key val
                     #?@(:clj [first second conj conj! empty])]
      :refer-macros [          first second conj conj! empty]]
    [quantum.core.fn               :as fn
      :refer        [#?@(:clj [with-do])]
      :refer-macros [          with-do]]
    [quantum.core.macros           :as macros
      :refer        [#?@(:clj [defnt])]
      :refer-macros [          defnt]]
    [quantum.core.reducers         :as red
      :refer        [#?@(:clj [join])]
      :refer-macros [          join]]
    [quantum.core.vars             :as var
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]))

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
  ([^list? coll f        ] (apply list (map f coll)))
  ([^list? coll _ to-join] (apply list to-join))
  ([^transientizable? coll f]
     (with-meta
       (persistent!
         (core/reduce
           (fn [r x] (core/conj! r (f x)))
           (transient (empty coll)) coll))
       (meta coll)))
  ([^transientizable? coll _ to-join]
     (with-meta
       (join (empty coll) to-join)
       (meta coll)))
  ; generic sequence fallback
  ; TODO add any seq in general

  ([#{cons? lseq? misc-seq? queue?} coll f        ] (map f coll))
  ([#{cons? lseq? misc-seq? queue?} coll _ to-join] (seq to-join))
  ([^vec+? coll f        ] (vec+ (mapv f coll)))
  ([^vec+? coll _ to-join] (vec+ to-join))
  ; Persistent collections that don't support transients
  #?(:clj  ([#{clojure.lang.PersistentStructMap
               clojure.lang.PersistentTreeMap
               clojure.lang.PersistentTreeSet} coll f]
             (core/reduce (fn [r x] (conj r (f x))) (empty coll) coll)))
  #?(:clj  ([#{clojure.lang.PersistentStructMap
               clojure.lang.PersistentTreeMap
               clojure.lang.PersistentTreeSet} coll _ to-join]
             (core/reduce conj (empty coll) to-join)))
  #?(:clj  ([^map-entry? coll f        ] (map-entry (f (key coll)) (f (val coll)))))
  #?(:clj  ([^map-entry? coll _ to-join] (map-entry (first to-join) (second to-join))))
  #?(:clj  ([^record?    coll f]
             (core/reduce (fn [r x] (conj r (f x))) coll coll)))
  #?(:clj  ([^record?    coll _ to-join]
             (core/reduce conj coll to-join)))
  #?(:clj  ([:else       x    f] x))
  #?(:cljs ([:else       x    f] (if (coll? x) (join (empty x) (map f x)) x)))
  #?(:cljs ([:else       x    _ to-join]
             (if (coll? x) (join (empty x) to-join)))))
;___________________________________________________________________________________________________________________________________
;=================================================={     ZIPPERS     }=====================================================
;=================================================={                 }=====================================================

(defalias node    zip/node   )

(defn node* [x] (if (instance? fast_zip.core.ZipperLocation x) (node x) x))

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
