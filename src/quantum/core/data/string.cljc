(ns quantum.core.data.string
  "A String is a special wrapper for a char array where different encodings, etc. are possible."
       (:require
         [quantum.core.type         :as t
           :refer [defnt]]
         [quantum.untyped.core.core :as ucore])
       (:import
#?(:clj  [com.carrotsearch.hppc CharArrayDeque])
#?(:cljs [goog.string           StringBuffer])))

(ucore/log-this-ns)

;; TODO investigate http://ahmadsoft.org/ropes/ : A rope is a high performance replacement for Strings. The datastructure, described in detail in "Ropes: an Alternative to Strings", provides asymptotically better performance than both String and StringBuffer
;; What about structural sharing with strings?
;; Wouldn't there have to be some sort of compact immutable bit map or something to diff it rather
;; than just making an entirely new string?

;; ===== General string-like entities ===== ;;

#?(:clj (def char-seq? (t/isa? java.lang.CharSequence)))

;; ===== Immutable strings ===== ;;

(def str? (t/isa? #?(:clj java.lang.String :cljs js/String)))

#_(defnt str ...) ; TODO TYPED

;; ----- Metable immutable strings ----- ;;

;; TODO TYPED `deftypet`
#?(:clj
(deftype MetableString [^String s ^clojure.lang.IPersistentMap _meta]
  clojure.lang.IObj
    (meta        [this]       _meta)
    (withMeta    [this meta'] (MetableString. s meta'))
  CharSequence
    (charAt      [this i]     (.charAt s i))
    (length      [this]       (.length s))
    (subSequence [this a b]   (.subSequence s a b))
  Object
    (toString    [this]       s)
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] s)))

#?(:clj
(defmethod print-method MetableString [^MetableString x ^java.io.Writer w]
  (print-method (.toString x) w)))

(def metable-str? #?(:clj (t/isa? MetableString) :cljs str?))

(defnt >metable-str
  > metable-str?
  ([s str?] #?(:clj (MetableString. s nil) :cljs s))
  ([s str?, meta' ??/meta?] #?(:clj (MetableString. s meta') :cljs (??/with-meta s new-meta))))

;; ===== Mutable strings ===== ;;

(def !str? (t/isa? #?(:clj java.lang.StringBuilder :cljs StringBuffer)))

(defnt !str
  "Creates a mutable string."
  > !str?
  ([]   #?(:clj (StringBuilder.)    :cljs (StringBuffer.)))
  ;; TODO
  #_([x0] #?(:clj (StringBuilder. x0) :cljs (StringBuffer. x0))))

;; ----- Synchronously mutable strings ----- ;;

#?(:clj (def !sync-str? (t/isa? java.lang.StringBuffer)))

#?(:clj
(defnt !sync-str
  "Creates a synchronized mutable string."
  > !sync-str?
  [] (StringBuffer.)))

;; ----- Mutable char deques ----- ;;

; TODO rework. Instead of |condf|, use records to represent
; parsed types and dispatch in |defnt| accordingly

; Currently only for strings
#_(:clj
(defn rreduce [f init ^String s]
  (loop [ret init i (-> s lasti int)] ; int because charAt requires int
    (if (>= i 0)
        (recur (f ret (.charAt s i)) (unchecked-dec i))
        ret))))
; Basically a double-sided StringBuilder
#_(:clj
(defn conjl! [^CharArrayDeque x ^String arg]
  (rreduce
    (fn [^CharArrayDeque ret c]
      (doto ret (.addFirst ^char (char c))))
    x
    arg)))

#_(:clj
(defn conjr! [^CharArrayDeque x ^String arg]
  (reduce
    (fn [^CharArrayDeque ret c]
      (doto ret (.addLast ^char (char c))))
    x
    arg)))

#_(:clj
(defnt concat!
  ([^com.carrotsearch.hppc.CharArrayDeque x arg] (conjr! x arg))
  ([^string? x arg] (conjl! arg x))
  #_([^fn? x arg] (conjl! ))))

#_(:clj
(defnt paren+
  ([^string? arg] (fn [sb] (conjl! sb "(") (conjr! sb arg) (conjr! sb ")")))
  ([^fn?     arg] (fn [sb] (conjl! sb "(") (conjr! sb (arg sb)) (conjr! sb ")")))))

;(conjl! sb (concat+ "abc" (paren+ (bracket+ "def"))))
;abc([def])

; class Operations extends PersistentVector.
; That way you have both contraint-classes and non-obfuscation of data.
; Data can be obfuscated by being too general, too.
;(bracket+ s) => (-> operations
;                    (conjr (*fn "]"))
;                    (conjl (*fn "[")))

; They can be broken down into operations
;(conjl! ")")
;(conjl! "]")
;(conjl! "def")
;(conjl! "[")
;(conjl! "(")
;(conjl! "abc")

#_(:clj
(defn sp+ [& args]
  (fn [sb]
    (doseqi [arg args n]
      (conjl! sb arg)
      (when (< n (-> args count dec))
        (conjl! sb " "))))))
