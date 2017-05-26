(ns quantum.reactive.db
  "Melds re-frame and posh."
  (:require
    [quantum.db.datomic      :as db]
    [quantum.db.datomic.core :as dbc]
    [quantum.core.log        :as log]
    [quantum.core.core
      :refer [?deref]]
    [quantum.core.fn
      :refer [fnl]]
    [quantum.core.error      :as err
      :refer [->ex TODO catch-all]]
    [quantum.core.spec       :as s
      :refer [validate]])
#?(:cljs
  (:require-macros
    [reagent.ratom
      :refer [reaction]])))

(defn- do-when-global-mconn! [register-fn type k f]
  (let [watch (fn [_ _ _ conn]
                (when (and (dbc/mconn? conn) (-> conn meta (get type)))
                  (catch-all
                    (do (log/pr ::debug "Registering" type k)
                        (register-fn conn k f))
                    e (log/pr :warn "Failed to register" k e))))]
    (watch nil nil nil @db/conn*)
    (add-watch db/conn* (keyword (str (name type) ":" (name k))) watch)))

(defn register-sub!
  "Like re-frame's `reg-sub`."
  {:example
    `(register-sub! global-conn :titles
       (fn [db args]
         #_"Do custom thing, e.g. datom seek or filter, (db/with db)"))}
  ([k f] (do-when-global-mconn! register-sub! :subs k f))
  ([conn k f]
    (TODO)
    (validate conn dbc/mconn?)
    (swap! (-> conn meta :subs) assoc k nil #_"Not sure what goes here yet")))

#?(:cljs
(defn register-q!
  "Registers a query function.
   Essentially names a query and makes it cacheable."
  {:usage `(register-q! :titles
             (fn [k args]
               [:find  '?title
                :where ['_ :title '?title]]))}
  ([k f] (do-when-global-mconn! register-q! :subs k f))
  ([conn k f]
    (validate conn dbc/mconn?)
    (swap! (-> conn meta :subs) assoc k
      (fn this
        ([k args] (this k args nil))
        ([k args opts]
          (let [rx (db/rx-q (f k args) conn
                     (when-not (:no-cache? opts) {:cache :forever}))]
            (if-let [post (-> f meta :post)]
              (reaction (post @rx))
              rx))))))))

#?(:cljs ; TODO merge this with `register-q!`
(defn register-q!*
  [k f & [post]]
  (register-q! k (with-meta (fn [_ args] (apply f args)) {:post post}))))

#?(:cljs
(defn register-kq! [k & [post]]
  (register-q! k (with-meta (fn [_ _] (dbc/kq k :value)) {:post post}))))

#?(:cljs
(defn register-kqa! [k a & [post]]
  (register-q! (keyword (str (name k) ":" (name a)))
    (with-meta (fn [_ _] (dbc/kq k a)) {:post post}))))

#?(:cljs
(defn register-transformer!
  "Registers a transformer function on ->`conn` to ->`k`.
   This fn immutably transitions/transforms the database e.g. using (db/with ...)."
  {:usage `(register-transformer! :add-title
             (fn [db k [title]]
               (db/with db
                 [(db/conj db :db.part/test false {:title title})])))}
  ([k f] (do-when-global-mconn! register-transformer! :transformers k f))
  ([conn k f]
    ; TODO add in event log call
    (validate conn dbc/mconn?)
    (swap! (-> conn meta :transformers) assoc k f))))

#?(:cljs (defn register-txn-transformer! [k f] (register-transformer! k (dbc/db-with f))))

#?(:cljs
(defn sub
  "From the connection ->`conn`, retrieves the subscription function associated
   with the key ->`k` and calls it with ->`args` and ->`opts` as params.
   Kind of like Re-Frame's subscriptions."
  ([k          ] (sub k nil    ))
  ([k args     ] (sub k nil nil))
  ([k args opts]
    (validate @db/conn* dbc/mconn?)
    (sub @db/conn* k nil nil))
  ([conn k args opts]
    (log/pr ::debug "sub" k)
    (if-let [subscriber (-> conn meta :subs ?deref (get k))]
      (subscriber k args opts)
      (throw (->ex :no-subscription-found {:k k}))))))

(defn transform!*
  "Applies the pure transformer pointed to by ->`k` to the database, with
   ->`args` as params. Calls all listeners associated with the connection,
   and returns the transaction report.
   See also `datascript.core/transact!`"
  ([k args]
    (validate @db/conn* dbc/mconn?)
    (transform!* @db/conn* k args))
  ([conn k args]
    (log/pr ::debug "transform" k)
    (if-let [transformer (-> conn meta :transformers ?deref (get k))]
      (let [report (atom nil)]
        (swap! conn (fn [db] ; TODO take this out of the swap, because we need side effects to be run no more than once
                      (let [r (transformer db k args)]
                        (log/pr ::debug "Pure transformer" k "applied")
                        (validate r (fnl instance? datascript.db.TxReport))
                        (reset! report r)
                        (:db-after r))))
        (log/pr ::debug "Transacted")
        (doseq [[_ callback] (-> conn meta :listeners deref)]
          (callback @report))
        @report)
      (throw (->ex :no-transformer-found {:k k})))))

#?(:cljs
(defn transform!
  ([k] (transform! k nil))
  ([k & args] (transform!* k args))))

