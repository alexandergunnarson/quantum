(ns quantum.test.core.data.complex.xml
  (:require [quantum.core.data.complex.xml :refer :all]))

(defn test:parse
  ([s])
  ([s
    {:keys [start-doc start-elem chars end-elem end-doc unk-elem
            aggregate-props? keywordize-attrs? keywordize-names?]
     :as opts}]))

; ==== PLIST PARSING ====

(defn test:parse-plist [x])

(defn test:lparse [x])