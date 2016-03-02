(ns quantum.core.util.test
  (:refer-clojure :exclude [merge]))

; (ns
;   ^{:doc "An almost-never-used namespace for generating random data structures
;           for testing purposes."
;     :attribution "Alex Gunnarson"}
;   quantum.core.util.test
;   (:refer-clojure :exclude [merge])
;   (:require
;     [quantum.core.ns :as ns
;       #?@(:clj [:refer [defalias alias-ns]])]
;   	[quantum.core.string :as str]
;   	[quantum.core.collections :as coll :refer [merge]])
;   #?(:clj (:gen-class)))

; #?(:clj (ns/require-all *ns* :clj))

; (defn rand-maps [ct]
;   (for [n (range 0 ct)]
;     (hash-map
;       (keyword (str/rand-str 30))
;       (str/rand-str 5))))

; (defn rand-map [ct]
;   (apply merge (rand-maps ct)))