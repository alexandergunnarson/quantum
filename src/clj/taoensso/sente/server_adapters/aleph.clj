(ns taoensso.sente.server-adapters.aleph
  "Sente server adapter for Aleph."
  {:author "Zach Tellman <@ztellman>, Alex Gunnarson <@alexandergunnarson>"}
  (:require [taoensso.sente.interfaces :as i   ]
            [aleph.http                :as http]
            [manifold.stream           :as s   ]
            [manifold.deferred         :as d   ]
            [quantum.core.log          :as log ]
            [quantum.core.collections  :as coll
              :refer [kmap]                    ]))

(extend-type manifold.stream.core.IEventSink
  i/IServerChan
  (sch-open?  [hk-ch] (log/pr ::debug "OPEN?" (not (s/closed? hk-ch))) (not (s/closed? hk-ch)))
  (sch-close! [hk-ch] (log/pr ::debug "CLOSING") (s/close! hk-ch) (log/pr ::debug "CLOSED"))
  (-sch-send! [hk-ch msg close-after-send?]
    (log/ppr ::debug (kmap msg close-after-send?))
    (s/put! hk-ch msg)
    (when close-after-send?
      (log/pr ::debug "CLOSE AFTER SEND REQUESTED." (kmap msg))
      #_(s/close! hk-ch)
      #_(log/pr ::debug "CLOSED IN SCH SEND"))))

(defn websocket-request? [req]
  (= "websocket" (-> req :headers (get "upgrade"))))

(defn do-this [s on-open on-msg on-close req]
  ; s is server channel
  (log/pr ::debug "DETERMINED WAS WEBSOCKET.")
  (when on-open (on-open s))
  (log/pr ::debug "AFTER ON-OPEN" on-open)
  (when on-msg (s/consume (fn [result] (on-msg s result)) s))
  (log/pr ::debug "AFTER ON-MSG" on-msg)
  (when on-close (s/on-closed s #(on-close s nil)))
  (log/pr ::debug "AFTER ON-CLOSE" on-close)
  {:body s})

(deftype AlephAsyncNetworkChannelAdapter []
  i/IServerChanAdapter
  (ring-req->server-ch-resp [this req callbacks]
    (log/ppr ::debug "ring-req->server-ch-resp" req)
    (let [{:keys [on-open on-msg on-close]} callbacks]
      (if (websocket-request? req)
          (d/chain (http/websocket-connection req)
            (fn [s]
              (@#'do-this s on-open on-msg on-close req)))
          (let [s (s/stream)]
            (log/pr ::debug "DETERMINED WAS NOT WEBSOCKET.")
            (when on-open (on-open s))
            (when on-close (s/on-closed s #(on-close s nil)))
            {:body s})))))

(def aleph-adapter (AlephAsyncNetworkChannelAdapter.))
(def sente-web-server-adapter aleph-adapter) ; Alias for ns import convenience