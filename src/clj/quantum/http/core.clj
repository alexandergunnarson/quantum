(ns
  ^{:doc "Some useful HTTP functions. HTTP request processing with
          error handling, log writing, etc."
    :attribution "Alex Gunnarson"}
  quantum.http.core
  (:require
    [quantum.core.ns  :as ns :refer :all]
    [quantum.core.data.json    :as json]
    [org.httpkit.client :as http])
  (:import
    org.apache.http.entity.mime.MultipartEntityBuilder
    org.apache.http.entity.ContentType
    org.apache.http.client.methods.HttpPost
    org.apache.http.client.methods.HttpEntityEnclosingRequestBase
    org.apache.http.impl.client.DefaultHttpClient
    java.io.File)
  (:gen-class))

(ns/require-all *ns* :lib :clj)

(def ^:dynamic *max-tries-http* 3)

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
              [[log-type] (time-loc/local-now)]
              (concat (when (= tries 0)
                        [[:tries tries :request]
                        (:request @log)])
                [[:tries tries log-type] (time-loc/local-now)
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

(defn request!
  "'Safe' because it handles various HTTP errors (401, 403, 500, etc.),
   and limits retries at |http-lib/*max-tries-http*| (which defaults at 3)."
   {:todo  ["EOFException SSL peer shut down incorrectly  sun.security.ssl.InputRecord.read
             INFO: I/O exception (java.net.SocketException) caught when connecting to
                   {s}->https://www.googleapis.com: Connection reset"
            "Probably should be rewritten."]
    :usage-0 '(proc-request! 0 nil {:req ...} (atom {:tries []}))
    :usage
     '(request!
        {:method :post
         :url         "https://www.googleapis.com/upload/drive/v2/files"
         :oauth-token (access-key :drive :offline)
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
    :or {as :auto}}
   &
   {:keys [^Int tries ^Atom log ^Fn handler status]
    :or {tries   0
         log     http-log
         handler vector}}]
  (if (= tries *max-tries-http*)
      (throw+ {:type :http
               :msg (str "HTTP exception, status " status ". Maximum tries (3) exceeded.")})
      (let [request-write!  (log-entry-write! log :request  tries)
            response        (trampoline request!* req) ; is |trampoline| wise here?
            response-write! (log-entry-write! log :response tries "OK")]  ; this is not executed if an exception happens
        (condf response
          (compr :status (f*n splice-or = 401 403 500))
            #(handler tries (:status %) req log)
          :else
            identity))))






