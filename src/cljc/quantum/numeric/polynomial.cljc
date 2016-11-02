(ns quantum.numeric.polynomial)

(defn value
  "Evaluate a polynomial at the given value x, for the coefficients given in
   descending order (so the last element of coefficients is the constant term)."
  {:source 'criterium.stats
   :todo ["Rework this to not use laziness"]}
  [x coefficients]
  (reduce #(+ (* x %1) %2) (first coefficients) (rest coefficients)))
