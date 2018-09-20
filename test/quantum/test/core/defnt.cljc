(ns quantum.test.core.defnt
  (:require
    [clojure.core    :as core]
    [criterium.core  :as bench]
    [quantum.core.fn :as fn
      :refer [fn->]]
    [quantum.core.logic
      :refer [fn-and]]
    [quantum.core.defnt                :as this
      :refer [!ref analyze defnt]]
    [quantum.core.macros.type-hint     :as th]
    [quantum.core.type.defs            :as tdef]
    [quantum.untyped.core.analyze.ast  :as ast]
    [quantum.untyped.core.analyze.expr :as xp]
    [quantum.untyped.core.form
      :refer [$ code=]]
    [quantum.untyped.core.form.type-hint
      :refer [tag]]
    [quantum.untyped.core.spec         :as s]
    [quantum.untyped.core.string
      :refer [istr]]
    [quantum.untyped.core.test         :as test
      :refer [deftest testing is is= throws]]
    [quantum.untyped.core.type         :as t])
#?(:clj
  (:import
    [clojure.lang Keyword Symbol]
    [quantum.core Numeric])))

;; ===== Type predicates ===== ;;
;; Declared here instead of in `quantum.untyped.core.type` to avoid dependency

#?(:clj (def boolean? (t/isa? #?(:clj Boolean :cljs js/Boolean))))
#?(:clj (def byte?    (t/isa? Byte)))
#?(:clj (def short?   (t/isa? Short)))
#?(:clj (def char?    (t/isa? Character)))
#?(:clj (def int?     (t/isa? Integer)))
#?(:clj (def long?    (t/isa? Long)))
#?(:clj (def float?   (t/isa? Float)))
        (def double?  (t/isa? #?(:clj Double :cljs js/Number)))

(def string? (t/isa? #?(:clj java.lang.String :cljs js/String)))

;; ===== End type predicates ===== ;;

(deftest test|arg-types>split
  (is= (this/arg-types>split
         [(t/or byte? double? string?)
          (t/or t/map? byte?)])
       [[(t/isa? Byte)   (t/isa? clojure.lang.ITransientMap)]
        [(t/isa? Byte)   (t/isa? clojure.lang.IPersistentMap)]
        [(t/isa? Byte)   (t/isa? java.util.Map)]
        [(t/isa? Byte)   (t/isa? Byte)]
        [(t/isa? Double) (t/isa? clojure.lang.ITransientMap)]
        [(t/isa? Double) (t/isa? clojure.lang.IPersistentMap)]
        [(t/isa? Double) (t/isa? java.util.Map)]
        [(t/isa? Double) (t/isa? Byte)]
        [(t/isa? String) (t/isa? clojure.lang.ITransientMap)]
        [(t/isa? String) (t/isa? clojure.lang.IPersistentMap)]
        [(t/isa? String) (t/isa? java.util.Map)]
        [(t/isa? String) (t/isa? Byte)]]))

;; ============== OLD TESTS ============== ;;

(deftest test|methods->spec
  (testing "Class hierarchy"
    (is=
      (this/methods->spec
        [{:rtype Object :argtypes [int? char?]}
         {:rtype Object :argtypes [String]}
         {:rtype Object :argtypes [CharSequence]}
         {:rtype Object :argtypes [Object]}
         {:rtype Object :argtypes [Comparable]}])
      (xp/casef count
        1 (xp/condpf-> t/<= (xp/get 0)
            (t/? string?)       (t/? t/object?)
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

(def truthy-objects [1 1.0 1N 1M "abc" :abc])
(def falsey-objects [nil])
(def objects {true truthy-objects false falsey-objects})

;; ----- Overload resolution -----

(defnt +*
  "Lax `+`. Continues on overflow/underflow."
  {:variadic-proxy? true}
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
  {:variadic-proxy? true}
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

;; ===== Dynamicity ===== ;;

(definterface Abcde (^long abcdemethod [^int a ^byte b]))
(def ^Abcde abcde (reify Abcde (abcdemethod [this a b] (inc a))))
(defn fghij [] (.abcdemethod abcde 1 5))
(is= (fghij) 2)
(def ^Abcde abcde (reify Abcde (abcdemethod [this a b] (+ a 2))))
(is= (fghij) 3)
(def abcde nil) ; To simulate clearing of unnecessary/invalid code
(throws NullPointerException (fghij))

;; This approach then benefits from the optional staticizing of vars in 1.8

;; ===== Performance ===== ;;

;; As we can see, there is definitely benefit (50% performance increase in this case) to be gained
;; from primitive overloads, but there is no apparent benefit to pre-casting in the reify for
;; reference types.
;; This is actually nice because we don't have to generate as much interface code.

(definterface PrimitiveIntTest (^long invoke [^int a]))
(def ^PrimitiveIntTest primitive-int-test|reify
  (reify PrimitiveIntTest (invoke [this a] (Numeric/add a 1))))
(defn primitive-int-test|primitive-fn [^long a] (Numeric/add a 1))
(defn primitive-int-test|fn [a] (Numeric/add (long a) 1))

(let [a (int 1)
      ^PrimitiveIntTest primitive-int-test|reify|direct @#'primitive-int-test|reify
      primitive-int-test|primitive-fn|direct @#'primitive-int-test|primitive-fn
      primitive-int-test|fn|direct @#'primitive-int-test|fn]
  ;; ~3.33 ns
  (bench/quick-bench (.invoke primitive-int-test|reify|direct a))
  ;; ~7.24 ns
  (bench/quick-bench (.invoke ^PrimitiveIntTest @#'primitive-int-test|reify a))
  ;; ~5.00 ns
  (bench/quick-bench (primitive-int-test|primitive-fn|direct a))
  ;; ~7.93 ns
  (bench/quick-bench (@#'primitive-int-test|primitive-fn a))
  ;; ~4.99 ns
  (bench/quick-bench (primitive-int-test|fn|direct a))
  ;; ~7.98 ns
  (bench/quick-bench (@#'primitive-int-test|fn a)))

(definterface String>Object|Test (^Object invoke [^String a]))
(def ^String>Object|Test string-test|reify
  (reify String>Object|Test (invoke [this a] (.length a))))
(definterface Object>Object|Test (^Object invoke [^Object a]))
(def ^Object>Object|Test string-test|generic-reify
  (reify Object>Object|Test (invoke [this a] (let [^String a a] (.length a)))))
(defn string-test|fn [^String a] (.length a))

(let [a ""
      ^String>Object|Test string-test|reify|direct @#'string-test|reify
      ^Object>Object|Test string-test|generic-reify|direct @#'string-test|generic-reify
      string-test|fn|direct @#'string-test|fn]
  ;; ~4.09 ns
  (bench/quick-bench (.invoke string-test|reify|direct a))
  ;; ~7.60 ns
  (bench/quick-bench (.invoke ^String>Object|Test @#'string-test|reify a))
  ;; ~3.94 ns
  (bench/quick-bench (.invoke string-test|generic-reify|direct a))
  ;; ~7.61 ns
  (bench/quick-bench (.invoke ^Object>Object|Test @#'string-test|generic-reify a))
  ;; ~3.94 ns
  (bench/quick-bench (string-test|fn|direct a))
  ;; ~7.55 ns
  (bench/quick-bench (@#'string-test|fn a)))

;; ============== Taken from untyped tests; should be modified in lock step =============== ;;

;; ----- Implicit compilation tests ----- ;;

(this/defnt abcde "Documentation" {:metadata "fhgjik"}
  ([a number? > number?] (inc a))
  ([a pos-int?, b pos-int?
    | (> a b)
    > (s/and number? #(> % a) #(> % b))] (+ a b))
  ([a #{"a" "b" "c"}
    b boolean?
    {:as   c
     :keys [ca keyword? cb string?]
     {:as cc
      {:as   cca
       :keys [ccaa keyword?]
       [[ccabaa some? {:as ccabab :keys [ccababa some?]} some?] some? ccabb some? & ccabc some? :as ccab]
       [:ccab seq?]}
      [:cca map?]}
     [:cc map?]}
    #(-> % count (= 3))
    [da double? & db seq? :as d] sequential?
    [ea symbol?] ^:gen? (s/coll-of symbol? :kind vector?)
    & [fa #{"a" "b" "c"} :as f] seq?
    | (and (> da 50) (contains? c a)
           a b c ca cb cc cca ccaa ccab ccabaa ccabab ccababa ccabb ccabc d da db ea f fa)
    > number?] 0))

(this/defns basic [a number? > number?] (rand))

(defspec-test test|basic `basic)

(this/defns equality [a number? > #(= % a)] a)

(defspec-test test|equality `equality)

(this/defns pre-post [a number? | (> a 3) > #(> % 4)] (inc a))

(defspec-test test|pre-post `pre-post)

(this/defns gen|seq|0 [[a number? b number? :as b] ^:gen? (s/tuple double? double?)])

(defspec-test test|gen|seq|0 `gen|seq|0)

(this/defns gen|seq|1
  [[a number? b number? :as b] ^:gen? (s/nonconforming (s/cat :a double? :b double?))])

(defspec-test test|gen|seq|1 `gen|seq|1)
