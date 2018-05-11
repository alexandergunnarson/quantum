(def i|>a+b  (t/isa? i.>a+b))
(def i|>a0   (t/isa? i.>a0))
(def i|>a1   (t/isa? i.>a1))
(def i|>b0   (t/isa? i.>b0))
(def i|>b1   (t/isa? i.>b1))
(def i|a     (t/isa? i.a))
(def i|b     (t/isa? i.b))
(def i|<a+b  (t/isa? i.<a+b))
(def i|<a0   (t/isa? i.<a0))
(def i|<a1   (t/isa? i.<a1))
(def i|<b0   (t/isa? i.<b0))
(def i|<b1   (t/isa? i.<b1))
(def i|><0   (t/isa? i.><0))
(def i|><1   (t/isa? i.><1))
(def i|><2   (t/isa? i.><2))

;; ----- Hierarchy within existing non-interfaces ----- ;;

(def >a+b (t/isa? java.util.AbstractCollection))
(def >a   (t/isa? java.util.AbstractList))
(def >b   (t/isa? java.util.AbstractSet))
(def a    (t/isa? java.util.ArrayList))
(def b    (t/isa? java.util.HashSet))
(def <a0  (t/isa? javax.management.AttributeList))
(def <a1  (t/isa? javax.management.relation.RoleList))
(def <b0  (t/isa? java.util.LinkedHashSet))
(def <b1  (t/isa? javax.print.attribute.standard.JobStateReasons))
(def ><0  t/byte?)
(def ><1  t/short?)
(def ><2  t/long?)


;; ===== TO FIX =====

- (testing "AndSpec + ClassSpec • #{<} • Extensible Concrete"
    (test-comparison -1 t/!array-list? (& t/iterable? (t/isa? java.util.RandomAccess))))
- (testing "AndSpec + ClassSpec • #{= <>}"
    (test-comparison  1 t/!array-list? (& t/!array-list? t/java-set?)))
- (testing "AndSpec + ClassSpec • #{> ><}"
    (test-comparison  2 t/!array-list? (& (t/isa? javax.management.AttributeList) t/java-set?)))
- AndSpec + ClassSpec • "#{= <>}" :
    (test-comparison  2 t/!array-list? (& (t/isa? javax.management.AttributeList) t/java-set?))
- test|or :
  (testing "via `not`"
      (is= (| a (! a))
           t/universal-set)
      (is= (| a b (! a))
           t/universal-set)
      (is= (| a b (| (! a) (! b)))
           t/universal-set))
- test|or : and + not + or
        (is= (& (| a b) (! a))
             b)
        ;; TODO fix impl
        (is= (& (! a) (| a b))
             b)
        ;; TODO fix impl
        (is= (& (| a b) (! b) (| b a))
             b)
- (test-comparison 0
    (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
    (& (| t/boolean? t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
       (! t/boolean?)))

TO IMPLEMENT
- (test-comparison ... (! a) (& a (! b)))
- (testing "AndSpec + ClassSpec • #{= >< <>}") ; <- TODO comparison should be 1
- (testing "AndSpec + ClassSpec • #{> <>}") ; <- TODO comparison should be 1
- (testing "AndSpec + ClassSpec • #{>< <>}") ; <- TODO comparison should be 3


(time (clojure.test/test-ns 'quantum.test.core.untyped.type))
668ms -> 3835ms after instrumenting (5.74 times less performant! :/)


"#{> <>}"
(test-comparison 2 (! a) (| b a)) FAIL
(t/compare (! a) (| b a)) -> 3 FAIL
(t/compare (! a) b) -> 1
(t/compare (! a) a) -> 3

"#{> <>}"
(test-comparison 2 a (| <a0 ><0 ><1)) PASS
(t/compare a (| <a0 ><0 ><1)) -> 2 PASS
(@#'t/compare|value-or-not+or a )
(t/compare a <a0) -> 1
(t/compare a ><0) -> 3
(t/compare a ><1) -> 3


(load-file "./test/quantum/test/core/untyped/type.cljc")
(do (require '[orchestra.spec.test :as st])
    (orchestra.spec.test/instrument))
(clojure.test/test-ns 'quantum.test.core.untyped.type)
