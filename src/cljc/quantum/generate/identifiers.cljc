(ns quantum.generate.identifiers
  (:refer-clojure :exclude [for array])
  (:require
    [#?(:clj  clojure.core
        :cljs cljs.core) :as core]
    #?(:clj
    [proteus
      :refer [let-mutable]   ])
    [quantum.core.error :as err
      :refer [->ex]]
    [quantum.core.numeric :as num]
    [quantum.core.fn
      :refer [<- fn->>]]
    [quantum.core.collections :as coll
      :refer [lfor for slice lasti join #?(:clj array)]]
    [quantum.core.nondeterministic :as rand]))

(def syls
  (-> (lfor
        [consonants ["b" "bl" "br"
                     "d"
                     "g" "gl" "gr"
                     "h"
                     "j"
                     "k" "kl"
                     "m"
                     "n"
                     "p" "pr" "pl"
                     "qu"
                     "r" "s" "z"
                     "t" "th" "thr" "tr"]
         vowels     ["a"      "ai" "ay"
                     "e" "ee" "ei"
                     "i" "ie"
                     "o" "oo" "oy"
                     "ue" ]]
        (str consonants vowels))
      vec
      (join ["wa" "wo" "ya" "yo"])))

(def neg-syl "wi")
(def neg-pattern (re-pattern (str "^" neg-syl "(.+)$")))

(def digits
  (#?(:clj array :cljs core/array) \0 \1 \2 \3 \4 \5
         \6 \7 \8 \9 \a \b
         \c \d \e \f \g \h
         \i \j \k \l \m \n
         \o \p \q \r \s \t
         \u \v \w \x \y \z))

#?(:clj
(defn int->digits
  {:tests '{(for [i (int->digits 123 2)] i)
            [1 1 1 1 0 1 1]}}
  [i-0 radix]
  (assert (> radix 1) #{radix})
  (let-mutable [i i-0
                buf (long-array 33)
                negative? (< i 0)
                charPos (core/int 32)] ; 32 because that's int length?
    (when (not negative?)
      (set! i (- i)))

    (while (<= i (- radix))
      (aset-long buf charPos (- (rem i radix)))
      (set! charPos (dec charPos))
      (set! i (core/int (num/div* i radix))))

    (aset-long buf charPos (- i))

    (when negative?
      (set! charPos (dec charPos))
      (aset-long buf charPos \-))
    (java.util.Arrays/copyOfRange buf charPos 33))))

#?(:clj
(defn int->word [n]
  (->> (int->digits n (count syls))
       (map (fn->> (get syls)))
       (apply str))))

;Given a Mnemo 'word', will split into its list of syllables.
;For example, "tsunashima" will be split into
;[ "tsu", "na", "shi", "ma" ]

; For Ruby
(defn getr* [x a b]
  (let [ct (count x)]
    (cond
      (> a b)
        (recur x b a)
      (>= b ct)
        (recur x a (dec ct))
      (>= a ct)
        (throw (->ex "Out of bounds"))
      (< a 0)
        (recur x 0 b)
      (< b 0)
        (throw (->ex "Out of bounds"))
      :else (slice x a (inc b)))))


; ===== HEROKU-ESQUE GENERATION =====

(def haiku-adjs
  ["autumn" "hidden" "bitter" "misty" "silent" "empty" "dry" "dark"
   "summer" "icy" "delicate" "quiet" "white" "cool" "spring" "winter"
   "patient" "unlit" "dawn" "crimson" "wispy" "weathered" "blue"
   "billowing" "cleft" "icy" "damp" "falling" "frosty" "green"
   "long" "late" "lingering" "bold" "little" "morning" "muddy"
   "reddened" "rough" "still" "small" "sparkling" "shy" "sole" "aged"
   "solemn" "soothing" "gentle" "unknown" "calm" "resolute" "endless"
   "wandering" "withered" "wild" "black" "young" "holy" "solitary"
   "fragrant" "aged" "snowy" "proud" "restless" "divine"
   "polished" "ancient" "purple" "lively" "nameless" "brazen" "sunless"
   "tattered"])

(def haiku-nouns
  ["wave" "waters" "brook" "waterfall" "river" "lake" "surf" "sea" "pond"
   "breeze" "moon" "wind"
   "hill"  "meadow" "mountain"
   "dust" "field" "fire" "flower"
   "silhouette" "glitter"
   "resonance" "silence" "sound"
   "darkness" "shade" "shadow" "smoke" "haze"
   "tree" "wood" "glade" "forest" "leaf" "pine"
   "violet" "wildflower" "grass"
   "dream" "cherry" "fog" "frost" "voice" "parchment"
   "frog" "sparrow" "butterfly" "firefly"
   "feather"
   "sky" "thunder" "rain" "snow" "snowflake" "cloud" "dew"
   "sun" "dawn" "morning" "sunset" "night"
   "star"
   "youth" "majesty"])

(defn gen-haiku-name []
  (str (get haiku-adjs  (rand/rand-int-between true 0 (lasti haiku-adjs )))
       "-"
       (get haiku-nouns (rand/rand-int-between true 0 (lasti haiku-nouns)))
       "-"
       (rand/rand-int-between true 1000 9999)))
