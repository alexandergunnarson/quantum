(ns quantum.db.datomic.sync)

RECONCILER DOC
"a reconciler which constantly
          pushes diffs from the EphemeralDatabase to the BackendDatabase
          and pulls new data from the BackendDatabase."

; TODO
Nested maps are expanded, so inner maps aren't just treated as single values in the DataScript db

; TODO pprint metadata?

#_(register-handler :this-transacts
  (fn [ds-db [_ _]]
    (with ds-db [<txns>])))

(do "

Posh, Datsync, Datview, Catalysis->Datsys,
Datreactor (coordination of side-effects)

===== MERGING HISTORIES =====

Suppose you wanted to assert some structural features for some class of entities users might want to transact
You can use transaction functions in the central Datomic node to accomplish that
But as soon as you are merging histories arbitrarily, you have to know that that old transaction is still valid (under the validations) if you changed the log leading up to it
If you're passing everything through a central transactor, they're just new transactions
So there's less work

The merging of histories is the gist of the problem with consistency. Depending on your semantics you can leverage different commutativity guarantees to gain eventual consistency. This is what all CRDT research is basically about. I started with a git-like approach in replikativ, so we have Confluent DVCS, where you can see the whole history and have to explicitly resolve conflicts (determine a total order of events that happend concurrently [partial order]).
This is because I wanted to be able to serialize a datascript transaction log.
To really scale one needs to distinguish between operations which need strong consistency and operations which can commute. So I would introduce a set for transactions (additions to the db) and apply this as well. Since datascript has no durable index, I apply all transactions on loading time by applying them and then stream new changes into the running db: https://topiq.es/

With re-frame subscriptions, the relationship between the component, and the subscribed path is in code, or more correctly in the closed overs of the function. Why not just expose the subscription paths themselves? That's all Om.Next is really doing.
You need:
1) Single source of truth (one atom for the app, not 20)
2) Ways of optimizing data access from the server (not REST, but e.g. websockets)
3) GUI's that are projections of state (data dependencies) to UI components, so that when the state changes the UI updates.
3) The more you can remove dependencies between components, and define clear interactions between them, the better.

2) Expressing data dependencies as data, so that the system can optimize data access and storage
3) Frameworks for optimizing data storage access by combining identical queries.



The main thing that om-next gives that we haven't really solved yet is that with om-next it's very easy to write a component and build its query method by composing all of its children's query methods
by composing queries this way, om.next can describe all data needed by the UI at any given moment by a single declarative query, so it only ever needs to query once: you can fetch all the data the current page needs in one server request, since you always have the full query available, which is good for performance.




A :keep-alive true (probably now :cache :forever) option to the various posh functions such that it doesn't kill the materialized view when dumped by a component.
That way for small collections of things (I'm thinking of the schema metadata in particular) that you know aren't going to blow up the app with, you can just make sure that everything is always available. We should keep the reaction in memory even when the rendered component that uses it tries to dispose it?




Oh, and the entire Ratom thing in Reagent is really a horrible hack. Look at how they register updates against atoms...it's just the wrong way to do it. Here's how it works a) when you de-ref a ratom inside a render function it is registered against the component who's renderer is currently running. Then when that ratom changes the renderer is re-triggered to update. What could go wrong?
Well let's say you have two ratoms, and only deref one? Well sucks to be you, cause only the one was registered, so when the other ratom changes your app won't re-render. What happens if you deref a ratom outside of a render...welp that won't link any components either. So better make sure you don't pre-calculate something outside of a render. It's a cute model, but horribly broken, IMO.



===== TIME TRAVEL =====

Just keep a history atom which tracks a set of states in order
You can do gc on that super easily, and it shouldn't take up a ton of mem cause of persistence

Yeah, I've done that before, but I guess what I'm saying is you can't do efficient time-based querying on DataScript

===== RE-FRAME =====

Makes the intention clear (by name)
and introduces a level of intermediation for increased flexibility/reuse (in case same subscriber logic is shared between multiple components) etc.


