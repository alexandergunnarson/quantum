(ns ^{:doc "Convenience functions for creating a system and registering components
            according to Stuart Sierra's Component framework."}
  quantum.core.resources
  (:require-quantum
    [:core #_num fn #_str log err macros logic vec #_coll log core-async async tpred])
  (:require [com.stuartsierra.component       :as component]
    #?(:clj [clojure.tools.namespace.repl
              :refer [refresh refresh-all set-refresh-dirs]]))
  #?(:clj (:import org.openqa.selenium.WebDriver
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

(def closed? (fn-not open?))

#?(:clj
(defnt close!
  #?@(:clj
 [([#{Writer Reader}     obj] (.close obj))
  ([^quantum.core.data.queue.LinkedBlockingQueue obj] (async/close! obj))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel   obj] (core-async/close! obj))
  ([                     obj]
    (when (nnil? obj) (throw (->ex :not-implemented))))))

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
  (start!  [this])
  (stop!   [this])
  (go!     [this])
  (reload! [this] [this ns-]))

(defrecord System [config sys-map make-system running?]
  ISystem
    (init! [this]
      (if @running?
          (log/pr :warn "System already created.")
          (do (reset! sys-map
                (make-system config))
              (log/pr :user "System created."))))
    (start! [this]
      (if @running?
          (log/pr :warn "System already running.")
          (do (swap! sys-map component/start)
              (reset! running? true)
              (log/pr :user "System started."))))
    (stop! [this]
      (if (and @sys-map @running?)
          (do (swap! sys-map
                (fn [s]
                  (log/pr :user "======== STOPPING SYSTEM ========")
                  (doto s
                    (component/stop))))
              (reset! running? false)
              (log/pr :user "System stopped."))
          (log/pr :warn "System cannot be stopped; system is not running.")))
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