(ns
  ^{:doc "Useful function-related functions (one could say 'metafunctions').

          Higher-order functions, currying, monoids, reverse comp, arrow macros, inner partials, juxts, etc."
    :attribution "Alex Gunnarson"}
  quantum.core.cljs.deps.function)

#?(:clj
(defmacro f*n  [func & args]
  `(fn [arg-inner#]
     (~func arg-inner# ~@args))))