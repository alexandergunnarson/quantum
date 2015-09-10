(ns
  ^{:doc "Some useful HTTP functions. HTTP request processing with
          error handling, log writing, etc."
    :attribution "Alex Gunnarson"}
  quantum.http.core
  (:require-quantum [:lib])
  (:require
    [org.httpkit.client     :as http])
  (:import
    org.apache.http.entity.mime.MultipartEntityBuilder
    org.apache.http.entity.ContentType
    org.apache.http.client.methods.HttpPost
    org.apache.http.client.methods.HttpEntityEnclosingRequestBase
    org.apache.http.impl.client.DefaultHttpClient
    java.io.File))

; http://www.december.com/html/spec/httpstat.html

(defrecord HTTPLogEntry [^Vec tries])
; TODO don't use a global log...
(defonce http-log (atom {}))

; TODO Implement Resumable uploads: https://developers.google.com/drive/web/manage-uploads
;___________________________________________________________________________________________________________________________________
;================================================={              LOG              }=================================================
;================================================={                               }=================================================
(defn log-entry-write!
  {:usage "log: {:request  time
                 :tries    [{:response time :status 401}]
                 :response time}"
   :attribution "Alex Gunnarson"
   :todo ["Likely needs to be rewritten"]}
  [^Atom log log-type ^Number tries & [status]]
  (let [to-conj
          (if (or (and (= tries 0) (= log-type :request)) ; initial request
                  (= status "OK"))                        ; or final response 
              [[log-type] (time/now) #_(time-loc/local-now)]
              (concat (when (= tries 0)
                        [[:tries tries :request]
                        (:request @log)])
                [[:tries tries log-type] (time/now) #_(time-loc/local-now)
                 [:tries tries :status]  status]))]
    (reset! log (apply assocs-in+ @log to-conj))))
;___________________________________________________________________________________________________________________________________
;================================================={     PROCESS HTTP REQUEST      }=================================================
;================================================={                               }=================================================
(defn ^String normalize-mime-type [mime-type]
  (whenc mime-type (fn-not (fn-or string? nil?))
    (condp = mime-type
      :json "application/json"
      :jpeg "image/jpeg"
      (throw+ (str/sp "Unknown mime type:" mime-type)))))

(defn ^String normalize-encoding-type [encoding]
  (whenc encoding (fn-not (fn-or string? nil?))
    (condp = encoding
      :utf-8 "UTF-8"
      (throw+ (str/sp "Unknown encoding type:" encoding)))))

(defn normalize-content [content ^String mime-type ^String encoding]
  (condp = mime-type
    "application/json"
      (json/encode content)
    content))
;___________________________________________________________________________________________________________________________________
;================================================={       NORMALIZE PARAMS        }=================================================
;================================================={                               }=================================================
(defn add-part!
  {:todo []}
  [^MultipartEntityBuilder meb
   {:keys [^String name mime-type encoding content]}]
  (let [^String mime-type-f (normalize-mime-type     mime-type)
        ^String encoding-f  (normalize-encoding-type encoding)
        content-f           (normalize-content content mime-type encoding)]
    (cond
    (string? content-f)
      (.addTextBody meb
        name
        ^String content
        (ContentType/create mime-type-f encoding-f))
    (file? content-f)
      (.addBinaryBody meb
        name
        ^File content)
    :else
      (throw+ (str/sp "Unknown content type:" (class content))))))

(defn add-header!
  [^HttpEntityEnclosingRequestBase req [header-name ^String content]]
  (condp = header-name
    :oauth-token
      (.addHeader req
        (org.apache.http.message.BasicHeader.
          "authorization"
          (str/sp "Bearer" content)))
    (throw+ (str/sp "Unknown header type:" header-name))))

(defn add-headers!
  {:todo ["Add support for all headers"]}
  [^HttpEntityEnclosingRequestBase req ^Map headers]
  (doseq [header headers]
    (add-header! req header)))

(defn post-multipart!
  {:todo ["Integrate this with the rest of clj-http"]}
  [{:keys [^String url ^Map multipart
           ^String oauth-token
           ^Map    headers]}]
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
      )))

(defn request!*
  {:todo ["Integrate this with the rest of clj-http"]}
  [{:keys [^Key method ^Map multipart] :as req}]
  (if (nnil? multipart)
      (condp = method
        :post (post-multipart! req)
        (throw+ (str/sp "Method" (str/squote method)
                 "not a valid HTTP request type.")))
      @(http/request req))) ; |deref| because it's a |promise|

#_(def http-response-map
  {401 :unauthorized
   403 :too-many-requests
   500 :server-error})

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
         :handlers {:default (fn [req resp] (throw+ (str/sp "Error:" (:status response))))
                    404      (fn [req resp] (throw+ "404 exception"))
                    401      (fn [req resp]
                               (refresh-access!)
                               (http/request! (assoc req :new-access-key 123)))}
         :multipart
           [{:name "file"
             :mime-type :json
             :encoding :utf-8
             :content (json/encode {:title "My File 2"})}
            {:name "file1"
             :mime-type "image/jpeg"
             :content
               (io/file
                 [:home "Collections" "Images" "Backgrounds" "3331-11.jpg"])}]})
    :attribution "Alex Gunnarson"}
  [{:as req
    :keys [handlers log? log tries max-tries parse?]
    :or {as :auto
         max-tries 3
         tries     0}}]
  (if (= tries max-tries)
      (throw+ (Err. :error/http
                (str "HTTP exception, status " (:status req) ". Maximum tries (3) exceeded.")
                (:status req)))
      (let [request-write!  (when log? (log-entry-write! (or log ) :request tries))
            response        (request!* (dissoc req :status :log))
            status          (:status response)]
        (-> (if (or (= status 200) (= status 201))
              response
              (let [status-handler
                     (or (get handlers status)
                         (get handlers :default)
                         (constantly
                           (do (log/pr :warn "unhandled HTTP status:" status) response)))
                    req-n+1
                      (assoc req
                        :tries (inc tries)
                        :max-tries max-tries) ]
                (status-handler req-n+1 response)))
            (whenf (fn-and (constantly parse?)
                           (fn-> :headers :content-type (containsv? "application/json")))
              (fn-> :body (json/parse-string str/keywordize)))))))

(defnt ping!
  ([^string? url] (with-open [socket (java.net.Socket. url 80)] socket)))

(defn network-connected? []
  (try (ping! "www.google.com")
       true
    (catch java.net.SocketException e
      (if (-> e .getMessage (= "Network is unreachable"))
          false
          :unknown))
    (catch java.net.UnknownHostException _ false)))

 ; Success (2xx)  
 ; Redirection (3xx)  
 ; Server errors (5xx)
 ; Client errors (4xx)

(defn download
  {:todo ["Show progress" "Get size of download beforehand" "Maybe use ->file"]}
  [{:keys [file-str out req]}]
  (let [^org.httpkit.BytesInputStream is
          (->> (request! req)
               :body)
        _ (assert (instance? org.httpkit.BytesInputStream is))
        ^OutputStream os
          (or out (FileOutputStream. ^File (io/file file-str)))
        buffer (byte-array 1024000)] ; ~1 MB buffer
    (let-mutable [len (int 0)]
      (set! len (.read is buffer)) 
      (while (not= len (unchecked-int -1))
        (.write os buffer 0 len)
        (set! len (.read is buffer))))
    (.close os)
    (.close is)))

(defn parse-json [resp]
  (-> resp :body (json/parse-string str/keywordize)))