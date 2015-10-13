(ns
  ^{:doc "JavaFX useful things."
    :contributors #{"Alex Gunnarson"}}
  quantum.ui.core
  (:refer-clojure :exclude [descendants])
  (:require-quantum [:lib])
  (:require
    [freactive.core     :as rx    :refer [rx]]
    [fx-clj.core        :as fx    :refer [run<! pset!]]
    [fx-clj.css         :as fx.css]
    [quantum.hotfix]
    [quantum.ui.css     :as css]
    [quantum.core.convert :as convert :refer [->observable ->predicate]])
  (:import
    (javafx.stage Modality Stage)
    (javafx.animation      Animation KeyValue KeyFrame Timeline AnimationTimer Interpolator
                           FadeTransition TranslateTransition RotateTransition ScaleTransition
                           PathTransition PathTransition$OrientationType)
    (javafx.collections    ObservableList FXCollections ListChangeListener
                           ListChangeListener$Change)
    (javafx.collections.transformation
                           FilteredList)
    (javafx.event          ActionEvent EventHandler EventType)
    (javafx.geometry       Insets Pos HPos)
    (javafx.scene          Group Scene Node Parent Cursor)
    (javafx.scene.effect   BoxBlur BlendMode Lighting Bloom)
    (javafx.scene.image    Image)
    (javafx.scene.input    DragEvent KeyEvent KeyCode MouseEvent)
    (javafx.scene.layout   Region)
    (javafx.scene.media    MediaPlayer Media MediaView)
    (javafx.scene.paint    Stop CycleMethod LinearGradient RadialGradient Color)
    (javafx.scene.text     Font FontPosture FontWeight Text TextBoundsType TextAlignment)
    (javafx.scene.layout   GridPane StackPane Pane Priority HBox VBox ColumnConstraints)
    (javafx.scene.shape    Circle Rectangle StrokeType Path PathElement MoveTo CubicCurveTo)
    (java.util             ArrayList List)
    (javafx.util           Duration Callback)
    (javafx.beans          InvalidationListener)
    (javafx.beans.property Property SimpleDoubleProperty SimpleStringProperty)
    (javafx.beans.value    ChangeListener ObservableValue)
    (javafx.scene.control
      ComboBox ContentDisplay Labeled TableColumn TableRow
      TableCell ListCell ListView Label Tooltip TextArea TextField ContentDisplay
      TableView
      TableView$TableViewSelectionModel TableColumn$CellDataFeatures TableColumn$CellEditEvent)
    (javafx.scene.control.cell PropertyValueFactory TextFieldTableCell)))



; serialize to hiccup
; deserialize to JavaFX
; Be able to save the scene to a file which outputs Clojure data structures/types. Tree-based
; (defn save-stage! []
;   (io/write! :directory [:resources] :name "stg" :data (serialize @fx/tree)))
; (defn load-stage! []
;   (io/read   :directory [:resources] :name "stg"))
; Implement a version of DaisyDisk.

(def pseudo-properties
  (atom
    {:grid-pane/column-index (fn [^Node node val-] (GridPane/setColumnIndex node (int val-)))
     :grid-pane/row-index    (fn [^Node node val-] (GridPane/setRowIndex    node (int val-)))
     :grid-pane/fill-width?  (fn [^Node node val-] (GridPane/setFillWidth   node      val-))
     :grid-pane/fill-height? (fn [^Node node val-] (GridPane/setFillHeight  node      val-))
     :grid-pane/h-alignment  (fn [^Node node val-] (GridPane/setHalignment  node      val-))
     :grid-pane/column-span  (fn [^Node node val-] (GridPane/setColumnSpan  node (int val-)))
     :grid-pane/margin       (fn [^Node node val-] (GridPane/setMargin      node ^Insets   val-))
     ;:grid-pane/pref-size    (fn [^Node node [x y]] (GridPane/setPrefSize   node (double x) (double y)))
     
     :h-box/h-grow           (fn [^Node node val-] (HBox/setHgrow           node ^Priority val-))
     :h-box/margin           (fn [^Node node val-] (HBox/setMargin          node ^Insets   val-))
     :v-box/v-grow           (fn [^Node node val-] (VBox/setVgrow           node ^Priority val-))
     :v-box/margin           (fn [^Node node val-] (VBox/setMargin          node ^Insets   val-))
     ; SELECTION MODEL
     :selected-item-listener (fn [^TableView node val-] (println "NODE" node)
                                 (-> node
                                     (.getSelectionModel)
                                     (.selectedItemProperty)
                                     (doto
                                       (.addListener
                                        
                                         (proxy [ChangeListener] []
                                           (changed [^ObservableValue observable, oldValue, newValue]
                                             (val- observable, oldValue, newValue))))
                                       #_(.addListener
                                         (proxy [InvalidationListener] []
                                           (changed [^ObservableValue observable, oldValue, newValue]
                                             (val- observable, oldValue, newValue))))))
                                 #_(-> node
                                     (.getSelectionModel)
                                     (.getSelectedItems)
                                     (.addListener
                                            (proxy [ListChangeListener] []
                                              (onChanged [^ListChangeListener$Change change]
                                                (.next change) ; Apparently necessary...
                                                (-> change (.getAddedSubList) first val-))))))}))

