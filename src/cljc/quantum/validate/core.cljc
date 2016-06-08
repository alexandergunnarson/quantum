(ns quantum.validate.core
           (:require [schema.core              :as s   ]
                     [quantum.core.logic
                       :refer [#?@(:clj [fn-not])]]
                     [quantum.core.error       :as err
                       :refer [->ex]                   ]
                     [quantum.core.string      :as str ]
                     [quantum.core.collections :as coll
                       :refer [#?@(:clj [containsv?])] ]
                     [quantum.core.vars        :as var 
                       :refer [#?(:clj defalias)]      ]
                     [quantum.validate.domain          ])
  #?(:cljs (:require-macros 
                     [quantum.core.collections :as coll
                       :refer [containsv?]             ]
                     [quantum.core.logic
                       :refer [fn-not]]
                     [quantum.core.vars        :as var 
                       :refer [defalias]               ]))
  #?(:clj (:import java.util.regex.Matcher)))

#?(:clj
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

(def constrained s/constrained)
(def pred        s/pred)
(def one*        s/one)
#?(:clj (defmacro one [schema]
  `(one* ~schema '~schema)))
(def optional*   s/optional)
#?(:clj (defmacro optional [schema]
  `(optional* ~schema '~schema)))
(def Str         s/Str)
(def Int         s/Int)
(def Keyword     s/Keyword)
(def Num         s/Num)

; A few built-in validators

(def no-blanks?  (pred (fn no-blanks? [x] (not (containsv? x " ")))))

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