a) The client transacts a change to DataScript. DataScript doesn't have built-in history, so there will have to be a listener for transaction history which conjes the transaction history onto an atom.
b) The client validates the transaction according to a DataScript schema which should be isomorphic to the server-side Datomic one (DataScript doesn't do schema-based validation, but Datomic requires it). It also makes any DataScript->Datomic conversion as necessary. The biggest issue here is maintaining valid entity-references and coordinating those between DS and Datomic. E.g. DS entity 1 matches up with Datomic's entity 300382833, etc.
b) On each transaction, client's history-listener pushes the validated change via e.g. a Sente websocket to the server.
c) Meanwhile, a server-side go-loop has been started which listens on e.g. a Sente websocket for transaction data.
d) The server ensures that the client is privileged to make the transaction (e.g. there might be protected or secure schemas that the client shouldn't be allowed to change) and then either transacts or rejects the change. The client would have to handle a rejection accordingly.
e) Meanwhile, another server-side go-loop has been started which listens to a Datomic Connection's tx-report for transactions which 'registered' clients are interested in. This is where a Clojure version of Posh comes in: you don't want to notify every client on every transaction — just the client-relevant ones. The server pushes these relevant transactions to the client.
f) The client transforms the pushed Datomic transactions into DataScript-valid ones (see also part b)) and transacts them.

when a :db.fn/call is referenced in a Posh reactive query, Posh is forced to push updates to the requestor (subscriber) of that query on every transaction.

A while back we talked about having posh take a :remote true argument, which would trigger a remote subscription to some query, similarly to om-next.

===== TRANSLATION =====

`normalize-tx` is probably returning a vector with a couple of nils at the end, because it assumes that all tx-forms are either a :db/add, :db/retract or a map/entity/pull form. We need to be able to unfold things like this, because we need to be able to translate the eids between DS and Datomic.
Annotate your posh reactions such that they're exported to the server and instruct this scoping

===== COMM =====

But if you wanted to batch transactions, you could reduce with d/with, collect the transaction reports, and create a single transaction out of the datoms created

===== ENT IDS =====

as soon as you are filtering, you dont have all the ids anymore, and you can't assert a new unique integer id without knowing what's available
Of course, the easy fix for all of this is to just use GUIDs
(Ideally, datomics monotonic guids)
My gut has been telling me that's the ticket for simplifying P2P


===== AUTH =====

security filters

There is a way to get a set of all entity ids affected by any transaction (to address validation/security concerns)
That's roughly the plan for auth
For central DB write authorization you'd get the tx-report from (d/with (d/db datomic-conn) client-tx-data), and that will have :tx-datoms you can inspect for all affected eids.
You can also see which attributes are affected as well
So you could add permissions at the attribute level easily as well

Introduce filters that ensure that when you send tx datoms to a client, they have permissions to see those datoms (A post-query filter on the tx-report datoms. Not all datoms in db.)
filter by |allowed-to-see?| or some such predicate, which in turn is based off of eid, value, or attribute
Plug into the tx-report flow, grab the datoms, and send them along some pipeline that processes and sends those datoms out to whomever needs them, based on permissions filters and scope filters (what do I need to render the page for the route client is on)
You could do transaction functions for auth, but then you'd have to run every single client transaction through such a tx-function, which could be prohibitively expensive, and introduce a bottleneck at the transactor

Re: geo-limiting, I'm thinking my client would send its lon-lat when connecting to the host. And then the host would only send txs for entities that are within a threshold of each client
(ie all entities have lon-lat coords)

Datomic transaction metadata makes for a lot of really interesting authorization patterns.
What you do is you put a stamp on every transaction that points to what user has access to that transaction

===== OPTIMISTIC UPDATES =====

Offline availability implies optimistic updates.
Behavior for optimistic updates with a timeout becomes a super straightforward configuration problem.
Dealing with conflicts is most certainly where all the thought goes in this.

Automatically feed DS transaction changes to the server, and maintain transaction metadata on the client sufficient for determining which server transactions have been successful and which need to be reverted.

Automated GC of DS data that's presently 'out of scope'.
(You can use Reagent's :componentDidUnmount and :componentDidMount etc. to do that, right? Whenever it unmounts, transactions matching the pattern originally specified in the Posh reactive query-expression are excised/dropped from DS and the subscription is canceled. I don't recall whether Posh handles that already, simply by virtue of the fact that the reactive expression will not be called again once it goes out of scope, and thus no additional information will be requested.

    I was thinking you could somehow use the on-set and on-dispose callbacks that are in Reagent's reaction code: https://github.com/reagent-project/reagent/blob/6e8a73cba3a0fb13d3cf6dc38168e889bef9d337/src/reagent/ratom.cljs#L472-L477

If on-dispose works like I think it does, it gets called when a component stops rendering, so we could use that to GC and tell the server not to watch that query any longer.)

I wish I could just write all the optimisic update stuff in time based datalog queries...
But I can't, because of lack of DS time-based queries

")


