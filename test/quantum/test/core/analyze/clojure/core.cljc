(ns quantum.test.core.analyze.clojure.core
  (:require
    [quantum.core.analyze.clojure.core :as ns]
    [quantum.core.test
      :refer [deftest is testing]]))

#?(:clj
(deftest test:jvm-typeof
  (is (= Long/TYPE
         (ns/jvm-typeof '(let [a 1 b 4] (+ a b)))))
  ; something which `typeof*` has trouble with
  (is (= (Class/forName "[B")
         (ns/jvm-typeof '(let [x (identity nil)]
                           (-> (java.nio.ByteBuffer/allocate 8)
                               (.putLong (long x))
                               .array)))))))
