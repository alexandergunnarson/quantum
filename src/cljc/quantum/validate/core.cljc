(ns quantum.validate.core
           (:refer-clojure :exclude [string? keyword? set? number? fn? assert])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )   :as core        ]
                     [#?(:clj  clojure.spec
                         :cljs cljs.spec   )   :as s           
                       :refer-macros [assert]]
                     [quantum.core.logic
                       :refer        [#?@(:clj [fn-not])       ]
                       :refer-macros [fn-not]                  ]
                     [quantum.core.error       :as err
                       :refer        [->ex]                    ]
                     [quantum.core.macros.core
                       :refer [#?(:clj if-cljs)]]
                     [quantum.core.string      :as str         ]
                     [quantum.core.collections :as coll
                                  :refer [#?@(:clj [containsv?])]
                       #?@(:cljs [:refer-macros [containsv?]])]
                     [quantum.core.vars        :as var 
                                  :refer        [#?@(:clj [defalias defmalias])]      
                       #?@(:cljs [:refer-macros [defalias defmalias]])]
                     [quantum.validate.domain                  ])
  #?(:cljs (:require-macros 
                     [quantum.validate.core
                       :refer [spec]]))
  #?(:clj (:import java.util.regex.Matcher)))
#_(:clj
(defmacro validate
  {:todo ["Should:
            - attempt to reduce the verbosity of its output by
              restricting the size of values that fail validation
              to 19 characters. If a value exceeds this, it will
              be replaced by the name of its class. You can adjust
              this size limitation by calling set-max-value-length!."]}
  [pred v]
  `(try (s/validate ~pred ~v)
     (catch clojure.lang.ExceptionInfo e#
       (let [data# (ex-data e#)
             value-unevaled# '~v]
         (throw (->ex (:type data#)
                      (str "Value does not match schema: " {:value-unevaled value-unevaled#
                                                            :value (:error data#)})
                      (assoc data# :value-unevaled value-unevaled#))))))))

#_(def constrained s/constrained)
#?(:clj (quantum.core.vars/defmalias spec     clojure.spec/spec   cljs.spec/spec  ))
#_(:clj (quantum.core.vars/defmalias validate clojure.spec/assert cljs.spec/assert))
#?(:clj
(defmacro validate [& args]
  (if-cljs &env
    `(cljs.spec/assert ~@args)
    `(clojure.spec/assert ~@args))))

#_(def one*        s/one)
#_(:clj (defmacro one [schema]
  `(one* ~schema '~schema)))
#_(def optional*   s/optional)
#_(:clj (defmacro optional [schema]
  `(optional* ~schema '~schema)))
(def string?  (spec core/string? ))
(def keyword? (spec core/keyword?))
(def set?     (spec core/set?    ))
(def number?  (spec core/number? ))
(def fn?      (spec core/fn?     ))
; A few built-in validators

(def no-blanks?  (spec (fn no-blanks? [x] (not (containsv? x " ")))))

