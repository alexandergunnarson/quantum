(ns quantum.cljs.http.core
  (:require-quantum [ns log async macros])
  (:import
    goog.Uri
    [goog.net EventType ErrorCode XhrIo Jsonp])
  (:require
    [cljs.core         :as core ]
    [goog.Uri          :as uri  ]
    [goog.userAgent    :as agent]
    [cognitect.transit :as t    ]
    [no.en.core                  :refer [base64-encode url-encode url-decode]]
    [cljs.reader                 :refer [read-string]]
    [clojure.string    :as str   :refer [blank? capitalize join split lower-case]]))

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

(defn build-url
  "Build the url from the request map."
  [{:keys [scheme server-name server-port uri query-string]}]
  (str (doto (Uri.)
         (.setScheme (name (or scheme :http)))
         (.setDomain server-name)
         (.setPort server-port)
         (.setPath uri)
         (.setQuery query-string true))))

(defn camelize
  "Returns dash separated string @s in camel case."
  [s]
  (->> (str/split (str s) #"-")
       (map str/capitalize)
       (join "-")))

(defn build-headers
  "Build the headers from the map."
  [m]
  (->> m keys (map camelize) (#(zipmap % (vals m))) clj->js))

(defn user-agent
  "Returns the user agent."
  [] (agent/getUserAgentString))

(defn android?
  "Returns true if the user agent is an Android client."
  [] (re-matches #"(?i).*android.*" (user-agent)))

(defn transit-decode
  "Transit decode an object from @s."
  [s type opts]
  (let [rdr (t/reader type opts)]
    (t/read rdr s)))

(defn transit-encode
  "Transit encode @x into a String."
  [x type opts]
  (let [wrtr (t/writer type opts)]
    (t/write wrtr x)))

(defn json-decode
  "JSON decode an object from @s."
  [s]
  (when-let [v (when-not (str/blank? s) (js/JSON.parse s))]
    (js->clj v :keywordize-keys true)))

(defn json-encode
  "JSON encode @x into a String."
  [x] (js/JSON.stringify (clj->js x)))

(defn parse-headers [headers]
  (reduce
   #(let [[k v] (split %2 #":\s+")]
      (if (or (blank? k) (blank? v))
        %1 (assoc %1 (lower-case k) v)))
   {} (split (or headers "") #"(\n)|(\r)|(\r\n)|(\n\r)")))

; CLJS HTTP CORE

(def pending-requests (atom {}))

(defn abort!
  "Attempt to close the given channel and abort the pending HTTP request
  with which it is associated."
  [channel]
  (when-let [req (@pending-requests channel)]
    (swap! pending-requests dissoc channel)
    (async/close! channel)
    (if (.hasOwnProperty req "abort")
        (.abort req)
        (.cancel (:jsonp req) (:request req)))))

(defn- aborted? [xhr]
  (= (.getLastErrorCode xhr) goog.net.ErrorCode.ABORT))

(defn apply-default-headers!
  "Takes an XhrIo object and applies the default-headers to it."
  [xhr headers]
  (doseq [h-name (map camelize (keys headers))
          h-val (vals headers)]
    (.set (.-headers xhr) h-name h-val)))

(defn build-xhr
  "Builds an XhrIo object from the request parameters."
  [{:keys [with-credentials? default-headers] :as request}]
  (let [timeout          (or (:timeout request) 0)
        send-credentials (or (nil? with-credentials?) with-credentials?)]
    (doto (XhrIo.)
          (apply-default-headers! default-headers)
          (.setTimeoutInterval timeout)
          (.setWithCredentials send-credentials))))

(defn kebabize [s]
  (-> s 
      str/lower-case
      (str/replace #"_" "-")))

;; Reverses the goog.net.ErrorCode constants to map to CLJS keywords
(def error-kw
  (->> (js->clj goog.net.ErrorCode)
       (keep (fn [[code-name n]]
               (when (integer? n)
                 [n (keyword (kebabize code-name))])))
       (into {})))

(defn xhr
  "Execute the HTTP request corresponding to the given Ring request
  map and return a core.async channel."
  [{:keys [request-method headers body with-credentials? cancel] :as request}]
  (let [channel     (async/chan)
        request-url (build-url request)
        xhr         (build-xhr (assoc request :default-headers headers))
        _ (log/pr :debug "XHR IS" xhr)
        headers-js  (build-headers headers) ; was camelizing before
        method      (-> request-method name str/upper-case)]
    (swap! pending-requests assoc channel xhr)
    (.listen xhr EventType.COMPLETE
      (fn [evt]
        (let [target (.-target evt)
              response
                {:status          (->> target .getStatus                                 )
                 :success         (->> target .isSuccess                                 )
                 :body            (->> target .getResponseText                           )
                 :headers         (->> target .getAllResponseHeaders parse-headers       )
                 :trace-redirects (->> target .getLastUri            (vector request-url))
                 :error-code      (->> target .getLastErrorCode      error-kw            )
                 :error-text      (->> target .getLastError                              )}]
          (when-not (aborted? xhr)
            (async/put! channel response))
          (swap! pending-requests dissoc channel)
          (when cancel (async/close! cancel))
          (async/close! channel))))
    (.send xhr request-url method body headers-js)
    (when cancel
      (go
        (let [v (async/<! cancel)]
          (when-not (.isComplete xhr)
            (.abort xhr)))))
    channel))

(defn jsonp
  "Execute the JSONP request corresponding to the given Ring request
  map and return a core.async channel."
  [{:keys [timeout callback-name cancel] :as request}]
  (let [channel (async/chan)
        jsonp (Jsonp. (build-url request) callback-name)]
    (.setRequestTimeout jsonp timeout)
    (let [req (.send jsonp nil
                     (fn success-callback [data]
                       (let [response {:status 200
                                       :success true
                                       :body (js->clj data :keywordize-keys true)}]
                         (async/put! channel response)
                         (swap! pending-requests dissoc channel)
                         (if cancel (async/close! cancel))
                         (async/close! channel)))
                     (fn error-callback []
                       (swap! pending-requests dissoc channel)
                       (if cancel (async/close! cancel))
                       (async/close! channel)))]
      (swap! pending-requests assoc channel {:jsonp jsonp :request req})
      (when cancel
        (go
          (let [v (async/<! cancel)]
            (.cancel jsonp req)))))
    channel))

(defn request*
  "Execute the HTTP request corresponding to the given Ring request
  map and return a core.async channel."
  [{:keys [request-method] :as req}]
  (log/pr :http "FINAL REQ" req)
  (if (= request-method :jsonp)
      (jsonp req)
      (xhr   req)))



(defn if-pos [v]
  (if (and v (pos? v)) v))

(defn parse-query-params
  "Parse @s as query params and return a hash map."
  [s]
  (when-not (blank? s)
    (reduce
     #(let [[k v] (split %2 #"=")]
        (assoc %1
          (keyword (url-decode k))
          (url-decode v)))
     {} (split (str s) #"&"))))

(defn parse-url
  "Parse @url into a hash map."
  [url]
  (if-not (blank? url)
    (let [uri (uri/parse url)
          query-data (.getQueryData uri)]
      {:scheme (keyword (.getScheme uri))
       :server-name (.getDomain uri)
       :server-port (if-pos (.getPort uri))
       :uri         (.getPath uri)
       :query-string (if-not (.isEmpty query-data)
                       (str query-data))
       :query-params (if-not (.isEmpty query-data)
                       (parse-query-params (str query-data)))})))

(def unexceptional-status?
  #{200 201 202 203 204 205 206 207 300 301 302 303 307})

(defn- encode-val [k v]
  (str (url-encode (name k)) "=" (url-encode (str v))))

(defn- encode-vals [k vs]
  (->>
    vs
    (map #(encode-val k %))
    (join "&")))

(defn- encode-param [[k v]]
  (if (coll? v)
    (encode-vals k v)
    (encode-val k v)))

(defn generate-query-string [params]
  (->>
    params
    (map encode-param)
    (join "&")))

(def regex-char-esc-smap
  (let [esc-chars "()*&^%$#!+"]
    (zipmap esc-chars
            (map #(str "\\" %) esc-chars))))

(defn escape-special
  "Escape special characters -- for content-type."
  [string]
  (->> string
       (replace regex-char-esc-smap)
       (reduce str)))

(defn decode-body
  "Decode the :body of @response with @decode-fn if the content type matches."
  [response decode-fn content-type request-method]
  (if (and (not= :head request-method)
           (not= 204 (:status response))
           (re-find (re-pattern (str "(?i)" (escape-special content-type)))
                    (str (get (:headers response) "content-type" ""))))
    (update-in response [:body] decode-fn)
    response))

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
        (async/map [(client request)]))))

(defn wrap-default-headers
  [client & [default-headers]]
  (fn [request]
    (if-let [default-headers (or (:default-headers request) default-headers)]
      (client (assoc request :default-headers default-headers))
      (client request))))

(defn wrap-accept
  [client & [accept]]
  (fn [request]
    (if-let [accept (or (:accept request) accept)]
      (client (assoc-in request [:headers "accept"] accept))
      (client request))))

(defn wrap-content-type
  [client & [content-type]]
  (fn [request]
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
    (if-let [params (or (:transit-params request)
                        (when (= (or (get-in request [:headers "Content-Type"])
                                     (get-in request [:headers "content-type"]))
                                 "application/transit+json")
                          (:body request)))]
      (let [{:keys [encoding encoding-opts]} (merge default-transit-opts
                                                    (:transit-opts request))]
        (-> (dissoc request :transit-params)
            (assoc :body (transit-encode params encoding encoding-opts))
            (assoc-in [:headers "content-type"] "application/transit+json")
            (assoc :content-type "application/transit+json")
            (client)))
      (client request))))

(defn wrap-transit-response
  "Decode application/transit+json responses."
  [client]
  (fn [request]
    (let [{:keys [decoding decoding-opts]}
            (merge default-transit-opts (:transit-opts request))
          transit-decode #(transit-decode % decoding decoding-opts)]
      (->> [(client request)]
           (async/map
             #(decode-body % transit-decode "application/transit+json"
                (:request-method request)))))))

(defn wrap-json-params
  "Encode :json-params in the `request` :body and set the appropriate
  Content Type header."
  [client]
  (fn [request]
    (if-let [params (:json-params request)]
      (-> (dissoc request :json-params)
          (assoc :body (json-encode params))
          (assoc-in [:headers "content-type"] "application/json")
          (client))
      (client request))))

(defn wrap-json-response
  "Decode application/json responses."
  [client]
  (fn [request]
    (->> [(client request)]
         (async/map
           #(decode-body % json-decode "application/json"
              (:request-method request))))))

(defn wrap-query-params [client]
  (fn [{:keys [query-params] :as req}]
    (if query-params
        (client (-> req (dissoc :query-params)
                    (assoc :query-string
                      (generate-query-string query-params))))
        (client req))))

(defn wrap-form-params [client]
  (fn [{:keys [form-params request-method] :as request}]
    (if (and form-params (#{:post :put :patch :delete} request-method))
        (client (-> request
                    (dissoc :form-params)
                    (assoc :body (generate-query-string form-params))
                    (assoc-in [:headers "content-type"] "application/x-www-form-urlencoded")))
        (client request))))

(defn generate-form-data [params]
  (let [form-data (js/FormData.)]
    (doseq [[k v] params]
      (.append form-data (name k) v))
    form-data))

(defn wrap-multipart-params [client]
  (fn [{:keys [multipart-params request-method] :as request}]
    (if (and multipart-params (#{:post :put :patch :delete} request-method))
        (client (-> request
                    (dissoc :multipart-params)
                    (assoc :body (generate-form-data multipart-params))))
        (client request))))

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
    (if-let [spec (parse-url (:url req))]
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
      (async/pipe (client request) custom-channel)
      (client request))))

(defn+ wrap-request
  "Returns a batteries-included HTTP request function corresponding to the given
   core client. See client/request"
  [req]
  (-> req
      wrap-accept
      wrap-form-params
      wrap-multipart-params
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
      wrap-default-headers))

(def #^{:doc
        "Executes the HTTP request corresponding to the given map and returns the
   response map for corresponding to the resulting HTTP response.
   In addition to the standard Ring request keys, the following keys are also
   recognized:
   * :url
   * :method
   * :query-params"}
  request! (wrap-request request*))
