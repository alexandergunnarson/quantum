(ns
  ^{:doc "Base collections operations. Pre-generics."
    :attribution "Alex Gunnarson"}
  quantum.core.collections.base
           (:refer-clojure :exclude [name])
           (:require [fast-zip.core              :as zip  ]
                     [clojure.string             :as str  ]
                     [clojure.walk
                       :refer [postwalk prewalk]          ]
                     [#?(:clj  clojure.core
                         :cljs cljs.core   )     :as core ]
                     [quantum.core.fn            :as fn
                       :refer [#?@(:clj [fn->])]          ]
                     [quantum.core.logic         :as logic
                       :refer [#?@(:clj [condf*n fn-not])]])
  #?(:cljs (:require-macros
                     [quantum.core.fn            :as fn
                       :refer [fn->]                      ]
                     [quantum.core.logic         :as logic
                       :refer [condf*n fn-not]            ])))

(defn name [x] (if (nil? x) "" (core/name x)))

(defn default-zipper [coll]
  (zip/zipper coll? seq (fn [_ c] c) coll))

(def ensure-set
  (condf*n
    nil?
      (constantly #{})
    (fn-not set?)
      hash-set
    :else identity))

(defn zip-reduce [f init z]
  (loop [z (zip/down z)
         ret-n init]
    (if (nil? z)
        ret-n
        (recur (zip/right z) (f ret-n z)))))

(defn camelcase
  "In the macro namespace because it is used with protocol creation."
  ^{:attribution  "flatland.useful.string"
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

#?(:clj
(defmacro kmap [& ks]
 `(zipmap (map keyword (quote ~ks)) (list ~@ks))))

(defn appears-within?
  "Returns true if x appears within coll at any nesting depth.."
  {:source "scgilardi/slingshot"
   :contributors {"Alex Gunnarson" "Added termination on find"}}
  [x coll]
  (let [result (atom false)]
    (try
      (clojure.walk/postwalk
        (fn [t]
          (when (= x t)
            (reset! result true)
            (throw #?(:clj (Exception.) :cljs (js/Error.)))))
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
