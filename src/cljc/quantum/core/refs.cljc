(ns quantum.core.refs
  (:refer-clojure :exclude
    [deref
     volatile!
     atom add-watch get-validator set-validator! reset! swap!
     agent agent-error await await-for commute
     alter io! sync dosync ensure ref-set error-handler error-mode set-error-handler!
       set-error-mode! set-agent-send-executor! set-agent-send-off-executor!
     var-set])
  (:require
    [clojure.core       :as core]
    [clojure.string     :as str]
    [quantum.core.core
      :refer [atom?]]
    [quantum.core.error :as err
      :refer [TODO]]
    [quantum.core.macros
      :refer [case-env defnt #?(:clj defnt') env-lang]]
    [quantum.core.type  :as t]
    [quantum.core.type.defs :as tdefs]
    [quantum.core.vars  :as var
      :refer [defalias]])
#?(:clj
  (:import
    [clojure.lang IDeref IAtom IPending]
    [java.util.concurrent.atomic AtomicReference AtomicBoolean AtomicInteger AtomicLong]
    [com.google.common.util.concurrent AtomicDouble])))

; ===== UNSYNCHRONIZED MUTABILITY ===== ;

(#?(:clj definterface :cljs defprotocol) IMutableReference
  (get       [#?(:cljs this)])
  (set       [#?(:cljs this) v])
  (getAndSet [#?(:cljs this) v]))

; TODO create for every primitive datatype as well
(deftype MutableReference [#?(:clj ^:unsynchronized-mutable val :cljs ^:mutable val)]
  IMutableReference
  (get       [this] val)
  (set       [this v] (set! val v) val)
  (getAndSet [this v] (let [v-prev val] (set! val v) v-prev))
  #?(:clj  clojure.lang.IDeref
     :cljs cljs.core/IDeref)
  (#?(:clj deref :cljs -deref) [this] val))

        (defnt    !ref* "Creates a mutable reference to an Object." [x] (MutableReference. x))
#?(:clj (defmacro !ref  ([] `(MutableReference. nil)) ([x] `(!ref* ~x))))

(defn gen-primitive-mutable-interface-and-deftype [kind]
  (let [interface-sym   (symbol (str "IMutable" (str/capitalize (name kind))))
        get-sym         (with-meta 'get       {:tag kind})
        set-sym         (with-meta 'set       {:tag kind})
        get-and-set-sym (with-meta 'getAndSet {:tag kind})
        v-sym           (with-meta 'v         {:tag kind})]
   `(do (definterface ~interface-sym
          (~get-sym         [])
          (~set-sym         [~v-sym])
          (~get-and-set-sym [~v-sym]))
        (deftype ~(symbol (str "Mutable" (str/capitalize (name kind))))
          [~(with-meta 'val {:unsynchronized-mutable true :tag kind})]
          ~interface-sym
             (~get-sym         [this#] ~'val)
             (~set-sym         [this# ~v-sym] (set! ~'val ~'v) ~'val)
             (~get-and-set-sym [this# ~v-sym] (let [v-prev# ~'val] (set! val ~'v) v-prev#))
           #_clojure.lang.IDeref #_(deref [this] val))))) ; conflicting interface and boxes value; TODO fix

(defn gen-primitive-mutable [kind]
  (let [defnt-sym   (symbol (str "!" kind "*"))
        macro-param (gensym "x")
        deftype-sym (symbol (str "Mutable" (str/capitalize (name kind))))]
   `(do ~(gen-primitive-mutable-interface-and-deftype kind)
        (defnt ~defnt-sym
          ~(str "Creates a mutable reference to a primitive " kind ".")
          [~(with-meta 'x {:tag kind})] (new ~deftype-sym ~'x))
        (defmacro  ~(symbol (str "!" kind))
          ([ ]            `(new ~'~deftype-sym (~'~kind 0)))
          ([~macro-param] `(~'~(var/qualify *ns* defnt-sym) ~~macro-param))))))

#?(:clj
(defmacro gen-primitive-mutables []
  (when (= :clj (env-lang))
    `(do ~@(for [kind (:clj tdefs/prim-types)] (gen-primitive-mutable kind))))))

(gen-primitive-mutables)

; ===== COMMON MUTATIVE OPERATIONS ===== ;

#?(:clj
(defnt' !
  "Creates an unsynchronized mutable reference."
  ([         x] (!ref     x))
  ([^boolean x] (!boolean x))
  ([^byte    x] (!byte    x))
  ([^char    x] (!char    x))
  ([^short   x] (!short   x))
  ([^int     x] (!int     x))
  ([^long    x] (!long    x))
  ([^float   x] (!float   x))
  ([^double  x] (!double  x))))

#?(:clj
(defnt setm!*
  ([^IMutableReference x          v] (.set x v))
  ([^IMutableBoolean   x ^boolean v] (.set x v))
  ([^IMutableByte      x ^byte    v] (.set x v))
  ([^IMutableChar      x ^char    v] (.set x v))
  ([^IMutableShort     x ^short   v] (.set x v))
  ([^IMutableInt       x ^int     v] (.set x v))
  ([^IMutableLong      x ^long    v] (.set x v))
  ([^IMutableFloat     x ^float   v] (.set x v))
  ([^IMutableDouble    x ^double  v] (.set x v))))

; TODO remove these soon
#?(:clj (defmacro setm!  "Set mutable" [x v] (case-env :cljs `(set! (.-val ~x) ~v) `(setm!*  ~x ~v))))
#?(:clj (defmacro setm!& "Set mutable" [x v] (case-env :cljs `(set! (.-val ~x) ~v) `(setm!*& ~x ~v))))

#?(:clj  (defnt deref
           ([#{clojure.lang.IDeref}         x] (.deref x))
           ([#{AtomicBoolean
               #_AtomicByte
               #_AtomicChar
               #_AtomicShort
               AtomicInteger
               AtomicLong
               #_AtomicFloat
               AtomicDouble
               AtomicReference
               java.util.concurrent.Future
               #_IMutableReference
               IMutableBoolean
               IMutableByte
               IMutableChar
               IMutableShort
               IMutableInt
               IMutableLong
               IMutableFloat
               IMutableDouble}              x] (.get x))
           ([#{clojure.lang.IBlockingDeref} x timeout-ms timeout-val]
             (.deref x timeout-ms timeout-val))
           ([#{java.util.concurrent.Future} x timeout-ms timeout-val]
             (try (.get x timeout-ms java.util.concurrent.TimeUnit/MILLISECONDS)
               (catch java.util.concurrent.TimeoutException e
                 timeout-val))))
   :cljs (defalias deref core/deref))

(defn ?deref [a] (when (some? a) (deref a))) ; TODO type this

(defn ->derefable [x]
  (if (t/derefable? x)
      x
      (reify #?(:clj clojure.lang.IDeref :cljs cljs.core/IDeref) ; TODO `reify-compatible`
         (#?(:clj deref :cljs -deref) [_] x))))

#?(:clj
(defmacro swapm! [*x0 *x1]
  `(let [*x0# ~*x0 *x1# ~*x1
         temp# (deref *x0#)]
     (setm! *x0# (deref *x1#))
     (setm! *x1# temp#))))

; ===== VOLATILES ===== ;

; TODO typed `volatile`s
(defn volatile [x] (clojure.lang.Volatile. x))

; ===== ATOMS ===== ;

; TODO typed `atom`s
(defalias atom core/atom)

#?(:clj
(defnt atom*
  "Like `atom`, but lighter-weight in that it doesn't have e.g. `swap!`,
   validation, etc." ; TODO lighter-weight for other reasons?
  ([^boolean v] (AtomicBoolean.   v))
  ; AtomicByte
  ; AtomicChar
  ; AtomicShort
  ([^int     v] (AtomicInteger.   v))
  ([^long    v] (AtomicLong.      v))
  ; AtomicFloat
  ([^double  v] (AtomicDouble.    v))
  ([^default v] (AtomicReference. v))))

#?(:clj  (defnt reset!
           ([#{IMutableBoolean AtomicBoolean}     x ^boolean v] (.set   x v) v)
           ([#{IMutableByte  #_AtomicByte}        x ^byte    v] (.set   x v) v)
           ([#{IMutableChar  #_AtomicChar}        x ^char    v] (.set   x v) v)
           ([#{IMutableShort #_AtomicShort}       x ^short   v] (.set   x v) v)
           ([#{IMutableInt     AtomicInteger}     x ^int     v] (.set   x v) v)
           ([#{IMutableLong AtomicLong}           x ^long    v] (.set   x v) v)
           ([#{IMutableFloat #_AtomicFloat}       x ^float   v] (.set   x v) v)
           ([#{IMutableDouble AtomicDouble}       x ^double  v] (.set   x v) v)
           ([#{IMutableReference AtomicReference} x          v] (.set   x v) v)
           ([#{IAtom clojure.lang.Volatile}       x          v] (.reset x v) v))
   :cljs (defalias reset! core/reset!))

#?(:clj  (defnt swap!
           ([^IAtom x f      ] (.swap x f      ))
           ([^IAtom x f a0   ] (.swap x f a0   ))
           ([^IAtom x f a0 a1] (.swap x f a0 a1)))
   :cljs (defalias swap! core/swap!))

#?(:clj (defmacro doto! [x & args] `(doto ~x (swap! ~@args))))

#?(:clj  (defnt get-reset!
           ([#{IMutableBoolean AtomicBoolean}     x ^boolean v] (.getAndSet   x v))
           ([#{IMutableByte  #_AtomicByte}        x ^byte    v] (.getAndSet   x v))
           ([#{IMutableChar  #_AtomicChar}        x ^char    v] (.getAndSet   x v))
           ([#{IMutableShort #_AtomicShort}       x ^short   v] (.getAndSet   x v))
           ([#{IMutableInt     AtomicInteger}     x ^int     v] (.getAndSet   x v))
           ([#{IMutableLong AtomicLong}           x ^long    v] (.getAndSet   x v))
           ([#{IMutableFloat #_AtomicFloat}       x ^float   v] (.getAndSet   x v))
           ([#{IMutableDouble AtomicDouble}       x ^double  v] (.getAndSet   x v))
           ([#{IMutableReference AtomicReference} x          v] (.getAndSet   x v))
           ([#{IAtom clojure.lang.Volatile}       x          v] (TODO)))
   :cljs (defn get-reset! [x v] (TODO)))

(defalias add-watch      core/add-watch)
(defalias get-validator  core/get-validator)
(defalias set-validator! core/set-validator!)

(defn ensure-validated-atom!
  "Ensures that `x` is an atom having `validator`."
  [x validator]
  (if (atom? x)
      (if (-> x get-validator (identical? validator))
          x
          (doto x (set-validator! validator)))
      (doto (atom x) (set-validator! validator))))

; ===== AGENTS ===== ;

#?(:clj (defalias agent       core/agent))
#?(:clj (defalias agent-error core/agent-error))
#?(:clj (defalias await       core/await))
#?(:clj (defalias await-for   core/await-for))
#?(:clj (defalias commute     core/commute))

; ===== REFS ===== ;

#?(:clj (defalias alter                        core/alter  ))
#?(:clj (defalias io!                          core/io!    ))
#?(:clj (defalias sync                         core/sync   ))
#?(:clj (defalias dosync                       core/dosync ))
#?(:clj (defalias ensure                       core/ensure ))
#?(:clj (defalias ref-set                      core/ref-set))
#?(:clj (defalias error-handler                core/error-handler))
#?(:clj (defalias set-error-handler!           core/set-error-handler!))
#?(:clj (defalias error-mode                   core/error-mode))
#?(:clj (defalias set-error-mode!              core/set-error-mode!))
#?(:clj (defalias set-agent-send-executor!     core/set-agent-send-executor!))
#?(:clj (defalias set-agent-send-off-executor! core/set-agent-send-off-executor!))

; ===== VARS ===== ;

#?(:clj (defalias var-set core/var-set))

; ===== OTHER ===== ;

#?(:clj
(defmacro fref
  "Creates a ref that re-evaluates `body` when derefed, like an `fn`."
  [& body]
 `(reify
    IPending
    (~(case-env :clj 'isRealized
                :cljs '-realized?) [_] false) ; in order to not print out `body` by default unless asked
    IDeref
    (~(case-env :clj  'deref
                :cljs '-deref) [_] ~@body))))

(defn lens
  ([x getter]
    (if (#?(:clj  instance?
            :cljs satisfies?) IDeref x)
        (reify IDeref
          (#?(:clj  deref
              :cljs -deref) [this] (getter @x)))
        (throw (#?(:clj  IllegalArgumentException.
                   :cljs js/Error.)
                "Argument to `lens` must be an IDeref")))))

(defn cursor ; TODO use `deftype-compatible`?
  {:todo #{"@setter currently doesn't do anything"}}
  [x getter & [setter]]
  (when-not (#?(:clj  instance?
                :cljs satisfies?) IDeref x)
    (throw (#?(:clj  IllegalArgumentException.
               :cljs js/Error.)
            "Argument to |cursor| must be an IDeref")))
  (reify
    IDeref
      (#?(:clj  deref
          :cljs -deref) [this] (getter @x))
    IAtom
    #?@(:clj
     [(swap [this f]
        (swap! x f))
      (swap [this f arg]
        (swap! x f arg))
      (swap [this f arg1 arg2]
        (swap! x f arg1 arg2))
      (swap [this f arg1 arg2 args]
        (apply core/swap! x f arg1 arg2 args)) ; TODO fix this
      (compareAndSet [this oldv newv]
        (compare-and-set! x oldv newv))
      (reset [this newv]
        (reset! x newv))]
      :cljs
   [cljs.core/IReset
      (-reset! [this newv]
        (reset! x newv))])
    #?(:clj  clojure.lang.IRef
       :cljs cljs.core/IWatchable)
    #?(:cljs
        (-notify-watches [this oldval newval]
          (doseq [[key f] (.-watches x)]
            (f key this oldval newval))
          this))
    #?(:clj
        (getWatches [this]
          (.getWatches ^clojure.lang.IRef x)))
    #?(:clj
        (setValidator [this f]
          (set-validator! x f)))
    #?(:clj
        (getValidator [this]
          (get-validator x)))
      (#?(:clj  addWatch
          :cljs -add-watch) [this k f]
        (add-watch x k f)
        this)
      (#?(:clj  removeWatch
          :cljs -remove-watch) [this k]
        (remove-watch x k)
        this)))
