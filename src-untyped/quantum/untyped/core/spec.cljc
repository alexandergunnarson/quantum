(ns quantum.untyped.core.spec
  (:refer-clojure :exclude
    [ident? string? keyword? set? number? any?
     assert keys merge + * cat and or constantly])
  (:require
    [clojure.core            :as core]
    [clojure.spec.alpha      :as s]
    [clojure.spec.test.alpha :as test]
    [cljs.spec.alpha]
    [clojure.spec.gen.alpha  :as gen]
    [fipp.ednize]
    [quantum.untyped.core.convert :as uconv]
    [quantum.untyped.core.data :as udata]
    [quantum.untyped.core.error
      :refer [catch-all err! TODO]]
    [quantum.untyped.core.fn
      :refer [constantly with-do]]
    [quantum.untyped.core.form.evaluate :as ufeval
      :refer [case-env]]
    [quantum.untyped.core.qualify :as uqual]
    [quantum.untyped.core.type.predicates
      :refer [ident?]]
    [quantum.untyped.core.vars
      :refer [defalias defmalias]])
#?(:cljs
  (:require-macros
    [quantum.untyped.core.spec :as self])))

(s/check-asserts true) ; TODO put this somewhere like a component

(defrecord ValidationError
  [problems failure at-line at-instant form locals invalidated invalidated-type]
  fipp.ednize/IOverride
  fipp.ednize/IEdn
    (-edn [this] (into {} this)))

(def spec-assertion-failed "Spec assertion failed")

(defn -validate-one ; TODO handle errors
  "Like |clojure(script).spec/assert*|, but adds a more detailed
   error message à la |taoensso.truss/have|. Also avoids printing
   the failed value to a string, which can be problematic with e.g.
   large collections or lazy seqs."
  [spec x form locals ns-str ?line kind]
  (let [inst        #?(:clj  (java.util.Date.)
                       :cljs (js/Date.))
        explained   (s/explain-data* spec [] [] [] x)
        data        (ValidationError.
                      (::s/problems explained)
                      kind
                      (str ns-str ":" (core/or ?line "?"))
                      inst
                      form
                      locals
                      x
                      (type x))]
    (err! spec-assertion-failed data)))

(def verbose? false) ; can be `with-redef`ed in tests

