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
    [quantum.untyped.core.logic
      :refer [ifs]]
    [quantum.untyped.core.numeric      :as unum]
    [quantum.untyped.core.numeric.combinatorics :as ucombo]
    [quantum.untyped.core.spec         :as s]
    [quantum.untyped.core.type         :as t
      :refer [& | !]]
    [quantum.untyped.core.defnt
      :refer [defns]]))

;; Here, `NotSpec` labels on `testing` mean such *after* simplification

(defmacro test-comparisons>comparisons [[_ _ a b]]
  `[[~@(for [a* (rest a)]
         `(t/compare ~a* ~b))]
    [~@(for [b* (rest b)]
         `(t/compare ~b* ~a))]])

;; TODO come back to this
#_(do (is= -1 (t/compare (t/value 1) t/numerically-byte?))

    (is= (& t/long? (>expr (fn1 = 1)))
         (t/value 1))

    (is= (& (t/value 1) (>expr unum/integer-value?))
         (t/value 1))

    (t/compare (t/value 1) (>expr unum/integer-value?))

    (is= 0 (t/compare (t/value 1) (>expr (fn1 =|long 1))))
    (is= 0 (t/compare (t/value 1) (>expr (fn [^long x] (= x 1)))))
    (is= 0 (t/compare (t/value 1) (>expr (fn [^long x] (== x 1)))))
    (is= 0 (t/compare (t/value 1) (>expr (fn [x] (core/== (long x) 1)))))
    (is= 0 (t/compare (t/value 1) (>expr (fn [x] (= (long x) 1))))))

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

(do (def >a+b (t/isa? java.util.AbstractCollection))
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
    (def ><2  t/long?))

(def Uc (t/isa? java.lang.Object))

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

(defns spec>spec-combos
  "To generate all commutative possibilities for a given spec."
  [spec t/spec? > (s/seq-of t/spec?)]
  (ifs (t/and-spec? spec) (->> spec t/and-spec>args ucombo/permutations
                               (map #(t/->AndSpec (vec %) (atom nil))))
       (t/or-spec?  spec) (->> spec t/or-spec>args  ucombo/permutations
                               (map #(t/->OrSpec  (vec %) (atom nil))))
       [spec]))

#?(:clj
(defmacro test-comparison
  "Performs a `t/compare` on `a` and `b`, ensuring that their relationship is symmetric,
   and that the inputs are internally commutative if applicable (e.g. if `a` is an `AndSpec`,
   ensures that it is commutative).
   The basis comparison is the first input."
  [c #_t/comparisons a #_t/spec? b #_t/spec?]
  `(let [c# ~c]
     (doseq ;; Commutativity
            [a*# (spec>spec-combos ~a)
             b*# (spec>spec-combos ~b)]
       ;; Symmetry
       (is= c#             (t/compare a*# b*#))
       (is= (t/inverse c#) (t/compare b*# a*#))))))

(def comparison-combinations
  ["#{<}"
   "#{< =}"
   "#{< = >}"
   "#{< = > ><}"
   "#{< = > >< <>}"
   "#{< = > <>}"
   "#{< = ><}"
   "#{< = >< <>}"
   "#{< = <>}"
   "#{< >}"
   "#{< > ><}"
   "#{< > >< <>}"
   "#{< > <>}"
   "#{< ><}"
   "#{< >< <>}"
   "#{< <>}"
   "#{=}"
   "#{= >}"
   "#{= > ><}"
   "#{= > >< <>}"
   "#{= > <>}"
   "#{= ><}"
   "#{= >< <>}"
   "#{= <>}"
   "#{>}"
   "#{> ><}"
   "#{> >< <>}"
   "#{> <>}"
   "#{><}"
   "#{>< <>}"
   "#{<>}"])

(deftest test|in|compare
  (testing "UniversalSetSpec"
    (testing "+ UniversalSetSpec"
      (test-comparison  0 t/universal-set t/universal-set))
    (testing "+ NullSetSpec"
      (test-comparison  1 t/universal-set t/empty-set))
    (testing "+ NotSpec"
      (test-comparison  1 t/universal-set (! a)))
    (testing "+ OrSpec"
      (test-comparison  1 t/universal-set (| ><0 ><1)))
    (testing "+ AndSpec")
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec"
      (doseq [spec protocol-specs]
        (test-comparison 1 t/universal-set spec)))
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"
      (doseq [spec [(t/value t/universal-set)
                    (t/value t/empty-set)
                    (t/value 0)
                    (t/value nil)]]
        (test-comparison 1 t/universal-set spec))))
  ;; The null set is considered to always (vacuously) be a subset of any set
  (testing "NullSetSpec"
    (testing "+ NullSetSpec"
      (test-comparison 0 t/empty-set t/empty-set))
    (testing "+ NotSpec"
      (testing "Inner ClassSpec"
        (test-comparison -1 t/empty-set (! a)))
      (testing "Inner ValueSpec"
        (test-comparison -1 t/empty-set (! (t/value 1)))))
    (testing "+ OrSpec"
      (test-comparison -1 t/empty-set (| ><0 ><1)))
    (testing "+ AndSpec")
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec"
      (doseq [spec protocol-specs]
        (test-comparison -1 t/empty-set spec)))
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"
      (test-comparison -1 t/empty-set (t/value t/empty-set))
      (test-comparison -1 t/empty-set (t/value 0))))
  (testing "NotSpec"
    (testing "+ NotSpec"
      (test-comparison  0 (! a)           (! a))
      (test-comparison  2 (! a)           (! b))
      (test-comparison  2 (! i|a)         (! i|b))
      (test-comparison  2 (! t/string?)   (! t/byte?))
      (test-comparison  1 (! a)           (! >a))
      (test-comparison -1 (! a)           (! <a0))
      (test-comparison  0 (! (t/value 1)) (! (t/value 1)))
      (test-comparison  2 (! (t/value 1)) (! (t/value 2))))
    ;; TODO continue to implement
    (testing "+ OrSpec"
      (testing "#{<}"
        ;; TODO Technically something like this but can't do the below b/c of simplification
        #_(test-comparison -1 (! a) (| (| (! a) <a0) (| (! a) <a1))))
    #_(testing "#{< =}")         ; Impossible for `OrSpec`
    #_(testing "#{< = >}")       ; Impossible for `OrSpec`
    #_(testing "#{< = > ><}")    ; Impossible for `OrSpec`
    #_(testing "#{< = > >< <>}") ; Impossible for `OrSpec`
    #_(testing "#{< = > <>}")    ; Impossible for `OrSpec`
    #_(testing "#{< = ><}")      ; Impossible for `OrSpec`
    #_(testing "#{< = >< <>}")   ; Impossible for `OrSpec`
    #_(testing "#{< = <>}")      ; Impossible for `OrSpec`
    #_(testing "#{< >}")         ; Impossible for `OrSpec`
    #_(testing "#{< > ><}")      ; Impossible for `OrSpec`
    #_(testing "#{< > >< <>}")   ; Impossible for `OrSpec`
    #_(testing "#{< > <>}")      ; Impossible for `OrSpec`
      (testing "#{< ><}"
        #_(test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1))
        #_(test-comparison -1 i|>a0 (| i|>a+b i|>a0)))
      (testing "#{< >< <>}"
        #_(test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1 t/string?)))
      (testing "#{< <>}"
        #_(test-comparison -1 a     (| >a ><0 ><1)))
    #_(testing "#{=}")           ; Impossible for `OrSpec`
    #_(testing "#{= >}")         ; Impossible for `OrSpec`
    #_(testing "#{= > ><}")      ; Impossible for `OrSpec`
    #_(testing "#{= > >< <>}")   ; Impossible for `OrSpec`
    #_(testing "#{= > <>}")      ; Impossible for `OrSpec`
      (testing "#{= ><}"
        (test-comparison -1 (! a)   (| (! a)   i|><0 i|><1))
        (test-comparison -1 (! i|a) (| (! i|a) i|><0 i|><1)))
      (testing "#{= >< <>}"
        #_(test-comparison -1 i|a   (| i|a i|><0 i|><1 t/string?)))
      (testing "#{= <>}"
        (test-comparison -1 (! a) (| (! a) <a0))
        (test-comparison -1 (! a) (| (! a) <a1))
        (test-comparison -1 (! a) (| (! a) <a0 <a1)))
      (testing "#{>}"
        #_(test-comparison  1 a     (| <a0 <a1))
        #_(test-comparison  1 i|a   (| i|<a0 i|<a1)))
      (testing "#{> ><}"
        #_(test-comparison  2 i|a   (| i|<a+b i|<a0 i|><0 i|><1)))
      (testing "#{> >< <>}"
        #_(test-comparison  2 i|a   (| i|<a+b i|<a0 i|><0 i|><1 t/string?)))
      (testing "#{> <>}"
        (test-comparison  2 (! a)   (| b a))
        (test-comparison  2 (! b)   (| a b))
        (test-comparison  2 (! ><0) (| ><0 ><1))
        (test-comparison  2 (! ><1) (| ><1 ><0)))
      (testing "#{><}"
        #_(test-comparison  2 i|a   (| i|><0 i|><1)))
      (testing "#{>< <>}"
        #_(test-comparison  2 i|a   (| i|><0 i|><1 t/string?)))
      (testing "#{<>}"
        (test-comparison  3 (! a)   (| <a0 <a1))))
    ;; TODO
    #_(testing "+ AndSpec"
      (testing "inner #{= <>}"
        (test-comparison ... (! a) (& a (! b)))))
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    (testing "+ ClassSpec"
      (test-comparison  3 (!     a)     a) ; inner =
      (test-comparison  3 (!   i|a)   i|a) ; inner =
      (test-comparison  3 (!     a)   <a0) ; inner >
      (test-comparison  3 (!   i|a) i|<a0) ; inner >
      (test-comparison  2 (!     a)    >a) ; inner <
      (test-comparison  2 (!   i|a) i|>a0) ; inner ><
      (test-comparison  1 (!   a  )   ><0) ; inner <>
      (test-comparison  2 (!   i|a) i|><0) ; inner ><
      (test-comparison  2 (!     a)    Uc) ; inner <
      (test-comparison  2 (!   i|a)    Uc) ; inner <
      (test-comparison  2 (!   <a0)     a) ; inner <
      (test-comparison  2 (! i|<a0)   i|a) ; inner <
      (test-comparison  2 (!   <a0)    >a) ; inner <
      (test-comparison  2 (! i|<a0) i|>a0) ; inner <
      (test-comparison  1 (!   <a0)   ><0) ; inner <>
      (test-comparison  2 (! i|<a0) i|><0) ; inner ><
      (test-comparison  2 (!   <a0)    Uc) ; inner <
      (test-comparison  2 (! i|<a0)    Uc) ; inner <
      (test-comparison  3 (!    >a)     a) ; inner >
      (test-comparison  3 (! i|>a0)   i|a) ; inner >
      (test-comparison  3 (!    >a)   <a0) ; inner >
      (test-comparison  3 (! i|>a0) i|<a0) ; inner >
      (test-comparison  1 (!    >a)   ><0) ; inner <>
      (test-comparison  2 (! i|>a0) i|><0) ; inner ><
      (test-comparison  2 (!    >a)    Uc) ; inner <
      (test-comparison  2 (! i|>a0)    Uc) ; inner <
      (test-comparison  1 (!   ><0)     a) ; inner <>
      (test-comparison  2 (! i|><0)   i|a) ; inner ><
      (test-comparison  1 (!   ><0)   <a0) ; inner <>
      (test-comparison  2 (! i|><0) i|<a0) ; inner ><
      (test-comparison  1 (!   ><0)    >a) ; inner <>
      (test-comparison  2 (! i|><0) i|>a0) ; inner ><
      (test-comparison  2 (!   ><0)    Uc) ; inner <
      (test-comparison  2 (! i|><0)    Uc) ; inner <
    (testing "+ ValueSpec"
      (test-comparison -1 (t/value 1)  (! (t/value 2)))
      (test-comparison  3 (t/value "") (! t/string?))))
  (testing "OrSpec"
    (testing "+ OrSpec"
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      ;; TODO add complete comparisons via `comparison-combinations`
      (testing "#{<}, #{<}"
        ;; comparisons:        < <                        < <
        (test-comparison  0 (| a b)                    (| a b))
        ;; comparisons:            <      <               <      <
        (test-comparison  0 (|     i|>a+b i|>a0)       (| i|>a+b i|>a0)))
      (testing "#{<}, #{<, ><}"
        ;; comparisons:            <      <               <      <     ><    ><
        (test-comparison -1 (|     i|>a+b i|>a0)       (| i|>a+b i|>a0 i|><0 i|><1))
        ;; comparisons:            <      <               <      <     ><    ><    ><
        (test-comparison -1 (|     i|>a+b i|>a0)       (| i|>a+b i|>a0 i|>a1 i|><0 i|><1))
        ;; comparisons:            <      <     <         <      <     <     ><    ><
        (test-comparison -1 (|     i|>a+b i|>a0 i|>a1) (| i|>a+b i|>a0 i|>a1 i|><0 i|><1)))
      (testing "#{<, ><}, #{<}"
        ;; comparisons:            <      <     ><        <      <
        (test-comparison  1 (|     i|>a+b i|>a0 i|>a1) (| i|>a+b i|>a0))
        ;; comparisons:        ><  <     <                <     <
        (test-comparison  1 (| i|a i|><0 i|><1)        (| i|><0 i|><1)))
      (testing "#{<, ><}, #{<, ><}"
        ;; comparisons:            <      ><              <                  ><
        (test-comparison  2 (|     i|>a+b i|>a0)       (| i|>a+b             i|><0))
        ;; comparisons:            <      ><    ><        <                  ><    ><
        (test-comparison  2 (|     i|>a+b i|>a0 i|>a1) (| i|>a+b             i|><0 i|><1))
        ;; comparisons:            <      <     ><        <      <           ><    ><
        (test-comparison  2 (|     i|>a+b i|>a0 i|>a1) (| i|>a+b i|>a0       i|><0 i|><1))
        ;; comparisons:        <   <      ><              <                  ><
        (test-comparison  2 (| i|a i|>a+b i|>a0)       (| i|a                i|><0))
        ;; comparisons:        <   ><     ><              <                  ><    ><
        (test-comparison  2 (| i|a i|>a+b i|>a0)       (| i|a                i|><0 i|><1))
        ;; comparisons:        ><  <                                         <     ><
        (test-comparison  2 (| i|a i|><0)              (|                    i|><0 i|><1))
        ;; comparisons:        ><        <     ><                                  ><    <
        (test-comparison  2 (| i|a       i|><1 i|><2)  (|                    i|><0 i|><1))
        ;; comparisons:        ><  ><    <                                         <     ><
        (test-comparison  2 (| i|a i|><0 i|><1)        (|                          i|><1 i|><2)))
      (testing "#{<, ><}, #{><}"
        ;; comparisons:        <   ><                     ><     ><
        (test-comparison  2 (| i|a i|><0)              (| i|>a+b i|>a0))
        ;; comparisons:        <   ><    ><               ><     ><
        (test-comparison  2 (| i|a i|><0 i|><1)        (| i|>a+b i|>a0))
        ;; comparisons:        <   ><                     ><     ><    ><
        (test-comparison  2 (| i|a i|><0)              (| i|>a+b i|>a0 i|>a1))
        ;; comparisons:        <   ><    ><               ><     ><    ><
        (test-comparison  2 (| i|a i|><0 i|><1)        (| i|>a+b i|>a0 i|>a1)))
      (testing "#{<, <>}, #{<, <>}"
        ;; comparisons:        <  <>                      <      <>
        (test-comparison  2 (| a  b)                   (| a      ><1))
        ;; comparisons:        <> <                       <      <>
        (test-comparison  2 (| a  b)                   (| b      ><1)))
      (testing "#{<, <>}, #{><, <>}"
        ;; comparisons:        <, <>                      >< <>  <>
        (test-comparison  2 (| a  b)                   (| >a ><0 ><1)))
      (testing "#{><}, #{<, ><}"
        ;; comparisons:        ><  ><     ><              <                  ><    ><
        (test-comparison  2 (| i|a i|>a+b i|>a0)       (| i|<a+b             i|><0 i|><1))
        ;; comparisons:        ><  ><     ><    ><        <                  ><    ><
        (test-comparison  2 (| i|a i|>a+b i|>a0 i|>a1) (| i|<a+b             i|><0 i|><1))
        ;; comparisons:        ><  ><     ><              <      <           ><    ><
        (test-comparison  2 (| i|a i|>a+b i|>a0)       (| i|<a+b i|<a0       i|><0 i|><1))
        ;; comparisons:        ><  ><     ><    ><        <      <           ><    ><
        (test-comparison  2 (| i|a i|>a+b i|>a0 i|>a1) (| i|<a+b i|<a0       i|><0 i|><1))
        ;; comparisons:        ><  ><     ><              <      <     <     ><    ><
        (test-comparison  2 (| i|a i|>a+b i|>a0)       (| i|<a+b i|<a0 i|<a1 i|><0 i|><1))
        ;; comparisons:        ><  ><     ><    ><        <      <     <     ><    ><
        (test-comparison  2 (| i|a i|>a+b i|>a0 i|>a1) (| i|<a+b i|<a0 i|<a1 i|><0 i|><1)))
      (testing "#{><}, #{><}"
        ;; comparisons:        ><  ><                                        ><    ><
        (test-comparison  2 (| i|a i|><2)              (|                    i|><0 i|><1))
        ;; comparisons:        ><  ><                                              ><    ><
        (test-comparison  2 (| i|a i|><0)              (|                          i|><1 i|><2)))
      (testing "#{<>}, #{<>}"
        ;; comparisons:        <> <>                         <>  <>
        (test-comparison  3 (| a  b)                   (|    ><0 ><1)))))
    ;; TODO fix tests/impl
    #_(testing "+ AndSpec"
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      (testing "#{= <+} -> #{<+}"
        (testing "+ #{<+}"
          ;; comparisons: [-1, -1], [-1, -1]
          (test-comparison  1 (| a >a+b >a0)     (& >a+b >a0))
          ;; comparisons: [-1, -1, 3], [-1, -1]
          (test-comparison  1 (| a >a+b >a0 >a1) (& >a+b >a0))
          ;; comparisons: [-1, -1], [-1, -1, 3]
          (test-comparison  3 (| a >a+b >a0)     (& >a+b >a0 >a1))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (test-comparison  1 (| a >a+b >a0 >a1) (& >a+b >a0 >a1)))
        (testing "+ #{∅+}"
          ;; comparisons: [3, 3, 3], [3, 3]
          (test-comparison  3 (| a >a+b >a0)     (& ><0 ><1)))
        (testing "+ #{<+ ∅+}"
          ;; comparisons: [-1, 3], [-1, 3, 3]
          (test-comparison  3 (| a >a+b >a0)    (& >a+b         ><0 ><1))
          ;; comparisons: [-1, 3, 3], [-1, 3, 3]
          (test-comparison  3 (| a >a+b >a0 >a1) (& >a+b         ><0 ><1))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3]
          (test-comparison  3 (| a >a+b >a0)     (& >a+b >a0     ><0 ><1))
          ;; comparisons: [-1, -1, 3], [-1, -1, 3, 3]
          (test-comparison  3 (| a >a+b >a0 >a1) (& >a+b >a0     ><0 ><1))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3, 3]
          (test-comparison  3 (| a >a+b >a0)     (& >a+b >a0 >a1 ><0 ><1))
          ;; comparisons: [-1, -1, -], [-1, -1, -1, 3, 3]
          (test-comparison  3 (| a >a+b >a0 >a1) (& >a+b >a0 >a1 ><0 ><1)))
        (testing "+ #{= ∅+}"
          ;; comparisons: [3, 3], [-1, 3]
          (test-comparison  3 (| a >a+b >a0)     (& a ><0))
          ;; comparisons: [3, 3], [-1, 3, 3]
          (test-comparison  3 (| a >a+b >a0)     (& a ><0 ><1)))
        (testing "+ #{>+ ∅+}"
          ;; comparisons: [3, 3], [-1, 3, 3]
          (test-comparison  3 (| a >a+b >a0)     (& <a+b         ><0 ><1))
          ;; comparisons: [3, 3, 3], [-1, 3, 3]
          (test-comparison  3 (| a >a+b >a0 >a1) (& <a+b         ><0 ><1))
          ;; comparisons: [3, 3], [-1, -1, 3, 3]
          (test-comparison  3 (| a >a+b >a0)     (& <a+b <a0     ><0 ><1))
          ;; comparisons: [3, 3, 3], [-1, -1, 3, 3]
          (test-comparison  3 (| a >a+b >a0 >a1) (& <a+b <a0     ><0 ><1))
          ;; comparisons: [3, 3], [-1, -1, 3, 3, 3]
          (test-comparison  3 (| a >a+b >a0)     (& <a+b <a0 <a1 ><0 ><1))
          ;; comparisons: [3, 3, 3], [-1, -1, -1, 3, 3]
          (test-comparison  3 (| a >a+b >a0 >a1) (& <a+b <a0 <a1 ><0 ><1)))))
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    (testing "+ ClassSpec"
      (testing "#{<}"
        (test-comparison -1 i|<a0 (| i|>a+b i|>a0 i|>a1)))
    #_(testing "#{< =}")         ; Impossible for `OrSpec`
    #_(testing "#{< = >}")       ; Impossible for `OrSpec`
    #_(testing "#{< = > ><}")    ; Impossible for `OrSpec`
    #_(testing "#{< = > >< <>}") ; Impossible for `OrSpec`
    #_(testing "#{< = > <>}")    ; Impossible for `OrSpec`
    #_(testing "#{< = ><}")      ; Impossible for `OrSpec`
    #_(testing "#{< = >< <>}")   ; Impossible for `OrSpec`
    #_(testing "#{< = <>}")      ; Impossible for `OrSpec`
    #_(testing "#{< >}")         ; Impossible for `OrSpec`
    #_(testing "#{< > ><}")      ; Impossible for `OrSpec`
    #_(testing "#{< > >< <>}")   ; Impossible for `OrSpec`
    #_(testing "#{< > <>}")      ; Impossible for `OrSpec`
      (testing "#{< ><}"
        (test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1))
        (test-comparison -1 i|>a0 (| i|>a+b i|>a0)))
      (testing "#{< >< <>}"
        (test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1 t/string?)))
      (testing "#{< <>}"
        (test-comparison -1 a     (| >a ><0 ><1)))
    #_(testing "#{=}")           ; Impossible for `OrSpec`
    #_(testing "#{= >}")         ; Impossible for `OrSpec`
    #_(testing "#{= > ><}")      ; Impossible for `OrSpec`
    #_(testing "#{= > >< <>}")   ; Impossible for `OrSpec`
    #_(testing "#{= > <>}")      ; Impossible for `OrSpec`
      (testing "#{= ><}"
        (test-comparison -1 i|a   (| i|a i|><0 i|><1)))
      (testing "#{= >< <>}"
        (test-comparison -1 i|a   (| i|a i|><0 i|><1 t/string?)))
      (testing "#{= <>}"
        (test-comparison -1 a     (| a ><0 ><1)))
      (testing "#{>}"
        (test-comparison  1 a     (| <a0 <a1))
        (test-comparison  1 i|a   (| i|<a0 i|<a1)))
      (testing "#{> ><}"
        (test-comparison  2 i|a   (| i|<a+b i|<a0 i|><0 i|><1)))
      (testing "#{> >< <>}"
        (test-comparison  2 i|a   (| i|<a+b i|<a0 i|><0 i|><1 t/string?)))
      (testing "#{> <>}"
        (test-comparison  2 a     (| <a0 ><0 ><1)))
      (testing "#{><}"
        (test-comparison  2 i|a   (| i|><0 i|><1)))
      (testing "#{>< <>}"
        (test-comparison  2 i|a   (| i|><0 i|><1 t/string?)))
      (testing "#{<>}"
        (test-comparison  3 a     (| ><0 ><1)))
      (testing "Nilable"
        (testing "<  nilabled: #{< <>}"
          (test-comparison -1 t/long?     (t/? t/object?)))
        (testing "=  nilabled: #{= <>}"
          (test-comparison -1 t/long?     (t/? t/long?)))
        (testing ">  nilabled: #{> <>}"
          (test-comparison  2 t/object?   (t/? t/long?)))
        (testing ">< nilabled: #{>< <>}"
          (test-comparison  2 t/iterable? (t/? t/comparable?)))
        (testing "<> nilabled: #{<>}"
          (test-comparison  3 t/long?     (t/? t/string?)))))
    (testing "+ ValueSpec"
      (testing "arg <"
        (testing "+ arg <")
        (testing "+ arg =")
        (testing "+ arg >")
        (testing "+ arg ><")
        (testing "+ arg <>"
          (test-comparison -1 (t/value "a") (| t/string? t/byte?))
          (test-comparison -1 (t/value 1)   (| (t/value 1) (t/value 2)))
          (test-comparison -1 (t/value 1)   (| (t/value 2) (t/value 1)))
          (testing "+ arg <>"
            (test-comparison -1 (t/value 1) (| (t/value 1) (t/value 2) (t/value 3)))
            (test-comparison -1 (t/value 1) (| (t/value 2) (t/value 1) (t/value 3)))
            (test-comparison -1 (t/value 1) (| (t/value 2) (t/value 3) (t/value 1))))))
      (testing "arg ="
        (testing "+ arg <>"
          (test-comparison -1 t/nil?        (| t/nil? t/string?))))
      (testing "arg <>"
        (testing "+ arg <>"
          (test-comparison  3 (t/value "a") (| t/byte? t/long?))
          (test-comparison  3 (t/value 3)   (| (t/value 1) (t/value 2)))))))
  (testing "AndSpec"
    (testing "+ AndSpec")
    (testing "+ InferSpec")
    (testing "+ Expression")
    (testing "+ ProtocolSpec")
    (testing "+ ClassSpec"
      (testing "#{<}"
        (testing "Boxed Primitive"
          (test-comparison -1 t/byte?        (& t/number?   t/comparable?)))
        (testing "Final Concrete"
          (test-comparison -1 t/string?      (& t/char-seq? t/comparable?)))
        (testing "Extensible Concrete"
          (test-comparison -1 a (& t/iterable? (t/isa? java.util.RandomAccess))))
        (testing "Abstract"
          (test-comparison -1 (t/isa? java.util.AbstractMap$SimpleEntry) (& (t/isa? java.util.Map$Entry) (t/isa? java.io.Serializable))))
        (testing "Interface"
          (test-comparison -1 i|a           (& i|>a0 i|>a1))))
      (testing "#{<}"
        (test-comparison -1 i|a           (& i|>a0 i|>a1)))
    #_(testing "#{< =}")         ; Impossible for `AndSpec`
    #_(testing "#{< = >}")       ; Impossible for `AndSpec`
    #_(testing "#{< = > ><}")    ; Impossible for `AndSpec`
    #_(testing "#{< = > >< <>}") ; Impossible for `AndSpec`
    #_(testing "#{< = > <>}")    ; Impossible for `AndSpec`
    #_(testing "#{< = ><}")      ; Impossible for `AndSpec`
    #_(testing "#{< = >< <>}")   ; Impossible for `AndSpec`
    #_(testing "#{< = <>}")      ; Impossible for `AndSpec`
    #_(testing "#{< >}")         ; Impossible for `AndSpec`
    #_(testing "#{< > ><}")      ; Impossible for `AndSpec`
    #_(testing "#{< > >< <>}")   ; Impossible for `AndSpec`
    #_(testing "#{< > <>}")      ; Impossible for `AndSpec`
      (testing "#{< ><}"
        (test-comparison  2 i|a            (& i|>a+b i|>a0 i|>a1 i|><0 i|><1)))
      (testing "#{< >< <>}"
        (test-comparison  2 t/java-set?    (& t/java-coll? t/char-seq? (t/isa? java.nio.ByteBuffer))))
      (testing "#{< <>}"
        (test-comparison  3 t/string?      (& t/char-seq? t/java-set?))
        (test-comparison  3 ><0            (& (! ><1) (! ><0)))
        (test-comparison  3 a              (& (! a)   (! b))))
    #_(testing "#{=}")           ; Impossible for `AndSpec`
    #_(testing "#{= >}")         ; Impossible for `AndSpec`
    #_(testing "#{= > ><}")      ; Impossible for `AndSpec`
    #_(testing "#{= > >< <>}")   ; Impossible for `AndSpec`
    #_(testing "#{= > <>}")      ; Impossible for `AndSpec`
      (testing "#{= ><}"
        (test-comparison  1 i|a            (& i|a i|><0 i|><1))
        (test-comparison  1 t/char-seq?    (& t/char-seq?   t/java-set?))
        (test-comparison  1 t/char-seq?    (& t/char-seq?   t/java-set? a)))
      (testing "#{= >< <>}") ; <- TODO comparison should be 1
      ;; TODO fix
      (testing "#{= <>}"
        (test-comparison  1 a              (& a t/java-set?)))
      (testing "#{>}"
        (test-comparison  1 i|a            (& i|<a+b i|<a0 i|<a1)))
      (testing "#{> ><}"
        (test-comparison  2 i|a            (& i|<a+b i|<a0 i|><0 i|><1))
        (test-comparison  2 a              (& (t/isa? javax.management.AttributeList) t/java-set?))
        (test-comparison  2 t/comparable?  (& (t/isa? java.nio.ByteBuffer) t/java-set?)))
      (testing "#{> >< <>}"
        (test-comparison  2 i|a            (& i|<a0 i|><0 a)))
      (testing "#{> <>}") ; <- TODO comparison should be 1
      (testing "#{><}"
        (test-comparison  2 i|a            (& i|><0 i|><1))
        (test-comparison  2 t/char-seq?    (& t/java-set? a)))
      (testing "#{>< <>}") ; <- TODO comparison should be 3
      (testing "#{<>}"
        (test-comparison  3 t/string?      (& a t/java-set?))))
    (testing "+ ValueSpec"
      (testing "#{<}"
        (test-comparison -1 (t/value "a")  (& t/char-seq? t/comparable?)))
    #_(testing "#{< =}")         ; not possible for `AndSpec`
    #_(testing "#{< = >}")       ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{< = > ><}")    ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< = > >< <>}") ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< = > <>}")    ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{< = ><}")      ; not possible for `AndSpec`; `><` not possible for `ValueSpec`
    #_(testing "#{< = >< <>}")   ; not possible for `AndSpec`; `><` not possible for `ValueSpec`
    #_(testing "#{< = <>}")      ; not possible for `AndSpec`
    #_(testing "#{< >}")         ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{< > ><}")      ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< > >< <>}")   ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{< > <>}")      ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{< ><}")        ; `><` not possible for `ValueSpec`
    #_(testing "#{< >< <>}")     ; `><` not possible for `ValueSpec`
      (testing "#{< <>}"
        (test-comparison  3 (t/value "a") (& t/char-seq? a))
        (test-comparison  3 (t/value "a") (& t/char-seq? t/java-set?)))
    #_(testing "#{=}")           ; not possible for `AndSpec`
    #_(testing "#{= >}")         ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{= > ><}")      ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{= > >< <>}")   ; not possible for `AndSpec`; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{= > <>}")      ; not possible for `AndSpec`; `>` not possible for `ValueSpec`
    #_(testing "#{= ><}")        ; `><` not possible for `ValueSpec`
    #_(testing "#{= >< <>}")     ; `><` not possible for `ValueSpec`
      (testing "#{= <>}")
    #_(testing "#{>}")           ; `>` not possible for `ValueSpec`
    #_(testing "#{> ><}")        ; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{> >< <>}")     ; `>` and `><` not possible for `ValueSpec`
    #_(testing "#{> <>}")        ; `>` not possible for `ValueSpec`
    #_(testing "#{><}")          ; `><` not possible for `ValueSpec`
    #_(testing "#{>< <>}")       ; `><` not possible for `ValueSpec`
      (testing "#{<>}"
        (test-comparison  3 (t/value "a") (& a t/java-set?)))))
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
      (test-comparison  0 (t/isa? AProtocolAll) (t/isa? AProtocolAll))
      (test-comparison  3 (t/isa? AProtocolAll) (t/isa? AProtocolNone)))
    (testing "+ ClassSpec")
    (testing "+ ValueSpec"
      (let [values #{t/universal-set t/empty-set nil {} 1 "" AProtocolAll
                     quantum.test.core.untyped.type.AProtocolAll}]
        (doseq [v values]
          (test-comparison -1 (t/value v) (t/isa? AProtocolAll)))
        (doseq [v [""]]
          (test-comparison -1 (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values "")]
          (test-comparison  3 (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values nil)]
          (test-comparison -1 (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-comparison  3 (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-comparison -1 (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v (disj values nil)]
          (test-comparison  3 (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v values]
          (test-comparison  3 (t/value v) (t/isa? AProtocolNone))))))
  (testing "ClassSpec"
    (testing "+ ClassSpec"
      (testing "Boxed Primitive + Boxed Primitive"
        (test-comparison 0 t/long? t/long?)
        (test-comparison 3 t/long? t/int?))
      (testing "Boxed Primitive + Final Concrete"
        (test-comparison 3 t/long? t/string?))
      (testing "Boxed Primitive + Extensible Concrete"
        (testing "< , >"
          (test-comparison -1  t/long? t/object?))
        (testing "<>"
          (test-comparison 3 t/long? t/thread?)))
      (testing "Boxed Primitive + Abstract"
        (test-comparison 3 t/long? (t/isa? java.util.AbstractCollection)))
      (testing "Boxed Primitive + Interface"
        (test-comparison 3 t/long? t/char-seq?))
      (testing "Final Concrete + Final Concrete"
        (test-comparison 0 t/string? t/string?))
      (testing "Final Concrete + Extensible Concrete"
        (testing "< , >"
          (test-comparison -1 t/string? t/object?))
        (testing "<>"
          (test-comparison  3 t/string? a)))
      (testing "Final Concrete + Abstract")
      (testing "Final Concrete + Interface"
        (testing "< , >"
          (test-comparison -1 t/string? t/comparable?))
        (testing "<>"
          (test-comparison  3 t/string? t/java-coll?)))
      (testing "Extensible Concrete + Extensible Concrete"
        (test-comparison 0 t/object? t/object?)
        (testing "< , >"
          (test-comparison -1 a t/object?))
        (testing "<>"
          (test-comparison  3 a t/thread?)))
      (testing "Extensible Concrete + Abstract"
        (testing "< , >"
          (test-comparison -1 (t/isa? java.util.AbstractCollection) t/object?)
          (test-comparison -1 a (t/isa? java.util.AbstractCollection)))
        (testing "<>"
          (test-comparison  3 t/thread? (t/isa? java.util.AbstractCollection))
          (test-comparison  3 (t/isa? java.util.AbstractCollection) t/thread?)))
      (testing "Extensible Concrete + Interface"
        (test-comparison 2 a t/char-seq?))
      (testing "Abstract + Abstract"
        (test-comparison 0 (t/isa? java.util.AbstractCollection) (t/isa? java.util.AbstractCollection))
        (testing "< , >"
          (test-comparison -1 (t/isa? java.util.AbstractList) (t/isa? java.util.AbstractCollection)))
        (testing "<>"
          (test-comparison  3 (t/isa? java.util.AbstractList) (t/isa? java.util.AbstractQueue))))
      (testing "Abstract + Interface"
        (testing "< , >"
          (test-comparison -1 (t/isa? java.util.AbstractCollection) t/java-coll?))
        (testing "><"
          (test-comparison  2 (t/isa? java.util.AbstractCollection) t/comparable?)))
      (testing "Interface + Interface"
        (testing "< , >"
          (test-comparison -1 t/java-coll? t/iterable?))
        (testing "><"
          (test-comparison  2 t/char-seq?  t/comparable?))))
    (testing "+ ValueSpec"
      (testing "<"
        (testing "Class equality"
          (test-comparison -1 (t/value "a") t/string?))
        (testing "Class inheritance"
          (test-comparison -1 (t/value "a") t/char-seq?)
          (test-comparison -1 (t/value "a") t/object?)))
      (testing "<>"
        (test-comparison 3 (t/value "a") t/byte?))))
  (testing "ValueSpec"
    (testing "+ ValueSpec"
      (testing "="
        (test-comparison 0 (t/value nil) (t/value nil))
        (test-comparison 0 (t/value 1  ) (t/value 1  ))
        (test-comparison 0 (t/value "a") (t/value "a")))
      (testing "=, non-strict"
        (test-comparison 0 (t/value (vector)         ) (t/value (list)          ))
        (test-comparison 0 (t/value (vector (vector))) (t/value (vector (list))))
        (test-comparison 0 (t/value (hash-map)       ) (t/value (sorted-map)    )))
      (testing "<>"
        (test-comparison 3 (t/value 1  ) (t/value 2  ))
        (test-comparison 3 (t/value "a") (t/value "b"))
        (test-comparison 3 (t/value 1  ) (t/value "a"))
        (test-comparison 3 (t/value nil) (t/value "a"))))))

(deftest test|not
  (testing "simplification"
    (testing "universal/null set"
      (is= (! t/universal-set)
           t/empty-set)
      (is= (! t/empty-set)
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
         t/empty-set))
  (testing "<"
    (is= (t/- a >a)
         t/empty-set))
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
    (is= (& t/universal-set t/empty-set)
         t/empty-set)
    (is= (& t/empty-set t/universal-set)
         t/empty-set)
    (is= (& t/universal-set t/empty-set t/universal-set)
         t/empty-set)
    (is= (& t/universal-set t/string?)
         t/string?)
    (is= (& t/universal-set t/char-seq? t/string?)
         t/string?)
    (is= (& t/universal-set t/string? t/char-seq?)
         t/string?)
    (is= (& t/empty-set t/string?)
         t/empty-set)
    (is= (& t/empty-set t/char-seq? t/string?)
         t/empty-set)
    (is= (& t/empty-set t/string? t/char-seq?)
         t/empty-set))
  (testing "simplification"
    (testing "via single-arg"
      (is= (& a)
           a))
    (testing "via identity"
      (is= (& a a)
           a)
      (is= (& (! a) (! a))
           (! a))
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
    (testing "empty-set"
      (is= (& a b)
           t/empty-set)
      (is= (& t/string? t/byte?)
           t/empty-set)
      (is= (& a ><0)
           t/empty-set)
      (is= (& a ><0 ><1)
           t/empty-set))
    (testing "nested `and` is expanded"
      (is= (& (& a b) (& ><0 ><1))
           (& a b ><0 ><1))
      (is= (& (& a b) (& ><0 ><1))
           (& a b ><0 ><1)))
    (testing "and + not"
      (is= (& a (! a))
           t/empty-set)
      (is= (& a (! b))
           a)
      (is= (& (! b) a)
           a)
      (testing "+ or"
        (is= (& (! a) a b)
             t/empty-set)
        (is= (& a (! a) b)
             t/empty-set)
        (is= (& a b (! a))
             t/empty-set)
        (is= (& (| a b) (! a))
             b)
        ;; TODO fix impls
        #_(is= (& (! a) (| a b))
             b)
        (is= (& (| a b) (! b) (| b a))
             a)
        (is= (& (| a b) (! b) (| ><0 b))
             t/empty-set))
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
  ;; Takes an inordinately long time to do `test-comparison 0 ...` even without instrumentation
  (is= (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
       (& (| t/boolean? t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
          (! t/boolean?)))
  (test-comparison 0 t/any? t/universal-set)
  (testing "universal class(-set) identity"
    (is (t/= t/val? (& t/any? t/val?)))))
