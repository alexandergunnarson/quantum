(ns quantum.ui.css
  (:require
   [quantum.core.ns          :as ns        :refer [Num Vec Key]        ]
   [quantum.core.data.map    :as map       :refer [map-entry]          ]
   [quantum.core.data.vector :as vec                                   ]
   [quantum.core.function    :as fn        :refer [f*n]                ]
   [quantum.core.logic       :as log       :refer [fn-not fn-and fn-or splice-or]]
   [quantum.core.numeric     :as num       :refer [neg]                ]
   [quantum.core.type        :as type      :refer [instance+? class]   ]
   [quantum.core.string      :as str                                   ]
   [quantum.ui.form          :as form                                  ]
   [clojure.walk                           :refer [postwalk]           ]
   [figwheel.client          :as fw                                    ]
   [garden.core              :as css       :refer [css]                ]
   [garden.stylesheet        :as css-style                             ]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]]
   [quantum.core.collections :as coll :refer
     [redv redm into+ reduce+
      rest+ first+
      lasti takeri+ taker-untili+
      split-remove+
      last-index-of+ index-of+
      count+ takel+
      getr+ vec+
      map+ filter+ group-by+
      merge+ key+
      merge-keep-left]]
   [goog.style :as gstyle]
   [quantum.ui.form :as form])
  (:require-macros
   [cljs.core.async.macros :refer [go-loop go]]
   [quantum.core.function  :refer [fn->> fn-> <-]]
   [quantum.core.logic     :refer [whenc whenf whenf*n whencf*n condf condf*n]]))

(def styles          (atom {}))
(def styles-template (atom {})) ; This needs to be initialized 


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

; About ClojureScript protocols:
; - If you try with js/String, you get an "__ is not ISeqable"

(defn dropl+ [obj n]
  (getr+ obj n (lasti obj))
  ;(subs obj n (- (count obj) n))
  )

(defn dropr+ [obj n]
  (getr+ obj 0 (- (lasti obj) n)))
  ;(subs obj 0 (- (- (lasti obj) n) 0))

(defn ^String css-block-str
  [css-block]
  (->> (whenf css-block (fn-not vector?)
         (fn->> (vector :a)))
       (css {:pretty-print? false})
       (<- dropl+ 2)   ; drops "a{"
       (<- dropr+ 1))) ; drops "}"

(defn ^String css-prop-str
  {:in  "{:width 2 :font-size [[\"16px\" \"1px\"] [\"17px\"]]}"
   :out "width:2;font-size:16px 1px,17px" }
  [css-block]
  (->> (whenf css-block (fn-not map?)
         (fn->> (array-map :a) (vector :a)))
       (css {:pretty-print? false})
       (<- dropl+ 2)   ; drops "a{"
       (<- dropr+ 1)   ; drops "}"
       (<- dropl+ 2))) ; drops "a:"  

