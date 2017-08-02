(ns quantum.core.analyze.clojure.core
  (:require
#?@(:clj
   [[clojure.jvm.tools.analyzer :as ana]
    [clojure.tools.analyzer.jvm :as clj-ana]])
 #_(:cljs [clojure.tools.analyzer.js])
    [quantum.core.collections.base :as cbase
      :refer [kw-map]]
    [quantum.core.error            :as err
      :refer [ex!]]
    [quantum.core.fn
      :refer [fn1 rcomp]]
    [quantum.core.logic
      :refer [whenf1 fn-not]]
    [quantum.core.macros.core      :as cmacros]
    [quantum.core.macros.type-hint :as th]
    [quantum.core.type.core        :as tcore]
    [quantum.core.vars             :as var
      :refer [defalias]])
#?(:clj
  (:import
    (clojure.lang RT Compiler))))

; ===== ANALYSIS ===== ;

; Ensures the return value of local bindings are inferred appropriately
#?(:clj
(extend-protocol ana/AnalysisToMap
  clojure.lang.Compiler$LocalBinding
  (analysis->map
    [lb env opt]
    (let [init (when-let [init (.init lb)]
                 (ana/analysis->map init env opt))]
      (merge
        {:op :local-binding
         :env (@#'ana/inherit-env init env)
         :sym (.sym lb)
         :tag (or (.tag lb) (when (.hasJavaClass lb) (.getJavaClass lb))) ; changed this
         :init init}
        (when (:children opt)
          {:children [[[:init] {}] ;optional
                      ]})
        (when (:java-obj opt)
          {:LocalBinding-obj lb}))))))

#?(:clj
(defmacro ast
  {:usage '(ast (let [a 1 b {:a a}] [(-> a (+ 4) (/ 5))]))}
  ([lang & args]
    (case lang
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
(defn macro-env->ana-env [env]
  {:ns (ns-name *ns*)
   :locals (->> env
                (map (juxt key (rcomp val (whenf1 (fn-not map?) (fn1 ana/analysis->map nil nil)))))
                (into {}))}))

#?(:clj
(defn jvm-typeof
  ([expr] (jvm-typeof expr nil))
  ([expr env]
    (try
      (with-bindings {Compiler/LOADER (RT/makeClassLoader)
                      Compiler/METHOD nil
                      Compiler/LOCAL_ENV env
                      Compiler/LOOP_LOCALS nil
                      Compiler/NEXT_LOCAL_NUM 0
                      RT/CURRENT_NS @RT/CURRENT_NS
                      RT/UNCHECKED_MATH @RT/UNCHECKED_MATH
                      Compiler/LINE_BEFORE (int -1)
                      Compiler/LINE_AFTER  (int -1)
                      Compiler/COLUMN_BEFORE (int -1)
                      Compiler/COLUMN_AFTER  (int -1)
                      ;RT/WARN_ON_REFLECTION false
                      RT/DATA_READERS @RT/DATA_READERS}
        (let [expr   (list 'fn [] expr)
              fn-ast ^clojure.lang.Compiler$FnExpr
                     (clojure.lang.Compiler/analyze
                      clojure.lang.Compiler$C/EXPRESSION expr)
              expr-ast ^clojure.lang.Compiler$BodyExpr
                       (.body ^clojure.lang.Compiler$ObjMethod (first (.methods fn-ast)))]
          (when (.hasJavaClass expr-ast)
            (.getJavaClass expr-ast))))
      #_(catch Throwable e nil))))) ; TODO fix in `go` block: `clojure.lang.Compiler$CompilerException: java.lang.ClassCastException: clojure.lang.PersistentArrayMap cannot be cast to clojure.lang.Compiler$LocalBinding, compiling:(null:7:31)`

#?(:clj
(defn jvm-typeof-respecting-hints
  "Like `jvm-typeof` but respects type hints."
  ([expr] (jvm-typeof-respecting-hints expr nil))
  ([expr env]
    (or (some-> expr th/type-hint th/tag->class) ; TODO don't assume CLJ
        (jvm-typeof expr env)))))


#?(:clj
(defn typeof*
  "Uses the Clojure compiler to analyze the given s-expr. Returns
   a class/tag indicating what the compiler concluded about the
   return value of the expression.
   A `nil` result means that the return value is nil, or that no
   type information is available."
  ([expr         ] (th/tag->class (:tag (clj-ana/analyze expr))))
  ([expr env     ] (th/tag->class (:tag (clj-ana/analyze expr (macro-env->ana-env env)))))
  ([expr env opts] (th/tag->class (:tag (clj-ana/analyze expr (macro-env->ana-env env) opts))))))

#?(:clj
(defmacro typeof
  "Compile-time `typeof*`"
  ([& args] (apply typeof* args))))

#?(:clj
(defmacro static-cast-depth [xs depth x]
  (cmacros/case-env
    :cljs (throw (ex-info "Depth casting not supported for hints in CLJS (yet)" (kw-map xs depth x)))
    :clj  (let [hint       (jvm-typeof-respecting-hints xs &env)
                _          (assert hint {:xs xs :hint hint})
                cast-class (th/tag->class (tcore/nth-elem-type:clj hint depth))]
            (if (.isPrimitive ^Class cast-class)
                `(~(symbol "clojure.core" (str cast-class)) ~x)
                (tcore/static-cast-code (th/->embeddable-hint cast-class) x))))))
