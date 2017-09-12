(ns quantum.core.untyped.collections.tree
  (:require
    [quantum.core.fn
      :refer [aritoid]]
    [quantum.core.untyped.reducers
      :refer [map+ into!]]))

; TODO move
; TODO `prewalk-fold`
(defn postwalk-fold
  "Performs a fold-like operation on a tree.
   May or may not be for side effects.
   `branch?f` and `childrenf` are like `tree-seq`'s `branch?` and `children`.
   `rf` and `cf` are like `fold`'s `rf` and `cf`. The elements fed into `rf` are the nodes."
  {:attribution 'alexandergunnarson}
  [rf cf branch?f childrenf root]
  (let [walk (fn walk [node nodes]
               (if (branch?f node)
                   (rf (->> node childrenf (map+ #(rf (walk % (rf)))) (reduce cf nodes))
                       node)
                   nodes))]
    (cf (walk root (cf)))))

; TODO move
; TODO `tree-sequence-prewalk`
(defn tree-sequence-postwalk
  "Walks the tree and outputs an eager sequence containing its nodes ordered from leaf to
   branch (i.e. postorder).
   `childrenf` must return a reducible."
  {:attribution 'alexandergunnarson}
  [branch?f childrenf root]
  (postwalk-fold (aritoid #(transient []) persistent! conj!)
                 (aritoid #(transient []) persistent! into!)
                 branch?f childrenf root))
