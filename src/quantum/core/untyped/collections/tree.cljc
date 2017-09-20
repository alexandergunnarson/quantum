(ns quantum.core.untyped.collections.tree
  (:require
    [quantum.core.error :as err
      :refer [TODO]]
    [quantum.core.fn
      :refer [aritoid]]
    [quantum.core.untyped.reducers
      :refer [map+ into!]]
    [quantum.core.vars :as var
      :refer [defalias]]))

; ----- WALK ----- ;

(defn walk
  "Like `clojure.walk`, but ensures preservation of metadata."
  [inner outer form]
  (cond
              (list?      form) (outer (replace-meta-from (apply list (map inner form))                    form))
    #?@(:clj [(map-entry? form) (outer (replace-meta-from (vec        (map inner form))                    form))])
              (seq?       form) (outer (replace-meta-from (doall      (map inner form))                    form))
              (record?    form) (outer (replace-meta-from (reduce (fn [r x] (conj r (inner x))) form form) form))
              (coll?      form) (outer (replace-meta-from (into (empty form) (map inner form))             form))
              :else (outer form)))

(defn postwalk [f form] (walk (partial postwalk f) f form))
(defn prewalk  [f form] (walk (partial prewalk  f) identity (f form)))

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
  (TODO))
