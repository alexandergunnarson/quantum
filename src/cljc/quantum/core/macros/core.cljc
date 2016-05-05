(ns ^{:doc "Macro-building helper functions."}
  quantum.core.macros.core
  (:refer-clojure :exclude [macroexpand #?(:clj macroexpand-1)])
  (:require [clojure.walk :as walk
              :refer [prewalk]]
   #?(:cljs [cljs.analyzer                      ])
  #?@(:clj [[clojure.jvm.tools.analyzer.hygienic]
            [clojure.jvm.tools.analyzer         ]
            [clojure.tools.analyzer.jvm         ]
            [riddley.walk                       ]
            [clojure.tools.reader :as r]])))

; ===== ENVIRONMENT =====

(defn cljs-env?
  "Given an &env from a macro, tells whether it is expanding into CLJS."
  [env]
  (boolean (:ns env)))

#?(:clj
(defmacro if-cljs
  "Return @then if the macro is generating CLJS code and @else for CLJ code."
  {:from "https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"}
  ([env then else]
    `(if (cljs-env? ~env) ~then ~else))))

#?(:clj
(defmacro when-cljs
  "Return @then if the macro is generating CLJS code."
  ([env then]
    `(when (cljs-env? ~env) ~then))))

#?(:clj
(defmacro context
  {:contributors {"The Joy of Clojure, 2nd ed." "Clojure implementation"
                  "Alex Gunnarson"              "ClojureScript implementation"}
   :todo ["'IOException: Pushback buffer overflow' on certain
            very large data structures"
          "Use reducers"]}
  ([]
    (let [lang- (if-cljs &env :cljs :clj)]
      (condp = lang-
        :clj 
          (let [symbols (keys &env)]
            (zipmap
              (map (fn [sym] `(quote ~sym))
                      symbols)
              symbols))
        :cljs
          ; #{:ns :context :locals :fn-scope :js-globals :line :column}
          `(->> '~&env
                :locals
                (map (fn [[sym# meta#]]
                       [sym# (-> meta# :init :form)]))
                (into {})))))))

; ===== LOCAL EVAL & RESOLVE =====

#?(:clj
(defn eval-local
  "Contextual (local) eval. Restricts the use of specific bindings to |eval|.

   Suffers from not being able to work on non-forms (e.g. atoms cannot be c-evaled)."
  {:attribution "The Joy of Clojure, 2nd ed."
   :contributors {"Alex Gunnarson" "Added error handling for too-large vars"}
   :todo ["'IOException: Pushback buffer overflow' on certain
            very large data structures"]}
  ([context expr]
    (eval
     `(let [~@(mapcat
                (fn [[k v]]
                  (try [k `'~v]
                    (catch java.io.IOException _ [k "var too large to show"])))
                context)]
        ~expr)))))
 
#?(:clj
(defmacro let-eval [expr]
  `(c-eval context ~expr)))

#?(:clj
(defmacro tag
  "Doesn't really work unless print-dup is defined for all local vars."
  [obj tag-]
  `(c-eval (context) (with-meta '~obj {:tag '~tag-}))))

#?(:clj
(defmacro resolve-local
  "Expands to sym if it names a local in the current environment or
  nil otherwise"
  [sym]
  (if (contains? &env sym) sym)))

#?(:clj
(defmacro compile-if
  "Evaluate @exp and if it returns logical true and doesn't error, expand to
  #then.  Else expand to @else."
  {:attribution "clojure.core.reducers"
   :usage '(compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
             (do-cool-stuff-with-fork-join)
             (fall-back-to-executor-services))}
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
     `(do ~then)
     `(do ~else))))

; ===== SYMBOLS =====

(defn hint-meta [sym hint] (with-meta sym {:tag hint}))

; ===== MACROEXPANSION ====

#?(:clj (def macroexpand     riddley.walk/macroexpand))

(defn macroexpand-1 [x & [impl]]
  (condp = impl
    #?@(:clj [:ctools         (clojure.tools.analyzer.jvm/macroexpand-1 x)])
    nil #?(:clj  (macroexpand-1 x)
           :cljs (cljs.analyzer/macroexpand-1 x))))

#?(:clj (defn macroexpand-all
  {:todo ["Compare implementations"]}
  [x & [impl]]
  (condp = impl
    ; Like clojure.walk/macroexpand-all but correctly handles lexical scope
    :ctools         (clojure.tools.analyzer.jvm/macroexpand-all      x)
    
    :tools.hygienic (clojure.jvm.tools.analyzer.hygienic/macroexpand x)
    :tools          (clojure.jvm.tools.analyzer/macroexpand          x)
    ; :walk         (clojure.walk/macroexpand-all x)

    (riddley.walk/macroexpand-all x))))

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

; ===== USEFUL =====

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
        (let [~orig-sym-f (if-cljs ~'&env '~cljs-sym '~clj-sym)]
          (when (= ~orig-sym-f 'nil)
            (throw (IllegalArgumentException. (str "Macro '" '~name "' not defined."))))
          ; Double unquote because we want it to be unquoted in the generated macro
          `(~~orig-sym-f ~@~args-sym)))))))

; ------------- SYNTAX QUOTE; QUOTE+ -------------

#?(:clj (defmalias syntax-quote r/syntax-quote))

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
 `(let [sym-map# (context)]
    (unquote-replacement sym-map# '~form))))