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
             [quantum.core.logic
               :refer        [#?@(:clj [whenf]) nnil?]
               :refer-macros [whenf]]
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

#?(:cljs (defn rx-adapt [super sub]
           (when super
             (whenf (aget super sub) nnil? rx/adapt-react-class))))

#?(:cljs (def text                (rx-adapt ReactNative "Text" )))
#?(:cljs (def view                (rx-adapt ReactNative "View" )))
#?(:cljs (def image               (rx-adapt ReactNative "Image")))
; var CacheImage = require('@remobile/react-native-cache-image'); doesn't work on web ; better to have something else
#?(:cljs (def touchable-highlight (rx-adapt ReactNative "TouchableHighlight")))
#?(:cljs (def accordion           (when-not (= sys/os "web")
                                    (err/ignore (rx/adapt-react-class (js/require "react-native-accordion"))))))
#?(:cljs (def text-input          (rx-adapt ReactNative "TextInput")))
#?(:cljs (def modal               (when-not (= sys/os "web")
                                    (rx-adapt ReactNative "Modal"))))
#?(:cljs (def scroll-view         (rx-adapt ReactNative "ScrollView")))
#?(:cljs (def list-view           (rx-adapt ReactNative "ListView"  )))
#?(:cljs (def video               (if (= sys/os "web")
                                      :video
                                       ; https://github.com/react-native-community/react-native-video
                                      #_(rx/adapt-react-class (js/require "react-native-video")))))

; Uses StreamingKit
; Supported codecs (list incomplete):
#_"mp4 audio (m4a)
   mp3
   aac
   wav"
#_"  seekToTime <double seconds >
   , goForward  <double seconds >
   , goBack     <double seconds >
   , getStatus  <fn     callback>"
; https://github.com/tlenclos/react-native-audio-streaming
#?(:cljs
(def Audio (when-not (= sys/os "web")
             (err/ignore
               (.-ReactNativeAudioStreaming (js/require "react-native-audio-streaming"))))))

#?(:cljs (def audio (when (= sys/os "web") :audio)))

#?(:cljs (def list-view-data-source (err/ignore (-> ReactNative .-ListView .-DataSource))))

; https://github.com/react-native-community/react-native-blur
#?(:cljs (def Blur          (when-not (= sys/os "web")
                              (err/ignore (js/require "react-native-blur")))))
#?(:cljs (def blur-view*    (rx-adapt Blur "BlurView"    )))
#?(:cljs (def vibrancy-view (rx-adapt Blur "VibrancyView")))
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
                (err/ignore
                  (-> (js/require "quantum-react-native-gpuimage")
                      (rx/adapt-react-class))))))

#?(:cljs
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
                          [content-fn calc-col-i]]))))))))))))

