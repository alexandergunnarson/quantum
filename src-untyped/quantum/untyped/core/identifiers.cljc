(ns quantum.untyped.core.identifiers
  "Functions related to variable identifiers/names (`name`, `namespace`, etc.) and
   qualification/unqualification of nameables."
      (:refer-clojure :exclude
        [ident? qualified-keyword? simple-symbol?])
      (:require
        [clojure.core               :as core]
#?(:clj [clojure.future             :as fcore])
        [clojure.string             :as str]
        [fipp.ednize]
        [quantum.untyped.core.core  :as ucore]
        [quantum.untyped.core.error :as uerr]
        [quantum.untyped.core.fn    :as ufn]
        [quantum.untyped.core.ns    :as uns
#?@(:clj [:refer [namespace?]])]
        [quantum.untyped.core.vars
          :refer [defalias]]))

(ucore/log-this-ns)

;; ===== Nameability ===== ;;

(defn named? [x]
  #?(:clj  (instance?   clojure.lang.Named x)
     :cljs (implements? cljs.core/INamed   x)))

(defn demunged>namespace [^String s] (subs s 0 (.lastIndexOf s "/")))
(defn demunged>name      [^String s] (subs s (inc (.lastIndexOf s "/"))))

(defn ?ns->name [?ns]
  (name #?(:clj (if (namespace? ?ns)
                    (ns-name ?ns)
                    ?ns)
           :cljs ?ns)))

(defn >name
  "Computes the name (the unqualified string identifier) of `x`."
  [x]
  (cond   (named?     x) (name x)
          (string?    x) x
#?@(:clj [(class?     x) (.getName ^Class x)
          (namespace? x) (-> x ns-name name)
          (var?       x) (-> x meta :name name)])
          (fn?        x) #?(:clj  (or (some-> (-> x meta :name) >name)
                                      (-> x class .getName clojure.lang.Compiler/demunge demunged>name))
                            :cljs (when-not (-> x .-name str/blank?)
                                    (-> x .-name demunge-str demunged>name)))
          :else          (uerr/not-supported! `>name x)))

(def >?name (ufn/? >name))

(defn >?namespace
  "Computes the nilable namespace (the string identifier-qualifier) of `x`."
  [x]
  (cond   (named?         x)   (namespace x)
          (or (nil?       x)
              (string?    x)
      #?(:clj (class?     x))
      #?(:clj (namespace? x))) nil
#?@(:clj [(var?           x)   (-> x meta :ns >name)])
          (fn?            x)   #?(:clj  (or (some-> (-> x meta :ns) >name)
                                            (-> x class .getName clojure.lang.Compiler/demunge demunged>namespace))
                                  :cljs (when-not (-> x .-name str/blank?)
                                          (-> x .-name demunge-str demunged>namespace)))
          :else                (uerr/not-supported! `>?namespace x)))

;; ===== Qualification ===== ;;

(defn qualify
  #?(:clj ([sym] (qualify *ns* sym)))
          ([ns-or-sym sym]
            (let [qualifier (cond (symbol? ns-or-sym)    (-> ns-or-sym         name)
                                  (namespace? ns-or-sym) (-> ns-or-sym ns-name name)
                                  :else                  (uerr/not-supported! `qualify ns-or-sym))]
              (symbol qualifier (name sym)))))

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

;; ===== Standard identifiers ===== ;;

#?(:clj  (eval `(defalias ~(if (resolve `fcore/qualified-keyword?)
                               `fcore/qualified-keyword?
                               `core/qualified-keyword?)))
   :cljs (defalias core/qualified-keyword?))

(defn >keyword [x]
  (cond (keyword? x) x
        (symbol?  x) (keyword (>?namespace x) (>name x))
        :else        (-> x str keyword)))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/simple-symbol?)
                               `fcore/simple-symbol?
                               `core/simple-symbol?)))
   :cljs (defalias core/simple-symbol?))

(defn >symbol
  "Converts `x` to a symbol (possibly qualified, meta-able identifier)."
  [x]
  (cond   (symbol?    x) x
          (string?    x) (symbol x)
          (or (keyword? x) #?(:clj (var? x)))
            (symbol (>?namespace x) (>name x))
#?@(:clj [(class?     x) (-> x >name symbol)
          (namespace? x) (ns-name x)])
          (fn? x)        #?(:clj  (or (when-let [ns- (-> x meta :ns)]
                                        (symbol (>name ns-) (-> x meta :name >name)))
                                      (-> x class .getName clojure.lang.Compiler/demunge recur))
                            :cljs (when-not (-> x .-name str/blank?)
                                    (-> x .-name demunge-str recur)))
          :else          (-> x str recur)))

#?(:clj  (eval `(defalias ~(if (resolve `fcore/ident?)
                               `fcore/ident?
                               `core/ident?)))
   :cljs (defalias core/ident?))

;; ===== UUIDs ===== ;;

(defn >uuid []
  #?(:clj  (java.util.UUID/randomUUID)
     :cljs (cljs.core/random-uuid)))

;; ===== Delimited identifiers  ===== ;;

(defrecord
  ^{:doc "A delimited identifier.
          Defaults to delimiting all qualifiers by the pipe symbol instead of slashes or dots."}
  DelimitedIdent [qualifiers #_(t/seq (t/and string? (t/not (fn1 contains? \|))))]
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (tagged-literal '| (symbol (str/join "|" qualifiers)))))

(defn delim-ident? [x] (instance? DelimitedIdent x))

(defn >delim-ident
  "Computes the delimited identifier of `x`."
  [x]
  (cond   (delim-ident? x) x
          (string?      x) (-> x (str/split #"\.|\||/") (DelimitedIdent.))
          (named?       x) (DelimitedIdent.
                             (concat (some-> (namespace x) (str/split #"\.|\||/"))
                                     (-> x >name (str/split #"\.|\||/"))))
#?@(:clj [(class?       x) (>delim-ident (.getName ^Class x))
          (namespace?   x) (-> x >name >delim-ident)
          (var?         x) (DelimitedIdent.
                             (concat (-> x >?namespace (str/split #"\.|\||/"))
                                     (-> x >name       (str/split #"\.|\||/"))))])
          (fn?          x) (DelimitedIdent.
                             #?(:clj  (or (some-> (-> x meta :name) >name (str/split #"\.|\||/"))
                                          (-> x class .getName clojure.lang.Compiler/demunge (str/split #"\.|\||/")))
                                :cljs (if (-> x .-name str/blank?)
                                          ["<anonymous>"]
                                          (-> x .-name demunge-str (str/split #"\.|\||/")))))
          (nil?         x) (uerr/not-supported! `>delim-ident x)
          :else            (-> x str recur)))
