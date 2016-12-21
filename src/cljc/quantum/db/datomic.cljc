(ns ^{:doc "The top level Datomic (and friends, e.g. DataScript) namespace"}
  quantum.db.datomic
  (:refer-clojure :exclude
    [conj conj! disj disj!
     assoc assoc! dissoc dissoc! update merge
     boolean?])
  (:require
    [clojure.core                     :as c]
#?@(:clj
   [[datomic.api                      :as bdb]
  #_[quantum.deploy.amazon            :as amz]]
    :cljs
   [[datomic-cljs.api                 :as bdb]
    [cljs-uuid-utils.core             :as uuid] ; TODO have a quantum UUID ns
    [posh.core                        :as rx-db]])
    [datascript.core                  :as mdb]
    [quantum.db.datomic.core          :as dbc]
    [com.stuartsierra.component       :as component]
    [quantum.core.collections         :as coll
      :refer [kmap containsv?]]
    [quantum.core.error               :as err
      :refer [->ex try-times TODO]]
    [quantum.core.fn                  :as fn
      :refer [fn1 fn->]]
    [quantum.core.log                 :as log
      :include-macros true]
    [quantum.core.logic               :as logic
      :refer [fn-and fn-or whenf condf nnil? nempty?]]
    [quantum.core.process             :as proc]
    [quantum.core.resources           :as res]
    [quantum.core.string              :as str]
    [quantum.core.async               :as async
      :refer [go <?]]
    [quantum.core.type                :as type
      :refer [atom? boolean?]]
    [quantum.core.vars                :as var
      :refer [defalias]]
    [quantum.core.io.core             :as io]
    [quantum.core.convert             :as conv
      :refer [->name]]
    [quantum.core.paths               :as path]
    [quantum.parse.core               :as parse]
    [quantum.core.validate            :as v
      :refer [validate]]
    [quantum.validate.core
      :refer [no-blanks?]])
#?(:clj
  (:import
    datomic.Peer
    [datomic.peer LocalConnection Connection]
    java.util.concurrent.ConcurrentHashMap)))

#?(:clj (ns-unmap 'quantum.db.datomic 'with))

; TODO take out repetition
(defonce db*   dbc/db*  )
(defonce conn* dbc/conn*)
(defonce part* dbc/part*)

(defalias q                dbc/q        )
(defalias transact!        dbc/transact!)
(defalias with             dbc/with     )
(defalias entity           dbc/entity   )
(defalias touch            dbc/touch    )

(defalias conj             dbc/conj     )
(defalias conj!            dbc/conj!    )
(defalias disj             dbc/disj     )
(defalias disj!            dbc/disj!    )
(defalias assoc            dbc/assoc    )
(defalias assoc!           dbc/assoc!   )
(defalias dissoc           dbc/dissoc   )
(defalias dissoc!          dbc/dissoc!  )
(defalias update           dbc/update   )
(defalias update!          dbc/update!  )
(defalias merge            dbc/merge    )
(defalias merge!           dbc/merge!   )

(defalias history->seq     dbc/history->seq  )
#_(defalias block->schemas   dbc/block->schemas)
(defalias replace-schemas! dbc/replace-schemas!)
(defalias db->seq          dbc/db->seq       )

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

; TODO this fn is unnecessary
#_(:clj
(defn init-schemas!
  "Transacts @schemas to the partition @part on the database connection @conn.
   Expects @schemas to be in block-format (see |dbc/block->schemas|)."
  ([schemas] (init-schemas! @conn* schemas))
  ([conn schemas]
    (when schemas
      (log/pr :debug "Initializing database with schemas...")

      (fn/with (dbc/transact! conn (dbc/block->schemas schemas))
        (log/pr :debug "Schema initialization complete."))))))


; RECORDS / RESOURCES (TODO: MOVE)

