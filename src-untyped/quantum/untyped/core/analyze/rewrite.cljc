(ns quantum.untyped.core.analyze.rewrite
  (:require
    [quantum.untyped.core.core :as ucore]))

(ucore/log-this-ns)

(defn remove-do [code]
  (if (and (seq? code) (-> code first (= 'do)))
      (rest code)
      code))
