(ns quantum.core.data.string
   (:import #?(:clj  com.carrotsearch.hppc.CharArrayDeque)
            #?(:cljs goog.string.StringBuffer)))

; TODO investigate http://ahmadsoft.org/ropes/ : A rope is a high performance replacement for Strings. The datastructure, described in detail in "Ropes: an Alternative to Strings", provides asymptotically better performance than both String and StringBuffer

(defn !str
  "Creates a mutable string."
  ([  ] #?(:clj (StringBuilder.   ) :cljs (StringBuffer.   )))
  ([a0] #?(:clj (StringBuilder. a0) :cljs (StringBuffer. a0))))

#?(:clj
(defn !sync-str
  "Creates a synchronized mutable string."
  []
  (StringBuffer.)))

; What about structural sharing with strings?
; Wouldn't there have to be some sort of compact immutable bit
; map or something to diff it rather than just making
; an entirely new string?

; TODO rework. Instead of |condf|, use records to represent
; parsed types and dispatch in |defnt| accordingly

; Currently only for strings
#_(:clj
(defn rreduce [f init ^String s]
  (loop [ret init i (-> s lasti int)] ; int because charAt requires int, I think
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

#?(:clj
(deftype StringWithMeta [^String s ^clojure.lang.IPersistentMap _meta]
  clojure.lang.IObj
    (meta        [this]       _meta)
    (withMeta    [this meta'] (StringWithMeta. s meta'))
  CharSequence
    (charAt      [this i]     (get s i))
    (length      [this]       (count s))
    (subSequence [this a b]   (.subSequence s a b))
  Object
    (toString    [this]       s)
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] s)))

#?(:clj
(defmethod print-method StringWithMeta [^StringWithMeta x ^java.io.Writer w]
  (print-method (.toString x) w)))

#?(:clj
(defn string-with-meta
  ([s] #?(:clj (StringWithMeta. s nil) :cljs s))
  ([s meta'] #?(:clj (StringWithMeta. s meta') :cljs (with-meta s new-meta)))))
