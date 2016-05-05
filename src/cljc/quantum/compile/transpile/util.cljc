(ns quantum.compile.transpile.util
           (:require [quantum.core.string      :as str  ]
                     [quantum.core.collections :as coll
                       :refer [dropr]                   ]
                     [quantum.core.fn          :as fn
                       :refer [#?@(:clj [<- f*n fn->])] ]
                     [quantum.core.logic       :as logic
                       :refer [#?@(:clj [whenf fn-not])]])
  #?(:cljs (:require-macros
                     [quantum.core.fn          :as fn
                       :refer [<- f*n fn->]             ]
                     [quantum.core.logic       :as logic
                       :refer [whenf fn-not]            ])))

(defn ^String semicoloned
  {:in  '["package" "qjava.abc"]
   :out "package qjava.abc;\n"}
  [& args]
  (str (str/join " " args) ";\n"))

(defn scolon
  "Appends a semicolon if it doesn't already have one on the end."
  [s]
  (if (str/ends-with? s ";")
      s
      (str s ";")))

(def default-indent-num 4)
(def ^:dynamic *indent-num* default-indent-num)
(def indentation (->> (repeat *indent-num* \space) (apply str)))

(defn indent [s]
  (->> s (str indentation) (<- str/replace #"\n" (str "\n" indentation))))

(defn bracket
  {:in '["class ABC" "println()"]
   :out "class ABC {
          println()
        }"}
  ([^String body]
    (str "{ " (str/replace body #"\n" (str "\n" indentation))
      " }"))
  ([^String header ^String body]
    (if (empty? body)
        (str/sp header "{}")
        (let [^String body-indented
               (str indentation
                 (-> body
                     (str/replace #"\n"     (str "\n"  indentation))
                     (str/replace #"}$" (str "}\n" indentation))))
              body-f
                (-> body-indented
                    (whenf (f*n str/ends-with? indentation) ; To get rid of trailing indentation
                      (partial dropr *indent-num*))
                    (whenf (fn-not (f*n str/ends-with? "\n"))
                      (fn-> (str "\n"))))]
          (str header " {\n"
            body-f
            "}")))))