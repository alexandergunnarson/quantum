(ns
  ^{:doc "Benchmarking utilities. Criterium is aliased and is especially useful."
    :attribution "Alex Gunnarson"}
  quantum.core.meta.bench
           (:require 
             #?(:clj [criterium.core         :as bench])
                     [quantum.core.string    :as str  ]
                     [quantum.core.fn        :as fn
                       :refer [#?@(:clj [fn->])]      ]
                     [quantum.core.vars      :as var
                       :refer [#?(:clj defalias)]     ])
  #?(:cljs (:require-macros
                     [quantum.core.fn        :as fn
                       :refer [fn->]                  ]
                     [quantum.core.vars      :as var
                       :refer [defalias]              ]))
  #?(:clj (:import com.carrotsearch.sizeof.RamUsageEstimator
                   quanta.ClassIntrospector)))

#?(:clj 
  (defn num-from-timing [time-str]
    (-> (str/replace time-str "\"" "")
        (str/replace "\n" "")
        (str/replace "Elapsed time: " "")
        (str/replace " msecs" "")
        (read-string))))

#?(:clj
(defmacro time-ms
  "Like |clojure.core/time| but returns a double instead of a printed string."
  {:source "hara.debug"}
  [expr]
  `(let [start# (System/nanoTime)
         ret# ~expr]
     (/ (double (- (System/nanoTime) start#)) 1000000.0))))

; FOR CLJS 
#?(:clj
(defmacro profile [k & body]
  ; if-cljs
  `(let [k# ~k]
     (.time js/console k#)
     (let [res# (do ~@body)]
       (.timeEnd js/console k#)
       res#))))

#?(:clj 
  (defn shoddy-benchmark [to-repeat func & args]
    (let [times-list
            (take to-repeat
              (repeatedly (fn [] (num-from-timing
                            (with-out-str (time-ms (apply func args)))))))]
    (/ (apply + times-list) to-repeat)))) ; average

#?(:clj (defalias bench bench/quick-bench))
#?(:clj (defalias complete-bench bench/bench))

; BYTE SIZE

#?(:clj (defn byte-size-alt-1 [obj] (RamUsageEstimator/sizeOf obj)))

#?(:clj
(defn byte-size-alt-2 [obj]
  (-> (ClassIntrospector.)
      (.introspect obj)
      (.getDeepSize))))

#?(:clj
(defn byte-size [obj]
  (let [result-1 (byte-size-alt-1 obj)
        result-2 (byte-size-alt-2 obj)]
    [(min result-1 result-2)
     (max result-1 result-2)])))

#?(:clj
(defn calc-byte-size-of-all-vars
  "WARNING: Takes a really long time"
  []
  (->> (all-ns)
       (map ns-name)
       (map (fn [ns-]
              (->> ns- ns-publics
                   (map (juxt key (fn-> val deref byte-size)))
                   (map (juxt (constantly ns-) first second)))))
       (apply concat)
       (sort-by #(get % 2)))))