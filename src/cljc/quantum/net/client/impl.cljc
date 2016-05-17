(ns ^{:doc "HTTP request processing with error handling, log writing, etc."}
  quantum.net.client.impl
           (:require  [#?(:clj  clojure.core
                         :cljs cljs.core   )        :as core  ]
             #?(:cljs [goog.userAgent               :as agent ])
                      [cognitect.transit            :as t     ]
             #?(:clj  [org.httpkit.client           :as http  ])
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
                        :refer [#?@(:clj [fn-> f*n])]         ]
                      [quantum.core.logic           :as logic
                        :refer [#?@(:clj [fn-and whenf*n])
                                nnil?]                        ]
                      [quantum.core.collections     :as coll
                        :refer [#?(:clj kmap)]                ]
                      [quantum.core.vars            :as var  
                        :refer [#?(:clj def-)]                ])
  #?(:cljs (:require-macros
                      [quantum.core.collections     :as coll
                        :refer [kmap]                         ]
                      [quantum.core.fn              :as fn
                        :refer [fn-> f*n]                     ]
                      [quantum.core.log             :as log   ]
                      [quantum.core.logic           :as logic
                        :refer [fn-and whenf*n]               ]
                      [quantum.core.vars            :as var
                        :refer [def-]                         ]))
  #?(:clj  (:import   org.apache.http.entity.mime.MultipartEntityBuilder
                      org.apache.http.entity.ContentType
                      org.apache.http.client.methods.HttpPost
                      org.apache.http.client.methods.HttpEntityEnclosingRequestBase
                      org.apache.http.impl.client.DefaultHttpClient
                      java.io.File)
     :cljs (:import   goog.Uri
                      [goog.net EventType ErrorCode XhrIo Jsonp])))

; CLJS HTTP UTIL

(defn basic-auth
  "Returns the value of the HTTP basic authentication header for
  @credentials."
  [credentials]
  (when credentials
    (let [[username password]
          (if (map? credentials)
              (map credentials [:username :password])
              credentials)]
      (str "Basic " (base64-encode (str username ":" password))))))

#?(:cljs
(defn build-url
  "Build the url from the request map."
  [{:keys [scheme server-name server-port uri query-string] :as req}]
  (log/pr ::debug (kmap req))
  (str (doto (Uri.)
         (.setScheme (name (or scheme :http)))
         (.setDomain server-name)
         (.setPort server-port)
         (.setPath uri)
         (.setQuery query-string true)))))

(defn build-headers
  "Build the headers from the map."
  [m]
  (->> m keys (map str/camelize) (#(zipmap % (vals m)))
      #?(:cljs clj->js)))

(defn parse-headers [headers]
  (reduce
   #(let [[k v] (str/split %2 #":\s+")]
      (if (or (str/blank? k) (str/blank? v))
        %1 (assoc %1 (str/->lower k) v)))
   {} (str/split (or headers "") #"(\n)|(\r)|(\r\n)|(\n\r)")))

; CLJS HTTP CORE

(defonce pending-requests (atom {}))

(defn abort!
  "Attempt to close the given channel and abort the pending HTTP request
  with which it is associated."
  [channel]
  #?(:clj (throw (->ex :not-implemented))
     :cljs
      (when-let [req (@pending-requests channel)]
        (swap! pending-requests dissoc channel)
        (casync/close! channel)
        (if (.hasOwnProperty req "abort")
            (.abort req)
            (.cancel (:jsonp req) (:request req))))))

(defn- aborted? [xhr]
  #?(:clj  (throw (->ex :not-implemented))
     :cljs (= (.getLastErrorCode xhr) goog.net.ErrorCode.ABORT)))

(defn apply-default-headers!
  "Takes an XhrIo object and applies the default-headers to it."
  [xhr headers]
  #?(:clj (throw (->ex :not-implemented))
     :cljs
      (doseq [h-name (map str/camelize (keys headers))
              h-val  (vals headers)]
        (.set (.-headers xhr) h-name h-val))))
   
(defn build-xhr
  "Builds an XhrIo object from the request parameters."
  [{:keys [with-credentials? default-headers] :as request}]
  (log/pr ::debug (kmap request))
  (let [timeout          (or (:timeout request) 0)]
    #?(:clj (throw (->ex :not-implemented))
       :cljs
       ; TODO Validate URLs passed to XMLHttpRequest.open.
       ; Current browsers allow these URLs to be cross
       ; domain; this behavior can lead to code injection
       ; by a remote attacker. Pay extra attention to absolute URLs.
        (doto (XhrIo.)
          (apply-default-headers! default-headers)
          (.setTimeoutInterval timeout)
          (.setWithCredentials (boolean with-credentials?))))))

;; Reverses the goog.net.ErrorCode constants to map to CLJS keywords
#?(:cljs
(def error-kw
  (->> (js->clj goog.net.ErrorCode)
       (keep (fn [[code-name n]]
               (when (integer? n)
                 [n (keyword (str/kebabize code-name))])))
       (into {}))))

