(ns quantum.core.string.find
  (:require-quantum [:core]))

; TODO pair with validation?
; TODO uncomment
#_(defn find-email [possible-email-string]
  (let [non-spaces "([A-Za-z_0-9]+)"
        pattern (str "(" non-spaces "@" non-spaces "\\." non-spaces ")+")]
     (str/re-find-all pattern possible-email-string)))