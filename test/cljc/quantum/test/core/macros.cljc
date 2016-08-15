(ns quantum.test.core.macros
  (:require [quantum.core.macros :as ns]))

(defn test:maptemplate
  [template-fn coll])

(defn test:let-alias* [bindings body])

(defn test:let-alias
  [bindings & body])

(defn test:var->symbol [x])

(defn test:qualify [x])

(defn test:deftransmacro
  [name clj-fn cljs-fn])

(defn test:variadic-proxy
  ([name clj-fn & [cljs-fn clj-single-arg-fn cljs-single-arg-fn]]))

(defn test:variadic-predicate-proxy
  ([name clj-fn & [cljs-fn clj-single-arg-fn cljs-single-arg-fn]]))

(defn test:env [])

(defn test:assert-args [fn-name & pairs])

(defn test:emit-comprehension
  [&form {:keys [emit-other emit-inner]} seq-exprs body-expr])

(defn test:do-mod [mod-pairs cont & {:keys [skip stop]}])