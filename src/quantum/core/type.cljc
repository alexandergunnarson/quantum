(ns quantum.core.type
  "This is this the namespace upon which all other fully-typed namespaces rest."
  (:refer-clojure :exclude
    [* - and any? defn fn fn? isa? not or ref seq? symbol? type var?])
  (:require
    [quantum.untyped.core.type.defnt :as udefnt]
    [quantum.untyped.core.type       :as ut]
    ;; TODO TYPED prefer e.g. `deft-alias`
    [quantum.untyped.core.vars
      :refer [defalias defaliases]]))

(defalias udefnt/fnt) ; TODO TYPED rename
(defalias udefnt/defn)

(defaliases ut
  type
  ;; Generators
  ? * isa?
  ; fn ; TODO TYPED rename
  ftype
  value, unvalue
  ;; Combinators
  and or - if not
  ;; Metadata suppliers
  ref unref, assume unassume
  ;; Predicates
  any?
  nil?
  none?
  ref?
  fn?)


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
