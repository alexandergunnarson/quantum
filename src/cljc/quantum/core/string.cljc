(ns
  ^{:doc "Useful string utils. Aliases clojure.string.

          Includes squote (single-quote), sp (spaced),
          val (reading a number of a string), keyword+
          (for joining strings and keywords into one
          keyword), etc."
    :attribution "Alex Gunnarson"}
  quantum.core.string
  (:refer-clojure :exclude [replace remove val re-find])
  (:require-quantum [ns fn set macros logic red num type loops])
  (:require         [clojure.string :as str]))

; http://www.regular-expressions.info
;___________________________________________________________________________________________________________________________________
;====================================================={ STRING MANIPULATION }=======================================================
;====================================================={                     }=======================================================
(def num-chars      #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9})

(def upper-chars    #{\A \B \C \D \E \F \G \H \I \J \K \L \M \N \O \P \Q \R \S \T \U \V \W \X \Y \Z})
(def lower-chars    #{\a \b \c \d \e \f \g \h \i \j \k \l \m \n \o \p \q \r \s \t \u \v \w \x \y \z})
(def alpha-chars    (set/union upper-chars lower-chars))

(def alphanum-chars (set/union alpha-chars num-chars))

(defnt numeric?
  #?@(:clj
 [char?   ([c] (contains? num-chars c))])
  string? ([s] (every? (extern (partial contains? num-chars)) s)))

(defnt upper?
  #?@(:clj
 [char?   ([c] (contains? upper-chars c))])
  string? ([s] (every? (extern (partial contains? upper-chars)) s)))

(defnt lower?
  #?@(:clj
 [char?   ([c] (contains? lower-chars c))])
  string? ([s] (every? (extern (partial contains? lower-chars)) s)))

(defnt alphanum?
  #?@(:clj
 [char?   ([c] (contains? alphanum-chars c))])
  string? ([s] (every? (extern (partial contains? alphanum-chars)) s)))

(defnt replace*
  string? ([pre post s] (.replace    ^String s pre ^String post))
  regex?  ([pre post s] (str/replace         s pre         post)))

; CompilerException java.lang.RuntimeException: Method code too large!
; if you do it "definline"...
(defn replace [s pre post] (replace* pre post s))

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

(def capitalize  str/capitalize)
(def split       str/split)
(def join        str/join)
(def upper-case  str/upper-case)
(def lower-case  str/lower-case)
(def trim        str/trim)
(def triml       str/triml)
(def trimr       str/trimr)
(def re-find     clojure.core/re-find)

;"Determines if an object @obj is a blank/empty string."
(def blank? (fn-and string? empty?))

(def  str-nil (whencf*n nil? ""))

(defn char+ [obj]
  (if ((fn-and string? (fn->> count (>= 1))) obj)
      (first obj)
      (char obj)))

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

(defn subs+ ; :from :to, vs. :from :for
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
  [s start-idx & [max-len]]
  {:pre [(or (nil? max-len) (num/nneg-int? max-len))]}
  (let [;; s       (str   s)
        slen       (count s)
        start-idx* (if (>= start-idx 0)
                     (min start-idx slen)
                     (max 0 (dec (+ slen start-idx))))
        end-idx*   (if-not max-len slen
                     (min (+ start-idx* max-len) slen))]
    ;; (println [start-idx* end-idx*])
    (.substring ^String s start-idx* end-idx*)))

(defn starts-with?
  {:todo ["Protocolize"]}
  [super sub]
  (cond
    (string? super)
      #?(:clj  (.startsWith ^String super ^String sub)         
         :cljs (zero? (.indexOf super sub))) ; .startsWith is not implemented everywhere
    (keyword? super)
      (starts-with? (name super) sub)))

(defn ends-with?
  {:todo ["Protocolize"]}
  [super sub]
  (cond
    (string? super)
      #?(:clj  (.endsWith ^String super ^String sub)
         :cljs (.endsWith         super         sub)) ; .endsWith is not implemented everywhere
    (keyword? super)
      (ends-with? (name super) sub)))

