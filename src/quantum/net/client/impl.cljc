(ns ^{:doc "HTTP request processing with error handling, log writing, etc."}
  quantum.net.client.impl
           (:require  [clojure.core                 :as core]
             #?(:cljs [goog.userAgent               :as agent])
                      [cognitect.transit            :as t]
                      [#?(:clj  org.httpkit.client
                          :cljs cljs-http.client)   :as http]
             #?(:cljs [cljs.reader
                        :refer [read-string]])
                      [clojure.core.async           :as casync]
                      [quantum.core.convert
                        :refer [base64-encode ->json json->
                                ->transit transit->]]
                      [quantum.core.error           :as err
                        :refer [>ex-info]]
                      [quantum.core.log             :as log]
                      [quantum.core.string          :as str]
                      [quantum.net.core             :as net]
                      [quantum.core.fn              :as fn
                        :refer [fn-> fn1 fn']]
                      [quantum.core.logic           :as logic
                        :refer [fn-and whenf1]]
                      [quantum.core.collections     :as coll
                        :refer [kw-map containsv?]]
                      [quantum.core.async           :as async
                        :refer [go]]
                      [quantum.core.vars            :as var
                        :refer [def-]])
  #?(:clj  (:import   org.apache.http.entity.mime.MultipartEntityBuilder
                      org.apache.http.entity.ContentType
                      org.apache.http.client.methods.HttpPost
                      org.apache.http.client.methods.HttpEntityEnclosingRequestBase
                      org.apache.http.impl.client.DefaultHttpClient
                      (org.httpkit.client HttpClient)
                      java.io.File)))

(def ^{:doc "According to OWASP, these are important and
             recommended headers to add to every request."}
  security-headers
  {; This header can be used to prevent ClickJacking in modern browsers.
   "X-Frame-Options"  "DENY"
   ; Reflected cross-scripting attack prevention
   "X-XSS-Protection" "1; mode=block"
   ; Force every browser request to be sent over TLS/SSL
   ; (this can prevent SSL strip attacks).
   "Strict-Transport-Security" "max-age=8640000; includeSubDomains"})

; ======== CLJS IMPLEMENTATION ========= ;

#?(:cljs
(defn wrap-request
  "Returns a batteries-included HTTP request function corresponding to the given
   core client. See |request|"
  [req]
  (log/pr ::debug (kw-map req))
  (-> req
      http/wrap-accept
      http/wrap-form-params
      http/wrap-multipart-params
      http/wrap-edn-params
      http/wrap-edn-response
      http/wrap-transit-params
      http/wrap-transit-response
      http/wrap-json-params
      http/wrap-json-response
      http/wrap-content-type
      http/wrap-query-params
      http/wrap-basic-auth
      http/wrap-oauth
      http/wrap-method
      http/wrap-url
      http/wrap-channel-from-request-map
      (http/wrap-default-headers security-headers))))

#?(:cljs
(def ^{:doc "Executes the HTTP request corresponding to the given map and
             returns the response map for corresponding to the resulting
             HTTP response.
             In addition to the standard Ring request keys, the following keys
             are also recognized:
             * :url
             * :method
             * :query-params"}
  request! (wrap-request cljs-http.core/request)))

; ======== CLOJURE IMPLEMENTATION ========= ;

; http://www.december.com/html/spec/httpstat.html

(defrecord HTTPLogEntry [tries])
; TODO don't use a global log...
(defonce http-log (atom {}))

; TODO Implement Resumable uploads: https://developers.google.com/drive/web/manage-uploads
;___________________________________________________________________________________________________________________________________
;================================================={       NORMALIZE PARAMS        }=================================================
;================================================={                               }=================================================
#?(:clj
(defn request!
  "'Safe' because it handles various HTTP errors (401, 403, 500, etc.),
   and limits retries at @max-tries (which defaults at 3)."
   {:todo  ["EOFException SSL peer shut down incorrectly  sun.security.ssl.InputRecord.read
             INFO: I/O exception (java.net.SocketException) caught when connecting to
                   {s}->https://www.googleapis.com: Connection reset"]
    :usage
     '(request!
        {:method :post
         :url         "https://www.googleapis.com/upload/drive/v2/files"
         :oauth-token (access-key :drive :offline)
         :parse?   true
         :handlers {:default (fn [req resp] (throw (>ex-info :http "HTTP exception" (:status response))))
                    404      (fn [req resp] (throw (>ex-info :http "404 exception")))
                    401      (fn [req resp]
                               (refresh-access!)
                               (http/request! (assoc req :new-access-key 123)))}
         :multipart
           [{:name "file"
             :mime-type :json
             :encoding :utf-8
             :content (->json {:title "My File 2"})}
            {:name "file1"
             :mime-type "image/jpeg"
             :content
               (io/file
                 [:home "Collections" "Images" "Backgrounds" "3331-11.jpg"])}]})
    :attribution "alexandergunnarson"}
  [{:as req
    :keys [handlers log? log tries max-tries keys-fn raw? middleware]
    :or {as :auto
         middleware identity
         max-tries 3
         tries     0}}]
  (log/ppr :http req)
  (if (= tries max-tries)
      (throw (>ex-info :error/http
                   (str "HTTP exception, status " (:status req) ". Maximum tries (3) exceeded.")
                   {:status (:status req)}))
      (let [response        @(http/request (dissoc req :status :log))
            status          (:status response)]
        (if (or (= status 200) (= status 201))
            ((or (get handlers status) fn/seconda) req (middleware response))
            (let [status-handler
                   (or (get handlers status)
                       (get handlers :default)
                       (fn'
                         (do (log/pr :http/warn "unhandled HTTP status:" status) response)))
                  req-n+1
                    (assoc req
                      :tries (inc tries)
                      :max-tries max-tries)]
              (status-handler req-n+1 (middleware response))))))))
