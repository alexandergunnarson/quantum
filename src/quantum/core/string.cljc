(ns
  ^{:doc "Useful string utils. Aliases clojure.string.

          Includes squote (single-quote), sp (spaced),
          val (reading a number of a string), keyword+
          (for joining strings and keywords into one
          keyword), etc."
    :attribution "alexandergunnarson"}
  quantum.core.string
  (:refer-clojure :exclude
    [reverse replace remove val re-find reduce])
  (:require
    [clojure.core                :as core]
    [clojure.string              :as str]
    [frak]
    [cuerdas.core                :as str+]
    [quantum.core.data.primitive :as p]
    [quantum.core.data.map       :as map]
    [quantum.core.data.set       :as set]
    [quantum.core.error
      :refer [>ex-info]]
    [quantum.core.fn             :as fn
      :refer [fn-> fn1 rfn fnl]]
    [quantum.core.logic          :as logic
      :refer [fn-and whenc whenc1 ifn condf]]
    [quantum.core.loops          :as loops
      :refer [reduce reducei]]
    [quantum.core.macros         :as macros
      :refer [defnt defnt']]
    [quantum.core.collections.core
      :refer [contains? containsv?]]
    [quantum.core.collections.logic
      :refer [seq-and]]
    [quantum.core.string.format  :as form]
    [quantum.core.string.regex   :as regex]
    [quantum.core.vars           :as var
      :refer [defalias]]
    [quantum.core.type-old       :as t
      :refer [val?]])
#?(:cljs
  (:require-macros
    [quantum.core.string
      :refer [starts-with? ends-with? remove*]]))
#?(:clj
  (:import
    java.net.IDN
    java.util.regex.Pattern)))

(def
  ^{:todo
      {0 "alias cuerdas.core and review the duplication of certain of these functions"}
    :explorations
      #{"https://github.com/expez/superstring"
        "http://www.regular-expressions.info"}
    :ideas
      ["What about structural sharing with strings? Wouldn't there have to be some
        sort of compact immutable bit map or something to diff it rather than just
        making an entirely new string?"]}
  annotations nil)

#?(:clj
(defn upper-first
  {:todo {0 "rename/move/delete"}}
  [s] (apply str (.toUpperCase (str (first s))) (rest s))))

; ===== CHARS =====

(def int->num-char
  (#?@(:clj [vector-of :char] :cljs [vector])
    \0 \1 \2 \3 \4 \5 \6 \7 \8 \9))
(def num-char->int (zipmap int->num-char (range)))
(def num-chars (set int->num-char))

(def int->upper-char
  (#?@(:clj [vector-of :char] :cljs [vector])
    \A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T \U \V \W \X \Y \Z))
(def upper-char->int (zipmap int->upper-char (range)))
(def upper-chars (set int->upper-char))

(def int->lower-char
  (#?@(:clj [vector-of :char] :cljs [vector])
    \a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z))
(def lower-char->int (zipmap int->lower-char (range)))
(def lower-chars (set int->lower-char))

(def alpha-chars    (set/union upper-chars lower-chars))
(def alphanum-chars (set/union alpha-chars num-chars))

(def line-terminator-chars
  #{\newline \return (char 0x2028) (char 0x2029)})

(declare line-terminator-chars-regex)

(def whitespace-chars
  (set/union
    line-terminator-chars
    #{\space \tab (char 12)
      (char 11) (char 0xa0)}))

; ===== WHOLE PREDICATES =====

(defnt numeric?
#?(:clj
  ([^char    c] (contains? num-chars c)))
  ([^string? s] (and (contains? s) (seq-and (fnl contains? num-chars) s))))

(defnt numeric-readable?
#?(:clj
  ([^char    x] (numeric? x)))
  ([^string? x] (str+/numeric? x)))

(defnt upper?
#?(:clj
  ([^char    c] (contains? upper-chars c)))
  ([^string? s] (and (contains? s) (seq-and (fnl contains? upper-chars) s))))

(defalias ->upper form/->upper)
; Converts string to all upper-case respecting
; the current system locale.
; On the JVM you can provide a concrete locale to
; use as the second optional argument.
(defalias ->locale-upper str+/locale-upper)

(defnt lower?
#?(:clj
  ([^char    c] (contains? lower-chars c)))
  ([^string? s] (and (contains? s) (seq-and (fnl contains? lower-chars) s))))

(defalias ->lower form/->lower)
; Converts string to all lower-case respecting
; the current system locale.
; On the JVM you can provide a concrete locale to
; use as the second optional argument.
(defalias ->locale-lower str+/locale-lower)

