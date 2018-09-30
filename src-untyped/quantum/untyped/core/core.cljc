(ns quantum.untyped.core.core
         (:refer-clojure :exclude
           [any?])
         (:require
           [clojure.core              :as core]
   #?(:clj [clojure.future            :as fcore])
           [cuerdas.core              :as str+]
 #?@(:clj [[environ.core              :as env]]))
#?(:cljs (:require-macros
           [quantum.untyped.core.core :as self])))

;; ===== Environment ===== ;;

(def lang #?(:clj :clj :cljs :cljs))

#?(:clj
(defn pid []
  (->> (java.lang.management.ManagementFactory/getRuntimeMXBean)
       (.getName))))

#?(:clj
(binding [*out* *err*]
  (when-not (= (:print-pid?          env/env) "false") (println "PID:" (pid)))
  (when-not (= (:print-java-version? env/env) "false") (println "Java version:" (System/getProperty "java.version")))
  (flush)))

;; ===== Compilation ===== ;;

(defonce externs? (atom true))

;; ===== quantum.core.system ===== ;;

(defonce *registered-components (atom {}))

;; ===== Miscellaneous ===== ;;

(defn >sentinel [] #?(:clj (Object.) :cljs #js {}))
(def >object >sentinel)

(defonce sentinel (>sentinel))

;; From `quantum.untyped.core.form.evaluate` — used below in `defalias`

(defn cljs-env?
  "Given an &env from a macro, tells whether it is expanding into CLJS."
  {:from "https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"}
  [env]
  (boolean (:ns env)))

(defn case-env|matches? [env k]
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
       (if (case-env|matches? env# ~k)
           ~v
           (case-env* env# ~@kvs))))))

#?(:clj
(defmacro case-env
  "Conditionally compiles depending on the supplied environment (e.g. CLJ, CLJS, CLR)."
  {:usage `(defmacro abcde [a]
             (case-env :clj `(+ ~a 2) :cljs `(+ ~a 1) `(+ ~a 3)))}
  ([& args] `(case-env* ~'&env ~@args))))

;; From `quantum.untyped.core.vars` — used below in `walk`

(def update-meta vary-meta)

(defn merge-meta-from   [to from] (update-meta to merge (meta from)))
(defn replace-meta-from [to from] (with-meta to (meta from)))

#?(:clj
(defn defalias* [^clojure.lang.Var orig-var ns-name- var-name]
  (let [;; to avoid warnings
        var-name' (with-meta var-name (-> orig-var meta (select-keys [:dynamic])))
        ^clojure.lang.Var var-
          (if (.hasRoot orig-var)
              (intern ns-name- var-name' @orig-var)
              (intern ns-name- var-name'))]
    ;; because this doesn't always get set correctly
    (cond-> var-
      (.isDynamic orig-var)
      (doto (.setDynamic))))))

#?(:clj
(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  {:attribution  'clojure.contrib.def/defalias
   :contributors ["Alex Gunnarson"]}
  ([orig]
    `(defalias ~(symbol (name orig)) ~orig))
  ([name orig]
    `(doto ~(case-env
               :clj  `(defalias* (var ~orig) '~(ns-name *ns*) '~name)
               :cljs `(def ~name (-> ~orig var deref)))
            (alter-meta! merge (meta (var ~orig)))))
  ([name orig doc]
     (list `defalias (with-meta name (assoc (meta name) :doc doc)) orig))))

#?(:clj
(defmacro defaliases'
  "`defalias`es multiple vars ->`names` in the given namespace ->`ns`."
  [ns- & names]
  `(do ~@(for [name- names]
           `(defalias ~name- ~(symbol (name ns-) (name name-)))))))

