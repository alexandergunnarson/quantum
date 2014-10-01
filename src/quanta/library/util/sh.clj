(ns quanta.library.util.sh)

; (clojure.java.shell/sh & args)
(defn exec [^String cmd]
  "Execute the provided command"
  ^{:attribution "thebusby.bagotricks"}
  (-> (Runtime/getRuntime)
      (.exec cmd)))