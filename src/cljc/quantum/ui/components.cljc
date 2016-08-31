(ns
  ^{:doc "Namespace for creating/defining UI components, especially
          with |defcomponent|."
    :attribution "Alex Gunnarson"}
  quantum.ui.components
           (:refer-clojure :exclude [for reduce])
           (:require
    #?(:cljs [cljs.core.async     :as async])
    #?(:cljs [reagent.core        :as rx   ])
    #?(:cljs [re-frame.core
               :refer [subscribe dispatch dispatch-sync reg-event reg-sub]])
             [quantum.ui.revision :as rev  ]
             [quantum.core.fn     :as fn
               :refer        [#?@(:clj [fn-> fn->> <-])]
               :refer-macros [fn-> fn->> <-]]
             [quantum.core.error  :as err  
               :include-macros true]
             [quantum.core.log    :as log  
               :include-macros true]
             [quantum.db.datomic  :as db   ]
             [quantum.core.system :as sys 
               :refer        [#?@(:cljs [ReactNative])]]
             [quantum.core.collections :as coll
               :refer        [#?@(:clj [for fori join kmap reduce])
                              map-vals+ ensurec merge-deep]
               :refer-macros [for fori join kmap reduce]]
             [quantum.ui.style.core
               :refer [layout-x layout-y layout layout-perp
                       layout-direction layout-fit autofit
                       layout-wrap scaled]])
  #?(:cljs (:require-macros
             [reagent.ratom
               :refer [reaction]]
             [cljs.core.async.macros
               :refer [go]])))  

(log/this-ns)

