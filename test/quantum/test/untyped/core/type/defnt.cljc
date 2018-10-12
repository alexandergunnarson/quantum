(ns quantum.test.untyped.core.type.defnt
  (:refer-clojure :exclude
    [> count get name seq some? zero?])
  (:require
    [clojure.core                           :as core]
    [quantum.test.untyped.core.type         :as tt]
    [quantum.untyped.core.type.defnt        :as self
      :refer [fnt unsupported!]]
    [quantum.untyped.core.data.array
      :refer [*<>]]
    [quantum.untyped.core.form
      :refer [$ code=]]
    [quantum.untyped.core.form.evaluate
      :refer [case-env env-lang macroexpand-all]]
    [quantum.untyped.core.form.type-hint
      :refer [tag]]
    [quantum.untyped.core.logic
      :refer [ifs]]
    [quantum.untyped.core.spec              :as s]
    [quantum.untyped.core.test              :as utest
      :refer [deftest is is= is-code= testing throws]]
    [quantum.untyped.core.type              :as t
      :refer [?]]
    [quantum.untyped.core.type.reifications :as utr])
  (:import
    [clojure.lang ASeq ISeq LazySeq Named Reduced Seqable]
    [quantum.core.data Array]
    [quantum.core Numeric Primitive]))

;; Just in case
(clojure.spec.test.alpha/unstrument)
(do (require '[orchestra.spec.test :as st])
    (orchestra.spec.test/instrument))

(defn B   [form] (tag "boolean"             form))
(defn Y   [form] (tag "byte"                form))
(defn S   [form] (tag "short"               form))
(defn C   [form] (tag "char"                form))
(defn I   [form] (tag "int"                 form))
(defn L   [form] (tag "long"                form))
(defn F   [form] (tag "float"               form))
(defn D   [form] (tag "double"              form))
(defn O   [form] (tag "java.lang.Object"    form))
(defn O<> [form] (tag "[Ljava.lang.Object;" form))
(defn ST  [form] (tag "java.lang.String"    form))

(defn cstr [x]
  (if (-> x resolve class?)
      (str x)
      (str (namespace x) "." (name x))))

#?(:clj
(deftest test|pid
  (let [actual
          (macroexpand '
            (self/defn pid|test [> (? t/string?)]
              (->> ^:val (java.lang.management.ManagementFactory/getRuntimeMXBean)
                         (.getName))))
        expected
          ($ (do (declare ~'pid|test)
                 (def ~(O<> 'pid|test|__0|types) (quantum.untyped.core.data.array/*<>))
                 (def ~'pid|test|__0
                   (reify* [>Object]
                     (~(O 'invoke) [~'_0__]
                       ~(ST (list '.
                                 (tag "java.lang.management.RuntimeMXBean"
                                   '(. java.lang.management.ManagementFactory getRuntimeMXBean))
                                 'getName)))))
                 (defn ~'pid|test
                   {:quantum.core.type/type
                     (t/ftype t/any? [:> (t/or (t/value nil) (t/isa? String))])}
                   ([] (. ~(tag (cstr `>Object) 'pid|test|__0) ~'invoke)))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is (t/string? (pid|test)))
                 (throws (pid|test 1))))))))

;; TODO test `:inline`

(deftest test|identity|uninlined
  (let [actual
          (macroexpand '
            (self/defn identity|uninlined ([x t/any? > (t/type x)] x)))
        expected
          (case (env-lang)
            :clj
              ($ (do (declare ~'identity|uninlined)

                     ;; [x t/any?]

                     (def ~(O<> 'identity|uninlined|__0|types) (*<> (t/isa? Boolean)))
                     (def ~'identity|uninlined|__0
                       (reify* [boolean>boolean] (~(B 'invoke) [~'_0__ ~(B 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__1|types) (*<> (t/isa? Byte)))
                     (def ~'identity|uninlined|__1
                       (reify* [byte>byte]       (~(Y 'invoke) [~'_1__ ~(Y 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__2|types) (*<> (t/isa? Short)))
                     (def ~'identity|uninlined|__2
                       (reify* [short>short]     (~(S 'invoke) [~'_2__ ~(S 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__3|types) (*<> (t/isa? Character)))
                     (def ~'identity|uninlined|__3
                       (reify* [char>char]       (~(C 'invoke) [~'_3__ ~(C 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__4|types) (*<> (t/isa? Integer)))
                     (def ~'identity|uninlined|__4
                       (reify* [int>int]         (~(I 'invoke) [~'_4__ ~(I 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__5|types) (*<> (t/isa? Long)))
                     (def ~'identity|uninlined|__5
                       (reify* [long>long]       (~(L 'invoke) [~'_5__ ~(L 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__6|types) (*<> (t/isa? Float)))
                     (def ~'identity|uninlined|__6
                       (reify* [float>float]     (~(F 'invoke) [~'_6__ ~(F 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__7|types) (*<> (t/isa? Double)))
                     (def ~'identity|uninlined|__7
                       (reify* [double>double]   (~(D 'invoke) [~'_7__ ~(D 'x)] ~'x)))
                     (def ~(O<> 'identity|uninlined|__8|types) (*<> t/any?))
                     (def ~'identity|uninlined|__8
                       (reify* [Object>Object]   (~(O 'invoke) [~'_8__ ~(O 'x)] ~(O 'x))))

                     (defn ~'identity|uninlined
                       {:quantum.core.type/type
                         (t/ftype t/any? [(t/isa? Boolean)   :> (t/isa? Boolean)]
                                         [(t/isa? Byte)      :> (t/isa? Byte)]
                                         [(t/isa? Short)     :> (t/isa? Short)]
                                         [(t/isa? Character) :> (t/isa? Character)]
                                         [(t/isa? Integer)   :> (t/isa? Integer)]
                                         [(t/isa? Long)      :> (t/isa? Long)]
                                         [(t/isa? Float)     :> (t/isa? Float)]
                                         [(t/isa? Double)    :> (t/isa? Double)]
                                         [t/any?             :> t/any?])}
                ([~'x00__]
                  (ifs
                     ((Array/get ~'identity|uninlined|__0|types 0) ~'x00__)
                       (. ~(tag (cstr `boolean>boolean) 'identity|uninlined|__0) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__1|types 0) ~'x00__)
                       (. ~(tag (cstr `byte>byte)       'identity|uninlined|__1) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__2|types 0) ~'x00__)
                       (. ~(tag (cstr `short>short)     'identity|uninlined|__2) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__3|types 0) ~'x00__)
                       (. ~(tag (cstr `char>char)       'identity|uninlined|__3) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__4|types 0) ~'x00__)
                       (. ~(tag (cstr `int>int)         'identity|uninlined|__4) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__5|types 0) ~'x00__)
                       (. ~(tag (cstr `long>long)       'identity|uninlined|__5) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__6|types 0) ~'x00__)
                       (. ~(tag (cstr `float>float)     'identity|uninlined|__6) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__7|types 0) ~'x00__)
                       (. ~(tag (cstr `double>double)   'identity|uninlined|__7) ~'invoke ~'x00__)
                     ((Array/get ~'identity|uninlined|__8|types 0) ~'x00__)
                       (. ~(tag (cstr `Object>Object)   'identity|uninlined|__8) ~'invoke ~'x00__)
                       ;; TODO no need for `unsupported!` because it will always get a valid branch
                       (unsupported! `identity|uninlined [~'x00__] 0))))))
            :cljs
              ;; Direct dispatch will be simple functions, not `reify`s
              ($ (do (defn ~'identity|uninlined [~'x] ~'x))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is= (identity|uninlined 1)  (identity 1))
                 (is= (identity|uninlined "") (identity "")))))))

(deftest test|name
  (let [actual
          (macroexpand '
            (self/defn #_:inline name > t/string?
                       ([x t/string?] x)
              #?(:clj  ([x (t/isa? Named)  > (t/* t/string?)] (.getName x))
                 :cljs ([x (t/isa? INamed) > (t/* t/string?)] (-name x)))))
        expected
          (case (env-lang)
            :clj
              ($ (do (declare ~'name)

                     ;; [x t/string?]

                     (def ~(O<> 'name|__0|types) (*<> (t/isa? java.lang.String)))
                     (def ~'name|__0
                       (reify* [Object>Object]
                         (~(O 'invoke) [~'_0__ ~(O 'x)] ~(ST 'x))))

                     ;; [x (t/isa? Named)] > (t/* t/string?)

                     (def ~(O<> 'name|__1|types) (*<> (t/isa? Named)))
                     (def ~'name|__1
                       (reify* [Object>Object]
                         (~(O 'invoke) [~'_1__ ~(O 'x)]
                           (t/validate ~(ST (list '. (tag "clojure.lang.Named" 'x) 'getName))
                                       ~'(t/* t/string?)))))

                     (defn ~'name
                       {:quantum.core.type/type
                         (t/ftype (t/isa? String)
                                  [(t/isa? String) :> (t/isa? String)]
                                  [(t/isa? Named)  :> (t/* (t/isa? String))])}
                       ([~'x00__]
                         (ifs ((Array/get ~'name|__0|types 0) ~'x00__)
                                (. ~(tag (cstr `Object>Object) 'name|__0) ~'invoke ~'x00__)
                              ((Array/get ~'name|__1|types 0) ~'x00__)
                                (. ~(tag (cstr `Object>Object) 'name|__1) ~'invoke ~'x00__)
                              (unsupported! `name [~'x00__] 0))))))
            :cljs
              ($ (do (defn ~'name [~'x00__]
                     (ifs (t/string? x)         x
                          (satisfies? INamed x) (-name x)
                          (unsupported! `name [~'x00__] 0))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is= (name "")       (core/name ""))
                 (is= (name "abc")    (core/name "abc"))
                 (is= (name :abc)     (core/name :abc))
                 (is= (name 'abc)     (core/name 'abc))
                 (is= (name :abc/def) (core/name :abc/def))
                 (is= (name 'abc/def) (core/name 'abc/def))
                 (throws (name nil))
                 (throws (name 1)))))))

(deftest test|some?
  (let [actual
          ;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
          (macroexpand '
            (self/defn #_:inline some? > t/boolean?
              ([x t/nil?] false)
              ;; Implicitly, `(- t/any? t/nil?)`, so `t/val?`
              ([x t/any?] true)))
        expected
        (case (env-lang)
          :clj
          ($ (do (declare ~'some?)

                 ;; [x t/nil?]

                 (def ~(O<> 'some?|__0|types) (*<> (t/value nil)))
                 (def ~'some?|__0
                   (reify* [Object>boolean]  (~(B 'invoke) [~'_0__ ~(O 'x)] false)))

                 ;; [x t/any?]

                 (def ~(O<> 'some?|__1|types) (*<> (t/isa? Boolean)))
                 (def ~'some?|__1 (reify* [boolean>boolean] (~(B 'invoke) [~'_1__ ~(B 'x)] true)))
                 (def ~(O<> 'some?|__2|types) (*<> (t/isa? Byte)))
                 (def ~'some?|__2 (reify* [byte>boolean]    (~(B 'invoke) [~'_2__ ~(Y 'x)] true)))
                 (def ~(O<> 'some?|__3|types) (*<> (t/isa? Short)))
                 (def ~'some?|__3 (reify* [short>boolean]   (~(B 'invoke) [~'_3__ ~(S 'x)] true)))
                 (def ~(O<> 'some?|__4|types) (*<> (t/isa? Character)))
                 (def ~'some?|__4 (reify* [char>boolean]    (~(B 'invoke) [~'_4__ ~(C 'x)] true)))
                 (def ~(O<> 'some?|__5|types) (*<> (t/isa? Integer)))
                 (def ~'some?|__5 (reify* [int>boolean]     (~(B 'invoke) [~'_5__ ~(I 'x)] true)))
                 (def ~(O<> 'some?|__6|types) (*<> (t/isa? Long)))
                 (def ~'some?|__6 (reify* [long>boolean]    (~(B 'invoke) [~'_6__ ~(L 'x)] true)))
                 (def ~(O<> 'some?|__7|types) (*<> (t/isa? Float)))
                 (def ~'some?|__7 (reify* [float>boolean]   (~(B 'invoke) [~'_7__ ~(F 'x)] true)))
                 (def ~(O<> 'some?|__8|types) (*<> (t/isa? Double)))
                 (def ~'some?|__8 (reify* [double>boolean]  (~(B 'invoke) [~'_8__ ~(D 'x)] true)))
                 (def ~(O<> 'some?|__9|types) (*<> t/any?))
                 (def ~'some?|__9 (reify* [Object>boolean]  (~(B 'invoke) [~'_9__ ~(O 'x)] true)))

                 (defn ~'some?
                   {:quantum.core.type/type
                     (t/ftype (t/isa? Boolean)
                              [(t/value nil)      :> (t/isa? Boolean)]
                              [(t/isa? Boolean)   :> (t/isa? Boolean)]
                              [(t/isa? Byte)      :> (t/isa? Boolean)]
                              [(t/isa? Short)     :> (t/isa? Boolean)]
                              [(t/isa? Character) :> (t/isa? Boolean)]
                              [(t/isa? Integer)   :> (t/isa? Boolean)]
                              [(t/isa? Long)      :> (t/isa? Boolean)]
                              [(t/isa? Float)     :> (t/isa? Boolean)]
                              [(t/isa? Double)    :> (t/isa? Boolean)]
                              [t/any?             :> (t/isa? Boolean)])}
                   ([~'x00__]
                     (ifs ((Array/get ~'some?|__0|types 0) ~'x00__)
                            (. ~(tag (cstr `Object>boolean)  'some?|__0) ~'invoke ~'x00__)
                          ;; TODO eliminate these checks below because they're not needed
                          ((Array/get ~'some?|__1|types 0) ~'x00__)
                            (. ~(tag (cstr `boolean>boolean) 'some?|__1) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__2|types 0) ~'x00__)
                            (. ~(tag (cstr `byte>boolean)    'some?|__2) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__3|types 0) ~'x00__)
                            (. ~(tag (cstr `short>boolean)   'some?|__3) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__4|types 0) ~'x00__)
                            (. ~(tag (cstr `char>boolean)    'some?|__4) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__5|types 0) ~'x00__)
                            (. ~(tag (cstr `int>boolean)     'some?|__5) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__6|types 0) ~'x00__)
                            (. ~(tag (cstr `long>boolean)    'some?|__6) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__7|types 0) ~'x00__)
                            (. ~(tag (cstr `float>boolean)   'some?|__7) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__8|types 0) ~'x00__)
                            (. ~(tag (cstr `double>boolean)  'some?|__8) ~'invoke ~'x00__)
                          ((Array/get ~'some?|__9|types 0) ~'x00__)
                            (. ~(tag (cstr `Object>boolean)  'some?|__9) ~'invoke ~'x00__)
                          (unsupported! `some? [~'x00__] 0))))))
          :cljs
          ($ (do (defn ~'some?| [~'x]
                   (ifs (nil? x) false
                        true)))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (throws (some?))
                 (is= (some? 123)   (core/some? 123))
                 (is= (some? true)  (core/some? true))
                 (is= (some? false) (core/some? false))
                 (is= (some? nil)   (core/some? nil)))))))

(deftest test|reduced?
  (let [actual
          ;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
          (macroexpand '
            (self/defn #_:inline reduced?|test
              ([x (t/isa? Reduced)] true)
              ;; Implicitly, `(- t/any? (t/isa? Reduced))`
              ([x t/any?          ] false)))
        expected
          (case (env-lang)
            :clj
              ($ (do ;; [x (t/isa? Reduced)]

                     (def ~(O<> 'reduced?|test|__0|input0|types)
                       (*<> (t/isa? Reduced)))
                     (def ~'reduced?|test|__0|0
                       (reify* [Object>boolean]
                         (~(B 'invoke) [~'_0__ ~(O 'x)]
                           (let* [~(tag "clojure.lang.Reduced" 'x) ~'x] true))))

                     ;; [x t/any?]

                     (def ~(O<> 'reduced?|test|__1|input0|types)
                       (*<> t/any?))
                     (def ~'reduced?|test|__1|0
                       (reify* [Object>boolean boolean>boolean byte>boolean short>boolean
                                char>boolean int>boolean long>boolean float>boolean double>boolean]
                         (~(B 'invoke) [~'_1__ ~(O 'x)] false)
                         (~(B 'invoke) [~'_2__ ~(B 'x)] false)
                         (~(B 'invoke) [~'_3__ ~(Y 'x)] false)
                         (~(B 'invoke) [~'_4__ ~(S 'x)] false)
                         (~(B 'invoke) [~'_5__ ~(C 'x)] false)
                         (~(B 'invoke) [~'_6__ ~(I 'x)] false)
                         (~(B 'invoke) [~'_7__ ~(L 'x)] false)
                         (~(B 'invoke) [~'_8__ ~(F 'x)] false)
                         (~(B 'invoke) [~'_9__ ~(D 'x)] false)))

                     (defn ~'reduced?|test
                       {:quantum.core.type/type
                         (t/fn t/any?
                               ~'[(t/isa? Reduced)]
                               ~'[t/any?])}
                       ([~'x00__]
                         (ifs ((Array/get ~'reduced?|test|__0|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `Object>boolean) 'reduced?|test|__0|0) ~'x00__)
                              ;; TODO eliminate this check because it's not needed (`t/any?`)
                              ((Array/get ~'reduced?|test|__1|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `Object>boolean) 'reduced?|test|__1|0) ~'x00__)
                              (unsupported! `reduced?|test [~'x00__] 0))))))
            :cljs
              ($ (do (defn ~'reduced?|test [~'x]
                       (ifs (instance? Reduced x) true false)))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (throws (reduced?|test))
                 (is= (reduced?|test 123)             (reduced? 123))
                 (is= (reduced?|test true)            (reduced? true))
                 (is= (reduced?|test false)           (reduced? false))
                 (is= (reduced?|test nil)             (reduced? nil))
                 (is= (reduced?|test (reduced 123))   (reduced? (reduced 123)))
                 (is= (reduced?|test (reduced true))  (reduced? (reduced true)))
                 (is= (reduced?|test (reduced false)) (reduced? (reduced false)))
                 (is= (reduced?|test (reduced nil))   (reduced? (reduced nil))))))))

(deftest test|>boolean
  (let [actual
          (macroexpand '
            (self/defn #_:inline >boolean
               ([x tt/boolean?] x)
               ([x t/nil?]      false)
               ([x t/any?]      true)))
        expected
          (case (env-lang)
            :clj
              ($ (do ;; [x tt/boolean?]

                     (def ~(O<> '>boolean|__0|input0|types)
                       (*<> (t/isa? Boolean)))
                     (def ~'>boolean|__0|0
                       (reify* [boolean>boolean]
                         (~(B 'invoke) [~'_0__  ~(B 'x)] ~'x)))

                     ;; [x t/nil? -> (- t/nil? tt/boolean?)]

                     (def ~(O<> '>boolean|__1|input0|types)
                       (*<> (t/value nil)))
                     (def ~'>boolean|__1|0
                       (reify* [Object>boolean]
                         (~(B 'invoke) [~'_1__  ~(O 'x)] false)))

                     ;; [x t/any? -> (- t/any? t/nil? tt/boolean?)]

                     (def ~(O<> '>boolean|__2|input0|types)
                       (*<> t/any?))
                     (def ~'>boolean|__2|0
                       (reify* [Object>boolean boolean>boolean byte>boolean short>boolean
                                char>boolean int>boolean long>boolean float>boolean double>boolean]
                         (~(B 'invoke) [~'_2__  ~(O 'x)] true)
                         (~(B 'invoke) [~'_3__  ~(B 'x)] true)
                         (~(B 'invoke) [~'_4__  ~(Y 'x)] true)
                         (~(B 'invoke) [~'_5__  ~(S 'x)] true)
                         (~(B 'invoke) [~'_6__  ~(C 'x)] true)
                         (~(B 'invoke) [~'_7__  ~(I 'x)] true)
                         (~(B 'invoke) [~'_8__  ~(L 'x)] true)
                         (~(B 'invoke) [~'_9__  ~(F 'x)] true)
                         (~(B 'invoke) [~'_10__ ~(D 'x)] true)))

                     (defn ~'>boolean
                       {:quantum.core.type/type
                         (t/fn t/any?
                               ~'[tt/boolean?]
                               ~'[t/nil?]
                               ~'[t/any?])}
                       ([~'x00__]
                         (ifs ((Array/get ~'>boolean|__0|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `boolean>boolean) '>boolean|__0|0) ~'x00__)
                              ((Array/get ~'>boolean|__1|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `Object>boolean)  '>boolean|__1|0) ~'x00__)
                              ;; TODO eliminate this check because it's not needed (`t/any?`)
                              ((Array/get ~'>boolean|__2|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `Object>boolean)  '>boolean|__2|0) ~'x00__)
                              (unsupported! `>boolean [~'x00__] 0))))))
            :cljs
              ($ (do (defn ~'>boolean [~'x]
                       (ifs (tt/boolean? x) x
                            (nil?     x) false
                            true)))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (throws (>boolean))
                 (is= (>boolean true)  (boolean true))
                 (is= (>boolean false) (boolean false))
                 (is= (>boolean nil)   (boolean nil))
                 (is= (>boolean 123)   (boolean 123)))))))

;; Let's say you have (t/| t/string? t/number?) in one `fnt` overload.
;; This means that you *can't* have a reify with two Object>Object overloads and expect it to work
;; at all.
;; Therefore, each `fnt` overload necessarily has a one-to-many relationship with `reify`s.
;; Only the primitivized overloads belong grouped together in one `reify`.

(deftest test|>int*
  (let [actual
          (macroexpand '
            ;; Auto-upcasts to long or double (because 64-bit) unless you tell it otherwise
            ;; Will error if not all return values can be safely converted to the return spec
            (self/defn #_:inline >int* > tt/int?
              ([x (t/- tt/primitive? tt/boolean?)] (Primitive/uncheckedIntCast x))
              ([x (t/ref (t/isa? Number))] (.intValue x))))
        expected
        (case (env-lang)
          :clj
          ($ (do ;; [x (t/- tt/primitive? tt/boolean?)]

                 ;; These are non-primitivized
                 (def ~(O<> '>int*|__0|input0|types)
                   (*<> (t/isa? java.lang.Byte)
                        (t/isa? java.lang.Short)
                        (t/isa? java.lang.Character)
                        (t/isa? java.lang.Integer)
                        (t/isa? java.lang.Long)
                        (t/isa? java.lang.Float)
                        (t/isa? java.lang.Double)))
                 (def ~'>int*|__0|0
                   (reify* [byte>int]
                     (~(I 'invoke) [~'_0__ ~(Y 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|1
                   (reify* [short>int]
                     (~(I 'invoke) [~'_1__ ~(S 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|2
                   (reify* [char>int]
                     (~(I 'invoke) [~'_2__ ~(C 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|3
                   (reify* [int>int]
                     (~(I 'invoke) [~'_3__ ~(I 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|4
                   (reify* [long>int]
                     (~(I 'invoke) [~'_4__ ~(L 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|5
                   (reify* [float>int]
                     (~(I 'invoke) [~'_5__ ~(F 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|6
                   (reify* [double>int]
                     (~(I 'invoke) [~'_6__ ~(D 'x)] ~'(. Primitive uncheckedIntCast x))))

                 ;; [x (t/ref (t/isa? Number))
                 ;;  -> (t/- (t/ref (t/isa? Number)) (t/- tt/primitive? tt/boolean?))]

                 (def ~(O<> '>int*|__1|input0|types)
                   (*<> ~(with-meta `(t/isa? Number) {:quantum.core.type/ref? true})))
                 (def ~'>int*|__1|0
                   (reify* [Object>int]
                     (~(I 'invoke) [~'_7__ ~(O 'x)]
                       (let* [~(tag "java.lang.Number" 'x) ~'x] ~'(. x intValue)))))

                 (defn ~'>int*
                   {:quantum.core.type/type
                     (t/fn ~'tt/int?
                           ~'[(t/- tt/primitive? tt/boolean?)]
                           ~'[(t/ref (t/isa? Number))])}
                   ([~'x00__]
                     (ifs ((Array/get ~'>int*|__0|input0|types 0) ~'x00__)
                            (.invoke ~(tag (cstr `byte>int)   '>int*|__0|0) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 1) ~'x00__)
                            (.invoke ~(tag (cstr `short>int)  '>int*|__0|1) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 2) ~'x00__)
                            (.invoke ~(tag (cstr `char>int)   '>int*|__0|2) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 3) ~'x00__)
                            (.invoke ~(tag (cstr `int>int)    '>int*|__0|3) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 4) ~'x00__)
                            (.invoke ~(tag (cstr `long>int)   '>int*|__0|4) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 5) ~'x00__)
                            (.invoke ~(tag (cstr `float>int)  '>int*|__0|5) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 6) ~'x00__)
                            (.invoke ~(tag (cstr `double>int) '>int*|__0|6) ~'x00__)
                          ((Array/get ~'>int*|__1|input0|types 0) ~'x00__)
                            (.invoke ~(tag (cstr `Object>int) '>int*|__1|0) ~'x00__)
                          (unsupported! `>int* [~'x00__] 0)))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (throws (>int*))
                 (throws (>int* nil))
                 (throws (>int* ""))
                 (is (identical? (>int*  1)       (clojure.lang.RT/uncheckedIntCast  1)))
                 (is (identical? (>int*  1.0)     (clojure.lang.RT/uncheckedIntCast  1.0)))
                 (is (identical? (>int*  1.1)     (clojure.lang.RT/uncheckedIntCast  1.1)))
                 (is (identical? (>int* -1)       (clojure.lang.RT/uncheckedIntCast -1)))
                 (is (identical? (>int* -1.0)     (clojure.lang.RT/uncheckedIntCast -1.0)))
                 (is (identical? (>int* -1.1)     (clojure.lang.RT/uncheckedIntCast -1.1)))
                 (is (identical? (>int* (byte 1)) (clojure.lang.RT/uncheckedIntCast (byte 1)))))))))

;; Because "Method code too large" error
(def >|ftype-form
  ($ (t/ftype #?(:clj (t/isa? Boolean) :cljs tt/boolean?)
   #?@(:clj  [[(t/isa? Byte)      (t/isa? Byte)      :> (t/isa? Boolean)]
              [(t/isa? Byte)      (t/isa? Short)     :> (t/isa? Boolean)]
              [(t/isa? Byte)      (t/isa? Character) :> (t/isa? Boolean)]
              [(t/isa? Byte)      (t/isa? Integer)   :> (t/isa? Boolean)]
              [(t/isa? Byte)      (t/isa? Long)      :> (t/isa? Boolean)]
              [(t/isa? Byte)      (t/isa? Float)     :> (t/isa? Boolean)]
              [(t/isa? Byte)      (t/isa? Double)    :> (t/isa? Boolean)]
              [(t/isa? Short)     (t/isa? Byte)      :> (t/isa? Boolean)]
              [(t/isa? Short)     (t/isa? Short)     :> (t/isa? Boolean)]
              [(t/isa? Short)     (t/isa? Character) :> (t/isa? Boolean)]
              [(t/isa? Short)     (t/isa? Integer)   :> (t/isa? Boolean)]
              [(t/isa? Short)     (t/isa? Long)      :> (t/isa? Boolean)]
              [(t/isa? Short)     (t/isa? Float)     :> (t/isa? Boolean)]
              [(t/isa? Short)     (t/isa? Double)    :> (t/isa? Boolean)]
              [(t/isa? Character) (t/isa? Byte)      :> (t/isa? Boolean)]
              [(t/isa? Character) (t/isa? Short)     :> (t/isa? Boolean)]
              [(t/isa? Character) (t/isa? Character) :> (t/isa? Boolean)]
              [(t/isa? Character) (t/isa? Integer)   :> (t/isa? Boolean)]
              [(t/isa? Character) (t/isa? Long)      :> (t/isa? Boolean)]
              [(t/isa? Character) (t/isa? Float)     :> (t/isa? Boolean)]
              [(t/isa? Character) (t/isa? Double)    :> (t/isa? Boolean)]
              [(t/isa? Integer)   (t/isa? Byte)      :> (t/isa? Boolean)]
              [(t/isa? Integer)   (t/isa? Short)     :> (t/isa? Boolean)]
              [(t/isa? Integer)   (t/isa? Character) :> (t/isa? Boolean)]
              [(t/isa? Integer)   (t/isa? Integer)   :> (t/isa? Boolean)]
              [(t/isa? Integer)   (t/isa? Long)      :> (t/isa? Boolean)]
              [(t/isa? Integer)   (t/isa? Float)     :> (t/isa? Boolean)]
              [(t/isa? Integer)   (t/isa? Double)    :> (t/isa? Boolean)]
              [(t/isa? Long)      (t/isa? Byte)      :> (t/isa? Boolean)]
              [(t/isa? Long)      (t/isa? Short)     :> (t/isa? Boolean)]
              [(t/isa? Long)      (t/isa? Character) :> (t/isa? Boolean)]
              [(t/isa? Long)      (t/isa? Integer)   :> (t/isa? Boolean)]
              [(t/isa? Long)      (t/isa? Long)      :> (t/isa? Boolean)]
              [(t/isa? Long)      (t/isa? Float)     :> (t/isa? Boolean)]
              [(t/isa? Long)      (t/isa? Double)    :> (t/isa? Boolean)]
              [(t/isa? Float)     (t/isa? Byte)      :> (t/isa? Boolean)]
              [(t/isa? Float)     (t/isa? Short)     :> (t/isa? Boolean)]
              [(t/isa? Float)     (t/isa? Character) :> (t/isa? Boolean)]
              [(t/isa? Float)     (t/isa? Integer)   :> (t/isa? Boolean)]
              [(t/isa? Float)     (t/isa? Long)      :> (t/isa? Boolean)]
              [(t/isa? Float)     (t/isa? Float)     :> (t/isa? Boolean)]
              [(t/isa? Float)     (t/isa? Double)    :> (t/isa? Boolean)]
              [(t/isa? Double)    (t/isa? Byte)      :> (t/isa? Boolean)]
              [(t/isa? Double)    (t/isa? Short)     :> (t/isa? Boolean)]
              [(t/isa? Double)    (t/isa? Character) :> (t/isa? Boolean)]
              [(t/isa? Double)    (t/isa? Integer)   :> (t/isa? Boolean)]
              [(t/isa? Double)    (t/isa? Long)      :> (t/isa? Boolean)]
              [(t/isa? Double)    (t/isa? Float)     :> (t/isa? Boolean)]
              [(t/isa? Double)    (t/isa? Double)    :> (t/isa? Boolean)]]
       :cljs [[tt/double? tt/double? :> (t/assume tt/boolean?)]]))))

(def >|dynamic-dispatch-form
  ($ (defn ~'> {:quantum.core.type/type ~>|ftype-form}
       ([~'x00__ ~'x10__]
         (ifs
           ((Array/get ~'>|__0|types 0) ~'x00__)
             (ifs
               ((Array/get ~'>|__0|types  1) ~'x10__)
                 (. ~(tag (cstr `byte+byte>boolean)      '>|__0)  ~'invoke  ~'x00__ ~'x10__)
               ((Array/get ~'>|__1|types  1) ~'x10__)
                 (. ~(tag (cstr `byte+short>boolean)     '>|__1)  ~'invoke  ~'x00__ ~'x10__)
               ((Array/get ~'>|__2|types  1) ~'x10__)
                 (. ~(tag (cstr `byte+char>boolean)      '>|__2)  ~'invoke  ~'x00__ ~'x10__)
               ((Array/get ~'>|__3|types  1) ~'x10__)
                 (. ~(tag (cstr `byte+int>boolean)       '>|__3)  ~'invoke  ~'x00__ ~'x10__)
               ((Array/get ~'>|__4|types  1) ~'x10__)
                 (. ~(tag (cstr `byte+long>boolean)      '>|__4)  ~'invoke  ~'x00__ ~'x10__)
               ((Array/get ~'>|__5|types  1) ~'x10__)
                 (. ~(tag (cstr `byte+float>boolean)     '>|__5)  ~'invoke  ~'x00__ ~'x10__)
               ((Array/get ~'>|__6|types  1) ~'x10__)
                 (. ~(tag (cstr `byte+double>boolean)    '>|__6)  ~'invoke  ~'x00__ ~'x10__)
               (unsupported! `> [~'x00__ ~'x10__] 1))
           ((Array/get ~'>|__7|types 0) ~'x00__)
             (ifs
               ((Array/get ~'>|__7|types  1) ~'x10__)
                 (. ~(tag (cstr `short+byte>boolean)     '>|__7)  ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__8|types  1) ~'x10__)
                 (. ~(tag (cstr `short+short>boolean)    '>|__8)  ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__9|types  1) ~'x10__)
                 (. ~(tag (cstr `short+char>boolean)     '>|__9)  ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__10|types 1) ~'x10__)
                 (. ~(tag (cstr `short+int>boolean)      '>|__10) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__11|types 1) ~'x10__)
                 (. ~(tag (cstr `short+long>boolean)     '>|__11) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__12|types 1) ~'x10__)
                 (. ~(tag (cstr `short+float>boolean)    '>|__12) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__13|types 1) ~'x10__)
                 (. ~(tag (cstr `short+double>boolean)   '>|__13) ~'invoke ~'x00__ ~'x10__)
               (unsupported! `> [~'x00__ ~'x10__] 1))
           ((Array/get ~'>|__14|types 0) ~'x00__)
             (ifs
               ((Array/get ~'>|__14|types 1) ~'x10__)
                 (. ~(tag (cstr `char+byte>boolean)      '>|__14) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__15|types 1) ~'x10__)
                 (. ~(tag (cstr `char+short>boolean)     '>|__15) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__16|types 1) ~'x10__)
                 (. ~(tag (cstr `char+char>boolean)      '>|__16) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__17|types 1) ~'x10__)
                 (. ~(tag (cstr `char+int>boolean)       '>|__17) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__18|types 1) ~'x10__)
                 (. ~(tag (cstr `char+long>boolean)      '>|__18) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__19|types 1) ~'x10__)
                 (. ~(tag (cstr `char+float>boolean)     '>|__19) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__20|types 1) ~'x10__)
                 (. ~(tag (cstr `char+double>boolean)    '>|__20) ~'invoke ~'x00__ ~'x10__)
               (unsupported! `> [~'x00__ ~'x10__] 1))
           ((Array/get ~'>|__21|types 0) ~'x00__)
             (ifs
               ((Array/get ~'>|__21|types 1) ~'x10__)
                 (. ~(tag (cstr `int+byte>boolean)       '>|__21) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__22|types 1) ~'x10__)
                 (. ~(tag (cstr `int+short>boolean)      '>|__22) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__23|types 1) ~'x10__)
                 (. ~(tag (cstr `int+char>boolean)       '>|__23) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__24|types 1) ~'x10__)
                 (. ~(tag (cstr `int+int>boolean)        '>|__24) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__25|types 1) ~'x10__)
                 (. ~(tag (cstr `int+long>boolean)       '>|__25) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__26|types 1) ~'x10__)
                 (. ~(tag (cstr `int+float>boolean)      '>|__26) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__27|types 1) ~'x10__)
                 (. ~(tag (cstr `int+double>boolean)     '>|__27) ~'invoke ~'x00__ ~'x10__)
               (unsupported! `> [~'x00__ ~'x10__] 1))
           ((Array/get ~'>|__28|types 0) ~'x00__)
             (ifs
               ((Array/get ~'>|__28|types 1) ~'x10__)
                 (. ~(tag (cstr `long+byte>boolean)      '>|__28) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__29|types 1) ~'x10__)
                 (. ~(tag (cstr `long+short>boolean)     '>|__29) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__30|types 1) ~'x10__)
                 (. ~(tag (cstr `long+char>boolean)      '>|__30) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__31|types 1) ~'x10__)
                 (. ~(tag (cstr `long+int>boolean)       '>|__31) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__32|types 1) ~'x10__)
                 (. ~(tag (cstr `long+long>boolean)      '>|__32) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__33|types 1) ~'x10__)
                 (. ~(tag (cstr `long+float>boolean)     '>|__33) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__34|types 1) ~'x10__)
                 (. ~(tag (cstr `long+double>boolean)    '>|__34) ~'invoke ~'x00__ ~'x10__)
               (unsupported! `> [~'x00__ ~'x10__] 1))
           ((Array/get ~'>|__35|types 0) ~'x00__)
             (ifs
               ((Array/get ~'>|__35|types 1) ~'x10__)
                 (. ~(tag (cstr `float+byte>boolean)     '>|__35) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__36|types 1) ~'x10__)
                 (. ~(tag (cstr `float+short>boolean)    '>|__36) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__37|types 1) ~'x10__)
                 (. ~(tag (cstr `float+char>boolean)     '>|__37) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__38|types 1) ~'x10__)
                 (. ~(tag (cstr `float+int>boolean)      '>|__38) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__39|types 1) ~'x10__)
                 (. ~(tag (cstr `float+long>boolean)     '>|__39) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__40|types 1) ~'x10__)
                 (. ~(tag (cstr `float+float>boolean)    '>|__40) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__41|types 1) ~'x10__)
                 (. ~(tag (cstr `float+double>boolean)   '>|__41) ~'invoke ~'x00__ ~'x10__)
               (unsupported! `> [~'x00__ ~'x10__] 1))
           ((Array/get ~'>|__42|types 0) ~'x00__)
             (ifs
               ((Array/get ~'>|__42|types 1) ~'x10__)
                 (. ~(tag (cstr `double+byte>boolean)    '>|__42) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__43|types 1) ~'x10__)
                 (. ~(tag (cstr `double+short>boolean)   '>|__43) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__44|types 1) ~'x10__)
                 (. ~(tag (cstr `double+char>boolean)    '>|__44) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__45|types 1) ~'x10__)
                 (. ~(tag (cstr `double+int>boolean)     '>|__45) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__46|types 1) ~'x10__)
                 (. ~(tag (cstr `double+long>boolean)    '>|__46) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__47|types 1) ~'x10__)
                 (. ~(tag (cstr `double+float>boolean)   '>|__47) ~'invoke ~'x00__ ~'x10__)
               ((Array/get ~'>|__48|types 1) ~'x10__)
                 (. ~(tag (cstr `double+double>boolean)  '>|__48) ~'invoke ~'x00__ ~'x10__)
               (unsupported! `> [~'x00__ ~'x10__] 1))
           (unsupported! `> [~'x00__ ~'x10__] 0))))))

(deftest test|>
  (let [actual
          (macroexpand '
            (self/defn #_:inline > > tt/boolean?
         #?(:clj  ([a tt/comparable-primitive? b tt/comparable-primitive? > tt/boolean?]
                    (Numeric/gt a b))
            :cljs ([a tt/double?               b tt/double?               > (t/assume tt/boolean?)]
                    (cljs.core/> a b)))))
        expected
        (case (env-lang)
          :clj
          ($ (do (declare ~'>)

                 ;; [a t/comparable-primitive? b t/comparable-primitive? > tt/boolean?]

                 (def ~(O<> '>|__0|types)  (*<> (t/isa? Byte) (t/isa? Byte)))
                 (def ~'>|__0
                   (reify* [byte+byte>boolean]
                     (~(B 'invoke) [~'_0__  ~(Y 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__1|types)  (*<> (t/isa? Byte) (t/isa? Short)))
                 (def ~'>|__1
                   (reify* [byte+short>boolean]
                     (~(B 'invoke) [~'_1__  ~(Y 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__2|types)  (*<> (t/isa? Byte) (t/isa? Character)))
                 (def ~'>|__2
                   (reify* [byte+char>boolean]
                     (~(B 'invoke) [~'_2__  ~(Y 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__3|types)  (*<> (t/isa? Byte) (t/isa? Integer)))
                 (def ~'>|__3
                   (reify* [byte+int>boolean]
                     (~(B 'invoke) [~'_3__  ~(Y 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__4|types)  (*<> (t/isa? Byte) (t/isa? Long)))
                 (def ~'>|__4
                   (reify* [byte+long>boolean]
                     (~(B 'invoke) [~'_4__  ~(Y 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__5|types)  (*<> (t/isa? Byte) (t/isa? Float)))
                 (def ~'>|__5
                   (reify* [byte+float>boolean]
                     (~(B 'invoke) [~'_5__  ~(Y 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__6|types)  (*<> (t/isa? Byte) (t/isa? Double)))
                 (def ~'>|__6
                   (reify* [byte+double>boolean]
                     (~(B 'invoke) [~'_6__  ~(Y 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__7|types)  (*<> (t/isa? Short) (t/isa? Byte)))
                 (def ~'>|__7
                   (reify* [short+byte>boolean]
                     (~(B 'invoke) [~'_7__  ~(S 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__8|types)  (*<> (t/isa? Short) (t/isa? Short)))
                 (def ~'>|__8
                   (reify* [short+short>boolean]
                     (~(B 'invoke) [~'_8__  ~(S 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__9|types)  (*<> (t/isa? Short) (t/isa? Character)))
                 (def ~'>|__9
                   (reify* [short+char>boolean]
                     (~(B 'invoke) [~'_9__  ~(S 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__10|types) (*<> (t/isa? Short) (t/isa? Integer)))
                 (def ~'>|__10
                   (reify* [short+int>boolean]
                     (~(B 'invoke) [~'_10__ ~(S 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__11|types) (*<> (t/isa? Short) (t/isa? Long)))
                 (def ~'>|__11
                   (reify* [short+long>boolean]
                     (~(B 'invoke) [~'_11__ ~(S 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__12|types) (*<> (t/isa? Short) (t/isa? Float)))
                 (def ~'>|__12
                   (reify* [short+float>boolean]
                     (~(B 'invoke) [~'_12__ ~(S 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__13|types) (*<> (t/isa? Short) (t/isa? Double)))
                 (def ~'>|__13
                   (reify* [short+double>boolean]
                     (~(B 'invoke) [~'_13__ ~(S 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__14|types) (*<> (t/isa? Character) (t/isa? Byte)))
                 (def ~'>|__14
                   (reify* [char+byte>boolean]
                     (~(B 'invoke) [~'_14__ ~(C 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__15|types) (*<> (t/isa? Character) (t/isa? Short)))
                 (def ~'>|__15
                   (reify* [char+short>boolean]
                     (~(B 'invoke) [~'_15__ ~(C 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__16|types) (*<> (t/isa? Character) (t/isa? Character)))
                 (def ~'>|__16
                   (reify* [char+char>boolean]
                     (~(B 'invoke) [~'_16__ ~(C 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__17|types) (*<> (t/isa? Character) (t/isa? Integer)))
                 (def ~'>|__17
                   (reify* [char+int>boolean]
                     (~(B 'invoke) [~'_17__ ~(C 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__18|types) (*<> (t/isa? Character) (t/isa? Long)))
                 (def ~'>|__18
                   (reify* [char+long>boolean]
                     (~(B 'invoke) [~'_18__ ~(C 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__19|types) (*<> (t/isa? Character) (t/isa? Float)))
                 (def ~'>|__19
                   (reify* [char+float>boolean]
                     (~(B 'invoke) [~'_19__ ~(C 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__20|types) (*<> (t/isa? Character) (t/isa? Double)))
                 (def ~'>|__20
                   (reify* [char+double>boolean]
                     (~(B 'invoke) [~'_20__ ~(C 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__21|types) (*<> (t/isa? Integer) (t/isa? Byte)))
                 (def ~'>|__21
                   (reify* [int+byte>boolean]
                     (~(B 'invoke) [~'_21__ ~(I 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__22|types) (*<> (t/isa? Integer) (t/isa? Short)))
                 (def ~'>|__22
                   (reify* [int+short>boolean]
                     (~(B 'invoke) [~'_22__ ~(I 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__23|types) (*<> (t/isa? Integer) (t/isa? Character)))
                 (def ~'>|__23
                   (reify* [int+char>boolean]
                     (~(B 'invoke) [~'_23__ ~(I 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__24|types) (*<> (t/isa? Integer) (t/isa? Integer)))
                 (def ~'>|__24
                   (reify* [int+int>boolean]
                     (~(B 'invoke) [~'_24__ ~(I 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__25|types) (*<> (t/isa? Integer) (t/isa? Long)))
                 (def ~'>|__25
                   (reify* [int+long>boolean]
                     (~(B 'invoke) [~'_25__ ~(I 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__26|types) (*<> (t/isa? Integer) (t/isa? Float)))
                 (def ~'>|__26
                   (reify* [int+float>boolean]
                     (~(B 'invoke) [~'_26__ ~(I 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__27|types) (*<> (t/isa? Integer) (t/isa? Double)))
                 (def ~'>|__27
                   (reify* [int+double>boolean]
                     (~(B 'invoke) [~'_27__ ~(I 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__28|types) (*<> (t/isa? Long) (t/isa? Byte)))
                 (def ~'>|__28
                   (reify* [long+byte>boolean]
                     (~(B 'invoke) [~'_28__ ~(L 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__29|types) (*<> (t/isa? Long) (t/isa? Short)))
                 (def ~'>|__29
                   (reify* [long+short>boolean]
                     (~(B 'invoke) [~'_29__ ~(L 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__30|types) (*<> (t/isa? Long) (t/isa? Character)))
                 (def ~'>|__30
                   (reify* [long+char>boolean]
                     (~(B 'invoke) [~'_30__ ~(L 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__31|types) (*<> (t/isa? Long) (t/isa? Integer)))
                 (def ~'>|__31
                   (reify* [long+int>boolean]
                     (~(B 'invoke) [~'_31__ ~(L 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__32|types) (*<> (t/isa? Long) (t/isa? Long)))
                 (def ~'>|__32
                   (reify* [long+long>boolean]
                     (~(B 'invoke) [~'_32__ ~(L 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__33|types) (*<> (t/isa? Long) (t/isa? Float)))
                 (def ~'>|__33
                   (reify* [long+float>boolean]
                     (~(B 'invoke) [~'_33__ ~(L 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__34|types) (*<> (t/isa? Long) (t/isa? Double)))
                 (def ~'>|__34
                   (reify* [long+double>boolean]
                     (~(B 'invoke) [~'_34__ ~(L 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__35|types) (*<> (t/isa? Float) (t/isa? Byte)))
                 (def ~'>|__35
                   (reify* [float+byte>boolean]
                     (~(B 'invoke) [~'_35__ ~(F 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__36|types) (*<> (t/isa? Float) (t/isa? Short)))
                 (def ~'>|__36
                   (reify* [float+short>boolean]
                     (~(B 'invoke) [~'_36__ ~(F 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__37|types) (*<> (t/isa? Float) (t/isa? Character)))
                 (def ~'>|__37
                   (reify* [float+char>boolean]
                     (~(B 'invoke) [~'_37__ ~(F 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__38|types) (*<> (t/isa? Float) (t/isa? Integer)))
                 (def ~'>|__38
                   (reify* [float+int>boolean]
                     (~(B 'invoke) [~'_38__ ~(F 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__39|types) (*<> (t/isa? Float) (t/isa? Long)))
                 (def ~'>|__39
                   (reify* [float+long>boolean]
                     (~(B 'invoke) [~'_39__ ~(F 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__40|types) (*<> (t/isa? Float) (t/isa? Float)))
                 (def ~'>|__40
                   (reify* [float+float>boolean]
                     (~(B 'invoke) [~'_40__ ~(F 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__41|types) (*<> (t/isa? Float) (t/isa? Double)))
                 (def ~'>|__41
                   (reify* [float+double>boolean]
                     (~(B 'invoke) [~'_41__ ~(F 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__42|types) (*<> (t/isa? Double) (t/isa? Byte)))
                 (def ~'>|__42
                   (reify* [double+byte>boolean]
                     (~(B 'invoke) [~'_42__ ~(D 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__43|types) (*<> (t/isa? Double) (t/isa? Short)))
                 (def ~'>|__43
                   (reify* [double+short>boolean]
                     (~(B 'invoke) [~'_43__ ~(D 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__44|types) (*<> (t/isa? Double) (t/isa? Character)))
                 (def ~'>|__44
                   (reify* [double+char>boolean]
                     (~(B 'invoke) [~'_44__ ~(D 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__45|types) (*<> (t/isa? Double) (t/isa? Integer)))
                 (def ~'>|__45
                   (reify* [double+int>boolean]
                     (~(B 'invoke) [~'_45__ ~(D 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__46|types) (*<> (t/isa? Double) (t/isa? Long)))
                 (def ~'>|__46
                   (reify* [double+long>boolean]
                     (~(B 'invoke) [~'_46__ ~(D 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__47|types) (*<> (t/isa? Double) (t/isa? Float)))
                 (def ~'>|__47
                   (reify* [double+float>boolean]
                     (~(B 'invoke) [~'_47__ ~(D 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(O<> '>|__48|types) (*<> (t/isa? Double) (t/isa? Double)))
                 (def ~'>|__48
                   (reify* [double+double>boolean]
                     (~(B 'invoke) [~'_48__ ~(D 'a) ~(D 'b)] ~'(. Numeric gt a b))))

                 ~>|dynamic-dispatch-form))
        :cljs
        ($ (do (defn ~'>
                 ([a0 a1]
                   (ifs (double? a0)
                          (ifs (double? a1)
                                 (let* [a a0 b a1] (cljs.core/> a b))
                               (unsupported! `> [a0 a1] 1))
                        (unsupported! `> [a0 a1] 0)))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is= (> 0 1)   (core/> 0 1))
                 (is= (> 1 0)   (core/> 1 0))
                 (is= (> 1.0 0) (core/> 1.0 0))))))

(deftest test|>long*
  (let [actual
          (macroexpand '
            (self/defn #_:inline >long*
              {:source "clojure.lang.RT.uncheckedLongCast"}
              > tt/long?
              ([x (t/- tt/primitive? tt/boolean?)] (Primitive/uncheckedLongCast x))
              ([x (t/ref (t/isa? Number))] (.longValue x))))
        expected
          (case (env-lang)
            :clj ($ (do (declare ~'>long*)

                        ;; [x (t/- tt/primitive? tt/boolean?)]

                        (def ~(O<> '>long*|__0|input0|types)
                          (*<> (t/isa? java.lang.Byte)
                               (t/isa? java.lang.Short)
                               (t/isa? java.lang.Character)
                               (t/isa? java.lang.Integer)
                               (t/isa? java.lang.Long)
                               (t/isa? java.lang.Float)
                               (t/isa? java.lang.Double)))
                        (def ~'>long*|__0|0
                          (reify* [byte>long]
                            (~(L 'invoke) [~'_0__ ~(Y             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~'>long*|__0|1
                          (reify* [short>long]
                            (~(L 'invoke) [~'_1__ ~(S            'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~'>long*|__0|2
                          (reify* [char>long]
                            (~(L 'invoke) [~'_2__ ~(C             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~'>long*|__0|3
                          (reify* [int>long]
                            (~(L 'invoke) [~'_3__ ~(I              'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~'>long*|__0|4
                          (reify* [long>long]
                            (~(L 'invoke) [~'_4__ ~(L             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~'>long*|__0|5
                          (reify* [float>long]
                            (~(L 'invoke) [~'_5__ ~(F            'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~'>long*|__0|6
                          (reify* [double>long]
                            (~(L 'invoke) [~'_6__ ~(D           'x)]
                              ~'(. Primitive uncheckedLongCast x))))

                        ;; [x (t/ref (t/isa? Number))]

                        (def ~(O<> '>long*|__1|input0|types)
                          (*<> ~(with-meta `(t/isa? Number) {:quantum.core.type/ref? true})))
                        (def ~'>long*|__1|0
                          (reify* [Object>long]
                            (~(L 'invoke) [~'_7__ ~(O 'x)]
                              (let* [~(tag "java.lang.Number" 'x) ~'x] ~'(. x longValue)))))

                        (defn ~'>long*
                          {:source "clojure.lang.RT.uncheckedLongCast"
                           :quantum.core.type/type
                             (t/fn ~'long?
                                   ~'[(t/- tt/primitive? tt/boolean?)]
                                   ~'[(t/ref (t/isa? Number))])}
                          ([~'x00__]
                            (ifs
                              ((Array/get ~'>long*|__0|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `byte>long)   '>long*|__0|0) ~'x00__)
                              ((Array/get ~'>long*|__0|input0|types 1) ~'x00__)
                                (.invoke ~(tag (cstr `short>long)  '>long*|__0|1) ~'x00__)
                              ((Array/get ~'>long*|__0|input0|types 2) ~'x00__)
                                (.invoke ~(tag (cstr `char>long)   '>long*|__0|2) ~'x00__)
                              ((Array/get ~'>long*|__0|input0|types 3) ~'x00__)
                                (.invoke ~(tag (cstr `int>long)    '>long*|__0|3) ~'x00__)
                              ((Array/get ~'>long*|__0|input0|types 4) ~'x00__)
                                (.invoke ~(tag (cstr `long>long)   '>long*|__0|4) ~'x00__)
                              ((Array/get ~'>long*|__0|input0|types 5) ~'x00__)
                                (.invoke ~(tag (cstr `float>long)  '>long*|__0|5) ~'x00__)
                              ((Array/get ~'>long*|__0|input0|types 6) ~'x00__)
                                (.invoke ~(tag (cstr `double>long) '>long*|__0|6) ~'x00__)
                              ((Array/get ~'>long*|__1|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `Object>long) '>long*|__1|0) ~'x00__)
                              (unsupported! `>long* [~'x00__] 0)))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval
        '(do (throws (>long*))
             (throws (>long* nil))
             (throws (>long* ""))
             (is (identical? (>long*  1)       (clojure.lang.RT/uncheckedLongCast  1)))
             (is (identical? (>long*  1.0)     (clojure.lang.RT/uncheckedLongCast  1.0)))
             (is (identical? (>long*  1.1)     (clojure.lang.RT/uncheckedLongCast  1.1)))
             (is (identical? (>long* -1)       (clojure.lang.RT/uncheckedLongCast -1)))
             (is (identical? (>long* -1.0)     (clojure.lang.RT/uncheckedLongCast -1.0)))
             (is (identical? (>long* -1.1)     (clojure.lang.RT/uncheckedLongCast -1.1)))
             (is (identical? (>long* (byte 1)) (clojure.lang.RT/uncheckedLongCast (byte 1)))))))))

#?(:clj
(deftest ref-output-type-test
  "Tests whether refs are output when requested instead of primitives"
  (let [actual
          (macroexpand '
            (self/defn ref-output-type
              ([x tt/boolean? > (t/ref tt/boolean?)] (Boolean. x))
              ([x tt/byte?    > (t/ref tt/byte?)]    (Byte.    x))))
        expected
          ($ (do (declare ~'ref-output-type)

                 ;; [x tt/boolean? > (t/ref tt/boolean?)]

                 (def ~(O<> 'ref-output-type|__0|types) (*<> (t/isa? java.lang.Boolean)))
                 (def ~'ref-output-type|__0
                   (reify* [boolean>Object] (~(O 'invoke) [~'_0__ ~(B 'x)] (new ~'Boolean ~'x))))

                 ;; [x tt/byte? > (t/ref tt/byte?)]

                 (def ~(O<> 'ref-output-type|__1|types) (*<> (t/isa? java.lang.Byte)))
                 (def ~'ref-output-type|__1
                   (reify* [byte>Object] (~(O 'invoke) [~'_1__ ~(Y 'x)] (new ~'Byte ~'x))))

                 (defn ~'ref-output-type
                   {:quantum.core.type/type
                     (t/ftype t/any?
                              [(t/isa? Boolean) :> (t/ref (t/isa? Boolean))]
                              [(t/isa? Byte)    :> (t/ref (t/isa? Byte))])}
                   ([~'x00__]
                     (ifs
                       ((Array/get ~'ref-output-type|__0|types 0) ~'x00__)
                         (. ~(tag (cstr `boolean>Object) 'ref-output-type|__0) ~'invoke ~'x00__)
                       ((Array/get ~'ref-output-type|__1|types 0) ~'x00__)
                         (. ~(tag (cstr `byte>Object)    'ref-output-type|__1) ~'invoke ~'x00__)
                       (unsupported! `ref-output-type [~'x00__] 0))))))]
    (testing "code equivalence" (is-code= actual expected)))))

(self/defn >big-integer > (t/isa? java.math.BigInteger)
  ([x tt/ratio? > (t/* (t/isa? java.math.BigInteger))] (.bigIntegerValue x)))

;; NOTE would use `>long` but that's already an interface
(deftest test|>long-checked
  (let [actual
          (macroexpand '
            (self/defn >long-checked
              {:source "clojure.lang.RT.longCast"}
              > tt/long?
              ;; TODO multi-arity `t/-`
              ([x (t/- tt/primitive? tt/boolean? tt/float? tt/double?)] (>long* x))
              ([x (t/and (t/or tt/double? tt/float?)
                         ;; TODO add this back in
                         #_(t/fn [x (t/or t/double? t/float?)] (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))]
                (>long* x))
              ([x (t/and (t/isa? clojure.lang.BigInt)
                         ;; TODO add this back in
                         #_(t/fn [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))]
                (.lpart x))
              ([x (t/and (t/isa? java.math.BigInteger)
                         ;; TODO add this back in
                         #_(t/fn [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))]
                (.longValue x))
              ([x tt/ratio?] (-> x >big-integer >long-checked))
              ([x (t/value true)]  1)
              ([x (t/value false)] 0)
              ([x t/string?] (Long/parseLong x))
              ([x t/string?, radix tt/int?] (Long/parseLong x radix))))
        expected
          (case (env-lang)
            :clj ($ (do #_[x (t/- tt/boolean? tt/boolean? float? double?)]

                        #_(def ~'>long|__0|input-types (*<> byte?))
                        (def ~'>long|__0
                          (reify byte>long
                            (~(L 'invoke) [_## ~(Y 'x)]
                              ;; Resolved from `(>long* x)`
                              (.invoke >long*|__0 ~'x))))

                        #_(def ~'>long|__1|input-types (*<> char?))
                        (def ~'>long|__1
                          (reify char>long
                            (~(L 'invoke) [_## ~(C 'x)]
                              ;; Resolved from `(>long* x)`
                              (.invoke >long*|__1 ~'x))))

                        #_(def ~'>long|__2|input-types (*<> short?))
                        (def ~'>long|__2
                          (reify short>long
                            (~(L 'invoke) [_## ~(S 'x)]
                              ;; Resolved from `(>long* x)`
                              (.invoke >long*|__2 ~'x))))

                        #_(def ~'>long|__3|input-types (*<> tt/int?))
                        (def ~'>long|__3
                          (reify int>long
                            (~(L 'invoke) [_## ~(I 'x)]
                              ;; Resolved from `(>long* x)`
                              (.invoke >long*|__3 ~'x))))

                        #_(def ~'>long|__4|input-types (*<> tt/long?))
                        (def ~'>long|__4
                          (reify long>long
                            (~(L 'invoke) [_## ~(L 'x)]
                              ;; Resolved from `(>long* x)`
                              (.invoke >long*|__4 ~'x))))

                        #_[x (t/and (t/or double? float?)
                                    (t/fn [x (t/or double? float?)]
                                      (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))]

                        #_(def ~'>long|__5|input-types
                          (*<> (t/and double?
                                      (t/fn [x (t/or double? float?)]
                                        (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))))
                        (def ~'>long|__5
                          (reify double>long
                            (~(L 'invoke) [_## ~(D 'x)]
                              ;; Resolved from `(>long* x)`
                              (.invoke >long*|__6 ~'x))))

                        #_(def ~'>long|__6|input-types
                          (*<> (t/and t/float?
                                      (t/fn [x (t/or double? float?)]
                                        (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))))
                        (def ~'>long|__6
                          (reify float>long
                            (~(L 'invoke) [_## ~(F 'x)]
                              ;; Resolved from `(>long* x)`
                              (.invoke >long*|__5 ~'x))))

                        #_[(t/and (t/isa? clojure.lang.BigInt)
                                  (t/fn [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))]

                        #_(def ~'>long|__7|input-types
                          (*<> (t/and (t/isa? clojure.lang.BigInt)
                                      (t/fn [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))))
                        (def ~'>long|__7
                          (reify Object>long
                            (~(L 'invoke) [_## ~(O 'x)]
                              (let* [~(tag "clojure.lang.BigInt" 'x) ~'x] ~'(.lpart x)))))

                        #_[x (t/and (t/isa? java.math.BigInteger)
                                    (t/fn [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))]

                        #_(def ~'>long|__8|input-types
                          (*<> (t/and (t/isa? java.math.BigInteger)
                                      (t/fn [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))))
                        (def ~'>long|__8
                          (reify Object>long
                            (~(L 'invoke) [_## ~(O 'x)]
                              (let* [~(tag "java.math.BigInteger" 'x) ~'x] ~'(.longValue x)))))

                        #_[x ratio?]

                        #_(def ~'>long|__9|input-types
                          (*<> ratio?))
                        #_(def ~'>long|__9|conditions
                          (*<> (-> long|__8|input-types (core/get 0) utr/and-type>args (core/get 1))))
                        (def ~'>long|__9
                          (reify Object>long
                            (~(L 'invoke) [_## ~(O 'x)]
                              (let* [~(tag "clojure.lang.Ratio" 'x) ~'x]
                                ;; Resolved from `(>long (.bigIntegerValue x))`
                                ;; In this case, `(t/compare (t/type-of '(.bigIntegerValue x)) overload-type)`:
                                ;; - `(t/- tt/boolean? tt/boolean? float? double?)`  -> t/<>
                                ;; - `(t/and (t/or double? float?) ...)`         -> t/<>
                                ;; - `(t/and (t/isa? clojure.lang.BigInt) ...)`  -> t/<>
                                ;; - `(t/and (t/isa? java.math.BigInteger) ...)` -> t/>
                                ;; - `ratio?`                                    -> t/<>
                                ;; - `(t/value true)`                            -> t/<>
                                ;; - `(t/value false)`                           -> t/<>
                                ;; - `t/string?`                                    -> t/<>
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
                            (~(L 'invoke) [_## ~(B 'x)] 1)))

                        #_[x (t/value false)]

                        #_(def ~'>long|__11|input-types
                          (*<> (t/value false)))
                        (def ~'>long|__11
                          (reify boolean>long
                            (~(L 'invoke) [_## ~(B 'x)] 0)))

                        #_[x t/string?]

                        #_(def ~'>long|__12|input-types
                          (*<> t/string?))
                        (def ~'>long|__12
                          (reify Object>long
                            (~(L 'invoke) [_## ~(O 'x)]
                              ~'(Long/parseLong x))))

                        #_[x t/string?]

                        #_(def ~'>long|__13|input-types
                          (*<> t/string? tt/int?))
                        (def ~'>long|__13
                          (reify Object+int>long
                            (~(L 'invoke) [_## ~(O 'x) ~(I 'radix)]
                              ~'(Long/parseLong x radix))))

                        #_(defn >long
                          {:quantum.core.type/type
                            (t/fn
                              [(t/- tt/boolean? tt/boolean? float? double?)]
                              [(t/and (t/or t/double? t/float?)
                                      (t/fn [x (t/or double? float?)]
                                        (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))]
                              [(t/and (t/isa? clojure.lang.BigInt)
                                      (t/fn [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))]
                              [(t/and (t/isa? java.math.BigInteger)
                                      (t/fn [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))]
                              [ratio?]
                              [(t/value true)]
                              [(t/value false)]
                              [t/string?]
                              [t/string? tt/int?])}
                          ([x0##] (ifs ((Array/get >long|__0|input-types 0) x0##)
                                         (.invoke >long|__0 x0##)
                                       ((Array/get >long|__1|input-types 0) x0##)
                                         (.invoke >long|__0 x0##)
                                       ((Array/get >long|__2|input-types 0) x0##)
                                         (.invoke >long|__2 x0##)))
                          ([x0## x1##] ...)))))]
    ;; TODO fix this
    #_(testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval
        '(do (throws (>long-checked))
             (throws (>long-checked nil))
             (throws (>long-checked ""))
             (is (identical? (>long-checked  1)       (clojure.lang.RT/longCast  1)))
             (is (identical? (>long-checked  1.0)     (clojure.lang.RT/longCast  1.0)))
             (is (identical? (>long-checked  1.1)     (clojure.lang.RT/longCast  1.1)))
             (is (identical? (>long-checked -1)       (clojure.lang.RT/longCast -1)))
             (is (identical? (>long-checked -1.0)     (clojure.lang.RT/longCast -1.0)))
             (is (identical? (>long-checked -1.1)     (clojure.lang.RT/longCast -1.1)))
             (is (identical? (>long-checked (byte 1)) (clojure.lang.RT/longCast (byte 1)))))))))

(deftest test|!str
  (let [actual
          (macroexpand '
            (self/defn !str > #?(:clj  (t/isa? StringBuilder)
                                 :cljs (t/isa? StringBuffer))
                    ([] #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
                    ;; If we had combined this arity, `t/or`ing the `t/string?` means it wouldn't have been
                    ;; handled any differently than `t/char-seq?`
            #?(:clj ([x t/string?] (StringBuilder. x)))
                    ([x #?(:clj  (t/or tt/char-seq? tt/int?)
                           :cljs t/val?)]
                      #?(:clj (StringBuilder. x) :cljs (StringBuffer. x)))))
        expected
          (case (env-lang)
            :clj ($ (do (def ~'!str|__0|0
                          (reify* [>Object]
                            (~(O 'invoke) [~'_0__]
                              ~(tag "java.lang.StringBuilder" '(new StringBuilder)))))

                        (def ~(O<> '!str|__1|input0|types)
                          (*<> (t/isa? java.lang.String)))
                        (def ~'!str|__1|0
                          (reify* [Object>Object]
                            (~(O 'invoke) [~'_1__ ~(O 'x)]
                              (let* [~(ST 'x) ~'x]
                                ~(tag "java.lang.StringBuilder"
                                      (list 'new 'StringBuilder (ST 'x)))))))

                        (def ~(O<> '!str|__2|input0|types)
                          (*<> (t/isa? java.lang.CharSequence)
                               (t/isa? java.lang.Integer)))
                        (def ~'!str|__2|0
                          (reify* [Object>Object]
                            (~(O 'invoke) [~'_2__ ~(O 'x)]
                              (let* [~(tag "java.lang.CharSequence" 'x) ~'x]
                                ~(tag "java.lang.StringBuilder"
                                      (list 'new 'StringBuilder
                                            (tag "java.lang.CharSequence" 'x)))))))
                        (def ~'!str|__2|1
                          (reify* [int>Object]
                            (~(O 'invoke) [~'_3__ ~(I 'x)]
                              ~(tag "java.lang.StringBuilder" '(new StringBuilder x)))))

                        (defn ~'!str
                          {:quantum.core.type/type
                            (t/fn ~'(t/isa? StringBuilder)
                                  ~'[]
                                  ~'[t/string?]
                                  ~'[(t/or tt/char-seq? tt/int?)])}
                          ([] (.invoke ~(tag "quantum.core.test.defnt_equivalences.>Object"
                                             '!str|__0|0)))
                          ([~'x00__]
                            (ifs
                              ((Array/get ~'!str|__1|input0|types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>Object"
                                               '!str|__1|0) ~'x00__)
                              ((Array/get ~'!str|__2|input0|types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.Object>Object"
                                               '!str|__2|0) ~'x00__)
                              ((Array/get ~'!str|__2|input0|types 1) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.int>Object"
                                               '!str|__2|1) ~'x00__)
                              (unsupported! `!str [~'x00__] 0)))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval
        '(do (is (instance? StringBuilder (!str)))
             (is (instance? StringBuilder (!str "asd")))
             (is (instance? StringBuilder (!str (int 123))))
             (is (instance? StringBuilder (!str (.subSequence "abc" 0 1)))))))))

(deftest defn-reference-test
  (testing "`t/defn` references itself"
    (let [actual
            (macroexpand '
              (self/defn defn-self-reference
                ([] nil)
                ([x tt/long?] (defn-self-reference))))
          expected
            (case (env-lang)
              :clj ($ (do (declare ~'defn-self-reference)
                          (def ~'defn-self-reference|__0|0
                            (reify* [>Object]
                              (~(O 'invoke) [~'_0__] nil)))
                          (def ~(O<> 'defn-self-reference|__1|input0|types)
                            (*<> (t/isa? java.lang.Long)))
                          (def ~'defn-self-reference|__1|0
                            (reify* [long>Object]
                              (~(O 'invoke) [~'_1__ ~'x] (~'defn-self-reference))))
                          (defn ~'defn-self-reference
                            {:quantum.core.type/type
                              (t/ftype t/any? [] [tt/long?])}
                            ([] (.invoke ~'defn-self-reference|__0|0))
                            ([~'x00__]
                              (ifs
                                ((Array/get ~'defn-self-reference|__1|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `long>Object) 'defn-self-reference|__1|0)
                                         ~'x00__)
                                (unsupported! `defn-self-reference [~'x00__] 0)))))))]
      (testing "code equivalence" (is-code= actual expected))
      (testing "functionality"
        (eval actual)
        (eval '(do (is= (defn-self-reference) nil))))))
  (testing "`t/defn` references other `t/defn`"
    (let [actual
            (macroexpand '
              (self/defn defn-reference
                ([] (>long* 1))))
          expected
            (case (env-lang)
              :clj ($ (do (declare ~'defn-reference)
                          (def ~'defn-reference|__0|0
                            (reify* [>long] (~(L 'invoke) [~'_0__] ~'(>long* 1))))
                          (defn ~'defn-reference
                            {:quantum.core.type/type (t/fn t/any? [])}
                            ([] (.invoke ~(tag (cstr `>long) 'defn-reference|__0|0)))))))]
      (testing "code equivalence" (is-code= actual expected))
      (testing "functionality"
        (eval actual)
        (eval '(do (is (identical? (defn-reference) 1))))))))

(deftest defn-assume-test
  "Tests that t/assume works properly in the context of `t/defn`"
  (throws (eval '(self/defn defn-assume-0 [> (t/assume tt/int?)] "asd")))
  (throws (eval '(self/defn defn-assume-1 [> (t/assume tt/int?)] nil)))
  (is= nil (do (eval '(self/defn defn-assume-2 [> (t/assume tt/int?)] (Object.)))
               nil))
  (is= nil (do (eval '(self/defn defn-assume-3 [> (t/assume tt/int?)] (or (Object.) nil)))
               nil)))

(deftest dependent-type-test
  (testing "Combination/integration test"
    (let [actual
            (macroexpand '
              (self/defn dependent-type-combo
                #_"1. Analyze `a` = `(t/type (>long-checked \"23\"))`
                      1. Analyze `(>long-checked \"23\")`
                         -> `(t/value 23)`
                      -> Put `out` in env as `(t/value 23)`"
                [out (t/type (>long-checked "23"))]
                (self/fn dependent-type-combo-inner
                  ([a (t/or tt/boolean? (t/type b))
                    b (t/or tt/byte? (t/type d))
                    c (t/or tt/short? tt/char?)
                    d (let [b (t/- tt/char? tt/long?)]
                        (t/or tt/char? (t/type b) (t/type c)))
                    > (t/or (t/type b) (t/type d))] b)))
          expected
            (case (env-lang)
              :clj
                ($ (do ...)))]
      (testing "code equivalence" (is-code= actual expected))
      (testing "functionality"
        (eval actual)
        (eval '(do ...))))))

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'!str
       (t/fn :> #?(:clj  (t/isa? StringBuilder)
                   :cljs (t/isa? StringBuffer))
         []
 #?(:clj [t/string?])
         [#?(:clj  (t/or t/char-seq? tt/int?)
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
                   ;; `(t/or t/char-seq? tt/int?)`
                   (def ^Object>Object !str|__2 ; `t/char-seq?`
                     (reify Object>Object
                       (^java.lang.Object invoke [_# ^java.lang.Object ~'x]
                         (let* [^CharSequence x x] (StringBuilder. x)))))
                   (def ^int>Object !str|__3 ; `tt/int?`
                     (reify int>Object (^java.lang.Object invoke [_# ^int ~'x]
                       (StringBuilder. x))))

                   (defn !str ([  ] (.invoke !str|__0))
                              ([a0] (ifs (tt/string? a0)      (.invoke !str|__1 a0)
                                         (tt/char-seq? a0) (.invoke !str|__2 a0)
                                         (tt/int? a0)      (.invoke !str|__3 a0)))))
        :cljs `(do (defn !str ([]   (StringBuffer.))
                              ([a0] (let* [x a0] (StringBuffer. x)))))))

;; =====|=====|=====|=====|===== ;;

;; TODO handle inline
(macroexpand '
(self/defn #_:inline str|test > t/string?
           ([] "")
           ([x t/nil?] "")
           ;; could have inferred but there may be other objects who have overridden .toString
  #?(#_:clj  #_([x (t/isa? Object) > (t/* t/string?)] (.toString x))
           ;; Can't infer that it returns a string (without a pre-constructed list of built-in fns)
           ;; As such, must explicitly mark
     :cljs ([x t/any? > (t/assume t/string?)] (.join #js [x] "")))
           ;; TODO only one variadic arity allowed currently; theoretically could dispatch on at
           ;; least pre-variadic args, if not variadic
           ;; TODO should have automatic currying?
           ;; TODO need to handle varargs
           #_([x (t/fn> str|test t/any?) & xs (? (t/seq-of t/any?))
            #?@(:cljs [> (t/assume t/string?)])]
             (let* [sb (-> x str|test !str)] ; determined to be StringBuilder
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
                     {:quantum.core.type/type
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

;; TODO enable the disabled parts of this
(macroexpand '
(self/defn #_:inline count #_> #_t/nneg-integer?
  ([xs t/array?  #_> #_t/nneg-int?] (.length xs))
  #_([xs t/string? > #?(:clj t/nneg-int? :cljs (t/assume t/nneg-int?))]
    (#?(:clj .length :cljs .-length) xs))
  #_([xs !+vector? > t/nneg-int?] (#?(:clj count :cljs (do (TODO) 0)) xs)))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'count
       (t/fn :> t/pos-integer?
         [t/array?  :> t/nneg-int?]
         [t/string?    :> #?(:clj t/nneg-int? :cljs (t/assume t/nneg-int?))]
         [!+vector? :> t/nneg-int?]))

     ~(case-env
        :clj  `(do ;; `array?`
                   (def count|__0__1 (reify Object>int (^int invoke [_# ^java.lang.Object ~'xs] (Array/count ^"[B" xs))))
                   ...)
        :cljs `(do ...)))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(self/defn #_:inline get
  ;; TODO `t/numerically
  ([xs t/array? , k #_(t/numerically tt/int?)] (#?(:clj Array/get :cljs aget) xs k))
  ([xs t/string?, k #_(t/numerically tt/int?)] (.charAt xs k))
  ([xs !+vector?, k t/any?] #?(:clj (.valAt xs k) :cljs (TODO))))
)
;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'count
       (t/fn :> t/pos-integer?
         [t/array?  (t/numerically tt/int?)]
         [t/string? (t/numerically tt/int?)]
         [!+vector? t/any?]))

     ...)

;; =====|=====|=====|=====|===== ;;

(self/defn zero? > tt/boolean?
  ([x (t/- tt/primitive? tt/boolean?)] (Numeric/isZero x)))

; TODO CLJS version will come after
#?(:clj
(macroexpand '
(self/defn seq
  "Taken from `clojure.lang.RT/seq`"
  > (t/? (t/isa? ISeq))
  ([xs t/nil?] nil)
  ([xs (t/isa? ASeq)] xs)
  ([xs (t/or (t/isa? LazySeq) (t/isa? Seqable))] (.seq xs))
  ([xs t/iterable?] (clojure.lang.RT/chunkIteratorSeq (.iterator xs)))
  ([xs t/char-seq?] (clojure.lang.StringSeq/create xs))
  ([xs (t/isa? java.util.Map)] (seq (.entrySet xs)))
  ([xs t/array? > (t/* (t/? (t/isa? ISeq)))]
    ;; We do this only because `clojure.lang.ArraySeq/createFromObject` is private but perhaps it
    ;; would be wise from a performance perspective to bypass that with e.g. a fast version of
    ;; reflection
    (clojure.core/seq xs))))
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
                 {:quantum.core.type/type
                   (t/ftype :> (t/? (t/isa? ISeq))
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
(self/defn first
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
(self/defn next > (? ISeq)
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
  (t/ftype "seed arity"       []
           "completing arity" [t/any?]
           "reducing arity"   [t/any? t/any?]))

;; ----- expanded code ----- ;;


; ================================================ ;

(self/defn ^:inline custom
  [x (s/if double?
           (t/or (s/fnt [x ?] (> x 3)) ; uses the above-defined `>`
                 (s/fnt [x ?] (< x 0.1)))
           (t/or str? !str?))
   y ?] (str x (name y))) ; uses the above-defined `name`


;; ===== `extend-defn!` tests ===== ;;

(binding [self/*compilation-mode* :test]
  (macroexpand
    '(self/defn extensible
       ([a t/double?]))))

;; Code
(do (declare ~'extensible)

    ;; We could keep a global map of defn-symbol to mapping, but if someone deletes the namespace
    ;; the `t/defn` is interned in, that mapping should go away too.
    ;; We only show this types decl because testing/debug is on. Otherwise the macro would just
    ;; `intern` the var and define it there rather than re-evaluating the types.
    (def ~'extensible|__types-decl
      (atom [{:id 0 :arg-types [(t/isa? Double)] :output-type t/any?}]))

    (def ~'extensible|__0|types (self/types-decl>arg-types ~'extensible|__types-decl 0))
    (def ~'extensible|__0 (reify* [double>Object] (invoke [_0__ a] nil)))

    ;; Could have done `intern`+`fn*` but JS needs some special things for it to work that may
    ;; change over time
    (defn extensible
      {:quantum.core.type/type (self/types-decl>ftype extensible|__types-decl t/any?)}
      ([~'x00__]
        (ifs ((Array/get ~'extensible|__0|types 0) ~'x00__)
               (. extensible|__0 invoke x00__)
             (unsupported! `extensible [~'x00__] 0)))))

(testing "Insertion"
  (self/extend-defn! extensible
    ([a t/boolean?]))

  (do ;; We only show this types decl because testing/debug is on. Otherwise the macro would just
      ;; `swap!` the types decl outside the code rather than re-evaluating the types.
      ;; To find where to put the overload, we find the first place where the inputs are `t/<`.
      ;; TODO test that when testing/debug mode is off, it doesn't emit this code
      (reset! quantum.test.untyped.core.type.defnt/extensible|__types-decl
        [{:id 1 :arg-types [(t/isa? Boolean)] :output-type t/any?}
         {:id 0 :arg-types [(t/isa? Double)]  :output-type t/any?}])

      ;; It's labeled as `extensible|__1` but internally that's not how it's ordered; it's just
      ;; incrementing based on the size of the types-decl
      ;; Currently we can't undefine overloads which I think is fine
      (def ~'extensible|__1|types
        (self/types-decl>arg-types quantum.test.untyped.core.type.defnt/extensible|__types-decl 0))
      (def ~'extensible|__1 (reify* [boolean>Object] (invoke [_0__ a] nil)))
      ;; The dynamic dispatch is currently redefined with every `extend-defn!`
      ;; We expect that `t/defn` extension will take place in only one thread
      (intern 'quantum.test.untyped.core.type.defnt
        (with-meta 'extensible
          {:quantum.core.type/type (self/types-decl>ftype extensible|__types-decl t/any?)})
        (fn* ([~'x00__]
               (ifs ((Array/get ~'extensible|__1|types 0) ~'x00__)
                      (. extensible|__1 invoke x00__)
                    ((Array/get ~'extensible|__0|types 0) ~'x00__)
                      (. extensible|__0 invoke x00__)
                    (unsupported! `extensible [~'x00__] 0)))))))
