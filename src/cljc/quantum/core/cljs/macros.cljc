(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.cljs.macros
  (:require-quantum [ns log pr err map set logic fn])
  (:require
    [quantum.core.type.core :as tcore]
    [clojure.string         :as str  ]
    [clojure.walk :refer [postwalk]]
    [quantum.core.macros]))

#?(:clj 
(defmacro defnt [& args]
  `(quantum.core.macros/defnt*-helper :cljs ~@args)))