(ns quanta.library.ui.custom-objs (:gen-class))
(require '[quanta.library.ns :as ns])
(ns/require-all *ns* :lib :clj :fx-core :qb :grid)
(ns/nss *ns*)

(defn fx-exists? [k]
  (try (-> k fx/lookup nnil?)
    (catch Exception e false)))
(defn custom-obj? [^Keyword obj-key]
  (apply splice-or obj-key = [:multi-rounded-rectangle]))
(defn jdissoc!
  ([parent elem]
     (when (fx-exists? elem)
       (fx/swap-content! (ns/eval-key parent)
         (partial remove (eq? (ns/eval-key elem))))
       (swap! fx/tree dissoc-in+
         (->> (conj (get-in @fx/objs [elem :parents]) elem)
              (interpose :children) ; [:rt :children :sr-day-box]
              vec+))
       (swap! fx/objs dissoc+ elem)
       (swap! fx/obj-key-pairs dissoc
         (ns/eval-key elem)))) ; or should it just be updating the keys to nil?
  ([parent k elem]
    (fx/swap-content! (ns/eval-key parent)
      (f*n update+ k
        (partial remove (eq? (ns/eval-key elem)))))))
(defn parent-key [^Keyword obj-key]
  (->  @fx/objs (get-in [obj-key :parents]) last+))
(defn jconj!
  "Makes @elem-k a child of @parent-k."
  ([parent-k elem-k]
    (let [parent (whenf parent-k keyword? ns/eval-key)
          elem   (whenf elem-k   keyword? ns/eval-key)]
     (swap! fx/tree assoc-in+
       (->> (conj (get-in @fx/objs [elem-k :parents]) parent-k elem-k)
            (interpose :children)
            vec+
            (<- conj :obj)) ; [:rt] -> [:rt :children :title :obj]
       elem)
     ; Add the parent's parents, and the parents
     (swap! fx/objs assoc-in+ [elem-k :parents]
       (conj (get-in @fx/objs [parent-k :parents]) parent-k)) ; [:rt :title]
     (fx/swap-content!* parent
       (f*n conj elem))))
  ([parent k elem]
     (fx/swap-content!* (ns/eval-key parent)
       (f*n update+ k conj (ns/eval-key elem)))))
(defn jnew
  "Defines an object with given properties.
   Checks if @ctrl is a custom object (e.g., |multi-rounded-rectangle|)."
  [^Keyword ctrl & props]
  (if (custom-obj? ctrl)
      (apply
        (ns-resolve
          (find-ns 'quanta.library.ui.custom-objs)
          (-> ctrl name symbol))
        props)
      (apply fx/fx* (-> ctrl name symbol) props)))
(def jdef-q (atom (queue))) ; FIFO
(defn jdef*
  "Defines an object with given properties.
   Also adds it to the scene graph, defaulting at the root node."
  {:todo ["FIX below..."]}
  [^Keyword obj-key ^Keyword ctrl & props]
  (intern *ns* (-> obj-key name symbol)
    (apply jnew ctrl props))
  (if (splice-or obj-key = :rt :scn :stg)
      (do (swap! fx/objs assoc-in+ [obj-key :parents] [])
          (swap! fx/tree assoc-in+ [obj-key :obj]
            (ns/eval-key obj-key)))
      (do (swap! fx/objs assoc-in+ [obj-key :parents] [:rt])
          (swap! fx/tree assoc-in+ [:rt :children obj-key :obj]
            (ns/eval-key obj-key))))
  (swap! fx/objs assoc-in+ [obj-key :ref] (ns/eval-key obj-key))
  (swap! fx/objs assoc-in+ [obj-key :style]
    (get (apply hash-map props) :button-style)) ; FIX THIS!!
  (swap! fx/obj-key-pairs
    assoc (ns/eval-key obj-key) obj-key)
  (ns/eval-key obj-key))
(defn jdef
  "Define an object with given properties and
   place it on a queue to be added to the root node."
  [^Keyword obj-key ^Keyword ctrl & props]
  (when (fx-exists? obj-key)
    (println "Redefining object" (str "'" (name obj-key) "'" "..."))
    (jdissoc! (parent-key obj-key) obj-key))
  (apply jdef* obj-key ctrl props)
  (swap! jdef-q conj obj-key) ; place it on the queue
  (ns/eval-key obj-key))
(defn jdef!
  "Same as /jdef/, but immediately /conj/es the obj to the root node."
  [^Keyword obj-key ^Keyword ctrl & props]
  (apply jdef obj-key ctrl props)
  (jconj! :rt obj-key)
  (swap! jdef-q pop)
  (ns/eval-key obj-key))
(defn jconj-all!
  "Conj all objs in queue to root node."
  []
  (doseq [^Keyword k @jdef-q]
    (jconj! :rt k))
  (reset! jdef-q (queue)))
;___________________________________________________________________________________________________________________________________
;========================================================={ OTHER HELPERS }=========================================================
;========================================================={               }=========================================================
;; TODO inefficient.
(defn j-set-get [method-type obj k args]
  (let [^APersistentMap prefix-table
          {:get {:is? "is"  :else "get"}
           :set {:is? "set" :else "set"}}
        ^APersistentMap ends-q-table
          (if (str/ends-with? (name k) "?")
              {:sub-fn butlast+ :type :is?}
              {:sub-fn identity :type :else})
        method (->> k name ((:sub-fn ends-q-table))
                    (fx/prepend-and-camel
                      (-> prefix-table method-type
                          (get (:type ends-q-table))))
                    str symbol)]
    (apply fx/exec-method obj method args)))
(defn jget 
  "Retrieves a property keyword @k from an object @obj-0"
  {:usage "(jget rectangle1 :width)"}
  ([obj-0 ^Keyword k]
    (let [obj (ifn obj-0 keyword? ns/eval-key identity)] ; Make this a protocol
      (j-set-get :get obj k nil)))
  ([obj   ^Keyword k & ks]
    (reduce+ (fn [ret k] (conj ret (jget obj k))) [] (conj ks k))))
(defmacro jget-prop "fetches a property from a node." [obj prop & args]
  (if (= \? (subs (name prop) (dec (count (name prop)))))
      "Error!"
      `(~(symbol (str "." (fx/prepend-and-camel (name prop) "property"))) ~obj ~@args)))
(defn jgets-map [obj & ks]
  (reduce+
    (fn [ret k]
      (assoc ret k (jget obj k)))
    (sorted-map+)
    ks))
(defn jget-in [obj & ks]
  (reduce+
    (fn [ret k] (jget ret k))
    obj ks))

(defn jset!
  ([obj-0 k args]
    (let [obj (ifn obj-0 keyword? ns/eval-key identity)]
      (do-fx (j-set-get :set obj k (coll-if args)))))
  ([obj k v & kvs]
    (reduce-2 jset! obj (conj kvs v k))))
(defn jupdate! [obj k func]
  (->> obj (<- jget k) func (jset! obj k)))
(defn remove-all-fx!
  ([obj]  
    (do (fx/swap-content!* obj (constantly [])) obj))
  ([obj k]
    (do (fx/swap-content!* obj
          (f*n update+ k (constantly []))) obj)))
(def clear! remove-all-fx!)

;___________________________________________________________________________________________________________________________________
;======================================================{ VISUAL ARRANGEMENT }=======================================================
;======================================================{                    }=======================================================
(defmacro fx-try [try-expr else-expr]
  ; I don't think try catch works here because
  ; it's operating on another thread...?
  `(whenf ~try-expr (eq? :error)
    (constantly ~else-expr)))
(defn setx! [obj v]
  (fx-try
    (jset! obj :x v)
    (jset! obj :translate-x v)))
(defn sety! [obj v]
 (fx-try
    (jset! obj :y v)
    (jset! obj :translate-y v)))
(defn getx [obj]
  (fx-try
    (jget obj :x)
    (jget obj :translate-x)))
(defn gety [obj]
  ; Rectangles calculate from the top-left; text, from the bottom-left
  (if (= javafx.scene.text.Text (jget obj :class))
      (- (jget obj :y)
         (jget-in obj :layout-bounds :height))
      (fx-try
        (jget obj :y)
        (jget obj :translate-y))))
(defn get-div [obj-0 n divs & [dim]]
  (let [obj (ifn obj-0 keyword? ns/eval-key identity)
        center {:x (+ (getx obj)
                      (-> obj (#(fx-try (jget-in % :layout-bounds :width)  (jget % :width)))
                              (/ divs) (* n)))
                :y (+ (gety obj)
                      (-> obj (#(fx-try (jget-in % :layout-bounds :height) (jget % :height)))
                              (/ divs) (* n)))}]
    (if (keyword? dim) (get center dim) center)))
(defn get-center [obj-0 & [dim]]
  (get-div obj-0 1 2 dim))
(defn get-size [obj-0 & [dim]]
  (let [obj (ifn obj-0 keyword? ns/eval-key identity)
        size {:x (jget-in obj :layout-bounds :width)
              :y (jget-in obj :layout-bounds :height)}]
    (if (keyword? dim) (get size dim) size)))
(defn get-pos [obj pos]
  (case pos
    :left
    (getx obj)
    :right
    (+ (getx obj) (get-size obj :x))
    :top
    (gety obj)
    :bottom
    (+ (gety obj) (get-size obj :y))
    (throw (Exception. "Unknown position requested."))))
(defn arrange-on! [obj-top obj-bottom n divs & [orientation]]
  ; default n is 2
  (let [set-fn (case orientation :x setx! :y sety! nil)]
    (if (nil? orientation)
        (do (arrange-on! obj-top obj-bottom n divs :x)
            (arrange-on! obj-top obj-bottom n divs :y))
        (when (splice-or orientation = :x :y)
          (set-fn obj-top
            (-> obj-bottom
                (get-div n divs orientation)
                ((case orientation ; condpc apparently causes strange problems here...
                   :x
                   -
                   :y
                   (if (= (jget obj-top :bounds-type)
                          (. javafx.scene.text.TextBoundsType VISUAL))
                       +
                       -))
                 (-> obj-top (get-size orientation) (/ 2)))))))))
(defn center-on! [obj-top obj-bottom & [orientation]]
  (arrange-on! obj-top obj-bottom 1 2 orientation))

(defn place-at! [obj at-obj pos]
  (case pos
    :middle (center-on! obj at-obj)
    :left 
    (do (center-on! obj at-obj :y)
        (setx! obj
          (- (get-pos at-obj :left)
             (get-size obj :x))))
    :right
    (do (center-on! obj at-obj :y)
        (setx! obj (get-pos at-obj :right)))
    :top
    (do (center-on! obj at-obj :x)
        (sety! obj
          (- (get-pos at-obj :top)
             (get-size obj :y))))
    :bottom
    (do (center-on! obj at-obj :x)
        (sety! obj (get-pos at-obj :bottom)))
    (throw+ {:type :unknown-placement
             :message "Unknown placement requested."})))
(defn nudge! [obj pos n]
  (case pos
    :left  (setx! obj (- (getx obj) n))
    :right (setx! obj (+ (getx obj) n))
    :up    (sety! obj (- (gety obj) n))
    :down  (sety! obj (+ (gety obj) n))))
(defn add-all!
  "Adds, via |.add|, all elements @args to an object @obj.
   Sometimes |.addAll| doesn't work; thus this function."
  ([obj arg]
    (try
      (.add obj arg) ; this needs to be reflection-powered
      obj
      (catch ClassCastException _ obj)))
  ([obj arg & args]
    (try
      (add-all! obj arg)
      (doseq [arg-n args]
        (add-all! obj arg-n))
      obj
      (catch ClassCastException _ obj))))
;___________________________________________________________________________________________________________________________________
;========================================================{   CUSTOM OBJECTS  }======================================================
;========================================================{                   }======================================================
(defn multi-rounded-rectangle
  [& {:keys [width height
             fill stroke stroke-width
             arc-radius-top-left    arc-radius-top-right
             arc-radius-bottom-left arc-radius-bottom-right]
      :or   {stroke-width            0
             arc-radius-top-left     0
             arc-radius-top-right    0
             arc-radius-bottom-left  0
             arc-radius-bottom-right 0}}]
  (let [^Path rect
         (jnew :path
               :fill         fill
               :stroke       stroke
               :stroke-width stroke-width)
        arc-radius-top-left     (/ arc-radius-top-left     2)
        arc-radius-top-right    (/ arc-radius-top-right    2)
        arc-radius-bottom-left  (/ arc-radius-bottom-left  2)
        arc-radius-bottom-right (/ arc-radius-bottom-right 2)]
    (add-all! 
      (jget rect :elements)
      (jnew :move-to
        :x arc-radius-top-left
        :y 0)
      (jnew :h-line-to
        :x (- width arc-radius-top-right))
      (jnew :arc-to
        :x          width
        :y          arc-radius-top-right
        :radius-x   arc-radius-top-right
        :radius-y   arc-radius-top-right
        :sweep-flag true)
      (jnew :v-line-to
        :y (- height arc-radius-bottom-right))
      (jnew :arc-to
        :x          (- width arc-radius-bottom-right)
        :y          height
        :radius-x   arc-radius-bottom-right
        :radius-y   arc-radius-bottom-right
        :sweep-flag true)
      (jnew :h-line-to
        :x arc-radius-bottom-left)
      (jnew :arc-to
        :x          0
        :y          (- height arc-radius-bottom-left)
        :radius-x   arc-radius-bottom-left
        :radius-y   arc-radius-bottom-left
        :sweep-flag true)
      (jnew :v-line-to
        :y arc-radius-top-left)
      (jnew :arc-to
        :x          arc-radius-top-left
        :y          0
        :radius-x   arc-radius-top-left
        :radius-y   arc-radius-top-left
        :sweep-flag true))
    rect))