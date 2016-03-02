(ns quantum.network.mirror
  (:require-quantum [:lib http auth web uconv])
  (:require
    [quantum.network.server :as server]
    [ring.adapter.jetty         :as jetty    ]
    [compojure.handler          :as handler  ]
    [compojure.core             :refer [routes GET ANY POST]]
    [compojure.route            :as route]
    [ring.middleware.params     :refer [wrap-params]])
  (:import
    org.apache.http.entity.mime.MultipartEntityBuilder
    org.apache.http.entity.ContentType
    org.apache.http.client.methods.HttpGet
    org.apache.http.client.methods.HttpEntityEnclosingRequestBase
    org.apache.http.impl.client.DefaultHttpClient))

(def stream->url
  (atom
    (fn [req]
      (! req)
      (let [^org.apache.http.HttpResponse resp
              (.execute (DefaultHttpClient.) (HttpGet. (-> req :query-params (get "url") str)))
            ^InputStream stream (-> resp
                                    .getEntity
                                    .getContent)
            content-length (atom nil)
            _ (doseq [header (seq (.getAllHeaders resp))]
                (when (-> header .getName (= "Content-Length"))
                  (reset! content-length (.getValue header))))]
        {:body    stream
         :headers {"Content-Length" (or @content-length (.available stream))
                   ; Chunked to stream
                   ;"Transfer-Encoding" "Chunked"
                   }}))))

(def handlers
  (routes
    (ANY "/stream-to-url" [] (-> @stream->url server/wrap-cors-resp wrap-params server/wrap-stacktrace))

    (route/resources "/")
    (compojure.core/rfn request
      (do (log/pr :debug "NOT FOUND REQUEST IS" request)
          ; (reset! temp1 {:fn (ns/this-fn-name :prev) :req request})
          {:body "" :status 404
           :headers {"Content-Type"                     "application/text"
                     "Access-Control-Allow-Origin"      "http://localhost:3450"
                     "Access-Control-Allow-Credentials" true
                     "Access-Control-Allow-Headers"     "Content-Type, Access-Control-Allow-Credentials, Access-Control-Allow-Origin"
                     ;"Access-Control-Allow-Credentials" true  ; cannot use wildcard when allow-credentials is true
                     }}))))

(def server (jetty/run handlers #_(server/wrap-stacktrace handlers)
                    {:port 3450 :host "localhost" :join? false}))