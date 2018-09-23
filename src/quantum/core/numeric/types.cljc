(ns quantum.core.numeric.types
         (:refer-clojure :exclude
           [denominator numerator ratio? #?@(:cljs [-compare]) read-string])
         (:require
           [clojure.core                      :as core]
           [clojure.string                    :as str]
           [clojure.tools.reader
             :refer [read-string]]
  #?(:cljs [com.gfredericks.goog.math.Integer :as int])
           [quantum.core.logic
             :refer [whenf fn-not fn=]]
           [quantum.core.type
             :refer [defnt]]
           [quantum.core.vars
             :refer [defalias]])
#?(:cljs (:require-macros
           [quantum.core.numeric.types :as self])))

(declare gcd)
(declare normalize)
#?(:cljs (declare ->bigint))
#?(:cljs (declare ->ratio))

#?(:cljs (defprotocol Add                 (-add                   [x y])))
#?(:cljs (defprotocol AddWithInteger      (-add-with-integer      [x y])))
#?(:cljs (defprotocol AddWithRatio        (-add-with-ratio        [x y])))
#?(:cljs (defprotocol Multiply            (-multiply              [x y])))
#?(:cljs (defprotocol MultiplyWithInteger (-multiply-with-integer [x y])))
#?(:cljs (defprotocol MultiplyWithRatio   (-multiply-with-ratio   [x y])))
#?(:cljs (defprotocol Invert              (-invert                [x]  )))
#?(:cljs (defprotocol Negate              (-negate                [x]  )))
#?(:cljs (defprotocol Ordered             (-compare               [x y])))
#?(:cljs (defprotocol CompareToInteger    (-compare-to-integer    [x y])))
#?(:cljs (defprotocol CompareToRatio      (-compare-to-ratio      [x y])))

#?(:cljs
(extend-type number
  Add                 (-add                   [x y] (-add                   (->bigint x) y))
  ;; I have a hard time reasoning about whether or not this is necessary
  AddWithInteger      (-add-with-integer      [x y] (-add-with-integer      (->bigint x) y))
  AddWithRatio        (-add-with-ratio        [x y] (-add-with-ratio        (->bigint x) y))
  Multiply            (-multiply              [x y] (-multiply              (->bigint x) y))
  MultiplyWithInteger (-multiply-with-integer [x y] (-multiply-with-integer (->bigint x) y))
  MultiplyWithRatio   (-multiply-with-ratio   [x y] (-multiply-with-ratio   (->bigint x) y))
  Negate              (-negate                [x]   (-negate                (->bigint x)  ))
  Ordered             (-compare               [x y] (-compare               (->bigint x) y))
  CompareToInteger    (-compare-to-integer    [x y] (-compare-to-integer    (->bigint x) y))
  CompareToRatio      (-compare-to-ratio      [x y] (-compare-to-ratio      (->bigint x) y))))

#?(:cljs
(extend-type com.gfredericks.goog.math.Integer
  Add                 (-add                   [x y] (-add-with-integer y x))
  AddWithInteger      (-add-with-integer      [x y] (.add x y))
  AddWithRatio        (-add-with-ratio        [x y] (-add-with-ratio (->ratio x) y))
  Multiply            (-multiply              [x y] (-multiply-with-integer y x))
  MultiplyWithInteger (-multiply-with-integer [x y] (.multiply x y))
  MultiplyWithRatio   (-multiply-with-ratio   [x y] (-multiply-with-ratio (->ratio x) y))
  Negate              (-negate                [x]   (.negate x))
  Invert              (-invert                [x]   (->ratio int/ONE x))
  Ordered             (-compare               [x y] (core/- (-compare-to-integer y x)))
  CompareToInteger    (-compare-to-integer    [x y] (.compare x y))
  CompareToRatio      (-compare-to-ratio      [x y] (-compare-to-ratio (->ratio x) y))
  IEquiv              (-equiv                 [x y] (and (instance? com.gfredericks.goog.math.Integer y) (.equals x y)))
  ;; dunno?
  IHash               (-hash                  [this] (reduce bit-xor 899242490 (.-bits_ this)))
  IComparable         (-compare               [x y]  (-compare x y))))


