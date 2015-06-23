(ns
  ^{:doc "A very experimental namespace based on using the shell,
          interacting with it, using common commands, etc., from
          within a REPL."
    :attribution "Alex Gunnarson"}
  quantum.core.util.sh
  (:refer-clojure :exclude [assoc! conj!])
  (:require-quantum [ns coll str io fn sys log logic macros thread async qasync
                     err res time])
  (:require [quantum.core.resources :as res :refer [closed?]])
  #?(:clj (:import (java.lang ProcessBuilder Process StringBuffer)
                   (java.io InputStreamReader BufferedReader
                     OutputStreamWriter BufferedWriter
                     IOException)
                   (java.util.concurrent LinkedBlockingQueue)
                   clojure.core.async.impl.channels.ManyToManyChannel)))
  
(defn line-creator-fn
  [char-stream line-stream
   ^StringBuffer line-temp line-capacity terminator]
  (loop [chars-read 0]
    (if (nempty? terminator) ; fine, it'll deliver it
        (put! line-stream (str line-temp))
        (let [c (take-with-timeout! char-stream 2000)] ; Update at least every 2 seconds
          (.append line-temp c)
          (if (or (str/line-terminator? c) (nil? c))
              (put! line-stream (str line-temp))
              (recur (inc chars-read))))))
  (.setLength  line-temp 0)
  (.trimToSize line-temp)
  (.ensureCapacity line-temp line-capacity))

(defn line-exporter-fn [line-stream ^StringBuffer buffer line-handler]
  (let [line (take! line-stream)]
    (log/pr ::debug "Appending to buffer")
    (.append buffer line)
    (log/pr ::debug "Appended to buffer")
    (line-handler line)))

(defn reader-fn [char-stream ^InputStreamReader reader]
  (put! char-stream (-> (.read reader) int char)))

#?(:clj
(defn read-streams
  [{:keys [process process-id input-streams output-stream writer buffers output-handler
           cleanup-seq]
    :or {output-handler fn-nil}}]
  (let [input-stream-worker
          (lt-thread {:name :input-stream :parent process-id :cleanup cleanup-seq}
            (loop []
              (when-not (closed? output-stream)
                (let [input-str (take! output-stream)]
                  (when (string? input-str)
                    (.write ^BufferedWriter writer ^String input-str)
                    (.flush ^BufferedWriter writer))
                  (recur)))))
        readers (for [stream input-streams]
                  (with-cleanup (InputStreamReader. stream) cleanup-seq))]
    (doseqi [^InputStreamReader reader readers n]
      (let [^StringBuffer buffer (get buffers n)
            char-stream  (with-cleanup (LinkedBlockingQueue.) cleanup-seq)
            line-stream  (with-cleanup (LinkedBlockingQueue.) cleanup-seq)
            line-capacity 200
            temp-line    (StringBuffer. line-capacity)
            line-creation-forcer (with-cleanup (LinkedBlockingQueue.) cleanup-seq)
            line-created (LinkedBlockingQueue.)
            line-creator-manager
              (lt-thread {:name :line-creator-manager :parent process-id :cleanup cleanup-seq}
                (loop [] (when-not (or (closed? char-stream) (closed? line-stream))
                           (Thread/sleep 2000)
                           (if (empty? line-created)
                               (put! line-creation-forcer true)
                               (.clear ^LinkedBlockingQueue line-created))
                           (recur))))
            line-creator-worker
              (lt-thread {:name :line-creator :parent process-id :cleanup cleanup-seq}
                (loop [] (when-not (or (closed? char-stream) (closed? line-stream))
                           (let []
                             (line-creator-fn char-stream line-stream
                                temp-line line-capacity line-creation-forcer)
                             (put! line-created true)
                             (.clear ^LinkedBlockingQueue line-creation-forcer)
                             (recur)))))
            reader-worker
              (lt-thread {:name :reader       :parent process-id :cleanup cleanup-seq}
                (loop [] (when-not (or (closed? char-stream) (closed? line-stream))
                           (reader-fn        char-stream reader     )
                           (recur))))
            line-worker
              (lt-thread {:name :line         :parent process-id :cleanup cleanup-seq}
                (loop [] (when-not (or (closed? char-stream) (closed? line-stream))
                           (line-exporter-fn line-stream buffer output-handler)
                           (recur))))])))))

