(ns
  ^{:doc "Useful operations on byte arrays. Reverse, split, copy,
          to-hex, to-CString, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.data.bytes
  (:refer-clojure :exclude [reverse assoc!])
           (:require
             #?(:clj [clojure.java.io          :as io])
                     [quantum.core.data.array  :as arr
                       :refer [#?(:clj byte-array+)]]
                     [quantum.core.collections.core
                       :refer [assoc!]]
                     [quantum.core.data.binary :as bin
                       :refer [&& >>>]]
                     [quantum.core.collections.base
                       :refer [nnil?]]
                     [quantum.core.fn          :as fn
                       :refer [fn1]])
  #?(:clj  (:import  java.util.Arrays)))

#?(:clj (set! *unchecked-math* true))

; (defn to-hex-string
;   "Converts a byte array to a string representation , with space as a default separator."
;   ([^"[B" bs]
;     (to-hex-string bs " "))
;   ([^"[B" bs separator]
;     (str/join separator (map #(hex/hex-string-from-byte %) bs))))

#?(:clj
(defn unchecked-byte-array
  "Like clojure.core/byte-array but performs unchecked casts on sequence values."
  {:attribution "mikera.cljutils.bytes"}
  (^"[B" [size-or-seq]
    (. clojure.lang.Numbers byte_array
      (if (number? size-or-seq)
          size-or-seq
          (map unchecked-byte size-or-seq ))))
  (^"[B" [size init-val-or-seq]
    (. clojure.lang.Numbers byte_array size
      (if (sequential? init-val-or-seq)
        (map unchecked-byte init-val-or-seq )
        init-val-or-seq)))))

#?(:clj
(defn ^String bytes-to-hex
  "Convert a byte array to a hex string."
  {:attribution "Alex Gunnarson, ported from a Java solution on StackOverflow."}
  [^"[B" digested]
  (let [^chars hex-arr   (.toCharArray "0123456789abcdef")
        ^chars hex-chars (-> digested count (* 2) char-array)]
    (loop [i 0]
      (if (< i (count digested))
          (let [v           (-> digested (get i) (&& 0xFF))
                bit-shifted (-> hex-arr  (get (>>> v 4   )) char)
                bit-anded   (-> hex-arr  (get (&&   v 0x0F)) char)]
                (assoc! hex-chars (char (* i 2))       bit-shifted)
                (assoc! hex-chars (char (+ (* i 2) 1)) bit-anded)
              (recur (inc i)))))
    (String. hex-chars))))

#?(:clj
(defn ^"[B" str->cstring
  "Convert a Java string to a CString (byte-array)."
  {:attribution "Alex Gunnarson, ported from a Java solution on StackOverflow."}
  [^String s]
  (when (nnil? s)
    (let [^"[B" bytes  (.getBytes s)
          ^"[B" result (byte-array+ (-> bytes count inc))]
        (System/arraycopy
          bytes  0
          result 0
          (count bytes))
        (assoc! result (-> result count dec) (byte 0))
        result))))

#?(:clj
(defn ^bytes parse-bytes
  [encoded-bytes]
  (->> (re-seq #"%.." encoded-bytes)
       (map (fn1 subs 1))
       (map #(.byteValue ^Integer (Integer/parseInt % 16)))
       (byte-array))))
