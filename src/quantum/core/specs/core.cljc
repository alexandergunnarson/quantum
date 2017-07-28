(ns quantum.core.specs.core)

(defmacro ->
  ("Anything that is coercible to x"
    [x]
    ...)
  ("Anything satisfying `from` that is coercible to `to`.
    Will be coerced to `to`."
    [from to]))

(defmacro or)

(defmacro and)

(defmacro range-of)

(defn instance? [])

(defn ?
  "'Maybe': nilable"
  [x])
