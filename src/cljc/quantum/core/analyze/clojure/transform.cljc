(ns quantum.core.analyze.clojure.transform
           (:refer-clojure :exclude [destructure])
           (:require [#?(:clj  clojure.core
                         :cljs cljs.core   )                  :as core ]
                     [quantum.core.analyze.clojure.predicates
                       :refer [if-statement? cond-statement?
                               when-statement?]                        ]
                     [quantum.core.error                      :as err
                       :refer [->ex]                                   ]
                     [quantum.core.fn                         :as fn
                       :refer [#?@(:clj [fn-> fn->>])]                 ]
                     [quantum.core.logic                      :as logic
                       :refer [#?@(:clj [fn-or if*n condf*n])]         ]
                     [quantum.core.vars                       :as var
                       :refer [#?(:clj defalias)]                      ])
  #?(:cljs (:require-macros
                     [quantum.core.fn                         :as fn
                        :refer [fn-> fn->>]                            ]
                     [quantum.core.logic                      :as logic
                        :refer [fn-or if*n condf*n]                    ]
                     [quantum.core.vars                       :as var
                       :refer [defalias]                               ])))

; TODO COMBINE THESE TWO VIA "UPDATE-N GET"
(def conditional-branches
  (condf*n
    (fn-or if-statement? cond-statement?)
      (fn->> rest
             (partition-all 2)
             (map (if*n (fn-> count (= 2))
                    second
                    first))
             doall)
    when-statement?
      last
    (constantly nil)))
; TODO COMBINE THESE TWO VIA "UPDATE-N GET"
;;(defn map-conditional-branches [f x]
;;  (condf x
;;    (fn-or if-statement? cond-statement?)
;;      (fn->> rest
;;             (partition-all 2)
;;             (map (if*n (fn-> count (= 2))
;;                    (f*n update-nth 1 f)
;;                    (f*n update-nth 0 f)))
;;             (cons (list (first x)))
;;             (apply concat))
;;    when-statement?
;;      (f*n update-last f)
;;    identity))

#?(:clj (defalias destructure core/destructure))

#?(:cljs
(defn destructure
  {:from "clojure.tools.analyzer.js.cljs.core"
   :todo ["Is this in cljs.core such that this fn won't be necessary?"]}
  [bindings]
  (let [bents (partition 2 bindings)
         pb (fn pb [bvec b v]
              (let [pvec
                     (fn [bvec b val]
                       (let [gvec (gensym "vec__")]
                         (loop [ret (-> bvec (conj gvec) (conj val))
                                     n 0
                                     bs b
                                     seen-rest? false]
                           (if (seq bs)
                               (let [firstb (first bs)]
                                 (cond
                                   (= firstb '&) (recur
                                                   (pb ret (second bs) (core/list `nthnext gvec n))
                                                   n
                                                   (nnext bs)
                                                   true)
                                   (= firstb :as) (pb ret (second bs) gvec)
                                   :else (if seen-rest?
                                           (throw (->ex nil "Unsupported binding form, only :as can follow & parameter"))
                                           (recur (pb ret firstb (core/list `nth gvec n nil))
                                                  (core/inc n)
                                                  (next bs)
                                                  seen-rest?))))
                               ret))))
                     pmap
                     (fn [bvec b v]
                       (let [gmap (gensym "map__")
                                  defaults (:or b)]
                         (loop [ret (-> bvec (conj gmap) (conj v)
                                             (conj gmap) (conj `(if (seq? ~gmap) (apply core/hash-map ~gmap) ~gmap))
                                             ((fn [ret]
                                                (if (:as b)
                                                  (conj ret (:as b) gmap)
                                                  ret))))
                                     bes (reduce
                                          (fn [bes entry]
                                            (reduce #(assoc %1 %2 ((val entry) %2))
                                                    (dissoc bes (key entry))
                                                    ((key entry) bes)))
                                          (dissoc b :as :or)
                                          {:keys #(if (core/keyword? %) % (keyword (core/str %))),
                                           :strs core/str, :syms #(core/list `quote %)})]
                           (if (seq bes)
                             (let [bb (key (first bes))
                                        bk (val (first bes))
                                        has-default (contains? defaults bb)]
                               (recur (pb ret bb (if has-default
                                                   (core/list `get gmap bk (defaults bb))
                                                   (core/list `get gmap bk)))
                                      (next bes)))
                             ret))))]
                    (cond
                      (core/symbol? b) (-> bvec (conj (if (namespace b) (symbol (name b)) b)) (conj v))
                      (core/keyword? b) (-> bvec (conj (symbol (name b))) (conj v))
                      (vector? b) (pvec bvec b v)
                      (map? b) (pmap bvec b v)
                      :else (throw (->ex nil (core/str "Unsupported binding form: " b))))))
         process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
        (if (every? core/symbol? (map first bents))
          bindings
          (if-let [kwbs (seq (filter #(core/keyword? (first %)) bents))]
            (throw (->ex :unsupported (core/str "Unsupported binding key:") (ffirst kwbs)))
            (reduce process-entry [] bents))))))