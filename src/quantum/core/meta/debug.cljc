(ns
  ^{:doc "Useful debug utils. Especially `trace`, `break`, etc."
    :attribution "alexandergunnarson"}
  quantum.core.meta.debug
  (:require
    [quantum.core.vars               :as var
      :refer [defaliases]]
    [quantum.untyped.core.meta.debug :as u]))

;; ===== Breakpointing ===== ;;

(defaliases u *breakpoint-types #?(:clj break))

;; ===== Error tracing ===== ;;

(defaliases u
  trace
  #?@(:clj [default-exception-handler print-pretty-exceptions!])
  stack-depth this-fn-name)
