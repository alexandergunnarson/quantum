(ns quantum.untyped.core.type.core
  (:refer-clojure :exclude
    [class])
  (:require
    [clojure.core.reducers            :as r]
    [clojure.set                      :as set]
    [clojure.string                   :as str]
#?@(:clj
   [[clojure.core.async.impl.protocols]
    [clojure.tools.analyzer.jvm.utils :as ana]]
    :cljs
   [[cljs.core.async.impl.channels]])
    [quantum.untyped.core.convert     :as uconv
      :refer [>name]]
    [quantum.untyped.core.core        :as ucore]
    [quantum.untyped.core.error
      :refer [>ex-info]]
    [quantum.untyped.core.fn
      :refer [<- fn->>]]
    [quantum.untyped.core.vars
      :refer [defalias]]
    [quantum.untyped.core.type.defs   :as utdef]))

(def class #?(:clj clojure.core/class :cljs type))

(def boxed-type-map
 '{boolean java.lang.Boolean
   byte    java.lang.Byte
   char    java.lang.Character
   long    java.lang.Long
   double  java.lang.Double
   short   java.lang.Short
   int     java.lang.Integer
   float   java.lang.Float})

#?(:clj
(def boxed->unboxed
  {Integer   Integer/TYPE
   Long      Long/TYPE
   Float     Float/TYPE
   Short     Short/TYPE
   Boolean   Boolean/TYPE
   Byte      Byte/TYPE
   Character Character/TYPE
   Double    Double/TYPE
   Void      Void/TYPE}))

#?(:clj
(def unboxed->boxed
  {Integer/TYPE   Integer
   Long/TYPE      Long
   Float/TYPE     Float
   Short/TYPE     Short
   Boolean/TYPE   Boolean
   Byte/TYPE      Byte
   Character/TYPE Character
   Double/TYPE    Double
   Void/TYPE      Void}))

