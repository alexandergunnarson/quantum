(ns quantum.test.ir.classify
           (:refer-clojure :exclude [reduce for])
           (:require
             [clojure.core :as core]
             [quantum.core.collections :as coll
               :refer [#?@(:clj [for lfor reduce join kmap])
                       map+ vals+ filter+ filter-vals+ flatten-1+ range+ ffilter
                       reduce-count]
               #?@(:cljs [:refer-macros [for lfor reduce join kmap]])]
             [quantum.core.numeric :as num]
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
               :refer [∏ ∑ sum]]
             [quantum.ir.classify :as this]
             [quantum.core.log :as log])
  #?(:cljs (:require-macros
             [quantum.core.log :as log])))

(log/enable! :test)
(log/pr :test "===== TESTING ======")

(let [training-docs
       {1 {:class :not-japan
           :words ["Taipei" "Taiwan"]}
        2 {:class :not-japan
           :words ["Macao" "Taiwan" "Shanghai"]}
        3 {:class :japan
           :words ["Japan" "Sapporo"]}
        4 {:class :japan
           :words ["Sapporo" "Osako" "Taiwan"]}}
      D training-docs
      test-doc ["Taiwan" "Taiwan" "Sapporo"]
      d' test-doc]
  (log/ppr :test "MULTINOMIAL IS"
    [(->> (this/classifier-score+ D :multinomial d')
          (join []) (sort-by second))
     (this/multinomial-naive-bayes-classifier D d')])
  (log/ppr :test "MULTIPLE BERNOULLI IS"
     [(->> (this/classifier-score+ D :bernoulli d')
           (join []) (sort-by second))
     (this/multiple-bernoulli-naive-bayes-classifier D d')]))

(log/pr :test "===== END TESTING ======")
