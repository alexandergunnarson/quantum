(ns ^{:doc "Convenience functions for creating a system and registering components
            according to Stuart Sierra's Component framework."}
  quantum.core.resources
  (:require
    [com.stuartsierra.component   :as comp]
#?(:clj
    [clojure.tools.namespace.repl :as repl
      :refer [refresh refresh-all
              set-refresh-dirs]                ])
    [clojure.core.async           :as casync]
    [quantum.core.cache           :as cache
      :refer [callable-times]]
    [quantum.core.core            :as qcore]
    [quantum.core.data.set        :as set]
    [quantum.core.error           :as err
      :refer [>ex-info catch-all]]
    [quantum.core.log             :as log      ]
    [quantum.core.fn
      :refer [fn1 fnl with-do fn-> <-]]
    [quantum.core.logic           :as logic
      :refer [whenf whenf1 fn-not fn-or whenp->]]
    [quantum.core.macros          :as macros
      :refer [defnt]]
    [quantum.core.async           :as async]
    [quantum.core.type            :as type
      :refer [atom? val?]]
    [quantum.core.spec            :as s
      :refer [validate]])
#?(:cljs
  (:require-macros
    [quantum.core.resources :as self]))
#?(:clj
  (:import
    org.openqa.selenium.WebDriver
    (java.lang ProcessBuilder Process StringBuffer)
    (java.io InputStream Reader Writer
      IOException)
    com.stuartsierra.component.Lifecycle
    (java.util.concurrent TimeUnit)
    ;quantum.core.data.queue.LinkedBlockingQueue
    clojure.core.async.impl.channels.ManyToManyChannel)))

#?(:clj
(defnt open?
#?(:clj
  ([^java.io.InputStream stream]
    (try (.available stream) true
      (catch IOException _ false)))
  ([^quantum.core.data.queue.LinkedBlockingQueue obj] (async/closed? obj)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel   obj] (throw (>ex-info :not-implemented)))))

#?(:cljs (declare open?))

(def closed? (fn-not open?))

#?(:clj
(defnt close!
  #?@(:clj
 [([#{Writer Reader}     obj] (.close obj))
  ([^quantum.core.data.queue.LinkedBlockingQueue obj] (async/close! obj))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel   obj] (casync/close! obj))
  ([                     obj]
    (when (val? obj) (throw (>ex-info :not-implemented))))))

#?(:cljs (declare close!))

#?(:clj
(defnt closeable?
  ([^java.io.Closeable x] true)
  ([x] false)))

(defnt cleanup!
  #?@(:clj
 [([^org.openqa.selenium.WebDriver x] (.quit  x))
  ([^java.io.Closeable             x] (.close x))
  ([^Lifecycle                     x] (comp/stop x))]
   :cljs
 [([x] (if (satisfies? comp/Lifecycle x)
           (comp/stop x)
           (throw (>ex-info "Cleanup not implemented for type" {:type (type x)}))))]))

