(ns
  ^{:doc "Useful bit/binary operations."
    :attribution "alexandergunnarson"}
  quantum.core.data.bits
        (:refer-clojure :exclude
          [and not or])
        (:require
          [clojure.core                :as core]
          [quantum.core.data.primitive :as p]
          [quantum.core.type           :as t
            :refer [defnt]]
          [quantum.core.vars           :as var
            :refer [defalias]])
#?(:clj (:import
          [quantum.core Numeric]
        #_java.nio.ByteBuffer)))

; Because "cannot resolve symbol 'import'"
#?(:clj
(doseq [sym '[reverse
              true? false? nil?]]
  (ns-unmap 'quantum.core.data.bits sym)))

; TODO
; bit-clear
; bit-and-not
; bit-test
; bit-flip
; bit-set

; TODO ExceptionInInitializerError somewhere over here...
;; TODO TYPED move
#_(defnt ^boolean nil?
  ([^Object x] (quantum.core.Numeric/isNil x))
  ([:else   x] false))

;; TODO TYPED move
#?(:clj (defalias nil? core/nil?))

;; TODO TYPED move
#_(defnt ^boolean not'
  ([^boolean? x] (Numeric/not x))
  ([x] (if (nil? x) true))) ; Lisp nil punning

;; TODO TYPED move
#_(defnt ^boolean true?
  ([^boolean? x] x)
  ([:else     x] false))

;; TODO TYPED move
#?(:clj (defalias true? core/true?))

