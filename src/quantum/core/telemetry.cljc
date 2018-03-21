(ns quantum.core.telemetry
  (:require
    [com.stuartsierra.component  :as comp]
    [quantum.core.async          :as async
      :refer [go-loop]]
    [quantum.core.async.pool     :as pool]
    [quantum.core.data.validated :as dv]
    [quantum.core.fn
      :refer [firsta]]
    [quantum.core.logic
      :refer [whenf]]
    [quantum.core.resources      :as res]
    [quantum.core.system         :as sys]
    [quantum.core.type           :as t]))

#_(t/def ::offloader keyword? "The offloader implementation to use")

#_(t/def ::config
  (t/keys :req-un
    [(spec :destinations
       (t/of map?
         (spec keyword? "destination ident")
         (t/keys :req-un
           [::offloader
            (spec :stats    (t/of set? keyword?) "The idents of the stats to gather")
            (spec :tags     (t/of map? named? named?))
            (spec :interval pos-int?             "In millis")
            (spec :config   map?                 "destination config")])))]))

(defmulti >stats "Given a stats ident, generates stats." firsta)

(defmulti stats>measurements
  "Given an offloader and a particular collection of stats, generates measurements." firsta)

(defmulti combine-measurements
  "Given an offloader and multiple collections of measurements, combines them into one
   (optionally tagged) collection of measurements." firsta)

(defmulti offload! "Offloads the provided measurements via the offloader and config." firsta)

(defn offload-stats! [offloader #_::offloader stats-idents tags config]
  (let [measurements|uncombined (->> stats-idents (map #(stats>measurements [offloader %] (>stats %))))
        measurements (combine-measurements offloader measurements|uncombined tags)]
    (offload! offloader measurements config)))

(defmethod >stats ::sys/memory [_] (sys/mem-stats))
(defmethod >stats ::sys/thread [_] (sys/thread-stats))
(defmethod >stats ::sys/cpu    [_] (sys/cpu-stats))

(res/defcomponent Telemeter
  ^{:doc "Component for handling telemetry ('measuring at a distance').
          Handles performing measurements and sending them to destinations.
          Theoretically this could handle logging as well, but that's for later."}
  [config executor]
  ([this]
    (let [tasks (->> config :destinations
                     (mapv (fn [[destination-ident
                                 {:keys [offloader stats tags interval config]}]]
                             {:ident destination-ident :interval interval
                              :f (fn [] (offload-stats! offloader stats tags config))})))]
      (assoc this :executor (comp/start (pool/>interval-executor {:tasks tasks})))))
  ([this] (update this :executor #(do (comp/stop %) nil))))

(defn >telemeter [config #_::config] (map->Telemeter {:config config}))

(res/register-component! ::telemeter >telemeter [])
