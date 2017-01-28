(ns quantum.core.macros.protocol
  (:require
    [quantum.core.analyze.clojure.predicates :as anap
      :refer [type-hint]]
    [quantum.core.analyze.clojure.transform
      :refer [unhint]]
    [quantum.core.macros.transform           :as trans]
    [quantum.core.fn                         :as fn
      :refer [fn1 fn-> fn->>]]
    [quantum.core.log                        :as log]
    [quantum.core.logic                      :as logic
      :refer [whenp]]
    [quantum.core.collections.base           :as cbase
      :refer [kmap update-first update-val nempty? nnil? ensure-set]]
    [quantum.core.macros.core                :as cmacros]
    [quantum.core.type.core                  :as tcore]))

(def ^{:doc "Primitive type hints translated into protocol-safe type hints."}
  protocol-type-hint-map
  '{:clj  {boolean java.lang.Boolean
           byte    long
           char    java.lang.Character
           short   long
           int     long
           float   double}
    :cljs {boolean boolean}})

(defn ensure-protocol-appropriate-type-hint
  [arg lang i]
  (let [unhinted (unhint    arg)
        hint-0   (type-hint arg)
        ; hint-f   (get-in protocol-type-hint-map [lang hint])
        ]
    (if (or (= hint-0 'Object)
            (= hint-0 'java.lang.Object) ; The extra object hints mess things up
            (-> protocol-type-hint-map (get lang) (get hint-0)))
        unhinted ; just remove the hint — don't "upgrade" it
        (cmacros/hint-meta arg hint-0))))

(defn ensure-protocol-appropriate-arglist
  [lang arglist-0]
  (->> arglist-0
       (map-indexed
         (fn [i arg] (ensure-protocol-appropriate-type-hint arg lang i)))
       (into [])))

(defn append-variant-identifier
  "Appends a variant identifier to a(n e.g. protocol) symbol.
   Used in auto-generating protocol names for additional dispatch arguments other than the first."
  [sym n]
  (assert (symbol? sym))
  (symbol (namespace sym)
          (str (name sym) "__" n)))

(defn gen-defprotocol-from-interface
  {:in '[[[Func [String IPersistentVector] long]]
         [[Func [String ITransientVector ] long]]]}
  [{:keys [genned-protocol-name
           genned-protocol-method-name
           arities]
    :as env}]
  {:post [(do (log/ppr-hints :macro-expand "PROTOCOL DEF" %)
              true)]}
  (let [distinct-arities (->> arities
                              (map (fn-> first trans/gen-arglist))
                              distinct
                              (sort-by count >))
        protocol-def
          (->> (reductions conj '() distinct-arities)
               rest
               (map-indexed
                 (fn [i arities]
                   (let [variant-identifier (- (count distinct-arities) i)
                         genned-protocol-method-name-variant
                          (if (= i (-> distinct-arities count dec))
                              genned-protocol-method-name
                              (append-variant-identifier genned-protocol-method-name
                                variant-identifier))]
                     (cons genned-protocol-method-name-variant arities))))
               (list* 'defprotocol genned-protocol-name))
        genned-protocol-method-names
          (->> protocol-def
               rest rest
               (map first))]
    (kmap protocol-def genned-protocol-method-names)))

; TODO hopefully get rid of this step
; TODO break it up

(defn gen-extend-protocol-from-interface
  {:todo #{"IMPORTANT: change `extend-protocol` to `extend-type` so that CLJS runs faster"}}
  ; Original Interface:         ([#{number?} x #{number?} y #{char? Object} z] ~@body)
  ; Expanded Interface Arity 1: ([^long      x ^int       y ^char           z] ~@body)
  ; Protocol Arity 1:           ([^long      x ^Integer   y ^Character      z]
  ;                               (let [y (int y) z (char z)] ; TODO ->int y, ->charz
  ;                                 ~@body)

  ; (def abcde 0)
  ; ((fn [^"[B" x ^long n] (clojure.lang.RT/aget x n)) my-byte-array abcde) => 0, no reflection
  [{:keys [genned-protocol-name genned-protocol-method-name
           reify-body lang first-types]}]
  (assert (nempty? reify-body))
  (assert (nnil? genned-protocol-name))
  (assert (nnil? genned-protocol-method-name))

  (let [body-sorted
          (->> reify-body rest rest
               (map (fn [[sym & body]]
                      (-> body (update-first (fn1 cmacros/hint-meta (type-hint sym))))))) ; remove reify method names
        body-filtered body-sorted
        _ (log/ppr-hints :macro-expand-protocol "BODY SORTED" body-sorted)
        body-mapped
          (->> body-filtered
               (map (fn [[arglist & body :as method]]
                      (log/ppr-hints :macro-expand "IN BODY MAPPED" (kmap arglist body))
                      (let [first-type         (-> arglist second type-hint)
                            first-type-unboxed (-> first-type tcore/->unboxed)
                            unboxed-version-exists? (get-in first-types [first-type-unboxed (-> arglist count dec)])]
                        (if ; Unboxed version of arity already exists? Skip generation of that type/arity
                            (and (tcore/boxed? first-type) unboxed-version-exists?) ; |dec| because 'this' is the first arg
                            [] ; Empty so concat gets nothing
                            (let [boxed-first-type (-> first-type (whenp (= lang :clj) tcore/->boxed))
                                             ; (whenc (-> arglist second type-hint tcore/->boxed)
                                             ;        (fn-> name (= "[Ljava.lang.Object;"))
                                             ;   '(Class/forName "[Ljava.lang.Object;"))
                                  return-type (-> arglist
                                                  (ensure-protocol-appropriate-type-hint lang 0))
                                  arglist-f   (->> arglist rest (ensure-protocol-appropriate-arglist lang))
                                  arglist-f   (if return-type
                                                  arglist-f
                                                  (cmacros/hint-meta arglist-f return-type) )
                                  body-f      (trans/hint-body-with-arglist body arglist lang :protocol)
                                  extension-f [boxed-first-type (cons genned-protocol-method-name (cons arglist-f body-f))]]
                              (log/ppr-hints :macro-expand "IN BODY AFTER MAPPED" (kmap boxed-first-type extension-f first-type))
                              (if (or (= boxed-first-type 'Object)
                                      (= boxed-first-type 'java.lang.Object))
                                  (into ['nil (-> extension-f rest first)] extension-f)
                                  extension-f))))))
               (apply concat)
               (partition 2))
        body-grouped
          (->> body-mapped
               (group-by first)
               (map (fn [[k v]] (->> v (map (fn->> second rest)) (cons genned-protocol-method-name) (list k))))
               (map (partial into [])))
        _ (log/ppr-hints :macro-expand-protocol "BODY GROUPED" body-grouped)
        extend-protocol-def
         (apply concat (list 'extend-protocol genned-protocol-name) body-grouped)]
    extend-protocol-def))

(log/disable! :macro-expand)
