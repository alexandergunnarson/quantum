(ns quantum.core.cache
  (:refer-clojure :exclude [memoize])
  (:require-quantum [:core fn logic err])
  #?(:clj (:import java.util.concurrent.ConcurrentHashMap)))

#?(:clj
(defmacro memoize-form
  {:attribution "ztellman/byte-streams"
   :contributors ["Alex Gunnarson"]}
  [m f get-fn assoc-fn memoize-only-first-arg? varargs? & [arg0 :as args]]
  (let [k (gensym 'k)]
    `(let [~k (if ~memoize-only-first-arg?
                  ~arg0
                  ~(if varargs?
                      `(list* ~@(butlast args) ~(last args))
                      `(vector ~@args)))
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
  ([f m-0 & [memoize-only-first-arg? get-fn-0 assoc-fn-0]]
    (let [m (or m-0 (ConcurrentHashMap.))
          first? memoize-only-first-arg?
          {:keys [get-fn assoc-fn]}
            (cond
              (instance? clojure.lang.IDeref m)
                {:get-fn   (or get-fn-0   (fn [m1 k1   ] (get @m1 k1)))
                 :assoc-fn (or assoc-fn-0 (fn [m1 k1 v1] (swap! m1 assoc k1 @v1)))} ; undelays it because usually that's what is wanted
              (instance? ConcurrentHashMap   m)
                {:get-fn   (or get-fn-0   (fn [m1 k1   ] (.get         ^ConcurrentHashMap m1 k1   )))
                 :assoc-fn (or assoc-fn-0 (fn [m1 k1 v1] (.putIfAbsent ^ConcurrentHashMap m1 k1 v1)))}
              :else
                (throw (->ex nil "No get-fn or assoc-fn defined for" m)))]
      {:m m
       :f (fn
            ([                  ] (memoize-form m f get-fn assoc-fn first? false                 ))
            ([x                 ] (memoize-form m f get-fn assoc-fn first? false x               ))
            ([x y               ] (memoize-form m f get-fn assoc-fn first? false x y             ))
            ([x y z             ] (memoize-form m f get-fn assoc-fn first? false x y z           ))
            ([x y z w           ] (memoize-form m f get-fn assoc-fn first? false x y z w         ))
            ([x y z w u         ] (memoize-form m f get-fn assoc-fn first? false x y z w u       ))
            ([x y z w u v       ] (memoize-form m f get-fn assoc-fn first? false x y z w u v     ))
            ([x y z w u v & rest] (memoize-form m f get-fn assoc-fn first? true  x y z w u v rest)))}))))

#?(:clj
(defn memoize [& args] (:f (apply memoize* args))))

#?(:cljs (defalias memoize core/memoize))