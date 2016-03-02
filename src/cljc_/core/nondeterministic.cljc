(ns
  ^{:doc "A few functions copied from thebusby.bagotricks.
          Not especially used at the moment."
    :attribution "Alex Gunnarson"}
  quantum.core.nondeterministic
  (:refer-clojure :exclude [bytes])
  (:require-quantum
    [:core err fn logic log async macros #_convert
     ;coll macros thread  
     ])
  (:require [quantum.core.lexical.core :as lex  ]
            [quantum.core.type.predicates :refer [regex?]]
            #?(:clj [loom.gen                  :as g-gen])) ; for now
  #?(:clj
  (:import java.util.Random
           java.security.SecureRandom
           [org.apache.commons.codec.binary Base64 Base32 Hex])))

; Affects KeyGenerator, KeyPairGenerator, KeyAgreement, and Signature.
; http://java-performance.com
; Do not share an instance of java.util.Random between several threads in any circumstances, wrap it in ThreadLocal instead.
; From Java 7 prefer java.util.concurrent.ThreadLocalRandom to java.util.Random in all
; circumstances - it is backwards compatible with existing code, but uses cheaper
; operations internally.
#?(:clj (defonce secure-random-generator (SecureRandom/getInstance "SHA1PRNG")))

#?(:clj
(defn get-generator [secure?]
  (if secure?
      secure-random-generator
      (java.util.concurrent.ThreadLocalRandom/current))))

#?(:cljs
(defn rand-prime [callback & web-workers?]
  (js/forge.prime.generateProbablePrime 1024
    #js {:algorithm "PRIMEINC"
         ; -1 means auto-optimiing 
         :workers (if web-workers? -1 0)}
    (fn [err n] (callback (.toString n 16))))))

(defn gen-native-secure-random-seeder []
  ; Register the main thread to send entropy or a Web Worker to receive
  ; entropy on demand from the main thread
  #?(:clj  (do nil)
           #_(async-loop {:id :native-secure-random-seeder}
       []
       (let [native-inst (SecureRandom/getInstance "NativePRNG")
             sleep-time  (* 10 (+ 50 (rand-int 5000)))
             random-bytes  (byte-array 512)
             _ (.nextBytes ^Random native-inst random-bytes)]
         (.setSeed ^SecureRandom secure-random-generator ^"[B" random-bytes)
         (async/sleep sleep-time)
         (recur)))
     :cljs (js/forge.random.registerWorker js/self)))

#_(:clj
(defonce native-secure-random-seeder
  (when (System/getProperty "quantum.core.cryptography:secure-PRNG")
    (gen-native-secure-random-seeder))))

; TODO shorten all this repetitiveness by adding no-arg option to defnt

#?(:clj
(defn rand-int-between
  ([        a b] (rand-int-between false a b))
  ([secure? a b]
    (let [^Random generator (get-generator secure?)]
      (+ a (.nextInt generator (inc (- b a))))))))

; js.Math/random

#?(:clj
(defn rand-char-between
  ([        a b] (rand-char-between false a b))
  ([secure? a b] (char (rand-int-between secure? a b)))))

#?(:clj
(defn ^String rand-chars-between
  ([n a b] (rand-chars-between false n a b))
  ([secure? n a b]
    (let [sb (StringBuilder.)]
      (dotimes [m n]
        (.append sb (rand-char-between secure? a b)))
      (str sb)))))

#?(:clj
(defnt ^String rand-numeric*
  ([^integer? n        ] (rand-chars-between         n 48 57 ))
  ([^boolean? secure?  ] (rand-char-between  secure?   48 57 ))
  ([^boolean? secure? n] (rand-chars-between secure? n 48 57 ))))

#?(:clj
(defn rand-numeric
  ([   ] (rand-numeric* false))
  ([a  ] (rand-numeric* a))
  ([a b] (rand-numeric* a b))))

#?(:clj
(defnt ^String rand-upper*
  ([^integer? n        ] (rand-chars-between         n 65 90 ))
  ([^boolean? secure?  ] (rand-char-between  secure?   65 90 ))
  ([^boolean? secure? n] (rand-chars-between secure? n 65 90 ))))

#?(:clj
(defn rand-upper
  ([   ] (rand-upper* false))
  ([a  ] (rand-upper* a))
  ([a b] (rand-upper* a b))))

