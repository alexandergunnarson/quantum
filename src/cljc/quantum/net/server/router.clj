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
            ; UTILS
            [com.stuartsierra.component :as component]
            [quantum.net.server.middleware :as mid   ]
            [quantum.validate.core         :as v     ]
            [quantum.core.string           :as str   ]
            [quantum.core.log              :as log   ]
            [quantum.core.resources        :as res   ]
            [quantum.core.paths            :as paths ]
            [quantum.core.core             :as qcore 
              :refer [lens]                          ]
            [quantum.core.logic            :as logic 
              :refer [nnil?]                         ]
            [quantum.core.fn               :as fn    
              :refer [<- fn->]                       ]))

; SECURITY MEASURES TAKEN CARE OF
; CSRF : ring.middleware.anti-forgery

; ===== ACCESS CONTROL (ROLES) =====

(def users {"admin" {:username "admin"
                     :password (creds/hash-bcrypt "admin")
                     :roles    #{::admin}}})

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
                    ^String (paths/url-path root)
                    (java.io.FileInputStream.))]
      {:body body} ; TODO add content-type  
      #_(add-mime-type resource-path options))))

; ROUTES PRESETS

(defn ws-routes
  [{:keys [ws-uri get-fn post-fn]}]
  (v/validate fn?     get-fn )
  (v/validate fn?     post-fn)
  (v/validate string? ws-uri)
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