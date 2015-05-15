#?(:clj (ns quantum.core.data.array))

(ns
  ^{:doc "Useful array functions. Array creation, joining, reversal, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.data.array
  #?@(:clj
      [(:import java.util.ArrayList)
       (:gen-class)]))

#?(:clj
  (require
    '[quantum.core.ns          :as ns :refer [defalias]]
    '[quantum.core.string      :as str                 ]
    '[quantum.core.data.hex    :as hex                 ]
    '[quantum.core.loops       :refer [doseqi]  ]))

#?(:clj (ns/require-all *ns* :clj))

(#?(:clj defalias :cljs def) aset! aset)

#?(:clj
  (defn typed-array
    "Creates a typed Java array of a collection of objects. Uses the class
     of the first object to determine the type of the array."
    {:attribution "mikera.cljutils.arrays"}
    ([objects]
      (let [cnt (count objects)
            cl (.getClass ^Object (first objects))
            ^objects arr (make-array cl cnt)]
        (doseqi [o objects i]
          (aset arr (int i) o))
        arr))))

; TODO: Use a macro for this
#?(:clj
  (defn long-array-of 
    "Creates a long array with the specified values."
    {:attribution "mikera.cljutils.arrays"}
    (^longs [] (long-array 0))
    (^longs [a] 
      (let [arr (long-array 1)]
        (aset! arr 0 (long a))
        arr))
    ([a b] 
      (let [arr (long-array 2)]
        (aset! arr 0 (long a))
        (aset! arr 1 (long b))
        arr))
    ([a b & more] 
      (let [arr (long-array (+ 2 (count more)))]
        (aset! arr 0 (long a))
        (aset! arr 1 (long b))
        (doseqi [x more i] (aset! arr (+ 2 i) (long x))) 
        arr))))

; TODO: Use a macro for this
#?(:clj
  (defn object-array-of 
    "Creates a long array with the specified values."
    {:attribution "mikera.cljutils.arrays"}
    ([] (object-array 0))
    ([a] 
      (let [arr (object-array 1)]
        (aset! arr 0 a)
        arr))
    ([a b] 
      (let [arr (object-array 2)]
        (aset! arr 0 a)
        (aset! arr 1 b)
        arr))
    ([a b & more] 
      (let [arr (object-array (+ 2 (count more)))]
        (aset! arr 0 a)
        (aset! arr 1 b)
        (doseqi [x more i] (aset! arr (+ 2 i) x)) 
        arr))))

#?(:clj
  (defn array-list [& args]
    (reduce (fn [ret elem]
    	      (.add ^ArrayList ret elem)
            ret)
            (ArrayList.) args)))
  
; NEED TO MAKE THESE LESS REPETITIVE  -  MACRO-BASED
#?(:clj
  (defn ^"[B"
    byte-array+
    "Like /byte-array/ but allows for array initializers a la Java:
     byte[] arr = byte[]{12, 8, 10}"
    {:attribution "Alex Gunnarson"}
    ([size]
      (byte-array (long size)))
    ([size & args]
      (let [^"[B" arr (byte-array (long size))]
        (doseqi [arg args n]
          (aset! arr n (-> arg first byte)))
        arr))))

#?(:clj
  (defn ^ints int-array+
    "Like /int-array/ but allows for array initializers a la Java:
     int[] arr = int[]{12, 8, 10}"
    {:attribution "Alex Gunnarson"}
    ([size]
      (int-array (long size)))
    ([size & args]
      (let [^ints arr (int-array (long size))]
        (doseqi [arg args n]
          (aset! arr (long n) (-> arg first int)))
        arr))))

; (definline objects
;   "Casts to object[]"
;   {:contributor "Alex Gunnarson"}
;   [xs] `(. clojure.lang.Numbers objects ~xs))

#?(:clj
  (defn to-hex-string 
    "Converts a byte array to a string representation , with space as a default separator."
    {:attribution "mikera.cljutils.bytes"}
    ([^"[B" bs]
      (to-hex-string bs " "))
    ([^"[B" bs separator]
      (str/join separator (map #(hex/hex-string-from-byte %) bs)))))

#?(:clj
  (defn areverse 
    {:attribution "mikera.cljutils.bytes"}
    (^"[B" [^"[B" bs]
      (let [n (alength bs)
            res (byte-array n)]
        (dotimes [i n]
          (aset! res i (aget bs (- n (inc i)))))
        res))))

#?(:clj
  (defn ajoin 
    "Concatenates two byte arrays"
    {:attribution "mikera.cljutils.bytes"}
    (^"[B" [^"[B" a ^"[B" b]
      (let [al (int (alength a))
            bl (int (alength b))
            n (int (+ al bl))
            ^"[B" res (byte-array n)]
        (System/arraycopy a (int 0) res (int 0) al)
        (System/arraycopy b (int 0) res (int al) bl)
        res))))  