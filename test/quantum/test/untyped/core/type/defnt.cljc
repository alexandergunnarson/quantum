(ns quantum.test.untyped.core.type.defnt
  (:refer-clojure :exclude
    [> count get identity name seq some? zero?])
  (:require
    [clojure.core                           :as core]
    [quantum.core.type
      :refer [dotyped]]
    [quantum.test.untyped.core.type         :as tt]
    [quantum.untyped.core.analyze           :as uana]
    [quantum.untyped.core.data.array
      :refer [*<> *<>|sized]]
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
    [quantum.untyped.core.type.defnt        :as self
      :refer [aget* aset* unsupported!]]
    [quantum.untyped.core.type.reifications :as utr]
    [quantum.untyped.core.vars
      :refer [defmeta-from]])
  (:import
    [clojure.lang                           ASeq ISeq LazySeq Named Reduced RT Seqable]
    [quantum.core.data                      Array]
    [quantum.core                           Numeric Primitive]
    [quantum.untyped.core.type.reifications TypedFn]))

;; TODO test `:inline`

(do (require '[orchestra.spec.test :as st])
    (clojure.spec.test.alpha/unstrument)
    (orchestra.spec.test/unstrument)
    (orchestra.spec.test/instrument))

(do

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
      (str (core/namespace x) "." (core/name x))))

(defn csym [x] (-> x cstr symbol))

