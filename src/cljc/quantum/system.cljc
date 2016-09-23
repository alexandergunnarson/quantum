(ns ^{:doc "A namespace for bootstrapping/streamlining system creation."}
  quantum.system
           (:require [com.stuartsierra.component       :as component]
            #?(:cljs [reagent.core                     :as rx       ])
                     [quantum.core.core
                       :refer        [deref* lens]                  ]
                     [quantum.core.fn                  :as fn
                       :refer        [#?@(:clj [fn-> with-do])]
                       :refer-macros [fn-> with-do]]
                     [quantum.core.log                 :as log
                       :include-macros true                         ]
                     [quantum.core.macros.core         :as cmacros
                       :refer        [#?@(:clj [if-cljs])]          ]
                     [quantum.core.resources           :as res      ]
                     [quantum.core.async               :as async    ]
                     [quantum.db.datomic               :as db       ]
                     [quantum.db.datomic.core          :as dbc      ]
                     [quantum.db.datomic.reactive.core :as db-rx    ]
                     [quantum.net.http                 :as http     ]
                     [quantum.net.websocket            :as conn     ]
                     [quantum.ui.core                  :as ui       ]))

(log/this-ns)

(defn default-config
  "A decent default configuration for a web app.
   TODO More default configs to follow.

   @frontend-init : a frontend init function"
  {:usage '(default-config
             {:server     {:routes         (*var router/routes)
                           :key-password   "password"
                           :trust-password "password"}
              :db         {:schemas        {:my/schema [:string :one {:unique? true}]}}
              :connection {:msg-handler    conn/ws-msg-handler}
              :frontend   {:init           init-ui!
                           :render         ui-render-fn
                           :root-id        "app"}})}
  [& [{:as config
       {:keys [port ssl-port host]          :as server    } :server
       {:keys [endpoint msg-handler]        :as connection} :connection
       {:keys [js-source-file]                            } :deployment
       {:keys [schemas ephemeral backend]   :as db        } :db
       {:keys [render root-id]              :as frontend  } :frontend
       {                                    :as threadpool} :threadpool}]]
  (let [host*            (or host "0.0.0.0")
        port*            (or port 80)
        server-type      :aleph ; :immutant
        js-source-file-f (or js-source-file "system")
        frontend-init    (-> config :frontend :init)]
  {:log
     {:levels                   #{:debug :warn}}
   #?@(:clj
  [:server
     (when server
       (merge
         {:host                     host*
          :port                     port*
          :ssl-port                 ssl-port
          :type                     server-type
          :http2?                   true}
         server))])
   :connection
     (when connection
       (merge
         {:endpoint                 (or endpoint "/chan")
          #?@(:cljs
         [:host                     (str host* ":" (or ssl-port port))])
          :packer                   :edn
          #?@(:clj
         [:server-type              server-type])
          :msg-handler              msg-handler}
         connection))
   :renderer
     (when frontend
       {:init-fn                    frontend-init
        :render-fn                  render
        :root-id                    (or root-id "root")
        :type                       :reagent})
   :db
     {:backend
       (when backend
         (merge
           {:type                   :free
            :name                   "test"
            :host                   "0.0.0.0"
            :port                   4334
            :create-if-not-present? true
            :default-partition      :db.part/test
            :schemas                schemas ; TODO update these
            :txr-props {:start?   true
                        :alias    "local"}})
           backend)
      #?@(:cljs
         [:ephemeral (when ephemeral
                       (merge ephemeral
                         {:history-limit  js/Number.MAX_SAFE_INTEGER
                          :reactive?      true
                          :set-main-conn? true
                          :set-main-part? true
                          :schemas        schemas}))])}
   #?@(:cljs
    [:threadpool
        (when threadpool
          {:thread-ct 2
           ; This is whatever the name of the compiled JavaScript will be
           :script-src (str "./js/compiled/" js-source-file-f ".js")})])
}))

(defn gen-system-creator [system-kw config]
  (delay
    (do (log/pr ::debug "Registering system...")
        (with-do
          (res/register-system!
            system-kw
            config
            (fn [{:as config-0 :keys [connection log threadpool db renderer server]}]
              (->> (conj {}
                     (when log        [:log
                                        (log/->log-initializer     log)])
                 #?(:cljs
                     (when threadpool [:threadpool
                                        (component/using
                                          (async/->threadpool      threadpool)
                                          [:log])]))
                     (when db         [:db
                                        (component/using
                                          (db/->db                 db)
                                          (if (and threadpool #?(:clj false :cljs true))
                                              [:log :threadpool]
                                              [:log]))])
                #?(:cljs
                     (when renderer   [:renderer
                                        (component/using
                                          (ui/map->Renderer        renderer)
                                          [:log :db])]))
                 #?(:clj
                     (when server     [:server
                                        (component/using
                                          (http/map->Server        server)
                                          [:log])]))
                     (when connection [:connection
                                        (component/using
                                          (conn/map->ChannelSocket (:connection config-0))
                                          [:log #?(:clj :server)])]))
                   seq flatten
                   (apply component/system-map))))
          (log/pr ::debug "Registered system.")))))

(defn gen-main
  "Creates a standard |-main| function.
   For Clojure, this is for JAR packaging.
   For ClojureScript, this can be used e.g. with Figwheel's :main."
  [config system-creator system sys-map]
  (fn [& [port]]
    (when @system
      (res/stop! @system))
    @system-creator
    (res/go! @system)
    (reset! db/conn* (-> @sys-map :db #?(:clj :backend :cljs :ephemeral) :conn #?(:clj deref*)))
    #?(:clj (reset! dbc/part* (-> @sys-map :db :backend :default-partition)))

    #_(:cljs (when (-> @sys-map :db :ephemeral :reactive?)
               (db-rx/react! @dbc/conn*)
               (log/pr ::debug "Ephemeral DB made reactive."))) ; Is this necessary?
    true))

#?(:clj
(defmacro create-system-vars
  "Generally @system-kw should be simply ::system.
   @config : A config-map."
  [stop-system? system-kw config]
  (let [main-sym (with-meta '-main {:export true})
        system-code `(lens res/systems ~system-kw)
        err      (if-cljs &env 'js/Error 'Throwable)]
    `(do (def ~'system  ~system-code)
         (def ~'sys-map (lens ~'system (fn-> :sys-map deref*)))

         (try
           (when (and ~stop-system? (deref* ~system-code))
             (res/stop! @~system-code)
             (when-let [sys-map# (-> res/systems deref ~system-kw :sys-map deref)]
               (res/stop! sys-map#)))
           (catch ~err e#
             (log/pr :warn "Could not stop system." e#)))

         ; In CLJS, ~main-sym content can't refer to e.g. ~'sys-map unless
         ; it has been previously declared outside of the macro
         (defn ~main-sym [& args#]
           (when-not (async/web-worker?)
             (let [system-kw# ~system-kw
                   system#    (lens res/systems system-kw#)
                   sys-map#   (lens system# (fn-> :sys-map deref*))
                   config#    ~config]
               (apply (gen-main config#
                                (gen-system-creator system-kw# config#)
                                system#
                                sys-map#)
                      args#))))))))
