(ns quantum.ui.style.fonts
  (:require
    [quantum.core.log :as log
      :include-macros true]))

(log/this-ns)

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

(def families
  {:garamond       {:pref ["'EB Garamond'"     "Baskerville" "Georgia"   "Times" "serif"     ]}
   :optima         {:pref ["'Optima'"          "Segoe"       "Calibri"   "Arial" "sans-serif"]}
   :firasans       {:pref ["'Fira Sans'"                     "Calibri"   "Arial" "sans-serif"]}
   :sourcecode-pro {:pref ["'Source Code Pro'"                                   "monospace" ]}
   :helvetica-neue {:pref ["'Helvetica Neue'"                "Helvetica" "Arial" "sans-serif"]}
   :lato           {:link "https://fonts.googleapis.com/css?family=Lato:100"
                    :pref ["'Lato'"                          "Helvetica" "Arial" "sans-serif"]}
   :open-sans      {:link "https://fonts.googleapis.com/css?family=Open+Sans:400,300,300italic,400italic,600,600italic,700,700italic"
                    :pref ["'Open Sans'"                     "Helvetica" "Arial" "sans-serif"]}
   :montserrat     {:link "https://fonts.googleapis.com/css?family=Montserrat:400,700"
                    :pref ["'Montserrat'"      "Gotham"      "Helvetica" "Arial" "sans-serif"]}})

(defn family [k] (get-in families [k :pref]))
(defn link   [k] (get-in families [k :link]))

(def fonts
  {:std {:std      "'Gotham Book'"
         :semibold "'Gotham Medium'"
         :light    "'Gotham Thin'"}
   :google #{"Pathway Gothic One" "Raleway" "Open Sans Condensed" "Fjalla One"}})

(defn font
  {:todo ["More arity"]}
  ([k]      (get-in fonts [k :std]))
  ([k & ks] (get-in fonts (apply vector k ks))))

; (defn load-font! [path-str]
;   (-> (str "file://" (io/path (:resources @io/dirs) "Fonts" path-str))
;       (Font/loadFont 12.0)))

; (defn font-loaded? [^String font-name]
;   (->> (Font/getFontNames)
;        (into #{})
;        (<- contains? font-name)))

; (defn font-or [font-0 font-alt]
;   (whenc font-0 (fn-not font-loaded?) font-alt))

; (defonce fonts
;   (do (log/pr :user
;         (str "Loading fonts from "
;           (io/path (:resources @io/dirs) "Fonts") "..."))
;       (load-font! "Myriad Pro/Light.ttf")
;       (load-font! "Myriad Pro/Regular.ttf")
;       (load-font! "Myriad Pro/Semibold.ttf")
;       (load-font! "Myriad Pro/LightSemiExt.ttf")
;       (load-font! "Arno Pro/Regular.otf") 
;       (load-font! "Arno Pro/Bold.otf")
;       (load-font! "Arno Pro/Italic.otf")
;       (load-font! "Gotham/Regular/Bold.otf") 
;       (load-font! "Gotham/Regular/Medium.otf")
;       (load-font! "Gotham/Regular/Book.otf")
;       (load-font! "Gotham/Regular/Light.otf")
;       (load-font! "Gotham/Regular/XLight.otf")
;       (atom
;         {:myriad {:lt 
;                     (font-or  "MyriadPro-Light"        "Myriad Pro Light"             )
;                   :reg
;                     (font-or  "MyriadPro-Regular"      "Myriad Pro"                   )
;                   :semibold
;                     (font-or  "MyriadPro-Semibold"     "Myriad Pro Semibold"          )
;                   :lt-semi-ext
;                     (font-or  "MyriadPro-LightSemiExt" "Myriad Pro Light SemiExtended")}
;          :arno   {:reg  (font-or "ArnoPro-Regular"   "Arno Pro"       )
;                   :bold (font-or "ArnoPro-Bold"      "Arno Pro Bold"  )
;                   :ital (font-or "ArnoPro-Italic"    "Arno Pro Italic")}
;          :gotham {:bold (font-or "Gotham-Bold"       "Gotham Bold"    )
;                   :med  (font-or "Gotham-Medium"     "Gotham Medium"  )
;                   :reg  (font-or "Gotham-Book"       "Gotham Book"    )
;                   :lt   (font-or "Gotham-Light"      "Gotham Light"   )
;                   :xlt  (font-or "Gotham-ExtraLight" "Gotham Thin"    )}})))
