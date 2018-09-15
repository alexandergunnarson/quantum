(ns
  ^{:doc "Benchmarking and profiling utilities. Criterium is aliased and is especially useful."
    :attribution "alexandergunnarson"}
  quantum.core.meta.profile
  (:refer-clojure :exclude [val reduce])
  (:require
    #?(:clj [criterium.core         :as bench])
            [taoensso.tufte         :as tufte]
            [quantum.core.string    :as str  ]
            [quantum.core.collections :as coll
              :refer [map+ remove+ join reduce val]]
            [quantum.core.fn        :as fn
              :refer [fn-> rcomp]]
            [quantum.core.logic
              :refer [fn-or fn-and]]
            [quantum.core.vars      :as var
              :refer [defalias]]
            [quantum.core.type-old  :as t])
  #?(:clj (:import com.carrotsearch.sizeof.RamUsageEstimator
                   quantum.misc.ClassIntrospector)))

; TO EXPLORE
; - Timbre profiling
; - Like Criterium for JS: https://github.com/bestiejs/benchmark.js
; ==================================

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

#?(:clj (defalias bench bench/quick-bench))
#?(:clj (defalias complete-bench bench/bench))

; BYTE SIZE

#?(:clj (defn shallow-byte-size [obj] (RamUsageEstimator/sizeOf obj)))

#?(:clj
(defn deep-byte-size
  "Warning: doesn't handle ref-cycles."
  [obj]
  ;; TODO port ClassIntrospector
  (-> (ClassIntrospector.)
      (.introspect obj)
      (.getDeepSize))))

#?(:clj (defalias p        tufte/p       ))
#?(:clj (defmacro with-p [k body arg]
          (list `p k `(~@body ~arg))))
#?(:clj (defmacro profile  ([     body] `(      profile {}    ~body))
                           ([opts body] `(tufte/profile ~opts ~body))))
#?(:clj (defalias profiled tufte/profiled))

#_(:clj
(defn byte-size-range [obj]
  (let [result-1 (byte-size-alt-1 obj)
        result-2 (byte-size-alt-2 obj)]
    [(min result-1 result-2)
     (max result-1 result-2)])))

#_(:clj
(defn calc-byte-size-of-all-vars
  "WARNING: Takes a really long time"
  []
  (->> (all-ns)
       (map ns-name)
       (map (fn [ns-]
              (->> ns- ns-publics
                   (map (juxt key (fn-> val deref byte-size)))
                   (map (juxt (fn' ns-) first second)))))
       (apply concat)
       (sort-by #(get % 2)))))