#?(:cljs
(defn table
  "An HTML table component.
   Expects rows to be indexed or grouped in some way."
  {:usage `[table (reaction {1 {:a 1 :b 2 :c 3}
                             2 {:a 4 :b 5 :c 6}})
                  (reaction [:a :b :c])]}
  [data col-getters]
  (fn table* []
    (let [gen-rows (fn [v split-fn k-display]
                     (for [d (split-fn v)]
                       (join [:tr [:td k-display]]
                         (for [getter @col-getters]
                           [:td (-> d getter pr-str)]))))]
      (join [:table]
        (for [[k v] @data]
          (join (first (gen-rows v (fn-> first vector) k  ))
                       (gen-rows v rest                nil))))))))

#?(:cljs
(defn render-db
  "A component which renders a DataScript or Datomic DB."
  [db]
  (fn []
    [table
      (reaction (->> @db db/db->seq (group-by :e) seq))
      (reaction [:a :v :added])])))

#?(:cljs
(defn alert [title]
  (if (= sys/os "web")
      (go (js/alert title)) ; even this totally stops everything
      (.alert (.-Alert @ReactNative) title))))

#?(:cljs (def text                (rx/adapt-react-class (.-Text  ReactNative))))
#?(:cljs (def view                (rx/adapt-react-class (.-View  ReactNative))))
#?(:cljs (def image               (rx/adapt-react-class (.-Image ReactNative))))
; var CacheImage = require('@remobile/react-native-cache-image'); doesn't work on web ; better to have something else
#?(:cljs (def touchable-highlight (rx/adapt-react-class (.-TouchableHighlight ReactNative))))
#?(:cljs (def accordion           (when-not (= sys/os "web")
                                    (rx/adapt-react-class (js/require "react-native-accordion")))))
#?(:cljs (def text-input          (rx/adapt-react-class (.-TextInput          ReactNative))))
#?(:cljs (def modal               (when-not (= sys/os "web")
                                    (rx/adapt-react-class (.-Modal ReactNative)))))
#?(:cljs (def scroll-view         (rx/adapt-react-class (.-ScrollView         ReactNative))))
#?(:cljs (def list-view           (rx/adapt-react-class (.-ListView           ReactNative))))
#?(:cljs (def video               (if (= sys/os "web")
                                      :video
                                       ; https://github.com/react-native-community/react-native-video
                                      #_(rx/adapt-react-class (js/require "react-native-video")))))
#?(:cljs (def list-view-data-source (-> ReactNative .-ListView .-DataSource)))

; https://github.com/react-native-community/react-native-blur
#?(:cljs (def Blur          (when-not (= sys/os "web")
                              (err/ignore (js/require "react-native-blur")))))
#?(:cljs (def blur-view*    (when Blur
                              (rx/adapt-react-class (.-BlurView     Blur)))))
#?(:cljs (def vibrancy-view (when Blur
                              (rx/adapt-react-class (.-VibrancyView Blur)))))
#?(:cljs (def ios-blur-view (when (= sys/os "ios")
                              (err/ignore
                                (-> (js/require "react-native-fxblurview")
                                    .-default
                                    rx/adapt-react-class)))))

#?(:cljs
(reg-event :register-blur
  (fn [db [_ blur-id]]
    (update db :blur-ids
      (fn-> (ensurec {}) (assoc blur-id true))))))

#?(:cljs
(reg-event :deregister-blur
  (fn [db [_ blur-id]]
    (update db :blur-ids
      (fn-> (ensurec {}) (dissoc blur-id))))))

(def set-dynamic
  (fn [db [_ blur-id dynamic?]]
    (update db :blur-ids
      (fn-> (ensurec {}) (assoc blur-id dynamic?)))))

#?(:cljs (reg-event :set-dynamic set-dynamic))

#?(:cljs
(reg-event :recompute-blur
  (fn [db [_ blur-id]] (set-dynamic db [_ blur-id true]))))

#?(:cljs
(reg-event :recompute-blurs
  (fn [db _]
    (update db :blur-ids
      (fn->> (map-vals+ (constantly true)) (join {}))))))

#?(:cljs
(reg-sub :dynamic?
  (fn [db [_ id]]
    (-> db :blur-ids (get id)))))

#?(:cljs
(defn ios-blur-once [props background]
  (let [id (or (:id props) (gensym))
        dynamic-requested? (:dynamic props)
        _ (dispatch-sync [:register-blur id])
        dynamic? (subscribe [:dynamic? id])]
    (rx/create-class
      {:component-will-unmount
         (fn [] (dispatch-sync [:deregister-blur id]))
       :reagent-render
         (fn [props] (let [props-f (assoc props :dynamic @dynamic?)]
                       (when (and (not dynamic-requested?) @dynamic?)
                         (dispatch [:set-dynamic id false]))
                       [ios-blur-view props-f background]))}))))

#?(:cljs
(defn blur-view [props background & children]
  (fn []
    (case sys/os
      "ios"     (into
                  [view (dissoc props :blur-enabled :blur-radius :dynamic :id)
                    [ios-blur-once (select-keys props
                                     [:blur-enabled :blur-radius :dynamic :id])
                      background]]
                  children)
      "android" (throw (ex-info "Android hasn't yet been figured out for blur effect" {}))
                #_[blur-view (select-keys props
                               [:view-ref :blur-radius
                                :downsample-factor :overlay-color])
                    background]  ; view-ref must be set on this
      "web" (into [view (-> props (dissoc :blur-enabled :blur-radius :dynamic :id))
                    [view {:style {:filter (str "blur(" (:blur-radius props) "px)")
                                   layout-fit autofit}}
                      background]]
                  children)))))

; https://github.com/BradLarson/GPUImage
#?(:cljs
(def image+ (if (= sys/os "web")
                image
                (-> (js/require "quantum-react-native-gpuimage")
                    (rx/adapt-react-class)))))

(defn grid-scroll-view [parent-props content-fn]
  (let [data-indices (range 0 (if (= sys/os "web") 15 5))
        margins      4
        width        (-> parent-props :width)]
    (fn []
      (println "rerendering with width" @width)
      (let [row-fit-number (int (/ @width 150))]
        (into [scroll-view
                (-> {:content-container-style
                          {layout-perp :stretch
                           layout-fit  autofit} ; shouldn't it stretch anyway without this?
                        :style {layout-direction  layout-x
                                layout-fit        autofit ; previously had manual height and width
                                :background-color :transparent
                                ;layout-wrap       :wrap ; doesn't work on scroll view 
                                :top              0
                                :position :absolute}}
                    (merge (when-not (= sys/os "web")
                             {:shows-horizontal-scroll-indicator false
                              :shows-vertical-scroll-indicator   false}))
                    (merge-deep parent-props)
                    (dissoc :width))]
            (when-not (= 0 @width)
              (fori [row-indices (->> data-indices (partition-all row-fit-number))
                     row-i]
                (let [first-row?    (= row-i 0)
                      last-row?     (= row-i (-> data-indices count dec))
                      total-margins (+ margins (* margins 2 row-fit-number) margins)
                      hw  (-> @width  
                              (- total-margins)
                              (/ row-fit-number))]
                  ^{:key (gensym)}
                  (into [view {:style {layout-direction  layout-x
                                       layout            :flex-start
                                       :height           (scaled hw)
                                       :margin-top       (scaled (if first-row? (* 2 margins) margins))
                                       :margin-bottom    (scaled (if last-row?  (* 2 margins) margins))}}]
                    (fori [calc-col-i row-indices col-i]
                      (let [first-col? (= col-i 0)
                            last-col?  (= col-i (-> row-indices count dec))]
                        ^{:key (gensym)}
                        [view {:style {layout-direction   layout-x
                                       layout-perp        :center
                                       layout             :center
                                       :border-radius     5
                                       :width             (scaled hw)
                                       :margin-left       (scaled (if first-col? (* 2 margins) margins))
                                       :margin-right      (scaled (if last-col?  (* 2 margins) margins))
                                       :background-color  :white}}
                          [content-fn calc-col-i]])))))))))))

; LinearGradient ; https://github.com/react-native-community/react-native-linear-gradient
; SideMenu ; https://github.com/react-native-community/react-native-side-menu
; Drawer (similar to SideMenu?) ; https://github.com/root-two/react-native-drawer
; tab-view ; https://github.com/react-native-community/react-native-tab-view
; Animatable (cool animations) ; https://github.com/oblador/react-native-animatable
; Maps (better than RN's core version) ; https://github.com/lelandrichardson/react-native-maps ; https://github.com/lelandrichardson/react-native-maps/blob/master/docs/installation.md for installation on Android
; Icons ; https://github.com/oblador/react-native-vector-icons

; table-view ; https://github.com/aksonov/react-native-tableview

