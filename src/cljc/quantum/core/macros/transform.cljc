(ns quantum.core.macros.transform
  (:require-quantum [:core fn logic cmacros log tcore err cbase])
  (:require [fast-zip.core :as zip]
            [quantum.core.analyze.clojure.predicates :as anap
              :refer [type-hint]]
            [clojure.walk
              :refer [postwalk]]
            [quantum.core.collections.base
              :refer [update-first update-val ensure-set
                      zip-reduce default-zipper]]))

; TODO should move (some of) these functions to core.analyze.clojure/transform?

(def default-hint {:clj 'Object :cljs 'object})


(defn any-hint-unresolved?
  ([args lang] (any-hint-unresolved? args lang nil))
  ([args lang env]
    (any? (fn-not (fn-or anap/hinted-literal?
                         (f*n anap/type-cast? lang)
                         anap/constructor?
                         type-hint
                         (fn [sym]
                           (when env
                             (log/ppr :macro-expand (str "TRYING TO RESOLVE HINT FOR SYM FROM &env " sym) env)
                             (log/pr  :macro-expand "SYM" sym "IN ENV?" (contains? env sym))
                             (log/pr  :macro-expand "ENV TYPE HINT" (-> env (find sym) first type-hint)))
                           (-> env (find sym) first type-hint))))
          args)))


(defn hint-body-with-arglist
  "Hints a code body with the hints in the arglist."
  ([body arglist lang] (hint-body-with-arglist body arglist lang nil))
  ([body arglist lang body-type]
  (let [arglist-set (into #{} arglist)
        body-hinted 
          (postwalk 
            (condf*n
              symbol?
                (fn [sym]
                  (if-let [arg (get arglist-set sym)]
                    (if (tcore/primitive? (type-hint arg)) ; Because "Can't type hint a primitive local"
                        sym
                        arg)
                    sym)) ; replace it
              anap/new-scope?
                (fn [scope]
                  (if-let [sym (->> arglist-set
                                    (filter (partial anap/shadows-var? (second scope)))
                                    first)]
                    (throw (->ex :unsupported "Arglist in |let| shadows hinted arg." sym))
                    scope))
              :else identity)
            body)
        body-unboxed
          (if (= body-type :protocol)
              body-hinted ; TODO? add (let [x (long x)] ...) unboxing
              body-hinted)]
    body-unboxed)))

(defn extract-type-hints-from-arglist
  {:tests '{'[w #{Exception} x ^int y ^integer? z]
            '[Object #{Exception} int integer?]}}
  [lang arglist]
  (zip-reduce
    (fn [type-hints z]
      (let [type-hint-n
             (cond
               (-> z zip/node symbol?)
                 (when-not ((fn-and nnil? (fn-or (fn-> zip/node set?)
                                                 (fn-> zip/node keyword?)))
                            (-> z zip/left))
                   (whenc (-> z zip/node meta :tag) nil?
                     (get default-hint lang))) ; Used to be :any... creates too many fns though
               (or (-> z zip/node set?)
                   (-> z zip/node keyword?))
                 (zip/node z))]
        (if type-hint-n
            (conj type-hints type-hint-n)
            type-hints)))
    []
    (-> arglist default-zipper)))

(defn extract-all-type-hints-from-arglist
  [lang sym arglist]
  (let [return-type-0 (or (type-hint arglist) (type-hint sym) 'Object)
        type-hints (->> arglist (extract-type-hints-from-arglist lang))]
    (->> type-hints
         (<- vector return-type-0))))

; TODO do CLJS version
(def vec-classes-for-count
  '{0 clojure.lang.Tuple$T0
    1 clojure.lang.Tuple$T1
    2 clojure.lang.Tuple$T2
    3 clojure.lang.Tuple$T3
    4 clojure.lang.Tuple$T4
    5 clojure.lang.Tuple$T5
    6 clojure.lang.Tuple$T6})

(defn try-hint-args
  {:todo ["Symbol resolution and hinting, etc."]}
  ([args lang] (try-hint-args args lang nil))
  ([args lang env]
    (for [arg args]
      (cond
        (seq? arg)
          (if-let [hint (get-in [lang (first arg)] tcore/type-casts-map)]
            (cmacros/hint-meta arg hint)
            arg)
        (vector? arg)
          ; Otherwise the tag meta is assumed to be 
          ; clojure.lang.IPersistentVector, etc.
          ; TODO do CLJS version
          (cmacros/hint-meta (list 'identity arg)
            (or (get vec-classes-for-count (count arg))
                'clojure.lang.PersistentVector))
        :else arg))))

(defn gen-arglist
  {:in  '[abcde hru1 fhacbd]
   :out '[a0 a1 a2]}
  [v]
  (->> v (map-indexed (fn [i elem] (symbol (str "a" i)))) (into [])))