(defn with-cleanup [obj cleanup-seq]
  (swap! cleanup-seq conj #(close! obj))
  obj)

(defonce systems (atom nil)) ; TODO cache
(def global-kw ::global)

(defonce components qcore/registered-components) ; TODO cache

#?(:clj
(defmacro defcomponent
  "Creates a startable/stoppable component whose `start` and `stop`
   functions are dynamically redefinable."
  [name fields startf stopf]
  (let [assert-valid-lifecyle-fn
         #(assert (and (seq? %)
                       (-> % first vector?)
                       (-> % first count (= 1))) %)
        _ (assert-valid-lifecyle-fn startf)
        _ (assert-valid-lifecyle-fn stopf)
        gen-lifecyle-fn
          (fn [[params & body] suffix]
           (let [sym (symbol (str name suffix))]
             {:sym sym
              :fn  `(defn ~sym [{:keys ~fields :as this#}]
                      (let [~(first params) this#] ~@body))}))
        startf-genned (gen-lifecyle-fn startf ":__start")
        stopf-genned  (gen-lifecyle-fn stopf  ":__stop")]
   `(do ~(:fn startf-genned)
        ~(:fn stopf-genned)
        (defrecord ~name ~fields
          comp/Lifecycle (start [this#] (~(:sym startf-genned) this#))
                         (stop  [this#] (~(:sym stopf-genned ) this#)))))))

(defn register-component! [k constructor & [deps]]
  (validate k           qualified-keyword?
            constructor (s/or* fn? (s/and var? (fn-> deref fn?)))
            deps        (s/or* nil? (s/coll-of keyword? :distinct true)))
  (when (contains? @components k) (log/pr :warn "Overwriting registered component" k))
  (swap! components assoc k (if deps (fn [config] (comp/using (constructor config) deps))
                                     constructor))
  true)

(defn start!
  ([] (start! (get @systems global-kw)))
  ([c] (comp/start c)))

(defn stop!
  ([] (stop! (get @systems global-kw)))
  ([c] (comp/stop c)))

(defn get-system
  ([] (get-system global-kw))
  ([k] (:sys-map (get @systems k))))

(defn update-system!
  {:usage `(res/update-system! (fn1 res/restart-components! [::log/log]))}
  ([f] (update-system! global-kw f))
                        ;; callable once because of `swap!` possibly running multiple times
  ([k f] (swap! systems (callable-times 1 (fn1 update-in [k :sys-map] f)))))

#?(:clj
(defmacro with-resources
  [bindings & body]
  `(let ~bindings
     (try
       ~@body
       (finally
         ; Release resources in reverse order of acquisition
         (doseq [resource# ~(->> bindings (partition-all 2) (map first) reverse vec)]
           (catch-all (cleanup! resource#) e#
             (log/ppr :warn "Failed attempting to release resource" {:resource resource# :error e#}))))))))

#?(:clj
(defmacro with-log-start! [level & body]
  `(let [level# ~level]
     (log/pr level# "Starting...")
     (with-do (do ~@body) (log/pr level# "Started.")))))

#?(:clj
(defmacro with-log-stop! [level & body]
  `(let [level# ~level]
     (log/pr level# "Stopping...")
     (with-do (do ~@body) (log/pr level# "Stopped.")))))

; ======= SYSTEM ========

#?(:clj (ns-unmap (ns-name *ns*) 'System))

(defn started?
  "Determines if `component` is in a started state."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (boolean (:started? component)))

(defn stopped?
  "Determines if `component` is in a stopped state."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (not (started? component)))

(defn start-with-pred!
  "Starts a component if not started, and associates a `:started?` key if it
   successfully starts."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (if (started? component)
      component
      (-> component start! (assoc :started? true))))

(defn transform-and-start-with-pred! [component-keys]
  (fn [k component]
    (if (started? component)
        component
        (let [xf (get component-keys k)]
          (-> component xf start! (assoc :started? true))))))

(defn stop-with-pred!
  "Stops a component if not stopped, and dissociates a `:started?` key if it
   successfully stops."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (if (stopped? component)
      component
      (-> component stop! (assoc :started? false))))

(defn transform-and-stop-with-pred! [component-keys]
  (fn [k component]
  (if (stopped? component)
      component
      (let [xf (get component-keys k)]
        (-> component xf stop! (assoc :started? false))))))

(defn descendant-connections
  [graph ks]
  (loop [ks'  (set ks)
         deps #{}]
    (let [k' (first ks')]
      (if (nil? k')
          deps
          (let [deps' (get graph k')]
            (recur (-> ks' (set/union deps') (disj k'))
                   (set/union deps deps')))))))

(defn update-components-and-connections!
  [system component-keys connection-k update-components? updatef]
  (updatef system
    (-> system
        (comp/dependency-graph (keys system))
        connection-k
        (descendant-connections component-keys)
        (whenp-> update-components? (set/union component-keys)))))

(defn- try-action
  "Like `component/try-action` but passes as the first argument to `f`
   the key of the component being started."
  [component system key f args]
  (catch-all (apply f key component args)
    e (throw (ex-info (str "Error in component " key
                           " in system " (com.stuartsierra.component.platform/type-name system)
                           " calling " f)
                      {:reason     ::comp/component-function-threw-exception
                       :function   f
                       :system-key key
                       :component  component
                       :system     system}
                      e))))

(defn update-system*
  "Like `component/update-system` but passes as the first argument to `f`
   the key of the component being started."
  [post-topo-sort-fn system component-keys f args]
  (let [graph (comp/dependency-graph system component-keys)]
    (reduce (fn [system key]
              (assoc system key
                     (-> (@#'comp/get-component system key)
                         (@#'comp/assoc-dependencies system)
                         (try-action system key f args))))
            system
            (post-topo-sort-fn
              (sort (com.stuartsierra.dependency/topo-comparator graph) component-keys)))))

(defn update-system [system component-keys f & args]
  (update-system* identity system component-keys f args))

(defn update-system-reverse [system component-keys f & args]
  (update-system* reverse system component-keys f args))

(defn start-components!
  "Recursively starts components in the system, in dependency order,
   assoc'ing in their dependencies along the way. component-keys is a
   collection of keys (order doesn't matter) in the system specifying
   the components to start.
   If an exception is thrown, it'll tear down the system and ensure any
   components that were started are stopped.

   If `component-keys` is a map, then it will assume that its keys map
   to functions which will update the respective components designated
   by those keys before starting them (if they are already stopped)."
  {:adapted-from "https://github.com/stuartsierra/component/pull/49/"}
  [system component-keys]
  (catch-all
    (if (map? component-keys)
        (update-system system (keys component-keys)
          (transform-and-start-with-pred! component-keys))
        (comp/update-system system component-keys start-with-pred!))
    e
    (let [thrown-system (-> e ex-data :system)
          started-keys  (->> thrown-system
                             (filter (fn [[k v]] (started? v)))
                             (keys))]
      (comp/update-system-reverse thrown-system started-keys stop-with-pred!)
      (throw e))))

(defn start-dependencies!
  "Starts the dependencies of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependencies false start-components!))

(defn start-components-and-dependencies!
  "Starts the components and dependencies of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependencies true start-components!))

(defn start-dependents!
  "Starts the dependencies of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependents false start-components!))

(defn start-components-and-dependents!
  "Starts the components and dependents of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependents true start-components!))

(defn start-system!
  "Runs `start-components!` on all dependencies in the
   system."
  {:adapted-from "https://github.com/stuartsierra/component/pull/49/"}
  ([system] (start-components! system (keys system))))

; ----- STOP ----- ;

(defn stop-components!
  "Recursively stops components in the system, in reverse dependency
   order. component-keys is a collection of keys (order doesn't matter)
   in the system specifying the components to stop."
  {:adapted-from "https://github.com/stuartsierra/component/pull/49/"}
  [system component-keys]
  (if (map? component-keys)
      (update-system-reverse system (keys component-keys)
        (transform-and-stop-with-pred! component-keys))
      (comp/update-system-reverse system component-keys stop-with-pred!)))

(defn stop-dependencies!
  "Stops the dependencies of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependencies false stop-components!))

(defn stop-components-and-dependencies!
  "Stops the components and dependencies of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependencies true stop-components!))

(defn stop-dependents!
  "Stops the dependencies of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependents false stop-components!))

(defn stop-components-and-dependents!
  "Stops the components and dependents of the provided component keys."
  [system component-keys]
  (update-components-and-connections! system component-keys
    :dependents true stop-components!))

(defn stop-system!
  "Runs `stop-components!` on all dependencies in the
   system."
  {:adapted-from "https://github.com/stuartsierra/component/pull/49/"}
  [system] (stop-components! system (keys system)))

; ----- RESTART ----- ;

(defn restart-components!
  "Recursively stops components in the system, in reverse dependency
   order, then starts components in the system, in dependency order.
   `component-keys` is a collection of keys (order doesn't matter)
   in the system specifying the components to restart."
  [system component-keys]
  (-> system
      (stop-components!  component-keys)
      (start-components! component-keys)))

(defn restart-dependencies!
  "Restarts the dependencies of the provided component keys."
  [system component-keys]
  (-> system
      (stop-dependencies!  component-keys)
      (start-dependencies! component-keys)))

(defn restart-components-and-dependencies!
  "Restarts the components and dependencies of the provided component keys."
  [system component-keys]
  (-> system
      (stop-components-and-dependencies!  component-keys)
      (start-components-and-dependencies! component-keys)))

(defn restart-dependents!
  "Restarts the dependencies of the provided component keys."
  [system component-keys]
  (-> system
      (stop-dependents!  component-keys)
      (start-dependents! component-keys)))

(defn restart-components-and-dependents!
  "Restarts the components and dependents of the provided component keys."
  [system component-keys]
  (-> system
      (stop-components-and-dependents!  component-keys)
      (start-components-and-dependents! component-keys)))

(defn restart-system!
  "Runs `restart-components!` on all dependencies in the
   system."
  [system] (restart-components! system (keys system)))

(defprotocol ISystem
  (reload! [this] [this ns-]))

(defrecord System [name config sys-map running?]
  comp/Lifecycle
    (start [this]
      (if running?
          (do (log/pr :warn "System" name "already running.")
              this)
          (try (log/pr :always "======== STARTING SYSTEM" name "========")
               (let [sys-map' (start-system! sys-map)]
                 (log/pr :always "System" name "started.")
                 (assoc this :sys-map sys-map' :running? true))
            (catch #?(:clj Throwable :cljs :default) t
              (log/ppr :warn (str "System " name " failed to start:") t)
              #?(:cljs (log/pr :warn #?(:cljs {:stack (.-stack t)})))
              this))))
    (stop [this]
      (if (and sys-map running?)
          (let [_ (log/pr :always "======== STOPPING SYSTEM" name "========")
                sys-map' (stop-system! sys-map)]
            (log/pr :always "System" name "stopped.")
            (assoc this :sys-map sys-map' :running? false))
          (do (log/pr :warn "System" name "cannot be stopped; system is not running.")
              this)))
  ISystem
    (reload! [this]
      (reload! this nil))
    (reload! [this ns-]
      (let [this' (stop! this)]
        #?(:clj (when ns- (refresh :after ns-)))
        (start! this'))))

(defn ->system
  "Constructor for |System|."
  [name config make-system]
  (validate name        qualified-keyword?
            config      map?
            make-system fn?)
  (System. name config (make-system config) false))

(defn default-make-system [config']
  (->> config' (map (fn [[k v]] [k (if-let [f (get @components k)] (f v) v)]))
       seq flatten
       (apply comp/system-map)))

(defn register-system!
  "Registers a system with the global system registry."
  ([k config]
    (register-system! k config default-make-system))
  ([k config make-system]
    (log/pr ::debug "Registering system...")
    (when (contains? @systems k) (log/pr :warn "Overwriting registered system" k))
    (with-do (swap! systems assoc k (->system k config make-system))
             (log/pr ::debug "Registered system."))))

(defn stop-registered-system! [system-kw] (swap! systems update system-kw (whenf1 val? stop!)))

(defn deregister-system! [system-kw] (swap! systems (fn-> (update system-kw (whenf1 val? stop!)) (dissoc system-kw))))

(defn default-main
  [system-kw re-register? config]
  (when-not (async/web-worker?)
    (let [system (get @systems system-kw)
          system (whenf system val? stop!)
          system (if (or re-register? (nil? system))
                     (get (register-system! system-kw config) system-kw)
                     system)
          system (swap! systems assoc system-kw (start! system))]
      system)))

#?(:clj
(defn reload-namespaces
  "Reloads all of the given namespaces."
  [namespaces]
  (doseq [ns-to-load namespaces]
    (require :reload (symbol ns-to-load)))))
