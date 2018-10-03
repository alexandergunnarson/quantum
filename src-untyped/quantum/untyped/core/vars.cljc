(ns quantum.untyped.core.vars
         (:refer-clojure :exclude
           [defonce resolve])
         (:require
           [clojure.core                       :as core]
           [quantum.untyped.core.core          :as ucore]
           [quantum.untyped.core.logic
             :refer [ifs]]
           [quantum.untyped.core.form.evaluate
             :refer [case-env case-env*]]
           [quantum.untyped.core.form.generate :as ufgen])
#?(:cljs (:require-macros
           [quantum.untyped.core.vars          :as self])))

(ucore/log-this-ns)

#?(:clj  (defn unbound? [x] (instance? clojure.lang.Var$Unbound x)))
#?(:clj  (defn dynamic? [x] (.isDynamic ^clojure.lang.Var x)))
#?(:cljs (defn defined? [x] (not (undefined? x))))

;; ===== Metadata ===== ;;

(defn metable? [x]
  #?(:clj  (instance?  clojure.lang.IMeta x)
     :cljs (satisfies? cljs.core/IMeta    x)))

(defn with-metable? [x]
  #?(:clj  (instance?  clojure.lang.IObj   x)
     :cljs (satisfies? cljs.core/IWithMeta x)))

(def update-meta       ucore/update-meta)
(def merge-meta-from   ucore/merge-meta-from)
(def replace-meta-from ucore/replace-meta-from)

;; ===== Definitions ===== ;;

#?(:clj
(defmacro defonce
  "Like `clojure.core/defonce` but supports optional docstring and attributes
   map for name symbol."
  [name & sigs]
  (let [[name [expr]] (ufgen/name-with-attrs name sigs)]
    `(core/defonce ~name ~expr))))

#?(:clj
(defmacro def-
  "Like `def` but adds the ^:private metadatum to the bound var.
   `def-` : `def` :: `defn-` : `defn`"
  {:attribution "alexandergunnarson"}
  [sym v]
  `(doto (def ~sym ~v)
         (alter-meta! merge {:private true}))))

#?(:clj
(defmacro defmacro-
  "Same as defmacro but yields a private definition"
  {:note "This used to be in clojure.contrib.def (by Steve Gilardi),
          which has not been migrated to the new contrib collection."
   :from "clojure.algo.generic.math-functions"}
  [name & decls]
  (list* `defmacro (with-meta name (assoc (meta name) :private true)) decls)))

;; ===== Aliases ===== ;;

#?(:clj (ucore/defaliases ucore defalias defaliases defaliases'))

#?(:clj
(defmacro defmalias
  "Defines an cross-platform alias for a macro.

   In Clojure one can use `defalias` for this purpose without a problem, but
   in ClojureScript macros can't be used in a `defalias` context because `defalias`
   creates a ClojureScript (var) binding where a Clojure (macro) one is needed.

   Defaults to the same binding for both Clojure and ClojureScript."
  {:attribution 'alexandergunnarson
   :todo ["Handle more platforms (if necessary)"]}
  ([name orig-sym] `(defmalias ~name ~orig-sym ~orig-sym))
  ([name clj-sym cljs-sym]
    (let [args-sym   (gensym "args")
          orig-sym-f (gensym "orig-sym")]
     `(defmacro ~name [& ~args-sym]
        (let [~orig-sym-f (case-env* ~'&env :clj '~clj-sym :cljs '~cljs-sym)
              _# (when (= ~orig-sym-f 'nil)
                   (throw (IllegalArgumentException. (str "Macro '" '~name "' not defined."))))]
          (cons ~orig-sym-f ~args-sym)))))))

#?(:clj
(defmacro def
  "Like `clojure.core/def`, but allows for docstring and metadata placement
   like `defn`."
  ([sym]   `(~'def ~sym))
  ([sym v] `(~'def ~sym ~v))
  ([sym doc-or-meta v]
    (if (string? doc-or-meta)
        `(quantum.untyped.core.vars/def ~sym ~doc-or-meta nil          ~v)
        `(quantum.untyped.core.vars/def ~sym nil          ~doc-or-meta ~v)))
  ([sym -doc -meta v] `(~'def ~(with-meta sym (assoc -meta :doc -doc)) ~v))))

;; ===== Symbol Resolution ===== ;;

(defn resolve-ns
  "Resolves the namespace of a symbol, checking in aliases first.
   Totally distinct from `core/ns-resolve`."
  {:incorporated {'clojure.lang.Compiler/namespaceFor "10/3/2018"}}
  ([sym] (resolve-ns *ns* sym))
  ([ns-val #_namespace?, sym]
    (let [ns-sym (-> sym namespace symbol)]
      (or (.lookupAlias ^clojure.lang.Namespace ns-val ns-sym)
          (find-ns ns-sym)))))

(defn resolve
  "Combines `core/resolve` with `core/ns-resolve` and does not throw an exception when a class can't
   be resolved."
  {:incorporated {'clojure.core/resolve                 "10/3/2018"
                  'clojure.core/ns-resolve              "10/3/2018"
                  'clojure.lang.Compiler/maybeResolveIn "10/3/2018"}}
  ([sym] (resolve *ns* sym))
  ([ns-val #_namespace?, sym] (resolve ns-val nil sym))
  ([ns-val #_namespace?, env #_map?, sym]
    (if (contains? env sym)
        (get env sym)
        (if (some? (namespace sym))
            (when-let [sym-ns-val (resolve-ns sym)]
              (.findInternedVar ^clojure.lang.Namespace sym-ns-val (-> sym name symbol)))
            (let [^String sym-name (name sym)]
              (ifs (or (and (pos? (.indexOf sym-name "."))
                            (not (.endsWith sym-name ".")))
                       (= (.charAt sym-name 0) \[))
                     (try (clojure.lang.RT/classForName sym-name)
                       (catch ClassNotFoundException _ nil))
                   (= sym 'ns)
                     #'core/ns
                   (= sym 'in-ns)
                     #'core/in-ns
                   (.getMapping ^clojure.lang.Namespace ns-val sym)))))))
