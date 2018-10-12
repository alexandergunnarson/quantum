(ns quantum.core.data.bits
  "Useful bit/binary operations.

   Note that bitwise operators on CLJS doubles behave differently than on CLJ doubles: the bitwise
   operators `<<`, `>>`, `&`, `|` and `~` are defined in terms of operations on 32-bit integers.
   Doing a bitwise operation converts the number to a 32-bit signed int, losing any fractions and
   higher-place bits than 32, before doing the calculation and then converting back to Number."
  {:todo #{"Port http://graphics.stanford.edu/~seander/bithacks.html"}}
        (:refer-clojure :exclude
          [and conj contains? empty not or])
        (:require
          [clojure.core                :as core]
          [quantum.core.data.numeric   :as dnum
            :refer [std-fixint?]]
          [quantum.core.data.primitive :as p
            :refer [>long]]
          [quantum.core.type           :as t]
          [quantum.core.vars           :as var
            :refer [defalias]])
#?(:clj (:import
          [clojure.lang Numbers]
          [quantum.core Numeric])))

;; TODO make sure that for all bit ops here, there's a checked and unchecked version Because
;; currently the CLJS version just truncates the input without warning

(def bit-false 0)
(def bit-true  1)

;; ===== Decremented bit sizes of types ===== ;;
;; For bit-manipulation purposes

(var/def dec-boolean-bits (core/dec p/boolean-bits))
(var/def dec-byte-bits    (core/dec p/byte-bits)))
(var/def dec-short-bits   (core/dec p/short-bits)))
(var/def dec-int-bits     (core/dec p/int-bits)))
(var/def dec-long-bits    (core/dec p/long-bits)))
(var/def dec-float-bits   (core/dec p/float-bits)))
(var/def dec-double-bits  (core/dec p/double-bits)))

(t/defn ^:inline dec-bits-of ; > dnum/fixint? ; TODO TYPED
  "For bit manipulation purposes"
        ([x p/boolean? > p/long?] dec-boolean-bits)
#?(:clj ([x p/byte?    > p/long?] dec-byte-bits))
#?(:clj ([x p/short?   > p/long?] dec-short-bits))
#?(:clj ([x p/int?     > p/long?] dec-int-bits))
#?(:clj ([x p/long?    > p/long?] dec-long-bits))
#?(:clj ([x p/float?   > p/long?] dec-float-bits))
        ([x p/double?  > p/long?] dec-double-bits))

;; ===== Logical bit-operations ===== ;;
;; NOTE: we won't be supporting `clojure.core/and-not`

