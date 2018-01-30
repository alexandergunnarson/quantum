(ns quantum.core.macros.type-hint
  (:require
    [quantum.untyped.core.form.type-hint :as u]
    [quantum.untyped.core.vars
      :refer [defaliases]]))

(defaliases u
  type-hint with-type-hint un-type-hint
  tag sanitize-tag with-sanitize-tag
  #?@(:clj [?symbol->class ?tag->class tag->class class->str class->symbol])
  #?(:clj type-hint|class) type-hint|sym
  fn-safe-type-hints-map
  #?@(:clj [class->instance?-safe-tag|sym ->fn-arglist-tag with-fn-arglist-type-hint])
  >body-embeddable-tag >arglist-embeddable-tag)
