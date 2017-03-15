(ns quantum.test.benchmarks.jvm
  (:require
    [quantum.core.macros.defnt
      :refer [defnt defnt' defntp]]
    [quantum.core.collections :as coll
      :refer [reduce-pair]]
    [quantum.core.meta.bench
      :refer [bench complete-bench]]
    [quantum.core.type :as t
      :refer [static-cast]])
  (:import
    [quantum.core Numeric Fn]
    [quantum.core.data Array]
    [clojure.lang BigInt]
    [java.util Map HashMap IdentityHashMap]
    [java.lang.invoke MethodHandle MethodHandles MethodType]
  #_[cern.colt.map OpenIntObjectHashMap]))

; TODO try to use static methods instead of `invokevirtual` on the `reify`?

; ===== STATIC ===== ;

(defnt' +*-static
  ([#{byte char short int long float double} x
    #{byte char short int long float double} y]
    (Numeric/add x y)))

(defnt' +*-static-boxed
  (^Object [#{byte char short int long float double} x
            #{byte char short int long float double} y]
    (.cast Object (Numeric/add x y))))

(defnt' +*-static-interface-boxed
  ([^Comparable x #{long double} y]
    (.cast Object true)))

; ===== PROTOCOL ===== ;

(defntp +*-protocol-1-byte
  ([^byte   y x] (Numeric/add (byte x) y))
  ([^char   y x] (Numeric/add (byte x) y))
  ([^short  y x] (Numeric/add (byte x) y))
  ([^int    y x] (Numeric/add (byte x) y))
  ([^long   y x] (Numeric/add (byte x) y))
  ([^float  y x] (Numeric/add (byte x) y))
  ([^double y x] (Numeric/add (byte x) y)))

(defntp +*-protocol-1-char
  ([^byte   y x] (Numeric/add (char x) y))
  ([^char   y x] (Numeric/add (char x) y))
  ([^short  y x] (Numeric/add (char x) y))
  ([^int    y x] (Numeric/add (char x) y))
  ([^long   y x] (Numeric/add (char x) y))
  ([^float  y x] (Numeric/add (char x) y))
  ([^double y x] (Numeric/add (char x) y)))

(defntp +*-protocol-1-short
  ([^byte   y x] (Numeric/add (short x) y))
  ([^char   y x] (Numeric/add (short x) y))
  ([^short  y x] (Numeric/add (short x) y))
  ([^int    y x] (Numeric/add (short x) y))
  ([^long   y x] (Numeric/add (short x) y))
  ([^float  y x] (Numeric/add (short x) y))
  ([^double y x] (Numeric/add (short x) y)))

(defntp +*-protocol-1-int
  ([^byte   y x] (Numeric/add (int x) y))
  ([^char   y x] (Numeric/add (int x) y))
  ([^short  y x] (Numeric/add (int x) y))
  ([^int    y x] (Numeric/add (int x) y))
  ([^long   y x] (Numeric/add (int x) y))
  ([^float  y x] (Numeric/add (int x) y))
  ([^double y x] (Numeric/add (int x) y)))

(defntp +*-protocol-1-long
  ([^byte   y x] (Numeric/add (long x) y))
  ([^char   y x] (Numeric/add (long x) y))
  ([^short  y x] (Numeric/add (long x) y))
  ([^int    y x] (Numeric/add (long x) y))
  ([^long   y x] (Numeric/add (long x) y))
  ([^float  y x] (Numeric/add (long x) y))
  ([^double y x] (Numeric/add (long x) y)))

(defntp +*-protocol-1-float
  ([^byte   y x] (Numeric/add (float x) y))
  ([^char   y x] (Numeric/add (float x) y))
  ([^short  y x] (Numeric/add (float x) y))
  ([^int    y x] (Numeric/add (float x) y))
  ([^long   y x] (Numeric/add (float x) y))
  ([^float  y x] (Numeric/add (float x) y))
  ([^double y x] (Numeric/add (float x) y)))

(defntp +*-protocol-1-double
  ([^byte   y x] (Numeric/add (double x) y))
  ([^char   y x] (Numeric/add (double x) y))
  ([^short  y x] (Numeric/add (double x) y))
  ([^int    y x] (Numeric/add (double x) y))
  ([^long   y x] (Numeric/add (double x) y))
  ([^float  y x] (Numeric/add (double x) y))
  ([^double y x] (Numeric/add (double x) y)))

(defntp +*-protocol-0
  ([^byte   x y] (+*-protocol-1-byte   y x))
  ([^char   x y] (+*-protocol-1-char   y x))
  ([^short  x y] (+*-protocol-1-short  y x))
  ([^int    x y] (+*-protocol-1-int    y x))
  ([^long   x y] (+*-protocol-1-long   y x))
  ([^float  x y] (+*-protocol-1-float  y x))
  ([^double x y] (+*-protocol-1-double y x)))

; ===== `INSTANCE?` DISPATCH ===== ;

(defn dispatch [x y]
  (cond (instance? Byte      x) (cond (instance? Byte      y) (+*-static (byte   x) (byte   y))
                                      (instance? Character y) (+*-static (byte   x) (char   y))
                                      (instance? Short     y) (+*-static (byte   x) (short  y))
                                      (instance? Integer   y) (+*-static (byte   x) (int    y))
                                      (instance? Long      y) (+*-static (byte   x) (long   y))
                                      (instance? Float     y) (+*-static (byte   x) (float  y))
                                      (instance? Double    y) (+*-static (byte   x) (double y)))
        (instance? Character x) (cond (instance? Byte      y) (+*-static (char   x) (byte   y))
                                      (instance? Character y) (+*-static (char   x) (char   y))
                                      (instance? Short     y) (+*-static (char   x) (short  y))
                                      (instance? Integer   y) (+*-static (char   x) (int    y))
                                      (instance? Long      y) (+*-static (char   x) (long   y))
                                      (instance? Float     y) (+*-static (char   x) (float  y))
                                      (instance? Double    y) (+*-static (char   x) (double y)))
        (instance? Short     x) (cond (instance? Byte      y) (+*-static (short  x) (byte   y))
                                      (instance? Character y) (+*-static (short  x) (char   y))
                                      (instance? Short     y) (+*-static (short  x) (short  y))
                                      (instance? Integer   y) (+*-static (short  x) (int    y))
                                      (instance? Long      y) (+*-static (short  x) (long   y))
                                      (instance? Float     y) (+*-static (short  x) (float  y))
                                      (instance? Double    y) (+*-static (short  x) (double y)))
        (instance? Integer   x) (cond (instance? Byte      y) (+*-static (int    x) (byte   y))
                                      (instance? Character y) (+*-static (int    x) (char   y))
                                      (instance? Short     y) (+*-static (int    x) (short  y))
                                      (instance? Integer   y) (+*-static (int    x) (int    y))
                                      (instance? Long      y) (+*-static (int    x) (long   y))
                                      (instance? Float     y) (+*-static (int    x) (float  y))
                                      (instance? Double    y) (+*-static (int    x) (double y)))
        (instance? Long      x) (cond (instance? Byte      y) (+*-static (long   x) (byte   y))
                                      (instance? Character y) (+*-static (long   x) (char   y))
                                      (instance? Short     y) (+*-static (long   x) (short  y))
                                      (instance? Integer   y) (+*-static (long   x) (int    y))
                                      (instance? Long      y) (+*-static (long   x) (long   y))
                                      (instance? Float     y) (+*-static (long   x) (float  y))
                                      (instance? Double    y) (+*-static (long   x) (double y)))
        (instance? Float     x) (cond (instance? Byte      y) (+*-static (float  x) (byte   y))
                                      (instance? Character y) (+*-static (float  x) (char   y))
                                      (instance? Short     y) (+*-static (float  x) (short  y))
                                      (instance? Integer   y) (+*-static (float  x) (int    y))
                                      (instance? Long      y) (+*-static (float  x) (long   y))
                                      (instance? Float     y) (+*-static (float  x) (float  y))
                                      (instance? Double    y) (+*-static (float  x) (double y)))
        (instance? Double    x) (cond (instance? Byte      y) (+*-static (double x) (byte   y))
                                      (instance? Character y) (+*-static (double x) (char   y))
                                      (instance? Short     y) (+*-static (double x) (short  y))
                                      (instance? Integer   y) (+*-static (double x) (int    y))
                                      (instance? Long      y) (+*-static (double x) (long   y))
                                      (instance? Float     y) (+*-static (double x) (float  y))
                                      (instance? Double    y) (+*-static (double x) (double y)))))

(defn case-string-dispatch [^Object x ^Object y]
  (case (if (nil? x) nil (-> x .getClass .getName))
        "java.lang.Byte"      (case (if (nil? y) nil (-> y .getClass .getName))
                                    "java.lang.Byte"      (+*-static (byte   x) (byte   y))
                                    "java.lang.Character" (+*-static (byte   x) (char   y))
                                    "java.lang.Short"     (+*-static (byte   x) (short  y))
                                    "java.lang.Integer"   (+*-static (byte   x) (int    y))
                                    "java.lang.Long"      (+*-static (byte   x) (long   y))
                                    "java.lang.Float"     (+*-static (byte   x) (float  y))
                                    "java.lang.Double"    (+*-static (byte   x) (double y)))
        "java.lang.Character" (case (if (nil? y) nil (-> y .getClass .getName))
                                    "java.lang.Byte"      (+*-static (char   x) (byte   y))
                                    "java.lang.Character" (+*-static (char   x) (char   y))
                                    "java.lang.Short"     (+*-static (char   x) (short  y))
                                    "java.lang.Integer"   (+*-static (char   x) (int    y))
                                    "java.lang.Long"      (+*-static (char   x) (long   y))
                                    "java.lang.Float"     (+*-static (char   x) (float  y))
                                    "java.lang.Double"    (+*-static (char   x) (double y)))
        "java.lang.Short"     (case (if (nil? y) nil (-> y .getClass .getName))
                                    "java.lang.Byte"      (+*-static (short  x) (byte   y))
                                    "java.lang.Character" (+*-static (short  x) (char   y))
                                    "java.lang.Short"     (+*-static (short  x) (short  y))
                                    "java.lang.Integer"   (+*-static (short  x) (int    y))
                                    "java.lang.Long"      (+*-static (short  x) (long   y))
                                    "java.lang.Float"     (+*-static (short  x) (float  y))
                                    "java.lang.Double"    (+*-static (short  x) (double y)))
        "java.lang.Integer"   (case (if (nil? y) nil (-> y .getClass .getName))
                                    "java.lang.Byte"      (+*-static (int    x) (byte   y))
                                    "java.lang.Character" (+*-static (int    x) (char   y))
                                    "java.lang.Short"     (+*-static (int    x) (short  y))
                                    "java.lang.Integer"   (+*-static (int    x) (int    y))
                                    "java.lang.Long"      (+*-static (int    x) (long   y))
                                    "java.lang.Float"     (+*-static (int    x) (float  y))
                                    "java.lang.Double"    (+*-static (int    x) (double y)))
        "java.lang.Long"      (case (if (nil? y) nil (-> y .getClass .getName))
                                    "java.lang.Byte"      (+*-static (long   x) (byte   y))
                                    "java.lang.Character" (+*-static (long   x) (char   y))
                                    "java.lang.Short"     (+*-static (long   x) (short  y))
                                    "java.lang.Integer"   (+*-static (long   x) (int    y))
                                    "java.lang.Long"      (+*-static (long   x) (long   y))
                                    "java.lang.Float"     (+*-static (long   x) (float  y))
                                    "java.lang.Double"    (+*-static (long   x) (double y)))
        "java.lang.Float"     (case (if (nil? y) nil (-> y .getClass .getName))
                                    "java.lang.Byte"      (+*-static (float  x) (byte   y))
                                    "java.lang.Character" (+*-static (float  x) (char   y))
                                    "java.lang.Short"     (+*-static (float  x) (short  y))
                                    "java.lang.Integer"   (+*-static (float  x) (int    y))
                                    "java.lang.Long"      (+*-static (float  x) (long   y))
                                    "java.lang.Float"     (+*-static (float  x) (float  y))
                                    "java.lang.Double"    (+*-static (float  x) (double y)))
        "java.lang.Double"    (case (if (nil? y) nil (-> y .getClass .getName))
                                    "java.lang.Byte"      (+*-static (double x) (byte   y))
                                    "java.lang.Character" (+*-static (double x) (char   y))
                                    "java.lang.Short"     (+*-static (double x) (short  y))
                                    "java.lang.Integer"   (+*-static (double x) (int    y))
                                    "java.lang.Long"      (+*-static (double x) (long   y))
                                    "java.lang.Float"     (+*-static (double x) (float  y))
                                    "java.lang.Double"    (+*-static (double x) (double y)))))

(defmacro compile-hash [class-sym] (-> class-sym eval System/identityHashCode))

(eval
 `(defn ~'case-hash-dispatch [^Object x# ^Object y#]
    (case (if (nil? x#) (int 0) (-> x# .getClass System/identityHashCode))
          ~(compile-hash Byte)      (case (if (nil? y#) (int 0) (-> y# .getClass System/identityHashCode))
                                      ~(compile-hash Byte)      (+*-static (byte   x#) (byte   y#))
                                      ~(compile-hash Character) (+*-static (byte   x#) (char   y#))
                                      ~(compile-hash Short)     (+*-static (byte   x#) (short  y#))
                                      ~(compile-hash Integer)   (+*-static (byte   x#) (int    y#))
                                      ~(compile-hash Long)      (+*-static (byte   x#) (long   y#))
                                      ~(compile-hash Float)     (+*-static (byte   x#) (float  y#))
                                      ~(compile-hash Double)    (+*-static (byte   x#) (double y#)))
          ~(compile-hash Character) (case (if (nil? y#) (int 0) (-> y# .getClass System/identityHashCode))
                                      ~(compile-hash Byte)      (+*-static (char   x#) (byte   y#))
                                      ~(compile-hash Character) (+*-static (char   x#) (char   y#))
                                      ~(compile-hash Short)     (+*-static (char   x#) (short  y#))
                                      ~(compile-hash Integer)   (+*-static (char   x#) (int    y#))
                                      ~(compile-hash Long)      (+*-static (char   x#) (long   y#))
                                      ~(compile-hash Float)     (+*-static (char   x#) (float  y#))
                                      ~(compile-hash Double)    (+*-static (char   x#) (double y#)))
          ~(compile-hash Short)     (case (if (nil? y#) (int 0) (-> y# .getClass System/identityHashCode))
                                      ~(compile-hash Byte)      (+*-static (short  x#) (byte   y#))
                                      ~(compile-hash Character) (+*-static (short  x#) (char   y#))
                                      ~(compile-hash Short)     (+*-static (short  x#) (short  y#))
                                      ~(compile-hash Integer)   (+*-static (short  x#) (int    y#))
                                      ~(compile-hash Long)      (+*-static (short  x#) (long   y#))
                                      ~(compile-hash Float)     (+*-static (short  x#) (float  y#))
                                      ~(compile-hash Double)    (+*-static (short  x#) (double y#)))
          ~(compile-hash Integer)   (case (if (nil? y#) (int 0) (-> y# .getClass System/identityHashCode))
                                      ~(compile-hash Byte)      (+*-static (int    x#) (byte   y#))
                                      ~(compile-hash Character) (+*-static (int    x#) (char   y#))
                                      ~(compile-hash Short)     (+*-static (int    x#) (short  y#))
                                      ~(compile-hash Integer)   (+*-static (int    x#) (int    y#))
                                      ~(compile-hash Long)      (+*-static (int    x#) (long   y#))
                                      ~(compile-hash Float)     (+*-static (int    x#) (float  y#))
                                      ~(compile-hash Double)    (+*-static (int    x#) (double y#)))
          ~(compile-hash Long)      (case (if (nil? y#) (int 0) (-> y# .getClass System/identityHashCode))
                                      ~(compile-hash Byte)      (+*-static (long   x#) (byte   y#))
                                      ~(compile-hash Character) (+*-static (long   x#) (char   y#))
                                      ~(compile-hash Short)     (+*-static (long   x#) (short  y#))
                                      ~(compile-hash Integer)   (+*-static (long   x#) (int    y#))
                                      ~(compile-hash Long)      (+*-static (long   x#) (long   y#))
                                      ~(compile-hash Float)     (+*-static (long   x#) (float  y#))
                                      ~(compile-hash Double)    (+*-static (long   x#) (double y#)))
          ~(compile-hash Float)     (case (if (nil? y#) (int 0) (-> y# .getClass System/identityHashCode))
                                      ~(compile-hash Byte)      (+*-static (float  x#) (byte   y#))
                                      ~(compile-hash Character) (+*-static (float  x#) (char   y#))
                                      ~(compile-hash Short)     (+*-static (float  x#) (short  y#))
                                      ~(compile-hash Integer)   (+*-static (float  x#) (int    y#))
                                      ~(compile-hash Long)      (+*-static (float  x#) (long   y#))
                                      ~(compile-hash Float)     (+*-static (float  x#) (float  y#))
                                      ~(compile-hash Double)    (+*-static (float  x#) (double y#)))
          ~(compile-hash Double)    (case (if (nil? y#) (int 0) (-> y# .getClass System/identityHashCode))
                                      ~(compile-hash Byte)      (+*-static (double x#) (byte   y#))
                                      ~(compile-hash Character) (+*-static (double x#) (char   y#))
                                      ~(compile-hash Short)     (+*-static (double x#) (short  y#))
                                      ~(compile-hash Integer)   (+*-static (double x#) (int    y#))
                                      ~(compile-hash Long)      (+*-static (double x#) (long   y#))
                                      ~(compile-hash Float)     (+*-static (double x#) (float  y#))
                                      ~(compile-hash Double)    (+*-static (double x#) (double y#))))))
; ===== LOOKUP MAP, IMMUTABLE ===== ;

(defn gen-dispatch-map [create-map]
  (create-map
    Byte       (create-map
                 Byte      (fn [x y] (+*-static (byte   x) (byte   y)))
                 Character (fn [x y] (+*-static (byte   x) (char   y)))
                 Short     (fn [x y] (+*-static (byte   x) (short  y)))
                 Integer   (fn [x y] (+*-static (byte   x) (int    y)))
                 Long      (fn [x y] (+*-static (byte   x) (long   y)))
                 Float     (fn [x y] (+*-static (byte   x) (float  y)))
                 Double    (fn [x y] (+*-static (byte   x) (double y))))
    Character  (create-map
                 Byte      (fn [x y] (+*-static (char   x) (byte   y)))
                 Character (fn [x y] (+*-static (char   x) (char   y)))
                 Short     (fn [x y] (+*-static (char   x) (short  y)))
                 Integer   (fn [x y] (+*-static (char   x) (int    y)))
                 Long      (fn [x y] (+*-static (char   x) (long   y)))
                 Float     (fn [x y] (+*-static (char   x) (float  y)))
                 Double    (fn [x y] (+*-static (char   x) (double y))))
    Short      (create-map
                 Byte      (fn [x y] (+*-static (short  x) (byte   y)))
                 Character (fn [x y] (+*-static (short  x) (char   y)))
                 Short     (fn [x y] (+*-static (short  x) (short  y)))
                 Integer   (fn [x y] (+*-static (short  x) (int    y)))
                 Long      (fn [x y] (+*-static (short  x) (long   y)))
                 Float     (fn [x y] (+*-static (short  x) (float  y)))
                 Double    (fn [x y] (+*-static (short  x) (double y))))
    Integer    (create-map
                 Byte      (fn [x y] (+*-static (int    x) (byte   y)))
                 Character (fn [x y] (+*-static (int    x) (char   y)))
                 Short     (fn [x y] (+*-static (int    x) (short  y)))
                 Integer   (fn [x y] (+*-static (int    x) (int    y)))
                 Long      (fn [x y] (+*-static (int    x) (long   y)))
                 Float     (fn [x y] (+*-static (int    x) (float  y)))
                 Double    (fn [x y] (+*-static (int    x) (double y))))
    Long       (create-map
                 Byte      (fn [x y] (+*-static (long   x) (byte   y)))
                 Character (fn [x y] (+*-static (long   x) (char   y)))
                 Short     (fn [x y] (+*-static (long   x) (short  y)))
                 Integer   (fn [x y] (+*-static (long   x) (int    y)))
                 Long      (fn [x y] (+*-static (long   x) (long   y)))
                 Float     (fn [x y] (+*-static (long   x) (float  y)))
                 Double    (fn [x y] (+*-static (long   x) (double y))))
    Float      (create-map
                 Byte      (fn [x y] (+*-static (float  x) (byte   y)))
                 Character (fn [x y] (+*-static (float  x) (char   y)))
                 Short     (fn [x y] (+*-static (float  x) (short  y)))
                 Integer   (fn [x y] (+*-static (float  x) (int    y)))
                 Long      (fn [x y] (+*-static (float  x) (long   y)))
                 Float     (fn [x y] (+*-static (float  x) (float  y)))
                 Double    (fn [x y] (+*-static (float  x) (double y))))
    Double     (create-map
                 Byte      (fn [x y] (+*-static (double x) (byte   y)))
                 Character (fn [x y] (+*-static (double x) (char   y)))
                 Short     (fn [x y] (+*-static (double x) (short  y)))
                 Integer   (fn [x y] (+*-static (double x) (int    y)))
                 Long      (fn [x y] (+*-static (double x) (long   y)))
                 Float     (fn [x y] (+*-static (double x) (float  y)))
                 Double    (fn [x y] (+*-static (double x) (double y))))))

(defn gen-inlined-dispatch-map [create-map]
  (create-map
    Byte       (create-map
                 Byte      (fn [x y] (Numeric/add (byte   x) (byte   y)))
                 Character (fn [x y] (Numeric/add (byte   x) (char   y)))
                 Short     (fn [x y] (Numeric/add (byte   x) (short  y)))
                 Integer   (fn [x y] (Numeric/add (byte   x) (int    y)))
                 Long      (fn [x y] (Numeric/add (byte   x) (long   y)))
                 Float     (fn [x y] (Numeric/add (byte   x) (float  y)))
                 Double    (fn [x y] (Numeric/add (byte   x) (double y))))
    Character  (create-map
                 Byte      (fn [x y] (Numeric/add (char   x) (byte   y)))
                 Character (fn [x y] (Numeric/add (char   x) (char   y)))
                 Short     (fn [x y] (Numeric/add (char   x) (short  y)))
                 Integer   (fn [x y] (Numeric/add (char   x) (int    y)))
                 Long      (fn [x y] (Numeric/add (char   x) (long   y)))
                 Float     (fn [x y] (Numeric/add (char   x) (float  y)))
                 Double    (fn [x y] (Numeric/add (char   x) (double y))))
    Short      (create-map
                 Byte      (fn [x y] (Numeric/add (short  x) (byte   y)))
                 Character (fn [x y] (Numeric/add (short  x) (char   y)))
                 Short     (fn [x y] (Numeric/add (short  x) (short  y)))
                 Integer   (fn [x y] (Numeric/add (short  x) (int    y)))
                 Long      (fn [x y] (Numeric/add (short  x) (long   y)))
                 Float     (fn [x y] (Numeric/add (short  x) (float  y)))
                 Double    (fn [x y] (Numeric/add (short  x) (double y))))
    Integer    (create-map
                 Byte      (fn [x y] (Numeric/add (int    x) (byte   y)))
                 Character (fn [x y] (Numeric/add (int    x) (char   y)))
                 Short     (fn [x y] (Numeric/add (int    x) (short  y)))
                 Integer   (fn [x y] (Numeric/add (int    x) (int    y)))
                 Long      (fn [x y] (Numeric/add (int    x) (long   y)))
                 Float     (fn [x y] (Numeric/add (int    x) (float  y)))
                 Double    (fn [x y] (Numeric/add (int    x) (double y))))
    Long       (create-map
                 Byte      (fn [x y] (Numeric/add (long   x) (byte   y)))
                 Character (fn [x y] (Numeric/add (long   x) (char   y)))
                 Short     (fn [x y] (Numeric/add (long   x) (short  y)))
                 Integer   (fn [x y] (Numeric/add (long   x) (int    y)))
                 Long      (fn [x y] (Numeric/add (long   x) (long   y)))
                 Float     (fn [x y] (Numeric/add (long   x) (float  y)))
                 Double    (fn [x y] (Numeric/add (long   x) (double y))))
    Float      (create-map
                 Byte      (fn [x y] (Numeric/add (float  x) (byte   y)))
                 Character (fn [x y] (Numeric/add (float  x) (char   y)))
                 Short     (fn [x y] (Numeric/add (float  x) (short  y)))
                 Integer   (fn [x y] (Numeric/add (float  x) (int    y)))
                 Long      (fn [x y] (Numeric/add (float  x) (long   y)))
                 Float     (fn [x y] (Numeric/add (float  x) (float  y)))
                 Double    (fn [x y] (Numeric/add (float  x) (double y))))
    Double     (create-map
                 Byte      (fn [x y] (Numeric/add (double x) (byte   y)))
                 Character (fn [x y] (Numeric/add (double x) (char   y)))
                 Short     (fn [x y] (Numeric/add (double x) (short  y)))
                 Integer   (fn [x y] (Numeric/add (double x) (int    y)))
                 Long      (fn [x y] (Numeric/add (double x) (long   y)))
                 Float     (fn [x y] (Numeric/add (double x) (float  y)))
                 Double    (fn [x y] (Numeric/add (double x) (double y))))))

(def dispatch-map (gen-dispatch-map hash-map))

(defn dispatch-with-map [x y]
  (let [f (some-> dispatch-map (get (class x)) (get (class y)))]
    (assert (some? f))
    (f x y)))

; ===== LOOKUP MAP, MUTABLE ===== ;

(defn map!* [constructor & args]
  (assert (-> args count even?))
  (reduce-pair (fn [ret k v] (.put ^Map ret k v) ret) (constructor) args))

(def hash-map! (partial map!* #(HashMap.)))

(def ^HashMap dispatch-map-mutable (gen-dispatch-map hash-map!))

(defn dispatch-with-map-mutable [x y]
  (let [f (some-> dispatch-map-mutable ^HashMap (.get (class x)) (.get (class y)))]
    (assert (some? f))
    (f x y)))

; ===== LOOKUP MAP, IDENTITY MUTABLE ===== ;

(def identity-hash-map! (partial map!* #(IdentityHashMap.)))

(def ^IdentityHashMap dispatch-identity-map-mutable (gen-dispatch-map identity-hash-map!))

(defn dispatch-with-identity-map-mutable [x y]
  (if-let [a0 (.get dispatch-identity-map-mutable (clojure.lang.Util/classOf x))]
    (if-let [a1 (.get ^IdentityHashMap a0 (clojure.lang.Util/classOf y))]
      (a1 x y)
      (throw (Exception. "Method not found")))
    (throw (Exception. "Method not found"))))

; ===== LOOKUP MAP, INT->OBJECT MUTABLE ===== ;

; Colt's OpenIntObjectHashMap doesn't help here

; ===== CUSTOMIZED (CLOJURE NUMERICS) ===== ;

(defn argtypes-unknown [a b] (+ a b))

; ===== INVOKEDYNAMIC (JAVA 8 METHOD HANDLES) ===== ;

(defmacro compile-time-class [x] (class (eval x)))

(def ^MethodHandle method-double-double
  (.findVirtual Fn/fnLookup (compile-time-class +*-static-reified) "_PLUS__STAR_Static"
    (Fn/methodType Double/TYPE Double/TYPE Double/TYPE)))

(def ^MethodHandle method
  (.unreflect Fn/fnLookup (.getMethod (class +*-static-reified) "_PLUS__STAR_Static")))

; 13.650164 ns
(complete-bench (do (Fn/invoke method-double-double +*-static-reified 1.0 3.0)
                    #_(Fn/invoke method +*-static-reified 1   3  )))

(complete-bench (do (Fn/invoke method +*-static-reified 1.0 3.0)
                    #_(Fn/invoke method +*-static-reified 1   3  )))

; ===== BENCHMARKS ===== ;

; All benchmarks use the lower quantile (2.5%) instead of considering outliers.
; It's more fair to benchmark this way.
; Also, all benchmarks were run multiple times to ensure complete and utter JVM warmup.

; 4.911254 ns
(complete-bench (do (+ 1.0 3.0)
                    (+ 1   3  )))
; Same time
(complete-bench (do (Numeric/add 1.0 3.0)
                    (Numeric/add 1   3  )))

; 5.970614 ns
; May currently have slightly worse performance since it isn't inlined, and is an instance instead of a static method
(let [^quantum.test.benchmarks.jvm._PLUS__STAR_StaticInterface
        +*-static-reified-direct @#'+*-static-reified] ; to get rid of var indirection, which can't be optimized away by the JVM because is marked as `volatile`
  (complete-bench (do (. +*-static-reified-direct _PLUS__STAR_Static 1.0 3.0)
                      (. +*-static-reified-direct _PLUS__STAR_Static 1   3  ))))

; 6.361026 ns
; May currently have slightly worse performance since it isn't inlined, and is an instance instead of a static method
(complete-bench (do (+*-static 1.0 3.0)
                    (+*-static 1   3  )))

; 22.369795 ns (4.55x)
(complete-bench (do (argtypes-unknown 1.0 3.0)
                    (argtypes-unknown 1   3  )))

; 30.778042 ns (6.27x) (on first run is 23.467812 ns, 4.78x)
(complete-bench (do (Fn/dispatch2 dispatch-identity-map-mutable 1.0 3.0)
                    (Fn/dispatch2 dispatch-identity-map-mutable 1   3  )))

; 34.343497 ns
#_(complete-bench (do (Fn/dispatch2 dispatch-map-mutable 1.0 3.0)
                    (Fn/dispatch2 dispatch-map-mutable 1   3  )))

; 37.636059 ns (7.66x)
(complete-bench (do (case-hash-dispatch 1.0 3.0)
                    (case-hash-dispatch 1   3  )))

; 38.843166 ns
(complete-bench (do (case-string-dispatch 1.0 3.0)
                    (case-string-dispatch 1   3  )))

; 51.184897 ns (10.42x) (on first run is 33.707466 ns, 6.86x)
(complete-bench (do (+*-protocol-0 1.0 3.0)
                    (+*-protocol-0 1   3  )))

; 48.844710 ns
(complete-bench (do (dispatch 1.0 3.0)
                    (dispatch 1   3  )))

; 40.611242 ns
(complete-bench (do (dispatch-with-identity-map-mutable 1.0 3.0)
                    (dispatch-with-identity-map-mutable 1   3  )))

; 42.714549 ns
(complete-bench (do (dispatch-with-map-mutable 1.0 3.0)
                    (dispatch-with-map-mutable 1   3  )))

; 79.343540 ns
#_(complete-bench (do (dispatch-with-int->object-map-mutable 1.0 3.0)
                    (dispatch-with-int->object-map-mutable 1   3  )))

; 188.686935 ns
(complete-bench (.invokeWithArguments method (Array/newObjectArray +*-static-reified 1.0 3.0)))

; 212.066270 ns
(complete-bench (do (dispatch-with-map 1.0 3.0)
                    (dispatch-with-map 1   3  )))

; Didn't even complete
(complete-bench
  (Fn/invoke (.findVirtual Fn/fnLookup
                           (compile-time-class +*-static-boxed-reified)
                           "_PLUS__STAR_StaticBoxed"
                           (.unwrap (Fn/methodType Object Long Long)))
             +*-static-boxed-reified 1 2))

#_"Resolved:
- Protocols may be 7.2x slower in this case but they're currently the only viable choice for multi-arg dispatch.
- Explore http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2216.pdf — perhaps a Clojure(Script)
  implementation might yield the same speed benefits — i.e. *no statistical difference in performance
  with direct dispatch*!

"

; With `(dotimes [i 1000000000] ...)`
; "Elapsed time: 365.916154 msecs"   +, Add
; "Elapsed time: 941.336378 msecs"   Static
; "Elapsed time: 20147.633612 msecs" Boxed math
; "Elapsed time: 34638.789978 msecs" Dispatch, Dispatch Inlined
; "Elapsed time: 46755.719601 msecs" Protocol
; "Elapsed time: 68588.693153 msecs" Hash Dispatch

; ===== SINGLE DISPATCH ===== ;
; Takeaways: - quantum `get` is just as good as handwritten.
;            - one protocol dispatch here is ~2x a direct call; often won't have to do N dispatches depending on type info known at compile time

; 5.580126 ns
(let [v [1 2 3 4 5]]
  (complete-bench (.get v 3)))

; 7.644827 ns ; will be same as direct dispatch when inlined
(let [v [1 2 3 4 5]]
  (complete-bench (quantum.core.collections.core/get v 3)))

; 10.542661 ns
(let [v [1 2 3 4 5]]
  (complete-bench (clojure.core/get v 3)))

; 10.686585 ns ; because it's on the fast track
(let [v [1 2 3 4 5]]
  (complete-bench (quantum.core.collections.core/get-protocol v 3)))


; 7.438636 ns
(let [v (long-array [1 2 3 4 5])]
  (complete-bench (clojure.core/aget v 3)))

; 7.649213 ns — statistically equivalent
(let [v (long-array [1 2 3 4 5])]
  (complete-bench (quantum.core.data.Array/get v 3)))

; 8.691139 ns
(let [v (long-array [1 2 3 4 5])]
  (complete-bench (quantum.core.collections.core/get v 3)))

; 15.832480 ns ; good performance, but not on the fast track
(let [v (long-array [1 2 3 4 5])]
  (complete-bench (quantum.core.collections.core/get-protocol v 3)))

; 53.855997 ns ; semi-reflection going on here
(let [v (long-array [1 2 3 4 5])]
  (complete-bench (clojure.core/get v 3)))