; ========== SERVER ==========

;; ## Applying changes from clients

;; ## Pushing changes to client

;; Every time we get a transaction, we want to send the results of that transaction to every client.
;; Eventually we can get fancy with installing subscription middleware, so for each client we have a
;; specification of what they have "checked out", but this is just a starting point.

;; The easiest thing to do here is take the tx-data (datoms) produced by each transaction and convert those to
;; :db/add and :db/retract statements which we can send to clients.

(defn tx-report-deltas
  [{:as tx-report :keys [db-after tx-data]}]
  (->> tx-data
       (d/q '[:find ?e ?aname ?v ?t ?added
              :in $ [[?e ?a ?v ?t ?added]]
              :where [?a :db/ident ?aname]]
           db-after)
       (map (fn [[e a v t b]] [({true :db/add false :db/retract} b) e a v]))))

(defn start-transaction-listener!
  [datomic-report-queue handler-function]
  (future
    (loop []
      ;; TODO Assumes queue is a java blocking queue; should dispatch to work with a core.async tap/chan for multiple queue consumers
      ;; Where should we try catch if at all?
      (let [tx-report (.take datomic-report-queue)]
        ;; Will our handler ever need the full tx-report?
        (handler-function (tx-report-deltas tx-report))
        (recur)))))

;; ## Transaction normalization
;; ----------------------------

;; We need some ability to normalize our transactions so they make sense in DataScript and are fully queryable.

;; ## Extracting schema
;; --------------------

;; We would like to be able to automatically extract the relevant schema datoms and translate them into a
;; DataScript compatible schema representation to get what aspects of the schema are supported.
;; However, we'll still keep this data as datoms as well, for queryability.

(def base-schema
  "The base datsync schema"
  ;; So we can use idents more effectively
  {:dat.sync.tx/origin {:db/doc "The origin of the transaction. Should be either :dat.sync.tx.origin/remote or .../local"}
   :dat.sync.remote.db/id {:db/unique :db.unique/identity
                           :db/doc "The eid of the entity on the remote."}
   :dat.sync/diff {:db/valueType :db.type/ref
                   :db/doc "An entity that represents all of the persisted changes to an entity that have not been confirmed yet."}
   ;; Navigation on the client; I guess the server may need to know this as well for it's scope... Maybe
   ;; redundant...
   :dat.sync/route {}})

;; ## Transaction metadata
;; -----------------------

;; We'll be using first class transaction metadata to track what transactions need to be validated on the
;; server, which have been validated, which need to be retracted, and which should remain local without
;; sending to the server. At least eventually... nothing like that yet.

;; One way to speed things up would be to not try and keep ids the same
;; Would make a lot easier.

;; So... if we want local schema, we have to decide whether or not we want it to be queryable.
;; However, for queryability and consistency, this requires that instead of using keywords like
;; :db.cardinality/one and :db.type/ref, we use the entity ids representing those idents :-/
;; For now this is probably fine, but we'll eventually want to add some functionality that would perhaps let
;; us translate from the idents we know and love automatically to entity stubs which get filled in from the
;; datomic db.
;; A problem with this currently is that we're making some strong assumptions about the way that schema
;; related attributes are handled in transactions (specifically with refs; see comments above).
;; These problems will need to be solved in order for us to make things nicer here.

;; ## Sending data back
;; --------------------

;; Note that this doesn't currently deal well with (for example) having a tx entry where the eid is an ident,
;; because of what seems to be a bug in DataScript get-else. So you have to do that translation yourself to
;; get the local eid if you want to use this function.

;; This should be reworked in terms of multimethods, so you can create translations for your own tx functions,

;; Now for our P2P/offline available/local first translation function
;; This will operate by translating eids from the datoms of a tx-report (tx-data) into lookup references, or into tempids
;; where app approapriate, based on the novelty relative to the db argument.

;; Relevant differences between Datomic/DataScript:
;; * Datomic needs a :db/id in transactions, but DataScript does not (both still resolve entities given identities though)
;; * We have to query for attr type differently in the two cases
;; * We can still query by attr-ident passed in as an arg in both cases
;; * Pulling :db/id on a nonexistent db id gives different results in datascript than it does in datomic
;;   * (d/pull [:db/id :db/ident] x) -> {:db/id x}
;;   * (d/pull [:db/ident] x) -> nil
;;   * (ds/pull [:db/id :db/ident] x) -> nil
;; * In DS You can transact a refid that isn't in any other datoms; will pull out, however not in Datomic
;; * You can call first on #datascript/Datoms but not datomic #datoms

