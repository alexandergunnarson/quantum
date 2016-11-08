(ns quantum.core.type.macros)

; macro because it will probably be heavily used
#?(:clj
(defmacro should-transientize? [coll]
  `(and (quantum.core.type/editable? ~coll)
        (quantum.core.type/counted?  ~coll)
        (-> ~coll count (> transient-threshold)))))
