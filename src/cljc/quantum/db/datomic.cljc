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
    [posh.reagent                     :as rx-db]])
    [datascript.core                  :as mdb]
    [quantum.db.datomic.core          :as dbc]
    [quantum.db.datomic.schema        :as dbs]
    [com.stuartsierra.component       :as component]
    [quantum.core.collections         :as coll
      :refer [kmap containsv? nnil? nempty?]]
    [quantum.core.error               :as err
      :refer [->ex try-times TODO]]
    [quantum.core.fn                  :as fn
      :refer [fn1 fn->]]
    [quantum.core.log                 :as log
      :include-macros true]
    [quantum.core.logic               :as logic
      :refer [fn-and fn-or whenf condf default]]
    [quantum.core.process             :as proc]
    [quantum.core.resources           :as res]
    [quantum.core.string              :as str]
    [quantum.core.async               :as async
      :refer [go <?]]
    [quantum.core.type                :as type
      :refer [atom? boolean?]]
    [quantum.core.vars                :as var
      :refer [defalias defaliases]]
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

(defonce db*   dbc/db*  )
(defonce conn* dbc/conn*)
(defonce part* dbc/part*)

(defaliases dbc
  q transact! with entity touch pull pull-many
  conj conj! disj disj! assoc assoc! dissoc dissoc!
  update update! merge merge!
  history->seq db->seq ->db)

(defalias replace-schemas! dbs/replace-schemas!)

; CORE FUNCTIONS

