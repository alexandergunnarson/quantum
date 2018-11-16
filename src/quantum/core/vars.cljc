(ns quantum.core.vars
  "Functions related to vars and their enclosing namespaces.

   We colocate namespace and var functions because namespaces cannot exist separately from vars:
   for instance, namespace-mapping values are either vars or classes, and vars exist only in the
   context of an enclosing namespace."
  (:refer-clojure :exclude
    [binding defonce intern loaded-libs ns var? with-local-vars])
  (:require
    ;; TODO TYPED remove reference to `clojure.core`
    [clojure.core                  :as core]
    [quantum.core.data.identifiers :as id]
    [quantum.core.data.meta        :as dm
      :refer [>meta]]
    [quantum.core.type             :as t]
    ;; TODO TYPED remove reference to `quantum.untyped.core.ns`
    [quantum.untyped.core.ns       :as uns]
    ;; TODO TYPED remove reference to `quantum.untyped.core.vars`
    [quantum.untyped.core.vars     :as uvar]))

;; ===== Namespaces ===== ;;

#?(:clj (def namespace? (t/isa? clojure.lang.Namespace)))

;; TODO TYPED
#?(:clj (defalias core/ns))

#?(:clj
(t/defn >?ns
  "Supersedes `clojure.core/find-ns`."
  [x id/symbol? > (t/? namespace?)] (clojure.lang.Namespace/find x)))

#?(:clj
(t/defn >ns
  "Supersedes `clojure.core/the-ns`."
  ([x namespace? > namespace?] x)
  ([x id/symbol? > (t/run namespace?)] (>?ns x))))

#?(:clj (t/extend-defn! id/>name (^:inline [x namespace?] (-> x .getName id/>name))))

;; TODO TYPED finish `id/unqualified-symbol?`
#_(:clj
(t/defn unmap!
  "Removes the mapping for the symbol from the namespace and outputs the namespace.

   Supersedes `clojure.core/ns-unmap`."
  [ns-val namespace?, sym id/unqualified-symbol? > namespace?]
  (.unmap ns-val sym)
  ns-val))

;; `in-ns` cannot be shadowed
#?(:clj (def in-ns in-ns))

;; TODO TYPED finish `t/of`
#_(:clj
(t/defn all-ns
  "Returns a `traversable?` of all namespaces."
  [> (t/assume (t/of namespace?))] (clojure.lang.Namespace/all)))

;; ===== Creation/Destruction ===== ;;

#?(:clj
(t/defn create-ns!
  "Creates a new namespace named by the symbol if one doesn't already exist. Returns it or the
   already-existing namespace of the same name.

   Supersedes `clojure.core/create-ns`."
  [x id/symbol? > (t/run namespace?)] (clojure.lang.Namespace/findOrCreate x)))

#?(:clj
(t/defn remove-ns!
  "Removes the namespace named by the symbol. Use with caution. Cannot be used to remove the
   `clojure.core` namespace."
  [x id/symbol? > (t/run namespace?)] (clojure.lang.Namespace/remove x)))

;; ===== Modification ===== ;;

#?(:clj
(t/defn alias!
  "Add an alias to another namespace in the destination namespace. Returns the destination
   namespace. This corresponds roughly to the `:as` directive in the ns macro.

   Supersedes `clojure.core/alias`."
  [dest-ns namespace?, alias-sym id/symbol?, ns-to-alias namespace?]
  (.addAlias dest-ns alias-sym ns-to-alias)
  dest-ns))

#?(:clj
(t/defn unalias!
  "Removes the alias as designated by `alias-sym` from the namespace."
  [ns-val namespace?, alias-sym id/symbol?]
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

;; ===== Vars ===== ;;

(def var? (t/isa? #?(:clj clojure.lang.Var :cljs cljs.core/Var)))

;; TODO maybe extend to CLJS?
#?(:clj (t/defn var-defined? [x var?] (.hasRoot x)))

;; TODO maybe extend to CLJS?
#?(:clj (t/defn dynamic? [x var?] (.isDynamic x)))

#?(:clj (t/extend-defn! id/>name      (^:inline [x var?] (-> x >meta :name id/>name))))
#?(:clj (t/extend-defn! id/>namespace (^:inline [x var?] (-> x >meta :ns   id/>name))))
#?(:clj (t/extend-defn! id/>symbol    (^:inline [x var?]
                                        (id/>symbol (id/>namespace x) (id/>name x)))))

;; ---- Var declaration/interning ----- ;;

#?(:clj
(t/defn intern
  "Finds or creates a var named by the symbol name in ->`ns-val`, setting its root binding to ->`v`
   if supplied. The namespace must exist. The var will adopt any metadata from ->`name-val`.
   Returns the var."
  > var?
  ([ns-val (t/or id/symbol? namespace?), var-name id/symbol? > (t/run var?)]
    (let [var-ref (clojure.lang.Var/intern (>ns ns-val) var-name)]
      (when (>meta var-name) (.setMeta var-ref (>meta var-name)))
      var-ref))
  ([ns-val (t/or id/symbol? namespace?), var-name id/symbol?, var-val t/ref? > (t/run var?)]
    (let [var-ref (clojure.lang.Var/intern (>ns ns-val) var-name var-val)]
      (when (>meta var-name) (.setMeta var-ref (>meta var-name)))
      var-ref))))

;; TODO TYPED
;; Note that `def` can never be shadowed
#?(:clj (uvar/defalias uvar/def))

