(ns
  ^{:doc "Functions centered around non-determinism (randomness)."
    :attribution "alexandergunnarson"}
  quantum.core.nondeterministic
  (:refer-clojure :exclude
    [reduce next for last nth
     int double conj! contains?
     shuffle count map get partition
     byte  char  long
     bytes chars longs])
  (:require
    #?(:clj [loom.gen                  :as g-gen])  ; for now
            [clojure.core              :as core]
            [quantum.core.convert      :as conv]
            [quantum.core.lexical.core :as lex]
            [quantum.core.data.string
              :refer [!str]]
            [quantum.core.collections.core :as ccoll]
            [quantum.core.collections  :as coll
              :refer [for fori, reduce reduce-2
                      join last lasti partition-all+
                      map map+ map-vals+ map-indexed+
                      filter+
                      get, contains? count conj!
                      indices+ kw-map copy slice]]
            [quantum.core.error        :as err
              :refer [>ex-info TODO throw-unless]]
            [quantum.core.macros       :as macros
              :refer [defnt]]
            [quantum.core.type         :as t
              :refer [regex?]]
            [quantum.core.logic        :as logic
              :refer [splice-or condf1 whenc default]]
            [quantum.core.log          :as log]
            [quantum.core.numeric      :as num]
            [quantum.core.fn           :as fn
              :refer [<- rfn fn1]]
            [quantum.core.data.array   :as arr   ]
            [quantum.core.vars
              :refer [defalias defaliases]]
            [quantum.untyped.core.nondeterministic :as u])
  (:import
    #?@(:clj  [[java.util Random Collections Collection ArrayList]
               java.security.SecureRandom
               smile.math.random.UniversalGenerator
               java.nio.ByteBuffer
               [org.apache.commons.codec.binary Base64 Base32 Hex]]
        :cljs [goog.string.StringBuffer])))

(log/this-ns)

; TO EXPLORE
; - java.util.Random
; TODO note that rand stuff in here is based on a uniform generator; sometimes one wants generators with a particular distribution
; =========================

; Affects KeyGenerator, KeyPairGenerator, KeyAgreement, and Signature.
; http://java-performance.com
; Do not share an instance of java.util.Random between several threads in any circumstances, wrap it in ThreadLocal instead.
; From Java 7 prefer java.util.concurrent.ThreadLocalRandom to java.util.Random in all
; circumstances - it is backwards compatible with existing code, but uses cheaper
; operations internally.
(defaliases u secure-random-generator get-generator)

#?(:cljs
(defn prime
  "Yields a random prime."
  [callback & web-workers?]
  (js/forge.prime.generateProbablePrime 1024
    #js {:algorithm "PRIMEINC"
         ; -1 means auto-optimiing
         :workers (if web-workers? -1 0)}
    (fn [err n] (callback (.toString n 16))))))

#?(:clj
(defn buffer
  "Yields a random buffer."
  [^long n]
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
             sleep-time  (* 10 (+ 50 (int 5000)))
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

(defn int-between
  "Yields a random int between a and b."
  ([        a b] (int-between false a b))
  ([secure? a b]
    #?(:clj (let [generator (get-generator secure?)]
              (+ a (.nextInt generator (inc (- b a)))))
       :cljs (if secure?
                 (TODO) ; "CLJS does not yet support secure random numbers"
                 (+ a (core/rand-int (inc (- b a))))))))

(defn int
  "Yields a random int from 0 to b."
  ([        b] (int false b))
  ([secure? b] (int-between secure? 0 b)))

(defn double-between
  "Yields a random double between a and b."
  ([        a b] (double-between false a b))
  ([secure? a b]
    #?(:clj (let [generator (get-generator secure?)]
              (+ a (* (.nextDouble generator) (- b a))))
       :cljs (if secure?
                 (TODO) ; "CLJS does not yet support secure random numbers"
                 (+ a (core/rand (inc (- b a))))))))

(defn double
  "Yields a random double from 0 to b."
  ([        b] (double false b))
  ([secure? b] (double-between secure? 0 b)))

(defn bits
  "Returns up to 32 random bits."
  [g] (TODO)
  ; return randInt(g) >>> (32 - numbits);
  )

