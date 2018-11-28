(ns quantum.test.untyped.core.analyze
  (:require
    [quantum.test.untyped.core.type         :as tt]
    [quantum.untyped.core.analyze           :as self]
    [quantum.untyped.core.analyze.ast       :as uast]
    [quantum.untyped.core.collections       :as uc]
    [quantum.untyped.core.collections.logic
      :refer [seq-and-2]]
    [quantum.untyped.core.data.map          :as umap]
    [quantum.untyped.core.data.reactive     :as urx]
    [quantum.untyped.core.fn
      :refer [<-]]
    [quantum.untyped.core.loops
      :refer [reduce-2]]
    [quantum.untyped.core.test
      :refer [deftest is is= testing throws]]
    [quantum.untyped.core.type              :as t]
    [quantum.untyped.core.type.reifications :as utr]))

;; Simulates a typed fn
(defn- >long-checked {:quantum.core.type/type (t/rx (t/ftype [t/string? :> tt/long?]))} [])

(defn- dummy {:quantum.core.type/type (t/rx (t/ftype [(t/or tt/short? tt/char?)]))} [])

;; For this fn, the input types combine when applying `t/or` (`(t/or t/nil? t/val?)`)
(defn- input-types-combine
  {:quantum.core.type/type
    (t/rx (t/ftype [t/nil?         tt/byte?]
                   [t/nil?         tt/char?]
                   [(t/ref t/val?) tt/byte?]
                   [(t/ref t/val?) tt/char?]))}
  [])

(defn- transform-ana [ana]
  (->> ana
       (mapv #(vector (->> % :env :opts :arg-env deref (uc/map-vals' :type))
                      (-> % :output-type-node :type)))))