;; TODO TYPED move
#_(defnt ^boolean false?
  ([^boolean? x] (not' x))
  ([:else     x] false))

;; TODO TYPED move
#?(:clj (defalias false? core/false?))

;; ===== Logical bit-operations ===== ;;

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline not
#_([x p/primitive? > (t/type x)] (Numeric/bitNot x))
  ([x p/boolean? > p/boolean?] (Numeric/bitNot x))
  ([x p/byte?    > p/byte?]    (Numeric/bitNot x))
  ([x p/short?   > p/short?]   (Numeric/bitNot x))
  ([x p/char?    > p/char?]    (Numeric/bitNot x))
  ([x p/int?     > p/int?]     (Numeric/bitNot x))
  ([x p/long?    > p/long?]    (Numeric/bitNot x))
  ([x p/float?   > p/float?]   (Numeric/bitNot x))
  ([x p/double?  > p/double?]  (Numeric/bitNot x)))

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline and
#_([a p/boolean?, b p/boolean? > ?] (Numeric/bitAnd a b))
#_([a (t/- p/primitive? t/boolean?), b (t/- p/primitive? t/boolean?) > ?] (Numeric/bitAnd a b))
  ([a p/boolean?, b p/boolean?                             > p/boolean?] (Numeric/bitAnd a b))
  ([a p/byte?   , b p/byte?                                > p/byte?]    (Numeric/bitAnd a b))
  ([a p/byte?   , b p/short?                               > p/short?]   (Numeric/bitAnd a b))
  ([a p/byte?   , b (t/or p/char? p/int?)                  > p/int?]     (Numeric/bitAnd a b))
  ([a p/byte?   , b p/long?                                > p/long?]    (Numeric/bitAnd a b))
  ([a p/byte?   , b p/float?                               > p/float?]   (Numeric/bitAnd a b))
  ([a p/byte?   , b p/double?                              > p/double?]  (Numeric/bitAnd a b))
  ([a p/short?  , b (t/or p/byte? p/short?)                > p/short?]   (Numeric/bitAnd a b))
  ([a p/short?  , b (t/or p/char? p/int?)                  > p/int?]     (Numeric/bitAnd a b))
  ([a p/short?  , b p/long?                                > p/long?]    (Numeric/bitAnd a b))
  ([a p/short?  , b p/float?                               > p/float?]   (Numeric/bitAnd a b))
  ([a p/short?  , b p/double?                              > p/double?]  (Numeric/bitAnd a b))
  ([a p/char?   , b (t/or p/byte? p/short?)                > p/int?]     (Numeric/bitAnd a b))
  ([a p/char?   , b p/char?                                > p/char?]    (Numeric/bitAnd a b))
  ([a p/char?   , b p/int?                                 > p/int?]     (Numeric/bitAnd a b))
  ([a p/char?   , b p/long?                                > p/long?]    (Numeric/bitAnd a b))
  ([a p/char?   , b p/float?                               > p/float?]   (Numeric/bitAnd a b))
  ([a p/char?   , b p/double?                              > p/double?]  (Numeric/bitAnd a b))
  ([a p/int?    , b (t/or p/byte? p/short? p/char? p/int?) > p/int?]     (Numeric/bitAnd a b))
  ([a p/int?    , b p/long?                                > p/long?]    (Numeric/bitAnd a b))
  ([a p/int?    , b p/float?                               > p/float?]   (Numeric/bitAnd a b))
  ([a p/int?    , b p/double?                              > p/double?]  (Numeric/bitAnd a b))
  ([a p/long?   , b (t/or p/byte? p/short? p/char? p/int?
                          p/int? p/long?)                  > p/long?]    (Numeric/bitAnd a b))
  ([a p/long?   , b (t/or p/float? p/double?)              > p/double?]  (Numeric/bitAnd a b))
  ([a p/float?  , b (t/or p/byte? p/short? p/char? p/int?
                          p/float?)                        > p/float?]   (Numeric/bitAnd a b))
  ([a p/float?  , b (t/or p/long? p/double?)               > p/double?]  (Numeric/bitAnd a b))
  ([a p/double? , b p/primitive?                           > p/double?]  (Numeric/bitAnd a b)))

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline or
#_([a p/boolean?, b p/boolean? > ?] (Numeric/bitOr a b))
#_([a (t/- p/primitive? t/boolean?), b (t/- p/primitive? t/boolean?) > ?] (Numeric/bitOr a b))
  ([a p/boolean?, b p/boolean?                             > p/boolean?] (Numeric/bitOr a b))
  ([a p/byte?   , b p/byte?                                > p/byte?]    (Numeric/bitOr a b))
  ([a p/byte?   , b p/short?                               > p/short?]   (Numeric/bitOr a b))
  ([a p/byte?   , b (t/or p/char? p/int?)                  > p/int?]     (Numeric/bitOr a b))
  ([a p/byte?   , b p/long?                                > p/long?]    (Numeric/bitOr a b))
  ([a p/byte?   , b p/float?                               > p/float?]   (Numeric/bitOr a b))
  ([a p/byte?   , b p/double?                              > p/double?]  (Numeric/bitOr a b))
  ([a p/short?  , b (t/or p/byte? p/short?)                > p/short?]   (Numeric/bitOr a b))
  ([a p/short?  , b (t/or p/char? p/int?)                  > p/int?]     (Numeric/bitOr a b))
  ([a p/short?  , b p/long?                                > p/long?]    (Numeric/bitOr a b))
  ([a p/short?  , b p/float?                               > p/float?]   (Numeric/bitOr a b))
  ([a p/short?  , b p/double?                              > p/double?]  (Numeric/bitOr a b))
  ([a p/char?   , b (t/or p/byte? p/short?)                > p/int?]     (Numeric/bitOr a b))
  ([a p/char?   , b p/char?                                > p/char?]    (Numeric/bitOr a b))
  ([a p/char?   , b p/int?                                 > p/int?]     (Numeric/bitOr a b))
  ([a p/char?   , b p/long?                                > p/long?]    (Numeric/bitOr a b))
  ([a p/char?   , b p/float?                               > p/float?]   (Numeric/bitOr a b))
  ([a p/char?   , b p/double?                              > p/double?]  (Numeric/bitOr a b))
  ([a p/int?    , b (t/or p/byte? p/short? p/char? p/int?) > p/int?]     (Numeric/bitOr a b))
  ([a p/int?    , b p/long?                                > p/long?]    (Numeric/bitOr a b))
  ([a p/int?    , b p/float?                               > p/float?]   (Numeric/bitOr a b))
  ([a p/int?    , b p/double?                              > p/double?]  (Numeric/bitOr a b))
  ([a p/long?   , b (t/or p/byte? p/short? p/char? p/int?
                          p/int? p/long?)                  > p/long?]    (Numeric/bitOr a b))
  ([a p/long?   , b (t/or p/float? p/double?)              > p/double?]  (Numeric/bitOr a b))
  ([a p/float?  , b (t/or p/byte? p/short? p/char? p/int?
                          p/float?)                        > p/float?]   (Numeric/bitOr a b))
  ([a p/float?  , b (t/or p/long? p/double?)               > p/double?]  (Numeric/bitOr a b))
  ([a p/double? , b p/primitive?                           > p/double?]  (Numeric/bitOr a b)))

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline xor
#_([a p/boolean?, b p/boolean? > ?] (Numeric/bitXOr a b))
#_([a (t/- p/primitive? t/boolean?), b (t/- p/primitive? t/boolean?) > ?] (Numeric/bitXOr a b))
  ([a p/boolean?, b p/boolean?                             > p/boolean?] (Numeric/bitXOr a b))
  ([a p/byte?   , b p/byte?                                > p/byte?]    (Numeric/bitXOr a b))
  ([a p/byte?   , b p/short?                               > p/short?]   (Numeric/bitXOr a b))
  ([a p/byte?   , b (t/or p/char? p/int?)                  > p/int?]     (Numeric/bitXOr a b))
  ([a p/byte?   , b p/long?                                > p/long?]    (Numeric/bitXOr a b))
  ([a p/byte?   , b p/float?                               > p/float?]   (Numeric/bitXOr a b))
  ([a p/byte?   , b p/double?                              > p/double?]  (Numeric/bitXOr a b))
  ([a p/short?  , b (t/or p/byte? p/short?)                > p/short?]   (Numeric/bitXOr a b))
  ([a p/short?  , b (t/or p/char? p/int?)                  > p/int?]     (Numeric/bitXOr a b))
  ([a p/short?  , b p/long?                                > p/long?]    (Numeric/bitXOr a b))
  ([a p/short?  , b p/float?                               > p/float?]   (Numeric/bitXOr a b))
  ([a p/short?  , b p/double?                              > p/double?]  (Numeric/bitXOr a b))
  ([a p/char?   , b (t/or p/byte? p/short?)                > p/int?]     (Numeric/bitXOr a b))
  ([a p/char?   , b p/char?                                > p/char?]    (Numeric/bitXOr a b))
  ([a p/char?   , b p/int?                                 > p/int?]     (Numeric/bitXOr a b))
  ([a p/char?   , b p/long?                                > p/long?]    (Numeric/bitXOr a b))
  ([a p/char?   , b p/float?                               > p/float?]   (Numeric/bitXOr a b))
  ([a p/char?   , b p/double?                              > p/double?]  (Numeric/bitXOr a b))
  ([a p/int?    , b (t/or p/byte? p/short? p/char? p/int?) > p/int?]     (Numeric/bitXOr a b))
  ([a p/int?    , b p/long?                                > p/long?]    (Numeric/bitXOr a b))
  ([a p/int?    , b p/float?                               > p/float?]   (Numeric/bitXOr a b))
  ([a p/int?    , b p/double?                              > p/double?]  (Numeric/bitXOr a b))
  ([a p/long?   , b (t/or p/byte? p/short? p/char? p/int?
                          p/int? p/long?)                  > p/long?]    (Numeric/bitXOr a b))
  ([a p/long?   , b (t/or p/float? p/double?)              > p/double?]  (Numeric/bitXOr a b))
  ([a p/float?  , b (t/or p/byte? p/short? p/char? p/int?
                          p/float?)                        > p/float?]   (Numeric/bitXOr a b))
  ([a p/float?  , b (t/or p/long? p/double?)               > p/double?]  (Numeric/bitXOr a b))
  ([a p/double? , b p/primitive?                           > p/double?]  (Numeric/bitXOr a b)))

