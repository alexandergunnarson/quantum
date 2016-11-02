(ns quantum.numeric.constructs
  (:require
    [quantum.core.vars
      :refer        [#?(:clj defalias)]
      :refer-macros [        defalias]]
    [quantum.core.error :as err
      :refer [TODO]]))

; ===== CONSTRUCTS ===== ;

; org.apache.commons.math3.Field
(defrecord Field [])

; org.apache.commons.math3.FieldElement
(defrecord FieldElement [])

; org.apache.commons.math3.RealFieldElement
(defrecord RealFieldElement []) ; + FieldElement

; Point ≈ Vector
; <org.apache.commons.math3.geometry.Space>
(defrecord Space [dimension]) ; incl. Subspace

; Interval-like
; 1D: interval
; 2D: segment
; 3D: ?

; Region-like
; 1D: Set<Interval>
; 2D: Set<Polygon>
; 3D: Set<Polyhedron>

; Oriented hyperplane -like
; 1D: point
; 2D: line
; 3D: plane
; 4D: hyperplane

(defrecord Spheroid [])
(defrecord Ellipsoid [])
(defrecord Arc [])

(defrecord PolynomialFn ; univariate
  [degree representation coefficients])

(defrecord
  ^{:doc "Represents a+bi.
          `real`: a
          `imaginary`: b"}
  Complex
  [real imaginary])

#_(defnt cx [^number? real ^number? imaginary] (Complex. real imaginary))
(defn cx [real imaginary] (Complex. real imaginary))

; ===== CONSTANTS ===== ;

(defrecord Inf [])

; <org.apache.commons.math3.complex>

(def ^:const inf (Inf.))

(def i     ^{:doc "The square root of -1" :const true} (cx 0   1  ))
(def iinf  ^{:doc "Represents +∞ + ∞i"    :const true} (cx inf inf))
(def ione  ^{:doc "1 + 0i"                :const true} (cx 1   0  ))
(def izero ^{:doc "0 + 0i"                :const true} (cx 0   0  ))

; <org.apache.commons.math3.complex.ComplexField>
#_(defrecord ComplexField []
  Field (*ident [this] (TODO))
        (+ident [this] (TODO)))

; TODO formatting/display of complex numbers

; <org.apache.commons.math3.complex.Quaternion>

; <org.apache.commons.math3.complex.RootsOfUnity>
