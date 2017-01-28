(ns quantum.numeric.linear
  (:refer-clojure :exclude [identity get subvec last])
  (:require
    [clojure.core.matrix.linear :as mat]
    [quantum.core.log :as log]
    [quantum.core.vars
      :refer [defalias]]
    [quantum.core.error :as err
      :refer [TODO]]))

(log/this-ns)

; TO EXPLORE
; - http://www.javadoc.io/doc/com.googlecode.matrix-toolkits-java/mtj/1.0.4
; - For breeze:
;   - Vectors and matrices over types other than Double, Float and Int are boxed,
;     so they will typically be a lot slower.

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
  {:implemented-by '#{smile.math.matrix.Matrix}}
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
   Breeze: DenseMatrix.rand(2, 3)
   R:      matrix(runif(6),2) (requires stats library)"
  [n m]
  (TODO))

; Operation Breeze  Matlab  Numpy R
; Reshaping a.reshape(3, 2) reshape(a, 3, 2)  a.reshape(3,2)  matrix(a,nrow=3,byrow=T)
; Flatten matrix  a.toDenseVector (Makes copy)  a(:)  a.flatten() as.vector(a)
; Copy lower triangle lowerTriangular(a)  tril(a) tril(a) a[upper.tri(a)] <- 0
; Copy upper triangle upperTriangular(a)  triu(a) triu(a) a[lower.tri(a)] <- 0
; Copy (note, no parens!!)  a.copy    np.copy(a)
; Create view of matrix diagonal  diag(a) NA  diagonal(a) (Numpy >= 1.9)  diag(a)
; Vector Assignment to subset a(1 to 4) := 5.0  a(2:5) = 5  a[1:5] = 5  a[2:5] = 5
; Vector Assignment to subset a(1 to 4) := DenseVector(1.0,2.0,3.0,4.0) a(2:5) = [1 2 3 4]  a[1:5] = array([1,2,3,4]) a[2:5] = c(1,2,3,4)
; Matrix Assignment to subset a(1 to 3,1 to 3) := 5.0 a(2:4,2:4) = 5  a[1:4,1:4] = 5  a[2:4,2:4] = 5
; Matrix Assignment to column a(::, 2) := 5.0 a(:,3) = 5  a[:,2] = 5  a[,3] = 5
; Matrix vertical concatenate DenseMatrix.vertcat(a,b)  [a ; b] vstack((a,b)) rbind(a, b)
; Matrix horizontal concatenate DenseMatrix.horzcat(d,e)  [d , e] hstack((d,e)) cbind(d, e)
; Vector concatenate  DenseVector.vertcat(a,b)  [a b] concatenate((a,b))  c(a, b)



