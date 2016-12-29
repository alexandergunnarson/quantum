(ns
  ^{:doc "Complex thread management made simple.
          Aliases core.async for convenience."
    :attribution "Alex Gunnarson"}
  quantum.core.thread
            (:refer-clojure :exclude [doseq boolean? memoize conj! assoc! empty?])
            (:require [clojure.core                          :as core]
            #?@(:clj [[clojure.core.async.impl.ioc-macros      :as ioc             ]
                      [clojure.core.async.impl.exec.threadpool :as async-threadpool]
                    #_[co.paralleluniverse.pulsar.core         :as pulsar          ]
                    #_[co.paralleluniverse.pulsar.async        :as pasync          ]])
                      [com.stuartsierra.component              :as component]
                      [quantum.core.string                     :as str             ]
                      [quantum.core.collections                :as coll
                        :refer [in? conj! assoc! empty? nempty? doseq dissoc-in+ nnil?]]
                      [quantum.core.numeric                    :as num]
                      #?(:clj [clojure.core.async :as casync])
                      [quantum.core.async                      :as async
                        :refer [put!! chan buffer go]]
                      [quantum.core.error                      :as err
                        :refer [throw-unless ->ex]]
                      [quantum.core.fn                         :as fn
                        :refer [<- fn-> fn1 rcomp call fn-nil fn$]]
                      [quantum.core.log                        :as log]
                      [quantum.core.logic                      :as logic
                        :refer [fn-or fn-not eq? whenf whenp whenf1 ifn1]                                             ]
                      [quantum.core.macros                     :as macros
                        :refer [defnt]]
                      [quantum.core.validate                   :as v
                        :refer [validate]]
                      [quantum.core.type                       :as type
                        :refer [boolean?]]
                      [quantum.core.time.core                  :as time]
                      [quantum.core.cache
                        :refer [memoize]])
  #?(:clj  (:import
             (java.lang Thread Process)
             (java.util.concurrent Future Executor Executors
                ExecutorService ThreadPoolExecutor)
             quantum.core.data.queue.LinkedBlockingQueue
             #_(co.paralleluniverse.fibers FiberScheduler DefaultFiberScheduler))))

; TO EXPLORE
; - Task manager using the ideas from here and from mikera/task:
;   - :source/:form, :promise, :creation-time
;   - Can print out a list of all running tasks in a pprint-table form
; ============================

(def wrap-delay (whenf1 (fn-not delay?) delay))

(defonce ^{:doc "Thread registry"} reg (atom {}))

(defn ids [] (-> reg deref keys sort))

(defonce reg-tree (atom {}))

#?(:clj
(defn add-child-proc! [parent id] ; really should lock reg-threads and reg
  {:pre [(throw-unless
           (contains? @reg parent)
           (->ex nil "Parent thread does not exist." parent))]}
  (log/pr ::debug "Adding child proc" id "to parent" parent)
  ; Add to tree
  #_(let [reg-view      @reg
        traversal-keys
          (loop [tk-f [parent] ; technically should be a deque
                 parent-n parent]
            (if-let [parent-n+1 (get-in reg-view [parent-n :parent])]
              (recur (conjl tk-f parent) parent-n+1)
              tk-f))
        path (get-in @reg-threads-tree traversal-keys)]
    (swap! reg-tree assoc-in (conjr traversal-keys id) {})
    (swap! reg      assoc-in [id :parents] traversal-keys))
  ; Add to flat
  (if (-> @reg parent (contains? :children))
      (swap! reg update-in [parent :children] conj id)
      (swap! reg assoc-in  [parent :children] #{id}))))

#?(:clj
(defn register-thread! [{:keys [id thread handlers parent] :as opts}]
  (if (contains? @reg id)
      (log/pr ::warn "Attempted to register existing thread:" id)
      (do (log/pr ::debug "REGISTERING THREAD" id)
          (swap! reg assoc id (dissoc opts :id))
          (when parent (add-child-proc! parent id))
          opts))))

#?(:clj
(defn deregister-thread! [id]
  (if (contains? @reg id)
      (let [{:keys [parent parents] :as opts} id]
        (log/pr ::debug "DEREGISTERING THREAD" id)
        (swap! reg
          (fn-> (dissoc id)
                (whenp parent (fn1 dissoc-in+ [parent :children id]))))
        (swap! reg-tree dissoc-in+ [parents id])))
      (log/pr ::warn "Attempted to deregister nonexistent thread:" id)))

