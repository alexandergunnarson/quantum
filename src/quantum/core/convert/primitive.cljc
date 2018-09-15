(ns quantum.core.convert.primitive
         (:require
  #?(:cljs [com.gfredericks.goog.math.Integer :as int])
           [clojure.core                :as core]
           [quantum.core.data.bits      :as bits
             :refer [&&]]
           [quantum.core.data.primitive :as p]
           [quantum.core.type           :as t
             :refer [defnt]]
           [quantum.core.vars           :as var
             :refer [defalias]])
#?(:cljs (:require-macros
           [quantum.core.convert.primitive]))
#?(:clj  (:import
           [java.nio ByteBuffer]
           [quantum.core Numeric Primitive])))

; TODO go back over these — there are inconsistencies

;; ===== Long ===== ;;

#?(:clj
(defmacro long-out-of-range [x]
  `(throw (ex-info (str "Value out of range for long: " ~x) {:type :illegal-argument}))))

#?(:clj
(defnt >long*
  {:source "clojure.lang.RT.uncheckedLongCast"}
  > p/long?
  ([x (t/- p/primitive? p/boolean?)] (Primitive/uncheckedLongCast x))
  ([x (t/ref (t/isa? Number))] (.longValue x)))))

#?(:clj
     (defnt >long
       {:source "clojure.lang.RT.longCast"}
       > p/long?
       ([x (t/isa? clojure.lang.BigInt)]
         (if (nil? (.bipart x))
             (.lpart x)
             (long-out-of-range x)))
       ([x (t/isa? java.math.BigInteger)]
         (if (< (.bitLength x) 64)
             (.longValue x)
             (long-out-of-range x)))
       ([x dnum/ratio?] (->long (.bigIntegerValue x)))
       ([x (t/or p/char? p/byte? p/short? p/int? p/long?)] (>long* x))
       ([x p/float?] (clojure.lang.RT/longCast x)) ; Because primitive casting in Clojure is not supported ; TODO fix
       ([x p/double?] (clojure.lang.RT/longCast x)) ; TODO fix
       ([x p/boolean?] (if x 1 0))
       ([x t/string?] (-> x Long/parseLong >long))
       ([x t/string?, radix p/int?] (Long/parseLong x radix)))
   :cljs
     (defnt >long > (t/range-of p/long?)
       ([x p/double?]  (js/Math.trunc x))
       ([x t/string?]  (-> x int/fromString >long))
       ([x p/boolean?] (if x 1 0))))

#?(:clj
(defmacro cast-via-long [class- x]
  `(let [n# (->long ~x)]
     (if (or (< n# ~(list '. class- 'MIN_VALUE)) (> n# ~(list '. class- 'MAX_VALUE)))
         (throw (ex-info (str ~(str "value out of range for " (name class-) ": ") ~x)
                         {:type :illegal-argument}))
         n#))))

;; ===== Boolean ===== ;;

#?(:clj
    (defnt ^boolean ->boolean
      {:source "clojure.lang.RT.booleanCast"}
      ([^boolean x] x)
      ([#{byte char short int long float double Object} x] (.booleanValue (not= x nil)))) ; TODO #{(- prim? boolean) Object}
   :cljs (defalias ->boolean core/boolean))

;; ===== Byte ===== ;;

#?(:clj
    (defnt ^byte ->byte
      {:source "clojure.lang.RT.byteCast"}
      ([^byte                          x] x)
      ([#{short int long float double} x] (clojure.lang.RT/byteCast x))
      ([#{boolean}                     x] (-> x ->long ->byte))
      ; TODO do other numbers
      ([                               x] (clojure.lang.RT/byteCast x)))
   :cljs (defalias ->byte core/byte))

#?(:clj
(defnt ^byte ->byte*
  {:source "clojure.lang.RT.uncheckedByteCast"}
  ([^Number x] (.byteValue x))
  ([#{byte short int long float double} x] (Primitive/uncheckedByteCast x))))

;; ===== Char ===== ;;

; TODO reflection issues
; (defnt ^char ->char
;   {:source "clojure.lang.RT.charCast"}
;   ([^char                    x] x)
;   ([^Character               x] (.charValue x))
;   ([#{byte short int long float double} x] (clojure.lang.RT/shortCast x))
;   ([:else x] (cast-via-long Character x)))
#?(:clj (defalias ->char core/char))

#?(:clj
(defnt ^char ->char*
  {:source "clojure.lang.RT.uncheckedCharCast"}
  ([^Number    x] (->char* (.longValue x)))
  ([#{byte short char int long float double} x] (Primitive/uncheckedCharCast x))
  ([^string?   x] (if (->> x .length (= 1))
                      (.charAt x 0)
                      (throw (ex-info "Cannot cast non-singleton string to char." {:string x}))))))

;; ===== Short ===== ;;

#?(:clj
(defnt ^short ->short*
  {:source "clojure.lang.RT.uncheckedShortCast"}
  ([^Number x] (.shortValue x))
  ([#{byte short int long float double} x] (Primitive/uncheckedShortCast x))))

#?(:clj
    (defnt ^short ->short
      {:source "clojure.lang.RT.shortCast"}
      ([#{byte short}            x] (->short* x))
      ([#{int long float double} x] (clojure.lang.RT/shortCast x))
      ([^string?                 x] (-> x Short/parseShort ->short))
      ([#{boolean}               x] (-> x ->long ->short)))
   :cljs (defalias ->short core/short))

;; ===== Int ===== ;;

#?(:clj
(defnt ^int ->int*
  {:source "clojure.lang.RT.uncheckedIntCast"}
  ([^Number    x] (.intValue x))
  ([#{byte short char int long float double} x] (Primitive/uncheckedIntCast x))))

; (defnt' ->IntExact
;   (^int [^long x] (Math/toIntExact x)))

#?(:clj
    (defnt ^int ->int
      {:source "clojure.lang.RT.intCast"}
      ([#{char byte short int} x] (->int* x))
      ([#{long double}         x] (clojure.lang.RT/intCast x))
      ([^float                 x] (Float/floatToRawIntBits x))
      ([^string?               x] (-> x #?(:clj Integer/parseInt :cljs js/parseInt) ->int))
      ([^string?               x radix] (#?(:clj Integer/parseInt :cljs int/fromString) x radix)))
   :cljs (defalias ->int core/int))

; js/Math.trunc for CLJS

;; ===== Float ===== ;;

#?(:clj
(defnt ^float ->float*
  {:source "clojure.lang.RT/uncheckedFloatCast"}
  ([^Number                             x] (.floatValue x))
  ([#{byte short int long float double} x] (Primitive/uncheckedFloatCast x))
  ([^string?                            x] (Float/parseFloat x))))

#?(:clj
(defnt ^float ->float
  {:source "clojure.lang.RT/floatCast"}
  ([#{byte short int float long} x] (->float* x))
  ([^string?                 x] (Float/parseFloat #_->float* x)))) ; TODO fix this

; round to float: (js.Math/fround x)

#?(:clj (defalias ->float core/float))

;; ===== Double ===== ;;

#?(:clj
(defnt ^double ->double*
  {:source "clojure.lang.RT/uncheckedDoubleCast"}
  ([^Number                      x] (.doubleValue x))
  ([^double                      x] x)
  ([#{byte short int long float} x] (Primitive/uncheckedDoubleCast x))
  ([                             x] (clojure.lang.RT/uncheckedDoubleCast x))))

#?(:clj
    (defnt ^double ->double
      {:source "clojure.lang.RT/doubleCast"
       :todo #{"Check for overflow}"}}
      ([^Number                 x] (.doubleValue x))
      ([^double                 x] x)
      ([#{byte short int float} x] (->double* x))
      ([^long                   x] (->double* x)) ; Double/longBitsToDouble is bad
      ([^string?                x] (-> x Double/parseDouble ->double)))
   :cljs (defalias ->double core/double))

#?(:clj
(defnt' ->boxed
  (^Boolean   ^:intrinsic [^boolean x] (Boolean/valueOf   x))
  (^Byte      ^:intrinsic [^byte    x] (Byte/valueOf      x))
  (^Character ^:intrinsic [^char    x] (Character/valueOf x))
  (^Short     ^:intrinsic [^short   x] (Short/valueOf     x))
  (^Integer   ^:intrinsic [^int     x] (Integer/valueOf   x))
  (^Long      ^:intrinsic [^long    x] (Long/valueOf      x))
  (^Float     ^:intrinsic [^float   x] (Float/valueOf     x))
  (^Double    ^:intrinsic [^double  x] (Double/valueOf    x))))

#?(:clj
(defnt' ->unboxed
  (^boolean ^:intrinsic [^Boolean   x] (.booleanValue x))
  (^byte    ^:intrinsic [^Byte      x] (.byteValue    x))
  (^char    ^:intrinsic [^Character x] (.charValue    x))
  (^short   ^:intrinsic [^Short     x] (.shortValue   x))
  (^int     ^:intrinsic [^Integer   x] (.intValue     x))
  (^long    ^:intrinsic [^Long      x] (.longValue    x))
  (^float   ^:intrinsic [^Float     x] (.floatValue   x))
  (^double  ^:intrinsic [^Double    x] (.doubleValue  x))))

;; ===== Unsigned ===== ;;

#?(:clj (def ^:const bytes2 (->short 0xFF)))
#?(:clj (def ^:const bytes4 (->int   0xFFFF)))
#?(:clj (def ^:const bytes8 (->long  0xFFFFFFFF)))

; (quantum.core.Numeric/bitAnd (->short' bytes2) (->byte 1))
#?(:clj
(defnt' ->unsigned
  {:attribution  ["ztellman/primitive-math" "gloss.data.primitives"]
   :contributors {"Alex Gunnarson" "defnt-ed"}
   :todo #{"change to unchecked-bit-and after making sure it won't overflow"}}
  ([^byte  x] (&& (->short* bytes2) x))
  ([^short x] (&& (->int*   bytes4) x))
  ([^int   x] (&& (->long*  bytes8) x))
  ([^long  x]
    (BigInteger. 1 (-> (ByteBuffer/allocate 8) (.putLong x) .array))))) ; TODO reflection

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
