(ns quantum.test.compile.transpile.util
  (:require [quantum.compile.transpile.util :refer :all]))

(defn ^String test:semicoloned
  [& args])

(defn test:scolon [s])

(defn test:indent [s])

(defn test:bracket
  ([^String body])
  ([^String header ^String body]))