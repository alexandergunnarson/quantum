(ns quantum.untyped.core.string
  (:require
    [clojure.string            :as str]
    [quantum.untyped.core.core :as ucore]
    [quantum.untyped.core.fn   :as fn
      :refer [fn->]]))

(ucore/log-this-ns)

(defn camelcase
  {:attribution  "flatland.useful.string"
   :contributors #{'alexandergunnarson}}
  [str-0 & [method?]]
  (-> str-0
      (str/replace #"[-_](\w)"
        (fn-> second str/upper-case))
      (#(if (not method?)
           (apply str (-> % first str/upper-case) (rest %))
           %))))
