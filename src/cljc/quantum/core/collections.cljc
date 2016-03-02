(ns quantum.core.collections
  (:require-quantum [:core fn logic set map err])
  (:require [quantum.core.type.predicates :refer [editable?]]
            [clojure.walk :refer [postwalk]]))

(defn reduce-map
  {:from "r0man/noencore"}
  [f coll]
  (if (editable? coll)
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

(defn safe-mapcat
  "Like |mapcat|, but works if the returned values aren't sequences."
  {:from "clojure.jvm.tools.analyzer.examples.tail-recursion"}
  [f & colls]
  (apply concat (map #(if (seq? %) % [%]) (apply map f colls))))

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