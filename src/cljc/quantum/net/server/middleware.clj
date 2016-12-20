(ns quantum.net.server.middleware
  (:require [clj-uuid :as uuid     ]
            [compojure.handler                                     ]
            ; MIDDLEWARE
            [ring.util.anti-forgery                   :as af       ]
            [ring.util.response                       :as resp     ]
            [ring.middleware.x-headers                :as x        ]
            [ring.middleware.gzip               :refer [wrap-gzip]                 ]
            [ring.middleware.session            :refer [wrap-session]              ]
            [ring.middleware.flash              :refer [wrap-flash]                ]
            [ring.middleware.keyword-params     :refer [wrap-keyword-params]       ]
            [ring.middleware.nested-params      :refer [wrap-nested-params]        ]
            [ring.middleware.anti-forgery       :refer [wrap-anti-forgery]         ]
            [ring.middleware.multipart-params   :refer [wrap-multipart-params]     ]
            [ring.middleware.params             :refer [wrap-params]               ]
            [ring.middleware.cookies            :refer [wrap-cookies]              ]
            [ring.middleware.resource           :refer [wrap-resource]             ]
            [ring.middleware.file               :refer [wrap-file]                 ]
            [ring.middleware.not-modified       :refer [wrap-not-modified]         ]
            [ring.middleware.content-type       :refer [wrap-content-type]         ]
            [ring.middleware.default-charset    :refer [wrap-default-charset]      ]
            [ring.middleware.absolute-redirects :refer [wrap-absolute-redirects]   ]
            [ring.middleware.proxy-headers      :refer [wrap-forwarded-remote-addr]]
            [ring.middleware.ssl                :refer [wrap-ssl-redirect
                                                        wrap-hsts
                                                        wrap-forwarded-scheme  ]]
            [ring.middleware.defaults   :as defaults]
            ; QUANTUM
            [quantum.core.string        :as str]
            [quantum.core.logic
              :refer [whenp fn-or]]
            [quantum.core.fn
              :refer [fn1 fn-> rcomp]]
            [quantum.core.collections   :as coll
              :refer [containsv? assocs-in+ flatten-1]]
            [quantum.core.log           :as log]
            [quantum.core.convert       :as conv]))

(def cors? (atom true))

(def cors-headers
  {"Access-Control-Allow-Origin"      "http://localhost:3450"
   "Access-Control-Allow-Credentials" "true" ; can't use boolean... don't know why... ; cannot use wildcard when allow-credentials is true
   "Access-Control-Allow-Headers"     "Content-Type, Accept, Access-Control-Allow-Credentials, Access-Control-Allow-Origin"})

#_(defn wrap-keywordify [f]
  (fn [req]
    (f (-> req
           (update :query-params coll/keywordify-keys)
           (update :params       coll/keywordify-keys)
           (update :headers      coll/keywordify-keys)))))

#_(defn wrap-cors-resp [f]
  (fn [req]
    (let [resp (f req)]
      (assoc resp :headers
        (if @cors?
            (mergel (:headers resp) cors-headers)
            (:headers resp))))))

(defn wrap-uid
  [app]
  (fn [req]
    (if-not (get-in req [:session :uid])
      (app (assoc-in req [:session :uid] (uuid/v1)))
      (app req))))

(defn content-security-policy [report-uri]
  (str/sp "default-src https: data: gap: https://ssl.gstatic.com;"
          "style-src    'self' 'unsafe-inline';"
          "script-src   'self' 'unsafe-inline';"
          "font-src     'self';"
          "form-action  'self';"
          "reflected-xss block;"
          "report-uri" (str report-uri ";")))

; TODO repetitive