(def pseudo-tags
  (atom
    {:column-constraints
      (fn [props]
        (let [node (ColumnConstraints.)]
          (when (containsk? props :percent-width)
            (.setPercentWidth node (:percent-width props)))
          node))}))

(defn fx [root-node]
  (let [node? (fn-and vector? (fn-not map-entry?)
                (fn-> first keyword?))
        node-and-has-properties? (fn-and node? (fn-> second map?))
        parse-node
          (fn [[tag props :as node-vec]]
            (let [doto-list (transient {})
                  props-f
                    (reduce
                      (fn [ret prop-k prop-v]
                        (if (containsk? @pseudo-properties prop-k)
                            (do (assoc! doto-list (get @pseudo-properties prop-k) prop-v)
                                ret)
                            (assoc! ret prop-k prop-v)))
                      (transient {})
                      props)
                  node-vec-trans (transient [])
                  flatten-vector-children
                    (doseqi [child node-vec i]
                      (cond
                        (= i 1)
                          (conj! node-vec-trans (persistent! props-f))
                        ((fn-and vector? (fn-> first keyword? not)) child)
                          (doseq [node child]
                            (when node ; to avoid nil children
                              (conj! node-vec-trans node)))
                        :else
                          (conj! node-vec-trans child)))
                  _ (log/pr ::debug "ABOUT TO COMPILE" node-vec-trans)
                  compiled (if (in? tag @pseudo-tags)
                               ((get @pseudo-tags tag) props)
                               (fx/compile-fx (persistent! node-vec-trans)))]
              (doseq [doto-fn doto-val (persistent! doto-list)]
                (doto-fn compiled doto-val))
              compiled))]
    (->> root-node
         (postwalk
           (whenf*n node-and-has-properties? parse-node)))))



; OBSERVABLE

(defrecord FXObservableAtom
  [^clojure.lang.IAtom immutable
   ^javafx.collections.transformation.FilteredList observable])

(defn fx-observable-atom [v]
  (FXObservableAtom. (atom v)
    ; Default is to show nothing...
    (-> v ->observable (FilteredList. (->predicate (constantly true))))))


; TREE

(defn uberparent [^Node x]
  (if-let [parent (-> x .getParent)]
    (uberparent parent)
    x))

(defnt children
  ([^javafx.scene.Node x]
    (try (->> x .getChildren (into []))
      (catch Exception _ []))))

(defnt descendants
  ([^javafx.scene.Node x] (->> x children (map+ (juxt identity (fn-> descendants))) redm)))

(defn print-nodes
  {:java-source "http://stackoverflow.com/questions/9904726/javafx-2-and-css-classes"}
  [^Node node depth]
  (dotimes [i depth]
    (println " "))
  (println node)
  (if (instance? Parent node)
      (doseq [child (.getChildrenUnmodifiable ^Parent node)]
        (print-nodes child (inc depth)))))

; STAGE

