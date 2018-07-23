;; See https://jsperf.com/js-property-access-comparison — all property accesses (at least of length 1) seem to be equal

(ns quantum.core.test.defnt-equivalences
  (:refer-clojure :exclude [name identity *])
  (:require
    [clojure.core              :as c]
    [quantum.core.defnt
      :refer [analyze defnt fnt|code *fn->type unsupported!]]
    [quantum.untyped.core.analyze.expr :as xp]
    [quantum.untyped.core.collections.diff :as diff
      :refer [diff]]
    [quantum.untyped.core.core :as ucore
      :refer [code=]]
    [quantum.untyped.core.data.array
      :refer [*<>]]
    [quantum.untyped.core.form
      :refer [$]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env env-lang macroexpand-all]]
    [quantum.untyped.core.form.type-hint
      :refer [tag]]
    [quantum.untyped.core.logic
      :refer [ifs]]
    [quantum.untyped.core.spec         :as s]
    [quantum.untyped.core.test         :as test
      :refer [deftest is is= is-code= testing throws]]
    [quantum.untyped.core.type :as t
      :refer [? *]]
    [quantum.untyped.core.type.reifications :as utr])
  (:import
    clojure.lang.Named
    clojure.lang.Reduced
    clojure.lang.ISeq
    clojure.lang.ASeq
    clojure.lang.LazySeq
    clojure.lang.Seqable
    quantum.core.data.Array
    [quantum.core Numeric Primitive]))

