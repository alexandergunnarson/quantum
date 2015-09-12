(ns
  ^{:doc "Complex thread management made simple.
          Aliases core.async for convenience."
    :attribution "Alex Gunnarson"}
  quantum.core.thread
  (:require-quantum [ns num fn str err macros logic vec coll log res core-async async])
  (:require
    #?@(:clj [[clojure.core.async.impl.ioc-macros :as ioc]
              [clojure.core.async.impl.exec.threadpool :as async-threadpool]]))
  #?(:clj (:import (java.lang Thread Process)
            (java.util.concurrent Future Executor ExecutorService ThreadPoolExecutor)
            quantum.core.data.queue.LinkedBlockingQueue)))

;(def #^{:macro true} go      #'async/go) ; defalias fails with macros (does it though?)...
; #?(:clj (defmalias go      core-async/go))
; #?(:clj (defmalias go-loop core-async/go-loop))

; #?(:clj (defalias <!       core-async/<!))
; #?(:clj (defalias <!!      core-async/<!!))
; #?(:clj (defalias >!       core-async/>!))
; #?(:clj (defalias >!!      core-async/>!!))

; #?(:clj (defalias chan     core-async/chan))
; ;#?(:clj (defalias close!   async/close!))

; #?(:clj (defalias alts!    core-async/alts!))
; #?(:clj (defalias alts!!   core-async/alts!!))
; #?(:clj (defalias thread   core-async/thread))


#?(:clj (defonce reg-threads (atom {}))) ; {:thread1 :open :thread2 :closed :thread3 :close-req}
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
(defnt interrupt!
  ([#{Thread}         x] (.interrupt x)) ; /join/ after interrupt doesn't work
  ([#{Process java.util.concurrent.Future} x] nil))) ; .cancel? 

#?(:clj
(defnt interrupted?
  ([#{Thread}         x] (.isInterrupted x))
  ([#{Process java.util.concurrent.Future} x] :unk)))

#?(:clj
(defnt close-impl!
  ([^Thread  x] (.stop    x))
  ([^Process x] (.destroy x))
  ([^java.util.concurrent.Future  x] (.cancel  x true))
  ([         x] (if (nil? x) true (throw :not-implemented)))))

#?(:clj
(defnt closed?
  ([^Thread x] (not (.isAlive x)))
  ([^Process x] (try (.exitValue x) true
                   (catch IllegalThreadStateException _ false)))
  ([^java.util.concurrent.Future x] (or (.isCancelled x) (.isDone x)))
  ([^boolean? x] x)
  ([x] (if (nil? x) true (throw :not-implemented)))))

#?(:clj (def open? (fn-not closed?)))

#?(:clj
(defn ^:private interrupt!* [thread thread-id interrupted force?]
  (if-not force?
    (do (log/pr ::debug "Before interrupt handler for id" thread-id)
        ((or interrupted fn-nil) thread)
        (log/pr ::debug "After interrupt handler for id" thread-id))
    (do (interrupt! thread)
        (if ((fn-or interrupted? closed?) thread)
            ((or interrupted fn-nil) thread)
            (throw+ {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be interrupted.")}))))))

#?(:clj
(defn ^:private close!* [thread thread-id close-reqs cleanup force?]
  (let [max-tries 3]
    (log/pr ::debug "Before close request for id" thread-id)
    (when (and close-reqs (not (async/closed? close-reqs)))
      (async/put!! close-reqs :req)) ; it calls its own close-request handler
    (log/pr ::debug "After close request for id" thread-id)
    (force cleanup) ; closes message queues, releases other resources
                    ; that can/should be released before full termination
    (log/pr ::debug "After cleanup for id" thread-id)
    (when force?
      (close-impl! thread) ; force shutdown
      (loop [tries 0]
        (log/pr :debug "Trying to close thread" thread-id)
        (when (and (open? thread) (<= tries max-tries))
          (log/pr :debug {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be closed.")})
          (Thread/sleep 100) ; wait a little bit for it to stop
          (recur (inc tries))))))))

#?(:clj
(defn close!
  "Closes a thread or lt-thread, possibly gracefully."
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

(defn close-threads! [pred]
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
      (close-impl! thread-))))

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

; (count (.getQueue thread/threadpool))
(def threadpool clojure.core.async.impl.exec.threadpool/the-executor)

(.setRejectedExecutionHandler threadpool
  (reify java.util.concurrent.RejectedExecutionHandler
    (^void rejectedExecution [this ^Runnable f ^ThreadPoolExecutor executor]
      (@rejected-execution-handler f))))

(defnt set-max-threads!
  [^java.util.concurrent.ThreadPoolExecutor threadpool-n n]
  (doto threadpool-n
    (.setCorePoolSize    n)
    (.setMaximumPoolSize n)))

(defn gen-threadpool [num-threads]
  (set-max-threads! 
    (java.util.concurrent.Executors/newFixedThreadPool num-threads)
    num-threads))

(defn clear-work-queue! [^ThreadPoolExecutor threadpool-n]
  (->> threadpool-n .getQueue (map #(.cancel ^Future % true)) dorun)
  (->> threadpool-n .purge))

#?(:clj
(defn ^:internal closeably-execute [threadpool-n ^Runnable r {:keys [id] :as opts}]
  (when (register-thread! (merge opts {:thread true}))
    (try
      (let [^Future future-obj
             (.submit ^ExecutorService threadpool-n r)]
        (swap! reg-threads assoc-in [id :thread future-obj]))
      (catch Throwable e ; what exception?
        (deregister-thread! id))))))

#?(:clj
(defn ^:internal threadpool-run
  "Runs Runnable @r in a threadpool thread"
  [threadpool-n ^Runnable r opts]
  (closeably-execute threadpool-n r opts)))

#?(:clj
(defmacro ^:internal lt-go-thread*  
  [opts & body]
  `(let [c# (chan 1)
         captured-bindings# (clojure.lang.Var/getThreadBindingFrame)]
     (threadpool-run
       (or (:threadpool ~opts) threadpool)
       (fn []
         (let [f# ~(ioc/state-machine `(do ~@body) 1 (keys &env) ioc/async-custom-terminators)
               state# (-> (f#)
                          (ioc/aset-all! ioc/USER-START-IDX c#
                                         ioc/BINDINGS-IDX captured-bindings#))]
           (ioc/run-state-machine-wrapped state#)))
       ~opts)
     c#)))

#?(:clj
(defmacro ^:internal lt-thread*  
  [opts & body]
  `(threadpool-run
     (or (:threadpool ~opts) threadpool)
     (fn [] ~@body)
     ~opts)))

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

#?(:clj
(defmacro lt-thread
  "Creates a closeable 'light thread' on a go block.

   Handlers: (maybe make certain of these callable once?) 
     For all threads:
     :close-req    — Called when another thread requests to close
                     (adds an obj to the close-reqs queue)  
                     Somewhat analogous to :interrupted.
     :closed       — Called by the thread reaper when the thread is fully closed.
                     Not called if the thread completes without a close request.
     :completed (Planned)    — Called when the thread runs to completion.
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
  (let [proc-id        (gensym "proc-id")
        close-req-call (gensym "close-req-call")
        close-reqs     (gensym "close-reqs")
        cleanup-f      (gensym "cleanup-f")
        opts-f         (gensym "opts-f")]
   `(let [~opts-f ~opts ; so you create it only once
          asserts# (with-throw (map? ~opts-f) "@opts must be a map.")
          ; Proper destructuring in a macro would be nice... oh well...
          close-req#   (-> ~opts-f :handlers :close-req)
          cleanup#     (-> ~opts-f :handlers :cleanup)
          id#          (:id          ~opts-f)
          parent#      (:parent      ~opts-f)
          name#        (:name        ~opts-f)
          cleanup-seq# (:cleanup-seq ~opts-f)
          asserts#
            (do (when cleanup#
                  (with-throw (fn? cleanup#) "@cleanup must be a function."))
                (when cleanup-seq#
                  (with-throw (instance? clojure.lang.IAtom cleanup-seq#)
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
          ~close-reqs (or (:close-reqs ~opts-f) (LinkedBlockingQueue.))
          ~proc-id   (gen-proc-id id# parent# name#)
          ~opts-f    (coll/assocs-in+         ~opts-f
                       [:close-reqs         ] ~close-reqs
                       [:id]                  ~proc-id
                       [:handlers :close-req] ~close-req-call
                       [:handlers :cleanup  ] ~cleanup-f)]
      (lt-thread* ~opts-f
        (.setName (Thread/currentThread) (name ~proc-id))
        (swap! reg-threads assoc-in [~proc-id :state] :running)
        (try+ ~@body
          (catch Object e#
            ((or (-> ~opts-f :handlers :err/any.pre ) fn-nil))
            (log/pr-opts :warn #{:thread?} "Exited with exception" e#)
            ((or (-> ~opts-f :handlers :err/any.post) fn-nil)))
          (finally 
            (log/pr-opts :quantum.core.thread/debug #{:thread?} "COMPLETED.")
            (when (get @reg-threads ~proc-id)
              (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLEANING UP" ~proc-id)
              (doseq [child-id# (get-in @reg-threads [~proc-id :children])]
                (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLOSING CHILD" child-id# "OF" ~proc-id "IN END-THREAD")
                (close! child-id#)
                (log/pr-opts :quantum.core.thread/debug #{:thread?} "CLOSING CHILD" child-id# "OF" ~proc-id "IN END-THREAD"))
              (force ~cleanup-f) ; Performed only once
              (swap! reg-threads assoc-in [~proc-id :state] :closed)
              (deregister-thread! ~proc-id)))))))))
  
#?(:clj
(defmacro lt-thread-loop
  [opts bindings & body]
  (let [close-reqs-f   (gensym)
        close-req-call (gensym)]
   `(let [opts-f# ~opts
          ~close-reqs-f (or (:close-reqs opts-f#) (LinkedBlockingQueue.))
          ~close-req-call (wrap-delay (-> opts-f# :handlers :close-req))]
      (lt-thread (coll/assocs-in+ ~opts
                   [:close-reqs         ] ~close-reqs-f
                   [:handlers :close-req] ~close-req-call)
        (try
          (loop ~bindings
            (when (empty? ~close-reqs-f)
              (do ~@body)))
          (finally
            (when (nempty? ~close-reqs-f)
              (force ~close-req-call)))))))))

(defn lt-thread-chain
  "To chain subprocesses together on the same thread.
   To track progress, etc. on phases of completion."
  [universal-opts thread-chain-template]
  (throw+ {:msg "Unimplemented"}))

#?(:clj
(defn reap-threads! []
  (doseq [id thread-meta @reg-threads]
    (let [{:keys [thread state handlers]} thread-meta]
      (when (or (closed? thread)
                (= state :closed))
        (whenf (:closed handlers) nnil?
          (if*n delay? force call))
        (deregister-thread! id))))))

#?(:clj (defonce thread-reaper-pause-requests  (LinkedBlockingQueue.)))
#?(:clj (defonce thread-reaper-resume-requests (LinkedBlockingQueue.)))

#?(:clj
(defonce thread-reaper
  (do (log/enable! :macro-expand)
      (with-do
        (let [id :thread-reaper]
          (lt-thread-loop
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
                    (Thread/sleep 2000)
                    (recur)))))
        (log/disable! :macro-expand)))))

#?(:clj (defn pause-thread-reaper!  [] (put!! thread-reaper-pause-requests true)))
#?(:clj (defn resume-thread-reaper! [] (put!! thread-reaper-resume-requests true)))

(defonce gc-worker
  (lt-thread {:id :gc-collector}
    (loop [] (System/gc) (Thread/sleep 60000))))

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
  (thread-or #(do (Thread/sleep 1000) (println :foo) true)
             #(do (Thread/sleep 3000) (println :bar)))
  ;;= true
 
  ;; prints :foo and :bar
  (thread-or #(do (Thread/sleep 1000) (println :foo))
             #(do (Thread/sleep 3000) (println :bar)))
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
  (thread-and #(do (Thread/sleep 1000) (println :foo))
              #(do (Thread/sleep 3000) (println :bar)))
 
  )


; (defn do-intervals [millis & args]
;   (->> args
;        (interpose #(Thread/sleep millis))
;        (map+ fold+)
;        fold+
;        pr/suppress))
; (defn do-every [millis n func]
;   (dotimes [_ n] (func) (Thread/sleep millis)))

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

