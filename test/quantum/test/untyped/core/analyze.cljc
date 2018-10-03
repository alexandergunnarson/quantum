(ns quantum.test.untyped.core.analyze
  (:require
    [quantum.test.untyped.core.type :as tt]
    [quantum.untyped.core.analyze   :as self]
    [quantum.untyped.core.test
      :refer [deftest is is= testing]]
    [quantum.untyped.core.type      :as t]))

;; More dependent type tests in `quantum.test.untyped.core.type.defnt` but those are more like
;; integration tests
(deftest dependent-type-test
  (testing "Output type dependent on non-splittable input"
    (testing "Not nested within another type"
      (let [ana (self/analyze-arg-syms {} {} {'x `tt/boolean?} `(t/type ~'x))]
        (is= t/boolean?
             (get-in ana [:env 'x :type]))))
    (testing "Nested within another type"
      (testing "Without arg shadowing"
        (let [ana (self/analyze-arg-syms {'x `tt/boolean?} `(t/or t/number? (t/type ~x)))]
          (is= t/boolean?
               (get-in ana [:env 'x :type])))))))


(quantum.untyped.core.print/ppr
  )