(defn remove-all [str-0 to-remove]
  (reduce #(replace %1 %2 "") str-0 to-remove))

#?(:clj
  (defn remove
    {:todo ["Port to cljs"]}
    [^String str-0 to-remove]
    (condfc to-remove
      string?
      (.replaceAll str-0 ^Pattern (conv-regex-specials to-remove) "")
      pattern?
      (replace str-0 to-remove ""))))

(defn join-once
  "Like /clojure.string/join/ but ensures no double separators."
  {:attribution "taoensso.encore"}
  [separator & coll]
  (reduce-
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

(defn remove-from-end [^String string ^String end]
  (if (.endsWith string end)
      (.substring string 0 (- (count string)
                         (count end)))
      string))

(defn remove-extra-whitespace [string-0]
  (loop [string-n string-0]
    (if (= string-n (str/replace string-n "  " " "))
        string-n
        (recur (str/replace string-n "  " " ")))))

(defn capitalize-each-word [string]
  (str/join " "
    (map str/capitalize
         (str/split string #" "))))

(def properize-keyword
  (fn-> (ifn nil? str-nil name) (replace #"\-" " ") capitalize-each-word))

(defn keywordize [^String kw]
  (-> kw
      (replace " " "-")
      (replace "_" "-")
      lower-case keyword))

(defalias camelcase macros/camelcase)

(defn un-camelcase
  {:todo ["Find more efficient algorithm"]}
  [sym]
  (let [str-0 (str sym)
        matches (->> str-0 (re-seq #"[a-z0-1][A-Z]") distinct)]
    (-> (reduce
          (fn [ret [char1 char2 :as match]]
            (replace ret match (str char1 "-" (lower-case char2))))
          str-0 matches)
        lower-case)))

(def quote-types #{"'" "\""})

(defn squote
  "Wraps a given string in single quotes."
  {:todo ["Protocolize"]}
  [str-0]
  (if (nil? str-0)
      (str "'" "nil" "'")
      (str "'" str-0 "'")))

(defn dquote
  "Wraps a given string in double quotes."
  {:todo ["Protocolize"]}
  [str-0]
  (if (nil? str-0)
      (str "\"" "nil" "\"")
      (str "\"" str-0 "\"")))

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

; REGEX

(defn re-get
  [regex ^String string]
  "Return the matched portion of a regex from a string"
  {:attribution "thebusby.bagotricks"}
  (let [[_ & xs] (re-find regex string)]
    xs))

(defn re-find+ [pat ^String in-str]
  (try (re-find (re-pattern pat) in-str)
    (catch #?(:clj  NullPointerException
              :cljs js/TypeError) _
      nil)))

(defn ^String rand-str [len] 
  (->> (for+ [n (range 0 len)]
         (->> (- (inc 90) 65)
              rand-int
              (+ 65)
              char str))
       (reduce str)))

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

; THIS SHOULD BELONG IN quantum.core.SEMANTIC (?)

(def vowels
  ["a" "e" "i" "o" "u"
   "A" "E" "I" "O" "U"])

#?(:clj
(defprotocol
  String+
  (str+ [obj] [obj1 obj2])))

#?(:clj
(extend-protocol String+
  java.io.InputStream
    (str+ [is]
      (let [^java.util.Scanner s
              (-> is (java.util.Scanner.) (.useDelimiter "\\A"))]
        (if (.hasNext s) (.next s) "")))
  Keyword
    (str+ ([k] (str+ k "/"))
          ([k joiner]
            (->> [(namespace k) (name k)]
                 (core/remove empty?)
                 (join joiner))))
  Object
    (str+ [obj] (str obj))))
   
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

(defn char->hex-code [c]
  (let [^String hex (Integer/toHexString (bit-or (int c) 0x10000))]
    (.substring hex 0 (-> hex count dec))))

(defn char->unicode    [c] (str "\\u" (char->hex-code c)))
(defn char->regex-code [c] (str "\\x" (char->hex-code c)))

(def line-terminator-chars
  #{\newline \return (char 0x2028) (char 0x2029)})

(def line-terminator-chars-regex
  (->> line-terminator-chars (map char->regex-code) join bracket))

(defnt line-terminator?
  #?@(:clj
 [char?   ([c] (contains? line-terminator-chars c))])
  string? ([s] (every? (extern (partial contains? line-terminator-chars)) s)))

(def whitespace-chars
  (set/union
    line-terminator-chars
    #{\space \tab (char 12)
      (char 11) (char 0xa0)}))

(defnt whitespace?
  #?@(:clj
 [char?   ([c] (contains? whitespace-chars c))])
  string? ([s] (every? (extern (partial contains? whitespace-chars)) s)))

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
     indexed (sorted-map)]
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
(defn ->str [^java.io.ByteArrayInputStream in-stream]
  (let [n (.available in-stream)
        ^bytes arr (byte-array n)
        _ (println "ARR IS" arr)]
    (.read in-stream arr, 0 n)
    (String. arr java.nio.charset.StandardCharsets/UTF_8))))

; http://stackoverflow.com/questions/327513/fuzzy-string-search-in-java