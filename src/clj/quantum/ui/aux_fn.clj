(ns
  ^{:doc "Some old auxiliary functions for JavaFX.
          Many, if not most, of these will be deprecated in favor of
          aaronc/freactive's reactive UI solution."
    :attribution "Alex Gunnarson"}
  quantum.ui.aux-fn
  (:gen-class))
(require '[quantum.core.ns :as ns])
(ns/require-all *ns* :lib :clj :grid)
(ns/nss *ns*)

; ;___________________________________________________________________________________________________________________________________
; ;=========================================================={   FUNCTIONS  }=========================================================
; ;=========================================================={              }=========================================================
; (defn ^Set complement-nodes
;   [^Node obj]
;   (let [^Keyword obj-key (get @fx/obj-key-pairs obj)]
;     (->> @fx/complement-nodes-set
;          (ffilter (f*n contains? obj-key))
;          (<- set/difference (hash-set obj-key)))))

; (defn ^Vec siblings 
;   {:todo ["Take this function away and incorporate into |jfx/jget|."]}
;   [^Node obj]
;   (->> (jget (jget obj :parent) :children)
;        (remove+ (eq? obj))
;        fold+))
; ;___________________________________________________________________________________________________________________________________
; ;======================================================{         FONTS        }=====================================================
; ;======================================================{                      }=====================================================
; (defn load-font! [path-str]
;   (-> (str "file://" (io/path (:resources io/dirs) "Fonts" path-str))
;       (Font/loadFont 12.0)))
; (defn font-loaded? [font-name]
;   (->> (Font/getFontNames)
;        (into+ [])
;        (<- index-of+ font-name)
;        (not= -1)))
; (defn font-or [font-0 font-alt]
;   (whenc font-0 (fn-not font-loaded?) font-alt))
; (defonce fonts
;   (do (println
;         (str "Loading fonts from "
;           (io/path (:resources io/dirs) "Fonts") "..."))
;       (load-font! "Myriad Pro/Light.otf")
;       (load-font! "Myriad Pro/Regular.otf")
;       (load-font! "Myriad Pro/Semibold.otf")
;       (load-font! "Myriad Pro/LightSemiExt.otf")
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
;                     (font-or "Myriad Pro Light"
;                              "MyriadPro-Light")
;                   :reg
;                     (font-or "Myriad Pro"
;                              "MyriadPro-Regular")
;                   :semibold
;                     (font-or "Myriad Pro Semibold"
;                              "MyriadPro-Semibold")
;                   :lt-semi-ext
;                     (font-or "Myriad Pro Light SemiExtended"
;                              "MyriadPro-LightSemiExt")}
;          :arno   {:reg  (font-or "Arno Pro"        "ArnoPro-Regular")
;                   :bold (font-or "Arno Pro Bold"   "ArnoPro-Bold")
;                   :ital (font-or "Arno Pro Italic" "ArnoPro-Italic")}
;          :gotham {:bold (font-or "Gotham Bold"     "Gotham-Bold")
;                   :med  (font-or "Gotham Medium"   "Gotham-Medium")
;                   :reg  (font-or "Gotham Book"     "Gotham-Book")
;                   :lt   (font-or "Gotham Light"    "Gotham-Light")
;                   :xlt  (font-or "Gotham Thin"     "Gotham-ExtraLight")}})))
; ;___________________________________________________________________________________________________________________________________
; ;======================================================{    SERIALIZATION     }=====================================================
; ;======================================================{                      }=====================================================
; (defprotocol JavaFXSerialize
;   (serialize [obj]))
; (extend-protocol JavaFXSerialize ; draggable?
;   Rectangle
;   (serialize [obj]
;     (jgets-map obj :class :x :y :fill :width :height))
;   Text
;   (serialize [obj]
;     (jgets-map obj :class :x :y :text :font))
;   Color
;   (serialize [obj]
;     (jgets-map obj :class :red :green :blue))
;   javafx.scene.control.ColorPicker
;   (serialize [obj]
;     (jgets-map obj :class :value))
;   Font
;   (serialize [obj]
;     (jgets-map obj :class :name :size))
;   nil
;   (serialize [obj] obj) 
;   clojure.lang.IPersistentMap
;   (serialize [obj]
;     (while-recur obj
;       (compr (partial coll/tree-filter fx-obj? identity) nempty?)
;       (partial postwalk (whenf*n fx-obj? serialize)))))

