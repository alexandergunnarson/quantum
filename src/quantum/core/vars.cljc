(ns quantum.core.vars
  "Var- and namespace-related functions."
  (:refer-clojure :exclude
    [defonce, intern, binding with-local-vars, meta, reset-meta!])
  (:require [clojure.core                 :as c]
            [quantum.core.macros.core     :as cmacros
              :refer [case-env]]
    #?(:clj [quantum.core.ns              :as ns])
            [quantum.core.untyped.qualify :as qual])
#?(:cljs
  (:require-macros
    [quantum.core.vars :as this])))

; ===== META ===== ;

(def reset-meta! c/reset-meta!)
(def meta c/meta)

(def update-meta vary-meta)

(defn merge-meta
  "See also `cljs.tools.reader/merge-meta`."
  [x m] (update-meta x merge m))

(defn merge-meta-from   [to from] (update-meta to merge (meta from)))
(defn replace-meta-from [to from] (with-meta to (meta from)))

; ===== DECLARATION/INTERNING ===== ;

#?(:clj (cmacros/defalias defalias cmacros/defalias))

#?(:clj (defalias intern c/intern))

#?(:clj
(defmacro defaliases'
  "|defalias|es multiple vars @names in the given namespace @ns."
  [ns- & names]
  `(do ~@(for [name- names]
           `(defalias ~name- ~(symbol (name ns-) (name name-)))))))

#?(:clj
(defmacro defaliases
  "|defalias|es multiple vars @names in the given namespace alias @alias."
  [alias- & names]
  (let [ns-sym (if-let [resolved (get (ns-aliases *ns*) alias-)]
                 (ns-name resolved)
                 alias-)]
    `(defaliases' ~ns-sym ~@names))))

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
          (str "Alias of " (-> var-0 meta :name))}
        (meta var-0)
        (meta sym)))
    (when (.hasRoot ^clojure.lang.Var var-0) [@var-0]))))

(comment
  "What to do when aliasing a macro:"
  ;(def cljs-doseqi (var loops/doseqi)) ; doesn't work because not a var in CLJS
  ;(def cljs-doseqi (mfn loops/doseqi)) ; doesn't work because no |eval| in CLJS
  ;(defalias doseqi #?(:clj loops/doseqi :cljs cljs-doseqi))
  ; #?(:clj (alter-meta! (var doseqi) assoc :macro true)) ; Sometimes this works

  #_(:clj (defmacro doseqi [& args] `(loops/doseqi ~@args))))

#?(:clj (quantum.core.macros.core/defmalias defmalias quantum.core.macros.core/defmalias))

#?(:clj
(defmacro defonce
  "Like `clojure.core/defonce` but supports optional docstring and attributes
   map for name symbol."
  [name & sigs]
  (let [[name [expr]] (cmacros/name-with-attrs name sigs)]
    `(c/defonce ~name ~expr))))

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

; ============ MANIPULATION + OTHER ============

; CLJS compatible only if you port |alter-var-root| as in-ns, def, in-ns
#?(:clj
(defn reset-var!
  "Like |reset!| but for vars."
  {:attribution "alexandergunnarson"}
  [var-0 val-f]
  ;(.bindRoot #'clojure.core/ns ns+)
  ;(alter-meta! #'clojure.core/ns merge (meta #'ns+))
  (alter-var-root var-0 (fn [_] val-f))))

; CLJS compatible
#?(:clj
(defn swap-var!
  "Like |swap!| but for vars."
  {:attribution "alexandergunnarson"}
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
  {:attribution "alexandergunnarson"}
  [& vars]
  (doseq [v vars]
    (reset-var! v nil))))

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
  {:attribution "alexandergunnarson"
   :usage '(defs 'a 1 'b 2 'c 3)}
  [& {:as vars}]
  (doseq [[sym v] vars]
    (intern *ns* sym v))))

#?(:clj
(defn defs-
  "Like |defs|, but each var defined is private."
  {:attribution "alexandergunnarson"
   :usage '(defs- 'a 1 'b 2 'c 3)}
  [& {:as vars}]
  (doseq [[sym v] vars]
    (intern *ns* (-> sym (with-meta {:private true})) v))))

; ===== THREAD-LOCAL ===== ;

#?(:clj (defalias binding         c/binding))
#?(:clj (defalias with-local-vars c/with-local-vars))

#?(:clj
(defmacro def
  ([sym]              `(~'def ~sym))
  ([sym v]            `(~'def ~sym ~v))
  ([sym doc-or-meta v]
    (if (string? doc-or-meta)
        `(~'def ~(with-meta sym {:doc doc-or-meta}) ~v)
        `(~'def ~(with-meta sym doc-or-meta) ~v)))
  ([sym -doc -meta v] `(~'def ~(with-meta sym (merge -meta {:doc -doc})) ~v))))
