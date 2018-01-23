(ns quantum.untyped.core.core
  (:refer-clojure :exclude [seqable? boolean?])
  (:require
    [cuerdas.core :as str+]))

(defn ->sentinel [] #?(:clj (Object.) :cljs #js {}))

(defn quote-map-base [kw-modifier ks & [no-quote?]]
  (->> ks
       (map #(vector (cond->> (kw-modifier %) (not no-quote?) (list 'quote)) %))
       (apply concat)))

(defn >keyword [x]
  (cond (keyword? x) x
        (symbol?  x) (keyword (namespace x) (name x))
        :else        (-> x str keyword)))

#?(:clj (defmacro kw-map [& ks] (list* `hash-map (quote-map-base >keyword ks))))

; ===== TYPE PREDICATES =====

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

(def val? some?)

(defn boolean? [x] #?(:clj  (instance? Boolean x)
                      :cljs (or (true? x) (false? x))))

(defn lookup? [x]
  #?(:clj  (instance? clojure.lang.ILookup x)
     :cljs (satisfies? ILookup x)))

#?(:clj
(defn protocol? [x]
  (and (lookup? x) (-> x (get :on-interface) class?))))

(defn regex? [x] (instance? #?(:clj java.util.regex.Pattern :cljs js/RegExp) x))

#?(:clj  (defn seqable?
           "Returns true if (seq x) will succeed, false otherwise."
           {:from "clojure.contrib.core"}
           [x]
           (or (seq? x)
               (instance? clojure.lang.Seqable x)
               (nil? x)
               (instance? Iterable x)
               (-> x class .isArray)
               (string? x)
               (instance? java.util.Map x)))
   :cljs (def seqable? core/seqable?))

(defn editable? [coll]
  #?(:clj  (instance?  clojure.lang.IEditableCollection coll)
     :cljs (satisfies? cljs.core.IEditableCollection    coll)))

#?(:clj (defn namespace? [x] (instance? clojure.lang.Namespace x)))

(defn metable? [x]
  #?(:clj  (instance?  clojure.lang.IMeta x)
     :cljs (satisfies? cljs.core/IMeta    x)))

; ===== COLLECTIONS =====

(defn seq=
  ([a b] (seq= a b =))
  ([a b eq-f]
  (boolean
    (when (or (sequential? b) #?(:clj  (instance? java.util.List b)
                                 :cljs (list? b)))
      (loop [a (seq a) b (seq b)]
        (when (= (nil? a) (nil? b))
          (or (nil? a)
              (when (eq-f (first a) (first b))
                (recur (next a) (next b))))))))))

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

(defn code=
  "Ensures that two pieces of code are equivalent.
   This means ensuring that seqs, vectors, and maps are only allowed to be compared with
   each other, and that metadata is equivalent."
  [code0 code1]
  (if (metable? code0)
      (and (metable? code1)
           (= (meta code0) (meta code1))
           (cond (seq?    code0) (and (seq?    code1) (seq=      code0       code1  code=))
                 (vector? code0) (and (vector? code1) (seq= (seq code0) (seq code1) code=))
                 (map?    code0) (and (map?    code1) (seq= (seq code0) (seq code1) code=))
                 :else           (= code0 code1)))
      (and (not (metable? code1))
           (= code0 code1))))

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
(defmacro defalias
  "Defines an alias for a var: a new var with the same root binding (if
  any) and similar metadata. The metadata of the alias is its initial
  metadata (as provided by def) merged into the metadata of the original."
  {:attribution  'clojure.contrib.def/defalias
   :contributors ["Alex Gunnarson"]}
  ([orig]
    `(defalias ~(symbol (name orig)) ~orig))
  ([name orig]
     `(do (if ~(case-env :clj `(-> (var ~orig) .hasRoot) :cljs true)
              (do (def ~name
                    (let [derefed# (-> ~orig var deref)]
                      (if (metable? derefed#)
                          (with-meta derefed# (meta (var ~orig)))
                          derefed#)))
                  ; The below is apparently necessary
                  (doto #'~name (alter-meta! merge (meta (var ~orig)))))
              (def ~name))
        (var ~name)))
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
