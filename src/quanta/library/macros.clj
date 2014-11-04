(ns quanta.library.macros)
(require
  '[quanta.library.ns   :as ns :refer [defalias source defs]]
  '[quanta.library.type        :refer :all         ]
  '[potemkin.types      :as t]) ; extend-protocol+ doesn't quite work...
(ns/require-all *ns* :clj)

(defalias definterface+    t/definterface+)
(defalias defprotocol+     t/defprotocol+)
(defalias extend-protocol+ t/extend-protocol+) ; extend-protocol+ doesn't quite work...

(defmacro extend-protocol-type
  [protocol prot-type & methods]
  `(extend-protocol ~protocol ~prot-type ~@methods))
(defmacro extend-protocol-types
  [protocol prot-types & methods]
  `(doseq [prot-type# ~prot-types]
     (extend-protocol-type ~protocol (eval prot-type#) ~@methods)))

; The most general.
; (defmacro extend-protocol-typed [expr]
;   (extend-protocol (count+ [% coll] (alength % coll))))


; (defprotocol+ QBItemSearch
;   (qb-item-search-base    [search-token ^AFunction filter-fn ^AFunction comparison-fn]
;     ([String Pattern]
;       (->> qb-items*
;            (filter-fn (compr key+ (comparison-fn search-token))))))
;   (qb-item-search-compare [search-token ^AFunction filter-fn]
;     (String
;       (qb-item-search-base search-token filter-fn eq?))
;     (Pattern
;       (qb-item-search-base search-token filter-fn (partial partial str/re-find+))))
;   (qb-item-search*        [search-token]
;     ([String Pattern]
;       (qb-item-search-compare search-token filter+)))
;   (qb-item-search-first* [search-token]
;     ([String Pattern]
;       (qb-item-search-compare search-token ffilter))))

; (defmacro defprotocol+ [protocol & exprs]
;   '(let [methods# ; Just take the functions
;           (->> (rest exprs)
;                (take-while (fn->> str first+ (not= "["))))]
;      (defprotocol ~protocol
;        ~@methods#)
;      ;(extend-protocol-types protocol (first exprs) methods#)
;      ))

; (let [a# '[[bb] (fn [] cc) (fn [] dd) [ee]]]
;   (->> (rest a#)
;        (take-while (fn->> str first+ (not= "[")))
;        (map (juxt first second))))


; (defmacro quote-exprs [& exprs]
;   `~(exprs))