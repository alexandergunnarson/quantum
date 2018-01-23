(ns quantum.untyped.form.generate
  "For code generation.")

(defmulti generate
  "Generates code according to the first argument, `kind`."
  (fn [kind _] kind))
