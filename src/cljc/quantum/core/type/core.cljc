(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.type.core
  (:refer-clojure :exclude [class])
  (:require
          [clojure.set    :as set]
          [clojure.string :as str]
 #?(:clj  [clojure.tools.analyzer.jvm.utils :as ana])
 #?(:clj  [clojure.core.async.impl.protocols]
    :cljs [cljs.core.async.impl.channels])
          [quantum.core.type.defs           :as defs]
          [quantum.core.fn
            :refer [<- fn->>]]
          [quantum.core.error               :as err
            :refer [->ex]]
          [quantum.core.vars                :as var
            :refer [defalias]]))

(def compiler-lang #?(:clj :clj :cljs :cljs))

(def class #?(:clj clojure.core/class :cljs type))

(defs/def-types #?(:clj :clj :cljs :cljs))

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

(def prim-types      (-> types-unevaled :clj (get 'prim?)))
(def primitive-types (-> types-unevaled :clj (get 'primitive?)))
(def primitive-boxed-types (set/difference primitive-types prim-types))

(def prim?      #(contains? prim-types %))
(def primitive? #(contains? primitive-types %))
#?(:clj  (def auto-unboxable? #(contains? primitive-boxed-types %))
   :cljs (defn auto-unboxable? [x] (throw (->ex :unsupported "|auto-unboxable?| not supported by CLJS"))))

; ===== ARRAYS ===== ;

(def primitive-array-types ; TODO get from type/defs
  '{:clj  #{                                    bytes shorts chars ints longs floats doubles}
    :cljs #{ubytes ubytes-clamped ushorts uints bytes shorts       ints       floats doubles}})

(def cljs-typed-array-convertible-classes
  (let [cljs-typed-array-types (-> defs/array-1d-types :cljs (dissoc :object))
        generalize-type (fn->> name str/lower-case (remove #{\u}))]
    (->> cljs-typed-array-types
         (reduce
           (fn [ret [type-sym k]]
             (let [type-sym' (generalize-type type-sym)]
               (assoc ret k (->> cljs-typed-array-types
                                 (filter (fn [[type-sym1 _]] (-> type-sym1 generalize-type (= type-sym'))))
                                 (map    val)
                                 set
                                 (<- disj k)))))
           {}))))

#?(:clj
(defn ->elem-type [x]
  (when (and (or (string? x) (symbol? x))
             (-> x name first (= \[)))
    (or (defs/elem-types x)
        (if (-> x name second (= \L))
            (->> x name rest rest drop-last (replace {\/ \.}) (apply str) symbol) ; Object array
            (->> x name rest                                  (apply str) symbol)))))) ; Primitive array

(def default-types (-> types-unevaled (get compiler-lang) :any))

(defn ->boxed   [t]
  #?(:clj  (if-let [boxed   (get boxed-type-map   t)] boxed   t)
     :cljs (throw (->ex :unsupported "|->boxed| not supported by CLJS"))))

(defn ->unboxed [t]
  #?(:clj  (if-let [unboxed (get unboxed-type-map t)] unboxed t)
     :cljs (throw (->ex :unsupported "|->boxed| not supported by CLJS"))))

(defn boxed?    [t]
  #?(:clj  (contains? unboxed-type-map t)
     :cljs (throw (->ex :unsupported "|boxed?| not supported by CLJS"))))

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
