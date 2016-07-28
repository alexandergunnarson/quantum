(ns quantum.test.db.datomic
  (:require [quantum.db.datomic :refer :all]
            [quantum.core.io :as io]))
; CORE FUNCTIONS

#?(:cljs
(defn test:rx-q
  ([query])
  ([query conn & args])))

#?(:cljs
(defn test:rx-pull
  ([selector eid])
  ([conn selector eid])))

#?(:cljs
(defn test:rx-transact!
  ([tx-data]     )
  ([conn tx-data])))

(defn test:init-schemas!
  ([schemas])
  ([conn schemas]))

; RECORDS / RESOURCES (TODO: MOVE)

(defn test:->ephemeral-db
  [{:keys [history-limit] :as config}])

#?(:clj
(defn test:start-transactor!
  [{:keys [type host port]}
   {:keys [kill-on-shutdown? datomic-path flags resources-path internal-props]
    :as   txr-props}]))

(defn test:->backend-db
  [{:keys [type name host port txr-alias create-if-not-present?]
    :as config}])

(defn test:->db
  [{:keys [backend reconciler ephemeral] :as config}])

(defmethod io/test:persist! EphemeralDatabase
  [_ persist-key
   {:keys [db history] :as persist-data}
   {:keys [schema]     :as opts        }])