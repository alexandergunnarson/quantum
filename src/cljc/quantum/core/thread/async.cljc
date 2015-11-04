(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"}
  quantum.core.thread.async
  (:refer-clojure :exclude [promise realized?])
  (:require-quantum
    [ns num fn str err logic vec err macros log coll])
  (:require [#?(:clj  clojure.core.async
                :cljs cljs.core.async)        :as async ]
   #?@(:clj [[co.paralleluniverse.pulsar.async :as async+]
             [co.paralleluniverse.pulsar.core  :as pasync]]))
  #?(:clj (:import clojure.core.async.impl.channels.ManyToManyChannel
                   (java.util.concurrent TimeUnit)
                   quantum.core.data.queue.LinkedBlockingQueue
                   co.paralleluniverse.fibers.Fiber
                   co.paralleluniverse.strands.Strand)))

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

(defnt chan*
  "(chan (buffer n)) or (chan n) are the same as (channel n :block   ) or (channel n).
   (chan (dropping-buffer n))    is  the same as (channel n :drop    )
   (chan (sliding-buffer n))     is  the same as (channel n :displace)

   Promises can be implemented in terms of |chan|:

   (let [a (promise)]
     (deliver a 123))

   (let [c (chan)]
     (>! c 123)" ; But then close chan
  ;([] (async+/chan))
  ([^integer? n]
   (async+/chan n))
  ([^keyword? type]
    (condp = type
      :std     (async+/chan)
      :queue   (LinkedBlockingQueue.)
      :casync  (async/chan)))
  ([^keyword? type n]
    (condpc = type
      :std     (async+/chan n)
      :queue   (LinkedBlockingQueue. ^Integer n) ; TODO reflection here
      :casync  (async/chan n))))

(defn chan
  ([         ] (async+/chan))
  ([arg0     ] (chan* arg0     ))
  ([arg0 arg1] (chan* arg0 arg1)))

(defn current-strand [] (Strand/currentStrand))
(defn current-fiber  [] (or (Fiber/currentFiber) (current-strand)))

;(chan) is the same as calling (channel).

(defalias buffer              #?(:clj async+/buffer              :cljs async/buffer             ))
(defalias dropping-buffer     #?(:clj async+/dropping-buffer     :cljs async/dropping-buffer    ))
(defalias sliding-buffer      #?(:clj async+/sliding-buffer      :cljs async/sliding-buffer     ))
(defalias unblocking-buffer?  #?(:clj async+/unblocking-buffer?  :cljs async/unblocking-buffer? ))

; TODO FIX THIS
(defnt take!! ; receive
#?@(:clj
 [([^quantum.core.data.queue.LinkedBlockingQueue        q  ] (.take q))
  ([^quantum.core.data.queue.LinkedBlockingQueue        q n] (.poll q n TimeUnit/MILLISECONDS))])
  ([^clojure.core.async.impl.channels.ManyToManyChannel c  ] (async/take! c identity))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c n] (async/alts! [(async/timeout n) c]))
  ([^co.paralleluniverse.strands.channels.ReceivePort   c  ] (async+/<! c)))

(defalias <!! take!!)

(defnt empty!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        q] (.clear q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c] (throw+ :unimplemented)))

(defnt put!! ; send
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        x obj] (.put x obj)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel x obj] (async/put! x obj))
  ([^co.paralleluniverse.strands.channels.ReceivePort   x obj] (async+/>! x obj)))

(defalias >!! put!!)

(defnt close!
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        q] (.close q)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel c] (throw+ :unimplemented))
  ([^co.paralleluniverse.strands.channels.SendPort      x] (.close x))
  ([^co.paralleluniverse.strands.channels.ReceivePort   x] (.close x)))

(defnt closed?
#?(:clj
  ([^quantum.core.data.queue.LinkedBlockingQueue        x] (.isClosed x)))
  ([^clojure.core.async.impl.channels.ManyToManyChannel x] (throw+ :unimplemented))
  ([^co.paralleluniverse.strands.channels.ReceivePort   x] (.isClosed x)))

