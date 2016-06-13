(ns quantum.ir.classify
  (:refer-clojure :exclude [reduce for])
  (:require
    [clojure.core :as core]
    [quantum.core.collections :as coll
      :refer [#?@(:clj [for for* lfor reduce join pjoin kmap in?])
              map+ vals+ filter+ remove+ take+ map-vals+ filter-vals+
              flatten-1+ range+ ffilter
              reduce-count]
      #?@(:cljs [:refer-macros [for lfor reduce join kmap in?]])]
    [quantum.core.numeric :as num]
    [quantum.numeric.core
      :refer [find-max-by]]
    [quantum.numeric.vectors :as v]
    [quantum.core.fn :as fn
      :refer [<- fn-> fn->>]]
    [quantum.core.cache
      :refer [#?(:clj defmemoized)]
      #?@(:cljs [:refer-macros  [defmemoized]])]
    [quantum.core.error
      :refer [->ex]]
    [quantum.core.logic
      :refer [coll-or nnil? #?@(:clj [condpc fn-and])]
      #?@(:clj [:refer-macros [condpc fn-and]])]
    [quantum.core.nondeterministic   :as rand]
    [quantum.numeric.core
      :refer [∏ ∑ sum]]))

(defn boolean-value [x] (if x 1 0)) ; TODO move

(defonce D-0 @alexandergunnarson.cs453.routes/D-0)
(def ^:dynamic *corpus*)
(defn corpus []
  (assert (nnil? *corpus*))
  *corpus*
  #_alexandergunnarson.cs453.routes/D-0)

(def doc->terms+
  (fn->> :document:words 
         (remove+ (fn-> :word:indexed:word :word:stopword?))
         (map+    (fn-> :word:indexed:word :word:text     ))))

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
  ([D c] ((Nt:c D) c)))

(defmemoized Nt:w+d {}
  "word @w :: Number of occurrences of @w in document @d"
  {:performance "O(n^2)"}
  ([d] (->> d doc->terms+
              (join [])
              frequencies))
  ([w d] ((Nt:w+d d) w)))

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

(defmacro P* [t D & args] ; TODO code pattern
  (assert (-> args count even?))
  (let [partitioned (->> args
                         (partition-all 2))
        pred-syms   (->> partitioned (map+ first ) (join []))
        pred-vals   (->> partitioned (map+ second) (join []))]
    (condp = pred-syms
      '[c]     `(P:c    ~t ~D ~@pred-vals)
      '[w]     `(P:w    ~t ~D ~@pred-vals)
      '[ŵ]     `(P:w    ~t ~D ~@pred-vals nil)
      '[w  c ] `(P:w|c  ~t ~D ~@pred-vals)
      '[c  w ] `(P:c|w  ~t ~D ~@pred-vals nil)
      '[c  ŵ ] `(P:c|w  ~t ~D ~@pred-vals nil)
      '[c  d'] `(P:c|d' ~t ~D ~@pred-vals)
      '[d' c ] `(P:d'|c ~t ~D ~@pred-vals)
      (throw (->ex nil "No matching predicate pairs:" pred-syms)))))

(defmacro P
  "The probability of something.
   This is specific to whatever it's talking about"
  [t D & args] ; TODO code pattern
  `(P* ~t ~D ~@(apply concat (for [arg args]
                           [arg arg]))))

(defmemoized P:c {}
  "The probability of observing class c"
  [t D c] (doto (/ (doto (Nd:c D c) #_(println "N c")) (N D))
                #_(println "P c")))

(defmemoized P:w {}
  ([t D w]   (doto (/ (doto (Nd:w D w) #_(println "* N w")) (N D))
                   #_(println "P w")))
  ([t D ŵ _] (doto (/ (doto (Nd:w D ŵ) #_(println "* N ŵ")) (N D))
                   #_(println "P ŵ"))))

(defmemoized P:c|w {}
  ([t D c w]   (doto (/ (doto (Nd:w D c w) #_(println "N c w" c w)) (doto (Nd:w D w) #_(println "N w")))
                   #_(println "P w|c")))
  ([t D c ŵ _] (doto (/ (doto (Nd:w D c ŵ) #_(println "N c ŵ" c ŵ)) (doto (Nd:w D ŵ) #_(println "N ŵ")))
                   #_(println "P ŵ|c"))))

(defn laplacian-smoothed-estimate
  [t D w c]
  (condp = t
    :multinomial (/ (+ (Nd:c+w D w c) 1)
                    (+ (Nt:c D c) (count (V D))))
    :bernoulli   (/ (+ (Nd:c+w D w c) 1)
                    (+ (Nd:c D c)   1))))

(defmemoized P:w|c {}
  ([t D w c]
    (condpc = t
      (coll-or :multinomial :bernoulli)
        (laplacian-smoothed-estimate t D w c)
      #_:collection-smoothed ; alternate for :bernoulli
        #_(/ (+ (Nd:c+w D w c) (* µ (/ (Nd:w D w) (N))))
             (+ (Nd:c D c) µ)))))

(defmemoized delta {}
  "delta(w, d) = 1 iff term w occurs in d, 0 otherwise"
  ; TODO inefficient — should index
  [w d]
  (->> d
       (ffilter #(= % w))
       boolean-value))

(defmemoized P:d'|c {}
  "The probability that document @d is observed, given that the class is known to be @c."
  ([t D d' c]
    (condp = t
      :multinomial (∏ (V D) (fn [w] (num/exp (double (P t D w c)) ; TODO extend num/exp to non-doubles
                                      (Nt:w+d w d'))))
      :bernoulli   (∏ (V D) (fn [w] (* (num/exp (double (P t D w c))
                                         (delta w d'))
                                       (num/exp (double (- 1 (P t D w c)))
                                         (- 1 (delta w d')))))))))

(defmemoized P:c|d' {}
  ([t D c d' & denom?]
    (condpc = t
      (coll-or :bernoulli :multinomial)
      (/ (* (P t D d' c) (P t D c))
         (if denom?
             (∑ (C D)
                (fn [c] (* (P t D d' c) (P t D c))))
             1))))) ; Because can be same denominator

(defn classifier-score+
  [t D d']
  (->> (C D) (map+ (fn [c] [c (P t D c d')]))))

(defn max-classifier-score
  "@D : set of training documents
   @t : the type of probability, in #{:multinomial :bernoulli}
   @d': test document"
  [t D d']
  (->> (classifier-score+ t D d')
       (reduce (partial find-max-by second) [nil 0])))

(defn multinomial-naive-bayes-classifier
  [D d']
  (max-classifier-score :multinomial D d'))

(defn multiple-bernoulli-naive-bayes-classifier
  [D d']
  (max-classifier-score :bernoulli D d'))

; ================================================


(defmemoized information-gain {}
  "@w : a term in the vocabulary.
   @C : the set of distinct natural classes in DC"
  [t D w C]
  (let [[ŵ] [w]
        self*log2 (fn [x] #_(println "x" x) (if (= x 0) 0 (* x (num/exactly (num/log 2 x)))))] ; (num/log 2 0) => -infinity
    (+ (- (∑ C (fn [c] (self*log2 (P t D c) ))))
       (* (P t D w)
          (∑ C (fn [c] (self*log2 (P t D c w)))))
       (* (P t D ŵ)
          (∑ C (fn [c] (self*log2 (P t D c ŵ))))))))

#_(information-gain (corpus) :prone (C))

(defmemoized all-information-gains {}
  ([t D]
  (let [w-to-ig (->> (V D)
                     (map+ (juxt identity #(information-gain D % (C))))
                     (pjoin {}))
        sorted (->> w-to-ig
                    (sort-by val))]
    {:w=>ig  w-to-ig
     :sorted sorted})))

#_(go (time (do (all-information-gains) (println "DONE"))))

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
        (->> (all-information-gains T)
             :sorted
             (filter+ (fn-> first (in? V)))
             (take+   M)
             (join #{})))))

(defn label
  "Assigns the most probable class for a particular document in |test-set|.
   @d : A document"
  {:out-doc "the class @c that should be assigned to @d"}
  [d]
  (compute-word-probability)
  #_argmax ;p(d|c)p(c)
  )

; ======== MNB-PROBABILITY ======== ;
; Used for training an MNB

(defmemoized compute-word-probability {}
  "Computes the probability of each distinct word in each natural class in C
   using training set @D."
  {:performance "4.373 sec on 16 cores"
   :out-doc "Includes for each word in the vocabulary its probability for each class in C."}
  [D]
  (let [C (C D)]
    (->> (V D)
         (map+ (fn [w] [w (for* {} [c C]
                            [c (P :multinomial D w c)])]))  ; uses Laplacian method here
         (pjoin {}))))

(defmemoized compute-class-probability {}
  "Computes the probability of each natural class in C using training set @D."
  {:out-doc "Includes the probability of each class in C."}
  [D]
  (for* {} [c (C D)]
    [c (P :multinomial D c)]))

(defn word-probability
  "Retrieves the probability value of a word in a particular class, which includes the
   probability value of each word not seen during the training phase of MNB.

   @w : a word
   @c : a class"
  {:out-doc "The probability of @w in @c"}
  [D w c]
  (or (get-in (compute-word-probability D) [w c]) 0))

(defn class-probability
  "Retrieves the probability value of a natural class.
   @c : a class"
  {:out-doc "The probability of @c"}
  [D c]
  (or (get (compute-class-probability D) c) 0))

; ======== MNB EVALUATION ========

(defn accuracy-measure
  "Computes the accuracy of the trained MNB.
   Accuracy is defined as the proportion of documents in test set for which
   their classification labels determined using the trained MNB are the same
   as their pre-defined, i.e., original, labels over the total number of
   documents in test set.

   @D : the set of documents in test set and their labels determined by using
        the method |label| in MNB-classification"
  {:out-doc "The classification accuracy of the documents in test set"}
  [D]
  
  )

(defmacro test* [& body]
  `(binding [~'quantum.ir.classify/*corpus* (alexandergunnarson.cs453.classify/corpus)]
    ~@body))

; TODO memoization problem: If DC is called while corpus is running, it doesn't realize that corpus has already started...

(defn D' [D] ; D-split
  (rand/split D [0.2 :test] [0.8 :training])) ; 45.968 ms (kind of a lot in the scheme of things)


(defn run-test []
  (let [{:keys [training test]} (D' (corpus))]
    (binding [*corpus* training]
      (feature-selection training) ; then word prob, then class prob
      (compute-class-probability training)
      (compute-word-probability  training)
  
      (test)
      )))

; Now:
; • Use the MNB model and the 5-fold cross validation approach to determine
;   the classification accuracy and the training and test time based on a
;   set of 10,000 documents in the 20NG dataset, without applying feature selection.
; • Use the trained MNB and the 10,000 documents in 20NG to determine the
;   effects of its accuracy, as well as its training and test time, when
;   considering different vocabulary sizes. In accomplishing this task, use
;   the 5-fold cross validation approach and for each one of the four
;   vocabulary size values M (∈ { 6200, 12400, 18600, 24800 })
;   (i) perform feature selection
;   (ii) determine the classification accuracy
;   (iii) determine the training and test time.
; In the 5-fold cross validation approach, each experiment is repeated 5 times.
; That is, each time a different subset of 20NG is used for the training and
; testing purpose. Thereafter, the averaged classification accuracy and the
; training and test time of each iteration should be reported.


; You don’t have to calculate the denominator since it will be the same for all classes.
;
; 2) Handle vocabulary that you did not see on the training.
; Add a term for example “not seen” and use the Laplacian Smoothed Estimate for this term on each class.
;
; 3) Create a small sample training and test set before running on the 10,000 . Use the example on slides 14 chapter 9.


; In verifying that your MNB implementation is working adequately, instead of
; using the subset of 10,000 documents in 20NG, you can assess the classes
; and methods im- plemented for training and testing the MNB model using the
; documents shown in Slide #14 in the Lecture Notes of Chapter 9, which should
; significantly reduce the time spent in debugging.

; Prior to passing off Project 4, you are required to prepare
; - (i) a diagram which shows the accuracy of your MNB classifier with and without
;   applying feature selection based on Information Gain, and
; - (ii) another diagram which shows the training and test time using different
; vocabulary sizes, which should allow the TA to verify the correct implementation
; of your Project.