;; Have to be careful changing the identifying attr of an entity; I think this necessitates transactionality
;; However, this is something that shouldn't really be frequently happening anyway... ids are ids

;; Note that the "fix" transaction in the docstring above needs to be a transaction function to avoid race conditions,
;; and the unlikely possibility of the round trip to Datomic taking longer than the local transaction (unlikely, but hey).
;; If that happened we would need to "merge" the entities (the local, and the one from Datomic with the identity)

;; ## Staging changes and commits
;; ------------------------------

;; We'll eventually need to build staging into the datsync application stack.
;; Say someone opens a form for an entity and starts editing, but hasn't explicitly confirmed their changes.
;; We'd like for the data to be instantly accessible to any other forms open for the same entity on other
;; secondaries (client datascript dbs).
;; However, we'd like these changes not to affect what others are doing.
;; Maybe we forgo this for now...


;; We need to build in transactions...
;; So we can query across them for most recently committed datoms



;; ## Distributed checksum
;; -----------------------

;; We'd like a simple method for ensuring we haven't missed any transactions, and that everything is
;; consistent.
;; I think we can do this with a simple hashing function that takes some key k (perhaps the client id)
;; and computes k' = (hash [k :hash]).
;; The next member of the checksum sequence would be (hash [k' :hash]), etc.
;; This gives us a sequence of hash values which should let us simply ensure consistency.



;; ## Putting it all together?
;; ---------------------------

;; We put this all together with a few handler registrations so dat.reactor will do the right
;; thing, if being used in your system.

;; Sidenote: Should be careful about :db/retract above (assumes all :db/add)

; Handlers to:
; ::apply-schema-changes
; ::merge-schema
; ::recv-remote-tx
; ::send-remote-tx
; :dat.remote/connected





; Essentially what happens is this:
; Client subscribes to server (source-of-truth) changes
; When client has new data, it pushes it to server
; Server will push this data to clients subscribed to / interested in that data
; Subscriptions will be handled à la posh, via transaction parsing
; Need some way to reconcile DataScript eids + 'idents' in CLJS with Datomic eids + idents
; Need some way to explicitly mark certain CLJS database data as e.g. :local

(ns your-app
  (:require [dat.sync.client]
            [dat.remote]
            [datascript.core :as d]
            [dat.reactor.dispatcher :as dispatcher]))

(defn new-system []
  (-> (component/system-map
        ;;          ;; A sente remote implementation handles client/server comms
        ::conn/conn nil #_(dat.remote.impl.sente/new-sente-remote)
        ;;          :: Dispatcher accepts messages from wherever (pluggable event streams?) and presents them to the reactor
        :dispatcher (dispatcher/new-strictly-ordered-dispatcher)
        ;;          ;; The reactor is what orchestrates the event processing and handles side effects
        :reactor    (component/using
                      ;; the reactor needs an :app attribute which points to the DataScript :conn
                      (reactor/new-simple-reactor {:app {:conn conn}})
                      [:remote :dispatcher])
        ;;          ;; The Datsync component pipes data from the remote in to the reactor for processing, and registers
        ;;          ;; default handlers on the reactor for this data
        :datsync    (component/using
                      (dat.sync.client/new-datsync)
                      [:remote :dispatcher]))))




Additionally, you'll need to pass the `:remote` and the `:dispatcher` components along to a `:reactor` component that actually reduces over the DataScript conn.
A `:reactor` implementation is available in [datreactor](https://github.com/metasoarous/datreactor) as `dat.reactor/SimpleReactor`.
The Datreactor project also has a couple of `:dispatcher` implementations, in particular `dat.reactor.dispatcher/StrictlyOrderedDispatcher`.


You may also specify `:app` in this system as a system component.
This is how [Datview](https://github.com/metasoarous/datview) works.
If you're using Datview, we recommend you look at the Datsys project for how that's set up.
And even if you're not, you may want to take a look at how it handles routes, history, etc.

Because all of these pieces are designed and build around protocols and specs, the semantics of how you'd
swap-out/customize system components are described in Datspec's [`dat.spec.protocol` namespace](https://github.com/metasoarous/datspec/blob/master/src/dat/spec/protocols.cljc).
That's also the best place to go for understanding the Datsys architectural vision.

#### What's going on here?

Behind the scenes, Datsync hooks things up so that messages coming from the server with event id `:dat.sync.client/recv-remote-tx` get transacted into our local database subject to the following conditions:


* Every entity gets a `:datsync.remote.db/id` mapping to the Datomic id
* Local ids try to match remote ids when possible
* A `:dat.sync.client/bootstrap` message is sent to the server to initiate the initial data payload from the server
* Any entity specified in the transaction which has a `:db/ident` attribute will be treated as part of the
  schema for the local `conn`'s db, and `assoc`'d into the db's `:schema` in a DataScript compatible manner
    * Any `:db/valueType` other than `:db.type/ref` will be removed, since DataScript errors on anything other
      than `:db.type/ref`
    * This operation updates the db indices by creating a new database with the new schema, and moving all the
      datoms over into it.
    * Schema entities are included as datoms in the db, not just as `:schema`
        * Keep in mind that idents aren't supported in DataScript, so to look up attribute entities, use attribute references
        * This can facilitate really powerful UI patterns based on schema metadata which direct the composition
          of UI components (in an overrideable fashion); have some WIP on this I might put in another lib eventually
    * We can also apply schema changes to an existing database using the `dat.sync.client/update-schema!` function, or by
      dispatching a `:dat.sync.client/apply-schema-changes` event.

Additionally, there is a `:dat.sync.client/send-remote-tx` event handler that takes transactions from the client and submits them to the server.
This function:

* Translates eids via `dat.sync.client/datomic-tx`
* Send a message to server over the remote as `[:dat.sync.remote/tx translated-tx]`

Handlers for the following messages are also set up:

* `[:dat.sync.client/apply-schema-changes schema-tx]` - Applies schema transaction to DataScript db conn
* `[:dat.sync.client/merge-schema schema-map]` - Merges the schema-map (DataScript style) into the DataScript db conn
* `[:dat.sync.client/bootstrap tx-data]` - Takes a bootstrap response and more or less delegates to `:dat.sync.client/recv-remote-tx`
* `[:dat.sync.client/request-bootstrap _]` - Initiates a remote request for the boostrap message (this is triggered automatically when the Datsync component fires up; you'll have to handle this on server)



{::db/db {...}}

(fn subscribe1 [db]
  (q db [:this :that/other]))

(fn publish1 [db]
  (with db (assoc  [:a 1] :b 4)
           (update [:b 2] :fn/myfn :e 5)))






(defn translate
  ; dat.sync.client/datomic-tx
  "Translates eids between Datomic and DataScript."
  [from-type to-type txn]
  )

(defrecord Reconciler
  {:todo #{#?(:clj  "Push all relevant txns (and schemas) that haven't been applied on client"
  ; :dat.sync.client/request-bootstrap
  ; :dat.sync.client/bootstrap
  ; get-bootstrap
              :cljs "Push all relevant txns (and schemas) that haven't been applied on server")}}
  [terminated?]
  component/Lifecycle
  (start [this]

    #?(:clj  (go (while-not @terminated?
                   (let [txns (<! txn-queue)]
                     (doseq [uids subscribed-uids] ; subscription middleware: for each client we have a specification of what they have "checked out"
                       (>! (get socket uid) (restrict :read uid txn-deltas))))))
       :cljs (add-watch conn ::reconciler
               (fn [txn]
                 (->> txn (translate :client :server) (put! socket)) ; TODO need to do an ordered `put!`
                 )))
    this)
  (stop [this]
    #?(:clj  (reset! terminated? true)
       :cljs (remove-watch conn ::reconciler
               (fn [])))
    this))

 ; listeners on this? IDK. Look at Posh

