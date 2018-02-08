(ns quantum.untyped.core.string
  (:require
    [clojure.string            :as str]
    [quantum.untyped.core.core :as ucore]
    [quantum.untyped.core.fn
      :refer [fn->]]
    [quantum.untyped.core.string.format :as ustr|form]
    [quantum.untyped.core.vars
      :refer [defalias]]))

(ucore/log-this-ns)

(defn join-once
  "Like /clojure.string/join/ but ensures no double separators."
  {:attribution "taoensso.encore"}
  [separator & coll]
  (reduce
    (fn [s1 s2]
      (let [s1 (str s1) s2 (str s2)]
        (if (str/ends-with? s1 separator) ; could use ends-with? but would be self-referring
            (if (str/starts-with? s2 separator)
                (str s1 (.substring s2 1))
                (str s1 s2))
            (if (str/starts-with? s2 separator)
                (str s1 s2)
                (if (or (= s1 "") (= s2 ""))
                    (str s1 s2)
                    (str s1 separator s2))))))
    nil
    coll))

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

#?(:clj (defalias ucore/istr))
