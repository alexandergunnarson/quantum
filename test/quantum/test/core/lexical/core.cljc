(ns quantum.test.core.lexical.core
  (:require [quantum.core.lexical.core :as ns]))

(defn test:match [re])

(defn test:observe [re])

(defn test:attach
  [rule f])

(defn test:series
  [& rules])

(defn test:choice
  [& rules])

(defn test:many
  [rule])

(defn test:forward [rule])

; RULES

(defn test:rnd-choice
  [coll])

(defn test:take-fn
  [n f])

(defn test:rnd-seq
  [f min max])

(defn test:char-range
  [from to])

(defn test:invert
  [chars])

(defn test:first-if-single
  [coll])

(defn test:combine-groups
  [merge-func tokens])

(defn test:sequence-of-chars
  [src])

(defn test:get-char-list
  [char-groups invert?])

(defn test:combine-many
  [tokens])