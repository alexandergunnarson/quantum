(ns quantum.ai.ml.similarity
  "Calculates similarity measures: distances, norms,
   and so on.
   A distance function maps pairs of points into the
   nonnegative reals and has to satisfy:
   - non-negativity: d(x, y) > 0
   - isolation:      d(x, y) = 0 iff x = y
   - symmetry:       d(x, y) = d(x, y)"
  (:refer-clojure :exclude [get])
  (:require
    [quantum.core.logic
      :refer [fn-or]]
    [quantum.core.fn
      :refer [<- fn1 fn->]]
    [quantum.core.collections :as coll
      :refer [map+ remove+ red-apply range+
              mutable setm! assoc-in!
              kw-map get-in* ifor get reducei]]
    [quantum.core.error
      :refer [->ex TODO]]
    [quantum.core.numeric :as cnum
      :refer [+* inc* pow abs sqrt]]
    [quantum.numeric.core :as num
      :refer [sum sq]]
    [quantum.numeric.tensors :as tens]
    [quantum.core.vars
      :refer [defalias]]
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
    :todo ["Move from |assoc-in!| to |assoc-in!*|"
           "Eliminate boxed math"
           "Allow for n-dimensional, and weighted Levenshtein"
           "Improve |coll/->multi-array|"
           "Incorporate lightweight stuff from gene sequencing"]}
  [s1 s2]
  (let [s1-ct+1 (-> s1 count int inc*)
        s2-ct+1 (-> s2 count int inc*)
        ^"[[I" m    (coll/->multi-array (int 0)
                      [(-> s1 count inc)
                       (-> s2 count inc)])
        cost (mutable 0)]
    (ifor [i 0 (< i s1-ct+1) (inc* i)]
      (assoc-in! m [i 0] i))
    (ifor [j 0 (< j s2-ct+1) (inc* j)]
      (assoc-in! m [0 j] j))
    (ifor [i 1 (< i s1-ct+1) (inc* i)]
      (ifor [j 1 (< j s2-ct+1) (inc* j)]
        (if (= (get s1 (dec i))
               (get s2 (dec j)))
            (setm! cost 0)
            (setm! cost 1))
        (assoc-in! m [i j]
          (min (inc     (get-in* m (dec i) j      ))     ; deletion
               (inc     (get-in* m i       (dec j)))     ; insertion
               (+ @cost (get-in* m (dec i) (dec j))))))) ; substitution
     m))


(defn edit [str1 str2]
  {:modified-by {"alexandergunnarson"
                 ["removed boxed math"
                  "|nth| -> |get|"
                  "removed unnecessary |persistent!| call"]}
   :original-source "https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Clojure"
   :todo #{"A generalization of the Levenshtein
            distance (Damerau-Levenshtein distance) allows the transposition of two
            chars."}
   :implemented-by '{smile.math.distance.EditDistance "faster array impl, and has more functionality"}}
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

(defalias levenshtein edit)

(defn l-p
  "[v p]
   The L-p norm of a vector.

   [a b p]
   The L-p distance between two n-dimensional vectors."
  {:implemented-by '{smile.math.distance.MinkowskiDistance "faster array implementation"
                     smile.math.distance.SparseMinkowskiDistance "for sparse arrays"}}
  ([v p]
    (pow (sum (map+ (fn-> abs (pow p)) v)) (/ p)))
  ([a b p] (TODO)))

(defalias minkowski l-p)

(defn l-inf
  "[v]
   The (L∞|Chebyshev|Lp when p -> ∞|max|max of abs) norm of a vector.

   [a b]
   The (L∞|Chebyshev|Lp when p -> ∞|max|max of abs) distance between two n-dimensional
   vectors."
  {:implemented-by '{smile.math.distance.ChebyshevDistance       "faster array implementation"
                     smile.math.distance.SparseChebyshevDistance "for sparse arrays"}}
  ([v] (->> v (map+ (fn1 abs)) (red-apply max)))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.ChebyshevDistance}}
  ([a b] (TODO)))

(defalias chebyshev l-inf)

