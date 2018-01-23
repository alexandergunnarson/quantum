(ns quantum.untyped.core.loops
  (:require
    [quantum.untyped.core.reducers :as r]))

(defn reduce-2
  "Reduces over two seqables at a time."
  {:todo #{"`defnt` this and have it dispatch to e.g. reduce-2:indexed"}}
  ([f xs0 xs1] (reduce-2 f nil xs0 xs1))
  ([f init xs0 xs1] (reduce-2 f init xs0 xs1 false))
  ([f init xs0 xs1 assert-same-count?]
    (loop [ret init xs0' xs0 xs1' xs1]
      (cond (reduced? ret)
            @ret
            (or (empty? xs0') (empty? xs1'))
            (do (when (and assert-same-count?
                           (or (and (empty? xs0') (seq    xs1'))
                               (and (seq    xs0') (empty? xs1'))))
                  (throw (ex-info "Seqables are not the same count" {})))
                ret)
            :else (recur (f ret (first xs0') (first xs1'))
                         (next xs0')
                         (next xs1'))))))
