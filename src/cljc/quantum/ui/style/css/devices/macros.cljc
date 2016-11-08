(ns quantum.ui.style.css.devices.macros)

#?(:clj
(defmacro defbreakpoint [name media-params]
  `(defn ~name [& rules#]
     (garden.stylesheet/at-media ~media-params [:& rules#]))))
