(ns quanta.ui.wrapper)
; (use '[seesaw.options :only
;         [option-provider option-map apply-options
;           default-option OptionProvider
;           get-option-value
;           apply-options]]) ; for some reason this instantly started up a (presumably Swing) window...


;  No method in multimethod '-item-to-ssa' for dispatch value: :def,

; #_ is completely skipped by the reader... more so than the comment form, which eval's to nil
(comment
  "The scene graph can be a map with name-keys associated with data structures representing objects.
  Their lower-level arcane operations will be handled through this wrapper.
  "
  (def scene-graph [:node-1 {:type :combo-box :other-options "other-vals"}])
  
  "What JavaFX will do, essentially, is refresh the window/view based on the changing values of the scene which are passed to it.
  This is more efficient/possible/feasible given the structural sharing that Clojure employs.
  Essentially it will be a functional UI data-wise, but 'under the hood' it will be mutable, because it will be based on JavaFX.
  Whether this will pose a problem (or many problems) is not currently known.")

(import
  '(javafx.animation Animation KeyValue KeyFrame Timeline AnimationTimer Interpolator
     FadeTransition TranslateTransition RotateTransition ScaleTransition
     PathTransition PathTransition$OrientationType) ; $ for inner classes
  '(javafx.application Application)
  '(javafx.beans.property SimpleDoubleProperty)
  '(javafx.collections ObservableList FXCollections)
  '(javafx.event ActionEvent EventHandler EventType)
  '(javafx.geometry Insets Pos)
  '(javafx.scene Group Scene)
  '(javafx.scene.effect BoxBlur BlendMode Lighting Bloom)
  '(javafx.scene.input DragEvent KeyEvent KeyCode MouseEvent)
  '(javafx.scene.paint Stop CycleMethod LinearGradient Color)
  '(javafx.scene.text Font FontPosture FontWeight Text)
  '(javafx.scene.layout GridPane StackPane Pane)
  '(javafx.scene.shape Circle Rectangle StrokeType Path PathElement MoveTo CubicCurveTo)
  '(javafx.stage.Stage)
  '(java.util ArrayList List)
  '(javafx.util Duration Callback))
(require '[clojure.string :as string])

(set! *warn-on-reflection* false)

(defonce force-toolkit-init (javafx.embed.swing.JFXPanel.)) ; this starts the application...
; IllegalStateException Toolkit not initialized  com.sun.javafx.application.PlatformImpl.runLater

(defn get-by-key [object info-type & args]
  (let [method-str (str "get" (string/capitalize (name info-type)))]
    (clojure.lang.Reflector/invokeInstanceMethod object method-str (to-array args))))

(defn do-fx*"
  A modification of run-later waiting for the running method to return. You should use do-fx.
  " [f]
  (if (javafx.application.Platform/isFxApplicationThread)
    (apply f []) ; why not just (f)?
    (let [result (promise)]
      (javafx.application.Platform/runLater
       (deliver result (try (f) (catch Throwable e e))))
      @result)))
(defmacro do-fx "
  Runs the code on the FX application thread and waits until the return value is delivered.
  " [& body]
    `(do-fx* (fn [] ~@body)))

(defn start [stage]
  (let [btn (-> (Button.)
                (.setText "Say 'Hello World'")
                (.setOnAction
                  (proxy [EventHandler] []
                    (handle [event] ; my guess is that "handle" is a method within the EventHandler class
                      (println "Hello World!")))))
        rt (-> (StackPane.)
               (.getChildren)
               (.add btn))
        scene (new Scene rt 300 250)
        stage
          (do-fx ; doto
            (-> (.build (StageBuilder/create))
                (.setScene scene)))]
    (-> (.setTitle stage "Hello World!")
        (.setScene stage scene)
        (.show stage))))

(defn constructor-helper [clazz & args]
  (do-fx (clojure.lang.Reflector/invokeConstructor (resolve clazz) (to-array (remove nil? args)))))

(defmacro construct [clazz keys]
  `(defmethod construct-node '~clazz [cl# ar#]
     (apply constructor-helper cl# (for [k# ~keys] (get ar# k#)))))
(defmulti construct-node (fn [class args] class))
(defmethod construct-node :default [class _]
  (do-fx (eval `(new ~class))))

(construct javafx.stage.Stage [:stage-style])

(extend-protocol Configurable
  javafx.stage.Stage
    (config* [this name] (get-option-value this name))
    (config!* [this args] (apply-options this args))
  javafx.scene.Scene
    (config* [this name] (get-option-value this name))
    (config!* [this args] (apply-options this args))
  javafx.scene.Node
    (config* [this name] (get-option-value this name))
    (config!* [this args] (apply-options this args)))

(defn- dash-case
  [^String s]
  (let [gsub (fn [s re sub] (.replaceAll (re-matcher re s) sub))]
    (-> s
      (gsub #"([A-Z]+)([A-Z][a-z])" "$1-$2")
      (gsub #"([a-z]+)([A-Z])" "$1-$2")
      (.replace "_" "-")
      (clojure.string/lower-case))))
(defn- get-option-info [m]
  (if (and (= 1 (count (.getParameterTypes m)))
          (.matches (.getName m) "^set[A-Z].*"))
    (let [base-name (.substring (.getName m) 3)
          type      (first (.getParameterTypes m))
          dash-name (dash-case base-name)
          boolean?  (= Boolean/TYPE type)]
      { :setter (symbol  (.getName m))
        :getter (symbol  (str (if boolean? "is" "get") base-name))
        :name   (keyword (if boolean?
                           (str dash-name "?")
                           dash-name))
        :event (if (= javafx.event.EventHandler type)
                  type)
        :type   type
        :paint (= javafx.scene.paint.Paint type)
        :enum   (.getEnumConstants type) })))
(defn- get-public-instance-methods [class]
  (->> class
    .getDeclaredMethods
    (remove #(.isSynthetic %))
    (filter #(let [ms (.getModifiers %)]
               (= java.lang.reflect.Modifier/PUBLIC
                  (bit-and ms
                           (bit-or java.lang.reflect.Modifier/PUBLIC
                                   java.lang.reflect.Modifier/STATIC)))))))
(defmacro options-for-class [class]
  `(option-map
     ~@(for [{:keys [setter getter name type enum]}
             (->> (resolve class)
               get-public-instance-methods
               (map get-option-info)
               (filter identity))]
         (cond
           enum `(let [set-conv# ~(into {} (for [e enum]
                                             [(keyword (dash-case (.name e)))
                                              (symbol (.getName type) (.name e)) ]))
                       get-conv# (clojure.set/map-invert set-conv#)]
                   (default-option
                      ~name
                      (fn [c# v#]
                        (.. c# (~setter (set-conv# v# v#))))
                      (fn [c#]    (get-conv# (.. c# ~getter)))
                     (keys set-conv#)))
           :else `(default-option
                      ~name
                      (fn [c# v#] (.. c# (~setter v#)))
                      (fn [c#] (.. c# ~getter))
                      [~type])))))
(defmacro defobject [func-name class-or-construct base-options extra-options]
  (let [opts-name (symbol (str (name func-name) "-options"))
        class (if (symbol? class-or-construct)
                class-or-construct
                (first class-or-construct))
        args  (if (symbol? class-or-construct)
                []
                (rest class-or-construct))]
    `(do
       (def ~opts-name
         (merge
           ~@base-options
           (options-for-class ~class)
           ~@extra-options))

       (option-provider ~class ~opts-name)

       (defn ~func-name
         [& opts#]
         (apply-options (new ~class ~@args) opts#)))))
(def window-options (options-for-class javafx.stage.Window))
#_(def stage-options
  (merge
    window-options
    (options-for-class javafx.stage.Stage)))

(defobject stage javafx.stage.Stage [window-options] [])
(defn make-stage []
  (stage
    :title "Path Transitions"
    :scene (new Scene (Pane.) 300 250)))
(defn run []
  (do-fx
    (-> ;(stage :scene (make-scene))
        (make-stage)
        .show)))

(comment
  Life-cycle
  The entry point for JavaFX applications is the Application class.
  The JavaFX runtime does the following, in order, whenever an application is launched:

  1. Constructs an instance of the specified Application class
  2. Calls the init() method
  3. Calls the start(javafx.stage.Stage) method
  4. Waits for the application to finish, which happens when either of the following occur:
  the application calls Platform.exit() the last window has been closed and the implicitExit
  attribute on Platform is true Calls the stop() method Note that the start method is abstract and must be overridden.)
(comment
  All builder classes (? or at least the StageBuilder one) were deprecated as of JavaFX 8.)
(comment
  https://github.com/friemen/async-ui
  A prototype demonstrating JavaFX or Swing GUI programming with clojure.core.async.

  source: http://www.falkoriemenschneider.de/a__2014-05-01__Applying-core-async-to-JavaFX.html
  My doubt about whether it makes sense to create a full-blown Clojure library for JavaFX programming has grown.
  While I'm convinced that the ideas and the design I used (like separated processes connected via channels,
  view representation by pure data, an explicit builder and a binding) are a good foundation, I'm afraid that
  project specific requirements between different applications vary in numerous details. A library that provides
  the level of comfort I consider as necessary inevitably becomes a batteries-included framework with lots of
  assumptions and implicit behaviour. My experience with those frameworks in Java-land tells me that such a
  "claim of omnipotence" almost always leads to pain and horrendous work-arounds. I still have to make up my
  mind if there is any piece in this picture that can be extracted to form a useful library.)

(javafx.stage.Stage.) ; Not on FX application thread
(do-fx (javafx.stage.Stage.)) ; Without having init-ed the application, probably waiting in the thread...
                              ; Having inited, gives the following exceptions:
(comment ; Even when I use this big thing from Upshot!!
  Exception in thread "JavaFX Application Thread" java.lang.AbstractMethodError: clojure.core$promise$reify__6363.run()V
  at com.sun.javafx.application.PlatformImpl$6$1.run(PlatformImpl.java:301)
  at com.sun.javafx.application.PlatformImpl$6$1.run(PlatformImpl.java:298)
  at java.security.AccessController.doPrivileged(Native Method)
  at com.sun.javafx.application.PlatformImpl$6.run(PlatformImpl.java:298)
  at com.sun.glass.ui.InvokeLaterDispatcher$Future.run(InvokeLaterDispatcher.java:95)
  #<IllegalStateException java.lang.IllegalStateException: Not on FX application thread, currentThread = nREPL-worker-24>)

(defn fx* [ctrl & args]
  (let [args#
          (if-not (and (nil? (first args))
                       (map? (first args)))
                  (apply hash-map args)
                  (first args))
        {:keys [bind listen content children]} args#
        props# bind
        listeners# listen
        content# (-> [] (into content) (into children))
        qualified-name# (get-qualified ctrl) ; get-qualified
        methods# (get-method-calls ctrl) ; get-method-calls
        args# (dissoc args# :bind :listen)
        obj# (construct-node qualified-name# args#)] ; construct-node ; YES
    (do-fx
      (doseq [arg# args#] ;; Apply arguments
        (if (contains? methods# (key arg#))
          (((key arg#) methods#) obj# (wrap-arg (val arg#) (type obj#)))))
      (doseq [prop# props#] ;; Bind properties
        (bind-property!* obj# (key prop#) (val prop#))) ; bind-property!*
      (doseq [listener# listeners#] ;; Add listeners
        (set-listener!* obj# (key listener#) (val listener#))) ; set-listener!*
      (if-not (empty? content#)
        (swap-content!* obj# (fn [_] content#))) ; swap-content!*
      obj#)))
(defn fx* [ctrl & args]
  (let [args#
          (if-not (and (nil? (first args))
                       (map? (first args)))
                  (apply hash-map args)
                  (first args))
        {:keys [bind content children]} args#
        props# bind
        content#
          (-> []
              (into content)
              (into children))
        qualified-name# (get-qualified ctrl) ; get-qualified
        methods# (get-method-calls ctrl) ; get-method-calls
        args# (dissoc args# :bind :listen)
        obj# (construct-node qualified-name# args#)] ; construct-node ; YES
    (do-fx
      (doseq [arg# args#] ;; Apply arguments
        (if (contains? methods# (key arg#))
          (((key arg#) methods#) obj# (wrap-arg (val arg#) (type obj#)))))
      (doseq [prop# props#] ;; Bind properties
        (bind-property!* obj# (key prop#) (val prop#))) ; bind-property!*
      (if-not (empty? content#)
        (swap-content!* obj# (fn [_] content#))) ; swap-content!*
      obj#)))

(defmacro fx "
  The central macro of ClojureFX. This takes the name of a node as declared in the pkgs atom and
  named arguments for the constructor arguments and object setters.

  Special keys:
   * `bind` takes a map where the key is a property name (e.g. :text or :grid-lines-visible) and the value an atom. This internally calls `bind-property!`.
   * `content` or `children` (equivalent) must be a datastructure a function given to `swap-content!*` would return.
  " [ctrl & args]
  `(fx* '~ctrl ~@args))
(defmacro def-fx [name ctrl & props]
  `(def ~name (fx ~ctrl ~@props)))

(def-fx stg stage
  :title (str "Experimental " version)
  :scene scn)
(do-fx (.show stg))


(defmethod wrap-arg :stage-style [arg class]
  (clojure.lang.Reflector/getStaticField javafx.stage.StageStyle (-> arg name str/upper-case)))

(defmethod swap-content!* javafx.stage.Stage [obj fun]
  (.setScene obj (fun (.getScene obj))))

; Though it's still in its infancy, I've been able to use JavaFx from the REPL using Upshot.
;The main trick is just to completely ignore Application and create your scene directly. 
;To do this you need only force the runtime to initialize and example of which can be seen at core.clj:69. 
;The other trick is that almost everything you do has to be wrapped in a do-fx block 
;to ensure it runs on the JavaFX thread. JavaFX is much more picky about threading than Swing.


(defn start []
(do-fx (.show stage)))


 (defn main- [& ^string[args]]
    (launch args))



(def scene
  (fx :type :scene
      :width 800
      :height 600
      :root rt))

;___________________________________________________________________________________________________________________________________
;========================================================{ CAPITALIZATION  }========================================================
;========================================================{                 }========================================================
(defn camelcase [in & [method?]]
  (let [in (name in)
        in (str/split in #"-")
        in (map #(if (= (str (first %)) (str/upper-case (first %)))
                   % (str (str/upper-case (subs % 0 1)) (subs % 1))) in)
        in (apply str (into [] in))]
    (if method?
      (str (str/lower-case (subs in 0 1)) (subs in 1))
      in)))
(defn uncamelcaseize [sym] ; "Uncamelcase-izes a string."
  (let [s (-> sym str seq)]
    (loop [s s
           out (list)]
      (if (empty? s)
        (subs (->> out reverse (apply str)) 1)
        (recur (rest s)
               (if (Character/isUpperCase (first s))
                 (->> out (cons \-) (cons (Character/toLowerCase (first s))))
                 (cons (first s) out)))))))
(defn prepend-and-camel [prep s]
  (let [c (camel (str prep "-" s))]
    (str (str/lower-case (subs c 0 1)) (subs c 1))))
