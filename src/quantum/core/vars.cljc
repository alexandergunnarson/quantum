(ns quantum.core.vars
  "Var- and namespace-related functions."
  (:refer-clojure :exclude
    [defonce, intern, binding with-local-vars, meta, reset-meta!])
  (:require [clojure.core                 :as c]
    #?(:clj [quantum.core.ns              :as ns])
            [quantum.untyped.core.form.evaluate
              :refer [case-env]]
            [quantum.untyped.core.qualify :as qual]
            [quantum.untyped.core.vars    :as u])
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

(def merge-meta-from   u/merge-meta-from)
(def replace-meta-from u/replace-meta-from)

; ===== DECLARATION/INTERNING ===== ;

#?(:clj (u/defalias u/defalias))

#?(:clj (defalias intern c/intern))
#?(:clj (u/defaliases u defaliases defaliases'))

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

#?(:clj (quantum.untyped.core.vars/defmalias defmalias quantum.untyped.core.vars/defmalias))

#?(:clj (defaliases u defonce def- defmacro-))
; ============ MANIPULATION + OTHER ============

#?(:clj
(defn reset-var!
  "Like `reset!` but for vars."
  {:attribution "alexandergunnarson"}
  [var-0 val-f]
  ;(.bindRoot #'clojure.core/ns ns+)
  ;(alter-meta! #'clojure.core/ns merge (meta #'ns+))
  (alter-var-root var-0 (fn [_] val-f))))

#?(:clj
(defn update-var!
  "Non-atomic var update"
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
  (doseq [v vars] (reset-var! v nil))))

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

#?(:clj (defalias u/def))
