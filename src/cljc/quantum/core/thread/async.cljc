(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"
    :cljs-self-referencing? true}
  quantum.core.thread.async
  (:refer-clojure :exclude [promise realized? future])
  (:require-quantum
    [:core #_num fn #_str err logic vec err #_macros log #_coll])
  (:require [com.stuartsierra.component       :as component]
            [#?(:clj  clojure.core.async
                :cljs cljs.core.async)        :as async    ]
  ;#?@(:clj [[co.paralleluniverse.pulsar.async :as async+   ]
  ;          [co.paralleluniverse.pulsar.core  :as pasync   ]])
   #?(:cljs [servant.core                     :as servant  ]))
  #?(:cljs
  (:require-macros
            [servant.macros                   :as servant
              :refer [defservantfn]                        ]
            [cljs.core.async.macros           :as asyncm   ]
            [quantum.core.thread.async
              :refer [go]                                  ]))
  #?(:clj (:import clojure.core.async.impl.channels.ManyToManyChannel
                   (java.util.concurrent TimeUnit)
                   #_quantum.core.data.queue.LinkedBlockingQueue
                   #_co.paralleluniverse.fibers.Fiber
                   #_co.paralleluniverse.strands.Strand)))

#?(:clj (defmalias go async/go asyncm/go))

