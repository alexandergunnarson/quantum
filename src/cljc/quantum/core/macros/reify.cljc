(ns quantum.core.macros.reify
           (:require [quantum.core.analyze.clojure.predicates :as anap
                       :refer [type-hint]                                ]
                     [quantum.core.collections.base           :as cbase
                       :refer [update-first update-val ensure-set
                               zip-reduce default-zipper #?(:clj kmap)]  ]
                     [quantum.core.error                      :as err                 
                               :refer [->ex]                             ]
                     [quantum.core.fn                         :as fn
                                :refer [#?@(:clj [fn-> fn->> <-])]       ]
                              [quantum.core.log               :as log    ]
                              [quantum.core.logic             :as logic
                                :refer [#?@(:clj [whenc]) nempty?]       ]
                     [quantum.core.macros.core                :as cmacros]
                     [quantum.core.macros.transform           :as trans  ])
  #?(:cljs (:require-macros
                     [quantum.core.collections.base           :as cbase
                       :refer [kmap]]
                     [quantum.core.fn                         :as fn
                       :refer [fn-> fn->> <-]                            ]
                     [quantum.core.log                        :as log    ]
                     [quantum.core.logic                      :as logic
                       :refer [whenc]                                    ])))
#?(:clj
(defn gen-reify-def
  [{:keys [sym ns-qualified-interface-name reify-body]}]
  (let [reified-sym (-> sym name
                        (str "-reified")
                        symbol)
        reified-sym-qualified
          (-> (symbol (name (ns-name *ns*)) (name reified-sym))
              (cmacros/hint-meta ns-qualified-interface-name))
        reify-def
          (list 'def reified-sym reify-body)]
    (kmap reified-sym
          reified-sym-qualified
          reify-def))))

#?(:clj
(defn gen-reify-body-raw
  [{:keys [ns-qualified-interface-name
           genned-method-name
           gen-interface-code-body-expanded]}]
  (apply list 'reify ns-qualified-interface-name
    (->> gen-interface-code-body-expanded
         (map (fn [[hints body]]
                (let [return-type-hinted-method
                       (cmacros/hint-meta genned-method-name (last hints))
                      arglist-n    (->> body first (into ['this]))
                      body-f       (->  body rest (trans/hint-body-with-arglist (first body) :clj))
                      updated-body (->> body-f (cons arglist-n))]
                  (cons return-type-hinted-method updated-body))))))))

#?(:clj
(defn verify-reify-body [reify-body sym]
  (let [; To handle ClassFormatError "Duplicate method name&signature"
        duplicate-methods
          (->> reify-body rest rest
               (map (fn-> rest
                         (update-first
                           (fn->> rest
                                  (mapv (fn-> type-hint (whenc nil? trans/default-hint)))))))
               (cbase/frequencies-by first)
               (group-by val)
               (<- dissoc 1))
        _ (when (nempty? duplicate-methods)
            (log/pr        :user "Duplicate methods for" sym ":")
            (log/ppr-hints :user duplicate-methods)
            (throw (->ex nil "Duplicate methods.")))]
    reify-body)))

#?(:clj
(defn gen-reify-body
  [{:as args
    :keys [sym
           ns-qualified-interface-name
           genned-method-name
           gen-interface-code-body-expanded]}]
  (-> (gen-reify-body-raw args)
      (verify-reify-body sym))))