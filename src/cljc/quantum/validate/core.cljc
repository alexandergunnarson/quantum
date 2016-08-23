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

(def email:special-chars     "\\p{Cntrl}\\(\\)<>@,;:'\\\\\\\"\\.\\[\\]")
(def email:valid-chars       (str "[^\\s" email:special-chars "]"))
(def email:quoted-user       "(\"[^\"]*\")")
(def email:word              (str "((" email:valid-chars "|')+|" email:quoted-user ")"))

(def email:pattern           (re-pattern "^\\s*?(.+)@(.+?)\\s*$"))
(def email:ip-domain-pattern (re-pattern "^\\[(.*)\\]$"))
(def email:user-pattern      (re-pattern (str "^\\s*" email:word "(\\." email:word ")*$")))

(def ^:const max-email:user-length 64)

; getInstance(boolean allowLocal, boolean allowTld) 

#?(:clj
(defn email:user?
  "Returns |true| if the user component of an email address is valid."
  {:todo ["Port to CLJS"]
   :derivations ["org.apache.commons.validator.routines.EmailValidator"
                 "Sandeep V. Tamhankar (stamhankar@hotmail.com)"]}
  [^String user]
  (and (string? user)
       (<= (count user) max-email:user-length)
       (.matches ^Matcher (re-matcher email:user-pattern user)))))
   
#?(:clj
(defn domain?
  "Returns true if the domain component of an email address is valid.
   @param domain being validated. May be in IDN format."
  {:todo ["Port to CLJS"]
   :derivations ["org.apache.commons.validator.routines.EmailValidator"
                 "Sandeep V. Tamhankar (stamhankar@hotmail.com)"]}
  [domain & [allow-local?]]
  ; see if domain is an IP address in brackets
  (let [^Matcher ipDomainMatcher (re-matcher email:ip-domain-pattern domain)]
    ; (if (.matches ipDomainMatcher) {
    ;     InetAddressValidator/getInstance.isValid(ipDomainMatcher.group(1));
    ; })
    ; Domain is symbolic name
    (quantum.validate.domain/valid? domain allow-local?))))

#?(:clj
(defn email?
  {:todo ["Port to CLJS"]
   :derivations ["org.apache.commons.validator.routines.EmailValidator"
                 "Sandeep V. Tamhankar (stamhankar@hotmail.com)"]}
  [email & [allow-local?]]
  (and (string? email)
       (not (str/ends-with? email ".")) ; Apparently this is common
       ;(= email (str/trim email))
       (let [^Matcher email-matcher (re-matcher email:pattern email)]
         (and (.matches email-matcher)
              (email:user? (.group email-matcher 1))
              (domain?     (.group email-matcher 2) allow-local?))))))
