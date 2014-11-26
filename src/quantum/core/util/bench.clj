(ns quantum.core.util.bench
  (:require
    [quantum.core.ns     :as ns :refer [defalias]]
    [quantum.core.string :as str]
    [criterium.core :as bench])
  (:gen-class))

(defn num-from-timing [time-str]
  (-> (str/replace time-str "\"" "")
      (str/replace "\n" "")
      (str/replace "Elapsed time: " "")
      (str/replace " msecs" "")
      (read-string)))
(defn shoddy-benchmark [to-repeat func & args]
  (let [times-list
          (take to-repeat
            (repeatedly (fn [] (num-from-timing
                          (with-out-str (time (apply func args)))))))]
  (/ (apply + times-list) to-repeat))) ; average
(defalias bench bench/quick-bench)
(defalias complete-bench bench/bench)

; (defn shoddy-benchmark1 [to-repeat func & args]
;   (let [results (transient [])]
;     (dotimes [n to-repeat]
;       (->> (apply func args) time with-out-str num-from-timing (conj! results)))
;     (println "results:" (persistent! results))
;     (->> results (apply +) (#(/ % to-repeat)))))