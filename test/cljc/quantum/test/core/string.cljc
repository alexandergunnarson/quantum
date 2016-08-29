(ns quantum.test.core.string
  (:require [quantum.core.string :as ns]))

; ===== WHOLE PREDICATES =====

(defn test:numeric?         [x])
(defn test:upper?           [x])
(defn test:lower?           [x])
(defn test:alpha?           [x])
(defn test:alphanum?        [x])
(defn test:blank?           [x])
(defn test:line-terminator? [x])

; ===== PARTIAL PREDICATES =====

(defn test:starts-with? [super sub])

(defn test:ends-with? [super sub])

(defn test:->pattern [x])

#?(:clj
(defn test:contains-pattern? [x pattern]))

; ===== SPLIT/JOIN =====

(defn test:split-by-regex
  [s pattern])

(defn test:split-by-comma
  [s])

(defn test:join
  ([coll])
  ([separator coll]))

(defn test:join-once
  [separator & coll])

(defn test:->path [& xs])

; ===== REPLACEMENT =====

(defn test:replace-with
  [s m])

(defn test:replace-uchars [x])

; ===== TRIMMING =====

(defn test:trim
  ([s])
  ([s chs]))

(defn test:triml
  ([s])
  ([s chs]))


(defn test:trimr
  ([s])
  ([s chs]))

(defn test:collapse-whitespace [s])

(defn test:clean [s])

(defn test:str-nil [x])

; ===== COERCION =====

(defn test:keyword+
  ([obj])
  ([obj & objs]))

; ===== CHAR COERCION =====

#?(:clj
(defn test:char->hex-code [c]))

#?(:clj (defn test:char->unicode    [c]))
#?(:clj (defn test:char->regex-code [c]))

; ===== SUBSEQ =====

; TODO For now
(defn test:subs+ ; :from :to, vs. :from :for
  ([s start-idx])
  ([s start-idx max-len]))

; ===== REMOVE =====

(defn test:remove-all [str-0 to-remove])

(defn test:remove* [a b])

(defn test:remove
  ([str-0 to-remove]))

(defn test:remove-from-end [^String string ^String end])

; ===== KEYWORDIZATION =====

(defn test:keywordize
  [^String x])

(defn test:properize-keyword [x])

(defn test:properize-key [k v])


(defn test:unkeywordize
  ([k]))

; ===== PUNCTUATION =====

(defn test:squote  [x])

(defn test:dquote  [x])

(defn test:paren   [x])

(defn test:bracket [x])

(defn test:sp
  [& args])

(defn test:sp-comma
  [& args])

; ===== REGEX =====

(defn test:re-find [pat ^String in-str])

#?(:clj
(defn test:re-find-all
  [re s]))

(defn test:re-get
  [regex ^String string])

(defn test:conv-regex-specials [^String str-0])

(defn test:re-index
  [reg s])

(defn test:re-preview [reg ^String s & [preview-ct]])

#?(:clj
(defn test:gsub
  [^String value pattern sub-fn]))

#?(:clj
(defn test:regionMatches
  [cs
   ignore-case?
   thisStart
   substring
   start
   length]))


; ===== STRING VALUES =====
   
(defn test:val [obj])

; ===== LANGUAGE STYLE REPLACEMENTS =====

; TODO MOVE NAMESPACE
(defn test:java->clojure
  [^String name])

(defn test:clojure->java
  ([name])
  ([^String name suffix]))

; ===== MISCELLANEOUS =====

(defn test:reverse [s])

(defn test:camelize [x])

(defn test:kebabize [x])

#?(:clj
(defn test:remove-accents [^String s]))

(defn test:search-str-within [super sub])