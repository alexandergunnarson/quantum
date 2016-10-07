(ns quantum.core.validate
  (:refer-clojure :exclude
    [string? keyword? set? number? fn?
     assert keys + * cat and or])
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core   )   :as core]
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

(def spec-assertion-failed "Spec assertion failed")

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
                          (str ns-str ":" (core/or ?line "?"))
                          inst
                          form
                          locals
                          x
                          (type x))]
        (throw (ex-info spec-assertion-failed data)))))

#?(:clj
(defmacro validate [spec x]
  (if-cljs &env
   `(if cljs.spec/*compile-asserts*
        (if cljs.spec/*runtime-asserts*
            (validate* ~spec ~x '(validate ~spec ~x) (locals ~&env) ~(str *ns*) ~(:line (meta &form)))
           ~x)
       ~x)
    (if clojure.spec/*compile-asserts*
       `(if (clojure.spec/check-asserts?) #_clojure.lang.RT/checkSpecAsserts
            (validate* ~spec ~x '(validate ~spec ~x) (locals ~&env) ~(str *ns*) ~(:line (meta &form)))
           ~x)
        x))))
#?(:clj
(defmacro validate-all
  "Multiple validations performed.
   Keys are values; vals are specs.
   Useful for :pre checks."
  [& args]
  `(do ~@(->> args (partition-all 2)
                   (map (fn [[v spec]] `(validate ~spec ~v)))))))

#?(:clj (quantum.core.vars/defmalias spec clojure.spec/spec cljs.spec/spec))
#?(:clj (quantum.core.vars/defmalias coll-of clojure.spec/coll-of cljs.spec/coll-of))

#?(:clj (quantum.core.vars/defmalias defspec clojure.spec/def  cljs.spec/def ))
#?(:clj (quantum.core.vars/defmalias keys    clojure.spec/keys cljs.spec/keys))
#?(:clj (quantum.core.vars/defmalias alt     clojure.spec/alt  cljs.spec/alt ))
#?(:clj (quantum.core.vars/defmalias cat     clojure.spec/cat  cljs.spec/cat ))
#?(:clj (quantum.core.vars/defmalias +       clojure.spec/+    cljs.spec/+   ))
#?(:clj (quantum.core.vars/defmalias *       clojure.spec/*    cljs.spec/*   ))
#?(:clj (quantum.core.vars/defmalias ?       clojure.spec/?    cljs.spec/?   ))
#?(:clj (quantum.core.vars/defmalias and     clojure.spec/and  cljs.spec/and ))
#?(:clj (quantum.core.vars/defmalias or      clojure.spec/or   cljs.spec/or  ))

#?(:clj
(defmacro or*
  "Like `spec/or`, except names aren't required for preds."
  [& preds]
  `(or ~@(->> preds (map #(vector (keyword (str %)) %)) (apply concat)))))
