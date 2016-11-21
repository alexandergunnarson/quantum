(ns quantum.core.string.semantic
  (:require
    [clojure.set         :as set]
    [cuerdas.core        :as str+]
    [quantum.core.logic  :as logic
      :refer [nempty?]]
    [quantum.core.macros :as macros
      :refer [defnt]]
    [quantum.core.vars   :as var
      :refer [defalias]]))

(def vowels
  #{\a \e \i \o \u
    \A \E \I \O \U})

(def ext-vowels
  (set/union vowels #{\y \Y}))

(defnt vowel?
  ([#{#?(:clj char? :cljs string?)} x] (vowels x)))

(defnt ext-vowel?
  ([#{#?(:clj char? :cljs string?)} x] (ext-vowels x)))

(defnt contains-ext-vowel?
  ([^string? x] (some ext-vowels x)))

(def ^{:doc "Specifically in English"} punctuation
  #{\[ \] \( \) \{ \} \< \> \:
    \, \; \- \– \— \! \? \. \… \` \' \" \/})

; Returns a vector of the words in the string.
(defalias words str+/words)
