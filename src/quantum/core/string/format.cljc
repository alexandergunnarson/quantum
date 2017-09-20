(ns
  ^{:doc "Useful string utils for formatting strings."
    :attribution "alexandergunnarson"}
  quantum.core.string.format
          (:refer-clojure :exclude [replace reduce])
          (:require [frak]
                    [cuerdas.core                  :as str+]
                    [clojure.string                :as str]
                    [clojure.core                  :as core]
           #?(:cljs [goog.string                   :as gstr])
                    [quantum.core.fn               :as fn
                      :refer [fn->>]]
                    [quantum.core.loops            :as loops
                      :refer [reduce]]
                    [quantum.core.macros           :as macros
                      :refer [defnt]]
                    [quantum.core.string.regex     :as regex]
                    [quantum.core.vars             :as var
                      :refer [defalias]])
  #?(:clj (:import java.net.IDN)))

(defnt replace* ; TODO .replace may try to compile a pattern instead of replacing the actual string
  ([^string? pre ^String post ^String s] (.replace    s pre post))
  ([^regex?  pre         post         s] (str/replace s pre post)))

(defn replace [s pre post] (replace* pre post s))

#?(:cljs
(defn ->regexp
  "Build or derive regexp instance."
  {:attribution "funcool/cuerdas"}
  ([s]
   (if (regexp? s)
     s
     (js/RegExp. s)))
  ([s flags]
   (if (regexp? s)
     (js/RegExp. (.-source s) flags)
     (js/RegExp. s flags)))))

; ====== CASES ======

(defnt ->lower-case
  "Converts string to all lower-case.
   Works in strictly locale independent way.
   If you want a localized version, use `->locale-lower`."
          ([^string? s] (.toLowerCase s))
  #?(:clj ([^char?   c] (Character/toLowerCase c))))
(defalias ->lower ->lower-case)

(defnt ->upper-case
  "Converts string to all upper-case.
   Works in strictly locale independent way.
   If you want a localized version, use `->locale-upper`."
          ([^string? s] (.toUpperCase s))
  #?(:clj ([^char?   c] (Character/toUpperCase c))))
(defalias ->upper ->upper-case)

(defn capitalize-each-word [string]
  (str/join " "
    (map str/capitalize
         (str/split string #" "))))

; Uppercases the first character.
(defalias ->capital-case str+/capital  )
(defalias capitalize     ->capital-case)
; Output will be: lowerUpperUpperNoSpaces.
(defalias ->camel-case   str+/camel    )
; Output will be: lower_cased_and_underscore_separated.
(defalias ->snake-case   str+/snake    )
; Output will be: Space separated with the first letter capitalized.
(defalias ->phrase-case  str+/phrase   )
; Output will be: lower cased and space separated
(defalias ->human-case   str+/human    )
; Output will be: Each Word Capitalized And Separated With Spaces
(defalias ->title-case   str+/title    )
; Output will be: CapitalizedAndTouchingTheNext
(defalias ->pascal-case  str+/pascal   )
; Output will be: lower-cased-and-separated-with-dashes
(defalias ->kebab-case   str+/kebab    )
(defalias ->spear-case   ->kebab-case  )
(defalias ->lisp-case    ->kebab-case  )

(defn java->clojure
  {:source "zcaudate/hara.object.util"}
  [^String name]
  (let [nname (cond (re-find #"(^get)|(^set)[A-Z].+" name)
                    (subs name 3)

                    (re-find #"^is[A-Z].+" name)
                    (str (subs name 2) "?")

                    (re-find #"^has[A-Z].+" name)
                    (str (subs name 3) "!")

                    :else name)]
    (->lisp-case nname)))

(defn clojure->java
  {:source "zcaudate/hara.object.util"}
  ([name] (clojure->java name :get))
  ([^String name suffix]
   (let [nname (cond (str/ends-with? name "?")
                     (str "is-" (.substring name 0 (.length name)))

                     (str/ends-with? name "!")
                     (str "has-" (.substring name 0 (.length name)))

                     :else
                     (str (clojure.core/name suffix) "-" name))]
     (->camel-case nname))))

; Unindent multiline text.
; Uses either a supplied regex or the shortest
; beginning-of-line to non-whitespace distance.
(defalias unindent str+/<<-)
