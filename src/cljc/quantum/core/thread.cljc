(ns
  ^{:doc "Complex thread management made simple.
          Aliases core.async for convenience."
    :attribution "Alex Gunnarson"}
  quantum.core.thread
  (:require-quantum [ns num fn str err macros logic vec coll log res core-async async])
  (:require
    #?@(:clj [[clojure.core.async.impl.ioc-macros      :as ioc             ]
              [clojure.core.async.impl.exec.threadpool :as async-threadpool]
              [co.paralleluniverse.pulsar.core         :as pulsar          ]
              [co.paralleluniverse.pulsar.async        :as pasync          ]]))
  #?(:clj (:import (java.lang Thread Process)
            (java.util.concurrent Future Executor ExecutorService ThreadPoolExecutor)
            quantum.core.data.queue.LinkedBlockingQueue
            (co.paralleluniverse.fibers FiberScheduler DefaultFiberScheduler))))

#?(:clj (defonce reg-threads (atom {}))) ; {:thread1 :open :thread2 :closed :thread3 :close-req}
#?(:clj (defn thread-ids [] (-> reg-threads deref keys sort)))
#?(:clj (defonce reg-threads-tree (atom {})))

(defn wrap-delay [f]
  (if (delay? f) f (delay ((or f fn-nil)))))

#?(:clj
(defn add-child-proc! [parent id] ; really should lock reg-threads and reg
  {:pre [(with-throw
           (containsk? @reg-threads parent)
           {:message (str/sp "Parent thread" parent "does not exist.")})]}
  (log/pr ::debug "Adding child proc" id "to parent" parent)
  ; Add to tree
  #_(let [reg-threads-view      @reg-threads
        traversal-keys
          (loop [tk-f [parent] ; technically should be a deque
                 parent-n parent]
            (if-let [parent-n+1 (get-in reg-threads-view [parent-n :parent])]
              (recur (conjl tk-f parent) parent-n+1)
              tk-f))
        path (get-in @reg-threads-tree traversal-keys)]
    (swap! reg-threads-tree assoc-in (conjr traversal-keys id) {})
    (swap! reg-threads      assoc-in [id :parents] traversal-keys))
  ; Add to flat
  (if (-> @reg-threads parent (containsk? :children))
      (swap! reg-threads update-in [parent :children] conj id)
      (swap! reg-threads assoc-in  [parent :children] #{id}))))

#?(:clj
(defn register-thread! [{:keys [id thread handlers parent] :as opts}]
  (if (containsk? @reg-threads id)
      (log/pr ::warn "Attempted to register existing thread:" id)
      (do (log/pr ::debug "REGISTERING THREAD" id)
          (swap! reg-threads assoc id (dissoc opts :id))
          (when parent (add-child-proc! parent id))
          opts))))

#?(:clj
(defn deregister-thread! [id]
  (if (containsk? @reg-threads id)
      (let [{:keys [parent parents] :as opts} id]
        (log/pr ::debug "DEREGISTERING THREAD" id)
        (swap! reg-threads
          (fn-> (dissoc id)
                (whenp parent (f*n dissoc-in+ [parent :children id]))))
        (swap! reg-threads-tree dissoc-in+ [parents id])))
      (log/pr ::warn "Attempted to deregister nonexistent thread:" id)))

