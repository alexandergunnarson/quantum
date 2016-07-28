(ns quantum.test.compile.transpile.from.java
  (:require [quantum.compile.transpile.from.java :as ns]))

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

