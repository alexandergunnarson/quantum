(ns quantum.untyped.core.collections
  "Operations on collections."
       (:refer-clojure :exclude
         [#?(:cljs array?) assoc-in cat conj! contains? count dedupe distinct distinct? drop first
          get group-by filter flatten frequencies key last map map-indexed mapcat partition-all pmap
          remove reverse run! take val zipmap])
       (:require
         [clojure.core                     :as core]
         [fast-zip.core                    :as zip]
#?(:cljs [goog.array                       :as garray])
         [quantum.untyped.core.core        :as ucore
           :refer [sentinel]]
         [quantum.untyped.core.data
           :refer [transient?]]
         [quantum.untyped.core.data
           :refer [val?]]
         [quantum.untyped.core.data.array
           :refer [array?]]
         [quantum.untyped.core.data.map    :as umap]
         [quantum.untyped.core.data.vector :as uvec]
         [quantum.untyped.core.error       :as uerr
           :refer [err!]]
         [quantum.untyped.core.fn          :as ufn
           :refer [<- ntha fn' aritoid]]
         [quantum.untyped.core.logic
           #?(:clj :refer :cljs :refer-macros) [ifs condf1 fn-not]] ; no idea why this is required  currently :/
         [quantum.untyped.core.loops
           :refer [reduce-2]]
         [quantum.untyped.core.reducers    :as ur
           :refer [defeager def-transducer>eager transducer->transformer educe]]))

(ucore/log-this-ns)

(def count core/count)
(def lrange core/range)

(defn ?persistent! [x]
  (if (transient? x) (persistent! x) x))

(def conj!|rf
  (fn ([] (transient []))
      ([x] (persistent! x))
      ([xs x] (core/conj! xs x))))

(defn conj!
  ([] (transient []))
  ([xs] xs)
  ([xs x0] (core/conj! xs x0))
  ([xs x0 x1] (-> xs (conj! x0) (conj! x1))))

(def first|rf (aritoid ufn/fn-nil identity (fn [_ x] (reduced x))))

(defn first [xs]
  (if (ur/transformer? xs)
      (educe first|rf xs)
      (core/first xs)))

(defn reverse [xs] (if (reversible? xs) (rseq xs) (core/reverse xs)))

(defn key [x]
  #?(:clj  (if (instance? java.util.Map$Entry x)
               (.getKey ^java.util.Map$Entry x)
               (first x))
     :cljs (first x)))

(defn val [x]
  #?(:clj  (if (instance? java.util.Map$Entry x)
               (.getValue ^java.util.Map$Entry x)
               (second x))
     :cljs (second x)))

;; ===== SOCIATIVE ===== ;;

(defn get
  ([  k]      (fn [x] (core/get x k)))
  ([x k]      (core/get x k))
  ([x k else] (core/get x k else)))

;; ----- UPDATE ----- ;;

(defn update-first [x f] (cons (f (first x)) (rest x)))

(defn update-val [[k v] f] [k (f v)])

(defn updates
  "For each key-function pair in @kfs,
   updates value in an associative data structure @coll associated with key
   by applying the function @f to the existing value."
  ([coll & kfs]
    (ur/reduce-pair update coll kfs))) ; TODO This is inefficient

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

(declare partition-all+)

(defn assoc-in
  "Like `assoc-in`, but allows multiple k-v pair arguments like `assoc`."
  ([  ks v] (fn [x] (core/assoc-in x ks v)))
  ([x ks v] (core/assoc-in x ks v))
  ([x ks v & ks-vs]
    (->> ks-vs
         (partition-all+ 2)
         (educe
           (aritoid nil identity (fn [x' [ks' v']] (assoc-in x' ks' v')))
           (assoc-in x ks v)))))

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

