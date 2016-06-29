(ns
  ^{:doc "A few functions copied from thebusby.bagotricks.
          Not especially used at the moment."
    :attribution "Alex Gunnarson"}
  quantum.core.nondeterministic
  (:refer-clojure :exclude [bytes reduce for last nth rand-nth shuffle])
          (:require
            #?(:clj [loom.gen                  :as g-gen])  ; for now
                    [#?(:clj  clojure.core
                        :cljs cljs.core)       :as core  ]
                    [quantum.core.convert      :as conv  ]
                    [quantum.core.lexical.core :as lex   ]
                    [quantum.core.data.set     :as set   
                      :refer [sorted-set+]               ]
                    [quantum.core.collections  :as coll
                      :refer [#?@(:clj [fori reduce for join last lasti nth])
                              map+ map-vals+ map-indexed+ indices+]]
                    [quantum.core.error        :as err
                      :refer [->ex #?(:clj throw-unless)]]
                    [quantum.core.macros       :as macros
                       :refer [#?@(:clj [defnt])]        ]
                    [quantum.core.type         :as type
                      :refer [#?(:clj regex?)]           ]
                    [quantum.core.logic        :as logic
                      :refer [splice-or nempty?
                              #?@(:clj [condf*n])]       ]
                    [quantum.core.numeric      :as num   ]
                    [quantum.core.fn           :as fn
                      :refer [#?(:clj <-)]               ]
                    [quantum.core.data.array   :as arr   ]
                    [quantum.core.vars
                      :refer [#?(:clj defalias)]])
  #?(:cljs (:require-macros
                    [quantum.core.convert      :as conv  ]
                    [quantum.core.collections  :as coll
                      :refer [reduce for join last lasti]]
                    [quantum.core.fn           :as fn
                      :refer [<-]                        ]
                    [quantum.core.logic
                      :refer [condf*n]]
                    [quantum.core.macros       :as macros
                      :refer [defnt]                     ]
                    [quantum.core.type         :as type
                      :refer [regex?]                    ]
                    [quantum.core.vars
                      :refer [defalias]]))
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
(defn rand-int-between ; |rand-int| ?
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

(defn rand-bytes ; [B for CLJ, Uint8Array for CLJS
  ([size] (rand-bytes false size))
  ([secure? size]
    #?(:clj  (let [^Random generator (get-generator secure?)
                   bytes-f (byte-array size)
                   _    (.nextBytes generator bytes-f)]
               bytes-f)
       :cljs (if secure?
                 (-> (js/forge.random.getBytesSync size)
                     js/forge.util.binary.raw.decode
                     arr/->int8-array)
                 (throw (->ex :illegal-argument "Insecure random generator not supported."))))))

#?(:clj
(defn rand-longs
  {:todo ["Base off of random longs, for speed"]}
  ([size] (rand-longs false size))
  ([secure? size]
    ; * 8 because longs are 8 bytes
    (conv/bytes->longs (rand-bytes secure? (* 8 size))))))

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

(defn rand-nth [coll]
  (nth coll (rand-int-between 0 (lasti coll))))

#?(:clj
(defmacro cond-percent
  "Similar to |cond|, but for each condition takes the percentage chance
   the form should be executed and returned.

   Ex. (cond-percent
         40         \"40% Chance\"
         my-percent \"50% Chance\"
         10         (str 10 \"% Chance\")

   NOTE: The conditions' probabilities must sum to 100%."
   {:derivation "thebusby.bagotricks"
    :contributors ["Alex Gunnarson"]}
  [& clauses]
  (throw-unless (-> clauses count even?)
    (->ex "Cond takes an even number of forms"))
  (let [curr-p-sym   (gensym "curr-p")
        p-f-sym      (gensym "p-f") ; actual probability
        partitioned  (partition 2 clauses)
        p-syms       (vec (repeatedly (count partitioned) #(gensym "p")))
        clauses-f    (fori [[_ form] partitioned i]
                       [(get p-syms i) form])
        let-bindings (apply conj [curr-p-sym `(volatile! 0)
                                  p-f-sym    `(-> (rand) (* 100))]
                       (apply concat
                         (fori [[p _] partitioned i]
                           [(get p-syms i) p])))]
    `(let ~let-bindings
       (throw-unless (= 100 (+ ~@p-syms))
         (->ex "Percent-probabilities must sum to 100."))
       (cond ~@(apply concat
                 (for [[p-sym form] clauses-f]
                  `[(let [lower# (deref ~curr-p-sym)
                          upper# (+ lower# ~p-sym)
                          test# (<= lower# ~p-f-sym upper#)]
                      (vreset! ~curr-p-sym upper#)
                      test#)
                    ~form])))))))

(defn prob
  "Probabilistically calls a function according to the probability-function pairs given.
   When |check-sum?| is true, the sum of the probabilities are validated to be 1."
  {:example `{(prob [[0.3 (constantly :red  )]
                     [0.2 (constantly :blue )]
                     [0.5 (constantly :green)]])
              :red}}
  ([ps+fs] (prob ps+fs false))
  ([ps+fs check-sum?]
    (when check-sum?
      (throw-unless (->> ps+fs (map+ first) (reduce + 0) (= 1))
        (->ex "Probabilities must sum to 1.")))
    (let [p-f (rand)] ; TODO |secure?|
      (reduce
        (fn [p-accum [p f]]
          (let [lower p-accum
                upper (+ p-accum p)]
          (if (<= lower p-f upper)
              (reduced (f))
              upper)))
        0
        ps+fs))))

(defalias shuffle core/shuffle)

(defn split
  "Randomly splits up a collection @coll according to the distributions @distrs-0 given,
   using the supplied predicate @pred."
  {:tests `{[[1 2 3 4 5] [0.2 :test] [0.8 :training]]
            {:test [4], :training [3 2 1 5]}
            [[1 2 3 4]   [0.2 :test] [0.8 :training]]
            {:test [2], :training [3 1 4]  }}}
  [coll & distrs-0]
  (assert (nempty? distrs-0))
  (let [coll        (vec coll) ; to be able to index
        to-distr    (condf*n vector? (juxt second first)
                             number? (juxt #(gensym (str "split-" %)) identity))
        distrs      (->> distrs-0
                         (map+ to-distr)
                         (join {}))
        total-p     (->> distrs vals (reduce + 0))
        _ (assert (splice-or total-p = 1 1.0) #{total-p}) ; TODO =
        ; probability-function pairs
        chunk-sizes (coll/allocate-by-percentages (count coll) (vals distrs))
        partitions  (zipmap (keys distrs) chunk-sizes)]
    ; for chunks of size c, chooses random indices to belong to that category
    ; TODO move
    (->> partitions
         (reduce
           (fn [[result remaining-indices] assigned-category chunk-size]
             (reduce
               (fn [[result' remaining-indices'] _]
                 (let [_        (assert (nempty? remaining-indices'))
                       chosen-i (rand-nth remaining-indices')]
                    [(update result' assigned-category conj! (get coll chosen-i))
                     (disj remaining-indices' chosen-i)]))
               [result remaining-indices]
               chunk-size))
           [(zipmap (keys distrs) (repeatedly #(transient [])))  ; to be able to do efficient nth and also disj ; TODO |indices|
            (->> coll indices+ (join (sorted-set+)))])
         first
         (map-vals+ persistent!)
         (join {}))))