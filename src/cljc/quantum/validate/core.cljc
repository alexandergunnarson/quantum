(ns quantum.validate.core
  (:require-quantum [:core fn logic str])
  (:require [quantum.validate.domain])
  #?(:clj (:import java.util.regex.Matcher)))

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
