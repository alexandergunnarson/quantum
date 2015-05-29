(ns
  ^{:doc "A very experimental namespace based on using the shell,
          interacting with it, using common commands, etc., from
          within a REPL."
    :attribution "Alex Gunnarson"}
  quantum.core.util.sh
  (:require-quantum [ns coll str io fn sys log logic])
  #?(:clj (:import java.lang.ProcessBuilder)))

; As of 1.5, ProcessBuilder.start() is the preferred way to create a Process.
(def processes (atom {}))

#?(:clj
(defn- update-proc-info!
  [^String proc-name ^Process proc ^Keyword out-type]
  (swap! processes update-in+ [proc-name out-type] 
      (fn [record]
        (let [stream
                (condp = out-type
                  :out (.getInputStream proc)
                  :err (.getErrorStream proc))
              in (-> stream str (str/split #"\n"))]
          (doseq [^String line ^Vec in]
            (println "PROCESS" proc-name ":" line))
          (if (vector? record)
              (conj record in)
              [in]))))))

#_(:clj
(defn exec!
  {:todo ["Likely deprecated."
          "A few of them written in the code here..."
          "Record in /processes/ when process is terminated"]
   :example "(exec! [:projects \"clojure-getting-started\"] \"heroku\" \"ps\")"}
  [dir-0 & args]
  (let [^ProcessBuilder pb (ProcessBuilder. ^java.util.List args)
        ^java.io.File dir-f
          (if (vector? dir-0)
              (io/file dir-0)
              (io/file [:home]))
        _ (.directory pb dir-f)
        _ (.redirectError pb java.lang.ProcessBuilder$Redirect/INHERIT) ; This redirects the output of the process to *out*
        ^String proc-name
          (if (vector? dir-0)
              (apply str/sp args)
              (apply str/sp dir-0 args))
        ^Process proc (.start pb)]
    ; TODO: Get these all the way, updating asynchronously, till done
    ; TODO: Do these simultaneously in 
    ;(update-proc-info! proc-name proc :out)
    ;(update-proc-info! proc-name proc :err)
   
    proc)))

#?(:clj
(defn run-process!
  {:source "bevuta/pepa.util"}
  [command args & [{:keys [ex-data env-vars dir timeout]
                    :or   {ex-data {} timeout Long/MAX_VALUE} :as opts}]]
  (let [process (->> args
                     (<- conjl command)
                     (map str)
                     into-array
                     (ProcessBuilder.))
        env-vars-f (merge-keep-left (or env-vars {}) sys/user-env)
        set-env-vars!
          (doseq [^String env-var ^String val- env-vars-f]
            (-> process (.environment) (.put env-var val-)))
        _ (when dir (.directory process (io/file dir)))
        _ (.redirectError  process java.lang.ProcessBuilder$Redirect/INHERIT)
        _ (.redirectOutput process java.lang.ProcessBuilder$Redirect/INHERIT)
        _ (log/pr :debug (str "Starting |" command "|") "with dir" dir "and environment" (.environment process))
        process (.start process)
        ;; TODO: We can use the new (.setTimeout process timeout unit) soon
        fut (future (.waitFor process))
        exit-code (if (and timeout (number? timeout))
                      (deref fut timeout ::timeout)
                      @fut)]
    (if (and (not= ::timeout exit-code)
             (zero? exit-code))
        process
        (throw (ex-info (str (pr-str command) " didn't terminate correctly")
                        (assoc ex-data
                               :exit-code exit-code
                               :args args
                               ::timeout timeout)))))))

#?(:clj (def exec! run-process!))

