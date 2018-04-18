#_(:clj
(do (require '[clojure.tools.namespace.repl :refer [refresh]])
    (require 'quantum.core.meta.dev)
    (quantum.core.meta.dev/enable-repl-utils!)
    (quantum.core.meta.debug/print-pretty-exceptions!)
    (refresh)))

(ns quantum.dev
  (:refer-clojure :exclude [reduce for])
  (:require
    [clojure.core                         :as core]
 #?@(:clj
   [[clojure.tools.namespace.repl
      :refer [refresh]]
    [quantum.core.collections             :as coll
      :refer [join]]
    [quantum.core.convert                 :as conv]
    [quantum.core.data.complex.xml        :as xml]
    [quantum.core.error                   :as err
      :refer [>ex-info]]
    [quantum.core.fn                      :as fn
      :refer [<- fn1]]
    [quantum.core.log                     :as log
      :refer [prl!]]
    [quantum.core.logic
      :refer [whenp whenf whenc]]
    [quantum.core.meta.repl
      :refer [#?@(:clj [source doc])]]
    [quantum.core.macros                  :as macros]
    [quantum.core.ns                      :as ns]
    [quantum.core.numeric                 :as num]
    [quantum.core.process                 :as proc]
    [quantum.core.resources               :as res]
    [quantum.core.io.core                 :as io]
    [quantum.core.paths                   :as path]
    [quantum.core.print                   :as pr]
    [quantum.core.string                  :as str]
    [quantum.core.system                  :as sys]
    [quantum.core.time.core               :as time]
    [quantum.db.datomic                   :as db]
    [quantum.db.datomic.core              :as dbc]
    [quantum.apis.amazon.cloud-drive.auth :as amz-auth]
    [quantum.apis.amazon.cloud-drive.core :as amz]
    [quantum.auth.core                    :as auth]
    [quantum.measure.convert              :as uconv]
    [quantum.net.http                     :as http]
    [quantum.net.server.middleware        :as mid]])
    [quantum.net.url                      :as url]
    [quantum.net.websocket                :as conn]
    [quantum.security.core                :as sec]
    [quantum.security.cryptography        :as crypto]))

#?(:cljs (enable-console-print!))
(println "Hey console!")
