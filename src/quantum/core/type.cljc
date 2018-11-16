(ns quantum.core.type
  "This is this the namespace upon which all other fully-typed namespaces rest."
  (:refer-clojure :exclude
    [- < <= = >= > and any? defn fn fn? isa? not or ref seq? symbol? type var?])
  (:require
    [quantum.untyped.core.type.defnt :as udefnt]
    [quantum.untyped.core.type       :as ut]
    ;; TODO TYPED prefer e.g. `deft-alias`
    [quantum.untyped.core.vars
      :refer [defalias defaliases]]))

;; TODO if we ever spec-instrument we need to be careful of these aliases as they'll no longer be
;; valid

(defalias def udefnt/def)

(defaliases udefnt dotyped fn defn extend-defn!)

(defaliases ut
  type type?
  ;; Generators
  ? run, isa? isa?|direct
  ; fn ; TODO TYPED rename
  ftype
  input-type  input-type|meta-or  input-type|or
  output-type output-type|meta-or output-type|or
  unordered ordered
  value unvalue
  ;; Combinators
  and or - if not
  ;; Metadata suppliers
  ref unref, assume unassume
  ;; Predicates
  any?
  nil?
  none?
  ref?
  fn?
  < <= = >= > <> ><)


;; TODO TYPED move
#_(defnt ^boolean nil?
  ([^Object x] (quantum.core.Numeric/isNil x))
  ([:else   x] false))

;; TODO TYPED move
#_(:clj (defalias nil? core/nil?))

;; TODO TYPED move
#_(defnt ^boolean not'
  ([^boolean? x] (Numeric/not x))
  ([x] (if (nil? x) true))) ; Lisp nil punning

;; TODO TYPED move
#_(defnt ^boolean true?
  ([^boolean? x] x)
  ([:else     x] false))

;; TODO TYPED move
#_(:clj (defalias true? core/true?))

;; TODO TYPED move
#_(defnt ^boolean false?
  ([^boolean? x] (not' x))
  ([:else     x] false))

;; TODO TYPED move
#_(:clj (defalias false? core/false?))
