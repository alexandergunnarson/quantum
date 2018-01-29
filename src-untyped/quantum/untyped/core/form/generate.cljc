(ns quantum.untyped.core.form.generate
  "For code generation."
  (:require
    [quantum.untyped.core.core :as ucore]))

(ucore/log-this-ns)

(defmulti generate
  "Generates code according to the first argument, `kind`."
  (fn [kind _] kind))

(defn ?wrap-do [codelist]
  (if (-> codelist count (< 2))
      (first codelist)
      (list* 'do codelist)))
