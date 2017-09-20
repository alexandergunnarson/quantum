(ns quantum.test.core.defnt
  (:require
    [quantum.core.core
      :refer [istr]]
    [quantum.core.defnt        :as this
      :refer [->type-info ->expr-info !ref ->typed]]
    [quantum.core.test         :as test
      :refer [deftest testing is is= throws]]
    [quantum.core.untyped.analyze.expr :as xp]
    [quantum.core.untyped.type :as t])
#?(:clj
  (:import
    [clojure.lang Keyword Symbol]
    [quantum.core Numeric])))


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


(->typed {'n (!ref (->type-info {:infer? true}))}
  '(Numeric/isTrue (Numeric/isZero n)))


(deftest test:methods->spec
  (testing "Class hierarchy"
    (is=
      (this/methods->spec
        [{:rtype Object :argtypes [t/int t/char]}
         {:rtype Object :argtypes [String]}
         {:rtype Object :argtypes [CharSequence]}
         {:rtype Object :argtypes [Object]}
         {:rtype Object :argtypes [Comparable]}])
      false))
  (testing "Complex dispatch based off of `Numeric/bitAnd`"
    (let [method-data
           ]
      (is=
        (this/methods->spec
          [{:rtype t/int   :argtypes [t/int   t/char]}
           {:rtype t/int   :argtypes [t/int   t/byte]}
           {:rtype t/int   :argtypes [t/int   t/short]}
           {:rtype t/int   :argtypes [t/int   t/int]}
           {:rtype t/long  :argtypes [t/short t/long]}
           {:rtype t/int   :argtypes [t/short t/int]}
           {:rtype t/short :argtypes [t/short t/short]}
           {:rtype t/long  :argtypes [t/long  t/long]}
           {:rtype t/long  :argtypes [t/long  t/int]}
           {:rtype t/long  :argtypes [t/long  t/short]}
           {:rtype t/long  :argtypes [t/long  t/char]}
           {:rtype t/long  :argtypes [t/long  t/byte]}
           {:rtype t/long  :argtypes [t/int   t/long]}
           {:rtype t/char  :argtypes [t/char  t/byte]}
           {:rtype t/long  :argtypes [t/byte  t/long]}
           {:rtype t/int   :argtypes [t/byte  t/int]}
           {:rtype t/short :argtypes [t/byte  t/short]}
           {:rtype t/char  :argtypes [t/byte  t/char]}
           {:rtype t/byte  :argtypes [t/byte  t/byte]}
           {:rtype t/short :argtypes [t/short t/char]}
           {:rtype t/short :argtypes [t/short t/byte]}
           {:rtype t/long  :argtypes [t/char  t/long]}
           {:rtype t/long  :argtypes [t/char  t/long t/long]}
           {:rtype t/char  :argtypes [t/char  t/char]}
           {:rtype t/short :argtypes [t/char  t/short]}
           {:rtype t/int   :argtypes [t/char  t/int]}])
        (xp/casef count
          2 (xp/condpf-> t/>= (xp/get 0)
              t/int
                (xp/condpf-> t/>= (xp/get 1)
                  t/char  t/int
                  t/byte  t/int
                  t/short t/int
                  t/int   t/int
                  t/long  t/long)
              t/short
                (xp/condpf-> t/>= (xp/get 1)
                  t/long  t/long
                  t/int   t/int
                  t/short t/short
                  t/char  t/short
                  t/byte  t/short)
              t/long
                (xp/condpf-> t/>= (xp/get 1)
                  t/long  t/long
                  t/int   t/long
                  t/short t/long
                  t/char  t/long
                  t/byte  t/long)
              t/char
                (xp/condpf-> t/>= (xp/get 1)
                  t/byte  t/char
                  t/long  t/long
                  t/char  t/char
                  t/short t/short
                  t/int   t/int)
              t/byte
                (xp/condpf-> t/>= (xp/get 1)
                  t/long  t/long
                  t/int   t/int
                  t/short t/short
                  t/char  t/char
                  t/byte  t/byte))
          3 (xp/condpf-> t/>= (xp/get 0)
              t/char
                (xp/condpf-> t/>= (xp/get 1)
                  t/long
                    (xp/condpf-> t/>= (xp/get 2)
                      t/long t/long))))))))


(def ff this/fn-type-satisfies-expr?)

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
        (binding [this/*conditional-branch-pruning?* pruning?]
          (doseq [pred (get objects branch)]
            (is= (->typed (list 'if pred true-form false-form))
                 ((get-in ->typed:if:test-cases [pruning? true-form false-form branch])
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

(def count:rf (aritoid + identity (rcomp firsta inc)))

(defn reduce-count
  {:performance "On non-counted collections, `count` is 71.542581 ms, whereas
                 `reduce-count` is 36.824665 ms - twice as fast"}
  [xs] (reduce count:rf xs))

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
  #?(:clj  ([x !map:byte->any?            , k ?                ] (.get x k)))
  #?(:clj  ([x !map:char->any?            , k ?                ] (.get x k)))
  #?(:clj  ([x !map:short->any?           , k ?                ] (.get x k)))
  #?(:clj  ([x !map:int->any?             , k ?                ] (.get x k)))
  #?(:clj  ([x !map:long->any?            , k ?                ] (.get x k)))
  #?(:clj  ([x !map:float->ref?           , k ?                ] (.get x k)))
  #?(:clj  ([x !map:double->ref?          , k ?                ] (.get x k)))
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
(definterface boolean•I•char   (^boolean invoke [^char   a0]))
(definterface boolean•I•int    (^boolean invoke [^int    a0]))
(definterface boolean•I•long   (^boolean invoke [^long   a0]))
(definterface boolean•I•float  (^boolean invoke [^float  a0]))
(definterface boolean•I•double (^boolean invoke [^double a0]))

(def zero? (reify boolean•I•byte   (^boolean invoke [this ^byte   n] (Numeric/isZero n))
                  boolean•I•char   (^boolean invoke [this ^char   n] (Numeric/isZero n))
                  boolean•I•int    (^boolean invoke [this ^int    n] (Numeric/isZero n))
                  boolean•I•long   (^boolean invoke [this ^long   n] (Numeric/isZero n))
                  boolean•I•float  (^boolean invoke [this ^float  n] (Numeric/isZero n))
                  boolean•I•double (^boolean invoke [this ^double n] (Numeric/isZero n))))

(defnt zero? [n ?] (Numeric/isZero n))


