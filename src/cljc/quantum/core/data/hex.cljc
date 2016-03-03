(ns
  ^{:doc "A hex string library."
    :attribution "Alex Gunnarson"}
  quantum.core.data.hex
  (:require-quantum [:core str macros log pconvert #_coll]))

#?(:clj
(defnt ^String ->hex-string 
  "Converts an an integer value to a hexadecimal string representing the unsigned value.
   The length of the output depends on the value of the integer." 
   {:attribution "mikera.cljutils.hex"
    :contributors #{"Alex Gunnarson"}}
  ([^long?    x] (Long/toHexString    x))
  ([#{int Integer} x] (Integer/toHexString x))
  ([^char?    x] (.substring 
                   ^String (->hex-string (->int x))
                   0 4))
  ([^byte?    x] (let [^String hs (->hex-string (+ 256 (long x)))
                       n-f  (count hs)]
                   (.substring hs (int (- n-f 2)))))
  ([^bytes? bs]
    (->hex-string bs " "))
  ([^bytes? bs separator]
    (str/join separator (map (fn [x] (->hex-string (->byte* ^Byte x))) bs)))
  #_([:else n zero-pad-length]
    (text/pad-left (->hex-string n) zero-pad-length "0"))))
  
#?(:clj
(defn hex-string->bytes 
  "Converts a string of hex digits into a byte array."
  {:attribution "mikera.cljutils.hex"}
  ([^String s]
    (let [s (str/replace s #"\s+" "")
          ^String s (str/replace s "0x" "")
          cc (.length s)
          n (quot cc 2)
          res (byte-array n)]
      (dotimes [i n]
        (aset res i (byte (+ (bit-and 0xF0 (bit-shift-left (Character/getNumericValue (.charAt s (int (* 2 i)))) 4)) 
                             (bit-and 0x0F           (long (Character/getNumericValue (.charAt s (int (+ (* 2 i) 1))))))))))
      res))))

