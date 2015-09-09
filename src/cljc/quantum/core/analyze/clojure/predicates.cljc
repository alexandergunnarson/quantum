(ns quantum.core.analyze.clojure.predicates
  (:require-quantum [ns fn logic])
  (:require
    [clojure.string         :as str]
    [quantum.core.type.core :as tcore]))

; SYMBOLS

#?(:clj 
(defn type-hint
  "Returns a symbol representing the tagged class of the symbol, or |nil| if none exists."
  {:source "ztellman/riddley.compiler"}
  [x]
  (when-let [tag (-> x meta :tag)]
    (let [sym (symbol
                (if (instance? Class tag)
                  (.getName ^Class tag)
                  (name tag)))]
      (when-not (= 'java.lang.Object sym)
        sym)))))

(defn symbol-eq? [s1 s2] (= (name s1) (name s2)))
(defn metaclass    [sym] (whenc (type-hint sym) (fn-> name empty?) nil))
(defn qualified?   [sym] (-> sym str (.indexOf "/") (not= -1)))
(defn unqualify    [sym] (-> sym name symbol))
(defn auto-genned? [sym] (-> sym name (.endsWith "__auto__")))

(def possible-type-predicate?
  (fn-or keyword? (fn-and symbol? (fn-> name (.contains "?")))))

(def hinted-literal? (fn-or char? number? string? vector? map? nil? keyword?))

; SCOPE

(defn shadows-var? [bindings v]
  (->> bindings (apply hash-map)
       keys
       (map name)
       (into #{})
       (<- contains? (name v))))

(def new-scope?
  (fn-and seq?
    (fn-> first symbol?)
    (fn-> first name (= "let"))))

; ARGLISTS

(def first-variadic?   (fn-> first name (= "&")))
(def variadic-arglist? (fn-> butlast last name (= "&")))

(defn arity-type [arglist]
  (if (variadic-arglist? arglist)
      :variadic
      :fixed))

(def arglist-arity
  (if*n
    variadic-arglist?
    (fn-> count dec)
    count))

; FORMS

(defn form-and-begins-with? [sym]
  (fn-and seq? (fn-> first (= sym))))
(defn form-and-begins-with-any? [set-n]
  (fn-and seq? (fn [x] (apply splice-or (first x) = set-n))))

(def else-pred?         (fn-or (eq? :else) (eq? true)))

(def str-expression?    (fn-and seq? (fn-> first (= 'str))))
(def string-concatable? (fn-or string? str-expression?))

; STATEMENTS
(def sym-call? (fn-and seq? (fn-> first symbol?)))
(def primitive-cast? (fn-and sym-call? (fn-> first name symbol tcore/primitive?)) )
(defn type-cast? [obj lang]
  (or (primitive-cast? obj)
      (and (sym-call? obj) (get-in tcore/type-casts-map [lang (-> obj first name symbol)]))))
(def constructor? (fn-and sym-call? (fn-> first name (.endsWith "."))))

(def return-statement?      (form-and-begins-with? 'return))
(def defn-statement?        (form-and-begins-with? 'defn  ))
(def fn-statement?          (form-and-begins-with? 'fn  ))
(def function-statement?    (fn-or defn-statement? fn-statement?))
(def scope?                 (form-and-begins-with-any? '#{defn fn while when doseq for do}))
(def let-statement?         (form-and-begins-with? 'let   ))
(def do-statement?          (form-and-begins-with? 'do   ))
(def if-statement?          (form-and-begins-with? 'if    ))
(def cond-statement?        (form-and-begins-with? 'cond  ))
(def when-statement?        (form-and-begins-with? 'when  ))

; CONDITIONAL BRANCHES

(def one-branched?          (fn-or when-statement?
                                   (fn-and if-statement?   (fn-> count (= 3)))
                                   (fn-and cond-statement? (fn-> count (= 3)))))
(def two-branched?          (fn-or (fn-and if-statement?   (fn-> count (= 4)))
                                   (fn-and cond-statement? (fn-> count (= 5))
                                                           (fn-> (nth 3) else-pred?))))
(def many-branched?         (fn-and cond-statement?
                              (fn-or (fn-and (fn-> count (= 5))
                                             (fn-> (nth 3) else-pred? not))
                                     (fn-> count (> 5)))))
(def conditional-statement? (fn-or cond-statement? if-statement? when-statement?))
(def cond-foldable?         (fn-and two-branched?
                              (fn-or (fn-and if-statement?   (fn-> (nth 3) conditional-statement?))
                                     (fn-and cond-statement? (fn-> (nth 4) conditional-statement?)))))