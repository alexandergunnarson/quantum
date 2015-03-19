(ns
  ^{:doc "Useful operations on byte arrays. Reverse, split, copy,
          to-hex, to-CString, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.data.bytes
  (:refer-clojure :exclude [reverse])
  (:require 
    [quantum.core.string           :as str]
    [quantum.core.data.binary      :as bin :refer :all]
    [quantum.core.logic :as log :refer
      #+clj  [splice-or fn-and fn-or fn-not ifn if*n whenc whenf whenf*n whencf*n condf condf*n nnil?]
      #+cljs [splice-or fn-and fn-or fn-not nnil?]
      #+cljs :refer-macros
      #+cljs [ifn if*n whenc whenf whenf*n whencf*n condf condf*n]]
    [quantum.core.data.array       :as arr :refer [byte-array+ aset!]]
    #+clj [quantum.core.collections.core         :refer :all])
  (:import java.util.Arrays)
  #+clj (:gen-class))

#+clj (set! *warn-on-reflection* true)
#+clj (set! *unchecked-math* true)

#+clj
(def BYTE-ARRAY-CLASS (Class/forName "[B"))

#+clj ; because apparently reversed byte-array...
(defn reverse 
  {:attribution "mikera.cljutils.bytes"}
  (^bytes [^bytes bs]
    (let [n (alength bs)
          res (byte-array n)]
      (dotimes [i n]
        (aset res i (aget bs (- n (inc i)))))
      res)))

#+clj
(defn join 
  "Concatenates two byte arrays"
  {:attribution "mikera.cljutils.bytes"}
  (^bytes [^bytes a ^bytes b]
    (let [al (int (alength a))
          bl (int (alength b))
          n (int (+ al bl))
          ^bytes res (byte-array n)]
      (System/arraycopy a (int 0) res (int 0) al)
      (System/arraycopy b (int 0) res (int al) bl)
      res)))

#+clj
(defn slice
  "Slices a byte array with a given start and length"
  {:attribution "mikera.cljutils.bytes"}
  (^bytes [a start]
    (slice a start (- (alength ^bytes a) start)))
  (^bytes [a start length]
    (let [al (int (alength ^bytes a))
          ^bytes res (byte-array length)]
      (System/arraycopy a (int start) res (int 0) length)
      res)))

; (defn to-hex-string 
;   "Converts a byte array to a string representation , with space as a default separator."
;   ([^bytes bs]
;     (to-hex-string bs " "))
;   ([^bytes bs separator]
;     (str/join separator (map #(hex/hex-string-from-byte %) bs))))

#+clj
(defn unchecked-byte-array 
  "Like clojure.core/byte-array but performs unchecked casts on sequence values."
  {:attribution "mikera.cljutils.bytes"}
  (^bytes [size-or-seq] 
    (. clojure.lang.Numbers byte_array 
      (if (number? size-or-seq) 
        size-or-seq
        (map unchecked-byte size-or-seq ))))
  (^bytes [size init-val-or-seq] 
    (. clojure.lang.Numbers byte_array size 
      (if (sequential? init-val-or-seq) 
        (map unchecked-byte init-val-or-seq )
        init-val-or-seq))))

#+clj
(defn bytes=
  "Compares two byte arrays for equality."
  {:attribution "mikera.cljutils.bytes"}
  ([^bytes a ^bytes b]
    (Arrays/equals a b)))

#+clj
(defn ^String bytes-to-hex
  "Convert a byte array to a hex string."
  {:attribution "Alex Gunnarson, ported from a Java solution on StackOverflow."}
  [^bytes digested]
  (let [^chars hex-arr   (.toCharArray "0123456789abcdef")
        ^chars hex-chars (-> digested count (* 2) char-array)]
    (loop [i 0] 
      (if (< i (count digested))
          (let [v           (-> digested (get+ i) (bit-and 0xFF))
                bit-shifted (-> hex-arr  (get+ (>>>     v 4   )))
                bit-anded   (-> hex-arr  (get+ (bit-and v 0x0F)))]
                (aset hex-chars (* i 2)       bit-shifted)
                (aset hex-chars (+ (* i 2) 1) bit-anded)
              (recur (inc i)))))
    (String. hex-chars)))

#+clj
(defn ^bytes str->cstring
  "Convert a Java string to a CString (byte-array)."
  {:attribution "Alex Gunnarson, ported from a Java solution on StackOverflow."}
  [^String s]
  (when (nnil? s)
    (let [^bytes bytes  (.getBytes s)
          ^bytes result (byte-array+ (-> bytes count inc))]
        (System/arraycopy
          bytes  0
          result 0
          (count bytes))
        (aset! result (-> result count dec) (byte 0))
        result)))

