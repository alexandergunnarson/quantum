(ns ^{:doc "A namespace that checks for availability of CSS features."
      :todo ["Possibly rename 'quantum.ui.platform'?"]}
  quantum.ui.features
  (:require-quantum [:core fn map logic]))

#?(:cljs
(defn flex-test [elem flex-name]
  (-> elem .-style .-display (set! ""))
  (-> elem .-style .-display (set! flex-name))
  (-> elem .-style .-display (not= ""))))

(defn feature-test []
  #?(:clj {:chrome true} ; Because JavaFX will use Chromium via JXBrowser?
     :cljs
      (let [div (.createElement js/document "div")]
        (->> {:chrome  "flex"
              :safari  "-webkit-flex"
              :safari- "-webkit-box" ; (Older)
              :ie      "-ms-flexbox"}
             (map (fn [browser s] (map-entry (whenc browser (eq? :safari-) :safari) (flex-test div s))))
             (into {})))))

#?(:cljs
(defn determine-browser
  {:from "http://stackoverflow.com/questions/9847580/how-to-detect-safari-chrome-ie-firefox-and-opera-browser"
   :contributors {"Alex Gunnarson" "Ported to CLJC"}}
  []
  (let [opera? (or (.-opera js/window)
                   (-> js/navigator .-userAgent (.indexOf " OPR/") (>= 0)))]
    (cond
      ; Opera 8.0+ (UA detection to detect Blink/v8-powered Opera)
      opera?
      :opera
      ;  Chrome 1+
      (and (.-chrome js/window)
           (not opera?))
      :chrome
      ; Firefox 1.0+
      (try (when js/InstallTrigger true)
        (catch js/Error e false))
      :firefox 
      ; At least Safari 3+: "[object HTMLElementConstructor]"
      (-> js/Object .-prototype .-toString
          (.call (.-HTMLElement js/window))
          (.indexOf "Constructor")
          (> 0))
      :safari
      ; At least IE6
      (.-documentMode js/document)
      :ie))))

#?(:cljs (def browser (delay (determine-browser))))

#?(:cljs
(def ^{:doc "Determines whether the device is 'touchable'"
       :from "pukhalski/tap"} touchable?
  (delay (and (or (.-propertyIsEnumerable js/window  )
                  (.-hasOwnProperty       js/document))
              (or (.propertyIsEnumerable  js/window   "ontouchstart")
                  (.hasOwnProperty        js/document "ontouchstart")
                  (.hasOwnProperty        js/window   "ontouchstart"))))))


#?(:cljs
(def touch-events  
  (delay (let [events (cond
                        @touchable?
                        ["touchstart"
                         "touchmove"
                         "touchend"]
                        ; IE10
                        (-> js/window .-navigator .-msPointerEnabled)
                        ["MSPointerDown"
                         "MSPointerMove"
                         "MSPointerUp"]
                        ; Modern, device-agnostic
                        (-> js/window .-navigator .-pointerEnabled)
                        ["pointerdown"
                         "pointermove"
                         "pointerup"])]
    (zipmap [:start :move :end] events)))))

; EVENT UTILS

#?(:cljs
(defn attachEvent [element eventName callback]
  (when (.addEventListener js/window)
    (.addEventListener element eventName callback false))))

#?(:cljs
(defn createEvent [name]
  (when (.-createEvent js/document)
    (doto (-> js/window .-document (.createEvent "HTMLEvents"))
          (.initEvent name true true)
          (-> .-eventName (set! name))))))

#?(:cljs
(defn fireFakeEvent [e eventName]
  (when (.-createEvent js/document)
    (-> e .-target (.dispatchEvent eventName)))))

#?(:cljs
(defn getRealEvent [e]
  (cond (and (.-originalEvent e)
             (-> e .-originalEvent .-touches)
             (-> e .-originalEvent .-touches .-length))
        (-> e .-originalEvent .-touches (aget 0))
        
        (and (.-touches e) (-> e .-touches .-length))
        (aget (.-touches e) 0)
        
        :else e)))

; END EVENT UTILS

(def options {:eventName       "tap"
              :fingerMaxOffset 11})

(def coords (atom {}))
(declare deviceEvents)

; document.getElementById('any-element').addEventListener('tap', function (e) {
  ; All the magic happens here
; });

#?(:cljs
(def handlers
  {:start       (fn [e]
                  (let [real-e (getRealEvent e)]
                    (swap! coords assoc
                      :start  [(.-pageX real-e) (.-pageY real-e)]
                      :offset [0 0])
                    real-e))
   :move        (fn [e]
                  (when (or (:start coords)
                            (:move  coords))
                    (let [real-e (getRealEvent e)]
                      (swap! coords
                        (fn [coords-0]
                         (assoc coords-0
                           :move   [(.-pageX real-e) (.-pageY real-e)]
                           :offset [(js/Math.abs #_num/abs (- (-> coords-0 :move  first )
                                                (-> coords-0 :start first )))
                                    (js/Math.abs #_num/abs (- (-> coords-0 :move  second)
                                                (-> coords-0 :start second)))])))
                    real-e)))
   :end         (fn [e]
                  (let [real-e (getRealEvent e)]
                    (when (and (< (-> @coords :offset first )
                                  (:fingerMaxOffset options))
                               (< (-> @coords :offset second)
                                  (:fingerMaxOffset options))
                               (not (fireFakeEvent real-e (:eventName options))))
                      ; Windows Phone 8.0 triggers |click| after |pointerup| firing
                      ; #16 https://github.com/pukhalski/tap/issues/16
                      (when (or (-> js/window .-navigator .-msPointerEnabled)
                                (-> js/window .-navigator .-pointerEnabled  ))
                        (let [preventDefault*
                               (fn preventDefault* [clickEvent]
                                 (.preventDefault clickEvent)
                                 (-> real-e .-target
                                     (.removeEventListener preventDefault*)))]
                          (-> real-e .-target
                              (.addEventListener "click" preventDefault* false))))
              
                      (.preventDefault real-e))
              
                    (reset! coords {})))
   :click       (fn [e]
                  (when-not (fireFakeEvent e (:eventName options))
                     (.preventDefault e)))
   :emulatedTap (fn [e] 
                  (when (:offset coords)
                     (fireFakeEvent e (:eventName options) ))
                  (.preventDefault e))}))

#?(:cljs
(defn attachDeviceEvent [eventName]
  (attachEvent js/document.documentElement
               (get deviceEvents eventName)
               (get handlers     eventName))))

; (defn init []
;   (doseq [[k event] eventMatrix]
;     for (i = 0, i < eventMatrix.length, i++) {
;       (when (eventMatrix[i].test)
;         deviceEvents = eventMatrix[i].events

;         attachDeviceEvent(:start);
;         attachDeviceEvent(:move);
;         attachDeviceEvent(:end);
;         utils.attachEvent(document.documentElement, 'click', handlers['emulatedTap']);

;         return false))

;     utils.attachEvent(document.documentElement, 'click', handlers.click))

; (attachEvent js/window "load" init)