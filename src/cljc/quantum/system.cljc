(ns ^{:doc "A namespace for bootstrapping/streamlining system creation."}
  quantum.system
           (:require [com.stuartsierra.component       :as component]
            #?(:cljs [reagent.core                     :as rx       ])
                     [quantum.core.core
                       :refer        [?deref lens]                  ]
                     [quantum.core.fn                  :as fn
                       :refer        [<- fn-> with-do]]
                     [quantum.core.log                 :as log]
                     [quantum.core.macros.core         :as cmacros
                       :refer        [if-cljs]]
                     [quantum.core.resources           :as res      ]
                     [quantum.core.async               :as async    ]
                     [quantum.core.collections         :as coll
                       :refer [map']]
                     [quantum.db.datomic               :as db       ]
                     [quantum.db.datomic.core          :as dbc      ]
                     [quantum.net.http                 :as http     ]
                     [quantum.net.websocket            :as conn     ]
                     [quantum.ui.core                  :as ui       ]))

(log/this-ns)


