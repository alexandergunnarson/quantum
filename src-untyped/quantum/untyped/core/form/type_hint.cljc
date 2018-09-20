(ns quantum.untyped.core.form.type-hint
  (:require
    [quantum.untyped.core.collections :as uc]
    [quantum.untyped.core.error
      :refer [err!]]
    [quantum.untyped.core.identifiers
      :refer [>name]]
    [quantum.untyped.core.logic
      :refer [ifs]]
    [quantum.untyped.core.loops
      :refer [reduce-2]]
    [quantum.untyped.core.type.core   :as utcore]
    [quantum.untyped.core.vars
      :refer [update-meta]]))

(defn type-hint [x] (-> x meta :tag))

(defn with-type-hint [x tag]
  (if (nil? tag)
      x
      (update-meta x assoc :tag tag)))

(defn un-type-hint [x] (update-meta x dissoc :tag))

(defn tag [tag- x] (with-type-hint x tag-))

(defn sanitize-tag [lang tag]
  #?(:clj  (or (get-in utcore/return-types-map [lang tag]) tag)
     :cljs (err! "`sanitize-tag` not supported in CLJS")))

#?(:clj
(defn with-sanitize-tag [lang sym]
  (with-type-hint sym (sanitize-tag lang (type-hint sym)))))

#?(:clj
(defn ?symbol->class [s]
  ;; `ns-resolve` doesn't resolve e.g. 'java.lang.Long/TYPE correctly
  (let [?c (eval (sanitize-tag :clj s))]
    (when (class? ?c) ?c))))

#?(:clj
(defn ?tag->class [tag]
  (cond (nil?    tag) tag
        (class?  tag) tag
        (symbol? tag) (?symbol->class tag)
        (string? tag) (Class/forName tag)
        :else         nil)))

#?(:clj
(defn tag->class [tag]
  (if (nil? tag)
      nil
      (or (?tag->class tag)
          (err! "Cannot convert tag to class" {:tag tag})))))

#?(:clj (defn class->str [^Class c] (.getName c)))
#?(:clj (defn class->symbol [^Class c] (-> c class->str symbol)))

#?(:clj (defn type-hint|class [x] (-> x type-hint tag->class)))

(defn type-hint|sym
  "Returns a symbol representing the tagged class of the symbol, or
   `nil` if none exists."
  {:source "ztellman/riddley.compiler"}
  [x]
  (when-let [tag (type-hint x)]
    (let [sym (symbol (when (symbol? tag) (namespace tag))
                      (if #?@(:clj  [(instance? Class tag) (.getName ^Class tag)]
                              :cljs [true])
                          (name tag)))]
      sym)))

(def ^{:doc "Primitive type hints translated into `fn`-safe type hints."}
  fn-safe-type-hints-map
  {:clj (select-keys utcore/boxed-type-map
         '[boolean byte char short int float])})

#?(:clj
(defn class->instance?-safe-tag|sym
  "Coerces `Class` `c` to a symbol for safe use with `instance?`."
  [^Class c]
  (-> c
      (cond-> (.isPrimitive c) utcore/unboxed->boxed)
      class->symbol)))

#?(:clj
(defn >fn-arglist-tag
  "`arglist-length` is count of positional (non-variadic) args"
  [tag lang arglist-length variadic?]
  (if (class? tag)
      (if (.isPrimitive ^Class tag)
          (recur (-> tag class->str symbol) lang arglist-length variadic?)
          (class->str tag))
      (or (-> fn-safe-type-hints-map (get lang) (get tag))
          (cond (or ;; The extra object hints mess things up
                    (= tag 'Object)
                    (= tag 'java.lang.Object))
                  nil
                (and (= lang :clj)
                     (utcore/prim|unevaled? tag)
                     (or ;; "fns taking primitives cannot be variadic"
                         variadic?
                         ;; "fns taking primitives support only 4 or fewer args"
                         (> arglist-length 4)))
                  (-> tag utcore/->boxed|sym name)
                (symbol? tag)
                  (str tag)
                :else
                  tag)))))

#?(:clj
(defn with-fn-arglist-type-hint
  "Ensures `sym` has a type hint appropriate for an `fn` arglist."
  [sym lang arglist-length variadic?]
  (if-let [tag (>fn-arglist-tag (type-hint sym) lang arglist-length variadic?)]
    (with-type-hint sym tag)
    (un-type-hint sym))))

(defn >body-embeddable-tag
  "Outputs a tag embeddable in an `fn` body (or implicit `fn` body
   when e.g. an expression outside of an `fn` body)

   The compiler ignores, at least in certain cases when not in arglists,
   etc., hints that are not strings or symbols, and does not allow
   primitive hints. This fn accommodates these requirements."
  [tag]
  #?(:clj (if (class? tag)
              (if (.isPrimitive ^Class tag)
                  nil
                  (.getName ^Class tag))
              tag)
     :cljs tag))

(defn >arglist-embeddable-tag
  "Outputs a tag embeddable in an (unrestricted, i.e. non-`fn`) arglist.

   The compiler seems to ignore hints that are not strings or symbols, and
   does not allow primitive hints. This fn accommodates these requirements."
  [tag #_(t/or string? symbol? class?)]
  (ifs           (or (string? tag) (symbol? tag))  tag
       #?@(:clj [(class?  tag)                     (.getName ^Class tag)])))

#?(:clj
(defn >interface-method-tag
  "Outputs a tag usable as an interface method return type or arg type.
   For primitive classes, the method must be tagged with the class itself (not a string etc.).
   For all other classes, `>arglist-embeddable-tag` will suffice."
  [tag #_(t/or string? symbol? class?)]
  (if (and (class? tag) (.isPrimitive ^Class tag))
      tag
      (>arglist-embeddable-tag tag))))

(defn static-cast|code
  "`(with-meta (list 'do expr) {:tag class-sym})` isn't enough"
  [class-sym expr]
  (let [cast-sym (gensym "cast-sym")]
    ; `let*` to preserve metadata even when macroexpanding
    (tag class-sym `(let* [~(tag class-sym cast-sym) ~expr] ~cast-sym))))

#?(:clj
(defmacro static-cast
  "Performs a static type cast"
  [class-sym expr]
  (static-cast|code class-sym expr)))

(defn primitive-cast|code [form c #_t/class?]
  (list (symbol "clojure.core" (>name c)) form))

(defn cast-bindings|code
  "Given a map of bindings to class, casts those bindings to their associated classes via
   a `let` statement."
  [form binding->class #_(t/map-of binding-symbol? t/class?)]
  (if (empty? binding->class)
      form
      (list 'let*
        (->> binding->class
             (uc/map+ (fn [[binding-sym c]]
                        [(with-type-hint binding-sym (>body-embeddable-tag c))
                         (if #?(:clj (.isPrimitive ^Class c) :cljs false)
                             (primitive-cast|code binding-sym c)
                             binding-sym)]))
             uc/cat)
        form)))

(defn hint-arglist-with
  [arglist #_seqable? hints #_seqable?]
  (reduce-2 (fn [arglist' arg hint]
              (conj arglist' (with-type-hint arg hint)))
            [] arglist hints))
