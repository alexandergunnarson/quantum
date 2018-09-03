(ns quantum.untyped.core.data.array
          (:refer-clojure :exclude
            [array array?])
          (:require
            [clojure.core               :as core]
            [quantum.untyped.core.loops :as uloop])
  #?(:clj (:import
            [quantum.core.data Array])))

(defn array? [x]
  #?(:clj  (-> x class .isArray) ; must be reflective
     :cljs (core/array? x)))

(defn ^"[Ljava.lang.Object;" *<>
  ([]
    #?(:clj  (Array/newUninitialized1dObjectArray 0)
       :cljs #js []))
  ([x0]
    #?(:clj  (Array/new1dObjectArray x0)
       :cljs #js                    [x0]))
  ([x0 x1]
    #?(:clj  (Array/new1dObjectArray x0 x1)
       :cljs #js                    [x0 x1]))
  ([x0 x1 x2]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2)
       :cljs #js                    [x0 x1 x2]))
  ([x0 x1 x2 x3]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3)
       :cljs #js                    [x0 x1 x2 x3]))
  ([x0 x1 x2 x3 x4]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3 x4)
       :cljs #js                    [x0 x1 x2 x3 x4]))
  ([x0 x1 x2 x3 x4 x5]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3 x4 x5)
       :cljs #js                    [x0 x1 x2 x3 x4 x5]))
  ([x0 x1 x2 x3 x4 x5 x6]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3 x4 x5 x6)
       :cljs #js                    [x0 x1 x2 x3 x4 x5 x6]))
  ([x0 x1 x2 x3 x4 x5 x6 x7]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3 x4 x5 x6 x7)
       :cljs #js                    [x0 x1 x2 x3 x4 x5 x6 x7]))
  ([x0 x1 x2 x3 x4 x5 x6 x7 x8]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3 x4 x5 x6 x7 x8)
       :cljs #js                    [x0 x1 x2 x3 x4 x5 x6 x7 x8]))
  ([x0 x1 x2 x3 x4 x5 x6 x7 x8 x9]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3 x4 x5 x6 x7 x8 x9)
       :cljs #js                    [x0 x1 x2 x3 x4 x5 x6 x7 x8 x9]))
  ([x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10]
    #?(:clj  (Array/new1dObjectArray x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10)
       :cljs #js                    [x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10]))
  ([x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10 & xs]
    #?(:clj  (let [arr (Array/newUninitialized1dObjectArray (+ 11 (count xs)))]
               (Array/set arr x0  0)
               (Array/set arr x1  1)
               (Array/set arr x2  2)
               (Array/set arr x3  3)
               (Array/set arr x4  4)
               (Array/set arr x5  5)
               (Array/set arr x6  6)
               (Array/set arr x7  7)
               (Array/set arr x8  8)
               (Array/set arr x9  9)
               (Array/set arr x10 10)
               (uloop/doseqi [x xs i] (doto arr (Array/set x (+ 11 i)))))
       :cljs (let [arr #js [x0 x1 x2 x3 x4 x5 x6 x7 x8 x9 x10]]
               (uloop/doseq  [x xs]   (doto arr (.push x)))))))
