(ns quantum.test.untyped.core.type.compare
  (:require
    [clojure.core                               :as core]
    [quantum.test.untyped.core.type
      :refer [i|>a+b i|>a0 i|>a1 i|>b0 i|>b1
              i|a i|b
              i|<a+b i|<a0 i|<a1 i|<b0 i|<b1
              i|><0 i|><1 i|><2

              >a+b >a >b
              a b
              <a0 <a1 <b0 <b1
              ><0 ><1 ><2]]
    [quantum.untyped.core.analyze.expr          :as xp
      :refer [>expr]]
    [quantum.untyped.core.collections           :as c]
    [quantum.untyped.core.compare               :as ucomp
      :refer [<ident =ident >ident ><ident <>ident]]
    [quantum.untyped.core.data.hash             :as uhash]
    [quantum.untyped.core.data.set              :as uset]
    [quantum.untyped.core.defnt
      :refer [defns]]
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
    [quantum.untyped.core.type.reifications     :as utr]))

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

;; TESTS ;;

(defns type>type-combos
  "To generate all commutative possibilities for a given type."
  [t t/type? > (s/seq-of t/type?)]
  (ifs (t/and-type? t) (->> t utr/and-type>args ucombo/permutations
                              (map #(utr/->AndType uhash/default uhash/default nil (vec %)
                                      (atom nil))))
       (t/or-type?  t) (->> t utr/or-type>args  ucombo/permutations
                              (map #(utr/->OrType  uhash/default uhash/default nil (vec %)
                                      (atom nil))))
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
  "Performs a `t/compare|in` and `t/compare|out` on `a` and `b`, ensuring that the
   comparison-relationship between `a` and `b` is symmetric.
   The basis comparison is the first input."
  [[c|in #_t/comparisons, c|out #_t/comparisons] #__, a #_t/type? b #_t/type?]
  `(let [c|out# ~c|out, c|in# ~c|in, a# ~a, b# ~b]
     ;; Symmetry
     (is= c|in#                 (t/compare|in  a# b#))
     (is= (ucomp/invert c|in#)  (t/compare|in  b# a#))
     (is= c|out#                (t/compare|out a# b#))
     (is= (ucomp/invert c|out#) (t/compare|out b# a#)))))

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
      (test-comparison =ident t/universal-set t/universal-set))
    (testing "+ EmptySetType"
      (test-comparison >ident t/universal-set t/empty-set))
    (testing "+ NotType"
      (test-comparison >ident t/universal-set (! a)))
    (testing "+ OrType"
      (test-comparison >ident t/universal-set (| ><0 ><1)))
    (testing "+ AndType")
    (testing "+ Expression")
    (testing "+ ProtocolType"
      (doseq [t protocol-types]
        (test-comparison >ident t/universal-set t)))
    (testing "+ ClassType")
    (testing "+ ValueType"
      (doseq [t [(t/value t/universal-set)
                 (t/value t/empty-set)
                 (t/value 0)
                 (t/value nil)]]
        (test-comparison >ident t/universal-set t))))
  ;; The null set is considered to always (vacuously) be a subset of any set
  (testing "EmptySetType"
    (testing "+ EmptySetType"
      (test-comparison =ident t/empty-set t/empty-set))
    (testing "+ NotType"
      (testing "Inner ClassType"
        (test-comparison <ident t/empty-set (! a)))
      (testing "Inner ValueType"
        (test-comparison <ident t/empty-set (! (t/value 1)))))
    (testing "+ OrType"
      (test-comparison <ident t/empty-set (| ><0 ><1)))
    (testing "+ AndType")
    (testing "+ Expression")
    (testing "+ ProtocolType"
      (doseq [t protocol-types]
        (test-comparison <ident t/empty-set t)))
    (testing "+ ClassType")
    (testing "+ ValueType"
      (test-comparison <ident t/empty-set (t/value t/empty-set))
      (test-comparison <ident t/empty-set (t/value 0))))
  (testing "NotType"
    (testing "+ NotType"
      (test-comparison  =ident (! a)           (! a))
      (test-comparison ><ident (! a)           (! b))
      (test-comparison ><ident (! i|a)         (! i|b))
      (test-comparison ><ident (! t/string?)   (! t/byte?))
      (test-comparison  >ident (! a)           (! >a))
      (test-comparison  <ident (! a)           (! <a0))
      (test-comparison  =ident (! (t/value 1)) (! (t/value 1)))
      (test-comparison ><ident (! (t/value 1)) (! (t/value 2))))
    ;; TODO continue to implement
    (testing "+ OrType"
      (testing "#{<}"
        ;; TODO Technically something like this but can't do the below b/c of simplification
        #_(test-comparison <ident (! a) (| (| (! a) <a0) (| (! a) <a1))))
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
        #_(test-comparison <ident i|a   (| i|>a+b i|>a0 i|><0 i|><1))
        #_(test-comparison <ident i|>a0 (| i|>a+b i|>a0)))
      (testing "#{< >< <>}"
        #_(test-comparison <ident i|a   (| i|>a+b i|>a0 i|><0 i|><1 t/string?)))
      (testing "#{< <>}"
        #_(test-comparison <ident a     (| >a ><0 ><1)))
    #_(testing "#{=}")           ; Impossible for `OrType`
    #_(testing "#{= >}")         ; Impossible for `OrType`
    #_(testing "#{= > ><}")      ; Impossible for `OrType`
    #_(testing "#{= > >< <>}")   ; Impossible for `OrType`
    #_(testing "#{= > <>}")      ; Impossible for `OrType`
      (testing "#{= ><}"
        (test-comparison <ident (! a)   (| (! a)   i|><0 i|><1))
        (test-comparison <ident (! i|a) (| (! i|a) i|><0 i|><1)))
      (testing "#{= >< <>}"
        #_(test-comparison <ident i|a   (| i|a i|><0 i|><1 t/string?)))
      (testing "#{= <>}"
        (test-comparison <ident (! a) (| (! a) <a0))
        (test-comparison <ident (! a) (| (! a) <a1))
        (test-comparison <ident (! a) (| (! a) <a0 <a1)))
      (testing "#{>}"
        #_(test-comparison  >ident a     (| <a0 <a1))
        #_(test-comparison  >ident i|a   (| i|<a0 i|<a1)))
      (testing "#{> ><}"
        #_(test-comparison ><ident i|a   (| i|<a+b i|<a0 i|><0 i|><1)))
      (testing "#{> >< <>}"
        #_(test-comparison ><ident i|a   (| i|<a+b i|<a0 i|><0 i|><1 t/string?)))
      (testing "#{> <>}"
        (test-comparison ><ident (! a)   (| b a))
        (test-comparison ><ident (! b)   (| a b))
        (test-comparison ><ident (! ><0) (| ><0 ><1))
        (test-comparison ><ident (! ><1) (| ><1 ><0)))
      (testing "#{><}"
        #_(test-comparison ><ident i|a   (| i|><0 i|><1)))
      (testing "#{>< <>}"
        #_(test-comparison ><ident i|a   (| i|><0 i|><1 t/string?)))
      (testing "#{<>}"
        (test-comparison <>ident (! a)   (| <a0 <a1))))
    ;; TODO
    #_(testing "+ AndType"
      (testing "inner #{= <>}"
        (test-comparison ... (! a) (& a (! b)))))
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType"
      (test-comparison <>ident (!     a)     a) ; inner =
      (test-comparison <>ident (!   i|a)   i|a) ; inner =
      (test-comparison <>ident (!     a)   <a0) ; inner >
      (test-comparison <>ident (!   i|a) i|<a0) ; inner >
      (test-comparison ><ident (!     a)    >a) ; inner <
      (test-comparison ><ident (!   i|a) i|>a0) ; inner ><
      (test-comparison  >ident (!   a  )   ><0) ; inner <>
      (test-comparison ><ident (!   i|a) i|><0) ; inner ><
      (test-comparison ><ident (!     a)    Uc) ; inner <
      (test-comparison ><ident (!   i|a)    Uc) ; inner <
      (test-comparison ><ident (!   <a0)     a) ; inner <
      (test-comparison ><ident (! i|<a0)   i|a) ; inner <
      (test-comparison ><ident (!   <a0)    >a) ; inner <
      (test-comparison ><ident (! i|<a0) i|>a0) ; inner <
      (test-comparison  >ident (!   <a0)   ><0) ; inner <>
      (test-comparison ><ident (! i|<a0) i|><0) ; inner ><
      (test-comparison ><ident (!   <a0)    Uc) ; inner <
      (test-comparison ><ident (! i|<a0)    Uc) ; inner <
      (test-comparison <>ident (!    >a)     a) ; inner >
      (test-comparison <>ident (! i|>a0)   i|a) ; inner >
      (test-comparison <>ident (!    >a)   <a0) ; inner >
      (test-comparison <>ident (! i|>a0) i|<a0) ; inner >
      (test-comparison  >ident (!    >a)   ><0) ; inner <>
      (test-comparison ><ident (! i|>a0) i|><0) ; inner ><
      (test-comparison ><ident (!    >a)    Uc) ; inner <
      (test-comparison ><ident (! i|>a0)    Uc) ; inner <
      (test-comparison  >ident (!   ><0)     a) ; inner <>
      (test-comparison ><ident (! i|><0)   i|a) ; inner ><
      (test-comparison  >ident (!   ><0)   <a0) ; inner <>
      (test-comparison ><ident (! i|><0) i|<a0) ; inner ><
      (test-comparison  >ident (!   ><0)    >a) ; inner <>
      (test-comparison ><ident (! i|><0) i|>a0) ; inner ><
      (test-comparison ><ident (!   ><0)    Uc) ; inner <
      (test-comparison ><ident (! i|><0)    Uc) ; inner <
    (testing "+ ValueType"
      (test-comparison  <ident (t/value 1)  (! (t/value 2)))
      (test-comparison <>ident (t/value "") (! t/string?))))
  (testing "OrType"
    (testing "+ OrType"
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      ;; TODO add complete comparisons via `comparison-combinations`
      (testing "#{<}, #{<}"
        ;; comparisons:             < <                        < <
        (test-comparison  =ident (| a b)                    (| a b))
        ;; comparisons:                 <      <               <      <
        (test-comparison  =ident (|     i|>a+b i|>a0)       (| i|>a+b i|>a0)))
      (testing "#{<}, #{<, ><}"
        ;; comparisons:                 <      <               <      <     ><    ><
        (test-comparison  <ident (|     i|>a+b i|>a0)       (| i|>a+b i|>a0 i|><0 i|><1))
        ;; comparisons:                 <      <               <      <     ><    ><    ><
        (test-comparison  <ident (|     i|>a+b i|>a0)       (| i|>a+b i|>a0 i|>a1 i|><0 i|><1))
        ;; comparisons:                 <      <     <         <      <     <     ><    ><
        (test-comparison  <ident (|     i|>a+b i|>a0 i|>a1) (| i|>a+b i|>a0 i|>a1 i|><0 i|><1)))
      (testing "#{<, ><}, #{<}"
        ;; comparisons:                 <      <     ><        <      <
        (test-comparison  >ident (|     i|>a+b i|>a0 i|>a1) (| i|>a+b i|>a0))
        ;; comparisons:             ><  <     <                <     <
        (test-comparison  >ident (| i|a i|><0 i|><1)        (| i|><0 i|><1)))
      (testing "#{<, ><}, #{<, ><}"
        ;; comparisons:                 <      ><              <                  ><
        (test-comparison ><ident (|     i|>a+b i|>a0)       (| i|>a+b             i|><0))
        ;; comparisons:                 <      ><    ><        <                  ><    ><
        (test-comparison ><ident (|     i|>a+b i|>a0 i|>a1) (| i|>a+b             i|><0 i|><1))
        ;; comparisons:                 <      <     ><        <      <           ><    ><
        (test-comparison ><ident (|     i|>a+b i|>a0 i|>a1) (| i|>a+b i|>a0       i|><0 i|><1))
        ;; comparisons:             <   <      ><              <                  ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0)       (| i|a                i|><0))
        ;; comparisons:             <   ><     ><              <                  ><    ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0)       (| i|a                i|><0 i|><1))
        ;; comparisons:             ><  <                                         <     ><
        (test-comparison ><ident (| i|a i|><0)              (|                    i|><0 i|><1))
        ;; comparisons:             ><        <     ><                                  ><    <
        (test-comparison ><ident (| i|a       i|><1 i|><2)  (|                    i|><0 i|><1))
        ;; comparisons:             ><  ><    <                                         <     ><
        (test-comparison ><ident (| i|a i|><0 i|><1)        (|                          i|><1 i|><2)))
      (testing "#{<, ><}, #{><}"
        ;; comparisons:             <   ><                     ><     ><
        (test-comparison ><ident (| i|a i|><0)              (| i|>a+b i|>a0))
        ;; comparisons:             <   ><    ><               ><     ><
        (test-comparison ><ident (| i|a i|><0 i|><1)        (| i|>a+b i|>a0))
        ;; comparisons:             <   ><                     ><     ><    ><
        (test-comparison ><ident (| i|a i|><0)              (| i|>a+b i|>a0 i|>a1))
        ;; comparisons:             <   ><    ><               ><     ><    ><
        (test-comparison ><ident (| i|a i|><0 i|><1)        (| i|>a+b i|>a0 i|>a1)))
      (testing "#{<, <>}, #{<, <>}"
        ;; comparisons:             <  <>                      <      <>
        (test-comparison ><ident (| a  b)                   (| a      ><1))
        ;; comparisons:             <> <                       <      <>
        (test-comparison ><ident (| a  b)                   (| b      ><1)))
      (testing "#{<, <>}, #{><, <>}"
        ;; comparisons:             <, <>                      >< <>  <>
        (test-comparison ><ident (| a  b)                   (| >a ><0 ><1)))
      (testing "#{><}, #{<, ><}"
        ;; comparisons:             ><  ><     ><              <                  ><    ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0)       (| i|<a+b             i|><0 i|><1))
        ;; comparisons:             ><  ><     ><    ><        <                  ><    ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0 i|>a1) (| i|<a+b             i|><0 i|><1))
        ;; comparisons:             ><  ><     ><              <      <           ><    ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0)       (| i|<a+b i|<a0       i|><0 i|><1))
        ;; comparisons:             ><  ><     ><    ><        <      <           ><    ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0 i|>a1) (| i|<a+b i|<a0       i|><0 i|><1))
        ;; comparisons:             ><  ><     ><              <      <     <     ><    ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0)       (| i|<a+b i|<a0 i|<a1 i|><0 i|><1))
        ;; comparisons:             ><  ><     ><    ><        <      <     <     ><    ><
        (test-comparison ><ident (| i|a i|>a+b i|>a0 i|>a1) (| i|<a+b i|<a0 i|<a1 i|><0 i|><1)))
      (testing "#{><}, #{><}"
        ;; comparisons:             ><  ><                                        ><    ><
        (test-comparison ><ident (| i|a i|><2)              (|                    i|><0 i|><1))
        ;; comparisons:             ><  ><                                        ><    ><
        (test-comparison ><ident (| i|a i|><0)              (|                    i|><1 i|><2)))
      (testing "#{<>}, #{<>}"
        ;; comparisons:             <> <>                         <>  <>
        (test-comparison <>ident (| a  b)                   (|    ><0 ><1)))))
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
          (test-comparison  >ident (| a >a+b >a0 >a1) (& >a+b >a0))
          ;; comparisons: [-1, -1], [-1, -1, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& >a+b >a0 >a1))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (test-comparison  >ident (| a >a+b >a0 >a1) (& >a+b >a0 >a1)))
        (testing "+ #{∅+}"
          ;; comparisons: [3, 3, 3], [3, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& ><0 ><1)))
        (testing "+ #{<+ ∅+}"
          ;; comparisons: [-1, 3], [-1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0)    (& >a+b         ><0 ><1))
          ;; comparisons: [-1, 3, 3], [-1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0 >a1) (& >a+b         ><0 ><1))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& >a+b >a0     ><0 ><1))
          ;; comparisons: [-1, -1, 3], [-1, -1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0 >a1) (& >a+b >a0     ><0 ><1))
          ;; comparisons: [-1, -1], [-1, -1, 3, 3, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& >a+b >a0 >a1 ><0 ><1))
          ;; comparisons: [-1, -1, -], [-1, -1, -1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0 >a1) (& >a+b >a0 >a1 ><0 ><1)))
        (testing "+ #{= ∅+}"
          ;; comparisons: [3, 3], [-1, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& a ><0))
          ;; comparisons: [3, 3], [-1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& a ><0 ><1)))
        (testing "+ #{>+ ∅+}"
          ;; comparisons: [3, 3], [-1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& <a+b         ><0 ><1))
          ;; comparisons: [3, 3, 3], [-1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0 >a1) (& <a+b         ><0 ><1))
          ;; comparisons: [3, 3], [-1, -1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& <a+b <a0     ><0 ><1))
          ;; comparisons: [3, 3, 3], [-1, -1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0 >a1) (& <a+b <a0     ><0 ><1))
          ;; comparisons: [3, 3], [-1, -1, 3, 3, 3]
          (test-comparison <>ident (| a >a+b >a0)     (& <a+b <a0 <a1 ><0 ><1))
          ;; comparisons: [3, 3, 3], [-1, -1, -1, 3, 3]
          (test-comparison <>ident (| a >a+b >a0 >a1) (& <a+b <a0 <a1 ><0 ><1)))))
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType"
      (testing "#{<}"
        (test-comparison  <ident i|<a0 (| i|>a+b i|>a0 i|>a1)))
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
        (test-comparison  <ident i|a   (| i|>a+b i|>a0 i|><0 i|><1))
        (test-comparison  <ident i|>a0 (| i|>a+b i|>a0)))
      (testing "#{< >< <>}"
        (test-comparison  <ident i|a   (| i|>a+b i|>a0 i|><0 i|><1 t/string?)))
      (testing "#{< <>}"
        (test-comparison  <ident a     (| >a ><0 ><1)))
    #_(testing "#{=}")           ; Impossible for `OrType`
    #_(testing "#{= >}")         ; Impossible for `OrType`
    #_(testing "#{= > ><}")      ; Impossible for `OrType`
    #_(testing "#{= > >< <>}")   ; Impossible for `OrType`
    #_(testing "#{= > <>}")      ; Impossible for `OrType`
      (testing "#{= ><}"
        (test-comparison  <ident i|a   (| i|a i|><0 i|><1)))
      (testing "#{= >< <>}"
        (test-comparison  <ident i|a   (| i|a i|><0 i|><1 t/string?)))
      (testing "#{= <>}"
        (test-comparison  <ident a     (| a ><0 ><1)))
      (testing "#{>}"
        (test-comparison  >ident a     (| <a0 <a1))
        (test-comparison  >ident i|a   (| i|<a0 i|<a1)))
      (testing "#{> ><}"
        (test-comparison ><ident i|a   (| i|<a+b i|<a0 i|><0 i|><1)))
      (testing "#{> >< <>}"
        (test-comparison ><ident i|a   (| i|<a+b i|<a0 i|><0 i|><1 t/string?)))
      (testing "#{> <>}"
        (test-comparison ><ident a     (| <a0 ><0 ><1)))
      (testing "#{><}"
        (test-comparison ><ident i|a   (| i|><0 i|><1)))
      (testing "#{>< <>}"
        (test-comparison ><ident i|a   (| i|><0 i|><1 t/string?)))
      (testing "#{<>}"
        (test-comparison <>ident a     (| ><0 ><1)))
      (testing "Nilable"
        (testing "<  nilabled: #{< <>}"
          (test-comparison  <ident t/long?     (t/? t/object?)))
        (testing "=  nilabled: #{= <>}"
          (test-comparison  <ident t/long?     (t/? t/long?)))
        (testing ">  nilabled: #{> <>}"
          (test-comparison ><ident t/object?   (t/? t/long?)))
        (testing ">< nilabled: #{>< <>}"
          (test-comparison ><ident t/iterable? (t/? t/comparable?)))
        (testing "<> nilabled: #{<>}"
          (test-comparison <>ident t/long?     (t/? t/string?)))))
    (testing "+ ValueType"
      (testing "arg <"
        (testing "+ arg <")
        (testing "+ arg =")
        (testing "+ arg >")
        (testing "+ arg ><")
        (testing "+ arg <>"
          (test-comparison <ident (t/value "a") (| t/string? t/byte?))
          (test-comparison <ident (t/value 1)   (| (t/value 1) (t/value 2)))
          (test-comparison <ident (t/value 1)   (| (t/value 2) (t/value 1)))
          (testing "+ arg <>"
            (test-comparison <ident (t/value 1) (| (t/value 1) (t/value 2) (t/value 3)))
            (test-comparison <ident (t/value 1) (| (t/value 2) (t/value 1) (t/value 3)))
            (test-comparison <ident (t/value 1) (| (t/value 2) (t/value 3) (t/value 1))))))
      (testing "arg ="
        (testing "+ arg <>"
          (test-comparison <ident t/nil?        (| t/nil? t/string?))))
      (testing "arg <>"
        (testing "+ arg <>"
          (test-comparison <>ident (t/value "a") (| t/byte? t/long?))
          (test-comparison <>ident (t/value 3)   (| (t/value 1) (t/value 2)))))))
  (testing "AndType"
    (testing "+ AndType")
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType"
      (testing "#{<}"
        (testing "Boxed Primitive"
          (test-comparison <ident t/byte?        (& t/number?   t/comparable?)))
        (testing "Final Concrete"
          (test-comparison <ident t/string?      (& t/char-seq? t/comparable?)))
        (testing "Extensible Concrete"
          (test-comparison <ident a (& t/iterable? (t/isa? java.util.RandomAccess))))
        (testing "Abstract"
          (test-comparison <ident (t/isa? java.util.AbstractMap$SimpleEntry)
                                  (& (t/isa? java.util.Map$Entry) (t/isa? java.io.Serializable))))
        (testing "Interface"
          (test-comparison <ident i|a           (& i|>a0 i|>a1))))
      (testing "#{<}"
        (test-comparison <ident i|a           (& i|>a0 i|>a1)))
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
        (test-comparison ><ident i|a            (& i|>a+b i|>a0 i|>a1 i|><0 i|><1)))
      (testing "#{< >< <>}"
        (test-comparison ><ident t/java-set?    (& t/java-coll? t/char-seq?
                                                   (t/isa? java.nio.ByteBuffer))))
      (testing "#{< <>}"
        (test-comparison <>ident t/string?      (& t/char-seq? t/java-set?))
        (test-comparison <>ident ><0            (& (! ><1) (! ><0)))
        (test-comparison <>ident a              (& (! a)   (! b))))
    #_(testing "#{=}")           ; Impossible for `AndType`
    #_(testing "#{= >}")         ; Impossible for `AndType`
    #_(testing "#{= > ><}")      ; Impossible for `AndType`
    #_(testing "#{= > >< <>}")   ; Impossible for `AndType`
    #_(testing "#{= > <>}")      ; Impossible for `AndType`
      (testing "#{= ><}"
        (test-comparison  >ident i|a            (& i|a i|><0 i|><1))
        (test-comparison  >ident t/char-seq?    (& t/char-seq?   t/java-set?))
        (test-comparison  >ident t/char-seq?    (& t/char-seq?   t/java-set? a)))
      (testing "#{= >< <>}") ; <- TODO comparison should be >ident
      ;; TODO fix
      (testing "#{= <>}"
        (test-comparison  >ident a              (& a t/java-set?)))
      (testing "#{>}"
        (test-comparison  >ident i|a            (& i|<a+b i|<a0 i|<a1)))
      (testing "#{> ><}"
        (test-comparison ><ident i|a            (& i|<a+b i|<a0 i|><0 i|><1))
        (test-comparison ><ident a              (& (t/isa? javax.management.AttributeList) t/java-set?))
        (test-comparison ><ident t/comparable?  (& (t/isa? java.nio.ByteBuffer) t/java-set?)))
      (testing "#{> >< <>}"
        (test-comparison ><ident i|a            (& i|<a0 i|><0 a)))
      (testing "#{> <>}") ; <- TODO comparison should be 1
      (testing "#{><}"
        (test-comparison ><ident i|a            (& i|><0 i|><1))
        (test-comparison ><ident t/char-seq?    (& t/java-set? a)))
      (testing "#{>< <>}") ; <- TODO comparison should be 3
      (testing "#{<>}"
        (test-comparison <>ident t/string?      (& a t/java-set?))))
    (testing "+ ValueType"
      (testing "#{<}"
        (test-comparison <ident (t/value "a")  (& t/char-seq? t/comparable?)))
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
        (test-comparison <>ident (t/value "a") (& t/char-seq? a))
        (test-comparison <>ident (t/value "a") (& t/char-seq? t/java-set?)))
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
        (test-comparison <>ident (t/value "a") (& a t/java-set?)))))
  (testing "Expression"
    (testing "+ Expression")
    (testing "+ ProtocolType")
    (testing "+ ClassType")
    (testing "+ ValueType"))
  (testing "ProtocolType"
    (testing "+ ProtocolType"
      (test-comparison  =ident (t/isa? AProtocolAll) (t/isa? AProtocolAll))
      (test-comparison <>ident (t/isa? AProtocolAll) (t/isa? AProtocolNone)))
    (testing "+ ClassType")
    (testing "+ ValueType"
      (let [values #{t/universal-set t/empty-set nil {} 1 "" AProtocolAll
                     quantum.test.untyped.core.type.compare.AProtocolAll}]
        (doseq [v values]
          (test-comparison  <ident (t/value v) (t/isa? AProtocolAll)))
        (doseq [v [""]]
          (test-comparison  <ident (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values "")]
          (test-comparison <>ident (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values nil)]
          (test-comparison  <ident (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-comparison <>ident (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-comparison  <ident (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v (disj values nil)]
          (test-comparison <>ident (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v values]
          (test-comparison <>ident (t/value v) (t/isa? AProtocolNone))))))
  (testing "ClassType"
    (testing "+ ClassType"
      (testing "Boxed Primitive + Boxed Primitive"
        (test-comparison  =ident t/long? t/long?)
        (test-comparison <>ident t/long? t/int?))
      (testing "Boxed Primitive + Final Concrete"
        (test-comparison <>ident t/long? t/string?))
      (testing "Boxed Primitive + Extensible Concrete"
        (testing "< , >"
          (test-comparison  <ident t/long? t/object?))
        (testing "<>"
          (test-comparison <>ident t/long? (t/isa? Thread))))
      (testing "Boxed Primitive + Abstract"
        (test-comparison <>ident t/long? (t/isa? java.util.AbstractCollection)))
      (testing "Boxed Primitive + Interface"
        (test-comparison <>ident t/long? t/char-seq?))
      (testing "Final Concrete + Final Concrete"
        (test-comparison =ident t/string? t/string?))
      (testing "Final Concrete + Extensible Concrete"
        (testing "< , >"
          (test-comparison  <ident t/string? t/object?))
        (testing "<>"
          (test-comparison <>ident t/string? a)))
      (testing "Final Concrete + Abstract")
      (testing "Final Concrete + Interface"
        (testing "< , >"
          (test-comparison  <ident t/string? t/comparable?))
        (testing "<>"
          (test-comparison <>ident t/string? t/java-coll?)))
      (testing "Extensible Concrete + Extensible Concrete"
        (test-comparison =ident t/object? t/object?)
        (testing "< , >"
          (test-comparison  <ident a t/object?))
        (testing "<>"
          (test-comparison <>ident a (t/isa? Thread))))
      (testing "Extensible Concrete + Abstract"
        (testing "< , >"
          (test-comparison  <ident (t/isa? java.util.AbstractCollection) t/object?)
          (test-comparison  <ident a (t/isa? java.util.AbstractCollection)))
        (testing "<>"
          (test-comparison <>ident (t/isa? Thread) (t/isa? java.util.AbstractCollection))
          (test-comparison <>ident (t/isa? java.util.AbstractCollection) (t/isa? Thread))))
      (testing "Extensible Concrete + Interface"
        (test-comparison ><ident a t/char-seq?))
      (testing "Abstract + Abstract"
        (test-comparison  =ident (t/isa? java.util.AbstractCollection) (t/isa? java.util.AbstractCollection))
        (testing "< , >"
          (test-comparison  <ident (t/isa? java.util.AbstractList) (t/isa? java.util.AbstractCollection)))
        (testing "<>"
          (test-comparison <>ident (t/isa? java.util.AbstractList) (t/isa? java.util.AbstractQueue))))
      (testing "Abstract + Interface"
        (testing "< , >"
          (test-comparison  <ident (t/isa? java.util.AbstractCollection) t/java-coll?))
        (testing "><"
          (test-comparison ><ident (t/isa? java.util.AbstractCollection) t/comparable?)))
      (testing "Interface + Interface"
        (testing "< , >",
          (test-comparison  <ident t/java-coll? t/iterable?))
        (testing "><"
          (test-comparison ><ident t/char-seq?  t/comparable?))))
    (testing "+ ValueType"
      (testing "<"
        (testing "Class equality"
          (test-comparison  <ident (t/value "a") t/string?))
        (testing "Class inheritance"
          (test-comparison  <ident (t/value "a") t/char-seq?)
          (test-comparison  <ident (t/value "a") t/object?)))
      (testing "<>"
        (test-comparison <>ident (t/value "a") t/byte?))))
  (testing "ValueType"
    (testing "+ ValueType"
      (testing "="
        (test-comparison  =ident (t/value nil) (t/value nil))
        (test-comparison  =ident (t/value 1  ) (t/value 1  ))
        (test-comparison  =ident (t/value "a") (t/value "a")))
      (testing "=, non-strict"
        (test-comparison  =ident (t/value (vector)         ) (t/value (list)          ))
        (test-comparison  =ident (t/value (vector (vector))) (t/value (vector (list))))
        (test-comparison  =ident (t/value (hash-map)       ) (t/value (sorted-map)    )))
      (testing "<>"
        (test-comparison <>ident (t/value 1  ) (t/value 2  ))
        (test-comparison <>ident (t/value "a") (t/value "b"))
        (test-comparison <>ident (t/value 1  ) (t/value "a"))
        (test-comparison <>ident (t/value nil) (t/value "a"))))))

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
     Furthermore, is it the case that `(t/< [[] t/any?] (t/fn t/any? []))`? Intuitively it doesn't
     seem like it should be, but under the WHK model it nevertheless seems to be the case.

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
                  (t/merge (t/closed-map :requirement animal?) (t/map-of id/keyword? t/any?)))
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
                  (t/merge (t/closed-map :requirement animal?) (t/map-of id/keyword? t/any?)))
     Contract non-satisfaction ('Breakage') is `>=|input` (input covariance) and `t/<=|output`
     (output contravariance)
     - Inputs
       - I require an animal but you give me any old organism
     - Outputs
       - I guarantee an animal but I provide any old organism
       - I guarantee a sheep and some wheat but I provide only a sheep
         - (t/?? (t/map :guarantee))"
  (testing "input arities <"
    (testing "same-arity input types <"
      (testing "output <"
        (test-comparison|fn [ <ident  <ident]
          (t/fn t/any?                 [t/boolean? :> t/boolean?])
          (t/fn t/any? []              [t/any?     :> t/long?])))
      (testing "output =")
      (testing "output >"
        (test-comparison|fn [ <ident  >ident]
          (t/fn t/any?                 [t/boolean?])
          (t/fn t/any? [:> t/boolean?] [t/any? :> t/boolean?])))
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ="
      (testing "output <"
        (test-comparison|fn [ <ident  <ident]
          (t/fn t/any? [:> t/boolean?])
          (t/fn t/any? []              [t/any?])))
      (testing "output ="
        (test-comparison|fn [ <ident  =ident]
          (t/fn t/any? [])
          (t/fn t/any? []              [t/any?])))
      (testing "output >"
        (test-comparison|fn [ <ident  >ident]
          (t/fn t/any? [])
          (t/fn t/any? [:> t/boolean?] [t/any? :> t/long?])))
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types >"
      (testing "output <"
        (test-comparison|fn [><ident  <ident]
          (t/fn t/any?                 [t/any? :> t/boolean?])
          (t/fn t/any? []              [t/boolean?])))
      (testing "output ="
        (test-comparison|fn [><ident  =ident]
          (t/fn t/any?                 [t/any?])
          (t/fn t/any? []              [t/boolean?])))
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ><"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types <>"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>")))
  (testing "input arities ="
    (testing "same-arity input types <"
      (testing "output <"
        (test-comparison|fn [ <ident  <ident]
          (t/fn t/any? [t/boolean? :> t/boolean?])
          (t/fn t/any? [t/any?])))
      (testing "output ="
        (test-comparison|fn [ <ident  =ident]
          (t/fn t/any? [t/boolean?])
          (t/fn t/any? [t/any?])))
      (testing "output >"
        (test-comparison|fn [ <ident  >ident]
          (t/fn t/any? [t/boolean?])
          (t/fn t/any? [t/any?     :> t/boolean?])))
      (testing "output ><"
        (test-comparison|fn [ <ident ><ident]
          (t/fn t/any? [t/boolean? :> i|><0])
          (t/fn t/any? [t/any?     :> i|><1])))
      (testing "output <>"
        (test-comparison|fn [ <ident <>ident]
          (t/fn t/any? [t/boolean? :> ><0])
          (t/fn t/any? [t/any?     :> ><1]))))
    (testing "same-arity input types ="
      (testing "output <"
        (test-comparison|fn [ =ident  >ident]
          (t/fn t/any? [])
          (t/fn t/any? [:> t/boolean?])))
      (testing "output ="
        (test-comparison|fn [ =ident  =ident]
          (t/fn t/any? [])
          (t/fn t/any? [])))
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types >"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ><"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types <>"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>")))
  (testing "input arities >"
    (testing "same-arity input types <"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ="
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types >"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ><"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types <>"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>")))
  (testing "input arities ><"
    (testing "same-arity input types <"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ="
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types >"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ><"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types <>"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>")))
  (testing "input arities <>"
    (testing "same-arity input types <"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ="
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types >"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types ><"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))
    (testing "same-arity input types <>"
      (testing "output <")
      (testing "output =")
      (testing "output >")
      (testing "output ><")
      (testing "output <>"))))
