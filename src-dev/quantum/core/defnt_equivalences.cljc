;; See https://jsperf.com/js-property-access-comparison — all property accesses (at least of length 1) seem to be equal

(ns quantum.core.test.defnt-equivalences
  (:refer-clojure :exclude [name identity *])
  (:require
    [clojure.core              :as c]
    [quantum.core.defnt
      :refer [analyze defnt fnt|code *fn->spec]]
    [quantum.untyped.core.analyze.expr :as xp]
    [quantum.untyped.core.collections.diff :as diff
      :refer [diff]]
    [quantum.untyped.core.core :as ucore
      :refer [code=]]
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
      :refer [deftest testing is is= throws]]
    [quantum.untyped.core.type :as t
      :refer [? *]])
  (:import
    clojure.lang.Named
    clojure.lang.Reduced
    clojure.lang.ISeq
    clojure.lang.ASeq
    clojure.lang.LazySeq
    clojure.lang.Seqable
    quantum.core.data.Array
    quantum.core.Primitive))

;; =====|=====|=====|=====|===== ;;

(is (code=

;; ----- implementation ----- ;;

(macroexpand '
  (defnt pid [> (? t/string?)]
    (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
         (.getName))))

;; ----- expanded code ----- ;;

($ (do (swap! *fn->spec assoc #'pid
         (t/fn [:> (? t/string?)]))

       (def ~'pid|__0
         (reify >Object
           (~(tag "java.lang.Object" 'invoke) [~'_]
             (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
                  (.getName)))))))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

(macroexpand '
(defnt identity|uninlined ([x _] x))
)

;; ----- implementation ----- ;;

(macroexpand '
(defnt identity|uninlined ([x t/any?] x))
)

;; ----- expanded code ----- ;;

($ (do (swap! *fn->spec assoc #'identity|uninlined
         (t/fn [t/any?]))

       ~@(case (env-lang)
                      ;; Because for `any?` it includes primitives as well
           :clj  ($ [;; Direct dispatch
                     ;; One reify per overload
                     (def ~'identity|uninlined|__0 ; `[x t/any?]`
                       (reify
                         Object>Object   (~(tag "java.lang.Object" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)] ~'x)
                         boolean>boolean (~(tag "boolean"          'invoke) [~'_ ~(tag "boolean"          'x)] ~'x)
                         byte>byte       (~(tag "byte"             'invoke) [~'_ ~(tag "byte"             'x)] ~'x)
                         short>short     (~(tag "short"            'invoke) [~'_ ~(tag "short"            'x)] ~'x)
                         char>char       (~(tag "char"             'invoke) [~'_ ~(tag "char"             'x)] ~'x)
                         int>int         (~(tag "int"              'invoke) [~'_ ~(tag "int"              'x)] ~'x)
                         long>long       (~(tag "long"             'invoke) [~'_ ~(tag "long"             'x)] ~'x)
                         float>float     (~(tag "float"            'invoke) [~'_ ~(tag "float"            'x)] ~'x)
                         double>double   (~(tag "double"           'invoke) [~'_ ~(tag "double"           'x)] ~'x)))
                     ;; TODO implement this
                     ;; Dynamic dispatch (invoked only if incomplete type information (incl. in untyped context))
                     ;; in this case no protocol is necessary because it boxes arguments anyway
                     ;; Var indirection may be avoided by making and using static fields via the Clojure 1.8 flag
                     #_(defn ~'identity|uninlined [~'x] (.invoke identity|uninlined|__0 ~'x))])
           :cljs ;; Direct dispatch will be simple functions, not `reify`s; not necessary here
                 ;; Dynamic dispatch will be approached later; not clear yet whether there is a huge savings
                 ($ [(defn ~'identity|uninlined [~'x] ~'x)])))))

)

;; =====|=====|=====|=====|===== ;;

;; TODO will deal with `inline` later
(defnt ^:inline identity ([x t/any?] x))

;; ----- test ----- ;;

(deftest test|identity
  (is= (identity 1 ) 1 )
  (is= (identity "") ""))

;; =====|=====|=====|=====|===== ;;

(is (code=

;; TODO don't ignore `:inline`
(macroexpand '
(defnt #_:inline name
           ([x t/string?       > t/string?]     x)
  #?(:clj  ([x (t/isa? Named)  > (* t/string?)] (.getName x))
     :cljs ([x (t/isa? INamed) > (* t/string?)] (-name x))))
)

;; ----- expanded code ----- ;;

($ (do (swap! *fn->spec assoc #'name
         (t/fn [t/string?       :> t/string?]
               [(t/isa? Named)  :> (* t/string?)]
               [(t/isa? INamed) :> (* t/string?)]))

       ~@(case (env-lang)
           :clj  ($ [;; Only direct dispatch for primitives or for Object, not for subclasses of Object
                     ;; Return value can be primitive; in this case it's not
                     ;; The macro in a typed context will find the appropriate dispatch at compile time
                     (def ~'name|__0
                       (reify Object>Object
                         (~(tag "java.lang.Object" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)]
                           (let* [~(tag "java.lang.String" 'x) ~'x]
                             ~'x))))
                     (def ~'name|__1
                       (reify Object>Object
                         (~(tag "java.lang.Object" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)]
                           (let* [~(tag "clojure.lang.Named" 'x) ~'x]
                             (let [~'out (.getName ~'x)]
                               (s/validate ~'out t/string?))))))
                     ;; TODO implement this
                     ;; This protocol is so suffixed because of the position of the argument on which
                     ;; it dispatches
                     #_(defprotocol name__Protocol__0
                       (name [~'x]))
                     #_(extend-protocol name__Protocol__0
                       java.lang.String   (name [x] (.invoke name|__0 x))
                       ;; this is part of the protocol because even though `Named` is an interface,
                       ;; `String` is final, so they're mutually exclusive
                       clojure.lang.Named (name [x] (.invoke name|__1 x)))])
           :cljs ($ [;; No protocol in ClojureScript; consider adding this if a performance increase is
                     ;; demonstrated when using a protocol
                     (defn ~'name [~'x]
                       (ifs (string? x)           x
                            (satisfies? INamed x) (-name x)
                            (err! "Not supported for type" {:fn `name :type (type x)})))]))))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
(macroexpand '
(defnt #_:inline some?
  ([x t/nil?] false)
  ;; Implicitly, `(- t/any? t/nil?)`, so `t/val?`
  ([x t/any?] true))
)

;; ----- expanded code ----- ;;

($ (do (swap! fn->spec assoc #'some?
         (t/fn [t/nil?]
               [t/any?]))

       ~@(case (env-lang)
           :clj  ($ [(def ~'some?|__0 ; `[x t/nil?]`
                       (reify
                         Object>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)] false)))
                     (def ~'some?|__1 ; `[x t/any?]`
                       (reify
                         Object>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)] true)
                         boolean>boolean (~(tag "boolean" 'invoke) [~'_ ~(tag "boolean"          'x)] true)
                         byte>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "byte"             'x)] true)
                         short>boolean   (~(tag "boolean" 'invoke) [~'_ ~(tag "short"            'x)] true)
                         char>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "char"             'x)] true)
                         int>boolean     (~(tag "boolean" 'invoke) [~'_ ~(tag "int"              'x)] true)
                         long>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "long"             'x)] true)
                         float>boolean   (~(tag "boolean" 'invoke) [~'_ ~(tag "float"            'x)] true)
                         double>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "double"           'x)] true)))
                     ;; TODO implement this
                     ;; Dynamic dispatch
                     #_(defn ~'some? [~'x]
                       (ifs (nil? x) (.invoke some?|__0 x)
                            (.invoke some?|__1 x)))])
           :cljs ($ [(defn ~'some? [~'x]
                       (ifs (nil? x) false
                            true))]))))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

;; Perhaps silly in ClojureScript, but avoids boxing in Clojure
(macroexpand '
(defnt #_:inline reduced?
  ([x (t/isa? Reduced)] true)
  ;; Implicitly, `(- t/any? (t/isa? Reduced))`
  ([x t/any?          ] false))
)

;; ----- expanded code ----- ;;

($ (do (swap! fn->spec assoc #'reduced?
         (t/fn [(t/isa? Reduced)]
               [t/any?]))

       ~@(case (env-lang)
           :clj  ($ [(def ~'reduced?|__0 ; `[x Reduced]`
                       (reify
                         Object>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)]
                                           (let* [~(tag "clojure.lang.Reduced" 'x) ~'x]
                                             true))))
                     (def ~'reduced?|__1 ; `[x t/any?]`
                       (reify
                         Object>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)] false)
                         boolean>boolean (~(tag "boolean" 'invoke) [~'_ ~(tag "boolean"          'x)] false)
                         byte>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "byte"             'x)] false)
                         short>boolean   (~(tag "boolean" 'invoke) [~'_ ~(tag "short"            'x)] false)
                         char>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "char"             'x)] false)
                         int>boolean     (~(tag "boolean" 'invoke) [~'_ ~(tag "int"              'x)] false)
                         long>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "long"             'x)] false)
                         float>boolean   (~(tag "boolean" 'invoke) [~'_ ~(tag "float"            'x)] false)
                         double>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "double"           'x)] false)))
                     ;; TODO implement
                     ;; No protocol because just one class; TODO evaluate whether this is better performance-wise? probably is
                     #_(defn ~'reduced? [~'x]
                       (ifs (instance? Reduced x) (.invoke reduced?|__0 ~'x)
                            (.invoke reduced?|__1 ~'x)))])
           :cljs ($ [(defn ~'reduced? [~'x]
                       (ifs (instance? Reduced x) true false))]))))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

(macroexpand '
(defnt #_:inline >boolean
   ([x t/boolean?] x)
   ([x t/nil?]     false)
   ;; Implicitly, `(- t/any? t/nil? t/boolean?)`
   ([x t/any?]     true))
)

;; ----- expanded code ----- ;;

($ (do (swap! fn->spec assoc #'>boolean
         (t/fn [t/boolean?]
               [t/nil?]
               [t/any?]))

       ~@(case (env-lang)
           :clj  ($ [(def ~'>boolean|__0 ; `[x t/boolean?]`
                       (reify
                         boolean>boolean (~(tag "boolean" 'invoke) [~'_ ~(tag "boolean"          'x)] ~'x)))
                     (def ~'>boolean|__1 ; `[x t/nil?]`
                       (reify
                         Object>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)] false)))
                     (def ~'>boolean|__2 ; `[x t/any?]`
                       (reify
                         Object>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)] true)
                         boolean>boolean (~(tag "boolean" 'invoke) [~'_ ~(tag "boolean"          'x)] true)
                         byte>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "byte"             'x)] true)
                         short>boolean   (~(tag "boolean" 'invoke) [~'_ ~(tag "short"            'x)] true)
                         char>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "char"             'x)] true)
                         int>boolean     (~(tag "boolean" 'invoke) [~'_ ~(tag "int"              'x)] true)
                         long>boolean    (~(tag "boolean" 'invoke) [~'_ ~(tag "long"             'x)] true)
                         float>boolean   (~(tag "boolean" 'invoke) [~'_ ~(tag "float"            'x)] true)
                         double>boolean  (~(tag "boolean" 'invoke) [~'_ ~(tag "double"           'x)] true)))
                     ;; TODO implement this
                     #_(defprotocol >boolean__Protocol
                       (>boolean [~'x]))
                     #_(extend-protocol >boolean__Protocol
                       java.lang.Boolean (>boolean [^java.lang.Boolean x] (.invoke >boolean|__0 x))
                       java.lang.Object  (>boolean [x]
                                           (ifs (nil? x) (.invoke >boolean|__1 x)
                                                (.invoke >boolean|__2 x))))])
           :cljs ($ [(defn ~'>boolean [~'x]
                       (ifs (boolean? x) x
                            (nil?     x) false
                            true))]))))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

;; auto-upcasts to long or double (because 64-bit) unless you tell it otherwise
;; will error if not all return values can be safely converted to the return spec
(macroexpand '
(defnt #_:inline >int* > int?
  ([x (t/- t/primitive? t/boolean?)] (Primitive/uncheckedIntCast x))
  ([x (t/ref (t/isa? Number))] (.intValue x)))
)

;; ----- expanded code ----- ;;

#?(:clj
`(do (swap! fn->spec assoc #'>int*
       (t/fn [(t/- t/primitive? t/boolean?)]
             [(t/ref (t/isa? Number))]))

     ~@(case (env-lang)
         :clj ($ [(def ~'>int*|__0 ; `(t/- t/primitive? t/boolean?)`
                    (reify byte>int   (~(tag "int" 'invoke) [~'_ ~(tag "byte"             'x)] (Primitive/uncheckedIntCast x))
                           short>int  (~(tag "int" 'invoke) [~'_ ~(tag "short"            'x)] (Primitive/uncheckedIntCast x))
                           char>int   (~(tag "int" 'invoke) [~'_ ~(tag "char"             'x)] (Primitive/uncheckedIntCast x))
                           int>int    (~(tag "int" 'invoke) [~'_ ~(tag "int"              'x)] (Primitive/uncheckedIntCast x))
                           long>int   (~(tag "int" 'invoke) [~'_ ~(tag "long"             'x)] (Primitive/uncheckedIntCast x))
                           float>int  (~(tag "int" 'invoke) [~'_ ~(tag "float"            'x)] (Primitive/uncheckedIntCast x))
                           double>int (~(tag "int" 'invoke) [~'_ ~(tag "double"           'x)] (Primitive/uncheckedIntCast x))))
                  (def ~'>int*|__1 ; `Number`
                    (reify Object>int (~(tag "int" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)]
                      (let* [~(tag "java.lang.Number" 'x) ~'x] (.intValue x)))))
                  ;; TODO implement this
                  #_(defprotocol >int*_Protocol
                    (>int* [~'x]))
                  #_(extend-protocol >int*__Protocol
                    java.lang.Byte      (>int* [~(tag "java.lang.Byte"      x)] (.invoke >int*|__0 x))
                    java.lang.Short     (>int* [~(tag "java.lang.Short"     x)] (.invoke >int*|__0 x))
                    java.lang.Character (>int* [~(tag "java.lang.Character" x)] (.invoke >int*|__0 x))
                    java.lang.Integer   (>int* [~(tag "java.lang.Integer"   x)] (.invoke >int*|__0 x))
                    java.lang.Long      (>int* [~(tag "java.lang.Long"      x)] (.invoke >int*|__0 x))
                    java.lang.Float     (>int* [~(tag "java.lang.Float"     x)] (.invoke >int*|__0 x))
                    java.lang.Double    (>int* [~(tag "java.lang.Double"    x)] (.invoke >int*|__0 x))
                    java.lang.Number    (>int* [~(tag "java.lang.Object"    x)] (.invoke >int*|__1 x)))]))))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

(macroexpand '
(defnt >long*
  {:source "clojure.lang.RT.uncheckedLongCast"}
  > t/long?
  ([x (t/- t/primitive? t/boolean?)] (Primitive/uncheckedLongCast x))
  ([x (t/ref (t/isa? Number))] (.longValue x))))

;; ----- expanded code ----- ;;

`(do ~@(case (env-lang)
         :clj ($ [(def ~'>long*|__0 ; `(t/- t/primitive? t/boolean?)`
                    (reify byte>long   (~(tag "long" 'invoke) [~'_ ~(tag "byte"             'x)] (~'Primitive/uncheckedLongCast ~'x))
                           short>long  (~(tag "long" 'invoke) [~'_ ~(tag "short"            'x)] (~'Primitive/uncheckedLongCast ~'x))
                           char>long   (~(tag "long" 'invoke) [~'_ ~(tag "char"             'x)] (~'Primitive/uncheckedLongCast ~'x))
                           int>long    (~(tag "long" 'invoke) [~'_ ~(tag "int"              'x)] (~'Primitive/uncheckedLongCast ~'x))
                           long>long   (~(tag "long" 'invoke) [~'_ ~(tag "long"             'x)] (~'Primitive/uncheckedLongCast ~'x))
                           float>long  (~(tag "long" 'invoke) [~'_ ~(tag "float"            'x)] (~'Primitive/uncheckedLongCast ~'x))
                           double>long (~(tag "long" 'invoke) [~'_ ~(tag "double"           'x)] (~'Primitive/uncheckedLongCast ~'x))))
                  (def ~'>long*|__1 ; `Number`
                    (reify Object>long (~(tag "long" 'invoke) [~'_ ~(tag "java.lang.Object" 'x)]
                      (let* [~(tag "java.lang.Number" 'x) ~'x] (.longValue ~'x)))))
                  ;; TODO implement this
                #_(defprotocol >long*__Protocol
                    (>long* [~'x]))
                #_(extend-protocol >int*|__Protocol
                    java.lang.Byte      (>long* [~(tag "java.lang.Byte"      x)] (.invoke >long*__0 x))
                    java.lang.Short     (>long* [~(tag "java.lang.Short"     x)] (.invoke >long*__0 x))
                    java.lang.Character (>long* [~(tag "java.lang.Character" x)] (.invoke >long*__0 x))
                    java.lang.Integer   (>long* [~(tag "java.lang.Integer"   x)] (.invoke >long*__0 x))
                    java.lang.Long      (>long* [~(tag "java.lang.Long"      x)] (.invoke >long*__0 x))
                    java.lang.Float     (>long* [~(tag "java.lang.Float"     x)] (.invoke >long*__0 x))
                    java.lang.Double    (>long* [~(tag "java.lang.Double"    x)] (.invoke >long*__0 x))
                    java.lang.Number    (>long* [~(tag "java.lang.Object"    x)] (.invoke >long*__1 x)))])))

))

;; =====|=====|=====|=====|===== ;;

(is (code=

(macroexpand '
(defnt >long
  {:source "clojure.lang.RT.longCast"}
  > t/long?
  ([x (t/isa? clojure.lang.BigInt)]
    (if (nil? (.bipart x))
        (.lpart x)
        (throw (Exception. "Long out of range"))))
  ([x (t/isa? java.math.BigInteger)]
    (if (< (.bitLength x) 64)
        (.longValue x)
        (throw (Exception. "Long out of range"))))
  ;; TODO handle recursion
  #_([x t/ratio?] (>long (.bigIntegerValue x)))
  ;; TODO handle calling of other `defnt`s
  #_([x (t/- t/primitive? t/boolean?)] (>long* x))
  ([x t/float?] (clojure.lang.RT/longCast x)) ; Because primitive casting in Clojure is not supported ; TODO fix
  ([x t/double?] (clojure.lang.RT/longCast x)) ; TODO fix
  ([x t/boolean?] (if x 1 0))
  ;; TODO handle recursion
  #_([x t/string?] (-> x Long/parseLong >long))
  ([x t/string?, radix t/int?] (Long/parseLong x radix))))

;; ----- expanded code ----- ;;

`(do ~@(case (env-lang)
         :clj ($ [
                  ])))

))

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
        :clj  `(do (def !str|__0
                     (reify >Object       (^java.lang.Object invoke [_#                      ] (StringBuilder.))))
                   ;; `(?* {:any-in-numeric-range? true})`
                   (def !str|__1__0 ; (StringBuilder. <CharSequence>)
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (StringBuilder. ^CharSequence x))))
                   (def !str|__1__1 ; (StringBuilder. <(range-of t/int?)>)
                     (reify int>Object    (^java.lang.Object invoke [_# ^int              ~'x] (StringBuilder. x)))
                     ...)
                   (def !str|__1__2 ; (StringBuilder. <String>)
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (StringBuilder. ^String x))))

                   (defprotocol !str__Protocol
                     (!str__protocol [~'x]))
                   (extend-protocol !str__Protocol
                     ...)
                   (defn !str ([  ] (.invoke !str|__0))
                              ([a0] (!str__protocol a0))))
        :cljs `(do (defn !str ([]   (StringBuffer.))
                              ([a0] (let [x a0] (StringBuffer. x)))))))

;; =====|=====|=====|=====|===== ;;

(macroexpand '
(defnt #_:inline >
           ;; This is admittedly a place where inference might be nice, but luckily there are no
           ;; "sparse" combinations
  #?(:clj  ([a t/comparable-primitive? b t/comparable-primitive? > t/boolean?]
             (quantum.core.Numeric/gt a b))
     :cljs ([a t/double?               b t/double?               > (t/assume t/boolean?)]
             (cljs.core/>             a b))))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'>
       (t/fn #?(:clj  [t/comparable-primitive? t/comparable-primitive? :> t/boolean?]
                :cljs [t/double?               t/double?               :> (t/assume t/boolean?)])))

     ~(case-env
        :clj  `(do (def >|__0
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

                   (defprotocol >__Protocol
                     (> [~'a0 ~'a1]))
                   (extend-protocol >__Protocol
                     ...))
        :cljs `(do (defn >
                     ([a0 a1]
                       (ifs (double? a0)
                              (ifs (double? a1)
                                     (let [a a0 b a1] (cljs.core/> a b))
                                   (unsupported! `> [a0 a1]))
                            (unsupported! `> [a0 a1])))))))

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
             (let [sb (-> x str !str)] ; determined to be StringBuilder
               ;; TODO is `doseq` the right approach, or using reduction?
               (doseq [x' xs] (.append sb (str x')))
               (.toString sb))))
)

;; ----- expanded code ----- ;;

`(do (swap! fn->spec assoc #'str
       (t/fn > t/string?
         []
         [t/nil?]
#?(:clj  [(t/isa? Object)])
#?(:cljs [t/any? :> (t/assume t/string?)])
         [(t/fn> str t/any?) :& (? (t/seq-of t/any?)) #?@(:cljs [:> (t/assume t/string?)])]))

     ~(case-env
        :clj  `(do (def str|__0
                     (reify >Object       (^java.lang.Object invoke [_#                      ] "")))
                   (def str|__1 ; `nil?`
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] "")))
                   (def str|__2 ; `Object`
                     (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'x] (.toString x))))

                   ;; No protocol needed because overloads of protocolizable arity (n>=1, not variadic) do not vary by class
                   (defn str
                     ([  ] (.invoke !str|__0))
                     ([a0] (ifs (nil? x) (.invoke !str|__1)
                                (.invoke !str|__2 a0)))
                     ([x & xs]
                       (let [sb (!str (str x))]
                         (doseq [x' xs] (.append sb (str x'))) ; TODO is `doseq` the right approach?
                         (.toString sb)))))
        :cljs `(do ;; No protocol needed because overloads of protocolizable arity (n>=1, not variadic) do not vary by class
                   (defn str
                     ([  ] "")
                     ([a0] (ifs (nil? x) ""
                                (.join #js [x] "")))
                     ([x & xs]
                       (let [sb (!str (str x))]
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
                   ...

                   (defprotocol count__Protocol ...))
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
  ([xs t/nil?                ] nil)
  ([xs t/array?              ] (ArraySeq/createFromObject xs))
  ([xs (t/isa? ASeq)         ] xs)
  ([xs (t/or (t/isa? LazySeq) (t/isa? Seqable))] (.seq xs))
  ([xs t/iterable?           ] (clojure.lang.RT/chunkIteratorSeq (.iterator xs)))
  ([xs t/char-seq?           ] (StringSeq/create xs))
  ([xs t/java-map?           ] (seq (.entrySet xs)))))
)

;; ----- expanded code ----- ;;

#?(:clj
`(do (swap! fn->spec assoc #'seq
       (t/fn > (t/? (t/isa? ISeq))
         [t/nil?]
         [t/array?]
         [(t/isa? ASeq)]
         [(t/or (t/isa? LazySeq) (t/isa? Seqable))]
         [t/iterable?]
         [t/char-seq?]
         [t/java-map?]))

     ~(case-env
        :clj  `(do ;; `nil?`
                   (def seq|__0    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] nil)))
                   ;; `array?`
                   (def seq|__1__0 (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (ArraySeq/createFromObject xs))))
                   ...
                   ;; `ASeq`
                   (def seq|__2    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] xs)))
                   ;; `LazySeq`
                   (def seq|__3__0 (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (.seq ^LazySeq xs))))
                   ;; `Seqable`
                   (def seq|__3__1 (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (.seq ^Seqable xs))))
                   ;; `Iterable`
                   (def seq|__4    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs]
                                                              (clojure.lang.RT/chunkIteratorSeq (.iterator ^Iterator xs)))))
                   ;; `CharSequence`
                   (def seq|__5    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (StringSeq/create ^CharSequence xs))))
                   ;; `Map`
                   (def seq|__6    (reify Object>Object (^java.lang.Object invoke [_# ^java.lang.Object ~'xs] (seq (.entrySet ^Map xs)))))

                   (defprotocol seq__Protocol
                     (seq [a0]))
                   (extend-protocol seq__Protocol
                     ;; `array?`
                     ...
                     ASeq    (seq [^ASeq    a0] (.invoke seq|__2 a0))
                     LazySeq (seq [^LazySeq a0] (.invoke seq|__3__0 a0))
                     Object  (seq [a0]
                               ;; these are sequential dispatch because none of these are concrete or abstract classes
                               ;; (most are interfaces etc.)
                               (ifs (nil? a0)                   (.invoke seq|__0 a0)
                                    (instance? ASeq         a0) (.invoke seq|__2 a0)
                                    (instance? Seqable      a0) (.invoke seq|__3__1 a0)
                                    (instance? Iterable     a0) (.invoke seq|__4 a0)
                                    (instance? CharSequence a0) (.invoke seq|__5 a0)
                                    (instance? Map          a0) (.invoke seq|__6 a0)
                                    (unsupported! `seq a0)))))
        :cljs `(do ...))))

;; =====|=====|=====|=====|===== ;;

#?(:clj
(macroexpand '
(defnt first
  ([xs t/nil?                          ] nil)
  ([xs (t/and t/sequential? t/indexed?)] (get xs 0))
  ([xs (t/isa? ISeq)                   ] (.first xs))
  ([xs ...                             ] (-> xs seq first))))
)

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
      ([        f rf?,        xs t/reducible?] (transduce identity f     xs))
      ([xf xf?, f rf?,        xs t/reducible?] (transduce xf       f (f) xs))
      ([xf xf?, f rf?, init _ xs t/reducible?]
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