#?(:clj (def ^{:dynamic true} *thread-num* (.. Runtime getRuntime availableProcessors)))
; Why you want to manage your threads when doing network-related things:
; http://eng.climate.com/2014/02/25/claypoole-threadpool-tools-for-clojure/
#?(:clj
  (defmacro thread+
    "Execute exprs in another thread and returns the thread."
    ^{:attribution "Alex Gunnarson"}
    [{:keys [id handlers]} expr & exprs]
    `(let [ns-0# *ns*
           pre#
             (and
               (with-throw
                 (keyword? ~id)
                 {:message "Thread-id must be a keyword."})
               (with-throw
                 ((fn-not core/contains?) @reg-threads ~id)
                 {:type    :key-exists
                  :message (str "Thread id '" (name ~id) "' already exists.")}))
           ^Atom result#
             (atom {:completed false
                    :result   nil})
           ^Thread thread#
             (Thread.
               (fn []
                 (try
                   (swap! reg-threads assoc-in [~id :state] :open)
                   (binding [*ns* ns-0#]
                     (swap! result# assoc :result
                       (do ~expr ~@exprs))
                     (swap! result# assoc :completed true))
                   (finally
                     (deregister-thread! ~id)
                     (log/pr :user "Thread" ~id "finished running.")))))]
       (.start thread#)
       (swap! reg-threads coll/assocs-in+
          [~id :thread] thread#
          [~id :handlers] ~handlers)
       result#)))

#?(:clj
(defn ^:private interrupt!* [thread thread-id interrupted force?]
  (if-not force?
    (do (log/pr ::debug "Before interrupt handler for id" thread-id)
        ((or interrupted fn-nil) thread)
        (log/pr ::debug "After interrupt handler for id" thread-id))
    (do (async/interrupt! thread)
        (if ((fn-or async/interrupted? async/closed?) thread)
            ((or interrupted fn-nil) thread)
            (throw+ {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be interrupted.")}))))))

#?(:clj
(defn+ ^:private ^:suspendable close!* [thread thread-id close-reqs cleanup force?]
  (let [max-tries 3]
    (log/pr ::debug "Before close request for id" thread-id)
    (when (and close-reqs (not (async/closed? close-reqs)))
      (async/put!! close-reqs :req)) ; it calls its own close-request handler
    (log/pr ::debug "After close request for id" thread-id)
    (force cleanup) ; closes message queues, releases other resources
                    ; that can/should be released before full termination
    (log/pr ::debug "After cleanup for id" thread-id)
    (when force?
      (async/close! thread) ; force shutdown
      (loop [tries 0]
        (log/pr :debug "Trying to close thread" thread-id)
        (when (and (async/open? thread) (<= tries max-tries))
          (log/pr :debug {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be closed.")})
          (async/sleep 100) ; wait a little bit for it to stop
          (recur (inc tries))))))))

#?(:clj
(defn close!
  "Closes a thread, possibly gracefully."
  {:attribution "Alex Gunnarson"}
  ([^Keyword thread-id] (close! thread-id nil)) ; don't force
  ([^Keyword thread-id {:keys [force?] :as opts}]
    (if-not (containsk? @reg-threads thread-id)
      (log/pr ::warn (str/sp "Thread-id" thread-id "does not exist; attempted to be closed."))
      (let [{:keys [thread children close-reqs]
             {:keys [interrupted cleanup]} :handlers} ; close-req is another, but it's called asynchronously 
              (get @reg-threads thread-id)]
        (doseq [child children]
          (close! child opts)) ; close children before parent
        (log/pr ::debug "After closing children" children "of" thread-id)
        (interrupt!* thread thread-id interrupted force?)
        (close!*     thread thread-id close-reqs cleanup force?)))))) ; closed handler is called by the thread reaper when it's actually close

(defn force-close-threads! [pred]
  (->> @reg-threads
       (coll/filter-keys+ pred)
       redm
       vals
       (map :thread)
       (map #(.cancel % true))))

#?(:clj
(defn close-all!
  {:todo ["Make dependency graph" "Incorporate into thread-reaper"]}
  ([] (close-all! false))
  ([force?]
    #_(identity ;while ((fn-and map? nempty?) @reg-threads-tree)
      (postwalk
        (fn [id]
          (let [traversal-keys (get-in @reg-threads [id :parents])]
            (try+ (close! id {:force? force?})
              (catch [:type :nonexistent-thread] {:keys [type]}
                (when-not (= type :nonexistent-thread) (throw+))))
            (if traversal-keys
                (swap! reg-threads-tree dissoc-in+ [traversal-keys id])
                (swap! reg-threads-tree dissoc id))
          ))
        @reg-threads-tree))
    (doseq [thread-id thread-meta @reg-threads]
      (close! thread-id {:force? force?})))))

(defn close-all-alt!
  "An alternative version of |close-all|. Test both."
  []
  (doseq [k v (->> @reg-threads (remove+ (compr key (eq? :thread-reaper))) redm)]
    (when-let [close-reqs (:close-reqs v)]
      (put!! close-reqs :req))
    (when-let [thread- (:thread v)]
      (async/close! thread-))))

(declare reg-threads)
(defn close-all-forcibly!
  "A working version of |close-all|."
  []
  (->> @reg-threads
       (<- dissoc :thread-reaper)
       (map (fn-> val :thread (.cancel true)))
       dorun))

#?(:clj
(defonce add-thread-shutdown-hooks!
  (-> (Runtime/getRuntime) (.addShutdownHook (Thread. (close-all! true))))))
 
; ASYNC

(def rejected-execution-handler
  (atom (fn [f] (log/pr ::debug "Function rejected for execution!" f))))


#?(:clj
(defonce threadpools
  (atom {:core.async ^ThreadPoolExecutor clojure.core.async.impl.exec.threadpool/the-executor
         :future     ^ThreadPoolExecutor clojure.lang.Agent/soloExecutor
         :agent      ^ThreadPoolExecutor clojure.lang.Agent/pooledExecutor
         :async      ^FiberScheduler     (DefaultFiberScheduler/getInstance) ; (-> _ .getExecutor) is ForkJoinPool / ExecutorService
         :reducers   ^ForkJoinPool       quantum.core.reducers/pool})))

(.setRejectedExecutionHandler ^ThreadPoolExecutor (:core.async @threadpools)
  (reify java.util.concurrent.RejectedExecutionHandler
    (^void rejectedExecution [this ^Runnable f ^ThreadPoolExecutor executor]
      (@rejected-execution-handler f))))

(defnt set-max-threads!
  [^java.util.concurrent.ThreadPoolExecutor threadpool-n n]
  (doto threadpool-n
    (.setCorePoolSize    n)
    (.setMaximumPoolSize n)))

(defn gen-threadpool [type num-threads & [name-]]
  (condp = type
    :fixed     (set-max-threads! 
                 (java.util.concurrent.Executors/newFixedThreadPool num-threads)
                 num-threads) 
    :fork-join (co.paralleluniverse.fibers.FiberForkJoinScheduler.
                 (or name- (name (gensym))) num-threads nil
                 co.paralleluniverse.common.monitoring.MonitorType/JMX false)))

(defn clear-work-queue! [^ThreadPoolExecutor threadpool-n]
  (->> threadpool-n .getQueue (map #(.cancel ^Future % true)) dorun)
  (->> threadpool-n .purge))

#?(:clj
(defn ^:internal closeably-execute [threadpool-n ^Runnable r {:keys [id] :as opts}]
  (when (register-thread! (merge opts {:thread false}))
    (try
      (let [^Future future-obj
             (.submit ^ExecutorService threadpool-n r)]
        (swap! reg-threads assoc-in [id :thread] future-obj)
        true)
      (catch Throwable e ; what exception?
        (log/pr :warn "Error in submitting to threadpool" e)
        (deregister-thread! id)
        false)))))

#?(:clj
(defmacro ^:internal async-chan*
  [opts & body]
  `(let [c# (chan :casync 1)
         captured-bindings# (clojure.lang.Var/getThreadBindingFrame)]
     (closeably-execute
       (or (:threadpool ~opts) (:core.async @threadpools))
       (fn []
         (let [f# ~(ioc/state-machine `(do ~@body) 1 (keys &env) ioc/async-custom-terminators)
               state# (-> (f#)
                          (ioc/aset-all! ioc/USER-START-IDX c#
                                         ioc/BINDINGS-IDX captured-bindings#))]
           (ioc/run-state-machine-wrapped state#)))
       ~opts)
     c#)))

(defn gen-proc-id [id parent name-]
  (if id id
    (keyword
      (gensym
        (cond
          (and parent name-)
            (str (name parent) "$" (name name-) ".")
          name-
            (str "$" (name name-) ".")
          parent
            (str (name parent) "$")
          :else
            "")))))

#?(:clj (defrecord f->chan-exc [^Throwable exc]))

(defn f->chan
  [c f & args]
  (pulsar/sfn []
    ; TODO can't simply use finally (as in core.async) because it triggers an instrumentation problem in do-alts!
    (let [ret (try (apply f args)
                (catch Throwable t
                  (->f->chan-exc t)))]
      (when-not (or (nil? ret) (instance? f->chan-exc ret))
        (async/>!! c ret))
      (close! c)
      (when (instance? f->chan-exc ret)
        (throw (:exc ret))))))

(defmacro async-fiber*
  ; The -jdk8 specification is 3x slower — benchmarked using their benchmarker
  {:benchmarks
    '{(dotimes [n 10000] (async {:id (gensym)} (async/sleep 2000) 123))
        2064
      (dotimes [n 10000] (async {:id (gensym)} (async/sleep 1000) 123))
        1429
      (dotimes [n 10000] (async {:id (gensym)}                    123))
        1464}}
  [opts async-fn & args]
  `(let [opts# ~opts
         c# (chan 1)
        ; It would be nice to wrap the fn because marking suspendable, etc. requires bytecode manipulation 
        ; and the shorter the fn, the less the bytecode and the faster the process
        ; Otherwise can take at least 1000 ms 
        ; But it causes instrumentation errors
        ; f (fn [] (async-fn))
        fiber# (pulsar/spawn-fiber
                :name       (name (:id         opts#))
                :stack-size (or   (:stack-size opts#) -1)
                :scheduler  (:threadpool opts#)
                (f->chan c#
                  ; If it's not marked as suspendable, it strangely (!) executes twice...
                  (pulsar/suspendable! ~async-fn)
                  ~@args))]
    
    (condp = (:ret opts#)
      :chan   c#
      :future (pulsar/fiber->future fiber#))))

(defn+ ^:suspendable gen-async-fn
  "This fn exists in part because it contains all the code
   that would normally take ~1000ms to bytecode-transform into suspendableness.
   This way it is only so transformed once."
  [body-fn {:keys [type id] :as opts}]
  #?(:clj
  (when (= type :thread)
    (.setName (Thread/currentThread) (name id))))

  (swap! reg-threads assoc-in [id :state] :running)
  (try+ (body-fn)
    (catch Object e
      ((or (-> opts :handlers :err/any.pre ) fn-nil))
      (log/pr-opts :warn #{:thread?} "Exited with exception" e)
      ((or (-> opts :handlers :err/any.post) fn-nil)))
    (finally 
      (log/pr-opts :quantum.core.thread/debug #{:thread?} "COMPLETED.")
      (when (get @reg-threads id)
        (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLEANING UP" id)
        (doseq [child-id (get-in @reg-threads [id :children])]
          (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLOSING CHILD" child-id "OF" id "IN END-THREAD")
          (close! child-id)
          (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLOSING CHILD" child-id "OF" id "IN END-THREAD"))
        (force (-> opts :handlers :cleanup)) ; Performed only once
        (swap! reg-threads assoc-in [id :state] :closed)
        (deregister-thread! id)))))

#?(:clj
(defmacro gen-async-opts
  "Generates options map for |async|."
  [opts & body]
  (let [proc-id        (gensym "proc-id")
        close-req-call (gensym "close-req-call")
        close-reqs     (gensym "close-reqs")
        cleanup-f      (gensym "cleanup-f")
        opts-f         (gensym "opts-f")]
   `(let [~opts-f ~opts ; so you create it only once
          asserts# (throw-unless (map? ~opts-f) "@opts must be a map.")
          ; TODO Proper destructuring in a macro would be nice... oh well...
          close-req#   (-> ~opts-f :handlers :close-req)
          cleanup#     (-> ~opts-f :handlers :cleanup)
          id#              (:id          ~opts-f)
          type#        (or (:type        ~opts-f) :fiber) ; Heavyweightness should be explicit
          _#           (throw-unless (in? type# #{:fiber :thread})
                         (Err. nil ":type must be in #{:fiber :thread}" type#))
          ret#         (or (:ret         ~opts-f) :chan)
          _#           (throw-unless (in? ret# #{:chan :future})
                         (Err. nil ":ret must be in #{:chan :future}" ret#))
          parent#          (:parent      ~opts-f)
          name#            (:name        ~opts-f)
          cleanup-seq#     (:cleanup-seq ~opts-f)
          asserts#
            (do (when cleanup#
                  (throw-unless (fn? cleanup#) "@cleanup must be a function."))
                (when cleanup-seq#
                  (throw-unless (instance? clojure.lang.IAtom cleanup-seq#)
                    "@cleanup-seq must be an atom.")))
          ~cleanup-f
            (if cleanup#
                (wrap-delay cleanup#)
                (when cleanup-seq#
                  (delay
                    (doseq [f# @cleanup-seq#]
                      (try+ (f#) 
                        (catch Object e#
                          (log/pr-opts :quantum.core.thread/warn #{:thread?} "Exception in cleanup:" e#)))))))
          ~close-req-call (wrap-delay close-req#)
          ~close-reqs (or (:close-reqs ~opts-f) (chan :queue))
          ~proc-id   (gen-proc-id id# parent# name#)
          async-body-fn# (if (= type# :fiber)
                             (pulsar/suspendable! (fn [] ~@body))
                             (fn [] ~@body))
          ~opts-f    (coll/assocs-in+         ~opts-f
                       [:close-reqs         ] ~close-reqs
                       [:id                 ] ~proc-id
                       [:handlers :close-req] ~close-req-call
                       [:handlers :cleanup  ] ~cleanup-f
                       [:ret                ] ret#
                       [:type               ] type#
                       [:body-fn            ] async-body-fn#)]
      ~opts-f))))

(defmacro async
  "Creates a closeable thread or 'fiber'.
   Options:
     :type #{:thread :fiber }
     :ret  #{:chan   :future}
     :threadpool — #{^ForkJoinPool
                     ^ThreadPoolExecutor}
     :id
     :handlers


   Handlers: (maybe make certain of these callable once?) 
     For all threads:
     :close-req    — Called when another thread requests to close
                     (adds an obj to the close-reqs queue)  
                     Somewhat analogous to :interrupted.
     :closed       — Called by the thread reaper when the thread is fully closed.
                     Not called if the thread completes without a close request.
     :completed (Planned) 
                   — Called when the thread runs to completion.
                     :completed and :closed may both be called.
     :exception   
       — :default  — Called on early termination (exit code not= 0)
     :error 
       — (Various) — Non-exceptions (not thrown)
                     Not automatically called.
                     Grouped with handlers for organization.
     For native processes:  
     :output-line  — Called when a new output line is available
                     or on timeout (default 2000 ms).

   Already handled:
     :cleanup (via |with-cleanup| on @cleanup-seq)"
  [opts & body]
  `(let [opts# ~opts
         opts-f# (gen-async-opts opts# ~@body)]
     (condp = (:type opts-f#)
        :fiber
          (async-fiber* opts-f# gen-async-fn (:body-fn opts-f#) opts-f#)
        :thread ; TODO assumes ret is chan
          (async-chan* opts-f#
            (gen-async-fn (:body-fn opts-f#) opts-f#)))))
  
#?(:clj
(defmacro async-loop
  "Like |go-loop| but inherits the additional flexibility
   that |async| provides."
  [opts bindings & body]
  (let [close-reqs-f   (gensym)
        close-req-call (gensym)]
   `(let [opts-f# ~opts
          ~close-reqs-f   (or (:close-reqs opts-f#) (chan :queue))
          ~close-req-call (wrap-delay (-> opts-f# :handlers :close-req))]
      (async (coll/assocs-in+ ~opts
                [:close-reqs         ] ~close-reqs-f
                [:handlers :close-req] ~close-req-call)
        (try
          (loop ~bindings
            (when (empty? ~close-reqs-f)
              (do ~@body)))
          (finally
            (when (nempty? ~close-reqs-f)
              (force ~close-req-call)))))))))

(defn proc-chain
  "To chain subprocesses together on the same thread.
   To track progress, etc. on phases of completion."
  [universal-opts thread-chain-template]
  (throw+ {:msg "Unimplemented"}))

#?(:clj
(defn reap-threads! []
  (doseq [id thread-meta @reg-threads]
    (let [{:keys [thread state handlers]} thread-meta]
      (when (or (async/closed? thread)
                (= state :closed))
        (whenf (:closed handlers) nnil?
          (if*n delay? force call))
        (deregister-thread! id))))))

#?(:clj (defonce thread-reaper-pause-requests  (LinkedBlockingQueue.)))
#?(:clj (defonce thread-reaper-resume-requests (LinkedBlockingQueue.)))

#?(:clj
(defonce thread-reaper
  (do #_(log/enable! :macro-expand)
      (with-do
        (let [id :thread-reaper]
          (async-loop
            {:id id
             :handlers {:close-req #(swap! reg-threads dissoc id) ; remove itself
                        :closed    #(log/pr :debug "Thread-reaper has been closed.")}} 
            []
            (if (nempty? thread-reaper-pause-requests)
                (do (async/empty! thread-reaper-pause-requests)
                    (log/pr-opts :debug #{:thread?} "Thread reaper paused.")
                    (swap! reg-threads assoc-in [id :state] :paused)
                    (take!! thread-reaper-resume-requests) ; blocking take
                    (async/empty! thread-reaper-resume-requests)
                    (log/pr-opts :debug #{:thread?} "Thread reaper resumed.")
                    (swap! reg-threads assoc-in [id :state] :running)
                    (recur))
                (do (reap-threads!)
                    (async/sleep 2000)
                    (recur)))))
        #_(log/disable! :macro-expand)))))

#?(:clj (defn pause-thread-reaper!  [] (put!! thread-reaper-pause-requests true)))
#?(:clj (defn resume-thread-reaper! [] (put!! thread-reaper-resume-requests true)))

#_(defonce gc-worker
  (async {:id :gc-collector}
    (loop [] (System/gc) (async/sleep 60000))))

; ===============================================================
; ============ TO BE INVESTIGATED AT SOME LATER DATE ============

#?(:clj
  (defn promise-concur
    {:attribution "Alex Gunnarson"}
    [method max-threads func list-0]
    (let [count- (count list-0)
          chunk-size
            (if (= max-threads :max)
                count-
                (-> count- (/ max-threads) (num/round :type :up)))] ; round up from decimal chunks
      (loop [list-n list-0
             promises ()]
        (if (empty? list-n)
          promises
          (recur
            (drop chunk-size list-n)
            (conj promises
              (future ; one thread for each chunk
                (if (= method :for)
                    (doall (for [elem (take chunk-size list-n)]
                      (func elem)))
                    (doseq [elem (take chunk-size list-n)]
                      (func elem)))))))))))

#?(:clj
  (defn concur [method max-threads func list-0]
    (map deref (promise-concur method max-threads func list-0))))

#?(:clj
  (defn promise-concur-go [method max-threads func list-0]
    (let [count- (count list-0)
          chunk-size
            (if (= max-threads :max)
                count-
                (-> count- (/ max-threads) (num/round :type :up)))] ; round up from decimal chunks
      (loop [list-n list-0
             promises []]
        (if (empty? list-n)
          promises
          (recur
            (drop chunk-size list-n)
            (conj promises ; [[ct0 (chan0)] [ct1 (chan1)] [ct2 (chan2)]]
              (let [chan-0  (chan)
            chunk-n (take chunk-size list-n)
                    chunk-size-n (count chunk-n)]
                (go ; one go block / "lightweight thread pool" for each chunk
                  (clojure.core/doseq [elem chunk-n]
                    ;(println "(func elem):" (func elem))
                    (>! chan-0 (func elem)))) ; the thread blocks it anyway
                [chunk-size-n chan-0]))))))))

#?(:clj
  (defn concur-go
    {:attribution "Alex Gunnarson"}
    [method max-threads func list-0]
    (let [chans (promise-concur-go method max-threads func list-0)]
      (if (= method :for)
          (->> chans
               (map+ (compr #(doall
                               (for [n (range (first %))]
                                 (<!! (second %))))
                            vec))
               redv
               (apply catvec))
          (doseq [chan-n chans] chan-n)))))

#?(:clj
  (defn+ thread-or
    "Call each of the fs on a separate thread. Return logical
    disjunction of the results. Short-circuit (and cancel the calls to
    remaining fs) on first truthy value returned."
    ^{:attribution "Michal Marczyk - https://gist.github.com/michalmarczyk/5992795"}
    [& fs]
    (let [ret (promise)
          fps (promise)]
      (deliver fps
               (doall (for [f fs]
                        (let [p (promise)]
                          [(future
                             (let [v (f)]
                               (locking fps
                                 (deliver p true)
                                 (if v
                                   (deliver ret v)
                                   (when (every? realized? (map (extern (mfn 1 peek)) @fps))
                                     (deliver ret nil))))))
                           p]))))
      (let [result @ret]
        (doseq [[fut] @fps]
          (future-cancel fut))
        result))))
 
(comment
 
  ;; prints :foo, but not :bar
  (thread-or #(do (async/sleep 1000) (println :foo) true)
             #(do (async/sleep 3000) (println :bar)))
  ;;= true
 
  ;; prints :foo and :bar
  (thread-or #(do (async/sleep 1000) (println :foo))
             #(do (async/sleep 3000) (println :bar)))
  ;;= nil
 
  )
#?(:clj
  (defn+ thread-and
    "Computes logical conjunction of return values of fs, each of which
    is called in a future. Short-circuits (cancelling the remaining
    futures) on first falsey value."
    ^{:attribution "Michal Marczyk - https://gist.github.com/michalmarczyk/5991353"}
    [& fs]
    (let [done (promise)
          ret  (atom true)
          fps  (promise)]
      (deliver fps (doall (for [f fs]
                            (let [p (promise)]
                              [(future
                                 (if-not (swap! ret #(and %1 %2) (f))
                                   (deliver done true))
                                 (locking fps
                                   (deliver p true)
                                   (when (every? realized? (map (extern (mfn 1 peek)) @fps))
                                     (deliver done true))))
                               p]))))
      @done
      (doseq [[fut] @fps]
        (future-cancel fut))
      @ret)))


(comment
 
  (thread-and (constantly true) (constantly true))
  ;;= true
 
  (thread-and (constantly true) (constantly false))
  ;;= false
 
  (every? false?
          (repeatedly 100000
                      #(thread-and (constantly true) (constantly false))))
  ;;= true
 
  ;; prints :foo, but not :bar
  (thread-and #(do (async/sleep 1000) (println :foo))
              #(do (async/sleep 3000) (println :bar)))
 
  )


; (defn+ ^:suspendable do-intervals [millis & args]
;   (->> args
;        (interpose #(async/sleep millis))
;        (map+ fold+)
;        fold+
;        pr/suppress))
; (defn+ do-every ^:suspendable [millis n func]
;   (dotimes [_ n] (func) (async/sleep millis)))

; ;___________________________________________________________________________________________________________________________________
; ;========================================================{  CAPTURE SYS.OUT  }======================================================
; ;========================================================{                   }======================================================
; (defn update-out-str-with! [out-str baos]
;     (swap! temp-rec conj baos)
; ; (swap! out-str conj (str baos))
;   (let [baos-str-0 (str baos)]
;     (if (empty? baos-str-0)
;         nil
;         (let [baos-str-f (getr+ baos-str-0 0 (-> baos-str-0 count+ dec dec))]
;         (swap! out-str conj
;           (str/subs+ baos-str-f
;                (whenf (+ 2 (last-index-of+ "\r\n" baos-str-f))
;                  (eq? 1) (constantly 0)))))))) ; if it's the same, keep it
; (defmacro with-capture-sys-out [expr out-str & [millis n-times]]
;   `(let [baos# (java.io.ByteArrayOutputStream.)
;          ps#   (java.io.OutputStreamWriter. baos#)]
;     (binding [*out* ps#]
;       (deref (future ~expr)) ; will process in background
;       (do-every
;         (whenf ~millis  nil? (constantly 500))
;         (whenf ~n-times nil? (constantly 6))
;         #(update-out-str-with! ~out-str baos#)))))




; SHUT DOWN ALL FUTURES
; ; DOESN'T ACTUALLY WORK
; (import 'clojure.lang.Agent)
; (import 'java.util.concurrent.Executors) 
; (defn close-all-futures! []
;   (shutdown-agents)
;   (.shutdownNow Agent/soloExecutor)
;   (set! Agent/soloExecutor (Executors/newCachedThreadPool)))

; INVESTIGATE AGENTS...



(defn chunk-doseq
  "Like |fold| but for |doseq|.
   Also configurable by thread names and threadpool, etc."
  [coll {:keys [total thread-count chunk-size threadpool thread-name chunk-fn] :as opts} f]
  (let [total-f (or total (count coll))
        chunks (coll/partition-all (or chunk-size
                                       (/ total-f
                                          (min total-f (or thread-count 10))))
                 (if total (take total coll) coll))]
    (doseqi [chunk chunks i]
      (let [thread-id (keyword (str thread-name "-" i))]
        (async (mergel {:id thread-id} opts)
          ((or chunk-fn fn-nil) chunk i)
          (doseqi [piece chunk n]
            (f piece n chunk i chunks)))))))
