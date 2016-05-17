(ns quantum.core.string.semantic
           (:refer-clojure :exclude [every?])
           (:require [quantum.core.logic  :as logic
                       :refer [nempty? every? any?]]
                     [quantum.core.macros :as macros
                       :refer [#?@(:clj [defnt])]   ])
  #?(:cljs (:require-macros
                     [quantum.core.macros :as macros
                       :refer [defnt]               ])))

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
  ([^string? x] (any? ext-vowels x)))