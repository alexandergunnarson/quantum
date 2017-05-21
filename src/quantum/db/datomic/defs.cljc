(ns quantum.db.datomic.defs
  (:refer-clojure :exclude [reduce])
  (:require [quantum.core.collections  :as c
              :refer [remove+ reduce]]
            [quantum.net.http          :as http]
    #?(:clj [instaparse.core           :as insta])
            [quantum.db.datomic        :as db]
            [quantum.db.datomic.core   :as dbc]
            [quantum.db.datomic.schema :as dbs]
            [quantum.validate.specs    :as sp]
            [quantum.db.datomic.fns    :as fns]))

(def mime-types-source "http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types")

#?(:clj
(def parser ; for what filetype?
  (insta/parser
    "lines      = (line <('\n' | '\r')>)* line?
     line       = <comment> / content
     comment    = <'#'> #'.*'
     content    = mime-type <'\t'+> extensions
     mime-type  = word
     extensions = (extension <' '>)* extension?
     extension  = word
     word       = #'[^\\s]+'"
    :start
    :lines)))

#?(:clj
(defonce mime-types ; TODO time-based cache invalidation
  (->> (http/request! {:url mime-types-source})
       :body
       parser
       (insta/transform
         {:lines     (fn [& lines] (->> lines
                                        (remove+ nil?)
                                        (reduce #(c/conj %1 %2) {})))
          :word      identity
          :extension keyword
          :mime-type keyword
          :extensions (fn [& args] (into #{} args))
          :content    (fn [mime-type extensions] [mime-type extensions])
          :line       (fn ([] nil) ([line] line))})
       delay)))

(defn transact-std-definitions! [conn & [{:as opts :keys [mime-types?]}]]
  (db/transact! conn (dbc/->partition conn :db.part/test))
  (db/transact! conn (dbc/->partition conn :db.part/fn  ))
  (db/transact! conn [(db/conj conn :db.part/db {:db/ident :dummy})])
  #?(:clj (fns/define-std-db-fns! conn))
  #?(:clj (dbs/transact-schemas! {:conn conn}))
  #_(db/conj! (dbs/->globals {:db/ident :globals*}))

  #?(:clj
    (when mime-types?
      (db/transact! conn
        (->> mime-types force
             (mapv (fn [[k v]] (db/conj conn :db.part/defs
                                 (sp/->data:format
                                   {:data:mime-type                  k
                                    :data:appropriate-extension:many v})))))))))

