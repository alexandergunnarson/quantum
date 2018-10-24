(ns quantum.untyped.core.data.reactive
  "Most of the content adapted from `reagent.ratom` 2018-10-20. Note that `lynaghk/reflex` was the
   source of the Reagent Atom and Reaction (and before that https://knockoutjs.com/documentation/computedObservables.html, and before that probably
   something else), and it makes do with 78 LOC (!) whereas we grapple with nearly 400 for
   presumably very similar functionality. Perhaps someday this code can be compressed.

   Includes `Atom` and `Reaction`; may include `Subscription` at some point.

   Currently only safe for single-threaded use; needs a rethink to accommodate concurrent
   modification/access and customizable queueing strategies.
   - We could either introduce concurrency-safe versions of `Reaction` and `Atom`, or we
     could introduce a global single thread on which `Reaction`s and `Atom`s are modified,
     but from which any number of threads can read, in a clojure.async sort of way."
        (:refer-clojure :exclude
          [atom run!])
        (:require
          [clojure.core                               :as core]
          [clojure.set                                :as set]
          [quantum.untyped.core.async                 :as uasync]
          [quantum.untyped.core.core
            :refer [dot dot!]]
          [quantum.untyped.core.error                 :as uerr]
          [quantum.untyped.core.form.generate.deftype :as udt]
          [quantum.untyped.core.log                   :as ulog]
          [quantum.untyped.core.logic
            :refer [ifs]]
          [quantum.untyped.core.vars
            :refer [defonce-]])
#?(:clj (:import [java.util ArrayList])))

;; TODO move
;; ===== Array-list fns ===== ;;

(defn- alist-get
  #?(:clj  [^ArrayList xs ^long   i]
     :cljs [           xs ^number i])
  (#?(:clj .get :cljs aget) xs i))

(defn- alist-set!
  #?(:clj  [^ArrayList xs ^long   i v]
     :cljs [           xs ^number i v])
  (#?(:clj .set :cljs aset) xs i v))

(defn- alist-conj! [#?(:clj ^ArrayList xs :cljs xs) v]
  (doto xs (#?(:clj .add :cljs .push) v)))

(defn- #?(:clj alist-count :cljs ^number alist-count) [#?(:clj ^ArrayList xs :cljs xs)]
  (#?(:clj .size :cljs alength) xs))

(defn- #?(:clj alist-empty? :cljs ^boolean alist-empty?) [#?(:clj ^ArrayList xs :cljs xs)]
  (== (#?(:clj .size :cljs alength) xs) 0))

(defn- alist-empty! [#?(:clj ^ArrayList xs :cljs xs)]
  #?(:clj (.clear xs) :cljs (set! (.-length xs) 0))
  xs)

(defn- #?(:clj alist== :cljs ^boolean alist==)
  [#?(:clj ^ArrayList x :cljs x) #?(:clj ^ArrayList y :cljs y)]
  (let [len (if (nil? x) 0 (long (alist-count x)))]
    (and (== len (if (nil? y) 0 (long (alist-count y))))
         (loop [i 0]
           (or (== i len)
               (if (identical? (alist-get x i) (alist-get y i))
                   (recur (inc i))
                   false))))))

(defn- #?(:clj ^ArrayList alist :cljs alist)
  ([]  #?(:clj (ArrayList.) :cljs #js []))
  ([x] #?(:clj (doto (ArrayList.) (.add x)) :cljs #js [x])))

;; ===== Internal functions for reactivity ===== ;;

(def ^:dynamic *atom-context* nil)

(def ^:dynamic #?(:clj *debug?* :cljs ^boolean *debug?*) false)

(defonce- *running (core/atom 0))

(defonce global-queue (alist))

(defn- check-watches [old new]
  (when (true? *debug?*) (swap! *running + (- (count new) (count old))))
  new)

(defn norx-deref [rx]
  (binding [*atom-context* nil]
    #?(:clj  (.deref ^clojure.lang.IDeref rx)
       :cljs (-deref ^non-native          rx))))

(defprotocol PWatchable
  (getWatches [this])
  (setWatches [this v]))

(defn- add-w! [^quantum.untyped.core.data.reactive.PWatchable x k f]
  (let [w (.getWatches x)]
    (.setWatches x (check-watches w (assoc w k f)))
    x))

(defn- remove-w! [^quantum.untyped.core.data.reactive.PWatchable x k]
  (let [w (.getWatches x)]
    (.setWatches x (check-watches w (dissoc w k)))
    x))

(defn- conj-kv! [#?(:clj ^ArrayList xs :cljs xs) k v]
  (-> xs (alist-conj! k) (alist-conj! v)))

(defn- notify-w! [^quantum.untyped.core.data.reactive.PWatchable x old new]
  ;; Unlike Reagent, we do not copy to an array-list because in order to do so, we have to traverse
  ;; the map anyway if the watches have changed. Plus we avoid garbage (except for the closure).
  ;; Reagent optimizes for the case that watches will more rarely change than not. It would be nice
  ;; to avoid that tradeoff by having a sufficiently fast reduction.
  (when-some [w #?(:clj ^clojure.lang.IKVReduce (.getWatches x) :cljs ^non-native (.getWatches x))]
    (#?(:clj .kvreduce :cljs -kv-reduce) w (fn [_ k f] (f k x old new)) nil))
  x)

#?(:cljs
(defn- pr-atom! [a writer opts s]
  (-write writer (str "#<" s " "))
  (pr-writer (binding [*atom-context* nil] (-deref ^non-native a)) writer opts)
  (-write writer ">")))

;; ===== Atom ===== ;;

(defprotocol PReactive)

(defprotocol PHasCaptured
  (getCaptured [this])
  (setCaptured [this v]))

(defn- notify-deref-watcher!
  "Add `derefed` to the `captured` field of `*atom-context*`.

  See also `in-context`"
  [derefed]
  (when-some [context *atom-context*]
    (let [^quantum.untyped.core.data.reactive.PHasCaptured r context]
      (if-some [c (.getCaptured r)]
        (alist-conj! c derefed)
        (.setCaptured r (alist derefed))))))

(udt/deftype Atom [^:! state meta validator ^:! watches]
  {;; IPrintWithWriter
   ;;   (-pr-writer [a w opts] (pr-atom a w opts "Atom:"))
   PReactive nil
   ?Equals {=      ([this that] (identical? this that))}
   ?Deref  {deref  ([this]
                     (notify-deref-watcher! this)
                     state)}
   ?Atom   {reset! ([a new-value]
                     (when-not (nil? validator)
                       (assert (validator new-value) "Validator rejected reference state"))
                     (let [old-value state]
                       (if (identical? old-value new-value)
                           new-value
                           (let [old-value state]
                             (set! state new-value)
                             (when-not (nil? watches)
                               (notify-w! a old-value new-value))
                             new-value))))
            swap!  (([a f]          (#?(:clj .reset :cljs -reset!) a (f state)))
                    ([a f x]        (#?(:clj .reset :cljs -reset!) a (f state x)))
                    ([a f x y]      (#?(:clj .reset :cljs -reset!) a (f state x y)))
                    ([a f x y more] (#?(:clj .reset :cljs -reset!) a (apply f state x y more))))}
   ?Watchable {add-watch!    ([this k f] (add-w!    this k f))
               remove-watch! ([this k]   (remove-w! this k))}
   PWatchable {getWatches    ([this]   watches)
               setWatches    ([this v] (set! watches v))}
   ?Meta      {meta      ([_] meta)
               with-meta ([_ meta'] (Atom. state meta' validator watches))}
#?@(:cljs [?Hash {hash    ([_] (goog/getUid this))}])})

(defn atom
  "Reactive 'atom'. Like `core/atom`, except that it keeps track of derefs."
  ([x] (Atom. x nil nil nil))
  ([x & {:keys [meta validator]}] (Atom. x meta validator nil)))

;; ===== Reaction ("Computed Observable") ===== ;;

;; Similar to java.io.Closeable
;; TODO move
(defprotocol PDisposable
  (dispose      [this])
  (addOnDispose [this f]))

(defn dispose!        [x]   (dispose      x))
(defn add-on-dispose! [x f] (addOnDispose x f))

(declare flush! run-reaction! update-watching!)

(udt/deftype Reaction
  [^:! ^boolean ^:get       alwaysRecompute
   ^:!          ^:get ^:set caught
   ^:!                      captured
   ^:! ^boolean ^:get ^:set computed
                            enqueue-fn
                            eq-fn
                            f
       ^boolean             no-cache?
   ^:!                      on-dispose
   ^:!                      on-dispose-arr
   ^:!                      on-set
                            queue
   ^:!          ^:get ^:set state
   ^:!          ^:get ^:set watching ; i.e. 'dependents'
   ^:!                      watches] ; TODO consider a mutable map for `watches`
  {;; IPrintWithWriter
   ;;   (-pr-writer [a w opts] (pr-atom a w opts (str "Reaction " (hash a) ":")))
   ?Equals {= ([this that] (identical? this that))}
#?@(:cljs [?Hash {hash ([this] (goog/getUid this))}])
   PReactive  nil
   ?Deref     {deref ([this]
                       (if-not (nil? caught)
                         (throw caught)
                         (let [non-reactive? (nil? *atom-context*)]
                           (when non-reactive? (flush! queue))
                           (if (and non-reactive? alwaysRecompute)
                               (when-not computed
                                 (let [old-state state]
                                   (set! state (f))
                                   (when-not (or (nil? watches) (eq-fn old-state state))
                                     (notify-w! this old-state state))))
                               (do (notify-deref-watcher! this)
                                   (when-not computed (run-reaction! this false))))
                           state)))}
   ?Watchable {add-watch!    ([this k f] (add-w! this k f))
               remove-watch! ([this k]
                               (let [was-empty? (empty? watches)]
                                 (remove-w! this k)
                                 (when (and (not was-empty?)
                                            (empty? watches)
                                            (true? alwaysRecompute))
                                   (.dispose this))))}
   PWatchable {getWatches ([this]   watches)
               setWatches ([this v] (set! watches v))}
   ?Atom
     {reset! ([a newv]
                (assert (fn? (.-on-set a)) "Reaction is read only; on-set is not allowed")
                (let [oldv state]
                  (set! state newv)
                  ((.-on-set a) oldv newv)
                  (notify-w! a oldv newv)
                  newv))
      swap!  (([a f]          (#?(:clj .reset :cljs -reset!) a (f (norx-deref a))))
              ([a f x]        (#?(:clj .reset :cljs -reset!) a (f (norx-deref a) x)))
              ([a f x y]      (#?(:clj .reset :cljs -reset!) a (f (norx-deref a) x y)))
              ([a f x y more] (#?(:clj .reset :cljs -reset!) a (apply f (norx-deref a) x y more))))}
  PHasCaptured
    {getCaptured ([this]   captured)
     setCaptured ([this v] (set! captured v))}
   PDisposable
     {dispose
       ([this]
         (let [s state, wg watching]
           (set! watching        nil)
           (set! state           nil)
           (set! alwaysRecompute #?(:clj (boolean true)  :cljs true))
           (set! computed        #?(:clj (boolean false) :cljs false))
           (doseq [w (set wg)] (#?(:clj remove-watch :cljs -remove-watch) w this))
           (when (some? (.-on-dispose this)) ((.-on-dispose this) s))
           (when-some [a (.-on-dispose-arr this)]
             (dotimes [i (long (alist-count a))] ((alist-get a i) this)))))
      addOnDispose
        ([this f]
          ;; f is called with the reaction as argument when it is no longer active
          (if-some [a (.-on-dispose-arr this)]
            (alist-conj! a f)
            (set! (.-on-dispose-arr this) (alist f))))}})

(defn- in-context
  "When f is executed, if (f) derefs any atoms, they are then added to
   'obj.captured'(*atom-context*).

   See function notify-deref-watcher! to know how *atom-context* is updated"
  [obj f] (binding [*atom-context* obj] (f)))

(defn- deref-capture!
  "Returns `(in-context f r)`. Calls `_update-watching` on r with any
   `deref`ed atoms captured during `in-context`, if any differ from the
   `watching` field of r. Sets the `computed` flag on r to true.

   Inside '_update-watching' along with adding the atoms in 'r.watching' of reaction,
   the reaction is also added to the list of watches on each atoms f derefs."
  [f ^Reaction rx]
  (.setCaptured rx nil)
  (let [res (in-context rx f)
        c   (.getCaptured rx)]
    (.setComputed rx true)
    ;; Optimize common case where derefs occur in same order
    (when-not (alist== c (.getWatching rx)) (update-watching! rx c))
    res))

(defn- try-capture! [^Reaction rx f]
  (uerr/catch-all
    (do (.setCaught rx nil)
        (deref-capture! f rx))
    e
    (do (.setState  rx e)
        (.setCaught rx e)
        (.setComputed rx true))))

(defn- run-reaction! [^Reaction rx check?]
  (let [old-state (.getState rx)
        new-state (if check?
                      (try-capture! rx (.-f rx))
                      (deref-capture! (.-f rx) rx))]
    (when-not (.-no-cache? rx)
      (.setState rx new-state)
      (when-not (or (nil? (.getWatches rx))
                    ((.-eq-fn rx) old-state new-state))
        (notify-w! rx old-state new-state)))
    new-state))

(defn- handle-reaction-change! [^Reaction rx sender oldv newv]
  (when-not (or (identical? oldv newv) (not (.getComputed rx)))
    (if (.getAlwaysRecompute rx)
        (do (.setComputed rx false)
            ((.-enqueue-fn rx) (.-queue rx) rx))
        (run-reaction! rx false))))

(defn- update-watching! [^Reaction rx derefed]
  (let [new (set derefed) ; TODO incrementally calculate `set`
        old (set (.getWatching rx))] ; TODO incrementally calculate `set`
    (.setWatching rx derefed)
    (doseq [w (set/difference new old)] ; TODO optimize
      (#?(:clj add-watch    :cljs -add-watch)    w rx handle-reaction-change!))
    (doseq [w (set/difference old new)] ; TODO optimize
      (#?(:clj remove-watch :cljs -remove-watch) w rx))))

(defn- run-reaction-from-queue! [^Reaction rx]
  (when-not (or (.getComputed rx) (nil? (.getWatching rx)))
    (run-reaction! rx true)))

(defn flush! [queue]
  (loop [i 0]
    (let [ct (-> queue alist-count long)]
      ;; NOTE: We avoid `pop`-ing in order to reduce churn but in theory it presents a memory issue
      ;;       due to the possible unboundedness of the queue
      ;; NOTE: In the Reagent version, every time a new "chunk" of the queue is worked on, that
      ;;       chunk is scheduled for re-render
      ;; I.e. took care of all queue entries and reached a stable state
      (if-let [reached-last-index? (>= i ct)]
        (alist-empty! queue)
        (let [remaining-ct (unchecked-subtract ct i)]
          (dotimes [i* remaining-ct]
            (run-reaction-from-queue! (alist-get queue (unchecked-add i i*))))
          ;; `recur`s because sometimes the queue gets added to in the process of running rx's
          (recur (+ i remaining-ct)))))))

(defn- default-enqueue! [queue rx]
  ;; Immediate run without touching the queue
  (run-reaction-from-queue! rx))

(def ^:dynamic *enqueue!* default-enqueue!)

(def ^:dynamic *queue* global-queue)

(defn ^Reaction >rx
  ([f] (>rx f nil))
  ([f {:keys [always-recompute? enqueue-fn eq-fn no-cache? on-set on-dispose queue]}]
    (Reaction. (if (nil? always-recompute?) false always-recompute?)
               nil
               nil
               false
               (or enqueue-fn *enqueue!*)
               (or eq-fn =)
               f
               (if (nil? no-cache?) false no-cache?)
               on-dispose
               nil
               on-set
               (or queue *queue*)
               nil nil nil)))

#?(:clj (defmacro rx [& body] `(>rx (fn [] ~@body))))

#?(:clj (defmacro eager-rx [& body] `(>rx (fn [] ~@body) {:always-recompute? true})))

#?(:clj
(defmacro run!
  "Runs body immediately, and runs again whenever atoms deferenced in the body change. Body should
   side effect."
  [& body]
  `(doto (rx ~@body) deref)))

;; ===== Track ===== ;;

(udt/deftype TrackableFn [f ^:! ^:get ^:set rxCache])

(declare cached-reaction)

;; For perf test in `quantum.test.untyped.core.data.reactive`. TODO excise?
(udt/deftype Track
  [^TrackableFn trackable-fn, args, ^:! ^:get ^:set ^quantum.untyped.core.data.reactive.Reaction rx]
  {;; IPrintWithWriter
   ;;   (-pr-writer [a w opts] (pr-atom a w opts "Track:"))
   PReactive nil
   ?Deref  {deref ([this]
                    (if (nil? rx)
                        (cached-reaction #(apply (.-f trackable-fn) args)
                          trackable-fn args this nil)
                        #?(:clj (.deref rx) :cljs (-deref ^non-native rx))))}}
   ?Equals {=     ([_ other]
                    (and (instance? Track other)
                         (-> ^Track other .-trackable-fn .-f (= (.-f trackable-fn)))
                         (-> ^Track other .-args             (= args))))}
   ?Hash   {hash  ([_] (hash [f args]))})

(defn- cached-reaction [f ^TrackableFn trackable-fn k ^Track t destroy-fn]
  (let [          m (.getRxCache trackable-fn)
                  m (if (nil? m) {} m)
        ^Reaction r (m k nil)]
    (cond
      (some? r) #?(:clj (.deref r) :cljs (-deref ^non-native r))
      (nil? *atom-context*) (f)
      :else (let [r (>rx f
                      {:on-dispose
                        (fn [x]
                          (when (true? *debug?*) (swap! *running dec))
                          (as-> (.getRxCache trackable-fn) cache
                            (dissoc cache k)
                            (.setRxCache trackable-fn cache))
                          (when (some? t)
                            (.setRx t nil))
                          (when (some? destroy-fn)
                            (destroy-fn x)))
                       ;; Inherits the queue
                       :queue (some-> t .getRx .-queue)})
                  v #?(:clj (.deref r) :cljs (-deref ^non-native r))]
              (.setRxCache trackable-fn (assoc m k r))
              (when (true? *debug?*) (swap! *running inc))
              (when (some? t)
                (.setRx t r))
              v))))

(defn ^Track >track [f args] (Track. (TrackableFn. f nil) args nil))

(defn >track! [f args opts]
  (let [t (>track f args)
        r (>rx (fn [] #?(:clj (.deref t) :cljs (-deref ^non-native t)))
               {:queue (or (:queue opts) global-queue)})]
    @r
    r))

(defn #?(:clj reactive? :cljs ^boolean reactive?) [x] (satisfies? PReactive x))
