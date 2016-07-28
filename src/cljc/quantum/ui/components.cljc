(ns
  ^{:doc "Namespace for creating/defining UI components, especially
          with |defcomponent|."
    :attribution "Alex Gunnarson"}
  quantum.ui.components
           (:refer-clojure :exclude [for reduce])
           (:require [quantum.ui.revision :as rev]
                     [quantum.core.fn :as fn
                       :refer [#?@(:clj [fn-> fn->> <-])]]
                     [quantum.db.datomic :as db]
                     [quantum.core.collections :as coll
                       :refer [#?@(:clj [for fori join kmap reduce])]])
  #?(:cljs (:require-macros
                     [quantum.core.fn          :as fn
                       :refer [fn-> fn->> <-]           ]
                     [quantum.core.collections :as coll
                       :refer [for fori join kmap reduce]]
                     [reagent.ratom
                       :refer [reaction]])))  

(defn table
  "An HTML table component.
   Expects rows to be indexed or grouped in some way."
  {:usage `[table (reaction {1 {:a 1 :b 2 :c 3}
                             2 {:a 4 :b 5 :c 6}})
                  (reaction [:a :b :c])]}
  [data col-getters]
  (fn table* []
    (let [gen-rows (fn [v split-fn k-display]
                     (for [d (split-fn v)]
                       (join [:tr [:td k-display]]
                         (for [getter @col-getters]
                           [:td (-> d getter pr-str)]))))]
      (join [:table]
        (for [[k v] @data]
          (join (first (gen-rows v (fn-> first vector) k  ))
                       (gen-rows v rest                nil)))))))

#?(:cljs
(defn render-db
  "A component which renders a DataScript or Datomic DB."
  [db]
  (fn []
    [table
      (reaction (->> @db db/db->seq (group-by :e) seq))
      (reaction [:a :v :added])])))