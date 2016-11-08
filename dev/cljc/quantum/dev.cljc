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
    [quantum.core.numeric                 :as num]
    [quantum.core.fn                      :as fn]
    [quantum.core.error                   :as err
      :refer [->ex]]
    [quantum.core.log                     :as log]
    [quantum.core.logic]
    [quantum.core.resources               :as res]
    [quantum.core.string                  :as str]
    [quantum.core.process                 :as proc]
    [quantum.net.websocket                :as conn]
    [quantum.core.convert                 :as conv]
    [quantum.core.io.core                 :as io]
    [quantum.core.paths                   :as path]
    [quantum.db.datomic                   :as db]
    [quantum.db.datomic.core              :as dbc]
    [quantum.apis.amazon.cloud-drive.auth :as amz-auth]
    [quantum.auth.core                    :as auth]
    [quantum.net.http                     :as http]
    [quantum.security.core                :as sec]
    [quantum.security.cryptography        :as crypto]
    [quantum.system                       :as gsys]
    [quantum.core.data.complex.xml        :as xml]
    [quantum.core.ns                      :as ns]
    [quantum.system                       :as gsys]
    [quantum.core.time.core               :as time]
    [quantum.core.print                   :as pr
      :refer        [#?(:clj !)]
      :refer-macros [!]]))

#?(:cljs (enable-console-print!))
(println "Hey console!")

#?(:clj (clojure.spec/check-asserts true))
