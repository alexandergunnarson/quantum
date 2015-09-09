(ns
  ^{:doc "CSS-specific functions.

          Mainly to style Hiccup-style components to use 'virtual'
          CSS instead of generating an external stylesheet."
    :attribution "Alex Gunnarson"}
  quantum.ui.css
  (:require-quantum [:lib])
  (:require
    [quantum.ui.form   :as form                  ]
    [garden.color      :as color     :refer [color? #?(:cljs CSSColor)]]
    [garden.core       :as css       :refer [css]]
    [garden.stylesheet :as css-style             ])
  #?(:clj (:import garden.color.CSSColor)))

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

(defn taker-until-incl
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

(defn derefable? [obj]
  (instance+? #?(:clj clojure.lang.IDeref :cljs core/IDeref) obj))

(defn merge-with-set [m1 m2]
  (merge-with (fn [v1 v2] (if (set? v1)
                              (if (set? v2)
                                  (set/union v1 v2)
                                  (conj v1 v2))
                              (if (set? v2)
                                  (conj v2 v1)
                                  #{v1 v2}))) m1 m2))

(defn unwrap-duplicates [m]
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

(defn ^String css-block-str
  [css-block]
  (->> (whenf css-block (fn-not vector?)
         (fn->> (vector :a)))
       (css {:pretty-print? false})
       (dropl 2)   ; drops "a{"
       (dropr 1))) ; drops "}"

(defn ^String css-prop-str
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

(def colors
  (atom
    (->> {:aquamarine "#7fffd4"
          :alice-blue "#f0f8ff"
          :antique-white "#faebd7"
          :aqua "#00ffff"
          :azure "#f0ffff"
          :beige "#f5f5dc"
          :bisque "#ffe4c4"
          :black "#000000"
          :blanched-almond "#ffebcd"
          :blue "#0000ff"
          :blue-violet "#8a2be2"
          :brown "#a52a2a"
          :burlywood "#deb887"
          :cadet-blue "#5f9ea0"
          :chartreuse "#7fff00"
          :chocolate "#d2691e"
          :coral "#ff7f50"
          :cornflower-blue "#6495ed"
          :corn-silk "#fff8dc"
          :crimson "#dc143c"
          :cyan "#00ffff"
          :dark-blue "#00008b"
          :dark-brown (color/rgb 84  56  71 )
          :dark-cyan "#008b8b"
          :dark-goldenrod "#b8860b"
          :dark-gray "#a9a9a9"
          :dark-green "#006400"
          :dark-khaki "#bdb76b"
          :dark-magenta "#8b008b"
          :dark-olivegreen "#556b2f"
          :dark-orange "#ff8c00"
          :dark-orchid "#9932cc"
          :dark-red "#8b0000"
          :dark-salmon "#e9967a"
          :dark-seagreen "#8fbc8f"
          :dark-slateblue "#483d8b"
          :dark-slategray "#2f4f4f"
          :dark-turquoise "#00ced1"
          :dark-violet "#9400d3"
          :deep-pink "#ff1493"
          :deep-sky-blue "#00bfff"
          :dim-gray "#696969"
          :dodger-blue "#1e90ff"
          :firebrick "#b22222"
          :floral-white "#fffaf0"
          :forest-green "#228b22"
          :fuchsia "#ff00ff"
          :gainsboro "#dcdcdc"
          :ghost-white "#f8f8ff"
          :gold "#ffd700"
          :goldenrod "#daa520"
          :gray "#808080"
          :green "#008000"
          :green-yellow "#adff2f"
          :honeydew "#f0fff0"
          :hot-pink "#ff69b4"
          :indian-red "#cd5c5c"
          :indigo "#4b0082"
          :ivory "#fffff0"
          :khaki "#f0e68c"
          :lavender "#e6e6fa"
          :lavender-blush "#fff0f5"
          :lawn-green "#7cfc00"
          :lemon-chiffon "#fffacd"
          :light-blue "#add8e6"
          :light-coral "#f08080"
          :light-cyan "#e0ffff"
          :light-goldenrod-yellow "#fafad2"
          :light-gray "#d3d3d3"
          :light-green "#90ee90"
          :light-pink "#ffb6c1"
          :light-salmon "#ffa07a"
          :light-sea-green "#20b2aa"
          :light-sky-blue "#87cefa"
          :light-slate-gray "#778899"
          :light-steel-blue "#b0c4de"
          :light-yellow "#ffffe0"
          :lime "#00ff00"
          :lime-green "#32cd32"
          :linen "#faf0e6"
          :magenta "#ff00ff"
          :maroon "#800000"
          :medium-aquamarine "#66cdaa"
          :medium-blue "#0000cd"
          :medium-orchid "#ba55d3"
          :medium-purple "#9370db"
          :medium-seagreen "#3cb371"
          :medium-slate-blue "#7b68ee"
          :medium-spring-green "#00fa9a"
          :medium-turquoise "#48d1cc"
          :medium-violet-red "#c71585"
          :midnight-blue "#191970"
          :mint-cream "#f5fffa"
          :misty-rose "#ffe4e1"
          :moccasin "#ffe4b5"
          :navajo-white "#ffdead"
          :navy "#000080"
          :old-lace "#fdf5e6"
          :olive "#808000"
          :olive-drab "#6b8e23"
          :orange "#ffa500"
          :orange-red "#ff4500"
          :orchid "#da70d6"
          :pale-goldenrod "#eee8aa"
          :pale-green "#98fb98"
          :pale-turquoise "#afeeee"
          :pale-violet-red "#db7093"
          :papaya-whip "#ffefd5"
          :peach-puff "#ffdab9"
          :peru "#cd853f"
          :pink "#ffc0cb"
          :plum "#dda0dd"
          :powder-blue "#b0e0e6"
          :purple "#800080"
          :red "#ff0000"
          :rosy-brown "#bc8f8f"
          :royal-blue "#4169e1"
          :saddle-brown "#8b4513"
          :salmon "#fa8072"
          :sandy-brown "#f4a460"
          :sea-green "#2e8b57"
          :seashell "#fff5ee"
          :sienna "#a0522d"
          :silver "#c0c0c0"
          :sky-blue "#87ceeb"
          :slate-blue "#6a5acd"
          :slate-gray "#708090"
          :snow "#fffafa"
          :spring-green "#00ff7f"
          :steel-blue "#4682b4"
          :tan "#d2b48c"
          :teal "#008080"
          :thistle "#d8bfd8"
          :tomato "#ff6347"
          :turquoise "#40e0d0"
          :violet "#ee82ee"
          :wheat "#f5deb3"
          :white "#ffffff"
          :white-smoke "#f5f5f5"
          :yellow "#ffff00"
          :yellow-green "#9acd32"}
        (coll/map-vals+ color/as-hsl)
        redm)))
 
(defn color [k] (get @colors k))


(def fonts
  {:std {:std      "Gotham Book"
         :semibold "Gotham Medium"
         :light    "Gotham Thin"}
   :google #{"Pathway Gothic One" "Raleway" "Open Sans Condensed" "Fjalla One"}
   :typeright #{}})


(defn font
  {:todo ["More arity"]}
  ([k]      (get-in fonts [k :std]))
  ([k & ks] (get-in fonts (apply vector k ks))))

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

#?(:cljs
(defn flex-test [elem flex-name]
  (-> elem .-style .-display (set! ""))
  (-> elem .-style .-display (set! flex-name))
  (-> elem .-style .-display (not= ""))))

(defn feature-test []
  #?(:clj {:chrome true}
     :cljs
      (let [div (.createElement js/document "div")]
        (->> {:chrome  "flex"
              :safari  "-webkit-flex"
              :safari- "-webkit-box"
              :ie      "-ms-flexbox"}
             (map+ (fn [browser s] (map-entry browser (flex-test div s))))
             redm))))

(defn determine-browser []
  (let [browser-possibilities
         (->> (feature-test)
              (filter (fn [[k v]] (true? v)))
              (map key)
              (into #{}))]
    (or (:chrome browser-possibilities)
        (:safari browser-possibilities)
        (first browser-possibilities))))

(def browser (atom (determine-browser)))

(def compatibility-chart
  {:display        {"flex"
                     {:safari  [[:display                "-webkit-flex"           ]]
                      :safari- [[:display                "-webkit-box"            ]]
                      :ie      [[:display                "-ms-flexbox"            ]]}}
   :background     {"linear-gradient"
                     {:safari  [[:background             "-webkit-linear-gradient"]]}}
   :flex-direction {"row"
                     {:safari- [[:-webkit-box-orient     "horizontal"             ]
                                [:-webkit-box-direction  "normal"                 ]]
                      :safari  [[:-webkit-flex-direction "row"                    ]]
                      :ie      [[:-ms-flex-direction     "row"                    ]]}
                    "column"
                     {:safari- [[:-webkit-box-orient     "vertical"               ]
                                [:-webkit-box-direction  "normal"                 ]]
                      :safari  [[:-webkit-flex-direction "column"                 ]]
                      :ie      [[:-ms-flex-direction     "column"                 ]]}}
   :flex-wrap      {"wrap"
                     {:safari  [[:-webkit-flex-wrap      "wrap"                   ]]
                      :ie      [[:-ms-flex-wrap          "wrap"                   ]]}}
   :flex-flow      {"column"
                     {:safari  [[:-webkit-flex-flow      "column"                 ]]
                      :ie      [[:-ms-flex-flow          "column"                 ]]}}
   ; H-ALIGN
   :align-items    {"stretch"
                     {:safari- [[:-webkit-box-align      "stretch"                ]] 
                      :safari  [[:-webkit-align-items    "stretch"                ]]
                      :ie      [[:-ms-flex-align         "stretch"                ]]}}
   ; V-ALIGN
   :align-content  {"center"
                     {:safari  [[:-webkit-align-content  "center"                 ]]
                      :ie      [[:-ms-flex-line-pack     "stretch"                ]]}}})

(defn css-color? [obj] (instance? CSSColor obj))

(defn ->rgb [c]
  (-> c color/as-rgb (assoc :alpha (:alpha c))))

(defn render-color [c]
  (if (:alpha c)
      (let [{:keys [red green blue alpha]} (->rgb c)]
        (str "rgba(" (str/join "," [red green blue (or alpha 1)]) ")"))
      (color/as-hex c)))

(defnt normalize-prop-v
  ([^keyword? v k] (name v))
  ([^number?  v k] (if (contains? px-props k) (px v) (str v)))
  ([^vector?  v k] (postwalk (f*n normalize-prop-v k) v))
  ([:else     v k] (whenf v css-color? render-color)))

(defn ^Vec compatibilize [style-map]
  (->> style-map
       (map (fn [[k v]]
              (get-in compatibility-chart [k v @browser])))
       (apply concat)
       redm
       (merge-with (fn [orig compatible] compatible) style-map)))

(defn ^Vec flattened-style
  "Takes a nested CSS-style map and flattens it
   into a style processable by js.React, reagent, or freactive.

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
          :background-position   "left"}}
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

(def rstyle flattened-style)

; ======== CSS GENERATION FROM CLASSES ========

(defn full-css-for-tag
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

(defn fill-in-css-from-tag!
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

(defn to-css-str [css-map]
  (->> css-map
       (map+ (juxt key (compr val flattened-style)))
       (map+ (fn [[k v]] (garden.core/css {:pretty-print? false} [k v])))
       redv
       (str/join "\n")))

#?(:clj
(defn export-css! [style-map]
  (->> style-map to-css-str
       (io/write!
        :path [:resources "public" "css" "styles.css"]
        :write-method :print :data))))
