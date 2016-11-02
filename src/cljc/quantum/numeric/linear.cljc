(ns quantum.numeric.linear
  (:refer-clojure :exclude [identity])
  (:require
    [clojure.core.matrix.linear :as mat]
    [quantum.core.vars
      :refer        [#?(:clj defalias)]
      :refer-macros [        defalias]]
    [quantum.core.error :as err
      :refer [TODO]]))

; TO EXPLORE
; - https://github.com/scalanlp/breeze/wiki/Linear-Algebra-Cheat-Sheet
;   - Left off at Reading and writing Matrices: Indexing and Slicing
; - http://www.javadoc.io/doc/com.googlecode.matrix-toolkits-java/mtj/1.0.4
; ============================

; ===== CREATION ===== ;

(defn zeros-d
  "[n m]
   Zeroed matrix
   Breeze: DenseMatrix.zeros[Double](n,m)
   Matlab: zeros(n,m)
   numpy:  zeros((n,m))
   R:      mat.or.vec(n, m)

   [n]
   Zeroed vector
   Breeze: DenseVector.zeros[Double](n)
   Matlab: zeros(n,1)
   numpy:  zeros(n)
   R:      mat.or.vec(n, 1)"
 ([n m] (TODO))
 ([n] (TODO)))

(defn of-d
  "[n v]
   Vector of particular number
   Breeze: DenseVector.fill(n){5.0}
   Matlab: ones(n,1) * 5
   numpy:  ones(n) * 5
   R:      (mat.or.vec(5, 1) + 1) * 5

   Vector of ones
   Breeze: DenseVector.ones[Double](n)
   Matlab: ones(n,1)
   numpy:  ones(n)
   R:      mat.or.vec(n, 1) + 1"
  ([n m v] (TODO))
  ([n v]
    (TODO)
    #_(if (== v 1)
        ; DenseVector.ones[Double](n)
        ; DenseVector.fill(n){v}
        )))

(defn iterate-d
  "Range given stepsize
   Breeze: DenseVector.range(from,to,step) or Vector.rangeD(from,to,step)
   R:      seq(from,to,step)"
  [from to step]
  (TODO))

(defn linspace-d
  "n element range
   Breeze: linspace(start,stop,numvals)
   Matlab: linspace(0,20,15)"
  [from to numv]
  (TODO))

(defn identity
  "Identity matrix
   Breeze: DenseMatrix.eye[Double](n)
   Matlab: eye(n)
   numpy:  eye(n)
   R:      identity(n)"
  [n]
  (TODO))

(defn diag
  "Diagonal matrix, given a vector
   Breeze: diag(DenseVector(1.0,2.0,3.0))
   Matlab: diag([1 2 3])
   numpy:  diag((1,2,3))
   R:      diag(c(1,2,3))"
 [v]
 (TODO))

(defn ->matrix
  "[& vs]
   Matrix inline creation
   Breeze: DenseMatrix((1.0,2.0), (3.0,4.0))
   Matlab: [1 2; 3 4]
   numpy:  array([ [1,2], [3,4] ])
   R:      matrix(c(1,2,3,4), nrow = 2, ncol = 2)"
  [& vs]
  (TODO))

(defn vec->matrix
  "Matrix creation from array
   Breeze: new DenseMatrix(2, 3, Array(11, 12, 13, 21, 22, 23))"
  [n m v]
  (TODO))

(defn ->vec
  "[x] | [& xs]
   Column vector inline creation
   Breeze: DenseVector(1,2,3,4)
   Matlab: [1 2 3 4]
   numpy:  array([1,2,3,4])
   R:      c(1,2,3,4)

   [x]
   Vector creation from array
   new DenseVector(Array(1, 2, 3, 4))"
 ([x] (TODO) (if (#_seqable? x)
          (TODO)
          (TODO)))
 ([x & xs] (TODO)))

(defn ->rows
  "Row vector inline creation
   Breeze: DenseVector(1,2,3,4).t
   Matlab: [1 2 3 4]'
   numpy:  array([1,2,3]).reshape(-1,1)
   R:      t(c(1,2,3,4))"
  [& xs]
  (TODO))

(defn ->vecf
  "Vector from function
   Breeze: DenseVector.tabulate(3){i => 2*i}"
  [n f]
  (TODO))

(defn ->matrixf
  "Matrix from function
   Breeze: DenseMatrix.tabulate(3, 2){case (i, j) => i+j}"
  [n m f]
  (TODO))

(defn ->rand-vec
  "Vector of n random elements from 0 to 1
   Breeze: DenseVector.rand(4)
   R:      runif(4) (requires stats library)"
  [n]
  (TODO))

(defn ->rand-matrix
  "nxm matrix of random elements from 0 to 1
   Brreze: DenseMatrix.rand(2, 3)
   R:      matrix(runif(6),2) (requires stats library)"
  [n m]
  (TODO))

; ===== PREDICATES ===== ;

; PACKING ;

(defn dense?  [m] (TODO))
(defn packed? [m] (TODO))
(defn banded? [m] (TODO))

; SYMMETRICITY/TRIANGULARITY/DIAGONALITY/SHAPE ;

(defn symmetric?        [m] (TODO))
(defn lower-symmetric?  [m] (TODO))
(defn upper-symmetric?  [m] (TODO))

(defn triangular?       [m] (TODO))
(defn lower-triangular? [m] (TODO))
(defn upper-triangular? [m] (TODO))

(defn tridiagonal?      [m] (TODO))
; TODO others

(defn unit-lower?       [m] (TODO))
(defn unit-upper?       [m] (TODO))

(defn square?           [m] (TODO))

; DEFINITIVITY ;

(defn positive-definite? [m] (TODO))
; TODO others

; ===== FEATURES ===== ;

(defalias norm          mat/norm)
(defalias rank          mat/rank)

(defn dims [m] (TODO))

; ===== DECOMPOSITIONS ===== ;

; LU ;

#_"Computes the Cholesky decomposition of a hermitian, positive definite matrix."
(defalias cholesky      mat/cholesky)

#_"Computes the LU(P) decomposition of a matrix with partial row pivoting."
(defalias lu            mat/lu)

; ORTHOGONAL ;

(defn lq
  "Computes the LQ decomposition of a matrix."
  [m] (TODO))

(defn ql
  "Computes the QL decomposition of a matrix."
  [m] (TODO))

#_"QR decomposition of a full rank matrix."
(defalias qr            mat/qr)

(defn rq
  "Computes the RQ decomposition of a matrix."
  [m] (TODO))

(defn givens-rotation
  "Givens plane rotation"
  [x] (TODO))

(defn qrp
  "Computes QR decompositions with column pivoting:
   A*P = Q*R where A(m,n), Q(m,m), and R(m,n), more generally:
   A*P = [Q1 Q2] * [R11, R12; 0 R22] and R22 elements are negligible."
  [m] (TODO))

; SPECTRAL ;

#_"Computes the eigenvalue decomposition of a diagonalizable matrix."
(defalias evd           mat/eigen)

#_"Computes the Singular Value decomposition of a matrix."
(defalias svd           mat/svd)

; ===== SOLUTIONS ===== ;

#_"Solves a linear matrix equation, i.e., X such that A.X = B"
(defalias solve         mat/solve)
#_"Computes least-squares solution to a linear matrix equation."
(defalias least-squares mat/least-squares)

(defn eigenvalues
  "Computes the eigenvalues of a matrix."
  [m] (TODO))


; <org.apache.commons.math3.filter>
; Implementations of common discrete-time linear filters.
