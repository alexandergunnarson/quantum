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
      (let [ana (self/analyze-arg-syms '{x tt/boolean?} '(t/type x))]
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
        (let [ana (self/analyze-arg-syms '{x tt/boolean?} '(t/or tt/byte? (t/type x)))]
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
                    '{x tt/boolean?}
                    '(let [x (>long-checked "123")]
                       (t/or (t/isa? Byte) (t/type x))))]
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
                '(t/type x))]
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
    (let [ana (self/analyze-arg-syms {'x 't/any?} '(t/type x))]
      (is= [[{'x tt/boolean?} tt/boolean?]
            [{'x tt/byte?}    tt/byte?]
            [{'x tt/short?}   tt/short?]
            [{'x tt/char?}    tt/char?]
            [{'x tt/int?}     tt/int?]
            [{'x tt/long?}    tt/long?]
            [{'x tt/float?}   tt/float?]
            [{'x tt/double?}  tt/double?]
            [{'x t/any?}      t/any?]]
           (transform-ana ana))))
  (testing "Input type dependent on other input type"
    (testing "Dependent type is not for first input"
      #_"1. Analyze `a` = `tt/byte?`
            -> Put `a` in env as `(t/isa? Byte)`
         2. Analyze `b` = `(t/type a)`
            -> Put `b` in env as `(t/isa? Byte)`"
      (let [ana (self/analyze-arg-syms '{a tt/byte?, b (t/type a)} 't/any?)]
        (is= [[{'a tt/byte? 'b tt/byte?} t/any?]]
             (transform-ana ana))))
    (testing "Dependent type is for first input"
      #_"1. Analyze `a` = `(t/type b)`.
         2. Analyze `b` = `tt/byte?`
            -> Put `b` in env as `(t/isa? Byte)`
         -> Put `a` in env as `(t/isa? Byte)`"
      (let [ana (self/analyze-arg-syms '{a (t/type b) b tt/byte?} 't/any?)])))
  (testing "Output type dependent on input type which is dependent on other input type"
    (testing "First input not splittable; second input not splittable"
      #_"1. Analyze `a` = `tt/byte?`
            -> Put `a` in env as `(t/isa? Byte)`
         2. Analyze `b` = `(t/type a)`
            -> Put `b` in env as `(t/isa? Byte)`
         3. Analyze out-type = `(t/type b)`
            -> `(t/isa? Byte)`"
      (let [ana (self/analyze-arg-syms '{a tt/byte? b (t/type a)} '(t/type b))]))
    (testing "First input splittable; second input not splittable"
      #_"1. Analyze `a` = `(t/or tt/boolean? tt/byte?)`. Splittable.
         2. Split:
            [[a tt/boolean?, b (t/type a) > (t/type b)]
             [a tt/byte?   , b (t/type a) > (t/type b)]]
         3. Analyze split 0.
            1. Analyze `a` = `tt/boolean?`
               -> Put `a` in env as `(t/isa? Boolean)`
            2. Analyze `b` = `(t/type a)`
               -> Put `b` in env as `(t/isa? Boolean)`
            3. Analyze out-type = `(t/type b)`
               -> `(t/isa? Boolean)`
         4. Analyze split 1 in the same way."
      (let [ana (self/analyze-arg-syms
                  '{a (t/or tt/boolean? tt/byte?) b (t/type a)} '(t/type b))]))
    (testing "Two input types directly depend on each other"
      (testing "Symbolically"
        #_"1. Analyze `a` = `(t/type b)`
              - Put `a` on queue
              1. Analyze `b` = `(t/type a)`
                 - Put `b` on queue
                 -> ERROR: `a` not in environment and `a` already on queue; circular
                           dependency detected"
        (let [ana (self/analyze-arg-syms '{a (t/type b) b (t/type a)} 't/any?)]))
      (testing "Non-symbolically"
        #_"1. Analyze `a` = `(t/type b)`
              - Put `a` on queue
              1. Analyze `b` = `(t/type [a])`
                 - Put `b` on queue
                 1. Analyze `[a]`
                    1. Analyze `a`
                       -> ERROR: `a` not in environment and `a` already on queue;
                                 circular dependency detected"
        (let [ana (self/analyze-arg-syms '{a (t/type b) b (t/type [a])} 't/any?)])))
    (testing "Two input types indirectly depend on each other"
      #_"1. Analyze `a` = `(t/type b)`
            1. Analyze `b` = `(t/type c)`
               1. Analyze `c` = `(t/type a)`
                  -> ERROR `a` not in environment and `a` already in queue; circular
                           dependency detected"
      (let [ana (self/analyze-arg-syms
                  '{a (t/type b) b (t/type c) c (t/type a)} 't/any?)]))
    (testing "Combination/integration test"
      ;; This test overview was put up in ~30 minutes on 9/30/2018 during a seemingly random walk of
      ;; thoughts without any testing or research whatsoever that happened to actually coalesce
      ;; into a working, clear, simple algorithm for handling dependent types. Not sure if
      ;; listening to Bach's Passacaglia & Fugue In C Minor for organ and then orchestra helped,
      ;; but there you go :)
      #_"1. Analyze `a` = `(t/or tt/boolean? (t/type b))`
            - Put `a` on queue
            1. Analyze `tt/boolean?`
               -> `(t/isa? Boolean)`
            2. Analyze `(t/type b)`
               1. Analyze `b` = `(t/or tt/byte? (t/type d))`
                  - Put `b` on queue
                  1. Analyze `tt/byte?`
                     -> `(t/isa? Byte)`
                  2. Analyze `(t/type d)`
                     1. Analyze `d` = `(let [b (t/- tt/char? tt/long?)]
                                         (t/or tt/char? (t/type b) (t/type c)))`
                        - Put `d` on queue
                        1. Analyze `b` = `(t/- tt/char? tt/long?)`
                           -> Put `b` in env as `t/none?`
                        2. Analyze `(t/or tt/char? (t/type b) (t/type c))`
                           1. Analyze `tt/char?`
                              -> `(t/isa? Character)`
                           2. Analyze `(t/type b)`
                              -> `t/none-type?`  <-- be careful of this
                           3. Analyze `(t/type c)`
                              1. Analyze `c` = `(t/or tt/short? tt/char?)`
                                 1. Analyze `tt/short?`
                                    -> `(t/isa? Short)`
                                 2. Analyze `tt/char?`
                                    -> `(t/isa? Character)`
                                 -> `c` candidate is:
                                    `(t/or (t/isa? Short) (t/isa? Character))`
                                    Splittable.
                                 - Split:
                                   [[a (t/or tt/boolean? (t/type b))
                                     b (t/or tt/byte? (t/type d))
                                     c (t/isa? Short)
                                     d (let [b (t/- tt/char? tt/long?)]
                                         (t/or tt/char? (t/type b) (t/type c)))
                                     > (t/or (t/type b) (t/type d))]
                                    [a (t/or tt/boolean? (t/type b))
                                     b (t/or tt/byte? (t/type d))
                                     c (t/isa? Character)
                                     d (let [b (t/- tt/char? tt/long?)]
                                         (t/or tt/char? (t/type b) (t/type c)))
                                     > (t/or (t/type b) (t/type d))]]
                                 - We continue with only Split 0 for brevity. Other
                                   splits should be handled the same.
                                 -> Put `c` in env as `(t/isa? Short)`
                              -> `(t/isa? Short)`
                           -> `(t/or (t/isa? Character)
                                     t/none-type?
                                     (t/isa? Short))`
                        - Remove `b` from env
                        - Remove `d` from queue
                        -> `d` candidate is:
                           `(t/or (t/isa? Character)
                                  t/none-type?
                                  (t/isa? Short))`.
                           Splittable.
                        - Split:
                          [[a (t/or tt/boolean? (t/type b))
                            b (t/or tt/byte? (t/type d))
                            c (t/isa? Short)
                            d (t/isa? Character)
                            > (t/or (t/type b) (t/type d))]
                           [a (t/or tt/boolean? (t/type b))
                            b (t/or tt/byte? (t/type d))
                            c (t/isa? Short)
                            d t/none-type?
                            > (t/or (t/type b) (t/type d))]
                           [a (t/or tt/boolean? (t/type b))
                            b (t/or tt/byte? (t/type d))
                            c (t/isa? Short)
                            d (t/isa? Short)
                            > (t/or (t/type b) (t/type d))]]
                        - We continue with only Split 0 for brevity. Other splits
                          should be handled the same.
                        -> Put `d` in env as `(t/isa? Character)`
                     -> `(t/isa? Character)`
                  -> `(t/isa? Character)`
               - Remove `b` from queue
               -> `b` candidate is:
                  `(t/or (t/isa? Byte) (t/isa? Character))`
                  Splittable.
               - Split:
                 [[a (t/or tt/boolean? (t/type b))
                   b (t/isa? Byte)
                   c (t/isa? Short)
                   d (t/isa? Character)
                   > (t/or (t/type b) (t/type d))]
                  [a (t/or tt/boolean? (t/type b))
                   b (t/isa? Character)
                   c (t/isa? Short)
                   d (t/isa? Character)
                   > (t/or (t/type b) (t/type d))]]
               - We continue with only Split 0 for brevity. Other splits should be
                 handled the same.
               -> Put `b` in env as `(t/isa? Byte)`
            -> `(t/isa? Byte)`
         - Remove `a` from queue
         -> `a` candidate is:
            `(t/or (t/isa? Boolean) (t/isa? Byte))`
            Splittable.
         - Split:
           [[a (t/isa? Boolean)
             b (t/isa? Byte)
             c (t/isa? Short)
             d (t/isa? Character)
             > (t/or (t/type b) (t/type d))]
            [a (t/isa? Byte)
             b (t/isa? Character)
             c (t/isa? Short)
             d (t/isa? Character)
             > (t/or (t/type b) (t/type d))]]
         - We continue with only Split 0 for brevity. Other splits should be handled
           the same.
         -> Put `a` in env as `(t/isa? Boolean)`
         2. Analyze out-type = `(t/or (t/type b) (t/type d))`
            -> (Cutting obvious corners) `(t/or (t/isa? Byte) (t/isa? Character))`
            - No splitting necessary because out-type
         - All input types are in env and output-type was analyzed. DONE"
      (let [ana (self/analyze-arg-syms
                  '{a (t/or tt/boolean? (t/type b))
                    b (t/or tt/byte? (t/type d))
                    c (t/or tt/short? tt/char?)
                    d (let [b (t/- tt/char? tt/long?)]
                        (t/or tt/char? (t/type b) (t/type c)))}
                  '(t/or (t/type b) (t/type d)))]
         (transform-ana ana)))))
