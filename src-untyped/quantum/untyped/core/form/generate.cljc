(ns quantum.untyped.core.form.generate
  "For code generation."
  (:require
    [quantum.untyped.core.core :as ucore]))

(ucore/log-this-ns)

(defmulti generate
  "Generates code according to the first argument, `kind`."
  (fn [kind _] kind))

(defn ?wrap-do [codelist]
  (if (-> codelist count (< 2))
      (first codelist)
      (list* 'do codelist)))

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
  ([min-n max-n s] (gen-args min-n max-n s identity))
  ([min-n max-n s gen-gensym]
    (->> (range min-n max-n) (mapv (fn [i] (gen-gensym (str s i)))))))

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

(def unified-gensym-regex #"([a-zA-Z0-9\-\_\'\*]+)#__\d+__auto__$")

(def gensym-regex #"(_|[a-zA-Z0-9\-\_\'\*]+)#?_+(\d+_*#?)+(auto__)?$")

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

(defn >reproducible-gensym|generator [& memoize?]
  (let [*counter (atom -1)]
    (cond-> #(symbol (str % (swap! *counter inc)))
      memoize? memoize)))

(defn unify-gensyms
  "All gensyms defined using two hash symbols are unified to the same
   value, even if they were defined within different syntax-quote scopes."
  {:attribution  'potemkin.macros
   :contributors ["Alex Gunnarson"]}
  ([body] (unify-gensyms body false))
  ([body reproducible-gensyms?]
    (let [gensym* (or *reproducible-gensym*
                      (memoize (if reproducible-gensyms?
                                   (>reproducible-gensym|generator true)
                                   gensym)))]
      (ucore/postwalk
        #(if (unified-gensym? %)
             (symbol (str (gensym* (str (un-gensym %) "__"))
                          (when-not reproducible-gensyms? "__auto__")))
             %)
        body))))
