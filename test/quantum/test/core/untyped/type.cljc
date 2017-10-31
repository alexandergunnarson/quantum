(ns quantum.test.core.untyped.type
  (:require
    [clojure.core                      :as core]
    [quantum.core.error                :as err
      :refer [>err]]
    [quantum.core.fn                   :as fn
      :refer [fn-> fn1]]
    [quantum.core.test                 :as test
      :refer [deftest testing is is= throws]]
    [quantum.core.untyped.analyze.ast  :as ast]
    [quantum.core.untyped.analyze.expr :as xp
      :refer [>expr]]
    [quantum.core.untyped.numeric      :as unum]
    [quantum.core.untyped.type         :as t]))

(is= -1 (t/compare (t/value 1) t/numerically-byte?))

(is= (t/and t/long? (>expr (fn1 = 1)))
     (t/value 1))

(is= (t/and (t/value 1) (>expr unum/integer-value?))
     (t/value 1))

(t/compare (t/value 1) (>expr unum/integer-value?))

(is= 0 (t/compare (t/value 1) (>expr (fn1 =|long 1))))
(is= 0 (t/compare (t/value 1) (>expr (fn [^long x] (= x 1)))))
(is= 0 (t/compare (t/value 1) (>expr (fn [^long x] (== x 1)))))
(is= 0 (t/compare (t/value 1) (>expr (fn [x] (core/== (long x) 1)))))
(is= 0 (t/compare (t/value 1) (>expr (fn [x] (= (long x) 1)))))

;; EXAMPLE HIERARCHY ;;

(gen-interface :name t.a+b⊂)
(gen-interface :name t.a⊂0)
(gen-interface :name t.a⊂1)
(gen-interface :name t.b⊂0)
(gen-interface :name t.b⊂1)

(gen-interface :name t.a    :extends [t.a⊂0 t.a⊂1 t.a+b⊂])
(gen-interface :name t.b    :extends [t.b⊂0 t.b⊂1 t.a+b⊂])

(gen-interface :name t.a+b⊃ :extends [t.a t.b])
(gen-interface :name t.a⊃0  :extends [t.a])
(gen-interface :name t.a⊃1  :extends [t.a])
(gen-interface :name t.b⊃0  :extends [t.b])
(gen-interface :name t.b⊃1  :extends [t.b])

(gen-interface :name t.∅0)
(gen-interface :name t.∅1)
(gen-interface :name t.∅2)

(def a+b⊂  (t/isa? t.a+b⊂))
(def a⊂0   (t/isa? t.a⊂0))
(def a⊂1   (t/isa? t.a⊂1))
(def b⊂0   (t/isa? t.b⊂0))
(def b⊂1   (t/isa? t.b⊂1))
(def a     (t/isa? t.a))
(def b     (t/isa? t.b))
(def a+b⊃  (t/isa? t.a+b⊃))
(def a⊃0   (t/isa? t.a⊃0))
(def a⊃1   (t/isa? t.a⊃1))
(def b⊃0   (t/isa? t.b⊃0))
(def b⊃1   (t/isa? t.b⊃1))
; ∅ : a (possibly empty) intersect that is neither a subset nor superset
(def ∅0   (t/isa? t.∅0))
(def ∅1   (t/isa? t.∅1))
(def ∅2   (t/isa? t.∅2))

;; TESTS ;;

