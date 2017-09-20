(ns quantum.core.spec
  (:refer-clojure :exclude
    [string? keyword? set? number? fn? any?
     assert keys merge + * cat and or constantly])
  (:require
    [clojure.core            :as core]
    [clojure.spec.alpha      :as s]
    [clojure.spec.test.alpha :as test]
    [cljs.spec.alpha]
    [clojure.spec.gen.alpha  :as gen]
    [quantum.core.core       :as qcore
      :refer [val?]]
    [quantum.core.error      :as err
      :refer [catch-all]]
    [quantum.core.fn
      :refer [fnl constantly with-do]]
    [quantum.core.logic
      :refer [fn-not]]
    [quantum.core.macros.core
      :refer [case-env locals]]
    [quantum.core.vars :as var
      :refer [defalias defmalias]]
  #?(:cljs [figwheel.client.utils])
  #?(:cljs [figwheel.client.socket])
  [#?(:clj org.httpkit.client :cljs cljs-http.client) :as http])
#?(:cljs
  (:require-macros
    [quantum.core.spec :as self])))

(s/check-asserts true) ; TODO put this somewhere like a component

; #?(:clj (defmacro random [] (case-env :cljs :cljs :other)))
; #?(:cljs (enable-console-print!))
; #?(:cljs (set! figwheel.client.utils/*print-debug* true))
; #?(:cljs (js/console.error "FROM js/console.error"))
; #?(:cljs (figwheel.client.utils/debug-prn "FROM figwheel.client.utils/debug-prn"))
; #?(:cljs (figwheel.client.utils/log :debug "FROM figwheel.client.utils/log"))
; #?(:cljs (quantum.core.log/prl :warn "THIS IS CLJS ENV" (quantum.core.spec/random)))
; #?(:cljs (figwheel.client.socket/send! "Sent this message!! :D"))
; #?(:clj  (http/post "http://localhost:8081" {:body (str "This is a test body from " :clj)}))
; #?(:cljs (http/post "http://localhost:8081" {:body (str "This is a test body from " :cljs)}))

(defrecord ValidationError
  [problems failure at-line at-instant form locals invalidated invalidated-type])

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
    (throw (ex-info spec-assertion-failed data))))

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
                              (-validate-one spec# x# (list '~'validate '~x '~spec) (locals ~&env) ~(str *ns*) ~(:line (meta &form)) ~error-kind))
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
                        (-validate-one spec# x# (list '~'validate '~x '~spec) (locals ~&env) ~(str *ns*) ~(:line (meta &form)) ~error-kind))
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

#?(:clj (quantum.core.vars/defmalias tuple     clojure.spec.alpha/tuple     cljs.spec.alpha/tuple    ))
#?(:clj (quantum.core.vars/defmalias coll-of   clojure.spec.alpha/coll-of   cljs.spec.alpha/coll-of  ))
#?(:clj (quantum.core.vars/defmalias map-of    clojure.spec.alpha/map-of    cljs.spec.alpha/map-of   ))

#?(:clj (quantum.core.vars/defmalias def       clojure.spec.alpha/def       cljs.spec.alpha/def      ))
#?(:clj (quantum.core.vars/defmalias fdef      clojure.spec.alpha/fdef      cljs.spec.alpha/fdef     ))

#?(:clj (quantum.core.vars/defmalias keys      clojure.spec.alpha/keys      cljs.spec.alpha/keys     ))
#?(:clj (quantum.core.vars/defmalias keys*     clojure.spec.alpha/keys*     cljs.spec.alpha/keys*    ))
#?(:clj (quantum.core.vars/defmalias merge     clojure.spec.alpha/merge     cljs.spec.alpha/merge    ))

#?(:clj (quantum.core.vars/defmalias spec      clojure.spec.alpha/spec      cljs.spec.alpha/spec     ))
#?(:clj (quantum.core.vars/defmalias +         clojure.spec.alpha/+         cljs.spec.alpha/+        ))
#?(:clj (quantum.core.vars/defmalias *         clojure.spec.alpha/*         cljs.spec.alpha/*        ))
#?(:clj (quantum.core.vars/defmalias ?         clojure.spec.alpha/?         cljs.spec.alpha/?        ))

;; Note that `and` results in a spec, and as such creates a new regex context :/
#?(:clj (quantum.core.vars/defmalias and       clojure.spec.alpha/and       cljs.spec.alpha/and      ))
#?(:clj (quantum.core.vars/defmalias or        clojure.spec.alpha/or        cljs.spec.alpha/or       ))
#?(:clj (quantum.core.vars/defmalias every     clojure.spec.alpha/every     cljs.spec.alpha/every    ))

#?(:clj (quantum.core.vars/defmalias conformer clojure.spec.alpha/conformer cljs.spec.alpha/conformer))
(defalias conform s/conform)
(defalias explain s/explain)

#?(:clj (quantum.core.vars/defmalias cat clojure.spec.alpha/cat cljs.spec.alpha/cat))
#?(:clj (defmacro cat* "`or` :`or*` :: `cat` : `cat*`" [& args] `(cat ~@(qcore/quote-map-base qcore/->keyword args true))))

#?(:clj (quantum.core.vars/defmalias alt clojure.spec.alpha/alt cljs.spec.alpha/alt))
#?(:clj (defmacro alt* "`or` :`or*` :: `alt` : `alt*`" [& args] `(alt ~@(qcore/quote-map-base qcore/->keyword args true))))

#?(:clj
(defmacro fdef! [sym & args]
  `(with-do (~(case-env :clj 'clojure.spec.alpha/fdef :cljs 'cljs.spec.alpha/fdef) ~sym ~@args)
     (when (s/check-asserts?) (test/instrument '~(var/qualify *ns* sym))))))

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

#?(:clj
(defmacro set-of [spec] ; TODO fix this up...
  `(let [spec# ~spec]
     (or*-forms (and core/set? (coll-of ~spec))
                (and core/set? (coll-of spec#))
                (coll-of ~spec :distinct true :into #{})
                (coll-of spec# :distinct true :into #{})))))

(defn validate:val? [x]
  (if (nil? x)
      (throw (ex-info "Value is not allowed to be nil but was" {}))
      x))

(def any? (constantly true))
