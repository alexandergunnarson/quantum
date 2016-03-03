(ns quantum.core.data.string
  (:require-quantum [:core ccore])
  #?(:clj (:import com.carrotsearch.hppc.CharArrayDeque)))

; What about structural sharing with strings?
; Wouldn't there have to be some sort of compact immutable bit
; map or something to diff it rather than just making
; an entirely new string?

; TODO rework. Instead of |condf|, use records to represent
; parsed types and dispatch in |defnt| accordingly

; Currently only for strings
#?(:clj
(defn rreduce [f init ^String s]
  (loop [ret init i (-> s lasti int)] ; int because charAt requires int, I think
    (if (>= i 0)
        (recur (f ret (.charAt s i)) (unchecked-dec i))
        ret))))
; Basically a double-sided StringBuilder
#?(:clj
(defn conjl! [^CharArrayDeque x ^String arg]
  (rreduce
    (fn [^CharArrayDeque ret c]
      (doto ret (.addFirst ^char (char c))))
    x
    arg)))

#?(:clj
(defn conjr! [^CharArrayDeque x ^String arg]
  (reduce
    (fn [^CharArrayDeque ret c]
      (doto ret (.addLast ^char (char c))))
    x
    arg)))

#?(:clj
(defnt concat!
  ([^com.carrotsearch.hppc.CharArrayDeque x arg] (conjr! x arg))
  ([^string? x arg] (conjl! arg x))
  #_([^fn? x arg] (conjl! ))))

#?(:clj
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


#?(:clj
(defn sp+ [& args]
  (fn [sb]
    (doseqi [arg args n]
      (conjl! sb arg)
      (when (< n (lasti args))
        (conjl! sb " "))))))
