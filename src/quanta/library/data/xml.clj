(ns quanta.library.data.xml)
(require
  '[quanta.library.ns          :as ns    :refer [defalias alias-ns]])
(ns/require-all *ns* :clj)
(require
  '[quanta.library.error                 :refer :all]
  '[quanta.library.logic                 :refer :all]
  '[quanta.library.function              :refer :all]
  '[quanta.library.type                  :refer :all]
  '[quanta.library.numeric     :as num   :refer [nneg?]]
  '[quanta.library.collections           :exclude [split] :refer :all]
  '[quanta.library.data.vector :as vec   :refer [catvec]]
  '[quanta.library.string      :as str   :refer [subs+]]
  '[quanta.library.log         :as log])

; XML PARSING FROM STRINGS PRE-BROKEN BY NEWLINES VIA THEBUSBY.IOTA

; On creation, an index of the file will be constructed so random access will be O(1),
; similar to a normal Clojure vector. This is significantly more memory efficient than
; a vector of Strings.
;___________________________________________________________________________________________________________________________________
;=================================================={       XML CREATION       }=====================================================
;=================================================={                          }=====================================================
(def brs-open  #(str "<" % ">"))
(def brs-close #(str "</" % ">"))

(defn take-while-not
  [^String s ^String elem]
  (getr+ s 0
    (whenc (index-of+ s elem) (eq? -1)
      (count+ s))))

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
(defn ^String opt
  "Stringifies options with corresponding tags."
  [tag & options]
  (->> (cons (name tag) options)
       (interpose " ")
       (apply str)))
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
(defn split
  {:todo ["Likely not efficient"]}
  [^String xml-str]
  (-> xml-str str
      (str/replace #"<"    "\n<")
      (str/replace #">"    ">\n")
      (str/replace #"\n\n" "\n")
      (str/split   #"\n")
      ((partial remove+ empty?))
      fold+))

(defn open? [^String elem]
  (and (-> elem first+  (=    "<"))
       (-> elem second+ (not= "/"))
       (-> elem last+   (=    ">"))))
(defn close? [^String elem]
  (-> elem (str/starts-with? "</")))
(defn body? [^String elem]
  (-> elem first+ (not= "<")))
(defn standalone? [^String elem]
  (and (or (-> elem (str/starts-with? "<" ))
           (-> elem (str/starts-with? "<?")))
       (or (-> elem (str/ends-with?   "/>"))
           (-> elem (str/ends-with?   "?>")))))

(defrecord XMLElem [elem-type tag content])
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
(defn ^String normalize-content
  [^String content]
  (-> content
      (str/replace "&amp;" "&")
      (str/replace "&apos;" "'")
      (str/replace "&quot;" "'")
      (str/replace "&#146;" "'")
      str/remove-extra-whitespace))
(defn label-elem
  {:in  "<myTag>"
   :out "{:type :open :tag :my-tag :content nil}"}
  [^String elem] 
  (XMLElem.
    (elem-type elem)
    (tag       elem)
    (content   elem)))
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
(extend-protocol XMLParse
  Keyword
    (parse
      [xml-type xml-0] (parse xml-0 xml-type))
  iota.FileVector
    (parse
      ([xml-0]
        (parse xml-0 :qbxml))
      ([xml-0 ^Keyword xml-type]
        (let [^AFunction split-item-if-necessary
                (fn-> str
                      (str/replace #"(?<!^)<" "\n<")
                      (str/replace #">(?!$)"  ">\n")
                      (str/split   #"\n"))
              ^AFunction remove-qbxml-headers-if-requested
                (if (= xml-type :qbxml)
                    (fn->> popl+
                           popl+ popr+ 
                           popl+ popr+ 
                           popl+ popr+)
                    identity)
              ^AFunction incorporate-split-items
                (fn->> (reduce+ catvec))] ; could probably parallelize this process
          (->> xml-0
               (remove+ nil?) ; to handle multiline descriptions
               (map+ split-item-if-necessary)
               ; for foldp+:
               ; No implementation of method "slicev" of protocol
               ; found for iota.FileVector
               fold+
               remove-qbxml-headers-if-requested
               incorporate-split-items
               (map+ label-elem)
               foldp+
               (<- parse xml-type)))))
  String
    (parse
      ^{:performance ["Is foldp+ wise here?"]}
      ([xml-0]
        (parse xml-0 :general))
      ([xml-0 xml-type]
        (->> xml-0 split
             (map+ label-elem)
             foldp+
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
        (let [^Atom      traversal-keys (atom [] )
              ^Atom      built-up-map   (atom {} )
              ^Atom      final-result   (atom nil)
              ^AFunction unique-tag-if-needed ; gensym ensures no re-association
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
            (fn [^APersistentVector ret ^XMLElem elem]
              (let [^Keyword elem-type (:elem-type elem)
                    ^Keyword tag       (:tag       elem)
                    ^String  content   (:content   elem)]
                (condp = elem-type
                  :standalone (conj ret {tag content})
                  :open       (do (log/pr :inspect-core "Found open element:" tag)
                                  (swap! traversal-keys conj
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
                                      (do (reset! traversal-keys [])
                                          (reset! final-result @built-up-map)
                                          (log/pr :inspect-core
                                            "About to conj map result:" @final-result)
                                          (reset! built-up-map   {})
                                          (conj ret @final-result))
                                      (do (swap! traversal-keys popr+)
                                          ret)))
                  :body       (do (log/pr :inspect-core "Found body element:" content)
                                  (swap! built-up-map
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

(defn wrap [string body]
  (if (nil? body)
      []
      [(brs-open  string)
       body
       (brs-close string)]))
(defn wrap-body [tag & body]
  (if (every? empty? body)
      []
      (catvec
        [(brs-open  tag)]
        (apply catvec body)
        [(brs-close tag)])))