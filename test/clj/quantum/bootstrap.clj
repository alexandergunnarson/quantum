(ns quantum.bootstrap
  (:require-quantum [ns])
  (:require
      [com.stuartsierra.component :as component]
      [clojure.tools.namespace.repl :refer
        [refresh refresh-all set-refresh-dirs]]))

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
  (if @system-running?
      (println "System already created.")
      (do (ns/reset-var! system
            (make-system))
          (println "System created."))))

(defn start! []
  (if @system-running?
      (println "System already running.")
      (do (ns/swap-var! system component/start)
          (reset! system-running? true)
          (println "System started."))))

(defn stop! []
  (if (and system @system-running?)
      (do (ns/swap-var! system
            (fn [s]
              (println "======== STOPPING SYSTEM ========")
              (doto s
                (component/stop))))
          (reset! system-running? false)
          (println "System stopped."))
      (println "System cannot be stopped; system is not running.")))

(defn go! []
  (init!)
  (start!))

(defn force-refresh! []
  (->> (all-ns)
       (map ns-name)
       (filter (fn [x] (and (-> x name (not= "quantum.bootstrap"))
                            (.contains ^String (name x) "quantum"))))
       (map remove-ns)))

(defn reload!
  ([] (reload! 'quantum.bootstrap/go!))
  ([f-sym]
    (stop!)
    (when f-sym
      (force-refresh!)
      (refresh-all :after f-sym))))
