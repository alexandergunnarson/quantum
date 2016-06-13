(ns 
  quantum.ir.classify
  (:refer-clojure :exclude [reduce for])
  (:require
    [clojure.core :as core]
    [quantum.core.collections :as coll
      :refer [#?@(:clj [for lfor reduce join kmap])
              map+ vals+ filter+ filter-vals+ flatten-1+ range+ ffilter
              reduce-count]
      #?@(:cljs [:refer-macros [for lfor reduce join kmap]])]
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
      :refer [coll-or #?@(:clj [condpc])]
      #?@(:clj [:refer-macros [condpc]])]
    [quantum.numeric.core
      :refer [∏ ∑ sum]]))

(defn boolean-value [x] (if x 1 0)) ; TODO move

(defn N
  "Total number of training documents"
  [tdocs]
  (count tdocs))

(defmemoized Nc {}
  "Number of training documents in class c"
  [tdocs c]
  (->> tdocs
       (filter-vals+ (fn-> :class (= c)))
       reduce-count))

(defmemoized tc {}
  "Number of non-unique terms in the training documents of class @c."
  [tdocs c]
  (->> tdocs vals+
       (filter+ (fn-> :class (= c)))
       (map+ (fn-> :words count))
       sum))

(defmemoized C {}
  "All classes in training documents"
  [tdocs]
  (->> tdocs vals+ (map+ :class) (join #{})))

(defmemoized V {}
  "The distinct terms in the training documents"
  [tdocs]
  (->> tdocs vals+
       (map+ :words)
       (reduce #(join %1 %2) #{})))

(defmemoized tf-w+d {}
  "Number of occurrences of @w in document @d"
  [w d]
  (->> d
       (filter+ #(= % w))
       reduce-count))

(defmemoized tf-w+c {}
  "Number of occurrences of @w in class @c"
  [tdocs w c]
  (->> tdocs vals+
       (filter+ (fn-> :class (= c)))
       (map+ :words)
       flatten-1+
       (filter+ #(= % w))
       reduce-count))

(defmemoized df-w+c {}
  "Number of documents in belonging to class @c which include term @w"
  [tdocs w c]
  (->> tdocs vals+
       (filter+ (fn-> :class (= c)))
       (map+ :words)
       (filter+ (fn->> (ffilter #(= % w))))
       reduce-count))

(defmemoized delta {}
  "delta(w, d) = 1 iff term w occurs in d, 0 otherwise"
  [w d]
  (->> d
       (ffilter #(= % w))
       boolean-value))

(defn laplacian-smoothed-estimate
  [D t w c]
  (condp = t
    :multinomial (/ (+ (tf-w+c D w c) 1)
                    (+ (tc D c) (count (V D))))
    :bernoulli   (/ (+ (df-w+c D w c) 1)
                    (+ (Nc D c)       1))))

(defn P ; this is specific to whatever it's talking about
  "@t: the type of probability, in #{:multinomial :bernoulli}"
  ([D t of-sym of]
    (condp = of-sym
      ; The probability of observing class c
      'c (let [c of] (/ (Nc D c) (N D)))))
  ([D t of-sym of given-sym given]
    (condpc = [t of-sym given-sym]
      (coll-or
        '[:bernoulli   c d']
        '[:multinomial c d'])
        (let [c of d' given]
           (/ (* (P D t 'd' d' 'c c) (P D t 'c c))
              (∑ (C D)
                 #(* (P D t 'd' d' 'c %) (P D t 'c %)))))
      ; TODO Also is collection-smoothed estimate
      (coll-or
        '[:multinomial w c]
        '[:bernoulli   w c])
        (let [w of c given]
          (laplacian-smoothed-estimate D t w c))
      ; The probability that document d is observed,
      ; given that the class is known to be c
      '[:multinomial d' c]
         (let [d' of c given]
           (∏ (V D) (fn [w] (num/exp (double (P D t 'w w 'c c)) ; TODO extend num/exp to non-doubles
                                     (tf-w+d w d')))))
      '[:bernoulli d' c]
         (let [d' of c given]
           (∏ (V D) (fn [w] (* (num/exp (double (P D t 'w w 'c c))
                                        (delta w d'))
                               (num/exp (double (- 1 (P D t 'w w 'c c)))
                                        (- 1 (delta w d')))))))
      (throw (->ex nil "No corresponding probability found"
                   (kmap t of-sym of given-sym given))))))

(defn classifier-score+
  [D t d']
  (->> (C D) (map+ (fn [c] [c (P D t 'c c 'd' d')]))))

(defn max-classifier-score
  "@t: the type of probability, in #{:multinomial :bernoulli}
   @D is set of training documents
   @d is test document"
  [D t d']
  (->> (classifier-score+ D t d')
       (reduce (partial find-max-by second) [nil 0])))

(defn multinomial-naive-bayes-classifier
  [D d']
  (max-classifier-score D :multinomial d'))

(defn multiple-bernoulli-naive-bayes-classifier
  [D d']
  (max-classifier-score D :bernoulli d'))

; ================================================