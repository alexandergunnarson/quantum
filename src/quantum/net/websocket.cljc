(ns quantum.net.websocket
  (:refer-clojure :exclude [promise])
  (:require [com.stuartsierra.component              :as component]
            [taoensso.sente                          :as ws       ]
  #?@(:clj [[immutant.web                            :as imm      ]
            [taoensso.sente.server-adapters.immutant :as a-imm    ]
            [taoensso.sente.server-adapters.aleph    :as a-aleph  ]])
            [quantum.core.refs
              :refer [lens ?deref]]
            [quantum.core.data.complex.json          :as json]
            [quantum.core.error                      :as err
              :refer [>ex-info]]
            [quantum.core.fn                         :as fn
              :refer [fn-> fn']]
            [quantum.core.log                        :as log]
            [quantum.core.async                      :as async
              :refer [promise offer! go]]
            [quantum.core.spec                       :as s
              :refer [validate]]
            [quantum.core.type           :as t
              :refer [val?]]
            [quantum.core.resources                  :as res]
    #?(:clj [quantum.net.server.router               :as router])))

(defmulti handle :id) ; Dispatch on event-id
(def send-msg! (lens res/systems (fn-> ::res/global :sys-map ::connection :send-fn)))

; Wrap for logging, catching, etc.:
(defn handle* [{:as ev-msg :keys [id ?data event]}]
  (handle ev-msg))

(declare put!)

; ===== DEFAULT HANDLERS ===== ;

#?(:clj
(defmethod handle :default ; Fallback
  [{:as ev-msg :keys [event id client-id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (log/ppr :debug (str "Unhandled event from " uid "(" client-id ")") ev-msg)

    (when ?reply-fn
      (log/pr :debug "Responding to callback")
      (?reply-fn {:unhandled-event event})))))

#?(:clj
(defmethod handle :chsk/uidport-open
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/pr :debug "uidport-open")))

#?(:clj
(defmethod handle :chsk/uidport-close
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (log/pr :debug "uidport-close")))

#?(:clj
(defmethod handle :chsk/ws-ping
  [ev-msg]
  ; Do nothing
  ))

; ===== `PUT!` ===== ;

