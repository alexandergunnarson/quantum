(ns quantum.untyped.core.identification
  "Functions related to variable identification/naming (name, namespace, etc.) and
   qualification/unqualification of nameables."
      (:refer-clojure :exclude
        [ident? qualified-keyword? simple-symbol?])
      (:require
        [clojure.core              :as core]
#?(:clj [clojure.future            :as fcore])
        [clojure.string            :as str]
        [fipp.ednize]
        [quantum.untyped.core.core :as ucore]
        [quantum.untyped.core.ns   :as uns
 #?@(:clj [:refer [namespace?]])]
        [quantum.untyped.core.vars
          :refer [defalias]]))

(ucore/log-this-ns)

(defn named? [x]
  #?(:clj  (instance?   clojure.lang.Named x)
     :cljs (implements? cljs.core/INamed   x)))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/ident?)
                               `fcore/ident?
                               `core/ident?)))
   :cljs (defalias core/ident?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/qualified-keyword?)
                               `fcore/qualified-keyword?
                               `core/qualified-keyword?)))
   :cljs (defalias core/qualified-keyword?))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/simple-symbol?)
                               `fcore/simple-symbol?
                               `core/simple-symbol?)))
   :cljs (defalias core/simple-symbol?))

(defn ?ns->name [?ns]
  (name #?(:clj (if (namespace? ?ns)
                    (ns-name ?ns)
                    ?ns)
           :cljs ?ns)))

;; ===== Qualification ===== ;;

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
          (if-let [alias- (do #?(:clj (uns/ns-name>alias *ns* (symbol n)) :cljs false))]
            (str alias- (when extra-slash? "/"))
            n)))      (name sym)))))

;; ===== Idents ===== ;;

(defrecord
  ^{:doc "A delimited identifier.
          Defaults to delimiting all qualifiers by the pipe symbol instead of slashes or dots."}
  DelimitedIdent [qualifiers #_(t/seq (t/and string? (t/not (fn1 contains? \|))))]
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (tagged-literal '| (symbol (str/join "|" qualifiers)))))

(defn delim-ident? [x] (instance? DelimitedIdent x))
