(ns ^{:doc "The top level Datomic (and friends, e.g. DataScript) namespace"}
  quantum.db.datomic
  (:refer-clojure :exclude [assoc dissoc conj disj update])
  (:require-quantum [:core err core-async pr log logic fn cbase tpred async])
  (:require
   #?(:clj  [clojure.core                     :as c        ]
      :cljs [cljs.core                        :as c        ])
   #?(:cljs [cljs-uuid-utils.core             :as uuid     ])
   #?(:clj  [datomic.api                      :as bdb      ]
      :cljs [datomic-cljs.api                 :as bdb      ])
            [datascript.core                  :as mdb      ]
            [quantum.db.datomic.core          :as db       ]
            [quantum.db.datomic.reactive.core :as rx-db    ]
    ;#?(:clj [quantum.deploy.amazon           :as amz      ])
            [com.stuartsierra.component       :as component]
            [quantum.core.collections         :as coll     ]
            [quantum.core.resources           :as res      ]
    #?(:clj [quantum.core.process             :as proc     ]))
  #?(:cljs (:require-macros
            [datomic-cljs.macros   
              :refer [<?]                                  ]))
  #?(:clj (:import datomic.Peer
                   [datomic.peer LocalConnection Connection]
                   java.util.concurrent.ConcurrentHashMap)))

; TODO use potemkin here
; |def| instead of |defalias| because CLJS doen't like how it's meta-ing atom
(def db*   db/db*  )
(def conn* db/conn*)
(def part* db/part*)

(defalias q            db/q)
(defalias transact!    db/transact!)
(defalias entity       db/entity)

(defalias conj         db/conj  )
(defalias disj         db/disj  )
(defalias assoc        db/assoc )
(defalias dissoc       db/dissoc)
(defalias update       db/update)

(defalias history->seq db/history->seq)
(defalias block->schemas db/block->schemas)
(defalias add-schemas! db/add-schemas!)

; CORE FUNCTIONS

(defn rx-q
  "Reactive |q|. Must be called within a Reagent component and will only
   update the component whenever the data it is querying has changed."
  {:todo ["Add Clojure support"]}
  ([query] (rx-q query @conn*))
  ([query conn & args] (apply rx-db/q conn query args)))

(defn rx-pull
  "Reactive |pull|. Only attempts to pull any new data if there has been a
   transaction of any datoms that might have changed the data it is looking at."
  {:todo ["Add Clojure support"]}
  ([selector eid] (rx-pull @conn* selector eid))
  ([conn selector eid] (rx-db/pull conn selector eid)))

(defn rx-transact!
  "Buffers its transactions in 1/60 second intervals, passes them through
   any handlers set up in |rx-db/before-tx!|, then batch transacts them to the database."
  {:todo ["Add (better) Clojure support"]}
  ([tx-data]      (rx-transact! @conn* tx-data))
  ([conn tx-data] (rx-db/transact! conn tx-data)))

(defn init-schemas!
  "Transacts @schemas to the partition @part on the database connection @conn.
   Expects @schemas to be in block-format (see |db/block->schemas|)."
  ([schemas] (init-schemas! @conn* schemas))
  ([conn schemas]
    (assert (nempty? schemas) #{schemas})
          
    (log/pr :debug "Initializing database with schemas...")
    
    (with (db/add-schemas! conn (db/block->schemas schemas {:conn conn}))
      (log/pr :debug "Schema initialization complete."))))


; RECORDS

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
   init-schemas? schemas]
  component/Lifecycle
    (start [this]
      (log/pr :user "Starting Ephemeral database...")
      (let [; Maintain DB history.
            history (when (pos? history-limit) (atom []))
            conn-f (mdb/create-conn)
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
            _ (when init-schemas? (init-schemas! conn-f schemas))
            ; Sets up the tx-report listener for a conn
            #?@(:cljs [_ (when reactive? (rx-db/react! conn-f))]) ; Is this enough? See also quantum.system
            _ (log/pr :user "Ephemeral database reactivity set up.")]
        (c/assoc this :conn              conn-f
                      :history           history
                      :default-partition default-partition-f)))
    (stop [this]
      (when (atom? conn)
        (reset! conn nil)) ; TODO is this wise?
      this))

