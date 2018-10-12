(ns quantum.core.primitive
  "Not merged into `quantum.core.data.primitive` because this namespace requires numeric ranges.")

;; ===== Conversion ===== ;;

;; ----- Boolean ----- ;;

;; TODO CLJS
;; TODO rethink — is everything that's a 0 false and everything that's a 1 a true? Or is it just
;; 0's that are false? Etc.
(t/defn ^:inline >boolean
  "Converts input to a boolean.
   Differs from asking whether something is truthy/falsey."
  > boolean?
         ([x boolean?] x)          ;; For purposes of Clojure intrinsics
#?(:clj  ([x (t/or long? double?)] (-> x clojure.lang.Numbers/isZero Numeric/not)))
#?(:clj  ([x (t/- primitive? boolean? long? double?)] (-> x Numeric/isZero Numeric/not))))

;; ----- Byte ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >byte*
  "May involve non-out-of-range truncation."
  > byte?
  ([x byte?] x)
  ([x (t/- primitive? byte? boolean?)] (Primitive/uncheckedByteCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(defnt ^:inline >byte > #?(:clj byte? :cljs numerically-byte?)
  "Does not involve truncation or rounding."
         ([x #?(:clj byte? :cljs numerically-byte?)] x)
#?(:clj  ([x (t/and (t/- primitive? byte? boolean?) numerically-byte?)] (>byte* x))
   :cljs ([x (t/and double? numerically-byte?)] x))
         ([x boolean?] (if x #?(:clj (byte 1) :cljs 1) #?(:clj (byte 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-byte?)] (>byte* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-byte?)] (.byteValue x)))
#?(:clj  ([x (t/and dn/ratio? numerically-byte?)] (-> x .bigIntegerValue .byteValue))))

;; ----- Short ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >short*
  "May involve non-out-of-range truncation."
  > short?
  ([x short?] x)
  ([x (t/- primitive? short? boolean?)] (Primitive/uncheckedShortCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >short > #?(:clj short? :cljs numerically-short?)
  "Does not involve truncation or rounding."
         ([x #?(:clj short? :cljs numerically-short?)] x)
#?(:clj  ([x (t/and (t/- primitive? short? boolean?) numerically-short?)] (>short* x))
   :cljs ([x (t/and double? numerically-short?)] x))
         ([x boolean?] (if x #?(:clj (short 1) :cljs 1) #?(:clj (short 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-short?)] (>short* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-short?)] (.shortValue x)))
#?(:clj  ([x (t/and dn/ratio? numerically-short?)] (-> x .bigIntegerValue .shortValue))))

;; ----- Char ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >char*
  "May involve non-out-of-range truncation."
  > char?
  ([x char?] x)
  ([x (t/- primitive? char? boolean?)] (Primitive/uncheckedCharCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >char > #?(:clj char? :cljs numerically-char?)
  "Does not involve truncation or rounding.
   For CLJS, returns not a String of length 1 but a numerically-char Number."
         ([x #?(:clj char? :cljs numerically-char?)] x)
#?(:clj  ([x (t/and (t/- primitive? char? boolean?) numerically-char?)] (>char* x))
   :cljs ([x (t/and double? numerically-char?)] x))
         ([x boolean?] (if x #?(:clj (char 1) :cljs 1) #?(:clj (char 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-char?)] (>char* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-char?)] (.charValue x)))
#?(:clj  ([x (t/and dn/ratio? numerically-char?)] (-> x .bigIntegerValue .charValue))))

;; ----- Int ----- ;;

;; TODO figure out how to use with goog.math.Integer/Long
#?(:clj
(t/defn ^:inline >int*
  "May involve non-out-of-range truncation."
  > int?
  ([x int?] x)                        ;; For purposes of Clojure intrinsics
  ([x (t/- primitive? int? boolean?)] (clojure.lang.RT/uncheckedIntCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >int
  "Does not involve truncation or rounding."
  > int?
         ([x int?] x)
#?(:clj  ([x (t/and (t/- primitive? int? boolean?) numerically-int?)] (>int* x))
   :cljs ([x (t/and double? numerically-int?)] x))
         ([x boolean?] (if x #?(:clj (int 1) :cljs 1) #?(:clj (int 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-int?)] (>int* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-int?)] (.intValue x)))
#?(:clj  ([x (t/and dn/ratio? numerically-int?)] (-> x .bigIntegerValue .intValue))))

;; ----- Long ----- ;;

;; TODO figure out how to use with CLJS, including goog.math.Integer/Long
#?(:clj
(t/defn ^:inline >long*
  "May involve non-out-of-range truncation."
  > long?
  ([x long?] x)
  ([x char?] (Primitive/uncheckedLongCast x))  ;; For purposes of Clojure intrinsics
  ([x (t/- primitive? long? boolean? char?)] (clojure.lang.RT/uncheckedLongCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >long
  "Does not involve truncation or rounding."
  > #?(:clj long? :cljs numerically-long?)
         ([x #?(:clj long? :cljs numerically-long?)] x)
#?(:clj  ([x (t/and (t/- primitive? long? boolean?) numerically-long?)] (>long* x))
   :cljs ([x (t/and double? numerically-long?)] x))
         ([x boolean?] (if x 1 0))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt)
                    numerically-long?
                    ;; TODO This might be faster than `numerically-long?`
                  #_(fnt [x ?] (nil? (.bipart x))))] (.lpart x)))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger)
                    numerically-long?
                    ;; TODO This might be faster than `numerically-long?`
                  #_(fnt [x ?] (< (.bitLength x) 64)))] (.longValue x)))
#?(:clj  ([x (t/and dn/ratio? numerically-long?)] (-> x .bigIntegerValue .longValue))))

;; ----- Float ----- ;;

;; TODO figure out how to use with CLJS
#?(:clj
(t/defn ^:inline >float*
  "May involve non-out-of-range truncation."
  > float?
  ([x float?] x)
  ([x (t/- primitive? float? boolean?)] (Primitive/uncheckedFloatCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >float > #?(:clj float? :cljs numerically-float?)
  "Does not involve truncation or rounding."
         ([x #?(:clj float? :cljs numerically-float?)] x)
#?(:clj  ([x (t/and (t/- primitive? float? boolean?) numerically-float?)] (>float* x))
   :cljs ([x (t/and double? numerically-float?)] x))
         ([x boolean?] (if x #?(:clj (float 1) :cljs 1) #?(:clj (float 0) :cljs 0)))
#?(:clj  ([x (t/and (t/isa? clojure.lang.BigInt) numerically-float?)] (>float* (.lpart x))))
#?(:clj  ([x (t/and (t/isa? java.math.BigInteger) numerically-float?)] (.floatValue x)))
#?(:clj  ([x (t/and dn/ratio? numerically-float?)] (-> x .bigIntegerValue .floatValue))))

;; ----- Double ----- ;;

;; TODO figure out how to use with goog.math.Integer/Long
(t/defn ^:inline >double*
  "May involve non-out-of-range truncation."
  > double?
        ([x double?] x)
        ([x char?] (Primitive/uncheckedDoubleCast x))  ;; For purposes of Clojure intrinsics
#?(:clj ([x (t/- primitive? double? boolean? char?)] (clojure.lang.RT/uncheckedDoubleCast x))))

;; TODO TYPED `numerically`
;; TODO figure out how to use with goog.math.Integer/Long
#_(t/defn ^:inline >double > double?
  "Does not involve truncation or rounding."
        ([x double?] x)
#?(:clj ([x (t/and (t/- primitive? double? boolean?) numerically-double?)] (>double* x)))
        ([x boolean?] (if x #?(:clj 1.0 :cljs 1) #?(:clj 1.0 :cljs 0)))
#?(:clj ([x (t/and (t/isa? clojure.lang.BigInt) numerically-double?)] (>double* (.lpart x))))
#?(:clj ([x (t/and (t/isa? java.math.BigInteger) numerically-double?)] (.doubleValue x)))
#?(:clj ([x (t/and dn/ratio? numerically-double?)] (-> x .bigIntegerValue .doubleValue))))

;; ===== Unsigned ===== ;;

#?(:clj
(t/defn >unsigned
  {:adapted-from #{'ztellman/primitive-math 'gloss.data.primitives}}
  ([x byte?]  (Numeric/bitAnd (short 0xFF)       x))
  ([x short?] (Numeric/bitAnd (int   0xFFFF)     x))
  ([x int?]   (Numeric/bitAnd (long  0xFFFFFFFF) x))
  ([x long?]  (java.math.BigInteger. (int 1)
                (-> ^:val (ByteBuffer/allocate (int 8))
                    ^:val (.putLong x)
                    .array)))))

;; TODO TYPED awaiting `>long`
#_(:clj (t/defn ubyte>byte   [x long?   > long?] (-> x >byte   >long)))
#_(:clj (t/defn ushort>short [x long?   > long?] (-> x >short  >long)))
#_(:clj (t/defn uint>int     [x long?   > long?] (-> x >int    >long)))
#_(:clj (t/defn ulong>long   [x bigint? > long?] (-> x >bigint >long)))
