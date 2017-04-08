(ns quantum.ai.ml.similarity
  "Calculates similarity measures: distances, norms,
   and so on.
   A distance function maps pairs of points into the
   nonnegative reals and has to satisfy:
   - non-negativity: d(x, y) > 0
   - isolation:      d(x, y) = 0 iff x = y
   - symmetry:       d(x, y) = d(x, y)"
  (:refer-clojure :exclude [get count])
  (:require
    [quantum.core.logic
      :refer [fn-or]]
    [quantum.core.fn
      :refer [<- fn1 fn->]]
    [quantum.core.collections.core :as ccoll]
    [quantum.core.collections :as coll
      :refer [map+, filter+, remove+, red-apply, range+
              assoc-in!, count
              kw-map, ifor, get get-in*, reducei]]
    [quantum.core.error
      :refer [->ex TODO]]
    [quantum.core.macros
      :refer [defnt #?@(:clj [defnt'])]]
    [quantum.core.numeric    :as cnum
      :refer [+* inc* pow abs sqrt floor div:natural]]
    [quantum.core.refs       :as refs
      :refer [!ref setm!]]
    [quantum.numeric.core    :as num
      :refer [sum sigma sq]]
    [quantum.numeric.tensors :as tens]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.string     :as str])
  #?(:clj (:import [quantum.ai.ml.core Attribute])))

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
        m       (ccoll/->ints-nd
                  (-> s1 count inc)
                  (-> s2 count inc))
        cost (!ref 0)]
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
   The L-p distance between two n-dimensional vectors.

   Does not appropriately handle non-continuous input attributes."
  {:implemented-by '{smile.math.distance.MinkowskiDistance "faster array implementation"
                     smile.math.distance.SparseMinkowskiDistance "for sparse arrays"}}
  ([v p]
    (->> v (map+ (fn-> abs (pow p))) sum (<- pow (/ p))))
  ([a b p] (TODO)))

(defalias minkowski l-p)

(defn l-inf
  "[v]
   The (L∞|Chebyshev|Lp when p -> ∞|max|max of abs) norm of a vector.

   [a b]
   The (L∞|Chebyshev|Lp when p -> ∞|max|max of abs) distance between two n-dimensional
   vectors.

   Does not appropriately handle non-continuous input attributes."
  {:implemented-by '{smile.math.distance.ChebyshevDistance       "faster array implementation"
                     smile.math.distance.SparseChebyshevDistance "for sparse arrays"}}
  ([v] (->> v (map+ (fn1 abs)) (red-apply max)))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.ChebyshevDistance}}
  ([a b] (TODO)))

(defalias l-∞       l-inf)
(defalias chebyshev l-inf)

