(ns
  ^{:doc "(Once-)useful XML functions.

          This namespace is very old and poorly written, and will likely be deprecated in favor of perhaps
          aliasing some more mature and feature-rich library."
    :attribution "Alex Gunnarson"}
  quantum.core.data.xml
  (:require
    [quantum.core.ns          :as ns
      #+clj :refer #+clj [defalias alias-ns]]
    [quantum.core.error                 #+clj :refer #+clj :all]
    [quantum.core.logic                 #+clj :refer #+clj :all]
    [quantum.core.function              #+clj :refer #+clj :all]
    [quantum.core.type                  #+clj :refer #+clj :all]
    [quantum.core.numeric     :as num   :refer [nneg?]]
    [quantum.core.collections           :exclude [split] #+clj :refer #+clj :all]
    [quantum.core.data.vector :as vec   :refer [catvec]]
    [quantum.core.string      :as str   :refer [subs+]]
    [quantum.core.log         :as log])
  #+clj (:gen-class))

#+clj (ns/require-all *ns* :clj)
;___________________________________________________________________________________________________________________________________
;=================================================={       XML CREATION       }=====================================================
;=================================================={                          }=====================================================
#+clj (def brs-open  #(str "<" % ">"))
#+clj (def brs-close #(str "</" % ">"))

#+clj
(defn ^String tag-wrap
  "Wraps @body in XML tags specified by @tag."
  {:in [":tag" "body"]
   :out "\"<tag>body</tag>\""}
  ([^Keyword tag body] ; as in, XML wise
    (tag-wrap tag nil body))
  ([^Keyword tag opts body]
    (let [^String open-tag
            (-> tag name
                (whenf (constantly (nnil? opts))
                  (f*n str " " opts))
                brs-open)
          ^String flattened-str-body
            (whenf body coll?
              (partial apply str))
          ^String close-tag
            (-> tag name (take-while-not " ") brs-close)]
    (str open-tag flattened-str-body close-tag))))
#+clj
(defn ^String opt
  "Stringifies options with corresponding tags."
  [tag & options]
  (->> (cons (name tag) options)
       (interpose " ")
       (apply str)))
#+clj
(defn ^String into-xml
  "Parses a map structure into string XML."
  {:todo ["Could find a more efficient way of doing the recursion,
           even parallelizably."]}
  [^OrderedMap m0]
  (letfn [(into-xml-fn [^OrderedMap m]
            (reduce+
              (fn [ret ^Keyword tag v]
                (log/pr :alert "Tag:" tag "v class:" (class v))
                (conj ret
                  (if (map? v)
                      (tag-wrap tag
                         (into-xml-fn v)) ; Could probably find a more efficient way of doing this
                      (tag-wrap
                        tag
                        v))))
              []
              m))]
    (->> m0 into-xml-fn first)))
;___________________________________________________________________________________________________________________________________
;=================================================={       XML PARSING        }=====================================================
;=================================================={                          }=====================================================
#+clj
(defn split
  {:todo ["Likely not efficient"]}
  [^String xml-str]
  (-> xml-str str
      (str/replace #"<"    "\n<")
      (str/replace #">"    ">\n")
      (str/replace #"\n\n" "\n")
      (str/split   #"\n")
      ((partial remove+ empty?))
      redv))
#+clj
(defn open? [^String elem]
  (and (-> elem first+  (=    "<"))
       (-> elem second+ (not= "/"))
       (-> elem last+   (=    ">"))))
#+clj
(defn close? [^String elem]
  (-> elem (str/starts-with? "</")))
#+clj
(defn body? [^String elem]
  (-> elem first+ (not= "<")))
#+clj
(defn standalone? [^String elem]
  (and (or (-> elem (str/starts-with? "<" ))
           (-> elem (str/starts-with? "<?")))
       (or (-> elem (str/ends-with?   "/>"))
           (-> elem (str/ends-with?   "?>")))))

#+clj (defrecord XMLElem [elem-type tag content])
#+clj
(defn elem-type [^String elem]
  (condfc elem
    body?       :body
    standalone? :standalone
    open?       :open
    close?      :close
    :else       (throw+ 
                  (str "XML element "
                       (str/squote elem)
                       " not recognized."))))
#+clj
(defn ^Keyword tag
  "Retrieves the tag of a raw XML-string element @elem."
  {:in  ["<abc ?eht/>"]
   :out :abc}
  [^String elem]
  (let [^Keyword ktype (elem-type elem)]
    (if (= :body ktype)
        nil
        (let [^String un-bracketed (-> elem popl+ popr+)
              ^AFunction get-tag-from-out-of-properties
                (fn [^String elem-n]
                  (take-while-not elem-n " "))
              ^String tag-str
                (condp = ktype
                  :open       (-> un-bracketed       get-tag-from-out-of-properties)
                  :close      (-> un-bracketed popl+)
                  :standalone (-> un-bracketed popr+ get-tag-from-out-of-properties))]
          (keyword tag-str)))))
#+clj
(defn ^String content
  "Retrieves the content of a raw XML-string element @elem."
  {:in  ["<abc ?eht/>"]
   :out "?eht"}
  [^String elem]
  (let [^Keyword elem-type (elem-type elem)]
    (condp = elem-type
      :body       elem
      :standalone
        (let [^String un-bracketed (-> elem popl+ popr+ popr+)]
          (getr+ un-bracketed
            (inc (index-of+ un-bracketed  " "))
            (count+ un-bracketed)))
      nil)))
#+clj
(defn ^String normalize-content
  [^String content]
  (-> content
      (str/replace "&amp;" "&")
      (str/replace "&apos;" "'")
      (str/replace "&quot;" "'")
      (str/replace "&#146;" "'")
      str/remove-extra-whitespace))
#+clj
(defn label-elem
  {:in  "<myTag>"
   :out "{:type :open :tag :my-tag :content nil}"}
  [^String elem] 
  (XMLElem.
    (elem-type elem)
    (tag       elem)
    (content   elem)))

#+clj
(defprotocol XMLParse
  (parse 
    ^{:doc  "Single-threadedly goes through the XML and associates
             each tag accordingly."
      :todo ["Optimize for transients only to fall back on if it is determined
              that parallel processing doesn't work well"
             "Could probably do parallel proc by splitting it up into a given number
              of threads, based on the threadpool and the file size, and doing it
              accordingly. Then match up the 'loose ends.'"]}
    [xml]
    [xml-type xml]))
#+clj
(extend-protocol XMLParse
  Keyword
    (parse
      [xml-type xml-0] (parse xml-0 xml-type))
  iota.FileVector
    (parse
      ([xml-0]
        (parse xml-0 :qbxml))
      ([xml-0 ^Keyword xml-type]
        (let [^Fn split-item-if-necessary
                (fn-> str
                      (str/replace #"(?<!^)<" "\n<")
                      (str/replace #">(?!$)"  ">\n")
                      (str/split   #"\n"))
              ^Fn remove-trailing-characters
                (whenf*n (f*n str/ends-with? "\r")
                         popr+)
              ^Fn remove-qbxml-headers-if-requested
                (if (= xml-type :qbxml)
                    (fn->> popl+
                           popl+ popr+ 
                           popl+ popr+ 
                           popl+ popr+)
                    identity)
              ^Fn incorporate-split-items
                (fn->> (reduce+ catvec))] ; could probably parallelize this process
          (->> xml-0
               (remove+ nil?) ; to handle multiline descriptions
               (map+ remove-trailing-characters)
               (map+ split-item-if-necessary)
               ; for foldp+:
               ; No implementation of method "slicev" of protocol
               ; found for iota.FileVector
               redv
               remove-qbxml-headers-if-requested
               incorporate-split-items
               (map+ label-elem)
               foldv
               (<- parse xml-type)))))
  String
    (parse
      ^{:performance ["Is foldv wise here?"]}
      ([xml-0]
        (parse xml-0 :general))
      ([xml-0 xml-type]
        (->> xml-0 split
             (map+ label-elem)
             foldv
             (<- parse xml-type))))
  APersistentVector
    (parse
      ([xml-0]
        (parse xml-0 :general))
      ([xml-0 ^Keyword xml-type]
        (->> xml-0 vec/vector+ (<- parse xml-type))))
  clojure.core.rrb_vector.rrbt.Vector
    (parse
      ^{:performance ["|normalize-content| is a possible bottleneck"]
        :todo  ["Use volatiles instead of atoms"]
        :bench ["Test case: QB items list
                 Starts from raw file vector:
                   (->> (io/read :read-method :str-vec
                                 :directory [:test \"Responses\"
                                            \"response 00.txt\"])
                        xml/parse)
                 Current               ~5.8  sec
                 Uniques-only (bad)    5.07  sec
                 Old    (|parse-xml+|) 12.86 sec
                 Oldest (|parse-xml|)  25    sec"]}
      ([xml-0] (parse xml-0 :general))
      ([xml-0 ^Keyword xml-type]
        (let [^Atom traversal-keys (volatile! [] )
              ^Atom built-up-map   (volatile! {} )
              ^Atom final-result   (volatile! nil)
              ^Fn   unique-tag-if-needed ; gensym ensures no re-association
                (fn [^Keyword tag-n]
                  (if (contains?
                        (get-in @built-up-map @traversal-keys)
                        tag-n)
                      (do (log/pr :alert-core
                            "Tag" (str/squote tag-n) "exists." "Creating new tag")
                          (-> tag-n (str "##")
                              rest+ ; to remove colon
                              gensym keyword)) 
                      tag-n))] 
          (reduce+
            (fn [^Vec ret ^XMLElem elem]
              (let [^Key    elem-type (:elem-type elem)
                    ^Key    tag       (:tag       elem)
                    ^String content   (:content   elem)]
                (condp = elem-type
                  :standalone (conj ret {tag content})
                  :open       (do (log/pr :inspect-core "Found open element:" tag)
                                  (vswap! traversal-keys conj
                                    (unique-tag-if-needed tag))
                                  (log/pr :inspect-core "Conj'ed" (str/squote tag) ";"
                                    "Traversal keys now:" @traversal-keys)
                                  (swap! built-up-map
                                    assoc-in+
                                    @traversal-keys
                                    {}) ; there's gotta be a better way
                                  ret)
                  :close      (do (log/pr :inspect-core "Found close element:" tag)
                                  (if (single? @traversal-keys) ; last level to close out
                                      (do (vreset! traversal-keys [])
                                          (vreset! final-result @built-up-map)
                                          (log/pr :inspect-core
                                            "About to conj map result:" @final-result)
                                          (vreset! built-up-map   {})
                                          (conj ret @final-result))
                                      (do (vswap! traversal-keys popr+)
                                          ret)))
                  :body       (do (log/pr :inspect-core "Found body element:" content)
                                  (vswap! built-up-map
                                     assoc-in+
                                     @traversal-keys
                                     (normalize-content content)) ; Possible bottleneck
                                  ret)
                  (throw+
                    (str "Unknown XML element type:" " "
                         (str/squote elem-type)      " "
                         (str/paren (str "requested for tag" " " tag ", "
                                    "content" " " content)))))))
            []
            xml-0)))))

#+clj
(defn wrap [string body]
  (if (nil? body)
      []
      [(brs-open  string)
       body
       (brs-close string)]))

#+clj
(defn wrap-body [tag & body]
  (if (every? empty? body)
      []
      (catvec
        [(brs-open  tag)]
        (apply catvec body)
        [(brs-close tag)])))