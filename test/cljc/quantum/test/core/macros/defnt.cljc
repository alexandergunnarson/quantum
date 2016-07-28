(ns quantum.test.core.macros.defnt
  (:require [quantum.core.macros.defnt :as ns]))

(defn test:special-defnt-keyword? [x])

(defn test:get-qualified-class-name [lang class-sym])

(defn test:classes-for-type-predicate
  ([pred lang])
  ([pred lang type-arglist]))

(defn test:expand-classes-for-type-hint
  ([x lang])
  ([x lang arglist]))

(defn test:hint-arglist-with [arglist hints])

(defn test:defnt-remove-hints [x])

(defn test:defnt-arities
  [body])

(defn test:defnt-arglists
  [body])

(defn test:defnt-gen-protocol-names [{:keys [sym strict? lang]}])

(defn test:defnt-gen-interface-unexpanded
  [{:keys [sym arities arglists-types lang]}])

(defn test:defnt-replace-kw
  [kw {:keys [type-hints type-arglist available-default-types hint inner-type-n]}])

(defn test:defnt-gen-interface-expanded
  [{:keys [lang
           gen-interface-code-body-unexpanded
           available-default-types]
    :as env}])

(defn test:defnt-gen-interface-def
  [{:keys [gen-interface-code-header gen-interface-code-body-expanded]}])

(defn test:defnt-positioned-types-for-arglist
  [arglist types])

(defn test:defnt-types-for-arg-positions
  [{:keys [lang arglists-types]}])

(defn test:protocol-verify-arglists
  [arglists lang])

(defn test:protocol-verify-unique-first-hint
  [arglists])

(defn test:defnt-gen-helper-macro
  [{:keys [genned-method-name
           genned-protocol-method-name-qualified
           reified-sym-qualified
           strict?
           relaxed?
           sym-with-meta
           args-sym
           args-hinted-sym
           lang]}])

(defn test:defnt-gen-helper-macro-interface-def
  [{:keys [interface-macro-sym-with-meta
           args-sym
           lang
           args-hinted-sym
           sym-with-meta
           reified-sym-qualified
           genned-method-name]}])

(defn test:defnt-gen-final-defnt-def
  [{:keys [lang sym strict? externs genned-protocol-method-name
           gen-interface-def helper-macro-interface-def
           reify-def reified-sym
           helper-macro-def
           protocol-def extend-protocol-def]}])

(defn test:defnt*-helper
  ([opts lang ns- sym doc- meta- body [unk & rest-unk]])
  ([opts lang ns- sym doc- meta- body])))

(defn test:defnt
  [sym & body])

(defn test:defnt'
  [sym & body])

(defn test:defntp
  [sym & body])