(t/defn ^:inline not
  "Bitwise `not`."
  {:incorporated {'clojure.core/bit-not #inst "2018-10-11"
                  'cljs.core/bit-not    #inst "2018-10-11"}}
  #?@(:clj  [([x p/primitive? > (t/type x)] (Numeric/bitNot x))]
      :cljs [([x p/boolean?   > (t/type x)] (if x false true))
             ([x p/double?    > (t/assume numerically-int?)] (core/bit-not x))]))

;; TODO make variadic
;; TODO TYPED we can shorten this by having dependent types
(t/defn ^:inline and
  "Bitwise `and`."
  {:incorporated {'clojure.core/bit-and #inst "2018-10-11"
                  'cljs.core/bit-and    #inst "2018-10-11"}}
#?@(:clj  [#_([a p/boolean?, b p/boolean? > ?] (Numeric/bitAnd a b))
           #_([a (t/- p/primitive? p/boolean?)
               b (t/- p/primitive? p/boolean?) > ?] (Numeric/bitAnd a b))
           (     [a p/boolean?, b p/boolean?                    > p/boolean?] (Numeric/bitAnd a b))
           (     [a p/byte?   , b p/byte?                       > p/byte?]    (Numeric/bitAnd a b))
           (     [a p/byte?   , b p/short?                      > p/short?]   (Numeric/bitAnd a b))
           (     [a p/byte?   , b (t/or p/char? p/int?)         > p/int?]     (Numeric/bitAnd a b))
           (     [a p/byte?   , b p/long?                       > p/long?]    (Numeric/bitAnd a b))
           (     [a p/byte?   , b p/float?                      > p/float?]   (Numeric/bitAnd a b))
           (     [a p/byte?   , b p/double?                     > p/double?]  (Numeric/bitAnd a b))
           (     [a p/short?  , b (t/or p/byte? p/short?)       > p/short?]   (Numeric/bitAnd a b))
           (     [a p/short?  , b (t/or p/char? p/int?)         > p/int?]     (Numeric/bitAnd a b))
           (     [a p/short?  , b p/long?                       > p/long?]    (Numeric/bitAnd a b))
           (     [a p/short?  , b p/float?                      > p/float?]   (Numeric/bitAnd a b))
           (     [a p/short?  , b p/double?                     > p/double?]  (Numeric/bitAnd a b))
           (     [a p/char?   , b (t/or p/byte? p/short?)       > p/int?]     (Numeric/bitAnd a b))
           (     [a p/char?   , b p/char?                       > p/char?]    (Numeric/bitAnd a b))
           (     [a p/char?   , b p/int?                        > p/int?]     (Numeric/bitAnd a b))
           (     [a p/char?   , b p/long?                       > p/long?]    (Numeric/bitAnd a b))
           (     [a p/char?   , b p/float?                      > p/float?]   (Numeric/bitAnd a b))
           (     [a p/char?   , b p/double?                     > p/double?]  (Numeric/bitAnd a b))
           (     [a p/int?    , b (t/or p/byte? p/short? p/char?
                                        p/int?)                 > p/int?]     (Numeric/bitAnd a b))
           (     [a p/int?    , b p/long?                       > p/long?]    (Numeric/bitAnd a b))
           (     [a p/int?    , b p/float?                      > p/float?]   (Numeric/bitAnd a b))
           (     [a p/int?    , b p/double?                     > p/double?]  (Numeric/bitAnd a b))
           (     [a p/long?   , b (t/or p/byte? p/short? p/char?
                                        p/int?)                 > p/long?]    (Numeric/bitAnd a b))
           (^:in [a p/long?   , b long?                         > p/long?]    (Numbers/and    a b))
           (     [a p/long?   , b (t/or p/float? p/double?)     > p/double?]  (Numeric/bitAnd a b))
           (     [a p/float?  , b (t/or p/byte? p/short? p/char?
                                        p/int? p/float?)        > p/float?]   (Numeric/bitAnd a b))
           (     [a p/float?  , b (t/or p/long? p/double?)      > p/double?]  (Numeric/bitAnd a b))
           (     [a p/double? , b (t/- p/primitive? p/boolean?) > p/double?]  (Numeric/bitAnd a b))]
    :cljs [(     [a p/boolean?, b p/boolean?                    > p/boolean?] (core/and       a b))
           (     [a p/double? , b p/double?                     > (t/assume numerically-int?)]
                  (core/bit-and a b))]))