#_(defn int
  [g] (TODO)
  ; return (int) Math.floor(Integer.MAX_VALUE * (2 * nextDouble() - 1.0));
  )

#_(defn long
  [g] (TODO)
  ; return (long) Math.floor(Long.MAX_VALUE * (2 * nextDouble() - 1.0));
  )

(defn gaussian
  [g] (TODO)
  ; use java.util.Random's impl
  )

(defn char-kind->range [kind]
  (case kind
    :numeric [48 57   ]
    :upper   [65 90   ]
    :lower   [97 122  ]
    :any     [0  65535]))

(defn char-between
  "Yields a random char between a and b."
  ([        a b] (char-between false a b))
  ([secure? a b] (core/char (int-between secure? a b))))

(defn char
  ([] (char :any))
  ([kind] (char kind false))
  ([kind secure?]
    (let [[a b] (char-kind->range kind)]
      (char-between secure? a b))))

(defn ^String chars-between
  ([n a b] (chars-between false n a b))
  ([secure? n a b]
    (let [sb (!str)]
      (dotimes [m n] (conj! sb (char-between secure? a b)))
      (str sb))))

; TODO multiple types, as in regex
; TODO |chars| where you can "harden" to a string or not. Also lazy version

(defn chars
  "Returns a random string that matches the regular expression."
  {:inspiration-from "weavejester/re-rand"}
  ([x]
    (assert (regex? x))
    (let [[generator not-matched] (lex/pattern (str x))]
      (when (empty? not-matched)
        (lex/first-if-single (generator)))))
  ([kind n] (chars kind false n))
  ([kind secure? n]
    (let [[a b] (char-kind->range kind)]
      (chars-between secure? n a b))))

(defn bytes ; [B for CLJ, Uint8Array for CLJS
  ([size] (bytes false size))
  ([secure? size]
    #?(:clj  (let [^Random generator (get-generator secure?)
                   bytes-f (byte-array size)
                   _    (.nextBytes generator bytes-f)]
               bytes-f)
       :cljs (if secure?
                 (-> (js/forge.random.getBytesSync size)
                     js/forge.util.binary.raw.decode
                     ccoll/->byte-array)
                 (throw (>ex-info :illegal-argument "Insecure random generator not supported."))))))

#?(:clj
(defn longs
  {:todo ["Base off of random longs, for speed"
          "Lazy |repeatedly| version"]}
  ([size] (longs false size))
  ([secure? size]
    ; * 8 because longs are 8 bytes
    (conv/bytes->longs (bytes secure? (* 8 size))))))

; TODO implement
; (defn rand-vec [...] ...)

; TODO CLJS
#?(:clj
(defn ^String string
  {:todo ["Performance of `rand/string` vs. `(String. rand/bytes)`"]}
  ([n] (string n nil))
  ([n opts]
    (if (or (nil? opts)
            (and (map? opts)
                 (-> opts keys count (= 1))
                 (-> opts keys first (= :secure?))))
        (chars-between (:secure? opts) n
          (core/int Character/MIN_VALUE)
          (core/int Character/MAX_VALUE))
        (let [opts-indexed (zipmap (coll/lrange) opts)
              sb (StringBuilder.)]
          (dotimes [i n]
            (let [generator-k (get opts-indexed
                                (int-between (:secure? opts) 0 (-> opts count dec)))]
              (.append sb (char generator-k (default (:secure? opts) false)))))
          (str sb))))))

#?(:clj
(defn graph
  "Creates a random graph."
  [type & args]
  (condp = type
    :probability (apply g-gen/gen-rand-p args)
    :default     (apply g-gen/gen-rand   args)
    (apply g-gen/gen-rand args))))

; OTHER MORE COMPLEX FUNCTIONS

