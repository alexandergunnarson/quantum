(ns quantum.core.string
  (:refer-clojure :exclude [replace contains?])
  (:require-quantum [:core])
  (:require [clojure.string :as str]))

(defn sp [x])

; ===== SPLIT/JOIN =====

;(defnt starts-with?
;  {:todo ["Make more portable by looking at java.lang.String/startsWith"]}
;  ([^string? super sub]
;    #?(:clj  (.startsWith super ^String sub)         
;       :cljs (zero? (.indexOf super sub)))) ; .startsWith is not implemented everywhere
;  ([^keyword? super sub]
;    (starts-with? (name super) sub)))

(defn starts-with?
  {:todo ["Make more portable by looking at java.lang.String/startsWith"]}
  ([super sub]
    #?(:clj  (.startsWith ^String super sub)         
       :cljs (zero? (.indexOf super sub))))) ; .startsWith is not implemented everywhere

;(defnt ends-with?
;  {:todo ["Make more portable by looking at java.lang.String/endsWith"]}
;  ([^string? super sub]
;    #?(:clj  (.endsWith super ^String sub)         
;       :cljs (.endsWith super         sub))) ; .endsWith is not implemented everywhere
;  ([^keyword? super sub]
;    (ends-with? (name super) sub)))

(defn ends-with?
  {:todo ["Make more portable by looking at java.lang.String/endsWith"]}
  ([super sub]
    #?(:clj  (.endsWith super ^String sub)         
       :cljs (.endsWith super         sub)))) ; .endsWith is not implemented everywhere

(defalias split       str/split)

; a form of |concat| - "concat-with"
; CANDIDATE 0
(defalias join        str/join)

; CANDIDATE 1
#_(defn join
  "Joins strings together with given separator."
  {:attribution "funcool/cuerdas"}
  ([coll]
   (apply str coll))
  ([separator coll]
   (apply str (interpose separator coll))))

(defn join-once
  "Like /clojure.string/join/ but ensures no double separators."
  {:attribution "taoensso.encore"}
  [separator & coll]
  (reduce
    (fn [s1 s2]
      (let [s1 (str s1) s2 (str s2)]
        (if (ends-with? s1 separator)
            (if (starts-with? s2 separator)
                (str s1 (.substring s2 1))
                (str s1 s2))
            (if (starts-with? s2 separator)
                (str s1 s2)
                (if (or (= s1 "") (= s2 ""))
                    (str s1 s2)
                    (str s1 separator s2))))))
    nil
    coll))

; SPLITTING

(defn split-by-regex
  "Split the string `s` by the regex `pattern`."
  {:from "r0man/noencore"}
  [s pattern]
  (if (sequential? s)
      s
      (when-not (str/blank? s)
        (str/split s pattern))))

(defn split-by-comma
  "Split the string `s` by comma."
  [s] (split-by-regex s #"\s*,\s*"))

; CONVERT

(defn camelize
  "Returns dash separated string @s in camel case."
  [s]
  (->> (str/split (str s) #"-")
       (map str/capitalize)
       (str/join "-")))

(defn kebabize [s]
  (-> s 
      str/lower-case
      (str/replace #"_" "-")))

(defalias replace str/replace)

(defn contains? [s sub]
  (not= (.indexOf ^String s sub) -1))

; CLOJURE IMPLEMENTATION
; (defn whitespace?
;   "Checks whether a given character is whitespace"
;   [ch]
;   (when ch
;     (or (Character/isWhitespace ^Character ch)
;         (identical? \,  ch))))

; (defn numeric?
;   "Checks whether a given character is numeric"
;   [^Character ch]
;   (when ch
;     (Character/isDigit ch)))

; (defn newline?
;   "Checks whether the character is a newline"
;   [c]
;   (or (identical? \newline c)
;       (nil? c)))

; CLOJURESCRIPT IMPLEMENTATION
; (defn ^boolean whitespace?
;   "Checks whether a given character is whitespace"
;   [ch]
;   (when-not (nil? ch)
;     (if (identical? ch \,)
;       true
;       (.test ws-rx ch))))

; (defn ^boolean numeric?
;   "Checks whether a given character is numeric"
;   [ch]
;   (when-not (nil? ch)
;     (gstring/isNumeric ch)))

; (defn ^boolean newline?
;   "Checks whether the character is a newline"
;   [c]
;   (or (identical? \newline c)
;       (identical? "\n" c)
;       (nil? c)))
