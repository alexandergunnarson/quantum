(ns
  ^{:doc "Asynchronous and thread-related functions."
    :attribution "alexandergunnarson"}
  quantum.core.async
  (:refer-clojure :exclude
    [locking
     promise deliver, delay force, realized? future map])
  (:require
    [clojure.core                      :as core]
    [com.stuartsierra.component        :as component]
    [clojure.core.async                :as async]
    [clojure.core.async.impl.protocols :as asyncp]
#?@(#_:clj
 #_[[co.paralleluniverse.pulsar.async  :as async+]
    [co.paralleluniverse.pulsar.core   :as pasync]])
    [quantum.core.collections          :as coll
      :refer [doseqi nempty? red-for break nnil? map]]
    [quantum.core.core                 :as qcore
      :refer [istr]]
    [quantum.core.error                :as err
      :refer [->ex TODO catch-all]]
    [quantum.core.fn
      :refer [fnl]]
    [quantum.core.log                  :as log]
    [quantum.core.logic                :as logic
      :refer [fn-and fn-or fn-not condpc whenc]]
    [quantum.core.macros.core          :as cmacros
      :refer [case-env]]
    [quantum.core.macros               :as macros
      :refer [defnt]]
    [quantum.core.system               :as sys]
    [quantum.core.vars                 :as var
      :refer [defalias defmalias]]
    [quantum.core.spec                 :as s
      :refer [validate]])
  (:require-macros
    [cljs.core.async.macros            :as asyncm]
    [quantum.core.async                :as self
      :refer [go]])
#?(:clj
  (:import
    clojure.core.async.impl.channels.ManyToManyChannel
    [java.util.concurrent Future FutureTask TimeUnit]
    quantum.core.data.queue.LinkedBlockingQueue
    [co.paralleluniverse.strands.channels SendPort]
    co.paralleluniverse.strands.channels.ReceivePort
    co.paralleluniverse.fibers.Fiber
    co.paralleluniverse.strands.Strand)))

(log/this-ns)

; ===== LOCKS AND SEMAPHORES ===== ;

; `monitor-enter`, `monitor-exit`

#?(:clj (defalias locking core/locking))

; ===== CORE.ASYNC ETC. ===== ;

#?(:clj (defmalias go    clojure.core.async/go cljs.core.async.macros/go))
#?(:clj (defalias  async go))

#?(:clj
(defmacro <?
  "Takes a value from a core.async channel, throwing the value if it
   is a js/Error or Throwable."
  [expr]
 `(let [expr-result# (<! ~expr)]
    (if (err/error? expr-result#)
        (throw expr-result#)
        expr-result#))))

#?(:clj
(defmacro try-go [& body] `(async (catch-all (do ~@body) e# e#))))

(deftype QueueCloseRequest [])
(deftype TerminationRequest [])

(defalias buffer async/buffer)

; TODO (SynchronousQueue.) <-> (chan)
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

; ----- PROMISE ----- ;

(defalias promise-chan async/promise-chan)
; co.paralleluniverse.strands.channels.QueueObjectChannel : (<! (go 1)) is similar to (deref (future 1))
; CLJS doesn't have `promise` yet
#?(:clj (defalias promise core/promise #_pasync/promise))
#?(:clj (defalias deliver core/deliver))

; ----- DELAY ----- ;

(defalias delay core/delay)
(defalias force core/force)

(defalias timeout async/timeout)

#?(:clj (defalias thread async/thread))

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

        (defalias <!  async/<!)
#?(:clj (defalias <!! async/<!!))

(defalias take! async/take!)

(declare empty!)

#?(:clj
(defnt empty!
#?(:clj
  ([^LinkedBlockingQueue q] (.clear q)))
  ([^m2m-chan?           c] (TODO))))

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

(def close-req? (fnl instance? QueueCloseRequest))

(declare peek!!)

#?(:clj
(defnt peek!!
  "Blocking peek."
#?@(:clj
 [([^LinkedBlockingQueue q]         (.blockingPeek q))
  ([^LinkedBlockingQueue q timeout] (.blockingPeek q timeout (. TimeUnit MILLISECONDS)))])
  ([^m2m-chan?           c] (TODO))))