#?(:cljs
(defn xhr
  "Execute the HTTP request corresponding to the given Ring request
  map and return a core.async channel."
  [{:keys [request-method headers body with-credentials? cancel] :as request}]
  (log/pr ::debug (kmap request))
  (let [channel     (casync/chan)
        request-url (build-url request)
        xhr         (build-xhr (assoc request :default-headers headers))
        ;_ (log/pr :debug "XHR IS" xhr)
        headers-js  (build-headers headers) ; was camelizing before
        method      (-> (or request-method :get) name str/upper-case)]
    (swap! pending-requests assoc channel xhr)
    ; xhr (and (.-headers xhr)) is a "circular structure"
    ; so it can't be converted to JSON
    (.listen xhr EventType.COMPLETE
      (fn [evt]
        (let [target (.-target evt)
              ; TODO Discard requests received over plain HTTP with HTTPS 
              response
                {:status          (->> target .getStatus                                 )
                 :success         (->> target .isSuccess                                 )
                 :body            (->> target .getResponseText                           )
                 :headers         (->> target .getAllResponseHeaders parse-headers       )
                 :trace-redirects (->> target .getLastUri            (vector request-url))
                 :error-code      (->> target .getLastErrorCode      error-kw            )
                 :error-text      (->> target .getLastError                              )}]
          (when-not (aborted? xhr)
            (casync/put! channel response))
          (swap! pending-requests dissoc channel)
          (when cancel (casync/close! cancel))
          (casync/close! channel))))
    (.send xhr request-url method body headers-js)
    (when cancel
      (go
        (let [v (casync/<! cancel)]
          (when-not (.isComplete xhr)
            (.abort xhr)))))
    channel)))

#?(:cljs
(defn jsonp
  "Execute the JSONP request corresponding to the given Ring request
  map and return a core.async channel."
  [{:keys [timeout callback-name cancel] :as request}]
  (let [channel (casync/chan)
        jsonp (Jsonp. (build-url request) callback-name)]
    (.setRequestTimeout jsonp timeout)
    (let [req (.send jsonp nil
                     (fn success-callback [data]
                       (let [response {:status 200
                                       :success true
                                       :body (js->clj data :keywordize-keys true)}]
                         (casync/put! channel response)
                         (swap! pending-requests dissoc channel)
                         (if cancel (casync/close! cancel))
                         (casync/close! channel)))
                     (fn error-callback []
                       (swap! pending-requests dissoc channel)
                       (if cancel (casync/close! cancel))
                       (casync/close! channel)))]
      (swap! pending-requests assoc channel {:jsonp jsonp :request req})
      (when cancel
        (go
          (let [v (casync/<! cancel)]
            (.cancel jsonp req)))))
    channel)))

(defn request*
  "Execute the HTTP request corresponding to the given Ring request
  map and return a core.async channel."
  [{:keys [request-method] :as req}]
  ;(log/pr ::debug "FINAL REQ" req)
  (if (= request-method :jsonp)
      #?(:clj  (throw (->ex :not-implemented))
         :cljs (jsonp req))
      #?(:clj  (throw (->ex :not-implemented))
         :cljs (xhr   req))))