(defn transact-local!
  "Transacts data that shouldn't be published to the server. This includes purely local state
   (e.g. UI state). Transient transactions could simply be marked not to be synced with external
   nodes.

   Some things only make sense on a client: who the current user is, which chat room is selected
   — session-dependent data."
  [])

#?(:cljs
(defn transact-from-server!
  "Updates the DataScript schema when transactions are recieved for anything with a `:db/ident`
   attribute (should maybe make this based on the install attribute, like in Datomic, but for
   now...).
   Ensures that transactions are marked as shouldn't be propagated server (or else there will be an infinite loop)."
  [txn]
  ))

#?(:clj
(defn restrict
  "Scope restriction

   Access control filters (read/write authorization security)

   Read: Assume that each client queries a coarse grained materialised view of the DB, which only
   exposes data that the client has access rights to. The pipeline feeding data to the client
   can have additional data/security filters for more fine grained control.

   As an example, say we have a Repo Manager app like Github. Certain users might not have access
   to private repos or only access to a limited set of \"data views\" within a given Project or
   Repo. Most of this filtering could be done server-side.

   Write: E.g. a user can't modify another user's password.

   View scope filters (checked-out/looking-at data)"
  [type uid txn-deltas]
  (validate type #{:read :write})

  (case type
    :read  (->> txn-deltas (db/filter (:read  (security-restriction-preds-for uid))))
    :write (->> txn-deltas (filter    (:write (security-restriction-preds-for uid)))))))


