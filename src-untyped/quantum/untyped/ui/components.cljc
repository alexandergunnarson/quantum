(ns
  ^{:doc "Namespace for creating/defining UI components."
    :attribution "alexandergunnarson"}
  quantum.untyped.ui.components
  (:refer-clojure :exclude [for reduce])
  (:require
  #?@(:cljs
   [[reagent.core        :as rx]])
    [quantum.untyped.core.logic
      :refer [whenf]]
    [quantum.untyped.core.error  :as err]
    [quantum.untyped.core.log    :as log]
    [quantum.untyped.core.system :as sys
      :refer  [#?@(:cljs [ReactNative])]]
    [quantum.untyped.core.type.predicates
      :refer [val?]]))

#?(:cljs
(defn alert [title]
  (if (= sys/os "web")
      (js/alert title) ; totally stops everything
      (.alert (.-Alert ReactNative) title))))

#?(:cljs (defn rx-adapt [super sub]
           (when super
             (whenf (aget super sub) val? rx/adapt-react-class))))

#?(:cljs (def text                (rx-adapt ReactNative "Text" )))
#?(:cljs (def view                (rx-adapt ReactNative "View" )))
#?(:cljs (def image               (rx-adapt ReactNative "Image")))
; var CacheImage = require('@remobile/react-native-cache-image'); doesn't work on web ; better to have something else
#?(:cljs (def touchable-highlight (rx-adapt ReactNative "TouchableHighlight")))
#?(:cljs (def accordion           (when-not (= sys/os "web")
                                    (err/ignore (rx/adapt-react-class (js/require "react-native-accordion"))))))
#?(:cljs (def text-input          (rx-adapt ReactNative "TextInput")))
#?(:cljs (def modal               (when-not (= sys/os "web")
                                    (rx-adapt ReactNative "Modal"))))
#?(:cljs (def scroll-view         (rx-adapt ReactNative "ScrollView")))
#?(:cljs (def list-view           (rx-adapt ReactNative "ListView"  )))
#?(:cljs (def video               (if (= sys/os "web")
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
(def Audio (when-not (= sys/os "web")
             (err/ignore
               (.-ReactNativeAudioStreaming (js/require "react-native-audio-streaming"))))))

#?(:cljs (def audio (when (= sys/os "web") :audio)))

#?(:cljs (def list-view-data-source (err/ignore (-> ReactNative .-ListView .-DataSource))))
