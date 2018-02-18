(ns quantum.untyped.core.nondeterministic
  (:require
    [clojure.core :as core]
    [quantum.untyped.core.error
      :refer [TODO]])
#?(:clj
  (:import
    java.security.SecureRandom)))

#?(:clj (defonce ^SecureRandom secure-random-generator
          (SecureRandom/getInstance "SHA1PRNG")))

(defn #?(:clj ^java.util.Random get-generator :cljs get-generator) [secure?]
  #?(:clj (if secure?
              secure-random-generator
              (java.util.concurrent.ThreadLocalRandom/current))
     :cljs (TODO)))

(defn double-between
  "Yields a random double between a and b."
  ([        a b] (double-between false a b))
  ([secure? a b]
    #?(:clj (let [generator (get-generator secure?)]
              (+ a (* (.nextDouble generator) (- b a))))
       :cljs (if secure?
                 (TODO "CLJS does not yet support secure random numbers")
                 (+ a (core/rand (inc (- b a))))))))
