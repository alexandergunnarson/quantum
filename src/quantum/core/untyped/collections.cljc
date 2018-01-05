(ns quantum.core.untyped.collections
  (:refer-clojure :exclude
    [assoc-in contains? get flatten])
  (:require
    [clojure.core    :as core]
    [fast-zip.core   :as zip]
    [quantum.core.fn :as fn
      :refer [fn']]
    [quantum.core.logic
      :refer [condf1 fn-not]]))

(defn contains?
  ([xs] (boolean (seq xs)))
  ([xs k] (core/contains? xs k)))

;; ===== SOCIATIVE ===== ;;

(defn get
  ([  k]      (fn [x] (core/get x k)))
  ([x k]      (core/get x k))
  ([x k else] (core/get x k else)))

;; ----- UPDATE ----- ;;

(defn update-first [x f] (cons (f (first x)) (rest x)))

(defn update-val [[k v] f] [k (f v)])

;; ----- *SOC ----- ;;

(defn dissoc-if
  "Like `dissoc`, but only dissociates when condition is true."
  {:contributors '#{alexandergunnarson}}
  ([m pred k] (if (pred m k) (dissoc m k) m)))

;; ----- *SOC-IN ----- ;;

(defn assoc-in
  "Like `assoc-in`, but allows multiple k-v pair arguments like `assoc`."
  ([  ks v] (fn [x] (core/assoc-in x ks v)))
  ([x ks v] (core/assoc-in x ks v))
  ([x ks v & ks-vs]
    (reduce (fn [x' [ks' v']] (assoc-in x' ks' v'))
            (assoc-in x ks v)
            (partition-all 2 ks-vs))))

(defn dissoc-in
  "Dissociate a value in a nested assocative structure, identified by a sequence
  of keys. Any collections left empty by the operation will be dissociated from
  their containing structures.
  This implementation was adapted from clojure.core.contrib"
  {:attribution "weavejester.medley"
   :todo ["Transientize"]}
  [m ks]
  (if-let [[k & ks] (seq ks)]
    (if (empty? ks)
        (dissoc m k)
        (let [new-n (dissoc-in (get m k) ks)] ; this is terrible
          (if (empty? new-n) ; dissoc's empty ones
              (dissoc m k)
              (assoc m k new-n))))
    m))

;; ===== GENERAL ===== ;;

(defn flatten
  ([] core/flatten)
  ([xs] (core/flatten xs))
  ([n xs]
    (if (<= n 0)
        xs
        (recur (dec n) (apply concat xs)))))

(defn frequencies-by
  "Like |frequencies| crossed with |group-by|."
  {:in  '[second [[1 2 3] [4 2 6] [5 2 7]]]
   :out '{[1 2 3] 3, [4 2 6] 3, [5 2 7] 3}}
  [f coll]
  (let [frequencies-0
         (persistent!
           (reduce
             (fn [counts x]
               (let [gotten (f x)
                     freq   (inc (get counts gotten 0))]
                 (assoc! counts gotten freq)))
             (transient {}) coll))
        frequencies-f
          (persistent!
            (reduce
              (fn [ret elem] (assoc! ret elem (get frequencies-0 (f elem))))
              (transient {}) coll))]
    frequencies-f))

(def lmap map)
(def lmap-indexed map-indexed)
(defn lindexed [xs] (lmap-indexed vector xs))

(def lfilter filter)

(defn lflatten-1 [xs] (apply concat xs))

;; ===== ZIPPER ===== ;;

(defn default-zipper [coll]
  (zip/zipper coll? seq (fn [_ c] c) coll))

;; ===== MISCELLANEOUS ===== ;;

(defn merge-call
  "Useful when e.g. there's a long series of functions which return their
   results to an aggregated result."
  {:example `(-> {}
                 (merge-call #(assoc % :a 1))
                 (merge-call my-associng-fn)
                 (merge-call fn-that-uses-the-previous-results))}
  ([m f] (merge m (f m)))
  ([m f & fs] (reduce merge-call (merge-call m f) fs)))

;; ===== COERCION ===== ;;

(def ensure-set
  (condf1
    nil?          (fn' #{})
    (fn-not set?) hash-set
    identity))