; Operation Breeze  Matlab  Numpy R
; Elementwise sum sum(a)  sum(sum(a)) a.sum() sum(a)
; Sum down each column (giving a row vector)  sum(a, Axis._0) or sum(a(::, *))  sum(a)  sum(a,0)  apply(a,2,sum)
; Sum across each row (giving a column vector)  sum(a, Axis._1) or sum(a(*, ::))  sum(a') sum(a,1)  apply(a,1,sum)
; Trace (sum of diagonal elements)  trace(a)  trace(a)  a.trace() sum(diag(a))
; Cumulative sum  accumulate(a) cumsum(a) a.cumsum()  apply(a,2,cumsum)




; Operation Breeze  Matlab  Numpy R
; Linear solve  a \ b a \ b linalg.solve(a,b) solve(a,b)
; Transpose a.t a'  a.conj.transpose()  t(a)
; Determinant det(a)  det(a)  linalg.det(a) det(a)
; Inverse inv(a)  inv(a)  linalg.inv(a) solve(a)
; Moore-Penrose Pseudoinverse pinv(a) pinv(a) linalg.pinv(a)
; Vector Frobenius Norm norm(a) norm(a) norm(a)
; Eigenvalues (Symmetric) eigSym(a) [v,l] = eig(a)  linalg.eig(a)[0]
; Eigenvalues val (er, ei, _) = eig(a) (separate real & imaginary part) eig(a)  linalg.eig(a)[0]  eigen(a)$values
; Eigenvectors  eig(a)._3 [v,l] = eig(a)  linalg.eig(a)[1]  eigen(a)$vectors
; Singular Value Decomposition  val svd.SVD(u,s,v) = svd(a) svd(a)  linalg.svd(a) svd(a)$d
; Rank  rank(a) rank(a) rank(a) rank(a)
; Vector length a.length  size(a) a.size  length(a)
; Matrix rows a.rows  size(a,1) a.shape[0]  nrow(a)
; Matrix columns  a.cols  size(a,2) a.shape[1]  ncol(a)


; Complex numbers ;
; If you make use of complex numbers, include a breeze.math._ import
; This declares a i variable, and provides implicit conversions from Scala’s
; basic types to complex types.

; (see https://github.com/scalanlp/breeze/wiki/Linear-Algebra-Cheat-Sheet)



; ===== MAPPING/REDUCTION OPERATIONS ===== ;

; https://github.com/scalanlp/breeze/wiki/Linear-Algebra-Cheat-Sheet
; Breeze provides a number of built in reduction functions such as sum, mean.
; You can implement a custom reduction using the higher order function reduce.

; ===== PREDICATES ===== ;

; PACKING ;

(defn dense?  [m] (TODO))
(defn packed? [m] (TODO))
(defn banded? [m] (TODO))

; SYMMETRICITY/TRIANGULARITY/DIAGONALITY/SHAPE ;

(defn symmetric?
  {:implemented-by '#{smile.math.matrix.Matrix}}
  [m] (TODO))
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

(defn singular?
  {:implemented-by '#{smile.math.matrix.QRDecomposition
                      smile.math.matrix.SingularValueDecomposition}}
  [m] (TODO))

(defn full-column-rank?
  {:implemented-by '#{smile.math.matrix.QRDecomposition}}
  [m] (TODO))

; DEFINITIVITY ;

(defn positive-definite?
  {:implemented-by '#{smile.math.matrix.Matrix}}
  [m] (TODO))
; TODO others

; ===== FEATURES ===== ;

(defalias norm          mat/norm)
(defalias rank          mat/rank)

; TODO use as an implementation for core.matrix
(defn rank-
  {:implemented-by '#{smile.math.matrix.Matrix
                      smile.math.matrix.SingularValueDecomposition}}
  [m] (TODO))

(defn det
  "Determinant"
  {:implemented-by '#{smile.math.matrix.Matrix
                      smile.math.matrix.LUDecomposition
                      smile.math.matrix.CholeskyDecomposition}}
  [m] (TODO))

(defn dims
  {:implemented-by '#{smile.math.matrix.Matrix}}
  [m] (TODO))

(defn inv
  "Inverse"
  {:implemented-by '#{smile.math.matrix.LUDecomposition
                      smile.math.matrix.CholeskyDecomposition
                      smile.math.matrix.QRDecomposition}}
  [m] (TODO))

; ===== DECOMPOSITIONS ===== ;

; LU ;

#_"Computes the Cholesky decomposition of a hermitian, positive definite matrix."
(defalias cholesky      mat/cholesky)

; TODO use as an implementation for core.matrix
(defn cholesky-
  "Returns:
   {; lower triangular factor
    :l ...}"
  {:implemented-by '#{smile.math.matrix.CholeskyDecomposition
                      smile.math.matrix.Matrix}}
  [?] (TODO))

#_"Computes the LU(P) decomposition of a matrix with partial row pivoting."
(defalias lu            mat/lu)

; TODO use as an implementation for core.matrix
(defn lu-
  "Returns, lazily computed:
   {; lower triangular factor
    :l ...
    ; upper triangular factor
    :u ...
    ; pivot permutation factor
    :pivot ...}"
  {:implemented-by '#{smile.math.matrix.LUDecomposition
                      smile.math.matrix.Matrix}}
  [?] (TODO))

