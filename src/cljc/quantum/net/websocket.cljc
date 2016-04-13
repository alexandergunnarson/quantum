(ns quantum.net.websocket
  (:require-quantum [:core log fn logic err res async core-async cbase])
  (:require [com.stuartsierra.component              :as component]
            [taoensso.sente                          :as ws       ]
  #?@(:clj [[immutant.web                            :as imm      ]
            [taoensso.sente.server-adapters.immutant :as a-imm    ]
            [taoensso.sente.server-adapters.aleph    :as a-aleph  ]])
            [quantum.core.core                
              :refer [lens deref*]                  ]))

(defmulti event-msg-handler :id) ; Dispatch on event-id
(def send-msg! (lens res/systems (fn-> :global :sys-map deref* :connection :send-fn)))

; Wrap for logging, catching, etc.:
(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (event-msg-handler ev-msg))

(declare put!)

#?(:clj
(defmethod event-msg-handler :default ; Fallback
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (log/pr :debug "Unhandled event:" ev-msg "from" uid)
    (log/pr :debug "Responding" "reply-fn?" ?reply-fn)
  
    (when ?reply-fn
      (?reply-fn {:unhandled-event event})))))

#?(:clj
(defmethod event-msg-handler :chsk/uidport-open
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/pr :debug "uidport-open")))

#?(:clj
(defmethod event-msg-handler :chsk/uidport-close
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/pr :debug "uidport-close")))

#?(:clj
(defmethod event-msg-handler :chsk/ws-ping
  [ev-msg]
  ; Do nothing
  ))

; ------> Add your (defmethod event-msg-handler <event-id> [ev-msg] <body>)s here <------

; ------> Add your (defmethod handle-event-msg! <event-id> [ev-msg] <body>)s here <------

(defn put!
  "Sends a message @msg across via a WebSocket connection."
  {:usage '(put! :my/button {:my-key1 "Data1"
                             :my-key2 "Data2"}
                 (fn [resp] (println "Response is" resp))
                 100)}
  [#?(:clj uid) [msg-id msg] callback & [timeout]]
  (let [f @send-msg!]
    (assert (nnil? f))
    (@send-msg! #?(:clj uid) [msg-id msg] (or timeout 200) callback)))

(defn try-put!
  "Try to send messsage @?times times with intervals of @?sleep
   milliseconds. As soon as message send is successful, no further
   messages are tried to be sent."
  [?times ?sleep & args]
  (let [times (when (integer? ?times) ?times)
        sleep (when (number?  ?sleep) ?sleep)
        args-f (cond (and times sleep) args
                     times             (cons ?sleep args)
                     :else             (concat (list ?times ?sleep) args))]
    (go (try-times (or times 3) (or sleep 500)
          (when (nil? (apply put! args-f))
            (throw (->ex nil "WebSocket apparently not open for message")))))))

(defrecord 
  ^{:doc "A WebSocket-channel abstraction of Sente's functionality.

          Creates a Sente WebSocket channel and Sente WebSocket channel
          message router.

          @chan-recv  : ChannelSocket's receive channel
          @chan-send! : ChannelSocket's send API fn
          @chan-state : Watchable, read-only atom
          @packer     : Client<->server serialization format"
    :usage '(map->ChannelSocket {:uri         "/chan"
                                 :packer      :edn
                                 :msg-handler my-msg-handler})
    :todo ["The recommended version supported in latest versions of all
            current browsers is RFC 6455 (supported by Firefox 11+, Chrome 16+,
            Safari 6, Opera 12.50, and IE10). Don't use previous versions."]}
  ChannelSocket
  [uri host chan chan-recv send-fn chan-state type server-type packer
   stop-fn ajax-post-fn ajax-get-or-ws-handshake-fn msg-handler
   connected-uids]
  component/Lifecycle
    (start [this]
      (let [stop-fn-f (atom (fn []))]
        (try
          (log/pr :debug "Starting Channel Socket with:" this)
          (assert (string? uri) #{uri})
          (assert (fn? msg-handler))
          (assert (or (nil? type) (contains? #{:auto :ajax :ws} type)))
        #?(:clj 
          (assert (contains? #{:aleph :immutant #_:http-kit} server-type) #{server-type}))
          (assert (keyword? packer))

          (let [{:keys [chsk ch-recv send-fn state] :as socket}
                 (ws/make-channel-socket!
                   #?(:clj (condp = server-type
                             ;:http-kit a-http-kit/sente-web-server-adapter
                             :aleph    a-aleph/sente-web-server-adapter
                             :immutant a-imm/sente-web-server-adapter)
                      :cljs uri)
                   {:type   (or type :auto)
                    :packer packer
                    #?@(:cljs
                    [:host host])})
                _ (reset! stop-fn-f (ws/start-chsk-router! ch-recv msg-handler))]
            (assoc this
              :chan                        chsk
              :chan-recv                   ch-recv
              :send-fn                     send-fn
              :chan-state                  state
              :stop-fn                     @stop-fn-f
              :ajax-post-fn                (:ajax-post-fn                socket)
              :ajax-get-or-ws-handshake-fn (:ajax-get-or-ws-handshake-fn socket)
              :connected-uids              (:connected-uids              socket)))
          (catch Throwable e
            (err/warn! e)
            (@stop-fn-f)
            (throw e)))))
    (stop [this]
      (try (when stop-fn (stop-fn))
        (catch Throwable e
          (err/warn! e)))
      ; TODO should assoc other vals as nil?
      this))