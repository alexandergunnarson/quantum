(ns
  ^{:doc "Regex utils"
    :attribution "alexandergunnarson"}
  quantum.core.string.regex
  (:refer-clojure :exclude [conj! range])
  (:require
    #?(:cljs [goog.string    :as gstr])
             [clojure.string :as str]
             [quantum.core.data.string
               :refer [!str]])
  (:import
   #?(:clj java.util.regex.Pattern)
   #?(:cljs goog.string.StringBuffer)))

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

; TODO: not using ccoll because this namespace is required for `defnt`
(defn conjr!* [#?(:clj ^StringBuilder !s :cljs StringBuffer !s) x] (.append !s x))
(defn conjl!* [#?(:clj ^StringBuilder !s :cljs StringBuffer !s) x]
  #?(:clj  (.insert !s 0 x)
     :cljs (-> (!str) (.append x) (.append !s))))

(defn c   "Creates a capturing group."             [& xs] (str "(" (apply str xs) ")"))
(defn !c  "Creates a mutable capturing group."     [x] (-> x (conjl!* "(") (conjr!* ")")))
(defn nc  "Creates a non-capturing group."         [& xs] (str "(?:" (apply str xs) ")"))
(defn !nc "Creates a mutable non-capturing group." [x] (-> x (conjl!* "(?:") (conjr!* ")")))
(defn ?   "Denotes optionality."                   [x] (str x "?"))
(defn !?  "Denotes mutable optionality."           [x] (conjr!* x "?"))

(defn bounded "Surrounds with start and end bounds." [& xs]
  (str "^" (apply nc xs) "$"))

(defn !bounded "Mutably surrounds with start and end bounds." [x]
  (-> x (conjl!* "^") (conjr!* "$")))

(defn range "Char range" [& xs] (str "[" (apply str xs) "]"))

(defn alts
  "Creates a regex alts-group, each element of which is a non-capturing group.
   That is:
   `((?:a)|(?:b)|...)`"
  [& xs]
  (str "(" (->> xs (map nc) (str/join "|")) ")"))
