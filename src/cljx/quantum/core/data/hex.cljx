(ns
  ^{:doc "A hex string library."
    :attribution "Alex Gunnarson"}
  quantum.core.data.hex
  (:require
    [quantum.core.ns :as ns]
    #+clj [clojure.string :as str]
    #+clj [mikera.cljutils.text :as text])
  #+clj (:gen-class))

#+clj (ns/require-all *ns* :clj)
#+clj (set! *unchecked-math* true)

#+clj
(defn hex-string 
  "Converts an an integer value to a hexadecimal string representing the unsigned value.
   The length of the output depends on the value of the integer." 
   {:attribution "mikera.cljutils.hex"}
  ([n]
    (cond
      (instance? Long n) (Long/toHexString (unchecked-long n))
      (instance? Integer n) (java.lang.Integer/toHexString (unchecked-int n))
      (instance? Character n) (.substring 
                                (java.lang.Integer/toHexString 
                                  (unchecked-int (char n)))
                                0 4)
      (instance? Byte n) (java.lang.Integer/toHexString (unchecked-byte n))
      :else (Long/toHexString (unchecked-long n))))
  ([n zero-pad-length]
    (text/pad-left (hex-string n) zero-pad-length "0")))

#+clj
(defn hex-string-from-long 
  "Converts an long value to a hexadecimal string representing the unsigned value of the long." 
  {:attribution "mikera.cljutils.hex"}
  ([^long n]
    (Long/toHexString n))
  ([^long n zero-pad-length]
    (text/pad-left (hex-string-from-long n) zero-pad-length "0")))

#+clj
(defn hex-string-from-byte 
  "Converts an byte value to a hexadecimal string representing the unsigned value of the byte." 
  {:attribution "mikera.cljutils.hex"}
  ([b]
    (let [hs (Long/toHexString (+ 256 (long b)))
          n (.length hs)]
      (.substring hs (int (- n 2))))))

#+clj
(defn bytes-from-hex-string 
  "Converts a string of hex digits into a byte array."
  {:attribution "mikera.cljutils.hex"}
  ([^String s]
    (let [s (str/replace s #"\s+" "")
          ^String s (str/replace s "0x" "")
          cc (.length s)
          n (quot cc 2)
          res (byte-array n)]
      (dotimes [i n]
        (aset res i (byte (+ (bit-and 0xF0 (bit-shift-left (Character/getNumericValue (char (.charAt s (int (* 2 i))))) 4)) 
                             (bit-and 0x0F (long (Character/getNumericValue (.charAt s (int (+ (* 2 i) 1))))))))))
      res)))