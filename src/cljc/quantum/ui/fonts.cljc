(ns quantum.ui.fonts
  (:require-quantum [:lib])
  (:require [garden.core :refer [css]])
  (:import javafx.scene.text.Font))

(defn load-font! [path-str]
  (-> (str "file://" (io/path (:resources @io/dirs) "Fonts" path-str))
      (Font/loadFont 12.0)))

(defn font-loaded? [^String font-name]
  (->> (Font/getFontNames)
       (into #{})
       (<- contains? font-name)))

(defn font-or [font-0 font-alt]
  (whenc font-0 (fn-not font-loaded?) font-alt))

(defonce fonts
  (do (log/pr :user
        (str "Loading fonts from "
          (io/path (:resources @io/dirs) "Fonts") "..."))
      (load-font! "Myriad Pro/Light.ttf")
      (load-font! "Myriad Pro/Regular.ttf")
      (load-font! "Myriad Pro/Semibold.ttf")
      (load-font! "Myriad Pro/LightSemiExt.ttf")
      (load-font! "Arno Pro/Regular.otf") 
      (load-font! "Arno Pro/Bold.otf")
      (load-font! "Arno Pro/Italic.otf")
      (load-font! "Gotham/Regular/Bold.otf") 
      (load-font! "Gotham/Regular/Medium.otf")
      (load-font! "Gotham/Regular/Book.otf")
      (load-font! "Gotham/Regular/Light.otf")
      (load-font! "Gotham/Regular/XLight.otf")
      (atom
        {:myriad {:lt 
                    (font-or  "MyriadPro-Light"        "Myriad Pro Light"             )
                  :reg
                    (font-or  "MyriadPro-Regular"      "Myriad Pro"                   )
                  :semibold
                    (font-or  "MyriadPro-Semibold"     "Myriad Pro Semibold"          )
                  :lt-semi-ext
                    (font-or  "MyriadPro-LightSemiExt" "Myriad Pro Light SemiExtended")}
         :arno   {:reg  (font-or "ArnoPro-Regular"   "Arno Pro"       )
                  :bold (font-or "ArnoPro-Bold"      "Arno Pro Bold"  )
                  :ital (font-or "ArnoPro-Italic"    "Arno Pro Italic")}
         :gotham {:bold (font-or "Gotham-Bold"       "Gotham Bold"    )
                  :med  (font-or "Gotham-Medium"     "Gotham Medium"  )
                  :reg  (font-or "Gotham-Book"       "Gotham Book"    )
                  :lt   (font-or "Gotham-Light"      "Gotham Light"   )
                  :xlt  (font-or "Gotham-ExtraLight" "Gotham Thin"    )}})))