(declare interrupt!)

#?(:clj
(defnt interrupt!
  ([#{Thread}         x] (.interrupt x)) ; `join` after interrupt doesn't work
  ([#{Process Future} x] nil))) ; .cancel?

#?(:clj
(defnt interrupted?*
  ([#{Thread Strand}  x] (.isInterrupted x))
  ([#{Process Future} x] (TODO))))

;#?(:clj
;(defn interrupted?
;  ([ ] (.isInterrupted ^Strand (current-strand)))
;  ([x] (interrupted?* x))))

(declare interrupted?)

#?(:clj
(defnt close!
  ([^Thread              x] (.stop    x))
  ([^Process             x] (.destroy x))
  ([#{Future FutureTask} x] (.cancel x true))
  ([^default             x] (if (nil? x) true (TODO)))
  ([^ManyToManyChannel   x] (asyncp/close! x))
  ([#{LinkedBlockingQueue ReceivePort SendPort} x] (.close x))))

#?(:clj
(defnt closed?
  ([^Thread                            x] (not (.isAlive x)))
  ([^Process                           x] (try (.exitValue x) true
                                            (catch IllegalThreadStateException _ false)))
  ([#{Fiber Future FutureTask}         x] (or (.isCancelled x) (.isDone x)))
  ([#{LinkedBlockingQueue ReceivePort} x] (.isClosed x))
  ([^ManyToManyChannel                 x] (asyncp/closed? x))
  ([^boolean                           x] x)
  ([^default                           x] (if (nil? x) true (TODO)))))

#?(:clj (def open? (fn-not closed?)))

#?(:clj
(defnt realized?
  ([^clojure.lang.IPending x] (realized? x))
  #_([^co.paralleluniverse.strands.channels.QueueObjectChannel x] ; The result of a Pulsar go-block
    (-> x .getQueueLength (> 0)))
  ([#{Future FutureTask Fiber} x] (.isDone x))))

#?(:clj
(defmacro wait!
  "`wait` within a `go` block"
  [millis]
  `(<! (timeout ~millis))
  #_(case-env
    :clj
    #_(if (Fiber/currentFiber)
          (Fiber/sleep  ~millis)
          (Strand/sleep ~millis)))))

#?(:clj (defnt wait!! "Blocking wait" [^long millis] (Thread/sleep millis)))

; MORE COMPLEX OPERATIONS

; ----- ALTS ----- ;

(defalias alts! async/alts!)

; For some reason, having lots of threads with core.async/alts!! "clogs the tubes", as it were
; Possibly because of deadlocking?
; So we're moving away from core.async, but keeping the same concepts
#?(:clj
(defn alts!!-queue [chans timeout] ; Unable to mark ^:suspendable because of synchronization
  (loop []
    (let [result (red-for [c   chans
                           ret nil]
                   (locking c ; Because it needs to have a consistent view of when it's empty and take accordingly
                     (when (nempty? c)
                       (break [(take!! c) c]))))]
      (whenc result nil?
        (do (wait!! 5)
            (recur)))))))

#?(:clj
(defnt alts!!
  "Takes the first available value from a chan."
  {:todo #{"Implement timeout"}
   :attribution "alexandergunnarson"}
  ([^keyword? type chans]
    (alts!! type chans nil))
  #_([^sequential? chans]
    (async+/alts!! chans))
  #_([^sequential? chans timeout]
    (async+/alts!! chans timeout))
  ([^keyword? type chans timeout]
    (case type
      ;:std     (if timeout
      ;             (async+/alts!! chans timeout)
      ;             (async+/alts!! chans))
      :queue   (alts!!-queue chans (or timeout Integer/MAX_VALUE))
      :casync  (if timeout
                   (async/alts!! chans timeout)
                   (async/alts!! chans))))))

; ----- FUTURE ----- ;

#?(:clj
(defmacro future
  "`future` for Clojure aliases `clojure.core/future`.

   For ClojureScript, obviously there is no `cljs.core/future`, but this replicates some of the
   behavior of `clojure.core/future`, with some differences:

   Since the context of each of these 'threads'/web workers is totally separate from the main (UI)
   thread, then messages passed back and forth will be all these threads know of each other.
   This is why web-worker code that attempts to generate side-effects on application state doesn't
   work or can produce strange effects.

   However, web workers can produce side-effects on e.g. local browser cache or create HTTP requests."
  [& body]
  (case-env
    :clj  `(clojure.core/future ~@body)
    :cljs `(servant.core/servant-thread global-threadpool
             servant.core/standard-message dispatch (fn [] ~@body)))))

; TODO incorporate
; future-call #'clojure.core/future-call,
; future-cancel #'clojure.core/future-cancel,
; future-cancelled? #'clojure.core/future-cancelled?,
; future-done? #'clojure.core/future-done?,

; ----- THREAD-LOCAL ----- ;

#?(:clj
(defn thread-local*
  {:from "flatland.useful.utils"}
  [init]
  (let [generator (proxy [ThreadLocal] []
                    (initialValue [] (init)))]
    (reify clojure.lang.IDeref
      (deref [this] (.get generator))))))

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

; ----- MISC ----- ;

#?(:clj
(defmacro seq<!
  "Given `ports`, a seq, calls `<!` on each one in turn.
   Lets each port initiate — if e.g. `go` blocks, will
   initiate concurrently. Aggregates the results into a vector."
  [ports]
  `(let [ret# (transient [])]
     (doseq [p# ~ports] (conj! ret# (<! p#)))
     (persistent! ret#))))

#?(:clj (defn seq<!! [ports] (map <!! ports)))

(defalias timeout async/timeout)

#?(:clj
(defmacro wait-until*
  "Waits until the value of `pred` becomes truthy."
  ([sleepf pred]
    (let [max-num (case-env :clj 'Long/MAX_VALUE :cljs 'js/Number.MAX_SAFE_INTEGER)]
      `(wait-until* ~sleepf ~max-num ~pred)))
  ([sleepf timeout pred]
   `(loop [timeout# ~timeout]
      (if (<= timeout# 0)
          (throw (->ex :timeout ~(istr "Operation timed out after ~{timeout} milliseconds") ~timeout))
          (when-not ~pred
            (~sleepf 10) ; Sleeping so as not to take up thread time
            (recur (- timeout# 11)))))))) ; Takes a tiny bit more time

#?(:clj (defmacro wait-until!  [& args] `(wait-until* sleep!  ~@args)))
#?(:clj (defmacro wait-until!! [& args] `(wait-until* sleep!! ~@args)))

#?(:clj
(defmacro try-times* [waitf max-n wait-millis & body]
 `(let [max-n#       ~max-n
        wait-millis# ~wait-millis]
    (loop [n# 0 error-n# nil]
      (if (> n# max-n#)
          (throw (->ex :max-tries-exceeded nil
                       {:tries n# :last-error error-n#}))
          (let [[error# result#]
                  (try [nil (do ~@body)]
                    (catch ~(err/generic-error &env) e#
                      (~waitf wait-millis#)
                      [e# nil]))]
            (if error#
                (recur (inc n#) error#)
                result#)))))))

#?(:clj (defmacro try-times!  [& args] `(try-times* wait!  ~@args)))
#?(:clj (defmacro try-times!! [& args] `(try-times* wait!! ~@args)))

(defn web-worker?
  "Checks whether the current thread is a WebWorker."
  []
  #?(:clj  false
     :cljs (and (-> js/window .-self)
                (-> js/window .-self .-document undefined?)
                (or (nil? sys/os) (= sys/os "web")))))

#_(:clj
(defn chunk-doseq
  "Like `fold` but for `doseq`.
   Also configurable by thread names and threadpool, etc."
  [coll {:keys [total thread-count chunk-size threadpool thread-name chunk-fn] :as opts} f]
  (let [total-f (or total (count coll))
        chunks (coll/partition-all (or chunk-size
                                       (/ total-f
                                          (min total-f (or thread-count 10))))
                 (if total (take total coll) coll))]
    (doseqi [chunk chunks i]
      (let [thread-id (keyword (str thread-name "-" i))]
        (async (mergel {:id thread-id} opts)
          ((or chunk-fn fn-nil) chunk i)
          (doseqi [piece chunk n]
            (f piece n chunk i chunks))))))))
