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

(defmacro report [source- & args] `(try+ ~@args (catch Object e# (log/pr-opts :debug #{:thread?} "FROM SOURCE" ~source- "THIS IS EXCEPTION" e#) (throw+))))

#?(:clj
(defn read-streams
  [{:keys [process id input-streams output-stream stream-names writer buffers output-line state
           cleanup-seq close-reqs line-timeout output-timeout output-timeout-handler]
    :or {output-line fn-nil
         line-timeout 500
         output-timeout 5000}}] ; for some reason this isn't happening
  (log/pr ::debug "READING STREAMS FOR PID" id "WITH STATE" state)
  (let [input-stream-worker
          (lt-thread-loop
            {:name :input-stream :parent id :close-reqs close-reqs}
            []
            (let [input-str (qasync/take-with-timeout! output-stream 2000)] ; so it can make sure to close afterward
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
            stream-source (get stream-names n)
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
                {:name :line-reader :parent id :close-reqs close-reqs
                 :handlers {:error/any.post (fn [state-] (thread/close! id))}}
                 ; atom because sleep-time isn't getting updated... weird...
                [i (long 0) slept? false sleep-time 0]
                (when (>= sleep-time 5000) ((or output-timeout-handler fn-nil) state sleep-time stream-source))
                (let [indices (for [c str/line-terminator-chars]
                                (.indexOf ^StringBuffer buffer (str c) i))
                      i-n (->> indices (remove neg?) num/least)]
                  (if (or (nil? i-n) (neg? i-n))
                      (if (and slept? (> (lasti buffer) i)) ; unoutputted chars in buffer 
                          (do (output-line state (.subSequence ^StringBuffer buffer i (lasti buffer)) stream-source)
                              (recur (lasti buffer) false 0))
                          (do (Thread/sleep line-timeout)
                              (recur i true (+ sleep-time line-timeout)))) 
                      (do  (output-line state (.subSequence ^StringBuffer buffer i i-n) stream-source)
                           (recur (inc (long i-n)) false 0)))))])))))

#?(:clj
(def ^{:help "http://www.tldp.org/LDP/abs/html/exitcodes.html"}
  exit-code-table 
  {1   (Err. :bash/general                  "General error"                                                                      nil)
   2   (Err. :bash/shell-misuse             "Misuse of shell builtins, missing keyword or command, or permission problem"        nil)
   126 (Err. :bash/command-not-executable   "Command invoked cannot execute. Permission problem or command is not an executable" nil)
   127 (Err. :bash/command-not-found        "Possible problem with $PATH or a typo"                                              nil)
   128 (Err. :bash/invalid-argument-to-exit "Exit takes only integer args in the range 0 - 255"                                  nil)
   130 (Err. :bash/user-terminated          "Process terminated by Control-C"                                                    nil)
   ; On Macs it's 137
   137 (Err. :bash/system-terminated        "Process terminated by system force quit"                                            nil)
   255 (Err. :bash/exit-code-out-of-range   "Exit code out of range"                                                             nil)}))

#?(:clj
(defn exit-code-handler
  [state exit-code process process-id early-termination-handlers]
  (with-throw (number? exit-code)
    (Err. ::exit-code-not-a-number
          "Exit code is not a number"
          exit-code))
  (try+
    (cond
      (zero? exit-code)
        (do (log/pr :debug "Process" process-id "finished running.")
            process)
      (get exit-code-table exit-code)
        (throw+ (get exit-code-table exit-code))
      (and (> exit-code 128) (< exit-code 255))
        (throw+ (Err. :bash/fatal-error "Fatal error signal" (- exit-code 128)))
      :else
        (throw+ (Err. ::exit-code-unknown
                      "Exit code not recognized"
                      exit-code)))
    (catch Object e
      ((or (get early-termination-handlers (:type e))
           (get early-termination-handlers :default )
           (throw+))
        state
        e)))))

#?(:clj
(defn run-process!
  "@output-handler is called on line terminator.
   If there is no line terminator, it is never called.
   Hopefully most command line programs will not output that way."
  [command args & [{{:keys [output-line closed]} :handlers
                    :keys [ex-data env-vars dir timeout id parent state
                           print-streams? read-streams? close-reqs
                           handlers output-timeout thread? pr-to-out?
                           cleanup-seq]
                    :or   {ex-data     {}
                           cleanup-seq (atom [])
                           close-reqs  (LinkedBlockingQueue.)
                           timeout     Long/MAX_VALUE}
                    :as opts}]]
  (let [parent (if thread? (thread/gen-proc-id nil parent :sh-process-wrapper) parent)
        entire-process
         (fn []
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
                 redirect-method (if pr-to-out?
                                     java.lang.ProcessBuilder$Redirect/INHERIT
                                     java.lang.ProcessBuilder$Redirect/PIPE)
                 _ (.redirectError  pb redirect-method)
                 _ (.redirectOutput pb redirect-method)
                 process-id (or id (genkeyword command))
                 _ (log/pr ::debug "STARTING PROCESS"
                     (str/paren (str/sp "process-id" process-id)) command args)
                 std-buffer    (StringBuffer. 400)
                 err-buffer    (StringBuffer. 400)
                 cleanup       (delay (doseq [f @cleanup-seq]
                                        (try+ (f) (catch Object e (log/pr :warn "Exception in cleanup:" e)))))
                 output-stream (with-cleanup (LinkedBlockingQueue.) cleanup-seq)
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
                       {:process                process
                        :id                     process-id
                        :writer                 writer
                        :output-stream          output-stream
                        :input-streams          [std-stream err-stream]
                        :buffers                [std-buffer err-buffer]
                        :stream-names           [:std :err]
                        :output-line            output-line
                        :cleanup-seq            cleanup-seq
                        :close-reqs             close-reqs
                        :output-timeout         output-timeout
                        :state                  state
                        :output-timeout-handler (:output-timeout handlers)}))
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
               (thread/close! child-id))
             ; The process thread is closed; the helper threads may still be being cleaned up
             (swap! thread/reg-threads assoc-in [process-id :state] :closed)
             (exit-code-handler state exit-code process process-id (:early-termination handlers))))]
    (if thread?
        (thread/lt-thread {:id parent} (entire-process))
        (entire-process)))))

#?(:clj (def exec! run-process!))
#?(:clj (def proc  run-process!))

#?(:clj
(defn input! [id s]
  (when-let [in-stream (get-in @thread/reg-threads [id :input-stream])]
    (put! in-stream s))))

