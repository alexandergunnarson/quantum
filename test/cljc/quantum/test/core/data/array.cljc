(ns quantum.test.core.data.array
  (:require [quantum.core.data.array :as ns]))

; ----- BOOLEAN ARRAY ----- ;

(defn test:boolean-array [n])

; ----- BYTE ARRAY ----- ;

(defn test:byte-array [n])

#?(:clj
  (defn
    test:byte-array+
    ([size])
    ([size & args])))

; ----- INT ARRAY ----- ;

#?(:clj
  (defn
    test:int-array+
    ([size])
    ([size & args])))

#?(:clj
  (defn test:long-array-of
    ([])
    ([a] )
    ([a b])
    ([a b & more])))

; ----- OBJECT ARRAY ----- ;

#?(:clj
  (defn test:object-array-of
    ([])
    ([a] )
    ([a b])
    ([a b & more])))

#?(:clj
(defmacro test:array [type n]))

; ===== BITMAPS ===== ;

; ===== CONVERSION ===== ;

(defn test:->bytes
  ([x])
  ([x arg]))

(defn test:bytes->longs
  ([x]))

(defn test:copy
  ([input output length]))

#?(:clj
(defn test:array-list [& args]))

(defn test:reverse [x])


(defn test:aconcat
  ([a b])))

(defn test:slice
  ([a start])
  ([a start length]))

(defn test:== [a b])

(defn test:->uint8-array [x])
(defn test:->int8-array [x])