; dat.sync.server/apply-remote-tx!
; dat.sync/start-transaction-listener!
; (dat.sync/start-transaction-listener! (d/tx-report-queue datomic-conn) handle-transaction-report!)
; use web workers to download datoms in background
"This function currently takes the Java blocking queue returned by `d/tx-report-queue` and consumes all changes placed on that queue.
Pass in a `core.async` channel so you can pull messages off Datomic's single `tx-report-queue` and mult them out to various processes that needed these notifications as well."
(defmethod receive :db/txn [txn uid]
  ; Assumes server is not compromised
  ; Client receives data from the server as transactions.
  #?(:clj  (->> txn (translate :client :server) (restrict :write uid) transact-async!)
     :cljs (->> txn (translate :server :client) transact-from-server!))
  #_(dat.sync/apply-remote-tx! conn tx-data))

; HTML5 History
; https://github.com/metasoarous/datview/blob/master/src/dat/view/router.cljs
; https://github.com/metasoarous/datview/blob/master/src/dat/view/routes.cljc






"Client GC

How do we efficiently implement 'Garbage collection' on the client? Can we mimic the least-recently used policy for discarding data used by Datomic on the server?

Since we can track the application going through various view states, we can also track which facts are being displayed. We could then keep an index to those facts and track which are the least recently used and can be deleted from the client cache (ie DataScript DB).
"








"Batch sync

Batch sync has the objective try to get all relevant facts since the last time the client was connected to the server.

A batch sync is initiated by the client in order to get the full state needed for a scoped view. This is needed on initial rendering of main data pages and on each top level view/scope change. Delta sync can then work on more granular view changes or when the view is static.

For this reason we can use the 'time machine' features of Datomic, and simply transfer facts since the last time the client was synced. This timestamp can be persisted on the client.

For a database that has millions of facts, we can't simply do a snapshot since a given time, since even the first transaction may still be relevant (not retracted). In theory we could compact the transaction log, to only keep the latest assertion of each entity, but the log might still be way too large to replicate in full.

In the Datascript sample app acha-acha, the complete transaction history of of ~14k facts are copied to each client in ~150ms. We could use this simple approach (with optional security policy?) for up to 20k facts, then switch to alternative batch strategy beyond that.

We could e.g. batch transfer per entity model, such as filtering/copying all Projects, all Todos etc. Maybe paging.

Doing this batch sync via a WebWorker would result in a much better user experience (no GUI thread interference!)
"

"Component queries/subscriptions (Posh reactive Datalog queries)

We need to send declarative Component Query Params (CQP) for each component currently mounted in the UI. The infrastructure for component queries could likely be reused on client and server.

We need to leverage the React lifecycle method such as componentDidMount and componentWillUnmount to register/unregister the component from auto-sync.


We should figure out a way to determine whether two queries are equivalent.
Also query planning.


Cleaning up server subscriptions:

Can be achieved by communicating component Query data needs from client If server doesn't receive any message from client within some timeout window, such as 60 secs, we could/should (perhaps?) clean up subscriber state for that client.

It is interesting whether the server should keep a cache of most relevant component (materialized) views to use for most clients, to be updated periodically or on specific transaction triggers so that each incoming request won't have to require a new server query which would quickly downgrade performance for large scale environments.

Each mount/unmount or change must publish on a channel which are merged, as these typically happen in a cascade (tree structure of components). The server must then keep track of which clients have which components currently mounted and how to (roughly) filter the data to fit their needs, in order not to send way more data than is required for that page!

Fine grained control can also be achieved by sending transient state in the form of query params (meta data), such as paging index etc.

For components that have multiple instances on the page, each must have a unique id, for which we can maintain such transient data.



"

"Optimistic client-side db updates and offline functionality"

"Merging history

3-way merge of node graphs. When we send the component-state query to the server, we send a logical time stamp (transaction version id). When we receive node graph data back, we can compare current client component state with server state and base state (from before we issued query, async snapshot cache entry). From these 3 graphs we can do a 3-way merge to set the client state, merging changes on both server (master) and client (local).

