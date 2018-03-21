(ns quantum.apis.librato.telemetry
  (:require
    [quantum.apis.librato.core :as librato]
    [quantum.core.system       :as sys]
    [quantum.core.telemetry    :as tel]))

(defn sys-mem-stats>measurements #_> #_(seq-of ::librato/measurement)
  [mem-stats #_(spec "Output of sys/mem-stats")]
  [{:name :sys.memory.total             :value (get-in mem-stats [:system   :total])}
   {:name :sys.memory.used              :value (get-in mem-stats [:system   :used])}
   {:name :vm.memory.heap.max           :value (get-in mem-stats [:heap     :max])}
   {:name :vm.memory.heap.used          :value (get-in mem-stats [:heap     :used])}
   {:name :vm.memory.non-heap.committed :value (get-in mem-stats [:non-heap :committed])}
   {:name :vm.memory.non-heap.used      :value (get-in mem-stats [:non-heap :used])}])

(defmethod tel/stats>measurements [:quantum.apis.librato ::sys/memory] [_ mem-stats]
  (sys-mem-stats>measurements mem-stats))

(defn sys-thread-stats>measurements #_> #_(seq-of ::librato/measurement)
  [thread-stats #_(spec "Output of sys/thread-stats")]
  [{:name :vm.threads.ct         :value (get thread-report :ct)}
   {:name :vm.threads.daemon-ct  :value (get thread-report :daemon-ct)}
   {:name :vm.threads.started-ct :value (get thread-report :started-ct)}])

(defmethod tel/stats>measurements [:quantum.apis.librato ::sys/thread] [_ thread-stats]
  (sys-thread-stats>measurements thread-stats))

(defn sys-cpu-stats>measurements #_> #_(seq-of ::librato/measurement)
  [cpu-stats #_(spec "Output of sys/cpu-stats")]
  [{:name :sys.cpu :value (get cpu-report :system)}
   {:name :vm.cpu  :value (get cpu-report :this)}])

(defmethod tel/stats>measurements [:quantum.apis.librato ::sys/cpu] [_ cpu-stats]
  (sys-cpu-stats>measurements cpu-stats))

(defmethod tel/combine-measurements :quantum.apis.librato [_ xs tags #_(s/of map? t/named? t/named?)]
  {:tags         tags
   :measurements (reduce into [] xs)})

(defmethod tel/offload! :quantum.apis.librato [_ measurements config]
  (librato/post-metrics! config measurements))
