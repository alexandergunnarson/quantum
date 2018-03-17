(ns quantum.apis.librato.core
  (:require
    [quantum.core.collections      :as coll]
    [quantum.core.paths
      :refer [path]]
    [quantum.net.http              :as http]
    [quantum.security.cryptography :as crypto]))

(def base-path "https://metrics-api.librato.com/v1")

(defn get-metrics
  [{:as auth :keys [username token]}
   {:keys [metric #_string? start-time #_unix-time? end-time #_unix-time?
           duration   #_(spec integer? "In minutes")
           resolution #_(spec integer? "In seconds")]}]
  (http/request!
    {:method       :get
     :url          (path base-path "measurements" metric)
     :headers      {"Authorization" (str "Basic " (crypto/encode64-string (str username ":" token)))}
     :query-params (->> {:start_time start-time :end_time end-time
                         :duration duration :resolution resolution}
                        (coll/remove-vals' nil?))}))
