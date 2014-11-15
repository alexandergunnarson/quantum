(ns quanta.library.type (:gen-class))
(set! *warn-on-reflection* true)
(require
  '[quanta.library.ns :as ns :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[quanta.library.logic     :refer :all]
  '[quanta.library.function  :refer :all])

; should include typecasting (/cast/)

(def  double?      (partial instance? Double))

(def  ShortArray   (type (short-array   0)))
(def  LongArray    (type (long-array    0)))
(def  FloatArray   (type (float-array   0)))
(def  IntArray     (type (int-array     0)))
(def  DoubleArray  (type (double-array  0.0)))
(def  BooleanArray (type (boolean-array [false])))
(def  ByteArray    (type (byte-array    [])))
(def  CharArray    (type (char-array    "")))
(def  ObjectArray  (type (object-array  [])))

(def  boolean?     (partial instance? Boolean))
(def  indexed?     (partial instance? clojure.lang.Indexed)) 
(def  array?       (compr class (jfn isArray))) ; getClass() shouldn't really be a slow call
(def  byte-array?  (partial instance? ByteArray))

(def  array-list?  (f*n splice-or #(instance? %2 %1) java.util.ArrayList java.util.Arrays$ArrayList))
(def  map-entry?   (partial instance? MapEntry))
(def  sorted-map?  (partial instance? clojure.lang.PersistentTreeMap))
(def  queue?       (partial instance? PersistentQueue))
(def  lseq?        (partial instance? LazySeq))
(def  coll+?       (fn-or coll? array-list?))
(def  pattern?     (partial instance? java.util.regex.Pattern))
(def  editable?    (partial instance? clojure.lang.IEditableCollection))
(def  transient?   (partial instance? clojure.lang.ITransientCollection))

(defn name-from-class [class-0]
  (let [^String class-str (str class-0)]
    (-> class-str
        (subs (-> class-str (.indexOf " ") inc))
        symbol)))