;; TODO make variadic
;; TODO TYPED we can shorten this by having dependent types
(t/defn ^:inline or
  "Bitwise `or`."
  {:incorporated {'clojure.core/bit-or #inst "2018-10-11"
                  'cljs.core/bit-or    #inst "2018-10-11"}}
#?@(:clj  [#_([a p/boolean?, b p/boolean? > ?] (Numeric/bitOr a b))
           #_([a (t/- p/primitive? p/boolean?)
               b (t/- p/primitive? p/boolean?) > ?] (Numeric/bitOr a b))
           (     [a p/boolean?, b p/boolean?                    > p/boolean?] (Numeric/bitOr a b))
           (     [a p/byte?   , b p/byte?                       > p/byte?]    (Numeric/bitOr a b))
           (     [a p/byte?   , b p/short?                      > p/short?]   (Numeric/bitOr a b))
           (     [a p/byte?   , b (t/or p/char? p/int?)         > p/int?]     (Numeric/bitOr a b))
           (     [a p/byte?   , b p/long?                       > p/long?]    (Numeric/bitOr a b))
           (     [a p/byte?   , b p/float?                      > p/float?]   (Numeric/bitOr a b))
           (     [a p/byte?   , b p/double?                     > p/double?]  (Numeric/bitOr a b))
           (     [a p/short?  , b (t/or p/byte? p/short?)       > p/short?]   (Numeric/bitOr a b))
           (     [a p/short?  , b (t/or p/char? p/int?)         > p/int?]     (Numeric/bitOr a b))
           (     [a p/short?  , b p/long?                       > p/long?]    (Numeric/bitOr a b))
           (     [a p/short?  , b p/float?                      > p/float?]   (Numeric/bitOr a b))
           (     [a p/short?  , b p/double?                     > p/double?]  (Numeric/bitOr a b))
           (     [a p/char?   , b (t/or p/byte? p/short?)       > p/int?]     (Numeric/bitOr a b))
           (     [a p/char?   , b p/char?                       > p/char?]    (Numeric/bitOr a b))
           (     [a p/char?   , b p/int?                        > p/int?]     (Numeric/bitOr a b))
           (     [a p/char?   , b p/long?                       > p/long?]    (Numeric/bitOr a b))
           (     [a p/char?   , b p/float?                      > p/float?]   (Numeric/bitOr a b))
           (     [a p/char?   , b p/double?                     > p/double?]  (Numeric/bitOr a b))
           (     [a p/int?    , b (t/or p/byte? p/short? p/char?
                                        p/int?)                 > p/int?]     (Numeric/bitOr a b))
           (     [a p/int?    , b p/long?                       > p/long?]    (Numeric/bitOr a b))
           (     [a p/int?    , b p/float?                      > p/float?]   (Numeric/bitOr a b))
           (     [a p/int?    , b p/double?                     > p/double?]  (Numeric/bitOr a b))
           (     [a p/long?   , b (t/or p/byte? p/short? p/char?
                                        p/int?)                 > p/long?]    (Numeric/bitOr a b))
           (^:in [a p/long?   , b long?                         > p/long?]    (Numbers/or    a b))
           (     [a p/long?   , b (t/or p/float? p/double?)     > p/double?]  (Numeric/bitOr a b))
           (     [a p/float?  , b (t/or p/byte? p/short? p/char?
                                        p/int? p/float?)        > p/float?]   (Numeric/bitOr a b))
           (     [a p/float?  , b (t/or p/long? p/double?)      > p/double?]  (Numeric/bitOr a b))
           (     [a p/double? , b (t/- p/primitive? p/boolean?) > p/double?]  (Numeric/bitOr a b))]
    :cljs [(     [a p/boolean?, b p/boolean?                    > p/boolean?] (core/or       a b))
           (     [a p/double? , b p/double?                     > (t/assume numerically-int?)]
                   (core/bit-or a b))]))