(defrecord
  ^{:doc "Datomic database.

          @start-txr? is a boolean which defines whether the transactor should be started.
          @partitions is a seq (preferably set) of keywords identifying partitions"
    :todo ["Decompose this"]}
  BackendDatabase
  [type
   name db-name table-name instance-name ; <- TODO disambiguate these three
   host port rest-port uri conn create-if-not-present?
   start-txr? txr-bin-path txr-props-path txr-dir txr-process txr-alias
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
                              (str "http://" host ":" rest-port "/" txr-alias "/" name)
                            :dynamo nil
                              #_(str "datomic:ddb://"    (amz/get-server-region instance-name)
                                   "/" db-name
                                   "/" table-name
                                   "?aws_access_key_id=" (amz/get-aws-id     instance-name)
                                   "&aws_secret_key="    (amz/get-aws-secret instance-name))
                            (throw (->ex :illegal-argument
                                         "Database type not supported"
                                         type)))
            txr-process-f
              (when start-txr?
                #?(:clj (let [proc (component/start
                                     (proc/->proc txr-bin-path
                                       [txr-props-path]
                                       {:pr-to-out? true
                                        :dir        txr-dir}))]
                          (log/pr :debug "Starting transactor..." (kmap txr-bin-path txr-props-path txr-dir))
                          (async/sleep 3000)
                          proc)))
            connect (fn [] (log/pr :debug "Trying to connect with" uri-f)
                           (let [conn-f (do #?(:clj  (bdb/connect uri-f)
                                               :cljs (bdb/connect host rest-port txr-alias name)))]
                             (log/pr :debug "Connection successful.")
                             conn-f))
            conn-f  (try 
                      (try-times 5 1000
                        (try (connect)
                          (catch #?(:clj RuntimeException :cljs js/Error) e
                            (log/pr :warn "RuntimeException while trying to connect:" e)
                            (when (and #?(:clj
                                           (-> e .getMessage
                                               (=  (str "Could not find " name " in catalog")))
                                          :cljs "TODO")
                                       create-if-not-present?)
                              (log/pr :warn "Creating database...")
                              #?(:clj  (Peer/createDatabase uri-f)
                                 :cljs (go (<? (bdb/create-database host rest-port txr-alias name)))))
                            (throw e))
                          (catch #?(:clj Throwable :cljs js/Error) e
                            (log/pr :warn "Error while trying to connect:" e)
                            (throw e))))
                      (catch #?(:clj Throwable :cljs js/Error) e
                        (log/pr :warn "Failed to connect:" e)
                        (throw e)))
            _ (reset! conn conn-f)
            default-partition-f (or default-partition :db.part/test)
            _ (when init-schemas? (init-schemas! conn-f schemas))]

      (c/assoc this
        :uri               uri-f
        :txr-process       txr-process-f
        :default-partition default-partition-f)))
    (stop [this]
      (when (and (atom? conn) (nnil? @conn))
        #?(:clj (bdb/release @conn))
        (reset! conn nil))
      (when txr-process
        (component/stop txr-process))
      this))

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
      (let [ephemeral-f  (when ephemeral  (component/start ephemeral ))
            backend-f    (when backend    (component/start backend   ))
            reconciler-f (when reconciler (component/start reconciler))]
        (c/assoc this
          :ephemeral  ephemeral-f
          :reconciler reconciler-f
          :backend    backend-f)))
    (stop [this]
      (let [reconciler-f (when reconciler (component/stop reconciler))
            ephemeral-f  (when ephemeral  (component/stop ephemeral ))
            backend-f    (when backend    (component/stop backend   ))]
        (c/assoc this
          :ephemeral  ephemeral-f
          :reconciler reconciler-f
          :backend    backend-f))))

(defn ->db
  "Constructor for |Database|."
  [{{:keys [type name host port rest-port txr-alias create-if-not-present?] :as backend}
    :backend
    {:keys [] :as reconciler}
    :reconciler
    {:keys [history-limit] :as ephemeral}
    :ephemeral
    :as config}]
  (log/pr :user (kmap config))
  (when backend
    (err/assert (contains? #{:free :http} type)) ; TODO for now
    (err/assert ((fn-and string? nempty?) name))
    (err/assert ((fn-and string? nempty?) host))
    (err/assert (integer? port))
    (err/assert ((fn-or nil? integer?) port))
    (err/assert ((fn-or nil? string?)  txr-alias))
    (err/assert ((fn-or nil? boolean?) create-if-not-present?)))

  (when ephemeral
    (err/assert ((fn-or nil? integer?) history-limit)))

  (Database.
    (when ephemeral
      (map->EphemeralDatabase
        (c/assoc ephemeral :history-limit (or history-limit 0))))
    reconciler
    (when backend
      (map->BackendDatabase 
        (c/assoc backend :uri  (atom nil)
                         :conn (atom nil))))))