; LinearGradient ; https://github.com/react-native-community/react-native-linear-gradient
; SideMenu ; https://github.com/react-native-community/react-native-side-menu
; Drawer (similar to SideMenu?) ; https://github.com/root-two/react-native-drawer
; tab-view ; https://github.com/react-native-community/react-native-tab-view
; Animatable (cool animations) ; https://github.com/oblador/react-native-animatable
; Maps (better than RN's core version) ; https://github.com/lelandrichardson/react-native-maps ; https://github.com/lelandrichardson/react-native-maps/blob/master/docs/installation.md for installation on Android
; Icons ; https://github.com/oblador/react-native-vector-icons

; table-view ; https://github.com/aksonov/react-native-tableview

#?(:cljs
(comment
(defn dropr-1-until-i [x i]
  (if (-> x count (> i))
      (pop x)
      x))


(defn grid-view-scroller
  "@data and @headers are reactive atoms"
  {:example '[grid-view-scroller
               {:width        500
                :height       500
                :headers      (rx/atom headers)
                :data         (reaction (get-file-data @state))
                :font-size    15
                :column-width (/ 500 4)}]}
  [{:keys [width height headers data column-width clamp-lines font-size
           line-height
           overscan-columns
           overscan-rows
           render-cell-fn]
    :or   {clamp-lines 3
           line-height 1}}]
  (fn []
    (let [data*    @data
          headers* @headers
          margin   5]
      [grid-view
        {:ref                    (name (gensym)) ; to not remount it
         :width                  width
         :height                 height
         :column-width           (+ (* 2 margin) column-width)
         :row-height             (+ (* 2 margin) (* font-size line-height clamp-lines))
         :columns-count          (count @headers)
         :rows-count             (count data*)
         :overscan-columns-count (or overscan-columns (min 20 (count headers*))) ; TODO these values are wrong
         :overscan-rows-count    (or overscan-rows    (min 20 (count data*   ))) ; TODO these values are wrong
         ;:useDynamicRowHeight true
         :render-cell   (or render-cell-fn
                          (fn [in]
                            (let [col (.-columnIndex in)
                                  row (.-rowIndex    in)]
                              (rx/as-element [:div.ellipsis {:style (merge (style/ellipsis clamp-lines font-size
                                                                            line-height)
                                                                           {:width column-width
                                                                            :margin margin})}
                                               (str (get-in data*
                                                      [row (-> headers* (get col) first)]))]))))}])))



(def this-val (fn-> (.-target) (.-value)))

(defn field-changed! [^Key comp-key]
  (when-not (-> @state comp-key :changed?)
    (swap! state assoc-in [comp-key :changed?] true)))

(defn clear-field-if-not-changed!
  "Starts out as grayed out text, and on first click, is cleared."
  [^Key comp-key]
  (when-not (-> @state comp-key :changed?)
    (swap! state assoc-in [comp-key :text] "")
    (field-changed! comp-key)))

(defn field-template
  {:examples '[[:input (fn/field-template :username)]
               [:input (fn/field-template :password)]
               [:input (fn/field-template :pin     )]]}
  [^Key k]
  (let [^Key comp-key (str/keyword+ k "-field")]
    {:type  (cond
              (= k :username)
                "text"
              (= k :password)
                "password"
              (= k :pin)
                "hidden"
              :else
                (throw (js/Error "Error in field type")))
     :value (-> @state (get comp-key) :text)
     :on-select #(clear-field-if-not-changed! comp-key)
     :on-change
       (fn [this]
         (field-changed! comp-key)
         (swap! state assoc-in [comp-key :text]
           (-> this this-val encrypt)))
     :on-click #(clear-field-if-not-changed! comp-key)
     :style    (merge
                 (when-not (-> @state (get comp-key) :changed?)
                   {:color   (style/color :light-gray)})
                 (when (-> @state (get comp-key) :hidden?)
                   {:display :none}))}))


(defrecord ByteEntity [name type data])

