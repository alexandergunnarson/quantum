#?(:clj (ns quantum.core.util.bench))

(ns
  ^{:doc "Benchmarking utilities. Criterium is aliased and is especially useful."
    :attribution "Alex Gunnarson"}
  quantum.core.util.bench
  (:require
    [quantum.core.ns     :as ns #?@(:clj [:refer [defalias]])]
    [quantum.core.string :as str]
    #?(:clj [criterium.core :as bench]))
  #?(:clj (:gen-class)))

#?(:clj 
  (defn num-from-timing [time-str]
    (-> (str/replace time-str "\"" "")
        (str/replace "\n" "")
        (str/replace "Elapsed time: " "")
        (str/replace " msecs" "")
        (read-string))))

#?(:clj 
  (defn shoddy-benchmark [to-repeat func & args]
    (let [times-list
            (take to-repeat
              (repeatedly (fn [] (num-from-timing
                            (with-out-str (time (apply func args)))))))]
    (/ (apply + times-list) to-repeat)))) ; average

#?(:clj (defalias bench bench/quick-bench))
#?(:clj (defalias complete-bench bench/bench))

; (defn shoddy-benchmark1 [to-repeat func & args]
;   (let [results (transient [])]
;     (dotimes [n to-repeat]
;       (->> (apply func args) time with-out-str num-from-timing (conj! results)))
;     (println "results:" (persistent! results))
;     (->> results (apply +) (#(/ % to-repeat)))))