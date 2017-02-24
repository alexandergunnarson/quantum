(ns quantum.core.macros.core
  "Macro-building helper functions."
  (:refer-clojure :exclude [macroexpand macroexpand-1])
  (:require  [clojure.core           :as core]
             [clojure.core.reducers  :as red]
             [clojure.walk           :as walk
               :refer [prewalk]]
             [cljs.analyzer]
   #?@(:clj [[clojure.jvm.tools.analyzer.hygienic]
             [clojure.jvm.tools.analyzer]
             [clojure.tools.analyzer.jvm]
             [riddley.walk]
             [clojure.tools.reader :as r]])
             [quantum.core.core    :as qcore])
  #?(:cljs (:require-macros
             [quantum.core.macros.core :as self
               :refer [env]])))

; ===== ENVIRONMENT =====

(defn cljs-env?
  "Given an &env from a macro, tells whether it is expanding into CLJS."
  {:from "https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"}
  [env]
  (boolean (:ns env)))

#?(:clj
(defmacro case-env*
  "Conditionally compiles depending on the supplied environment (e.g. CLJ, CLJS, CLR)."
  {:usage `(defmacro abcde [a]
             (case-env* &env :clj `(+ ~a 2) :cljs `(+ ~a 1) `(+ ~a 3)))
   :todo  {0 "Not sure how CLJ environment would be differentiated from others"}}
  ([env])
  ([env v] v)
  ([env k v & kvs]
    (let [accepted?
           (case k
             :clj  true ; TODO 0
             :cljs (cljs-env? env)
             :clr  (throw (ex-info "TODO: Conditional compilation for CLR not supported" {:platform :clr}))
             (throw (ex-info "Conditional compilation for platform not supported" {:platform k})))]
      (if accepted?
          v
          `(case-env* ~env ~@kvs))))))

#?(:clj
(defmacro case-env
  "Conditionally compiles depending on the supplied environment (e.g. CLJ, CLJS, CLR)."
  {:usage `(defmacro abcde [a]
             (case-env :clj `(+ ~a 2) :cljs `(+ ~a 1) `(+ ~a 3)))}
  ([& args] `(case-env* ~&env ~@args))))

#?(:clj
(defn core-symbol [env sym] (symbol (str (case-env :cljs "cljs" "clojure") ".core") (name sym))))

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
           (red/map (fn [[sym _]] [`(quote ~sym) sym]))
           (into {}))))))

#?(:clj
(defmacro env
  "Retrieves the (sanitized) macroexpansion environment."
  []
  `(identity
     '~(->> &env
            (clojure.walk/postwalk
              (fn [x#] (cond (instance? clojure.lang.Compiler$LocalBinding x#)
                             (.name ^clojure.lang.Compiler$LocalBinding x#)
                             (nil? x#)
                             []
                             :else x#)))))))

; ===== LOCAL EVAL & RESOLVE =====

#?(:clj
(defn eval-local
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
  `(c-eval locals ~expr)))

#?(:clj
(defmacro tag
  "Doesn't really work unless print-dup is defined for all local vars."
  [obj tag-]
  `(c-eval (locals) (with-meta '~obj {:tag '~tag-}))))

#?(:clj
(defmacro resolve-local
  "Expands to sym if it names a local in the current environment or
  nil otherwise"
  [sym]
  (if (contains? (case-env :cljs (:locals &env) &env) sym) sym)))

#?(:clj
(defmacro compile-if
  "Evaluate @exp and if it returns logical true and doesn't error, expand to
  #then.  Else expand to @else."
  {:attribution "clojure.core.reducers"
   :usage '(compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
             (do-cool-stuff-with-fork-join)
             (fall-back-to-executor-services))}
  [pred then else]
  (if (try (eval pred)
           (catch Throwable _ false))
     `(do ~then)
     `(do ~else))))

#?(:clj
(defmacro compile-when [pred then] `(compile-if ~pred ~then nil)))

; ===== SYMBOLS =====

(defn hint-meta [sym hint] (vary-meta sym assoc :tag hint))

; ===== MACROEXPANSION ====

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
        (walk/walk #(cljs-macroexpand-all % env-) identity expanded)))))

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


; ===== MACRO CREATION HELPERS =====

(defn name-with-attrs
  "Handles optional docstrings & attr maps for a macro def's name."
  {:from "clojure.tools.macro"}
  [name macro-args]
  (let [[docstring macro-args] (if (string? (first macro-args))
                                   [(first macro-args) (next macro-args)]
                                   [nil macro-args])
        [attr      macro-args] (if (map? (first macro-args))
                                   [(first macro-args) (next macro-args)]
                                   [{} macro-args])
        attr (if docstring (assoc attr :doc docstring) attr)
        attr (if (meta name) (conj (meta name) attr)   attr)]
    [(with-meta name attr) macro-args]))

; ===== ALIASING =====

;#?(:clj (def mfn reg/mfn))

#?(:clj
(defmacro defmalias
  "Defines an cross-platform alias for a macro.

   In Clojure one can use |defalias| for this purpose without a problem, but
   in ClojureScript macros can't be used in a |defalias| context because |defalias|
   creates a ClojureScript (var) binding where a Clojure (macro) one is needed.

   Defaults to the same binding for both Clojure and ClojureScript."
  {:attribution "Alex Gunnarson"
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

; ------------- SYNTAX QUOTE; QUOTE+ -------------

#?(:clj (defmalias syntax-quote clojure.tools.reader/syntax-quote))

#?(:clj
(defmacro unquote-replacement
  "Replaces all duple-lists: (clojure.core/unquote ___) with the unquoted version of the inner content."
  [sym-map quoted-form]
  `(prewalk
     (fn [obj#]
       (if (and (seq? obj#)
                (-> obj# count   (= 2))
                (-> obj# (nth 0) (= 'clojure.core/unquote)))
           (if (contains? ~sym-map (-> obj# (nth 1)))
               (get ~sym-map (-> obj# (nth 1)))
               (throw (ex-info "Symbol does not evaluate to anything" (-> obj# (nth 1)))))
           obj#))
     ~quoted-form)))

#?(:clj
(defmacro quote+
  "Normal quoting with unquoting that works as in |syntax-quote|."
  {:in '[(let [a 1]
           (for [b 2] (inc ~a)))]
   :out '(for [a 1] (inc 1))}
  [form]
 `(let [sym-map# (locals)]
    (unquote-replacement sym-map# '~form))))
