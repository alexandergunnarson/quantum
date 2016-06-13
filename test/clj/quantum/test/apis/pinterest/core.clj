(ns quantum.test.apis.pinterest.core
  #_(:require [quantum.apis.pinterest.core :as root])
  #_(:require-quantum [auth http url web]))

#_(defn lower-ids-are-previous-ids? []
  (let [id_created-date (->> @pin->meta
                             (coll/filter-vals+ map?)
                             (map-vals+ (fn-> :data :created-at))
                             (into (sorted-map)))]
    (->> id_created-date vals
         (filter+ string?)
         (map+ (fn-> (time/->instant "yyyy-MM-dd'T'HH:mm:ss")))
         (map+ :nanos)
         redv
         (apply <=))))