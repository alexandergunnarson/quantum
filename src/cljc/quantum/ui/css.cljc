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
  (satisfies? #?(:clj clojure.lang.IDeref :cljs core/IDeref) obj))

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
           (dropl 2)   ; drops "a{"
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
    {:dark-brown (rgb 84  56  71 )
     :light-gray (rgb 208 213 217)
     :white      (rgb 255 255 255)}))
 
(defn color [k] (get @colors k))

(def px-props
  #{:height :width
    :margin
      :margin-top   :margin-bottom
      :margin-left  :margin-right
    :font-size
    :padding
      :padding-top  :padding-bottom
      :padding-left :padding-right})

(defn flex-test [elem flex-name]
  (-> elem .-style .-display (set! ""))
  (-> elem .-style .-display (set! flex-name))
  (-> elem .-style .-display (not= "")))

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

(defnt normalize-prop-v
  keyword? ([v k] (name v))
  number?  ([v k] (if (contains? px-props k) (px v) (str v)))
  vector?  ([v k] (postwalk (f*n normalize-prop-v k) v))
  :default ([v k] v))

(defn css-color? [obj] (instance? CSSColor obj))

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
                      (fn->> color/as-hex (map-entry prop-k))
                    map?
                      (fn->> (map+
                               (fn [sub-prop-k sub-prop-v]
                                 (let [sub-prop-k-f
                                        (str/keyword+ prop-k "-" sub-prop-k)]
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
             (merge+ ret normalized-props)))
         {})
       compatibilize))

(def rstyle flattened-style)

; ======== CSS GENERATION FROM CLASSES ========

(defn each-class
  {:todo ["A rather large function which should be broken up
           into more modular pieces."]
   :attribution "Alex Gunnarson"}
  [^Map ret ^Keyword tag]
  (let [^Map style-assembled
         (if (-> tag name (contains? "."))
             (loop [^Map    class-props-n {}
                    ^String tag-n (name tag)] ; :div.pillar.pillar-upper ; or maybe :#abc
               (let [^String class-n
                       (if (contains? tag-n ".")
                           (->> tag-n (taker-until-incl "."))
                           tag-n)
                     ;_ (log/pr :debug "tag-n" tag-n "class-n" class-n)
                     ] ; .pillar-upper
                 (if (empty? tag-n)
                     class-props-n
                     (recur
                       (merge-keep-left ; gives precedence to subclasses
                         class-props-n
                         (get @styles-template (keyword class-n)))
                       (dropr (count class-n) tag-n)))))
             (let [^Keyword tag-f
                     (if (-> tag (str/starts-with? "#"))
                         (rest tag) ; TODO: handle # differently ; should determine it the other way around, by HTML not by CSS
                         tag)]
               (-> @styles-template (get tag-f))))
_ (log/pr :debug "style-assembled for" tag ":" style-assembled)
         ^Map style-flattened
           (flattened-style style-assembled)]
    (assoc ret tag style-flattened)))

(defn ^Map each-tag
  {:attribution "Alex Gunnarson"}
  [^Map ret ^Vec domain-for-freq]
  (->> domain-for-freq
       (reduce
         #(each-class %1 %2)
         {})
       (merge+ ret)))

(defn ^Map each-frequency
  {:attribution "Alex Gunnarson"}
  [^Map domain-frequencies ^Vec frequencies-sorted]
  (reduce
    (fn [^Map ret ^Number frequency]
      (let [^Vec domain-for-freq
              (get domain-frequencies frequency)]
        (each-tag ret domain-for-freq)))
    {}
    frequencies-sorted))

(defn ^Map fill-in-css-classes
  "Given a domain of HTML tags, assembles procedurally
   from the initial CSS stylesheet, |styles-template|,
   a 'filled-in' CSS stylesheet which contains styles
   associated with all classes and tags in the domain for which
   there are corresponding styles in the initial template.

   For instance, suppose the domain consisted of only one tag:
   |:div.pillar.pillar-upper|. Then the return value would be so: 

   {:div.pillar.pillar-upper
     {<merge of style of :.pillar-upper if present in stylesheet,
           then style of :.pillar       if present in stylesheet,
           then style of :div           if present in stylesheet>}}

   The purpose is to avoid an external stylesheet and style components
   dynamically using js.React.

   This 'filling in' is only done once, when the page loads."
  {:todo ["Possibly use the to-be-implemented macro /map->record/
           to save access time and memory?"]}
  [^Set domain]
  (let [^Map domain-period-frequencies
          (->> domain
               (coll/group-by+ ; group-by+
                 (fn-> name (coll/indices-of ".") count))
               redm)
        ^Vec frequencies-sorted
          (->> domain-period-frequencies
               (map key)
               sort ; test performance of this sort (is it qsort?)
               vec)
        ^Map frequencies-taken-care-of
          (each-frequency
             domain-period-frequencies
             frequencies-sorted)]
    frequencies-taken-care-of))

(defn fill-in-css-from-domain!
  {:todo ["Handle # (IDs) differently"]}
  [^Set domain]
  (->> domain
       fill-in-css-classes
       (swap! styles merge-keep-left)))

(defn fill-in-css-from-tag!
  [tag]
  (when-not (contains? @styles tag) ; If it didn't already calculate the styles
    (fill-in-css-from-domain! #{tag})))

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