(defrecord
  ^{:doc "Ephemeral (in-memory) database. Currently implemented as
          DataScript database. Once the reference to @conn is lost,
          the database is garbage-collected.

          @conn, while also a 'connection', in the case of DataScript is really an atom
          with the current DataScript DB value.

          One can set @init-schemas? to be true and the |start| function will transact
          @schemas to the database using |init-schemas!|. This is primarily useful
          for backend-syncing purposes where schemas are not just 'a good idea' but
          actually required.

          Likewise, e.g. DataScript has no built-in partitions, but they are nevertheless
          required for Datomic, and so for syncing purposes @default-partition is required
          to initialize @schemas."}
  EphemeralDatabase
  [conn history history-limit reactive? evented?
   default-partition
   schemas
   set-main-conn?
   set-main-part?
   post]
  component/Lifecycle
    (start [this]
      (TODO "Finish block schemas patch")
      (try
        (log/pr ::debug "Starting Ephemeral database...")
        (log/pr ::debug "EPHEMERAL:" (kmap post schemas set-main-conn? reactive?))
        (let [; Maintain DB history.
              history (when (pos? history-limit) (atom []))
              default-schemas {:db/ident {:db/unique :db.unique/identity}}
              default-partition-f (or default-partition :db.part/test)
              #_block-schemas #_(when schemas
                               (dbc/block->schemas schemas
                                 {:datascript? true
                                  :part        default-partition-f}))

              db        (mdb/empty-db {})
              conn-f    (atom db :meta
                          (c/merge {:listeners (atom {})}
                            (when evented?
                              {:subs         (atom {})
                               :transformers (atom {})})))
              schemas-f (c/merge default-schemas #_block-schemas)
              _ (replace-schemas! conn-f schemas-f)
              _ (log/pr ::debug "Ephemeral database and connection created.")
              _ (when (pos? history-limit)
                  (log/pr ::debug "Ephemeral database history set up.")
                  (mdb/listen! conn-f :history1 ; just ":history" doesn't work
                    (fn [tx-report]
                      (log/pr ::debug "Adding to history")
                      (let [{:keys [db-before db-after]} tx-report]
                        (when (and db-before db-after)
                          (swap! history
                            (fn-> (coll/drop-tail #(identical? % db-before))
                                  (c/conj db-after)
                                  (coll/trim-head history-limit))))))))
              ; Sets up the tx-report listener for a conn
              #?@(:cljs [_ (when reactive? (rx-db/posh! conn-f))]) ; Is this enough? See also quantum.system
              _ (log/pr ::debug "Ephemeral database reactivity set up. Conn's meta keys:" (-> conn-f meta keys))]
          (when set-main-conn? (reset! conn* conn-f))
          (when set-main-part? (reset! part* default-partition-f))
          (when post (post))
          (c/assoc this :conn              conn-f
                        :history           history
                        :default-partition default-partition-f))
        (catch #?(:clj Throwable :cljs :default) e
          (log/ppr :warn "Error in starting EphemeralDatabase"
            {:this this :err {:e e :stack #?(:clj (.getStackTrace e) :cljs (.-stack e))}})
          e)))
    (stop [this]
      (when (atom? conn)
        (reset! conn nil)) ; TODO is this wise?
      this))

(defn ->ephemeral-db
  [{:keys [history-limit] :as config}]
  (validate history-limit (v/or* nil? integer?))
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
                                        :data-dir               (validate (if res (path/path res "data"          ) "data"          ) no-blanks?)
                                        :log-dir                (validate (if res (path/path res "log"           ) "log"           ) no-blanks?)
                                        :pid-file               (validate (if res (path/path res "transactor.pid") "transactor.pid") no-blanks?)}
                                     (c/dissoc internal-props :path))]
                         (io/assoc! props-path-f
                           (parse/output :java-properties internal-props-f {:no-quote? true})
                           {:method :print})))
        _ (when (map? internal-props) (write-props!))
        _ (log/pr ::debug "Starting transactor..." (kmap datomic-path flags props-path-f resources-path))
        proc (res/start!
               (proc/->proc (path/path "." "bin" "transactor")
                 (c/conj (or flags []) props-path-f)
                 {:pr-to-out? true
                  :dir        datomic-path}))
        _ (when kill-on-shutdown?
            (.addShutdownHook (Runtime/getRuntime)
              (Thread. #(res/stop! proc))))]
    (async/sleep 5000)
    (log/pr ::debug "Done.")
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
   schemas]
  component/Lifecycle
    (start [this]
      (log/pr ::debug "Starting Datomic database...")
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
            connect (fn [] (log/pr ::debug "Trying to connect with" uri-f)
                           (let [conn-f (do #?(:clj  (bdb/connect uri-f)
                                               :cljs (bdb/connect host rest-port (:alias txr-props) name)))]
                             (log/pr ::debug "Connection successful.")
                             conn-f))
            create-db! (fn []
                         (when create-if-not-present?
                           (log/pr ::debug "Creating database...")
                           #?(:clj  (Peer/createDatabase uri-f)
                              :cljs (go (<? (bdb/create-database host rest-port (:alias txr-props) name))))
                           (log/pr ::debug "Done.")))
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
            ;_ (when schemas #?(:clj (init-schemas! conn-f schemas)))
            ]
      (log/pr ::debug "Datomic database initialized.")
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
  [{:keys [type name host port txr-alias create-if-not-present?
           default-partition]
    :or   {type :free name "test"
           host "0.0.0.0" port 4334
           create-if-not-present? true
           txr-alias         "local"
           default-partition :db.part/test}
    :as config}]
  (validate type                   #{:free :http}  ; TODO for now
            name                   (v/and string? nempty?)
            host                   (v/and string? nempty?)
            port                   integer? ; TODO `net/valid-port?`
            txr-alias              string?
            create-if-not-present? (fn1 boolean?)
            default-partition      (v/and keyword? (fn-> namespace (= "db.part"))))
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

::db/db
     {:backend

      #?@(:cljs
         [:ephemeral (when ephemeral
                       (merge ephemeral
                         {:history-limit  js/Number.MAX_SAFE_INTEGER
                          :reactive?      true
                          :set-main-conn? true
                          :set-main-part? true
                          :schemas        schemas}))])}

(res/register-component! ::db ->db [::log/log]) ; TODO maybe for :cljs need ::async/threadpool ?

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
