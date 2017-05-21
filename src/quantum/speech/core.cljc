(ns quantum.speech.core
  (:require
    [quantum.core.error :as err 
      :refer [TODO]]
    [quantum.apis.google.speech]))

(defn text->speech [text]
  ; https://github.com/naoufal/react-native-speech
  (TODO))

(defn speech->text [speech] (TODO))