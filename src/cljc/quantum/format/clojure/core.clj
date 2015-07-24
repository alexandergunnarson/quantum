(ns quantum.format.clojure.core
  (:refer-clojure :exclude [format])
  (:require-quantum [:lib])
  (:require [quantum.analyze.clojure.predicates :refer :all]
            [quantum.analyze.clojure.core       :refer :all]))

(def file-loc "/Users/alexandergunnarson/Development/Source Code Projects/quanta-test/test/temp.java")
(def parsed (-> (java.io.File. ^String file-loc) quantum.compile.java/parse quantum.compile.java/clean))

; TODO rework. Instead of |condf|, use records to represent
; parsed types and dispatch in |defnt| accordingly
(import 'com.carrotsearch.hppc.CharArrayDeque)

; Currently only for strings
(defn rreduce [f init ^String s]
  (loop [ret init i (-> s lasti int)] ; int because charAt requires int, I think
    (if (>= i 0)
        (recur (f ret (.charAt s i)) (unchecked-dec i))
        ret)))
; Basically a double-sided StringBuilder
(defn conjl! [^CharArrayDeque x ^String arg]
  (rreduce
    (fn [^CharArrayDeque ret c]
      (doto ret (.addFirst ^char (char c))))
    x
    arg))

(defn conjr! [^CharArrayDeque x ^String arg]
  (reduce
    (fn [^CharArrayDeque ret c]
      (doto ret (.addLast ^char (char c))))
    x
    arg))

(defnt concat!
  [CharArrayDeque] ([x arg] (conjr! x arg))
  string?          ([x arg] (conjl! arg x))
  #_fn?          #_([x arg] (conjl! )))

(defnt paren+
  string? ([arg] (fn [sb] (conjl! sb "(") (conjr! sb arg) (conjr! sb ")")))
  fn?     ([arg] (fn [sb] (conjl! sb "(") (conjr! sb (arg sb)) (conjr! sb ")"))))

(conjl! sb (concat+ "abc" (paren+ (bracket+ "def"))))
abc([def])

; class Operations extends PersistentVector.
; That way you have both contraint-classes and non-obfuscation of data.
; Data can be obfuscated by being too general, too.
(bracket+ s) => (-> operations
                    (conjr (*fn "]"))
                    (conjl (*fn "[")))

; They can be broken down into operations
(conjl! ")")
(conjl! "]")
(conjl! "def")
(conjl! "[")
(conjl! "(")
(conjl! "abc")


(defn sp+ [& args]
  (fn [sb]
    (doseqi [arg args n]
      (conjl! sb arg)
      (when (< n (lasti args))
        (conjl! sb " ")))))


(defn format
  ([x] (String. (.toArray ^CharArrayDeque (format x "" 0 false))))
  ([x s i split?]
    (concat! s ; otherwise you end up appending in weird places...
      (if (sequential? x)
          (condf x
            do-statement?
              (paren+
                (sp+ (-> x first str) (rest x)))
            defn-statement?
              (constantly nil)
            :else (constantly nil))
          x)))) ; will get converted to a string anyway





