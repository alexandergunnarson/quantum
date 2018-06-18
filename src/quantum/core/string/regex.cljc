(ns
  ^{:doc "Regex utils"
    :attribution "alexandergunnarson"}
  quantum.core.string.regex
  (:refer-clojure :exclude [conj! range concat find])
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

#?(:clj
(defn ->caseless-regex [^String s]
  (Pattern/compile s (bit-or Pattern/CASE_INSENSITIVE Pattern/UNICODE_CASE))))

; http://stackoverflow.com/questions/22945910/what-regex-is-b-equivalent-to-and-is-there-a-way-to-deparse-it
#_(= #"(?:(?<!\w)(?=\w)|(?<=\w)(?!\w))"
     #"\b")

; TODO: not using ccoll because this namespace is required for `defnt`
(defn conjr!* [#?(:clj ^StringBuilder !s :cljs ^StringBuffer !s) x] (.append !s x))
(defn conjl!* [#?(:clj ^StringBuilder !s :cljs ^StringBuffer !s) x]
  #?(:clj  (.insert !s 0 x)
     :cljs (-> (!str) (.append x) (.append !s))))

(defn c   "Creates a capturing group."             [& xs] (str "(" (apply str xs) ")"))
(defn !c  "Creates a mutable capturing group."     [x] (-> x (conjl!* "(") (conjr!* ")")))
(defn nc  "Creates a non-capturing group."         [& xs] (str "(?:" (apply str xs) ")"))
(defn !nc "Creates a mutable non-capturing group." [x] (-> x (conjl!* "(?:") (conjr!* ")")))
(defn ?   "Denotes optionality."                   [x] (str x "?"))
(defn !?  "Denotes mutable optionality."           [x] (conjr!* x "?"))

(defn concat [& xs] (->> xs (map str) (map nc) (apply str)))

(defn bounded "Surrounds with start and end bounds." [& xs]
  (str "^" (apply nc xs) "$"))

(defn !bounded "Mutably surrounds with start and end bounds." [x]
  (-> x (conjl!* "^") (conjr!* "$")))

(defn range "Char range" [& xs] (str "[" (apply str xs) "]"))
(defn not-range "Not- char range" [& xs] (str "[^" (apply str xs) "]"))

(defn alts
  "Creates a regex alts-group, each element of which is a non-capturing group.
   That is:
   `((?:a)|(?:b)|...)`"
  [& xs]
  (str "(" (->> xs (map nc) (str/join "|")) ")"))

(def pattern re-pattern)
(def find    re-find)
(def matches re-matches)
