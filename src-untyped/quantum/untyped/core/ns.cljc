(ns
  ^{:doc "Useful namespace and var-related functions."
    :attribution "alexandergunnarson"}
  quantum.untyped.core.ns
  (:refer-clojure :exclude
    [ns in-ns all-ns create-ns the-ns find-ns ns-name ns-map
     alias ns-aliases require import ns-imports use
     ns-interns ns-publics refer ns-refers refer-clojure ns-unalias ns-unmap loaded-libs
     remove-ns])
  (:require
    [clojure.core              :as core]
    [clojure.set               :as set]
    [clojure.string            :as str]
    [quantum.untyped.core.vars :as uvar
      :refer [defalias defaliases]]))

#?(:clj (do
(defaliases core ns the-ns find-ns ns-name ns-map)
(defalias core/ns-unmap               )
(defalias ns-unmap!      ns-unmap     )
(defalias core/in-ns                  )
(defalias core/all-ns                 )
(defalias core/create-ns              )
(defalias create-ns!     create-ns    )
(defalias core/alias                  )
(defalias alias!         alias        )
(defalias core/ns-unalias             )
(defalias ns-unalias!    ns-unalias   )
(defalias core/ns-aliases             )
(defalias core/require                )
(defalias require!       require      )
(defalias core/import                 )
(defalias import!        import       )
(defalias core/ns-imports             )
(defalias core/use                    )
(defalias use!           use          )
(defalias core/ns-interns             )
(defalias core/ns-publics             )
(defalias core/refer                  )
(defalias refer!         refer        )
(defalias core/refer-clojure          )
(defalias refer-clojure! refer-clojure)
(defalias core/ns-refers              )
(defalias core/remove-ns              )
(defalias remove-ns!     remove-ns    )))

#?(:clj (defn the-alias [alias-sym] (.lookupAlias *ns* alias-sym)))

#?(:clj
(defn ns>alias [ns- lookup-ns] ; TODO faster via caching
  (->> ns- ns-aliases seq
       (filter (fn [[alias- ns']] (= ns' lookup-ns)))
       ffirst)))

#?(:clj
(defn ns-name>alias [ns- lookup-ns-name] ; TODO faster via caching
  (->> ns- ns-aliases seq
       (filter (fn [[alias- ns']] (= (ns-name ns') lookup-ns-name)))
       ffirst)))

#?(:clj
(defn clear-ns-interns!
  "Clears a namespace of all vars, public and private, but does not
   delete the namespace."
  [ns-]
  (->> ns- ns-interns keys (map #(ns-unmap ns- %)) dorun)
  ns-))

#?(:clj
(defmacro search-var
  "Searches for a var ->`var0` in the available namespaces."
  {:usage '(ns-find abc)
   :todo ["Make it better and filter out unnecessary results"]
   :attribution "alexandergunnarson"}
  [var0]
 `(->> (all-ns)
       (map ns-publics)
       (filter
         (fn [obj#]
           (->> obj#
                keys
                (map name)
                (apply str)
                (re-find (re-pattern (str '~var0)))))))))

#?(:clj
(defmacro ns-exclude [& syms]
  `(doseq [sym# '~syms]
     (ns-unmap *ns* sym#))))

#?(:clj
(defmacro with-ns
  "Perform an operation in another ns."
  [ns- & body]
  (let [ns-0 (ns-name *ns*)]
    `(do (in-ns ~ns-) ~@body (in-ns '~ns-0)))))

#?(:clj
(defmacro with-temp-ns
  "Evaluates @exprs in a temporarily-created namespace.
  All created vars will be destroyed after evaluation."
  {:source "zcaudate/hara.namespace.eval"}
  [& exprs]
  `(try
     (create-ns 'sym#)
     (let [res# (with-ns 'sym#
                  (clojure.core/refer-clojure)
                  ~@exprs)]
       res#)
     (finally (remove-ns 'sym#)))))

#?(:clj
(defmacro import-static
  "Imports the named static fields and/or static methods of the class
  as (private) symbols in the current namespace.
  Example:
      user=> (import-static java.lang.Math PI sqrt)
      nil
      user=> PI
      3.141592653589793
      user=> (sqrt 16)
      4.0
  Note: The class name must be fully qualified, even if it has already
  been imported.  Static methods are defined as MACROS, not
  first-class fns."
  {:source "Stuart Sierra, via clojure.clojure-contrib/import-static"}
  [class & fields-and-methods]
  (let [only (set (map str fields-and-methods))
        ^Class the-class (Class/forName (str class))
        static?       (fn [^java.lang.reflect.Member x]
                        (-> x .getModifiers java.lang.reflect.Modifier/isStatic))
        statics       (fn [array]
                        (set (map (fn [^java.lang.reflect.Member x] (.getName x))
                                  (filter static? array))))
        all-fields    (-> the-class .getFields  statics)
        all-methods   (-> the-class .getMethods statics)
        fields-to-do  (set/intersection all-fields  only)
        methods-to-do (set/intersection all-methods only)
        make-sym      (fn [string]
                          (with-meta (symbol string) {:private true}))
        import-field  (fn [name]
                          (list 'def (make-sym name)
                                (list '. class (symbol name))))
        import-method (fn [name]
                          (list 'defmacro (make-sym name)
                                '[& args]
                                (list 'list ''. (list 'quote class)
                                      (list 'apply 'list
                                            (list 'quote (symbol name))
                                            'args))))]
    `(do ~@(map import-field fields-to-do)
         ~@(map import-method methods-to-do)))))

; DYNAMIC LOADING

#?(:clj
(defn load-ns [path ns-sym]
  (remove-ns ns-sym)
  (load-file path)))

#?(:clj
(defn load-nss
  {:todo ["This function is not robust"]}
  ([ns-syms] (load-nss "./src/cljc" ns-syms)) ; TODO all src paths from project.clj
  ([base-path ns-syms]
    (doseq [ns-sym ns-syms]
      (load-ns (str base-path "/" (str/replace (name ns-sym) "." "/") ".cljc") ; TODO path separator
               ns-sym)))))

#?(:clj (defalias loaded-libs core/loaded-libs))

#?(:clj
(defmacro load-lib [lib]
  `(do (require 'alembic.still)
       (alembic.still/distill ~lib))))

#_(:clj (defalias lein alembic.still/lein))

#?(:clj
(defn assert-ns-aliased
  "Asserts that the provided namespace is aliased in the current namespace."
  [ns-]
  (let [there (->> (keys (ns-publics ns-))
                   (remove #(->> % name (re-find #"([pP]rotocol)|-reified|externed")))
                   set)
        here  (set (keys (ns-publics *ns*)))]
    (assert (set/subset? there here)
            {:ns      ns-
             :missing (set/difference there here)}))))
