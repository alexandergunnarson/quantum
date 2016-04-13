(ns ^{:doc "A namespace for bootstrapping/streamlining system creation."}
  quantum.system
  (:require-quantum [:core async log err logic fn res debug])
  (:require [com.stuartsierra.component       :as component]
   #?(:cljs [reagent.core                     :as rx       ])
            [quantum.core.core                   
              :refer [deref* lens]                         ]
            [quantum.db.datomic               :as db       ]
            [quantum.db.datomic.core          :as dbc      ]
            [quantum.db.datomic.reactive.core :as db-rx    ]
            [quantum.net.http                 :as http     ]
            [quantum.net.websocket            :as conn     ]
            [quantum.ui.core                  :as ui       ]))

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
       {:keys [port ssl-port routes host]   :as server} :server
       {:keys [uri msg-handler]             :as connection} :connection
       {:keys [js-source-file]                            } :deployment
       {:keys [schemas ephemeral backend]   :as db        } :db
       {:keys [render root-id]                            } :frontend}]]
  (let [host*            (or "0.0.0.0" host)
        port*            (or port 80)
        server-type      :aleph ; :immutant
        js-source-file-f (or js-source-file "system")
        frontend-init    (-> config :frontend :init)]
  {:log
     {:levels                   #{:debug :warn}}
   #?@(:clj
  [:server
     (merge
       {:host                     host*
        :port                     port*
        :ssl-port                 ssl-port
        :routes                   routes
        :type                     server-type
        :http2?                   true}
       server)])
   :connection
     (when connection
       (merge
         {:uri                      (or uri "/chan")
          #?@(:cljs
         [:host                     (str host* ":" (or ssl-port port))])
          :packer                   :edn
          #?@(:clj
         [:server-type              server-type])
          :msg-handler              msg-handler}
         connection))
   :renderer
     {:init-fn                    frontend-init
      :render-fn                  render
      :root-id                    (or root-id "root")
      :type                       :reagent}
   :db
     {:backend 
       (when backend
         (merge
           {:type                   :free
            :name                   "test"
            :host                   "localhost"
            :port                   4334
            :create-if-not-present? true
            :txr-alias              "local"
            :default-partition      :db.part/test
            :init-schemas?          true
            :schemas                schemas
            #?@(:clj
           [:start-txr?             true
            :txr-dir                (str (System/getProperty "user.dir")
                                         "/resources/datomic-free-0.9.5344")
            :txr-bin-path           "./bin/transactor"
            :txr-props-path         "./config/samples/free-transactor-template.properties"])}
           backend))
      #?@(:cljs
         [:ephemeral (merge ephemeral
                       {:history-limit  js/Number.MAX_SAFE_INTEGER
                        :reactive?      true
                        :set-main-conn? true
                        :schemas        schemas})])}
   #?@(:cljs
    [:threadpool
        {:thread-ct 2
         ; This is whatever the name of the compiled JavaScript will be
         :script-src (str "./js/compiled/" js-source-file-f ".js")}])
        }))

(defn gen-system-creator [system-kw config]
  (delay
    (res/register-system!
      system-kw
      config
      (fn [{:as config-0 :keys [connection]}]
        (apply component/system-map
          :log           (log/->log-initializer     (:log        config-0))
    #?@(:cljs
         [:threadpool    (component/using 
                           (async/->threadpool      (:threadpool config-0))
                           [:log])])
          :db            (component/using 
                           (db/->db                 (:db         config-0))
                           [:log #?(:cljs :threadpool)])
    #?@(:cljs
         [:renderer      (component/using
                           (ui/map->Renderer        (:renderer   config-0))
                           [:log :db])])
    #?@(:clj
         [:server        (component/using 
                           (http/map->Server        (:server     config-0))
                           [:log])])
         (when connection
           [:connection
            (component/using 
              (conn/map->ChannelSocket (:connection config-0))
              [:log :server])]))))))

(defn gen-main
  "Creates a standard |-main| function.
   For Clojure, this is for JAR packaging.
   For ClojureScript, this can be used e.g. with Figwheel's :main."
  [config system-creator system sys-map]
  (fn [& [port]]
    @system-creator
    (res/reload! @system)
    (reset! db/conn* (-> @sys-map :db #?(:clj :backend :cljs :ephemeral) :conn #?(:clj deref*)))
    #?(:clj (reset! dbc/part* (-> @sys-map :db :backend :default-partition)))
    
    #?(:cljs (when (-> @sys-map :db :ephemeral :reactive?)
               (db-rx/react! @dbc/conn*))) ; Is this necessary?
    ))

#?(:clj
(defmacro create-system-vars
  "Generally @system-kw should be simply ::system.
   @config : A config-map."
  [system-kw config]
  (let [main-sym (with-meta '-main {:export true})
        system-code `(lens res/systems ~system-kw)
        err      (if-cljs &env 'js/Error 'Throwable)]
    `(do (def ~'system  ~system-code)
         (def ~'sys-map (lens ~'system (fn-> :sys-map deref*)))
         
         (try
           (when (deref* ~system-code)
             (res/stop! @~system-code)
             (when-let [sys-map# (-> res/systems deref ~system-kw :sys-map deref)]
               (component/stop sys-map#)))
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