#?(:clj
(def unboxed->convertible
  "If the argument is a primitive Class, returns a set of primitive Classes
   to which the primitive Class can be casted"
  {Integer/TYPE #{Integer/TYPE Long/TYPE Short/TYPE Byte/TYPE}
   Float/TYPE   #{Float/TYPE   Double/TYPE}
   Double/TYPE  #{Double/TYPE  Float/TYPE}
   Long/TYPE    #{Long/TYPE    Integer/TYPE Short/TYPE Byte/TYPE}}))

;; (defn numeric?
;;   "Returns true if the given class is numeric"
;;   [c]
;;   (when c
;;     (.isAssignableFrom Number (box c))))

;; (def wider-than
;;   "If the argument is a numeric primitive Class, returns a set of primitive Classes
;;    that are narrower than the given one"
;;   {Long/TYPE    #{Integer/TYPE Short/TYPE Byte/TYPE}
;;    Integer/TYPE #{Short/TYPE Byte/TYPE}
;;    Float/TYPE   #{Integer/TYPE Short/TYPE Byte/TYPE Long/TYPE}
;;    Double/TYPE  #{Integer/TYPE Short/TYPE Byte/TYPE Long/TYPE Float/TYPE}
;;    Short/TYPE   #{Byte/TYPE}
;;    Byte/TYPE    #{}})

#?(:clj
; Returns true if it's possible to convert from c1 to c2
(defalias convertible? ana/convertible?))

#?(:clj
(def unboxed-type-map
  (zipmap (vals boxed-type-map) (keys boxed-type-map))))

(def prim-types                     #?(:clj  #{Boolean/TYPE Byte/TYPE Character/TYPE
                                               Short/TYPE Integer/TYPE Long/TYPE
                                               Float/TYPE Double/TYPE}
                                       :cljs #{js/Boolean js/Number}))
(def prim-types|unevaled            '#{boolean byte char short int long float double})

(def primitive-boxed-types          #?(:clj  #{Boolean Byte Character Short Integer Long Float Double}
                                       :cljs #{js/Boolean js/Number}))
(def primitive-boxed-types|unevaled #?(:clj  '#{java.lang.Boolean java.lang.Byte java.lang.Character
                                                java.lang.Short java.lang.Integer java.lang.Long
                                                java.lang.Float java.lang.Double}
                                       :cljs '#{js/Boolean js/Number}))

(def primitive-types                (set/union prim-types primitive-boxed-types))
(def primitive-types|unevaled       (set/union prim-types|unevaled primitive-boxed-types|unevaled))

(def prim|unevaled?      #(contains? prim-types|unevaled %))
(def primitive|unevaled? #(contains? primitive-types|unevaled %))

#?(:clj  (def  auto-unboxable|unevaled? #(contains? primitive-boxed-types|unevaled %))
   :cljs (defn auto-unboxable|unevaled? [x] (throw (>ex-info :unsupported "`auto-unboxable?` not supported by CLJS"))))

#?(:clj
(defn most-primitive-class-of [x]
  (let [c (class x)]
    (or (boxed->unboxed c) c))))

; ===== ARRAYS ===== ;

(def primitive-array-types ; TODO get from type/defs
  '{:clj  #{                                    bytes shorts chars ints longs floats doubles}
    :cljs #{ubytes ubytes-clamped ushorts uints bytes shorts       ints       floats doubles}})

(def cljs-typed-array-convertible-classes
  (let [cljs-typed-array-types '{:byte          js/Int8Array
                                 :ubyte         js/Uint8Array
                                 :ubyte-clamped js/Uint8ClampedArray
                                 :char          js/Uint16Array ; kind of
                                 :ushort        js/Uint16Array
                                 :short         js/Int16Array
                                 :int           js/Int32Array
                                 :uint          js/Uint32Array
                                 :float         js/Float32Array
                                 :double        js/Float64Array}
        generalize-type (fn->> name str/lower-case (remove #{\u}))]
    (->> cljs-typed-array-types
         (reduce
           (fn [ret [type-sym k]]
             (let [type-sym' (generalize-type type-sym)]
               (assoc ret k (->> cljs-typed-array-types
                                 (filter (fn [[type-sym1 _]] (-> type-sym1 generalize-type (= type-sym'))))
                                 (map    val)
                                 set
                                 (<- (disj k))))))
           {}))))

(def java-array-type-regex #"(\[+)(?:(Z|S|B|C|I|J|F|D)|(?:L(.+);))") ; TODO create this regex dynamically

#?(:clj
(defn array-depth-equal?
  "Efficiency prioritized because this fn is used a lot in `untyped.core.type/compare|class|class`"
  [^Class class0 ^Class class1]
  (let [s0 (.getName class0) s1 (.getName class1)]
    (loop [i 0]
      (let [c0  (.charAt s0 i)     c1  (.charAt s1 i)
            c0? (identical? c0 \[) c1? (identical? c1 \[)]
        (or (and (not c0?) (not c1?))
            (and c0? c1? (recur (unchecked-inc i)))))))))

#?(:clj
(defn nth-elem-type|clj
  "`x` must be Java array type (for now)
   Returns a string or symbol"
  [x n]
  (assert (or (string? x) (symbol? x) (class? x)) {:x x})
  (let [s (>name x)
        [java-array-type? brackets ?array-ident ?object-type]
          (re-matches java-array-type-regex s)
        array-depth (count brackets)]
    (assert java-array-type? {:x x})
    (assert (<= n array-depth) {:msg         "Can't get an element deeper than array depth"
                                :requested-n n
                                :array-depth array-depth})
    (if ?array-ident
        (if (= n array-depth)
            (utdef/array-ident->primitive-sym ?array-ident)
            (str (apply str (drop n brackets)) ?array-ident))
        (if (= n array-depth)
            (symbol ?object-type)
            (str (apply str (drop n brackets)) "L" ?object-type ";"))))))

(def default-types '#{#?(:clj java.lang.Object :cljs (quote default))})

(defn ->boxed|sym   [t]
  #?(:clj  (if-let [boxed   (get boxed-type-map   t)] boxed   t)
     :cljs (throw (>ex-info :unsupported "|->boxed| not supported by CLJS"))))

(defn ->unboxed|sym [t]
  #?(:clj  (if-let [unboxed (get unboxed-type-map t)] unboxed t)
     :cljs (throw (>ex-info :unsupported "|->boxed| not supported by CLJS"))))

(defn boxed?|sym    [t]
  #?(:clj  (contains? unboxed-type-map t)
     :cljs (throw (>ex-info :unsupported "|boxed?| not supported by CLJS"))))

#?(:clj (defn primitive-array-type? [^Class c]
          (and (class? c)
               (.isArray c)
               (-> c .getComponentType .isPrimitive))))

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

(def return-types-map
  {:clj (merge (:clj type-casts-map)
          '{boolean java.lang.Boolean/TYPE
            byte    java.lang.Byte/TYPE
            char    java.lang.Character/TYPE
            short   java.lang.Short/TYPE
            int     java.lang.Integer/TYPE
            long    java.lang.Long/TYPE
            float   java.lang.Float/TYPE
            double  java.lang.Double/TYPE})})

#?(:clj
(defn class>prim-subclasses [^Class c]
  (->> primitive-boxed-types
       (r/filter #(isa? % c))
       (r/map boxed->unboxed)
       (into #{}))))
