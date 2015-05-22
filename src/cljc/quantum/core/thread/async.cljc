(ns
  ^{:doc "Asynchronous things."
    :attribution "Alex Gunnarson"}
  quantum.core.thread
  (:refer-clojure :exclude
    [contains? read for doseq reduce repeat repeatedly range merge count vec
     into first second rest last butlast get pop peek])
  #?(:clj (:gen-class)))

#?(:clj (require
  '[quantum.core.ns          :as ns    :refer [defalias alias-ns]]))
#?(:clj (ns/require-all *ns* :clj))
#?(:clj (require
  '[quantum.core.numeric     :as num]
  '[quantum.core.function    :as fn   :refer :all]
  '[quantum.core.string      :as str]
  '[quantum.core.error                :refer :all]
  '[quantum.core.logic       :as log  :refer :all]
  '[quantum.core.data.vector :as vec  :refer [catvec]]
  '[quantum.core.collections :as coll :refer :all]
  '[quantum.core.error       :as err  :refer [throw+ try+]]
  '[clojure.core.async :as async :refer [go <! >! alts!]]))

#?(:clj
(defmacro <? [expr]
  `(let [expr-result# (<! ~expr)]
     (if ;(quantum.core.type/error? chan-0#)
         (instance? js/Error expr-result#)
         (throw expr-result#)
         expr-result#))))

; #?(:clj
; (defmacro <? [ch]
;   `(let [e# (<! ~ch)]
;      (when (instance? js/Error e#) (throw e#))
;      e#)))

(defmacro try-go
  {:attribution "pepa.async"}
  [& body]
  `(cljs.core.async.macros/go
     (try
       ~@body
       (catch js/Error e#
         e#))))
