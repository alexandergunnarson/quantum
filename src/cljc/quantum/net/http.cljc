(ns quantum.net.http
  (:require-quantum [:core err logic fn log async casync])
  (:require        [com.stuartsierra.component              :as component]
                   [taoensso.sente                          :as ws       ]
           #?(:clj [immutant.web                            :as imm      ]
                   [aleph.http                              :as server   ])
           #_(:clj [taoensso.sente.server-adapters.immutant :as a-imm    ]
                   #_[taoensso.sente.server-adapters.aleph    :as a-aleph  ])
                   [clojure.string                          :as str      ]
                   [quantum.net.client.impl                 :as impl     ]
                   [quantum.net.core                        :as net      ]))

#?(:clj
(defrecord
  ^{:doc "A web server. Currently only the :aleph, :immutant, and :http-kit server @types are supported."}
  Server
  [routes server type host port ssl-port http2?
   key-store-path key-password
   trust-store-path trust-password
   stop-fn stop-timeout
   ran ssl-context
   socket-address
   executor
   raw-stream?
   bootstrap-transform
   pipeline-transform
   ssl-context
   request-buffer-size
   shutdown-executor?
   rejected-handler
   epoll?]
  component/Lifecycle
    (start [this]
      (err/assert (net/valid-port? port) #{port})
      (err/assert (contains? #{:immutant :aleph #_:http-kit} type) #{type})

      (let [opts (merge
                   {:host           (or host     "localhost")
                    :port           (or port     80)
                    :ssl-port       (or ssl-port 443)
                    :http2?         (or http2?   false)
                    :keystore       key-store-path
                    :truststore     trust-store-path}
                   (kmap
                    key-password
                    trust-password
                    ssl-context
                    socket-address
                    executor
                    raw-stream?
                    bootstrap-transform
                    pipeline-transform
                    ssl-context
                    request-buffer-size
                    shutdown-executor?
                    rejected-handler
                    epoll?))
            _ (log/ppr :debug "Launching server with options:" opts)
            server (condp =
                     :aleph    (aleph/start-server routes opts)
                     :immutant (imm/run            routes opts)
                     ;:http-kit (http-kit/run-server routes {:port (or port 0)})
                     )]
        (assoc this
          :ran     server
          :server  (condp = type
                     :aleph    nil ; aleph doesn't expose it
                     :immutant (imm/server server)
                     :http-kit nil) ; http-kit doesn't expose it
          :port    (condp = type
                     :aleph    (aleph.netty/port server)
                     :http-kit (:local-port (meta server))
                     port)
          :stop-fn (condp = type
                     :aleph    #(.close server)
                     :immutant #(do (when (nnil? server)
                                      (imm/stop server)))
                     :http-kit server))))
    (stop [this]
      (condp = type
        :http-kit (stop-fn :timeout (or stop-timeout 100))
        :immutant (stop-fn))
      (assoc this
        :stop-fn nil))))

(defalias request! impl/request!)

; UTILS

(defn ip
  "Returns the caller's IP address."
  []
  (-> (request! {:url "http://checkip.amazonaws.com" :as :text})
      :body
      str/trim))

#?(:cljs
(defn- check-xhr
  "Checks the status of an XHR connection"
  {:help-from "HubSpot/offline"}
  [xhr on-up on-down]
  (let [check-status
         (fn []
          (println "Check status" (.-status xhr) )
           (if (and (.-status xhr) (< (.-status xhr) 12000))
               (on-up)
               (on-down)))
        on-error-0              (.-onerror            xhr)
        on-timeout-0            (.-ontimeout          xhr)
        on-load-0               (.-onload             xhr)
        on-ready-state-change-0 (.-onreadystatechange xhr)]
    (if (nil? (.-onprogress xhr)) ; Feature checking?
        ; TODO derepetitivize
        (do (set! (.-onerror xhr)
              (fn [& args]
                (println "Error")
                (on-down)
                (apply (or on-error-0 fn-nil) args)))
            (set! (.-ontimeout xhr)
              (fn [& args]
                (println "Timeout")
                (on-down)
                (apply (or on-timeout-0 fn-nil) args)))
            (set! (.-onload xhr)
              (fn [& args]
                (println "Load")
                (check-status)
                (apply (or on-load-0 fn-nil) args))))
        (do (set! (.-onreadystatechange xhr) 
              (fn [& args]
                (println "State change")
                (condp = (.-readyState xhr)
                  4 (check-status)
                  0 (on-down)
                  (apply (or on-ready-state-change-0 fn-nil) args)))))))))

; ===== NETWORK CONNECTIVITY CHECKING/HANDLING ===== 

(defonce network-connected? (atom nil))
(defonce network-connection-handlers
  (atom {true  (fn [] (log/pr :debug "Network connection acquired."))
         false (fn [] (log/pr :debug "Network connection lost."    ))}))

(defn get-network-connected?
  #?(:cljs "Makes an XHR request to load your /favicon.ico to check the connection.
            If you don't have such a file, it will 404 in the console, but otherwise
            work fine (even a 404 means the connection is up).")
  {:help-from "HubSpot/offline"
   :usage '(go (<! (get-network-connected?)))}
  []
  #?(:clj (let [s (atom nil)]
            (try (let [default-url "www.google.com"] ; ~8x faster than checkip.amazonaws.com
                   (reset! s (java.net.Socket. default-url 80))
                   true)
              (catch java.net.SocketException e
                (if (-> e .getMessage (= "Network is unreachable"))
                    false
                    :unknown))
              (catch java.net.UnknownHostException _ false)
              (finally (when @s (.close ^java.net.Socket @s)))))
     :cljs (let [; The date is included as a cache buster
                 url     (str "/favicon.ico?_=" (.getTime (js/Date.)))
                 timeout 5000
                 type    "HEAD"
                 xhr     (js/XMLHttpRequest.) ; TODO use XHRIo
                 c       (async/chan 1)
                 gen-handler (fn [b] ; for bool
                               (fn []
                                 (async/put! c b)
                                 (reset! network-connected? b)
                                 ((or (get @network-connection-handlers b) fn-nil))))]
            (set! (.-offline xhr) false)
            (.open xhr type url true)
            (when (.-timeout xhr) ; Feature detection?
              (set! (.-timeout xhr) 5000))

            (check-xhr xhr
              (gen-handler true)
              (gen-handler false))
            (try (.send xhr)
                 c
              (catch js/Error e
                (async/put! c false)
                c)))))