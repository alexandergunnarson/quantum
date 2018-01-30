(ns quantum.untyped.core.form.generate.definterface
  (:require
    [clojure.core                        :as core]
    [quantum.untyped.core.form.generate  :as ufgen]
    [quantum.untyped.core.form.type-hint :as uth]))

; Spec for :definterface/method
#_(keys :ret    (? type-hint?)
        :name   method-symbol?
        :inputs (vector-of (tuple param-symbol? type-symbol?)))

(defmethod ufgen/generate ::core/definterface|method
  [_ {:keys [ret name inputs]}]
  (list (uth/with-type-hint name ret)
        (->> inputs
             (mapv (fn [[sym hint]] (uth/with-type-hint sym hint))))))

; Spec for :definterface
; (def method-group-sym? (s/or protocol-sym? interface-sym?))
#_(keys :name    interface-symbol?
        :methods (unkeyed-coll-of :code:definterface:method))

(defmethod ufgen/generate ::core/definterface
  [_ {:keys [name methods]}]
  (list* 'definterface name methods))
