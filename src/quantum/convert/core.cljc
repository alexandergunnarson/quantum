(ns quantum.convert.core
  (:refer-clojure :exclude [transduce])
  (:require
    [quantum.core.collections :as coll
      :refer [transduce map+ join]]
    [quantum.core.macros
      :refer [defnt]]
    [quantum.core.reflect     :as refl])
  (:import
    [javax.imageio.metadata IIOAttr IIOMetadataNode]))

;; TODO MOVE
(defnt ->hiccup [^IIOMetadataNode x]
  ; .getPrefix
  ; .getLocalName
  ; .getBaseURI
  ; .getNodeName
  ; .getTextContent
  (apply vector
    (-> x .getTagName keyword)
    (->> (-> x .getAttributes (refl/get-field "nodes"))
         (map+ (fn [^IIOAttr attr]
                 [(.getName attr) (.getValue attr)]))
         (join {}))
    (transduce
      (fn ([] (transient []))
          ([ret] (persistent! ret))
          ([!children ^long i]
            (if-let [child (-> x .getChildNodes (.item i))]
              (conj! !children (->hiccup child))
              (reduced !children))))
      (range))))