(defn wrap-x-permitted-cross-domain-policies
  {:doc "Recommended implicitly by https://github.com/twitter/secureheaders"}
  [handler]
  (fn [request]
    (when-let [response (handler request)]
      (resp/header response "X-Permitted-Cross-Domain-Policies" "none" #_"master-only")))) ; either one is fine; Twitter uses "none"

(defn wrap-x-download-options
  {:doc "Recommended implicitly by https://github.com/twitter/secureheaders"}
  [handler]
  (fn [request]
    (when-let [response (handler request)]
      (resp/header response "X-Download-Options" "noopen"))))

(defn wrap-strictest-transport-security
  {:doc "Considered the 'most secure' STS setting"}
  [handler]
  (fn [request]
    (when-let [response (handler request)]
      (resp/header response "Strict-Transport-Security" "max-age=10886400; includeSubDomains; preload"))))

(defn wrap-hide-server
  [handler]
  (fn [request]
    (when-let [response (handler request)]
      (resp/header response "Server" ""))))

(defn wrap-exception-handling
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable e
        (log/ppr :warn "Error in HTTP handler:" e)
        {:status 500
         :headers {"Content-Type" "text/html"}
         :body "<html><div>Something didn't go quite right.</div><i>HTTP error 500</div></html>"}))))

; ===== REQUEST CONTENT TYPE COERCION ===== ;
; TODO move this?

(def ->content-type
  (rcomp :headers
    (fn-or :content-type (fn1 get "Content-Type") (fn1 get "content-type"))))

(defmulti  coerce-request-content-type ->content-type)
(defmethod coerce-request-content-type :default           [req] req)
(defmethod coerce-request-content-type "application/text" [req] (update req :body (fn1 conv/->text)))
(defmethod coerce-request-content-type "text/html"        [req] (update req :body (fn1 conv/->text)))
(defmethod coerce-request-content-type "application/json" [req] (update req :body (fn-> conv/->text conv/json->)))

(defn wrap-coerce-request-content-type
  [handler] (fn [request] (handler (coerce-request-content-type request))))

; ===== RESPONSE CONTENT TYPE COERCION ===== ;

(defmulti  coerce-response-content-type ->content-type)
(defmethod coerce-response-content-type :default           [req] req)
(defmethod coerce-response-content-type "application/json" [req] (update req :body (fn1 conv/->json)))
(defmethod coerce-response-content-type "application/text" [req]
  (update req :body
    #(if (or (instance? java.io.InputStream %)
             (instance? java.nio.Buffer     %)
             (instance? java.io.File        %))
         %
         (conv/->text %))))

(defn wrap-coerce-response-content-type
  [handler] (fn [request] (coerce-response-content-type (handler request))))

(defn wrap-logging [handler]
  (fn [request]
    (log/ppr ::debug "Request" request)
    (let [resp (handler request)]
      (log/ppr ::debug "Response" resp)
      resp)))

; ===== MIDDLEWARE ===== ;

(defn wrap-middleware [routes & [opts]]
  (-> routes
      #_wrap-logging
      (whenp (:resp-content-type opts) wrap-coerce-response-content-type)
      (whenp (:req-content-type  opts) wrap-coerce-request-content-type )
      wrap-uid
      (whenp (-> opts :anti-forgery false? not)
        (fn1 wrap-anti-forgery {:read-token (fn [req] (-> req :params :csrf-token))}))
      (defaults/wrap-defaults
        (apply assocs-in+ defaults/secure-site-defaults
          [:security :anti-forgery] false
          [:static   :resources   ] false
          [:static   :files       ] false
          (-> opts
              :override-secure-site-defaults
              flatten-1)))
      wrap-strictest-transport-security
      wrap-x-permitted-cross-domain-policies
      wrap-x-download-options
      wrap-hide-server
      #_(friend/authenticate {:credential-fn #(creds/bcrypt-credential-fn users %)
                            :workflows [(workflows/interactive-form)]})
      ; Sente requires the Ring |wrap-params| + |wrap-keyword-params| middleware to work.
      wrap-gzip
      compojure.handler/site ; ?
      #_(friend/requires-scheme :https)
      wrap-logging
      wrap-exception-handling))
