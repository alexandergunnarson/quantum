(ns quanta.library.data.set(:gen-class))
(require
  '[quanta.library.ns               :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[clojure.set              :as set]
  '[clojure.data.finger-tree :as ftree]
  '[clojure.data.avl         :as avl]
  '[flatland.ordered.set     :as oset])
;   (:refer-clojure :exclude [sorted-set sorted-set-by])
(defalias union          set/union)
(defalias intersection   set/intersection)
(defalias difference     set/difference)
(defalias ordered-set    oset/ordered-set)
(defalias c-sorted-set   ftree/counted-sorted-set) ; sorted set that provides log-n nth
(defalias sorted-set+    avl/sorted-set)
(defalias sorted-set-by+ avl/sorted-set-by)

(defn xset? [fn-key set1 set2]
  (let [funcs 
         (case fn-key
           :sub          {:eq <= :fn #(vector (fn [s] (contains? %2 s)) %1)}
           :super        {:eq >= :fn #(vector (fn [s] (contains? %1 s)) %2)}
           :proper-sub   {:eq <  :fn #(vector %2 %1)}
           :proper-super {:eq >  :fn #(vector %1 %2)})]
    (and ((:eq funcs) (count set1) (count set2))
         (apply every? ((:fn funcs) set1 set2)))))
; probably a way to make this more terse, like a "define-vars" macro with intern or something
; multimethod or maybe a protocol
(def subset?          #(xset? :sub          %1 %2))
(def superset?        #(xset? :super        %1 %2))
(def proper-subset?   #(xset? :proper-sub   %1 %2))
(def proper-superset? #(xset? :proper-super %1 %2))