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
    [quantum.core.fn          :as fn
      :refer [fn-> fn->> fn']]
    [quantum.core.logic       :as logic
      :refer [condf]]
    [quantum.core.data.vector :as vec]  ; To work around CLJS non-spliceability of Tuples
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
  ([obj]
    (binding [*print-length* (or *print-length* 1000)] ; A reasonable default
      (if (instance? #?(:clj Throwable :cljs js/Error) obj)
          #?(:clj  (debug/trace obj)
             :cljs (let [obj' (if (instance? ExceptionInfo obj)
                                  {:type    'cljs.core/ExceptionInfo
                                   :stack   (-> obj .-stack str/split-lines)
                                   :message (.-message obj)
                                   :data    (ex-data obj)}
                                  {:type    'js/Error
                                   :stack   (-> obj .-stack str/split-lines)
                                   :message (.-message obj)})]
                     (cljs.pprint/pprint obj')))
          (do
            (cond
              (and (string? obj) (> (count obj) *print-length*))
                (println
                  (str "String is too long to print ("
                       (str (count obj) " elements")
                       ").")
                  "`*print-length*` is set at" (str *print-length* ".")) ; TODO fix so ellipsize
              (contains? @blacklist (type obj))
                (println
                  "Object's class"
                  (str (type obj) "(" ")")
                  "is blacklisted for printing.")
              :else
                (#?(:clj  pr/pprint
                    :cljs pr/pprint) obj))
            nil))))
  ([obj & objs]
    (doseq [obj-n (cons obj objs)]
      (ppr obj-n))))

(defn ppr-str
  "Like `pr-str`, but pretty-prints."
  [x] (with-out-str (ppr x)))

(defn ppr-meta  [x] (binding [*print-meta* true] (ppr x)))

;; TODO fix this
(defn ppr-hints [x] (binding [*print-meta* true] (ppr x)))

;; Makes it so fipp doesn't print tagged literals for every record
#_(quantum.core.vars/reset-var! #'fipp.ednize/record->tagged
  (fn [x] (tagged-literal '... (into {} x))))

#?(:clj (reset! debug/pretty-printer ppr))

(def suppress (partial (fn' nil)))

;; Pretty-prints an array. Returns a String containing the pretty-printed representation.
#?(:clj (defalias pprint-arr mpprint/pm))
