(ns ^{:doc "Clojure (and variants) code analysis namespace. Required for quantum.core.macros."}
  quantum.core.analyze.clojure.predicates
  (:refer-clojure :exclude [name])
  (:require
    [clojure.core                      :as core]
    [clojure.string                    :as str]
#?(:clj
    [clojure.jvm.tools.analyzer        :as tana])
    [quantum.core.analyze.clojure.core :as ana]
    [quantum.core.fn                   :as fn
      :refer [fn$ <- fn-> fn->>]]
    [quantum.core.logic                :as logic
      :refer [splice-or fn= fn-or fn-and fn-not whenc ifn ifn1]]
    [quantum.core.type.core            :as tcore]
    [quantum.core.vars                 :as var
      :refer [defalias]]))

(defn safe-mapcat
  "Like |mapcat|, but works if the returned values aren't sequences."
  {:from "clojure.jvm.tools.analyzer.examples.tail-recursion"}
  [f & colls]
  (apply concat (map #(if (seq? %) % [%]) (apply map f colls))))

; Because clojure.string/index-of works for CLJ but not CLJS yet
(defn str-index-of [x sub]
  #?(:clj  (.indexOf ^String x ^String sub)
     :cljs (.indexOf         x sub)))

(defn str-ends-with? [x sub]
  #?(:clj  (.endsWith ^String x ^String sub)
     :cljs (.endsWith         x sub)))

; SYMBOLS
(defn name [x] (if (nil? x) nil (core/name x))) ; TODO move

(defn type-hint "Returns a symbol representing the tagged class of the symbol, or |nil| if none exists."
  {:source "ztellman/riddley.compiler"} [x]
  (when-let [tag (-> x meta :tag)]
  (let [sym (symbol (cond (symbol? tag) (namespace tag)
                          :else         nil)
                    (if #?@(:clj  [(instance? Class tag) (.getName ^Class tag)]
                            :cljs [true])
                        (name tag)))]
    sym)))

; TODO abstract platform-dependent member calls

(defn symbol-eq? [s1 s2] (= (name s1) (name s2)))

(defn metaclass    [sym]
  (whenc (type-hint sym) (fn-> name empty?) nil))