(defn put!
  "Sends a message ->`msg` across via a WebSocket connection."
  {:usage '(put! [:my/button {:my-key1 "Data1"
                              :my-key2 "Data2"}]
                 (fn [resp] (println "Response is" resp))
                 100)}
  #?(:cljs ([msg-pack] (put! msg-pack (fn [_]))))
  #?(:cljs ([msg-pack callback] (put! msg-pack callback 200)))
           ([#?(:clj uid) msg-pack #?(:cljs callback) #?(:cljs timeout)]
             (let [f @send-msg!]
               (assert (val? f))
               (@send-msg! #?(:clj uid) msg-pack #?(:cljs timeout) #?(:cljs (or callback (fn [_]))))))) ; to ensure no auto-close

(defn put-chan!
  #?(:cljs ([msg-pack] (put-chan! msg-pack nil)))
           ([#?(:clj uid) msg-pack #?(:cljs timeout)]
             (let [ret (promise)]
               (put! #?(:clj uid) msg-pack
                 #?(:cljs (fn [resp] (offer! ret resp)))
                 #?(:cljs timeout))
               ret)))

(defn try-put!
  "Try to send messsage ->`?times` times with intervals of ->`?sleep`
   milliseconds. As soon as message send is successful, no further
   messages are tried to be sent."
  [?times ?sleep & args]
  (let [times (when (integer? ?times) ?times)
        sleep (when (number?  ?sleep) ?sleep)
        args-f (cond (and times sleep) args
                     times             (cons ?sleep args)
                     :else             (concat (list ?times ?sleep) args))]
    (go (async/try-times! (or times 3) (or sleep 500)
          (when (nil? (apply put! args-f))
            (throw (>ex-info "WebSocket apparently not open for message")))))))

(deftype JSONPacker []
  taoensso.sente.interfaces/IPacker
  (pack   [_ x] (json/->json x))
  (unpack [_ s]
    (let [unpacked (json/json-> s)]
      (if (sequential? unpacked)
          (if (-> unpacked first string?) ; assume it's an event ID
              (vec (cons (-> unpacked first keyword) (rest unpacked)))
              (vec unpacked))
          unpacked))))

(def json-packer (JSONPacker.))

(defrecord
  ^{:doc "A WebSocket-channel abstraction of Sente's functionality.

          Creates a Sente WebSocket channel and Sente WebSocket channel
          message router.

          ->`chan-recv`  : ChannelSocket's handle channel
          ->`chan-send!` : ChannelSocket's send API fn
          ->`chan-state` : Watchable, read-only atom
          ->`packer`     : Client<->server serialization format"
    :usage '(map->ChannelSocket {:uri         "/chan"
                                 :packer      :edn
                                 :handler     my-msg-handler})
    :todo ["The recommended version supported in latest versions of all
            current browsers is RFC 6455 (supported by Firefox 11+, Chrome 16+,
            Safari 6, Opera 12.50, and IE10). Don't use previous versions."]}
  ChannelSocket
  [endpoint #?(:cljs [port host]) chan chan-recv send-fn chan-state type packer
   stop-fn post-fn get-fn handler
   connected-uids]
  component/Lifecycle
    (start [this]
      (let [stop-fn-f (atom (fn []))
            server    (:quantum.net.http/server this)
            endpoint  (or endpoint "/chan")
            handler   (or handler  #'handle*)]
        (try
          (log/prl ::debug "Starting channel-socket with:" endpoint #?@(:cljs [host port]) type packer connected-uids)
          (validate endpoint string?)
          (validate handler  (s/or* fn?  (s/and var? (fn-> deref fn?))))
          (validate type     (s/or* nil? #{:auto :ajax :ws}))
          #?(:clj  (do (validate server val?)
                       (validate (:type server) #{:aleph :immutant}))
             :cljs (do (validate host string?) ; technically, valid hostname
                       (validate port integer?))) ; technically, valid port
          (let [packer (case packer (nil :edn) :edn
                                    :json      json-packer
                                    packer) ; let Sente handle it, I say
                {:keys [chsk ch-recv send-fn state] :as socket}
                 (ws/make-channel-socket!
                   #?(:clj (case (:type server)
                             ;:http-kit a-http-kit/sente-web-server-adapter
                             :aleph    (a-aleph/get-sch-adapter)
                             :immutant (a-imm/get-sch-adapter))
                      :cljs endpoint)
                   {:type   (or type :auto)
                    :packer packer
         #?@(:cljs [:host   (str host ":" port)])})
                _ (reset! stop-fn-f (ws/start-chsk-router! ch-recv handler))
                this' (assoc this
                        :chan           chsk
                        :chan-recv      ch-recv
                        :send-fn        send-fn
                        :chan-state     state
                        :packer         packer
                        :stop-fn        @stop-fn-f
                        :post-fn        (:ajax-post-fn                socket)
                        :get-fn         (:ajax-get-or-ws-handshake-fn socket)
                        :connected-uids (:connected-uids              socket))]
            #?(:clj (alter-var-root (:routes-var server) ; TODO defnt |reset!|
                      (fn' (router/make-routes (merge this' server {:ws-uri endpoint})))))
            (log/pr ::debug "Channel-socket started.")
            this')
          (catch #?(:clj Throwable :cljs js/Error) e
            (log/pr :warn e)
            (@stop-fn-f)
            (throw e)))))
    (stop [this]
      (try (when stop-fn (stop-fn))
        (catch #?(:clj Throwable :cljs js/Error) e
          (log/pr :warn e)))
      ; TODO should assoc other vals as nil?
      this))

(res/register-component! ::connection map->ChannelSocket [::log/log #?(:clj :quantum.net.http/server)])
