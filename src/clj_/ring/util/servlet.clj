(ns ring.util.servlet
  "Compatibility functions for turning a ring handler into a Java servlet."
  (:require-quantum [:lib])
  (:import
    [java.util Locale]
    [javax.servlet.http
      HttpServlet HttpServletRequest HttpServletResponse]
    [javax.servlet WriteListener AsyncContext ServletOutputStream]
    [java.nio ByteBuffer]
    [java.nio.channels WritableByteChannel
      ReadableByteChannel]))

(defn- get-headers
  "Creates a name/value map of all the request headers."
  [^HttpServletRequest request]
  (reduce
    (fn [headers ^String name]
      (assoc headers
        (.toLowerCase name Locale/ENGLISH)
        (->> (.getHeaders request name)
             (enumeration-seq)
             (str/join ","))))
    {}
    (enumeration-seq (.getHeaderNames request))))

(defn- get-content-length
  "Returns the content length, or nil if there is no content."
  [^HttpServletRequest request]
  (let [length (.getContentLength request)]
    (when (>= length 0) length)))

(defn- get-client-cert
  "Returns the SSL client certificate of the request, if one exists."
  [^HttpServletRequest request]
  (-> request
      (.getAttribute "javax.servlet.request.X509Certificate")
      first))

(defn build-request-map
  "Create the request map from the HttpServletRequest object."
  [^HttpServletRequest request]
  {:server-port        (-> request .getServerPort       )
   :server-name        (-> request .getServerName       )
   :remote-addr        (-> request .getRemoteAddr       )
   :uri                (-> request .getRequestURI       )
   :query-string       (-> request .getQueryString      )
   :scheme             (-> request .getScheme 
                           keyword)
   :request-method     (-> request .getMethod          
                           (.toLowerCase Locale/ENGLISH)
                           keyword)
   :protocol           (-> request .getProtocol         )
   :headers            (-> request get-headers          )
   :content-type       (-> request .getContentType      )
   :content-length     (-> request get-content-length   )
   :character-encoding (-> request .getCharacterEncoding)
   :ssl-client-cert    (-> request get-client-cert      )
   :body               (-> request .getInputStream      )})

(defn merge-servlet-keys
  "Associate servlet-specific keys with the request map for use with legacy
  systems."
  [request-map
   ^HttpServlet         servlet
   ^HttpServletRequest  request
   ^HttpServletResponse response]
  (merge request-map
         {:servlet              servlet
          :servlet-request      request
          :servlet-response     response
          :servlet-context      (.getServletContext servlet)
          :servlet-context-path (.getContextPath request)}))

(defn- set-status
  "Update a HttpServletResponse with a status code."
  [^HttpServletResponse response, status]
  (.setStatus response status))

(defn- set-headers
  "Update a HttpServletResponse with a map of headers."
  [^HttpServletResponse response headers]
  (core/doseq [[k val-or-vals] headers]
    (cond
      (string? val-or-vals)
        (.setHeader response k val-or-vals)
      (coll?   val-or-vals)
        (doseq [v val-or-vals]
          (.addHeader response k v))
      :else
        (->> val-or-vals str (.setHeader response k))))
  ; Some headers must be set through specific methods
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType response content-type)))

(defn- set-body
  "Update a HttpServletResponse body with a String, ISeq, File or InputStream."
  ([^HttpServletResponse response, body]
  (cond
    (string? body)
      (with-open [writer (.getWriter response)]
        (.print writer body))
    (seq? body)
      (with-open [writer (.getWriter response)]
        (doseq [chunk body]
          (.print writer (str chunk))))
    (instance? InputStream body)
      (with-open [^InputStream b body]
        (io/copy! b (.getOutputStream response)))
    (instance? File body)
      (let [^File f body]
        (with-open [stream (FileInputStream. f)]
          (set-body response stream)))
    (nil? body)
      nil
    :else
      (throw (Exception. ^String (format "Unrecognized body: %s" body)))))
  ([^HttpServletResponse response
   ^HttpServletRequest request
   body]
  (cond
    (string? body)
      (with-open [writer (.getWriter response)]
        (.print writer body))
    (seq? body)
      (with-open [writer (.getWriter response)]
        (doseq [chunk body]
          (.print writer (str chunk))))
    (instance? InputStream body)
    ; TODO Stream InputStream continuously instead of just
    ; copying the whole thing and blocking
      (with-open [^InputStream b body]
        ; Very inefficient for huge InputStreams
        (io/copy! b (.getOutputStream response)))
      ; https://webtide.com/servlet-3-1-async-io-and-jetty/
      ; TODO: test for long streams
      ; TODO: it downloads it ALL and only then does it unblock the outputstream
      #_(let [^ServletOutputStream out-stream (.getOutputStream response)
            ^AsyncContext        async  (.startAsync      request)
            ^ReadableByteChannel in     (conv/->byte-channel ^InputStream body)
            ^WritableByteChannel out    (conv/->byte-channel out-stream)
            ^ByteBuffer          buffer (ByteBuffer/allocateDirect (* 16 1024))]
        #_(.setContentLength response (.available ^InputStream body))
        (.setWriteListener out-stream
          (proxy [WriteListener] []
            (onWritePossible []
              (println "beginning stream write")
              (while (and (.isReady out-stream) 
                          (not= -1 (.read in buffer)))
                (.flip buffer)
                (.write out buffer)
                (.compact buffer))
              (.flip buffer)
              (while (and (.isReady out-stream) 
                          (.hasRemaining buffer))
                (.write out buffer))
              (println "completing async")
              (.complete async))
              ; TODO close channels?
            (onError [^Throwable t]
              (log/pr :warn "Error in inputStream" t)
              (.complete async)))))
    (instance? File body)
      (let [^File f body]
        (with-open [stream (FileInputStream. f)]
          (set-body response stream)))
    (nil? body)
      nil
    :else
      (throw (Exception. ^String (format "Unrecognized body: %s" body))))))

(defn update-servlet-response
  "Update the HttpServletResponse using a response map."
  {:arglists '([response response-map])}
  [^HttpServletRequest  request
   ^HttpServletResponse response
   {:keys [status headers body]}]
  (when-not response
    (throw (Exception. "Null response given.")))
  (when status
    (set-status response status))
  (doto response
    (set-headers headers)
    (set-body request body)))

(defn make-service-method
  "Turns a handler into a function that takes the same arguments and has the
  same return value as the service method in the HttpServlet class."
  [handler]
  (fn [^HttpServlet         servlet
       ^HttpServletRequest  request
       ^HttpServletResponse response]
    (let [request-map (-> request
                          (build-request-map)
                          (merge-servlet-keys servlet request response))]
      (if-let [response-map (handler request-map)]
        (update-servlet-response request response response-map)
        (throw (NullPointerException. "Handler returned nil"))))))

(defn servlet
  "Create a servlet from a Ring handler."
  [handler]
  (let [service-method (make-service-method handler)]
    (proxy [HttpServlet] []
      (service [request response]
        (service-method this request response)))))

(defmacro defservice
  "Defines a service method with an optional prefix suitable for being used by
  genclass to compile a HttpServlet class.
  For example:
    (defservice my-handler)
    (defservice \"my-prefix-\" my-handler)"
  ([handler]
     `(defservice "-" ~handler))
  ([prefix handler]
     `(let [service-method# (make-service-method ~handler)]
        (defn ~(symbol (str prefix "service"))
          [servlet# request# response#]
          (service-method# servlet# request# response#)))))