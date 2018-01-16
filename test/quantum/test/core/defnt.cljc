(ns quantum.test.core.defnt
  (:require
    [clojure.core :as core]
    [quantum.core.core
      :refer [istr]]
    [quantum.core.fn :as fn
      :refer [fn->]]
    [quantum.core.logic
      :refer [fn-and]]
    [quantum.core.defnt        :as this
      :refer [!ref analyze defnt]]
    [quantum.core.spec         :as s]
    [quantum.core.test         :as test
      :refer [deftest testing is is= throws]]
    [quantum.core.untyped.analyze.ast  :as ast]
    [quantum.core.untyped.analyze.expr :as xp]
    [quantum.core.untyped.core
      :refer [code=]]
    [quantum.core.untyped.type :as t])
#?(:clj
  (:import
    [clojure.lang Keyword Symbol]
    [quantum.core Numeric])))

(def test-data|abc
  `(defnt ~'abc []))

(this/fnt|overload-data->overload {:lang :clj}
  (s/validate (rest test-data|abc) ::this/defnt)
  test-data|abc)

(deftest fnt|overloads>protocol
  (testing
    (is (code=
          (fnt|overloads>protocol
            {:fn|name   'abc
             :overloads
               })
          {:defprotocol
             `(defprotocol ~'abc|gen__Protocol__
                ())
           :extend-protocols

           :defn
             }))))

(deftest test|methods->spec
  (testing "Class hierarchy"
    (is=
      (this/methods->spec
        [{:rtype Object :argtypes [t/int? t/char?]}
         {:rtype Object :argtypes [String]}
         {:rtype Object :argtypes [CharSequence]}
         {:rtype Object :argtypes [Object]}
         {:rtype Object :argtypes [Comparable]}])
      (xp/casef count
        1 (xp/condpf-> t/<= (xp/get 0)
            (t/? t/string?)     (t/? t/object?)
            (t/? t/char-seq?)   (t/? t/object?)
            (t/? t/comparable?) (t/? t/object?)
            (t/? t/object?)     (t/? t/object?))
        2 (xp/condpf-> t/<= (xp/get 0)
            t/int? (xp/condpf-> t/<= (xp/get 1)
                    t/char? (t/? t/object?))))))
  (testing "Complex dispatch based off of `Numeric/bitAnd`"
    (is=
      (this/methods->spec
        [{:rtype t/int?   :argtypes [t/int?   t/char?]}
         {:rtype t/int?   :argtypes [t/int?   t/byte?]}
         {:rtype t/int?   :argtypes [t/int?   t/short?]}
         {:rtype t/int?   :argtypes [t/int?   t/int?]}
         {:rtype t/long?  :argtypes [t/short? t/long?]}
         {:rtype t/int?   :argtypes [t/short? t/int?]}
         {:rtype t/short? :argtypes [t/short? t/short?]}
         {:rtype t/long?  :argtypes [t/long?  t/long?]}
         {:rtype t/long?  :argtypes [t/long?  t/int?]}
         {:rtype t/long?  :argtypes [t/long?  t/short?]}
         {:rtype t/long?  :argtypes [t/long?  t/char?]}
         {:rtype t/long?  :argtypes [t/long?  t/byte?]}
         {:rtype t/long?  :argtypes [t/int?   t/long?]}
         {:rtype t/char?  :argtypes [t/char?  t/byte?]}
         {:rtype t/long?  :argtypes [t/byte?  t/long?]}
         {:rtype t/int?   :argtypes [t/byte?  t/int?]}
         {:rtype t/short? :argtypes [t/byte?  t/short?]}
         {:rtype t/char?  :argtypes [t/byte?  t/char?]}
         {:rtype t/byte?  :argtypes [t/byte?  t/byte?]}
         {:rtype t/short? :argtypes [t/short? t/char?]}
         {:rtype t/short? :argtypes [t/short? t/byte?]}
         {:rtype t/long?  :argtypes [t/char?  t/long?]}
         {:rtype t/long?  :argtypes [t/char?  t/long? t/long?]}
         {:rtype t/char?  :argtypes [t/char?  t/char?]}
         {:rtype t/short? :argtypes [t/char?  t/short?]}
         {:rtype t/int?   :argtypes [t/char?  t/int?]}])
      (xp/casef count
        2 (xp/condpf-> t/<= (xp/get 0)
            t/int?
              (xp/condpf-> t/<= (xp/get 1)
                t/char?  t/int?
                t/byte?  t/int?
                t/short? t/int?
                t/int?   t/int?
                t/long?  t/long?)
            t/short?
              (xp/condpf-> t/<= (xp/get 1)
                t/long?  t/long?
                t/int?   t/int?
                t/short? t/short?
                t/char?  t/short?
                t/byte?  t/short?)
            t/long?
              (xp/condpf-> t/<= (xp/get 1)
                t/long?  t/long?
                t/int?   t/long?
                t/short? t/long?
                t/char?  t/long?
                t/byte?  t/long?)
            t/char?
              (xp/condpf-> t/<= (xp/get 1)
                t/byte?  t/char?
                t/long?  t/long?
                t/char?  t/char?
                t/short? t/short?
                t/int?   t/int?)
            t/byte?
              (xp/condpf-> t/<= (xp/get 1)
                t/long?  t/long?
                t/int?   t/int?
                t/short? t/short?
                t/char?  t/char?
                t/byte?  t/byte?))
        3 (xp/condpf-> t/<= (xp/get 0)
            t/char?
              (xp/condpf-> t/<= (xp/get 1)
                t/long?
                  (xp/condpf-> t/<= (xp/get 2)
                    t/long? t/long?)))))))

