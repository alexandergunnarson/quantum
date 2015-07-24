(ns
  ^{:doc "JavaFX useful things."
    :contributors #{"Alex Gunnarson"}}
  quantum.ui.core
  (:require-quantum [:lib])
  (:require
    [freactive.core     :as rx    :refer [rx]]
    [fx-clj.core        :as fx    :refer [run<! pset!]]
    [fx-clj.css         :as fx.css]
    [quantum.core.io.filesystem :as fs]
    [quantum.ui.css     :as css])
  (:import
    (javafx.animation      Animation KeyValue KeyFrame Timeline AnimationTimer Interpolator
                           FadeTransition TranslateTransition RotateTransition ScaleTransition
                           PathTransition PathTransition$OrientationType)
    (javafx.collections    ObservableList FXCollections ListChangeListener
                           ListChangeListener$Change)
    (javafx.event          ActionEvent EventHandler EventType)
    (javafx.geometry       Insets Pos HPos)
    (javafx.scene          Group Scene Node Parent)
    (javafx.scene.effect   BoxBlur BlendMode Lighting Bloom)
    (javafx.scene.image    Image)
    (javafx.scene.input    DragEvent KeyEvent KeyCode MouseEvent)
    (javafx.scene.media    MediaPlayer Media MediaView)
    (javafx.scene.paint    Stop CycleMethod LinearGradient RadialGradient Color)
    (javafx.scene.text     Font FontPosture FontWeight Text TextBoundsType TextAlignment)
    (javafx.scene.layout   GridPane StackPane Pane Priority HBox VBox ColumnConstraints)
    (javafx.scene.shape    Circle Rectangle StrokeType Path PathElement MoveTo CubicCurveTo)
    (java.util             ArrayList List)
    (javafx.util           Duration Callback)
    (javafx.beans          InvalidationListener)
    (javafx.beans.property SimpleDoubleProperty)
    (javafx.beans.value    ChangeListener ObservableValue)
    (javafx.scene.control
      ComboBox ContentDisplay Labeled TableColumn TableRow
      TableCell ListCell ListView Label Tooltip TextArea TextField ContentDisplay
      TableView TableView$TableViewSelectionModel)
    (javafx.scene.control.cell PropertyValueFactory)))

(in-ns 'fx-clj.css)
(defn remove-global-stylesheet! [url]
  (let [^java.util.ArrayList stylesheets
          (-> (StyleManager/getInstance)
              (quantum.core.java/field userAgentStylesheets))
        stylesheet-index
          (-> (StyleManager/getInstance)
              (quantum.core.java/invoke getIndex (str url)))]
    (.remove stylesheets stylesheet-index) ; This apparently doesn't work
    (.clear stylesheets)))
(in-ns 'quantum.ui.core)

(def pseudo-properties
  {:grid-pane {:column-index    (fn [^Node node val-] (GridPane/setColumnIndex node (int val-)))
               :row-index       (fn [^Node node val-] (GridPane/setRowIndex    node (int val-)))
               :fill-width?     (fn [^Node node val-] (GridPane/setFillWidth   node val-))
               :fill-height?    (fn [^Node node val-] (GridPane/setFillHeight  node val-))
               :h-alignment     (fn [^Node node val-] (GridPane/setHalignment  node val-))
               :column-span     (fn [^Node node val-] (GridPane/setColumnSpan  node (int val-)))}
   :selected-item-listener
              {:selection-model-doto (fn [^TableView node val-] (println "NODE" node)
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
                                                 (-> change (.getAddedSubList) first val-))))))}})

(def pseudo-properties-set
  (->> pseudo-properties vals
       (map keys)
       (apply concat)
       (into #{})))

(def pseudo-tags
  (atom
    {:column-constraints
      (fn [props]
        (let [node (ColumnConstraints.)]
          (when (contains? props :percent-width)
            (.setPercentWidth node (:percent-width props)))
          node))}))

(defn fx [root-node]
  (let [node?           (fn-and vector? (fn-not map-entry?)
                          (fn-> first  keyword?))
        node-and-has-properties? (fn-and node?   (fn-> second map?    ))]
    (->> root-node
         (postwalk
           (whenf*n node-and-has-properties?
             (fn [[tag props :as node-vec]]
               (let [doto-list (transient {})
                     props-f
                       (reduce
                         (fn [ret prop-k prop-v]
                           (if (contains? pseudo-properties-set prop-k)
                               (let [pseudo-property-ns  (first  prop-v)
                                     pseudo-property-val (second prop-v)]
                                 (assoc! doto-list (-> pseudo-properties (get pseudo-property-ns) (get prop-k))
                                   pseudo-property-val)
                                 ret)
                               (assoc! ret prop-k prop-v)))
                         (transient {})
                         props)
                     node-vec-f (assoc node-vec 1 (persistent! props-f))
                     _ (log/pr ::debug "ABOUT TO COMPILE" node-vec-f)

                     compiled (if (in? tag @pseudo-tags)
                                  ((get @pseudo-tags tag) props)
                                  (fx/compile-fx node-vec-f))]
                 (doseq [doto-fn doto-val (persistent! doto-list)]
                   (doto-fn compiled doto-val))
                 compiled)))))))

(defn set-css! [file]
  #_(let [file-str (io/file-str file)]
    (fx/run! (javafx.application.Application/setUserAgentStylesheet nil)
     #_(-> (StyleManager/getInstance)
         (java/field userAgentStylesheets)
         (.clear))
     (.setDefaultUserAgentStylesheet
       (StyleManager/getInstance) file-str)
     (.addUserAgentStylesheet
       (StyleManager/getInstance)  file-str)
     (javafx.application.Application/setUserAgentStylesheet file-str)))
  
  #_ (fx/run!
    (Application/setUserAgentStylesheet (io/file-str [:resources "test.css"]))
    (.addUserAgentStylesheet (StyleManager/getInstance) (io/file-str [:resources "test.css"])))
  ; http://www.guigarage.com/2013/03/global-stylesheet-for-your-javafx-application/
  (fx/run<!!
    (fx.css/set-global-css!
      (io/read :read-method :str :path file))))
(import 'javafx.application.Application)

(def css-file-modified-handler
  (atom (fn [file]
          (set-css! (io/file-str file))
          (log/pr ::debug "CSS set."))))

(defonce css-file-watched (atom [:resources "test.css"]))

(defonce css-file-watcher
  (fs/file-watcher
    {:file     css-file-watched
     :handlers {:modified (fn [e] (@css-file-modified-handler e))}}
    {:id :css-file-watcher}))

(defn print-nodes
  {:java-source "http://stackoverflow.com/questions/9904726/javafx-2-and-css-classes"}
  [^Node node depth]
  (dotimes [i depth]
    (println " "))
  (println node)
  (if (instance? Parent node)
      (doseq [child (.getChildrenUnmodifiable ^Parent node)]
        (print-nodes child (inc depth)))))