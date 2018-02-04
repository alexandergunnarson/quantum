Next steps

#_"
- IMMEDIATE
  - Handle case where body needs to be analyzed when there is a generated primitive reify overload
  - Will not handle/allow function-wide output type `>` right after name-sym, in addition to
    just arity-specific output types. We could do a t/and* (unordered `and`), but this would break the idea
    of having each overload's spec be self-contained. So we will support either one, but not both.
  - each `defnt` definition needs a lookup mechanism to ensure that calls to the function are routed to the correct
    reify
- MUCH LATER
  - generate Clojure specs for each function that requires runtime specs to be applied, so that function can
    have generative tests semi-subsume those specs being run in production, and thereby obviate the associated
    performance hit

#?(:clj
(defn spec>reify-classes [spec]
  (let [classes (t/spec>classes spec)]
    (if (contains? classes Object)
        (join classes primitive-classes) ; TODO possibly have an option to include primitives or not
        classes))))

- Keep going with porting quantum.core.core to typed Clojure, keeping in mind CLJS
"

