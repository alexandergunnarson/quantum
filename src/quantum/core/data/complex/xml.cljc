(ns
  ^{:doc "Useful XML functions."
    :attribution "alexandergunnarson"}
  quantum.core.data.complex.xml
  (:refer-clojure :exclude [reduce split])
  (:require
#?@(:clj
   [[clojure.data.xml              :as cxml]])
    [quantum.core.collections      :as coll
      :refer [lasti map+ partition-all+ join reduce unique-conj]]
    [quantum.core.error            :as err
      :refer [>ex-info TODO catch-all]]
    [quantum.core.fn               :as fn
      :refer [fn->]]
    [quantum.core.log              :as log
      :refer [prl]]
    [quantum.core.logic
      :refer [whenp]]
    [quantum.core.string           :as str   ]
    [quantum.security.cryptography :as crypto]
    [quantum.core.macros           :as macros
      :refer [defnt]]
    [quantum.core.time.core        :as time]
    [quantum.core.vars             :as var
      :refer [def- defalias]])
#?(:cljs
  (:require-macros
    [quantum.core.data.complex.xml :as self]))
#?(:clj
  (:import
    [javax.xml.stream XMLInputFactory XMLEventReader]
    [javax.xml.stream.events XMLEvent Attribute
      StartElement EndElement Characters]
    [javax.xml.namespace QName]
    java.io.StringReader
    java.util.Iterator)))

; TODO use some non-lazy XML library

;___________________________________________________________________________________________________________________________________
;=================================================={       XML CREATION       }=====================================================
;=================================================={                          }=====================================================
; TODO use some non-lazy XML library
;___________________________________________________________________________________________________________________________________
;=================================================={       XML PARSING        }=====================================================
;=================================================={                          }=====================================================
; ALSO APPLIES TO HTML

#?(:clj (defrecord XMLAttr [name val]))
#?(:clj (defrecord XMLElem [name attrs val children]))

#?(:clj
(defn xml-attr-vec [^Iterator iter]
  (when (.hasNext iter)
    (let [vec-f (transient [])]
      (while (.hasNext iter)
        (let [^Attribute attr-n (.next iter)]
          (conj! vec-f (XMLAttr. (.getLocalPart ^QName (.getName attr-n))
                                 (-> attr-n .getValue)))))
      (persistent! vec-f)))))

#?(:clj
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
      (persistent! map-f)))))

; TODO this isn't right and you need to fix it...
(def down           (fn-> first :children))
(def- first-content (fn-> :content first))

#?(:clj
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
                         nil))))

#?(:clj
(defn chars-handler [^Characters obj chars e-ct stack xml-f]
  ((or chars :null) obj)
  (assoc! stack (lasti stack)
    (assoc (peek stack) :val (.getData obj)))))

#?(:clj
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
          (pop! stack)))))

(def default-handler (fn [obj]))

#?(:clj
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
                      (do (unk-elem-handler   xml-event)))))))))

; ==== PLIST PARSING ====

(def ^:dynamic *keywordize* identity)
(def ^:dynamic *skip-unparseable* #{})

(defmulti parse-plist (fn [x] (:tag x)))

(defmethod parse-plist :array [x]
  (->> x :content (map+ parse-plist) (join [])))

(defmethod parse-plist :data [x]
  (catch-all
    (->> x first-content (crypto/decode :base64))
    e
    (when-not (contains? *skip-unparseable* :data)
      (throw e))))

(defmethod parse-plist :date [x]
  (-> x first-content time/->zoned-date-time))

(defmethod parse-plist :dict [x]
  (->> x :content (map+ parse-plist) (partition-all+ 2) (reduce unique-conj {})))

(defmethod parse-plist :true    [x] true)
(defmethod parse-plist :false   [x] false)

(defmethod parse-plist :key     [x] (-> x first-content *keywordize*))

(defmethod parse-plist :integer [x] (str/val (first-content x)))
(defmethod parse-plist :real    [x] (str/val (first-content x)))

(defmethod parse-plist :string  [x] (first-content x))

#?(:clj
(defnt lparse
  ([^string? x] (-> x (java.io.StringReader.) (java.io.BufferedReader.) cxml/parse))
  ([^file?   x] (-> x (java.io.FileReader.)   (java.io.BufferedReader.) cxml/parse))
  ; TODO Just [x opts] gives `clojure.lang.ArityException: Wrong number of args (2)` when you pass a `file?` or `string?`
  ; This is because in the protocol, the function relating to `file?` or `string?` is found, but only has one argument,
  ; rather than trying to first find by arity, then by class
  ([#{string? file?} x opts] (binding [*keywordize* (:keywordize opts)] (lparse x)))))
