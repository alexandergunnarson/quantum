(ns quantum.test.core.untyped.type
  (:require
    [clojure.core                      :as core]
    [quantum.core.error                :as err
      :refer [>err]]
    [quantum.core.fn                   :as fn
      :refer [fn-> fn1]]
    [quantum.core.test                 :as test
      :refer [deftest testing is is= throws]]
    [quantum.untyped.core.analyze.ast  :as ast]
    [quantum.untyped.core.analyze.expr :as xp
      :refer [>expr]]
    [quantum.untyped.core.numeric      :as unum]
    [quantum.untyped.core.type         :as t
      :refer [& | !]]))

(is= -1 (t/compare (t/value 1) t/numerically-byte?))

(is= (& t/long? (>expr (fn1 = 1)))
     (t/value 1))

(is= (& (t/value 1) (>expr unum/integer-value?))
     (t/value 1))

(t/compare (t/value 1) (>expr unum/integer-value?))

(is= 0 (t/compare (t/value 1) (>expr (fn1 =|long 1))))
(is= 0 (t/compare (t/value 1) (>expr (fn [^long x] (= x 1)))))
(is= 0 (t/compare (t/value 1) (>expr (fn [^long x] (== x 1)))))
(is= 0 (t/compare (t/value 1) (>expr (fn [x] (core/== (long x) 1)))))
(is= 0 (t/compare (t/value 1) (>expr (fn [x] (= (long x) 1)))))

;; ----- Example interface hierarchy ----- ;;

(do

(gen-interface :name i.>a+b)
(gen-interface :name i.>a0)
(gen-interface :name i.>a1)
(gen-interface :name i.>b0)
(gen-interface :name i.>b1)

(gen-interface :name i.a    :extends [i.>a0 i.>a1 i.>a+b])
(gen-interface :name i.b    :extends [i.>b0 i.>b1 i.>a+b])

(gen-interface :name i.<a+b :extends [i.a i.b])
(gen-interface :name i.<a0  :extends [i.a])
(gen-interface :name i.<a1  :extends [i.a])
(gen-interface :name i.<b0  :extends [i.b])
(gen-interface :name i.<b1  :extends [i.b])

(gen-interface :name i.><0)
(gen-interface :name i.><1)
(gen-interface :name i.><2)

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

)

;; ----- Hierarchy within existing non-interfaces ----- ;;

(do (def >a  (t/isa? java.util.AbstractList))
    (def >b  (t/isa? java.util.AbstractSet))
    (def a   (t/isa? java.util.ArrayList))
    (def b   (t/isa? java.util.HashSet))
    (def <a0 (t/isa? javax.management.AttributeList))
    (def <a1 (t/isa? javax.management.relation.RoleList))
    (def <b0 (t/isa? java.util.LinkedHashSet))
    (def <b1 (t/isa? javax.print.attribute.standard.JobStateReasons))
    (def ><0 t/byte?)
    (def ><1 t/short?)
    (def ><2 t/long?))

(def Uc (t/isa? t/universal-class))

;; ----- Example protocols ----- ;;

(do

(defprotocol AProtocolAll (a-protocol-all [this]))

(extend-protocol AProtocolAll
  nil    (a-protocol-all [this])
  Object (a-protocol-all [this]))

(defprotocol AProtocolString (a-protocol-string [this]))

(extend-protocol AProtocolString
  java.lang.String (a-protocol-string [this]))

(defprotocol AProtocolNonNil (a-protocol-non-nil [this]))

(extend-protocol AProtocolNonNil
  Object (a-protocol-non-nil [this]))

(defprotocol AProtocolOnlyNil (a-protocol-only-nil [this]))

(extend-protocol AProtocolOnlyNil
  nil (a-protocol-only-nil [this]))

(defprotocol AProtocolNone (a-protocol-none [this]))

(def protocol-specs
  (->> [AProtocolAll AProtocolString AProtocolNonNil AProtocolOnlyNil AProtocolNone]
       (map t/>spec) set))

)

;; TESTS ;;

(defn test-symmetric
  "Performs a `t/compare` on `a` and `b`, ensuring that their relationship is symmetric.
   The basis comparison is the first argument."
  [c a b]
  (is= c             (t/compare a b))
  (is= (t/inverse c) (t/compare b a)))

