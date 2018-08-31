(ns quantum.test.untyped.core.type.compare
  (:require
    [clojure.core                               :as core]
    [quantum.untyped.core.analyze.expr          :as xp
      :refer [>expr]]
    [quantum.untyped.core.compare               :as ucomp]
    [quantum.untyped.core.data.hash             :as uhash]
    [quantum.untyped.core.fn
      :refer [fn1]]
    [quantum.untyped.core.logic
      :refer [ifs]]
    [quantum.untyped.core.numeric               :as unum]
    [quantum.untyped.core.numeric.combinatorics :as ucombo]
    [quantum.untyped.core.spec                  :as s]
    [quantum.untyped.core.test
      :refer [deftest testing is is= throws]]
    [quantum.untyped.core.type                  :as t
      :refer [& | !]]
    [quantum.untyped.core.type.compare          :as tcomp]
    [quantum.untyped.core.type.reifications     :as utr]
    [quantum.untyped.core.defnt
      :refer [defns]]))

;; Here, `NotType` labels on `testing` mean such *after* simplification

#?(:clj
(defmacro test-comparisons>comparisons [[_ _ a b]]
  `[[~@(for [a* (rest a)]
         `(t/compare ~a* ~b))]
    [~@(for [b* (rest b)]
         `(t/compare ~b* ~a))]]))

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

(def protocol-types
  (->> [AProtocolAll AProtocolString AProtocolNonNil AProtocolOnlyNil AProtocolNone]
       (map t/>type) set))

)

;; TESTS ;;

(defns type>type-combos
  "To generate all commutative possibilities for a given type."
  [t t/type? > (s/seq-of t/type?)]
  (ifs (t/and-type? t) (->> t utr/and-type>args ucombo/permutations
                              (map #(utr/->AndType uhash/default uhash/default (vec %) (atom nil))))
       (t/or-type?  t) (->> t utr/or-type>args  ucombo/permutations
                              (map #(utr/->OrType  uhash/default uhash/default (vec %) (atom nil))))
       [t]))

#?(:clj
(defmacro test-comparison
  "Performs a `t/compare` on `a` and `b`, ensuring that their relationship is symmetric, and that
   the inputs are internally commutative if applicable (e.g. if `a` is an `AndType`, ensures that
   it is commutative).
   The basis comparison is the first input."
  [c #_t/comparisons a #_t/type? b #_t/type?]
  `(let [c# ~c]
     (doseq ;; Commutativity
            [a*# (type>type-combos ~a)
             b*# (type>type-combos ~b)]
       ;; Symmetry
       (is= c#                (t/compare a*# b*#))
       (is= (ucomp/invert c#) (t/compare b*# a*#))))))

#?(:clj
(defmacro test-comparison|fn
  "Performs a `tcomp/compare|input` and `tcomp/compare|output` on `a` and `b`, ensuring that the
   comparison-relationship between `a` and `b` is symmetric.
   The basis comparison is the first input."
  [[c|out #_t/comparisons, c|in #_t/comparisons] #__, a #_t/type? b #_t/type?]
  `(let [c|out# ~c|out, c|in# ~c|in, a# ~a, b# ~b]
     ;; Symmetry
     (is= c|in#                 (tcomp/compare|in  a# b#))
     (is= (ucomp/invert c|in#)  (tcomp/compare|in  b# a#))
     (is= c|out#                (tcomp/compare|out a# b#))
     (is= (ucomp/invert c|out#) (tcomp/compare|out b# a#)))))

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
  (testing "UniversalSetType"
    (testing "+ UniversalSetType"
      (test-comparison  0 t/universal-set t/universal-set))
    (testing "+ EmptySetType"
      (test-comparison  1 t/universal-set t/empty-set))
    (testing "+ NotType"
      (test-comparison  1 t/universal-set (! a)))
    (testing "+ OrType"
      (test-comparison  1 t/universal-set (| ><0 ><1)))
    (testing "+ AndType")
    (testing "+ Expression")
    (testing "+ ProtocolType"
      (doseq [t protocol-types]
        (test-comparison 1 t/universal-set t)))
    (testing "+ ClassType")
    (testing "+ ValueType"
      (doseq [t [(t/value t/universal-set)
                 (t/value t/empty-set)
                 (t/value 0)
                 (t/value nil)]]
        (test-comparison 1 t/universal-set t))))
  ;; The null set is considered to always (vacuously) be a subset of any set
  (testing "EmptySetType"
    (testing "+ EmptySetType"
      (test-comparison 0 t/empty-set t/empty-set))
    (testing "+ NotType"
      (testing "Inner ClassType"
        (test-comparison -1 t/empty-set (! a)))
      (testing "Inner ValueType"
        (test-comparison -1 t/empty-set (! (t/value 1)))))
    (testing "+ OrType"
      (test-comparison -1 t/empty-set (| ><0 ><1)))
    (testing "+ AndType")
    (testing "+ Expression")
    (testing "+ ProtocolType"
      (doseq [t protocol-types]
        (test-comparison -1 t/empty-set t)))
    (testing "+ ClassType")
    (testing "+ ValueType"
      (test-comparison -1 t/empty-set (t/value t/empty-set))
      (test-comparison -1 t/empty-set (t/value 0))))
  (testing "NotType"
    (testing "+ NotType"
      (test-comparison  0 (! a)           (! a))
      (test-comparison  2 (! a)           (! b))
      (test-comparison  2 (! i|a)         (! i|b))
      (test-comparison  2 (! t/string?)   (! t/byte?))
      (test-comparison  1 (! a)           (! >a))
      (test-comparison -1 (! a)           (! <a0))
      (test-comparison  0 (! (t/value 1)) (! (t/value 1)))
      (test-comparison  2 (! (t/value 1)) (! (t/value 2))))
    ;; TODO continue to implement
    (testing "+ OrType"
      (testing "#{<}"
        ;; TODO Technically something like this but can't do the below b/c of simplification
        #_(test-comparison -1 (! a) (| (| (! a) <a0) (| (! a) <a1))))
    #_(testing "#{< =}")         ; Impossible for `OrType`
    #_(testing "#{< = >}")       ; Impossible for `OrType`
    #_(testing "#{< = > ><}")    ; Impossible for `OrType`
    #_(testing "#{< = > >< <>}") ; Impossible for `OrType`
    #_(testing "#{< = > <>}")    ; Impossible for `OrType`
    #_(testing "#{< = ><}")      ; Impossible for `OrType`
    #_(testing "#{< = >< <>}")   ; Impossible for `OrType`
    #_(testing "#{< = <>}")      ; Impossible for `OrType`
    #_(testing "#{< >}")         ; Impossible for `OrType`
    #_(testing "#{< > ><}")      ; Impossible for `OrType`
    #_(testing "#{< > >< <>}")   ; Impossible for `OrType`
    #_(testing "#{< > <>}")      ; Impossible for `OrType`
      (testing "#{< ><}"
        #_(test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1))
        #_(test-comparison -1 i|>a0 (| i|>a+b i|>a0)))
      (testing "#{< >< <>}"
        #_(test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1 t/string?)))
      (testing "#{< <>}"
        #_(test-comparison -1 a     (| >a ><0 ><1)))
    #_(testing "#{=}")           ; Impossible for `OrType`
    #_(testing "#{= >}")         ; Impossible for `OrType`
    #_(testing "#{= > ><}")      ; Impossible for `OrType`
    #_(testing "#{= > >< <>}")   ; Impossible for `OrType`
    #_(testing "#{= > <>}")      ; Impossible for `OrType`
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
    #_(testing "+ AndType"
      (testing "inner #{= <>}"
        (test-comparison ... (! a) (& a (! b)))))
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType"
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
    (testing "+ ValueType"
      (test-comparison -1 (t/value 1)  (! (t/value 2)))
      (test-comparison  3 (t/value "") (! t/string?))))
  (testing "OrType"
    (testing "+ OrType"
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
    #_(testing "+ AndType"
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
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType"
      (testing "#{<}"
        (test-comparison -1 i|<a0 (| i|>a+b i|>a0 i|>a1)))
    #_(testing "#{< =}")         ; Impossible for `OrType`
    #_(testing "#{< = >}")       ; Impossible for `OrType`
    #_(testing "#{< = > ><}")    ; Impossible for `OrType`
    #_(testing "#{< = > >< <>}") ; Impossible for `OrType`
    #_(testing "#{< = > <>}")    ; Impossible for `OrType`
    #_(testing "#{< = ><}")      ; Impossible for `OrType`
    #_(testing "#{< = >< <>}")   ; Impossible for `OrType`
    #_(testing "#{< = <>}")      ; Impossible for `OrType`
    #_(testing "#{< >}")         ; Impossible for `OrType`
    #_(testing "#{< > ><}")      ; Impossible for `OrType`
    #_(testing "#{< > >< <>}")   ; Impossible for `OrType`
    #_(testing "#{< > <>}")      ; Impossible for `OrType`
      (testing "#{< ><}"
        (test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1))
        (test-comparison -1 i|>a0 (| i|>a+b i|>a0)))
      (testing "#{< >< <>}"
        (test-comparison -1 i|a   (| i|>a+b i|>a0 i|><0 i|><1 t/string?)))
      (testing "#{< <>}"
        (test-comparison -1 a     (| >a ><0 ><1)))
    #_(testing "#{=}")           ; Impossible for `OrType`
    #_(testing "#{= >}")         ; Impossible for `OrType`
    #_(testing "#{= > ><}")      ; Impossible for `OrType`
    #_(testing "#{= > >< <>}")   ; Impossible for `OrType`
    #_(testing "#{= > <>}")      ; Impossible for `OrType`
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
    (testing "+ ValueType"
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
  (testing "AndType"
    (testing "+ AndType")
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType"
      (testing "#{<}"
        (testing "Boxed Primitive"
          (test-comparison -1 t/byte?        (& t/number?   t/comparable?)))
        (testing "Final Concrete"
          (test-comparison -1 t/string?      (& t/char-seq? t/comparable?)))
        (testing "Extensible Concrete"
          (test-comparison -1 a (& t/iterable? (t/isa? java.util.RandomAccess))))
        (testing "Abstract"
          (test-comparison -1 (t/isa? java.util.AbstractMap$SimpleEntry)
                              (& (t/isa? java.util.Map$Entry) (t/isa? java.io.Serializable))))
        (testing "Interface"
          (test-comparison -1 i|a           (& i|>a0 i|>a1))))
      (testing "#{<}"
        (test-comparison -1 i|a           (& i|>a0 i|>a1)))
    #_(testing "#{< =}")         ; Impossible for `AndType`
    #_(testing "#{< = >}")       ; Impossible for `AndType`
    #_(testing "#{< = > ><}")    ; Impossible for `AndType`
    #_(testing "#{< = > >< <>}") ; Impossible for `AndType`
    #_(testing "#{< = > <>}")    ; Impossible for `AndType`
    #_(testing "#{< = ><}")      ; Impossible for `AndType`
    #_(testing "#{< = >< <>}")   ; Impossible for `AndType`
    #_(testing "#{< = <>}")      ; Impossible for `AndType`
    #_(testing "#{< >}")         ; Impossible for `AndType`
    #_(testing "#{< > ><}")      ; Impossible for `AndType`
    #_(testing "#{< > >< <>}")   ; Impossible for `AndType`
    #_(testing "#{< > <>}")      ; Impossible for `AndType`
      (testing "#{< ><}"
        (test-comparison  2 i|a            (& i|>a+b i|>a0 i|>a1 i|><0 i|><1)))
      (testing "#{< >< <>}"
        (test-comparison  2 t/java-set?    (& t/java-coll? t/char-seq?
                                              (t/isa? java.nio.ByteBuffer))))
      (testing "#{< <>}"
        (test-comparison  3 t/string?      (& t/char-seq? t/java-set?))
        (test-comparison  3 ><0            (& (! ><1) (! ><0)))
        (test-comparison  3 a              (& (! a)   (! b))))
    #_(testing "#{=}")           ; Impossible for `AndType`
    #_(testing "#{= >}")         ; Impossible for `AndType`
    #_(testing "#{= > ><}")      ; Impossible for `AndType`
    #_(testing "#{= > >< <>}")   ; Impossible for `AndType`
    #_(testing "#{= > <>}")      ; Impossible for `AndType`
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
    (testing "+ ValueType"
      (testing "#{<}"
        (test-comparison -1 (t/value "a")  (& t/char-seq? t/comparable?)))
    #_(testing "#{< =}")         ; not possible for `AndType`
    #_(testing "#{< = >}")       ; not possible for `AndType`; `>` not possible for `ValueType`
    #_(testing "#{< = > ><}")    ; not possible for `AndType`; `>` and `><` not possible for `ValueType`
    #_(testing "#{< = > >< <>}") ; not possible for `AndType`; `>` and `><` not possible for `ValueType`
    #_(testing "#{< = > <>}")    ; not possible for `AndType`; `>` not possible for `ValueType`
    #_(testing "#{< = ><}")      ; not possible for `AndType`; `><` not possible for `ValueType`
    #_(testing "#{< = >< <>}")   ; not possible for `AndType`; `><` not possible for `ValueType`
    #_(testing "#{< = <>}")      ; not possible for `AndType`
    #_(testing "#{< >}")         ; not possible for `AndType`; `>` not possible for `ValueType`
    #_(testing "#{< > ><}")      ; not possible for `AndType`; `>` and `><` not possible for `ValueType`
    #_(testing "#{< > >< <>}")   ; not possible for `AndType`; `>` and `><` not possible for `ValueType`
    #_(testing "#{< > <>}")      ; not possible for `AndType`; `>` not possible for `ValueType`
    #_(testing "#{< ><}")        ; `><` not possible for `ValueType`
    #_(testing "#{< >< <>}")     ; `><` not possible for `ValueType`
      (testing "#{< <>}"
        (test-comparison  3 (t/value "a") (& t/char-seq? a))
        (test-comparison  3 (t/value "a") (& t/char-seq? t/java-set?)))
    #_(testing "#{=}")           ; not possible for `AndType`
    #_(testing "#{= >}")         ; not possible for `AndType`; `>` not possible for `ValueType`
    #_(testing "#{= > ><}")      ; not possible for `AndType`; `>` and `><` not possible for `ValueType`
    #_(testing "#{= > >< <>}")   ; not possible for `AndType`; `>` and `><` not possible for `ValueType`
    #_(testing "#{= > <>}")      ; not possible for `AndType`; `>` not possible for `ValueType`
    #_(testing "#{= ><}")        ; `><` not possible for `ValueType`
    #_(testing "#{= >< <>}")     ; `><` not possible for `ValueType`
      (testing "#{= <>}")
    #_(testing "#{>}")           ; `>` not possible for `ValueType`
    #_(testing "#{> ><}")        ; `>` and `><` not possible for `ValueType`
    #_(testing "#{> >< <>}")     ; `>` and `><` not possible for `ValueType`
    #_(testing "#{> <>}")        ; `>` not possible for `ValueType`
    #_(testing "#{><}")          ; `><` not possible for `ValueType`
    #_(testing "#{>< <>}")       ; `><` not possible for `ValueType`
      (testing "#{<>}"
        (test-comparison  3 (t/value "a") (& a t/java-set?)))))
  (testing "Expression"
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType")
    (testing "+ ValueType"))
  (testing "ProtocolType"
    (testing "+ ProtocolType"
      (test-comparison  0 (t/isa? AProtocolAll) (t/isa? AProtocolAll))
      (test-comparison  3 (t/isa? AProtocolAll) (t/isa? AProtocolNone)))
    (testing "+ ClassType")
    (testing "+ ValueType"
      (let [values #{t/universal-set t/empty-set nil {} 1 "" AProtocolAll
                     quantum.test.untyped.core.type.compare.AProtocolAll}]
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
  (testing "ClassType"
    (testing "+ ClassType"
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
    (testing "+ ValueType"
      (testing "<"
        (testing "Class equality"
          (test-comparison -1 (t/value "a") t/string?))
        (testing "Class inheritance"
          (test-comparison -1 (t/value "a") t/char-seq?)
          (test-comparison -1 (t/value "a") t/object?)))
      (testing "<>"
        (test-comparison 3 (t/value "a") t/byte?))))
  (testing "ValueType"
    (testing "+ ValueType"
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

(deftest test|=
  ;; Takes an inordinately long time to do `test-comparison 0 ...` even without instrumentation
  (is= (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
       (& (| t/boolean? t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
          (! t/boolean?)))
  (test-comparison 0 t/any? t/universal-set)
  (testing "universal class(-set) identity"
    (is (t/= t/val? (& t/any? t/val?)))))

;; TODO incorporate into the other test?
(deftest test|fn
  #_"When we compare a t/fn to another t/fn, we are comparing set extensionality, as always.
     If we take the Wiener–Hausdorff–Kuratowski definition of a function as our definition of
     choice, then we may model a function as a set of ordered pairs, each of whose first element
     consists of an ordered tuple of inputs, and whose second element consists of one output. Thus
     under this model, if we wish to compare the extension of two functions, it would be in error
     to compare the extension of their inputs and the extension of their outputs separately.

     That said, it's not clear how useful this sort of comparison is.
     Furthermore, is it the case that `(t/< [[] t/any?] (t/fn []))`? Intuitively it doesn't seem
     like it should be, but under the WHK model it nevertheless seems to be the case.

     So we opt to make `t/fn`s `t/compare`-able only with what its underlying function object is
     `t/compare`-able with, and introduce instead a `t/compare|input` and `t/compare|output`.
     See `quantum.test.untyped.core.type.compare` for how these sorts of comparisons are supposed
     to behave.
     "
  ;; [0 1 2] means t/compare|input is 0, t/compare|output is 1, and t/compare is 2
    "Liskov’s Substitution Principle
     Contract satisfaction ('Growth') is `t/<=|input` (you cannot require more) and `t/>=|output`
     (you cannot guarantee less)
     - Inputs
       - I require an animal and you give me a sheep:
         - `(t/<= sheep? animal?)`
       - If I require an animal and you give me a sheep and some wheat, it has to be in an
         acceptable open container of some sort (generally a map) because the caller is not
         guaranteed to know how to handle it otherwise:
         - `(t/<> (t/tuple sheep? wheat?) animal?)
         - `(t/<> (t/map :requirement sheep? :extra0 wheat?) animal?)
         - `(t/<= (t/closed-map :requirement sheep? :extra0 wheat?)
                  (t/merge (t/closed-map :requirement animal?) (t/map-of t/keyword? t/any?)))
         - `(t/<= (t/map :requirement sheep? :extra0 wheat?)
                  (t/map :requirement animal?))
     - Outputs
       - I guarantee an animal and I provide a sheep:
         - `(t/<= sheep? animal?)`
       - If I guarantee an animal and I provide a sheep and some wheat, it has to be in an
         acceptable open container of some sort (generally a map) because the caller is not
         guaranteed to know how to handle it otherwise:
         - `(t/<> (t/tuple sheep? wheat?) animal?)
         - `(t/<> (t/map :guarantee sheep? :extra0 wheat?) animal?)
         - `(t/<= (t/closed-map :guarantee sheep? :extra0 wheat?)
                  (t/merge (t/closed-map :requirement animal?) (t/map-of t/keyword? t/any?)))
     Contract non-satisfaction ('Breakage') is `>=|input` (input covariance) and `t/<=|output`
     (output contravariance)
     - Inputs
       - I require an animal but you give me any old organism
     - Outputs
       - I guarantee an animal but I provide any old organism
       - I guarantee a sheep and some wheat but I provide only a sheep
         - (t/?? (t/map :guarantee))"

  ;; For comparing arities:
  ;; (This uses set/difference in both directions)
  ;; (set/compare (-> f0 fn>arities (map count) set) (-> f1 fn>arities (map count) set))

  (testing "output <"
    (testing "input <"
      (test-comparison|fn [-1 -1] (t/fn [t/boolean? :> t/boolean?])
                                  (t/fn []
                                        [t/any?])))
    (testing "input =")
    (testing "input >")
    (testing "input ><")
    (testing "input <>"))
  (testing "output ="
    (testing "input <"
      (testing "due to input arity <"
        (test-comparison|fn [ 0 -1] (t/fn [t/any?])
                                    (t/fn []
                                          [t/any?])))
      (testing "due to input types <"
        (test-comparison|fn [ 0 -1] (t/fn []
                                          [t/boolean?])
                                    (t/fn []
                                          [t/any?])))
      (testing "due to input arity and types <"
        (test-comparison|fn [ 0 -1] (t/fn [t/boolean?])
                                    (t/fn []
                                          [t/any?]))))
    (testing "input ="
      (test-comparison|fn [ 0  0] (t/fn [])
                                  (t/fn [])))
    (testing "input >")
    (testing "input ><")
    (testing "input <>"))
  (testing "output >"
    (testing "input <"
      (testing "due to input arity <"
        (test-comparison|fn [ 1 -1] (t/fn [t/any?])
                                    (t/fn []
                                          [t/any? :> t/boolean?])))
      (testing "due to input types <"
        (test-comparison|fn [ 1 -1] (t/fn []
                                          [t/boolean?])
                                    (t/fn []
                                          [t/any? :> t/boolean?])))
      (testing "due to input arity and types <"
        (test-comparison|fn [ 1 -1] (t/fn [t/boolean?])
                                    (t/fn []
                                          [t/any? :> t/boolean?]))))
    (testing "input ="
      (test-comparison|fn [ 1  0] (t/fn [:> t/boolean?])
                                  (t/fn []))
      (test-comparison|fn [ 1  0] (t/fn [:> t/boolean?]
                                        [t/any? :> t/boolean?])
                                  (t/fn []
                                        [t/any?])))
    (testing "input >")
    (testing "input ><")
    (testing "input <>"))
  (testing "output ><"
    (testing "input <"
      (test-comparison|fn [ 2 -1] (t/fn [t/boolean? :> i|><0])
                                  (t/fn []
                                        [t/any?     :> i|><1])))
    (testing "input =")
    (testing "input >")
    (testing "input ><")
    (testing "input <>"))
  (testing "output <>"
    (testing "input <")
    (testing "input =")
    (testing "input >")
    (testing "input ><")
    (testing "input <>"))

  (testing "input arities <"
    (testing "input types <"
      (testing "output <>"
        (test-comparison|fn [-1  3  ?] (t/fn                 [t/boolean? :> t/boolean?])
                                       (t/fn []              [t/any?     :> t/long?]))))
    (testing "input types ="
      (testing "output <"
        (test-comparison|fn [-1 -1  ?] (t/fn [:> t/boolean?])
                                       (t/fn []              [t/any?])))
      (testing "output ="
        (test-comparison|fn [-1  0  ?] (t/fn [])
                                       (t/fn []              [t/any?])))
      (testing "output >"
        (test-comparison|fn [-1  1  ?] (t/fn [])
                                       (t/fn [:> t/boolean?] [t/any? :> t/long?])))
      (testing "output ><")
      (testing "output <>"))
    (testing "input types >"
      (testing "output <"
        (test-comparison|fn [ 2 -1  ?] (t/fn    [t/any?])
                                       (t/fn [] [t/long?])))
      (testing "output ="
        (test-comparison|fn [ 2  0  ?] (t/fn                [t/any?])
                                       (t/fn []             [t/boolean?])))
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "input types ><"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "input types <>"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>")))
  (testing "input arities ="
    (testing "input types <"
      (testing "output <"
        (test-comparison|fn [-1 -1 -1] (t/fn [t/boolean? :> t/boolean?])
                                       (t/fn [t/any?])))
      (testing "output ="
        (test-comparison|fn [-1  0 -1] (t/fn [t/boolean?])
                                       (t/fn [t/any?])))
      (testing "output >"
        (test-comparison|fn [-1  1  2] (t/fn [t/boolean?])
                                       (t/fn [t/any?     :> t/boolean?])))
      (testing "output ><"
        (test-comparison|fn [-1  2  2] (t/fn [t/boolean? :> i|><0])
                                       (t/fn [t/any?     :> i|><1])))
      (testing "output <>"
        (test-comparison|fn [-1  3  ?] (t/fn [t/boolean? :> i|><0])
                                       (t/fn [t/any?     :> i|><1]))))
    (testing "input types ="
      (testing "output <")
      (testing "output ="
        )
      (testing "output >"
        )
      (testing "output ><")
      (testing "output <>"))

(require '[quantum.untyped.core.data.bits :as ubit])
(let [cs [0 0]]
  (first
    (reduce
      (fn [[ret found] c]
        (let [found' (-> found (ubit/conj c) long)]
          (ifs (ubit/contains? found' ucomp/<ident)
               )


          (ifs (or (ubit/contains? found' ucomp/<ident)
                   (ubit/contains? found' ucomp/=ident))
               (reduced [ucomp/<ident found'])

               (or (ubit/contains? found' ucomp/><ident)
                   (and (ubit/contains? found' ucomp/>ident)
                        (ubit/contains? found' ucomp/<>ident)))
               [ucomp/><ident found']

               [c found'])))
      [(first cs) ubit/empty]
      (rest cs))))


(defns compare|input [x0 t/fnt-type?, x1 t/fnt-type?]
  (let [ct->arity|x0 (->> x0 fn>arities (group-by arity>count) (c/map-vals' first))
        ct->arity|x1 (->> x1 fn>arities (group-by arity>count) (c/map-vals' first))
        arity-cts-only-in-x0 (uset/- (-> ct->arity|x0 keys set) (-> ct->arity|x1 keys set))
        arity-cts-only-in-x1 (uset/- (-> ct->arity|x1 keys set) (-> ct->arity|x0 keys set))]
    (->> ct->arity|x0
         (filter (fn-> first ct->arity|x1))
         (map (fn [ct arity|x0] (combine-in-some-way
                                  (c/lmap t/compare arity|x0 (ct->arity|x1 ct)))))
         combine-in-some-possibly-other-way)))

(defns compare|output [x0 t/fnt-type?, x1 t/fnt-type?]
  (t/compare (->> x0 fn>arities (c/lmap fn|arity>output) (apply t/or))
             (->> x1 fn>arities (c/lmap fn|arity>output) (apply t/or))))

(defns compare|fn+fn [x0 t/fnt-type?, x1 t/fnt-type?]
  (combine-comparisons-in-a-tand???-sort-of-way ; maybe the combination is similar (or the same?) to the above not-yet-fleshed-out combination fns
    (compare|input  x0 x1)
    (compare|output x0 x1)))