#?(:clj
(defn run-process!
  "@output-handler is called on line terminator.
   If there is no line terminator, it is never called.
   Hopefully most command line programs will not output that way."
  [command args & [{:keys [ex-data env-vars dir timeout id parent-id
                           print-streams? read-streams? log-delay
                           output-handler]
                    :or   {ex-data {}
                           timeout Long/MAX_VALUE}
                    :as opts}]]
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
        _ (.redirectError  process java.lang.ProcessBuilder$Redirect/PIPE)
        _ (.redirectOutput process java.lang.ProcessBuilder$Redirect/PIPE)
        process-id (or id (genkeyword command))
        _ (log/pr ::debug "STARTING PROCESS"
            (str/paren (str/sp "process-id" process-id)) command args)
        std-buffer    (StringBuffer. 400)
        err-buffer    (StringBuffer. 400)
        cleanup-seq   (atom [])
        close-reqs    (with-cleanup (chan) cleanup-seq)
        output-stream (with-cleanup (chan) cleanup-seq)
        _ (swap! thread/reg-threads assocs-in+
            [process-id :output       ] std-buffer
            [process-id :err-output   ] err-buffer
            [process-id :close-reqs   ] close-reqs
            [process-id :output-stream] output-stream)
        _ (when parent-id
            (swap! thread/reg-threads assoc-in
              [parent-id :children] process-id))

        ^Process process (.start process)
        std-stream (.getInputStream process)
        err-stream (.getErrorStream process)
        writer     (-> process .getOutputStream
                       (OutputStreamWriter.)
                       (BufferedWriter.)
                       (with-cleanup cleanup-seq))
        _ (swap! thread/reg-threads assoc-in
            [process-id :thread] process)
        worker-threads
          (when (or read-streams? print-streams? log-delay)
            (read-streams
              {:process        process
               :process-id     process-id
               :writer         writer
               :output-stream  output-stream
               :input-streams  [std-stream err-stream]
               :buffers        [std-buffer err-buffer]
               :output-handler output-handler
               :log-delay      log-delay
               :cleanup-seq    cleanup-seq}))
        close-listener
          (lt-thread {:name :close-listener :parent process-id}
            (<! close-reqs)
            (doseq [f @cleanup-seq]
              (try+ (f) (catch Object _)))
            (log/pr :debug process-id "cleaned up!"))
        exit-code (.waitFor process)]
    (thread/close-gracefully! process-id)
    (if (zero? exit-code)
        (do (log/pr :user "Process" process-id "finished running.")
            process)
        (throw+ {:msg (str/sp command "didn't terminate correctly")
                 :type :process/early-termination
                 :exit-code exit-code
                 :args args})))))

#?(:clj (def exec! run-process!))

#?(:clj
(defn gen-cleanup-handler [id & [parent-id]]
  #(do (swap! thread/reg-threads dissoc id)
       (when parent-id
         (swap! thread/reg-threads update-in
           [parent-id :children] (f*n disj id))))))

#?(:clj
(defn input! [id s]
  (when-let [in-stream (get-in @thread/reg-threads [id :input-stream])]
    (put! in-stream s))))




(comment
(let [n "57"
      thread-id (str/keyword+ "convert" n)
        proc-id   (str/keyword+ "ffmpeg-convert" n)
        mp4-path  [:resources "SIRE" (str n ".mp4")]]
        (log/enable! :debug)
    (thread+ {:id thread-id :handlers {:closed (fn [t] (io/delete! :path mp4-path))}}
      (thread/add-child-proc! thread-id proc-id) 
      (run-process! "ffmpeg"
      ["-i" (io/file-str [:resources "SIRE" (str n ".wmv")])
       "-codec:v" "libx264" "-crf" 23 ; average
       "-codec:a" "libfaac" "-qscale:a" 100
       "-threads" 1
       (io/file-str  mp4-path)]
      (merge-keep-left
        {:read-streams? true 
         :threads 1
         :output-handler
           (fn [line]
             (log/pr :debug "Calling output handler")
             (when (contains? line "Overwrite ?")
                 (input! proc-id "y\n")))
         :id proc-id}
       {:print-output :ffmpeg-convert
        :log-delay 10}))))
  )