(deftest test|analyze-seq|do
  (is= (analyze '(do))
       (ast/do {:env  {}
                :form '(do)
                :body []
                :spec t/nil?}))
  (is= (analyze '(do 1))
       (ast/do {:env  {}
                :form '(do 1)
                :body [1]
                :spec (t/value 1)}))
  (is= (analyze '(do 1 "a"))
       (ast/do {:env  {}
                :form '(do 1 "a")
                :body [1 "a"]
                :spec (t/value "a")})))

(deftest test|analyze
  (testing "symbol"
    (testing "unbound"
      (is= (analyze {'c (ast/unbound 'c)} 'c)
           (ast/unbound 'c))))
  (testing "static call"
    (testing "literal arguments"
      (is= (analyze '(Numeric/bitAnd 1 2))
           (ast/macro-call
             {:form     '(Numeric/bitAnd 1 2),
              :expanded (ast/method-call
                          {:env    {}
                           :form   '(. Numeric bitAnd 1 2)
                           :target (ast/symbol 'Numeric t/class?)
                           :method 'bitAnd
                           :args   [(ast/literal 1 (t/value 1))
                                    (ast/literal 2 (t/value 2))]
                           :spec   t/long?}) ;; TODO more specific than this?
              :spec     t/long?})) ;; TODO more specific than this?
      (throws (analyze '(Numeric/bitAnd 1.0 2.0))
        (fn-and (fn-> :message (= "No matching clause found"))
                (fn-> :data    (= {:v (t/value 1.0)}))))
      (throws (analyze '(Numeric/bitAnd 1.0 2))
        (fn-and (fn-> :message (= "No matching clause found"))
                (fn-> :data    (= {:v (t/value 1.0)}))))
      (throws (analyze '(Numeric/bitAnd 1 2.0))
        (fn-and (fn-> :message (= "No matching clause found"))
                (fn-> :data    (= {:v (t/value 2.0)}))))
      (throws (analyze '(Numeric/bitAnd "" 2.0))
        (fn-and (fn-> :message (= "No matching clause found"))
                (fn-> :data    (= {:v (t/value "")}))))
      (throws (analyze '(Numeric/bitAnd nil 2.0))
        (fn-and (fn-> :message (= "No matching clause found"))
                (fn-> :data    (= {:v t/nil?}))))

      (is= (analyze '(byte 1))
           (ast/macro-call
             {:form '(byte 1)
              :expanded (ast/method-call
                          {:env    {}
                           :form   '(. clojure.lang.RT (uncheckedByteCast 1))
                           :target (ast/symbol 'clojure.lang.RT t/class?)
                           :method 'uncheckedByteCast
                           :args   [(ast/literal 1 t/long?)]
                           :spec   t/byte?})
              :spec t/byte?}))
      (throws (analyze '(byte "")) ; TODO fix
        (fn-> :message (= "Spec assertion failed"))))
    (testing "unbound arguments"
      (analyze {'a (ast/unbound 'a)}
        '(Numeric/isZero a)))))

(let* [a 1 b (byte 2)]
                a
                (Numeric/add c (Numeric/bitAnd a b)))

;; For any unquoted seq-expression E that has at least one leaf:
;;   if E is an expression whose type must be inferred:
;;     if E has not reached stability (stability = only one reified, TODO what about abstracts?)
;;       E's type must itself be inferred

#_(let [gen-unbound
        #(!ref (->type-info
                 {:reifieds #{}
                  :infer? true}))
      gen-expected
        (fn [env ast]
          [env ast]
          #_(->expr-info
            {:env  env
             :form form
             :type-info
               (->type-info type-info)}))]
  #_(let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(and:boolean a b)]
    (is= (->typed env form)
         (gen-expected form env
           {:reifieds  #{boolean}
            :abstracts #{#_...}
            #_:conditionals
              #_{boolean {boolean #{boolean}}}})))
  #_(let [env  {'a (gen-unbound)}
        form '(Numeric/isZero a)]
    (is= (-> (->typed env form)
             (assoc-in [:type-info :fn-types] nil))
         (gen-expected
           {'a (!ref (t/ast 'a ? (t/or t/byte t/char t/short t/int t/long t/float t/double)))}
           (t/ast '(. Numeric isZero a) (t/fn' t/boolean)))))
  #_(let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(Numeric/bitAnd a b)]
    (is= nil #_(->typed env form)
         (gen-expected
           {'a (!ref (t/ast 'a ? (t/or t/byte t/char t/short t/int t/long)))
            'b (!ref (t/ast 'b ? (t/or t/byte t/char t/short t/int t/long)))}
           (t/ast '(. Numeric bitAnd a b)
             ;; TODO make spec fns easily editable
             (t/spec [[a0 a1]] ; input is sequence of arg-specs; return value is spec
               (condp = a0
                 ;; TODO use map lookup?
                 t/byte  (condp = a1
                           t/byte  t/byte
                           t/char  t/char
                           t/short t/short
                           t/int   t/int
                           t/long  t/long)
                 t/char  (condp = a1
                           t/byte  t/char
                           t/char  t/char
                           t/short t/short
                           t/int   t/int
                           t/long  t/long)
                 t/short (condp = a1
                           t/byte  t/short
                           t/char  t/short
                           t/short t/short
                           t/int   t/int
                           t/long  t/long)
                 t/int   (condp = a1
                           t/byte  t/int
                           t/char  t/int
                           t/short t/int
                           t/int   t/int
                           t/long  t/long)
                 t/long  (condp = a1
                           t/byte  t/long
                           t/char  t/long
                           t/short t/long
                           t/int   t/long
                           t/long  t/long)))))))

  (let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(Numeric/negate (Numeric/bitAnd a b))]
    (is= #_(->typed env form)
         (gen-expected form
           {'a (!ref (t/ast 'a ? ...))
            'b (!ref (t/ast 'b ? ...))}
           (t/ast '(. Numeric bitAnd a b))
           {:reifieds  #{byte char short int long}
            :abstracts #{...}})))
  #_(let [env  {'a (gen-unbound)
              'b (gen-unbound)}
        form '(negate:int|long (Numeric/bitAnd a b))]
    ;; Because the only valid argtypes to `negate:int|long` are S = #{[int] [long]},
    ;; `Numeric/bitAnd` must only accept argtypes that produce a subset of S
    ;; The argtypes to `Numeric/bitAnd` that produce a subset of S are:
    #_#{[byte  int]
        [byte  long]
        [char  int]
        [char  long]
        [short int]
        [short long]
        [int   byte]
        [int   char]
        [int   short]
        [int   int]
        [int   long]
        [long  byte]
        [long  char]
        [long  short]
        [long  int]
        [long  long]}
    ;; So `a`, then, can be:
    #_#{byte char short int long}
    ;; and likewise `b` can be:
    #_#{byte char short int long}
    (is= (->typed env form)
         (gen-expected form
           {'a (!ref (->type-info
                       {:reifieds #{byte char short int long}
                        :fn-types {}
                        :infer? true}))
            'b (!ref (->type-info
                       {:reifieds #{byte char short int long}
                        :fn-types {}
                        :infer? true}))}
           {:reifieds  #{int long}
            :abstracts #{...}}))))

(def ff this/fn-type-satisfies-expr?)

(deftest test|fn-type-satisfies-expr?
  (is= (ff )))

(defn test|->typed|literal-equivalence [f formf]
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

(deftest test|->typed|literals
  (test|->typed|literal-equivalence ->typed identity))

(deftest test|->typed|do
  (testing "Base case"
    (is= (->typed '(do))
         (->expr-info {:env {} :form nil
                       :type-info (->type-info {:reifieds #{:nil}})})))
  (testing "Literals"
    (test|->typed|literal-equivalence #(->typed (list 'do %)) #(list 'do %))))

(deftest test|->typed|let
  (testing "Base case"
    (is= (->typed '(let []))
         (->expr-info {:env {} :form '(let* [] (do))})))
  (testing "Literals"
    (test|->typed|literal-equivalence
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

(deftest test|->typed|if
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
        (binding [this/*conditional-branch-pruning?* pruning?]
          (doseq [pred (get objects branch)]
            (is= (->typed (list 'if pred true-form false-form))
                 ((get-in ->typed|if|test-cases [pruning? true-form false-form branch])
                  pred true-form false-form))))))))



;; ----- Overload resolution -----

; TODO use logic programming and variable unification e.g. `?1` `?2` ?

(defnt +*
  "Lax `+`. Continues on overflow/underflow."
  {:variadic-proxy true}
  ([] 0)
  ;; Here `Number`, determined to be a class, is treated like an `instance?` predicate
  ([a (t/or numeric-primitive? Number)] a)
  ;; Note that you can envision any function arglist as an s/cat
  ([a ?, b ?] ; ? is t/?, an alias for t/infer
    (Numeric/add a b)) ; uses reflection to infer types
  ([a BigInt    , b BigInt    ] (.add a b))
  ([a BigDecimal, b BigDecimal]
    (if (nil? *math-context*)
        (.add x y)
        (.add x y *math-context*)))
  ;; Protocols cannot participate in variadic arities, but we can get around this
  ;; TODO auto-gen extensions to variadic arities like [a b c], [a b c d], etc.
  ([a ?, b ? & args ?] (apply +* (+* a b) args))) ; the `apply` used in a typed context uses `reduce` underneath the covers

(defnt +'
  "Strict `+`. Throws exception on overflow/underflow."
  {:variadic-proxy true}
  ([a int? , b int? ] (Math/addExact a b))
  ([a long?, b long?] (Math/addExact x y))
  ; TODO do the rest
  ([a (t/or numeric-primitive? Number)] a))

(defnt bit-and [n ?] (Numeric/bitAnd n))

(defnt zero? [n ?] (Numeric/isZero n))

(defnt even? [n ?] (zero? (bit-and n 1)))

(defnt +*-even
  "Lax `+` on only even numbers."
  [a even?, b even?] (+* a b))

(defnt + [a numerically-byte?
          b numerically-byte?]
  ...)

; ===== COLLECTIONS ===== ;

(def count|rf (aritoid + identity (rcomp firsta inc)))

(defn reduce-count
  {:performance "On non-counted collections, `count` is 71.542581 ms, whereas
                 `reduce-count` is 36.824665 ms - twice as fast"}
  [xs] (reduce count|rf xs))

(defnt ^:inline name
           ([x string?] x)
  #?(:clj  ([x Named  ] (.getName x))
     :cljs ([x INamed ] (-name x))))

; the order encountered is the preferred order in case of ambiguity
; Some things tracked include arity of function, arguments to function, etc.
; Lazily compiled; will cause a chain reaction of compilations
; Have the choice to AOT compile *everything* but that isn't a good idea probably...

(defnt ^:inline count
  "Incorporated `clojure.lang.RT/count` and `clojure.lang.RT/countFrom`"
  {:todo #{"handle persistent maps"}}
           ([x nil?                 ] 0)
           ([x array?               ] (#?(:clj Array/count :cljs .-length) x))
           ([x tuple?               ] (count (.-vs x)))
  #?(:clj  ([x Map$Entry            ] 2))
  #?(:cljs ([x string?              ] (.-length   x)))
  #?(:cljs ([x !string?             ] (.getLength x)))
  #?(:clj  ([x char-seq?            ] (.length x)))
           ([x ?                    ] (count (name x)))
           ([x m2m-chan?            ] (count (#?(:clj .buf :cljs .-buf) x)))
           ([x +vector?             ] (#?(:clj .count :cljs core/count) x))
  #?(:clj  ([x (s/or Collection Map)] (.size x)))
  #?(:clj  ([x Counted              ] (.count x)))
           ([x transformer?         ] (reduce-count x))
           ([x IPersistentCollection]
             (core/count x)
             ; ISeq s = seq(o);
             ; o = null;
             ; int i = 0;
             ; for(; s != null; s = s.next()) {
             ;   if(s instanceof Counted)
             ;     return i + s.count();
             ;   i++;
             ; }
             ; return i;
             ))

(defnt ^:inline get
  {:imported    "clojure.lang.RT/get"
   :performance "(java.lang.reflect.Array/get coll n) is about 4 times faster than core/get"}
           ([x nil?                       , k ?                ] nil)
           ([x tuple?                     , k ?                ] (get (.-vs x) k))
  #?(:clj  ([x ILookup                    , k ?                ] (.valAt x k)))
           ([x nil?                       , k ?, if-not-found ?] nil)
  #?(:clj  ([x ILookup                    , k ?, if-not-found ?] (.valAt x k if-not-found)))
  #?(:clj  ([x (s/or Map IPersistentSet)  , k ?                ] (.get x k)))
  #?(:clj  ([x !map|byte->any?            , k ?                ] (.get x k)))
  #?(:clj  ([x !map|char->any?            , k ?                ] (.get x k)))
  #?(:clj  ([x !map|short->any?           , k ?                ] (.get x k)))
  #?(:clj  ([x !map|int->any?             , k ?                ] (.get x k)))
  #?(:clj  ([x !map|long->any?            , k ?                ] (.get x k)))
  #?(:clj  ([x !map|float->ref?           , k ?                ] (.get x k)))
  #?(:clj  ([x !map|double->ref?          , k ?                ] (.get x k)))
           ([x string?                    , k ?, if-not-found ?] (if (>= k (count x)) if-not-found (.charAt x k)))
  #?(:clj  ([x !array-list?               , k ?, if-not-found ?] (if (>= k (count x)) if-not-found (.get    x k))))
           ([x (s/or string? !array-list?), k ?                ] (get x k nil))
  #?(:cljs ([x array-1d?                  , k js-integer?      ] (core/aget x k)))
  #?(:clj  ([x ?                          , k ?                ] (Array/get x k))))

(defnt transformer
  "Given a reducible collection, and a transformation function transform,
  returns a reducible collection, where any supplied reducing
  fn will be transformed by transform. transform is a function of reducing fn to
  reducing fn."
  ([xs reducible?, xf xfn?]
    (if (instance? Transformer xs)
        (Transformer. (.-xs ^Transformer xs) xs xf)
        (Transformer. xs                     xs xf))))

(defnt transducer->transformer
  "Converts a transducer into a transformer."
  {:todo #{"More arity"}}
  ([n ?, xf xfn?]
    (case n
          0 (fn ([]            (xf))
                ([xs]          (transformer xs (xf))))
          1 (fn ([a0]          (xf a0))
                ([a0 xs]       (transformer xs (xf a0))))
          2 (fn ([a0 a1]       (xf a0 a1))
                ([a0 a1 xs]    (transformer xs (xf a0 a1))))
          3 (fn ([a0 a1 a2]    (xf a0 a1 a2))
                ([a0 a1 a2 xs] (transformer xs (xf a0 a1 a2))))
          (throw (ex-info "Unhandled arity for transducer" nil)))))

(defnt map:transducer [f ?]
  ; TODO what does this actually entail? should it be that it errors on `f`s that don't implement *all* possible arities?
  (fnt [rf ?]
    (fn ; TODO auto-generate? ; TODO `fnt` ?
      ([]                  (rf))
      ([ret]               (rf ret))
      ([ret x0]            (rf ret       (f x0)))
      ([ret x0 x1]         (rf ret       (f x0 x1)))
      ([ret x0 x1 x2]      (rf ret       (f x0 x1 x2)))
      ([ret x0 x1 x2 & xs] (rf ret (apply f x0 x1 x2 xs))))))

(def map+ (transducer->transformer 1 map:transducer))

(defnt get-in*
  ([x ? k0 ?]                                              (get x k0))
  ([x ? k0 ? k1 ?]                                         (Array/get x k0 k1))
  ([x ? k0 ? k1 ? k2 ?]                                    (Array/get x k0 k1 k2))
  ([x ? k0 ? k1 ? k2 ? k3 ?]                               (Array/get x k0 k1 k2 k3))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ?]                          (Array/get x k0 k1 k2 k3 k4))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ?]                     (Array/get x k0 k1 k2 k3 k4 k5))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ?]                (Array/get x k0 k1 k2 k3 k4 k5 k6))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ? k7 ?]           (Array/get x k0 k1 k2 k3 k4 k5 k6 k7))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ? k7 ? k8 ?]      (Array/get x k0 k1 k2 k3 k4 k5 k6 k7 k8))
  ([x ? k0 ? k1 ? k2 ? k3 ? k4 ? k5 ? k6 ? k7 ? k8 ? k9 ?] (Array/get x k0 k1 k2 k3 k4 k5 k6 k7 k8 k9))
  ([x ? k0 ? k1 ?]                                         (-> x (get k0) (get k1)))))

(argtypes get-in*) #_"=>" #_[[booleans          int]
                             [bytes             int]
                             ...
                             [IPersistentVector long]
                             ...
                             [ints              int int]
                             ...
                             [IPersistentVector long long]]

(defonce *interfaces (atom {}))

; IF AN EAGER RESULT:

; +* 0 arity
(definterface long•I (^long invoke []))

; `+*` 1 arity
(definterface byte•I•byte     (^byte   invoke [^byte   a0]))
(definterface char•I•char     (^char   invoke [^char   a0]))
(definterface int•I•int       (^int    invoke [^int    a0]))
(definterface long•I•long     (^long   invoke [^long   a0]))
(definterface float•I•float   (^float  invoke [^float  a0]))
(definterface double•I•double (^double invoke [^double a0]))

; `+*` 2-arity
(definterface byte•I•byte     (^byte   invoke [^byte   a0 ...]))
(definterface char•I•char     (^char   invoke [^char   a0 ...]))
(definterface int•I•int       (^int    invoke [^int    a0 ...]))
(definterface long•I•long     (^long   invoke [^long   a0 ...]))
(definterface float•I•float   (^float  invoke [^float  a0 ...]))
(definterface double•I•double (^double invoke [^double a0 ...]))
(definterface double•I•double (^double invoke [^double a0 ...]))
...

; `+*` 2-arity variadic
?

(definterface boolean•I•byte   (^boolean invoke [^byte   a0]))
(or (get @*interfaces 'boolean•I•byte) (swap! *interfaces assoc 'boolean•I•byte boolean•I•byte))
(definterface boolean•I•char   (^boolean invoke [^char   a0]))
(or (get @*interfaces 'boolean•I•char) (swap! *interfaces assoc 'boolean•I•char boolean•I•char))
(definterface boolean•I•int    (^boolean invoke [^int    a0]))
(swap! *interfaces assoc 'boolean•I•byte boolean•I•byte)
(definterface boolean•I•long   (^boolean invoke [^long   a0]))
(swap! *interfaces assoc 'boolean•I•byte boolean•I•byte)
(definterface boolean•I•float  (^boolean invoke [^float  a0]))
(swap! *interfaces assoc 'boolean•I•byte boolean•I•byte)
(definterface boolean•I•double (^boolean invoke [^double a0]))
(swap! *interfaces assoc 'boolean•I•byte boolean•I•byte)

(def zero? (reify boolean•I•byte   (^boolean invoke [this ^byte   n] (Numeric/isZero n))
                  boolean•I•char   (^boolean invoke [this ^char   n] (Numeric/isZero n))
                  boolean•I•int    (^boolean invoke [this ^int    n] (Numeric/isZero n))
                  boolean•I•long   (^boolean invoke [this ^long   n] (Numeric/isZero n))
                  boolean•I•float  (^boolean invoke [this ^float  n] (Numeric/isZero n))
                  boolean•I•double (^boolean invoke [this ^double n] (Numeric/isZero n))))

(defnt zero? [n ?] (Numeric/isZero n))







(defnt zero? ([n long] (Numeric/isZero n)))

(defn zero?)

(defnt zero? ([n ?] (Numeric/isZero n)))
(zero? 1)

(let [^boolean•I•double z zero?] (.invoke z 3.0)) ; it's just a simple reify

#_(defnt even?
  [n ?] (zero? (bit-and n 1)))
#_=>
(def even? (reify ))

; Normally `zero?` when passed e.g. as a higher-order function might be like

;; ----- Spec'ed `defnt+`s -----

;; One thing that would be nice is to marry `defnt` with `clojure.spec`.
;; We want the specs to be reflected in the parameter declaration, type hints, and so on.
;;
;; We also want it to know about e.g., since a function returns `(< 5 x 100)`, then x must
;; be not just a number, but *specifically* a number between 5 and 100, exclusive.
;; Non-`Collection` datatypes are opaque and do not participate in this benefit (?).
;;
;; core.spec functions like `s/or`, `s/and`, `s/coll-of`, and certain type predicates are
;; able to be leveraged in computing the best overload with the least dynamic dispatch
;; possible.

(defnt example
  ([a (s/and even? #(< 5 % 100))
    b t/any?
    c ::number-between-6-and-20
    d {:req-un [e  (default t/boolean? true)
                :f t/number?
                g  (default (s/or t/number? t/sequential?) 0)]}
    | (< a @c) ; pre
    > (s/and (s/coll odd? :kind t/array?) ; post
             #(= (first %) c))]
   ...)
  ([a string?
    b (s/coll bigdec? :kind vector?)
    c t/any?
    d t/any?
   ...))

;; expands to:

(dv/def ::example:a (s/and even? #(< 5 % 100)))
(dv/def ::example:b t/any)
(dv/def ::example:c ::number-between-6-and-20)
(dv/def-map ::example:d
  :conformer (fn [m#] (assoc-when-not-contains m# :e true :g 0))
  :req-un [[:e t/boolean?]
           [:f t/number?]
           [:g (s/or* t/number? t/sequential?)]])
(dv/def ::example|__ret
  (s/and (s/coll-of odd? :kind t/array?)
                 #(= (first %) (:c ...)))) ; TODO fix `...`

;; -> TODO should it be:
(defnt example
  [^example:a a ^:example|b b ^example|c c ^example|d d]
  (let [ret (do ...)]
    (validate ret ::example|__ret)))
;; -> OR
(defnt example
  [^number? a b ^number? c ^map? d]
  (let [ret (do ...)]
    (validate ret ::example|__ret)))
;; ? The issue is one of performance. Maybe we don't want boxed values all over the place.

(s/fdef example
  :args (s/cat :a ::example|a
               :b ::example|b
               :c ::example|c
               :d ::example|d)
  :fn   ::example|__ret)


;; ----- TYPE INFERENCE ----- ;;

(expr-info '(let [a (Integer. 2) b (Double. 3)] a))
; => {:class java.lang.Integer, :prim? false}
(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a b)))
; => nil
;    But I'd like to have it infer the "LCD", namely, `(v/and number? (v/or* (fn= 2) (fn= 3)))`.

;; I realize that this also is probably prohibitively expensive.

(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (int b))))
; => nil (inferred `Integer` or `int`)

(expr-info '(let [a (Integer. 2) b (Double. 3)] (if false a (Integer. b))))
; => {:class java.lang.Integer, :prim? false}

;; At very least it would be nice to have "spec inference". I.e. know, via `fdef`, that a
;; function meets a particular set of specs/characteristics and so any call to that function
;; will necessarily comply with the type.


