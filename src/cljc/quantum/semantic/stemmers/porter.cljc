(ns ^{:original-java "Rajiv Yerra"}
  quantum.semantic.stemmers.porter
           (:refer-clojure :exclude [get count reduce when-let])
           (:require
             [quantum.core.logic           :as logic
               :refer [nempty? #?@(:clj [when-let])]]
             [quantum.core.string          :as str  ]
             [quantum.core.collections     :as coll
               :refer [#?@(:clj [reduce get count]) in? dropr]   ]
             [quantum.core.string.semantic :as sem  ])
  #?(:cljs (:require-macros
             [quantum.core.logic           :as logic
               :refer [when-let]]
             [quantum.core.collections
               :refer [reduce get count]            ])))

(defn ends-with-doubled-consonant? [s]
  (when (nempty? s)
    (let [c (get s (-> s count (- 1)))]
      (and (= c (get s (-> s count (- 2))))
           (not (sem/contains-ext-vowel? (coll/taker 2 s)))))))

(defn cvc-measure
  [s]
  (first
    (reduce
      (fn [ret c]
        (let [[ct vowel-seen?] ret]
          (cond (sem/vowel? c)
                [ct true]
                vowel-seen?
                [(inc ct) false]
                :else ret)))
      [0 false]
      s)))
    
(defn ends-with-cvc?
  "Does stem end with CVC?"
  [s]
  (when (-> s count (>= 3))
    (let [c  (get s (-> s count (- 1)))
          v  (get s (-> s count (- 2)))
          c2 (get s (-> s count (- 3)))]
      (and (not (in? c #{\w \x \y}))
           (not (sem/vowel? c))
           (sem/vowel? v)
           (not (sem/vowel? c2))))))

(defn- step1a [s]
  (condp #(str/ends-with? %2 %1) s
    "sses" (dropr 2 s)
    "ies"  (dropr 2 s)
    "ss"   s
    "s"    (dropr 1 s)
    s))

(defn step1b2 [s]
  ; AT -> ATE
  (cond
    (logic/seq-or #(str/ends-with? s %) ["at" "bl" "iz"])
    (str s "e"))
    (and (ends-with-doubled-consonant? s)
         (not (logic/seq-or #(str/ends-with? s %1) ["l" "s" "z"])))
    (dropr 1 s)
    (and (= 1 (cvc-measure s))
         (ends-with-cvc? s))
    (str s "e")
    :else
    s)

(defn- step1b [s]
  (cond
    ; (m > 0) EED -> EE
    (str/ends-with? s "eed")
    (if (> (cvc-measure (dropr 3 s)) 0)
        (dropr 1 s)
        s)
    ; (*v*) ED ->
    (and (str/ends-with? s "ed")
         (sem/contains-ext-vowel? (dropr 2 s)))
    (step1b2 (dropr 2 s))
    ; (*v*) ING ->
    (and (str/ends-with? s "ing")
         (sem/contains-ext-vowel? (dropr 3 s))) 
    (step1b2 (dropr 3 s))
    :else
    s))

(defn step1c [s]
  ; (*v*) Y -> I
  (if (str/ends-with? s "y")
      (if (sem/contains-ext-vowel? (dropr 1 s))
          (str (dropr 1 s) "i")
          s)
      s))

(defn str-measure-step [s gt sub len & [suffix]]
  (when-let [_   (str/ends-with? s sub)
             s-f (dropr len s)
             _   (> (cvc-measure s-f) gt)]
    (if suffix (str s-f suffix) s-f)))

(defn step2 [s]
  (or (str-measure-step s 0 "ational" 5 "e" )
      (str-measure-step s 0 "tional"  2     )
      (str-measure-step s 0 "enci"    2     )
      (str-measure-step s 0 "anci"    1 "e" )
      (str-measure-step s 0 "izer"    1     )
      (str-measure-step s 0 "abli"    1 "e" )
      (str-measure-step s 0 "alli"    2     )
      (str-measure-step s 0 "entli"   2     )
      (str-measure-step s 0 "eli"     2     )
      (str-measure-step s 0 "ousli"   2     )
      (str-measure-step s 0 "ization" 5 "e" )
      (str-measure-step s 0 "ation"   3 "e" )
      (str-measure-step s 0 "ator"    2 "e" )
      (str-measure-step s 0 "alism"   3     )
      (str-measure-step s 0 "iveness" 4     )
      (str-measure-step s 0 "fulness" 4     )
      (str-measure-step s 0 "ousness" 4     )
      (str-measure-step s 0 "aliti"   3     )
      (str-measure-step s 0 "iviti"   3 "e" )
      (str-measure-step s 0 "biliti"  5 "le")
      s))

(defn step3 [s]
  (or (str-measure-step s 0 "icate" 3)
      (str-measure-step s 0 "ative" 5)
      (str-measure-step s 0 "alize" 3)
      (str-measure-step s 0 "iciti" 3)
      (str-measure-step s 0 "ical"  2)
      (str-measure-step s 0 "ful"   3)
      (str-measure-step s 0 "ness"  4)
      s))

(defn step4 [s]
  (or (str-measure-step s 1 "al"    2)
      (str-measure-step s 1 "ance"  4)
      (str-measure-step s 1 "ence"  4)
      (str-measure-step s 1 "er"    2)
      (str-measure-step s 1 "ic"    2)
      (str-measure-step s 1 "able"  4)
      (str-measure-step s 1 "ible"  4)
      (str-measure-step s 1 "ant"   3)
      (str-measure-step s 1 "ement" 5)
      (str-measure-step s 1 "ment"  3)
      (str-measure-step s 1 "ent"   3)
      (str-measure-step s 1 "sion"  3)
      (str-measure-step s 1 "tion"  3)
      (str-measure-step s 1 "ou"    2)
      (str-measure-step s 1 "ism"   3)
      (str-measure-step s 1 "ate"   3)
      (str-measure-step s 1 "iti"   3)
      (str-measure-step s 1 "ous"   3)
      (str-measure-step s 1 "ive"   3)
      (str-measure-step s 1 "ize"   3)
      s))

(defn step5a [s]
  (let [s-1      (dropr 1 s)
        measured (cvc-measure s-1)]
    (if (or (and (> measured 1)
                 (str/ends-with? s "e"))
            (and (= measured 1)
                 (not (ends-with-cvc? s-1))
                 (str/ends-with? s "e")))
        s-1
        s)))

(defn step5b [s]
  (if (and (str/ends-with? s "l")
           (ends-with-doubled-consonant? s)
           (> (cvc-measure (dropr 1 s)) 1))
      (dropr 1 s)
      s))

(defn stem [s]
  (assert (and (nempty? s) (str/alpha? s)) #{s})
  (-> s
      step1a
      step1a
      step1b
      step1c
      step2 
      step3 
      step4 
      step5a
      step5b))