#?(:clj (def open? (fn-not closed?)))

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

#?(:clj
(defnt interrupt!
  ([#{Thread}         x] (.interrupt x)) ; /join/ after interrupt doesn't work
  ([#{Process java.util.concurrent.Future} x] nil))) ; .cancel? 

#?(:clj
(defnt interrupted?
  ([#{Thread}         x] (.isInterrupted x))
  ([#{Process java.util.concurrent.Future} x] :unk)))

#?(:clj
(defnt close!
  ([^Thread                      x] (.stop    x))
  ([^Process                     x] (.destroy x))
  ([^java.util.concurrent.Future x] (.cancel  x true))
  ([                             x] (if (nil? x) true (throw :not-implemented)))))

#?(:clj
(defnt closed?
  ([^Thread x] (not (.isAlive x)))
  ([^Process x] (try (.exitValue x) true
                   (catch IllegalThreadStateException _ false)))
  ([#{java.util.concurrent.Future
      co.paralleluniverse.fibers.Fiber} x] (or (.isCancelled x) (.isDone x)))
  ([^boolean? x] x)
  ([x] (if (nil? x) true (throw :not-implemented)))))

#?(:clj
(defnt realized?
  ([^clojure.lang.IPending x] (realized? x))
  ([^co.paralleluniverse.strands.channels.QueueObjectChannel x] ; The result of a Pulsar go-block
    (-> x .getQueueLength (> 0)))
  ([#{java.util.concurrent.Future
      co.paralleluniverse.fibers.Fiber} x] (.isDone x))))

(defn sleep
  "Never use Thread/sleep in a go block. Use |(<! (timeout <msec>))|.
   Never use Thread/sleep in a fiber or it will generate constant warnings."
  [^long msec]
  #?(:clj   (if (Fiber/currentFiber)
                (Fiber/sleep  msec)
                (Strand/sleep msec))
     :cljs (async/<! (async/timeout msec))))

; MORE COMPLEX OPERATIONS

; For some reason, having lots of threads with core.async/alts!! "clogs the tubes", as it were
; Possibly because of deadlocking?
; So we're moving away from core.async, but keeping the same concepts
(defn alts!!-queue [chans timeout]
  (loop []
    (let [result (seq-loop [c   chans
                            ret nil] 
                   (locking c ; Because it needs to have a consistent view of when it's empty and take accordingly
                     (when (nempty? c)
                       (break [(take!! c) c]))))]
      (whenc result nil?
        (do (sleep 5)
            (recur))))))

(defnt alts!!
  "Takes the first available value from a chan."
  {:todo ["Implement timeout"]
   :attribution "Alex Gunnarson"}
  ([^keyword? type chans]
    (alts!! type chans nil))
  ([^coll? chans]
    (async+/alts!! chans))
  ([^coll? chans timeout]
    (async+/alts!! chans timeout))
  ([^keyword? type chans timeout]
    (condp = type
      :std     (if timeout
                   (async+/alts!! chans timeout)
                   (async+/alts!! chans))
      :queue   (alts!!-queue chans (or timeout Integer/MAX_VALUE))
      :casync  (if timeout
                   (async/alts!! chans timeout)
                   (async/alts!! chans)))))



; Promise, delay, future
; co.paralleluniverse.strands.channels.QueueObjectChannel : (<! (go 1)) is similar to (deref (future 1))
(defalias promise #?(:clj pasync/promise :cljs core/promise))

(defmacro wait-until
  ([pred] `(wait-until Long/MAX_VALUE ~pred))
  ([timeout pred]
   `(loop [timeout# ~timeout]
      (if (<= timeout# 0)
          (throw+ (Err. :timeout (str/sp "Operation timed out after" ~timeout "milliseconds") ~timeout))
          (when-not ~pred
            (sleep 10) ; Sleeping so as not to take up thread time
            (recur (- timeout# 11))))))) ; Takes a tiny bit more time