;; TODO make variadic
;; TODO TYPED we can shorten this by having dependent types
(t/defn ^:inline xor
  "Bitwise `xor`."
  {:incorporated {'clojure.core/bit-xor #inst "2018-10-11"
                  'cljs.core/bit-xor    #inst "2018-10-11"}}
#?@(:clj  [#_([a p/boolean?, b p/boolean? > ?] (Numeric/bitXOr a b))
           #_([a (t/- p/primitive? p/boolean?)
               b (t/- p/primitive? p/boolean?) > ?] (Numeric/bitXOr a b))
           (     [a p/boolean?, b p/boolean?                    > p/boolean?] (Numeric/bitXOr a b))
           (     [a p/byte?   , b p/byte?                       > p/byte?]    (Numeric/bitXOr a b))
           (     [a p/byte?   , b p/short?                      > p/short?]   (Numeric/bitXOr a b))
           (     [a p/byte?   , b (t/or p/char? p/int?)         > p/int?]     (Numeric/bitXOr a b))
           (     [a p/byte?   , b p/long?                       > p/long?]    (Numeric/bitXOr a b))
           (     [a p/byte?   , b p/float?                      > p/float?]   (Numeric/bitXOr a b))
           (     [a p/byte?   , b p/double?                     > p/double?]  (Numeric/bitXOr a b))
           (     [a p/short?  , b (t/or p/byte? p/short?)       > p/short?]   (Numeric/bitXOr a b))
           (     [a p/short?  , b (t/or p/char? p/int?)         > p/int?]     (Numeric/bitXOr a b))
           (     [a p/short?  , b p/long?                       > p/long?]    (Numeric/bitXOr a b))
           (     [a p/short?  , b p/float?                      > p/float?]   (Numeric/bitXOr a b))
           (     [a p/short?  , b p/double?                     > p/double?]  (Numeric/bitXOr a b))
           (     [a p/char?   , b (t/or p/byte? p/short?)       > p/int?]     (Numeric/bitXOr a b))
           (     [a p/char?   , b p/char?                       > p/char?]    (Numeric/bitXOr a b))
           (     [a p/char?   , b p/int?                        > p/int?]     (Numeric/bitXOr a b))
           (     [a p/char?   , b p/long?                       > p/long?]    (Numeric/bitXOr a b))
           (     [a p/char?   , b p/float?                      > p/float?]   (Numeric/bitXOr a b))
           (     [a p/char?   , b p/double?                     > p/double?]  (Numeric/bitXOr a b))
           (     [a p/int?    , b (t/or p/byte? p/short? p/char?
                                        p/int?)                 > p/int?]     (Numeric/bitXOr a b))
           (     [a p/int?    , b p/long?                       > p/long?]    (Numeric/bitXOr a b))
           (     [a p/int?    , b p/float?                      > p/float?]   (Numeric/bitXOr a b))
           (     [a p/int?    , b p/double?                     > p/double?]  (Numeric/bitXOr a b))
           (     [a p/long?   , b (t/or p/byte? p/short? p/char?
                                        p/int?)                 > p/long?]    (Numeric/bitXOr a b))
           (^:in [a p/long?   , b long?                         > p/long?]    (Numbers/xor    a b))
           (     [a p/long?   , b (t/or p/float? p/double?)     > p/double?]  (Numeric/bitXOr a b))
           (     [a p/float?  , b (t/or p/byte? p/short? p/char?
                                        p/int? p/float?)        > p/float?]   (Numeric/bitXOr a b))
           (     [a p/float?  , b (t/or p/long? p/double?)      > p/double?]  (Numeric/bitXOr a b))
           (     [a p/double? , b (t/- p/primitive? p/boolean?) > p/double?]  (Numeric/bitXOr a b))]
    :cljs [(     [a p/boolean?, b p/boolean?                    > p/boolean?]
                   (js* "(~{} !== ~{})" a b))
           (     [a p/double? , b p/double?                     > (t/assume numerically-int?)]
                   (core/bit-xor a b))]))

;; ===== Bit-shifts ===== ;;

;; ----- Logical bit-shifts ---- ;;

;; TODO make variadic
(t/defn ^:inline <<<
  "Unsigned (logical) bitwise shift left"
#?(:clj  (^:in [x p/int? , n p/int?  > (t/type x)] (Numbers/shiftRightInt x n)))
#?(:clj  (^:in [x p/long?, n p/long? > (t/type x)] (Numbers/shiftRight    x n)))
#?(:clj  ;; TODO implement the `char` op correctly because it likely isn't correct just to do
         ;; the straight bit op in Java
         (     [x (t/- p/primitive? p/boolean? p/int? p/long?), n p/integral? > (t/type x)]
                 (Numeric/shiftRight x n)))
#?(:clj  (     [x p/int?   , n (t/- p/integral? p/int?)  > (t/type x)] (Numeric/shiftRight x n)))
#?(:clj  (     [x p/long?  , n (t/- p/integral? p/long?) > (t/type x)] (Numeric/shiftRight x n)))
#?(:cljs (     [x p/double?, n std-fixint? > (t/assume numerically-int?)]
                 (core/bit-shift-right x n))))

