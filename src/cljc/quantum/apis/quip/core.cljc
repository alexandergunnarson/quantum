(ns quantum.apis.quip.core
  #_(:require-quantum [:lib http auth])
  #_(:require [hickory.core   :as hp]
            [hickory.select :as hs]))

#_(defn request! [req]
  (http/request!
    (assoc req
      :oauth-token (auth/datum :quip :access-token))))

#_(defn get-thread [id]
  (request!
    {:url (str "https://platform.quip.com/1/threads/" id)}))

#_(defn parse-spreadsheet [thread-id]
  (let [html (-> (get-thread thread-id) http/parse-json :html)
        table (->> html
                   hp/parse
                   hp/as-hickory
                   (hs/select
                     (hs/descendant
                       (hs/tag :table)))
                   first :content)
        column-letters
          (->> table first :content first :content
               (map (fn-> :content first)))
        columns
          (->> table second :content
               (mapv (fn->> :content
                            (mapv (fn-> :content first :content first
                                        (whenf map? (fn-> :content first)))))))]
    (coll/zipmap map/om (first columns) (-> columns rest coll/pivot))))

#_(def ^{:doc "Checks whether the argument is a singleton string consisting of
             the zero-width space character."
       :todo ["Move to other namespace?"]}
  essentially-empty-string?
  (fn-and string? coll/single? (fn-> first core/int (= 8203))))

#_(def essentially-empty? (fn-or nil? essentially-empty-string?))