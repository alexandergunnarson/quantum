(ns quantum.test.ui.style.css.core
  (:require [quantum.ui.style.css.core :refer :all]))

; ======== HELPER FUNCTIONS ========

(defn ^String test:px [n])

(defn ^String test:rgb
  [r g b])

(defn ^String test:url
  [s])

(defn ^String test:hex [s])

(defn ^String test:css-block-str
  [css-block])

(defn ^String test:css-prop-str
  [css-block])

(defn test:normalize-prop-v [x])

(defn test:compatibilize
  [style-map])

(defn test:flattened-style
  [css-block])

; ======== CSS GENERATION FROM CLASSES ========

(defn test:full-css-for-tag
  [styles-calc tag])

(defn test:fill-in-css-from-tag!
  [tag])

(defn test:add-css-media-style!
  [])

(defn test:to-css-str [css-map])

(defn test:trans
  [id & props])
