(ns quantum.net.server.middleware.figwheel
  (:require
    [aleph.http]
    [figwheel-sidecar.components.figwheel-server :as fig]
    [quantum.core.async         :as async]))

; Asynchronous
#_"
script0 = document.createElement('script');
cript0.src = 'js/nr-compiled/quantum.js';
document.body.append(script0);
"
; Synchronous
#_"
var xhrObj = new XMLHttpRequest();
// open and send a synchronous request
xhrObj.open('GET', 'js/nr-compiled/quantum.js', false);
xhrObj.send('');
// add the returned content to a newly created script tag
var se = document.createElement('script');
se.type = 'text/javascript';
se.text = xhrObj.responseText;
document.getElementsByTagName('head')[0].appendChild(se);
"

; From figwheel-sidecar 0.5.8
(defn setup-file-change-sender [{:keys [file-change-atom compile-wait-time connection-count] :as server-state}
                                {:keys [desired-build-id] :as params}
                                in out]
  (let [watch-key (keyword (gensym "message-watch-"))]
    (fig/update-connection-count connection-count desired-build-id inc)
    (add-watch
     file-change-atom
     watch-key
     (fn [_ _ o n]
       (let [msg (first n)]
         (when (and msg
                    (or
                     ;; broadcast all css messages
                     (= ::broadcast (:build-id msg))
                     ;; if its nil you get it all
                     (nil? desired-build-id)
                     ;; otherwise you only get messages for your build id
                     (= desired-build-id (:build-id msg))))
           (async/<!! (async/timeout compile-wait-time))
           (when-not (async/closed? out)
             (async/put! out (prn-str msg)))))))

    (async/go
      (loop []
        (if-let [data (async/<! in)]
          (do (#'fig/handle-client-msg server-state data)
              (recur))
          (do (fig/update-connection-count connection-count desired-build-id dec)
              (remove-watch file-change-atom watch-key)
              ; TODO close both chans when?
              ))))

    ;; Keep alive!!
    (async/go
      (loop []
        (async/<! (async/timeout 5000))
        (when-not (async/closed? out)
          (async/put! out (prn-str {:msg-name :ping
                                    :project-id (:unique-id server-state)}))
          (recur))))))

; Adapted from figwheel-sidecar 0.5.8
; TODO expects an Aleph request; not currently extensible to other backends
; Using the built-in figwheel-sidecar uses httpkit
(defn reload-handler [server-state]
  (fn [req]
    (let [manifold @(aleph.http/websocket-connection req)
          in  (async/chan)
          _   (manifold.stream/connect manifold in)
          out (async/chan)
          _   (manifold.stream/connect out      manifold)]
      (#'setup-file-change-sender server-state (:params req) in out)
      {:body "Accepted websocket" :status 200})))

; TODO move?
(defn websocket-request? [req]
  (-> req :headers (get "upgrade") (= "websocket")))

(defn websocket* [{:keys [uri] :as req} handler websocket-handler]
  (if (and (websocket-request? req)
           (-> req :request-method (= :get)))
      (cond
         (= uri "/figwheel-ws")
         (websocket-handler req)

         (fig/parse-build-id uri)
         (websocket-handler
           (assoc-in req [:params :desired-build-id] (fig/parse-build-id uri)))

         :else (handler req))
      (handler req)))

(defn wrap-figwheel-websocket [handler figwheel-ws-state]
  (fn [req] (#'websocket* req handler (reload-handler @figwheel-ws-state))))
