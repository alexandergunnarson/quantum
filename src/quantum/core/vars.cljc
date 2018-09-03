(ns quantum.core.vars
  "Var- and namespace-related functions."
  (:refer-clojure :exclude
    [defonce, intern, binding with-local-vars, meta, reset-meta!])
  (:require [clojure.core                 :as c]
    #?(:clj [quantum.core.ns              :as ns])
            [quantum.core.type            :as t
              :refer [?]]
            [quantum.untyped.core.defnt
              :refer [defnt fnt]]
            [quantum.untyped.core.form.evaluate
              :refer [case-env]]
            [quantum.untyped.core.vars    :as u])
#?(:cljs
  (:require-macros
    [quantum.core.vars :as this])))

;; ===== Meta ===== ;;

(def #_t/def meta? (? t/+map?))

(defnt meta
  "Returns the metadata of `x`, returns nil if there is no metadata."
  [x t/metable? > meta?] (.meta x))

(defnt with-meta
  "Returns an object of the same type and value as `x`, with map `meta-` as its metadata."
  [x t/with-metable?, meta- meta? > (t/spec-of meta-)] (.withMeta x meta-))

(defnt reset-meta!
  "Atomically resets the metadata for a namespace/var/ref/agent/atom"
  [iref (t/isa? #?(:clj clojure.lang.IReference :cljs (TODO))) meta- meta? > (t/spec-of meta-)]
  (.resetMeta iref meta-))

(defnt update-meta
 "Returns an object of the same type and value as `x`, with `(apply f (meta x) args)` as its
  metadata."
 ;; TODO `f` should more specifically be able to handle the args arity and specs
 [x (t/and t/with-metable? t/metable?) f t/fn? & args]
 (with-meta x (apply f (meta x) args)))

(defnt merge-meta
  "See also `cljs.tools.reader/merge-meta`."
  [x (t/and t/with-metable? t/metable?) meta- meta? > (t/spec-of x)]
  (update-meta x merge meta-))

(defnt merge-meta-from [to (t/and t/with-metable? t/metable?), from t/metable?]
  (update-meta to merge (meta from)))

(defnt replace-meta-from [to t/with-metable?, from t/metable?]
  (with-meta to (meta from)))

;; ===== Declaration/Interning ===== ;;

(defnt intern
  "Finds or creates a var named by the symbol name in the namespace `ns`, setting its root binding
   to `v` if supplied. The namespace must exist. The var will adopt any metadata from `name`.
   Returns the var."
  ([ns- (t/or t/symbol? t/namespace?), name- symbol?]
    (let [var- (clojure.lang.Var/intern (the-ns ns-) name-)]
      (when (meta name-) (.setMeta var- (meta name-)))
      var-))
  ([ns- (t/or t/symbol? t/namespace?), name- symbol?, v _]
    (let [v (clojure.lang.Var/intern (the-ns ns-) name- v)]
      (when (meta name-) (.setMeta var- (meta name-)))
      var-)))

;; TODO typed
#?(:clj (defalias u/def))

;; TODO typed
#?(:clj (u/defalias u/defalias))

;; TODO typed
#?(:clj (u/defaliases u defaliases defaliases'))

#?(:clj (defnt defined? [x t/var?] (.hasRoot x)))

#?(:clj
(defnt alias-var
  "Create a var with the supplied name in the current namespace, having the same metadata and
   root-binding as the supplied var."
  {:attribution  "flatland.useful.ns"
   :contributors ["Alex Gunnarson"]}
  [sym t/symbol?, var- t/var?]
  (apply intern *ns*
    (with-meta sym
      (merge
        {:dont-test
          (str "Alias of " (-> var- meta :name))}
        (meta var-0)
        (meta sym)))
    (when (defined? var-) [(deref var-)]))))

;; TODO typed
#?(:clj (quantum.untyped.core.vars/defmalias defmalias quantum.untyped.core.vars/defmalias))

;; TODO typed
#?(:clj (defaliases u defonce def- defmacro-))

;; ===== Modification ===== ;;

#?(:clj
(defnt reset-var!
  "Like `reset!` but for vars. Atomically sets the root binding of ->`var-` to ->`v`."
  {:attribution "alexandergunnarson"}
  [var- t/var?, v _ > t/var?]
  (.alterRoot var- (fnt [_] v))))

#?(:clj
(defnt update-var!
  {:attribution "alexandergunnarson"}
  ([var- t/var?, f (t/fn [_]) > t/var?]
    (do (.alterRoot var- f)
        var-))
  ;; TODO we need to be able to conditionalize `f`'s arity based on the count of `args`
  ([var- f t/fn? & args (? t/seq?) > t/var?]
    (do (.alterRoot var- (fnt [v' _] (apply f v' args)))
        var-))))

#?(:clj
(defnt clear-vars!
  "Sets each var in ->`vars` to nil."
  {:attribution "alexandergunnarson"}
  [& vars (? (t/seq-of t/var?))]
  (doseq [v vars] (reset-var! v nil))))

;; ===== Thread-local ===== ;;

;; TODO typed
#?(:clj (defalias binding         c/binding))
;; TODO typed
#?(:clj (defalias with-local-vars c/with-local-vars))