; ORTHOGONAL ;

(defn lq
  "Computes the LQ decomposition of a matrix."
  [m] (TODO))

(defn ql
  "Computes the QL decomposition of a matrix."
  [m] (TODO))

#_"QR decomposition of a full rank matrix."
(defalias qr            mat/qr)

; TODO use as an implementation for core.matrix
(defn qr-
  "Returns, lazily computed:
   {; orthogonal factor
    :q ...
    ; upper triangular factor
    :r ...}"
  {:implemented-by '#{smile.math.matrix.QRDecomposition
                      smile.math.matrix.Matrix}}
  [?] (TODO))

(defn update-qr!
  "Rank-1 update of the QR decomposition for A = A + u * v.
   Instead of a full decomposition from scratch in O(N^3),
   we can update the QR factorization in O(N^2)."
  {:implemented-by '#{smile.math.matrix.QRDecomposition}}
  [?] (TODO))

(defn householders
  "The Householder vectors:Lower trapezoidal matrix whose
   columns define the reflections."
  {:implemented-by '#{smile.math.matrix.QRDecomposition}}
  [?] (TODO))

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

; TODO use as an implementation for core.matrix
(defn evd-
  "Returns
   {:eigenvectors ...
    :eigenvalues  ...
    :real-eigenvalues ...
    :imaginary-eigenvalues ...
    ; the block diagonal eigenvalue matrix whose diagonal are the real
    ; part of eigenvalues, lower subdiagonal are positive imaginary parts, and
    ; upper subdiagonal are negative imaginary parts.
    :d ...
    }"
  {:implemented-by '#{smile.math.matrix.EigenValueDecomposition
                      smile.math.matrix.Matrix}}
  [?] (TODO))

(defn page-rank
  "Calculates the page rank vector."
  {:implemented-by '#{smile.math.matrix.EigenValueDecomposition}}
  [?] (TODO))

#_"Computes the Singular Value decomposition of a matrix."
(defalias svd           mat/svd)

; TODO use as an implementation for core.matrix
(defn svd-
  "Returns:
   {; The left singular vectors
    :u ...
    ; The right singular vectors
    :v ...
    ; The one-dimensional array of singular values, ordered by
    ; from largest to smallest.
    :s ...
    ; Lazily computed. The diagonal matrix of singular values
    :S ...
    ; Lazily computed. Returns the dimension of null space.
    ; The number of negligible singular values.
    :nullity ...
    ; Lazily computed. Returns a matrix of which columns give
    ; an orthonormal basis for the range space.
    :range-space ...
    ; Lazily computed. Returns a matrix of which columns give
    ; an orthonormal basis for the null space.
    :null-space ...}"
  {:implemented-by '#{smile.math.matrix.SingularValueDecomposition
                      smile.math.matrix.Matrix}}
  [?] (TODO))

; ===== SOLUTIONS ===== ;

#_"Solves a linear matrix equation, i.e., X such that A.X = B"
(defalias solve         mat/solve)

; TODO use as an implementation for core.matrix
(defn solve-
  {:implemented-by '#{smile.math.matrix.LUDecomposition
                      smile.math.matrix.CholeskyDecomposition
                      smile.math.matrix.SingularValueDecomposition
                      smile.math.matrix.QRDecomposition}}
  [?] (TODO))

#_"Computes least-squares solution to a linear matrix equation."
(defalias least-squares mat/least-squares)

(defn eigenvalues
  "Computes the eigenvalues of a matrix."
  [m] (TODO))


; <org.apache.commons.math3.filter>
; Implementations of common discrete-time linear filters.

; <smile.math.kernel>
; All sorts of kernel operations.
