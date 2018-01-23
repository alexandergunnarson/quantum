(ns quantum.untyped.core.collections
  (:refer-clojure :exclude
    [#?(:cljs array?) assoc-in contains? distinct? get filter flatten last map map-indexed
     mapcat pmap remove vec])
  (:require
    [clojure.core                  :as core]
    [fast-zip.core                 :as zip]
    [quantum.untyped.core.error    :as uerr
      :refer [err!]]
    [quantum.untyped.core.fn       :as ufn
      :refer [fn']]
    [quantum.untyped.core.logic
      :refer [condf1 fn-not]]
    [quantum.untyped.core.reducers :as ur
      :refer [defeager transducer->transformer]]))

;; ===== SOCIATIVE ===== ;;

(defn get
  ([  k]      (fn [x] (core/get x k)))
  ([x k]      (core/get x k))
  ([x k else] (core/get x k else)))

;; ----- UPDATE ----- ;;

(defn update-first [x f] (cons (f (first x)) (rest x)))

(defn update-val [[k v] f] [k (f v)])

;; ----- *SOC ----- ;;

(defn dissoc*
  "Like `dissoc`, but returns `nil` when `dissoc` has caused the map to become empty."
  [m k]
  (let [m' (dissoc m k)]
    (when-not (empty? m')
      m')))

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


;; TODO move to type predicates
(defn array? [x]
  #?(:clj  (-> x class .isArray) ; must be reflective
     :cljs (core/array? x)))

;; ===== Sequential ==== ;;

(defn lasti
  "Last index of a coll."
  [xs]
  (dec (count xs)))

(defn last [xs]
  (if (or (and (counted? xs) (indexed? xs))
          (string? xs)
          (array? xs))
      (get xs (lasti xs))
      (core/last xs)))

;; ===== Keyed ==== ;;

(defn contains?
  ([xs] (boolean (seq xs)))
  ([xs k] (core/contains? xs k)))

;; ===== ... ==== ;;

(defn subview
  "Returns a subview of ->`xs`, [->`a` to ->`b`), in O(1) time."
  ([xs ^long a] (subview xs a (count xs)))
  ([xs ^long a ^long b]
    (cond           (vector? xs) (subvec xs a b)
          #?@(:clj [(string? xs) (.subSequence ^String xs a b)])
                    :else        (uerr/not-supported! `subview xs))))

(defn slice
  "Makes a subcopy of ->`x`, [->`a`, ->`b`), in the most efficient way possible.
   Differs from `subview` in that it does not simply return a view in O(1) time.
   Some copies are more efficient than others — some might be O(N); others O(log(N))."
  ([xs ^long a] (slice xs a (count xs)))
  ([xs ^long a ^long b]
    (if (string? xs)
        (.substring ^String xs a b)
        (->> xs (drop a) (take b)))))

(defn subview-or-slice
  ([xs a] (subview-or-slice xs a (count xs)))
  ([xs a b]
    (if (or (vector? xs) (string? xs)) ; `subviewable?`
        (subview xs a b)
        (slice a b))))

(def mapcat+ (transducer->transformer 1 core/mapcat))
(defeager mapcat mapcat+)

(def map+ (transducer->transformer 1 core/map))
(defeager map map+)
(def lmap map)

(def map-indexed+ (transducer->transformer 1 core/map-indexed))
(defeager map-indexed map-indexed+)
(def lmap-indexed map-indexed)

(def indexed+ #(->> % (map-indexed+ vector)))
(defn lindexed [xs] (lmap-indexed vector xs))

(defn- map-keys* [f-xs] (fn [f xs] (->> xs (f-xs (juxt (comp f key) val)))))
(def  map-keys+ (map-keys* map+))
(def  map-keys' (map-keys* map'))
(def  map-keys  (map-keys* map ))
(def  lmap-keys (map-keys* lmap))

(defn- map-vals* [f-xs] (fn [f xs] (->> xs (f-xs (juxt key (comp f val))))))
(def  map-vals+ (map-vals* map+))
(def  map-vals' (map-vals* map'))
(def  map-vals  (map-vals* map ))
(def  lmap-vals (map-vals* lmap))

(def filter+ (transducer->transformer 1 core/filter))
(defeager filter filter+)
(def lfilter core/filter)

(def remove+ (transducer->transformer 1 core/remove))
(defeager remove remove+)
(def lremove core/remove)

(defn- pred-keys [f-xs] (fn [pred xs] (->> xs (f-xs (comp pred key)))))
(def      filter-keys+ (pred-keys filter+))
(defeager filter-keys  filter-keys+)
(def      remove-keys+ (pred-keys remove+))
(defeager remove-keys  remove-keys+)

(defn- pred-vals [f-xs] (fn [pred xs] (->> xs (f-xs (comp pred val)))))
(def      filter-vals+ (pred-vals filter+))
(defeager filter-vals  filter-vals+)
(def      remove-vals+ (pred-vals remove+))
(defeager remove-vals  remove-vals+)

(def partition-all+ (transducer->transformer 1 core/partition-all))

(def distinct+      (transducer->transformer 0 core/distinct))

;; ===== COERCIVE ===== ;;

(defn vec [xs] (ur/join xs))

(def ensure-set
  (condf1
    nil?          (fn' #{})
    (fn-not set?) hash-set
    identity))

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


(defn lflatten-1 [xs] (apply concat xs))

(defn distinct?
  "Like `clojure.core/distinct?` except operates on reducibles."
  [xs]
  (boolean
    (reduce (fn [distincts x]
              (if (contains? distincts x)
                  (reduced false)
                  (conj distincts x)))
            #{}
            xs)))

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

(defn unchunk
  "Given a sequence that may have chunks, return a sequence that is 1-at-a-time
   lazy with no chunks. Chunks are good for efficiency when the data items are
   small, but when being processed via map, for example, a reference is kept to
   every function result in the chunk until the entire chunk has been processed,
   which increases the amount of memory in use that cannot be garbage
   collected."
  {:author "Mark Engelberg"
   :from "clojure.math.combinatorics"}
  [s]
  (lazy-seq
    (when (seq s)
      (cons (first s) (unchunk (rest s))))))