Note that this query approach should not be used as the main sync approach in most cases. It is likely most appropriate when doing top level scope change, such as top menu in app or going switch between user/admin mode etc. or when the app is bootstrapped. In any case, the client might already have data even on app bootstrap (such as preloaded from IndexDB store) in which case we need a merge (scope overlap).

3-way merge of graph structures

We can perform a 3-way merge efficiently O(n log(n))

For most real life scenarios, we expect 2-4 levels of nesting with no more than 10-50 data items for each merge, so it should be super fast! Most of the UI is static structure and styling, with only a few data items :)

Note: On any merge conflict, ask client to resolve (5 secs). Server changes wins on timeout or declined resolve.

Example: no conflicts
-----------------------------------
Base:   [A [B [x y] C]]
Server: [A [Z [d y] C]]
Client: [A [B [x a] C]]

Compare A, A, A: A : [A]
Compare B, Z, B -> Z : [A [Z [x y]] ;; will overwrite change B.a from client
Compare C, C, C -> d : [A [Z [d] a] C]

Result: [A [Z [d] a] C]
---

Example: Mixed changes (conflicts)
-----------------------------------
Base:   [A [B [x y] C]]
Server: [A [Z [-x y] C]]
Client: [A [-B D]]

Compare A, A, A: A : [A]
Compare B, Z, -B  -> Z  : [A [Z]] *1*
Compare x, -x     -> -x : [A [Z []]]
Compare y, y      -> a  : [A [Z [y]]]
Compare y, y      -> a  : [A [Z [y]]]
Compare C, C, D   -> D  : [A [Z [y]] D]

*1* : App may request user to resolve conflict: delete (own) or update (server)?)
Server/add wins by default if not resolved by user.

Result: [A [Z [y]] D]
On the server we need a registry of queries per component type. Possibly we can reuse queries from client (as per posh). Requires that queries are maintained in a separate namespace in one file (conventions!)

(register-component-queries {
  :TodoList todoListQuery ;; username, id, page, itemsPerPage
  :TodoItem todoQuery ;; label, status
})
From this tutorial we know that we can add a full map (merged tree) to the DB, which will then be executed as a set of add operations.

... Each map a transaction contains is equivalent to a set of one or more add operations. The map must include a :db/id key identifying the entity data is being added to. It may include any number of attribute, value pairs.

{ :db/id entity-id
  attribute value
  attribute value
  ... }
Internally, the map structure gets transformed to the list structure. Each attribute, value pair becomes a :db/add list using the entity-id value associated with the :db/id key.

The map structure is supported as a convenience when adding data. As a further convenience, the attribute keys in the map may be either keywords or strings."




"Strong eventual consistency, enabling optimistic updates and offline updates on clients. There are many routes we could take here:



Transaction metadata and history would give us what I think is the best path forward for implementing eventual consistency.

We can implement a transaction metadata system on the client which enables us to track what changes have been confirmed by the server, and which haven't. Garbage collection could be built around this metadata: history is only retained for transactions which have been transacted on the server.

Keeping transaction metadata as datoms is actually not particularly challenging (it just requires using a customized version of the d/transact! function (for this reason, it would be nice if DataScript had some kind of DBRef protocol...)).

Keeping the history may be a bit more difficult. DataScript prevents saving history by defining eq and hash on datoms such that the tx id and the added bool are ignored.

It's not clear whether defining our own Datom type which did not have these properties would solve this problem, or whether there would be other work involved.


An approach to tracking the metadata, would be to do it externally to the DataScript DB, such as in LocalStorage in the form of an index or even a simple value with the transaction id of the last server committed transaction. In a normal DB, the index is also stored and maintained separate to the data since it provides a lot of benefits and ensures a clear separation.



Conflict resolution

Let whoever writes to the transactor first win if there are transaction conflicts in our eventually consistent system. Ideally though, we'd have support for more nuanced control of the conflict resolution process, potentially even allowing user interaction as part of the resolution process.

Side note: transaction commit systems

This work also feeds into the possibility of transaction commit systems, where form views can persist 'WIP' changes between clients (allowing for collaborative editing), but don't show up in main views of data which has been assumed 'committed' or 'confirmed' somehow by the user.
"

"Confirmed and unconfirmed client dbs

Another approach towards solving this problem is to have two databases: one with only confirmed changes, and one with unconfirmed changes. If a transaction sent to the server fails, a client side handler could respond by rolling back to the last set of unconfirmed changes. While this may be a little easier to implement, the story here for conflict resolution and commit systems could be made more difficult. It's rather compelling and intriguing that building history and time into the databases so elegantly solves not only the main problem of attaining strong eventual consistency, but also these other related issues."





