(ns quantum.net.server.router
  (:require-quantum [:core logic fn err debug log res str coll])
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
              :refer [resources not-found]                         ]
            [compojure.handler                                     ]
            ; MIDDLEWARE
            [ring.util.anti-forgery                   :as af       ]
            [ring.util.response                       :as resp     ]
            [ring.middleware.x-headers :as x]
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
            [ring.middleware.defaults :as defaults]
            ; UTILS
            [com.stuartsierra.component               :as component]
            [clj-uuid                                 :as uuid     ]))




; SECURITY MEASURES TAKEN CARE OF
; CSRF : ring.middleware.anti-forgery

(def sys-map (lens res/systems (fn-> :global :sys-map qcore/deref*)))

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

; ===== HANDLERS =====

(def ring-ajax-get-or-ws-handshake (lens sys-map (fn-> :connection :ajax-get-or-ws-handshake-fn)))
(def ring-ajax-post                (lens sys-map (fn-> :connection :ajax-post-fn)))

#_(def index-file (slurp "resources/public/index.html"))
(def index-file "")

(def not-found-page "<h1>Page not found. Sorry!</h1>")

; This is why you serve your page dynamically, so you can place the anti forgery field there 
(defn token-index [req]
  (str/replace index-file #"token-string" (af/anti-forgery-field)))

(defn login!
  "Here's where you'll add your server-side login/auth procedure (Friend).
  In our simplified example we'll just always successfully authenticate the user
  with whatever user-id they provided in the auth request."
  [ring-request]
  (let [{:keys [session params]} ring-request
        {:keys [user-id]} params]
    {:status 200 :session (assoc session :uid user-id)}))

; ===== ROUTES =====

(def chan-uri "/chan")

; This seems really useful
#_(defn create-api []
  ["/"                   
   [
    ["" (bidi.ring/redirect "index.html")]
    ["favicon.ico" (yada nil)]
    ["" (yada.yada/yada (io/file "target/dev"))]
    ]])

#_(bidi.ring/make-handler (create-api))

(def server-root (str/->path (System/getProperty "user.dir") "/dev-resources/public"))
(def main-page (slurp (str/->path server-root "index.html")))

(defn resources+
  "A route for serving resources on the classpath. Accepts the following
  keys:
    :root       - the root prefix path of the resources, defaults to 'public'
    :mime-types - an optional map of file extensions to mime types"
  [path & [options]]
  (GET (@#'compojure.route/add-wildcard path) {{resource-path :*} :route-params :as req}
    (let [root (:root options "public")
          body (if (contains? #{"js/compiled/system.js"} resource-path)
                   ""
                   (->> resource-path
                        (<- str/remove "..") ; to prevent weird things
                        (str/->path root)
                        (java.io.FileInputStream.)))]
      {:body body}
      #_(add-mime-type resource-path options))))

(defroutes app-routes
  (GET "/"        req (fn [req]
                        (when (-> req :query-params :user (= "alex"))
                          {:content-type "text/html"
                           :body main-page})) #_(friend/authenticated (#'token-index req)))
  (GET "/admin"   req (friend/authorize #{::admin}
                        #_any-code-requiring-admin-authorization
                        "Admin page."))
  ; Accessible by anonymous users. 
  (GET  "/login"  req (login! req))
  ; Accessible by anonymous users. 
  (GET  "/logout" req (friend/logout* (resp/redirect (str (:context req) "/login"))))
  ; If the page serving the JavaScript (ClojureScript) is running
  ; HTTPS, the Sente channel sockets will run over HTTPS and/or
  ; the WebSocket equivalent (WSS).
  (GET  chan-uri  req (do #_friend/authenticated
                        (let [get-f @ring-ajax-get-or-ws-handshake]
                          (assert (nnil? get-f))
                          (get-f req))))
  (POST chan-uri  req (do #_friend/authenticated
                        (let [post-f @ring-ajax-post]
                          (assert (nnil? post-f))
                          (post-f req))))
  (resources+ "/" {:root server-root}) ; static files
  (not-found not-found-page))

(defn wrap-middleware [routes]
  (-> routes
      wrap-uid
      (wrap-anti-forgery {:read-token (fn [req] (-> req :params :csrf-token))})
      (defaults/wrap-defaults
        (assocs-in+ defaults/secure-site-defaults
          [:security :anti-forgery] false
          [:static   :resources   ] false
          [:static   :files       ] false))
      #_(friend/authenticate {:credential-fn #(creds/bcrypt-credential-fn users %)
                            :workflows [(workflows/interactive-form)]})
      ; Sente requires the Ring |wrap-params| + |wrap-keyword-params| middleware to work.
      wrap-gzip
      compojure.handler/site ; ?
      #_(friend/requires-scheme :https))) ; TODO make HTTPS work

(defroutes routes (wrap-middleware app-routes))


(comment
 "Blob storage: instead of transmitting the same data twice, simply
  asks for an authentication key from the database and uploads directly to the blob
  storage using that authentication key.    

  Unfortunately the same is not true of things one wishes to put in the database.
  That data must be sent twice because the database is not REST-accessible (possibly
  thank goodness). But luckily there is not much data that need be sent this way.
 
  Find out whether AWS, Azure, or Google is cheaper (for storage, specifically). 
  
  ")