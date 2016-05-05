(ns
  ^{:doc "A very experimental namespace based on using the shell,
          interacting with it, using common commands, etc., from
          within a REPL."
    :attribution "Alex Gunnarson"}
  quantum.core.process
           (:require [com.stuartsierra.component :as component]
           #?@(:clj [[clojure.java.io            :as io       ]
                     [clojure.java.shell         :as shell    ]])
                     [quantum.core.paths         :as paths    ]
                     [quantum.core.resources     :as res     
                       :refer [closed?]                       ]
                     [quantum.core.thread.async 
                       :refer [close-req? message?]]
                    [quantum.core.vars           :as var
                       :refer [#?@(:clj [defalias])]          ])
  #?(:cljs (:require-macros
                     [quantum.core.vars          :as var
                       :refer [defalias]                      ]))
  #?(:clj (:import (java.lang ProcessBuilder StringBuffer)
                   (java.io InputStreamReader BufferedReader
                     OutputStreamWriter BufferedWriter
                     IOException)
                   ;quantum.core.data.queue.LinkedBlockingQueue
                   clojure.core.async.impl.channels.ManyToManyChannel)))

; #?(:clj
; (defn read-streams
;   "@output-streams are 'output' relative to the process.
;    They are, of course, 'input' streams relative to the consumers of these streams."
;   [{:keys [process id stream-names
;            output-streams buffers output-chans
;            input-chan  std-in-writer output-line state
;            cleanup-seq close-reqs line-timeout output-timeout output-timeout-handler]
;     :or {line-timeout 500
;          output-timeout 5000}}] ; for some reason this isn't happening
;   (log/pr ::debug "READING STREAMS FOR PID" id "WITH STATE" state)
;   (let [output-line            (or output-line            fn-nil)
;         output-timeout-handler (or output-timeout-handler fn-nil)
;         input-chan-worker
;           (async-loop
;             {:name :input-chan :parent id :close-reqs close-reqs}
;             []
;             (log/pr ::inspect "Input stream doing its job")
;             (let [input-str (async/take!! input-chan 2000)] ; so it can make sure to close afterward
;               (when (string? input-str)
;                 (.write ^BufferedWriter std-in-writer ^String input-str)
;                 (.flush ^BufferedWriter std-in-writer))
;               (recur)))
;         readers (for [stream output-streams]
;                   (-> stream
;                       (InputStreamReader.)
;                       (BufferedReader.)
;                       (with-cleanup cleanup-seq)))]
;     (doseqi [^BufferedReader reader readers n]
;       (let [^StringBuffer buffer (get buffers n)
;             stream-source (get stream-names n)
;             output-chan (get output-chans n)
;             buffer-writer
;               (async-loop
;                 {:name (str/keyword+ :buffer-writer- stream-source) :parent id :close-reqs close-reqs}
;                 []
;                 (let [i (int (.read ^BufferedReader reader))] ; doesn't actually block... actually pretty stupid
;                   (if (= i (int -1))
;                       (async/sleep 10) ; 10ms delay to read the next char if there's no chars
;                       (->> i char
;                            (.append ^StringBuffer buffer)))
;                   (recur)))
;             line-reader
;               (async-loop
;                 {:name (str/keyword+ :line-reader- stream-source) :parent id :close-reqs close-reqs
;                  :handlers {:error/any.post (fn [state-] (thread/close! id))}}
;                  ; atom because sleep-time isn't getting updated... weird...
;                 [i (long 0) slept? false sleep-time 0]
;                 (when (>= sleep-time 5000)
;                   (output-timeout-handler state sleep-time stream-source))
;                 (let [; first index of any line terminator after i
;                       indices (for [c str/line-terminator-chars]
;                                 (.indexOf ^StringBuffer buffer (str c) i))
;                       i-n (->> indices (remove neg?) num/least)]
;                   (if (nil? i-n)
;                       (if (and slept? (> (lasti buffer) i)) ; unoutputted chars in buffer 
;                           ; Output all of the remaning ones
;                           (do (let [rest-line (.subSequence ^StringBuffer buffer i (lasti buffer))]
;                                 (when-not (str/whitespace? rest-line)
;                                   (>!! output-chan rest-line))
;                                 (output-line state rest-line stream-source))
;                               (recur (lasti buffer) false 0))
;                           (do (async/sleep line-timeout)
;                               (recur i true (+ sleep-time line-timeout)))) 
;                       (do (let [line (.subSequence ^StringBuffer buffer i (inc (long i-n)))]
;                             (when-not (str/whitespace? line)
;                                (>!! output-chan line))
;                             (output-line state line stream-source))
;                           (recur (inc (long i-n)) false 0)))))])))))