#?(:clj (def thread-num (.. Runtime getRuntime availableProcessors)))

; Why you want to manage your threads when doing network-related things:
; http://eng.climate.com/2014/02/25/claypoole-threadpool-tools-for-clojure/
#(:clj
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
                 ((fn-not core/contains?) @reg ~id)
                 {:type    :key-exists
                  :message (str "Thread id '" (name ~id) "' already exists.")}))
           ^Atom result#
             (atom {:completed false
                    :result   nil})
           ^Thread thread#
             (Thread.
               (fn []
                 (try
                   (swap! reg assoc-in [~id :state] :open)
                   (binding [*ns* ns-0#]
                     (swap! result# assoc :result
                       (do ~expr ~@exprs))
                     (swap! result# assoc :completed true))
                   (finally
                     (deregister-thread! ~id)
                     (log/pr :user "Thread" ~id "finished running.")))))]
       (.start thread#)
       (swap! reg coll/assocs-in+
          [~id :thread] thread#
          [~id :handlers] ~handlers)
       result#)))

;#?(:clj
;(defn ^:private interrupt!* [thread thread-id interrupted force?]
;  (if-not force?
;    (do (log/pr ::debug "Before interrupt handler for id" thread-id)
;        ((or interrupted fn-nil) thread)
;        (log/pr ::debug "After interrupt handler for id" thread-id))
;    (do (async/interrupt! thread)
;        (if ((fn-or async/interrupted? async/closed?) thread)
;            ((or interrupted fn-nil) thread)
;            (throw+ {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be interrupted.")}))))))

; TODO for now
#?(:clj (def interrupt!* (fn [thread & args] thread)))
; TODO for now
#?(:clj (def close!*     (fn [thread & args] thread)))

;#?(:clj
;(defn+ ^:private ^:suspendable close!* [thread thread-id close-reqs cleanup force?]
;  (let [max-tries 3]
;    (log/pr ::debug "Before close request for id" thread-id)
;    (when (and close-reqs (not (async/closed? close-reqs)))
;      (async/put!! close-reqs :req)) ; it calls its own close-request handler
;    (log/pr ::debug "After close request for id" thread-id)
;    (force cleanup) ; closes message queues, releases other resources
;                    ; that can/should be released before full termination
;    (log/pr ::debug "After cleanup for id" thread-id)
;    (when force?
;      (async/close! thread) ; force shutdown
;      (loop [tries 0]
;        (log/pr :debug "Trying to close thread" thread-id)
;        (when (and (async/open? thread) (<= tries max-tries))
;          (log/pr :debug {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be closed.")})
;          (async/sleep 100) ; wait a little bit for it to stop
;          (recur (inc tries))))))))

#?(:clj
(defn close!
  "Closes a thread, possibly gracefully."
  {:attribution "Alex Gunnarson"}
  ([thread-id] (close! thread-id nil)) ; don't force
  ([thread-id {:keys [force?] :as opts}]
    (if-not (contains? @reg thread-id)
      (log/pr ::warn (str/sp "Thread-id" thread-id "does not exist; attempted to be closed."))
      (let [{:keys [thread children close-reqs]
             {:keys [interrupted cleanup]} :handlers} ; close-req is another, but it's called asynchronously
              (get @reg thread-id)]
        (doseq [child children]
          (close! child opts)) ; close children before parent
        (log/pr ::debug "After closing children" children "of" thread-id)
        (interrupt!* thread thread-id interrupted force?)
        (close!*     thread thread-id close-reqs cleanup force?)))))) ; closed handler is called by the thread reaper when it's actually close

; TEMP COMMENT
;(defn force-close-threads! [pred]
;  (->> @reg
;       (coll/filter-keys+ pred)
;       redm
;       vals
;       (map :thread)
;       (map #(.cancel % true))
;       doall))

#?(:clj
(defn close-all!
  {:todo ["Make dependency graph" "Incorporate into thread-reaper"]}
  ([] (close-all! false))
  ([force?]
    #_(identity ;while ((fn-and map? nempty?) @reg-tree)
      (postwalk
        (fn [id]
          (let [traversal-keys (get-in @reg-threads [id :parents])]
            (try+ (close! id {:force? force?})
              (catch [:type :nonexistent-thread] {:keys [type]}
                (when-not (= type :nonexistent-thread) (throw+))))
            (if traversal-keys
                (swap! reg-tree dissoc-in+ [traversal-keys id])
                (swap! reg-tree dissoc id))
          ))
        @reg-tree))
    (doseq [thread-id thread-meta @reg]
      (close! thread-id {:force? force?})))))

#?(:clj
(defn close-all-alt!
  "An alternative version of |close-all|. Test both."
  []
  (doseq [[k v] (->> @reg (remove (rcomp key (eq? :thread-reaper))) (into {}))]
    (when-let [close-reqs (:close-reqs v)]
      (put!! close-reqs :req))
    (when-let [thread- (:thread v)]
      (async/close! thread-)))))

#?(:clj
(defn close-all-forcibly!
  "A working version of |close-all|."
  []
  (->> @reg
       (<- dissoc :thread-reaper)
       (map (fn-> val :thread async/close!))
       dorun)))

#?(:clj
(defonce add-thread-shutdown-hooks!
  (-> (Runtime/getRuntime) (.addShutdownHook (Thread. #(close-all! true))))))

; ASYNC

#?(:clj
(def rejected-execution-handler
  (atom (fn [f] (log/pr ::debug "Function rejected for execution!" f)))))

#?(:clj
(defonce threadpools
  (atom {:core.async ^ThreadPoolExecutor (Executors/newFixedThreadPool
                                           @@#'clojure.core.async.impl.exec.threadpool/pool-size
                                           (clojure.core.async.impl.concurrent/counted-thread-factory "async-dispatch-%d" true))
         ; ^clojure.core.async.impl.protocols.Executor @clojure.core.async.impl.dispatch/executor ; because it's a Delay
         :future     ^ThreadPoolExecutor clojure.lang.Agent/soloExecutor
         :agent      ^ThreadPoolExecutor clojure.lang.Agent/pooledExecutor
         ; TODO commented temporarily
         ;:async      ^FiberScheduler     (DefaultFiberScheduler/getInstance) ; (-> _ .getExecutor) is ForkJoinPool / ExecutorService
         :reducers   ^ForkJoinPool       quantum.core.reducers.fold/pool})))

 #?(:clj
 (.setRejectedExecutionHandler ^ThreadPoolExecutor (:core.async @threadpools)
   (reify java.util.concurrent.RejectedExecutionHandler
     (^void rejectedExecution [this ^Runnable f ^ThreadPoolExecutor executor]
       (@rejected-execution-handler f)))))

#?(:clj
(defnt set-max-threads!
  [^java.util.concurrent.ThreadPoolExecutor threadpool-n n]
  (doto threadpool-n
    (.setCorePoolSize    n)
    (.setMaximumPoolSize n))))

#?(:clj
(defn gen-threadpool [type num-threads & [name-]]
  (condp = type
    :fixed     (set-max-threads!
                 (java.util.concurrent.Executors/newFixedThreadPool num-threads)
                 num-threads)
    :fork-join (co.paralleluniverse.fibers.FiberForkJoinScheduler.
                 (or name- (name (gensym))) num-threads nil
                 co.paralleluniverse.common.monitoring.MonitorType/JMX false))))

#?(:clj
(defn clear-work-queue! [^ThreadPoolExecutor threadpool-n]
  (->> threadpool-n .getQueue (map #(.cancel ^Future % true)) dorun)
  (->> threadpool-n .purge)))

#?(:clj
(defn ^:internal closeably-execute [threadpool-n ^Runnable r {:keys [id] :as opts}]
  (when (register-thread! (merge opts {:thread false}))
    (try
      (let [^Future future-obj
             (.submit ^ExecutorService threadpool-n r)]
        (swap! reg assoc-in [id :thread] future-obj)
        true)
      (catch Throwable e ; what exception?
        (log/pr :warn "Error in submitting to threadpool" e)
        (deregister-thread! id)
        false)))))

#?(:clj
(defmacro ^:internal async-chan*
  [opts & body]
  `(let [c# (chan 1)
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

#_(:clj
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
        (throw (:exc ret)))))))

#?(:clj
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
      :future (pulsar/fiber->future fiber#)))))

#?(:clj
(defn gen-async-fn ; defn+ ^:suspendable
  "This fn exists in part because it contains all the code
   that would normally take ~1000ms to bytecode-transform into suspendableness.
   This way it is only so transformed once."
  [body-fn {:keys [type id] :as opts}]
  #?(:clj
  (when (= type :thread)
    (.setName (Thread/currentThread) (name id))))

  (swap! reg assoc-in [id :state] :running)
  (try (body-fn)
    (catch Throwable e
      ((or (-> opts :handlers :err/any.pre ) fn-nil))
      (log/pr-opts :warn #{:thread?} "Exited with exception" e)
      ((or (-> opts :handlers :err/any.post) fn-nil)))
    (finally
      (log/pr-opts :quantum.core.thread/debug #{:thread?} "COMPLETED.")
      (when (get @reg id)
        (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLEANING UP" id)
        (doseq [child-id (get-in @reg [id :children])]
          (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLOSING CHILD" child-id "OF" id "IN END-THREAD")
          (close! child-id)
          (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLOSING CHILD" child-id "OF" id "IN END-THREAD"))
        (force (-> opts :handlers :cleanup)) ; Performed only once
        (swap! reg assoc-in [id :state] :closed)
        (deregister-thread! id))))))

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
          type#        (or (:type        ~opts-f) #_:fiber :thread) ; Heavyweightness should be explicit
          _#           (throw-unless (in? type# #{#_:fiber :thread})
                         (->ex nil ":type must be in #{:fiber :thread}" type#))
          ret#         (or (:ret         ~opts-f) :chan)
          _#           (throw-unless (in? ret# #{:chan :future})
                         (->ex nil ":ret must be in #{:chan :future}" ret#))
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
                      (try (f#)
                        (catch Throwable e#
                          (log/pr-opts :quantum.core.thread/warn #{:thread?} "Exception in cleanup:" e#)))))))
          ~close-req-call (wrap-delay close-req#)
          ~close-reqs (or (:close-reqs ~opts-f) (async/chan :queue))
          ~proc-id   (gen-proc-id id# parent# name#)
          async-body-fn# (if (= type# :fiber)
                             (fn [] ~@body)
                             #_(pulsar/suspendable! (fn [] ~@body))
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

#?(:clj
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
        #_:fiber
          #_(async-fiber* opts-f# gen-async-fn (:body-fn opts-f#) opts-f#)
        :thread ; TODO assumes ret is chan
          (async-chan* opts-f#
            (gen-async-fn (:body-fn opts-f#) opts-f#))))))

#?(:clj
(defmacro async-loop
  "Like |go-loop| but inherits the additional flexibility
   that |async| provides."
  [opts bindings & body]
  (let [close-reqs-f   (gensym)
        close-req-call (gensym)]
   `(let [opts-f# ~opts
          ~close-reqs-f   (or (:close-reqs opts-f#) (async/chan :queue))
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

#?(:clj
(defn proc-chain
  "To chain subprocesses together on the same thread.
   To track progress, etc. on phases of completion."
  [universal-opts thread-chain-template]
  (throw (->ex :not-implemented))))

#?(:clj
(defn reap-threads! []
  (doseq [id thread-meta @reg]
    (let [{:keys [thread state handlers]} thread-meta]
      (when (or (async/closed? thread)
                (= state :closed))
        (whenf (:closed handlers) nnil?
          (ifn1 delay? force call))
        (deregister-thread! id))))))

#?(:clj (defonce thread-reaper-pause-requests  (LinkedBlockingQueue.)))
#?(:clj (defonce thread-reaper-resume-requests (LinkedBlockingQueue.)))

#?(:clj
(defonce thread-reaper
  (do #_(log/enable! :macro-expand)
      (fn/with-do
        (let [id :thread-reaper]
          (async-loop
            {:id id
             :handlers {:close-req #(swap! reg dissoc id) ; remove itself
                        :closed    #(log/pr :debug "Thread-reaper has been closed.")
                        :type      :thread}}
            []
            (if (nempty? thread-reaper-pause-requests)
                (do (async/empty! thread-reaper-pause-requests)
                    (log/pr-opts :debug #{:thread?} "Thread reaper paused.")
                    (swap! reg assoc-in [id :state] :paused)
                    (async/take!! thread-reaper-resume-requests) ; blocking take
                    (async/empty! thread-reaper-resume-requests)
                    (log/pr-opts :debug #{:thread?} "Thread reaper resumed.")
                    (swap! reg assoc-in [id :state] :running)
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

#_(:clj
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

#_(:clj
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

#_(:clj
(defn concur-go
  {:attribution "Alex Gunnarson"}
  [method max-threads func list-0]
  (let [chans (promise-concur-go method max-threads func list-0)]
    (if (= method :for)
        (->> chans
             (map+ (rcomp #(doall
                             (for [n (range (first %))]
                               (<!! (second %))))
                          vec))
             redv
             (apply catvec))
        (doseq [chan-n chans] chan-n)))))

#_(:clj
(defn+ thread-or
  "Call each of the fs on a separate thread. Return logical
  disjunction of the results. Short-circuit (and cancel the calls to
  remaining fs) on first truthy value returned."
  {:attribution "Michal Marczyk - https://gist.github.com/michalmarczyk/5992795"
   :tests '{(let [x (atom nil)]
              (thread-or #(do (async/sleep 1000) (swap! x conj :a) true)
                         #(do (async/sleep 3000) (swap! x conj :b) true))
              (Thread/sleep 3500)
              @x)
            [:a]
            (let [x (atom nil)]
              (thread-or #(do (async/sleep 1000) (swap! x conj :a) false)
                         #(do (async/sleep 3000) (swap! x conj :b) true))
              (Thread/sleep 3500)
              @x)
            [:a :b]}}
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

#_(:clj
  (defn+ thread-and
    "Computes logical conjunction of return values of @fs, each of which
    is called in a future. Short-circuits (cancelling the remaining
    futures) on first falsey value."
    {:attribution "Michal Marczyk - https://gist.github.com/michalmarczyk/5991353"
     :tests '{(thread-and (constantly true) (constantly true))
                true
              (thread-and (constantly true) (constantly false))
                false
              (thread-and #(do (async/sleep 1000) :foo)
                          #(do (async/sleep 3000) :bar))
               :foo}}
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



#_(:clj
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
            (f piece n chunk i chunks))))))))

; ===== DISTRIBUTOR =====

(defrecord Distributor
  [name
   work-queue
   cache
   max-threads
   thread-registrar
   threadpool
   distributor-fn
   interrupted?
   logging-key
   log]
  component/Lifecycle (stop [this] (reset! interrupted? true) this))

; TODO move
#?(:clj
(defnt shutdown!
  [^java.util.concurrent.ThreadPoolExecutor x]
  (.shutdownNow x)))

#?(:clj
(defn ->distributor
  {:usage '(->distributor inc {:cache true
                               :memoize-only-first-arg? true
                               :max-threads 8 :name "distrib"})
   :todo ["Add thread types options"
          "Add validators to component atoms"
          "Register distributor and ensure uniquity"]}
  [f {:keys [cache memoize-only-first-arg? threadpool max-threads
             max-work-queue-size name logging-key] :as opts}]
  (validate max-threads         (v/or* nil? integer?)
            max-work-queue-size (v/or* nil? integer?)
            name                (v/or* nil? string? )
            threadpool          (v/or* nil? (fn$ instance? ThreadPoolExecutor))
            f                   fn?
            logging-key         (v/or* nil? keyword?))
  (let [cache-f        (if (true? cache)
                           (atom {})
                           cache) ; TODO bounded or auto-invalidating cache?
        log            (atom [])
        distributor-fn (atom (if cache-f
                                 (memoize f cache-f memoize-only-first-arg?) ; It doesn't cache errors, by default
                                 f))
        name-f         (or name (-> "distributor" gensym core/name keyword))
        max-threads-f  (or max-threads (-> (Runtime/getRuntime) (.availableProcessors)))
        thread-registrar (atom {})
        work-queue     (if max-work-queue-size
                          (chan (buffer max-work-queue-size))
                          (chan)) ; Unbounded queues don't factor in to core.async
        threadpool-f   (atom (or threadpool (gen-threadpool :fixed max-threads-f)))
        threadpool-interrupted?
          (doto (atom false)
            (set-validator! #(boolean? %))
            (add-watch :interrupt-monitor
              (fn [_ _ _ newv]
                (when (true? newv)
                  (err/suppress (shutdown! @threadpool-f))))))
        distributor-f  (Distributor.
                         name-f
                         work-queue
                         cache-f
                         max-threads-f
                         thread-registrar
                         threadpool-f
                         distributor-fn
                         threadpool-interrupted?
                         logging-key
                         log)]
    (dotimes [i max-threads-f]
      (let [thread-name  (keyword (str (core/name name-f) "-" i))
            interrupted? (atom false)]
        (assoc! thread-registrar thread-name {:interrupted? interrupted?})
        (async-loop {:type       :thread
                     :id         thread-name
                     :threadpool @threadpool-f}
          []
          (logic/when-let
            [[val- queue-]    (casync/alts!! [work-queue (casync/timeout 500)])
             [timestamp work] val-]
            (try (apply @distributor-fn work)
              (catch Throwable e ; err/suppress doesn't yet work
                (log/ppr logging-key e)
                (conj! log [(time/now-instant) thread-name e])))) ; 500 because it may be wise to be in parked rather than always checking for interrupt
          (when-not (or @threadpool-interrupted? @interrupted?)
            (recur)))))
    distributor-f)))

#?(:clj
(defn distribute!*
  {:usage '(distribute!* offer! (->distributor) [1 2 3 5 6] {:cache? true})}
  [enqueue-fn distributor & inputs]
  (validate distributor (fn$ instance? Distributor))
  (enqueue-fn (:work-queue distributor) [(time/now-instant) inputs])))

#?(:clj (defn distribute!  [distributor & inputs] (apply distribute!* async/offer! distributor inputs)))
#?(:clj (defn distribute>! [distributor & inputs] (apply distribute!* async/>!!    distributor inputs)))

#?(:clj
(defn distribute-all! [distributor inputs-set & [apply?]]
  (for [inputs inputs-set]
    (if apply?
        (apply distribute! distributor inputs)
        (distribute! distributor inputs)))))
