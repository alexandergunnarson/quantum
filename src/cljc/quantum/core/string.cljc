(ns
  ^{:doc "Useful string utils. Aliases clojure.string.

          Includes squote (single-quote), sp (spaced),
          val (reading a number of a string), keyword+
          (for joining strings and keywords into one
          keyword), etc."
    :attribution "Alex Gunnarson"}
  quantum.core.string
  (:refer-clojure :exclude [reverse replace remove val re-find reduce])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )     :as core              ]
                     [clojure.string             :as str               ]
                     [frak                                             ]
                     [quantum.core.data.map      :as map               ]
                     [quantum.core.data.set      :as set               ]
                     [quantum.core.fn            :as fn 
                       :refer [#?@(:clj [fn->])]                       ]
                     [quantum.core.logic         :as logic
                       :refer [#?@(:clj [fn-and whencf*n ifn]) nempty?]]
                     [quantum.core.loops         :as loops
                       :refer [reduce reducei]                         ]
                     [quantum.core.macros        :as macros
                       :refer [#?@(:clj [defnt defnt'])]               ]
                     [quantum.core.string.format :as form              ]
                     [quantum.core.string.regex  :as regex             ]
                     [quantum.core.vars          :as var
                       :refer [#?@(:clj [defalias])]                   ])
  #?(:cljs (:require-macros
                     [quantum.core.fn            :as fn
                       :refer [fn->]                                   ]
                     [quantum.core.logic         :as logic
                       :refer [fn-and whencf*n ifn]                    ]
                     [quantum.core.loops         :as loops
                       :refer [reduce reducei]                         ]
                     [quantum.core.macros        :as macros
                       :refer [defnt defnt']                           ]
                     [quantum.core.vars          :as var
                       :refer [defalias]                               ]))
  #?(:clj (:import java.net.IDN)))

#_(defn contains? [s sub]
  (not= (.indexOf ^String s sub) -1))

; http://www.regular-expressions.info
; What about structural sharing with strings? Wouldn't there have to be some sort
; of compact immutable bit map or something to diff it rather than just making
; an entirely new string?

; ===== CHARS =====

(def num-chars      #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9})

(def upper-chars    #{\A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T \U \V \W \X \Y \Z})
(def lower-chars    #{\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z})
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
  ([^char?   c] (contains? num-chars c)))
  ([^string? s] (and (nempty? s) (every? (extern (partial contains? num-chars     )) s))))

(defnt upper?
#?(:clj
  ([^char?   c] (contains? upper-chars c)))
  ([^string? s] (and (nempty? s) (every? (extern (partial contains? upper-chars   )) s))))

(defalias ->upper form/->upper)

(defnt lower?
#?(:clj
  ([^char?   c] (contains? lower-chars c)))
  ([^string? s] (and (nempty? s) (every? (extern (partial contains? lower-chars   )) s))))

(defalias ->lower form/->lower)

(defnt alphanum?
#?(:clj
  ([^char?   c] (contains? alphanum-chars c)))
  ([^string? s] (and (nempty? s) (every? (extern (partial contains? alphanum-chars)) s))))

(defnt blank?
#?(:clj
  ([^char?   c] (contains? whitespace-chars c)))
  ([^string? s] (every? (extern (partial contains? whitespace-chars)) s)))

(def whitespace? (fn-and nempty? blank?))

(defnt line-terminator?
#?(:clj
  ([^char?   c] (contains? line-terminator-chars c)))
  ([^string? s] (and (nempty? s) (every? (extern (partial contains? line-terminator-chars)) s))))

(defalias capitalize form/capitalize)

; ===== PARTIAL PREDICATES =====

(defnt starts-with?
  {:todo ["Make more portable by looking at java.lang.String/startsWith"]}
  ([^string? super sub]
    #?(:clj  (.startsWith super ^String sub)         
       :cljs (zero? (.indexOf super sub)))) ; .startsWith is not implemented everywhere)
  ([^keyword? super sub]
    (starts-with? (name super) sub)))

(defnt ends-with?
  {:todo ["Make more portable by looking at java.lang.String/endsWith"]}
  ([^string? super sub]
    #?(:clj  (.endsWith super ^String sub)         
       :cljs (.endsWith super         sub))) ; .endsWith is not implemented everywhere)
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
  ([^vector? x] (vec->pattern x)))

