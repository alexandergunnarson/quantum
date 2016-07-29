(ns quantum.test.core.compare
  (:require [quantum.core.compare :as ns]))

(defn test:=
  ([x])
  ([x y])
  ([x y & more]))

(defn test:not=
  ([x])
  ([x y])
  ([x y & more]))

(defn test:<
  ([x])
  ([x y])
  ([x y & more]))

(defn test:<=
  ([x] )
  ([x y])
  ([x y & more]))

(defn test:>
  ([x])
  ([x y])
  ([x y & more]))

(defn test:>=
  ([x])
  ([x y])
  ([x y & more]))

(defn test:max
  ([x])
  ([x y])
  ([x y & more]))

(defn test:min
  ([x])
  ([x y])
  ([x y & more]))

(defn test:min-key
  ([k x])
  ([k x y])
  ([k x y & more]))

(defn test:max-key
  ([k x])
  ([k x y])
  ([k x y & more]))

(defn test:rcompare [x y])
(defn test:least [coll & [?comparator]])
(defn test:greatest [coll & [?comparator]])
(defn test:least-or [a b else])
(defn test:greatest-or [a b else])

(defn test:compare-bytes-lexicographically
  [a b]))

(defn test:extreme-comparator [comparator-n])

; ===== APPROXIMATION ===== ;

(defn test:approx=
  [x y eps])

(defn test:within-tolerance? [n total tolerance])