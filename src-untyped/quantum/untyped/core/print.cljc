(ns quantum.untyped.core.print
  (:require
#?@(:clj
   [[io.aviso.exception]])
    [fipp.edn                         :as fipp]
    [quantum.untyped.core.collections :as uc]
    [quantum.untyped.core.core        :as ucore]
    [quantum.untyped.core.error       :as uerr
      :refer [error? >err]]
    [quantum.untyped.core.vars        :as uvar
      :refer [defalias]]))

(ucore/log-this-ns)

;; ===== Data and dynamic bindings ===== ;;

(uvar/defonce *blacklist "A set of classes not to print" (atom #{}))

(def ^:dynamic ^{:doc "Flag for namespace-'collapsing' symbols"}
  *collapse-symbols?* false)

(def ^:dynamic
  ^{:doc "Flag for printing out expressions as a developer would see them in source code"}
  *print-as-code?* false)

;; ===== `println` varieties ===== ;;

(defn js-println [& args]
  (print "\n/* " )
  (apply println args)
  (println "*/"))

;; ===== `ppr` ===== ;;

(defn ppr
  "Fast pretty print using brandonbloom/fipp.
   At least 5 times faster than `clojure.pprint/pprint`.
   Prints no later than having consumed the bound amount of memory,
   so you see your first few lines of output instantaneously."
  ([] (println))
  ([x]
    (binding [*print-length* (or *print-length* 1000)] ; A reasonable default
      (do (cond
            (error? x)
              (fipp/pprint (>err x))
            (and (string? x) (> (count x) *print-length*))
              (println
                (str "String is too long to print ("
                     (str (count x) " elements")
                     ").")
                "`*print-length*` is set at" (str *print-length* ".")) ; TODO fix so ellipsize
            (contains? @*blacklist (type x))
              (println
                "Object's class"
                (str (type x) "(" ")")
                "is blacklisted for printing.")
            :else
              (fipp/pprint x))
          nil)))
  ([x & xs]
    (doseq [x' (cons x xs)] (ppr x'))))

;; ===== `ppr` varieties ===== ;;

(defn ppr-meta  [x] (binding [*print-meta* true] (ppr x)))
(defn ppr-hints [x] (binding [*print-meta* true] (ppr x))) ; TODO this isn't right

(defn ppr-error [x]
  #?(:clj (do (println "EXCEPTION TRACE + MESSAGE:")
              (print (io.aviso.exception/format-exception x {:properties false}))
              (let [e (>err x)
                    e (or (:cause e) e)]
                (println "--------------------")
                (when-let [e' (->> (dissoc e :trace :cause :message :type)
                                   (uc/filter-vals+ some?)
                                   (into (array-map))
                                   not-empty)]
                  (println "EXCEPTION DATA:")
                  (ppr e')))) ; TODO fix so it doesn't print "empty: false"

     :cljs (ppr x)))

(defn ppr-str
  "Like `pr-str`, but pretty-prints."
  [x] (with-out-str (ppr x)))

;; ===== Print groups ===== ;;

(deftype ^{:doc "Defines a print group."} Group [xs])

(defn >group
  ([xs] (Group. xs))
  ([x & args] (Group. (cons x args))))

(defn group? [x] (instance? Group x))
