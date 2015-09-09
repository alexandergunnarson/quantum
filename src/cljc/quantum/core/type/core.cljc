(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.type.core
  (:require-quantum [ns log pr err map set logic fn])
  (:require [quantum.core.type.bootstrap :as boot])
  #?(:cljs
      (:require-macros [quantum.core.type.bootstrap :as boot])))

#?(:cljs (def class type))

(boot/def-types #?(:clj :clj :cljs :cljs))

(defn listy? [obj] (seq? obj)
  #_(->> obj class
         (contains? (get types 'listy?))))

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

#?(:clj
(def unboxed-type-map
  (zipmap (vals boxed-type-map) (keys boxed-type-map))))

#?(:clj (def primitive-types (-> types-unevaled :clj (get 'primitive?))))
#?(:clj (def primitive? (partial contains? primitive-types)))
#?(:clj (def auto-unboxable? primitive?))
#?(:clj (def default-types   (-> types-unevaled :clj (get :any))))

#?(:clj (defn ->boxed   [t] (if-let [boxed   (get boxed-type-map   t)] boxed   t)))
#?(:clj (defn ->unboxed [t] (if-let [unboxed (get unboxed-type-map t)] unboxed t)))
#?(:clj (defn boxed? [t] (contains? unboxed-type-map t)))

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