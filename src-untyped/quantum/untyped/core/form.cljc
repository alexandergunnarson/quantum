(ns quantum.untyped.core.form
  (:require
    [quantum.untyped.core.core          :as ucore
      :refer [defalias]]
    [quantum.untyped.core.form.evaluate :as ueval
      :refer [case-env*]]))

(ucore/log-this-ns)

(defn core-symbol [env sym] (symbol (str (case-env* env :cljs "cljs" "clojure") ".core") (name sym)))

;; TODO move this code generation code to a different namespace

;; ===== Macro code generation ===== ;;

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

;; ===== Function code generation ===== ;;

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

;; ===== Gensym unification ===== ;;
;; Adapted from Potemkin for use with both CLJ and CLJS

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
      (ucore/postwalk
        #(if (unified-gensym? %)
             (symbol (str (gensym* (str (un-gensym %) "__")) (when-not reproducible-gensyms? "__auto__")))
             %)
        body))))

;; ===== Code quoting ===== ;;

; ------------- SYNTAX QUOTE; QUOTE+ -------------

#?(:clj (defalias syntax-quote clojure.tools.reader/syntax-quote))

#?(:clj
(defn unquote-replacement
  "Replaces each instance of `(clojure.core/unquote <whatever>)` in `quoted-form` with
   the unquoted version of its inner content."
  {:examples '{(unquote-replacement {'a 3} '(+ 1 ~a))
               '(+ 1 3)}}
  [sym-map quoted-form]
  (ucore/prewalk
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
(defmacro $
  "Reproducibly, unifiedly syntax quote without messing up the format as a literal
   syntax quote might do."
  [body]
  `(binding [*reproducible-gensym* (reproducible-gensym|generator)]
     (unify-gensyms (syntax-quote ~body) true))))
