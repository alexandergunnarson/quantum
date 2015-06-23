(ns
  ^{:doc "Simple thread management through 'registered threads'.
          Aliases core.async for convenience.

          A little rough, but some useful functionality."
    :attribution "Alex Gunnarson"}
  quantum.core.thread
  (:require-quantum [ns num fn str err macros logic vec coll log res qasync])
  (:require
    [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]
    #?@(:clj [[clojure.core.async.impl.ioc-macros :as ioc]
              [clojure.core.async.impl.exec.threadpool :as async-threadpool]]))
  #?(:clj (:import (java.lang Thread Process)
            (java.util.concurrent Future Executor ExecutorService))))

;(def #^{:macro true} go      #'async/go) ; defalias fails with macros (does it though?)...
#?(:clj (defmalias go      async/go))
#?(:clj (defmalias go-loop async/go-loop))

#?(:clj (defalias <!       async/<!))
#?(:clj (defalias <!!      async/<!!))
#?(:clj (defalias >!       async/>!))
#?(:clj (defalias >!!      async/>!!))

#?(:clj (defalias chan     async/chan))
;#?(:clj (defalias close!   async/close!))

#?(:clj (defalias alts!    async/alts!))
#?(:clj (defalias alts!!   async/alts!!))
#?(:clj (defalias thread   async/thread))


#?(:clj (defonce reg-threads (atom {}))) ; {:thread1 :open :thread2 :closed :thread3 :close-req}

#?(:clj
  (defn stop-thread!
    {:attribution "Alex Gunnarson"}
    [thread-id]
    (case (get @reg-threads thread-id)
      nil
      (log/pr :user (str "Thread '" (name thread-id) "' is not registered."))
      :open
      (do (swap! reg-threads assoc thread-id :close-req)
          (log/pr :user (str "Closing thread '" (name thread-id) "'..."))
          (while (not= :closed (get @reg-threads thread-id)))
          (log/pr :user (str "Thread '" (name thread-id) "' closed.")))
      :closed
      (log/pr :user (str "Thread '" (name thread-id) "' is already closed."))
      nil)))

#?(:clj
(defn register-thread! [{:keys [id thread handlers]}]
  (swap! reg-threads coll/assocs-in+
    [id :thread  ] thread
    [id :handlers] handlers)))

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
                 ((fn-not contains?) @reg-threads ~id)
                 {:type    :key-exists
                  :message (str "Thread id '" (name ~id) "' already exists.")}))
           ^Atom result#
             (atom {:completed false
                    :result   nil})
           ^Thread thread#
             (Thread.
               (fn []
                 (try
                   (swap! reg-threads assoc-in [~id :running?] :open)
                   (binding [*ns* ns-0#]
                     (swap! result# assoc :result
                       (do ~expr ~@exprs))
                     (swap! result# assoc :completed true))
                   (finally
                     (swap! reg-threads dissoc ~id)
                     (log/pr :user "Thread" ~id "finished running.")))))]
       (.start thread#)
       (swap! reg-threads coll/assocs-in+
          [~id :thread] thread#
          [~id :handlers] ~handlers)
       result#)))

#?(:clj
(defnt interrupt!
  [Thread]  ([x] (.interrupt x))))  ; /join/ after interrupt doesn't work

#?(:clj
(defnt interrupted?
  [Thread]  ([x] (.isInterrupted x))))

#?(:clj
(defnt close-impl!
  [Thread]  ([x] (.stop    x))
  [Process] ([x] (.destroy x))
  [Future]  ([x] (.cancel  x true))
  nil?      ([x] nil)))

#?(:clj
(defnt closed?
  [Thread]  ([x] (not (.isAlive x)))
  [Process] ([x] (try (.exitValue x) true
                   (catch IllegalThreadStateException _ false)))
  [Future]  ([x] (or (.isCancelled x) (.isDone x)))))

