(ns quantum.core.untyped.qualify
  "Functions related to qualification (name, namespace, etc.) and unqualification
   of nameables."
  (:require
    [clojure.string            :as str]
    [fipp.ednize]
    [quantum.core.untyped.core :as qcore
      :refer [namespace?]]
    [quantum.core.ns           :as ns]))

(defn named? [x] (instance? #?(:clj clojure.lang.Named :cljs cljs.core/INamed) x))

(defn ?ns->name [?ns]
  (name #?(:clj (if (namespace? ?ns)
                    (ns-name ?ns)
                    ?ns)
           :cljs ?ns)))

;; ===== QUALIFICATION ===== ;;

(defn qualify
  #?(:clj ([sym] (qualify *ns* sym)))
  ([?ns sym] (symbol (?ns->name ?ns) (name sym))))

(defn qualify|dot [sym ns-]
  (symbol (str (?ns->name ns-) "." (name sym))))

#?(:clj (defn qualify|class [sym] (symbol (str (-> *ns* ns-name name munge) "." sym))))

(defn unqualify [sym] (-> sym name symbol))

#?(:clj
(defn collapse-symbol
  ([sym] (collapse-symbol sym true))
  ([sym extra-slash?]
    (symbol
      (when-let [n (namespace sym)]
        (when-not (= n (-> *ns* ns-name name))
          (if-let [alias- (do #?(:clj (ns/ns-name->alias *ns* (symbol n)) :cljs false))]
            (str alias- (when extra-slash? "/"))
            n)))
      (name sym)))))

;; ===== IDENTS ===== ;;

(defrecord
  ^{:doc "A delimited identifier.
          Defaults to delimiting all qualifiers by the pipe symbol instead of slashes or dots."}
  DelimitedIdent [qualifiers #_(t/seq (t/and string? (t/not (fn1 contains? \|))))]
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (tagged-literal '| (symbol (str/join "|" qualifiers)))))

(defn delim-ident? [x] (instance? DelimitedIdent x))
