(ns quantum.net.server.router
  (:require ; AUTHENTICATION
            [cemerick.friend                          :as friend   ]
            [cemerick.friend.workflows                :as workflows]
            [cemerick.friend.credentials              :as creds    ]
            ; WEBSOCKETS
            [taoensso.sente                           :as ws       ]
            ; ROUTING
            [compojure.core                           :as route
              :refer [GET ANY POST defroutes]                      ]
            [compojure.route
              :refer [not-found]                                   ]
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
            ; UTILS
            [com.stuartsierra.component :as component]
            [clj-uuid                   :as uuid     ]
            [quantum.core.string        :as str      ]
            [quantum.core.log           :as log      ]
            [quantum.core.resources     :as res      ]
            [quantum.core.collections   :as coll     
              :refer [containsv? assocs-in+]         ]
            [quantum.core.core          :as qcore    
              :refer [lens]                          ]
            [quantum.core.logic         :as logic    
              :refer [nnil?]                         ]
            [quantum.core.fn            :as fn    
              :refer [<- fn->]                       ]))

; SECURITY MEASURES TAKEN CARE OF
; CSRF : ring.middleware.anti-forgery

; ===== ACCESS CONTROL (ROLES) =====

(def users {"admin" {:username "admin"
                     :password (creds/hash-bcrypt "admin")
                     :roles    #{::admin}}})

; ===== MIDDLEWARE =====

(defn wrap-uid
  [app]
  (fn [req]
    (if-not (get-in req [:session :uid])
      (app (assoc-in req [:session :uid] (uuid/v1)))
      (app req))))

; This is why you serve your page dynamically, so you can place the anti forgery field there 
(defn token-index [req]
  (str/replace index-file #"token-string" (af/anti-forgery-field)))

; ===== ROUTES =====

; This seems really useful
#_(defn create-api []
  ["/"                   
   [
    ["" (bidi.ring/redirect "index.html")]
    ["favicon.ico" (yada nil)]
    ["" (yada.yada/yada (io/file "target/dev"))]
    ]])

#_(bidi.ring/make-handler (create-api))

(defn resources
  "A route for serving resources on the classpath. Accepts the following keys:
    :root       - the root prefix path of the resources, defaults to 'public'
    :mime-types - an optional map of file extensions to mime types
   (This is an improved version of Compojure's |resources| fn.)"
  [path & [options]]
  (GET (@#'compojure.route/add-wildcard path) {{resource-path :*} :route-params :as req}
    (let [root (get options :root "public")
          _ (println "RESOURCE PATH" resource-path)
          body (->> resource-path
                    (<- str/remove "..") ; to prevent insecure access
                    ^String (str/->path root)
                    (java.io.FileInputStream.))]
      {:body body} ; TODO add content-type  
      #_(add-mime-type resource-path options))))

(defn content-security-policy [report-uri]
  (str/sp "default-src https: data: gap: https://ssl.gstatic.com;"
          "style-src    'self' 'unsafe-inline';"
          "script-src   'self' 'unsafe-inline';"
          "font-src     'self';"
          "form-action  'self';"
          "reflected-xss block;"
          "report-uri" (str report-uri ";")))

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
      (resp/header response "Server" "nil"))))

(defn wrap-middleware [routes]
  (-> routes
      wrap-uid
      (wrap-anti-forgery {:read-token (fn [req] (-> req :params :csrf-token))})
      (defaults/wrap-defaults
        (assocs-in+ defaults/secure-site-defaults
          [:security :anti-forgery] false
          [:static   :resources   ] false
          [:static   :files       ] false))
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
      wrap-exception-handling))

; ROUTES PRESETS

(defn ws-routes
  [{:keys [ws-uri get-fn post-fn]}]
  (assert (fn? get-fn )) ; TODO use clojure.spec
  (assert (fn? post-fn))
  (assert (string? ws-uri))
  [(GET  ws-uri req (get-fn  req))
   (POST ws-uri req (post-fn req))])

(defn csp-report-route
  [{:keys [csp-report-uri csp-report-handler]}]
  [(POST csp-report-uri req csp-report-handler)])

(defn not-found-route
  [opts]
  (or (:not-found-handler opts)
      (fn [req]
        {:status 404
         :body   (when-not (= (:request-method req) :head)
                   "<h1>Page not found.<h1>")})))

(defn routes
  [{:keys [ws-uri csp-report-uri
           root-path]
    :as opts}]
  (concat (when ws-uri (ws-routes opts))
          ((:routes-fn opts) opts)
          (when csp-report-uri (csp-report-route opts))
          [(resources "/" {:root root-path}) ; static files
           (not-found-route opts)]))

(defn make-routes
  [{:keys [middleware]
    :as opts}]
  (log/ppr :debug "Making routes with" opts)
  (middleware
    (apply route/routes (routes opts))))

(comment
 "Blob storage: instead of transmitting the same data twice, simply
  asks for an authentication key from the database and uploads directly to the blob
  storage using that authentication key.    

  Unfortunately the same is not true of things one wishes to put in the database.
  That data must be sent twice because the database is not REST-accessible (possibly
  thank goodness).
 
  Find out whether AWS, Azure, or Google is cheaper (for storage, specifically). 
  
  ")