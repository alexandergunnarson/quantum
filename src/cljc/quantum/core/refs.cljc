(ns quantum.core.refs
  (:refer-clojure :exclude
    [deref
     atom add-watch get-validator set-validator!
     agent agent-error await await-for
     alter io! sync dosync ensure ref-set error-handler error-mode set-error-mode!
       set-error-mode! set-agent-send-executor! set-agent-send-off-executor!
     var-set])
  (:require
    [clojure.core      :as core]
    [quantum.core.core
      :refer [atom?]]
    [quantum.core.macros.core
      :refer [case-env]]
    [quantum.core.vars
      :refer [defalias]])
  #?(:clj (:import [clojure.lang IDeref IAtom IPending])))

(defalias deref     core/deref)

(defn ?deref [a] (if (nil? a) nil (deref a)))

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
        (apply swap! x f arg1 arg2 args))
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

; ===== ATOMS ===== ;

(defalias atom           core/atom)
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
