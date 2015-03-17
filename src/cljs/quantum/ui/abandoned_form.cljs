(ns quantum.ui.abandoned-form)
; (ns quantum.ui.form
;   (:require
;    [reagent.core :as re]
;    [quantum.core.ns          :as ns        :refer [Num Vec Key Seq JSObj]        ]
;    [quantum.core.data.map    :as map       :refer [map-entry]          ]
;    [quantum.core.data.vector :as vec                                   ]
;    [quantum.core.function    :as fn        :refer [f*n compr]                ]
;    [quantum.core.logic       :as log       :refer [fn-not fn-and fn-or splice-or nnil?]]
;    [quantum.core.numeric     :as num       :refer [neg]                ]
;    [quantum.core.type        :as type      :refer [instance+? class]   ]
;    [quantum.core.string      :as str                                   ]
;    [sablono.core             :as sab       :include-macros true        ]
;    [clojure.walk                           :refer [postwalk]           ]
;    [figwheel.client          :as fw                                    ]
;    [garden.core              :as css       :refer [css]                ]
;    [garden.stylesheet        :as css-style                             ]
;    [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]]
;    [goog.object :as gobject]
;    [quantum.core.error     :refer [with-throw]]
;    [quantum.core.collections :as coll :refer
;      [redv redm into+ reduce+
;       rest+ first+ single?
;       lasti takeri+ taker-untili+
;       split-remove+ ffilter remove+
;       last-index-of+ index-of+
;       dropl+ dropr+ takel+
;       getr+ interpose+ in?
;       map+ filter+ lfilter lremove group-by+
;       merge+ key+ val+
;       merge-keep-left]])
;   (:require-macros
;    [cljs.core.async.macros :refer [go-loop go]]
;    [quantum.core.function  :refer [fn->> fn-> <-]]
;    [quantum.core.logic     :refer [whenf whenf*n whenc whencf*n condf*n]]))  

; ; Inspiration taken from sablono.

; (defn rename-keys [m-0 rename-m]
;   (reduce+
;     (fn [ret k-0 k-f]
;       (-> ret
;           (assoc  k-f (get ret k-0))
;           (dissoc k-0)))
;     m-0
;     rename-m))

