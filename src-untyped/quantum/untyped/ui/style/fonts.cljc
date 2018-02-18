(ns quantum.untyped.ui.style.fonts
  (:require
    [quantum.untyped.core.core        :as ucore]
    [quantum.untyped.core.error
      :refer [err! TODO]]
    [quantum.untyped.core.system      :as usys]
    [quantum.untyped.core.type.predicates
      #?@(:cljs [:refer [defined?]])]
    [quantum.untyped.ui.style.css     :as ucss]
    [quantum.untyped.ui.style.css.dom :as ucss-dom]))

(ucore/log-this-ns)

(def ^{:from "http://stackoverflow.com/questions/33320664/which-fonts-are-available-in-ios-9"}
  ios-default-fonts
  #{"AcademyEngravedLetPlain"
    "AlNile"
    "AlNile-Bold"
    "AmericanTypewriter"
    "AmericanTypewriter-Bold"
    "AmericanTypewriter-Condensed"
    "AmericanTypewriter-CondensedBold"
    "AmericanTypewriter-CondensedLight"
    "AmericanTypewriter-Light"
    "AppleColorEmoji"
    "AppleSDGothicNeo-Bold"
    "AppleSDGothicNeo-Light"
    "AppleSDGothicNeo-Medium"
    "AppleSDGothicNeo-Regular"
    "AppleSDGothicNeo-SemiBold"
    "AppleSDGothicNeo-Thin"
    "AppleSDGothicNeo-UltraLight"
    "Arial-BoldItalicMT"
    "Arial-BoldMT"
    "Arial-ItalicMT"
    "ArialHebrew"
    "ArialHebrew-Bold"
    "ArialHebrew-Light"
    "ArialMT"
    "ArialRoundedMTBold"
    "Avenir-Black"
    "Avenir-BlackOblique"
    "Avenir-Book"
    "Avenir-BookOblique"
    "Avenir-Heavy"
    "Avenir-HeavyOblique"
    "Avenir-Light"
    "Avenir-LightOblique"
    "Avenir-Medium"
    "Avenir-MediumOblique"
    "Avenir-Oblique"
    "Avenir-Roman"
    "AvenirNext-Bold"
    "AvenirNext-BoldItalic"
    "AvenirNext-DemiBold"
    "AvenirNext-DemiBoldItalic"
    "AvenirNext-Heavy"
    "AvenirNext-HeavyItalic"
    "AvenirNext-Italic"
    "AvenirNext-Medium"
    "AvenirNext-MediumItalic"
    "AvenirNext-Regular"
    "AvenirNext-UltraLight"
    "AvenirNext-UltraLightItalic"
    "AvenirNextCondensed-Bold"
    "AvenirNextCondensed-BoldItalic"
    "AvenirNextCondensed-DemiBold"
    "AvenirNextCondensed-DemiBoldItalic"
    "AvenirNextCondensed-Heavy"
    "AvenirNextCondensed-HeavyItalic"
    "AvenirNextCondensed-Italic"
    "AvenirNextCondensed-Medium"
    "AvenirNextCondensed-MediumItalic"
    "AvenirNextCondensed-Regular"
    "AvenirNextCondensed-UltraLight"
    "AvenirNextCondensed-UltraLightItalic"
    "Baskerville"
    "Baskerville-Bold"
    "Baskerville-BoldItalic"
    "Baskerville-Italic"
    "Baskerville-SemiBold"
    "Baskerville-SemiBoldItalic"
    "BodoniOrnamentsITCTT"
    "BodoniSvtyTwoITCTT-Bold"
    "BodoniSvtyTwoITCTT-Book"
    "BodoniSvtyTwoITCTT-BookIta"
    "BodoniSvtyTwoOSITCTT-Bold"
    "BodoniSvtyTwoOSITCTT-Book"
    "BodoniSvtyTwoOSITCTT-BookIt"
    "BodoniSvtyTwoSCITCTT-Book"
    "BradleyHandITCTT-Bold"
    "ChalkboardSE-Bold"
    "ChalkboardSE-Light"
    "ChalkboardSE-Regular"
    "Chalkduster"
    "Cochin"
    "Cochin-Bold"
    "Cochin-BoldItalic"
    "Cochin-Italic"
    "Copperplate"
    "Copperplate-Bold"
    "Copperplate-Light"
    "Courier"
    "Courier-Bold"
    "Courier-BoldOblique"
    "Courier-Oblique"
    "CourierNewPS-BoldItalicMT"
    "CourierNewPS-BoldMT"
    "CourierNewPS-ItalicMT"
    "CourierNewPSMT"
    "Damascus"
    "DamascusBold"
    "DamascusLight"
    "DamascusMedium"
    "DamascusSemiBold"
    "DevanagariSangamMN"
    "DevanagariSangamMN-Bold"
    "Didot"
    "Didot-Bold"
    "Didot-Italic"
    "DiwanMishafi"
    "EuphemiaUCAS"
    "EuphemiaUCAS-Bold"
    "EuphemiaUCAS-Italic"
    "Farah"
    "Futura-CondensedExtraBold"
    "Futura-CondensedMedium"
    "Futura-Medium"
    "Futura-MediumItalic"
    "GeezaPro"
    "GeezaPro-Bold"
    "Georgia"
    "Georgia-Bold"
    "Georgia-BoldItalic"
    "Georgia-Italic"
    "GillSans"
    "GillSans-Bold"
    "GillSans-BoldItalic"
    "GillSans-Italic"
    "GillSans-Light"
    "GillSans-LightItalic"
    "GillSans-SemiBold"
    "GillSans-SemiBoldItalic"
    "GillSans-UltraBold"
    "GujaratiSangamMN"
    "GujaratiSangamMN-Bold"
    "GurmukhiMN"
    "GurmukhiMN-Bold"
    "Helvetica"
    "Helvetica-Bold"
    "Helvetica-BoldOblique"
    "Helvetica-Light"
    "Helvetica-LightOblique"
    "Helvetica-Oblique"
    "HelveticaNeue"
    "HelveticaNeue-Bold"
    "HelveticaNeue-BoldItalic"
    "HelveticaNeue-CondensedBlack"
    "HelveticaNeue-CondensedBold"
    "HelveticaNeue-Italic"
    "HelveticaNeue-Light"
    "HelveticaNeue-LightItalic"
    "HelveticaNeue-Medium"
    "HelveticaNeue-MediumItalic"
    "HelveticaNeue-Thin"
    "HelveticaNeue-ThinItalic"
    "HelveticaNeue-UltraLight"
    "HelveticaNeue-UltraLightItalic"
    "HiraginoSans-W3"
    "HiraginoSans-W6"
    "HiraMinProN-W3"
    "HiraMinProN-W6"
    "HoeflerText-Black"
    "HoeflerText-BlackItalic"
    "HoeflerText-Italic"
    "HoeflerText-Regular"
    "IowanOldStyle-Bold"
    "IowanOldStyle-BoldItalic"
    "IowanOldStyle-Italic"
    "IowanOldStyle-Roman"
    "Kailasa"
    "Kailasa-Bold"
    "KannadaSangamMN"
    "KannadaSangamMN-Bold"
    "KhmerSangamMN"
    "KohinoorBangla-Light"
    "KohinoorBangla-Regular"
    "KohinoorBangla-Semibold"
    "KohinoorDevanagari-Light"
    "KohinoorDevanagari-Regular"
    "KohinoorDevanagari-Semibold"
    "KohinoorTelugu-Light"
    "KohinoorTelugu-Medium"
    "KohinoorTelugu-Regular"
    "LaoSangamMN"
    "MalayalamSangamMN"
    "MalayalamSangamMN-Bold"
    "MarkerFelt-Thin"
    "MarkerFelt-Wide"
    "Menlo-Bold"
    "Menlo-BoldItalic"
    "Menlo-Italic"
    "Menlo-Regular"
    "Noteworthy-Bold"
    "Noteworthy-Light"
    "Optima-Bold"
    "Optima-BoldItalic"
    "Optima-ExtraBlack"
    "Optima-Italic"
    "Optima-Regular"
    "OriyaSangamMN"
    "OriyaSangamMN-Bold"
    "Palatino-Bold"
    "Palatino-BoldItalic"
    "Palatino-Italic"
    "Palatino-Roman"
    "Papyrus"
    "Papyrus-Condensed"
    "PartyLetPlain"
    "PingFangHK-Light"
    "PingFangHK-Medium"
    "PingFangHK-Regular"
    "PingFangHK-Semibold"
    "PingFangHK-Thin"
    "PingFangHK-Ultralight"
    "PingFangSC-Light"
    "PingFangSC-Medium"
    "PingFangSC-Regular"
    "PingFangSC-Semibold"
    "PingFangSC-Thin"
    "PingFangSC-Ultralight"
    "PingFangTC-Light"
    "PingFangTC-Medium"
    "PingFangTC-Regular"
    "PingFangTC-Semibold"
    "PingFangTC-Thin"
    "PingFangTC-Ultralight"
    "SavoyeLetPlain"
    "SinhalaSangamMN"
    "SinhalaSangamMN-Bold"
    "SnellRoundhand"
    "SnellRoundhand-Black"
    "SnellRoundhand-Bold"
    "Symbol"
    "TamilSangamMN"
    "TamilSangamMN-Bold"
    "Thonburi"
    "Thonburi-Bold"
    "Thonburi-Light"
    "TimesNewRomanPS-BoldItalicMT"
    "TimesNewRomanPS-BoldMT"
    "TimesNewRomanPS-ItalicMT"
    "TimesNewRomanPSMT"
    "Trebuchet-BoldItalic"
    "TrebuchetMS"
    "TrebuchetMS-Bold"
    "TrebuchetMS-Italic"
    "Verdana"
    "Verdana-Bold"
    "Verdana-BoldItalic"
    "Verdana-Italic"
    "ZapfDingbatsITC"
    "Zapfino"})

