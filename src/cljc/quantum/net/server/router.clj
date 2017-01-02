(ns quantum.net.server.router
  (:require
    ; AUTHENTICATION
    [cemerick.friend                  :as friend]
    [cemerick.friend.workflows        :as workflows]
    [cemerick.friend.credentials      :as creds]
    ; WEBSOCKETS
    [taoensso.sente                   :as ws]
    ; ROUTING
    [compojure.core                   :as route
      :refer [GET ANY POST defroutes]]
    [compojure.route
      :refer [not-found]]
    ; UTILS
    [com.stuartsierra.component       :as component]
    [quantum.net.server.middleware    :as mid]
    [quantum.core.validate            :as v
      :refer [validate]]
    [quantum.core.string              :as str]
    [quantum.core.collections.base
      :refer [nnil?]]
    [quantum.core.log                 :as log]
    [quantum.core.resources           :as res]
    [quantum.core.paths               :as paths]
    [quantum.core.core                :as qcore
      :refer [lens]]
    [quantum.core.fn                  :as fn
      :refer [<- fn->]]))

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
          body (->> resource-path
                    (<- str/remove "..") ; to prevent insecure access
                    ^String (paths/url-path root)
                    (java.io.FileInputStream.))]
      {:body body} ; TODO add content-type
      #_(add-mime-type resource-path options))))

; ROUTES PRESETS

(defn ws-routes
  [{:keys [ws-uri get-fn post-fn]}]
  (validate get-fn  fn?
            post-fn fn?
            ws-uri  string?)
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
           root-path serve-files routes-fn]
    :as opts}]
  (validate routes-fn (v/or* fn? var?))
  (concat (when ws-uri (ws-routes opts))
          (routes-fn opts)
          (when csp-report-uri (csp-report-route opts))
          (when serve-files
            [(resources "/" {:root root-path})]) ; static files
          [(not-found-route opts)]))

(defn make-routes
  [{:keys [middleware]
    :as opts}]
  (validate middleware (v/or* fn? var?))
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
