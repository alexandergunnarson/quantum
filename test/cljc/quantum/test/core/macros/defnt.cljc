(ns quantum.test.core.macros.defnt
  (:require
    [quantum.core.macros.defnt :as ns]
    [quantum.core.log   :as log
      :refer [prl]]
    [quantum.core.error :as err]
    [quantum.core.macros.protocol  :as proto]
    [quantum.core.macros.reify     :as reify]
    [quantum.core.macros.transform :as trans]
    [quantum.core.analyze.clojure.core :as ana]
    [quantum.core.macros.core
      :refer [case-env]]
    [quantum.core.macros
      :refer [macroexpand-all]]
    [quantum.core.test
      :refer [deftest is testing]])
  (:require-macros
    [quantum.test.core.macros.defnt
      :refer [with-merge-test]])
  #?(:clj (:import java.util.concurrent.atomic.AtomicInteger)))

; TODO look at this for type-checked Spec implementation: https://github.com/arohner/spectrum/blob/master/src/spectrum/conform.clj
; TODO look at this thread for useful discussion on potential type systems in Clojure: https://groups.google.com/forum/#!topic/clojure/Dxk-rCVL5Ss
; TODO look at HM impl : https://github.com/ericnormand/hindley-milner

(defn <typed-interface []
  `(definterface))

'(defmacro defnt+
  "`defnt` enables gradual static typing of Clojure(Script) functions.
    It may be seen as a high-performance marriage of core.typed and core.spec.

    It provides more powerful and convenient functionality than core.typed:
    - Allows for type-checking e.g. numbers which have specific values, sequence lengths, etc.
    - Performs incremental, (relatively) fast type-checking
    - Does not require all types to be annotated; any type / data shape may be checked only at runtime if desired
    - Syntax is simpler and almost entirely core.spec-like, which lowers cognitive barriers to entry

    Additionally, in leveraging core.spec, it inherits all its benefits: writing specs (type annotations)
    yields automatic documentation and generative tests, etc.

   `defnt` macro-generates three things:
   1) A multiple-dispatch call site resolved at runtime, for all type overloads present
      (Implementation: a multi-protocol. The performance advantage of this over other multiple-dispatch
       techniques was empirically determined.)
   2) A direct-dispatch call site resolved at compile time, for all non-variadic arities and overloads
      which contain any non-default (non-`Object`, in CLJ's case) types
      (Implementation:
       In CL(J|R), a reify and corresponding functional interfaces.
       In CLJS, a uniquely named, type-hinted function.)
   3) A type-hinted `defn`:
      For each arity of the `defnt`,
        If type overloads are present,
          Invokes the multiple-dispatch call site (1) with the provided arguments
        Else
          If the arity contains non-default types,
            Invokes the direct-dispatch call site (2) with the provided arguments
          Else
            Executes the body right within that arity of the `defn`

   If the generated `defn` (3) is called in a typed context (within `defnt`, `fnt`, or a `typed` scope),
     If sufficient type information is deduced at compile time,
       If the arity contains non-default types,
         It picks the best method and calls the `reify` (2).
       Else
         The `defn` (3) will be called
     Else
       If the arity is known at compile time (a normal call, or `apply` with determinate argument length),
         A) :
         The `apply` call will be macro-removed
         If the arity has multiple type overloads,
           The multi-protocol (1) will be called
         Else
           If the arity contains non-default types,
             The `reify` (2) will be called (TODO what about type resolution/matching ambiguity?)
           Else
             The `defn` (3) will be called
         End
       Else (`apply` with indeterminate argument length)
         The `defn` is called, at which point it will dispatch accordingly
       End
     End
   Else (No type information is gathered: the `defn` (3) is not called in a typed context)
     The `defn` is called, at which point it will dispatch accordingly
   End

   Implementation notes:
   - Each `defnt` definition caches type information in the macro language/sphere (e.g.
     in CLJ, when compiling CLJS). Type hints are also applied wherever possible in order to aid interop
     performance if direct access to the multi-protocol or `reify` is desired.
   - Let's say you write a `defnt` with a signature of [^indexed? ^indexed? ^indexed?]. This would
     generate thousands of possibilities if eagerly loaded, but instead it is lazily loaded.
   - Interfaces are never re-defined.
   - Not only type, but value contracts are held up even when `eval` is used: outside of a `typed`
     context, runtime spec checking is enforced.

   Options for each overload and argument:

   Possible options:
   - `:compile-time-ex?`   : When truthy, emits compile-time exception when specs are incompatible.
   - `:runtime-ex?`        : When truthy, emits runtime exception when specs are incompatible.
   - `:compile-time-warn?` : When truthy, emits compile-time warning (not exception) when specs are incompatible.
   - `:runtime-warn?`      : When truthy, emits runtime warning (not exception) when specs are incompatible.
   - `:no-check?`          : Doesn't check the type. Type will be used for documentation purposes only.
   - `:inline?`            : When truthy, inlines the body of the overload in question when called in a
                             typed context and compile-time type resolution is successful.
   Default options:
   - `:compile-time-ex?`
   - `:runtime-ex?`
   "
  {:todo #{"Possibly type inference like Haskell? Not just on return values but on inputs too"}}
  [sym arities]
  )

; TODO test (macroexpand '(refs/deref (refs/atom* 1))) — should all be `reify` calls but for some reason aren't...

'(deftest defnt+:simple
  (let [simple '(defnt+ simple [a even?] (+ a 5))
        expanded (macroexpand-all simple)]
    (eval expanded)
    (testing "Full expansion"
      (testing "CLJ"
        (is (meta= (expand-all-arities-for-defnt+ simple {:lang :clj})
              '(do (in-ns 'quantum.core.type.interfaces)
                   (swap! interfaces update [Number quantum.test.core.macros.defnt.simple_COLON_a] (whenc1 nil? FnInterface0))
                   (swap! interfaces update [Number Object] (whenc1 nil? FnInterface1))
                   (definterface FnInterface0
                     (^Number invoke [^quantum.test.core.macros.defnt.simple_COLON_a a0]))
                   (definterface FnInterface1
                     (^Number invoke [^Object a0]))
                   (in-ns 'quantum.test.core.macros.defnt)
                   (reify simple:__reify
                     FnInterface0
                     (^Number invoke [this ^quantum.test.core.macros.defnt.simple_COLON_a a0]
                       (+ @a0 5))
                     FnInterface1
                     (^Number invoke [this ^Object a0]
                       (. this invoke (->quantum.test.core.macros.defnt/simple:a a0))))))))
      (testing "CLJS"
        (is (meta= (expand-all-arities-for-defnt+ simple {:lang :cljs})
              '(do (def simple:__overloads (js-obj))
                   (aset simple:__overloads "fn0"
                     (fn [a] (+ (.deref a) 5)))
                   (aset simple:__overloads "fn1"
                     (fn [a] (. simple:__overloads fn0 (->quantum.test.core.macros.defnt/simple:a a))))
                   )))))
    (testing "Macro-expansion"
      (testing "Definition"
        (is (= expanded
               '(do (defn simple
                      ([a]
                        (validate a even?)
                        (+ a 5)))))))
      (testing "Valid types"
        (testing "Type check (compile-time check)"
          (is (= '(. ^Fn__number__even simple:__reify invoke 2)
                 (macroexpand-all '(typed (simple 2))))))
        (testing "No type check (runtime check)"
          (is (= '(simple 2)
                 (macroexpand-all '(simple 2))))))
      (testing "Invalid types"
        (testing "Type check (compile-time check)"
          (is (error? (macroexpand-all '(typed (simple "asd")))))
          (is (error? (macroexpand-all '(typed (simple 3))))))
        (testing "No type check (runtime check)"
          (is (= '(simple "asd")
                 (macroexpand-all '(simple "asd"))))
          (is (= '(simple 3)
                 (macroexpand-all '(simple 3)))))))
    (testing "Evaluation"
      (testing "Valid types"
        (testing "Type check (compile-time check)"
          (is (= 6 (eval '(typed (simple 2))))))
        (testing "No type check (runtime check)"
          (is (= 6 (eval '(simple 2))))))
      (testing "Invalid types"
        (testing "Type check (compile-time check)"
          (is (compile-error? (eval '(typed (simple "asd")))))
          (is (compile-error? (eval '(typed (simple 3))))))
        (testing "No type check (runtime check)"
          (is (runtime-error? (eval '(simple "asd"))))
          (is (runtime-error? (eval '(simple 3)))))))



    (un-defnt+ simple))
  (let [simple:opts '(defnt+ simple:opts
                       ([a {:spec even? :opts #{:compile-time?}}]))]))

(def defnt+:example
  '(defnt+ example
     ([a                   (s/and even? #(< 5 % 100))
       b                   t/any
       c                   ::number-between-6-and-20
       {:as d :keys [e g]} (s/keys* :req-un [[:e t/boolean? true]
                                             [:f t/number?]
                                             [:g (s/or* t/number? t/sequential?)
                                                 0]])]
      {:pre  (< a @c))
       :post (s/and (s/coll-of odd? :kind t/array?)
                    #(= (first %) c))}
      ...)
     ([a string?
       b (s/coll-of bigdec? :kind vector?)
       c t/any
       d t/any
      ...)))

(def data
  {:ns-       *ns*
   :lang      :clj
   :class-sym 'AtomicInteger
   :expanded  'java.util.concurrent.atomic.AtomicInteger})

#?(:clj
(deftest test:get-qualified-class-name ; TODO :clj only for now
  (let [{:keys [ns- lang class-sym]} data]
    (is (= (ns/get-qualified-class-name
             lang ns- class-sym)
           (:expanded data))))))

(defn test:classes-for-type-predicate
  ([pred lang])
  ([pred lang type-arglist]))

(deftest test:defnt-keyword->positional-profundal ; TODO derepeat
  (is (thrown? Throwable (ns/defnt-keyword->positional-profundal :else)))
  (is (thrown? Throwable (ns/defnt-keyword->positional-profundal :nope<>)))
  (is (=       nil       (ns/defnt-keyword->positional-profundal "<0>")))
  (testing "Positional"
    (is (=       [0 nil]   (ns/defnt-keyword->positional-profundal :<0>)))
    (is (=       [0 nil]   (ns/defnt-keyword->positional-profundal '<0>)))
    (is (thrown? Throwable (ns/defnt-keyword->positional-profundal :<01>)))
    (is (=       nil       (ns/defnt-keyword->positional-profundal '<01>))) ; Assumes it's an actual type
    (is (=       [1 nil]   (ns/defnt-keyword->positional-profundal '<1>)))
    (is (=       [89 nil]  (ns/defnt-keyword->positional-profundal '<89>))))
  (testing "Elemental"
    (is (=       [0 1]     (ns/defnt-keyword->positional-profundal :<0>:1)))
    (is (=       [0 0]     (ns/defnt-keyword->positional-profundal :<0>:0)))
    (is (=       [93 27]   (ns/defnt-keyword->positional-profundal :<93>:27)))
    (is (thrown? Throwable (ns/defnt-keyword->positional-profundal :<93>:07)))))

(deftest test:positional-profundal->hint
  (is (= 'String (ns/positional-profundal->hint
                   :clj 0 nil '[a b c d] '[String :<0> "[D" :<2>:1])))
  (is (= 'double (ns/positional-profundal->hint
                   :clj 2 1 '[a b c d] '[String :<0> "[D" :<2>:1])))
  (is (= :<2>:1  (ns/positional-profundal->hint
                   :clj 3 nil '[a b c d] '[String :<0> "[D" :<2>:1]))))

(deftest test:hints->with-replace-special-kws
  (testing "Position+depth spec"
    (is (= '[String String "[D" double]
           (ns/hints->with-replace-special-kws
             {:lang    :clj
              :arglist '[a b c d]
              :hints   '[String :<0> "[D" :<2>:1]}))))
  (testing "CLJS throws on depth spec"
    (is (thrown? Throwable
          (ns/hints->with-replace-special-kws
            {:lang    :cljs
             :arglist '[a b c d]
             :hints   '[String :<0> "[D" :<2>:1]}))))
  (testing "Refs"
    (testing "No cycles"
      (is (= '["[D" "[D" "[D" double]
             (ns/hints->with-replace-special-kws
               {:lang    :clj
                :arglist '[a b c d]
                :hints   '["[D" :<0> :<1> :<2>:1]}))))
    (testing "Self-reference"
      (is (thrown? Throwable
             (ns/hints->with-replace-special-kws
               {:lang    :clj
                :arglist '[a b c d]
                :hints   '[:<0> :<0> "[D" :<2>:1]}))))
    (testing "Currently doesn't handle forward references"
      (is (thrown? Throwable
             (ns/hints->with-replace-special-kws
               {:lang    :clj
                :arglist '[a b c d]
                :hints   '[:<1> String "[D" :<2>:1]}))))))

#?(:clj
(ns/defnt dummy-defnt
  ([^objects?                      x ^int k v] 0)
  ([^clojure.lang.PersistentVector x      k v] 1)
  ([^default                       x      k v] 2)
  ([                               x      k  ] 3)
  ([^clojure.lang.PersistentVector x      k  ] 4)
  ([                               x         ] 5)))

(defn interface-call? [code] (-> code first (= '.)))
(defn protocol-call? [code] (-> code first (= 'quantum.test.core.macros.defnt/dummy-defnt-protocol)))

#?(:clj
(deftest test:defnt:0
  (testing "Specific match -> interface"
    (let [code (macroexpand-all '(dummy-defnt (object-array 2) 4 (Object.)))]
      (is (interface-call? code))
      (is (= 0 (eval code)))))
  (testing "Matches only a default method (not in interface) -> protocol"
    (let [code (macroexpand-all '(dummy-defnt (Object.) 4 (Object.)))]
      (is (protocol-call? code))
      (is (= 2 (eval code)))))
  (testing "Non-default `Object` method -> interface"
    (let [code (macroexpand-all '(dummy-defnt (Object.)))]
      (is (= '. (first code)))
      (is (= nil #_"java.lang.Object" (-> code (nth 3) ana/type-hint)))
      (is (= 5 (eval code)))))
  (testing "Downcast on related"
    (let [code (macroexpand-all '(dummy-defnt ^clojure.lang.IPersistentVector (vector) (long 4)))]
      (is (= '. (first code)))
      (is (= "clojure.lang.PersistentVector" (-> code (nth 3) ana/type-hint)))
      (is (= 4 (eval code)))))
  (testing "No downcast on Object"
    (let [code (macroexpand-all '(dummy-defnt (vector) (long 4)))]
      (is (= '. (first code)))
      (is (= "java.lang.Object" (-> code (nth 3) ana/type-hint)))
      (is (= 3 (eval code)))))))

#?(:clj
(deftest test:expand-classes-for-type-hint ; TODO :clj only for now
  (let [{:keys [ns- lang class-sym]} data]
    (is (= (ns/expand-classes-for-type-hint
             class-sym lang ns- [class-sym])
           #{(:expanded data)})))))

(deftest
  ^{:todo ["Add failure tests"]}
  test:protocol-verify-unique-first-hint
  (is (= nil
         (ns/protocol-verify-unique-first-hint
          '([Test_COLON_defnt_COLON_nsEval
             [#{java.util.concurrent.atomic.AtomicInteger}]
             Object]
            [Test_COLON_defnt_COLON_nsEval
             [#{java.lang.Short java.lang.Integer java.math.BigInteger long short
                int java.lang.Long clojure.lang.BigInt}]
             Object])))))

; Non |deftest| tests

#_(log/enable! :macro-expand)
#_(log/enable! :macro-expand-protocol)

#?(:clj (def test-boxed-long (Long.    1)))
#?(:clj (def test-boxed-int  (Integer. 1)))
(def test-string     "abcde")

#_(ns/defnt test:defnt-def**
  ([^integer? a b] (+ a b)))

#_(ns/defnt test:defnt-def
  #?(:clj ([^AtomicInteger a] (.get a))) ; namespace-resolved class
          ([^integer?      a] (inc a))   ; predicate class
          #_([^integer? a ^integer? b] [a b])
          ([#{String StringBuilder} a #{boolean char} b] [a b])
          ([#{byte short} a #{int long} b #{float double} c] [a b c]))

#_(ns/defnt test:defnt-def-generic
  ([^string? x] (first x)) ; predicate class
  ([x] x)) ; Generic

#?(:clj
(defmacro with-merge-test [env sym expected]
  `(quantum.core.collections.base/merge-call ~env
     (fn [env#]
       (testing '~sym
         (let [ret# (~sym env#)]
           (log/ppr-hints :user ~(str "<< TESTING " (name sym) " >>") ret#)
           (is (= ret# ~expected))
           ret#))))))

#_(deftest integration:defnt
  (let [sym 'test-defnt
        env {:sym           sym ; TODO test the defnt before it gets to this point
             :strict?       false
             :relaxed?      false
             :sym-with-meta sym
             :lang          :clj
             :ns-           *ns*
             :body          '(([a] (.get a))
                              ([a] (inc a))
                              ([#{String StringBuilder} a #{boolean char} b] [a b])
                              ([#{short byte} a #{long int} b #{double float} c] [a b c]))
             :externs       (atom [])}
        reify-body (fn [env] {:reify-body (reify/gen-reify-body env)})
        reify-def  (fn [{:keys [lang] :as env}]
                     (when (= lang :clj)
                       (reify/gen-reify-def env)))
        defprotocol-from-interface
                   (fn [{:keys [strict?] :as env}]
                     (when-not strict?
                       (proto/gen-defprotocol-from-interface env)))
        reify-body-result
         '(reify user.TestDefntInterface
            (^Object TestDefnt [this ^java.lang.Object        a                     ] (inc ^java.lang.Object    a    ))
            (^Object TestDefnt [this ^java.lang.StringBuilder a ^boolean b          ] [^java.lang.StringBuilder a b  ])
            (^Object TestDefnt [this ^java.lang.StringBuilder a ^char    b          ] [^java.lang.StringBuilder a b  ])
            (^Object TestDefnt [this ^java.lang.String        a ^boolean b          ] [^java.lang.String        a b  ])
            (^Object TestDefnt [this ^java.lang.String        a ^char    b          ] [^java.lang.String        a b  ])
            (^Object TestDefnt [this ^short                   a ^long    b ^double c] [                         a b c])
            (^Object TestDefnt [this ^short                   a ^long    b ^float  c] [                         a b c])
            (^Object TestDefnt [this ^short                   a ^int     b ^double c] [                         a b c])
            (^Object TestDefnt [this ^short                   a ^int     b ^float  c] [                         a b c])
            (^Object TestDefnt [this ^byte                    a ^long    b ^double c] [                         a b c])
            (^Object TestDefnt [this ^byte                    a ^long    b ^float  c] [                         a b c])
            (^Object TestDefnt [this ^byte                    a ^int     b ^double c] [                         a b c])
            (^Object TestDefnt [this ^byte                    a ^int     b ^float  c] [                         a b c]))
        env (-> env
                (with-merge-test ns/defnt-arities
                  '{:arities
                    [[[a] (.get a)]
                     [[a] (inc a)]
                     [[a b] [a b]]
                     [[a b c] [a b c]]]})
                (with-merge-test ns/defnt-arglists
                  '{:arglists
                     [[a]
                      [a]
                      [#{StringBuilder String} a
                       #{boolean char}         b]
                      [#{short byte}   a
                       #{long int}     b
                       #{double float} c]]})
                (with-merge-test ns/defnt-gen-protocol-names
                  '{:genned-protocol-name                  TestDefntProtocol,
                    :genned-protocol-method-name           test-defnt-protocol,
                    :genned-protocol-method-name-qualified user/test-defnt-protocol})
                (with-merge-test ns/defnt-arglists-types
                  '{:arglists-types
                     ([[Object] Object]
                      [[Object] Object]
                      [[#{StringBuilder String}
                        #{boolean char}]
                       Object]
                      [[#{short byte}
                        #{long int}
                        #{double float}]
                       Object])})
                (with-merge-test ns/defnt-gen-interface-unexpanded
                  '{:genned-method-name          TestDefnt,
                    :genned-interface-name       TestDefntInterface,
                    :ns-qualified-interface-name user.TestDefntInterface,
                    :gen-interface-code-header
                      (gen-interface :name user.TestDefntInterface :methods),
                    :gen-interface-code-body-unexpanded
                      {[TestDefnt [#{java.lang.Object}] Object] [[a] (inc a)],
                       [TestDefnt
                        [#{java.lang.StringBuilder java.lang.String} #{boolean char}]
                        Object]
                       [[a b] [a b]],
                       [TestDefnt [#{short byte} #{long int} #{double float}] Object]
                      [[a b c] [a b c]]}})
                (with-merge-test ns/defnt-types-for-arg-positions
                  {:types-for-arg-positions
                    '{0 {java.lang.Object        #{1},
                         java.lang.String        #{2},
                         java.lang.StringBuilder #{2},
                         short                   #{3},
                         byte                    #{3}},
                      1 {boolean #{2}, char  #{2}, long #{3}, int #{3}},
                      2 {double  #{3}, float #{3}}},
                   :first-types
                    '{java.lang.Object        #{1},
                      java.lang.String        #{2},
                      java.lang.StringBuilder #{2},
                      short                   #{3},
                      byte                    #{3}}})
                (with-merge-test ns/defnt-available-default-types
                  '{:available-default-types
                     {0 #{Object boolean char long double int float},
                      1 #{Object double short float byte},
                      2 #{Object boolean char long short int byte}}})
                (with-merge-test ns/defnt-gen-interface-expanded
                  '{:gen-interface-code-body-expanded
                    [[[TestDefnt [java.lang.Object                   ] Object] ([^java.lang.Object        a                  ] (inc a))]
                     [[TestDefnt [java.lang.StringBuilder boolean    ] Object] ([^java.lang.StringBuilder a ^boolean b       ] [a b  ])]
                     [[TestDefnt [java.lang.StringBuilder char       ] Object] ([^java.lang.StringBuilder a ^char b          ] [a b  ])]
                     [[TestDefnt [java.lang.String        boolean    ] Object] ([^java.lang.String        a ^boolean b       ] [a b  ])]
                     [[TestDefnt [java.lang.String        char       ] Object] ([^java.lang.String        a ^char b          ] [a b  ])]
                     [[TestDefnt [short                   long double] Object] ([^short                   a ^long b ^double c] [a b c])]
                     [[TestDefnt [short                   long float ] Object] ([^short                   a ^long b ^float  c] [a b c])]
                     [[TestDefnt [short                   int  double] Object] ([^short                   a ^int  b ^double c] [a b c])]
                     [[TestDefnt [short                   int  float ] Object] ([^short                   a ^int  b ^float  c] [a b c])]
                     [[TestDefnt [byte                    long double] Object] ([^byte                    a ^long b ^double c] [a b c])]
                     [[TestDefnt [byte                    long float ] Object] ([^byte                    a ^long b ^float  c] [a b c])]
                     [[TestDefnt [byte                    int  double] Object] ([^byte                    a ^int  b ^double c] [a b c])]
                     [[TestDefnt [byte                    int  float ] Object] ([^byte                    a ^int  b ^float  c] [a b c])]]})
                (with-merge-test ns/defnt-gen-interface-def
                  '{:gen-interface-def
                     (gen-interface
                       :name user.TestDefntInterface
                       :methods
                       [[TestDefnt [java.lang.Object                      ] Object]
                        [TestDefnt [java.lang.StringBuilder boolean       ] Object]
                        [TestDefnt [java.lang.StringBuilder char          ] Object]
                        [TestDefnt [java.lang.String        boolean       ] Object]
                        [TestDefnt [java.lang.String        char          ] Object]
                        [TestDefnt [short                   long    double] Object]
                        [TestDefnt [short                   long    float ] Object]
                        [TestDefnt [short                   int     double] Object]
                        [TestDefnt [short                   int     float ] Object]
                        [TestDefnt [byte                    long    double] Object]
                        [TestDefnt [byte                    long    float ] Object]
                        [TestDefnt [byte                    int     double] Object]
                        [TestDefnt [byte                    int     float ] Object]])})
                (with-merge-test reify-body
                  {:reify-body reify-body-result})
                (with-merge-test reify-def
                   {:reified-sym
                      'test-defnt-reified,
                    :reified-sym-qualified
                      ^user.TestDefntInterface 'user/test-defnt-reified,
                    :reify-def
                      (list 'def 'test-defnt-reified reify-body-result)})
                (with-merge-test defprotocol-from-interface
                  '{:protocol-def
                     (defprotocol TestDefntProtocol
                       (test-defnt-protocol__3 [a0 a1 a2])
                       (test-defnt-protocol__2 [a0 a1] [a0 a1 a2])
                       (test-defnt-protocol [a0] [a0 a1] [a0 a1 a2])),
                     :genned-protocol-method-names
                       (test-defnt-protocol__3
                        test-defnt-protocol__2
                        test-defnt-protocol)})
                (with-merge-test ns/defnt-extend-protocol-def
                  '{:extend-protocol-def
                     (extend-protocol
                       TestDefntProtocol
                       nil
                       (test-defnt-protocol
                         ([#_(tag nil) a] (let [] (inc ^java.lang.Object a))))
                       java.lang.StringBuilder
                       (test-defnt-protocol
                         ([^java.lang.StringBuilder a b] (let [b (boolean b)] [^java.lang.StringBuilder a b]))
                         ([^java.lang.StringBuilder a b] (let [b (char    b)] [^java.lang.StringBuilder a b])))
                       java.lang.String
                       (test-defnt-protocol
                         ([^java.lang.String a b] (let [b (boolean b)] [^java.lang.String a b]))
                         ([^java.lang.String a b] (let [b (char    b)] [^java.lang.String a b])))
                       java.lang.Short
                       (test-defnt-protocol
                         ([a ^long b ^double c] (let [a (short a) b (long b) c (double c)] [a b c]))
                         ([a ^long b         c] (let [a (short a) b (long b) c (float  c)] [a b c]))
                         ([a       b ^double c] (let [a (short a) b (int  b) c (double c)] [a b c]))
                         ([a       b         c] (let [a (short a) b (int  b) c (float  c)] [a b c])))
                       java.lang.Byte
                       (test-defnt-protocol
                         ([a ^long b ^double c] (let [a (byte  a) b (long b) c (double c)] [a b c]))
                         ([a ^long b         c] (let [a (byte  a) b (long b) c (float  c)] [a b c]))
                         ([a       b ^double c] (let [a (byte  a) b (int  b) c (double c)] [a b c]))
                         ([a       b         c] (let [a (byte  a) b (int  b) c (float  c)] [a b c]))))})
                #_(with-merge-test ns/defnt-gen-helper-macro
                  '{}))]))

#_(deftest test:defnt
  #?(:clj (is (= 300 (test:defnt-def (AtomicInteger. 300))))) ; reify
          (is (= 2   (test:defnt-def test-boxed-long))) ; protocol
          (is (= 2   (test:defnt-def test-boxed-int )))
          (is (= 2   (test:defnt-def 1))) ; reify
  #?(:clj (is (instance? IllegalArgumentException
                (err/suppress (test:defnt-def 1.0))))) ; reify, 'No matching method found'
          (is (= \a (test:defnt-def-generic test-string)))
          (is (= 1  (test:defnt-def-generic 1)))
          (is (= [1 2] (test:defnt-def 1 2))) ; reify
          (is (= [1 "abcde"] (test:defnt-def test-boxed-int test-string))) ; reify
          )

(log/disable! :macro-expand)


#_(:clj
  (defnt' div*-bin-
  "Lax |/|; continues on overflow/underflow.
   TODO Doesn't preserve ratios."
  (^double ; is it actually always double?
    [#{byte char short int long float double} n
     #{byte char short int long float double} d]
    (quantum.core.Numeric/divide n d))
  (^Number [^java.math.BigInteger n ^java.math.BigInteger d]
    (when (.equals d BigInteger/ZERO)
      (throw (ArithmeticException. "Divide by zero")))
    (let [^BigInteger gcd (.gcd n d)]
      (if (.equals gcd BigInteger/ZERO)
          BigInt/ZERO
          (let [n-f (.divide n gcd)
                d-f (.divide d gcd)]
            (cond
              (.equals d BigInteger/ONE)
                (BigInt/fromBigInteger n-f)
              (.equals d (.negate BigInteger/ONE))
                (BigInt/fromBigInteger (.negate n-f))
              :else (clojure.lang.Ratio. (if (neg? d-f) (.negate n-f) n-f)
                                         (if (neg? d-f) (.negate d-f) d-f)))))))
  ([^clojure.lang.BigInt n ^clojure.lang.BigInt d]
    (div*-bin- (->big-integer n) (->big-integer d)))
  ([^java.math.BigDecimal n ^java.math.BigDecimal d]
    (if (nil? *math-context*)
        (.divide n d)
        (.divide n d *math-context*)))))

; TODO you lose the |defnt'| power with this

; (:clj (defnt div*-bin-denom-byte
; ([#{byte char short int long float double} d ^byte   n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-char
; ([#{byte char short int long float double} d ^char   n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-short
; ([#{byte char short int long float double} d ^short  n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-int
; ([#{byte char short int long float double} d ^int    n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-long
; ([#{byte char short int long float double} d ^long   n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-float
; ([#{byte char short int long float double} d ^float  n] (div*-bin- n d))))
; (:clj (defnt div*-bin-denom-double
; ([#{byte char short int long float double} d ^double n] (div*-bin- n d))))
