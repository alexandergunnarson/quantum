(ns quantum.core.core
  (:refer-clojure :exclude [#?(:cljs seqable?)])
  (:require-quantum [reg])
  (:require #?(:clj  [clojure.core  :as core ]
               :cljs [cljs.core     :as core
                       :refer [IDeref IAtom]]))
  #?(:clj (:import [clojure.lang IDeref
                                 IAtom])))

; ===== TYPE PREDICATES =====

(defn atom?    [x] (#?(:clj  instance?
                       :cljs satisfies?)
                    IAtom x))

(defn boolean? [x] (or (true? x) (false? x)))

#?(:clj
     (defn seqable?
       "Returns true if (seq x) will succeed, false otherwise."
       {:from "clojure.contrib.core"}
       [x]
       (or (seq? x)
           (instance? clojure.lang.Seqable x)
           (nil? x)
           (instance? Iterable x)
           (-> x class .isArray)
           (string? x)
           (instance? java.util.Map x)))
   :cljs (def seqable? core/seqable?))

(defn editable? [coll]
  #?(:clj  (instance? clojure.lang.IEditableCollection coll)
     :cljs (satisfies? cljs.core.IEditableCollection coll)))

; ===== REFS AND ATOMS =====

(defn deref* [a] (if (nil? a) nil (deref a)))

(defn lens [x getter]
  (when-not (#?(:clj  instance?
                :cljs satisfies?) IDeref x)
    (throw (#?(:clj  IllegalArgumentException.
               :cljs js/Error.)
            "Argument to |lens| must be an IDeref")))
  (reify IDeref
    (#?(:clj  deref
        :cljs -deref) [this] (getter @x))))

(defn cursor
  {:todo ["@setter currently doesn't do anything"]}
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

(defn seq-equals [a b]
  (boolean
    (when (or (sequential? b) #?(:clj  (instance? java.util.List b)  
                                 :cljs (list? b)))
      (loop [a (seq a) b (seq b)]
        (when (= (nil? a) (nil? b))
          (or
            (nil? a)
            (when (= (first a) (first b))
              (recur (next a) (next b)))))))))

; ===== TYPE =====

(def unchecked-inc-long 
  #?(:clj  (fn [^long x] (unchecked-inc x))
     :cljs inc))

#?(:clj
(defmacro with
  "Evaluates @expr, then @body, then returns @expr.
   For side effects."
  [expr & body]
  `(let [expr# ~expr]
    ~@body
    expr#)))

(defn name+ [x] (if (nil? x) x (name x)))

; Nested |let-mutable| :
    ; ClassCastException java.lang.Long cannot be cast to proteus.Containers$L


#?(:cljs
(defn ensure-println [& args]
  (enable-console-print!)
  (apply println args)))