(ns quantum.test.core.data.complex.csv
  (:require [quantum.core.data.complex.csv :refer :all]))

(defn test:parse
  ([text])
  ([text {:as opts :keys [as-vector? as-map? as-lseq? reducer? headers]}]))