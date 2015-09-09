(ns quantum.core.resources
  (:require-quantum
    [ns num fn str err macros logic vec coll log async qasync])
  #?(:clj (:require
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer
              [refresh refresh-all set-refresh-dirs]]))
  #?(:clj (:import org.openqa.selenium.WebDriver
                   (java.lang ProcessBuilder Process StringBuffer)
                   (java.io InputStream Reader Writer
                     IOException)
                   (java.util.concurrent TimeUnit)
                   quantum.core.data.queue.LinkedBlockingQueue
                   clojure.core.async.impl.channels.ManyToManyChannel)))

(defnt open?
#?(:clj
  ([^java.io.InputStream stream]
    (try (.available stream) true
      (catch IOException _ false)))
  ([^quantum.core.data.queue.LinkedBlockingQueue obj] (qasync/closed? obj)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel   obj] (throw+ :not-implemented)))

(def closed? (fn-not open?))

(defnt close!
  #?@(:clj
 [([#{Writer Reader}     obj] (.close obj))
  ([^quantum.core.data.queue.LinkedBlockingQueue obj] (qasync/close! obj))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel   obj] (async/close! obj))
  ([                     obj]
    (when (nnil? obj) (throw+ :not-implemented))))

(defnt closeable?
  ([^java.io.Closeable x] true)
  ([x] false))

(defnt cleanup!
  #?@(:clj
 [([^org.openqa.selenium.WebDriver obj] (.quit  obj))
  ([^java.io.Closeable             obj] (.close obj))]))

(defn with-cleanup [obj cleanup-seq]
  (conj! cleanup-seq #(close! obj))
  obj)

(defmacro with-resources
  [bindings & body]
  `(let ~bindings
     (try
       ~@body
       (finally
         (doseq [resource# ~(->> bindings (apply array-map) keys vec)]
           (cleanup! resource#))))))

; ======= SYSTEM ========


(defonce system-factory (atom {}))

(defn make-system []
  (apply component/system-map
    (reduce
      (fn [ret k f]
        (conj ret k (f)))
      []
      @system-factory)))

(defonce system nil)

(defonce system-running? (atom false))

(defn init! []
  (log/disable! :macro-expand)
  (if @system-running?
      (log/pr :warn "System already created.")
      (do (reset-var! system
            (make-system))
          (log/pr :user "System created."))))

(defn start! []
  (log/enable! :Debug)
  (if @system-running?
      (log/pr :warn "System already running.")
      (do (swap-var! system component/start)
          (reset! system-running? true)
          (log/pr :user "System started."))))

(defn stop! []
  (if (and system @system-running?)
      (do (swap-var! system
            (fn [s]
              (log/pr :user "======== STOPPING SYSTEM ========")
              (doto s
                (component/stop))))
          (reset! system-running? false)
          (log/pr :user "System stopped."))
      (log/pr :warn "System cannot be stopped; system is not running.")))

(defn go! []
  (init!)
  (start!))

(defn reload!
  ([] (reload! nil))
  ([ns-]
    (stop!)
    (when ns-
      (refresh :after ns-))
    (go!)))

(defn force-refresh! []
  (->> (all-ns)
       (map ns-name)
       (filter (fn-> name (containsv? "quantum")))
       (map remove-ns)))

#?(:clj
(defmacro register-component! [start-fn stop-fn]
  (let [sym 'make-component__gen]
    `(do ~(quote+ (defrecord NsComponent []
                    com.stuartsierra.component/Lifecycle
                    (start [component#] (~start-fn component#))
                    (stop  [component#] (~stop-fn  component#))))
         ~(quote+ (defn ~sym []
                    (map->NsComponent {})))
         (swap! quantum.core.resources/system-factory assoc
           (-> ~*ns* ns-name name keyword) ~sym)))))
