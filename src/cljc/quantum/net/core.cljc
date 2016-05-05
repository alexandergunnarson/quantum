(ns quantum.net.core
           (:require 
            #?(:cljs [goog.userAgent           :as agent      ])
            #?(:cljs [goog.Uri                 :as uri        ])
                     [quantum.core.error       :as err
                       :refer [->ex]                          ]
                     [quantum.core.collections :as coll       ]
                     [quantum.core.string      :as str        ]
                     [quantum.core.convert     :as conv       ]
                     [quantum.core.logic       :as logic
                       :refer [#?@(:clj [fn-not fn-or whenc])]])
  #?(:cljs (:require-macros
                     [quantum.core.logic       :as logic
                       :refer [fn-not fn-or whenc]            ]))
  #?(:clj  (:import  (java.net URLEncoder URLDecoder))))

; UTILS

(defn valid-port? [x]
  (and (integer? x) (>= x 0) (<= x 65536)))

(defn user-agent
  "Returns the user agent."
  [] #?(:clj  (throw (->ex :not-implemented))
        :cljs (agent/getUserAgentString)))

; TODO user agent can be spoofed
(defn android?
  "Returns true if the user agent is an Android client."
  {:todo ["Rename?"]}
  [] (re-matches #"(?i).*android.*" (user-agent)))

; ===== STATUS =====

(def unexceptional-status?
  #{200 201 202 203 204 205 206 207 300 301 302 303 307})

(def http-response-map
  {401 :unauthorized
   403 :too-many-requests
   500 :server-error})

; ===== URL =====

(def url-regex #"([^:]+)://(([^:]+):([^@]+)@)?(([^:/]+)(:([0-9]+))?((/[^?]*)(\?([^#]*))?)?)(\#(.*))?")

; ===== PORTS =====

(def port-number
  {:http 80
   :https 443
   :mysql 3306
   :postgresql 5432
   :rabbitmq 5672})

; ===== CONTENT TYPE =====

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

(defn ^String mime-type->str
  {:in-types '{mime-type Keyword}}
  [mime-type]
  (whenc mime-type (fn-not (fn-or string? nil?)) ; |fn-not| gives an ExceptionInInitializer error
    (condp = mime-type
      :json "application/json"
      :jpeg "image/jpeg"
      (throw (->ex :illegal-argument "Unknown mime type" mime-type)))))

(defn ^String normalize-encoding-type [encoding]
  (whenc encoding (fn-not (fn-or string? nil?))
    (condp = encoding
      :utf-8 "UTF-8"
      (throw (->ex :illegal-argument "Unknown encoding type" encoding)))))

(defn normalize-content
  "Transforms @content to the appropriate format of @mime-type."
  {:todo ["Use a content normalization map, mime-type->coercion-fn"]}
  [content ^String mime-type]
  (condp = mime-type
    "application/json"
      (conv/->json content)
    content))

; ===== URL =====

(defn url-encode
  "Returns `s` as an URL encoded string."
  [s & [encoding]]
  (when s
    #?(:clj (-> (URLEncoder/encode (str s) (or encoding "UTF-8"))
                (str/replace "%7E" "~")
                (str/replace "*" "%2A")
                (str/replace "+" "%20"))
       :cljs (-> (js/encodeURIComponent (str s))
                 (str/replace "*" "%2A")))))

; ;; Credits: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent
; ;; TODO: Fix it in cemerick.url. (RM 2015-07-02)
; (defn url-encode [s]
;   (apply str
;          (map (fn [c] (if (contains? #{\! \' \( \) \*} c)
;                         (str \% (-> c (.charCodeAt 0) (.toString 16)))
;                         c))
;               (cemerick.url /url-encode s))))

(defn url-decode
  "Returns `s` as an URL decoded string."
  [s & [encoding]]
  (when s
    #?(:clj (URLDecoder/decode s (or encoding "UTF-8"))
       :cljs (js/decodeURIComponent s))))

(declare format-query-params parse-query-params)

(defn format-url
  "Format the Ring map as an url."
  {:from "r0man/noencore"}
  [m]
  (if (not (empty? m))
    (let [query-params (:query-params m)]
      (str (if (:scheme m)
             (str (name (:scheme m)) "://"))
           (let [{:keys [user password]} m]
             (if user (str (if user user) (if password (str ":" password)) "@")))
           (:server-name m)
           (if-let [port (:server-port m)]
             (if-not (= port (port-number (:scheme m)))
               (str ":" port)))
           (if (and (nil? (:uri m))
                    (not (empty? query-params)))
             "/" (:uri m))
           (if-not (empty? query-params)
             (str "?" (format-query-params query-params)))
           (if-not (str/blank? (:fragment m))
             (str "#" (:fragment m)))))))

#?(:clj
(defn parse-url
  "Parse the url `s` and return a Ring compatible map."
  {:from "r0man/noencore"}
  [s]
  (if-let [matches (re-matches url-regex (str s))]
    (let [scheme (keyword (nth matches 1))]
      (coll/compact-map
       {:scheme scheme
        :user         (nth matches 3)
        :password     (nth matches 4)
        :server-name  (nth matches 6)
        :server-port  (or (conv/parse-integer (nth matches 8)) (port-number scheme))
        :uri          (nth matches 10)
        :query-params (parse-query-params  (nth matches 12))
        :query-string (nth matches 12)
        :fragment     (nth matches 14)})))))

#?(:cljs
(defn parse-url
  "Parse @url into a hash map."
  [url]
  (if-not (str/blank? url)
    (let [uri (uri/parse url)
          query-data (.getQueryData uri)]
      {:scheme (keyword (.getScheme uri))
       :server-name (.getDomain uri)
       :server-port (when-let [p (.getPort uri)
                               _ (pos? p)] 
                      p)
       :uri         (.getPath uri)
       :query-string (if-not (.isEmpty query-data)
                       (str query-data))
       :query-params (if-not (.isEmpty query-data)
                       (parse-query-params (str query-data)))}))))

; ===== QUERY PARAMS =====


(defn parse-query-params
  "Parse the query parameter string `s` and return a map."
  [s]
  (when s
    (->> (str/split (str s) #"&")
         (map #(str/split %1 #"="))
         (filter #(= 2 (count %1)))
         (mapcat #(vector (keyword (url-decode (first %1))) (url-decode (second %1))))
         (apply hash-map))))

(defn format-query-params
  "Format the map `m` into a query parameter string."
  {:from "r0man/noencore"}
  [m]
  (let [params (->> (sort-by first (seq m))
                    (remove #(str/blank? (str (second %1))))
                    (map #(vector (url-encode (name (first %1)))
                                  (url-encode (second %1))))
                    (map #(str/join "=" %1))
                    (str/join "&"))]
    (if-not (str/blank? params)
      params)))





 ; Success (2xx)  
 ; Redirection (3xx)  
 ; Server errors (5xx)
 ; Client errors (4xx)

#_(defn download
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

#_(defn parse-json [resp]
  (-> resp :body (json/parse-string str/keywordize)))