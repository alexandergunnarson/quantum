(ns quantum.core.analyze.clojure.core
  (:require-quantum [:core logic fn])
  (:require #?(:clj  [clojure.jvm.tools.analyzer :as ana])
            #?(:clj  [clojure.tools.analyzer.jvm]
               ;:cljs [clojure.tools.analyzer.js ]
               )))

#?(:clj
(defmacro ast
  {:usage '(ast (let [a 1 b {:a a}] [(-> a (+ 4) (/ 5))]))}
  ([lang & args]
    (condp = lang
      :clj  `(cond
               true  (ana/ast     ~@args)
               :else (clojure.tools.analyzer.jvm/analyze ~@args))
      ;:cljs `(clojure.tools.analyzer.js/analyze)
      ))))

#?(:clj
(defalias
  ^{:doc "Returns a vector of maps representing the ASTs of the forms
          in the target file."
    :usage '(analyze-file "my/ns.clj")}
  analyze-file ana/analyze-file))