(ns quantum.ui.core
  (:require [com.stuartsierra.component :as component]
   #?(:cljs [reagent.core               :as rx])
            [quantum.core.error         :as err]
            [quantum.core.log           :as log]
            [quantum.core.loops
              :refer  [until]]
            [quantum.core.async         :as async]
            [quantum.core.validate      :as v
              :refer  [validate]]
            [quantum.core.resources     :as res]))

(defrecord
  ^{:doc "An abstraction for a renderer.

          @type      : The renderer type. Currently only Reagent is
                       supported.
          @init-fn   : An optional function used to initialize the
                       UI or frontend in some (any) way.
          @render-fn : A render function. Generally for e.g. Reagent
                       this will be the root node's render function.
          @root-id   : Used mainly by React and like frameworks (e.g.
                       Reagent). The root ID of the node to which e.g.
                       React mounts."}
  Renderer
  [type init-fn render-fn root-id]
  component/Lifecycle
  (start [this]
    (let [root-id (or root-id "root")
          type    (or type :reagent)]
      (validate type #{:reagent})
      (assert (not (async/web-worker?)))

      (when init-fn
        (validate init-fn fn?)
        (init-fn))

      (case type
        :reagent
        (do (validate render-fn fn?
                      root-id   string?)
              ; If the root node does not exist, creates it as a div and
              ; appends it to the DOM
              #?(:cljs
              (let [elem (or (.getElementById js/document root-id)
                             (doto (.createElement js/document "div")
                                   (-> .-id (set! root-id))
                                   (->> (.appendChild (.-body js/document)))))]

                (log/pr :debug "Now rendering on root node, id" root-id)

                (rx/render [render-fn] elem)))))
      this))
  (stop [this]
    ; There's not really a "stop rendering" function... short of simply
    ; removing the ID, which is not a desired or reasonable outcome.
    this))

#?(:cljs (res/register-component! ::renderer map->Renderer [::log/log ::quantum.db.datomic/db]))