;; TODO make variadic
(t/defn ^:inline >>>
  "Unsigned (logical) bitwise shift right"
  {:incorporated {'clojure.core/unsigned-bit-shift-right #inst "2018-10-11"
                  'cljs.core/unsigned-bit-shift-right    #inst "2018-10-11"}}
#?(:clj  (^:in [x p/int? , n p/int?  > (t/type x)] (Numbers/unsignedShiftRightInt x n)))
#?(:clj  (^:in [x p/long?, n p/long? > (t/type x)] (Numbers/unsignedShiftRight    x n)))
#?(:clj  ;; TODO implement the `char` op correctly because it likely isn't correct just to do
         ;; the straight bit op in Java
         (     [x (t/- p/primitive? p/boolean? p/int? p/long?), n p/integral? > (t/type x)]
                 (Numeric/uShiftRight x n)))
#?(:clj  (     [x p/int?   , n (t/- p/integral? p/int?)  > (t/type x)] (Numeric/uShiftRight x n)))
#?(:clj  (     [x p/long?  , n (t/- p/integral? p/long?) > (t/type x)] (Numeric/uShiftRight x n)))
#?(:cljs ([x p/double?, n std-fixint? > (t/assume numerically-int?)]
           (core/unsigned-bit-shift-right x n))))

;; ----- Arithmetic bit-shifts ----- ;;

;; TODO make variadic
(t/defn ^:inline <<
  "Arithmetic bitwise shift left"
  {:incorporated {'clojure.core/bit-shift-left #inst "2018-10-11"
                  'cljs.core/bit-shift-left    #inst "2018-10-11"}}
#?(:clj  (^:in [x p/int? , n p/int?  > (t/type x)] (Numbers/shiftLeftInt x n)))
#?(:clj  (^:in [x p/long?, n p/long? > (t/type x)] (Numbers/shiftLeft    x n)))
#?(:clj  ;; TODO implement the `char` op correctly because it likely isn't correct just to do
         ;; the straight bit op in Java
         (     [x (t/- p/primitive? p/boolean? p/int? p/long?), n p/integral? > (t/type x)]
                 (Numeric/shiftLeft x n)))
#?(:clj  (     [x p/int?   , n (t/- p/integral? p/int?)  > (t/type x)] (Numeric/shiftLeft x n)))
#?(:clj  (     [x p/long?  , n (t/- p/integral? p/long?) > (t/type x)] (Numeric/shiftLeft x n)))
#?(:cljs (     [x p/double?, n std-fixint? > (t/assume numerically-int?)]
                 (core/bit-shift-left x n))))

;; TODO make variadic
;; TODO TYPED `t/numerically-int?`
(t/defn ^:inline >>
  "Arithmetic bitwise shift right"
  {:incorporated {'clojure.core/bit-shift-right #inst "2018-10-11"
                  'cljs.core/bit-shift-right    #inst "2018-10-11"}}
#?(:clj  (^:in [x p/int? , n p/int?  > (t/type x)] (Numbers/shiftRightInt x n)))
#?(:clj  (^:in [x p/long?, n p/long? > (t/type x)] (Numbers/shiftRight    x n)))
#?(:clj  ;; TODO implement the `char` op correctly because it likely isn't correct just to do
         ;; the straight bit op in Java
         (     [x (t/- p/primitive? p/boolean? p/int? p/long?), n p/integral? > (t/type x)]
                 (Numeric/shiftRight x n)))
#?(:clj  (     [x p/int?   , n (t/- p/integral? p/int?)  > (t/type x)] (Numeric/shiftRight x n)))
#?(:clj  (     [x p/long?  , n (t/- p/integral? p/long?) > (t/type x)] (Numeric/shiftRight x n)))
#?(:cljs (     [x p/double?, n std-fixint? > (t/assume numerically-int?)]
                 (core/bit-shift-right x n))))

