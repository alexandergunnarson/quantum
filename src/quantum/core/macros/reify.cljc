(ns quantum.core.macros.reify
  (:require
    [quantum.core.analyze.clojure.core :as ana
      :refer [type-hint]]
    [quantum.core.collections.base     :as cbase
      :refer [update-first update-val ensure-set kw-map nempty?]]
    [quantum.core.error                :as err
      :refer [->ex]]
    [quantum.core.fn                   :as fn
      :refer [fn-> fn->> <-]]
    [quantum.core.log                  :as log]
    [quantum.core.logic                :as logic
      :refer [whenc fn-and]]
    [quantum.core.macros.core          :as cmacros]
    [quantum.core.macros.transform     :as trans]))

(defn gen-reify-def
  [{:keys [ns- sym ns-qualified-interface-name reify-body]}]
  (let [reified-sym (-> sym name
                        (str "-reified")
                        symbol)
        reified-sym-qualified
          (-> (symbol (name (ns-name ns-)) (name reified-sym))
              (cmacros/hint-meta ns-qualified-interface-name))
        reify-body-relevant
          (->> reify-body (filter (fn-and (fn-> meta :default not)
                                          (fn-> meta :nil?    not))))
        reify-def
          (list 'def reified-sym reify-body-relevant)]
    (kw-map reified-sym
            reified-sym-qualified
            reify-def)))

(defn gen-reify-body-unverified
  [{:keys [ns-qualified-interface-name
           genned-method-name
           gen-interface-code-body-expanded]}]
  (apply list 'reify ns-qualified-interface-name
    (->> gen-interface-code-body-expanded
         (map (fn [arity]
                (let [[hints body] arity
                      return-type-hinted-method
                       (cmacros/hint-meta genned-method-name (last hints))
                      arglist-n    (->> body first (into ['this]))
                      body-f       (->  body rest (trans/hint-body-with-arglist (first body) :clj))
                      updated-body (->> body-f (cons arglist-n))]
                  (with-meta
                    (cons return-type-hinted-method updated-body)
                    (meta arity)))))))) ; to pass on :default and :nil?

(defn verify-reify-body [reify-body sym]
  (let [; To handle ClassFormatError "Duplicate method name&signature"
        duplicate-methods
          (->> reify-body rest rest
               (map (fn-> rest
                         (update-first
                           (fn->> rest
                                  (mapv (fn-> type-hint (whenc nil? trans/default-hint)))))))
               (cbase/frequencies-by first)
               (group-by val)
               (<- dissoc 1))
        _ (when (nempty? duplicate-methods)
            (log/pr        :always "Duplicate methods for" sym ":")
            (log/ppr-hints :always duplicate-methods)
            (throw (->ex "Duplicate methods")))]
    reify-body))

(defn gen-reify-body
  [{:as args
    :keys [sym
           ns-qualified-interface-name
           genned-method-name
           gen-interface-code-body-expanded]}]
  {:post [(log/ppr-hints :macro-expand "REIFY BODY" %)]}
  (-> (gen-reify-body-unverified args)
      (verify-reify-body sym)))
