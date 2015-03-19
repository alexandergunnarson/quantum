(ns quantum.core.string
  (:refer-clojure :exclude [replace remove contains? val re-find])
  (:require
    [quantum.core.function :as fn :refer
                                                             
             [compr f*n fn* unary]
             :refer-macros
             [fn->> fn-> <-]]
    [quantum.core.logic :as log :refer
                                                                                                             
             [splice-or fn-and fn-or fn-not nnil?]
             :refer-macros
             [ifn if*n whenc whenf whenf*n whencf*n condf condf*n condfc]]
    [quantum.core.ns :as ns :refer
                               
             [Exception IllegalArgumentException
              Nil Bool Num ExactNum Int Decimal Key Vec Set
              ArrList TreeMap LSeq Regex Editable Transient Queue Map]]
    [quantum.core.numeric  :as num]
    [quantum.core.reducers :as r :refer
                                                                                        
             [map+ reduce+ filter+ remove+ take+ take-while+ reducei+ fold+ range+]
             :refer-macros
             [for+]]
    [quantum.core.type     :as type :refer
      [                     class instance+? array-list? boolean? double? map-entry?
       sorted-map? queue? lseq? coll+? pattern? regex? editable? transient?]]
    [quantum.core.macros   :as macros]
    [clojure.string        :as str]
           [cljs.core :refer [Keyword]])
       
          
                        
                    
                    
                                               
                                                                      
                    )

                                      


; http://www.regular-expressions.info
;___________________________________________________________________________________________________________________________________
;====================================================={ STRING MANIPULATION }=======================================================
;====================================================={                     }=======================================================
(def replace     str/replace)
(def capitalize  str/capitalize)
(def split       str/split)
(def join        str/join)
(def upper-case  str/upper-case)
(def lower-case  str/lower-case)
(def triml       str/triml)
(def trimr       str/trimr)
(def re-find     clojure.core/re-find)
(defn blank?
  "Determines if an object @obj is a blank/empty string."
  [obj]
  ((fn-and string? empty?) obj))
(def  str-nil (whencf*n nil? ""))
(defn upper-case? [c]
                                             
         (= c (.toUpperCase c)))
(defn lower-case? [c]
                                             
         (= c (.toLowerCase c)))
