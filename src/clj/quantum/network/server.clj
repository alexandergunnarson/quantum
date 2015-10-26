(ns quantum.network.server
  (:require-quantum [:lib])
  (:require
    [ring.adapter.jetty         :as jetty    ]
    [compojure.handler          :as handler  ]
    [compojure.core             :refer [routes GET ANY POST]]
    [compojure.route            :as route]
    [cognitect.transit          :as t]
    ; [ring.middleware.transit    :refer [wrap-transit-body]]
    [ring.middleware.params     :refer [wrap-params]]
    ; [ring.middleware.accept     :refer [wrap-accept]]
    ))

(def cors? (atom true))
(def *error* (atom nil))
(def cors-headers
  {"Access-Control-Allow-Origin"      "http://localhost:3450"    
   "Access-Control-Allow-Credentials" "true" ; can't use boolean... dunno why... ; cannot use wildcard when allow-credentials is true  
   "Access-Control-Allow-Headers"     "Content-Type, Accept, Access-Control-Allow-Credentials, Access-Control-Allow-Origin"})

(defn wrap-keywordify [f]
  (fn [req]
    (f (-> req
           (update :query-params coll/keywordify-keys)
           (update :params       coll/keywordify-keys)
           (update :headers      coll/keywordify-keys)))))

(defn wrap-cors-resp [f]
  (fn [req]
    (let [resp (f req)]
      (assoc resp :headers
        (if @cors?
            (merge-keep-left (:headers resp) cors-headers)
            (:headers resp))))))

; They go backwards
(def wrap-standard
  (fn-> wrap-keywordify
        wrap-params
        #_(wrap-transit-body #{:keywords?})
        wrap-cors-resp))

(defn wrap-stacktrace
  "Wrap a handler such that exceptions are logged to *err* and then rethrown.
  Accepts the following options:
  :color? - if true, apply ANSI colors to stacktrace (default false)"
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable e
      	(reset! *error* e)
        (trace e)
        (throw e)))))
