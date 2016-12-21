(ns ^{:doc "A namespace for bootstrapping/streamlining system creation."}
  quantum.system
           (:require [com.stuartsierra.component       :as component]
            #?(:cljs [reagent.core                     :as rx       ])
                     [quantum.core.core
                       :refer        [deref* lens]                  ]
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
                     [quantum.db.datomic.reactive.core :as db-rx    ]
                     [quantum.net.http                 :as http     ]
                     [quantum.net.websocket            :as conn     ]
                     [quantum.ui.core                  :as ui       ]))

(log/this-ns)

(defn gen-main
  "Creates a standard |-main| function.
   For Clojure, this is for JAR packaging.
   For ClojureScript, this can be used e.g. with Figwheel's :main."
  [config system-creator system sys-map]
  (fn [& [port]]
    (when @system
      (res/stop! @system))
    @system-creator
    (res/go! @system)
    (reset! db/conn* (-> @sys-map ::db/db #?(:clj :backend :cljs :ephemeral) :conn #?(:clj deref*))) ; TODO set this in the DB NS or make a lens in datomic into global system
    #?(:clj (reset! dbc/part* (-> @sys-map ::db/db :backend :default-partition)))  ; TODO set this in the DB NS or make a lens in datomic into global system
    #_(:cljs (when (-> @sys-map :db :ephemeral :reactive?)
               (db-rx/react! @dbc/conn*)
               (log/pr ::debug "Ephemeral DB made reactive."))) ; Is this necessary?
    true))

#?(:clj
(defmacro create-system-vars
  "Generally @system-kw should be simply ::system.
   @config : A config-map."
  [stop-system? system-kw config]
  (let [main-sym (with-meta '-main {:export true})
        system-code `(lens res/systems ~system-kw)
        err      (if-cljs &env 'js/Error 'Throwable)]
    `(do (def ~'system  ~system-code)
         (def ~'sys-map (lens ~'system (fn-> :sys-map deref*)))

         (try
           (when (and ~stop-system? (deref* ~system-code))
             (res/stop! @~system-code)
             (when-let [sys-map# (-> res/systems deref ~system-kw :sys-map deref)]
               (res/stop! sys-map#)))
           (catch ~err e#
             (log/pr :warn "Could not stop system." e#)))

         ; In CLJS, ~main-sym content can't refer to e.g. ~'sys-map unless
         ; it has been previously declared outside of the macro
         (defn ~main-sym [& args#]
           (when-not (async/web-worker?)
             (let [system-kw# ~system-kw
                   system#    (lens res/systems system-kw#)
                   sys-map#   (lens system# (fn-> :sys-map deref*))
                   config#    ~config]
               (apply (gen-main config#
                                (delay (res/register-system! system-kw# config#))
                                system#
                                sys-map#)
                      args#))))))))
