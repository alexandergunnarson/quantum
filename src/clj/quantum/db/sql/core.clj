(ns quantum.db.sql.core
  (:require-quantum [:lib]))

(defn connection-str [^Map m]
  (->> (for [k v m]
         (let [v-f (cond (true? v) "True" (false? v) "False" :else v)]
           (str k "=" v-f)))
       (str/join ";")))