; #?(:clj
; (def ^{:help "http://www.tldp.org/LDP/abs/html/exitcodes.html"}
;   exit-code-table 
;   {1   (Err. :bash/general                  "General error"                                                                      nil)
;    2   (Err. :bash/shell-misuse             "Misuse of shell builtins, missing keyword or command, or permission problem"        nil)
;    126 (Err. :bash/command-not-executable   "Command invoked cannot execute. Permission problem or command is not an executable" nil)
;    127 (Err. :bash/command-not-found        "Possible problem with $PATH or a typo"                                              nil)
;    128 (Err. :bash/invalid-argument-to-exit "Exit takes only integer args in the range 0 - 255"                                  nil)
;    130 (Err. :bash/user-terminated          "Process terminated by Control-C"                                                    nil)
;    ; On Macs it's 137
;    137 (Err. :bash/system-terminated        "Process terminated by system force quit"                                            nil)
;    255 (Err. :bash/exit-code-out-of-range   "Exit code out of range"                                                             nil)}))

; #?(:clj
; (defn exit-code-handler
;   [state exit-code process process-id early-termination-handlers]
;   (with-throw (number? exit-code)
;     (Err. ::exit-code-not-a-number
;           "Exit code is not a number"
;           exit-code))
;   (try+
;     (cond
;       (zero? exit-code)
;         (do (log/pr :debug "Process" process-id "finished running.")
;             process)
;       (get exit-code-table exit-code)
;         (throw+ (get exit-code-table exit-code))
;       (and (> exit-code 128) (< exit-code 255))
;         (throw+ (Err. :bash/fatal-error "Fatal error signal" (- exit-code 128)))
;       :else
;         (throw+ (Err. ::exit-code-unknown
;                       "Exit code not recognized"
;                       exit-code)))
;     (catch Object e
;       ((or (get early-termination-handlers (:type e))
;            (get early-termination-handlers :default )
;            (throw+))
;         state
;         e)))))

; #?(:clj
; (defn flush-stream! [^InputStream is ^StringBuffer buffer]
;   (let [reader (-> is (InputStreamReader.) (BufferedReader.))]
;     (while (.ready reader)
;       (->> reader .read int char (.append buffer))))))

