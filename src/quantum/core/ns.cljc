(ns
  ^{:doc "Functions related to namespace access and manipulation."
    :attribution "alexandergunnarson"}
  quantum.core.ns
  (:refer-clojure :exclude
    [ns loaded-libs])
  (:require
    ;; TODO TYPED remove reference to `clojure.core`
    [clojure.core              :as core]
    [quantum.core.type         :as t
      :refer [defnt]]
    ;; TODO TYPED remove reference to `quantum.untyped.core.ns`
    [quantum.untyped.core.ns   :as uns]
    ;; TODO TYPED remove reference to `quantum.untyped.core.vars`
    [quantum.untyped.core.vars :as uvar
      :refer [defalias defaliases]]))

#?(:clj (def namespace? (t/isa? clojure.lang.Namespace)))

;; TODO TYPED
#?(:clj (defalias core/ns))

#?(:clj
(defnt >?ns
  "Supersedes `clojure.core/find-ns`."
  [x t/symbol? > (t/? namespace?)] (clojure.lang.Namespace/find x)))

#?(:clj
(defnt >ns
  "Supersedes `clojure.core/the-ns`."
  ([x namespace? > namespace?] x)
  ([x t/symbol? > (t/* namespace?)] (>?ns x))))

;; TODO TYPED finish `t/unqualified-symbol?`
#_(:clj
(defnt unmap!
  "Removes the mapping for the symbol from the namespace and outputs the namespace.

   Supersedes `clojure.core/ns-unmap`."
  [ns-val namespace?, sym t/unqualified-symbol? > namespace?]
  (.unmap ns-val sym)
  ns-val))

#?(:clj (def in in-ns))

;; TODO TYPED finish `t/of`
#_(:clj
(defnt all
  "Returns a sequence of all namespaces."
  [> (t/assume (t/of t/seq? namespace?))] (clojure.lang.Namespace/all)))

;; ===== Creation/Destruction ===== ;;

#?(:clj
(defnt create!
  "Creates a new namespace named by the symbol if one doesn't already exist. Returns it or the
   already-existing namespace of the same name.

   Supersedes `clojure.core/create-ns`."
  [x t/symbol? > (t/* namespace?)] (clojure.lang.Namespace/findOrCreate x)))

#?(:clj
(defnt remove!
  "Removes the namespace named by the symbol. Use with caution. Cannot be used to remove the
   `clojure.core` namespace."
  [x t/symbol? > (t/* namespace?)] (clojure.lang.Namespace/remove x)))

;; ===== Modification ===== ;;

#?(:clj
(defnt alias!
  "Add an alias to another namespace in the destination namespace. Returns the destination
   namespace. This corresponds roughly to the `:as` directive in the ns macro.

   Supersedes `clojure.core/alias`."
  [dest-ns namespace?, alias-sym t/symbol?, ns-to-alias namespace?]
  (.addAlias dest-ns alias-sym ns-to-alias)
  dest-ns))

#?(:clj
(defnt unalias!
  "Removes the alias as designated by `alias-sym` from the namespace."
  [ns-val namespace?, alias-sym t/symbol?]
  (.removeAlias ns-val alias-sym)
  ns-val))

;; TODO TYPED
#?(:clj (defalias require! core/require))

;; TODO TYPED
#?(:clj (defalias import! core/import))

;; TODO TYPED
#?(:clj (defalias refer! core/refer))

;; TODO TYPED
#?(:clj (defalias refer-clojure! core/refer-clojure))

;; ===== Mappings ===== ;;

;; TODO TYPED finish `t/of`, `t/unqualified-symbol?`
#_(:clj
(defnt ns>mappings
  "Supersedes `clojure.core/ns-map`."
  [x namespace? > (t/assume (t/of ut/+map? t/unqualified-symbol? (t/or t/var? t/class?)))]
  (.getMappings x)))

;; TODO TYPED finish `t/of`, `t/unqualified-symbol?`
#_(:clj
(defnt ns>alias-map
  "Outputs the alias->namespace mappings for the namespace.

   Supersedes `clojure.core/ns-aliases`."
  [x namespace? > (t/assume (t/of ut/+map? t/unqualified-symbol? namespace?))]
  (.getAliases x)))

;; TODO TYPED finish `t/of`, `t/unqualified-symbol?`, decide on `filter-vals'`?
#_(:clj
(defnt ns>imports
  "Outputs the import-mappings for the namespace.

   Supersedes `clojure.core/ns-imports`."
  [x namespace? > (t/assume (t/of ut/+map? t/unqualified-symbol? t/class?))]
  (->> x (filter-vals' t/class?))))

;; TODO TYPED finish `t/of`, `t/unqualified-symbol?`, decide on `filter-vals'`?
#_(:clj
(defnt ns>interns
  "Outputs the intern-mappings for the namespace.

   Supersedes `clojure.core/ns-interns`."
  [ns-val namespace? > (t/assume (t/of ut/+map? t/unqualified-symbol? t/var?))]
  (->> ns-val
       ns>mappings
       (filter-vals' (fn [^clojure.lang.Var v] (and (t/var? v) (= ns-val (.ns v))))))))

;; TODO TYPED finish `t/of`, `t/unqualified-symbol?`, decide on `filter-vals'`?
#_(:clj
(defnt ns>publics
  "Outputs the public intern-mappings for the namespace.

   Supersedes `clojure.core/ns-publics`."
  [ns-val namespace? > (t/assume (t/of ut/+map? t/unqualified-symbol? t/var?))]
  (->> ns-val
       ns>interns
       (filter-vals' (fn [^clojure.lang.Var v] (.isPublic v))))))

;; TODO TYPED finish `t/of`, `t/unqualified-symbol?`, decide on `remove-vals'`?
#_(:clj
(defnt ns>refers
  "Outputs the refer-mappings for the namespace.

   Supersedes `clojure.core/ns-refers`."
  [ns-val namespace? > (t/assume (t/of ut/+map? t/unqualified-symbol? t/var?))]
  (->> ns-val
       ns>mappings
       (remove-vals' (fn [^clojure.lang.Var v] (and (t/var? v) (= ns-val (.ns v))))))))

#?(:clj
(defnt alias>?ns [src-ns namespace?, sym t/symbol? > (t/? namespace?)] (.lookupAlias src-ns sym)))

;; TODO TYPED
#?(:clj
(defaliases uns
  ns>alias ns-name>alias clear-ns-interns! search-var ns-exclude with-ns with-temp-ns import-static
  load-ns load-nss loaded-libs load-lib! load-package! load-dep! assert-ns-aliased))

;; TODO TYPED — enable
#_(:clj
(defn alias-ns
  "Create vars in the current namespace to alias each of the public vars in
  the supplied namespace.
  Takes a symbol."
  {:attribution "flatland.useful.ns"}
  [ns-name-]
  (require ns-name-)
  (doseq [[name var] (ns>publics (the-ns ns-name-))]
    (uvar/alias-var name var))))
