(ns quantum.test.core.macros.reify
  (:require [quantum.core.macros.reify :as ns]))

(defn test:gen-reify-def
  [{:keys [sym ns-qualified-interface-name reify-body]}])

(defn test:gen-reify-body-raw
  [{:keys [ns-qualified-interface-name
           genned-method-name
           gen-interface-code-body-expanded]}])

(defn test:verify-reify-body [reify-body sym])

(defn test:gen-reify-body
  [{:as args
    :keys [sym
           ns-qualified-interface-name
           genned-method-name
           gen-interface-code-body-expanded]}])