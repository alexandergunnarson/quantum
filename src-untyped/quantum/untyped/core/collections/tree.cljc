(ns quantum.untyped.core.collections.tree
  (:require
    [quantum.untyped.core.collections :as ucoll]
    [quantum.untyped.core.core        :as ucore]
    [quantum.untyped.core.reducers    :as ur]))

(ucore/log-this-ns)

; ----- WALK ----- ;

(def walk     ucore/walk)
(def postwalk ucore/postwalk)
(def prewalk  ucore/prewalk)

;; TODO `prewalk-fold`
(defn postwalk-fold
  "Performs a fold-like operation on a tree.
   May or may not be for side effects.
   `branch?f` and `childrenf` are like `tree-seq`'s `branch?` and `children`.
   `rf` and `cf` are like `fold`'s `rf` and `cf`. The elements fed into `rf` are the nodes."
  {:attribution 'alexandergunnarson}
  [rf cf branch?f childrenf root]
  (let [walk (fn walk [node nodes]
               (if (branch?f node)
                   (rf (->> node childrenf (ucoll/map+ #(rf (walk % (rf)))) (reduce cf nodes))
                       node)
                   nodes))]
    (cf (walk root (cf)))))

;; TODO `tree-sequence-prewalk`
(defn tree-sequence-postwalk
  "Walks the tree and outputs an eager sequence containing its nodes ordered from leaf to
   branch (i.e. postorder).
   `childrenf` must return a reducible."
  {:attribution 'alexandergunnarson}
  [branch?f childrenf root]
  (postwalk-fold (fn ([] (transient [])) ([x] (persistent! x)) ([xs x] (conj!    xs x)))
                 (fn ([] (transient [])) ([x] (persistent! x)) ([a  b] (ur/into! a  b)))
                 branch?f childrenf root))

(defn prewalk-find
  "Returns true if ->`x` appears within ->`coll` at any nesting depth."
  {:adapted-from "scgilardi/slingshot"
   :contributors ["Alex Gunnarson"]}
  [pred coll]
  (let [result (atom [false nil])]
    (try
      (prewalk
        (fn [x]
          (if (pred x) ; TODO fix — if there's an exception then this will misleadingly say it's not found instead of propagating the exception
              (do (reset! result [true x])
                  (throw #?(:clj (Exception.) :cljs (js/Error.))))
              x))
        coll)
      @result
      (catch #?(:clj Exception :cljs js/Error) _ @result))))

(defn postwalk-find
  [pred coll]
  (throw (ex-info "TODO" {})))
