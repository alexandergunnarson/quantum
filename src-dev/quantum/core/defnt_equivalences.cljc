;; See https://jsperf.com/js-property-access-comparison — all property accesses (at least of length 1) seem to be equal

(ns quantum.core.test.defnt-equivalences
  (:refer-clojure :exclude [name])
  (:require
    [clojure.core              :as c]
    [quantum.core.defnt
      :refer [analyze defnt fnt|code *fn->spec]]
    [quantum.core.macros.core
      :refer [$]]
    [quantum.core.macros
      :refer [macroexpand-all case-env env-lang quote+]]
    [quantum.core.macros.type-hint
      :refer [tag]]
    [quantum.core.spec         :as s]
    [quantum.core.test         :as test
      :refer [deftest testing is is= throws]]
    [quantum.untyped.core.analyze.expr :as xp]
    [quantum.untyped.core.collections.diff :as diff
      :refer [diff]]
    [quantum.untyped.core.core :as ucore
      :refer [code=]]
    [quantum.untyped.core.type :as t
      :refer [? !]])
  (:import clojure.lang.Named
           clojure.lang.Reduced
           quantum.core.data.Array
           quantum.core.Primitive))

(require '[quantum.core.spec :as s]
         '[quantum.core.fn :refer [fn->]])

;; =====|=====|=====|=====|===== ;;

(is (code=

;; ----- implementation ----- ;;

(macroexpand '
  (defnt pid > (? t/string?) []
     (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
          (.getName))))

;; ----- expanded code ----- ;;

($ (do (swap! *fn->spec assoc `pid
         (xp/>expr
           (fn [args##]
             (case (count args##) 0 nil))))
       (def ~'pid|__0
         (reify >java|lang|String
           (~(tag "java.lang.String" 'invoke) [~'_]
             (~'->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
                    (.getName)))))))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

;; ----- implementation ----- ;;

;; TODO it needs to vary return classes of the overloads with the input
(macroexpand '
(defnt identity|gen|uninlined ([x t/any?] x))
)

;; ----- expanded code ----- ;;

($ (do #_(swap! *fn->spec assoc `identity|gen|uninlined
         (xp/>expr
           (fn [args##] (case (count args##) 1 nil #_(fn-> first t/->spec)))))

       ~(case (env-lang)
                     ;; Because for `any?` it includes primitives as well
          :clj  ($ (do ;; Direct dispatch
                       ;; One reify per overload
                       (def ~'identity|gen|uninlined|__0 ; `t/any?`
                         (reify boolean>boolean (~(tag "boolean"          'invoke) [~'_ ~(tag "boolean"          'x)] ~'x)
                                byte>byte       (~(tag "byte"             'invoke) [~'_ ~(tag "byte"             'x)] ~'x)
                                short>short     (~(tag "short"            'invoke) [~'_ ~(tag "short"            'x)] ~'x)
                                char>char       (~(tag "char"             'invoke) [~'_ ~(tag "char"             'x)] ~'x)
                                int>int         (~(tag "int"              'invoke) [~'_ ~(tag "int"              'x)] ~'x)
                                long>long       (~(tag "long"             'invoke) [~'_ ~(tag "long"             'x)] ~'x)
                                float>float     (~(tag "float"            'invoke) [~'_ ~(tag "float"            'x)] ~'x)
                                double>double   (~(tag "double"           'invoke) [~'_ ~(tag "double"           'x)] ~'x)
                                Object>Object   (~(tag "java.lang.Object" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)] ~'x)))
                       ;; Dynamic dispatch (invoked only if incomplete type information (incl. in untyped context))
                       ;; in this case no protocol is necessary because it boxes arguments anyway
                       ;; Var indirection may be avoided by making and using static fields via the Clojure 1.8 flag
                       (defn ~'identity|gen|uninlined [~'x] (.invoke identity|gen|uninlined|__0 ~'x))))
          :cljs ;; Direct dispatch will be simple functions, not `reify`s; not necessary here
                ;; Dynamic dispatch will be approached later; not clear yet whether there is a huge savings
                ($ (defn ~'identity|gen|uninlined [~'x] ~'x))))))

)

;; =====|=====|=====|=====|===== ;;

;; TODO will deal with `inline` later
(defnt ^:inline identity|gen ([x t/any?] x))

;; ----- test ----- ;;

(deftest test|identity|gen
  (is= (identity|gen 1 ) 1 )
  (is= (identity|gen "") ""))

;; =====|=====|=====|=====|===== ;;

(is (code=

;; TODO don't ignore `:inline`
;; TODO `.getName` returns `(? string?)` so we need to add an assertion to guarantee
(macroexpand '
(defnt #_:inline name|gen
           ([x t/string? > t/string?    ] x)
  #?(:clj  ([x Named     > (! t/string?)] (.getName x))
     :cljs ([x INamed    > (! t/string?)] (-name x))))
)

;; ----- expanded code ----- ;;

($ (do (swap! *fn->spec assoc #'name|gen
         (xp/casef count
           1 (xp/condpf-> t/<= (xp/get 0)
               t/string? (fn-> t/->spec) ; TODO fix this
               ~(case (env-lang) :clj `Named :cljs `INamed) t/string?)))

       ~(case (env-lang)
          :clj  ($ (do ;; Only direct dispatch for primitives or for Object, not for subclasses of Object
                       ;; Return value can be primitive; in this case it's not
                       ;; The macro in a typed context will find the appropriate dispatch at compile time
                       (def ~'name|gen|__0
                         (reify java|lang|String>java|lang|String
                           (~(tag "java.lang.String"   'invoke) [~'_ ~(tag "java.lang.String"   'x)] ~'x)))
                       (def ~'name|gen|__1
                         (reify clojure|lang|Named>java|lang|String
                           (~(tag "clojure.lang.Named" 'invoke) [~'_ ~(tag "clojure.lang.Named" 'x)]
                             (let [~'out (.getName ~'x)]
                               (s/validate ~'out t/string?)))))

                       ;; This protocol is so suffixed because of the position of the argument on which
                       ;; it dispatches
                       (defprotocol name|gen__Protocol__0
                         (name|gen [~'x]))
                       (extend-protocol name|gen__Protocol__0
                         java.lang.String   (name|gen [x] (.invoke name|gen|__0 x))
                         ;; this is part of the protocol because even though `Named` is an interface,
                         ;; `String` is final, so they're mutually exclusive
                         clojure.lang.Named (name|gen [x] (.invoke name|gen|__1 x)))))
          :cljs ($ (do ;; No protocol in ClojureScript
                       (defn name|gen [~'x]
                         (cond* (string? x)           x
                                (satisfies? INamed x) (-name x)
                                (err! "Not supported for type" {:fn `name|gen :type (type x)}))))))))

))

;; =====|=====|=====|=====|===== ;;

;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
(macroexpand '
(defnt #_:inline some?|gen
  ([x t/nil?] false)
  ([x t/any?] true))
)

;; ----- expanded code ----- ;;

($ (do (swap! fn->spec assoc #'some?|gen
         (xp/casef c/count
           1 (xp/condpf-> t/<= (xp/get 0)
               t/nil? (t/value false)
               t/any? (t/value true ))))

       ~(case (env-lang)
          :clj  ($ (do (def some?|gen|__0 ; `nil?`
                         (reify Object>boolean  (^boolean invoke [_# ^java.lang.Object ~'x] false)))
                       (def some?|gen|__1 ; `t/any?`
                         (reify boolean>boolean (^boolean invoke [_# ^boolean          ~'x] true)
                                byte>boolean    (^boolean invoke [_# ^byte             ~'x] true)
                                short>boolean   (^boolean invoke [_# ^short            ~'x] true)
                                char>boolean    (^boolean invoke [_# ^char             ~'x] true)
                                int>boolean     (^boolean invoke [_# ^int              ~'x] true)
                                long>boolean    (^boolean invoke [_# ^long             ~'x] true)
                                float>boolean   (^boolean invoke [_# ^float            ~'x] true)
                                double>boolean  (^boolean invoke [_# ^double           ~'x] true)
                                Object>boolean  (^boolean invoke [_# ^java.lang.Object ~'x] true)))
                       ;; Dynamic dispatch
                       (defn some?|gen [~'x]
                         (cond* (nil? x) (.invoke some?|gen|__0 x)
                                (.invoke some?|gen|__1 x)))))
          :cljs `(do (defn some?|gen [~'x]
                       (cond* (nil? x) false
                              true))))))

;; =====|=====|=====|=====|===== ;;

;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
(macroexpand '
(defnt #_:inline reduced?|gen
  ([x Reduced] true)
  ([x t/any? ] false))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'reduced?|gen
       (xp/casef c/count
         1 (xp/condpf-> t/<= (xp/get 0)
             t/reduced? (t/value true)
             t/any?     (t/value false))))

     ~(case-env
        :clj  `(do (def reduced?|gen|__0 ; `Reduced`
                     (reify Object>boolean  (^boolean invoke [_# ^java.lang.Object ~'x] true)))
                   (def reduced?|gen|__1 ; `t/any?`
                     (reify boolean>boolean (^boolean invoke [_# ^boolean          ~'x] false)
                            byte>boolean    (^boolean invoke [_# ^byte             ~'x] false)
                            short>boolean   (^boolean invoke [_# ^short            ~'x] false)
                            char>boolean    (^boolean invoke [_# ^char             ~'x] false)
                            int>boolean     (^boolean invoke [_# ^int              ~'x] false)
                            long>boolean    (^boolean invoke [_# ^long             ~'x] false)
                            float>boolean   (^boolean invoke [_# ^float            ~'x] false)
                            double>boolean  (^boolean invoke [_# ^double           ~'x] false)
                            Object>boolean  (^boolean invoke [_# ^java.lang.Object ~'x] false)))
                   ;; No protocol because just one class; TODO evaluate whether this is better performance-wise? probably is
                   (defn reduced?|gen [~'x]
                     (cond* (instance? Reduced x) (.invoke reduced?|gen|__0 x)
                            (.invoke reduced?|gen|__1 x))))
        :cljs `(do (defn reduced?|gen [~'x]
                     (cond* (instance? Reduced x) true false)))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline >boolean
   ([x t/boolean?] x)
   ([x t/nil?    ] false)
   ([x t/any?    ] true))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'>boolean|gen
       (xp/casef c/count
         1 (xp/condpf-> t/<= (xp/get 0)
             t/boolean? (fn-> t/->spec) ; TODO fix this
             t/nil?     (t/value false)
             t/any?     (t/value true ))))

     ~(case-env
        :clj  `(do (def >boolean|gen|__0 ; `Reduced`
                     (reify boolean>boolean (^boolean invoke [_# ^boolean          ~'x] x)))
                   (def >boolean|gen|__1 ; `nil?`
                     (reify Object>boolean  (^boolean invoke [_# ^java.lang.Object ~'x] false)))
                   (def >boolean|gen|__2 ; `t/any?`
                     (reify boolean>boolean (^boolean invoke [_# ^boolean          ~'x] true)
                            byte>boolean    (^boolean invoke [_# ^byte             ~'x] true)
                            short>boolean   (^boolean invoke [_# ^short            ~'x] true)
                            char>boolean    (^boolean invoke [_# ^char             ~'x] true)
                            int>boolean     (^boolean invoke [_# ^int              ~'x] true)
                            long>boolean    (^boolean invoke [_# ^long             ~'x] true)
                            float>boolean   (^boolean invoke [_# ^float            ~'x] true)
                            double>boolean  (^boolean invoke [_# ^double           ~'x] true)
                            Object>boolean  (^boolean invoke [_# ^java.lang.Object ~'x] true)))

                   (defprotocol >boolean|gen__Protocol
                     (>boolean|gen [~'x]))
                   (extend-protocol >boolean|gen__Protocol
                     java.lang.Boolean (>boolean|gen [^java.lang.Boolean x] (.invoke >boolean|gen|__0 x))
                     java.lang.Object  (>boolean|gen [x]
                                         (cond* (nil? x) (.invoke >boolean|gen|__1 x)
                                                (.invoke >boolean|gen|__2 x)))))
        :cljs `(do (defn >boolean|gen [~'x]
                     (cond* (boolean? x) x
                            (nil?     x) false
                            true)))))

;; =====|=====|=====|=====|===== ;;

#?(:clj
;; auto-upcasts to long or double (because 64-bit) unless you tell it otherwise
;; will error if not all return values can be safely converted to the return spec
(defnt #_:inline >int* > int
  ([x (t/and t/primitive? (t/not t/boolean?)) #_?] (Primitive/uncheckedIntCast x))
  ([x Number] (.intValue x))))

;; ----- expanded code ----- ;;

#?(:clj
`(do (swap! fn->spec assoc #'>int*|gen
       (xp/casef c/count
         1 (xp/condpf-> t/<= (xp/get 0)
             (s/and primitive? (s/not boolean?)) t/int?
             Number                              t/int?)))

     ~(case-env
        :clj  `(do (def >int*|gen|__0 ; `(s/and primitive? (s/not boolean?))`
                     (reify byte>int   (^int invoke [_# ^byte             ~'x] (Primitive/uncheckedIntCast x))
                            short>int  (^int invoke [_# ^short            ~'x] (Primitive/uncheckedIntCast x))
                            char>int   (^int invoke [_# ^char             ~'x] (Primitive/uncheckedIntCast x))
                            int>int    (^int invoke [_# ^int              ~'x] (Primitive/uncheckedIntCast x))
                            long>int   (^int invoke [_# ^long             ~'x] (Primitive/uncheckedIntCast x))
                            float>int  (^int invoke [_# ^float            ~'x] (Primitive/uncheckedIntCast x))
                            double>int (^int invoke [_# ^double           ~'x] (Primitive/uncheckedIntCast x))))
                   (def >int*|gen|__1 ; `Number`
                     (reify Object>int (^int invoke [_# ^java.lang.Object ~'x] (.intValue ^Number x))))

                   (defprotocol >int*|gen__Protocol
                     (>int*|gen [~'x]))
                   (extend-protocol >int*|gen__Protocol
                     java.lang.Byte      (>int*|gen [^java.lang.Byte      x] (.invoke >int*|gen|__0 x))
                     java.lang.Short     (>int*|gen [^java.lang.Short     x] (.invoke >int*|gen|__0 x))
                     java.lang.Character (>int*|gen [^java.lang.Character x] (.invoke >int*|gen|__0 x))
                     java.lang.Integer   (>int*|gen [^java.lang.Integer   x] (.invoke >int*|gen|__0 x))
                     java.lang.Long      (>int*|gen [^java.lang.Long      x] (.invoke >int*|gen|__0 x))
                     java.lang.Float     (>int*|gen [^java.lang.Float     x] (.invoke >int*|gen|__0 x))
                     java.lang.Double    (>int*|gen [^java.lang.Double    x] (.invoke >int*|gen|__0 x))
                     java.lang.Number    (>int*|gen [                     x] (.invoke >int*|gen|__1 x)))))))

;; =====|=====|=====|=====|===== ;;

(defnt !str
  ([] #?(:clj (StringBuilder.) :cljs (StringBuffer.)))
  ;; `?*` means infer with opts
  ;; This means that e.g., `x` can be an `int`, and since `:any-in-numeric-range?` is `true`, `x` can be
  ;;   anything in the numeric range of an `int`.
  ;; TODO the `:any-in-numeric-range?` option could simply be reduced to a `(if numeric? in-range? identity)`
  ;;   sort of predicate — `(s/and integer-value? (s/range-of int))`
  ;; By default it enforces reasonably strict type checks (i.e. not allowing strange byte manipulation),
  ;;   so it does not allow e.g. short strings convertible to an arbitrary `int` representation.
  ([x #?(:clj (?* {:any-in-numeric-range? true}) :cljs t/any?)] ; TODO unknown if `t/any?` is really allowed here
    #?(:clj (StringBuilder. x) :cljs (StringBuffer. x))))

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'!str|gen
       (xp/casef c/count
         0 ~(case-env :clj  (t/>spec StringBuilder)
                      :cljs (t/>spec StringBuffer ))
         1 (xp/condpf-> t/<= (xp/get 0)
             (s/and primitive? (s/not boolean?)) t/int?
             Number                              t/int?)))

     ~(case-env
        :clj  `(do (def !str|gen|__0
                     (reify >Object       (^java.lang.Object invoke [_#                      ] (StringBuilder.))))
                   ;; `(?* {:any-in-numeric-range? true})`
                   (def !str|gen|__1__0 ; (StringBuilder. <CharSequence>)
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (StringBuilder. ^CharSequence x))))
                   (def !str|gen|__1__1 ; (StringBuilder. <(range-of t/int?)>)
                     (reify int>Object    (^java.lang.Object invoke [_# ^int              ~'x] (StringBuilder. x)))
                     ...)
                   (def !str|gen|__1__2 ; (StringBuilder. <String>)
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (StringBuilder. ^String x))))

                   (defprotocol !str|gen__Protocol
                     (!str|gen__protocol [~'x]))
                   (extend-protocol !str|gen__Protocol
                     ...)
                   (defn !str|gen ([  ] (.invoke !str|gen|__0))
                                  ([a0] (!str|gen__protocol a0))))
        :cljs `(do (defn !str|gen ([]   (StringBuffer.))
                                  ([a0] (let [x a0] (StringBuffer. x)))))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline >|gen
  #?(:clj  ([a ?         b ?        ] (quantum.core.Numeric/gt a b))
     :cljs ([a t/double? b t/double?] (cljs.core/> a b))))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'>|gen
       (xp/casef c/count
         2 (xp/condpf-> t/<= (xp/get 0)
             t/byte?   (xp/condpf-> t/<= (xp/get 1)
                         t/byte?   t/boolean?
                         t/char?   t/boolean?
                         t/short?  t/boolean?
                         t/int?    t/boolean?
                         t/long?   t/boolean?
                         t/float?  t/boolean?
                         t/double? t/boolean?)
             t/char?   (xp/condpf-> t/<= (xp/get 1)
                         t/byte?   t/boolean?
                         t/char?   t/boolean?
                         t/short?  t/boolean?
                         t/int?    t/boolean?
                         t/long?   t/boolean?
                         t/float?  t/boolean?
                         t/double? t/boolean?)
             t/short?  (xp/condpf-> t/<= (xp/get 1)
                         t/byte?   t/boolean?
                         t/char?   t/boolean?
                         t/short?  t/boolean?
                         t/int?    t/boolean?
                         t/long?   t/boolean?
                         t/float?  t/boolean?
                         t/double? t/boolean?)
             t/int?    (xp/condpf-> t/<= (xp/get 1)
                         t/byte?   t/boolean?
                         t/char?   t/boolean?
                         t/short?  t/boolean?
                         t/int?    t/boolean?
                         t/long?   t/boolean?
                         t/float?  t/boolean?
                         t/double? t/boolean?)
             t/long?   (xp/condpf-> t/<= (xp/get 1)
                         t/byte?   t/boolean?
                         t/char?   t/boolean?
                         t/short?  t/boolean?
                         t/int?    t/boolean?
                         t/long?   t/boolean?
                         t/float?  t/boolean?
                         t/double? t/boolean?)
             t/float?  (xp/condpf-> t/<= (xp/get 1)
                         t/byte?   t/boolean?
                         t/char?   t/boolean?
                         t/short?  t/boolean?
                         t/int?    t/boolean?
                         t/long?   t/boolean?
                         t/float?  t/boolean?
                         t/double? t/boolean?)
             t/double? (xp/condpf-> t/<= (xp/get 1)
                         t/byte?   t/boolean?
                         t/char?   t/boolean?
                         t/short?  t/boolean?
                         t/int?    t/boolean?
                         t/long?   t/boolean?
                         t/float?  t/boolean?
                         t/double? t/boolean?))))


     ~(case-env
        :clj  `(do (def >|gen|__0
                     (reify byte+byte>boolean     (^boolean invoke [_# ^byte   a ^byte   b] (Numeric/gt a b))
                            byte+char>boolean     (^boolean invoke [_# ^byte   a ^char   b] (Numeric/gt a b))
                            byte+short>boolean    (^boolean invoke [_# ^byte   a ^short  b] (Numeric/gt a b))
                            byte+int>boolean      (^boolean invoke [_# ^byte   a ^int    b] (Numeric/gt a b))
                            byte+long>boolean     (^boolean invoke [_# ^byte   a ^long   b] (Numeric/gt a b))
                            byte+float>boolean    (^boolean invoke [_# ^byte   a ^float  b] (Numeric/gt a b))
                            byte+double>boolean   (^boolean invoke [_# ^byte   a ^double b] (Numeric/gt a b))
                            char+byte>boolean     (^boolean invoke [_# ^char   a ^byte   b] (Numeric/gt a b))
                            char+char>boolean     (^boolean invoke [_# ^char   a ^char   b] (Numeric/gt a b))
                            char+short>boolean    (^boolean invoke [_# ^char   a ^short  b] (Numeric/gt a b))
                            char+int>boolean      (^boolean invoke [_# ^char   a ^int    b] (Numeric/gt a b))
                            char+long>boolean     (^boolean invoke [_# ^char   a ^long   b] (Numeric/gt a b))
                            char+float>boolean    (^boolean invoke [_# ^char   a ^float  b] (Numeric/gt a b))
                            char+double>boolean   (^boolean invoke [_# ^char   a ^double b] (Numeric/gt a b))
                            short+byte>boolean    (^boolean invoke [_# ^short  a ^byte   b] (Numeric/gt a b))
                            short+char>boolean    (^boolean invoke [_# ^short  a ^char   b] (Numeric/gt a b))
                            short+short>boolean   (^boolean invoke [_# ^short  a ^short  b] (Numeric/gt a b))
                            short+int>boolean     (^boolean invoke [_# ^short  a ^int    b] (Numeric/gt a b))
                            short+long>boolean    (^boolean invoke [_# ^short  a ^long   b] (Numeric/gt a b))
                            short+float>boolean   (^boolean invoke [_# ^short  a ^float  b] (Numeric/gt a b))
                            short+double>boolean  (^boolean invoke [_# ^short  a ^double b] (Numeric/gt a b))
                            int+byte>boolean      (^boolean invoke [_# ^int    a ^byte   b] (Numeric/gt a b))
                            int+char>boolean      (^boolean invoke [_# ^int    a ^char   b] (Numeric/gt a b))
                            int+short>boolean     (^boolean invoke [_# ^int    a ^short  b] (Numeric/gt a b))
                            int+int>boolean       (^boolean invoke [_# ^int    a ^int    b] (Numeric/gt a b))
                            int+long>boolean      (^boolean invoke [_# ^int    a ^long   b] (Numeric/gt a b))
                            int+float>boolean     (^boolean invoke [_# ^int    a ^float  b] (Numeric/gt a b))
                            int+double>boolean    (^boolean invoke [_# ^int    a ^double b] (Numeric/gt a b))
                            long+byte>boolean     (^boolean invoke [_# ^long   a ^byte   b] (Numeric/gt a b))
                            long+char>boolean     (^boolean invoke [_# ^long   a ^char   b] (Numeric/gt a b))
                            long+short>boolean    (^boolean invoke [_# ^long   a ^short  b] (Numeric/gt a b))
                            long+int>boolean      (^boolean invoke [_# ^long   a ^int    b] (Numeric/gt a b))
                            long+long>boolean     (^boolean invoke [_# ^long   a ^long   b] (Numeric/gt a b))
                            long+float>boolean    (^boolean invoke [_# ^long   a ^float  b] (Numeric/gt a b))
                            long+double>boolean   (^boolean invoke [_# ^long   a ^double b] (Numeric/gt a b))
                            float+byte>boolean    (^boolean invoke [_# ^float  a ^byte   b] (Numeric/gt a b))
                            float+char>boolean    (^boolean invoke [_# ^float  a ^char   b] (Numeric/gt a b))
                            float+short>boolean   (^boolean invoke [_# ^float  a ^short  b] (Numeric/gt a b))
                            float+int>boolean     (^boolean invoke [_# ^float  a ^int    b] (Numeric/gt a b))
                            float+long>boolean    (^boolean invoke [_# ^float  a ^long   b] (Numeric/gt a b))
                            float+float>boolean   (^boolean invoke [_# ^float  a ^float  b] (Numeric/gt a b))
                            float+double>boolean  (^boolean invoke [_# ^float  a ^double b] (Numeric/gt a b))
                            double+byte>boolean   (^boolean invoke [_# ^double a ^byte   b] (Numeric/gt a b))
                            double+char>boolean   (^boolean invoke [_# ^double a ^char   b] (Numeric/gt a b))
                            double+short>boolean  (^boolean invoke [_# ^double a ^short  b] (Numeric/gt a b))
                            double+int>boolean    (^boolean invoke [_# ^double a ^int    b] (Numeric/gt a b))
                            double+long>boolean   (^boolean invoke [_# ^double a ^long   b] (Numeric/gt a b))
                            double+float>boolean  (^boolean invoke [_# ^double a ^float  b] (Numeric/gt a b))
                            double+double>boolean (^boolean invoke [_# ^double a ^double b] (Numeric/gt a b))))

                   (defprotocol >|gen__Protocol
                     (>|gen [~'a0 ~'a1]))
                   (extend-protocol >|gen__Protocol
                     ...))
        :cljs `(do (defn >|gen
                     ([a0 a1]
                       (cond* (double? a0)
                                (cond* (double? a1)
                                         (let [a a0 b a1] (cljs.core/> a b))
                                       (unsupported! `>|gen [a0 a1]))
                              (unsupported! `>|gen [a0 a1])))))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline str
           ([] "")
           ([x t/nil?] "")
           ;; could have inferred but there may be other objects who have overridden .toString
  #?(:clj  ([x Object] (.toString x))
           ;; Can't infer that it returns a string (without a pre-constructed list of built-in functions)
           ;; As such, must explicitly mark
     :cljs ([x t/any? > (t/assume string?)] (.join #js [x] "")))
           ;; TODO only one variadic arity allowed currently; theoretically could dispatch on at least pre-variadic args, if not variadic
           ([x ? & xs (s/seq ?) #?@(:clj [> (t/assume string?)])] ; TODO should have automatic currying?
             (let [sb (!str (str x))]
               (doseq [x' xs] (.append sb (str x'))) ; TODO is `doseq` the right approach?
               (.toString sb))))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'str|gen
       (xp/casef c/count
         0 (t/value "")
         1 (xp/condpf-> t/<= (xp/get 0)
             nil?   (t/value "")
            ~@(case-env :clj  `[Object t/string?]
                        :cljs `[t/any? t/string?]))
         (xp/condpf-> t/<= ...)))

     ~(case-env
        :clj  `(do (def str|gen|__0
                     (reify >Object       (^java.lang.Object invoke [_#                      ] "")))
                   (def str|gen|__1 ; `nil?`
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] "")))
                   (def str|gen|__2 ; `Object`
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (.toString x))))

                   ;; No protocol needed because overloads of protocolizable arity (n>=1, not variadic) do not vary by class
                   (defn str|gen
                     ([  ] (.invoke !str|gen|__0))
                     ([a0] (cond* (nil? x) (.invoke !str|gen|__1)
                                  (.invoke !str|gen|__2 a0)))
                     ([x & xs]
                       (let [sb (!str (str x))]
                         (doseq [x' xs] (.append sb (str x'))) ; TODO is `doseq` the right approach?
                         (.toString sb)))))
        :cljs `(do ;; No protocol needed because overloads of protocolizable arity (n>=1, not variadic) do not vary by class
                   (defn str|gen
                     ([  ] "")
                     ([a0] (cond* (nil? x) ""
                                  (.join #js [x] "")))
                     ([x & xs]
                       (let [sb (!str (str x))]
                         (doseq [x' xs] (.append sb (str x'))) ; TODO is `doseq` the right approach?
                         (.toString sb)))))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline count
  ([xs #?(:clj ? :cljs array?) #?@(:cljs [> t/int?])] (#?(:clj Array/count :cljs .-length) xs))
  ([xs t/string?               #?@(:cljs [> t/int?])] (#?(:clj .length     :cljs .-length) xs))
  ([xs !+vector?               #?@(:cljs [> t/int?])] (#?(:clj count       :cljs (do (TODO) 0)) xs)))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'count|gen
       (xp/casef c/count
         1 (xp/condpf-> t/<= (xp/get 0)
             array?    t/int?
             string?   t/int?
             !+vector? t/int?)))

     ~(case-env
        :clj  `(do ;; `array?`
                   (def count|gen|__0__1 (reify Object>int (^int invoke [_# ^java.lang.Object ~'xs] (Array/count ^"[B" xs))))
                   ...

                   (defprotocol count|gen__Protocol ...))
        :cljs `(do ...)))

;; =====|=====|=====|=====|===== ;;

(defnt #_:inline get
  ([xs t/array? , k (t/-> integer? ?)] (#?(:clj Array/get :cljs aget) xs k))
  ([xs t/string?, k (t/-> integer? ?)] (.charAt xs k))
  ([xs !+vector?, k t/any?] #?(:clj (.valAt xs k) :cljs (TODO))))

;; ----- expanded code ----- ;;

;; =====|=====|=====|=====|===== ;;

; TODO CLJS version will come after
#?(:clj
(macroexpand '
(defnt seq|gen > (t/? ISeq)
  "Taken from `clojure.lang.RT/seq`"
  ([xs t/nil?                ] nil)
  ([xs t/array?              ] (ArraySeq/createFromObject xs))
  ([xs ASeq                  ] xs)
  ([xs (t/or LazySeq Seqable)] (.seq xs))
  ([xs Iterable              ] (clojure.lang.RT/chunkIteratorSeq (.iterator xs)))
  ([xs CharSequence          ] (StringSeq/create xs))
  ([xs Map                   ] (seq|gen (.entrySet xs)))))
)

;; ----- expanded code ----- ;;

#?(:clj
`(do (swap! fn->spec assoc #'seq|gen
       (xp/casef c/count
         1 (xp/condpf-> t/<= (xp/get 0)
             nil?                   (t/value nil)
             array?                 (t/>spec ISeq)
             (t/>spec ASeq)         t/>spec ; TODO fix
             (t/or LazySeq Seqable) (t/>spec ISeq)
             (t/>spec Iterable)     (t/>spec ISeq)
             (t/>spec CharSequence) (t/>spec ISeq)
             (t/>spec Map)          (t/>spec ISeq))))

     ~(case-env
        :clj  `(do ;; `nil?`
                   (def seq|gen|__0    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] nil)))
                   ;; `array?`
                   (def seq|gen|__1__0 (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (ArraySeq/createFromObject xs))))
                   ...
                   ;; `ASeq`
                   (def seq|gen|__2    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] xs)))
                   ;; `LazySeq`
                   (def seq|gen|__3__0 (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (.seq ^LazySeq xs))))
                   ;; `Seqable`
                   (def seq|gen|__3__1 (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (.seq ^Seqable xs))))
                   ;; `Iterable`
                   (def seq|gen|__4    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                                                              (clojure.lang.RT/chunkIteratorSeq (.iterator ^Iterator xs)))))
                   ;; `CharSequence`
                   (def seq|gen|__5    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (StringSeq/create ^CharSequence xs))))
                   ;; `Map`
                   (def seq|gen|__6    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (seq|gen (.entrySet ^Map xs)))))

                   (defprotocol seq|gen__Protocol
                     (seq|gen [a0]))
                   (extend-protocol seq|gen__Protocol
                     ;; `array?`
                     ...
                     ASeq    (seq|gen [^ASeq    a0] (.invoke seq|gen|__2 a0))
                     LazySeq (seq|gen [^LazySeq a0] (.invoke seq|gen|__3__0 a0))
                     Object  (seq|gen [a0]
                               ;; these are sequential dispatch because none of these are concrete or abstract classes
                               ;; (most are interfaces etc.)
                               (cond* (nil? a0)                   (.invoke seq|gen|__0 a0)
                                      (instance? ASeq         a0) (.invoke seq|gen|__2 a0)
                                      (instance? Seqable      a0) (.invoke seq|gen|__3__1 a0)
                                      (instance? Iterable     a0) (.invoke seq|gen|__4 a0)
                                      (instance? CharSequence a0) (.invoke seq|gen|__5 a0)
                                      (instance? Map          a0) (.invoke seq|gen|__6 a0)
                                      (unsupported! `seq|gen a0)))))
        :cljs `(do ...))))

;; =====|=====|=====|=====|===== ;;

#?(:clj
(macroexpand '
(defnt first|gen
  ([xs t/nil?                      ] nil)
  ([xs (t/and sequential? indexed?)] (get|gen xs 0))
  ([xs ISeq                        ] (.first xs))
  ([xs ?                           ] (first|gen (seq|gen xs)))))
)

#?(:clj
`(do (swap! fn->spec assoc #'seq|gen
       (xp/casef c/count
         1 (xp/condpf-> t/<= (xp/get 0)
             nil?                         (t/value nil)
             (t/and sequential? indexed?) ...
             (t/>spec ISeq)               Object
             (t/or nil?
                   array?
                   ASeq
                   (t/or LazySeq Seqable)
                   Iterable
                   CharSequence
                   Map                   ) ...)))

     ~(case-env
        :clj  `(do ...)
        :cljs `(do ...))))

;; ----- expanded code ----- ;;

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt next|gen > (? ISeq)
  "Taken from `clojure.lang.RT/next`"
  ([xs t/nil?] nil)
  ([xs ISeq  ] (.next xs))
  ([xs ?     ] (next|gen (seq|gen xs))))
)

;; ----- expanded code ----- ;;

;; =====|=====|=====|=====|===== ;;

(defnt reduce
  "Much of this content taken from clojure.core.protocols for inlining and
   type-checking purposes."
  {:attribution "alexandergunnarson"}
         ([f ?                 xs nil?] (f))
         ([f (fn-of 2), init ? xs nil?] init)
         ([f ?, init ?, z fast_zip.core.ZipperLocation]
           (loop [xs (zip/down z) v init]
             (if (some? z)
                 (let [ret (f v z)]
                   (if (reduced? ret)
                       @ret
                       (recur (zip/right xs) ret)))
                 v)))
         ; TODO look at CLJS `array-reduce`
         ([f ?, init ?, xs (t/or array? string? !+vector?)] ; because transient vectors aren't reducible
           (let [ct (count xs)]
             (loop [i 0 v init]
               (if (< i ct)
                   (let [ret (f v (get xs i))]
                     (if (reduced? ret)
                         @ret
                         (recur (inc* i) ret)))
                   v))))
#?(:clj  ([f ?, init ?, xs clojure.lang.StringSeq]
           (let [s (.s xs)]
             (loop [i (.i xs) v init]
               (if (< i (count s))
                   (let [ret (f v (get s i))]
                     (if (reduced? ret)
                         @ret
                         (recur (inc* i) ret)))
                   v)))))
#?(:clj  ([f ?
           xs (t/or clojure.lang.PersistentVector ; vector's chunked seq is faster than its iter
                    clojure.lang.LazySeq ; for range
                    clojure.lang.ASeq)] ; aseqs are iterable, masking internal-reducers
           (if-let [s (seq xs)]
             (clojure.core.protocols/internal-reduce (next s) f (first s))
             (f))))
#?(:clj  ([f ?, init ?
           xs (t/or clojure.lang.PersistentVector ; vector's chunked seq is faster than its iter
                    clojure.lang.LazySeq ; for range
                    clojure.lang.ASeq)]  ; aseqs are iterable, masking internal-reducers
           (let [s (seq xs)]
             (clojure.core.protocols/internal-reduce s f init))))
         ([x transformer?, f ?]
           (let [rf ((.-xf x) f)]
             (rf (reduce rf (rf) (.-prev x)))))
         ([x transformer?, f ?, init ?]
           (let [rf ((.-xf x) f)]
             (rf (reduce rf init (.-prev x)))))
         ([f ?, init ?, x chan?] (async/reduce f init x)) ; TODO spec `async/reduce`
#?(:cljs ([f ?, init ?, xs +map?] (#_(:clj  clojure.core.protocols/kv-reduce
                                      :cljs -kv-reduce) ; in order to use transducers...
                                -reduce-seq xs f init)))
#?(:cljs ([f ?, init ?, xs +set?] (-reduce-seq xs f init)))
         ([f ?, init ?, n numerically-int?]
           (loop [i 0 v init]
             (if (< i n)
                 (let [ret (f v i)]
                   (if (reduced? ret)
                       @ret
                       (recur (inc* i) ret))) ; TODO should only be unchecked if `n` is within unchecked range
                 v)))
         ;; `iter-reduce`
#?(:clj  ([f ?
           xs (t/or clojure.lang.APersistentMap$KeySeq
                    clojure.lang.APersistentMap$ValSeq
                    Iterable)]
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
#?(:clj  ([f ?, init ?
           xs (t/or clojure.lang.APersistentMap$KeySeq
                    clojure.lang.APersistentMap$ValSeq
                    Iterable)]
           (let [iter (.iterator xs)]
             (loop [ret init]
               (if (.hasNext iter)
                   (let [ret (f ret (.next iter))]
                     (if (reduced? ret)
                         @ret
                         (recur ret)))
                   ret)))))
#?(:clj  ([f ?,       xs clojure.lang.IReduce    ] (.reduce   xs f)))
#?(:clj  ([f ?, init, xs clojure.lang.IKVReduce  ] (.kvreduce xs f init)))
#?(:clj  ([f ?, init, xs clojure.lang.IReduceInit] (.reduce   xs f init)))
         ([f (fn-of 2), xs any?]
           (#?(:clj  clojure.core.protocols/coll-reduce
               :cljs -reduce) xs f))
         ([f (fn-of 2), init ?, xs any?]
           (#?(:clj  clojure.core.protocols/coll-reduce
               :cljs -reduce) xs f init)))

;; ----- expanded code ----- ;;

;; =====|=====|=====|=====|===== ;;

(defnt transduce
  ([     f ? xs ?] (transduce identity f     xs))
  ([xf ? f ? xs ?] (transduce xf       f (f) xs))
  ([xf ? f ? init ? xs ?]
    (let [f' (xf f)] (f' (reduce f' init xs)))))

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

(name (read ))

(defn def-interfaces
  [{:keys [::*interfaces]}]
  *interfaces)

(defn atom? [x] (instance? clojure.lang.IAtom x))

(s/def ::*interfaces (s/and atom? (fn-> deref map?)))
(s/def ::signatures (s/coll-of (s/tuple symbol? (s/+ symbol?)) :kind sequential?))

(s/fdef def-interfaces
  :args (s/cat :a0 (s/keys :req [::signatures ::*interfaces]))
  #_:ret #_int?
  #_:fn #_(s/and #(>= (:ret %) (-> % :args :start))
             #(< (:ret %) (-> % :args :end))))

(s/def ::lang #{:clj :cljs})

(s/def ::expand-signatures:opts (s/keys :opt-un [::lang]))

(s/fdef expand-signatures
  :args (s/cat :signatures ::signatures
               :opts       (s/? ::expand-signatures:opts))
  :ret  ::signatures)

(defn expand-signatures [signatures & [opts]]
  signatures)

(instrument)
(def-interfaces {::signatures  [['boolean ['nil?]]
                                ['boolean ['boolean]]
                                ['boolean ['any?]]]
                 ::*interfaces (atom {})})

(expand-signatures
  [['boolean ['nil?]]
   ['boolean ['boolean]]
   ['boolean ['any?]]]
  {:lang :clj})

(do
; (optional) function — only when the `defnt` has an arity with 0 arguments

; (optional) inline macros — invoked only if in a typed context and not used as a function
(do #?(:clj (defmacro clj:name:java:lang:String   [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro cljs:name:string            [a0] `(let [~'x ~a0] ~'x)))
    #?(:clj (defmacro clj:name:clojure:lang:Named [a0] `(let [~'x ~a0] ~'(-name x))))
    #?(:clj (defmacro cljs:name:cljs:core:INamed  [a0] `(let [~'x ~a0] ~'(.getName x)))))
)

; ================================================ ;

(defnt ^:inline custom
  [x (s/if double?
           (t/or (s/fnt [x ?] (> x 3)) ; uses the above-defined `>`
                 (s/fnt [x ?] (< x 0.1)))
           (t/or string? !string?))
   y ?] (str x (name y))) ; uses the above-defined `name`


;; ===== CLOJURESCRIPT ===== ;;

;; In order for specs to be enforceable at compile time, they must be able to be executed by the compilation
;; language. The case of one language compiled in a different one (e.g. ClojureScript in Clojure/Java) is
;; thus problematic.

;; For instance, this is only able to be checked in CLJS, because `js-object?` is not implemented in CLJ:
(defnt abcde1
  [x #?(:clj string? :cljs js-object?)] ...)

;; This could be checked in CLJ, but it would be an error to do so:
(defn my-spec [x] #?(:clj (check-this) :cljs (check-that)))

(defnt abcde2
  [x my-spec] ...)

;; So what is the solution? The solution is to forgo some functionality in ClojureScript and instead rely
;; fundamentally on the aggregative relationships among predicates created using the `defnt` spec system.

;; For instance:

(defnt abcde1 [x (t/pc :clj string? :cljs js-object?)] ...)

;; Or:

(t/def abcde1|x? :clj string? :cljs js-object?)

(defnt abcde1 [x abcde1|x?] ...)

;; Because the spec was registered using the `defnt` spec system, the quoted forms can be analyzed and
;; at least some things can be deduced.

;; In this case, the spec of `x` is deducible: `abcde1|x?` (`js-object?` deeper down). The return spec
;; is also deducible as being the return spec of `abcde1`:

(defnt abcde2 [x ?] (abcde1 x))
