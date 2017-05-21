(ns quantum.test.core.macros.fn
  (:require [quantum.core.macros.fn :as fn]))

(defn test:defn-variant-organizer
  [f opts lang ns- sym doc- meta- body [unk & rest-unk]])

(defn test:optimize-defn-variant-body!
  [body externs])

(defn test:fn+*
  ([sym doc- meta- arglist body [unk & rest-unk]]))

(defn test:fn+ [sym & body])

(defn test:defn+ [sym & body])

(defn test:defmethod+
  [sym type & arities])

(defn test:defmethods+
  [sym dispatches & args])