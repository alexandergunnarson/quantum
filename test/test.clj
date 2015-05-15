(ns test
  (:require
    [clojure.tools.namespace.repl :refer [refresh refresh-all set-refresh-dirs]]
    [quantum.core.ns :as ns]))

;(ns/require :clj :lib)
(ns/require-all *ns* :clj :lib)

(defn init! [] (println "Hey :)"))
; (refresh-all :after 'test/init!)

; (in-ns 'clojure.tools.namespace.parse)
; (defn read-ns-decl
;   "Attempts to read a (ns ...) declaration from a
;   java.io.PushbackReader, and returns the unevaluated form. Returns
;   the first top-level ns form found. Returns nil if read fails or if a
;   ns declaration cannot be found. Note that read can execute code
;   (controlled by *read-eval*), and as such should be used only with
;   trusted sources."
;   [rdr]
;   {:pre [(instance? java.io.PushbackReader rdr)]}
;   (try
;    (loop []
;      (let [form (doto (read {:read-cond :allow} rdr) str)]  ; str forces errors, see TNS-1
;        (if (ns-decl? form)
;          form
;          (recur))))
;    (catch Exception e nil)))
; (in-ns 'test)