(ns
  ^{:doc "A few functions copied from thebusby.bagotricks.
          Not especially used at the moment."
    :attribution "Alex Gunnarson"}
  quantum.core.nondeterministic
  (:refer-clojure :exclude [bytes reduce next for last nth rand-nth rand-int shuffle])
  (:require
    #?(:clj [loom.gen                  :as g-gen])  ; for now
            [clojure.core              :as core]
            [quantum.core.convert      :as conv]
            [quantum.core.lexical.core :as lex]
            [quantum.core.data.set     :as set
              :refer [sorted-set+]]
            [quantum.core.collections  :as coll
              :refer [fori reduce nempty?
                      for join last lasti nth
                      map+ map-vals+ map-indexed+
                      indices+]]
            [quantum.core.error        :as err
              :refer [->ex TODO throw-unless]]
            [quantum.core.macros       :as macros
              :refer [defnt]]
            [quantum.core.type         :as type
              :refer [regex?]]
            [quantum.core.logic        :as logic
              :refer [splice-or condf1]]
            [quantum.core.log          :as log]
            [quantum.core.numeric      :as num]
            [quantum.core.fn           :as fn
              :refer [<-]]
            [quantum.core.data.array   :as arr   ]
            [quantum.core.vars
              :refer [defalias]])
  (:import
    #?@(:clj  [java.util.Random
               java.security.SecureRandom
               smile.math.random.UniversalGenerator
               java.nio.ByteBuffer
               [org.apache.commons.codec.binary Base64 Base32 Hex]]
        :cljs [goog.string.StringBuffer])))

(log/this-ns)

; TO EXPLORE
; - java.util.Random
; =========================

; Affects KeyGenerator, KeyPairGenerator, KeyAgreement, and Signature.
; http://java-performance.com
; Do not share an instance of java.util.Random between several threads in any circumstances, wrap it in ThreadLocal instead.
; From Java 7 prefer java.util.concurrent.ThreadLocalRandom to java.util.Random in all
; circumstances - it is backwards compatible with existing code, but uses cheaper
; operations internally.
#?(:clj (defonce ^SecureRandom secure-random-generator
          (SecureRandom/getInstance "SHA1PRNG")))

(defn get-generator [secure?]
  #?(:clj (if secure?
              secure-random-generator
              (java.util.concurrent.ThreadLocalRandom/current))
     :cljs (TODO)))

#?(:cljs
(defn rand-prime [callback & web-workers?]
  (js/forge.prime.generateProbablePrime 1024
    #js {:algorithm "PRIMEINC"
         ; -1 means auto-optimiing
         :workers (if web-workers? -1 0)}
    (fn [err n] (callback (.toString n 16))))))

#?(:clj
(defn rand-buffer [^long n]
  (let [b (ByteBuffer/allocate n)]
    (.nextBytes secure-random-generator (.array b))
    b)))

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

(defn rand-int-between ; |rand-int| ?
  ([        a b] (rand-int-between false a b))
  ([secure? a b]
    #?(:clj (let [^Random generator (get-generator secure?)]
              (+ a (.nextInt generator (inc (- b a)))))
       :cljs (if secure?
                 (TODO) ; "CLJS does not yet support secure random numbers"
                 (+ a (core/rand-int (inc (- b a))))))))

(defn rand-int
  ([        b] (rand-int false b))
  ([secure? b] (rand-int-between secure? 0 b)))

(defn rand-bits
  "Returns up to 32 random bits."
  [g] (TODO)
  ; return randInt(g) >>> (32 - numbits);
  )

#_(defn rand-int
  [g] (TODO)
  ; return (int) Math.floor(Integer.MAX_VALUE * (2 * nextDouble() - 1.0));
  )

#_(defn rand-long
  [g] (TODO)
  ; return (long) Math.floor(Long.MAX_VALUE * (2 * nextDouble() - 1.0));
  )

(defn rand-gaussian
  [g] (TODO)
  ; use java.util.Random's impl
  )


(defn rand-char-between
  ([        a b] (rand-char-between false a b))
  ([secure? a b] (char (rand-int-between secure? a b))))

(defn ^String rand-chars-between
  ([n a b] (rand-chars-between false n a b))
  ([secure? n a b]
    (let [sb (#?(:clj StringBuilder. :cljs StringBuffer.))]
      (dotimes [m n]
        (.append sb (rand-char-between secure? a b)))
      (str sb))))

#_(def generators
  {:numeric rand-numeric
   :upper   rand-upper
   :lower   rand-lower})

; TODO multiple types, as in regex
; TODO |rand-chars| where you can "harden" to a string or not. Also lazy version

(defn rand-chars
  "Returns a random string that matches the regular expression."
  {:inspiration-from "weavejester/re-rand"}
  ([x]
    (assert (regex? x))
    (let [[generator not-matched] (lex/pattern (str x))]
      (when (empty? not-matched)
        (lex/first-if-single (generator)))))
  ([type n] (rand-chars type false n))
  ([type secure? n]
    (let [[a b]
           (case type
             :numeric [48 57   ]
             :upper   [65 90   ]
             :lower   [97 122  ]
             :any     [0  65535])]
      (rand-chars-between secure? n a b))))

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
                     arr/->byte-array)
                 (throw (->ex :illegal-argument "Insecure random generator not supported."))))))

