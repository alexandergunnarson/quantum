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
    [quantum.untyped.core.system :as usys
      :refer  [#?@(:cljs [ReactNative])]]
    [quantum.untyped.core.type.predicates
      :refer [val?]]))

#?(:cljs
(defn alert [title]
  (if (= usys/os "web")
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
#?(:cljs (def touchable           (rx-adapt ReactNative "TouchableOpacity"  )))
#?(:cljs (def accordion           (when-not (= usys/os "web")
                                    (err/ignore (rx/adapt-react-class (js/require "react-native-accordion"))))))
#?(:cljs (def text-input*         (rx-adapt ReactNative "TextInput")))

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
                                    (rx-adapt ReactNative "Modal"))))
#?(:cljs (def scroll-view         (rx-adapt ReactNative "ScrollView")))
#?(:cljs (def list-view           (rx-adapt ReactNative "ListView"  )))
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
             (err/ignore
               (.-ReactNativeAudioStreaming (js/require "react-native-audio-streaming"))))))

#?(:cljs (def audio (when (= usys/os "web") :audio)))

#?(:cljs (def list-view-data-source (err/ignore (-> ReactNative .-ListView .-DataSource))))

#?(:cljs (def react-virtualized (usys/>module nil ["react-virtualized"])))

#?(:cljs (def react-grid (rx-adapt react-virtualized "Grid")))

#?(:cljs (def react-sortable-hoc (usys/>module nil ["react-sortable-hoc"])))

#?(:Cljs (def sortable-container (rx/adapt react-sortable-hoc "SortableContainer")))
