(ns quantum.db.datomic.defs
           (:require [quantum.core.collections   :as c    
                       :refer [remove+]                   ]
                     [quantum.net.http           :as http ]
                     [instaparse.core            :as insta]
                     [quantum.db.datomic         :as db   ]
                     [quantum.db.datomic.schemas :as s    ])
  #?(:cljs (:require-macros
                     [quantum.core.collections   :as c    ])))

(def mime-types-source "http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types")

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
    :lines))

(defonce mime-types ; TODO time-based cache invalidation
  (->> (http/request! {:url mime-types-source})
       :body
       parser
       (insta/transform
         {:lines     (fn [& lines] (->> lines
                                        (remove+ nil?)
                                        force
                                        (reduce #(c/conj %1 %2) {})))
          :word      identity
          :extension keyword
          :mime-type keyword
          :extensions (fn [& args] (into #{} args))
          :content    (fn [mime-type extensions] [mime-type extensions])
          :line       (fn ([] nil) ([line] line))})
       delay))

(defn transact-std-definitions! []
  ; Transact mime-types
  (db/transact!
    (->> mime-types force
         (mapv (fn [[k v]] (db/conj (s/->data:format
                                      {:data:mime-type                  k
                                       :data:appropriate-extension:many v})))))))





