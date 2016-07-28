(ns quantum.test.core.macros.protocol
  (:require [quantum.core.macros.protocol :as ns]))

(defn test:ensure-protocol-appropriate-type-hint [arg lang i])

(defn test:ensure-protocol-appropriate-arglist [lang arglist-0])

(defn test:gen-protocol-from-interface
  [{:keys [gen-interface-code-body-expanded
           genned-protocol-name
           genned-protocol-method-name]}])

(defn test:gen-extend-protocol-from-interface
  [{:keys [genned-protocol-name genned-protocol-method-name
           reify-body lang first-types]}])