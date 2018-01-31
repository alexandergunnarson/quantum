(ns quantum.untyped.ui.dom)

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
