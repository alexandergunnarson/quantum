(ns quantum.core.macros.type-hint
  (:require
    [quantum.core.error       :as err
      :refer [ex!]]
    [quantum.core.macros.core :as cmacros]
    [quantum.core.type.core   :as tcore]
    [quantum.core.vars        :as var
      :refer [update-meta]]))

(defn type-hint [x] (-> x meta :tag))

(defn with-type-hint [x hint]
  (if (nil? hint)
      x
      (update-meta x assoc :tag hint)))

(defn un-type-hint [x] (update-meta x dissoc :tag))

(defn sanitize-tag [lang tag]
  #?(:clj  (or (get-in tcore/return-types-map [lang tag]) tag)
     :cljs (ex! "`sanitize-tag` not supported in CLJS")))

#?(:clj
(defn sanitize-sym-tag [lang sym]
  (with-type-hint sym (sanitize-tag lang (type-hint sym)))))

#?(:clj
(defn tag->class [tag]
  (cond (or (nil? tag) (class? tag))
        tag
        (symbol? tag)
        (eval (sanitize-tag :clj tag)) ; `ns-resolve` doesn't resolve e.g. 'java.lang.Long/TYPE correctly
        (string? tag)
        (Class/forName tag)
        :else (throw (ex-info "Cannot convert tag to class" {:tag tag})))))

#?(:clj (defn type-hint:class [x] (-> x type-hint tag->class)))

(defn type-hint:sym
  "Returns a symbol representing the tagged class of the symbol, or
   `nil` if none exists."
  {:source "ztellman/riddley.compiler"}
  [x]
  (when-let [tag (type-hint x)]
    (let [sym (symbol (cond (symbol? tag) (namespace tag)
                            :else         nil)
                      (if #?@(:clj  [(instance? Class tag) (.getName ^Class tag)]
                              :cljs [true])
                          (name tag)))]
      sym)))

(defn ->embeddable-hint
  "The compiler ignores, at least in cases, hints that are not string or symbols,
   and does not allow primitive hints.
   This fn accommodates these requirements."
  [hint]
  #?(:clj (if (class? hint)
              (if (.isPrimitive ^Class hint)
                  nil
                  (.getName ^Class hint))
              hint)
     :cljs hint))
