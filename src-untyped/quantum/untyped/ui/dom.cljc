(ns quantum.untyped.ui.dom
  #?(:cljs (:require [re-frame.core :as re])))

#?(:cljs
(defn append-element! [parent #_dom-element? tag #_t/string? id #_t/string?]
  (or (.getElementById js/document id)
      (doto (.createElement js/document tag)
            (-> .-id (set! id))
            (->> (.appendChild parent))))))

#?(:cljs
(defn replace-element! [parent tag id]
  (when-let [node (.getElementById js/document id)]
    (.removeChild parent node))
  (append-element! parent tag id)))

#?(:cljs
(defn viewport-w []
  (-> js/document .-documentElement .-clientWidth)))

#?(:cljs
(defn viewport-h []
  (-> js/document .-documentElement .-clientHeight)))

#?(:cljs
(defn visual-length [text]
  (let [elem (.createElement js/document "span")]
    (set! (.-innerHTML elem) text)
    (.-clientWidth elem))))

#?(:cljs
(re/reg-fx :dom/prevent-default
  (fn [e] (.preventDefault e))))

#?(:cljs
(re/reg-fx :dom/focus
  (fn [dom-node] (.focus dom-node))))

#?(:cljs
(re/reg-fx :dom/unfocus
  (fn [dom-node] (.blur dom-node))))