(deftest test|in|compare
  (testing "ValueSpec"
    (testing "+ ValueSpec"
      (testing "="
        (is=  0  (t/compare (t/value 1) (t/value 1)))
        (is=  0  (t/compare (t/value "a") (t/value "a"))))
      (testing "<"
        (is= -1  (t/compare (t/value 1) (t/value 2)))
        (is= -1  (t/compare (t/value "a") (t/value "b"))))
      (testing ">"
        (is=  1  (t/compare (t/value 2) (t/value 1)))
        (is=  1  (t/compare (t/value "b") (t/value "a"))))
      (testing "∅"
        (is= nil (t/compare (t/value 1) (t/value "a")))))
    (testing "+ ClassSpec"
      (testing "<"
        (testing "Class equality"
          (is= -1 (t/compare (t/value "a") t/string?)))
        (testing "Class inheritance"
          (is= -1 (t/compare (t/value "a") t/char-seq?))
          (is= -1 (t/compare (t/value "a") t/object?))))
      (testing "∅"
        (is= nil (t/compare (t/value "a") t/byte?))))
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
    (testing "+ OrSpec"
      (testing "<"
        ;;    #{"a"} ∅ t/byte?
        ;;    #{"a"} ⊂ t/string?
        ;; -> #{"a"} ⊂ (t/byte? ∪ t/string?)
        (is= -1  (t/compare (t/value "a") (t/or t/byte? t/string?))))
      (testing "∅"
        ;;    #{"a"} ∅ t/byte?
        ;;    #{"a"} ∅ t/long?
        ;; -> #{"a"} ∅ (t/byte? ∪ t/long?)
        (is= nil (t/compare (t/value "a") (t/or t/byte? t/long?)))))
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec"
      (testing "in>"
        (is= nil (t/compare (t/value "a") (t/and t/string? ...))))
      (testing "in: disjoint"
        ;;    #{"a"} ∅ t/byte?
        ;;    #{"a"} ⊂ t/char-seq?
        ;; -> #{"a"} ∅ (t/byte? ∩ t/char-seq?)
        (is= nil (t/compare (t/value "a") (t/and t/byte? t/char-seq?)))
        ;;    #{"a"} ∅ t/byte?
        ;;    #{"a"} ∅ t/long?
        ;; -> #{"a"} ∅ (t/byte? ∩ t/long?)
        (is= nil (t/compare (t/value "a") (t/and t/byte? t/long?)))))
    (testing "+ UnorderedAndSpec"))
  (testing "ClassSpec"
    (testing "+ ValueSpec"
      (testing ">"
        (testing "Class equality"
          (is= 1 (t/compare t/string?   (t/value "a"))))
        (testing "Class inheritance"
          (is= 1 (t/compare t/char-seq? (t/value "a")))
          (is= 1 (t/compare t/object?   (t/value "a")))))
      (testing "∅"
        (is= nil (t/compare t/byte? (t/value "a")))))
    (testing "+ ClassSpec"
      (testing "="
        (is= 0 (t/compare t/long?   t/long?))
        (is= 0 (t/compare t/object? t/object?)))
      (testing ">"
        (testing "Primitive"
          (is= 1 (t/compare t/object?   t/long?)))
        (testing "Reference"
          (is= 1 (t/compare t/object?   t/string?)))
        (testing "Interface"
          (is= 1 (t/compare t/char-seq? t/string?))))
      (testing "<"
        (testing "Primitive"
          (is= -1 (t/compare t/long?   t/object?)))
        (testing "Reference"
          (is= -1 (t/compare t/string? t/object?)))
        (testing "Interface"
          (is= -1 (t/compare t/string? t/char-seq?))))
      (testing "∅"
        (testing "Primitive + Primitive"
          (is= nil (t/compare t/long?    t/int?))
          (is= nil (t/compare t/int?     t/long?)))
        (testing "Primitive + Reference"
          (is= nil (t/compare t/long?    t/string?))
          (is= nil (t/compare t/string?  t/long?)))
        (testing "Reference + Reference"
          (is= nil (t/compare t/string? (t/isa? java.util.Collection)))
          (is= nil (t/compare (t/isa? java.util.Collection) t/string?)))
        (testing "Reference + Interface"
          (is= nil (t/compare (t/isa? java.util.ArrayList) t/char-seq?))
          (is= nil (t/compare t/char-seq? (t/isa? java.util.ArrayList))))
        (testing "Interface + Interface"
          (is= nil (t/compare t/char-seq? t/comparable?))
          (is= nil (t/compare t/comparable? t/char-seq?)))))
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
    (testing "+ OrSpec"
      ;; #{(⊂ | =) ∅} -> ⊂
      ;; #{(⊃ ?) ∅} -> ∅
      ;; Otherwise whatever it is
      (testing "#{⊂+} -> ⊂"
        (is= -1  (t/compare a (t/or a+b⊂ a⊂0 a⊂1))))
      (testing "#{∅+} -> ∅"
        (is= nil (t/compare a (t/or ∅0 ∅1))))
      (testing "#{⊂+ ∅+} -> ⊂"
        (is= -1  (t/compare a (t/or a+b⊂ a⊂0 ∅0 ∅1))))
      (testing "#{=+ ∅+} -> ⊂"
        (is= -1  (t/compare a (t/or a ∅0 ∅1))))
      (testing "#{⊃+ ∅+} -> ∅"
        (is= nil (t/compare a (t/or a+b⊃ a⊃0 ∅0 ∅1)))))
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec"
      ;; Any ∅ -> ∅
      ;; Otherwise whatever it is
      (testing "#{⊂+} -> ⊂"
        (is= -1  (t/compare a (t/and a+b⊂ a⊂0 a⊂1))))
      (testing "#{⊃+} -> ⊃"
        (is=  1  (t/compare a (t/and a+b⊃ a⊃0 a⊃1))))
      (testing "#{∅+} -> ∅"
        (is= nil (t/compare a (t/and ∅0 ∅1))))
      (testing "#{⊂+ ∅+} -> ∅"
        (is= nil (t/compare a (t/and a+b⊂ a⊂0 a⊂1 ∅0 ∅1))))
      (testing "#{=+ ∅+} -> ∅"
        (is= nil (t/compare a (t/and a ∅0 ∅1))))
      (testing "#{⊃+ ∅+} -> ∅"
        (is= nil (t/compare a (t/and a+b⊃ a⊃0 ∅0 ∅1)))))
    (testing "+ UnorderedAndSpec"))
  (testing "ProtocolSpec"
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec"))
  (testing "MaybeSpec"
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec"))
  (testing "OrSpec"
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
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
      (testing "#{= ⊂+} -> #{⊂+}"
        (testing "+ #{⊂+}"
          ;; comparisons: [-1, -1], [-1, -1]
          (is=  0  (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊂ a⊂0)))
          ;; comparisons: [-1, -1, nil], [-1, -1]
          (is=  1  (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊂ a⊂0)))
          ;; comparisons: [-1, -1], [-1, -1, nil]
          (is= -1  (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊂ a⊂0 a⊂1)))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (is=  0  (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊂ a⊂0 a⊂1))))
        (testing "+ #{∅+}"
          ;; comparisons: [nil, nil, nil], [nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/or ∅0 ∅1))))
        (testing "+ #{⊂+ ∅+}"
          ;; comparisons: [-1, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊂         ∅0 ∅1)))
          ;; comparisons: [-1, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊂         ∅0 ∅1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil]
          (is= -1  (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊂ a⊂0     ∅0 ∅1)))
          ;; comparisons: [-1, -1, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊂ a⊂0     ∅0 ∅1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil, nil]
          (is= -1  (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊂ a⊂0 a⊂1 ∅0 ∅1)))
          ;; comparisons: [-1, -1, 1], [-1, -1, -1, nil, nil]
          (is= -1  (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊂ a⊂0 a⊂1 ∅0 ∅1))))
        (testing "+ #{= ∅+}"
          ;; comparisons: [nil, nil], [-1, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/or a ∅0)))
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/or a ∅0 ∅1))))
        (testing "+ #{⊃+ ∅+}"
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊃         ∅0 ∅1)))
          ;; comparisons: [nil, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊃         ∅0 ∅1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊃ a⊃0     ∅0 ∅1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, nil nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊃ a⊃0     ∅0 ∅1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/or a+b⊃ a⊃0 a⊃1 ∅0 ∅1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/or a+b⊃ a⊃0 a⊃1 ∅0 ∅1)))))
      (testing "#{= ∅+}"
        (testing "+ #{⊂+}"
          ;; comparisons: [-1, nil], [nil, nil]
          (is= nil (t/compare (t/or a ∅0)           (t/or a+b⊂ a⊂0)))
          ;; comparisons: [-1, nil, nil], [nil, nil]
          (is= nil (t/compare (t/or a ∅0 ∅1)        (t/or a+b⊂ a⊂0)))
          ;; comparisons: [-1, nil], [nil, nil, nil]
          (is= nil (t/compare (t/or a ∅0)           (t/or a+b⊂ a⊂0 a⊂1)))
          ;; comparisons: [-1, nil, nil], [nil, nil, nil]
          (is= nil (t/compare (t/or a ∅0 ∅1)        (t/or a+b⊂ a⊂0 a⊂1))))
        (testing "+ #{∅+}"
          ;; comparisons: [nil, -1], [-1, nil]
          (is= nil (t/compare (t/or a ∅0)     (t/or ∅0 ∅1)))
          ;; comparisons: [nil, -1, -1], [-1, -1]
          (is=  1  (t/compare (t/or a ∅0 ∅1)  (t/or ∅0 ∅1)))
          ;; comparisons: [nil, nil], [nil, nil]
          (is= nil (t/compare (t/or a ∅2)     (t/or ∅0 ∅1)))
          ;; comparisons: [nil, nil, -1], [nil, -1]
          (is= nil (t/compare (t/or a ∅2 ∅1)  (t/or ∅0 ∅1)))
          ;; comparisons: [nil, nil], [nil, nil]
          (is= nil (t/compare (t/or a ∅0)     (t/or ∅1 ∅2)))
          ;; comparisons: [nil, nil, -1], [-1, nil]
          (is= nil (t/compare (t/or a ∅0 ∅1)  (t/or ∅1 ∅2))))
        (testing "+ #{⊂+ ∅+}")  ;; TODO flesh out (?)
        (testing "+ #{= ∅+}")   ;; TODO flesh out (?)
        (testing "+ #{⊃+ ∅+}")) ;; TODO flesh out (?)

      (testing "#{⊂+ ∅+} -> ⊂")
      (testing "#{= ∅+} -> ⊂")
      (testing "#{⊃+ ∅+} -> ∅"))
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec"
      ;; (if <all -1 on right-compare?> 1 nil)
      ;;
      ;; Comparison annotations achieved by first comparing each element of the first/left
      ;; to the entire second/right, then comparing each element of the second/right to the
      ;; entire first/left
      (testing "#{= ⊂+} -> #{⊂+}"
        (testing "+ #{⊂+}"
          ;; comparisons: [-1, -1], [-1, -1]
          (is=  1  (t/compare (t/or a a+b⊂ a⊂0)     (t/and a+b⊂ a⊂0)))
          ;; comparisons: [-1, -1, nil], [-1, -1]
          (is=  1  (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊂ a⊂0)))
          ;; comparisons: [-1, -1], [-1, -1, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a+b⊂ a⊂0 a⊂1)))
          ;; comparisons: [-1, -1, -1], [-1, -1, -1]
          (is=  1  (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊂ a⊂0 a⊂1))))
        (testing "+ #{∅+}"
          ;; comparisons: [nil, nil, nil], [nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and ∅0 ∅1))))
        (testing "+ #{⊂+ ∅+}"
          ;; comparisons: [-1, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)    (t/and a+b⊂         ∅0 ∅1)))
          ;; comparisons: [-1, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊂         ∅0 ∅1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a+b⊂ a⊂0     ∅0 ∅1)))
          ;; comparisons: [-1, -1, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊂ a⊂0     ∅0 ∅1)))
          ;; comparisons: [-1, -1], [-1, -1, nil, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a+b⊂ a⊂0 a⊂1 ∅0 ∅1)))
          ;; comparisons: [-1, -1, -], [-1, -1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊂ a⊂0 a⊂1 ∅0 ∅1))))
        (testing "+ #{= ∅+}"
          ;; comparisons: [nil, nil], [-1, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a ∅0)))
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a ∅0 ∅1))))
        (testing "+ #{⊃+ ∅+}"
          ;; comparisons: [nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a+b⊃         ∅0 ∅1)))
          ;; comparisons: [nil, nil, nil], [-1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊃         ∅0 ∅1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a+b⊃ a⊃0     ∅0 ∅1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, nil nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊃ a⊃0     ∅0 ∅1)))
          ;; comparisons: [nil, nil], [-1, -1, nil, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0)     (t/and a+b⊃ a⊃0 a⊃1 ∅0 ∅1)))
          ;; comparisons: [nil, nil, nil], [-1, -1, -1, nil, nil]
          (is= nil (t/compare (t/or a a+b⊂ a⊂0 a⊂1) (t/and a+b⊃ a⊃0 a⊃1 ∅0 ∅1))))))
    (testing "+ UnorderedAndSpec"))
  (testing "UnorderedOrSpec"
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec"))
  (testing "AndSpec"
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
    (testing "+ OrSpec")
    (testing "+ UnorderedOrSpec")
    (testing "+ AndSpec")
    (testing "+ UnorderedAndSpec"))
  (testing "UnorderedAndSpec"
    (testing "+ ValueSpec")
    (testing "+ ClassSpec")
    (testing "+ ProtocolSpec")
    (testing "+ MaybeSpec")
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

(deftest test|or
  (testing "simplification"
    (testing "via single-arg"
      (is= (t/or t/long?)
           t/long?))
    (testing "via identity"
      (is= (t/or t/long? t/long?)
           t/long?))
    (testing "nested"
      (is= (t/or (t/or t/long? t/long?) t/long?)
           t/long?))
    (testing "#{⊂+ =} -> #{⊂+}"
      (is= (.-args (t/or a+b⊂ a⊂0 a))
           [a+b⊂ a⊂0]))
    (testing "#{⊂+ ⊃+} -> #{⊂+}"
      (is= (.-args (t/or a+b⊂ a⊂0 a+b⊃ a⊃0))
           [a+b⊂ a⊂0]))
    (testing "#{⊃+ =} -> #{=}"
      (is= (t/or a+b⊃ a⊃0 a)
           a))
    (testing "#{⊂+ ⊃+ ∅+} -> #{⊂+ ∅+}"
      (is= (.-args (t/or a+b⊂ a⊂0 a+b⊃ a⊃0 ∅0 ∅1))
           [a+b⊂ a⊂0 ∅0 ∅1]))
    (testing "#{⊂+ =+ ⊃+ ∅+} -> #{⊂+ ∅+}"
      (is= (.-args (t/or a+b⊂ a⊂0 a a+b⊃ a⊃0 ∅0 ∅1))
           [a+b⊂ a⊂0 ∅0 ∅1])))

(deftest test|and
  ;; TODO return `(constantly false)` when impossible intersection
  (testing "simplification"
    (testing "via single-arg"
      (is= (t/and t/long?)
           t/long?))
    (testing "via identity"
      (is= (t/and t/long? t/long?)
           t/long?))
    (testing "nested"
      (is= (t/and (t/and t/long? t/long?) t/long?)
           t/long?)
      (is= (.-args (t/or (t/or t/string? t/double?)
                         (t/or t/double? t/string?)))
           [t/string? t/double?])
      (is= (.-args (t/or (t/or t/string? t/double?)
                         t/double?))
           [t/string? t/double?])
      ;; TODO this is failing with (t/or (t/or t/string? t/double?) t/char-seq?)
      (is= (.-args (t/or (t/or t/string? t/double?)
                         t/char-seq?))
           [t/char-seq? t/double?])
      (is= (.-args (t/or (t/or t/string? t/double?)
                         (t/or t/double? t/char-seq?)))
           [t/double? t/char-seq?])
      (is= (.-args (t/or (t/or t/string? t/double?)
                         (t/or t/char-seq? t/number?)))
           [t/char-seq? t/number?]))
    (testing "#{⊂+ =} -> #{=}"

      (is= (t/and a+b⊂ a⊂0 a)
           a))
    (testing "#{⊃+ =+} -> #{⊃+}"
      (is= (.-args (t/and a+b⊃ a⊃0 a))
           [a+b⊃ a⊃0]))
    (testing "#{⊂+ ⊃+} -> #{⊃+}"
      (is= (.-args (t/and a+b⊂ a⊂0 a+b⊃ a⊃0))
           [a+b⊃ a⊃0]))
    (testing "#{⊂+ ⊃+ ∅+} -> #{⊃+ ∅+}"
      (is= (.-args (t/and a+b⊂ a⊂0 a+b⊃ a⊃0 ∅0 ∅1))
           [a+b⊃ a⊃0 ∅0 ∅1]))
    (testing "#{⊂+ =+ ⊃+ ∅+} -> #{⊃+ ∅+}"
      (is= (.-args (t/and a+b⊂ a⊂0 a a+b⊃ a⊃0 ∅0 ∅1))
           [a+b⊃ a⊃0 ∅0 ∅1]))))