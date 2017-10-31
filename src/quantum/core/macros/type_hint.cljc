(ns quantum.core.macros.type-hint
  (:require
    [quantum.core.error       :as err
      :refer [ex!]]
    [quantum.core.macros.core :as cmacros]
    [quantum.core.type.core   :as tcore]
    [quantum.core.vars        :as var
      :refer [update-meta]]))

(defn type-hint [x] (-> x meta :tag))

(defn with-type-hint [x tag]
  (if (nil? tag)
      x
      (update-meta x assoc :tag tag)))

(defn un-type-hint [x] (update-meta x dissoc :tag))

(defn sanitize-tag [lang tag]
  #?(:clj  (or (get-in tcore/return-types-map [lang tag]) tag)
     :cljs (ex! "`sanitize-tag` not supported in CLJS")))

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
          (throw (ex-info "Cannot convert tag to class" {:tag tag}))))))

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
  {:clj (select-keys tcore/boxed-type-map
         '[boolean byte char short int float])})

#?(:clj
(defn class->instance?-safe-tag|sym
  "Coerces `Class` `c` to a symbol for safe use with `instance?`."
  [^Class c]
  (-> c
      (cond-> (.isPrimitive c) tcore/unboxed->boxed)
      class->symbol)))

(defn ->fn-arglist-tag
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
                     (tcore/prim? tag)
                     (or ;; "fns taking primitives cannot be variadic"
                         variadic?
                         ;; "fns taking primitives support only 4 or fewer args"
                         (> arglist-length 4)))
                  (-> tag tcore/->boxed|sym name)
                (symbol? tag)
                  (str tag)
                :else
                  tag))))

(defn with-fn-arglist-type-hint
  "Ensures `sym` has a type hint appropriate for an `fn` arglist."
  [sym lang arglist-length variadic?]
  (if-let [tag (->fn-arglist-tag (type-hint sym) lang arglist-length variadic?)]
    (with-type-hint sym tag)
    (un-type-hint sym)))

(defn ->body-embeddable-tag
  "Returns a tag embeddable in an `fn` body (or implicit `fn` body
   when e.g. an expression outside of an `fn` body)

   The compiler ignores, at least in certain cases when not in arglists,
   etc., hints that are not strings or symbols, and does not allow
   primitive hints.
   This fn accommodates these requirements."
  [tag]
  #?(:clj (if (class? tag)
              (if (.isPrimitive ^Class tag)
                  nil
                  (.getName ^Class tag))
              tag)
     :cljs tag))
