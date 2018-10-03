(ns quantum.test.untyped.core.analyze
  (:require
    [quantum.test.untyped.core.type   :as tt]
    [quantum.untyped.core.analyze     :as self]
    [quantum.untyped.core.analyze.ast :as uast]
    [quantum.untyped.core.test
      :refer [deftest is is= testing]]
    [quantum.untyped.core.type        :as t]))

(self/analyze-arg-syms {'x `tt/boolean?} `(t/type ~'x))
(self/analyze-arg-syms {'x `tt/boolean?} `tt/byte)
(self/analyze-arg-syms {'x `tt/boolean?} `(tt/value tt/byte))
(self/analyze-arg-syms {'x `tt/boolean?} `(t/isa? Byte))

(defn fake-typed-defn
  {:quantum.core.type/type (t/ftype nil [t/string? :> tt/long?])}
  [])

;; More dependent type tests in `quantum.test.untyped.core.type.defnt` but those are more like
;; integration tests
(deftest dependent-type-test
  (testing "Output type dependent on non-splittable input"
    (testing "Not nested within another type"
    #_"1. Analyze `x` = `tt/boolean?`
          -> Put `x` in env as `(t/isa? Boolean)`
       2. Analyze out-type = `(t/type x)`
          -> `(t/isa? Boolean)`"
      (let [ana (self/analyze-arg-syms {'x `tt/boolean?} `(t/type ~'x))]
        (is= t/boolean?
             (get-in ana [:arg-sym->arg-type 'x]))
        (is= t/boolean?
             (get-in ana [:out-type]))))
    (testing "Nested within another type"
      (testing "Without arg shadowing"
      #_"1. Analyze `x` = `tt/boolean?`
            -> Put `x` in env as `(t/isa? Boolean)`
         2. Analyze out-type = `(t/or t/byte? (t/type x))`
            1. Analyze `(t/type x)`
               -> `(t/isa? Boolean)`
            -> `(t/or (t/isa? Byte) (t/isa? Boolean))`"
        (let [ana (self/analyze-arg-syms {'x `tt/boolean?} `(t/or tt/byte? (t/type ~'x)))]
          (is= t/boolean?
               (get-in ana [:arg-sym->arg-type 'x]))
          (is= (t/or tt/byte? tt/boolean?)
               (get-in ana [:out-type]))))
      (testing "With arg shadowing"
      #_"1. Analyze `x` = `tt/boolean?`
            -> Put `x` in env as `(t/isa? Boolean)`
         2. Analyze out-type = `(let [x (>long-checked \"123\")]
                                  (t/or t/number? (t/type x)))`
            1. Analyze `(>long-checked \"123\")`
               -> Put `x` in env as `(t/isa? Long)`
            2. Analyze `(t/or t/number? (t/type x))`
               1. Analyze `(t/type x)`
                  -> `(t/isa? Long)`
               -> `(t/or (t/isa? Number) (t/isa? Long))
               -> (t/isa? Number)`"
        (let [ana (self/analyze-arg-syms
                    {'x `tt/boolean?}
                    `(let [~'x (fake-typed-defn "123")]
                       (t/or (t/isa? Number) (t/type ~'x))))]
          (is= t/boolean?
               (get-in ana [:arg-sym->arg-type 'x]))
          (is= (t/or tt/byte? tt/boolean?)
               (get-in ana [:out-type])))))))
