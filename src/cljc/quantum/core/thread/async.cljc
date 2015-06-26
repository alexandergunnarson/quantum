(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"}
  quantum.core.thread.async
  (:require-quantum
    [ns num fn str err logic vec err async macros])
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

; #?(:clj
; (defmacro <? [ch]
;   `(let [e# (<! ~ch)]
;      (when (instance? js/Error e#) (throw e#))
;      e#)))

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
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q n]
      (.poll q n (. TimeUnit MILLISECONDS)))]
  [ManyToManyChannel]
    ([c n] (async/alts! [(async/timeout n) c]))))

(defnt take!
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q] (.take q))])
  [ManyToManyChannel]
    ([c] (async/take! c)))

(defnt empty!
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q] (.clear q))]))

(defnt put! 
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q obj] (.put  q obj))])
  [ManyToManyChannel]
    ([c obj] (async/put! c obj)))

(defnt close!
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q] (.close q))]))

(defnt closed?
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q] (.isClosed q))]))

(defnt message?
  [QueueCloseRequest]  ([obj] false)
  [TerminationRequest] ([obj] false)
  nil?                 ([obj] false)
  :default             ([obj] true ))

(def close-req? (partial instance? QueueCloseRequest))

(defnt peek!
  "Blocking peek."
  #?@(:clj
 [[LinkedBlockingQueue]
   (([q]        (.blockingPeek q))
   ([q timeout] (.blockingPeek q timeout (. TimeUnit MILLISECONDS))))]))

