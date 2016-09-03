(ns quantum.ui.style.color
           (:require [clojure.string    :as str]
                     [garden.color      :as color 
                       :refer [color? #?(:cljs CSSColor)]]
                     [quantum.core.vars :as var 
                       :refer [#?(:clj defalias)]        ])
  #?(:cljs (:require-macros
                     [quantum.core.vars :as var 
                       :refer [defalias]                 ]))
  #?(:clj  (:import  garden.color.CSSColor)))

;(defnt ->color*
;  ([^garden.color.CSSColor c]
;    (Color/hsb
;      (-> c :hue (/ 100))
;      (-> c :saturation (/ 100))
;      (-> c :lightness (/ 100)))))

; (def ->color
;   (memoize (fn [c] (->color* c))))

(def colors
  (atom
    (->> {:aquamarine             "#7fffd4"
          :alice-blue             "#f0f8ff"
          :antique-white          "#faebd7"
          :aqua                   "#00ffff"
          :azure                  "#f0ffff"
          :beige                  "#f5f5dc"
          :bisque                 "#ffe4c4"
          :black                  "#000000"
          :blanched-almond        "#ffebcd"
          :blue                   "#0000ff"
          :blue-violet            "#8a2be2"
          :brown                  "#a52a2a"
          :burlywood              "#deb887"
          :cadet-blue             "#5f9ea0"
          :chartreuse             "#7fff00"
          :chocolate              "#d2691e"
          :coral                  "#ff7f50"
          :cornflower-blue        "#6495ed"
          :corn-silk              "#fff8dc"
          :crimson                "#dc143c"
          :cyan                   "#00ffff"
          :dark-blue              "#00008b"
          :dark-brown             (color/rgb 84 56 71)
          :dark-cyan              "#008b8b"
          :dark-goldenrod         "#b8860b"
          :dark-gray              "#a9a9a9"
          :dark-green             "#006400"
          :dark-khaki             "#bdb76b"
          :dark-magenta           "#8b008b"
          :dark-olivegreen        "#556b2f"
          :dark-orange            "#ff8c00"
          :dark-orchid            "#9932cc"
          :dark-red               "#8b0000"
          :dark-salmon            "#e9967a"
          :dark-seagreen          "#8fbc8f"
          :dark-slateblue         "#483d8b"
          :dark-slategray         "#2f4f4f"
          :dark-turquoise         "#00ced1"
          :dark-violet            "#9400d3"
          :deep-pink              "#ff1493"
          :deep-sky-blue          "#00bfff"
          :dim-gray               "#696969"
          :dodger-blue            "#1e90ff"
          :firebrick              "#b22222"
          :floral-white           "#fffaf0"
          :forest-green           "#228b22"
          :fuchsia                "#ff00ff"
          :gainsboro              "#dcdcdc"
          :ghost-white            "#f8f8ff"
          :gold                   "#ffd700"
          :goldenrod              "#daa520"
          :gray                   "#808080"
          :green                  "#008000"
          :green-yellow           "#adff2f"
          :honeydew               "#f0fff0"
          :hot-pink               "#ff69b4"
          :indian-red             "#cd5c5c"
          :indigo                 "#4b0082"
          :ivory                  "#fffff0"
          :khaki                  "#f0e68c"
          :lavender               "#e6e6fa"
          :lavender-blush         "#fff0f5"
          :lawn-green             "#7cfc00"
          :lemon-chiffon          "#fffacd"
          :light-blue             "#add8e6"
          :light-coral            "#f08080"
          :light-cyan             "#e0ffff"
          :light-goldenrod-yellow "#fafad2"
          :light-gray             "#d3d3d3"
          :light-green            "#90ee90"
          :light-pink             "#ffb6c1"
          :light-salmon           "#ffa07a"
          :light-sea-green        "#20b2aa"
          :light-sky-blue         "#87cefa"
          :light-slate-gray       "#778899"
          :light-steel-blue       "#b0c4de"
          :light-yellow           "#ffffe0"
          :lime                   "#00ff00"
          :lime-green             "#32cd32"
          :linen                  "#faf0e6"
          :magenta                "#ff00ff"
          :maroon                 "#800000"
          :medium-aquamarine      "#66cdaa"
          :medium-blue            "#0000cd"
          :medium-orchid          "#ba55d3"
          :medium-purple          "#9370db"
          :medium-seagreen        "#3cb371"
          :medium-slate-blue      "#7b68ee"
          :medium-spring-green    "#00fa9a"
          :medium-turquoise       "#48d1cc"
          :medium-violet-red      "#c71585"
          :midnight-blue          "#191970"
          ;:mint-cream             "#f5fffa"
          :misty-rose             "#ffe4e1"
          ;:moccasin               "#ffe4b5"
          :navajo-white           "#ffdead"
          :navy                   "#000080"
          :old-lace               "#fdf5e6"
          :olive                  "#808000"
          :olive-drab             "#6b8e23"
          :orange                 "#ffa500"
          :orange-red             "#ff4500"
          :orchid                 "#da70d6"
          :pale-goldenrod         "#eee8aa"
          :pale-green             "#98fb98"
          :pale-turquoise         "#afeeee"
          :pale-violet-red        "#db7093"
          :papaya-whip            "#ffefd5"
          :peach-puff             "#ffdab9"
          :peru                   "#cd853f"
          :pink                   "#ffc0cb"
          :plum                   "#dda0dd"
          :powder-blue            "#b0e0e6"
          :purple                 "#800080"
          :red                    "#ff0000"
          :rosy-brown             "#bc8f8f"
          :royal-blue             "#4169e1"
          :saddle-brown           "#8b4513"
          :salmon                 "#fa8072"
          :sandy-brown            "#f4a460"
          :sea-green              "#2e8b57"
          :seashell               "#fff5ee"
          :sienna                 "#a0522d"
          :silver                 "#c0c0c0"
          :sky-blue               "#87ceeb"
          :slate-blue             "#6a5acd"
          :slate-gray             "#708090"
          :snow                   "#fffafa"
          :spring-green           "#00ff7f"
          :steel-blue             "#4682b4"
          :tan                    "#d2b48c"
          :teal                   "#008080"
          :thistle                "#d8bfd8"
          :tomato                 "#ff6347"
          :turquoise              "#40e0d0"
          :violet                 "#ee82ee"
          :wheat                  "#f5deb3"
          :white                  "#ffffff"
          :white-smoke            "#f5f5f5"
          :yellow                 "#ffff00"
          :yellow-green           "#9acd32"}
        (map (fn [[k v]] [k (color/as-rgb v)]))
        (into {}))))
 
(defn color [k] (get @colors k))

(defn css-color? [obj] (instance? CSSColor obj))

(defn ->rgba [c]
  (-> c color/as-rgb (assoc :alpha (:alpha c))))

(defn ->hsla [c]
  (-> c color/as-hsl (assoc :alpha (:alpha c))))

(defn render-color [c] ; ->str
  (if (:alpha c)
      (let [{:keys [red green blue alpha]} (->rgba c)]
        (str "rgba(" (str/join "," [red green blue (or alpha 1)]) ")"))
      (color/as-hex c)))

(defalias darken color/darken)
(defalias lighten color/lighten)

(defn ->hex [c] (-> c ->hsla color/as-hex))