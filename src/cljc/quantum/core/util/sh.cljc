(ns
  ^{:doc "A very experimental namespace based on using the shell,
          interacting with it, using common commands, etc., from
          within a REPL."
    :attribution "Alex Gunnarson"}
  quantum.core.util.sh
  (:refer-clojure :exclude [assoc! conj!])
  (:require-quantum [ns coll str io fn sys log logic macros thread async qasync
                     err res time num])
  (:require [quantum.core.resources    :as res    :refer [closed?   ]]
            [quantum.core.thread.async :as qasync :refer [close-req? message?]])
  #?(:clj (:import (java.lang ProcessBuilder Process StringBuffer)
                   (java.io InputStreamReader BufferedReader
                     OutputStreamWriter BufferedWriter
                     IOException)
                   quantum.core.data.queue.LinkedBlockingQueue
                   clojure.core.async.impl.channels.ManyToManyChannel)))

#?(:clj
(defn read-streams
  [{:keys [process id input-streams output-stream writer buffers output-line
           cleanup-seq close-reqs line-timeout]
    :or {output-line fn-nil
         line-timeout 500}}]
  (log/pr ::debug "READING STREAMS FOR PID" id)
  (let [input-stream-worker
          (lt-thread-loop
            {:name :input-stream :parent id :close-reqs close-reqs}
            []
            (let [input-str (take! output-stream)]
              (when (string? input-str)
                (.write ^BufferedWriter writer ^String input-str)
                (.flush ^BufferedWriter writer))
              (recur)))
        readers (for [stream input-streams]
                  (-> stream
                      (InputStreamReader.)
                      (BufferedReader.)
                      (with-cleanup cleanup-seq)))]
    (doseqi [^BufferedReader reader readers n]
      (let [^StringBuffer buffer (get buffers n)
            buffer-writer
              (lt-thread-loop
                {:name :buffer-writer :parent id :close-reqs close-reqs}
                []
                (let [i (int (.read ^BufferedReader reader))] ; doesn't actually block... actually pretty stupid
                  (if (= i (int -1))
                      (Thread/sleep 10) ; 10ms delay to read the next char if there's no chars
                      (->> i char
                           (.append ^StringBuffer buffer)))
                  (recur)))
            line-reader
              (lt-thread-loop
                {:name :line-reader :parent id :close-reqs close-reqs}
                [i (long 0)]
                (let [indices (for [c str/line-terminator-chars]
                                (.indexOf ^StringBuffer buffer (str c) i))
                      i-n (->> indices (remove neg?) num/least)]
                  (if (or (nil? i-n) (neg? i-n))
                      (do (Thread/sleep line-timeout)
                          (recur i))
                      (do (output-line (.subSequence ^StringBuffer buffer i i-n))
                          (recur (inc (long i-n)))))))])))))

#?(:clj
(defn run-process!
  "@output-handler is called on line terminator.
   If there is no line terminator, it is never called.
   Hopefully most command line programs will not output that way."
  [command args & [{{:keys [output-line closed]} :handlers
                    :keys [ex-data env-vars dir timeout id parent
                           print-streams? read-streams? close-reqs
                           handlers
                           cleanup-seq]
                    :or   {ex-data     {}
                           cleanup-seq (atom [])
                           close-reqs  (LinkedBlockingQueue.)
                           timeout     Long/MAX_VALUE}
                    :as opts}]]
  (log/pr ::debug "READING STREAMS FOR PROC" id "?" ":" read-streams?)
  (let [pb (->> args
                (<- conjl command)
                (map str)
                into-array
                (ProcessBuilder.))
        env-vars-f (merge-keep-left (or env-vars {}) sys/user-env)
        set-env-vars!
          (doseq [^String env-var ^String val- env-vars-f]
            (-> pb (.environment) (.put env-var val-)))
        _ (when dir (.directory pb (io/file dir)))
        _ (.redirectError  pb java.lang.ProcessBuilder$Redirect/PIPE)
        _ (.redirectOutput pb java.lang.ProcessBuilder$Redirect/PIPE)
        process-id (or id (genkeyword command))
        _ (log/pr ::debug "STARTING PROCESS"
            (str/paren (str/sp "process-id" process-id)) command args)
        std-buffer    (StringBuffer. 400)
        err-buffer    (StringBuffer. 400)
        cleanup       (delay (doseq [f @cleanup-seq]
                               (try+ (f) (catch Object e (log/pr :warn "Exception in cleanup:" e)))))
        output-stream (with-cleanup (chan) cleanup-seq)
        _ (log/pr ::debug "BEFORE REG THREAD" )
        _ (thread/register-thread!
            {:id            process-id
             :parent        parent
             :std-output    std-buffer 
             :err-output    err-buffer 
             :close-reqs    close-reqs
             :output-stream output-stream
             :state         :running
             :handlers
               {:closed      (thread/wrap-delay closed)
                :output-line output-line
                :cleanup     cleanup}})
        _ (log/pr ::debug "AFTER REG THREAD" )
        ^Process process (.start pb)
        _ (log/pr ::debug "AFTER PROCESS START" )
        _ (swap! thread/reg-threads assoc-in
            [process-id :thread] process)
        std-stream       (.getInputStream process)
        err-stream       (.getErrorStream process)
        writer     (-> process (.getOutputStream)
                       (OutputStreamWriter.)
                       (BufferedWriter.)
                       (with-cleanup cleanup-seq))
       _ (log/pr ::debug "BEFORE WORKER THREADS" )
        worker-threads
          (when (or read-streams? print-streams?)
            (read-streams
              {:process        process
               :id             process-id
               :writer         writer
               :output-stream  output-stream
               :input-streams  [std-stream err-stream]
               :buffers        [std-buffer err-buffer]
               :output-line    output-line
               :cleanup-seq    cleanup-seq
               :close-reqs     close-reqs}))
        children (get-in @thread/reg-threads [id :children])
        _ (swap! cleanup-seq conj #(thread/close-impl! process))
        exit-code (.waitFor process)
        ; In order to have the print output catch up
        print-delay
          (when (and (empty? close-reqs)
                     (or read-streams? print-streams?))
            (Thread/sleep 1000))]
    ; the close operation asynchronously/indirectly
    ; invokes the close-listener if it has not already been done
    (doseq [child-id children]
      (thread/close! child-id #{:gracefully?}))
    ; The process thread is closed; the helper threads may still be being cleaned up
    (swap! thread/reg-threads assoc-in [process-id :state] :closed)
    (if (zero? exit-code)
        (do (log/pr :debug "Process" process-id "finished running.")
            process)
        (if-let [early-termination-handler
                  (-> handlers :early-termination :default)]
          (early-termination-handler {:exit-code exit-code :args args})
          (throw+ {:msg (str/sp "Unhandled exception:" command "didn't terminate correctly")
                   :type :process/early-termination
                   :exit-code exit-code
                   :args args}))))))

#?(:clj (def exec! run-process!))

#?(:clj
(defn input! [id s]
  (when-let [in-stream (get-in @thread/reg-threads [id :input-stream])]
    (put! in-stream s))))