; #?(:clj
; (defn run-process!
;   "@output-handler is called on line terminator.
;    If there is no line terminator, it is never called.
;    Hopefully most command line programs will not output that way."
;   [command args & [{{:keys [output-line closed]} :handlers
;                     :keys [ex-data env-vars dir timeout id parent state
;                            print-streams? ; Overrides @output-line handler; TODO handle this
;                            pr-to-out? ; Overrides other options; TODO handle this
;                            read-streams? 
;                            write-streams? ; Currently doesn't do much
;                            close-reqs
;                            std-buffer err-buffer ; for recording the process's output
;                            input-chan      ; chan for std-in
;                            err-output-chan ; chan for std-err
;                            std-output-chan ; chan for std-out
;                            handlers output-timeout
;                            thread? ; whether the process should be run asynchronously
;                            cleanup-seq]
;                     :or   {ex-data     {}
;                            cleanup-seq (atom [])
;                            close-reqs  (LinkedBlockingQueue.)
;                            timeout     Long/MAX_VALUE}
;                     :as opts}]]
;   (let [parent (if thread? (thread/gen-proc-id nil parent :sh-process-wrapper) parent)
;         entire-process
;          (fn []
;            (let [pb (->> args
;                          (<- conjl command)
;                          (map str)
;                          into-array
;                          (ProcessBuilder.))
;                  env-vars-f (merge-keep-left (or env-vars {}) @paths/user-env)
;                  set-env-vars!
;                    (doseq [^String env-var ^String val- env-vars-f]
;                      (-> pb (.environment) (.put env-var val-)))
;                  _ (when dir (.directory pb (io/file dir)))
;                  redirect-method (if pr-to-out?
;                                      java.lang.ProcessBuilder$Redirect/INHERIT
;                                      java.lang.ProcessBuilder$Redirect/PIPE)
;                  _ (.redirectError  pb redirect-method)
;                  _ (.redirectOutput pb redirect-method)
;                  process-id (or id (genkeyword command))
;                  _ (log/pr ::debug "STARTING PROCESS"
;                      (str/paren (str/sp "process-id" process-id)) command args)
;                  std-out-buffer    (or std-buffer (StringBuffer. 400))
;                  err-out-buffer    (or err-buffer (StringBuffer. 400))
;                  cleanup       (delay (doseq [f @cleanup-seq]
;                                         (try+ (f) (catch Object e (log/pr :warn "Exception in cleanup:" e)))))
;                  input-chan      (or input-chan      (with-cleanup (chan) cleanup-seq))
;                  err-output-chan (or err-output-chan (with-cleanup (chan) cleanup-seq))
;                  std-output-chan (or std-output-chan (with-cleanup (chan) cleanup-seq))
;                  output-line-handler (if print-streams? (fn [_ line _] (print line)) output-line)
;                  _ (log/pr ::debug "BEFORE REG THREAD" )
;                  _ (thread/register-thread!
;                      {:id              process-id
;                       :parent          parent
;                       :std-output      std-out-buffer 
;                       :err-output      err-out-buffer
;                       :err-output-chan err-output-chan
;                       :std-output-chan std-output-chan
;                       :close-reqs      close-reqs
;                       :input-chan      input-chan
;                       :status          :running
;                       :handlers
;                         {:closed      (thread/wrap-delay closed)
;                          :output-line output-line-handler
;                          :cleanup     cleanup}})
;                  _ (log/pr ::debug "AFTER REG THREAD" )
;                  ^Process process (.start pb)
;                  _ (log/pr ::debug "AFTER PROCESS START" )
;                  _ (swap! thread/reg-threads assoc-in
;                      [process-id :thread] process)
;                  std-out-stream       (.getInputStream process)
;                  err-out-stream       (.getErrorStream process)
;                  std-in-writer  (-> process
;                                     ; only "output" because the thread writes to it to
;                                     ; communicate with the underlying process
;                                     (.getOutputStream)
;                                     (OutputStreamWriter.)
;                                     (BufferedWriter.)
;                                     (with-cleanup cleanup-seq))
;                 _ (log/pr ::debug "BEFORE WORKER THREADS")
;                  worker-threads
;                    (when (or read-streams? print-streams?)
;                      (read-streams
;                        {:process                process
;                         :id                     process-id
;                         :std-in-writer          std-in-writer
;                         :output-streams         [std-out-stream  err-out-stream]
;                         :buffers                [std-out-buffer  err-out-buffer]
;                         :output-chans           [std-output-chan err-output-chan]
;                         :stream-names           [:std :err]
;                         :output-line            output-line-handler
;                         :input-chan             input-chan
;                         :cleanup-seq            cleanup-seq
;                         :close-reqs             close-reqs
;                         :output-timeout         output-timeout
;                         :state                  state
;                         :output-timeout-handler (:output-timeout handlers)}))
;                  children (get-in @thread/reg-threads [id :children])
;                  _ (swap! cleanup-seq conj #(async/close! process))
;                  _ (log/pr ::debug "Now waiting for process.")
;                  exit-code (.waitFor process)
;                  _ (log/pr ::debug "Finished waiting for process.")
;                  ; In order to have the print output catch up
;                  print-delay
;                    (when (and (empty? close-reqs)
;                               (or read-streams? print-streams?))
;                      (async/sleep 1000))]
;              (when (and (empty? close-reqs) write-streams?)
;                 (flush-stream! std-out-stream std-out-buffer)
;                 (flush-stream! err-out-stream err-out-buffer))
;              ; the close operation asynchronously/indirectly
;              ; invokes the close-listener if it has not already been done
;              (doseq [child-id children]
;                (thread/close! child-id))
;              _ (log/pr ::debug "Child processes closed.")
;              ; The process thread is closed; the helper threads may still be being cleaned up
;              (swap! thread/reg-threads assoc-in [process-id :state] :closed)
;              (exit-code-handler state exit-code process process-id (:early-termination handlers))))]
;     (if thread?
;         (thread/async {:id parent :type :thread} (entire-process))
;         (entire-process)))))

; #?(:clj (def exec! run-process!))
; #?(:clj (def proc  run-process!))

; #?(:clj
; (defn input! [id s]
;   (when-let [in-stream (get-in @thread/reg-threads [id :input-chan])]
;     (>!! in-stream s))))


#?(:clj (ns-unmap (ns-name *ns*) 'Process))

#?(:clj
(defrecord
  ^{:doc "|start| : Runs the process specified by @command, with the provided arguments
                    @args and options @opts."}
  Process
  [process command args env-vars dir pr-to-out?]
  component/Lifecycle
    (start [this]
      (let [pb (->> (into [command] args)
                    (map str)
                    into-array
                    (ProcessBuilder.))
            set-env-vars!
              (doseq [[^String env-var ^String val-] env-vars]
                (-> pb (.environment) (.put env-var val-)))
            _ (when dir (.directory pb (io/file dir)))
            redirect-method (if pr-to-out?
                                java.lang.ProcessBuilder$Redirect/INHERIT
                                java.lang.ProcessBuilder$Redirect/PIPE)
            _ (.redirectError  pb redirect-method)
            _ (.redirectOutput pb redirect-method)
            process (.start pb)]
        (assoc this :process process)))
    (stop [this]
      (when process
        (.destroy process))
      this)))

#?(:clj
(defn ->proc
  {:usage '(->proc "./bin/transactor"
             ["./config/samples/free-transactor-template.properties"]
             {:dir        "/home/datomic/"
              :pr-to-out? true
              :env-vars   {"DATOMIC_HOME" "/home/datomic/"}})}
  [command args & [{:keys [env-vars dir pr-to-out?]
                    :as opts}]]
  (Process. nil command args env-vars dir pr-to-out?)))

#?(:clj (defalias exec! shell/sh))