(defnt nth
  "Return a random element of a sequence."
  [#{array? !vector? default} xs]
  (coll/nth xs (core/long (int-between 0 (lasti xs)))))

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
    (>ex-info "Cond takes an even number of forms"))
  (let [curr-p-sym   (gensym "curr-p")
        p-f-sym      (gensym "p-f") ; actual probability
        partitioned  (core/partition 2 clauses)
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
         (>ex-info "Percent-probabilities must sum to 100."))
       (cond ~@(apply concat
                 (for [[p-sym form] clauses-f]
                  `[(let [lower# (deref ~curr-p-sym)
                          upper# (+ lower# ~p-sym)
                          test# (<= lower# ~p-f-sym upper#)]
                      (vreset! ~curr-p-sym upper#)
                      test#)
                    ~form])))))))

(defn prob
  "Probabilistically returns an element according to the probability-element pairs
   (pairs may compose anything reducible) given.
   The sum of the probabilities must be 1."
  {:example `{(prob [[(fn' :red  ) 0.3]
                     [(fn' :blue ) 0.2]
                     [(fn' :green) 0.5]])
              :red}} ; or :blue, or :green, depending
  ([<x+p>•] (prob <x+p>• false))
  ([<x+p>• secure?]
    (throw-unless (->> <x+p>• (map+ second) (reduce + 0) (== 1))
      (>ex-info "Probabilities must sum to 1." {:arg <x+p>•}))
    (let [p-f (double-between secure? 0 1)] ; the range of possible probabilities
      (reduce
        (rfn [p-accum x p]
          (let [lower p-accum
                upper (+ p-accum p)]
          (if (<= lower p-f upper)
              (reduced x)
              upper)))
        0
        <x+p>•))))

#?(:clj
(defnt shuffle!
  "Shuffles a collection in place."
  {:todo #{"This will work for any mutable list — allow this"
           "CLJS"}}
  ([#{array? !array-list?} x]
    (shuffle! x (get-generator false)))
  ([^array?        x ^Random r]
    (loop [i (count x)]
      (if (> i 1)
          (do (arr/swap-at! x (dec i) (.nextInt r i))
              (recur (dec i)))
          x)))
  ([#{!array-list?} x ^Random r] (doto x (Collections/shuffle r)))))

(defnt shuffle
  "Shuffles a copy of a collection."
  {:todo {0 "Allow all nd-arrays to be copied, thus enabling shuffling"}}
           ([#{array-1d? #?(:clj Collection :cljs +vector?)} xs]
             #?(:clj  (shuffle xs (get-generator false))
                :cljs (core/shuffle xs)))
  #?(:clj  ([^Collection xs ^Random r]
             (-> xs (ArrayList.) (shuffle! r)
                 (.toArray) clojure.lang.RT/vector))
     :cljs ([+vector?       xs r] (TODO)))
           ([^array-1d?   xs #?(:clj #{Random}) r] ; TODO 0
             #?(:clj  (-> xs copy (shuffle! r))
                :cljs (TODO))))

(defn partition
  "Randomly partitions up a collection @coll according to the distributions @distrs-0 given,
   using the supplied predicate @pred."
  {:usage `{(partition [0.5 0.1 0.2] [0 1 2 3 4 5 6 7])
            [[2 7 6 1 5] [0] [4 3]]}}
  ([distributions xs] (partition distributions (get-generator false) xs))
  ([distributions rand-src xs]
    (assert (t/sequential? distributions) (kw-map distributions))
    (let [shuffled    (shuffle xs rand-src)
          allocations (coll/allocate-by-percentages (count shuffled) distributions)]
      (->> (reduce
             (fn [[xs' i] n]
               [(conj! xs' (slice shuffled i (+ i n)))
                (+ i n)])
             [(transient []) 0]
             allocations)
           first persistent!))))

(defn partition-2 [ps xs0 xs1] ; TODO `partition-n`
  (let [parted (->> (persistent! (reduce-2 #(conj! %1 [%2 %3]) (transient []) xs0 xs1)) ; TODO code pattern ; kind of like `v-op+`
                    (partition ps))]
    (->> parted
         (map (juxt (map (fn1 get 0))
                    (map (fn1 get 1)))))))

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
                                      (set-seed! [this seed] (.setSeed g (core/long seed)))))
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

(defn random-sample+
  "Samples items from a reducible according to the random generator passed, to for instance
   provide sampling according to a particular distribution. Defaults to complete randomness."
  ([prob]        (filter+ (fn [_] (< (double) prob))))
  ([prob     xs] (filter+ (fn [_] (< (double) prob)) xs))
  ([prob gen xs] (TODO)))

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
; }