(defn sandbox
  "Creates a JavaFX stage with the root element of the stage's scene set to
  the result of evaluating refresh-fn. If F5 is pressed within the stage,
  refresh-fn will be re-evaluated and its new result will be bound to as the
  root of the scene. This can be very useful for prototyping.
  Suggested usage:
  (defn my-refresh-fn [] (do-create-view....))
  (sandbox #'my-refresh-fn)
  ;; By binding to a var,  my-refresh-fn can be  easily updated and reloaded
  ;; at the REPL"
  ([refresh-fn] (sandbox refresh-fn nil))
  ([refresh-fn {:keys [title maximized export-stage export-scene on-key-pressed]
               :or {title (-> (gensym "Sandbox") name) on-key-pressed fn-nil}}]
    (fx/run<!!
      (let [scene (fx/scene (refresh-fn))
            _ (if export-scene (reset! export-scene scene))
            ^Stage stage-f (if export-stage (reset! export-stage (fx/stage)) (fx/stage))]
        (fx/pset! scene
               {:on-key-pressed
                 (fn do-sandbox-refresh [^KeyEvent e]
                   (when (= KeyCode/F5 (.getCode e))
                     (pset! scene {:root (refresh-fn)}))
                   (on-key-pressed e))})
        (.setScene stage-f scene)
        (.initModality stage-f Modality/NONE)
        (pset! stage-f {:title title})
        (when maximized (.setMaximized stage-f true))
        (.show stage-f)
        stage-f))))

; RESIZABLE

(defn set-absolute-height! [^Region region n]
  (.setMinHeight region n)
  (.setMaxHeight region n))

(defn set-absolute-width! [^Region region n]
  (.setMinWidth region n)
  (.setMaxWidth region n))

(defn set-drag-resizable!
  "Only height resizing is currently implemented."
  {:usage '(set-drag-resizable! @resizable-region :top :x)}
  ([^Region region type dimension] (set-drag-resizable! region type dimension nil))
  ([^Region region type dimension {:keys [ex0-fn ex1-fn] :as opts}]
    (throw-unless (in? type #{:top :bottom :both}) "Resize type not recognized.")
    (throw-unless (in? dimension #{:x :y}) "Dimension not recognized.")
    (let [; It's okay to use volatiles because there's only one thread
          ; (the FX GUI thread) accessing it
          breadth-fn (condp = dimension
                       :x (fn ^double [] (.getWidth  region))
                       :y (fn ^double [] (.getHeight region)))
          loc-fn (condp = dimension
                   :x (fn ^double [ ^MouseEvent event] (.getX event))
                   :y (fn ^double [ ^MouseEvent event] (.getY event)))
          appropriate-cursor
            (condp = dimension
              :x Cursor/W_RESIZE
              :y Cursor/N_RESIZE)
          _ (log/pr :debug "CURSOR FOR RESIZE" appropriate-cursor)
          ; extremity 1: bottom or right
          ex1? (MutableContainer. nil)
          ; extremity 0: top or left
          ex0-resizable? (or (= type :top   ) (= type :left ) (= type :both))
          ; extremity 1: bottom or right
          ex1-resizable? (or (= type :bottom) (= type :right) (= type :both))
          ;The margin around the control that a user can click in to start resizing
          ;the region.
          resize-margin (or (:resize-margin opts) 10)
          ; The y of the event is relative to the pane
          within-ex0-tolerance?
           (fn [^MouseEvent event]
             (and ex0-resizable?
                  (num/within-tolerance? (loc-fn event) 0 resize-margin)))
          within-ex1-tolerance?
           (fn [^MouseEvent event]
             (and ex1-resizable?
                  (num/within-tolerance? (loc-fn event) (breadth-fn) resize-margin)))
          within-bounds? (fn-or within-ex0-tolerance? within-ex1-tolerance?)
          set-absolute-breadth!
            (condp = dimension
              :x set-absolute-width!
              :y set-absolute-height!)
          set-ex0-breadth!
            (or ex0-fn
                (fn [^MouseEvent event]
                  (set-absolute-breadth! region (- (double (breadth-fn)) (double (loc-fn event))))))
          set-ex1-breadth!
            (or ex1-fn
                (fn [^MouseEvent event]
                  (set-absolute-breadth! region (- (double (breadth-fn)) (- (double (breadth-fn)) (double (loc-fn event)))))))]
      (.setOnMouseMoved    region
        (fx/event-handler [^MouseEvent event]
          (if (within-bounds? event)
              (.setCursor region appropriate-cursor)
              (.setCursor region Cursor/DEFAULT))))
      (.setOnMousePressed  region
        (fx/event-handler [^MouseEvent event]
          (cond
            (within-ex0-tolerance? event)
              (do (.set ex1? false)
                  (set-ex0-breadth! event))
            (within-ex1-tolerance? event)
              (do (.set ex1? false)
                  (set-ex1-breadth! event)))))
      (.setOnMouseDragged  region
        (fx/event-handler [^MouseEvent event]
          (cond
            (and (.get ex1?)       ex1-resizable?)
              (set-ex1-breadth! event)
            (and (not (.get ex1?)) ex0-resizable?)
              (set-ex0-breadth! event))))
      (.setOnMouseReleased region
        (fx/event-handler [^MouseEvent event]
          (.setCursor region Cursor/DEFAULT))))))

(defn listen! [^Property prop listener]
  (.addListener prop
    (proxy [ChangeListener] []
      (changed [^ObservableValue observable, oldValue newValue]
        (listener observable, oldValue newValue)))))

(defn listen-invalidated! [^Property prop f]
  (.addListener prop
    (proxy [InvalidationListener] []
      (invalidated [arg0]
        (f arg0)))))

