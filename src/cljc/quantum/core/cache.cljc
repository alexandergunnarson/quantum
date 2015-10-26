(ns quantum.core.cache
  (:refer-clojure :exclude [memoize])
  (:require-quantum [ns fn logic err])
  #?(:clj (:import java.util.concurrent.ConcurrentHashMap)))

#?(:clj
(defmacro memoize-form
  {:attribution "ztellman/byte-streams"
   :contributors ["Alex Gunnarson"]}
  [m f get-fn assoc-fn varargs? & args]
  (let [k (gensym 'k)]
    `(let [~k ~(if varargs?
                   `(list* ~@(butlast args) ~(last args))
                   `(vector ~@args))
           v# (~get-fn ~m ~k)]
       (if (nil? v#)
           (let [v# (delay ~(if varargs?  ; Delay makes sure even in multithreaded environments it only calculates once
                                `(apply ~f ~k)
                                `(~f ~@args)))]
             @(do (~assoc-fn ~m ~k v#) v#))
           @v#)))))

#?(:clj
(defn memoize*
  "A faster, customizable version of |core/memoize|."
  {:attribution ["Alex Gunnarson"]
   :todo ["Take out repetitiveness via macro"]}
  ([f] (memoize* f (ConcurrentHashMap.)))
  ([f m & [get-fn-0 assoc-fn-0]]
    (let [{:keys [get-fn assoc-fn]}
            (cond
              (instance? clojure.lang.IDeref m)
                {:get-fn   (fn [m1 k1   ] (get @m1 k1))
                 :assoc-fn (fn [m1 k1 v1] (swap! m1 assoc k1 v1))}
              (instance? ConcurrentHashMap   m)
                {:get-fn   (fn [m1 k1   ] (.get         ^ConcurrentHashMap m1 k1   ))
                 :assoc-fn (fn [m1 k1 v1] (.putIfAbsent ^ConcurrentHashMap m1 k1 v1))}
              :else
                (throw+ (Err. nil "No get-fn or assoc-fn defined for" m)))]
      {:m m
       :f (fn
            ([                  ] (memoize-form m f get-fn assoc-fn false                 ))
            ([x                 ] (memoize-form m f get-fn assoc-fn false x               ))
            ([x y               ] (memoize-form m f get-fn assoc-fn false x y             ))
            ([x y z             ] (memoize-form m f get-fn assoc-fn false x y z           ))
            ([x y z w           ] (memoize-form m f get-fn assoc-fn false x y z w         ))
            ([x y z w u         ] (memoize-form m f get-fn assoc-fn false x y z w u       ))
            ([x y z w u v       ] (memoize-form m f get-fn assoc-fn false x y z w u v     ))
            ([x y z w u v & rest] (memoize-form m f get-fn assoc-fn true  x y z w u v rest)))}))))

#?(:clj
(defn memoize [& args] (:f (apply memoize* args))))

#?(:cljs (defalias memoize core/memoize))