#?(:clj
(deftest test|pid
  (let [actual
          (macroexpand '
            (defnt pid|test [> (? t/string?)]
              (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
                   (.getName))))
        expected
          ($ (do (def ~'pid|test|__0
                   (reify >Object
                     (~(tag "java.lang.Object" 'invoke) [~'_0__]
                       ~'(->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
                              (.getName)))))
                 (defn ~'pid|test
                   {::t/type (t/fn [:> ~'(? t/string?)])}
                   ([] (.invoke ~(tag "quantum.core.test.defnt_equivalences.>Object"
                                'pid|test|__0))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is (string? (pid|test)))
                 (throws (pid|test 1))))))))

;; TODO test `:inline`

(deftest test|identity|uninlined
  (let [actual
          (macroexpand '
            (defnt identity|uninlined ([x t/any?] x)))
        expected
          (case (env-lang)
            :clj
              ($ (do ;; [x t/any?]

                     (def ~(tag "[Ljava.lang.Object;" 'identity|uninlined|__0|input-types)
                       (*<> ~'t/any?))
                     ;; One `reify` because `t/any?` in CLJ does not have any `t/or`-separability
                     (def ~'identity|uninlined|__0
                       (reify
                         Object>Object   (~(tag "java.lang.Object" 'invoke)
                                           [~'_0__ ~(tag "java.lang.Object" 'x)] ~'x)
                         boolean>boolean (~(tag "boolean"          'invoke)
                                           [~'_1__ ~(tag "boolean"          'x)] ~'x)
                         byte>byte       (~(tag "byte"             'invoke)
                                           [~'_2__ ~(tag "byte"             'x)] ~'x)
                         short>short     (~(tag "short"            'invoke)
                                           [~'_3__ ~(tag "short"            'x)] ~'x)
                         char>char       (~(tag "char"             'invoke)
                                           [~'_4__ ~(tag "char"             'x)] ~'x)
                         int>int         (~(tag "int"              'invoke)
                                           [~'_5__ ~(tag "int"              'x)] ~'x)
                         long>long       (~(tag "long"             'invoke)
                                           [~'_6__ ~(tag "long"             'x)] ~'x)
                         float>float     (~(tag "float"            'invoke)
                                           [~'_7__ ~(tag "float"            'x)] ~'x)
                         double>double   (~(tag "double"           'invoke)
                                           [~'_8__ ~(tag "double"           'x)] ~'x)))

                     (defn ~'identity|uninlined
                       {::t/type (t/fn ~'[t/any?])}
                       ([~'x00__]
                         ;; Checks elided because `t/any?` doesn't require a check
                         ;; and all args are `t/=` `t/any?`
                         (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>Object"
                                        'identity|uninlined|__0) ~'x00__)))))
            :cljs
              ;; Direct dispatch will be simple functions, not `reify`s
              ($ (do (defn ~'identity|uninlined [~'x] ~'x))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is= (identity|uninlined 1 ) 1 )
                 (is= (identity|uninlined "") ""))))))

(deftest test|name
  (let [actual
          (macroexpand '
            (defnt #_:inline name|test
                       ([x t/string?       > t/string?]     x)
              #?(:clj  ([x (t/isa? Named)  > (* t/string?)] (.getName x))
                 :cljs ([x (t/isa? INamed) > (* t/string?)] (-name x)))))
        expected
          (case (env-lang)
            :clj
              ($ (do ;; Only direct dispatch for prims or for Object, not for subclasses of Object
                     ;; Return value can be primitive; in this case it's not
                     ;; The macro in a typed context will find the right dispatch at compile time

                     ;; [t/string?]

                     (def ~(tag "[Ljava.lang.Object;" 'name|test|__0|input-types)
                       (*<> ~'t/string?))
                     (def ~'name|test|__0
                       (reify Object>Object
                         (~(tag "java.lang.Object" 'invoke) [~'_0__ ~(tag "java.lang.Object" 'x)]
                           (let* [~(tag "java.lang.String" 'x) ~'x] ~'x))))

                     ;; [(t/isa? Named)]

                     (def ~(tag "[Ljava.lang.Object;" 'name|test|__1|input-types)
                       (*<> ~'(t/isa? Named)))
                     (def ~'name|test|__1
                       (reify Object>Object
                         (~(tag "java.lang.Object" 'invoke) [~'_1__ ~(tag "java.lang.Object" 'x)]
                           (let* [~(tag "clojure.lang.Named" 'x) ~'x]
                             (t/validate ~'(.getName x) ~'(* t/string?))))))

                     (defn ~'name|test
                       {::t/type
                         (t/fn ~'[t/string?      :> t/string?]
                               ~'[(t/isa? Named) :> (* t/string?)])}
                       ([~'x00__]
                         (ifs ((Array/get ~'name|test|__0|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>Object"
                                               'name|test|__0) ~'x00__)
                              ((Array/get ~'name|test|__1|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>Object"
                                               'name|test|__1) ~'x00__)
                              (unsupported! `name|test [~'x00__] 0))))))
            :cljs
              ($ (do (defn ~'name|test [~'x00__]
                     (ifs (t/string? x)         x
                          (satisfies? INamed x) (-name x)
                          (unsupported! `name|test [~'x00__] 0))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is= (name|test "")       "")
                 (is= (name|test "abc")    "abc")
                 (is= (name|test :abc)     "abc")
                 (is= (name|test 'abc)     "abc")
                 (is= (name|test :abc/def) "def")
                 (is= (name|test 'abc/def) "def")
                 (throws (name|test nil))
                 (throws (name|test 1)))))))

(deftest test|some?
  (let [actual
          ;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
          (macroexpand '
            (defnt #_:inline some?|test
              ([x t/nil?] false)
              ;; Implicitly, `(- t/any? t/nil?)`, so `t/val?`
              ([x t/any?] true)))
        expected
          (case (env-lang)
            :clj
              ($ (do ;; [x t/nil?]

                     (def ~(tag "[Ljava.lang.Object;" 'some?|test|__0|input-types)
                       (*<> ~'t/nil?))
                     (def ~'some?|test|__0
                       (reify
                         Object>boolean
                           (~(tag "boolean" 'invoke) [~'_0__ ~(tag "java.lang.Object" 'x)] false)))

                     ;; [x t/any?]

                     (def ~(tag "[Ljava.lang.Object;" 'some?|test|__1|input-types)
                       (*<> ~'t/any?))
                     (def ~'some?|test|__1
                       (reify
                         Object>boolean
                           (~(tag "boolean" 'invoke) [~'_1__ ~(tag "java.lang.Object" 'x)] true)
                         boolean>boolean
                           (~(tag "boolean" 'invoke) [~'_2__ ~(tag "boolean"          'x)] true)
                         byte>boolean
                           (~(tag "boolean" 'invoke) [~'_3__ ~(tag "byte"             'x)] true)
                         short>boolean
                           (~(tag "boolean" 'invoke) [~'_4__ ~(tag "short"            'x)] true)
                         char>boolean
                           (~(tag "boolean" 'invoke) [~'_5__ ~(tag "char"             'x)] true)
                         int>boolean
                           (~(tag "boolean" 'invoke) [~'_6__ ~(tag "int"              'x)] true)
                         long>boolean
                           (~(tag "boolean" 'invoke) [~'_7__ ~(tag "long"             'x)] true)
                         float>boolean
                           (~(tag "boolean" 'invoke) [~'_8__ ~(tag "float"            'x)] true)
                         double>boolean
                           (~(tag "boolean" 'invoke) [~'_9__ ~(tag "double"           'x)] true)))

                     (defn ~'some?|test
                       {::t/type (t/fn ~'[t/nil?]
                                       ~'[t/any?])}
                       ([~'x00__]
                         (ifs ((Array/get ~'some?|test|__0|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>boolean"
                                               'some?|test|__0) ~'x00__)
                              ;; TODO eliminate this check because it's not needed (`t/any?`)
                              ((Array/get ~'some?|test|__1|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>boolean"
                                               'some?|test|__1) ~'x00__)
                              (unsupported! `some?|test [~'x00__] 0))))))
            :cljs
              ($ (do (defn ~'some?|test [~'x]
                       (ifs (nil? x) false
                            true)))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (throws (some?|test))
                 (is= (some?|test 123) true)
                 (is= (some?|test true) true)
                 (is= (some?|test false) true)
                 (is= (some?|test nil) false))))))

(deftest test|reduced?
  (let [actual
          ;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
          (macroexpand '
            (defnt #_:inline reduced?|test
              ([x (t/isa? Reduced)] true)
              ;; Implicitly, `(- t/any? (t/isa? Reduced))`
              ([x t/any?          ] false)))
        expected
          (case (env-lang)
            :clj
              ($ (do ;; [x (t/isa? Reduced)]

                     (def ~(tag "[Ljava.lang.Object;" 'reduced?|test|__0|input-types)
                       (*<> ~'(t/isa? Reduced)))
                     (def ~'reduced?|test|__0
                       (reify
                         Object>boolean
                           (~(tag "boolean" 'invoke) [~'_0__ ~(tag "java.lang.Object" 'x)]
                             (let* [~(tag "clojure.lang.Reduced" 'x) ~'x] true))))

                     ;; [x t/any?]

                     (def ~(tag "[Ljava.lang.Object;" 'reduced?|test|__1|input-types)
                       (*<> ~'t/any?))
                     (def ~'reduced?|test|__1
                       (reify
                         Object>boolean
                           (~(tag "boolean" 'invoke) [~'_1__ ~(tag "java.lang.Object" 'x)] false)
                         boolean>boolean
                           (~(tag "boolean" 'invoke) [~'_2__ ~(tag "boolean"          'x)] false)
                         byte>boolean
                           (~(tag "boolean" 'invoke) [~'_3__ ~(tag "byte"             'x)] false)
                         short>boolean
                           (~(tag "boolean" 'invoke) [~'_4__ ~(tag "short"            'x)] false)
                         char>boolean
                           (~(tag "boolean" 'invoke) [~'_5__ ~(tag "char"             'x)] false)
                         int>boolean
                           (~(tag "boolean" 'invoke) [~'_6__ ~(tag "int"              'x)] false)
                         long>boolean
                           (~(tag "boolean" 'invoke) [~'_7__ ~(tag "long"             'x)] false)
                         float>boolean
                           (~(tag "boolean" 'invoke) [~'_8__ ~(tag "float"            'x)] false)
                         double>boolean
                           (~(tag "boolean" 'invoke) [~'_9__ ~(tag "double"           'x)] false)))

                     (defn ~'reduced?|test
                       {::t/type (t/fn ~'[(t/isa? Reduced)]
                                       ~'[t/any?])}
                       ([~'x00__]
                         (ifs ((Array/get ~'reduced?|test|__0|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>boolean"
                                               'reduced?|test|__0) ~'x00__)
                              ;; TODO eliminate this check because it's not needed (`t/any?`)
                              ((Array/get ~'reduced?|test|__1|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>boolean"
                                               'reduced?|test|__1) ~'x00__)
                              (unsupported! `reduced?|test [~'x00__] 0))))))
            :cljs
              ($ (do (defn ~'reduced?|test [~'x]
                       (ifs (instance? Reduced x) true false)))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (throws (reduced?|test))
                 (is= (reduced?|test 123) false)
                 (is= (reduced?|test true) false)
                 (is= (reduced?|test false) false)
                 (is= (reduced?|test nil) false)
                 (is= (reduced?|test (reduced 123)) true)
                 (is= (reduced?|test (reduced true)) true)
                 (is= (reduced?|test (reduced false)) true)
                 (is= (reduced?|test (reduced nil)) true)))))))

(deftest test|>boolean
  (let [actual
          (macroexpand '
            (defnt #_:inline >boolean
               ([x t/boolean?] x)
               ([x t/nil?]     false)
               ([x t/any?]     true)))
        expected
          (case (env-lang)
            :clj
              ($ (do ;; [x t/boolean?]

                     (def ~(tag "[Ljava.lang.Object;" '>boolean|__0|input-types)
                       (*<> ~'t/boolean?))
                     (def ~'>boolean|__0
                       (reify
                         boolean>boolean
                           (~(tag "boolean" 'invoke) [~'_0__  ~(tag "boolean"          'x)] ~'x)))

                     ;; [x t/nil? -> (- t/nil? t/boolean?)]

                     (def ~(tag "[Ljava.lang.Object;" '>boolean|__1|input-types)
                       (*<> ~'t/nil?))
                     (def ~'>boolean|__1
                       (reify
                         Object>boolean
                           (~(tag "boolean" 'invoke) [~'_1__  ~(tag "java.lang.Object" 'x)] false)))

                     ;; [x t/any? -> (- t/any? t/nil? t/boolean?)]

                     (def ~(tag "[Ljava.lang.Object;" '>boolean|__2|input-types)
                       (*<> ~'t/any?))
                     (def ~'>boolean|__2
                       (reify
                         Object>boolean
                           (~(tag "boolean" 'invoke) [~'_2__  ~(tag "java.lang.Object" 'x)] true)
                         boolean>boolean
                           (~(tag "boolean" 'invoke) [~'_3__  ~(tag "boolean"          'x)] true)
                         byte>boolean
                           (~(tag "boolean" 'invoke) [~'_4__  ~(tag "byte"             'x)] true)
                         short>boolean
                           (~(tag "boolean" 'invoke) [~'_5__  ~(tag "short"            'x)] true)
                         char>boolean
                           (~(tag "boolean" 'invoke) [~'_6__  ~(tag "char"             'x)] true)
                         int>boolean
                           (~(tag "boolean" 'invoke) [~'_7__  ~(tag "int"              'x)] true)
                         long>boolean
                           (~(tag "boolean" 'invoke) [~'_8__  ~(tag "long"             'x)] true)
                         float>boolean
                           (~(tag "boolean" 'invoke) [~'_9__  ~(tag "float"            'x)] true)
                         double>boolean
                           (~(tag "boolean" 'invoke) [~'_10__ ~(tag "double"           'x)] true)))

                     (defn ~'>boolean
                       {::t/type (t/fn ~'[t/boolean?]
                                       ~'[t/nil?]
                                       ~'[t/any?])}
                       ([~'x00__]
                         (ifs ((Array/get ~'>boolean|__0|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.boolean>boolean"
                                               '>boolean|__0) ~'x00__)
                              ((Array/get ~'>boolean|__1|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>boolean"
                                               '>boolean|__1) ~'x00__)
                              ;; TODO eliminate this check because it's not needed (`t/any?`)
                              ((Array/get ~'>boolean|__2|input-types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>boolean"
                                               '>boolean|__2) ~'x00__)
                              (unsupported! `>boolean [~'x00__] 0))))))
            :cljs
              ($ (do (defn ~'>boolean [~'x]
                       (ifs (boolean? x) x
                            (nil?     x) false
                            true)))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (throws (>boolean))
                 (is= (>boolean true) true)
                 (is= (>boolean false) false)
                 (is= (>boolean nil) false)
                 (is= (>boolean 123) true))))))

;; =====|=====|=====|=====|===== ;;

(is-code=

;; auto-upcasts to long or double (because 64-bit) unless you tell it otherwise
;; will error if not all return values can be safely converted to the return spec
(macroexpand '
(defnt #_:inline >int* > t/int?
  ([x (t/- t/primitive? t/boolean?)] (Primitive/uncheckedIntCast x))
  ([x (t/ref (t/isa? Number))] (.intValue x)))
)

;; ----- expanded code ----- ;;

(case (env-lang)
  :clj ($ (do #_(swap! fn->spec assoc #'>int*
                  (t/fn [(t/- t/primitive? t/boolean?)]
                        [(t/ref (t/isa? Number))]))

              ;; [x (t/- t/primitive? t/boolean?)]

              (def ~(tag "[Ljava.lang.Object;" '>int*|__0|input-types)
                (*<> (t/- t/primitive? t/boolean?)))
              (def ~'>int*|__0
                (reify
                  byte>int   (~(tag "int" 'invoke) [~'_0__ ~(tag "byte"             'x)]
                               ~'(Primitive/uncheckedIntCast x))
                  short>int  (~(tag "int" 'invoke) [~'_1__ ~(tag "short"            'x)]
                               ~'(Primitive/uncheckedIntCast x))
                  char>int   (~(tag "int" 'invoke) [~'_2__ ~(tag "char"             'x)]
                               ~'(Primitive/uncheckedIntCast x))
                  int>int    (~(tag "int" 'invoke) [~'_3__ ~(tag "int"              'x)]
                               ~'(Primitive/uncheckedIntCast x))
                  long>int   (~(tag "int" 'invoke) [~'_4__ ~(tag "long"             'x)]
                               ~'(Primitive/uncheckedIntCast x))
                  float>int  (~(tag "int" 'invoke) [~'_5__ ~(tag "float"            'x)]
                               ~'(Primitive/uncheckedIntCast x))
                  double>int (~(tag "int" 'invoke) [~'_6__ ~(tag "double"           'x)]
                               ~'(Primitive/uncheckedIntCast x))))

              ;; [x (t/ref (t/isa? Number))
              ;;  -> (t/- (t/ref (t/isa? Number)) (t/- t/primitive? t/boolean?))]

              (def ~'>int*|__1
                (reify
                  Object>int (~(tag "int" 'invoke) [~'_7__ ~(tag "java.lang.Object" 'x)]
                               (let* [~(tag "java.lang.Number" 'x) ~'x] ~'(.intValue x))))))))

)

;; =====|=====|=====|=====|===== ;;

(is-code=

(macroexpand '
(defnt #_:inline >
           ;; This is admittedly a place where inference might be nice, but luckily there are no
           ;; "sparse" combinations
  #?(:clj  ([a t/comparable-primitive? b t/comparable-primitive? > t/boolean?]
             (Numeric/gt a b))
     :cljs ([a t/double?               b t/double?               > (t/assume t/boolean?)]
             (cljs.core/> a b))))
)

;; ----- expanded code ----- ;;

(case (env-lang)
  :clj  ($ (do (def ~'>|__0
                 (reify
                   byte+byte>boolean
                     (~(tag "boolean" 'invoke) [~'_0__  ~(tag "byte"   'a) ~(tag "byte"   'b)]
                       ~'(Numeric/gt a b))
                   byte+short>boolean
                     (~(tag "boolean" 'invoke) [~'_1__  ~(tag "byte"   'a) ~(tag "short"  'b)]
                       ~'(Numeric/gt a b))
                   byte+char>boolean
                     (~(tag "boolean" 'invoke) [~'_2__  ~(tag "byte"   'a) ~(tag "char"   'b)]
                       ~'(Numeric/gt a b))
                   byte+int>boolean
                     (~(tag "boolean" 'invoke) [~'_3__  ~(tag "byte"   'a) ~(tag "int"    'b)]
                       ~'(Numeric/gt a b))
                   byte+long>boolean
                     (~(tag "boolean" 'invoke) [~'_4__  ~(tag "byte"   'a) ~(tag "long"   'b)]
                       ~'(Numeric/gt a b))
                   byte+float>boolean
                     (~(tag "boolean" 'invoke) [~'_5__  ~(tag "byte"   'a) ~(tag "float"  'b)]
                       ~'(Numeric/gt a b))
                   byte+double>boolean
                     (~(tag "boolean" 'invoke) [~'_6__  ~(tag "byte"   'a) ~(tag "double" 'b)]
                       ~'(Numeric/gt a b))
                   short+byte>boolean
                     (~(tag "boolean" 'invoke) [~'_7__  ~(tag "short"  'a) ~(tag "byte"   'b)]
                       ~'(Numeric/gt a b))
                   short+short>boolean
                     (~(tag "boolean" 'invoke) [~'_8__  ~(tag "short"  'a) ~(tag "short"  'b)]
                       ~'(Numeric/gt a b))
                   short+char>boolean
                     (~(tag "boolean" 'invoke) [~'_9__  ~(tag "short"  'a) ~(tag "char"   'b)]
                       ~'(Numeric/gt a b))
                   short+int>boolean
                     (~(tag "boolean" 'invoke) [~'_10__ ~(tag "short"  'a) ~(tag "int"    'b)]
                       ~'(Numeric/gt a b))
                   short+long>boolean
                     (~(tag "boolean" 'invoke) [~'_11__ ~(tag "short"  'a) ~(tag "long"   'b)]
                       ~'(Numeric/gt a b))
                   short+float>boolean
                     (~(tag "boolean" 'invoke) [~'_12__ ~(tag "short"  'a) ~(tag "float"  'b)]
                       ~'(Numeric/gt a b))
                   short+double>boolean
                     (~(tag "boolean" 'invoke) [~'_13__ ~(tag "short"  'a) ~(tag "double" 'b)]
                       ~'(Numeric/gt a b))
                   char+byte>boolean
                     (~(tag "boolean" 'invoke) [~'_14__ ~(tag "char"   'a) ~(tag "byte"   'b)]
                       ~'(Numeric/gt a b))
                   char+short>boolean
                     (~(tag "boolean" 'invoke) [~'_15__ ~(tag "char"   'a) ~(tag "short"  'b)]
                       ~'(Numeric/gt a b))
                   char+char>boolean
                     (~(tag "boolean" 'invoke) [~'_16__ ~(tag "char"   'a) ~(tag "char"   'b)]
                       ~'(Numeric/gt a b))
                   char+int>boolean
                     (~(tag "boolean" 'invoke) [~'_17__ ~(tag "char"   'a) ~(tag "int"    'b)]
                       ~'(Numeric/gt a b))
                   char+long>boolean
                     (~(tag "boolean" 'invoke) [~'_18__ ~(tag "char"   'a) ~(tag "long"   'b)]
                       ~'(Numeric/gt a b))
                   char+float>boolean
                     (~(tag "boolean" 'invoke) [~'_19__ ~(tag "char"   'a) ~(tag "float"  'b)]
                       ~'(Numeric/gt a b))
                   char+double>boolean
                     (~(tag "boolean" 'invoke) [~'_20__ ~(tag "char"   'a) ~(tag "double" 'b)]
                       ~'(Numeric/gt a b))
                   int+byte>boolean
                     (~(tag "boolean" 'invoke) [~'_21__ ~(tag "int"    'a) ~(tag "byte"   'b)]
                       ~'(Numeric/gt a b))
                   int+short>boolean
                     (~(tag "boolean" 'invoke) [~'_22__ ~(tag "int"    'a) ~(tag "short"  'b)]
                       ~'(Numeric/gt a b))
                   int+char>boolean
                     (~(tag "boolean" 'invoke) [~'_23__ ~(tag "int"    'a) ~(tag "char"   'b)]
                       ~'(Numeric/gt a b))
                   int+int>boolean
                     (~(tag "boolean" 'invoke) [~'_24__ ~(tag "int"    'a) ~(tag "int"    'b)]
                       ~'(Numeric/gt a b))
                   int+long>boolean
                     (~(tag "boolean" 'invoke) [~'_25__ ~(tag "int"    'a) ~(tag "long"   'b)]
                       ~'(Numeric/gt a b))
                   int+float>boolean
                     (~(tag "boolean" 'invoke) [~'_26__ ~(tag "int"    'a) ~(tag "float"  'b)]
                       ~'(Numeric/gt a b))
                   int+double>boolean
                     (~(tag "boolean" 'invoke) [~'_27__ ~(tag "int"    'a) ~(tag "double" 'b)]
                       ~'(Numeric/gt a b))
                   long+byte>boolean
                     (~(tag "boolean" 'invoke) [~'_28__ ~(tag "long"   'a) ~(tag "byte"   'b)]
                       ~'(Numeric/gt a b))
                   long+short>boolean
                     (~(tag "boolean" 'invoke) [~'_29__ ~(tag "long"   'a) ~(tag "short"  'b)]
                       ~'(Numeric/gt a b))
                   long+char>boolean
                     (~(tag "boolean" 'invoke) [~'_30__ ~(tag "long"   'a) ~(tag "char"   'b)]
                       ~'(Numeric/gt a b))
                   long+int>boolean
                     (~(tag "boolean" 'invoke) [~'_31__ ~(tag "long"   'a) ~(tag "int"    'b)]
                       ~'(Numeric/gt a b))
                   long+long>boolean
                     (~(tag "boolean" 'invoke) [~'_32__ ~(tag "long"   'a) ~(tag "long"   'b)]
                       ~'(Numeric/gt a b))
                   long+float>boolean
                     (~(tag "boolean" 'invoke) [~'_33__ ~(tag "long"   'a) ~(tag "float"  'b)]
                       ~'(Numeric/gt a b))
                   long+double>boolean
                     (~(tag "boolean" 'invoke) [~'_34__ ~(tag "long"   'a) ~(tag "double" 'b)]
                       ~'(Numeric/gt a b))
                   float+byte>boolean
                     (~(tag "boolean" 'invoke) [~'_35__ ~(tag "float"  'a) ~(tag "byte"   'b)]
                       ~'(Numeric/gt a b))
                   float+short>boolean
                     (~(tag "boolean" 'invoke) [~'_36__ ~(tag "float"  'a) ~(tag "short"  'b)]
                       ~'(Numeric/gt a b))
                   float+char>boolean
                     (~(tag "boolean" 'invoke) [~'_37__ ~(tag "float"  'a) ~(tag "char"   'b)]
                       ~'(Numeric/gt a b))
                   float+int>boolean
                     (~(tag "boolean" 'invoke) [~'_38__ ~(tag "float"  'a) ~(tag "int"    'b)]
                       ~'(Numeric/gt a b))
                   float+long>boolean
                     (~(tag "boolean" 'invoke) [~'_39__ ~(tag "float"  'a) ~(tag "long"   'b)]
                       ~'(Numeric/gt a b))
                   float+float>boolean
                     (~(tag "boolean" 'invoke) [~'_40__ ~(tag "float"  'a) ~(tag "float"  'b)]
                       ~'(Numeric/gt a b))
                   float+double>boolean
                     (~(tag "boolean" 'invoke) [~'_41__ ~(tag "float"  'a) ~(tag "double" 'b)]
                       ~'(Numeric/gt a b))
                   double+byte>boolean
                     (~(tag "boolean" 'invoke) [~'_42__ ~(tag "double" 'a) ~(tag "byte"   'b)]
                       ~'(Numeric/gt a b))
                   double+short>boolean
                     (~(tag "boolean" 'invoke) [~'_43__ ~(tag "double" 'a) ~(tag "short"  'b)]
                       ~'(Numeric/gt a b))
                   double+char>boolean
                     (~(tag "boolean" 'invoke) [~'_44__ ~(tag "double" 'a) ~(tag "char"   'b)]
                       ~'(Numeric/gt a b))
                   double+int>boolean
                     (~(tag "boolean" 'invoke) [~'_45__ ~(tag "double" 'a) ~(tag "int"    'b)]
                       ~'(Numeric/gt a b))
                   double+long>boolean
                     (~(tag "boolean" 'invoke) [~'_46__ ~(tag "double" 'a) ~(tag "long"   'b)]
                       ~'(Numeric/gt a b))
                   double+float>boolean
                     (~(tag "boolean" 'invoke) [~'_47__ ~(tag "double" 'a) ~(tag "float"  'b)]
                       ~'(Numeric/gt a b))
                   double+double>boolean
                     (~(tag "boolean" 'invoke) [~'_48__ ~(tag "double" 'a) ~(tag "double" 'b)]
                       ~'(Numeric/gt a b))))

                 #_(defn >
                     {::t/type
                       (t/fn #?(:clj  [t/comparable-primitive? t/comparable-primitive?
                                       :> t/boolean?]
                                :cljs [t/double?               t/double?
                                       :> (t/assume t/boolean?)]))}
                     ([a0 a1]
                             (ifs (t/byte? a0)
                                    (ifs (t/byte? a1) (.invoke ^byte+byte>boolean >|__0 a0 a1)
                                         (t/char? a1) (.invoke ...)
                                         ...)
                                  (t/char? a0)
                                    (ifs (t/byte? a1) (.invoke ^char+byte>boolean >|__0 a0 a1)
                                         ...)
                                  ...
                                  (unsupported! `> [a0 a1] 0))))))
        :cljs `(do (defn >
                     ([a0 a1]
                       (ifs (double? a0)
                              (ifs (double? a1)
                                     (let* [a a0 b a1] (cljs.core/> a b))
                                   (unsupported! `> [a0 a1] 1))
                            (unsupported! `> [a0 a1] 0)))))))

;; =====|=====|=====|=====|===== ;;

;; TODO fix: current implementation prefers to consolidate into one `reify` rather than splitting it
;; up as below
(is-code=

(macroexpand '
(defnt #_:inline >long*
  {:source "clojure.lang.RT.uncheckedLongCast"}
  > t/long?
  ([x (t/- t/primitive? t/boolean?)] (Primitive/uncheckedLongCast x))
  ([x (t/ref (t/isa? Number))] (.longValue x))))

;; ----- expanded code ----- ;;

(case (env-lang)
  :clj ($ (do ;; [x (t/- t/primitive? t/boolean?)]

              #_(def ~'>long*|__0|input-types (*<> t/byte?))
              (def ~'>long*|__0
                (reify byte>long   (~(tag "long" 'invoke) [~'_0__ ~(tag "byte"             'x)]
                  ~'(Primitive/uncheckedLongCast x))))

              #_(def ~'>long*|__1|input-types (*<> t/char?))
              (def ~'>long*|__1
                (reify char>long   (~(tag "long" 'invoke) [~'_1__ ~(tag "char"             'x)]
                  ~'(Primitive/uncheckedLongCast x))))

              #_(def ~'>long*|__2|input-types (*<> t/short?))
              (def ~'>long*|__2
                (reify short>long  (~(tag "long" 'invoke) [~'_2__ ~(tag "short"            'x)]
                  ~'(Primitive/uncheckedLongCast x))))

              #_(def ~'>long*|__3|input-types (*<> t/int?))
              (def ~'>long*|__3
                (reify int>long    (~(tag "long" 'invoke) [~'_3__ ~(tag "int"              'x)]
                  ~'(Primitive/uncheckedLongCast x))))

              #_(def ~'>long*|__4|input-types (*<> t/long?))
              (def ~'>long*|__4
                (reify long>long   (~(tag "long" 'invoke) [~'_4__ ~(tag "long"             'x)]
                  ~'(Primitive/uncheckedLongCast x))))

              #_(def ~'>long*|__5|input-types (*<> t/float?))
              (def ~'>long*|__5
                (reify float>long  (~(tag "long" 'invoke) [~'_5__ ~(tag "float"            'x)]
                  ~'(Primitive/uncheckedLongCast x))))

              #_(def ~'>long*|__6|input-types (*<> t/double?))
              (def ~'>long*|__6
                (reify double>long (~(tag "long" 'invoke) [~'_6__ ~(tag "double"           'x)]
                  ~'(Primitive/uncheckedLongCast x))))

              ;; [x (t/ref (t/isa? Number))]

              #_(def ~'>long*|__7|input-types (*<> (t/isa? Number)))
              (def ~'>long*|__7
                (reify Object>long (~(tag "long" 'invoke) [~'_7__ ~(tag "java.lang.Object" 'x)]
                  (let* [~(tag "java.lang.Number" 'x) ~'x] ~'(.longValue x)))))

              #_(defn >long*
                {::t/type (t/fn [(t/- t/primitive? t/boolean?)]
                                [(t/ref (t/isa? Number))])}
                [a0##] (ifs ((Array/get >long*|__0|input-types 0) a0##)
                              (.invoke >long*|__0 a0##)
                            ...))

              )))

)

;; =====|=====|=====|=====|===== ;;

;; TODO requires `>long*` being defined for it to work
(is-code=

(macroexpand '
(defnt >long
  {:source "clojure.lang.RT.longCast"}
  > t/long?
  ([x (t/- t/primitive? t/boolean? t/float? t/double?)] (>long* x))
  ([x (t/and (t/or t/double? t/float?)
             (fnt [x (t/or double? float?)] (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))]
    (>long* x))
  ([x (t/and (t/isa? clojure.lang.BigInt)
             (fnt [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))]
    (.lpart x))
  ([x (t/and (t/isa? java.math.BigInteger)
             (fnt [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))]
    (.longValue x))
  ([x t/ratio?] (>long (.bigIntegerValue x)))
  ([x (t/value true)]  1)
  ([x (t/value false)] 0)
  ([x t/string?] (Long/parseLong x))
  ([x t/string?, radix t/int?] (Long/parseLong x radix))))

;; ----- expanded code ----- ;;

(case (env-lang)
  :clj ($ (do #_[x (t/- t/primitive? t/boolean? t/float? t/double?)]

              #_(def ~'>long|__0|input-types (*<> t/byte?))
              (def ~'>long|__0
                (reify byte>long
                  (~(tag "long" 'invoke) [_## ~(tag "byte" 'x)]
                    ;; Resolved from `(>long* x)`
                    (.invoke >long*|__0 ~'x))))

              #_(def ~'>long|__1|input-types (*<> t/char?))
              (def ~'>long|__1
                (reify char>long
                  (~(tag "long" 'invoke) [_## ~(tag "char" 'x)]
                    ;; Resolved from `(>long* x)`
                    (.invoke >long*|__1 ~'x))))

              #_(def ~'>long|__2|input-types (*<> t/short?))
              (def ~'>long|__2
                (reify short>long
                  (~(tag "long" 'invoke) [_## ~(tag "short" 'x)]
                    ;; Resolved from `(>long* x)`
                    (.invoke >long*|__2 ~'x))))

              #_(def ~'>long|__3|input-types (*<> t/int?))
              (def ~'>long|__3
                (reify int>long
                  (~(tag "long" 'invoke) [_## ~(tag "int" 'x)]
                    ;; Resolved from `(>long* x)`
                    (.invoke >long*|__3 ~'x))))

              #_(def ~'>long|__4|input-types (*<> t/long?))
              (def ~'>long|__4
                (reify long>long
                  (~(tag "long" 'invoke) [_## ~(tag "long" 'x)]
                    ;; Resolved from `(>long* x)`
                    (.invoke >long*|__4 ~'x))))

              #_[x (t/and (t/or t/double? t/float?)
                          (fnt [x (t/or double? float?)]
                            (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))]

              #_(def ~'>long|__5|input-types
                (*<> (t/and t/double?
                            (fnt [x (t/or double? float?)]
                              (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))))
              (def ~'>long|__5
                (reify double>long
                  (~(tag "long" 'invoke) [_## ~(tag "double" 'x)]
                    ;; Resolved from `(>long* x)`
                    (.invoke >long*|__6 ~'x))))

              #_(def ~'>long|__6|input-types
                (*<> (t/and t/float?
                            (fnt [x (t/or double? float?)]
                              (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))))
              (def ~'>long|__6
                (reify float>long
                  (~(tag "long" 'invoke) [_## ~(tag "float" 'x)]
                    ;; Resolved from `(>long* x)`
                    (.invoke >long*|__5 ~'x))))

              #_[(t/and (t/isa? clojure.lang.BigInt)
                        (fnt [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))]

              #_(def ~'>long|__7|input-types
                (*<> (t/and (t/isa? clojure.lang.BigInt)
                            (fnt [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))))
              (def ~'>long|__7
                (reify Object>long
                  (~(tag "long" 'invoke) [_## ~(tag "java.lang.Object" 'x)]
                    (let* [~(tag "clojure.lang.BigInt" 'x) ~'x] ~'(.lpart x)))))

              #_[x (t/and (t/isa? java.math.BigInteger)
                          (fnt [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))]

              #_(def ~'>long|__8|input-types
                (*<> (t/and (t/isa? java.math.BigInteger)
                            (fnt [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))))
              (def ~'>long|__8
                (reify Object>long
                  (~(tag "long" 'invoke) [_## ~(tag "java.lang.Object" 'x)]
                    (let* [~(tag "java.math.BigInteger" 'x) ~'x] ~'(.longValue x)))))

              #_[x t/ratio?]

              #_(def ~'>long|__9|input-types
                (*<> t/ratio?))
              #_(def ~'>long|__9|conditions
                (*<> (-> long|__8|input-types (get 0) utr/and-type>args (get 1))))
              (def ~'>long|__9
                (reify Object>long
                  (~(tag "long" 'invoke) [_## ~(tag "java.lang.Object" 'x)]
                    (let* [~(tag "clojure.lang.Ratio" 'x) ~'x]
                      ;; Resolved from `(>long (.bigIntegerValue x))`
                      ;; In this case, `(t/compare (type-of '(.bigIntegerValue x)) overload-type)`:
                      ;; - `(t/- t/primitive? t/boolean? t/float? t/double?)` -> t/<>
                      ;; - `(t/and (t/or t/double? t/float?) ...)`            -> t/<>
                      ;; - `(t/and (t/isa? clojure.lang.BigInt) ...)`         -> t/<>
                      ;; - `(t/and (t/isa? java.math.BigInteger) ...)`        -> t/>
                      ;; - `t/ratio?`                                         -> t/<>
                      ;; - `(t/value true)`                                   -> t/<>
                      ;; - `(t/value false)`                                  -> t/<>
                      ;; - `t/string?`                                        -> t/<>
                      ;;
                      ;; Since there is no overload that results in t/<, no compile-time match can
                      ;; be found, but a possible runtime match lies in the overload that results in
                      ;; t/>. The remaining uncertainty will have to be resolved at compile time.
                      ;; Note that if there had been multiple overloads with t/>, we would have had
                      ;; to dispatch on that and resolve accordingly.
                      (let [x## ~'(.bigIntegerValue x)]
                        (if ((Array/get >long|__9|conditions 0) x##)
                            (.invoke >long|__8 x##)
                            (unsupported! `>long x##)))))))

              #_[x (t/value true)]

              #_(def ~'>long|__10|input-types
                (*<> (t/value true)))
              (def ~'>long|__10
                (reify boolean>long
                  (~(tag "long" 'invoke) [_## ~(tag "boolean" 'x)] 1)))

              #_[x (t/value false)]

              #_(def ~'>long|__11|input-types
                (*<> (t/value false)))
              (def ~'>long|__11
                (reify boolean>long
                  (~(tag "long" 'invoke) [_## ~(tag "boolean" 'x)] 0)))

              #_[x t/string?]

              #_(def ~'>long|__12|input-types
                (*<> t/string?))
              (def ~'>long|__12
                (reify Object>long
                  (~(tag "long" 'invoke) [_## ~(tag "java.lang.Object" 'x)]
                    ~'(Long/parseLong x))))

              #_[x t/string?]

              #_(def ~'>long|__13|input-types
                (*<> t/string? t/int?))
              (def ~'>long|__13
                (reify Object+int>long
                  (~(tag "long" 'invoke) [_## ~(tag "java.lang.Object" 'x) ~(tag "int" 'radix)]
                    ~'(Long/parseLong x radix))))

              #_(defn >long
                {::t/type
                  (t/fn
                    [(t/- t/primitive? t/boolean? t/float? t/double?)]
                    [(t/and (t/or t/double? t/float?)
                            (fnt [x (t/or double? float?)]
                              (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))]
                    [(t/and (t/isa? clojure.lang.BigInt)
                            (fnt [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))]
                    [(t/and (t/isa? java.math.BigInteger)
                            (fnt [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))]
                    [t/ratio?]
                    [(t/value true)]
                    [(t/value false)]
                    [t/string?]
                    [t/string? t/int?])}
                ([x0##] (ifs ((Array/get >long|__0|input-types 0) x0##)
                               (.invoke >long|__0 x0##)
                             ((Array/get >long|__1|input-types 0) x0##)
                               (.invoke >long|__0 x0##)
                             ((Array/get >long|__2|input-types 0) x0##)
                               (.invoke >long|__2 x0##)))
                ([x0## x1##] ...)))))

)

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt !str > #?(:clj  (t/isa? StringBuilder)
                 :cljs (t/isa? StringBuffer))
        ([] #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
        ;; If we had combined this arity, `t/or`ing the `t/string?` means it wouldn't have been
        ;; handled any differently than `t/char-seq?`
#?(:clj ([x t/string?] (StringBuilder. x)))
        ([x #?(:clj  (t/or t/char-seq? t/int?)
               :cljs t/val?)]
          #?(:clj (StringBuilder. x) :cljs (StringBuffer. x))))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'!str
       (t/fn :> #?(:clj  (t/isa? StringBuilder)
                   :cljs (t/isa? StringBuffer))
         []
 #?(:clj [t/string?])
         [#?(:clj  (t/or t/char-seq? t/int?)
             :cljs t/val?)]))

     ~(case-env
        :clj  `(do (def ^>Object !str|__0
                     (reify >Object
                       (^java.lang.Object invoke [_#]
                         (StringBuilder.))))
                   ;; `t/string?`
                   (def ^Object>Object !str|__1 ; `t/string?`
                     (reify Object>Object
                       (^java.lang.Object invoke [_# ^java.lang.Object ~'x]
                         (let* [^String x x] (StringBuilder. x)))))
                   ;; `(t/or t/char-seq? t/int?)`
                   (def ^Object>Object !str|__2 ; `t/char-seq?`
                     (reify Object>Object
                       (^java.lang.Object invoke [_# ^java.lang.Object ~'x]
                         (let* [^CharSequence x x] (StringBuilder. x)))))
                   (def ^int>Object !str|__3 ; `t/int?`
                     (reify int>Object (^java.lang.Object invoke [_# ^int ~'x]
                       (StringBuilder. x))))

                   (defn !str ([  ] (.invoke !str|__0))
                              ([a0] (ifs (t/string? a0)   (.invoke !str|__1 a0)
                                         (t/char-seq? a0) (.invoke !str|__2 a0)
                                         (t/int? a0)      (.invoke !str|__3 a0)))))
        :cljs `(do (defn !str ([]   (StringBuffer.))
                              ([a0] (let* [x a0] (StringBuffer. x)))))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline str > t/string?
           ([] "")
           ([x t/nil?] "")
           ;; could have inferred but there may be other objects who have overridden .toString
  #?(:clj  ([x (t/isa? Object)] (.toString x))
           ;; Can't infer that it returns a string (without a pre-constructed list of built-in fns)
           ;; As such, must explicitly mark
     :cljs ([x t/any? > (t/assume t/string?)] (.join #js [x] "")))
           ;; TODO only one variadic arity allowed currently; theoretically could dispatch on at
           ;; least pre-variadic args, if not variadic
           ;; TODO should have automatic currying?
           ([x (t/fn> str t/any?) & xs (? (t/seq-of t/any?)) #?@(:cljs [> (t/assume t/string?)])]
             (let* [sb (-> x str !str)] ; determined to be StringBuilder
               ;; TODO is `doseq` the right approach, or using reduction?
               (doseq [x' xs] (.append sb (str x')))
               (.toString sb))))
)

;; ----- expanded code ----- ;;

`(do ~(case-env
        :clj  `(do (def str|__0
                     (reify >Object       (^java.lang.Object invoke [_#                      ] "")))
                   (def str|__1 ; `nil?`
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] "")))
                   (def str|__2 ; `Object`
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (.toString x))))

                   (defn str
                     {::t/type
                       (t/fn :> t/string?
                         []
                         [t/nil?]
                #?(:clj  [(t/isa? Object)])
                #?(:cljs [t/any? :> (t/assume t/string?)])
                         [(t/fn> str t/any?) :& (? (t/seq-of t/any?)) #?@(:cljs [:> (t/assume t/string?)])])}
                     ([  ] (.invoke !str|__0))
                     ([a0] (ifs (nil? x) (.invoke !str|__1)
                                (.invoke !str|__2 a0)))
                     ([x & xs]
                       (let* [sb (!str (str x))]
                         (doseq [x' xs] (.append sb (str x'))) ; TODO is `doseq` the right approach?
                         (.toString sb)))))
        :cljs `(do (defn str
                     ([  ] "")
                     ([a0] (ifs (nil? x) ""
                                (.join #js [x] "")))
                     ([x & xs]
                       (let* [sb (!str (str x))]
                         (doseq [x' xs] (.append sb (str x'))) ; TODO is `doseq` the right approach?
                         (.toString sb)))))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline count > t/nneg-integer?
  ([xs t/array?  > t/nneg-int?] (.-length xs))
  ([xs t/string? > #?(:clj t/nneg-int? :cljs (t/assume t/nneg-int?))]
    (#?(:clj .length :cljs .-length) xs))
  ([xs !+vector? > t/nneg-int?] (#?(:clj count :cljs (do (TODO) 0)) xs)))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'count
       (t/fn :> t/pos-integer?
         [t/array?  :> t/nneg-int?]
         [t/string? :> #?(:clj t/nneg-int? :cljs (t/assume t/nneg-int?))]
         [!+vector? :> t/nneg-int?]))

     ~(case-env
        :clj  `(do ;; `array?`
                   (def count|__0__1 (reify Object>int (^int invoke [_# ^java.lang.Object ~'xs] (Array/count ^"[B" xs))))
                   ...)
        :cljs `(do ...)))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline get
  ([xs t/array? , k (t/numerically t/int?)] (#?(:clj Array/get :cljs aget) xs k))
  ([xs t/string?, k (t/numerically t/int?)] (.charAt xs k))
  ([xs !+vector?, k t/any?] #?(:clj (.valAt xs k) :cljs (TODO))))
)
;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'count
       (t/fn :> t/pos-integer?
         [t/array?  (t/numerically t/int?)]
         [t/string? (t/numerically t/int?)]
         [!+vector? t/any?]))

     ...)

;; =====|=====|=====|=====|===== ;;

; TODO CLJS version will come after
#?(:clj
(macroexpand '
(defnt seq
  "Taken from `clojure.lang.RT/seq`"
  > (t/? (t/isa? ISeq))
  ([xs t/nil?                 ] nil)
  ([xs (t/isa? ASeq)          ] xs)
  ([xs (t/or (t/isa? LazySeq)
             (t/isa? Seqable))] (.seq xs))
  ([xs t/iterable?            ] (clojure.lang.RT/chunkIteratorSeq (.iterator xs)))
  ([xs t/char-seq?            ] (StringSeq/create xs))
  ([xs (t/isa? Map)           ] (seq (.entrySet xs)))
  ([xs t/array?               ] (ArraySeq/createFromObject xs))))
)

;; ----- expanded code ----- ;;

#?(:clj
`(do ~(case-env
        :clj
          `(do ;; [t/nil?]

               (def seq|__0|input-types (*<> t/nil?))
               (def ^Object>Object seq|__0
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     ;; Notice, no casting for nil input
                     nil)))

               ;; [(t/isa? ASeq)]

               (def seq|__2|input-types (*<> (t/isa? ASeq)))
               (def ^Object>Object seq|__2
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^ASeq xs xs] xs))))

               ;; [(t/or (t/isa? LazySeq) (t/isa? Seqable))]

               (def seq|__3|input-types (*<> (t/isa? LazySeq)))
               (def ^Object>Object seq|__3
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^LazySeq xs xs] (.seq xs)))))

               (def seq|__4|input-types (*<> (t/isa? Seqable)))
               (def ^Object>Object seq|__4
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^Seqable xs xs] (.seq xs)))))

               ;; [t/iterable?]

               (def seq|__5|input-types (*<> t/iterable?))
               (def ^Object>Object seq|__5
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^Iterable xs xs] (clojure.lang.RT/chunkIteratorSeq (.iterator xs))))))

               ;; [t/char-seq?]

               (def seq|__6|input-types (*<> t/iterable?))
               (def ^Object>Object seq|__6
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^CharSequence xs xs] (StringSeq/create xs)))))

               ;; [(t/isa? Map)]

               (def seq|__7|input-types (*<> (t/isa? Map)))
               (def ^Object>Object seq|__7
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     ;; This is after expansion; it's the first one that matches the overload
                     ;; If no overload is found it'll have to be dispatched at runtime (protocol or
                     ;; equivalent) and potentially a configurable warning can be emitted
                     (let [^Map xs xs] (.invoke seq|__4__0 (.entrySet xs))))))

               ;; [t/array?]

               ;; TODO perhaps at some point figure out that it doesn't need to create any more
               ;; overloads here than just one?
               (def seq|__8|input-types (*<> (t/isa? (Class/forName "[Z"))))
               (def ^Object>Object seq|__8
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^"[Z" xs xs] (ArraySeq/createFromObject xs)))))

               (def seq|__9|input-types (*<> (t/isa? (Class/forName "[B"))))
               (def ^Object>Object seq|__9
                 (reify Object>Object
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^"[B" xs xs] (ArraySeq/createFromObject xs)))))
               ...

               (defn seq
                 "Taken from `clojure.lang.RT/seq`"
                 {::t/type
                   (t/fn > (t/? (t/isa? ISeq))
                     [t/nil?]
                     [(t/isa? ASeq)]
                     [(t/or (t/isa? LazySeq) (t/isa? Seqable))]
                     [t/iterable?]
                     [t/char-seq?]
                     [(t/isa? Map)]
                     [t/array?])}
                 [a0]
                 (ifs ((Array/get seq|__0|input-types  0) a0) (.invoke seq|__0  a0)
                      ((Array/get seq|__1|input-types  0) a0) (.invoke seq|__1  a0)
                      ...
                      ((Array/get seq|__30|input-types 0) a0) (.invoke seq|__30 a0)
                      ((Array/get seq|__31|input-types 0) a0) (.invoke seq|__31 a0)
                      ((Array/get seq|__32|input-types 0) a0) (.invoke seq|__32 a0)
                      ((Array/get seq|__33|input-types 0) a0) (.invoke seq|__33 a0)
                      ((Array/get seq|__34|input-types 0) a0) (.invoke seq|__34 a0)
                      ((Array/get seq|__35|input-types 0) a0) (.invoke seq|__35 a0)
                      (unsupported! `seq [a0] 0)))
               ))
        :cljs
          `(do ...))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt first
  ([xs t/nil?                          ] nil)
  ([xs (t/and t/sequential? t/indexed?)] (get xs 0))
  ([xs (t/isa? ISeq)                   ] (.first xs))
  ([xs ...                             ] (-> xs seq first))))

#?(:clj
`(do (swap! fn->spec assoc #'seq
       (t/fn
         [t/nil?]
         [(t/and t/sequential? t/indexed?)]
         [(t/isa? ISeq)]
         [...]))

     ~(case-env
        :clj  `(do ...)
        :cljs `(do ...))))

;; ----- expanded code ----- ;;

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt next > (? ISeq)
  "Taken from `clojure.lang.RT/next`"
  ([xs t/nil?]        nil)
  ([xs (t/isa? ISeq)] (.next xs))
  ([xs ...]           (-> xs seq next)))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'next
       (t/fn
         [t/nil?]
         [(t/isa? ISeq)]
         [...]))

     ...)

;; =====|=====|=====|=====|===== ;;

;; TODO: conditionally optional arities etc. for t/fn

(t/def rf? "Reducing function"
  (t/fn "seed arity"       []
        "completing arity" [_]
        "reducing arity"   [_ _]))

(defnt reduce
  "Much of this content taken from clojure.core.protocols for inlining and
   type-checking purposes."
  {:attribution "alexandergunnarson"}
         ([f rf?         xs t/nil?] (f))
         ([f rf?, init _ xs t/nil?] init)
         ([f rf?, init _, z (t/isa? fast_zip.core.ZipperLocation)]
           (loop [xs (zip/down z) v init]
             (if (some? z)
                 (let [ret (f v z)]
                   (if (reduced? ret)
                       @ret
                       (recur (zip/right xs) ret)))
                 v)))
         ;; TODO look at CLJS `array-reduce`
         ([f rf?, init _, xs (t/or t/array? t/string? t/!+vector?)] ; because transient vectors aren't reducible
           (let [ct (count xs)]
             (loop [i 0 v init]
               (if (< i ct)
                   (let [ret (f v (get xs i))]
                     (if (reduced? ret)
                         @ret
                         (recur (inc* i) ret)))
                   v))))
#?(:clj  ([f rf?, init _, xs (t/isa? clojure.lang.StringSeq)]
           (let [s (.s xs)]
             (loop [i (.i xs) v init]
               (if (< i (count s))
                   (let [ret (f v (get s i))]
                     (if (reduced? ret)
                         @ret
                         (recur (inc* i) ret)))
                   v)))))
#?(:clj  ([f  rf?
           xs (t/or (t/isa? clojure.lang.PersistentVector) ; vector's chunked seq is faster than its iter
                    (t/isa? clojure.lang.LazySeq) ; for range
                    (t/isa? clojure.lang.ASeq))] ; aseqs are iterable, masking internal-reducers
           (if-let [s (seq xs)]
             (clojure.core.protocols/internal-reduce (next s) f (first s))
             (f))))
#?(:clj  ([f rf?, init _
           xs (t/or (isa? clojure.lang.PersistentVector) ; vector's chunked seq is faster than its iter
                    (isa? clojure.lang.LazySeq) ; for range
                    (isa? clojure.lang.ASeq))]  ; aseqs are iterable, masking internal-reducers
           (let [s (seq xs)]
             (clojure.core.protocols/internal-reduce s f init))))
         ([x transformer?, f rf?]
           (let [rf ((.-xf x) f)]
             (rf (reduce rf (rf) (.-prev x)))))
         ([x transformer?, f rf?, init _]
           (let [rf ((.-xf x) f)]
             (rf (reduce rf init (.-prev x)))))
         ([f rf?, init _, x  t/chan?] (async/reduce f init x)) ; TODO spec `async/reduce`
#?(:cljs ([f rf?, init _, xs t/+map?] (#_(:clj  clojure.core.protocols/kv-reduce
                                      :cljs -kv-reduce) ; in order to use transducers...
                                -reduce-seq xs f init)))
#?(:cljs ([f rf?, init _, xs t/+set?] (-reduce-seq xs f init)))
         ([f rf?, init _, n (t/numerically t/int?)]
           (loop [i 0 v init]
             (if (< i n)
                 (let [ret (f v i)]
                   (if (reduced? ret)
                       @ret
                       (recur (inc* i) ret))) ; TODO should only be unchecked if `n` is within unchecked range
                 v)))
         ;; `iter-reduce`
#?(:clj  ([f  rf?
           xs (t/or (t/isa? clojure.lang.APersistentMap$KeySeq)
                    (t/isa? clojure.lang.APersistentMap$ValSeq)
                    t/iterable?)]
           (let [iter (.iterator xs)]
             (if (.hasNext iter)
                 (loop [ret (.next iter)]
                   (if (.hasNext iter)
                       (let [ret (f ret (.next iter))]
                         (if (reduced? ret)
                             @ret
                             (recur ret)))
                       ret))
                 (f)))))
         ;; `iter-reduce`
#?(:clj  ([f  rf?, init _
           xs (t/or (t/isa? clojure.lang.APersistentMap$KeySeq)
                    (t/isa? clojure.lang.APersistentMap$ValSeq)
                    t/iterable?)]
           (let [iter (.iterator xs)]
             (loop [ret init]
               (if (.hasNext iter)
                   (let [ret (f ret (.next iter))]
                     (if (reduced? ret)
                         @ret
                         (recur ret)))
                   ret)))))
#?(:clj  ([f rf?,         xs (t/isa? clojure.lang.IReduce)    ] (.reduce   xs f)))
#?(:clj  ([f rf?, init _, xs (t/isa? clojure.lang.IKVReduce)  ] (.kvreduce xs f init)))
#?(:clj  ([f rf?, init _, xs (t/isa? clojure.lang.IReduceInit)] (.reduce   xs f init)))
         ([f rf?, xs (t/isa? clojure.core.protocols/CollReduce)]
           (#?(:clj  clojure.core.protocols/coll-reduce
               :cljs -reduce) xs f))
         ([f rf?, init _, xs (t/isa? clojure.core.protocols/CollReduce)]
           (#?(:clj  clojure.core.protocols/coll-reduce
               :cljs -reduce) xs f init)))

;; ----- expanded code ----- ;;

;; =====|=====|=====|=====|===== ;;

(do (t/def xf? "Transforming function"
      (t/fn [rf? :> rf?]))

    (defnt transduce
      ([        f rf?,         xs t/reducible?] (transduce identity f     xs))
      ([xf xf?, f rf?,         xs t/reducible?] (transduce xf       f (f) xs))
      ([xf xf?, f rf?, init _, xs t/reducible?]
        (let [f' (xf f)] (f' (reduce f' init xs))))))

;; ----- expanded code ----- ;;


; ================================================ ;

(do

; (optional) function — only when the `defnt` has an arity with 0 arguments

; (optional) inline macros — invoked only if in a typed context and not used as a function
(do #?(:clj (defmacro clj:name:java:lang:String  [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro cljs:name:string [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro clj:name:clojure:lang:Named   [a0] `(let [~'x ~a0] ~'(-name x))))
    #?(:clj (defmacro cljs:name:cljs:core:INamed [a0] `(let [~'x ~a0] ~'(.getName x)))))
)

(extend-defnt abc/name ; for use outside of ns
  ([a ?, b ?] (...)))

;; This is necessarily dynamic dispatch
(name (read ))

(do
; (optional) function — only when the `defnt` has an arity with 0 arguments

; (optional) inline macros — invoked only if in a typed context and not used as a function
(do #?(:clj (defmacro clj:name:java:lang:String   [a0] `(let* [~'x ~a0] ~'x)))
    #?(:clj (defmacro cljs:name:string            [a0] `(let* [~'x ~a0] ~'x)))
    #?(:clj (defmacro clj:name:clojure:lang:Named [a0] `(let* [~'x ~a0] ~'(-name x))))
    #?(:clj (defmacro cljs:name:cljs:core:INamed  [a0] `(let* [~'x ~a0] ~'(.getName x)))))
)

; ================================================ ;

(defnt ^:inline custom
  [x (s/if double?
           (t/or (s/fnt [x ?] (> x 3)) ; uses the above-defined `>`
                 (s/fnt [x ?] (< x 0.1)))
           (t/or string? !string?))
   y ?] (str x (name y))) ; uses the above-defined `name`


;; ===== CLOJURESCRIPT ===== ;;

;; In order for specs to be enforceable at compile time, they must be able to be executed by the
;; compilation language. The case of one language compiled in a different one (e.g. ClojureScript
;; in Clojure/Java) is thus problematic.

;; For instance, this is only able to be checked in CLJS, because `js-object?` is not implemented
;; in CLJ:
(defnt abcde1
  [x #?(:clj string? :cljs js-object?)] ...)

;; This could be checked in CLJ, but it would be an error to do so:
(defn my-spec [x] #?(:clj (check-this) :cljs (check-that)))

(defnt abcde2
  [x my-spec] ...)

;; So what is the solution? One solution is to forgo some functionality in ClojureScript and
;; instead rely fundamentally on the aggregative relationships among predicates created using the
;; `defnt` spec system.

;; For instance:

(defnt abcde1 [x (t/pc :clj string? :cljs js-object?)] ...)

;; Or:

(t/def abcde1|x? :clj string? :cljs js-object?)

(defnt abcde1 [x abcde1|x?] ...)

;; Because the spec was registered using the `defnt` spec system, the quoted forms can be analyzed and
;; at least some things can be deduced.

;; In this case, the spec of `x` is deducible: `abcde1|x?` (`js-object?` deeper down). The return spec is also deducible as being the return spec of `abcde1`:

(defnt abcde2 [x ?] (abcde1 x))
