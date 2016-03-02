(ns quantum.style
  (:require-quantum [:core fn logic err log pr res])
  (:require        [quantum.system        :as sys ]
                   [quantum.ui.style.css.core :as css
                     :refer [theme]]
                   [quantum.ui.style.css.dom  :as css-dom]
                   [quantum.ui.style.color    :as color :refer [color]]
                   [quantum.ui.style.fonts
                     :refer [family]]
                   [garden.core                  :refer [css]]
                   [garden.stylesheet            :refer [at-keyframes]]
                   [clojure.walk                 :refer [postwalk]]
                   [quantum.core.type.predicates :refer [map-entry?]]
                   [garden.selectors :as s])
  (:require-macros [garden.def :refer [defkeyframes]]))

(swap! css/themes assoc
  :sky (->> {:light-blue "#25c6f2"
             :red        "#e7735f"}
            (map (fn [[k v]] [k (color/->hsla v)]))
            (into {})))

(defn trans
  "Defines a transition."
  [id & props]
  [(at-keyframes (-> id name (str "-in"))
     [:from {:-webkit-transition [:width :height]
             :transition         [:width :height]}]
     [:to   {:-webkit-transition [:width :height]
             :transition         [:width :height]}])
   (at-keyframes (-> id name (str "-out"))
     [:from {:-webkit-transition [:width :height]
             :transition         [:width :height]}]
     [:to   {:-webkit-transition [:width :height]
             :transition         [:width :height]}])])

(def style
  (->
    [
   ; CSS reset
   [:html :body :span :object :iframe
    :h1 :h2 :h3 :h4 :h5 :h6 :p
    :blockquote :pre :abbr :address
    :cite :code :del :dfn :em :img 
    :ins :kbd :q :samp :small :strong
    :sub :sup :var :b :i :dl :dt :dd
    :ol :ul :li :fieldset :form :label
    :legend :table :caption :tbody :tfoot
    :thead :tr :th :td
    :article :aside :figure :footer :header
    :hgroup :menu :nav :section :time :mark
    :audio :video
    {:margin          0
      :padding        0
      :border         0
      :outline        0
      :font-size      "100%"
      :vertical-align :baseline
      :background     :transparent}]
   [:html :body
     {:height  "100%"
      :width   "100%"
      :font                   (family :montserrat)
      :font-size              "14px"
      :background             (theme :sky :light-blue) ;"#eaeaea"
      :color                  :white
      :font-smoothing         :antialiased}]
   [:h1.name
     {:align :center
      :font-size "30px"
      :margin-top 15
      :margin-bottom 15}]
   [:h2.resume
     {:align :center
      :font-size "20px"
      :margin-top 25
      :margin-bottom 15}]
   [:#root
     {:display                :flex
      :flex-direction         :column
      :justify-content        :center
      :align-items            :center
      :font-family            (family :montserrat)}]
   [:#div-root
     {:display                :flex
      :flex-direction         :column
      :justify-content        :stretch
      :align-items            :stretch
      }]
   [:button
     {:font-family (family :montserrat)
      :font-size   12
      :background  :white
      :color       (theme :sky :light-blue)}]
   [:div#current-weather
     {:display :flex
      :flex-direction :column
      :align-items :center
      :justify-content :center}]
   [:div#forecast
     {:display :flex
      :flex-direction :column
      :align-items :center}
     [:table
       {:border-spacing 10
        :padding 10}]]
   [:div.row
     {:display             :flex
      :flex-direction      :row
      ;:align-items         :center
      ;:justify-content     :center
      ;:margin-bottom       20
      :font-family         (family :montserrat)
      :background          "rgba(0, 0, 0, 0.1)"
      :transition-name     :color-out
      :transition-duration "0.1s"}]
   [:div.row:hover
     {;:color              :red ;(color/darken (color :white) 0.4)
      :transition-name     :color-in
      ;:font-size "25px"
      :background          "rgba(0, 0, 0, 0.2)"
      :transition-duration "0.1s"
      ;:transition [[:all "0.4s" :ease-in-out]]
      }]
   [:div.weather-item.selected
       {:background "rgba(0, 0, 0, 0.2)"
        :color      :inherit}]
   [:div.weather-item:hover
     {:transition-name     :color-in
      :transition-duration "0.4s"
      :background          "rgba(0, 0, 0, 0.2)"
      :color               "rgba(255, 255, 255, 0.9)"
      }]
   [:img.cutout
     {:border-radius "50%"
       :color :inherit}]
   [:div.cutout
     {:width 200
      :height 200
      :border-radius "100px"
      :box-shadow [[0 0 "8px" "rgba(0, 0, 0, .8)"]]}]
   [:div#resume
     {:display :flex
      :flex-direction :column
      :align-items :center
      :justify-content :center
      }]
   [:div.caption1
     {:text-transform         :uppercase
      :font-size              "25px"
      :font-family            (family :lato)}]
   [:div#status-span
     {:display :flex
      :flex-direction :row
      :width "100%"
      :align-items :center
      :justify-content :center
      :margin-bottom   10}]
   [:.p5
     {:font-size      "12px"
      :text-transform :uppercase
      :letter-spacing 1}]
   [:a:link :a:visited
     {:color (theme :sky :red)
      :font-size :inherit}]
   [:p#footer
     {:margin-top 25
      :margin-bottom 15
      :color :inherit}]
   [:#top-bar
     {:padding                30
      :background             (theme :sky :red)
      :box-shadow             [["0px" "5px" "5px" "rgba(0, 0, 0, 0.4)"]]
      :font-family            (family :montserrat)
      :font-size              "20px"
      :text-transform         :uppercase
      :letter-spacing         2
      :display                :flex
      :flex-direction         :row
      :align-items            :baseline
      :justify-content        :space-around}]
    [:div#status
     {:font-size      "60px"
      :font-weight    100
      :text-transform :uppercase
      :font-family    (family :lato)
      :letter-spacing 5
      :margin-top     20}]
   ; :white-space        :pre
   ; :word-break         :break-word
   ; :list-style :none
     ]
  (into 
    (apply concat
      [(trans :size  :width :height)
       (trans :color :background :color)]))))