(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"}
  quantum.core.thread.async
  (:require-quantum
    [ns num fn str err logic vec err async macros])
  #?(:clj (:import clojure.core.async.impl.channels.ManyToManyChannel
                   (java.util.concurrent LinkedBlockingQueue TimeUnit))))

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

(defnt take-with-timeout!
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q n]
      (if (instance? QueueCloseRequest (.peek q))
          (throw (InterruptedException. "Queue closed."))
          (.poll q n (. TimeUnit MILLISECONDS))))]
  [ManyToManyChannel]
    ([c n] (async/alts! [(async/timeout n) c]))))

(defnt take!
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q] (if (instance? QueueCloseRequest (.peek q))
             (throw (InterruptedException. "Queue closed."))
             (.take q)))])
  [ManyToManyChannel]
    ([c] (async/take! c)))

(defnt put! 
  #?@(:clj
 [[LinkedBlockingQueue]
    ([q obj]
      (if (instance? QueueCloseRequest (.peek q))
          (throw (InterruptedException. "Queue closed."))
          (.put  q obj)))])
  [ManyToManyChannel]
    ([c obj] (async/put! c obj)))