(defn synchronize-deltas
  "Automatically resolves merge conflicts.

   We can only approximate causality, and thus we need a way to merge these transaction sets in a way that is: idempotent, commutative and associative: whatever order you apply the add/remove operations in, you will (eventually) end up with a consistent state.

   Modern distributed databases such as Riak, Casandra etc. all use CRDTs to achieve this. A CRDT is an idempotent, commutative monoid — a joint Semi Lattice. What does this mean in practice?

   Idempotence:   (f A A) = A
   Commutativity: (f A B) = (f B A)
   Associativity: (f A (f B A)) = (f (f A B) C)

   See datsync wiki."
  [])

Concurrent adds    commute since each one is unique.
Concurrent removes commute because any common pairs have the same effect, and any disjoint pairs have independent effects.
Concurrent add(e) and remove(f) also commute:
  if e != f they are independent, and
  if e = f the remove has no effect.

2P Set (Two phase set), Can't add an already deleted element (Tombstone set). Has a tombstone that maintains deleted elements.

LWW element set, adds a (logical) Timestamp to each set. Last one wins. Graphs can be built from sets.

The best solution is likely to use some sort of combination of: OR, 2P and LWW sets, possibly with a logical (Lamport) timestamp. Experimentation and gradual improvement is needed here.

On the client, the server transactions should take precedence.

Transaction log synchronization

;; filters transaction log, multiple filters can be composed
;; filter-log(log: SyncLog) -> SyncLog

{transact: log,
 commit: log,
 cancel: log}

Each log is a simple list of `SyncLog`
Each socket groups together transactions by socket-id key {user-123: [log]} send back sync response to client: {socket-id: id, commit-log: log, cancel-log: log}

First filters log. Then asynchronously:

- send transations to Datomic that are approved to be committed
- notify clients of non-committed transactions (for info or intervention/playback)

The non committed transactions are typically those in the tombstone set.

The filters can be composed from a mix of standard and custom strategies, such as LWW (Last Write Wins) etc. Compare Entity/Attribute conflicts by looking at each transaction in the log.

Note also that we need similar merge stategies on the client, since a client can be offline for a while (between tube stations!) and concurrently update own transaction history to be merged with incoming updates from server!

(defn handle-transactions [transact-log]
  ;; `filter` is a composed filter function using `filter-log` internally
  ;; (ie. a pipeline)

  ;; `filtered` is a SyncLog (see above)

  (let [filtered (filter log)]
    (go
      (do-commit (:commit filtered)) ;; async
      (do-cancel (:cancel filtered)) ;; async
    )))

;; group transactions by `socket-id`
;; send each cancelled log subset to socket for that client
(defn do-cancel [log])
  (->
    (group-by :socket-id log)
    (send-cancelled-to-client))

;; commit each transaction to transaction log
(defn do-commit [log-item & rest])
  (doseq [x (join [log-item] rest)] (commit-transaction X)))

;; receives a list of cancelled transactions grouped by socket-id
;; should async send each such group to socket
(defn send-cancelled-to-client [client-log]
  ;; ... use core.async to split and handle each list item
  )


- Modern web apps can use Service Worker to act more like a native app
; in time even using a manifest file to install the app as an app on the device.
; In addition, a Service Worker can cached the app and allow it to run offline,
; and thus gain access to cached data from the Offline store and syncing when online.
- The quotas depend on the available storage of the browser device. Even small mobiles come with GBs of storage these days, only set to increase!
- Each application has a limitation to have 20% of the available TEMPORARY storage pool (i.e. 20% of 50% of available disk). (Not restricted to 5Mb anymore)
- When TEMPORARY storage quota is exceeded, all the data (incl. AppCache, IndexedDB, WebSQL, File System API) stored for oldest used origin gets deleted . [Note: since each app can only use up to 20% of the pool, this won’t happen very frequently unless the user has more than 5 active offline apps.]
If app tries to make a modification which will result in exceeding TEMPORARY storage quota (20% of pool), an error will be thrown.
Each application can query how much data is stored or how much more space is available for the app by calling queryUsageAndQuota() method of Quota API.
- https://www.html5rocks.com/en/tutorials/offline/quota-research/ (Working with quota on mobile browsers)
- https://developers.google.com/web/updates/2011/11/Quota-Management-API-Fast-Facts
"




