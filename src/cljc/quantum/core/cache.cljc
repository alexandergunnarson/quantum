(ns quantum.core.cache
  (:refer-clojure :exclude [memoize])
  (:require-quantum [ns fn logic])
  #?(:clj (:import java.util.concurrent.ConcurrentHashMap)))

#?(:clj
(defmacro memoize-form
  {:attribution "ztellman/byte-streams"
   :contributors ["Alex Gunnarson"]}
  [m f varargs? & args]
  (let [k (gensym 'k)]
    `(let [~k ~(if varargs?
                   `(list* ~@(butlast args) ~(last args))
                   `(vector ~@args))]
       (let [v# (.get ~m ~k)]
         (if (nil? v#)
           (let [v# (delay ~(if varargs?
                                `(apply ~f ~k)
                                `(~f ~@args)))]
             @(or (.putIfAbsent ~m ~k v#) v#))
           @v#))))))

#?(:clj
    (defn memoize
      "A version of |memoize| which has equivalent behavior, but is faster."
      {:attribution  "ztellman/byte-streams"
       :contributors ["Alex Gunnarson"]}
      [f]
      (let [m (ConcurrentHashMap.)]
        (fn
          ([                  ] (memoize-form m f false                 ))
          ([x                 ] (memoize-form m f false x               ))
          ([x y               ] (memoize-form m f false x y             ))
          ([x y z             ] (memoize-form m f false x y z           ))
          ([x y z w           ] (memoize-form m f false x y z w         ))
          ([x y z w u         ] (memoize-form m f false x y z w u       ))
          ([x y z w u v       ] (memoize-form m f false x y z w u v     ))
          ([x y z w u v & rest] (memoize-form m f true  x y z w u v rest)))))
   :cljs (defalias memoize core/memoize))