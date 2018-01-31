(ns quantum.ui.style.css.dom
  (:require
    [quantum.untyped.core.vars
      :refer [defaliases]]
    [quantum.untyped.ui.style.css.dom :as u]))

#?(:cljs
(defaliases u add-link! append-css! replace-css-at!))
