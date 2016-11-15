(ns ^{:doc "HTTP request processing with error handling, log writing, etc."}
  quantum.net.client.impl
           (:require  [#?(:clj  clojure.core
                         :cljs cljs.core   )        :as core  ]
             #?(:cljs [goog.userAgent               :as agent ])
                      [cognitect.transit            :as t     ]
                      [#?(:clj org.httpkit.client
                          :cljs cljs-http.client)   :as http  ]
             #?(:cljs [cljs.reader
                        :refer [read-string]                  ])
                      [#?(:clj  clojure.core.async
                          :cljs cljs.core.async   ) :as casync]
                      [quantum.core.convert
                        :refer [base64-encode ->json json->
                                ->transit transit->]          ]
                      [quantum.core.error           :as err
                        :refer [->ex]                         ]
                      [quantum.core.log             :as log   ]
                      [quantum.core.string          :as str   ]
                      [quantum.net.core             :as net   ]
                      [quantum.core.fn              :as fn
                        :refer [#?@(:clj [fn-> fn1])]         ]
                      [quantum.core.logic           :as logic
                        :refer [#?@(:clj [fn-and whenf1])
                                nnil?]                        ]
                      [quantum.core.collections     :as coll
                        :refer [#?@(:clj [kmap containsv?])]  ]
                      [quantum.core.vars            :as var
                        :refer [#?(:clj def-)]                ])
  #?(:cljs (:require-macros
                      [cljs.core.async.macros
                        :refer [go]                           ]
                      [quantum.core.collections     :as coll
                        :refer [kmap containsv?]              ]
                      [quantum.core.fn              :as fn
                        :refer [fn-> fn1]                     ]
                      [quantum.core.log             :as log   ]
                      [quantum.core.logic           :as logic
                        :refer [fn-and whenf1]               ]
                      [quantum.core.vars            :as var
                        :refer [def-]                         ]))
  #?(:clj  (:import   org.apache.http.entity.mime.MultipartEntityBuilder
                      org.apache.http.entity.ContentType
                      org.apache.http.client.methods.HttpPost
                      org.apache.http.client.methods.HttpEntityEnclosingRequestBase
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
  (log/pr ::debug (kmap req))
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
; Somehow, it always results in SerializationException if I try to use the built-in CLJ-HTTP multipart
#?(:clj
(defn add-part!
  {:todo []}
  [^MultipartEntityBuilder meb
   {:keys [^String name mime-type encoding content]}]
  (let [^String mime-type-f (net/mime-type->str            mime-type)
        ^String encoding-f  (net/normalize-encoding-type   encoding)
        content-f           (net/normalize-content content mime-type)]
    (cond
    (string? content-f)
      (.addTextBody meb
        name
        ^String content
        (ContentType/create mime-type-f encoding-f))
    (instance? File content-f)
      (.addBinaryBody meb
        name
        ^File content)
    :else
      (throw (->ex :unknown-content-type nil (class content)))))))

#?(:clj
(defn add-header!
  [^HttpEntityEnclosingRequestBase req [header-name ^String content]]
  (condp = header-name
    :oauth-token
      (.addHeader req
        (org.apache.http.message.BasicHeader.
          "authorization"
          (str "Bearer " content)))
    (throw (->ex :unknown-header-type header-name)))))

#?(:clj
(defn add-headers!
  {:todo ["Add support for all headers"]}
  [^HttpEntityEnclosingRequestBase req headers]
  (core/doseq [header headers]
    (add-header! req header))))

#?(:clj
(defn post-multipart!
  {:todo ["Integrate this with the rest of clj-http"]}
  [{:keys [^String url ^Vec multipart ; vector of maps
           ^String oauth-token
                   headers]}]
  (let [^HttpClient client @http/default-client
        ^HttpEntityEnclosingRequestBase req (HttpPost. url)
        ^MultipartEntityBuilder meb
          (org.apache.http.entity.mime.MultipartEntityBuilder/create)]
    (doseq [part multipart]
      (add-part! meb part))
    (.setEntity req (.build meb))
    (add-headers! req (merge headers (kmap oauth-token)))

    (-> client (.execute req)
      ;(.getEntity)
      ))))

 (let [entities (into (map (fn [{:keys [name content filename content-type]}]
                                  (MultipartEntity. name content filename content-type)) multipart)
                      (map (fn [[k v]] (MultipartEntity. k v nil nil)) form-params))
            boundary (MultipartEntity/genBoundary entities)]
        (-> r
            (assoc-in [:headers "Content-Type"]
                      (str "multipart/form-data; boundary=" boundary))
            (assoc :body (MultipartEntity/encode boundary entities))))

#?(:clj
(defn request!*
  {:todo ["Integrate this with the rest of clj-http"]}
  [{:keys [method multipart] :as req}]
  (if multipart
      (case method
        :post (post-multipart! req)
        (throw (->ex :invalid-request-type
                      "Method not a valid HTTP request type."
                      method)))
      @(http/request req)))) ; |deref| because it's a |promise|

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
         :handlers {:default (fn [req resp] (throw (->ex :http "HTTP exception" (:status response))))
                    404      (fn [req resp] (throw (->ex :http "404 exception")))
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
    :attribution "Alex Gunnarson"}
  [{:as req
    :keys [handlers log? log tries max-tries keys-fn raw?]
    :or {as :auto
         max-tries 3
         tries     0}}]
  (log/ppr :http req)
  (if (= tries max-tries)
      (throw (->ex :error/http
                   (str "HTTP exception, status " (:status req) ". Maximum tries (3) exceeded.")
                   {:status (:status req)}))
      (let [response        (request!* (dissoc req :status :log))
            status          (:status response)
            parse-middleware (whenf1 (fn-and (constantly (not raw?))
                                              (fn-> :headers :content-type (containsv? "application/json")))
                               (fn-> (update :body (fn1 json-> keys-fn))))]
        (if (or (= status 200) (= status 201))
            ((or (get handlers status) fn/seconda) req (parse-middleware response))
            (let [status-handler
                   (or (get handlers status)
                       (get handlers :default)
                       (constantly
                         (do (log/pr :warn "unhandled HTTP status:" status) response)))
                  req-n+1
                    (assoc req
                      :tries (inc tries)
                      :max-tries max-tries)]
              (status-handler req-n+1 (parse-middleware response))))))))
