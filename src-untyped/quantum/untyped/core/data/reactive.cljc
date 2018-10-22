(ns quantum.untyped.core.data.reactive
  "Most of the content adapted from `reagent.ratom` 2018-10-20.

   Includes ReactiveAtom and Reaction; will include Subscription.

   Currently only safe for single-threaded use; needs a rethink to accommodate concurrent
   modification/access and customizable queueing strategies.
   - We could either introduce concurrency-safe versions of `Reaction` and `ReactiveAtom`, or we
     could introduce a global single thread on which `Reaction`s and `ReactiveAtom`s are modified,
     but from which any number of threads can read, in a clojure.async sort of way."
        (:refer-clojure :exclude
          [run!])
        (:require
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

;; TODO add subscriptions to this too; for now we will just use the Reagent ratom
;; TODO the update batching is very Reagent-specific; we need to abstract that for it to work in CLJS

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

(def ^:dynamic *ratom-context* nil)

(def ^:dynamic #?(:clj *debug?* :cljs ^boolean *debug?*) false)

(defonce- *running (atom 0))

(defonce global-queue (alist))

(defn #?(:clj reactive? :cljs ^boolean reactive?) [] (some? *ratom-context*))

(defn- check-watches [old new]
  (when (true? *debug?*) (swap! *running + (- (count new) (count old))))
  new)

(defprotocol PWatchable
  (getWatches    [this])
  (setWatches    [this v])
  (getWatchesArr [this])
  (setWatchesArr [this v]))

(defn- add-w! [^quantum.untyped.core.data.reactive.PWatchable this k f]
  (let [w (.getWatches this)]
    (.setWatches    this (check-watches w (assoc w k f)))
    (.setWatchesArr this nil)))

(defn- remove-w! [^quantum.untyped.core.data.reactive.PWatchable this k]
  (let [w (.getWatches this)]
    (.setWatches    this (check-watches w (dissoc w k)))
    (.setWatchesArr this nil)))

(defn- conj-kv! [#?(:clj ^ArrayList xs :cljs xs) k v]
  (-> xs (alist-conj! k) (alist-conj! v)))

(defn- notify-w! [^quantum.untyped.core.data.reactive.PWatchable this old new]
  (let [w (.getWatchesArr this)
        #?(:clj ^ArrayList a :cljs a)
          (if (nil? w)
              ;; Copy watches to array-list for speed
              (->> (.getWatches this)
                   (reduce-kv conj-kv! (alist))
                   (.setWatchesArr this))
              w)]
    (let [len (long (alist-count a))]
      (loop [i (int 0)]
        (when (< i len)
          (let [k (alist-get a i)
                f (alist-get a (unchecked-inc-int i))]
            (f k this old new))
          (recur (+ 2 i)))))))

#?(:cljs
(defn- pr-atom! [a writer opts s]
  (-write writer (str "#<" s " "))
  (pr-writer (binding [*ratom-context* nil] (-deref a)) writer opts)
  (-write writer ">")))

;; ===== RAtom ===== ;;

(defprotocol PReactiveAtom)

(defprotocol PHasCaptured
  (getCaptured [this])
  (setCaptured [this v]))

(defn- notify-deref-watcher!
  "Add `derefed` to the `captured` field of `*ratom-context*`.

  See also `in-context`"
  [derefed]
  (when-some [context *ratom-context*]
    (let [^quantum.untyped.core.data.reactive.PHasCaptured r context]
      (if-some [c (.getCaptured r)]
        (alist-conj! c derefed)
        (.setCaptured r (alist derefed))))))

(udt/deftype ReactiveAtom [^:! state meta validator ^:! watches ^:! watchesArr]
  {;; IPrintWithWriter
   ;;   (-pr-writer [a w opts] (pr-atom a w opts "Atom:"))
   PReactiveAtom {}
   ?Equals {=      ([this that] (identical? this that))}
   ?Deref  {deref  ([this]
                     (notify-deref-watcher! this)
                     state)}
   ?Atom   {reset! ([a new-value]
                     (when-not (nil? validator)
                       (assert (validator new-value) "Validator rejected reference state"))
                     (let [old-value state]
                       (set! state new-value)
                       (when-not (nil? watches)
                         (notify-w! a old-value new-value))
                       new-value))
            swap!  (([a f]          (#?(:clj .reset :cljs -reset!) a (f state)))
                    ([a f x]        (#?(:clj .reset :cljs -reset!) a (f state x)))
                    ([a f x y]      (#?(:clj .reset :cljs -reset!) a (f state x y)))
                    ([a f x y more] (#?(:clj .reset :cljs -reset!) a (apply f state x y more))))}
   ?Watchable {add-watch!    ([this k f] (add-w!    this k f))
               remove-watch! ([this k]   (remove-w! this k))}
   PWatchable {getWatches    ([this]   watches)
               setWatches    ([this v] (set! watches    v))
               getWatchesArr ([this]   watchesArr)
               setWatchesArr ([this v] (set! watchesArr v))}
   ?Meta      {meta      ([_] meta)
               with-meta ([_ meta'] (ReactiveAtom. state meta' validator watches watchesArr))}
#?@(:cljs [?Hash {hash    ([_] (goog/getUid this))}])})

(defn ratom
  "'R'eactive 'atom'. Like `core/atom`, except that it keeps track of derefs."
  ([x] (ReactiveAtom. x nil nil nil nil))
  ([x & {:keys [meta validator]}] (ReactiveAtom. x meta validator nil nil)))

;; ===== Reaction ("Computed Observable") ===== ;;

;; Similar to java.io.Closeable
;; TODO move
(defprotocol PDisposable
  (dispose      [this])
  (addOnDispose [this f]))

(defn dispose!        [x]   (dispose      x))
(defn add-on-dispose! [x f] (addOnDispose x f))

(declare flush! peek-at run-reaction! update-watching!)

(udt/deftype Reaction
  [^:!          ^:get       autoRun
   ^:!          ^:get ^:set caught
   ^:!                      captured
   ^:! ^boolean ^:get ^:set dirty
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
   ^:!                      watches
   ^:!                      watchesArr]
  {;; IPrintWithWriter
   ;;   (-pr-writer [a w opts] (pr-atom a w opts (str "Reaction " (hash a) ":")))
   ?Equals {= ([this that] (identical? this that))}
#?@(:cljs [?Hash {hash ([this] (goog/getUid this))}])
   PReactiveAtom {}
   ?Deref     {deref ([this]
                       (when-some [e caught] (throw e))
                       (let [non-reactive? (nil? *ratom-context*)]
                         (when non-reactive? (flush! queue))
                         (if (and non-reactive? (nil? autoRun))
                             (when dirty
                               (let [old-state state]
                                 (set! state (f))
                                 (when-not (or (nil? watches) (eq-fn old-state state))
                                   (notify-w! this old-state state))))
                             (do (notify-deref-watcher! this)
                                 (when dirty (run-reaction! this false)))))
                       state)}
   ?Watchable {add-watch!    ([this k f] (add-w! this k f))
               remove-watch! ([this k]
                               (let [was-empty? (empty? watches)]
                                 (remove-w! this k)
                                 (when (and (not was-empty?)
                                            (empty? watches)
                                            (nil? autoRun))
                                   (.dispose this))))}
   PWatchable {getWatches    ([this]   watches)
               setWatches    ([this v] (set! watches v))
               getWatchesArr ([this]   watchesArr)
               setWatchesArr ([this v] (set! watchesArr v))}
   ?Atom
     {reset! ([a newv]
                (assert (fn? (.-on-set a)) "Reaction is read only; on-set is not allowed")
                (let [oldv state]
                  (set! state newv)
                  ((.-on-set a) oldv newv)
                  (notify-w! a oldv newv)
                  newv))
      swap!  (([a f]          (#?(:clj .reset :cljs -reset!) a (f (peek-at a))))
              ([a f x]        (#?(:clj .reset :cljs -reset!) a (f (peek-at a) x)))
              ([a f x y]      (#?(:clj .reset :cljs -reset!) a (f (peek-at a) x y)))
              ([a f x y more] (#?(:clj .reset :cljs -reset!) a (apply f (peek-at a) x y more))))}
  PHasCaptured
    {getCaptured ([this]   captured)
     setCaptured ([this v] (set! captured v))}
   PDisposable
     {dispose
       ([this]
         (let [s state, wg watching]
           (set! watching nil)
           (set! state    nil)
           (set! autoRun  nil)
           (set! dirty    #?(:clj (boolean true) :cljs true))
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

(defn- peek-at [^Reaction rx] (binding [*ratom-context* nil] (#?(:clj .deref :cljs -deref) rx)))

(defn- in-context
  "When f is executed, if (f) derefs any ratoms, they are then added to
   'obj.captured'(*ratom-context*).

   See function notify-deref-watcher! to know how *ratom-context* is updated"
  [obj f] (binding [*ratom-context* obj] (f)))

(defn- deref-capture!
  "Returns `(in-context f r)`. Calls `_update-watching` on r with any
   `deref`ed atoms captured during `in-context`, if any differ from the
   `watching` field of r. Clears the `dirty` flag on r.

   Inside '_update-watching' along with adding the ratoms in 'r.watching' of reaction,
   the reaction is also added to the list of watches on each ratoms f derefs."
  [f ^Reaction rx]
  (.setCaptured rx nil)
  (let [res (in-context rx f)
        c   (.getCaptured rx)]
    (.setDirty rx false)
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
        (.setDirty  rx false))))

(defn- run-reaction! [^Reaction rx check?]
  (let [old-state (.getState rx)
        res (if check?
                (try-capture! rx (.-f rx))
                (deref-capture! (.-f rx) rx))]
    (when-not (.-no-cache? rx)
      (.setState rx res)
      ;; Use = to determine equality from reactions, since
      ;; they are likely to produce new data structures.
      (when-not (or (nil? (.getWatches rx))
                    (= old-state res))
        (notify-w! rx old-state res)))
    res))

(defn- handle-reaction-change! [^Reaction rx sender oldv newv]
  (when-not (or (identical? oldv newv) (.getDirty rx))
    (let [auto-run (.getAutoRun rx)]
      (ifs (nil? auto-run)
             (do (.setDirty rx true)
                 ((.-enqueue-fn rx) (.-queue rx) rx))
           (true? auto-run)
             (run-reaction! rx false)
           (auto-run rx)))))

(defn- update-watching! [^Reaction rx derefed]
  (let [new (set derefed) ; TODO incrementally calculate `set`
        old (set (.getWatching rx))]
    (.setWatching rx derefed)
    (doseq [w (set/difference new old)]
      (#?(:clj add-watch    :cljs -add-watch)    w rx handle-reaction-change!))
    (doseq [w (set/difference old new)]
      (#?(:clj remove-watch :cljs -remove-watch) w rx))))

(defn- run-reaction-from-queue! [^Reaction rx]
  (when (and (.getDirty rx) (some? (.getWatching rx)))
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
  ([f {:keys [auto-run enqueue-fn eq-fn no-cache? on-set on-dispose queue]}]
    (Reaction. auto-run nil nil true (or enqueue-fn *enqueue!*) (or eq-fn =) f
               (if (nil? no-cache?) false no-cache?) on-dispose nil on-set (or queue *queue*) nil
               nil nil nil)))

#?(:clj (defmacro rx [& body] `(>rx (fn [] ~@body))))

#?(:clj
(defmacro run!
  "Runs body immediately, and runs again whenever atoms deferenced in the body change. Body should side effect."
  [& body]
  `(let [co# (>rx (fn [] ~@body) {:auto-run true})]
     (deref co#)
     co#)))

;; ===== Track ===== ;;

(udt/deftype TrackableFn [f ^:! ^:get ^:set rxCache])

(declare cached-reaction)

(udt/deftype Track
  [^TrackableFn trackable-fn, args, ^:! ^:get ^:set ^quantum.untyped.core.data.reactive.Reaction rx]
  {;; IPrintWithWriter
   ;;   (-pr-writer [a w opts] (pr-atom a w opts "Track:"))
   PReactiveAtom {}
   ?Deref  {deref ([this]
                    (if (nil? rx)
                        (cached-reaction #(apply (.-f trackable-fn) args)
                          trackable-fn args this nil)
                        (#?(:clj .deref :cljs -deref) rx)))}}
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
      (some? r) (#?(:clj .deref :cljs -deref) r)
      (nil? *ratom-context*) (f)
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
                  v (#?(:clj .deref :cljs -deref) r)]
              (.setRxCache trackable-fn (assoc m k r))
              (when (true? *debug?*) (swap! *running inc))
              (when (some? t)
                (.setRx t r))
              v))))

(defn ^Track >track [f args] (Track. (TrackableFn. f nil) args nil))

(defn >track! [f args opts]
  (let [t (>track f args)
        r (>rx #(#?(:clj .deref :cljs -deref) t)
               {:auto-run true :queue (or (:queue opts) global-queue)})]
    @r
    r))
