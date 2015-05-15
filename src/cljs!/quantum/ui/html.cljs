(ns
  ^{:doc "This namespace is for HTML-specific things."
    :attribution "Alex Gunnarson"}
  quantum.ui.html
  (:require
   [quantum.core.ns          :as ns        :refer [Num Vec Key]        ]
   [quantum.core.data.map    :as map                                   ]
   [quantum.core.data.vector :as vec                                   ]
   [quantum.core.function    :as fn        :refer [f*n]                ]
   [quantum.core.logic       :as log       :refer [fn-not fn-and fn-or]]
   [quantum.core.numeric     :as num       :refer [neg]                ]
   [quantum.core.type        :as type      :refer [instance+?]         ]
   [clojure.walk                           :refer [postwalk]           ]
   [figwheel.client          :as fw                                    ]
   [garden.core              :as css       :refer [css]                ]
   [garden.stylesheet        :as css-style                             ]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]]
   [quantum.core.collections :refer
     [redv redm into+ reduce+
      rest+ first+
      lasti takeri+ taker-untili+
      split-remove+
      last-index-of+ index-of+
      dropl+ dropr+ takel+
      getr+
      map+ filter+ group-by+
      merge+ key+
      merge-keep-left]])
  (:require-macros
   [cljs.core.async.macros :refer [go-loop go]]
   [quantum.core.function  :refer [fn->> fn-> <-]]
   [quantum.core.logic     :refer [whenc whenf whenf*n whencf*n condf condf*n]]))