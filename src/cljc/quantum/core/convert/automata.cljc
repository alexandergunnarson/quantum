(ns quantum.core.automata)
; (defnt errorize
;   string?  ([k] (-> k keyword errorize))
;   keyword? ([k] (if (-> k namespace (= "error"))
;                     k
;                     (keyword (str "error/"
;                                (whenf (namespace k) nnil? (f*n str "."))
;                                (name k)))))
;   nil?     ([k] nil))

; (defonce workers (atom {}))

; ; Add to quantum.core.state-machine
; (defn update-state! [state new-status & args] 
;   (with-throw (nnil? (:handlers @state)) "Handlers nil at update-state! How?")
;   (let [{:keys [handlers result-set result-id]} @state
;         get-ns-handler #(get handlers (keyword (namespace new-status) %))
;         state-handler
;           (or (get handlers new-status)
;               (get-ns-handler "default")
;               fn-nil)]
;     (log/pr-no-trace :debug (str/bracket (:id @state)) "STATE TRANSITION FROM" (:status @state) "TO" new-status)
;     (assoc! state :status new-status)
;     (swap! result-set assoc-in [result-id :status] (:status @state))
  
;     (apply (or (get-ns-handler "any.pre" ) fn-nil) state args)
;     (apply state-handler state args)
;     (apply (or (get-ns-handler "any.post") fn-nil) state args)))

; (defn download!
;   [{:keys [type url file id parent
;            handlers close-reqs state]
;     :as opts}]
;   (with-throw (nnil? (:handlers @state)) "Handlers nil at download! How?")
;   (let [output-line (or (:output-line handlers) fn-nil)
;         handlers-f
;          (assoc (or handlers {})
;            :output-line
;              (fn [state line stream-source]
;                (output-line state line stream-source)
;                (cond
;                  (contains? line "HTTP error 404 Not Found")
;                    (update-state! state :error/http.404-not-found))))]
;     (log/pr :debug "DOWNLOADING" opts)
;     (sh/run-process! "ffmpeg" ["-i" url "-c" "copy" file]
;       (->> {:id           id
;             :parent       parent
;             :print-output :ffmpeg
;             :handlers     handlers-f}
;            (merge-keep-left (dissoc opts :handlers))
;            (<- assoc :read-streams? true :close-reqs close-reqs)))))

; (def log-buffer!
;   (atom
;     (fn [state line stream-source]
;       (with-throw (nnil? (:handlers @state)) "Handlers nil at log-buffer! How?")
;       (let [logged-buffer-get #(get-in @thread/reg-threads [(:id @state) :logged-buffer]) 
;             _ (when-not (logged-buffer-get)
;                 (swap! thread/reg-threads assoc-in
;                   [(:id @state) :logged-buffer] (StringBuffer. 200)))
;             ^StringBuffer buffer (logged-buffer-get)]
;         (.append buffer \[)
;         (.append buffer (str (time/str-now)))
;         (.append buffer \])
;         (.append buffer \space)
;         (.append buffer line)
;         (.append buffer \newline)))))

