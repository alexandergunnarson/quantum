(ns quantum.test.compile.transpile.from.java
  (:require
    [#?(:clj clojure.test
        :cljs cljs.test)
      :refer        [#?@(:clj [deftest is testing])]
      :refer-macros [deftest is testing]]
    [quantum.core.print                  :as pr
      :refer [!]]
    [quantum.core.convert                :as conv]
    [quantum.core.collections            :as coll
      :refer [dropl dropr popr]]
    [quantum.compile.transpile.from.java :as ns]))

(defn test:add-context [x])

(defn test:clean-javadoc [s])

(defn test:type-hint* [x]
  (let [hint-0 (str x)
        hint (if (= hint-0 "byte[]") ; TODO elaborate on this
                 "\"[B\""
                 hint-0)]
    (->> hint (str "^") symbol)))

(defn test:do-each [x])

(defn test:test:implicit-do [x])

(defn test:remove-do-when-possible [x])

#?(:clj
(defn test:parse-modifiers [mods]))

#?(:clj
(defn test:parse-operator [x]))

#?(:clj
(defn test:parse-conditional [x]))

#?(:clj
(defn test:parse [x]))

#?(:clj
(defn test:clean [x]))

#?(:clj (require '[cljfmt.core :as fmt]))

#?(:clj
(defn test-integration
  []
  (->> "./dev-resources/test/quantum/compile/transpile/BitSieve.java"
       conv/->file
       ns/parse
       ns/clean
       doall
       (#(with-out-str (! %)))
       (dropl 5) ; (do
       popr popr ; TODO fix to do before `apply str`
       fmt/reformat-string))) ; TODO fix this

#?(:clj (spit "./dev-resources/test/quantum/compile/transpile/bit_sieve.clj"
              (let [ret (test-integration)]
                (with-out-str (println ret)))))
