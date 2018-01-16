(ns quantum.core.macros.core
  "Macro-building helper functions."
  (:refer-clojure :exclude [macroexpand macroexpand-1])
  (:require
    [clojure.core                          :as core]
    [quantum.core.untyped.reducers         :as r]
    [quantum.core.untyped.collections.tree :as tree
      :refer [prewalk postwalk]]
    [cljs.analyzer]
#?@(:clj
   [[clojure.jvm.tools.analyzer.hygienic]
    [clojure.jvm.tools.analyzer]
    [clojure.tools.analyzer.jvm]
    [riddley.walk]
    [clojure.tools.reader :as read]])
    [quantum.core.core    :as qcore])
#?(:cljs
  (:require-macros
    [quantum.core.macros.core :as self
      :refer [env]])))

(defmulti generate
  "Generates code according to the first argument, `kind`."
  (fn [kind _] kind))

; ===== ENVIRONMENT =====

(defn cljs-env?
  "Given an &env from a macro, tells whether it is expanding into CLJS."
  {:from "https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"}
  [env]
  (boolean (:ns env)))

(defn case-env:matches? [env k]
  (case k
    :clj  (not (cljs-env? env)) ; TODO should make this branching
    :cljs (cljs-env? env)
    :clr  (throw (ex-info "TODO: Conditional compilation for CLR not supported" {:platform :clr}))
    (throw (ex-info "Conditional compilation for platform not supported" {:platform k}))))

