(ns quantum.apis.librato.core
  (:require
    [quantum.core.collections      :as c]
    [quantum.core.convert          :as conv]
    [quantum.core.paths
      :refer [path]]
    [quantum.core.spec             :as s]
    [quantum.net.http              :as http]
    [quantum.security.cryptography :as crypto]))

(def base-path "https://metrics-api.librato.com/v1")

#_(s/def ::measurement
    (s/keys :req-un [(def :name  (s/and t/named? #"[A-Za-z0-9\.\:]+"))
                     (def :value t/named?)]))

(defn request>with-auth [req #_ring-request? username #_string? token #_string?]
  (assoc-in req [:headers "Authorization"]
    (str "Basic " (crypto/encode :base64-string (str username ":" token)))))

(defn get-measurements
  [{:as auth :keys [username #_string? token #_string?]}
   {:keys [metric #_string? start-time #_unix-time? end-time #_unix-time?
           duration   #_(spec integer? "In minutes")
           resolution #_(spec integer? "In seconds")]}]
  (-> {:url          (path base-path "measurements" metric)
       :method       :get
       :query-params (->> {:start_time start-time :end_time end-time
                           :duration duration :resolution resolution}
                          (c/remove-vals' nil?))}
      (request>with-auth username token)
      http/request!))

(defn post-measurements!
  [{:as auth :keys [username #_string? token #_string?]}
   body #_(s/keys :opt-un [(def :tags         (s/of map? t/named?))
                           (def :measurements (s/seq-of ::measurement))])]
  (-> {:url     (path base-path "measurements")
       :method  :post
       :headers {"Content-Type"  "application/json"}
       :body    (conv/->json body)}
      (request>with-auth username token)
      http/request!))
