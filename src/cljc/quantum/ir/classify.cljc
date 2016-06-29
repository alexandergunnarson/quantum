(ns quantum.ir.classify
           (:refer-clojure :exclude [reduce for])
           (:require
             [#?(:clj  clojure.core
                 :cljs cljs.core   )          :as core   ]
             [quantum.core.collections :as coll
               :refer [#?@(:clj [for for* fori lfor reduce join pjoin kmap in?])
                       map+ vals+ filter+ remove+ take+ map-vals+ filter-vals+
                       flatten-1+ range+ ffilter
                       reduce-count]
               #?@(:cljs [:refer-macros [for lfor reduce join kmap in?]])]
             [quantum.core.numeric :as num]
             [quantum.numeric.core
               :refer [find-max-by]]
             [quantum.numeric.vectors :as v]
             [quantum.core.fn :as fn
               :refer        [#?@(:clj [<- fn-> fn->>])]
               #?@(:cljs [:refer-macros [<- fn-> fn->>]])]
             [quantum.core.cache :as cache
               :refer [#?(:clj defmemoized)]
               #?@(:cljs [:refer-macros  [defmemoized]])]
             [quantum.core.error
               :refer [->ex]]
             [quantum.core.logic
               :refer [nnil? #?@(:clj [coll-or condpc fn-and])]
               #?@(:clj [:refer-macros [coll-or condpc fn-and]])]
             [quantum.core.nondeterministic   :as rand]
             [quantum.core.thread             :as thread
                          :refer        [#?(:clj async)]
               #?@(:cljs [:refer-macros [async]])]
             [quantum.numeric.core            :as num*
               :refer [∏ ∑ sum]]
     #?(:clj [taoensso.timbre.profiling :as prof
               :refer [profile defnp p]])))

(defn boolean-value [x] (if x 1 0)) ; TODO move

(def ^:dynamic *exact?* true)

(def doc->terms+
  (fn->> :document:words 
         (remove+ (fn-> :word:indexed:word :word:stopword?))
         (map+    (fn-> :word:indexed:word :word:stem:porter #_:word:text     ))
         (filter+ nnil?)))

(defmemoized C {}
  "All the classes in doc collection @D."
  {:performance "O(n)"
   :query `[:find  ?c
            :where [_ :document:class ?c]]}
  ([D] (->> D
            (map+ :document:class)
            (join #{}))))

(defn V+ [D]
  (->> D
       (filter+ :document:words) ; is nil for a document
       (map+    (fn->> doc->terms+ (join #{})))))

; cached query
(defmemoized V {}
  "All the distinct non-stopwords of a corpus @C."
  {:performance "O(n^2)"
   :query `[:find ?w ; quicker to cache the whole corpus and manipulate it than to not
            :where [?doc   :document:corpus-id :ng20 ]
                   [?doc   :document:words     ?words]
                   [?words :word:indexed:word  ?word ] ; 2,870,488 of these
                   [?word  :word:text          ?w    ]]}
  ([D] (->> D V+ (reduce #(join %1 %2) #{}))))

; =================================================
; ----------------------- N -----------------------
; =================================================

(defmacro N*
  ([D] `(N- ~D))
  ([D arg & args] ; TODO code pattern
    (assert (-> (cons arg args) count even?))
    (let [partitioned (->> (cons arg args)
                           (partition-all 2))
          pred-syms   (->> partitioned (map+ first ) (join []))
          pred-vals   (->> partitioned (map+ second) (join []))]
      (condp = pred-syms
        '[c]   `(N:c   ~D ~@pred-vals)
        '[w]   `(N:w   ~D ~@pred-vals)
        '[ŵ]   `(N:w   ~D ~@pred-vals nil)
        (throw (->ex nil "No matching predicate pairs:" pred-syms))))))

(defmacro N [D & args] ; TODO code pattern
  `(N* ~D ~@(apply concat (for [arg args]
                           [arg arg]))))

(defmemoized N- {}
  (^{:doc "The total number of documents in doc collection @D."}
    [D] (count D)))

; cached query
(defmemoized Nd:c {}
  "class @c :: the number of documents in doc collection @D labeled as @c"
  ([D] (->> D
            (map+ :document:class)
            (join [])
            frequencies))
  (^{:doc "the number of documents in doc collection @D labeled as @c"}
   [D c] (or ((Nd:c D) c)
             (throw (->ex nil "Class does not exist in doc collection" c)))))

(defmemoized Nt:c {}
  "Total number of occurrences of words in doc collection @D of class @c."
  {:performance "O(n^2)"}
  ([D] (->> D 
            (filter+ (fn-and :document:class :document:words))
            (map+ (juxt :document:class (fn-> doc->terms+ reduce-count)))
            (map+ vector)
            (reduce (partial merge-with +) {})))
  ([D c] (or ((Nt:c D) c)
             (throw (->ex nil "Class does not exist in doc collection" c)))))

(defmemoized Nt:w+d {}
  "word @w :: Number of occurrences of @w in document @d"
  {:performance "O(n^2)"}
  ([d] (->> d doc->terms+
              (join [])
              frequencies))
  ([w d] (or ((Nt:w+d d) w) 0)))

; cached query
(defmemoized Nd:w {}
  "word @w :: the number of documents in corpus @C in which @w occurs"
  ([D] (->> D
            V+
            (map+ #(zipmap % (repeat 1)))
            (join [])
            (reduce (partial merge-with +) {})))
  (^{:doc "The number of documents in corpus @C in which @w occurs"}
   [D w] (or ((Nd:w D) w)
             (throw (->ex nil "Word does not exist" w))))
  (^{:doc "The number of documents in corpus @C in which @w does not occur"}
   [D ŵ _] (- (N D) (Nd:w D ŵ))))

; cached query
(defmemoized Nd:c+w {}
  "class @c :: word @w :: The number of documents in corpus @D
                          labeled as @c, in which @w occurs"
  {:out-like `{:c1 {:ct    4
                    :words {:w1 1
                            :w2 3}}
               :c2 ...}}
  ([D] (->> D 
            (filter+ (fn-and :document:class :document:words)) ; in case is nil
            (map+ (juxt :document:class (fn->> Nt:w+d (map-vals+ (constantly 1)) (join {}))))
            (map+ vector)
            (reduce (fn [ret elem]
                      (merge-with
                        (partial merge-with +)  ret elem))
                    {})))
  (^{:doc "The number of documents in corpus @D labeled as @c, in which @w occurs"}
   [D c w] (or (get-in (Nd:c+w D) [c w]) 0))
  (^{:doc "The number of documents in corpus @D labeled as @c, in which @w does not occur"}
   [D c ŵ _] (- ((Nd:c D) c) (Nd:c+w D c ŵ))))

(defmemoized N:w+c {}
  "class @c :: word @w :: The number of occurrences of @w in @c"
  ([D] (->> D
            (filter+ (fn-and :document:class :document:words)) ; in case is nil
            (map+ (juxt :document:class Nt:w+d))
            (reduce (partial merge-with
                      (partial merge-with +)))))
  ([D w c] (or (get-in (N:w+c D) [c w] 0))))

; =================================================
; ------------------ PROBABILITY ------------------
; =================================================

(defmemoized P:c {}
  "The probability of observing class @c"
  #_([t D c] (P:c t D c (V D)))
  ([t D c V] (/ (Nd:c D c) (N D))))

(defmemoized P:w {}
  "The probability of observing word @w"
  ([t D w]   (/ (Nd:w D w) (N D)))
  ([t D ŵ _] (/ (Nd:w D ŵ nil) (N D))))

(defmemoized P:c|w {}
  "The probability of a word @w being of class @c."
  ([t D c w]   (/ (Nd:c+w D c w) (Nd:w D w)))
  ([t D c ŵ _] (/ (Nd:c+w D c ŵ nil) (Nd:w D ŵ nil))))

(defn laplacian-smoothed-estimate
  [t D w c V]
  (condp = t
    :multinomial (/ (+ (p :Ndcw (Nd:c+w D c w)) 1)
                    (+ (p :Ntc (Nt:c D c)) (count V)))
    :bernoulli   (/ (+ (Nd:c+w D c w) 1)
                    (+ (Nd:c D c)   1))))

(defmemoized P:w|c {}
  "The probability of class @c containing word @w."
  ([t D w c V]
    (laplacian-smoothed-estimate t D w c V) ; Weird memoization problem here! TODO FIX
    #_(condp = t
      (coll-or :multinomial :bernoulli)
        (laplacian-smoothed-estimate t D w c V)

      #_:collection-smoothed ; alternate for :bernoulli
        #_(/ (+ (Nd:c+w D c w) (* µ (/ (Nd:w D w) (N))))
             (+ (Nd:c D c) µ)))))

(def get-word-probability P:w|c)

(def P:w|c laplacian-smoothed-estimate)

(defmemoized delta {}
  "delta(w, d) = 1 iff term w occurs in d, 0 otherwise"
  ; TODO inefficient — should index
  [w d]
  (->> d
       (ffilter #(= % w))
       boolean-value))

(defn expi
  "Exponent to the integer power" ; TODO move
  [x i]
  (cond (> i 1) (reduce (fn [ret _] (* ret x)) x (dec i))
        (= i 1) x
        (= i 0) 1
        (< i 0) (throw (->ex "Not handled"))))

(defmemoized P:d'|c {}
  "The probability that document @d is observed, given that the class is known to be @c."
  #_([t D d' c] (P:d'|c t D d' c (V D)))
  ([t D d' c V]
    (condp = t
      :multinomial (if *exact?*
                       (∏ V (fn [w] (expi (P:w|c t D w c V) 
                                          (Nt:w+d w d'))))
                       #_(->> (V D) ; somehow doesn't work :((
                            (map+ (fn [w] (num/log 2 (double (expi (P:w|c t D w c V)
                                                               (Nt:w+d w d'))))))
                            (reduce #(log %1 %2) 0.0))
                       (∏ V (fn [w] (with-precision 10 (bigdec (expi (P:w|c t D w c V) ; 99%
                                                                     (Nt:w+d w d')
                                                                     ))))))         ; Addition and such: 43%!
      :bernoulli   (∏ V (fn [w] (* (expi (P:w|c t D w c V) ; TODO use |exp'|  ; TODO extend num/exp to non-doubles
                                         (delta w d'))
                                       (expi (- 1 (P:w|c t D w c V))
                                         (- 1 (delta w d')))))))))

(defmemoized P:c|d' {}
  "The probability that given document @d, it should be classified as class @c."
  #_([t D c d'] (P:c|d' t D c d' (V D)))
  ([t D c d' V] (P:c|d' t D c d' V false))
  ([t D c d' V denom?]
    (condpc = t
      (coll-or :bernoulli :multinomial)
      (let [Pc  (P:c t D c V)
            Pc' (if *exact?*
                    Pc
                    (with-precision 10 (bigdec Pc)))]
        (/ (* (P:d'|c t D d' c V) Pc')
           (if denom?
               (∑ (C D)
                  (fn [c] (* (P:d'|c t D d' c V) (P:c t D c V))))
               1)))))) ; Because are all same denominator

(defn classifier-score+
  [t D d' V]
  (->> (C D) (map+ (fn [c] [c (P:c|d' t D c d' V)]))))

(defn classifier-scores
  [t D d' V]
  (->> (classifier-score+ t D d' V)
       (join {})))

(defmemoized max-classifier-score {}
  "@D : set of training documents
   @t : the type of probability, in #{:multinomial :bernoulli}
   @d': test document"
  ([t D d'] (max-classifier-score t D d' (V D)))
  ([t D d' V]
    (->> (classifier-score+ t D d' V)
         (reduce (partial find-max-by second) [nil 0]))))

(defn multinomial-naive-bayes-classifier
  ([D d'] (multinomial-naive-bayes-classifier D d' (V D)))
  ([D d' V] (max-classifier-score :multinomial D d' V)))

(defn multiple-bernoulli-naive-bayes-classifier
  ([D d'] (multiple-bernoulli-naive-bayes-classifier D d' (V D)))
  ([D d' V] (max-classifier-score :bernoulli D d' V)))

; ================================================


(defmemoized information-gain {} ; 9 seconds
  "The information gain of a vocabulary word @w.
   @w : a term in the vocabulary.
   @C : the set of distinct natural classes in DC"
  [t D w C V]
  (let [[ŵ] [w]
        self*log2 (fn [x] (if (= x 0) 0 (* x (identity #_num/exactly (num/log 2 x)))))] ; (num/log 2 0) => -infinity
    (+ (- (∑ C (fn [c] (self*log2 (P:c t D c V) ))))
       (* (P:w t D w)
          (∑ C (fn [c] (self*log2 (P:c|w t D c w)))))
       (* (P:w t D ŵ nil)
          (∑ C (fn [c] (self*log2 (P:c|w t D c ŵ nil))))))))

(defmemoized all-information-gains {}
  "All the information gains from document collection @D."
  ([t D V]
    (let [w-to-ig (->> V
                       (map+ (juxt identity #(information-gain t D % (C D) V)))
                       (pjoin {}))
          sorted (->> w-to-ig
                      (sort-by val))]
      {:w=>ig  w-to-ig
       :sorted sorted})))

(defn feature-selection
  "Determines the (sampled) words which should be chosen to represent
   documents in training set and test set based on the information gain (IG)
   of each @w in |V|.

   @T : The DC training set to be used.
   @M : A user-defined value, ≥ 1.
        It denotes the number of words in the vocabulary that should be selected."
  {:out-doc "A data structure with @M words (denoted |selected-features|).
             They are the words in the vocabulary with IG values higher than
             the other words in the DC-training set and are used to represent
             each document in |training-set| and |test-set|."}
  [T M]
  (assert (>= M 1) #{M})
  (let [V (V T)]
    (if (= M (count V))
        V
        (->> (all-information-gains :multinomial T V)
             :sorted
             (filter+ (fn-> first (in? V))) ; make sure your selected ones are in your vocab
             (take+   M)
             (map+ first)
             (join #{})))))



; ======== MNB EVALUATION ========

(defn label
  "Assigns the most probable class for a particular document in |test-set|.
   @D : The set of training document
   @d : A document in |test-set|"
  {:out-doc "the class @c that should be assigned to @d"}
  [D d V']
  (multinomial-naive-bayes-classifier D d V'))

(defmemoized accuracy-measure {}
  "Computes the accuracy of the trained MNB.
   Accuracy is defined as the proportion of documents in test set for which
   their classification labels determined using the trained MNB are the same
   as their pre-defined, i.e., original, labels over the total number of
   documents in test set.

   @D  : the set of documents in training set
   @D' : the set of documents in test set"
  {:out-doc "The classification accuracy of the documents in test set"}
  [D D' V']
  (let [scores (->> D' (filter+ :document:class)
                       (map+    (fn [d] (let [c (:document:class d)
                                              [c' score] (label D d V')]
                                          [c c' score])))
                       #_(coll/notify-progress+ ::accuracy
                         (fn [i x] (str "Document #" i " processed.")))
                       (pjoin []) ; pjoin didn't work here with {} and is weak for []. Why??
                       (group-by #(= (first %) (second %))))]
    {:scores scores
     :accuracy (double (/ (-> scores (get true) count)
                          (count D')))})) 

; TODO memoization problem: If DC is called while corpus is running, it doesn't realize that corpus has already started...

(defn D-split [D]
  (rand/split D [0.2 :test] [0.8 :training])) ; 45.968 ms (kind of a lot in the scheme of things)

