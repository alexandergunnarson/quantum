(ns quantum.test.core.meta.debug
  (:require [quantum.core.meta.debug :as ns]))

; ===== BREAKPOINTS =====

(defn test:break
  [break-enable-sym])

; ===== ERROR TRACING =====

(defn test:cause-trace
  [exception])

(defn test:this-fn-name
  ([])
  ([i]))

(defn test:rescue
  [form error-form])
