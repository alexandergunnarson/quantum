(ns quantum.core.data.binary)

(defn >>>
  "Bit shift right, replace with zeros"
  [v bits]
  (bit-shift-right (bit-and 0xFFFFFFFF v) bits))