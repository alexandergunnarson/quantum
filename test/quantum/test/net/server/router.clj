(ns quantum.test.net.server.router
  (:require [quantum.net.server.router :as ns]))

; ===== ROUTES =====

(defn test:resources
  [path & [options]])

; ROUTES PRESETS

(defn test:ws-routes
  [{:keys [ws-uri get-fn post-fn]}])

(defn test:csp-report-route
  [{:keys [csp-report-uri csp-report-handler]}])

(defn test:not-found-route
  [opts])

(defn test:routes
  [{:keys [ws-uri csp-report-uri
           root-path]
    :as opts}])

(defn test:make-routes
  [{:keys [middleware]
    :as opts}])