(defn qualified?   [sym] (-> sym str (str-index-of "/") (not= -1)))
(defn auto-genned? [sym] (-> sym name (str-ends-with? "__auto__")))
(def possible-type-predicate? (fn-or keyword? (fn-and symbol? (fn-> name (str-index-of "?") (not= -1)))))
(def hinted-literal?          (fn-or #?(:clj char?) number? string? vector? map? nil? keyword?))

;  ===== SCOPE =====
(defn shadows-var? [bindings v]
  (->> bindings (apply hash-map) keys (map name)
       (into #{})
       (<- contains? (name v))))
(def new-scope? (fn-and seq? (fn-> first symbol?)
   (fn-> first name (= "let"))))
; ===== ARGLISTS =====
(def first-variadic? (fn-> first name (= "&")))
(def variadic-arglist?
  (fn-> butlast last (ifn nil? (constantly nil) name) (= "&")))
(defn arity-type [arglist] (if (variadic-arglist? arglist) :variadic :fixed))
(def arglist-arity (ifn1 variadic-arglist? (fn-> count dec) count))

; ===== FORMS =====
(defn form-and-begins-with? [sym] (fn-and seq? (fn-> first (= sym))))
(defn form-and-begins-with-any? [set-n]
  (fn-and seq? (fn [x] (apply splice-or (first x) = set-n))))
(def else-pred?         (fn-or (fn= :else) (fn= true))) ; TODO this is wrong
(def str-expression?    (fn-and seq? (fn-> first (= 'str))))
(def string-concatable? (fn-or string? str-expression?))

; ===== STATEMENTS =====
(def sym-call? (fn-and seq? (fn-> first symbol?)))
(defalias s-expr? sym-call?)

(def primitive-cast? (fn-and sym-call? (fn-> first name symbol tcore/prim?)))

(defn type-cast? [obj lang]
  (or (primitive-cast? obj)
      (and (sym-call? obj)
           (get-in tcore/type-casts-map
              [lang (-> obj first name symbol)]))))

(def constructor? (fn-and sym-call? (fn-> first ^String name (.endsWith "."))))

; TODO use quantum str package
(def return-statement?      (form-and-begins-with? 'return))
(def defn-statement?        (form-and-begins-with? 'defn  ))
(def defnt-statement?       (form-and-begins-with? 'defnt ))
(def fn-statement?          (form-and-begins-with? 'fn  ))
(def function-statement?    (fn-or defn-statement? fn-statement? defnt-statement?))
(def scope?                 (form-and-begins-with-any? '#{defn fn while when doseq for do}))
(def let-statement?         (form-and-begins-with? 'let   ))
(def do-statement?          (form-and-begins-with? 'do    ))
(def if-statement?          (form-and-begins-with? 'if    ))
(def cond-statement?        (form-and-begins-with? 'cond  ))
(def when-statement?        (form-and-begins-with? 'when  ))
(def throw-statement?       (form-and-begins-with? 'throw ))
(def quote-statement?       (form-and-begins-with? 'quote ))
; ; CONDITIONAL (AND TRY) BRANCHES
(def branching-syms #{'when 'if 'cond 'case 'try})
; ; TODO Is |try| really considered branching?
(def branching-expr?        (fn-and s-expr? (fn->> first (contains? branching-syms))))
(def one-branched?          (fn-or when-statement? (fn-and if-statement?   (fn-> count (= 3))) (fn-and cond-statement? (fn-> count (= 3)))));
(def two-branched?          (fn-or (fn-and if-statement?   (fn-> count (= 4))) (fn-and cond-statement? (fn-> count (= 5)) (fn-> (nth 3) else-pred?))))
(def many-branched?         (fn-and cond-statement? (fn-or (fn-and (fn-> count (= 5)) (fn-> (nth 3) else-pred? not)) (fn-> count (> 5)))))
(def conditional-statement? (fn-or cond-statement? if-statement? when-statement?))
(def cond-foldable?         (fn-and two-branched?
                                    (fn-or (fn-and if-statement?
                                                   (fn-> (nth 3) conditional-statement?))
                                           (fn-and cond-statement?
                                                   (fn-> (nth 4) conditional-statement?)))))

#?(:clj
(defn- find-tail-ops
  "Returns a list of the function calls that are in tail position."
  {:from "clojure.jvm.tools.analyzer.examples.tail-recursion"}
  [tree]
  (case (:op tree)
    :def       (safe-mapcat find-tail-ops (rest (tana/children tree)))
    :do        (recur (last (tana/children tree)))
    :fn-expr   (safe-mapcat find-tail-ops (:methods tree))
    :fn-method (recur (:body tree))

    :invoke
    (or (-> tree :fexpr :local-binding :sym)
        (-> tree :fexpr :var))

    :let (recur (:body tree))
    :if (map find-tail-ops [(:then tree) (:else tree)])
    nil)))

#?(:clj
(defn- tail-recursive?*
  {:from "clojure.jvm.tools.analyzer.examples.tail-recursion"}
  [fn-tree]
  (let [fn-name (or (-> fn-tree :name) (-> fn-tree :var))
        tail-ops (find-tail-ops fn-tree)]
    (boolean (when fn-name (some (partial = fn-name) tail-ops))))))

#?(:clj
(defn tail-recursive?
  "Returns `true` if there is a call to the function being defined
   in a tail position.  This does not necessarily mean that the tail call
   can be replaced with `recur`, since that does not work with functions of
   different arity, or across `try`."
  [expr]
  (->> expr (ana/ast #?(:clj :clj :cljs :cljs)) tail-recursive?)))

(defn constant? [v] (when (var? v) (-> v meta :const  )))

(defn private? [v]
  (when (var? v)
    (or (-> v meta :private)
        #?(:clj (not (.isPublic ^clojure.lang.Var v))))))

(defn macro? [v]
  (when (var? v)
    (or (-> v meta :macro)
        #?(:clj (.isMacro ^clojure.lang.Var v)))))

(defn dynamic? [v]
  (when (var? v)
    (or (-> v meta :dynamic)
        #?(:clj (.isDynamic ^clojure.lang.Var v))))) ; workaround needed since Clojure doesn't always propagate :dynamic

(def ^{:doc "As in, macro-embeddable"} ; Really, anything for which `print-dup` is defined
  embeddable?
  (fn-or number? symbol? keyword? string?
         char? list? vector? set?
         (fn-and map? (fn-not record?))
         nil?
         #?@(:clj [(fn$ instance? clojure.lang.Cons   ) ; TODO maybe other things too
                   (fn$ instance? clojure.lang.LazySeq)])))
