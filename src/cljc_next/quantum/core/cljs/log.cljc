(ns
  ^{:doc "ClojureScript versions of macros.
          Annoying that this has to be done this way."}
  quantum.core.cljs.log
  (:refer-clojure :exclude [pr])
  (:require-quantum [ns fn logic])
  (:require
  	[quantum.core.log   :as log]))

#?(:clj
(defmacro pr [pr-type & args]
  `(quantum.core.log/pr* false println ~pr-type (delay (list ~@args))))) ; No ns reporting... but oh well)