;; ===== Bit-shifts ===== ;;

;; ----- Logical bit-shifts ---- ;;

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline <<<
  "Unsigned (logical) bit shift left"
#_([x (t/- p/primitive? t/boolean?), n (t/- p/primitive? t/boolean?) > (t/type x)]
    (Numeric/bitOr a b))
  ([x p/byte?  , n (t/- p/primitive? p/boolean?) > p/byte?]   (Numeric/shiftLeft x n))
  ([x p/short? , n (t/- p/primitive? p/boolean?) > p/short?]  (Numeric/shiftLeft x n))
  ;; TODO implement this correctly because it likely isn't correct just to do `<<` in Java
#_([x p/char?  , n (t/- p/primitive? p/boolean?) > p/char?]   (Numeric/shiftLeft x n))
  ([x p/int?   , n (t/- p/primitive? p/boolean?) > p/int?]    (Numeric/shiftLeft x n))
  ([x p/long?  , n (t/- p/primitive? p/boolean?) > p/long?]   (Numeric/shiftLeft x n))
  ([x p/float? , n (t/- p/primitive? p/boolean?) > p/float?]  (Numeric/shiftLeft x n))
  ([x p/double?, n (t/- p/primitive? p/boolean?) > p/double?] (Numeric/shiftLeft x n)))

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline >>>
  "Unsigned (logical) bit shift right"
#_([x (t/- p/primitive? t/boolean?), n (t/- p/primitive? t/boolean?) > (t/type x)]
    (Numeric/bitOr a b))
  ([x p/byte?  , n (t/- p/primitive? p/boolean?) > p/byte?]   (Numeric/uShiftRight x n))
  ([x p/short? , n (t/- p/primitive? p/boolean?) > p/short?]  (Numeric/uShiftRight x n))
  ([x p/char?  , n (t/- p/primitive? p/boolean?) > p/char?]   (Numeric/uShiftRight x n))
  ([x p/int?   , n (t/- p/primitive? p/boolean?) > p/int?]    (Numeric/uShiftRight x n))
  ([x p/long?  , n (t/- p/primitive? p/boolean?) > p/long?]   (Numeric/uShiftRight x n))
  ([x p/float? , n (t/- p/primitive? p/boolean?) > p/float?]  (Numeric/uShiftRight x n))
  ([x p/double?, n (t/- p/primitive? p/boolean?) > p/double?] (Numeric/uShiftRight x n)))