;; More dependent type tests in `quantum.test.untyped.core.type.defnt` but those are more like
;; integration tests
(deftest test|dependent-type
  (testing "Output type dependent on non-splittable input"
    (testing "Not nested within another type"
    #_"1. Analyze `x` = `tt/boolean?`
          -> Put `x` in env as `(t/isa? Boolean)`
       2. Analyze out-type = `(t/type x)`
          -> `(t/isa? Boolean)`"
      (let [ana (self/analyze-arg-syms '{x tt/boolean?} '(t/type x))]
        (is= (transform-ana ana)
             [[{'x tt/boolean?} tt/boolean?]])))
    (testing "Nested within another type"
      (testing "Without arg shadowing"
      #_"1. Analyze `x` = `tt/boolean?`
            -> Put `x` in env as `(t/isa? Boolean)`
         2. Analyze out-type = `(t/or t/byte? (t/type x))`
            1. Analyze `(t/type x)`
               -> `(t/isa? Boolean)`
            -> `(t/or (t/isa? Byte) (t/isa? Boolean))`"
        (let [ana (self/analyze-arg-syms '{x tt/boolean?} '(t/or tt/byte? (t/type x)))]
          (is= (transform-ana ana)
               [[{'x tt/boolean?} (t/or tt/byte? tt/boolean?)]])))
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
          (is= (transform-ana ana)
               [[{'x tt/boolean?} (t/or (t/isa? Byte) tt/long?)]])))))
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
      (is= (transform-ana ana)
           [[{'x tt/boolean?} tt/boolean?]
            [{'x tt/string?}  tt/string?]])))
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
      (is= (transform-ana ana)
           [[{'x tt/boolean?} tt/boolean?]
            [{'x tt/byte?}    tt/byte?]
            [{'x tt/short?}   tt/short?]
            [{'x tt/char?}    tt/char?]
            [{'x tt/int?}     tt/int?]
            [{'x tt/long?}    tt/long?]
            [{'x tt/float?}   tt/float?]
            [{'x tt/double?}  tt/double?]
            [{'x t/any?}      t/any?]])))
  (testing "Input type dependent on other input type"
    (testing "Dependent type is not for first input"
      #_"1. Analyze `a` = `tt/byte?`
            -> Put `a` in env as `(t/isa? Byte)`
         2. Analyze `b` = `(t/type a)`
            -> Put `b` in env as `(t/isa? Byte)`"
      (let [ana (self/analyze-arg-syms '{a tt/byte?, b (t/type a)} 't/any?)]
        (is= (transform-ana ana)
             [[{'a tt/byte? 'b tt/byte?} t/any?]])))
    (testing "Dependent type is for first input"
      #_"1. Analyze `a` = `(t/type b)`.
         2. Analyze `b` = `tt/byte?`
            -> Put `b` in env as `(t/isa? Byte)`
         -> Put `a` in env as `(t/isa? Byte)`"
      (let [ana (self/analyze-arg-syms '{a (t/type b) b tt/byte?} 't/any?)]
        (is= (transform-ana ana)
             [[{'a (t/isa? Byte) 'b (t/isa? Byte)} t/any?]]))))
  (testing "Output type dependent on input type which is dependent on other input type"
    (testing "First input not splittable; second input not splittable"
      #_"1. Analyze `a` = `tt/byte?`
            -> Put `a` in env as `(t/isa? Byte)`
         2. Analyze `b` = `(t/type a)`
            -> Put `b` in env as `(t/isa? Byte)`
         3. Analyze out-type = `(t/type b)`
            -> `(t/isa? Byte)`"
      (is= (-> (self/analyze-arg-syms '{a tt/byte? b (t/type a)} '(t/type b))
               transform-ana)
           [[{'a (t/isa? Byte) 'b (t/isa? Byte)} (t/isa? Byte)]]))
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
      (is= (-> (self/analyze-arg-syms '{a (t/or tt/boolean? tt/byte?) b (t/type a)} '(t/type b))
               transform-ana)
           [[{'a (t/isa? Boolean) 'b (t/isa? Boolean)} (t/isa? Boolean)]
            [{'a (t/isa? Byte)    'b (t/isa? Byte)}    (t/isa? Byte)]]))
    (testing "Two input types directly depend on each other"
      (testing "Symbolically"
        #_"1. Analyze `a` = `(t/type b)`
              - Put `a` on queue
              1. Analyze `b` = `(t/type a)`
                 - Put `b` on queue
                 -> ERROR: `a` not in environment and `a` already on queue; circular
                           dependency detected"
        (throws (self/analyze-arg-syms '{a (t/type b) b (t/type a)} 't/any?)))
      (testing "Non-symbolically"
        #_"1. Analyze `a` = `(t/type b)`
              - Put `a` on queue
              1. Analyze `b` = `(t/type [a])`
                 - Put `b` on queue
                 1. Analyze `[a]`
                    1. Analyze `a`
                       -> ERROR: `a` not in environment and `a` already on queue;
                                 circular dependency detected"
        (throws (self/analyze-arg-syms '{a (t/type b) b (t/type [a])} 't/any?))))
    (testing "Two input types indirectly depend on each other"
      #_"1. Analyze `a` = `(t/type b)`
            1. Analyze `b` = `(t/type c)`
               1. Analyze `c` = `(t/type a)`
                  -> ERROR `a` not in environment and `a` already in queue; circular
                           dependency detected"
      (throws (self/analyze-arg-syms '{a (t/type b) b (t/type c) c (t/type a)} 't/any?)))
    (testing "Complex test for `t/type` and simple test for `t/input`"
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
       (let [ret [[{'a (t/isa? Boolean)
                    'b (t/isa? Byte)
                    'c (t/isa? Short)
                    'd (t/isa? Short)}
                   (t/or (t/isa? Byte) (t/isa? Short))]
                  [{'a (t/isa? Byte)
                    'b (t/isa? Byte)
                    'c (t/isa? Short)
                    'd (t/isa? Short)}
                   (t/or (t/isa? Byte) (t/isa? Short))]
                  [{'a (t/isa? Boolean)
                    'b (t/isa? Short)
                    'c (t/isa? Short)
                    'd (t/isa? Short)}
                   (t/isa? Short)]
                  [{'a (t/isa? Short)
                    'b (t/isa? Short)
                    'c (t/isa? Short)
                    'd (t/isa? Short)}
                   (t/isa? Short)]
                  [{'a (t/isa? Boolean)
                    'b (t/isa? Byte)
                    'c (t/isa? Short)
                    'd (t/isa? Character)}
                   (t/or (t/isa? Byte) (t/isa? Character))]
                  [{'a (t/isa? Byte)
                    'b (t/isa? Byte)
                    'c (t/isa? Short)
                    'd (t/isa? Character)}
                   (t/or (t/isa? Byte) (t/isa? Character))]
                  [{'a (t/isa? Boolean)
                    'b (t/isa? Character)
                    'c (t/isa? Short)
                    'd (t/isa? Character)}
                   (t/isa? Character)]
                  [{'a (t/isa? Character)
                    'b (t/isa? Character)
                    'c (t/isa? Short)
                    'd (t/isa? Character)}
                   (t/isa? Character)]
                  [{'a (t/isa? Boolean)
                    'b (t/isa? Byte)
                    'c (t/isa? Short)
                    'd (t/value (t/isa? Character))}
                   (t/or (t/isa? Byte) (t/value (t/isa? Character)))]
                  [{'a (t/isa? Byte)
                    'b (t/isa? Byte)
                    'c (t/isa? Short)
                    'd (t/value (t/isa? Character))}
                   (t/or (t/isa? Byte) (t/value (t/isa? Character)))]
                  [{'a (t/isa? Boolean)
                    'b (t/value (t/isa? Character))
                    'c (t/isa? Short)
                    'd (t/value (t/isa? Character))}
                   (t/value (t/isa? Character))]
                  [{'a (t/value (t/isa? Character))
                    'b (t/value (t/isa? Character))
                    'c (t/isa? Short)
                    'd (t/value (t/isa? Character))}
                   (t/value (t/isa? Character))]
                  [{'a (t/isa? Boolean)
                    'b (t/isa? Byte)
                    'c (t/isa? Character)
                    'd (t/isa? Character)}
                   (t/or (t/isa? Byte) (t/isa? Character))]
                  [{'a (t/isa? Byte)
                    'b (t/isa? Byte)
                    'c (t/isa? Character)
                    'd (t/isa? Character)}
                   (t/or (t/isa? Byte) (t/isa? Character))]
                  [{'a (t/isa? Boolean)
                    'b (t/isa? Character)
                    'c (t/isa? Character)
                    'd (t/isa? Character)}
                   (t/isa? Character)]
                  [{'a (t/isa? Character)
                    'b (t/isa? Character)
                    'c (t/isa? Character)
                    'd (t/isa? Character)}
                   (t/isa? Character)]
                  [{'a (t/isa? Boolean)
                    'b (t/isa? Byte)
                    'c (t/isa? Character)
                    'd (t/value (t/isa? Character))}
                   (t/or (t/isa? Byte) (t/value (t/isa? Character)))]
                  [{'a (t/isa? Byte)
                    'b (t/isa? Byte)
                    'c (t/isa? Character)
                    'd (t/value (t/isa? Character))}
                   (t/or (t/isa? Byte) (t/value (t/isa? Character)))]
                  [{'a (t/isa? Boolean)
                    'b (t/value (t/isa? Character))
                    'c (t/isa? Character)
                    'd (t/value (t/isa? Character))}
                   (t/value (t/isa? Character))]
                  [{'a (t/value (t/isa? Character))
                    'b (t/value (t/isa? Character))
                    'c (t/isa? Character)
                    'd (t/value (t/isa? Character))}
                   (t/value (t/isa? Character))]]]
         (is= (-> (self/analyze-arg-syms
                    '{a (t/or tt/boolean? (t/type b))
                      b (t/or tt/byte? (t/type d))
                      c (t/or tt/short? tt/char?)
                      d (let [b (t/- tt/char? tt/long?)]
                          (t/or tt/char? (t/type b) (t/type c)))}
                    '(t/or (t/type b) (t/type d)))
                  transform-ana)
              ret)
         (is= (-> (self/analyze-arg-syms
                    '{a (t/or tt/boolean? (t/type b))
                      b (t/or tt/byte? (t/type d))
                      c (t/input dummy :?)
                      d (let [b (t/- tt/char? tt/long?)]
                          (t/or tt/char? (t/type b) (t/type c)))}
                    '(t/or (t/type b) (t/type d)))
                  transform-ana)
              ret)))
    ;; TODO add multiple tests for this (`input-types-combine`)
    (testing "`t/input` + `t/type`"
      (is= (-> (self/analyze-arg-syms
                 '{a (t/or  (t/input input-types-combine :? (t/type c)) tt/string?)
                   b (t/and (t/input input-types-combine :? (t/type c)) tt/long?)
                   c (t/or tt/byte? tt/char?)}
                 'tt/int?)
               transform-ana)
           [[{'a (t/or (t/isa? String) (t/value nil))
              'b (t/isa? Long)
              'c (t/isa? Byte)}
             (t/isa? Integer)]
            [{'a (t/or (t/isa? String) (t/value nil))
              'b t/none?
              'c (t/isa? Byte)}
             (t/isa? Integer)]
            [{'a (t/ref (t/not (t/value nil)))
              'b (t/isa? Long)
              'c (t/isa? Byte)}
             (t/isa? Integer)]
            [{'a (t/ref (t/not (t/value nil)))
              'b t/none?
              'c (t/isa? Byte)}
             (t/isa? Integer)]
            [{'a (t/or (t/isa? String) (t/value nil))
              'b (t/isa? Long)
              'c (t/isa? Character)}
             (t/isa? Integer)]
            [{'a (t/or (t/isa? String) (t/value nil))
              'b t/none?
              'c (t/isa? Character)}
             (t/isa? Integer)]
            [{'a (t/ref (t/not (t/value nil)))
              'b (t/isa? Long)
              'c (t/isa? Character)}
             (t/isa? Integer)]
            [{'a (t/ref (t/not (t/value nil)))
              'b t/none?
              'c (t/isa? Character)}
             (t/isa? Integer)]]))
   (testing "input to `t/input` depends on another `t/input`; `t/output` depends on
             other `t/input`s"
     (is= (-> (self/analyze-arg-syms
                '{a (t/input tt/fake-compare :?         :_)
                  b (t/input tt/fake-compare (t/type a) :?)}
                '(t/output tt/fake-compare (t/type a) (t/type b)))
              transform-ana)
          [;; Directly from `[t/long? t/long?]`
           [{'a (t/isa? Long)                 'b (t/isa? Long)}                 (t/isa? Integer)]
           ;; Because arg0 is `t/long?`, is `t/<=` `(t/ref t/val?)`, and so this is from
           ;; `[(t/ref t/val?) t/nil?]`
           [{'a (t/isa? Long)                 'b (t/value nil)}                 (t/isa? Integer)]
           ;; Directly from `[t/nil? t/nil?]`
           [{'a (t/value nil)                 'b (t/value nil)}                 (t/isa? Integer)]
           ;; Directly from `[t/nil? (t/ref t/val?)]`
           [{'a (t/value nil)                 'b (t/ref (t/not (t/value nil)))} (t/isa? Integer)]
           ;; Directly from `[(t/ref t/val?) t/nil?]`
           [{'a (t/ref (t/not (t/value nil))) 'b (t/value nil)}                 (t/isa? Integer)]])
     (is= (-> (self/analyze-arg-syms
                '{a (t/input tt/fake-compare :?             :_)
                  b (t/input tt/fake-compare [= (t/type a)] :?)}
                '(t/output tt/fake-compare (t/type a) (t/type b)))
              transform-ana)
          [;; Directly from `[t/long? t/long?]`
           [{'a (t/isa? Long)                 'b (t/isa? Long)}                 (t/isa? Integer)]
           ;; Directly from `[t/nil? t/nil?]`
           [{'a (t/value nil)                 'b (t/value nil)}                 (t/isa? Integer)]
           ;; Directly from `[t/nil? (t/ref t/val?)]`
           [{'a (t/value nil)                 'b (t/ref (t/not (t/value nil)))} (t/isa? Integer)]
           ;; Directly from `[(t/ref t/val?) t/nil?]`
           [{'a (t/ref (t/not (t/value nil))) 'b (t/value nil)}                 (t/isa? Integer)]]))))

(defn- rx=* [a b]
  (if (and (utr/rx-type? a)
           (utr/rx-type? b))
      (= (urx/norx-deref a) (urx/norx-deref b))
      (= a b)))

(defn- rx= [a b]
  (seq-and-2
    (fn [[input-types-0 output-type-0] [input-types-1 output-type-1]]
      (and (rx=* output-type-1 output-type-1)
           (seq-and-2 rx=* (->> input-types-0 (sort-by key) (map val))
                           (->> input-types-1 (sort-by key) (map val)))))
    a b))

(deftest test|arglist-forms>arglist-basis
  (is= (-> (self/analyze-arg-syms
             '{a (t/or tt/boolean? (t/type b))
               b (t/or tt/byte? (t/type d))
               c (t/or tt/short? tt/char?)
               d (let [b (t/- tt/char? tt/long?)]
                   (t/or tt/char? (t/type b) (t/type c)))}
             '(t/or (t/type b) (t/type d))
             false)
           transform-ana)
       (let [c (t/or tt/short? tt/char?)
             d (t/or tt/char? (t/value (t/- tt/char? tt/long?)) c)
             b (t/or tt/byte? d)
             a (t/or tt/boolean? b)]
         [[{'a a 'b b 'c c 'd d} (t/or b d)]]))
  (is (rx= (-> (self/analyze-arg-syms
                 '{a (t/or tt/boolean? (t/type b))
                   b (t/or tt/byte? (t/type d))
                   c (t/or tt/short? tt/char?)
                   d (let [b (t/- tt/char? tt/long?)]
                       (t/or tt/char? (t/type b) (t/input >long-checked :?) (t/type c)))}
                 '(t/or (t/type b) (t/type d))
                 false)
               transform-ana)
           (let [c (t/or tt/short? tt/char?)
                 d (t/or tt/char?
                         (t/value (t/- tt/char? tt/long?))
                         (t/rx (t/input*
                                 (-> #'>long-checked meta :quantum.core.type/type deref) [:?]))
                         c)
                 b (t/or tt/byte? d)
                 a (t/or tt/boolean? b)]
             [[{'a a 'b b 'c c 'd d} (t/or b d)]]))))
