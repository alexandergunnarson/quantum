(ns quantum.cljtest
  (:require-quantum [:core sys fn logic pr log err debug])
  (:require [quantum.system                       :as gsys    ]
            [quantum.net.websocket                :as conn    ]
            [quantum.core.convert                 :as conv    ]
            [quantum.net.http                     :as http    ]
            [quantum.net.server.router            :as router  ]
            [quantum.apis.amazon.cloud-drive.auth :as amz-auth]
            [quantum.auth.core                    :as auth    ]
            [quantum.core.io.core                 :as io      ]
            [quantum.security.cryptography        :as crypto  ]))

(gsys/create-system-vars
  ::system
  (gsys/default-config
    {:log        {:levels           #{:debug :warn :quantum.net.client.impl/debug}}
     :server     {:port             8081
                  :ssl-port         9998
                  :routes           #'router/routes
                  :key-password     "test123"
                  :key-store-path   "./dev-resources/keystore"
                  :trust-password   "test123"
                  :trust-store-path "./dev-resources/keystore"}
     :db         {:backend        false
                  :schemas        {:my/schema [:string :one {:unique? true}]}}
     :connection {:msg-handler    conn/event-msg-handler*}}))

(-main)
(defn ->base-64 [x]
  (String. (.encode (java.util.Base64/getEncoder) x) java.nio.charset.StandardCharsets/ISO_8859_1))
(defn base64->bytes [x]
  (->> x (<- .getBytes java.nio.charset.StandardCharsets/ISO_8859_1)
         (.decode (java.util.Base64/getDecoder))))


(defmethod conn/event-msg-handler :amazon/cloud-drive
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (when ?reply-fn
    (let [to-send ;(io/get "/Users/alexandergunnarson/Quanta/Keys/Amazon.cljx")
          (binding [auth/*mem?* true]
            (amz-auth/refresh-token! nil)
            {:cloud-drive
              {:access-tokens
                {:current
                  {:access-token
                    (auth/access-token :amazon :cloud-drive)}}}})]
      (?reply-fn
        (-> to-send
            (conv/->transit :json)
            (quantum.security.cryptography/aes :encrypt (System/getProperty "use-this"))
            ((fn [x]
               (->> x (map (fn [[k v]] [k (->base-64 v)])) (into {}))))
            (conv/->transit :json))))))