;; ===== Single-bit operations ===== ;;

;; TODO add bit operations with checked indices

(t/defn ^:inline bit-set-false*
  "Makes the bit at the provided index ->`i` `bit-false`.
   Unchecked w.r.t. the bit index.
   Equivalent to `clojure.core/bit-clear`."
  {:incorporated {'clojure.core/bit-clear #inst "2018-10-11"
                  'cljs.core/bit-clear    #inst "2018-10-11"}
   :todo #{"Extend index to non-longs"
           "Extend usage to non-primitives"}}
#?(:clj  ([x (t/- p/primitive? p/boolean?), i p/long? > (t/type x)] (Numeric/bitClear x i))
   :cljs ([x p/double?, i std-fixint? > (t/assume numerically-int?)] (core/bit-clear x i))))

(t/defn ^:inline bit-set-true*
  "Makes the bit at the provided index ->`i` `bit-true`.
   Unchecked w.r.t. the bit index.
   Equivalent to `clojure.core/bit-set`."
  {:incorporated {'clojure.core/bit-set #inst "2018-10-11"
                  'cljs.core/bit-set    #inst "2018-10-11"}
   :todo #{"Extend index to non-longs"
           "Extend usage to non-primitives"}}
#?(:clj  ([x (t/- p/primitive? p/boolean?), i p/long? > (t/type x)] (Numeric/bitSet x i))
   :cljs ([x p/double?, i std/fixint? > (t/assume numerically-int?)] (core/bit-set x i))))

(t/defn ^:inline bit-not*
  "Applies `not` to the bit at the provided index ->`i`.
   Unchecked w.r.t. the bit index.
   Equivalent to `clojure.core/bit-flip`."
  {:incorporated {'clojure.core/bit-flip #inst "2018-10-11"
                  'cljs.core/bit-flip    #inst "2018-10-11"}
   :todo #{"Extend index to non-longs"
           "Extend usage to non-primitives"}}
#?(:clj  ([x (t/- p/primitive? p/boolean?), i p/long? > (t/type x)] (Numeric/bitFlip x i))
   :cljs ([x p/double?, i std-fixint? > (t/assume numerically-int?)] (core/bit-flip x i))))

(t/defn ^:inline bit-true?*
  "Outputs whether the bit at the provided index ->`i` is `bit-true`.
   Unchecked w.r.t. the bit index.
   Equivalent to `clojure.core/bit-test`."
  {:incorporated {'clojure.core/bit-test #inst "2018-10-11"
                  'cljs.core/bit-test    #inst "2018-10-11"}
   :todo #{"Extend index to non-longs"
           "Extend usage to non-primitives"}}
#?(:clj  ([x (t/- p/primitive? p/boolean?), i p/long?     > p/boolean?] (Numeric/bitTest x i))
   :cljs ([x p/double?                    , i std-fixint? > p/boolean?] (core/bit-test x i))))

(defalias ? test*)

;; ===== Rotations ===== ;;

;; TODO TYPED
#_(defnt rotate-left
  {:from "http://hg.openjdk.java.net/jdk7u/jdk7u6/jdk/file/8c2c5d63a17e/src/share/classes/java/lang/Integer.java"}
  [x ???, n ???] (or (<< x n) (>>> x (- n))))

;; TODO extend to CLJ
;; TODO can use e.g. java.lang.Integer/bitCount for the purpose
;; TODO TYPED
#_(:cljs
(defn bit-count
  "Counts the number of bits set in n"
  {:from 'cljs.core}
  [v]
  (let [v (- v (and (>> v 1) 0x55555555))
        v (+ (and v 0x33333333) (and (>> v 2) 0x33333333))]
    (>> (* (and (+ v (>> v 4)) 0xF0F0F0F) 0x1010101) 24))))

;; ===== Bulk bit-operations ===== ;;

;; TODO TYPED
#_(defnt >bits
  "The bits of ->`x`, aggregated into a vector and truncated/extended to length ->`n`."
  {:adapted-from 'gloss.data.primitives}
  [x , n length?]
  (->> (range n)
       (mapv (t/fn [] (if (pos? (and (<< 1 %) x))
                          bit-true
                          bit-false)))))

