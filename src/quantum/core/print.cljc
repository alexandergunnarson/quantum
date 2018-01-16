(ns
  ^{:doc "Printing functions such as fipp.edn (a fast pretty printer),
          |pr-attrs| (which prints the key attributes of a given object
          or expression, blacklisted printable objects (so you don't
          have to wait while you accidentally print out an entire database),
          and so on."
    :attribution "alexandergunnarson"}
  quantum.core.print
  (:require
    [clojure.string           :as str]
    [quantum.core.core        :as qcore]
    [quantum.core.error       :as err
      :refer [>err error?]]
    [quantum.core.fn          :as fn
      :refer [fn-> fn->> fn']]
    [quantum.core.logic       :as logic
      :refer [condf]]
    [quantum.core.data.vector :as vec]  ; To work around CLJS non-spliceability of Tuples
    [quantum.core.untyped.reducers :as r
      :refer [filter-vals+]]
    [quantum.core.untyped.convert :as uconv
      :refer [>symbol]]
    [quantum.core.vars        :as var
      :refer [defalias]]
    [quantum.core.meta.debug  :as debug]
#?(:clj [clojure.core.matrix.impl.pprint :as mpprint])
#?(:clj
    [fipp.edn
     #_fipp.clojure             :as pr] ; Fipp currently has strange execution problems in CLJS
   :cljs
    [cljs.pprint              :as pr
      :include-macros true])))

; TODO it would be nice to be able to print functions prettily like #fn clojure.core/inc

(defonce ^{:doc "A set of classes not to print"}
  blacklist (atom #{}))

(defalias js-println qcore/js-println)

(defn ppr
  "Fast pretty print using brandonbloom/fipp.
   At least 5 times faster than |clojure.pprint/pprint|.
   Prints no later than having consumed the bound amount of memory,
   so you see your first few lines of output instantaneously."
  ([] (println))
  ([x]
    (binding [*print-length* (or *print-length* 1000)] ; A reasonable default
      (do (cond
            (error? x)
              (pr/pprint (>err x))
            (and (string? x) (> (count x) *print-length*))
              (println
                (str "String is too long to print ("
                     (str (count x) " elements")
                     ").")
                "`*print-length*` is set at" (str *print-length* ".")) ; TODO fix so ellipsize
            (contains? @blacklist (type x))
              (println
                "Object's class"
                (str (type x) "(" ")")
                "is blacklisted for printing.")
            :else
              (#?(:clj  pr/pprint
                  :cljs pr/pprint) x))
          nil)))
  ([x & xs]
    (doseq [x' (cons x xs)] (ppr x'))))

(defn ppr-str
  "Like `pr-str`, but pretty-prints."
  [x] (with-out-str (ppr x)))

(defn ppr-meta  [x] (binding [*print-meta* true] (ppr x)))

;; TODO fix this
(defn ppr-hints [x] (binding [*print-meta* true] (ppr x)))

(defn ppr-error [x]
  #?(:clj (do (println "EXCEPTION TRACE + MESSAGE:")
              (print (io.aviso.exception/format-exception x {:properties false}))
              (let [e (>err x)
                    e (or (:cause e) e)]
                (println "--------------------")
                (when-let [e' (->> (dissoc e :trace :cause :message :type)
                                   (filter-vals+ some?)
                                   (into (array-map))
                                   not-empty)]
                  (println "EXCEPTION DATA:")
                  (ppr e')))) ; TODO fix so it doesn't print "empty: false"

     :cljs (ppr x)))

;; Makes it so fipp doesn't print tagged literals for every record
#_(quantum.core.vars/reset-var! #'fipp.ednize/record->tagged
  (fn [x] (tagged-literal '... (into {} x))))

#?(:clj (reset! debug/pretty-printer ppr))

(def suppress (partial (fn' nil)))

;; Pretty-prints an array. Returns a String containing the pretty-printed representation.
#?(:clj (defalias pprint-arr mpprint/pm))

(def ^:dynamic ^{:doc "Flag for namespace-'collapsing' symbols"}
  *collapse-symbols?* false)

(def ^:dynamic
  ^{:doc "Flag for printing out expressions as a developer would see them in source code"}
  *print-as-code?* false)

(defn expr->code [x] (cond-> x (fn? x) >symbol))

(deftype ^{:doc "Defines a print group."} Group [xs])

(defn >group
  ([xs] (Group. xs))
  ([x & args] (Group. (cons x args))))
(defn group? [x] (instance? Group x))