(defn drop-valid? [e]
  (some #{"Files"} (array-seq e.dataTransfer.types)))

(defn uploader [{:keys [on-drop] :as opts}]
  [:div.uploader
    ; https://groups.google.com/forum/#!topic/clojurescript/RTIPDlNFScI
    (merger opts
      {:draggable true ; -> otherwise the browser won't let you drag it
       :on-drag-over  #(.preventDefault %) ;; because DnD in HTMl5 is crazy...
       :on-drag-enter #(.preventDefault %) ;; because DnD in HTMl5 is crazy...
       :on-drag-start #(.setData (.-dataTransfer %) "text/plain" "") ;; for Firefox. You MUST set something as data.
       :on-drag-end   (fn [e] (.preventDefault e) (println "drag ended!"))
       :on-drop
         (fn [e]
           (.preventDefault e)
           (when (drop-valid? e)
             (log/pr :debug "successfully dropped!")
             (let [^FileList files
                     (array-seq
                       (or (-> e .-target       .-files)
                           (-> e .-dataTransfer .-files)))]
               ((or on-drop fn-nil) files))))})
    "UPLOADING"])

(defn field-changed! [^Key comp-key]
  (when-not (-> @state comp-key :changed?)
    (swap! state assoc-in [comp-key :changed?] true)))

(defn clear-field-if-not-changed!
  "Starts out as grayed out text, and on first click, is cleared."
  [^Key comp-key elem]
  (when-not (-> @state comp-key :changed?)
    (swap! state assoc-in [comp-key :text] "")
    (field-changed! comp-key)
    #_(.click elem)))

(def default-handler #(do % nil))

(defn place-cursor-at-end [elem] ; Input
  (.focus elem)
  (if (.-setSelectionRange elem) ; Doesn't work in IE
      (let [; Double the length because Opera is inconsistent about whether a carriage return is one character or two
            ct (-> elem (.val) count (* 2))]
        (.setSelectionRange elem ct ct))
      ; ... otherwise replace the contents with itself (Doesn't work in Google Chrome)
      (set! (.-value elem) (.-value elem)))

  ; Scroll to the bottom, in case we're in a tall textarea
  ; (Necessary for Firefox and Google Chrome)
  (set! (.-scrollTop elem) js/MAX_SAFE_INTEGER))

(defn field-template
  [^Key k {:keys [initial-style changed-style
                  on-change on-select
                  on-click]}]
  (log/pr :debug "Initial style for" k "is" initial-style)
  (let [^Key comp-key (str/keyword+ k "-field")
        type-n (condp = k
                 :username
                   "text"
                 :password
                   "password"
                 :pin
                   "hidden"
                 "text")
        on-change (or on-change default-handler)
        on-select (or on-select default-handler)
        on-click  (or on-click  default-handler)
        default-initial-color (css/color :light-gray)
        default-changed-color :black
        changed? (cursor state [comp-key :changed?])
        hidden?  (cursor state [comp-key :hidden? ])
        default-style
          {:font-size   20
           :font-family (style/font  :std)
           :border :none
           :padding 6
           :margin
             {:left   15
              :right  15
              :bottom 10}}]
    [:input
      {:type      type-n :selectionstart 0 :selectionend 0
       :value     (cursor state [comp-key :text])
       :on-select (fn [e] (clear-field-if-not-changed! comp-key (.-target e)) (.focus (.-target e))
                   (.select (.-target e)) (println "Seclected" (.-target e)) (on-select e))
       :on-change
         (fn [e]
           (field-changed! comp-key)
           (swap! state assoc-in [comp-key :text]
             (-> e this-val encrypt)) (println "Changed" (.-target e))
           (on-change e))
       :on-click (fn [e]
                    (.preventDefault e)
                    (.-focus (.-target e))
                   (clear-field-if-not-changed! comp-key (.-target e))
                   (on-click e)
                   )
       :style
       (merge-keep-left
         (dissoc (or initial-style default-style) :color :display)
         {:color   (rx  (doto (if @changed?
                           (or (:color changed-style) default-changed-color)
                           (or (:color initial-style) default-initial-color))
                         (println "IS COLOR FOR FIELD")))
          :display (rx (if @hidden? :none :table ; this is a fit-text-to-width trick
                           ))})}]))

(defn uploader-type [data-cache]
  (uploader
    {:on-drop (fn [[file & files]]
                (println "DROPPED!")
                (go
                  (let [doc- (quantum.core.io.ByteEntity. (.-name file) (f/mime-type file) file)
                        reader (file-reader
                                 {:on-load
                                   (fn [e]
                                     (->> e .-target .-result
                                          (reset! data-cache)))})
                        _ (.readAsText reader (:data doc-))])))}))

(defn smooth-pop-out-box []
  (let [over? (rx/atom false)]
    (fn []
      [:div {:class (if @over? "smooth-pop-up active" "smooth-pop-up")
             :style {:width 100 :height 200
                     :background :white
                     :margin-bottom 20
                     :color    :transparent
                     :position :absolute
                     :z-index 5}
             :on-mouse-over (fn [e] (reset! over? true ))
             :on-mouse-out  (fn [e] (reset! over? false))}
        "A"])))



(defn cell-component [{:keys [selected-row-i row-index font-size cell-width clamp-lines col-key data]}]
  ; If you make this an actual component (Fn []) it doesn't preserve order, and duplicates too
  [:div.hbox.vcenter
    [:div.hbox.vcenter {:className (cond (even? row-index)
                            (if (= @selected-row-i row-index) "cell hover" "cell even")
                            :else
                            (if (= @selected-row-i row-index) "cell hover" "cell"     ))
           :style {:font-size     font-size
                   :width         (- cell-width font-size)
                   :height        (* font-size 1 clamp-lines)

                   :justify-content :flex-start}}
                      [:div {:style {:white-space   :nowrap
                                     :overflow      :hidden
                                     :text-overflow :ellipsis}} (str (get-in @data [row-index col-key]))]]])

(defn touchable-area
  "A touchable area for (Facebook) ReactFixedDataTable."
  {:adapted-from "https://github.com/facebook/fixed-data-table/blob/master/site/examples/TouchableArea.js"}
  [{:keys [scroller] :as props} & children]
  (let [props (coll/merger {:touchable? true} props)
        handle-touch-start
          (fn [e]
            (println "TOUCH START")
            (when (and (atom? scroller) (:touchable? props))
              (println "SCROLLING IN TOUCH START")
              (-> scroller deref (.doTouchStart (.-touches e) (.-timeStamp e)))
              (.preventDefault e)))
        handle-touch-move
          (fn [e]
            (println "TOUCH MOVE")
            (when (and (atom? scroller) (:touchable? props))
              (println "SCROLLING IN TOUCH MOVE")
              (-> scroller deref (.doTouchMove (.-touches e) (.-timeStamp e) (.-scale e)))
              (.preventDefault e)))
        handle-touch-end
          (fn [e]
            (println "TOUCH END")
            (when (and (atom? scroller) (:touchable? props))
              (println "SCROLLING IN TOUCH END")
              #_(-> scroller deref (.doTouchEnd (.-timeStamp e)))
              ; Without this the scroller was reset to top:0 left: 0 on touchEnd.
              (.preventDefault e)))]
    (fn []
      (into
        [:div.touchable-area
          {:onTouchStart  handle-touch-start
           :onTouchMove   handle-touch-move
           :onTouchEnd    handle-touch-end
           :onTouchCancel handle-touch-end}]
        children))))

(defn dotoscrolltop [top]
  (println "DOING +10 to SCROLLTOP" top)
  (+ 10 top))

(def scroll-top*  (rx/atom 0))
(def scroll-left* (rx/atom 0))

(defn touch-wrapper
  [[child-tag {:keys [table-width table-height] :as child-props} & child-elems] ]
  (assert (atom? table-width ))
  (assert (atom? table-height))
  (let [scroller (rx/atom nil)
        left     (rx/atom 0)
        top      (rx/atom 0)
        props {:left          left
               :top           top
               :contentHeight 0
               :contentWidth  0
               :scroller      scroller}
        on-content-height-change
          (fn [content-height]
            (println "CONTENT HEIGHT CHANGE"
               @table-width
               @table-height
               (js/Math.max 600 @table-width)
               content-height)
            (.setDimensions @scroller
              @table-width
              @table-height
              (js/Math.max 600 @table-width) ; TODO 600 is hard-coded in
              content-height))
        handle-scroll
          (fn [left-n top-n]
            ; Don't allow to scroll sub 0
            (swap! left (fn [x] (if (> x 0) x 0)))
            (swap! top  (fn [x] (if (> x 0) x 0))))
        scroll-left* scroll-left*
        scroll-top*  scroll-top*]
    (reset! scroller ; Because only on mount, not on render
      (new js/ZyngaScroller.Scroller
        (fn [left-n top-n]
        ; Don't allow to scroll sub 0
        (println "HANDLE SCROLL" @left left-n "|" @top top-n)
        (swap! left (fn [x] (if (> x 0) x 0)))
        (swap! top  (fn [x] (@#'dotoscrolltop x) #_(if (> x 0) x 0))))))
    (with-meta
      (fn []
        (if (not @tu/touch-device?)
            (into [child-tag (assoc child-props
                               :height @table-height
                               :width  @table-width
                               :scrollLeft @scroll-left*
                               :scrollTop @scroll-top*)]
              child-elems)
            (do (println "RE-RENDERING TOUCHABLE AREA" "SCROLL LEFT" @left "SCROLL TOP" @top "HEIGHT" @table-height "WIDTH" @table-width)
                [touchable-area {:scroller scroller}
                  (into [child-tag (assoc child-props
                                     :onContentHeightChange on-content-height-change
                                     :scrollLeft @left
                                     :scrollTop  @top
                                     :height     @table-height
                                     :width      @table-width
                                     :overflowX  "hidden"
                                     :overflowY  "hidden")]
                    child-elems)])))
      {:componentWillMount (fn [] (println "<<<<---- DOING COMPONENT WILL MOUNT <<<<----") #_(reset! scroller ))})))

(defn fb-table-example
  [{:keys [data headers headers-widths
           style width height cell-props-fn fixed-header-key]}]
  (assert (nnil? width ))
  (assert (nnil? height))
  (let [std-col-width (/ @width (count @headers))
        col-indices   (reaction (->> @headers ; TODO code pattern
                                     (map-indexed (fn [i x] [x i]))
                                     (into {})))
        col-widths    (rx/atom (->> @headers
                                    (map+ #(first %1))
                                    (map+ (juxt identity (fn [col-key]
                                                           (or (get headers-widths col-key) std-col-width))))
                                    (into {})))
        selected-row-i (rx/atom nil)]
    (fn []
      (let [font-size 18
            clamp-lines 2
            props {;:style                     {:-webkit-overflow-scrolling :touch :overflow :scroll}
                   :row-height                (+ (* font-size 1 clamp-lines) 20)
                   :rows-count                (count @data)
                   :onScrollStart (fn [& args] (println "SCROLL START" args))
                   :onScrollEnd   (fn [& args] (println "SCROLL END" args))
                   :table-width   width
                   :table-height  height
                   :header-height             (* font-size 1 clamp-lines)
                   :onColumnResizeEndCallback (fn [new-col-width col-key-str]
                                                (swap! col-widths assoc (keyword col-key-str) new-col-width))
                   :isColumnResizing          false}]
        (println "CREATING TOUCH WRAPPER")
        [touch-wrapper
          (into
            [fb-table props]
            (for [[col-key col-label] @headers]
              [fb-column
                {:header (rx/as-element
                           [fb-cell [:div {:style {:text-align    :center
                                                   :font-size     font-size
                                                   :font-weight   :normal
                                                   :font-family   "Gotham Rounded Medium"
                                                   :width         (- (get @col-widths col-key) font-size)
                                                   :white-space   :nowrap
                                                   :overflow      :hidden
                                                   :text-overflow :ellipsis}}
                                      col-label]])
                 :isResizable true
                 :columnKey   (name col-key)
                 :fixed       (when (= col-key @fixed-header-key) true)
                 :cell   (fn [props]
                           (let [col-key     (keyword (.-columnKey props))
                                 cell-height (.-height   props)
                                 cell-width  (get @col-widths col-key) ; (.-width    props)
                                 row-index   (.-rowIndex props)
                                 on-mouse-over #(reset! selected-row-i row-index)]
                             (rx/as-element
                               [fb-cell (merge (kmap on-mouse-over)
                                          (when cell-props-fn
                                            (cell-props-fn col-key row-index)))
                                 [cell-component (kmap selected-row-i row-index font-size cell-width clamp-lines col-key data)]])))
                 :width  (get @col-widths col-key)
                 ;:min-width (min std-col-width (js/Math.abs (- std-col-width 10)))
                 }]))]))))

(defn virtual-scroller [{:keys [width height row-height rows-count style row-fn data overscan-rows-count]}]
  (fn []
    (let [width* @width
          row-height* @row-height]
      [:div {:style style}
        [virtual-scroll
          {:height              @height
           :width               @width
           :overscan-rows-count (or overscan-rows-count 10)
           :rows-count          @rows-count
           :row-height          row-height*
           :no-rows-renderer
             (fn [] (rx/as-element [:div]))
           :row-renderer (fn [row-index]
                           (rx/as-element [:div {:style {:height @row-height :overflow-y :hidden}}
                                            (row-fn row-index width* row-height* data)]))}]])))

(defn canvas-scroller
  [{{:as style :keys [width height]} :style
    :keys [data row-height num-items-getter row-height-getter
           scrolling-deceleration scrolling-acceleration
           row-render-fn]
    :or {row-height-getter      (fn [] row-height)
         scrolling-deceleration 0.97
         scrolling-acceleration 0.13}}]
  (assert (atom? data  ))
  (assert (nnil? width ))
  (assert (nnil? height))
  (assert (fn? row-render-fn))
  (let []
    (fn []
      [canvas-surface {:top    0
                       :left   0
                       :width  width
                       :height height}
        [canvas-list-view
          {:style (mergel style
                    {:top  0
                     :left 0})
           :numberOfItemsGetter              (or num-items-getter (fn [] (count @data)))
           :itemHeightGetter                 row-height-getter
           :scrollingDeceleration            scrolling-deceleration ; closer to 1 = closer to zero friction
           :scrollingPenetrationAcceleration scrolling-acceleration
           ; Render the item at the given index
           :itemGetter          (fn [row scroll-top]
                                  (rx/as-element
                                    (row-render-fn row scroll-top width (row-height-getter))))}]])))

; TODO use TableView https://github.com/bvaughn/react-virtualized/blob/master/source/Grid/Grid.example.js
(defn test-flex-table [headers data]
  (let [ref-name (name (gensym))]
    (fn []
      (let [row-height (* 15 1 3)
            width      (:width @state)]
        [:div {:style {:width width}}
          (rx/create-element (.-AutoSizer js/ReactVirtualized)
            #js{:width width :disableHeight true}
            (fn [props]
              (let [width  (.-width  props)
                    height (whenc (.-height props) (fn-or nil? zero?) 500)
                    _ (println "THIS IS WIDTH" width "HEIGHT" height props)]
                  (rx/as-element
                    (into
                      [flex-table {:ref               ref-name
                                   :width             width
                                   :height            height
                                   :headerHeight      row-height
                                   :rowHeight         row-height
                                   :sort              (fn [sort-by-key sort-direction] (println "Attempting to sort")) ; TODO not implemented
                                   :sortBy            (name :name)
                                   :noRowsRenderer    (fn [] (rx/as-element
                                                               [:div "No rows"]))
                                   :overscanRowsCount 15
                                   :sortDirection     (-> js/ReactVirtualized .-SortDirection .-ASC)
                                   :rowGetter         (fn [row] (get @data row))
                                   :rowsCount         (count @data)}]
                      (for [[header-key header-label] @headers]
                        [flex-column {:dataKey (name header-key)
                                      :width   (/ width (count @headers))
                                      :label   header-label
                                      :flexGrow 1
                                      :cellDataGetter (fn [data-key row-data column-data]
                                                        (println "data-key" data-key "column-data" column-data "row-data-gotten" (get row-data (keyword data-key)))
                                                        (str (get row-data (keyword data-key))))}]))))))]))))


(defn ellipsis [{:keys [clamp-lines font-size line-height width]
                 :as style}
                content]
  (assert (nnil? clamp-lines))
  (assert (nnil? font-size  ))
  (assert (nnil? width      ))
  (fn []
    [:div.ellipsis {:style (merge (style/ellipsis clamp-lines font-size line-height)
                                  (dissoc style
                                    :clamp-lines
                                    :font-size
                                    :line-height))}
      content]))


(defn pdf-canvas-1-page []
  (set! js/PDFJS.workerSrc "./js/pdfjs.worker.min.js")
  (let [canvas-elem
        (with-meta
          (fn [] [:canvas])
          {:component-did-mount
            (fn [this]
              (let [canvas (rx/dom-node this)]
                (println "===========CANVAS" canvas)
                ; Asynchronous download PDF
                (-> (.getDocument js/PDFJS "./Purely Functional Data Structures.pdf")
                    (.then (fn [pdf]
                             ; Fetch the first page
                             (-> pdf (.getPage 1)
                                 (.then (fn [page]
                                          (let [scale 1.5
                                                viewport (.getViewport page scale)
                                                desired-width 200
                                                h-scale 1 ; (/ desired-width (.-width viewport))
                                                v-scale 1
                                                ; scale = desiredWidth / viewport.width;
                                                context  (.getContext canvas "2d")
                                                ; Prepare canvas using PDF page dimensions
                                                _ (set! (.-width  canvas) (* (.-width  viewport) h-scale))
                                                _ (set! (.-height canvas) (* (.-height viewport) v-scale))
                                                _ (.render page #js {:canvasContext context
                                                                     :viewport      viewport})])))))))))})]
    [canvas-elem]))
)
)