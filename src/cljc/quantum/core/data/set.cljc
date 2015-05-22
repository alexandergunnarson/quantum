(ns
  ^{:doc "Useful set-related functions. Includes a dispatch function, |xset?|, 
          from which |subset|, |superset|, |proper-subset?|, and so on may be called."
    :attribution "Alex Gunnarson"}
  quantum.core.data.set
  (:require-quantum [ns])
  (:require   [clojure.set              :as set]
              [clojure.data.avl         :as avl]
    #?@(:clj [[clojure.data.finger-tree :as ftree]
              [flatland.ordered.set     :as oset ]])))

(def union          set/union)
(def intersection   set/intersection)
(def difference     set/difference)
#?(:clj (def ordered-set  oset/ordered-set))
#?(:clj (def c-sorted-set ftree/counted-sorted-set)) ; sorted set that provides log-n nth
(def sorted-set+    avl/sorted-set)
(def sorted-set-by+ avl/sorted-set-by)

(defn xset?
  {:attribution "Alex Gunnarson"
   :todo ["A cool idea... but improve performance"]}
  [fn-key set1 set2]
  (let [funcs 
         (case fn-key
           :sub          {:eq <= :fn #(vector (partial contains? %2) %1)}
           :super        {:eq >= :fn #(vector (partial contains? %1) %2)}
           :proper-sub   {:eq <  :fn #(vector %2 %1)}
           :proper-super {:eq >  :fn #(vector %1 %2)})]
    (and ((:eq funcs) (count set1) (count set2))
         (apply every? ((:fn funcs) set1 set2)))))

; TODO probably a way to make this more terse, like a "define-vars" macro with intern or something
(def subset?          #(xset? :sub          %1 %2))
(def superset?        #(xset? :super        %1 %2))
(def proper-subset?   #(xset? :proper-sub   %1 %2))
(def proper-superset? #(xset? :proper-super %1 %2))
