(ns
  ^{:doc "Namespace for creating/defining UI components."
    :attribution "alexandergunnarson"}
  quantum.untyped.ui.components
  (:refer-clojure :exclude [for reduce])
  (:require
  #?@(:cljs
   [[reagent.core                  :as rx]
    [reagent.impl.component
      :refer [react-class?]]
    [reagent.interop
      :refer [$ $!]]])
    [quantum.untyped.core.data
      :refer [val?]]
    [quantum.untyped.core.log      :as log]
    [quantum.untyped.core.system   :as usys
      :refer  [#?@(:cljs [react-native])]]
    [quantum.untyped.reactive.core :as re]))

(def id :testID) ; because camelCase is a little ugly in Clojure :)

;; ----- Local state ----- ;;

(defn update-local-state
  ([db ident f]
    (update-in db [:local-state ident] f))
  ([db ident f & args]
    (apply update-in db [:local-state ident] f args)))

(defn >local-state [db ident] (-> db :local-state (get ident)))

#?(:cljs (re/reg-sub :local-state get-in))

#?(:cljs
(re/reg-event-db :local-state
  (fn [db msg] (assoc-in db (butlast msg) (last msg)))))

#?(:cljs
(re/reg-event-db ::gc-local-state false
  (fn [db [_ ident]] (update db :local-state dissoc ident))))

#?(:cljs
(defn with-local-state [component-f]
  (assert (fn? component-f))
  (-> (fn [& args]
        (let [ident         (cljs.core/random-uuid)
              component-ret (apply component-f ident args)]
          (assert (or (fn? component-ret) (react-class? component-ret)))
          (if (react-class? component-ret)
              (let [orig-component-will-unmount
                      (-> component-ret ($ :prototype) ($ :componentWillUnmount))]
                (doto component-ret
                  (-> ($ :prototype)
                      ($! :componentWillUnmount
                          (fn componentWillUnmount []
                            (this-as c
                              (when-not (nil? orig-component-will-unmount)
                                (.call orig-component-will-unmount c))
                              (re/event! [::gc-local-state ident])))))))
              (rx/create-class
                {:render component-ret
                 :component-will-unmount
                  (fn [_] (re/event-sync! [::gc-local-state ident]))}))))
      (with-meta (meta component-f))
      (doto ($! :name (.-name component-f))))))

;; ----- General components ----- ;;

#?(:cljs
(defn alert [title]
  (if (= usys/os "web")
      (js/alert title) ; totally stops everything
      (.alert (.-Alert react-native) title))))

#?(:cljs (def react-native-animatable (usys/>module nil ["react-native-animatable"])))
#?(:cljs (def adapt-animated
           (when-let [adapt-animated* (some-> react-native-animatable .-createAnimatableComponent)]
             (fn [react-component] (-> react-component adapt-animated* rx/adapt-react-class)))))

#?(:cljs (def text                (some-> react-native .-Text             rx/adapt-react-class)))
#?(:cljs (def text|animatable
           (if (= usys/os "web")
               (some-> react-native .-Animated .-Text rx/adapt-react-class)
               (some-> react-native-animatable .-Text rx/adapt-react-class))))

#?(:cljs (def view                (some-> react-native .-View             rx/adapt-react-class)))
#?(:cljs (def view|animatable
           (if (= usys/os "web")
               (some-> react-native .-Animated .-View rx/adapt-react-class)
               (some-> react-native-animatable .-View rx/adapt-react-class))))

#?(:cljs (def view|masked|ios
           (when-not (= usys/os "web")
             (some-> react-native .-MaskedViewIOS rx/adapt-react-class))))
#?(:cljs (def view|masked|animatable|ios
           (when-not (= usys/os "web")
             (when adapt-animated
               (some-> react-native .-MaskedViewIOS adapt-animated)))))

#?(:cljs (def view|keyboard-avoiding (some-> react-native .-KeyboardAvoidingView rx/adapt-react-class)))

#?(:cljs (def image               (some-> react-native .-Image           rx/adapt-react-class)))
#?(:cljs (def image|animatable
           (if (= usys/os "web")
               (some-> react-native .-Animated .-Image rx/adapt-react-class)
               (some-> react-native-animatable .-Image rx/adapt-react-class))))

#?(:cljs (def svg-enabled-image   (some-> (usys/>module nil ["react-native-remote-svg"]) .-default rx/adapt-react-class)))

; var CacheImage = require('@remobile/react-native-cache-image'); doesn't work on web ; better to have something else
#?(:cljs (def touchable           (some-> react-native .-TouchableWithoutFeedback rx/adapt-react-class)))
#?(:cljs (def touchable-highlight (some-> react-native .-TouchableHighlight       rx/adapt-react-class)))
#?(:cljs (def touchable-opacity   (some-> react-native .-TouchableOpacity         rx/adapt-react-class)))
#?(:cljs (def accordion           (when-not (= usys/os "web")
                                    (some-> (usys/>module nil ["react-native-accordion"]) rx/adapt-react-class))))
#?(:cljs (def text-input*         (some-> react-native .-TextInput rx/adapt-react-class)))

#?(:cljs
(def text-input
     (if (= usys/os "web")
         (fn text-input [props]
           (let [props' props
                 props' (case (:auto-complete props)
                          true  (assoc props' :auto-complete "on")
                          false (assoc props' :auto-complete "off")
                          props')
                 ;; a shim for one aspect of https://github.com/necolas/react-native-web/issues/501
                 props' (if (and (val? (:auto-correct props))
                                 (nil? (:spell-check props)))
                            (assoc props' :spell-check (:auto-correct props))
                            props')]
             [text-input* props']))
         text-input*)))

#?(:cljs (def modal               (when-not (= usys/os "web")
                                    (some-> react-native .-Modal rx/adapt-react-class))))
#?(:cljs (def scroll-view         (some-> react-native .-ScrollView rx/adapt-react-class)))
#?(:cljs (def list-view           (some-> react-native .-ListView   rx/adapt-react-class)))
#?(:cljs (def video               (if (= usys/os "web")
                                      :video
                                       ; https://github.com/react-native-community/react-native-video
                                      #_(rx/adapt-react-class (js/require "react-native-video")))))

#?(:cljs (def web-view            (some-> react-native .-WebView    rx/adapt-react-class)))

; Uses StreamingKit
; Supported codecs (list incomplete):
#_"mp4 audio (m4a)
   mp3
   aac
   wav"
#_"  seekToTime <double seconds >
   , goForward  <double seconds >
   , goBack     <double seconds >
   , getStatus  <fn     callback>"
; https://github.com/tlenclos/react-native-audio-streaming
#?(:cljs
(def Audio (when-not (= usys/os "web")
             (some-> (usys/>module nil ["react-native-audio-streaming"]) .-ReactNativeAudioStreaming))))

#?(:cljs (def audio (when (= usys/os "web") :audio)))

#?(:cljs (def list-view-data-source (some-> react-native .-ListView .-DataSource)))

#?(:cljs (def react-virtualized  (usys/>module nil ["react-virtualized"])))

#?(:cljs (def react-grid         (some-> react-virtualized .-Grid rx/adapt-react-class)))

#?(:cljs (def react-sortable-hoc (usys/>module nil ["react-sortable-hoc"])))

#?(:cljs (def sortable-container (some-> react-sortable-hoc .-SortableContainer rx/adapt-react-class)))

;; ----- Custom ----- ;;
