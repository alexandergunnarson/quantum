(ns
  ^{:doc "Some useful macros, like de-repetitivizing protocol extensions.
          Also some plumbing macros for |for| loops and the like."
    :attribution "Alex Gunnarson"}
  quantum.core.cljs.macros
  (:require-quantum [ns log pr err map set logic fn])
  (:require
    [quantum.core.macros]))

#?(:clj 
(defmacro defnt [& args]
  `(quantum.core.macros/defnt*-helper :cljs ~@args)))