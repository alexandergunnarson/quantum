(ns quantum.core.convert.primitive
  (:refer-clojure :exclude
    [boolean byte char short int long float double])
  (:require-quantum [ns logic fn macros log bin err])
#?(:clj (:import java.nio.ByteBuffer [quantum.core Numeric])))

#?(:clj
(defmacro long-out-of-range [x]
  `(throw (IllegalArgumentException. (str "Value out of range for long: " ~x)))))

#?(:clj
(defmacro cast-via-long [class- x]
  (let [n (with-meta (gensym "n") {:tag 'long})]
    `(let [~n (long ~x)]
     (if (or (< ~n ~(list '. class- 'MIN_VALUE)) (> ~n ~(list '. class- 'MAX_VALUE)))
         (throw (IllegalArgumentException. (str ~(str "value out of range for " (name class-) ": ") ~x)))
         ~n)))))

(declare ->long)
(declare long)
;_____________________________________________________________________
;==================={          BOOLEAN         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
    (defnt ^boolean ->boolean
      {:source "clojure.lang.RT.booleanCast"}
      ([^Boolean x] (.booleanValue x))
      ([^boolean x] x)
      ([:else    x] (not= x nil)))
   :cljs (defalias ->boolean core/boolean))

(defalias boolean ->boolean)
;_____________________________________________________________________
;==================={           BYTE           }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
    (defnt ^byte ->byte
      {:source "clojure.lang.RT.byteCast"}
      ([^byte                          x] x)
      ([^Byte                          x] (.byteValue x))
      ([#{short int long float double} x] (clojure.lang.RT/byteCast x))
      ([:else                          x] (cast-via-long Byte x)))
   :cljs (defalias ->byte core/byte))

(defalias byte ->byte)

; Doesn't autocast
#?(:clj
(defnt ^byte ->byte*
  {:source "clojure.lang.RT.uncheckedByteCast"}
  ([^Number x] (.byteValue x))
  ([#{byte short int long float double} x] (clojure.lang.RT/uncheckedByteCast x))))
;_____________________________________________________________________
;==================={           CHAR           }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
; TODO reflection issues
; (defnt ^char ->char
;   {:source "clojure.lang.RT.charCast"}
;   ([^char                    x] x)
;   ([^Character               x] (.charValue x))
;   ([#{byte short int long float double} x] (clojure.lang.RT/shortCast x))
;   ([:else x] (cast-via-long Character x)))
(defalias ->char core/char)
(defalias char core/char)

#?(:clj
(defnt ^char ->char*
  {:source "clojure.lang.RT.uncheckedCharCast"}
  ([^Character x] (.charValue x))
  ([^Number    x] (->char* (.longValue x)))
  ([#{byte short char int long float double} x] (clojure.lang.RT/uncheckedCharCast x))
  ([^string?   x] (if (->> x count (= 1))
                      (first x)
                      (throw+ (Err. nil "Cannot cast non-singleton string to char." x))))))
;_____________________________________________________________________
;==================={           SHORT          }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
(defnt ->Short
  ([^string? s] (try (Short. s) (catch NumberFormatException _)))))

#?(:clj
(defnt ^short ->short*
  {:source "clojure.lang.RT.uncheckedShortCast"}
  ([^Number x] (.shortValue x))
  ([#{byte short int long float double} x] (clojure.lang.RT/uncheckedShortCast x))))

#?(:clj
    (defnt ^short ->short
      {:source "clojure.lang.RT.shortCast"}
      ([^Short                   x] (.shortValue x))
      ([#{byte short}            x] (->short* x))
      ([#{int long float double} x] (clojure.lang.RT/shortCast x))
      ([^string?                 x] (-> x ->Short ->short))
      ([:else x] (cast-via-long Short x)))
   :cljs (defalias ->short core/short))

(defalias short ->short)
;_____________________________________________________________________
;==================={            INT           }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
(defnt ->Integer
  ; ~10% faster than (Integer. x)
  ([^string? x] (#?(:clj Integer/parseInt :cljs js/parseInt) x))))

#?(:clj
(defnt ^int ->int*
  {:source "clojure.lang.RT.uncheckedIntCast"}
  ([^Number    x] (.intValue x))
  ([^Character x] (->int* (->char (.charValue x))))
  ([#{byte short char int long float double} x] (clojure.lang.RT/uncheckedIntCast x))))

#?(:clj
    (defnt ^int ->int
      {:source "clojure.lang.RT.intCast"}
      ([#{Integer}             x] (.intValue x))
      ([#{char byte short int} x] (->int* x))
      ([#{long double}         x] (clojure.lang.RT/intCast x))
      ([^float                 x] (Float/floatToRawIntBits x))
      ([^string?               x] (-> x #?(:clj ->Integer :cljs js/parseInt) ->int))
      ([^string?               x radix] (Integer/parseInt x radix))
      ([:else                  x  (-> x ->long ->int)]))
   :cljs (defalias ->int core/int))

(defalias int ->int)
;_____________________________________________________________________
;==================={           LONG           }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj
(defnt ->Long
  ([^string? x] (Long/parseLong x))))

#?(:clj
(defnt ^long ->long*
  {:source "clojure.lang.RT.uncheckedLongCast"}
  ([^Number    x] (.longValue x))
  ([#{char} x] (Numeric/uncheckedLongCast x))
  ([#{byte short int long float double} x] (clojure.lang.RT/uncheckedLongCast x))))

#?(:clj
    (defnt ^long ->long
      {:source "clojure.lang.RT.longCast"}
      ([#{Integer Long Byte Short} x] (.longValue x))
      ([^clojure.lang.BigInt x]
        (if (nil? (.bipart x))
            (.lpart x)
            (long-out-of-range x)))
      ([^java.math.BigInteger x]
        (if (< (.bitLength x) 64)
            (.longValue x)
            (long-out-of-range x)))
      ([^clojure.lang.Ratio         x] (->long (.bigIntegerValue x)))
      ([^Character                  x] (->long (.charValue       x)))
      ([#{Double Float}             x] (->long (.doubleValue     x)))
      ([#{char byte short int long} x] (->long* x))
      ([#{float}                    x] (clojure.lang.RT/longCast x))  ; Because primitive casting in Clojure is not supported
      ([#{double}                   x] (Double/doubleToRawLongBits x))
      ([^string?                    x] #?(:clj  (-> x ->Long    ->long)
                                          :cljs (-> x ->Integer ->long)))
    #?(:clj
      ([^string?                    x radix] (Long/parseLong x radix))))
   :cljs (defalias ->long core/long))

(defalias long ->long)
;_____________________________________________________________________
;==================={          FLOAT           }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(declare ->double)

#?(:clj
(defnt ->Float
  ([^string? s] (try (Float. s) (catch NumberFormatException _)))))

#?(:clj
(defnt ^float ->float*
  {:source "clojure.lang.RT/uncheckedFloatCast"}
  ([^Number                             x] (.floatValue x))
  ([#{byte short int long float double} x] (clojure.lang.RT/uncheckedFloatCast x))))

; ; TODO duplicate methods
; (defnt ^float ->float
;   {:source "clojure.lang.RT/floatCast"}
;   ([^Float                   x] (.floatValue x))
;   ([#{byte short float long} x] (->float* x))
;   ([^int                     x] (Float/intBitsToFloat x)
;   ([^string?                 x] (-> x ->Float ->float))
;   ([:else]
;     (let [n (->double x)]
;       (if (or (< n (- Float/MAX_VALUE))
;               (> n Float/MAX_VALUE))
;           (throw (IllegalArgumentException. (str "Value out of range for float: " x)))
;           n))))

(defalias ->float core/float)
(defalias float   core/float)
;_____________________________________________________________________
;==================={          DOUBLE          }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°

#?(:clj
(defnt ->Double
  ([^string? s] (try (Double. s) (catch NumberFormatException _)))))

#?(:clj
(defnt ^double ->double*
  {:source "clojure.lang.RT/uncheckedDoubleCast"}
  ([^Number                      x] (.doubleValue x))
  ([^double                      x] x)
  ([#{byte short int long float} x] (clojure.lang.RT/uncheckedDoubleCast x))))

#?(:clj
    (defnt ^double ->double
      {:source "clojure.lang.RT/doubleCast"
       :todo "Check for overflow"}
      ([^Number                 x] (.doubleValue x))
      ([^double                 x] x)
      ([#{byte short int float} x] (->double* x))
      ([^long                   x] (->double* x)) ; Double/longBitsToDouble is bad
      ([^string?                x] (-> x ->Double ->double)))
   :cljs (defalias ->double core/double))

(defalias double ->double)
;_____________________________________________________________________
;==================={         UNSIGNED         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
(def ^:const bytes2 (->short 0xFF))
(def ^:const bytes4 (->int   0xFFFF))
(def ^:const bytes8 (->long  0xFFFFFFFF))

; (quantum.core.Numeric/bitAnd (->short' bytes2) (->byte 1))
#?(:clj
(defnt' ->unsigned
  {:attribution  ["ztellman/primitive-math" "gloss.data.primitives"]
   :contributors {"Alex Gunnarson" "defnt-ed"}
   :todo ["change to unchecked-bit-and after making sure it won't overflow"]}
  (^short [^byte  x] (bit-and (->short*' bytes2) x))
  (^int   [^short x] (bit-and (->int*'   bytes4) x))
  (^long  [^int   x] (bit-and (->long*'  bytes8) x))
  (       [^long  x]
    (BigInteger. 1 (-> (ByteBuffer/allocate 8) (.putLong x) .array)))))

#?(:clj
(defn ubyte->byte
  {:inline (fn [x] `(byte (long ~x)))}
  ^long [^long x]
  (long (byte x))))

#?(:clj
(defn ushort->short
  {:inline (fn [x] `(short (long ~x)))}
  ^long [^long x]
  (long (short x))))

#?(:clj
(defn uint->int
  {:inline (fn [x] `(int (long ~x)))}
  ^long [^long x]
  (long (int x))))

#?(:clj
(defn ulong->long 
  ^long [x]
  (.longValue ^clojure.lang.BigInt (bigint x))))
