(ns quantum.ui.interaction.input)

#?(:cljs
(def key->code
  {:enter 13
   :esc   27} ))

#?(:cljs
(def code->key
  (zipmap (vals key->code)
          (keys key->code))))