#?(:clj
(defmacro case-env*
  "Conditionally compiles depending on the supplied environment (e.g. CLJ, CLJS, CLR)."
  {:usage `(defmacro abcde [a]
             (case-env* &env :clj `(+ ~a 2) :cljs `(+ ~a 1) `(+ ~a 3)))
   :todo  {0 "Not sure how CLJ environment would be differentiated from others"}}
  ([env]
    `(throw (ex-info "Compilation unhandled for environment" {:env ~env})))
  ([env v] v)
  ([env k v & kvs]
    `(let [env# ~env]
       (if (case-env:matches? env# ~k)
           ~v
           (case-env* env# ~@kvs))))))

#?(:clj
(defmacro case-env
  "Conditionally compiles depending on the supplied environment (e.g. CLJ, CLJS, CLR)."
  {:usage `(defmacro abcde [a]
             (case-env :clj `(+ ~a 2) :cljs `(+ ~a 1) `(+ ~a 3)))}
  ([& args] `(case-env* ~'&env ~@args))))

#?(:clj (defmacro env-lang [] (case-env :clj :clj :cljs :cljs :clr :clr)))

#?(:clj
(defn core-symbol [env sym] (symbol (str (case-env* env :cljs "cljs" "clojure") ".core") (name sym))))

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

   In Clojure one can use `defalias` for this purpose without a problem, but
   in ClojureScript macros can't be used in a `defalias` context because `defalias`
   creates a ClojureScript (var) binding where a Clojure (macro) one is needed.

   Defaults to the same binding for both Clojure and ClojureScript."
  {:attribution "alexandergunnarson"
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
(defn unquote-replacement
  "Replaces each instance of `(clojure.core/unquote <whatever>)` in `quoted-form` with
   the unquoted version of its inner content."
  {:examples '{(unquote-replacement {'a 3} '(+ 1 ~a))
               '(+ 1 3)}}
  [sym-map quoted-form]
  (println "sym-map" sym-map)
  (println "quoted-form" quoted-form)
  (prewalk
    (fn [x]
      (if (and (seq? x)
               (-> x count   (= 2))
               (-> x (nth 0) (= 'clojure.core/unquote)))
          (if (contains? sym-map (nth x 1))
              (get sym-map (nth x 1))
              (eval (nth x 1)))
          x))
    quoted-form)))

#?(:clj
(defmacro quote+
  "Normal quoting with unquoting that works as in |syntax-quote|."
  {:examples '{(let [a 1]
                 (quote+ (for [b 2] (inc ~a))))
               '(for [a 1] (inc 1))}}
  [form]
  `(unquote-replacement (locals) '~form)))

#?(:clj
(defn syntax-quoted|sym [sym]
  (assert symbol?)
  (-> sym (@#'clojure.tools.reader/syntax-quote*) second)))

; ----- BUILDING FNS ----- ;

(defn gen-args
  ([max-n] (gen-args 0 max-n))
  ([min-n max-n] (gen-args min-n max-n "x"))
  ([min-n max-n s] (gen-args min-n max-n s false))
  ([min-n max-n s gensym?]
    (->> (range min-n max-n) (mapv (fn [i] (symbol (str (if gensym? (gensym s) s) i)))))))

(defn arity-builder [positionalf variadicf & [min-positional-arity max-positional-arity sym-genf no-gensym?]]
  (let [mina (or min-positional-arity 0)
        maxa (or max-positional-arity 18)
        args (->> (range mina (+ mina maxa))
                  (map-indexed (fn [iter i]
                                 (-> (if sym-genf (sym-genf iter) "x")
                                     (cond-> (not no-gensym?) gensym)
                                     (str iter)
                                     symbol))))
        variadic-arg (-> "xs" (cond-> (not no-gensym?) gensym) symbol)]
    `[~@(for [arity (range mina (inc maxa))]
          (let [args:arity (take arity args)]
            `([~@args:arity] ~(positionalf args:arity))))
      ~@(when variadicf
          [`([~@args ~'& ~variadic-arg] ~(variadicf args variadic-arg))])]))

(def max-positional-arity {:clj 18 :cljs 18})

; ----- UNIFY GENSYMS ----- ;
; Adapted from Potemkin for use with both CLJ and CLJS

(def unified-gensym-regex #"([a-zA-Z0-9\-\'\*]+)#__\d+__auto__$")

(def gensym-regex #"(_|[a-zA-Z0-9\-\'\*]+)#?_+(\d+_*#?)+(auto__)?$")

(defn unified-gensym?
  {:attribution 'potemkin.macros}
  [s]
  (and (symbol? s)
       (re-find unified-gensym-regex (str s))))

(defn gensym?
  {:attribution 'potemkin.macros}
  [s]
  (and (symbol? s)
       (re-find gensym-regex (str s))))

(defn un-gensym
  {:attribution 'potemkin.macros}
  [s]
  (second (re-find gensym-regex (str s))))

(def ^:dynamic *reproducible-gensym* nil)

(defn reproducible-gensym|generator []
  (let [*counter (atom -1)]
    (memoize #(symbol (str % (swap! *counter inc))))))

(defn unify-gensyms
  "All gensyms defined using two hash symbols are unified to the same
   value, even if they were defined within different syntax-quote scopes."
  {:attribution  'potemkin.macros
   :contributors ["Alex Gunnarson"]}
  ([body] (unify-gensyms body false))
  ([body reproducible-gensyms?]
    (let [gensym* (or *reproducible-gensym*
                      (memoize (if reproducible-gensyms?
                                   (reproducible-gensym|generator)
                                   gensym)))]
      (postwalk
        #(if (unified-gensym? %)
             (symbol (str (gensym* (str (un-gensym %) "__")) (when-not reproducible-gensyms? "__auto__")))
             %)
        body))))

#?(:clj
(defmacro $
  "Reproducibly, unifiedly syntax quote without messing up the format as a literal
   syntax quote might do."
  [body]
  `(binding [*reproducible-gensym* (reproducible-gensym|generator)]
     (unify-gensyms (syntax-quote ~body) true))))

; ===== VARS ===== ;

#?(:clj
(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  {:attribution  'clojure.contrib.def/defalias
   :contributors ["Alex Gunnarson"]}
  ([name orig]
     `(do (if ~(case-env :clj `(-> (var ~orig) .hasRoot) :cljs true)
              (do (def ~name (with-meta (-> ~orig var deref) (meta (var ~orig))))
                  ; The below is apparently necessary
                  (doto #'~name (alter-meta! merge (meta (var ~orig)))))
              (def ~name))
        (var ~name)))
  ([name orig doc]
     (list `defalias (with-meta name (assoc (meta name) :doc doc)) orig))))

