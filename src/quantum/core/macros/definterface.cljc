(ns quantum.core.macros.definterface
  (:require
    [clojure.core                       :as core]
    [quantum.core.macros.type-hint      :as th]
    [quantum.untyped.core.form.generate :as ufgen]))

; Spec for :definterface/method
#_(keys :ret    (? type-hint?)
        :name   method-symbol?
        :inputs (vector-of (tuple param-symbol? type-symbol?)))

(defmethod ufgen/generate ::core/definterface:method
  [_ {:keys [ret name inputs]}]
  (list (th/with-type-hint name ret)
        (->> inputs
             (mapv (fn [[sym hint]] (th/with-type-hint sym hint))))))

; Spec for :definterface
; (def method-group-sym? (s/or protocol-sym? interface-sym?))
#_(keys :name    interface-symbol?
        :methods (unkeyed-coll-of :code:definterface:method))

(defmethod ufgen/generate ::core/definterface
  [_ {:keys [name methods]}]
  (list* 'definterface name methods))