(defn select
  ([x k] {k (get x k)})
  ([x k & ks]
    (reduce
      (fn [ret k'] (assoc ret k' (get x k')))
      (select x k) ks)))

(defn select-in
  ([x ks] (assoc-in {} ks (get-in x ks)))
  ([x ks & kss]
    (reduce
      (fn [ret ks'] (assoc-in ret ks' (get-in x ks')))
      (select-in x ks) kss)))

(defn merge-deep-with
  "Like `merge-with` but merges maps recursively, applying the given fn
  only when there's a non-map at a particular level.

  (merge-deep-with + {:a {:b {:c 1 :d {:x 1 :y 2}} :e 3} :f 4}
                    {:a {:b {:c 2 :d {:z 9} :z 3} :e 100}})
  => {:a {:b {:z 3, :c 3, :d {:z 9, :x 1, :y 2}}, :e 103}, :f 4}"
  {:attribution "clojure.contrib.map-utils via taoensso.encore"}
  [f & maps]
  (apply
    (fn merge* [& maps]
      (if (every? map? maps)
          (apply merge-with merge* maps)
          (apply f maps)))
    maps))

(def merge-deep
  (partial merge-deep-with
    (fn ([x]   (second x))
        ([x y] y))))

(defn merge-at [k m & ms]
  (educe (aritoid nil identity (fn [m' m-next] (update m k merge (get m-next k)))) m ms))

(defn mergev-with
  "Like `merge-with`, but merges elements of successive vectors at the same indices,
   `conj`ing when the element is not present.
   `f`: takes three inputs, `i`, `v0`, `v1`"
  [f & xss]
  (reduce
    (fn [xs' xs]
      (ifs (empty? xs') xs
           (empty? xs ) xs'
           (reduce
             (fn [xs'' ^long i|xs]
               (if (>= i|xs (count xs''))
                   (conj xs'' (get xs i|xs))
                   (let [v|xs'' (get xs'' i|xs)
                         v|xs   (get xs   i|xs)]
                     (if (not= v|xs'' v|xs)
                         (assoc xs'' i|xs (f i|xs v|xs'' v|xs))
                         xs''))))
             xs'
             (lrange (count xs)))))
    []
    xss))

(def mergev (partial mergev-with (fn [i v0 v1] v1)))

(defn zipmap-into [x ks vs]
  (reduce-2 assoc (fn [_ _ _] (throw (ex-info "Seqs' count is not the same"))) x ks vs))

(defn zipmap [ks vs] (zipmap-into {} ks vs))

;; ===== Sequential ==== ;;

(defn lasti
  "Last index of a coll."
  [xs]
  (dec (count xs)))

(defn last
  "Gets the last element of ->`xs` in as short a time as possible.
   In the case of collections that are both counted and indexed, this is sublinear (often `O(1)`).
   Otherwise, resorts to a linear traversal."
  [xs]
  (ifs (or (and (counted? xs) (indexed? xs))
           (string? xs)
           (array? xs))
         (get xs (lasti xs))
       (reversible? xs)
         (-> xs rseq first)
       (core/last xs)))

;; ===== Keyed ==== ;;

(defn contains?
  ([xs] (not (empty? xs)))
  ([xs k] (core/contains? xs k)))

;; ===== ... ==== ;;

(defn index-of
  ([xs elem]
    (cond (and (string? xs) (string? elem))
          (let [i (.indexOf ^String xs ^String elem)] (if (= i -1) nil i))
          :else (uerr/not-supported! `index-of xs))))

(defn containsv?
  ([x elem]
    (cond (string? x)
          (and (val? elem) (index-of x elem))
          :else (uerr/not-supported! `containsv? x))))

;; NOTE: The below functions, built on transducers, inasmuch as they require a 0- or 1-arity
;; reducing function to behave correctly (e.g. `partition-all+`), are unsafe for use with
;; core/reduce. Prefer `educe` instead.

(def-transducer>eager map         core/map         1)
(def-transducer>eager map-indexed core/map-indexed 1)
(def-transducer>eager mapcat      core/mapcat      1)

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

(def-transducer>eager filter core/filter 1)
(def-transducer>eager remove core/remove 1)

(defn- pred-keys [f-xs] (fn [pred xs] (->> xs (f-xs (comp pred key)))))
(def      filter-keys+ (pred-keys filter+))
(defeager filter-keys  filter-keys+ 1)
(def      remove-keys+ (pred-keys remove+))
(defeager remove-keys  remove-keys+ 1)

(defn- pred-vals [f-xs] (fn [pred xs] (->> xs (f-xs (comp pred val)))))
(def      filter-vals+ (pred-vals filter+))
(defeager filter-vals  filter-vals+ 1)
(def      remove-vals+ (pred-vals remove+))
(defeager remove-vals  remove-vals+ 1)

(defn keys+ [xs] (->> xs (map+ key)))
(defn vals+ [xs] (->> xs (map+ val)))

(defn indexed+ [xs] (map-indexed+ vector xs))
(defn lindexed [xs] (lmap-indexed vector xs))
(defeager indexed indexed+ 0)

(def-transducer>eager partition-all core/partition-all 1)
(def-transducer>eager distinct      core/distinct      0)
(def-transducer>eager take          core/take          1)
(def-transducer>eager drop          core/drop          1)

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

;; ===== COERCIVE ===== ;;

(defn >vec [xs] (ur/join xs))

(defn >set [xs] (if (set? xs) xs (ur/join #{} xs)))

(def ensure-set
  (condf1
    nil?          (fn' #{})
    (fn-not set?) hash-set
    identity))

;; ===== GENERAL ===== ;;

(defn cat|transducer
  "Like `clojure.core/cat` but uses `educe` internally."
  []
  (fn [rf]
    (let [rrf (ur/preserving-reduced rf)]
      (fn ([] (rf))
          ([result] (rf result))
          ([result input] (educe rrf result input))))))

(defn lcat [xs] (apply concat xs))

(def-transducer>eager cat cat|transducer 0 lcat)

(defn flatten
  ([] core/flatten)
  ([xs] (core/flatten xs))
  ([n xs]
    (if (<= n 0)
        xs
        (recur (dec n) (lcat xs)))))

(defn frequencies
  "Like `frequencies` but uses `educe` internally"
  [f xs]
  (educe (fn ([] (transient {}))
             ([cts] (persistent! cts))
             ([cts x] (assoc! cts x (inc (get cts x 0)))))
         xs))

(defn frequencies-by
  "Like `frequencies` crossed with `group-by`."
  {:in  '[second [[1 2 3] [4 2 6] [5 2 7]]]
   :out '{[1 2 3] 3, [4 2 6] 3, [5 2 7] 3}}
  [f coll]
  (let [frequencies-0
         (educe
           (aritoid (fn' (transient {})) persistent!
             (fn [counts x]
               (let [gotten (f x)
                     freq   (inc (get counts gotten 0))]
                 (assoc! counts gotten freq))))
           coll)
        frequencies-f
          (educe
            (aritoid (fn' (transient {})) persistent!
              (fn [ret elem] (assoc! ret elem (get frequencies-0 (f elem)))))
             coll)]
    frequencies-f))

(def group-by|rf     (aritoid (fn [] (transient {})) persistent! nil assoc!))
(def group-by|sub-rf (aritoid vector nil conj))

(def group-by|!rf
  (aritoid umap/>!hash-map identity nil
    #?(:clj (fn [^java.util.HashMap ret k v] (doto ret (.put k v))) :cljs assoc!)))

(def group-by|!sub-rf uvec/alist-conj!)

(defn group-by-into
  "Like `group-by`, but uses `educe` internally, and you can choose what collection and
   subcollection to group into."
  ([kf rf        xs] (group-by-into kf rf group-by|sub-rf xs))
  ([kf rf sub-rf xs]
    (educe
      (aritoid rf rf
        (fn [ret x]
          (let [k (kf x), v (get ret k sentinel)]
            (rf ret k (sub-rf (if (identical? v sentinel) (sub-rf) v) x)))))
      xs)))

(defn group-by [kf xs] (group-by-into kf group-by|rf xs))

(defn group-into
  ([rf        xs] (group-by-into identity rf group-by|rf xs))
  ([rf sub-rf xs] (group-by-into identity rf sub-rf      xs)))

(defn group [xs] (group-by-into identity group-by|rf xs))

(defn- group-deep-by-into* [i n kf rf sub-rf xs]
  (if (>= i n)
      xs
      (->> xs
           (group-by-into (fn [x] (kf i x)) group-by|!rf group-by|!sub-rf)
           (map-vals+ (fn [sub-xs] (group-deep-by-into* (inc i) n kf rf sub-rf sub-xs)))
           (educe (aritoid rf rf (fn [ret [k v]] (rf ret k v)))))))

(defn group-deep-by-into
  "Like `group-by-into` but:
   - Expects a reducible of reducibles
   - Performs up to N groupings, defaulting to the max size of the inner reducibles
   - `kf` takes two inputs: `depth` and `x`.

   E.g. `(group-deep-by (fn [i x] (get x i)) [[1 4] [3 2] [1 2] [3 2 5]])`
     -> `{1 {2 {nil [[1 2]]}
             4 {nil [[1 4]]}}
          3 {2 {nil [[3 2]]
                5   [[3 2 5]]}}}`"
  ([          kf rf        xs] (group-deep-by-into kf rf group-by|sub-rf xs))
  ([          kf rf sub-rf xs]
    (group-deep-by-into (->> xs (map+ count) (educe max 0)) kf rf sub-rf xs))
  ([n #_(> 0) kf rf sub-rf xs] (group-deep-by-into* 0 n kf rf sub-rf xs)))

(defn group-deep-by
  ([  kf xs] (group-deep-by-into   kf group-by|rf group-by|sub-rf xs))
  ([n kf xs] (group-deep-by-into n kf group-by|rf group-by|sub-rf xs)))

(defn group-deep-into
  ([  rf        xs] (group-deep-by-into   (fn [i x] x) rf group-by|sub-rf xs))
  ([  rf sub-rf xs] (group-deep-by-into   (fn [i x] x) rf sub-rf          xs))
  ([n rf sub-rf xs] (group-deep-by-into n (fn [i x] x) rf sub-rf          xs)))

(defn group-deep
  ([  xs] (group-deep-by-into   (fn [i x] x) group-by|rf group-by|sub-rf xs))
  ([n xs] (group-deep-by-into n (fn [i x] x) group-by|rf group-by|sub-rf xs)))

(defn lcat [xs] (apply concat xs))

(defn run!
  "Like `core/run!` but uses `educe` internally."
  [f xs] (->> xs (educe (fn ([] nil) ([ret] ret) ([ret x] (f x))))))

(defn distinct?
  "Like `clojure.core/distinct?` except operates on reducibles."
  [xs]
  (->> xs
       (educe (aritoid (fn' (transient #{})) ?persistent!
                (fn [distincts x]
                  (if (contains? distincts x)
                      (reduced false)
                      (conj! distincts x)))))
       boolean))

(defn dedupe-by|tf
  "Like `dedupe`'s transducer but is able to dedupe by comparing two inputs by `eq-f` rather than
   only `=`."
  [eq-f]
  (fn [rf]
    (let [pv (volatile! ::none)]
      (fn ([] (rf))
          ([result] (rf result))
          ([result input]
             (let [prior @pv]
               (vreset! pv input)
               (if (and (not (identical? prior ::none))
                        (eq-f prior input))
                   result
                   (rf result input))))))))

(def-transducer>eager dedupe-by dedupe-by|tf 1)

(def dedupe|tf
  (let [gen-rf (dedupe-by|tf =)]
    (fn [] gen-rf)))

(def-transducer>eager dedupe dedupe|tf 0)

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
  ([m] m)
  ([m f] (merge m (f m)))
  ([m f & fs] (educe merge-call (merge-call m f) fs)))

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

(defn seq=
  ([a b] (seq= a b =))
  ([a b eq-f]
    (boolean
      (loop [a (seq a) b (seq b)]
        (let [a-nil? (nil? a)]
          (and (identical? a-nil? (nil? b))
               (or a-nil?
                   (and (eq-f (first a) (first b))
                        (recur (next a) (next b))))))))))

(defn >combinatoric-tree
  "See tests for examples.

   Assumes all are sorted, grouped, and of the same count."
  {:todo #{"Generalize to handle uneven input lengths and unsorted combination"}}
  ([n #_pos-int?, xs #_(t/of (t/tuple (t/spec t/any? "identifier") (t/of)))]
    (>combinatoric-tree
      n = conj conj (fn ([] []) ([ret] ret) ([ret [k [x*]]] (conj ret [x* k]))) xs))
  ([n #_pos-int?, eq-f groupsf groupf terminalf xs]
    (if (<= n 1)
        (educe terminalf xs)
        (let [terminate-group
                (fn [grouped curr-group curr-x*]
                  (groupsf grouped
                    [curr-x*
                     (>combinatoric-tree
                       (dec n) eq-f groupsf groupf terminalf (groupf curr-group))]))]
          (educe
            (fn ([] [(groupsf) (groupf) sentinel])
                ([[grouped curr-group curr-x*]]
                  (groupsf (terminate-group grouped curr-group curr-x*)))
                ([[grouped curr-group curr-x*] [k [x* & xs*]]]
                  (ifs (identical? curr-x* sentinel) [grouped (groupf curr-group [k xs*]) x*]
                       (eq-f       curr-x* x*)       [grouped (groupf curr-group [k xs*]) curr-x*]
                       [(terminate-group grouped curr-group curr-x*)
                        (groupf (groupf) [k xs*])
                        x*])))
            xs)))))

(defn aswap!
  [#?(:clj ^"[Ljava.lang.Object;" !xs :cljs !xs)
   #?(:clj ^long i :cljs ^number i)
   #?(:clj ^long j :cljs ^number j)]
   (let [tmp (aget !xs i)]
     (doto !xs (aset i (aget !xs j))
               (aset j tmp))))

(defn shuffle!
  "Uses the Fisher–Yates shuffle as enhanced by Durstenfeld."
  [#?(:clj ^"[Ljava.lang.Object;" !xs :cljs !xs)]
  (let [r #?(:clj (java.util.concurrent.ThreadLocalRandom/current) :cljs nil)]
    (loop [i (-> !xs alength unchecked-dec)]
      (if (> i 0)
          (do (aswap! !xs i (#?@(:clj [.nextInt r] :cljs rand-int) (unchecked-inc i)))
              (recur (unchecked-dec i)))
          !xs))))

(defn sort|insertion!
  {:adapted-from "https://en.wikipedia.org/wiki/Insertion_sort"}
  ([#?(:clj ^"[Ljava.lang.Object;" !xs :cljs !xs)] (sort|insertion! compare !xs))
  ([compf #?(:clj ^"[Ljava.lang.Object;" !xs :cljs !xs)]
    (let [ct (alength !xs)]
      (loop [i 1]
        (if (< i ct)
            (let [x (aget !xs i)]
              (loop [j (unchecked-dec i)]
                (if (and (>= j 0) (pos? (int (compf (aget !xs j) x))))
                    (do (aset !xs (unchecked-inc j) (aget !xs j))
                        (recur (unchecked-dec j)))
                    (aset !xs (unchecked-inc j) x)))
              (recur (unchecked-inc i)))
            !xs)))))

(defn sort-by|insertion!
  ([kf !xs] (sort-by|insertion! kf compare !xs))
  ([kf compf !xs] (sort|insertion! (fn [a b] (compf (kf a) (kf b))) !xs)))

(defn sort!
  "Like `sort` but coerces `xs` to an array and then sorts it in place, returning the coerced array
   instead of a seq on top of it. If `xs` is already an array, modifies `xs`."
  ([xs] (sort! compare !xs))
  ([compf xs]
    (let [#?(:clj ^objects !xs :cljs !xs) (if (array? xs) xs (to-array xs))]
      (doto !xs #?(:clj  (java.util.Arrays/sort ^Comparator compf)
                   :cljs (garray/stableSort !xs (@#'fn->comparator compf)))))))

(defn sort-by!
  "Like `sort-by` but coerces `xs` to an array and then sorts it in place, returning the coerced
   array instead of a seq on top of it. If `xs` is already an array, modifies `xs`."
  ([kf xs] (sort-by! kf compare xs))
  ([kf compf xs] (sort! (fn [a b] (compf (kf a) (kf b))) xs)))
