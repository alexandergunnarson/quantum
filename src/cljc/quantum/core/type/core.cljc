(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.type.core
           (:require
             #?(:clj [clojure.tools.analyzer.jvm.utils :as ana])
                     [quantum.core.type.bootstrap      :as boot]
                     [quantum.core.vars                :as var
                       :refer [#?@(:clj [defalias])]           ])
  #?(:cljs (:require-macros
                     [quantum.core.type.bootstrap      :as boot]
                     [quantum.core.vars                :as var
                       :refer [defalias]                       ])))

#?(:cljs (def class type))

(boot/def-types #?(:clj :clj :cljs :cljs))

(defn name-from-class
  [class-0]
#?(:clj
     (let [^String class-str (str class-0)]
       (-> class-str
           (subs (-> class-str (.indexOf " ") inc))
           symbol))
   :cljs
     (if (-> types (get 'primitive?) (contains? class-0))
         (or (get primitive-types class-0)
             (throw+ {:msg (str "Class " (type->str class-0) " not found in primitive types.")}))
         (-> class-0 type->str symbol))))

#?(:clj
(def boxed-type-map
 '{boolean java.lang.Boolean
   byte    java.lang.Byte
   char    java.lang.Character
   long    java.lang.Long
   double  java.lang.Double
   short   java.lang.Short
   int     java.lang.Integer
   float   java.lang.Float}))



; (def primitive?
;   "Returns non-nil if the argument represents a primitive Class other than Void"
;   #{Double/TYPE Character/TYPE Byte/TYPE Boolean/TYPE
;     Short/TYPE Float/TYPE Long/TYPE Integer/TYPE})

; (def ^:private convertible-primitives
;   "If the argument is a primitive Class, returns a set of Classes
;    to which the primitive Class can be casted"
;   {Integer/TYPE   #{Integer Long/TYPE Long Short/TYPE Byte/TYPE Object Number}
;    Float/TYPE     #{Float Double/TYPE Object Number}
;    Double/TYPE    #{Double Float/TYPE Object Number}
;    Long/TYPE      #{Long Integer/TYPE Short/TYPE Byte/TYPE Object Number}
;    Character/TYPE #{Character Object}
;    Short/TYPE     #{Short Object Number}
;    Byte/TYPE      #{Byte Object Number}
;    Boolean/TYPE   #{Boolean Object}
;    Void/TYPE      #{Void}})

; (defn ^Class box
;   "If the argument is a primitive Class, returns its boxed equivalent,
;    otherwise returns the argument"
;   [c]
;   ({Integer/TYPE   Integer
;     Float/TYPE     Float
;     Double/TYPE    Double
;     Long/TYPE      Long
;     Character/TYPE Character
;     Short/TYPE     Short
;     Byte/TYPE      Byte
;     Boolean/TYPE   Boolean
;     Void/TYPE      Void}
;    c c))

; (defn ^Class unbox
;   "If the argument is a Class with a primitive equivalent, returns that,
;    otherwise returns the argument"
;   [c]
;   ({Integer   Integer/TYPE,
;     Long      Long/TYPE,
;     Float     Float/TYPE,
;     Short     Short/TYPE,
;     Boolean   Boolean/TYPE,
;     Byte      Byte/TYPE,
;     Character Character/TYPE,
;     Double    Double/TYPE,
;     Void      Void/TYPE}
;    c c))

; (defn numeric?
;   "Returns true if the given class is numeric"
;   [c]
;   (when c
;     (.isAssignableFrom Number (box c))))

; (def wider-than
;   "If the argument is a numeric primitive Class, returns a set of primitive Classes
;    that are narrower than the given one"
;   {Long/TYPE    #{Integer/TYPE Short/TYPE Byte/TYPE}
;    Integer/TYPE #{Short/TYPE Byte/TYPE}
;    Float/TYPE   #{Integer/TYPE Short/TYPE Byte/TYPE Long/TYPE}
;    Double/TYPE  #{Integer/TYPE Short/TYPE Byte/TYPE Long/TYPE Float/TYPE}
;    Short/TYPE   #{Byte/TYPE}
;    Byte/TYPE    #{}})

#?(:clj
; Returns true if it's possible to convert from c1 to c2
(defalias convertible? ana/convertible?))

#?(:clj
(def unboxed-type-map
  (zipmap (vals boxed-type-map) (keys boxed-type-map))))

#?(:clj (def primitive-types (-> types-unevaled :clj (get 'primitive?))))
(def primitive? (partial contains? primitive-types))
#?(:clj (def auto-unboxable? primitive?))

(def compiler-lang #?(:clj :clj :cljs :cljs))
(def default-types (-> types-unevaled (get compiler-lang) :any))

#?(:clj (defn ->boxed   [t] (if-let [boxed   (get boxed-type-map   t)] boxed   t)))
#?(:clj (defn ->unboxed [t] (if-let [unboxed (get unboxed-type-map t)] unboxed t)))
#?(:clj (defn boxed?    [t] (contains? unboxed-type-map t)))

(def type-casts-map
  {:clj
    {; Primitive casts are not hint-supported
     'bigdec        'java.math.BigDecimal
     'bigint        'clojure.lang.BigInt
     'boolean-array (symbol "[Z")
     'byte-array    (symbol "[B")
     'char-array    (symbol "[C")
     'short-array   (symbol "[S")
     'long-array    (symbol "[J")
     'float-array   (symbol "[F")
     'int-array     (symbol "[I")
     'double-array  (symbol "[D")
     'object-array  (symbol "[Ljava.lang.Object;")}})