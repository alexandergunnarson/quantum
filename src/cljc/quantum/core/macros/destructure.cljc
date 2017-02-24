(ns quantum.core.macros.destructure
  (:refer-clojure :exclude [destructure])
  (:require
    [clojure.core :as core]
    [quantum.core.collections.core]
    [quantum.core.vars
      :refer [#?(:clj reset-var!)]]))

(def destructure-orig @#'core/destructure)

; TODO cljs destructuring edit
#?(:clj
(defn destructure [bindings]
  (let [bents (partition 2 bindings)
        pb (fn pb [bvec b v]
             (let [pseq ; Renamed this pvec -> pseq
                   (fn [bvec b val]
                     (let [gvec (gensym "vec__")
                           gseq (gensym "seq__")
                           gfirst (gensym "first__")
                           has-rest (some #{'&} b)]
                       (loop [ret (let [ret (conj bvec gvec val)]
                                    (if has-rest
                                      (conj ret gseq (list `seq gvec))
                                      ret))
                              n 0
                              bs b
                              seen-rest? false]
                         (if (seq bs)
                           (let [firstb (first bs)]
                             (cond
                              (= firstb '&) (recur (pb ret (second bs) gseq)
                                                   n
                                                   (nnext bs)
                                                   true)
                              (= firstb :as) (pb ret (second bs) gvec)
                              :else (if seen-rest?
                                      (throw (new Exception "Unsupported binding form, only :as can follow & parameter"))
                                      (recur (pb (if has-rest
                                                   (conj ret
                                                         gfirst `(first ~gseq)
                                                         gseq `(next ~gseq))
                                                   ret)
                                                 firstb
                                                 (if has-rest
                                                   gfirst
                                                   (list `nth gvec n nil)))
                                             (inc n)
                                             (next bs)
                                             seen-rest?))))
                           ret))))
                   pvec ; Added this
                   (fn [bvec b val]
                     (let [gvec (gensym "vec__")
                           gfirst (gensym "first__")
                           gorig (gensym "orig__")
                           has-rest (some #{'&} b)]
                       (loop [ret (conj bvec gorig val gvec gorig)
                              n 0
                              bs b
                              seen-rest? false]
                         (if (seq bs)
                           (let [firstb (first bs)]
                             (cond
                              (= firstb '&) (recur (pb (conj ret gvec `(clojure.core/drop ~n ~gvec))
                                                       (second bs)
                                                       gvec)
                                                   n
                                                   (nnext bs)
                                                   true)
                              (= firstb :as) (pb ret (second bs) gorig)
                              :else (if seen-rest?
                                      (throw (new Exception "Unsupported binding form, only :as can follow & parameter"))
                                      (recur (pb ret
                                                 firstb
                                                 (list `quantum.core.collections.core/nth gvec n))
                                             (inc n)
                                             (next bs)
                                             seen-rest?))))
                           ret))))
                   pmap
                   (fn [bvec b v]
                     (let [gmap (gensym "map__")
                           gmapseq (with-meta gmap {:tag 'clojure.lang.ISeq})
                           defaults (:or b)]
                       (loop [ret (-> bvec (conj gmap) (conj v)
                                      (conj gmap) (conj `(if (seq? ~gmap) (clojure.lang.PersistentHashMap/create (seq ~gmapseq)) ~gmap))
                                      ((fn [ret]
                                         (if (:as b)
                                           (conj ret (:as b) gmap)
                                           ret))))
                              bes (let [transforms
                                          (reduce
                                            (fn [transforms mk]
                                              (if (keyword? mk)
                                                (let [mkns (namespace mk)
                                                      mkn (name mk)]
                                                  (cond (= mkn "keys") (assoc transforms mk #(keyword (or mkns (namespace %)) (name %)))
                                                        (= mkn "syms") (assoc transforms mk #(list `quote (symbol (or mkns (namespace %)) (name %))))
                                                        (= mkn "strs") (assoc transforms mk str)
                                                        :else transforms))
                                                transforms))
                                            {}
                                            (keys b))]
                                    (reduce
                                        (fn [bes entry]
                                          (reduce #(assoc %1 %2 ((val entry) %2))
                                                   (dissoc bes (key entry))
                                                   ((key entry) bes)))
                                        (dissoc b :as :or)
                                        transforms))]
                         (if (seq bes)
                           (let [bb (key (first bes))
                                 bk (val (first bes))
                                 local (if (instance? clojure.lang.Named bb) (with-meta (symbol nil (name bb)) (meta bb)) bb)
                                 bv (if (contains? defaults local)
                                      (list `quantum.core.collections.core/get gmap bk (defaults local))
                                      (list `quantum.core.collections.core/get gmap bk))]
                             (recur (if (or (keyword? bb) (symbol? bb))
                                      (-> ret (conj local bv))
                                      (pb ret bb bv))
                                    (next bes)))
                           ret))))]
               (cond
                (symbol? b) (-> bvec (conj b) (conj v))
                (vector? b) (pvec bvec b v)
                (map?    b) (pmap bvec b v)
                (seq?    b) (pseq bvec b v) ; Added this
                :else (throw (new Exception (str "Unsupported binding form: " b))))))
        process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
    (if (every? symbol? (map first bents))
      bindings
      (reduce process-entry [] bents)))))

; ===== ALTERATION ===== ;
; TODO determine under what circumstances to conditionally run this

#?(:clj (reset-var! #'clojure.core/destructure destructure))

(in-ns 'clojure.core)

#?(:clj
(defmacro let
  "binding => binding-form init-expr

  Evaluates the exprs in a lexical context in which the symbols in
  the binding-forms are bound to their respective init-exprs or parts
  therein.

  Now allows destructuring of arrays and other similarly primitive
  collections in a maximally efficient way, as if done by hand."
  {:added "1.0", :special-form true, :forms '[(let [bindings*] exprs*)]
   :example '(let [[{:as a :keys [b c]} d & (e f) :as all]
                     (quantum.core.data.Array/new1dObjectArray {:b 1.0 :c 2.0} 3.0 4.0 5.0)]
               (+ b c d e f))}
  [bindings & body]
  (assert-args
     (vector? bindings) "a vector for its binding"
     (even? (count bindings)) "an even number of forms in binding vector")
  `(let* ~(#'quantum.core.macros.destructure/destructure bindings) ~@body)))

(in-ns 'quantum.core.macros.destructure)
