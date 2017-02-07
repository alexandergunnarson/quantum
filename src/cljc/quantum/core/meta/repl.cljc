(ns quantum.core.meta.repl
  (:require [quantum.core.vars    :as var
              :refer [#?(:clj defalias)]]
  #?@(:clj [[clojure.repl         :as repl]
            [clojure.java.javadoc         ]])))

; ; TODO: Possibly find a way to do this in ClojureScript?
#?(:clj (defalias source   repl/source  ))
#?(:clj (defalias find-doc repl/find-doc))
#?(:clj (defalias doc      repl/doc     ))
#?(:clj (defalias javadoc  clojure.java.javadoc/javadoc))
