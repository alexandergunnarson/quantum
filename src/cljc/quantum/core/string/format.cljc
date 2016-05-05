(ns
  ^{:doc "Useful string utils for formatting strings."
    :attribution "Alex Gunnarson"}
  quantum.core.string.format
           (:refer-clojure :exclude [replace])
           (:require [frak]
                     [clojure.string                :as str   ]
                     [#?(:clj  clojure.core
                         :cljs cljs.core   )        :as core  ]
                     [quantum.core.collections.base :as cbase ]
            #?(:cljs [goog.string                   :as gstr  ])
                     [quantum.core.fn               :as fn
                       :refer [#?@(:clj [fn->>])]             ]
                     [quantum.core.loops            :as loops
                       :refer [#?@(:clj [reduce])]            ]
                     [quantum.core.macros           :as macros
                       :refer [#?@(:clj [defnt])]             ]
                     [quantum.core.string.regex     :as regex ]
                     [quantum.core.vars             :as var
                       :refer [#?@(:clj [defalias])]          ])
  #?(:cljs (:require-macros
                     [quantum.core.fn               :as fn
                       :refer [fn->>]                         ]
                     [quantum.core.loops            :as loops
                       :refer [reduce]                        ]
                     [quantum.core.macros           :as macros
                       :refer [defnt]                         ]
                     [quantum.core.vars             :as var
                       :refer [defalias]                      ]))
  #?(:clj (:import java.net.IDN)))

(defnt replace* ; TODO .replace may try to compile a pattern instead of replacing the actual string
  ([^string? pre post s] (.replace    ^String s pre ^String post))
  ([^regex?  pre post s] (str/replace         s pre         post)))

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
          ([^string? s] (.toLowerCase s))
  #?(:clj ([^char?   c] (Character/toLowerCase c))))
(defalias ->lower ->lower-case)

(defnt ->upper-case
          ([^string? s] (.toUpperCase s))
  #?(:clj ([^char?   c] (Character/toUpperCase c))))
(defalias ->upper ->upper-case)

; CANDIDATE 0
(def capitalize  str/capitalize)

; CANDIDATE 1
#_(defn capitalize
  "Converts first letter of the string to uppercase."
  {:attribution "funcool/cuerdas"}
  [s]
  (when-not (nil? s)
    (-> (.charAt ^String s 0)
        #?(:clj (String/valueOf))
        ->upper
        (str (slice s 1)))))

(defn capitalize-each-word [string]
  (str/join " "
    (map str/capitalize
         (str/split string #" "))))

(def hump-pattern #"[a-z0-9][A-Z]")
(def non-camel-separator-pattern #"[_| |\-][A-Za-z]")
(def non-snake-separator-pattern #"[ |\-]")
(def non-spear-separator-pattern #"[ |\_]")

(defalias camelcase cbase/camelcase)

(defn un-camelcase
  {:todo ["Find more efficient algorithm"]}
  [sym]
  (let [str-0 (str sym)
        matches (->> str-0 (re-seq #"[a-z0-1][A-Z]") distinct)]
    (-> (reduce
          (fn [ret [char1 char2 :as match]]
            (replace ret match (str char1 "-" (->lower-case char2))))
          str-0 matches)
        ->lower-case)))

(declare gsub)

(defn separate-camel-humps
  {:attribution "zcaudate/hara"}
  [value]
  (gsub value hump-pattern
    #(fn->> seq (str/join " "))))

; CANDIDATE 0
(defn ->title-case
  "Human readable."
  {:attribution "zcaudate/hara"
   :tests {["hello-world"] "Hello World"}}
  [value]
  (str/join " "
    (map capitalize
      (str/split (separate-camel-humps value) #"[ |\-|_]"))))

; CANDIDATE 1
#?(:clj
(defn ->title-case*
  "Converts a string into TitleCase."
  ([s] (->title-case* s nil))
  ([s delimeters-0]
    (when-not (nil? s)
      (let [delimeters (if delimeters-0
                         (regex/escape delimeters-0)
                         "\\s")
            delimeters (str "|[" delimeters "]+")
            rx         (re-pattern (str "(^" delimeters ")([a-z])"))]
        (replace s rx (fn [[c1 _]]
                        (->upper c1))))))))

; CANDIDATE 1
#?(:cljs
(defn ->title-case*
  "Converts a string into TitleCase."
  ([s]
    (when (string? s)
      (gstr/toTitleCase s)))
  ([s delimiters]
    (gstr/toTitleCase s delimiters))))

(defn ->camel-case
  "Java, JavaScript, etc."
  {:attribution "zcaudate/hara"
   :tests {["hello-world"] "helloWorld"}}
  [value]
  (gsub value non-camel-separator-pattern
    #(->upper-case (apply str (rest %)))))

; CANDIDATE 0
(defn ->capital-camel-case
  "C#, Java classes"
  {:attribution "zcaudate/hara"
   :tests {["hello-world"] "HelloWorld"}}
  [value]
  (let [^String camel (->camel-case value)]
   (str (->upper-case (.substring camel 0 1))
     (.substring camel 1 (.length camel)))))

; CANDIDATE 1
#_(defn ->capital-camel-case ;'classify'
  "Converts string to camelized class name. First letter is always upper case."
  {:attribution "funcool/cuerdas"}
  [s]
  (some-> s
          (str)
          (replace #"[\W_]" " ")
          (camelize)
          (replace #"\s" "")
          (capitalize)))

; CANDIDATE 0
(defn ->snake-case
  "Python, C, some C++"
  {:attribution "zcaudate/hara"
   :tests {["hello-world"] "hello_world"}}
  [value]
  (replace
    (->lower-case (separate-camel-humps value))
    non-snake-separator-pattern
    "_"))

; CANDIDATE 1
#_(defn ->snake-case
  "Converts a camelized or dasherized string
  into an underscored one."
  {:attribution "funcool/cuerdas"}
  [s]
  (some-> s
          (trim)
          (replace #?(:clj  #"([a-z\d])([A-Z]+)"
                      :cljs (->regexp #"([a-z\d])([A-Z]+)" "g"))"$1_$2")
          (replace #?(:clj  #"[-\s]+"
                      :cljs (->regexp #"[-\s]+", "g")) "_")
          (lower)))

(defn ->spear-case
  "Lisps"
  {:attribution "zcaudate/hara"
   :tests {["Hello World"] "hello-world"}}
  [value]
  (replace
    (->lower-case (separate-camel-humps value))
    non-spear-separator-pattern
    "-"))

(defn ->human-case
  "Converts an underscored, camelized, or
  dasherized string into a humanized one."
  {:attribution "funcool/cuerdas"}
  [s]
  (some-> s
          ->snake-case
          (replace #"_id$", "")
          (replace #?(:clj "_" :cljs (->regexp "_" "g")) " ")
          (capitalize)))