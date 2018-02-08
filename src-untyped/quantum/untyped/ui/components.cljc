(ns
  ^{:doc "Namespace for creating/defining UI components."
    :attribution "alexandergunnarson"}
  quantum.untyped.ui.components
  (:refer-clojure :exclude [for reduce])
  (:require
  #?@(:cljs
   [[reagent.core        :as rx]])
    [quantum.untyped.core.log    :as log]
    [quantum.untyped.core.system :as usys
      :refer  [#?@(:cljs [ReactNative])]]
    [quantum.untyped.core.type.predicates
      :refer [val?]]))

#?(:cljs
(defn alert [title]
  (if (= usys/os "web")
      (js/alert title) ; totally stops everything
      (.alert (.-Alert ReactNative) title))))

#?(:cljs (def react-native-animatable (usys/>module nil ["react-native-animatable"])))

#?(:cljs (def text                (some-> ReactNative .-Text             rx/adapt-react-class)))
#?(:cljs (def text|animated
           (if (= usys/os "web")
               (some-> ReactNative .-Animated  .-Text rx/adapt-react-class)
               (some-> react-native-animatable .-Text rx/adapt-react-class))))

#?(:cljs (def view                (some-> ReactNative .-View             rx/adapt-react-class)))
#?(:cljs (def view|animated
           (if (= usys/os "web")
               (some-> ReactNative .-Animated  .-View rx/adapt-react-class)
               (some-> react-native-animatable .-View rx/adapt-react-class))))

#?(:cljs (def image               (some-> ReactNative .-Image           rx/adapt-react-class)))
#?(:cljs (def image|animated
           (if (= usys/os "web")
               (some-> ReactNative .-Animated  .-Image rx/adapt-react-class)
               (some-> react-native-animatable .-Image rx/adapt-react-class))))

#?(:cljs (def svg-enabled-image   (some-> (usys/>module nil ["react-native-remote-svg"]) .-default rx/adapt-react-class)))

; var CacheImage = require('@remobile/react-native-cache-image'); doesn't work on web ; better to have something else
#?(:cljs (def touchable-highlight (some-> ReactNative .-TouchableHighlight rx/adapt-react-class)))
#?(:cljs (def touchable-opacity   (some-> ReactNative .-TouchableOpacity   rx/adapt-react-class)))
#?(:cljs (def accordion           (when-not (= usys/os "web")
                                    (some-> (usys/>module nil ["react-native-accordion"]) rx/adapt-react-class))))
#?(:cljs (def text-input*         (some-> ReactNative .-TextInput rx/adapt-react-class)))

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
                                    (some-> ReactNative .-Modal rx/adapt-react-class))))
#?(:cljs (def scroll-view         (some-> ReactNative .-ScrollView rx/adapt-react-class)))
#?(:cljs (def list-view           (some-> ReactNative .-ListView   rx/adapt-react-class)))
#?(:cljs (def video               (if (= usys/os "web")
                                      :video
                                       ; https://github.com/react-native-community/react-native-video
                                      #_(rx/adapt-react-class (js/require "react-native-video")))))

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

#?(:cljs (def list-view-data-source (some-> ReactNative .-ListView .-DataSource)))

#?(:cljs (def react-virtualized  (usys/>module nil ["react-virtualized"])))

#?(:cljs (def react-grid         (some-> react-virtualized .-Grid rx/adapt-react-class)))

#?(:cljs (def react-sortable-hoc (usys/>module nil ["react-sortable-hoc"])))

#?(:cljs (def sortable-container (some-> react-sortable-hoc .-SortableContainer rx/adapt-react-class)))
