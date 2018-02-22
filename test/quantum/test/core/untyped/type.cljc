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

(def U (t/isa? t/universal-class))

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
      (is= 0  (t/compare t/universal-set t/universal-set)))
    (testing "+ NullSetSpec"
      (test-symmetric 1 t/universal-set t/null-set))
    (testing "+ InferSpec")
    (testing "+ ValueSpec"
      (doseq [spec [(t/value t/universal-set)
                    (t/value t/null-set)
                    (t/value 0)
                    (t/value nil)]]
        (is= 1 (t/compare t/universal-set spec))))
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec"
      (doseq [spec protocol-specs]
        (is=  1 (t/compare t/universal-set spec))
        (is= -1 (t/compare spec t/universal-set))))
    (testing "+ NotSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec")
    (testing "+ Expression"))
  (testing "NullSetSpec"
    (testing "+ NullSetSpec"
      (is=  0  (t/compare t/null-set t/null-set)))
    (testing "+ InferSpec")
    (testing "+ ValueSpec"
      (test-symmetric nil t/null-set (t/value t/null-set))
      (test-symmetric nil t/null-set (t/value 0)))
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec"
      (doseq [spec protocol-specs]
        (is= nil (t/compare t/null-set spec))
        (is= nil (t/compare spec t/null-set))))
    (testing "+ NotSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec")
    (testing "+ Expression"))
  (testing "InferSpec"
    (testing "+ InferSpec")
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ NotSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec")
    (testing "+ Expression"))
  (testing "ValueSpec"
    (testing "+ ValueSpec"
      (testing "="
        (is= 0 (t/compare (t/value nil) (t/value nil)))
        (is= 0 (t/compare (t/value 1  ) (t/value 1  )))
        (is= 0 (t/compare (t/value "a") (t/value "a"))))
      (testing "=, non-strict"
        (is= 0 (t/compare (t/value (vector)         ) (t/value (list)          )))
        (is= 0 (t/compare (t/value (vector (vector))) (t/value (vector (list)))))
        (is= 0 (t/compare (t/value (hash-map)       ) (t/value (sorted-map)    ))))
      (testing "<>"
        (is= nil (t/compare (t/value 2  ) (t/value 1  )))
        (is= nil (t/compare (t/value "b") (t/value "a")))
        (is= nil (t/compare (t/value 1  ) (t/value 2  )))
        (is= nil (t/compare (t/value "a") (t/value "b")))
        (is= nil (t/compare (t/value 1  ) (t/value "a")))
        (is= nil (t/compare (t/value nil) (t/value "a")))))
    (testing "+ ClassSpec"
      (testing "<"
        (testing "Class equality"
          (test-symmetric -1 (t/value "a") t/string?))
        (testing "Class inheritance"
          (test-symmetric -1 (t/value "a") t/char-seq?)
          (test-symmetric -1 (t/value "a") t/object?)))
      (testing "<>"
        (test-symmetric nil (t/value "a") t/byte?)))
    (testing "+ ProtocolSpec"
      (let [values #{nil {} 1 "" AProtocolAll quantum.test.core.untyped.type.AProtocolAll}]
        (doseq [v values]
          (test-symmetric -1  (t/value v) (t/isa? AProtocolAll)))
        (doseq [v [""]]
          (test-symmetric -1  (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values "")]
          (test-symmetric nil (t/value v) (t/isa? AProtocolString)))
        (doseq [v (disj values nil)]
          (test-symmetric -1  (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-symmetric nil (t/value v) (t/isa? AProtocolNonNil)))
        (doseq [v [nil]]
          (test-symmetric -1  (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v (disj values nil)]
          (test-symmetric nil (t/value v) (t/isa? AProtocolOnlyNil)))
        (doseq [v values]
          (test-symmetric nil (t/value v) (t/isa? AProtocolNone)))))
    (testing "+ OrSpec"
      (testing "<"
        ;;    #{"a"} <> t/byte?
        ;;    #{"a"} <  t/string?
        ;; -> #{"a"} <  (t/byte? ∪ t/string?)
        (is= -1  (t/compare (t/value "a") (| t/byte? t/string?))))
      (testing "<>"
        ;;    #{"a"} <> t/byte?
        ;;    #{"a"} <> t/long?
        ;; -> #{"a"} <> (t/byte? ∪ t/long?)
        (is= nil (t/compare (t/value "a") (| t/byte? t/long?)))))
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec"
      #_(testing ">" ; TODO fix test
        (is= nil (t/compare (t/value "a") (& t/string? ...))))
      (testing "<"
        ;;    #{"a"} < t/comparable?
        ;;    #{"a"} < t/char-seq?
        ;; -> #{"a"} < (t/comparable? ∩ t/char-seq?)
        (is= -1  (t/compare (t/value "a") (& t/comparable? t/char-seq?))))
      (testing "><"
        ;;    #{"a"} <> t/array-list?
        ;;    #{"a"} <  t/char-seq?
        ;; -> #{"a"} >< (t/array-list? ∩ t/char-seq?)
        (is=  2  (t/compare (t/value "a") (& t/array-list? t/char-seq?)))) ; TODO fix impl
      (testing "<>"
        ;;    #{"a"} <> t/array-list?
        ;;    #{"a"} <> t/?
        ;; -> #{"a"} <> (t/array-list? ∩ t/long?)
        (is= nil (t/compare (t/value "a") (& t/array-list? t/java-set?)))))
    (testing "+ UnorderedAndSpec"))
  (testing "ClassSpec"
    (testing "+ ClassSpec"
      (testing "Boxed Primitive + Boxed Primitive"
        (is=  0  (t/compare t/long?    t/long?))
        (is= nil (t/compare t/long?    t/int?))
        (is= nil (t/compare t/int?     t/long?)))
      (testing "Boxed Primitive + Final Concrete"
        (is= nil (t/compare t/long?    t/string?))
        (is= nil (t/compare t/string?  t/long?)))
      (testing "Boxed Primitive + Extensible Concrete"
        (testing "< , >"
          (is= -1  (t/compare t/long?    t/object?))
          (is=  1  (t/compare t/object?  t/long?)))
        (testing "<>"
          (is= nil (t/compare t/long?    t/thread?))
          (is= nil (t/compare t/thread?  t/long?))))
      (testing "Boxed Primitive + Abstract"
        (is= nil (t/compare t/long? (t/isa? java.util.AbstractCollection)))
        (is= nil (t/compare (t/isa? java.util.AbstractCollection) t/long?)))
      (testing "Boxed Primitive + Interface"
        (is= nil (t/compare t/long?      t/char-seq?))
        (is= nil (t/compare t/char-seq?  t/long?)))
      (testing "Final Concrete + Final Concrete"
        (is=  0  (t/compare t/string?    t/string?)))
      (testing "Final Concrete + Extensible Concrete"
        (testing "< , >"
          (is= -1  (t/compare t/string?     t/object?))
          (is=  1  (t/compare t/object?     t/string?)))
        (testing "<>"
          (is= nil (t/compare t/string?     t/array-list?))
          (is= nil (t/compare t/array-list? t/string?))))
      (testing "Final Concrete + Abstract")
      (testing "Final Concrete + Interface"
        (testing "< , >"
          (is= -1  (t/compare t/string?     t/comparable?))
          (is=  1  (t/compare t/comparable? t/string?)))
        (testing "<>"
          (is= nil (t/compare t/string?     t/java-coll?))
          (is= nil (t/compare t/java-coll?  t/string?))))
      (testing "Extensible Concrete + Extensible Concrete"
        (is=  0  (t/compare t/object? t/object?))
        (testing "< , >"
          (is= -1  (t/compare t/array-list? t/object?))
          (is=  1  (t/compare t/object?     t/array-list?)))
        (testing "<>"
          (is= nil (t/compare t/array-list? t/thread?))
          (is= nil (t/compare t/thread? t/array-list?))))
      (testing "Extensible Concrete + Abstract"
        (testing "< , >"
          (is= -1  (t/compare (t/isa? java.util.AbstractCollection) t/object?))
          (is=  1  (t/compare t/object? (t/isa? java.util.AbstractCollection)))
          (is= -1  (t/compare t/array-list? (t/isa? java.util.AbstractCollection)))
          (is=  1  (t/compare (t/isa? java.util.AbstractCollection) t/array-list?)))
        (testing "<>"
          (is= nil (t/compare t/thread? (t/isa? java.util.AbstractCollection)))
          (is= nil (t/compare (t/isa? java.util.AbstractCollection) t/thread?))))
      (testing "Extensible Concrete + Interface"
        (is=  2  (t/compare t/array-list? t/char-seq?))
        (is=  2  (t/compare t/char-seq? t/array-list?)))
      (testing "Abstract + Abstract"
        (is=  0  (t/compare (t/isa? java.util.AbstractCollection) (t/isa? java.util.AbstractCollection)))
        (testing "< , >"
          (is= -1  (t/compare (t/isa? java.util.AbstractList) (t/isa? java.util.AbstractCollection)))
          (is=  1  (t/compare (t/isa? java.util.AbstractCollection) (t/isa? java.util.AbstractList))))
        (testing "<>"
          (is= nil (t/compare (t/isa? java.util.AbstractList)  (t/isa? java.util.AbstractQueue)))
          (is= nil (t/compare (t/isa? java.util.AbstractQueue) (t/isa? java.util.AbstractList)))))
      (testing "Abstract + Interface"
        (testing "< , >"
          (is= -1  (t/compare (t/isa? java.util.AbstractCollection) t/java-coll?))
          (is=  1  (t/compare t/java-coll? (t/isa? java.util.AbstractCollection))))
        (testing "><"
          (is=  2  (t/compare (t/isa? java.util.AbstractCollection) t/comparable?))
          (is=  2  (t/compare t/comparable? (t/isa? java.util.AbstractCollection)))))
      (testing "Interface + Interface"
        (testing "< , >"
          (is= -1  (t/compare t/java-coll?  t/iterable?))
          (is=  1  (t/compare t/iterable?   t/java-coll?)))
        (testing "><"
          (is=  2  (t/compare t/char-seq?   t/comparable?))
          (is=  2  (t/compare t/comparable? t/char-seq?)))))
    (testing "+ ProtocolSpec")
    (testing "+ OrSpec"
      ;; #{(< | =) ∅} -> <
      ;; #{(> ?) ∅} -> ∅
      ;; Otherwise whatever it is
      (testing "#{<+} -> <"
        (is= -1  (t/compare a (| >a+b >a0 >a1))))
      (testing "#{∅+} -> ∅"
        (is= nil (t/compare a (| ><0 ><1))))
      (testing "#{<+ ∅+} -> <"
        (is= -1  (t/compare a (| >a+b >a0 ><0 ><1))))
      (testing "#{=+ ∅+} -> <"
        (is= -1  (t/compare a (| a ><0 ><1))))
      (testing "#{>+ ∅+} -> ∅"
        (is= nil (t/compare a (| <a+b <a0 ><0 ><1))))
      (testing "Nilable"
        (testing "= nilabled"
          (is= -1  (t/compare t/long?     (t/? t/long?))))
        (testing "< nilabled"
          (is= -1  (t/compare t/long?     (t/? t/object?))))
        (testing "> nilabled"
          (is=  2  (t/compare t/object?   (t/? t/long?))))
        (testing ">< nilabled"
          (is=  2  (t/compare t/iterable? (t/? t/comparable?))))
        (testing "<> nilabled"
          (is= nil (t/compare t/long?     (t/? t/string?))))))
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec"
      ;; Any ∅ -> ∅
      ;; Otherwise whatever it is
      (testing "#{<+} -> <"
        (is= -1  (t/compare a (& >a+b >a0 >a1))))
      (testing "#{>+} -> >"
        (is=  1  (t/compare a (& <a+b <a0 <a1))))
      (testing "#{∅+} -> ∅"
        (is= nil (t/compare a (& ><0 ><1))))
      (testing "#{<+ ∅+} -> ∅"
        (is= nil (t/compare a (& >a+b >a0 >a1 ><0 ><1)))) ; TODO fix impl
      (testing "#{=+ ∅+} -> ∅"
        (is= nil (t/compare a (& a ><0 ><1)))) ; TODO fix impl
      (testing "#{>+ ∅+} -> ∅"
        (is= nil (t/compare a (& <a+b <a0 ><0 ><1))))) ; TODO fix impl
    (testing "+ UnorderedAndSpec"))
  (testing "ProtocolSpec"
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec"))
  (testing "NotSpec"
    (testing "+ UniversalSetSpec"
      (is= -1  (t/compare (! t/universal-set) t/universal-set))  ; inner =
      (is=  0  (t/compare (! t/null-set)      t/universal-set))) ; inner <
    (testing "+ NullSetSpec"
      (is=  0  (t/compare (! t/universal-set) t/null-set))       ; inner >
      (is=  1  (t/compare (! t/null-set)      t/null-set)))      ; inner =
    (testing "+ ValueSpec"
      (is= nil (t/compare (! t/universal-set) (t/value 1)))      ; inner >
      (is=  1  (t/compare (! t/null-set)      (t/value 1))))     ; inner ∅
    (testing "+ ClassSpec"
      (is= nil (t/compare (! a  ) a  ))  ; inner =
      (is= nil (t/compare (! a  ) <a0))  ; inner >
      (is=  2  (t/compare (! a  ) >a0))  ; inner < ; intersect
      (is= nil (t/compare (! a  ) ><0 ))  ; inner ∅
      (is= -1  (t/compare (! a  ) U  ))  ; inner <
      (is=  2  (t/compare (! <a0) a  ))  ; inner < ; intersect
      (is=  2  (t/compare (! <a0) >a0))  ; inner < ; intersect
      (is= nil (t/compare (! <a0) ><0 ))  ; inner ∅
      (is= -1  (t/compare (! <a0) U  ))  ; inner <
      (is= nil (t/compare (! >a0) a  ))  ; inner >
      (is= nil (t/compare (! >a0) <a0))  ; inner >
      (is= nil (t/compare (! >a0) ><0 ))  ; inner ∅
      (is= -1  (t/compare (! >a0) U  ))  ; inner <
      (is= nil (t/compare (! ><0 ) a  ))  ; inner ∅
      (is= nil (t/compare (! ><0 ) <a0))  ; inner ∅
      (is= nil (t/compare (! ><0 ) >a0))  ; inner ∅
      (is= -1  (t/compare (! ><0 ) U  ))) ; inner <
    (testing "+ ProtocolSpec")
    (testing "+ NotSpec"
      (is=  0  (t/compare (! t/universal-set) (! t/universal-set)))
      (is=  0  (t/compare (! t/null-set)      (! t/null-set)))
      (is=  0  (t/compare (! a)               (! a)))
      (is=  2  (t/compare (! a)               (! b)))
      (is=  2  (t/compare (! b)               (! a)))
      (is= nil (t/compare (! t/string?)       (! t/byte?)))
      (is= nil (t/compare (! t/byte?)         (! t/string?)))
      (is= -1  (t/compare (! a)               (! >a0)))
      (is=  1  (t/compare (! a)               (! <a0))))
    (testing "+ OrSpec"
      (is= nil (t/compare (! t/universal-set) (| ><0 ><1)))
      (is=  1  (t/compare (! t/null-set)      (| ><0 ><1)))
      (is= nil (t/compare (! ><0)              (| ><0 ><1)))
      (is= nil (t/compare (! ><1)              (| ><0 ><1))))
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec")
    (testing "+ Expression"))
  (testing "OrSpec"
    (testing "+ UniversalSetSpec")
    (testing "+ NullSetSpec")
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ OrSpec"
      ;; (let [l <all -1 on left-compare?>
      ;;       r <all -1 on right-compare?>]
      ;;   (if l
      ;;       (if r 0 -1)
      ;;       (if r 1 nil)))
      ;;
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      (testing "#{= <+} -> #{<+}"
        (testing "+ #{<+}"
          ;; comparisons: [-1, -1], [-1, -1]
          (is=  0  (t/compare (| a >a+b >a0)     (| >a+b >a0)))
          ;; comparisons: [-1, -1, nil], [-1, -1]
          (is=  1  (t/compare (| a >a+b >a0 >a1) (| >a+b >a0)))
          ;; comparisons: [-1, -1], [-1, -1, nil]
          (is= -1  (t/compare (| a >a+b >a0)     (| >a+b >a0 >a1)))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (is=  0  (t/compare (| a >a+b >a0 >a1) (| >a+b >a0 >a1))))
        (testing "+ #{∅+}"
          ;; comparisons: [nil, nil, nil], [nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (| ><0 ><1))))
        (testing "+ #{<+ ∅+}"
          ;; comparisons: [-1, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (| >a+b         ><0 ><1)))
          ;; comparisons: [-1, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (| >a+b         ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil]
          (is= -1  (t/compare (| a >a+b >a0)     (| >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (| >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil, nil]
          (is= -1  (t/compare (| a >a+b >a0)     (| >a+b >a0 >a1 ><0 ><1)))
          ;; comparisons: [-1, -1, 1], [-1, -1, -1, nil, nil]
          (is= -1  (t/compare (| a >a+b >a0 >a1) (| >a+b >a0 >a1 ><0 ><1))))
        (testing "+ #{= ∅+}"
          ;; comparisons: [nil, nil], [-1, nil]
          (is= nil (t/compare (| a >a+b >a0)     (| a ><0)))
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (| a ><0 ><1))))
        (testing "+ #{>+ ∅+}"
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (| <a+b         ><0 ><1)))
          ;; comparisons: [nil, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (| <a+b         ><0 ><1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (| <a+b <a0     ><0 ><1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, nil nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (| <a+b <a0     ><0 ><1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (| <a+b <a0 <a1 ><0 ><1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (| <a+b <a0 <a1 ><0 ><1)))))
      (testing "#{= ∅+}"
        (testing "+ #{<+}"
          ;; comparisons: [-1, nil], [nil, nil]
          (is= nil (t/compare (| a ><0)           (| >a+b >a0)))
          ;; comparisons: [-1, nil, nil], [nil, nil]
          (is= nil (t/compare (| a ><0 ><1)        (| >a+b >a0)))
          ;; comparisons: [-1, nil], [nil, nil, nil]
          (is= nil (t/compare (| a ><0)           (| >a+b >a0 >a1)))
          ;; comparisons: [-1, nil, nil], [nil, nil, nil]
          (is= nil (t/compare (| a ><0 ><1)        (| >a+b >a0 >a1))))
        (testing "+ #{∅+}"
          ;; comparisons: [nil, -1], [-1, nil]
          (is= nil (t/compare (| a ><0)     (| ><0 ><1)))
          ;; comparisons: [nil, -1, -1], [-1, -1]
          (is=  1  (t/compare (| a ><0 ><1)  (| ><0 ><1)))
          ;; comparisons: [nil, nil], [nil, nil]
          (is= nil (t/compare (| a ><2)     (| ><0 ><1)))
          ;; comparisons: [nil, nil, -1], [nil, -1]
          (is= nil (t/compare (| a ><2 ><1)  (| ><0 ><1)))
          ;; comparisons: [nil, nil], [nil, nil]
          (is= nil (t/compare (| a ><0)     (| ><1 ><2)))
          ;; comparisons: [nil, nil, -1], [-1, nil]
          (is= nil (t/compare (| a ><0 ><1)  (| ><1 ><2))))
        (testing "+ #{<+ ∅+}")  ;; TODO flesh out (?)
        (testing "+ #{= ∅+}")   ;; TODO flesh out (?)
        (testing "+ #{>+ ∅+}")) ;; TODO flesh out (?)

      (testing "#{<+ ∅+} -> <")
      (testing "#{= ∅+} -> <")
      (testing "#{>+ ∅+} -> ∅"))
    (testing "+ UnorderedOrSpec"
      (testing "+ UniversalSetSpec")
      (testing "+ NullSetSpec"))
    ;; TODO fix impls
    (testing "+ AndSpec"
      (testing "+ UniversalSetSpec")
      (testing "+ NullSetSpec")
      ;; (if <all -1 on right-compare?> 1 nil)
      ;;
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      (testing "#{= <+} -> #{<+}"
        (testing "+ #{<+}"
          ;; comparisons: [-1, -1], [-1, -1]
          (is=  1  (t/compare (| a >a+b >a0)     (& >a+b >a0)))
          ;; comparisons: [-1, -1, nil], [-1, -1]
          (is=  1  (t/compare (| a >a+b >a0 >a1) (& >a+b >a0)))
          ;; comparisons: [-1, -1], [-1, -1, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& >a+b >a0 >a1)))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (is=  1  (t/compare (| a >a+b >a0 >a1) (& >a+b >a0 >a1))))
        (testing "+ #{∅+}"
          ;; comparisons: [nil, nil, nil], [nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& ><0 ><1))))
        (testing "+ #{<+ ∅+}"
          ;; comparisons: [-1, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)    (& >a+b         ><0 ><1)))
          ;; comparisons: [-1, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (& >a+b         ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (& >a+b >a0     ><0 ><1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& >a+b >a0 >a1 ><0 ><1)))
          ;; comparisons: [-1, -1, -], [-1, -1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (& >a+b >a0 >a1 ><0 ><1))))
        (testing "+ #{= ∅+}"
          ;; comparisons: [nil, nil], [-1, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& a ><0)))
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& a ><0 ><1))))
        (testing "+ #{>+ ∅+}"
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& <a+b         ><0 ><1)))
          ;; comparisons: [nil, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (& <a+b         ><0 ><1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& <a+b <a0     ><0 ><1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, nil nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (& <a+b <a0     ><0 ><1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil, nil]
          (is= nil (t/compare (| a >a+b >a0)     (& <a+b <a0 <a1 ><0 ><1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, -1, nil, nil]
          (is= nil (t/compare (| a >a+b >a0 >a1) (& <a+b <a0 <a1 ><0 ><1))))))
    (testing "+ UnorderedAndSpec"))
  (testing "UnorderedOrSpec"
    (testing "+ UniversalSetSpec")
    (testing "+ NullSetSpec")
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec"))
  (testing "AndSpec"
    (testing "+ UniversalSetSpec")
    (testing "+ NullSetSpec")
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec"))
  (testing "UnorderedAndSpec"
    (testing "+ UniversalSetSpec")
    (testing "+ NullSetSpec")
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec")))

(deftest test|intersection|spec
  (testing "equality"
    (is= (t/intersection|spec t/long? t/long?)
         t/long?))
  (testing "specificity"
    (testing "Primitive + Reference"
      (is= (t/intersection|spec t/object? t/int?)
           t/int?)
      (is= (t/intersection|spec t/int? t/object?)
           t/int?))
    (testing "Reference + Reference"
      (is= (t/intersection|spec t/object? t/string?)
           t/string?)
      (is= (t/intersection|spec t/string? t/object?)
           t/string?))
    (testing "Reference + Interface"
      (is= (t/intersection|spec t/char-seq? t/string?)
           t/string?)
      (is= (t/intersection|spec t/string? t/char-seq?)
           t/string?)
      (is= (t/intersection|spec t/char-seq? t/object?)
           t/char-seq?)
      (is= (t/intersection|spec t/object? t/char-seq?)
           t/char-seq?)))
  (testing "disjointness"
    (testing "Primitive + Primitive"
      (is= (t/intersection|spec t/long? t/int?)
           nil))))

(deftest test|union|spec
  (testing "equality"
    (is= (t/union|spec t/long? t/long?)
         t/long?))
  (testing "specificity"
    (testing "Primitive + Reference"
      (is= (t/union|spec t/object? t/int?)
           t/object?)
      (is= (t/union|spec t/int? t/object?)
           t/object?))
    (testing "Reference + Reference"
      (is= (t/union|spec t/object? t/string?)
           t/object?)
      (is= (t/union|spec t/string? t/object?)
           t/object?))
    (testing "Reference + Interface"
      (is= (t/union|spec t/char-seq? t/string?)
           t/char-seq?)
      (is= (t/union|spec t/string? t/char-seq?)
           t/char-seq?)
      (is= (t/union|spec t/char-seq? t/object?)
           t/object?)
      (is= (t/union|spec t/object? t/char-seq?)
           t/object?)))
  (testing "disjointness"
    (testing "Primitive + Primitive"
      (is= (t/union|spec t/long? t/int?)
           #{t/long? t/int?}))))

(deftest test|not
  (testing "simplification"
    (testing "universal/null set"
      (is= (! t/universal-set)
           t/null-set)
      (is= (! t/null-set)
           t/universal-set))
    (testing "DeMorgan's Law"
      (is= (! (| (t/value 1)     (t/value 2)))
           (& (! (t/value 1)) (! (t/value 2))))
      (is= (! (& (t/value 1)     (t/value 2)))
           (| (! (t/value 1)) (! (t/value 2))))
      (is= (! (| (! a) (! b)))
           (&       a     b))
      (is= (! (& (! a) (! b)))
           (|       a     b)))))

(deftest test|or
  (testing "equality"
    (is= (| a b) (| a b))
    (is= -1  (t/compare t/nil?      (| t/nil? t/string?)))
    (is= -1  (t/compare (t/value 1) (| (t/value 1) (t/value 2))))
    (is= -1  (t/compare (t/value 1) (| (t/value 2) (t/value 1))))
    (is= nil (t/compare (t/value 3) (| (t/value 1) (t/value 2))))
    (is= -1  (t/compare (t/value 1) (| (t/value 1) (t/value 2) (t/value 3))))
    (is= -1  (t/compare (t/value 1) (| (t/value 2) (t/value 1) (t/value 3))))
    (is= -1  (t/compare (t/value 1) (| (t/value 2) (t/value 3) (t/value 1)))))
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
      (is= (t/or-spec>args (| >a+b >a0 a))
           [>a+b >a0]))
    (testing "#{<+ >+} -> #{<+}"
      (is= (t/or-spec>args (| >a+b >a0 <a+b <a0))
           [>a+b >a0]))
    (testing "#{>+ =} -> #{=}"
      (is= (| <a+b <a0 a)
           a))
    (testing "#{<+ >+ ∅+} -> #{<+ ∅+}"
      (is= (t/or-spec>args (| >a+b >a0 <a+b <a0 ><0 ><1))
           [>a+b >a0 ><0 ><1]))
    (testing "#{<+ =+ >+ ∅+} -> #{<+ ∅+}"
      (is= (t/or-spec>args (| >a+b >a0 a <a+b <a0 ><0 ><1))
           [>a+b >a0 ><0 ><1]))))

(deftest test|and
  (testing "equality"
    (is= (& a b) (& a b)))
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
  ;; TODO return `t/null-set` when impossible intersection
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
      ;; TODO fix impl
      (is= (& (| a b) (| b a))
           (| a b))
      ;; TODO fix impl
      (is= (& (| a b ><0) (| a ><0 b))
           (| a b ><0)))
    (testing ""
      (is= (t/and-spec>args (& a b))
           [a b]))
    (testing "null-set"
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
      ;; TODO fix impl
      (testing "+ or"
        (is= (& (! a) a b)
           t/null-set)
        (is= (& a (! a) b)
             t/null-set)
        (is= (& a b (! a))
             t/null-set)
        (is= (& (| a b) (! a))
             b)
        (is= (& (! a) (| a b))
             b)
        (is= (& (| a b) (! b) (| b a))
             b)
        (is= (& (| a b) (! b) (| ><0 b))
             t/null-set))
      ;; TODO fix impl
      (is= (& t/primitive? (! t/boolean?))
           (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)))
    (testing "#{<+ =} -> #{=}"
      (is= (& >a+b >a0 a)
           a))
    (testing "#{>+ =+} -> #{>+}"
      (is= (t/and-spec>args (& <a+b <a0 a))
           [<a+b <a0]))
    (testing "#{<+ >+} -> #{>+}"
      (is= (t/and-spec>args (& >a+b >a0 <a+b <a0))
           [<a+b <a0]))
    (testing "#{<+ >+ ∅+} -> #{>+ ∅+}"
      (is= (t/and-spec>args (& >a+b >a0 <a+b <a0 ><0 ><1))
           [<a+b <a0 ><0 ><1]))
    (testing "#{<+ =+ >+ ∅+} -> #{>+ ∅+}"
      (is= (t/and-spec>args (& >a+b >a0 a <a+b <a0 ><0 ><1))
           [<a+b <a0 ><0 ><1]))))

(deftest test|=
  (is (t/= (| t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
           (& (| t/boolean? t/byte? t/char? t/short? t/int? t/long? t/float? t/double?)
                  (! t/boolean?))))
  (testing "universal class(-set) identity"
    (is (not= t/val? (& t/any? t/val?)))
    (is (t/= t/val? (& t/any? t/val?)))))
