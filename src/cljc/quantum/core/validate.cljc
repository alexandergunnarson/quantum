(ns quantum.core.validate
  (:refer-clojure :as core :exclude [string? keyword? set? number? fn? assert])
  (:require
    [#?(:clj  clojure.spec
        :cljs cljs.spec   )   :as s]
    [quantum.core.macros.core
      :refer        [#?@(:clj [if-cljs locals])]
      :refer-macros [locals]]
    [quantum.core.vars        :as var
      :refer        [#?@(:clj [defalias defmalias])]
      :refer-macros [defalias defmalias]]))

(s/check-asserts true) ; TODO put this somewhere like a component

(defrecord ValidationError
  [problems failure at-line at-instant form locals invalidated invalidated-type])

(defn validate* ; TODO handle errors
  "Like |clojure(script).spec/assert*|, but adds a more detailed
   error message à la |taoensso.truss/have|. Also avoids printing
   the failed value to a string, which can be problematic with e.g.
   large collections or lazy seqs."
  [spec x form locals ns-str ?line]
  (if (s/valid? spec x)
      x
      (let [inst        #?(:clj  (java.util.Date.)
                           :cljs (js/Date.))
            explained   (s/explain-data* spec [] [] [] x)
            data        (ValidationError.
                          (::s/problems explained)
                          :assertion-failed
                          (str ns-str ":" (or ?line "?"))
                          inst
                          form
                          locals
                          x
                          (type x))]
        (throw (ex-info "Spec assertion failed" data)))))

#?(:clj
(defmacro validate [spec x]
  (if-cljs &env
   `(if cljs.spec/*compile-asserts*
        (if cljs.spec/*runtime-asserts*
            (validate* ~spec ~x '(validate ~spec ~x) (locals ~&env) ~(str *ns*) ~(:line (meta &form)))
           ~x)
       ~x)
    (if clojure.spec/*compile-asserts*
       `(if clojure.lang.RT/checkSpecAsserts
            (validate* ~spec ~x '(validate ~spec ~x) (locals ~&env) ~(str *ns*) ~(:line (meta &form)))
           ~x)
        x))))

#?(:clj (quantum.core.vars/defmalias spec clojure.spec/spec cljs.spec/spec))
