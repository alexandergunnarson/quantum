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
      :refer [fnl fn1]]
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

#?(:clj (defmalias go      clojure.core.async/go      cljs.core.async.macros/go))
#?(:clj (defmalias go-loop clojure.core.async/go-loop cljs.core.async.macros/go-loop))
#_(:clj (defalias  async go)) ; TODO fix this

#?(:clj
(defmacro <?*
  "Takes a value from a core.async channel, throwing the value if it
   is an error."
  [c takef]
 `(let [result# (~takef ~c)]
    (if (err/error? result#)
        (throw result#)
        result#))))

#?(:clj (defmacro <!?  [c] `(<?* ~c <!)))
#?(:clj (defmacro <!!? [c] `(<?* ~c <!!)))

#?(:clj (defmacro try-go [& body] `(go (catch-all (do ~@body) e# e#))))

(deftype QueueCloseRequest [])
(deftype TerminationRequest [])

(defalias buffer async/buffer)

; TODO (SynchronousQueue.) <-> (chan)
(defnt chan*
  "(chan (buffer n)) or (chan n) are the same as (channel n :block   ) or (channel n).
   (chan (dropping-buffer n))    is  the same as (channel n :drop    )
   (chan (sliding-buffer n))     is  the same as (channel n :displace)"
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

(defalias poll! async/poll!)

(defalias take! async/take!)

(defalias <! async/<!)

#?(:clj
(defnt <!! ; receive
  ([^LinkedBlockingQueue x  ] (.take x))
  ([^LinkedBlockingQueue x n] (.poll x n TimeUnit/MILLISECONDS))
  ([^default             x  ] (async/<!! x))
  ([^default             x n] (first (async/alts!! [x (timeout n)])))
  #_([^co.paralleluniverse.strands.channels.ReceivePort   c  ] (async+/<! c))))

#?(:clj
(defnt empty!
  ([^LinkedBlockingQueue q] (.clear q))
  ([^m2m-chan?           c] (TODO)))) ; `drain!` TODO

(defalias offer! async/offer!)

(defalias put! async/put!)

(defalias >! async/>!)

#?(:clj
(defnt >!! ; send
  ([^LinkedBlockingQueue x v] (.put x v))
  ([^default             x v] (async/>!! x v))
  #_([^co.paralleluniverse.strands.channels.ReceivePort   x obj] (async+/>! x obj))))

(defnt message?
  ([^quantum.core.async.QueueCloseRequest  obj] false)
  ([^quantum.core.async.TerminationRequest obj] false)
  ([                    obj] (when (nnil? obj) true)))

(def close-req? (fnl instance? QueueCloseRequest))

(declare peek!!)

#?(:clj
(defnt peek!!
  "Blocking peek."
  ([^LinkedBlockingQueue q]         (.blockingPeek q))
  ([^LinkedBlockingQueue q timeout] (.blockingPeek q timeout (. TimeUnit MILLISECONDS)))
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

#?(:clj ; TODO CLJS
(defnt close!
  ([^Thread              x] (.stop    x))
  ([^Process             x] (.destroy x))
  ([#{Future FutureTask} x] (.cancel x true))
  ([^default             x] (asyncp/close! x))
  ([#{LinkedBlockingQueue ReceivePort SendPort} x] (.close x))))

#?(:clj ; TODO CLJS
(defnt closed?
  ([^Thread                            x] (not (.isAlive x)))
  ([^Process                           x] (try (.exitValue x) true
                                            (catch IllegalThreadStateException _ false)))
  ([#{Fiber Future FutureTask}         x] (or (.isCancelled x) (.isDone x)))
  ([#{LinkedBlockingQueue ReceivePort} x] (.isClosed x))
  ([^boolean                           x] x)
  ([^default                           x] (asyncp/closed? x))))

#?(:clj (def open? (fn-not closed?))) ; TODO CLJS

#?(:clj
(defnt realized? ; TODO CLJS
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
                       (reduced [(<!! c) c]))))]
      (whenc result nil?
        (do (wait!! 5)
            (recur)))))))

#?(:clj
(defnt alts!!
  "Takes the first available value from a chan."
  {:todo #{"Implement timeout"}
   :attribution "alexandergunnarson"}
  ([chans] (async/alts!! chans))
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

#?(:clj (defn seq<!! [ports] (map (fn1 <!!) ports)))

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

; ----- PROMISE ----- ;

; TODO CLJS
(deftype Promise [c]
  #?@(:clj [clojure.lang.IDeref
              (deref [_] (<!! c))
            clojure.lang.IBlockingDeref
              (deref [_ timeout-ms timeout-val]
                (let [[v _] (alts!! [c (timeout timeout-ms)])]
                  (or v timeout-val)))])
            clojure.lang.IPending
              (isRealized [_] (some? (poll! c)))
            clojure.lang.IFn ; deliver
              (invoke [_ x] (offer! c x))
            asyncp/ReadPort
              (take! [_ handler] (asyncp/take! c handler))
            asyncp/WritePort
              (put! [_ x handler] (asyncp/put! c x handler))
            asyncp/Channel
              (close! [_] (asyncp/close! c))
              (closed? [_] (asyncp/closed? c)))

(defn promise
  "A cross between a clojure `promise` and an `async/promise-chan`."
  ([] (promise nil))
  ([xf] (promise xf nil))
  ([xf ex-handler] (Promise. (async/promise-chan xf ex-handler))))

(def promise? (fnl instance? Promise)) ; TODO what about Clojure promises or JS built-in ones?

(defnt deliver [^Promise p v] (>!! p v))

(defnt request-stop! [^Promise p] (offer! p true))

; ----- PIPING ----- ;

(defn pipe!*
  "Like `pipe`, but returns the `go-loop` created by `pipe!` instead of the `to` chan,
   and allows for arbitrary stopping of the pipe via the promise(-chan) that is returned."
  ([from to       ] (pipe!* from to true))
  ([from to close?]
    (let [stop (promise)]
      (go-loop []
        (let [v (<! from)]
          (if (or (nil? v) (and stop (some? (poll! stop))))
              (when close? (async/close! to)) ; TODO issues with `async/close!` here
              (when (>! to v)
                (recur)))))
      stop)))

; TODO CLJS
#?(:clj
(defn pipeline!*
  "Exactly the same as `pipeline` but the arguments are reordered in a more reasonable
   way, must pass a kind `#{:!! :blocking :compute :! :async}`, and allows for arbitrary
   stopping of the pipeline via the promise(-chan) that is returned.

   When stopped, will not continue to take from `from` chan, but will finish up
   pending jobs before terminating."
  ([kind conc from xf to                  ] (pipeline!* kind conc from xf to true))
  ([kind conc from xf to close?           ] (pipeline!* kind conc from xf to close? nil))
  ([kind conc from xf to close? ex-handler]
     (assert (pos? conc))
     (let [stop (promise)
           ex-handler
             (or ex-handler
                 (fn [ex]
                   (-> (Thread/currentThread)
                       .getUncaughtExceptionHandler
                       (.uncaughtException (Thread/currentThread) ex))
                   nil))
           jobs    (chan conc)
           results (chan conc)
           process (fn [[v p :as job]]
                     (if (nil? job)
                         (do (close! results) nil)
                         (let [res (async/chan 1 xf ex-handler)] ; TODO async/chan -> chan
                           (>!! res v)
                           (close! res)
                           (put! p res)
                           true)))
           async (fn [[v p :as job]]
                   (if (nil? job)
                       (do (close! results) nil)
                       (let [res (chan 1)]
                         (xf v res)
                         (put! p res)
                         true)))]
       (dotimes [_ conc]
         (case kind
           (:!! :blocking) (thread
                             (let [job (<!! jobs)]
                               (when (process job)
                                 (recur))))
           :compute        (go-loop []
                             (let [job (<! jobs)]
                               (when (process job)
                                 (recur))))
           (:! :async)     (go-loop []
                             (let [job (<! jobs)]
                               (when (async job)
                                 (recur))))))
       (go-loop []
         (let [v (<! from)]
           (if (or (nil? v) (and stop (some? (poll! stop))))
               (async/close! jobs) ; TODO fix this to use this ns/`close!`
               (let [p (chan 1)]
                 (>! jobs [v p])
                 (>! results p)
                 (recur)))))
       (go-loop []
         (let [p (<! results)]
           (if (nil? p)
               (when close? (async/close! to)) ; TODO fix this to use this ns/`close!`
               (let [res (<! p)]
                 (loop []
                   (let [v (<! res)]
                     (when (and (not (nil? v)) (>! to v))
                       (recur))))
                 (recur)))))
       stop))))

; TODO CLJS
#?(:clj
(defn concur-each!*
  "Concurrent processing of a source chan for side effects à la `each`.
   Similar to `pipeline!*`, but does not offload the results of the concurrent
   processing of the source chan onto a sink chan."
  {:todo #{"Refactor code shared with `pipeline!*` into somewhere else"}}
  ([kind conc from xf           ] (concur-each!* kind conc from xf nil))
  ([kind conc from xf ex-handler]
    (assert (pos? conc))
    (let [stop (promise)
          ex-handler
            (or ex-handler
                (fn [ex]
                  (-> (Thread/currentThread)
                      .getUncaughtExceptionHandler
                      (.uncaughtException (Thread/currentThread) ex))
                  nil))
          jobs    (chan conc)
          process (fn [[v p :as job]]
                    (if (nil? job)
                        nil
                        (let [res (async/chan 1 xf ex-handler)]  ; TODO async/chan -> chan
                          (>!! res v)
                          (close! res)
                          (put! p res)
                          true)))
          async   (fn [[v p :as job]]
                    (if (nil? job)
                        nil
                        (let [res (chan 1)]
                          (xf v res)
                          (put! p res)
                          true)))]
      (dotimes [_ conc]
        (case kind
          (:!! :blocking) (thread
                            (let [job (<!! jobs)]
                              (when (process job) (recur))))
          :compute        (go-loop []
                            (let [job (<! jobs)]
                              (when (process job) (recur))))
          (:! :async)     (go-loop []
                            (let [job (<! jobs)]
                              (when (async job) (recur))))))
      (go-loop []
        (let [v (<! from)]
          (if (or (nil? v) (and stop (some? (poll! stop))))
              (async/close! jobs) ; TODO fix this to use this ns/`close!`
              (let [p (chan 1)]
                (>! jobs [v p])
                (recur)))))
      stop))))