; (def output-line-handler
;   (atom
;     (fn [state line stream-source]
;       (with-throw (nnil? (:handlers @state)) "Handlers nil at output-line-handler! How?")
;       (@log-buffer! state line stream-source)
;       (if (or (str/starts-with? line "frame=") (any? (partial contains? line) #{"Non-monotonous" "Queue input is backward"}))
;           (log/pr-opts :unimportant #{:thread? :timestamp?} line)
;           (log/pr-opts :debug       #{:thread? :timestamp?} line))
;       (condpc contains? line
;         "Overwrite ?"
;           (do (sh/input! (:id @state) "N\n")
;               (update-state! state :error/download-overwrite-requested)
;               (thread/close! (:parent @state)))
;         "Failed to resolve hostname"
;           (update-state! state :error/host-not-found)
;         "Input/output error"
;           (update-state! state :error/io-error)))))

; (def output-timeout-handler 
;   (atom
;     (fn [state sleep-time stream-source]
;       (with-throw (nnil? (:handlers @state)) "Handlers nil at output-timeout-handler! How?")
;       (when (= stream-source :err) ; because there's no input from :std anyway...
;         (log/pr-opts :warn #{:thread?} "Strange issue with not outputting info from process" sleep-time)
;         (let [timeout  1 ; 1 minute
;               timeout? (-> sleep-time (/ 1000) (/ 60) (> timeout))]
;           (condp = (:status @state)
;             :downloading
;             ; After 1 minute of nonresponsiveness then shut it down and delete the file
;               (if (http/network-connected?)
;                   (if timeout?
;                       (update-state! state :error/downloading-timeout)
;                       (log/pr-opts :warn #{:thread?} "Strange issue with not outputting info from process" sleep-time))
;                   (update-state! state :error/connection-lost))
;             :converting
;               (if timeout?
;                   (update-state! state :error/converting-timeout)
;                   (log/pr-opts :warn #{:thread?} "Strange issue with not outputting info from process" sleep-time))
;             (log/pr :warn "OUTPUT TIMEOUT IN OTHER STATUS:" (:status @state))))))))

; (defn async-chain
;   [{:keys [state status result-id result-set]
;     :or {state      (atom {})
;          status     :running
;          result-set (atom {})
;          result-id  (gensym "result-id-")}
;     :as parent-opts} & thread-seq]
;   (with-throw (instance? clojure.lang.IAtom state) "State must be an atom")
;   (let [thread-seq (->> thread-seq (remove empty?))
;         _ (swap! state assoc
;             :handlers (:handlers parent-opts)
;             :status status :result-set result-set :result-id result-id)
;         state state]
;     (async (dissoc parent-opts :state :status :result-id :result-set)
;       (try+
;         (log/pr :debug "TRYING TO RUN THREADS" thread-seq)
;         (doseqi [thread-opts thread-seq n]
;           (log/pr :debug "TRYING TO RUN THREAD" thread-opts)
;           (swap! state assoc
;             :id       (:id       thread-opts)
;             :parent   (:id       parent-opts)
;             :handlers (:handlers thread-opts))
;           (let [error-state? #(do (when (and (nempty? (:close-reqs parent-opts))
;                                              (nempty? (:close-reqs thread-opts))
;                                              (not (-> @state :status namespace (= "error"))))
;                                     (update-state! state :error/close-req))
;                                   (-> @state :status namespace (= "error")))]
;             (try+ 
;               (when-not (error-state?)
;                 (update-state! state
;                   (or (-> thread-opts :status :init) (str/keyword+ "phase-" n "-running")))
;                 ((or (:proc thread-opts) fn-nil)))
;               (when-not (error-state?)
;                 (update-state! state
;                   (or (-> thread-opts :status :post) (str/keyword+ "phase-" n "-complete"))))
;               (catch Object e
;                 (log/pr-opts :warn #{:pretty? :thread?} "EXCEPTION:" e)
;                 (when-not (error-state?)
;                   (update-state! state (or (-> e :type errorize) :error/exception)
;                     e))))))
;         (when-not (-> @state :status namespace (= "error"))
;           (update-state! state :locally-complete))
;         (finally
;           ((or (:finally parent-opts) fn-nil))
;           (swap! result-set assoc-in [result-id :status] (:status @state))
;           (log/pr-opts :debug #{:thread?} "FINAL STATE OF" (:id parent-opts) "IS" (:status @state)))))))

; (def ^Delay file-name->number (fn-> (str/remove ".mp4") num/int+))

; (defn download-videos!
;   ([at-a-time] (download-videos! at-a-time nil))
;   ([at-a-time {:keys [print? no-check? meet-ids]}]
;     (when-not no-check?
;       (deref (future (update-results!)) 3000 nil))
;     (let [meet-ids
;            (if meet-ids
;                (zipmap meet-ids (gets @table meet-ids))
;                (->> (available-videos)
;                     (coll/take+ at-a-time) redm))]
;       (doseq [vid-id vid-meta meet-ids]
;         (log/pr :debug "Initing video" vid-id)
;         (let [meet-name     (:desc vid-meta)
;               url           (streaming-url vid-id)
;               wmv-file      (io/file-str [:resources "SIRE" (str vid-id ".wmv")])
;               mp4-file      (io/file-str [:resources "SIRE" (str vid-id ".mp4")])
;               thread-id     (str/keyword+ :newmarket-video-num- (str vid-id))
;               downloading-id (thread/gen-proc-id nil thread-id :ffmpeg-download)
;               converting-id  (thread/gen-proc-id nil thread-id :ffmpeg-convert)
;               error-map {:file {:downloading wmv-file
;                                 :converting  mp4-file}}
;               any-error-handler
;                 (fn [type]
;                   (fn handler
;                     ([state] (log/pr :debug "ANY ERROR HANDLER") (handler state nil))
;                     ([state e]
;                       (log/pr :debug "ANY ERROR HANDLER")
;                       (io/delete! :path (get-in error-map [:file type]))
;                       (thread/close-impl! (-> @thread/reg-threads (get downloading-id) :thread)))))
;               close-reqs (LinkedBlockingQueue.)
;               state (atom {})
;               downloading-handlers
;                 {:error/any.post (any-error-handler :downloading)
;                  :error/http.404-not-found
;                    (fn [state e] (println "404 not found")
;                             #_(thread/close! thread-id))
;                  :error/connection-lost     (fn [state e]    (thread/close-impl! (-> @thread/reg-threads (get downloading-id) :thread)))
;                  :error/timeout (fn [state e]  (log/pr :debug "IN TIMEOUT HANDLER WITH THREAD" (-> @thread/reg-threads (get downloading-id) :thread))  (thread/close-impl! (-> @thread/reg-threads (get downloading-id) :thread))) ; well this doesn't actually work
;                  :output-timeout (fn [state sleep-time stream-source] (@output-timeout-handler state sleep-time stream-source))
;                  :output-line    (fn [state line       stream-source] (@output-line-handler    state line       stream-source))}
;               downloading-block
;                 {:status {:init :downloading :post :downloaded}
;                  :handlers downloading-handlers
;                  :id       downloading-id
;                  :proc 
;                    #(do (assoc! workers vid-id :downloading)
;                         (download!
;                           {:type       :wmv
;                            :state      state
;                            :file       wmv-file
;                            :id         downloading-id
;                            :url        url
;                            :parent     thread-id
;                            :close-reqs close-reqs
;                            :handlers   downloading-handlers}))}
;               converting-handlers
;                 {:error/any      (any-error-handler :converting)
;                  :error/timeout (fn [state e] (-> @thread/reg-threads (get converting-id) :thread))
;                  :output-line    (fn [state line       stream-source] (@output-line-handler    state line       stream-source))}
;               converting-block
;                 {:status {:init :converting :post :converted}
;                  :handlers converting-handlers
;                  :id       converting-id
;                  :proc
;                    #(do (assoc! workers vid-id :converting)
;                         (wmv->mp4 wmv-file mp4-file
;                           {:id         converting-id
;                            :parent     thread-id
;                            :close-reqs close-reqs
;                            :state      state
;                            :handlers   converting-handlers}))}]
;           (async-chain
;             {:state state
;              :id         thread-id
;              :close-reqs close-reqs
;              :result-set table
;              :result-id  vid-id
;              :finally    #(do (swap! workers dissoc vid-id))}
;             (when-not (-> @table (get vid-id) :status (= :downloaded))
;               downloading-block)
;             converting-block))))))

; (defonce process-limit (atom nil))

; (defn process-at-a-time!
;   ([n] (process-at-a-time! n false))
;   ([n force?]
;     (let [orig-process-limit? @process-limit]
;       (reset! process-limit n)
;       (when (or (not orig-process-limit?) force?)
;         (log/pr :debug "Spawning processor")
;         (async-loop
;           {:id :video-processor}
;           []
;           (when (nempty? (available-videos))
;             (when (-> @workers count (< @process-limit))
;               (download-videos! (- @process-limit (count @workers))))
;             (async/sleep 500)  ; check every half second
;             (recur)))))))

; (defn convert!
;   ([n & xs]
;     (doseq [x (cons n xs)]
;       (convert! x)))
;   ([n]
;   (let [thread-id (str/keyword+ "convert" n)
;         proc-id   (str/keyword+ "ffmpeg-convert" n)
;         mp4-path  [:resources "SIRE" (str n ".mp4")]
;         state     (atom {:id proc-id :parent thread-id})]
;     (async
;       {:id thread-id
;        :type :thread
;        :handlers {:closed #(io/delete! :path mp4-path)}}
;       (wmv->mp4 [:resources "SIRE" (str n ".wmv")]
;                 mp4-path
;         {:read-streams? true 
;          :threads 1
;          :state state
;          :handlers
;            {:output-line
;              (fn [state line stream-source]
;                (@output-line-handler state line stream-source))}
;          :id proc-id})))))