; (defn camel-case
;   "Returns camel case version of the key, e.g. :http-equiv becomes :httpEquiv."
;   {:attribution "sablono.util"
;    :todo ["Improve performance"]}
;   [^Key k]
;   (when (nnil? k)
;     (let [[first-word & words] (str/split (name k) #"-")]
;       (if (or (empty? words)
;               (= "aria" first-word)  ; what do these have to do with anything?
;               (= "data" first-word)) ; what do these have to do with anything?
;           k
;           (->> words
;                (map str/capitalize)
;                (<- conj first-word)
;                str/join
;                keyword)))))




; (def ui-element?
;   (fn-and
;     vector?                ; element
;     (fn-> first+ keyword?) ; tag  
;     (fn-> first+ (not= :style)) ; to make sure it's not just an attribute ; check this further
;     (fn-or
;       (fn-> second coll?)
;       (fn-> first+ name (str/contains? ".")))))  ; attributes

; (defn determine-tag-domain!
;   [^Vec html ^Atom ui-tag-domain]
;   (->> html
;        (postwalk ; [:style {...}] ; should be map-entry but... and this will happen with other attributes
;          (fn [elem]
;            (when (ui-element? elem)
;              (let [^Key tag (first elem)]
;                (swap! ui-tag-domain conj tag)))
;            elem))))

; ; (camel-case-keys {:on-click inc :on-drag true :bold-text-area :no})
; (defn camel-case-keys
;   "Recursively transforms all map keys into camel case."
;   {:attribution "sablono.util"
;    :todo        ["Improve performance"]}
;   [m]
;   (whenf m map?
;     (fn->>
;       (map+ (fn [[k v]] (map-entry (camel-case k) v)))
;       redm
;       (<- whenf (fn-> :style map?)
;         (f*n update-in [:style] camel-case-keys)))))

; (defprotocol IJSValue
;   (to-js [x]))

; (defn- to-js-map
;   [^Map m]
;   ; #js ; same as cljs.tagged_literals.JSValue
;     (->> m
;          (map+
;            (fn [k v]
;              (map-entry (to-js k) (to-js v))))
;          redm
;          js-obj))

; ; {:attribution "sablono.util"}
; (extend-protocol IJSValue
;   cljs.core/PersistentArrayMap
;     (to-js [x] (to-js-map x))
;   cljs.core/PersistentHashMap
;     (to-js [x] (to-js-map x))
;   cljs.core/PersistentVector
;     (to-js [x] ;  #js
;       (->> x (map+ to-js) redv js-obj))
;   object
;     (to-js [x] x)
;   nil
;     (to-js [_] nil))

; ; ===================== ATTRIBUTES =====================

; ; (defn attributes
; ;   {:attribution "sablono.interpreter"}
; ;   [^Map attrs-0]
; ;   (let [^JSObj attrs (-> attrs-0 camel-case-keys html-to-dom-attrs clj->js)
; ;         class-0 (.-className attrs)
; ;         class-f (if (array? class-0) (join " " class) class-0)]
; ;     (if (blank? class-f)
; ;         (js-delete attrs "className") ; why does this low-level operation need to be called?
; ;         (set! (.-className attrs) class-f))
; ;     attrs))

; (def dom-rename-map
;   {:class :className
;    :for   :htmlFor})

; (defn html-to-dom-attrs
;   "Converts all HTML attributes to their DOM equivalents."
;   {:attribution "sablono"}
;   [^Map attrs]
;   (rename-keys attrs dom-rename-map))

; (defn ^MapEntry compile-attr
;   {:attribution "Alex Gunnarson"
;    :todo        ["Catch attributes that aren't strings or functions, etc."]}
;   [[^Key k v]]
;   (println "k" k "v" v)
;   (let [v-f
;           (cond
;             (= k :class) (interpose+ " " v)
;             (= k :style) (-> v camel-case-keys to-js)
;             :else        v)]
;     (map-entry k v-f)))

; ; (compile-attrs {:style {:bold true} :on-click inc})
; (defn ^JSObj compile-attrs
;   "Compile a HTML attribute map."
;   {:attribution "sablono.util"}
;   [^Map attrs]
;   (->> attrs
;        (map+ compile-attr)
;        redm
;        camel-case-keys
;        html-to-dom-attrs
;        to-js))


; ; ; ===================== WHOLE ELEMENTS =====================

; ; Possibilities
; ; [:div [...]]
; ; [:div {style} [...]]

; (defn wrap-form-element [ctor ^String display-name]
;   (js/React.createFactory
;    (js/React.createClass
;     #js
;     {:getDisplayName
;      (fn [] (name display-name))
;      :getInitialState
;      (fn []
;        (this-as this
;          #js {:value (aget (.-props this) "value")}))
;      :onChange
;      (fn [e]
;        (this-as this
;          (let [handler (aget (.-props this) "onChange")]
;            (when (nnil? handler)
;              (handler e)
;              (.setState this #js {:value (.. e -target -value)})))))
;      :componentWillReceiveProps
;      (fn [new-props]
;        (this-as this
;          (.setState this #js {:value (aget new-props "value")})))
;      :render
;      (fn []
;        (this-as this
;          ;; NOTE: if switch to macro we remove a closure allocation
;          (let [props #js {}]
;            (gobject/extend props (.-props this)
;                            #js {:value    (aget (.-state this) "value")
;                                 :onChange (aget this "onChange")
;                                 :children (aget (.-props this) "children")})
;            (ctor props))))})))

; (def ^Map wrapped-types
;   {"input"    "" ; (wrap-form-element js/React.DOM.input    "input"   ) ; These have to be done in a .clj setting
;    "option"   "" ; (wrap-form-element js/React.DOM.option   "option"  ) ; These have to be done in a .clj setting
;    "textarea" "" ; (wrap-form-element js/React.DOM.textarea "textarea") ; These have to be done in a .clj setting
;    })

; (defn wrapped-type?
;   "Determines if the element @type needs to be wrapped."
;   [elem-type]
;   (-> elem-type (in? wrapped-types)))

; (defn create-wrapped-element
;   [^String elem-type ^Map props & children]
;   (let [^Fn create-fn
;           (if (wrapped-type? elem-type)
;               (get wrapped-types type)
;               (partial js/React.createElement elem-type))]
;     (create-fn props
;       (whenf children (fn-and sequential? single?)
;         first))))

; (defn ^Fn react-fn
;   "Returns an fn that builds a React element."
;   [type]
;   (if (wrapped-type? type)
;       create-wrapped-element
;       js/React.createElement))

; (defn ^Vec split-tag
;   "Split tag into a vector of tag name, id, and CSS classes."
;   {:in   [":div.html-me.abc#def"]
;    :out  "['div' 'def' ['html-me' 'abc']]"
;    :todo ["Avoid |re-seq|" "Fix [tag-name *nil* classes] return"]}
;   [^Keyword tag-0]
;   (let [matches (re-seq #"[#.]?[^#.]+" (name tag-0)) ; ("div" ".html-me" ".abc" "#def")
;         ^String elem-type (first matches) 
;         ^Vec classes
;           (->> matches
;                (filter+ (fn-> first+ (= ".")))
;                (map+ rest+) ; remove beginning period
;                redv)
;         ^String id
;           (->> matches
;                (ffilter (fn-> first+ (= "#")))
;                (<- whenf nnil? rest+))] ; remove beginning '#'
;     [elem-type id classes])) ; would normally output a hash-map but it's quicker to construct a vector

; (defn with->> [f arg] (f arg) arg)

; ; (normalize-element [:div.tag#idd {:a 1 :style {:a 1}}  "Abc" "Def"])

; ; (compile-react-element [:div.tag#idd {:a 1 :style {:a 1}}  "Abc" "Def"])
; (defn ^Vec normalize-element
;   "Ensure an element vector is of the form [tag-name attrs content]."
;   {:attribution  "sablono.compiler"
;    :contributors ["Alex Gunnarson"]
;    :todo         ["Don't create map for tag-attrs"]
;    :in           ["[:div.tag#idd {:a 1 :style {:a 1}} 'Abc' 'Def']"]
;    :out          "['div' {:a 1, :style {:a 1}, :class ['tag'], :id 'idd'} ('Abc' 'Def')]"}
;   [[^Key tag & content]]
;   #_{:pre [(with-throw
;            (keyword? tag)
;            (str tag " is not a valid element name."))]}
;   (let [[^String tag-f ^String id ^Vec classes] (split-tag tag)
;         _ (println "tag-f" tag-f)
;         ^Map tag-attrs (->> {:id id :class classes})
;         map-attrs (first content)
;         ^Map attrs-f
;           (->> (if (map? map-attrs)
;                    (merge+ tag-attrs map-attrs)
;                    tag-attrs)
;                (with->> (fn->> (println "Attrs at this point")))
;                (remove+ (compr val+ (fn-or nil? (fn-and coll? empty?))))
;                redm)
;         _ (println "attrs-f" attrs-f)
;         ^Vec content-f
;           (if (map? map-attrs)
;               (rest content)
;               content)]
;     [tag-f attrs-f content-f]))

; (declare compile-react-element)


; ; 1. normalize-element
; (defprotocol CompileReact
;   (compile-react [obj]))

; ; TODO make less repetitive
; (extend-protocol CompileReact
;   cljs.core/PersistentVector
;   (compile-react [coll]
;     (compile-react-element coll))
;   cljs.core/ChunkedSeq ; |let| statement destructuring
;     (compile-react [coll]
;       (if (empty? coll)
;           nil
;           (->> coll (map+ compile-react) redv)))
;   cljs.core/IndexedSeq ; |fn| variadic args
;     (compile-react [coll]
;       (if (empty? coll)
;           nil
;           (->> coll (map+ compile-react) redv)))
;   string 
;     (compile-react [obj] obj)
;   object
;     (compile-react [obj] obj)
;   nil
;     (compile-react [obj] nil))

; ; Need to go through the style step at this point too...
; (defn compile-react-element
;   "Render an element vector as a HTML element."
;   {:attribution  "sablono.compiler"
;    :contributors ["Alex Gunnarson"]}
;   [element]
;   (let [[^String html-elem-type ^Map attrs ^Seq content]
;           (normalize-element element)
;         ^Fn fn-compiled (react-fn html-elem-type)
;         _ (println "REACT: ATTRS-F" attrs)
;         _ (println "REACT: FN-COMP" fn-compiled "from elem-type" html-elem-type)
;         attrs-compiled (compile-attrs attrs)
;         _ (println "REACT: ATTRS-COMP" attrs-compiled)
;         react-compiled (compile-react content)
;         _ (println "REACT: REACT-COMP" react-compiled (class react-compiled) "array?" (array? react-compiled))
;         ]
;     (apply fn-compiled html-elem-type attrs-compiled react-compiled)))

; (defn html [^Vec html-vec] (compile-react-element html-vec))
