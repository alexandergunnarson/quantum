(ns quantum.test.untyped.core.type.defnt
  (:refer-clojure :exclude
    [> count get identity name seq some? zero?])
  (:require
    [clojure.core                           :as core]
    [quantum.core.type
      :refer [dotyped]]
    [quantum.test.untyped.core.type         :as tt]
    [quantum.untyped.core.type.defnt        :as self
      :refer [unsupported!]]
    [quantum.untyped.core.data.array
      :refer [*<> *<>|macro]]
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
    [quantum.untyped.core.type              :as t]
    [quantum.untyped.core.type.reifications :as utr]
    [quantum.untyped.core.vars
      :refer [defmeta]])
  (:import
    [clojure.lang                    ASeq ISeq LazySeq Named Reduced RT Seqable]
    [quantum.core.data               Array]
    [quantum.core                    Numeric Primitive]
    [quantum.untyped.core.type.defnt AnonFn]))

;; TODO test `:inline`

;; Just in case
(clojure.spec.test.alpha/unstrument)
(do (require '[orchestra.spec.test :as st])
    (orchestra.spec.test/unstrument)
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

(defn >B__B [form] (tag (-> 'B__B resolve str) form))
(defn >Y__Y [form] (tag (-> 'Y__Y resolve str) form))
(defn >S__S [form] (tag (-> 'S__S resolve str) form))
(defn >C__C [form] (tag (-> 'C__C resolve str) form))
(defn >I__I [form] (tag (-> 'I__I resolve str) form))
(defn >L__L [form] (tag (-> 'L__L resolve str) form))
(defn >F__F [form] (tag (-> 'F__F resolve str) form))
(defn >D__D [form] (tag (-> 'D__D resolve str) form))
(defn >O__O [form] (tag (-> 'O__O resolve str) form))

(defn cstr [x]
  (if (-> x resolve class?)
      (str x)
      (str (core/namespace x) "." (core/name x))))

(def ts (O<> 'ts__))
(def fs (O<> 'fs__))

(defn aget* [x i]   (list '. 'clojure.lang.RT 'aget x i))
(defn aset* [x i v] (list '. 'clojure.lang.RT 'aset x i v))

#?(:clj
(deftest test|pid
  (let [actual
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn pid [> (t/? t/string?)]
                (->> ^:val (java.lang.management.ManagementFactory/getRuntimeMXBean)
                           (.getName)))))
        expected
        ($ (do (declare ~'pid)
               [[0 0 false [] (t/or t/nil? t/string?)]]
               (defmeta-from ~'pid
                 (let* [~fs   (*<>|sized|macro 0)
                        ~'f__ (new TypedFn
                                {:quantum.core.type/type ...}
                                pid|__!types ; defined/created within `t/defn`
                                fs
                                (fn* ([~ts ~fs] (. ~(aget* fs 0) ~'invoke))))]
                   ~(aset* fs 0
                      `(reify* [__O]
                         (~(O 'invoke) [~'_0__]
                           ~(ST (list '.
                                  (tag "java.lang.management.RuntimeMXBean"
                                       '(. java.lang.management.ManagementFactory getRuntimeMXBean))
                                  'getName)))))
                   f))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is (t/string? (pid)))
                 (throws (pid 1))))))))

(deftest test|identity
  (let [actual
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn ^:inline identity ([x t/any? > (t/type x) #_"TODO TYPED (t/== x)"] x))))
        expected
          (case (env-lang)
            :clj
            ($ (do (declare ~'identity)

                   ;; [x t/any?]

                   (def ~(>B__B 'identity|__0)
                     (reify* [B__B] (~(B 'invoke) [~'_0__ ~(B 'x)] ~'x)))
                   (def ~(>Y__Y 'identity|__1)
                     (reify* [Y__Y] (~(Y 'invoke) [~'_1__ ~(Y 'x)] ~'x)))
                   (def ~(>S__S 'identity|__2)
                     (reify* [S__S] (~(S 'invoke) [~'_2__ ~(S 'x)] ~'x)))
                   (def ~(>C__C 'identity|__3)
                     (reify* [C__C] (~(C 'invoke) [~'_3__ ~(C 'x)] ~'x)))
                   (def ~(>I__I 'identity|__4)
                     (reify* [I__I] (~(I 'invoke) [~'_4__ ~(I 'x)] ~'x)))
                   (def ~(>L__L 'identity|__5)
                     (reify* [L__L] (~(L 'invoke) [~'_5__ ~(L 'x)] ~'x)))
                   (def ~(>F__F 'identity|__6)
                     (reify* [F__F] (~(F 'invoke) [~'_6__ ~(F 'x)] ~'x)))
                   (def ~(>D__D 'identity|__7)
                     (reify* [D__D] (~(D 'invoke) [~'_7__ ~(D 'x)] ~'x)))
                   (def ~(>O__O 'identity|__8)
                     (reify* [O__O] (~(O 'invoke) [~'_8__ ~(O 'x)] ~(O 'x))))

                   [[0 0 true [t/boolean?] t/boolean?]
                    [1 1 true [t/byte?]    t/byte?]
                    [2 2 true [t/short?]   t/short?]
                    [3 3 true [t/char?]    t/char?]
                    [4 4 true [t/int?]     t/int?]
                    [5 5 true [t/long?]    t/long?]
                    [6 6 true [t/float?]   t/float?]
                    [7 7 true [t/double?]  t/double?]
                    [8 8 true [t/any?]     t/any?]]

                   (defmeta ~'identity
                     {:quantum.core.type/type identity|__type}
                     (fn* ([~'x00__]
                            (ifs
                               ((Array/get identity|__0|types 0) ~'x00__)
                                 (. identity|__0 ~'invoke ~'x00__)
                               ((Array/get identity|__1|types 0) ~'x00__)
                                 (. identity|__1 ~'invoke ~'x00__)
                               ((Array/get identity|__2|types 0) ~'x00__)
                                 (. identity|__2 ~'invoke ~'x00__)
                               ((Array/get identity|__3|types 0) ~'x00__)
                                 (. identity|__3 ~'invoke ~'x00__)
                               ((Array/get identity|__4|types 0) ~'x00__)
                                 (. identity|__4 ~'invoke ~'x00__)
                               ((Array/get identity|__5|types 0) ~'x00__)
                                 (. identity|__5 ~'invoke ~'x00__)
                               ((Array/get identity|__6|types 0) ~'x00__)
                                 (. identity|__6 ~'invoke ~'x00__)
                               ((Array/get identity|__7|types 0) ~'x00__)
                                 (. identity|__7 ~'invoke ~'x00__)
                               ((Array/get identity|__8|types 0) ~'x00__)
                                 (. identity|__8 ~'invoke ~'x00__)
                                 ;; TODO no need for `unsupported!` because it will always get a valid
                                 ;; branch
                                 (unsupported! `identity [~'x00__] 0)))))))
            :cljs
            ;; Direct dispatch will be simple functions, not `reify`s
            ($ (do (defn ~'identity [~'x] ~'x))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality"
      (eval actual)
      (eval '(do (is= (identity 1)  (dotyped (identity 1))  (core/identity 1))
                 (is= (identity "") (dotyped (identity "")) (core/identity "")))))))

(deftest test|name
  (let [actual
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn #_:inline name > t/string?
                         ([x t/string?] x)
                #?(:clj  ([x (t/isa? Named)  > (t/run t/string?)] (.getName x))
                   :cljs ([x (t/isa? INamed) > (t/run t/string?)] (-name x))))))
        expected
        (case (env-lang)
          :clj
          ($ (do (declare ~'name)

                 ;; [x t/string?]

                 (def ~(>O__O 'name|__0)
                   (reify* [O__O] (~(O 'invoke) [~'_0__ ~(O 'x)] ~(ST 'x))))

                 ;; [x (t/isa? Named)] > (t/run t/string?)

                 (def ~(>O__O 'name|__1)
                   (reify* [O__O]
                     (~(O 'invoke) [~'_1__ ~(O 'x)]
                       (t/validate ~(ST (list '. (tag "clojure.lang.Named" 'x) 'getName))
                                   ~'(t/run t/string?)))))

                 [{:id 0 :index 0 :arg-types [(t/isa? String)] :output-type (t/isa? String)}
                  {:id 1 :index 1 :arg-types [(t/isa? Named)]  :output-type (t/run (t/isa? String))}]

                 (defmeta ~'name
                   {:quantum.core.type/type name|__type}
                   (fn* ([~'x00__]
                          (ifs ((Array/get name|__0|types 0) ~'x00__) (. name|__0 ~'invoke ~'x00__)
                               ((Array/get name|__1|types 0) ~'x00__) (. name|__1 ~'invoke ~'x00__)
                               (unsupported! `name [~'x00__] 0)))))))
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
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn #_:inline some? > t/boolean?
                ([x t/nil?] false)
                ;; Implicitly, `(- t/any? t/nil?)`, so `t/val?`
                ([x t/any?] true))))
        expected
        (case (env-lang)
          :clj
          ($ (do (declare ~'some?)

                 ;; [x t/nil?]

                 (def ~(tag (cstr `O__B) 'some?|__0)
                   (reify* [O__B] (~(B 'invoke) [~'_0__ ~(O 'x)] false)))

                 ;; [x t/any?]

                 (def ~(>B__B 'some?|__1)
                   (reify* [B__B] (~(B 'invoke) [~'_1__ ~(B 'x)] true)))
                 (def ~(>Y__B 'some?|__2)
                   (reify* [Y__B] (~(B 'invoke) [~'_2__ ~(Y 'x)] true)))
                 (def ~(>S__B 'some?|__3)
                   (reify* [S__B] (~(B 'invoke) [~'_3__ ~(S 'x)] true)))
                 (def ~(>C__B 'some?|__4)
                   (reify* [C__B] (~(B 'invoke) [~'_4__ ~(C 'x)] true)))
                 (def ~(>I__B 'some?|__5)
                   (reify* [I__B] (~(B 'invoke) [~'_5__ ~(I 'x)] true)))
                 (def ~(>L__B 'some?|__6)
                   (reify* [L__B] (~(B 'invoke) [~'_6__ ~(L 'x)] true)))
                 (def ~(>F__B 'some?|__7)
                   (reify* [F__B] (~(B 'invoke) [~'_7__ ~(F 'x)] true)))
                 (def ~(>D__B 'some?|__8)
                   (reify* [D__B] (~(B 'invoke) [~'_8__ ~(D 'x)] true)))
                 (def ~(>O__B 'some?|__9)
                   (reify* [O__B] (~(B 'invoke) [~'_9__ ~(O 'x)] true)))

                 [{:id 0 :index 0 :arg-types [(t/value nil)]      :output-type (t/isa? Boolean)}
                  {:id 1 :index 1 :arg-types [(t/isa? Boolean)]   :output-type (t/isa? Boolean)}
                  {:id 2 :index 2 :arg-types [(t/isa? Byte)]      :output-type (t/isa? Boolean)}
                  {:id 3 :index 3 :arg-types [(t/isa? Short)]     :output-type (t/isa? Boolean)}
                  {:id 4 :index 4 :arg-types [(t/isa? Character)] :output-type (t/isa? Boolean)}
                  {:id 5 :index 5 :arg-types [(t/isa? Integer)]   :output-type (t/isa? Boolean)}
                  {:id 6 :index 6 :arg-types [(t/isa? Long)]      :output-type (t/isa? Boolean)}
                  {:id 7 :index 7 :arg-types [(t/isa? Float)]     :output-type (t/isa? Boolean)}
                  {:id 8 :index 8 :arg-types [(t/isa? Double)]    :output-type (t/isa? Boolean)}
                  {:id 9 :index 9 :arg-types [t/any?]             :output-type (t/isa? Boolean)}]

                 (defmeta ~'some?
                   {:quantum.core.type/type some?|__type}
                   (fn*
                     ([~'x00__]
                       (ifs ((Array/get some?|__0|types 0) ~'x00__) (. some?|__0 ~'invoke ~'x00__)
                            ;; TODO eliminate these checks below because they're not needed
                            ((Array/get some?|__1|types 0) ~'x00__) (. some?|__1 ~'invoke ~'x00__)
                            ((Array/get some?|__2|types 0) ~'x00__) (. some?|__2 ~'invoke ~'x00__)
                            ((Array/get some?|__3|types 0) ~'x00__) (. some?|__3 ~'invoke ~'x00__)
                            ((Array/get some?|__4|types 0) ~'x00__) (. some?|__4 ~'invoke ~'x00__)
                            ((Array/get some?|__5|types 0) ~'x00__) (. some?|__5 ~'invoke ~'x00__)
                            ((Array/get some?|__6|types 0) ~'x00__) (. some?|__6 ~'invoke ~'x00__)
                            ((Array/get some?|__7|types 0) ~'x00__) (. some?|__7 ~'invoke ~'x00__)
                            ((Array/get some?|__8|types 0) ~'x00__) (. some?|__8 ~'invoke ~'x00__)
                            ((Array/get some?|__9|types 0) ~'x00__) (. some?|__9 ~'invoke ~'x00__)
                            (unsupported! `some? [~'x00__] 0)))))))
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

                     (def ~'reduced?|test|__0|0
                       (reify* [O__B]
                         (~(B 'invoke) [~'_0__ ~(O 'x)]
                           (let* [~(tag "clojure.lang.Reduced" 'x) ~'x] true))))

                     ;; [x t/any?]

                     (def ~'reduced?|test|__1|0
                       (reify* [O__B B__B Y__B S__B C__B I__B L__B F__B D__B]
                         (~(B 'invoke) [~'_1__ ~(O 'x)] false)
                         (~(B 'invoke) [~'_2__ ~(B 'x)] false)
                         (~(B 'invoke) [~'_3__ ~(Y 'x)] false)
                         (~(B 'invoke) [~'_4__ ~(S 'x)] false)
                         (~(B 'invoke) [~'_5__ ~(C 'x)] false)
                         (~(B 'invoke) [~'_6__ ~(I 'x)] false)
                         (~(B 'invoke) [~'_7__ ~(L 'x)] false)
                         (~(B 'invoke) [~'_8__ ~(F 'x)] false)
                         (~(B 'invoke) [~'_9__ ~(D 'x)] false)))

                     (defmeta ~'reduced?|test
                       {:quantum.core.type/type
                         (t/fn t/any?
                               ~'[(t/isa? Reduced)]
                               ~'[t/any?])}
                       (fn* ([~'x00__]
                         (ifs ((Array/get ~'reduced?|test|__0|input0|types 0) ~'x00__)
                                (.invoke reduced?|test|__0|0 ~'x00__)
                              ;; TODO eliminate this check because it's not needed (`t/any?`)
                              ((Array/get ~'reduced?|test|__1|input0|types 0) ~'x00__)
                                (.invoke reduced?|test|__1|0 ~'x00__)
                              (unsupported! `reduced?|test [~'x00__] 0)))))))
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
                       (reify* [B__B]
                         (~(B 'invoke) [~'_0__  ~(B 'x)] ~'x)))

                     ;; [x t/nil? -> (- t/nil? tt/boolean?)]

                     (def ~(O<> '>boolean|__1|input0|types)
                       (*<> (t/value nil)))
                     (def ~'>boolean|__1|0
                       (reify* [O__B]
                         (~(B 'invoke) [~'_1__  ~(O 'x)] false)))

                     ;; [x t/any? -> (- t/any? t/nil? tt/boolean?)]

                     (def ~(O<> '>boolean|__2|input0|types)
                       (*<> t/any?))
                     (def ~'>boolean|__2|0
                       (reify* [O__B B__B Y__B S__B C__B I__B L__B F__B D__B]
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
                                (.invoke ~(tag (cstr `B__B) '>boolean|__0|0) ~'x00__)
                              ((Array/get ~'>boolean|__1|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `O__B)  '>boolean|__1|0) ~'x00__)
                              ;; TODO eliminate this check because it's not needed (`t/any?`)
                              ((Array/get ~'>boolean|__2|input0|types 0) ~'x00__)
                                (.invoke ~(tag (cstr `O__B)  '>boolean|__2|0) ~'x00__)
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
                   (*<> (t/ref (t/isa? Number))))
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
(def >|types-form
  ($ [{:id 0  :index 0  :arg-types [(t/isa? Byte)      (t/isa? Byte)     ]
       :output-type (t/isa? Boolean)}
      {:id 1  :index 1  :arg-types [(t/isa? Byte)      (t/isa? Short)    ]
       :output-type (t/isa? Boolean)}
      {:id 2  :index 2  :arg-types [(t/isa? Byte)      (t/isa? Character)]
       :output-type (t/isa? Boolean)}
      {:id 3  :index 3  :arg-types [(t/isa? Byte)      (t/isa? Integer)  ]
       :output-type (t/isa? Boolean)}
      {:id 4  :index 4  :arg-types [(t/isa? Byte)      (t/isa? Long)     ]
       :output-type (t/isa? Boolean)}
      {:id 5  :index 5  :arg-types [(t/isa? Byte)      (t/isa? Float)    ]
       :output-type (t/isa? Boolean)}
      {:id 6  :index 6  :arg-types [(t/isa? Byte)      (t/isa? Double)   ]
       :output-type (t/isa? Boolean)}
      {:id 7  :index 7  :arg-types [(t/isa? Short)     (t/isa? Byte)     ]
       :output-type (t/isa? Boolean)}
      {:id 8  :index 8  :arg-types [(t/isa? Short)     (t/isa? Short)    ]
       :output-type (t/isa? Boolean)}
      {:id 9  :index 9  :arg-types [(t/isa? Short)     (t/isa? Character)]
       :output-type (t/isa? Boolean)}
      {:id 10 :index 10 :arg-types [(t/isa? Short)     (t/isa? Integer)  ]
       :output-type (t/isa? Boolean)}
      {:id 11 :index 11 :arg-types [(t/isa? Short)     (t/isa? Long)     ]
       :output-type (t/isa? Boolean)}
      {:id 12 :index 12 :arg-types [(t/isa? Short)     (t/isa? Float)    ]
       :output-type (t/isa? Boolean)}
      {:id 13 :index 13 :arg-types [(t/isa? Short)     (t/isa? Double)   ]
       :output-type (t/isa? Boolean)}
      {:id 14 :index 14 :arg-types [(t/isa? Character) (t/isa? Byte)     ]
       :output-type (t/isa? Boolean)}
      {:id 15 :index 15 :arg-types [(t/isa? Character) (t/isa? Short)    ]
       :output-type (t/isa? Boolean)}
      {:id 16 :index 16 :arg-types [(t/isa? Character) (t/isa? Character)]
       :output-type (t/isa? Boolean)}
      {:id 17 :index 17 :arg-types [(t/isa? Character) (t/isa? Integer)  ]
       :output-type (t/isa? Boolean)}
      {:id 18 :index 18 :arg-types [(t/isa? Character) (t/isa? Long)     ]
       :output-type (t/isa? Boolean)}
      {:id 19 :index 19 :arg-types [(t/isa? Character) (t/isa? Float)    ]
       :output-type (t/isa? Boolean)}
      {:id 20 :index 20 :arg-types [(t/isa? Character) (t/isa? Double)   ]
       :output-type (t/isa? Boolean)}
      {:id 21 :index 21 :arg-types [(t/isa? Integer)   (t/isa? Byte)     ]
       :output-type (t/isa? Boolean)}
      {:id 22 :index 22 :arg-types [(t/isa? Integer)   (t/isa? Short)    ]
       :output-type (t/isa? Boolean)}
      {:id 23 :index 23 :arg-types [(t/isa? Integer)   (t/isa? Character)]
       :output-type (t/isa? Boolean)}
      {:id 24 :index 24 :arg-types [(t/isa? Integer)   (t/isa? Integer)  ]
       :output-type (t/isa? Boolean)}
      {:id 25 :index 25 :arg-types [(t/isa? Integer)   (t/isa? Long)     ]
       :output-type (t/isa? Boolean)}
      {:id 26 :index 26 :arg-types [(t/isa? Integer)   (t/isa? Float)    ]
       :output-type (t/isa? Boolean)}
      {:id 27 :index 27 :arg-types [(t/isa? Integer)   (t/isa? Double)   ]
       :output-type (t/isa? Boolean)}
      {:id 28 :index 28 :arg-types [(t/isa? Long)      (t/isa? Byte)     ]
       :output-type (t/isa? Boolean)}
      {:id 29 :index 29 :arg-types [(t/isa? Long)      (t/isa? Short)    ]
       :output-type (t/isa? Boolean)}
      {:id 30 :index 30 :arg-types [(t/isa? Long)      (t/isa? Character)]
       :output-type (t/isa? Boolean)}
      {:id 31 :index 31 :arg-types [(t/isa? Long)      (t/isa? Integer)  ]
       :output-type (t/isa? Boolean)}
      {:id 32 :index 32 :arg-types [(t/isa? Long)      (t/isa? Long)     ]
       :output-type (t/isa? Boolean)}
      {:id 33 :index 33 :arg-types [(t/isa? Long)      (t/isa? Float)    ]
       :output-type (t/isa? Boolean)}
      {:id 34 :index 34 :arg-types [(t/isa? Long)      (t/isa? Double)   ]
       :output-type (t/isa? Boolean)}
      {:id 35 :index 35 :arg-types [(t/isa? Float)     (t/isa? Byte)     ]
       :output-type (t/isa? Boolean)}
      {:id 36 :index 36 :arg-types [(t/isa? Float)     (t/isa? Short)    ]
       :output-type (t/isa? Boolean)}
      {:id 37 :index 37 :arg-types [(t/isa? Float)     (t/isa? Character)]
       :output-type (t/isa? Boolean)}
      {:id 38 :index 38 :arg-types [(t/isa? Float)     (t/isa? Integer)  ]
       :output-type (t/isa? Boolean)}
      {:id 39 :index 39 :arg-types [(t/isa? Float)     (t/isa? Long)     ]
       :output-type (t/isa? Boolean)}
      {:id 40 :index 40 :arg-types [(t/isa? Float)     (t/isa? Float)    ]
       :output-type (t/isa? Boolean)}
      {:id 41 :index 41 :arg-types [(t/isa? Float)     (t/isa? Double)   ]
       :output-type (t/isa? Boolean)}
      {:id 42 :index 42 :arg-types [(t/isa? Double)    (t/isa? Byte)     ]
       :output-type (t/isa? Boolean)}
      {:id 43 :index 43 :arg-types [(t/isa? Double)    (t/isa? Short)    ]
       :output-type (t/isa? Boolean)}
      {:id 44 :index 44 :arg-types [(t/isa? Double)    (t/isa? Character)]
       :output-type (t/isa? Boolean)}
      {:id 45 :index 45 :arg-types [(t/isa? Double)    (t/isa? Integer)  ]
       :output-type (t/isa? Boolean)}
      {:id 46 :index 46 :arg-types [(t/isa? Double)    (t/isa? Long)     ]
       :output-type (t/isa? Boolean)}
      {:id 47 :index 47 :arg-types [(t/isa? Double)    (t/isa? Float)    ]
       :output-type (t/isa? Boolean)}
      {:id 48 :index 48 :arg-types [(t/isa? Double)    (t/isa? Double)   ]
       :output-type (t/isa? Boolean)}]))

(def >|dynamic-dispatch-form
  ($ (defmeta ~'>
       {:quantum.core.type/type >|__type}
       (fn* ([~'x00__ ~'x10__]
              (ifs
                ((Array/get >|__0|types 0) ~'x00__)
                  (ifs
                    ((Array/get >|__0|types  1) ~'x10__)
                      (. >|__0  ~'invoke  ~'x00__ ~'x10__)
                    ((Array/get >|__1|types  1) ~'x10__)
                      (. >|__1  ~'invoke  ~'x00__ ~'x10__)
                    ((Array/get >|__2|types  1) ~'x10__)
                      (. >|__2  ~'invoke  ~'x00__ ~'x10__)
                    ((Array/get >|__3|types  1) ~'x10__)
                      (. >|__3  ~'invoke  ~'x00__ ~'x10__)
                    ((Array/get >|__4|types  1) ~'x10__)
                      (. >|__4  ~'invoke  ~'x00__ ~'x10__)
                    ((Array/get >|__5|types  1) ~'x10__)
                      (. >|__5  ~'invoke  ~'x00__ ~'x10__)
                    ((Array/get >|__6|types  1) ~'x10__)
                      (. >|__6  ~'invoke  ~'x00__ ~'x10__)
                    (unsupported! `> [~'x00__ ~'x10__] 1))
                ((Array/get >|__7|types 0) ~'x00__)
                  (ifs
                    ((Array/get >|__7|types  1) ~'x10__)
                      (. >|__7  ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__8|types  1) ~'x10__)
                      (. >|__8  ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__9|types  1) ~'x10__)
                      (. >|__9  ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__10|types 1) ~'x10__)
                      (. >|__10 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__11|types 1) ~'x10__)
                      (. >|__11 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__12|types 1) ~'x10__)
                      (. >|__12 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__13|types 1) ~'x10__)
                      (. >|__13 ~'invoke ~'x00__ ~'x10__)
                    (unsupported! `> [~'x00__ ~'x10__] 1))
                ((Array/get >|__14|types 0) ~'x00__)
                  (ifs
                    ((Array/get >|__14|types 1) ~'x10__)
                      (. >|__14 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__15|types 1) ~'x10__)
                      (. >|__15 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__16|types 1) ~'x10__)
                      (. >|__16 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__17|types 1) ~'x10__)
                      (. >|__17 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__18|types 1) ~'x10__)
                      (. >|__18 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__19|types 1) ~'x10__)
                      (. >|__19 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__20|types 1) ~'x10__)
                      (. >|__20 ~'invoke ~'x00__ ~'x10__)
                    (unsupported! `> [~'x00__ ~'x10__] 1))
                ((Array/get >|__21|types 0) ~'x00__)
                  (ifs
                    ((Array/get >|__21|types 1) ~'x10__)
                      (. >|__21 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__22|types 1) ~'x10__)
                      (. >|__22 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__23|types 1) ~'x10__)
                      (. >|__23 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__24|types 1) ~'x10__)
                      (. >|__24 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__25|types 1) ~'x10__)
                      (. >|__25 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__26|types 1) ~'x10__)
                      (. >|__26 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__27|types 1) ~'x10__)
                      (. >|__27 ~'invoke ~'x00__ ~'x10__)
                    (unsupported! `> [~'x00__ ~'x10__] 1))
                ((Array/get >|__28|types 0) ~'x00__)
                  (ifs
                    ((Array/get >|__28|types 1) ~'x10__)
                      (. >|__28 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__29|types 1) ~'x10__)
                      (. >|__29 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__30|types 1) ~'x10__)
                      (. >|__30 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__31|types 1) ~'x10__)
                      (. >|__31 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__32|types 1) ~'x10__)
                      (. >|__32 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__33|types 1) ~'x10__)
                      (. >|__33 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__34|types 1) ~'x10__)
                      (. >|__34 ~'invoke ~'x00__ ~'x10__)
                    (unsupported! `> [~'x00__ ~'x10__] 1))
                ((Array/get >|__35|types 0) ~'x00__)
                  (ifs
                    ((Array/get >|__35|types 1) ~'x10__)
                      (. >|__35 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__36|types 1) ~'x10__)
                      (. >|__36 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__37|types 1) ~'x10__)
                      (. >|__37 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__38|types 1) ~'x10__)
                      (. >|__38 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__39|types 1) ~'x10__)
                      (. >|__39 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__40|types 1) ~'x10__)
                      (. >|__40 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__41|types 1) ~'x10__)
                      (. >|__41 ~'invoke ~'x00__ ~'x10__)
                    (unsupported! `> [~'x00__ ~'x10__] 1))
                ((Array/get >|__42|types 0) ~'x00__)
                  (ifs
                    ((Array/get >|__42|types 1) ~'x10__)
                      (. >|__42 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__43|types 1) ~'x10__)
                      (. >|__43 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__44|types 1) ~'x10__)
                      (. >|__44 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__45|types 1) ~'x10__)
                      (. >|__45 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__46|types 1) ~'x10__)
                      (. >|__46 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__47|types 1) ~'x10__)
                      (. >|__47 ~'invoke ~'x00__ ~'x10__)
                    ((Array/get >|__48|types 1) ~'x10__)
                      (. >|__48 ~'invoke ~'x00__ ~'x10__)
                    (unsupported! `> [~'x00__ ~'x10__] 1))
                (unsupported! `> [~'x00__ ~'x10__] 0)))))))

(deftest test|>
  (let [actual
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn #_:inline > > tt/boolean?
           #?(:clj  ([a tt/comparable-primitive? b tt/comparable-primitive? > tt/boolean?]
                      (Numeric/gt a b))
              :cljs ([a tt/double?               b tt/double?               > (t/assume tt/boolean?)]
                      (cljs.core/> a b))))))
        expected
        (case (env-lang)
          :clj
          ($ (do (declare ~'>)

                 ;; [a t/comparable-primitive? b t/comparable-primitive? > tt/boolean?]

                 (def ~(tag (cstr `byte+Y__B) '>|__0)
                   (reify* [byte+Y__B]
                     (~(B 'invoke) [~'_0__  ~(Y 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `byte+S__B) '>|__1)
                   (reify* [byte+S__B]
                     (~(B 'invoke) [~'_1__  ~(Y 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `byte+C__B) '>|__2)
                   (reify* [byte+C__B]
                     (~(B 'invoke) [~'_2__  ~(Y 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `byte+I__B) '>|__3)
                   (reify* [byte+I__B]
                     (~(B 'invoke) [~'_3__  ~(Y 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `byte+L__B) '>|__4)
                   (reify* [byte+L__B]
                     (~(B 'invoke) [~'_4__  ~(Y 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `byte+F__B) '>|__5)
                   (reify* [byte+F__B]
                     (~(B 'invoke) [~'_5__  ~(Y 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `byte+D__B) '>|__6)
                   (reify* [byte+D__B]
                     (~(B 'invoke) [~'_6__  ~(Y 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `short+Y__B) '>|__7)
                   (reify* [short+Y__B]
                     (~(B 'invoke) [~'_7__  ~(S 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `short+S__B) '>|__8)
                   (reify* [short+S__B]
                     (~(B 'invoke) [~'_8__  ~(S 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `short+C__B) '>|__9)
                   (reify* [short+C__B]
                     (~(B 'invoke) [~'_9__  ~(S 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `short+I__B) '>|__10)
                   (reify* [short+I__B]
                     (~(B 'invoke) [~'_10__ ~(S 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `short+L__B) '>|__11)
                   (reify* [short+L__B]
                     (~(B 'invoke) [~'_11__ ~(S 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `short+F__B) '>|__12)
                   (reify* [short+F__B]
                     (~(B 'invoke) [~'_12__ ~(S 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `short+D__B) '>|__13)
                   (reify* [short+D__B]
                     (~(B 'invoke) [~'_13__ ~(S 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `char+Y__B) '>|__14)
                   (reify* [char+Y__B]
                     (~(B 'invoke) [~'_14__ ~(C 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `char+S__B) '>|__15)
                   (reify* [char+S__B]
                     (~(B 'invoke) [~'_15__ ~(C 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `char+C__B) '>|__16)
                   (reify* [char+C__B]
                     (~(B 'invoke) [~'_16__ ~(C 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `char+I__B) '>|__17)
                   (reify* [char+I__B]
                     (~(B 'invoke) [~'_17__ ~(C 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `char+L__B) '>|__18)
                   (reify* [char+L__B]
                     (~(B 'invoke) [~'_18__ ~(C 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `char+F__B) '>|__19)
                   (reify* [char+F__B]
                     (~(B 'invoke) [~'_19__ ~(C 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `char+D__B) '>|__20)
                   (reify* [char+D__B]
                     (~(B 'invoke) [~'_20__ ~(C 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `int+Y__B) '>|__21)
                   (reify* [int+Y__B]
                     (~(B 'invoke) [~'_21__ ~(I 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `int+S__B) '>|__22)
                   (reify* [int+S__B]
                     (~(B 'invoke) [~'_22__ ~(I 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `int+C__B) '>|__23)
                   (reify* [int+C__B]
                     (~(B 'invoke) [~'_23__ ~(I 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `int+I__B) '>|__24)
                   (reify* [int+I__B]
                     (~(B 'invoke) [~'_24__ ~(I 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `int+L__B) '>|__25)
                   (reify* [int+L__B]
                     (~(B 'invoke) [~'_25__ ~(I 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `int+F__B) '>|__26)
                   (reify* [int+F__B]
                     (~(B 'invoke) [~'_26__ ~(I 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `int+D__B) '>|__27)
                   (reify* [int+D__B]
                     (~(B 'invoke) [~'_27__ ~(I 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `long+Y__B) '>|__28)
                   (reify* [long+Y__B]
                     (~(B 'invoke) [~'_28__ ~(L 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `long+S__B) '>|__29)
                   (reify* [long+S__B]
                     (~(B 'invoke) [~'_29__ ~(L 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `long+C__B) '>|__30)
                   (reify* [long+C__B]
                     (~(B 'invoke) [~'_30__ ~(L 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `long+I__B) '>|__31)
                   (reify* [long+I__B]
                     (~(B 'invoke) [~'_31__ ~(L 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `long+L__B) '>|__32)
                   (reify* [long+L__B]
                     (~(B 'invoke) [~'_32__ ~(L 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `long+F__B) '>|__33)
                   (reify* [long+F__B]
                     (~(B 'invoke) [~'_33__ ~(L 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `long+D__B) '>|__34)
                   (reify* [long+D__B]
                     (~(B 'invoke) [~'_34__ ~(L 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `float+Y__B) '>|__35)
                   (reify* [float+Y__B]
                     (~(B 'invoke) [~'_35__ ~(F 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `float+S__B) '>|__36)
                   (reify* [float+S__B]
                     (~(B 'invoke) [~'_36__ ~(F 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `float+C__B) '>|__37)
                   (reify* [float+C__B]
                     (~(B 'invoke) [~'_37__ ~(F 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `float+I__B) '>|__38)
                   (reify* [float+I__B]
                     (~(B 'invoke) [~'_38__ ~(F 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `float+L__B) '>|__39)
                   (reify* [float+L__B]
                     (~(B 'invoke) [~'_39__ ~(F 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `float+F__B) '>|__40)
                   (reify* [float+F__B]
                     (~(B 'invoke) [~'_40__ ~(F 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `float+D__B) '>|__41)
                   (reify* [float+D__B]
                     (~(B 'invoke) [~'_41__ ~(F 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `double+Y__B) '>|__42)
                   (reify* [double+Y__B]
                     (~(B 'invoke) [~'_42__ ~(D 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `double+S__B) '>|__43)
                   (reify* [double+S__B]
                     (~(B 'invoke) [~'_43__ ~(D 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `double+C__B) '>|__44)
                   (reify* [double+C__B]
                     (~(B 'invoke) [~'_44__ ~(D 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `double+I__B) '>|__45)
                   (reify* [double+I__B]
                     (~(B 'invoke) [~'_45__ ~(D 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `double+L__B) '>|__46)
                   (reify* [double+L__B]
                     (~(B 'invoke) [~'_46__ ~(D 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `double+F__B) '>|__47)
                   (reify* [double+F__B]
                     (~(B 'invoke) [~'_47__ ~(D 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `double+D__B) '>|__48)
                   (reify* [double+D__B]
                     (~(B 'invoke) [~'_48__ ~(D 'a) ~(D 'b)] ~'(. Numeric gt a b))))

                 ~>|types-form
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
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn ^:inline >long*
                {:source "clojure.lang.RT.uncheckedLongCast"}
                > tt/long?
                ([x (t/- tt/primitive? tt/boolean?)] (Primitive/uncheckedLongCast x))
                ([x (t/ref (t/isa? Number))] (.longValue x)))))
        expected
          (case (env-lang)
            :clj ($ (do (declare ~'>long*)

                        ;; [x (t/- tt/primitive? tt/boolean?)]

                        (def ~(tag (cstr `byte>long) '>long*|__0)
                          (reify* [byte>long]
                            (~(L 'invoke) [~'_0__ ~(Y             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `short>long) '>long*|__1)
                          (reify* [short>long]
                            (~(L 'invoke) [~'_1__ ~(S            'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `char>long) '>long*|__2)
                          (reify* [char>long]
                            (~(L 'invoke) [~'_2__ ~(C             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `int>long) '>long*|__3)
                          (reify* [int>long]
                            (~(L 'invoke) [~'_3__ ~(I              'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `long>long) '>long*|__4)
                          (reify* [long>long]
                            (~(L 'invoke) [~'_4__ ~(L             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `float>long) '>long*|__5)
                          (reify* [float>long]
                            (~(L 'invoke) [~'_5__ ~(F            'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `double>long) '>long*|__6)
                          (reify* [double>long]
                            (~(L 'invoke) [~'_6__ ~(D           'x)]
                              ~'(. Primitive uncheckedLongCast x))))

                        ;; [x (t/ref (t/isa? Number))]

                        (def ~(tag (cstr `Object>long) '>long*|__7)
                          (reify* [Object>long]
                            (~(L 'invoke) [~'_7__ ~(O 'x)]
                              (. ~(tag "java.lang.Number" 'x) ~'longValue))))

                        [[0 0 true [t/byte?]                 t/long?]
                         [1 1 true [t/short?]                t/long?]
                         [2 2 true [t/char?]                 t/long?]
                         [3 3 true [t/int?]                  t/long?]
                         [4 4 true [t/long?]                 t/long?]
                         [5 5 true [t/float?]                t/long?]
                         [6 6 true [t/double?]               t/long?]
                         [7 7 true [(t/ref (t/isa? Number))] t/long?]]

                        (defmeta ~'>long*
                          {:source "clojure.lang.RT.uncheckedLongCast"
                           :quantum.core.type/type >long*|__type}
                          (fn* ([~'x00__]
                            (ifs
                              ((Array/get >long*|__0|types 0) ~'x00__)
                                (. >long*|__0 ~'invoke ~'x00__)
                              ((Array/get >long*|__1|types 0) ~'x00__)
                                (. >long*|__1 ~'invoke ~'x00__)
                              ((Array/get >long*|__2|types 0) ~'x00__)
                                (. >long*|__2 ~'invoke ~'x00__)
                              ((Array/get >long*|__3|types 0) ~'x00__)
                                (. >long*|__3 ~'invoke ~'x00__)
                              ((Array/get >long*|__4|types 0) ~'x00__)
                                (. >long*|__4 ~'invoke ~'x00__)
                              ((Array/get >long*|__5|types 0) ~'x00__)
                                (. >long*|__5 ~'invoke ~'x00__)
                              ((Array/get >long*|__6|types 0) ~'x00__)
                                (. >long*|__6 ~'invoke ~'x00__)
                              ((Array/get >long*|__7|types 0) ~'x00__)
                                (. >long*|__7 ~'invoke ~'x00__)
                              (unsupported! `>long* [~'x00__] 0))))))))]
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
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn ref-output-type
                ([x tt/boolean? > (t/ref tt/boolean?)] (Boolean. x))
                ([x tt/byte?    > (t/ref tt/byte?)]    (Byte.    x)))))
        expected
          ($ (do (declare ~'ref-output-type)

                 ;; [x tt/boolean? > (t/ref tt/boolean?)]

                 (def ~(tag (cstr `boolean>Object) 'ref-output-type|__0)
                   (reify* [boolean>Object] (~(O 'invoke) [~'_0__ ~(B 'x)] (new ~'Boolean ~'x))))

                 ;; [x tt/byte? > (t/ref tt/byte?)]

                 (def ~(tag (cstr `byte>Object) 'ref-output-type|__1)
                   (reify* [byte>Object] (~(O 'invoke) [~'_1__ ~(Y 'x)] (new ~'Byte ~'x))))

                 [[0 0 nil [(t/isa? Boolean)] (t/ref (t/isa? Boolean))]
                  [1 1 nil [(t/isa? Byte)]    (t/ref (t/isa? Byte))]]

                 (defmeta ~'ref-output-type
                   {:quantum.core.type/type ref-output-type|__type}
                   (fn* ([~'x00__]
                          (ifs
                            ((Array/get ref-output-type|__0|types 0) ~'x00__)
                              (. ref-output-type|__0 ~'invoke ~'x00__)
                            ((Array/get ref-output-type|__1|types 0) ~'x00__)
                              (. ref-output-type|__1 ~'invoke ~'x00__)
                            (unsupported! `ref-output-type [~'x00__] 0)))))))]
    (testing "code equivalence" (is-code= actual expected))
    (testing "functionality" (eval actual)))))

(self/defn >big-integer > (t/isa? java.math.BigInteger)
  ([x tt/ratio? > (t/run (t/isa? java.math.BigInteger))] (.bigIntegerValue x)))

;; NOTE would use `>long` but that's already an interface
(deftest test|>long-checked
  (let [actual
          (binding [self/*compilation-mode* :test]
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
                               ;; FIXME it doesn't know what `>long-checked`'s type is i.e. what it
                               ;; has defined so far
                ([x tt/ratio?] 5 (-> x >big-integer >long-checked))
                ([x (t/value true)]  1)
                ([x (t/value false)] 0)
                ([x t/string?] (Long/parseLong x))
                ([x t/string?, radix tt/int?] (Long/parseLong x radix)))))
        expected
          (case (env-lang)
            :clj ($ (do #_[x (t/- tt/boolean? tt/boolean? float? double?)]

                        #_(def ~'>long|__0|input-types (*<> byte?))
                        (def ~'>long|__0
                          (reify byte>long
                            (~(L 'invoke) [_## ~(Y 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__0 invoke ~'x))))

                        #_(def ~'>long|__1|input-types (*<> short?))
                        (def ~'>long|__1
                          (reify short>long
                            (~(L 'invoke) [_## ~(C 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__1 invoke ~'x))))

                        #_(def ~'>long|__2|input-types (*<> char?))
                        (def ~'>long|__2
                          (reify char>long
                            (~(L 'invoke) [_## ~(S 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__2 invoke ~'x))))

                        #_(def ~'>long|__3|input-types (*<> tt/int?))
                        (def ~'>long|__3
                          (reify int>long
                            (~(L 'invoke) [_## ~(I 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__3 invoke ~'x))))

                        #_(def ~'>long|__4|input-types (*<> tt/long?))
                        (def ~'>long|__4
                          (reify long>long
                            (~(L 'invoke) [_## ~(L 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__4 invoke ~'x))))

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
                              (. >long*|__6 invoke ~'x))))

                        #_(def ~'>long|__6|input-types
                          (*<> (t/and t/float?
                                      (t/fn [x (t/or double? float?)]
                                        (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))))
                        (def ~'>long|__6
                          (reify float>long
                            (~(L 'invoke) [_## ~(F 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__6 invoke ~'x))))

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
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn !str > #?(:clj  (t/isa? StringBuilder)
                                   :cljs (t/isa? StringBuffer))
                      ([] #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
                      ;; If we had combined this arity, `t/or`ing the `t/string?` means it wouldn't have been
                      ;; handled any differently than `t/char-seq?`
              #?(:clj ([x t/string?] (StringBuilder. x)))
                      ([x #?(:clj  (t/or tt/char-seq? tt/int?)
                             :cljs t/val?)]
                        #?(:clj (StringBuilder. x) :cljs (StringBuffer. x))))))
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
            (binding [self/*compilation-mode* :test]
              (macroexpand '
                (self/defn defn-self-reference
                  ([> tt/double?] 2.0)
                  ([x tt/long? > tt/double?] (defn-self-reference)))))
          expected
            (case (env-lang)
              :clj ($ (do (declare ~'defn-self-reference)

                          ;; [> tt/double?]

                          (def ~'defn-self-reference|__0
                            (reify* [>double]
                              (~(O 'invoke) [~'_0__] 2.0)))

                          ;; [x tt/long? > tt/double?]

                          (def ~'defn-self-reference|__1
                            (reify* [long>double]
                              (~(O 'invoke) [~'_1__ ~'x] (~'defn-self-reference))))

                          [{:id 0 :index 0 :arg-types []             :output-type (t/isa? Double)}
                           {:id 1 :index 1 :arg-types [(t/isa? Long) :output-type (t/isa? Double)]}]

                          (defmeta ~'defn-self-reference
                            {:quantum.core.type/type defn-self-reference|__type}
                            ([] (. ~'defn-self-reference|__0 invoke))
                            ([~'x00__]
                              (ifs
                                ((Array/get ~'defn-self-reference|__1|types 0) ~'x00__)
                                  (. defn-self-reference|__1 invoke ~'x00__)
                                (unsupported! `defn-self-reference [~'x00__] 0)))))))]
      (testing "code equivalence" (is-code= actual expected))
      (testing "functionality"
        (eval actual)
        (eval '(do (is= (defn-self-reference) 2.0))))))
  (testing "`t/defn` references other `t/defn`"
    (let [actual
            (binding [self/*compilation-mode* :test]
              (macroexpand '
                (self/defn defn-reference
                  ([> tt/long?] (>long* 1)))))
          expected
            (case (env-lang)
              :clj ($ (do (declare ~'defn-reference)
                          (def ~(tag (cstr `>long) 'defn-reference|__0)
                            (reify* [>long] (~(L 'invoke) [~'_0__] ~'(>long* 1))))

                          [{:id 0 :index 0 :arg-types [] :output-type (t/isa? Long)}]

                          (defmeta ~'defn-reference
                            {:quantum.core.type/type defn-reference|__type}
                            (fn* ([] (. defn-reference|__0 ~'invoke)))))))]
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
  (testing "t/type") ;; tested in `extend-defn!` test
  (testing "t/input-type"
    (let [actual
            (macroexpand '
              (self/defn input-type-test
                [> (t/output-type >long-checked [t/string?])] 1))
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
  #?(#_:clj  #_([x (t/isa? Object) > (t/run t/string?)] (.toString x))
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
  ([xs t/array? > (t/run (t/? (t/isa? ISeq)))]
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
                   (t/ftype (t/? (t/isa? ISeq))
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

(def dependent-extensible|direct-dispatch|codelist
 `[(def ~(tag (cstr `boolean+byte+short+short>Object)     'dependent-extensible|__0)
      (reify* [boolean+byte+short+short>Object]
        (~(O 'invoke) [~'_0__  ~(B 'a) ~(Y 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `boolean+byte+short+char>Object)      'dependent-extensible|__1)
     (reify* [boolean+byte+short+char>Object]
       (~(O 'invoke) [~'_1__  ~(B 'a) ~(Y 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `boolean+byte+short+Object>Object)    'dependent-extensible|__2)
     (reify* [boolean+byte+short+Object>Object]
       (~(O 'invoke) [~'_2__  ~(B 'a) ~(Y 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `boolean+byte+Object+char>Object)     'dependent-extensible|__3)
     (reify* [boolean+byte+Object+char>Object]
       (~(O 'invoke) [~'_3__  ~(B 'a) ~(Y 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `boolean+byte+Object+Object>Object)   'dependent-extensible|__4)
     (reify* [boolean+byte+Object+Object>Object]
       (~(O 'invoke) [~'_4__  ~(B 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `boolean+byte+Object+Object>Object)   'dependent-extensible|__5)
     (reify* [boolean+byte+Object+Object>Object]
       (~(O 'invoke) [~'_5__  ~(B 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `boolean+short+short+short>Object)    'dependent-extensible|__6)
     (reify* [boolean+short+short+short>Object]
       (~(O 'invoke) [~'_6__  ~(B 'a) ~(S 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `boolean+char+short+char>Object)      'dependent-extensible|__7)
     (reify* [boolean+char+short+char>Object]
       (~(O 'invoke) [~'_7__  ~(B 'a) ~(C 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `boolean+char+Object+char>Object)     'dependent-extensible|__8)
     (reify* [boolean+char+Object+char>Object]
       (~(O 'invoke) [~'_8__  ~(B 'a) ~(C 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `boolean+Object+short+Object>Object)  'dependent-extensible|__9)
     (reify* [boolean+Object+short+Object>Object]
       (~(O 'invoke) [~'_9__ ~(B 'a) ~(O 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `boolean+Object+Object+Object>Object) 'dependent-extensible|__10)
     (reify* [boolean+Object+Object+Object>Object]
       (~(O 'invoke) [~'_10__ ~(B 'a) ~(O 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `boolean+Object+Object+Object>Object) 'dependent-extensible|__11)
     (reify* [boolean+Object+Object+Object>Object]
       (~(O 'invoke) [~'_11__ ~(B 'a) ~(O 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `byte+byte+short+short>Object)        'dependent-extensible|__12)
     (reify* [byte+byte+short+short>Object]
       (~(O 'invoke) [~'_12__ ~(Y 'a) ~(Y 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `byte+byte+short+char>Object)         'dependent-extensible|__13)
     (reify* [byte+byte+short+char>Object]
       (~(O 'invoke) [~'_13__ ~(Y 'a) ~(Y 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `byte+byte+short+Object>Object)       'dependent-extensible|__14)
     (reify* [byte+byte+short+Object>Object]
       (~(O 'invoke) [~'_14__ ~(Y 'a) ~(Y 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `byte+byte+Object+char>Object)        'dependent-extensible|__15)
     (reify* [byte+byte+Object+char>Object]
       (~(O 'invoke) [~'_15__ ~(Y 'a) ~(Y 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `byte+byte+Object+Object>Object)      'dependent-extensible|__16)
     (reify* [byte+byte+Object+Object>Object]
       (~(O 'invoke) [~'_16__ ~(Y 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `byte+byte+Object+Object>Object)      'dependent-extensible|__17)
     (reify* [byte+byte+Object+Object>Object]
       (~(O 'invoke) [~'_17__ ~(Y 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `short+short+short+short>Object)      'dependent-extensible|__18)
     (reify* [short+short+short+short>Object]
       (~(O 'invoke) [~'_18__ ~(S 'a) ~(S 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `char+char+short+char>Object)         'dependent-extensible|__19)
     (reify* [char+char+short+char>Object]
       (~(O 'invoke) [~'_19__ ~(C 'a) ~(C 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `char+char+Object+char>Object)        'dependent-extensible|__20)
     (reify* [char+char+Object+char>Object]
       (~(O 'invoke) [~'_20__ ~(C 'a) ~(C 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `Object+Object+short+Object>Object)   'dependent-extensible|__21)
     (reify* [Object+Object+short+Object>Object]
       (~(O 'invoke) [~'_21__ ~(O 'a) ~(O 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `Object+Object+Object+Object>Object)  'dependent-extensible|__22)
     (reify* [Object+Object+Object+Object>Object]
       (~(O 'invoke) [~'_22__ ~(O 'a) ~(O 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `Object+Object+Object+Object>Object)  'dependent-extensible|__23)
     (reify* [Object+Object+Object+Object>Object]
       (~(O 'invoke) [~'_23__ ~(O 'a) ~(O 'b) ~(O 'c) ~(O 'd)] 1)))])

(def dependent-extensible|fn|form
 `(fn* ([~'x00__ ~'x10__ ~'x20__ ~'x30__]
    (ifs ((Array/get dependent-extensible|__0|types 0) ~'x00__)
         (ifs ((Array/get dependent-extensible|__0|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__0|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__0|types 3) ~'x30__)
                       (. dependent-extensible|__0 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__1|types 3) ~'x30__)
                       (. dependent-extensible|__1 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__2|types 3) ~'x30__)
                       (. dependent-extensible|__2 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  ((Array/get dependent-extensible|__3|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__3|types 3) ~'x30__)
                       (. dependent-extensible|__3 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__4|types 3) ~'x30__)
                       (. dependent-extensible|__4 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__5|types 3) ~'x30__)
                       (. dependent-extensible|__5 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             ((Array/get dependent-extensible|__6|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__6|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__6|types 3) ~'x30__)
                       (. dependent-extensible|__6 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             ((Array/get dependent-extensible|__7|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__7|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__7|types 3) ~'x30__)
                       (. dependent-extensible|__7 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  ((Array/get dependent-extensible|__8|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__8|types 3) ~'x30__)
                       (. dependent-extensible|__8 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             ((Array/get dependent-extensible|__9|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__9|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__9|types 3) ~'x30__)
                       (. dependent-extensible|__9 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  ((Array/get dependent-extensible|__10|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__10|types 3) ~'x30__)
                       (. dependent-extensible|__10 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             ((Array/get dependent-extensible|__11|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__11|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__11|types 3) ~'x30__)
                       (. dependent-extensible|__11 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 1))
        ((Array/get dependent-extensible|__12|types 0) ~'x00__)
        (ifs ((Array/get dependent-extensible|__12|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__12|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__12|types 3) ~'x30__)
                       (. dependent-extensible|__12 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__13|types 3) ~'x30__)
                       (. dependent-extensible|__13 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__14|types 3) ~'x30__)
                       (. dependent-extensible|__14 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  ((Array/get dependent-extensible|__15|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__15|types 3) ~'x30__)
                       (. dependent-extensible|__15 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__16|types 3) ~'x30__)
                       (. dependent-extensible|__16 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       ((Array/get dependent-extensible|__17|types 3) ~'x30__)
                       (. dependent-extensible|__17 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 1))
        ((Array/get dependent-extensible|__18|types 0) ~'x00__)
        (ifs ((Array/get dependent-extensible|__18|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__18|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__18|types 3) ~'x30__)
                       (. dependent-extensible|__18 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 1))
        ((Array/get dependent-extensible|__19|types 0) ~'x00__)
        (ifs ((Array/get dependent-extensible|__19|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__19|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__19|types 3) ~'x30__)
                       (. dependent-extensible|__19 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  ((Array/get dependent-extensible|__20|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__20|types 3) ~'x30__)
                       (. dependent-extensible|__20 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
         (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 1))
        ((Array/get dependent-extensible|__21|types 0) ~'x00__)
        (ifs ((Array/get dependent-extensible|__21|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__21|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__21|types 3) ~'x30__)
                       (. dependent-extensible|__21 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  ((Array/get dependent-extensible|__22|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__22|types 3) ~'x30__)
                       (. dependent-extensible|__22 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 1))
        ((Array/get dependent-extensible|__23|types 0) ~'x00__)
        (ifs ((Array/get dependent-extensible|__23|types 1) ~'x10__)
             (ifs ((Array/get dependent-extensible|__23|types 2) ~'x20__)
                  (ifs ((Array/get dependent-extensible|__23|types 3) ~'x30__)
                       (. dependent-extensible|__23 ~'invoke ~'x00__ ~'x10__ ~'x20__ ~'x30__)
                       (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 3))
                  (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 2))
             (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 1))
        (unsupported! `dependent-extensible [~'x00__ ~'x10__ ~'x20__ ~'x30__] 0)))))

(deftest extend-defn!|test
  (testing "simple test"
    (testing "definition"
      (let [actual
              (binding [self/*compilation-mode* :test]
                (macroexpand '
                  (self/defn extensible
                    ([a t/double?]))))
            expected
              (case (env-lang)
                :clj ($ (do (declare ~'extensible)
                            (def ~(tag (cstr `double>Object) 'extensible|__0)
                              (reify* [double>Object] (~(O 'invoke) [~'_0__ ~(D 'a)] nil)))

                            [{:id 0 :index 0 :arg-types [(t/isa? Double)] :output-type t/any?}]

                            (defmeta ~'extensible
                              {:quantum.core.type/type extensible|__type}
                              (fn* ([~'x00__]
                                     (ifs ((Array/get extensible|__0|types 0) ~'x00__)
                                            (. extensible|__0 ~'invoke ~'x00__)
                                          (unsupported! `extensible [~'x00__] 0))))))))]
        (testing "code equivalence" (is-code= actual expected))
        (eval actual)))
    (testing "extension"
      (let [actual
              (binding [self/*compilation-mode* :test]
                (macroexpand '
                  (self/extend-defn! extensible
                    ([a t/boolean?]))))
            expected
              (case (env-lang)
                :clj ($ (do (def ~(tag (cstr `boolean>Object) 'extensible|__1)
                              (reify* [boolean>Object]
                                (~(O 'invoke) [~'_0__ ~(B 'a)] nil)))

                            [{:id 1 :index 0 :arg-types [(t/isa? Boolean)] :output-type t/any?}
                             {:id 0 :index 1 :arg-types [(t/isa? Double)]  :output-type t/any?}]

                            (doto (intern '~(ns-name *ns*) '~'extensible
                                    ~(with-meta
                                       `(fn* ([~'x00__]
                                            (ifs ((Array/get extensible|__1|types 0) ~'x00__)
                                                   (. extensible|__1 ~'invoke ~'x00__)
                                                 ((Array/get extensible|__0|types 0) ~'x00__)
                                                   (. extensible|__0 ~'invoke ~'x00__)
                                                 (unsupported! `extensible [~'x00__] 0))))
                                       `{:quantum.core.type/type extensible|__type}))
                                  (alter-meta! merge
                                    {:quantum.core.type/type extensible|__type})))))]
        (testing "code equivalence" (is-code= actual expected))
        (eval actual)))
    (testing "re-extension"
      ;; TODO figure out whether we just want to have nothing happen, or whether we want to
      ;;      re-evaluate
      (let [actual
              (binding [self/*compilation-mode* :test]
                (macroexpand '
                  (self/extend-defn! extensible ([a t/boolean?]))))
            expected
              (case (env-lang)
                :clj ($ (do [{:id 1 :index 0 :arg-types [(t/isa? Boolean)] :output-type t/any?}
                             {:id 0 :index 1 :arg-types [(t/isa? Double)]  :output-type t/any?}]

                            (doto (intern '~(ns-name *ns*) '~'extensible
                                    ~(with-meta
                                       `(fn* ([~'x00__]
                                            (ifs ((Array/get extensible|__1|types 0) ~'x00__)
                                                   (. extensible|__1 ~'invoke ~'x00__)
                                                 ((Array/get extensible|__0|types 0) ~'x00__)
                                                   (. extensible|__0 ~'invoke ~'x00__)
                                                 (unsupported! `extensible [~'x00__] 0))))
                                       `{:quantum.core.type/type extensible|__type}))
                                  (alter-meta! merge
                                    {:quantum.core.type/type extensible|__type})))))]
        (testing "code equivalence" (is-code= actual expected))
        (eval actual))))
  (testing "dependent type"
    (testing "definition"
      (let [actual
              (doto (binding [self/*compilation-mode* :test]
                      (macroexpand '
                        (self/defn dependent-extensible
                          [a (t/or tt/boolean? (t/type b))
                           b (t/or tt/byte? (t/type d))
                           c (t/or tt/short? tt/string?)
                           d (let [b (t/- tt/int? tt/long?)]
                               (t/or tt/char? (t/type b) (t/type c)))
                           > (t/or (t/type b) (t/type d) tt/long?)] 1)))
                    eval)
            expected
              (case (env-lang)
                :clj
                ($ (do (declare ~'dependent-extensible)
                       ~@dependent-extensible|direct-dispatch|codelist
                 [{:id 0 :index 0
                   :arg-types   [(t/isa? Boolean) (t/isa? Byte) (t/isa? Short) (t/isa? Short)]
                   :output-type (t/or (t/isa? Byte) (t/isa? Short) (t/isa? Long))}
                  {:id 1 :index 1
                   :arg-types   [(t/isa? Boolean) (t/isa? Byte) (t/isa? Short) (t/isa? Character)]
                   :output-type (t/or (t/isa? Byte) (t/isa? Character) (t/isa? Long))}
                  {:id 2 :index 2
                   :arg-types   [(t/isa? Boolean) (t/isa? Byte) (t/isa? Short)
                                 (t/value (t/isa? Integer))]
                   :output-type (t/or (t/isa? Byte) (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 3 :index 3
                   :output-type (t/or (t/isa? Byte) (t/isa? Character) (t/isa? Long))
                   :arg-types   [(t/isa? Boolean) (t/isa? Byte) (t/isa? String) (t/isa? Character)]}
                  {:id 4 :index 4
                   :arg-types   [(t/isa? Boolean) (t/isa? Byte) (t/isa? String)
                                 (t/value (t/isa? Integer))]
                   :output-type (t/or (t/isa? Byte) (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 5 :index 5
                   :arg-types   [(t/isa? Boolean) (t/isa? Byte) (t/isa? String) (t/isa? String)]
                   :output-type (t/or (t/isa? Byte) (t/isa? String) (t/isa? Long))}
                  {:id 6 :index 6
                   :arg-types   [(t/isa? Boolean) (t/isa? Short) (t/isa? Short) (t/isa? Short)]
                   :output-type (t/or (t/isa? Short) (t/isa? Long))}
                  {:id 7 :index 7
                   :arg-types   [(t/isa? Boolean) (t/isa? Character) (t/isa? Short)
                                 (t/isa? Character)]
                   :output-type (t/or (t/isa? Character) (t/isa? Long))}
                  {:id 8 :index 8
                   :arg-types   [(t/isa? Boolean) (t/isa? Character) (t/isa? String)
                                 (t/isa? Character)]
                   :output-type (t/or (t/isa? Character) (t/isa? Long))}
                  {:id 9 :index 9
                   :arg-types   [(t/isa? Boolean) (t/value (t/isa? Integer)) (t/isa? Short)
                                 (t/value (t/isa? Integer))]
                   :output-type (t/or (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 10 :index 10
                   :arg-types   [(t/isa? Boolean) (t/value (t/isa? Integer)) (t/isa? String)
                                  (t/value (t/isa? Integer))]
                   :output-type (t/or (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 11 :index 11
                   :arg-types   [(t/isa? Boolean) (t/isa? String) (t/isa? String) (t/isa? String)]
                   :output-type (t/or (t/isa? String) (t/isa? Long))}
                  {:id 12 :index 12
                   :arg-types   [(t/isa? Byte) (t/isa? Byte) (t/isa? Short) (t/isa? Short)]
                   :output-type (t/or (t/isa? Byte) (t/isa? Short) (t/isa? Long))}
                  {:id 13 :index 13
                   :arg-types   [(t/isa? Byte) (t/isa? Byte) (t/isa? Short) (t/isa? Character)]
                   :output-type (t/or (t/isa? Byte) (t/isa? Character) (t/isa? Long))}
                  {:id 14 :index 14
                   :arg-types   [(t/isa? Byte) (t/isa? Byte) (t/isa? Short)
                                 (t/value (t/isa? Integer))]
                   :output-type (t/or (t/isa? Byte) (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 15 :index 15
                   :arg-types   [(t/isa? Byte) (t/isa? Byte) (t/isa? String) (t/isa? Character)]
                   :output-type (t/or (t/isa? Byte) (t/isa? Character) (t/isa? Long))}
                  {:id 16 :index 16
                   :arg-types   [(t/isa? Byte) (t/isa? Byte) (t/isa? String)
                                 (t/value (t/isa? Integer))]
                   :output-type (t/or (t/isa? Byte) (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 17 :index 17
                   :arg-types   [(t/isa? Byte) (t/isa? Byte) (t/isa? String) (t/isa? String)]
                   :output-type (t/or (t/isa? Byte) (t/isa? String) (t/isa? Long))}
                  {:id 18 :index 18
                   :arg-types   [(t/isa? Short) (t/isa? Short) (t/isa? Short) (t/isa? Short)]
                   :output-type (t/or (t/isa? Short) (t/isa? Long))}
                  {:id 19 :index 19
                   :arg-types   [(t/isa? Character) (t/isa? Character) (t/isa? Short)
                                 (t/isa? Character)]
                   :output-type (t/or (t/isa? Character) (t/isa? Long))}
                  {:id 20 :index 20
                   :arg-types   [(t/isa? Character) (t/isa? Character) (t/isa? String)
                                 (t/isa? Character)]
                   :output-type (t/or (t/isa? Character) (t/isa? Long))}
                  {:id 21 :index 21
                   :arg-types   [(t/value (t/isa? Integer)) (t/value (t/isa? Integer))
                                 (t/isa? Short) (t/value (t/isa? Integer))]
                   :output-type (t/or (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 22 :index 22
                   :arg-types   [(t/value (t/isa? Integer)) (t/value (t/isa? Integer))
                                 (t/isa? String) (t/value (t/isa? Integer))]
                   :output-type (t/or (t/value (t/isa? Integer)) (t/isa? Long))}
                  {:id 23 :index 23
                   :arg-types   [(t/isa? String) (t/isa? String) (t/isa? String) (t/isa? String)]
                   :output-type (t/or (t/isa? String) (t/isa? Long))}]
                 (defmeta ~'dependent-extensible
                   {:quantum.core.type/type dependent-extensible|__type}
                   ~dependent-extensible|fn|form))))]
        (testing "code equivalence" (is-code= actual expected)))))
  (testing "reactive type"
    (testing "definition"
      (let [actual
              (doto (binding [self/*compilation-mode* :test]
                      [(macroexpand '
                         (self/defn simple-reactive-dependee ([a tt/char?] 1)))
                       (macroexpand '
                         (self/defn simple-reactive-dependent
                           ([a (t/input-type simple-reactive-dependee :?)] "abc")))])
                    eval)
            expected
              (case (env-lang)
                :clj ($ [(do (declare ~'simple-reactive-dependee)
                             (def ~(tag (cstr `char>Object) 'simple-reactive-dependee|__0)
                               (reify* [char>Object] (~(O 'invoke) [~'_0__ ~(C 'a)] 1)))
                             [{:id 0 :index 0 :arg-types [(t/isa? Character)] :output-type t/any?}]
                             (defmeta ~'simple-reactive-dependee
                               {:quantum.core.type/type simple-reactive-dependee|__type}
                               (fn* ([~'x00__]
                                 (ifs ((Array/get simple-reactive-dependee|__0|types 0) ~'x00__)
                                      (. simple-reactive-dependee|__0 ~'invoke ~'x00__)
                                      (unsupported! `simple-reactive-dependee [~'x00__] 0))))))
                         (do (declare ~'simple-reactive-dependent)
                             (def ~(tag (cstr `char>Object) 'simple-reactive-dependent|__0)
                               (reify* [char>Object] (~(O 'invoke) [~'_0__ ~(C 'a)] "abc")))
                             [{:id 0 :index 0 :arg-types [(t/isa? Character)] :output-type t/any?}]
                             (defmeta ~'simple-reactive-dependent
                               {:quantum.core.type/type simple-reactive-dependent|__type}
                               (fn* ([~'x00__]
                                 (ifs ((Array/get simple-reactive-dependent|__0|types 0) ~'x00__)
                                      (. simple-reactive-dependent|__0 ~'invoke ~'x00__)
                                      (unsupported! `simple-reactive-dependent [~'x00__] 0))))))]))]
        (testing "code equivalence" (is-code= actual expected))))
    (testing "advanced (with dependent types) definition"
      (let [actual
              (binding [self/*compilation-mode* :test]
                (macroexpand '
                  (self/defn reactive-extensible
                    ([a (t/input-type dependent-extensible :?)]))))])))
  ;; TODO make this into an actual test
  (doto (macroexpand '(self/extend-defn! dependent-extensible ([] 5)))
        eval))

;; ===== Reactive types ===== ;;

([a (t/or tt/boolean? (t/type b))
  b (t/or tt/byte? (t/type d))
  c (t/or tt/short? tt/string?)
  d (let [b (t/- tt/int? tt/long?)]
      (t/or tt/char? (t/type b) (t/type c)))
  > (t/or (t/type b) (t/type d))])
->
;; Imagine this with `let`s, essentially — reference sharing. This is just written out
;; This is just to capture what will require type-recomputation
;; We will still have to analyze the arglist and body every time; we leave as an enhancement a
;; clever way to avoid reanalyzing the arglist every time. It seems that we will have to analyze the
;; body every time though, at least to some extent.
  ([a (t/rx (t/or tt/boolean?
              (t/arglist-type ; t/arglist-type is always t/><
                (t/or tt/byte?
                      (t/arglist-type
                        (t/or tt/char?
                              (t/- @(t/input-type* abcde :?) tt/long?)
                              (t/arglist-type (t/or tt/short? tt/string?))))))))
    b (t/rx (t/or tt/byte?
              (t/arglist-type
                (t/or tt/char?
                      (t/- @(t/input-type* abcde :?) tt/long?)
                      (t/arglist-type (t/or tt/short? tt/string?))))))
    c (t/or tt/short? tt/string?)
    d (t/rx (t/or tt/char?
                  (t/- @(t/input-type* abcde :?) tt/long?)
                  (t/arglist-type (t/or tt/short? tt/string?))))
    > (t/rx (t/or (t/arglist-type
                    (t/or tt/byte?
                          (t/arglist-type
                            (t/or tt/char?
                                  (t/- @(t/input-type* abcde :?) tt/long?)
                                  (t/arglist-type (t/or tt/short? tt/string?))))))
                  (t/or tt/char?
                        (t/- @(t/input-type* abcde :?) tt/long?)
                        (t/arglist-type (t/or tt/short? tt/string?)))))]))

- Suppose you have:
  - (defn- expand-rx-types [rx-types]
      (->> rx-types
           (c/map+ (fn [{:keys [arg-types out-type]}]
                     {:arg-types (mapv ?deref arg-types) :out-type (?deref out-type)}))
           expand-types))
  - overloads>overload-types
    - Shouldn't re-analyze
    - Attaches `:overload` for newly analyzed overloads
  - (defn- overload-queue-interceptor [_ _ oldv newv]
      (let [first-new-id (count oldv)]
        (->> newv
             (uc/map (fn [x]
                       (if (-> x :id (>= first-new-id))
                           (do (alist-conj! defnt/overload-queue (:overload x))
                               ;; to save memory
                               (dissoc x :overload))
                           x))))))
  - (t/defn abcde [a t/int? > t/long?] ...) ; in `ns0`
    - Resulting in `abcde`'s compile-time-emission code (assuming no :test mode) as:
      - (do ;; These are append-only
            ;; TODO need to analyze these bodies in the proper context. Thus can't do `t/defn`
            ;; (properly) unless in most "exterior" part of namespace
            ;; Yes we could drop `:body` for non-reactive, non-inline overloads but it's fine for
            ;; now; we will optimize later after correctness is achieved
            ;; Needs to maintain previous fully-derefed version so `overloads>type-decl` knows which
            ;; reactive overloads have changed
            (intern 'ns0 'abcde|__bases ; CLJS compiler needs this to perform analysis
              (rx/! {:norx-prev nil
                     :current
                      [{:ns                'ns0
                        :args-form         nil
                        :arg-types-basis   [t/int?]
                        :output-type-form  nil
                        :output-type-basis t/long?
                        :body              [...]
                        :dependent?        false
                        :reactive?         false}]}))
            ;; Will not re-analyze overload basis if it is identical (`=`?) to the previous version
            ;; of that overload (when derefing everything)
            ;; Must include the :body and :defined-in-ns of each one in order to analyze and create
            ;; new overloads when putting on the overload queue
            ;; Internally rx-derefs reactive overloads
            ;; CLJS compiler needs this to perform analysis
            (intern 'ns0 'abcde|__types
              (doto (!rx (overload-bases>type-decl @ns0/abcde|__bases))
                    (uref/add-interceptor! :overload-queue overload-queue-interceptor)
                    urx/norx-deref))
            (intern 'ns0 'abcde|__type
              (let [out-type t/any?]
                (t/rx (type-data>ftype @ns0/abcde|__types (?deref out-type)))))
            ;; Each of these types should be completely unreactive.
            (when (= lang :clj)
              (intern 'ns0 'abcde|__types|0
                (overload-types>arg-types (rx/norx-deref ns0/abcde|__types) 0))))
    - Resulting in `abcde`'s runtime-emission code in CLJ as:
      - (do (def abcde|__0 (reify* [int>long] (invoke ([x00__ a] ...))))
            (defn abcde [x00__]
              (ifs ((Array/get ns0/abcde|__types|0 0) x00__) ...
                   (unsupported! ...))))
    - Resulting in `abcde`'s runtime-emission code in CLJS (assuming runtime type data elision i.e.
      no type decl or reactivity etc.) as:
      - (do (def abcde|__types|0 (array t/int?))
            (def abcde|__0 (do (deftype* A [] nil (extend-type A Object (invoke ([x00__ a] ...))))
                               (new A)))
            (defn abcde [x00__]
              (ifs ((aget ns0/abcde|__types|0 0) x00__) ...
                   (unsupported! ...))))
  - (t/defn fghij ; in `ns1`
      ([b t/string? > (t/type b)] ...)
      ([c (t/input-type ns0/abcde :?) > (t/output-type ns0/abcde (t/type c))] ...))
    - Resulting in `fghij`'s compile-time-emission code (assuming no :test mode) as:
      - (do (intern 'ns1 'fghij|__bases
              (rx/! {:norx-prev nil
                     :current
                       [(let [t0 t/string?]
                          {:ns                'ns1
                           :args-form         nil
                           :arg-types-basis   [t0]
                           :output-type|form  nil
                           :output-type|basis t0
                           ;; used for inline, and reactive, but could be nil'ed out once it's used
                           ;; by `overload-bases>type-decl`
                           :body-codelist     [...]
                           :dependent?        true
                           :reactive?         false})
                        (let [t0 (t/rx (t/input-type* @ns0/abcde|__type :?))]
                          {:ns                'ns1
                           ;; This is only present when there is at least one dependent type in the
                           ;; arglist / output
                           :args-form         (om 'c '(t/input-type ns0/abcde :?))
                           :arg-types-basis   [t0]
                           ;; This is only present when there is at least one dependent type in the
                           ;; arglist / output
                           :output-type|form  '(t/output-type ns0/abcde (t/type c))
                           :output-type|basis (t/rx (t/output-type* @ns0/abcde|__type @t0))
                           :body-codelist     [...]
                           :dependent?        true
                           :reactive?         true})]}))
            (intern 'ns1 'fghij|__types
              (doto (!rx (overload-bases>type-decl @ns1/fghij|__bases))
                    (uref/add-interceptor! :overload-queue overload-queue-interceptor)
                    urx/norx-deref))
            (intern 'ns1 'fghij|__type
              (let [out-type t/any?]
                (t/rx (type-data>ftype @ns1/fghij|__types (?deref out-type)))))
            ;; Consuming the overload queue in `direct-dispatch`
            (when (= lang :clj)
              (intern 'ns1 'fghij|__types|0
                (overload-types>arg-types (rx/norx-deref ns1/fghij|__types) 0))))
    - Resulting in `fghij`'s runtime-emission code in CLJ as:
      - (do (def fghij|__0 (reify* [int>long]      (invoke ([x00__ b] ...))))
            (def fghij|__1 (reify* [Object>Object] (invoke ([x00__ c] ...))))
            (defn fghij [x00__]
              (ifs ((Array/get ns0/fghij|__types|0 0) x00__) (. ns0/fghij|__0 invoke x00__)
                   (unsupported! ...))))
    - Resulting in `abcde`'s runtime-emission code in CLJS (assuming runtime type data elision i.e.
      no type decl or reactivity etc.) as:
      - (do (def fghij|__types|0 (array t/int?))
            (def fghij|__0 (do (deftype* B [] nil (extend-type A Object (invoke ([x00__ b] ...))))
                               (new B)))
            (def fghij|__1 (do (deftype* C [] nil (extend-type A Object (invoke ([x00__ c] ...))))
                               (new C)))
            (defn fghij [x00__]
              (ifs ((aget ns1/fghij|__types|0 0) x00__) (. ns1/fghij|__0 invoke x00__)
                   (unsupported! ...))))
  - (t/extend-defn! abcde [d t/byte? > t/char?] ...) ; in `ns2`
    - Resulting in `abcde`'s compile-time-emission code (assuming no :test mode) as:
      - (do (uref/update! ns0/abcde|__bases
              (fn [overloads]
                {:norx-prev overloads
                 :current
                   (join overloads
                     [{:ns            ns2
                       :arg-types     [t/byte?]
                       :output-type   t/char?
                       :body-codelist [...]
                       :dependent?    false
                       :reactive?     false}])}))
            ;; Not explicitly executed, but this is what happens reactively as
            ;; `abcde|__bases` is `update!`ed:
            ;; Reactively due to `abcde|__bases` changing
            (rx-set! ns0/abcde|__types
              [{:id 1 :ns 'ns2 :arg-types [t/byte?] :output-type t/char? :body [...]}
               {:id 0 :ns 'ns0 :arg-types [t/int?]  :output-type t/long? :body [...]}])
            ;; Reactively in `:overload-queue` watch on `abcde|__types`
            (alist-conj! defnt/overload-queue
              ['ns0/abcde {:id 1 :ns 'ns2 :arg-types [t/byte?] :output-type t/char? :body [...]}])
            ;; Reactively due to `abcde|__types` changing
            (rx-set! ns0/abcde|__type (ftype t/any? [t/byte? :> t/char?] [t/int? :> t/long?]))
            ;; Reactively due to `abcde|__type` changing
            (rx-set! ns1/fghij|__types
              [{:id 2 :ns 'ns1 :arg-types [t/byte?]   :output-type t/char?   :body [...]}
               {:id 0 :ns 'ns1 :arg-types [t/int?]    :output-type t/long?   :body [...]}
               {:id 1 :ns 'ns1 :arg-types [t/string?] :output-type t/string? :body [...]}])
            ;; Reactively in `:overload-queue` watch on `fghij|__types`
            (alist-conj! defnt/overload-queue
              ['ns1/fghij {:id 2 :ns 'ns1 :arg-types [t/byte?] :output-type t/char? :body [...]}])
            ;; Reactively due to `fghij|__types` changing
            (rx-set! fghij|__type (ftype t/any? [t/byte? :> t/char?] [t/int? :> t/long?]))
            ;; Consuming the `defnt/overload-queue` (iterate then clear, not incremental pop)
            (when (= lang :clj)
              (intern 'ns2 'abcde|__types|1
                (overload-types>arg-types (rx/norx-deref ns0/abcde|__types) 1))
              (intern 'ns2 'fghij|__types|2
                (overload-types>arg-types (rx/norx-deref ns1/fghij|__types) 2))))
    - Resulting in `abcde`'s runtime-emission code in CLJ as (easy to adapt for CLJS):
      - (do ;; Consuming the `defnt/overload-queue` (iterate then clear, not incremental pop)
            (intern 'ns2 'abcde|__1 (reify* [...] (invoke ([x00__ d] ...))))
            (intern 'ns0 'abcde
              (fn [x00__]
                (ifs ((Array/get ns0/abcde|__types|0 0) x00__) (. ns0/abcde|__0 invoke x00__)
                     ((Array/get ns2/abcde|__types|1 1) x00__) (. ns2/abcde|__1 invoke x00__)
                     (unsupported! ...))))
            (intern 'ns2 'fghij|__2 (reify* [...] (invoke ([x00__ b] ...))))
            (intern 'ns1 'fghij
              (fn [x00__]
                (ifs ((Array/get ns2/fghij|__types|2 0) x00__) (. ns2/fghij|__2 invoke x00__)
                     ((Array/get ns1/fghij|__types|0 0) x00__) (. ns1/fghij|__0 invoke x00__)
                     ((Array/get ns1/fghij|__types|1 0) x00__) (. ns1/fghij|__1 invoke x00__)
                     (unsupported! ...))))
            (var ns0/abcde))



(deftest test|sort-overload-types
  (is= (self/sort-overload-types core/identity
         [[(t/isa? Boolean)            (t/value nil)]
          [(t/isa? Double)             (t/isa? Byte)]
          [(t/isa? Double)             (t/isa? Short)]
          [(t/isa? Double)             (t/isa? Character)]
          [(t/isa? Double)             (t/isa? Integer)]
          [(t/isa? Double)             (t/isa? Long)]
          [(t/isa? Double)             (t/isa? Float)]
          [(t/isa? Double)             (t/isa? Double)]
          [(t/isa? Double)             (t/ref (t/isa? Comparable))]
          [(t/isa? Double)             (t/value nil)]
          [(t/value nil)               (t/isa? Boolean)]
          [(t/value nil)               (t/isa? Byte)]
          [(t/value nil)               (t/isa? Short)]
          [(t/value nil)               (t/isa? Character)]
          [(t/value nil)               (t/isa? Integer)]
          [(t/value nil)               (t/isa? Long)]
          [(t/value nil)               (t/isa? Float)]
          [(t/value nil)               (t/isa? Double)]
          [(t/value nil)               (t/value nil)]
          [(t/value nil)               (t/not (t/value nil))]
          [(t/value true)              (t/value false)]
          [(t/value true)              (t/value true)]
          [(t/value false)             (t/value false)]
          [(t/value false)             (t/value true)]
          [(t/ref (t/isa? Comparable)) (t/isa? Byte)]
          [(t/ref (t/isa? Comparable)) (t/isa? Short)]
          [(t/ref (t/isa? Comparable)) (t/isa? Character)]
          [(t/ref (t/isa? Comparable)) (t/isa? Integer)]
          [(t/ref (t/isa? Comparable)) (t/isa? Long)]
          [(t/ref (t/isa? Comparable)) (t/isa? Float)]
          [(t/ref (t/isa? Comparable)) (t/isa? Double)]
          [(t/ref (t/isa? Comparable)) (t/ref (t/isa? Comparable))]])
         [[(t/value nil)               (t/value nil)]
          [(t/isa? Boolean)            (t/value nil)]
          [(t/value nil)               (t/isa? Boolean)]
          [(t/value nil)               (t/isa? Byte)]
          [(t/isa? Double)             (t/isa? Byte)]
          [(t/value nil)               (t/isa? Short)]
          [(t/isa? Double)             (t/isa? Short)]
          [(t/value nil)               (t/isa? Character)]
          [(t/isa? Double)             (t/isa? Character)]
          [(t/value nil)               (t/isa? Integer)]
          [(t/isa? Double)             (t/isa? Integer)]
          [(t/value nil)               (t/isa? Long)]
          [(t/isa? Double)             (t/isa? Long)]
          [(t/value nil)               (t/isa? Float)]
          [(t/isa? Double)             (t/isa? Float)]
          [(t/value nil)               (t/isa? Double)]
          [(t/isa? Double)             (t/isa? Double)]
          [(t/isa? Double)             (t/ref (t/isa? Comparable))]
          [(t/isa? Double)             (t/value nil)]
          [(t/value true)              (t/value false)]
          [(t/value true)              (t/value true)]
          [(t/value false)             (t/value false)]
          [(t/value false)             (t/value true)]
          [(t/ref (t/isa? Comparable)) (t/isa? Byte)]
          [(t/ref (t/isa? Comparable)) (t/isa? Short)]
          [(t/ref (t/isa? Comparable)) (t/isa? Character)]
          [(t/ref (t/isa? Comparable)) (t/isa? Integer)]
          [(t/ref (t/isa? Comparable)) (t/isa? Long)]
          [(t/ref (t/isa? Comparable)) (t/isa? Float)]
          [(t/ref (t/isa? Comparable)) (t/isa? Double)]
          [(t/ref (t/isa? Comparable)) (t/ref (t/isa? Comparable))]
          [(t/value nil)               (t/not (t/value nil))]]))

(deftest test|fn
  (let [actual (binding [self/*compilation-mode* :test]
                 (macroexpand '
                   ;: FIXME this contract is not being held up when returning nil
                   (self/defn f0 [a (t/or tt/boolean? tt/double?)
                                  > (t/ftype [tt/byte? :> (t/ftype [tt/char?])])]
                     ;; TODO this fits into a larger scheme of, should we have output types be
                     ;; `(t/and actual declared)` or should we just have them be `declared`? The
                     ;; latter is easier but it seems like the `t/fn` dispatch forces our hand
                     ;; towards the former. We need to think about this more.
                     (self/fn [b (t/or tt/byte? tt/char?)
                               > (t/ftype [(t/or (t/type a) tt/short?)])]
                       (self/fn f1 [c (t/or (t/type a) tt/short?)]
                         b (f1 a) (f1 c))))))
        expected
          (case (env-lang)
            :clj
            ($ (do (declare ~'f0)
                   [[0 0 false [] (t/ftype tt/boolean? [tt/byte? :> (t/ftype [tt/char?])])]]
(defmeta-from ~'f0
  (let* [fs (*<>|sized|macro 2)
         f  (new TypedFn
              {:quantum.core.type/type ~'f0|__type}
              (fn* ([~ts ~fs ~'x00__]
                     (ifs (~(aget* (aget* ts 0) 0) ~'x00__)
                          (. ~(aget* ts 0) ~'invoke ~'x00__)
                          (~(aget* (aget* ts 1) 0) ~'x00__)
                          (. ~(aget* ts 1) ~'invoke ~'x00__)
                          (unsupported! `f0 [~'x00__] 0)))))]
   ~(aset* fs 0
     `(reify* [boolean>Object]
        (~'invoke [~'_0__ ~(B 'a)]
          ;; From `(self/fn [b ...])`
          (self/>anon-fn
            ;; TODO perhaps extern this (and parts thereof) whenever possible in `let*`
            ;; statement on the very outside of the fn (so around the outer `reify*`) ?
            (*<>|macro (*<>|macro (t/isa? Byte)) (*<>|macro (t/isa? Character)))
            (*<>|macro
              (reify* [byte>Object]
                (~'invoke [~'_0__ ~(Y 'b)]
                  ;; From `(self/fn [c ...])`
                  (self/>anon-fn
                    (*<>|macro (*<>|macro (t/isa? Boolean)) (*<>|macro (t/isa? Short)))
                    (fn* [~(tag (cstr `AnonFn) 'this__)]
                      (*<>|macro
                        (reify* [boolean>Object]
                          (~'invoke [~'_0__ (B 'c)]
                            ~'b
                            (. (tag (cstr `boolean>Object) (Array/get (.-fs ~'this__) 0)) ~'invoke ~'a)
                            (. (tag (cstr `boolean>Object) (Array/get (.-fs ~'this__) 0)) ~'invoke ~'c)))
                        (reify* [short>Object]
                          (~'invoke [~'_0__ (S 'c)]
                            ~'b
                            (. (tag (cstr `boolean>Object) (Array/get (.-fs ~'this__) 0)) ~'invoke ~'a)
                            (. (tag (cstr `short>Object)   (Array/get (.-fs ~'this__) 1)) ~'invoke ~'c)))))
                    (fn* ([~(O<> 'types__) ~(O<> 'fs__) ~'x00]
                           (ifs ((Array/get (Array/get ~'types__ 0) 0) ~'x00__)
                                (. ~(tag (cstr `boolean>Object) `(Array/get ~'fs__ 0))
                                   ~'invoke ~'x00__)
                                ((Array/get (Array/get ~'types__ 1) 0) ~'x00__)
                                (. ~(tag (cstr `short>Object)   `(Array/get ~'fs__ 1))
                                   ~'invoke ~'x00__)
                                (unsupported! [~'x00__] 0)))))))
              (reify* [char>Object]
                (~'invoke [~'_0__ ~(C 'a)] ...)))
            (fn* ([~(O<> 'types__) ~(O<> 'fs__) ~'x00__]
                   (ifs ((Array/get (Array/get ~'types__ 0) 0) ~'x00__)
                        (. ~(tag (cstr `byte>Object) `(Array/get ~'fs__ 0)) ~'invoke ~'x00__)
                        ((Array/get (Array/get ~'types__ 1) 0) ~'x00__)
                        (. ~(tag (cstr `char>Object) `(Array/get ~'fs__ 1)) ~'invoke ~'x00__)
                        (unsupported! [~'x00__] 0)))))))
   ~(aset* fs 1
     `(reify* [double>Object]
        (~'invoke [~'_0__ ~(D 'a)] ...)))))

)))]
    ))


"
FIXME the below can be fixed if each `t/fn` and/or `t/defn` was encapsulated by a concrete type,
like AnonFn, that stored references to the types, overloads, etc.
We should probably have standard overload indices for ftypes (maybe we already do? we should sort
ftypes' overload-types in the same way that we sort `t/defn` overload-types. maybe that will make it
standard) so direct dispatch can be performed even in the absence of an fn-name.

We could do:
`(. ^TheReifyType (aget (.-ts f) <index>) invoke <~@args>)`
- This is all fine when `f` is `=` (perhaps `t/=`?) to the declared type, but when it's `t/<`, it
  may allow for more than what the declared type requires, in which case it may have more and/or
  different overloads. So do something like this:
   (t/defn a [f (t/ftype [t/long?])] (f 1))
   -> (def a|__0 (reify [_ ^TypedFn f ^int f|__i] (.invoke ^long>Object (RT/aget (.-overloads f) f|__i) 1)))
   (t/defn b [x (t/or t/boolean? t/long?)] x)
   (t/dotyped (a b))
   -> (.invoke a|__0 b|__f 1) ; meaning, use the overload at index 1. If -1 then

   This would require arglist expansion which is kind of a pain but stack allocation is always
   cheaper than heap.

TODO let's see what we can do with the expansion/inlining of `compf`. It may prove subtle/tricky?
"
{:message "No name found for typed fn corresponding to caller",
 :data {:type (quantum.untyped.core.type/ftype
               (quantum.untyped.core.type/isa?
                java.lang.Boolean)
               [(quantum.untyped.core.type/or
                 (quantum.untyped.core.type/isa?
                  java.lang.Number)
                 (quantum.untyped.core.type/isa?
                  java.lang.Character))
                (quantum.untyped.core.type/or
                 (quantum.untyped.core.type/isa?
                  java.lang.Number)
                 (quantum.untyped.core.type/isa?
                  java.lang.Character))]),
        :form numeric-compf}}
