(ns quantum.core.vars
  "Functions related to vars and metadata."
         (:refer-clojure :exclude
           [binding defonce intern meta reset-meta! var? with-local-vars with-meta])
         (:require
           ;; TODO TYPED remove reference to `clojure.core`
           [clojure.core              :as c]
           [quantum.core.ns           :as ns]
           [quantum.core.type         :as t
             :refer [defnt]]
           ;; TODO TYPED remove reference to `quantum.untyped.core.vars`
           [quantum.untyped.core.vars :as uvar])
#?(:cljs (:require-macros
           [quantum.core.vars         :as this])))

#?(:clj (def var? t/var?))

;; ===== Meta ===== ;;

(def meta? (t/? t/+map?))

(defnt meta
  "Returns the (possibly nil) metadata of ->`x`."
  > meta?
  [x t/metable?] (#?(:clj .meta :cljs cljs.core/-meta) x))

(defnt with-meta
  "Returns an object of the same type and value as ->`x`, with ->`meta'` as its metadata."
  > t/with-metable?
           ([x t/with-metable?, meta' meta? > (t/* t/with-metable?) #_(TODO TYPED (t/value-of x))]
             (#?(:clj .withMeta :cljs cljs.core/-with-meta) x meta'))
  #?(:cljs ([x goog/isFunction, meta' meta?]
             (cljs.core/MetaFn. x meta'))))

(defnt reset-meta!
  "Atomically resets ->`x`'s metadata to be ->`meta'`."
  > meta?
  [x (t/isa? #?(:clj clojure.lang.IReference :cljs (TODO))) meta' meta?]
  (#?(:clj .resetMeta :cljs (set! (.-meta x) m)) x meta'))

;; TODO TYPED
#_(defnt update-meta
  "Returns an object of the same type and value as ->`x`, with its metadata updated by ->`f`."
  ;; TODO `f` should more specifically be able to handle the args arity and specs
  [x (t/and t/with-metable? t/metable?) f (t/fn meta? [& (t/type-of %args)]) & args _]
  (with-meta x (apply f (meta x) args)))

;; TODO TYPED
#_(defnt merge-meta
  {:alternate-implementations #{'cljs.tools.reader/merge-meta}}
  [x (t/and t/with-metable? t/metable?) meta- meta? > (t/spec-of x)]
  (update-meta x merge meta-))

;; TODO TYPED
#_(defnt merge-meta-from [to (t/and t/with-metable? t/metable?), from t/metable?]
  (update-meta to merge (meta from)))

(defnt replace-meta-from > t/with-metable? [to t/with-metable?, from t/metable?]
  (with-meta to (meta from)))

;; ===== Declaration/Interning ===== ;;

#?(:clj
(defnt intern
  "Finds or creates a var named by the symbol name in ->`ns-val`, setting its root binding to ->`v`
   if supplied. The namespace must exist. The var will adopt any metadata from ->`name-val`.
   Returns the var."
  > t/var?
  ([ns-val (t/or t/symbol? t/namespace?), var-name t/symbol? > (t/* t/var?)]
    (let [var-ref (clojure.lang.Var/intern (ns/>ns ns-val) var-name)]
      (when (meta var-name) (.setMeta var-ref (meta var-name)))
      var-ref))
  ([ns-val (t/or t/symbol? t/namespace?), var-name t/symbol?, var-val (t/ref t/any?) > (t/* t/var?)]
    (let [var-ref (clojure.lang.Var/intern (ns/>ns ns-val) var-name var-val)]
      (when (meta var-name) (.setMeta var-ref (meta var-name)))
      var-ref))))

;; TODO TYPED
#?(:clj (defalias uvar/def))

;; TODO TYPED
#?(:clj (uvar/defaliases uvar defalias defaliases defaliases'))

#?(:clj (defnt defined? [x t/var?] (.hasRoot x)))

;; TODO TYPED — need to do `apply`, and `apply` with defnt; also `merge`, `str`, `deref`
#_(:clj
(defnt alias-var
  "Create a var with the supplied name in the current namespace, having the same metadata and
   root-binding as the supplied var."
  {:attribution  "flatland.useful.ns"
   :contributors ["Alex Gunnarson"]}
  [sym t/symbol?, var-val t/var?]
  (apply intern *ns*
    (with-meta sym
      (merge
        {:dont-test
          (str "Alias of " (-> var-val meta :name))}
        (meta var-0)
        (meta sym)))
    (when (defined? var-) [(deref var-val)]))))

;; TODO TYPED
#?(:clj (quantum.untyped.core.vars/defmalias defmalias quantum.untyped.core.vars/defmalias))

;; TODO TYPED
#?(:clj (defaliases uvar defonce def- defmacro-))

;; ===== Modification ===== ;;

;; TODO TYPED — need to do `fnt`
#_(:clj
(defnt reset-var!
  "Like `reset!` but for vars. Atomically sets the root binding of ->`var-` to ->`v`."
  {:attribution "alexandergunnarson"}
  [var-val t/var?, v (t/ref t/any?) > t/var?]
  (.alterRoot var-val (fnt [_] v))))

;; TODO TYPED — need to do `fnt`, `apply`
#_(:clj
(defnt update-var!
  {:attribution "alexandergunnarson"}
  ([var- t/var?, f (t/fn [_]) > t/var?]
    (do (.alterRoot var- f)
        var-))
  ;; TODO we need to be able to conditionalize `f`'s arity based on the count of `args`
  ([var- f t/fn? & args (? t/seq?) > t/var?]
    (do (.alterRoot var- (fnt [v' _] (apply f v' args)))
        var-))))

;; TODO TYPED — `doseq`
#_(:clj
(defnt clear-vars!
  "Sets each var in ->`vars` to nil."
  {:attribution "alexandergunnarson"}
  [& vars (? (t/seq-of t/var?))]
  (doseq [v vars] (reset-var! v nil))))

;; ===== Thread-local ===== ;;

;; TODO TYPED
#?(:clj (defalias binding         c/binding))
;; TODO TYPED
#?(:clj (defalias with-local-vars c/with-local-vars))
