(ns quantum.numeric.tensors
  "1D array: vector
   2D array: matrix
   ND array: tensor

   Note: (Java-only) Colt performs atrociously."
  {:todo #{"Incorporate https://github.com/thi-ng/geom/blob/master/geom-core/src/vector.org"
           "Incorporate https://github.com/thi-ng/geom/blob/master/geom-core/src/matrix.org"
           "Incorporate https://github.com/thi-ng/geom/blob/master/geom-core/src/quaternion.org"
           "Probably use core.matrix API"
           "Incorporate core.matrix"
           "EJML if performance on small matrices is more important than features"
           "Neanderthal if performance on large (n >= 50) matrices is needed.
            Even for very small matrices (except for matrices smaller than 5x5), Neanderthal is
            faster than the pure Java library Vectorz.
            - http://blog.mikiobraun.de/2009/04/some-benchmark-numbers-for-jblas.html"
           "MTJ="
           "spark.mllib -> breeze                     -> netlib-java -> BLAS/LAPACK
                           matrix-toolkits-java (MTJ) -> netlib-java -> BLAS/LAPACK"
           "Mathematica
            - Matrix and data manipulation tools including support for sparse arrays"}}
  (:refer-clojure :exclude
    [max count get subvec swap! first last empty contains?
     for dotimes, reduce])
  (:require
#?@(:clj
   [[uncomplicate.neanderthal
      [real   :as real]
      [native :as nat]
      [opencl :as cl]
      [core   :as fnum]] ; fast num
    [uncomplicate.neanderthal.impl.fluokitten :as fluo]])
    [clojure.core.matrix                      :as mat]
    [quantum.core.collections.core            :as ccoll]
    [quantum.core.collections                 :as c
      :refer [map+ range+ first contains? red-for red-fori
              reduce join count slice kw-map
              for lfor dotimes doreduce
              ->objects]]
    [quantum.core.compare
      :refer [max]]
    [quantum.core.data.primitive
      :refer [>int >long >double]]
    [quantum.core.error                       :as err
      :refer [>ex-info TODO]]
    [quantum.core.fn                          :as fn
      :refer [fn1 fn&2 fn-> fn->>]]
    [quantum.core.numeric                     :as cnum
      :refer [sqrt]]
    [quantum.numeric.core                     :as num
      :refer [pi* sigma sq sum]]
    [quantum.core.macros
      :refer [defnt #?@(:clj [defnt'])]]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.spec                        :as s
      :refer [validate]]
    [quantum.core.type-old                    :as t])
  #?(:clj
    (:import
      [org.apache.spark.mllib.linalg BLAS DenseVector]
      [uncomplicate.neanderthal.protocols Vector RealVector RealMatrix RealChangeable])))

; =================================

; =================

(def ^:dynamic *impl* :neanderthal) ; can be #{:neanderthal :mllib}

; Just like operations in core.numeric, <op>* means approximate, i.e., no
; auto-promotion

; This takes after mllib/breeze's API

; ============ CREATE ============ ;