(def fonts
  {:circular
     {:light           "CircularStd-Book"
      :medium          "CircularStd-Book"
      :medium-contrast "CircularStd-Black"
      :semibold        "CircularStd-Medium"}
   :metropolis
     {:light           "Metropolis-Light"
      :regular         "Metropolis-Regular"
      :medium          "Metropolis-Medium"
      :medium-contrast "Metropolis-Medium"
      :semibold        "Metropolis-SemiBold"}})

(defn >font [family-name weight-name] (-> fonts (get family-name) (get weight-name)))

#?(:cljs
(def ^{:doc "`FontFace` is a newer technology (no IE/Edge/Android, Chrome (2013), Safari
             incl. iOS (late 2016))"}
  supports-dynamic-font-face? (defined? (.-FontFace usys/global))))

#?(:cljs
(defn load-font-by-css-src!
  "Warning: CSS injection possible with this function."
  [font-name css-src]
  (if-let [>font-face (.-FontFace usys/global)]
    ;; The third argument for the FontFace constructor is supposed to be optional
    ;; but some browsers (Chrome 35 and 36, Opera 22 and 23) throw an error if omitted.
    (doto (new >font-face font-name css-src #js{}) (.load))
    ;; TODO assumes that `@font-face` is supported
    (ucss-dom/append-css!
      (str "@font-face { font-family: '" font-name "'; src: " css-src "}")))))

;; TODO https://stackoverflow.com/questions/33638879/detect-woff-support-with-javascript
;; Most browsers do support it but still
#?(:cljs (def supports-woff? true))

;; TODO just need to add in `load-font-by-url!`
#_(:cljs
(def
  ^{:doc          "WOFF2 is a newer technology (no IE, Edge (late 2016), Firefox (2015),
                   Chrome (2014), Safari (late 2016) post-El-Capitan, iOS (2016), no Android)"
    :adapted-from "https://www.filamentgroup.com/lab/font-loading.html"}
  supports-woff2?
  (and supports-dynamic-font-face?
       (-> (load-font-by-url! "t" "url(data:application/font-woff2,) format(woff2)")
           .-status
           (= "loading")))))

;; TODO just need `supports-woff2?`
#_(:cljs
(defn load-font!
  ([font-name] (TODO "Load from LocalStorage"))
  ([font-name css-src-coercible] (load-font-by-css-src! font-name (ucss/>css-src css-src-coercible)))
  ([font-name, mime-type #_keyword?, base64-string]
    (if (and (= mime-type :application/font-woff2)
             (not supports-woff2?))
        (err! "WOFF2 format not supported")
        (let [css-src (str "url(data:" (-> mime-type >symbol str) ";charset=utf-8;base64," base64-string ")")] ; TODO append e.g. ' format(woff2)'?
          (load-font-by-css-src! font-name css-src))))))
