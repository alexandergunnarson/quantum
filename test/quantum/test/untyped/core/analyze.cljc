(ns quantum.test.untyped.core.analyze
  (:require
    [quantum.test.untyped.core.type   :as tt]
    [quantum.untyped.core.analyze     :as self]
    [quantum.untyped.core.analyze.ast :as uast]
    [quantum.untyped.core.collections :as uc]
    [quantum.untyped.core.data.map    :as umap]
    [quantum.untyped.core.fn
      :refer [<-]]
    [quantum.untyped.core.test
      :refer [deftest is is= testing]]
    [quantum.untyped.core.type        :as t]))

(self/analyze-arg-syms {'x `tt/boolean?} `(t/type ~'x))
(self/analyze-arg-syms {'x `tt/boolean?} `tt/byte)
(self/analyze-arg-syms {'x `tt/boolean?} `(tt/value tt/byte))
(self/analyze-arg-syms {'x `tt/boolean?} `(t/isa? Byte))

;; Simulates a typed fn
(defn- >long-checked
  {:quantum.core.type/type (t/ftype nil [t/string? :> tt/long?])}
  [])

(defn- transform-ana [ana]
  (->> ana
       (mapv #(do [(->> % :env (<- (dissoc :opts)) (uc/map-vals' :type))
                   (-> % :out-type-node :type)]))))

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
        (is= [[{'x tt/boolean?} tt/boolean?]]
             (transform-ana ana))))
    (testing "Nested within another type"
      (testing "Without arg shadowing"
      #_"1. Analyze `x` = `tt/boolean?`
            -> Put `x` in env as `(t/isa? Boolean)`
         2. Analyze out-type = `(t/or t/byte? (t/type x))`
            1. Analyze `(t/type x)`
               -> `(t/isa? Boolean)`
            -> `(t/or (t/isa? Byte) (t/isa? Boolean))`"
        (let [ana (self/analyze-arg-syms {'x 'tt/boolean?} `(t/or tt/byte? (t/type ~'x)))]
          (is= [[{'x tt/boolean?} (t/or tt/byte? tt/boolean?)]]
               (transform-ana ana))))
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
               -> `(t/or (t/isa? Byte) (t/isa? Long))"
        (let [ana (self/analyze-arg-syms
                    {'x 'tt/boolean?}
                    `(let [~'x (>long-checked "123")]
                       (t/or (t/isa? Byte) (t/type ~'x))))]
          (is= [[{'x tt/boolean?} (t/or (t/isa? Byte) tt/long?)]]
               (transform-ana ana))))))
  (testing "Output type dependent on splittable but non-primitive-splittable input"
    #_"1. Analyze `x` = `(t/or tt/boolean? tt/string?)`. Splittable.
       2. Split `(t/or tt/boolean? tt/string?)`:
          [[x tt/boolean? > (t/type x)]
           [x tt/string?  > (t/type x)]]
       3. Analyze split 0.
          1. Analyze `x` = `tt/boolean?`
             -> Put `x` in env as `(t/isa? Boolean)`
          2. Analyze out-type = `(t/type x)`
             -> `(t/isa? Boolean)`
       4. Analyze split 1.
          1. Analyze `x` = `tt/string?`
             -> Put `x` in env as `(t/isa? String)`
          2. Analyze out-type = `(t/type x)`
             -> `(t/isa? String)`"
    (let [ana (self/analyze-arg-syms
                {'x '(t/or tt/boolean? tt/string?)}
                `(t/type ~'x))]
      (is= [[{'x tt/boolean?} tt/boolean?]
            [{'x tt/string?}  tt/string?]]
           (transform-ana ana))))
  (testing "Output type dependent on primitive-splittable input"
    #_"1. Analyze `x` = `t/any?`. Primitive-splittable.
       2. Split `t/any?`:
          [[x tt/boolean? > (t/type x)]
           [x ... > (t/type x)]]
       3. Analyze split 0.
          1. Analyze `x` = `tt/boolean?`
             -> Put `x` in env as `(t/isa? Boolean)`
          2. Analyze out-type = `(t/type x)`
             -> `(t/isa? Boolean)`
       4. Analyze rest of splits in the same way."
    (let [ana (self/analyze-arg-syms {'x 't/any?} `(t/type ~'x))]
      (is= [[{'x tt/boolean?} tt/boolean?]
            [{'x tt/byte?}    tt/byte?]
            [{'x tt/short?}   tt/short?]
            [{'x tt/char?}    tt/char?]
            [{'x tt/int?}     tt/int?]
            [{'x tt/long?}    tt/long?]
            [{'x tt/float?}   tt/float?]
            [{'x tt/double?}  tt/double?]
            [{'x t/any?}      t/any?]]
           (transform-ana ana)))))
