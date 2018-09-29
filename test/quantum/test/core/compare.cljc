(ns quantum.test.core.compare
  (:require
    [quantum.core.compare :as ns]
    [quantum.core.test
      :refer [deftest is testing]]))

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
