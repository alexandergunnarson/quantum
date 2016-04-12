(ns quantum.net.server.router
  (:require-quantum [:core logic fn err debug log res])
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
            [ring.middleware.anti-forgery             :as maf      ]
            [ring.middleware.params                   :as params   ]
            [ring.middleware.gzip                     :as gzip     ]
            [ring.middleware.keyword-params           :as kw-params]
            [ring.middleware.session                  :as session  ]
            ; UTILS
            [com.stuartsierra.component               :as component]
            [clojure.string                           :as str      ]
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

(defroutes app-routes
  (GET "/"        req (friend/authenticated (#'token-index req)))
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
  (resources "/")  ; static files
  (not-found not-found-page))

(defn wrap-middleware [routes]
  (-> routes
      wrap-uid
      (maf/wrap-anti-forgery {:read-token (fn [req] (-> req :params :csrf-token))})
      (friend/authenticate {:credential-fn #(creds/bcrypt-credential-fn users %)
                            :workflows [(workflows/interactive-form)]})
      ; Sente requires the Ring |wrap-params| + |wrap-keyword-params| middleware to work.
      kw-params/wrap-keyword-params
      params/wrap-params
      gzip/wrap-gzip
      session/wrap-session
      compojure.handler/site
      #_(friend/requires-scheme :https))) ; TODO make HTTPS work

(defroutes routes (wrap-middleware app-routes))
