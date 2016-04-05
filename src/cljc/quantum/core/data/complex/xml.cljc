(ns
  ^{:doc "Useful XML functions."
    :attribution "Alex Gunnarson"}
  quantum.core.data.complex.xml
  (:refer-clojure :exclude [split])
  (:require-quantum [:core err logic fn type #_num #_coll vec #_str macros log #_time #_crypto])
  #?(:clj (:require [clojure.data.xml :as cxml]))
  #?(:clj (:import (javax.xml.stream XMLInputFactory XMLEventReader)
                   (javax.xml.stream.events XMLEvent Attribute
                     StartElement EndElement Characters)
                   (javax.xml.namespace QName)
                   java.io.StringReader
                   java.util.Iterator)))

; ENTIRE FILE IS CLJ-ONLY (for now)
; Commented out temporarily 
#_(:clj (do
;___________________________________________________________________________________________________________________________________
;=================================================={       XML CREATION       }=====================================================
;=================================================={                          }=====================================================
; (def brs-open  #(str "<" % ">"))
; (def brs-close #(str "</" % ">"))

; (defn ^String tag-wrap
;   "Wraps @body in XML tags specified by @tag."
;   {:in [":tag" "body"]
;    :out "\"<tag>body</tag>\""}
;   ([^Keyword tag body] ; as in, XML wise
;     (tag-wrap tag nil body))
;   ([^Keyword tag opts body]
;     (let [^String open-tag
;             (-> tag name
;                 (whenf (constantly (nnil? opts))
;                   (f*n str " " opts))
;                 brs-open)
;           ^String flattened-str-body
;             (whenf body coll?
;               (partial apply str))
;           ^String close-tag
;             (-> tag name (take-untili+ " ") brs-close)]
;     (str open-tag flattened-str-body close-tag))))

; (defn ^String opt
;   "Stringifies options with corresponding tags."
;   [tag & options]
;   (->> (cons (name tag) options)
;        (interpose " ")
;        (apply str)))

; (defn ^String into-xml
;   "Parses a map structure into string XML."
;   {:todo ["Could find a more efficient way of doing the recursion,
;            even parallelizably."]}
;   [^OrderedMap m0]
;   (letfn [(into-xml-fn [^OrderedMap m]
;             (reduce
;               (fn [ret ^Keyword tag v]
;                 (log/pr :alert "Tag:" tag "v class:" (class v))
;                 (conj ret
;                   (if (map? v)
;                       (tag-wrap tag
;                          (into-xml-fn v)) ; Could probably find a more efficient way of doing this
;                       (tag-wrap
;                         tag
;                         v))))
;               []
;               m))]
;     (->> m0 into-xml-fn first)))
; ;___________________________________________________________________________________________________________________________________
; ;=================================================={       XML PARSING        }=====================================================
; ;=================================================={                          }=====================================================
; ALSO APPLIES TO HTML

(defn split
  {:todo ["Likely not efficient"]}
  [^String xml-str]
  (->> xml-str str
      (<- str/replace #"<"    "\n<")
      (<- str/replace #">"    ">\n")
      (<- str/replace #"\n\n" "\n")
      (<- str/split   #"\n")
      (remove+ (partial every? (fn-eq? \space)))
      redv))

(defn open? [^String elem]
  (and (-> elem first  (=    "<"))
       (-> elem second (not= "/"))
       (-> elem last   (=    ">"))))

(defn close? [^String elem]
  (-> elem (str/starts-with? "</")))

(defn body? [^String elem]
  (-> elem first (not= "<")))

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

; NEW


(defrecord XMLAttr [name val])
(defrecord XMLElem [name attrs val children])

(defn xml-attr-vec [^Iterator iter]
  (when (.hasNext iter)
    (let [vec-f (transient [])]
      (while (.hasNext iter)
        (let [^Attribute attr-n (.next iter)]
          (conj! vec-f (XMLAttr. (.getLocalPart ^QName (.getName attr-n))
                                 (-> attr-n .getValue)))))
      (persistent! vec-f))))

(defn xml-attr-map [^Iterator iter keywordize?]
  (when (.hasNext iter)
    (let [map-f (transient {})]
      (while (.hasNext iter)
        (let [^Attribute attr-n (.next iter)
              k (.getLocalPart ^QName (.getName attr-n))
              v (.getValue attr-n)]
          (assoc! map-f
            (if keywordize?
                (keyword k)
                k)
            (if (and keywordize? (= k "type"))
                (keyword v)
                v))))
      (persistent! map-f))))

; TODO this isn't right and you need to fix it...
(def down           (fn-> first :children))
(def- first-content (fn-> :content first))

(defn start-elem-handler
  [^StartElement obj start-elem e-ct stack xml-f
   aggregate-props? keywordize-attrs? keywordize-names?]
  ((or start-elem :null) obj)
  (vreset! e-ct (long 0))
  (conj! stack (XMLElem. (if keywordize-names?
                             (-> (.getLocalPart ^QName (.getName obj)) keyword)
                             (-> (.getLocalPart ^QName (.getName obj))))
                         (if aggregate-props?
                             (-> obj .getAttributes (xml-attr-map keywordize-attrs?))
                             (-> obj .getAttributes xml-attr-vec))
                         nil
                         nil)))

(defn chars-handler [^Characters obj chars e-ct stack xml-f]
  ((or chars :null) obj)
  (assoc! stack (lasti stack)
    (assoc (peek stack) :val (.getData obj))))

(defn end-elem-handler [^EndElement obj end-elem e-ct stack xml-f]
  ((or end-elem :null) obj)
  (vreset! e-ct (unchecked-inc (long @e-ct)))
  (if (> (long @e-ct) 1) ; going up a level
      (do (assoc! stack (lasti stack)
            (assoc (peek stack) :children (persistent! @xml-f)))
          (vreset! xml-f (transient [])) ; new container
          (conj! @xml-f (peek stack))
          (pop! stack))
      (do (conj! @xml-f (peek stack)) ; Add curr to temp elem-container
          (pop! stack))))

(def default-handler (fn [obj]))

(defn parse
  {:performance "360 µs vs. clojure.data.xml's 865 µs! :D
                 268 µs vs. 402 µs
                 Some tests shows them to be the same,
                 but most show this to be better! Yay eagerness!"
   :todo ["Apparently is broken sometimes... FIX THIS"]}
  ([s] (parse s nil))
  ([s
    {:keys [start-doc start-elem chars end-elem end-doc unk-elem
            aggregate-props? keywordize-attrs? keywordize-names?]
     :as opts}]
    (let [stack (transient [])
          xml-f (volatile! (transient []))
          e-ct  (volatile! (long 0))
          start-doc-handler  (or start-doc default-handler)
          end-doc-handler    (or end-elem  default-handler)
          unk-elem-handler   (or unk-elem  default-handler)
          ^XMLInputFactory xif (XMLInputFactory/newInstance)
          ^XMLEventReader  xer (.createXMLEventReader xif (StringReader. s))]
            (loop [^XMLEvent xml-event (.nextEvent xer)]
              (if-not (.hasNext xer)
                  (persistent! @xml-f)
                  (cond
                    (.isStartDocument xml-event)
                      (do (start-doc-handler  xml-event)
                          (recur (.nextEvent xer)))
                    (.isStartElement xml-event)
                      (do (start-elem-handler (.asStartElement xml-event)
                            start-elem e-ct stack xml-f
                            aggregate-props? keywordize-attrs? keywordize-names?)
                          (recur (.nextEvent xer)))
                    (.isCharacters   xml-event)
                      (do (chars-handler      (.asCharacters   xml-event)
                            chars e-ct stack xml-f)
                          (recur (.nextEvent xer)))
                    (.isEndElement   xml-event)
                      (do (end-elem-handler   (.asEndElement   xml-event)
                            end-elem e-ct stack xml-f)
                          (recur (.nextEvent xer)))
                    (.isEndDocument  xml-event)
                      (do (end-doc-handler    xml-event)
                          (recur (.nextEvent xer)))
                    :else
                      (do (unk-elem-handler   xml-event))))))))

; ==== PLIST PARSING ====

(defmulti parse-plist (fn [c] (:tag c)))

(defmethod parse-plist :array [c]
  (for [item (:content c)] (parse-plist item)))

(defmethod parse-plist :data [c]
  (-> c first-content (crypto/decode :base64)))

(defmethod parse-plist :date [c]
  (-> c first-content (org.joda.time.DateTime.) time/->instant))

(defmethod parse-plist :dict [c]
  (apply hash-map (for [item (:content c)] (parse-plist item))))

(defmethod parse-plist :true [c] true)
(defmethod parse-plist :false [c] false)

(defmethod parse-plist :key [c] (first-content c))

(defmethod parse-plist :integer [c] (str/val (first-content c)))
(defmethod parse-plist :real    [c] (str/val (first-content c)))

(defmethod parse-plist :string [c] (first-content c))




))

#?(:clj
(defnt lparse
  ([^string? x]
    (-> x (java.io.StringReader.) (java.io.BufferedReader.) cxml/parse))
  ([^file?   x]
    (-> x (java.io.FileReader.)   (java.io.BufferedReader.) cxml/parse))
  #_([#{string? file?} data k]
    (throw-unless (contains? #{:plist} k) (->ex nil "Parser option not recognized" k))
    (condp = k
      :plist (->> data lparse first-content parse-plist)))))

