(ns quantum.numeric.vectors
  (:refer-clojure :exclude [reduce max for count get subvec swap!])
           (:require
   #?@(:clj [[uncomplicate.neanderthal
               [real   :as real]
               [native :as nat]
               [opencl :as cl]
               [core   :as fnum]] ; fast num
             [uncomplicate.neanderthal.impl.fluokitten :as fluo]])
             [clojure.core.matrix      :as mat]
             [quantum.core.collections :as c
               :refer        [map+ range+
                              #?@(:clj [for lfor reduce join count])]
               :refer-macros [          for lfor reduce join count]]
             [quantum.core.compare
               :refer        [max]]
             [quantum.core.numeric :as cnum
               :refer        [#?@(:clj [sqrt])]
               :refer-macros [          sqrt]]
             [quantum.core.fn      :as fn
               :refer        [#?@(:clj [fn1 <- fn-> fn->>])]
               :refer-macros [          fn1 <- fn-> fn->>]]
             [quantum.core.error   :as err
               :refer [->ex]]
             [quantum.numeric.core :as num
               :refer [pi* sigma sq]]
             [quantum.core.vars
               :refer        [#?(:clj defalias)]
               :refer-macros [        defalias]])
  #?(:clj (:import (org.apache.spark.mllib.linalg BLAS DenseVector))))

; TODO probably use core.matrix API

; TO EXPLORE
; - core.matrix — incorporate!
; - EJML if performance on small matrices is more important than features
; - Neanderthal if performance on large (n >= 50) matrices is needed
; http://blog.mikiobraun.de/2009/04/some-benchmark-numbers-for-jblas.html
; - (Java-only) Colt performs atrociously
; Even for very small matrices (except for matrices smaller than 5x5),
; Neanderthal is faster than pure Java library Vectorz.
; MTJ=matrix-toolkits-java
; spark.mllib -> breeze -> netlib-java -> BLAS/LAPACK
;                MTJ    -> netlib-java -> BLAS/LAPACK
; =================

(def ^:dynamic *impl* :neanderthal) ; can be #{:neanderthal :mllib}

; Just like operations in core.numeric, <op>* means approximate, i.e., no
; auto-promotion

; This takes after mllib/breeze's API

; ============ GET / SET ============

#_"Returns a primitive ^double i-th entry of vector x, or ij-th entry of matrix m."
#?(:clj (defalias                                get              real/entry ))
#_"Sets the i-th entry of vector x, or ij-th entry of matrix m."
#?(:clj (defalias                                set!             real/entry!))
#_"Returns the BOXED i-th entry of vector x, or ij-th entry of matrix m."
#?(:clj (defalias                                boxed-get        fnum/entry ))
#?(:clj (defalias                                boxed-set!       fnum/entry!))

#_"May take either a boxed or unboxed fn:
   (update! (dv 1 2 3) 2 (fn ^double [^double x] (inc x)))"
#?(:clj (defalias                                update!          fnum/alter!))

; ============ CREATE ============

#_"Creates a native-backed float vector from source."
#?(:clj (defalias                                ->fvec           nat/sv ))
#_"Creates a native-backed double vector from source."
#?(:clj (defalias                                ->dvec           nat/dv ))
#?(:clj (defalias                                ->cl-vec         cl/clv ))
#?(:clj (defalias                                ->vec            fnum/create-vector)) ; TODO internal type dispatch can be improved

#_"Creates a native-backed, dense, float mxn matrix from source.
   If called with two arguments, creates a zero matrix with dimensions mxn."
#?(:clj (defalias                                ->fmatrix        nat/sge))
#_"Creates a native-backed, dense, double mxn matrix from source.
   If called with two arguments, creates a zero matrix with dimensions mxn."
#?(:clj (defalias                                ->dmatrix        nat/dge))
#?(:clj (defalias                                ->cl-matrix      cl/clge))
#?(:clj (defalias                                ->matrix         fnum/create-ge-matrix)) ; TODO internal type dispatch can be improved

#_"Returns an uninitialized instance of the same type and dimension(s) as x"
#?(:clj (defalias                                ->raw            fnum/raw       ))
#_"Returns an instance of the same type and dimension(s) as x"
#?(:clj (defalias                                ->zeroed         fnum/zero      ))
#?(:clj (defalias                                create           fnum/create    ))
#?(:clj (defalias                                create-raw       fnum/create-raw))

; ============ PREDICATES ============

#?(:clj (defalias                                vec?             fnum/vect?  ))
#?(:clj (defalias                                matrix?          fnum/matrix?))

#_"Check whether two objects that have some memory context are compatible."
#?(:clj (defalias                                compatible?      fnum/compatible?))

; ============ MEMORY ============

#_"Ensures that the data x is in the native main memory"
#?(:clj (defalias                                ->native         fnum/native   ))
#?(:clj (def                                     transfer!        fnum/transfer!)) ; Because multimethod
#?(:clj (defalias                                transfer         fnum/transfer ))

;; ============ OPERATIONS (BLAS) ============

#_"Returns the dimension of the vector x."
#?(:clj (defalias                                dim              fnum/dim      ))
#_"Returns the total number of elements in all dimensions of a block x
  of (possibly strided) memory."
#?(:clj (defalias                                ecount           fnum/ecount   ))
#_"Returns a subvector starting witk a, b entries long"
#?(:clj (defalias                                subvec           fnum/subvector))

#?(:clj (defalias                                num-rows         fnum/mrows    ))
#?(:clj (defalias                                num-cols         fnum/ncols    ))
#_"Returns the i-th row of the matrix m as a vector."
#?(:clj (defalias                                row              fnum/row      ))
#_"Returns the j-th column of the matrix m as a vector."
#?(:clj (defalias                                col              fnum/col      ))
#_"Returns a lazy sequence of vectors that represent
   columns of the matrix m."
#?(:clj (defalias                                lcols            fnum/cols     ))
#_"Returns a lazy sequence of vectors that represent
   rows of the matrix m."
#?(:clj (defalias                                lrows            fnum/rows     ))
#_"Returns a submatrix of m starting with row i, column j,
   that has k columns and l rows."
#?(:clj (defalias                                submatrix        fnum/submatrix))
#?(:clj (defalias                                transpose*       fnum/trans    ))

#_"Computes the dot product of vectors x and y."
#?(:clj (defalias ^{:time-complexity 'n}         dot*             real/dot   ))
#_"Computes the Euclidan (L2) norm of vector x."
#?(:clj (defalias ^{:time-complexity 'n}         l2-norm          real/nrm2  ))
#_"Sums absolute values of entries of vector x."
#?(:clj (defalias ^{:time-complexity 'n}         abs-sum          real/asum  ))
#_"Sums values of entries of x."
#?(:clj (defalias                                sum*             real/sum   ))
#_"The index of the largest absolute value."
#?(:clj (defalias ^{:time-complexity 'n}         index-of-max-abs fnum/iamax ))
#_"The index of the largest value."
#?(:clj (defalias ^{:time-complexity 'n}         index-of-max     fnum/imax  ))
#_"The index of the smallest value."
#?(:clj (defalias ^{:time-complexity 'n}         index-of-min     fnum/imin  ))
#_"Mutably computes x = ax"
#?(:clj (defalias ^{:time-complexity 'n}         ax!              fnum/scal! ))
#_"Immutably computes x = ax"
#?(:clj (defalias                                scale            fnum/ax    ))
#_"Apply plane rotation"
#?(:clj (defalias ^{:time-complexity 'n}         rotate!          fnum/rot!  ))
#_"Apply modified plane rotation"
#?(:clj (defalias ^{:time-complexity 'n}         mod-rotate!      fnum/rotm! ))
#_"Generate plane rotation"
#?(:clj (defalias ^{:time-complexity 'n}         gen-rotate!      fnum/rotg! ))
#_"Generate modified plane rotation."
#?(:clj (defalias ^{:time-complexity 'n}         gen-mod-rotate!  fnum/rotmg!))
#_"Mutably computes y = ax + y."
#?(:clj (defalias ^{:time-complexity 'n}         ax+y!            fnum/axpy! ))
#_"Immutably computes y = ax + y."
#?(:clj (defalias                                ax+y             fnum/axpy  ))
#_"Sums containers x, y & zs. The result is a new vector."
#?(:clj (defalias                                v+               fnum/xpy   ))
#_"Computes c = α*a*b + β*c. Matrix multiply."
#?(:clj (defalias ^{:time-complexity '(pow n 3)} m*!              fnum/mm!   ))
#?(:clj (defalias ^{:time-complexity '(pow n 3)} m*               fnum/mm    ))
#_"Computes y = α*a*x + β*y. Matrix-vector multiply."
#?(:clj (defalias ^{:time-complexity '(pow n 2)} v*!              fnum/mv!   ))
#?(:clj (defalias v*               fnum/mv    ))
#_"General rank-1 update.
   Computes a = alpha * x * y' + a"
#?(:clj (defalias ^{:time-complexity '(pow n 2)} rank!            fnum/rank! ))
#?(:clj (defalias rank             fnum/rank  ))
#?(:clj (defalias ^{:time-complexity 'n}         copy!            fnum/copy! ))
#?(:clj (defalias copy             fnum/copy  ))
#_"Swaps the entries of containers x and y."
#?(:clj (defalias ^{:time-complexity 'n}         swap!            fnum/swp!  ))

(defn transpose
  "Transpose a vector of vectors."
  {:adapted-from 'criterium.stats
   :todo ["better implementation"]}
  [data]
  (if (vector? (first data))
      (apply mapv vector data)
      data))

; TODO have reducers version of these?
; TODO use numeric core functions

(defn v-op+ [op v1 v2]
  (assert (= (count v1) (count v2))) ; TODO maybe use (map+ f v1 v2) ?
  (->> (range+ 0 (count v1))
       (map+ #(op (c/get v1 %) (c/get v2 %)))))

(defn v-+
  {:tests `{[[1 2 3] [4 5 6]]
            [-3 -3 -3]}}
  [v1 v2]
  (v-op+ - v1 v2))

(defn v++
  {:tests `{[[1 2 3] [4 5 6]]
            [5 7 9]}}
  [v1 v2]
  (v-op+ + v1 v2))

(defn v-div+
  {:tests `{[[1 2 3] [4 5 6]]
            [1/4 2/5 1/2]}}
  [v1 v2] (v-op+ / v1 v2))

(defn v*+
  {:tests `{[[1 2 3] [4 5 6]]
            [4 10 2]}}
  [v1 v2] (v-op+ * v1 v2))

(defn vsq+ [v] (v*+ v v))

(defn dot [v1 v2] (num/sum (v*+ v1 v2)))

(defn vsum [vs] (reduce v++ (first vs) (rest vs))) ; TODO optimize better

(defn centroid+ [vs]
  (->> vs vsum (map+ (fn1 / (count vs)))))

(defalias vmean+ centroid+)

#?(:clj
(defn vector-map!
  ([f a      ] (fluo/vector-fmap! a f      ) a)
  ([f a b    ] (fluo/vector-fmap! a f b    ) a)
  ([f a b c  ] (fluo/vector-fmap! a f b c  ) a)
  ([f a b c d] (fluo/vector-fmap! a f b c d) a)))

#?(:clj
(defn vector-map
  ([f a      ] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (vector-map! f a      )))
  ([f a b    ] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (vector-map! f a b    )))
  ([f a b c  ] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (vector-map! f a b c  )))
  ([f a b c d] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (vector-map! f a b c d)))))

