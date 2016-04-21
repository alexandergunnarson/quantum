(ns
  ^{:doc "A hex string library."
    :attribution "Alex Gunnarson"}
  quantum.core.data.hex
  (:require-quantum [:core str macros log pconvert #_coll]))

(defnt ^String ->hex-string 
  "Converts an an integer value to a hexadecimal string representing the unsigned value.
   The length of the output depends on the value of the integer." 
  #?(:clj  ([^long?    x] (Long/toHexString x)))
  #?(:cljs ([^number?  x] (.toString x 16)))
  #?(:clj  ([#{int Integer} x] (Integer/toHexString x)))
  #?(:clj  ([^char?    x]
             (.substring ^String (->hex-string (->int x)) 0 4)))
  #?(:clj  ([^byte?    x]
             (let [^String hs (->hex-string (+ 256 (long x)))
                   n-f  (count hs)]
               (.substring hs (int (- n-f 2))))))
  #?(:cljs ([#{js/Uint8Array js/Int8Array array?} x]
             (js/goog.crypt.byteArrayToHex x)))
  #?(:clj  ([^bytes? bs]
             (->hex-string bs " ")))
  #?(:clj  ([^bytes? bs separator]
             (str/join separator (map (fn [x] (->hex-string (->byte* ^Byte x))) bs))))
  #_([:else n zero-pad-length]
    (text/pad-left (->hex-string n) zero-pad-length "0")))
  
(defnt hex-string->bytes 
  "Converts a string of hex digits into a byte array."
  {:attribution "mikera.cljutils.hex"}
  ([^string? x]
    #?(:clj (let [s (str/replace x #"\s+" "")
                  ^String s (str/replace s "0x" "")
                  cc (.length x)
                  n (quot cc 2)
                  res (byte-array n)]
              (dotimes [i n]
                (aset res i (byte (+ (bit-and 0xF0 (bit-shift-left (Character/getNumericValue (.charAt x (int (* 2 i)))) 4)) 
                                     (bit-and 0x0F           (long (Character/getNumericValue (.charAt x (int (+ (* 2 i) 1))))))))))
              res)
       :cljs (-> x  js/goog.crypt.hexToByteArray js/Uint8Array.))))