(deftest test|in|compare
  (testing "UniversalSetSpec"
    (testing "+ UniversalSetSpec"
      (is=  0 (t/compare t/universal-set t/universal-set)))
    (testing "+ NullSetSpec"
      (test-symmetric 1 t/universal-set t/null-set))
    (testing "+ NotSpec"
      (test-symmetric -1 (! t/universal-set) t/universal-set)  ; inner =
      (test-symmetric  0 (! t/null-set)      t/universal-set)) ; inner <
    (testing "+ OrSpec"
      (test-symmetric  1 t/universal-set (| ><0 ><1)))
    (testing "+ AndSpec")
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec"
      (doseq [spec protocol-specs]
        (is=  1 (t/compare t/universal-set spec))
        (is= -1 (t/compare spec t/universal-set))))
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"
      (doseq [spec [(t/value t/universal-set)
                    (t/value t/null-set)
                    (t/value 0)
                    (t/value nil)]]
        (is= 1 (t/compare t/universal-set spec)))))
  ;; The null set is considered to always (vacuously) be a subset of any set
  (testing "NullSetSpec"
    (testing "+ NullSetSpec"
      (is=  0 (t/compare t/null-set t/null-set)))
    (testing "+ NotSpec"
      (testing "Inner ClassSpec"
        (is= -1 (t/compare t/null-set (! a))))
      (testing "Inner ValueSpec"
        (is= -1 (t/compare t/null-set (! (t/value 1))))))
    (testing "+ OrSpec"
      (test-symmetric -1 t/null-set (| ><0 ><1)))
    (testing "+ AndSpec")
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec"
      (doseq [spec protocol-specs]
        (test-symmetric -1 t/null-set spec)))
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"
      (test-symmetric -1 t/null-set (t/value t/null-set))
      (test-symmetric -1 t/null-set (t/value 0))))
  (testing "NotSpec"
    (testing "+ NotSpec"
      (is=  0 (t/compare (! a)           (! a)))
      (test-symmetric  2 (! a)           (! b))
      (test-symmetric  2 (! i|a)         (! i|b))
      (test-symmetric  2 (! t/string?)   (! t/byte?))
      (test-symmetric  1 (! a)           (! >a))
      (test-symmetric -1 (! a)           (! <a0))
      (test-symmetric  0 (! (t/value 1)) (! (t/value 1)))
      (test-symmetric  2 (! (t/value 1)) (! (t/value 2))))
    (testing "+ OrSpec"
      (test-symmetric  2 (! ><0) (| ><0 ><1))  ; TODO fix impl
      (test-symmetric  2 (! ><1) (| ><0 ><1))) ; TODO fix impl
    (testing "+ AndSpec")
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    (testing "+ ClassSpec"
      (test-symmetric  3 (!     a)     a) ; inner =
      (test-symmetric  3 (!   i|a)   i|a) ; inner =
      (test-symmetric  3 (!     a)   <a0) ; inner >
      (test-symmetric  3 (!   i|a) i|<a0) ; inner >
      (test-symmetric  2 (!     a)    >a) ; inner <
      (test-symmetric  2 (!   i|a) i|>a0) ; inner ><
      (test-symmetric  1 (!   a  )   ><0) ; inner <>
      (test-symmetric  2 (!   i|a) i|><0) ; inner ><
      (test-symmetric  2 (!     a)    Uc) ; inner <
      (test-symmetric  2 (!   i|a)    Uc) ; inner <
      (test-symmetric  2 (!   <a0)     a) ; inner <
      (test-symmetric  2 (! i|<a0)   i|a) ; inner <
      (test-symmetric  2 (!   <a0)    >a) ; inner <
      (test-symmetric  2 (! i|<a0) i|>a0) ; inner <
      (test-symmetric  1 (!   <a0)   ><0) ; inner <>
      (test-symmetric  2 (! i|<a0) i|><0) ; inner ><
      (test-symmetric  2 (!   <a0)    Uc) ; inner <
      (test-symmetric  2 (! i|<a0)    Uc) ; inner <
      (test-symmetric  3 (!    >a)     a) ; inner >
      (test-symmetric  3 (! i|>a0)   i|a) ; inner >
      (test-symmetric  3 (!    >a)   <a0) ; inner >
      (test-symmetric  3 (! i|>a0) i|<a0) ; inner >
      (test-symmetric  1 (!    >a)   ><0) ; inner <>
      (test-symmetric  2 (! i|>a0) i|><0) ; inner ><
      (test-symmetric  2 (!    >a)    Uc) ; inner <
      (test-symmetric  2 (! i|>a0)    Uc) ; inner <
      (test-symmetric  1 (!   ><0)     a) ; inner <>
      (test-symmetric  2 (! i|><0)   i|a) ; inner ><
      (test-symmetric  1 (!   ><0)   <a0) ; inner <>
      (test-symmetric  2 (! i|><0) i|<a0) ; inner ><
      (test-symmetric  1 (!   ><0)    >a) ; inner <>
      (test-symmetric  2 (! i|><0) i|>a0) ; inner ><
      (test-symmetric  2 (!   ><0)    Uc) ; inner <
      (test-symmetric  2 (! i|><0)    Uc)) ; inner <
    (testing "+ ValueSpec"
      (test-symmetric -1 (t/value 1)  (! (t/value 2)))
      (test-symmetric  3 (t/value "") (! t/string?))))
  ;; TODO fix tests
  (testing "OrSpec"
    (testing "+ OrSpec"
      ;; (let [l <all -1 on left-compare?>
      ;;       r <all -1 on right-compare?>]
      ;;   (if l
      ;;       (if r 0 -1)
      ;;       (if r 1  3)))
      ;;
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      (testing "#{= <+} -> #{<+}"
        (testing "+ #{<+}"
          ;; comparisons: [-1, -1], [-1, -1]
          (is=  0 (t/compare (| i|a i|>a+b i|>a0)       (| i|>a+b i|>a0)))
          ;; comparisons: [-1, -1, 3], [-1, -1]
          (is=  1 (t/compare (| i|a i|>a+b i|>a0 i|>a1) (| i|>a+b i|>a0)))
          ;; comparisons: [-1, -1], [-1, -1, 3]
          (is= -1 (t/compare (| a >a+b >a0)     (| >a+b >a0 >a1)))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (is=  0 (t/compare (| a >a+b >a0 >a1) (| >a+b >a0 >a1))))
        (testing "+ #{∅+}"
          ;; comparisons: [3, 3, 3], [3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (| ><0 ><1))))
        (testing "+ #{<+ ∅+}"
          ;; comparisons: [-1, 3], [-1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (| >a+b         ><0 ><1)))
          ;; comparisons: [-1, 3, 3], [-1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (| >a+b         ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3]
          (is= -1 (t/compare (| a >a+b >a0)     (| >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1, 3], [-1, -1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (| >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3, 3]
          (is= -1 (t/compare (| a >a+b >a0)     (| >a+b >a0 >a1 ><0 ><1)))
          ;; comparisons: [-1, -1, 1], [-1, -1, -1, 3, 3]
          (is= -1 (t/compare (| a >a+b >a0 >a1) (| >a+b >a0 >a1 ><0 ><1))))
        (testing "+ #{= ∅+}"
          ;; comparisons: [3, 3], [-1, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (| a ><0)))
          ;; comparisons: [3, 3], [-1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (| a ><0 ><1))))
        (testing "+ #{>+ ∅+}"
          ;; comparisons: [3, 3], [-1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (| <a+b         ><0 ><1)))
          ;; comparisons: [3, 3, 3], [-1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (| <a+b         ><0 ><1)))
          ;; comparisons: [3, 3], [-1, -1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (| <a+b <a0     ><0 ><1)))
          ;; comparisons: [3, 3, 3], [-1, -1, 3 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (| <a+b <a0     ><0 ><1)))
          ;; comparisons: [3, 3], [-1, -1, 3, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (| <a+b <a0 <a1 ><0 ><1)))
          ;; comparisons: [3, 3, 3], [-1, -1, -1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (| <a+b <a0 <a1 ><0 ><1)))))
      (testing "#{= ∅+}"
        (testing "+ #{<+}"
          ;; comparisons: [-1, 3], [3, 3]
          (is=  3 (t/compare (| a ><0)           (| >a+b >a0)))
          ;; comparisons: [-1, 3, 3], [3, 3]
          (is=  3 (t/compare (| a ><0 ><1)        (| >a+b >a0)))
          ;; comparisons: [-1, 3], [3, 3, 3]
          (is=  3 (t/compare (| a ><0)           (| >a+b >a0 >a1)))
          ;; comparisons: [-1, 3, 3], [3, 3, 3]
          (is=  3 (t/compare (| a ><0 ><1)        (| >a+b >a0 >a1))))
        (testing "+ #{∅+}"
          ;; comparisons: [3, -1], [-1, 3]
          (is=  3 (t/compare (| a ><0)     (| ><0 ><1)))
          ;; comparisons: [3, -1, -1], [-1, -1]
          (is=  1 (t/compare (| a ><0 ><1)  (| ><0 ><1)))
          ;; comparisons: [3, 3], [3, 3]
          (is=  3 (t/compare (| a ><2)     (| ><0 ><1)))
          ;; comparisons: [3, 3, -1], [3, -1]
          (is=  3 (t/compare (| a ><2 ><1)  (| ><0 ><1)))
          ;; comparisons: [3, 3], [3, 3]
          (is=  3 (t/compare (| a ><0)     (| ><1 ><2)))
          ;; comparisons: [3, 3, -1], [-1, 3]
          (is=  3 (t/compare (| a ><0 ><1)  (| ><1 ><2))))
        (testing "+ #{<+ ∅+}")  ;; TODO flesh out (?)
        (testing "+ #{= ∅+}")   ;; TODO flesh out (?)
        (testing "+ #{>+ ∅+}")) ;; TODO flesh out (?)

      (testing "#{<+ ∅+} -> <")
      (testing "#{= ∅+} -> <")
      (testing "#{>+ ∅+} -> ∅"))
    ;; TODO fix tests
    (testing "+ AndSpec"
      ;; (if <all -1 on right-compare?> 1 3)
      ;;
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      (testing "#{= <+} -> #{<+}"
        (testing "+ #{<+}"
          ;; comparisons: [-1, -1], [-1, -1]
          (is=  1 (t/compare (| a >a+b >a0)     (& >a+b >a0)))
          ;; comparisons: [-1, -1, 3], [-1, -1]
          (is=  1 (t/compare (| a >a+b >a0 >a1) (& >a+b >a0)))
          ;; comparisons: [-1, -1], [-1, -1, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (& >a+b >a0 >a1)))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (is=  1 (t/compare (| a >a+b >a0 >a1) (& >a+b >a0 >a1))))
        (testing "+ #{∅+}"
          ;; comparisons: [3, 3, 3], [3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (& ><0 ><1))))
        (testing "+ #{<+ ∅+}"
          ;; comparisons: [-1, 3], [-1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)    (& >a+b         ><0 ><1)))
          ;; comparisons: [-1, 3, 3], [-1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (& >a+b         ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (& >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1, 3], [-1, -1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (& >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0)     (& >a+b >a0 >a1 ><0 ><1)))
          ;; comparisons: [-1, -1, -], [-1, -1, -1, 3, 3]
          (is=  3 (t/compare (| a >a+b >a0 >a1) (& >a+b >a0 >a1 ><0 ><1))))
        (testing "+ #{= ∅+}"
          ;; comparisons: [3, 3], [-1, 3]
          (is= 3 (t/compare (| a >a+b >a0)     (& a ><0)))
          ;; comparisons: [3, 3], [-1, 3, 3]
          (is= 3 (t/compare (| a >a+b >a0)     (& a ><0 ><1))))
        (testing "+ #{>+ ∅+}"
          ;; comparisons: [3, 3], [-1, 3, 3]
          (is= 3 (t/compare (| a >a+b >a0)     (& <a+b         ><0 ><1)))
          ;; comparisons: [3, 3, 3], [-1, 3, 3]
          (is= 3 (t/compare (| a >a+b >a0 >a1) (& <a+b         ><0 ><1)))
          ;; comparisons: [3, 3], [-1, -1, 3, 3]
          (is= 3 (t/compare (| a >a+b >a0)     (& <a+b <a0     ><0 ><1)))
          ;; comparisons: [3, 3, 3], [-1, -1, 3, 3]
          (is= 3 (t/compare (| a >a+b >a0 >a1) (& <a+b <a0     ><0 ><1)))
          ;; comparisons: [3, 3], [-1, -1, 3, 3, 3]
          (is= 3 (t/compare (| a >a+b >a0)     (& <a+b <a0 <a1 ><0 ><1)))
          ;; comparisons: [3, 3, 3], [-1, -1, -1, 3, 3]
          (is= 3 (t/compare (| a >a+b >a0 >a1) (& <a+b <a0 <a1 ><0 ><1))))))
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    ;; TODO fix impl
    (testing "+ ClassSpec"
      ;; #{(< | =), (? *)    } -> <
      ;; #{>      , (<> | ><)} -> ><
      ;; Otherwise whatever it is
      (testing "#{<+} -> <"
        (is= -1 (t/compare i|<a0 (| i|>a+b i|>a0 i|>a1))))
      (testing "#{><+} -> ><"
        (is=  2 (t/compare i|a   (| i|><0 i|><1))))
      (testing "#{<>+} -> <>"
        (is=  3 (t/compare a     (| ><0 ><1))))
      (testing "#{<+ ><+} -> <"
        (is= -1 (t/compare i|a   (| i|>a+b i|>a0 i|><0 i|><1)))
        (is= -1 (t/compare i|>a0 (| i|>a+b i|>a0)))) ; TODO fix impl
      (testing "#{<+ <>+} -> <"
        (is= -1 (t/compare a     (| >a ><0 ><1))))
      (testing "#{=+ ><+} -> ><"
        (is=  2 (t/compare i|a   (| i|a i|><0 i|><1))))
      (testing "#{=+ <>+} -> <"
        (is= -1 (t/compare a     (| a ><0 ><1))))
      (testing "#{>+ ><+} -> ><"
        (is=  2 (t/compare i|a   (| i|<a+b i|<a0 i|><0 i|><1))))
      (testing "#{>+ <>+} -> ><"
        (is=  2 (t/compare a     (| <a0 ><0 ><1))))
      (testing "Nilable"
        (testing "= nilabled"
          (is= -1 (t/compare t/long?     (t/? t/long?))))
        (testing "< nilabled"
          (is= -1 (t/compare t/long?     (t/? t/object?))))
        (testing "> nilabled"
          (is=  2 (t/compare t/object?   (t/? t/long?))))
        (testing ">< nilabled"
          (is=  2 (t/compare t/iterable? (t/? t/comparable?))))
        (testing "<> nilabled"
          (is=  3 (t/compare t/long?     (t/? t/string?))))))
    (testing "+ ValueSpec"
      (testing "arg <"
        (testing "+ arg <")
        (testing "+ arg =")
        (testing "+ arg >")
        (testing "+ arg ><")
        (testing "+ arg <>"
          (test-symmetric -1 (t/value "a") (| t/string? t/byte?))
          (test-symmetric -1 (t/value 1)   (| (t/value 1) (t/value 2)))
          (test-symmetric -1 (t/value 1)   (| (t/value 2) (t/value 1)))
          (testing "+ arg <>"
            (test-symmetric -1 (t/value 1) (| (t/value 1) (t/value 2) (t/value 3)))
            (test-symmetric -1 (t/value 1) (| (t/value 2) (t/value 1) (t/value 3)))
            (test-symmetric -1 (t/value 1) (| (t/value 2) (t/value 3) (t/value 1))))))
      (testing "arg ="
        (testing "+ arg <>"
          (test-symmetric -1 t/nil?      (| t/nil? t/string?))))
      (testing "arg <>"
        (testing "+ arg <>"
          (test-symmetric  3 (t/value "a") (| t/byte? t/long?))
          (test-symmetric  3 (t/value 3)   (| (t/value 1) (t/value 2)))))))
  ;; TODO fix impl and go over tests
  (testing "AndSpec"
    (testing "+ AndSpec")
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    (testing "+ ClassSpec"
      (testing "#{<}"
        (testing "Boxed Primitive"
          (test-symmetric -1 t/byte?       (& t/number?   t/comparable?)))
        (testing "Final Concrete"
          (test-symmetric -1 t/string?     (& t/char-seq? t/comparable?)))
        (testing "Extensible Concrete"
          (test-symmetric -1 t/array-list? (& t/iterable? (t/isa? java.util.RandomAccess))))
        (testing "Abstract"
          (test-symmetric -1 (t/isa? java.util.AbstractMap$SimpleEntry) (& (t/isa? java.util.Map$Entry) (t/isa? java.io.Serializable))))
        (testing "Interface"
          (test-symmetric -1 i|a           (& i|>a0 i|>a1))))
    #_(testing "#{< =}")         ; not possible for `AndSpec`
    #_(testing "#{< = >}")       ; not possible for `AndSpec`
    #_(testing "#{< = > ><}")    ; not possible for `AndSpec`
    #_(testing "#{< = > >< <>}") ; not possible for `AndSpec`
    #_(testing "#{< >}")         ; not possible for `AndSpec`
    #_(testing "#{< > ><}")      ; not possible for `AndSpec`
    #_(testing "#{< > >< <>}")   ; not possible for `AndSpec`
      (testing "#{< ><}"
        (test-symmetric  2 i|a           (& i|>a+b i|>a0 i|>a1 i|><0 i|><1)))
      (testing "#{< >< <>}"
        (test-symmetric  2 t/java-set?   (& t/java-coll? t/char-seq? (t/isa? java.nio.ByteBuffer))))
      (testing "#{< <>}"
        (test-symmetric  3 t/string?     (& t/char-seq? t/java-set?)))
    #_(testing "#{= >}")       ; not possible for `AndSpec`
    #_(testing "#{= > ><}")    ; not possible for `AndSpec`
    #_(testing "#{= > >< <>}") ; not possible for `AndSpec`
      (testing "#{= ><}"
        (test-symmetric  1 i|a           (& i|a i|><0 i|><1))
        (test-symmetric  1 t/char-seq?   (& t/char-seq?   t/java-set?)))
      (testing "#{= >< <>}"
        (test-symmetric  1 t/char-seq?   (& t/char-seq?   t/java-set? t/array-list?)))
      (testing "#{= <>}"
        (test-symmetric  1 t/array-list? (& t/array-list? t/java-set?)))
      (testing "#{>}"
        (test-symmetric  1 i|a           (& i|<a+b i|<a0 i|<a1)))
      (testing "#{> ><}"
        (test-symmetric  2 i|a           (& i|<a+b i|<a0 i|><0 i|><1))
        (test-symmetric  2 t/array-list? (& (t/isa? javax.management.AttributeList) t/java-set?))
        (test-symmetric  2 t/comparable? (& (t/isa? java.nio.ByteBuffer) t/java-set?)))
      (testing "#{> >< <>}"
        (test-symmetric  2 i|a           (& i|<a0 i|><0 t/array-list?)))
      (testing "#{> <>}") ; <- comparison should be 1
      (testing "#{><}"
        (test-symmetric  2 i|a           (& i|><0 i|><1))
        (test-symmetric  2 t/char-seq?   (& t/java-set? t/array-list?)))
      (testing "#{>< <>}") ; <- comparison should be 3
      (testing "#{<>}"
        (test-symmetric  3 t/string?     (& t/array-list? t/java-set?))))
    (testing "+ ValueSpec"
      (testing "#{<}"
        (test-symmetric -1 (t/value "a") (& t/char-seq? t/comparable?)))
    #_(testing "#{< =}")         ; not possible for `AndSpec`
    #_(testing "#{< >}")         ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{< =}")         ; not possible for `AndSpec`
    #_(testing "#{< = >}")       ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{< = > ><}")    ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< = > >< <>}") ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< >}")         ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{< > ><}")      ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< > >< <>}")   ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< ><}")        ; `><` not possible for `ValueSpec`
    #_(testing "#{< >< <>}")     ; `><` not possible for `ValueSpec`
      (testing "#{< <>}"
        (test-symmetric  3 (t/value "a") (& t/char-seq? t/array-list?))
        (test-symmetric  3 (t/value "a") (& t/char-seq? t/java-set?)))
    #_(testing "#{= >}")       ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{= > ><}")    ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{= > >< <>}") ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{= ><}")      ; `><` not possible for `ValueSpec`
    #_(testing "#{= >< <>}")   ; `><` not possible for `ValueSpec`
      (testing "#{= <>}")
    #_(testing "#{>}")         ; `>` not possible for `ValueSpec`
    #_(testing "#{> ><}")      ; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{> >< <>}")   ; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{> <>}")      ; `>` not possible for `ValueSpec`
    #_(testing "#{><}")        ; `><` not possible for `ValueSpec`
    #_(testing "#{>< <>}")     ; `><` not possible for `ValueSpec`
      (testing "#{<>}"
        (test-symmetric  3 (t/value "a") (& t/array-list? t/java-set?)))))
  (testing "InferSpec"
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"))
  (testing "Expression"
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"))
  (testing "ProtocolSpec"
    (testing "+ ProtocolSpec"
      (is=  0 (t/compare (t/isa? AProtocolAll) (t/isa? AProtocolAll)))
      (is=  3 (t/compare (t/isa? AProtocolAll) (t/isa? AProtocolNone))))
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"
      (let [values #{t/universal-set t/null-set nil {} 1 "" AProtocolAll
                     quantum.test.core.untyped.type.AProtocolAll}]
        (doseq [v values]
          (test-symmetric -1 (t/value v) (t/isa? AProtocolAll)))
        (doseq [v [""]]
          (test-symmetric -1 (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values "")]
          (test-symmetric  3 (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values nil)]
          (test-symmetric -1 (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-symmetric  3 (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-symmetric -1 (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v (disj values nil)]
          (test-symmetric  3 (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v values]
          (test-symmetric  3 (t/value v) (t/isa? AProtocolNone))))))
  (testing "ClassSpec"
    (testing "+ ClassSpec"
      (testing "Boxed Primitive + Boxed Primitive"
        (is= 0 (t/compare t/long? t/long?))
        (test-symmetric 3 t/long? t/int?))
      (testing "Boxed Primitive + Final Concrete"
        (test-symmetric 3 t/long? t/string?))
      (testing "Boxed Primitive + Extensible Concrete"
        (testing "< , >"
          (test-symmetric -1  t/long? t/object?))
        (testing "<>"
          (test-symmetric 3 t/long? t/thread?)))
      (testing "Boxed Primitive + Abstract"
        (test-symmetric 3 t/long? (t/isa? java.util.AbstractCollection)))
      (testing "Boxed Primitive + Interface"
        (test-symmetric 3 t/long? t/char-seq?))
      (testing "Final Concrete + Final Concrete"
        (test-symmetric 0 t/string? t/string?))
      (testing "Final Concrete + Extensible Concrete"
        (testing "< , >"
          (test-symmetric -1 t/string? t/object?))
        (testing "<>"
          (test-symmetric  3 t/string? t/array-list?)))
      (testing "Final Concrete + Abstract")
      (testing "Final Concrete + Interface"
        (testing "< , >"
          (test-symmetric -1 t/string? t/comparable?))
        (testing "<>"
          (test-symmetric  3 t/string? t/java-coll?)))
      (testing "Extensible Concrete + Extensible Concrete"
        (test-symmetric 0 t/object? t/object?)
        (testing "< , >"
          (test-symmetric -1 t/array-list? t/object?))
        (testing "<>"
          (test-symmetric  3 t/array-list? t/thread?)))
      (testing "Extensible Concrete + Abstract"
        (testing "< , >"
          (test-symmetric -1 (t/isa? java.util.AbstractCollection) t/object?)
          (test-symmetric -1 t/array-list? (t/isa? java.util.AbstractCollection)))
        (testing "<>"
          (test-symmetric  3 t/thread? (t/isa? java.util.AbstractCollection))
          (test-symmetric  3 (t/isa? java.util.AbstractCollection) t/thread?)))
      (testing "Extensible Concrete + Interface"
        (test-symmetric 2 t/array-list? t/char-seq?))
      (testing "Abstract + Abstract"
        (test-symmetric 0 (t/isa? java.util.AbstractCollection) (t/isa? java.util.AbstractCollection))
        (testing "< , >"
          (test-symmetric -1 (t/isa? java.util.AbstractList) (t/isa? java.util.AbstractCollection)))
        (testing "<>"
          (test-symmetric  3 (t/isa? java.util.AbstractList) (t/isa? java.util.AbstractQueue))))
      (testing "Abstract + Interface"
        (testing "< , >"
          (test-symmetric -1 (t/isa? java.util.AbstractCollection) t/java-coll?))
        (testing "><"
          (test-symmetric  2 (t/isa? java.util.AbstractCollection) t/comparable?)))
      (testing "Interface + Interface"
        (testing "< , >"
          (test-symmetric -1 t/java-coll? t/iterable?))
        (testing "><"
          (test-symmetric  2 t/char-seq?  t/comparable?))))
    (testing "+ ValueSpec"
      (testing "<"
        (testing "Class equality"
          (test-symmetric -1 (t/value "a") t/string?))
        (testing "Class inheritance"
          (test-symmetric -1 (t/value "a") t/char-seq?)
          (test-symmetric -1 (t/value "a") t/object?)))
      (testing "<>"
        (test-symmetric 3 (t/value "a") t/byte?))))
  (testing "ValueSpec"
    (testing "+ ValueSpec"
      (testing "="
        (is= 0 (t/compare (t/value nil) (t/value nil)))
        (is= 0 (t/compare (t/value 1  ) (t/value 1  )))
        (is= 0 (t/compare (t/value "a") (t/value "a"))))
      (testing "=, non-strict"
        (test-symmetric 0 (t/value (vector)         ) (t/value (list)          ))
        (test-symmetric 0 (t/value (vector (vector))) (t/value (vector (list))))
        (test-symmetric 0 (t/value (hash-map)       ) (t/value (sorted-map)    )))
      (testing "<>"
        (test-symmetric 3 (t/value 1  ) (t/value 2  ))
        (test-symmetric 3 (t/value "a") (t/value "b"))
        (test-symmetric 3 (t/value 1  ) (t/value "a"))
        (test-symmetric 3 (t/value nil) (t/value "a"))))))

(deftest test|not
  (testing "simplification"
    (testing "universal/null set"
      (is= (! t/universal-set)
           t/null-set)
      (is= (! t/null-set)
           t/universal-set))
    (testing "universal class-set"
      (is= (! t/val?)
           t/nil?)
      (is= (! t/val|by-class?)
           t/nil?))
    (testing "DeMorgan's Law"
      (is= (! (| i|a i|b))
           (& (! i|a) (! i|b)))
      (is= (! (& i|a i|b))
           (| (! i|a) (! i|b)))
      (is= (! (| (! i|a) (! i|b)))
           (&       i|a     i|b))
      (is= (! (& (! i|a) (! i|b)))
           (|       i|a     i|b)))))

(deftest test|-
  (testing "="
    (is= (t/- a a)
         t/null-set))
  (testing "<"
    (is= (t/- a >a)
         t/null-set))
  (testing "<>"
    (is= (t/- a b)
         a))
  (testing ">"
    (is= (t/- (| a b) a)
         b)
    (is= (t/- (| a b t/long?) a)
         (| b t/long?)))
  (testing "><"
    ))

(deftest test|or
  (testing "equality"
    (is= (| a b) (| a b)))
  (testing "simplification"
    (testing "via single-arg"
      (is= (| a)
           a))
    (testing "via identity"
      (is= (| a a)
           a)
      (is= (| (| a a) a)
           a)
      (is= (| a (| a a))
           a)
      (is= (| (| a b) (| b a))
           (| a b))
      (is= (| (| a b ><0) (| a ><0 b))
           (| a b ><0)))
    (testing "nested `or` is expanded"
      (is= (| (| a b) (| ><0 ><1))
           (| a b ><0 ><1))
      (is= (| (| a b) (| ><0 ><1))
           (| a b ><0 ><1)))
    ;; TODO fix impl
    (testing "via `not`"
      (is= (| a (! a))
           t/universal-set)
      (is= (| a b (! a))
           t/universal-set)
      (is= (| a b (| (! a) (! b)))
           t/universal-set))
    (testing "nested"
      (is= (t/or-spec>args (| (| t/string? t/double?)
                              t/char-seq?))
           [t/double? t/char-seq?])
      (is= (t/or-spec>args (| (| t/string? t/double?)
                              (| t/double? t/char-seq?)))
           [t/double? t/char-seq?])
      (is= (t/or-spec>args (| (| t/string? t/double?)
                              (| t/char-seq? t/number?)))
           [t/char-seq? t/number?]))
    (testing "#{<+ =} -> #{<+}"
      (is= (t/or-spec>args (| i|>a+b i|>a0 i|a))
           [i|>a+b i|>a0]))
    (testing "#{<+ >+} -> #{<+}"
      (is= (t/or-spec>args (| i|>a+b i|>a0 i|<a+b i|<a0))
           [i|>a+b i|>a0]))
    (testing "#{>+ =} -> #{=}"
      (is= (| i|<a+b i|<a0 i|a)
           i|a))
    (testing "#{<+ >+ ><+} -> #{<+ ><+}"
      (is= (t/or-spec>args (| i|>a+b i|>a0 i|<a+b i|<a0 i|><0 i|><1))
           [i|>a+b i|>a0 i|><0 i|><1]))
    (testing "#{<+ >+ <>+} -> #{<+ <>+}"
      (is= (t/or-spec>args (| >a <a0 ><0 ><1))
           [>a ><0 ><1]))
    (testing "#{<+ =+ >+ ><+} -> #{<+ ><+}"
      (is= (t/or-spec>args (| i|>a+b i|>a0 i|a i|<a+b i|<a0 i|><0 i|><1))
           [i|>a+b i|>a0 i|><0 i|><1]))
    (testing "#{<+ =+ >+ <>+} -> #{<+ <>+}"
      (is= (t/or-spec>args (| >a a <a0 ><0 ><1))
           [>a ><0 ><1]))))

(deftest test|and
  (testing "equality"
    (is= (& i|a i|b) (& i|a i|b)))
  (testing "null set / universal set"
    (is= (& t/universal-set t/universal-set)
         t/universal-set)
    (is= (& t/universal-set t/null-set)
         t/null-set)
    (is= (& t/null-set t/universal-set)
         t/null-set)
    (is= (& t/universal-set t/null-set t/universal-set)
         t/null-set)
    (is= (& t/universal-set t/string?)
         t/string?)
    (is= (& t/universal-set t/char-seq? t/string?)
         t/string?)
    (is= (& t/universal-set t/string? t/char-seq?)
         t/string?)
    (is= (& t/null-set t/string?)
         t/null-set)
    (is= (& t/null-set t/char-seq? t/string?)
         t/null-set)
    (is= (& t/null-set t/string? t/char-seq?)
         t/null-set))
  (testing "simplification"
    (testing "via single-arg"
      (is= (& a)
           a))
    (testing "via identity"
      (is= (& a a)
           a)
      (is= (& (& a a) a)
           a)
      (is= (& a (& a a))
           a)
      (is= (& (| t/string? t/byte?) (| t/byte? t/string?))
           (| t/string? t/byte?))
      (is= (& (| a b) (| b a))
           (| a b))
      (is= (& (| a b ><0) (| a ><0 b))
           (| a b ><0)))
    (testing ""
      (is= (t/and-spec>args (& i|a i|b))
           [i|a i|b]))
    (testing "null-set"
      (is= (& a b)
           t/null-set)
      (is= (& t/string? t/byte?)
           t/null-set)
      (is= (& a ><0)
           t/null-set)
      (is= (& a ><0 ><1)
           t/null-set))
    (testing "nested `and` is expanded"
      (is= (& (& a b) (& ><0 ><1))
           (& a b ><0 ><1))
      (is= (& (& a b) (& ><0 ><1))
           (& a b ><0 ><1)))
    (testing "and + not"
      (is= (& a (! a))
           t/null-set)
      (testing "+ or"
        (is= (& (! a) a b)
             t/null-set)
        (is= (& a (! a) b)
             t/null-set)
        (is= (& a b (! a))
             t/null-set)
        ;; TODO fix impl
        (is= (& (| a b) (! a))
             b)
        ;; TODO fix impl
        (is= (& (! a) (| a b))
             b)
        ;; TODO fix impl
        (is= (& (| a b) (! b) (| b a))
             b)
        (is= (& (| a b) (! b) (| ><0 b))
             t/null-set))
      ;; TODO fix impl
      (is= (& t/primitive? (! t/boolean?))
           (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)))
    (testing "#{<+ =} -> #{=}"
      (is= (& i|>a+b i|>a0 i|a)
           i|a))
    (testing "#{>+ =+} -> #{>+}"
      (is= (t/and-spec>args (& i|<a+b i|<a0 i|a))
           [i|<a+b i|<a0]))
    (testing "#{<+ >+} -> #{>+}"
      (is= (t/and-spec>args (& i|>a+b i|>a0 i|<a+b i|<a0))
           [i|<a+b i|<a0]))
    (testing "#{<+ >+ ∅+} -> #{>+ ∅+}"
      (is= (t/and-spec>args (& i|>a+b i|>a0 i|<a+b i|<a0 i|><0 i|><1))
           [i|<a+b i|<a0 i|><0 i|><1]))
    (testing "#{<+ =+ >+ ∅+} -> #{>+ ∅+}"
      (is= (t/and-spec>args (& i|>a+b i|>a0 i|a i|<a+b i|<a0 i|><0 i|><1))
           [i|<a+b i|<a0 i|><0 i|><1]))))

(deftest test|=
  ;; TODO fix impl
  (test-symmetric 0
    (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
    (& (| t/boolean? t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
       (! t/boolean?)))
  (test-symmetric 0 t/any? t/universal-set)
  (testing "universal class(-set) identity"
    (is (not= t/val? (& t/any? t/val?)))
    ;; TODO fix impl
    (is (t/= t/val? (& t/any? t/val?)))))
