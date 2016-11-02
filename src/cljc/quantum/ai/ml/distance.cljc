(ns quantum.ai.ml.distance
  "Calculates distances and norms."
  (:refer-clojure :exclude [assert get])
  (:require
    [quantum.core.logic
      :refer        [#?@(:clj [fn-or])]
      :refer-macros [          fn-or]]
    [quantum.core.fn
      :refer        [#?@(:clj [<- fn1 fn->])]
      :refer-macros [          <- fn1 fn->]]
    [quantum.core.collections :as coll
      :refer [map+ remove+ red-apply range+
              mutable! eq! aset-in!
              #?@(:clj [kmap aget-in aget-in*
                        ifor get reducei])]
      :refer-macros [   kmap aget-in aget-in*
                        ifor get reducei]]
    [quantum.core.error
      :refer        [ ->ex TODO #?(:clj assert)]
      :refer-macros [assert]]
    [quantum.core.numeric :as cnum
      :refer        [#?@(:clj [+* inc* pow abs sqrt])]
      :refer-macros [          +* inc* pow abs sqrt]]
    [quantum.numeric.core :as num
      :refer [sum sq]]
    [quantum.numeric.vectors :as v]
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.string :as str]))

(defn levenshtein-matrix
  {:tests '{["kitten" "sitting"]
            [[0 1 2 3 4 5 6 7]
             [1 1 2 3 4 5 6 7]
             [2 2 1 2 3 4 5 6]
             [3 3 2 1 2 3 4 5]
             [4 4 3 2 1 2 3 4]
             [5 5 4 3 2 2 3 4]
             [6 6 5 4 3 3 2 3]]}
   :performance
      ["Boxed math doesn't seem to
        make a difference in performance"
       "|ifor| shaves about 20-40% off the time compared to
        |doseq| with |range|! pretty amazing"]
    :todo ["Move from |aset-in!| to |aset-in!*|"
           "Eliminate boxed math"
           "Allow for n-dimensional, and weighted Levenshtein"
           "Improve |coll/->multi-array|"]}
  [s1 s2]
  (let [s1-ct+1 (-> s1 count int inc*)
        s2-ct+1 (-> s2 count int inc*)
        ^"[[I" m    (coll/->multi-array (int 0)
                      [(-> s1 count inc)
                       (-> s2 count inc)])
        cost (mutable! 0)]
    (ifor [i 0 (< i s1-ct+1) (inc* i)]
      (aset-in! m [i 0] i))
    (ifor [j 0 (< j s2-ct+1) (inc* j)]
      (aset-in! m [0 j] j))
    (ifor [i 1 (< i s1-ct+1) (inc* i)]
      (ifor [j 1 (< j s2-ct+1) (inc* j)]
        (if (= (get s1 (dec i))
               (get s2 (dec j)))
            (eq! cost 0)
            (eq! cost 1))
        (aset-in! m [i j]
          (min (inc     (aget-in* m (dec i) j      ))     ; deletion
               (inc     (aget-in* m i       (dec j)))     ; insertion
               (+ @cost (aget-in* m (dec i) (dec j))))))) ; substitution
     m))

(defn levenshtein [str1 str2]
  {:modified-by {"Alex Gunnarson"
                 ["removed boxed math"
                  "|nth| -> |get|"
                  "removed unnecessary |persistent!| call"]}
   :original-source "https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Clojure"}
  (let [str1 (name str1)
        str2 (name str2)
        n (-> str1 count int)
        m (-> str2 count int)]
    (cond
     (= 0 n) m
     (= 0 m) n
     :else
     (let [prev-col (transient (vec (range (inc* m))))
           col      (transient [])] ; initialization for the first column.
       (dotimes [i n]
         (let [i (int i)]
           (assoc! col 0 (inc* i)) ; update col[0]
           (dotimes [j m]
             (let [j (int j)]
               (assoc! col (inc* j)  ; update col[1..m]
               (min (inc* (int (get col      j       )))
                    (inc* (int (get prev-col (inc* j))))
                    (+*   (int (get prev-col j))
                          (if (= (get str1 i)
                                 (get str2 j))
                              0 1))))))
           (dotimes [i (count prev-col)]
             (assoc! prev-col i (get col i))))) ;
       (last col))))) ; last element of last column

(defn l-p
  "[v p]
   The L-p norm of a vector.

   [a b p]
   The L-p distance between two n-dimensional vectors."
  ([v p]
    (pow (sum (map+ (fn-> abs (pow p)) v)) (/ p)))
  ([a b p] (TODO)))

(defn l-inf
  "[v]
   The (L∞|Chebyshev|Lp when p -> ∞|max|max of abs) norm of a vector.

   [a b]
   The (L∞|Chebyshev|Lp when p -> ∞|max|max of abs) distance between two n-dimensional
   vectors."
  ([v] (->> v (map+ (fn1 abs)) (red-apply max)))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.ChebyshevDistance}}
  ([a b] (TODO)))

(defalias chebyshev l-inf)

(defn l-1
  "[v]
   The (L1|Manhattan|sum of abs) norm of a vector.

   [a b]
   The (L1|Manhattan|sum of abs) distance between two n-dimensional vectors."
  ([v] (->> v (map+ (fn1 abs)) sum))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.ManhattanDistance}}
  ([a b] (TODO)))

(defalias manhattan l-1)

(defn l-2
  "[v]
   The (L-2|Euclidean) norm of a vector.

   [a b]
   The (L-2|Euclidean) distance between two n-dimensional vectors."
  ([v] (->> v (map+ sq) sum sqrt))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.EuclideanDistance}}
  ([a b] (TODO)))

(defalias euclidean l-2)
(defalias vlength   l-2)
#_(defalias dist      l-2) ; TODO this is fine

(defn cosine-similarity [a b]
  (/ (v/dot a b)
     (* (l-2 a) (l-2 b))))

(defn dist* [v1 v2] ; TODO I assume this is L-2 between n-dimensional vectors
  (assert (= (count v1) (count v2)))
  (->> (range+ 0 (count v1))
       (map+ #(- (get v1 %) (get v2 %)))
       (map+ sq)
       num/sum))

(defn dist [v1 v2] (sqrt (dist* v1 v2)))

(defn canberra
  "Calculates the Canberra distance between two n-dimensional vectors."
  {:implemented-by '#{org.apache.commons.math3.ml.distance.CanberraDistance}}
  [a b] (TODO))

(defn earth-movers
  "Calculates the Earh Mover's distance (also known as
   Wasserstein metric) between two distributions."
  {:implemented-by '#{org.apache.commons.math3.ml.distance.EarthMoversDistance}}
  [a b] (TODO))

(defalias wasserstein-metric earth-movers)
