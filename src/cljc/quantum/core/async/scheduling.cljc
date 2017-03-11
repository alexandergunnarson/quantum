(ns quantum.core.async.scheduling
  (:refer-clojure :exclude [integer?])
  (:require
    [com.stuartsierra.component :as comp]
    [quantum.core.fn
      :refer [fn1 fn-> fn$]]
    [quantum.core.collections   :as coll
      :refer [nempty? kw-map]]
    [quantum.core.error         :as err
      :refer [catch-all]]
    [quantum.core.spec          :as s
      :refer [validate]]
    [quantum.core.resources     :as res]
    [quantum.core.log           :as log]
    [quantum.core.data.map      :as map]
    [quantum.core.time.core     :as time]
    [quantum.core.type
      :refer [atom? integer?]]
    [quantum.core.macros        :as macros
      :refer [defnt]])
  #?(:clj
  (:import
    (java.util.concurrent ScheduledExecutorService Executors))))

; ===== Scheduler ===== ;

#?(:clj
(defrecord JavaScheduler
  [^ScheduledExecutorService pool threads]
  comp/Lifecycle
  (comp/start [this]
    (validate threads (fn1 integer?))
    (assoc this :pool (Executors/newScheduledThreadPool (long threads))))
  (comp/stop [this]
    (let [_ (.shutdownNow pool)]
      (assoc this :pool nil)))))

#?(:clj (res/register-component! ::java-scheduler map->JavaScheduler []))

#?(:clj
(defrecord
  ^{:doc "Why busy waiting?
          http://www.rationaljava.com/2015/10/measuring-microsecond-in-java.html
          The only way to pause for anything less than a millisecond accurately is by busy waiting.
          Thread.sleep(1) is only 75% accurate
          LockSupport only begins to get accurate at 100us
          By contrast, busy waiting on >=10us is almost 100% accurate.
          The disadvantage, of course, is that busy waiting will tie up a CPU."}
  BusyWaitScheduler
  [queue interrupted? shut-down? busy-waiter]
  comp/Lifecycle
  (comp/start [this]
    (let [shut-down?   (atom false)
          queue        (atom (map/sorted-rank-map))
          interrupted? (atom false)
          busy-waiter
            (future
              (log/pr ::debug "Started busy waiter.")
              (while (not (or @interrupted?
                              (and @shut-down? (empty? @queue))))
                (catch-all
                  (let [now                 (System/nanoTime)
                        [prevs [_ curr-fs]] (map/split-key now @queue)]
                    (when (or (nempty? prevs) (nempty? curr-fs))
                      (doseq [[_ prev-fs] prevs]
                        (doseq [prev-f prev-fs] (prev-f)))
                      (when curr-fs
                        (doseq [curr-f curr-fs] (curr-f)))
                      ; TODO simplistic in that it potentially drops/overwrites old ones
                      ; also if it fails to execute, it should still remove from queue
                      (swap! queue (fn-> (#(reduce dissoc % (keys prevs)))
                                         (dissoc now)))))
                  e (log/pr :warn e)))
              (log/pr ::debug "BusyWaitScheduler finished running."))]
      (merge this (kw-map queue interrupted? shut-down? busy-waiter))))
  (comp/stop [this]
    (reset! interrupted? true)
    (reset! shut-down?   true)
    this)))

#?(:clj (res/register-component! ::busy-wait-scheduler map->BusyWaitScheduler []))


#?(:clj
(defnt shut-down!
  ([^ScheduledExecutorService x] (.shutdown x))
  ([^JavaScheduler            x] (-> x :pool shut-down!))
  ([^BusyWaitScheduler        x] (-> x :shut-down? (reset! true)))))

#?(:clj
(defnt await-termination! ; TODO include timeouts
  ([^ScheduledExecutorService x] (shut-down! x) (.awaitTermination x Integer/MAX_VALUE (time/->timeunit :millis)))
  ([^JavaScheduler            x] (shut-down! x) (-> x :pool await-termination!))
  ([^BusyWaitScheduler        x] (shut-down! x) @(:busy-waiter x))))

#?(:clj
(defnt schedule!
  ([^JavaScheduler scheduler at f]
    (validate at (s/and number? (fn1 >= 0))
              f  fn?)
    (let [wait (max 0 ; Negative delay goes to 0
                    (- at (System/nanoTime)))
          scheduler* (-> scheduler :pool
                         (validate (fn$ instance? ScheduledExecutorService)))]
      (.schedule ^ScheduledExecutorService scheduler* ^Callable f
                 (long wait) (time/->timeunit :ns))))
  ([^BusyWaitScheduler scheduler at f]
    (validate at (s/and number? (fn1 >= 0))
              f  fn?)
    (let [shut-down? (-> scheduler :shut-down? (validate atom?) deref)
          queue      (-> scheduler :queue      (validate atom?))]
      (if shut-down?
          false
          (do (swap! queue update (long at) (fn-> (coll/ensurec []) (conj f)))
              true))))))
