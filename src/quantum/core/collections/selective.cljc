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
    :attribution "alexandergunnarson"}
  quantum.core.collections.selective
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
    [quantum.core.collections.core :as coll
      :refer        [key val
                     #?@(:clj [first conj! contains? containsk? containsv?])]
      :refer-macros [          first conj! contains? containsk? containsv?]]
    [quantum.core.fn               :as fn
      :refer        [#?@(:clj [fn1 rcomp])]
      :refer-macros [          fn1 rcomp]]
    [quantum.core.reducers         :as red
      :refer        [map+ filter+
                     #?@(:clj [reduce join])]
      :refer-macros [          reduce join]]))

(defn in?
  "The inverse (converse?) of |contains?|"
  {:todo ["|definline| this?"]}
  [elem coll] (contains? coll elem))

(defn in-k?
  {:todo ["|definline| this?"]}
  [elem coll] (containsk? coll elem))

(defn in-v?
  {:todo ["|definline| this?"]}
  [elem coll] (containsv? coll elem))

; ;-----------------------{       SELECT-KEYS       }-----------------------
(defn select-keys
  "A transient and reducing version of clojure.core's |select-keys|."
  {:performance
    "45.3 ms vs. core's 60.29 ms on:
     (dotimes [_ 100000]
       (select-keys
         {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7}
         [:b :c :e]))).
     Performs much better on large set of keys."}
  [keyseq m]
    (-> (reduce
          (fn [ret k]
            (let [entry (find m k)]
              (if entry
                  (conj! ret entry)
                  ret)))
          (transient {})
          (seq keyseq))
        persistent!
        (with-meta (meta m))))

(defn select-keys+
  "Not as fast as select-keys with transients."
  {:todo ["Fix performance"]}
  [ks m]
  (let [ks-set (set ks)]
    (->> m
         (filter+
           (rcomp key (fn1 in-k? ks-set))))))

; ;-----------------------{       CONTAINMENT       }-----------------------

; ; index-of-from [o val index-from] - index-of, starting at index-from
; (defn contains-or? [coll elems]
;   (seq-or (map (partial contains? coll) elems)))
(defn get-keys
  {:attribution "alexandergunnarson"}
  [m obj]
  (persistent!
    (reduce
      (fn [ret k v]
        (if (identical? obj v)
            (conj! ret k)
            ret))
      (transient [])
      m)))

(defn get-key
  {:todo ["Wasteful lack of performance"]}
  [m obj] (-> m (get-keys obj) first))

(defn vals+
  {:attribution "alexandergunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ val) (join [])))

(defn keys+
  {:attribution "alexandergunnarson"
   :todo ["Compare performance with core functions"]}
  [m]
  (->> m (map+ key) (join [])))
