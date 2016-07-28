(ns ^{:doc "The top level Datomic (and friends, e.g. DataScript) namespace"}
  quantum.db.datomic
          (:refer-clojure :exclude [conj conj! disj disj!
                                    assoc assoc! dissoc dissoc! update merge
                                    boolean?])
          (:require [#?(:clj  clojure.core
                        :cljs cljs.core   )           :as c        ]
                    [#?(:clj  clojure.core.async
                        :cljs cljs.core.async)
                       :refer [#?(:clj go)]                        ]
           #?(:cljs [cljs-uuid-utils.core             :as uuid     ]) ; TODO have a quantum UUID ns
           #?(:clj  [datomic.api                      :as bdb      ]
              :cljs [datomic-cljs.api                 :as bdb      ])
                    [datascript.core                  :as mdb      ]
                    [quantum.db.datomic.core          :as db       ]
           #?(:cljs [posh.core                        :as rx-db    ])
           ;#?(:clj [quantum.deploy.amazon            :as amz      ])
                    [com.stuartsierra.component       :as component]
                    [quantum.core.collections         :as coll     
                       :refer [#?@(:clj [kmap containsv?])]        ]
                    [quantum.core.error               :as err
                       :refer [->ex #?(:clj try-times)]            ]
                    [quantum.core.fn                  :as fn
                       :refer [#?@(:clj [with])]                   ]
                    [quantum.core.log                 :as log      ]
                    [quantum.core.logic               :as logic
                       :refer [#?@(:clj [fn-and fn-or whenf condf])
                               nnil? nempty?]     ]
                    [quantum.core.resources           :as res      ]
                    [quantum.core.string              :as str      ]
            #?(:clj [quantum.core.process             :as proc     ])
                    [quantum.core.thread.async        :as async    ]
                    [quantum.core.type                :as type
                      :refer [atom? #?(:clj boolean?)]]
                    [quantum.core.vars                :as var
                      :refer [#?(:clj defalias)]                   ]
                    [quantum.core.io.core             :as io       ]
                    [quantum.core.convert             :as conv
                      :refer [->name]                              ]
                    [quantum.core.paths               :as path     ]
                    [quantum.parse.core               :as parse    ]
                    [quantum.validate.core            :as val
                      :refer [#?(:clj validate)]                   ])
  #?(:cljs (:require-macros
                    [cljs.core.async.macros
                       :refer [go]                                 ]
                    [datomic-cljs.macros   
                      :refer [<?]                                  ]
                    [quantum.core.collections         :as coll     
                       :refer [kmap containsv?]                    ]
                    [quantum.core.error               :as err
                       :refer [try-times]                          ]
                    [quantum.core.fn                  :as fn
                       :refer [with]                               ]
                    [quantum.core.logic               :as logic
                       :refer [fn-and fn-or]                       ]
                    [quantum.core.log                 :as log      ]
                    [quantum.core.thread.async        :as async    ]
                    [quantum.core.type                :as type
                      :refer [boolean?]                            ]
                    [quantum.core.vars                :as var
                      :refer [defalias]                            ]
                    [quantum.validate.core            :as val
                      :refer [validate]]))
  #?(:clj  (:import datomic.Peer
                    [datomic.peer LocalConnection Connection]
                    java.util.concurrent.ConcurrentHashMap)))

; TODO take out repetition
(defonce db*   db/db*  )
(defonce conn* db/conn*)
(defonce part* db/part*)

(defalias q              db/q        )
(defalias transact!      db/transact!)
(defalias entity         db/entity   )
(defalias touch          db/touch    )

(defalias conj           db/conj     )
(defalias conj!          db/conj!    )
(defalias disj           db/disj     )
(defalias disj!          db/disj!    )
(defalias assoc          db/assoc    )
(defalias assoc!         db/assoc!   )
(defalias dissoc         db/dissoc   )
(defalias dissoc!        db/dissoc!  )
(defalias update         db/update   )
(defalias update!        db/update!  )
(defalias merge          db/merge    )
(defalias merge!         db/merge!   )

(defalias history->seq   db/history->seq  )
(defalias block->schemas db/block->schemas)
(defalias add-schemas!   db/add-schemas!  )
(defalias db->seq        db/db->seq       )

; CORE FUNCTIONS

#?(:cljs
(defn rx-q
  "Reactive |q|. Must be called within a Reagent component and will only
   update the component whenever the data it is querying has changed."
  {:todo ["Add Clojure support"]}
  ([query] (rx-q query @conn*))
  ([query conn & args] (apply rx-db/q conn query args))))

#?(:cljs
(defn rx-pull
  "Reactive |pull|. Only attempts to pull any new data if there has been a
   transaction of any datoms that might have changed the data it is looking at."
  {:todo ["Add Clojure support"]}
  ([selector eid] (rx-pull @conn* selector eid))
  ([conn selector eid] (rx-db/pull conn selector eid))))

#?(:cljs
(defn rx-transact!
  "Buffers its transactions in 1/60 second intervals, passes them through
   any handlers set up in |rx-db/before-tx!|, then batch transacts them to the database."
  {:todo ["Add (better) Clojure support"]}
  ([tx-data]      (rx-transact! @conn* tx-data))
  ([conn tx-data] (rx-db/transact! conn tx-data))))

(defn init-schemas!
  "Transacts @schemas to the partition @part on the database connection @conn.
   Expects @schemas to be in block-format (see |db/block->schemas|)."
  ([schemas] (init-schemas! @conn* schemas))
  ([conn schemas]
    (when schemas
      (log/pr :debug "Initializing database with schemas...")
      
      (with (db/add-schemas! conn (db/block->schemas schemas {:conn conn}))
        (log/pr :debug "Schema initialization complete.")))))


; RECORDS / RESOURCES (TODO: MOVE)

(defrecord
  ^{:doc "Ephemeral (in-memory) database. Currently implemented as
          DataScript database. Once the reference to @conn is lost,
          the database is garbage-collected.

          @conn, while also a 'connection', in the case of DataScript is really an atom
          with the current DataScript DB value.

          Though e.g. DataScript has no schemas (or at least they server no purpose),
          one can set @init-schemas? to be true and the |start| function will transact
          @schemas to the database using |init-schemas!|. This is mainly only useful
          for backend-syncing purposes where schemas are not just 'a good idea' but
          actually required.

          Likewise, e.g. DataScript has no built-in partitions, but they are nevertheless
          required for Datomic, and so for syncing purposes @default-partition is required
          to initialize @schemas."}
  EphemeralDatabase
  [conn history history-limit reactive?
   default-partition
   init-schemas? schemas
   set-main-conn?
   post]
  component/Lifecycle
    (start [this]
      (log/pr :user "Starting Ephemeral database...")
      (log/pr :user "EPHEMERAL:" (kmap post schemas set-main-conn? init-schemas? reactive?))
      (let [; Maintain DB history.
            history (when (pos? history-limit) (atom []))
            conn-f (mdb/create-conn
                     (if init-schemas?
                         (db/block->schemas schemas)
                         {}))
            _ (when (pos? history-limit)
                (log/pr :user "Ephemeral database history set up.")
                (mdb/listen! conn-f :history1 ; just ":history" doesn't work
                  (fn [tx-report]
                    (log/pr :user "Adding to history")
                    (let [{:keys [db-before db-after]} tx-report]
                      (when (and db-before db-after)
                        (swap! history (fn [h]
                          (-> h
                              (coll/drop-tail #(identical? % db-before))
                              (c/conj db-after)
                              (coll/trim-head history-limit)))))))))
            default-partition-f (or default-partition :db.part/test)
            ; Sets up the tx-report listener for a conn
            #?@(:cljs [_ (when reactive? (rx-db/posh! conn-f))]) ; Is this enough? See also quantum.system
            _ (log/pr :user "Ephemeral database reactivity set up.")]
        (when set-main-conn? (reset! conn* conn-f))
        (when post (post))
        (c/assoc this :conn              conn-f
                      :history           history
                      :default-partition default-partition-f)))
    (stop [this]
      (when (atom? conn)
        (reset! conn nil)) ; TODO is this wise?
      this))

(defn ->ephemeral-db
  [{:keys [history-limit] :as config}]
  (err/assert ((fn-or nil? integer?) history-limit))
  (map->EphemeralDatabase
    (c/assoc config :history-limit (or history-limit 0))))

#?(:clj
(defn start-transactor!
  [{:keys [type host port]}
   {:keys [kill-on-shutdown? datomic-path flags resources-path internal-props]
    :as   txr-props}]
  (let [res          resources-path
        props-path-f (condf internal-props
                            string? identity
                            map?    (fn-or :path
                                           (fn [_]
                                             (path/path resources-path
                                               (str (-> "generated" gensym name) ".properties"))))
                            #(throw (->ex nil "Invalid transactor props" %)))
        write-props! (fn write-props! []
                       (let [internal-props-f
                              (c/merge {:protocol               (name type)
                                        :host                   host ; "localhost"
                                        :port                   port ; 4334
                                         ; TODO dynamically determine based on flag passed
                                        :memory-index-threshold "32m" ; Recommended settings for -Xmx1g usage 
                                        :memory-index-max       "128"
                                        :object-cache-max       "128m"
                                        :data-dir               (validate val/no-blanks?
                                                                  (if res (path/path res "data"          ) "data"          ))
                                        :log-dir                (validate val/no-blanks?
                                                                  (if res (path/path res "log"           ) "log"           ))
                                        :pid-file               (validate val/no-blanks?
                                                                  (if res (path/path res "transactor.pid") "transactor.pid"))}
                                     (c/dissoc internal-props :path))]
                         (io/assoc! props-path-f
                           (parse/output :java-properties internal-props-f {:no-quote? true})
                           {:method :print})))
        _ (when (map? internal-props) (write-props!))
        _ (log/pr :debug "Starting transactor..." (kmap datomic-path flags props-path-f resources-path))
        proc (res/start!
               (proc/->proc (path/path datomic-path "bin" "transactor")
                 (c/conj (or flags []) props-path-f)
                 {:pr-to-out? true
                  :dir        datomic-path}))
        _ (when kill-on-shutdown?
            (.addShutdownHook (Runtime/getRuntime)
              (Thread. #(res/stop! proc))))]
    (async/sleep 5000)
    (log/pr :debug "Done.")
    proc)))

(defrecord
  ^{:doc "Datomic database.

          @start-txr? is a boolean which defines whether the transactor should be started.
          @partitions is a seq (preferably set) of keywords identifying partitions"
    :todo ["Decompose this"]}
  BackendDatabase
  [type
   name table-name instance-name ; <- TODO disambiguate these three
   host port rest-port uri conn create-if-not-present?
   txr-props txr-process
   init-partitions? partitions
   default-partition
   init-schemas? schemas]
  component/Lifecycle
    (start [this]
      ; Set all transactor logs to WARN 
      #?(:clj (try
                (doseq [^ch.qos.logback.classic.Logger logger
                          (->> (ch.qos.logback.classic.util.ContextSelectorStaticBinder/getSingleton)
                               (.getContextSelector)
                               (.getLoggerContext)
                               (.getLoggerList))]
                  (.setLevel logger ch.qos.logback.classic.Level/WARN))
                (catch NullPointerException e)))
      (let [uri-f (condp = type
                            :free
                              (str "datomic:" (c/name type)
                                   "://" host ":" port "/" name)
                            :mem
                              (str "datomic:" (c/name type)
                                   "://" name)
                            :http
                              (str "http://" host ":" rest-port "/" (:alias txr-props) "/" name)
                            :dynamo nil
                              #_(str "datomic:ddb://"    (amz/get-server-region instance-name)
                                   "/" name
                                   "/" table-name
                                   "?aws_access_key_id=" (amz/get-aws-id     instance-name)
                                   "&aws_secret_key="    (amz/get-aws-secret instance-name))
                            (throw (->ex :illegal-argument
                                         "Database type not supported"
                                         type)))
            txr-process-f
              (when (:start? txr-props)
                #?(:clj (start-transactor! (kmap type host port) txr-props)))
            connect (fn [] (log/pr :debug "Trying to connect with" uri-f)
                           (let [conn-f (do #?(:clj  (bdb/connect uri-f)
                                               :cljs (bdb/connect host rest-port (:alias txr-props) name)))]
                             (log/pr :debug "Connection successful.")
                             conn-f))
            create-db! (fn []
                         (when create-if-not-present?
                           (log/pr :debug "Creating database...")
                           #?(:clj  (Peer/createDatabase uri-f)
                              :cljs (go (<? (bdb/create-database host rest-port (:alias txr-props) name))))
                           (log/pr :debug "Done.")))
            conn-f  (try 
                      (try-times 5 1000
                        (try (connect)
                          (catch #?(:clj Throwable :cljs js/Error) e
                            (log/pr :warn "Error while trying to connect:" e)
                            #?(:clj (when (-> e .getMessage (containsv? #"Could not find .* in catalog"))
                                      (create-db!)))
                            (throw e))))
                      (catch #?(:clj Throwable :cljs js/Error) e
                        (log/pr :warn "Failed to connect:" e)
                        (throw e)))
            _ (log/pr :debug "Connected.")
            _ (reset! conn conn-f)
            default-partition-f (or default-partition :db.part/test)
            _ (when init-schemas? (init-schemas! conn-f schemas))]
      (log/pr :debug "Datomic database initialized.")
      (c/assoc this
        :uri               uri-f
        :txr-process       txr-process-f
        :default-partition default-partition-f)))
    (stop [this]
      (when (and (atom? conn) (nnil? @conn))
        #?(:clj (bdb/release @conn))
        (reset! conn nil))
      (when txr-process
        (res/stop! txr-process))
      this))

(defn ->backend-db
  [{:keys [type name host port txr-alias create-if-not-present?]
    :as config}]
  ; TODO change these things to use schema?  
  (err/assert (contains? #{:free :http} type)) ; TODO for now
  (err/assert ((fn-and string? nempty?) name))
  (err/assert ((fn-and string? nempty?) host))
  (err/assert ((fn-or nil? integer?) port))
  (err/assert ((fn-or nil? string?)  txr-alias))
  (err/assert ((fn-or nil? boolean?) create-if-not-present?))
  (map->BackendDatabase 
    (c/assoc config :uri  (atom nil)
                    :conn (atom nil))))

(defrecord
  ^{:doc "Database-system consisting of an EphemeralDatabase (e.g. DataScript),
          BackendDatabase (e.g. Datomic), and a reconciler which constantly
          pushes diffs from the EphemeralDatabase to the BackendDatabase
          and pulls new data from the BackendDatabase.

          A Datomic subscription model would be really nice for performance
          (ostensibly) to avoid the constant backend polling of the reconciler,
          but unfortunately Datomic does not have this.

          @backend
            Can be one of three things:
              1) A direct connection to a Datomic database using the Datomic Peer API
                 - This option is for Clojure (e.g. server) only, not ClojureScript
              2) A direct connection to a Datomic database using the Datomic HTTP API
                 - This option is currently not proven to be secure and is awaiting
                   further developments by the Cognitect team.
              3) A REST endpoint pair:
                 - One for pushing, e.g. 'POST /db'
                 - One for pulling, e.g. 'GET  /db'
                 - This way the Datomic database is not directly exposed to the client,
                   but rather the server is able to use access control and other
                   security measures when handling queries from the client.
                   This is the (currently) recommended option.
          @reconciler
            is"}
  Database
  [ephemeral reconciler backend]
  ; TODO code pattern here
  component/Lifecycle
    (start [this]
      (let [ephemeral-f  (when ephemeral  (res/start! ephemeral ))
            backend-f    (when backend    (res/start! backend   ))
            reconciler-f (when reconciler (res/start! reconciler))]
        (c/assoc this
          :ephemeral  ephemeral-f
          :reconciler reconciler-f
          :backend    backend-f)))
    (stop [this]
      (let [reconciler-f (when reconciler (res/stop! reconciler))
            ephemeral-f  (when ephemeral  (res/stop! ephemeral ))
            backend-f    (when backend    (res/stop! backend   ))]
        (c/assoc this
          :ephemeral  ephemeral-f
          :reconciler reconciler-f
          :backend    backend-f))))

(defn ->db
  "Constructor for |Database|."
  [{:keys [backend reconciler ephemeral] :as config}]
  (log/pr ::resources (kmap config))
  (Database.
    (whenf ephemeral nnil? ->ephemeral-db)
    reconciler
    (whenf backend   nnil? ->backend-db  )))

(defmethod io/persist! EphemeralDatabase
  [_ persist-key
   {:keys [db history] :as persist-data}
   {:keys [schema]     :as opts        }]
  #?(:cljs
    (when (-> db meta :listeners (c/get persist-key))
      (throw (->ex :duplicate-persisters
                   "Cannot have multiple ClojureScript Persisters for DataScript database"))))
    ; restoring once persisted DB on page load
    (or (when-let [stored (io/get (->name persist-key))]
          (let [stored-db (conv/->mdb stored)]
            (when (= (:schema stored-db) schema) ; check for code update
              (reset! db stored-db)
              (swap! history c/conj @db)
              true)))
        ; (mdb/transact! conn schema)
        )
    (mdb/listen! db :persister
      (fn [tx-report] ; TODO do not notify with nil as db-report
                      ; TODO do not notify if tx-data is empty
        (when-let [db (:db-after tx-report)]
          (go (io/assoc! persist-key db))))))