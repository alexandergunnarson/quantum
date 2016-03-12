(ns 
  ^{:doc "Useful binary operations."
    :attribution "Alex Gunnarson"}
  quantum.core.data.binary
  (:refer-clojure :exclude
    [unsigned-bit-shift-right bit-shift-left bit-shift-right
     bit-or bit-and bit-xor bit-not])
  (:require-quantum [:core macros log])
  #?(:clj (:import #_[quantum.core Numeric]
                   java.nio.ByteBuffer)))

; Because "cannot resolve symbol 'import'"
#?(:clj
(doseq [sym '[reverse
              true? false? nil?]]
  (ns-unmap 'quantum.core.data.binary sym)))

; TODO
; bit-clear
; bit-and-not
; bit-test
; bit-flip
; bit-set

; TODO ExceptionInInitializerError somewhere over here...
; TODO move namespace
#_(defnt ^boolean nil?
  ([^Object x] (quantum.core.Numeric/isNil x))
  ([:else   x] false))

#?(:clj (defalias nil? core/nil?))

#_(defnt ^boolean not'
  ([^boolean? x] (Numeric/not x))
  ([x] (if (nil? x) true))) ; Lisp nil punning

#_(defnt ^boolean true?
  ([^boolean? x] x)
  ([:else     x] false))

#?(:clj (defalias true? core/true?))

#_(defnt ^boolean false?
  ([^boolean? x] (not' x))
  ([:else     x] false))

#?(:clj (defalias false? core/false?))

#_(defmacro bit-not [x] `(Numeric/bitNot ~x))
#_(macros/variadic-proxy bit-and  quantum.core.Numeric/bitAnd) ; &
#_(macros/variadic-proxy bit-or   quantum.core.Numeric/bitOr)  ; |
#_(macros/variadic-proxy bit-xor  quantum.core.Numeric/bitXor)
#_(macros/variadic-proxy bool-and quantum.core.Numeric/and)    ; &&
#_(macros/variadic-proxy bool-or  quantum.core.Numeric/or)     ; ||
#_(macros/variadic-proxy bool-xor quantum.core.Numeric/xor)

(defalias bit-not core/bit-not)
(defalias bit-and core/bit-and)
(defalias bit-or  core/bit-or)
(defalias bit-xor core/bit-xor)

; ===== SHIFTS =====

#?(:clj (defalias bit-shift-left core/bit-shift-left)
     #_(defmacro bit-shift-left  [n bits]
       `(Numeric/shiftLeft ~n ~bits))
   :cljs (defalias bit-shift-left core/bit-shift-left))
(defalias << bit-shift-left)

#?(:clj (defalias bit-shift-right core/bit-shift-right)
    #_(defmacro bit-shift-right [n bits]
      `(Numeric/shiftRight ~n ~bits))
   :cljs (defalias bit-shift-right core/bit-shift-right))
(defalias >> bit-shift-right)

#?(:clj (defalias unsigned-bit-shift-right core/unsigned-bit-shift-right)
    #_(defmacro unsigned-bit-shift-right
      "Bit shift right, replace with zeros"
      [n bits]
      `(Numeric/unsignedShiftRight ~n ~bits))
   :cljs (defalias unsigned-bit-shift-right core/unsigned-bit-shift-right))

(defalias >>> unsigned-bit-shift-right)

(defn bits
  {:attribution "gloss.data.primitives"}
  [x num-bits]
  (map #(if (pos? (bit-and (bit-shift-left 1 %) x)) 1 0) (range num-bits)))

; ====== ENDIANNESS REVERSAL =======

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
             (<< (bit-and (long b6) 0xff) 48)
             (<< (bit-and (long b5) 0xff) 40)
             (<< (bit-and (long b4) 0xff) 32)
             (<< (bit-and (long b3) 0xff) 24)
             (<< (bit-and (long b2) 0xff) 16)
             (<< (bit-and (long b1) 0xff)  8)
                 (bit-and (long b0) 0xff))))
