(ns quantum.untyped.core.string
  (:require
    [clojure.string  :as str]
    [quantum.core.fn :as fn
      :refer [fn->]]))

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