#?(:cljs
(defn rx-q
  "Reactive |q|. Must be called within a Reagent component and will only
   update the component whenever the data it is querying has changed."
  {:todo ["Add Clojure support"]}
  ([query] (rx-q query @conn*))
  ([query conn & args] (apply rx-db/q query conn args))))

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
          to initialize @schemas."
    :todo {0 "Make the history recording *much* more efficient"}}
  EphemeralDatabase
  [conn history history-limit reactive? evented?
   default-partition
   schemas
   set-main-conn?
   set-main-part?
   post]
  component/Lifecycle
    (start [this]
      (log/pr ::debug "Starting Ephemeral database...")
      (let [history-limit  (validate (or history-limit
                                         #?(:clj  Integer/MAX_VALUE
                                            :cljs js/Number.MAX_SAFE_INTEGER)) integer?)
            reactive?      (validate (default reactive?      true ) (fn1 boolean?))
            set-main-conn? (validate (default set-main-conn? false) (fn1 boolean?))
            set-main-part? (validate (default set-main-part? false) (fn1 boolean?))
            _              (validate conn nil?)]
        (try
          (log/pr ::debug "EPHEMERAL:" (kmap post schemas set-main-conn? reactive?))
          (let [; Maintain DB history.
                history (when (pos? history-limit) (atom []))
                default-schemas {:db/ident {:db/unique :db.unique/identity}}
                default-partition-f (or default-partition :db.part/test)
                db        (mdb/empty-db {})
                conn-f    (atom db :meta
                            (c/merge {:listeners (atom {})}
                              (when evented? ; The re-frame + posh abstraction
                                {:subs         (atom {})
                                 :transformers (atom {})})))
                schemas-f (c/merge default-schemas schemas)
                _ (replace-schemas! conn-f schemas-f)
                _ (log/pr ::debug "Ephemeral database and connection created.")
                _ (when (pos? history-limit)
                    (log/pr ::debug "Ephemeral database history set up.")
                    (mdb/listen! conn-f :history1 ; just ":history" doesn't work
                      (fn [tx-report]
                        (log/pr ::debug "Adding to history")
                        (let [{:keys [db-before db-after]} tx-report]
                          (when (and db-before db-after) ; TODO 0
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
            this))))
    (stop [this]
      (when (atom? conn)
        (reset! conn nil)) ; TODO is this wise? ; TODO unregister all listeners?
      this))

(def ->ephemeral-db map->EphemeralDatabase)

(res/register-component! ::ephemeral ->ephemeral-db [::log/log])

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
                            #(throw (->ex "Invalid transactor props" %)))
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
          @partitions is a seq (preferably set) of keywords identifying partitions

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
                 This is the (currently) recommended option."
    :todo ["Decompose this"]}
  BackendDatabase
  [type
   name auth
   host port rest-port uri conn
   set-main-conn? set-main-part? create? create-if-not-present?
   txr-props txr-process
   connection-retries
   partitions
   default-partition
   schemas]
  component/Lifecycle
    (start [this]
      (log/pr ::debug "Starting Datomic database...")
      (let [type                   (validate (or type :free)                        #{:free :http :dynamo :mem}) ; TODO for now; how does :dev differ?
            name                   (validate (or name "test")                       (v/and string? nempty?))
            host                   (validate (or host "localhost")                  (v/and string? nempty?))
            port                   (validate (or port 4334)                         integer?) ; TODO `net/valid-port?`
            txr-alias              (validate (or (:alias txr-props) "local")        string?)
            create?                (validate (default create?                false) (fn1 boolean?))
            create-if-not-present? (validate (default create-if-not-present? true ) (fn1 boolean?))
            set-main-conn?         (validate (default set-main-conn?         false) (fn1 boolean?))
            set-main-part?         (validate (default set-main-part?         false) (fn1 boolean?))
            default-partition      (validate (or default-partition :db.part/test)   (v/and keyword? (fn-> namespace (= "db.part"))))
            conn                   (validate (or conn (atom nil))                   atom?)
            connection-retries     (validate (or (if (= type :dynamo) 1 5))         integer?) ; DynamoDB auto-retries
            uri (case type
                      :free
                        (str "datomic:" (c/name type)
                             "://" host ":" port "/" name)
                      :mem
                        (str "datomic:" (c/name type)
                             "://" name)
                      :http
                        (str "http://" host ":" rest-port "/" (:alias txr-props) "/" name)
                      :dynamo
                        (str "datomic:ddb://"      (validate (or (:server-region auth) "us-east-1") string?) ; TODO validate server regions better
                             "/"                   name
                             "/"                   (validate (:table-name auth) string?)
                             "?aws_access_key_id=" (validate (:id         auth) string?)
                             "&aws_secret_key="    (validate (:secret     auth) string?)))]
        ; Set all transactor logs to WARN
        #?(:clj (try
                  (doseq [^ch.qos.logback.classic.Logger logger
                            (->> (ch.qos.logback.classic.util.ContextSelectorStaticBinder/getSingleton)
                                 (.getContextSelector)
                                 (.getLoggerContext)
                                 (.getLoggerList))]
                    (.setLevel logger ch.qos.logback.classic.Level/WARN))
                  (catch NullPointerException e)))
        (log/prl ::debug type name host port default-partition uri)
        (let [txr-process-f
                (when (and (:start? txr-props)
                           (not= type :mem))
                  #?(:clj (start-transactor! (kmap type host port) txr-props)))
              connect (fn [] (log/pr ::debug "Trying to connect with" uri)
                             (let [conn-f (do #?(:clj  (bdb/connect uri)
                                                 :cljs (bdb/connect host rest-port (:alias txr-props) name)))]
                               (log/pr ::debug "Connection successful.")
                               conn-f))
              create-db! (fn []
                           (log/pr ::debug "Creating database...")
                           #?(:clj  (bdb/create-database uri)
                              :cljs (go (<? (bdb/create-database host rest-port (:alias txr-props) name))))
                           (log/pr ::debug "Done."))
              _          (when create? (create-db!))
              conn-f  (try
                        (try-times connection-retries 1000
                          (try (connect)
                            (catch #?(:clj Throwable :cljs js/Error) e
                              (log/pr :warn "Error while trying to connect:" e)
                              #?(:clj (when (and create-if-not-present?
                                                 (not create?)
                                                 (-> e .getMessage (containsv? #"Could not find .* in catalog")))
                                        (create-db!)))
                              (throw e))))
                        (catch #?(:clj Throwable :cljs js/Error) e
                          (log/pr :warn "Failed to connect:" e)
                          (throw e)))
              _ (log/pr :debug "Connected.")
              _ (reset! conn conn-f)
              ;_ (when schemas #?(:clj (init-schemas! conn-f schemas)))
              ]
        (when set-main-conn? (reset! conn* conn-f))
        (when set-main-part? (reset! part* default-partition))
        (log/pr ::debug "Datomic database initialized.")
        (c/merge ; TODO add txr-alias
          (c/assoc this :txr-process txr-process-f)
          (kmap type uri name host port create-if-not-present? default-partition conn)))))
    (stop [this]
      (when (and (atom? conn) (nnil? @conn))
        #?(:clj (bdb/release @conn))
        (swap! conn* #(if (identical? % @conn) nil %))
        (reset! conn nil))
      (when txr-process
        (res/stop! txr-process))
      #?(:clj
      (when (= type :mem)
        (when-not (bdb/delete-database uri)
          (log/pr :warn "Failed to delete in-memory database at" uri))))
      this))

(defn ->backend-db
  [{:keys [type name host port txr-alias create-if-not-present?
           default-partition]

    :as config}]

  (map->BackendDatabase
    (c/assoc config :uri  (atom nil)
                    :conn (atom nil))))

(res/register-component! ::backend ->backend-db [::log/log])

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
