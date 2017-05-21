(ns quantum.test.core.macros.transform
  (:require [quantum.core.macros.transform :as ns]))

(defn test:hint-resolved? [x lang env])

(defn test:any-hint-unresolved?
  ([args lang])
  ([args lang env]))


(defn test:hint-body-with-arglist
  ([body arglist lang])
  ([body arglist lang body-type]))

(defn test:extract-type-hints-from-arglist
  [lang arglist])

(defn test:extract-all-type-hints-from-arglist
  [lang sym arglist])

(defn test:try-hint-args
  ([args lang])
  ([args lang env]))

(defn test:gen-arglist
  [v])
