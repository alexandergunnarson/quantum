(ns quantum.core.type.predicates
           (:refer-clojure :exclude
             [map-entry? boolean? seqable?])
           (:require
             #?(:clj [clojure.core             :as core   ])
                     [quantum.core.core        :as c      ]
                     [quantum.core.fn          :as fn
                       :refer [#?@(:clj [fn->])]          ]
                     [quantum.core.logic       :as logic
                       :refer [#?@(:clj [fn-and])]        ]
                     [quantum.core.vars        :as var
                       :refer [#?(:clj defalias)]         ])
  #?(:cljs (:require-macros
                     [quantum.core.fn          :as fn
                       :refer [fn->]                      ]
                     [quantum.core.logic       :as logic
                       :refer [fn-and]                    ]
                     [quantum.core.vars        :as var
                       :refer [defalias]                  ])))

(defalias atom?     c/atom?    )
(defalias seqable?  c/seqable? )
(defalias boolean?  c/boolean? )
(defalias editable? c/editable?)

(defn regex? [obj]
  #?(:clj  (instance? java.util.regex.Pattern obj)
     :cljs (instance? js/RegExp               obj)))

(defn derefable? [obj]
  (satisfies? #?(:clj clojure.lang.IDeref :cljs cljs.core/IDeref) obj))

(def map-entry?  #?(:clj  core/map-entry?
                    :cljs (fn-and vector? (fn-> count (= 2)))))

(defn listy? [obj] (seq? obj)
  #_(->> obj class
         (contains? (get types 'listy?))))