#?(:clj
(defnt contains-pattern?
  ([^string? x pattern] (-> (->pattern ^Pattern pattern) (.matcher x) .find))))

; ===== SPLIT/JOIN =====

(def split       str/split)

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

; a form of |concat| - "concat-with"
; CANDIDATE 0
(def join        str/join)

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

(defn ->path [& xs]
  (apply join-once "/" xs)) ; TODO fix

; ===== REPLACEMENT =====

(defalias replace form/replace)

(defn replace-with
  "Replace all."
  {:in '["and" (om "a" "abc")]
   :out "abcnd"
   :todo "FIX reduce-kv to just normal reduce..."}
  [s m]
  (core/reduce-kv
    (fn [ret old-n new-n]
      (replace ret old-n new-n))
    s
    m))

(def replace-uchars
  (fn-> (str/replace "\\u0026" "&")))

; ===== TRIMMING =====

; CANDIDATE 0
(def trim        str/trim)
; CANDIDATE 1
#_(defn trim
  "Removes whitespace or specified characters
  from both ends of string."
  {:attribution "funcool/cuerdas"}
  ([s] (trim s " "))
  ([s chs]
   (when-not (nil? s)
     (let [rxstr (str "[" #?(:clj chs :cljs (regex/escape chs)) "]")
           rxstr (str "^" rxstr "+|" rxstr "+$")]
       (as-> (re-pattern rxstr) rx
             (replace s rx ""))))))

; CANDIDATE 0
(def triml       str/triml)
; CANDIDATE 1
#_(defn ltrim
  "Removes whitespace or specified characters
  from left side of string."
  {:attribution "funcool/cuerdas"}
  ([s] (ltrim s " "))
  ([s chs]
   (when-not (nil? s)
     (let [rxstr (str "[" #?(:clj chs :cljs (regex/escape chs)) "]")
           rxstr (str "^" rxstr "+")]
       (as-> (re-pattern rxstr) rx
             (replace s rx ""))))))

; CANDIDATE 0
(def trimr       str/trimr)
; CANDIDATE 1
#_(defn rtrim
  "Removes whitespace or specified characters
  from right side of string."
  ([s] (rtrim s " "))
  ([s chs]
   (when-not (nil? s)
     (let [rxstr (str "[" #?(:clj chs :cljs (regex/escape chs)) "]")
           rxstr (str rxstr "+$")]
       (as-> (re-pattern rxstr) rx
             (replace s rx ""))))))

(defalias strip  trim)
(defalias stripr trimr)
(defalias stripl triml)

