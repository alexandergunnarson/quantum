(ns
  ^{:doc "Regex utils"
    :attribution "Alex Gunnarson"}
  quantum.core.string.regex
  (:require
    #?(:cljs [goog.string    :as gstr])
             [clojure.string :as str])
  #?(:clj  (:import java.util.regex.Pattern)))

(defn escape
  "Escapes characters in the string that are not safe
   to use in a RegExp."
   {:attribution "funcool/cuerdas"}
  [s]
  #?(:clj  (Pattern/quote ^String s)
     :cljs (gstr/regExpEscape s)))

; http://stackoverflow.com/questions/22945910/what-regex-is-b-equivalent-to-and-is-there-a-way-to-deparse-it
#_(= #"(?:(?<!\w)(?=\w)|(?<=\w)(?!\w))"
     #"\b")

(defn c  "Creates a capturing group."     [& xs] (str "(" (apply str xs) ")"))
(defn nc "Creates a non-capturing group." [& xs] (str "(?:" (apply str xs) ")"))
(defn ?  "Denotes optionality."           [x] (str x "?"))

(defn alts
  "Creates a regex alts-group, each element of which is a non-capturing group.
   That is:
   `((?:a)|(?:b)|...)`"
  [& xs]
  (str "(" (->> xs (map nc) (str/join "|")) ")"))
