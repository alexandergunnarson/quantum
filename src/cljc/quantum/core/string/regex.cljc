(ns
  ^{:doc "Useful string utils. Aliases clojure.string.

          Includes squote (single-quote), sp (spaced),
          val (reading a number of a string), keyword+
          (for joining strings and keywords into one
          keyword), etc."
    :attribution "Alex Gunnarson"}
  quantum.core.string.regex
  (:refer-clojure :exclude [reverse replace remove val re-find])
  (:require-quantum [:core fn set map macros logic red type loops cbase log err])
  (:require
    [frak]
    #?(:cljs [goog.string :as gstr]))
  #?(:clj (:import java.util.regex.Pattern)))

(defn escape
  "Escapes characters in the string that are not safe
   to use in a RegExp."
   {:attribution "funcool/cuerdas"}
  [s]
  #?(:clj  (Pattern/quote ^String s)
     :cljs (gstr/regExpEscape s)))