; CANDIDATE 0
(defn collapse-whitespace
  "Converts all adjacent whitespace characters
  to a single space."
  {:attribution "funcool/cuerdas"}
  [s]
  (some-> s
          (replace #"[\s\xa0]+" " ")
          (replace #"^\s+|\s+$" "")))

; CANDIDATE 1
#_(defn collapse-whitespace [string-0]
  (loop [string-n string-0]
    (if (= string-n (str/replace string-n "  " " "))
        string-n
        (recur (str/replace string-n "  " " ")))))

(defn clean
  "Trim and collapse whitespace."
  {:attribution "funcool/cuerdas"}
  [s]
  (-> s trim
      (replace #"\s+" " "))) ; should it be |collapse-whitespace| instead?

(def  str-nil (whencf*n nil? ""))

; ===== COERCION =====

(defn keyword+
  "Like |str| but for keywords."
  ([obj]
    (cond ; would have done |condf| but likely |cond| is faster...?
      (keyword? obj) obj
      (string?  obj) (keyword obj)))
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
  {:todo ["Port to cljs"]}
  ([^string? str-0 to-remove]
    (remove* to-remove str-0)))

(defn remove-from-end [^String string ^String end]
  (if (.endsWith string end)
      (.substring string 0 (- (count string)
                         (count end)))
      string))

; ===== KEYWORDIZATION =====

(def properize-keyword
  (fn-> (ifn nil? str-nil name) (replace #"\-" " ") form/capitalize-each-word))

(defn keywordize [^String kw]
  (-> kw
      (replace " " "-")
      (replace "_" "-")
      form/->lower keyword))

(defnt unkeywordize
  ([^keyword? k]
    (-> k name (replace "-" " ") form/capitalize-each-word)))

; ===== PUNCTUATION =====

(def quote-types #{"'" "\""})

(defn squote
  "Wraps a given string in single quotes."
  {:todo ["Protocolize"]}
  [str-0]
  (if (nil? str-0)
      (str \' "nil" \')
      (str \' str-0 \')))

(defn dquote
  "Wraps a given string in double quotes."
  {:todo ["Protocolize"]}
  [str-0]
  (if (nil? str-0)
      (str \" "nil" \")
      (str \" str-0 \")))

; (defn dequote
;   {:in  "'Abcdef'"
;    :out "Abcdef"}
;   [s]
;   (if (and  (= (first s) (last s))
;         (any? (partial starts-with? s) quote-types)
;         (any? (partial ends-with?   s) quote-types))
;       (-> s popl popr) ; TODO this will be optimized away 
;       s))

(defn paren
  "Wraps a given string in parentheses."
  {:todo ["Protocolize"]}
  [str-0]
  (if (nil? str-0)
      (str \( "nil" \))
      (str \( str-0 \))))

(defn bracket [s] (str \[ s \]))

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

  TODO: Document how this differs from |re-find|."
  [re s]
  (let [m (re-matcher (->pattern re) s)]
    ((fn step []
       (when (. m (find))
         (cons (.group m 0) (lazy-seq (step)))))))))

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
      (replace "$" "\\$")
      (replace "^" "\\^")
      (replace "." "\\.")
      (replace "|" "\\|")
      (replace "*" "\\*")
      (replace "+" "\\+")
      (replace "(" "\\(")
      (replace ")" "\\)")
      (replace "[" "\\[")
      (replace "{" "\\{")))

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
    [#?@(:clj  [^String s-n s]
         :cljs [        s-n s])
     matches (re-seq reg s)
     indexed (map/sorted-map)]
    (if (empty? matches)
        indexed
        (let [match-n    (first matches)
              i-relative (.indexOf s-n match-n)
              i-absolute
                (if (empty? indexed)
                    i-relative
                    (+ i-relative (-> indexed last first)
                                  (-> indexed last second count)))]
          (recur (.substring s-n (+ i-relative (count match-n))
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
  [value pattern sub-fn]
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

; ===== LANGUAGE STYLE REPLACEMENTS =====

; TODO MOVE NAMESPACE
(defn java->clojure
  {:source "zcaudate/hara.object.util"}
  [^String name]
  (let [nname (cond (re-find #"(^get)|(^set)[A-Z].+" name)
                    (subs name 3)

                    (re-find #"^is[A-Z].+" name)
                    (str (subs name 2) "?")

                    (re-find #"^has[A-Z].+" name)
                    (str (subs name 3) "!")

                    :else name)]
    (form/->spear-case nname)))

; TODO MOVE NAMESPACE
(defn clojure->java
  {:source "zcaudate/hara.object.util"}
  ([name] (clojure->java name :get))
  ([^String name suffix]
   (let [nname (cond (.endsWith name "?")
                     (str "is-" (.substring name 0 (.length name)))

                     (.endsWith name "!")
                     (str "has-" (.substring name 0 (.length name)))

                     :else
                     (str (clojure.core/name suffix) "-" name))]
     (form/->camel-case nname))))

; ===== MISCELLANEOUS =====

(defn reverse
  "Return string reversed."
  {:attribution "funcool/cuerdas"}
  [s]
  (when-not (nil? s)
    #?(:clj (let [sb (StringBuilder. ^String s)]
              (str (.reverse sb)))
       :cljs (-> s (.split "") (.reverse) (.join "")))))

; ___________________________________________
; ================ TO IMPORT ================
; -------------------------------------------

; (ns cuerdas.core
;   (:refer-clojure :exclude [contains? empty? repeat replace reverse chars
;     #?@(:clj [unquote format])])
;   (:require #?(:cljs [goog.string :as gstr])
;             [clojure.set  :refer [map-invert]]
;             [clojure.walk :refer [stringify-keys]]))

; #?(:clj (declare slice))

; #?(:cljs
; (defn- regexp
;   "Build or derive regexp instance."
;   {:attribution "funcool/cuerdas"}
;   ([s]
;    (if (regexp? s)
;      s
;      (js/RegExp. s)))
;   ([s flags]
;    (if (regexp? s)
;      (js/RegExp. (.-source s) flags)
;      (js/RegExp. s flags)))))

; (declare escape-regexp)
; (declare replace)

; (defn strip-prefix
;   "Strip prefix in more efficient way."
;   {:attribution "funcool/cuerdas"}
;   [^String s ^String prefix]
;   (if (starts-with? s prefix)
;     (slice s (count prefix) (count s))
;     s))

; (defn strip-suffix
;   "Strip suffix in more efficient way."
;   {:attribution "funcool/cuerdas"}
;   [^String s ^String prefix]
;   (if (ends-with? s prefix)
;     (slice s 0 (- (count s) (count prefix)))
;     s))

; (declare join)

; (defn repeat
;   "Repeats string n times."
;   {:attribution "funcool/cuerdas"}
;   ([s] (repeat s 1))
;   ([s n]
;    (when-not (nil? s)
;      #?(:clj  (join (clojure.core/repeat n s))
;         :cljs (gstr/repeat s n)))))

; (defn slice
;   "Extracts a section of a string and returns a new string."
;   {:attribution "funcool/cuerdas"}
;   ([s begin]
;     #?(:clj  (slice s begin (count s))
;        :cljs (when-not (nil? s)
;                (.slice s begin))))
;   ([s #?@(:clj [^long begin ^long end] :cljs [begin end])]
;    #?(:clj (if (nil? s)
;                s
;                (let [end   (if (< end 0) (+ (count s) end) end)
;                      begin (if (< begin 0) (+ (count s) begin) begin)
;                      end   (if (> end (count s)) (count s) end)]
;                  (if (> begin end)
;                    ""
;                    (let [begin (if (< begin 0) 0 begin)
;                          end (if (< end 0) 0 end)]
;                      (.substring ^String s begin end)))))
;       :cljs (when-not (nil? s)
;               (.slice s begin end)))))

; (defn replace
;   "Replaces all instance of match with replacement in s.
;   The replacement is literal (i.e. none of its characters are treated
;   specially) for all cases above except pattern / string.
;   In match is pattern instance, replacement can contain $1, $2, etc.
;   will be substituted with string that matcher the corresponding
;   parenthesized group in pattern.
;   If you wish your replacement string to be used literary,
;   use `(escape-regexp replacement)`.
;   Example:
;     (replace \"Almost Pig Latin\" #\"\\b(\\w)(\\w+)\\b\" \"$2$1ay\")
;     ;; => \"lmostAay igPay atinLay\"
;   "

;   [s match replacement]
;   (when-not (nil? s)
;     #?(:clj  (str/replace s match replacement)
;        :cljs (.replace s (regexp match "g") replacement))))

; #?(:cljs
; (defn ireplace
;   "Replaces all instances of match with replacement in s."
;   {:attribution "funcool/cuerdas"}
;   [s match replacement]
;   (when-not (nil? s)
;     (.replace s (regexp match "ig") replacement))))

; (defn replace-first
;   "Replaces first instance of match with replacement in s."
;   {:attribution "funcool/cuerdas"}
;   [^String s match replacement]
;   (when-not (nil? s)
;     #?(:clj  (str/replace-first s match replacement)
;        :cljs (.replace s (regexp match) replacement))))

; #?(:cljs
; (defn ireplace-first
;   "Replaces first instance of match with replacement in s."
;   {:attribution "funcool/cuerdas"}
;   [s match replacement]
;   (when-not (nil? s)
;     (.replace s (regexp match "i") replacement))))


; (defn prune
;   "Truncates a string to a certain length and adds '...'
;   if necessary."
;   {:attribution "funcool/cuerdas"}
;   ([s num] (prune s num "..."))
;   ([s num subs]
;    (if (< (count s) num)
;      s
;      (let [tmpl (fn [c] (if (not= (upper c) (lower c)) "A" " "))
;            template (-> (slice s 0 (inc (count s)))
;                         (replace #".(?=\W*\w*$)" tmpl))
;            tmp (slice template (- (count template) 2))
;            template (if #?(:clj  (.matches ^String tmp "\\w\\w")
;                            :cljs (.match (slice template (- (count template) 2)) #"\w\w"))
;                       (replace-first template #"\s*\S+$" "")
;                       (rtrim (slice template 0 (dec (count template)))))]
;        (if (> (count (str template subs)) (count s))
;          s
;          (str (slice s 0 (count template)) subs))))))

; (defn strip-newlines
;   "Takes a string and replaces newlines with a space.
;   Multiple lines are replaced with a single space."
;   {:attribution "funcool/cuerdas"}
;   [^String s]
;   (replace s #?(:clj #"[\n\r|\n]+" :cljs #"(\r\n|\r|\n)+") " "))

; (defn split
;   "Splits a string on a separator a limited
;   number of times. The separator can be a string
;   or Pattern (clj) / RegExp (cljs) instance."
;   {:attribution "funcool/cuerdas"}
;   ([s] (split s #"\s" #?(:cljs nil)))
;   ([s sep]
;    #?(:clj  (cond
;               (nil? s) s
;               (instance? Pattern sep) (str/split s sep)
;               :else (str/split s (re-pattern sep)))
;       :cljs (split s sep nil)))
;   ([s sep num]
;    (cond
;      (nil? s) s
;      #?(:clj  (instance? Pattern sep)
;         :cljs (regexp?           sep)) (str/split s sep num)
;      :else (str/split s (re-pattern sep) num))))




; (defn lines
;   "Return a list of the lines in the string."
;   {:attribution "funcool/cuerdas"}
;   [s]
;   (split s #"\n|\r\n"))

; (defn unlines
;   "Returns a new string joining a list of strings with a newline char (\\n)."
;   {:attribution "funcool/cuerdas"}
;   [s]
;   (if (nil? s)
;     s
;     (str/join "\n" s)))

; (defn format
;   "Simple string interpolation."
;   {:attribution "funcool/cuerdas"}
;   [s & args]
;   (if (and (= (count args) 1) (map? (first args)))
;     (let [params (#?(:clj stringify-keys :cljs clj->js) (first args))]
;       (replace s #"%\(\w+\)s"
;                (fn [match]
;                  (str (#?(:clj get :cljs aget) params (slice match 2 -2))))))
;     (let [params #?(:clj (java.util.ArrayList. ^List args) :cljs (clj->js args))]
;       (replace s #?(:clj #"%s" :cljs (regexp "%s" "g"))
;         (fn [_] (str #?(:clj  (.remove params 0)
;                         :cljs (.shift  params))))))))



; (defn surround
;   "Surround a string with another string."
;   {:attribution "funcool/cuerdas"}
;   [s wrap]
;   (when-not (nil? s)
;     (join #?(:cljs "") [wrap s wrap])))

; (defn unsurround
;   "Unsurround a string surrounded by another."
;   {:attribution "funcool/cuerdas"}
;   [s surrounding]
;   (let [length (count surrounding)
;         fstr (slice s 0 length)
;         slength (count s)
;         rightend (- slength length)
;         lstr (slice s rightend slength)]
;     (if (and (= fstr surrounding) (= lstr surrounding))
;       (slice s length rightend)
;       s)))

; (defn quote
;   "Quotes a string."
;   {:attribution "funcool/cuerdas"}
;   ([s] (surround s "\""))
;   ([s qchar] (surround s qchar)))

; (defn unquote
;   "Unquote a string."
;   {:attribution "funcool/cuerdas"}
;   ([s] (unsurround s "\""))
;   ([s qchar]
;    (unsurround s qchar)))

; (defn slugify
;   "Transform text into a URL slug."
;   {:attribution "funcool/cuerdas"}
;   [s]
;   (when s
;     (let [from  "ąàáäâãåæăćčĉęèéëêĝĥìíïîĵłľńňòóöőôõðøśșšŝťțŭùúüűûñÿýçżźž"
;           to    "aaaaaaaaaccceeeeeghiiiijllnnoooooooossssttuuuuuunyyczzz"
;           regex (re-pattern (str "[" (escape-regexp from) "]"))]
;       (-> (lower s)
;           (replace regex (fn [^String c]
;                            (let [index (.indexOf from c)
;                                  res   #?(:clj  (String/valueOf (.charAt to index))
;                                           :cljs (.charAt to index))]
;                              (if (empty? res) "-" res))))
;           (replace #"[^\w\s-]" "")
;           (spear-case)))))

; ;; (defn pad
; ;;   "Pads the str with characters until the total string
; ;;   length is equal to the passed length parameter. By
; ;;   default, pads on the left with the space char."
; ;;   [s & [{:keys [length padding type]
; ;;          :or {length 0 padding " " type :left}}]]
; ;;   (let [padding (aget padding 0)
; ;;         padlen  (- length (count s))]
; ;;     (condp = type
; ;;       :right (str s (repeat padding padlen))
; ;;       :both  (let [first (repeat padding (js/Math.ceil (/ padlen 2)))
; ;;                    second (repeat padding (js/Math.floor (/ padlen 2)))]
; ;;                (str first s second))
; ;;       :left  (str (repeat padding padlen) s))))

; #?(:cljs
; (defn pad
;   "Pads the str with characters until the total string
;   length is equal to the passed length parameter. By
;   default, pads on the left with the space char."
;   {:attribution "funcool/cuerdas"}
;   [s & [{:keys [length padding type]
;          :or {length 0 padding " " type :left}}]]
;   (when-not (nil? s)
;     (let [padding (aget padding 0)
;           padlen  (- length (count s))]
;       (condp = type
;         :right (str s (repeat padding padlen))
;         :both  (let [first (repeat padding (js/Math.ceil (/ padlen 2)))
;                      second (repeat padding (js/Math.floor (/ padlen 2)))]
;                  (str first s second))
;         :left  (str (repeat padding padlen) s))))))

; (defn camelize
;   "Converts a string from selector-case to camelCase."
;   {:attribution "funcool/cuerdas"}
;   [s]
;   (some-> s
;           (trim)
;           (replace #?(:clj  #"[-_\s]+(.)?"
;                       :cljs (regexp #"[-_\s]+(.)?" "g"))
;             (fn [[match c]] (if c (upper c) "")))))

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

; #?(:cljs
; (defn- parse-number-impl
;   [source]
;   (or (* source 1) 0)))

; #?(:cljs
; (defn parse-number
;   "General purpose function for parse number like
;   string to number. It works with both integers
;   and floats."
;   {:attribution "funcool/cuerdas"}
;   ([s] (parse-number s 0))
;   ([s precision]
;    (if (nil? s)
;      0
;      (let [s  (trim s)
;            rx #"^-?\d+(?:\.\d+)?$"]
;        (if (.match s rx)
;          (parse-number-impl (.toFixed (parse-number-impl s) precision))
;          NaN))))))

; #?(:cljs
; (defn parse-float
;   "Return the float value, wraps parseFloat."
;   {:attribution "funcool/cuerdas"}
;   ([s] (js/parseFloat s))
;   ([s precision]
;    (if (nil? precision)
;      (js/parseFloat s)
;      (-> (js/parseFloat s)
;          (.toFixed precision)
;          (js/parseFloat))))))

; (defn parse-double
;   "Return the double value from string."
;   {:attribution "funcool/cuerdas"}
;   [^String s]
;   (cond
;     (nil? s) Double/NaN
;     :else (Double/parseDouble s)))

; #?(:cljs
; (defn parse-int
;   "Return the number value in integer form."
;   {:attribution "funcool/cuerdas"}
;   [s]
;   (let [rx (regexp "^\\s*-?0x" "i")]
;     (if (.test rx s)
;       (js/parseInt s 16)
;       (js/parseInt s 10)))))

; (defn parse-long
;   "Return the long value from string."
;   {:attribution "funcool/cuerdas"}
;   [^String s]
;   (cond
;     (nil? s) Double/NaN
;     :else (let [r (Double. (Double/parseDouble s))]
;             (.longValue ^java.lang.Double r))))

; (defn pad
;   "Pads the str with characters until the total string
;   length is equal to the passed length parameter. By
;   default, pads on the left with the space char."
;   {:attribution "funcool/cuerdas"}
;   [s & [{:keys [length padding type]
;          :or {length 0 padding " " type :left}}]]
;   (when-not (nil? s)
;     (let [padding (slice padding 0 1)
;           padlen  (- length (count s))]
;       (condp = type
;         :right (str s (repeat padding padlen))
;         :both  (let [first (repeat padding (Math/ceil (/ padlen 2)))
;                      second (repeat padding (Math/floor (/ padlen 2)))]
;                  (str first s second))
;         :left  (str (repeat padding padlen) s)))))



; #?(:cljs
; (def html-escape-chars
;   {"lt" "<"
;    "gt" ">"
;    "quot" "\""
;    "amp" "&"
;    "apos" "'"}))

; #?(:cljs
; (def reversed-html-escape-chars
;   (map-invert html-escape-chars)))

; ;; reversedEscapeChars["'"] = '#39';

; #?(:cljs
; (defn escape-html
; {:attribution "funcool/cuerdas"}
;   [s]
;   "Converts HTML special characters to their entity equivalents."
;   (let [escapechars (assoc reversed-html-escape-chars "'" "#39")
;         rx (re-pattern "[&<>\"']")]
;     (replace s rx (fn [x]
;                     (str "&" (get escapechars x) ";"))))))

; ;; Complete logic for unescape-html
; ;;   if (entityCode in escapeChars) {
; ;;     return escapeChars[entityCode];
; ;;   } else if (match = entityCode.match(/^#x([\da-fA-F]+)$/)) {
; ;;     return String.fromCharCode(parseInt(match[1], 16));
; ;;   } else if (match = entityCode.match(/^#(\d+)$/)) {
; ;;     return String.fromCharCode(~~match[1]);
; ;;   } else {
; ;;     return entity;
; ;;   }

; ;; TODO: basic implementation

; #?(:cljs
; (defn unescape-html
;   "Converts entity characters to HTML equivalents."
;   {:attribution "funcool/cuerdas"}
;   [s]
;   (replace s #"\&(\w+);" (fn [x y]
;                            (cond
;                              (cljs.core/contains? html-escape-chars y)
;                              (get html-escape-chars y)
;                              :else y)))))

; (defn- strip-tags-impl
; {:attribution "funcool/cuerdas"}
;   [s tags mappings]
;   (let [kwdize (comp keyword lower name)
;         tags (cond
;                (nil? tags) tags
;                (string? tags) (hash-set (kwdize tags))
;                (sequential? tags) (set (map kwdize tags)))
;         rx   (re-pattern "<\\/?([^<>]*)>")
;         replacer (if (nil? tags)
;                    (fn #?(:clj [[match tag]] :cljs [match tag])
;                      (let [tag (kwdize tag)]
;                        (get mappings tag "")))
;                    (fn #?(:clj [[match tag]] :cljs [match tag])
;                      (let [tag (kwdize tag)]
;                        (if (tags tag)
;                          (get mappings tag "")
;                          match))))]
;     (replace s rx replacer)))

; (defn strip-tags
;   "Remove html tags from string."
;   {:attribution "funcool/cuerdas"}
;   ([s] (strip-tags-impl s nil {}))
;   ([s tags]
;    (if (map? tags)
;        (strip-tags-impl s nil  tags)
;        (strip-tags-impl s tags {}  )))
;   ([s tags mapping]
;    (strip-tags-impl s tags mapping)))

; (defn substr-between
;   "Find string that is nested in between two strings. Return first match"
;   {:attribution "funcool/cuerdas"}
;   [s prefix suffix]
;   (cond
;     (nil? s) nil
;     (nil? prefix) nil
;     (nil? suffix) nil
;     (not (contains? s prefix)) nil
;     (not (contains? s suffix)) nil
;     :else
;     (some-> s
;             (split prefix)
;             second
;             (split suffix)
;             first)))

#?(:clj
(defn remove-accents [^String s]
  {:attribution "github.com/jkk/sundry/string"}
  (-> (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFD)
      (.replaceAll "[^\\p{ASCII}]" ""))))