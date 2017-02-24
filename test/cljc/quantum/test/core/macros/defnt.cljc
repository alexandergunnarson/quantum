(ns quantum.test.core.macros.defnt
  (:require
    [quantum.core.macros.defnt :as ns]
    [quantum.core.log   :as log]
    [quantum.core.error :as err]
    [quantum.core.macros.protocol  :as proto]
    [quantum.core.macros.reify     :as reify]
    [quantum.core.macros.transform :as trans]
    [quantum.core.macros.core
      :refer [case-env]]
    [quantum.core.test
      :refer [deftest is testing]])
  (:require-macros
    [quantum.test.core.macros.defnt
      :refer [with-merge-test]])
  #?(:clj (:import java.util.concurrent.atomic.AtomicInteger)))

(def data
  {:ns- *ns*
   :lang :clj
   :class-sym 'AtomicInteger
   :expanded 'java.util.concurrent.atomic.AtomicInteger})

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
(deftest test:expand-classes-for-type-hint ; TODO :clj only for now
  (let [{:keys [ns- lang class-sym]} data]
    (is (= (ns/expand-classes-for-type-hint
             class-sym lang ns- [class-sym])
           #{(:expanded data)})))))

(defn test:hint-arglist-with [arglist hints])

(defn test:defnt-remove-hints [x])

(defn test:defnt-arities
  [body])

(defn test:defnt-arglists
  [body])

(defn test:defnt-gen-protocol-names [{:keys [sym strict? lang]}])

(defn test:defnt-gen-interface-unexpanded
  [{:keys [sym arities arglists-types lang]}])

(defn test:defnt-replace-kw
  [kw {:keys [type-hints type-arglist available-default-types hint inner-type-n]}])

(defn test:defnt-gen-interface-expanded
  [{:keys [lang
           gen-interface-code-body-unexpanded
           available-default-types]
    :as env}])

(defn test:defnt-gen-interface-def
  [{:keys [gen-interface-code-header gen-interface-code-body-expanded]}])

(defn test:defnt-positioned-types-for-arglist
  [arglist types])

(defn test:defnt-types-for-arg-positions
  [{:keys [lang arglists-types]}])

(defn test:protocol-verify-arglists
  [arglists lang])

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

(defn test:defnt-gen-helper-macro
  [{:keys [genned-method-name
           genned-protocol-method-name-qualified
           reified-sym-qualified
           strict?
           relaxed?
           sym-with-meta
           args-sym
           args-hinted-sym
           lang]}])

(defn test:defnt-gen-final-defnt-def
  [{:keys [lang sym strict? externs genned-protocol-method-name
           gen-interface-def helper-macro-interface-def
           reify-def reified-sym
           helper-macro-def
           protocol-def extend-protocol-def]}])

(defn test:defnt*-helper
  ([opts lang ns- sym doc- meta- body [unk & rest-unk]])
  ([opts lang ns- sym doc- meta- body]))

(defn test:defnt
  [sym & body])

(defn test:defnt'
  [sym & body])

(defn test:defntp
  [sym & body])

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
