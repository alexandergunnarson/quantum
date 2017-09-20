(ns quantum.core.untyped.qualify
  "Functions related to qualification (name, namespace, etc.) and unqualification
   of nameables."
  (:require
    [quantum.core.core
      :refer [namespace?]]))

(defn ?ns->name [?ns]
  (name #?(:clj (if (namespace? ?ns)
                    (ns-name ?ns)
                    ?ns)
           :cljs ?ns)))

;; ===== QUALIFICATION ===== ;;

(defn qualify
  #?(:clj ([sym] (qualify *ns* sym)))
  ([?ns sym] (symbol (?ns->name ?ns) (name sym))))

(defn qualify:dot [sym ns-]
  (symbol (str (?ns->name ns-) "." (name sym))))

#?(:clj (defn qualify:class [sym] (symbol (str (-> *ns* ->symbol munge) "." sym))))

(defn unqualify [sym] (-> sym name symbol))

#?(:clj
(defn collapse-symbol [sym]
  (symbol
    (when-let [n (namespace sym)]
      (if-let [alias- (do #?(:clj (ns/ns-name->alias *ns* (symbol n)) :cljs false))]
        (str alias- "/")
        n))
    (name sym))))
