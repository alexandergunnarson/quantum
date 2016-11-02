(ns
  ^{:doc "Regex utils"
    :attribution "Alex Gunnarson"}
  quantum.core.string.regex
  #?(:cljs (:require [goog.string :as gstr]))
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
