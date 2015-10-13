(ns
  ^{:doc "Logic-related functions. nnil?, nempty?, fn-not, fn-and, splice-or,
          ifn, whenf*n, compr, fn->, condpc, and the like. Extremely useful
          and used everywhere in the quantum library."
    :attribution "Alex Gunnarson"}
  quantum.core.cljs.logic
  (:require [quantum.core.logic :as log]))

; Variants which don't auto-extern arguments
#?(:clj (defmacro fn-or  [& preds] `(log/fn-logic-base :cljs or  ~@preds)))
#?(:clj (defmacro fn-and [& preds] `(log/fn-logic-base :cljs and ~@preds)))
#?(:clj (defmacro fn-not [pred]    `(log/fn-logic-base :cljs not ~pred)))