#?(:clj
(defn matrix-map!
  ([f a      ] (fluo/matrix-fmap! a f      ) a)
  ([f a b    ] (fluo/matrix-fmap! a f b    ) a)
  ([f a b c  ] (fluo/matrix-fmap! a f b c  ) a)
  ([f a b c d] (fluo/matrix-fmap! a f b c d) a)))

#?(:clj
(defn matrix-map
  ([f a      ] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (matrix-map! f a      )))
  ([f a b    ] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (matrix-map! f a b    )))
  ([f a b c  ] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (matrix-map! f a b c  )))
  ([f a b c d] (uncomplicate.commons.core/let-release [res (fnum/copy a)] (matrix-map! f a b c d)))))

(defn scalar++
  "Add a scalar to a vector"
  [v scalar] (map+ (fn1 + scalar) v))

(defn scalar-+
  "Subtract a scalar from a vector"
  [v scalar] (map+ (fn1 - scalar) v))

(defn scalar*+
  "Multiply a scalar with a vector by element-wise"
  [v scalar] (map+ (fn1 * scalar) v))

(defn scalar-div+
  "Divide a scalar with a vector by element-wise"
  [v scalar] (map+ (fn1 / scalar) v))

(defn clamp+
  "Clamp the vector between minimum and maximum values"
  [cmin cmax v] (map+ #(max cmin (min cmax %)) v))

(defn abs+
  "Element-wise absolute operation to a vector"
  [v] (map+ (fn1 cnum/abs) v))

(defn pow+
  "Element-wise power operation to a vector"
  [v scalar] (map+ (fn1 cnum/pow scalar) v))
