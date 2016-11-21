(ns ^{:doc "Convenience functions for creating a system and registering components
            according to Stuart Sierra's Component framework."}
  quantum.core.resources
           (:require [com.stuartsierra.component   :as component]
             #?(:clj [clojure.tools.namespace.repl :as repl
                       :refer [refresh refresh-all
                               set-refresh-dirs]                ])
                     [clojure.core.async           :as casync]
                     [quantum.core.error           :as err
                       :refer [->ex catch-all]]
                     [quantum.core.log             :as log      ]
                     [quantum.core.logic           :as logic
                       :refer [fn-not fn-or nnil?]]
                     [quantum.core.macros          :as macros
                       :refer [defnt]]
                     [quantum.core.async           :as async    ]
                     [quantum.core.type            :as type
                       :refer [atom?]                           ])
  #?(:clj  (:import org.openqa.selenium.WebDriver
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
  ([^clojure.core.async.impl.channels.ManyToManyChannel   obj] (throw (->ex :not-implemented)))))

#?(:cljs (declare open?))

(def closed? (fn-not open?))

#?(:clj
(defnt close!
  #?@(:clj
 [([#{Writer Reader}     obj] (.close obj))
  ([^quantum.core.data.queue.LinkedBlockingQueue obj] (async/close! obj))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel   obj] (casync/close! obj))
  ([                     obj]
    (when (nnil? obj) (throw (->ex :not-implemented))))))

#?(:cljs (declare close!))

#?(:clj
(defnt closeable?
  ([^java.io.Closeable x] true)
  ([x] false)))

(defnt cleanup!
  #?@(:clj
 [([^org.openqa.selenium.WebDriver x] (.quit  x))
  ([^java.io.Closeable             x] (.close x))
  ([^Lifecycle                     x] (component/stop x))]
   :cljs
 [([x] (if (satisfies? component/Lifecycle x)
           (component/stop x)
           (throw (->ex nil "Cleanup not implemented for type" {:type (type x)}))))]))

(defn with-cleanup [obj cleanup-seq]
  (swap! cleanup-seq conj #(close! obj))
  obj)

(defonce systems (atom nil)) ; TODO cache

(defn start!
  ([] (start! (:global @systems)))
  ([c] (component/start c)))

(defn stop!
  ([] (stop! (:global @systems)))
  ([c] (component/stop c)))

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

; ======= SYSTEM ========

#?(:clj (ns-unmap (ns-name *ns*) 'System))

(defn- start-with-pred
  "Starts a component and associates a started? key if it
   successfully starts."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (-> component start! (assoc :started? true)))

(defn- stop-with-pred
  "Stops a component and dissasociates a started? key if it
   successfully stops."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (-> component stop! (assoc :started? false)))

(defn started?
  "Determines if the component is in a started state."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (boolean (:started? component)))

(defn stopped?
  "Determines if the given component is in a stopped state."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  [component]
  (not (started? component)))

(defn start-system!
  "Recursively starts components in the system, in dependency order,
  assoc'ing in their dependencies along the way. component-keys is a
  collection of keys (order doesn't matter) in the system specifying
  the components to start, defaults to all keys in the system. If
  an exception is thrown, it'll tear down the system and ensure any
  components that were started are stopped."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  ([system]
     (start-system! system (keys system)))
  ([system component-keys]
   (try
     (component/update-system system component-keys start-with-pred)
     (catch #?(:clj Throwable :cljs :default) e
       (let [thrown-system (-> e ex-data :system)
             started-keys (->> thrown-system
                               (filter (fn [[k v]] (started? v)))
                               (keys))]
         (component/update-system-reverse thrown-system started-keys stop-with-pred))
       (throw e)))))


(defn stop-system!
  "Recursively stops components in the system, in reverse dependency
  order. component-keys is a collection of keys (order doesn't matter)
  in the system specifying the components to stop, defaults to all
  keys in the system."
  {:from "https://github.com/stuartsierra/component/pull/49/"}
  ([system]
   (stop-system! system (keys system)))
  ([system component-keys]
   (component/update-system-reverse system component-keys stop-with-pred)))

(defprotocol ISystem
  (init!   [this])
  (go!     [this])
  (reload! [this] [this ns-]))

(defrecord System [config sys-map make-system running?]
  component/Lifecycle
    (start [this]
      (if @running?
          (log/pr :warn "System already running.")
          (try (log/pr :user "======== STARTING SYSTEM ========")
               (let [sys-map' (start-system! @sys-map)]
                 (reset! sys-map sys-map')) ; TODO fix this to be immutable
               (reset! running? true) ; TODO fix this to be immutable
               (log/pr :user "System started.")
            (catch #?(:clj Throwable :cljs :default) t
              (log/pr :warn "System failed to start:" t #?(:cljs {:stack (.-stack t)}))))))
    (stop [this]
      (if (and @sys-map @running?)
          (let [_ (log/pr :user "======== STOPPING SYSTEM ========")
                sys-map' (stop-system! @sys-map)]
            (reset! sys-map sys-map')
            (reset! running? false)
            (log/pr :user "System stopped."))
          (log/pr :warn "System cannot be stopped; system is not running.")))
  ISystem
    (init! [this]
      (if @sys-map
          (log/pr :warn "System already created.")
          (let [sys-map' (make-system config)]
            (reset! sys-map sys-map')
            (log/pr :user "System created."))))
    (go! [this]
      (init!  this)
      (start! this))
    (reload! [this]
      (reload! this nil))
    (reload! [this ns-]
      (stop! this)
      #?(:clj (when ns-
                (refresh :after ns-)))
      (go! this)))

(defn ->system
  "Constructor for |System|."
  [config make-system]
  (assert ((fn-or atom? map?) config) #{config})
  (assert (fn? make-system) #{make-system})

  (System. config (atom nil) make-system (atom false)))

(defn register-system!
  "Registers a system with the global system registry."
  [ident config make-system]
  (swap! systems assoc ident (->system config make-system)))


#?(:clj
(defn reload-namespaces
  "Reloads all of the given namespaces."
  [namespaces]
  (doseq [ns-to-load namespaces]
    (require :reload (symbol ns-to-load)))))
