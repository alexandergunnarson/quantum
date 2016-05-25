(ns quantum.db.subs
          (:require [re-frame.core      :as re   
                      :refer [subscribe dispatch]]
                    [quantum.db.datomic :as db])
 #?(:cljs (:require-macros
             [reagent.ratom :as rx
               :refer [reaction]]
              [quantum.core.fn :as fn
              :refer [<-]])))

(def dom-id 0)

(def subscriptions
  {:db       (fn [db [_ _]] db)
   :q        (fn [db [_ q]] ; yes, it's impure but that's how it has to be for now
               (db/rx-q @db/conn* q))
   :entity   (fn [db [_ eid]]
               (reaction
                 (->> eid
                      db/entity ; yes, it's impure but that's how it has to be for now
                      (into {:db/id eid}))))
   :entities (fn [db [_ q]]
               (reaction
                 (->> @(subscribe [:q q])
                      (mapv (fn [x] @(subscribe [:entity (first x)]))))))
   :dom (fn [db [_ k]]
          (reaction
            (->> dom-id
                 db/entity
                 (into {})
                 (<- get k))))})

(def handlers
  {:transact! (fn [db [_ txn]]
                (db/transact! txn)
                @@db/conn*) ; TODO impure
   :assoc!    (fn [db [_ id k v]]
                (db/transact! [(db/assoc id k v)])
                @@db/conn*)
   :dom!      (fn [db [_ k v]]
                (dispatch
                  [:transact!
                    [(db/assoc dom-id k v)]])
                @@db/conn*)}) ; TODO impure
             