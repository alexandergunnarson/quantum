(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"}
  quantum.core.async
  (:refer-clojure :exclude [promise realized? future])
  (:require
    [clojure.core                     :as core]
    [com.stuartsierra.component       :as component]
    [clojure.core.async               :as async]
    [clojure.core.async.impl.protocols :as asyncp]
#?@(#_:clj
 #_[[co.paralleluniverse.pulsar.async :as async+]
    [co.paralleluniverse.pulsar.core  :as pasync]]
    :cljs
   [[servant.core                     :as servant]])
    [quantum.core.core                :as qcore]
    [quantum.core.error               :as err
      :refer [->ex TODO catch-all]]
    [quantum.core.collections         :as coll
      :refer [nempty? seq-loop break nnil?]]
    [quantum.core.log                 :as log]
    [quantum.core.logic               :as logic
      :refer [fn-and fn-or fn-not condpc whenc]]
    [quantum.core.macros.core         :as cmacros
      :refer [case-env]]
    [quantum.core.macros              :as macros
      :refer [defnt]]
    [quantum.core.system              :as sys]
    [quantum.core.vars                :as var
      :refer  [defalias defmalias]]
    [quantum.core.spec                :as s
      :refer [validate]])
  (:require-macros
    [servant.macros                   :as servant
      :refer [defservantfn]                        ]
    [cljs.core.async.macros           :as asyncm   ]
    [quantum.core.async               :as self
      :refer [go]])
#?(:clj
  (:import
    clojure.core.async.impl.channels.ManyToManyChannel
    (java.util.concurrent TimeUnit)
    quantum.core.data.queue.LinkedBlockingQueue
    #_co.paralleluniverse.fibers.Fiber
    #_co.paralleluniverse.strands.Strand)))

(log/this-ns)

; Use optimal/maximum core async pool size

#?(:clj (System/setProperty
          "clojure.core.async.pool-size"
          (str (.. Runtime getRuntime availableProcessors))))

#?(:clj (defmalias go    clojure.core.async/go cljs.core.async.macros/go))
#?(:clj (defalias  async go))

