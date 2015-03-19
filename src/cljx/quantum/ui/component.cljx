(ns
  ^{:doc "Namespace for creating/defining UI components, especially
          with |defcomponent|."
    :attribution "Alex Gunnarson"}
  quantum.ui.component
  (:require
   [quantum.core.ns          :as ns                                        ]
   [quantum.core.data.map    :as map       :refer [map-entry]              ]
   [quantum.core.data.vector :as vec                                       ]
   [quantum.core.function    :as fn        :refer [f*n compr]              ]
   [quantum.core.logic       :as log       :refer
      [fn-not fn-and fn-or splice-or nnil?]]
   [quantum.core.numeric     :as num       :refer [neg]                    ]
   [quantum.core.type        :as type      :refer [instance+? #+cljs class]]
   [quantum.core.string      :as str                                       ]
   [quantum.core.collections :as coll :refer
     [redv redm into+ reduce+
      rest+ first+ single?
      lasti takeri+ taker-untili+
      split-remove+ ffilter remove+
      last-index-of+ index-of+
      dropl+ dropr+ takel+
      getr+ interpose+ in?
      map+ filter+ lfilter lremove group-by+
      merge+ key+ val+
      merge-keep-left]]
   #+cljs
   [quantum.ui.css  :as css]
   #+cljs
   [quantum.ui.form :as form])
  #+clj (:gen-class))  

(def components (atom #{}))

(defn register-component! [var-0]
  (swap! components conj var-0))

(def state (atom {}))

(def ^:dynamic *component-hook*
  (fn [html]
    #+cljs
    (css/update-css-once! html @state) ; because it's a double reference...
    #+cljs
    (css/style html)))

(defn throw-args [args]
  (throw (ex-info "Arguments to |defcomponent| must be a vector:" args)))

(defmacro defcomponent
  "Defines a UI component in @body with the hook |*component-hook*|.

   The *component-hook* initializes to 1) a CSS auto-styling on re-render,
   and 2) component registration."
  {:attribution "Alex Gunnarson"
   :todo ["Make less repetitive."]}
  ([name-0 args]
    `(defcomponent ~name-0 ~args nil nil (list)))
  ([name-0 args body]
    `(defcomponent ~name-0 ~args nil nil ~body))
  ([name-0 doc-0 meta-0 args & body]
    (if (symbol? name-0)
        (if (string? doc-0)
            (if (map? meta-0)
                (if (vector? args)
                    `(doto (defn ~name-0 ~doc-0 ~meta-0 ~args
                             (*component-hook*
                               (do ~@body)))
                           register-component!)
                    (throw-args args))
                (if (vector? meta-0)
                    `(doto (defn ~name-0 ~doc-0 ~meta-0
                             (*component-hook*
                               (do ~args ~@body)))
                           register-component!)
                    (throw-args meta-0)))
            (if (map? doc-0)
                (if (vector? doc-0)
                    `(doto (defn ~name-0 ~doc-0 ~meta-0
                             (*component-hook*
                               (do ~args ~@body)))
                           register-component!)
                    (throw-args doc-0))
                (if (vector? doc-0)
                    `(doto (defn ~name-0 ~doc-0
                             (*component-hook*
                               (do ~meta-0 ~args ~@body)))
                           register-component!)
                    (throw-args doc-0))))
        (throw (ex-info "Component name must be a symbol:" name-0)))))
