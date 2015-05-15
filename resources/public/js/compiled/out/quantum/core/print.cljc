#?(:clj
(ns quantum.core.print
  (:refer-clojure :exclude
    [contains? for doseq reduce repeat repeatedly range merge count
     vec into first second rest
     last butlast get pop peek])))

(ns
  ^{:doc "Printing functions such as fipp.edn (a fast pretty printer),
          |pr-attrs| (which prints the key attributes of a given object
          or expression, blacklisted printable objects (so you don't
          have to wait while you accidentally print out an entire database), 
          and so on."
    :attribution "Alex Gunnarson"}
  quantum.core.print
  (:refer-clojure :exclude
    [contains? for doseq reduce repeat repeatedly range merge count
     vec into first second rest
     last butlast get pop peek])
  (:require
    [quantum.core.ns           :as ns   #?@(:clj [:refer [defalias alias-ns]])]
    [quantum.core.collections                    :refer [assoc-in+]]
    [quantum.core.function              #?@(:clj [:refer :all]               )]
    [quantum.core.logic                 #?@(:clj [:refer :all]               )]
    [quantum.core.type                  #?@(:clj [:refer :all]               )]
    [quantum.core.numeric      :as num]
    [quantum.core.string       :as str]
    [quantum.core.data.xml     :as xml]
    #?(:clj [clojure.pprint    :as pprint])
    #?(:clj [fipp.edn          :as pr]))
  #?(:clj (:gen-class)))

#?(:clj
(do
(ns/require-all *ns* :clj)

; "At least 5 times faster than clojure.pprint/pprint
;  Prints no later than having consumed the bound amount of memory,
;  so you see your first few lines of output instantaneously.
;  :attribution: https://github.com/brandonbloom/fipp"
(defalias pprint pr/pprint)
(def ^:dynamic *max-length* 1000)
(def ^:dynamic *blacklist* (atom #{})) ; a list of classes not to print

(defn ! [obj]
  (let [ct (long
             (try
               (count obj)
               (catch UnsupportedOperationException _ 1)))]
    (cond
      (> ct (long *max-length*))
        (println
          (str "Object is too long to print ("
               (str/sp ct "elements")
               ").")
          "*max-length* is set at" (str *max-length* "."))
      (contains? @*blacklist* (class obj))
        (println
          "Object's class"
          (str/paren (class obj))
          "is blacklisted for printing.")
      :else
        (pr/pprint obj))))

(defalias print-table pprint/print-table) 

(def ^:dynamic *print-right-margin* pprint/*print-right-margin*)

(def  suppress (partial (constantly nil)))

(defn pprint-xml
  {:attribution "Alex Gunnarson"
   :todo ["A rather large function. Did this a long time ago" "Where does this function belong?"]}
  [xml-str]
  (let [print-tabbed 
          (fn [[tab xml-ln]]
            (->> " "
                 (repeat tab)
                 (apply str)
                 (<- str xml-ln)
                 println))
        print-body
          (fn [[elem-n :as elems-n] 
                elem-n-1
                ^long tabs-n
                elems-f]
            (if (empty? elems-n)
                (doseq [elem-n elems-f] (print-tabbed elem-n))
                (let [type-n   (xml/elem-type elem-n)
                      type-n-1 (xml/elem-type elem-n-1)
                      tabs-n+1
                        (cond (or (= :beg type-n)
                                  (= :body  type-n))
                              (if (= :beg type-n-1)
                                  (+ (long tabs-n)  2)
                                  (long tabs-n))
                              :end
                              (if (= :beg type-n-1)
                                  (long tabs-n)
                                  (- (long tabs-n) 2)))]
                  (recur (rest elems-n)
                         elem-n
                         (long tabs-n+1)
                         (conj elems-f [tabs-n+1 elem-n])))))]
    (print-body (whenf xml-str string? xml/split) nil (long 0) [])))

(defn representative-coll
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
                      record? (fn [source-n*]
                                (let [^Fn constructor-fn
                                        (get-map-constructor source-n*)]
                                  (->> source-n* first (apply hash-map)
                                       constructor-fn)))
                      map?    (fn->> first (apply hash-map))
                      vector? (fn->> first vector)
                      lseq?   (fn->> first vector)) ; is this a good decision?
                  assoc-key-n+1
                    (condf ret-n+1-0
                      map?    fkey+
                      vector? (constantly 0)
                      lseq?   (constantly 0))
                  assoc-keys-n+1
                    (conj assoc-keys assoc-key-n+1)
                  ret-n+1
                    (if (nil? ret)
                        ret-n+1-0
                        (assoc-in+ ret assoc-keys ret-n+1-0))
                  source-n+1
                    (condf ret-n+1-0
                      map?    fval+
                      vector? first
                      lseq?   first)]
              (recur source-n+1
                     assoc-keys-n+1
                     ret-n+1))))))  

(defn !* [obj] (-> obj representative-coll !))

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
       (println "Repr. object: ") (-> ~obj quantum.core.print/representative-coll !)
       (println)))))