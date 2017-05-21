(ns quantum.test.core.compare
  (:require
    [quantum.core.compare :as ns]
    [quantum.core.test
      :refer [deftest is testing]]))

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
  [a b])

(defn test:extreme-comparator [comparator-n])

; ===== APPROXIMATION ===== ;

(defn test:approx=
  [x y eps])

(defn test:within-tolerance? [n total tolerance])

(deftest test:comp-keys-into
  (is (= [0]       (apply ns/comp-keys-into vector   #(ns/<     %1 %2) identity [0 2 3 4 4 3])))
  (is (= [4 4]     (apply ns/comp-keys-into vector   #(ns/>     %1 %2) identity [0 2 3 4 4 3])))
  (is (= #{4}      (apply ns/comp-keys-into hash-set #(ns/>     %1 %2) identity [0 2 3 4 4 3])))
  (is (= ["0"]     (apply ns/comp-keys-into vector   #(ns/comp< %1 %2) identity ["0" "2" "3" "4" "4" "3"])))
  (is (= ["4" "4"] (apply ns/comp-keys-into vector   #(ns/comp> %1 %2) identity ["0" "2" "3" "4" "4" "3"])))
  (is (= #{"4"}    (apply ns/comp-keys-into hash-set #(ns/comp> %1 %2) identity ["0" "2" "3" "4" "4" "3"]))))

(deftest test:reduce-comp-keys
  (is (= [[5 8]]
       #_(ns/reduce-maxes                          {0 2 3 4 5 8 1 1 100 -4})
         (ns/reduce-max-keys                second {0 2 3 4 5 8 1 1 100 -4})
         (ns/reduce-max-keys-into  vector   second {0 2 3 4 5 8 1 1 100 -4})
         (ns/reduce-comp-keys             > second {0 2 3 4 5 8 1 1 100 -4})
         (ns/reduce-comp-keys-into vector > second {0 2 3 4 5 8 1 1 100 -4}))))
