;; ===== TO FIX =====

- test|or : and + not + or
        (is= (& (! a) (| a b))
             b)

TO IMPLEMENT
- (test-comparison ... (! a) (& a (! b)))
- (testing "AndSpec + ClassSpec • #{= >< <>}") ; <- TODO comparison should be 1
- (testing "AndSpec + ClassSpec • #{> <>}") ; <- TODO comparison should be 1
- (testing "AndSpec + ClassSpec • #{>< <>}") ; <- TODO comparison should be 3


(time (clojure.test/test-ns 'quantum.test.core.untyped.type))
668ms -> 3835ms after instrumenting (5.74 times less performant! :/)
Also certain things take *much* longer:
  - (= (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
       (& (| t/boolean? t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
          (! t/boolean?)))
  - 170ms vs 5.5ms — 30 times less performant!!

(load-file "./test/quantum/test/core/untyped/type.cljc")
(do (require '[orchestra.spec.test :as st])
    (orchestra.spec.test/instrument))
(clojure.test/test-ns 'quantum.test.core.untyped.type)
