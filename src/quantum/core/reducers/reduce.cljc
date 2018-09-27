(ns ^{:doc
      "A library for reduction and parallel folding. Alpha and subject
      to change.  Note that fold and its derivatives require Java 7+ or
      Java 6 + jsr166y.jar for fork/join support.

      Adds some interesting reducers and folders from different sources
      gleaned from the far reaches of the internet. Some of them have
      unexpectedly great performance."
      :author       "Rich Hickey"
      :contributors #{"Alan Malloy" "Alex Gunnarson" "Christophe Grand"}}
  quantum.core.reducers.reduce
  (:refer-clojure :exclude
    [reduce into, deref reset! transduce])
  (:require
    [clojure.core                  :as core]
    [clojure.core.async            :as async]
    [fast-zip.core                 :as zip]
    [quantum.core.data.vector      :as vec
      :refer [catvec]]
#?@(:clj
   [[seqspert.hash-set]
    [seqspert.hash-map]])
    [quantum.core.data.set         :as set]
    [quantum.core.data.map         :as map]
    [quantum.core.error            :as err
      :refer [>ex-info]]
    [quantum.core.fn
      :refer [fnl]]
    [quantum.core.macros           :as macros
      :refer [defnt]]
    [quantum.core.refs             :as refs
      :refer [deref !boolean !long ! reset!]]
    [quantum.core.type-old         :as t
      :refer [editable? val?]]
    [quantum.core.type.defs
      #?@(:cljs [:refer [Transformer]])]
    [quantum.core.vars             :as var
      :refer [defalias defaliases]]
    [quantum.untyped.core.reducers :as ur])
#?(:cljs
  (:require-macros
    [quantum.core.reducers.reduce  :as self
      :refer [reduce]]))
  (:import
  #?@(:clj  [[quantum.untyped.core.reducers Transformer]
             quantum.core.data.Array]
      :cljs [[goog.string StringBuffer]])))


; HEADLESS FIX
; {:attribution "Christophe Grand - http://clj-me.cgrand.net/2013/09/11/macros-closures-and-unexpected-object-retention/"}
; Creating a reducer holds the head of the collection in a closure.
; Thus, a huge lazy-seq can be tied up memory as it becomes realized.

; Fixing it so the seqs are headless.
; Christophe Grand - https://groups.google.com/forum/#!searchin/clojure-dev/reducer/clojure-dev/t6NhGnYNH1A/2lXghJS5HywJ

;; TODO TYPED
(defaliases ur transformer transformer? transducer->transformer)

(defn conj-red
  "Like |conj| but includes a 3-arity for |reduce-kv| purposes."
  ([ret] ret)
  ([ret x  ] (conj ret x))
  ([ret k v] (conj ret [k v])))

(defn conj!-red
  "Like |conj!| but includes a 3-arity for |reduce-kv| purposes."
  ([ret] ret)
  ([ret x  ] (conj! ret x))
  ([ret k v] (conj! ret [k v])))

(defn transient-into [to from]
  (when-let [ret (reduce* from conj!-red (transient to))]
    (-> ret persistent! (with-meta (meta to)))))

(defn persistent-into [to from]
  (reduce* from conj-red to))