#?(:clj
(defnt ^String rand-lower*
  ([^integer? n        ] (rand-chars-between         n 97 122))
  ([^boolean? secure?  ] (rand-char-between  secure?   97 122))
  ([^boolean? secure? n] (rand-chars-between secure? n 97 122))))

#?(:clj
(defn rand-lower
  ([   ] (rand-lower* false))
  ([a  ] (rand-lower* a))
  ([a b] (rand-lower* a b))))

#?(:clj
(def generators
  {:numeric rand-numeric
   :upper   rand-upper
   :lower   rand-lower}))

(defn rand-chars
  "Returns a random string that matches the regular expression."
  {:from "weavejester/re-rand"}
  [x]
  (assert (regex? x))
  (let [[generator not-matched] (lex/pattern (str x))]
    (when (empty? not-matched)
      (lex/first-if-single (generator)))))

; (defn rand-bytes
;   #?(:cljs {:todo ["Comes out in Forge bytes string, but should be in UInt8Array"]})
;   ([size] (rand-bytes false size))
;   ([secure? size]
;     #?(:clj  (let [^Random generator (get-generator secure?)
;                    bytes-f (byte-array size)
;                    _    (.nextBytes generator bytes-f)]
;                bytes-f)
;        :cljs (if secure?
;                  (js/forge.random.getBytesSync size)
;                  (throw (->ex :illegal-argument "Insecure random generator not supported."))))))

; ; TODO DEPS ONLY
; #_(defn rand-longs
;   {:todo ["Base off of random longs, for speed"]}
;   ([size] (rand-longs false size))
;   ([secure? size]
;     ; * 8 because longs are 8 bytes
;     (conv/bytes->longs (rand-bytes secure? (* 8 size)))))

; ; TODO DEPS ONLY
; #_(:clj
; (defn ^String rand-string
;   {:todo ["Performance of |rand-string| vs. |(String. rand-bytes)|"]}
;   ([n] (rand-string n nil))
;   ([n opts]
;     (if (or (nil? opts)
;             (and (map? opts)
;                  (-> opts keys count (= 1))
;                  (-> opts keys first (= :secure?))))
;         (rand-chars-between (:secure? opts) n
;           (core/int Character/MIN_VALUE)
;           (core/int Character/MAX_VALUE))
;         (let [opts-indexed (zipmap (coll/lrange) opts)
;               sb (StringBuilder.)]
;           (dotimes [i n]
;             (let [generator-k (get opts-indexed
;                                 (rand-int-between (:secure? opts) 0 (-> opts count dec)))
;                   generator (get generators generator-k)]
;               (.append sb (generator (whenc (:secure? opts) nil? false)))))
;           (str sb))))))

; #?(:clj
; (defn rand-graph
;   "Creates a random graph."
;   [type & args]
;   (condp = type
;     :probability (apply g-gen/gen-rand-p args)
;     :default     (apply g-gen/gen-rand   args)
;     (apply g-gen/gen-rand args))))

; ; OTHER MORE COMPLEX FUNCTIONS

; #?(:clj
; (defmacro cond-percent*
;   {:attribution "thebusby.bagotricks"}
;   [random-percent & clauses]
;   (let [cond-details (->> clauses
;                           (partition 2 2 nil)
;                           (sort-by (MWA first) >)
;                           (reduce (fn [[agg total] [percent clause]]
;                                     (if (not (and percent clause))
;                                       (throw (->ex nil "cond-percent requires an even number of forms"))
;                                       (let [nval (+ percent total)]
;                                         [(conj agg
;                                                (list clojure.core/<
;                                                      random-percent
;                                                      nval)
;                                                clause)
;                                          nval])))
;                                   [['clojure.core/cond] 0]))]
;     (if (== (second cond-details) 100)
;       (-> cond-details
;           first
;           seq)
;       (throw (->ex nil
;               "cond-percent requires percent clauses sum to 100%"))))))

; #?(:clj
; (defmacro cond-percent
;   "Similar to |cond|, but for each condition takes the percentage chance
;    the form should be executed and returned.

;    Ex. (cond-percent
;          50 \"50% Chance\"
;          40 \"40% Chance\"
;          10 (str 10 \"% Chance\")

;    NOTE: all conditions must sum to 100%"
;    {:attribution "thebusby.bagotricks"}
;   [& clauses]
;   `(let [random-percent# (* (core/rand) 100)]
;      (cond-percent* random-percent# ~@clauses))))