(defn l-1
  "[v]
   The (L1|Manhattan|sum of abs) norm of a vector.

   [a b]
   The (L1|Manhattan|sum of abs) distance between two n-dimensional vectors.

   Called `Manhattan distance` based on the gridlike street geography of Manhattan.
   Requires less computation than the Euclidean distance metric.
   Does not appropriately handle non-continuous input attributes."
  {:implemented-by '{smile.math.distance.ManhattanDistance "faster array implementation"
                     smile.math.distance.SparseManhattanDistance "for sparse arrays"}}
  ([v] (->> v (map+ (fn1 abs)) sum))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.ManhattanDistance}}
  ([a b] (sum (tens/v-op+ #(abs (- %1 %2)) a b))))

(defalias manhattan l-1)
(defalias city-block l-1)

(#?(:clj defnt' :cljs defnt) l-2
  "[x•]
   The (L-2|Euclidean) norm of a vector.

   [x•0 x•1]
   The (L-2|Euclidean) distance between two n-dimensional vectors.

   By far the most commonly used distance metric.

   The square root is often not computed in practice, but e.g. distance-
   weighted k-nearest neighbor (Dudani, 1976) requires it to be computed.

   One weakness is that if one of the input attributes has a relatively
   large range, then it can overpower the other attributes. Thus
   normalization is preferred in the inputs.

   Does not appropriately handle non-continuous input attributes."
  {:implemented-by '{smile.math.distance.EuclideanDistance "faster array implementation"
                     smile.math.distance.SparseEuclideanDistance "for sparse arrays"
                     smile.math.matrix.SingularValueDecomposition "The largest singular value"}}
  ([#_indexed? #{array-1d? +vec?} x•]
    (->> x• (map+ (fn1 sq)) sum sqrt))
  ^{:implemented-by '#{org.apache.commons.math3.ml.distance.EuclideanDistance}}
  ([#_indexed? #{array-1d? +vec?} x•0 #_indexed? #{array-1d? +vec?} x•1]
    (->> (tens/v-op+ #(sq (- %1 %2)) x•0 x•1) sum sqrt)))

#?(:clj (defalias euclidean l-2))
(defn vlength [v] (l-2 v))

(defn cosine-similarity [a b]
  (/ (tens/dot a b)
     (* (l-2 a) (l-2 b))))

(defn canberra
  "Calculates the Canberra distance between two n-dimensional vectors.
   Does not appropriately handle non-continuous input attributes."
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
   to a known one.
   Does not appropriately handle non-continuous input attributes."
  {:implemented-by '#{smile.math.distance.MahalanobisDistance}}
  [a b] (TODO))

(defn earth-movers
  "Calculates the Earh Mover's distance (also known as
   Wasserstein metric) between two distributions."
  {:implemented-by '#{org.apache.commons.math3.ml.distance.EarthMoversDistance}}
  [a b] (TODO))

(defalias wasserstein-metric earth-movers)

(defn quadratic
  "Does not appropriately handle non-continuous input attributes."
  {:see-also "Journal of Artificial Intelligence Research 6 (1997) 1-34,
              Improved Heterogeneous Distance Functions"}
  [] (TODO))

(defn correlation
  "Does not appropriately handle non-continuous input attributes."
  {:see-also "Journal of Artificial Intelligence Research 6 (1997) 1-34,
              Improved Heterogeneous Distance Functions"}
  [] (TODO))

(defn chi-square
  "Does not appropriately handle non-continuous input attributes."
  {:see-also "Journal of Artificial Intelligence Research 6 (1997) 1-34,
              Improved Heterogeneous Distance Functions"}
  [] (TODO))

(defn kendall-rank-correlation
  "Does not appropriately handle non-continuous input attributes."
  {:see-also "Journal of Artificial Intelligence Research 6 (1997) 1-34,
              Improved Heterogeneous Distance Functions"}
  [] (TODO))

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

(defn heom
  "Heterogeneous Euclidean-Overlap Metric (HEOM).
   Removes the effects of the arbitrary ordering of nominal values, but its
   overly simplistic approach to handling nominal attributes fails to make
   use of additional information provided by nominal attribute values that
   can aid in generalization."
  {:deprecated? true
   :see-also  "Journal of Artificial Intelligence Research 6 (1997) 1-34,
               Improved Heterogeneous Distance Functions"}
  [] (TODO))



(defnt' <n:x:a
  "The number of instances in the training set that have value `x` for attribute `a`."
  {:complexity "`T(n)`: n = `(count <x•+l>•)`"}
  ([<x•+l>• ^Attribute a x] (->> <x•+l>• (filter+ (fn-> :x• (get (.-i a)) (= x))) count)))

(defnt' <n:x:a:l
  "The number of instances in the training set that have value `x` for attribute `a`
   and label class/value `l`."
  {:params-doc '{l• "The vector containing labels for all instances"}
   :complexity "`T(n)`: n = `(count <x•+l>•)`"}
  ([<x•+l>• ^Attribute a x l]
    (->> <x•+l>• (filter+ (fn-> :x• (get (.-i a)) (= x)))
                 (filter+ (fn-> :l                (= l)))
                 count)))

(defnt' <p:x:a:l
  "The conditional probability that the label class/value is `l` given that attribute
   `a` has the value `x`, i.e., `P(l | xa)`."
  {:complexity "`T(2n)`: n = `(count <x•+l>•)`"}
  ([<x•+l>• ^Attribute a x l]
    (div:natural (<n:x:a:l <x•+l>• a x l)
       (<n:x:a   <x•+l>• a x)))
  ^{:complexity "Saves `n` -> `T(n)`"}
  ([<x•+l>• ^Attribute a x l, ^long n:x:a]
    (div:natural (<n:x:a:l <x•+l>• a x l) n:x:a)))

(defnt' <p:x:a:l:double
  "The conditional probability that the label class/value is `l` given that attribute
   `a` has the value `x`, i.e., `P(l | xa)`.
   Outputs a double instead of a ratio."
  {:complexity "`T(2n)`: n = `(count <x•+l>•)`"}
  ([<x•+l>• ^Attribute a x l]
    (div:natural (double (<n:x:a:l <x•+l>• a x l))
                 (double (<n:x:a   <x•+l>• a x))))
  ^{:complexity "Saves `n` -> `n`"}
  ([<x•+l>• ^Attribute a x l, ^long n:x:a]
    (div:natural (double (<n:x:a:l <x•+l>• a x l)) n:x:a)))

(defnt' vdm:single
  "VDM for a single combination of `x0`, `x1`, and `a` rather than vectors of them."
  {:params-doc '{l◦ "Unique label values (i.e., all label classes).
                     Assumes only 1-element label vectors (only 1 label-attribute)."}
   :todo       #{"Rename to something better"}
   :algorithm  "Stanfill & Waltz, 1986"
   :complexity "`T(c•4n)`: c = `(count c•:l)
                           n = `(count <x•+l>•)`"}
  ([<x•+l>• l◦ ^Attribute a x0 x1 ^long q]
    (sigma l◦ (fn [l] (-> (- (<p:x:a:l:double <x•+l>• a x0 l)
                             (<p:x:a:l:double <x•+l>• a x1 l))
                             abs
                             (pow q)))))
  ^{:complexity "Saves `c•2n` -> `T(c•2n)`"}
  ([<x•+l>• l◦ ^Attribute a, x0 n:x0:a, x1 n:x1:a, ^long q]
    (sigma l◦ (fn [l] (-> (- (<p:x:a:l:double <x•+l>• a x0 l n:x0:a)
                             (<p:x:a:l:double <x•+l>• a x1 l n:x1:a))
                             abs
                             (pow q))))))

(defn vdm
  "Value distance metric.
   Designed to find reasonable distance values between nominal attribute values.
   Largely ignores continuous attributes, requiring discretization to map continuous
   values into nominal values, which can degrade generalization accuracy."
  {:algorithm "Stanfill & Waltz, 1986"
   :see-also  "Journal of Artificial Intelligence Research 6 (1997) 1-34,
               Improved Heterogeneous Distance Functions"}
  [] (TODO))

(defn mvdm
  "Modified value distance metric.
   Like VDM, but uses a different weighting scheme."
  {:algorithm "Cost & Salzberg, 1993; Rachlin et al., 1994"
   :see-also  "Journal of Artificial Intelligence Research 6 (1997) 1-34,
               Improved Heterogeneous Distance Functions"}
  [] (TODO))

(defn hvdm
  "Heterogeneous Value Difference Metric."
  {:algorithm "Journal of Artificial Intelligence Research 6 (1997) 1-34,
               Improved Heterogeneous Distance Functions"}
  [] (TODO))

(defn wvdm
  "Windowed Value Difference Metric"
  {:algorithm "Journal of Artificial Intelligence Research 6 (1997) 1-34,
               Improved Heterogeneous Distance Functions"}
  [] (TODO))

(defnt' width:disc
  "The width of a discretized interval for a continuous attribute `a`.
   `s` is a hyperparameter."
  {:params-doc '{a:min "The minimum value of `a`"
                 a:max "The maximum value of `a`"
                 s "From Journal of Artificial Intelligence Research 6 (1997) 1-34,
                    Improved Heterogeneous Distance Functions:

                    Unfortunately, there is currently little guidance on what value
                    of s to use. A value that is too large will reduce the statistical
                    strength of the values of P, while a value too small will not allow
                    for discrimination among classes.
                    However, s could be determined automatically by the following
                    heuristic: let s be 5 or C, whichever is greatest, where C is the
                    number of output classes in the problem domain.
                    Current research is examining more sophisticated techniques for
                    determining good values of s ... (e.g., Tapia & Thompson, 1978, p. 67)."}
   :todo #{"The types of these parameters are too strict"}}
  ([^Attribute a ^long s]
    (/ (abs (- (.-max a) (.-min a))) s)))

(defnt' width:disc:auto
  {:params-doc '{n:l◦ "Number of unique label values (i.e. label classes)"}}
  ([^Attribute a ^long n:l◦]
    (width:disc a (max 5 n:l◦))))

(defnt' mid:disc
  "The midpoint of a discretized range u.
   `s` is a hyperparameter."
  [^double u ^Attribute a ^long s]
  (+ (.-min a) (* (width:disc a s) (+ u 0.5)))) ; TODO could be 1/2

(defnt' discretize
  "The discretized value of a continuous value x for attribute a is an integer from 1 to `s`.
   `s` is a hyperparameter.
   Returns `u` as used elsewhere."
  [^double x ^Attribute a ^long s]
  (cond (.-discrete? a)
          x
        (= x (.-max a))
          s
        :else
          (inc (floor (/ (- x (.-min a))
                         (width:disc a s))))))

(defnt' p:interpolated
  "The interpolated probability value of a continuous value x
   for attribute a and label-value l.
   `s` is a hyperparameter."
  {:complexity "`T(4n)`: n = `(count <x•+l>•)`"}
  [<x•+l>• ^double x l ^Attribute a ^long s]
  (let [u         (discretize x a s)
        u         (if (< x (mid:disc u a s))
                      (dec u)
                      u)
        mid:u     (mid:disc u       a s)
        mid:u+1   (mid:disc (inc u) a s)
        p:mid:u   (<p:x:a:l:double <x•+l>• a mid:u   l) ; T(2n)
        p:mid:u+1 (<p:x:a:l:double <x•+l>• a mid:u+1 l)] ; T(2n)
    (+ p:mid:u
       (* (/ (- x mid:u)
             (- mid:u+1 mid:u))
          (- p:mid:u+1 p:mid:u)))))

(defnt' ivdm:single
  "Interpolated Value Difference Metric for a single pair of x."
  {:params-doc '{l◦ "Unique label values (i.e., all label classes).
                     Assumes only 1-element label vectors (only 1 label-attribute)."}
   :complexity {:worst "`T(c•8n)`: c = `(count c•:l)`
                                   n = `(count <x•+l>•)`"}}
  [<x•+l>• l◦ ^double x0 ^double x1 ^Attribute a ^long s]
  (if (:discrete? a)
      (vdm:single <x•+l>• l◦ a x0 x1 2) ; T(c•4n)
      ;; T(c•8n)
      (sigma l◦ (fn [l] (sq (abs (- (p:interpolated <x•+l>• x0 l a s) ; T(4n)
                                    (p:interpolated <x•+l>• x1 l a s)))))))) ; T(4n)

(defnt' ivdm
  "Interpolated Value Difference Metric.

   Unlike most other distance metrics, IVDM is designed to handle nominal
   attributes, continuous attributes, or both."
  {:algorithm  "Journal of Artificial Intelligence Research 6 (1997) 1-34,
                Improved Heterogeneous Distance Functions"
   :accuracy   "Averages 5% more accurate than Euclidean on a wide variety of datasets,
                especially ones with both nominal and continuous attributes.
                More accurate than HVDM and slightly more accurate than WVDM."
   :complexity {:worst "`T(i•c•8n)`: i = `(count x•0)`
                                     c = `(count c•:l)`
                                     n = `(count <x•+l>•)`
                        Storage: mn+mvC
                        Learning time: mn+mvC
                        Generalization time: O(mnC)
                        Requires less time and storage than WVDM."}
   :params-doc '{a• "Attributes which apply to both instances"
                 l◦ "Unique label values (i.e., all label classes).
                     Assumes only 1-element label vectors (only 1 label-attribute)."}}
  ([#_indexed? #{doubles? +vec?} x•0
    #_indexed? #{doubles? +vec?} x•1
               #{objects? +vec?} a•
    <x•+l>• l◦]
    (ivdm x•0 x•1 a• <x•+l>• l◦ (max 5 (count l◦))))
  ([#_indexed? #{doubles? +vec?} x•0
    #_indexed? #{doubles? +vec?} x•1
               #{objects? +vec?} a•
    <x•+l>• l◦ ^long s]
    (sqrt (sum (tens/v-op+ #(sq (ivdm:single <x•+l>• l◦ %1 %2 %3 s)) x•0 x•1 a•)))))