#?(:cljs
(deftype Ratio [n d]
  ;; "Ratios should not be constructed directly by user code; we assume n and d are
  ;;  canonical; i.e., they are coprime and at most n is negative."
  Object
    (toString [_] (str "#ratio [" n " " d "]"))
  Add            (-add              [x y] (-add-with-ratio y x))
  AddWithInteger (-add-with-integer [x y] (-add-with-ratio x (->ratio y)))
  AddWithRatio
    (-add-with-ratio [x y]
      (let [+ -add-with-integer
            * -multiply-with-integer
            n' (+ (* (.-n x) (.-d y))
                  (* (.-d x) (.-n y)))
            d' (* (.-d x) (.-d y))
            the-gcd (gcd n' d')]
        (normalize (.divide n' the-gcd) (.divide d' the-gcd))))
  Multiply            (-multiply              [x y] (-multiply-with-ratio y x        ))
  MultiplyWithInteger (-multiply-with-integer [x y] (-multiply            x (->ratio y)))
  MultiplyWithRatio
    (-multiply-with-ratio [x y]
      (let [* -multiply-with-integer
            n' (* (.-n x) (.-n y))
            d' (* (.-d x) (.-d y))
            the-gcd (gcd n' d')]
        (normalize (.divide n' the-gcd) (.divide d' the-gcd))))
  Negate (-negate [x] (->ratio (-negate n) d))
  Invert           (-invert             [x]   (normalize d n))
  Ordered          (-compare            [x y] (core/- (-compare-to-ratio y x)))
  CompareToInteger (-compare-to-integer [x y] (-compare-to-ratio x (->ratio y)))
  CompareToRatio
    (-compare-to-ratio [x y]
      (let [* -multiply-with-integer]
        (-compare-to-integer (* (.-n x) (.-d y))
                             (* (.-n y) (.-d x)))))
  IEquiv
    (-equiv [_ other]
      (and (instance? Ratio other)
           (core/= n (.-n other))
           (core/= d (.-d other))))
  IHash
    (-hash [_] (bit-xor 124790411 (-hash n) (-hash d)))
  IComparable
    (-compare [x y] (-compare x y))))

#?(:cljs
(defn- normalize
  [n d]
  (if (.isNegative d)
    (let [n' (.negate n)
          d' (.negate d)]
      (if (.equals d' int/ONE)
          n'
          (->ratio n' d')))
    (if (.equals d int/ONE)
        n
        (->ratio n d)))))



#?(:clj  (defalias numerator core/numerator)
   :cljs (defnt numerator
           ([^ratio? x] (.-n x))))

#?(:clj  (defalias denominator core/denominator)
   :cljs (defnt denominator
           ([^ratio? x] (.-d x))))

#?(:clj  (defalias ratio? core/ratio?)
   :cljs (defn ratio? [x] (instance? Ratio x)))

(defn read-rational
  "Create cross-platform literal rational numbers from decimal, without intermediate inexact
   (e.g. float/double) representation.
   #r 2.712 -> (rationalize 2.712M)"
  {:todo #{"Support exponent notation e.g. 2.313E7 | 2.313e7"}}
  [^String r]
  (let [r-str (cond (string? r)
                    r
                    (symbol? r)
                    (do (assert (-> r namespace nil?))
                        (assert (-> r name first (= \r)))
                        (->> r name rest (apply str))))
        minus-ct (->> r-str (filter #(= % \-)) count)
        _        (assert (#{0 1} minus-ct))
        r-str    (case minus-ct
                   0 r-str
                   1 (do (assert (-> r-str first (= \-)))
                         (->> r-str rest (apply str))))
        [integral-str decimal-str :as split] (str/split r-str #"\.")
        _ (when (-> split count (> 2))
            (throw (ex-info "Number cannot have more than one decimal point" {:num r-str})))
        _ (doseq [s split]
            (when-not (every? #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9} s)
              (throw (ex-info "Number must have only numeric characters" {:num s}))))
        integral (read-string integral-str)
        decimal  (read-string decimal-str)
        scale    (if decimal
                     (#?(:clj Math/pow :cljs js/Math.pow) 10 (count decimal-str))
                     1)]
    (* (if (= minus-ct 1) -1 1)
       (->ratio (+ (* scale integral) (or decimal 0))
                scale))))