(defn parse-query-params
  "Parse @s as query params and return a hash map."
  [s]
  (when-not (str/blank? s)
    (reduce
     #(let [[k v] (str/split %2 #"=")]
        (assoc %1
          (keyword (net/url-decode k))
          (net/url-decode v)))
     {} (str/split (str s) #"&"))))



(defn- encode-val [k v]
  (str (net/url-encode (name k)) "=" (net/url-encode (str v))))

(defn- encode-vals [k vs]
  (->>
    vs
    (map #(encode-val k %))
    (str/join "&")))

(defn- encode-param [[k v]]
  (if (coll? v)
      (encode-vals k v)
      (encode-val k v)))

(defn generate-query-string [params]
  (->>
    params
    (map encode-param)
    (str/join "&")))

(defn decode-body
  "Decode the :body of @response with @decode-fn if the content type matches."
  [response decode-fn content-type request-method]
  (if (and (not= :head request-method)
           (not= 204 (:status response))
           (re-find (re-pattern (str "(?i)" (net/escape-special content-type)))
                    (str (get (:headers response) "content-type" ""))))
    (update-in response [:body] decode-fn)
    response))

; MIDDLEWARE

(defn wrap-edn-params
  "Encode :edn-params in the `request` :body and set the appropriate
  Content Type header."
  [client]
  (fn [request]
    (if-let [params (:edn-params request)]
      (-> (dissoc request :edn-params)
          (assoc :body (pr-str params))
          (assoc-in [:headers "content-type"] "application/edn")
          (client))
      (client request))))

(defn wrap-edn-response
  "Decode application/edn responses."
  [client]
  (fn [request]
    (-> #(decode-body % read-string "application/edn" (:request-method request))
        (casync/map [(client request)]))))

(defn wrap-default-headers
  [client & [default-headers]]
  (fn [request]
    (log/pr ::debug (kmap request))
    (if-let [default-headers (or (:default-headers request) default-headers)]
      (client (assoc request :default-headers default-headers))
      (client request))))

(defn wrap-accept
  [client & [accept]]
  (fn [request]
    (log/pr ::debug (kmap request))
    (if-let [accept (or (:accept request) accept)]
      (client (assoc-in request [:headers "accept"] accept))
      (client request))))

(defn wrap-content-type
  [client & [content-type]]
  (fn [request]
    (log/pr ::debug (kmap request))
    (if-let [content-type (or (:content-type request) content-type)]
      (client (assoc-in request [:headers "content-type"] content-type))
      (client request))))

(def- default-transit-opts
  {:encoding :json :encoding-opts {}
   :decoding :json :decoding-opts {}})

(defn wrap-transit-params
  "Encode :transit-params in the `request` :body and set the appropriate
  Content Type header.
  A :transit-opts map can be optionally provided with the following keys:
  :encoding                #{:json, :json-verbose}
  :decoding                #{:json, :json-verbose}
  :encoding/decoding-opts  appropriate map of options to be passed to
                           transit writer/reader, respectively."
  [client]
  (fn [request]
    (log/pr ::debug (kmap request))
    (if-let [params (or (:transit-params request)
                        (when (= (or (get-in request [:headers "Content-Type"])
                                     (get-in request [:headers "content-type"]))
                                 "application/transit+json")
                          (:body request)))]
      (let [{:keys [encoding encoding-opts]} (merge default-transit-opts
                                                    (:transit-opts request))]
        (-> (dissoc request :transit-params)
            (assoc :body (->transit params encoding encoding-opts))
            (assoc-in [:headers "content-type"] "application/transit+json")
            (assoc :content-type "application/transit+json")
            (client)))
      (client request))))

(defn wrap-transit-response
  "Decode application/transit+json responses."
  [client]
  (fn [request]
    (log/pr ::debug (kmap request))
    (let [{:keys [decoding decoding-opts]}
            (merge default-transit-opts (:transit-opts request))
          transit-decode #(transit-> % decoding decoding-opts)]
      (->> [(client request)]
           (casync/map
             #(decode-body % transit-decode "application/transit+json"
                (:request-method request)))))))

(defn wrap-json-params
  "Encode :json-params in the `request` :body and set the appropriate
  Content Type header."
  [client]
  (fn [request]
    (log/pr ::debug (kmap request))
    (if-let [params (:json-params request)]
      (-> (dissoc request :json-params)
          (assoc :body (->json params))
          (assoc-in [:headers "content-type"] "application/json")
          (client))
      (client request))))

(defn wrap-json-response
  "Decode application/json responses."
  [client]
  (fn [request]
    (log/pr ::debug (kmap request))
    (->> [(client request)]
         (casync/map
           #(decode-body % (fn [x] (json-> x str/keywordize)) "application/json"
              (:request-method request))))))

(defn wrap-query-params [client]
  (fn [{:keys [query-params] :as req}]
    (log/pr ::debug (kmap req))
    (if query-params
        (client (-> req (dissoc :query-params)
                    (assoc :query-string
                      (generate-query-string query-params))))
        (client req))))

(defn wrap-form-params [client]
  (fn [{:keys [form-params request-method] :as request}]
    (log/pr ::debug (kmap request))
    (if (and form-params (#{:post :put :patch :delete} request-method))
        (client (-> request
                    (dissoc :form-params)
                    (assoc :body (generate-query-string form-params))
                    (assoc-in [:headers "content-type"] "application/x-www-form-urlencoded")))
        (client request))))

#?(:cljs
(defn generate-form-data [params]
  (let [form-data (js/FormData.)]
    (doseq [[k v] params]
      (.append form-data (name k) v))
    form-data)))

#?(:cljs
(defn wrap-multipart-params [client]
  (fn [{:keys [multipart-params request-method] :as request}]
    (log/pr ::debug (kmap request))
    (if (and multipart-params (#{:post :put :patch :delete} request-method))
        (client (-> request
                    (dissoc :multipart-params)
                    (assoc :body (generate-form-data multipart-params))))
        (client request)))))

(defn wrap-method [client]
  (fn [req]
    (if-let [m (:method req)]
      (client (-> req (dissoc :method)
                  (assoc :request-method m)))
      (client req))))

(defn wrap-server-name [client server-name]
  #(client (assoc %1 :server-name server-name)))

(defn wrap-url [client]
  (fn [{:keys [query-params] :as req}]
    (if-let [spec (net/parse-url (:url req))]
      (client (-> (merge req spec)
                  (dissoc :url)
                  (update-in [:query-params] #(merge %1 query-params))))
      (client req))))

(defn wrap-basic-auth
  "Middleware converting the :basic-auth option or `credentials` into
  an Authorization header."
  [client & [credentials]]
  (fn [req]
    (let [credentials (or (:basic-auth req) credentials)]
      (if-not (empty? credentials)
        (client (-> (dissoc req :basic-auth)
                    (assoc-in [:headers "authorization"] (basic-auth credentials))))
        (client req)))))

(defn wrap-oauth
  "Middleware converting the :oauth-token option into an Authorization header."
  [client]
  (fn [req]
    (if-let [oauth-token (:oauth-token req)]
      (client (-> req (dissoc :oauth-token)
                  (assoc-in [:headers "authorization"]
                            (str "Bearer " oauth-token))))
      (client req))))

(defn wrap-channel-from-request-map
  "Pipe the response-channel into the request-map's
   custom channel (e.g. to enable transducers)"
  [client]
  (fn [request]
    (if-let [custom-channel (:channel request)]
      (casync/pipe (client request) custom-channel)
      (client request))))

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

(defn wrap-request
  "Returns a batteries-included HTTP request function corresponding to the given
   core client. See |request|"
  [req]
  (log/pr ::debug (kmap req))
  (-> req
      wrap-accept
      wrap-form-params
      #?(:cljs wrap-multipart-params)
      wrap-edn-params
      wrap-edn-response
      wrap-transit-params
      wrap-transit-response
      wrap-json-params
      wrap-json-response
      wrap-content-type
      wrap-query-params
      wrap-basic-auth
      wrap-oauth
      wrap-method
      wrap-url
      wrap-channel-from-request-map
      (wrap-default-headers security-headers)))

#?(:cljs
(def ^{:doc "Executes the HTTP request corresponding to the given map and
             returns the response map for corresponding to the resulting
             HTTP response.
             In addition to the standard Ring request keys, the following keys
             are also recognized:
             * :url
             * :method
             * :query-params"}
  request! (wrap-request request*)))

; ======== CLOJURE IMPLEMENTATION =========

; http://www.december.com/html/spec/httpstat.html

(defrecord HTTPLogEntry [tries])
; TODO don't use a global log...
(defonce http-log (atom {}))

; TODO Implement Resumable uploads: https://developers.google.com/drive/web/manage-uploads
;___________________________________________________________________________________________________________________________________
;================================================={       NORMALIZE PARAMS        }=================================================
;================================================={                               }=================================================
#?(:clj
(defn add-part!
  {:todo []}
  [^MultipartEntityBuilder meb
   {:keys [^String name mime-type encoding content]}]
  (let [^String mime-type-f (net/mime-type->str            mime-type)
        ^String encoding-f  (net/normalize-encoding-type   encoding)
        content-f           (net/normalize-content content mime-type encoding)]
    (cond
    (string? content-f)
      (.addTextBody meb
        name
        ^String content
        (ContentType/create mime-type-f encoding-f))
    (instance? java.io.File content-f)
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
  (let [^DefaultHttpClient client (DefaultHttpClient.)
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

#?(:clj
(defn request!*
  {:todo ["Integrate this with the rest of clj-http"]}
  [{:keys [^Key method ^Map multipart] :as req}]
  (if (nnil? multipart)
      (condp = method
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
  (if (= tries max-tries)
      (throw (->ex :error/http
                   (str "HTTP exception, status " (:status req) ". Maximum tries (3) exceeded.")
                   {:status (:status req)}))
      (let [response        (request!* (dissoc req :status :log))
            status          (:status response)
            parse-middleware (whenf*n (fn-and (constantly (not raw?))
                                              (fn-> :headers :content-type (.contains "application/json"))) ; containsv?
                               (fn-> (update :body (f*n json-> (or keys-fn str/keywordize)))))]
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