(defn ->sparse-matrix
  "A sparse matrix is a matrix populated primarily with zeros. Conceptually,
   sparsity corresponds to systems which are loosely coupled. Huge sparse
   matrices often appear when solving partial differential equations."
  {:implemented-by '#{package smile.math.matrix.SparseMatrix}}
  [?] (TODO))

(defn ->band-matrix
  "A band matrix is a sparse matrix whose non-zero entries are confined to
   a diagonal band, comprising the main diagonal and zero or more diagonals
   on either side."
  {:implemented-by '#{package smile.math.matrix.BandMatrix}}
  [?] (TODO))

(defn ->row-major-matrix
  "A dense matrix whose data is stored in a single 1D array of
   doubles in row major order."
  {:implemented-by '#{package smile.math.matrix.RowMajorMatrix}}
  [?] (TODO))

(defn ->column-major-matrix
  "A dense matrix whose data is stored in a single 1D array of
   doubles in column major order."
  {:implemented-by '#{package smile.math.matrix.ColumnMajorMatrix}}
  [?] (TODO))


#_"Creates a native-backed float vector from source."
#?(:clj (defalias                                ->fvec           nat/sv ))
#?(:clj (alter-meta! #'->fvec assoc :tag RealVector))
#_"Creates a native-backed double vector from source."
#?(:clj (defalias                                ->dvec           nat/dv ))
#?(:clj (alter-meta! #'->dvec assoc :tag RealVector))
#?(:clj (defalias                                ->cl-vec         cl/clv ))
#?(:clj (defalias                                ->vec            fnum/create-vector)) ; TODO internal type dispatch can be improved

; Creates an OpenCL matrix
#?(:clj (defalias                                ->cl-matrix      cl/clge))
#?(:clj (defalias                                ->matrix         fnum/create-ge-matrix)) ; TODO internal type dispatch can be improved

#_"Returns an uninitialized instance of the same type and dimension(s) as x"
#?(:clj (defalias                                empty            fnum/raw       ))
#_"Returns an instance of the same type and dimension(s) as x"
#?(:clj (defalias                                ->zeroed         fnum/zero      ))
#?(:clj (defalias                                create           fnum/create    ))
#?(:clj (defalias                                create-raw       fnum/create-raw))

; ============ GET / SET ============ ;

(defnt get-in*
  "Returns the i-th entry of vector x,
   or ij-th entry of matrix m.
   Breeze: a(0,1)
   Matlab: a(1,2)
   numpy:  a[0,1]
   R:      a[1,2]"
  {:implemented-by '#{smile.math.matrix.Matrix}}
  #?(:clj (^double [^RealVector X ^long a        ] (>double (real/entry X a  ))))
          (        [            X       a        ] (TODO))
  #?(:clj (^double [^RealMatrix X ^long a ^long b] (>double (real/entry X a b))))
          (        [            X       a       b] (TODO)))

(defnt set-in!*
  "Sets the i-th entry of vector x,
   or ij-th entry of matrix m."
  {:implemented-by '#{smile.math.matrix.Matrix}}
  #?(:clj ([^RealVector     X ^double v ^long a        ] ^RealVector (real/entry! X a v  ))) ; TODO should be RealChangeable??
          ([                X         v       a        ] (TODO))
  #?(:clj ([^RealMatrix     X ^double v ^long b ^long a] ^RealMatrix (real/entry! X a b v)))
          ([                X         v       b       a] (TODO)))

#_"Returns the BOXED i-th entry of vector x, or ij-th entry of matrix m."
#?(:clj (defalias                                boxed-get-in*    fnum/entry ))
#?(:clj (defalias                                boxed-set-in!*   fnum/entry!))

; ============ CREATE ============ ;

#_"Creates a native-backed, dense, float mxn matrix from source.
   If called with two arguments, creates a zero matrix with dimensions mxn."
#?(:clj (defalias                                ->fmatrix        nat/sge))

#?(:clj (alter-meta! #'nat/dge assoc :tag 'RealMatrix))

#?(:clj
(defnt ^RealMatrix ->dmatrix
  "Creates a native-backed, dense, double mxn matrix from source.
   If called with two arguments, creates a zero matrix with dimensions mxn.
   If called with one argument on a 2D array or 2D sequence, creates from it."
  ([^long m ^long n source options] (nat/dge m n source options))
  ([^long m ^long n arg           ] (nat/dge m n arg))
  ([^long m ^long n               ] (nat/dge m n))
  ([      x                       ] (nat/dge x))
  ([^array-2d? x]
    (let [width  (-> x c/first ccoll/count& >long) ; TODO fix where type hints aren't showing up
          height (c/count x)
          ret    (->dmatrix width height)]
      (dotimes [i height j width]
        (set-in!* ret (-> x (c/get-in* (int i) (int j)) >double) i j)) ; TODO figure out why `>int` instead of `int` creates verifyerror
      ret))
  ([^vector? x] ; TODO lists/seqs are okay too
    (if (c/empty? x)
        (->dmatrix 0 0)
        (let [_      (validate x (fn-> c/first t/sequential?))
              width  (-> x c/first c/count >long)
              height (c/count x)
              ret    (->dmatrix width height)]
          (red-fori [row  x
                     ret' ret i]
            ; All rows must be same width
            (assert (-> row c/count (= width)) (kw-map row width i)) ; TODO cheap `validate`
            (red-fori [elem row _ nil j]
              (set-in!* ret' (>double elem) i j)))
          ret)))))

#_"May take either a boxed or unboxed fn:
   (update! (dv 1 2 3) 2 (fn ^double [^double x] (inc x)))"
#?(:clj (defalias                                update!          fnum/alter!))

(defnt subvec
  "Extract subset of vector.
   Returns a subvector starting with a, b entries long.
   Breeze: a(1 to 4) or a(1 until 5) or a.slice(1,5)
   Matlab: a(2:5)
   numpy:  a[1:5]
   R:      a[2:5]

   (negative steps)
   Breeze: a(5 to 0 by -1)
   Matlab: a(6:-1:1)
   numpy:  a[5::-1]

   (tail)  a(1 to -1)  a(2:end)  a[1:] a[2:length(a)] or tail(a,n=length(a)-1)"
  #?(:clj ([^Vector X ^long a ^long b] (fnum/subvector X a b)))
          ([        X       a       b] (TODO)))

(defn last
  "(last element)
   Breeze: a( -1 )
   Matlab: a(end)
   numpy:  a[-1]
   R:      tail(a, n=1)"
  [m] (TODO))

(defn col
  "Extract column of matrix
   Breeze: a(::, 2)
   Matlab: a(:,3)
   numpy:  a[:,2]
   R:      a[,2]"
  [m i] (TODO))

; ============ PREDICATES ============

#?(:clj (defalias                                vector?             fnum/vect?  ))
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
#?(:clj (defnt' ^long dim [^RealVector x] (fnum/dim x)))
#_"Returns the total number of elements in all dimensions of a block x
  of (possibly strided) memory."
#?(:clj (defalias                                ecount           fnum/ecount   ))

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
; Also implemented in Breeze
#?(:clj
(defnt' ^double dot* {:time-complexity 'n} [^RealVector a ^RealVector b] (real/dot a b)))

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
#?(:clj (defalias ^{:time-complexity 'n}         scale!           fnum/scal! ))
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
#?(:clj (defalias                                v*               fnum/mv    ))
#_"General rank-1 update.
   Computes a = alpha * x * y' + a"
#?(:clj (defalias ^{:time-complexity '(pow n 2)} rank!            fnum/rank! ))
#?(:clj (defalias                                rank             fnum/rank  ))
#?(:clj (defalias ^{:time-complexity 'n}         copy!            fnum/copy! ))
#?(:clj (defalias                                copy             fnum/copy  ))
#_"Swaps the entries of containers x and y."
#?(:clj (defalias ^{:time-complexity 'n}         swap!            fnum/swp!  ))

; ===== LAPACK ===== ;

(defn dgemm!
  {:implemented-by '#{org.apache.spark.ml.ann.BreezeUtil/dgemm}}
  [a A B b C] (TODO))

(defn dgemv!
  {:implemented-by '#{org.apache.spark.ml.ann.BreezeUtil/dgemv}}
  [a A x b y] (TODO))

; ===== SPARSE MATRIX SOLUTIONS ===== ;

; <no.uib.cipr.matrix.sparse.*>
; Unstructured sparse matrices and vectors with iterative solvers and preconditioners.

; ===== IMMUTABLE ===== ;

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

(defnt' v-op+
  ([f #_indexed? #{array-1d? +vector?} x•0 #_indexed? #{array-1d? +vector?} x•1]
    (assert (= (count x•0) (count x•1)) (kw-map (count x•0) (count x•1))); TODO maybe use (map+ f v1 v2) ?
    (->> (range+ 0 (count x•0))
         (map+ (fn [^long i] (f (c/get x•0 i) (c/get x•1 i))))))
  ([f #_indexed? #{array-1d? +vector?} x•0 #_indexed? ^:<0> x•1 #_indexed? ^:<0> x•2]
    (assert (= (count x•0) (count x•1) (count x•2)) (kw-map (count x•0) (count x•1) (count x•2))) ; TODO maybe use (map+ f v1 v2) ?
    (->> (range+ 0 (count x•0))
         (map+ (fn [^long i] (f (c/get x•0 i) (c/get x•1 i) (c/get x•2 i)))))))

(defnt' v-opi+
  ([f #_indexed? #{array-1d? +vector?} x•0 #_indexed? #{array-1d? +vector?} x•1]
    (assert (= (count x•0) (count x•1)) (kw-map (count x•0) (count x•1))); TODO maybe use (map+ f v1 v2) ?
    (->> (range+ 0 (count x•0))
         (map+ (fn [^long i] (f i (c/get x•0 i) (c/get x•1 i))))))
  ([f #_indexed? #{array-1d? +vector?} x•0 #_indexed? ^:<0> x•1 #_indexed? ^:<0> x•2]
    (assert (= (count x•0) (count x•1) (count x•2)) (kw-map (count x•0) (count x•1) (count x•2))) ; TODO maybe use (map+ f v1 v2) ?
    (->> (range+ 0 (count x•0))
         (map+ (fn [^long i] (f i (c/get x•0 i) (c/get x•1 i) (c/get x•2 i)))))))

(#?(:clj defnt' :cljs defnt) v-+
  [#_indexed? #{array-1d? +vector?} x•0 #_indexed? #{array-1d? +vector?} x•1]
  (v-op+ - x•0 x•1))

(#?(:clj defnt' :cljs defnt) v++
  [#_indexed? #{array-1d? +vector?} x•0 #_indexed? #{array-1d? +vector?} x•1]
  (v-op+ + x•0 x•1))

(#?(:clj defnt' :cljs defnt) v-div+
  [#_indexed? #{array-1d? +vector?} x•0 #_indexed? #{array-1d? +vector?} x•1]
  (v-op+ / x•0 x•1))

(#?(:clj defnt' :cljs defnt) v*+
  [#_indexed? #{array-1d? +vector?} x•0 #_indexed? #{array-1d? +vector?} x•1]
  (v-op+ * x•0 x•1))

(#?(:clj defnt' :cljs defnt) vsq+
  [#_indexed? #{array-1d? +vector?} x•] (v*+ x• x•))

(defn dot
  "Dot product"
  ([v0 v1] (num/sum (v*+ v0 v1)))
  ([v0 v1 & vs]
    (sigma (range+ 0 (c/count (c/first v0)))
           (fn [i] (->> vs (map+ (fn1 c/get i)) num/product (* (c/get v0 i) (c/get v1 i)))))))

#?(:clj (defalias • dot))

(defn column+ [i xs] (->> xs (map+ (fn1 c/get i))))

(defn vsum [vs] (reduce (fn&2 v++) (first vs) (rest vs))) ; TODO optimize better

(defn centroid+
  "Also sometimes called the center of mass or barycenter"
  ([v]
    (->> v vsum (map+ (fn1 / (count v))))))

(defn vmean+ [v] (centroid+ v))

#?(:clj
(defnt' vsum! [^numeric-1d? x•0 #{numeric-1d? +vector?} x•1]
  (doreduce (v-opi+ (fn [i x0 x1] (c/assoc! x•0 i (cnum/+ x0 x1))) x•0 x•1)) ; TODO fix this ; TODO type inference
  x•0))

#_(defnt centroid*
  "Handles heterogeneous and nominal attributes via `centroid:column-fn`."
  ([#{array-2d?} x•• ^fn? centroid:column-fn]
    (let [centroid-0 (->objects (count x••))]
      (reduce (fn [^objects centroid' x•]
                (dotimes [i x••])) centroid-0 (cons x• x•s)))))

(defnt centroid
  ([^numeric-2d? x••] (TODO)
    #_(let [centroid-0 (c/blank (c/first x••))]
      #_(reduce vsum! centroid-0 x••) ; the cleaner way
      (dotimes [i (count x••)] (assoc-in!* x•• )))))

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
