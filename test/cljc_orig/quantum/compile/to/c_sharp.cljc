(ns quantum.compile.to.c-sharp
  (:require-quantum [:lib])
  (:require
    [quantum.compile.util    :as util]))

(defn nspace [^String class-str ^String ns-name-0]
  (let [^String ns-str (str/sp "namespace" ns-name-0)]
    (util/bracket ns-str class-str)))