; ; (defn unserialize! [m-0]
; ;   (defn unserialize* [m]
; ;     (reduce+
; ;       (fn [ret k v]
; ;         (cond
; ;           ((fn-and map? (f*n contains? :class)) v) ; then make it
; ;           `(jdef     ~k ~(unserialize* v))
; ;           ((fn-and map? empty?) v)
; ;           nil ; ?
; ;           (map? v)
; ;           `(conj-fx! ~k ~(unserialize* v)) ; add it
; ;           (= :class k)
; ;           (merge ret
; ;             [k (-> v str 
; ;                    (#(str/subs+ % (inc (coll/last-index-of "." %))))
; ;                    str/un-camelcase)]) ; make this into a normal function - to get the clojurized name of a Java class - used in q.lib.ui.jfx as well
; ;           :else (merge ret [k v])))
; ;       {}
; ;       m))
; ;   (unserialize* m-0))

; (defn save-stage! []
;   (io/write! :directory [:resources] :name "stg" :data (serialize @fx/tree)))
; (defn load-stage! []
;   (io/read   :directory [:resources] :name "stg"))
; ;___________________________________________________________________________________________________________________________________
; ;=========================================================={     COLOR    }=========================================================
; ;=========================================================={              }=========================================================
; (defn css-color [^Color color]
;   (-> color str (str/replace "0x" "#")))
; (defn to-rgb [color-0]
;   ; (->> color-0 serialize vals
;   ;      (map+ (fn-> (* 255) int)) fold+)
;  (map (fn->> (* 255) int) (jget color-0 :red :green :blue)))
; (defn to-back!  [obj] (do-fx (.toBack  obj)))
; (defn to-front! [obj] (do-fx (.toFront obj)))
; (defn set-font-size! [obj size]
;   (jset! obj :font (Font. (-> obj (jget :font) (jget :name)) size)))
; (defn color [r g b & [trans]]
;   (Color. (/ r 255) (/ g 255) (/ b 255)
;     (if (nnil? trans) (/ trans 100) 1.0)))
; (defn color* [] (jget :color-picker :value))
; (defn color! [] (to-rgb (color*)))
; (def  colors
;   (atom {:black    (color 0 0 0)
;          :white    (color 255 255 255)
;          :red
;            {:rich (color 173 44 44)
;             :lt   (color 0 0 0)}
;          :green
;            {:med   (color 51 102 51)
;             :lt    (color 77 128 77)
;             :clear (color 128 179 128)
;             :pale  (color 179 230 179)}
;          :trans (color 0 0 0 0)}))
; ;___________________________________________________________________________________________________________________________________
; ;=========================================================={   FUNCTIONS  }=========================================================
; ;=========================================================={              }=========================================================
; (defn do-intervals [millis & args]
;   (->> args
;        (interpose #(Thread/sleep millis))
;        (map+ fold+)
;        fold+
;        pr/suppress))
; (defn do-every [millis n func]
;   (dotimes [_ n] (func) (Thread/sleep millis)))
; ;___________________________________________________________________________________________________________________________________
; ;======================================================{   DRAGGABLE ITEMS    }=====================================================
; ;======================================================{                      }=====================================================
; (defmacro in-fx [expr]
;   `(do (in-ns 'quantum.core.ui.jfx)
;        ~expr
;        (in-ns 'quantum.core.ui.experimental)))
; (defn handler-event [& fns]
;   (proxy [EventHandler] []
;      (handle [event]
;        (doall (map (*fn event) fns))
;        (.consume event))))
; (defmacro wrap-handler
;   ([^Object obj-to-affect ^AFunction handler]
;     `(wrap-handler ~obj-to-affect identity ~handler))
;   ([^Object obj-to-affect ^AFunction pre-handler ^AFunction handler]
;   `(let [ns-0# ~*ns*] 
;      (proxy [EventHandler] []
;        (handle [event#]
;          (binding [*ns* ns-0#]
;            (~pre-handler ~obj-to-affect)
;            (~handler ~obj-to-affect)
;            (.consume event#)))))))
; (def last-mouse-event (atom 0))
; (def mouse-inset (atom [0 0])) ; if you try to use the is! and * method, you will overwrite
; ;each thing, because referring to name is calling the original def and resetting it.
; ;So, is! with normal, and everything else with @
; (defn mouse-inset-x []
;   (get @mouse-inset 0))
; (defn mouse-inset-y []
;   (get @mouse-inset 1))
; (defn set-mouse-inset! [mouse-x mouse-y obj-x obj-y]
;   (reset! mouse-inset
;     [(- mouse-x obj-x)
;      (- mouse-y obj-y)]))
; (defn mouse-event-info [event obj]
;   (println "MOUSE X:"  (jget event  :scene-x)
;            "Y:"        (jget event  :scene-y))
;   (println "OBJECT X:" (jget obj :x)
;            "Y:"        (jget obj :y))
;   (println "INSET X:"  (mouse-inset-x)
;            "Y:"        (mouse-inset-y)))
; (defn released-handler [obj]
;   (fn [event] (reset! last-mouse-event (. MouseEvent MOUSE_RELEASED))))
; (defn pressed-handler [obj]
;   (fn [event]
;     (do (set-mouse-inset!
;           (jget event :scene-x)
;           (jget event :scene-y)
;           (jget obj :x)
;           (jget obj :y))
;         (println (str "Pressed " (fx/get-tree-key obj) "."))
;         (reset! last-mouse-event (. MouseEvent MOUSE_PRESSED)))))
; (defn drag-at-handler [obj & [x-y-meta]]
;   (fn [event]
;     (when (= (jget event :event-type) (. MouseEvent MOUSE_DRAGGED))
;           (jset! obj
;             :x (- (jget event :scene-x) ; starts at the bottom left-hand corner of the text
;                   (mouse-inset-x))
;             :y (- (jget event :scene-y)
;                   (mouse-inset-y)))
;           (reset! last-mouse-event (. MouseEvent MOUSE_DRAGGED))
;           (when x-y-meta
;             (jset! x-y-meta  ; only runs into trouble when you have more than one x-y-meta
;               :x (-> obj (jget :x))
;               :y (-> obj (jget :y) (+ 10))
;               :text (str "x: " (format "%.0f" (jget obj :x)) " "
;                                        "y: " (format "%.0f" (jget obj :y))))))))
; (defn set-draggable-t! [obj & [meta?]]
;   (when meta?
;     (jdef :x-y-meta :text
;       :x 200
;       :y 200
;       :text "x: 100 y: 200"
;       :font (Font. (-> @fonts :myriad :lt) 10)
;       :fill (. Color RED))
;     (jconj! :rt :x-y-meta))
;   (jset! obj
;     :on-mouse-released (wrap-handler obj released-handler)
;     :on-mouse-pressed  (wrap-handler obj pressed-handler)
;     :on-mouse-dragged  (wrap-handler obj drag-at-handler
;                          (fn [obj-to-affect]
;                            (when meta? (eval 'x-y-meta))))))
; (defn set-draggable-f! [obj & [meta?]]
;   (jset! obj
;     :on-mouse-released nil
;     :on-mouse-pressed  nil
;     :on-mouse-dragged  nil))
; (defn set-draggable! [obj-key set-drag? & [meta?]]
;   (if set-drag?
;       (set-draggable-t! (ns/eval-key obj-key) meta?)
;       (set-draggable-f! (ns/eval-key obj-key) meta?)))
; ;___________________________________________________________________________________________________________________________________
; ;==========================================================={  LISTENERS  }=========================================================
; ;==========================================================={             }=========================================================
; (defn limit-text-to! [text-obj-0 max-len]
;   (let [text-obj (whenf text-obj-0 keyword? ns/eval-key)
;         ns-0     *ns*]
;     (-> text-obj .textProperty
;         (.addListener 
;           (proxy [ChangeListener] []
;             (changed [obs-val old-val new-val] ; also don't allow letters in field
;               (binding [*ns* ns-0]
;                 (when (-> text-obj (jget :text) count+ (> max-len)) 
;                   (jupdate! text-obj :text (f*n str/subs+ 0 max-len))))))))))
; ;___________________________________________________________________________________________________________________________________
; ;========================================================{  CAPTURE SYS.OUT  }======================================================
; ;========================================================{                   }======================================================
; (defn update-out-str-with! [out-str baos]
;     (swap! temp-rec conj baos)
; ; (swap! out-str conj (str baos))
;   (let [baos-str-0 (str baos)]
;     (if (empty? baos-str-0)
;         nil
;         (let [baos-str-f (getr+ baos-str-0 0 (-> baos-str-0 count+ dec dec))]
;         (swap! out-str conj
;           (str/subs+ baos-str-f
;                (whenf (+ 2 (last-index-of+ "\r\n" baos-str-f))
;                  (eq? 1) (constantly 0)))))))) ; if it's the same, keep it
; (defmacro with-capture-sys-out [expr out-str & [millis n-times]]
;   `(let [baos# (java.io.ByteArrayOutputStream.)
;          ps#   (java.io.OutputStreamWriter. baos#)]
;     (binding [*out* ps#]
;       (deref (future ~expr)) ; will process in background
;       (do-every
;         (whenf ~millis  nil? (constantly 500))
;         (whenf ~n-times nil? (constantly 6))
;         #(update-out-str-with! ~out-str baos#)))))