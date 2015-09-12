(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"}
  quantum.core.thread.async
  (:require-quantum
    [ns num fn str err logic vec err macros log coll])
  (:require [#?(:clj  clojure.core.async
                :cljs cljs.core.async) :as async])
  #?(:clj (:import clojure.core.async.impl.channels.ManyToManyChannel
                   (java.util.concurrent TimeUnit)
                   quantum.core.data.queue.LinkedBlockingQueue)))

#?(:clj
(defmacro <? [expr]
  `(let [expr-result# (<! ~expr)]
     (if ;(quantum.core.type/error? chan-0#)
         (instance? js/Error expr-result#)
         (throw expr-result#)
         expr-result#))))

#?(:clj
(defmacro try-go
  {:attribution "pepa.async"}
  [& body]
  `(macros/go
     (try+
       ~@body
       (catch :default e#
         e#)))))

(defrecord QueueCloseRequest [])
(defrecord TerminationRequest [])

(defn chan
  ([ ] (LinkedBlockingQueue.  ))
  ([n] (LinkedBlockingQueue. n)))

(defnt take-with-timeout!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue q n]
    (.poll q n (. TimeUnit MILLISECONDS))))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c n]
    (async/alts! [(async/timeout n) c])))

; TODO FIX THIS
(defnt take!!
#?@(:clj
 [([^quantum.core.data.queue.LinkedBlockingQueue q]   (.take q))
  ([^quantum.core.data.queue.LinkedBlockingQueue q n] (.poll q n (. TimeUnit MILLISECONDS)))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel c]   (async/take! c identity))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c n] (async/alts! [(async/timeout n) c])))

(defalias <!! take!!)

(defnt empty!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        q] (.clear q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c] (throw+ :unimplemented)))

(defnt put!!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        q obj] (.put q obj)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c obj] (async/put! c obj)))

(defalias >!! put!!)

(defnt close!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        q] (.close q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c] (throw+ :unimplemented)))

(defnt closed?
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        q] (.isClosed q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c] (throw+ (Err. :not-implemented "Not yet implemented." nil))))

(defnt message?
  ([^quantum.core.thread.async.QueueCloseRequest  obj] false)
  ([^quantum.core.thread.async.TerminationRequest obj] false)
  ([                    obj] (when (nnil? obj) true)))

(def close-req? (partial instance? QueueCloseRequest))

(defnt peek!!
  "Blocking peek."
#?@(:clj
 [([^quantum.core.data.queue.LinkedBlockingQueue q]         (.blockingPeek q))
  ([^quantum.core.data.queue.LinkedBlockingQueue q timeout] (.blockingPeek q timeout (. TimeUnit MILLISECONDS)))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel   c] (throw+ (Err. :not-implemented "Not yet implemented." nil))))

; MORE COMPLEX OPERATIONS

; For some reason, having lots of threads with core.async/alts!! "clogs the tubes", as it were
; Possibly because of deadlocking?
; So we're moving away from core.async, but keeping the same concepts
(defn alts!!
  {:todo ["Implement timeout"]}
  ([chans]
    (alts!! chans Integer/MAX_VALUE))
  ([chans timeout]
    (loop []
      (let [result (seq-loop [c   chans
                              ret nil] 
                     (locking c ; Because it needs to have a consistent view of when it's empty and take accordingly
                       (when (nempty? c)
                         (break [(take!! c) c]))))]
        (whenc result nil?
          (do (Thread/sleep 5)
              (recur))))))
  )

(defmacro wait-until
  ([pred] `(wait-until Long/MAX_VALUE ~pred))
  ([timeout pred]
   `(loop [timeout# ~timeout]
      (if (<= timeout# 0)
          (throw+ (Err. :timeout (str/sp "Operation timed out after" ~timeout "milliseconds") ~timeout))
          (when-not ~pred
            (Thread/sleep 10) ; Sleeping so as not to take up thread time
            (recur (- timeout# 11))))))) ; Takes a tiny bit more time