(defn ^String react-style
  {:in  "{:width     2
          :font-size [[\"16px\" \"1px\"] [\"17px\"]]
          :background
            {:color      :transparent  ; background-color
             :image      (url \"../imgs/flappy-base.png\")
             :repeat     :no-repeat
             :attachment :top
             :position   :left}}"
   :out "{:width                 2
          :font-size             \"16px 1px,17px\"
          :background-color      :transparent
          :background-image      \"url(../imgs/flappy-base.png\")
          :background-repeat     :no-repeat
          :background-attachment :top
          :background-position   :left}" }
  [^Map css-block]
  (->> css-block 
       (reduce+
         (fn [^Map ret ^Key prop-k prop-v]
           (let [^Map normalized-props
                  (condf prop-v
                    map?
                      (fn->> (map+
                               (fn [[sub-prop-k sub-prop-v]]
                                 ;(println "prop-k" prop-k "prop-v" prop-v "sub-prop-k" sub-prop-k "sub-prop-v" sub-prop-v)
                                 (map-entry
                                   (str/keyword+ prop-k "-" sub-prop-k)
                                   (css-prop-str sub-prop-v))))
                             redm)
                    vector?
                      (fn->> css-prop-str
                             (map-entry prop-k))
                    :else
                      (fn->> (map-entry prop-k)))
                 ;_ (println "normalized-props" normalized-props)
                 ;_ (println "merged" (merge+ ret normalized-props))
                 ]
             (merge+ ret normalized-props)))
         {})))

;(-> @state ui-form-template   (dcss/style-html style/styles))

(defn take-care-of-each-class
  [^Map ret ^Keyword tag]
  (let [^Map style-assembled
         (if (-> tag name (str/contains? "."))
             (loop [^Map    class-props-n {}
                    ^String tag-n (name tag)] ; :div.pillar.pillar-upper ; or maybe :#abc
               (let [^String class-n
                       (if (str/contains? tag-n ".")
                           (-> tag-n (takeri+ "."))
                           tag-n)] ; .pillar-upper
                 (if (empty? tag-n)
                     class-props-n
                     (recur
                       (merge-keep-left ; gives precedence to subclasses
                         class-props-n
                         (get @styles-template (keyword class-n)))
                       (dropr+ tag-n (count class-n))))))
             (let [^Keyword tag-f
                     (if (-> tag (str/starts-with? "#"))
                         (rest+ tag) ; TODO: handle # differently ; should determine it the other way around, by HTML not by CSS
                         tag)]
               (-> @styles-template (get tag-f))))
         ^Map style-react
           (react-style style-assembled)]
    (assoc ret tag style-react)))

(defn ^Map take-care-of-each-tag
  [^Map ret ^Vec domain-for-freq]
  (->> domain-for-freq
       (reduce+
         #(take-care-of-each-class %1 %2)
         {})
       (merge+ ret)))

(defn ^Map take-care-of-each-frequency
  [^Map domain-frequencies ^Vec frequencies-sorted]
  (reduce+
    (fn [^Map ret ^Number frequency]
      (let [; [:div.pillar.pillar-upper
            ;  :div.pillar.pillar-lower]
            ^Vec domain-for-freq
              (get domain-frequencies frequency)]
        (take-care-of-each-tag ret domain-for-freq)))
    {}
    frequencies-sorted))

; ret-f :
; {:div.pillar.pillar-upper
;   <merge of :.pillar-upper if present,
;        then :.pillar       if present,
;        then :div           if present> }
(defn ^Map fill-in-css-classes
  {:todo ["Possibly use the to-be-implemented macro /map->record/
           to save access time and memory?"]}
  [^Set domain]
  (let [;{2 [:div.pillar.pillar-upper
        ;    :div.pillar.pillar-lower]
        ; 1 [:div.pillars
        ;    :div.board
        ;    :h1.score
        ;    :div.flappy
        ;    :div.scrolling-border]
        ; 0 :div}
        ^Map domain-period-frequencies
          (->> domain
               (group-by ; group-by+
                 (fn->> name (filter+ (fn-> str (= "."))) redv count)))
        ; [0 1 2]
        ^Vec frequencies-sorted
          (->> domain-period-frequencies
               (map key+)
               sort ; lazy sort... test performance of this
               vec+)
        ^Map frequencies-taken-care-of
          (take-care-of-each-frequency
             domain-period-frequencies
             frequencies-sorted)]
    frequencies-taken-care-of))

(defn create-css-from-domain!
  {:todo ["Handle # (IDs) differently"]}
  [^Set domain]
  (->> domain
       (<- fill-in-css-classes)
       (swap! styles merge-keep-left)))

(defn add-css-media-style!
  "DOM manipulation.
   Adds @media style to <styles> tag in HTML."
  []
  (->> @styles-template
       (<- get :+media)
       (apply css-style/at-media)
       css-block-str
       gstyle/installStyles))

; (-> @state ui-form-template   (dcss/style-html style/styles))
; (dcss/create-css-from-domain! @form/ui-tag-domain style/styles style/styles-template)
; (dcss/fill-in-css-classes     @form/ui-tag-domain style/styles-template)

(defn ^Vec update-css-once!
  [^Vec html ^Atom state]
  (when-not (:css-created? @state)
    (form/determine-tag-domain! html)
    (create-css-from-domain! @form/ui-tag-domain))
  html)

; The `style` prop expects a mapping from style properties to values, not a string.

(defn style
  {:todo ["Use |split-at| down in the code"]}
  [^Vec html]
  (->> html
       (postwalk
         (whenf*n form/ui-element?
           (fn [^Vec elem]
             (let [^Key tag       (first+ elem)
                   ct             (count elem)
                   ^Map style-n+1 (get @styles tag)
                   ^Vec elem-n+1
                     (cond
                       (= ct 0) (throw "Empty HTML element")
                       (= ct 1) (conj elem style-n+1)
                       :else    (if (-> elem (get 1) map?)
                                    (update-in elem [1 :style]
                                      (fn-> (merge-keep-left style-n+1) react-style))
                                    (into+ [tag {:style (-> style-n+1 react-style)}]
                                      (rest+ elem))))]
                     (when-not (vector? elem-n+1) (throw "ELEM N+1 NOT VECTOR!"))
                     elem-n+1)))))) ; TODO: better to use /split-at/ and/or /insert-at/
            