;; ----- Arithmetic bit-shifts ----- ;;

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline <<
  "Arithmetic bit shift left"
#_([x (t/- p/primitive? t/boolean?), n (t/- p/primitive? t/boolean?) > (t/type x)]
    (Numeric/bitOr a b))
  ([x p/byte?  , n (t/- p/primitive? p/boolean?) > p/byte?]   (Numeric/shiftLeft x n))
  ([x p/short? , n (t/- p/primitive? p/boolean?) > p/short?]  (Numeric/shiftLeft x n))
  ([x p/char?  , n (t/- p/primitive? p/boolean?) > p/char?]   (Numeric/shiftLeft x n))
  ([x p/int?   , n (t/- p/primitive? p/boolean?) > p/int?]    (Numeric/shiftLeft x n))
  ([x p/long?  , n (t/- p/primitive? p/boolean?) > p/long?]   (Numeric/shiftLeft x n))
  ([x p/float? , n (t/- p/primitive? p/boolean?) > p/float?]  (Numeric/shiftLeft x n))
  ([x p/double?, n (t/- p/primitive? p/boolean?) > p/double?] (Numeric/shiftLeft x n)))

;; TODO TYPED we can shorten this by having dependent types
(defnt ^:inline >>
  "Arithmetic bit shift right"
#_([x (t/- p/primitive? t/boolean?), n (t/- p/primitive? t/boolean?) > (t/type x)]
    (Numeric/bitOr a b))
  ([x p/byte?  , n (t/- p/primitive? p/boolean?) > p/byte?]   (Numeric/shiftRight x n))
  ([x p/short? , n (t/- p/primitive? p/boolean?) > p/short?]  (Numeric/shiftRight x n))
  ([x p/char?  , n (t/- p/primitive? p/boolean?) > p/char?]   (Numeric/shiftRight x n))
  ([x p/int?   , n (t/- p/primitive? p/boolean?) > p/int?]    (Numeric/shiftRight x n))
  ([x p/long?  , n (t/- p/primitive? p/boolean?) > p/long?]   (Numeric/shiftRight x n))
  ([x p/float? , n (t/- p/primitive? p/boolean?) > p/float?]  (Numeric/shiftRight x n))
  ([x p/double?, n (t/- p/primitive? p/boolean?) > p/double?] (Numeric/shiftRight x n)))

;; ===== Rotations ===== ;;

(defnt rotate-left
  {:from "http://hg.openjdk.java.net/jdk7u/jdk7u6/jdk/file/8c2c5d63a17e/src/share/classes/java/lang/Integer.java"}
  [x ???, n ???] (or (<< x n) (>>> x (- n))))

(defn bit-count
  "Counts the number of bits set in n"
  {:from 'cljs.core}
  [v]
  (let [v (- v (bit-and (bit-shift-right v 1) 0x55555555))
        v (+ (bit-and v 0x33333333) (bit-and (bit-shift-right v 2) 0x33333333))]
    (bit-shift-right (* (bit-and (+ v (bit-shift-right v 4)) 0xF0F0F0F) 0x1010101) 24)))

(declare bits)

(defalias ? core/bit-test)

;; ===== Bulk bit-operations ===== ;;

(defn ?-coll
  "Returns true or false for the bit at the given index of the collection."
  [bits i]
  (? (bits (>> i 6)) (and i 0x3f)))

(defn bits
  "The bits of x, aggregated into a vector and truncated/extended to length n."
  {:adapted-from 'gloss.data.primitives}
  [x n]
  (mapv #(if (pos? (and (<< 1 %) x)) 1 0) (range n)))

(bits 1 64)

(defn truncate
  "Truncates x to the specified number of bits."
  {:adapted-from 'bigml.sketchy.murmur}
  [#?(:clj ^long x :cljs x)
   #?(:clj ^long n :cljs n)]
  (and x (unchecked-dec (<< 1 n))))

;; ====== Endianness reversal ====== ;;

; TODO DEPS
#_(:clj
(defnt reverse
  (^short  [^short  x] (Numeric/reverseShort x))
  (^int    [^int    x] (Numeric/reverseInt x))
  (^long   [^long   x] (Numeric/reverseLong x))))

#_(:clj
(defnt' make-long
  "Combines byte values into a long value."
  [^byte b7 ^byte b6 ^byte b5 ^byte b4
   ^byte b3 ^byte b2 ^byte b1 ^byte b0]
     (bit-or (<<          (long b7)       56)
             (<< (and (long b6) 0xff) 48)
             (<< (and (long b5) 0xff) 40)
             (<< (and (long b4) 0xff) 32)
             (<< (and (long b3) 0xff) 24)
             (<< (and (long b2) 0xff) 16)
             (<< (and (long b1) 0xff)  8)
                 (and (long b0) 0xff))))
