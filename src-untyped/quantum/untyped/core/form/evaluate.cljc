(ns quantum.untyped.core.form.evaluate
  (:refer-clojure :exclude
    [macroexpand macroexpand-1])
  (:require
#?@(:clj
   [[cljs.analyzer]
    [clojure.jvm.tools.analyzer]
    [clojure.jvm.tools.analyzer.hygienic]
    [clojure.tools.analyzer.jvm]
    [riddley.walk]])
    [clojure.core              :as core]
    [clojure.core.reducers     :as r]
    [quantum.untyped.core.core :as ucore
      :refer [defaliases]])
#?(:cljs
  (:require-macros
    [quantum.untyped.core.form.evaluate :as self
      :refer [env]])))

(ucore/log-this-ns)

;; ===== Environment ===== ;;

(defaliases ucore cljs-env? case-env|matches? #?@(:clj [case-env* case-env]))

#?(:clj (defmacro env-lang [] (case-env :clj :clj :cljs :cljs :clr :clr)))

#?(:clj
(defmacro locals
  "Returns a map of the local variables in scope of wherever
   this macro is expanded, from symbols to values.

   Inspired by The Joy of Clojure, 2nd ed., |context| macro."
  {:contributors #{"Alex Gunnarson"}
   :todo ["'IOException: Pushback buffer overflow' on certain
            very large data structures"]}
  ([] `(locals ~&env)) ; #{:ns :context :locals :fn-scope :js-globals :line :column}
  ([env]
    (let [getter (case-env :cljs :locals identity)]
      (->> env getter
           (r/map (fn [[sym _]] (let [sym' (vary-meta sym dissoc :tag)] [`(quote ~sym') sym'])))
           (into {}))))))

#?(:clj
(defmacro env
  "Retrieves the (sanitized) macroexpansion environment."
  []
  `(identity
     '~(->> &env
            (ucore/postwalk
              (fn [x#] (cond (instance? clojure.lang.Compiler$LocalBinding x#)
                             (.name ^clojure.lang.Compiler$LocalBinding x#)
                             (nil? x#)
                             []
                             :else x#)))))))

;; ===== Local `eval` and `resolve` ===== ;;

#?(:clj
(defn eval|local
  "Contextual (local) eval. Restricts the use of specific bindings to |eval|.

   Suffers from not being able to work on non-forms (e.g. atoms cannot be c-evaled)."
  {:attribution "The Joy of Clojure, 2nd ed."
   :contributors {"Alex Gunnarson" "Added error handling for too-large vars"}
   :todo ["'IOException: Pushback buffer overflow' on certain
            very large data structures"]}
  ([locals expr]
    (eval
     `(let [~@(mapcat
                (fn [[k v]]
                  (try [k `'~v]
                    (catch java.io.IOException _ [k "var too large to show"])))
                locals)]
        ~expr)))))

#?(:clj
(defmacro let-eval [expr]
  `(eval|local locals ~expr)))
(ucore/log-this-ns)

#?(:clj
(defmacro resolve|local
  "Expands to sym if it names a local in the current environment or
  nil otherwise"
  [sym]
  (if (contains? (case-env :cljs (:locals &env) &env) sym) sym)))

;; TODO deprecate
#_(:clj
(defmacro tag
  "Doesn't really work unless print-dup is defined for all local vars."
  [obj tag-]
  `(eval-local (locals) (with-meta '~obj {:tag '~tag-}))))

;; ===== Conditional compilation ===== ;;

#?(:clj
(defmacro compile-if
  "Evaluates `->pred` and if it returns logical true and doesn't error, expands to
   `then`. Else expands to `else`."
  {:attribution "clojure.core.reducers"
   :usage '(compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
             (do-stuff-with-fork-join)
             (fall-back-to-executor-services))}
  [pred then else]
  (if (try (eval pred)
           (catch Throwable _ false))
     `(do ~then)
     `(do ~else))))

#?(:clj
(defmacro compile-when [pred then] `(compile-if ~pred ~then nil)))

;; ===== Macroexpansion ===== ;;

(defn macroexpand-1 [form & [impl & args]]
  (case impl
    #?@(:clj [:ana (apply clojure.tools.analyzer.jvm/macroexpand-1 form args)])
    #?(:clj  (core/macroexpand-1 form)
       :cljs (apply cljs.analyzer/macroexpand-1 (concat args [form])))))

(defn cljs-macroexpand
  {:adapted-from 'com.rpl.specter/cljs-macroexpand}
  ([form] (cljs-macroexpand (env)))
  ([form env-]
    (let [mform (cljs.analyzer/macroexpand-1 env- form)]
      (cond (identical? form mform) mform
            (and (seq? mform) (#{'js*} (first mform))) form
            :else (cljs-macroexpand mform env-)))))

#?(:clj  (def macroexpand riddley.walk/macroexpand)
   :cljs (def macroexpand cljs-macroexpand))

(defn cljs-macroexpand-all
  {:adapted-from 'com.rpl.specter/cljs-macroexpand-all}
  ([form] (cljs-macroexpand-all (env)))
  ([form env-]
    (if (and (seq? form)
             (#{'fn 'fn* 'cljs.core/fn} (first form)))
      form
      (let [expanded (if (seq? form) (cljs-macroexpand form env-) form)]
        (ucore/walk #(cljs-macroexpand-all % env-) identity expanded)))))

#?(:clj
    (defn macroexpand-all
      {:todo ["Compare implementations"]}
      [form & [impl & args]]
      (case impl
        ; Like clojure.walk/macroexpand-all but correctly handles lexical scope
        :ctools         (clojure.tools.analyzer.jvm/macroexpand-all      form)
        :tools.hygienic (clojure.jvm.tools.analyzer.hygienic/macroexpand form)
        :tools          (clojure.jvm.tools.analyzer/macroexpand          form)
        :cljs           (apply cljs-macroexpand-all form args)
        ; :walk         (clojure.walk/macroexpand-all form)
        (riddley.walk/macroexpand-all form)))
   :cljs
    (def macroexpand-all cljs-macroexpand-all))