#?(:clj
(defmacro validate-one*
  {:todo {0 {:desc     "Optimize validation peformance"
             :priority 1}}}
  [opts spec x]
  (let [error-kind (core/or (:type opts) :spec.validate/failed)]
    (case-env
     :cljs `(if (core/or ~(:runtime? opts) cljs.spec.alpha/*compile-asserts*) ; TODO should probably be outside quote like this
                (if (core/or ~(:runtime? opts) cljs.spec.alpha/*runtime-asserts*)
                    (let [spec# ~spec x# ~x
                          conformed# (cljs.spec.alpha/conform spec# x#)]
                      (if (= conformed# :cljs.spec.alpha/invalid)
                          (if (core/and ~(:terse? opts) (not verbose?))
                              (throw (ex-info spec-assertion-failed {:form (list '~'validate '~x '~spec) :type ~error-kind}))
                              (-validate-one spec# x# (list '~'validate '~x '~spec) (ufeval/locals ~&env) ~(str *ns*) ~(:line (meta &form)) ~error-kind))
                          conformed#))
                   ~x)
               ~x)
      (if (core/or (:runtime? opts) clojure.spec.alpha/*compile-asserts*)
         `(if (core/or ~(:runtime? opts) (clojure.spec.alpha/check-asserts?))
              (let [spec# ~spec x# ~x
                    conformed# (clojure.spec.alpha/conform spec# x#)]
                (if (= conformed# :clojure.spec.alpha/invalid)
                    (if (core/and ~(:terse? opts) (not verbose?))
                        (throw (ex-info spec-assertion-failed {:form (list '~'validate '~x '~spec) :type ~error-kind}))
                        (-validate-one spec# x# (list '~'validate '~x '~spec) (ufeval/locals ~&env) ~(str *ns*) ~(:line (meta &form)) ~error-kind))
                    conformed#))
             ~x)
          x)))))

#?(:clj
(defmacro validate-one [spec x] `(validate-one* nil ~spec ~x)))

#?(:clj
(defmacro validate*
  "Multiple validations performed.
   Keys are values; vals are specs.
   Useful for :pre checks.
   `opts` is an additional argument."
  [opts & args] (core/assert (-> args count even?))
  `(do ~@(->> args (partition-all 2)
                   (map (fn [[v spec]] `(validate-one* ~opts ~spec ~v)))))))

#?(:clj
(defmacro validate
  "Multiple validations performed.
   Keys are values; vals are specs.
   Useful for :pre checks."
  [& args] (core/assert (-> args count even?))
  `(do ~@(->> args (partition-all 2)
                   (map (fn [[v spec]] `(validate-one ~spec ~v)))))))

#?(:clj (quantum.untyped.core.vars/defmalias tuple         clojure.spec.alpha/tuple         cljs.spec.alpha/tuple    ))
#?(:clj (quantum.untyped.core.vars/defmalias coll-of       clojure.spec.alpha/coll-of       cljs.spec.alpha/coll-of  ))
#?(:clj (quantum.untyped.core.vars/defmalias map-of        clojure.spec.alpha/map-of        cljs.spec.alpha/map-of   ))

#?(:clj (quantum.untyped.core.vars/defmalias def           clojure.spec.alpha/def           cljs.spec.alpha/def      ))
#?(:clj (quantum.untyped.core.vars/defmalias fdef          clojure.spec.alpha/fdef          cljs.spec.alpha/fdef     ))

#?(:clj (quantum.untyped.core.vars/defmalias keys          clojure.spec.alpha/keys          cljs.spec.alpha/keys     ))
#?(:clj (quantum.untyped.core.vars/defmalias keys*         clojure.spec.alpha/keys*         cljs.spec.alpha/keys*    ))
#?(:clj (quantum.untyped.core.vars/defmalias merge         clojure.spec.alpha/merge         cljs.spec.alpha/merge    ))

#?(:clj (quantum.untyped.core.vars/defmalias spec          clojure.spec.alpha/spec          cljs.spec.alpha/spec     ))
#?(:clj (quantum.untyped.core.vars/defmalias +             clojure.spec.alpha/+             cljs.spec.alpha/+        ))
#?(:clj (quantum.untyped.core.vars/defmalias *             clojure.spec.alpha/*             cljs.spec.alpha/*        ))
#?(:clj (quantum.untyped.core.vars/defmalias ?             clojure.spec.alpha/?             cljs.spec.alpha/?        ))

;; Note that `and` results in a spec, and as such creates a new regex context :/
#?(:clj (quantum.untyped.core.vars/defmalias and           clojure.spec.alpha/and           cljs.spec.alpha/and      ))
#?(:clj (quantum.untyped.core.vars/defmalias or            clojure.spec.alpha/or            cljs.spec.alpha/or       ))
#?(:clj (quantum.untyped.core.vars/defmalias every         clojure.spec.alpha/every         cljs.spec.alpha/every    ))

#?(:clj (quantum.untyped.core.vars/defmalias conformer     clojure.spec.alpha/conformer     cljs.spec.alpha/conformer))

#?(:clj (quantum.untyped.core.vars/defmalias nilable       clojure.spec.alpha/nilable     cljs.spec.alpha/nilable))

(defalias s/conform)
(defalias s/nonconforming)
(defalias • nonconforming)
(defalias s/explain)
(defalias s/explain-data)
(defalias s/describe)

#?(:clj (quantum.untyped.core.vars/defmalias cat clojure.spec.alpha/cat cljs.spec.alpha/cat))
#?(:clj (defmacro cat* "`or` :`or*` :: `cat` : `cat*`" [& args] `(cat ~@(udata/quote-map-base uconv/>keyword args true))))

#?(:clj (quantum.untyped.core.vars/defmalias alt clojure.spec.alpha/alt cljs.spec.alpha/alt))
#?(:clj (defmacro alt* "`or` :`or*` :: `alt` : `alt*`" [& args] `(alt ~@(udata/quote-map-base uconv/>keyword args true))))

#?(:clj
(defmacro fdef! [sym & args]
  `(with-do (~(case-env :clj 'clojure.spec.alpha/fdef :cljs 'cljs.spec.alpha/fdef) ~sym ~@args)
     (when (s/check-asserts?) (test/instrument '~(uqual/qualify *ns* sym))))))

#?(:clj
(defmacro or-auto
  "Like `spec/or`, except labels aren't required for preds ('auto-labels' preds)."
  [& preds]
  `(or ~@(->> preds (map #(vector (keyword (str %)) %)) (apply concat)))))

; TODO API differs from `invalid?`
(defn valid? [s v] (catch-all (s/valid? s v) _ false))

(defn invalid? ; TODO temporary
  "tests the validity of a conform return value"
  [ret] (#?(:clj identical? :cljs keyword-identical?) ::s/invalid ret))

(defn- pvalid? ; TODO temporary
  "internal helper function that returns true when x is valid for spec."
  ([pred x]
   (not (invalid? (@#'s/dt pred x ::s/unknown))))
  ([pred x form]
   (not (invalid? (@#'s/dt pred x form)))))

(defn or*-spec-impl
  [keys forms preds gfn]
  (let [id (#?(:clj java.util.UUID/randomUUID :cljs random-uuid))
        kps (zipmap keys preds)
        specs (delay (mapv @#'s/specize preds forms))
        cform (case (count preds)
                    2 (fn [x]
                        (let [specs @specs
                              ret (catch-all (s/conform* (specs 0) x) _ ::s/invalid)]
                          (if (invalid? ret)
                            (let [ret (catch-all (s/conform* (specs 1) x) _ ::s/invalid)]
                              (if (invalid? ret)
                                ::s/invalid
                                ret))
                            ret)))
                    3 (fn [x]
                        (let [specs @specs
                              ret (catch-all (s/conform* (specs 0) x) _ ::s/invalid)]
                          (if (invalid? ret)
                            (let [ret (catch-all (s/conform* (specs 1) x) _ ::s/invalid)]
                              (if (invalid? ret)
                                (let [ret (catch-all (s/conform* (specs 2) x) _ ::s/invalid)]
                                  (if (invalid? ret)
                                    ::s/invalid
                                    ret))
                                ret))
                            ret)))
                    (fn [x]
                      (let [specs @specs]
                        (loop [i 0]
                          (if (< i (count specs))
                            (let [spec (specs i)]
                              (let [ret (catch-all (s/conform* spec x) _ ::s/invalid)]
                                (if (invalid? ret)
                                  (recur (inc i))
                                  ret)))
                            ::s/invalid)))))]
    (reify
     #_s/Specize
     #_(specize* [s] s)
     #_(specize* [s _] s)

     s/Spec
     (conform* [_ x] (cform x))
     (unform* [_ [k x]] (s/unform (kps k) x))
     (explain* [this path via in x]
               (when-not (@#'pvalid? this x)
                 (apply concat
                        (map (fn [k form pred]
                               (when-not (@#'pvalid? pred x)
                                 (@#'s/explain-1 form pred (conj path k) via in x)))
                             keys forms preds))))
     (gen* [_ overrides path rmap] ; TODO this may need to fixed? Untested
           (if gfn
             (gfn)
             (let [gen (fn [k p f]
                         (let [rmap (@#'s/inck rmap id)]
                           (when-not (@#'s/recur-limit? rmap id path k)
                             (gen/delay
                              (@#'s/gensub p overrides (conj path k) rmap f)))))
                   gs (remove nil? (map gen keys preds forms))]
               (when-not (empty? gs)
                 (gen/one-of gs)))))
     (with-gen* [_ gfn] (or*-spec-impl keys forms preds gfn))
     (describe* [_] `(or* ~@forms)))))

#?(:clj
(defmacro or*
  "Takes preds, e.g.
   (s/or* even? #(< % 42))
   Returns a spec that returns the first matching pred's value."
  [& pred-forms]
  (let [pf (mapv (case-env :cljs #(@#'cljs.spec.alpha/res &env %) @#'clojure.spec.alpha/res)
                 pred-forms)]
    `(or*-spec-impl '~pred-forms '~pf ~(vec pred-forms) nil))))

#?(:clj
(defmacro or*-forms
  "Like `or*` but allows direct specification of the form to be used in `s/explain`
   regardless of the code to be executed.
   They are expected to be interleaved, quoted0 exec0 quoted1 exec1, etc."
  [& pred-forms]
  (let [partitioned       (partition-all 2 pred-forms)
        _                 (core/assert (-> partitioned count even?))
        pred-forms-quoted (map first  partitioned)
        pred-forms-exec   (map second partitioned)
        pf (mapv (case-env :cljs #(@#'cljs.spec.alpha/res &env %) @#'clojure.spec.alpha/res)
                 pred-forms-exec)]
    `(or*-spec-impl '~pred-forms-quoted '~pf ~(vec pred-forms-exec) nil))))

#?(:clj
(defmacro constantly-or [& exprs]
  `(or* ~@(map #(list 'fn [(gensym "_")] %) exprs))))

#?(:clj (defmacro vec-of [spec & opts] `(coll-of ~spec ~@opts :kind core/vector?)))
#?(:clj (defmacro set-of [spec & opts] `(coll-of ~spec ~@opts :kind core/set?)))
;; Really, `seqable-of`
#?(:clj (defmacro seq-of [spec & opts] `(coll-of ~spec ~@opts)))

(defn validate|val? [x]
  (if (nil? x)
      (throw (ex-info "Value is not allowed to be nil but was" {}))
      x))

(defn assert-conform [spec x]
  (let [conformed (s/conform spec x)]
    (if (s/invalid? conformed)
        (let [ed (core/merge (assoc (s/explain-data* spec [] [] [] x)
                               ::s/failure :assertion-failed))]
          (throw (ex-info (str "Spec assertion failed\n" (with-out-str (s/explain-out ed))) ed)))
        conformed)))

(defn kv
  "Based on `s/map-spec-impl`"
  ([k->s #_(s/map-of any? specable?)] (kv k->s nil))
  ([k->s #_(s/map-of any? specable?) gen-fn #_(? fn?)]
    (let [id (#?(:clj java.util.UUID/randomUUID :cljs random-uuid))
          k->s|desc (->> k->s
                         (map (fn [[k specable]]
                                [k (if (ident? specable) specable (s/describe specable))]))
                         (into {}))]
      (reify
        s/Specize
          (specize* [this] this)
          (specize* [this _] this)
        s/Spec
          (conform* [_ x]
            (reduce
              (fn [x' [k s]]
                (let [v  (get x' k)
                      cv (s/conform s v)]
                  (if (s/invalid? cv)
                      ::s/invalid
                      (if (identical? cv v)
                          x'
                          ;; TODO we might want to do `assoc?!`, depending
                          (assoc x' k cv)))))
              x
              k->s))
          (unform* [_ x]
            (reduce
              (fn [x' [k s]]
                (let [cv (get x' k)
                      v  (s/unform s cv)]
                  (if (identical? cv v)
                      x'
                      ;; TODO we might want to do `assoc?!`, depending
                      (assoc x' k v))))
              x
              k->s))
          (explain* [_ path via in x]
            (if-not ;; TODO we might want a more generalized `map?` predicate like `t/map?`, depending,
                    ;; which would affect more code below
                    (map? x)
              [{:path path :pred 'map? :val x :via via :in in}]
              ;; TODO use reducers?
              (->> k->s
                   (map (fn [[k s]]
                          (let [v (get x k)]
                            (when-not (s/valid? s v)
                              (@#'s/explain-1 (get k->s|desc k) s (conj path k) via (conj in k) v)))))
                   (filter some?)
                   (apply concat))))
          (gen* [_ overrides path rmap]
            (if gen-fn
                (gen-fn)
                (let [rmap (assoc rmap id (inc (core/or (get rmap id) 0)))
                      gen  (fn [[k s]]
                             (when-not (@#'s/recur-limit? rmap id path k)
                               [k (gen/delay (@#'s/gensub s overrides (conj path k) rmap k))]))
                      gens (->> k->s (map gen) (remove nil?) (into {}))]
                  (gen/bind (gen/choose 0 (count gens))
                            (fn [n]
                              (let [args (-> gens seq shuffle)]
                                (->> args
                                     (take n)
                                     (apply concat)
                                     (apply gen/hash-map))))))))
          (with-gen* [_ gen-fn'] (kv k->s gen-fn'))
          (describe* [_] `(kv ~k->s|desc))))))

(defn with-gen-spec-impl
  "Do not call this directly; use 'with-gen-spec'."
  [extract-f extract-f|form gen-spec gen-spec|form]
  (if (fn? gen-spec)
      (let [form      `(with-gen-spec ~extract-f|form ~gen-spec|form)
            gen-spec' (fn [x]
                        (let [spec (gen-spec x)
                              desc (s/describe spec)
                              desc (if (= desc ::s/unknown)
                                       (list 'some-generated-spec gen-spec|form)
                                       desc)]
                          (s/nonconforming (s/and (s/conformer extract-f)
                                                  (@#'s/spec-impl desc spec nil nil)))))]
        (reify
          s/Specize
            (s/specize*  [this] this)
            (s/specize*  [this _] this)
          s/Spec
            (s/conform*  [_ x] (s/conform* (gen-spec' x) x))
            (s/unform*   [_ x] (s/unform* (gen-spec' x) x))
            (s/explain*  [_ path via in x] (s/explain* (gen-spec' x) path via in x))
            (s/gen*      [_ _ _ _] (gen/gen-for-pred gen-spec))
            (s/with-gen* [_ _] (throw (ex-info "TODO" {})))
            (s/describe* [_] form)))
      (throw (ex-info "`wrap-spec` may only be called on fns" {:input gen-spec}))))

#?(:clj
(defmacro with-gen-spec
  "`gen-spec` : an fn that returns a spec based on the input.
   `extract-f`: extracts the piece of data from the input that the generated spec will validate.
   E.g.:
   (s/explain
     (s/with-gen-spec (fn [{:keys [a]}] a) (fn [{:keys [b]}] #(> % b)))
     {:a 1 :b 1})"
  [extract-f gen-spec]
  `(with-gen-spec-impl ~extract-f '~extract-f ~gen-spec '~gen-spec)))
