(ns quantum.ui.form
  (:require
   [reagent.core :as re]
   [quantum.core.ns          :as ns        :refer [Num Vec Key Seq JSObj]        ]
   [quantum.core.data.map    :as map       :refer [map-entry]          ]
   [quantum.core.data.vector :as vec                                   ]
   [quantum.core.function    :as fn        :refer [f*n compr]                ]
   [quantum.core.logic       :as log       :refer [fn-not fn-and fn-or splice-or nnil?]]
   [quantum.core.numeric     :as num       :refer [neg]                ]
   [quantum.core.type        :as type      :refer [instance+? class]   ]
   [quantum.core.string      :as str                                   ]
   [clojure.walk                           :refer [postwalk]           ]
   [figwheel.client          :as fw                                    ]
   [garden.core              :as css       :refer [css]                ]
   [garden.stylesheet        :as css-style                             ]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]]
   [goog.object :as gobject]
   [quantum.core.error     :refer [with-throw]]
   [quantum.core.collections :as coll :refer
     [redv redm into+ reduce+
      rest+ first+ single?
      lasti takeri+ taker-untili+
      split-remove+ ffilter remove+
      last-index-of+ index-of+
      dropl+ dropr+ takel+
      getr+ interpose+ in?
      map+ filter+ lfilter lremove group-by+
      merge+ key+ val+
      merge-keep-left]])
  (:require-macros
   [cljs.core.async.macros :refer [go-loop go]]
   [quantum.core.function  :refer [fn->> fn-> <-]]
   [quantum.core.logic     :refer [whenf whenf*n whenc whencf*n condf*n]]))  

(def ui-tag-domain (atom #{}))

; TODO: Don't use |postwalk| and you won't have to check so many things.
(def ui-element?
  (fn-and
    vector?                ; element
    (fn-> first+ keyword?) ; tag  
    (fn-> first+ (not= :style)) ; to make sure it's not just an attribute ; check this further
    (fn-or
      (fn-> second coll?)
      (fn-> first+ name (str/contains? ".")))))  ; attributes

(defn determine-tag-domain!
  [^Vec html]
  (->> html
       (postwalk ; [:style {...}] ; should be map-entry but... and this will happen with other attributes
         (fn [elem]
           (when (ui-element? elem)
             (let [^Key tag (first elem)]
               (swap! ui-tag-domain conj tag)))
           elem))))
