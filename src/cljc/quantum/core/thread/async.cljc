(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"}
  quantum.core.thread.async
  (:require-quantum
    [ns num fn str err logic vec err async macros log])
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

(defnt take-with-timeout!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue q n]
    (.poll q n (. TimeUnit MILLISECONDS))))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c n]
    (async/alts! [(async/timeout n) c])))

; TODO FIX THIS
(defnt take!
#?@(:clj
 [([^quantum.core.data.queue.LinkedBlockingQueue q]   (.take q))
  ([^quantum.core.data.queue.LinkedBlockingQueue q n] (.poll q n (. TimeUnit MILLISECONDS)))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel c]   (async/take! c identity))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c n] (async/alts! [(async/timeout n) c])))


(defnt empty!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue q] (.clear q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel   c] (throw+ :unimplemented)))

(defnt put! 
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue q obj] (.put  q obj)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel   c obj] (async/put! c obj)))

(defnt close!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue q] (.close q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel   c] (throw+ :unimplemented)))

(defnt closed?
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue q] (.isClosed q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel   c] (throw+ (Err. :not-implemented "Not yet implemented." nil))))

(defnt message?
  ([^quantum.core.thread.async.QueueCloseRequest  obj] false)
  ([^quantum.core.thread.async.TerminationRequest obj] false)
  ([                    obj] (when (nnil? obj) true)))

(def close-req? (partial instance? QueueCloseRequest))

(defnt peek!
  "Blocking peek."
#?@(:clj
 [([^quantum.core.data.queue.LinkedBlockingQueue q]         (.blockingPeek q))
  ([^quantum.core.data.queue.LinkedBlockingQueue q timeout] (.blockingPeek q timeout (. TimeUnit MILLISECONDS)))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel   c] (throw+ (Err. :not-implemented "Not yet implemented." nil))))