;; TODO TYPED
#_(defnt test*-coll
  "Returns true or false for the bit at the given index ->`i` of ->`xs`."
  [xs (t/of bit-like?), i index?]
  (? (>bits (>> i 6)) (and i 0x3f)))

;; TODO TYPED
#_(defn truncate
  "Truncates ->`x` to the specified number of bits."
  {:adapted-from 'bigml.sketchy.murmur}
  [#?(:clj ^long x :cljs x)
   #?(:clj ^long n :cljs n)]
  (and x (unchecked-dec (<< 1 n))))

;; ====== Endianness reversal ====== ;;

;; TODO implement based on https://github.com/ztellman/primitive-math/blob/master/src/primitive_math/Primitives.java
#_(:clj
(defnt reverse [x p/primitive? > (t/type x)] ...))

;; TODO implement `reverse-bytes` (see related methods in e.g. `Integer` and `Long` classes)

#?(:clj
(defnt bytes>long
  "Combines safely-byte-coercible values into a long value."
  {:todo #{"Move"
           "Implement for CLJS"
           "Awaiting `bit/or` variadicity to make slightly cleaner"
           "Support anything safely-byte-coercible, not just direct bytes"}}
  > p/long?
  [b7 p/byte?, b6 p/byte?, b5 p/byte?, b4 p/byte?
   b3 p/byte?, b2 p/byte?, b1 p/byte?, b0 p/byte?]
  (-> (<< (>long b7) 56)
      (or (<< (and (>long b6) 0xff) 48))
      (or (<< (and (>long b5) 0xff) 40))
      (or (<< (and (>long b4) 0xff) 32))
      (or (<< (and (>long b3) 0xff) 24))
      (or (<< (and (>long b2) 0xff) 16))
      (or (<< (and (>long b1) 0xff)  8))
      (or (and (>long b0) 0xff)))))

;; ===== Bit sets ===== ;;
;; May be thought of as a map from bit-index / non-negative integer to boolean, or as a set of
;; bit-indices / non-negative integers.

(def bit-set? #?(:clj p/integral? :cljs p/double?))

(var/def empty
  "For bit set purposes.
   We choose the default bit set size to be `long` in CLJ and `number` (i.e. `double`) in CLJS to
   give it the maximum size possible for a primitive bit set."
  0)

;; TODO TYPED variadic, expressions
#_(:clj
(defnt conj
  "For bit set purposes."
  {:todo #{"Implement variadic arity"
           "Implement for CLJS"
           ""}}
  ([] empty)
  ([v #?(:clj p/long? :cljs p/double?)] (conj empty v))
  ([xs bit-set?, v0 bit-set-value?] (bit-set-true* xs v0))
  ([xs bit-set?, v0 bit-set-value?, v1 bit-set-value?] (-> xs (conj v0) (conj v1)))
#_([xs bit-set?, v0 bit-set-value?, v1 bit-set-value? & vs (t/of bit-set-value?)] ...)))

;; TODO TYPED `numerically-integer?`, expressions
#_(defnt contains?
  "Tests if the bit set ->`xs` contains the value ->`v`."
  > p/boolean?
  ([xs p/byte? , v (t/and t/numerically-integer? (<= 0 % (>bit-size ...))] (bit-true?* xs v))
  ([xs p/short?, v (t/and t/numerically-integer? (<= 0 % (>bit-size ...)))] (bit-true?* xs v))
  ([xs p/char? , v (t/and t/numerically-integer? (<= 0 % (>bit-size ...)))] (bit-true?* xs v))
  ([xs p/int?  , v (t/and t/numerically-integer? (<= 0 % (>bit-size ...)))] (bit-true?* xs v))
  ([xs p/long? , v (t/and t/numerically-integer? (<= 0 % (>bit-size ...)))] (bit-true?* xs v)))
