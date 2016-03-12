(ns
  ^{:doc "CSS-specific functions.

          Mainly to style Hiccup-style components to use 'virtual'
          CSS instead of generating an external stylesheet."
    :attribution "Alex Gunnarson"}
  quantum.ui.style.css.core
  (:require-quantum [:core map log err logic fn tpred #_:lib])
  (:require
    [garden.color         :as color     :refer [color? #?(:cljs CSSColor)]]
    [garden.core          :as css       :refer [css]]
    [garden.stylesheet    :as css-style :refer [at-keyframes at-font-face]])
  #?(:clj (:import garden.color.CSSColor)))

; TODO generate this in a more sane way 
(def listener-map
  {"load"       "load"
   "mouse-over" "mouseover"
   "mouseover"  "mouseover"
   "mouse-out"  "mouseout"
   "mouseout"   "mouseout"
   "mouse-down" "mousedown"
   "mousedown"  "mousedown"
   "mouse-up"   "mouseup"
   "mouseup"    "mouseup"
   "click"      "click"
   "change"     "change"
   "drag-over"  "dragover"
   "dragover"   "dragover"
   "drag-enter" "dragenter"
   "dragenter"  "dragenter"
   "drag-start" "dragstart"
   "dragstart"  "dragstart"
   "drag-end"   "dragend"
   "dragend"    "dragend"
   "drop"       "drop"})

(def dynamic-styles? (atom false))

; TODO move
#_(defn taker-until-incl
  "Take until index of, starting at the right."
  {:in  ["c" "abcdefg"]
   :out "cdefg"}
  ([sub super]
    (taker-until-incl sub super super))
  ([sub alt super]
    (let [i (last-index-of super sub)]
      (if i
          (coll/taker-untili (dec i) super)
          alt))))



#_(defn unwrap-duplicates [m]
  (->> m
       (map+ (fn [k vs]
               (if (set? vs)
                   (->> vs (map (partial map-entry k)))
                   vs)))
       quantum.core.reducers/flatten-1+
       redv))

; ======== HELPER FUNCTIONS ========

; TODO Darkening and lightening functions

; 100
; 200
; 300
; 400 400 is the same as normal, 
; 500
; 600
; 700 700 is the same as bold
; 800
; 900 

(defn ^String px [n] (str n "px"))

(defn ^String rgb
  {:todo "simplify with str package"
   :in   [244 176 36]
   :out  "rgb(244,176,36)"}
  [r g b]
  (str "rgb" "(" r "," g "," b ")"))

(defn ^String url
  [s]
  (str "url(" s ")"))

(defn ^String hex [s] (str "#" s))

#_(defn ^String css-block-str
  [css-block]
  (->> (whenf css-block (fn-not vector?)
         (fn->> (vector :a)))
       (css {:pretty-print? false})
       (dropl 2)   ; drops "a{"
       (dropr 1))) ; drops "}"

#_(defn ^String css-prop-str
  {:in  "{:width 2 :font-size [[\"16px\" \"1px\"] [\"17px\"]]}"
   :out "width:2;font-size:16px 1px,17px" }
  [css-block]
  (if (or (derefable? css-block)
          (and (string? css-block)
               (str/starts-with? css-block "calc")))
      css-block
      (->> (whenf css-block (fn-not map?)
             (fn->> (array-map :a) (vector :a)))
           (css {:pretty-print? false})
           (dropl 2)   ; drops "a{"g
           (dropr 1)   ; drops "}"
           (dropl 2)))) ; drops "a:"  

(def styles          (atom {}))
(def class-watch?    (atom false))
(def class-watchlist (atom {}))
(def themes          (atom {}))
(def styles-template (atom {})) ; This needs to be initialized 

(defn theme
  {:todo ["More arity"]}
  ([k sub-k]      (get-in @themes [k sub-k]))
  ([k sub-k & ks] (get-in @themes (apply vector k sub-k ks))))

(def px-props
  #{:height :width
    :margin
      :margin-top   :margin-bottom
      :margin-left  :margin-right
    :font-size
    :padding
      :padding-top  :padding-bottom
      :padding-left :padding-right
    :border
      :border-top  :border-bottom
      :border-left :border-right})


