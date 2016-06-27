(ns ^{:doc "Convenience functions for creating a system and registering components
            according to Stuart Sierra's Component framework."}
  quantum.core.resources
           (:require [com.stuartsierra.component   :as component]
             #?(:clj [clojure.tools.namespace.repl :as repl
                       :refer [refresh refresh-all
                               set-refresh-dirs]                ])
                     [#?(:clj  clojure.core.async
                         :cljs cljs.core.async   ) :as casync   ]
                     [quantum.core.error           :as err
                       :refer [->ex]                            ]
                     [quantum.core.log             :as log      ]
                     [quantum.core.logic           :as logic
                       :refer [#?@(:clj [fn-not fn-or]) nnil?]  ]
                     [quantum.core.macros          :as macros
                       :refer [#?@(:clj [defnt])]               ]
                     [quantum.core.thread.async    :as async    ]
                     [quantum.core.type            :as type          
                       :refer [atom?]                           ])
  #?(:cljs (:require-macros
                     [quantum.core.logic           :as logic
                       :refer [fn-not fn-or]                    ]
                     [quantum.core.macros          :as macros
                       :refer [defnt]                           ]
                     [quantum.core.log             :as log      ]))
  #?(:clj  (:import org.openqa.selenium.WebDriver
                    (java.lang ProcessBuilder Process StringBuffer)
                    (java.io InputStream Reader Writer
                      IOException)
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

#?(:clj
(defnt cleanup!
  #?@(:clj
 [([^org.openqa.selenium.WebDriver obj] (.quit  obj))
  ([^java.io.Closeable             obj] (.close obj))])))

(defn with-cleanup [obj cleanup-seq]
  (swap! cleanup-seq conj #(close! obj))
  obj)

(def start! component/start)
(def stop!  component/stop )

#?(:clj 
(defmacro with-resources
  [bindings & body]
  `(let ~bindings
     (try
       ~@body
       (finally
         (doseq [resource# ~(->> bindings (apply array-map) keys (into []))]
           (cleanup! resource#)))))))

; ======= SYSTEM ========

#?(:clj (ns-unmap (ns-name *ns*) 'System))

(defonce systems (atom nil))

(defprotocol ISystem
  (init!   [this])
  (go!     [this])
  (reload! [this] [this ns-]))

(defrecord System [config sys-map make-system running?]
  component/Lifecycle
    (start [this]
      (if @running?
          (log/pr :warn "System already running.")
          (let [[started? system']
                 (try [true (start! @sys-map)]
                   (catch Throwable t
                     (-> t ex-data :system stop!)
                     [false @sys-map]))]
            (if started?
                (do (reset! sys-map  system') ; TODO fix this to be immutable
                    (reset! running? true   ) ; TODO fix this to be immutable
                    (log/pr :user "System started."))
                (log/pr :user "System failed to start.")))))
    (stop [this]
      (if (and @sys-map @running?)
          (do (swap! sys-map
                (fn [s]
                  (log/pr :user "======== STOPPING SYSTEM ========")
                  (doto s stop!)))
              (reset! running? false)
              (log/pr :user "System stopped."))
          (log/pr :warn "System cannot be stopped; system is not running.")))
  ISystem
    (init! [this]
      (if @running?
          (log/pr :warn "System already created.")
          (do (reset! sys-map
                (make-system config))
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