(defnt alpha?
#?(:clj
  ([^char    c] (contains? alpha-chars c)))
  ([^string? s] (and (contains? s) (seq-and (fnl contains? alpha-chars) s))))

(defnt alphanum?
#?(:clj
  ([^char    c] (contains? alphanum-chars c)))
  ([^string? s] (and (contains? s) (seq-and (fnl contains? alphanum-chars) s))))

(defnt letters?
  "Checks if string contains only letters.
   This function will use all the unicode range."
  ([^string? x] (str+/letters? x)))

(defnt word?
  "Checks if a string contains only the word characters.
   This function will use all the unicode range."
  ([^string? x] (str+/word? x)))

(defnt blank?
#?(:clj
  ([^char    c] (contains? whitespace-chars c)))
  ([^string? s] (seq-and (fnl contains? whitespace-chars) s)))

(def whitespace? (fn-and contains? blank?))

(defnt line-terminator?
#?(:clj
  ([^char    c] (contains? line-terminator-chars c)))
  ([^string? s] (and (contains? s) (seq-and (fnl contains? line-terminator-chars) s))))

(defalias capitalize form/capitalize)

; ===== PARTIAL PREDICATES =====

(defnt starts-with?
  "Check if the string starts with prefix."
  ([^string? super sub] (str+/starts-with? super sub))
  ([^keyword? super sub]
    (starts-with? (name super) sub)))

(defnt ends-with?
  "Check if the string ends with suffix."
  ([^string? super sub] (str+/ends-with? super sub))
  ([^keyword? super sub]
    (ends-with? (name super) sub)))

