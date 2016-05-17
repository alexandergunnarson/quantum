(ns
  ^{:doc "Useful set-related functions. Includes a dispatch function, |xset?|, 
          from which |subset|, |superset|, |proper-subset?|, and so on may be called."
    :attribution "Alex Gunnarson"}
  quantum.core.data.set
           (:require [clojure.set              :as set  ]
                     [clojure.data.avl         :as avl  ]
                     [quantum.core.vars        :as var
                       :refer [#?(:clj defalias)]       ]
                     [quantum.core.error       :as err 
                       :refer [->ex]                    ]
           #?@(:clj [[clojure.data.finger-tree :as ftree]
                     [flatland.ordered.set     :as oset ]
                     [seqspert.hash-set                 ]
                     [clojure.data.int-map     :as imap ]]))
  #?(:cljs (:require-macros
                     [quantum.core.vars        :as var
                        :refer [defalias]               ])))

; ============ STRUCTURES ============

#?(:clj (defalias ordered-set  oset/ordered-set))
#?(:clj (defalias c-sorted-set ftree/counted-sorted-set)) ; sorted set that provides log-n nth
(defalias sorted-set+    avl/sorted-set)
(defalias sorted-set-by+ avl/sorted-set-by)

#?(:clj (defalias int-set       imap/int-set))
#?(:clj (defalias dense-int-set imap/dense-int-set))

#?(:clj (def hash-set? (partial instance? clojure.lang.PersistentHashSet)))

; ============ PREDICATES ============

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

; ============ OPERATIONS ============

; TODO use |clojure.data.int-map/union, intersection, difference| for int sets and dense int sets
; Benchmark these
#?(:clj
    (defn union
      "337.050528 msecs (core/union s1 s2)
       158.255666 msecs (seqspert.hash-set/sequential-splice-hash-sets s1 s2)))
       This is superseded by quantum.core.reducers.reduce/join."
      ([] (hash-set))
      ([s0] s0)
      ([s0 s1]
        ; To avoid NullPointerException
        (cond (nil? s0) s1
              (nil? s1) s0
              (and (hash-set? s0) (hash-set? s1))
              (seqspert.hash-set/sequential-splice-hash-sets s0 s1)
              :else (set/union s0 s1)))
      ([s0 s1 & ss]
        (reduce union (union s0 s1) ss)))
   :cljs (defalias union set/union))

#?(:clj
(defn punion
  "337.050528 msecs (core/union s1 s2)
   28.837984  msecs (seqspert.hash-set/parallel-splice-hash-sets s1 s2)))
   This is superseded by quantum.core.reducers.fold/pjoin."
  ([] (hash-set))
  ([s0] s0)
  ([s0 s1]
    (cond (nil? s0) s1
          (nil? s1) s0
          (and (hash-set? s0) (hash-set? s1))
            (#?(:clj  seqspert.hash-set/parallel-splice-hash-sets
                :cljs seqspert.hash-set/sequential-splice-hash-sets) s0 s1)
          :else (throw (->ex nil "Could not perform parallel union; can try sequential."))))
  ([s0 s1 & ss]
    (reduce punion (punion s0 s1) ss))))

(defalias intersection set/intersection)
(defalias difference   set/difference  )