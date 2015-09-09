(ns quantum.core.data.complex.csv
  (:require-quantum [ns coll str err])
  #?(:clj (:require [clojure.data.csv :as csv])))

#?(:clj
(defn parse
  ([text] (parse text #{:as-vector?}))
  ([text {:as opts :keys [as-vector? as-map? as-lseq? reducer? headers]}]
    (let [block (csv/read-csv text)
          headers-f (or headers (first block))
          rows (if headers block (rest block))
          reducer-fn (if reducer? identity redv)]
      (cond
        as-lseq? 
          block
        as-map?
          (->> rows
               (map+ (fn [row]
                 (reducei
                    (fn [m datum n]
                      (assoc m (str/keywordize (get headers-f n)) datum))
                      {} row)))
               reducer-fn)
        :else 
          (throw+ (Err. :invalid-option nil opts)))))))