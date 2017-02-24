(ns quantum.core.analyze.clojure.core
           (:require
            #?(:clj  [clojure.jvm.tools.analyzer :as ana  ])
            #?(:clj  [clojure.tools.analyzer.jvm :as clj-ana]
              ;:cljs [clojure.tools.analyzer.js           ]
                     )
                     [quantum.core.macros.core   :as cmacros]
                     [quantum.core.vars          :as var
                             :refer [#?@(:clj [defalias])]]
                     [quantum.core.fn
                       :refer [fn1 rcomp]]
                     [quantum.core.logic
                       :refer [whenf1 fn-not]]
                     [quantum.core.type.core     :as tcore])
  #?(:cljs (:require-macros
                     [quantum.core.vars          :as var
                       :refer [defalias]                  ])))

; ===== TAGS / TYPE HINTS ===== ;

(defn type-hint [x] (-> x meta :tag))

#?(:clj
(defn sanitize-tag [lang tag]
  (or (get-in tcore/return-types-map [lang tag]) tag)))

#?(:clj
(defn sanitize-sym-tag [lang sym]
  (cmacros/hint-meta sym (sanitize-tag lang (type-hint sym)))))

#?(:clj
(defn tag->class [tag]
  (cond (or (nil? tag) (class? tag))
        tag
        (symbol? tag)
        (eval (sanitize-tag :clj tag))
        (string? tag)
        (Class/forName tag)
        :else (throw (ex-info "Cannot convert tag to class" {:tag tag})))))

#?(:clj (defn type-hint:class [x] (-> x type-hint tag->class)))

(defn type-hint:sym "Returns a symbol representing the tagged class of the symbol, or |nil| if none exists."
  {:source "ztellman/riddley.compiler"} [x]
  (when-let [tag (-> x meta :tag)]
    (let [sym (symbol (cond (symbol? tag) (namespace tag)
                            :else         nil)
                      (if #?@(:clj  [(instance? Class tag) (.getName ^Class tag)]
                              :cljs [true])
                          (name tag)))]
      sym)))

#?(:clj
(defn ->embeddable-hint
  "The compiler ignores, at least in cases, hints that are not string or symbols,
   and does not allow primitive hints.
   This fn accommodates these requirements."
  [hint]
  (if (class? hint)
      (if (.isPrimitive ^Class hint)
          nil
          (.getName ^Class hint))
      hint)))

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
         :tag (or (.tag lb) (and (.hasJavaClass lb) (.getJavaClass lb)))
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
(defn macro-env->ana-env [env]
  {:ns (ns-name *ns*)
   :locals (->> env
                (map (juxt key (rcomp val (whenf1 (fn-not map?) (fn1 ana/analysis->map nil nil)))))
                (into {}))}))

#?(:clj
 (defn fast-typeof*
  {:attribution 'clisk.util}
  [expr]
  (let [expr   (list 'fn [] expr)
        fn-ast ^clojure.lang.Compiler$FnExpr
               (clojure.lang.Compiler/analyze
                clojure.lang.Compiler$C/EXPRESSION expr)
        expr-ast ^clojure.lang.Compiler$BodyExpr
                 (.body ^clojure.lang.Compiler$ObjMethod (first (.methods fn-ast)))]
    (when (.hasJavaClass expr-ast)
      (.getJavaClass expr-ast)))))

#?(:clj
(defn typeof*
  "Uses the Clojure compiler to analyze the given s-expr. Returns
   a class/tag indicating what the compiler concluded about the
   return value of the expression.
   A `nil` result means that the return value is nil, or that no
   type information is available."
  ([expr] (tag->class (:tag (clj-ana/analyze expr))))
  ([expr env] (tag->class (:tag (clj-ana/analyze expr (macro-env->ana-env env)))))
  ([expr env opts] (tag->class (:tag (clj-ana/analyze expr (macro-env->ana-env env) opts))))))

#?(:clj
(defmacro typeof
  "Compile-time `typeof*`"
  ([& args] (typeof* ~@args))))

#?(:clj
(defn typeof**
  "Like `typeof` but respects type hints."
  ([expr]
    (or (some->> expr type-hint tag->class) ; TODO don't assume CLJ
        (fast-typeof* expr)))
  ([expr env]
    (or (some->> expr type-hint tag->class) ; TODO don't assume CLJ
        (let [tag (typeof* expr env)]
          (if (= tag java.lang.Object) ; the class
              ; TODO doesn't accept an env...
              (fast-typeof* expr) ; because typeof* doesn't always get it right apparently: case in point `'(let [x (identity nil)] (-> (java.nio.ByteBuffer/allocate 8) (.putLong (long x)) .array))`
              tag))))))
