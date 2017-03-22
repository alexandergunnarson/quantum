(ns quantum.test.numeric.tensors
  (:require
    [quantum.core.test      :as test
      :refer [deftest is testing]]
    [quantum.numeric.tensors :as ns]
    [quantum.core.collections :as coll
      :refer [map+ join]]))

(defn test:vlength [v])

(defn test:v-op [op v1 v2])

(deftest test:v-+
  (is (= [-3 -3 -3]
         (join (ns/v-+ [1 2 3] [4 5 6])))))

(deftest test:v++
  (is (= [5 7 9]
         (join (ns/v++ [1 2 3] [4 5 6])))))

(deftest test:v-div+
  (is (= [1/4 2/5 1/2]
         (join (ns/v-div+ [1 2 3] [4 5 6])))))

(deftest test:v*+
  (is (= [4 10 18]
         (join (ns/v*+ [1 2 3] [4 5 6])))))

(deftest test:vsq+
  (is (= [16 25 36]
         (join (ns/vsq+ [4 5 6])))))

(defn test:dot-product [v1 v2])

(defn test:vsum [vs])

(defn test:centroid [vs])

(defn test:cosine-similarity [a b])

(defn test:dist* [v1 v2])

(defn test:dist [v1 v2])

#?(:clj
(deftest test:->dmatrix
  (is (= (->> [[1 2 3] [4 5 6]]
              ns/->dmatrix
              (map+ vec)
              (join []))
         (->> (to-array-2d [[1 2 3] [4 5 6]])
              ns/->dmatrix
              (map+ vec)
              (join []))
         [[1.0 2.0 3.0]
          [4.0 5.0 6.0]]))))
