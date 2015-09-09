(ns
  ^{:doc "Benchmarking utilities. Criterium is aliased and is especially useful."
    :attribution "Alex Gunnarson"}
  quantum.core.util.bench
  (:require-quantum [ns str fn logic])
  #?(:clj (:require [criterium.core :as bench]))
  #?(:clj (:import com.carrotsearch.sizeof.RamUsageEstimator quanta.ClassIntrospector)))

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

#?(:clj (defn byte-size [obj] (RamUsageEstimator/sizeOf obj)))

#?(:clj
(defn byte-size-alt [obj]
  (-> (ClassIntrospector.)
      (.introspect obj)
      (.getDeepSize))))

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
       (sort-by (f*n get 2)))))