#?(:clj
(defmacro <?
  "Takes a value from a core.async channel, throwing the value if it
   is a js/Error or Throwable."
  [expr]
  (let [err-class (if-cljs &env 'js/Error 'Throwable)]
   `(let [expr-result# (<! ~expr)]
      (if ;(quantum.core.type/error? expr-result#)
          (instance? ~err-class expr-result#)
          (throw expr-result#)
          expr-result#)))))

#?(:clj
(defmacro try-go
  [& body]
  (let [err-class (if-cljs &env 'js/Error 'Throwable)]
    `(asyncm/go
       (try
         ~@body
         (catch ~err-class e#
           e#))))))

(defrecord QueueCloseRequest [])
(defrecord TerminationRequest [])

;(defnt chan*
;  "(chan (buffer n)) or (chan n) are the same as (channel n :block   ) or (channel n).
;   (chan (dropping-buffer n))    is  the same as (channel n :drop    )
;   (chan (sliding-buffer n))     is  the same as (channel n :displace)
;
;   Promises can be implemented in terms of |chan|:
;
;   (let [a (promise)]
;     (deliver a 123))
;
;   (let [c (chan)]
;     (>! c 123)" ; But then close chan
;  ;([] (async+/chan))
;  ([^integer? n]
;   (async+/chan n))
;  ([^keyword? type]
;    (condp = type
;      :std     (async+/chan)
;      :queue   (LinkedBlockingQueue.)
;      :casync  (async/chan)))
;  ([^keyword? type n]
;    (condpc = type
;      :std     (async+/chan n)
;      :queue   (LinkedBlockingQueue. ^Integer n) ; TODO reflection here
;      :casync  (async/chan n))))
;
;(defn chan
;  ([         ] (async+/chan))
;  ([arg0     ] (chan* arg0     ))
;  ([arg0 arg1] (chan* arg0 arg1)))

(defalias chan async/chan)


;(defn current-strand [] (Strand/currentStrand))
#?(:clj (defn current-strand [] (Thread/currentThread)))
;(defn current-fiber  [] (or (Fiber/currentFiber) (current-strand)))

;(defalias buffer              #?(:clj async+/buffer              :cljs async/buffer             ))
;(defalias dropping-buffer     #?(:clj async+/dropping-buffer     :cljs async/dropping-buffer    ))
;(defalias sliding-buffer      #?(:clj async+/sliding-buffer      :cljs async/sliding-buffer     ))
;(defalias unblocking-buffer?  #?(:clj async+/unblocking-buffer?  :cljs async/unblocking-buffer? ))

; TODO FIX THIS
;(defnt take!! ; receive
;#?@(:clj
; [([^quantum.core.data.queue.LinkedBlockingQueue        q  ] (.take q))
;  ([^quantum.core.data.queue.LinkedBlockingQueue        q n] (.poll q n TimeUnit/MILLISECONDS))])
;  ([^clojure.core.async.impl.channels.ManyToManyChannel c  ] (async/take! c identity))
;  ([^clojure.core.async.impl.channels.ManyToManyChannel c n] (async/alts! [(async/timeout n) c]))
;  ([^co.paralleluniverse.strands.channels.ReceivePort   c  ] (async+/<! c)))

(declare take!!)

;(defalias <!! take!!)

(declare <!!)

(defalias take! async/take!)

(defalias <! async/<!)

;(defnt empty!
;#?(:clj
;  ([^quantum.core.data.queue.LinkedBlockingQueue        q] (.clear q)))
;  ([^clojure.core.async.impl.channels.ManyToManyChannel c] (throw+ :unimplemented)))
;

(declare empty!)

(defalias put! async/put!)

;(defnt put!! ; send
;#?(:clj
;  ([^quantum.core.data.queue.LinkedBlockingQueue        x obj] (.put x obj)))
;  ([^clojure.core.async.impl.channels.ManyToManyChannel x obj] (async/put! x obj))
;  ([^co.paralleluniverse.strands.channels.ReceivePort   x obj] (async+/>! x obj)))

(declare put!!)

;(defalias >!! put!!)
(declare >!!)

(defalias >! async/>!)
;
;(defnt message?
;  ([^quantum.core.thread.async.QueueCloseRequest  obj] false)
;  ([^quantum.core.thread.async.TerminationRequest obj] false)
;  ([                    obj] (when (nnil? obj) true)))

(declare message?)

(def close-req? (partial instance? QueueCloseRequest))

;(defnt peek!!
;  "Blocking peek."
;#?@(:clj
; [([^quantum.core.data.queue.LinkedBlockingQueue q]         (.blockingPeek q))
;  ([^quantum.core.data.queue.LinkedBlockingQueue q timeout] (.blockingPeek q timeout (. TimeUnit MILLISECONDS)))])
;  ([^clojure.core.async.impl.channels.ManyToManyChannel   c] (throw+ (Err. :not-implemented "Not yet implemented." nil))))

(declare peek!!)

;#?(:clj
;(defnt interrupt!
;  ([#{Thread}         x] (.interrupt x)) ; /join/ after interrupt doesn't work
;  ([#{Process java.util.concurrent.Future} x] nil))) ; .cancel? 

(declare interrupt!)

;#?(:clj
;(defnt interrupted?*
;  ([#{Thread co.paralleluniverse.strands.Strand}         x] (.isInterrupted x))
;  ([#{Process java.util.concurrent.Future} x] (throw+ (Err. :not-implemented "Not yet implemented." nil)))))

;#?(:clj
;(defn interrupted?
;  ([ ] (.isInterrupted ^Strand (current-strand)))
;  ([x] (interrupted?* x))))

(declare interrupted?)

;(defnt close!
;#?(:clj
;  ([^Thread                      x] (.stop    x))
;  ([^Process                     x] (.destroy x))
;  ([^java.util.concurrent.Future x] (.cancel  x true))
;  ([                             x] (if (nil? x) true (throw :not-implemented)))
;  ([^quantum.core.data.queue.LinkedBlockingQueue        q] (.close q))
;  ([^clojure.core.async.impl.channels.ManyToManyChannel c] (throw+ :unimplemented))
;  ([^co.paralleluniverse.strands.channels.SendPort      x] (.close x))
;  ([^co.paralleluniverse.strands.channels.ReceivePort   x] (.close x))))

(declare close!)

;(defnt closed?
;#?(:clj
;  ([^Thread x] (not (.isAlive x)))
;  ([^Process x] (try (.exitValue x) true
;                   (catch IllegalThreadStateException _ false)))
;  ([#{java.util.concurrent.Future
;      co.paralleluniverse.fibers.Fiber} x] (or (.isCancelled x) (.isDone x)))
;  ([^quantum.core.data.queue.LinkedBlockingQueue        x] (.isClosed x))
;  ([^clojure.core.async.impl.channels.ManyToManyChannel x] (throw+ :unimplemented))
;  ([^co.paralleluniverse.strands.channels.ReceivePort   x] (.isClosed x))
;  ([^boolean? x] x)
;  ([x] (if (nil? x) true (throw :not-implemented)))))

(declare closed?)

#?(:clj (def open? (fn-not closed?)))

;#?(:clj
;(defnt realized?
;  ([^clojure.lang.IPending x] (realized? x))
;  ([^co.paralleluniverse.strands.channels.QueueObjectChannel x] ; The result of a Pulsar go-block
;    (-> x .getQueueLength (> 0)))
;  ([#{java.util.concurrent.Future
;      co.paralleluniverse.fibers.Fiber} x] (.isDone x))))

#?(:clj
(defmacro sleep
  "Macro because CLJS needs |<!| to be within a |go| block not
   just in compiled form, but in uncompiled (syntactic) form.

   Never use Thread/sleep in a go block. Use |(<! (timeout <msec>))|.
   Never use Thread/sleep in a fiber or it will generate constant warnings.

   Never use |sleep| without marking the enclosing function |suspendable!| (and probably all other
   functions that call it...)."
  [millis]
  (if-cljs &env
    `(<! (timeout ~millis))
    `(Thread/sleep ~millis)
    #_(if (Fiber/currentFiber)
          (Fiber/sleep  ~millis)
          (Strand/sleep ~millis)))))

; MORE COMPLEX OPERATIONS

; For some reason, having lots of threads with core.async/alts!! "clogs the tubes", as it were
; Possibly because of deadlocking?
; So we're moving away from core.async, but keeping the same concepts
;(defn+ alts!!-queue [chans timeout] ; Unable to mark ^:suspendable because of synchronization
;  (loop []
;    (let [result (seq-loop [c   chans
;                            ret nil] 
;                   (locking c ; Because it needs to have a consistent view of when it's empty and take accordingly
;                     (when (nempty? c)
;                       (break [(take!! c) c]))))]
;      (whenc result nil?
;        (do (sleep 5)
;            (recur))))))

;(defnt alts!!
;  "Takes the first available value from a chan."
;  {:todo ["Implement timeout"]
;   :attribution "Alex Gunnarson"}
;  ([^keyword? type chans]
;    (alts!! type chans nil))
;  ([^coll? chans]
;    (async+/alts!! chans))
;  ([^coll? chans timeout]
;    (async+/alts!! chans timeout))
;  ([^keyword? type chans timeout]
;    (condp = type
;      :std     (if timeout
;                   (async+/alts!! chans timeout)
;                   (async+/alts!! chans))
;      :queue   (alts!!-queue chans (or timeout Integer/MAX_VALUE))
;      :casync  (if timeout
;                   (async/alts!! chans timeout)
;                   (async/alts!! chans)))))

(declare alts!!)

; Promise, delay, future
; co.paralleluniverse.strands.channels.QueueObjectChannel : (<! (go 1)) is similar to (deref (future 1))
; CLJS doesn't have |promise| yet
#?(:clj (defalias promise core/promise #_pasync/promise))

(defalias timeout async/timeout)

#?(:clj
(defmacro wait-until
  ([pred]
    (let [max-num (if-cljs &env 'js/Number.MAX_SAFE_INTEGER 'Long/MAX_VALUE)]
      `(wait-until ~max-num ~pred)))
  ([timeout pred]
   `(loop [timeout# ~timeout]
      (if (<= timeout# 0)
          (throw (->ex :timeout (str/sp "Operation timed out after" ~timeout "milliseconds") ~timeout))
          (when-not ~pred
            (sleep 10) ; Sleeping so as not to take up thread time
            (recur (- timeout# 11)))))))) ; Takes a tiny bit more time

(defn concur
  "Executes functions @fs concurrently, waiting on the slowest one to finish.
   Returns a vector of the results.

   Note: The example should only take 2 seconds, not 3."
  {:usage '(concur #(do (println "A") (Thread/sleep 1000))
                   #(do (println "B") (Thread/sleep 2000)))}
  [& fs]
  (->> fs
       (mapv (fn [f] (go (f))))
       (mapv #(#?(:clj  async/<!!
                  :cljs async/<!) %))))




#?(:cljs (def web-workers-set-up? (atom false)))

(defn web-worker?
  "Checks whether the current thread is a WebWorker."
  []
  #?(:clj  false
     :cljs (servant/webworker?)))

#?(:cljs (defalias bootstrap-worker servant.worker/bootstrap ))

#?(:cljs
(defrecord Threadpool [thread-ct threads script-src]
  component/Lifecycle
    (start [this]
      ; Bootstrap the web workers if that hasn't been done already
      (when (and (web-worker?)
                 ((fn-and number? pos?) thread-ct)
                 (not @web-workers-set-up?))
        (log/pr :user "Bootstrapping web workers")
        ; Run the setup code for the web workers
        (bootstrap-worker)
        (reset! web-workers-set-up? true))

      ; We need to make sure that only the main thread/script will spawn the servants.
      (if (servant/webworker?)
          this
          (do (log/pr :debug "Spawning" thread-ct "-thread web-worker threadpool")
              (assoc this
                ; Returns a buffered channel of web workers
                :threads (servant/spawn-servants thread-ct script-src)))))
    (stop  [this]
      (when threads
        (log/pr :debug "Destroying" thread-ct "-thread web-worker threadpool")
        (servant/kill-servants threads thread-ct))

      this)))

#?(:cljs
(defn ->threadpool [{:keys [thread-ct script-src] :as opts}]
  (assert ((fn-or nil? integer?) thread-ct) #{thread-ct})
  (assert (string? script-src) #{script-src})

  (when (pos? thread-ct)
    (map->Threadpool opts))))

#?(:cljs (defservantfn dispatch "The global web worker dispatch fn" [f] (f)))

#?(:clj
(defmacro async
  "Evaluates @body asynchronously and returns a channel
   awaiting the result.
   Same as |go|, but named more reasonably."
  [& body]
  (let [go-impl
         (if-cljs &env 'cljs.core.async.macros/go
                       'clojure.core.async/go)]
    `(~go-impl ~@body))))

#?(:clj
(defmacro future
  "|future| for ClojureScript. A little different, though, for the following reasons:

   Since the context of each of these 'threads'/web workers is totally separate from the main (UI)
   thread, then messages passed back and forth will be all these threads know of each other.
   This is why web-worker code which attempts to generate side-effects on application state doesn't
   work or can produce strange effects.

   However, web workers can produce side-effects on e.g. local browser cache or create HTTP requests."
  [& body]
  `(servant.core/servant-thread global-threadpool
     servant.core/standard-message dispatch (fn [] ~@body))))
