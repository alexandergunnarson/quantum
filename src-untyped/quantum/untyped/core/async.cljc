(ns quantum.untyped.core.async
  (:require
    [quantum.untyped.core.system :as usys]))

#?(:cljs
(def at-next-tick
  (or (.-requestAnimationFrame       usys/global)
      (.-webkitRequestAnimationFrame usys/global)
      (.-mozRequestAnimationFrame    usys/global)
      (.-msRequestAnimationFrame     usys/global)
      (.-oRequestAnimationFrame      usys/global)
      (let [t0 (.getTime (js/Date.))]
        (fn [f]
          (js/setTimeout
           #(f (- (.getTime (js/Date.)) t0))
           16.66666))))))
