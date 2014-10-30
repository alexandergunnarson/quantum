(ns quanta.library.ui.jfx
  (:require
    [quanta.library.ui.init]
    [quanta.library.data.queue  :as q        :refer [queue]]
    [clojure.data               :as cljdata]
    [clojure.reflect                         :refer :all])
  (:gen-class))
(import 'javafx.scene.Node)
(require '[quanta.library.ns :as ns])
(ns/require-all *ns* :lib :clj)
(ns/nss *ns*)

(comment
  "What JavaFX will do, essentially, is
  refresh the window/view based on the changing values of the scene
  which are passed to it.
  This is more efficient/possible/feasible given the structural
  sharing that Clojure employs.")

(def tree (atom {}))
(def objs (atom {}))
; Look up keyword IDs by object reference
(def obj-key-pairs (atom {}))
(def complement-nodes-set (atom #{}))
(defprotocol Lookup
  (lookup [obj]))
; Looks up an object by keyword or node
(extend-protocol Lookup
  Keyword (lookup [k]   (get @objs k)))
(extend-protocol-for-all Lookup
  [Node Object nil]
    (lookup [obj] (get @objs (get @obj-key-pairs obj))))
(def fx-node? (partial instance? Node))
(def fx-obj?
  (fn-or fx-node? (partial instance? javafx.scene.text.Font)))
(defn get-tree-key [obj]
  (->> @tree
      (coll/tree-filter
        (fn-and vector?
          (compr first  keyword?)
          (compr second (f*n identical? obj)))
        first) ; the keyword
      first)) ; the first result
(defn get-tree-val [k]
  (->> @tree
      (coll/tree-filter
        (fn-and vector?
          (compr first (eq? k)))
        second) ; the val
      first)) ; the first result

; Preprocess the reflection-powered things. And make them not reflection powered.
; For instance:
; "Elapsed time: 410.539 msecs"
; "Elapsed time: 17.087 msecs"
;___________________________________________________________________________________________________________________________________
;======================================================{ THREADING HELPERS  }=======================================================
;======================================================{                    }=======================================================
(defn run-later*
  "Simple wrapper for Platform/runLater. You should use run-later."
  [f]
  (javafx.application.Platform/runLater f))

(defmacro run-later [& body]
  `(run-later* (fn [] ~@body)))

(defn run-now*"
A modification of run-later waiting for the running method to return. You should use run-now.
" [f]
(if (javafx.application.Platform/isFxApplicationThread)
  (apply f [])
  (let [result (promise)]
    (run-later
     (deliver result (try (f) (catch Throwable e e))))
    @result)))

(defmacro run-now "
Runs the code on the FX application thread and waits until the return value is delivered.
" [& body]
  `(run-now* (fn [] ~@body)))
(defmacro do-fx [& body]
  `(run-now* (fn [] ~@body)))
;___________________________________________________________________________________________________________________________________
;======================================================={ HELPER FUNCTIONS }========================================================
;======================================================={                  }========================================================
(def method-fetcher "Fetches all public methods of a class."
  (memoize
   (fn [class]
     (let [cl (reflect class)
           current-methods
             (->> cl :members
                  (filter :return-type)
                  (filter (fn-> :flags (contains? :public))))
           static-methods   (filter (fn-> :flags (contains? :static)) current-methods)
           instance-methods (remove (fn-> :flags (contains? :static)) current-methods)
           methods {:instance (map :name instance-methods)
                    :static   (map :name static-methods)}]
       (if (nil? cl)
           methods
           (reduce (fn [a b]
                     (let [b (method-fetcher (resolve b))]
                       (-> a
                          (update-in [:instance] (fn-> (conj (:instance b)) flatten))
                          (update-in [:static]   (fn-> (conj (:static   b)) flatten)))))
                   methods (:bases cl)))))))

(defn exec-method [inst method-0 & args]
  (let [method (symbol method-0)
        clazz# (-> inst class .getName symbol)
        clazz# (if (= clazz# 'java.lang.Class) inst clazz#)
        methods# (method-fetcher inst)]
        ; (when (= method-0 'setFill)
        ;   (println methods#)
        ;   (println method-0)
        ;   (println inst)
        ;   (println (class inst)))
    (cond
     (->> methods# :instance (filter (eq? method)) nempty?)
     (clojure.lang.Reflector/invokeInstanceMethod inst          (name method) (to-array args))
     (->> methods# :static   (filter (eq? method)) nempty?)
     (clojure.lang.Reflector/invokeStaticMethod   (name clazz#) (name method) (to-array args))
     :else (do (println (str "Method does not exist: " method)) :error))))
;___________________________________________________________________________________________________________________________________
;========================================================{ CAPITALIZATION  }========================================================
;========================================================{                 }========================================================
(defn prepend-and-camel [prep s]
  (let [c (str/camelcase (str prep "-" s))]
    (str (str/lower-case (subs c 0 1))
         (subs c 1))))
;___________________________________________________________________________________________________________________________________
;======================================================{ COLLECTION HELPERS }=======================================================
;======================================================{                    }=======================================================
;; This probably isn't the ideal approach for mutable collections. Check back for better ones.
(defn seq->observable [s]
  (javafx.collections.FXCollections/unmodifiableObservableList s))

(defn map->observable [m]
  (javafx.collections.FXCollections/unmodifiableObservableMap m))

(defn set->observable [s]
  (javafx.collections.FXCollections/unmodifiableObservableSet s))
;___________________________________________________________________________________________________________________________________
;======================================================{ ARGUMENT WRAPPING  }=======================================================
;======================================================{                    }=======================================================
(defmulti wrap-arg "Autoboxing-like behaviour for arguments for ClojureFX nodes." (fn [arg class] arg))

(defmethod wrap-arg :default [arg class] arg)

(defmethod wrap-arg :accelerator [arg _]
  (if (string? arg)
    (javafx.scene.input.KeyCombination/keyCombination arg)
    arg))
;___________________________________________________________________________________________________________________________________
;======================================================={   DATA BINDING   }========================================================
;======================================================={                  }========================================================
(defmulti bidirectional-bind-property! (fn [type obj prop & args] type))
(defmulti bind-property!* (fn [obj prop target] (type target)))
(defmethod bind-property!* clojure.lang.Atom [obj prop at]
  (let [listeners (atom [])
        inv-listeners (atom [])
        prop (str (str/camelcase prop true) "Property")
        property (run-now (clojure.lang.Reflector/invokeInstanceMethod obj prop (to-array [])))
        observable (reify javafx.beans.value.ObservableValue
                     (^void addListener [this ^javafx.beans.value.ChangeListener l] (swap! listeners conj l))
                     (^void addListener [this ^javafx.beans.InvalidationListener l] (swap! inv-listeners conj l))
                     (^void removeListener [this ^javafx.beans.InvalidationListener l] (swap! inv-listeners #(remove #{l} %)))
                     (^void removeListener [this ^javafx.beans.value.ChangeListener l] (swap! listeners #(remove #{l} %)))
                     (getValue [this] @at))]
    (add-watch at (keyword (name prop))
               (fn [_ r oldS newS]
                 (run-now (doseq [listener @inv-listeners] (.invalidated listener observable))
                          (doseq [listener @listeners] (.changed listener observable oldS newS)))))
    (run-now (.bind property observable))
    (run-now (doseq [listener @inv-listeners] (.invalidated listener observable)))
    obj))

(defmacro bind-property! "Binds properties to atoms.
Other STM objects might be supported in the future.
Whenever the content of the atom changes, this change is propagated to the property.

args is a named-argument-list, where the key is the property name (e.g. :text) and the value the atom.
" [obj & args]
  (let [m# (apply hash-map args)]
    `(do ~@(for [entry# m#]
             `(bind-property!* ~obj ~(key+ entry#) ~(val+ entry#))))))

(defn- prep-key-code [k]
  {:keycode k
   :name        (-> (.getName k) str/lower-case keyword)
   :value       (-> (.valueOf k) str/lower-case keyword)
   :arrow?      (.isArrowKey k)
   :digit?      (.isDigitKey k)
   :function?   (.isFunctionKey k)
   :keypad?     (.isKeypadKey k)
   :media?      (.isMediaKey k)
   :modifier?   (.isModifierKey k)
   :navigation? (.isNavigationKey k)
   :whitespace? (.isWhitespaceKey k)})

(defn- prep-pickresult [p]
  {:pickresult p
   :distance (.getIntersectedDistance p)
   :face (.getIntersectedFace p)
   :node (.getIntersectedNode p)
   :point (.getIntersectedPoint p)
   :tex-coord (.getIntersectedTexCoord p)})

(defn- prep-event-map [e & {:as m}]
  (let [prep {:event e
              :source (.getSource e)
              :type (.getEventType e)
              :target (.getTarget e)
              :consume #(.consume e)
              :string (.toString e)}]
    (if (nil? m) prep (merge prep m))))
(defn- add-modifiers [m e]
  (merge m
         {:alt-down? (.isAltDown e)
          :control-down? (.isControlDown e)
          :meta-down? (.isMetaDown e)
          :shift-down? (.isShiftDown e)
          :shortcut-down? (.isShortcutDown e)}))
(defn- add-coords [m e]
  (merge m
         {:screen-coords {:x (.getScreenX e) :y (.getScreenY e)}
          :scene-coords {:x (.getSceneX e) :y (.getSceneY e)}
          :coords {:x (.getX e) :y (.getY e) :z (.getZ e)}}))

(defmulti preprocess-event (fn [e] (type e))) ; this can be improved with defprotocols
(defmethod preprocess-event :default [e]
  (prep-event-map e))
(defmethod preprocess-event javafx.scene.input.ContextMenuEvent [e]
  (-> (prep-event-map e
                     :pickresult (prep-pickresult (.getPickResult e))
                     :keyboard-trigger? (.isKeyboardTrigger e))
     (add-coords e)))
(defmethod preprocess-event javafx.scene.input.InputMethodEvent [e]
  (prep-event-map e
                  :caret-position (.getCaretPosition e)
                  :committed (.getCommitted e)
                  :composed (.getComposed e)))
(defmethod preprocess-event javafx.scene.input.KeyEvent [e]
  (-> (prep-event-map e
                     :character (.getCharacter e)
                     :code (prep-key-code (.getCode e))
                     :text (.getText e))
     (add-modifiers e)))
(defmethod preprocess-event javafx.scene.input.MouseEvent [e]
  (-> (prep-event-map e
                     :button            (-> (.getButton e) .valueOf str/lower-case keyword)
                     :click-count       (.getClickCount e)
                     :pickresult        (prep-pickresult (.getPickResult e))
                     :drag-detected?    (.isDragDetect          e)
                     :primary-button?   (.isPrimaryButtonDown   e)
                     :secondary-button? (.isSecondaryButtonDown e)
                     :middle-button?    (.isMiddleButtonDown    e)
                     :popup-trigger?    (.isPopupTrigger        e)
                     :sill-since-press? (.isStillSincePress     e)
                     :synthesized?      (.isSynthesized         e))
     (add-modifiers e)
     (add-coords e)))
(defmethod preprocess-event javafx.scene.input.TouchEvent [e]
  (prep-event-map e
    :set-id       (.getEventSetId e)
    :touch-count  (.getTouchCount e)
    :touch-point  (.getTouchPoint e) ;; TODO Wrapper for TouchPoint
    :touch-points (.getTouchPoints e)
    :alt-down? (.isAltDown e)
    :control-down? (.isControlDown e)
    :meta-down? (.isMetaDown e)
    :shift-down? (.isShiftDown e)))
;___________________________________________________________________________________________________________________________________
;============================================================{   API   }============================================================
;============================================================{         }============================================================
(defn set-listener!* [obj event fun]
  (run-now
    (clojure.lang.Reflector/invokeInstanceMethod obj
      (prepend-and-camel "set" (name event))
      (to-array [(reify javafx.event.EventHandler
                   (handle [this t]
                     (fun (preprocess-event t))))]))))

(defmacro set-listener! "Adds a listener to a node event.
The listener gets a preprocessed event map as shown above.
" [obj event args & body]
`(set-listener!* ~obj ~event (fn ~args ~@body)))
;___________________________________________________________________________________________________________________________________
;========================================================={  CONTENT MOD. }=========================================================
;========================================================={  CHILD ELEMS. }=========================================================
;; Usage: `(swap-content! <object> <modification-function>)`.
;; The return value of modification-function becomes the new value.

(defmulti swap-content!* (fn [obj fun] (class obj)))
(defmacro def-simple-swapper [class-0 getter setter]
  `(defmethod swap-content!* ~class-0 [obj# func#] ; defmethods are terrible
     (let [bunch# (~getter obj#)]
       (run-now (~setter bunch# (func# (into [] bunch#)))))))

(defmacro swap-content!
  ([obj fun] `(do (swap-content!* ~obj ~fun) ~obj))
  ([obj f arg & args]
    `(do (swap-content!* ~obj (f*n ~f ~arg ~@args)) ~obj)))

; (def update-fx [node-0 prop-key update-val] ; change to accomodate more key-val pairs
;   (fx node-0 )) ; traverse all the properties and create a new one with the updated value(s)
; (def update-fx! [node-0 prop-key update-val] ; change to accomodate more key-val pairs
;   (conj-fx! (get-fx node-0 :parent)
;     (update-fx node-0 prop-key update-val)))

(def-simple-swapper javafx.scene.layout.Pane             .getChildren     .setAll)
(def-simple-swapper javafx.scene.shape.Path              .getElements     .setAll)
(def-simple-swapper javafx.scene.Group                   .getChildren     .setAll)
(def-simple-swapper javafx.scene.control.Accordion       .getPanes        .setAll)
(def-simple-swapper javafx.scene.control.ChoiceBox       .getItems        .setAll)
(def-simple-swapper javafx.scene.control.ColorPicker     .getCustomColors .setAll)
(def-simple-swapper javafx.scene.control.ComboBox        .getItems        .setAll)
(def-simple-swapper javafx.scene.control.ContextMenu     .getItems        .setAll)
(def-simple-swapper javafx.scene.control.ListView        .getItems        .setAll)
(def-simple-swapper javafx.scene.control.Menu            .getItems        .setAll)
(def-simple-swapper javafx.scene.control.MenuBar         .getMenus        .setAll)
(def-simple-swapper javafx.scene.control.TableColumn     .getColumns      .setAll)
(def-simple-swapper javafx.scene.control.TabPane         .getTabs         .setAll)
(def-simple-swapper javafx.scene.control.ToggleGroup     .getToggles      .setAll)
(def-simple-swapper javafx.scene.control.ToolBar         .getItems        .setAll)
(def-simple-swapper javafx.scene.control.TreeItem        .getChildren     .setAll)
(def-simple-swapper javafx.scene.control.TreeTableColumn .getColumns      .setAll)

(defmethod swap-content!* javafx.scene.control.SplitPane [obj fun]
  (let [data {:items (into [] (.getItems obj))
              :dividers (into [] (.getDividers obj))}
        res (fun data)]
    (.setAll (.getItems obj) (:items res))
    (.setAll (.getDividers obj) (:dividers res))))
(defmethod swap-content!* javafx.scene.control.ScrollPane [obj fun]
  (.setContent obj (fun (.getContent obj))))
(defmethod swap-content!* javafx.scene.control.TitledPane [obj fun]
  (.setContent obj (fun (.getContent obj))))
(defmethod swap-content!* javafx.scene.control.Tab        [obj fun]
  (.setContent obj (fun (.getContent obj))))
;___________________________________________________________________________________________________________________________________
;========================================================{ BUILDER PARSING }========================================================
;========================================================{                 }========================================================
(def pkgs (atom {"javafx.scene.control" '[accordion button cell check-box check-box-tree-item check-menu-item choice-box
                                          color-picker combo-box context-menu custom-menu-item date-picker date-cell hyperlink
                                          indexed-cell index-range label list-cell list-view menu-bar menu menu-button menu-item
                                          pagination password-field popup-control progress-bar progress-indicator
                                          radio-button radio-menu-item scroll-bar scroll-pane separator
                                          separator-menu-item slider split-menu-button split-pane tab table-cell table-column
                                          table-position table-row table-view tab-pane text-area text-field tree-cell
                                          titled-pane toggle-button toggle-group tool-bar tooltip tree-item tree-view
                                          tree-table-cell tree-table-column tree-table-row tree-table-view]
                 "javafx.scene.control.cell" '[check-box-list-cell check-box-table-cell check-box-tree-cell
                                               choice-box-list-cell choice-box-table-cell choice-box-tree-cell
                                               combo-box-list-cell combo-box-table-cell combo-box-tree-cell
                                               text-field-list-cell text-field-table-cell text-field-tree-cell
                                               property-value-factory]
                 "javafx.scene.layout" '[anchor-pane border-pane column-constraints flow-pane grid-pane h-box pane region
                                         row-constraints stack-pane tile-pane v-box]
                 "javafx.scene.text" '[text font text-flow]
                 "javafx.scene.shape" '[arc arc-to circle close-path cubic-curve cubic-curve-to ellipse
                                        h-line-to v-line-to line line-to move-to path polygon polyline quad-curve quad-curve-to
                                        rectangle SVG-path box cylinder mesh-view sphere]
                 "javafx.scene.canvas" '[canvas]
                 "javafx.scene.image" '[image-view]
                 "javafx.scene.input" '[clipboard-content key-character-combination key-code-combination mnemonic]
                 "javafx.scene.effect" '[blend bloom box-blur color-adjust color-input displacement-map drop-shadow float-map
                                         gaussian-blur glow image-input inner-shadow lighting motion-blur perspective-transform
                                         reflection sepia-tone shadow]
                 "javafx.scene.paint" '[color image-pattern linear-gradient radial-gradient stop]
                 "javafx.scene.chart" '[chart area-chart bar-chart line-chart bubble-chart pie-chart scatter-chart
                                        stacked-area-chart stacked-bar-chart x-y-chart
                                        axis category-axis number-axis value-axis]
                 "javafx.scene.media" '[audio-clip media media-player media-view]
                 "javafx.scene.transform" '[affine rotate scale shear translate]
                 "javafx.scene.web" '[HTML-editor prompt-data web-engine web-view]
                 "javafx.scene" '[scene group image-cursor perspective-camera snapshot-parameters
                                  ambient-light parallel-camera perspective-camera point-light]
                 "javafx.animation" '[fade-transition fill-transition parallel-transition path-transition pause-transition
                                      rotate-transition scale-transition sequential-transition stroke-transition timeline
                                      translate-transition]
                 "javafx.stage" '[stage directory-chooser file-chooser popup]
                 "javafx.geometry" '[bounding-box dimension-2D insets point-2D point-3D rectangle-2D]
                 "javafx.embed.swing" '[JFX-panel]}))

(def get-qualified "
An exhaustive list of every visual JavaFX component. To add entries, modify the pkgs atom.<br/>
Don't use this yourself; See the macros \"fx\" and \"def-fx\" below.
" (memoize (fn [builder]
             (let [builder (symbol builder)]
               (first (filter (comp not nil?) (for [k (keys @pkgs)]
                                              (if (not (empty? (filter #(= % builder) (get @pkgs k))))
                                                (symbol (str k "." (str/camelcase (name builder))))))))))))

(def get-method-calls (memoize (fn [ctrl]
                                 (let [full (resolve (get-qualified ctrl))
                                       fnm (eval `(method-fetcher ~full))
                                       fns (flatten [(:static fnm) (:instance fnm)])
                                       calls (atom {})]
                                   (doseq [fun fns
                                           :when (= "set" (subs (str fun) 0 3))]
                                     (swap! calls assoc
                                            (keyword (str/un-camelcase (subs (name fun) 3)))
                                            (eval `(fn [obj# arg#] (. obj# ~fun arg#)))))
                                   @calls))))
;___________________________________________________________________________________________________________________________________
;======================================================={ CONSTRUCTOR TOOLS }=======================================================
;======================================================={                   }=======================================================
(defn constructor-helper [class-0 & args]
  (run-now (clojure.lang.Reflector/invokeConstructor (resolve class-0) (to-array (remove nil? args))))) ; maybe it would help not to use Reflect

(defmacro construct [clazz keys]
 `(defmethod construct-node '~clazz [cl# ar#]
    (apply constructor-helper cl#
      (for [k# ~keys]
        (if (vector? k#)
            ((second k#) (get ar# (first k#)))
            (get ar# k#))))))

(defmulti construct-node (fn [class-sym args] class-sym))
(defmethod construct-node :default [class-0 _]
  (println class-0 "in default construct-node.")
  (run-now (eval `(new ~class-0))))

(construct javafx.scene.control.ColorPicker [:color])
(construct javafx.scene.layout.BackgroundImage
  [:image :repeat-x :repeat-y :position :size])
(construct javafx.scene.layout.BorderImage
  [:image :widths :insets :slices :filled :repeat-x :repeat-y]) ;; TODO Wrapper for BorderWidths, BorderRepeat and Insets
;___________________________________________________________________________________________________________________________________
;=========================================================={ BUILDER API }==========================================================
;=========================================================={             }==========================================================
(defn- symbolwalker [q]
  (if (symbol? q)
    (if-let [x (resolve q)]
      x
      q)
    q))

(def ^:private varwalker (whenf*n var? deref))

(defn fx* [ctrl & args]
  (let [{:keys [bind listen content children] :as args#}
         (ifn args
              (compr first (fn-or nnil? (fn-not map?)))
              (partial apply hash-map) ; oh, okay then
              first)
        props#     bind
        listeners# listen
        content# (-> [] (into+ content) (into+ children))
        qualified-name# (get-qualified ctrl)
        methods# (get-method-calls ctrl)
        args# (dissoc args# :bind :listen)
        obj# (construct-node qualified-name# args#)] ; that's how it knows what to construct
    (run-now (doseq [arg# args#] ;; Apply arguments
               (if (contains? methods# (key arg#))
                 (((key arg#) methods#) obj# (wrap-arg (val arg#) (type obj#)))))
             (doseq [prop# props#] ;; Bind properties
               (bind-property!* obj# (key prop#) (val prop#)))
             (doseq [listener# listeners#] ;; Add listeners
               (set-listener!* obj# (key listener#) (val listener#)))
             (if-not (empty? content#)
               (swap-content!* obj# (fn [_] content#)))
             obj#)))

(defmacro fx "
The central macro of ClojureFX. This takes the name of a node as declared in the pkgs atom and
named arguments for the constructor arguments and object setters.

Special keys:
 * `bind` takes a map where the key is a property name (e.g. :text or :grid-lines-visible) and the value an atom. This internally calls `bind-property!`.
 * `listen` takes a map where the key is an event name (e.g. :on-action) and the value a function handling this event.
 * `content` or `children` (equivalent) must be a datastructure a function given to `swap-content!*` would return.
" [ctrl & args]
`(fx* '~ctrl ~@args))
;___________________________________________________________________________________________________________________________________
;============================================================={ STAGE }=============================================================
;============================================================={       }=============================================================
(construct javafx.stage.Stage [:stage-style])
(defmethod wrap-arg :stage-style [arg class]
  (clojure.lang.Reflector/getStaticField javafx.stage.StageStyle (-> arg name str/upper-case)))
(defmethod swap-content!* javafx.stage.Stage [obj fun]
  (.setScene obj (fun (.getScene obj))))
;___________________________________________________________________________________________________________________________________
;============================================================={ SCENE }=============================================================
;============================================================={       }=============================================================
(construct javafx.scene.Scene [:root :width :height :depth-buffer :scene-antialiasing])
(defmethod wrap-arg :scene-antialiasing [arg class]
  (clojure.lang.Reflector/getStaticField javafx.scene.SceneAntialiasing (-> arg name str/upper-case)))
(defmethod swap-content!* javafx.scene.Scene [obj fun]
  (.setRoot obj (fun (.getRoot obj))))
;___________________________________________________________________________________________________________________________________
;============================================================={ IMAGE }=============================================================
;============================================================={       }=============================================================
(defmethod construct-node javafx.scene.image.Image
  [c {:keys [is requested-width requested-height preserve-ratio smooth url background-loading] :as args}]
  (cond
   (contains? args :is) (constructor-helper c [is requested-width requested-height preserve-ratio smooth])
   (and (contains? args :url)
      (= 2 (count (keys args)))) (constructor-helper c [url background-loading])
   :else (constructor-helper c [url requested-width requested-height preserve-ratio smooth background-loading])))
;___________________________________________________________________________________________________________________________________
;========================================================={    GRADIENTS    }=======================================================
;========================================================={                 }=======================================================
(defmethod construct-node 'javafx.scene.paint.LinearGradient
  [c {:keys [start-x start-y end-x end-y proportional cycle-method stops]}]
  (constructor-helper c
    [start-x start-y end-x end-y proportional cycle-method (into-array javafx.scene.paint.Stop stops)]))
; (defmethod construct-node 'javafx.scene.paint.RadialGradient
;   [c {:keys [focus-angle focus-distance center-x center-y radius proportional cycle-method stops]}]
;   (println "RadialGradient!")
;   (constructor-helper c
;     focus-angle focus-distance center-x center-y radius proportional cycle-method (into-array javafx.scene.paint.Stop stops)))
(construct javafx.scene.paint.RadialGradient
  [:focus-angle :focus-distance :center-x :center-y :radius :proportional :cycle-method [:stops (partial into-array javafx.scene.paint.Stop)]])
;___________________________________________________________________________________________________________________________________
;============================================================{ GRID PANE }==========================================================
;============================================================{           }==========================================================
;___________________________________________________________________________________________________________________________________
;============================================================{ TABLE VIEW }=========================================================
;============================================================{            }=========================================================
(defmethod wrap-arg :items javafx.scene.control.TableView [arg clazz]
  (seq->observable arg))

(defmethod wrap-arg :columns javafx.scene.control.TableView [arg clazz]
  (seq->observable arg))

(defmethod swap-content!* javafx.scene.control.TableView [obj fun]
  (let [data {:items                (into+ [] (.getItems              obj))
              :columns              (into+ [] (.getColumns            obj))
              :sort-order           (into+ [] (.getSortOrder          obj))
              :visible-leaf-columns (into+ [] (.getVisibleLeafColumns obj))}
        res (fun data)]
    (.setAll (.getItems              obj) (:items                res))
    (.setAll (.getColumns            obj) (:columns              res))
    (.setAll (.getSortOrder          obj) (:sort-order           res))
    (.setAll (.getVisibleLeafColumns obj) (:visible-leaf-columns res))))

