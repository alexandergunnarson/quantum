(ns quantum.test.apis.google.places.core
  (:require [quantum.apis.google.places.core :refer :all]))

#_(defn test:search
  [coord api-key & [{:keys [radius search-type place-types parse?]
                       :or {radius 50000}}]])