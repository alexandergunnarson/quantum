(ns quantum.untyped.ui.style.css.dom
  (:require
    [clojure.string            :as str]
    [quantum.untyped.core.fn
      :refer [fn->]]
    [quantum.untyped.core.spec :as us]
    [quantum.untyped.core.type.predicates
      :refer [val?]]
    [quantum.untyped.ui.dom    :as udom]))

#?(:cljs
(defn add-link! [link #_href-string?]
  (let [elem (.createElement js/document "link")]
    (set! (.-href elem) link)
    (set! (.-rel  elem) "stylesheet")
    (set! (.-type elem) "text/css")
    (.appendChild (.-head js/document) elem)
    elem)))

#?(:cljs
(defn append-css! [css-str #_css-string?]
  "Inserts stylesheet into document head"
  {:from "https://github.com/facjure/gardener/dom"}
  (let [elem (.createElement  js/document "style")
        text (.createTextNode js/document css-str)]
    (.appendChild elem text)
    (.appendChild (.-head js/document) elem)
    elem)))

#?(:cljs
(defn replace-css-at! [id #_dom-id-string? css-str #_css-string?]
  "Replaces CSS at a (possibly generated) style node."
  (let [elem (udom/append-element!
               (or (some-> (.getElementById js/document id) .-parentNode)
                   (.-head js/document))
               "style"
               id)
        _ (us/validate elem (us/and val? (fn-> .-tagName str/lower-case (= "style"))))
        text (.createTextNode js/document css-str)]
    (while (.-firstChild elem)
      (.removeChild elem (.-firstChild elem)))

    (.appendChild elem text)
    elem)))
