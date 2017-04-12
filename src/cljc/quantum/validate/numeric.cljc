(ns quantum.validate.numeric
  (:refer-clojure
    :exclude [conj! reduce])
  (:require
    [quantum.core.data.string
      :refer [!str]]
    [quantum.core.string.regex :as re]
    [quantum.core.collections  :as coll
      :refer [red-for, join join! reduce, conj!, subview, map+, range+]]))

; TODO move
(def num-char->int {\0 0 \1 1 \2 2 \3 3 \4 4 \5 5 \6 6 \7 7 \8 8 \9 9})
; TODO move
(def int->num-char (coll/invert num-char->int))

(defn dec-integer-at [n:str i]
  (-> n:str (get i) num-char->int dec (max 0)))

; TODO for better performance, see `https://github.com/jonschlinkert/to-regex-range/blob/master/index.js`
(defn equidistant-range-around-0->pattern
  "Given an integer `n`, produces the pattern that matches the range bounded
   by `-n` and `+n`."
  [n:str]
  (assert (and (string? n:str)
               (-> n:str count (> 0))))
  (str
    (re/bounded "0")
    "|"
    (re/bounded
      (re/c "-?")
      (re/nc
        (let [ct (count n:str)
              !s  (!str)
              _ (-> !s (conj! (re/nc "(?!0)\\d{1," (dec ct) "}")) (conj! "|"))]
          (->> (range+ ct)
               (map+   (fn [i]
                         ; TODO modularize these more; use mutable strings
                         (if (zero? i)
                             (str (re/range "1-" (max 1 (dec-integer-at n:str i))) "\\d" "{" (- ct i 1) "}")
                             (str "|" (subview n:str 0 (dec i))
                                      (re/range "0" "-" (if (= i (dec ct))
                                                            (get n:str i)
                                                            (dec-integer-at n:str i)))
                                      "\\d" "{" (- ct i 1) "}"))))
               (join! !s)))))))

(defn bit-range->pattern [abs-min abs-max]
  (str (re/bounded "-" abs-min)
       "|"
       (equidistant-range-around-0->pattern abs-max)))

; TODO make these bounds dynamic like byte:max-value:string
(def ^{:doc "Pattern for a `byte` value." } byte-pattern
  (bit-range->pattern "128"                 "127"))

(def ^{:doc "Regex for a `byte` value."   } byte-regex
  (re-pattern byte-pattern))

(def ^{:doc "Pattern for a `short` value."} short-pattern
  (bit-range->pattern "32768"               "32767"))

(def ^{:doc "Regex for a `short` value."  } short-regex
  (re-pattern short-pattern))

(def ^{:doc "Pattern for an `int` value." } int-pattern
  (bit-range->pattern "2147483648"          "2147483647"))

(def ^{:doc "Regex for an `int` value."   } int-regex
  (re-pattern int-pattern))

(def ^{:doc "Pattern for a `long` value." } long-pattern
  (bit-range->pattern "9223372036854775808" "9223372036854775807"))

(def ^{:doc "Regex for a `long` value."   } long-regex
  (re-pattern long-pattern))
