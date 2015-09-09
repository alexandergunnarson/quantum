; Jetty 9 instead of 7
(ns ring.adapter.jetty
  "A Ring adapter that uses the Jetty 9 embedded web server.
  Adapters are used to convert Ring handlers into running web servers."
  (:require-quantum [:lib])
  (:require [ring.util.servlet :as servlet])
  (:import  [org.eclipse.jetty.server Server Request ServerConnector
              HttpConfiguration HttpConnectionFactory ConnectionFactory]
            [org.eclipse.jetty.server.handler AbstractHandler]
            [org.eclipse.jetty.util.thread    QueuedThreadPool]
            [org.eclipse.jetty.util.ssl       SslContextFactory]
            [javax.servlet.http HttpServletRequest HttpServletResponse]))

(set! *warn-on-reflection* true)

(defonce last-resp (atom nil))
(defonce last-resp-parsed (atom nil))

(defn- proxy-handler
  "Returns an Jetty Handler implementation for the given Ring handler."
  [handler]
  ;(reify Handler
  ;  (handle [this _ base-request request response]
  ;    (let [request-map  (servlet/build-request-map request)
  ;          response-map (handler request-map)]
  ;      (when response-map
  ;        (servlet/update-servlet-response response response-map)
  ;        (.setHandled ^Request base-request true)))))
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request response]
      (let [request-map  (servlet/build-request-map request)
            response-map (handler request-map)]
        (when response-map
          (servlet/update-servlet-response request response response-map)
          (.setHandled base-request true))))))

(defn- ssl-context-factory
  "Creates a new SslContextFactory instance from a map of options."
  [options]
  (let [context (SslContextFactory.)]
    (if (string? (options :keystore))
      (.setKeyStorePath context (options :keystore))
      (.setKeyStore context ^java.security.KeyStore (options :keystore)))
    (.setKeyStorePassword context (options :key-password))
    (cond
      (string? (options :truststore))
        (.setTrustStore context ^String (options :truststore))
      (instance? java.security.KeyStore (options :truststore))
        (.setTrustStore context ^java.security.KeyStore (options :truststore)))
    (when (options :trust-password)
      (.setTrustStorePassword context (options :trust-password)))
    (case (options :client-auth)
      :need (.setNeedClientAuth context true)
      :want (.setWantClientAuth context true)
      nil)
    context))

(defn- ssl-connector
  "use SelectChannelConnector with SslContextFactory."
  [server {:as options :keys [ssl? ssl-port ssl-host host max-idle-time]}]
  (doto (ServerConnector. server
          ^SslContextFactory (ssl-context-factory options))
    (.setPort (or ssl-port 443))
    (.setHost (or ssl-host host))
    (.setIdleTimeout (or max-idle-time 200000))))

(defn- create-server
  "Construct a Jetty Server instance."
  [{:as options :keys [ssl? ssl-port port host ssl-host max-idle-time min-threads daemon?]}]
  (let [^QueuedThreadPool p
          (doto (QueuedThreadPool. ^Integer (options :max-threads 50))
            (.setMinThreads (or min-threads 8)) )
        ; (when-let [max-queued (:max-queued options)] ; Deprecated, apparently
        ;   (.setMaxQueued p max-queued))
        _ (when (or daemon? false)
            (.setDaemon p true))
        server   (Server. p)
        config   (doto (HttpConfiguration.)
                   (.setSendDateHeader true))
        conn-factory (HttpConnectionFactory. config)
        connector (doto (ServerConnector. server) ; (ServerConnector. server conn-factory)
                    (.setPort (or port 80))
                    (.setHost host)
                    (.setIdleTimeout (or max-idle-time 200000)))]
    (.addConnector server connector)
    (when (or ssl? ssl-port)
      (.addConnector server (ssl-connector server options)))
    server))

(defn ^Server run
  "Start a Jetty webserver to serve the given handler according to the
  supplied options:
  :configurator   - a function called with the Jetty Server instance
  :port           - the port to listen on (defaults to 80)
  :host           - the hostname to listen on
  :join?          - blocks the thread until server ends (defaults to true)
  :daemon?        - use daemon threads (defaults to false)
  :ssl?           - allow connections over HTTPS
  :ssl-port       - the SSL port to listen on (defaults to 443, implies :ssl?)
  :keystore       - the keystore to use for SSL connections
  :key-password   - the password to the keystore
  :truststore     - a truststore to use for SSL connections
  :trust-password - the password to the truststore
  :max-threads    - the maximum number of threads to use (default 50)
  :min-threads    - the minimum number of threads to use (default 8)
  :max-queued     - the maximum number of requests to queue (default unbounded)
  :max-idle-time  - the maximum idle time in milliseconds for a connection (default 200000)
  :client-auth    - SSL client certificate authenticate, may be set to :need,
                    :want or :none (defaults to :none)"
  [handler options]
  (let [^Server s (create-server (dissoc options :configurator))]
    (doto s
      (.setHandler (proxy-handler handler)))
    (when-let [configurator (:configurator options)]
      (configurator s))
    (try
      (.start s)
      (when (:join? options true)
        (.join s))
      s
      (catch Exception ex
        (.stop s)
        (throw ex)))))
