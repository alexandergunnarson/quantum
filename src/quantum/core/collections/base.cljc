(ns
  ^{:doc "Base collections operations. Pre-generics."
    :attribution "alexandergunnarson"}
  quantum.core.collections.base
  (:refer-clojure :exclude [name])
  (:require
    [fast-zip.core              :as zip]
    [clojure.string             :as str]
    [clojure.core               :as core]
    [quantum.core.fn            :as fn
      :refer [fn-> fn']]
    [quantum.core.core          :as qcore]
    [quantum.core.logic         :as logic
      :refer [condf1 fn-not]]
    [quantum.core.vars             :as var
      :refer [replace-meta-from]])
#?(:cljs
  (:require-macros
    [quantum.core.collections.base :as self])))

(defn name [x] (if (nil? x) "" (core/name x)))

(def nnil?   core/some?)
(def nempty? (comp not empty?)) ; TODO fix this performance-wise

(defn default-zipper [coll]
  (zip/zipper coll? seq (fn [_ c] c) coll))

(def ensure-set
  (condf1
    nil?
      (fn' #{})
    (fn-not set?)
      hash-set
    identity))

(defn zip-reduce* [f init z]
  (loop [xs (zip/down z) v init]
    (if (nil? xs)
        v
        (let [ret (f v xs)]
          (if (reduced? ret)
              @ret
              (recur (zip/right xs) ret))))))

(defn reducei [f init coll]
  (let [i (volatile! (long -1))]
    (reduce
      (fn ([ret elem]
            (vswap! i inc)
            (f ret elem @i))
          ([ret k v]
            (vswap! i inc)
            (f ret k v @i)))
      init
      coll)))

(defn reduce-pair
  "Like |reduce|, but reduces over two items in a collection at a time.

   Its function @func must take three arguments:
   1) The accumulated return value of the reduction function
   2) The                next item in the collection being reduced over
   3) The item after the next item in the collection being reduced over

   Doesn't use `reduce`... so not as fast."
  {:todo        ["Possibly find a better way to do it?"]
   :attribution "alexandergunnarson"}
  [func init coll]
  (loop [ret init coll-n coll]
    (if (empty? coll-n)
        ret
        (recur (func ret (first coll-n) (second coll-n))
               (-> coll-n rest rest)))))

(defn merge-call
  "Useful when e.g. there's a long series of functions which return their
   results to an aggregated result."
  {:example `(-> {}
                 (merge-call #(assoc % :a 1))
                 (merge-call my-associng-fn)
                 (merge-call fn-that-uses-the-previous-results))}
  ([m f] (merge m (f m)))
  ([m f & fs] (reduce merge-call (merge-call m f) fs)))

(defn camelcase
  "In the macro namespace because it is used with protocol creation."
  {:attribution  "flatland.useful.string"
   :contributors "Alex Gunnarson"}
  [str-0 & [method?]]
  (-> str-0
      (str/replace #"[-_](\w)"
        (fn-> second str/upper-case))
      (#(if (not method?)
           (apply str (-> % first str/upper-case) (rest %))
           %))))

(defn ns-qualify [sym ns-]
  (symbol (str (name ns-) "." (name sym))))

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

(def comparators
  {#?@(:clj
        [Class (fn [^Class a ^Class b]
                 (.compareTo (.getName a) (.getName b)))])})

(defn update-first [x f] (cons (f (first x)) (rest x)))

(defn update-val [[k v] f]
  [k (f v)])

#?(:clj (defmacro kw-map    [& ks] (qcore/quote-map-base `hash-map (comp keyword str) ks)))
#?(:clj (defmacro quote-map [& ks] (qcore/quote-map-base `hash-map identity           ks)))

; ----- WALK ----- ;

(defn walk
  "Like `clojure.walk`, but ensures preservation of metadata."
  [inner outer form]
  (cond
              (list?      form) (outer (replace-meta-from (apply list (map inner form))                    form))
    #?@(:clj [(map-entry? form) (outer (replace-meta-from (vec        (map inner form))                    form))])
              (seq?       form) (outer (replace-meta-from (doall      (map inner form))                    form))
              (record?    form) (outer (replace-meta-from (reduce (fn [r x] (conj r (inner x))) form form) form))
              (coll?      form) (outer (replace-meta-from (into (empty form) (map inner form))             form))
              :else (outer form)))

(defn postwalk [f form] (walk (partial postwalk f) f form))
(defn prewalk  [f form] (walk (partial prewalk  f) identity (f form)))

; ----- COLLECTIONS ----- ;

(defn prewalk-find
  "Returns true if ->`x` appears within ->`coll` at any nesting depth."
  {:adapted-from "scgilardi/slingshot"
   :contributors ["Alex Gunnarson"]}
  [pred coll]
  (let [result (atom [false nil])]
    (try
      (prewalk
        (fn [x]
          (if (pred x) ; TODO fix — if there's an exception then this will misleadingly say it's not found instead of propagating the exception
              (do (reset! result [true x])
                  (throw #?(:clj (Exception.) :cljs (js/Error.))))
              x))
        coll)
      @result
      (catch #?(:clj Exception :cljs js/Error) _ @result))))

; TODO DELETE AFTER INCORPORATING REAL COLLECTIONS
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

