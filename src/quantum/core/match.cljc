(ns quantum.core.match
  (:refer-clojure :exclude [+ * cat])
  (:require
 #?@(:clj
    [[net.cgrand.seqexp                :as se]
     [clojure.core.match               :as match]])
     [quantum.core.fn                  :as fn
       :refer [<- fn-> fnl]]
     [quantum.untyped.core.qualify     :as qual]
     [quantum.core.vars                :as var
       :refer [defalias]]
     [quantum.core.logic
       :refer [fn-not fn-and fn-or whenf1 condf1]]
     [quantum.core.collections         :as coll
       :refer [postwalk map-vals+ join]]
     [quantum.core.macros
       :refer [macroexpand-all]]
     [quantum.core.collections.tree    :as tree]
     [quantum.core.collections.zippers :as zip]))

; Regex seq matching

; TODO fix the performance implications of multiple apply and varargs
(defn wrap-eq [f]
  (fn [& args]
    (apply f (map (whenf1 (fn-not (fn-or fn? (fnl instance? net.cgrand.seqexp.Pattern)))
                    (fn [x] #(= % x))) ; non-fns are wrapped in =
                  args))))

#?(:clj (def &      (wrap-eq  se/cat )))
#?(:clj (def ?      (wrap-eq  se/?   )))
#?(:clj (def |      (wrap-eq  se/|   )))
#?(:clj (def +      (wrap-eq  se/+   )))
#?(:clj (def *      (wrap-eq  se/*   )))
#?(:clj (def ?=     (wrap-eq  se/?=  )))
#?(:clj (def ?!     (wrap-eq  se/?!  )))
#?(:clj (defalias   re-match* se/exec))
#?(:clj (defalias   _         se/_   ))
#?(:clj (defalias   as        se/as  ))


#?(:clj
(defn re-match-whole* [preds x]
  (let [ret (re-match* preds x)]
    (when (empty? (:rest ret)) ret))))

(def defs
  (let [defs-syms '#{& ? | + * ?= ?! _}]
    (->> (zipmap defs-syms
                 (mapv (fnl qual/qualify 'quantum.core.match) defs-syms))
         (apply concat) vec)))

#?(:clj
(defn re-match-whole-with-found*
  [found preds x]
  (let [matched (re-match-whole* preds x)]
    (when matched (swap! found merge (-> matched (dissoc :rest :match))))
    matched)))

(defn replace-cats [found-sym pattern]
  (let [cat? (fn-and seq? (fn-> first symbol?) (fn-> first name (= "&")))
        replace-inner-cats
         (fnl postwalk (whenf1 cat? (fnl list 'partial `re-match-whole-with-found* found-sym)))]
    (if (cat? pattern)
        (list* (first pattern) (map replace-inner-cats (rest pattern)))
        (replace-inner-cats pattern))))

#?(:clj
(defmacro re-match-variant [f x preds]
  (let [found    (gensym "found")
        expanded (macroexpand-all preds)]
    `(let [~found   (atom {})
           matched# (~f (let ~defs ~(replace-cats found expanded)) ~x)
           merged#  (merge @~found matched#)]
       (->> merged#
            (<- (dissoc :rest :match))
            (map-vals+ first)
            (join {})
            (<- (merge (select-keys merged# [:rest :match]))))))))

#?(:clj (defmacro re-match       [x preds] `(re-match-variant ~`re-match*       ~x ~preds)))
#?(:clj (defmacro re-match-whole [x preds] `(re-match-variant ~`re-match-whole* ~x ~preds)))
#?(:clj (defalias match re-match-whole))

; TODO fix naming here
#?(:clj
 (defmacro core-match? [test-expr pred-clause]
  `(match/match ~test-expr ~pred-clause true :else false)))
