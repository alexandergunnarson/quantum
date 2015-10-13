(ns
  ^{:doc "Base collections operations. Pre-generics."
    :attribution "Alex Gunnarson"}
  quantum.core.collections.base
  (:refer-clojure :exclude [name])
  (:require-quantum [ns log pr err map set vec logic fn ftree])
  (:require
            [quantum.core.type.core     :as tcore]
            [fast-zip.core              :as zip  ]
            [clojure.string             :as str  ]
            [clojure.walk :refer [postwalk prewalk]]
    #?(:clj [clojure.math.combinatorics :as combo])))

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
        (compr second str/upper-case))
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