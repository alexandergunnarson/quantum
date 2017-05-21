(ns quantum.test.core.defnt
  (:require
    [quantum.core.test        :as test
      :refer [deftest testing is is= throws]]
    [quantum.core.defnt :as ns
      :refer [->type-info ->expr-info !ref ->typed]]
    [quantum.core.core
      :refer [istr]])
#?(:clj
  (:import
    [clojure.lang Keyword Symbol]
    [quantum.core Numeric])))

(->typed {'n (!ref (->type-info {:infer? true}))}
  '(Numeric/isTrue (Numeric/isZero n)))

(def ff ns/fn-type-satisfies-expr?)

(deftest test:fn-type-satisfies-expr?
  (is= (ff )))

(defn test:->typed:literal-equivalence [f formf]
  (testing "nil"
    (is= (f nil)
         (->expr-info {:env {} :form (formf nil)
                       :type-info (->type-info {:reifieds #{:nil}})})))
  (testing "numbers"
    (is= (f 1)
         (->expr-info {:env {} :form (formf 1)
                       :type-info (->type-info {:reifieds #{Long/TYPE}})}))
    (is= (f 1.0)
         (->expr-info {:env {} :form (formf 1.0)
                       :type-info (->type-info {:reifieds #{Double/TYPE}})}))
    (is= (f 1N)
         (->expr-info {:env {} :form (formf 1N)
                       :type-info (->type-info {:reifieds #{clojure.lang.BigInt}})}))
    (is= (f 1M)
         (->expr-info {:env {} :form (formf 1M)
                       :type-info (->type-info {:reifieds #{java.math.BigDecimal}})})))
  (testing "string"
    (is= (f "abc")
         (->expr-info {:env {} :form (formf "abc")
                       :type-info (->type-info {:reifieds #{String}})})))
  (testing "keyword"
    (is= (f :abc)
         (->expr-info {:env {} :form (formf :abc)
                       :type-info (->type-info {:reifieds #{Keyword}})}))))

(deftest test:->typed:literals
  (test:->typed:literal-equivalence ->typed identity))

(deftest test:->typed:do
  (testing "Base case"
    (is= (->typed '(do))
         (->expr-info {:env {} :form nil
                       :type-info (->type-info {:reifieds #{:nil}})})))
  (testing "Literals"
    (test:->typed:literal-equivalence #(->typed (list 'do %)) #(list 'do %))))

(deftest test:->typed:let
  (testing "Base case"
    (is= (->typed '(let []))
         (->expr-info {:env {} :form '(let* [] (do))})))
  (testing "Literals"
    (test:->typed:literal-equivalence
      #(->typed (list 'let* '[a nil] %))
      #(list 'let* '[a nil] (list 'do %))))
  )

(def ->typed:if:test-cases
; pruning?, true-form, false-form, branch
  {false    {2         {3          {true  (fn [pred true-form false-form]
                                            (->expr-info {:env       {}
                                                          :form      (list 'if pred true-form false-form)
                                                          :type-info (->type-info {:reifieds #{Long/TYPE}})}))
                                    false (fn [pred true-form false-form]
                                            (->expr-info {:env       {}
                                                          :form      (list 'if pred true-form false-form)
                                                          :type-info (->type-info {:reifieds #{Long/TYPE}})}))}}}
   true     {2         {3          {true  (fn [pred true-form false-form]
                                            (->expr-info {:env       {}
                                                          :form      true-form
                                                          :type-info (->type-info {:reifieds #{Long/TYPE}})}))
                                    false (fn [pred true-form false-form]
                                            (->expr-info {:env       {}
                                                          :form      false-form
                                                          :type-info (->type-info {:reifieds #{Long/TYPE}})}))}}}})

(def truthy-objects [1 1.0 1N 1M "abc" :abc])
(def falsey-objects [nil])
(def objects {true truthy-objects false falsey-objects})

(deftest test:->typed:if
  (testing "Syntax"
    (throws (->typed '(if)))
    (throws (->typed '(if 1)))
    (throws (->typed '(if 1 2))))
  (testing "Literals"
    (doseq [pruning?   [true false]
            true-form  [2]
            false-form [3]
            branch     [true false]]
      (testing (istr "conditional branch pruning = ~{pruning?}; form = ~{(list 'if true-form false-form)}; branch = ~{branch}")
        (binding [ns/*conditional-branch-pruning?* pruning?]
          (doseq [pred (get objects branch)]
            (is= (->typed (list 'if pred true-form false-form))
                 ((get-in ->typed:if:test-cases [pruning? true-form false-form branch])
                  pred true-form false-form))))))))

```
f : [#{int}]  -> #{short}
  : [#{long}] -> #{boolean String}
```

The argument `x`, described below, might be a `boolean`, `int`, or `String`:

```
x = #{boolean int String}
```

The valid argument types of `(f x)`, then, are computed below:

```
(f x) :     (argtypes f)  &    (types [x])      ?
      => ⸢ #{[#{int}]   ⸣    ⸢ #{[#{boolean}]  ⸣
             [#{long}]}   &     [#{int}]        ?
         ⸤              ⸥    ⸤   [#{String}]}  ⸥
      => #{[#{int}]}                            ✓
```

The valid return types are thus easily found via a lookup:

```
#{#{short}}
```

A map can then be generated from argument types to return types:

```
{[#{int}] : #{short}}
```

### Example 2

The below example uses the same notation, but this time uses 'free' constraints (i.e. ones not encapsulated in a type such as `PositiveLong` or `NumberLessThan12`). Employing such constraints is normally assumed to be beyond the capabilities of the sort of `∀` proof done by a type checker, and thus to fit exclusively within the scope of a merely `∃` "soft proof" performed by e.g. generative testing (`core.spec` being a prime example). The idea is to check as much as possible at compile time but leave the rest to generative tests and, as a last resort, runtime checks.

```
g : [#{long < 15}, #{int}] -> #{boolean}
  : [#{String}]            -> #{String}
y = #{int < 10, String}
z = #{short >= 5, boolean}
(g y z) :     (argtypes f)        &   (types [y z])              ?
        => ⸢ #{[long < 15, int] ⸣    ⸢ #{[int < 10, short >= 5] ⸣
               [String]}          &     [int < 10, boolean]
                                        [String  , short >= 5]   ?
           ⸤                    ⸥    ⸤   [String  , boolean]}   ⸥
        => #{[long < 15, int]}    &   #{[int < 10, short >= 5]}  ?
        => ; (long < 15) ⊇ int < 10   ✓
           ; int         ⊇ short >= 5 ✓
           #{[long < 15, int]}                                   ✓

{[#{long < 15}, #{int}] : #{boolean}}
```
