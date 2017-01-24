(ns ; ^{:clojure.tools.namespace.repl/unload false} ; because of cache
  quantum.core.cache
           (:refer-clojure :exclude [memoize])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )  :as core]
                     [quantum.core.error      :as err
                       :refer [->ex]                  ]
                     [quantum.core.fn         :as fn
                       :refer [#?@(:clj [fn1])]]
                     [quantum.core.logic
                       :refer [whenc1]]
                     [quantum.core.vars       :as var
                       :refer [defalias]]
             #?(:clj [taoensso.timbre.profiling :as p])
                     [quantum.core.macros.core  :as cmacros
                       :refer [#?(:clj if-cljs)]])
  (:require-macros [quantum.core.cache :as self])
  #?(:clj (:import java.util.concurrent.ConcurrentHashMap)))

#?(:clj
(defmacro memoize-form
  {:attribution "ztellman/byte-streams"
   :contributors ["Alex Gunnarson"]}
  [m f get-fn assoc-fn memoize-only-first-arg? n-args varargs? & [arg0 :as args]]
  (let [k (gensym 'k)]
    `(let [n-args# ~n-args
           ~k (if ~memoize-only-first-arg?
                  ~arg0
                  (let [args# ~(if varargs?
                                  `(list* ~@(butlast args) ~(last args))
                                  `(vector ~@args))]
                    (if n-args#
                        (take n-args# args#)
                        args#)))
           v# (~get-fn ~m ~k)]
       (if (nil? v#)
           (let [v-delay# (delay ~(if varargs?  ; Delay: laziness
                                     `(apply ~f ~k)
                                     `(~f ~@args)))]
             @(do (~assoc-fn ~m ~k v-delay#) v-delay#))
           (if (delay? v#) @v# v#))))))

#?(:clj
(defn memoize*
  "A faster, customizable version of |core/memoize|."
  {:attribution ["Alex Gunnarson"]
   :todo ["Take out repetitiveness via macro"]}
  ([f] (memoize* f nil))
  ([f m-0 & [memoize-only-first-arg? get-fn-0 assoc-fn-0 memoize-first-n-args]]
    (let [m (or m-0 (ConcurrentHashMap.))
          first? memoize-only-first-arg?
          n-args memoize-first-n-args
          {:keys [get-fn assoc-fn]}
            (cond
              (instance? clojure.lang.IDeref m)
                {:get-fn   (or get-fn-0   (fn [data-n k1   ] (get @data-n k1)))
                 :assoc-fn (or assoc-fn-0 (fn [data-n k1 v1] (swap! data-n assoc k1 @v1)))} ; undelays it because usually that's what is wanted
              (instance? ConcurrentHashMap   m)
                {:get-fn   (or get-fn-0   (fn [m1 k1   ] (.get         ^ConcurrentHashMap m1 k1   )))
                 :assoc-fn (or assoc-fn-0 (fn [m1 k1 v1] (.putIfAbsent ^ConcurrentHashMap m1 k1 v1)))}
              :else
                (throw (->ex "No get-fn or assoc-fn defined for" m)))]
      {:m m
       :f (fn
            ([                  ] (memoize-form m f get-fn assoc-fn first? n-args false                 ))
            ([x                 ] (memoize-form m f get-fn assoc-fn first? n-args false x               ))
            ([x y               ] (memoize-form m f get-fn assoc-fn first? n-args false x y             ))
            ([x y z             ] (memoize-form m f get-fn assoc-fn first? n-args false x y z           ))
            ([x y z w           ] (memoize-form m f get-fn assoc-fn first? n-args false x y z w         ))
            ([x y z w u         ] (memoize-form m f get-fn assoc-fn first? n-args false x y z w u       ))
            ([x y z w u v       ] (memoize-form m f get-fn assoc-fn first? n-args false x y z w u v     ))
            ([x y z w u v & rest] (memoize-form m f get-fn assoc-fn first? n-args true  x y z w u v rest)))}))))

#?(:clj
(defn memoize [& args] (:f (apply memoize* args))))

#?(:cljs (defalias memoize core/memoize))

(defonce caches         (atom {}))
(defonce init-cache-fns (atom {}))

(defn init! [var-]
  (when-let [f (get @init-cache-fns var-)]
    (f)))

(defn clear! [var-]
  (swap! (get @caches var-) empty)
  true) ; to avoid printing out the entire cache

#?(:clj
(defmacro defmemoized
  [sym opts & args]
  (let [cache-sym      (symbol (str (name sym) "-cache"))
        sym-star       (symbol (str (name sym) "*"))]
    `(do (declare ~sym ~sym-star)
         (~(if-cljs &env `defn `p/defnp) ~sym-star ~@args)
         (defonce ~cache-sym
           (let [cache-f# (or (:cache ~opts) (atom {}))]
             (swap! caches update (var ~sym) (whenc1 nil? cache-f#)) ; override cache only if not present
             cache-f#))
         (def ~sym (let [opts# ~opts]
                     (when-let [init-cache-fn# (:init-fn opts#)]
                       (swap! init-cache-fns assoc (var ~sym) init-cache-fn#))
                     (memoize ~sym-star ~cache-sym
                       (:memoize-only-first? opts#)
                       (:get-fn              opts#)
                       (:assoc-fn            opts#)
                       (:memoize-first       opts#))))
         (doto (var ~sym)
           (alter-meta! ; transfer metadata from |sym-star| to |sym|
             (fn1 merge (-> (var ~sym-star)
                            meta
                            (dissoc :line)
                            (dissoc :name)))))))))
