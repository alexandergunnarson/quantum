(ns 
  ^{:doc "Useful binary operations."
    :attribution "Alex Gunnarson"}
  quantum.core.data.binary)

(defn >>>
  "Bit shift right, replace with zeros"
  {:attribution "Alex Gunnarson"}
  [v bits]
  (-> 0xFFFFFFFF (bit-and v) (bit-shift-right bits)))