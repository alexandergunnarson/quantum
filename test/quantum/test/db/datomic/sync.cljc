(ns quantum.test.db.datomic.sync
  (:require
    [clojure.test
      #?(:clj :refer :cljs :refer-macros) [deftest is testing]]
  #_[quantum.db.datomic.sync :as ns]
    [quantum.db.datomic      :as db]
    [quantum.core.resources  :as res]
    [quantum.core.log        :as log]))

(deftest syncing
  (let [server-config
          {::db/ephemeral {}
           ::log/log      {:level #{:warn ::db/debug}}}
        client-config
          {::db/ephemeral {}
           ::log/log      {:level #{:warn ::db/debug}}}]
    (res/with-resources
      [server (res/start! (res/->system ::server server-config res/default-make-system))
       client (res/start! (res/->system ::client client-config res/default-make-system))]
      ; TODO perform syncing between the two and ensure it all works
      ; Try with websockets; then try
      )))
