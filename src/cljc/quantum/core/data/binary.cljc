(ns 
  ^{:doc "Useful binary operations."
    :attribution "Alex Gunnarson"}
  quantum.core.data.binary
  (:require-quantum [ns macros log])
  #?(:clj (:import [quantum.core Numeric] java.nio.ByteBuffer)))

; Because "cannot resolve symbol 'import'"
(doseq [sym '[bit-or bit-and bit-xor bit-not
              unsigned-bit-shift-right
              reverse bit-shift-left bit-shift-right
              true? false? nil?]]
  (ns-unmap 'quantum.core.data.binary sym))

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

(defalias nil? core/nil?)

#_(defnt ^boolean not'
  ([^boolean? x] (Numeric/not x))
  ([x] (if (nil? x) true))) ; Lisp nil punning

#_(defnt ^boolean true?
  ([^boolean? x] x)
  ([:else     x] false))

(defalias true? core/true?)

#_(defnt ^boolean false?
  ([^boolean? x] (not' x))
  ([:else     x] false))

(defalias false? core/false?)

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

(defmacro bit-shift-left  [n bits]
  `(Numeric/shiftLeft ~n ~bits))
(defalias << bit-shift-left)

(defmacro bit-shift-right [n bits]
  `(Numeric/shiftRight ~n ~bits))
(defalias >> bit-shift-left)

(defmacro unsigned-bit-shift-right
  "Bit shift right, replace with zeros"
  [n bits]
  `(Numeric/unsignedShiftRight ~n ~bits))
(defalias >>> unsigned-bit-shift-right)

(defn bits
  {:attribution "gloss.data.primitives"}
  [x num-bits]
  (map #(if (pos? (bit-and (bit-shift-left 1 %) x)) 1 0) (range num-bits)))

; ====== ENDIANNESS REVERSAL =======

(defnt reverse
  (^short  [^short  x] (Numeric/reverseShort x))
  (^int    [^int    x] (Numeric/reverseInt x))
  (^long   [^long   x] (Numeric/reverseLong x)))
