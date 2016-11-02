(ns quantum.numeric.symbolic)

; TO EXPLORE
; - Mathematica
;   - Symbolic computation
; ==========================

(defrecord Sqrt [n])

(defn sqrt [x] (Sqrt. x))
