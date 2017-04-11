(ns quantum.net.server.middleware
  (:refer-clojure :exclude [assoc-in cat])
  (:require [clj-uuid                                 :as secure-uuid]
            [compojure.handler]
            ; MIDDLEWARE
            [ring.util.anti-forgery                   :as af]
            [ring.util.response                       :as resp]
            [ring.middleware.x-headers                :as x]
            [ring.middleware.gzip               :refer [wrap-gzip]                 ]
            [ring.middleware.session            :refer [wrap-session]              ]
            [ring.middleware.flash              :refer [wrap-flash]                ]
            [ring.middleware.keyword-params     :refer [wrap-keyword-params]       ]
            [ring.middleware.nested-params      :refer [wrap-nested-params]        ]
            [ring.middleware.anti-forgery       :refer [wrap-anti-forgery]         ]
            [ring.middleware.multipart-params   :refer [wrap-multipart-params]     ]
            [ring.middleware.params             :refer [wrap-params]               ]
            [ring.middleware.cookies            :refer [wrap-cookies]              ]
            [ring.middleware.file               :refer [wrap-file]                 ]
            [ring.middleware.not-modified       :refer [wrap-not-modified]         ]
            [ring.middleware.content-type       :refer [wrap-content-type]         ]
            [ring.middleware.default-charset    :refer [wrap-default-charset]      ]
            [ring.middleware.resource           :refer [wrap-resource]             ]
            [ring.middleware.absolute-redirects :refer [wrap-absolute-redirects]   ]
            [ring.middleware.proxy-headers      :refer [wrap-forwarded-remote-addr]]
            [ring.middleware.ssl                :refer [wrap-ssl-redirect
                                                        wrap-hsts
                                                        wrap-forwarded-scheme  ]]
            [ring.middleware.defaults   :as defaults]
            ; QUANTUM
            [quantum.core.string        :as str]
            [quantum.core.error         :as err]
            [quantum.core.logic
              :refer [whenp whenp-> fn-or]]
            [quantum.core.fn
              :refer [fnl fn1 fn-> rcomp fn']]
            [quantum.core.collections   :as coll
              :refer [containsv? assoc-in cat]]
            [quantum.core.log           :as log
              :refer [prl]]
            [quantum.core.print         :as pr
              :refer [ppr-str]]
            [quantum.core.convert       :as conv]
            [quantum.core.async         :as async]))

(def cors? (atom true))

(def cors-headers
  {"Access-Control-Allow-Origin"      "http://localhost:3450"
   "Access-Control-Allow-Credentials" "true" ; can't use boolean... don't know why... ; cannot use wildcard when allow-credentials is true
   "Access-Control-Allow-Headers"     "Content-Type, Accept, Access-Control-Allow-Credentials, Access-Control-Allow-Origin"})

(defn websocket-request? [req] (= (get-in req [:headers "upgrade"]) "websocket"))

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

(defn wrap-uid-with
  [handler f]
  (fn uid-with [req]
    (-> req
        (update-in [:session :uid] #(or % (f req)))
        handler)))

(defn content-security-policy [report-uri & [{:keys [whitelist]}]]
  (str/sp "default-src https: wss: data: gap: " (apply str/sp whitelist) ";"
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
  (fn hide-server [request]
    (when-let [response (handler request)]
      (resp/header response "Server" ""))))

(defn wrap-hide-exception [handler k gen-error]
  (fn hide-exception [req]
    (try (handler req)
      (catch Throwable e
        (log/ppr k "Error in HTTP handler:" e)
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (conv/->json {:error (gen-error e)})}))))

(defn wrap-show-exception [handler k]
  (fn show-exception [req]
    (try (handler req)
      (catch Throwable e
        (log/ppr k "Error in HTTP handler:" e)
        {:status  500
         :headers {"Content-Type" "application/json"}
         :body    (conv/->json {:error (ppr-str (merge {:message (.getMessage e)}
                                                       (when (err/ex-info? e) {:data (ex-data e)})))})}))))

(defn wrap-show-stacktrace [handler k] ; TODO move?
  (fn show-stacktrace [req]
    (try (handler req)
      (catch Throwable e
        (log/ppr k "Error in HTTP handler:" e)
        {:status  500
         :headers {"Content-Type" "application/json"}
         :body    (conv/->json {:error (ppr-str e)})}))))

; ===== REQUEST CONTENT TYPE COERCION ===== ;
; TODO move this?

(def ->content-type
  (rcomp :headers
    (fn-or :content-type (fn1 get "Content-Type") (fn1 get "content-type"))))

(defmulti  coerce-request-content-type ->content-type)
(defmethod coerce-request-content-type :default                 [req] req)
(defmethod coerce-request-content-type "application/text"       [req] (update req :body (fn1 conv/->text)))
(defmethod coerce-request-content-type "text/html"              [req] (update req :body (fn1 conv/->text)))
(defmethod coerce-request-content-type "application/csp-report" [req] (update req :body (fn1 conv/->text)))
(defmethod coerce-request-content-type "application/json"       [req] (update req :body (fn-> conv/->text conv/json->)))

(defn wrap-coerce-request-content-type
  [handler] (fn [request] (handler (coerce-request-content-type request))))

; ===== RESPONSE CONTENT TYPE COERCION ===== ;

(defn update-to-text [req]
  (update req :body
    #(if (or (instance? java.io.InputStream %)
             (instance? java.nio.Buffer     %)
             (instance? java.io.File        %))
         %
         (conv/->text %))))

(defmulti  coerce-response-content-type ->content-type)
(defmethod coerce-response-content-type :default                         [req] req)
(defmethod coerce-response-content-type "application/json"               [req] (update req :body (fn1 conv/->json)))
(defmethod coerce-response-content-type "text/javascript"                [req] (update-to-text req))
(defmethod coerce-response-content-type "text/javascript; charset=utf-8" [req] (update-to-text req)) ; TODO fix this to be more dynamic
(defmethod coerce-response-content-type "application/text"               [req] (update-to-text req))

(defn wrap-coerce-response-content-type
  [handler] (fn [request] (coerce-response-content-type (handler request))))

(defn wrap-in-logging [handler]
  (fn logging [request]
    (log/ppr ::debug "Initial Request" request)
    (let [resp (handler request)]
      (log/ppr ::debug "Final Response" resp)
      resp)))

(defn wrap-out-logging [handler]
  (fn logging [req]
    (log/ppr ::debug "Final Request" req)
    (let [resp (handler req)]
      (log/ppr ::debug "Initial Response" resp)
      resp)))

(defn wrap-logging [handler k]
  (fn [req] (log/ppr ::debug "In" k)
            (let [resp (handler req)]
              (log/ppr ::debug "Out" k)
              resp)))

; TODO this is a hotfix for a particular version of anti-forgery
; It allows for a whitelist of endpoints even when anti-forgery validation fails
(in-ns 'ring.middleware.anti-forgery)

(defn wrap-anti-forgery
  {:arglists '([handler] [handler options])}
  [handler & [{:keys [read-token whitelisted?]
               :or   {read-token default-request-token
                      whitelisted? (fn [_] false)}
               :as   options}]]
  {:pre [(not (and (:error-response options)
                   (:error-handler options)))]}
  (fn [request]
    (binding [*anti-forgery-token* (or (session-token request) (new-token))]
      (if (and (not (whitelisted? request))
               (not (get-request? request))
               (not (valid-request? request read-token)))
        (handle-error options request)
        (if-let [response (handler request)]
          (assoc-session-token response request *anti-forgery-token*))))))

(in-ns 'quantum.net.server.middleware)

(defn wrap-authenticate-ws-client-id
  [handler authentication-fn]
  (fn authenticate-ws-client-id [req]
    (if (websocket-request? req)
        (let [{:as auth :keys [client-id response]} (authentication-fn req)]
          (cond response
                response
                client-id
                (-> req
                    (update :params #(merge % (dissoc auth :response))) ; TODO warn when overwriting
                    handler)
                :else {:status  403
                       :headers {"Content-Type" "application/json"}
                       :body    (conv/->json {:error "WebSocket authentication failed"})}))
        (handler req))))

(defn wrap-middleware [routes & [opts]]
  (let [static-resources-path (-> opts :override-secure-site-defaults (get [:static :resources]))]
    (-> routes
        wrap-out-logging
        (whenp (:figwheel-ws opts)
          (fn1 (do (require '[quantum.net.server.middleware.figwheel])
                   (eval 'quantum.net.server.middleware.figwheel/wrap-figwheel-websocket))
               (:figwheel-ws opts)))
        (whenp static-resources-path
          (fn1 wrap-resource static-resources-path))
        (whenp (:resp-content-type opts) wrap-coerce-response-content-type)
        (whenp (:req-content-type  opts) wrap-coerce-request-content-type )
        (wrap-uid-with (or (:wrap-uid-with opts) (fn [_] (secure-uuid/v1))))
        (whenp (:authenticate-ws-client-id opts)
          (fn1 wrap-authenticate-ws-client-id (:authenticate-ws-client-id opts)))
        (whenp (-> opts :anti-forgery false? not)
          (fn1 wrap-anti-forgery (merge {:read-token (fn [req] (-> req :params :csrf-token))}
                                   (:anti-forgery opts))))
        ; NOTE: Sente requires the Ring |wrap-params| + |wrap-keyword-params| middleware to work.
        (defaults/wrap-defaults
          (apply assoc-in defaults/secure-site-defaults
            [:security :anti-forgery] false
            [:static   :resources   ] false ; This short-circuits the rest of the middleware when placed here
            [:static   :files       ] false
            (-> opts
                :override-secure-site-defaults
                (dissoc [:static :resources])
                cat)))
        wrap-strictest-transport-security
        wrap-x-permitted-cross-domain-policies
        wrap-x-download-options
        wrap-hide-server
        #_(friend/authenticate {:credential-fn #(creds/bcrypt-credential-fn users %)
                              :workflows [(workflows/interactive-form)]})
        wrap-gzip
        #_(friend/requires-scheme :https)
        wrap-in-logging
        (wrap-hide-exception :warn (fn' "Internal")))))