;; TODO TYPED
#?(:clj (uvar/defaliases uvar defalias defaliases defaliases'))

;; TODO TYPED — need to do `apply`, and `apply` with t/defn; also `merge`, `str`, `deref`
#_(:clj
(t/defn alias-var
  "Create a var with the supplied name in the current namespace, having the same metadata and
   root-binding as the supplied var."
  {:attribution  "flatland.useful.ns"
   :contributors ["Alex Gunnarson"]}
  [sym id/symbol?, var-val var?]
  (apply intern *ns*
    (dm/with-meta sym
      (merge
        {:dont-test
          (str "Alias of " (-> var-val >meta :name))}
        (>meta var-0)
        (>meta sym)))
    (when (defined? var-) [(deref var-val)]))))

;; TODO TYPED
#?(:clj (quantum.untyped.core.vars/defmalias defmalias quantum.untyped.core.vars/defmalias))

;; TODO TYPED
#?(:clj (defaliases uvar defonce def- defmacro-))

;; ----- Var modification ----- ;;

;; TODO TYPED — need to do `fnt`
#_(:clj
(t/defn reset-var!
  "Like `reset!` but for vars. Atomically sets the root binding of ->`var-` to ->`v`."
  {:attribution "alexandergunnarson"}
  [var-val var?, v t/ref? > var?]
  (.alterRoot var-val (t/fn [_] v))))

;; TODO TYPED — need to do `fnt`, `apply`
#_(:clj
(t/defn update-var!
  {:attribution "alexandergunnarson"}
  ([var- var?, f (t/fn [_]) > var?]
    (do (.alterRoot var- f)
        var-))
  ;; TODO we need to be able to conditionalize `f`'s arity based on the count of `args`
  ([var- f t/fn? & args (? t/seq?) > var?]
    (do (.alterRoot var- (t/fn [v' _] (apply f v' args)))
        var-))))

;; TODO TYPED — `doseq`
#_(:clj
(t/defn clear-vars!
  "Sets each var in ->`vars` to nil."
  {:attribution "alexandergunnarson"}
  [& vars (? (t/seq-of var?))]
  (doseq [v vars] (reset-var! v nil))))

;; ----- Thread-local ----- ;;

;; TODO TYPED
#?(:clj (defalias binding         core/binding))
;; TODO TYPED
#?(:clj (defalias with-local-vars core/with-local-vars))

;; ----- Mappings ----- ;;

;; TODO TYPED finish `t/of`, `id/unqualified-symbol?`
#_(:clj
(t/defn ns>mappings
  "Supersedes `clojure.core/ns-map`."
  [x namespace? > (t/assume (t/of ut/+map? id/unqualified-symbol? (t/or var? t/class?)))]
  (.getMappings x)))

;; TODO TYPED finish `t/of`, `id/unqualified-symbol?`
#_(:clj
(t/defn ns>alias-map
  "Outputs the alias->namespace mappings for the namespace.

   Supersedes `clojure.core/ns-aliases`."
  [x namespace? > (t/assume (t/of ut/+map? id/unqualified-symbol? namespace?))]
  (.getAliases x)))

;; TODO TYPED finish `t/of`, `id/unqualified-symbol?`, decide on `filter-vals'`?
#_(:clj
(t/defn ns>imports
  "Outputs the import-mappings for the namespace.

   Supersedes `clojure.core/ns-imports`."
  [x namespace? > (t/assume (t/of ut/+map? id/unqualified-symbol? t/class?))]
  (->> x (filter-vals' t/class?))))

;; TODO TYPED finish `t/of`, `id/unqualified-symbol?`, decide on `filter-vals'`?
#_(:clj
(t/defn ns>interns
  "Outputs the intern-mappings for the namespace.

   Supersedes `clojure.core/ns-interns`."
  [ns-val namespace? > (t/assume (t/of ut/+map? id/unqualified-symbol? var?))]
  (->> ns-val
       ns>mappings
       (filter-vals' (fn [^clojure.lang.Var v] (and (var? v) (= ns-val (.ns v))))))))

;; TODO TYPED finish `t/of`, `id/unqualified-symbol?`, decide on `filter-vals'`?
#_(:clj
(t/defn ns>publics
  "Outputs the public intern-mappings for the namespace.

   Supersedes `clojure.core/ns-publics`."
  [ns-val namespace? > (t/assume (t/of ut/+map? id/unqualified-symbol? var?))]
  (->> ns-val
       ns>interns
       (filter-vals' (fn [^clojure.lang.Var v] (.isPublic v))))))

;; TODO TYPED finish `t/of`, `id/unqualified-symbol?`, decide on `remove-vals'`?
#_(:clj
(t/defn ns>refers
  "Outputs the refer-mappings for the namespace.

   Supersedes `clojure.core/ns-refers`."
  [ns-val namespace? > (t/assume (t/of ut/+map? id/unqualified-symbol? var?))]
  (->> ns-val
       ns>mappings
       (remove-vals' (fn [^clojure.lang.Var v] (and (var? v) (= ns-val (.ns v))))))))

#?(:clj
(t/defn alias>?ns [src-ns namespace?, sym id/symbol? > (t/? namespace?)] (.lookupAlias src-ns sym)))

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

;; TODO TYPED
#?(:clj
(defaliases uns
  ns>alias ns-name>alias clear-ns-interns! search-var ns-exclude with-ns with-temp-ns import-static
  load-ns load-nss loaded-libs load-lib! load-package! load-dep! assert-ns-aliased ?resolve intern! intern-once!))
