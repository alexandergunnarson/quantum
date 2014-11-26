(ns quanta.ui.core2 (:gen-class))
(require '[quantum.core.ns :as ns])
(ns/require-all *ns* :lib :clj)
(ns/nss *ns*)

(require
  '[clojure.string  :as    str]
  '[clojure.data    :as    cljdata]
  '[clojure.reflect :refer :all])


(defn handle-press!
  [& {:keys [^Object src-obj ^Object to-affect ^Keyword style ^AFunction handler]}]
  (jset! src-obj :on-mouse-released
    (wrap-handler to-affect
      (get-in @styles [style :release-handler])
      handler))
  (jset! src-obj :on-mouse-pressed 
    (wrap-handler to-affect
      (get-in @styles [style :press-handler])
      identity)))
(defn create-button!
  {:usage "(create-button! :id :adj-go
              :parent  :adj-buttons-pane*
              :text    'GO'
              :handler proc/adj-fn)"}
  [& {:keys [^Keyword id    ^Keyword parent
             ^Number  width ^Number  height
             ^Keyword button-style ^Keyword text-style
             ^String  text  ^AFunction handler
             ^Number  arc-radius-top-left    
             ^Number  arc-radius-top-right   
             ^Number  arc-radius-bottom-left 
             ^Number  arc-radius-bottom-right]
      :or   {button-style :button
             text-style   :button-text}}]
  (let [^Keyword parent-id       parent
        ^Keyword button-pane-id  (keyword (str (name id) "-button-pane*"))
        ^Keyword button-id       (keyword (str (name id) "-button"))
        ^Keyword button-label-id (keyword (str (name id) "-button-label"))]
    (jdef button-pane-id :stack-pane
      :alignment (. Pos CENTER))
    (jconj! parent-id button-pane-id)
    (if (every? nil?
          [arc-radius-top-left    arc-radius-top-right
           arc-radius-bottom-left arc-radius-bottom-right])
        (apply jdef button-id :rectangle
          :width width :height height
          (style button-style))
        (apply jdef button-id :multi-rounded-rectangle
          :width width :height height
          :arc-radius-top-left     arc-radius-top-left    
          :arc-radius-top-right    arc-radius-top-right   
          :arc-radius-bottom-left  arc-radius-bottom-left 
          :arc-radius-bottom-right arc-radius-bottom-right
          :button-style :select-button
          (style :select-button)))
    (jconj! button-pane-id button-id)
    (apply jdef button-label-id :text
      :text text
      (style text-style))
    (jconj! button-pane-id button-label-id)
    
    (handle-press! :src-obj (-> button-id name symbol eval)
      :to-affect (-> button-id name symbol eval)
      :handler   handler
      :style     button-style)
    (handle-press! :src-obj (-> button-label-id name symbol eval)
      :to-affect (-> button-id name symbol eval)
      :handler   handler
      :style     button-style)))

(defnode
  {:root
    {:type }})

(jdef* :rt :v-box
  :alignment (. Pos TOP_CENTER)
  :style (str "-fx-background-color: "
              (-> @colors :green :med css-color)))
(HBox/setHgrow rt (. Priority ALWAYS))
(VBox/setVgrow rt (. Priority ALWAYS))
;-----Scene/Window-----
(jdef* :scn :scene
  :width  800
  :height 550
  :root   rt)
;-----Stage/Window-surround-----
(jdef* :stg :stage
  :title (str "SOC QuickBooks Interfacer " *version*)
  :min-width 563
  :min-height 410
  :scene scn)
; SET ICON
(-> :stg ; (jset! :stg :icon (Image. "abc"))
    (jget :icons)
    (.add (Image. (str "file:"
                       (io/path (dirs :resources) "icon.png")))))

(in-ns 'clj-qb.ui.core)
(objs/clear! rt)
; ========= TITLE =========
(jdef! :title-box :v-box
  ; :parent ...
  ; :h-grow
  :min-height 100
  :max-height 100
  :style (str "-fx-background-color: "
              (-> @colors :green :med css-color))
  :alignment (. Pos CENTER))
(HBox/setHgrow title-box (. Priority ALWAYS))
; default /jdef/ should be to nothing
(jdef :title-text :text
  :text "QuickBooks Access"
  :fill (color 255 255 255)
  :alignment   (. TextAlignment  CENTER)
  :bounds-type (. TextBoundsType VISUAL)
  :font (Font. (-> @fonts :gotham :xlt) 40))
(jconj! :title-box :title-text)
; ========= CATEGORY TITLES =========
(jdef! :category-title-box* :h-box
  :alignment  (. Pos CENTER)
  :spacing    1
  :min-height (-> :title-box (jget :min-height) (/ 2))
  :max-height (-> :title-box (jget :max-height) (/ 2)))
(HBox/setHgrow category-title-box* (. Priority ALWAYS))

(apply jdef :sr-title-box :h-box
  (style :body-box))
(HBox/setHgrow sr-title-box (. Priority ALWAYS))
(jconj! :category-title-box* :sr-title-box)
;; (jset! :sr-title-box :on-mouse-drag-over)

(apply jdef :adj-title-box :h-box
  (style :body-box))
(HBox/setHgrow adj-title-box (. Priority ALWAYS))
(jconj! :category-title-box* :adj-title-box)

(apply jdef :sr-title :text
  :text        "SALES RECEIPTS"
  (style :background-title))
(jconj! :sr-title-box :sr-title)

(apply jdef :inv-adj-title :text
  :text        "INV. ADJUSTMENTS"
  (style :background-title))
(jconj! :adj-title-box :inv-adj-title)
; ========= BODY =========
(jdef :body-box* :h-box
  :spacing 1)
(HBox/setHgrow body-box* (. Priority ALWAYS))
(VBox/setVgrow body-box* (. Priority ALWAYS))
(jconj! :rt :body-box*)
; ========= SR =========
(jdef :sr-box :v-box
  :alignment (. Pos CENTER)
  :spacing 11
  :style (str "-fx-background-color: "
              (-> @colors :green :lt css-color)))
(HBox/setHgrow sr-box (. Priority ALWAYS))
(VBox/setVgrow sr-box (. Priority ALWAYS))
(jconj! :body-box* :sr-box)

(jdef :sr-date-box :v-box
  :alignment (. Pos CENTER)
  :style (str "-fx-background-color: "
              (-> @colors :green :lt css-color))
  :spacing 11)
;(VBox/setMargin sr-date-box (Insets. 0 90 0 90))
(jconj! :sr-box :sr-date-box)

(apply jdef :sr-date-label :text
  :text        "ENTER  DATE:"
  (style :date-label-text))
(jconj! :sr-date-box :sr-date-label)
;(VBox/setMargin sr-date-label (Insets. 11 0 0 0))

(jdef :sr-date-entry-box* :h-box
  :alignment   (. Pos CENTER)
  :spacing     1)
(jconj! :sr-date-box :sr-date-entry-box*)

(apply jdef :sr-day-box :text-field ; was jdef!
  :pref-column-count 2
  (style :text-field))
(limit-text-to! :sr-day-box 2)
(jconj! :sr-date-entry-box* :sr-day-box)

(apply jdef! :sr-month-box :text-field
  :pref-column-count 2
  (style :text-field))
(limit-text-to! :sr-month-box 2)
(jconj! :sr-date-entry-box* :sr-month-box)

(apply jdef! :sr-year-box :text-field
  :pref-column-count 3
  (style :text-field))
(limit-text-to! :sr-year-box 4)
(jconj! :sr-date-entry-box* :sr-year-box)


; ========= ADJ =========
(jdef :adj-box :v-box
  :alignment (. Pos CENTER)
  :style (str "-fx-background-color: "
              (-> @colors :green :lt css-color)))
(HBox/setHgrow adj-box (. Priority ALWAYS))
(VBox/setVgrow adj-box (. Priority ALWAYS))
(jconj! :body-box* :adj-box)


(jdef :adj-date-box :v-box
  :alignment (. Pos CENTER)
  :style (str "-fx-background-color: "
              (-> @colors :green :lt css-color))
  :spacing 11)
;(VBox/setMargin sr-date-box (Insets. 0 90 0 90))
(jconj! :adj-box :adj-date-box)

(apply jdef :adj-date-label :text
  :text        "ENTER  DATE:"
  (style :date-label-text))
(jconj! :adj-date-box :adj-date-label)
;(VBox/setMargin sr-date-label (Insets. 11 0 0 0))

(jdef :adj-date-entry-box* :h-box
  :alignment   (. Pos CENTER)
  :spacing     1)
(jconj! :adj-date-box :adj-date-entry-box*)

(apply jdef! :adj-month-box :text-field
  :pref-column-count 2
  (style :text-field))
(limit-text-to! :adj-month-box 2)
(jconj! :adj-date-entry-box* :adj-month-box)

(apply jdef! :adj-year-box :text-field
  :pref-column-count 3
  (style :text-field))
(limit-text-to! :adj-year-box 4)
(jconj! :adj-date-entry-box* :adj-year-box)

; ========= OUTPUT =========
(jdef :output-box :h-box
  :min-height 100
  :max-height 100
  :alignment (. Pos CENTER)
  :style (str "-fx-background-color: "
              (-> @colors :green :med css-color)))
(HBox/setHgrow output-box (. Priority ALWAYS))
(jconj! :rt :output-box)

(apply jdef :output-text :text
  :text        "Waiting for request..."
  (style :output-text))
(jconj! :output-box  :output-text)


; ====== BUTTON FNS  ======
(jdef :sr-buttons-pane* :h-box
  :alignment (. Pos CENTER)
  :spacing   11)
(jconj! :sr-date-box :sr-buttons-pane*)
;(VBox/setMargin sr-buttons-pane* (Insets. 0 0 11 0))
; ====== SR GO BUTTON ======
(create-button! :id :sr-go
  :parent :sr-buttons-pane*
  :width   100
  :height  40
  :text    "GO"
  :handler proc/sr-fn)
; ====== SR CANCEL BUTTON ======
(create-button! :id :sr-cancel
  :parent :sr-buttons-pane*
  :width   120
  :height  40
  :text    "CANCEL"
  :handler proc/sr-cancel-fn)

; ====== ADJ OPTIONS PANE ======
(jdef :adj-options-pane* :v-box
  :alignment (. Pos CENTER)
  :spacing   1)
(jconj! :adj-date-box :adj-options-pane*)
; ====== ADJ ITEMS/CARDS PANE ======
(jdef :adj-items-cards-pane* :h-box
  :alignment (. Pos CENTER)
  :spacing   1)
(jconj! :adj-options-pane* :adj-items-cards-pane*)
;(VBox/setMargin sr-buttons-pane* (Insets. 0 0 11 0))
; ====== ADJ ITEMS BUTTON ======
(create-button! :id :adj-cards
  :parent :adj-items-cards-pane*
  :arc-radius-top-left     10
  :arc-radius-top-right    0
  :arc-radius-bottom-left  0
  :arc-radius-bottom-right 0
  :width   80
  :height  30
  :text    "CARDS"
  :button-style :select-button
  :handler (proc/select-adj-type! :cards))
; ====== ADJ CARDS BUTTON ======
(create-button! :id :adj-items
  :parent :adj-items-cards-pane*
  :arc-radius-top-left     0
  :arc-radius-top-right    10
  :arc-radius-bottom-left  0
  :arc-radius-bottom-right 0
  :width   80
  :height  30
  :text    "ITEMS"
  :button-style :select-button
  :handler (proc/select-adj-type! :items))
; ====== ADJ FLORAL/FOODS PANE ======
(jdef :adj-floral-foods-pane* :h-box
  :alignment (. Pos CENTER)
  :spacing   1)
(jconj! :adj-options-pane* :adj-floral-foods-pane*)
; ====== ADJ FLORAL BUTTON ======
(create-button! :id :adj-floral
  :parent :adj-floral-foods-pane*
  :arc-radius-top-left     0
  :arc-radius-top-right    0
  :arc-radius-bottom-left  0
  :arc-radius-bottom-right 0
  :width   80
  :height  30
  :text    "FLORAL"
  :button-style :select-button
  :handler (proc/select-adj-type! :floral))
(create-button! :id :adj-foods
  :parent :adj-floral-foods-pane*
  :arc-radius-top-left     0 ; If you don't have this, then it doesn't deselect correctly...
  :arc-radius-top-right    0
  :arc-radius-bottom-left  0
  :arc-radius-bottom-right 0
  :width   80
  :height  30
  :text    "FOODS"
  :button-style :select-button
  :handler (proc/select-adj-type! :foods))
; ====== ADJ SALES TOOLS PANE ======
(jdef :adj-sales-tools-pane* :h-box
  :alignment (. Pos CENTER)
  :spacing   1)
(jconj! :adj-options-pane* :adj-sales-tools-pane*)
; ====== ADJ SALES TOOLS BUTTON ======
(create-button! :id :adj-sales-tools
  :parent :adj-sales-tools-pane*
  :arc-radius-top-left     0
  :arc-radius-top-right    0
  :arc-radius-bottom-left  10
  :arc-radius-bottom-right 10
  :width   161 ; extra 1 because of spacing
  :height  30
  :text    "SALES TOOLS"
  :button-style :select-button
  :handler (proc/select-adj-type! :sales-tools))

; ====== ADJ SELECT ALL PANEL ======
(jdef :adj-select-all-pane* :h-box
  :alignment (. Pos CENTER)
  :spacing   11)
(jconj! :adj-date-box :adj-select-all-pane*)
(create-button! :id :adj-select-all
  :parent :adj-select-all-pane*
  :arc-radius-top-left     10
  :arc-radius-top-right    10
  :arc-radius-bottom-left  10
  :arc-radius-bottom-right 10
  :width   161 ; extra 1 because of spacing
  :height  30
  :text    "ADJUST ALL"
  :button-style :select-button
  :handler (proc/select-adj-type! :all))

(aux/add-complement-nodes!
  #{:adj-items-button
    :adj-cards-button
    :adj-floral-button
    :adj-foods-button
    :adj-sales-tools-button
    :adj-select-all-button})

; ====== ADJ GO/CANCEL PANEL ======
(jdef :adj-go-cancel-pane* :h-box
  :alignment (. Pos CENTER)
  :spacing   11)
(jconj! :adj-date-box :adj-go-cancel-pane*)
; ====== ADJ GO BUTTON ======
(create-button! :id :adj-go
  :parent :adj-go-cancel-pane*
  :width   60
  :height  40
  :text    "GO"
  :handler proc/adj-fn)
; ====== ADJ CANCEL BUTTON ======
(create-button! :id :adj-cancel
  :parent :adj-go-cancel-pane*
  :width   90
  :height  40
  :text    "CANCEL"
  :handler proc/adj-cancel-fn)
