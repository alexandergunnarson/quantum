(ns quantum.test.benchmarks.jvm
  (:require
    [quantum.core.macros.defnt
      :refer [defnt defnt' defntp]]
    [quantum.core.collections :as coll
      :refer [reduce-2]]
    [quantum.core.meta.bench
      :refer [bench complete-bench]]
    [quantum.core.type :as type
      :refer [static-cast]])
  (:import
    [quantum.core Numeric Fn]
    [quantum.core.data Array]
    [clojure.lang BigInt]
    [java.util HashMap IdentityHashMap]
    [java.lang.invoke MethodHandle MethodHandles MethodType]))

(defmacro bad-bench [n & body]
  `(time (dotimes [i# ~n] ~@body)))

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
     Character (create-map
                 Byte      (fn [x y] (+*-static (char   x) (byte   y)))
                 Character (fn [x y] (+*-static (char   x) (char   y)))
                 Short     (fn [x y] (+*-static (char   x) (short  y)))
                 Integer   (fn [x y] (+*-static (char   x) (int    y)))
                 Long      (fn [x y] (+*-static (char   x) (long   y)))
                 Float     (fn [x y] (+*-static (char   x) (float  y)))
                 Double    (fn [x y] (+*-static (char   x) (double y))))
     Short     (create-map
                 Byte      (fn [x y] (+*-static (short  x) (byte   y)))
                 Character (fn [x y] (+*-static (short  x) (char   y)))
                 Short     (fn [x y] (+*-static (short  x) (short  y)))
                 Integer   (fn [x y] (+*-static (short  x) (int    y)))
                 Long      (fn [x y] (+*-static (short  x) (long   y)))
                 Float     (fn [x y] (+*-static (short  x) (float  y)))
                 Double    (fn [x y] (+*-static (short  x) (double y))))
     Integer   (create-map
                 Byte      (fn [x y] (+*-static (int    x) (byte   y)))
                 Character (fn [x y] (+*-static (int    x) (char   y)))
                 Short     (fn [x y] (+*-static (int    x) (short  y)))
                 Integer   (fn [x y] (+*-static (int    x) (int    y)))
                 Long      (fn [x y] (+*-static (int    x) (long   y)))
                 Float     (fn [x y] (+*-static (int    x) (float  y)))
                 Double    (fn [x y] (+*-static (int    x) (double y))))
     Long      (create-map
                 Byte      (fn [x y] (+*-static (long   x) (byte   y)))
                 Character (fn [x y] (+*-static (long   x) (char   y)))
                 Short     (fn [x y] (+*-static (long   x) (short  y)))
                 Integer   (fn [x y] (+*-static (long   x) (int    y)))
                 Long      (fn [x y] (+*-static (long   x) (long   y)))
                 Float     (fn [x y] (+*-static (long   x) (float  y)))
                 Double    (fn [x y] (+*-static (long   x) (double y))))
     Float     (create-map
                 Byte      (fn [x y] (+*-static (float  x) (byte   y)))
                 Character (fn [x y] (+*-static (float  x) (char   y)))
                 Short     (fn [x y] (+*-static (float  x) (short  y)))
                 Integer   (fn [x y] (+*-static (float  x) (int    y)))
                 Long      (fn [x y] (+*-static (float  x) (long   y)))
                 Float     (fn [x y] (+*-static (float  x) (float  y)))
                 Double    (fn [x y] (+*-static (float  x) (double y))))
     Double    (create-map
                 Byte      (fn [x y] (+*-static (double x) (byte   y)))
                 Character (fn [x y] (+*-static (double x) (char   y)))
                 Short     (fn [x y] (+*-static (double x) (short  y)))
                 Integer   (fn [x y] (+*-static (double x) (int    y)))
                 Long      (fn [x y] (+*-static (double x) (long   y)))
                 Float     (fn [x y] (+*-static (double x) (float  y)))
                 Double    (fn [x y] (+*-static (double x) (double y))))))

(def dispatch-map (gen-dispatch-map hash-map))

(defn dispatch-with-map [x y]
  (let [f (some-> dispatch-map (get (class x)) (get (class y)))]
    (assert (some? f))
    (f x y)))

; ===== LOOKUP MAP, MUTABLE ===== ;

(defn hash-map! [& args]
  (assert (-> args count even?))
  (reduce-2 (fn [ret k v] (.put ^HashMap ret k v) ret) (HashMap.) args))

(def dispatch-map-mutable (gen-dispatch-map hash-map!))

(defn dispatch-with-map-mutable [x y]
  (let [f (some-> ^HashMap dispatch-map-mutable ^HashMap (.get (class x)) (.get (class y)))]
    (assert (some? f))
    (f x y)))

; ===== LOOKUP MAP, IDENTITY MUTABLE ===== ;

(defn identity-hash-map! [& args]
  (assert (-> args count even?))
  (reduce-2 (fn [ret k v] (.put ^IdentityHashMap ret k v) ret) (IdentityHashMap.) args))

(def ^IdentityHashMap dispatch-identity-map-mutable (gen-dispatch-map identity-hash-map!))

(defn dispatch-with-identity-map-mutable [x y]
  (if-let [a0 (.get dispatch-identity-map-mutable (clojure.lang.Util/classOf x))]
    (if-let [a1 (.get ^IdentityHashMap a0 (clojure.lang.Util/classOf y))]
      (a1 x y)
      (throw (Exception. "Method not found")))
    (throw (Exception. "Method not found"))))

; ===== INVOKEDYNAMIC (JAVA 8 METHOD HANDLES) ===== ;

(defmacro compile-time-class [x] (class (eval x)))

(def ^MethodHandle method-double-double
  (.findVirtual Fn/fnLookup (compile-time-class +*-static-reified) "_PLUS__STAR_Static"
    (Fn/methodType Double/TYPE Double/TYPE Double/TYPE)))

(def ^MethodHandle method
  (.unreflect Fn/fnLookup (.getMethod (class +*-static-reified) "_PLUS__STAR_Static")))

; ===== CUSTOMIZED (CLOJURE NUMERICS) ===== ;

(defn argtypes-unknown [a b] (+ a b))

; ===== BENCHMARKS ===== ;

; TODO try to use static methods instead of `invokevirtual`?

; 4.674689 ns
; This is because the compiler makes `+` an intrinsic
(complete-bench (do (+ 1.0 3.0)
                    (+ 1   3  )))

; 5.983952 ns ; 92% variance from outliers; wil be lower
; May initially have slightly worse performance since it isn't inlined by default
(complete-bench (do (+*-static 1.0 3.0)
                    (+*-static 1   3  )))

; 13.650164 ns
(complete-bench (do (Fn/invoke method-double-double +*-static-reified 1.0 3.0)
                    #_(Fn/invoke method +*-static-reified 1   3  )))

(complete-bench (do (Fn/invoke method +*-static-reified 1.0 3.0)
                    #_(Fn/invoke method +*-static-reified 1   3  )))

; 24.095043 ns (5.15x)
(complete-bench (do (argtypes-unknown 1.0 3.0)
                    (argtypes-unknown 1   3  )))

; 33.707466 ns (7.20x)
(complete-bench (do (+*-protocol-0 1.0 3.0)
                    (+*-protocol-0 1   3  )))

; 46.083352 ns
(complete-bench (do (dispatch-with-identity-map-mutable 1.0 3.0)
                    (dispatch-with-identity-map-mutable 1   3  )))

; 57.338497 ns
(complete-bench (do (dispatch-with-map-mutable 1.0 3.0)
                    (dispatch-with-map-mutable 1   3  )))

; 43.563184 ns
(complete-bench (do (dispatch 1.0 3.0)
                    (dispatch 1   3  )))

; 190.962758 ns
(complete-bench (do (dispatch-with-map 1.0 3.0)
                    (dispatch-with-map 1   3  )))

; 188.686935 ns
(complete-bench (.invokeWithArguments method (Array/newObjectArray +*-static-reified 1.0 3.0)))

; Didn't even complete
(complete-bench
  (Fn/invoke (.findVirtual Fn/fnLookup
                           (compile-time-class +*-static-boxed-reified)
                           "_PLUS__STAR_StaticBoxed"
                           (.unwrap (Fn/methodType Object Long Long)))
             +*-static-boxed-reified 1 2))

#_"Resolved:
- Protocols may be 7.2x slower but they're currently the only viable choice for multi-arg dispatch.
- Explore http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2007/n2216.pdf — perhaps a Clojure(Script)
  implementation might yield the same speed benefits — i.e. *no statistical difference in performance
  with direct dispatch*!

"
