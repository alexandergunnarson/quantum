(ns 
  ^{:doc "Useful binary operations."
    :attribution "Alex Gunnarson"}
  quantum.core.data.binary)

(def >> bit-shift-right)
(def << bit-shift-left )
; What about the other bit shifts?
; bit-and &
; bit-clear
; bit-and-not
; bit-xor
; bit-test
; bit-flip
; bit-not
; bit-or  |
; bit-set

(defn >>>
  "Bit shift right, replace with zeros"
  {:attribution "Alex Gunnarson"}
  [v bits]
  (-> 0xFFFFFFFF (bit-and v) (bit-shift-right bits)))