(defn char+ [obj]
  (if ((fn-and string? #(fn->> count (>= 1))) obj)
      (first obj)
      (char obj)))
(defn conv-regex-specials [^String str-0]
  (-> str-0
      (str/replace "\\" "\\\\")
      (str/replace "$" "\\$")
      (str/replace "^" "\\^")
      (str/replace "." "\\.")
      (str/replace "|" "\\|")
      (str/replace "*" "\\*")
      (str/replace "+" "\\+")
      (str/replace "(" "\\(")
      (str/replace ")" "\\)")
      (str/replace "[" "\\[")
      (str/replace "{" "\\{")))

(defn subs+ ; :from :to, vs. :from :for
  "Gives a consistent, flexible, cross-platform substring API with support for:
    * Clamping of indexes beyond string limits.
    * Negative indexes: [   0   |   1   |  ...  |  n-1  |   n   ) or
                        [  -n   | -n+1  |  ...  |  -1   |   0   ).
                        (start index inclusive, end index exclusive).

  Note that `max-len` was chosen over `end-idx` since it's less ambiguous and
  easier to reason about - esp. when accepting negative indexes.
  From taoensso.encore."
  {:usage
    ["(subs+ \"Hello\"  0 5)" "Hello"
     "(subs+ \"Hello\"  0 9)" "Hello"
     "(subs+ \"Hello\" -4 5)" "Hello"
     "(subs+ \"Hello\"  2 2)" "ll"   
     "(subs+ \"Hello\" -2 2)" "ll"   
     "(subs+ \"Hello\" -2)  " "llo"  
     "(subs+ \"Hello\"  2)  " "llo"  
     "(subs+ \"Hello\"  9 9)" ""     
     "(subs+ \"Hello\"  0 0)" ""]}
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
                                                    
             (.startsWith         super         sub)
    (keyword? super)
      (starts-with? (name super) sub)))

(defn ends-with?
  {:todo ["Protocolize"]}
  [super sub]
  (cond
    (string? super)
                                                  
             (.endsWith         super         sub)
    (keyword? super)
      (ends-with? (name super) sub)))

(defn remove-all [^String str-0 to-remove]
  (reduce #(replace %1 %2 "") str-0 to-remove))
     
            
                          
                           
                   
           
                                                                   
            
                                  
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
  (-> kw (replace " " "-") lower-case keyword))
(defn camelcase
  ^{:attribution  "flatland.useful.string"
    :contributors "Alex Gunnarson"}
  [str-0 & [method?]]
  (-> str-0
      (replace #"[-_](\w)"
        (compr second upper-case))
      (#(if (not method?)
           (apply str (-> % first upper-case) (rest %))
           %))))
(defn un-camelcase [sym]
  (let [str-0 (str sym)
        matches (->> str-0 (re-seq #"[a-z0-1][A-Z]") distinct)]
    (-> (reduce+ (fn [ret [char1 char2 :as match]]
                   (replace ret match (str char1 "-" (lower-case char2))))
          str-0 matches)
        lower-case)))

(defn squote
  "Wraps a given string in single quotes."
  {:todo ["Protocolize"]}
  [str-0]
  (if (nil? str-0)
      (str "'" "nil" "'")
      (str "'" str-0 "'")))
(defn paren
  "Wraps a given string in parentheses."
  {:todo ["Protocolize"]}
  [str-0]
  (if (nil? str-0)
      (str "(" "nil" ")")
      (str "(" str-0 ")")))
(defn sp
  "Like |str|, but adds spaces between the arguments."
  [& args]
  (reducei+
    (fn [ret elem n]
      (if (= n (dec (count args)))
          (str ret elem)
          (str ret elem " ")))
    ""
    args))
(defn sp-comma
  "Like |sp|, but adds commas and spaces between the arguments."
  [& args]
  (reducei+
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
  ^{:attribution "thebusby.bagotricks"}
  (let [[_ & xs] (re-find regex string)]
    xs))
(defn re-find+ [pat ^String in-str]
  (try (re-find (re-pattern pat) in-str)
    (catch                            
                  js/TypeError _
      nil)))
(defn contains?
  [^String super sub]
  (condfc sub
    string?
                                        
           (not= -1 (.indexOf super sub))
    pattern?
    (nnil? (re-find+ sub super))))
(defn substring? [^String substr ^String string] ; the complement of contains?
  (contains? string substr))

(def alphabet
  (->> (map+ (fn-> char str) (range+ 65 (inc 90)))
       fold+)) ; this is actually reduce into vector...

(defn ^String rand-str [len] 
  (->> (for+ [n (range 0 len)]
         (->> (- (inc 90) 65)
              rand-int
              (+ 65)
              char str))
       (reduce+ str)))

(defn val [obj]
  (if (string? obj)
           
                                 
                          
                                   
                              
                                           
                                            
            
      (whenc (js/Number obj) js/isNaN 
        obj)
      obj))

; THIS SHOULD BELONG IN quantum.core.SEMANTIC (?)

(def vowels
  ["a" "e" "i" "o" "u"
   "A" "E" "I" "O" "U"])

     
             
         
                            

     
                        
                     
              
                                
                                                                 
                                        
        
                           

(defn keyword+
  "Like |str| but for keywords."
  ([obj]
    (cond ; would have done |condf| but likely |cond| is faster...?
      (keyword? obj) obj
      (string?  obj) (keyword obj)))
  ([obj & objs]
    (->> (cons obj objs)
         (reduce+
           (fn [ret elem] ; elem might be string or keyword
             (str ret (name elem)))
           "")
         keyword)))


;;;;;;;;;;;; This file autogenerated from src/cljx/quantum/core/string.cljx