(defn >__O  [form] (tag (cstr `__O)  form))
(defn >B__B [form] (tag (cstr `B__B) form))
(defn >Y__Y [form] (tag (cstr `Y__Y) form))
(defn >S__S [form] (tag (cstr `S__S) form))
(defn >C__C [form] (tag (cstr `C__C) form))
(defn >I__I [form] (tag (cstr `I__I) form))
(defn >L__L [form] (tag (cstr `L__L) form))
(defn >F__F [form] (tag (cstr `F__F) form))
(defn >D__D [form] (tag (cstr `D__D) form))
(defn >O__F [form] (tag (cstr `O__F) form))
(defn >O__O [form] (tag (cstr `O__O) form))

(def &ts (O<> 'ts0__))
(def &fs (O<> 'fs0__))
(def &this '&this)

)

#?(:clj
(deftest test|pid
  (let [actual
          (binding [self/*compilation-mode* :test]
            (macroexpand '
              (self/defn pid [> (t/? t/string?)]
                (->> ^:val (java.lang.management.ManagementFactory/getRuntimeMXBean)
                           (.getName)))))
        expected
        ($ (do (defmeta-from ~'pid
                 (let* [~'pid|__fs (*<>|sized 1)
                        ~'pid (new TypedFn
                                {:quantum.core.type/type pid|__type}
                                ;; [[0 0 false [] (t/or t/nil? t/string?)]]
                                pid|__ts ; defined/created within `t/defn` ; FIXME make it so the arrays are shown here
                                ~'pid|__fs
                                (fn* ([~&ts ~&fs] (. ~(>__O (aget* &fs 0)) ~'invoke))))]
                   ~(aset* 'pid|__fs 0
                     `(reify* [~(csym `__O)]
                        (~(O 'invoke) [~'_0__]
                          ~(ST (list '.
                                 (tag "java.lang.management.RuntimeMXBean"
                                      '(. java.lang.management.ManagementFactory getRuntimeMXBean))
                                 'getName)))))
                   ~'pid))))]
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
            ($ (do [[0 0 true [t/boolean?] t/boolean?]
                    [1 1 true [t/byte?]    t/byte?]
                    [2 2 true [t/short?]   t/short?]
                    [3 3 true [t/char?]    t/char?]
                    [4 4 true [t/int?]     t/int?]
                    [5 5 true [t/long?]    t/long?]
                    [6 6 true [t/float?]   t/float?]
                    [7 7 true [t/double?]  t/double?]
                    [8 8 true [t/any?]     t/any?]]
                   (defmeta-from ~'identity
                     (let* [~'identity|__fs (*<>|sized 9)
                            ~'identity
                              (new TypedFn
                                {:quantum.core.type/type identity|__type}
                                identity|__ts
                                ~'identity|__fs
                                (fn* ([~&ts ~&fs ~'x00__]
                                       (ifs (~(aget* (O<> (aget* &ts 0)) 0) ~'x00__)
                                              (. ~(>B__B (aget* &fs 0)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 1)) 0) ~'x00__)
                                              (. ~(>Y__Y (aget* &fs 1)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 2)) 0) ~'x00__)
                                              (. ~(>S__S (aget* &fs 2)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 3)) 0) ~'x00__)
                                              (. ~(>C__C (aget* &fs 3)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 4)) 0) ~'x00__)
                                              (. ~(>I__I (aget* &fs 4)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 5)) 0) ~'x00__)
                                              (. ~(>L__L (aget* &fs 5)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 6)) 0) ~'x00__)
                                              (. ~(>F__F (aget* &fs 6)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 7)) 0) ~'x00__)
                                              (. ~(>D__D (aget* &fs 7)) ~'invoke ~'x00__)
                                            (~(aget* (O<> (aget* &ts 8)) 0) ~'x00__)
                                              (. ~(>O__O (aget* &fs 8)) ~'invoke ~'x00__)
                                            ;; TODO no need for `unsupported!` because it will
                                            ;;      always get a valid  branch
                                            (unsupported! `identity [~'x00__] 0)))))]
                       ;; [x t/any?]

                       ~(aset* 'identity|__fs 0
                          `(reify* [~(csym `B__B)] (~(B 'invoke) [~'_0__ ~(B 'x)] ~'x)))
                       ~(aset* 'identity|__fs 1
                          `(reify* [~(csym `Y__Y)] (~(Y 'invoke) [~'_1__ ~(Y 'x)] ~'x)))
                       ~(aset* 'identity|__fs 2
                          `(reify* [~(csym `S__S)] (~(S 'invoke) [~'_2__ ~(S 'x)] ~'x)))
                       ~(aset* 'identity|__fs 3
                          `(reify* [~(csym `C__C)] (~(C 'invoke) [~'_3__ ~(C 'x)] ~'x)))
                       ~(aset* 'identity|__fs 4
                          `(reify* [~(csym `I__I)] (~(I 'invoke) [~'_4__ ~(I 'x)] ~'x)))
                       ~(aset* 'identity|__fs 5
                          `(reify* [~(csym `L__L)] (~(L 'invoke) [~'_5__ ~(L 'x)] ~'x)))
                       ~(aset* 'identity|__fs 6
                          `(reify* [~(csym `F__F)] (~(F 'invoke) [~'_6__ ~(F 'x)] ~'x)))
                       ~(aset* 'identity|__fs 7
                          `(reify* [~(csym `D__D)] (~(D 'invoke) [~'_7__ ~(D 'x)] ~'x)))
                       ~(aset* 'identity|__fs 8
                          `(reify* [~(csym `O__O)] (~(O 'invoke) [~'_8__ ~(O 'x)] ~(O 'x))))
                       ~'identity))))
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
                   (reify* [~(csym `O__O)] (~(O 'invoke) [~'_0__ ~(O 'x)] ~(ST 'x))))

                 ;; [x (t/isa? Named)] > (t/run t/string?)

                 (def ~(>O__O 'name|__1)
                   (reify* [~(csym `O__O)]
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
                   (reify* [~(csym `O__B)] (~(B 'invoke) [~'_0__ ~(O 'x)] false)))

                 ;; [x t/any?]

                 (def ~(>B__B 'some?|__1)
                   (reify* [~(csym `B__B)] (~(B 'invoke) [~'_1__ ~(B 'x)] true)))
                 (def ~(>Y__B 'some?|__2)
                   (reify* [~(csym `Y__B)] (~(B 'invoke) [~'_2__ ~(Y 'x)] true)))
                 (def ~(>S__B 'some?|__3)
                   (reify* [~(csym `S__B)] (~(B 'invoke) [~'_3__ ~(S 'x)] true)))
                 (def ~(>C__B 'some?|__4)
                   (reify* [~(csym `C__B)] (~(B 'invoke) [~'_4__ ~(C 'x)] true)))
                 (def ~(>I__B 'some?|__5)
                   (reify* [~(csym `I__B)] (~(B 'invoke) [~'_5__ ~(I 'x)] true)))
                 (def ~(>L__B 'some?|__6)
                   (reify* [~(csym `L__B)] (~(B 'invoke) [~'_6__ ~(L 'x)] true)))
                 (def ~(>F__B 'some?|__7)
                   (reify* [~(csym `F__B)] (~(B 'invoke) [~'_7__ ~(F 'x)] true)))
                 (def ~(>D__B 'some?|__8)
                   (reify* [~(csym `D__B)] (~(B 'invoke) [~'_8__ ~(D 'x)] true)))
                 (def ~(>O__B 'some?|__9)
                   (reify* [~(csym `O__B)] (~(B 'invoke) [~'_9__ ~(O 'x)] true)))

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
                       (reify* [~(csym `O__B)]
                         (~(B 'invoke) [~'_0__ ~(O 'x)]
                           (let* [~(tag "clojure.lang.Reduced" 'x) ~'x] true))))

                     ;; [x t/any?]

                     (def ~'reduced?|test|__1|0
                       (reify* [~@(map csym `[O__B B__B Y__B S__B C__B I__B L__B F__B D__B])]
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
                       (reify* [~(csym `B__B)]
                         (~(B 'invoke) [~'_0__  ~(B 'x)] ~'x)))

                     ;; [x t/nil? -> (- t/nil? tt/boolean?)]

                     (def ~(O<> '>boolean|__1|input0|types)
                       (*<> (t/value nil)))
                     (def ~'>boolean|__1|0
                       (reify* [~(csym `O__B)]
                         (~(B 'invoke) [~'_1__  ~(O 'x)] false)))

                     ;; [x t/any? -> (- t/any? t/nil? tt/boolean?)]

                     (def ~(O<> '>boolean|__2|input0|types)
                       (*<> t/any?))
                     (def ~'>boolean|__2|0
                       (reify* [~@(map csym `[O__B B__B Y__B S__B C__B I__B L__B F__B D__B])]
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
;; This means that you *can't* have a reify with two O__O overloads and expect it to work
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
                   (reify* [~(csym `Y__I)]
                     (~(I 'invoke) [~'_0__ ~(Y 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|1
                   (reify* [~(csym `S__I)]
                     (~(I 'invoke) [~'_1__ ~(S 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|2
                   (reify* [~(csym `C__I)]
                     (~(I 'invoke) [~'_2__ ~(C 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|3
                   (reify* [~(csym `I__I)]
                     (~(I 'invoke) [~'_3__ ~(I 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|4
                   (reify* [~(csym `L__I)]
                     (~(I 'invoke) [~'_4__ ~(L 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|5
                   (reify* [~(csym `F__I)]
                     (~(I 'invoke) [~'_5__ ~(F 'x)] ~'(. Primitive uncheckedIntCast x))))
                 (def ~'>int*|__0|6
                   (reify* [~(csym `D__I)]
                     (~(I 'invoke) [~'_6__ ~(D 'x)] ~'(. Primitive uncheckedIntCast x))))

                 ;; [x (t/ref (t/isa? Number))
                 ;;  -> (t/- (t/ref (t/isa? Number)) (t/- tt/primitive? tt/boolean?))]

                 (def ~(O<> '>int*|__1|input0|types)
                   (*<> (t/ref (t/isa? Number))))
                 (def ~'>int*|__1|0
                   (reify* [~(csym `O__I)]
                     (~(I 'invoke) [~'_7__ ~(O 'x)]
                       (let* [~(tag "java.lang.Number" 'x) ~'x] ~'(. x intValue)))))

                 (defn ~'>int*
                   {:quantum.core.type/type
                     (t/fn ~'tt/int?
                           ~'[(t/- tt/primitive? tt/boolean?)]
                           ~'[(t/ref (t/isa? Number))])}
                   ([~'x00__]
                     (ifs ((Array/get ~'>int*|__0|input0|types 0) ~'x00__)
                            (.invoke ~(tag (cstr `Y__I)   '>int*|__0|0) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 1) ~'x00__)
                            (.invoke ~(tag (cstr `S__I)  '>int*|__0|1) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 2) ~'x00__)
                            (.invoke ~(tag (cstr `C__I)   '>int*|__0|2) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 3) ~'x00__)
                            (.invoke ~(tag (cstr `I__I)    '>int*|__0|3) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 4) ~'x00__)
                            (.invoke ~(tag (cstr `L__I)   '>int*|__0|4) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 5) ~'x00__)
                            (.invoke ~(tag (cstr `F__I)  '>int*|__0|5) ~'x00__)
                          ((Array/get ~'>int*|__0|input0|types 6) ~'x00__)
                            (.invoke ~(tag (cstr `D__I) '>int*|__0|6) ~'x00__)
                          ((Array/get ~'>int*|__1|input0|types 0) ~'x00__)
                            (.invoke ~(tag (cstr `O__I) '>int*|__1|0) ~'x00__)
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

                 (def ~(tag (cstr `YY__B) '>|__0)
                   (reify* [~(csym ~YY__B)]
                     (~(B 'invoke) [~'_0__  ~(Y 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `YS__B) '>|__1)
                   (reify* [~(csym ~YS__B)]
                     (~(B 'invoke) [~'_1__  ~(Y 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `YC__B) '>|__2)
                   (reify* [~(csym ~YC__B)]
                     (~(B 'invoke) [~'_2__  ~(Y 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `YI__B) '>|__3)
                   (reify* [~(csym ~YI__B)]
                     (~(B 'invoke) [~'_3__  ~(Y 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `YL__B) '>|__4)
                   (reify* [~(csym ~YL__B)]
                     (~(B 'invoke) [~'_4__  ~(Y 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `YF__B) '>|__5)
                   (reify* [~(csym ~YF__B)]
                     (~(B 'invoke) [~'_5__  ~(Y 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `YD__B) '>|__6)
                   (reify* [~(csym ~YD__B)]
                     (~(B 'invoke) [~'_6__  ~(Y 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `SY__B) '>|__7)
                   (reify* [~(csym `SY__B)]
                     (~(B 'invoke) [~'_7__  ~(S 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `SS__B) '>|__8)
                   (reify* [~(csym `SS__B)]
                     (~(B 'invoke) [~'_8__  ~(S 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `SC__B) '>|__9)
                   (reify* [~(csym `SC__B)]
                     (~(B 'invoke) [~'_9__  ~(S 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `SI__B) '>|__10)
                   (reify* [~(csym `SI__B)]
                     (~(B 'invoke) [~'_10__ ~(S 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `SL__B) '>|__11)
                   (reify* [~(csym `SL__B)]
                     (~(B 'invoke) [~'_11__ ~(S 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `SF__B) '>|__12)
                   (reify* [~(csym `SF__B)]
                     (~(B 'invoke) [~'_12__ ~(S 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `SD__B) '>|__13)
                   (reify* [~(csym `SD__B)]
                     (~(B 'invoke) [~'_13__ ~(S 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `CY__B) '>|__14)
                   (reify* [~(csym `CY__B)]
                     (~(B 'invoke) [~'_14__ ~(C 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `CS__B) '>|__15)
                   (reify* [~(csym `CS__B)]
                     (~(B 'invoke) [~'_15__ ~(C 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `CC__B) '>|__16)
                   (reify* [~(csym `CC__B)]
                     (~(B 'invoke) [~'_16__ ~(C 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `CI__B) '>|__17)
                   (reify* [~(csym `CI__B)]
                     (~(B 'invoke) [~'_17__ ~(C 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `CL__B) '>|__18)
                   (reify* [~(csym `CL__B)]
                     (~(B 'invoke) [~'_18__ ~(C 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `CF__B) '>|__19)
                   (reify* [~(csym `CF__B)]
                     (~(B 'invoke) [~'_19__ ~(C 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `CD__B) '>|__20)
                   (reify* [~(csym `CD__B)]
                     (~(B 'invoke) [~'_20__ ~(C 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `IY__B) '>|__21)
                   (reify* [~(csym `IY__B)]
                     (~(B 'invoke) [~'_21__ ~(I 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `IS__B) '>|__22)
                   (reify* [~(csym `IS__B)]
                     (~(B 'invoke) [~'_22__ ~(I 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `IC__B) '>|__23)
                   (reify* [~(csym `IC__B)]
                     (~(B 'invoke) [~'_23__ ~(I 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `II__B) '>|__24)
                   (reify* [~(csym `II__B)]
                     (~(B 'invoke) [~'_24__ ~(I 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `IL__B) '>|__25)
                   (reify* [~(csym `IL__B)]
                     (~(B 'invoke) [~'_25__ ~(I 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `IF__B) '>|__26)
                   (reify* [~(csym `IF__B)]
                     (~(B 'invoke) [~'_26__ ~(I 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `ID__B) '>|__27)
                   (reify* [~(csym `ID__B)]
                     (~(B 'invoke) [~'_27__ ~(I 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `LY__B) '>|__28)
                   (reify* [~(csym `LY__B)]
                     (~(B 'invoke) [~'_28__ ~(L 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `LS__B) '>|__29)
                   (reify* [~(csym `LS__B)]
                     (~(B 'invoke) [~'_29__ ~(L 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `LC__B) '>|__30)
                   (reify* [~(csym `LC__B)]
                     (~(B 'invoke) [~'_30__ ~(L 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `LI__B) '>|__31)
                   (reify* [~(csym `LI__B)]
                     (~(B 'invoke) [~'_31__ ~(L 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `LL__B) '>|__32)
                   (reify* [~(csym `LL__B)]
                     (~(B 'invoke) [~'_32__ ~(L 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `LF__B) '>|__33)
                   (reify* [~(csym `LF__B)]
                     (~(B 'invoke) [~'_33__ ~(L 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `LD__B) '>|__34)
                   (reify* [~(csym `LD__B)]
                     (~(B 'invoke) [~'_34__ ~(L 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `FY__B) '>|__35)
                   (reify* [~(csym `FY__B)]
                     (~(B 'invoke) [~'_35__ ~(F 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `FS__B) '>|__36)
                   (reify* [~(csym `FS__B)]
                     (~(B 'invoke) [~'_36__ ~(F 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `FC__B) '>|__37)
                   (reify* [~(csym `FC__B)]
                     (~(B 'invoke) [~'_37__ ~(F 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `FI__B) '>|__38)
                   (reify* [~(csym `FI__B)]
                     (~(B 'invoke) [~'_38__ ~(F 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `FL__B) '>|__39)
                   (reify* [~(csym `FL__B)]
                     (~(B 'invoke) [~'_39__ ~(F 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `FF__B) '>|__40)
                   (reify* [~(csym `FF__B)]
                     (~(B 'invoke) [~'_40__ ~(F 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `FD__B) '>|__41)
                   (reify* [~(csym `FD__B)]
                     (~(B 'invoke) [~'_41__ ~(F 'a) ~(D 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `DY__B) '>|__42)
                   (reify* [~(csym `DY__B)]
                     (~(B 'invoke) [~'_42__ ~(D 'a) ~(Y 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `DS__B) '>|__43)
                   (reify* [~(csym `DS__B)]
                     (~(B 'invoke) [~'_43__ ~(D 'a) ~(S 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `DC__B) '>|__44)
                   (reify* [~(csym `DC__B)]
                     (~(B 'invoke) [~'_44__ ~(D 'a) ~(C 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `DI__B) '>|__45)
                   (reify* [~(csym `DI__B)]
                     (~(B 'invoke) [~'_45__ ~(D 'a) ~(I 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `DL__B) '>|__46)
                   (reify* [~(csym `DL__B)]
                     (~(B 'invoke) [~'_46__ ~(D 'a) ~(L 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `DF__B) '>|__47)
                   (reify* [~(csym `DF__B)]
                     (~(B 'invoke) [~'_47__ ~(D 'a) ~(F 'b)] ~'(. Numeric gt a b))))
                 (def ~(tag (cstr `DD__B) '>|__48)
                   (reify* [~(csym `DD__B)]
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

                        (def ~(tag (cstr `Y__L) '>long*|__0)
                          (reify* [~(csym `Y__L)]
                            (~(L 'invoke) [~'_0__ ~(Y             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `S__L) '>long*|__1)
                          (reify* [~(csym `S__L)]
                            (~(L 'invoke) [~'_1__ ~(S            'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `C__L) '>long*|__2)
                          (reify* [~(csym `C__L)]
                            (~(L 'invoke) [~'_2__ ~(C             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `I__L) '>long*|__3)
                          (reify* [~(csym `I__L)]
                            (~(L 'invoke) [~'_3__ ~(I              'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `L__L) '>long*|__4)
                          (reify* [~(csym `L__L)]
                            (~(L 'invoke) [~'_4__ ~(L             'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `F__L) '>long*|__5)
                          (reify* [~(csym `F__L)]
                            (~(L 'invoke) [~'_5__ ~(F            'x)]
                              ~'(. Primitive uncheckedLongCast x))))
                        (def ~(tag (cstr `D__L) '>long*|__6)
                          (reify* [~(csym `D__L)]
                            (~(L 'invoke) [~'_6__ ~(D           'x)]
                              ~'(. Primitive uncheckedLongCast x))))

                        ;; [x (t/ref (t/isa? Number))]

                        (def ~(tag (cstr `O__L) '>long*|__7)
                          (reify* [~(csym `O__L)]
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

                 (def ~(tag (cstr `B__O) 'ref-output-type|__0)
                   (reify* [~(csym `B__O)] (~(O 'invoke) [~'_0__ ~(B 'x)] (new ~'Boolean ~'x))))

                 ;; [x tt/byte? > (t/ref tt/byte?)]

                 (def ~(tag (cstr `Y__O) 'ref-output-type|__1)
                   (reify* [~(csym `Y__O)] (~(O 'invoke) [~'_1__ ~(Y 'x)] (new ~'Byte ~'x))))

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
                          (reify Y__L
                            (~(L 'invoke) [_## ~(Y 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__0 invoke ~'x))))

                        #_(def ~'>long|__1|input-types (*<> short?))
                        (def ~'>long|__1
                          (reify S__L
                            (~(L 'invoke) [_## ~(C 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__1 invoke ~'x))))

                        #_(def ~'>long|__2|input-types (*<> char?))
                        (def ~'>long|__2
                          (reify C__L
                            (~(L 'invoke) [_## ~(S 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__2 invoke ~'x))))

                        #_(def ~'>long|__3|input-types (*<> tt/int?))
                        (def ~'>long|__3
                          (reify I__L
                            (~(L 'invoke) [_## ~(I 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__3 invoke ~'x))))

                        #_(def ~'>long|__4|input-types (*<> tt/long?))
                        (def ~'>long|__4
                          (reify L__L
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
                          (reify D__L
                            (~(L 'invoke) [_## ~(D 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__6 invoke ~'x))))

                        #_(def ~'>long|__6|input-types
                          (*<> (t/and t/float?
                                      (t/fn [x (t/or double? float?)]
                                        (and (>= x Long/MIN_VALUE) (<= x Long/MAX_VALUE))))))
                        (def ~'>long|__6
                          (reify F__L
                            (~(L 'invoke) [_## ~(F 'x)]
                              ;; Resolved from `(>long* x)`
                              (. >long*|__6 invoke ~'x))))

                        #_[(t/and (t/isa? clojure.lang.BigInt)
                                  (t/fn [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))]

                        #_(def ~'>long|__7|input-types
                          (*<> (t/and (t/isa? clojure.lang.BigInt)
                                      (t/fn [x (t/isa? clojure.lang.BigInt)] (t/nil? (.bipart x))))))
                        (def ~'>long|__7
                          (reify O__L
                            (~(L 'invoke) [_## ~(O 'x)]
                              (let* [~(tag "clojure.lang.BigInt" 'x) ~'x] ~'(.lpart x)))))

                        #_[x (t/and (t/isa? java.math.BigInteger)
                                    (t/fn [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))]

                        #_(def ~'>long|__8|input-types
                          (*<> (t/and (t/isa? java.math.BigInteger)
                                      (t/fn [x (t/isa? java.math.BigInteger)] (< (.bitLength x) 64)))))
                        (def ~'>long|__8
                          (reify O__L
                            (~(L 'invoke) [_## ~(O 'x)]
                              (let* [~(tag "java.math.BigInteger" 'x) ~'x] ~'(.longValue x)))))

                        #_[x ratio?]

                        #_(def ~'>long|__9|input-types
                          (*<> ratio?))
                        #_(def ~'>long|__9|conditions
                          (*<> (-> long|__8|input-types (core/get 0) utr/and-type>args (core/get 1))))
                        (def ~'>long|__9
                          (reify O__L
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
                          (reify O__L
                            (~(L 'invoke) [_## ~(O 'x)]
                              ~'(Long/parseLong x))))

                        #_[x t/string?]

                        #_(def ~'>long|__13|input-types
                          (*<> t/string? tt/int?))
                        (def ~'>long|__13
                          (reify OI__L
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
                          (reify* [~(csym `__O)]
                            (~(O 'invoke) [~'_0__]
                              ~(tag "java.lang.StringBuilder" '(new StringBuilder)))))

                        (def ~(O<> '!str|__1|input0|types)
                          (*<> (t/isa? java.lang.String)))
                        (def ~'!str|__1|0
                          (reify* [~(csym `O__O)]
                            (~(O 'invoke) [~'_1__ ~(O 'x)]
                              (let* [~(ST 'x) ~'x]
                                ~(tag "java.lang.StringBuilder"
                                      (list 'new 'StringBuilder (ST 'x)))))))

                        (def ~(O<> '!str|__2|input0|types)
                          (*<> (t/isa? java.lang.CharSequence)
                               (t/isa? java.lang.Integer)))
                        (def ~'!str|__2|0
                          (reify* [~(csym `O__O)]
                            (~(O 'invoke) [~'_2__ ~(O 'x)]
                              (let* [~(tag "java.lang.CharSequence" 'x) ~'x]
                                ~(tag "java.lang.StringBuilder"
                                      (list 'new 'StringBuilder
                                            (tag "java.lang.CharSequence" 'x)))))))
                        (def ~'!str|__2|1
                          (reify* [~(csym `I__O)]
                            (~(O 'invoke) [~'_3__ ~(I 'x)]
                              ~(tag "java.lang.StringBuilder" '(new StringBuilder x)))))

                        (defn ~'!str
                          {:quantum.core.type/type
                            (t/fn ~'(t/isa? StringBuilder)
                                  ~'[]
                                  ~'[t/string?]
                                  ~'[(t/or tt/char-seq? tt/int?)])}
                          ([] (.invoke ~(tag "quantum.core.test.defnt_equivalences.__O"
                                             '!str|__0|0)))
                          ([~'x00__]
                            (ifs
                              ((Array/get ~'!str|__1|input0|types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.O__O"
                                               '!str|__1|0) ~'x00__)
                              ((Array/get ~'!str|__2|input0|types 0) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.O__O"
                                               '!str|__2|0) ~'x00__)
                              ((Array/get ~'!str|__2|input0|types 1) ~'x00__)
                                (.invoke ~(tag "quantum.core.test.defnt_equivalences.I__O"
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
                            (reify* [~(csym `__D)]
                              (~(O 'invoke) [~'_0__] 2.0)))

                          ;; [x tt/long? > tt/double?]

                          (def ~'defn-self-reference|__1
                            (reify* [~(csym `L__D)]
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
                            ( [>long] (~(L 'invoke) [~'_0__] ~'(>long* 1))))

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
  (testing "t/input"
    (let [actual
            (macroexpand '
              (self/defn input-type-test
                [> (t/output >long-checked [t/string?])] 1))
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
        :clj  `(do (def ^__O !str|__0
                     (reify __O
                       (^java.lang.Object invoke [_#]
                         (StringBuilder.))))
                   ;; `t/string?`
                   (def ^O__O !str|__1 ; `t/string?`
                     (reify O__O
                       (^java.lang.Object invoke [_# ^java.lang.Object ~'x]
                         (let* [^String x x] (StringBuilder. x)))))
                   ;; `(t/or t/char-seq? tt/int?)`
                   (def ^O__O !str|__2 ; `t/char-seq?`
                     (reify O__O
                       (^java.lang.Object invoke [_# ^java.lang.Object ~'x]
                         (let* [^CharSequence x x] (StringBuilder. x)))))
                   (def ^I__O !str|__3 ; `tt/int?`
                     (reify I__O (^java.lang.Object invoke [_# ^int ~'x]
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
                     (reify __O       (^java.lang.Object invoke [_#                      ] "")))
                   (def str|__1 ; `nil?`
                     (reify O__O (^java.lang.Object invoke [_# ^java.lang.Object ~'x] "")))
                   (def str|__2 ; `Object`
                     (reify O__O (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (.toString x))))

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
                   (def count|__0__1 (reify O__I (^int invoke [_# ^java.lang.Object ~'xs] (Array/count ^"[B" xs))))
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
               (def ^O__O seq|__0
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     ;; Notice, no casting for nil input
                     nil)))

               ;; [(t/isa? ASeq)]

               (def seq|__2|input-types (*<> (t/isa? ASeq)))
               (def ^O__O seq|__2
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^ASeq xs xs] xs))))

               ;; [(t/or (t/isa? LazySeq) (t/isa? Seqable))]

               (def seq|__3|input-types (*<> (t/isa? LazySeq)))
               (def ^O__O seq|__3
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^LazySeq xs xs] (.seq xs)))))

               (def seq|__4|input-types (*<> (t/isa? Seqable)))
               (def ^O__O seq|__4
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^Seqable xs xs] (.seq xs)))))

               ;; [t/iterable?]

               (def seq|__5|input-types (*<> t/iterable?))
               (def ^O__O seq|__5
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^Iterable xs xs] (clojure.lang.RT/chunkIteratorSeq (.iterator xs))))))

               ;; [t/char-seq?]

               (def seq|__6|input-types (*<> t/iterable?))
               (def ^O__O seq|__6
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^CharSequence xs xs] (StringSeq/create xs)))))

               ;; [(t/isa? Map)]

               (def seq|__7|input-types (*<> (t/isa? Map)))
               (def ^O__O seq|__7
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     ;; This is after expansion; it's the first one that matches the overload
                     ;; If no overload is found it'll have to be dispatched at runtime (protocol or
                     ;; equivalent) and potentially a configurable warning can be emitted
                     (let [^Map xs xs] (.invoke seq|__4__0 (.entrySet xs))))))

               ;; [t/array?]

               ;; TODO perhaps at some point figure out that it doesn't need to create any more
               ;; overloads here than just one?
               (def seq|__8|input-types (*<> (t/isa? (Class/forName "[Z"))))
               (def ^O__O seq|__8
                 (reify O__O
                   (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                     (let* [^"[Z" xs xs] (ArraySeq/createFromObject xs)))))

               (def seq|__9|input-types (*<> (t/isa? (Class/forName "[B"))))
               (def ^O__O seq|__9
                 (reify O__O
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
 `[(def ~(tag (cstr `BYSS__O)     'dependent-extensible|__0)
     (reify* [~(csym `BYSS__O)]
       (~(O 'invoke) [~'_0__  ~(B 'a) ~(Y 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `BYSC__O)      'dependent-extensible|__1)
     (reify* [~(csym `BYSC__O)]
       (~(O 'invoke) [~'_1__  ~(B 'a) ~(Y 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `BYSO__O)    'dependent-extensible|__2)
     (reify* [~(csym `BYSO__O)]
       (~(O 'invoke) [~'_2__  ~(B 'a) ~(Y 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `BYOC__O)     'dependent-extensible|__3)
     (reify* [~(csym `BYOC__O)]
       (~(O 'invoke) [~'_3__  ~(B 'a) ~(Y 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `BYOO__O)   'dependent-extensible|__4)
     (reify* [~(csym `BYOO__O)]
       (~(O 'invoke) [~'_4__  ~(B 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `BYOO__O)   'dependent-extensible|__5)
     (reify* [~(csym `BYOO__O)]
       (~(O 'invoke) [~'_5__  ~(B 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `BSSS__O)    'dependent-extensible|__6)
     (reify* [~(csym `BSSS__O)]
       (~(O 'invoke) [~'_6__  ~(B 'a) ~(S 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `BCSC__O)      'dependent-extensible|__7)
     (reify* [~(csym `BCSC__O)]
       (~(O 'invoke) [~'_7__  ~(B 'a) ~(C 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `BCOC__O)     'dependent-extensible|__8)
     (reify* [~(csym `BCOC__O)]
       (~(O 'invoke) [~'_8__  ~(B 'a) ~(C 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `BOSO__O)  'dependent-extensible|__9)
     (reify* [~(csym `BOSO__O)]
       (~(O 'invoke) [~'_9__ ~(B 'a) ~(O 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `BOOO__O) 'dependent-extensible|__10)
     (reify* [~(csym `BOOO__O)]
       (~(O 'invoke) [~'_10__ ~(B 'a) ~(O 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `BOOO__O) 'dependent-extensible|__11)
     (reify* [~(csym `BOOO__O)]
       (~(O 'invoke) [~'_11__ ~(B 'a) ~(O 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `YYSS__O)        'dependent-extensible|__12)
     (reify* [~(csym `YYSS__O)]
       (~(O 'invoke) [~'_12__ ~(Y 'a) ~(Y 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `YYSC__O)         'dependent-extensible|__13)
     (reify* [~(csym `YYSC__O)]
       (~(O 'invoke) [~'_13__ ~(Y 'a) ~(Y 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `YYSO__O)       'dependent-extensible|__14)
     (reify* [~(csym `YYSO__O)]
       (~(O 'invoke) [~'_14__ ~(Y 'a) ~(Y 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `YYOC__O)        'dependent-extensible|__15)
     (reify* [~(csym `YYOC__O)]
       (~(O 'invoke) [~'_15__ ~(Y 'a) ~(Y 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `YYOO__O)      'dependent-extensible|__16)
     (reify* [~(csym `YYOO__O)]
       (~(O 'invoke) [~'_16__ ~(Y 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `YYOO__O)      'dependent-extensible|__17)
     (reify* [~(csym `YYOO__O)]
       (~(O 'invoke) [~'_17__ ~(Y 'a) ~(Y 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `SSSS__O)      'dependent-extensible|__18)
     (reify* [~(csym `SSSS__O)]
       (~(O 'invoke) [~'_18__ ~(S 'a) ~(S 'b) ~(S 'c) ~(S 'd)] 1)))
   (def ~(tag (cstr `CCSC__O)         'dependent-extensible|__19)
     (reify* [~(csym `CCSC__O)]
       (~(O 'invoke) [~'_19__ ~(C 'a) ~(C 'b) ~(S 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `CCOC__O)        'dependent-extensible|__20)
     (reify* [~(csym `CCOC__O)]
       (~(O 'invoke) [~'_20__ ~(C 'a) ~(C 'b) ~(O 'c) ~(C 'd)] 1)))
   (def ~(tag (cstr `OOSO__O)   'dependent-extensible|__21)
     (reify* [~(csym `OOSO__O)]
       (~(O 'invoke) [~'_21__ ~(O 'a) ~(O 'b) ~(S 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `OOOO__O)  'dependent-extensible|__22)
     (reify* [~(csym `OOOO__O)]
       (~(O 'invoke) [~'_22__ ~(O 'a) ~(O 'b) ~(O 'c) ~(O 'd)] 1)))
   (def ~(tag (cstr `OOOO__O)  'dependent-extensible|__23)
     (reify* [~(csym `OOOO__O)]
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
                            (def ~(tag (cstr `D__O) 'extensible|__0)
                              ( [D__O] (~(O 'invoke) [~'_0__ ~(D 'a)] nil)))

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
                :clj ($ (do (def ~(tag (cstr `B__O) 'extensible|__1)
                              (reify* [~(csym `B__O)]
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
                           ([a (t/input simple-reactive-dependee :?)] "abc")))])
                    eval)
            expected
              (case (env-lang)
                :clj ($ [(do (declare ~'simple-reactive-dependee)
                             (def ~(tag (cstr `C__O) 'simple-reactive-dependee|__0)
                               (reify* [~(csym `C__O)] (~(O 'invoke) [~'_0__ ~(C 'a)] 1)))
                             [{:id 0 :index 0 :arg-types [(t/isa? Character)] :output-type t/any?}]
                             (defmeta ~'simple-reactive-dependee
                               {:quantum.core.type/type simple-reactive-dependee|__type}
                               (fn* ([~'x00__]
                                 (ifs ((Array/get simple-reactive-dependee|__0|types 0) ~'x00__)
                                      (. simple-reactive-dependee|__0 ~'invoke ~'x00__)
                                      (unsupported! `simple-reactive-dependee [~'x00__] 0))))))
                         (do (declare ~'simple-reactive-dependent)
                             (def ~(tag (cstr `C__O) 'simple-reactive-dependent|__0)
                               (reify* [~(csym `C__O)] (~(O 'invoke) [~'_0__ ~(C 'a)] "abc")))
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
                    ([a (t/input dependent-extensible :?)]))))])))
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
                              (t/- @(t/input* abcde :?) tt/long?)
                              (t/arglist-type (t/or tt/short? tt/string?))))))))
    b (t/rx (t/or tt/byte?
              (t/arglist-type
                (t/or tt/char?
                      (t/- @(t/input* abcde :?) tt/long?)
                      (t/arglist-type (t/or tt/short? tt/string?))))))
    c (t/or tt/short? tt/string?)
    d (t/rx (t/or tt/char?
                  (t/- @(t/input* abcde :?) tt/long?)
                  (t/arglist-type (t/or tt/short? tt/string?))))
    > (t/rx (t/or (t/arglist-type
                    (t/or tt/byte?
                          (t/arglist-type
                            (t/or tt/char?
                                  (t/- @(t/input* abcde :?) tt/long?)
                                  (t/arglist-type (t/or tt/short? tt/string?))))))
                  (t/or tt/char?
                        (t/- @(t/input* abcde :?) tt/long?)
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
      - (do (def abcde|__0 (reify* [~(csym `I__L)] (invoke ([x00__ a] ...))))
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
      ([c (t/input ns0/abcde :?) > (t/output ns0/abcde (t/type c))] ...))
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
                        (let [t0 (t/rx (t/input* @ns0/abcde|__type :?))]
                          {:ns                'ns1
                           ;; This is only present when there is at least one dependent type in the
                           ;; arglist / output
                           :args-form         (om 'c '(t/input ns0/abcde :?))
                           :arg-types-basis   [t0]
                           ;; This is only present when there is at least one dependent type in the
                           ;; arglist / output
                           :output-type|form  '(t/output ns0/abcde (t/type c))
                           :output-type|basis (t/rx (t/output* @ns0/abcde|__type @t0))
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
      - (do (def fghij|__0 (reify* [~(csym `I__L)]      (invoke ([x00__ b] ...))))
            (def fghij|__1 (reify* [~(csym `O__O)] (invoke ([x00__ c] ...))))
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
  (testing "Nested fns"
    (let [actual (binding [self/*compilation-mode* :test]
                   (macroexpand '
                     ;: FIXME this contract is not being held up when returning nil
                     (self/defn f0|test [a (t/or tt/boolean? tt/double?)
                                         ;> (t/ftype [tt/byte? :> (t/ftype [tt/char?])])
                                         ]
                       ;; TODO this fits into a larger scheme of, should we have output types be
                       ;; `(t/and actual declared)` or should we just have them be `declared`? The
                       ;; latter is easier but it seems like the `t/fn` dispatch forces our hand
                       ;; towards the former. We need to think about this more.
                       (self/fn [b (t/or tt/byte? tt/char?)
                                 ;> (t/ftype [(t/or (t/type a) tt/short?)])
                                 ]
                         (self/fn f1|test [c (t/or (t/type a) tt/short?)]
                           b (f1|test a) (f1|test c))))))
          expected
           (case (env-lang)
             :clj
             ($ (do (defmeta-from ~'f0|test
                  (let* [~'f0|test|__fs (*<>|sized 2)
                         ~'f0|test
                           (new TypedFn
                             {:quantum.core.type/type f0|test|__type}
                             f0|test|__ts
                             ~'f0|test|__fs
                             (fn* ([~&ts ~&fs ~'x00__]
                                    (ifs (~(aget* (aget* &ts 0) 0) ~'x00__)
                                         (. ~(aget* &fs 0) ~'invoke ~'x00__)
                                         (~(aget* (aget* &ts 1) 0) ~'x00__)
                                         (. ~(aget* &fs 1) ~'invoke ~'x00__)
                                         (unsupported! `f0|test [~'x00__] 0)))))]
                   ~(aset* 'f0|test|__fs 0
                     `(reify* [~(csym `B__O)]
                        (~'invoke [~&this ~(B 'a)]
                          ;; From `(self/fn [b ...])`
                          (let* [~'__anon0__|__fs (*<>|sized 2)
                                 ~'__anon0__
                                   (new TypedFn nil
                                     ;; TODO perhaps extern this (and parts thereof) whenever
                                     ;; possible in `let*` statement on the very outside of the fn
                                     ;; (so around the outer `reify*`) ?
                                     (*<> (*<> t/byte?) (*<> t/char?))
                                     ~'__anon0__|__fs
                                     (fn* ([~&ts ~&fs ~'x00__]
                                            (ifs (~(aget* (aget* ~&ts 0) 0) ~'x00__)
                                                 (. ~(>Y__O (aget* &fs 0)) ~'invoke ~'x00__)
                                                 (~(aget* (aget* ~&ts 1) 0) ~'x00__)
                                                 (. ~(>C__O (aget* &fs 1)) ~'invoke ~'x00__)
                                                 (unsupported! [~'x00__] 0)))))]
                           ~(aset* '__anon0__|__fs 0
                             `(reify* [~(csym `Y__O)]
                                (~'invoke [~'_0__ ~(Y 'b)]
                                  ;; From `(self/fn [c ...])`
                                  (let* [~'f1|test|__fs (*<>|sized 2)
                                         ~'f1|test
                                           (new TypedFn nil
                                             (*<> (*<> t/boolean?) (*<> t/short?))
                                             ~'f1|test|__fs
                                             (fn* ([~&ts ~&fs ~'x00]
                                                    (ifs (~(aget* (aget* &ts 0) 0) ~'x00__)
                                                         (. ~(>B__O (aget* &fs 0)) ~'invoke ~'x00__)
                                                         (~(aget* (aget* &ts 1) 0) ~'x00__)
                                                         (. ~(>S__O (aget* &fs 1)) ~'invoke ~'x00__)
                                                         (unsupported! [~'x00__] 0))))))]
                                    ~(aset* 'f1|test|__fs 0
                                      `(reify* [~(csym `B__O)]
                                         (~'invoke [~&this (B 'c)]
                                           ~'b
                                           (. ~(>B__O (aget* 'f1|test|__fs 0)) ~'invoke ~'a)
                                           (. ~(>B__O (aget* 'f1|test|__fs 0)) ~'invoke ~'c))))
                                    ~(aset* 'f1|test|__fs 1
                                      `(reify* [~(csym `S__O)]
                                         (~'invoke [~&this (S 'c)]
                                           ~'b
                                           (. ~(>B__O (aget* 'f1|test|__fs 0)) ~'invoke ~'a)
                                           (. ~(>S__O (aget* 'f1|test|__fs 1)) ~'invoke ~'c))))
                                    ~'f1|test)))
                             ~(aset* '__anon0__|__fs 1
                                (reify* [~(csym `C__O)]
                                  (~'invoke [~&this ~(C 'a)] ...)))
                             ~'__anon0__))))
                   ~(aset* 'f0|test|__fs 1
                     `(reify* [~(csym `D__O)]
                        (~'invoke [~&this ~(D 'a)] ...)))
                   ~'f0|test))))]))
  (testing "Calling fns"
    (let [actual (binding [self/*compilation-mode* :test]
                   (macroexpand '
                     (do (self/defn g|test [f0 (t/ftype [tt/long? :> tt/float?]) > tt/float?]
                           (f0 5))
                         (self/defn h|test [f0 (t/ftype [tt/string? :> tt/char?]) > (t/type f0)]
                           f0)
                         ;; This won't compile
                       #_(self/defn i|test [f0 (t/ftype  [tt/string? :> tt/char?])
                                            >  (t/ftype' [tt/string? :> tt/char?])]
                           f0)
                         (self/defn i|test [f0 (t/ftype' [tt/string? :> tt/char?]) > (t/type f0)]
                           f0)
                         (self/defn j|test [f0 (t/ftype  [tt/long?   :> tt/float?]
                                                         [tt/string? :> tt/char?])
                                            f1 (t/ftype  [tt/byte?   :> tt/boolean?]
                                                         [tt/long?   :> tt/char?]
                                                         [tt/string? :> tt/char?])
                                            f2 (t/ftype' [tt/long?   :> tt/float?])
                                            >  tt/char?]
                           (f0 7)
                           (f1 21)
                           (g|test f0)
                           ((h|test f0) "63")
                           ;; This won't compile
                         #_((i|test f0) "98")
                           ((i|test ^:wrap f0) "98")
                           (j|test f1 f0)
                           ;; FIXME for `t/ftype` comparison: what if `f1` also has the overload
                           ;; `[(t/and tt/string? (fn-> count (= 2))) :> tt/double?]`?
                           ;; Then yes `f1` accepts at least `tt/string?`, which outputs no more
                           ;; than `tt/boolean?`, but it's not clear whether `tt/double?` or `tt/char?` gets output
                           (f1 "11")))))
          expected
            (case (env-lang)
              :clj
          ($ (do (defmeta-from ~'g|test
                   (let* [~'g|test|__fs (*<>|sized 1)
                          ~'g|test
                            (new TypedFn
                              {:quantum.core.type/type ~'g|__type}
                              ...
                              ~'g|test|__fs
                              (fn* ([~&ts ~&fs ~'x00]
                                     (ifs (~(aget* (aget* &ts 0) 0) ~'x00__)
                                          (. ~(aget* &fs 0) ~'invoke ~'x00__)
                                          (unsupported! `g|test [~'x00__] 0)))))]
                     ~(aset* g|test|__fs 0
                       `(reify* [~(csym `OI__F)]
                          (~'invoke [~&this ~(O 'f0)] (~'f0 5))))
                     ~'g|test))
                 (defmeta-from ~'h|test
                   (let* [~'h|test|__fs (*<>|sized 1)
                          ~'h|test
                            (new TypedFn
                              {:quantum.core.type/type ~'h|__type}
                              ...
                              ~'h|test|__fs
                              (fn* ([~&ts ~&fs ~'x00]
                                     (ifs (~(aget* (aget* &ts 0) 0) ~'x00__)
                                          (. ~(aget* &fs 0) ~'invoke ~'x00__)
                                          (unsupported! `h|test [~'x00__] 0)))))]
                     ~(aset* h|test|__fs 0
                       `(reify* [~(csym `O__O)]
                          (~'invoke [~&this ~(O 'f0)] f0)))
                     ~'h|test))
                 (defmeta-from ~'i|test
                   (let* [~'i|test|__fs (*<>|sized 1)
                          ~'i|test
                            (new TypedFn
                              {:quantum.core.type/type ~'i|__type}
                              ...
                              ~'i|test|__fs
                              (fn* ([~&ts ~&fs ~'x00]
                                     (ifs (~(aget* (aget* &ts 0) 0) ~'x00__)
                                          (. ~(aget* &fs 0) ~'invoke ~'x00__)
                                          (unsupported! `i|test [~'x00__] 0)))))]
                     ~(aset* i|test|__fs 0
                       `(reify* [~(csym `O__O)]
                          (~'invoke [~&this ~(O 'f0)] f0)))
                     ~'i|test))
                       ;; to avoid var indirection
                 (let* [~'g|test|__ (deref ~(list 'var `g|test))
                        ~'h|test|__ (deref ~(list 'var `h|test))
                        ~'i|test|__ (deref ~(list 'var `i|test))]
                   (defmeta-from ~'j|test
                     (let* [~'j|test|__fs (*<>|sized 1)
                            ~'j|test
                              (new TypedFn
                                {:quantum.core.type/type ~'j|__type}
                                ...
                                ~'j|test|__fs
                                (fn* ([~&ts ~&fs ~'x00__ ~'x01__]
                                       (ifs (~(aget* (aget* &ts 0) 0) ~'x00__)
                                            (ifs (~(aget* (aget* &ts 0) 1) ~'x00__)
                                                 (. ~(aget* &fs 0) ~'invoke ~'x00__ ~'x01__)
                                                 (unsupported! `j|test [~'x00__ ~'x01__] 1))
                                            (unsupported! `j|test [~'x00__ ~'x01__] 0)))))]
                      ;; NOTE we could accommodate for the situation in which a reify overload gets
                      ;; redefined (same ID): to ensure the interface stays the same, we could
                      ;; ensure that each interface has (arbitrarily) up to 10 extra params, with an
                      ;; int array as the last extra param which is then unpacked. However, this
                      ;; would not accommodate for the facts that 1) the meaning of each int param
                      ;; could change, and even if this were able to be controlled, 2) existing call
                      ;; sites using that overload would either have too many or few params. Of
                      ;; course, we could add a default interface implementation for every supported
                      ;; arity, each one of which would curry in the params to the actually
                      ;; implemented arity (there are other ways too probably). But it just seems
                      ;; like a lot of overhead with not too much gain.
                      ;; (reify* [~(csym `OOIII__C)]
                      ;;   (~'invoke [~&this ~(O 'f0) ~(O 'f1)
                      ;;              ~(I 'i__0)  ; overload ID of `f0` : `[tt/long?]`
                      ;;              ~(I 'i__2)  ; overload ID of `f1` : `[tt/long?]`
                      ;;              ~(I 'i__5)] ; overload ID of `f1` : `[tt/string?]`
                      ;;     ...))
                      ~(aset* j|test|__fs 0
                       `(reify* [~(csym `OO__C)]
                          (~'invoke [~&this ~(O 'f0) ~(O 'f1)]
                            (~'f0 7)
                            (. ~(aget* `(. ~'f2 ~'fs) 0) ~'invoke 21)
                            ;; - It doesn't just refer to `g|test|__fs` or whatever because the fn
                            ;;   in question (`g|test`) is extensible. Otherwise it would skip the
                            ;;   ceremony of `(aget* `(. ~'g|test|__ ~'fs) 0)` and just do e.g.
                            ;;   `g|test|__0`.
                            (. ~(aget* `(. ~'g|test|__ ~'fs) 0) ~'invoke ~'f0)
                            ;; We need to know how to call the resulting fn and the callee can't
                            ;; provide that information to all callers. So we have to make this call
                            ;; dynamic, sadly.
                            ((. ~(aget* `(. ~'h|test|__ ~'fs) 0) ~'invoke ~'f0) "63")
                            (. ~(aget* `(. ~(aget* `(. ~'i|test|__ ~'fs) 0) ~'invoke
                                           ;; Two arrays (`ts` — two arrays, `fs` — two `reify`s),
                                           ;; one `TypedFunction` object. Not super cheap
                                           (self/fn ([~'x0__ tt/long?   ~'> tt/float?] (f0 x0__))
                                                    ([~'x0__ tt/string? ~'> tt/char?]  (f0 x0__))))
                                       1)
                               ~'invoke "98")
                            ;; - It doesn't just refer to `j|test|__fs` because the fn in question
                            ;;   (`j|test`) is extensible. Otherwise it would skip the ceremony of
                            ;;   `(aget* `(. ~'j|test|__f ~'fs) 0)` and just do e.g. `j|test|__0`.
                            (. ~(aget* `(. ~'j|test ~'fs) 0) ~'invoke ~'f1 ~'f0)
                            ;; - Knows that calling `f1` with `["11"]` means:
                            ;;   - We need to know overload ID of `f1` with input types
                            ;;     `[tt/string?]`
                            (f1 "11"))))
                       ~'j|test)))))))]
              ...)))

;; Automatically analyzes the body of `fnt?`s which have at least one input of `t/fn?`
;; Meaning, the body is analyzed as if inline, but if not marked `inline`, will intern a separate fn
;; for the purpose
(t/defn- apply* [f (t/meta-or t/fnt? t/fn?), !xs !alist?]
  (case (count !xs)
    0  (f !xs)
    1  (f (get !xs 0))
    2  (f (get !xs 0)  (get !xs 1))
    3  (f (get !xs 0)  (get !xs 1)  (get !xs 2))
    4  (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3))
    5  (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4))
    6  (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5))
    7  (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6))
    8  (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7))
    9  (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8))
    10 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9))
    11 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9)
          (get !xs 10))
    12 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9)
          (get !xs 10) (get !xs 11))
    13 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 9)  (get !xs 10)
          (get !xs 10) (get !xs 11) (get !xs 12))
    14 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 9)  (get !xs 10)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13))
    15 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 9)  (get !xs 10)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14))
    16 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 9)  (get !xs 10)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
          (get !xs 15))
    17 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 9)  (get !xs 10)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
          (get !xs 15) (get !xs 16))
    18 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 9)  (get !xs 10)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
          (get !xs 15) (get !xs 16) (get !xs 17))
    19 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 9)  (get !xs 10)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
          (get !xs 15) (get !xs 16) (get !xs 17) (get !xs 18))
    20 (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
          (get !xs 15) (get !xs 16) (get !xs 17) (get !xs 18) (get !xs 19))
    (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
       (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9)
       (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
       (get !xs 15) (get !xs 16) (get !xs 17) (get !xs 18) (get !xs 19)
       (let [variadic-ct (-> !xs count (- 20))
             !variadics  (<>*|ct variadic-ct)]
         (loops/dotimes [i variadic-ct]
           (assoc! !variadics i (get !xs i)))))))

;; Assuming collections.core is available
(t/defn ^:inline apply
  ([f fn?, xs reducible?]
    (apply* f (>!alist xs)))
  ([f fn?, x0 ?,                    xs reducible?]
    (apply* f (join! (!alist x0)          xs)))
  ([f fn?, x0 ?, x1 ?,              xs reducible?]
    (apply* f (join! (!alist x0 x1)       xs)))
  ([f fn?, x0 ?, x1 ?, x2 ?,        xs reducible?]
    (apply* f (join! (!alist x0 x1 x2)    xs)))
  ([f fn?, x0 ?, x1 ?, x2 ?, x3 ? & xs reducible?]
    (apply* f (join! (!alist x0 x1 x2 x3) xs))))

;; Now *this* is where type inference would come in handy...
(self/defn comp
  (^:inline [> (t/value identity)] identity)
  (^:inline [f0 t/fnt? > (t/type f0)] f0)
  ([f0 (t/ftype [(t/output f1 :any)]), f1 t/fnt?]
    (self/fn
      ([]
        (f0 (f1)))
      ([x0 (t/input f1 :?)]
        (f0 (f1 x0)))
      ([x0 (t/input f1 :? :_), x1 (t/input g (t/type x0) :?)]
        (f0 (f1 x0 x1)))
      ([x0 (t/input f1 :?          :_          :_)
        x1 (t/input f1 (t/type x0) :?          :_)
        x2 (t/input f1 (t/type x0) (t/type x1) :?)]
        (f0 (f1 x0 x1 x2)))
      ([x0 (t/input f1 :?                         :&)
        x1 (t/input f1 (t/type x0) :?             :&)
        x2 (t/input f1 (t/type x0) (t/type x1) :? :&)
        & xs (t/input f1 (t/type x0) (t/type x1) (t/type x2) :?&)]
        (f0 (apply f1 x0 x1 x2 xs)))))
  ([f0 f1 & fs] (reduce1 comp (list* f0 f1 fs))))

(self/defn comp
  ;; `> ?` is everywhere implied
  (^:inline [] identity)
  (^:inline [f0 t/fnt?] f0)
  ([f0 ?, f1 ?]
    (self/fn
      ([]                       (f0       (f1)))
      ([x0 ?]                   (f0       (f1 x0)))
      ([x0 ?, x1 ?]             (f0       (f1 x0 x1)))
      ([x0 ?, x1 ?, x2 ?]       (f0       (f1 x0 x1 x2)))
      ([x0 ?, x1 ?, x2 ?, xs ?] (f0 (apply f1 x0 x1 x2 xs))))))

;; Type inference would come in handy here too
(self/defn aritoid
  ([f0 (t/ftype [])
    >  (t/ftype [:> (t/output f0)])]
    (self/fn ([>  (t/output f0)] (f0))))
  ([f0 (t/ftype [])
    f1 (t/ftype [t/eps?]) ; needs to be `t/eps?` meaning just an 'epsilon' above `t/none?` in order to be a valid input, but still `t/<` w.r.t. everything else
    >  (t/ftype [:> (t/output f0)]
                [:> (t/output f1 :_)])]
    (self/fn ([>  (t/output f0)]             (f0))
             ([x0 (t/input  f1 :?)
               >  (t/output f1 (t/type x0))] (f1 x0))))
  ([f0 (t/ftype [])
    f1 (t/ftype [t/none?])
    f2 (t/ftype [t/none? t/none?])
    >  (t/ftype [:> (t/output f0)]
                [:> (t/output f1 :_)]
                [:> (t/output f2 :_ :_)])]
    (self/fn ([>  (t/output f0)]                         (f0))
             ([x0 (t/input  f1 :?)
               >  (t/output f1 (t/type x0))]             (f1 x0))
             ([x0 (t/input  f2 :?          :_)
               x1 (t/input  f2 (t/type x0) :?)
               >  (t/output f1 (t/type x0) (t/type x1))] (f2 x0 x1)))))

(self/defn aritoid
  ;; `> ?` is everywhere implied
  ([f0 ?]
    (self/fn ([] (f0))))
  ([f0 ?, f1 ?]
    (self/fn ([]     (f0))
             ([x0 ?] (f1 x0))))
  ([f0 ?, f1 ?, f2 ?]
    (self/fn ([]           (f0))
             ([x0 ?]       (f1 x0))
             ([x0 ?, x1 ?] (f2 x0 x1)))))

;; ===== Inner expansion ===== ;;

(t/dotyped (apply inc [1]))
-> (let* [f inc, xs [1]]
     (apply* f (>!alist xs)))
-> (let* [f inc, xs [1]]
     (let* [!xs (>!alist xs)]
       ;; This part isn't really analyzed in whole; just `(count !xs)` first
       (case (count !xs) ; figures out that the count is 1
         0  (f !xs)
         1  (f (get !xs 0))
         ...)))
-> (let* [f inc, xs [1]]
     (let* [f f, !xs (>!alist xs)]
       (f (get !xs 0)))) ; maybe it could figure out that `(get !xs 0)` yields `1`?
A -> (let* [f inc, xs [1]]
       (let* [f f, !xs (>!alist xs)]
         (f (.get !xs 0))))
B -> (let* [f inc, xs [1]]
       (let* [f f, !xs (>!alist xs)] ; eliminate unused, non-side-effecting (since fn `>!alist` not marked with `:!`) param (`!xs`). Don't warn about unused param since this is only in an "inner expansion"
         (f 1)))
     -> (let* [f inc, xs [1]]
          (let* [f f] ; eliminate iso-binding
            (f 1)))
     -> (let* [f inc, xs [1]] (f 1)) ; eliminate unused, non-side-effecting (since fn `>!alist` not marked with `:!`) binding (`xs`). Don't warn about unused param since this is only in an "inner expansion"
     -> (inc 1)
     -> 2

(t/dotyped (apply + (range 25)))
-> (apply + (clojure.lang.LongRange/create 25)) ; retains metadata about count, etc.
-> (let* [f +, xs (clojure.lang.LongRange/create 25)]
     (apply* f (>!alist xs))) ; expanded because `(t/type f)` `t/<` `(t/type apply.f)`
-> (let* [f +, xs (clojure.lang.LongRange/create 25)]
     (let* [!xs (>!alist xs)]
       ;; This part isn't really analyzed in whole; just `(count !xs)` first
       (case (count !xs) ; figures out that the count is 25
         ...
         ;; This is not supposed to be inlined, just held out
         (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
            (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9)
            (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
            (get !xs 15) (get !xs 16) (get !xs 17) (get !xs 18) (get !xs 19)
            (let [variadic-ct (-> !xs count (- 20))
                  !variadics  (<>*|ct variadic-ct)]
              (loops/dotimes [i variadic-ct]
                (assoc! !variadics i (get !xs i))))))))
-> (let* [f +, xs (clojure.lang.LongRange/create 25)]
     (let* [!xs (>!alist xs)] ; knows the count is 25
       ;; This is not supposed to be inlined, just held out
       (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
          (get !xs 15) (get !xs 16) (get !xs 17) (get !xs 18) (get !xs 19)
          (let [variadic-ct (-> 25 (- 20))
                !variadics  (<>*|ct variadic-ct)]
            (loops/dotimes [i variadic-ct]
              (assoc! !variadics i (get !xs i)))))))
-> (let* [f +, xs (clojure.lang.LongRange/create 25)]
     (let* [!xs (>!alist xs)] ; knows the count is 25
       ;; This is not supposed to be inlined, just held out
       (f (get !xs 0)  (get !xs 1)  (get !xs 2)  (get !xs 3)  (get !xs 4)
          (get !xs 5)  (get !xs 6)  (get !xs 7)  (get !xs 8)  (get !xs 9)
          (get !xs 10) (get !xs 11) (get !xs 12) (get !xs 13) (get !xs 14)
          (get !xs 15) (get !xs 16) (get !xs 17) (get !xs 18) (get !xs 19)
          (let [variadic-ct 5
                !variadics  (<>*|ct variadic-ct)]
            (loops/dotimes [i variadic-ct]
              (assoc! !variadics i (get !xs i)))))))
-> (let* [f +, xs (clojure.lang.LongRange/create 25)]
     (let* [!xs (>!alist xs)] ; knows the count is 25
       ;; - This is not supposed to be inlined, just held out
       ;; - Splices in the inlined fns here
       (f (. !xs get 0)  (. !xs get 1)  (. !xs get 2)  (. !xs get 3)  (. !xs get 4)
          (. !xs get 5)  (. !xs get 6)  (. !xs get 7)  (. !xs get 8)  (. !xs get 9)
          (. !xs get 10) (. !xs get 11) (. !xs get 12) (. !xs get 13) (. !xs get 14)
          (. !xs get 15) (. !xs get 16) (. !xs get 17) (. !xs get 18) (. !xs get 19)
          (let [variadic-ct 5
                !variadics  (<>*|ct variadic-ct)]
            (loops/dotimes [i variadic-ct] ; this will of course be expanded too
              (let* [!xs !variadics]
                (do (. RT aset !xs i (. !xs get i))
                    !xs)))))))
;; Assumes it's at a top-level place in the ns since `dotyped` was invoked in an untyped context
-> (let* [apply*|expansion|__0
            (reify* [OO__O]
              (invoke [&this f !xs]
                (let* [^ArrayList !xs !xs]
                  (f (. !xs get 0)  (. !xs get 1)  (. !xs get 2)  (. !xs get 3)  (. !xs get 4)
                     (. !xs get 5)  (. !xs get 6)  (. !xs get 7)  (. !xs get 8)  (. !xs get 9)
                     (. !xs get 10) (. !xs get 11) (. !xs get 12) (. !xs get 13) (. !xs get 14)
                     (. !xs get 15) (. !xs get 16) (. !xs get 17) (. !xs get 18) (. !xs get 19)
                     (let [variadic-ct 5
                           !variadics  (<>*|ct variadic-ct)]
                       (loops/dotimes [i variadic-ct] ; this will of course be expanded too
                         (let* [!xs !variadics]
                           (do (. RT aset !xs i (. !xs get i))
                               !xs))))))))]
     (let* [f +, xs (clojure.lang.LongRange/create 25)]
       (let* [!xs (>!alist xs)]
         (. apply*|expansion|__0 invoke f !xs))))

(t/fn [] (apply + (range 25)))
->       ;; because nothing is "higher" than the `t/fn`
         ;; TODO what if it's defined in an untyped context not in the ns?
   (let* [apply*|expansion|__0 ...]
     (reify* [...]
       (invoke [...]
         (let* [f +, xs (clojure.lang.LongRange/create 25)]
           (let* [!xs (>!alist xs)]
             (. apply*|expansion|__0 invoke f !xs))))))

(t/defn abc [] (t/fn [] (apply + (range 25))))
->       ;; derives from a queue-like collection of expansions found while analyzing `abc`
   (let* [apply*|expansion|__0 ...]
     ...
     (reify* [...]
       (invoke [...]
         (t/fn []
           (let* [f +, xs (clojure.lang.LongRange/create 25)]
             (let* [!xs (>!alist xs)]
               (. apply*|expansion|__0 invoke f !xs)))))))


(t/defn test|apply [xs reducible?]
  (apply inc xs))
-> (t/defn test|apply [xs reducible?]
     (let* [f inc, xs xs]
       (apply* inc (>!alist xs))))
-> Not analyzed because `reducible?` (type of `xs`) is not `t/<` `reducible?` (type of `apply*.xs`)

;; TODO Lazy compilation? Maybe just before the typed context when it gets used is when it can be compiled

(t/defn map|transducer [f t/fnt?]
  (t/fn [rf ?]
    (^:inline t/fn
      ([] (rf))
      ([ret ?] (rf ret))
      ([ret ?, input ?]
        (rf ret (f input))))))

(t/defn reduce
  (^:inline [rf rf?, init t/ref?, xs (t/isa? IReduce)]
    (.reduce xs rf init))) ; .reduce on IReduce will be handled specially

(t/defn transduce [xf ?, rf ?, init ?, xs ?]
  (let [f   (xf rf)
        ret (reduce f init xs)]
   (f ret)))

(t/dotyped (transduce (map|transducer inc) conj [1 2]))
-> (transduce
     ;; inner expansion
     (let* [f inc]
       ;; `?` are resolved
       (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
         (^:inline t/fn
           ;; We avoid resolving `?` return types since the body may change
           ([] (rf))
           ([ret (t/input rf :?)] (rf ret))
           ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
             (rf ret (f x))))))
     conj
     []
     [1 2])
-> ;; inner expansion
   (let* [xf (let* [f inc]
               (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
                 (^:inline t/fn
                   ([] (rf))
                   ([ret (t/input rf :?)] (rf ret))
                   ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                     (rf ret (f x))))))
          rf   conj
          init []
          xs   [1 2]]
     (let* [f   (xf rf)
            ret (reduce f init xs)]
       (f ret)))
-> (let* [xf (let* [f inc]
               (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
                 (^:inline t/fn
                   ([] (rf))
                   ([ret (t/input rf :?)] (rf ret))
                   ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                     (rf ret (f x))))))
          rf   conj
          init []
          xs   [1 2]]
     (let* [f   ;; inner expansion
                (let* [f inc]
                  ;; elides isobindings so no additional `let*`
                  (^:inline t/fn
                    ([] (rf))
                    ([ret (t/input rf :?)] (rf ret))
                    ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                      (rf ret (f x)))))
            ret (reduce f init xs)]
       (f ret)))
-> (let* [xf (let* [f inc]
               (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
                 (^:inline t/fn
                   ([] (rf))
                   ([ret (t/input rf :?)] (rf ret))
                   ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                     (rf ret (f x))))))
          rf   conj
          init []
          xs   [1 2]]
     (let* [f   (let* [f inc]
                  (^:inline t/fn
                    ([] []) ; expanded from `(conj)`
                    ([ret (t/input rf :?)] ret) ; expanded from `(conj ret)`
                    ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                      (rf ret (f x)))))
            ret (.reduce xs f init)] ; inlined; elides isobindings so no `let*`
       (f ret)))
-> (let* [xf (let* [f inc]
               (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
                 (^:inline t/fn
                   ([] (rf))
                   ([ret (t/input rf :?)] (rf ret))
                   ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                     (rf ret (f x))))))
          rf   conj
          init []
          xs   [1 2]]
     (let* [f   (let* [f inc]
                  (^:inline t/fn
                    ([] [])
                    ([ret (t/input rf :?)] ret)
                    ([ret (t/input rf :? (t/output f (t/type x)))
                      x   (t/and (t/input f :?) (t/element xs))] ; throw if `t/none?` results
                      ;; because the class of `x` was determined
                      (let* [x (. Numbers uncheckedLongCast x)]
                        (rf ret (. Numbers inc x))))))
            ;; `.reduce` is handled specially and does type checking ahead of time on `f` to avoid
            ;; runtime checks as much as possible
            ret (.reduce xs
                  (c/fn [ret x]
                    (let* [x (. Numbers uncheckedLongCast x)]
                      (rf ret (. Numbers inc x))))
                  init)]
       (f ret)))
-> (let* [xf (let* [f inc]
               (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
                 (^:inline t/fn
                   ([] (rf))
                   ([ret (t/input rf :?)] (rf ret))
                   ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                     (rf ret (f x))))))
          rf   conj
          init []
          xs   [1 2]]
     (let* [f   (let* [f inc]
                  (^:inline t/fn
                    ([] [])
                    ([ret (t/input rf :?)] ret)
                    ([ret (t/input rf :? (t/output f (t/type x)))
                      x   (t/and (t/input f :?) (t/element xs))] ; throw if `t/none?` results
                      ;; because the class of `x` was determined
                      (let* [x (. Numbers uncheckedLongCast x)]
                        (rf ret (. Numbers inc x))))))
            ;; `.reduce` is handled specially and does type checking ahead of time on `f` to avoid
            ;; runtime checks as much as possible
            ret (.reduce xs
                  (c/fn [ret x]
                    (let* [x (. Numbers uncheckedLongCast x)]
                      (rf ret (. Numbers inc x))))
                  init)]
       ret))
-> (let* [xf (let* [f inc]
               (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
                 (^:inline t/fn
                   ([] (rf))
                   ([ret (t/input rf :?)] (rf ret))
                   ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                     (rf ret (f x))))))
          rf   conj
          init []
          xs   [1 2]]
     (let* [f   (let* [f inc]
                  ;; TODO need to analyze this as if we're doing:
                  ;; (loop [i 0, ret init]
                  ;;   (if (>= i (count xs))
                  ;;       ret
                  ;;       (let* [x (nth xs)] (recur (below-fn ret x)))))
                  (^:inline t/fn
                    ([] [])
                    ([;; throws if `t/none?` results
                      ret (t/and (t/input rf :?)
                                 ...)] ret)
                    ([;; throws if `t/none?` results
                      ret (t/and (t/input rf :? (t/output f (t/type x)))
                                 (t/or (t/type init)
                                       ...))
                      ;; throws if `t/none?` results
                      x   (t/and (t/input f :?) (t/element xs))]
                      ;; because the class of `x` was determined
                      (let* [x (. Numbers uncheckedLongCast x)]
                        (rf ret (. Numbers inc x))))))]
       (.reduce xs
         (c/fn [ret x]
           (let* [x (. Numbers uncheckedLongCast x)]
             (. conj|__0 invoke ret (. Numbers inc x))))
         init)))
-> (let* [xf (let* [f inc]
               (t/fn [rf (t/ftype [] [t/any?] [t/any? (t/input f :?)])]
                 (^:inline t/fn
                   ([] (rf))
                   ([ret (t/input rf :?)] (rf ret))
                   ([ret (t/input rf :? (t/output f (t/type x))), x (t/input f :?)]
                     (rf ret (f x))))))
          rf   conj
          init []
          xs   [1 2]]
     (.reduce xs
       (c/fn [ret x]
         (let* [x (. Numbers uncheckedLongCast x)]
           (rf ret (. Numbers inc x))))
       init))
-> (let* [init []
          xs   [1 2]]
     (.reduce xs
       (c/fn [ret x]
         (let* [x (. Numbers uncheckedLongCast x)]
           (. conj|__0 invoke ret (. Numbers inc x))))
       init))
-> (let* [transduce|expanded__0
            (reify* [O__O]
              (invoke [&this init xs]
                (.reduce xs
                  (c/fn [ret x]
                    (let* [x (. Numbers uncheckedLongCast x)]
                      (. conj|__0 invoke ret (. Numbers inc x))))
                  init)))]
     (. transduce|expanded__0 invoke [] [1 2]))
;; So, did this actually help? Yes; we were able to elide a lot of checks/dynamism/overhead. Should
;; we use it in all occasions? Unclear. It's a lot of work to implement and the compilation might be
;; slow. It may be best to leave for an "advanced compilation" enhancement later. We can afford
;; dynamism as long as it's 'fast enough' — e.g. instead of `(t/boolean? x)` we expand to
;; `(instance? Boolean x)`. Clojure has this kind of dynamism with pretty much everything anyway.
;; The real issue is when we dynamically invoke an fn that does some more complex checks.
;; We should be able to have configurable levels of dynamism:
;; 1) advanced : eliminate as much dynamism as possible
;; 2) normal   : eliminate as much dynamism as possible for global fns only
;; 3) dynamic  : make everything dynamic, for faster compilation and thought processes

;; =========================

"
So `f1`'s declared type is `(t/ftype [t/eps?])`, meaning, it has to at least accept 1 input, whose
type is `t/>=` `t/eps?`.
Then really we're just doing arity checking here. Input and output type checks become kind of
meaningless unless you know the actual input type.




To fix this passed fn specificity problem, we could leverage a global map of the comparison of all
types to all types (`O(n^2)` worst case space, but I think we can do it in less).

Let's say fimpl is (t/ftype [p/byte? (t/and p/string? whatever)]
                            [p/byte? p/string?])
How do we get `(f a)` to recognize that it needs to go to overload 0, not 1?
We could have some sort of interval tree, or perhaps a map sorted by comparisons, in which we can
have log-time access to the right thing — like if you hand it the hash of `p/byte?` `p/string?` then
it goes to the first thing matching that. That way we don't have to rely on dynamism absolutely
everywhere.

This still doesn't get around other issues seen in `aritoid` and `comp`. TODO
"

"
We could do:
`(. ^TheReifyType (aget (.-ts f) <index>) invoke <~@args>)`
- This is all fine when `f` is `=` (perhaps `t/=`?) to the declared type, but when it's `t/<`, it
  may allow for more than what the declared type requires, in which case it may have more and/or
  different overloads. So do something like this:
   (t/defn a [f (t/ftype [t/long?])] (f 1))
   -> (def a|__0 (reify [_ ^TypedFn f ^int f|__i] (.invoke ^L__O (RT/aget (.-overloads f) f|__i) 1)))
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

;; ===== Failed approaches ===== ;;
"
To fix this passed fn specificity problem, we could leverage either inheritance or a global map of
the comparison of all types to all types (`O(n^2)` worst case space, but I think we can do it in
less).

For instance,
(t/defn a [f (t/ftype [p/byte? p/string?])]
  (f a))

F = (t/ftype [p/byte? (t/and p/string? (fn-> count (= 5)))])
- `p/byte?`'s hash is -627458773 and `(t/and p/string? (fn-> count (= 5))`'s hash is -123456
So FClass implements _-627458773_-123456

G = (t/ftype [p/byte? p/string?])
- `p/byte?`'s hash is -627458773 and `p/string?`'s hash is -1854681952
So GClass implements _-627458773_-1854681952

But now _-627458773_-123456 needs to extend _-627458773_-1854681952, which requires a rewrite of all
things that use it... so I don't think inheritance is our answer.
"

;; TODO quantum.test.untyped.core.defnt should be incorporated here — includes some destructuring