#?(:clj
(defmacro defaliases
  "`defalias`es multiple vars ->`names` in the given namespace alias ->`alias`."
  [alias- & names]
  (let [ns-sym (if-let [resolved (get (ns-aliases *ns*) alias-)]
                 (ns-name resolved)
                 alias-)]
    `(defaliases' ~ns-sym ~@names))))

;; ===== Fundamental type predicates ===== ;;
;; TODO maybe move to `quantum.untyped.core.data`?

#?(:clj  (eval `(defalias ~(if (resolve `fcore/any?)
                               `fcore/any?
                               `core/any?)))
   :cljs (defalias core/any?))

;; This is in here only because `uclass/protocol?` needs it; it's aliased later
(defn lookup? [x]
  #?(:clj  (instance?  clojure.lang.ILookup x)
     :cljs (satisfies? cljs.core/ILookup    x)))

;; From `quantum.untyped.core.collections.tree` — used in `quantum.untyped.core.macros`

(defn walk
  "Like `clojure.walk`, but ensures preservation of metadata."
  [inner outer form]
  (cond
              (list?      form) (outer (replace-meta-from (apply list (map inner form))                    form))
    #?@(:clj [(map-entry? form) (outer (replace-meta-from (vec        (map inner form))                    form))])
              (seq?       form) (outer (replace-meta-from (doall      (map inner form))                    form))
              (record?    form) (outer (replace-meta-from (reduce (fn [r x] (conj r (inner x))) form form) form))
              (coll?      form) (outer (replace-meta-from (into (empty form) (map inner form))             form))
              :else (outer form)))

(defn postwalk [f form] (walk (partial postwalk f) f form))
(defn prewalk  [f form] (walk (partial prewalk  f) identity (f form)))

;; From `quantum.untyped.core.log` — used to log namespaces

#?(:cljs (enable-console-print!))

(defrecord
  ^{:doc "This is a record and not a map because it's quicker
          to check the default levels (member access: O(1)) than
          it would be with a hash-map (O(log32(n)))."}
  LoggingLevels
  [warn user alert info inspect debug macro-expand trace env])

(defonce *log-levels
  (atom (map->LoggingLevels
          (zipmap #{:always :error :warn :ns} (repeat true)))))

(defonce *outs
  (atom #?(:clj  (let [file-stream (when-let [path (System/getProperty "quantum.core.log|out-file")]
                                     (binding [*out* *err*] (println "Logging to" path))
                                     (-> path
                                         (java.io.FileOutputStream.)
                                         (java.io.OutputStreamWriter.)
                                         (java.io.BufferedWriter.)))
                       print-to-stderror (System/getProperty "quantum.core.log|print-to-stderror")
                       out-stream
                         (when (not= "false" print-to-stderror) *err*)
                       outs (->> [out-stream file-stream] (filterv some?))]
                   (fn [] outs))
           :cljs (fn [] [*out*]))))

(defn print-ns-name-to-outs! [ns-name-]
  (doseq [out (@*outs)] (binding [*out* out] (println lang ":" "loading namespace" ns-name-) (flush)))
  ns-name-)

#?(:clj
(defmacro log-this-ns []
  `(if (get @*log-levels :ns) (print-ns-name-to-outs! '~(ns-name *ns*)) true)))

;; From `quantum.untyped.core.string`

#?(:clj
(defmacro istr
  "'Interpolated string.' Accepts one or more strings; emits a `str` invocation that
  concatenates the string data and evaluated expressions contained
  within that argument.  Evaluation is controlled using ~{} and ~()
  forms. The former is used for simple value replacement using
  clojure.core/str; the latter can be used to embed the results of
  arbitrary function invocation into the produced string.
  Examples:
      user=> (def v 30.5)
      #'user/v
      user=> (istr \"This trial required ~{v}ml of solution.\")
      \"This trial required 30.5ml of solution.\"
      user=> (istr \"There are ~(int v) days in November.\")
      \"There are 30 days in November.\"
      user=> (def m {:a [1 2 3]})
      #'user/m
      user=> (istr \"The total for your order is $~(->> m :a (apply +)).\")
      \"The total for your order is $6.\"
      user=> (istr \"Just split a long interpolated string up into ~(-> m :a (get 0)), \"
               \"~(-> m :a (get 1)), or even ~(-> m :a (get 2)) separate strings \"
               \"if you don't want a << expression to end up being e.g. ~(* 4 (int v)) \"
               \"columns wide.\")
      \"Just split a long interpolated string up into 1, 2, or even 3 separate strings if you don't want a << expression to end up being e.g. 120 columns wide.\"
  Note that quotes surrounding string literals within ~() forms must be
  escaped."
  [& args] `(str+/istr ~@args)))
