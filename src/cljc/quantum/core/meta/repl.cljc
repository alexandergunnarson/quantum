(ns quantum.core.meta.repl
  (:require-quantum [reg var])
  #?(:clj
  (:require [clojure.repl         :as repl]
            [clojure.java.javadoc         ])))

; |find-doc|, |doc|, and |source| are incl. in |user| ns but apparently not in any others
; ; TODO: Possibly find a way to do this in ClojureScript?
#?(:clj (defalias source   repl/source  ))
#?(:clj (defalias find-doc repl/find-doc)) ; searches in clojure function names and docstrings!
#?(:clj (defalias doc      repl/doc     ))
#?(:clj (defalias javadoc  clojure.java.javadoc/javadoc))