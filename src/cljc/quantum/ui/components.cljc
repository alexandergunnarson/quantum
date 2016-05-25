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
                       :refer [for fori join kmap reduce]])))  

; COLUMN

#_(defn column-factory [f]
  (fx/callback [^TableColumn$CellDataFeatures p]
    (SimpleStringProperty. (-> p (.getValue) (f) str))))

#_(defn default-column
  ([states ^FXObservableAtom data width text getter validators transformers not-editable?]
    (default-column states ^FXObservableAtom data width text getter validators transformers not-editable? nil))
  ([states ^FXObservableAtom data width text getter validators transformers
    not-editable? {:keys [editable?] :as opts}]
    (let [transformer (or (get @transformers getter) identity)
          validator   (or (get @validators   getter) identity)]
      [:table-column
        {:on-edit-commit
           (fx/event-handler [^TableColumn$CellEditEvent e]
             (let [row-index (-> e (.getTablePosition) (.getRow))
                   new-value (-> e (.getNewValue) validator)]
               (rev/oswap! states true data update row-index
                 (f*n assoc getter new-value))))
         :pref-width         width
         :editable           (whenc editable? nil? (not (get @not-editable? getter)))
         :cell-factory       (TextFieldTableCell/forTableColumn)
         :text               text
         :cell-value-factory (column-factory (compr getter transformer))}])))


#_(defn gen-columns [data data-k fields states width-map
                   validators transformers not-editable?
                   uneditable-map]
  (for [field fields]
    (let [width (or (get width-map field) 80)]
      (default-column states data width
         (-> field str/unkeywordize str/upper-case) field
         validators transformers
         not-editable? {:editable? (not (get-in uneditable-map [data-k field]))}))))

; TABLE VIEW

#_(defn table-view
  ([data columns] (table-view data columns nil))
  ([data columns opts]
    [:table-view
      (mergel opts
        {:pref-width   800
         :pref-height  1000
         :items        (:observable data)
         :editable true
         :columns columns})]))

#_(defn command-ribbon [opts & buttons]
  (let [spacer [:h-box.spacer {:min-width (or (:spacer-width opts) 0)}]]
    (->> buttons
         (reducei
           (fn [container btn n]
             (conj container
               [:button.std (-> {:text (str/upper-case (:text btn))}
                                (mergel btn)
                                (mergel
                                  {:h-box/margin (Insets. 5 0 5 0)
                                   :on-mouse-released
                                     (fx/event-handler [e]
                                       ((or (:handler btn) fn-nil)))})
                                (dissoc :handler))]
               spacer))
           [:h-box.ribbon (dissoc opts :spacer-width) spacer]))))

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

(defn render-db
  "A component which renders a DataScript or Datomic DB."
  [db]
  (fn []
    [table
      (reaction (->> @db db/db->seq (group-by :e) seq))
      (reaction [:a :v :added])]))