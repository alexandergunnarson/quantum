(ns ^{:doc "Var- and namespace-related functions."}
  quantum.core.vars
           (:refer-clojure :exclude [defonce])
           (:require [quantum.core.macros.core :as cmacros
                       :refer [#?@(:clj [if-cljs when-cljs])]])
  #?(:cljs (:require-macros
                     [quantum.core.macros.core :as cmacros
                       :refer [if-cljs when-cljs]            ])))

; ============ DECLARATION ============

#?(:clj
(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  {:attribution "clojure.contrib.def/defalias"
   :contributors ["Alex Gunnarson"]}
  ([name orig]
     `(do
        (let [orig-var# (var ~orig)]
          (if true ; Can't have different clj-cljs things within macro...  ;#?(:clj (-> orig-var# .hasRoot) :cljs true)
              (do (def ~name (with-meta (-> ~orig var deref) (meta (var ~orig))))
                  ; for some reason, the :macro metadata doesn't really register unless you do it manually 
                  (when (-> orig-var# meta :macro true?)
                    (alter-meta! #'~name assoc :macro true)))
              (def ~name)))
        (var ~name)))
  ([name orig doc]
     (list `defalias (with-meta name (assoc (meta name) :doc doc)) orig))))

#?(:clj 
(defn var-name
  "Get the namespace-qualified name of a var."
  {:attribution "flatland.useful.ns"}
  [v]
  (apply symbol (map str ((juxt (comp ns-name :ns)
                                :name)
                          (meta v))))))

#?(:clj
(defn alias-var
  "Create a var with the supplied name in the current namespace, having the same
  metadata and root-binding as the supplied var."
  {:attribution "flatland.useful.ns"}
  [sym var-0]
  (apply intern *ns*
    (with-meta sym
      (merge
        {:dont-test
          (str "Alias of " (var-name var-0))}
        (meta var-0)
        (meta sym)))
    (when (.hasRoot ^clojure.lang.Var var-0) [@var-0]))))

#?(:clj (quantum.core.macros.core/defmalias defmalias quantum.core.macros.core/defmalias))

#?(:clj
(defmacro defonce
  "Like |clojure.core/defonce| but supports optional docstring and attributes
   map for name symbol."
  [name & sigs]
  (let [[name [expr]] (cmacros/name-with-attrs name sigs)]
    `(core/defonce ~name ~expr))))

#?(:clj
(defmacro def-
  "Like |def| but adds the ^:private metadatum to the bound var.
   |def-| : |def| :: |defn-| : |defn|"
  {:attribution "Alex Gunnarson"}
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

; ============ MANIPULATION + OTHER ============
; CLJS compatible only if you port |alter-var-root| as in-ns, def, in-ns
#?(:clj
(defn reset-var!
  "Like |reset!| but for vars."
  {:attribution "Alex Gunnarson"}
  [var-0 val-f]
  ;(.bindRoot #'clojure.core/ns ns+)
  ;(alter-meta! #'clojure.core/ns merge (meta #'ns+))
  (alter-var-root var-0 (constantly val-f))))

; CLJS compatible
#?(:clj
(defn swap-var!
  "Like |swap!| but for vars."
  {:attribution "Alex Gunnarson"}
  ([var-0 f]
  (do (alter-var-root var-0 f)
       var-0))
  ([var-0 f & args]
  (do (alter-var-root var-0
         (fn [var-n]
           (apply f var-n args)))
       var-0))))

#?(:clj
(defn clear-vars!
  "Sets each var in ~@vars to nil."
  {:attribution "Alex Gunnarson"}
  [& vars]
  (doseq [v vars]
    (reset-var! v nil))))

#?(:clj
 (defn var-name
   "Get the namespace-qualified name of a var."
   {:attribution "flatland.useful.ns"}
   [v]
   (apply symbol
     (map str
       ((juxt (comp ns-name :ns)
              :name)
              (meta v))))))

#?(:clj
(defn alias-var
  "Create a var with the supplied name in the current namespace, having the same
  metadata and root-binding as the supplied var."
  {:attribution "flatland.useful.ns"}
  [sym var-0]
  (apply intern *ns*
    (with-meta sym
      (merge
        {:dont-test
          (str "Alias of " (var-name var-0))}
        (meta var-0)
        (meta sym)))
    (when (.hasRoot ^clojure.lang.Var var-0) [@var-0]))))

#?(:clj
(defn alias-ns
  "Create vars in the current namespace to alias each of the public vars in
  the supplied namespace.
  Takes a symbol."
  {:attribution "flatland.useful.ns"}
  [ns-name-]
  (require ns-name-)
  (doseq [[name var] (ns-publics (the-ns ns-name-))]
    (alias-var name var))))


#?(:clj
(defn defs
  "Defines a provided list of symbol-value pairs as vars in the
   current namespace."
  {:attribution "Alex Gunnarson"
   :usage '(defs 'a 1 'b 2 'c 3)}
  [& {:as vars}]
  (doseq [[sym v] vars]
    (intern *ns* sym v))))

#?(:clj
(defn defs-
  "Like |defs|, but each var defined is private."
  {:attribution "Alex Gunnarson"
   :usage '(defs-private 'a 1 'b 2 'c 3)}
  [& {:as vars}]
  (doseq [[sym v] vars]
    (intern *ns* (-> sym (with-meta {:private true})) v))))

#?(:clj
(defn namespace-exists?
  {:todo ["There has to be a better way to do this"]}
  [ns-sym]
  (try (require ns-sym)
       true
    (catch java.io.FileNotFoundException e
      false))))

(defn unqualify [sym] (-> sym name symbol))