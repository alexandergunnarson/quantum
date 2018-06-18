(ns quantum.untyped.ui.style.color
  (:require
    [clojure.string            :as str]
    [garden.color              :as color
      #?@(:cljs [:refer [CSSColor]])]
    [quantum.untyped.core.vars :as uvar
      :refer [defalias]])
#?(:clj
  (:import
    garden.color.CSSColor)))

(defn css-color? [obj] (instance? CSSColor obj))

(defalias >rgb color/as-rgb)

(defn >rgba
  ([c] (if (and (css-color? c)
                (:red       c)
                (:green     c)
                (:blue      c)
                (:alpha     c))
           c
           (-> c >rgb (assoc :alpha (or (:alpha c) 1)))))
  ([r g b] (>rgba r g b 1))
  ([r g b a] (color/map->CSSColor {:red r :green g :blue b :alpha a})))

(defn >rgba|str
  ([c] (let [{:keys [red green blue alpha]} (>rgba c)]
         (str "rgba(" (str/join "," [red green blue alpha]) ")")))
  ([r g b]   (>rgba|str (>rgba r g b)))
  ([r g b a] (>rgba|str (>rgba r g b a))))

(defalias >hsl color/as-hsl)

(defn >hsla [c]
  (-> c >hsl (assoc :alpha (or (:alpha c) 1))))

(defn >hex [c] (-> c >hsla color/as-hex))

(defalias color/darken )
(defalias color/lighten)

(def colors
  ;; `rgba` primarily because that's what `react-native-animatable` accepts
  {;; ----- Brand-specific ----- ;;
   :facebook-blue       (>rgba|str 59  88  152)
   :google-red          (>rgba|str 214 72  55 )
   ;; ----- Miscellaneous ----- ;;
   :cheery-seaside-blue (>rgba|str 51  197 255)
   :sky-blue            (>rgba|str 119 225 255)
   :serious-sea-foam    (>rgba|str 91  206 193) ; 'serious' because slightly desaturated
   :dark-sea-foam       (>rgba|str 49  120 133)
   :golden              (>rgba|str 255 197 37 )
   ;; ----- Grayscale ----- ;;
   :white               (>rgba|str 255 255 255)
   ;; above white
   :light-gray          (>rgba|str 244 245 247)
   ;; good placeholder text on `light-gray`
   :medium-light-gray   (>rgba|str 183 190 204)
   :medium-gray         (>rgba|str 122 134 154)
   ;; good text on `light-gray`
   :dark-gray           (>rgba|str 66  82  110)
   ;; essentially black; good text on white
   :darkest-gray        (>rgba|str 23  43  77 )
   :overlay             (>rgba|str 9   30  66  0.04)
   ;; good text on `light-gray`+`overlay`
   :black               (>rgba|str 0   0   0  )
   :transparent         (>rgba|str 0   0   0   0)})

(defn >color [k] (get colors k))