(defn l-1
  "[v]
   The (L1|Manhattan|sum of abs) norm of a vector.

   [a b]
   The (L1|Manhattan|sum of abs) distance between two n-dimensional vectors.
   Called `Manhattan distance` based on the gridlike street geography of Manhattan."
  {:implemented-by '{smile.math.distance.ManhattanDistance "faster array implementation"
                     smile.math.distance.SparseManhattanDistance "for sparse arrays"}}
  ([v] (->> v (map+ (fn1 abs)) sum))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.ManhattanDistance}}
  ([a b] (sum (tens/v-op+ #(abs (- %1 %2)) a b))))

(defalias manhattan l-1)

(defn l-2
  "[v]
   The (L-2|Euclidean) norm of a vector.

   [a b]
   The (L-2|Euclidean) distance between two n-dimensional vectors."
  {:implemented-by '{smile.math.distance.EuclideanDistance "faster array implementation"
                     smile.math.distance.SparseEuclideanDistance "for sparse arrays"
                     smile.math.matrix.SingularValueDecomposition "The largest singular value"}}
  ([v] (->> v (map+ sq) sum sqrt))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.EuclideanDistance}}
  ([a b] (TODO)))

(defalias euclidean l-2)
(defalias vlength   l-2)
#_(defalias dist      l-2) ; TODO this is fine

(defn cosine-similarity [a b]
  (/ (tens/dot a b)
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

(defn hamming
  "Calculates the Hamming distance between two objects.
   Measures the minimum number of substitutions required to change one
   string into the other, or the number of errors that transformed one
   string into the other. For a fixed length n, the Hamming
   distance is a metric on the vector space of the words of that length."
  {:implemented-by '#{smile.math.distance.HammingDistance}}
  [a b] (TODO))

(defn jaccard
  "The Jaccard index, also known as the Jaccard similarity coefficient.
   The Jaccard coefficient measures similarity    between sample sets.
   The Jaccard distance    measures dissimilarity between sample sets."
  {:implemented-by '#{smile.math.distance.JaccardDistance}}
  [a b] (TODO))

(defn jensen-shannon
  "The Jensen-Shannon (distance|divergence) measures the similarity
   between two probability distributions. It is also known as information
   radius or total divergence to the average."
  {:implemented-by '#{smile.math.distance.JensenShannonDistance}}
  [a b] (TODO))

(defn lee
  "Lee distance"
  {:implemented-by '#{smile.math.distance.LeeDistance}}
  [a b] (TODO))

(defn mahalanobis
  "In statistics, Mahalanobis distance is based on correlations between
   variables by which different patterns can be identified and analyzed.
   It is a useful way of determining similarity of an unknown sample set
   to a known one."
  {:implemented-by '#{smile.math.distance.MahalanobisDistance}}
  [a b] (TODO))

(defn earth-movers
  "Calculates the Earh Mover's distance (also known as
   Wasserstein metric) between two distributions."
  {:implemented-by '#{org.apache.commons.math3.ml.distance.EarthMoversDistance}}
  [a b] (TODO))

(defalias wasserstein-metric earth-movers)

; ========== OTHER SIMILARITY MEASURES ========== ;

; ===== MULTIDIMENSIONAL SCALING (MDS) ===== ;
; A set of related statistical techniques often used in information
; visualization for exploring similarities or dissimilarities in data.

(defn classical-mds
  "Classical multidimensional scaling, also known as principal coordinates
   analysis.
   When Euclidean distances are used, MDS is equivalent to PCA.
   Finds a set of points in low dimensional space that well-approximates the
   dissimilarities in A."
  {:implemented-by '#{smile.mds.MDS}}
  [?] (TODO))

(defn non-metric-mds
  "Kruskal's non-metric multidimensional scaling.
   Finds both a non-parametric monotonic relationship between the dissimilarities."
  {:implemented-by '#{smile.mds.IsotonicMDS}}
  [?] (TODO))

(defn sammons-mapping
  "A special case of metric least-square multidimensional scaling.
   An iterative technique for making interpoint distances in the low-dimensional
   projection as close as possible to the interpoint distances in the
   high-dimensional object. Two points close together in the high-dimensional
   space should appear close together in the projection, while two points far
   apart in the high dimensional space should appear far apart in the projection."
  {:implemented-by '#{smile.mds.SammonMapping}}
  [?] (TODO))

; TODO
; Metric multidimensional scaling
; - A superset of classical MDS
; Generalized multidimensional scaling
; - An extension of metric multidimensional scaling