(defalias
  ^{:doc "Transforms collections of strings into regexes for matching those strings.
          (frak/pattern [\"foo\" \"bar\" \"baz\" \"quux\"])
          #\"(?:ba[rz]|foo|quux)\"
          user> (frak/pattern [\"Clojure\" \"Clojars\" \"ClojureScript\"])
          #\"Cloj(?:ure(?:Script)?|ars)\""}
  vec->pattern frak/pattern)

(defnt ->pattern
  ([^string? x] (re-pattern   x))
  ([^regex?  x] x)
  ([^vector?    x] (vec->pattern x)))

#?(:clj
(defnt contains-pattern?
  ([^string? x pattern] (-> pattern ^Pattern ->pattern-protocol (.matcher x) .find))))

; ===== SPLIT/JOIN =====

(defnt split*
  #?(:cljs ([^nil?    sep x  ] x))
  #?(:cljs ([^nil?    sep x n] x))
           ([^string? sep x  ] (str/split x (re-pattern (regex/escape sep))  ))
           ([^string? sep x n] (str/split x (re-pattern (regex/escape sep)) n))
           ([^regex?  sep x  ] (str/split x sep  ))
           ([^regex?  sep x n] (str/split x sep n))
   #?(:clj ([^default sep x  ] (if (nil? sep) x (throw (>ex-info "`split*` not supported for type" {:type (type sep)})))))
   #?(:clj ([^default sep x n] (if (nil? sep) x (throw (>ex-info "`split*` not supported for type" {:type (type sep)}))))))

(defnt split
  #?(:cljs ([^nil?             x sep  ] x))
  #?(:cljs ([^nil?             x sep n] x))
           ([#{string? regex?} x sep  ] (split*-protocol sep x  )) ; TODO deprotocolize
           ([#{string? regex?} x sep n] (split*-protocol sep x n)) ; TODO deprotocolize
   #?(:clj ([^default          x sep  ] (if (nil? x) x (throw (>ex-info "`split` not supported for type" {:type (type x)})))))
   #?(:clj ([^default          x sep n] (if (nil? x) x (throw (>ex-info "`split` not supported for type" {:type (type x)}))))))

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

(defn split-by-lines
  "Return a list of the lines in the string."
  [s] (split-by-regex s #"\n|\r\n"))

; a form of |concat| - "concat-with"
; CANDIDATE 0
(defalias join str+/join)

(defn join-once
  "Like /clojure.string/join/ but ensures no double separators."
  {:attribution "taoensso.encore"}
  [separator & coll]
  (reduce
    (fn [s1 s2]
      (let [s1 (str s1) s2 (str s2)]
        (if (ends-with? s1 separator) ; could use ends-with? but would be self-referring
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

(defn ->path [& xs]
  (apply join-once "/" xs)) ; TODO fix

; ===== REPLACEMENT =====

(defalias replace form/replace)

(defn replace-with
  "Replace all."
  {:in '["and" (om "a" "abc")]
   :out "abcnd"}
  [s m]
  (reduce
    (rfn [ret old-n new-n]
      (replace ret old-n new-n)) s m))

; Replaces first instance of match with replacement in s.
(defalias replace-first str+/replace-first)

(def replace-uchars ; TODO much more to this
  (fn-> (str/replace "\\u0026" "&")))

; ===== WHITESPACE =====

(defalias trim   str+/trim )
(defalias strip  trim      )
(defalias triml  str+/ltrim)
(defalias stripl triml     )
(defalias trimr  str+/rtrim)
(defalias stripr trimr     )

(defalias strip-prefix str+/strip-prefix)
(defalias strip-suffix str+/strip-suffix)

; Takes a string and replaces newlines with a space.
; Multiple lines are replaced with a single space.
(defalias newlines->space str+/strip-newlines)

; Converts all adjacent whitespace characters to a single space.
(defalias collapse-whitespace str+/collapse-whitespace)
; Trim and collapse whitespace.
(defalias clean               str+/clean)

; Truncates a string to a certain length based on word boundary
; and adds '...' if necessary.
(defalias ellipsize           str+/prune)

(def str-nil (whenc1 nil? ""))

; ===== COERCION =====

(defn keyword+
  "Like |str| but for keywords."
  ([obj]
    (condf obj
      keyword? identity
      string?  keyword))
  ([obj & objs]
    (->> (cons obj objs)
         (reduce
           (fn [ret elem] ; elem might be string or keyword
             (str ret (name elem)))
           "")
         keyword)))

; ===== CHAR COERCION =====

#?(:clj
(defn char->hex-code [c]
  (let [^String hex (Integer/toHexString (bit-or (int c) 0x10000))]
    (.substring hex 0 (-> hex count dec)))))

#?(:clj (defn char->unicode    [c] (str "\\u" (char->hex-code c))))
#?(:clj (defn char->regex-code [c] (str "\\x" (char->hex-code c))))

; ===== SUBSEQ =====

; TODO For now
#_(defn subs+ ; :from :to, vs. :from :for
  "Gives a consistent, flexible, cross-platform substring API with support for:
    * Clamping of indexes beyond string limits.
    * Negative indexes: [   0   |   1   |  ...  |  n-1  |   n   ) or
                        [  -n   | -n+1  |  ...  |  -1   |   0).
                        (start index inclusive, end index exclusive).

  Note that `max-len` was chosen over `end-idx` since it's less ambiguous and
  easier to reason about - esp. when accepting negative indexes."
  {:usage
    ["(subs+ \"Hello\"  0 5)" "Hello"
     "(subs+ \"Hello\"  0 9)" "Hello"
     "(subs+ \"Hello\" -4 5)" "Hello"
     "(subs+ \"Hello\"  2 2)" "ll"
     "(subs+ \"Hello\" -2 2)" "ll"
     "(subs+ \"Hello\" -2)  " "llo"
     "(subs+ \"Hello\"  2)  " "llo"
     "(subs+ \"Hello\"  9 9)" ""
     "(subs+ \"Hello\"  0 0)" ""]
   :attribution "taoensso.encore"}
  ([s ^long start-idx] (subs+ s start-idx nil))
  ([s ^long start-idx max-len]
    {:pre [(or (nil? max-len) (num/nneg-int? max-len))]}
    (let [;; s       (str   s)
          slen       (count s)
          start-idx* (if (>= start-idx 0)
                       (min start-idx slen)
                       (max 0 (dec (+ slen start-idx))))
          end-idx*   (if-not max-len slen
                       (min (+ start-idx* max-len) slen))]
      ;; (println [start-idx* end-idx*])
      (.substring ^String s start-idx* end-idx*))))

; ===== REMOVE =====

(declare conv-regex-specials)

(defn remove-all [str-0 to-remove]
  (reduce #(replace %1 %2 "") str-0 to-remove))

(defnt remove*
  ([^string? to-remove str-0]
    (.replaceAll ^String str-0 (conv-regex-specials to-remove) ""))
  ([^regex?  to-remove str-0]
    (replace str-0 to-remove "")))

(defnt remove
  ([^string? str-0 to-remove]
    (remove*-protocol to-remove str-0))) ; TODO deprotocolize

(defn remove-from-end [^String string ^String end]
  (if (ends-with? string end)
      (.substring string 0 (- (count string)
                         (count end)))
      string))

; ===== KEYWORDIZATION =====

(defnt keywordize
  "Transforms string @`x` to a lisp-case keyword."
  [^string? x] (-> x form/->lisp-case keyword))

(def properize-keyword
  (fn-> (ifn nil? str-nil name) (replace #"\-" " ") form/capitalize-each-word))

(defn properize-key [k v]
  (let [k-0 (keywordize k)
        k-f (if (p/boolean? v)
                (keyword+ k-0 "?")
                k-0)]
    k-f))

; ===== PUNCTUATION =====

(def quote-types #{\' \"})

(defn wrap
  "Wraps the string representation of ->`x` in the string representation of ->`to-wrap`."
  [x to-wrap] (str to-wrap x to-wrap))

(defn squote "Wraps the string representation of ->`x` in single quotes." [x] (wrap x \'))
(defn dquote "Wraps the string representation of ->`x` in double quotes." [x] (wrap x \"))

(defn paren   "Wraps the string representation of ->`x` in parentheses."  [x] (str \( x \)))

(defn bracket "Wraps the string representation of ->`x` in brackets."     [x] (str \[ x \]))

; (defn dequote
;   {:in  "'Abcdef'"
;    :out "Abcdef"}
;   [s]
;   (if (and  (= (first s) (last s))
;         (val? (partial starts-with? s) quote-types)
;         (val? (partial ends-with?   s) quote-types))
;       (-> s popl popr) ; TODO this will be optimized away
;       s))


(defn sp
  "Like |str|, but adds spaces between the arguments."
  [& args]
  (reducei
    (fn [ret elem n]
      (if (= n (dec (count args)))
          (str ret elem)
          (str ret elem \space
            )))
    ""
    args))
(defn sp-comma
  "Like |sp|, but adds commas and spaces between the arguments."
  [& args]
  (reducei
    (fn [ret elem n]
      (if (= n (dec (count args)))
          (str ret elem)
          (str ret elem ", ")))
    ""
    args))

; Pads the str with characters until the total string
; length is equal to the passed length parameter. By
; default, pads on the left with the space char.
(defalias pad str+/pad)

; ===== REGEX =====
(defn re-find [pat ^String in-str]
  (try (core/re-find (re-pattern pat) in-str)
    (catch #?(:clj  NullPointerException
              :cljs js/TypeError) _
      nil)))

#?(:clj
(defn re-find-all
  "Returns a lazy sequence of successive matches of pattern in string,
   using java.util.regex.Matcher.find().
   `re-find` just finds one match; this finds all of them."
  [re s]
  (let [m (re-matcher (->pattern re) s)]
    ((fn step []
       (when (. m (find))
         (cons (.group m 0) (lazy-seq (step)))))))))

(defn search-str-within [super sub]
  (let [strict-search (fn1 containsv? sub)
        regexized     (->> sub conv-regex-specials (str "(?i)") re-pattern)
        case-insensitive-regex-search
          (partial re-find regexized)]
    (->> super
         (reducei
          (fn [ret m n]
            (if (->> m vals
                     (filter (fn-> str case-insensitive-regex-search))
                     first)
                (assoc! ret n m)
                ret))
          (transient {}))
        persistent!)))

(def ^{:doc "Special characters in various regular expression implementations."}
  regex-metacharacters
  {:default #{\\ \^ \$ \* \+ \? \. \| \( \) \{ \} \[ \]}
   ; Vimscript "very-magic" mode
   :vim (set (core/remove #(re-find #"\w" (str %)) (map char (range 0x21 0x7f))))})

#?(:clj
(def line-terminator-chars-regex
  (->> line-terminator-chars (map char->regex-code) join bracket)))

(defn re-get
  [regex ^String string]
  "Return the matched portion of a regex from a string"
  {:attribution "thebusby.bagotricks"}
  (let [[_ & xs] (re-find regex string)]
    xs))

(defn conv-regex-specials [^String str-0]
  (-> str-0
      (replace "\\" "\\\\")
      (replace "$" "\\$"  )
      (replace "^" "\\^"  )
      (replace "." "\\."  )
      (replace "|" "\\|"  )
      (replace "*" "\\*"  )
      (replace "+" "\\+"  )
      (replace "(" "\\("  )
      (replace ")" "\\)"  )
      (replace "[" "\\["  )
      (replace "{" "\\{"  )))

(defn re-index
  "Indexed matches."
  {:todo ["Use bounded regex searches for MUCH greater efficiency."]
   :out '{4   "div"
          10  "divb"
          22  "divh"
          25  "div0a"
          212 "divg"}}
  [reg s]
  (loop
    [s-n s
     matches (re-seq reg s)
     indexed (map/sorted-rank-map)]
    (if (empty? matches)
        indexed
        (let [match-n    (first matches)
              i-relative (.indexOf ^String s-n ^String match-n)
              i-absolute
                (if (empty? indexed)
                    i-relative
                    (+ i-relative (-> indexed last first)
                                  (-> indexed last second count)))]
          (recur (.substring ^String s-n
                   (+ i-relative (count match-n))
                   (-> s-n count dec))
                 (rest matches)
                 (assoc indexed i-absolute match-n))))))

(defn re-preview [reg ^String s & [preview-ct]]
  (->> (re-index reg s)
       (map (fn [[i v]]
              (.substring s i (+ i (or preview-ct 20)))))))

#?(:clj
(defn gsub
  "Matches patterns and replaces those matches with a specified value.
  Expects a string to run the operation on, a pattern in the form of a
  regular expression, and a function that handles the replacing."
  {:source "zcaudate/hara"}
  [^String value pattern sub-fn]
  (loop [matcher (re-matcher pattern value) result [] last-end 0]
    (if (.find matcher)
      (recur matcher
        (conj result
          (.substring value last-end (.start matcher))
          (sub-fn (re-groups matcher)))
        (.end matcher))
      (apply str (conj result (.substring value last-end)))))))

#?(:clj
(defnt'
 regionMatches
 "Green implementation of regionMatches.
  @cs:         the |CharSequence| to be processed
  @ignoreCase: whether or not to be case insensitive
  @thisStart:  the index to start on @cs
  @substring:  the |CharSequence| to be looked for
  @start:      the index to start on the @substring
  @length:     character length of the region

  Returns whether the region matched."
  {:source "org.apache.commons.codec.binary.CharSequenceUtils"
   :todo   ["Lots of code to do probably a simple thing"]}
  [^CharSequence cs
   ^boolean ignore-case?
   ^int thisStart
   ^CharSequence substring
   ^int start
   ^int length]
  (if (and (instance? String cs)
           (instance? String substring))
      (let [^String cs1 cs]
        (.regionMatches
          cs1
          ignore-case?
          thisStart
          ^String substring
          (int start)
          (int length)))
      (loop [index1 (core/int thisStart )
             index2 (core/int start     )
             tmpLen (-> length dec core/int)]
        (if (> tmpLen 0)
            (let [index1-n+1 (-> index1 inc core/int)
                  index2-n+1 (-> index2 inc core/int)
                  c1 (.charAt cs        index1-n+1)
                  c2 (.charAt substring index2-n+1)]
              (cond
                (= c1 c2)
                  (recur index1-n+1
                         index2-n+1
                         (-> tmpLen dec core/int))
                (or (not ignore-case?)
                    (and (not= (->upper c1)
                               (->upper c2))
                         (not= (->lower c1)
                               (->lower c2))))
                  false
                :else
                  (recur index1-n+1
                         index2-n+1
                         (-> tmpLen dec core/int))))
            true)))))

; ===== STRING VALUES =====

; (def from-string-chart
;   {:keyword (fn [v] (keyword v))
;    :bigint  (fn [v] (BigInteger. v))
;    :bigdec  (fn [v] (BigDecimal. v))
;    :long    (fn [v] (Long/parseLong v))
;    :float   (fn [v] (Float/parseFloat v))
;    :double  (fn [v] (Double/parseDouble v))
;    :instant (fn [v] (.parse date-format-json v))
;    :uuid    (fn [v] (hara.common/uuid v))
;    :uri     (fn [v] (hara.common/uri v))
;    :enum    (fn [v] (read-enum v))
;    :ref     (fn [v] (read-ref v))})

(defn val [obj]
  ; TODO maybe use (edn/read-string s)
  ; TODO Should we use NaN?
  (if (string? obj)
      #?(:clj
        (try (Integer/parseInt obj)
          (catch Exception e
            (try (Long/parseLong obj)
              (catch Exception e
                (try (Double/parseDouble obj)
                  (catch Exception e obj))))))
        :cljs
        (whenc (js/Number obj) js/isNaN
          obj))
      obj))

; ===== MISCELLANEOUS =====

(defalias reverse str+/reverse)

#?(:clj
(defn remove-accents [^String s]
  {:attribution "github.com/jkk/sundry/string"}
  (-> (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFD)
      (.replaceAll "[^\\p{ASCII}]" ""))))

; ===== EQUALITY ===== ;

; Compare strings in a case-insensitive manner.
; This function is locale independent.
(defalias caseless= str+/caseless=)
; Compare strings in a case-insensitive manner
; respecting the current locale.
; An optional locale can be passed as third
; argument (only on JVM).
(defalias locale-caseless= str+/locale-caseless=)
