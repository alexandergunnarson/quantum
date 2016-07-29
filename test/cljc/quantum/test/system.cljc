(ns quantum.test.system
  (:require [quantum.system :as ns]))

(defn test:default-config
  [& [{:as config
       {:keys [port ssl-port host]          :as server    } :server
       {:keys [uri msg-handler]             :as connection} :connection
       {:keys [js-source-file]                            } :deployment
       {:keys [schemas ephemeral backend]   :as db        } :db
       {:keys [render root-id]              :as frontend  } :frontend
       {                                    :as threadpool} :threadpool}]])

(defn test:gen-system-creator [system-kw config])