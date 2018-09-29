(ns quantum.untyped.core.classes
        (:require
          [quantum.untyped.core.core :as ucore])
#?(:clj (:import
          [java.lang.reflect Modifier])))

#?(:clj (defn final?     [x] (and (class? x) (Modifier/isFinal (.getModifiers ^Class x)))))
#?(:clj (defn interface? [x] (and (class? x) (.isInterface ^Class x))))
#?(:clj (defn static?    [x] (and (class? x) (Modifier/isStatic (.getModifiers ^Class x)))))
#?(:clj (defn primitive? [x] (and (class? x) (.isPrimitive ^Class x))))
#?(:clj (defn array?     [x] (and (class? x) (.isArray     ^Class x))))

(defn protocol? [x]
  #?(:clj  (and (ucore/lookup? x) (-> x (get :on-interface) class?))
           ;; Unfortunately there's no better check in CLJS, at least as of 03/18/2018
     :cljs (and (fn? x) (= (str x) "function (){}"))))

#?(:clj (defn protocol>class [x] (:on-interface x)))
