(ns quantum.core.analyze.clojure.core
           (:require
            #?(:clj  [clojure.jvm.tools.analyzer :as ana  ])
            #?(:clj  [clojure.tools.analyzer.jvm          ]
              ;:cljs [clojure.tools.analyzer.js           ]
                     )
                     [quantum.core.vars          :as var
                             :refer [#?@(:clj [defalias])]])
  #?(:cljs (:require-macros
                     [quantum.core.vars          :as var
                       :refer [defalias]                  ])))

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

#?(:clj
 (defn expr-info*
  {:attribution 'clisk.util}
  [expr]
  (let [fn-ast ^clojure.lang.Compiler$FnExpr
               (clojure.lang.Compiler/analyze
                clojure.lang.Compiler$C/EXPRESSION expr)
        expr-ast ^clojure.lang.Compiler$BodyExpr
                 (.body ^clojure.lang.Compiler$ObjMethod (first (.methods fn-ast)))]
        (println "class" (class expr-ast ))
    (when (.hasJavaClass expr-ast)
      {:class      (.getJavaClass expr-ast)
       :primitive? (.isPrimitive (.getJavaClass expr-ast))}))))

#?(:clj
(defn expr-info
  "Uses the Clojure compiler to analyze the given s-expr.  Returns
  a map with keys :class and :primitive? indicating what the compiler
  concluded about the return value of the expression.  Returns nil if
  no type info can be determined at compile-time.

  Example: (expression-info '(+ (int 5) (float 10)))
  Returns: {:class float, :primitive? true}"
  {:attribution 'clisk.util}
  [expr]
  (expr-info* `(fn [] ~expr))))

#?(:clj (defmacro typeof     {:attribution 'clisk.util}
          ([expr] (-> expr expr-info :class     ))))
#?(:clj (defmacro primitive? {:attribution 'clisk.util}
          ([expr] (-> expr expr-info :primitive?))))
