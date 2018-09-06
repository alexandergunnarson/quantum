(ns
  ^{:doc "Functions related to namespace access and manipulation."
    :attribution "alexandergunnarson"}
  quantum.core.ns
  (:refer-clojure :exclude
    [alias import loaded-libs ns ns-aliases ns-imports ns-interns ns-publics
     ns-refers ns-unalias refer refer-clojure remove-ns require use])
  (:require
    ;; TODO TYPED remove reference to `clojure.core`
    [clojure.core              :as core]
    [quantum.core.type         :as t
      :refer [defnt]]
    ;; TODO TYPED remove reference to `quantum.untyped.core.ns`
    [quantum.untyped.core.ns   :as uns]
    ;; TODO TYPED remove reference to `quantum.untyped.core.vars`
    [quantum.untyped.core.vars :as uvar
      :refer [defalias]]))

(def namespace? (t/isa? clojure.lang.Namespace))

;; TODO TYPED
(defalias core/ns)

#?(:clj
(defnt >?ns
  "Supersedes `clojure.core/find-ns`."
  [x t/symbol? > (t/? namespace?)] (clojure.lang.Namespace/find x)))

#?(:clj
(defnt >ns
  "Supersedes `clojure.core/the-ns`."
  ([x namespace? > namespace?] x)
  ([x t/symbol? > (t/* namespace?)] (>?ns x))))

;; TODO TYPED finish `t/assume`, `t/of`, `t/unqualified-symbol?`
#_(:clj
(defnt ns>var-map
  "Outputs a map of all the symbol->var mappings for the namespace.
   Supersedes `clojure.core/ns-map`."
  [x namespace? > (t/assume (t/of t/+map? t/unqualified-symbol? t/var?))]
  (.getMappings x)))

;; TODO TYPED finish `t/unqualified-symbol?`
#_(:clj
(defnt unmap!
  "Removes the var mapping for the symbol from the namespace and outputs the namespace.
   Supersedes `clojure.core/ns-unmap`."
  [ns-val namespace?, sym t/unqualified-symbol? > namespace?]
  (.unmap ns-val sym)
  ns-val))

(def in in-ns)

;; TODO TYPED finish `t/of`, `t/assume`
#_(:clj
(defnt all
  "Returns a sequence of all namespaces."
  [> (t/assume (t/of t/seq? namespace?))] (clojure.lang.Namespace/all)))

(defnt create!
  "Creates a new namespace named by the symbol if one doesn't already exist. Returns it or the
   already-existing namespace of the same name.

   Supersedes `clojure.core/create-ns`."
  [x t/symbol? > (t/* namespace?)] (clojure.lang.Namespace/findOrCreate x))

;; TODO TYPED
(defaliases uns
  alias alias!
  ns-unalias ns-unalias!
  ns-aliases
  require require!
  import import!
  ns-imports
  use use!
  ns-interns
  ns-publics
  refer refer!
  refer-clojure refer-clojure!
  ns-refers
  remove-ns remove-ns!
  ;;
  the-alias ns>alias ns-name>alias
  clear-ns-interns! search-var ns-exclude
  with-ns with-temp-ns
  import-static load-ns load-nss
  loaded-libs load-lib! load-package! load-dep!
  assert-ns-aliased)

;; TODO type and enable
#_(:clj
(defn alias-ns
  "Create vars in the current namespace to alias each of the public vars in
  the supplied namespace.
  Takes a symbol."
  {:attribution "flatland.useful.ns"}
  [ns-name-]
  (require ns-name-)
  (doseq [[name var] (ns-publics (the-ns ns-name-))]
    (uvar/alias-var name var))))
