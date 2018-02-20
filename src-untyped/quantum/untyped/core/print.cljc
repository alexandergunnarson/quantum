(ns quantum.untyped.core.print
  (:require
#?@(:clj
   [[io.aviso.exception]])
    [quantum.untyped.core.collections :as uc]
    [quantum.untyped.core.core        :as ucore]
    [quantum.untyped.core.error       :as uerr
      :refer [>err]]
    [quantum.untyped.core.vars        :as uvar
      :refer [defalias]]))

(ucore/log-this-ns)

;; ===== Data and dynamic bindings ===== ;;

(defalias *blacklist uerr/*print-blacklist)

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

(defalias uerr/ppr)

;; ===== `ppr` varieties ===== ;;

(defn ppr-meta  [x] (binding [*print-meta* true] (ppr x)))
(defn ppr-hints [x] (binding [*print-meta* true] (ppr x))) ; TODO this isn't right

(defn ppr-error [x]
  #?(:clj (let [pr-data-to-str? @uerr/*pr-data-to-str?]
            (println (str "EXCEPTION" (when-not pr-data-to-str? " TRACE + MESSAGE") ":"))
            (print (io.aviso.exception/format-exception x {:properties false}))
            (when-not pr-data-to-str?
              (let [e (>err x)
                    e (or (:cause e) e)]
                (println "--------------------")
                (when-let [e' (->> (dissoc e :trace :cause :message :type)
                                   (uc/filter-vals+ some?)
                                   (into (array-map))
                                   not-empty)]
                  (println "EXCEPTION DATA:")
                  (ppr e'))))) ; TODO fix so it doesn't print "empty: false"
     :cljs (ppr x)))

(defalias uerr/ppr-str)

;; ===== Print groups ===== ;;

(deftype ^{:doc "Defines a print group."} Group [xs])

(defn >group
  ([xs] (Group. xs))
  ([x & args] (Group. (cons x args))))

(defn group? [x] (instance? Group x))