(def ANY #?(:clj (Object.) :cljs #js {}))
(def compatibility-chart
  {:background     {:linear-gradient
                     {:safari  [[:background             "-webkit-linear-gradient"]]}}
   :transition     {ANY
                     {:chrome  [[:-webkit-transition]]
                      :safari  [[:-webkit-transition]]}}
   :transition-duration
                   {ANY
                     {:chrome  [[:-webkit-transition-duration]]
                      :safari  [[:-webkit-transition-duration]]}}
   :appearance     {ANY
                     {:safari  [[:-webkit-appearance]]
                      :ie      [[:-ms-appearance    ]]
                      :opera   [[:-o-appearance     ]]}}
   :font-smoothing {ANY
                     {:safari  [[:-webkit-font-smoothing]]
                      :ie      [[:-ms-font-smoothing    ]]
                      :opera   [[:-o-font-smoothing     ]]
                      :firefox [[:-moz-font-smoothing   ]]}}
   :text-rendering {ANY
                     {:safari  [[:-webkit-text-rendering]]
                      :ie      [[:-ms-text-rendering    ]]
                      :opera   [[:-o-text-rendering     ]]
                      :firefox [[:-moz-text-rendering   ]]}}
   :box-sizing     {ANY
                     {:ie      [[:-ms-box-sizing ]]
                      :opera   [[:-o-box-sizing  ]]
                      :firefox [[:-moz-box-sizing]]}}
   :display        {:flex
                     {:ie      [[:display :-ms-flexbox ]]
                      :opera   [[:display :-webkit-box ]]
                      :firefox [[:display :-moz-box    ]]
                      :safari- [[:display :-webkit-flex]]
                      :safari  [[:display :-webkit-box ]]}}
   :flex-direction {:row
                     {:safari  [[:-webkit-flex-direction "row"                    ]
                                [:-webkit-box-orient     "horizontal"             ]
                                [:-webkit-box-direction  "normal"                 ]]
                      :ie      [[:-ms-flex-direction     "row"                    ]]}
                    :column
                     {:safari  [[:-webkit-flex-direction "column"                 ]
                                [:-webkit-box-orient     "vertical"               ]
                                [:-webkit-box-direction  "normal"                 ]]
                      :ie      [[:-ms-flex-direction     "column"                 ]]}}
   :flex-wrap      {:wrap
                     {:safari  [[:-webkit-flex-wrap      "wrap"                   ]]
                      :ie      [[:-ms-flex-wrap          "wrap"                   ]]}}
   :flex-flow      {:column
                     {:safari  [[:-webkit-flex-flow      "column"                 ]]
                      :ie      [[:-ms-flex-flow          "column"                 ]]}}
   ; H-ALIGN
   :align-items    {:stretch
                     {:safari  [[:-webkit-align-items    "stretch"                ]
                                [:-webkit-box-align      "stretch"                ]]
                      :ie      [[:-ms-flex-align         "stretch"                ]]}}
   ; V-ALIGN
   :align-content  {:center
                     {:safari  [[:-webkit-align-content  "center"                 ]]
                      :ie      [[:-ms-flex-line-pack     "stretch"                ]]}}})

#_(defnt normalize-prop-v
  ([^keyword? v k] (name v))
  ([^number?  v k] (if (contains? px-props k) (px v) (str v)))
  ([^vector?  v k] (postwalk (f*n normalize-prop-v k) v))
  ([:else     v k] (whenf v css-color? render-color)))

#_(defn compatibilize
  {:out-type "Vector"}
  [style-map]
  (->> style-map
       (map (fn [[k v]]
              (get-in compatibility-chart [k v @browser])))
       (apply concat)
       redm
       (merge-with (fn [orig compatible] compatible) style-map)))

#_(defn flattened-style
  "Takes a nested CSS-style map and flattens it
   into a style processable by js.React, reagent, etc.

   Vector in order to allow for multiple of the same keys
   (for compatibility)."
  {:todo ["A rather large function which should be broken up
           into more modular pieces."]
   :attribution "Alex Gunnarson"
   :in  '{:width     2
          :font-size [["16px" "1px"] ["17px"]]
          :background
            {:color      :transparent  ; background-color
             :image      (url "../imgs/flappy-base.png")
             :repeat     :no-repeat
             :attachment :top
             :position   :left}}
   :out '{:width                 "2"
          :font-size             "16px 1px,17px"
          :background-color      "transparent"
          :background-image      "url(\"../imgs/flappy-base.png\")"
          :background-repeat     "no-repeat"
          :background-attachment "top"
          :background-position   "left"}
   :out-type "Vector"}
  [^Map css-block]
  (->> css-block 
       (reduce
         (fn [^Map ret ^Key prop-k prop-v]
           (let [^Map normalized-props
                  (condf prop-v
                    css-color?
                      (fn->> render-color (map-entry prop-k))
                    map?
                      (fn->> (map+
                               (fn [sub-prop-k sub-prop-v]
                                 (let [sub-prop-k-f
                                        (str/keyword+ prop-k "-" sub-prop-k)]
                                   (println "SUB " (-> sub-prop-v
                                         (normalize-prop-v sub-prop-k-f)))
                                   (map-entry
                                     sub-prop-k-f
                                     (-> sub-prop-v
                                         (normalize-prop-v sub-prop-k-f)
                                         css-prop-str)))))
                             redm)
                    :else
                      (fn->> (<- normalize-prop-v prop-k)
                             css-prop-str
                             (map-entry prop-k)))]
             (merge ret normalized-props)))
         {})
       compatibilize))

