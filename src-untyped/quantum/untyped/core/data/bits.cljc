(ns
  ^{:doc "Useful bit/binary operations, at the primitive (boolean, byte, long, etc.) and
          pre-primitive level."
    :attribution "alexandergunnarson"}
  quantum.untyped.core.data.bits
        (:refer-clojure :exclude
          [and not or, conj contains? disj empty reverse])
        (:require
          [clojure.core              :as core]
  #?(:clj [clojure.future            :as fcore])
          [quantum.untyped.core.core :as ucore]
          [quantum.untyped.core.vars
            :refer [defalias]])
#?(:clj (:import quantum.core.Numeric)))

(ucore/log-this-ns)

;; ===== Bit logic ===== ;;

(defalias not     bit-not)
(defalias and     bit-and)
(defalias and-not bit-and-not)
(defalias or      bit-or)
(defalias xor     bit-xor)
(defalias not!    bit-flip)

;; ===== Bit set operations ===== ;;

(defalias disj    bit-clear)

(def ^:const empty 0)

(defn conj
  ([] empty)
  ([xs] xs)
  ([xs v] (bit-set xs v))
  ([xs v0 v1] (-> xs (conj v0) (conj v1))))

(defalias contains? bit-test)

;; ===== Shifts ===== ;;

(defalias <<  bit-shift-left)
(defalias >>  bit-shift-right)
(defalias >>> unsigned-bit-shift-right)

;; ===== Rotations ===== ;;

(defn rotate-left|long
  {:adapted-from "http://hg.openjdk.java.net/jdk7u/jdk7u6/jdk/file/8c2c5d63a17e/src/share/classes/java/lang/Integer.java"}
  [^long x ^long n]
  (or (<< x n) (>>> x (- n))))

(defn bit-count|int
  "Counts the number of bits set in ->`x`.
   AKA Hamming weight."
  {:adapted-from 'java.lang.Integer/bitCount}
  [x]
  #?(:clj  (java.lang.Integer/bitCount (int x))
     :cljs (let [x (- x (and (>>> x 1) 0x55555555))
                 x (+ (and x 0x33333333) (and (>>> x 2) 0x33333333))
                 x (and (+ x (>>> x 4)) 0x0f0f0f0f)
                 x (+ x (>>> x 8))
                 x (+ x (>>> x 16))]
             (and x 0x3f))))

#?(:clj
(defn bit-count|long
  "Counts the number of bits set in ->`x`.
   AKA Hamming weight."
  [^long x] (Long/bitCount x)))

(declare bits)

;; ===== Bulk bit operations ===== ;;

(defn ?-coll
  "Returns true or false for the bit at the given index of the collection."
  [bits #?(:clj ^long i :cljs i)]
  (contains? (bits (>> i 6)) (and i 0x3f)))

(defn bits
  "The bits of x, aggregated into a vector and truncated/extended to length n."
  {:adapted-from 'gloss.data.primitives}
  [x n]
  (mapv #(if (pos? (and (<< 1 %) x)) 1 0) (range n)))

(defn truncate
  "Truncates x to the specified number of bits."
  {:adapted-from 'bigml.sketchy.murmur}
  [#?(:clj ^long x :cljs x)
   #?(:clj ^long n :cljs n)]
  (and x (unchecked-dec (<< 1 n))))

;; ===== Primitives ===== ;;

#?(:clj  (eval `(defalias ~(if (resolve `fcore/boolean?)
                               `fcore/boolean?
                               `core/boolean?)))
   :cljs (defalias core/boolean?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/double?)
                               `fcore/double?
                               `core/double?)))
   :cljs (defalias core/double?))
