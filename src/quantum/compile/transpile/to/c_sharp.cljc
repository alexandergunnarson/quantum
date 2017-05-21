(ns quantum.compile.transpile.to.c-sharp
  (:require [quantum.core.string            :as str ]
            [quantum.compile.transpile.util :as util]))

(defn nspace [^String class-str ^String ns-name-0]
  (let [^String ns-str (str/sp "namespace" ns-name-0)]
    (util/bracket ns-str class-str)))