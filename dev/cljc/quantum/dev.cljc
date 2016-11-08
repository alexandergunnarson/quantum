#_(do (require '[clojure.tools.namespace.repl :refer [refresh clear]])
            #_(clear)
            (refresh))

(ns quantum.dev
  (:refer-clojure :exclude [reduce for])
  (:require
    [clojure.core                         :as core]
 #?@(:clj
   [[clojure.tools.namespace.repl
      :refer [refresh]]
    [quantum.net.server.middleware        :as mid]])
    [quantum.core.collections             :as coll]
    [quantum.core.error                   :as err
      :refer [->ex]]
    [quantum.core.fn                      :as fn]
    [quantum.core.convert                 :as conv]
    [quantum.core.data.complex.xml        :as xml]
    [quantum.core.log                     :as log]
    [quantum.core.logic]
    [quantum.core.ns                      :as ns]
    [quantum.core.numeric                 :as num]
    [quantum.core.process                 :as proc]
    [quantum.core.resources               :as res]
    [quantum.core.string                  :as str]
    [quantum.net.websocket                :as conn]

    [quantum.core.io.core                 :as io]
    [quantum.core.paths                   :as path]
    [quantum.core.print                   :as pr
      :refer        [!]]
    [quantum.core.time.core               :as time]
    [quantum.db.datomic                   :as db]
    [quantum.db.datomic.core              :as dbc]
    [quantum.apis.amazon.cloud-drive.auth :as amz-auth]
    [quantum.auth.core                    :as auth]
    [quantum.net.http                     :as http]
    [quantum.security.core                :as sec]
    [quantum.security.cryptography        :as crypto]
    [quantum.system                       :as gsys]))

#?(:cljs (enable-console-print!))
(println "Hey console!")

#?(:clj (clojure.spec/check-asserts true))
