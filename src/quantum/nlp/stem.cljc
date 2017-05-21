(ns quantum.nlp.stem
  "A stemmer transforms a word into its root form. The stemming is a process
   for removing the commoner morphological and inflexional endings from words
   (in English). Its main use is as part of a term normalization process."
  (:require
    [quantum.core.vars
      :refer        [#?@(:clj [defalias])]
      :refer-macros [          defalias]]
    [quantum.core.error
      :refer [->ex TODO]]
    [quantum.core.log :as log
      :include-macros true]
    [quantum.nlp.stem.impl.porter :as porter]))

(log/this-ns)

(defn porter
  "Porter's stemming algorithm. The stemmer is based on the idea that the
   suffixes in the English language are mostly made up of a combination of
   smaller and simpler suffixes."
  {:implemented-by '#{smile.nlp.stemmer.PorterStemmer
                      quantum.nlp.stem.porter}}
  ([s] (porter s :quantum))
  ([s impl]
    (case impl
      :smile   (TODO)
      :quantum (porter/stem s))))

(defn lancaster
  "The Paice/Husk Lancaster stemming algorithm. The stemmer is a conflation
   based iterative stemmer. The stemmer, although remaining efficient and
   easily implemented, is known to be very strong and aggressive."
  {:implemented-by '#{smile.nlp.stemmer.LancasterStemmer}}
  ([s] (TODO)))
