(ns quantum.core.macros.transform
  (:require
    [fast-zip.core                           :as zip]
    [clojure.walk
      :refer [postwalk]]
 #?(:clj [clojure.jvm.tools.analyzer         :as clj-ana])
    [quantum.core.analyze.clojure.core       :as ana
      :refer [type-hint]]
    [quantum.core.analyze.clojure.predicates :as anap]
    [quantum.core.analyze.clojure.transform
      :refer [unhint]]
    [quantum.core.macros.optimization        :as opt]
    [quantum.core.collections.base           :as cbase
      :refer [update-first update-val ensure-set
              zip-reduce* default-zipper nnil?]]
    [quantum.core.error                      :as err
      :refer [->ex]]
    [quantum.core.fn                         :as fn
      :refer [<- fn1 fn-> rcomp]]
    [quantum.core.log                        :as log
      :refer [prl]]
    [quantum.core.logic                      :as logic
      :refer [fn-not fn-or fn-and whenc condf1]]
    [quantum.core.macros.core                :as cmacros]
    [quantum.core.type.core                  :as tcore]))

; TODO should move (some of) these functions to core.analyze.clojure/transform?

(def default-hint {:clj 'java.lang.Object :cljs 'object})

(defn hint-resolved? [x lang env]
  ((fn-or anap/hinted-literal?
     (fn1 anap/type-cast? lang)
     anap/constructor?
     type-hint
     (fn [sym]
       (when env
         (log/ppr :macro-expand/params (str "TRYING TO RESOLVE HINT FOR SYM FROM &env " sym) (keys env))
         (log/pr  :macro-expand/params "SYM" sym "IN ENV?" (contains? env sym))
         (log/pr  :macro-expand/params "ENV TYPE HINT" (-> env (find sym) first type-hint)))
       (-> env (find sym) first type-hint)))
   x))

(defn any-hint-unresolved?
  ([args lang] (any-hint-unresolved? args lang nil))
  ([args lang env]
    {:post [(log/ppr-hints :macro-expand/params "Any hint unresolved?"
              {:unresolved? % :args args})]}
    (some (fn-not #(hint-resolved? % lang env)) args)))

(defn hint-body-with-arglist
  "Hints a code body with the hints in the arglist.
   Assumes code body is a seq of one or more expressions."
  ([body arglist lang] (hint-body-with-arglist body arglist lang nil))
  ([body arglist lang body-type]
  (let [arglist-map (->> arglist
                         (map (fn [sym] [sym (type-hint sym)]))
                         (into {}))
        body-hinted
          (postwalk
            (condf1
              symbol?
                (fn [sym]
                  (let [[hinted hint] (find arglist-map sym)]
                    (if (and hinted
                             (-> hint tcore/prim? not)  ; Because "Can't type hint a primitive local"
                             (-> hint (not= 'Object))
                             (-> hint (not= 'java.lang.Object))
                             (-> sym type-hint not))
                        hinted
                        sym)))
              anap/new-scope?
                (fn [scope]
                  (if-let [sym (->> arglist-map keys
                                    (filter (partial anap/shadows-var? (second scope)))
                                    first)]
                    (throw (->ex :unsupported "Arglist in |let| shadows hinted arg." sym))
                    scope))
              identity)
            body)
        body-unboxed
          (if (= body-type :protocol)
              (list
                (list* 'let
                  (->> arglist ; to preserve order
                       (map (juxt unhint type-hint))
                       (filter (fn-> second tcore/prim?))
                       (map (fn [[sym hint]]
                              [sym (list hint sym)])) ; add primitive type cast in let-binding
                       (apply concat)
                       vec)
                  body-hinted))
              body-hinted)]
    body-unboxed)))

(defn extract-type-hints-from-arglist
  {:tests '{'[w #{Exception} x ^int y ^integer? z]
            '[Object #{Exception} int integer?]}}
  [lang arglist]
  (zip-reduce*
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
  [lang sym {:keys [arglist body]}]
  (let [return-type-0 (or (type-hint arglist) (type-hint sym) #_(-> body ana/expr-info :class) #_(get default-hint lang))
        type-hints (->> arglist (extract-type-hints-from-arglist lang))]
    (vector type-hints return-type-0)))

(def vec-classes-for-count {}
  #_{0 clojure.lang.Tuple$T0
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
  {:post [(log/ppr-hints :macro-expand "ARGS HINTED" %)]}
    (for [arg args]
      #?(:clj  (if (or (anap/hinted-literal? arg) ; can't hint a literal
                       (type-hint arg))
                   arg
                   (if-let [hint (ana/typeof* arg env)]
                     (if (= hint Object) ; Object class
                         arg ; ignore it for now
                         (cmacros/hint-meta arg hint))
                     arg))
         :cljs (cond ; TODO use core analyzer
                 (seq? arg)
                   (if-let [hint (get-in [lang (first arg)] tcore/type-casts-map)]
                     (cmacros/hint-meta arg hint)
                     arg)
                 (vector? arg)
                   ; Otherwise the tag meta is assumed to be
                   ; clojure.lang.IPersistentVector, etc.
                   (cmacros/hint-meta (list `quantum.core.macros.optimization/identity* arg)
                     (or (get vec-classes-for-count (count arg))
                         (case lang
                           :clj  'clojure.lang.PersistentVector
                           :cljs 'cljs.core.PersistentVector)))
                 :else arg)))))

(defn gen-arglist
  {:in  '[abcde hru1 fhacbd]
   :out '[a0 a1 a2]}
  [v]
  (->> v (map-indexed (fn [i elem] (symbol (str "a" i)))) (into [])))
