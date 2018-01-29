(ns quantum.untyped.core.form.evaluate
  (:refer-clojure :exclude
    [macroexpand macroexpand-1])
  (:require
#?@(:clj
   [[cljs.analyzer]
    [clojure.jvm.tools.analyzer]
    [clojure.jvm.tools.analyzer.hygienic]
    [clojure.tools.analyzer.jvm]
    [riddley.walk]])
    [clojure.core              :as core]
    [clojure.core.reducers     :as r]
    [quantum.untyped.core.core :as ucore
      :refer [defaliases]])
#?(:cljs
  (:require-macros
    [quantum.untyped.core.form.evaluate :as self
      ])))

(ucore/log-this-ns)

;; ===== Environment ===== ;;

