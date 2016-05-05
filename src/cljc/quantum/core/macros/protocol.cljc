(ns quantum.core.macros.protocol
           (:require [quantum.core.analyze.clojure.predicates :as anap
                       :refer [type-hint]]
                     [quantum.core.macros.transform :as trans      ]
                     [quantum.core.fn               :as fn
                                :refer [#?@(:clj [f*n fn-> fn->>])]]
                     [quantum.core.log              :as log        ]
                     [quantum.core.logic            :as logic
                       :refer [#?@(:clj [whenp]) nempty? nnil?]    ]
                     [quantum.core.collections.base :as cbase
                       :refer [update-first update-val ensure-set
                               zip-reduce default-zipper]          ]
                     [quantum.core.macros.core      :as cmacros    ]
                     [quantum.core.type.core        :as tcore      ])
  #?(:cljs (:require-macros
                     [quantum.core.fn               :as fn
                       :refer [f*n fn-> fn->>]                     ]
                     [quantum.core.log              :as log        ]
                     [quantum.core.logic            :as logic
                       :refer [whenp]                              ])))

(def ^{:doc "Primitive type hints translated into protocol-safe type hints."}
  protocol-type-hint-map
  '{:clj  {boolean java.lang.Boolean
           byte    long
           char    java.lang.Character
           short   long
           int     long
           float   double}
    :cljs {boolean boolean}})

(defn ensure-protocol-appropriate-type-hint [lang i hint]
  (when-not (and (> i 0) (= hint 'Object)) ; The extra object hints mess things up
    (if-let [protocol-appropriate-type-hint (get-in protocol-type-hint-map [lang hint])]
      protocol-appropriate-type-hint
      hint)))

(defn ensure-protocol-appropriate-arglist [lang arglist-0]
  (->> arglist-0
       (map-indexed
         (fn [i arg]
           (cmacros/hint-meta arg
             (ensure-protocol-appropriate-type-hint lang i (type-hint arg)))))
       (into [])))

(defn gen-protocol-from-interface
  {:in '[[[Func [String IPersistentVector] long]]
         [[Func [String ITransientVector ] long]]]}
  [{:keys [gen-interface-code-body-expanded
           genned-protocol-name
           genned-protocol-method-name]}]
  (let [protocol-def-body
          (->> gen-interface-code-body-expanded
               (map (fn-> first second))
               (group-by count)
               (map (fn-> val first trans/gen-arglist))
               (cons genned-protocol-method-name))
        protocol-def 
          (list 'defprotocol genned-protocol-name protocol-def-body)]
    protocol-def))

; TODO hopefully get rid of this step
#?(:clj 
(defn gen-extend-protocol-from-interface
  ; Original Interface:         ([#{number?} x #{number?} y #{char? Object} z] ~@body)
  ; Expanded Interface Arity 1: ([^long      x ^int       y ^char           z] ~@body)
  ; Protocol Arity 1:           ([^long      x ^Integer   y ^Character      z]
  ;                               (let [y (int y) z (char z)] ; TODO ->int y, ->charz
  ;                                 ~@body)

  ; (def abcde 0)
  ; ((fn [^"[B" x ^long n] (clojure.lang.RT/aget x n)) my-byte-array abcde) => 0, no reflection
  [{:keys [genned-protocol-name genned-protocol-method-name
           reify-body lang first-types]}]
  (log/ppr-hints :macro-expand-protocol "REIFY BODY" reify-body)
  (assert (nempty? reify-body))
  (assert (nnil? genned-protocol-name))
  (assert (nnil? genned-protocol-method-name))

  (let [body-sorted
          (->> reify-body rest rest
               (map (fn [[sym & body]]
                      (-> body (update-first (f*n cmacros/hint-meta (type-hint sym))))))) ; remove reify method names
        body-filtered body-sorted
        _ (log/ppr-hints :macro-expand-protocol "BODY SORTED" body-sorted)
        body-mapped
          (->> body-filtered
               (map (fn [[arglist & body :as method]]
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
                                  return-type (->> arglist type-hint
                                                   (ensure-protocol-appropriate-type-hint lang 0))
                                  arglist-f (->> arglist rest (ensure-protocol-appropriate-arglist lang))
                                  arglist-f (if return-type
                                                arglist-f
                                                (cmacros/hint-meta arglist-f return-type) )
                                  body-f (trans/hint-body-with-arglist body arglist-f lang :protocol)
                                  extension-f [boxed-first-type (cons genned-protocol-method-name (cons arglist-f body-f))]]
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
    extend-protocol-def)))