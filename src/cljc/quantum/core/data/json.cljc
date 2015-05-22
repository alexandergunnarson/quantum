(ns
  ^{:doc "A simple JSON library which aliases cheshire.core."
    :attribution "Alex Gunnarson"}
  quantum.core.data.json
  (:require-quantum [ns]))

#?(:clj (alias-ns 'cheshire.core)) ; for now

; 2.888831 ms for Cheshire (on what?) vs. clojure.data.json : 7.036831 ms