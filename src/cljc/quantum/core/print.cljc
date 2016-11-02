(ns
  ^{:doc "Printing functions such as fipp.edn (a fast pretty printer),
          |pr-attrs| (which prints the key attributes of a given object
          or expression, blacklisted printable objects (so you don't
          have to wait while you accidentally print out an entire database),
          and so on."
    :attribution "Alex Gunnarson"}
  quantum.core.print
  (:require
    [quantum.core.core        :as qcore]
    [quantum.core.fn          :as fn
      :refer        [#?@(:clj [fn-> fn->>])]
      :refer-macros [          fn-> fn->>]]
    [quantum.core.logic       :as logic
      :refer        [#?@(:clj [condf])]
      :refer-macros [          condf]]
    [quantum.core.data.vector :as vec]  ; To work around CLJS non-spliceability of Tuples
    [quantum.core.vars        :as var
      :refer        [#?(:clj defalias)]
      :refer-macros [        defalias]]
    [quantum.core.meta.debug  :as debug]
#?(:clj [clojure.core.matrix.impl.pprint :as mpprint])
#?(:clj
    [fipp.edn                 :as pr] ; Fipp currently has strange execution problems in CLJS
   :cljs
    [cljs.pprint              :as pr
      :include-macros true])))

(defonce ^{:doc "A set of classes not to print"}
  blacklist  (atom #{}))

(defalias js-println qcore/js-println)

(defn !
  "Fast pretty print using brandonbloom/fipp.
   At least 5 times faster than |clojure.pprint/pprint|.
   Prints no later than having consumed the bound amount of memory,
   so you see your first few lines of output instantaneously."
  ([] (println))
  ([obj]
    (binding [*print-length* (or *print-length* 1000)] ; A reasonable default
      (if #?(:clj  (instance? Throwable obj)
             :cljs false)
          #?(:clj  (debug/trace obj)
             :cljs false)
          (do
            (cond
              (and (string? obj) (> (count obj) *print-length*))
                (println
                  (str "String is too long to print ("
                       (str (count obj) " elements")
                       ").")
                  "|max-length| is set at" (str *print-length* ".")) ; TODO fix so ellipsize
              (contains? @blacklist (type obj))
                (println
                  "Object's class"
                  (str (type obj) "(" ")")
                  "is blacklisted for printing.")
              :else
                (#?(:clj  fipp.edn/pprint
                    :cljs cljs.pprint/pprint) obj))
            nil))))
  ([obj & objs]
    (doseq [obj-n (cons obj objs)]
      (! obj-n))))

#?(:clj (reset! debug/pretty-printer !))

(def suppress (partial (constantly nil)))

#_(defn representative-coll
  "Gets the first element of every collection, until it returns empty.

   Useful for printing out representative samples of large collections
   which would be undesirable to print in whole."
  {:attribution "Alex Gunnarson"}
  [source-0]
  (if ((fn-or (fn-not coll?) empty?) source-0)
      source-0
      (loop [source-n   source-0
             assoc-keys []
             ret        nil]
        (if ((fn-or (fn-not coll?) empty?) source-n)
            ret
            (let [ret-n+1-0
                    (condf source-n
                      ; TODO |get-map-constructor| is in collections
                      ; record? (fn [source-n*]
                      ;           (let [^Fn constructor-fn
                      ;                   (get-map-constructor source-n*)]
                      ;             (->> source-n* first (apply hash-map)
                      ;                  constructor-fn)))
                      map?    (fn->> first (apply hash-map))
                      vector? (fn->> first vector))
                  assoc-key-n+1
                    (condf ret-n+1-0
                      map?    (fn-> keys first)
                      vector? (constantly 0))
                  assoc-keys-n+1
                    (conj assoc-keys assoc-key-n+1)
                  ret-n+1
                    (if (nil? ret)
                        ret-n+1-0
                        (assoc-in ret assoc-keys ret-n+1-0))
                  source-n+1
                    (condf ret-n+1-0
                      map?    (-> vals first)
                      vector? first)]
              (recur source-n+1
                     assoc-keys-n+1
                     ret-n+1))))))

(declare representative-coll)

(defn !* [obj] (-> obj representative-coll !))

#?(:clj
(defmacro pr-attrs
  "Prints the attributes of a given object/expression.
   These attributes include the object/expression that was evaluated
   (the text itself), the class, its keys (if applicable), and the
   representative value of the object/expression.

   Representative value means the recursive first item in a collection.
   This eliminates, or at least greatly diminishes, massive and
   tiring pauses spent printing out the result of an object/expression."
  {:attribution "Alex Gunnarson"}
  [obj]
  `(do (println "Expr:"     (quote ~obj))
       (println "Class:"    (class ~obj))
       (println "Keys:"     (ifn   ~obj coll? keys+ (constantly nil)))
       (println "Repr. object: ") (-> ~obj representative-coll !)
       (println))))

(defn- pprint-symbol [x]
  (when-let [has-hint? (-> x meta (contains? :tag))]
    (print "^")
    (print (-> x meta :tag))
    (print " "))
  (when-let [ns- (namespace x)]
    (print ns-)
    (print "/"))
  (print (name x)))

#?(:clj
(defonce pprint-vector-0
  (.getMethod ^clojure.lang.MultiFn clojure.pprint/simple-dispatch clojure.lang.APersistentVector)))

#?(:clj
(defn- pprint-vector [x]
  (when-let [has-hint? (-> x meta (contains? :tag))]
    (print "^")
    (print (-> x meta :tag))
    (print " "))
  (pprint-vector-0 x)))

#?(:clj (.addMethod ^clojure.lang.MultiFn clojure.pprint/simple-dispatch clojure.lang.Symbol pprint-symbol))
#?(:clj (.addMethod ^clojure.lang.MultiFn clojure.pprint/simple-dispatch clojure.lang.APersistentVector pprint-vector))

(defn pprint-hints [x]
  #?(:clj
      (clojure.pprint/with-pprint-dispatch clojure.pprint/simple-dispatch  ;;Make the dispatch to your print function
        (clojure.pprint/pprint x))
     :cljs
      (! x)))

#_"Pretty-prints an array. Returns a String containing the pretty-printed representation."
#?(:clj (defalias pprint-arr mpprint/pm))
