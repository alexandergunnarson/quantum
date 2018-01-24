(ns quantum.core.macros.protocol
  (:refer-clojure :exclude [contains?])
  (:require
    [quantum.core.macros.transform           :as trans]
    [quantum.core.macros.type-hint           :as th]
    [quantum.core.fn                         :as fn
      :refer [fn1 fn-> fn->>]]
    [quantum.core.log                        :as log]
    [quantum.core.logic                      :as logic
      :refer [whenp]]
    [quantum.core.type.core                  :as tcore]
    [quantum.untyped.core.collections        :as ucoll
      :refer [contains? update-first]]
    [quantum.untyped.core.data
      :refer [kw-map]]
    [quantum.untyped.core.type.predicates
      :refer [val?]]))

(defn with-protocol-arglist-type-hint
  [sym lang arglist-ct]
  (th/with-fn-arglist-type-hint sym lang
    (or arglist-ct 0) ; TODO fix — this is a hack to get around checking return types
    false))

(defn with-protocol-appropriate-arglist
  [lang arglist-0]
  (->> arglist-0
       (map-indexed
         (fn [i arg] (with-protocol-arglist-type-hint arg lang (count arglist-0))))
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
           full-arities]
    :as env}]
  {:post [(do (log/ppr-hints :macro-expand "PROTOCOL DEF" %)
              true)]}
  (let [distinct-arities (->> full-arities
                              (map (fn-> first trans/gen-arglist))
                              distinct
                              (sort-by count >))
        protocol-def
          (->> (reductions conj '() distinct-arities)
               rest
               (map-indexed
                 (fn [i arities']
                   (let [variant-identifier (- (count distinct-arities) i)
                         genned-protocol-method-name-variant
                          (if (= i (-> distinct-arities count dec))
                              genned-protocol-method-name
                              (append-variant-identifier genned-protocol-method-name
                                variant-identifier))]
                     (cons genned-protocol-method-name-variant arities'))))
               (list* 'defprotocol genned-protocol-name))
        genned-protocol-method-names
          (->> protocol-def
               rest rest
               (map first))]
    (kw-map protocol-def genned-protocol-method-names)))

; TODO hopefully get rid of this step
; TODO break it up

(defn gen-extend-protocol-from-interface
  {:todo {0 {:msg "change `extend-protocol` to `extend-type` so that CLJS runs faster"
             :priority 10}}}
  ; Original Interface:         ([#{number?} x #{number?} y #{char? Object} z] ~@body)
  ; Expanded Interface Arity 1: ([^long      x ^int       y ^char           z] ~@body)
  ; Protocol Arity 1:           ([^long      x ^Integer   y ^Character      z]
  ;                               (let [y (int y) z (char z)] ; TODO ->int y, ->charz
  ;                                 ~@body)

  ; (def abcde 0)
  ; ((fn [^"[B" x ^long n] (clojure.lang.RT/aget x n)) my-byte-array abcde) => 0, no reflection
  [{:keys [genned-protocol-name genned-protocol-method-name
           reify-body lang first-types]}]
  (assert (contains? reify-body))
  (assert (val? genned-protocol-name))
  (assert (val? genned-protocol-method-name))

  (let [body-sorted
          (->> reify-body rest rest
               (map (fn [[sym & body]]
                      (-> body (update-first (fn1 th/with-type-hint (th/type-hint sym))))))) ; remove reify method names
        body-filtered body-sorted
        _ (log/ppr-hints :macro-expand-protocol "BODY SORTED" body-sorted)
        body-mapped
          (->> body-filtered
               (map (fn [[arglist & body :as method]]
                      (log/ppr-hints :macro-expand "IN BODY MAPPED" (kw-map arglist body))
                      (let [first-type         (-> arglist second th/type-hint)
                            first-type-unboxed (-> first-type tcore/->unboxed|sym)
                            unboxed-version-exists? (get-in first-types [first-type-unboxed (-> arglist count dec)])]
                        (if ; Unboxed version of arity already exists? Skip generation of that type/arity
                            (and (tcore/boxed?|sym first-type) unboxed-version-exists?) ; |dec| because 'this' is the first arg
                            [] ; Empty so concat gets nothing
                            (let [boxed-first-type (-> first-type (whenp (= lang :clj) tcore/->boxed|sym))
                                             ; (whenc (-> arglist second th/type-hint tcore/->boxed|sym)
                                             ;        (fn-> name (= "[Ljava.lang.Object;"))
                                             ;   '(Class/forName "[Ljava.lang.Object;"))
                                  return-type (-> arglist
                                                  (with-protocol-arglist-type-hint lang nil)) ; the arglist length doesn't matter
                                  arglist-f   (->> arglist rest (with-protocol-appropriate-arglist lang))
                                  arglist-f   (if return-type
                                                  arglist-f
                                                  (th/with-type-hint arglist-f return-type) )
                                  body-f      (trans/hint-body-with-arglist body arglist lang :protocol)
                                  extension-f [boxed-first-type (cons genned-protocol-method-name (cons arglist-f body-f))]]
                              (log/ppr-hints :macro-expand "IN BODY AFTER MAPPED" (kw-map boxed-first-type extension-f first-type))
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
