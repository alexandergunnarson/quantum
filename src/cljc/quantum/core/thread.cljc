(ns
  ^{:doc "Simple thread management through 'registered threads'.
          Aliases core.async for convenience.

          A little rough, but some useful functionality."
    :attribution "Alex Gunnarson"}
  quantum.core.thread
  (:require-quantum [ns num fn str err logic vec coll err])
  (:require [#?(:clj clojure.core.async :cljs cljs.core.async) :as async]))


;(def #^{:macro true} go      #'async/go) ; defalias fails with macros (does it though?)...
#?(:clj (defmalias go      async/go))
#?(:clj (defmalias go-loop async/go-loop))

#?(:clj (defalias <!       async/<!))
#?(:clj (defalias <!!      async/<!!))
#?(:clj (defalias >!       async/>!))
#?(:clj (defalias >!!      async/>!!))

#?(:clj (defalias chan     async/chan))
#?(:clj (defalias close!   async/close!))

#?(:clj (defalias alts!    async/alts!))
#?(:clj (defalias alts!!   async/alts!!))
#?(:clj (defalias thread   async/thread))


#?(:clj (def  reg-threads (atom {}))) ; {:thread1 :open :thread2 :closed :thread3 :close-req}

#?(:clj
  (defn stop-thread!
    {:attribution "Alex Gunnarson"}
    [thread-id]
    (case (get @reg-threads thread-id)
      nil
      (println (str "Thread '" (name thread-id) "' is not registered."))
      :open
      (do (swap! reg-threads assoc thread-id :close-req)
          (println (str "Closing thread '" (name thread-id) "'..."))
          (while (not= :closed (get @reg-threads thread-id)))
          (println (str "Thread '" (name thread-id) "' closed.")))
      :closed
      (println (str "Thread '" (name thread-id) "' is already closed."))
      nil)))

#?(:clj (def ^{:dynamic true} *thread-num* (.. Runtime getRuntime availableProcessors)))
; Why you want to manage your threads when doing network-related things:
; http://eng.climate.com/2014/02/25/claypoole-threadpool-tools-for-clojure/
#?(:clj
  (defmacro thread+
    "Execute exprs in another thread and returns the thread."
    ^{:attribution "Alex Gunnarson"}
    [^Keyword thread-id & exprs]
    `(let [ns-0# *ns*
           pre#
             (and
               (with-throw
                 (keyword? ~thread-id)
                 {:message "Thread-id must be a keyword."})
               (with-throw
                 ((fn-not contains?) @reg-threads ~thread-id)
                 {:type    :key-exists
                  :message (str "Thread id '" (name ~thread-id) "' already exists.")}))
           ^Atom result#
             (atom {:completed false
                    :result   nil})
           ^java.lang.Thread thread#
             (java.lang.Thread.
               (fn []
                 (try
                   (swap! reg-threads assoc-in [~thread-id :running?] :open)
                   (binding [*ns* ns-0#]
                     (swap! result# assoc :result    (do ~@exprs))
                     (swap! result# assoc :completed true))
                   (finally
                     (swap! reg-threads dissoc ~thread-id)
                     (println "Thread" ~thread-id "finished running.")))))]
       (.start thread#)
       (swap! reg-threads assoc-in [~thread-id :thread] thread#)
       result#)))

#?(:clj
  (defn close!
    {:attribution "Alex Gunnarson"}
    [^Keyword thread-id]
    {:pre [(with-throw
             (contains? @reg-threads thread-id)
             {:message (str/sp "Thread-id" thread-id "does not exist.")})]}
    (let [^java.lang.Thread thread
            (-> @reg-threads (get thread-id) :thread)]
      (.interrupt thread) ; /join/ after interrupt doesn't work 
      (if (.isInterrupted thread)
          (do (.stop thread)
              (Thread/sleep 100) ; wait a little bit for it to stop
              (when (.isAlive thread)
                (throw+ {:message (str/sp "Thread" thread-id "cannot be closed.")})))
          (throw+
            {:message (str/sp "Thread" thread-id "cannot be closed.")})))))

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


