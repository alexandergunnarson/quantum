(ns quantum.core.convert.primitive
  (:refer-clojure :exclude
    [boolean byte char short int long float double])
  (:require-quantum [:core logic fn macros log bin err])
  #_(:cljs (:require [com.gfredericks.goog.math.Integer :as int]))
  #?(:clj  (:import java.nio.ByteBuffer [quantum.core Numeric])))

(declare byte    ->byte    ->byte*
         double  ->double  ->double*
         char    ->char    ->char*
         boolean ->boolean
         long    ->long    ->long*
         short   ->short   ->short*
         float   ->float   ->float*
         int     ->int     ->int*
         ->unboxed)
#?(:clj (def long core/long))

#?(:clj
(defmacro long-out-of-range [x]
  `(throw (->ex :illegal-argument (str "Value out of range for long: " ~x)))))

#?(:clj
(defmacro cast-via-long [class- x]
  (let [n (with-meta (gensym "n") {:tag 'long})]
    `(let [~n (long ~x)]
     (if (or (< ~n ~(list '. class- 'MIN_VALUE)) (> ~n ~(list '. class- 'MAX_VALUE)))
         (throw (->ex :illegal-argument (str ~(str "value out of range for " (name class-) ": ") ~x)))
         ~n)))))

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

#?(:clj (defalias boolean ->boolean))
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

#?(:clj (defalias byte ->byte))

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
#?(:clj (defalias ->char core/char))
#?(:clj (defalias char core/char))

#?(:clj
(defnt ^char ->char*
  {:source "clojure.lang.RT.uncheckedCharCast"}
  ([^Character x] (.charValue x))
  ([^Number    x] (->char* (.longValue x)))
  ([#{byte short char int long float double} x] (clojure.lang.RT/uncheckedCharCast x))
  ([^string?   x] (if (->> x count (= 1))
                      (first x)
                      (throw (->ex nil "Cannot cast non-singleton string to char." x))))))
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

#?(:clj (defalias short ->short))
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

; (defnt' ->IntExact
;   (^int [^long x] (Math/toIntExact x)))

#?(:clj
    (defnt ^int ->int
      {:source "clojure.lang.RT.intCast"}
      ([#{Integer}             x] (.intValue x))
      ([#{char byte short int} x] (->int* x))
      ([#{long double}         x] (clojure.lang.RT/intCast x))
      ([^float                 x] (Float/floatToRawIntBits x))
      ([^string?               x] (-> x #?(:clj ->Integer :cljs js/parseInt) ->int))
      ([^string?               x radix] (#?(:clj Integer/parseInt :cljs int/fromString) x radix))
      ([:else                  x  (-> x ->long ->int)]))
   :cljs (defalias ->int core/int))

; js/Math.trunc for CLJS

#?(:clj (defalias int ->int))
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
      (^long [#{Integer Long Byte Short} x] (.longValue x))
      (^long [^clojure.lang.BigInt x]
        (if (nil? (.bipart x))
            (.lpart x)
            (long-out-of-range x)))
      (^long [^java.math.BigInteger x]
        (if (< (.bitLength x) 64)
            (.longValue x)
            (long-out-of-range x)))
      (^long [^clojure.lang.Ratio         x] (->long (.bigIntegerValue x)))
      (^long [^Character                  x] (->long (.charValue       x)))
      (^long [#{Double Float}             x] (->long (.doubleValue     x)))
      (^long [#{char byte short int long} x] (->long* x))
      (^long [#{float}                    x] (clojure.lang.RT/longCast x))  ; Because primitive casting in Clojure is not supported
      (^long [#{double}                   x] (Double/doubleToRawLongBits x))
      (^long [^string?                    x] #?(:clj  (-> x ->Long    ->long)
                                          :cljs (-> x ->Integer ->long)))
    #?(:clj
      (^long [^string?                    x radix] (Long/parseLong x radix))))
   :cljs (defalias ->long core/long))

#?(:clj (defalias long ->long))
;_____________________________________________________________________
;==================={          FLOAT           }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
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

; round to float: (js.Math/fround x)

#?(:clj (defalias ->float core/float))
#?(:clj (defalias float   core/float))
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

#?(:clj (defalias double ->double))

#?(:clj
(defnt' ->boxed
  "These are all intrinsics."
  (^Boolean   [^boolean x] (Boolean/valueOf   x))
  (^Byte      [^byte    x] (Byte/valueOf      x))
  (^Character [^char    x] (Character/valueOf x))
  (^Short     [^short   x] (Short/valueOf     x))
  (^Integer   [^int     x] (Integer/valueOf   x))
  (^Long      [^long    x] (Long/valueOf      x))
  (^Float     [^float   x] (Float/valueOf     x))
  (^Double    [^double  x] (Double/valueOf    x))))

#?(:clj
(defnt' ->unboxed
  "These are all intrinsics."
  (^boolean [^Boolean   x] (.booleanValue x))
  (^byte    [^Byte      x] (.byteValue    x))
  (^char    [^Character x] (.charValue    x))
  (^short   [^Short     x] (.shortValue   x))
  (^int     [^Integer   x] (.intValue     x))
  (^long    [^Long      x] (.longValue    x))
  (^float   [^Float     x] (.floatValue   x))
  (^double  [^Double    x] (.doubleValue  x))))
;_____________________________________________________________________
;==================={         UNSIGNED         }======================
;°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°°
#?(:clj (def ^:const bytes2 (->short 0xFF)))
#?(:clj (def ^:const bytes4 (->int   0xFFFF)))
#?(:clj (def ^:const bytes8 (->long  0xFFFFFFFF)))

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