#?(:clj
(defmacro <?
  "Takes a value from a core.async channel, throwing the value if it
   is a js/Error or Throwable."
  [expr]
  (let [err-class (case-env :clj 'Throwable :cljs 'js/Error)]
   `(let [expr-result# (<! ~expr)]
      (if ;(quantum.core.type/error? expr-result#)
          (instance? ~err-class expr-result#)
          (throw expr-result#)
          expr-result#)))))

#?(:clj
(defmacro try-go
  [& body]
  `(asyncm/go (catch-all (do ~@body) e# e#))))

(deftype QueueCloseRequest [])
(deftype TerminationRequest [])

(defalias buffer async/buffer)

(defnt chan*
  "(chan (buffer n)) or (chan n) are the same as (channel n :block   ) or (channel n).
   (chan (dropping-buffer n))    is  the same as (channel n :drop    )
   (chan (sliding-buffer n))     is  the same as (channel n :displace)

   Promises can be implemented in terms of |chan|:

   (let [a (promise)]
     (deliver a 123))

   (let [c (chan)]
     (>! c 123)" ; But then close chan
  ;([] (async+/chan)) ; can't have no-arg |defnt|
  ([#{#?(:clj integer? :cljs number?) #?(:clj clojure.core.async.impl.buffers.FixedBuffer)} n]
   (async/chan n)
   #_(async+/chan n))
  ([^keyword? type]
    (case type
      #_:std     #_(async+/chan)
      :queue   #?(:clj (LinkedBlockingQueue.)
                  :cljs (TODO))
      :casync  (async/chan)))
  ([^keyword? type n]
    (case type
     #_:std     #_(async+/chan n)
      :casync  (async/chan n)
      :queue   #?(:clj  (LinkedBlockingQueue. ^Integer n)
                  :cljs (TODO))))) ; TODO reflection here

(defn chan
  ([         ] (async/chan) #_(async+/chan))
  ([arg0     ] (chan* arg0     ))
  ([arg0 arg1] (chan* arg0 arg1)))

(defalias promise-chan async/promise-chan)

(defalias timeout async/timeout)

;(defn current-strand [] (Strand/currentStrand))
#?(:clj (defn current-strand [] (Thread/currentThread)))
;(defn current-fiber  [] (or (Fiber/currentFiber) (current-strand)))

;(defalias buffer              #?(:clj async+/buffer              :cljs async/buffer             ))
;(defalias dropping-buffer     #?(:clj async+/dropping-buffer     :cljs async/dropping-buffer    ))
;(defalias sliding-buffer      #?(:clj async+/sliding-buffer      :cljs async/sliding-buffer     ))
;(defalias unblocking-buffer?  #?(:clj async+/unblocking-buffer?  :cljs async/unblocking-buffer? ))

(declare take!!)

; TODO FIX THIS
#?(:clj (defnt take!! ; receive
#?@(:clj
 [([^LinkedBlockingQueue q  ] (.take q))
  ([^LinkedBlockingQueue q n] (.poll q n TimeUnit/MILLISECONDS))])
  ([^m2m-chan?           c  ] (async/take! c identity))
  ([^m2m-chan?           c n] (async/alts! [(async/timeout n) c]))
  #_([^co.paralleluniverse.strands.channels.ReceivePort   c  ] (async+/<! c))))

;(defalias <!! take!!)

#?(:clj (defalias <!! async/<!!))

(defalias take! async/take!)

#?(:clj (defmacro <! [& args] `(~(case-env :cljs 'cljs.core.async/<! 'clojure.core.async/<!) ~@args)))

(declare empty!)

#?(:clj
(defnt empty!
#?(:clj
  ([^LinkedBlockingQueue q] (.clear q)))
  ([^m2m-chan?           c] (throw (->ex :unimplemented)))))

(defalias put! async/put!)

(declare put!!)

#?(:clj (defalias >!! async/>!!))

(defalias offer! async/offer!)

#?(:clj
(defnt put!! ; send
#?(:clj
  ([^LinkedBlockingQueue x obj] (.put x obj)))
  ([^m2m-chan?           x obj] (async/put! x obj))
  #_([^co.paralleluniverse.strands.channels.ReceivePort   x obj] (async+/>! x obj))))

;(defalias >!! put!!)

(defalias >! async/>!)

(defnt message?
  ([^quantum.core.async.QueueCloseRequest  obj] false)
  ([^quantum.core.async.TerminationRequest obj] false)
  ([                    obj] (when (nnil? obj) true)))

(def close-req? #(instance? QueueCloseRequest %))

(declare peek!!)

#?(:clj
(defnt peek!!
  "Blocking peek."
#?@(:clj
 [([^LinkedBlockingQueue q]         (.blockingPeek q))
  ([^LinkedBlockingQueue q timeout] (.blockingPeek q timeout (. TimeUnit MILLISECONDS)))])
  ([^m2m-chan?           c] (throw (->ex :not-implemented "Not yet implemented." nil)))))

(declare interrupt!)

#?(:clj
(defnt interrupt!
  ([#{Thread}         x] (.interrupt x)) ; /join/ after interrupt doesn't work
  ([#{Process java.util.concurrent.Future} x] nil))) ; .cancel?

#?(:clj
(defnt interrupted?*
  ([#{Thread #_co.paralleluniverse.strands.Strand}         x] (.isInterrupted x))
  ([#{Process java.util.concurrent.Future} x] (throw (->ex :not-implemented "Not yet implemented." nil)))))

;#?(:clj
;(defn interrupted?
;  ([ ] (.isInterrupted ^Strand (current-strand)))
;  ([x] (interrupted?* x))))

(declare interrupted?)

(declare close!)

#?(:clj
(defnt close!
  ([^Thread                      x] (.stop    x))
  ([^Process                     x] (.destroy x))
  ([#{java.util.concurrent.Future
      java.util.concurrent.FutureTask} x] (.cancel x true))
  ([                             x] (if (nil? x) true (throw :not-implemented)))
  ([^quantum.core.data.queue.LinkedBlockingQueue        x] (.close x))
  ([^clojure.core.async.impl.channels.ManyToManyChannel x] (asyncp/close! x))
  ([^co.paralleluniverse.strands.channels.SendPort      x] (.close x))
  ([^co.paralleluniverse.strands.channels.ReceivePort   x] (.close x))))

(declare closed?)

#?(:clj
(defnt closed?
  ([^Thread x] (not (.isAlive x)))
  ([^Process x] (try (.exitValue x) true
                   (catch IllegalThreadStateException _ false)))
  ([#{java.util.concurrent.Future
      java.util.concurrent.FutureTask
      #_co.paralleluniverse.fibers.Fiber} x] (or (.isCancelled x) (.isDone x)))
  ([^quantum.core.data.queue.LinkedBlockingQueue        x] (.isClosed x))
  ([^clojure.core.async.impl.channels.ManyToManyChannel x] (asyncp/closed? x))
  #_([^co.paralleluniverse.strands.channels.ReceivePort   x] (.isClosed x))
  ([^boolean? x] x)
  ([x] (if (nil? x) true (throw (->ex :not-implemented))))))

#?(:clj (def open? (fn-not closed?)))

#?(:clj
(defnt realized?
  ([^clojure.lang.IPending x] (realized? x))
  #_([^co.paralleluniverse.strands.channels.QueueObjectChannel x] ; The result of a Pulsar go-block
    (-> x .getQueueLength (> 0)))
  ([#{java.util.concurrent.Future
      java.util.concurrent.FutureTask
      #_co.paralleluniverse.fibers.Fiber} x] (.isDone x))))

#?(:clj
(defmacro sleep
  "Macro because CLJS needs |<!| to be within a |go| block not
   just in compiled form, but in uncompiled (syntactic) form.

   Never use Thread/sleep in a go block. Use |(<! (timeout <msec>))|.
   Never use Thread/sleep in a fiber or it will generate constant warnings.

   Never use |sleep| without marking the enclosing function |suspendable!| (and probably all other
   functions that call it...)."
  [millis]
  (case-env
    :clj  `(Thread/sleep ~millis)
    :cljs `(<! (timeout ~millis))
    #_(if (Fiber/currentFiber)
          (Fiber/sleep  ~millis)
          (Strand/sleep ~millis)))))

; MORE COMPLEX OPERATIONS

; For some reason, having lots of threads with core.async/alts!! "clogs the tubes", as it were
; Possibly because of deadlocking?
; So we're moving away from core.async, but keeping the same concepts
#?(:clj
(defn alts!!-queue [chans timeout] ; Unable to mark ^:suspendable because of synchronization
  (loop []
    (let [result (seq-loop [c   chans
                            ret nil]
                   (locking c ; Because it needs to have a consistent view of when it's empty and take accordingly
                     (when (nempty? c)
                       (break [(take!! c) c]))))]
      (whenc result nil?
        (do (sleep 5)
            (recur)))))))

(declare alts!!)

#?(:clj
(defnt alts!!
  "Takes the first available value from a chan."
  {:todo #{"Implement timeout"}
   :attribution "Alex Gunnarson"}
  ([^keyword? type chans]
    (alts!! type chans nil))
  #_([^coll? chans]
    (async+/alts!! chans))
  #_([^coll? chans timeout]
    (async+/alts!! chans timeout))
  ([^keyword? type chans timeout]
    (condp = type
      ;:std     (if timeout
      ;             (async+/alts!! chans timeout)
      ;             (async+/alts!! chans))
      :queue   (alts!!-queue chans (or timeout Integer/MAX_VALUE))
      :casync  (if timeout
                   (async/alts!! chans timeout)
                   (async/alts!! chans))))))

; Promise, delay, future
; co.paralleluniverse.strands.channels.QueueObjectChannel : (<! (go 1)) is similar to (deref (future 1))
; CLJS doesn't have |promise| yet
#?(:clj (defalias promise core/promise #_pasync/promise))

(defalias timeout async/timeout)

#?(:clj
(defmacro wait-until
  ([pred]
    (let [max-num (case-env :clj 'Long/MAX_VALUE :cljs 'js/Number.MAX_SAFE_INTEGER)]
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
     :cljs (and (-> js/window .-self)
                (-> js/window .-self .-document undefined?)
                (or (nil? sys/os) (= sys/os "web")))))

#?(:cljs (defalias bootstrap-worker servant.worker/bootstrap ))

#?(:cljs
(defrecord Threadpool [thread-ct threads script-src]
  component/Lifecycle
    (start [this]
      ; Bootstrap the web workers if that hasn't been done already
      (let [thread-ct (or thread-ct 2)]
        (when (and (web-worker?)
                   ((fn-and integer? pos?) thread-ct)
                   (not @web-workers-set-up?))
          (log/pr :user "Bootstrapping web workers")
          ; Run the setup code for the web workers
          (bootstrap-worker)
          (reset! web-workers-set-up? true))

        ; We need to make sure that only the main thread/script will spawn the servants.
        (if (servant/webworker?)
            this
            (do (log/pr :debug "Spawning" thread-ct "-thread web-worker threadpool")
                (validate script-src string?
                          thread-ct  (s/and integer? pos?))
                (assoc this
                  ; Returns a buffered channel of web workers
                  :threads (servant/spawn-servants thread-ct script-src))))))
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

#?(:cljs (swap! qcore/registered-components assoc ::threadpool
            #(component/using (->threadpool %) [::log/log])))

#?(:cljs (defservantfn dispatch "The global web worker dispatch fn" [f] (f)))

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


#?(:clj
(defn thread-local*
  {:from "flatland.useful.utils"}
  [init]
  (let [generator (proxy [ThreadLocal] []
                    (initialValue [] (init)))]
    (reify clojure.lang.IDeref
      (deref [this]
        (.get generator))))))

#?(:clj
(defmacro thread-local
  "Takes a body of expressions, and returns a java.lang.ThreadLocal object.
   (see http://download.oracle.com/javase/6/docs/api/java/lang/ThreadLocal.html).
   To get the current value of the thread-local binding, you must deref (@) the
   thread-local object. The body of expressions will be executed once per thread
   and future derefs will be cached."
  {:from "flatland.useful.utils"}
  [& body]
  `(thread-local* (fn [] ~@body))))
