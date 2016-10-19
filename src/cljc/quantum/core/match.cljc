(ns quantum.core.match
  (:refer-clojure :exclude [+ * cat])
  (:require
 #?(:clj
     [net.cgrand.seqexp            :as se])
     [quantum.core.fn              :as fn
       :refer        [#?@(:clj [fn-> fn$])]
       :refer-macros [          fn-> fn$]]
     [quantum.core.vars            :as var
       :refer        [#?@(:clj [defalias])]
       :refer-macros [          defalias]]
     [quantum.core.logic
       :refer        [#?@(:clj [fn-not fn-and fn-or whenf1 condf1])]
       :refer-macros [          fn-not fn-and fn-or whenf1 condf1]]
     [quantum.core.collections      :as coll]
     [quantum.core.collections.tree :as tree]
     [quantum.core.collections.zip  :as zip]))

; Regex seq matching

; TODO fix the performance implications of multiple apply and varargs
(defn wrap-eq [f]
  (fn [& args]
    (apply f (map (whenf1 (fn-not (fn-or fn? (fn$ instance? net.cgrand.seqexp.Pattern)))
                    (fn [x] #(= % x))) ; non-fns are wrapped in =
                  args))))

#?(:clj (def &      (wrap-eq se/cat )))
#?(:clj (def ?      (wrap-eq se/?   )))
#?(:clj (def |      (wrap-eq se/|   )))
#?(:clj (def +      (wrap-eq se/+   )))
#?(:clj (def *      (wrap-eq se/*   )))
#?(:clj (def ?=     (wrap-eq se/?=  )))
#?(:clj (def ?!     (wrap-eq se/?!  )))
#?(:clj (defalias   re-match se/exec))
#?(:clj (defalias   _        se/_   ))
#?(:clj (defalias   as       se/as  ))


#?(:clj
(defn re-match-whole [preds x]
  (let [ret (re-match preds x)]
    (when (empty? (:rest ret)) ret))))

(def defs
  (let [defs-syms '#{& ? | + * ?= ?! _}]
    (->> (zipmap defs-syms
                 (mapv (fn$ var/qualify 'quantum.core.match) defs-syms))
         (apply concat) vec)))

#?(:clj (defmacro re-match*       [x preds] `(re-match       (let ~defs ~preds) ~x)))
#?(:clj (defmacro re-match-whole* [x preds] `(re-match-whole (let ~defs ~preds) ~x)))