#?(:clj
(defn rand-longs
  {:todo ["Base off of random longs, for speed"
          "Lazy |repeatedly| version"]}
  ([size] (rand-longs false size))
  ([secure? size]
    ; * 8 because longs are 8 bytes
    (conv/bytes->longs (rand-bytes secure? (* 8 size))))))

; TODO implement
; (defn rand-vec [...] ...)

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

#?(:clj
(defn rand-graph
  "Creates a random graph."
  [type & args]
  (condp = type
    :probability (apply g-gen/gen-rand-p args)
    :default     (apply g-gen/gen-rand   args)
    (apply g-gen/gen-rand args))))

; OTHER MORE COMPLEX FUNCTIONS

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
              :red}} ; or :blue, or :green, depending
  ([ps+fs] (prob ps+fs false false))
  ([ps+fs check-sum? secure?]
    (when secure? (TODO))
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
        to-distr    (condf1 vector? (juxt second first)
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

(defmulti
  ^{:doc "Generates a random object."}
  generate (fn [k & args] k))

; TODO :unique-<object>, :string, :keyword, :symbol, etc.
; TODO generate both valid and invalid types for tests, e.g. according to differences in datatype, length, etc.

; Some options are :lcg (linear congruential generator, Java's default)
; and :twister (Marsenne twister).

; ===== GENERATORS ===== ;

(defn generator:mersenne-twister-32
  {:implemented-by '#{smile.math.random.MersenneTwister
                      org.apache.commons.math3.random.MersenneTwister}}
  [] (TODO))

(defn generator:mersenne-twister-64
  {:implemented-by '#{smile.math.random.MersenneTwister
                      org.apache.commons.math3.random.MersenneTwister64}}
  [] (TODO))

#?(:clj
(defn generator:universal
  "The so called \"Universal Generator\" based on multiplicative congruential
   method, which originally appeared in \"Toward a Universal Random Number
   Generator\" by Marsaglia, Zaman and Tsang."
  {:implemented-by '#{}}
  ([] (UniversalGenerator.))
  ([^long seed] (UniversalGenerator. seed))))

(defprotocol IRandomGenerator
  ; Returns the next pseudorandom, uniformly distributed double value between 0.0 and 1.0
  (next      [this])
  (impl      [this])
  (set-seed! [this seed]))

(defn generator
  "All these generators are from org.apache.commons.math3.random.*"
  [type & [seed :as args]]
  (TODO)
  (case type
    :universal           #?(:clj  (let [^UniversalGenerator g (apply generator:universal args)]
                                    (reify IRandomGenerator
                                      (next      [this]      (.nextDouble g))
                                      (impl      [this]      g)
                                      (set-seed! [this seed] (.setSeed g (long seed)))))
                            :cljs (TODO))
    ; gaussian normalized random generator for scalars.
    :gaussian-normalized nil
    :halton-sequence     nil
    :sobol-sequence      nil
    :stable-normalized   nil
    :normalized-uniform  nil
    ; a fast cryptographic pseudo-random number generator
    ; ISAAC (Indirection, Shift, Accumulate, Add, and Count)
    ; generates 32-bit random numbers
    :isaac               nil
    :jdk        (get-generator false)
    :jdk-secure (get-generator true ) ; TODO more varieties are available
    ; A powerful pseudo-random number generator
    ; developed by Makoto Matsumoto and Takuji Nishimura during 1996-1997.
    ; Has the advantage of having a far longer period and the ability to use a
    ; far larger seed value.
    :mersenne-twister-32 nil
    :mersenne-twister-64 nil
    ; The below are from François Panneton, Pierre L'Ecuyer and Makoto Matsumoto
    :well512a   nil
    :well1024a  nil
    :well19937a nil
    :well19937c nil
    :well44497a nil
    :well44497b nil))


; public class Random {
;     /**
;      * Generates a permutation of 0, 1, 2, ..., n-1, which is useful for
;      * sampling without replacement.
;      */
;     public int[] permutate(int n) {
;         int[] x = new int[n];
;         for (int i = 0; i < n; i++) {
;             x[i] = i;
;         }

;         permutate(x);

;         return x;
;     }

;     public void shuffle(Object[] x) {
;         for (int i = 0; i < x.length; i++) {
;             int j = i + nextInt(x.length - i);
;             Math.swap(x, i, j);
;         }
;     }
; }