#_(def rstyle flattened-style)

; ======== CSS GENERATION FROM CLASSES ========

#_(defn full-css-for-tag
  "  0,0 | 0       div
   < 1,1 | 1       .pillar
   < 0,1 | 0,1     div.pillar
   < 2,2 | 2       .upper
   < 1,2 | 1,2     .pillar.upper
   < 0,2 | 0,1,2   div.pillar.upper
   < 3,3 | 3       .left
   < 2,3 | 2,3     .upper.left
   < 1,3 | 1,2,3   .pillar.upper.left
   < 0,3 | 0,1,2,3 div.pillar.upper.left"
  {:todo ["A rather large function which should be broken up
           into more modular pieces."]
   :attribution "Alex Gunnarson"}
  [^Atom styles-calc ^Keyword tag]
  (let [^Map style-assembled
         (let [^Vec tags (-> tag name (str/split #"\."))]
               ; Starts with the most specific and works upward
               (loop [^Map class-props-n {}
                      tagl-n 0
                      tagr-n 0] 
                 (let [^Vec tags-n (subseq tags tagl-n tagr-n) ; [div pillar upper] ; or maybe :#abc
                       joined (str/join "." tags-n)
                       ^Key class-n (keyword (if (= tagl-n 0) joined (str "." joined)))
                       class-calculated? (contains? @styles-calc class-n)
                       class-props-n+1
                         (if class-calculated?
                             (get @styles-calc class-n)
                             (mergel
                               (get @styles-template class-n) ; gives precedence to subclasses
                               class-props-n))]
                   (when (and (not class-calculated?) (= tagl-n 0))
                     (assoc! styles-calc class-n class-props-n+1))
                   ;(println "tagl-n tagr-n class-n class-props-n+1" tagl-n tagr-n class-n class-props-n+1)
                   (if (= tagl-n 0)
                       (if (= tagr-n (lasti tags))
                           class-props-n
                           (recur class-props-n+1 (inc tagr-n) (inc tagr-n)))
                       (recur class-props-n+1 (dec tagl-n) tagr-n)))))
        style-f (flattened-style style-assembled)
        _ (log/pr :debug "CALCULATED STYLE FOR" tag ":" style-f)]
    style-f))

#_(defn fill-in-css-from-tag!
  [tag]
  (when-not (contains? @styles tag) ; If it didn't already calculate the styles
    (assoc! styles tag (full-css-for-tag styles tag))))

#_(defn add-css-media-style!
  "DOM manipulation.
   Adds @media style to <styles> tag in HTML.

   Likely not the best way to go about this."
  {:attribution "Alex Gunnarson"}
  []
  (->> @styles-template
       (<- get :+media)
       (apply css-style/at-media)
       css-block-str
       gstyle/installStyles))

#_(defn to-css-str [css-map]
  (->> css-map
       (map+ (juxt key (compr val flattened-style)))
       (map+ (fn [[k v]] (garden.core/css {:pretty-print? false} [k v])))
       redv
       (str/join "\n")))

; #?(:clj
; (defn export-css! [style-map]
;   (->> style-map to-css-str
;        (io/write!
;         :path [:resources "public" "css" "styles.css"]
;         :write-method :print :data))))


(defn trans
  "Defines a transition."
  [id & props]
  [(at-keyframes (-> id name (str "-in"))
     [:from {:-webkit-transition props
             :transition         props}]
     [:to   {:-webkit-transition props
             :transition         props}])
   (at-keyframes (-> id name (str "-out"))
     [:from {:-webkit-transition props
             :transition         props}]
     [:to   {:-webkit-transition props
             :transition         props}])])

(defn hpadding [v] {:padding-left v :padding-right  v})
(defn vpadding [v] {:padding-top  v :padding-bottom v})
(defn hmargin  [v] {:margin-left  v :margin-right   v})
(defn vmargin  [v] {:margin-top   v :margin-bottom  v})