#?(:clj
(defn interrupt!* [thread thread-id interrupted]
  (interrupt! thread)
  (if ((fn-or interrupted? closed?) thread)
      ((or interrupted fn-nil) thread)
      (throw+ {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be interrupted.")}))))

#?(:clj
(defn close!* [thread thread-id closed close-reqs]
  (let [max-tries 3]
    (when close-reqs
      (qasync/put! close-reqs :req)
      (Thread/sleep 200)) ; wait for graceful shutdown
    (close-impl! thread) ; force shutdown
    (loop [tries 0]
      (if (or (closed? thread) (>= tries max-tries))
          (do ((or closed fn-nil) thread)
              (when (closed? thread)
                (swap! reg-threads dissoc thread-id)))
          (do (log/pr :debug {:type :thread-already-closed :msg (str/sp "Thread" thread-id "cannot be closed.")})
              (Thread/sleep 100) ; wait a little bit for it to stop)
              (recur (inc tries))))))))

#?(:clj
  (defn close!
    {:attribution "Alex Gunnarson"}
    [^Keyword thread-id]
    {:pre [(with-throw
             (contains? @reg-threads thread-id)
             {:type :nonexistent-thread :msg (str/sp "Thread-id" thread-id "does not exist.")})]}
    (let [{:keys [thread children close-reqs]
           {:keys [closed interrupted]} :handlers}
            (get @reg-threads thread-id)]
      (doseq [child children]
        (close! child)) ; close children before parent
      (when (instance? Thread thread)
        (interrupt!* thread thread-id interrupted))
      (close!* thread thread-id closed close-reqs))))

#?(:clj
(defn close-gracefully! [id]
  (when-let [c (-> @reg-threads id :close-reqs)]
    (put! c :req))))

#?(:clj
(defn add-child-proc! [parent-id child-id]
  (if (contains? @reg-threads parent-id)
      (if (-> @reg-threads parent-id (contains? :children))
          (swap! reg-threads update-in [parent-id :children] conj child-id)
          (swap! reg-threads assoc-in  [parent-id :children] #{child-id}))
      (throw+ {:message (str/sp "Parent thread" parent-id "does not exist.")}))))

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
(defn close-all! []
  (while ((fn-and map? nempty?) @reg-threads)
    (doseq [thread (keys @reg-threads)]
      (try+ (close! thread)
        (catch [:type :nonexistent-thread] {:keys [type]}
          (when-not (= type :nonexistent-thread) (throw+))))))))

#?(:clj
(defonce add-thread-shutdown-hooks!
  (-> (Runtime/getRuntime) (.addShutdownHook (Thread. close-all!)))))
 
; ASYNC

#?(:clj
(defn closeably-execute [^Runnable r {:keys [cleanup parent id] :as opts}]
  (let [^Future future-obj
         (.submit ^ExecutorService async-threadpool/the-executor r)]
    (when cleanup (res/with-cleanup future-obj cleanup))
    (register-thread! (merge opts {:thread future-obj}))
    (when parent
      (log/pr :debug "Adding child proc" id "to parent" parent)
      (add-child-proc! parent id)))))

#?(:clj
(defn threadpool-run
  "Runs Runnable @r in a threadpool thread"
  [^Runnable r opts]
  (closeably-execute r opts)))

#?(:clj
(defmacro lt-thread
  "Creates a closeable 'light thread' on a go block."
  [{:keys [cleanup id parent] :as opts} & body]
  `(let [c# (chan 1)
         name# (:name ~opts)
         captured-bindings# (clojure.lang.Var/getThreadBindingFrame)
         proc-id# (if ~id ~id
                      (keyword
                        (gensym
                          (cond
                            (and ~parent name#)
                              (str (name ~parent) "$" (name name#) ".")
                            name#
                              (str "$" (name name#) ".")
                            ~parent
                              (str (name ~parent) "$")
                            :else
                              ""))))]
     (threadpool-run
       (fn []
         (let [f# ~(ioc/state-machine `(do ~@body) 1 (keys &env) ioc/async-custom-terminators)
               state# (-> (f#)
                          (ioc/aset-all! ioc/USER-START-IDX c#
                                         ioc/BINDINGS-IDX captured-bindings#))]
           (ioc/run-state-machine-wrapped state#)))
       (assoc ~opts :id proc-id#))
     c#)))

#?(:clj
  (defn- thread-or
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
                                   (when (every? realized? (map peek @fps))
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
  (defn- thread-and
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
                                   (when (every? realized? (map peek @fps))
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



